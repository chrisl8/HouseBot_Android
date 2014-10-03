package com.lofland.housebot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.lofland.housebot.NanoHTTPD.Response.IStatus;
import com.lofland.housebot.NanoHTTPD.Response.Status;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class WebServer extends NanoHTTPD {
	private Context context = null;
	private Handler robotWebServerMessageHandler;
	private Robot myRobot;

	public WebServer(Context context, int port, Handler robotWebServerMessageHandler, Robot myRobot) {
		super(port);
		this.context = context;
		this.robotWebServerMessageHandler = robotWebServerMessageHandler;
		this.myRobot = myRobot;
	}

	// You have to add android.permission.WRITE_EXTERNAL_STORAGE to the manifest for this to work.
	@Override
	public Response serve(IHTTPSession session) {
		Map<String, List<String>> decodedQueryParameters = decodeParameters(session
				.getQueryParameterString());

		String URItext = String.valueOf(session.getUri());
		
		/* The return line is now in EACH if/else
		 * line so that I can control the return type per response
		 */
		/*
		 * Remember if one command CONTAINS another put it second!
		 */

		StringBuilder sb = new StringBuilder();
		if (URItext.contains("hello")) { // Just a test page
			sb.append("<html><head><title>Hello!</title></head><body>Hello to you too!</body></html>");
			return new Response(Status.OK, MIME_HTML, sb.toString());
		} else if (URItext.contains("FORWARD")) {
			sendRobotCommandToUIthread(Command.FORWARD);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("LEFT")) {
			sendRobotCommandToUIthread(Command.LEFT);
			sb.append("Left");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("RIGHT")) {
			sendRobotCommandToUIthread(Command.RIGHT);
			sb.append("Right");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("REVERSE")) {
			sendRobotCommandToUIthread(Command.BACKWARD);
			sb.append("Reverse");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("STOP")) {
			sendRobotCommandToUIthread(Command.STOP);
			sb.append("All Stop");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("DISCONNECT")) {
			//sendAndroidCommandToUIthread(AndroidCommand.CONNECT);
			myRobot.setConnectRequested(false);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("CONNECT")) {
			//sendAndroidCommandToUIthread(AndroidCommand.CONNECT);
			myRobot.setConnectRequested(true);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("CAMERA")) {
			sendAndroidCommandToUIthread(AndroidCommand.CAMERA);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
			/*
		} else if (URItext.contains("FORWARD")) {
			sendCommandToUIthread(Command.FORWARD);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
test.html

		} else if (URItext.contains("FORWARD")) {
			sendCommandToUIthread(Command.FORWARD);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
ping.html

		} else if (URItext.contains("FORWARD")) {
			sendCommandToUIthread(Command.FORWARD);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
proceed.html

		} else if (URItext.contains("FORWARD")) {
			sendCommandToUIthread(Command.FORWARD);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
camera.html

		} else if (URItext.contains("FORWARD")) {
			sendCommandToUIthread(Command.FORWARD);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
stayclose.html

		} else if (URItext.contains("FORWARD")) {
			sendCommandToUIthread(Command.FORWARD);
			sb.append("Roger Roger");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
reset.html
*/
		} else if (URItext.contains("index")) {
			/*
			 * I'm moving this to a file, because converting it to a string is too difficult to maintain.
			 * http://www.javacodegeeks.com/2012/02/android-read-file-from-assets.html
			 */
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				// Put the file in "assets" in Eclipse
				input = assetManager.open("index.html");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, MIME_HTML, input);
		/*
		 * Leaving an example here if you want to handle a file
		 * as text. This converts the input to text
		 * and lets you add to/modify it.
		 * Mostly though I just want to send the input straight
		 * to NanoHTTPD, which is MUCH faster, and in many cases
		 * the only method that actually works.
		 * So this isn't being used much anymore.
		 * So I'm leaving a copy here for reference. 	
		} else if (URItext.contains("lcars.css")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("lcars.css");
				int size = input.available();
				byte[] buffer = new byte[size];
				input.read(buffer);
				input.close();
				String text = new String(buffer);
				sb.append(text);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "text/css", sb.toString());
*/
		} else if (URItext.contains("lcars.css")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("lcars.css");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "text/css", input);
			// JQuery UI Custom CSS
		} else if (URItext.contains("jquery-ui-1.10.4.custom.min.css")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("jquery-ui-1.10.4.custom.min.css");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "text/css", input);
			// middle line graphic
			/* Example
			 * http://stackoverflow.com/questions/17102954/why-images-and-style-files-couldnt-found-on-nanohttpd
			 */
		} else if (URItext.contains("line.png")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("line.png");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "image/png", input);

			// JQuery
		} else if (URItext.contains("jquery-2.1.1.min.js")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("jquery-2.1.1.min.js");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "application/javascript", input);
			// Custom JQuery UI
			// http://jqueryui.com/download/
		} else if (URItext.contains("jquery-ui-1.10.4.custom.min.js")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("jquery-ui-1.10.4.custom.min.js");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "application/javascript", input);
			// JavaScript include file
		} else if (URItext.contains("lcars.js")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("lcars.js");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "application/javascript", input);
			
			// Fonts, because some browsers don't allow cross domain font loading
			// EOT
		} else if (URItext.contains("lcars-webfont.eot")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("lcars-webfont.eot");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, MIME_PLAINTEXT, input);
			// TTF
		} else if (URItext.contains("lcars-webfont.ttf")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("lcars-webfont.ttf");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, MIME_PLAINTEXT, input);
			// SVG
		} else if (URItext.contains("lcars-webfont.svg")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("lcars-webfont.svg");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "image/svg+xml", input);
			// WOFF
		} else if (URItext.contains("lcars-webfont.woff")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				input = assetManager.open("lcars-webfont.woff");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, "application/x-font-woff", input);

		} else if (URItext.contains("status.json")) {
			//int count = 0; // for debugging
			//while (count < 3) {
				//count++;
				//Log.d("WebServer", "" + count);
				/*
				 * The delay here serves 2 purposes:
				 * 1. It demonstrates that NanoHTTPD is multi-threaded,
				 * so that other pages can load even when one
				 * is blocking.
				 * 2. It demonstrate that the index AJAX status
				 * query will wait for a response before starting another.
				 * 
				 * I think what I should do is have this only respond when
				 * the status is different from what it was last time.
				 * Or after a timeout if no change happens within a given
				 * amount of time.
				 * 
				 * Or not. :)
				 */
/*				try { // Stall the satus poller just to make sure it isn't overzealous!
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
*/			//}
			Date date = new Date();
			//TODO: We don't need the quotes on the integers in JSON
			sb.append("{\"distanceCenter\":"
					+ myRobot.getDistanceCenter()
					+ ",\"distanceLeft\":"
					+ myRobot.getDistanceLeft()
					+ ",\"distanceRight\":"
					+ myRobot.getDistanceRight()
					+ ",\"Heading\":"
					+ myRobot.getHeading()
					+ ",\"travelSpeed\":"
					+ myRobot.getTravelSpeed()
					+ ",\"rotateSpeed\":"
					+ myRobot.getRotateSpeed()
					+ ",\"viewAngle\":"
					+ myRobot.getViewAngle()
					+ ",\"serverTime\":\""
					+ date.toString()
					+ "\",\"connectRequested\":"
					+ myRobot.isConnectRequested()
					+ ",\"connectedToNXT\":"
					+ myRobot.isConnectedToNXT()
					+ ",\"isDoing\":\""
					+ myRobot.getIsDoing()
					+ "\",\"lastResult\":\""
					+ myRobot.getLastResult()
					+ "\",\"tilt\":\""
					+ myRobot.getTilt()
					+ "\"}");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
			
			/*
			 * Config:
			 * Return some static values and initial settings to use on a fresh load of the web interfaace.
			 */
		} else if (URItext.contains("config.json")) {
			sb.append("{\"refreshInterval\": "
					+ "1000"
					+ ",\"mode\":\""
					+ "live"
					+ "\",\"travelSpeed\":"
					+ myRobot.getTravelSpeed()
					+ ",\"rotateSpeed\":"
					+ myRobot.getRotateSpeed()
					+ ",\"robotIP\":\""
					//TODO Make this dynamic
					+ "192.168.1.95"
					+ "\",\"cameraPort\": "
					+ "8080"
					+ "}");
			return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		} else if (URItext.contains("sendtestdata.html")) {
			AssetManager assetManager = context.getAssets();
			InputStream input = null;
			try {
				// Put the file in "assets" in Eclipse
				input = assetManager.open("sendtestdata.html");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Response(Status.OK, MIME_HTML, input);
		} else if (URItext.contains("sendDataTEST")) {
			sb.append("Access-Control-Allow-Origin: true");
			sb.append("<html>");
			sb.append("<head><title>Test Send Data</title></head>");
			sb.append("<body>");
			sb.append("<h1>Data Received:</h1>");

			sb.append("<p><blockquote><b>URI</b> = ")
					.append(String.valueOf(session.getUri()))
					.append("<br />");

			sb.append("<b>Method</b> = ")
					.append(String.valueOf(session.getMethod()))
					.append("</blockquote></p>");

			sb.append("<h3>Headers</h3><p><blockquote>")
					.append(toString(session.getHeaders()))
					.append("</blockquote></p>");

			sb.append("<h3>Parms</h3><p><blockquote>")
					.append(toString(session.getParms()))
					.append("</blockquote></p>");

			sb.append("<h3>Parms (multi values?)</h3><p><blockquote>")
					.append(toString(decodedQueryParameters))
					.append("</blockquote></p>");
			String messageFromRobot = session.getParms().get("messageForRobot");
			if (messageFromRobot != null) {
			Log.d("messageFromRobot", messageFromRobot);

			sb.append("<h3>JSON Data</h3><p><blockquote>");
			try {
				//Map<String, String> data = new HashMap<String, String>();
				//session.parseBody(data);
				//sb.append(toString(data));
				//Log.d("JSON", "" + data);
				//Log.d("JSON", "" + data.get("messageForRobot"));
				//String messageFromRobot = data.get("messageForRobot");
				JSONObject statusJObject;
				// JSON in Android: http://stackoverflow.com/questions/9605913/how-to-parse-json-in-android
				try {
				    statusJObject = new JSONObject(messageFromRobot);

				    // Place the variables into myRobot
				    
				    sb.append("travelSpeed: ").append(statusJObject.getString("travelSpeed"));
				    myRobot.setTravelSpeed(statusJObject.getInt("travelSpeed"));
				    sb.append("<br />");
				    sb.append("rotateSpeed: ").append(statusJObject.getString("rotateSpeed"));
				    myRobot.setRotateSpeed(statusJObject.getInt("rotateSpeed"));
				    sb.append("<br />");
				    sb.append("viewAngle: ").append(statusJObject.getString("viewAngle"));
				    myRobot.setViewAngle(statusJObject.getInt("viewAngle"));

				} catch (JSONException e) {
				    Log.d("WebServer", "JSon Parse Exception");
				    // e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			sb.append("</blockquote></p>");
			} else {
				sb.append("Input Error");
			}

			sb.append("</body>");
			sb.append("</html>");
			return new Response(Status.OK, MIME_HTML, sb.toString());
		} else if (URItext.contains("sendData")) {
			String messageFromRobot = session.getParms().get("messageForRobot");
			if (messageFromRobot != null) {
			try {
				JSONObject statusJObject;
				try {
				    statusJObject = new JSONObject(messageFromRobot);
				    myRobot.setTravelSpeed(statusJObject.getInt("travelSpeed"));
				    myRobot.setRotateSpeed(statusJObject.getInt("rotateSpeed"));
				    myRobot.setViewAngle(statusJObject.getInt("viewAngle"));
				} catch (JSONException e) {
				    Log.d("WebServer", "JSon Parse Exception");
				    // e.printStackTrace();
				    sb.append("JSON Parse Error");
				}
			} catch (Exception e) {
				Log.d("WebServer", "JSON Object Error");
				//e.printStackTrace();
				sb.append("JSON Error");
			}
			} else {
				sb.append("Input Error");
			}
			return new Response(Status.OK, MIME_HTML, sb.toString());
		} else if (URItext.contains("debug")){
			/*
			 * This is the demonstration code from the Nanohttpd
			 * page. It is just here as a demo for future coding.
			 * You can make this the default to see what URI results
			 * from various entries at the browser.
			 * 
			 * Send a get request like this to see how we can use get or post data:
			 * http://192.168.1.95:8081/debug.html?name1=value1&name2=value2
			 */
			sb.append("<html>");
			sb.append("<head><title>Debug Server</title></head>");
			sb.append("<body>");
			sb.append("<h1>Debug Server</h1>");

			sb.append("<p><blockquote><b>URI</b> = ")
					.append(String.valueOf(session.getUri()))
					.append("<br />");

			sb.append("<b>Method</b> = ")
					.append(String.valueOf(session.getMethod()))
					.append("</blockquote></p>");

			sb.append("<h3>Headers</h3><p><blockquote>")
					.append(toString(session.getHeaders()))
					.append("</blockquote></p>");

			sb.append("<h3>Parms</h3><p><blockquote>")
					.append(toString(session.getParms()))
					.append("</blockquote></p>");

			sb.append("<h3>Parms (multi values?)</h3><p><blockquote>")
					.append(toString(decodedQueryParameters))
					.append("</blockquote></p>");

			try {
				Map<String, String> files = new HashMap<String, String>();
				session.parseBody(files);
				sb.append("<h3>Files</h3><p><blockquote>")
						.append(toString(files))
						.append("</blockquote></p>");
			} catch (Exception e) {
				e.printStackTrace();
			}

			sb.append("</body>");
			sb.append("</html>");
			return new Response(Status.OK, MIME_HTML, sb.toString());
		} else {
			/*
			 * Sending the index as default seems nice,
			 * but then if we send any bad commands via the interface
			 * by accident we get the entire site
			 * as a response. This way we get a useable text only response.
			 */
		sb.append("Does Not Compute");
		return new Response(Status.OK, MIME_PLAINTEXT, sb.toString());
		}
	}

	private String toString(Map<String, ? extends Object> map) {
		if (map.size() == 0) {
			return "";
		}
		return unsortedList(map);
	}

	private String unsortedList(Map<String, ? extends Object> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		for (Map.Entry entry : map.entrySet()) {
			listItem(sb, entry);
		}
		sb.append("</ul>");
		return sb.toString();
	}

	private void listItem(StringBuilder sb, Map.Entry entry) {
		sb.append("<li><code><b>").append(entry.getKey()).append("</b> = ")
				.append(entry.getValue()).append("</code></li>");
	}
	
	private void sendRobotCommandToUIthread(Command commandToSend) {
		Message message_holder = new Message();
		message_holder.what = CommandType.ROBOT.ordinal();
		message_holder.arg1 = commandToSend.ordinal();
		robotWebServerMessageHandler.sendMessage(message_holder);
	}
	
	private void sendAndroidCommandToUIthread(AndroidCommand commandToSend) {
		Message message_holder = new Message();
		message_holder.what = CommandType.ANDROID.ordinal();
		message_holder.arg1 = commandToSend.ordinal();
		robotWebServerMessageHandler.sendMessage(message_holder);
	}


}
