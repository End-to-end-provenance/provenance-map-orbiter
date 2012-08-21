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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;



/**
 * Miscellaneous XML and DOM utilities
 * 
 * @author Peter Macko
 */
public class XMLUtils {


	/**
	 * Get a single element of some type
	 *
	 * @param element the DOM element
	 * @param child the expected child node name
	 * @return the element
	 * @throws ParserException if there is no such child element, or there is more than one such element
	 */
	public static Element getSingleElement(Element element, String child) throws ParserException {
		
		Element e = getSingleOptionalElement(element, child);
		if (e == null) throw new ParserException("There is no <" + child + "> within <" + element.getNodeName() + ">");
		
		return e;
	}


	/**
	 * Get a single element of some type
	 *
	 * @param element the DOM element
	 * @param child the expected child node name
	 * @return the element, or null if not found
	 * @throws ParserException if there is more than one such element
	 */
	public static Element getSingleOptionalElement(Element element, String child) throws ParserException {
		
		for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element && child.equals(n.getNodeName())) {
				Element e = (Element) n;
				
				// Check for more
				
				for (n = n.getNextSibling(); n != null; n = n.getNextSibling()) {
					if (n instanceof Element && child.equals(n.getNodeName())) {
						throw new ParserException("There can be no more than one <" + child + "> within <" + element.getNodeName() + ">");
					}
				}
				
				return e;
			}
		}

		return null;
	}
	
	
	/**
	 * Get a text value from a child element
	 * 
	 * @param element the DOM element
	 * @param key the expected child node name
	 * @return the text value
	 * @throws ParserException if there is no such child element, or there is more than one such element
	 */
	public static String getTextValue(Element element, String key) throws ParserException {
		
		Element e = getSingleOptionalElement(element, key);
		if (e == null) throw new ParserException("There is no <" + key + "> within <" + element.getNodeName() + ">");
		
		return e.getTextContent();
	}
	
	
	/**
	 * Get a text value from a child element
	 * 
	 * @param element the DOM element
	 * @param key the expected child node name
	 * @param def the default value, if the DOM element is not present
	 * @return the text value
	 * @throws ParserException if there is more than one such element
	 */
	public static String getTextValue(Element element, String key, String def) throws ParserException {
		
		Element e = getSingleOptionalElement(element, key);
		if (e == null) return def;
		
		return e.getTextContent();
	}
	
	
	/**
	 * Get an attribute value
	 * 
	 * @param element the DOM element
	 * @param key the attribute name
	 * @return the text value
	 * @throws ParserException if the attribute does not exist
	 */
	public static String getAttribute(Element element, String key) throws ParserException {
		
		if (!element.hasAttribute(key)) throw new ParserException("No \"" + key + "\" attribute in <" + element.getNodeName() + ">");
		String s = element.getAttribute(key);
		return s;
	}
	
	
	/**
	 * Get an attribute value
	 * 
	 * @param element the DOM element
	 * @param key the attribute name
	 * @param def the default value, if the attribute is not present
	 * @return the text value
	 */
	public static String getAttribute(Element element, String key, String def) {
		
		if (!element.hasAttribute(key)) return def;
		String s = element.getAttribute(key);
		return s;
	}
	
	
	/**
	 * Get an attribute value
	 * 
	 * @param attributes the set of attributes
	 * @param key the attribute name
	 * @return the text value
	 * @throws SAXException if the attribute does not exist
	 */
	public static String getAttribute(Attributes attributes, String key) throws SAXException {
		
		String s = attributes.getValue(key);
		if (s == null) throw new SAXException("No \"" + key + "\" attribute");
		return s;
	}
	
	
	/**
	 * Get an attribute value
	 * 
	 * @param attributes the set of attributes
	 * @param key the attribute name
	 * @param def the default value, if the attribute is not present
	 * @return the text value
	 */
	public static String getAttribute(Attributes attributes, String key, String def) {
		
		String s = attributes.getValue(key);
		if (s == null) return def;
		return s;
	}
}
