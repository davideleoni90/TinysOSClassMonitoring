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

/*
 Store the data state for a single shape:
 type, two points, color
 Supports DShapeModelListeners.
 */
import java.awt.*;

import javax.swing.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;

/**
 * The model of a field. TO BE CHECKED: why serializable???
 * 
 * @author user
 * 
 */

class DMoteModel extends Object implements Serializable {

	public static final int VALUE = 0;
	public static final int MOTION = 1;
	public static final int ANY = 1;

	public DDocument root;
	transient private ArrayList listeners;

	protected int x, y, id;
	protected float[] values;
	protected Color[] colors;
	protected int[] sizes;

	protected int SHAPE_SIZE_MAX = 100;
	protected int COLOR_MAX = 230;

	/**
	 * This constructor creates a field model specifying its position on the
	 * canvas, its shape and its colors
	 * 
	 * @param id
	 * @param x
	 * @param y
	 * @param values
	 * @param root
	 */

	public DMoteModel(int id, int x, int y, float[] values, DDocument root) {
		this.root = root;
		this.x = x;
		this.y = y;
		this.id = id;

		values = new float[root.sensed_motes.size()];
		colors = new Color[root.sensed_motes.size()];
		sizes = new int[root.sensed_motes.size()];

		for (int i = 0; i < values.length; i++) {
			colors[i] = setColor(values[i]);
		}
		for (int i = 0; i < values.length; i++) {
			sizes[i] = setShapeSize(values[i]);
		}

		listeners = null;
	}

	/**
	 * Another constructor for mote model: does nothing TO BE CHECKED
	 * 
	 * @param root
	 * @param id
	 * @param name
	 */

	public DMoteModel(DDocument root, int id, String name) {

	}

	/**
	 * Constructor which is actually used for field model: position, shape and
	 * colors are determined providing a random numbers generator
	 * 
	 * @param id
	 * @param rand
	 * @param root
	 */

	public DMoteModel(int id, Random rand, DDocument root) {

		/**
		 * The root container (DDocument)
		 */

		this.root = root;

		/**
		 * ID of the mote
		 */

		this.id = id;

		/**
		 * X coordinate w.r.t. to the container
		 */

		x = 20 + rand.nextInt(root.canvas.getWidth() - 20);

		/**
		 * Y coordinate w.r.t. to the container
		 */

		y = 20 + rand.nextInt(root.canvas.getHeight() - 20);

		/**
		 * Array of float values with a number of elements as
		 * the number of fields of a message
		 */

		values = new float[root.sensed_motes.size()];

		/**
		 * As many colors as fields of a message
 		 */

		colors = new Color[root.sensed_motes.size()];

		/**
		 * As many sizes as fields of a message
		 */

		sizes = new int[root.sensed_motes.size()];

		/**
		 * Set random color and size for each field
		 */

		for (int i = 0; i < root.sensed_motes.size(); i++) {
			float value = rand.nextFloat() * 1000;
			values[i] = value;
			colors[i] = setColor(values[i]);
			sizes[i] = setShapeSize(values[i]);
		}

		listeners = null;
	}

	public int getId() {
		return id;
	}

	/**
	 * Set a random color for the field
	 * 
	 * @param value
	 * @return
	 */

	public Color setColor(float value) {
		int color = (int) (value) % COLOR_MAX;
		return new Color(color + 15, color, color + 25);
	}
	
	/**
	 * Set the maximum size for the shape representing the
	 * field
	 * @param value
	 * @return
	 */
	
	public int setShapeSize(float value) {
		return SHAPE_SIZE_MAX;
		// return (int)(value/root.maxValues[root.selectedFieldIndex] *
		// SHAPE_SIZE_MAX);
	}
	
	/**
	 * Get one of the random values used to determine the color and size
	 * of the shapes representing the fields of messages
	 * @param index
	 * @return
	 */
	
	public float getValue(int index) {
		if (values.length <= index) {
			return 0;
		} else {
			return (values[index]);
		}
	}
	
	/**
	 * Explicitly set the value for a field by its index
	 * @param field
	 * @param value
	 * @return
	 */

	public boolean setMoteValue(String field, int value) {
		int index = root.sensed_motes.indexOf(field);
		if (index < 0)
			return false;
		colors[index] = setColor((float) value);
		setValue(index, (float) value);
		return true;
	}

	/* edit by Leo90 - start */
	public boolean setMoteValue(String field, short value) {
		int index = root.sensed_motes.indexOf(field);
		if (index < 0)
			return false;
		colors[index] = setColor((float) value);
		setValue(index, (float) value);
		return true;
	}

	/* edit by Leo90 - end */

	public int getX() {
		return (x);
	}

	public int getY() {
		return (y);
	}

	public ImageIcon getIcon() {
		return root.icon;
	}

	public void setValue(int index, float value) {
		values[index] = value;
		fireChanges();
	}

	public void applyDeltas(int dx, int dy) {
		x += dx;
		y += dy;
		fireChanges();
	}

	public Image getImage() {
		return root.image;
	}

	public int getWidth(int index) {
		return getIcon().getImage().getWidth(this.root);
		// return sizes[index];
	}

	public int getHeight(int index) {
		return getIcon().getImage().getHeight(this.root);
		// return sizes[index];
	}

	public int getLeft() {
		return getLocX() - getWidth(0) / 2;
	}

	public int getTop() {
		return getLocY() - getHeight(0) / 2;
	}

	public int getLocX() {
		return x;
	}

	public int getLocY() {
		return y;
	}

	public Color getColor(int index) {
		return colors[index];
	}
	
	/**
	 * Add a DMoteModelListener to the set of listeners, namely those
	 * object that will be notified of any change in the DMoteModel
	 * @param listener
	 */

	public void addListener(DMoteModelListener listener) {
		if (listeners == null)
			listeners = new ArrayList();
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			if (it.next() == listener)
				return;
		}
		listeners.add(listener);
	}
	
	/**
	 * Remove a registered listener
	 * @param listener
	 */

	public void removeListener(DMoteModelListener listener) {
		if (listeners == null)
			return;
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			if (it.next() == listener) {
				it.remove();
				return;
			}
		}
	}

	// =========================================================================/
	
	/**
	 * Notify all the registered listeners about a change of the field model
	 */
	
	protected void fireChanges() {
		if (listeners == null)
			return;
		Iterator it = listeners.iterator();
		while (it.hasNext())
			((DMoteModelListener) (it.next())).shapeChanged(this, ANY);
	}

	public void requestRepaint() {
		fireChanges();
	}

	// =========================================================================/
	public void move(int x, int y) {
		this.x = x;
		this.y = y;
		fireChanges();
	}

	public boolean equals(Object o) {
		if (o instanceof DMoteModel) {
			DMoteModel dm = (DMoteModel) o;
			if (dm.getId() == getId()) {
				return true;
			}
		}
		return false;
	}
}
