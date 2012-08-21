/*
 * A Collection of Miscellaneous Utilities
 *
 * Copyright 2011
 *      The President and Fellows of Harvard College.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package edu.harvard.pass.orbiter.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import edu.harvard.pass.*;
import edu.harvard.util.attribute.Attribute;
import edu.harvard.util.gui.GraphDecorator;


/**
 * An icon of a node or an edge
 * 
 * @author Peter Macko
 */
public class NodeEdgeIcon implements Icon {
	
	enum Shape {
		ELLIPSE, RECTANGLE, LINE, THICK_LINE
	}

	private int height;
	private int width;
	
	private PNode node = null;
	private GraphDecorator<PNode, PEdge, PSummaryNode, PGraph> decorator = null;

	private Shape shape;
	private Attribute<Color> fillColor = null;
	private Attribute<Color> outlineColor = null;
	
	
	/**
	 * Create an instance of class PNodeIcon
	 * 
	 * @param node the node
	 * @param width the icon width
	 * @param height the icon height
	 * @param decorator the graph decorator
	 */
	public NodeEdgeIcon(PNode node, int width, int height, GraphDecorator<PNode, PEdge, PSummaryNode, PGraph> decorator) {
		this.shape = Shape.ELLIPSE;
		this.node = node;
		this.width = width;
		this.height = height;
		this.decorator = decorator;
	}
	
	
	/**
	 * Create an instance of class PNodeIcon
	 * 
	 * @param fillColor the fill color attribute
	 * @param outlineColor the outline color attribute
	 * @param shape the icon shape
	 * @param width the icon width
	 * @param height the icon height
	 */
	public NodeEdgeIcon(Attribute<Color> fillColor, Attribute<Color> outlineColor, Shape shape, int width, int height) {
		this.shape = shape;
		this.fillColor = fillColor;
		this.outlineColor = outlineColor;
		this.width = width;
		this.height = height;
	}
	
	
	/**
	 * Create an instance of class PNodeIcon
	 * 
	 * @param fillColor the fill color
	 * @param outlineColor the outline color
	 * @param shape the icon shape
	 * @param width the icon width
	 * @param height the icon height
	 */
	public NodeEdgeIcon(Color fillColor, Color outlineColor, Shape shape, int width, int height) {
		this.shape = shape;
		this.fillColor = new Attribute<Color>("Fill color", true, fillColor);
		this.outlineColor = new Attribute<Color>("Outline color", true, outlineColor);
		this.width = width;
		this.height = height;
	}
	
	
	/**
	 * Get the icon height
	 * 
	 * @return the icon height
	 */
	@Override
	public int getIconHeight() {
		return height;
	}

	
	/**
	 * Get the icon width
	 * 
	 * @return the icon width
	 */
	@Override
	public int getIconWidth() {
		return width;
	}

	
	/**
	 * Paint the icon
	 * 
	 * @param c the component
	 * @param g the graphics context
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 */
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		
		if (node != null) {
			g.setColor(decorator.getNodeColor(node, GraphDecorator.NONE));
		}
		else {
			g.setColor(fillColor.get());
		}
		switch (shape) {
		case ELLIPSE: g.fillOval(x, y, width, height); break;
		case RECTANGLE: g.fillRect(x, y, width, height); break;
		}
		
		if (node != null) {
			g.setColor(decorator.getNodeOutlineColor(node, GraphDecorator.NONE));
		}
		else {
			g.setColor(outlineColor.get());
		}
		switch (shape) {
		case ELLIPSE: g.drawOval(x, y, width, height); break;
		case RECTANGLE: g.drawRect(x, y, width, height); break;
		case LINE:
			g.drawLine(x, y + height / 2    , x + width, y + height / 2    );
			break;
		case THICK_LINE:
			g.drawLine(x, y + height / 2 - 1, x + width, y + height / 2 - 1);
			g.drawLine(x, y + height / 2    , x + width, y + height / 2    );
			g.drawLine(x, y + height / 2 + 1, x + width, y + height / 2 + 1);
			break;
		}
	}
}
