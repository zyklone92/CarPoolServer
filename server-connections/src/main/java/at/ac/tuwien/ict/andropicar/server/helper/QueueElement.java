package at.ac.tuwien.ict.andropicar.server.helper;

import at.ac.tuwien.ict.andropicar.server.connections.Connection;

/**
 * This is a Tuple that is used as part of the {@link at.ac.tuwien.ict.andropicar.server.Server}s operation-queue. It contains the {@link EOperationType} and the {@link Connection} to be processed.<br>
 * 
 * @author Boeck
 *
 */
public class QueueElement {

	/** The type of operation that the {@link #connection} would like to inform the main-thread about. */
	private EOperationType operationType;
	
	/** The device that wants to inform the main-thread about something. */
	private Connection connection;
	
	/**
	 * Instantiates a new object of this class, with the set parameters.
	 * @param operationType a value that represents the receiving end what to do with the connection
	 * @param connection the device that wants to inform the receiving end about something
	 */
	public QueueElement(EOperationType operationType, Connection connection) {
		this.operationType = operationType;
		this.connection = connection;
	}
	
	/**
	 * @return the type of operation that the {@link #connection} would like to inform the main-thread about.
	 */
	public EOperationType getOperationType() {
		return operationType;
	}

	/**
	 * @return the {@link Connection} of this queue-element.
	 */
	public Connection getConnection() {
		return connection;
	}

}
