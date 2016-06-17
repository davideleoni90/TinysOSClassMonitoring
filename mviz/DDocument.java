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
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import javax.swing.text.BadLocationException;

/*
 * import classes to handle http requests
 */

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.entity.*;
import org.json.*;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBaseIterators.ParentIterator;

import sun.applet.Main;

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

implements ActionListener, TableModelListener {

	/**
	 * the name of the Java classes, produced with "mig" tool, representing the
	 * messages sent by the network of motes
	 */

	static final String messagesJavaClass = "node.SensorsDataMsg";

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
	protected String hostImg;
	protected String parseGetURL;
	protected String parsePostURL;
	protected String parseApplicationId;
	protected String parseRESTApiKey;
	protected JPanel canvas;

	/**
	 * The ID of the root mote
	 */

	protected int rootMote;

	/**
	 * Image used to draw motes on canvas
	 */

	public Image motesImage;

	/**
	 * Dimension of the image chosen to represent the motes
	 */

	public Dimension motesImageDimension;

	/**
	 * Dimension of the image chosen to represent the host
	 */

	public Dimension hostImageDimension;

	/**
	 * Image used to draw host on canvas
	 */

	public Image hostImage;

	/**
	 * Array of colors used to draw paths: since they are randomly
	 * chosen, every time we draw a path we check that the color
	 * has not been used yet
	 */

	private ArrayList<Color> pathColors;

	/**
	 * Color used to draw the last path
	 */

	Color currentPathColor;

	/**
	 * A list of the links among motes, together with
	 * the value of quality of these links
	 */

	public DLinksViewer linksViewer;

	/**
	 * The table model for a table (motesTable) with 
	 * one row for each mote involved in the collection
	 * tree protocol, whose icon is shown in the canvas
	 */

	private PathsTableModel tableModel;
	private JTable motesTable;

	/**
	 * SENSORS DATA TABLE ->components for the table with data from sensors
	 */

	protected Button measuresButton;
	private JTable measuresTable;
	public int measuresTableWidth = 600;
	public int measuresTableHeight = 600;

	protected HashMap motes;
	protected HashMap links;
	private MeasuresTableModel measuresTableModel;

	/**
	 * Coordinates for the shape representing the host
	 */

	private int hostX=-1;
	private int hostY=-1;

	/**
	 * DDocument Constructor: ->initialize all its graphic elements
	 */

	public DDocument(int width, int height, int rootMote,String directory, String moteImg, String hostImg,String parseGetURL,String parsePostURL,String
			parseApplicationId, String parseRESTApiKey) {
		super();

		/**
		 * Directory where the script is executed (by the default) is the
		 * current directory
		 */

		this.directory = directory;

		/**
		 * Path to the image to be used to represent motes on the canvas
		 */

		this.moteImg = moteImg;

		/**
		 * Initialize the list with colors for paths
		 */

		this.pathColors=new ArrayList<Color>();

		/**
		 * URL of the HTTP GET Parse request
		 */

		this.parseGetURL=parseGetURL;
		
		/**
		 * URL of the HTTP POST Parse request
		 */
		
		this.parsePostURL=parsePostURL;

		/**
		 * Value for X-Parse-Application-Id
		 */

		this.parseApplicationId=parseApplicationId;

		/**
		 * Value for X-Parse-REST-API-Key
		 */

		this.parseRESTApiKey=parseRESTApiKey;

		/**
		 * ID of root mote
		 */

		this.rootMote=rootMote;

		/**
		 * Set LayoutManager for DDocument to BorderLayout: arranges components
		 * in one out of five regions: NORTH , SOUTH ,EAST , WEST ,
		 * and CENTER; each region can hold onl one Component object.
		 */

		setLayout(new BorderLayout());

		/**
		 * The architecture of Swing is designed so that one may change
		 * the "look and feel" (L&F) of his/her application's GUI:
		 *  "Look" refers to the appearance of GUI widgets (more formally,
		 *  JComponents) and "feel" refers to the way the widgets behave.
		 */

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignore) {
		}

		motes = new HashMap();
		links = new HashMap();

		/**
		 * Load images to represent motes on the canvas
		 */

		String imgName = directory + moteImg;
		motesImage= Toolkit.getDefaultToolkit().getImage(imgName);

		BufferedImage bimg;
		try {

			/**
			 * Get dimensions of the motes image
			 */

			bimg = ImageIO.read(new File(imgName));
			motesImageDimension=new Dimension(bimg.getWidth(),bimg.getHeight());
		} catch (IOException e) {
			e.printStackTrace();
		}

		/**
		 * Load images to represent host on the canvas
		 */

		imgName = directory + hostImg;
		hostImage = Toolkit.getDefaultToolkit().getImage(imgName);

		try {

			/**
			 * Get dimensions of the host image
			 */

			bimg = ImageIO.read(new File(imgName));
			hostImageDimension=new Dimension(bimg.getWidth(),bimg.getHeight());
		} catch (IOException e) {
			e.printStackTrace();
		}

		/**
		 * CANVAS START
		 * ->reate the canvas where motes and links will be displayed
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
		 * Add the canvas at the center of DDocument.
		 */

		add(canvas, BorderLayout.CENTER);

		/**
		 * ComponentListener is the interface an object has to implement in
		 * order to receive events from a component; here we create an anonymous
		 * inner class
		 */

		/*canvas.addComponentListener(new ComponentListener() {
		public void componentResized(ComponentEvent e) {
			//linksViewer.redrawAllLayers();
		}

		public void componentHidden(ComponentEvent arg0) {
		}

		public void componentMoved(ComponentEvent arg0) {
		}

		public void componentShown(ComponentEvent arg0) {

		}
	});*/

		/**
		 * CANVAS END
		 */

		/**
		 * WEST AREA START:
		 * ->create the panel for the west area of DDocument
		 */

		JPanel west = new JPanel();

		/**
		 * Set the border for west area
		 */

		west.setBorder(BorderFactory.createLineBorder(Color.GRAY,3));

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

		/**
		 * WEST AREA END
		 */

		/**
		 * Create the lateral links-viewer; put it inside a JPanel
		 * with a titled border
		 */

		JPanel linksViewerPanel=new JPanel();
		linksViewerPanel.setBorder(BorderFactory.createTitledBorder("Links Viewer"));

		/**
		 * Create the links-viewer
		 */

		linksViewer = new DLinksViewer(this);

		/**
		 * Add the links-viewer to the its panel and add it to the
		 * west area
		 */

		linksViewerPanel.add(linksViewer);
		west.add(linksViewerPanel);

		/**
		 * RETRIEVE MEASURES AREA START:
		 * -> add a panel containing an editable text field and a button:
		 * the user sets a number in the text fields and then clicks the
		 * button in order to retrieve the specified number of most
		 * recently updated values on Parse
		 */

		JPanel retrieveMeasuresPanel=new JPanel(true);

		/**
		 * Set margin
		 */

		retrieveMeasuresPanel.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));

		/**
		 * We use the BoxLayout for this panel, but with left-to-right
		 * alignment
		 */

		retrieveMeasuresPanel.setLayout(new BoxLayout(retrieveMeasuresPanel, BoxLayout.X_AXIS));

		/**
		 * Add a label specifying the functionality of the button
		 */

		JLabel retrieveMeasuresLabel=new JLabel("Click to retrieve last updated measures");
		retrieveMeasuresPanel.add(retrieveMeasuresLabel);

		/**
		 * Add an editable text field for the user to enter the number
		 * of measures he wants to retrieve from Parse
		 */

		JTextField numberOfMeasuresField=new JTextField("1");
		numberOfMeasuresField.setPreferredSize(new Dimension(30,40));
		numberOfMeasuresField.setMaximumSize(new Dimension(40,30));
		numberOfMeasuresField.setHorizontalAlignment(JTextField.CENTER);
		retrieveMeasuresPanel.add(numberOfMeasuresField);

		/**
		 * Add space between textfield and button
		 */

		retrieveMeasuresPanel.add(Box.createRigidArea(new Dimension(20,0)));

		/**
		 * Create the button that has to be clicked in order to retrieve measures
		 * from Parse
		 */

		measuresButton = new Button("Retrieve");

		/**
		 * Add the button to lateral control area of DDocument
		 */

		retrieveMeasuresPanel.add(measuresButton);

		west.add(retrieveMeasuresPanel);

		/**
		 * MEASURES TABLE START:
		 * ->create a new instance of the table showing measures from sensors stored on Parse.
		 * First create its data model and then instantiate the table.
		 */

		measuresTableModel = new MeasuresTableModel();
		measuresTable = new JTable(measuresTableModel);
		
		/**
		 * This is necessary to have horizontal scrollbar
		 */
		
		measuresTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		/**
		 * Insert table in a panel with titled border
		 */

		JPanel measuresTablePanel= new JPanel();
		measuresTablePanel.setBorder(BorderFactory.createTitledBorder("Measures Table"));

		/**
		 * Make table sortable
		 */

		measuresTable.setAutoCreateRowSorter(true);

		/**
		 * Set the measuresTable as the listener for button events
		 */

		measuresButton.addActionListener(measuresTableModel);

		/**
		 * Set the measuresTable as the listener for text field events
		 */

		numberOfMeasuresField.getDocument().addDocumentListener(measuresTableModel);

		/**
		 * Create a scrollable pane out of the table with measures
		 * retrieved from Parse repository
		 */

		JScrollPane measuresScroller = new JScrollPane(measuresTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		measuresTable.setFillsViewportHeight(true);
		Dimension dimension=measuresTable.getPreferredSize();
		measuresScroller.setPreferredSize(new Dimension(dimension.width,measuresTable.getRowHeight()*10));

		/**
		 * Add scrollable pane
		 */

		measuresTablePanel.add(measuresScroller);
		west.add(measuresTablePanel);

		/**
		 * MOTES TABLE START
		 * ->create a new instance of the table showing motes involved
		 * into the data collection protocol.
		 * First create its data model and then instantiate the table.
		 */

		/**
		 * Create a title panel to host this table
		 */

		JPanel motesTablePanel=new JPanel();
		motesTablePanel.setBorder(BorderFactory.createTitledBorder("Paths Table"));

		/**
		 * Create a new instance of the table model to represent
		 * the set of motes in the WSN
		 */

		tableModel = new PathsTableModel();

		/**
		 * Create a new table out of the preceding table model
		 */

		motesTable = new JTable(tableModel);
		motesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		motesTable.setFillsViewportHeight(true);

		/**
		 * Set a custom renderer for the color cells in order to
		 * draw the background color
		 */

		motesTable.getColumnModel().getColumn(2).setCellRenderer(new CustomCellRenderer());
		tableModel.addTableModelListener(this);
		/**
		 * Create a scrollable pane out of the table with motes
		 */

		JScrollPane motesScroller = new JScrollPane(motesTable);
		motesScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		dimension=motesTable.getPreferredSize();
		motesScroller.setPreferredSize(new Dimension(dimension.width,motesTable.getRowHeight()*10));

		/**
		 * Add motes table
		 */

		motesTablePanel.add(motesScroller);
		west.add(motesTablePanel);

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
		enableEvents(NewMoteEvent.EVENT_ID);
	}

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

	private DMoteModel createNewMote(int moteID,boolean isProducer) {
		DMoteModel m = new DMoteModel(moteID, rand, this,isProducer);
		motes.put(new Integer(moteID), m);
		//tableModel.add(m);
		return m;
	}

	private void drawMote(int moteID,Graphics g){
		DShape m = new DMote(((DMoteModel)motes.get(moteID)), this);
		m.paintShape(g);
	}

	/**
	 * Draw the link corresponding to the given link model using the
	 * given color and adding the given index
	 * @param model
	 * @param g
	 * @param color
	 */

	private void drawLink(DLinkModel model,Graphics g,Color color,int index){

		/**
		 * Create a new shape to draw the link on the canvas
		 */

		DLink lnk = new DLink(model, this,color,index);

		/**
		 * Draw the new link
		 */

		lnk.paintShape(g);

	}

	/**
	 * Create a new event indicating that the value of a field of messages
	 * that has changed, then post the event in the queue of events of the
	 * applet (from where they will be dispatched to proper handler causing
	 * some components of the GUI to be modified)
	 * @param moteID
	 * @param name
	 * @param value
	 */

	public void setMoteValue(int moteID, String name, int value) {

		/**
		 * Create a new event, specifying DDocument as the container
		 * that will be notified of the event as it will be processed
		 * by AWT framework
		 */

		ValueSetEvent vsv = new ValueSetEvent(this, moteID, name, value);

		/**
		 * Get the EventQueue associated to the current applet
		 */

		EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();

		/**
		 * Post event to the queue
		 */

		eq.postEvent(vsv);
	}

	/**
	 * Create a new event indicating that a new mote has been detected,
	 * then post the event in the queue of events of the applet 
	 * (from where they will be dispatched to proper handler causing
	 * some components of the GUI to be modified) 
	 * @param start
	 * @param end
	 * @return
	 */

	public void setNewMote(int moteID){

		/**
		 * Create a new event, specifying DDocument as the container
		 * that will be notified of the event as it will be processed
		 * by AWT framework
		 */

		NewMoteEvent nme=new NewMoteEvent(this, moteID);

		/**
		 * Get the EventQueue associated to the current applet
		 */

		EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();

		/**
		 * Post event to the queue
		 */

		eq.postEvent(nme);
	}

	private DLinkModel createNewLink(DMoteModel start, DMoteModel end) {
		DLinkModel linkModel = new DLinkModel(start, end,this);
		links.put(start.getId()+"->"+end.getId(), linkModel);
		return linkModel;
	}

	public void setLinkValue(int linkQuality,int startMote, int endMote,boolean isProducer) {
		LinkSetEvent linkSetEvent = new LinkSetEvent(this,linkQuality,startMote,
				endMote,isProducer);
		EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
		eq.postEvent(linkSetEvent);
	}

	/**
	 * Add a new path to the Paths Table (if not already there)
	 * @param path
	 */

	public void setNewPath(int[] path){

		/**
		 * Check if already in the table
		 */

		if(tableModel.containsPath(path)){

			/**
			 * In this case we only have to update the path
			 */

			tableModel.updatePath(path);
		}
		else{

			/**
			 * Randomly assign a color to the new path
			 * (always choose new color)
			 */

			float r = rand.nextFloat();
			float g = rand.nextFloat();
			float b = rand.nextFloat();
			Color backgroundColor=new Color(r,g,b);
			while(pathColors.contains(backgroundColor)){
				r = rand.nextFloat();
				g = rand.nextFloat();
				b = rand.nextFloat();
				backgroundColor=new Color(r,g,b);
			}
			pathColors.add(backgroundColor);
			currentPathColor=backgroundColor;
			tableModel.add(path,currentPathColor);
		}
	}

	/**
	 * Event handler for AWTEvents whose target is DDocument
	 */

	protected void processEvent(AWTEvent event) {

		/**
		 * We only deal with new links set; we don't care
		 * about other events
		 */

		if (event instanceof LinkSetEvent) {

			/**
			 * Parse event
			 */

			LinkSetEvent linkSetEvent = (LinkSetEvent) event;
			int linkQuality = linkSetEvent.linkQuality();
			int startMote = linkSetEvent.startMote();
			int endMote = linkSetEvent.endMote();
			boolean isProducer=linkSetEvent.isProducer;

			/**
			 * Check if the two motes composing the link have been already sensed:
			 * if not, draw one DMote shape each to represent them
			 */

			DMoteModel m1 = (DMoteModel) motes.get(new Integer(startMote));
			if (m1==null) {
				m1 = createNewMote(startMote,isProducer);
			}

			DMoteModel m2 = (DMoteModel) motes.get(new Integer(endMote));
			if (m2 == null) {

				/**
				 * Endpoint of a link can't be a producer because we
				 * consider directed link, so second parameter has to
				 * be false
				 */

				m2 = createNewMote(endMote,false);

				/**
				 * When the root mote is created, also set coordinates for
				 * the shape representing the host to which it's connected;
				 * this has to be done once and we try to draw the host close
				 * to the root mote
				 */

				if((endMote==rootMote)&&(hostX==-1)){

					boolean overlapping=true;
					while(overlapping){

						/**
						 * X coordinate w.r.t. to the container; we don't want the mote to have
						 * its center exactly on the border, so adjust x with the width of the
						 * image used to represent the mote
						 */

						hostX = (int) m2.getLocX()+rand.nextInt((int)motesImageDimension.getWidth()+(int)hostImageDimension.getWidth()+50);

						boolean foundX=true;

						/**
						 * Check that the x coordinate is not beyond the limit of the canvas
						 */

						if((int)(hostX+hostImageDimension.getWidth())>=canvas.getWidth()){
							continue;
						}
						Iterator moteIterator=motes.entrySet().iterator();
						while(moteIterator.hasNext()){
							DMoteModel current=((DMoteModel)((Map.Entry)moteIterator.next()).getValue());
							if((current.x-(int) motesImageDimension.getWidth())<=hostX && hostX<=(current.x+(int) motesImageDimension.getWidth())){
								foundX=false;
								break;
							}
						}
						/**
						 * Y coordinate w.r.t. to the container; we don't want the mote to have
						 * its center exactly on the border, so adjust y with the height of the
						 * image used to represent the mote
						 */

						boolean foundY=true;
						hostY = (int) m2.getLocY()+rand.nextInt((int)motesImageDimension.getHeight()+(int)hostImageDimension.getHeight()+50);

						/**
						 * Check that the y coordinate is not beyond the limit of the canvas
						 */

						if((int)(hostY+hostImageDimension.getHeight())>=canvas.getHeight()){
							continue;
						}
						moteIterator=motes.entrySet().iterator();
						while(moteIterator.hasNext()){
							DMoteModel current=((DMoteModel)((Map.Entry)moteIterator.next()).getValue());
							if((current.y-(int) motesImageDimension.getHeight())<=hostY && hostY<=(current.y+(int) motesImageDimension.getHeight())){
								foundY=false;
								break;
							}
						}
						if(!foundY&&!foundX)
							continue;
						else {
							overlapping=false;
						}
					}
				}
			}

			/**
			 * Check if this link between the motes has been already sensed:
			 * if note, draw one DLink shape to represent it
			 */

			DLinkModel dl = (DLinkModel) links.get(startMote + "->"
					+ endMote);
			if (dl == null) {
				dl = createNewLink(m1, m2);
			}

			/**
			 * ID of the link is START_MOTE->END_MOTE
			 */

			String link=new Integer(startMote).toString()+"->"+new Integer(endMote).toString();

			/**
			 * Update the value of the link in the corresponding row of the
			 * links table and in the links viewer
			 */

			dl.setLinkValue(link,linkQuality);
			linksViewer.updateLink(dl);

			/**
			 * Since the link quality is BIDIRECTIONAL (it's computed 
			 * from the the in-bound and out-bound qualitites of both 
			 * motes in the link), the value has to updated not only 
			 * for link A->B, but also for link B->A if a message has
			 * already crossed the link in this direction too
			 */

			dl = (DLinkModel) links.get(endMote + "->"
					+ startMote);
			if (dl != null) {
				dl.setLinkValue(new Integer(endMote).toString()+"->"+new Integer(startMote).toString(),linkQuality);
				linksViewer.updateLink(dl);
			}

			/**
			 * Finally redraw the whole canvas with motes
			 * and links
			 */

			redrawCanvas();
		}
	}

	/**
	 * This method draws all the motes and only the links belonging
	 * to the path currently selected in the Paths Model
	 */

	void redrawCanvas(){

		/**
		 * A BufferedImage is a subclass of Image with accessible buffer of image data.
		 * Below constructor takes the following parameters:
		 * 1 - width of the created image
		 * 2 - height of the created image
		 * 3 - type of the created image; "TYPE_INT_ARGB" represents an image with 8-bit RGBA color
		 * 	   components packed into integer pixels
		 */

		Image offscreen = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);

		/**
		 * Graphics objects represent drawing areas (a.k.a. graphics context): contains methods to
		 * draw in this area. It's called "context" because it includes information about the drawing
		 * area. In particular BufferedImage returns a Graphics2D object, which can be
		 * used to draw either shapes or texts or images
		 */

		Graphics g = offscreen.getGraphics();
		Graphics2D g2d = (Graphics2D) g;

		/**
		 * Clears the specified rectangle filling it with the background color of the
		 * current drawing surface
		 */

		g2d.clearRect(0, 0,canvas.getWidth(), canvas.getHeight());

		/**
		 * Fills the specified rectangle using context's current color
		 */

		g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

		/**
		 * Draw all motes
		 */

		Iterator motesIterator=motes.keySet().iterator();
		while(motesIterator.hasNext()){
			drawMote(((Integer)motesIterator.next()).intValue(), g2d);
		}

		/**
		 * Get the color of the path to draw
		 */

		Color pathColor=(Color)(tableModel.getValueAt(tableModel.selected, 2));

		/**
		 * Draw a link only if belonging to the current selected path
		 */

		Iterator linksIterator=links.entrySet().iterator();
		while(linksIterator.hasNext()){
			java.util.Map.Entry entry=(java.util.Map.Entry)linksIterator.next();
			if(tableModel.belongsToPath((DLinkModel)(entry.getValue()))!=-1)
				drawLink((DLinkModel)(entry.getValue()), g2d,pathColor,tableModel.belongsToPath((DLinkModel)(entry.getValue()))+1);
		}

		canvas.getGraphics().drawImage(offscreen, 0, 0, this);
	}

	public static void usage() {
		System.out.println("usage: class-monitor [-comm source]");
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

		try {

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
			 * Get the ID of the root node
			 * NOTE: it has to be the same as in the header file "Acceleration.h"
			 */

			String  rootMote=properties.getProperty("rootMote");

			/**
			 * Get the path to the folder containing the icons to be used to
			 * draw the motes
			 */

			String dir = properties.getProperty("imagesDir");

			/**
			 * Get the name of the icon to be used to draw the motes on the canvas
			 */

			String moteImage = properties.getProperty("moteImage");

			/**
			 * Get the name of the icon to be used to draw the motes on the canvas
			 */

			String hostImage = properties.getProperty("hostImage");

			/**
			 * If motes image does not exist, exit
			 */

			File f = new File(dir+moteImage);
			if(!f.exists() || f.isDirectory()) {
				System.out.println("ERROR: could not find mote image file "+dir+moteImage);
				return;
			}

			/**
			 * If host image does not exist, exit
			 */

			f = new File(dir+hostImage);
			if(!f.exists() || f.isDirectory()) {
				System.out.println("ERROR: could not find host image file "+dir+hostImage);
				return;
			}

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
			 * Get the URL of the HTTP POST request to Parse repository
			 */

			String parsePostUrl=properties.getProperty("parsePostUrl");

			DDocument doc = new DDocument(Integer.parseInt(properties
					.getProperty("height")), Integer.parseInt(properties
							.getProperty("width")),Integer.parseInt(rootMote), dir,
							moteImage,hostImage,parseGetUrl,parsePostUrl,parseApplicationId,parseRESTApiKey);

			/**
			 * JWindows and JFrames consist of a number of separated overlapping "panes": among
			 * these, the contentPane is a Container that covers visible area
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
			 * instantiate a new object to deal with messages coming from motes
			 */

			MessageInput input = new MessageInput(null, doc);

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

	/**
	 * Here's the model for the "PathsTable", namely a table with one
	 * row for each path from one "producer mote" (a mote with embedded 
	 * accelerometer) to the the "root mote" (the mote connected to the
	 * host running the applet)
	 *
	 */

	private class PathsTableModel extends AbstractTableModel implements
	DMoteModelListener {

		/**
		 * Actual data of the the table model are represented by a matrix with a
		 * number of rows and two columns. The columns are:
		 * - Origin: ID of the producer mote
		 * - Hopscount: number of motes between the the producer and the root
		 * - Timestamp of the last message sent by the producer mote (this can
		 * 	 be useful to check whether the mote is up or if it's down)
		 */

		ArrayList<Object[]>data = new ArrayList<Object[]>();

		/**
		 * Fixed headers of the columns. Beside the two above columns containing
		 * data, there's a column dedicated to the "color" of the path
		 * on the canvas and one checkbox to be clicked in order to draw the
		 * path
		 */

		String[] columnNames = new String[] { "Origin", "Hopscount","Last message","Color","Selected"};

		/**
		 * Index of the row corresponding to currently selected row
		 */

		int selected=0;

		public PathsTableModel() {
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public int getColumnCount() {
			return 4;
		}

		public int getRowCount() {
			return data.size();
		}

		public Object getValueAt(int row, int col) {

			/**
			 * In the first column only show the origin
			 * of the path (producer mote)
			 */

			switch (col) {
			case 0:
				return ((List<Integer>)data.get(row)[col]).get(0);
			default:
				return data.get(row)[col];
			}
		}

		@Override
		public Class getColumnClass(int column) {
			switch (column) {
			case 0:
				return Integer.class;
			case 1:
				return Integer.class;
			case 2:
				return Color.class;
			case 3:
				return Boolean.class;
			default:
				return String.class;
			}
		}

		/**
		 * Make last column selectable by the user
		 */

		@Override
		public boolean isCellEditable(int row, int col) {
			switch (col) {
			case 3:
				return true;
			default:
				return false;
			}
		}

		/**
		 * When the checkbox is selected, draw the corresponding path
		 * and deselect the other because we want ONLY ONE PATH SHOWN
		 * AT A TIME 
		 */

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex){

			switch (columnIndex) {
			case 3:

				/**
				 * Set the "selected" field to the selected row
				 */

				selected=rowIndex;
				data.get(rowIndex)[3]=Boolean.TRUE;

				/**
				 * Reset all the other checkboxes
				 */

				for(int i=0;i<getRowCount();i++){
					if(i!=rowIndex)
						data.get(i)[3]=Boolean.FALSE;
				}

				/**
				 * Notify changes
				 */

				fireTableCellUpdated(rowIndex, columnIndex);
				fireTableRowsUpdated(0, getRowCount()-1);
				break;
			default:
				break;
			}
		}

		/**
		 * Method to check if a link belongs to the currently
		 * selected path. It returns the index of the link
		 * within the path if present, -1 otherwise
		 */

		public int belongsToPath(DLinkModel linkModel){

			/**
			 * If the start mote (we consider only directed link)
			 * is in the list of motes of the selected path, and
			 * the next element in the list is the other extreme
			 * of the link, then the link belongs to the path
			 */

			ArrayList<Integer> pathArrayList=(ArrayList<Integer>)data.get(selected)[0];
			int index=pathArrayList.indexOf(new Integer(linkModel.m1.getId()));
			if((index!=-1)&&(pathArrayList.get(index+1)==linkModel.m2.getId())){
				return index;
			}
			return -1;
		}

		/**
		 * Method to check if a path is already stored in the
		 * table
		 */

		public boolean containsPath(int[] path){

			int origin=path[0];

			/**
			 * Go through all the paths and stop if one whose origin
			 * coincides with the one of the provided path is found
			 */

			Iterator<Object[]> rowsIterator=data.iterator();
			while(rowsIterator.hasNext()){
				if(((List<Integer>)(rowsIterator.next()[0])).get(0)==origin)
					return true;
			}
			return false;
		}

		/**
		 * Update a path from producer to root
		 * @param path
		 */

		public void updatePath(int[] path){

			int producer=path[0];

			/**
			 * Get the path corresponding to the given origin and
			 * update it
			 */

			Iterator<Object[]> rowsIterator=data.iterator();
			int rowIndex=0;
			while(rowsIterator.hasNext()){

				Object[] rowValue=rowsIterator.next();
				if(((List<Integer>)(rowValue[0])).get(0)==producer){

					/**
					 * Create the updated list of motes along the
					 * path from the given parameter
					 */

					ArrayList<Integer> pathList=new ArrayList();
					for(int i=0;i<path.length;i++){
						pathList.add(new Integer(path[i]));
					}

					/**
					 * Add the root mote
					 */

					pathList.add(rootMote);
					rowValue[0]=pathList;
					rowValue[1]=path.length;
					fireTableRowsUpdated(rowIndex, rowIndex);
					return;
				}
				++rowIndex;
			}
		}

		public void shapeChanged(DMoteModel changed) {

			/**
			 * Find the row in the table corresponding to the given
			 * mote model
			 */

			int row = findModel(changed);
			if (row != -1) {

				/**
				 * Notifies all listeners that rows in the range [firstRow, lastRow],
				 * inclusive, have been updated; here only the row corresponding to
				 * the mote has to be updated
				 */

				fireTableRowsUpdated(row, row);
			}
			fireTableDataChanged();
		}

		/**
		 * Add a new row to the PathsTable
		 */

		public void add(int[] path,Color background) {
			//model.addListener(this);

			/**
			 * Turn the int array into a list (it's easier
			 * to check if the path contains one link)
			 */

			ArrayList<Integer> pathList=new ArrayList();
			for(int i=0;i<path.length;i++){
				pathList.add(new Integer(path[i]));
			}

			/**
			 * Add the root as last element
			 */

			pathList.add(rootMote);

			/**
			 * If the table is empty, show the new path,
			 * else keep showing the last selected path
			 */

			if(getRowCount()==0)
				data.add(new Object[]{pathList,path.length,background,true});
			else
				data.add(new Object[]{pathList,path.length,background,false});
			fireTableDataChanged();
		}

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

		/**
		 * Get the row in the motes table corresponding to
		 * the given mote model
		 */

		private int findModel(DMoteModel changed) {
			for (int i = 0; i < DDocument.this.motes.size(); i++) {
				return i;
			}
			return -1;

		}
	}

	/**
	 * In order to create a TableModel, the easiest solution is
	 * subclassing the abstract class AbstractTableModel and overriding
	 * any behavior we want to change. By default cells are not
	 * editable.
	 * @author user
	 *
	 */

	private class MeasuresTableModel extends AbstractTableModel implements
	ActionListener,DocumentListener {

		/**
		 * Actual data of the the table model are represented by a matrix with a
		 * number of rows and five columns. The columns are:
		 * - X Acc: value of the acceleration along x-axis
		 * - Y Acc: value of the acceleration along y-axis
		 * - Z Acc: value of the acceleration along z-axis
		 * - MoteID: ID of the mote where the values were collected by the accelerometer
		 * - updatedA: timestamp of the moment when data were uploaded on Parse
		 * 
		 * We use a list of arrays of Object to represent data, because we want to let
		 * the user get any number of data uploaded on Parse; each array of Object 
		 * represents a row of the MeasureTable
		 */

		ArrayList<Object[]>data = new ArrayList<Object[]>();

		/**
		 * Fixed headers of the columns
		 */

		String[] columnNames = new String[] { "X Acc", "Y Acc", "Z Acc",
				"updatedAt","Origin" };

		/**
		 * The number of last updated measures to retrieve from Parse on
		 * next user request; by default it's 1, but can be changes by
		 * the user editing the value in the dedicated text field (see
		 * above)
		 */

		String limit="1";

		/**
		 * Constructor for our custom table: simply initialize all rows
		 * with null values the X,Y and Z columns and with empty string
		 * the column indicating the date of the updating of the value
		 */

		public MeasuresTableModel() {
			super();
		}

		/**
		 * An abstract method that must be implemented when
		 * class AbstractTableModel is subclassed; this method
		 * returns the number of rows in the table
		 */

		public int getRowCount() {
			return data.size();
		}

		/**
		 * An abstract method that must be implemented when
		 * class AbstractTableModel is subclassed; this method
		 * returns the number of columns in the table
		 */

		public int getColumnCount() {
			return columnNames.length;
		}

		/**
		 * An abstract method that must be implemented when
		 * class AbstractTableModel is subclassed; this method
		 * returns the element of the table corresponding to
		 * given row and column
		 */

		public Object getValueAt(int row, int column) {
			return data.get(row)[column];
		}

		/**
		 * This method is needed when AbstractTableModel is subclassed,
		 * because it gives name to headers of columns
		 */

		public String getColumnName(int column) {
			return columnNames[column];
		}

		/**
		 * Override this method in order to properly implement sorting;
		 * when a column is sorted, sorting has to be consistent with 
		 * the type of values inside the column
		 */

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (data.isEmpty()) {
				return Object.class;
			}
			return getValueAt(0, columnIndex).getClass();
		}

		/**
		 * Get last updated values from sensors from Parse repository.
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
				 * property file and the actual number of values to retrieve is
				 * added
				 */

				HttpGet httpGet = new HttpGet(
						parseGetURL+limit);

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
					data.add(new Object[]{row.getInt("X"),row.getInt("Y"),row.getInt("Z"),row.getString("updatedAt"),row.getInt("Origin")});
				}
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			} catch (JSONException ex) {
				System.out.println(ex.getMessage());
			}
		}

		/**
		 * Event handler associated to the button
		 */

		public void actionPerformed(ActionEvent e) {

			/**
			 * When the button is clicked, retrieve a number of data from
			 * Parse as specified by the text field
			 */

			retrieveMeasures();

			/**
			 * Method from AbstractTableModel class: notifies all
			 * listeners that all cells in the table's rows may have
			 * changed so that JTable should reload them
			 */

			fireTableDataChanged();
		}

		/**
		 * Event handler from DocumentListener interface: it's necessary
		 * to get the value inserted by the user in the text field
		 */

		@Override
		public void changedUpdate(DocumentEvent arg0) {
		}

		/**
		 * When a value is written by the user in the text field, check if
		 * it's a number: if so, store the value, otherwise do nothing
		 */

		@Override
		public void insertUpdate(DocumentEvent arg0) {
			try {
				int length=arg0.getDocument().getLength();
				String value=arg0.getDocument().getText(0, length);
				try{
					Integer.parseInt(value);
					limit=value;
				}
				catch (NumberFormatException e) {

					/**
					 * The text is not a number: keep the
					 * default value for "limit" (1)
					 */
					limit="1";
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void removeUpdate(DocumentEvent arg0) {
		}
	}

	/**
	 * Extend JPanel class to include reference to the main DDocument
	 * object
	 *
	 */

	private class DPanel extends JPanel {

		private DDocument doc;

		public DPanel(DDocument d) {
			super();
			doc = d;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			setOpaque(false);
		}

		public void paint(Graphics g) {
		}
	}

	/**
	 * User defined AWT event: it's generated every time a new value
	 * of a field in a received message is read
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

		/**
		 * Extend AWTEvent class with some fields that help to better
		 * describe the generated event. These are:
		 * - name of the field the value is referred to
		 * - actual value of the field
		 * - mote from which the message with the current value was
		 *   received
		 */

		private String name;
		private int value;
		private int mote;

		public ValueSetEvent(Object target, int mote, String name, int value) {

			/**
			 * Constructor of class AWTEvent takes two parameters:
			 * - the object where the event originated
			 * - ID of the event
			 */

			super(target, EVENT_ID);

			/**
			 * Set specific parameters of this event
			 */

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
	 * User defined AWT event: it's generated every time a new mote
	 * appears along the path of a message from leaf to root of the
	 * tree network
	 * 
	 * @author user
	 * 
	 */

	protected class NewMoteEvent extends AWTEvent {

		/**
		 * as suggested by Java Official API, user defined AWT events should get
		 * an ID which is higher than the "AWTEvent.RESERVED_ID_MAX"
		 */

		public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;

		/**
		 * Extend AWTEvent class with some fields that help to better
		 * describe the generated event. These are:
		 * - name of the field the value is referred to
		 * - actual value of the field
		 * - mote from which the message with the current value was
		 *   received
		 */

		private int moteID;

		public NewMoteEvent(Object target, int moteID) {

			/**
			 * Constructor of class AWTEvent takes two parameters:
			 * - the object where the event originated
			 * - ID of the event
			 */

			super(target, EVENT_ID);

			/**
			 * Set specific parameters of this event
			 */

			this.moteID = moteID;
		}

		public int moteID() {
			return moteID;
		}
	}

	/**
	 * User defined AWT event: there's a link between two motes
	 * 
	 * @author user
	 * 
	 */

	protected class LinkSetEvent extends AWTEvent {

		public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 2;
		private int linkQuality;
		private int startMote;
		private int endMote;

		/**
		 * If true, startMote is a producer
		 */

		private boolean isProducer;

		public LinkSetEvent(Object target,int linkQuality, int startMote,
				int endMote,boolean isProducer) {
			super(target, EVENT_ID);
			this.linkQuality = linkQuality;
			this.startMote = startMote;
			this.endMote = endMote;			
			this.isProducer=isProducer;
		}

		public int linkQuality() {
			return linkQuality;
		}

		public int startMote() {
			return startMote;
		}

		public int endMote() {
			return endMote;
		}
	}

	/**
	 * User defined AWT event: it's generated every time a new message
	 * coming from the network of motes is received by the host running
	 * this applet
	 * 
	 * @author user
	 * 
	 */

	protected class NewMessageEvent extends AWTEvent {

		/**
		 * as suggested by Java Official API, user defined AWT events should get
		 * an ID which is higher than the "AWTEvent.RESERVED_ID_MAX"
		 */

		public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;

		/**
		 * Extend AWTEvent class with some fields that help to better
		 * describe the generated event. These are:
		 * - quality of the link
		 * - sender of the message
		 * - recipient of the message
		 */

		private int linkQuality;
		private int startMote;
		private int endMote;

		public NewMessageEvent(Object target, int linkQuality, int startMote,int endMote) {

			/**
			 * Constructor of class AWTEvent takes two parameters:
			 * - the object where the event originated
			 * - ID of the event
			 */

			super(target, EVENT_ID);

			/**
			 * Set specific parameters of this event
			 */

			this.linkQuality = linkQuality;
			this.startMote = startMote;
			this.endMote = endMote;

			CustomCellRenderer backgroundCellRenderer=new CustomCellRenderer();
			motesTable.getColumnModel().getColumn(3).setCellRenderer(backgroundCellRenderer);


		}

		/**
		 * Methods to retrieve parameters of the event
		 */

		public int linkQuality() {
			return linkQuality;
		}

		public int startMote() {
			return startMote;
		}

		public int endMote() {
			return endMote;
		}
	}

	public class CustomCellRenderer extends JLabel implements TableCellRenderer  {

		public CustomCellRenderer() {
			setOpaque(true); //MUST do this for background to show up.
		}

		public Component getTableCellRendererComponent(
				JTable table, Object value,
				boolean isSelected, boolean hasFocus,
				int row, int column) {

			/**
			 * Set the background of the "Color" cell to the
			 * current background color
			 */

			setBackground((Color)(tableModel.getValueAt(row, column)));
			return this;
		}
	}

	/**
	 * When a new path is selected, draw it
	 */

	@Override
	public void tableChanged(TableModelEvent arg0) {
		if(arg0.getColumn()==3){
			redrawCanvas();
		}

	}

	public int getHostX() {
		return hostX;
	}

	public int getHostY() {
		return hostY;
	}
}
