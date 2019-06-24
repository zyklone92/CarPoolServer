package at.ac.tuwien.ict.andropicar.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.ac.tuwien.ict.andropicar.server.connections.Connection;
import at.ac.tuwien.ict.andropicar.server.helper.EOperationType;
import at.ac.tuwien.ict.andropicar.server.helper.QueueElement;

/**
 * A ConnectionAcceptor listens to new incoming TCP-connections on the specified port until a new connection is available. It then sets up the connections {@link Socket}
 * and creates a new {@link Connection} (or subclass thereof) with it, which is then put onto the {@link ConnectionAcceptor#operationQueue}
 * to inform the main thread.
 * 
 * @author Boeck
 */
public abstract class ConnectionAcceptor implements Runnable{	
	
	/** The {@link ServerSocket} that is used to listen for new incoming Connections. */
	private ServerSocket acceptor = null;
	
	/** The port that the {@link ServerSocket} tries to listen on, for new incoming Connections. */
	private int port;
	
	/** The flag, that indicates, if this runnable should continue to be executed or not. */
	private boolean keepRunning = true;
	
	/** The operationQueue can be written to, to inform the main-thread about certain events and send information-relevant data. */
	private LinkedBlockingQueue<QueueElement> operationQueue;
	
	/** The logger that is used for logging messages. Part of the log4j2 library. */
	private static final Logger logger = LogManager.getLogger(ConnectionAcceptor.class);
	
	
	/**
	 * @param port the port, that the {@link ServerSocket} should try to listen on.
	 * @param operationQueue the {@link LinkedBlockingQueue} that is used to inform the main-thread about new incoming connections.
	 */
	public ConnectionAcceptor(int port, LinkedBlockingQueue<QueueElement> operationQueue)
	{
		this.port = port;
		this.operationQueue = operationQueue;
	}
	
	
	/**
	 * @return the port that the ServerSocket tries to listen on, for new incoming Connections.
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * @return the operationQueue can be written to, to inform the main-thread about certain events and send information-relevant data.
	 */
	protected LinkedBlockingQueue<QueueElement> getOperationQueue() {
		return this.operationQueue;
	}
	
	/**
	 * @return true, if this runnable should continue to be executed, othwise false.
	 */
	protected boolean isKeepRunning() {
		return this.keepRunning;
	}
	
	/**
	 * Sets the keepRunning-flag to false, so that the {@link ServerSocket} will be closed and the runnable will finish.
	 */
	public void finishAcceptor()
	{
		keepRunning = false;
	}
	
	/** 
	 * Tries to bind the ServerSocket on the given port.
	 * If it fails to do that 3 times, it tries binding on the next two port 3 times.
	 * If binding on all 3 ports fails, zero is returned to indicate an error.
	 * @return the actual port, that the ServerSocket was able to be bound to, or 0 if an error occured.
	 */
	private int bindAcceptor()
	{
		int currentPort = port;
		int numberOfTries = 1;
		while(acceptor == null)
		{
			try {
				if((numberOfTries%3) == 1)
					ConnectionAcceptor.logger.info("Trying to bind ServerSocket to port " + currentPort);
				acceptor = new ServerSocket(currentPort);
				ConnectionAcceptor.logger.info("Binding successful");
			} catch(IOException ioe) {
				if(numberOfTries%3 == 0)
				{
					numberOfTries = 1;
					if(currentPort == (port + 2))
						return 0;
					else
						currentPort ++;
				}
				else
					numberOfTries ++;
			}
		}
		return currentPort;
	}
	
	/**
	 * Tries to bind the acceptor on a port.
	 * Informs the main-thread, if it is unable to do so and sets keepRunning to false.
	 */
	protected void setupAcceptor() {
		port = bindAcceptor();
		if(port == 0){
			ConnectionAcceptor.logger.error("Unable to bind ServerSocket to a port!");
			try{
				this.operationQueue.put(new QueueElement(EOperationType.SOCKET_ERROR, null));
				this.keepRunning = false;
			} catch(InterruptedException ie){
				ConnectionAcceptor.logger.warn("Unable to put QueueElement on the Operation-Queue.", ie);
			}
			return;
		}
	}
	
	/**
	 * Blocks until the acceptor accepts a new Socket, sets up the Socket and return it.<br>
	 * After being accepted, the Sockets so_timeout is set to 1 ms,
	 * nagles algorithm is deactivated (prevent data-collection),
	 * maximum allowed shutdown time is set to 1 second and
	 * the DSCP-byte is set to prioritize the Sockets data-packets over others.
	 * @return the fully set up Socket.
	 */
	protected Socket setupSocket() {
		Socket newConnection = null;
		while(newConnection == null) {
			try {
				ConnectionAcceptor.logger.info("Listening for incoming connect-requests...");
				
				newConnection = this.acceptor.accept();
				ConnectionAcceptor.logger.info("Accepted new connection. Setting it up for further usage.");
				newConnection.setSoTimeout(1);
				newConnection.setTcpNoDelay(true);	// deactivate nagle's algorithm, which collects data before it actually sends it to the network
				newConnection.setSoLinger(true, 1);	// set maximum allowed time for graceful shutdown
				newConnection.setTrafficClass(112);	// set the ToS-Byte (called DSCP nowadays, ToS is deprecated) to prioritize data
				return newConnection;
			} catch(IOException ioe) {
				newConnection = null;
				ConnectionAcceptor.logger.error("Error accepting new Connection.",ioe);
				ioe.printStackTrace();
			}
		}
		return newConnection;
	}
	
	/**
	 * Creates a new {@link QueueElement} with the passed operation-type and {@link Connection} and puts it on the {@link #operationQueue}.
	 * @param operationType the operationType for the new {@link QueueElement}
	 * @param connection the {@link Connection} fore the new {@link QueueElement}
	 * @return true, if the new {@link QueueElement} has been put on the {@link #operationQueue}, otherwise false.
	 */
	protected boolean writeToOperationQueue(EOperationType operationType, Connection connection) {
		if(connection == null)
			return false;
		try {
			this.operationQueue.put(new QueueElement(operationType, connection));
			return true;
		} catch(InterruptedException ie) {
		// this should never happen, since we never interrupt a thread
			ConnectionAcceptor.logger.error("Unable to put QueueElement on the Operation-Queue.", ie);
		}
		return false;
	}

}
