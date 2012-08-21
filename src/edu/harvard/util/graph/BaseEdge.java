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

package edu.harvard.util.graph;

import java.io.*;


/**
 * A directed graph edge
 * 
 * @author Peter Macko
 */
public class BaseEdge implements Serializable, Comparable<BaseEdge> {
	
	private static final long serialVersionUID = -7584478379931006041L;

	int index;
	protected String label;
	
	protected BaseNode from;
	protected BaseNode to;
	
	
	/**
	 * Create an instance of class BaseEdge
	 * 
	 * @param from the from node
	 * @param to the to node
	 */
	public BaseEdge(BaseNode from, BaseNode to) {
		
		this(-1, from, to, null);		// The index will get assigned upon graph addition
	}
	
	
	/**
	 * Create an instance of class BaseEdge
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @param label the edge label
	 */
	public BaseEdge(BaseNode from, BaseNode to, String label) {
		
		this(-1, from, to, label);		// The index will get assigned upon graph addition
	}
	
	
	/**
	 * Create an instance of class BaseEdge using a specified index. Use at your own risk.
	 * 
	 * @param index the edge index
	 * @param from the from node
	 * @param to the to node
	 * @param label the edge label
	 */
	protected BaseEdge(int index, BaseNode from, BaseNode to, String label) {
		
		this.index = index;
		this.label = label;
		
		this.from = from;
		this.to = to;
		
		if (from.graph != to.graph || from.graph == null) {
			throw new IllegalArgumentException("The input nodes must be assigned to the same graph");
		}
	}

	
	/**
	 * Get a numerical index (meaningful only in the context of its parent graph)
	 * 
	 * @return an index of the edge
	 */
	public int getIndex() {
		return index;
	}
	
	
	/**
	 * Get the parent graph
	 * 
	 * @return the parent graph
	 */
	public BaseGraph getGraph() {
		return from.graph;
	}
	

	/**
	 * Return the from node
	 *
	 * @return the from node
	 */
	public BaseNode getBaseFrom() {
		return from;
	}
	

	/**
	 * Return the to node
	 *
	 * @return the to node
	 */
	public BaseNode getBaseTo() {
		return to;
	}
	
	
	/**
	 * Get the label
	 * 
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	
	/**
	 * Set the label
	 * 
	 * @param label the new label (can be null)
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	
	/**
	 * Calculate the hash code
	 * 
	 * @return the hash code
	 */
	public int hashCode() {
		return (from.index << 16) | to.index;
	}
	
	
	/**
	 * Indicates whether some other object is "equal to" this one
	 * 
	 * @param obj the other object
	 * @return true if the two are equal
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof BaseEdge)) return false;
		BaseEdge e = (BaseEdge) obj;
		return e.from.index == from.index && e.to.index == to.index;
	}
	
	
	/**
	 * Return a string version of the edge
	 * 
	 * @return the string version
	 */
	public String toString() {
		return from.toString() + " --> " + to.toString();
	}


	/**
	 * Compare this object to another instance of BaseNode
	 * 
	 * @param other the other instance
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(BaseEdge other) {
		if (other.from.graph != from.graph) {
			throw new IllegalArgumentException("Cannot compare edges from different graphs");
		}
		return index - other.index;
	}
}
