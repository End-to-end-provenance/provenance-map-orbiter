/*
 * Provenance Aware Storage System - Java Utilities
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

package edu.harvard.pass.parser;

import java.io.*;
import java.net.URI;

import edu.harvard.pass.PMeta;
import edu.harvard.util.*;


/**
 * A parser for .twig and .twig_dump files
 * 
 * @author Peter Macko
 */
public class TwigParser implements Parser {
	
	/**
	 * Create an instance of class TwigParser
	 */
	public TwigParser() {
	}
	
	
	/**
	 * Initialize the parser given an input URI. If the file has an ambiguous type (such as *.xml),
	 * the method should check whether the file has a proper format that can be handled by the parser.
	 * 
	 * @param uri the input URI
	 * @throws ParserException on error
	 * @throws ParserFormatException if the file does not have the appropriate format
	 */
	public void initialize(URI uri) throws ParserException {
		
		if (!accepts(uri)) throw new ParserFormatException();
	}
	

	/**
	 * Parse an object identified by the given URI
	 * 
	 * @param uri the input URI
	 * @param handler the callback for parser events
	 * @throws ParserException on error
	 */
	public void parse(URI uri, ParserHandler handler) throws ParserException {
		
		if (!"file".equals(uri.getScheme())) throw new ParserFormatException();
		File file = new File(uri);
		String ext = Utils.getExtension(file);
		
		handler.setMeta(PMeta.PASS());
		
		if ("twig".equals(ext)) {
			
			try {
				String[] cmds = {"twig_dump", file.getAbsolutePath()};
				ExternalProcess p = new ExternalProcess(cmds);
				p.start();
				InputStream in = p.getProcessOutputStream();
				loadFromStream(in, handler);
				p.finish();
			}
			catch (IOException e) {
				throw new ParserException("I/O Error", e);
			}
		}
		else {
			try {
				FileInputStream in = new FileInputStream(file);
				loadFromStream(in, handler);
				in.close();
			}
			catch (IOException e) {
				throw new ParserException("I/O Error", e);
			}
		}
	}
	
	
	/**
	 * Determine whether the parser accepts the given URI
	 * 
	 * @param uri the input URI
	 * @return true if it accepts the input
	 */
	public boolean accepts(URI uri) {
		
		if (!"file".equals(uri.getScheme())) return false;
		File file = new File(uri);
		String ext = Utils.getExtension(file);
		
		if ("twig".equals(ext)) return true;
		if ("twig_dump".equals(ext)) return true;
		
		return false;
	}


	/**
	 * Load the twig_dump output
	 *
	 * @param in the input stream
	 * @param hadler the callback for parser events
	 * @throws ParserException on error
	 */
	private void loadFromStream(InputStream in, ParserHandler handler) throws ParserException {
		
		// Parse the graph
		
		BufferedReader bin = new BufferedReader(new InputStreamReader(in));
		
		try {
			
			String l;			
			handler.beginParsing();
	
			while ((l = bin.readLine()) != null) {	
	
				// Filter out irrelevant records
				
				if (l.startsWith("BEGIN")) continue;
				if (l.startsWith("END")) continue;
				if (l.startsWith("WAP")) continue;
				if (l.startsWith("file:")) continue;
	
	
				// Parse the triple
	
				boolean ancestry = false;
				int s1 = l.indexOf(" "); if (s1 <= 0) continue;
				int s2 = l.indexOf(" ", s1 + 1); if (s2 <= 0) continue;
				
				String s_pnode = l.substring(0, s1);
				String s_edge = l.substring(s1+1, s2);
				String s_value = l.substring(s2 + 1).trim();
				
				if (s_edge.equals("")) continue;
	
				if (s_value.startsWith("[ANC]")) {
					ancestry = true;
					s_value = s_value.substring(6);
				}
				
				
				// Load the triple
	
				if (ancestry) {
					handler.loadTripleAncestry(s_pnode, s_edge, s_value);
				}
				else {
					handler.loadTripleAttribute(s_pnode, s_edge, s_value);
				}
			}
			
			
			// Finish
			
			handler.endParsing();
		
			//bin.close();
		}
		catch (ParserException e) {
			try { bin.close(); } catch (Throwable t) {};
			throw e;
		}
		catch (IOException e) {
			throw new ParserException("I/O Error", e);
		}
	}
}
