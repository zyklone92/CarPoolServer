package at.ac.tuwien.ict.andropicar.server.connections;

import java.util.HashMap;

public interface IDataListener {
	
	/**
	 * Informs the listener about new data.
	 * @param carId		the cars ID the data-set is for or from.
	 * @param phoneId	the phones ID the data-set is for or from.
	 * @param dataset	the new data-set
	 */
	public void updateListener(long carId, String phoneId, HashMap<String, Object> dataset);

}
