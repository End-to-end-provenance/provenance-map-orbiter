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


/**
 * The job to export a provenance document
 * 
 * @author Peter Macko
 */
public class ExportDocumentJob extends AbstractJob {

	private String fileName;
	private Document input;


	/**
	 * Constructor of class ExportDocumentJob
	 *
	 * @param fileName the file name to load
	 * @param input the input
	 */
	public ExportDocumentJob(String fileName, Document input) {
		super("Exporting the document");
		this.fileName = fileName;
		this.input = input;
	}
	
	
	/**
	 * Run the job
	 * 
	 * @throws JobException if the job failed
	 * @throws JobCanceledException if the job was canceled
	 */
	public void run() throws JobException {

		// Export
		
		try {
			input.writeToProjectFile(new File(fileName));
		}
		catch (Throwable t) {
			throw new JobException(t);
		}
	}
}
