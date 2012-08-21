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
 */
public class BaseNode implements Serializable, Comparable<BaseNode> {
	
	private static final long serialVersionUID = 3394763644050877499L;
	static final Set<BaseNode> NO_NODES = Collections.<BaseNode>emptySet();
	static final Set<BaseEdge> NO_EDGES = Collections.<BaseEdge>emptySet();

	
	// Graph and index within the graph
	
	int index;
	BaseGraph graph;
	
	
	// Node visibility
	
	boolean visible;
	
	
	// Edges
	
	protected HashSet<BaseEdge> incoming;
	protected HashSet<BaseEdge> outgoing;
	
	
	// Summarization
	
	BaseSummaryNode parent;	// parent node (group)
	int depth;
	
	
	// Label & ID
	
	int id;
	protected String label;
	
	
	/**
	 * Create an instance of BaseNode
	 */
	public BaseNode() {
		
		this(-1);	// The index will be assigned upon graph addition
	}
	
	
	/**
	 * Create an instance of BaseNode using a specified index. Use at your own risk.
	 * 
	 * @param index the index
	 */
	protected BaseNode(int index) {
		
		this.id = -1;
		this.index = index;
		this.graph = null;
		this.visible = true;
		
		this.incoming = new HashSet<BaseEdge>();
		this.outgoing = new HashSet<BaseEdge>();
		
		this.parent = null;
		this.depth = 0;
		
		this.label = "";
	}

	
	/**
	 * Get the node ID
	 * 
	 * @return the node ID, or -1 if not assigned
	 */
	public int getID() {
		return id;
	}

	
	/**
	 * Set the node ID
	 * 
	 * @param id the new node ID (must be positive or 0)
	 */
	public void setID(int id) {
		
		if (graph != null) {
			if (graph.nodeMap.get(this.id) == this) {
				graph.nodeMap.remove(this.id);
			}
		}
		
		if (id < 0) {
			this.id = -1;
			return;
		}
		
		this.id = id;
		
		if (graph != null) {
			graph.nodeMap.put(this.id, this);
		}
	}
	
	
	/**
	 * Get the node label
	 * 
	 * @return the node label
	 */
	public String getLabel() {
		return label;
	}
	
	
	/**
	 * Set the node label
	 * 
	 * @param label the new node label
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	
	/**
	 * Get a collection of incoming edges
	 * 
	 * @return a collection of edges
	 */
	public Collection<BaseEdge> getIncomingBaseEdges() {
		return incoming;
	}
	
	
	/**
	 * Get a collection of outgoing edges
	 * 
	 * @return a collection of edges
	 */
	public Collection<BaseEdge> getOutgoingBaseEdges() {
		return outgoing;
	}
	
	
	/**
	 * Get a collection of endpoints of incoming edges
	 * 
	 * @return a collection of nodes
	 */
	public Collection<BaseNode> getIncomingBaseNodes() {
		HashSet<BaseNode> s = new HashSet<BaseNode>();
		for (BaseEdge e : incoming) s.add(e.getBaseFrom());
		return s;
	}
	
	
	/**
	 * Get a collection of endpoints of outgoing edges
	 * 
	 * @return a collection of nodes
	 */
	public Collection<BaseNode> getOutgoingBaseNodes() {
		HashSet<BaseNode> s = new HashSet<BaseNode>();
		for (BaseEdge e : outgoing) s.add(e.getBaseFrom());
		return s;
	}

	
	/**
	 * Get a numerical index (meaningful only in the context of its parent graph)
	 * 
	 * @return an index of the node
	 */
	public int getIndex() {
		return index;
	}
	
	
	/**
	 * Update the node depth
	 */
	protected void updateDepth() {
		depth = parent == null ? 0 : parent.depth + 1;
		if (this instanceof BaseSummaryNode) {
			Collection<BaseNode> children = ((BaseSummaryNode) this).getBaseChildren();
			if (children != null) for (BaseNode n : children) n.updateDepth();
		}
	}
	
	
	/**
	 * Get the node depth in the summary hierarchy
	 * 
	 * @return the node depth
	 */
	public int getDepth() {
		return depth;
	}

	
	/**
	 * Get the parent graph
	 * 
	 * @return the parent graph
	 */
	public BaseGraph getGraph() {
		return graph;
	}
	
	
	/**
	 * Determine whether the node is visible
	 * 
	 * @return true if it is visible
	 */
	public boolean isVisible() {
		return visible;
	}
	
	
	/**
	 * Set whether the node should be visible. Note that the changes
	 * might not get fully reflected until the summaries and the graph
	 * layout are recomputed.
	 * 
	 * @param v true if the node should be visible
	 */
	public void setVisible(boolean v) {
		visible = v;
	}
	
	
	/**
	 * Get the parent node group
	 * 
	 * @return the node group
	 */
	public BaseSummaryNode getParent() {
		return parent;
	}
	
	
	/**
	 * Get the common ancestor. The search algorithm is optimized for the case
	 * when "this" is an ancestor of "other."
	 * 
	 * @param other the other node
	 * @return the common ancestor
	 * @throws IllegalStateException if there is no common ancestor
	 */
	public BaseNode getCommonAncestor(BaseNode other) {
		
		if (graph != parent.graph) {
			throw new IllegalArgumentException("The other node is not in the same graph");
		}
		
		if (this == other) return this;
		
		for (BaseNode a = this; a != null; a = a.parent) {
			for (BaseNode b = other; b != null; b = b.parent) {
				if (a == b) return a;
			}
		}
		
		throw new IllegalStateException("No common ancestor");
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
		if (!(obj instanceof BaseNode)) return false;
		BaseNode n = (BaseNode) obj;
		return n.graph == graph && n.index == index;
	}
	
	
	/**
	 * Return a string version of the node
	 * 
	 * @return the string version
	 */
	public String toString() {
		return "[" + id + ":" + index + "]";
	}


	/**
	 * Compare this object to another instance of BaseNode
	 * 
	 * @param other the other instance
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(BaseNode other) {
		if (other.graph != graph) throw new IllegalArgumentException("Cannot compare nodes from different graphs");
		return index - other.index;
	}
}
