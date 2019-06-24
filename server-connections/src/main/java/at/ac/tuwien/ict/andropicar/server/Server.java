package at.ac.tuwien.ict.andropicar.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.Logger;

import at.ac.tuwien.ict.andropicar.server.connections.CarConnection;
import at.ac.tuwien.ict.andropicar.server.connections.Connection;
import at.ac.tuwien.ict.andropicar.server.connections.PhoneConnection;
import at.ac.tuwien.ict.andropicar.server.helper.CarInformation;
import at.ac.tuwien.ict.andropicar.server.helper.Keywords;
import at.ac.tuwien.ict.andropicar.server.helper.QueueElement;

import org.apache.logging.log4j.LogManager;

/**
 * The main class of this Server.<br>
 * It implements the IConnectionControl interface, which means control of connections is maintained by another component, making this part of the server a trusted environment
 * with the other component deciding which devices to allow connection.
 * Information of allowed links between {@link PhoneConnection}s and {@link CarConnection}s is stored in the {@link #whitelist}.
 * A {@link PhoneConnection} can only be allowed control over exactly one {@link CarConnection} at one point in time.
 * If an entry in the {@link #whitelist} is removed or gets overridden, the affected {@link PhoneConnection} loses control over the car it is currently connected to, if any.<br>
 * <br>
 * TODO This section needs to be updated.
 * Other threads (e.g. the connection acceptor or different connection-type-classes) can inform this class about new events (e.g. new connection, connect-request, etc.) via the operationQueue.<br>
 * It starts a {@link PhoneConnectionAcceptor} and {@link CarConnectionAcceptor} in new threads and waits for new Connections to be accepted.<br>
 * The new connections are stored in a {@link PhoneConnection} or {@link CarConnection} (depending on the ConnectionAcceptor),
 * which are started in a new thread and stored in the list of unidentified devices.<br>
 * Before those connections are able to interact with the server, they need to register themselves with an id to be able to easily identify them.
 * In order for {@link PhoneConnection}s to be able to maintain a connection, they need to be whitelisted via the {@link IConnectionControl}-interface.<br>
 * <br>
 * {@link PhoneConnection}s can send requests to the server to be connected (linked) with or disconnected from a {@link CarConnection},
 * but only if the phone has been granted access control over that car (again via making use of the {@link IConnectionControl}-interface).
 * The server then searches in the list of connected cars for a car with the corresponding ID and links it to the phone,
 * regardless of the previous connection state of the car (a previously connected phone gets disconnected).<br>
 * An existing link between a phone and a car can be terminated at any time by removing the corresponding entry from the {@link #whitelist}.
 * This can be done via the {@link IConnectionControl}-interface.<br>
 * 
 * If a connection is closed, its corresponding thread finishes and it is removed from {@link #identifiedDevices} or {@link #unidentifiedDevices}, whichever applies.<br>
 * 
 * @author Boeck
 *
 */
public class Server implements IConnectionControl {
	
	/** The port, the CarConnectionAcceptor listens on for new connections. */
	private int carAcceptorPort = 6633;
	
	/** The port, the PhoneConnectionAcceptor listens on for new connections. */
	private int phoneAcceptorPort = 6636;
	
	/** The list of devices that have not identified themselves yet. */
	private List<Connection> unidentifiedDevices = new ArrayList<Connection>();
	
	/** The list of devices, that have identified themselves. */
	private List<Connection> identifiedDevices = new ArrayList<Connection>();
	
	/** The operationQueue can be written to, to inform the main-thread about certain events and send information-relevant data. */  
	private LinkedBlockingQueue<QueueElement> operationQueue = new LinkedBlockingQueue<>();
	
	/** The list of all connected cars and their informations. */
	private Collection<CarInformation> carInformations = new ArrayList<>();
	
	/** The list of all links between phones and cars that the server allows. */
	private HashMap<String, Long> whitelist = new HashMap<>();
	
	/** The logger that is used for logging messages. Part of the log4j2 library. */
	private static final Logger logger = LogManager.getLogger(Server.class);
	
	
	/**
	 * @return the port that the CarConnectionAcceptor tries to listen on for new Connections
	 */
	public int getCarAcceptorPort()
	{
		return carAcceptorPort;
	}
	
	/**
	 * @return the port that the PhoneConnectionAcceptor tries to listen on for new Connections
	 */
	public int getPhoneAcceptorPort()
	{
		return phoneAcceptorPort;
	}
	
