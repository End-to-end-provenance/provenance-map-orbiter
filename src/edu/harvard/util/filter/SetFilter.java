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
 * A filter
 * 
 * @author Peter Macko
 */
public class SetFilter<T> extends Filter<T> {
	
	private Set<T> set;
	protected boolean exclude;
	private boolean all;


	/**
	 * Create an instance of class HashSetFilter
	 *
	 * @param name the filter name
	 * @param exclude whether to exclude elements from the list
	 */
	public SetFilter(String name, boolean exclude) {
		super(name);
		
		this.exclude = exclude;
		
		this.set = null;
		this.all = false;
	}


	/**
	 * Create an instance of class HashSetFilter that accepts all values added to the set
	 *
	 * @param name the filter name
	 */
	public SetFilter(String name) {
		this(name, false);
	}
	
	
	/**
	 * Return the expression represented by this filter
	 * 
	 * @return the expression string
	 */
	public String toExpressionString() {
		// TODO Needs to include a user-defined set description name
		if (all || (exclude && (set == null || set.isEmpty()))) return "";
		return (exclude ? "not in " : "in ") + name;
	}

	
	/**
	 * Determine whether to accept a value
	 * 
	 * @param value the value to be examined
	 * @return true if the value should be accepted
	 */
	public boolean accept(T value) {
		if (all) return true;
		if (set == null) return exclude;
		boolean b = set.contains(value);
		return exclude ? !b : b;
	}
	
	
	/**
	 * Clear the set
	 */
	public void clear() {
		set = null;
		fireFilterChanged();
	}
	
	
	/**
	 * Set whether the values in the set are excluded
	 * 
	 * @param exclude whether to exclude the elements in the set
	 */
	public void setExclude(boolean exclude) {
		this.exclude = exclude;
	}
	
	
	/**
	 * Clear the set and set the filter mode to "accept all" until the set is set
	 */
	public void acceptAll() {
		all = true;
		set = null;
		fireFilterChanged();
	}
	
	
	/**
	 * Set the set
	 * 
	 * @param set the new set
	 */
	public void set(Set<T> set) {
		this.all = false;
		this.set = set;
		fireFilterChanged();
	}
}
