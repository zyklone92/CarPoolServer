package at.ac.tuwien.ict.andropicar.server.connections;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.ac.tuwien.ict.andropicar.server.helper.CarInformation;
import at.ac.tuwien.ict.andropicar.server.helper.EOperationType;
import at.ac.tuwien.ict.andropicar.server.helper.Keywords;
import at.ac.tuwien.ict.andropicar.server.helper.QueueElement;


/**
 * This class is used to communicate with a car via a TCP-connection (see {@link Connection} class for more information on the basic functionalities).<br>
 * It holds the reference to the {@link PhoneConnection} of a linked phone (if any) in order to be able to forward information such as sensor-data to it.<br>
 * This class provides the means for a linked phone to send data (e.g. control-data) to the car.
 * 
 * @author Boeck
 */
public class CarConnection extends Connection
{	
	/** This cars ID. */
	private long id;
	
	/** The cars informations. */
	private CarInformation carInfo;
	
	/** The phone that this car is linked to through the server. */
	private volatile PhoneConnection linkedPhone = null;
	
	/** The logger that is used for logging messages. Part of the log4j2 library. */
	private static final Logger logger = LogManager.getLogger(CarConnection.class);

	
	/**
	 * Used to initialize this class with a {@link Socket}, this cars ID and a {@link LinkedBlockingQueue} for communicating with the main-thread. 
	 * @param connection the {@link Socket} for this class. Needs to be pre-configured (priority, read-timeout, etc.),
	 * since this class does not provide means to configure the {@link Socket}.
	 * @param operationQueue this {@link LinkedBlockingQueue} is used to communicate with the main-thread.
	 */
	public CarConnection(Socket connection, LinkedBlockingQueue<QueueElement> operationQueue)
	{
		super(connection, operationQueue);
	}


	/**
	 * @return the cars ID.
	 */
	public long getId() { return this.id; }

	/**
	 * @return the linked Phone.
	 */
	public PhoneConnection getLinkedPhone() { return linkedPhone; }
	
	/**
	 * @return the cars informations.
	 */
	public CarInformation getCarInformation() { return this.carInfo; }

	/**
	 * Sets the linked Phone to the given parameter, therefore terminating any link that might already be established.<br>
	 * If the {@link #linkedPhone}s state is currently not null, it is informed about the termination of the link.
	 * If the {@link #linkedPhone}s state is going to be set to null, the car itself is informed about the termination of the link.
	 * @param linkedPhone the {@link PhoneConnection} that {@link #linkedPhone} should be set to.
	 */
	public void setLinkedPhone(PhoneConnection linkedPhone) {
		if(this.linkedPhone != null)
			// tell the currently connected phone, that it is no longer connected (new phone will be the controlling one)
			this.linkedPhone.addToOutputMessageMap(Keywords.state, 0);
		this.linkedPhone = linkedPhone;
		if(this.linkedPhone == null)
			this.addToOutputMessageMap(Keywords.stop, 1);
		else
			this.linkedPhone.addToOutputMessageMap(Keywords.state, 2);
	}
	
	/**
	 * {@inheritDoc}<br>
	 * Also, the linked phone (if any) is informed about the termination of the link, by setting its linked car to null.
	 */
	protected void closeConnection() {
		if(this.linkedPhone != null)
		{
			this.linkedPhone.setLinkedCar(null);
			this.setLinkedPhone(null);
		}
		super.closeConnection();
	}
	
	protected void updateListeners(HashMap<String, Object> decodedDataset) {
		for(IDataListener listener : this.getListeners())
			listener.updateListener(this.id, this.linkedPhone.getId(), decodedDataset);
	}
	
	/**
	 *  Processes all sensor-related data and puts it into the HashMap that is being forwarded to the linked phone.
	 * @param decodedDataset the HashMap that contains the message that is to be searched for sensor-data.
	 */
	private void processSensorData(HashMap<String, Object> decodedDataset) {
		Object cache;
		if((cache = decodedDataset.get(Keywords.ultrasonicSensor)) instanceof Long) {
			this.addToForwardMessageMap(Keywords.ultrasonicSensor, (Long) cache);
        }
        if((cache = decodedDataset.get(Keywords.leftInfraredSensor)) instanceof Long) {
        	this.addToForwardMessageMap(Keywords.leftInfraredSensor, (Long) cache);
        }
        if((cache = decodedDataset.get(Keywords.rightInfraredSensor)) instanceof Long) {
        	this.addToForwardMessageMap(Keywords.rightInfraredSensor, (Long) cache);
        }
        if((cache = decodedDataset.get(Keywords.hallSensor)) instanceof Long) {
        	this.addToForwardMessageMap(Keywords.hallSensor, (Long) cache);
        }
	}
	
	/**
	 * Searches through a list of Strings and updates {@link #carInfo}, if it contains certain keywords.
	 * @param infos the list of String to be searched through.
	 */
	private void processCarInformation(List<String> infos){
		for(String info : infos) {
			switch(info) {
				case "camera": this.carInfo.setCamera(true);
				break;
				case "lights": this.carInfo.setLights(true);
					break;
				case "winkers": this.carInfo.setWinkers(true);
					break;
				case Keywords.ultrasonicSensor: this.carInfo.setFrontDistanceSensor(true);
					break;
				case Keywords.leftInfraredSensor: this.carInfo.setLeftDistanceSensor(true);
					break;
				case Keywords.rightInfraredSensor: this.carInfo.setRightDistanceSensor(true);
					break;
				case Keywords.hallSensor: this.carInfo.setVelocitySensor(true);
					break;
			}
		}
	}
	
	@Override
	protected Long register() {
		Object id = null;
		while(!((id = super.register()) instanceof Long))
			if(CarConnection.logger.isWarnEnabled())
				CarConnection.logger.warn("Received id in a wrong format. Trying again.");
		return (long) id;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Registers the car with the server and then processes all incoming information.<br>
	 * The runnable first waits until the car has successfully identified itself.<br>
	 * It then continuously waits for data from the car and processes it as long as the closeConnection-flag is not set.
	 * If the car sends information about the cars capabilities, {@link #carInformation} is updated and the main-thread informed.
	 * If a phone is linked to this car, all data-sets destined for the phone (e.g. sensor-data) are forwarded to it.
	 */
	public void run() {
		
		this.id = register();
		this.carInfo = new CarInformation(id, super.getIpAddress());
		// registering was successful, informing the client
		addToOutputMessageMap(Keywords.state, 1);
				
		while(!super.getCloseConnection()) {
			
			HashMap<String, Object> decodedDataset = null;
			decodedDataset = readFromStreamToHashMap();
			if(decodedDataset != null) {
				Object cache;
				// process incoming message
				
				if((cache = decodedDataset.get(Keywords.properties)) instanceof List) {
					this.processCarInformation((List<String>) cache);
					super.writeToOperationQueue(EOperationType.UPDATE_INFO);
				}
				
				if(this.linkedPhone != null) {
					if((cache = decodedDataset.get(Keywords.phoneControl)) instanceof Long){
						addToForwardMessageMap(Keywords.phoneControl, (Long) cache);
					}
					processSensorData(decodedDataset);
				}
				
				updateListeners(decodedDataset);
				forwardMessageToDevice(this.linkedPhone);
			}
			super.sendOutputMessageMap();
		}
		
		super.sendOutputMessageMap();
		closeConnection();
	}

}
