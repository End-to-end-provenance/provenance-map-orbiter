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

import edu.harvard.util.*;

import java.util.*;


/**
 * A collection of filter templates
 * 
 * @author Peter Macko
 */
public class FilterFactory<T> {
	
	private TreeMap<String, Class<?>> filters;

	
	/**
	 * Constructor of class FilterFactory
	 */
	public FilterFactory() {
		filters = new TreeMap<String, Class<?>>();
	}
	
	
	/**
	 * Add a filter class
	 *
	 * @param name the filter name
	 * @param cl the filter class
	 */
	public void add(String name, Class<?> cl) {
		filters.put(name, cl);
	}


	/**
	 * Return the collection of supported filters as a map from names to filter classes
	 *
	 * @return an ordered map from names to filters
	 */
	public Map<String, Class<?>> getFilterMap() {
		return filters;
	}


	/**
	 * Return the collection of supported filter names
	 *
	 * @return the set of names
	 */
	public Set<String> getFilterNames() {
		return filters.keySet();
	}


	/**
	 * Instantiate a filter by name
	 *
	 * @param name the name of the filter
	 * @return the instantiated filter
	 */
	public Filter<T> create(String name) throws Exception {
		Class<?> c = filters.get(name);
		if (c == null) throw new Exception("No such filter: " + name);
		return Utils.<Filter<T>>cast(c.newInstance());
	}
}
