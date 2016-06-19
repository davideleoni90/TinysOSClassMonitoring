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
import java.awt.*;
import java.util.*;
import java.io.*;

/**
 * The model of data associated to a mote TO BE CHECKED: why serializable???
 * 
 * @author user
 * 
 */

class DMoteModel extends Object implements Serializable {

	/**
	 * Reference to the main panel (DDocument)
	 */

	public DDocument root;

	/**
	 * Coordinates on the canvas of the mote represented by this model
	 */

	protected int x, y;

	/**
	 * ID of the mote represented by this model
	 */

	protected int id;

	/**
	 * Boolean value indicating whether the mote is a
	 * producer one or not; this information is used
	 * to draw the mote
	 */

	private boolean isProducer;

	/**
	 * Constructor position on the canvas is randomly determined
	 * at the beginning
	 * 
	 * @param id
	 * @param rand
	 * @param root
	 */

	public DMoteModel(int id, Random rand, DDocument root,boolean isProducer) {

		/**
		 * The root container (DDocument)
		 */

		this.root = root;

		/**
		 * ID of the mote
		 */

		this.id = id;

		/**
		 * Randomly choose position of the mote on the canvas avoiding
		 * overlappings
		 */

		boolean overlapping=true;
		while(overlapping){

			/**
			 * X coordinate w.r.t. to the container; we don't want the mote to have
			 * its center exactly on the border, so adjust x with the width of the
			 * image used to represent the mote
			 */

			x = (int) root.motesImageDimension.getWidth()+rand.nextInt(root.canvas.getWidth() - 2*(int) root.motesImageDimension.getWidth());

			boolean foundX=true;
			Iterator moteIterator=root.motes.entrySet().iterator();
			while(moteIterator.hasNext()){
				DMoteModel current=((DMoteModel)((Map.Entry)moteIterator.next()).getValue());
				if((current.x-(int) root.motesImageDimension.getWidth())<=x && x<=(current.x+(int) root.motesImageDimension.getWidth())){
					foundX=false;
					break;
				}
			}

			/**
			 * Y coordinate w.r.t. to the container; we don't want the mote to have
			 * its center exactly on the border, so adjust y with the height of the
			 * image used to represent the mote
			 */

			y = (int) root.motesImageDimension.getHeight()+rand.nextInt(root.canvas.getHeight() - 2*(int) root.motesImageDimension.getHeight());
			boolean foundY=true;
			moteIterator=root.motes.entrySet().iterator();
			while(moteIterator.hasNext()){
				DMoteModel current=((DMoteModel)((Map.Entry)moteIterator.next()).getValue());
				if((current.y-(int) root.motesImageDimension.getHeight())<=y && y<=(current.y+(int) root.motesImageDimension.getHeight())){
					foundY=false;
					break;
				}
			}
			if(!foundY&&!foundX)
				
				/**
				 * The mote overlaps another one: iterate
				 * to try new random coordinates
				 */
				
				continue;
			else {
				overlapping=false;
			}
		}

		this.isProducer=isProducer;
	}

	/**
	 * Get the id of the mote represented by this motemodel
	 * 
	 * @return
	 */

	public int getId() {
		return id;
	}

	public Image getImage() {
		return root.motesImage;
	}

	public int getLocX() {
		return x;
	}

	public int getLocY() {
		return y;
	}

	public boolean isProducer() {
		return isProducer;
	}
}
