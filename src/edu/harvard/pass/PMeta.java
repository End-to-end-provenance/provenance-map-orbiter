/*
 * Provenance Aware Storage System - Java Utilities
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

package edu.harvard.pass;

import java.util.*;
import java.util.Map.Entry;

import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import edu.harvard.util.ParserException;
import edu.harvard.util.XMLUtils;


/**
 * Meta-data information about the provenance graph
 * 
 * @author Peter Macko
 */
public class PMeta {
	
	public static final String DOM_ELEMENT = "meta";
	
	private HashMap<String, PEdge.Type> edgeTypes;
	private HashMap<PEdge.Type, String> edgeLabels;
	private HashMap<String, PObject.Type> objectTypes;
	private HashMap<PObject.Type, String> objectLabels;
	private HashMap<String, PObject.Attribute> objectAttributeCodes;
	private HashMap<PObject.Attribute, String> objectAttributeNames;
	private HashMap<String, PNode.Attribute> nodeAttributeCodes;
	private HashMap<PNode.Attribute, String> nodeAttributeNames;
	private boolean caseSensitive;

	
	/**
	 * Create an instance of class PMeta
	 */
	public PMeta() {

		clear();
	}
	
	
	/**
	 * Create the metadata object and initialize it to PASS defaults
	 * 
	 * @return the new metadata object, initialized to PASS defaults
	 */
	public static PMeta PASS() {
		
		PMeta m = new PMeta();
		
		
		// PASS-specific settings
		
		m.addEdgeLabel("INPUT", PEdge.Type.DATA);
		m.addEdgeLabel("FORKPARENT", PEdge.Type.CONTROL);
		m.addEdgeLabel("VERSION", PEdge.Type.VERSION);
		
		m.addObjectExtType("PROC", PObject.Type.PROCESS);
		m.addObjectExtType("FILE", PObject.Type.ARTIFACT);
		m.addObjectExtType("NP_FILE", PObject.Type.ARTIFACT);
		
		m.addObjectAttributeCode("Name", PObject.Attribute.NAME);
		m.addObjectAttributeCode("Type", PObject.Attribute.TYPE);
		
		m.addNodeAttributeCode("FreezeTime", PNode.Attribute.FREEZETIME);
		m.addNodeAttributeCode("Time", PNode.Attribute.TIME);
		
		
		// Non-standard
		
		m.addNodeAttributeCode("LABEL", PNode.Attribute.LABEL);
		
		
		// RDF-specific settings
		
		m.addNodeAttributeCode("http://www.w3.org/2000/01/rdf-schema#label", PNode.Attribute.LABEL);

		return m;
	}
	
	
	/**
	 * Create the metadata object and initialize it to CPL defaults
	 * 
	 * @return the new metadata object, initialized to CPL defaults
	 */
	public static PMeta CPL() {
		
		PMeta m = new PMeta();
		
		
		// Standard CPL
		
		m.addEdgeLabel("INPUT", PEdge.Type.DATA);
		m.addEdgeLabel("IPC", PEdge.Type.DATA);
		m.addEdgeLabel("COPY", PEdge.Type.DATA);
		m.addEdgeLabel("TRANSLATION", PEdge.Type.DATA);
		m.addEdgeLabel("OP", PEdge.Type.CONTROL);
		m.addEdgeLabel("START", PEdge.Type.CONTROL);
		m.addEdgeLabel("VERSION", PEdge.Type.VERSION);
		m.addEdgeLabel("PREV", PEdge.Type.VERSION);
		
		m.addObjectExtType("PROC", PObject.Type.PROCESS);
		m.addObjectExtType("PROCESS", PObject.Type.PROCESS);
		m.addObjectExtType("FILE", PObject.Type.ARTIFACT);
		m.addObjectExtType("ARTIFACT", PObject.Type.ARTIFACT);
		
		m.addObjectAttributeCode("Name", PObject.Attribute.NAME);
		m.addObjectAttributeCode("Type", PObject.Attribute.TYPE);
		
		m.addNodeAttributeCode("Time", PNode.Attribute.TIME);
		m.addNodeAttributeCode("LABEL", PNode.Attribute.LABEL);
		
		
		// Non-standard
		
		m.addObjectExtType("Database", PObject.Type.ARTIFACT);
		m.addObjectExtType("Operator", PObject.Type.PROCESS);
		m.addObjectExtType("Operation", PObject.Type.PROCESS);
		m.addObjectExtType("Token", PObject.Type.ARTIFACT);
		
		return m;
	}
	
	
	/**
	 * Clear/initialize the metadata object
	 */
	public void clear() {
		
		caseSensitive = false;
		
		edgeTypes = new HashMap<String, PEdge.Type>();
		edgeLabels = new HashMap<PEdge.Type, String>();
		objectTypes = new HashMap<String, PObject.Type>();
		objectLabels = new HashMap<PObject.Type, String>();
		objectAttributeCodes = new HashMap<String, PObject.Attribute>();
		objectAttributeNames = new HashMap<PObject.Attribute, String>();
		nodeAttributeCodes = new HashMap<String, PNode.Attribute>();
		nodeAttributeNames = new HashMap<PNode.Attribute, String>();
	}
	
	
	/**
	 * Set whether the string parsing should be case sensitive
	 * 
	 * @param value true if the parsing should be case sensitive
	 */
	public void setCaseSensitive(boolean value) {
		caseSensitive = value;
	}
	
	
	/**
	 * Determine whether the string parsing is case sensitive
	 * 
	 * @return true if the parsing is case sensitive
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}
	
	
	/**
	 * Add an edge label/type
	 * 
	 * @param label edge label
	 * @param type the edge type
	 */
	public void addEdgeLabel(String label, PEdge.Type type) {
		edgeTypes.put(caseSensitive ? label : label.toLowerCase(), type);
		if (!edgeLabels.containsKey(type)) edgeLabels.put(type, label);
	}
	
	
	/**
	 * Add an object (node) extended type information
	 * 
	 * @param extType extended type
	 * @param type the object type
	 */
	public void addObjectExtType(String extType, PObject.Type type) {
		objectTypes.put(caseSensitive ? extType : extType.toLowerCase(), type);
		if (!objectLabels.containsKey(type)) objectLabels.put(type, extType);
	}
	
	
	/**
	 * Add information about a standard object attribute name
	 * 
	 * @param name the object attribute name
	 * @param code the object attribute code
	 */
	public void addObjectAttributeCode(String name, PObject.Attribute code) {
		objectAttributeCodes.put(caseSensitive ? name : name.toLowerCase(), code);
		if (!objectLabels.containsKey(code)) objectAttributeNames.put(code, name);
	}
	
	
	/**
	 * Add information about a standard node attribute name
	 * 
	 * @param name the node attribute name
	 * @param code the node attribute code
	 */
	public void addNodeAttributeCode(String name, PNode.Attribute code) {
		nodeAttributeCodes.put(caseSensitive ? name : name.toLowerCase(), code);
		if (!objectLabels.containsKey(code)) nodeAttributeNames.put(code, name);
	}
	
	
	/**
	 * Convert an edge label into an edge type
	 * 
	 * @param label the edge label
	 * @return the edge type
	 */
	public PEdge.Type getEdgeType(String label) {
		PEdge.Type t = edgeTypes.get(caseSensitive ? label : label.toLowerCase());
		return t == null ? PEdge.Type.OTHER : t;
	}
	
	
	/**
	 * Get a default edge label for the given edge type
	 * 
	 * @param type the edge type
	 * @return the default edge label
	 */
	public String getEdgeLabel(PEdge.Type type) {
		String label = edgeLabels.get(type);
		return label == null ? "" : label;
	}
	
	
	/**
	 * Convert an extended object (node) type into a type code
	 * 
	 * @param extType the node extended type
	 * @return the node type
	 */
	public PObject.Type getObjectType(String extType) {
		PObject.Type t = objectTypes.get(caseSensitive ? extType : extType.toLowerCase());
		return t == null ? PObject.Type.OTHER : t;
	}
	
	
	/**
	 * Get a default extended object (node) type for the given type code
	 * 
	 * @param type the node type code
	 * @return the default node extended type
	 */
	public String getObjectExtType(PObject.Type type) {
		String extType = objectLabels.get(type);
		return extType == null ? "" : extType;
	}
	
	
	/**
	 * Convert an object attribute name into a an attribute code
	 * 
	 * @param name the attribute name
	 * @return the attribute code
	 */
	public PObject.Attribute getObjectAttributeCode(String name) {
		PObject.Attribute t = objectAttributeCodes.get(caseSensitive ? name : name.toLowerCase());
		return t == null ? PObject.Attribute.OTHER : t;
	}
	
	
	/**
	 * Get the default label for the given object attribute code
	 * 
	 * @param type the attribute code
	 * @return the attribute name
	 */
	public String getObjectAttributeName(PObject.Attribute type) {
		String name = objectAttributeNames.get(type);
		return name == null ? "" : name;
	}
	
	
	/**
	 * Convert a node attribute name into a an attribute code
	 * 
	 * @param name the node name
	 * @return the node code
	 */
	public PNode.Attribute getNodeAttributeCode(String name) {
		PNode.Attribute t = nodeAttributeCodes.get(caseSensitive ? name : name.toLowerCase());
		return t == null ? PNode.Attribute.OTHER : t;
	}
	
	
	/**
	 * Get the default label for the given node attribute code
	 * 
	 * @param type the attribute code
	 * @return the attribute name
	 */
	public String getNodeAttributeName(PNode.Attribute type) {
		String name = nodeAttributeNames.get(type);
		return name == null ? "" : name;
	}


