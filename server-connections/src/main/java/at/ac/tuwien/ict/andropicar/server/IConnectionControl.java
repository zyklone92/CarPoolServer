package at.ac.tuwien.ict.andropicar.server;

import java.util.Collection;

import at.ac.tuwien.ict.andropicar.server.helper.CarInformation;


/**
 * IConnectionControl provides methods to allow or disallow linking of certain devices as well as obtain {@link CarInformation}s of all registered cars.
 * 
 * @author Boeck
 */
public interface IConnectionControl {
	
	/**
	 * Allows a {@link at.ac.tuwien.ict.andropicar.server.connections.PhoneConnection} with the specified ID to establish a link with the {@link at.ac.tuwien.ict.andropicar.server.connections.CarConnection} with the specified ID.
	 * @param phoneId the ID of the affected phone.
	 * @param carId the ID of the affected car.
	 */
	void allowLink(String phoneId, long carId);
	
	/**
	 * Removes the permission of a {@link at.ac.tuwien.ict.andropicar.server.connections.PhoneConnection} to establish a link with a {@link at.ac.tuwien.ict.andropicar.server.connections.CarConnection} and
	 * terminates any existing links between the specified phone and other devices.
	 * @param phoneId the ID of the affected phone.
	 * @return true, if the current state of this object has changed due to the call of this method, otherwise false.
	 */
	boolean terminateLink(String phoneId);
	
	/**
	 * @return the list of all {@link CarInformation}s of all connected cars.
	 */
	Collection<CarInformation> getCarInformations();

}
