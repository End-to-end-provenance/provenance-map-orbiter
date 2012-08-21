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

import java.util.Stack;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.xerces.dom.DocumentImpl;


/**
 * A SAX parser handler that creates a DOM as a result
 * 
 * @author Peter Macko
 */
public class SAX2DOMHandler extends DefaultHandler {
	
	private Document doc;
	private Stack<Element> stack;
	
	
	/**
	 * Create an instance of class SAX2DOMHandler
	 */
	public SAX2DOMHandler() {
		 doc = new DocumentImpl();
		 stack = new Stack<Element>();
	}
	
	
	/**
	 * Get the DOM document
	 * 
	 * @return the parsed DOM document
	 */
	public Document getDocument() {
		return doc;
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
		
		// Start the new element
		
		Element e = doc.createElementNS(null, qName);
		stack.push(e);
		
		for (int i = 0; i < attributes.getLength(); i++) {
			e.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}
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
		
		if (stack.isEmpty()) return;
		
		Element e = stack.peek();		
		String s = new String(ch, start, length);
		e.appendChild(doc.createTextNode(s));
	}
	
	
	/**
	 * Finish an element
	 * 
	 * @param uri the URI
	 * @param localName the element's local name
	 * @param qName the element's fully qualified name
	 * @throws SAXException on error
	 */
	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		Element e = stack.pop();
		
		
		// Append the element
		
		if (stack.isEmpty()) {
			doc.appendChild(e);
		}
		else {
			stack.peek().appendChild(e);
		}
	}
}
