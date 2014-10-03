package com.lofland.housebot;

/* Version History:
 * (Use this to know where to know what day to pull backups from to restore ;)
 * 
 */

/* http://www.lejos.org/nxt/nxj/tutorial/Android/Android.htm
 * I put a lot of notes here and there, so check around.
 * Be sure to look in NXTCommAndroid.java
 * 
 * This app is heavily influenced by Jacek Fedorynski's NXTRemoteControl for which he has released the source code
 * under Apache license.
 * 
 * If you run the Android Lint you will get errors about pccomm.jar calling APIs that are not in Android,
 * and you will have to suppress them.
 * I chose to suppress them "in this file" so it won't affect future error checking.
 * 
 * If you do a validate in Eclipse you will get this on a lot of Android's XML files:
 * "No grammar constraints (DTD or XML schema) detected for the document."
 * This bugs me because the caution signs obscure very real issues that often crop up in these files,
 * so do this to supress that error:
 * Right click on the project ->Properties->Validation->XML Syntax
 * CHECK the "Enable project specific settings" box
 * Set "No grammar specified" to "ignore"
 * This way you get other important errors about the XML files, and don't screw up your Eclipse settings for
 * XML files in other projects.
 * I *think* that these "per project" settings may also follow your project if you move it
 * 
 * Note: If you ever import an Android project and it is all red and confused,
 * look in properties, Android and make sure the target environment is checked!
 */

/* Status:
 * 
 * My intent is to use this over VNC, so no need for a PC connection app,
 * although if it all gets wonderful some day, maybe that would be next, since Java
 * should be able to talk to Java
 * 
 * The connect button works!
 * Next I should make it do something simple, to see if I can get that far.
 * I would like to emulate the functions of the NXT RemoteControl app, and then add on
 * running custom functions, etc. 
 * Then see if I can make connectivity have status.
 * Also RCNavComms is a cut and paste affair, maybe needs cleaned up, or all functions implemented?
 * or just scrapped?
 * (NOTE: NXTCommAndroid IS SUPPOSED to be a cut and paste affair.)
 */

