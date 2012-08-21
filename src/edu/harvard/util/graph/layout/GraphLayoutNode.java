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

import edu.harvard.util.graph.*;


/**
 * A node in the graph layout
 * 
 * @author Peter Macko
 */
public class GraphLayoutNode implements java.io.Serializable {
	
	private static final long serialVersionUID = 2561776526577254844L;
	
	protected BaseNode node;
	protected double x;
	protected double y;
	protected double width;
	protected double height;
	
	
	/**
	 * Constructor for objects of type GraphLayoutNode
	 * 
	 * @param node the base node
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 */
	public GraphLayoutNode(BaseNode node, double x, double y) {

		this.node = node;
		
		this.x = x;
		this.y = y;
		
		this.width = 0;
		this.height = 0;
	}


	/**
	 * Return the node index
	 *
	 * @return the node index
	 */
	public int getIndex() {
		return node.getIndex();
	}


	/**
	 * Return the associated base node
	 *
	 * @return the base node
	 */
	public BaseNode getBaseNode() {
		return node;
	}
	
	
	/**
	 * Return the X coordinate of the center of the node
	 * 
	 * @return the X-coordinate of the center of the object
	 */
	public double getX() {
		return x;
	}
	
	
	/**
	 * Return the Y coordinate of the center of the node
	 * 
	 * @return the Y-coordinate of the center of the object
	 */
	public double getY() {
		return y;
	}
	
	
	/**
	 * Set the position of the node
	 * 
	 * @param x the new X-coordinate
	 * @param y the new Y-coordinate
	 */
	public void setPosition(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	
	/**
	 * Move the node relatively to its current position 
	 * 
	 * @param dx the shift in the X direction
	 * @param dy the shift in the Y direction
	 */
	public void moveRelatively(double dx, double dy) {
		this.x += dx;
		this.y += dy;
	}
	
	
	/**
	 * Scale the node and reposition it with respect to the pivot, which does not move
	 * 
	 * @param sx the scale on the X axis
	 * @param sy the scale on the Y axis
	 * @param px pivot's X-coordinate
	 * @param py pivot's Y-coordinate
	 */
	public void scaleWithPivot(double sx, double sy, double px, double py) {
		x = px + (x - px) * sx;
		y = py + (y - py) * sy;
		width *= sx;
		height *= sy;
	}
	
	
	/**
	 * Get the node width
	 * 
	 * @return the node width
	 */
	public double getWidth() {
		return width;
	}
	
	
	/**
	 * Get the node height
	 * 
	 * @return the node height
	 */
	public double getHeight() {
		return height;
	}
	
	
	/**
	 * Set the node size
	 * 
	 * @param width the node width
	 * @param height the node height
	 */
	public void setSize(double width, double height) {
		this.width = width;
		this.height = height;
	}
	
	
	/**
	 * Calculate the hash code
	 * 
	 * @return the hash code
	 */
	public int hashCode() {
		return node.hashCode();
	}
	
	
	/**
	 * Indicates whether some other object is "equal to" this one
	 * 
	 * @param obj the other object
	 * @return true if the two are equal
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphLayoutNode)) return false;
		GraphLayoutNode n = (GraphLayoutNode) obj;
		return node.equals(n.node) && x == n.x && y == n.y;
	}
	
	
	/**
	 * Get a string representation of this object
	 * 
	 * @return the string representation
	 */
	public String toString() {
		return "" + node + " [pos=" + x + ":" + y + ", size=" + width + "x" + height + "]";
	}
}
