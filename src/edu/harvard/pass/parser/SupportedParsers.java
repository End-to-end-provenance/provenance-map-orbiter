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

import java.io.File;
import java.net.URI;
import java.util.*;

import edu.harvard.util.ParserException;
import edu.harvard.util.ParserFormatException;


/**
 * A collection of supported parsers
 * 
 * @author Peter Macko
 */
public class SupportedParsers {

	private Vector<Parser> parsers;
	private static SupportedParsers instance = null;
	
	
	/**
	 * Create an instance of class SupportedParsers
	 */
	protected SupportedParsers() {
		
		parsers = new Vector<Parser>();
		parsers.add(new TwigParser());
		parsers.add(new DDGParser());
		parsers.add(new PhyloXMLParser());
		parsers.add(new RDFParser());
		parsers.add(new OPMParser());
		parsers.add(new CPLParser());
	}
	
	
	/**
	 * Get an instance of this class
	 * 
	 * @return the instance
	 */
	public static SupportedParsers getInstance() {
		
		if (instance == null) instance = new SupportedParsers();
		return instance;
	}
	
	
	/**
	 * Determine whether any of the parsers accept the given URI
	 * 
	 * @param uri the input URI
	 * @return true if it accepts the input
	 */
	public boolean accepts(URI uri) {
		
		for (Parser p : parsers) {
			if (p.accepts(uri)) return true;
		}
		
		return false;
	}
	
	
	/**
	 * Get the appropriate parser for the URI
	 * 
	 * @param uri the input URI
	 * @return an appropriate parser - already initialized for the particular URI
	 * @throws ParserException if a parser accepted the URI, but could not parse it
	 * @throws ParserFormatException if no appropriate parser could be found
	 */
	public Parser getParser(URI uri) throws ParserException {
		
		// Check the parsers
		
		for (Parser p : parsers) {
			if (!p.accepts(uri)) continue;
			
			try {
				Parser x = p.getClass().newInstance();
				x.initialize(uri);
				return x;
			}
			catch (InstantiationException e) {
			}
			catch (IllegalAccessException e) {
			}
			catch (ParserFormatException e) {
			}
			catch (ParserException e) {
				throw e;
			}
		}
		
		if (!"file".equals(uri.getScheme())) {
			throw new ParserFormatException("Unrecognized URI format: " + uri);
		}
		else {
			throw new ParserFormatException("Unrecognized file format: " + (new File(uri)).getName());
		}
	}
}
