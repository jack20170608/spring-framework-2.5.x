/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.dao;

/**
 * Root for exceptions thrown when we use a data access resource incorrectly.
 * Thrown for example on specifying bad SQL when using a RDBMS.
 * Resource-specific subclasses will probably be supplied by data access packages.
 * @author Rod Johnson
 * @version $Id: InvalidDataAccessResourceUsageException.java,v 1.3 2004-02-11 01:06:33 jhoeller Exp $
 */
public class InvalidDataAccessResourceUsageException extends DataAccessException {
	
	/**
	 * Constructor for InvalidDataAccessResourceUsageException.
	 * @param msg message
	 */
	public InvalidDataAccessResourceUsageException(String msg) {
		super(msg);
	}
	/**
	 * Constructor for InvalidDataAccessResourceUsageException.
	 * @param msg message
	 * @param ex root cause
	 */
	public InvalidDataAccessResourceUsageException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
