package com.lofland.housebot;

// From package lejos.android;

/**
 * Use this enum in both the RCNavigationControl program on the PC and the
 * RCNavigator that runs on the NXT
 * 
 * @author Roger Glassey
 */
public enum Command {
	/*
	 * NOTE: You must have the SAME number of ENUMS here in the SAME ORDER as in
	 * the program on the robot!
	 */
	EMPTY, STATUS, PROCEED, FORWARD, LEFT, RIGHT, BACKWARD, STOP, BEHAVE, PING, TEST, VIEWANGLE, STAYCLOSE, RESET, FINDCLEARRIGHT, FINDCLEARLEFT, ROTATETOA, ROTATETOC, CALIBRATE;
	// commands to be transmitted to the NXT
}
