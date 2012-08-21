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

package edu.harvard.util;

import java.util.*;


/**
 * An event on a time-line
 *
 * @author Peter Macko
 */
public class TimelineEvent<T> {

	private T value;
	private String name;
	private double start;
	private double finish;
	
	private ArrayList<TimelineEvent<T>> subevents;
	private TimelineEvent<T> parent;


	/**
	 * Constructor for objects of type TimelineEvent
	 *
	 * @param value the event value or name
	 * @param start the start time
	 * @param finish the end time
	 */
	public TimelineEvent(T value, double start, double finish) {
		this.value = value;
		this.name = null;
		this.start = start;
		this.finish = finish;
		subevents = null;
		parent = null;
	}


	/**
	 * Get the name of the event
	 *
	 * @return the name of the event
	 */
	public String getName() {
		return name == null ? "" + value : name;
	}


	/**
	 * Get the value
	 *
	 * @return the value associated with the event
	 */
	public T getValue() {
		return value;
	}


	/**
	 * Get the start time
	 *
	 * @return the start time
	 */
	public double getStart() {
		return start;
	}


	/**
	 * Get the finish time
	 *
	 * @return the finish time
	 */
	public double getFinish() {
		return finish;
	}


	/**
	 * Set the start time
	 *
	 * @param t the new start time
	 */
	public void setStart(double t) {
		start = t;
	}


	/**
	 * Set the finish time
	 *
	 * @param t the new finish time
	 */
	public void setFinish(double t) {
		finish = t;
	}
	
	
	/**
	 * Set the time range
	 * 
	 * @param start the earliest time
	 * @param finish the latest time
	 */
	public void setRange(double start, double finish) {
		this.start = start;
		this.finish = finish;
	}


	/**
	 * Get the duration
	 *
	 * @return the duration
	 */
	public double getDuration() {
		return finish - start;
	}
	
	
	/**
	 * Get the parent event (if this is a sub-event of something)
	 * 
	 * @return the parent event, or null if none
	 */
	public TimelineEvent<T> getParent() {
		return parent;
	}
	
	
	/**
	 * Return the number of sub-events
	 * 
	 * @return the number of sub-events
	 */
	public int getSubEventCount() {
		return subevents == null ? 0 : subevents.size();
	}
	
	
	/**
	 * Return the list of sub-events
	 * 
	 * @return the list of sub-events
	 */
	public List<TimelineEvent<T>> getSubEvents() {
		if (subevents == null) subevents = new ArrayList<TimelineEvent<T>>();
		return subevents;
	}
	
	
	/**
	 * Add a sub-event
	 * 
	 * @param e the sub-event to add
	 */
	public void addSubEvent(TimelineEvent<T> e) {
		if (subevents == null) subevents = new ArrayList<TimelineEvent<T>>();
		e.parent = this;
		subevents.add(e);
	}
	
	
	/**
	 * Recursively sort all sub-events
	 * 
	 * @param comparator the comparator to use for sorting 
	 */
	public void sortSubEvents(Comparator<TimelineEvent<?>> comparator) {
		if (subevents == null) return;
		Collections.sort(subevents, comparator);
		for (TimelineEvent<T> e : subevents) {
			e.sortSubEvents(comparator);
		}
	}
	
	
	/**
	 * Recursively sort all sub-events by the start time
	 */
	public void sortSubEvents() {
		sortSubEvents(new StartComparator());
	}
	
	
	/**
	 * Get a string representation
	 * 
	 * @return the string value
	 */
	public String toString() {
		return getName();
	}
	
	
	/**
	 * The comparator by start time
	 * 
	 * @author Peter Macko
	 */
	public static class StartComparator implements Comparator<TimelineEvent<?>> {

		/**
		 * Compare two objects based on their start time
		 * 
		 * @param a the first object
		 * @param b the second object
		 * @return the result of the comparison
		 */
		@Override
		public int compare(TimelineEvent<?> a, TimelineEvent<?> b) {
			
			if (a == b) return 0;
			
			if (a.start < b.start) return -1;
			if (a.start > b.start) return  1;
			
			int i = a.getName().compareTo(b.getName());
			if (i != 0) return i;
			
			i = ("" + a.value).compareTo("" + b.value);
			if (i != 0) return i;
			
			return -1;
		}	
	}
}
