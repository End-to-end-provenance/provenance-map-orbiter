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
import edu.harvard.util.gui.*;
import edu.harvard.util.ColorMap;

import java.awt.Color;


/**
 * The graph decorator specific to PASS provenance graphs
 * 
 * @author Peter Macko
 */
public class PASSDecorator extends DefaultGraphDecorator<PNode, PEdge, PSummaryNode, PGraph> {
	
	PGraph pass;
	ColorNodesBy colorNodesBy;
	ColorEdgesBy colorEdgesBy;
	boolean hadProvRank;
	
	ColorMap depthColorMap;
	ColorMap provrankColorMap;
	ColorMap provrankEdgeColorMap;
	
	Color colorArtifact, colorAgent, colorProcess, colorOther;
	Color colorPipe, colorNPFile;
	
	Color colorControl, colorData, colorVersion, colorCompound, colorEdgeOther;
	Color colorEdgeSelectedFrom, colorEdgeSelectedTo, colorEdgeSelectedBoth;
	
	
	/**
	 * The set of different options the nodes can be colored by
	 * 
	 * @author Peter Macko
	 */
	public static enum ColorNodesBy {
		TYPE, DEPTH, PROVRANK
	}
	
	
	/**
	 * The set of different options the edges can be colored by
	 * 
	 * @author Peter Macko
	 */
	public static enum ColorEdgesBy {
		TYPE, PROVRANK
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
		
		
		// Initialize the default colors for OPM node types 
		
		colorAgent    = new Color(255, 255, 128);
		colorArtifact = new Color(255, 255, 255);
		colorProcess  = new Color(128, 255, 128);
		colorOther    = new Color(255, 216, 216);
		
		
		// Initialize default colors for PASS node types
		
		colorPipe   = new Color(255, 128, 255);
		colorNPFile = new Color(216, 216, 216);
		
		
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
		
		
		// Colors more suitable for inclusion in a paper - uncomment if necessary
		
		/*colorNPFile = nodeColor;
		colorArtifact = nodeColor;
		backgroundColor = new Color(216, 216, 216);*/
		
		
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
		
		
		// Create the color maps
		
		PGraphStat s = pass == null ? null : pass.getStat();
		this.hadProvRank = pass == null ? false : pass.wasProvRankComputed();
		
		if (colorNodesBy == ColorNodesBy.PROVRANK) {
			if (pass != null) if (!hadProvRank) pass.requireProvRank();
		}
		
		Color colorMin = Color.WHITE;
		Color colorMax = Color.RED;
		
		this.depthColorMap        = new ColorMap(colorMin, colorMax, 0, s == null ? 1 : s.depthMax);
		this.provrankColorMap     = new ColorMap(colorMin, colorMax,
				s == null ? 0 : Math.log(s.provRankMin), s == null ? 1 : Math.log(s.provRankMax));
		
		colorMin = Color.BLACK;
		colorMax = Color.ORANGE;
		this.provrankEdgeColorMap = new ColorMap(colorMin, colorMax,
				s == null ? 0 : s.provRankLogJumpMin, s == null ? 1 : s.provRankLogJumpMax);
	}
	
	
	/**
	 * Set the "color nodes by" property
	 * 
	 * @param v the new value that specifies how the nodes are colored
	 */
	public void setColorNodesBy(ColorNodesBy v) {
		colorNodesBy = v;
		if (colorNodesBy == ColorNodesBy.PROVRANK) {
			if (pass != null) {
				if (!hadProvRank || pass.requireProvRank()) setPGraph(pass);
			}
		}
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
		
		if (colorNodesBy == ColorNodesBy.PROVRANK) {
			if (pass != null) {
				if (!hadProvRank || pass.requireProvRank()) setPGraph(pass);
			}
		}
		
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
				
			case PROVRANK:
				c = provrankColorMap.getColor(Math.log(node.getProvRank()));
				break;
		}
	
		if (c != null) {
			return adjustColorFill(c, selectionLevel);
		}
		
		return super.getNodeColor(node, selectionLevel);
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
