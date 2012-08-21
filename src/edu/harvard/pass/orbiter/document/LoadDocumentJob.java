/*
 * Provenance Map Orbiter: A visualization tool for large provenance graphs
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

package edu.harvard.pass.orbiter.document;

import java.io.File;

import edu.harvard.util.job.*;
import edu.harvard.util.*;


/**
 * The job to load a document file
 * 
 * @author Peter Macko
 */
public class LoadDocumentJob extends AbstractJob {

	private String fileName;
	private Document result;
	private Pointer<Document> output;


	/**
	 * Constructor of class LoadDocumentJob
	 *
	 * @param fileName the file name to load
	 * @param output the output pointer
	 */
	public LoadDocumentJob(String fileName, Pointer<Document> output) {
		super("Loading the graph");
		this.fileName = fileName;
		result = null;
		this.output = output;
	}


	/**
	 * Constructor of class LoadDocumentJob
	 *
	 * @param fileName the file name to load
	 */
	public LoadDocumentJob(String fileName) {
		this(fileName, null);
	}


	/**
	 * Return the result of the job
	 *
	 * @return the result
	 */
	public Document getResult() {
		return result;
	}
	
	
	/**
	 * Run the job
	 * 
	 * @throws JobException if the job failed
	 * @throws JobCanceledException if the job was canceled
	 */
	public void run() throws JobException {
		
		try {
			result = Document.loadFromProjectFile(new File(fileName), observer);
		}
		catch (Throwable t) {
			result = null;
			throw new JobException(t);
		}

		if (result == null) throw new JobException("Loaded a null document");
		if (output != null) output.set(result);
	}
}
