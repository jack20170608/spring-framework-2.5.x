/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.framework;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.TargetSource;

/**
 * InvocationHandler implementation for the Spring AOP framework,
 * based on J2SE 1.3+ dynamic proxies.
 *
 * <p>Creates a J2SE proxy when proxied interfaces are given, a CGLIB proxy
 * for the actual target class if not. Note that the latter will only work
 * if the target class does not have final methods, as a dynamic subclass
 * will be created at runtime.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by a ProxyConfig implementation. This class is internal
 * to the Spring framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class can be threadsafe if the
 * underlying (target) class is threadsafe.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @version $Id: JdkDynamicAopProxy.java,v 1.1 2003-12-01 15:40:46 johnsonr Exp $
 * @see java.lang.reflect.Proxy
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
	
	/*
	 * NOTE: we could avoid the code duplication between this class and the CGLIB
	 * proxies by refactoring invoke() into a template method. However, this approach
	 * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
	 * elegance for performance. (We have a good test suite to ensure that the different
	 * proxies behave the same :-)
	 * This way, we can also more easily take advantage of minor optimizations in each class.
	 */
	
	
	private final Log logger = LogFactory.getLog(getClass());

	/** Config used to configure this proxy */
	private final AdvisedSupport advised;

	/**
	 * 
	 * @throws AopConfigException if the config is invalid. We try
	 * to throw an informative exception in this case, rather than let
	 * a mysterious failure happen later.
	 */
	protected JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		if (config == null)
			throw new AopConfigException("Cannot create AopProxy with null ProxyConfig");
		if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE)
			throw new AopConfigException("Cannot create AopProxy with no advisors and no target source");
		this.advised = config;
	}

	
	
	/**
	 * Implementation of InvocationHandler.invoke.
	 * Callers will see exactly the exception thrown by the target, unless a hook
	 * method throws an exception.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	
		MethodInvocation invocation = null;
		MethodInvocation oldInvocation = null;
		Object oldProxy = null;
		boolean setInvocationContext = false;
		boolean setProxyContext = false;
	
		TargetSource targetSource = advised.getTargetSource();
		Class targetClass = null;//targetSource.getTargetClass();
		Object target = null;		
		
		try {
			// Try special rules for equals() method and implementation of the
			// ProxyConfig AOP configuration interface
			if (AopProxyUtils.EQUALS_METHOD.equals(method)) {
				// What if equals throws exception!?

				// This class implements the equals() method itself
				return method.invoke(this, args);
			}
			else if (Advised.class.equals(method.getDeclaringClass())) {
				// Service invocations on ProxyConfig with the proxy config
				return method.invoke(this.advised, args);
			}
			
			Object retVal = null;
			
			// May be null. Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			target = targetSource.getTarget();
			if (target != null) {
				targetClass = target.getClass();
			}
		
			List chain = advised.getAdvisorChainFactory().getInterceptorsAndDynamicInterceptionAdvice(this.advised, proxy, method, targetClass);
			
			
			
			// Check whether we only have one InvokerInterceptor: that is, no real advice,
			// but just reflective invocation of the target.
			// We can only do this if the Advised config object lets us.
			if (advised.canOptimizeOutEmptyAdviceChain() && 
					chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying
				retVal = AopProxyUtils.invokeJoinpointUsingReflection(target, method, args);
			}
			else {
				// We need to create a method invocation...
				invocation = advised.getMethodInvocationFactory().getMethodInvocation(proxy, method, targetClass, target, args, chain, advised);
			
				if (this.advised.getExposeInvocation()) {
					// Make invocation available if necessary.
					// Save the old value to reset when this method returns
					// so that we don't blow away any existing state
					oldInvocation = AopContext.setCurrentInvocation(invocation);
					// We need to know whether we actually set it, as
					// this block may not have been reached even if exposeInvocation
					// is true
					setInvocationContext = true;
				}
				
				if (this.advised.getExposeProxy()) {
					// Make invocation available if necessary
					oldProxy = AopContext.setCurrentProxy(proxy);
					setProxyContext = true;
				}
				
				// If we get here, we need to create a MethodInvocation
				retVal = invocation.proceed();
				
			}
			
			// Massage return value if necessary
			if (retVal != null && retVal == target) {
				// Special case: it returned "this"
				// Note that we can't help if the target sets
				// a reference to itself in another returned object
				retVal = proxy;
			}
			return retVal;
		}
		finally {
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource
				targetSource.releaseTarget(target);
			}
			
			if (setInvocationContext) {
				// Restore old invocation, which may be null
				AopContext.setCurrentInvocation(oldInvocation);
			}
			if (setProxyContext) {
				// Restore old proxy
				AopContext.setCurrentProxy(oldProxy);
			}
			
			if (invocation != null) {
				advised.getMethodInvocationFactory().release(invocation);
			}
		}
	}
	

	/**
	 * Creates a new Proxy object for the given object, proxying
	 * the given interface. Uses the thread context class loader.
	 */
	public Object getProxy() {
		return getProxy(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Creates a new Proxy object for the given object, proxying
	 * the given interface. Uses the given class loader.
	 */
	public Object getProxy(ClassLoader cl) {
		// Proxy specific interfaces: J2SE dynamic proxy is sufficient
		if (logger.isInfoEnabled())
			logger.info("Creating J2SE proxy for [" + this.advised.getTargetSource().getTargetClass() + "]");
		Class[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(advised);
		return Proxy.newProxyInstance(cl, proxiedInterfaces, this);
	}

	

	/**
	 * Equality means interceptors and interfaces are ==.
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @param other may be a dynamic proxy wrapping an instance
	 * of this class
	 */
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		
		JdkDynamicAopProxy aopr2 = null;
		if (other instanceof JdkDynamicAopProxy) {
			aopr2 = (JdkDynamicAopProxy) other;
		}
		else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy))
				return false;
			aopr2 = (JdkDynamicAopProxy) ih; 
		}
		else {
			// Not a valid comparison
			return false;
		}
		
		// If we get here, aopr2 is the other AopProxy
		if (this == aopr2)
			return true;
			
		if (!Arrays.equals(aopr2.advised.getProxiedInterfaces(), this.advised.getProxiedInterfaces()))
			return false;
		
		if (!Arrays.equals(aopr2.advised.getAdvisors(), this.advised.getAdvisors()))
			return false;
			
		return true;
	}

}
