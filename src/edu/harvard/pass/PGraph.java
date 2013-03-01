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

import edu.harvard.pass.filter.AncestryFilter;
import edu.harvard.pass.job.ProvRankJob;
import edu.harvard.pass.job.SubRankJob;
import edu.harvard.pass.parser.ParserHandler;
import edu.harvard.pass.parser.RDFParser;
import edu.harvard.util.filter.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.graph.algorithm.BetweennessCentrality;
import edu.harvard.util.graph.algorithm.DangalchevClosenessCentrality;
import edu.harvard.util.graph.layout.FastGraphLayout;
import edu.harvard.util.graph.layout.GraphLayout;
import edu.harvard.util.graph.layout.GraphLayoutAlgorithm;
import edu.harvard.util.graph.layout.GraphLayoutEdge;
import edu.harvard.util.graph.layout.GraphLayoutNode;
import edu.harvard.util.gui.JobMasterDialog;
import edu.harvard.util.job.*;
import edu.harvard.util.*;
import edu.harvard.util.XMLUtils;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import javax.xml.transform.sax.TransformerHandler;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;
import org.openrdf.rio.turtle.TurtleWriter;
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
	
	private static final boolean ENABLE_FORKPARENT_FIX = false;
	
	protected HashMap<Integer, PObject> fdToObject;
	protected HashMap<String, PNode> idToNode;
	protected int lastNegativeFD;
	protected boolean fixForkparentEdges;

	protected PGraphStat stat;
	protected PMeta meta;
	
	protected PGraph parent;
	protected TreeMap<String, PGraph> derivedGraphs;
	protected String description;
	
	protected boolean hasSubRank;
	protected boolean hasProvRank;
	
	private ParserHandler parserHandler;
	
	
	/**
	 * The graph type
	 */
	public enum Type {
		GENERIC, LINEAGE_QUERY, COMPARISON
	}
	
	protected Type type;
	protected AncestryFilter lineageQuery;
	protected Vector<Pair<Set<String>, String>> comparisonNodeSets;

	
	/**
	 * Constructor for objects of type PGraph
	 */
	public PGraph() {
		
		this(new PMeta());
	}
	
	
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
	 * Get the graph type
	 * 
	 * @return the graph type
	 */
	public Type getType() {
		return type;
	}
	
	
	/**
	 * Get the ancestry filter associated with the lineage query
	 * 
	 * @return the ancestry filter, or null if none
	 */
	public AncestryFilter getLineageQueryAncestryFilter() {
		return type == Type.LINEAGE_QUERY ? lineageQuery : null;
	}
	
	
	/**
	 * Get the comparison node sets
	 * 
	 * @return the collection of description/node-set ID pairs
	 */
	public Vector<Pair<Set<String>, String>> getComparisonNodeSets() {
		return comparisonNodeSets;
	}
	
	
	/**
	 * Set a comparison node set
	 * 
	 * @param index the index
	 * @param set the node ID set
	 * @param description the node set description 
	 */
	public void setComparisonNodeSet(int index, Set<String> set, String description) {
		if (index < 0 || index >= 2) throw new IllegalArgumentException("index out of range");
		for (int i = comparisonNodeSets.size(); i <= index; i++) comparisonNodeSets.add(null);
		comparisonNodeSets.set(index, new Pair<Set<String>, String>(set, description));
	}
	
	
	/**
	 * Derive a new PGraph that is a result of a lineage query
	 * 
	 * @param ancestryFilter the query ancestry filter
	 * @return the new graph
	 */
	public PGraph lineageQuery(AncestryFilter ancestryFilter) {
		
		String name = null;
		
		if (ancestryFilter instanceof AncestryFilter.Ancestors) {
			name = "Ancestors of " + ancestryFilter.getPNode().getPublicID();
		}
		else if (ancestryFilter instanceof AncestryFilter.Descendants) {
			name = "Descendants of " + ancestryFilter.getPNode().getPublicID();
		}
		else {
			throw new IllegalArgumentException();
		}
		
		PGraph p = createSummaryGraph(ancestryFilter, name, true);
		p.type = Type.LINEAGE_QUERY;
		p.lineageQuery = ancestryFilter;
		
		return p;
	}
	
	
	/**
	 * Derive a comparison graph from the currently configured comparison node-sets
	 * 
	 * @return the new PGraph
	 */
	public PGraph createComparison() {
		
		if (comparisonNodeSets.size() != 2
				|| comparisonNodeSets.get(0) == null
				|| comparisonNodeSets.get(1) == null) {
			throw new IllegalStateException("The comparison node-sets are not set");
		}
		
		HashSet<PNode> set = new HashSet<PNode>();
		for (Pair<Set<String>, String> x : comparisonNodeSets) {
			for (String s : x.getFirst()) set.add(idToNode.get(s));
		}
		
		SetFilter<PNode> filter = new SetFilter<PNode>("Union of the Node-Sets");
		filter.set(set);
		
		PGraph p = createSummaryGraph(filter, "Comparison", false);
		p.type = Type.COMPARISON;
		
		p.comparisonNodeSets = new Vector<Pair<Set<String>, String>>();
		for (Pair<Set<String>, String> x : comparisonNodeSets) {
			HashSet<String> clonedSet = new HashSet<String>();
			clonedSet.addAll(x.getFirst());
			p.comparisonNodeSets.add(new Pair<Set<String>, String>(clonedSet, x.getSecond()));
		}
		
		return p;
	}
	
	
	/**
	 * Derive a difference graph from the currently configured comparison node-sets
	 * 
	 * @return the new PGraph
	 */
	public PGraph createDifference() {
		
		if (comparisonNodeSets.size() != 2
				|| comparisonNodeSets.get(0) == null
				|| comparisonNodeSets.get(1) == null) {
			throw new IllegalStateException("The comparison node-sets are not set");
		}
		
		HashSet<String> set1 = new HashSet<String>();
		set1.addAll(comparisonNodeSets.get(0).getFirst());
		set1.removeAll(comparisonNodeSets.get(1).getFirst());
		
		HashSet<String> set2 = new HashSet<String>();
		set2.addAll(comparisonNodeSets.get(1).getFirst());
		set2.removeAll(comparisonNodeSets.get(0).getFirst());
		
		HashSet<PNode> set = new HashSet<PNode>();
		for (String s : set1) set.add(idToNode.get(s));
		for (String s : set2) set.add(idToNode.get(s));
		
		SetFilter<PNode> filter = new SetFilter<PNode>("XOR of the Node-Sets");
		filter.set(set);
		
		PGraph p = createSummaryGraph(filter, "Difference", false);
		p.type = Type.COMPARISON;
		
		p.comparisonNodeSets = new Vector<Pair<Set<String>, String>>();
		for (Pair<Set<String>, String> x : comparisonNodeSets) {
			HashSet<String> clonedSet = new HashSet<String>();
			clonedSet.addAll(x.getFirst());
			p.comparisonNodeSets.add(new Pair<Set<String>, String>(clonedSet, x.getSecond()));
		}
		
		return p;
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
		
		type = Type.GENERIC;
		lineageQuery = null;
		comparisonNodeSets = new Vector<Pair<Set<String>, String>>();
		comparisonNodeSets.add(null);
		comparisonNodeSets.add(null);
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
	 * Determine whether the SubRank has been computed
	 * 
	 * @return true if it is computed
	 */
	public boolean wasSubRankComputed() {
		return hasSubRank;
	}
	
	
	/**
	 * Set whether the SubRank has been computed
	 * 
	 * @param v true if SubRank was computed
	 */
	public void setWasSubRankComputed(boolean v) {
		hasSubRank = v;
	}
	
	
	/**
	 * Require that SubRank has been computed (compute if necessary)
	 * 
	 * @return true if it was just computed
	 */
	public boolean requireSubRank() {
		
		if (hasSubRank) return false;
		
		computeSubRank();
		return true;
	}
	
	
	/**
	 * Compute SubRank, recomputing it if it already has been computed
	 */
	public void computeSubRank() {
		JobMasterDialog j = new JobMasterDialog(null, "SubRank");
		j.add(new SubRankJob(new Pointer<PGraph>(this)));
		
		try {
			j.run();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (!hasSubRank) throw new RuntimeException("SubRank has not been computed");
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
		
		computeProvRank();
		return true;
	}
	
	
	/**
	 * Compute ProvRank, recomputing it if it already has been computed
	 */
	public void computeProvRank() {
		JobMasterDialog j = new JobMasterDialog(null, "ProvRank");
		j.add(new ProvRankJob(new Pointer<PGraph>(this)));
		
		try {
			j.run();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (!hasProvRank) throw new RuntimeException("ProvRank has not been computed");
	}
	
	
	/**
	 * Require that Dangalchev's closeness centrality has been computed (compute if necessary)
	 * 
	 * @param direction the graph direction
	 * @return true if it was just computed
	 */
	public boolean requireDangalchevClosenessCentrality(GraphDirection direction) {
		
		DangalchevClosenessCentrality centrality = new DangalchevClosenessCentrality(this, direction);
		
		if (getPerNodeAttributeOverlay(centrality.getAttributeName()) != null) {
			return false;
		}
		
		JobMasterDialog j = new JobMasterDialog(null, centrality.getAttributeName());
		j.add(centrality.createJob());
		
		try {
			j.run();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (getPerNodeAttributeOverlay(centrality.getAttributeName()) == null) {
			throw new InternalError();
		}
		
		return true;
	}
	
	
	/**
	 * Require that Betweenness Centrality has been computed (compute if necessary)
	 * 
	 * @param direction the graph direction
	 * @return true if it was just computed
	 */
	public boolean requireBetweennessCentrality(GraphDirection direction) {
		
		BetweennessCentrality centrality = new BetweennessCentrality(this, direction);
		
		if (getPerNodeAttributeOverlay(centrality.getAttributeName()) != null) {
			return false;
		}
		
		JobMasterDialog j = new JobMasterDialog(null, centrality.getAttributeName());
		j.add(centrality.createJob());
		
		try {
			j.run();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (getPerNodeAttributeOverlay(centrality.getAttributeName()) == null) {
			throw new InternalError();
		}
		
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
		
		updateSubRankStatisticsForNode(node);
		updateProvRankStatisticsForNode(node);
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
	 * Get the object by the FD
	 * 
	 * @param fd the object ID
	 * @return the object
	 * @throws NoSuchElementException if not found
	 */
	public PObject getObject(int fd) {
		PObject object = fdToObject.get(fd);
		if (object == null) throw new NoSuchElementException();
		return object;
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
	 * Update SubRank statistics with the given node
	 * 
	 * @param n the node
	 */
	protected void updateSubRankStatisticsForNode(PNode n) {

		double f = n.getSubRank();
		if (f > 0 && n.getObject().getName() != null) {
			if (f < stat.subRankMin) stat.subRankMin = f;
			if (f > stat.subRankMax) stat.subRankMax = f;
			
			double sumLogJumpDelta = 0;
			int count = 0;
			
			for (PEdge e : n.getOutgoingEdges()) {
				PNode m = e.getTo();
				double g = m.getSubRank();
				
				if (g > 0 && m.getObject().getName() != null) {
					double d = g - f;
					if (d < stat.subRankJumpMin) stat.subRankJumpMin = d;
					if (d > stat.subRankJumpMax) stat.subRankJumpMax = d;
					d = Math.log(g) - Math.log(f);
					if (d < stat.subRankLogJumpMin) stat.subRankLogJumpMin = d;
					if (d > stat.subRankLogJumpMax) stat.subRankLogJumpMax = d;
					sumLogJumpDelta += d; count++;
				}
			}
			
			if (count > 0) {
				double mean = sumLogJumpDelta / count;
				if (mean < stat.subRankMeanLogJumpMin) stat.subRankMeanLogJumpMin = mean;
				if (mean > stat.subRankMeanLogJumpMax) stat.subRankMeanLogJumpMax = mean;
			}
		}
	}

	
	/**
	 * Recompute SubRank statistics
	 */
	public void updateSubRankStatistics() {
		
		stat.subRankMin = Double.MAX_VALUE;
		stat.subRankMax = Double.MIN_VALUE;
		stat.subRankJumpMin = Double.MAX_VALUE;
		stat.subRankJumpMax = Double.MIN_VALUE;
		stat.subRankLogJumpMin = Double.MAX_VALUE;
		stat.subRankLogJumpMax = Double.MIN_VALUE;
		stat.subRankMeanLogJumpMin = Double.MAX_VALUE;
		stat.subRankMeanLogJumpMax = Double.MIN_VALUE;

		for (PNode n : getNodes()) {
			updateSubRankStatisticsForNode(n);
		}
	}

	
	/**
	 * Update ProvRank statistics with the given node
	 * 
	 * @param n the node
	 */
	protected void updateProvRankStatisticsForNode(PNode n) {

		double f = n.getProvRank();
		if (f > 0 && n.getObject().getName() != null) {
			if (f < stat.provRankMin) stat.provRankMin = f;
			if (f > stat.provRankMax) stat.provRankMax = f;
			
			double sumLogJumpDelta = 0;
			int count = 0;
			
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
					sumLogJumpDelta += d; count++;
				}
			}
			
			if (count > 0) {
				double mean = sumLogJumpDelta / count;
				if (mean < stat.provRankMeanLogJumpMin) stat.provRankMeanLogJumpMin = mean;
				if (mean > stat.provRankMeanLogJumpMax) stat.provRankMeanLogJumpMax = mean;
			}
		}
	}

	
	/**
	 * Recompute ProvRank statistics
	 */
	public void updateProvRankStatistics() {
		
		stat.provRankMin = Double.MAX_VALUE;
		stat.provRankMax = Double.MIN_VALUE;
		stat.provRankJumpMin = Double.MAX_VALUE;
		stat.provRankJumpMax = Double.MIN_VALUE;
		stat.provRankLogJumpMin = Double.MAX_VALUE;
		stat.provRankLogJumpMax = Double.MIN_VALUE;
		stat.provRankMeanLogJumpMin = Double.MAX_VALUE;
		stat.provRankMeanLogJumpMax = Double.MIN_VALUE;

		for (PNode n : getNodes()) {
			updateProvRankStatisticsForNode(n);
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

		if (ext.equals("dot") || ext.equals("gv") || ext.equals("txt")) {
			exportGraphviz(file, observer);
			return;
		}


		// RDF

		if (RDFFormat.forFileName(file.getName()) != null) {
			exportRDF(file, observer);
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
		for (PNode n : getNodes()) {
			if (!n.isVisible()) continue;
			String label = Utils.escapeSimple(n.getLabel());
			out.println("  " + n.getIndex() + " [label=\"" + label + "\"];");
		}
		for (PEdge e : getEdges()) {
			if (!e.getFrom().isVisible() || !e.getTo().isVisible()) continue;
			out.println("  " + e.getFrom().getIndex() + " -> " + e.getTo().getIndex());
		}
		out.println("}");

		out.close();
	}


	/**
	 * Export to the RDF format
	 *
	 * @param file the file
	 * @param observer the job progress observer
	 * @throws IOException if an error occurred
	 */
	public void exportRDF(File file, JobObserver observer) throws IOException {
		
		// Get the writer 

		FileWriter out = new FileWriter(file);

		RDFFormat format = RDFFormat.forFileName(file.getName());
		RDFWriter writer = null;
		
		if (format == RDFFormat.N3) writer = new N3Writer(out);
		if (format == RDFFormat.NTRIPLES) writer = new NTriplesWriter(out);
		if (format == RDFFormat.RDFXML) writer = new RDFXMLPrettyWriter(out);
		if (format == RDFFormat.TRIG) writer = new TriGWriter(out);
		if (format == RDFFormat.TRIX) writer = new TriXWriter(out);
		if (format == RDFFormat.TURTLE) writer = new TurtleWriter(out);
		
		if (writer == null) {
			out.close();
			throw new IllegalArgumentException();
		}

		try {
			writer.startRDF();
		}
		catch (RDFHandlerException e) {
			throw new IOException(e);
		}
		
		
		// Write the RDF document
		
		try {
			
			ValueFactory valueFactory = new ValueFactoryImpl();
			URI namePredicate = valueFactory.createURI(RDFParser.DEFAULT_URI, "NAME");
			URI typePredicate = valueFactory.createURI(RDFParser.DEFAULT_URI, "TYPE");
			URI freezeTimePredicate = valueFactory.createURI(RDFParser.DEFAULT_URI, "FREEZETIME");
			
			for (PNode n : getNodes()) {
				Resource subject = valueFactory.createURI(RDFParser.DEFAULT_URI, n.getPublicID());
				
				if (n.getVersion() == 0) {
					if (n.getObject().getName() != null) {
						writer.handleStatement(new StatementImpl(subject, namePredicate,
								valueFactory.createLiteral(n.getObject().getName())));
					}
					if (n.getObject().getExtendedType() != null) {
						writer.handleStatement(new StatementImpl(subject, typePredicate,
								valueFactory.createLiteral(n.getObject().getExtendedType())));
					}
					else {
						writer.handleStatement(new StatementImpl(subject, typePredicate,
								valueFactory.createLiteral(n.getObject().getType().toString())));
					}
					for (Entry<String, String> a : n.getObject().getExtendedAttributes().entrySet()) {
						writer.handleStatement(new StatementImpl(subject,
								valueFactory.createURI(RDFParser.DEFAULT_URI, a.getKey()),
								valueFactory.createLiteral(a.getValue())));
					}
				}
				
				for (Entry<String, String> a : n.getExtendedAttributes().entrySet()) {
					writer.handleStatement(new StatementImpl(subject,
							valueFactory.createURI(RDFParser.DEFAULT_URI, a.getKey()),
							valueFactory.createLiteral(a.getValue())));
				}
				
				if (n.getFreezeTime() >= 0) {
					writer.handleStatement(new StatementImpl(subject, freezeTimePredicate,
							valueFactory.createLiteral(n.getFreezeTime())));
				}
			}
			
			for (PEdge e : getEdges()) {
				if (e.getType() == PEdge.Type.VERSION) {
					if (e.getFrom().getVersion() == e.getTo().getVersion() + 1) {
						continue;
					}
				}
				
				Resource subject = valueFactory.createURI(RDFParser.DEFAULT_URI, e.getFrom().getPublicID());
				Resource object  = valueFactory.createURI(RDFParser.DEFAULT_URI, e.getTo().getPublicID());
				
				URI predicate;
				if (e.getLabel() != null && !"".equals(e.getLabel())) {
					predicate = valueFactory.createURI(RDFParser.DEFAULT_URI, e.getLabel());
				}
				else {
					predicate = valueFactory.createURI(RDFParser.DEFAULT_URI, e.getType().toString());
				}
				
				writer.handleStatement(new StatementImpl(subject, predicate, object));
			}
		}
		catch (RDFHandlerException e) {
			throw new IOException(e);
		}

		
		// Finish

		try {
			writer.endRDF();
			out.close();
		}
		catch (RDFHandlerException e) {
			throw new IOException(e);
		}
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
		
		if (type == Type.LINEAGE_QUERY) {
			
			// Export the nodes in the BFS order from the root
			
			boolean outgoing = lineageQuery instanceof AncestryFilter.Ancestors;
			
			LinkedList<PNode> q = new LinkedList<PNode>();
			HashSet<PNode> visited = new HashSet<PNode>();
			q.add(lineageQuery.getPNode());
			visited.add(q.getFirst());
			
			while (!q.isEmpty()) {
				PNode n = q.removeFirst();
				if (n.isVisible()) out.println(n.toCSV());
				for (PEdge e : (outgoing ? n.getOutgoingEdges() : n.getIncomingEdges())) {
					PNode m = e.getOther(n);
					if (visited.add(m)) q.addLast(m);
				}
			}
		}
		else {
			
			// Otherwise export the nodes in an arbitrary order 
			
			for (PNode n : getNodes()) {
				if (!n.isVisible()) continue;
				out.println(n.toCSV());
			}
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
	 * @param nodes the selected nodes
	 * @param visited the set of visited nodes
	 * @param graph the result graph that is being constructed
	 * @param connected true if the nodes are connected
	 * @return the number of edges added to the result set
	 */
	private int createSummaryGraphHelper(PNode node, PNode start, Set<PNode> nodes,
			Set<PNode> visited, PGraph graph, boolean connected) {
		
		visited.add(node);
		
		int count  = 0;
		
		for (PEdge e : node.getOutgoingEdges()) {
			PNode target = e.getTo();
			
			if (nodes.contains(target)) {
				visited.add(target);
				PEdge.Type type = node == start ? e.getType() : PEdge.Type.COMPOUND;
				graph.addEdge(new PEdge(graph.getNodeByID(start.getID()), graph.getNodeByID(target.getID()), type));
				count++;
			}
			else if (!connected) {
				if (visited.contains(target)) continue;
				count += createSummaryGraphHelper(e.getTo(), start, nodes, visited, graph, connected);
			}
		}
		
		return count;
	}
	
	
	/**
	 * Create a summary graph with coarser granularity
	 * 
	 * @param filter the filter that accepts the nodes to include in the result
	 * @param description a short description or name of the summary graph
	 * @param connected true if the nodes are connected
	 * @return the graph with only the filtered nodes 
	 */
	public PGraph createSummaryGraph(Filter<PNode> filter, String description, boolean connected) {
		
		// Initialize
		
		PGraph graph = new PGraph();
		HashSet<PNode> accepted = new HashSet<PNode>();
		
		
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
			createSummaryGraphHelper(n, n, accepted, visited, graph, connected);
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
		return createSummaryGraph(filter, filter.toExpressionString(), false);
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

			if (ENABLE_FORKPARENT_FIX) {
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
			
			// Algorithm
			
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
			
			
			// Contents
			
			attrs.clear();
			
			hd.startElement("", "", "layout-nodes", attrs);
			for (GraphLayoutNode x : l.getLayoutNodes()) {
				if (x != null) x.writeToXML(hd);
			}
			hd.endElement("", "", "layout-nodes");
			
			hd.startElement("", "", "layout-edges", attrs);
			for (GraphLayoutEdge x : l.getLayoutEdges()) {
				if (x != null) x.writeToXML(hd);
			}
			hd.endElement("", "", "layout-edges");

			
			// Finish the layout
			
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
		private String depth3qName = null;
		private String lastqName = null;
		private String tmp = "";
		
		private SAX2DOMHandler delegate = null;
		
		private PGraph graph = null; 
		private Stack<PSummaryNode> summaryNodes = new Stack<PSummaryNode>();
		private HashMap<Integer, Integer> summaryNodeIndexRemap = new HashMap<Integer, Integer>();
		
		private boolean layoutDefault = false;
		private String layoutDescription = null;
		private String layoutAlgorithmClass = null;
		private GraphLayout layout = null;
		private boolean layoutInitialized = false;
	
		
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
					layoutDescription = XMLUtils.getAttribute(attributes, "description");
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
				
				depth3qName = qName;

				boolean skipDelegate = false;
				
				if (depth1qName.equals("layouts") && depth2qName.equals("layout")) {
					
					if (qName.equals("algorithm")) {
						delegate = new SAX2DOMHandler();
						layoutAlgorithmClass = XMLUtils.getAttribute(attributes, "class");
						skipDelegate = true;
					}
					
					else if (qName.equals("layout-nodes")) {
						skipDelegate = true;
					}
					
					else if (qName.equals("layout-edges")) {
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
			
			
			// Depth 4: Layout contents, or inside a delegate
			
			if (depth == 4) {
				
				if (depth1qName.equals("layouts") && depth2qName.equals("layout") && depth3qName.equals("layout-nodes")) {
					
					if (qName.equals(GraphLayoutNode.DOM_ELEMENT)) {
						delegate = new SAX2DOMHandler();
					}

					else {
						throw new SAXException("Unexpected <" + qName + "> inside <" + depth3qName + ">");
					}
				}
				
				if (depth1qName.equals("layouts") && depth2qName.equals("layout") && depth3qName.equals("layout-edges")) {
					
					if (qName.equals(GraphLayoutEdge.DOM_ELEMENT)) {
						delegate = new SAX2DOMHandler();
					}

					else {
						throw new SAXException("Unexpected <" + qName + "> inside <" + depth3qName + ">");
					}
				}
				
				if (delegate != null) {
					delegate.startElement(uri, localName, qName, attributes);
				}
			}
			
			
			// Depth 5+: Inside a delegate
			
			if (depth >= 5) {
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
					int index = Integer.parseInt(XMLUtils.getAttribute(attributes, "index"));
					
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
					summaryNodeIndexRemap.put(index, s.getIndex());
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
						
						if (!layoutInitialized) {
							layout = layout.getAlgorithm().initializeLayout(graph,
									layout.getAlgorithm().isZoomOptimized() ? 2 : -1, null);
						}
						
						GraphLayout dl = graph.getDefaultLayout();
						graph.addLayout(layout);
						if (layoutDefault || dl == null) dl = layout;
						graph.setDefaultLayout(dl);
						
						layoutInitialized = false;
						layout = null;
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
							
							layout = new FastGraphLayout(graph, l, layoutDescription);
							layoutInitialized = false;
							
							delegate = null;
						}
						
						else if (qName.equals("layout-nodes")) {
							layoutInitialized = true;
						}
						
						else if (qName.equals("layout-edges")) {
							layoutInitialized = true;
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
			
			
			// Depth 4: Layout contents, or inside a delegate
			
			if (depth == 4) {
				if (delegate != null) {
					delegate.endElement(uri, localName, qName);
				}
				
				try {
					if (depth1qName.equals("layouts") && depth2qName.equals("layout") && depth3qName.equals("layout-nodes")) {
						
						if (qName.equals(GraphLayoutNode.DOM_ELEMENT)) {
							GraphLayoutNode n = GraphLayoutNode.loadFromXML(layout, summaryNodeIndexRemap,
									delegate.getDocument().getDocumentElement());
							if (n != null) layout.addLayoutNode(n);
							delegate = null;
						}

						else {
							throw new SAXException("Unexpected <" + qName + "> inside <" + depth3qName + ">");
						}
					}
					
					if (depth1qName.equals("layouts") && depth2qName.equals("layout") && depth3qName.equals("layout-edges")) {
						
						if (qName.equals(GraphLayoutEdge.DOM_ELEMENT)) {
							GraphLayoutEdge e = GraphLayoutEdge.loadFromXML(layout, delegate.getDocument().getDocumentElement());
							if (e != null) layout.addLayoutEdge(e);
							delegate = null;
						}

						else {
							throw new SAXException("Unexpected <" + qName + "> inside <" + depth3qName + ">");
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
			
			
			// Depth 5+: Inside a delegate
			
			if (depth >= 5) {
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
