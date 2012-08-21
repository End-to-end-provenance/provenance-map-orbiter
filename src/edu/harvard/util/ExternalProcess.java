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

package edu.harvard.util;

import java.io.*;


/**
 * A handle to an external process 
 * 
 * @author Peter Macko
 */
public class ExternalProcess implements Cancelable {
	
	private String[] cmds;
	private Process p;
	private boolean canceled;
	private boolean useBinSH;
	
	
	/**
	 * Initialize the process. Note that running the process using
	 * an external shell might result in cancel() not working properly.
	 * 
	 * @param cmd the command to run using /bin/sh
	 */
	public ExternalProcess(String cmd) {
		this.cmds = new String[1];
		this.cmds[0] = cmd;
		this.p = null;
		this.canceled = false;
		this.useBinSH = true;
	}
	
	
	/**
	 * Initialize the process
	 * 
	 * @param cmds the command and arguments to run using Java's Runtime
	 */
	public ExternalProcess(String[] cmds) {
		this.cmds = new String[cmds.length];
		for (int i = 0; i < cmds.length; i++) this.cmds[i] = cmds[i];
		this.p = null;
		this.canceled = false;
		this.useBinSH = false;
	}
	

	/**
	 * Start a program and return its input stream
	 *
	 * @return the program output
	 * @throws IOException if an error occurred
	 */
	public OutputStream start() throws IOException {
		
		if (p != null) {
			throw new IOException("The program is already running");
		}

		canceled = false;
		if (useBinSH) {
			p = Utils.exec(cmds[0]);
		}
		else {
			p = Runtime.getRuntime().exec(cmds);
		}

		return p.getOutputStream();
	}
	
	
	/**
	 * Get the output stream of the process
	 * 
	 * @return the output stream
	 * @throws IOException if an error occurred
	 */
	public InputStream getProcessOutputStream() throws IOException {
		
		if (p == null) {
			throw new IOException("The program is not running");
		}
		
		return p.getInputStream();
	}
	

	/**
	 * Finish the program started with start() - capture the output,
	 * wait for completion, and throw an exception if its execution
	 * failed.
	 *
	 * @return the program output
	 * @throws IOException if an error occurred
	 */
	public String finish() throws IOException {

		String line;
		StringBuilder error = new StringBuilder();
		StringBuilder output = new StringBuilder();
		int result = -1;
		
		if (p == null) {
			throw new IOException("The program is not running");
		}

		try {
			BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader os = new BufferedReader(new InputStreamReader(p.getInputStream()));

			while ((line = os.readLine()) != null) output.append(line);
			while ((line = es.readLine()) != null) error.append(line);

			es.close();
			os.close();

			result = p.waitFor();
			p = null;
		}
		catch (Exception e) {
			result = -1;
			error.append(e.getMessage());
		}
		
		p = null;

		if (result != 0) {
			if (canceled) throw new IOException("Canceled");
			throw new IOException(error.toString());
		}
		else {
			return output.toString();
		}
	}
	

	/**
	 * Run a program, capture the output, wait for completion, and throw
	 * an exception if its execution failed. If the program is already
	 * running, the behavior of this method is undefined.
	 *
	 * @return the program output
	 * @throws IOException if an error occurred
	 */
	public String run() throws IOException {
		
		start();
		return finish();
	}
	
	
	/**
	 * Cancel the task
	 */
	public void cancel() {
		
		// NOTE Java's Process.destroy() does not kill subprocesses, so something
		// should be done about this...
		
		try {
			if (p != null) {
				canceled = true;
				p.destroy();
			}
		}
		catch (Exception e) {
			// Silent failover
		}
	}
}
