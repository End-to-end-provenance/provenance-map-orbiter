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

package edu.harvard.util.graph.layout;

import edu.harvard.util.graph.*;
import edu.harvard.util.job.*;
import edu.harvard.util.*;

import java.io.*;
import java.util.*;


/**
 * A two-dimensional graph layout
 * 
 * @author Peter Macko
 */
public abstract class GraphLayout implements Serializable {

	private static final long serialVersionUID = 7963405699924481830L;
	
	public static final String[] INPUT_FORMATS = { "plain" };
	public static final String[] OUTPUT_FORMATS = { };
	
	public static boolean preciseStatistics = false;
	public static boolean debugStatistics = false;
	
	private BaseGraph graph;
	private String description;
	private GraphLayoutAlgorithm algorithm;
	
	private GraphLayoutStat stat;
	private double margin;
	private boolean zoomOptimized;
	
	
	/**
	 * Create an instance of class GraphLayout
	 * 
	 * @param graph the graph
	 * @param algorithm the graph layout algorithm
	 * @param description the layout description
	 */
	public GraphLayout(BaseGraph graph, GraphLayoutAlgorithm algorithm, String description) {
		
		this.graph = graph;
		this.description = description;
		this.algorithm = algorithm;
		this.stat = null;
		
		this.margin = 0.05f * Graphviz.IMPORT_SCALE;
		this.zoomOptimized = false;
	}
	
	
	/**
	 * Create a stub for a graph layout from the given graph, algorithm, and description
	 * 
	 * @param graph the graph
	 * @param algorithm the graph layout algorithm
	 * @param description the layout description
	 * @return an instance of FastGraphLayout just with the layout for the collapsed root summary node
	 */
	public static GraphLayout createStub(BaseGraph graph, GraphLayoutAlgorithm algorithm, String description) {
		
		GraphLayout layout = new FastGraphLayout(graph, algorithm, description);
		
		BaseSummaryNode root = graph.getRootBaseSummaryNode();
		if (root == null) {
			throw new IllegalStateException("No root summary node (the graph was not summarized)");
		}
		
		GraphLayoutNode lr = new GraphLayoutNode(root, 0, 0);
		layout.centerAt(0, 0);
		lr.setSize(400, 300);
		layout.addLayoutNode(lr);
		
		return layout;
	}
	
	
	/**
	 * Return the graph the layout is computed for
	 * 
	 * @return the graph
	 */
	public BaseGraph getGraph() {
		return graph;
	}
	
	
	/**
	 * Return the string description
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description; 
	}
	
	
	/**
	 * Return whether the layout is optimized for semantic zoom
	 * 
	 * @return true if the layout is optimized for semantic zoom
	 */
	public boolean isOptimizedForZoom() {
		return zoomOptimized;
	}
	
	
	/**
	 * Set whether the layout is optimized for semantic zoom
	 * 
	 * @param v true if the layout is optimized for semantic zoom
	 */
	public void setOptimizedForZoom(boolean v) {
		zoomOptimized = v;
	}
	
	
	/**
	 * Return the object used to create the graph layout
	 * 
	 * @return the graph layout algorithm
	 */
	public GraphLayoutAlgorithm getAlgorithm() {
		return algorithm;
	}
	
	
	/**
	 * Return the graph layout statistics
	 * 
	 * @return the graph layout statistics
	 */
	public GraphLayoutStat getStat() {
		if (stat == null) updateStatistics();
		return stat;
	}
	
	
	/**
	 * Get the graph width
	 * 
	 * @return the width including a margin
	 */
	public double getWidth() {
		if (stat == null) updateStatistics();
		return stat.xMax - stat.xMin + 2 * margin + stat.widthMax;
	}
	
	
	/**
	 * Get the graph height
	 * 
	 * @return the height including a margin
	 */
	public double getHeight() {
		if (stat == null) updateStatistics();
		return stat.yMax - stat.yMin + 2 * margin + stat.heightMax;
	}


	/**
	 * Get the list of input formats
	 *
	 * @return an array of supported file extensions
	 */
	public static String[] getInputFormats() {
		return INPUT_FORMATS;
	}


	/**
	 * Get the list of output formats
	 *
	 * @return an array of supported file extensions
	 */
	public static String[] getOutputFormats() {
		return OUTPUT_FORMATS;
	}

	
	/**
	 * Return the graph nodes
	 *
	 * @return the collection of nodes
	 */
	public abstract Collection<GraphLayoutNode> getLayoutNodes();
	

