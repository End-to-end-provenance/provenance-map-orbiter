/*
 * A Collection of Miscellaneous Utilities
 *
 * Copyright 2010
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

package edu.harvard.util.graph.layout;

import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import edu.harvard.util.ParserException;
import edu.harvard.util.XMLUtils;
import edu.harvard.util.graph.*;


/**
 * An edge in the graph layout
 * 
 * @author Peter Macko
 */
public class GraphLayoutEdge implements java.io.Serializable {

	private static final long serialVersionUID = -1033815483000434164L;
	
	public static final String DOM_ELEMENT = "layout-edge";

	protected BaseEdge edge;
	
	protected double[] x;
	protected double[] y;
	
	protected GraphLayoutNode from;
	protected GraphLayoutNode to;
	
	
	/**
	 * Constructor for objects of type GraphLayoutEdge
	 * 
	 * @param edge the base edge
	 * @param from the from node
	 * @param to the to node
	 * @param x the X coordinates
	 * @param y the Y coordinates
	 */
	public GraphLayoutEdge(BaseEdge edge, GraphLayoutNode from, GraphLayoutNode to, double[] x, double[] y) {
		
		this.edge = edge;
		
		this.x = x;
		this.y = y;
		
		this.from = from;
		this.to = to;
	}
	
	
	/**
	 * Constructor for objects of type GraphLayoutEdge
	 * 
	 * @param edge the base edge
	 * @param from the from node
	 * @param to the to node
	 */
	public GraphLayoutEdge(BaseEdge edge, GraphLayoutNode from, GraphLayoutNode to) {
		
		this.edge = edge;
		
		this.x = null;
		this.y = null;
		
		this.from = from;
		this.to = to;
	}


	/**
	 * Return the edge index
	 *
	 * @return the edge index
	 */
	public int getIndex() {
		return edge.getIndex();
	}

	
	/**
	 * Return the base edge
	 *
	 * @return the base edge
	 */
	public BaseEdge getBaseEdge() {
		return edge;
	}
	
	
	/**
	 * Return the X coordinates
	 * 
	 * @return the X-coordinates
	 */
	public double[] getX() {
		if (x == null) {
			x = new double[2];
			x[0] = from.getX();
			x[1] = to.getX();
		}
		return x;
	}
	
	
	/**
	 * Return the Y coordinates
	 * 
	 * @return the Y-coordinates
	 */
	public double[] getY() {
		if (y == null) {
			y = new double[2];
			y[0] = from.getY();
			y[1] = to.getY();
		}
		return y;
	}


	/**
	 * Return the from node
	 *
	 * @return the from node
	 */
	public GraphLayoutNode getFrom() {
		return from;
	}


	/**
	 * Return the to node
	 *
	 * @return the to node
	 */
	public GraphLayoutNode getTo() {
		return to;
	}


