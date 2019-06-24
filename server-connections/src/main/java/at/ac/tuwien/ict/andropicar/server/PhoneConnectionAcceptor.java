package at.ac.tuwien.ict.andropicar.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.ac.tuwien.ict.andropicar.server.connections.PhoneConnection;
import at.ac.tuwien.ict.andropicar.server.helper.EOperationType;
import at.ac.tuwien.ict.andropicar.server.helper.QueueElement;

/**
 * A PhoneConnectionAcceptor listens for new Socket-connections on a specified port. Devices that connect to the server on this port are recognized as phones.<br>
 * Listens to new incoming connections on the given port until a new connection is available. It then sets up the connections {@link Socket}
 * and creates a new {@link PhoneConnection} with it, which is then put onto the {@link PhoneConnectionAcceptor#operationQueue} to inform the main thread.
 * 
 * @author Boeck
 */
public class PhoneConnectionAcceptor extends ConnectionAcceptor {
	
	/** The logger that is used for logging messages. Part of the log4j2 library. */
	private static final Logger logger = LogManager.getLogger(PhoneConnectionAcceptor.class);
	
	
	/**
	 * @param port the port, that the {@link ServerSocket} should try to listen on, first.
	 * @param operationQueue the {@link LinkedBlockingQueue} that is used to inform the main-thread about new incoming connections.
	 */
	public PhoneConnectionAcceptor(int port, LinkedBlockingQueue<QueueElement> operationQueue) {
		super(port, operationQueue);
	}
	
	
	@Override
	/**
	 * Listens to new incoming connections on the given port until a new connection is available. It then sets up the connections {@link Socket}
	 * and creates a new {@link GeneralConnection} with it, which is then put onto the {@link ConnectionAcceptor#operationQueue} to inform the main thread.
	 */
	public void run() {
		setupAcceptor();
		PhoneConnectionAcceptor.logger.info("Starting to listen for incoming connect-requests from phones on port " + getPort() + "...");
		while(super.isKeepRunning()) {
			PhoneConnection newConnection = null;
			(new Thread((newConnection =  new PhoneConnection(super.setupSocket(), super.getOperationQueue())), "PhoneConnection")).start();
			if(!writeToOperationQueue(EOperationType.CONNECT, newConnection)) {
				// this should never happen, since we never interrupt a thread
				PhoneConnectionAcceptor.logger.warn("The accepted phone-connection will not be handled anymore.");
				if(newConnection != null)
					newConnection.finishConnection();
			}
		}
		finishAcceptor();
	}

}
