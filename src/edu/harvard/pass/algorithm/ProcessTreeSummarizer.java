/*
 * Provenance Aware Storage System - Java Utilities
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

package edu.harvard.pass.algorithm;

import edu.harvard.pass.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.graph.summarizer.*;
import edu.harvard.util.*;

import java.util.*;


/**
 * Summarize a provenance graph by the process tree
 * 
 * @author Peter Macko
 */
public class ProcessTreeSummarizer implements GraphSummarizer {
	
	
	/**
	 * Get the name of the summarization algorithm
	 * 
	 * @return the name of the summarization algorithm
	 */
	public String getName() {
		return "Process Tree";
	}

	
	/**
	 * Compute the process-tree timeline
	 * 
	 * @param graph the graph
	 * @return the timeline root
	 */
	public TimelineEvent<PObject> computeProcessTimeline(PGraph graph) {
		
		HashMap<Integer, PObject> objectMap = graph.getObjectMap();
		HashMap<Integer, TimelineEvent<PObject>> eventMap = new HashMap<Integer, TimelineEvent<PObject>>();
		
		// TODO The result of this computation should be memoized inside PGraph
		
		
		// Create event object for all processes and put them to a map
		
		for (PObject o : objectMap.values()) {
			if (o.getType() != PObject.Type.PROCESS) continue;
			
			double start = o.getFirstTime();
			double finish = o.getNode(o.getLatestVersion()).getTime();
			if (start < 0) start = (graph.getStat().getTimeUnadjustedMax() - graph.getTimeBase());
			if (finish < 0) finish = start;
			
			TimelineEvent<PObject> e = new TimelineEvent<PObject>(o, start, finish);
			eventMap.put(o.getFD(), e);
		}
		
		
		// Update the finish times based on freeze-times of the output objects
		
		for (TimelineEvent<PObject> t : eventMap.values()) {
			
			for (PNode n : t.getValue().getVersions()) {
				if (n == null) continue;
				for (PEdge e : n.getIncomingEdges()) {
					
					if (e.getType() != PEdge.Type.CONTROL) continue;
					
					PNode x = e.getFrom();
					double time = x.getTime();
					if (time > t.getFinish()) t.setFinish(time);
				}
			}
		} 
		
		
		// Now reconstruct the tree structure
		
		TimelineEvent<PObject> root = new TimelineEvent<PObject>(null, 0, 0);
		
		for (TimelineEvent<PObject> e : eventMap.values()) {
			int parentFD = e.getValue().getParentFD();
			TimelineEvent<PObject> parent = parentFD == PObject.INVALID_FD ? null : eventMap.get(parentFD);
			if (parent == null) parent = root;
			
			
			// Fix the event time
			
			if (e.getStart() == (graph.getStat().getTimeUnadjustedMax() - graph.getTimeBase())) {
				e.setStart(parent.getStart() + 0.0001);
			}
			
			
			// Add the event to the tree
			
			parent.addSubEvent(e);
			
			
			// Fix the parent times
			
			for (TimelineEvent<PObject> x = parent; x != null; x = x.getParent()) {
				if (x.getStart () > e.getStart ()) x.setStart (e.getStart ());
				if (x.getFinish() < e.getFinish()) x.setFinish(e.getFinish());
			}
		}
		
		
		// Finalize
		
		root.sortSubEvents();
		return root;
	}
	
	
	/**
	 * Convert a process-tree timeline to summaries
	 * 
	 * @param timeline the root of the timeline process tree
	 * @param processSummaries the map of process summaries
	 * @param root the summary root
	 * @return the summary node with parent root
	 */
	private PSummaryNode convertTimelineToSummaryHelper(TimelineEvent<PObject> timeline,
			HashMap<PObject, PSummaryNode> processSummaries) {
		
		PObject o = timeline.getValue();
		PSummaryNode summary = o == null ? null : processSummaries.get(o);
		
		for (TimelineEvent<PObject> t : timeline.getSubEvents()) {
			PSummaryNode s = convertTimelineToSummaryHelper(t, processSummaries);
			if (summary != null && s != null) summary.moveNodeFromParent(s);
		}
		
		return summary;
	}
	
	
	/**
	 * Convert a process-tree timeline to summaries
	 * 
	 * @param graph the graph
	 * @param timeline the root of the timeline process tree
	 * @param root the summary root
	 */
	private void convertTimelineToSummary(PGraph graph, TimelineEvent<PObject> timeline, PSummaryNode root) {
		
		// Create a summary for each process
		
		HashMap<Integer, PObject> objectMap = graph.getObjectMap();
		HashMap<PObject, PSummaryNode> processSummaries = new HashMap<PObject, PSummaryNode>();
		
		for (PObject o : objectMap.values()) {
			
			if (o.getType() != PObject.Type.PROCESS) continue;
			
			PSummaryNode summary = graph.newSummaryNode(root);
			summary.setLabel(o.getShortName());
			processSummaries.put(o, summary);
			
			
			// Add all versions of the process
			
			for (PNode n : o.getVersions()) {
				if (n == null) continue;
				summary.moveNodeFromParent(n);
			}
		}

		
		// Build a hierarchy of the processes
		
		convertTimelineToSummaryHelper(timeline, processSummaries);
		
		
		// Assign unassigned nodes
		
		SummarizerUtils.assignUnassigned(graph);
		
		
		// Remove summary nodes with only one child
		
		SummarizerUtils.removeSingletonSummaries(graph);
	}

	
	/**
	 * Recursively compute the graph summary
	 * 
	 * @param graph the graph
	 */
	public void summarize(BaseGraph graph) {

		if (!(graph instanceof PGraph)) {
			throw new IllegalArgumentException("The graph needs to be an instance of PGraph");
		}
		
		PGraph g = Utils.<PGraph>cast(graph);
		
		
		// Initialize
		
		TimelineEvent<PObject> timeline = computeProcessTimeline(g);
		
		g.summarizationBegin();
		PSummaryNode root = g.getRootSummaryNode();
		
		
		// Convert the timeline into the summaries
		
		convertTimelineToSummary(g, timeline, root);
		
		
		// Finalize
		
		g.summarizationEnd();
	}
}
