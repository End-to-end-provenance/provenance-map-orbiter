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

package edu.harvard.pass.filter;

import edu.harvard.util.attribute.*;
import edu.harvard.util.filter.Filter;
import edu.harvard.util.filter.FilterListener;
import edu.harvard.pass.*;

import java.util.*;


/**
 * Ancestry PASS node filters
 * 
 * @author Peter Macko
 */
public abstract class AncestryFilter extends PNodeSetFilter {
	
	private boolean followOutgoing;
	
	private Attribute<String> a;
	private Filter<PNode> traversalFilter = null;
	private Listener listener;
	
	private int maxResultSize = -1;
	private boolean includeStoppingNodes = true;
	
	
	/**
	 * Create an instance of class
	 * 
	 * @param name the filter name
	 * @param followOutgoing true to follow outgoing edges
	 */
	protected AncestryFilter(String name, boolean followOutgoing) {
		super(name);
		this.followOutgoing = followOutgoing;
		
		listener = new Listener();
		
		a = new Attribute<String>(getName(), true, "1.0");
		a.clearOperators();
		a.addOperator("of");
		a.addAttributeListener(listener);
		addAttribute(a);
		
		acceptAll();
	}
	
	
	/**
	 * Set the PNode of interest
	 * 
	 * @param n the PNode of interest
	 */
	public void setPNode(PNode n) {
		a.set(n.getPublicID());
	}
	
	
	/**
	 * Get the PNode
	 * 
	 * @return the PNode, or null if not available or invalid
	 */
	public PNode getPNode() {
		try {
			return pass.getNodeMap().get(a.get());
		}
		catch (Exception e) {
			return null;
		}
	}
	
	
	/**
	 * Set the maximum result size
	 * 
	 * @param max the new maximum size (use a negative number to disable the constraint)
	 */
	public void setMaxResultSize(int max) {
		maxResultSize = max;

		update();
		fireFilterChanged();
	}
	
	
	/**
	 * Return the maximum result size
	 * 
	 * @return the maximum number of nodes in a result, or -1 if unlimited
	 */
	public int getMaxResultSize() {
		return maxResultSize >= 0 ? maxResultSize : -1;
	}
	
	
	/**
	 * Set whether to include the stopping nodes in the result, even if they
	 * don't satisfy the traversal condition filter
	 * 
	 * @param v true to include the stopping nodes
	 */
	public void setIncludingStoppingNodes(boolean v) {
		includeStoppingNodes = v;

		update();
		fireFilterChanged();
	}
	
	
	/**
	 * Return whether to include the stopping nodes in the result, even if they
	 * don't satisfy the traversal condition filter
	 * 
	 * @return true if including the stopping nodes
	 */
	public boolean isIncludingStoppingNodes() {
		return includeStoppingNodes;
	}
	
	
	/**
	 * Set the traversal condition filter
	 * 
	 * @param filter the filter
	 */
	public void setTraversalConditionFilter(Filter<PNode> filter) {
		
		if (traversalFilter != null) traversalFilter.removeFilterListener(listener);
		traversalFilter = filter;
		if (traversalFilter != null) traversalFilter.addFilterListener(listener);
		
		update();
		fireFilterChanged();
	}
	
	
	/**
	 * Callback for when the provenance graph has been set or changed
	 */
	protected void graphChanged() {
		fireFilterChanged();
	}
	
	
	/**
	 * Update the filter
	 */
	protected void update() {
		

		// Get the PNode
		
		PNode n = null;
		
		try {
			n = pass.getNodeMap().get(a.get());
			if (n == null) throw new NoSuchElementException();
		}
		catch (Exception e) {
			// TODO Do something saner
			//throw new RuntimeException("No such node: " + a.get());
			set(new HashSet<PNode>());
			return;
		}
		
		
		// Get the ancestors (via the breadth-first search)
		
		LinkedList<PNode> queue = new LinkedList<PNode>();
		HashSet<PNode> set = new HashSet<PNode>();
		queue.add(n);
		set.add(n);
		
		while (!queue.isEmpty()) {
			PNode x = queue.removeFirst();
			
			for (PEdge e : (followOutgoing ? x.getOutgoingEdges() : x.getIncomingEdges())) {
				PNode o = (followOutgoing ? e.getTo() : e.getFrom());
				
				if (!set.contains(o)) {
					if (traversalFilter == null
							|| traversalFilter.accept(o.getOriginal())) {
						
						if (maxResultSize >= 0 && maxResultSize <= set.size()) break;
						
						set.add(o);
						queue.addLast(o);
					}
					else if (includeStoppingNodes) {
						
						if (maxResultSize >= 0 && maxResultSize <= set.size()) break;
						
						set.add(o);
					}
				}
			}
			
			if (maxResultSize >= 0 && maxResultSize <= set.size()) break;
		}
		
		set(set);
	}
	
	
	/**
	 * Return the expression represented by this filter
	 * 
	 * @return the expression string
	 */
	public String toExpressionString() {
		String s = getName();
		if (traversalFilter != null) {
			String t = traversalFilter.toExpressionString();
			if (t != null && !"".equals(t) && !"true".equalsIgnoreCase(t)) {
				s += ", until " + t;
			}
		}
		return s;
	}

	
	/**
	 * Attribute listener
	 */
	private class Listener implements AttributeListener, FilterListener<PNode> {

		
		/**
		 * Callback for when the attribute value has changed
		 * 
		 * @param attr the attribute
		 */
		public void attributeValueChanged(AbstractAttribute attr) {
			
			if (attr != a) return;
			update();
		}
		

		/**
		 * Callback for when the attribute constraints have changed
		 * 
		 * @param attr the attribute
		 */
		@Override
		public void attributeConstraintsChanged(AbstractAttribute attr) {
		}

		
		/**
		 * The filter change callback
		 * 
		 * @param filter the filter that changed
		 */
		@Override
		public void filterChanged(Filter<PNode> filter) {
			
			update();
			fireFilterChanged();
		}
	}
	
	
	/**
	 * Class Ancestors
	 */
	public static class Ancestors extends AncestryFilter {
		
		/**
		 * Create an instance of class AncestryFilter.Ancestors
		 */
		public Ancestors() {
			super("Ancestors", true);
		}
	}
	
	
	/**
	 * Class Descendants
	 */
	public static class Descendants extends AncestryFilter {
		
		/**
		 * Create an instance of class AncestryFilter.Descendants
		 */
		public Descendants() {
			super("Descendants", false);
		}
	}
}
