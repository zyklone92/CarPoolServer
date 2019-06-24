package at.ac.tuwien.ict.andropicar.server.helper;

import at.ac.tuwien.ict.andropicar.server.connections.Connection;

/**
 * The type of operation that the {@link Connection} wants to inform the main-thread about.
 * @author Boeck
 *
 */
public enum EOperationType {
	SOCKET_ERROR, CONNECT, REGISTER, LINK, CLOSE, UPDATE_INFO;
}
