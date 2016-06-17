/*
 * Copyright (c) 2006 Stanford University.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Stanford University nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL STANFORD
 * UNIVERSITY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.tinyos.mviz;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.*;
import java.util.*;

import net.tinyos.message.*;
import net.tinyos.mviz.DDocument.LinkSetEvent;
import net.tinyos.mviz.DDocument.NewMessageEvent;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import node.SensorsDataMsg;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.*;

/**
 * This class represents the link between DDocument (GUI part) and BuildSource,
 * which is in charge of creating an interface to communicate with motes
 * (through serial port,TCP connection,etc...). Messages from motes are
 * collected and delivered to DDocument by mean of ValueSetEvent and
 * LinkSetEvent. Modified to include REST Client in order to upload values on
 * Parse repository; implements MessageListener interface
 * 
 * @author user
 * @see MessageListener
 */

public class MessageInput implements net.tinyos.message.MessageListener {

	private MoteIF moteIF;
	private DDocument document;
	private int[] valuesToUpload;
	private ArrayList<Integer> motes;
	private SensorsDataMsg messagesFormat;

	/**
	 * Constructor for the MessageInput class
	 * 
	 * @param packetVector
	 * @param commSource
	 * @param doc
	 */

	public MessageInput(String commSource, DDocument doc) {

		/**
		 * Set the reference to the main container (DDocument) where data
		 * collected from motes will be displayed
		 */

		document = doc;

		/**
		 * Create a new instance from the class "SensorDataMsg", which
		 * represents allowed format of messages from motes
		 */

		messagesFormat = new SensorsDataMsg();

		/**
		 * Initialize the list of motes
		 */

		motes=new ArrayList<Integer>();
		try {

			/**
			 * Create sources to retrieve messages sent by motes: use the source
			 * specified as argument of the application or, if no source was
			 * specified, use the one defined by environment variable MOTECOM
			 */

			createSource(commSource);

			/**
			 * Register this class as listener for the type of messages defined
			 * by class SensorsDataMsg, namely this class will be notified by
			 * underlying MoteIF when only messages of this type are received.
			 * On its turn, MessageInput will notify this to DDocument
			 * 
			 * @see MoteIF
			 */

			addMsgType(messagesFormat);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	/**
	 * Create a new instance of the class representing a source of packets
	 * 
	 * @param source
	 */

	private void createSource(String source) {
		if (source != null) {

			/**
			 * Create source using specified source
			 */

			moteIF = new MoteIF(BuildSource.makePhoenix(source,
					PrintStreamMessenger.err));
		} else {

			/**
			 * Create source using the one specified by MOTECOM variable; the
			 * second parameter is the object in charge of printing status
			 * messages sent by the source: here's the system error stream
			 * 
			 * @see BuildSource
			 */

			moteIF = new MoteIF(
					BuildSource.makePhoenix(PrintStreamMessenger.err));
		}
	}

	/**
	 * Tell the underlying MoteIF instance to listen for messages of the
	 * provided type
	 * 
	 * @param msg
	 */

	private void addMsgType(Message msg) {
		moteIF.registerListener(msg, this);
	}

	public void start() {
	}

	/**
	 * This is the callback function that is invoked by underlying
	 * layers of the applet when a new message is received by motes:
	 * here we parse the message to upload the data and to show the
	 * network formed by motes to deliver messages to host
	 */

	public void messageReceived(int to, Message message) {

		/**
		 * Check whether the format of the received message is the one defined
		 * by SensorsDataMsg: if so, read the message, otherwise ignore it
		 */

		try {

			SensorsDataMsg msg = (SensorsDataMsg) message;

			/**
			 * Notify the main class that a path exists between a "producer mote" and the root mote
			 */

			document.setNewPath(msg.get_message_path());

			/**
			 * Extract the number of motes along the path,from the source to the
			 * sink for the current message
			 */

			int hopcount = msg.get_hopcount();

			/**
			 * Initialize the vector with values to upload on Parse, namely: - x
			 * component of acceleration - y component of acceleration - z
			 * component of acceleration - source of the message - list of IDs
			 * of motes along the path from the source to the sink
			 */

			valuesToUpload = new int[4];

			/**
			 * Extract id of the origin of the message
			 */

			int origin = msg.getElement_message_path(0);

			/**
			 * Fill the array "valuesToSend" with values to be sent to be
			 * upload to Parse Repository
			 */
			
			/**
			 * First add values of acceleration
			 */

			Short x = msg.get_x_acceleration();
			valuesToUpload[1] = x;
			Short y = msg.get_y_acceleration();
			valuesToUpload[2] = y;
			Short z = msg.get_z_acceleration();
			valuesToUpload[3] = z;
			
			/**
			 * Then add ID of the sender
			 */
			
			valuesToUpload[3]=origin;
			
			/**
			 * Update data received on Parse
			 */

			updateOnParse();

			/**
			 * For each segment of the path of the current message,
			 * create a new event and post it to the AWT thread:
			 * when this will be processed the tables and the canvas
			 * will be updated depending on the data contained in the
			 * message received
			 */

			/**
			 * The number of links for a message that was processed
			 * by n motes is n-1
			 */

			for (int i = 0; i < hopcount-1; i++) {

				/**
				 * The quality of the link
				 */

				int linkQuality = msg.getElement_path_quality(i);

				/**
				 * "Producers" (motes with accelerometer, leaves of the collection
				 * tree) will have the border of the icon colored in order to 
				 * be distinguishable from the other motes, so set a corresponding
				 * property of the event for the first link of the path
				 */

				if(i==0){

					/**
					 * Last argument of the constructor tells if the mote is a producer
					 */

					document.setLinkValue(linkQuality, msg.getElement_message_path(i),msg.getElement_message_path(i+1),true);
				}
				else{

					/**
					 * Last link is between the root node and its child
					 */

					if(i!=hopcount-2){
						//document.setLinkValue(msg.getElement_message_path(i), msg.getElement_message_path(i+1),new Integer(msg.getElement_message_path(i)).toString()+"->"+new Integer(msg.getElement_message_path(i+1)).toString(),linkQuality);

						/**
						 * Notify the main class that a link exists between this couple of motes
						 */

						document.setLinkValue(linkQuality, msg.getElement_message_path(i),msg.getElement_message_path(i+1),false);
					}
					else{
						//document.setLinkValue(msg.getElement_message_path(i), document.rootMote,new Integer(msg.getElement_message_path(i)).toString()+"->"+new Integer(document.rootMote).toString(),linkQuality);

						/**
						 * Notify the main class that a link exists between this couple of motes
						 * (and one of them is the root mote)
						 */

						document.setLinkValue(linkQuality, msg.getElement_message_path(i),document.rootMote,false);
					}
				}
			}
		}
		catch (ClassCastException e) {

			/**
			 * Do nothing, ignore the message
			 */
		}
	}

	/**
	 * Update values for acceleration on the repository
	 * at Parse
	 */

	void updateOnParse() {
		String[] parameters = { "X", "Y", "Z","Origin"};
		try {

			/**
			 * Client to make http requests
			 */

			CloseableHttpClient httpclient = HttpClients.createDefault();

			/**
			 * Set URL of the request
			 */

			HttpPost httpPost = new HttpPost(document.parsePostURL);

			/**
			 * Add headers to the post request, as needed to use
			 * the Parse API
			 */

			httpPost.addHeader("X-Parse-Application-Id",document.parseApplicationId);
			httpPost.addHeader("X-Parse-REST-API-Key",document.parseRESTApiKey);

			/**
			 * Create the body of the POST request:the syntax to use with PARSE API
			 * is the following:
			 * 
			 * {"PARAMETER1":VALUE1,"PARAMETER2":VALUE2,"PARAMETER3":VALUE3,...}
			 */

			String requestBody = "{";
			for (int i = 0; i < 4; i++) {
				requestBody += "\"" + String.valueOf(parameters[i]) + "\""
						+ ": " + Integer.toString(valuesToUpload[i]) + ",";
			}

			/**
			 * Remove last comma
			 */

			requestBody = requestBody.substring(0,
					requestBody.length() - 1);

			/**
			 * Close the body
			 */

			requestBody += "}";

			/**
			 * Set the body
			 */
			
			StringEntity myEntity = new StringEntity(requestBody,ContentType.create("application/json", "UTF-8"));
			httpPost.setEntity(myEntity);

			/**
			 * Make the request
			 */

			CloseableHttpResponse response = httpclient.execute(httpPost);
		}
		catch (IOException ex) {
			System.out.println("Exception:" + ex.getMessage());
		}
	}
}
