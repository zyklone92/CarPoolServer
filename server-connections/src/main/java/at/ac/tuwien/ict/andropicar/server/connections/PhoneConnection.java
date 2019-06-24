package at.ac.tuwien.ict.andropicar.server.connections;

import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.ac.tuwien.ict.andropicar.server.helper.EOperationType;
import at.ac.tuwien.ict.andropicar.server.helper.Keywords;
import at.ac.tuwien.ict.andropicar.server.helper.QueueElement;
import at.ac.tuwien.ict.andropicar.json.JSONDecoder;


/**
 * This class is used to communicate with a phone via a TCP-connection (see {@link Connection} class for more information on the basic functionalities).<br>
 * It holds the reference to the {@link CarConnection} of a linked car (if any) in order to be able to forward information such as control-data to it.
 * It is also able to request the establishment of a link between this phone and a car.<br>
 * This class provides the means for a linked car to send data (e.g. sensor-data) to the phone.
 * 
 * @author Boeck
 */
public class PhoneConnection extends Connection implements Runnable {
	
	/** The phones ID. */
	private String id = null;
	
	/** The car that this phone is linked to through the server. */
	private volatile CarConnection linkedCar = null;
	
	/** Contains the cars ID that this phone wants to be linked to, or 0. */
	private volatile long linkRequestId = 0;
	
	/** The logger that is used for logging messages. Part of the log4j2 library. */
	private static final Logger logger = LogManager.getLogger(PhoneConnection.class);
	
	
	/**
	 * Used to initialize this class with a {@link Socket} and a {@link LinkedBlockingQueue} for communicating with the main-thread. 
	 * @param connection the {@link Socket} for this class. Needs to be pre-configured (priority, read-timeout, etc.),
	 * since this class does not provide means to configure the {@link Socket}.
	 * @param operationQueue this {@link LinkedBlockingQueue} is used to communicate with the main-thread.
	 */
	public PhoneConnection(Socket connection, LinkedBlockingQueue<QueueElement> operationQueue) {
		super(connection, operationQueue);
	}

	
	/**
	 * @return the phones ID.
	 */
	public String getId() { return this.id; }
	
	/**
	 * @return Returns the cars ID that this phone wants to connect to, or 0 if there has not been any request yet.
	 */
	public long getLinkRequestId() { return linkRequestId; }
	
	/**
	 * @return Returns the {@link CarConnection} that this phone is linked to, or null.
	 */
	public CarConnection getLinkedCar() { return linkedCar; }
	
	/**
	 * Sets the {@link CarConnection}, to link this device to the given car.
	 * @param linkedCar the {@link CarConnection} to link to.
	 */
	public void setLinkedCar(CarConnection linkedCar) {
		if(linkedCar == null && this.linkedCar != null) {
			this.linkedCar.setLinkedPhone(null);
			this.addToOutputMessageMap(Keywords.state, 0);
			return;
		}
		this.linkedCar = linkedCar;
	}
	
	/**
	 * {@inheritDoc}<br>
	 * Also, the linked car (if any) is informed about the termination of the link, by setting its linked phone to null.
	 */
	protected void closeConnection() {
		if(this.linkedCar != null)
		{
			this.linkedCar.setLinkedPhone(null);
			this.linkedCar = null;
		}
		super.closeConnection();
	}
	
	protected void updateListeners(HashMap<String, Object> decodedDataset) {
		//inform listeners about incoming message
		for(IDataListener listener : super.getListeners())
			listener.updateListener(this.linkedCar.getId(), this.id, decodedDataset);
	}
	
	/**
	 * If the passed argument can be parsed to a long and is greater than 0, {@link #linkRequestId} is set accordingly and the main-thread is informed about the request.
	 * If it is -1, the current link, if any, is terminated.
	 * @param carId the cars ID to be linked to, or -1 to terminate an existing link.
	 */
	private void processConnectRequest(Object carId) {
		long id = 0;
		try {
			id = ((Long)carId).intValue();
		} catch(ClassCastException cce) { return; }
		
		if(id > 0) {
			// phone wants to be linked to a car
			this.linkRequestId = id;
			super.writeToOperationQueue(EOperationType.LINK);
		}
		else if(id == -1 && this.linkedCar != null) {
			// phone wants to terminate a link to the car
			this.linkedCar.setLinkedPhone(null);
			this.linkedCar = null;
		}
	}
	
	/**
	 * Processes all parts of an incoming message that should be forwarded to the linked car, if any, and forwards them accordingly.
	 * @param decodedDataset the HashMap that contains the message.
	 */
	private void processRemainingData(HashMap<String, Object> decodedDataset) {
		if(linkedCar == null)
			return;
		Object cache;
		// look for key-value-pairs in the decodedDataset, that should be forwarded and put them in the forwardMessageMap
		for(String keyword : Keywords.forwardKeywords)
			if((cache = decodedDataset.get(keyword)) instanceof Long)
				this.addToForwardMessageMap(keyword, (long) cache);
	}
	
	@Override
	protected String register() {
		Object id = null;
		while(!((id = super.register()) instanceof String))
			if(PhoneConnection.logger.isWarnEnabled())
				PhoneConnection.logger.warn("Received id in a wrong format. Trying again.");
		return (String) id;
	}
	
	
	@Override
	/**
	 * Registers the phone with the server and then processes all incoming information.<br>
	 * The runnable first waits until the phone has successfully identified itself.<br>
	 * It then continuously waits for data from the car and processes it as long as the closeConnection-flag is not set.
	 * If the car sends a request to be linked to a car, the main-thread is informed to process the request.
	 * If a car is linked to this phone, all data-sets destined for the car (e.g. control-data) are forwarded to it.
	 */
	public void run() {
		
		this.id = register();
		
		while(!super.getCloseConnection()) {
			
			String inputMessage = this.readFromStream();
			if(inputMessage != null) {
				if(PhoneConnection.logger.isDebugEnabled())
					PhoneConnection.logger.debug("Incoming message from a phone: " + inputMessage);
				//forwardMessageMap = new HashMap<String, Object>();
				HashMap<String, Object> decodedDataset = JSONDecoder.decode(inputMessage);
				Object cache;

				// this is the connect request data
				if((cache = decodedDataset.get(Keywords.connect)) instanceof Long) {
					processConnectRequest(cache);
				}
				// this is to request certain information
				else if((cache = decodedDataset.get(Keywords.request)) instanceof String) {
					// not needed yet
				}

				if(this.linkedCar != null) {
					processRemainingData(decodedDataset);
					forwardMessageToDevice(this.linkedCar);
				}
				updateListeners(decodedDataset);
			}
			super.sendOutputMessageMap();
		}
		
		super.sendOutputMessageMap();
		closeConnection();
	}
	
}
