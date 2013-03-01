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
import java.lang.ref.WeakReference;
import java.util.*;

import org.jgrapht.EdgeFactory;

import edu.harvard.util.graph.layout.*;
import edu.harvard.util.Pair;
import edu.harvard.util.Utils;


/**
 * A graph represented as a collection of nodes and edges
 * 
 * @author Peter Macko
 */
public abstract class BaseGraph implements Serializable {
	
	private static final long serialVersionUID = 6948955479601114260L;
	
	
	// Underlying graph (without summarization)
	
	Vector<BaseNode> nodes;
	Vector<BaseEdge> edges;
	
	
	// Summarization
	
	Vector<BaseSummaryNode> summaryNodes;
	Vector<BaseSummaryEdge> summaryEdges;
	BaseSummaryNode rootSummaryNode;
	
	HashMap<Pair<BaseNode, BaseNode>, BaseSummaryEdge> summaryEdgesMap;
	
	
	// Map: node ID --> node
	
	HashMap<Integer, BaseNode> nodeMap;
	
	
	// Computed graph layouts
	
	TreeMap<String, GraphLayout> layoutMap;
	GraphLayout defaultLayout;
	
	
	// Overlays
	
	HashMap<String, PerNodeAttribute<?>> overlayNodeAttributes;
	
	
	// Counts of nodes, edges, and summary nodes
	
	int numNodes;
	int numEdges;
	int numSummaryNodes;
	
	
	// State
	
	transient boolean summarizationActive;
	
	
	// Caches
	
