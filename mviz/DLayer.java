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

// DDocument.java

import java.awt.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.util.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.*;

import java.awt.image.*;

// Standard imports for XML
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

/**
 * Class representing one layer in the navigator.
 * In the original version a layer comprises a checkbox, a label and
 * two buttons to move the layer up or down within the navigator
 * @author user
 *
 */

public class DLayer extends JPanel implements ActionListener {
	
	/**
	 * Constants representing the three different types of layers in the navigator
	 */

	public static final int MOTE = 0;
	public static final int LINK = 1;
	public static final int FIELD = 2;
	
	/**
	 * Three colors, one per type of layer in the navigator
	 */
	
	private static final Color[] COLORS = { new Color(231, 220, 206),
			new Color(250, 210, 99), new Color(209, 230, 179) };

	private int type;
	protected int index;
	protected int zIndex;
	protected int z_index = 0;
	private ArrayList layer = new ArrayList();

	private JLabel label;
	
	/**
	 * CheckBox to display the layer in the navigator
	 */
	
	private JCheckBox check;
	
	/**
	 * Labels for the options in the ComboBox of each layer:for each type of DLayer, there's
	 * a specific set of labels, respectively MOTE("circle", "img", "txt"), LINK("line", "line+label", "label")
	 * and FIELD ("color 256", "color 1024", "color 4096", "color 16384").
	 */
	
	private String[][] DISPLAYS = { { "circle", "img", "txt" },
			{ "line", "line+label", "label" },
			{ "color 256", "color 1024", "color 4096", "color 16384" } };
	
	/**
	 * A ComboBox to choose the option for each type of layer
	 */
	
	private JComboBox displays;
	
	/**
	 * List of motes
	 */

	private ArrayList models;
	
	/**
	 * List of links
	 */
	
	private ArrayList linkModels;
	
	/**
	 * TO BE CHECKED (???)
	 */
	
	protected int paintMode = 0;
	// Values chosen for COLOR so that readings can be right shifted
	// that many bits to be in range 0-255
	static public final int COLOR_256 = 0;
	static public final int OVAL = 1;
	static public final int COLOR_1024 = 2;
	static public final int IMG = 3;
	static public final int COLOR_4096 = 4;
	static public final int TXT_MOTE = 5;
	static public final int COLOR_16384 = 6;
	static public final int LINE = 7;
	static public final int LABEL = 8;
	static public final int LINE_LABEL = 9;
	
	/**
	 * The navigator each DLayer belongs to
	 */
	
	protected DNavigate navigator;
	
	/**
	 * Name of the layer
	 */
	
	private String name;
	
	/**
	 * Parent container of the DLayer
	 */
	
	private DDocument parent;
	
	/**
	 * Constructor for DLayer
	 * @param zIndex
	 * @param index
	 * @param label
	 * @param type
	 * @param parent
	 * @param models
	 * @param navigator
	 */
	
