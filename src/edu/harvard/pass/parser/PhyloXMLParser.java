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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.harvard.pass.PEdge;
import edu.harvard.pass.PMeta;
import edu.harvard.pass.PObject;
import edu.harvard.util.ParserException;
import edu.harvard.util.ParserFormatException;
import edu.harvard.util.Utils;
import edu.harvard.util.XMLUtils;


/**
 * A parser for OPM files
 * 
 * @author Peter Macko
 */
public class PhyloXMLParser implements Parser {
	
	protected static boolean DEBUG = false;
	protected static boolean FAIL_ON_UNKNOWN_ELEMENTS = false;
	
	protected File file;

	
	/**
	 * Create an instance of class PhyloXMLParser
	 */
	public PhyloXMLParser() {
		
		file = null;
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
		
		if (!"file".equals(uri.getScheme())) throw new ParserFormatException();
		
		this.file = new File(uri);
		
		try {
			FileInputStream in = new FileInputStream(file);
			
			byte[] buf = new byte[128];
			int len = in.read(buf);
			
			in.close();
			
			String s = new String(buf, 0, len);
			if (!s.contains("<phyloxml ") && !s.contains("<phyloxml>")) {
				throw new ParserFormatException("Not a PhyloXML file");
			}
		}
		catch (IOException e) {
			throw new ParserException(e);
		}
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
		
		if (DEBUG) {
			System.err.println();
			System.err.println("PhyloXML Import " + file.getName());
		}
	
		
		// Start
		
		PMeta meta = new PMeta();
		
		meta.addObjectAttributeCode("Name", PObject.Attribute.NAME);
		meta.addObjectAttributeCode("scientific_name", PObject.Attribute.NAME);
		meta.addObjectAttributeCode("common_name", PObject.Attribute.NAME);
		
		meta.addObjectAttributeCode("Type", PObject.Attribute.TYPE);
		
		meta.addObjectExtType("Clade", PObject.Type.ARTIFACT);
		meta.addObjectExtType("Root", PObject.Type.ARTIFACT);
		meta.addEdgeLabel("Parent", PEdge.Type.DATA);
		
		handler.beginParsing();
		handler.setMeta(meta);
		
		
		// Read the XML file
		
		try {
			FileInputStream in = new FileInputStream(file);
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			
			PhyloXMLParserHandler p = new PhyloXMLParserHandler(handler);
			sp.parse(in, p);
			
			in.close();
		}
		catch (IOException e) {
			throw new ParserException(e);
		}
		catch (ParserConfigurationException e) {
			throw new ParserException(e);
		} 
		catch (SAXException e) {
			if (e.getCause() instanceof ParserException) {
				throw (ParserException) e.getCause();
			}
			throw new ParserException(e);
		}
		
		
		// Finalize
		
		handler.endParsing();
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
		
		if ("PhyloXML".equalsIgnoreCase(ext)) return true;
		if ("xml"     .equalsIgnoreCase(ext)) return true;

		return false;
	}
	
	
	/**
	 * SAX parser
	 */
	private static class PhyloXMLParserHandler extends DefaultHandler {
		
		private static final Set<String> PROPERTIES;
		
		private ParserHandler handler;
		
		private int depth = 0;
		private int lastId = -1;
		private String tmp = "";
		private String property = null;
		
		private Stack<Integer> stack;
		private List<Integer> roots;
		private boolean rooted;

		
		/**
		 * Static initialization
		 */
		static {
			HashSet<String> p = new HashSet<String>();
			
			p.add("name");
			p.add("branch_length");
			p.add("confidence");
			p.add("id");
			p.add("scientific_name");
			p.add("common_name");
			
			PROPERTIES = Collections.unmodifiableSet(p);
		}
		
		
		/**
		 * Create an instance of PhyloXMLParserHandler
		 * 
		 * @param handler the parser handler
		 */
		public PhyloXMLParserHandler(ParserHandler handler) {

			this.handler = handler;
			
			stack = new Stack<Integer>();
			roots = new ArrayList<Integer>();
			rooted = false;
		}
		
		
		/**
		 * Start new element
		 * 
		 * @param uri the URI
		 * @param localName the element's local name
		 * @param qName the element's fully qualified name
		 * @param attributes the attributes
		 * @throws SAXException on error
		 */
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			
			
