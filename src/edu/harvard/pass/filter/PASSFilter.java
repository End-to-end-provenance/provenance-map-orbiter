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

import java.text.DecimalFormat;

import edu.harvard.util.attribute.*;
import edu.harvard.pass.*;


/**
 * Basic PASS node filters
 * 
 * @author Peter Macko
 */
public class PASSFilter {
	
	/**
	 * Class FD
	 */
	public static class FD extends PNodeFilter {
		
		private Attribute<Integer> a;
		
		/**
		 * Create an instance of class PASSFilter.FD
		 */
		public FD() {
			super("PNode.FD");
			a = new Attribute<Integer>(getName(), false, 0);
			a.setMinimum(0);
			a.setOperator("<=");
			addAttribute(a);
		}
		
		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getFD());
		}
		
		/**
		 * Set the file descriptor
		 * 
		 * @param fd the file descriptor
		 */
		public void setFD(int fd) {
			a.set(fd);
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			if (pass != null && pass.getBaseNodes().size() > 0) {
				a.setMinimum(pass.getStat().fdMin);
				a.setMaximum(pass.getStat().fdMax);
				a.setDefaultValue(a.getMaximum());
			}
			else {
				a.setMinimum(null);
				a.setMaximum(null);
			}
			fireFilterChanged();
		}
	}


	/**
	 * Class Version
	 */
	public static class Version extends PNodeFilter {

		private Attribute<Integer> a;

		/**
		 * Create an instance of class PASSFilter.Version
		 */
		public Version() {
			super("PNode.Ver");
			a = new Attribute<Integer>(getName(), true, 0);
			a.setMinimum(0);
			a.setOperator("<=");
			addAttribute(a);
		}

		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getVersion());
		}
		
		/**
		 * Set the version
		 * 
		 * @param ver the version
		 */
		public void setVersion(int ver) {
			a.set(ver);
		}
	}


	/**
	 * Class Name
	 */
	public static class Name extends PNodeFilter {

		private Attribute<String> a;

		/**
		 * Create an instance of class PASSFilter.Name
		 */
		public Name() {
			super("Name");
			a = new Attribute<String>(getName(), true, "");
			addAttribute(a);
		}

		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getObject().getName());
		}
		
		/**
		 * Set the object name
		 * 
		 * @param name the object name
		 */
		public void setName(String name) {
			a.set(name);
		}
	}


	/**
	 * Class Type
	 */
	public static class Type extends PNodeFilter {

		private Attribute<String> a;

		/**
		 * Create an instance of class PASSFilter.Type
		 */
		public Type() {
			super("Type");
			a = new Attribute<String>(getName(), true, "");
			addAttribute(a);
		}

		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getObject().getExtendedType());
		}
		
		/**
		 * Set the object type
		 * 
		 * @param type the object type
		 */
		public void setType(String type) {
			a.set(type);
		}
	}


	/**
	 * Class TypeCode
	 */
	public static class TypeCode extends PNodeFilter {

		private Attribute<PObject.Type> a;

		/**
		 * Create an instance of class PASSFilter.Type
		 */
		public TypeCode() {
			super("Type Code");
			a = new Attribute<PObject.Type>(getName(), true, PObject.Type.ARTIFACT);
			addAttribute(a);
		}

		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getObject().getType());
		}
		
		/**
		 * Set the object type
		 * 
		 * @param type the object type
		 */
		public void setTypeCode(PObject.Type type) {
			a.set(type);
		}
	}


	/**
	 * Class Time
	 */
	public static class Time extends PNodeFilter {

		private Attribute<Double> a;

		/**
		 * Create an instance of class PASSFilter.Time
		 */
		public Time() {
			super("Time");
			a = new Attribute<Double>(getName(), false, 0.0);
			a.setMinimum(0.0);
			a.setOperator("<=");
			addAttribute(a);
		}

		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getTime());
		}
		
		/**
		 * Set the time
		 * 
		 * @param time the time
		 */
		public void setTime(double time) {
			a.set(time);
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			if (pass != null && pass.getBaseNodes().size() > 0) {
				a.setMaximum(pass.getStat().getTimeUnadjustedMax() - pass.getTimeBase());
				a.setDefaultValue(a.getMaximum());
			}
			else {
				a.setMaximum(null);
			}
			fireFilterChanged();
		}
	}


	/**
	 * Class ProvRank
	 */
	public static class ProvRank extends PNodeFilter {

		private final DecimalFormat defaultDecimalFormat = new DecimalFormat("#.####"); 
		private Attribute<Double> a;
		

		/**
		 * Create an instance of class PASSFilter.ProvRank
		 */
		public ProvRank() {
			super("ProvRank");
			a = new Attribute<Double>("log("+getName()+")", false, 0.0);
			a.setOperator("<=");
			addAttribute(a);
		}

		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(Math.log(node.getProvRank()));
		}
		
		/**
		 * Set the ProvRank
		 * 
		 * @param pr the ProvRank
		 */
		public void setProvRank(double pr) {
			a.set(pr);
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			if (pass != null && pass.getBaseNodes().size() > 0) {
				pass.requireProvRank();
				a.setMinimum(Math.log(pass.getStat().provRankMin));
				a.setMaximum(Math.log(pass.getStat().provRankMax));
				a.setDefaultValue(a.getMaximum());
			}
			else {
				a.setMinimum(null);
				a.setMaximum(null);
			}
			fireFilterChanged();
		}
		
		
		/**
		 * Get the string representation of the associated filter value
		 * 
		 * @return the string representation of the associated filter value
		 */
		public String getAttributeString() {
			return defaultDecimalFormat.format(Math.exp(a.get()));
		}
	}

	
	/**
	 * Class Indegree
	 */
	public static class Indegree extends PNodeFilter {
		
		private Attribute<Integer> a;
		
		/**
		 * Create an instance of class Indegree
		 */
		public Indegree() {
			super("Indegree");
			a = new Attribute<Integer>(getName(), false, 0);
			a.setMinimum(0);
			a.setOperator("<=");
			addAttribute(a);
		}
		
		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getIncomingEdges().size());
		}
		
		/**
		 * Set the degree
		 * 
		 * @param v the degree
		 */
		public void setIndegree(int v) {
			a.set(v);
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			if (pass != null && pass.getBaseNodes().size() > 0) {
				a.setMaximum(pass.getStat().indegreeMax);
				a.setDefaultValue(a.getMaximum());
			}
			else {
				a.setMaximum(null);
			}
			fireFilterChanged();
		}
	}
	
	
	/**
	 * Class Outdegree
	 */
	public static class Outdegree extends PNodeFilter {
		
		private Attribute<Integer> a;
		
		/**
		 * Create an instance of class Outdegree
		 */
		public Outdegree() {
			super("Outdegree");
			a = new Attribute<Integer>(getName(), false, 0);
			a.setMinimum(0);
			a.setOperator("<=");
			addAttribute(a);
		}
		
		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getOutgoingEdges().size());
		}
		
		/**
		 * Set the degree
		 * 
		 * @param v the degree
		 */
		public void setOutdegree(int v) {
			a.set(v);
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			if (pass != null && pass.getBaseNodes().size() > 0) {
				a.setMaximum(pass.getStat().outdegreeMax);
				a.setDefaultValue(a.getMaximum());
			}
			else {
				a.setMaximum(null);
			}
			fireFilterChanged();
		}
	}
	
	
	/**
	 * Class Degree
	 */
	public static class Degree extends PNodeFilter {
		
		private Attribute<Integer> a;
		
		/**
		 * Create an instance of class Degree
		 */
		public Degree() {
			super("Degree");
			a = new Attribute<Integer>(getName(), false, 0);
			a.setMinimum(0);
			a.setOperator("<=");
			addAttribute(a);
		}
		
		/**
		 * Determine whether to accept a PASS node
		 * 
		 * @param node the node to be examined
		 * @return true if the value should be accepted
		 */
		public boolean accept(PNode node) {
			return a.compareLeft(node.getIncomingEdges().size() + node.getOutgoingEdges().size());
		}
		
		/**
		 * Set the degree
		 * 
		 * @param v the degree
		 */
		public void setDegree(int v) {
			a.set(v);
		}
		
		/**
		 * Callback for when the provenance graph has been set or changed
		 */
		protected void graphChanged() {
			if (pass != null && pass.getBaseNodes().size() > 0) {
				a.setMaximum(pass.getStat().degreeMax);
				a.setDefaultValue(a.getMaximum());
			}
			else {
				a.setMaximum(null);
			}
			fireFilterChanged();
		}
	}
}
