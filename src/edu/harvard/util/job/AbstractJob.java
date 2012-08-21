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

package edu.harvard.util.job;


/**
 * An abstract job
 * 
 * @author Peter Macko
 */
public abstract class AbstractJob implements Job {

	protected String title;
	protected JobObserver observer;


	/**
	 * Constructor of class AbstractJob
	 *
	 * @param title the job title
	 */
	protected AbstractJob(String title) {
		this.title = title;
		observer = null;
	}
	
		
	/**
	 * Get the job name
	 *
	 * @return the job name
	 */
	public String getName() {
		return title;
	}
	
	
	/**
	 * Determine whether this is a minor job that does
	 * not need to be listed. Minor jobs should complete
	 * in a fraction of a second.
	 * 
	 * @return true if this is a minor job
	 */
	public boolean isMinor() {
		// Jobs are major by default - override if necessary
		return false;
	}

	
	/**
	 * Set the progress update callback
	 *
	 * @param callback the callback
	 */
	public void setJobObserver(JobObserver callback) {
		observer = callback;
	}
	
	
	/**
	 * Determine whether the job can be canceled
	 * 
	 * @return true if the job can be canceled
	 */
	public boolean isCancelable() {
		return false;
	}
	

	/**
	 * Cancel the job (if possible)
	 */
	public void cancel() {
	}
}