			// Depth 0: <phyloxml>
			
			if (depth == 0) {
				if (!qName.equals("phyloxml")) {
					throw new SAXException("Expected <phyloxml>, found <" + qName + ">");
				}
			}
			
			
			// Depth 1: <phylogeny>
			
			if (depth == 1) {
				if (!qName.equals("phylogeny")) {
					throw new SAXException("Expected <phylogeny>, found <" + qName + ">");
				}

				if (XMLUtils.getAttribute(attributes, "rooted", null) != null)
					rooted = "true".equalsIgnoreCase(XMLUtils.getAttribute(attributes, "rooted"));
			}
			
			
			// Depth 2+: Inside a <clade>
			
			if (depth >= 2) {
				
				if (qName.equals("clade")) {
					try {
						int id = ++lastId;
						handler.loadTripleAttribute("" + id, "TYPE", "CLADE");
						if (stack.isEmpty()) {
							roots.add(id);
						}
						else {
							handler.loadTripleAncestry("" + id, "PARENT", stack.peek().toString());
						}
						stack.push(id);
					}
					catch (ParserException e) {
						throw new SAXException(e);
					}
				}
				
				else if (qName.equals("property")) {
					if (stack.isEmpty()) {
						throw new SAXException("Expected <" + qName + "> must be inside <clade>");
					}
					property = XMLUtils.getAttribute(attributes, "ref");
				}
				
				else if (qName.equals("taxonomy") || PROPERTIES.contains(qName)) {
					if (stack.isEmpty()) {
						throw new SAXException("Expected <" + qName + "> must be inside <clade>");
					}					
				}
				
				else {
					if (FAIL_ON_UNKNOWN_ELEMENTS) {
						throw new SAXException("Unsupported element <" + qName + ">");
					}
					else {
						System.err.println("Warning: Unsupported element <" + qName + ">");
					}
				}
			}
			
			
			// Update depth
			
			depth++;
			tmp = "";
		}
		

		/**
		 * Characters
		 * 
		 * @param ch the array of characters
		 * @param start the start within the array
		 * @param length the length of the string 
		 * @throws SAXException on error
		 */
		public void characters(char[] ch, int start, int length) throws SAXException {
			
			tmp += new String(ch, start, length);
		}
		
		
		/**
		 * Finish an element
		 * 
		 * @param uri the URI
		 * @param localName the element's local name
		 * @param qName the element's fully qualified name
		 * @param attributes the attributes
		 * @throws SAXException on error
		 */
		public void endElement(String uri, String localName, String qName) throws SAXException {
			
			depth--;
			
			
			// Depth 1: Inside <phylogeny>
			
			if (depth == 1) {
				
				if (rooted && roots.size() > 1) {
					try {
						int id = ++lastId;
						handler.loadTripleAttribute("" + id, "TYPE", "ROOT");
						for (Integer x : roots) {
							handler.loadTripleAncestry(x.toString(), "PARENT", "" + id);
						}
						roots.clear();
						roots.add(id);
					}
					catch (ParserException e) {
						throw new SAXException(e);
					}
				}
			}
			
			
			// Depth 2+: Inside <clade>
			
			if (depth >= 2) {

				try {
					if (qName.equals("clade")) {
						stack.pop();
					}
					
					else if (qName.equals("property")) {
						handler.loadTripleAttribute(stack.peek().toString(), property, tmp);
					}
					
					else if (PROPERTIES.contains(qName)) {
						handler.loadTripleAttribute(stack.peek().toString(), qName, tmp);				
					}
				}
				catch (ParserException e) {
					throw new SAXException(e);
				}
			}
		}
	}
}
