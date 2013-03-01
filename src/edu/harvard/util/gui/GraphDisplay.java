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

package edu.harvard.util.gui;

import edu.harvard.util.filter.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.graph.layout.*;
import edu.harvard.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;


/**
 * The graph display
 * 
 * @author Peter Macko
 * 
 * @param <N> the node type
 * @param <E> the edge type
 * @param <S> the summary node type
 * @param <G> the graph type
 */
@SuppressWarnings("serial")
public class GraphDisplay<N extends Node<N, E>, E extends Edge<N>, S extends SummaryNode<N, E, S, G>, G extends Graph<N, E, S, G>>
	   extends JPanel {

	private static final double FONT_SCALE = 1;
	private static final double NODE_HALF_WIDTH_FACTOR = 1.2;
	private static final double NODE_HALF_WIDTH_EXTRA  = 4;
	
	private static final double CLIP_MARGIN = 1000;
	private static final int ZOOM_BUTTON_SIZE = 20;
	
	
	// Debugging

	private static boolean DEBUG_NODE_VISIBILITY = false;
	private static boolean DEBUG_SEMANTIC_ZOOM = false;
	private static boolean DEBUG_PERFORMANCE = false;
	private static boolean DEBUG_RENDER_NODE = false;
	
	
	// Event handler
	
	private EventHandler handler;
	
	
	// Graph, its layout, decorator, and listeners for graph events
	
	private G graph;
	private GraphLayout layout;
	private GraphDecorator<N, E, S, G> decorator;
	private LinkedList<GraphDisplayListener<N, E, S, G>> listeners;
	
	
	// Node filters for filtering and highlighting, and a set of selected nodes

	private FilterSet<N> nodeFilters;
	private FilterSet<N> nodeHighlightFilters;
	private FilterSet<N> nodeJointHighlightFilter;
	private HashSet<BaseNode> selectedNodes;
	
	
	// Graph navigation - pan (offset) and zoom

	private double baseScale;
	private double zoomToScale;
	private int zoom;
	
	private double offsetX;
	private double offsetY;
	
	
	// Flags affecting the display
	
	private boolean drawNodes;
	private boolean drawNodeLabels;
	private boolean drawArrows;
	private boolean drawSplines;
	private boolean drawNodesAsPoints;
	private boolean drawRootSummaryNode;
	private boolean drawExpandedNodes;
	private boolean drawThickEdgesFromToSelectedNodes;
	private boolean drawShadedEdgesOutsideOfDisplay;
	
	private int arrowHeadSize;
	private int arrowMinLength;
	private int pointSize;
	
	
	// Flags affecting the behavior
	
	private boolean semanticZoom;
	private int semanticZoomSizeThreshold;
	private boolean reconfigureBasedOnLayout;
	private boolean autoManageSelection;
	
	private boolean filterOnOriginals;

	
	// Font
	
	private Font defaultFont;
	private float defaultFontHeight;
	private FontMetrics defaultFontMetrics;
	
	
	// The graph cache
	
	private HashSet<GraphLayoutNode> nodes;
	private HashSet<GraphLayoutEdge> edges;
	private HashSet<GraphLayoutNode> summaryNodes;
	private HashSet<BaseSummaryNode> expandedSummaryNodes;
	private HashSet<GraphLayoutNode> highlightedNodes;
	private HashSet<GraphLayoutNode> selectedLayoutNodes;
	private String longestLabel;

	
	/**
	 * Constructor of class GraphDisplay
	 */
	public GraphDisplay() {
	
		graph = null;
		
		
		// Graph navigation - pan (offset) and zoom
		
		baseScale = 1;
		zoomToScale = 1.2;
		zoom = 1;
		offsetX = 0;
		offsetY = 0;
		
		
		// Flags affecting the display
		
		drawNodes = true;
		drawNodeLabels = true;
		drawArrows = false;
		drawSplines = true;
		drawNodesAsPoints = false;
		drawRootSummaryNode = false;
		drawExpandedNodes = false;
		drawThickEdgesFromToSelectedNodes = true;
		drawShadedEdgesOutsideOfDisplay = true;
		
		arrowHeadSize = 9;
		arrowMinLength = 15;
		pointSize = 6;
		
		
		// Flags affecting the behavior
		
		semanticZoom = false;
		semanticZoomSizeThreshold = 45;
		reconfigureBasedOnLayout = true;
		autoManageSelection = true;
		
		filterOnOriginals = true;
		
		
		// The graph cache
		
		nodes = new HashSet<GraphLayoutNode>();
		edges = new HashSet<GraphLayoutEdge>();
		summaryNodes = new HashSet<GraphLayoutNode>();
		expandedSummaryNodes = new HashSet<BaseSummaryNode>();
		highlightedNodes = new HashSet<GraphLayoutNode>();
		
		
		// Font
		
		JLabel l = new JLabel();
		defaultFontHeight = (float) (12 * FONT_SCALE);
		defaultFont = l.getFont().deriveFont(defaultFontHeight);
		defaultFontMetrics = l.getFontMetrics(defaultFont);
		
		
		// Component appearance
		
		decorator = new DefaultGraphDecorator<N, E, S, G>();
		
		setPreferredSize(new Dimension(640, 480));
		
		
		// Listeners

		handler = new EventHandler();
		
		addMouseListener(handler);
		addMouseMotionListener(handler);
		addMouseWheelListener(handler);
		
		listeners = new LinkedList<GraphDisplayListener<N,E,S,G>>();
		
		
		// Filters
		
		nodeFilters = new FilterSet<N>(true);
		nodeFilters.addFilterListener(handler);
		
		nodeHighlightFilters = new FilterSet<N>(false);
		nodeHighlightFilters.addFilterListener(handler);
		
		nodeJointHighlightFilter = new FilterSet<N>(false);
		nodeJointHighlightFilter.add(nodeFilters);
		nodeJointHighlightFilter.add(nodeHighlightFilters);
		
		
		// Node selection
		
		selectedNodes = new HashSet<BaseNode>();
		selectedLayoutNodes = new HashSet<GraphLayoutNode>();
		
		
		// The rest of the initialization
		
		computeGraphCache();
	}
	
	
	/**
	 * Set the graph
	 * 
	 * @param graph the new graph
	 * @param layout the graph layout
	 */
	public void setGraph(G graph, GraphLayout layout) {
		
		this.graph = graph;
		this.layout = layout;
		baseScale = 1;
		
		selectedNodes.clear();
		expandedSummaryNodes.clear();
		
		if (graph == null || layout == null) {
			nodes.clear();
			edges.clear();
			repaint();
			return;
		}
		
		
		// Reconfigure the component based on the layout
		
		if (reconfigureBasedOnLayout) {
			
			// Semantic zoom
			
			if (layout.isOptimizedForZoom()) {
				semanticZoom = true;
				drawExpandedNodes = true;
			}
			else {
				semanticZoom = false;
				drawExpandedNodes = false;
			}
		}
		
		
		// Expand the appropriate parts of the graph
		
		expandedSummaryNodes.add(graph.getRootBaseSummaryNode());
		if (!semanticZoom) graph.getRootBaseSummaryNode().collectBaseSummaryNodes(expandedSummaryNodes);
		
		
		// Finish
		
		computeGraphCache();
		centerGraph();
		repaint();
	}


	/**
	 * Return the graph
	 *
	 * @return the graph
	 */
	public G getGraph() {
		return graph;
	}
	

	/**
	 * Return the graph layout
	 *
	 * @return the graph layout
	 */
	public GraphLayout getGraphLayout() {
		return layout;
	}
	
	
	/**
	 * Set the graph decorator
	 * 
	 * @param decorator the new decorator
	 */
	public void setDecorator(GraphDecorator<N, E, S, G> decorator) {
		this.decorator = decorator == null ? new DefaultGraphDecorator<N, E, S, G>() : decorator;
		computeGraphCache();
	}
	
	
	/**
	 * Return the graph decorator
	 * 
	 * @return the decorator
	 */
	public GraphDecorator<N, E, S, G> getDecorator() {
		return decorator;
	}


	/**
	 * Add a listener
	 *
	 * @param listener the listener to add
	 */
	public void addGraphDisplayListener(GraphDisplayListener<N, E, S, G> listener) {
		if (!listeners.contains(listener)) listeners.add(listener);
	}


	/**
	 * Remove a listener
	 *
	 * @param listener the listener to remove
	 */
	public void removeGraphDisplayListener(GraphDisplayListener<N, E, S, G> listener) {
		listeners.remove(listener);
	}


	/**
	 * Fire all callbacks when the user clicked a node
	 * 
	 * @param node the clicked node
	 */
	protected void fireNodeClicked(BaseNode node) {
		
		if (node == null) {
			for (GraphDisplayListener<N, E, S, G> l : listeners) l.backgroundClicked();
		}
		else if (node instanceof Node<?, ?>) { 
			for (GraphDisplayListener<N, E, S, G> l : listeners) l.nodeClicked(Utils.<N>cast(node));
		}
		else if (node instanceof SummaryNode<?, ?, ?, ?>) { 
			for (GraphDisplayListener<N, E, S, G> l : listeners) l.summaryNodeClicked(Utils.<S>cast(node));
		}
		else {
			throw new IllegalArgumentException();
		}
	}


	/**
	 * Fire all callbacks when the selection changed
	 */
	protected void fireSelectionChanged() {
		for (GraphDisplayListener<N, E, S, G> l : listeners) l.selectionChanged();
	}


	/**
	 * Return the drawing scale
	 *
	 * @return the scale
	 */
	public double getScale() {
		return baseScale * Math.pow(zoomToScale, zoom);
	}


	/**
	 * Return the zoom level
	 *
	 * @return the zoom level
	 */
	public int getZoom() {
		return zoom;
	}


	/**
	 * Set the zoom level
	 *
	 * @param zoom the new zoom level
	 */
	public void setZoom(int zoom) {
		this.zoom = zoom;
	}


	/**
	 * Get the X offset
	 *
	 * @return the X offset
	 */
	public double getOffsetX() {
		return offsetX;
	}


	/**
	 * Get the Y offset
	 *
	 * @return the Y offset
	 */
	public double getOffsetY() {
		return offsetY;
	}


	/**
	 * Set the offset
	 *
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 */
	public void setOffset(double x, double y) {
		offsetX = x;
		offsetY = y;
	}


	/**
	 * Add a node filter
	 *
	 * @param filter the filter to add
	 */
	public void addFilter(Filter<N> filter) {
		nodeFilters.add(filter);
	}


	/**
	 * Remove a node filter
	 *
	 * @param filter the filter to remove
	 */
	public void removeFilter(Filter<N> filter) {
		nodeFilters.remove(filter);
	}


	/**
	 * Remove all node filters
	 */
	public void clearFilters() {
		nodeFilters.clear();
	}
	
	
	/**
	 * Get the node filters
	 * 
	 * @return the filters
	 */
	public FilterSet<N> getFilters() {
		return nodeFilters;
	}


	/**
	 * Add a node highlight filter
	 *
	 * @param filter the filter to add
	 */
	public void addHighlightFilter(Filter<N> filter) {
		nodeHighlightFilters.add(filter);
	}


	/**
	 * Remove a node highlight filter
	 *
	 * @param filter the filter to remove
	 */
	public void removeHighlightFilter(Filter<N> filter) {
		nodeHighlightFilters.remove(filter);
	}


	/**
	 * Remove all node highlight filters
	 */
	public void clearHighlightFilters() {
		nodeHighlightFilters.clear();
	}
	
	
	/**
	 * Get the node highlight filters
	 * 
	 * @return the filters
	 */
	public FilterSet<N> getHighlightFilters() {
		return nodeHighlightFilters;
	}
	
	
	/**
	 * Set whether to run filters on the original nodes
	 * 
	 * @param onOriginals true to run filters on originals
	 */
	public void setFilteringOnOriginals(boolean onOriginals) {
		filterOnOriginals = onOriginals;
	}
	
	
	/**
	 * Determine whether the component should run filters on the original nodes
	 * 
	 * @return true to run filters on originals
	 */
	public boolean isFilteringOnOriginals() {
		return filterOnOriginals;
	}
	
	
	/**
	 * Determine whether to draw splines
	 * 
	 * @param splines true to draw splines, false to draw straight lines
	 */
	public void setDrawingSplines(boolean splines) {
		this.drawSplines = splines;
	}
	
	
	/**
	 * Determine whether to the component is configured to draw splines
	 * 
	 * @return true for splines, false for straight lines
	 */
	public boolean isDrawingSplines() {
		return this.drawSplines;
	}
	
	
	/**
	 * Determine whether to draw arrows
	 * 
	 * @param arrows true to draw arrows
	 */
	public void setDrawingArrows(boolean arrows) {
		this.drawArrows = arrows;
	}
	
	
	/**
	 * Determine whether to the component is configured to draw arrows
	 * 
	 * @return true for arrows
	 */
	public boolean isDrawingArrows() {
		return this.drawArrows;
	}
	
	
	/**
	 * Set whether to draw nodes as points
	 * 
	 * @param asPoints true to draw nodes as points, false as shapes
	 */
	public void setDrawingNodesAsPoints(boolean asPoints) {
		this.drawNodesAsPoints = asPoints;
	}
	
	
	/**
	 * Determine whether to the component is configured to draw nodes as points
	 * 
	 * @return true for nodes as points, false for nodes as shapes
	 */
	public boolean isDrawingNodesAsPoints() {
		return this.drawNodesAsPoints;
	}
	
	
	/**
	 * Add a node to selection
	 * 
	 * @param node the node to add
	 */
	public void addNodeToSelection(BaseNode node) {
		
		if (node.getGraph() != graph) {
			throw new IllegalArgumentException("Node " + node + " is not in the graph");
		}
		
		selectedNodes.add(node);
		computeSelectionCache();
		fireSelectionChanged();
	}
	
	
	/**
	 * Remove a node from selection
	 * 
	 * @param node the node to remove
	 */
	public void removeNodeFromSelection(BaseNode node) {
		
		if (node.getGraph() != graph) {
			throw new IllegalArgumentException("Node " + node + " is not in the graph");
		}
		
		selectedNodes.remove(node);
		computeSelectionCache();
		fireSelectionChanged();
	}
	
	
	/**
	 * Select a single node
	 * 
	 * @param node the node to select
	 */
	public void selectNode(BaseNode node) {
		
		if (node.getGraph() != graph) {
			throw new IllegalArgumentException("Node " + node + " is not in the graph");
		}
		
		selectedNodes.clear();
		selectedNodes.add(node);
		computeSelectionCache();
		fireSelectionChanged();
	}
	
	
	/**
	 * Select a collection of nodes
	 * 
	 * @param nodes the nodes to select
	 */
	public void selectNodes(Collection<BaseNode> nodes) {

		selectedNodes.clear();
		
		for (BaseNode node : nodes) {
			if (node.getGraph() != graph) {
				throw new IllegalArgumentException("Node " + node + " is not in the graph");
			}
			selectedNodes.add(node);
		}
		
		computeSelectionCache();
		fireSelectionChanged();
	}
	
	
	/**
	 * Clear selection
	 */
	public void clearSelection() {
		selectedNodes.clear();
		computeSelectionCache();
		fireSelectionChanged();
	}
	
	
	/**
	 * Get the set of selected nodes
	 * 
	 * @return the set of selected nodes - both nodes of type N and summary nodes
	 */
	public Set<BaseNode> getSelectedBaseNodes() {
		return selectedNodes;
	}
	
	
	/**
	 * Get a layout node for the given base graph node
	 * 
	 * @param index the base graph node index
	 * @return the layout node
	 */
	protected GraphLayoutNode getLayoutNode(int index) {
		
		GraphLayoutNode l = layout.getLayoutNode(index);
		if (l != null) return l;
		
		
		// Compute the layout (and time it, if necessary)
		
		long t_start = 0;
		if (DEBUG_PERFORMANCE) {
			t_start = System.currentTimeMillis();
		}
		
		BaseNode node = graph.getBaseNode(index);
		BaseSummaryNode s = node.getParent();
		if (DEBUG_PERFORMANCE && s.getInternalEdges().size() >= 4000) {
			System.err.println("Too many edges: " + s.getInternalEdges().size() + " in " + s.getLabel());
		}
		layout.getAlgorithm().updateLayout(layout, s, null);
		
		long t_end = 0;
		if (DEBUG_PERFORMANCE) {
			t_end = System.currentTimeMillis();
			long t = t_end - t_start;
			
			if (t > 100) {
				System.err.println("Layout for " + s + " was computed in " + t + " ms.");
			}
		}
		
		
		// Get the final layout
		
		l = layout.getLayoutNode(index);
		return l;
	}
	
	
	/**
	 * Return the node the given node maps to in the summarized graph
	 * 
	 * @param node the node of interest
	 * @return the summary node the given node maps to, or the node itself if it does not
	 */
	protected BaseNode map(BaseNode node) {
		BaseNode n = node;
		
		while (n != null && n != graph.getRootBaseSummaryNode()) {
			BaseSummaryNode p = n.getParent();
			if (expandedSummaryNodes.contains(p)) return n;
			n = p;
		}

		return n;
	}
	
	
	/**
	 * Add edges from the given node to the list to the downstream nodes that satisfy the filter
	 * 
	 * @param node the current node
	 * @param start the start node
	 * @param visited the set of visited nodes
	 * @param crossedEdges the collection of crossed edges
	 * @return the number of edges added to the result set
	 */
	private int createSummaryGraphHelper(BaseNode node, GraphLayoutNode start, Set<BaseNode> visited, Collection<BaseEdge> crossedEdges) {
		
		visited.add(node);
		
		int count  = 0;
		
		for (BaseEdge e : node.getOutgoingBaseEdges()) {
			BaseNode target = e.getBaseTo();

			
			// Check if the target node is visible in the filtered graph
			
			if (target.isVisible()
					&& nodeFilters.accept(!filterOnOriginals ? Utils.<N>cast(target)
							: Utils.<N>cast(target).getOriginal())) {
				visited.add(target);
				
				
				// Find the layout for the node 
				
				BaseNode t = map(target);
				GraphLayoutNode lt = getLayoutNode(t.getIndex());
				if (start == lt) continue;
				if (lt == null) throw new IllegalStateException("There is no layout for node " + t);
				
				
				// Add the layout for the edge
				
				if (start.getBaseNode() == node) {
					
					// Try to find an existing layout for the given edge
					
					GraphLayoutEdge l = layout.getLayoutEdge(e.getIndex());
					if (l == null) {
						BaseEdge x = graph.getBaseEdgeExt(start.getBaseNode(), t);
						if (x != null) {
							l = layout.getLayoutEdge(x.getIndex());
							
							// If it does not exist, generate a new one (which defaults to a straight
							// line between the two points)
							
							if (l == null) {
								l = new GraphLayoutEdge(x, start, lt);
								
								// TODO Should we add the new layout edge to the actual graph layout?
							}
						}
					}
										
					if (l == null) {
						// We should never get here
						throw new IllegalStateException();
					}
					
					
					// Add the edge
					
					if (l != null) edges.add(l);
				}
				else {
					
					if (crossedEdges == null) throw new IllegalStateException();
					
					BaseEdge x = graph.getBaseEdgeExt(node, t);
					if (x == null) throw new IllegalStateException();
					crossedEdges.add(x);
					
					BaseSummaryEdge se = graph.getOrCreateBaseSummaryEdge(start.getBaseNode(), t, crossedEdges);
					
					
					// Create a default layout for the edge
					
					edges.add(new GraphLayoutEdge(se, start, lt));
				}
				
				count++;
			}
			else {
				if (visited.contains(target)) continue;
				
				if (crossedEdges == null) crossedEdges = new ArrayList<BaseEdge>();
				BaseEdge x = graph.getBaseEdgeExt(node, e.getBaseTo());
				if (x == null) throw new IllegalStateException();
				crossedEdges.add(x);
				
				count += createSummaryGraphHelper(e.getBaseTo(), start, visited, crossedEdges);
			}
		}
		
		return count;
	}

	
	/**
	 * Compute the graph cache - which nodes and edges are visible
	 */
	protected void computeGraphCache() {
		
		
		// Clear the cache
		
		nodes.clear();
		edges.clear();
		summaryNodes.clear();
		
		if (graph == null || layout == null) return;
		
		
		// Initialize the timer
		
		long t_start = 0;
		if (DEBUG_PERFORMANCE) {
			t_start = System.currentTimeMillis();
		}
		
		
		// Initialize
		
		longestLabel = "";
		BaseSummaryNode root = graph.getRootBaseSummaryNode();
		
		
		// Get the list of visible nodes
		
		for (N node : graph.getNodes()) {

			if (!node.isVisible()) continue;
			if (filterOnOriginals) {
				if (!nodeFilters.accept(node.getOriginal())) continue;
			}
			else { 
				if (!nodeFilters.accept(node)) continue;
			}
			
			
			// Add the corresponding summary node
			
			BaseNode n = map(node);
			if (!n.isVisible()) continue;
			
			GraphLayoutNode ln = getLayoutNode(n.getIndex());
			if (ln == null) {
				if (n.getIncomingBaseEdges().isEmpty() && n.getOutgoingBaseEdges().isEmpty()) continue;
				throw new IllegalStateException("There is no layout information for node " + n);
			}
			nodes.add(ln);
			
			
			// Compute the longest node label
			
			String label = n.getLabel();
			if (label.length() > longestLabel.length()) longestLabel = label;
			
			
			// Add all parent summary nodes
			
			for (BaseSummaryNode s = n.getParent(); s != root && s != null; s = s.getParent()) {
				GraphLayoutNode ls = getLayoutNode(s.getIndex());
				if (ls != null) {
					if (!summaryNodes.add(ls)) break;
				}
			}
		}
		
		if (drawRootSummaryNode) {
			GraphLayoutNode ls = getLayoutNode(root.getIndex());
			if (ls != null) {
				summaryNodes.add(ls);
			}
		}
		
		
		// Get the list of edges
		
		HashSet<BaseNode> visited = new HashSet<BaseNode>();
		for (GraphLayoutNode n : nodes) {
			visited.clear();
			if (n.getBaseNode().isVisible()) createSummaryGraphHelper(n.getBaseNode(), n, visited, null);
		}
		
		
		// Highlight & selection
		
		computeHighlightCache();
		computeSelectionCache();
		
		
		// Finish
		
		long t_end = 0;
		if (DEBUG_PERFORMANCE) {
			t_end = System.currentTimeMillis();
			long t = t_end - t_start;
			
			if (t > 100) {
				System.err.println("Graph cache computed in " + t + " ms:");
				System.err.println("  " + nodes.size() + " nodes, " + summaryNodes.size() + " expanded nodes, "
										+ edges.size() + " edges");
				System.err.println();
			}
		}
	}

	
	/**
	 * Compute the cache of which nodes are highlighted
	 */
	protected void computeHighlightCache() {
		
		
		// Clear the cache
		
		highlightedNodes.clear();
		
		if (graph == null || layout == null) return;
		if (nodeHighlightFilters.isEmpty()) return;
		
		
		// Check if a node is supposed to be highlighted
		
		for (N node : graph.getNodes()) {

			if (!node.isVisible()) continue;
			if (filterOnOriginals) {
				if (!nodeFilters.accept(node.getOriginal())) continue;
				if (!nodeHighlightFilters.accept(node.getOriginal())) continue;
			}
			else { 
				if (!nodeFilters.accept(node)) continue;
				if (!nodeHighlightFilters.accept(node)) continue;
			}
			
			
			// Add the corresponding summary node
			
			BaseNode n = map(node);
			if (!n.isVisible()) continue;
			
			GraphLayoutNode ln = getLayoutNode(n.getIndex());
			if (ln == null) {
				if (n.getIncomingBaseEdges().isEmpty() && n.getOutgoingBaseEdges().isEmpty()) continue;
				throw new IllegalStateException("There is no layout information for node " + n);
			}
			
			highlightedNodes.add(ln);
		}
	}
	
	
	/**
	 * Compute the cache of which nodes are selected
	 */
	protected void computeSelectionCache() {
		
		selectedLayoutNodes.clear();
		for (BaseNode node : selectedNodes) {
			
			BaseNode n = map(node);
			if (n == null) continue;
			if (!n.isVisible()) continue;
			
			GraphLayoutNode ln = getLayoutNode(n.getIndex());
			if (ln == null) {
				if (n.getIncomingBaseEdges().isEmpty() && n.getOutgoingBaseEdges().isEmpty()) continue;
				throw new IllegalStateException("There is no layout information for node " + n);
			}
			
			selectedLayoutNodes.add(ln);
		}
	}
	
	
	/**
	 * Expand a summary node
	 * 
	 * @param node the summary node to expand
	 * @return true if the node was actually expanded
	 */
	public boolean expandNode(BaseNode node) {
		
		if (!(node instanceof BaseSummaryNode)) return false;
		if (!node.isVisible()) return false;
		
		BaseSummaryNode s = (BaseSummaryNode) node;
		if (expandedSummaryNodes.contains(s)) return false;
		
		GraphLayoutNode ls = getLayoutNode(s.getIndex());
		if (ls == null) return false;
		
		
		// Expand all parents
		
		for (BaseNode n = s.getParent(); n != null; n = n.getParent()) {
			if (n instanceof BaseSummaryNode) {
				if (!expandedSummaryNodes.contains(n)) {
					if (!expandNode(n)) return false;
				}
			}
		}
		
		if (expandedSummaryNodes.contains(s)) return false;
		
		
		// Get the child nodes
		
		HashSet<GraphLayoutNode> toExpand = new HashSet<GraphLayoutNode>();
		
		for (BaseNode n : s.getBaseChildren()) {
			
			if (!n.isVisible()) continue;
			
			
			// Check the filters
			
			if (n instanceof SummaryNode<?, ?, ?, ?>) {
				if (!Utils.<S>cast(n).checkFilter(nodeFilters)) continue;
			}
			
			if (n instanceof Node<?, ?>) {
				if (filterOnOriginals) {
					if (!nodeFilters.accept(Utils.<N>cast(n).getOriginal())) continue;
				}
				else { 
					if (!nodeFilters.accept(Utils.<N>cast(n))) continue;
				}
			}
			
			
			// Get the layout for a child node
			
			GraphLayoutNode ln = getLayoutNode(n.getIndex());
			if (ln == null) {
				if (n.getIncomingBaseEdges().isEmpty() && n.getOutgoingBaseEdges().isEmpty()) continue;
				throw new IllegalStateException("There is no layout information for node " + n);
			}
			
			toExpand.add(ln);
		}
		
		
		// Move the node from the set of nodes to the set of expanded summary nodes
		
		if (!nodes.remove(ls)) return false;
		
		if (!expandedSummaryNodes.add(s)) return false;
		summaryNodes.add(ls);
		
		highlightedNodes.remove(ls);
		
		
		// Add the expanded nodes
		
		for (GraphLayoutNode ln : toExpand) {
			BaseNode n = ln.getBaseNode();
			
			nodes.add(ln);
						
			
			// Check whether the node should be highlighted
			
			boolean highlight = false;
			
			if (n instanceof SummaryNode<?, ?, ?, ?>) {
				if (Utils.<S>cast(n).checkFilter(nodeJointHighlightFilter)) highlight = true;
			}
			
			if (n instanceof Node<?, ?>) {
				if (nodeJointHighlightFilter.accept(Utils.<N>cast(n))) highlight = true;
			}
			
			if (highlight) highlightedNodes.add(ln);
		}
		
		
		// Remove the edges from and to the summary node
		
		for (Iterator<GraphLayoutEdge> ile = edges.iterator(); ile.hasNext(); ) {
			GraphLayoutEdge le = ile.next();
			
			if (le.getTo() == ls) {
				ile.remove();
				toExpand.add(le.getFrom());
				continue;
			}
			
			if (le.getFrom() == ls) {
				ile.remove();
				continue;
			}
		}
		
		
		// Add edges from the newly added nodes
		
		HashSet<BaseNode> visited = new HashSet<BaseNode>();
		for (GraphLayoutNode n : toExpand) {
			visited.clear();
			createSummaryGraphHelper(n.getBaseNode(), n, visited, null);
		}
		
		
		// Expand all summary nodes with only one child 
		
		for (BaseNode n : s.getBaseChildren()) {
			if (n instanceof BaseSummaryNode) {
				if (((BaseSummaryNode) n).getBaseChildren().size() == 1) {
					expandNode(n);
				}
			}
		}
		
		
		// Recompute the node selection cache
		
		computeSelectionCache();
		
		return true;
	}
	
	
	/**
	 * Collapse a summary node
	 * 
	 * @param node the summary node to collapse
	 */
	public void collapseNode(BaseNode node) {
		
		if (!(node instanceof BaseSummaryNode)) return;
		if (!node.isVisible()) return;
		
		BaseSummaryNode s = (BaseSummaryNode) node;
		if (!expandedSummaryNodes.contains(s)) return;
		
		GraphLayoutNode ls = getLayoutNode(s.getIndex());
		if (ls == null) return;
		
		
		// Collapse all children
		
		for (BaseNode n : s.getBaseChildren()) if (n instanceof BaseSummaryNode) collapseNode(n);
		
		
		// Mark the node as collapsed
		
		if (!expandedSummaryNodes.remove(s)) return;
		
		
		// Remove the child nodes
		
		HashSet<GraphLayoutNode> removed = new HashSet<GraphLayoutNode>();
		
		for (BaseNode n : s.getBaseChildren()) {
			
			if (!n.isVisible()) continue;
			
			GraphLayoutNode ln = getLayoutNode(n.getIndex());
			if (ln == null) {
				if (n.getIncomingBaseEdges().isEmpty() && n.getOutgoingBaseEdges().isEmpty()) continue;
				throw new IllegalStateException("There is no layout information for node " + n);
			}
			
			nodes.remove(ln);
			removed.add(ln);
			
			highlightedNodes.remove(ln);
		}
		
		if (removed.isEmpty()) return;
		
		
		// Move the node from set of expanded summary nodes to the set of regular nodes
		
		if (!nodes.add(ls)) return;
		summaryNodes.remove(ls);
		
		
		// Check whether the node should be highlighted
		
		if (node instanceof SummaryNode<?, ?, ?, ?>) {
			if (Utils.<S>cast(node).checkFilter(nodeJointHighlightFilter)) highlightedNodes.add(ls);
		}
		
		
		// Remove the edges from and to the child nodes
		
		HashSet<GraphLayoutNode> toExpand = new HashSet<GraphLayoutNode>();
		toExpand.add(ls);
		
		for (Iterator<GraphLayoutEdge> ile = edges.iterator(); ile.hasNext(); ) {
			GraphLayoutEdge le = ile.next();
			
			if (removed.contains(le.getTo())) {
				ile.remove();
				if (!removed.contains(le.getFrom())) toExpand.add(le.getFrom());
				continue;
			}
			
			if (removed.contains(le.getFrom())) {
				ile.remove();
				continue;
			}
		}
		
		
		// Add edges from the newly added nodes
		
		HashSet<BaseNode> visited = new HashSet<BaseNode>();
		for (GraphLayoutNode n : toExpand) {
			visited.clear();
			createSummaryGraphHelper(n.getBaseNode(), n, visited, null);
		}
		
		
		// Recompute the node selection cache
		
		computeSelectionCache();
	}
	
	
	/**
	 * Scale a length
	 * 
	 * @param l the length to scale
	 * @param s the scale
	 * @return the scaled length
	 */
	protected double scale(double l, double s) {
		return l * s;
	}


	/**
	 * Translate an X coordinate
	 *
	 * @param x the original coordinate
	 * @param scale the scale level
	 * @param width the image width
	 * @return the translated version
	 */
	protected int translateX(double x, double scale, int width) {
		
		double dx = (x + offsetX) * scale + width / 2;
		
		if (dx < -CLIP_MARGIN) dx = -CLIP_MARGIN;
		if (dx > width + CLIP_MARGIN) dx = width + CLIP_MARGIN;
		
		return (int) Math.round(dx);
	}


	/**
	 * Translate a Y coordinate
	 *
	 * @param y the original coordinate
	 * @param scale the scale level
	 * @param height the image height
	 * @return the translated version
	 */
	protected int translateY(double y, double scale, int height) {
		
		double dy = (y + offsetY) * scale + height / 2;
		
		if (dy < -CLIP_MARGIN) dy = -CLIP_MARGIN;
		if (dy > height + CLIP_MARGIN) dy = height + CLIP_MARGIN;
		
		return (int) Math.round(dy);
	}


	/**
	 * Translate a line
	 *
	 * @param line the line (will be modified)
	 * @param scale the scale level
	 * @param width the image width
	 * @param height the image height
	 * @return true if the line is within the clipped region
	 */
	protected boolean translateLine(Line2D line, double scale, int width, int height) {
		
		Rectangle2D r = new Rectangle2D.Double(-CLIP_MARGIN, -CLIP_MARGIN,
							width + CLIP_MARGIN * 2, height + CLIP_MARGIN * 2);
		
		line.setLine((line.getX1() + offsetX) * scale + width  / 2,
				     (line.getY1() + offsetY) * scale + height / 2,
				     (line.getX2() + offsetX) * scale + width  / 2,
				     (line.getY2() + offsetY) * scale + height / 2);
		
		return GraphicUtils.clipLine(line, r);
	}
	
	
	/**
	 * Get the node bounds
	 * 
	 * @param n the layout node
	 * @param scale the scale level
	 * @param width the image width
	 * @param height the image height
	 * @param out the output rectangle to modify (would be modified only if the method returns true)
	 * @return true if any part of the node is visible
	 */
	protected boolean getNodeBounds(GraphLayoutNode n, double scale, int width, int height, Rectangle out) {
		
		boolean base = n.getBaseNode() instanceof BaseSummaryNode;
		
		int sx = translateX(n.getX(), scale, width);
		int sy = translateY(n.getY(), scale, height);
		int sx1 = translateX(n.getX() - n.getWidth () / 2, scale, width);
		int sy1 = translateY(n.getY() - n.getHeight() / 2, scale, height);
		int sx2 = translateX(n.getX() + n.getWidth () / 2, scale, width);
		int sy2 = translateY(n.getY() + n.getHeight() / 2, scale, height);
		int hw  = (sx2 - sx1) / 2;
		int hh  = (sy2 - sy1) / 2;
		
		if (!base) {
			if (hh > 24 * FONT_SCALE) hh = (int) (24 * FONT_SCALE);
			if (drawNodesAsPoints) {
				double hnw = scale * n.getWidth () / 2;
				sx1 = sx;
				sx2 = sx1 + (int) (hnw * 2);
				sx  = sx1 + (int)  hnw;
			}
		}
		
		if (sx2 < 0 || sy2 < 0 || sx1 > width || sy1 > height) return false;
		if (sx < -hw || sy < -hh || sx > width + hw || sy > height + hh) return false;
		
		
		// Adjust for the label
		
		if (!base) {
			String label = n.getBaseNode().getLabel();
			Rectangle2D r = GraphicUtils.getTextBounds(defaultFontMetrics, label);
			
			int ideal_hw = (int)(NODE_HALF_WIDTH_FACTOR * r.getWidth () / 2 + NODE_HALF_WIDTH_EXTRA);
			
			if (ideal_hw < hw) {
				hw = ideal_hw;
				if (sx < -hw || sx > width + hw) return false;
			}
		}
		
		
		// Write out the bounds
		
		int ihw = (int) Math.round(hw);
		int ihh = (int) Math.round(hh);
		
		if (base) {
			out.x = sx1;
			out.y = sy1;
			out.width = ihw * 2;
			out.height = ihh * 2;
		}
		else {
			if (drawNodesAsPoints) {
				out.x = sx1;
			}
			else {
				out.x = sx - ihw;
			}
			out.y = sy - ihh;
			out.width = ihw * 2;
			out.height = ihh * 2;
		}
		
		return true;
	}
	
	
	/**
	 * Get the node bounds
	 * 
	 * @param n the layout node
	 * @param out the output rectangle to modify (would be modified only if the method returns true)
	 * @return true if any part of the node is visible
	 */
	protected boolean getNodeBounds(GraphLayoutNode n, Rectangle out) {
		return getNodeBounds(n, getScale(), getWidth(), getHeight(), out);
	}
	
	
	/**
	 * Get the node bounds
	 * 
	 * @param n the layout node
	 * @return the node bounds, or null if the node is not visible
	 */
	protected Rectangle getNodeBounds(GraphLayoutNode n) {
		Rectangle r = new Rectangle();
		return getNodeBounds(n, r) ? r : null;
	}


	/**
	 * Paint the component
	 * 
	 * @param g the graphics object
	 */
	public void paint(Graphics g) {
		
		// Get the panel size
		
		int width = getWidth();
		int height = getHeight();
		
		
		// Render
		
		render(g, width, height);
		
		
		// Render the UI overlay
		
		g.setColor(Color.BLACK);
		g.setFont(g.getFont().deriveFont(ZOOM_BUTTON_SIZE * 0.75f));
		GraphicUtils.drawText(g, "+", ZOOM_BUTTON_SIZE / 2, ZOOM_BUTTON_SIZE / 2, GraphicUtils.TextJustify.CENTER);
		GraphicUtils.drawText(g, "-", ZOOM_BUTTON_SIZE + ZOOM_BUTTON_SIZE / 2, ZOOM_BUTTON_SIZE / 2, GraphicUtils.TextJustify.CENTER);
	}


	/**
	 * Render the graph onto a graphics context
	 * 
	 * @param g the graphics object
	 * @param width the image width
	 * @param height the image height
	 */
	public void render(Graphics g, int width, int height) {

		// Initialize
		
		long t_start = 0;
		long t_last = 0;
		if (DEBUG_PERFORMANCE) {
			t_start = System.currentTimeMillis();
			t_last = t_start;
		}
		
		double scale = getScale(); 
		Graphics2D g2 = (Graphics2D) g;
		
		Stroke selectionStroke = new BasicStroke(3);
		Stroke edgeSelectionStroke = new BasicStroke(2);
		Stroke stroke = g2.getStroke();
		
		
		// Font
		
		Font orgFont = g.getFont();
		g.setFont(defaultFont);
		
		
		// Clear the panel

		Color bg = decorator.getBackgroundColor();
		g.setColor(bg);
		
		Rectangle clip = new Rectangle(0, 0, width, height);
		g.setClip(clip);
		
		g.fillRect(0, 0, width, height);
		if (graph == null || layout == null) return;
		
		
		// Support for semantic zoom
		
		// NOTE This technically does not belong here, but then we need stuff like max_hw and max_hh,
		// for which we need the graphics context g.
		
		if (semanticZoom) {
			
			// Consider the expanded summary nodes for collapse
			
			boolean done = false;
			while (!done) {
				done = true;
				
				HashSet<GraphLayoutNode> summaryNodesCopy = Utils.cast(summaryNodes.clone());
				
				for (GraphLayoutNode n : summaryNodesCopy) {
					BaseNode node = n.getBaseNode();
					if (!(node instanceof BaseSummaryNode)) continue;
					if (!expandedSummaryNodes.contains((BaseSummaryNode) node)) continue;
					
					boolean b = false;
					
					int sx1 = translateX(n.getX() - n.getWidth () / 2, scale, width);
					int sy1 = translateY(n.getY() - n.getHeight() / 2, scale, height);
					int sx2 = translateX(n.getX() + n.getWidth () / 2, scale, width);
					int sy2 = translateY(n.getY() + n.getHeight() / 2, scale, height);
					int hw  = (sx2 - sx1) / 2;
					int hh  = (sy2 - sy1) / 2;
		
					if (sx2 < 0 || sy2 < 0 || sx1 > width || sy1 > height) {
						b = true;
					}
					
					if (hh < semanticZoomSizeThreshold || hw < semanticZoomSizeThreshold) {
						b = true;
					}
					
					if (b) {
						if (DEBUG_SEMANTIC_ZOOM) System.err.println("[Zoom " + zoom + "] Collapse: " + n.getBaseNode().getLabel());
						collapseNode(node);
						done = false;
					}
				}
			}
			
			
			// Consider the collapsed summary nodes for expansion
			
			done = false;
			while (!done) {
				done = true;
				
				HashSet<GraphLayoutNode> nodesCopy = Utils.cast(nodes.clone());
				
				for (GraphLayoutNode n : nodesCopy) {
					BaseNode node = n.getBaseNode();
					if (!(node instanceof BaseSummaryNode)) continue;
					if (expandedSummaryNodes.contains((BaseSummaryNode) node)) continue;
					
					int sx1 = translateX(n.getX() - n.getWidth () / 2, scale, width);
					int sy1 = translateY(n.getY() - n.getHeight() / 2, scale, height);
					int sx2 = translateX(n.getX() + n.getWidth () / 2, scale, width);
					int sy2 = translateY(n.getY() + n.getHeight() / 2, scale, height);
					int hw  = (sx2 - sx1) / 2;
					int hh  = (sy2 - sy1) / 2;
		
					if (sx2 < 0 || sy2 < 0 || sx1 > width || sy1 > height) {
						continue;
					}
					
					if (hh < semanticZoomSizeThreshold || hw < semanticZoomSizeThreshold) {
						continue;
					}
					
					if (DEBUG_SEMANTIC_ZOOM) System.err.println("[Zoom " + zoom + "] Expand  : " + n.getBaseNode().getLabel());
					if (expandNode(node)) {
						done = false;
					}
				}
			}
		}
		
		long t_semantic_zoom = 0;
		if (DEBUG_PERFORMANCE) {
			long t = System.currentTimeMillis();
			t_semantic_zoom = t - t_last;
			t_last = t;
		}
		
		
		// Useful common variables
		
		Rectangle r = new Rectangle();
		Line2D l = new Line2D.Double();
		
		Set<GraphLayoutNode> immutable_nodes = Collections.unmodifiableSet(Utils.<Set<GraphLayoutNode>>cast(nodes));
		Set<GraphLayoutEdge> immutable_edges = Collections.unmodifiableSet(Utils.<Set<GraphLayoutEdge>>cast(edges));
		Set<GraphLayoutNode> immutable_summaryNodes = Collections.unmodifiableSet(Utils.<Set<GraphLayoutNode>>cast(summaryNodes));
		
		
		// Check how many nodes are visible
		
		boolean drawNodesThisTime = drawNodes;
		
		if (drawNodes) {
			double vnThreshold = 10 * (width * height / 10000.0);	// Units per 100x100 px area
			int visibleNodes = 0;

			for (GraphLayoutNode n : immutable_nodes) {
				
				// Check the position
				
				int sx = translateX(n.getX(), scale, width);
				int sy = translateY(n.getY(), scale, height);
				
				if (sx < 0 || sy < 0 || sx > width || sy > height) continue;
				
				
				// Update the state
				
				visibleNodes++;
				if (visibleNodes > vnThreshold) {
					drawNodesThisTime = false;
					break;
				}
			}
			
			
			// Debug
			
			if (DEBUG_NODE_VISIBILITY && visibleNodes == 0) {
				
				double prev_scale = baseScale * Math.pow(zoomToScale, zoom - 2);
				
				System.err.println();
				System.err.println("======= No nodes are visible =======");
				System.err.println();
				System.err.println("Width : " + width);
				System.err.println("Height: " + height);
				System.err.println("Zoom  : " + zoom);
				System.err.println("Scale : " + scale);
				System.err.println();
				
				for (GraphLayoutNode n : immutable_nodes) {
					
					int sx = translateX(n.getX(), scale, width);
					int sy = translateY(n.getY(), scale, height);
					
					int px = translateX(n.getX(), prev_scale, width);
					int py = translateY(n.getY(), prev_scale, height);
					
					boolean sv = !(sx < 0 || sy < 0 || sx > width || sy > height);
					boolean pv = !(px < 0 || py < 0 || px > width || py > height);
					
					String ss = sv ? "V" : "-";
					String ps = pv ? "P" : "-";
					
					System.err.println(ss + ps + " " + n.getBaseNode().getLabel() + "\t" + sx + "\t" + sy + "\t\t" + px + "\t" + py);
				}
				
				System.err.println();
			}
		}
		
		
		// Draw fills of expanded summary nodes
		
		if (drawExpandedNodes) {
			for (GraphLayoutNode n : immutable_summaryNodes) {
				
				if (!getNodeBounds(n, scale, width, height, r)) continue;
				
				S node = Utils.<S>cast(n.getBaseNode());
				boolean highlighted = highlightedNodes.contains(n);
				boolean selected = selectedLayoutNodes.contains(n);
				
				int selectionLevel = GraphDecorator.NONE;
				if (highlighted) selectionLevel = GraphDecorator.HIGHLIGHTED;
				if (selected) selectionLevel = GraphDecorator.SELECTED;
				
				Color c = decorator.getSummaryNodeColor(node, true, selectionLevel);
				if (c != null) {
					g.setColor(c);
					g.fillRect(r.x, r.y, r.width, r.height);
					
					if (DEBUG_RENDER_NODE) {
						System.err.println("Draw expanded summary "
								+ "node [" + n.getBaseNode().getIndex() + "] " + n.getBaseNode().getLabel()
								+ ": center=" + r.getCenterX() + ":" + r.getCenterY()
								+ ", size=" + r.getWidth() + ":" + r.getHeight());
					}
				}
			}
		}
		
		long t_draw_expanded_nodes_1 = 0;
		if (DEBUG_PERFORMANCE) {
			long t = System.currentTimeMillis();
			t_draw_expanded_nodes_1 = t - t_last;
			t_last = t;
		}
		

		// Draw edges
		
		BSpline spline = new BSpline();
		boolean edgeClipTo = drawNodesThisTime && !drawNodesAsPoints;

		for (int pass = 0; pass < 3; pass++) {
			for (GraphLayoutEdge e : immutable_edges) {
				E edge = e.getBaseEdge() instanceof Edge<?> ? Utils.<E>cast(e.getBaseEdge()) : null;
				
				
				// Get the end-points coordinates
				
				int x1 = translateX(e.getFrom().getX(), scale, width);
				int y1 = translateY(e.getFrom().getY(), scale, height);
				int x2 = translateX(e.getTo()  .getX(), scale, width);
				int y2 = translateY(e.getTo()  .getY(), scale, height);
				
				
				// Determine which pass the edges should be drawn in and ensure it
				
				boolean inside1 = x1 >= 0 && y1 >= 1 && x1 < width && y1 < height;
				boolean inside2 = x2 >= 0 && y2 >= 1 && x2 < width && y2 < height;
				boolean none = !inside1 && !inside2;
				boolean both =  inside1 &&  inside2;
				boolean one  = (inside1 ||  inside2) && !both;
				
				if (pass == 0) if (!none) continue;
				if (pass == 1) if (!one ) continue;
				if (pass == 2) if (!both) continue;
				
				
				// Get information about the from and to end-points of the edge
				
				int fromSelectionLevel = GraphDecorator.NONE;
				int toSelectionLevel = GraphDecorator.NONE;
				
				// TODO Consider also highlighted edges
				
				for (BaseNode n : selectedNodes) {
					if (getLayoutNode(map(n).getIndex()) == e.getFrom()) {
						fromSelectionLevel = GraphDecorator.SELECTED;
					}
					if (getLayoutNode(map(n).getIndex()) == e.getTo()) {
						toSelectionLevel = GraphDecorator.SELECTED;
					}
				}

				
				// Color
				
				Color c;
				if (e.getBaseEdge() instanceof BaseSummaryEdge) {
					c = decorator.getSummaryEdgeColor((BaseSummaryEdge) e.getBaseEdge(), fromSelectionLevel, toSelectionLevel);
				}
				else {
					c = decorator.getEdgeColor(edge, fromSelectionLevel, toSelectionLevel);
				}
				if (drawShadedEdgesOutsideOfDisplay && !both) {
					double f = one ? 0.5f : 0.125f;
					if (fromSelectionLevel == GraphDecorator.SELECTED
							|| toSelectionLevel == GraphDecorator.SELECTED) {
						f = one ? 0.75f : 0.5f;
					}
					int cr = (int)(bg.getRed  () + f * (c.getRed  () - bg.getRed  ()));
					int cg = (int)(bg.getGreen() + f * (c.getGreen() - bg.getGreen()));
					int cb = (int)(bg.getBlue () + f * (c.getBlue () - bg.getBlue ()));
					c = new Color(cr, cg, cb);
				}
				g.setColor(c);
				
				
				// Stroke
				
				if (drawThickEdgesFromToSelectedNodes) {
					if (fromSelectionLevel >= GraphDecorator.SELECTED || toSelectionLevel >= GraphDecorator.SELECTED) {
						g2.setStroke(edgeSelectionStroke);
					}
					else {
						g2.setStroke(stroke);
					}
				}
					
					
				// Draw the edge
				
				if (drawSplines && e.sizeCP() > 2) {
			
					double[] x = e.getX();
					double[] y = e.getY();
					
					spline.clearCP();

					
					// Get the first point
					
					int xl, yl, xt, yt, xa, ya;
					xa = xt = xl = translateX(x[0], scale, width);
					ya = yt = yl = translateY(y[0], scale, height);
					spline.addCP(xl, yl);

					
					// Prepare for clipping by the target node
					
					boolean clipToNow = false;
					Ellipse2D s = null;
					
					if (edgeClipTo) {
						clipToNow = getNodeBounds(e.getTo(), scale, width, height, r);
						if (r.getHeight() <= 3) clipToNow = false;
						if (clipToNow && !(e.getTo().getBaseNode() instanceof BaseSummaryNode)) {
							s = new Ellipse2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
						}
					}
					
					
					// Add the control point of each segment
					
					for (int i = 1; i < x.length; i++) {
						
						xa = xl;
						ya = yl;
						
						xt = translateX(x[i], scale, width);
						yt = translateY(y[i], scale, height);
						
						
						// Check clipping
						
						if (clipToNow) {
							
							l.setLine(xl, yl, xt, yt);
							
							boolean b;
							if (s == null) {
								b = GraphicUtils.clipLine(l, r);
							}
							else {
								b = GraphicUtils.clipLine(l, s);
							}
							
							if (b) {
								xt = (int) l.getX1();
								yt = (int) l.getY1();
								spline.addCP(xt, yt);
								break;
							}
						}
						
						
						// Add the control point
						
						spline.addCP(xt, yt);
						
						xl = xt;
						yl = yt;
					}
					
					
					// Render the spline
					
					if (spline.sizeCP() <= 2) {
						int xs = translateX(x[0], scale, width);
						int ys = translateY(y[0], scale, height);
						g.drawLine(xs, ys, xt, yt);
					}
					else {
						int d = spline.getDegree();
						if (spline.sizeCP() <= d) {
							spline.setDegree(spline.sizeCP() - 1);
						}
						spline.render(g);
						spline.setDegree(d);
					}
					
					
					// Arrow
					
					if (drawArrows) {
						
						int xs = translateX(x[0], scale, width);
						int ys = translateY(y[0], scale, height);
						int les = (xs-xt) * (xs-xt) + (ys-yt) * (ys-yt);
						
						if (les > arrowMinLength*arrowMinLength) {
							GraphicUtils.drawArrowHead(g, xa, ya, xt, yt, arrowHeadSize, true);							
						}
					}
				}
				else {
					
					// Get the line
					
					l.setLine(e.getFrom().getX(), e.getFrom().getY(), e.getTo().getX(), e.getTo().getY());
					translateLine(l, scale, width, height);
					
					
					// Clip by the destination node
					
					if (edgeClipTo) {
						if (getNodeBounds(e.getTo(), scale, width, height, r)) {
							if (r.getHeight() > 3) {
								
								Line2D l2 = Utils.cast(l.clone());
								
								if (e.getTo().getBaseNode() instanceof BaseSummaryNode) {
									if (GraphicUtils.clipLine(l2, r)) {
										l.setLine(l.getX1(), l.getY1(), l2.getX1(), l2.getY1());
									}
								}
								else {
									Ellipse2D s = new Ellipse2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
									if (GraphicUtils.clipLine(l2, s)) {
										l.setLine(l.getX1(), l.getY1(), l2.getX1(), l2.getY1());
									}
								}
							}
						}
					}
					
					
					// Draw, possibly with arrow
					
					double dx = e.getTo().getX() - e.getFrom().getX();
					double dy = e.getTo().getY() - e.getFrom().getY();
					
					if (drawArrows && (dx*dx + dy*dy) > arrowMinLength*arrowMinLength) {
						GraphicUtils.drawArrow(g, (int) l.getX1(), (int) l.getY1(), (int) l.getX2(), (int) l.getY2(), arrowHeadSize, true);
					}
					else {
						g.drawLine((int) l.getX1(), (int) l.getY1(), (int) l.getX2(), (int) l.getY2());
					}
				}
			}
		}
		
		long t_draw_edges = 0;
		if (DEBUG_PERFORMANCE) {
			long t = System.currentTimeMillis();
			t_draw_edges = t - t_last;
			t_last = t;
		}
		
		
		// Draw outlines of expanded summary nodes
		
		g2.setStroke(stroke);
		
		if (drawExpandedNodes) {
			for (GraphLayoutNode n : immutable_summaryNodes) {
				
				if (!getNodeBounds(n, scale, width, height, r)) continue;
				
				S node = Utils.<S>cast(n.getBaseNode());
				boolean highlighted = highlightedNodes.contains(n);
				boolean selected = selectedLayoutNodes.contains(n);
				
				int selectionLevel = GraphDecorator.NONE;
				if (highlighted) selectionLevel = GraphDecorator.HIGHLIGHTED;
				if (selected) selectionLevel = GraphDecorator.SELECTED;
				
				g.setColor(decorator.getSummaryNodeOutlineColor(node, true, selectionLevel));
				if (selected) g2.setStroke(selectionStroke);
				g.drawRect(r.x, r.y, r.width, r.height);
				g2.setStroke(stroke);
			}
		}
		
		long t_draw_expanded_nodes_2 = 0;
		if (DEBUG_PERFORMANCE) {
			long t = System.currentTimeMillis();
			t_draw_expanded_nodes_2 = t - t_last;
			t_last = t;
		}
		

		// Draw nodes
		
		g2.setStroke(stroke);

		if (drawNodesThisTime) {

			for (GraphLayoutNode n : immutable_nodes) {
				
				if (!getNodeBounds(n, scale, width, height, r)) continue;
				
				int sx = (int) r.getCenterX();
				int sy = (int) r.getCenterY();
				
				
				// Label

				String label = n.getBaseNode().getLabel();
				
				
				// Font
				
				Rectangle2D textBounds = GraphicUtils.getTextBounds(defaultFontMetrics, label);
				
				float fh = (float) (r.getHeight() / 2);
				fh = Math.min(fh, (float) (defaultFontHeight * r.getWidth() / textBounds.getWidth()));
				if (fh > defaultFontHeight) fh = defaultFontHeight;
				
				boolean drawLabel = drawNodeLabels && fh > 4.0f;
				if (drawLabel) g.setFont(orgFont.deriveFont(fh));
				
				
				// Highlight & selection
				
				boolean highlighted = highlightedNodes.contains(n);
				boolean selected = selectedLayoutNodes.contains(n);
				
				int selectionLevel = GraphDecorator.NONE;
				if (highlighted) selectionLevel = GraphDecorator.HIGHLIGHTED;
				if (selected) selectionLevel = GraphDecorator.SELECTED;
				
				
				// Debug
				
				if (DEBUG_RENDER_NODE) {
					System.err.println("Draw " + (n.getBaseNode() instanceof BaseSummaryNode ? "summary " : "")
							+ "node [" + n.getBaseNode().getIndex() + "] " + n.getBaseNode().getLabel()
							+ ": center=" + r.getCenterX() + ":" + r.getCenterY()
							+ ", size=" + r.getWidth() + ":" + r.getHeight());
				}
				
				
				// Draw the node
				
				if (n.getBaseNode() instanceof BaseSummaryNode) {
					
					// Draw a summary node
					
					S s = Utils.<S>cast(n.getBaseNode());
					
					g.setColor(decorator.getSummaryNodeColor(s, false, selectionLevel));
					g.fillRect(r.x, r.y, r.width, r.height);
					
					g.setColor(decorator.getSummaryNodeOutlineColor(s, false, selectionLevel));
					if (selected) g2.setStroke(selectionStroke);
					g.drawRect(r.x, r.y, r.width, r.height);
					g2.setStroke(stroke);
					
					if (drawLabel) {
						g.setColor(decorator.getSummaryNodeTextColor(s, false, selectionLevel));
						GraphicUtils.drawText(g, label, sx, sy, GraphicUtils.TextJustify.CENTER);
					}
				}
				else {
					
					// Draw a regular node
					
					N node = Utils.<N>cast(n.getBaseNode());
					
					
					// Node color
					
					Color colorFill = decorator.getNodeColor(node, selectionLevel);
					Color colorOutline = decorator.getNodeOutlineColor(node, selectionLevel);
					Color colorText = decorator.getNodeTextColor(node, selectionLevel);
					
					
					// Draw the node
					
					if (!drawNodesAsPoints) {
						
						// Draw the oval
						
						if (colorFill != null) {
							g.setColor(colorFill);
							g.fillOval(r.x, r.y, r.width, r.height);
						}
						
						g.setColor(colorOutline);
						if (selected) g2.setStroke(selectionStroke);
						g.drawOval(r.x, r.y, r.width, r.height);
						g2.setStroke(stroke);
						
						if (drawLabel) {
							g.setColor(colorText);
							GraphicUtils.drawText(g, label, sx, sy, GraphicUtils.TextJustify.CENTER);
						}
					}
					else {
						
						// Draw the point
		
						if (colorFill != null) {
							g.setColor(colorFill);
							g.fillOval(r.x - pointSize/2, sy - pointSize/2, pointSize, pointSize);
						}
						
						if (drawLabel) {
							g.setColor(decorator.getBackgroundColor());
							GraphicUtils.drawText(g, label, r.x + pointSize + 1, sy + 1, GraphicUtils.TextJustify.LEFT);
							g.setColor(colorText);
							GraphicUtils.drawText(g, label, r.x + pointSize, sy, GraphicUtils.TextJustify.LEFT);
						}
					}
				}
			}
		}
		
		long t_draw_nodes = 0;
		if (DEBUG_PERFORMANCE) {
			long t = System.currentTimeMillis();
			t_draw_nodes = t - t_last;
			t_last = t;
		}

		Font graphFont = g.getFont();
		
		
		// Finish

		g.setClip(null);
		g.setFont(graphFont);
		
		if (DEBUG_RENDER_NODE) System.err.println();
		
		long t_end = 0;
		if (DEBUG_PERFORMANCE) {
			t_end = System.currentTimeMillis();
			long t = t_end - t_start;
			
			if (t > 100) {
				System.err.println("Total rendering time   :   " + t + " ms");
				System.err.println("Semantic zoom          :   " + t_semantic_zoom + " ms");
				System.err.println("Expanded nodes, part 1 :   " + t_draw_expanded_nodes_1 + " ms");
				System.err.println("Edges                  :   " + t_draw_edges + " ms");
				System.err.println("Expanded nodes, part 2 :   " + t_draw_expanded_nodes_2 + " ms");
				System.err.println("Nodes                  :   " + t_draw_nodes + " ms");
				System.err.println("(" + immutable_nodes.size() + " nodes, " + immutable_summaryNodes.size() + " expanded nodes, "
									   + immutable_edges.size() + " edges)");
				System.err.println();
			}
		}
	}

	
	/**
	 * Center the graph
	 */
	public void centerGraph() {
		if (graph == null) return;
		
		GraphLayoutStat stat = layout.getStat();
		double xMin = stat.xMin - stat.widthMax;
		double xMax = stat.xMax + stat.widthMax;
		double yMin = stat.yMin - stat.heightMax;
		double yMax = stat.yMax + stat.heightMax;
		
		double sx = getWidth () / layout.getWidth();
		double sy = getHeight() / layout.getHeight();
		baseScale = 1.0 * (sx < sy ? sx : sy);
		
		setOffset(-xMin - (xMax - xMin) / 2, -yMin - (yMax - yMin) / 2);
	}
	
	
	/**
	 * Center the graph at the given node
	 * 
	 * @param node the node (either a node of type N or a summary node)
	 */
	public void centerAt(BaseNode node) {
		
		// TODO Set the appropriate zoom level for the node
		
		BaseNode n = map(node);
		GraphLayoutNode ln = layout.getLayoutNode(n.getIndex());
		
		if (ln == null) throw new IllegalArgumentException("No layout for node " + n);
		
		offsetX = -ln.getX();
		offsetY = -ln.getY();
		
		repaint();
	}
	
	
	/**
	 * Translate a point to a node
	 * 
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return the base node (either a node of type N or a summary node), or null
	 */
	public BaseNode getNodeByPosition(int x, int y) {
		
		if (!drawNodes) return null;
			
		Rectangle r = new Rectangle();
			
		int width = getWidth();
		int height = getHeight();
		double scale = getScale(); 
			
		for (GraphLayoutNode n : nodes) {
				
			if (!getNodeBounds(n, scale, width, height, r)) continue;
			if (!r.contains(x, y)) continue;
			
			BaseNode node = n.getBaseNode();

			if (node instanceof BaseSummaryNode) {
				return node;
			}
			else {
				Ellipse2D e = new Ellipse2D.Double(r.x, r.y, r.width, r.height);
				if (e.contains(x, y)) return node;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Export to Graphviz with formatting
	 * 
	 * @param file the file to write to
	 * @param all true to export all nodes, false to export only from the current zoom level
	 * @throws IOException on I/O error
	 */
	public void exportToGraphvizWithFormatting(File file, boolean all) throws IOException {
		PrintWriter out = new PrintWriter(file);
		exportToGraphvizWithFormatting(out, all);
		out.close();
	}
	
	
	/**
	 * Export to Graphviz with formatting
	 * 
	 * @param out the writer to write to
	 * @param all true to export all nodes, false to export only from the current zoom level
	 */
	public void exportToGraphvizWithFormatting(PrintWriter out, boolean all) {
		
		// Start
		
		out.println("digraph \"Graph\" {");
		if (layout.getAlgorithm() instanceof Graphviz) {
			out.println("  rankdir=" + ((Graphviz) layout.getAlgorithm()).getRankDir() + ";");
		}
		
		boolean nodesAsPoints = drawNodesAsPoints && false;	// Change to true for Graphviz 11/2011 or later
		
		
		// Gather all nodes
		
		Collection<BaseNode> nodes; 
		if (all) {
			nodes = this.graph.getBaseNodes();
		}
		else {
			nodes = new ArrayList<BaseNode>(this.nodes.size());
			for (GraphLayoutNode n : this.nodes) nodes.add(n.getBaseNode());
		}
		
		
		// Export all nodes
		
		final String ELLIPSE = "ellipse";
		
		for (BaseNode n : nodes) {
			if (!n.isVisible()) continue;
				
			String label = n.getLabel();
			if (label == null || " ".equals(label)) label = "";

			String shape;
			Color fillColor;
			Color textColor;
			Color outlineColor;
			
			if (n instanceof BaseSummaryNode) {
				shape = "box";
				fillColor = decorator.getSummaryNodeColor(Utils.<S>cast(n),
						expandedSummaryNodes.contains(n), GraphDecorator.NONE);
				textColor = decorator.getSummaryNodeTextColor(Utils.<S>cast(n),
						expandedSummaryNodes.contains(n), GraphDecorator.NONE);
				outlineColor = decorator.getSummaryNodeOutlineColor(Utils.<S>cast(n),
						expandedSummaryNodes.contains(n), GraphDecorator.NONE);
			}
			else {
				shape = nodesAsPoints ? "point" : ("".equals(label) ? "circle" : ELLIPSE);
				fillColor = decorator.getNodeColor(Utils.<N>cast(n), GraphDecorator.NONE);
				textColor = decorator.getNodeTextColor(Utils.<N>cast(n), GraphDecorator.NONE);
				outlineColor = decorator.getNodeOutlineColor(Utils.<N>cast(n), GraphDecorator.NONE);
				if (nodesAsPoints) {
					outlineColor = fillColor;
				}
			}
			
			String style;
			if (shape != ELLIPSE) {
				style = "shape=" + shape + ",";
			}
			else {
				style = "";
			}
			if (fillColor == null) {
				style += "style=solid";
			}
			else {
				style += "style=filled,fillcolor="
						+ String.format("\"#%02x%02x%02x\"", fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue());
			}
			if (textColor != Color.BLACK) {
				style += ",fontcolor="
						+ String.format("\"#%02x%02x%02x\"", textColor.getRed(), textColor.getGreen(), textColor.getBlue());
			}
			if (textColor != Color.BLACK) {
				style += ",color="
						+ String.format("\"#%02x%02x%02x\"", outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue());
			}
			
			String labelType = nodesAsPoints ? "xlabel" : "label";			
			out.println("  \"" + n.getIndex() + "\" [" + style + "," + labelType + "=\"" + Utils.escapeSimple(label) + "\"];");
		}
		
		
		// Gather all edges
		
		Collection<BaseEdge> edges; 
		if (all) {
			edges = this.graph.getBaseEdges();
		}
		else {
			edges = new ArrayList<BaseEdge>(this.edges.size());
			for (GraphLayoutEdge n : this.edges) {
				if (n.getBaseEdge() == null) {
					throw new RuntimeException("Base edge is null");
				}
				edges.add(n.getBaseEdge());
			}
		}
		
		
		// Export all edges
		
		for (BaseEdge e : edges) {
			if (!e.getBaseFrom().isVisible() || !e.getBaseTo().isVisible()) continue;
			
			Color color;
			if (e instanceof BaseSummaryEdge) {
				color = decorator.getSummaryEdgeColor((BaseSummaryEdge) e,
						GraphDecorator.NONE, GraphDecorator.NONE);
			}
			else {
				color = decorator.getEdgeColor(Utils.<E>cast(e),
						GraphDecorator.NONE, GraphDecorator.NONE);
			}
			
			String style;
			if (color == Color.BLACK && drawArrows) {
				style = "";
			}
			else {
				style = " [";
				if (!drawArrows) {
					style += "arrowhead=none";
				}
				if (color != Color.BLACK) {
					if (!drawArrows) style += ",";
					style += "color="
							+ String.format("\"#%02x%02x%02x\"", color.getRed(), color.getGreen(), color.getBlue());
				}
				style += "]";
			}
			
			out.println("  \"" + e.getBaseFrom().getIndex() + "\" -> \"" + e.getBaseTo().getIndex() + "\""
					+ style + ";");
		}
		
		
		// Finish
		
		out.println("}");
	}
	
	
	/**
	 * The event handler
	 * 
	 * @author Peter Macko
	 */
	private class EventHandler implements MouseListener, MouseMotionListener, MouseWheelListener, FilterListener<N> {
		
		private int prevX, prevY;
		private Timer zoomTimer;
		
		
		/**
		 * Create an object of type EventHandler
		 */
		public EventHandler() {
			prevX = prevY = 0;
			zoomTimer = null;
		}
		

		/**
		 * Handler for clicking a mouse button
		 * 
		 * @param e the mouse event object
		 */
		public void mouseClicked(MouseEvent e) {
			
			// Zoom
			
			if (e.getClickCount() == 2) {
				try { if (zoomTimer != null) { zoomTimer.cancel(); zoomTimer = null; } } catch (Exception ex) {}
				zoomTimer = new Timer();
				zoomTimer.scheduleAtFixedRate(new ZoomTimerTask(e.getX(), e.getY(), 1, 10), 0, 75);
				return;
			}
			
			
			// Find the node the user clicked
			
			BaseNode n = getNodeByPosition(e.getX(), e.getY());

			
			// Fire the events
			
			fireNodeClicked(n);
			
			
			// Manage selection
			
			if (autoManageSelection) {
				if (n == null) {
					clearSelection();
				}
				else {
					selectNode(n);
				}
				repaint();
			}
		}
		
		
		/**
		 * Handler for releasing a mouse button
		 * 
		 * @param e the mouse event object
		 */
		public void mouseReleased(MouseEvent e) {
			try { if (zoomTimer != null) { zoomTimer.cancel(); zoomTimer = null; } } catch (Exception ex) {}
		}
		
		
		/**
		 * Handler for pressing a mouse button
		 * 
		 * @param e the mouse event object
		 */
		public void mousePressed(MouseEvent e) {
			prevX = e.getX();
			prevY = e.getY();
			
			
			// Zoom buttons
			
			if (prevX < ZOOM_BUTTON_SIZE && prevY < ZOOM_BUTTON_SIZE) {
				try { if (zoomTimer != null) { zoomTimer.cancel(); zoomTimer = null; } } catch (Exception ex) {}
				zoomTimer = new Timer();
				zoomTimer.scheduleAtFixedRate(new ZoomTimerTask(getWidth() / 2, getHeight() / 2, 1), 0, 75); 
				return;
			}
			
			if (prevX < ZOOM_BUTTON_SIZE * 2 && prevY < ZOOM_BUTTON_SIZE) {
				try { if (zoomTimer != null) { zoomTimer.cancel(); zoomTimer = null; } } catch (Exception ex) {}
				zoomTimer = new Timer();
				zoomTimer.scheduleAtFixedRate(new ZoomTimerTask(getWidth() / 2, getHeight() / 2, -1), 0, 75); 
				return;
			}
		}
		
		
		/**
		 * Handler for mouse moving
		 * 
		 * @param e the mouse event object
		 */
		public void mouseMoved(MouseEvent e) {
		}
		
		
		/**
		 * Handler for mouse dragging
		 * 
		 * @param e the mouse event object
		 */
		public void mouseDragged(MouseEvent e) {
			if (graph == null) return;

			
			// Update the node position or scroll
			
			int pX = e.getX();
			int pY = e.getY();
			double scale = getScale();
			
			setOffset(getOffsetX() + (pX - prevX) / scale,
					  getOffsetY() + (pY - prevY) / scale);
			
			prevX = pX;
			prevY = pY;
			
			repaint();
		}
		

		/**
		 * Handler for mouse cursor entering the object
		 * 
		 * @param e the mouse event object
		 */
		public void mouseEntered(MouseEvent e) {
		}

		
		/**
		 * Handler for mouse cursor exiting the object
		 * 
		 * @param e the mouse event object
		 */
		public void mouseExited(MouseEvent e) {
		}
		
		
		/**
		 * Handler for mouse wheel events
		 *
		 * @param e the mouse event object
		 */
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (graph == null) return;
			
			int notches = e.getWheelRotation();
			if (notches == 0) return;
			
			
			// Get the panel size and the original scale
			
			int width = getWidth();
			int height = getHeight();
			double orgScale = getScale();
			
			
			// Update the zoom level and get the new scale

			zoom += notches;
			if (zoom < -4) zoom = -4;
			//if (zoom > 95) zoom = 95;
			double scale = getScale();
			
			
			// Reposition the graph
			
			double dx = (e.getX() - width  / 2) * ((1.0 / scale) - (1.0 / orgScale));
			double dy = (e.getY() - height / 2) * ((1.0 / scale) - (1.0 / orgScale));
			setOffset(getOffsetX() + dx, getOffsetY() + dy);
			
			
			// Repaint
			
			repaint();
		}


		/**
		 * Callback for when the filter changed
		 *
		 * @param filter the filter
		 */
		public void filterChanged(Filter<N> filter) {
			
			if (filter == nodeFilters) {
				computeGraphCache();
			}
			
			if (filter == nodeHighlightFilters) {
				computeHighlightCache();
			}
			
			repaint();
		}
		
		
		/**
		 * The zoom timer task
		 */
		private class ZoomTimerTask extends TimerTask {
			
			private int width, height;
			private int centerX, centerY;
			private int dNotches;
			private int runCount;
			private int maxRunCount;

			
			/**
			 * Create an instance of the task
			 */
			ZoomTimerTask(int centerX, int centerY, int dNotches, int maxRunCount) {
				
				this.centerX = centerX;
				this.centerY = centerY;
				this.dNotches = dNotches;
				
				width = getWidth();
				height = getHeight();
				
				this.runCount = 0;
				this.maxRunCount = maxRunCount;
			}

			
			/**
			 * Create an instance of the task
			 */
			ZoomTimerTask(int centerX, int centerY, int dNotches) {
				this(centerX, centerY, dNotches, Integer.MAX_VALUE);
			}
			
			
			/**
			 * Run the timer task
			 */
			public void run() {
				
				double orgScale = getScale();
				
				
				// Run count
				
				if (runCount >= maxRunCount) {
					try { cancel(); } catch (Exception ex) {}
					return;
				}
				runCount++;
				
				
				// Update the zoom level and get the new scale

				zoom += dNotches;
				if (zoom < -4) zoom = -4;
				//if (zoom > 95) zoom = 95;
				double scale = getScale();
				
				
				// Reposition the graph
				
				double dx = (centerX - width  / 2) * ((1.0 / scale) - (1.0 / orgScale));
				double dy = (centerY - height / 2) * ((1.0 / scale) - (1.0 / orgScale));
				setOffset(getOffsetX() + dx, getOffsetY() + dy);
				
				
				// Repaint
				
				repaint();
			}
		}
	}
}
