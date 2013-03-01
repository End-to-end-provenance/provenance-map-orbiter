/*
 *
 * Provenance Map Orbiter: A visualization tool for large provenance graphs
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

package edu.harvard.pass.orbiter.gui;

import edu.harvard.pass.*;
import edu.harvard.util.graph.BaseEdge;
import edu.harvard.util.graph.BaseSummaryEdge;
import edu.harvard.util.graph.GraphDirection;
import edu.harvard.util.graph.PerNodeComparableAttribute;
import edu.harvard.util.graph.algorithm.BetweennessCentrality;
import edu.harvard.util.graph.algorithm.DangalchevClosenessCentrality;
import edu.harvard.util.gui.*;
import edu.harvard.util.ColorMap;
import edu.harvard.util.Pair;
import edu.harvard.util.Utils;

import java.awt.Color;
import java.util.Set;
import java.util.Vector;


/**
 * The graph decorator specific to PASS provenance graphs
 * 
 * @author Peter Macko
 */
public class PASSDecorator extends DefaultGraphDecorator<PNode, PEdge, PSummaryNode, PGraph> {
	
	public static final ColorScheme DEFAULT_COLOR_SCHEME = ColorScheme.INPROV;
	
	PGraph pass;
	ColorNodesBy colorNodesBy;
	ColorEdgesBy colorEdgesBy;
	ColorScheme colorScheme;
	
	boolean hadSubRank;
	boolean hadProvRank;
	
	PerNodeComparableAttribute<Double> dangalchevClosenessCentrality;
	PerNodeComparableAttribute<Double> dangalchevClosenessCentralityInverted;
	PerNodeComparableAttribute<Double> dangalchevClosenessCentralityUndirected;
	boolean hadDangalchevClosenessCentrality;	
	boolean hadDangalchevClosenessCentralityInverted;	
	boolean hadDangalchevClosenessCentralityUndirected;
	
	PerNodeComparableAttribute<Double> betweennessCentrality;
	PerNodeComparableAttribute<Double> betweennessCentralityInverted;
	PerNodeComparableAttribute<Double> betweennessCentralityUndirected;
	boolean hadBetweennessCentrality;	
	boolean hadBetweennessCentralityInverted;	
	boolean hadBetweennessCentralityUndirected;
	
	ColorMap depthColorMap;
	ColorMap degreeColorMap;
	ColorMap indegreeColorMap;
	ColorMap outdegreeColorMap;
	ColorMap degreeScaledColorMap;
	ColorMap indegreeScaledColorMap;
	ColorMap outdegreeScaledColorMap;
	
	ColorMap subrankColorMap;
	ColorMap subrankLogColorMap;
	ColorMap subrankMaxJumpColorMap;
	ColorMap subrankMeanJumpColorMap;
	ColorMap subrankEdgeColorMap;
	
	ColorMap provrankColorMap;
	ColorMap provrankLogColorMap;
	ColorMap provrankMaxJumpColorMap;
	ColorMap provrankMeanJumpColorMap;
	ColorMap provrankEdgeColorMap;
	
	ColorMap dangalchevCCColorMap;
	ColorMap dangalchevCCInvertedColorMap;
	ColorMap dangalchevCCUndirectedColorMap;
	ColorMap betweennessCColorMap;
	ColorMap betweennessCInvertedColorMap;
	ColorMap betweennessCUndirectedColorMap;
	
	Color colorArtifact, colorAgent, colorProcess, colorOther;
	Color colorPipe, colorNPFile;
	
	Color colorTextArtifact, colorTextAgent, colorTextProcess, colorTextOther;
	Color colorTextPipe, colorTextNPFile;
	
	Color colorControl, colorData, colorVersion, colorCompound, colorEdgeOther;
	Color colorEdgeSelectedFrom, colorEdgeSelectedTo, colorEdgeSelectedBoth;
	
