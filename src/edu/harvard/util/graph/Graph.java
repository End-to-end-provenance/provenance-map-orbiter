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
 * A graph represented as a collection of nodes and edges
 * 
 * @author Peter Macko
 * 
 * @param <N> the node type
 * @param <E> the edge type
 * @param <S> the summary node type
 * @param <G> the graph type
 */
public class Graph<N extends Node<N, E>, E extends Edge<N>, S extends SummaryNode<N, E, S, G>, G extends Graph<N, E, S, G>>
	   extends BaseGraph implements Serializable {
	
	private static final long serialVersionUID = -7395140102453285124L;


	/**
	 * Create an instance of class Graph
	 */
	public Graph() {
	}
	
	
	/**
	 * Add a node
	 * 
	 * @param node a new node
	 */
	public void addNode(N node) {
		addBaseNode(node);
	}
	
	
	/**
	 * Add a node using the specified index. Use at your own risk.
	 * 
	 * @param node a new node
	 */
	protected void addNodeExt(N node) {
		addBaseNodeExt(node);
	}
	
	
	/**
	 * Add an edge
	 * 
	 * @param edge a new edge
	 */
	public void addEdge(E edge) {
		addBaseEdge(edge);
	}
	
	
	/**
	 * Add an edge using the specified index. Use at your own risk.
	 * 
	 * @param edge a new edge
	 */
	protected void addEdgeExt(E edge) {
		addBaseEdgeExt(edge);
	}

	
	/**
	 * Return the graph nodes
	 *
	 * @return the collection of nodes
	 */
	@SuppressWarnings("unchecked")
	public Collection<N> getNodes() {
		return (Collection<N>) ((Object) nodes);
	}
	

	/**
	 * Return the graph edges
	 *
	 * @return the collection of edges
	 */
	@SuppressWarnings("unchecked")
	public Collection<E> getEdges() {
		return (Collection<E>) ((Object) edges);
	}
	
	
	/**
	 * Get a node by its index
	 * 
	 * @param index the node index
	 * @return the node
	 */
	@SuppressWarnings("unchecked")
	public N getNode(int index) {
		return (N) nodes.get(index);
	}
	
	
	/**
	 * Get an edge by its index
	 * 
	 * @param index the edge index
	 * @return the edge
	 */
	@SuppressWarnings("unchecked")
	public E getEdge(int index) {
		return (E) edges.get(index);
	}
	
	
	/**
	 * Get a node by its ID
	 * 
	 * @param id the node ID
	 * @return the node, or null if not fount
	 */
	@SuppressWarnings("unchecked")
	public N getNodeByID(int id) {
		return (N) nodeMap.get(id);
	}
	
	
	/**
	 * Create an empty root summary node. Must override
	 * 
	 * @return the instantiated summary node
	 */
	protected S newRootSummaryNode() {
		throw new UnsupportedOperationException();
	}
	
	
	/**
	 * Get the root (main) node group
	 * 
	 * @return the main node group
	 */
	@SuppressWarnings("unchecked")
	public S getRootSummaryNode() {
		return (S) rootSummaryNode;
	}
	
	
	/**
	 * Create an empty summary node. Must override
	 * 
	 * @param parent the parent node
	 * @return the instantiated summary node
	 */
	@Override
	public S newSummaryNode(BaseSummaryNode parent) {
		throw new UnsupportedOperationException();
	}
}
