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

package edu.harvard.util.graph;


/**
 * A comparable attribute to be attached to every node, but external to the Node objects
 * 
 * @author Peter Macko
 * 
 * @param <T> the type of the node attribute
 */
public class PerNodeComparableAttribute<T extends Comparable<T>> extends PerNodeAttribute<T> {
	
	private static final long serialVersionUID = -3148195516501847629L;

	
	// The min and max value cache
	
	private T min;
	private T max;
	
	
	/**
	 * Create an instance of class PerNodeComparableAttribute
	 * 
	 * @param name the attribute name
	 */
	public PerNodeComparableAttribute(String name) {
		super(name);
		min = null;
		max = null;
	}
	
	
	/**
	 * Set a value for a node
	 * 
	 * @param node the node
	 * @param value the new value
	 */
	public void set(BaseNode node, T value) {
		
		T old = get(node);
		if (min == old) min = null;
		if (max == old) max = null;
		
		super.set(node, value);
		
		if (min != null && value != null) {
			if (min.compareTo(value) > 0) min = value;
		}
		if (max != null && value != null) {
			if (max.compareTo(value) < 0) max = value;
		}
	}
	
	
	/**
	 * Get the minimum value
	 * 
	 * @return the minimum value, or null if there are no values
	 */
	public T getMin() {
		
		T x = min;
		if (x != null) return x;
		
		x = null;
		for (int i = 0; i < values.size(); i++) {
			T value = values.get(i);
			if (x != null) {
				if (x.compareTo(value) > 0) x = value;
			}
			else if (value != null) {
				x = value;
			}
		}
		
		min = x;
		return x;
	}
	
	
	/**
	 * Get the maximum value
	 * 
	 * @return the maximum value, or null if there are no values
	 */
	public T getMax() {
		
		T x = max;
		if (x != null) return x;
		
		x = null;
		for (int i = 0; i < values.size(); i++) {
			T value = values.get(i);
			if (x != null) {
				if (x.compareTo(value) < 0) x = value;
			}
			else if (value != null) {
				x = value;
			}
		}
		
		max = x;
		return x;
	}
}
