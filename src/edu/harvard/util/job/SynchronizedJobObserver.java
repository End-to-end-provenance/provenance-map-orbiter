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

package edu.harvard.util.job;


/**
 * A synchronized adapter for a job observer
 * 
 * @author Peter Macko
 */
public class SynchronizedJobObserver implements JobObserver {
	
	protected JobObserver observer;
	protected int min, max, pos;
	
	
	/**
	 * Create an instance of class SynchronizedJobObserver
	 * 
	 * @param observer the nested job observer
	 */
	public SynchronizedJobObserver(JobObserver observer) {
		
		this.observer = observer;
		
		this.pos = 0;
		this.min = 0;
		this.max = 100;
	}
	
	
	/**
	 * Set the range of progress values
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 */
	public synchronized void setRange(int min, int max) {
		
		if (min > max) {
			int t = min; min = max; max = t;
		}
		
		this.pos = min;
		this.min = min;
		this.max = max;
		
		if (observer != null) observer.setRange(min, max);
	}
	
	
	/**
	 * Set the progress value
	 *
	 * @param value the progress value
	 */
	public synchronized void setProgress(int value) {
		
		this.pos = value;
		if (pos < min) pos = min;
		if (pos > max) pos = max;
		
		if (observer != null) observer.setProgress(pos);
	}
	
	
	/**
	 * Set the progress as indeterminate
	 */
	public synchronized void makeIndeterminate() {
		if (observer != null) observer.makeIndeterminate();
	}
	
	
	/**
	 * Add to the progress value
	 *
	 * @param delta the change in the progress value
	 */
	public synchronized void addProgress(int delta) {
		setProgress(pos + delta);
	}
}