	/**
	 * Return the graph edges
	 *
	 * @return the collection of edges
	 */
	public abstract Collection<GraphLayoutEdge> getLayoutEdges();
	
	
	/**
	 * Get a node by its index
	 * 
	 * @param index the node index
	 * @return the node
	 */
	public abstract GraphLayoutNode getLayoutNode(int index);
	
	
	/**
	 * Get an edge by its index
	 * 
	 * @param index the edge index
	 * @return the edge
	 */
	public abstract GraphLayoutEdge getLayoutEdge(int index);
	
	
	/**
	 * Add a layout node without checking for errors
	 * 
	 * @param node the layout node to add
	 */
	protected abstract void addLayoutNodeFast(GraphLayoutNode node);
	
	
	/**
	 * Add a layout edge without checking for errors
	 * 
	 * @param edge the layout edge to add
	 */
	protected abstract void addLayoutEdgeFast(GraphLayoutEdge edge);
	
	
	/**
	 * Add a layout node
	 * 
	 * @param node the layout node to add
	 * @throws IllegalArgumentException if the node does not belong to the associated graph 
	 */
	public void addLayoutNode(GraphLayoutNode node) {
		
		if (node.getIndex() < 0 || node.getIndex() > graph.getMaxNodeIndex() || node.getBaseNode().getGraph() != graph) {
			throw new IllegalArgumentException();
		}
		
		addLayoutNodeFast(node);
		
		
		// Fix statistics, if applicable
		
		if (stat != null) {
			
			double x = node.x;
			double y = node.y;
			double w = node.width;
			double h = node.height;
			
			if (x < stat.xMin) stat.xMin = x;
			if (x > stat.xMax) stat.xMax = x;
			if (y < stat.yMin) stat.yMin = y;
			if (y > stat.yMax) stat.yMax = y;
			
			if (w > stat.widthMax ) stat.widthMax  = w;
			if (h > stat.heightMax) stat.heightMax = h;
		}
	}
	
	
	/**
	 * Add a layout edge
	 * 
	 * @param edge the layout edge to add
	 * @throws IllegalArgumentException if the edge does not belong to the associated graph 
	 */
	public void addLayoutEdge(GraphLayoutEdge edge) {
		
		if (edge.getIndex() < 0 || edge.getIndex() > graph.getMaxEdgeIndex() || edge.getBaseEdge().getGraph() != graph) {
			throw new IllegalArgumentException();
		}
		
		addLayoutEdgeFast(edge);
	}
	
	
	/**
	 * Set the margin
	 * 
	 * @param margin the new margin
	 */
	public void setMargin(double margin) {
		this.margin = margin;
	}
	
	
	/**
	 * Get the margin
	 * 
	 * @return the margin
	 */
	public double getMargin() {
		return margin;
	}

	
	/**
	 * Create the object from file. Determine the file format from the file extension
	 *
	 * @param graph the abstract graph this layout is for
	 * @param file the file object
	 * @param algorithm the graph layout algorithm that produced the layout (can be null if necessary)
	 * @param observer the job progress observer
	 * @return the new object
	 * @throws IOException if an error occurred
	 */
	public static GraphLayout loadFromFile(BaseGraph graph, File file, GraphLayoutAlgorithm algorithm, JobObserver observer) throws IOException {

		String ext = Utils.getExtension(file);
		GraphLayout layout = new FastGraphLayout(graph, algorithm, ext);
		layout.updateFromFile(file, observer);
		return layout;
	}

	
	/**
	 * Update the layout from the information stored in the given file.
	 * Determine the file format from the file extension
	 *
	 * @param file the file object
	 * @param observer the job progress observer
	 */
	public void updateFromFile(File file, JobObserver observer) throws IOException {

		String ext = Utils.getExtension(file);

		FileInputStream stream = new FileInputStream(file);
		
		try {
			updateFromStream(stream, ext.toLowerCase(), observer);
		}
		finally {
			stream.close();
		}
	}

	
	/**
	 * Update the layout from the information loaded from the given stream
	 *
	 * @param stream the input stream
	 * @param format the input format (the file extension of the input file)
	 * @param observer the job progress observer
	 */
	public void updateFromStream(InputStream stream, String format, JobObserver observer) throws IOException {

		// Plain Graphviz output

		if (format.equals("plain")) {
			updateFromGraphviz(stream, observer);
			return;
		}


		// Unrecognized file extension

		throw new IOException("Unrecognized file format: " + format);
	}
	
	
	/**
	 * Load the Graphviz output
	 *
	 * @param stream the input stream
	 * @param observer the job progress observer
	 * @throws IOException if an error occurred
	 */
	private void updateFromGraphviz(InputStream stream, JobObserver observer) throws IOException {
		
		boolean loadSplines = true;
		
		if (observer != null) observer.makeIndeterminate();
		
		
		// Graph parameters
		
		double g_scale = -1;
		//double g_width = -1;
		//double g_height = -1;
		
		double xMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		double yMin = Double.MAX_VALUE;
		double yMax = Double.MIN_VALUE;
		double wMax = Double.MIN_VALUE;
		double hMax = Double.MIN_VALUE;


		// Load the graph
		
		BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
		
		String l;
		String part = "";
		int count = 0;
		
		while ((l = bin.readLine()) != null) {
			count++;
			l = part + l;
			part = "";

			if (((count % 2500) == 0) && (observer != null)) observer.setProgress(count);
			
			
			// Incomplete line
			
			if (l.endsWith("\\")) {
				part = l.substring(0, l.length() - 1);
				continue;
			}
			
			
			// Graph
			
			if (l.startsWith("graph")) {
				String[] a = l.split(" ");
				if (a.length != 4) {
					throw new IOException("Invalid \"graph\" statement: " + (a.length-1) + " arguments given, 3 expected");
				}
				g_scale  = Double.parseDouble(a[1]);
				//g_width  = Double.parseDouble(a[2]);
				//g_height = Double.parseDouble(a[3]);
				continue;
			}
			
			
			// Node
			
			if (l.startsWith("node")) {
				String[] a = l.split(" ");

				String p = Utils.removeQuotes(a[1]);
				int id = Integer.parseInt(p);
				if (id < 0) {
					throw new IOException("Invalid node ID on line " + count + ": " + id);
				}
				
				BaseNode n = graph.getBaseNode(id);
				if (n == null) {
					throw new IOException("Nonexistent node referenced on line " + count + ": " + id);
				}
				
				int d = a[2].length() == 0 ? 1 : 0;
				double x =  Double.parseDouble(a[2 + d]) * Graphviz.IMPORT_SCALE;
				double y = -Double.parseDouble(a[3 + d]) * Graphviz.IMPORT_SCALE;
				double w =  Double.parseDouble(a[4 + d]) * Graphviz.IMPORT_SCALE;
				double h =  Double.parseDouble(a[5 + d]) * Graphviz.IMPORT_SCALE;
				
				if (x < xMin) xMin = x;
				if (x > xMax) xMax = x;
				if (y < yMin) yMin = y;
				if (y > yMax) yMax = y;
				if (w > wMax) wMax = w;
				if (h > hMax) hMax = h;

				GraphLayoutNode ln = new GraphLayoutNode(n, x, y);
				ln.setSize(w, h);
				
				addLayoutNodeFast(ln);
				continue;
			}
			
			
			// DisplayableEdge
			
			if (l.startsWith("edge")) {

				int s1 = l.indexOf(" ");
				int s2 = l.indexOf(" ", s1 + 1);
				int s3 = l.indexOf(" ", s2 + 1);
				
				int f = Integer.parseInt(Utils.removeQuotes(l.substring(s1+1, s2)));
				int t = Integer.parseInt(Utils.removeQuotes(l.substring(s2+1, s3)));

				int num = 0;
				double[] x;
				double[] y;

				if (loadSplines) {
					String[] a = l.split(" ");
				
					num = Integer.parseInt(a[3]);
					x = new double[num + 2];
					y = new double[num + 2];

					int k = 4;

					for (int i = 1; i <= num; i++) {
						double dx =  Double.parseDouble(a[k++]);
						double dy = -Double.parseDouble(a[k++]);
						x[i] = dx * Graphviz.IMPORT_SCALE;
						y[i] = dy * Graphviz.IMPORT_SCALE;
					}
				}
				else {
					x = new double[2];
					y = new double[2];
				}
				
				GraphLayoutNode nf = getLayoutNode(f);
				if (nf == null) {
					throw new IOException("Edge references nonexistent node " + f + " on line " + count + ": " + f + " -> " + t);
				}
				
				GraphLayoutNode nt = getLayoutNode(t);
				if (nt == null) {
					throw new IOException("Edge references nonexistent node " + t + " on line " + count + ": " + f + " -> " + t);
				}
				
				x[0] = nf.getX();
				y[0] = nf.getY();
				x[num+1] = nt.getX();
				y[num+1] = nt.getY();
				
				BaseEdge e = graph.getBaseEdgeExt(nf.getBaseNode(), nt.getBaseNode());
				
				// Should we throw an exception here or fail gracefully?
				// if (e == null) throw new IOException("Edge references nonexistent edge on line " + count + ": " + f + " -> " + t);
				
				if (e != null) addLayoutEdgeFast(new GraphLayoutEdge(e, nf, nt, x, y));
				
				continue;
			}
			
			
			// Stop
			
			if (l.startsWith("stop")) {
				break;
			}
			
			
			throw new IOException("Invalid Graphviz plain output\nUnexpected line: " + l);
		}
		
		if (g_scale < 0) {
			throw new IOException("Missing \"graph\" statement");
		}
		
		if (Math.abs(g_scale - 1) > 0.0001) {
			System.err.println("Warning: scale = " + g_scale + " (expected: 1)");
		}


		// Compute basic node and edge statistics

		if (observer != null) observer.makeIndeterminate();
		
		
		// Create statistics
			
		if (stat == null) {
			stat = new GraphLayoutStat();
		}
		else {
			stat.clear();
		}
			
		stat.xMin = xMin;
		stat.xMax = xMax;
		stat.yMin = yMin;
		stat.yMax = yMax;
		
		stat.widthMax  = wMax;
		stat.heightMax = hMax;
			
		if (debugStatistics) checkStatistics();
	}
	
	
	/**
	 * Import layout information from another layout from the same graph
	 * 
	 * @param layout the layout to import (warning: it will be modified destructively)
	 * @param overwrite whether to overwrite local information in the case of conflict
	 * @throws IllegalArgumentException if the two layouts are not for the same graph
	 */
	public void importLayout(GraphLayout layout, boolean overwrite) {
		
		if (layout.graph != graph) {
			throw new IllegalArgumentException("The two layouts are not for the same graph");
		}
		
		
		// Import nodes
		
		for (GraphLayoutNode n : layout.getLayoutNodes()) {
			if (n != null && (overwrite || getLayoutNode(n.getIndex()) == null)) addLayoutNodeFast(n);
		}
		
		
		// Import & adjust edges
		
		for (GraphLayoutEdge e : layout.getLayoutEdges()) {
			if (e != null && (overwrite || getLayoutEdge(e.getBaseEdge().getIndex()) == null)) {
				if (!overwrite) {
					e.from = getLayoutNode(e.from.getIndex());
					e.to = getLayoutNode(e.to.getIndex());
				}
				addLayoutEdgeFast(e);
			}
		}
		
		if (overwrite) {
			for (GraphLayoutEdge e : getLayoutEdges()) {
				if (e != null) {
					e.from = getLayoutNode(e.from.getIndex());
					e.to = getLayoutNode(e.to.getIndex());
				}
			}
		}
		
		
		// Combine the statistics, if possible
		
		if (stat != null && !preciseStatistics) {
			
			if (layout.stat == null) {
				
				// In this case, we just have to invalidate the statistics
				
				stat = null;
			}
			else {
				
				// Warning: This is very imprecise - this works well only if there
				// is no overlap between the two merged layouts
				
				if (layout.stat.xMin < stat.xMin) stat.xMin = layout.stat.xMin; 
				if (layout.stat.xMax > stat.xMax) stat.xMax = layout.stat.xMax; 
				if (layout.stat.yMin < stat.yMin) stat.yMin = layout.stat.yMin; 
				if (layout.stat.yMax > stat.yMax) stat.yMax = layout.stat.yMax; 
				
				if (layout.stat.widthMax  > stat.widthMax ) stat.widthMax  = layout.stat.widthMax; 
				if (layout.stat.heightMax > stat.heightMax) stat.heightMax = layout.stat.heightMax; 
			}
			
			if (debugStatistics) checkStatistics();
		}
		else {
			
			// Combining is inherently imprecise, so just invalidate
			
			stat = null;
		}
	}
	
	
	/**
	 * Move the contents of the entire graph layout relatively to their current positions 
	 * 
	 * @param dx the shift in the X direction
	 * @param dy the shift in the Y direction
	 */
	public void moveRelatively(double dx, double dy) {
		
		for (GraphLayoutNode n : getLayoutNodes()) if (n != null) n.moveRelatively(dx, dy);
		for (GraphLayoutEdge e : getLayoutEdges()) if (e != null) e.moveRelatively(dx, dy);
		
		
		// Update statistics, if available
		
		if (stat != null) {
			stat.xMax += dx;
			stat.xMin += dx;
			stat.yMax += dy;
			stat.yMin += dy;
		}
		
		if (debugStatistics) checkStatistics();
	}
	
	
	/**
	 * Move the graph contents so that the center of the graph is at the given coordinates
	 * 
	 * @param x the new X-coordinate of the center of the graph
	 * @param y the new Y-coordinate of the center of the graph
	 */
	public void centerAt(double x, double y) {
		
		GraphLayoutStat s = getStat();
		
		double dx = x - (s.xMax + s.xMin) / 2;
		double dy = y - (s.yMax + s.yMin) / 2;
		
		moveRelatively(dx, dy);
	}
	
	
	/**
	 * Scale the graph with respect to the pivot
	 * 
	 * @param sx the scale on the X axis
	 * @param sy the scale on the Y axis
	 * @param px pivot's X-coordinate
	 * @param py pivot's Y-coordinate
	 */
	public void scaleWithPivot(double sx, double sy, double px, double py) {
		
		for (GraphLayoutNode n : getLayoutNodes()) if (n != null) n.scaleWithPivot(sx, sy, px, py);
		for (GraphLayoutEdge e : getLayoutEdges()) if (e != null) e.scaleWithPivot(sx, sy, px, py);
		
		margin *= Math.min(sx, sy);
		
		
		// Update statistics, if available
		
		if (stat != null) {
			
			stat.xMax = px + (stat.xMax - px) * sx;
			stat.xMin = px + (stat.xMin - px) * sx;
			stat.yMax = py + (stat.yMax - py) * sy;
			stat.yMin = py + (stat.yMin - py) * sy;
			
			stat.widthMax  *= sx;
			stat.heightMax *= sy;
		}
		
		if (debugStatistics) checkStatistics();
	}
	
	
	/**
	 * Scale the graph
	 * 
	 * @param sx the scale on the X axis
	 * @param sy the scale on the Y axis
	 */
	public void scale(double sx, double sy) {
		
		GraphLayoutStat s = getStat();
		
		double cx = (s.xMax + s.xMin) / 2;
		double cy = (s.yMax + s.yMin) / 2;
		
		scaleWithPivot(sx, sy, cx, cy);
	}
	
	
	/**
	 * Scale the graph
	 * 
	 * @param s the scale
	 */
	public void scale(double s) {
		scale(s, s);
	}
	
	
	/**
	 * Recompute the statistics
	 */
	protected void updateStatistics() {

		double f;
		
		if (stat == null) stat = new GraphLayoutStat();
		stat.clear();

		for (GraphLayoutNode n : getLayoutNodes()) {
			if (n == null) continue;

			f = n.x;
			if (f < stat.xMin) stat.xMin = f;
			if (f > stat.xMax) stat.xMax = f;

			f = n.y;
			if (f < stat.yMin) stat.yMin = f;
			if (f > stat.yMax) stat.yMax = f;

			f = n.width;
			if (f > stat.widthMax) stat.widthMax = f;

			f = n.height;
			if (f > stat.heightMax) stat.heightMax = f;
		}
		
		if (debugStatistics) checkStatistics();
	}
	
	
	/**
	 * Check the correctness of the statistics
	 * 
	 * @throws IllegalStateException on error
	 */
	protected void checkStatistics() {
		
		if (!debugStatistics) return;
		if (stat == null) return;

		double f;

		for (GraphLayoutNode n : getLayoutNodes()) {
			if (n == null) continue;

			f = n.x;
			if (f < stat.xMin || f > stat.xMax) {
				throw new IllegalStateException("X = " + f + " is out of bounds [" + stat.xMin + ", " + stat.xMax + "]");
			}

			f = n.y;
			if (f < stat.yMin || f > stat.yMax) {
				throw new IllegalStateException("Y = " + f + " is out of bounds [" + stat.yMin + ", " + stat.yMax + "]");
			}
		}
	}
}
