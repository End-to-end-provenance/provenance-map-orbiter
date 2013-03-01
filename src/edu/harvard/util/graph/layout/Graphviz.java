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

package edu.harvard.util.graph.layout;

import edu.harvard.util.*;
import edu.harvard.util.XMLUtils;
import edu.harvard.util.convert.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.gui.HasWizardPanelConfigGUI;
import edu.harvard.util.gui.WizardPanel;
import edu.harvard.util.job.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * An interface to Graphviz
 *
 * @author Peter Macko
 */
public class Graphviz implements ExternalFileConverter, Cancelable, GraphLayoutAlgorithm, Cloneable,
								 HasWizardPanelConfigGUI, XMLSerializable {

	public static final String[] ALGORITHMS = { "circo", "dot", "fdp", "neato", "sfdp", "twopi" };
	public static final String[] ALGORITHMS_DIRECTED = { "dot" };
	public static final String[] ALGORITHMS_CAN_USE_SUMMARIES = { "dot", "circo", "fdp" };
	public static final String[] INPUT_FORMATS = { "dot" };
	public static final String[] RANKDIRS = { "RL", "LR", "BT", "TB" };
	public static final String[] RANKDIRS_LONG = { "Right-Left", "Left-Right", "Bottom-Top", "Top-Bottom" };
	protected static String[] outputFormats;
	
	public static final double IMPORT_SCALE = 20000.0;
	public static final double DEFAULT_SUMMARY_NODE_WIDTH = 4;
	public static final double DEFAULT_SUMMARY_NODE_HEIGHT = 3;
	
	private static HashSet<String> verifiedAlgorithms = new HashSet<String>();
	
	private String algorithm;
	private boolean directed;
	private boolean bySummaryNodes;
	private boolean zoomBasedLayout;
	private String rankdir;
	
	private ExternalProcess currentExternalProcess;
	private boolean running;
	private boolean canceled;
	private Set<Graphviz> nested;
	
	List<RecursiveTask> taskQueue;
	private int maxWorkers;


	/**
	 * Create an instance of class Graphviz
	 */
	public Graphviz() throws Exception {
		this("dot");
	}


	/**
	 * Create an instance of class Graphviz
	 *
	 * @param algorithm the graph algorithm to use
	 */
	public Graphviz(String algorithm) throws Exception {
		
		this.bySummaryNodes = true;
		this.zoomBasedLayout = true;
		this.rankdir = "RL";
		
		
		// Initialize the process management
		
		this.currentExternalProcess = null;
		this.running = false;
		this.canceled = false;
		this.nested = new HashSet<Graphviz>();
		
		
		// Initialize the thread management
		
		this.taskQueue = Collections.synchronizedList(new LinkedList<RecursiveTask>());
		this.maxWorkers = Runtime.getRuntime().availableProcessors();
		
		
		// Set the algorithm
		
		setAlgorithm(algorithm);
	}
	
	
	/**
	 * Set the algorithm
	 * 
	 * @param algorithm the new algorithm
	 */
	public void setAlgorithm(String algorithm) throws Exception {

		this.algorithm = algorithm.toLowerCase();
		

		// Check whether the algorithm is supported

		if (!Utils.contains(ALGORITHMS, algorithm)) throw new Exception("Unsupported algorithm: " + algorithm);
		
		
		// Check whether the algorithm is directional
		
		directed = Utils.contains(ALGORITHMS_DIRECTED, algorithm);


		// Check whether the appropriate external tool is installed

		synchronized (verifiedAlgorithms) {
			if (!verifiedAlgorithms.contains(algorithm)) {
				if (!Utils.isProgramInstalled(algorithm)) {
					throw new Exception("The external tool for computing "
						+ algorithm + " is not (properly) installed");
				}
				else {
					verifiedAlgorithms.add(algorithm);
				}
			}
		}
		
	}
	
	
	/**
	 * Create a clone of the algorithm with the same settings
	 * 
	 * @return the clone
	 */
	@Override
	public Graphviz clone() {
		try {
			Graphviz g = new Graphviz(algorithm);
			g.bySummaryNodes = bySummaryNodes;
			g.zoomBasedLayout = zoomBasedLayout;
			g.rankdir = rankdir;
			return g;
		} catch (Exception e) {
			throw new RuntimeException("Cannot re-instantiate the algorithm", e);
		}
	}


	/**
	 * Get the name of the converter
	 *
	 * @return the name
	 */
	public String getName() {
		return "Graphviz " + algorithm.toUpperCase();
	}
	
	
	/**
	 * Get the name of the algorithm
	 * 
	 * @return the name of the algorithm
	 */
	public String getAlgorithm() {
		return algorithm;
	}
	
	
	/**
	 * Determine whether the algorithm accepts directed graphs 
	 * 
	 * @return true if it accepts directed graphs
	 */
	public boolean isDirected() {
		return directed;
	}
	
	
	/**
	 * Determine whether the layout would be computed recursively one summary node at a time
	 * 
	 * @return true if the layout would be computed with respect to the summaries
	 */
	public boolean isBySummaries() {
		return bySummaryNodes;
	}
	
	
	/**
	 * Configure whether the layout should be computed recursively one summary node at a time
	 * 
	 * @param v true if the layout should be computed with respect to the summaries
	 */
	public void setBySummaries(boolean v) {
		bySummaryNodes = v;
	}
	
	
	/**
	 * Determine whether the layout would be optimized for semantic zoom
	 * 
	 * @return true if the layout would be optimized for semantic zoom
	 */
	public boolean isOptimizedForZoom() {
		return zoomBasedLayout;
	}
	
	
	/**
	 * Configure whether the layout would be optimized for semantic zoom
	 * 
	 * @param v true if the layout would be optimized for semantic zoom
	 */
	public void setOptimizedForZoom(boolean v) {
		zoomBasedLayout = v;
	}
	
	
	/**
	 * Return the graph direction
	 * 
	 * @return the rankdir direction
	 */
	public String getRankDir() {
		return rankdir;
	}
	
	
	/**
	 * Set the graph direction
	 * 
	 * @param v the new graph direction (must be an element of RANKDIRS)
	 */
	public void setRankDir(String v) {
		for (String x : RANKDIRS) {
			if (x.equals(v)) {
				this.rankdir = v;
				return;
			}
		}
		throw new IllegalArgumentException("Not a valid RANKDIR");
	}

	
	/**
	 * Determine whether the algorithm produces zoom-based (or zoom-optimized) layouts
	 * 
	 * @return true if it produces zoom-optimized layouts
	 */
	public boolean isZoomOptimized() {
		return zoomBasedLayout && bySummaryNodes;
	}


	/**
	 * Get the list of input formats
	 *
	 * @return an array of supported file extensions
	 */
	public String[] getInputFormats() {
		return INPUT_FORMATS;
	}


	/**
	 * Get the list of output formats
	 *
	 * @return an array of supported file extensions
	 */
	public String[] getOutputFormats() {
	
		if (outputFormats != null) return outputFormats;


		// Initialize the array of output formats

		try {
			Utils.execAndWait("dot -Thello < /dev/null");
		}
		catch (IOException e) {
			String s = e.getMessage().trim();
			String t = "Use one of: ";
			if (s.indexOf(t) >= 0) outputFormats = s.substring(s.indexOf(t) + t.length()).trim().split(" ");
		}

		if (outputFormats == null || outputFormats.length == 0) {
			outputFormats = new String[1];
			outputFormats[0] = "plain";
		}

		return outputFormats;
	}
	
	
	/**
	 * Set the external process handle
	 * 
	 * @param r whether the conversion is in progress
	 * @param p the process handle (null to clear)
	 * @return the previous process handle
	 * @throws RuntimeException if the synchronization is violated
	 */
	private synchronized void setProcessHandle(boolean r, ExternalProcess p) {
		
		if (r && running) {
			throw new RuntimeException("Another conversion is currently in progress");
		}
		
		running = r;
		currentExternalProcess = p;
		canceled = false;
		
		nested.clear();
	}


	/**
	 * Convert a file. Determine the input and output formats using
	 * from the file extensions
	 *
	 * @param input the input file
	 * @param output the output file
	 * @return the external process
	 * @throws IOException on error
	 */
	public ExternalProcess createExternalProcess(File input, File output) throws IOException {
		String[] cmds = {algorithm, "-T" + Utils.getExtension(output), input.getPath(), "-o" + output.getPath()};
		return new ExternalProcess(cmds);
	}


	/**
	 * Convert a file. Determine the input and output formats using
	 * the file extensions. Only one conversion can be in process
	 * when invoked using this method or when using compute().
	 *
	 * @param input the input file
	 * @param output the output file
	 * @throws IOException on error
	 */
	public void convert(File input, File output) throws IOException {
		
		ExternalProcess p = createExternalProcess(input, output);
		setProcessHandle(true, p);
		
		try {
			p.run();
		}
		catch (IOException e) {
			setProcessHandle(false, null);
			throw e;
		}
		catch (Throwable t) {
			setProcessHandle(false, null);
			throw new RuntimeException(t);
		}
		
		setProcessHandle(false, null);
	}
	
	
	/**
	 * Cancel the task
	 */
	public synchronized void cancel() {
		
		if (!running) return;
		
		try {
			canceled = true;
			if (currentExternalProcess != null) currentExternalProcess.cancel();
		}
		catch (Throwable t) {
			// Silent failover
		}
		
		try {
			for (Graphviz g : nested) g.cancel();
		}
		catch (Throwable t) {
			// Silent failover
		}
		
		nested.clear();
	}
	
	
	/**
	 * Create a nested clone of the algorithm
	 * 
	 * @return the nested clone
	 */
	protected synchronized Graphviz newNested() {
		Graphviz g = clone();
		nested.add(g);
		return g;
	}
	
	
	/**
	 * Remove a nested clone
	 * 
	 * @param g the nested clone
	 */
	protected synchronized void removeNested(Graphviz g) {
		nested.remove(g);
	}
	
	
	/**
	 * Update the given layout from the given .dot file. The object must be locked
	 * by setProcessHandle() before calling this method.
	 * 
	 * @param layout the graph layout to update
	 * @param dot the input .dot file
	 * @param observer the job observer
	 */
	private void updateLayoutFromFile(GraphLayout layout, File dot, JobObserver observer) throws IOException {
		
		// Generate the layout
		
		File out = File.createTempFile("orbiter", ".plain");
		out.deleteOnExit();
		
		ExternalProcess p = createExternalProcess(dot, out);
		currentExternalProcess = p;
		p.run();
		currentExternalProcess = null;
		
		if (canceled) throw new RuntimeException("Canceled");
		
		
		// Load the layout
		
		layout.updateFromFile(out, null);
		
		if (canceled) throw new RuntimeException("Canceled");
		
		
		// Cleanup
		
		try {
			out.delete();
		}
		catch (Throwable t) {
			// Silent failover
		}
	}

	
	/**
	 * Do the layout algorithm and update an existing GraphLayout
	 * 
	 * @param layout the graph layout to update
	 * @param observer the job observer
	 */
	private void updateLayout(GraphLayout layout, JobObserver observer) {
		
		setProcessHandle(true, null);
		
		if (observer != null) observer.makeIndeterminate();
		
		try {
			
			// Export the input graph
			
			File dot = File.createTempFile("orbiter", ".dot");
			dot.deleteOnExit();
			layout.getGraph().dumpGraphviz(dot, "graph", directed);
			//graph.getRootBaseSummaryNode().dumpGraphviz(dot, "graph", directed);
			
			if (canceled) throw new RuntimeException("Canceled");
			
			
			// Call Graphviz
			
			updateLayoutFromFile(layout, dot, observer);
			
			
			// Cleanup
			
			try {
				dot.delete();
			}
			catch (Throwable t) {
				// Silent failover
			}
		}
		catch (IOException e) {
			setProcessHandle(false, null);
			throw new RuntimeException(e);
		}
		catch (Throwable t) {
			setProcessHandle(false, null);
			throw new RuntimeException(t);
		}
		
		setProcessHandle(false, null);
	}
	
	
	/**
	 * Write the children of the given summary node in the Graphviz format
	 *
	 * @param out the output stream
	 * @param node the summary node
	 * @param layoutMap the map of the child summary nodes (only the immediate children of the node) to their layouts
	 */
	private void printSummaryNodeToGraphviz(PrintStream out, BaseSummaryNode node, Map<BaseSummaryNode, GraphLayout> layoutMap) {
		
		out.println((directed ? "digraph" : "graph") + " \"cluster\" {");
		out.println("  rankdir=" + rankdir + ";");
		
		int numEdges = 0;
		for (BaseEdge e : node.getInternalEdges()) {
			if (!e.getBaseFrom().isVisible()) continue;
			if (!e.getBaseTo().isVisible()) continue;
			out.println("  " + e.getBaseFrom().getIndex() + (directed ? " -> " : " -- ") + e.getBaseTo().getIndex());
			numEdges++;
		}
		
		int numNodes = 0;
		for (BaseNode n : node.getBaseChildren()) {
			if (!n.isVisible()) continue;
			
			String label = Utils.escapeSimple(n.getLabel()); 

			if (n instanceof BaseSummaryNode) {
				GraphLayout l = layoutMap == null ? null : layoutMap.get(n);
				double w = 1;
				double h = 0;
				if (l == null) {
					w += DEFAULT_SUMMARY_NODE_WIDTH;
					h += DEFAULT_SUMMARY_NODE_HEIGHT;
				}
				else {
					w += l.getWidth()  / IMPORT_SCALE;
					h += l.getHeight() / IMPORT_SCALE;
				}
				out.println("  " + n.getIndex() + " [width=" + w + ",height=" + h + ",label=\"   " + label + "   \"];");
			}
			else {
				out.println("  " + n.getIndex() + " [label=\"   " + label + "   \"];");
			}
			
			numNodes++;
		}
		
		if (numEdges > 1000 && numNodes > 1000) {
			// NOTE I wonder if we should do something sane here
			System.err.println("Warning: Graphviz graph is too big (#nodes = " + numNodes + ", #edges = " + numEdges + ", parent = \"" + Utils.escapeSimple(node.getLabel()) + "\")");
			// For debugging: if (numEdges > 10000 && out != System.out) printSummaryNodeToGraphviz(System.out, node, layoutMap);
		}
		
		out.println("}");
	}

	
	/**
	 * Run GraphViz only on the specified summary node and compute the layout
	 * 
	 * @param node the summary node
	 * @param layoutMap the map of the child summary nodes (only the immediate children of the node) to their layouts
	 * @param observer the job observer
	 */
	private GraphLayout computeLayoutOfSummaryNode(BaseSummaryNode node, Map<BaseSummaryNode, GraphLayout> layoutMap,
			JobObserver observer) {
		
		
		// The trivial case - a single node, or no node at all
		
		int numChildNodes = node.getBaseChildren().size();
		
		if (numChildNodes == 0) return new SparseGraphLayout(node.getGraph(), this, getName());
		
		
		// The general case
		
		setProcessHandle(true, null);
		GraphLayout layout = null;
		
		if (observer != null) observer.makeIndeterminate();
		
		try {
			
			// Start Graphviz
			
			if (node.getGraph().getRootBaseSummaryNode() == node) {
				layout = new FastGraphLayout(node.getGraph(), this, getName());
			}
			else {
				layout = new SparseGraphLayout(node.getGraph(), this, getName());
			}
			
			String[] cmds = {algorithm, "-Tplain"};
			ExternalProcess p = new ExternalProcess(cmds);
			currentExternalProcess = p;
			PrintStream out = new PrintStream(p.start());
			
			if (canceled) throw new RuntimeException("Canceled");
			
			
			// Export the summary node
			
			printSummaryNodeToGraphviz(out, node, layoutMap);
			out.close();
			
			if (canceled) throw new RuntimeException("Canceled");
			
			
			// Finish Graphviz and load the layout

			layout.updateFromStream(p.getProcessOutputStream(), "plain", null);
			
			out.close();
			p.finish();
			currentExternalProcess = null;
			
			if (canceled) throw new RuntimeException("Canceled");
			
		}
		catch (IOException e) {
			setProcessHandle(false, null);
			throw new RuntimeException(e);
		}
		catch (Throwable t) {
			setProcessHandle(false, null);
			throw new RuntimeException(t);
		}
		
		setProcessHandle(false, null);
		return layout;
	}
	
	
	/**
	 * Combine layouts for a summary node. The object must be already locked
	 * by calling setProcessHandle()
	 * 
	 * @param node the summary node
	 * @param layoutMap the layouts of the child summary nodes
	 * @param g the graphviz object to use (this or a nested object)
	 * @return the graph layout
	 */
	private GraphLayout combineLayoutsForSummaryNode(BaseSummaryNode node, Map<BaseSummaryNode, GraphLayout> layoutMap, Graphviz g) {
		
		// Compute the layout of the summary node
		
		GraphLayout layout = g.computeLayoutOfSummaryNode(node, layoutMap, null);
		if (canceled) throw new RuntimeException("Canceled");
		
		
		// Merge the layouts
		
		for (BaseNode n : node.getBaseChildren()) {
			if (n instanceof BaseSummaryNode) {
				
				GraphLayout l = layoutMap.get((BaseSummaryNode) n);
				if (l == null) continue;
				
				GraphLayoutNode ln = layout.getLayoutNode(n.getIndex());
				
				l.centerAt(ln.getX(), ln.getY());
				if (!zoomBasedLayout) ln.setSize(l.getWidth(), l.getHeight());
				
				layout.importLayout(l, false);
			}
		}
		
		
		// Resize the layout
		
		if (zoomBasedLayout) {
			double newWidth = DEFAULT_SUMMARY_NODE_WIDTH * IMPORT_SCALE;
			double newHeight = DEFAULT_SUMMARY_NODE_HEIGHT * IMPORT_SCALE;
			double oldWidth = layout.getWidth();
			double oldHeight = layout.getHeight();
			
			double sw = newWidth / oldWidth;
			double sh = newHeight / oldHeight;
			double s  = Math.min(sw, sh);
			
			layout.scale(s);
		}
		
		return layout;
	}
	
	
	/**
	 * Create tasks for computing the layout recursively. The object must be already locked
	 * by calling setProcessHandle()
	 * 
	 * @param parent the parent task
	 * @param node the summary node
	 * @param maxDepth the maximum depth in the summary hierarchy
	 * @param observer the job observer
	 * @return the task that corresponds to the given summary node
	 */
	private RecursiveTask computeLayoutRecursivelyCreateTasks(RecursiveTask parent, BaseSummaryNode node, int maxDepth, SynchronizedJobObserver observer) {
		
		// Count the number of summary node children
		
		int numChildSummaries = 0;
		
		if (maxDepth > 1 || maxDepth < 0) {
			for (BaseNode n : node.getBaseChildren()) {
				if (n instanceof BaseSummaryNode) {
					numChildSummaries++;
				}
			}
		}
		
		
		// Create my task
		
		RecursiveTask task = new RecursiveTask(parent, node, observer);
		task.layoutMapExpected = numChildSummaries;
		
		if (numChildSummaries == 0) {
			taskQueue.add(task);
			return task;
		}
		
		
		// Create tasks for computing the layout of the child summary nodes
		
		if (maxDepth > 1 || maxDepth < 0) {
			for (BaseNode n : node.getBaseChildren()) {
				if (n instanceof BaseSummaryNode) {
					int d = maxDepth < 0 ? -1 : (maxDepth - 1);
					RecursiveTask t = computeLayoutRecursivelyCreateTasks(task, (BaseSummaryNode) n, d, observer);
					task.numTasks += t.numTasks;
				}
			}
		}
		
		
		// Finish
		
		return task;
	}
	
	
	/**
	 * Compute the layout recursively. The object must be already locked
	 * by calling setProcessHandle()
	 * 
	 * @param node the summary node
	 * @param maxDepth the maximum depth in the summary hierarchy
	 * @param observer the job observer
	 * @return the graph layout
	 */
	private GraphLayout computeLayoutRecursively(BaseSummaryNode node, int maxDepth, SynchronizedJobObserver observer) {
		
		// Create the tasks recursively for all summary nodes
		
		taskQueue.clear();
		Pointer<GraphLayout> pResult = new Pointer<GraphLayout>();
		RecursiveTask rootTask = computeLayoutRecursivelyCreateTasks(null, node, maxDepth, observer);
		rootTask.pResult = pResult;
		
		
		// Create & run the worker threads
		
		if (observer != null) observer.setRange(0, rootTask.numTasks);
		
		Vector<WorkerThread> workers = new Vector<WorkerThread>(maxWorkers);
		for (int i = 0; i < maxWorkers; i++) {
			WorkerThread w = new WorkerThread();
			workers.add(w);
			w.start();
		}
		
		
		// Wait for the results
		
		for (WorkerThread w : workers) {
			try {
				w.join();
			}
			catch (InterruptedException e) {
				if (w.error != null) w.error = e;
			}
		}
		
		
		// Process errors
		
		for (WorkerThread w : workers) {
			if (w.error != null) {
				if (w.error instanceof RuntimeException) {
					throw ((RuntimeException) w.error);
				}
				else {
					throw new RuntimeException(w.error);
				}
			}
		}
		
		
		// Finish
		
		return pResult.get();
	}

	
	/**
	 * Initialize the graph layout for the given graph
	 * 
	 * @param graph the input graph
	 * @param levels the number of levels in the hierarchy of summary nodes to precompute 
	 * @param observer the job observer
	 * @return the graph layout
	 */
	public GraphLayout initializeLayout(BaseGraph graph, int levels, JobObserver observer) {
		
		if (observer != null) observer.makeIndeterminate();
		
		
		// The simple case: ignore the summary nodes and give GraphViz the entire graph
		
		if (!bySummaryNodes) {
			GraphLayout layout = new FastGraphLayout(graph, this, getName());
			updateLayout(layout, observer);
			return layout;
		}
		
		
		// The complicated case: build the graph layout recursively using summary nodes
		
		setProcessHandle(true, null);
		GraphLayout layout = null;
		
		try {
			BaseSummaryNode root = graph.getRootBaseSummaryNode();
			if (root == null) {
				throw new IllegalStateException("No root summary node (the graph was not summarized)");
			}
			SynchronizedJobObserver sjo = new SynchronizedJobObserver(observer);
			layout = computeLayoutRecursively(root, levels, sjo);
			
			
			// Add the root node
			
			GraphLayoutNode lr = new GraphLayoutNode(root, 0, 0);
			layout.centerAt(0, 0);
			lr.setSize(layout.getWidth(), layout.getHeight());
			layout.addLayoutNode(lr);
			
			
			// Finish
			
			layout.setOptimizedForZoom(isZoomOptimized());
		}
		catch (RuntimeException e) {
			setProcessHandle(false, null);
			throw e;
		}
		catch (Error e) {
			setProcessHandle(false, null);
			throw e;
		}
		catch (Throwable t) {
			setProcessHandle(false, null);
			throw new RuntimeException(t);
		}
		
		setProcessHandle(false, null);
		return layout;
	}

	
	/**
	 * Update an existing layout by incrementally expanding the given summary node
	 * 
	 * @param layout the graph to update
	 * @param node the summary node to expand
	 * @param observer the job observer
	 */
	public void updateLayout(GraphLayout layout, BaseSummaryNode node, JobObserver observer) {
		
		if (!node.isVisible()) return;
		
		GraphLayoutNode layoutNode = layout.getLayoutNode(node.getIndex());
		
		
		// In the case of root nodes, we can just make something up
		
		if (layoutNode == null && node == node.getGraph().getRootBaseSummaryNode()) {
			layoutNode = new GraphLayoutNode(node, 0, 0);
			layoutNode.setSize(4 * IMPORT_SCALE, 3 * IMPORT_SCALE);
			layout.addLayoutNode(layoutNode);
		}
		
		
		// If the node does not already have a layout, escalate the update one level higher
		
		if (layoutNode == null) {
			updateLayout(layout, node.getParent(), observer);
			layoutNode = layout.getLayoutNode(node.getIndex());
			if (layoutNode == null) {
				throw new RuntimeException("Failed to compute the layout of node " + node);
			}
		}
		
		
		// Updating is worth it only if at least one of the children does not have a layout
		
		boolean okay = false;
		
		for (BaseNode n : node.getBaseChildren()) {
			if (n.isVisible() && layout.getLayoutNode(n.getIndex()) == null) {
				okay = true;
				break;
			}
		}
		
		if (!okay) return;
		
		
		// Do the magic
		
		setProcessHandle(true, null);
		if (observer != null) observer.makeIndeterminate();
		
		try {
			
			SynchronizedJobObserver sjo = observer == null ? null : new SynchronizedJobObserver(observer);
			GraphLayout l = computeLayoutRecursively(node, 1, sjo);

			
			// Resize
			
			double newWidth = layoutNode.getWidth();
			double newHeight = layoutNode.getHeight();
			double oldWidth = l.getWidth();
			double oldHeight = l.getHeight();
			
			double sw = newWidth / oldWidth;
			double sh = newHeight / oldHeight;
			double s  = Math.min(sw, sh);
			
			l.scale(s);
			
			
			// Center & combine
			
			l.centerAt(layoutNode.getX(), layoutNode.getY());
			layout.importLayout(l, false);
		}
		catch (RuntimeException e) {
			setProcessHandle(false, null);
			throw e;
		}
		catch (Throwable t) {
			setProcessHandle(false, null);
			throw new RuntimeException(t);
		}
		
		setProcessHandle(false, null);
	}
	
	
	/**
	 * Compute the layout for the entire graph
	 * 
	 * @param graph the input graph
	 * @param observer the job observer
	 * @return the graph layout
	 */
	public GraphLayout computeLayout(BaseGraph graph, JobObserver observer) {
		return initializeLayout(graph, -1, observer);
	}
	

	/**
	 * Get a configuration GUI for the parser 
	 * 
	 * @return a list of WizardPanel's for the GUI configuration, or null if not necessary
	 */
	public List<WizardPanel> createConfigurationGUI() {
		
		List<WizardPanel> r = new Vector<WizardPanel>();
		
		r.add(new ConfigPanel());
		
		return r;
	}
	
	
	/**
	 * Write the object to XML
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {
		
		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		attrs.addAttribute("", "", "algorithm", "CDATA", "" + algorithm);
		hd.startElement("", "", "graphviz", attrs);
		
		attrs.clear();
		String s = rankdir;
		hd.startElement("", "", "rankdir", attrs);
		hd.characters(s.toCharArray(), 0, s.length());
		hd.endElement("", "", "rankdir");
		
		s = "" + bySummaryNodes;
		hd.startElement("", "", "by-summary-nodes", attrs);
		hd.characters(s.toCharArray(), 0, s.length());
		hd.endElement("", "", "by-summary-nodes");
		
		s = "" + zoomBasedLayout;
		hd.startElement("", "", "zoom-based", attrs);
		hd.characters(s.toCharArray(), 0, s.length());
		hd.endElement("", "", "zoom-based");
		
		hd.endElement("", "", "graphviz");
	}
	
	
	/**
	 * Load the object from XML
	 * 
	 * @param element the XML DOM element
	 * @throws SAXException on error
	 */
	public void loadFromXML(Element element) throws SAXException, ParserException {
		
		if (!element.getNodeName().equals("graphviz")) {
			throw new ParserException("Expected <graphviz>, found <" + element.getNodeName() + ">");
		}
		
		
		// Attributes
		
		try {
			setAlgorithm(XMLUtils.getAttribute(element, "algorithm"));
		}
		catch (Exception e) {
			throw new ParserException(e);
		}
		
		
		// Algorithm properties
		
		rankdir = XMLUtils.getTextValue(element, "rankdir");
		bySummaryNodes = Boolean.parseBoolean(XMLUtils.getTextValue(element, "by-summary-nodes"));
		zoomBasedLayout = Boolean.parseBoolean(XMLUtils.getTextValue(element, "zoom-based"));
	}
	
	
	/**
	 * A recursive task with continuation
	 */
	private class RecursiveTask implements Runnable {
		
		private BaseSummaryNode node;
		private SynchronizedJobObserver observer;
		
		public GraphLayout result;
		public Pointer<GraphLayout> pResult;
		public Map<BaseSummaryNode, GraphLayout> layoutMap;
		public int layoutMapExpected;
		public int numTasks;
		
		public RecursiveTask parent;
		public Throwable error;
		public Graphviz g;
		
		
		/**
		 * Create an instance of class RecursiveTask
		 * 
		 * @param parent the parent task
		 * @param node the summary node
		 * @param observer the job observer
		 */
		public RecursiveTask(RecursiveTask parent, BaseSummaryNode node, SynchronizedJobObserver observer) {
			
			this.node = node;
			this.observer = observer;
			
			this.result = null;
			this.pResult = null;
			
			this.layoutMap = new HashMap<BaseSummaryNode, GraphLayout>();;
			this.layoutMapExpected = Integer.MAX_VALUE;
			this.numTasks = 1;
			
			this.parent = parent;
			this.error = null;
			this.g = null;
		}
		
		
		/**
		 * Perform the work
		 */
		@Override
		public void run() {
			
			if (result != null) return;
			
			try {
				error = null;
				result = combineLayoutsForSummaryNode(node, layoutMap, g == null ? Graphviz.this : g);
				
				if (pResult != null) pResult.set(result);
				if (observer != null) observer.addProgress(1);
				
				
				// Store the result and schedule the continuation if we reached
				// the required number of inputs to the continuation task
				
				if (parent != null) {
					synchronized (parent) {
						parent.layoutMap.put(node, result);
						if (parent.layoutMap.size() >= parent.layoutMapExpected) {
							taskQueue.add(parent);
						}
					}
				}
			}
			catch (Throwable t) {
				error = t;
			}
		}
	}
	
	
	/**
	 * A worker thread for recursive tasks
	 */
	private class WorkerThread extends Thread {
		
		public Throwable error;
		
		
		/**
		 * Create an instance of the worker thread
		 */
		public WorkerThread() {
			error = null;
		}
		
		
		/**
		 * Run the tasks
		 */
		@Override
		public void run() {
			
			Graphviz g = newNested();
			error = null;

			try {
				while (true) {
					
					RecursiveTask t = taskQueue.remove(0);
					t.g = g;
					t.run();
					
					if (t.error != null) {
						error = t.error;
						break;
					}
					
					if (canceled) break;
				}
			}
			catch (IndexOutOfBoundsException e) {
				// Done
			}
			catch (Throwable t) {
				if (error != null) error = t;
				cancel();
			}
			
			removeNested(g);
		}
	}
	
	
	/**
	 * Configuration panel
	 */
	private class ConfigPanel extends WizardPanel implements ActionListener {
		
		private JLabel topLabel;
		private JLabel algorithmLabel;
		private JComboBox algorithmCombo;
		private JLabel rankdirLabel;
		private JComboBox rankdirCombo;
		private JCheckBox useSummariesCheck;
		private JCheckBox semanticZoomCheck;
		
		// TODO Make the following configurable
		private boolean mustUseSummaries = true;

		
		/**
		 * Create an instance of ConfigPanel
		 */
		public ConfigPanel() {
			super("Configure Graphviz");
			
			
			// Initialize the panel
			
			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			
			int gridy = 0;
			
			
			// Header

			topLabel = new JLabel("Configure graph layout:");
			topLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			c.weightx = 1;
			c.weighty = 0;
			panel.add(topLabel, c);
			c.weightx = 0;
			c.gridwidth = 1;
			
			gridy++;

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(new JLabel(" "), c);
			c.gridwidth = 1;

			gridy++;
			
			
			// Algorithm
			
			algorithmLabel = new JLabel("Graph layout algorithm:   ");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(algorithmLabel, c);
			
			algorithmCombo = new JComboBox(mustUseSummaries ? ALGORITHMS_CAN_USE_SUMMARIES : ALGORITHMS);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(algorithmCombo, c);
			
			int i = 0, index = 0;
			for (String a : (mustUseSummaries ? ALGORITHMS_CAN_USE_SUMMARIES : ALGORITHMS)) {
				if (a.equals(algorithm)) index = i;
				i++;
			}
			
			algorithmCombo.setSelectedIndex(index);
			
			gridy++;
			
			
			// Rank-direction
			
			rankdirLabel = new JLabel("Graph direction:   ");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(rankdirLabel, c);
			
			rankdirCombo = new JComboBox(RANKDIRS_LONG);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(rankdirCombo, c);
			
			i = 0; index = 0;
			for (String a : RANKDIRS) {
				if (a.equals(rankdir)) index = i;
				i++;
			}
			
			rankdirCombo.setSelectedIndex(index);
			
			gridy++;
			
			
			// Use of summaries

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(new JLabel(" "), c);
			c.gridwidth = 1;

			gridy++;
			
			useSummariesCheck = new JCheckBox("Compute the layout using node summaries");
			useSummariesCheck.setSelected(bySummaryNodes);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(useSummariesCheck, c);
			c.gridwidth = 1;
			
			gridy++;
			
			semanticZoomCheck = new JCheckBox("Optimize the layout for semantic zoom");
			semanticZoomCheck.setSelected(zoomBasedLayout);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(semanticZoomCheck, c);
			c.gridwidth = 1;
			
			gridy++;
			
			
			// Finish

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			c.weighty = 1;
			panel.add(new JLabel(" "), c);
			c.weighty = 0;
			c.gridwidth = 1;

			gridy++;
			
			algorithmCombo.addActionListener(this);
			useSummariesCheck.addActionListener(this);
			
			updateEnabled();
		}
		
		
		/**
		 * Callback for when the next button was clicked
		 */
		protected void wizardNext() {
			
			String a = (mustUseSummaries ? ALGORITHMS_CAN_USE_SUMMARIES : ALGORITHMS)[algorithmCombo.getSelectedIndex()];
			boolean canUseSummaries = Utils.contains(ALGORITHMS_CAN_USE_SUMMARIES, a);
			
			try {
				setAlgorithm(a);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			rankdir = RANKDIRS[rankdirCombo.getSelectedIndex()];
			
			bySummaryNodes = useSummariesCheck.isSelected() && canUseSummaries;
			zoomBasedLayout = semanticZoomCheck.isSelected() && bySummaryNodes;
		}
		
		
		/**
		 * Update the enabled properties
		 */
		private void updateEnabled() {
			
			String a = (mustUseSummaries ? ALGORITHMS_CAN_USE_SUMMARIES : ALGORITHMS)[algorithmCombo.getSelectedIndex()];
			boolean dot = "dot".equalsIgnoreCase(a);
			boolean canUseSummaries = Utils.contains(ALGORITHMS_CAN_USE_SUMMARIES, a);
			
			rankdirLabel.setEnabled(dot);
			rankdirCombo.setEnabled(dot);
			
			useSummariesCheck.setEnabled(canUseSummaries);
			semanticZoomCheck.setEnabled(useSummariesCheck.isSelected() && canUseSummaries);
		}


		/**
		 * A callback for when an action has been performed
		 * 
		 * @param e the action event
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			updateEnabled();
		}
	}
}
