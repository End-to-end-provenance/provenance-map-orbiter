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
import java.util.*;


/**
 * A graph node
 * 
 * @author Peter Macko
 * 
 * @param <E> the edge type
 */
public class Node<N extends BaseNode, E extends BaseEdge> extends BaseNode implements Serializable {
	
	private static final long serialVersionUID = -2524946760171196349L;


	/**
	 * Create an instance of Node
	 */
	public Node() {
	}
	
	
	/**
	 * Create an instance of Node using a specified index. Use at your own risk.
	 * 
	 * @param index the index
	 */
	protected Node(int index) {
		super(index);
	}
	
	
	/**
	 * Get the original node, if this node is derived
	 * 
	 * @return original the original node, or this if this is the original
	 */
	@SuppressWarnings("unchecked")
	public N getOriginal() {
		BaseNode original = getBaseOriginal();
		if (original instanceof BaseSummaryNode) throw new IllegalStateException();
		return (N) original;
	}

	
	/**
	 * Get a collection of incoming edges
	 * 
	 * @return a collection of edges
	 */
	@SuppressWarnings("unchecked")
	public List<E> getIncomingEdges() {
		return (List<E>) incoming;
	}
	
	
	/**
	 * Get a collection of outgoing edges
	 * 
	 * @return a collection of edges
	 */
	@SuppressWarnings("unchecked")
	public List<E> getOutgoingEdges() {
		return (List<E>) outgoing;
	}
	
	
	/**
	 * Get a collection of endpoints of incoming edges
	 * 
	 * @return a collection of nodes
	 */
	@SuppressWarnings("unchecked")
	public List<N> getIncomingNodes() {
		return (List<N>) getIncomingBaseNodes();
	}
	
	
	/**
	 * Get a collection of endpoints of outgoing edges
	 * 
	 * @return a collection of nodes
	 */
	@SuppressWarnings("unchecked")
	public List<N> getOutgoingNodes() {
		return (List<N>) getOutgoingBaseNodes();
	}

	
	/**
	 * Calculate the hash code
	 * 
	 * @return the hash code
	 */
	public int hashCode() {
		return index;
	}
	
	
	/**
	 * Indicates whether some other object is "equal to" this one
	 * 
	 * @param obj the other object
	 * @return true if the two are equal
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof Node<?, ?>)) return false;
		Node<?, ?> n = (Node<?, ?>) obj;
		return n.graph == graph && n.index == index;
	}
}
