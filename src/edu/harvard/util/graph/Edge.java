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
 * 
 * @param <N> the node type
 */
public class Edge<N extends BaseNode> extends BaseEdge implements Serializable {
	
	private static final long serialVersionUID = -8662345946082424442L;


	/**
	 * Create an instance of class Edge
	 * 
	 * @param from the from node
	 * @param to the to node
	 */
	public Edge(N from, N to) {
		super(from, to);
	}


	/**
	 * Create an instance of class Edge
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @param label the edge label
	 */
	public Edge(N from, N to, String label) {
		super(from, to, label);
	}
	
	
	/**
	 * Create an instance of class Edge using a specified index. Use at your own risk.
	 * 
	 * @param index the edge index
	 * @param from the from node
	 * @param to the to node
	 * @param label the edge label
	 */
	protected Edge(int index, N from, N to, String label) {
		super(index, from, to, label);
	}
	

	/**
	 * Return the from node
	 *
	 * @return the from node
	 */
	@SuppressWarnings("unchecked")
	public N getFrom() {
		return (N) from;
	}
	

	/**
	 * Return the to node
	 *
	 * @return the to node
	 */
	@SuppressWarnings("unchecked")
	public N getTo() {
		return (N) to;
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
		if (!(obj instanceof Edge<?>)) return false;
		Edge<?> e = (Edge<?>) obj;
		return e.from.index == from.index && e.to.index == to.index;
	}
}
