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


/**
 * A filter that can be switched on and off
 * 
 * @author Peter Macko
 */
public class SwitchableFilter<T> extends Filter<T> {
	
	private Filter<T> filter;
	private Listener listener;
	private boolean enabled;
	private boolean acceptAllIfDisabled;


	/**
	 * Create an instance of class SwitchableFilter
	 * 
	 * @param filter the embedded filter
	 * @param enabled true to enable the filter
	 * @param acceptAllIfDisabled true to accept everything if the filter is disabled
	 */
	public SwitchableFilter(Filter<T> filter, boolean enabled, boolean acceptAllIfDisabled) {
		
		this.filter = filter;
		this.enabled = enabled;
		this.acceptAllIfDisabled = acceptAllIfDisabled;
		
		listener = new Listener();
		if (this.filter != null) this.filter.addFilterListener(listener);
	}


	/**
	 * Create an instance of class SwitchableFilter
	 * 
	 * @param filter the embedded filter
	 * @param enabled true to enable the filter
	 */
	public SwitchableFilter(Filter<T> filter, boolean enabled) {
		this(filter, enabled, true);
	}


	/**
	 * Create an instance of class SwitchableFilter
	 * 
	 * @param filter the embedded filter
	 */
	public SwitchableFilter(Filter<T> filter) {
		this(filter, true, true);
	}
	
	
	/**
	 * Return the embedded filter
	 *
	 * @return the filter
	 */
	public Filter<T> getFilter() {
		return filter;
	}
	
	
	/**
	 * Set the embedded filter
	 *
	 * @param filter the new filter
	 */
	public void setFilter(Filter<T> filter) {
		if (this.filter != null) this.filter.removeFilterListener(listener);
		this.filter = filter;
		if (this.filter != null) this.filter.addFilterListener(listener);
		fireFilterChanged();
	}
	
	
	/**
	 * Determine whether the filter is currently enabled
	 * 
	 * @return true if it is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	
	/**
	 * Enable or disable the filter
	 * 
	 * @param enabled true to enable the filter, false to disable
	 */
	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			fireFilterChanged();
		}
	}
	
	
	/**
	 * Determine whether to accept a value
	 * 
	 * @param value the value to be examined
	 * @return true if the value should be accepted
	 */
	public boolean accept(T value) {
		
		if (enabled && filter != null) {
			return filter.accept(value);
		}
		else {
			return acceptAllIfDisabled;
		}
	}
	
	
	/**
	 * Return the expression represented by this filter
	 * 
	 * @return the expression string
	 */
	public String toExpressionString() {
		
		if (enabled && filter != null) {
			return filter.toExpressionString();
		}
		else {
			return "" + acceptAllIfDisabled;
		}
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

			for (FilterListener<T> l : listeners) l.filterChanged(SwitchableFilter.this);
		}
	}
}