	/**
	 * 
	 * @param phoneId the id of the {@link at.ac.tuwien.ict.andropicar.server.connections.PhoneConnection PhoneConnection} that should be found.
	 * @return the {@link PhoneConnection} whose id matches the passed one or null, if no match was found.
	 */
	public PhoneConnection findPhone(String phoneId) {
		for(Connection conn : this.identifiedDevices) {
			if(conn instanceof PhoneConnection) {
				if(((PhoneConnection) conn).getId().equals(phoneId))
					return (PhoneConnection) conn;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param carId the id of the {@link at.ac.tuwien.ict.andropicar.server.connections.PhoneConnection CarConnection} that should be found.
	 * @return the {@link CarConnection} whose id matches the passed one or null, if no match was found.
	 */
	public CarConnection findCar(long carId) {
		for(Connection conn : this.identifiedDevices) {
			if(conn instanceof CarConnection) {
				if(((CarConnection) conn).getId() == carId)
					return (CarConnection) conn;
			}
		}
		return null;
	}
	
	/**
	 * Adds a new entry to the {@link #whitelist}, based on the passed parameters.
	 * @param phoneId the phoneId to be whitelisted.
	 * @param carId the carId that the phone will be allowed to take control of.
	 */
	@Override
	public void allowLink(String phoneId, long carId) {
		if(this.findPhone(phoneId) != null && this.whitelist.get(phoneId) != null && this.whitelist.get(phoneId) != carId)
			this.findPhone(phoneId).setLinkedCar(null);
		this.whitelist.put(phoneId, carId);
	}

	/**
	 * Removes an entry from the {@link #whitelist} and terminates any link between a car and the affected phone, if any.
	 * @param phoneId the phoneId whose entry in the {@link #whitelist} should be deleted.
	 * @return true, if an entry for the passed phoneId was removed, otherwise false.
	 */
	@Override
	public boolean terminateLink(String phoneId) {
		if(this.whitelist.remove(phoneId) == null)
			return false;
		for(Connection conn : this.identifiedDevices) {
			if(conn instanceof PhoneConnection) {
				if(((PhoneConnection) conn).getId().equals(phoneId)) {
					((PhoneConnection) conn).setLinkedCar(null);
					((PhoneConnection) conn).finishConnection();
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return the list of all {@link CarInformation}s of all connected cars.
	 */
	@Override
	public Collection<CarInformation> getCarInformations() {
		return this.carInformations;
	}
	
	/**
	 * Adds the passed {@link Connection} to {@link #unidentifiedDevices}, removing any already contained {@link Connection}s with the same device.
	 * @param connection the {@link Connection} to be added to {@link #unidentifiedDevices}.
	 */
	private void addUnidentifiedDevice(Connection connection) {
		this.unidentifiedDevices.remove(connection);
		this.unidentifiedDevices.add(connection);
		if(connection instanceof PhoneConnection)
			Server.logger.info("Added a new PhoneConnection to the list of unidentified devices.");
		if(connection instanceof CarConnection)
			Server.logger.info("Added a new CarConnection to the list of unidentified devices.");
	}
	
	/**
	 * Takes a {@link Connection} and adds it to the list of identified devices.
	 * {@link PhoneConnection}s can only be added, if an entry in the whitelist exists for that phone,
	 * otherwise the registration will be rejected and the connection closed.<br>
	 * The registering device is informed, if the process has been successful or not.
	 * @param connection The {@link Connection} to be registered.
	 */
	private void identifyDevice(Connection connection) {
		if(connection instanceof PhoneConnection) {
			PhoneConnection phone = (PhoneConnection) connection;
			// only add the phone if it is whitelisted
			if(this.whitelist.containsKey(phone.getId())) {
				this.identifiedDevices.add(connection);
				Server.logger.info("Added the phone to the list of connected phones.");
				// registering was successful, informing the client
				phone.addToOutputMessageMap(Keywords.state, 1);
			}
			else {
				phone.addToOutputMessageMap(Keywords.state, -1);
				phone.finishConnection();
				return;
			}
			
		}
		else if(connection instanceof CarConnection) {
			if(this.findCar(((CarConnection) connection).getId()) == null) {
				this.identifiedDevices.add(connection);
				connection.addToOutputMessageMap(Keywords.state, 1);
				Server.logger.info("Added the car to the list of connected cars.");
			}
			else {
				connection.addToOutputMessageMap(Keywords.state, -1);
				Server.logger.info("A car with a duplicate ID tried to connect.");
				return;
			}
		}
		else {
			connection.addToOutputMessageMap(Keywords.state, -1);
		}
		if(this.unidentifiedDevices.remove(connection))
			Server.logger.info("Removed aformentioned device from list of unidentified devices");
	}
	
	/**
	 * Tries to establish a link between a {@link PhoneConnection} and a {@link CarConnection}.<br>
	 * The passed connection has to be the {@link PhoneConnection} that requested control over a car.
	 * If that requirement is met, and the {@link PhoneConnection} has permission to be linked with the car,
	 * the corresponding {@link CarConnection} is searched for in the list of {@link #identifiedDevices}.
	 * If the car is found, a link between the two devices is established.<br>
	 * The {@link PhoneConnection} is informed of the success of the operation.
	 * @param connection the {@link PhoneConnection} to be linked to a car.
	 */
	private void linkDevices(Connection connection) {
		if(connection instanceof PhoneConnection)
		{
			PhoneConnection phone = ((PhoneConnection)connection);
			Server.logger.info("A phone wants to be connected to a car with and ID of " + phone.getLinkRequestId());
			if(this.whitelist.get(phone.getId()) != phone.getLinkRequestId()) {
				phone.addToOutputMessageMap(Keywords.state, 0);
				Server.logger.info("Phone is not whitelisted to be connected with that car");
				return;
			}
			boolean found = false;
			for(Connection conn : this.identifiedDevices)
			{
				if(!(conn instanceof CarConnection))
					continue;
				CarConnection car = (CarConnection) conn;
				if(phone.getLinkRequestId() == car.getId())
				{
					car.setLinkedPhone(phone);
					phone.setLinkedCar(car);
					phone.addToOutputMessageMap(Keywords.state, 2);
					found = true;
					Server.logger.info("Found the requested car and connected the phone to it.");
					break;
				}
			}
			if(found == false)
			{
				Server.logger.info("The car with an ID of " + phone.getLinkRequestId() + ", is not connected at the moment.");
				phone.addToOutputMessageMap(Keywords.state, 0);
			}
		}
	}
	
	/**
	 * Removes the passed {@link Connection} either from {@link #unidentifiedDevices} or {@link #identifiedDevices}, whichever list contains the element.
	 * @param connection the connection to be removed from the lists of connected devices.
	 */
	private void removeDevice(Connection connection) {
		if(this.unidentifiedDevices.contains(connection)) {
			Server.logger.warn("An unknown device just got disconnected.");
			this.unidentifiedDevices.remove(connection);
			return;
		}
		else if(this.identifiedDevices.remove(connection)) {
			if(connection instanceof PhoneConnection)
				Server.logger.warn("A phone just got disconnected.");
			else if(connection instanceof CarConnection)
				Server.logger.warn("A car just got disconnected.");
		}
		else {
			Server.logger.warn("A request to remove a connected device could not be executed, because the device was not in any list of connected devices.");
		}
	}
		
	/**
	 * Checks if the passed {@link Connection} is an instance of a {@link CarConnection} and updates {@link #carInformations} with the passed {@link CarConnection}s car information, if so.
	 * If {@link #getCarInformations()} contains a {@link CarInformation} with the same id as the new one, it gets overridden.
	 * @param connection the connection that should be checked for a new {@link CarInformation}.
	 */
	private void updateCarInformations(Connection connection) {
		if(connection == null || !(connection instanceof CarConnection))
			return;
		CarInformation carInfo = ((CarConnection) connection).getCarInformation();
		this.carInformations.remove(carInfo);
		this.carInformations.add(carInfo);
	}
	
	/**
	 * Takes a {@link QueueElement} and processes its {@link Connection} based on the {@link at.ac.tuwien.ict.andropicar.server.helper.EOperationType}.
	 * @param nextOperation the {@link QueueElement} to be processed.
	 */
	private void processQueueElement(QueueElement nextOperation) {
		if(nextOperation == null)
			return;
		switch(nextOperation.getOperationType()) {
		case SOCKET_ERROR:
			Server.logger.fatal("Server could not be started due the inability to bind a ServerSocket to a port.\nExiting...\n");
			System.exit(-1);
		case CONNECT:
			addUnidentifiedDevice(nextOperation.getConnection());
			break;
		case REGISTER:
			identifyDevice(nextOperation.getConnection());
			break;
		case LINK:
			linkDevices(nextOperation.getConnection());
			break;
		case CLOSE:
			removeDevice(nextOperation.getConnection());
			break;
		case UPDATE_INFO:
			updateCarInformations(nextOperation.getConnection());
			break;
		}
	}
	
	/**
	 * see class documentation for more information
	 */
	public void run()
	{
		Server.logger.info("Starting Connection-acceptors...");
		(new Thread(new CarConnectionAcceptor(this.carAcceptorPort, this.operationQueue), "CarConnection Acceptor")).start();
		Server.logger.info("Car-Connection-Acceptor successfully started, listening for new car-connections.");
		(new Thread(new PhoneConnectionAcceptor(this.phoneAcceptorPort, this.operationQueue), "PhoneConnection Acceptor")).start();
		Server.logger.info("Phone-Connection-Acceptor successfully started, listening for new phone-connections.");
		QueueElement nextOperation = null;
		
		while(true) {
			// wait for new queue-element and process it
			try {
				nextOperation = operationQueue.take();
			} catch(InterruptedException ie) {
				ie.printStackTrace();
			}
			
			if(nextOperation == null)
				continue;
			
			processQueueElement(nextOperation);
		}
	}
	
	
	/**
	 * Starts the server.
	 * @param args not used
	 */
	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

}