/* Goals:
 * Connect to NXT in "default" mode and run the required program.
 * List programs on NXT and run one of choosing.
 * Run various programs to perform functions and report status or 'call home' for help when there are problems:
 * stuck, fell over, etc.
 * Have Tablet monitor for uprightness and stop and call for help if it falls over.
 * 
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTConnector;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.lofland.housebot.Command;

public class BotController extends Activity implements OnInitListener {
	/*
	 * My entire "base" class implements "OnInitListener" presumably for the sole purpose of being able to use Text To Speech. I'm not sure that is right.
	 */

	// Why are all of these "public" & static?
	public static ToggleButton connectToggleButton;
	public static TextView distanceText;
	public static TextView distanceLeftText;
	public static TextView distanceRightText;
	public static TextView headingText;
	public static TextView tiltText;
	public static TextView isDoingText;
	public static TextView lastResultText;

	// for Text to Speech
	private TextToSpeech tts;
	private Button btnSpeak;
	private EditText txtText;

	// Robot parameters and status, moving to an object near you!
	private Robot myRobot;
	/*
	 * Defaults for passing to myRobot later: These could also be in a config file or something.
	 */
	private final int DEFAULTVIEWANGLE = 40;
	private final int INITIALTRAVELSPEED = 70;
	private final int INITIALROTATESPEED = 30;

	// For Web Interface
	private final int PORT = 8081;
	private WebServer robotWebServer;

	// For StatusTread
	private StatusThread mStatusThread;

	/*
	 * Use this to tell the Connector what type of connection to make, So that we can use both. In theory we could use an LCP connection to connect BEFORE the NXT Is running the
	 * HouseBot program and run it! If this turns out to be impossible or useless then I should remove this, and use the "no argument" version of nxtConnect.
	 */
	private static enum CONN_TYPE {
		LEJOS_PACKET, LEGO_LCP
	}

	private Handler nxtStatusMessageHandler;
	public static final String BOT_RESPONSE = "rp"; // Key to use in nxtStatusMessageHandler

	// For debug log
	private final static String TAG = "HouseBot";

	/*
	 * Message handler for web service. Prototyping on UIMessageHandler
	 */
	private Handler robotWebServerMessageHandler; // For web service to pass messages here.
	// public static final String WEB_RESPONSE = "wp"; // I don't remember what this is for

	// For the command queue
	// I think volatile is required for it to be thread safe?
	volatile Command nextCommand = Command.EMPTY;
	volatile int nextValueToSend = 0;

	/*
	 * Building the message handler the "old fashioned" way like this can create memory leaks:
	 * http://android-developers.blogspot.in/2009/01/avoiding-memory-leaks.html See Romain
	 * Guy's answer here: https://groups.google.com/forum/#!topic/android-developers/1aPZXZG6kWk
	 */
	// class WebServerMessageHandler extends Handler {
	/*
	 * The full example of how to implement this new much more complicated version is here:
	 * http://stackoverflow.com/questions/17899328/this-handler-class-should-be-static-or-leaks-might-occur-com-test-test3-ui-main
	 * It is annoying because it just seems to add
	 * complexity, but it removes one more lint warning about memory leaks,
	 * and according to my reading above, this is a very real possible source of memory leaks.
	 * 
	 * Here is another example, that also has some notes on rebuilding it when the activity is destroyed on things like a screen rotation:
	 * http://stackoverflow.com/questions/18221593/android-handler-changing-weakreference
	 * 
	 * A brief primer on weak references: https://weblogs.java.net/blog/2006/05/04/understanding-weak-references
	 */
	private static class WebServerMessageHandler extends Handler {
		private final WeakReference<BotController> mTarget;

		public WebServerMessageHandler(BotController context) {
			mTarget = new WeakReference<BotController>((BotController) context);
		}

		public void handleMessage(Message msg) {
			super.handleMessage(msg); // As seen in the example
			// and as told here: http://stackoverflow.com/questions/8837753/handler-in-android It does nothing :)
			/*
			 * I did not understand what this "target" business was about, but if you try to call a function from the outer class, "BotController" in this instance, you will find
			 * that you cannot see it or reference it! You need a context for it to get to it from in here!
			 */
			BotController target = mTarget.get();
			/*
			 * Now we have a reference to our outer class so we can call its functions. NOTE: We may need to check to see if this is NULL before using it?
			 */

			// TODO:
			/*
			 * Now should we use the Command ENUMS? Should we send text versions of them and convert? Or ordinal versions and convert? Or serialize them? Or just use constants?
			 * 
			 * Is this "WEB_REPONSE" token necessary? Is msg.getData().getString... the best way to do this?
			 */
			// String messageFromWebServer = msg.getData().getString(WEB_RESPONSE);
			// Log.d(TAG, "Web server: " + messageFromWebServer);
			CommandType commandType = CommandType.values()[msg.what];
			if (commandType == CommandType.ROBOT) {
				Command commandToSend = Command.values()[msg.arg1];
				/*
				 * OK, before I do anything with this, I'm gonna test it! But I THINK this is the same code used on the robot when it gets an integer, and it probably has some
				 * bounds checking I should copy too ;)
				 */
				Log.d(TAG, commandToSend.name());
				// queueCommandToRobot(Command.PING); // Broken, NO TARGET!
				/*
				 * So now the "queueCommandToRobot()" function does not exist, because this inner class is isolated from the outer class! So we need to find a way to reference it,
				 * viola, the target! :)
				 */
				// See, it works!
				target.queueCommandToRobot(commandToSend);
			} else if (commandType == CommandType.ANDROID) {
				AndroidCommand androidCommandToSend = AndroidCommand.values()[msg.arg1];
				if (androidCommandToSend == AndroidCommand.CAMERA) {
					target.startCamera();
				}
			}
		}
	}

	/*
	 * NOTE: As far as I am aware, this cannot be made static,
	 * because of the need to call the runOnUiThread.
	 * If I am wrong, then this could be made static like WebServerMessageHandler.
	 */
	private class StatusMessageHandler extends Handler {
		//private final WeakReference<BotController> mTarget; // Not used

/*		public StatusMessageHandler(BotController context) { // Not used
			mTarget = new WeakReference<BotController>((BotController) context);
		}
*/
		public void handleMessage(Message msg) {
			super.handleMessage(msg); // Because it is documented this way, even though this does nothing.
			String messageFromRobot = msg.getData().getString(BOT_RESPONSE);
			// Log.d(TAG, messageFromRobot); // Gets a little chatty now
			JSONObject statusJObject;
			// JSON in Android: http://stackoverflow.com/questions/9605913/how-to-parse-json-in-android
			try {
				statusJObject = new JSONObject(messageFromRobot);

				// Place the variables into myRobot

				myRobot.setDistanceCenter(statusJObject
						.getInt("distanceCenter"));
				myRobot.setDistanceLeft(statusJObject.getInt("distanceLeft"));
				myRobot.setDistanceRight(statusJObject.getInt("distanceRight"));
				myRobot.setHeading(statusJObject.getInt("Heading"));
				myRobot.setIsDoing(statusJObject.getString("robotIsDoing"));
				myRobot.setLastResult(statusJObject.getString("lastResult"));
				myRobot.setTilt(statusJObject.getString("tilt"));
			} catch (JSONException e) {
				Log.d(TAG, "JSon Parse Exception");
				// e.printStackTrace();
			}

			// A next step here is to add sensor data from the phone/tablet!
			// http://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi

			// Update the screen in the UI Thread with values we just placed in myRobot
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// stuff that updates ui
					// Forward Distance:
					// distanceCenter = Integer.parseInt(parts[2]);
					TextView distanceText = (TextView) findViewById(R.id.distanceText);
					final int distanceCenter = myRobot.getDistanceCenter();
					if (distanceCenter < 255)
						distanceText.setText("" + distanceCenter);
					else
						distanceText.setText("OOR");
					// Left Distance:
					final int distanceLeft = myRobot.getDistanceLeft();
					TextView distanceLeftText = (TextView) findViewById(R.id.distanceLeftText);
					if (distanceLeft < 255)
						distanceLeftText.setText("" + distanceLeft);
					else
						distanceLeftText.setText("OOR");
					// Right Distance:
					final int distanceRight = myRobot.getDistanceRight();
					TextView distanceRightText = (TextView) findViewById(R.id.distanceRightText);
					if (distanceRight < 255)
						distanceRightText.setText("" + distanceRight);
					else
						distanceRightText.setText("OOR");

					final int heading = myRobot.getHeading();
					String headingString = String.valueOf(heading);
					if (heading < 23) {
						headingString = heading + " (N)";
					} else if (heading < 68) {
						headingString = heading + " (NW)";
					} else if (heading < 113) {
						headingString = heading + " (W)";
					} else if (heading < 158) {
						headingString = heading + " (SW)";
					} else if (heading < 203) {
						headingString = heading + " (S)";
					} else if (heading < 248) {
						headingString = heading + " (SE)";
					} else if (heading < 293) {
						headingString = heading + " (E)";
					} else if (heading < 338) {
						headingString = heading + " (NE)";
					} else {
						headingString = heading + " (N)";
					}
					TextView headingText = (TextView) findViewById(R.id.headingText);
					headingText.setText(headingString);
					TextView isDoingText = (TextView) findViewById(R.id.isDoingText);
					isDoingText.setText(myRobot.getIsDoing());
					TextView lastResultText = (TextView) findViewById(R.id.lastResultText);
					lastResultText.setText(myRobot.getLastResult());
					TextView tiltText = (TextView) findViewById(R.id.tiltText);
					tiltText.setText(myRobot.getTilt());
				}
			});
		}
	}

	// Android calls "onCreate" when the activity is started.
	// Most initialization should go here.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.housebot);
		Log.d(TAG, "onCreate");
		
		/*
		 * TODO: If you need to maintain and pass around context:
		 * http://stackoverflow.com/questions/987072/using-application-context-everywhere
		 */

		// Display start up toast:
		// http://developer.android.com/guide/topics/ui/notifiers/toasts.html
		Context context = getApplicationContext();
		CharSequence text = "ex Nehilo!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		// Display said toast
		toast.show();

		/*
		 *  Turn on WiFi if it is off
		 *  http://stackoverflow.com/questions/8863509/how-to-programmatically-turn-off-wifi-on-android-device
		 */
		WifiManager wifiManager = (WifiManager) this
				.getSystemService(Context.WIFI_SERVICE);
		wifiManager.setWifiEnabled(true);

		// Prevent keyboard from popping up as soon as app launches: (it works!)
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		/*
		 * Keep the screen on, because as long as we are talking to the robot, this needs to be up http://stackoverflow.com/questions/9335908/how-to- prevent-the-screen-of
		 * -an-android-device-to-turn-off-during-the-execution
		 */
		this.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		/*
		 * One or more of these three lines sets the screen to minimum brightness, Which should extend battery drain, and be less distracting on the robot.
		 */
		final WindowManager.LayoutParams winParams = this.getWindow()
				.getAttributes();
		winParams.screenBrightness = 0.01f;
		winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;

		// The "Connect" screen has a "Name" and "Address box"
		// I'm not really sure what one puts in these, and if you leave them
		// empty it works fine.
		// So not 110% sure what to do with these. Can the text just be: ""?
		// Just comment these out, and run the command with nulls?
		// mName = (EditText) findViewById(R.id.name_edit);
		// mAddress = (EditText) findViewById(R.id.address_edit);
		// The NXT Remote Control app pops up a list of seen NXTs, so maybe I
		// need to implement that instead.

		// Text to speech
		tts = new TextToSpeech(this, this);

		btnSpeak = (Button) findViewById(R.id.btnSpeak);

		txtText = (EditText) findViewById(R.id.txtSpeakText);

		// Now we need a Robot object before we an use any Roboty values!
		// We can pass defaults in or use the ones it provides
		myRobot = new Robot(INITIALTRAVELSPEED, INITIALROTATESPEED,
				DEFAULTVIEWANGLE);
		// We may have to pass this robot around a bit. ;)

		// Status Lines
		TextView distanceText = (TextView) findViewById(R.id.distanceText);
		distanceText.setText("???");
		TextView distanceLeftText = (TextView) findViewById(R.id.distanceLeftText);
		distanceLeftText.setText("???");
		TextView distanceRightText = (TextView) findViewById(R.id.distanceRightText);
		distanceRightText.setText("???");
		TextView headingText = (TextView) findViewById(R.id.headingText);
		headingText.setText("???");
		TextView actionText = (TextView) findViewById(R.id.isDoingText);
		actionText.setText("None");

		// Travel speed slider bar
		SeekBar travelSpeedBar = (SeekBar) findViewById(R.id.travelSpeed_seekBar);
		travelSpeedBar.setProgress(myRobot.getTravelSpeed());
		travelSpeedBar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						myRobot.setTravelSpeed(progress);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
				});

		// View angle slider bar
		SeekBar viewAngleBar = (SeekBar) findViewById(R.id.viewAngle_seekBar);
		viewAngleBar.setProgress(myRobot.getViewAngle());
		viewAngleBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				myRobot.setViewAngle(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Log.d(TAG, "View Angle Slider ");
				/*
				 * If this gets sent every few milliseconds with the normal output, then there is no need to send a specific command every time it changes!
				 */
				// sendCommandToRobot(Command.VIEWANGLE);
			}
		});

		// Rotation speed slider bar speedSP_seekBar
		SeekBar rotateSpeedBar = (SeekBar) findViewById(R.id.rotateSpeed_seekBar);
		rotateSpeedBar.setProgress(myRobot.getRotateSpeed());
		rotateSpeedBar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						myRobot.setRotateSpeed(progress);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
				});

		/*
		 * This is where we find the four direction buttons defined So we could define ALL buttons this way, set up an ENUM with all options, and call the same function for ALL
		 * options!
		 * 
		 * Just be sure the "release" only stops the robot on these, and not every button on the screen.
		 * 
		 * Maybe see how the code I stole this form does it?
		 */
		Button buttonUp = (Button) findViewById(R.id.forward_button);
		buttonUp.setOnTouchListener(new DirectionButtonOnTouchListener(
				Direction.FORWARD));
		Button buttonDown = (Button) findViewById(R.id.reverse_button);
		buttonDown.setOnTouchListener(new DirectionButtonOnTouchListener(
				Direction.REVERSE));
		Button buttonLeft = (Button) findViewById(R.id.left_button);
		buttonLeft.setOnTouchListener(new DirectionButtonOnTouchListener(
				Direction.LEFT));
		Button buttonRight = (Button) findViewById(R.id.right_button);
		buttonRight.setOnTouchListener(new DirectionButtonOnTouchListener(
				Direction.RIGHT));

		// button on click event
		btnSpeak.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				speakOut();
			}

		});

		/*
		 * This causes the typed text to be spoken when the Enter key is pressed.
		 */
		txtText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					// Perform action on key press
					// Toast.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
					speakOut();
					return true;
				}
				return false;
			}
		});

		// Connect button
		connectToggleButton = (ToggleButton) findViewById(R.id.connectToggleButton);
		connectToggleButton
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							// The toggle is enabled
							Log.d(TAG, "Connect button Toggled On!");
							myRobot.setConnectRequested(true);
						} else {
							// The toggle is disabled
							Log.d(TAG, "Connect button Toggled OFF.");
							myRobot.setConnectRequested(false);
						}
					}
				});

		if (nxtStatusMessageHandler == null)
			nxtStatusMessageHandler = new StatusMessageHandler();

		mStatusThread = new StatusThread(context, nxtStatusMessageHandler);
		mStatusThread.start();

		// Start the web service!
		Log.d(TAG, "Start web service here:");
		// Initiate message handler for web service
		if (robotWebServerMessageHandler == null)
			robotWebServerMessageHandler = new WebServerMessageHandler(this); // Has to include "this" to send context, see WebServerMessageHandler for explanation

		robotWebServer = new WebServer(context, PORT,
				robotWebServerMessageHandler, myRobot);

		try {
			robotWebServer.start();
		} catch (IOException e) {
			Log.d(TAG, "robotWebServer failed to start");
			//e.printStackTrace();
		}

		// TODO:
		/*
		 * See this reference on how to rebuild this handler in onCreate if it is gone: http://stackoverflow.com/questions/18221593/android-handler-changing-weakreference
		 */
		// TODO - Should I stop this thread in Destroy or some such place?

		Log.d(TAG, "Web service was started.");

	}

	@Override
	public void onDestroy() {
		// Don't forget to shutdown tts!
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}

		myRobot.setConnectRequested(false);

		if (robotWebServer != null)
			robotWebServer.stop();

		super.onDestroy();
	}

	@Override
	public void onInit(int status) {

		if (status == TextToSpeech.SUCCESS) {

			int result = tts.setLanguage(Locale.US);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "This Language is not supported");
			} else {
				btnSpeak.setEnabled(true);
				speakOut();
			}
		} else {
			Log.e("TTS", "Initilization Failed!");
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void queueCommandToRobot(Command commandToSend) {
		/*
		 * Overload for sending "no" command, it will just call the next one with a 0 for the value
		 */
		queueCommandToRobot(commandToSend, 0);
	}

	public void queueCommandToRobot(Command commandToSend, int valueToSend) {
		if (!myRobot.isConnectedToNXT()) {
			// Don't queue commands when not connected
			Context context = getApplicationContext();
			Toast robotMessageToast = Toast.makeText(context, "Not Connected!",
					Toast.LENGTH_SHORT);
			// Display the toast
			robotMessageToast.show();
		} else if (nextCommand == Command.EMPTY) {
			// If "buffer" is empty, fill it with this
			nextCommand = commandToSend;
			nextValueToSend = valueToSend;
			/*
			 * The toast is too slow and gets way behind. Context context = getApplicationContext(); Toast robotMessageToast = Toast.makeText(context, commandToSend.toString(),
			 * Toast.LENGTH_SHORT); // Display the toast robotMessageToast.show();
			 */
		} else if (commandToSend == Command.STOP) {
			// Let STOP override other commands!
			nextCommand = commandToSend;
			nextValueToSend = valueToSend;
			Context context = getApplicationContext();
			Toast robotMessageToast = Toast.makeText(context, "OVERRIDE!",
					Toast.LENGTH_SHORT);
			// Display the toast
			robotMessageToast.show();
		} else {
			// Otherwise consider buffer full and system is "BUSY"
			// Essentially command is discarded
			Context context = getApplicationContext();
			Toast robotMessageToast = Toast.makeText(context, "BUSY!",
					Toast.LENGTH_SHORT);
			// Display the toast
			robotMessageToast.show();
		}
	}

	protected void onPause() {
		Log.d(TAG, "onPause called");
		super.onPause();
		// I've gutted this. My testing shows if you pause the app and resume
		// it, the connection stays up fine.
		// Just need to catch if he connection is dead, or maybe put some
		// disconnect in an "onDestroy" or whatever happens when the app really
		// dies.
		/*
		 * Even if the screen is "always on" onPause/onResume get called if you switch apps, So this needs to work.
		 * 
		 * I notice that NXTRemoteControl only uses onStart & onStop, but in the grand scheme of things, I'm not sure if that makes any difference.
		 * 
		 * The LeJOS-Droid app calls communicator all the time without checking if it isn't null, which causes the app to crash if the connection is gone. It even tries to close it
		 * onPause without see if it exists. So I'm checking to ensure it isn't null every time I call it, because the connection CAN die! and pausing kills it.
		 */
		// if (communicator != null) {
		// communicator.end();
		// Interestingly you have to check both,
		// Because a failed connection still leaves a !NULL
		// communicator.getConnector,
		// Though I assume the getNXTInfo would be rather empty?
		/*
		 * if (communicator.getConnector() != null) { try { communicator.getConnector().close(); } catch (Exception e) { Log.e(TAG, "onPause() error closing NXTComm ", e); } }
		 */
		// }
	}

	// @Override <- I don't know what these do, so I'm eliminating them if they
	// aren't required

	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume called");
	}

	/*
	 * This will launch the camera app! And we can use the "background" button to come back here! See "ApiTest" from http://ip-webcam.appspot.com/cheats.html
	 */
	public void handleCameraButton(View v) {
		startCamera();
		/*
		 * TODO
		 * Can we bring Housebot to the front automatically after a brief period?
		 * http://stackoverflow.com/questions/12838230/ways-to-bring-an-android-app-in-background-to-foreground
		 */
	}
	
	public void startCamera() {
		// old "return to launcher" intent
		// Intent launcher = new
		// Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);

		// To get it not to run ANOTHER instance of House Bot, use the below two
		// lines for the return
		// intent for housebot
		// From:
		// http://stackoverflow.com/questions/2424488/android-new-intent-starts-new-instance-with-androidlaunchmode-singletop
		// Also added android:launchMode="singleTop" to the manifest
		Intent housebot = new Intent().setClassName(BotController.this,
				BotController.class.getName());
		housebot.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		Intent ipwebcam = new Intent()
				.setClassName("com.pas.webcam", "com.pas.webcam.Rolling")
				// Original
				/*
				 * .putExtra("cheats", new String[] { "set(Photo,1024,768)", // set photo resolution to 1024x768 "set(DisableVideo,true)", // Disable video streaming (only photo
				 * and immediate photo) "reset(Port)", // Use default port 8080 "set(HtmlPath,/sdcard/html/)", // Override server pages with ones in this directory })
				 * .putExtra("hidebtn1", true) // Hide help button .putExtra("caption2", "Run in background") // Change caption on "Actions..." .putExtra("intent2", launcher) //
				 * And give button another purpose .putExtra("returnto", new Intent().setClassName(ApiTest.this,ApiTest.class.getName())); // Set activity to return to
				 */
				// Mine
				.putExtra(
						"cheats",
						new String[] {
								"set(Video,640,480)", // set photo resolution to
														// 1024x768
								"reset(Photo)", "reset(DisableVideo)",
								"set(DisablePhoto,true)",
								"set(DisablePhotoAF,true)",
								"set(DisableImmediatePhoto,true)",
								"set(DisableTorchCtl,true)", "reset(HtmlPath)",
								"reset(Port)", // Use default port 8080
								"set(Awake,true)", "set(Quality,50)" })
				.putExtra("hidebtn1", true) // Hide help button
				.putExtra("caption2", "Run in background") // Change caption on
															// "Actions..."
				.putExtra("intent2", housebot); // And give button another
												// purpose, go to Housebot :)
		// .putExtra("returnto", housebot ); // Also returns to Housebot
		// No, then if I exit HouseBot it goes here and then I get a small loop
		startActivity(ipwebcam);
	}
	
	// TODO: Turn all of these "handle..." functions into something pretty like the arrow buttons use.

	public void handleTestButton(View v) {
		queueCommandToRobot(Command.TEST);
	}

	public void handleProceed(View v) {
		queueCommandToRobot(Command.PROCEED);
	}

	public void handleStayClose(View v) {
		queueCommandToRobot(Command.STAYCLOSE);
	}

	public void handleReset(View v) {
		queueCommandToRobot(Command.RESET);
		/*
		 * Since this is a queue, we cannot disconnect now, or the robot will never get the comand. The sender has to catch this and disconnect.
		 */
	}

	public void handleCalibrate(View v) {
		queueCommandToRobot(Command.CALIBRATE);
	}

	public void handleNW(View v) {
		queueCommandToRobot(Command.ROTATETOC, 315);
	}

	public void handleN(View v) {
		queueCommandToRobot(Command.ROTATETOC, 0);
	}

	public void handleNE(View v) {
		queueCommandToRobot(Command.ROTATETOC, 45);
	}

	public void handleW(View v) {
		queueCommandToRobot(Command.ROTATETOC, 270);
	}

	public void handleE(View v) {
		queueCommandToRobot(Command.ROTATETOC, 90);
	}

	public void handleSW(View v) {
		queueCommandToRobot(Command.ROTATETOC, 225);
	}

	public void handleS(View v) {
		queueCommandToRobot(Command.ROTATETOC, 180);
	}

	public void handleSE(View v) {
		queueCommandToRobot(Command.ROTATETOC, 135);
	}

	public void handleNeg45(View v) {
		queueCommandToRobot(Command.ROTATETOA, -45);
	}

	public void handlePlus360(View v) {
		queueCommandToRobot(Command.ROTATETOA, 360);
	}

	public void handlePlus45(View v) {
		queueCommandToRobot(Command.ROTATETOA, 45);
	}

	public void handleNeg90(View v) {
		queueCommandToRobot(Command.ROTATETOA, -90);
	}

	public void handleNeg360(View v) {
		queueCommandToRobot(Command.ROTATETOA, -360);
	}

	public void handlePlus90(View v) {
		queueCommandToRobot(Command.ROTATETOA, 90);
	}

	public void handleNeg135(View v) {
		queueCommandToRobot(Command.ROTATETOA, -135);
	}

	public void handlePlus180(View v) {
		queueCommandToRobot(Command.ROTATETOA, 180);
	}

	public void handlePlus135(View v) {
		queueCommandToRobot(Command.ROTATETOA, 135);
	}

	/*
	 * This is a new class to listen for touch/release events on buttons to start/stop robot instead of using one to start and one for stop.
	 */
	/*
	 * This needs to mirror closely the sendCommandToRobot(Command.ENUM); function, as it sends commands itself. At some point it would make sense to call that from here instead,
	 * but right now this is simpler.
	 */

	private class DirectionButtonOnTouchListener implements OnTouchListener {
		private Direction direction;

		public DirectionButtonOnTouchListener(Direction d) {
			direction = d;
		}

		public boolean onTouch(View v, MotionEvent event) {
			Log.d(TAG, "DirectionButtonOnTouchListener called ");
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				switch (direction) {
				case FORWARD:
					queueCommandToRobot(Command.FORWARD);
					break;
				case REVERSE:
					queueCommandToRobot(Command.BACKWARD);
					break;
				case LEFT:
					queueCommandToRobot(Command.LEFT);
					break;
				case RIGHT:
					queueCommandToRobot(Command.RIGHT);
					break;
				default:
					queueCommandToRobot(Command.STOP);
					break;
				}
			} else if ((action == MotionEvent.ACTION_UP)
					|| (action == MotionEvent.ACTION_CANCEL)) {
				queueCommandToRobot(Command.STOP);
			}
			return false;
		}
	}

	public void handleStop(View v) {
		queueCommandToRobot(Command.STOP);
	}

	public void handleButtonOne(View v) {
		// First command to use queue!
		queueCommandToRobot(Command.PING);
	}

	/*
	 * Thread to communicate with the NXT This is the heart of this app. It could go in another class, but since the UI exists to do this, so far it makes sense to keep it here.
	 */
	private class StatusThread extends Thread {

		// An NXT Connection to use
		private NXTConnector myNXTConnection;

		// An NXT DataOutputStream to use
		private DataOutputStream dataOut;
		private DataInputStream dataIn;

		private Context context = null;
		private Handler nxtStatusMessageHandler;

		// A BlueTooth adapter for all to use
		private BluetoothAdapter mBluetoothAdapter;

		// Message from robot
		private String stringFromRobot = "EMPTY";

		// Use this to signal the tear down of an existing connection
		private boolean connectionOK = false;
		// Use this to signal the tar down of a failed connection that never worked
		private boolean tearDownConnection = false;

		// Use this to make an internal copy of nextCommand & nextValue
		private Command myCopyOfNextCommand = Command.EMPTY;
		private int myCopyOfNextValueToSend = 0;

		public StatusThread(Context context, Handler nxtStatusMessageHandler) {
			super();
			this.context = context;
			this.nxtStatusMessageHandler = nxtStatusMessageHandler;
			// Get the BlueTooth adapter
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}

		public void run() {
			Looper.prepare();
			Log.d(TAG, "StatusThread started.");
			while (true) {
				if (myRobot.isConnectRequested() && !myRobot.isConnectedToNXT()) {

					Log.d(TAG, "Connection initiated");
					
					tearDownConnection = false;

					if (mBluetoothAdapter == null) {
						Log.d(TAG, "Device does not support Bluetooth!");
						myRobot.setConnectRequested(false); // Turn that back off now that we tried and failed.
						myRobot.setConnectedToNXT(false); // This should be redundant, but just in case . . .
						// "Toggle" the connect button to "OFF"
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// stuff that updates ui
								connectToggleButton.setChecked(false);
							}
						});
					} else {

						/*
						 * Cycle the BlueTooth adaptor off and on, This will save us enormous headaches. The only problem is that it is async, so we may have to watch/loop to see
						 * when it is available.
						 */

						// This requires BLUETOOTH_ADMIN rights in the Manifest!

						// Disable BlueTooth
						if (mBluetoothAdapter.isEnabled()) {
							Log.d("Bluetooth", "Disabling");
							mBluetoothAdapter.disable();

							// Wait for it to be disabled
							do {
								Log.d("Bluetooth", "Waiting for it to go OFF");
								try {
									Thread.sleep(5000);
								} catch (InterruptedException e) {
									Log.d(TAG, "Sleep Interrupted!");
									// e.printStackTrace();
								}
							} while (mBluetoothAdapter.isEnabled());
						}

						/*
						 * Turn on the BlueTooth adapter if it is not on already which it should not be since we just disabled it.
						 */
						if (!mBluetoothAdapter.isEnabled()) {
							Log.d("Bluetooth", "Enabling");
							mBluetoothAdapter.enable(); // Requires BLUETOOTH_ADMIN rights
							/*
							 * This is the OFFICIAL way to do it, but you have to watch for the result in an intent Intent enableBtIntent = new Intent(
							 * BluetoothAdapter.ACTION_REQUEST_ENABLE); startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
							 */
							// Wait for it to be enabled
							do {
								Log.d("Bluetooth", "Waiting for it to go ON");

								// Five second sleep
								try {
									Thread.sleep(5000);
								} catch (InterruptedException e) {
									Log.d(TAG, "Sleep Interrupted!");
									// e.printStackTrace();
								}
							} while (!mBluetoothAdapter.isEnabled());
						}

						/*
						 * It appears that i need to create this folder for NXTConnector: NXTConnector NXJ log:(6489): Failed to write cache file: /sdcard/LeJOS/nxj.cache (No such
						 * file or directory)
						 * 
						 * Reference: http://stackoverflow.com/questions/2130932/how-to-create-directory-automatically-on-sd-card
						 */
						// Create Folder
						// Hard Coded version:
						// File folder = new File("/sdcard/LeJOS");
						/*
						 * I originally hard coded /sdcard/ because the error from NXTConnector said /sdcard/ I do not know if that is hardcoded there or not. But using the
						 * recommended method works on my device at least.
						 */
						// NON Hard Coded version: This DOES work.
						File folder = new File(Environment
								.getExternalStorageDirectory().toString()
								+ "/LeJOS");
						folder.mkdirs();

						// Just make the connection
						// This should be done in a thread
						Log.d("NXT Connect", "Connecting . . .");
						// myNXTConnection = connect(CONN_TYPE.LEJOS_PACKET);

						myRobot.setConnectedToNXT(connectToNXT(CONN_TYPE.LEJOS_PACKET));
						if (!myRobot.isConnectedToNXT())
							tearDownConnection = true;
					}
				}

				if (myRobot.isConnectRequested() && myRobot.isConnectedToNXT()) {
					// Ensure "Connect" button is "ON", since we believe we are connected!
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// stuff that updates ui
							connectToggleButton.setChecked(true);
						}
					});
					try {
						Thread.sleep(200); // Just how fast can we drive this?
						/*
						 * Issues to consider: Network load (BlueTooth) Interference with camera opeartion (CPU) and streamingn (network) CPU load on Android CPU load on NXT
						 */
						/*
						 * 100ms works, but there is some delay. Is this because I am getting ahead of the NXT? or something else, gonna try slower. 150ms is MUCH less delay! The
						 * entire thing is far more responsive. Trying 200ms next.
						 * 
						 * 200
						 * 
						 * The robot has a timer that resets on each read, and is watched by the sensor thread. If it takes too long between reads it disconnects. 10000 is long
						 * enough for it to time out. 8000 is short enough for it to remain stable. Obviously we don't want to be anywhere near this slow, but just for testing,
						 * there it is.
						 */
					} catch (InterruptedException e) {
						Log.d(TAG, "Status Thread sleep failed.");
						//e.printStackTrace();
					}
					/*
					 * Make a copy of nextCommand and nextValue and release them so that they don't move around under us or we don't overwrite something different from what we
					 * took.
					 */
					myCopyOfNextCommand = nextCommand;
					myCopyOfNextValueToSend = nextValueToSend;
					// Command copied, clear buffer for next command!
					nextCommand = Command.EMPTY;

					if (myCopyOfNextCommand == Command.EMPTY) {
						/*
						 * No command to send, so just get status
						 */
						try {
							dataOut.writeInt(Command.STATUS.ordinal()); // convert the enum to an integer
							dataOut.writeInt(0); // Data Point (Right now we only send 1 for all commands
							dataOut.writeInt(myRobot.getTravelSpeed());
							dataOut.writeInt(myRobot.getRotateSpeed());
							dataOut.writeInt(myRobot.getViewAngle());
							dataOut.flush();
						} catch (IOException e2) {
							// e2.printStackTrace();
							Log.d("IOException", "dataOut");
						}
					} else {
						try {
							dataOut.writeInt(myCopyOfNextCommand.ordinal()); // convert the enum to an integer
							dataOut.writeInt(myCopyOfNextValueToSend); // Data Point (Right now we only send 1 for all commands
							dataOut.writeInt(myRobot.getTravelSpeed());
							dataOut.writeInt(myRobot.getRotateSpeed());
							dataOut.writeInt(myRobot.getViewAngle());
							dataOut.flush();
						} catch (IOException e2) {
							// e2.printStackTrace();
							Log.d("IOException", "dataOut");
						}
					}

					/*
					 * Now start a read for status with a timeout The robot should respond to every message with a status message
					 */

					final int TIMEOUT_FOR_ACK = 15;
					TimeLimiter limiter = new SimpleTimeLimiter();
					try {
						stringFromRobot = limiter.callWithTimeout(
								new Callable<String>() {
									public String call() {
										try {
											return dataIn.readUTF();
										} catch (IOException e) {
											Log.d("ACK", "dataIn failed");
											// e.printStackTrace();
											return "dataIn FAIL";
										}
									}
								}, TIMEOUT_FOR_ACK, TimeUnit.SECONDS, false);
					} catch (Exception e2) {
						Log.d("ACK", "Timed out: " + TIMEOUT_FOR_ACK);
						// e2.printStackTrace();
						stringFromRobot = "Timed Out";
					}

					// Log.d("RESULT", stringFromRobot); // Gets very chatty!

					if (stringFromRobot.contains("status")) { // This is a json string, so we can key off of any text we expect to always be in it.
						connectionOK = true;
						// Send command to UI thread
						sendStatusToUIthread(stringFromRobot);
					} else {
						connectionOK = false;
						Log.d(TAG, "CONNECTION FAILED! ABANDON SHIP!");
					}
				}

				// Tear down connection if it is not "OK" or no longer "Requested"
				// or after a failed connection sets "tearDownConnection"
				if ((myRobot.isConnectedToNXT() && (!connectionOK || !myRobot.isConnectRequested())) || tearDownConnection) {
					if (!connectionOK)
						Log.d(TAG, "Tearing down failed connection.");
					if (!myRobot.isConnectRequested())
						Log.d(TAG, "Tearing down connection at user request.");
					if (!tearDownConnection)
						Log.d(TAG, "Tearing down connection that did not start up.");

					if (dataOut != null) { // It will be null if the connection never succeeded.
					Log.d("NXT", "Closing dataOut");
					try {
						dataOut.close();
					} catch (IOException e1) {
						Log.d("NXT", "Close dataOut IOException");
						// e1.printStackTrace();
					}
					}

					if (dataIn != null) { // It will be null if the connection never succeeded.
					Log.d("NXT", "Closing dataIn");
					try {
						dataIn.close();
					} catch (IOException e1) {
						Log.d("NXT", "Close dataIn IOException");
						// e1.printStackTrace();
					}
					}

					if (myNXTConnection != null) { // It will be null if the connection was never actually requested.
					Log.d("NXT", "Closing myNXTConnection");
					try {
						myNXTConnection.close();
					} catch (IOException e) {
						Log.d("NXT", "Close myNXTConnection IOException");
						// e.printStackTrace();
					} catch (NullPointerException npe) { 
						/*
						 * NOTE: IT IS CONSIDERED BAD PRACTICE TO CATCH NULL POINTER EXCEPTIONS!
						 * This is a very handy example, and it does its job, but do NOT do this
						 * in production code!
						 * NOTE: Catching null pointer exceptions can be considered a security
						 * vulnerability in production code!
						 * 
						 * PURPOSE:
						 * If a connection was initiated, but failed, your NXTConnector object will not be null,
						 * nevertheless, it will throw a a null pointer exception when you try to close it!
						 * I presume this is a bug in NXTConnector,
						 * Or, perhaps there is something else that I am supposed to check first?
						 * Or something else I can run that closes a connection?
						 * 
						 * TODO: Write to the leJOS community about this to find out if I am doing something
						 * wrong or if this is a known bug.
						 */
						 Log.d(TAG, "Null Pointer Exception from myNXTConnection.close");
						 myNXTConnection = null;
					 }
					}

					/*
					 * Shut down BlueTooth to conserve power and set robot free. (only do this on dedicated device!)
					 */
					if (mBluetoothAdapter.isEnabled()) {
						Log.d("Bluetooth", "Disabling");
						mBluetoothAdapter.disable();
					}

					myRobot.setConnectedToNXT(false);
					myRobot.setConnectRequested(false); // Make the user request this again if he wants it.
					tearDownConnection = false;
					// "Toggle" the connect button to "OFF"
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// stuff that updates ui
							connectToggleButton.setChecked(false);
						}
					});
				}
			}
			// Looper.loop(); // Unreachable
		}

		private boolean connectToNXT(final CONN_TYPE connection_type) {
			myNXTConnection = new NXTConnector();

			boolean connected = false; // default = NOT connected
			switch (connection_type) { // specifies the connection type i.e. LCP or packet
			case LEGO_LCP:
				connected = myNXTConnection.connectTo("btspp://NXT",
						NXTComm.LCP);
				break;
			case LEJOS_PACKET:
				connected = myNXTConnection.connectTo("btspp://");
				break;
			}
			Log.d("connectToNXT", " connect result " + connected);

			if (!connected) {
				return connected;
			}
			dataIn = myNXTConnection.getDataIn();
			dataOut = myNXTConnection.getDataOut();

			myNXTConnection.setDebug(false); // Maybe make it less chatty?
			return connected;
		}

		private void sendStatusToUIthread(String statusFromNXT) {
			Bundle b = new Bundle();
			b.putString(BOT_RESPONSE, statusFromNXT);
			Message message_holder = new Message();
			message_holder.setData(b);
			nxtStatusMessageHandler.sendMessage(message_holder);
		}

	}

	/*
	 * private class robotWebInterfaceThread extends Thread {
	 * 
	 * This is the start of building a web interface, instead of using VNC, I don't know if it will be too slow or not. I want this thread to be able to put commands on the queue
	 * just like the buttons do, but of course if this is a raging success then the buttons will become obsolete.
	 * 
	 * 
	 * private WebServer server = null; Context context = getApplicationContext();
	 * 
	 * public void run() { while(true) { Thread.yield(); server = new WebServer(context, robotWebServerMessageHandler); server.startServer();
	 * 
	 * Currently the web server just runs and does its own thing, it has its own internal loop. I'm not sure if it is better to find a way for it to populate data that we can pick
	 * up, or if it is better to break the loop, and do the looping here.
	 * 
	 * One concern is that if we do the loop here, the web interface could become unresponsive and produce irratic results. Or maybe not.
	 * 
	 * } } }
	 */
	// For TextToSpeach
	private void speakOut() {
		String text = txtText.getText().toString();
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		txtText.setText("");
	}
}
