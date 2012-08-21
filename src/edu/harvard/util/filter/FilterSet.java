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

import java.util.*;


/**
 * The list of filters
 * 
 * @author Peter Macko
 */
public class FilterSet<T> extends Filter<T> {
	
	private LinkedList<Filter<T>> filters;
	private Listener listener;
	private boolean acceptAllIfEmpty;


	/**
	 * Create an instance of class FilterList
	 * 
	 * @param acceptAllIfEmpty whether to accept all values if the filter is empty
	 */
	public FilterSet(boolean acceptAllIfEmpty) {
		filters = new LinkedList<Filter<T>>();
		listener = new Listener();
		this.acceptAllIfEmpty = acceptAllIfEmpty;
	}
	
	
	/**
	 * Add a filter
	 *
	 * @param f the filter to add
	 */
	public void add(Filter<T> f) {
		if (filters.contains(f)) return;
		filters.add(f);
		f.addFilterListener(listener);
		fireFilterChanged();
	}
	
	
	/**
	 * Remove a filter
	 *
	 * @param f the filter to remove
	 */
	public void remove(Filter<T> f) {
		if (!filters.remove(f)) return;
		f.removeFilterListener(listener);
		fireFilterChanged();
	}
	
	
	/**
	 * Return the list of filters
	 *
	 * @return the linked list of filters
	 */
	public LinkedList<Filter<T>> getFilters() {
		return filters;
	}
	
	
	/**
	 * Determine whether the set of filters is empty
	 * 
	 * @return true if it is empty
	 */
	public boolean isEmpty() {
		return filters.isEmpty();
	}
	
	
	/**
	 * Determine whether to accept a value
	 * 
	 * @param value the value to be examined
	 * @return true if the value should be accepted
	 */
	public boolean accept(T value) {
		
		if (filters.isEmpty()) return acceptAllIfEmpty;
		
		for (Filter<T> f : filters) {
			if (!f.accept(value)) return false;
		}
		return true;
	}
	
	
	/**
	 * Return the expression represented by this filter
	 * 
	 * @return the expression string
	 */
	public String toExpressionString() {
		
		if (filters.size() == 0) return "";
		if (filters.size() == 1) return filters.getFirst().toExpressionString();
		
		StringBuilder b = new StringBuilder();
		boolean first = true;
		
		for (Filter<T> f : filters) {
			String s = f.toExpressionString();
			if (s.length() == 0) continue;
			
			if (first) {
				first = false;
			}
			else {
				b.append(" and ");
			}
			
			b.append("(");
			b.append(s);
			b.append(")");
		}
		
		return b.toString();
	}


	/**
	 * A listener for changes in the children filters
	 */
	private class Listener implements FilterListener<T> {

		/**
		 * Callback for when the filter changed
		 *
		 * @param filter the filter
		 */
		public void filterChanged(Filter<T> filter) {

			// Forward the event

			for (FilterListener<T> l : listeners) l.filterChanged(FilterSet.this);
		}
	}
}