	public DLayer(int zIndex, int index, String label, int type,
			DDocument parent, ArrayList models, DNavigate navigator) {
  		this.parent = parent;
		this.type = type;
		this.models = models;
		this.zIndex = zIndex;
		this.index = index;
		this.navigator = navigator;
		this.name = label;
		
		/**
		 * Motes are represented with ovals, links are represented with lines
		 */
		
		if (type == MOTE) {
			this.paintMode = OVAL;
		} else if (type == LINK) {
			this.paintMode = LINE;
		}
		
		/**
		 * SpringLayout lays out the children of its associated container according to a set of constraints
		 * between edges of components
		 */

		SpringLayout layout = new SpringLayout();

		/**
		 * Set the Layout Manager for DLayer
		 */
		
		setLayout(layout);
		
		/**
		 * Set maximum size of a DLayer
		 */
		
		setMaximumSize(new Dimension(350, 25));
		
		/**
		 * Set preferred size of a DLayer
		 */
		
		setPreferredSize(new Dimension(350, 25));
		
		/**
		 * Set size of a DLayer
		 */
		
		setSize(new Dimension(350, 25));
		
		/**
		 * Use double buffering for DLayers
		 */
		
		setDoubleBuffered(true);
		
		/**
		 * Background color of a DLayer depends on its type
		 */
		
		setBackground(COLORS[type]);
		
		/**
		 * Set the border of the DLayer
		 */
		
		setBorder(new LineBorder(new Color(155, 155, 155)));
		
		/**
		 * Create a CheckBox to be selected in order to show a field or a link in the canvas
		 */

		check = new JCheckBox();
		check.setSize(35, 25);
		check.setMaximumSize(new Dimension(35, 25));
		check.setMinimumSize(new Dimension(35, 25));
		check.setPreferredSize(new Dimension(35, 25));
		
		/**
		 * Create a JLabel for each DLayer
		 */

		this.label = new JLabel(" " + label, JLabel.LEFT);
		this.label.setSize(125, 25);
		this.label.setMaximumSize(new Dimension(125, 25));
		this.label.setMinimumSize(new Dimension(125, 25));
		this.label.setPreferredSize(new Dimension(125, 25));
		
		/**
		 * Set a different background color for a layer depending on
		 * its type
		 */
		
		switch (type) {
		case MOTE:
			this.label.setBackground(new Color(255, 200, 200));
			break;
		case FIELD:
			this.label.setBackground(new Color(200, 255, 200));
			break;
		case LINK:
			this.label.setBackground(new Color(200, 200, 255));
			break;
		}
		
		/**
		 * Create a ComboBox whose options are the elements in the array given to the constructor;
		 * these elements are selected depending on the type of the current layer
		 */

		displays = new JComboBox(DISPLAYS[type]);
		displays.setSize(100, 25);
		displays.setMinimumSize(new Dimension(125, 25));
		displays.setPreferredSize(new Dimension(125, 25));
		
		/**
		 * The layer listens for events from checkbox and combobox
		 */

		check.addActionListener(this);
		displays.addActionListener(this);
		
		/**
		 * Link EAST edge of label to WEST edge of combobox with no distance (0) between
		 * edges
		 */

		layout.putConstraint(SpringLayout.EAST, this.label, 0,
				SpringLayout.WEST, displays);
		
		/**
		 * Link EAST edge of combobox to EAST edge of the layer with no distance (0) between
		 * edges
		 */
		
		layout.putConstraint(SpringLayout.EAST, displays, 0, SpringLayout.EAST,
				this);
		
		/**
		 * Add components to the layout
		 */

		add(check);
		add(this.label);
		add(displays);

	}

	public boolean isFieldSelected() {
		return (type == FIELD && check.isSelected());
	}
	
	/**
	 * Method to be defined by those classes that implement the
	 * ActionListener interface: it's the event handler
	 */

	public void actionPerformed(ActionEvent e) {
		
		/**
		 * Checkbox has been clicked
		 */
		
		if (e.getSource() == check) {
			
			/**
			 * If checkbox has been checked, set the corresponding variable in the
			 * parent DDocument, otherwise do nothing
			 * TO BE CHECKED (???)
			 */
			
			if (check.isSelected()) {
				
				parent.selectedFieldIndex = index;
				// System.out.println("redraw index " +zIndex +" on layer");
			} else if (type == FIELD) {
				// System.out.println("clear");
				// parent.canvas.repaint();
				// repaintLayer(g);
			} else {
				// repaintLayer(g);
			}
			
			/**
			 * The selected option in the ComboBox has changed: each option
			 * corresponds to a different visualization mode
			 */
			
		} else if (e.getSource() == displays) {
			String selected = (String) displays.getSelectedItem();
			if (selected.equals("circle")) {
				paintMode = OVAL;
			} else if (selected.equals("img")) {
				paintMode = IMG;
			} else if (selected.equals("txt")) {
				paintMode = TXT_MOTE;
			} else if (selected.equals("color 256")) {
				paintMode = COLOR_256;
			} else if (selected.equals("color 1024")) {
				paintMode = COLOR_1024;
			} else if (selected.equals("color 4096")) {
				paintMode = COLOR_4096;
			} else if (selected.equals("color 16384")) {
				paintMode = COLOR_16384;
			} else if (selected.equals("line")) {
				paintMode = LINE;
			} else if (selected.equals("label")) {
				paintMode = LABEL;
			} else if (selected.equals("line+label")) {
				paintMode = LINE_LABEL;
			}
		}
		// System.out.println("Repainting parent?");
		// parent.repaint();
	}
	
