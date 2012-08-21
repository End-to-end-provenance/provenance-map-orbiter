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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.harvard.util.Pair;
import edu.harvard.util.Utils;


/**
 * A group of base nodes
 * 
 * @author Peter Macko
 */
public class BaseSummaryNode extends BaseNode implements java.io.Serializable {

	private static final long serialVersionUID = -3453113606393905635L;
	
	HashSet<BaseNode> children;
	
	HashSet<BaseEdge> internalEdges;
	HashMap<Pair<BaseNode, BaseNode>, BaseEdge> internalEdgesMap;
	
	
	/**
	 * Create an instance of class BaseSummaryNode
	 * 
	 * @param parent the parent node group (this adds this object to this parent)
	 */
	protected BaseSummaryNode(BaseSummaryNode parent) {
		this(parent.graph);
		
		graph.addBaseSummaryNode(this);
		parent.addChild(this);
	}
	
	
	/**
	 * Create an instance of class BaseSummaryNode
	 * 
	 * @param graph the parent graph
	 */
	protected BaseSummaryNode(BaseGraph graph) {
		
		this.parent = null;
		this.graph = graph;
		
		this.children = null;
		this.internalEdges = null;
	}
	
	
	/**
	 * Add (move) a node or a group from the parent group, or in the case
	 * of the root group, directly from the parent group 
	 * 
	 * @param node the node to add
	 */
	public void moveBaseNodeFromParent(BaseNode node) {
		
		if (node.parent != parent) {
			throw new IllegalArgumentException("Trying to add a node that is not in the parent's collection");
		}
		
		
		// Move from the parent summary node to this node
		
		if (parent != null) {
			if (parent.children == null) throw new IllegalStateException();
			if (!parent.removeChild(node)) throw new IllegalStateException();
		}
		
		addChild(node);
		
		
		// Add the appropriate edges to this summary node,
		// and remove those that are now entirely contained in the summary node
		
		for (BaseEdge e : node.getIncomingBaseEdges()) {
			BaseNode n = e.getBaseFrom();
			if (contains(n)) {
				outgoing.remove(e);
			}
			else {
				incoming.add(e);
			}
		}
		
		for (BaseEdge e : node.getOutgoingBaseEdges()) {
			BaseNode n = e.getBaseTo();
			if (contains(n)) {
				incoming.remove(e);
			}
			else {
				outgoing.add(e);
			}
		}
	}
	
	
	/**
	 * Add (move) a node or a summary node from an ancestor
	 * 
	 * @param node the node to add
	 */
	public void moveBaseNodeFromAncestor(BaseNode node) {
		
		if (node == this) return;
		if (node.parent == this) return;
		
		if (node.parent == parent) {
			moveBaseNodeFromParent(node);
			return;
		}
		
		if (parent != null) {
			parent.moveBaseNodeFromAncestor(node);
			moveBaseNodeFromParent(node);
			return;
		}
		
		throw new IllegalArgumentException("Trying to add a node that is not in the collection of any ancestor");
	}
	
	
	/**
	 * Add (move) a node or a group from the child group
	 * 
	 * @param node the node to add
	 */
	public void moveBaseNodeFromChild(BaseNode node) {
		
		BaseSummaryNode from = node.parent;
		if (from == null || from.parent != this) {
			throw new IllegalArgumentException("Trying to add a node that is not in the this nodes collection");
		}
		
		
		// Move from the parent summary node to this node
		
		if (from.children == null) throw new IllegalStateException();
		if (!from.removeChild(node)) throw new IllegalStateException();
		
		addChild(node);
		
		
		// Add the appropriate edges to this summary node,
		// and remove those that are now entirely contained in the summary node
		
		for (BaseEdge e : node.getIncomingBaseEdges()) {
			BaseNode n = e.getBaseFrom();
			if (from.contains(n)) {
				from.outgoing.add(e);
			}
			else {
				from.incoming.remove(e);
			}
		}
		
		for (BaseEdge e : node.getOutgoingBaseEdges()) {
			BaseNode n = e.getBaseTo();
			if (from.contains(n)) {
				from.incoming.add(e);
			}
			else {
				from.outgoing.remove(e);
			}
		}
	}
	
	
	/**
	 * Unlink the empty summary node from its parent
	 */
	public void unlinkEmpty() {
		
		if (children != null && !children.isEmpty()) {
			throw new IllegalStateException("Not empty");
		}
		
		if (parent != null) {
			parent.children.remove(this);
		}
	}
	
	
	/**
	 * Collect all summary nodes
	 * 
	 * @param out the output collection
	 */
	public void collectBaseSummaryNodes(Collection<BaseSummaryNode> out) {
		
		out.add(this);
		
		for (BaseNode n : getBaseChildren()) {
			if (n instanceof BaseSummaryNode) {
				((BaseSummaryNode) n).collectBaseSummaryNodes(out);
			}
		}
	}
	
	
	/**
	 * Collect the actual (not summary) nodes
	 * 
	 * @param out the output collection
	 */
	public void collectActualBaseNodes(Collection<BaseNode> out) {
		
		for (BaseNode n : getBaseChildren()) {
			if (n instanceof BaseSummaryNode) {
				((BaseSummaryNode) n).collectActualBaseNodes(out);
			}
			else {
				out.add(n);
			}
		}
	}
	
	
	/**
	 * Count the number of nested summary nodes
	 * 
	 * @return the number of summary nodes, including this node
	 */
	public int countSummaryNodes() {
		
		int r = 1;
		
		for (BaseNode n : getBaseChildren()) {
			if (n instanceof BaseSummaryNode) {
				r += ((BaseSummaryNode) n).countSummaryNodes();
			}
		}
		
		return r;
	}
	
	
	/**
	 * Get the children
	 * 
	 * @return the set of children
	 */
	public Set<BaseNode> getBaseChildren() {
		return children == null ? NO_NODES : children;
	}
	
	
	/**
	 * Add a child
	 * 
	 * @param child the children node
	 */
	protected void addChild(BaseNode child) {
		
		if (!(this instanceof BaseSummaryNode)) throw new UnsupportedOperationException();
		if (!graph.summarizationActive) {
			throw new IllegalStateException("Trying to make a summary node while summarization is not in progress");
		}
		
		if (children == null) children = new HashSet<BaseNode>();
		
		child.parent = Utils.<BaseSummaryNode>cast(this);
		children.add(child);
		child.updateDepth();
		
		internalEdges = null;	// Drop the edge cache
	}
	
	
	/**
	 * Remove a child from the collection of children, but do not update the edge cache
	 * 
	 * @param child the children node
	 * @return true if the child was removed, or false if it was not there to begin with
	 */
	protected boolean removeChild(BaseNode child) {
		
		if (!(this instanceof BaseSummaryNode)) throw new UnsupportedOperationException();
		if (!graph.summarizationActive) {
			throw new IllegalStateException("Trying to make a summary node while summarization is not in progress");
		}
		
		if (children == null) return false;
		return children.remove(child);
	}
	
	
	/**
	 * Determine whether this node contains the other node
	 * 
	 * @param other the other node
	 * @return true if this node contains (is ancestor of) the given node
	 */
	public boolean contains(BaseNode other) {
		
		if (graph != other.graph) {
			throw new IllegalArgumentException("The other node is not in the same graph");
		}
		
		for (BaseNode a = other; a != null; a = a.parent) {
			if (a == this) return true;
		}
		
		return false;
	}
	
	
	/**
	 * Determine which immediate child of this node contains the other node
	 * 
	 * @param other the other node
	 * @return the child of this node that contains the other node, or null
	 */
	public BaseNode containedIn(BaseNode other) {
		
		if (graph != other.graph) {
			throw new IllegalArgumentException("The other node is not in the same graph");
		}
		
		for (BaseNode a = other; a != null; a = a.parent) {
			if (a.parent == this) return a;
		}
		
		return null;
	}
	
	
	/**
	 * Get the internal edges between the immediate children of this node.
	 * If the set has not been computed yet, it will be computed and memoized.
	 */
	public Set<BaseEdge> getInternalEdges() {
		
		if (children == null) return NO_EDGES;
		if (internalEdges != null) return internalEdges;
		
		
		// Compute the set
		
		internalEdges = new HashSet<BaseEdge>();
		internalEdgesMap = new HashMap<Pair<BaseNode, BaseNode>, BaseEdge>();
		
		for (BaseNode n : children) {
			for (BaseEdge e : n.getOutgoingBaseEdges()) {
				
				BaseNode f = containedIn(e.from);
				BaseNode t = containedIn(e.to);
				
				if (f == null || t == null) continue;
				Pair<BaseNode, BaseNode> p = new Pair<BaseNode, BaseNode>(f, t); 
				
				if (f == e.from && t == e.to) {
					internalEdges.add(e);
					internalEdgesMap.put(p, e);
				}
				else {
					BaseEdge summaryEdge = internalEdgesMap.get(p);
					if (summaryEdge == null) {
						summaryEdge = new BaseSummaryEdge(f, t, e);
					}
					else {
						if (!(summaryEdge instanceof BaseSummaryEdge)) throw new IllegalStateException();
						BaseSummaryEdge se = (BaseSummaryEdge) summaryEdge;
						se.addBaseEdge(e);
					}
					internalEdges.add(summaryEdge);
					internalEdgesMap.put(p, summaryEdge);
				}
			}
		}
		
		return internalEdges;
	}
	
	
	/**
	 * Check consistency of the information stored with this node
	 * 
	 * @throws IllegalStateException if a problem is encountered
	 */
	public void checkConsistency() {
		
		// Only summary nodes can have children
		
		if (!(this instanceof BaseSummaryNode)) {
			if (children != null) throw new IllegalStateException();
		}
		
		
		// Check the edges
		
		for (BaseEdge e : getIncomingBaseEdges()) {
			BaseNode n = e.getBaseFrom();
			BaseNode m = e.getBaseTo();
			
			if (m != this) {
				if (!contains(m)) throw new IllegalStateException();
			}
			
			if (n == this) {
				if (m != this) throw new IllegalStateException();
			}
			else {
				if (contains(n)) throw new IllegalStateException();
			}
		}
		
		for (BaseEdge e : getOutgoingBaseEdges()) {
			BaseNode n = e.getBaseTo();
			BaseNode m = e.getBaseFrom();
			
			if (m != this) {
				if (!contains(m)) throw new IllegalStateException();
			}
			
			if (n == this) {
				if (m != this) throw new IllegalStateException();
			}
			else {
				if (contains(n)) throw new IllegalStateException();
			}
		}
		
		
		// Check the children-parent relationships
		
		if (children != null) {
			for (BaseNode n : children) {
				if (n.parent != this ) throw new IllegalStateException();
				if (n.graph  != graph) throw new IllegalStateException();
			}
		}
		
		
		// Recursive step
		
		if (children != null) {
			for (BaseNode n : children) {
				if (n instanceof BaseSummaryNode) ((BaseSummaryNode) n).checkConsistency();
			}
		}
	}
	
	
	/**
	 * Dump the graph structure in a Graphviz format
	 * 
	 * @param out the writer
	 * @param title the graph title
	 * @param directed true if the graph should be exported as directed
	 */
	public void dumpGraphviz(PrintStream out, String title, boolean directed) {
		out.println((directed ? "digraph" : "graph") + " \"" + title + "\" {");
		for (BaseEdge e : getInternalEdges()) {
			out.println("  " + e.getBaseFrom().getIndex() + (directed ? " -> " : " -- ") + e.getBaseTo().getIndex());
		}
		out.println("}");
	}
	
	
	/**
	 * Dump the graph structure in a Graphviz format
	 * 
	 * @param file the file to write to
	 * @param title the graph title
	 * @param directed true if the graph should be exported as directed
	 * @throws IOException on I/O error
	 */
	public void dumpGraphviz(File file, String title, boolean directed) throws IOException {
		PrintStream out = new PrintStream(file);
		dumpGraphviz(out, title, directed);
		out.close();
	}
}
