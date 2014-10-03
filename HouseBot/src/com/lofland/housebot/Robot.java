package com.lofland.housebot;

public class Robot {
	// Robot parameters and status
	private int travelSpeed = 70; // Set the initial speed here
	private int rotateSpeed = 30; // Set the initial speed here
	private int viewAngle = 40; // Set default view angle here
	String[] parts; // I forgot what this was for.
	/*
	 * For Leg Mindstorm UltraSonic sensors,
	 * 255 = out of range or error, so it is a good
	 * default value to assume basically,
	 * "We don't see anything." until we do.
	 * 
	 * In theory another option could be to differentiate
	 * between the Android's default of,
	 * "I've never heard from the sensor",
	 * and the robot's
	 * "I've never seen anything."
	 * But I'm not sure how useful that would be.
	 */
	private int distanceCenter = 255;
	private int distanceLeft = 255;
	private int distanceRight = 255;
	private int heading = 0;
	private volatile boolean connectRequested = false;
	private volatile boolean connectedToNXT = false;
	private String isDoing = "";
	private String lastResult = "";
	private String tilt = "TILT";
	
	/**
	 * @param initialTravelSpeed the initial Travel Speed
	 * @param initialRotateSpeed the initial Rotation Speed
	 * @param defaultViewAngle the default View Angle
	 */
	public Robot(int initialTravelSpeed, int initialRotateSpeed, int defaultViewAngle) {
		this.setTravelSpeed(initialTravelSpeed);
		this.setRotateSpeed(initialRotateSpeed);
		this.setViewAngle(defaultViewAngle);
	}
	
	public Robot() {
		
	}
	
	public void setDistanceCenter(int distanceCenter) {
		this.distanceCenter = distanceCenter;
	}
	
	public int getDistanceCenter() {
		return this.distanceCenter;
	}

	public int getTravelSpeed() {
		return travelSpeed;
	}

	public void setTravelSpeed(int travelSpeed) {
		this.travelSpeed = travelSpeed;
	}

	public int getRotateSpeed() {
		return rotateSpeed;
	}

	public void setRotateSpeed(int rotateSpeed) {
		this.rotateSpeed = rotateSpeed;
	}

	public int getViewAngle() {
		return viewAngle;
	}

	public void setViewAngle(int viewAngle) {
		this.viewAngle = viewAngle;
	}

	public int getDistanceLeft() {
		return distanceLeft;
	}

	public void setDistanceLeft(int distanceLeft) {
		this.distanceLeft = distanceLeft;
	}

	public int getDistanceRight() {
		return distanceRight;
	}

	public void setDistanceRight(int distanceRight) {
		this.distanceRight = distanceRight;
	}

	public int getHeading() {
		return heading;
	}

	public void setHeading(int heading) {
		this.heading = heading;
	}

	public boolean isConnectRequested() {
		return connectRequested;
	}

	public void setConnectRequested(boolean connectRequested) {
		this.connectRequested = connectRequested;
	}

	public boolean isConnectedToNXT() {
		return connectedToNXT;
	}

	public void setConnectedToNXT(boolean connectedToNXT) {
		this.connectedToNXT = connectedToNXT;
	}

	public String getIsDoing() {
		return isDoing;
	}

	public void setIsDoing(String isDoing) {
		this.isDoing = isDoing;
	}

	public String getLastResult() {
		return lastResult;
	}

	public void setLastResult(String lastResult) {
		this.lastResult = lastResult;
	}

	public String getTilt() {
		return tilt;
	}

	public void setTilt(String tilt) {
		this.tilt = tilt;
	}

}