	transient JGraphTDirectedAdapter adapterJGraphT;
	transient JGraphTInvertedAdapter adapterJGraphTInverted;
	transient JGraphTUndirectedAdapter adapterJGraphTUndirected;
	
	
	/**
	 * Create an instance of class Graph
	 */
	public BaseGraph() {
		
		clear();
	}
	
	
	/**
	 * Clear the graph
	 */
	protected void clear() {
		
		this.nodes = new Vector<BaseNode>();
		this.edges = new Vector<BaseEdge>();
		this.summaryNodes = new Vector<BaseSummaryNode>();
		this.summaryEdges = new Vector<BaseSummaryEdge>();
		this.summaryEdgesMap = new HashMap<Pair<BaseNode, BaseNode>, BaseSummaryEdge>();
		
		this.numNodes = 0;
		this.numEdges = 0;
		this.numSummaryNodes = 0;
		
		this.nodeMap = new HashMap<Integer, BaseNode>();
		this.rootSummaryNode = null;
		this.summarizationActive = true;
		
		this.layoutMap = new TreeMap<String, GraphLayout>();
		this.defaultLayout = null;
		
		this.overlayNodeAttributes = new HashMap<String, PerNodeAttribute<?>>();
		
		this.adapterJGraphT = null;
		this.adapterJGraphTInverted = null;
		this.adapterJGraphTUndirected = null;
	}
	
	
	/**
	 * Add a node
	 * 
	 * @param node a new node
	 */
	synchronized void addBaseNode(BaseNode node) {
		
		if (node.graph != null && node.graph != this) {
			throw new RuntimeException("The node is already contained in another graph");
		}
		
		if (node.graph == this && node.index >= 0) {
			throw new RuntimeException("The node is already contained in the graph");
		}
		
		if (numSummaryNodes > 0) {
			throw new IllegalStateException("Cannot add a node to the graph after summary nodes have been created");
		}
		
		numNodes++;
		node.index = nodes.size();
		if (node.id < 0) node.id = node.index;
		
		node.graph = this;
		nodes.add(node);
		nodeMap.put(node.id, node);
	}
	
	
	/**
	 * Add a node using the specified index. Use at your own risk.
	 * 
	 * @param node a new node
	 */
	synchronized void addBaseNodeExt(BaseNode node) {
		
		if (node.graph != null && node.graph != this) {
			throw new RuntimeException("The node is already contained in another graph");
		}
		
		if (node.index < 0) {
			throw new RuntimeException("The index is not specified");
		}
		
		if (numSummaryNodes > 0) {
			throw new IllegalStateException("Cannot add a node to the graph after summary nodes have been created");
		}
		
		for (int i = nodes.size(); i <= node.index; i++) nodes.add(null);
		numNodes = nodes.size();
		
		node.graph = this;
		nodes.set(node.index, node);
		nodeMap.put(node.id, node);
	}
	
	
	/**
	 * Add an edge
	 * 
	 * @param edge a new edge
	 */
	synchronized void addBaseEdge(BaseEdge edge) {
		
		if (edge.from.graph != this) {
			throw new RuntimeException("The edge is already contained in another graph");
		}
		
		if (numSummaryNodes > 0) {
			throw new IllegalStateException("Cannot add an edge to the graph after summary nodes have been created");
		}
		
		numEdges++;
		edge.index = edges.size(); 
		edges.add(edge);
		
		edge.from.outgoing.add(edge);
		edge.to.incoming.add(edge);
	}
	
	
	/**
	 * Add an edge using the specified index. Use at your own risk.
	 * 
	 * @param edge a new edge
	 */
	synchronized void addBaseEdgeExt(BaseEdge edge) {
		
		if (edge.from.graph != this) {
			throw new RuntimeException("The edge is already contained in another graph");
		}
		
		if (edge.index < 0) {
			throw new RuntimeException("The index is not specified");
		}
		
		if (numSummaryNodes > 0) {
			throw new IllegalStateException("Cannot add an edge to the graph after summary nodes have been created");
		}
		
		for (int i = edges.size(); i <= edge.index; i++) edges.add(null);
		numEdges = edges.size();
		edges.set(edge.index, edge);
		
		edge.from.outgoing.add(edge);
		edge.to.incoming.add(edge);
	}
	
	
	/**
	 * Add a summary node
	 * 
	 * @param node a new summary node (must have no children at this point)
	 */
	synchronized void addBaseSummaryNode(BaseSummaryNode node) {
		
		if (node.graph != this) {
			throw new RuntimeException("The summary node has not been constructed for this graph");
		}
		
		if (node.graph == this && node.index >= 0) {
			throw new RuntimeException("The summary node is already contained in the graph");
		}
		
		node.index = numNodes + numSummaryNodes;
		summaryNodes.add(node);
		numSummaryNodes++;
		
		if (rootSummaryNode == null) rootSummaryNode = node;
	}
	
	
	/**
	 * Add a summary edge
	 * 
	 * @param edge a new edge
	 */
	private synchronized void addBaseSummaryEdge(BaseSummaryEdge edge) {
		
		if (edge.from.graph != this) {
			throw new RuntimeException("The edge is already contained in another graph");
		}
		
		edge.index = numEdges + summaryEdges.size(); 
		summaryEdges.add(edge);
		summaryEdgesMap.put(new Pair<BaseNode, BaseNode>(edge.getBaseFrom(), edge.getBaseTo()), edge);
	}
	
	
	/**
	 * Create an empty root summary node. Please override to provide type safety for typed graphs
	 * 
	 * @return the instantiated summary node of type SummaryNode<?, ?> if possible, or BaseSummaryNode if not 
	 */
	protected BaseSummaryNode newRootSummaryNode() {
		return new BaseSummaryNode(this);
	}

	
	/**
	 * Create an empty summary node. Please override to provide type safety for typed graphs
	 * 
	 * @param parent the parent node
	 * @return the instantiated summary node of type SummaryNode<?, ?> if possible, or BaseSummaryNode if not 
	 */
	public BaseSummaryNode newSummaryNode(BaseSummaryNode parent) {
		return new BaseSummaryNode(parent);
	}
	
	
	/**
	 * Create & initialize the root summary node. No other nodes can be added to the graph after this
	 */
	synchronized void createRootSummaryNode() {
		
		if (numSummaryNodes > 0 || rootSummaryNode != null) {
			throw new IllegalStateException("The graph already contains a root summary node");
		}
		
		addBaseSummaryNode(newRootSummaryNode());
		
		for (BaseNode n : nodes) {
			rootSummaryNode.addChild(n);
		}
	}
	
	
	/**
	 * Recursively register all internal edges of summaries
	 * 
	 * @param node the node
	 */
	private void registerInternalSummaryEdges(BaseSummaryNode node) {
		
		for (BaseEdge e : node.getInternalEdges()) {
			if (e instanceof BaseSummaryEdge) addBaseSummaryEdge((BaseSummaryEdge) e);
		}
		
		for (BaseNode n : node.getBaseChildren()) {
			if (n instanceof BaseSummaryNode) {
				BaseSummaryNode s = (BaseSummaryNode) n;
				if (s.children != null) registerInternalSummaryEdges(s);
			}
		}
	}
	
	
	/**
	 * Start graph summarization. No other nodes can be added to the graph after this
	 */
	public synchronized void summarizationBegin() {
		
		if (rootSummaryNode == null) createRootSummaryNode();
		summarizationActive = true;
	}
	
	
	/**
	 * Finish graph summarization. No summary nodes can be added or modified after this point
	 */
	public synchronized void summarizationEnd() {
		
		summarizationActive = false;
		registerInternalSummaryEdges(rootSummaryNode);
	}
	
	
	/**
	 * Get the largest node index
	 * 
	 * @return the largest node index
	 */
	public int getMaxNodeIndex() {
		return numNodes + numSummaryNodes - 1;
	}
	
	
	/**
	 * Get the largest node index for the underlying (i.e. non-summary nodes)
	 * 
	 * @return the largest node index
	 */
	public int getMaxNodeIndexBase() {
		return numNodes - 1;
	}
	
	
	/**
	 * Get the largest edge index
	 * 
	 * @return the largest edge index
	 */
	public int getMaxEdgeIndex() {
		return edges.size() + summaryEdges.size() - 1;
	}

	
	/**
	 * Return the graph nodes
	 *
	 * @return the collection of nodes
	 */
	public Collection<BaseNode> getBaseNodes() {
		return nodes;
	}
	

