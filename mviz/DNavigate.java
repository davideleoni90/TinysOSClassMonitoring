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

import javax.swing.table.*;
import java.awt.image.*;

// Standard imports for XML
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

public class DNavigate extends JPanel implements ActionListener {

    private DDocument parent;
    protected ArrayList layers = new ArrayList();
    private int _tmp_i = 0;
    protected int totalLayers = 0;

    private int default_width = 600;
    private int default_height = 600;
    
    /**
     * The navigator of the applet: there are two rows for each field in the messages sent by motes and one row for each link
     * between motes
     * @param label_motes
     * @param label_links
     * @param parent
     */

    public DNavigate(Vector label_motes, Vector label_links, DDocument parent) {
    	
    	/**
    	 * Set the parent of the navigator, of type DDocument
    	 */
    	
        this.parent = parent;
        
        /**
         * The Layout manager for the navigator is, once again, a BoxLayout.
         * Because of Y_AXIS parameter, components are laid out vertically
         * from top to bottom
         */
        
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        
        /**
         * Set the layout for navigator
         */
        
        this.setLayout(layout);
        
        /**
         * The number of rows in the navigator
         * TO BE CHECKED: why two rows for each sensed mote?
         */
        
        totalLayers = 2 * label_motes.size() + label_links.size();

        this._tmp_i = 0;
        
        /**
         * Add a row in the navigator for each field, for each mote and for
         * each field of the packets sent by motes
         * TO BE CHECKED: why fields and motes are both taken from the vector
         * "motes" of the parent? Shouldn't they be taken from different
         * vectors?
         */
        
        addLayer(label_motes, DLayer.MOTE, parent.motes);
        addLayer(label_links, DLayer.LINK, parent.links);
        addLayer(label_motes, DLayer.FIELD, parent.motes);
        
        /**
         * Update indexes of the layers
         */
        
        updateLayerIndex();
        setMinimumSize(getPreferredSize());
        }

    protected void addMote(DMoteModel model) {
        Iterator it = layers.iterator();
        while (it.hasNext()) {
            DLayer layer = (DLayer) it.next();
            System.out.println("Adding mote "+layer.getLabel().getText());
            layer.addMote(model);
        }
    }
    
    /**
     * Create a layer in the navigator: for the given type (mote, field or link) create a label
     * @param labels
     * @param type
     * @param models
     */

    private void addLayer(Vector labels, int type, ArrayList models) {
        for (int i = 0; i < labels.size(); i++, _tmp_i++) {
            DLayer d = new DLayer(_tmp_i, i, (String) labels.elementAt(i), type, parent, models, this);
            this.add(d);
            layers.add(d);
        }
    }
    
    /**
     * Update index for all layers.
     * Here by "index" we mean the vertical position within
     * the navigator
     */
    
    private void updateLayerIndex() {
    	
    	/**
    	 * TO BE CHECKED: variable length is useless
    	 */
    	
        int length = layers.size();
        
        /**
         * Iterate through the list of layers and update
         * the zIndex for each of them
         */
        
        Iterator it = layers.iterator();
        int i = 0;
        while (it.hasNext()) {
            DLayer d = (DLayer) it.next();
            d.updateIndex(i);
            ++i;
        }
    }

    public void redrawNavigator() {
        //System.out.println("Redrawing navigator.");
        Iterator it = layers.iterator();
        while (it.hasNext()) {
            remove((DLayer) it.next());
        }
        it = layers.iterator();
        while (it.hasNext()) {
            add((DLayer) it.next());
        }

        revalidate();
	    //repaint();
        //parent.repaint();
        //parent.canvas.repaint();
        redrawAllLayers();
    }

    public void moveLayerUp(int zIndex) {
        if (zIndex == 0) {
            return;
        }
        DLayer d = (DLayer) layers.remove(zIndex);
        layers.add(zIndex - 1, d);
        updateLayerIndex();
        redrawNavigator();
        redrawAllLayers();
    }

    public void moveLayerDown(int zIndex) {
        if (zIndex == layers.size() - 1) {
            return;
        }
        DLayer d = (DLayer) layers.remove(zIndex);
        layers.add(zIndex + 1, d);
        updateLayerIndex();
        redrawNavigator();
        redrawAllLayers();
    }

    public void init() {
        Iterator it = layers.iterator();
        while (it.hasNext()) {
            DLayer layer = (DLayer) it.next();
            layer.init();
        }
    }

    public void paint() {
        //System.out.println("Painting navigator");
        redrawNavigator();
        Iterator it = layers.iterator();
    }

    public void actionPerformed(ActionEvent e) {
	// TODO Auto-generated method stub

    }

    private long currentSecond = -1;
    private long PERIOD = 500;

    protected void redrawAllLayers() {
        Date date = new Date();
        /*REMOVE uncomment this /*
        /*if (date.getTime() - currentSecond < PERIOD) {
            System.out.println("time: " + (date.getTime() - currentSecond));
            return;
        } else {
            currentSecond = date.getTime();
        }*/

        int start = totalLayers - 1;
        for (int i = 0; i < totalLayers; i++) {
            DLayer a = (DLayer) layers.get(i);
            if (a.isFieldSelected()) {
                start = a.zIndex;
                break;
            }
        }
        DLayer bg = (DLayer) layers.get(start);
        Image offscreen = new BufferedImage(parent.canvas.getWidth(), parent.canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = offscreen.getGraphics();
        Graphics2D g2d = (Graphics2D) g;
        g2d.clearRect(0, 0, parent.canvas.getWidth(), parent.canvas.getHeight());
        g2d.fillRect(0, 0, parent.canvas.getWidth(), parent.canvas.getHeight());

        for (int i = start; i >= 0; i--) {
            DLayer a = (DLayer) layers.get(i);
            a.repaintLayer(g);
        }
        parent.canvas.getGraphics().drawImage(offscreen, 0, 0, this);
    }

    public void update(Graphics g) {
        paint(g);
    }

}
