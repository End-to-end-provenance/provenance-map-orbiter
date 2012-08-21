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
import edu.harvard.pass.*;

import java.util.*;


/**
 * Ancestry PASS node filters
 * 
 * @author Peter Macko
 */
public class AncestryFilter {
	
	
	/**
	 * Class Ancestors
	 */
	public static class Ancestors extends PNodeSetFilter {
		
		private Attribute<String> a;
		
		/**
		 * Create an instance of class AncestryFilter.Ancestors
		 */
		public Ancestors() {
			super("Ancestors");
			a = new Attribute<String>(getName(), true, "1.0");
			a.clearOperators();
			a.addOperator("of");
			a.addAttributeListener(new Listener());
			addAttribute(a);
			acceptAll();
		}
		
		/**
		 * Set the PNode of interest
		 * 
		 * @param n the PNode of interest
		 */
		public void setPNode(PNode n) {
			a.set("" + n.getFD() + "." + n.getVersion());
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			fireFilterChanged();
		}
		
		/**
		 * Attribute listener
		 */
		private class Listener implements AttributeListener {

			/**
			 * Callback for when the attribute value has changed
			 * 
			 * @param attr the attribute
			 */
			public void attributeValueChanged(AbstractAttribute attr) {
				
				if (attr != a) return;
				String s = a.get();
				
				
				// Get the PNode
				
				PNode n = null;
				
				try {
					n = pass.getNodeMap().get(s);
					if (n == null) throw new NoSuchElementException();
				}
				catch (Exception e) {
					// TODO Do something saner
					throw new RuntimeException("No such node: " + s); 
				}
				
				
				// Get the ancestors
				
				LinkedList<PNode> queue = new LinkedList<PNode>();
				HashSet<PNode> set = new HashSet<PNode>();
				queue.add(n);
				set.add(n);
				
				while (!queue.isEmpty()) {
					PNode x = queue.removeFirst();
					for (PEdge e : x.getOutgoingEdges()) {
						PNode o = e.getTo();
						if (!set.contains(o)) {
							set.add(o);
							queue.addLast(o);
						}
					}
				}
				
				set(set);
			}

			/**
			 * Callback for when the attribute constraints have changed
			 * 
			 * @param attr the attribute
			 */
			@Override
			public void attributeConstraintsChanged(AbstractAttribute attr) {
			}
		}
	}
	
	
	/**
	 * Class Descendants
	 */
	public static class Descendants extends PNodeSetFilter {
		
		private Attribute<String> a;
		
		/**
		 * Create an instance of class AncestryFilter.Descendants
		 */
		public Descendants() {
			super("Descendants");
			a = new Attribute<String>(getName(), true, "1.0");
			a.clearOperators();
			a.addOperator("of");
			a.addAttributeListener(new Listener());
			addAttribute(a);
			acceptAll();
		}
		
		/**
		 * Set the PNode of interest
		 * 
		 * @param n the PNode of interest
		 */
		public void setPNode(PNode n) {
			a.set("" + n.getFD() + "." + n.getVersion());
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			fireFilterChanged();
		}
		
		/**
		 * Attribute listener
		 */
		private class Listener implements AttributeListener {

			/**
			 * Callback for when the attribute value has changed
			 * 
			 * @param attr the attribute
			 */
			public void attributeValueChanged(AbstractAttribute attr) {
				
				if (attr != a) return;
				String s = a.get();
				
				
				// Get the PNode
				
				PNode n = null;
				
				try {
					n = pass.getNodeMap().get(s);
					if (n == null) throw new NoSuchElementException();
				}
				catch (Exception e) {
					// TODO Do something saner
					throw new RuntimeException("No such node: " + s); 
				}
				
				
				// Get the ancestors
				
				LinkedList<PNode> queue = new LinkedList<PNode>();
				HashSet<PNode> set = new HashSet<PNode>();
				queue.add(n);
				set.add(n);
				
				while (!queue.isEmpty()) {
					PNode x = queue.removeFirst();
					for (PEdge e : x.getIncomingEdges()) {
						PNode o = e.getFrom();
						if (!set.contains(o)) {
							set.add(o);
							queue.addLast(o);
						}
					}
				}
				
				set(set);
			}

			/**
			 * Callback for when the attribute constraints have changed
			 * 
			 * @param attr the attribute
			 */
			@Override
			public void attributeConstraintsChanged(AbstractAttribute attr) {
			}
		}
	}
}
