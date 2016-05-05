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

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

import net.tinyos.message.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;

/*my edit 5/9/2015:include RESTClient*/
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.entity.*;


/**
 * This class represents the bridge between DDocument (GUI part)
 * and BuilSource, which is in charge of creating an interface
 * to communicate with motes (through serial port,TCP connection,etc...).
 * Messages from motes are collected and delivered to DDocument
 * by mean of ValueSetEvent and LinkSetEvent.
 * Modified to include REST Client in order to upload values
 * on Parse repository; implements MessageListener interface
 * @author user
 * @see MessageListener
 */

public class MessageInput implements net.tinyos.message.MessageListener {

	private Vector msgVector = new Vector();
	private MoteIF moteIF;
	private DDocument document;
	private int[] valuesToSend;
	
	/**
	 * Constructor for the MessageInput class
	 * @param packetVector
	 * @param commSource
	 * @param doc
	 */

	public MessageInput(Vector packetVector, String commSource, DDocument doc) {

		/**
		 * Set the reference to the main container (DDocument) where data collected
		 * from motes will be displayed
		 */
		
		document = doc;
		
		/**
		 * Load instances of classes implementing the 
		 * Message interface: they determine how messages
		 * from motes should be parsed
		 */
		
		loadMessages(packetVector);	
		try {
			
			/**
			 * Create sources to retrieve messages sent
			 * by motes: use the source specified as
			 * argument of the application or, if no
			 * source was specified, use the one defined
			 * by environment variable MOTECOM
			 */
			
			createSource(commSource);
			
			/**
			 * Register this class as listener for all the
			 * type of messages specified via packetVector,
			 * namely this class will be notified by underlying
			 * MoteIF when messages of the allowed types are
			 * received. On its turn, MessageInput will notify
			 * this to DDocument
			 * @see MoteIF
			 */
			
			installListeners();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	/**
	 * Fill in array "msgVector" with one instance of each class
	 * of message packet format
	 * @param packetVector
	 */

	private void loadMessages(Vector packetVector) {
		for (int i = 0; i < packetVector.size(); i++) {
			String className = (String) packetVector.elementAt(i);
			try {
				Class c = Class.forName(className);
				Object packet = c.newInstance();
				Message msg = (Message) packet;
				msgVector.addElement(msg);
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}
	
	/**
	 * Create a new instance of the class representing
	 * a source of packets
	 * @param source
	 */
	
	private void createSource(String source) {
		if (source != null) {
			
			/**
			 * Create source using specified source
			 */
			
			moteIF = new MoteIF(BuildSource.makePhoenix(source, PrintStreamMessenger.err));
		} else {
			
			/**
			 * Create source using the one specified by MOTECOM variable; the second parameter
			 * is the object in charge of printing status messages sent by the source: here's the
			 * system error stream
			 * @see BuildSource
			 */
			
			moteIF = new MoteIF(BuildSource.makePhoenix(PrintStreamMessenger.err));
		}
	}

	/**
	 * Tell the underlying MoteIF instance to
	 * listen for messages of the provided type
	 * @param msg
	 */
	
	private void addMsgType(Message msg) {
		moteIF.registerListener(msg, this);
	}
	
	/**
	 * For each object added into msgVector,
	 * cast it to Message class (classes representing
	 * format of messages, created with mig, extend
	 * Message class) and set a listener for it.
	 */

	private void installListeners() {
		Enumeration msgs = msgVector.elements();
		int i = 0;
		while (msgs.hasMoreElements()) {
			Message m = (Message) msgs.nextElement();
			this.addMsgType(m);
		}
	}

	public void start() {
	}

	public void messageReceived(int to, Message message) {
		Hashtable table = new Hashtable();
		Hashtable linkTable = new Hashtable();
		Class pktClass = message.getClass();
		Method[] methods = pktClass.getMethods();
		valuesToSend = new int[3];
		char[] parameters = {'X', 'Y', 'Z'};
		int index = 0;
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			String name = method.getName();
			Class[] params = method.getParameterTypes();
			Class returnType = method.getReturnType();
			/*if (params.length != 0 || returnType.isArray()) {
             continue;
             }*/
			if (params.length != 0) {
				continue;
			}
			if (name.startsWith("get_") && !name.startsWith("get_link")) {
				name = name.substring(4); // Chop off "get_"
				try {
					/* edit by Leo90 - start*/

					//System.out.println(name + " returns " + res);
					//Short result = (Short)method.invoke(message, null);
					//Integer result = (Integer)method.invoke(message, null);
					//table.put(name, result);
					if (name.equals("x_acceleration") || name.equals("y_acceleration") || name.equals("z_acceleration")) {
						Short result = (Short) method.invoke(message, null);
						table.put(name, result);
						valuesToSend[index] = result;
						index++;
						System.out.println("Name:" + name + " result:" + result + " string:" + method.toGenericString());
					} else if (name.equals("message_path")) {
						int[] result = (int[]) method.invoke(message, null);
						for (int j = 0; j < result.length; j++) {
							table.put(name + j, result[j]);
							System.out.println("Name:" + name + j + " result:" + result[j] + " string:" + method.toGenericString());
						}
					} else {
						Integer result = (Integer) method.invoke(message, null);
						table.put(name, result);
						System.out.println("Name:" + name + " result:" + result + " string:" + method.toGenericString());
					}
					//System.out.println("Name:"+name+" result:"+result+" string:"+method.toGenericString());

					/* edit by Leo90 - end*/
				} catch (java.lang.IllegalAccessException exc) {
					System.err.println("Unable to access field " + name);
				} catch (java.lang.reflect.InvocationTargetException exc) {
					System.err.println("Unable to access target " + name);
				}
			} else if (name.startsWith("get_link_")) {
				name = name.substring(9); // chop off "get_link_"
				try {
					Integer result = (Integer) method.invoke(message, null);
					linkTable.put(name, result);
					System.out.println("Name:" + name + " result:" + result + " string:" + method.toGenericString());
				} catch (java.lang.IllegalAccessException exc) {
					System.err.println("Unable to access field " + name);
				} catch (java.lang.reflect.InvocationTargetException exc) {
					System.err.println("Unable to access target " + name);
				}
			}
		}
		/*REMOVE Print keys*/
		Enumeration keys = table.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			System.err.println(key + " " + table.get(key));
		}
		if (table.containsKey("origin")) {
			int count = 0;
			Integer origin = (Integer) table.get("origin");
			//table.remove("origin");
			Enumeration elements = table.keys();
			while (elements.hasMoreElements()) {
				String key = (String) elements.nextElement();
				/*Integer value = (Integer)table.get(key);
                 document.setMoteValue(origin.intValue(), key, value.intValue());*/
				/* edit by Leo90 - start*/
				if (key.equals("x_acceleration") || key.equals("y_acceleration") || key.equals("z_acceleration")) {
					Short value = (Short) table.get(key);
					document.setMoteValue(origin.intValue(), key, value.shortValue());
					System.err.println("COUNT:" + count);
					count++;
				} else {
					Integer value = (Integer) table.get(key);
					document.setMoteValue(origin.intValue(), key, value.intValue());
					System.err.println("COUNT:" + count);
					count++;
				}
				/* edit by Leo90 - end*/
			}
			elements = linkTable.keys();
			while (elements.hasMoreElements()) {
				String key = (String) elements.nextElement();
				if (!key.endsWith("_value")) {
					continue;
				}
				Integer value = (Integer) linkTable.get(key);
				key = key.substring(0, key.length() - 6); // chop off "_value"
				String addrkey = key + "_addr";
				if (!linkTable.containsKey(addrkey)) {
					continue;
				}
				Integer addr = (Integer) linkTable.get(addrkey);
				document.setLinkValue(origin.intValue(), addr.intValue(), key, value.intValue());
				System.err.println("COUNT:" + count);
				count++;
			}
		} else {
			System.err.println("Could not find origin field, discarding message.");
		}
		Integer origine = (Integer) table.get("origin");
		System.out.println("ORIGIN:" + origine);
		Thread t = new Thread(new Runnable() {
			public void run() {
				updateOnParse();
			}
		});
		if (origine.intValue() == 1) {
			t.start();
		}
		/*my edit 5/9/2015:include RESTClient
         try{
         CloseableHttpClient httpclient = HttpClients.createDefault();
         HttpPost httpPost = new HttpPost("https://api.parse.com/1/classes/Acceleration");
         httpPost.addHeader("X-Parse-Application-Id","5n2Djg2xAkoeFWmOsfUbvFVBo93Q7Auy7b3T3qd3");
         httpPost.addHeader("X-Parse-REST-API-Key","YAyYipCKIC5yJHRRrWm74ObGX8ajUSFNR0Ab7Xgf");
         String messageToSend="{";
         for(int i=0;i<3;i++){
         messageToSend+="\""+String.valueOf(parameters[i])+"\""+": "+Integer.toString(valuesToSend[i])+",";
         }
         messageToSend=messageToSend.substring(0,messageToSend.length()-1);
         messageToSend+="}";
         System.out.println(messageToSend);
         StringEntity myEntity = new StringEntity(messageToSend,ContentType.create("application/json", "UTF-8"));
         httpPost.setEntity(myEntity);
         CloseableHttpResponse response = httpclient.execute(httpPost);
         BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
         String line = "";
         while ((line = rd.readLine()) != null) {
         System.out.println(line);

         }
         if(3==4){
         throw new IOException();
         }
         }
         catch(IOException ex){
         System.out.println("Exception:"+ex.getMessage());
         }
         my edit 5/9/2015:include RESTClient*/
	}

	void updateOnParse() {
		char[] parameters = {'X', 'Y', 'Z'};
		try {
			System.err.println("updating on Parse");
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost httpPost = new HttpPost("https://api.parse.com/1/classes/Acceleration");
			httpPost.addHeader("X-Parse-Application-Id", "5n2Djg2xAkoeFWmOsfUbvFVBo93Q7Auy7b3T3qd3");
			httpPost.addHeader("X-Parse-REST-API-Key", "YAyYipCKIC5yJHRRrWm74ObGX8ajUSFNR0Ab7Xgf");
			String messageToSend = "{";
			for (int i = 0; i < 3; i++) {
				messageToSend += "\"" + String.valueOf(parameters[i]) + "\"" + ": " + Integer.toString(valuesToSend[i]) + ",";
			}
			messageToSend = messageToSend.substring(0, messageToSend.length() - 1);
			messageToSend += "}";
			System.out.println(messageToSend);
			StringEntity myEntity = new StringEntity(messageToSend, ContentType.create("application/json", "UTF-8"));
			httpPost.setEntity(myEntity);
			CloseableHttpResponse response = httpclient.execute(httpPost);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				System.out.println(line);

			}
		} catch (IOException ex) {
			System.out.println("Exception:" + ex.getMessage());
		}
	}

}