	/**
	 * Write the metadata information to XML
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {

		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		attrs.addAttribute("", "", "case-sensitive", "CDATA", "" + caseSensitive);
		hd.startElement("", "", DOM_ELEMENT, attrs);
		
		
		// Write the edges
		
		attrs.clear();
		hd.startElement("", "", "edge-types", attrs);
		
		for (Entry<String, PEdge.Type> e : edgeTypes.entrySet()) {
			boolean def = caseSensitive ? edgeLabels.get(e.getValue()).equals(e.getKey())
                    : edgeLabels.get(e.getValue()).equalsIgnoreCase(e.getKey());
			attrs.clear();
			attrs.addAttribute("", "", "code", "CDATA", "" + e.getValue());
			if (def) attrs.addAttribute("", "", "default", "CDATA", "" + def);
			hd.startElement("", "", "edge-type", attrs);
			String s = def ? edgeLabels.get(e.getValue()) : e.getKey();
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "edge-type");
		}
		
		hd.endElement("", "", "edge-types");
		
		
		// Write the object types
		
		attrs.clear();
		hd.startElement("", "", "object-types", attrs);
		
		for (Entry<String, PObject.Type> e : objectTypes.entrySet()) {
			boolean def = caseSensitive ? objectLabels.get(e.getValue()).equals(e.getKey())
                    : objectLabels.get(e.getValue()).equalsIgnoreCase(e.getKey());
			attrs.clear();
			attrs.addAttribute("", "", "code", "CDATA", "" + e.getValue());
			if (def) attrs.addAttribute("", "", "default", "CDATA", "" + def);
			hd.startElement("", "", "object-type", attrs);
			String s = def ? objectLabels.get(e.getValue()) : e.getKey();
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "object-type");
		}
		
		hd.endElement("", "", "object-types");
		
		
		// Write the edges
		
		attrs.clear();
		hd.startElement("", "", "object-attributes", attrs);
		
		for (Entry<String, PObject.Attribute> e : objectAttributeCodes.entrySet()) {
			boolean def = caseSensitive ? objectAttributeNames.get(e.getValue()).equals(e.getKey())
                    : objectAttributeNames.get(e.getValue()).equalsIgnoreCase(e.getKey());
			attrs.clear();
			attrs.addAttribute("", "", "code", "CDATA", "" + e.getValue());
			if (def) attrs.addAttribute("", "", "default", "CDATA", "" + def);
			hd.startElement("", "", "object-attribute", attrs);
			String s = def ? objectAttributeNames.get(e.getValue()) : e.getKey();
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "object-attribute");
		}
		
		hd.endElement("", "", "object-attributes");
		
		
		// Write the edges
		
		attrs.clear();
		hd.startElement("", "", "node-attributes", attrs);
		
		for (Entry<String, PNode.Attribute> e : nodeAttributeCodes.entrySet()) {
			boolean def = caseSensitive ? nodeAttributeNames.get(e.getValue()).equals(e.getKey())
                    : nodeAttributeNames.get(e.getValue()).equalsIgnoreCase(e.getKey());
			attrs.clear();
			attrs.addAttribute("", "", "code", "CDATA", "" + e.getValue());
			if (def) attrs.addAttribute("", "", "default", "CDATA", "" + def);
			hd.startElement("", "", "node-attribute", attrs);
			String s = def ? nodeAttributeNames.get(e.getValue()) : e.getKey();
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "node-attribute");
		}
		
		hd.endElement("", "", "node-attributes");
		
		
		// Finish
		
		hd.endElement("", "", DOM_ELEMENT);
	}
	
	
	/**
	 * Load the graph metadata from an XML element
	 * 
	 * @param element the XML element
	 * @return the graph metadata object
	 * @throws ParserException on DOM parser error
	 */
	public static PMeta loadFromXML(Element element) throws ParserException {
		
		if (!element.getNodeName().equals(DOM_ELEMENT)) {
			throw new ParserException("Expected <" + DOM_ELEMENT + ">, found <" + element.getNodeName() + ">");
		}
		
		
		// Create the object
		
		PMeta meta = new PMeta();
		
		
		// Case sensitivity
		
		meta.caseSensitive = Boolean.parseBoolean(XMLUtils.getAttribute(element, "case-sensitive"));
		
		
		// Parse the metadata
		
		for (Node n = XMLUtils.getSingleElement(element, "edge-types").getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element && "edge-type".equals(n.getNodeName())) {
				Element el = (Element) n;
				boolean def = Boolean.parseBoolean(XMLUtils.getAttribute(el, "default", "false"));
				
				PEdge.Type t = Enum.valueOf(PEdge.Type.class, el.getAttribute("code"));
				meta.addEdgeLabel(el.getTextContent(), t);
				if (def) meta.edgeLabels.put(t, el.getTextContent());
			}
		}
		