	/**
	 * Return the graph edges
	 *
	 * @return the collection of edges
	 */
	public Collection<BaseEdge> getBaseEdges() {
		return edges;
	}
	
	
	/**
	 * Get a node by its index
	 * 
	 * @param index the node index
	 * @return the node
	 */
	public BaseNode getBaseNode(int index) {
		if (index < numNodes) {
			return nodes.get(index);
		}
		else if (index < numNodes + summaryNodes.size()) {
			return summaryNodes.get(index - numNodes);
		}
		else {
			return null;
		}
	}
	
	
	/**
	 * Get an edge by its index
	 * 
	 * @param index the edge index
	 * @return the edge
	 */
	public BaseEdge getBaseEdge(int index) {
		if (index < numEdges) {
			return edges.get(index);
		}
		else if (index < numEdges + summaryEdges.size()) {
			return summaryEdges.get(index - numEdges);
		}
		else {
			return null;
		}
	}
	
	
	/**
	 * Get a node by its ID
	 * 
	 * @param id the node ID
	 * @return the node, or null if not fount
	 */
	public BaseNode getBaseNodeByID(int id) {
		return nodeMap.get(id);
	}
	
	
	/**
	 * Get an edge by specifying the two endpoints. If more than one such
	 * edge exits, pick one arbitrarily.
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @return the edge, or null if not found
	 */
	public BaseEdge getBaseEdgeExt(BaseNode from, BaseNode to) {
		
		if (!(from instanceof BaseSummaryNode) && !(to instanceof BaseSummaryNode)) {
			
			// Check the edges in the underlying (unsummarized) graph

			Collection<BaseEdge> f = from.getOutgoingBaseEdges();
			Collection<BaseEdge> t =   to.getIncomingBaseEdges();
			Collection<BaseEdge> c = f.size() < t.size() ? f : t;
			for (BaseEdge e : c) {
				if (e.getBaseFrom() == from && e.getBaseTo() == to) return e;
			}
			for (BaseEdge e : c) {
				if (e.getBaseFrom().equals(from) && e.getBaseTo().equals(to)) return e;
			}
		}
		
		
		// If there are no such edges, get it from the summary edge map
			
		Pair<BaseNode, BaseNode> p = new Pair<BaseNode, BaseNode>(from, to);
		BaseEdge e = summaryEdgesMap.get(p);
		if (e != null) return e;
		
		
		// If there is really no such edge, try to create it, if it really exists
		
		Collection<BaseEdge> c = getAllBaseEdges(from, to);
		if (!c.isEmpty()) {
			BaseSummaryEdge s = new BaseSummaryEdge(from, to, c);
			addBaseSummaryEdge(s);
			return s;
		}
		
		
		// There is no such edge
		
		return null;
	}
	
	
	/**
	 * Get or create a summary edge from a collection of base edges
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @param baseEdges the collection of base edges
	 * @return the summary edge
	 */
	public BaseSummaryEdge getOrCreateBaseSummaryEdge(BaseNode from, BaseNode to, Collection<BaseEdge> baseEdges) {
		
		Pair<BaseNode, BaseNode> p = new Pair<BaseNode, BaseNode>(from, to);
		BaseSummaryEdge e = summaryEdgesMap.get(p);
		if (e != null) return e;
		
		e = new BaseSummaryEdge(from, to, baseEdges);
		addBaseSummaryEdge(e);
		
		return e;
	}
	
	
	/**
	 * Get an edge by specifying the two endpoints. If more than one such
	 * edge exits, pick one arbitrarily.
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @return the edge
	 * @throws NoSuchElementException if there is no such edge
	 */
	public BaseEdge getBaseEdge(BaseNode from, BaseNode to) {
		BaseEdge e = getBaseEdgeExt(from, to);
		if (e == null) throw new NoSuchElementException("No such edge");
		return e;
	}
	
	
	/**
	 * Get the root (main) node group
	 * 
	 * @return the main node group
	 */
	public BaseSummaryNode getRootBaseSummaryNode() {
		return rootSummaryNode;
	}
	
	
	/**
	 * Find all actual (not summary) edges between two (potentially summary) nodes.
	 * Note that the edges are not traversed transitively
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @return the collection of all such base edges
	 */
	public Collection<BaseEdge> getAllBaseEdges(BaseNode from, BaseNode to) {
		
		HashSet<BaseNode> fromNodes = new HashSet<BaseNode>();
		HashSet<BaseNode> toNodes = new HashSet<BaseNode>();
		
		if (from instanceof BaseSummaryNode) {
			((BaseSummaryNode) from).collectActualBaseNodes(fromNodes);
		}
		else {
			fromNodes.add(from);
		}
		
		if (to instanceof BaseSummaryNode) {
			((BaseSummaryNode) to).collectActualBaseNodes(toNodes);
		}
		else {
			toNodes.add(to);
		}
		
		Collection<BaseEdge> edges = new HashSet<BaseEdge>();
		
		for (BaseNode f : fromNodes) {
			for (BaseEdge e : f.getOutgoingBaseEdges()) {
				if (toNodes.contains(e.getBaseTo())) edges.add(e);
			}
		}
		
		return edges;
	}
	
	
	/**
	 * Find all actual (not summary) edges between two (potentially summary) nodes.
	 * Note that the edges are not traversed transitively
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @return the set of all such base edges
	 */
	public Set<BaseEdge> getAllBaseEdgesAsSet(BaseNode from, BaseNode to) {
		
		Collection<BaseEdge> c = getAllBaseEdges(from, to);
		if (c instanceof Set<?>) return (Set<BaseEdge>) c;
		
		HashSet<BaseEdge> set = new HashSet<BaseEdge>();
		for (BaseEdge e : c) set.add(e);
		
		return set;
	}
	
	
	/**
	 * Determine if this graph is a tree and if so, find the root
	 * 
	 * @return the tree root, or null if not a tree
	 */
	public BaseNode findBaseTreeRoot() {
		
		if (nodes.isEmpty()) return null;
		if (nodes.size() != edges.size() + 1) return null; 
		
		for (boolean dirOutgoing : new boolean[] { true, false }) {
			
			// Find a candidate root
			
			boolean forceFail = false;			
			BaseNode root = null;
			
			for (BaseNode n : nodes) {
				if ((dirOutgoing ? n.getOutgoingBaseEdges() : n.getIncomingBaseEdges()).isEmpty()) {
					if (root == null) {
						root = n;
					}
					else {
						forceFail = true;
						break;
					}
				}
			}
			
			if (forceFail) continue;
			
			
			// Check the tree structure
			
			HashSet<BaseNode> visited = new HashSet<BaseNode>();
			Stack<BaseNode> stack = new Stack<BaseNode>();
			
			visited.add(root);
			stack.push(root);
			
			while (!stack.isEmpty()) {
				BaseNode n = stack.pop();
				Collection<BaseEdge> edges =  dirOutgoing ? n.getOutgoingBaseEdges() : n.getIncomingBaseEdges();
				
				for (BaseEdge e : edges) {
					BaseNode other = e.getBaseOther(n);
					if (visited.contains(other)) {
						forceFail = true;
						break;
					}
					else {
						visited.add(other);
						
						Collection<BaseEdge> parents = !dirOutgoing
								? other.getOutgoingBaseEdges() : other.getIncomingBaseEdges();
						if (parents.size() != 1) {
							forceFail = true;
							break;
						}
						
						if (n != parents.iterator().next().getBaseOther(other)) {
							forceFail = true;
							break;
						}
					}
				}
				
				if (forceFail) break;
			}
			
			if (forceFail) continue;
			
			return root;
		}
		
		return null;
	}
	
	
	/**
	 * Add a graph layout to the base graph structure
	 * 
	 * @param layout the layout
	 */
	public void addLayout(GraphLayout layout) {
		if (layout.getGraph() != this) {
			throw new IllegalArgumentException("The graph layout was generated for another graph");
		}
		
		layoutMap.put(layout.getDescription(), layout);
		if (defaultLayout == null) defaultLayout = layout;
	}
	
	
	/**
	 * Retrieve a layout based on its name
	 * 
	 * @param name the layout name or description
	 * @return the layout, or null if does not exist
	 */
	public GraphLayout getLayout(String name) {
		return layoutMap.get(name);
	}
	
	
	/**
	 * Retrieve the default graph layout
	 * 
	 * @return the default (or current) graph layout
	 */
	public GraphLayout getDefaultLayout() {
		return defaultLayout;
	}
	
