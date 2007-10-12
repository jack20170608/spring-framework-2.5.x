
package org.springframework.samples.petclinic.hibernate;

import java.util.Collection;
import org.hibernate.SessionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.Clinic;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.PetType;
import org.springframework.samples.petclinic.Vet;
import org.springframework.samples.petclinic.Visit;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of the Clinic interface.
 * <p>
 * The mappings are defined in "petclinic.hbm.xml", located in the root of the
 * class path.
 * <p>
 * Note that transactions are declared with annotations and that some methods
 * contain "readOnly = true" which is an optimization that is particularly
 * valuable when using Hibernate (to suppress unnecessary flush attempts for
 * read-only operations).
 * 
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @since 19.10.2003
 */
@Transactional
public class HibernateClinic implements Clinic {

	@Autowired
	private SessionFactory sessionFactory;


	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public Collection<Vet> getVets() throws DataAccessException {
		return sessionFactory.getCurrentSession().createQuery("from Vet vet order by vet.lastName, vet.firstName").list();
	}

	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public Collection<PetType> getPetTypes() throws DataAccessException {
		return sessionFactory.getCurrentSession().createQuery("from PetType type order by type.name").list();
	}

	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public Collection<Owner> findOwners(String lastName) throws DataAccessException {
		return sessionFactory.getCurrentSession().createQuery("from Owner owner where owner.lastName like :lastName")
				.setString("lastName", lastName + "%").list();
	}

	@Transactional(readOnly = true)
	public Owner loadOwner(int id) throws DataAccessException {
		return (Owner) sessionFactory.getCurrentSession().load(Owner.class, id);
	}

	@Transactional(readOnly = true)
	public Pet loadPet(int id) throws DataAccessException {
		return (Pet) sessionFactory.getCurrentSession().load(Pet.class, id);
	}

	public void storeOwner(Owner owner) throws DataAccessException {
		// Note: Hibernate3's merge operation does not reassociate the object
		// with the current Hibernate Session. Instead, it will always copy the
		// state over to a registered representation of the entity. In case of a
		// new entity, it will register a copy as well, but will not update the
		// id of the passed-in object. To still update the ids of the original
		// objects too, we need to register Spring's
		// IdTransferringMergeEventListener on our SessionFactory.
		sessionFactory.getCurrentSession().merge(owner);
	}

	public void storePet(Pet pet) throws DataAccessException {
		sessionFactory.getCurrentSession().merge(pet);
	}

	public void storeVisit(Visit visit) throws DataAccessException {
		sessionFactory.getCurrentSession().merge(visit);
	}

}
