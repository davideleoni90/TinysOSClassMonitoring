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

/**
 * java AWT (Abstract Window Toolkit) contains user interface toolkit and base graphic
 * classes
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import net.tinyos.message.*;

/*
 * import classes to handle http requests
 */

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.entity.*;
import org.json.*;

/**
 * DDOCUMENT: ->This is the class which is actually invoked when executing the
 * script "tos-mviz"
 */

public class DDocument

/**
 * generic container element that groups components inside JFrames and other
 * JPanels
 */

extends JPanel

/**
 * ActionListener is the basic Swing interface to receive ActionEvents;it
 * comprises only one method "actionPerformed" which is the basic event handler
 */

implements ActionListener {

	/**
	 * the name of the Java classes, produced with "mig" tool, representing the
	 * messages sent by the network of motes
	 */

	static final String messagesJavaClass = "SensorsDataMsg";
	
	/**
	 * Data from sensors, after having being uploaded on Parse
	 * repository can be retrieved through HTTP GET requests.
	 * These requests must include two HTTP Headers:
	 * 1- X-Parse-Application-Id
	 * 2- X-Parse-REST-API-Key
	 * 
	 * Values for these headers are written in the java file
	 * property.
	 */
	
	static final String parseApplicationIdHeader ="X-Parse-Application-Id";
	static final String parseRESTApiKeyHeader ="X-Parse-REST-API-Key";

	protected String directory;
	protected String moteImg;
	protected String parseRequestURL;
	protected String parseApplicationId;
	protected String parseRESTApiKey;
	protected JPanel canvas;
	protected Vector layers;

	private Color currentColor;

	public float[] maxValues;
	public int selectedFieldIndex;
	public int selectedLinkIndex;
	public ImageIcon icon;
	public Image image;

	public DNavigate navigator;

	public Color getColor() {
		return currentColor;
	}

	public Vector sensed_motes;
	public Vector sensed_links;
	public ArrayList moteModels;
	public ArrayList linkModels;
	private JTextField jText;
	private DrawTableModel tableModel;
	private JTable jTable;

	/*
	 * SENSORS DATA TABLE ->components for the table with data from sensors -
	 * start
	 */

	protected Button measuresButton;
	private JTable measuresTable;
	public int measuresTableWidth = 600;
	public int measuresTableHeight = 600;

	protected ArrayList motes = new ArrayList();
	protected ArrayList links = new ArrayList();
	protected DMoteModel selected = null;

	protected HashMap moteIndex;
	protected HashMap linkIndex;
	private MeasuresTableModel measuresTableModel;

	/*
	 * SENSORS DATA TABLE - END
	 */

	private String[] toStringArray(Vector v) {
		String[] array = new String[v.size()];
		for (int i = 0; i < v.size(); i++) {
			array[i] = (String) v.elementAt(i);
		}
		return array;
	}

	/*
	 * DDOCUMENT CONSTRUCTOR: ->initialize all the graphic elements within
	 * DDocument
	 */

	public DDocument(int width, int height, Vector fieldVector,
			Vector linkVector, String dir, String mote, String parseGetUrl,String
			parseApplicationId, String parseRESTApiKey) {
		super();
		layers = new Vector();

		/**
		 * Directory where the script is executed (by the default) is the
		 * current directory
		 */

		directory = dir;
		
		/**
		 * Path to the image to be used to represent motes on the canvas
		 */
		
		moteImg = mote;
		
		/**
		 * URL of the HTTP GET Parse request
		 */
		
		parseRequestURL=parseGetUrl;
		
		/**
		 * Value for X-Parse-Application-Id
		 */
		
		this.parseApplicationId=parseApplicationId;
		
		/**
		 * Value for X-Parse-REST-API-Key
		 */
		
		this.parseRESTApiKey=parseRESTApiKey;

		setOpaque(false);

		/**
		 * Set LayoutManager for DDocument to BorderLayout: arranges components
		 * in one of five geographical locations NORTH , SOUTH ,EAST , WEST ,
		 * and CENTER. The two parameters are respectively horizontal and
		 * vertical gaps between components
		 */

		setLayout(new BorderLayout(6, 6));

		/**
		 * TO BE CHECKED
		 */

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignore) {
		}

		selectedFieldIndex = 0;
		selectedLinkIndex = 0;

		/**
		 * Create the canvas where motes and links will be displayed
		 */

		canvas = new DPanel(this);

		/**
		 * Set the layout manager for canvas to NULL => components are
		 * absolutely positioned
		 */

		canvas.setLayout(null);

		/**
		 * Use double buffering: Swing first performs drawing operations in an
		 * offscreen buffer and then copies the completed work to the display in
		 * a single painting operation. This reduces performances but eliminates
		 * flickering
		 */

		canvas.setDoubleBuffered(true);

		/**
		 * Layout managers tries to set components to their preferred size
		 */
		canvas.setPreferredSize(new Dimension(width, height));

		/**
		 * Revalidate() has to be call every time we change the preferredSize of
		 * a component
		 */

		canvas.revalidate();

		/**
		 * Set minimumSize of the canvas
		 */

		canvas.setMinimumSize(new Dimension(width, height));

		/**
		 * Set actual size of the canvas
		 */

		canvas.setSize(new Dimension(width, height));

		/**
		 * Canvas is absolutely non-transaparent
		 */

		canvas.setOpaque(false);

		/**
		 * Set border of the canvas
		 */

		canvas.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));

		/**
		 * Add the canvas at the center of DDocument.
		 */

		add(canvas, BorderLayout.CENTER);

		/**
		 * Get the list of motes and links
		 */

		sensed_motes = fieldVector;
		sensed_links = linkVector;
		moteIndex = new HashMap();
		linkIndex = new HashMap();

		/**
		 * Load images to represent motes on the canvas
		 */

		String imgName = directory + moteImg;
		try {
			image = Toolkit.getDefaultToolkit().getImage(imgName);
		} catch (Exception e) {
			System.out.println(e);
		}

		/**
		 * ComponentListener is the interface an object has to implement in
		 * order to receive events from a component; here we create an anonymous
		 * inner class
		 */

		canvas.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				navigator.redrawAllLayers();
			}

			public void componentHidden(ComponentEvent arg0) {
				
				/**
				 * do nothing
				 */
			}

			public void componentMoved(ComponentEvent arg0) {
				
				/**
				 * do nothing
				 */
			}

			public void componentShown(ComponentEvent arg0) {
				
				/**
				 * do nothing
				 */
			}
		});

		/**
		 * Create the control area, on the left of the window
		 */

		JPanel west = new JPanel();

		/**
		 * Set the double buffer for the control area
		 */

		west.setDoubleBuffered(true);

		/**
		 * Set the BoxLayout for the control area
		 */

		west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));

		/**
		 * Add the control area to the window
		 */

		add(west, BorderLayout.WEST);
		// currentColor = Color.GRAY;

		/**
		 * Create the lateral navigator
		 */

		navigator = new DNavigate(sensed_motes, sensed_links, this);
		
		/**
		 * Add the navigator to DDocument
		 */
		
		west.add(navigator);
		
		/**
		 * In a vertical box, this is used to create an invisible fixed-height component
		 * with the aim of create space between components: here we want to insert space
		 * between navigator and the table with values from Parse
		 */
		
		west.add(Box.createVerticalStrut(50));
		
		/**
		 * Create a new instance of the table showing measures from sensors stored on Parse
		 */
		
		measuresTableModel = new MeasuresTableModel();
		measuresTable = new JTable(measuresTableModel);
		measuresTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		/**
		 * Use default size for the table
		 */
		
		Dimension tableDimension = measuresTable
				.getPreferredScrollableViewportSize();
		
		/**
		 * Create a scrollable pane out of the table with measures
		 * retrieved from Parse repository
		 */
		
		JScrollPane measuresScroller = new JScrollPane(measuresTable);
		measuresScroller.setPreferredSize(new Dimension(tableDimension.width,
				measuresTable.getRowHeight() * 3 + 1));
		
		/**
		 * Create the button that has to be clicked in order to retrieve measures
		 * from Parse
		 */
		
		measuresButton = new Button("Click to get the 3 last updated values");
		measuresButton.addActionListener(measuresTableModel);
		
		/**
		 * Add the button to lateral control area of DDocument
		 */
		
		west.add(measuresButton);
		
		/**
		 * Separate button from underlying table
		 */
		
		west.add(Box.createVerticalStrut(10));
		
		/**
		 * Add scrollable pane
		 */
		
		west.add(measuresScroller);
		
		/**
		 * Create a new instance of the table model to represent
		 * the set of motes in the WSN
		 */
		
		tableModel = new DrawTableModel(sensed_motes);
		
		/**
		 * Create a new table out of the preceding table model
		 */
		
		jTable = new JTable(tableModel);
		// jTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		tableDimension = jTable.getPreferredScrollableViewportSize();
		
		/**
		 * Create a scrollable pane out of the table with motes
		 */
		
		JScrollPane scroller = new JScrollPane(jTable);
		scroller.setPreferredSize(new Dimension(tableDimension.width,
				measuresTable.getRowHeight() * 3 + 1));
		// scroller.setPreferredSize(new Dimension(350, 200));
		/*
		 * scroller.setMinimumSize(new Dimension(350, 200));
		 * scroller.setSize(new Dimension(350, 200));
		 */
		
		/**
		 * Separate measures table from motes table
		 */
		
		west.add(Box.createVerticalStrut(10));
		
		/**
		 * Add motes table
		 */
		
		west.add(scroller);

		/**
		 * Enable events defined by the mask given as a parameter
		 * to be delivered to DDocument. We defined this mask as
		 * the max id reserved for ATWEvents+1 and max id reserved
		 * for ATWEvents+2 for ValueSetEvent and LinkSetEvent
		 * respectively.
		 * With this method calls, the two events below are delivered
		 * to DDocument (method "processEvent" even though it is not
		 * registered as a listener.
		 */
		
		enableEvents(LinkSetEvent.EVENT_ID);
		enableEvents(ValueSetEvent.EVENT_ID);
	}

	/*
	 * DDOCUMENT CONSTRUCTOR - END
	 */

	public void actionPerformed(ActionEvent e) {
	}

	private void zMove(int direction) {
		tableModel.updateTable();
	}

	public DShape getSelected() {
		return null;
	}

	public void setSelected(DShape selected) {
	}

	Random rand = new Random();

	private DMoteModel createNewMote(int moteID) {
		DMoteModel m = new DMoteModel(moteID, rand, this);
		System.out.println("Adding mote " + moteID);
		motes.add(m);
		moteIndex.put(new Integer(moteID), m);
		tableModel.add(m);

		navigator.addMote(m);
		return m;
	}

	public void setMoteValue(int moteID, String name, int value) {
		ValueSetEvent vsv = new ValueSetEvent(this, moteID, name, value);

		/**
		 * T
		 */
		EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
		eq.postEvent(vsv);
	}

	private DLinkModel createNewLink(DMoteModel start, DMoteModel end) {
		DLinkModel dl = new DLinkModel(start, end, rand, this);
		links.add(dl);
		linkIndex.put(start.getId() + " " + end.getId(), dl);
		return dl;
	}

	public void setLinkValue(int startMote, int endMote, String name, int value) {
		LinkSetEvent lsv = new LinkSetEvent(this, name, value, startMote,
				endMote);
		EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
		eq.postEvent(lsv);
	}

	protected void processEvent(AWTEvent event) {
		System.err.println(event.getSource().toString() + "->"
				+ event.paramString());
		if (event instanceof ValueSetEvent) {
			ValueSetEvent vsv = (ValueSetEvent) event;
			String name = vsv.name();
			int moteID = vsv.moteId();
			int value = vsv.value();
			DMoteModel m = (DMoteModel) moteIndex.get(new Integer(moteID));
			if (m == null) {
				m = createNewMote(moteID);
			}
			m.setMoteValue(name, value);
			navigator.redrawAllLayers();
		} else if (event instanceof LinkSetEvent) {
			LinkSetEvent lsv = (LinkSetEvent) event;
			String name = lsv.name();
			int startMote = lsv.start();
			int endMote = lsv.end();
			int value = lsv.value();
			DMoteModel m = (DMoteModel) moteIndex.get(new Integer(startMote));
			if (m == null) {
				m = createNewMote(startMote);
			}
			DMoteModel m2 = (DMoteModel) moteIndex.get(new Integer(endMote));
			if (m2 == null) {
				m2 = createNewMote(endMote);
			}
			DLinkModel dl = (DLinkModel) linkIndex.get(startMote + " "
					+ endMote);
			if (dl == null) {
				// System.out.println("Does not contain key <" + startMote + " "
				// + endMote + ">");
				dl = createNewLink(m, m2);
			}
			dl.setLinkValue(name, value);
			navigator.redrawAllLayers();
		} else {
			super.processEvent(event);
		}
	}

	public static void usage() {
		// REMOVE
		// System.err.println("usage: tos-mviz [-comm source] [-dir image_dir] message_type [message_type ...]");
		System.err.println("usage: class-monitor [-comm source]");
	}

	/**
	 * The main function of this java class
	 */

	public static void main(String[] args) throws IOException,
			FileNotFoundException, ClassNotFoundException {

		/**
		 * set a top-level window (JFrame) according to the Swing framework
		 */

		JFrame frame = new JFrame("Class Monitoring GUI");

		// REMOVE Vector packetVector = new Vector();

		// REMOVE String source = null;

		// REMOVE String dir = ".";
		// REMOVE String moteImg = "/mote.gif";

		/*
		 * REMOVE if (args.length > 0) { for (int i = 0; i < args.length; i++) {
		 * if (args[i].equals("-comm")) { source = args[++i]; break; } else {
		 * usage(); System.exit(1); } } }
		 */

		try {

			/**
			 * set a default file to output errors - TO BE MODIFIED
			 */

			PrintStream err = new PrintStream("/home/user/Desktop/err");
			System.setErr(err);

			/**
			 * input stream to read the configuration file; if it is not found
			 * in the current directory, an exception is thrown
			 */

			FileInputStream propertiesInputStream = new FileInputStream(
					System.getProperty("user.dir") + "/config.properties");

			/**
			 * the Properties object holds the configuration of the application:
			 * it's loaded from the file "config.properties", whose rows have
			 * the form "property_name"="property_value"
			 */

			Properties properties = new Properties();
			properties.load(propertiesInputStream);

			/**
			 * get the path to the folder containing the icons to be used to
			 * draw the motes
			 */

			String dir = properties.getProperty("imagesDir");

			/**
			 * get the name of the icon to be used to draw the motes on the canvas
			 */
			
			String moteImage = properties.getProperty("moteImage");
			
			/**
			 * Data from sensors, after having being uploaded on Parse
			 * repository can be retrieved through HTTP GET requests.
			 * These requests must include two HTTP Headers:
			 * 1- X-Parse-Application-Id
			 * 2- X-Parse-REST-API-Key
			 * 
			 * Values for these headers are written in the java file
			 * property
			 */
			
			String parseApplicationId=properties.getProperty("parseApplicationId");
			String parseRESTApiKey=properties.getProperty("parseRESTApiKey");
			
			/**
			 * Get the URL of the HTTP GET request to Parse repository
			 */
			
			String parseGetUrl=properties.getProperty("parseGetUrl");
			
			/**
			 * Vector whose elements are java classes representing format of packets
			 * allowed
			 * TO BE MODIFIED - ONLY ONE FORMAT
			 */
			
			Vector packetVector = new Vector();
			packetVector.add(messagesJavaClass);
			
			/**
			 * The data model, namely the set of information provided by packets
			 */
			
			DataModel model = new DataModel(packetVector);
			
			/**
			 * Instantiate DDocument, which is the main GUI of the application.
			 * Constructor requires the set of fields and links from data model,
			 * because they are used to initialized the "navigator" (a control area)
			 * within DDocument
			 */
			
			DDocument doc = new DDocument(Integer.parseInt(properties
					.getProperty("height")), Integer.parseInt(properties
					.getProperty("width")), model.fields(), model.links(), dir,
					moteImage,parseGetUrl,parseApplicationId,parseRESTApiKey);
			
			/**
			 * JWindows and JFrames consist of a number of separated overlapping "panes": among
			 * those, the contentPane is a Container that covers visible area
			 */
			
			frame.setContentPane(doc);
			
			/**
			 * Default close operation
			 */
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			/**
			 * pack(): resize frame to to the minimum size required to hold all
			 * its components
			 */

			frame.pack();

			/**
			 * show frame
			 */

			frame.setVisible(true);

			/**
			 * instantiate a new object to deal with messages from the specified
			 * source
			 */

			MessageInput input = new MessageInput(packetVector, null, doc);

			/**
			 * start this object
			 */

			input.start();
		} catch (FileNotFoundException notFoundEx) {
			System.out
					.println("ERROR: could not find the file properties \"config.properties\"");
			System.exit(1);
		} catch (SecurityException secEx) {
			System.out
					.println("ERROR: denied access to the file properties \"config.properties\"");
			System.exit(1);
		}
	}

	private void repaintAllMotes() {
		Iterator it = motes.iterator();
		while (it.hasNext()) {
			((DMoteModel) it.next()).requestRepaint();
		}
	}

	private void repaintAllLinks() {
		Iterator it = links.iterator();
		while (it.hasNext()) {
			((DLink) it.next()).repaint();
		}
	}

	// #########################################################################//
	
	/**
	 * The table model for the table holding values from motes within
	 * DDocument; it extends the swing abstract class AbstractTableModel
	 * with the three methods getRowCount,getColumnCount and getValueAt
	 * @author user
	 *
	 */

	private class DrawTableModel extends AbstractTableModel implements
			DMoteModelListener {

		private Vector fields;

		public DrawTableModel(Vector fields) {
			this.fields = fields;
		}

		// -----------------------------o
		public String getColumnName(int col) {
			switch (col) {
			case 0:
				return "X";
			case 1:
				return "Y";
			default:
				return (String) fields.elementAt(col - 2);
			}
		}

		// -----------------------------o
		public int getColumnCount() {
			return fields.size() + 2;
		}

		// -----------------------------o
		public int getRowCount() {
			return DDocument.this.motes.size();
		}

		// -----------------------------o
		
		/**
		 * Each row in the table corresponds to a mote in DDocument (in the
		 * array "motes"); there is a number of columns equal to the number
		 * of fields in the messages sent by motes (+ 2, namely X and Y columns)
		 */
		
		public Object getValueAt(int row, int col) {
			DMoteModel model = (DMoteModel) DDocument.this.motes.get(row);
			switch (col) {
			case 0:
				return "" + (int) model.getLocX();
			case 1:
				return "" + (int) model.getLocY();
			default:
				return ("" + (int) model.getValue(col - 2));
			}
		}

		// -----------------------------o
		public void shapeChanged(DMoteModel changed, int type) {
			int row = findModel(changed);
			if (row != -1) {
				fireTableRowsUpdated(row, row);
			}
			fireTableDataChanged();
		}

		// -----------------------------o
		
		/**
		 * Add a mote to the table of values from sensors
		 * @param model
		 */
		
		public void add(DMoteModel model) {
			model.addListener(this);
			int last = DDocument.this.motes.size() - 1;
			fireTableRowsInserted(last, last);
			fireTableDataChanged();
		}

		// -----------------------------o
		public void remove(DMoteModel model) {
			int row = findModel(model);
			if (row != -1) {
				fireTableRowsDeleted(row, row);
			}
			fireTableDataChanged();
		}

		// -----------------------------o
		public void updateTable() {
			fireTableDataChanged();
		}

		// -----------------------------o
		private int findModel(DMoteModel changed) {
			for (int i = 0; i < DDocument.this.motes.size(); i++) {
				if ((DMoteModel) DDocument.this.motes.get(i) == changed) {
					System.out.println((DMoteModel) DDocument.this.motes.get(i)
							+ " has changed!");
				}
				return i;
			}
			return -1;

		}
	}

	/**
	 * In order to easily create a TableModel, the easiest solution is
	 * subclassing the abstract class AbstractTableModel and overriding
	 * any behavior we want to change. By default cells are not
	 * editable.
	 * @author user
	 *
	 */
	private class MeasuresTableModel extends AbstractTableModel implements
			ActionListener {
		
		/**
		 * Actual data of the the table model are represented by a matrix of
		 * 3 rows and four columns. The columns are:
		 * 1 - X Acc: value of the acceleration along x-axis
		 * 1 - Y Acc: value of the acceleration along y-axis
		 * 1 - Z Acc: value of the acceleration along z-axis
		 */
		
		Object data[][] = new Object[3][4];
		
		/**
		 * Fixed headers of the columns
		 */
		String[] headings = new String[] { "X Acc", "Y Acc", "Z Acc",
				"updatedAt" };
		
		/**
		 * Constructor for our custom table: simply initialize all rows
		 * with null values the X,Y and Z columns and with empty string
		 * the column indicating the date of the updating of the value
		 */

		public MeasuresTableModel() {
			super();
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					data[i][j] = new Integer(0);
				}
				data[i][3] = "";
			}
			
			/**
			 * Get the last three updated values from Parse
			 * repository
			 */
			
			// retrieveMeasures();
		}
		
		/**
		 * An abstract method that must be implemented when
		 * class AbstractTableModel is subclassed; this method
		 * returns the number of rows in the table
		 */
		
		public int getRowCount() {
			return data.length;
		}
		
		/**
		 * An abstract method that must be implemented when
		 * class AbstractTableModel is subclassed; this method
		 * returns the number of columns in the table
		 */

		public int getColumnCount() {
			return data[0].length;
		}
		
		/**
		 * An abstract method that must be implemented when
		 * class AbstractTableModel is subclassed; this method
		 * returns the element of the table corresponding to
		 * given row and column
		 */
		
		public Object getValueAt(int row, int column) {
			return data[row][column];
		}
		
		/**
		 * This method is needed when AbstractTableModel is subclassed,
		 * because it gives name to headers of columns
		 */

		public String getColumnName(int column) {
			return headings[column];
		}
		
		/**
		 * Get last three updated values from sensors from Parse repository.
		 * Requests to Parse are simply HTTP GET request; response is given
		 * as JSON Object. URL of the request to Parse is read from Java
		 * property file. Here we use an HTTP Client from Apache to make
		 * HTTP GET requests
		 */

		void retrieveMeasures() {
			try {
				
				/**
				 * Create an instance of HTTP client
				 */
				
				CloseableHttpClient httpclient = HttpClients.createDefault();
				
				/**
				 * Create an instance of an HTTP GET request; URL is taken from
				 * property file
				 */
				
				HttpGet httpGet = new HttpGet(
						parseRequestURL);
				
				/**
				 * Add mandatory headers to make requests to Parse repository
				 */
				
				httpGet.addHeader(parseApplicationIdHeader,
						parseApplicationId);
				httpGet.addHeader(parseRESTApiKeyHeader,
						parseRESTApiKey);
				
				/**
				 * Execute the HTTP GET request and get an HTTP Response
				 */
				
				CloseableHttpResponse response = httpclient.execute(httpGet);
				
				/**
				 * Open a buffer to read the content associated to the HTTP
				 * Response received
				 */
				
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				
				/**
				 * Read content as a string
				 */
				String line = "";
				String content = "";
				while ((line = rd.readLine()) != null) {
					content += line;
				}
				
				/**
				 * Content is actually a JSONObject, more precisely
				 * a JSONArray with one element for each result of
				 * the query (namely for the three last updated
				 * values)
				 */
				
				JSONObject json = new JSONObject(content);
				
				/**
				 * Extract JSONArray from JSONObject; its name
				 * is "results"
				 */
				
				JSONArray results = json.getJSONArray("results");
				
				/**
				 * Parse each element in JSONArray to get X,Y and Z
				 * values of acceleration and the "update date"
				 */
				
				for (int i = 0; i < results.length(); i++) {
					JSONObject row = (JSONObject) results.get(i);
					data[i][0] = new Integer(row.getInt("X"));
					data[i][1] = new Integer(row.getInt("Y"));
					data[i][2] = new Integer(row.getInt("Z"));
					data[i][3] = row.getString("updatedAt");
				}
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			} catch (JSONException ex) {
				System.out.println(ex.getMessage());
			}
		}
		
		/**
		 * Event handler associated to the button above the table:
		 * every time the user clicks on it, we get the last three
		 * updated measures and show them in the table
		 */
		public void actionPerformed(ActionEvent e) {
			
			/**
			 * Retrieve measures
			 */
			
			retrieveMeasures();
			
			/**
			 * Method from AbstractTableModel class: notifies all
			 * listeners that all cells in the table's rows may have
			 * changed so that JTable should reload them
			 */
			
			fireTableDataChanged();
		}
	}

	private class DPanel extends JPanel {

		private DDocument doc;
		private int lastX = -1;
		private int lastY = -1;

		public DPanel(DDocument d) {
			super();
			doc = d;
			addMouseListener(new MouseAdapter() {
				private boolean withinRange(int val, int low, int high) {
					return (val >= low && val <= high);
				}

				public void mousePressed(MouseEvent e) {
					lastX = e.getX();
					lastY = e.getY();
					Iterator it = doc.motes.iterator();
					while (it.hasNext()) {
						DMoteModel model = (DMoteModel) it.next();
						if (withinRange(e.getX(), model.getLocX() - 20,
								model.getLocX() + 20)
								&& withinRange(e.getY(), model.getLocY() - 20,
										model.getLocY() + 20)) {
							selected = model;
							System.out.println("ID:" + model.getId());
							return;
						}
					}
				}

				public void mouseReleased(MouseEvent e) {
					if (doc.selected != null) {
						doc.selected = null;
						lastX = -1;
						lastY = -1;
					}
				}
			});
			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent e) {
					if (doc.selected != null) {
						if (lastY == -1) {
							lastY = e.getY();
						}
						if (lastX == -1) {
							lastX = e.getX();
						}
						int x = e.getX();
						int y = e.getY();
						int dx = x - lastX;
						int dy = y - lastY;
						lastX = x;
						lastY = y;

						selected.move(selected.getLocX() + dx,
								selected.getLocY() + dy);
					}
					doc.navigator.redrawAllLayers();
				}

			});
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			setOpaque(false);
			// System.out.println("Painting panel!");
			doc.navigator.redrawAllLayers();
		}
	}

	private class CanvasMouse extends MouseAdapter {

	}

	/**
	 * User defined AWT event
	 * 
	 * @author user
	 * 
	 */

	protected class ValueSetEvent extends AWTEvent {

		/**
		 * as suggested by Java Official API, user defined AWT events should get
		 * an ID which is higher than the "AWTEvent.RESERVED_ID_MAX"
		 */

		public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;
		private String name;
		private int value;
		private int mote;

		public ValueSetEvent(Object target, int mote, String name, int value) {
			super(target, EVENT_ID);
			this.value = value;
			this.name = name;
			this.mote = mote;
		}

		public String name() {
			return name;
		}

		public int value() {
			return value;
		}

		public int moteId() {
			return mote;
		}
	}

	/**
	 * User defined AWT event: a link between two motes has been set
	 * 
	 * @author user
	 * 
	 */

	protected class LinkSetEvent extends AWTEvent {

		public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 2;
		private String name;
		private int value;
		private int start;
		private int end;

		public LinkSetEvent(Object target, String name, int value, int start,
				int end) {
			super(target, EVENT_ID);
			this.value = value;
			this.name = name;
			this.start = start;
			this.end = end;
		}

		public String name() {
			return name;
		}

		public int value() {
			return value;
		}

		public int start() {
			return start;
		}

		public int end() {
			return end;
		}
	}
}