	/**
	 * Set the default graph layout
	 * 
	 * @param layout the default (or current) graph layout
	 */
	public void setDefaultLayout(GraphLayout layout) {
		addLayout(layout);	// If the layout is already there, this would be a NO-OP
		defaultLayout = layout;
	}
	
	
	/**
	 * Return a collection of layouts
	 * 
	 * @return a collection of layouts
	 */
	public Collection<GraphLayout> getLayouts() {
		return layoutMap.values();
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
		
		for (BaseNode n : getBaseNodes()) {
			if (!n.isVisible()) continue;
			out.println("  " + n.getIndex() + " [label=\"" + Utils.escapeSimple(n.getLabel()) + "\"];");
		}
		
		for (BaseEdge e : getBaseEdges()) {
			if (!e.getBaseFrom().isVisible()) continue;
			if (!e.getBaseTo().isVisible()) continue;
			out.println("  " + e.getBaseFrom().getIndex() + (directed ? " -> " : " -- ") + e.getBaseTo().getIndex() + ";");
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
	
	
	/**
	 * Add or update a per-node attribute overlay
	 * 
	 * @param overlay the overlay
	 */
	public void addPerNodeAttributeOverlay(PerNodeAttribute<?> overlay) {
		overlayNodeAttributes.put(overlay.getName(), overlay);
	}
	
	
	/**
	 * Get the map of all per-node attribute overlays
	 * 
	 * @return the map of names to overlays
	 */
	public Map<String, PerNodeAttribute<?>> getPerNodeAttributeOverlayMap() {
		return overlayNodeAttributes;
	}
	
	
	/**
	 * Get a specific per-node attribute overlay
	 * 
	 * @param name the overlay/attribute name
	 * @return the overlay, or null if not there
	 */
	public PerNodeAttribute<?> getPerNodeAttributeOverlay(String name) {
		return overlayNodeAttributes.get(name);
	}

	
	/**
	 * Get the JGraphT adapter for BaseGraph
	 * 
	 * @return the adapter
	 */
	public synchronized org.jgrapht.DirectedGraph<BaseNode, BaseEdge> getBaseJGraphTAdapter() {
		if (adapterJGraphT == null) {
			adapterJGraphT = new JGraphTDirectedAdapter();
		}
		return adapterJGraphT;
	}

	
	/**
	 * Get the JGraphT adapter for BaseGraph
	 * 
	 * @param direction the graph direction
	 * @return the adapter
	 */
	public synchronized org.jgrapht.Graph<BaseNode, BaseEdge> getBaseJGraphTAdapter(GraphDirection direction) {
		switch (direction) {
		case DIRECTED: return getBaseJGraphTAdapter();
		case UNDIRECTED: return getBaseJGraphTUndirectedAdapter();
		case INVERTED: return getBaseJGraphTInvertedAdapter();
		default: throw new IllegalArgumentException();
		}
	}

	
	/**
	 * Get the JGraphT adapter for a variant of BaseGraph with inverted edges
	 * 
	 * @return the adapter
	 */
	public synchronized org.jgrapht.DirectedGraph<BaseNode, BaseEdge> getBaseJGraphTInvertedAdapter() {
		if (adapterJGraphTInverted == null) {
			adapterJGraphTInverted = new JGraphTInvertedAdapter();
		}
		return adapterJGraphTInverted;
	}

	
	/**
	 * Get the JGraphT adapter for the undirected version of BaseGraph
	 * 
	 * @return the adapter
	 */
	public synchronized org.jgrapht.Graph<BaseNode, BaseEdge> getBaseJGraphTUndirectedAdapter() {
		if (adapterJGraphTUndirected == null) {
			adapterJGraphTUndirected = new JGraphTUndirectedAdapter();
		}
		return adapterJGraphTUndirected;
	}
	
	
	/**
	 * The JGraphT adapter for an undirected BaseGraph
	 */
	protected class JGraphTUndirectedAdapter implements org.jgrapht.Graph<BaseNode, BaseEdge> {
		
		protected WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>> bothEdgesCache;
		
		protected WeakReference<Set<BaseNode>> allNodesCache;
		protected WeakReference<Set<BaseEdge>> allEdgesCache;
		
		protected boolean directed = false;
		
		public JGraphTUndirectedAdapter() {
			bothEdgesCache = new WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>>();
			allNodesCache = new WeakReference<Set<BaseNode>>(null);
			allEdgesCache = new WeakReference<Set<BaseEdge>>(null);
		}
		

		@Override
		public BaseEdge addEdge(BaseNode from, BaseNode to) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addEdge(BaseNode from, BaseNode to, BaseEdge edge) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addVertex(BaseNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsEdge(BaseEdge edge) {
			return edge.getGraph() == BaseGraph.this;
		}

		@Override
		public boolean containsEdge(BaseNode from, BaseNode to) {
			if (getBaseEdgeExt(from, to) != null) return true;
			if (!directed) if (getBaseEdgeExt(to, from) != null) return true;
			return false;
		}

		@Override
		public boolean containsVertex(BaseNode node) {
			return node.getGraph() == BaseGraph.this;
		}

		@Override
		public synchronized Set<BaseEdge> edgeSet() {
			Set<BaseEdge> set = allEdgesCache.get();
			if (set == null || set.size() != edges.size()) {
				if (set == null) set = new HashSet<BaseEdge>();
				for (BaseEdge e : edges) set.add(e);
				allEdgesCache = new WeakReference<Set<BaseEdge>>(set);
			}
			return set;
		}

		@Override
		public synchronized Set<BaseEdge> edgesOf(BaseNode node) {
			List<BaseEdge> l1 = node.getIncomingBaseEdges();
			List<BaseEdge> l2 = node.getOutgoingBaseEdges();
			WeakReference<Set<BaseEdge>> w = bothEdgesCache.get(node);;
			Set<BaseEdge> set = w == null ? null : w.get();
			// Note: This condition does not handle self-loops
			if (set == null || set.size() != l1.size() + l2.size()) {
				if (set == null) set = new HashSet<BaseEdge>();
				for (BaseEdge e : l1) set.add(e);
				for (BaseEdge e : l2) set.add(e);
				bothEdgesCache.put(node, new WeakReference<Set<BaseEdge>>(set));
			}
			return set;
		}

		@Override
		public Set<BaseEdge> getAllEdges(BaseNode from, BaseNode to) {
			Set<BaseEdge> e = getAllBaseEdgesAsSet(from, to);
			if (!directed) {
				Set<BaseEdge> x = getAllBaseEdgesAsSet(to, from);
				if (e == null) e = new HashSet<BaseEdge>();
				for (BaseEdge edge : x) e.add(edge);
			}
			return e;
		}

		@Override
		public BaseEdge getEdge(BaseNode from, BaseNode to) {
			BaseEdge e = getBaseEdgeExt(from, to);
			if (e == null && !directed) {
				e = getBaseEdgeExt(to, from);
			}
			return e;
		}

		@Override
		public EdgeFactory<BaseNode, BaseEdge> getEdgeFactory() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BaseNode getEdgeSource(BaseEdge e) {
			return e.getBaseFrom();
		}

		@Override
		public BaseNode getEdgeTarget(BaseEdge e) {
			return e.getBaseTo();
		}

		@Override
		public double getEdgeWeight(BaseEdge e) {
			return 1;
		}

		@Override
		public boolean removeAllEdges(Collection<? extends BaseEdge> edges) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<BaseEdge> removeAllEdges(BaseNode from, BaseNode to) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAllVertices(Collection<? extends BaseNode> nodes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeEdge(BaseEdge edge) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BaseEdge removeEdge(BaseNode from, BaseNode to) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeVertex(BaseNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized Set<BaseNode> vertexSet() {
			Set<BaseNode> set = allNodesCache.get();
			if (set == null || set.size() != nodes.size()) {
				if (set == null) set = new HashSet<BaseNode>();
				for (BaseNode n : nodes) set.add(n);
				allNodesCache = new WeakReference<Set<BaseNode>>(set);
			}
			return set;
		}
	}
	
	
	/**
	 * The JGraphT adapter for a directed BaseGraph
	 */
	protected class JGraphTDirectedAdapter extends JGraphTUndirectedAdapter
		implements org.jgrapht.DirectedGraph<BaseNode, BaseEdge> {
		
		protected WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>> incomingEdgesCache;
		protected WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>> outgoingEdgesCache;
		
		public JGraphTDirectedAdapter() {
			incomingEdgesCache = new WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>>();
			outgoingEdgesCache = new WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>>();
			directed = true;
		}

		@Override
		public int inDegreeOf(BaseNode node) {
			return node.getIncomingBaseEdges().size();
		}

		@Override
		public synchronized Set<BaseEdge> incomingEdgesOf(BaseNode node) {
			List<BaseEdge> l = node.getIncomingBaseEdges();
			WeakReference<Set<BaseEdge>> w = incomingEdgesCache.get(node);;
			Set<BaseEdge> set = w == null ? null : w.get();
			if (set == null || set.size() != l.size()) {
				if (set == null) set = new HashSet<BaseEdge>();
				for (BaseEdge e : l) set.add(e);
				incomingEdgesCache.put(node, new WeakReference<Set<BaseEdge>>(set));
			}
			return set;
		}

		@Override
		public int outDegreeOf(BaseNode node) {
			return node.getOutgoingBaseEdges().size();
		}

		@Override
		public synchronized Set<BaseEdge> outgoingEdgesOf(BaseNode node) {
			List<BaseEdge> l = node.getOutgoingBaseEdges();
			WeakReference<Set<BaseEdge>> w = outgoingEdgesCache.get(node);;
			Set<BaseEdge> set = w == null ? null : w.get();
			if (set == null || set.size() != l.size()) {
				if (set == null) set = new HashSet<BaseEdge>();
				for (BaseEdge e : l) set.add(e);
				outgoingEdgesCache.put(node, new WeakReference<Set<BaseEdge>>(set));
			}
			return set;
		}
	}
	
	
	/**
	 * The JGraphT adapter for a directed BaseGraph with inverted edges
	 */
	protected class JGraphTInvertedAdapter extends JGraphTUndirectedAdapter
		implements org.jgrapht.DirectedGraph<BaseNode, BaseEdge> {
		
		protected WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>> incomingEdgesCache;
		protected WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>> outgoingEdgesCache;
		
		public JGraphTInvertedAdapter() {
			incomingEdgesCache = new WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>>();
			outgoingEdgesCache = new WeakHashMap<BaseNode, WeakReference<Set<BaseEdge>>>();
			directed = true;
		}

		@Override
		public boolean containsEdge(BaseNode from, BaseNode to) {
			return getBaseEdgeExt(to, from) != null;
		}

		@Override
		public BaseEdge getEdge(BaseNode from, BaseNode to) {
			return getBaseEdgeExt(to, from);
		}

		@Override
		public BaseNode getEdgeSource(BaseEdge e) {
			return e.getBaseTo();
		}

		@Override
		public BaseNode getEdgeTarget(BaseEdge e) {
			return e.getBaseFrom();
		}

		@Override
		public int inDegreeOf(BaseNode node) {
			return node.getOutgoingBaseEdges().size();
		}

		@Override
		public synchronized Set<BaseEdge> incomingEdgesOf(BaseNode node) {
			List<BaseEdge> l = node.getOutgoingBaseEdges();
			WeakReference<Set<BaseEdge>> w = outgoingEdgesCache.get(node);;
			Set<BaseEdge> set = w == null ? null : w.get();
			if (set == null || set.size() != l.size()) {
				if (set == null) set = new HashSet<BaseEdge>();
				for (BaseEdge e : l) set.add(e);
				outgoingEdgesCache.put(node, new WeakReference<Set<BaseEdge>>(set));
			}
			return set;
		}

		@Override
		public int outDegreeOf(BaseNode node) {
			return node.getIncomingBaseEdges().size();
		}

		@Override
		public synchronized Set<BaseEdge> outgoingEdgesOf(BaseNode node) {
			List<BaseEdge> l = node.getIncomingBaseEdges();
			WeakReference<Set<BaseEdge>> w = incomingEdgesCache.get(node);;
			Set<BaseEdge> set = w == null ? null : w.get();
			if (set == null || set.size() != l.size()) {
				if (set == null) set = new HashSet<BaseEdge>();
				for (BaseEdge e : l) set.add(e);
				incomingEdgesCache.put(node, new WeakReference<Set<BaseEdge>>(set));
			}
			return set;
		}
	}
}