	/**
	 * Return the length of the edge
	 *
	 * @return the length of the edge
	 */
	public double getLength() {
		double dx = from.getX() - to.getX();
		double dy = from.getY() - to.getY();
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	
	/**
	 * Get the number of control points
	 * 
	 * @return the number of control points
	 */
	public int sizeCP() {
		return x == null ? 2 : x.length; 
	}
	
	
	/**
	 * Move the edge relatively to its current position 
	 * 
	 * @param dx the shift in the X direction
	 * @param dy the shift in the Y direction
	 */
	public void moveRelatively(double dx, double dy) {
		
		if (x != null) {
			for (int i = 0; i < x.length; i++) x[i] += dx;
		}
		
		if (y != null) {
			for (int i = 0; i < y.length; i++) y[i] += dy;
		}
	}
	
	
	/**
	 * Scale the edge and reposition its control points with respect to the pivot, which does not move
	 * 
	 * @param sx the scale on the X axis
	 * @param sy the scale on the Y axis
	 * @param px pivot's X-coordinate
	 * @param py pivot's Y-coordinate
	 */
	public void scaleWithPivot(double sx, double sy, double px, double py) {

		if (x != null) {
			for (int i = 0; i < x.length; i++) x[i] = px + (x[i] - px) * sx;
		}
		
		if (y != null) {
			for (int i = 0; i < y.length; i++) y[i] = py + (y[i] - py) * sy;
		}
	}
	
	
	/**
	 * Calculate the hash code
	 * 
	 * @return the hash code
	 */
	public int hashCode() {
		if (edge == null) {
			return (from.node.getIndex() << 16) | to.node.getIndex();
		}
		return edge.hashCode();
	}
	
	
	/**
	 * Indicates whether some other object is "equal to" this one
	 * 
	 * @param obj the other object
	 * @return true if the two are equal
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphLayoutEdge)) return false;
		GraphLayoutEdge e = (GraphLayoutEdge) obj;
		
		if (edge == null && e.edge == null) {
			return from.node.equals(e.from.node) && to.node.equals(e.to.node);
		}
		
		if (edge == null || e.edge == null) {
			return false;
		}
		
		return edge.equals(e.edge);		// Should we check the coordinates?
	}

	
	/**
	 * Write the edge to XML
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {

		String s;
		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		attrs.addAttribute("", "", "index", "CDATA", "" + getIndex());
		hd.startElement("", "", DOM_ELEMENT, attrs);
		
		
		// Write the basic attributes
		
		attrs.clear();
		
		s = "";
		for (double v : getX()) {
			if (!"".equals(s)) s += " ";
			s += v;
		}
		
		hd.startElement("", "", "x", attrs);
		hd.characters(s.toCharArray(), 0, s.length());
		hd.endElement("", "", "x");
		
		s = "";
		for (double v : getY()) {
			if (!"".equals(s)) s += " ";
			s += v;
		}
		
		hd.startElement("", "", "y", attrs);
		hd.characters(s.toCharArray(), 0, s.length());
		hd.endElement("", "", "y");

		
		// Finish
		
		hd.endElement("", "", DOM_ELEMENT);
	}
	
	
	/**
	 * Load the edge from an XML element
	 * 
	 * @param layout the graph layout
	 * @param element the XML element
	 * @return the edge object
	 * @throws ParserException on DOM parser error
	 */
	public static GraphLayoutEdge loadFromXML(GraphLayout layout, Element element) throws ParserException {
		
		if (!element.getNodeName().equals(DOM_ELEMENT)) {
			throw new ParserException("Expected <" + DOM_ELEMENT + ">, found <" + element.getNodeName() + ">");
		}
		
		
		// Attributes
		
		int index = Integer.parseInt(XMLUtils.getAttribute(element, "index"));
		
		BaseEdge edge = layout.getGraph().getBaseEdge(index);
		if (edge == null) {
			// XXX Need a better way to deal with summary edges than to sometimes ignore them and sometimes get wrong edges instead
			return null;//throw new ParserException("Edge with index " + index + " does not exist");
		}
		
		GraphLayoutNode nf = layout.getLayoutNode(edge.getBaseFrom().getIndex());
		if (nf == null) {
			throw new ParserException("Layout node with index " + edge.getBaseFrom().getIndex() + " does not exist");
		}
		
		GraphLayoutNode nt = layout.getLayoutNode(edge.getBaseTo().getIndex());
		if (nt == null) {
			throw new ParserException("Layout node with index " + edge.getBaseTo().getIndex() + " does not exist");
		}
		
		
		// Parse the basic properties
		
		String s;
		String[] a; 
		
		s = XMLUtils.getTextValue(element, "x", null);
		if (s == null) {
			throw new ParserException("Layout node with index " + index + " does not contain element <x>");
		}
		
		a = s.split(" ");
		double[] x = new double[a.length];
		for (int i = 0; i < a.length; i++) x[i] = Double.parseDouble(a[i]);
		
		s = XMLUtils.getTextValue(element, "y", null);
		if (s == null) {
			throw new ParserException("Layout node with index " + index + " does not contain element <y>");
		}
		
		a = s.split(" ");
		double[] y = new double[a.length];
		for (int i = 0; i < a.length; i++) y[i] = Double.parseDouble(a[i]);
		
		if (x.length != y.length) {
			throw new ParserException("Layout edge with index " + index + " does not have x and y arrays of the same length");
		}
		
		
		// Create the object
		
		GraphLayoutEdge l = new GraphLayoutEdge(edge, nf, nt, x, y);

		
		// Finish
		
		return l;
	}
}
