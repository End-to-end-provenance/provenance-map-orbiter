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

package edu.harvard.pass;

import java.util.*;
import java.util.Map.Entry;

import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import edu.harvard.util.ParserException;
import edu.harvard.util.Utils;
import edu.harvard.util.XMLUtils;


/**
 * A PASS object. Contains what is usually what corresponds to whatever information
 * is stored with the zeroth version in Lasagna
 * 
 * @author Peter Macko
 */
public class PObject implements Comparable<PObject>, java.io.Serializable {
	
	private static final long serialVersionUID = 3849271376566993198L;

	public static final int INVALID_FD = Integer.MIN_VALUE;
	
	public static final String DOM_ELEMENT = "object";
	
	
	/**
	 * Node type
	 */
	public enum Type {
		PROCESS, ARTIFACT, AGENT, OTHER
	}
	
	
	/**
	 * Standard object attributes
	 */
	public enum Attribute {
		NAME, TYPE, OTHER
	}
	
	
	private int fd;
	protected PGraph graph;
	
	private String name;
	private Type type;
	private String extType;
	protected Map<String, String> attributes;

	private int parentFD;
	private String shortName;
	
	protected Vector<PNode> versions;


	/**
	 * Constructor for objects of type PObject
	 * 
	 * @param graph the parent graph
	 * @param fd the file descriptor
	 */
	public PObject(PGraph graph, int fd) {

		this.fd = fd;
		this.graph = graph;

		this.name = null;
		this.type = Type.OTHER;
		this.extType = null;
		this.attributes = null;
		
		this.parentFD = INVALID_FD;
		this.shortName = null;

		this.versions = new Vector<PNode>();
	}


	/**
	 * Create an empty copy of this object
	 * 
	 * @param graph the parent graph
	 */
	public PObject copy(PGraph graph) {

		PObject o = new PObject(graph, fd);

		o.name = name;
		o.shortName = shortName;
		
		o.type = type;
		o.extType = extType;
		
		if (attributes != null) {
			o.attributes = new HashMap<String, String>();
			o.attributes.putAll(attributes);
		}
		
		o.parentFD = parentFD;
		
		return o;
	}


	/**
	 * Return the associated file descriptor
	 *
	 * @return the file descriptor
	 */
	public int getFD() {
		return fd;
	}
	
	
	/**
	 * Return the parent graph
	 * 
	 * @return the parent graph
	 */
	public PGraph getGraph() {
		return graph;
	}


	/**
	 * Return the node name
	 *
	 * @return name
	 */
	public String getName() {
		return name;
	}


	/**
	 * Set the node name
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		
		this.name = name;
		
		
		// Update the short name
		
		shortName = name;
		if (shortName == null) shortName = "";
		
		int slash = shortName.lastIndexOf('/');
		if (slash >= 0) {
			shortName = shortName.substring(slash + 1);
		}
		
		for (PNode n : versions) {
			if (n != null) {
				if (n.useComputedLabel) n.setLabel(n.computeNodeLabel());
			}
		}
	}
	
	
	/**
	 * Get the short name of the object
	 * 
	 * @return the short name
	 */
	public String getShortName() {
		
		if (shortName == null) {
			if (versions.size() == 1) {
				return versions.get(0).getPublicID();
			}
			else {
				return "<null>";
			}
		}
		else {
			return shortName;
		}
	}


	/**
	 * Return the coarse node type
	 *
	 * @return the type code
	 */
	public Type getType() {
		return type;
	}


	/**
	 * Return the node type as a string
	 *
	 * @return the type
	 */
	public String getExtendedType() {
		return extType;
	}