		for (Node n = XMLUtils.getSingleElement(element, "object-types").getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element && "object-type".equals(n.getNodeName())) {
				Element el = (Element) n;
				boolean def = Boolean.parseBoolean(XMLUtils.getAttribute(el, "default", "false"));
				
				PObject.Type t = Enum.valueOf(PObject.Type.class, el.getAttribute("code"));
				meta.addObjectExtType(el.getTextContent(), t);
				if (def) meta.objectLabels.put(t, el.getTextContent());
			}
		}
		
		for (Node n = XMLUtils.getSingleElement(element, "object-attributes").getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element && "object-attribute".equals(n.getNodeName())) {
				Element el = (Element) n;
				boolean def = Boolean.parseBoolean(XMLUtils.getAttribute(el, "default", "false"));
				
				PObject.Attribute t = Enum.valueOf(PObject.Attribute.class, el.getAttribute("code"));
				meta.addObjectAttributeCode(el.getTextContent(), t);
				if (def) meta.objectAttributeNames.put(t, el.getTextContent());
			}
		}
		
		for (Node n = XMLUtils.getSingleElement(element, "node-attributes").getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element && "node-attribute".equals(n.getNodeName())) {
				Element el = (Element) n;
				boolean def = Boolean.parseBoolean(XMLUtils.getAttribute(el, "default", "false"));
				
				PNode.Attribute t = Enum.valueOf(PNode.Attribute.class, el.getAttribute("code"));
				meta.addNodeAttributeCode(el.getTextContent(), t);
				if (def) meta.nodeAttributeNames.put(t, el.getTextContent());
			}
		}
		
		
		// Finish
		
		return meta;
	}
}
