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

import edu.harvard.util.GraphicUtils;
import edu.harvard.util.graph.*;

import java.awt.Color;


/**
 * The default graph decorator
 * 
 * @author Peter Macko
 *
 * @param <N> the node type
 * @param <E> the edge type
 * @param <S> the summary node type
 * @param <G> the graph type
 */
public class DefaultGraphDecorator<N extends Node<N, E>, E extends Edge<N>, S extends SummaryNode<N, E, S, G>, G extends Graph<N, E, S, G>>
	   implements GraphDecorator<N, E, S, G> {
	
	public Color backgroundColor;
	public Color nodeColor;
	public Color nodeOutlineColor;
	public Color edgeColor;
	
	public Color nodeSelectionColor;
	protected double nodeSelectionWeightFill;
	protected double nodeSelectionWeightText;
	protected double nodeSelectionWeightOutline;
	
	public Color nodeHighlightColor;
	protected double nodeHighlightWeightFill;
	protected double nodeHighlightWeightText;
	protected double nodeHighlightWeightOutline;
	
	
	/**
	 * Constructor for objects of type DefaultGraphDecorator
	 *
	 * @param backgroundColor the background color
	 * @param nodeColor the node fill color
	 * @param nodeOutlineColor the node outline color
	 * @param edgeColor the edge color
	 * @param nodeSelectionColor the node selection color
	 */
	public DefaultGraphDecorator(Color backgroundColor, Color nodeColor, Color nodeOutlineColor, Color edgeColor, Color nodeSelectionColor) {
		
		this.backgroundColor = backgroundColor;
		this.nodeColor = nodeColor;
		this.nodeOutlineColor = nodeOutlineColor;
		this.edgeColor = edgeColor;
		
		this.nodeSelectionColor = nodeSelectionColor;
		this.nodeSelectionWeightFill = 0.75;
		this.nodeSelectionWeightText = 0.25;
		this.nodeSelectionWeightOutline = 0.75;
		
		this.nodeHighlightColor = nodeSelectionColor;
		this.nodeHighlightWeightFill = 0.30;
		this.nodeHighlightWeightText = 0.10;
		this.nodeHighlightWeightOutline = 0.30;
	}


	/**
	 * Constructor for objects of type DefaultGraphDecorator
	 */
	public DefaultGraphDecorator() {
		this(Color.LIGHT_GRAY, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.DARK_GRAY, new Color(128, 128, 255));
	}


	/**
	 * Determine the background color of a graph
	 *
	 * @return the background color
	 */
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	
	/**
	 * Adjust the color for selection or highlight
	 * 
	 * @param c the color
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the adjusted color
	 */
	protected Color adjustColorFill(Color c, int selectionLevel) {
		
		switch (selectionLevel) {
		case NONE: return c;
		case HIGHLIGHTED: return GraphicUtils.getColorInBetween(c, nodeHighlightColor, nodeHighlightWeightFill);
		case SELECTED: return GraphicUtils.getColorInBetween(c, nodeSelectionColor, nodeSelectionWeightFill);
		}
		
		throw new IllegalArgumentException();
	}
	
	
	/**
	 * Adjust the color for selection or highlight
	 * 
	 * @param c the color
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the adjusted color
	 */
	protected Color adjustColorOutline(Color c, int selectionLevel) {
		
		switch (selectionLevel) {
		case NONE: return c;
		case HIGHLIGHTED: return GraphicUtils.getColorInBetween(c, nodeHighlightColor, nodeHighlightWeightOutline);
		case SELECTED: return GraphicUtils.getColorInBetween(c, nodeSelectionColor, nodeSelectionWeightOutline);
		}
		
		throw new IllegalArgumentException();
	}
	
	
	/**
	 * Adjust the color for selection or highlight
	 * 
	 * @param c the color
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the adjusted color
	 */
	protected Color adjustColorText(Color c, int selectionLevel) {
		
		switch (selectionLevel) {
		case NONE: return c;
		case HIGHLIGHTED: return GraphicUtils.getColorInBetween(c, nodeHighlightColor, nodeHighlightWeightText);
		case SELECTED: return GraphicUtils.getColorInBetween(c, nodeSelectionColor, nodeSelectionWeightText);
		}
		
		throw new IllegalArgumentException();
	}


	/**
	 * Determine the fill color of a node
	 * 
	 * @param node the graph node
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the node color
	 */
	public Color getNodeColor(N node, int selectionLevel) {
		return adjustColorFill(nodeColor, selectionLevel);
	}

	
	/**
	 * Determine the outline color of a node
	 * 
	 * @param node the graph node
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the node color
	 */
	public Color getNodeOutlineColor(N node, int selectionLevel) {
		return adjustColorOutline(nodeOutlineColor, selectionLevel);
	}

	
	/**
	 * Determine the text color of a node
	 * 
	 * @param node the graph node
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the text color
	 */
	public Color getNodeTextColor(N node, int selectionLevel) {
		return adjustColorText(Color.BLACK, selectionLevel);
	}
	

	/**
	 * Determine the fill color of a summary node
	 * 
	 * @param node the summary node
	 * @param expanded whether the node is expanded
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the node color
	 */
	public Color getSummaryNodeColor(S node, boolean expanded, int selectionLevel) {
		return expanded ? backgroundColor : adjustColorFill(nodeColor, selectionLevel);
	}
	

	/**
	 * Determine the outline color of a summary node
	 * 
	 * @param node the summary node
	 * @param expanded whether the node is expanded
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the node color
	 */
	public Color getSummaryNodeOutlineColor(S node, boolean expanded, int selectionLevel) {
		return adjustColorOutline(nodeOutlineColor, selectionLevel);
	}
	
	
	/**
	 * Determine the text color of a summary node
	 * 
	 * @param node the summary node
	 * @param expanded whether the node is expanded
	 * @param selectionLevel the selection level (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the text color
	 */
	public Color getSummaryNodeTextColor(S node, boolean expanded, int selectionLevel) {
		return adjustColorText(Color.BLACK, selectionLevel);
	}


	/**
	 * Determine the color of an edge
	 * 
	 * @param edge the graph edge
	 * @param fromSelectionLevel the selection level of the from node (NONE, HIGHLIGHTED, or SELECTED)
	 * @param toSelectionLevel the selection level of the to node (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the edge color
	 */
	public Color getEdgeColor(E edge, int fromSelectionLevel, int toSelectionLevel) {
		return edgeColor;
	}

	
	/**
	 * Determine the color of a summary edge
	 * 
	 * @param edge the graph edge
	 * @param fromSelectionLevel the selection level of the from node (NONE, HIGHLIGHTED, or SELECTED)
	 * @param toSelectionLevel the selection level of the to node (NONE, HIGHLIGHTED, or SELECTED)
	 * @return the edge color
	 */
	public Color getSummaryEdgeColor(BaseSummaryEdge edge, int fromSelectionLevel, int toSelectionLevel) {
		return edgeColor;
	}
}
