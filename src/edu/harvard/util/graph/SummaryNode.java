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

import edu.harvard.util.filter.Filter;


/**
 * A summary node
 * 
 * @author Peter Macko
 * 
 * @param <N> the node type
 * @param <E> the edge type
 * @param <S> the summary node type
 * @param <G> the graph type
 */
public class SummaryNode<N extends Node<N, E>, E extends Edge<N>, S extends SummaryNode<N, E, S, G>, G extends Graph<N, E, S, G>>
	   extends BaseSummaryNode implements Serializable {
	
	private static final long serialVersionUID = -7395140102453285124L;


	/**
	 * Create an instance of class SummaryNode
	 * 
	 * @param parent the parent node group (this adds this object to this parent)
	 */
	protected SummaryNode(S parent) {
		super(parent);
	}
	
	
	/**
	 * Create an instance of class SummaryNode
	 * 
	 * @param graph the parent graph
	 */
	protected SummaryNode(G graph) {
		super(graph);
	}

	
	/**
	 * Get the parent graph
	 * 
	 * @return the parent graph
	 */
	@SuppressWarnings("unchecked")
	public G getGraph() {
		return (G) graph;
	}
	
	
	/**
	 * Add (move) a node from the parent group 
	 * 
	 * @param node the node to add
	 */
	public void moveNodeFromParent(N node) {
		moveBaseNodeFromParent(node);
	}
	
	
	/**
	 * Add (move) a summary from the parent group, or in the case
	 * of the root group, directly from the parent group 
	 * 
	 * @param node the node to add
	 */
	public void moveNodeFromParent(S node) {
		moveBaseNodeFromParent(node);
	}
	
	
	/**
	 * Add (move) a node from an ancestor
	 * 
	 * @param node the node to add
	 */
	public void moveNodeFromAncestor(N node) {
		moveBaseNodeFromAncestor(node);
	}
	
	
	/**
	 * Add (move) a summary node from an ancestor
	 * 
	 * @param node the node to add
	 */
	public void moveNodeFromAncestor(S node) {
		moveBaseNodeFromAncestor(node);
	}

	
	/**
	 * Add (move) a node from the child group
	 * 
	 * @param node the node to add
	 */
	public void moveNodeFromChild(N node) {
		moveBaseNodeFromChild(node);
	}

	
	/**
	 * Add (move) a node from the child group
	 * 
	 * @param node the node to add
	 */
	public void moveNodeFromChild(S node) {
		moveBaseNodeFromChild(node);
	}

	
	/**
	 * Collect all summary nodes
	 * 
	 * @param out the output collection
	 */
	@SuppressWarnings("unchecked")
	public void collectSummaryNodes(Collection<S> out) {
		collectBaseSummaryNodes((Collection<BaseSummaryNode>) (Object) out);
	}
	
	
	/**
	 * Collect the actual (not summary) nodes
	 * 
	 * @param out the output collection
	 */
	@SuppressWarnings("unchecked")
	public void collectActualNodes(Collection<N> out) {
		collectActualBaseNodes((Collection<BaseNode>) (Object) out);
	}

	
	/**
	 * Checks whether any of the descendant nodes satisfy the given node filter
	 * 
	 * @param filter the filter
	 * @return true if at least one child node satisfies the given node filter
	 */
	@SuppressWarnings("unchecked")
	public boolean checkFilter(Filter<N> filter) {
		
		if (children == null) return false;
		
		for (BaseNode n : children) {
			
			if (n instanceof SummaryNode<?, ?, ?, ?>) {
				if (((S) n).checkFilter(filter)) return true;
				continue;
			}
			
			if (n instanceof Node<?, ?>) {
				if (filter.accept((N) n)) return true;
				continue;
			}
			
			throw new IllegalStateException("Encountered node " + n + " with an invalid type -- neither Node nor SummaryNode");
		}
		
		return false;
	}
}
