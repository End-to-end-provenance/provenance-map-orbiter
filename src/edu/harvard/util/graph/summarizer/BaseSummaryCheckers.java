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

package edu.harvard.util.graph.summarizer;

import java.util.*;
import java.util.regex.*;

import edu.harvard.util.graph.*;


/**
 * A collection of miscellaneous summary checkers
 * 
 * @author Peter Macko
 */
public class BaseSummaryCheckers {
	
	
	/**
	 * Summary checker for nodes with the same (index % some constant).
	 * This is not useful for anything other than testing
	 * 
	 * @author Peter Macko
	 */
	public static class Hash implements BaseSummaryChecker {
		
		private int constant;
		
		/**
		 * Create an instance of class BaseSummaryCheckers.Hash
		 */
		public Hash() {
			this.constant = 10;
		}
		
		/**
		 * Create an instance of class BaseSummaryCheckers.Hash
		 * 
		 * @param constant the hash parameter
		 */
		public Hash(int constant) {
			this.constant = constant;
		}
		
		/**
		 * Determine whether the given two nodes can be merged into one summary node (group)
		 * 
		 * @param a the first node
		 * @param b the second node
		 * @return true if they can be grouped
		 */
		public boolean canGroup(BaseNode a, BaseNode b) {
			return a.getIndex() % constant == b.getIndex() % constant;
		}
	}
	
	
	/**
	 * Summary checker for nodes with the same (index / some constant).
	 * This is not useful for anything other than testing
	 * 
	 * @author Peter Macko
	 */
	public static class ConsecutiveIndex implements BaseSummaryChecker {
		
		private int size;
		
		/**
		 * Create an instance of class BaseSummaryCheckers.ConsecutiveIndex
		 */
		public ConsecutiveIndex() {
			this.size = 10;
		}
		
		/**
		 * Create an instance of class BaseSummaryCheckers.ConsecutiveIndex
		 * 
		 * @param size the number of consecutive indices in one group
		 */
		public ConsecutiveIndex(int size) {
			this.size = size;
		}
		
		/**
		 * Determine whether the given two nodes can be merged into one summary node (group)
		 * 
		 * @param a the first node
		 * @param b the second node
		 * @return true if they can be grouped
		 */
		public boolean canGroup(BaseNode a, BaseNode b) {
			return a.getIndex() / size == b.getIndex() / size;
		}
	}
	
	
	/**
	 * Summary checker for nodes connected to the same nodes
	 * 
	 * @author Peter Macko
	 */
	public static class SameConnectedNodes implements BaseSummaryChecker {
		
		/**
		 * Determine whether the given two nodes can be merged into one summary node (group)
		 * 
		 * @param a the first node
		 * @param b the second node
		 * @return true if they can be grouped
		 */
		public boolean canGroup(BaseNode a, BaseNode b) {
			
			if (a.getIncomingBaseEdges().size() != b.getIncomingBaseEdges().size()) return false;
			if (a.getOutgoingBaseEdges().size() != b.getOutgoingBaseEdges().size()) return false;
			
			// XXX Too slow
			
			HashSet<BaseNode> sa = new HashSet<BaseNode>();
			for (BaseEdge e : a.getIncomingBaseEdges()) sa.add(e.getBaseFrom());
			
			HashSet<BaseNode> sb = new HashSet<BaseNode>();
			for (BaseEdge e : b.getIncomingBaseEdges()) sb.add(e.getBaseFrom());
			
			if (!sa.equals(sb)) return false;
			
			sa.clear();
			for (BaseEdge e : a.getOutgoingBaseEdges()) sa.add(e.getBaseTo());
			
			sb.clear();
			for (BaseEdge e : b.getOutgoingBaseEdges()) sb.add(e.getBaseTo());
			
			if (!sa.equals(sb)) return false;
			
			return true;
		}
	}
	
	
	/**
	 * Summary checker for nodes satisfying the same regex
	 * 
	 * @author Peter Macko
	 */
	public static class RegEx implements BaseSummaryChecker, BaseSummaryLabeler {
		
		private Pattern pattern;
		private String label;
		
		/**
		 * Create an instance of class BaseSummaryCheckers.RegEx
		 * 
		 * @param regex the regular expression
		 * @param label the label
		 */
		public RegEx(String regex, String label) {
			pattern = Pattern.compile(regex);
			this.label = label;
		}
		
		/**
		 * Determine whether the given two nodes can be merged into one summary node (group)
		 * 
		 * @param a the first node
		 * @param b the second node
		 * @return true if they can be grouped
		 */
		public boolean canGroup(BaseNode a, BaseNode b) {
			
			if (pattern.matcher(a.getLabel()).matches() && pattern.matcher(b.getLabel()).matches()) {
				return true;
			}
			
			return false;
		}
		
		/**
		 * Label the given summary node
		 * 
		 * @param node the summary node
		 */
		public void label(BaseSummaryNode node) {
			node.setLabel(label);
		}
	}
}