	Color colorComparisonSet1, colorComparisonSet2, colorComparisonNone, colorComparisonBoth;
	
	
	/**
	 * The set of different options the nodes can be colored by
	 * 
	 * @author Peter Macko
	 */
	public static enum ColorNodesBy {
		TYPE, COMPARISON, DEPTH,
		DEGREE, INDEGREE, OUTDEGREE,
		DEGREE_SCALED, INDEGREE_SCALED, OUTDEGREE_SCALED,
		SUBRANK, SUBRANK_LOG, SUBRANK_MAX_JUMP, SUBRANK_MEAN_JUMP,
		PROVRANK, PROVRANK_LOG, PROVRANK_MAX_JUMP, PROVRANK_MEAN_JUMP,
		DANGALCHEV_CC, DANGALCHEV_CC_INVERTED, DANGALCHEV_CC_UNDIRECTED,
		BETWEENNESS_CENTRALITY, BETWEENNESS_CENTRALITY_INVERTED, BETWEENNESS_CENTRALITY_UNDIRECTED
	}
	
	
	/**
	 * The set of different options the edges can be colored by
	 * 
	 * @author Peter Macko
	 */
	public static enum ColorEdgesBy {
		TYPE, SUBRANK, PROVRANK
	}
	
		
	/**
	 * Color scheme
	 */
	public static enum ColorScheme {
		ORIGINAL, INPROV
	}
	
	
	/**
	 * Constructor for objects of type PASSDecorator
	 *
	 * @param pass the provenance graph
	 */
	public PASSDecorator(PGraph pass) {
		
		this.pass = pass;
		this.colorNodesBy = ColorNodesBy.TYPE;
		this.colorEdgesBy = ColorEdgesBy.TYPE;
		this.colorScheme = DEFAULT_COLOR_SCHEME;
		
		
		// Initialize the default colors for OPM node types 
		
		colorAgent    = new Color(255, 255, 128);
		colorArtifact = new Color(255, 255, 255);
		colorProcess  = new Color(128, 255, 128);
		colorOther    = new Color(255, 216, 216);
		
		colorTextAgent    = Color.BLACK;
		colorTextArtifact = Color.BLACK;
		colorTextProcess  = Color.BLACK;
		colorTextOther    = Color.BLACK;
		
		
		// Initialize default colors for PASS node types
		
		colorPipe   = new Color(255, 128, 255);
		colorNPFile = new Color(216, 216, 216);
		
		colorTextPipe   = Color.BLACK;
		colorTextNPFile = Color.BLACK;
	
		
		// Edge colors 
		
		colorControl = Color.BLUE.darker();
		colorData = Color.DARK_GRAY;
		colorVersion = Color.GREEN.darker();
		colorCompound = Color.DARK_GRAY;
		colorEdgeOther = Color.RED.darker();
		
		
		// Edge colors if one or both of the endpoints is selected
		
		colorEdgeSelectedFrom = Color.BLUE.darker();
		colorEdgeSelectedTo = Color.RED.darker();
		colorEdgeSelectedBoth = Color.MAGENTA.darker();
		
		
		// Comparison colors
		
		colorComparisonNone = new Color(192, 192, 192);
		colorComparisonSet1 = new Color(255, 128, 128);
		colorComparisonSet2 = new Color(128, 128, 255);
		colorComparisonBoth = Color.WHITE;
		
		
		// Colors more suitable for inclusion in a paper - uncomment if necessary
		
		/*colorNPFile = nodeColor;
		colorArtifact = nodeColor;
		backgroundColor = new Color(216, 216, 216);*/
		
		
		// Colors for the inprov scheme
		
		if (colorScheme == ColorScheme.INPROV) {
			
			colorAgent    = new Color(255, 255, 128);
			colorArtifact = new Color(247, 247, 247);
			colorProcess  = new Color( 64,  64,  64);
			colorOther    = new Color(0xaa, 0xaa, 0xaa);
			
			colorTextAgent    = Color.BLACK;
			colorTextArtifact = Color.BLACK;
			colorTextProcess  = Color.WHITE;
			colorTextOther    = Color.BLACK;
			
			colorPipe   = colorOther;
			colorNPFile = colorOther;
			
			colorTextPipe   = Color.BLACK;
			colorTextNPFile = Color.BLACK;
			
			backgroundColor = new Color(224, 224, 224);
		}
		
		
		// Finish
		
		setPGraph(pass);
	}
	
	
	/**
	 * Return the associated graph
	 * 
	 * @return the graph
	 */
	public PGraph getGraph() {
		return pass;
	}
	
	
	/**
	 * Set the associated graph
	 * 
	 * @param g the graph
	 */
	public void setPGraph(PGraph g) {
		
		pass = g;
		
		boolean reload = true;
		int iteration = 0;
		
		while (reload) {
			reload = false;
			iteration++;

				
			// Fetch the available data for coloring
			
			PGraphStat s = pass == null ? null : pass.getStat();
			this.hadSubRank = pass == null ? false : pass.wasSubRankComputed();
			this.hadProvRank = pass == null ? false : pass.wasProvRankComputed();
	
			this.dangalchevClosenessCentrality =
					Utils.<PerNodeComparableAttribute<Double>>cast(
							pass == null ? null 
									: pass.getPerNodeAttributeOverlay(DangalchevClosenessCentrality.ATTRIBUTE_NAME));
			this.dangalchevClosenessCentralityInverted =
					Utils.<PerNodeComparableAttribute<Double>>cast(
							pass == null ? null 
									: pass.getPerNodeAttributeOverlay(DangalchevClosenessCentrality.ATTRIBUTE_NAME_INVERTED));
			this.dangalchevClosenessCentralityUndirected =
					Utils.<PerNodeComparableAttribute<Double>>cast(
							pass == null ? null 
									: pass.getPerNodeAttributeOverlay(DangalchevClosenessCentrality.ATTRIBUTE_NAME_UNDIRECTED));
			this.hadDangalchevClosenessCentrality = dangalchevClosenessCentrality != null;
			this.hadDangalchevClosenessCentralityInverted = dangalchevClosenessCentralityInverted != null;
			this.hadDangalchevClosenessCentralityUndirected = dangalchevClosenessCentralityUndirected != null;
			
			this.betweennessCentrality =
					Utils.<PerNodeComparableAttribute<Double>>cast(
							pass == null ? null 
									: pass.getPerNodeAttributeOverlay(BetweennessCentrality.ATTRIBUTE_NAME));
			this.betweennessCentralityInverted =
					Utils.<PerNodeComparableAttribute<Double>>cast(
							pass == null ? null 
									: pass.getPerNodeAttributeOverlay(BetweennessCentrality.ATTRIBUTE_NAME_INVERTED));
			this.betweennessCentralityUndirected =
					Utils.<PerNodeComparableAttribute<Double>>cast(
							pass == null ? null 
									: pass.getPerNodeAttributeOverlay(BetweennessCentrality.ATTRIBUTE_NAME_UNDIRECTED));
			this.hadBetweennessCentrality = betweennessCentrality != null;
			this.hadBetweennessCentralityInverted = betweennessCentralityInverted != null;
			this.hadBetweennessCentralityUndirected = betweennessCentralityUndirected != null;
			
			
			// Do we need to recompute something?
			
			if (ensureColorNodesBy()) {
				if (iteration <= 1) {
					reload = true;
					continue;
				}
			}
			
			
			// Create the node color maps
			
			Color colorMin = Color.WHITE;
			Color colorMax = Color.RED;
			
			
			// Create the node color maps - summarization
			
			this.depthColorMap = new ColorMap(colorMin, colorMax, 0, s == null ? 1 : s.depthMax);
			
			
			// Create the node color maps - SubRank and ProvRank
			
			this.subrankColorMap         = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.subRankMin, s == null ? 1 : s.subRankMax);
			this.subrankLogColorMap      = new ColorMap(colorMin, colorMax,
					s == null ? 0 : Math.log(s.subRankMin), s == null ? 1 : Math.log(s.subRankMax));
			this.subrankMaxJumpColorMap  = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.subRankLogJumpMin, s == null ? 1 : s.subRankLogJumpMax);
			this.subrankMeanJumpColorMap = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.subRankMeanLogJumpMin, s == null ? 1 : s.subRankMeanLogJumpMax);
			
			this.provrankColorMap         = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.provRankMin, s == null ? 1 : s.provRankMax);
			this.provrankLogColorMap      = new ColorMap(colorMin, colorMax,
					s == null ? 0 : Math.log(s.provRankMin), s == null ? 1 : Math.log(s.provRankMax));
			this.provrankMaxJumpColorMap  = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.provRankLogJumpMin, s == null ? 1 : s.provRankLogJumpMax);
			this.provrankMeanJumpColorMap = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.provRankMeanLogJumpMin, s == null ? 1 : s.provRankMeanLogJumpMax);
			
			
			// Create the node color maps - basic graph metrics

			this.degreeColorMap    = new ColorMap(colorMin, colorMax, 0, s == null ? 1 : s.degreeMax);
			this.indegreeColorMap  = new ColorMap(colorMin, colorMax, 0, s == null ? 1 : s.indegreeMax);
			this.outdegreeColorMap = new ColorMap(colorMin, colorMax, 0, s == null ? 1 : s.outdegreeMax);

			this.degreeScaledColorMap    = new ColorMap(colorMin, colorMax,
					0, s == null ? 1 : Math.sqrt(Math.sqrt(s.degreeMax)));
			this.indegreeScaledColorMap  = new ColorMap(colorMin, colorMax,
					0, s == null ? 1 : Math.sqrt(Math.sqrt(s.indegreeMax)));
			this.outdegreeScaledColorMap = new ColorMap(colorMin, colorMax,
					0, s == null ? 1 : Math.sqrt(Math.sqrt(s.outdegreeMax)));

			
			// Create the node color maps - graph structure metrics
			
			this.dangalchevCCColorMap = new ColorMap(colorMin, colorMax,
					dangalchevClosenessCentrality == null ? 0 : dangalchevClosenessCentrality.getMin(),
					dangalchevClosenessCentrality == null ? 1 : dangalchevClosenessCentrality.getMax());
			this.dangalchevCCInvertedColorMap = new ColorMap(colorMin, colorMax,
					dangalchevClosenessCentralityInverted == null ? 0 : dangalchevClosenessCentralityInverted.getMin(),
					dangalchevClosenessCentralityInverted == null ? 1 : dangalchevClosenessCentralityInverted.getMax());
			this.dangalchevCCUndirectedColorMap = new ColorMap(colorMin, colorMax,
					dangalchevClosenessCentralityUndirected == null ? 0 : dangalchevClosenessCentralityUndirected.getMin(),
					dangalchevClosenessCentralityUndirected == null ? 1 : dangalchevClosenessCentralityUndirected.getMax());
			
			this.betweennessCColorMap = new ColorMap(colorMin, colorMax,
					betweennessCentrality == null ? 0 : betweennessCentrality.getMin(),
					betweennessCentrality == null ? 1 : betweennessCentrality.getMax());
			this.betweennessCInvertedColorMap = new ColorMap(colorMin, colorMax,
					betweennessCentralityInverted == null ? 0 : betweennessCentralityInverted.getMin(),
					betweennessCentralityInverted == null ? 1 : betweennessCentralityInverted.getMax());
			this.betweennessCUndirectedColorMap = new ColorMap(colorMin, colorMax,
					betweennessCentralityUndirected == null ? 0 : betweennessCentralityUndirected.getMin(),
					betweennessCentralityUndirected == null ? 1 : betweennessCentralityUndirected.getMax());
			
			
			// Create the edge color maps
			
			colorMin = Color.BLACK;
			colorMax = Color.ORANGE;
			this.subrankEdgeColorMap = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.subRankLogJumpMin, s == null ? 1 : s.subRankLogJumpMax);
			this.provrankEdgeColorMap = new ColorMap(colorMin, colorMax,
					s == null ? 0 : s.provRankLogJumpMin, s == null ? 1 : s.provRankLogJumpMax);
		}
	}
	
	
	/**
	 * Set the "color scheme" property
	 * 
	 * @param v the new value that specifies how the objects are colored
	 */
	public void setColorScheme(ColorScheme v) {
		colorScheme = v;
	}
	
	
	/**
	 * Return the color scheme
	 * 
	 * @return the color scheme
	 */
	public ColorScheme getColorScheme() {
		return colorScheme;
	}
	
	
	/**
	 * Set the "color nodes by" property
	 * 
	 * @param v the new value that specifies how the nodes are colored
	 */
	public void setColorNodesBy(ColorNodesBy v) {
		colorNodesBy = v;
		if (ensureColorNodesBy()) setPGraph(pass);
	}
	
	
	/**
	 * Ensure that we have the data for the "color nodes by" property
	 * 
	 * @return true if the data was recomputed
	 */
	protected boolean ensureColorNodesBy() {
		
		if (colorNodesBy == ColorNodesBy.SUBRANK
				|| colorNodesBy == ColorNodesBy.SUBRANK_LOG
				|| colorNodesBy == ColorNodesBy.SUBRANK_MAX_JUMP
				|| colorNodesBy == ColorNodesBy.SUBRANK_MEAN_JUMP) {
			if (pass != null) {
				if (!hadSubRank && pass.requireSubRank()) return true;
			}
		} 
		
		if (colorNodesBy == ColorNodesBy.PROVRANK
				|| colorNodesBy == ColorNodesBy.PROVRANK_LOG
				|| colorNodesBy == ColorNodesBy.PROVRANK_MAX_JUMP
				|| colorNodesBy == ColorNodesBy.PROVRANK_MEAN_JUMP) {
			if (pass != null) {
				if (!hadProvRank && pass.requireProvRank()) return true;
			}
		} 
		
		if (colorNodesBy == ColorNodesBy.DANGALCHEV_CC) {
			if (pass != null) {
				if (!hadDangalchevClosenessCentrality
						&& pass.requireDangalchevClosenessCentrality(GraphDirection.DIRECTED)) return true;
			}
		}
		if (colorNodesBy == ColorNodesBy.DANGALCHEV_CC_INVERTED) {
			if (pass != null) {
				if (!hadDangalchevClosenessCentralityInverted
						&& pass.requireDangalchevClosenessCentrality(GraphDirection.INVERTED)) return true;
			}
		}
		if (colorNodesBy == ColorNodesBy.DANGALCHEV_CC_UNDIRECTED) {
			if (pass != null) {
				if (!hadDangalchevClosenessCentralityUndirected
						&& pass.requireDangalchevClosenessCentrality(GraphDirection.UNDIRECTED)) return true;
			}
		}
		
		if (colorNodesBy == ColorNodesBy.BETWEENNESS_CENTRALITY) {
			if (pass != null) {
				if (!hadBetweennessCentrality
						&& pass.requireBetweennessCentrality(GraphDirection.DIRECTED)) return true;
			}
		}
		if (colorNodesBy == ColorNodesBy.BETWEENNESS_CENTRALITY_INVERTED) {
			if (pass != null) {
				if (!hadBetweennessCentralityInverted
						&& pass.requireBetweennessCentrality(GraphDirection.INVERTED)) return true;
			}
		}
		if (colorNodesBy == ColorNodesBy.BETWEENNESS_CENTRALITY_UNDIRECTED) {
			if (pass != null) {
				if (!hadBetweennessCentralityUndirected
						&& pass.requireBetweennessCentrality(GraphDirection.UNDIRECTED)) return true;
			}
		}
		
		return false;
	}

	
	/**
	 * Return the "color nodes by" property
	 * 
	 * @return the value that specifies how the nodes are colored
	 */
	public ColorNodesBy getColorNodesBy() {
		return colorNodesBy;
	}
	
	
	/**
	 * Set the "color edges by" property
	 * 
	 * @param v the new value that specifies how the edges are colored
	 */
	public void setColorEdgesBy(ColorEdgesBy v) {
		colorEdgesBy = v;
		if (colorEdgesBy == ColorEdgesBy.SUBRANK) {
			if (pass != null) {
				if (!hadSubRank || pass.requireSubRank()) setPGraph(pass);
			}
		}
		if (colorEdgesBy == ColorEdgesBy.PROVRANK) {
			if (pass != null) {
				if (!hadProvRank || pass.requireProvRank()) setPGraph(pass);
			}
		}
	}
	
	
	/**
	 * Return the "color edges by" property
	 * 
	 * @return the value that specifies how the edges are colored
	 */
	public ColorEdgesBy getColorEdgesBy() {
		return colorEdgesBy;
	}


	/**
	 * Determine the color of a node
	 * 
	 * @param node the graph node
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the node color
	 */
	@Override
	public Color getNodeColor(PNode node, int selectionLevel) {
		
		Color c = null;
		double max, sum;
		
		switch (colorNodesBy) {
		
			case TYPE:
				PObject.Type type = node.getObject().getType();
				String extType = node.getObject().getExtendedType();
				
				switch (type) {
				case AGENT:
					c = colorAgent;
					break;
				case ARTIFACT:
					if ("FILE".equals(extType)) c = colorArtifact;
					else if ("NP_FILE".equals(extType)) c = colorNPFile;
					else c = colorArtifact;
					break;
				case PROCESS:
					c = colorProcess;
					break;
				default:
					if ("PIPE".equals(extType)) c = colorPipe;
					else c = colorOther;
				}
				
				break;
				
			case DEPTH:
				c = depthColorMap.getColor(node.getDepth());
				break;
				
			case SUBRANK:
				if (!hadSubRank) return null;
				c = subrankColorMap.getColor(node.getSubRank());
				break;
				
			case SUBRANK_LOG:
				if (!hadSubRank) return null;
				c = subrankLogColorMap.getColor(Math.log(node.getSubRank()));
				break;
				
			case SUBRANK_MAX_JUMP:
				if (!hadSubRank) return null;
				max = Double.MIN_VALUE;
				for (PEdge e : node.getIncomingEdges()) {
					PNode from = e.getFrom();
					double d = Math.log(node.getSubRank()) - Math.log(from.getSubRank());
					if (d > max) max = d;
				}
				c = subrankMaxJumpColorMap.getColor(max);
				break;
				
			case SUBRANK_MEAN_JUMP:
				if (!hadSubRank) return null;
				sum = 0;		
				for (PEdge e : node.getIncomingEdges()) {
					PNode from = e.getFrom();
					sum += Math.log(node.getSubRank()) - Math.log(from.getSubRank());
				}
				if (node.getIncomingEdges().isEmpty()) {
					c = subrankMeanJumpColorMap.getColorByFraction(0);
				}
				else {
					double mean = sum / node.getIncomingEdges().size();
					c = subrankMeanJumpColorMap.getColor(mean);
				}
				break;
				
			case PROVRANK:
				if (!hadProvRank) return null;
				c = provrankColorMap.getColor(node.getProvRank());
				break;
				
			case PROVRANK_LOG:
				if (!hadProvRank) return null;
				c = provrankLogColorMap.getColor(Math.log(node.getProvRank()));
				break;
				
			case PROVRANK_MAX_JUMP:
				if (!hadProvRank) return null;
				max = Double.MIN_VALUE;
				for (PEdge e : node.getIncomingEdges()) {
					PNode from = e.getFrom();
					double d = Math.log(node.getProvRank()) - Math.log(from.getProvRank());
					if (d > max) max = d;
				}
				c = provrankMaxJumpColorMap.getColor(max);
				break;
				
			case PROVRANK_MEAN_JUMP:
				if (!hadProvRank) return null;
				sum = 0;		
				for (PEdge e : node.getIncomingEdges()) {
					PNode from = e.getFrom();
					sum += Math.log(node.getProvRank()) - Math.log(from.getProvRank());
				}
				if (node.getIncomingEdges().isEmpty()) {
					c = provrankMeanJumpColorMap.getColorByFraction(0);
				}
				else {
					double mean = sum / node.getIncomingEdges().size();
					c = provrankMeanJumpColorMap.getColor(mean);
				}
				break;
				
			case DEGREE:
				c = degreeColorMap.getColor(node.getIncomingBaseEdges().size()
						+ node.getOutgoingBaseEdges().size());
				break;
				
			case INDEGREE:
				c = degreeColorMap.getColor(node.getIncomingBaseEdges().size());
				break;
				
			case OUTDEGREE:
				c = degreeColorMap.getColor(node.getOutgoingBaseEdges().size());
				break;
				
			case DEGREE_SCALED:
				c = degreeScaledColorMap.getColor(Math.sqrt(Math.sqrt(node.getIncomingBaseEdges().size()
						+ node.getOutgoingBaseEdges().size())));
				break;
				
			case INDEGREE_SCALED:
				c = degreeScaledColorMap.getColor(Math.sqrt(Math.sqrt(node.getIncomingBaseEdges().size())));
				break;
				
			case OUTDEGREE_SCALED:
				c = degreeScaledColorMap.getColor(Math.sqrt(Math.sqrt(node.getOutgoingBaseEdges().size())));
				break;
				
			case COMPARISON:
				Vector<Pair<Set<String>, String>> sets = pass.getComparisonNodeSets();
				boolean in1 = sets.get(0) == null ? false
						: sets.get(0).getFirst().contains(node.getPublicID());
				boolean in2 = sets.get(1) == null ? false
						: sets.get(1).getFirst().contains(node.getPublicID());
				if (in1 && in2) c = colorComparisonBoth;
				else if (in1) c = colorComparisonSet1;
				else if (in2) c = colorComparisonSet2;
				else c = colorComparisonNone;
				break;

			case DANGALCHEV_CC:
				if (dangalchevClosenessCentrality == null) return null;
				c = dangalchevCCColorMap.getColor(dangalchevClosenessCentrality.get(node));
				break;
				
			case DANGALCHEV_CC_INVERTED:
				if (dangalchevClosenessCentralityInverted == null)  return null;
				c = dangalchevCCInvertedColorMap.getColor(dangalchevClosenessCentralityInverted.get(node));
				break;
				
			case DANGALCHEV_CC_UNDIRECTED:
				if (dangalchevClosenessCentralityUndirected == null)  return null;
				c = dangalchevCCUndirectedColorMap.getColor(dangalchevClosenessCentralityUndirected.get(node));
				break;
				
			case BETWEENNESS_CENTRALITY:
				if (betweennessCentrality == null)  return null;
				c = betweennessCColorMap.getColor(betweennessCentrality.get(node));
				break;
				
			case BETWEENNESS_CENTRALITY_INVERTED:
				if (betweennessCentralityInverted == null)  return null;
				c = betweennessCInvertedColorMap.getColor(betweennessCentralityInverted.get(node));
				break;
				
			case BETWEENNESS_CENTRALITY_UNDIRECTED:
				if (betweennessCentralityUndirected == null)  return null;
				c = betweennessCUndirectedColorMap.getColor(betweennessCentralityUndirected.get(node));
				break;
		}
	
		if (c != null) {
			return adjustColorFill(c, selectionLevel);
		}
		
		return super.getNodeColor(node, selectionLevel);
	}
	
	
	/**
	 * Determine the text color of a node
	 * 
	 * @param node the graph node
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the text color
	 */
	@Override
	public Color getNodeTextColor(PNode node, int selectionLevel) {
		
		Color c = null;
		
		switch (colorNodesBy) {
		
			case TYPE:
				PObject.Type type = node.getObject().getType();
				String extType = node.getObject().getExtendedType();
				
				switch (type) {
				case AGENT:
					c = colorTextAgent;
					break;
				case ARTIFACT:
					if ("FILE".equals(extType)) c = colorTextArtifact;
					else if ("NP_FILE".equals(extType)) c = colorTextNPFile;
					else c = colorTextArtifact;
					break;
				case PROCESS:
					c = colorTextProcess;
					break;
				default:
					if ("PIPE".equals(extType)) c = colorTextPipe;
					else c = colorTextOther;
				}
				
				break;
				
			default:
				c = Color.BLACK;	
		}

		return adjustColorText(c, selectionLevel);
	}
	

	/**
	 * Determine the fill color of a summary node
	 * 
	 * @param node the summary node
	 * @param expanded whether the node is expanded
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the node color
	 */
	@Override
	public Color getSummaryNodeColor(PSummaryNode node, boolean expanded, int selectionLevel) {
		if (expanded) return null;	// Do not fill
		return super.getSummaryNodeColor(node, expanded, selectionLevel);
	}


	/**
	 * Determine the color of an edge
	 * 
	 * @param edge the graph edge
	 * @param fromSelectionLevel the selection level of the from node (NONE, HIGHLIGHTED, or SELECTED)
	 * @param toSelectionLevel the selection level of the to node (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the edge color
	 */
	@Override
	public Color getEdgeColor(PEdge edge, int fromSelectionLevel, int toSelectionLevel) {
		
		if (colorEdgesBy == ColorEdgesBy.PROVRANK) {
			if (pass != null) {
				if (!hadProvRank || pass.requireProvRank()) setPGraph(pass);
			}
		}
		
		
		// The case if one or both of the edge endpoints is selected
		
		if (colorEdgesBy == ColorEdgesBy.TYPE || colorEdgesBy == ColorEdgesBy.PROVRANK) {
			if (fromSelectionLevel >= SELECTED || toSelectionLevel >= SELECTED) {
				if (fromSelectionLevel >= SELECTED && toSelectionLevel >= SELECTED) {
					return colorEdgeSelectedBoth;
				}
				else if (fromSelectionLevel >= SELECTED) {
					return colorEdgeSelectedFrom;
				}
				else if (toSelectionLevel >= SELECTED) {
					return colorEdgeSelectedTo;
				}
			}
		}
		
		
		// Edges of an unknown type
		
		if (edge == null) return colorCompound;

		
		// Color the edges
		
		switch (colorEdgesBy) {
		
			case TYPE:
				switch (edge.getType()) {
					case CONTROL:	return colorControl;
					case DATA:		return colorData;
					case VERSION:	return colorVersion;
					case COMPOUND:	return colorCompound;
					default:		return colorEdgeOther;
				}
				
			case PROVRANK:
				return provrankEdgeColorMap.getColor(Math.log(edge.getTo().getProvRank()) - Math.log(edge.getFrom().getProvRank()));
				
			default:
				return colorCompound;
		}
	}

	
	/**
	 * Determine the color of a summary edge
	 * 
	 * @param edge the graph edge
	 * @param fromSelectionLevel the selection level of the from node (NONE, HIGHLIGHTED, or SELECTED)
	 * @param toSelectionLevel the selection level of the to node (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the edge color
	 */
	@Override
	public Color getSummaryEdgeColor(BaseSummaryEdge edge, int fromSelectionLevel, int toSelectionLevel) {
		
		if (colorEdgesBy == ColorEdgesBy.PROVRANK) {
			if (pass != null) {
				if (!hadProvRank || pass.requireProvRank()) setPGraph(pass);
			}
		}
		
		
		// The case if one or both of the edge endpoints is selected
		
		if (colorEdgesBy == ColorEdgesBy.TYPE || colorEdgesBy == ColorEdgesBy.PROVRANK) {
			if (fromSelectionLevel >= SELECTED || toSelectionLevel >= SELECTED) {
				if (fromSelectionLevel >= SELECTED && toSelectionLevel >= SELECTED) {
					return colorEdgeSelectedBoth;
				}
				else if (fromSelectionLevel >= SELECTED) {
					return colorEdgeSelectedFrom;
				}
				else if (toSelectionLevel >= SELECTED) {
					return colorEdgeSelectedTo;
				}
			}
		}
		
		
		// Edges of an unknown type
		
		if (edge == null) return colorCompound;

		
		// Color the edges
		
		switch (colorEdgesBy) {
				
			case PROVRANK:
				
				// Choose the biggest difference in the logs of provranks
				
				double max = 0;		
				for (BaseEdge e : edge.getBaseEdges()) {
					PNode from = null;
					PNode to = null;
					if (e.getBaseFrom() instanceof PNode) from = (PNode) e.getBaseFrom();
					if (e.getBaseTo  () instanceof PNode) to   = (PNode) e.getBaseTo  ();
					
					if (to == null || from == null) continue; // For now
					double d = Math.log(to.getProvRank()) - Math.log(from.getProvRank());
					if (d > max) max = d;
				}
				
				return provrankEdgeColorMap.getColor(max);
				
			default:
				return colorCompound;
		}

	}
}
