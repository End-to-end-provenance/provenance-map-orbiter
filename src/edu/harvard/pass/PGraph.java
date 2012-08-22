/*
 * Provenance Aware Storage System - Java Utilities
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

package edu.harvard.pass;

import edu.harvard.pass.job.ProvRankJob;
import edu.harvard.pass.parser.ParserHandler;
import edu.harvard.util.filter.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.graph.layout.GraphLayout;
import edu.harvard.util.graph.layout.GraphLayoutAlgorithm;
import edu.harvard.util.gui.JobMasterDialog;
import edu.harvard.util.job.*;
import edu.harvard.util.*;
import edu.harvard.util.XMLUtils;

import java.util.*;
import java.io.*;

import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;


/**
 * A provenance graph. It contains nodes of type PNode and edges of type PEdge.
 * 
 * @author Peter Macko
 */
public class PGraph extends Graph<PNode, PEdge, PSummaryNode, PGraph> implements java.io.Serializable, WithPMeta {

	private static final long serialVersionUID = 655662268695399297L;
	
	public static final String DOM_ELEMENT = "provenance-graph";
	
	protected HashMap<Integer, PObject> fdToObject;
	protected HashMap<String, PNode> idToNode;
	protected int lastNegativeFD;
	protected boolean fixForkparentEdges;

	protected PGraphStat stat;
	protected PMeta meta;
	
	protected PGraph parent;
	protected TreeMap<String, PGraph> derivedGraphs;
	protected String description;
	
	protected boolean hasProvRank;
	
