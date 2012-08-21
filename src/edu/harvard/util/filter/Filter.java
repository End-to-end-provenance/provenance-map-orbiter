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

package edu.harvard.util.filter;

import edu.harvard.util.attribute.*;

import java.util.*;


/**
 * A filter
 * 
 * @author Peter Macko
 */
public abstract class Filter<T> implements WithAttribute {
	
	protected ComplexAttribute attributes;
	protected String name;

	private Listener listener;
	protected LinkedList<FilterListener<T>> listeners;


	/**
	 * Create an instance of class Filter
	 *
	 * @param name the filter name
	 */
	protected Filter(String name) {

		this.name = name;
		
		this.listener = new Listener();
		this.listeners = new LinkedList<FilterListener<T>>();
		
		this.attributes = new ComplexAttribute(name);
		this.attributes.addAttributeListener(listener);
	}


	/**
	 * Create an instance of class Filter
	 */
	protected Filter() {
		this("");
	}
	
	
	/**
	 * Return the name of the filter
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * Return the attribute of the filter
	 *
	 * @return the attribute, or if there are multiple attributes, the corresponding complex attribute
	 */
	public AbstractAttribute getAttribute() {
		return attributes.size() == 1 ? attributes.getAttributes().getFirst() : attributes;
	}


	/**
	 * Return the attributes of the filter
	 *
	 * @return the complex attribute
	 */
	public ComplexAttribute getAttributes() {
		return attributes;
	}


	/**
	 * Add an attribute
	 *
	 * @param attr the attribute to add
	 */
	protected void addAttribute(AbstractAttribute attr) {
		attributes.add(attr);
		fireFilterChanged();
	}
	
	
	/**
	 * Return the expression represented by this filter
	 * 
	 * @return the expression string
	 */
	public String toExpressionString() {
		return getAttribute().toExpressionString();
	}


	/**
	 * Add a listener
	 *
	 * @param listener the listener to add
	 */
	public void addFilterListener(FilterListener<T> listener) {
		if (!listeners.contains(listener)) listeners.add(listener);
	}


	/**
	 * Remove a listener
	 *
	 * @param listener the listener to remove
	 */
	public void removeFilterListener(FilterListener<T> listener) {
		listeners.remove(listener);
	}


	/**
	 * Fire all callbacks when the filter has changed
	 */
	protected void fireFilterChanged() {
		for (FilterListener<T> l : listeners) l.filterChanged(this);
	}
	
	
	/**
	 * Get the string representation of the associated filter value
	 * 
	 * @return the string representation of the associated filter value
	 */
	public String getAttributeString() {
		return getAttribute().toString();
	}
	
	
	/**
	 * Determine whether to accept a value
	 * 
	 * @param value the value to be examined
	 * @return true if the value should be accepted
	 */
	public abstract boolean accept(T value);


	/**
	 * A listener for changes in the attributes of the filter
	 */
	private class Listener implements AttributeListener {

		/**
		 * Callback for when the attribute value changed
		 *
		 * @param attr the attribute
		 */
		public void attributeValueChanged(AbstractAttribute attr) {
			fireFilterChanged();
		}

		/**
		 * Callback for when the attribute constraints changed
		 *
		 * @param attr the attribute
		 */
		public void attributeConstraintsChanged(AbstractAttribute attr) {
		}
	}
}