	/**
	 * This method is invoked from the the navigator, which is the
	 * container for the layers. It's use
	 */

	public void init() {
		if (type == LINK) {
			// addLinks(true);
		} else {
			addMotes();
		}
	}

	public String toString() {
		return "Layer " + name + " " + type;
	}

	// private void addLinks(boolean paint){
	// Iterator it = models.iterator();
	// while(it.hasNext()){
	// DLink mm = (DLink) it.next();
	// //canvas.add(mm);
	// if (paint) mm.repaint();
	// }
	// }
	
	/**
	 * Add the single mote to the canvas. Motes are instances of
	 * the class DMote and derive from the class DShape
	 * @param model
	 */

	protected void addMote(DMoteModel model) {
		DShape mote = new DMote(model, this.parent, this);
		layer.add(mote);
	}
	
	/**
	 * Add motes to the canvas
	 */

	private void addMotes() {
		Iterator it = models.iterator();
		while (it.hasNext()) {
			addMote((DMoteModel) it.next());
		}
	}
	
	/**
	 * Update index of the layer, namely the vertical position
	 * within the navigator
	 * @param index
	 */

	public void updateIndex(int index) {
		
		/**
		 * The index of this layer within the navigator
		 */
		
		zIndex = index;
		
		/**
		 * The actual position of the layer within the
		 * navigator
		 */
		
		z_index = (navigator.totalLayers - zIndex) * 100;
		// parent.canvas.setLayer(d.canvas, length - i);
	}

	public void paintScreenBefore(Graphics g) {

		Dimension d = parent.canvas.getSize();
		int x = 0;
		int y = 0;
		int xstep = (int) (d.width / 40);
		int ystep = (int) (d.height / 40);

		for (; x < d.width; x += xstep) {
			for (y = 0; y < d.height; y += ystep) {
				double val = 0;
				double sum = 0;
				double total = 0;
				double min = 10000000;
				Iterator it = models.iterator();
				while (it.hasNext()) {
					DMoteModel m = (DMoteModel) it.next();
					double dist = distance(x, y, m.x, m.y);
					if (true) { // 121
						if (dist < min)
							min = dist;
						val += ((double) (((int) m.getValue(index)) >> paintMode))
								/ dist / dist;
						sum += (1 / dist / dist);
					}
				}
				int reading = (int) (val / sum);
				// System.out.println("Reading: " + reading);
				if (reading > 255)
					reading = 255;
				g.setColor(new Color(reading, reading, reading));
				// System.out.println("Filling " + x + "+" + step + " " + y +
				// "+" + step + " with " + g.getColor());
				g.fillRect(x, y, xstep, ystep);
			}
		}

	}

	public double distance(int x, int y, int x1, int y1) {
		return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
	}

	protected void repaintLayer(Graphics g) {
		if (check.isSelected()) {
			if (type == FIELD) {
				paintScreenBefore(g);
			} else if (type == LINK) {
				Iterator it = models.iterator();
				// System.out.print("Draw links: ");
				while (it.hasNext()) {
					DLinkModel model = (DLinkModel) it.next();
					DLink lnk = new DLink(model, parent, this);
					lnk.paintShape(g);
					// System.out.print("+");
				}
				// System.out.println();
			} else if (type == MOTE) {
				Iterator it = models.iterator();
				// System.out.print("Draw motes: ");
				while (it.hasNext()) {
					DMoteModel model = (DMoteModel) it.next();
					DShape m = new DMote(model, parent, this);
					m.paintShape(g);
					// System.out.print("+");
				}
				// System.out.println();
			}
		}
	}

	public JLabel getLabel() {
		return this.label;
	}
}