	private ParserHandler parserHandler;
	
	
	/**
	 * Constructor for objects of type PGraph
	 * 
	 * @param meta the metadata information
	 */
	public PGraph(PMeta meta) {
		
		this.meta = meta;
		
		parent = null;
		derivedGraphs = new TreeMap<String, PGraph>();
		description = "";
		
		parserHandler = null;
		
		clear();
	}
	
	
	/**
	 * Create an empty root summary node
	 * 
	 * @return the instantiated summary node
	 */
	@Override
	protected PSummaryNode newRootSummaryNode() {
		return new PSummaryNode(this);
	}
	
	
	/**
	 * Create an empty summary node
	 * 
	 * @param parent the parent node
	 * @return the instantiated summary node
	 */
	@Override
	public PSummaryNode newSummaryNode(BaseSummaryNode parent) {
		return new PSummaryNode((PSummaryNode) parent);
	}

	
	/**
	 * Constructor for objects of type PGraph
	 */
	public PGraph() {
		
		this(new PMeta());
	}
	
	
	/**
	 * Clear
	 */
	protected void clear() {
		
		super.clear();
		
		fdToObject = new HashMap<Integer, PObject>();
		idToNode = new HashMap<String, PNode>();
		
		lastNegativeFD = 0;
		fixForkparentEdges = false;
		
		stat = new PGraphStat();
		
		hasProvRank = false;
	}
	
	
	/**
	 * Get the time base, relative to which the time should be reported
	 * 
	 * @return the time base
	 */
	public double getTimeBase() {
		return stat.timeUnadjustedMin > stat.timeUnadjustedMax ? 0 : stat.timeUnadjustedMin;
	}
	
	
	/**
	 * Get the graph description used for summary and other kinds of derived
	 * graphs. This is also used as a key when looking up derived graphs.
	 * 
	 * @return the description or the name of the derived graph
	 */
	public String getDescription() {
		return description;
	}
	
	
	/**
	 * Return the graph this graph was derived from
	 * 
	 * @return the parent graph, or null if none
	 */
	public PGraph getParent() {
		return parent;
	}
	
	
	/**
	 * Return an existing derived graph by its description/name
	 * 
	 * @return the derived graph, or null if none
	 */
	public PGraph getDerivedGraph(String description) {
		return derivedGraphs.get(description);
	}
	
	
	/**
	 * Return all derived graphs
	 * 
	 * @return the collection of derived graphs
	 */
	public Collection<PGraph> getDerivedGraphs() {
		return derivedGraphs.values();
	}
	
	
	/**
	 * Set the metadata that should be associated with the graph
	 * 
	 * @param meta the new metadata
	 */
	public void setMeta(PMeta meta) {
		this.meta = meta;
	}
	
	
	/**
	 * Get the metadata associated with the graph
	 * 
	 * @return the metadata
	 */
	public PMeta getMeta() {
		return meta;
	}
	
	
	/**
	 * Get parser handler that creates this graph
	 * 
	 * @return the handler for parsing
	 */
	public ParserHandler getParserHandler() {
		if (parserHandler == null) parserHandler = new PGraphParserHandler();
		return parserHandler;
	}
	
	
	/**
	 * Determine whether the ProvRank has been computed
	 * 
	 * @return true if it is computed
	 */
	public boolean wasProvRankComputed() {
		return hasProvRank;
	}
	
	
	/**
	 * Set whether the ProvRank has been computed
	 * 
	 * @param v true if ProvRank was computed
	 */
	public void setWasProvRankComputed(boolean v) {
		hasProvRank = v;
	}
	
	
	/**
	 * Require that ProvRank has been computed (compute if necessary)
	 * 
	 * @return true if it was just computed
	 */
	public boolean requireProvRank() {
		
		if (hasProvRank) return false;
		
		JobMasterDialog j = new JobMasterDialog(null, "ProvRank");
		j.add(new ProvRankJob(new Pointer<PGraph>(this)));
		
		try {
			j.run();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (!hasProvRank) throw new RuntimeException("ProvRank has not been computed");
		
		return true;
	}
	
	
	/**
	 * Update the statistics given the node
	 * 
	 * @param node the node
	 */
	private void updateStatistics(PNode node) {
		
		int i;
		double f;
		PObject o = node.getObject();
		
		i = node.getObject().getFD();
		if (i < stat.fdMin) stat.fdMin = i;
		if (i > stat.fdMax) stat.fdMax = i;

		f = node.getTimeUnadjusted();
		if (f >= 0.001) {
			if (f < stat.timeUnadjustedMin) stat.timeUnadjustedMin = f;
			if (f > stat.timeUnadjustedMax) stat.timeUnadjustedMax = f;
		}
		
		i = node.getIncomingEdges().size();
		if (i < stat.indegreeMin) stat.indegreeMin = i;
		if (i > stat.indegreeMax) stat.indegreeMax = i;
		
		i = node.getOutgoingEdges().size();
		if (i < stat.outdegreeMin) stat.outdegreeMin = i;
		if (i > stat.outdegreeMax) stat.outdegreeMax = i;
		
		i = node.getIncomingEdges().size() + node.getOutgoingEdges().size();
		if (i < stat.degreeMin) stat.degreeMin = i;
		if (i > stat.degreeMax) stat.degreeMax = i;
		
		i = node.getDepth();
		if (i < stat.depthMin) stat.depthMin = i;
		if (i > stat.depthMax) stat.depthMax = i;
		
		f = node.getProvRank();
		if (f > 0 && o.getName() != null) {
			if (f < stat.provRankMin) stat.provRankMin = f;
			if (f > stat.provRankMax) stat.provRankMax = f;
			
			for (PEdge e : node.getOutgoingEdges()) {
				PNode m = e.getTo();
				double g = m.getProvRank();
				
				if (g > 0 && m.getObject().getName() != null) {
					double d = g - f;
					if (d < stat.provRankJumpMin) stat.provRankJumpMin = d;
					if (d > stat.provRankJumpMax) stat.provRankJumpMax = d;
					d = Math.log(g) - Math.log(f);
					if (d < stat.provRankLogJumpMin) stat.provRankLogJumpMin = d;
					if (d > stat.provRankLogJumpMax) stat.provRankLogJumpMax = d;
				}
			}
		}
	}
	
	
	/**
	 * Finish adding a node
	 * 
	 * @param node a new node
	 */
	private void addNodeFinish(PNode node) {
		
		idToNode.put(node.getPublicID(), node);
		
		PObject o = node.getObject();
		if (fdToObject.get(o.getFD()) == null) {
			fdToObject.put(o.getFD(), o);
		}
		
		updateStatistics(node);
	}
	
	
	/**
	 * Add a node
	 * 
	 * @param node a new node
	 */
	@Override
	public void addNode(PNode node) {
		
		super.addNode(node);
		addNodeFinish(node);
	}
	
	
	/**
	 * Add a node using the specified index. Use at your own risk.
	 * 
	 * @param node a new node
	 */
	@Override
	protected void addNodeExt(PNode node) {
		
		super.addNodeExt(node);
		addNodeFinish(node);
	}


	/**
	 * Get a node given its ID. Create the node if necessary
	 *
	 * @param id the node ID
	 * @return the node
	 */
	protected PNode getNode(String id) {
		
		id = Utils.removeQuotes(id);
		PNode n = idToNode.get(id);
		if (n != null) return n;
		
		
		// Parse out the FD and the version, if possible
		
		int fd, ver;

		try {
			int dot = id.indexOf('.');
			if (dot <= 0) throw new Exception();
			
			String sf = id.substring(0, dot);
			String sv = id.substring(dot + 1);
			fd = Integer.parseInt(sf);
			ver = Integer.parseInt(sv);
		}
		catch (Exception e) {
			
			fd = --lastNegativeFD;
			ver = 0;
		}
		
		
		// Get the provenance object, create if necessary
		
		PObject o = fdToObject.get(fd);
		if (o == null) {
			o = new PObject(this, fd);
			fdToObject.put(fd, o);
		}
		

		// Create the node
		
		n = new PNode(o, ver, fd < 0 ? id : null);
		addNode(n);
		
		return n;
	}


	/**
	 * Get the node. Create it if necessary
	 *
	 * @param o the provenance object
	 * @param ver the version
	 * @return the node
	 */
	protected PNode getNode(PObject o, int ver) {
	
		PNode p = o.getNode(ver);
		if (p != null) return p;
		
		
		// Create the node
		
		p = new PNode(o, ver);
		addNode(p);
		
		return p;
	}


	/**
	 * Get the next version of the node. Create it if necessary
	 *
	 * @param n the PNode
	 * @return the next version
	 */
	protected PNode getNextNode(PNode n) {
		return getNode(n.getObject(), n.getVersion() + 1);
	}
	
	
	/**
	 * Add an edge
	 * 
	 * @param edge a new edge
	 */
	@Override
	public void addEdge(PEdge edge) {
		
		super.addEdge(edge);
		updateStatistics(edge.getFrom());
	}

	
	/**
	 * Recompute ProvRank statistics
	 */
	public void updateProvRankStatistics() {

		double f;
		
		stat.provRankMin = Double.MAX_VALUE;
		stat.provRankMax = Double.MIN_VALUE;
		stat.provRankJumpMin = Double.MAX_VALUE;
		stat.provRankJumpMax = Double.MIN_VALUE;
		stat.provRankLogJumpMin = Double.MAX_VALUE;
		stat.provRankLogJumpMax = Double.MIN_VALUE;

		for (PNode n : getNodes()) {
			
			f = n.getProvRank();
			if (f > 0 && n.getObject().getName() != null) {
				if (f < stat.provRankMin) stat.provRankMin = f;
				if (f > stat.provRankMax) stat.provRankMax = f;
				
				for (PEdge e : n.getOutgoingEdges()) {
					PNode m = e.getTo();
					double g = m.getProvRank();
					
					if (g > 0 && m.getObject().getName() != null) {
						double d = g - f;
						if (d < stat.provRankJumpMin) stat.provRankJumpMin = d;
						if (d > stat.provRankJumpMax) stat.provRankJumpMax = d;
						d = Math.log(g) - Math.log(f);
						if (d < stat.provRankLogJumpMin) stat.provRankLogJumpMin = d;
						if (d > stat.provRankLogJumpMax) stat.provRankLogJumpMax = d;
					}
				}
			}
		}
	}


	/**
	 * Return the graph statistics
	 *
	 * @return the graph statistics
	 */
	public PGraphStat getStat() {
		return stat;
	}


	/**
	 * Export the graph. Determine the output format from the file extension
	 *
	 * @param fileName the file name
	 * @param observer the job progress observer
	 * @throws IOException if an error occurred
	 */
	public void export(String fileName, JobObserver observer) throws IOException {

		File file = new File(fileName);
		String ext = Utils.getExtension(file);


		// Graphviz

		if (ext.equals("dot")) {
			exportGraphviz(file, observer);
			return;
		}


		// Unrecognized file extension

		throw new IOException("Unrecognized file extension ." + ext);
	}


	/**
	 * Export to the Graphviz format
	 *
	 * @param file the file
	 * @param observer the job progress observer
	 * @throws IOException if an error occurred
	 */
	public void exportGraphviz(File file, JobObserver observer) throws IOException {

		PrintWriter out = new PrintWriter(new FileWriter(file));

		out.println("digraph twig {");
		for (PEdge e : getEdges()) {
			out.println("  " + e.getFrom().getIndex() + " -> " + e.getTo().getIndex());
		}
		out.println("}");

		out.close();
	}


	/**
	 * Export the nodes. Determine the output format from the file extension
	 *
	 * @param fileName the file name
	 * @param observer the job progress observer
	 * @throws IOException if an error occurred
	 */
	public void exportNodes(String fileName, JobObserver observer) throws IOException {

		File file = new File(fileName);
		String ext = Utils.getExtension(file);


		// CSV

		if (ext.equals("csv")) {
			exportNodesCSV(file, observer);
			return;
		}


		// Unrecognized file extension

		throw new IOException("Unrecognized file extension ." + ext);
	}


	/**
	 * Export nodes to the CSV format
	 *
	 * @param file the file
	 * @param observer the job progress observer
	 * @throws IOException if an error occurred
	 */
	public void exportNodesCSV(File file, JobObserver observer) throws IOException {

		PrintWriter out = new PrintWriter(new FileWriter(file));

		out.println(PNode.getHeaderCSV());
		for (PNode n : getNodes()) {
			out.println(n.toCSV());
		}

		out.close();
	}


	/**
	 * Return the map of file descriptors to provenance objects
	 *
	 * @return the map of objects
	 */
	public HashMap<Integer, PObject> getObjectMap() {
		return fdToObject;
	}


	/**
	 * Return the map of IDs to provenance nodes
	 *
	 * @return the map of nodes
	 */
	public HashMap<String, PNode> getNodeMap() {
		return idToNode;
	}
	
	
	/**
	 * Import a node from another PGraph. Clone the node, preserving its ID,
	 * and add it to this PGraph.
	 * 
	 * @param node the node to import
	 * @return the cloned node
	 */
	public PNode importNode(PNode node) {
		
		// Get a clone of the PObject, create if necessary
		
		PObject o = fdToObject.get(node.getObject().getFD());
		if (o == null) {
			o = node.getObject().copy(this);
			fdToObject.put(node.getObject().getFD(), o);
		}
		
		
		// Clone the node
		
		PNode c = node.copy(o);
		c.setID(node.getID());
		addNode(c);

		return c;
	}
	
	
	/**
	 * Add edges from the given node to the list to the downstream nodes that satisfy the filter
	 * 
	 * @param node the current node
	 * @param start the start node
	 * @param filter the filter that accepts the nodes to include in the result
	 * @param visited the set of visited nodes
	 * @param graph the result graph that is being constructed
	 * @return the number of edges added to the result set
	 */
	private int createSummaryGraphHelper(PNode node, PNode start, Filter<PNode> filter, Set<PNode> visited, PGraph graph) {
		
		visited.add(node);
		
		int count  = 0;
		
		for (PEdge e : node.getOutgoingEdges()) {
			PNode target = e.getTo();
			
			if (filter.accept(target)) {
				visited.add(target);
				PEdge.Type type = node == start ? e.getType() : PEdge.Type.COMPOUND;
				graph.addEdge(new PEdge(graph.getNodeByID(start.getID()), graph.getNodeByID(target.getID()), type));
				count++;
			}
			else {
				if (visited.contains(target)) continue;
				count += createSummaryGraphHelper(e.getTo(), start, filter, visited, graph);
			}
		}
		
		return count;
	}
	
	
	/**
	 * Create a summary graph with coarser granularity
	 * 
	 * @param filter the filter that accepts the nodes to include in the result
	 * @param description a short description or name of the summary graph
	 * @return the graph with only the filtered nodes 
	 */
	public PGraph createSummaryGraph(Filter<PNode> filter, String description) {
		
		// Initialize
		
		PGraph graph = new PGraph();
		LinkedList<PNode> accepted = new LinkedList<PNode>();
		
		
		// Keep only the requested nodes
		
		for (PNode p : getNodes()) {
			if (filter.accept(p)) {
				graph.importNode(p);
				accepted.add(p);
			}
		}
		
		
		// Now add the edges
		
		HashSet<PNode> visited = new HashSet<PNode>();
		for (PNode n : accepted) {
			visited.clear();
			createSummaryGraphHelper(n, n, filter, visited, graph);
		}
		
		
		// Finish
		
		graph.description = description;
		graph.parent = this;
		derivedGraphs.put(graph.description, graph);

		return graph;
	}
	
	
	/**
	 * Create a summary graph with coarser granularity
	 * 
	 * @param filter the filter that accepts the nodes to include in the result
	 * @return the graph with only the filtered nodes 
	 */
	public PGraph createSummaryGraph(Filter<PNode> filter) {
		return createSummaryGraph(filter, filter.toExpressionString());
	}
	
	
	/**
	 * Create a graph where all versions are collapsed
	 * 
	 * @return the graph with the collapsed versions (note that this is no longer a DAG)
	 */
	public PGraph createCollapsedGraph() {
		
		// Initialize
		
		PGraph graph = new PGraph();
		LinkedList<PNode> accepted = new LinkedList<PNode>();
		
		
		// Keep only one node per version
		
		for (PNode n : getNodes()) {
			if (n.getVersion() == 0) {
				PNode imported = graph.importNode(n);
				imported.setVisible(true);
				accepted.add(n);
			}
		}
		
		
		// Now add the edges
		
		Set<PNode> visited = new HashSet<PNode>();
		
		for (PNode n : accepted) {
			visited.clear();
			for (PNode p : n.getObject().getVersions()) {
				for (PEdge e : p.getOutgoingEdges()) {
					if (visited.contains(e.getTo())) continue;
					
					PNode f = graph.getNodeByID(n.getID());
					PNode t = graph.getNodeByID(e.getTo().getObject().getNode(0).getID());
					
					if (f != t || e.getFrom() == e.getTo()) graph.addEdge(new PEdge(f, t, PEdge.Type.COMPOUND));
					visited.add(e.getTo());
				}
			}
		}
		
		
		// Finish
		
		graph.description = "Collapsed versions";
		graph.parent = this;
		derivedGraphs.put(graph.description, graph);
		
		return graph;
	}
	
	
	/**
	 * Finish graph summarization. No summary nodes can be added or modified after this point
	 */
	@Override
	public synchronized void summarizationEnd() {
		super.summarizationEnd();
		stat.clear();
		for (PNode n : getNodes()) updateStatistics(n);
	}
	
	
	/**
	 * Parser handler
	 * 
	 * @author Peter Macko
	 */
	private class PGraphParserHandler implements WithPMeta, ParserHandler {
		
		/**
		 * Start loading the graph
		 */
		public void beginParsing() {
			clear();
		}
		
		
		/**
		 * Process an ancestry triple
		 * 
		 * @param s_pnode the string version of a p-node
		 * @param s_edge the string version of an edge
		 * @param s_value the string version of a value
		 */
		public void loadTripleAncestry(String s_pnode, String s_edge, String s_value) {

			// Source node (silently skip over bad input)
			
			PNode n;
			try {
				n = getNode(s_pnode);
			}
			catch (IllegalStateException e) { throw e; }
			catch (UnsupportedOperationException e) { throw e; }
			catch (IllegalArgumentException e) { throw e; }
			catch (Exception e) { return; }
			
			
			// Target node
			
			PNode t = getNode(s_value);
			
			
			// Edge type
			
			PEdge.Type type = meta.getEdgeType(s_edge);
			
			
			// PASS-specific control-flow (FORKPARENT) handling

			if (type == PEdge.Type.CONTROL) {
				if (t.getObject().getType() != PObject.Type.AGENT) {
					n.getObject().setParentFD(t.getFD());
				}
				
				if (t.getVersion() == 0 && t.getFD() > 0 && "FORKPARENT".equalsIgnoreCase(s_edge)) {
					fixForkparentEdges = true;
					return;
				}
				
				if (n.getFD() > 0 && n.getVersion() == 0) {
					n = getNextNode(n);
				}
			}
			
			
			// Add the edge

			addEdge(new PEdge(n, t, type, s_edge));
		}
		
		
		/**
		 * Process an attribute triple
		 * 
		 * @param s_pnode the string version of a p-node
		 * @param s_edge the string version of an edge
		 * @param s_value the string version of the second p-node
		 */
		public void loadTripleAttribute(String s_pnode, String s_edge, String s_value) {

			// Source node (silently skip over bad input)
			
			PNode n;
			try {
				n = getNode(s_pnode);
			}
			catch (IllegalStateException e) { throw e; }
			catch (UnsupportedOperationException e) { throw e; }
			catch (IllegalArgumentException e) { throw e; }
			catch (Exception e) { return; }
			
			
			// Process the node properties

			PNode.Attribute pna = meta.getNodeAttributeCode(s_edge);
			if (pna != PNode.Attribute.OTHER) {
				n.setAttribute(s_edge, s_value);
				updateStatistics(n);
				return;
			}
			
			
			// Process the object / node properties

			if (n.getVersion() == 0) {
				n.getObject().setAttribute(s_edge, s_value);
			}
			else {
				n.setAttribute(s_edge, s_value);
			}
		}
		
		
		/**
		 * Finish loading the graph
		 */
		public void endParsing() {

			// Fix the PASS control flow (FORKPARENT) edges

			if (fixForkparentEdges) {
				for (PObject o : fdToObject.values()) {
					if (o.getParentFD() == PObject.INVALID_FD) continue;
		
					PNode on = o.getLatestVersion() == 0 ? o.getNode(0) : o.getNode(1);
					boolean found = false;
					
					
					// First, check whether we already have a control flow edge
					
					for (PEdge e : on.getOutgoingEdges()) {
						if (e.getType() == PEdge.Type.CONTROL) {
							found = true;
							break;
						}
					}
					
					if (found) continue;
					
					
					// Find the appropriate parent node
					
					PObject parent = fdToObject.get(o.getParentFD());
					
					if (parent != null) {
						double start = o.getFirstFreezeTime();
						
						if (parent.getLatestVersion() == 0) {
							addEdge(new PEdge(on, getNode(parent, 0), PEdge.Type.CONTROL));
							found = true;
							continue;
						}
						
						for (int i = 1; i <= parent.getLatestVersion(); i++) {
							if (parent.getNode(i) == null) continue;
							if (parent.getNode(i).getFreezeTime() > start || i == parent.getLatestVersion()) {
								addEdge(new PEdge(on, getNode(parent, i), PEdge.Type.CONTROL));
								found = true;
								break;
							}
						}
					}
					
					if (!found) {
						String w = "Warning: Was not able to find parent of [" + o.getFD() + "]" + o.getName() + " : [" + o.getParentFD() + "]";
						if (parent == null) w += " (not in the object map)"; else w += parent.getName(); 
						System.err.println(w);
					}
				}
			}
			
			
			// Add version edges
			
			for (PObject o : fdToObject.values()) {
				
				for (int i = 1; i <= o.getLatestVersion(); i++) {
					PNode n = o.getVersions().get(i);
					if (n == null) continue;
					
					for (int j = i + 1; j <= o.getLatestVersion(); j++) {
						PNode m = o.getVersions().get(j);
						if (m == null) continue;
						
						// Add the version edge if it does not already exists
						
						boolean found = false;
						for (PEdge e : m.getOutgoingEdges()) {
							if (e.getType() == PEdge.Type.VERSION && e.getTo() == n) {
								found = true;
								break;
							}
						}
						if (!found) {
							PEdge e = new PEdge(m, n, PEdge.Type.VERSION);
							addEdge(e);
						}
						break;
					}
				}
			}
			
			
			// Hide lonely nodes with version 0
			
			for (PNode n : getNodes()) {
				if (n.getIncomingEdges().isEmpty() && n.getOutgoingEdges().isEmpty() && n.getVersion() == 0) {
					n.setVisible(false);
				}
			}
		}
		
		
		/**
		 * Set the metadata that should be associated with the graph
		 * 
		 * @param meta the new metadata
		 */
		public void setMeta(PMeta meta) {
			PGraph.this.setMeta(meta);
		}
		
		
		/**
		 * Get the metadata associated with the graph
		 * 
		 * @return the metadata
		 */
		public PMeta getMeta() {
			return meta;
		}
	}


	/**
	 * Write the summary node recursively to XML
	 * 
	 * @param hd the XML output
	 * @param summary the summary node
	 * @throws SAXException on error
	 */
	public void writeSummaryNodeToXML(TransformerHandler hd, PSummaryNode summary) throws SAXException {

		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		attrs.addAttribute("", "", "id", "CDATA", "" + summary.getID());
		attrs.addAttribute("", "", "index", "CDATA", "" + summary.getIndex());
		if (!summary.isVisible()) attrs.addAttribute("", "", "visible", "CDATA", "" + summary.isVisible());
		hd.startElement("", "", "summary-node", attrs);
		
		attrs.clear();
		String s = summary.getLabel();
		hd.startElement("", "", "label", attrs);
		hd.characters(s.toCharArray(), 0, s.length());
		hd.endElement("", "", "label");
		
		for (BaseNode n : summary.getBaseChildren()) {
			
			if (n instanceof PSummaryNode) {
				writeSummaryNodeToXML(hd, Utils.<PSummaryNode>cast(n));
			}
			else {
				attrs.clear();
				attrs.addAttribute("", "", "index", "CDATA", "" + n.getIndex());
				hd.startElement("", "", "node-xref", attrs);
				hd.endElement("", "", "node-xref");
			}
		}
		
		hd.endElement("", "", "summary-node");
	}
	

	/**
	 * Write the graph to XML
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {

		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		attrs.addAttribute("", "", "description", "CDATA", description);
		if (hasProvRank) attrs.addAttribute("", "", "has-provrank", "CDATA", "" + hasProvRank);
		hd.startElement("", "", DOM_ELEMENT, attrs);
		
		
		// Write the metadata
		
		if (meta != null) meta.writeToXML(hd);
		
		
		// Write the objects
		
		attrs.clear();
		hd.startElement("", "", PObject.DOM_ELEMENT + "s", attrs);
		
		for (PObject o : fdToObject.values()) {
			o.writeToXML(hd);
		}
		
		hd.endElement("", "", PObject.DOM_ELEMENT + "s");
		
		
		// Write the edges
		
		attrs.clear();
		hd.startElement("", "", PEdge.DOM_ELEMENT + "s", attrs);
		
		for (PEdge e : getEdges()) {
			e.writeToXML(hd);
		}
		
		hd.endElement("", "", PEdge.DOM_ELEMENT + "s");
		
		
		// Write the summary nodes
		
		if (getRootSummaryNode() != null) {
			writeSummaryNodeToXML(hd, getRootSummaryNode());
		}
		
		
		// Write layouts
		
		attrs.clear();
		hd.startElement("", "", "layouts", attrs);
		
		GraphLayout defaultLayout = getDefaultLayout(); 
		for (GraphLayout l : getLayouts()) {
			attrs.clear();
			attrs.addAttribute("", "", "description", "CDATA", l.getDescription());
			if (l == defaultLayout) attrs.addAttribute("", "", "default", "CDATA", "" + (l == defaultLayout));
			hd.startElement("", "", "layout", attrs);
			
			attrs.clear();
			attrs.addAttribute("", "", "class", "CDATA", l.getAlgorithm().getClass().getCanonicalName());
			hd.startElement("", "", "algorithm", attrs);
			
			if (!(l.getAlgorithm() instanceof XMLSerializable)) {
				throw new SAXException(l.getAlgorithm().getClass().getCanonicalName() + " is not XMLSerializable");
			}
			((XMLSerializable) l.getAlgorithm()).writeToXML(hd);
			
			hd.endElement("", "", "algorithm");
			
			hd.endElement("", "", "layout");
		}
		
		hd.endElement("", "", "layouts");
		
		
		// Finish
		
		hd.endElement("", "", DOM_ELEMENT);
	}
	
	
	/**
	 * Return SAX XML parser that would create the document
	 * 
	 * @return the pair of a result pointer and a XML parser handler
	 */
	public static Pair<Pointer<PGraph>, DefaultHandler> createSAXParserHandler() {
		
		Pointer<PGraph> pr = new Pointer<PGraph>();
		PGraphParser parser = new PGraphParser();
		parser.resultPointer = pr;
		
		return new Pair<Pointer<PGraph>, DefaultHandler>(pr, parser);
	}
	
	
	/**
	 * SAX graph parser
	 */
	private static class PGraphParser extends DefaultHandler {
		
		public Pointer<PGraph> resultPointer;
		
		private int depth = 0;
		private String depth1qName = null;
		private String depth2qName = null;
		private String lastqName = null;
		private String tmp = "";
		
		private SAX2DOMHandler delegate = null;
		
		private PGraph graph = null; 
		private Stack<PSummaryNode> summaryNodes = new Stack<PSummaryNode>();
		private boolean layoutDefault = false;
		//private String layoutDescription = null;
		private String layoutAlgorithmClass = null;
	
		
		/**
		 * Start new element
		 * 
		 * @param uri the URI
		 * @param localName the element's local name
		 * @param qName the element's fully qualified name
		 * @param attributes the attributes
		 * @throws SAXException on error
		 */
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			
			// Depth 0: Document
			
			if (depth == 0) {
				if (!qName.equals(DOM_ELEMENT)) {
					throw new SAXException("Expected <" + DOM_ELEMENT + ">, found <" + qName + ">");
				}
				
				graph = new PGraph();
				graph.description = XMLUtils.getAttribute(attributes, "description");
				graph.hasProvRank = Boolean.parseBoolean(XMLUtils.getAttribute(attributes, "has-provrank", "false"));
				
				resultPointer.set(graph);
			}
			
			
			// Depth 1: Metadata, objects, edges, or a summary node
			
			if (depth == 1) {
				
				depth1qName = qName;
				
				if (qName.equals(PMeta.DOM_ELEMENT)) {
					delegate = new SAX2DOMHandler();
				}
				
				else if (qName.equals(PObject.DOM_ELEMENT + "s")) {
				}
				
				else if (qName.equals(PEdge.DOM_ELEMENT + "s")) {
				}
				
				else if (qName.equals("summary-node")) {
				}
				
				else if (qName.equals("layouts")) {
				}
				
				else {
					throw new SAXException("Unexpected <" + qName + "> inside <" + DOM_ELEMENT + ">");
				}
 
				
				if (delegate != null) {
					delegate.startElement(uri, localName, qName, attributes);
				}
			}
			
			
			// Depth 2: Object, edge, summary node, layout, or inside a delegate
			
			if (depth == 2) {
				
				depth2qName = qName;
				
				if (depth1qName.equals(PMeta.DOM_ELEMENT)) {
					// Nothing to do, the delegate already exists
				}
				
				else if (depth1qName.equals(PObject.DOM_ELEMENT + "s") && qName.equals(PObject.DOM_ELEMENT)) {
					delegate = new SAX2DOMHandler();
				}
				
				else if (depth1qName.equals(PEdge.DOM_ELEMENT + "s") && qName.equals(PEdge.DOM_ELEMENT)) {
					delegate = new SAX2DOMHandler();
				}
				
				else if (depth1qName.equals("summary-node") && (qName.equals("summary-node") || qName.equals("node-xref") || qName.equals("label"))) {
				}
				
				else if (depth1qName.equals("layouts") && qName.equals("layout")) {
					layoutDefault = Boolean.parseBoolean(XMLUtils.getAttribute(attributes, "default", "false"));
					// NOTE Layout description is currently automatically generated by the graph layout algorithm
					// layoutDescription = XMLUtils.getAttribute(attributes, "description");
				}
				
				else {
					throw new SAXException("Unexpected <" + qName + "> inside <" + depth1qName + ">");
				}
				
				
				if (delegate != null) {
					delegate.startElement(uri, localName, qName, attributes);
				}
			}
			
			
			// Depth 3: Contents of the graph layout, or inside a delegate
			
			if (depth == 3) {
				
				boolean skipDelegate = false;
				
				if (depth1qName.equals("layouts") && depth2qName.equals("layout")) {
					
					if (qName.equals("algorithm")) {
						delegate = new SAX2DOMHandler();
						layoutAlgorithmClass = XMLUtils.getAttribute(attributes, "class");
						skipDelegate = true;
					}
					
					else {
						throw new SAXException("Unexpected <" + qName + "> inside <" + depth2qName + ">");
					}
				}
				
				if (delegate != null && !skipDelegate) {
					delegate.startElement(uri, localName, qName, attributes);
				}
			}
			
			
			// Depth 4+: Inside a delegate
			
			if (depth >= 4) {
				if (delegate != null) {
					delegate.startElement(uri, localName, qName, attributes);
				}
			}
			
			
			// Summary nodes
			
			if (depth >= 1 && depth1qName.equals("summary-node")) {
				
				PSummaryNode parent = summaryNodes.isEmpty() ? null : summaryNodes.peek();
				
				if (qName.equals("summary-node")) {
					
					int id = Integer.parseInt(XMLUtils.getAttribute(attributes, "id"));
					boolean visible = Boolean.parseBoolean(XMLUtils.getAttribute(attributes, "visible", "true"));
					// int index = Integer.parseInt(XMLUtils.getAttribute(attributes, "index"));
					// TODO Make sure the newly created node has the specified index
					//      Not necessary at this point, but it would be necessary if we store the graph layout explicitly
					
					PSummaryNode s;
					if (parent == null) {
						graph.summarizationBegin();
						s = graph.getRootSummaryNode();
					}
					else {
						s = (PSummaryNode) graph.newSummaryNode(parent);
					}
					s.setID(id);
					s.setVisible(visible);
					
					summaryNodes.push(s);
				}
				
				else if (qName.equals("node-xref")) {
					
					int index = Integer.parseInt(XMLUtils.getAttribute(attributes, "index"));
					parent.moveNodeFromAncestor(graph.getNode(index));
				}
			}
			
			
			// Update depth
			
			depth++;
			lastqName = qName;
			tmp = "";
		}
		

		/**
		 * Characters
		 * 
		 * @param ch the array of characters
		 * @param start the start within the array
		 * @param length the length of the string 
		 * @throws SAXException on error
		 */
		public void characters(char[] ch, int start, int length) throws SAXException {
			
			boolean skipDelegate = false;
			
			
			// Summary nodes
			
			if (depth > 1 && depth1qName.equals("summary-node")) {
				
				if (lastqName.equals("label")) {
					tmp += new String(ch, start, length);
				}
			}
			
			
			// Graph layout algorithm
			
			if (depth - 1 == 3) {
				
				if (depth1qName.equals("layouts") && depth2qName.equals("layout")) {
					if (lastqName.equals("algorithm")) {
						skipDelegate = true;
					}
				}
			}
			
			
			// Delegate
			
			if (delegate != null && !skipDelegate) {
				delegate.characters(ch, start, length);
			}
		}
		
		
		/**
		 * Finish an element
		 * 
		 * @param uri the URI
		 * @param localName the element's local name
		 * @param qName the element's fully qualified name
		 * @param attributes the attributes
		 * @throws SAXException on error
		 */
		public void endElement(String uri, String localName, String qName) throws SAXException {
			
			depth--;
			
			
			// Depth 1: Metadata, objects, edges, or a summary node
			
			if (depth == 1) {
				if (delegate != null) {
					delegate.endElement(uri, localName, qName);
				}
				
				try {
					if (qName.equals(PMeta.DOM_ELEMENT)) {
						Element el = delegate.getDocument().getDocumentElement();
						PMeta meta = PMeta.loadFromXML(el);
						graph.setMeta(meta);
					}
					
					if (qName.equals("summary-node")) {
						graph.summarizationEnd();
					}
				}
				catch (ParserException e) {
					throw new SAXException(e);
				}
				
				delegate = null;
			}
			
			
			// Depth 2: Object, edge, summary node, layout, or inside a delegate
			
			if (depth == 2) {
				if (delegate != null) {
					delegate.endElement(uri, localName, qName);
				}
				
				try {
					if (depth1qName.equals(PObject.DOM_ELEMENT + "s") && qName.equals(PObject.DOM_ELEMENT)) {
						Element el = delegate.getDocument().getDocumentElement();
						PObject o = PObject.loadFromXML(graph, el);
						for (PNode pn : o.getVersions()) {
							if (pn != null) graph.addNodeExt(pn);
						}
						delegate = null;
					}
					
					else if (depth1qName.equals(PEdge.DOM_ELEMENT + "s") && qName.equals(PEdge.DOM_ELEMENT)) {
						Element el = delegate.getDocument().getDocumentElement();
						PEdge e = PEdge.loadFromXML(graph, el);
						graph.addEdgeExt(e);
						delegate = null;
					}
					
					else if (depth1qName.equals("layouts") && qName.equals("layout")) {
						// The layout was created when the <algorithm> element ended
					}
				}
				catch (ParserException e) {
					throw new SAXException(e);
				}
			}
			
			
			// Depth 3: Contents of the graph layout, or inside a delegate
			
			if (depth == 3) {
				
				boolean skipDelegate = false;
				
				// Graph layout algorithm
				
				if (depth1qName.equals("layouts") && depth2qName.equals("layout")) {
					if (qName.equals("algorithm")) {
						skipDelegate = true;
					}
				}
				
				if (delegate != null && !skipDelegate) {
					delegate.endElement(uri, localName, qName);
				}
				
				try {
					if (depth1qName.equals("layouts") && depth2qName.equals("layout")) {
						
						if (qName.equals("algorithm")) {
							
							Class<?> c = Class.forName(layoutAlgorithmClass);
							Object o = c.newInstance();
							if (!(o instanceof GraphLayoutAlgorithm)) {
								throw new SAXException(layoutAlgorithmClass + " is not a GraphLayoutAlgorithm");
							}
							GraphLayoutAlgorithm l = (GraphLayoutAlgorithm) o;
							
							if (!(l instanceof XMLSerializable)) {
								throw new SAXException(layoutAlgorithmClass + " is not XMLSerializable");
							}
							((XMLSerializable) l).loadFromXML(delegate.getDocument().getDocumentElement());
							
							GraphLayout layout = l.initializeLayout(graph, l.isZoomOptimized() ? 2 : -1, null);
							GraphLayout dl = graph.getDefaultLayout();
							graph.addLayout(layout);
							if (layoutDefault || dl == null) dl = layout;
							graph.setDefaultLayout(dl);
							
							delegate = null;
						}
						
						else {
							throw new SAXException("Unexpected <" + qName + "> inside <" + depth2qName + ">");
						}
					}
				}
				catch (SAXException e) {
					throw e;
				}
				catch (Exception e) {
					throw new SAXException(e);
				}
			}
			
			
			// Depth 4+: Inside a delegate
			
			if (depth >= 4) {
				if (delegate != null) {
					delegate.endElement(uri, localName, qName);
				}
			}
			
			
			// Summary nodes
			
			if (depth >= 1 && depth1qName.equals("summary-node")) {
				
				if (qName.equals("summary-node")) {
					summaryNodes.pop();
				}

				if (qName.equals("label")) {
					PSummaryNode s = summaryNodes.peek();
					s.setLabel(tmp);
				}
			}
		}
	}
}