	/**
	 * Set the node type
	 *
	 * @param type the new type
	 * @param extType the extended type represented as a string
	 */
	public void setType(Type type, String extType) {
		this.type = type;
		this.extType = extType;
	}
	
	
	/**
	 * Set an attribute
	 * 
	 * @param code the attribute code
	 * @param value the attribute value
	 */
	public void setAttribute(Attribute code, String value) {
		switch (code) {
		case NAME: setName(value); break;
		case TYPE: setType(graph.meta.getObjectType(value), value); break;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	
	/**
	 * Set an attribute
	 * 
	 * @param key the attribute name
	 * @param value the attribute value
	 */
	public void setAttribute(String key, String value) {
		Attribute code = graph.meta.getObjectAttributeCode(key);
		if (code != Attribute.OTHER) {
			setAttribute(code, value);
		}
		else {
			if (key == null || "".equals(key)) throw new IllegalArgumentException("Invalid attribute name"); 
			if (attributes == null) attributes = new HashMap<String, String>();
			attributes.put(key, value);
		}
	}


	/**
	 * Return an attribute
	 *
	 * @param key the attribute name
	 * @return the value, or null if not found
	 */
	public String getAttribute(String key) {
		
		switch (graph.meta.getObjectAttributeCode(key)) {
		case NAME: return getName();
		case TYPE: return getExtendedType();
		default: return attributes == null ? null : attributes.get(key);
		}
	}
	
	
	/**
	 * Return a map of extended attributes
	 * 
	 * @return a map of extended attributes
	 */
	public Map<String, String> getExtendedAttributes() {
		return attributes == null ? Utils.<Map<String, String>>cast(Collections.EMPTY_MAP) : attributes;
	}


	/**
	 * Return the file descriptor of the parent node (for processes)
	 *
	 * @return the parent's file descriptor
	 */
	public int getParentFD() {
		return parentFD;
	}


	/**
	 * Set the parent node
	 *
	 * @param pfd the new parent FD
	 */
	public void setParentFD(int pfd) {
		this.parentFD = pfd;
	}
	
	
	/**
	 * Get the node that corresponds to the given version
	 *
	 * @param ver the version
	 * @return the given version
	 */
	public PNode getNode(int ver) {
		if (ver < 0) return null;
		if (ver >= versions.size()) return null;
		
		return versions.get(ver);
	}
	

	/**
	 * Set the version node
	 *
	 * @param ver the version
	 * @param node the version node
	 */
	public void setNode(int ver, PNode node) {
		if (ver < 0 || node.getObject() != this) return;
		
		while (versions.size() < ver) versions.add(null);
		
		if (versions.size() <= ver) {
			versions.add(node);
		}
		else {
			versions.set(ver, node);
		}
	}


	/**
	 * Return the most recent version number
	 *
	 * @return the most recent version number
	 */
	public int getLatestVersion() {
		return versions.size() - 1;
	}
	
	
	/**
	 * Return the collection of versions
	 * 
	 * @return all available versions
	 */
	public Vector<PNode> getVersions() {
		return versions;
	}
	
	
	/**
	 * Return the string representation of the object
	 * 
	 * @return the string representation
	 */
	public String toString() {
		return "[" + fd + "] " + name;
	}


	/**
	 * Compare this object with the specified node
	 * 
	 * @param other the other node to compare to
	 * @return a negative number if this &lt; other, 0 if this = other, or a positive number if this &gt; other
	 */
	@Override
	public int compareTo(PObject other) {
		return fd - other.fd;
	}
	
	
	/**
	 * Calculate the hash code
	 * 
	 * @return the hash code
	 */
	public int hashCode() {
		return fd;
	}
	
	
	/**
	 * Indicates whether some other object is "equal to" this one
	 * 
	 * @param obj the other object
	 * @return true if the two are equal
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof PObject)) return false;
		PObject o = (PObject) obj;
		return o.fd == fd && o.graph == graph;
	}

	
	/**
	 * Write the object to XML
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {

		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		attrs.addAttribute("", "", "fd", "CDATA", "" + fd);
		if (parentFD != INVALID_FD) attrs.addAttribute("", "", "parent-fd", "CDATA", "" + parentFD);
		hd.startElement("", "", DOM_ELEMENT, attrs);
		
		
		// Write the basic attributes
		
		if (name != null) {
			attrs.clear();
			hd.startElement("", "", "name", attrs);
			hd.characters(name.toCharArray(), 0, name.length());
			hd.endElement("", "", "name");
		}
		
		if (shortName != null) {
			attrs.clear();
			hd.startElement("", "", "short-name", attrs);
			hd.characters(shortName.toCharArray(), 0, shortName.length());
			hd.endElement("", "", "short-name");
		}
		
		attrs.clear();
		attrs.addAttribute("", "", "code", "CDATA", "" + type);
		hd.startElement("", "", "type", attrs);
		if (extType != null) hd.characters(extType.toCharArray(), 0, extType.length());
		hd.endElement("", "", "type");
		
		
		// Write the attributes
		
		if (attributes != null) {
			attrs.clear();
			hd.startElement("", "", "object-attributes", attrs);
			for (Entry<String, String> e : attributes.entrySet()) {
				attrs.clear();
				attrs.addAttribute("", "", "key", "CDATA", "" + e.getKey());
				hd.startElement("", "", "object-attribute", attrs);
				hd.characters(e.getValue().toCharArray(), 0, e.getValue().length());
				hd.endElement("", "", "object-attribute");
			}
			hd.endElement("", "", "object-attributes");
		}
		
		
		// Write the nodes
		
		attrs.clear();
		hd.startElement("", "", PNode.DOM_ELEMENT + "s", attrs);
		
		for (PNode n : versions) {
			if (n == null) continue;
			n.writeToXML(hd);
		}
		
		hd.endElement("", "", PNode.DOM_ELEMENT + "s");
		
		
		// Finish
		
		hd.endElement("", "", DOM_ELEMENT);
	}
	
	
	/**
	 * Load the object from an XML element
	 * 
	 * @param graph the provenance graph
	 * @param element the XML element
	 * @return the object
	 * @throws ParserException on DOM parser error
	 */
	public static PObject loadFromXML(PGraph graph, Element element) throws ParserException {
		
		if (!element.getNodeName().equals(DOM_ELEMENT)) {
			throw new ParserException("Expected <" + DOM_ELEMENT + ">, found <" + element.getNodeName() + ">");
		}
		
		
		// Attributes
		
		int fd = Integer.parseInt(XMLUtils.getAttribute(element, "fd"));
		
		String s_parentFD = element.getAttribute("parent-fd");
		int parentFD = s_parentFD.length() == 0 ? INVALID_FD : Integer.parseInt(s_parentFD);
		
		
		// Create the object
		
		PObject o = new PObject(graph, fd);
		o.parentFD = parentFD;
		
		
		// Parse the basic properties
		
		o.name = XMLUtils.getTextValue(element, "name", null);
		o.shortName = XMLUtils.getTextValue(element, "short-name", null);
		o.extType = XMLUtils.getTextValue(element, "type");
		o.type = Enum.valueOf(PObject.Type.class, XMLUtils.getSingleElement(element, "type").getAttribute("code"));
		
		
		// Extended attributes
		
		Element attrs = XMLUtils.getSingleOptionalElement(element, "object-attributes");
		if (attrs != null) {
			if (o.attributes == null) o.attributes = new HashMap<String, String>();
			for (org.w3c.dom.Node n = attrs.getFirstChild(); n != null; n = n.getNextSibling()) {
				if (n instanceof Element && "object-attribute".equals(n.getNodeName())) {
					Element e = (Element) n;
					String key = XMLUtils.getAttribute(e, "key");
					o.attributes.put(key, e.getTextContent());
				}
			}
		}
		
		
		// Nodes
		
		for (Node n = XMLUtils.getSingleElement(element, PNode.DOM_ELEMENT + "s").getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element && PNode.DOM_ELEMENT.equals(n.getNodeName())) {
				PNode.loadFromXML(o, (Element) n);
			}
		}
		
		
		// Finish
		
		return o;
	}
}
