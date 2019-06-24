package at.ac.tuwien.ict.andropicar.server.helper;


/**
 * This class is used to store information about a connected car and its properties.
 * @author Boeck
 *
 */
public class CarInformation {
	
	/** The cars id. */
	private long id;
	
	/** The cars ip-address */
	private String ip = null;
	
	/** True if the car is equipped with a camera, otherwise false. */
	private boolean camera = false;
	/** True if the car is equipped with lights, otherwise false. */
	private boolean lights = false;
	/** True if the car is equipped with winkers, otherwise false. */
	private boolean winkers = false;
	/** True if the car is equipped with a front distance-sensor, otherwise false. */
	private boolean frontDistanceSensor = false;
	/** True if the car is equipped with a left-side distance-sensor, otherwise false. */
	private boolean leftDistanceSensor = false;
	/** True if the car is equipped with a right-side distance-sensor, otherwise false. */
	private boolean rightDistanceSensor = false;
	/** True if the car is equipped with a velocity-sensor, otherwise false. */
	private boolean velocitySensor = false;
	
	/**
	 * @param id the cars id.
	 */
	public CarInformation(long id){
		this.id = id;
	} 
	
	/**
	 * 
	 * @param id the cars id.
	 * @param ip the cars ip.
	 */
	public CarInformation(long id, String ip){
		this.id = id;
		this.ip = ip;
	} 
	
	/**
	 * @param id the cars id.
	 * @param hasAll true, if the car has a camera, lights, winkers, front distance-sensors, side distance-sensors and a velocity-sensor, otherwise false.
	 */
	public CarInformation(long id, boolean hasAll){
		this.id = id;
		if(hasAll){
			this.camera = true;
			this.lights = true;
			this.winkers = true;
			this.frontDistanceSensor = true;
			this.leftDistanceSensor = true;
			this.rightDistanceSensor = true;
			this.velocitySensor = true;
		}
	}
	
	/**
	 * 
	 * @param id the cars id.
	 * @param ip the cars ip.
	 * @param camera true if the car is equipped with a camera, otherwise false.
	 * @param lights true if the car is equipped with lights, otherwise false.
	 * @param winkers true if the car is equipped with winkers, otherwise false.
	 * @param frontDistanceSensor true if the car is equipped with a front distance-sensor, otherwise false.
	 * @param leftDistanceSensor true if the car is equipped with left-side distance-sensors, otherwise false.
	 * @param rightDistanceSensor true if the car is equipped with right-side distance-sensors, otherwise false.
	 * @param velocitySensor true if the car is equipped with a velocity-sensor, otherwise false.
	 */
	public CarInformation(long id, String ip, boolean camera, boolean lights, boolean winkers, boolean frontDistanceSensor,
			boolean leftDistanceSensor, boolean rightDistanceSensor, boolean velocitySensor) {
		this.id = id;
		this.ip = ip;
		this.camera = camera;
		this.lights = lights;
		this.winkers = winkers;
		this.frontDistanceSensor = frontDistanceSensor;
		this.leftDistanceSensor = leftDistanceSensor;
		this.rightDistanceSensor = rightDistanceSensor;
		this.velocitySensor = velocitySensor;
	}


	/**
	 * @param id the cars id.
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	/**
	 * @param ip the cars ip.
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * @param camera true if the car is equipped with a camera, otherwise false.
	 */
	public void setCamera(boolean camera) {
		this.camera = camera;
	}

	/**
	 * @param lights true if the car is equipped with lights, otherwise false.
	 */
	public void setLights(boolean lights) {
		this.lights = lights;
	}

	/**
	 * @param winkers true if the car is equipped with winkers, otherwise false.
	 */
	public void setWinkers(boolean winkers) {
		this.winkers = winkers;
	}

	/**
	 * @param frontDistanceSensors true if the car is equipped with a front distance-sensor, otherwise false.
	 */
	public void setFrontDistanceSensor(boolean frontDistanceSensors) {
		this.frontDistanceSensor = frontDistanceSensors;
	}

	/**
	 * @param leftDistanceSensor true if the car is equipped with left-side distance-sensor, otherwise false.
	 */
	public void setLeftDistanceSensor(boolean leftDistanceSensor) {
		this.leftDistanceSensor = leftDistanceSensor;
	}
	
	/**
	 * @param rightDistanceSensor true if the car is equipped with right-side distance-sensor, otherwise false.
	 */
	public void setRightDistanceSensor(boolean rightDistanceSensor) {
		this.rightDistanceSensor = rightDistanceSensor;
	}

	/**
	 * @param velocitySensor true if the car is equipped with a velocity-sensor, otherwise false.
	 */
	public void setVelocitySensor(boolean velocitySensor) {
		this.velocitySensor = velocitySensor;
	}
	
	/**
	 * @return the cars id.
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * @return the cars ip.
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @return true if the car is equipped with a camera, otherwise false.
	 */
	public boolean hasCamera() {
		return camera;
	}

	/**
	 * @return true if the car is equipped with lights, otherwise false.
	 */
	public boolean hasLights() {
		return lights;
	}

	/**
	 * @return true if the car is equipped with winkers, otherwise false.
	 */
	public boolean hasWinkers() {
		return winkers;
	}

	/**
	 * @return true if the car is equipped with a front distance-sensor, otherwise false.
	 */
	public boolean hasFrontDistanceSensor() {
		return frontDistanceSensor;
	}

	/**
	 * @return true if the car is equipped with a left-side distance-sensor, otherwise false.
	 */
	public boolean hasLeftDistanceSensor() {
		return leftDistanceSensor;
	}
	
	/**
	 * @return true if the car is equipped with a right-side distance-sensor, otherwise false.
	 */
	public boolean hasRightDistanceSensor() {
		return rightDistanceSensor;
	}

	/**
	 * @return true if the car is equipped with a velocity-sensor, otherwise false.
	 */
	public boolean hasVelocitySensor() {
		return velocitySensor;
	}
	
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj instanceof CarInformation) {
			if(((CarInformation) obj).id == this.id)
				return true;
		}
		return false;
	}
	
}
