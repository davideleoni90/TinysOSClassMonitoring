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
import javax.swing.*;
import java.awt.geom.Line2D;

public class DLink 
extends JComponent 
{

	protected DLinkModel model;
	protected DDocument document;
	private Color color;
	private int index;
	private int lastX, lastY;
	
	public DLink(DLinkModel model, DDocument document,Color color,int index) {
		super();
		this.model = model;
		this.document = document;
		this.color=color;
		this.index=index;
		}

	public void paintShape(Graphics g){
		Graphics2D g2 = (Graphics2D) g;
		
		/**
		 * Check if valid link (it should always be
		 * this case)
		 */
		
		int diffX = (model.m1.getLocX() - model.m2.getLocX());
		int diffY = (model.m1.getLocY() - model.m2.getLocY());
		if (diffX == 0 && diffY == 0) {
			return;
		}
		if (diffX == 0) {diffX = 1;}
		if (diffY == 0) {diffY = 1;}
		int midX = (model.m1.getLocX() + model.m2.getLocX()) / 2;
		int midY = (model.m1.getLocY() + model.m2.getLocY()) / 2;
		midY += 8;
		midX += 10;
		
		if (diffX * diffY < 0) {
			midY += Math.abs(((double)diffX / ((double)Math.abs(diffY) + (double)Math.abs(diffX))) * 10);
			midX += Math.abs(((double)diffX / ((double)Math.abs(diffY) + (double)Math.abs(diffX))) * 10);
		}
		else {
			midY -= Math.abs(((double)diffX / ((double)Math.abs(diffY) + (double)Math.abs(diffX))) * 10);
			midX += Math.abs((double)diffX / ((double)Math.abs(diffY) + (double)Math.abs(diffX)) * 10);
		}
		
		/**
		 * Draw the link
		 */
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(2));
		g2.draw(new Line2D.Double(model.m1.getLocX(),  model.m1.getLocY(), model.m2.getLocX(), model.m2.getLocY()));
		g2.setColor(Color.BLACK);
		
		/**
		 * Draw the index of the link within the currently
		 * shown path
		 */
		
		g2.drawString(Integer.toString(index), midX, midY);
	}
}



