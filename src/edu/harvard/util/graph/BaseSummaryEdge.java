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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * A directed summary graph edge composed from two or more edges
 * 
 * @author Peter Macko
 */
public class BaseSummaryEdge extends BaseEdge {
	
	private static final long serialVersionUID = -5527240255086381777L;

	private Set<BaseEdge> baseEdges;
	

	/**
	 * Create an instance of class BaseSummaryEdge
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @param edge the base edge this edge represents
	 */
	public BaseSummaryEdge(BaseNode from, BaseNode to, BaseEdge edge) {	
		super(from, to);

		baseEdges = new HashSet<BaseEdge>();
		addBaseEdge(edge);
	}
	

	/**
	 * Create an instance of class BaseSummaryEdge
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @param edges the collection of base edge this edge represents
	 */
	public BaseSummaryEdge(BaseNode from, BaseNode to, Collection<? extends BaseEdge> edges) {	
		super(from, to);

		baseEdges = new HashSet<BaseEdge>();
		for (BaseEdge edge : edges) addBaseEdge(edge);
	}

	
	/**
	 * Get the collection of base edges that this edge summarizes
	 * 
	 * @return the collection of base edges
	 */
	public Collection<BaseEdge> getBaseEdges() {
		return baseEdges;
	}
	
	
	/**
	 * Add a base edge to the collection
	 * 
	 * @param base the base edge
	 */
	public void addBaseEdge(BaseEdge edge) {
		if (edge instanceof BaseSummaryEdge) {
			baseEdges.addAll(((BaseSummaryEdge) edge).baseEdges);
		}
		else {
			baseEdges.add(edge);
		}
	}
}
