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

package edu.harvard.pass.filter;

import edu.harvard.pass.*;
import edu.harvard.util.filter.*;
import edu.harvard.util.*;


/**
 * The collection of supported filters
 * 
 * @author Peter Macko
 */
public class SupportedFilters extends FilterFactory<PNode> {
	
	private PGraph pass;

	
	/**
	 * Constructor of class SupportedFilters
	 */
	public SupportedFilters() {
	
		add("PNode.FD", PASSFilter.FD.class);
		add("PNode.Ver", PASSFilter.Version.class);
		add("Name", PASSFilter.Name.class);
		add("Type", PASSFilter.Type.class);
		add("Time", PASSFilter.Time.class);
		
		add("SubRank", PASSFilter.SubRank.class);
		add("SubRank.MaxLogJump", PASSFilter.SubRankMaxLogJump.class);
		add("SubRank.MeanLogJump", PASSFilter.SubRankMeanLogJump.class);
		
		add("ProvRank", PASSFilter.ProvRank.class);
		add("ProvRank.MaxLogJump", PASSFilter.ProvRankMaxLogJump.class);
		add("ProvRank.MeanLogJump", PASSFilter.ProvRankMeanLogJump.class);
		
		add("Indegree", PASSFilter.Indegree.class);
		add("Outdegree", PASSFilter.Outdegree.class);
		add("Degree", PASSFilter.Degree.class);
		
		add("Ancestors", AncestryFilter.Ancestors.class);
		add("Descendants", AncestryFilter.Descendants.class);
		
		pass = null;
	}
	
	
	/**
	 * Set the provenance graph
	 * 
	 * @param pass the provenance graph
	 */
	public void setPGraph(PGraph pass) {
		this.pass = pass;
	}
	
	
	/**
	 * Get the provenance graph
	 * 
	 * @return the provenance graph
	 */
	public PGraph getPGraph() {
		return pass;
	}


	/**
	 * Instantiate a filter by name
	 *
	 * @param name the name of the filter
	 * @return the instantiated filter
	 */
	public Filter<PNode> create(String name) throws Exception {
		Filter<PNode> f = super.create(name);
		
		if (f instanceof WithPGraph) {
			WithPGraph F = Utils.<WithPGraph>cast(f);
			if (pass != null) {
				F.setPGraph(pass);
				f.getAttribute().setToDefault();
			}
		}
		
		return f;
	}
}
