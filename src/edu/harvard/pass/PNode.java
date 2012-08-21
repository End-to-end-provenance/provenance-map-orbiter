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

import edu.harvard.util.ParserException;
import edu.harvard.util.Utils;
import edu.harvard.util.XMLUtils;
import edu.harvard.util.graph.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * A node in a provenance graph
 * 
 * @author Peter Macko
 */
public class PNode extends Node<PNode, PEdge> implements Serializable, WithTimeInterval {
	
	private static final long serialVersionUID = 5390207287414090359L;
	
	public static final String DOM_ELEMENT = "node";
	
	
	/**
	 * Standard object attributes
	 */
	public enum Attribute {
		FREEZETIME, TIME, LABEL, OTHER
	}
	
	
	/*
	 * Basic node properties
	 */
	PObject object;
	int ver;
	String id;
	boolean useComputedLabel;
	
	
	/*
	 * Node attributes
	 */
	
	private double time;
	
	protected Map<String, String> attributes;
	
	
	/*
	 * PASS-specific fields
	 */
	private double freezeTime;
	
	
	/*
	 * Graph algorithm fields
	 */
	private transient double aux;
	private double provrank;


	/**
	 * Constructor for objects of type PNode
	 * 
	 * @param object the provenance object
	 * @param ver the version
	 * @param id the string ID
	 */
	public PNode(PObject object, int ver, String id) {
		
		this(-1, object, ver, id);
	}


	/**
	 * Constructor for objects of type PNode
	 * 
	 * @param object the provenance object
	 * @param ver the version
	 */
	public PNode(PObject object, int ver) {
		
		this(-1, object, ver, null);
	}
	
	
	/**
	 * Create an instance of PNode using a specified index. Use at your own risk.
	 * 
	 * @param index the index
	 * @param object the provenance object
	 * @param ver the version
	 * @param id the string ID
	 */
	protected PNode(int index, PObject object, int ver, String id) {
		super(index);
		
		this.object = object;
		this.ver = ver;
		this.id = id;
		
		this.freezeTime = 0;				// Not set
		this.time = Double.MIN_VALUE;		// Not set
		this.attributes = null;
		
		this.aux = 0;
		this.provrank = Double.MIN_VALUE;	// Not set
		this.useComputedLabel = true;
		
		object.setNode(ver, this);
		if (this.useComputedLabel) setLabel(computeNodeLabel());
	}
	
	
	/**
	 * Create a copy of the node
	 * 
	 * @param object the PObject the copy/clone should be associated with
	 * @return the cloned object
	 */
	public PNode copy(PObject object) {
		
		PNode p = new PNode(object, ver);
		p.time = time;
		p.freezeTime = freezeTime;
		
		p.provrank = provrank;
		p.id = id;
		p.useComputedLabel = useComputedLabel;
		
		if (p.useComputedLabel) p.setLabel(p.computeNodeLabel());
		p.setVisible(isVisible());
		
		return p;
	}
	
	
	/**
	 * Return the node's publicly visible ID
	 * 
	 * @return the public ID
	 */
	public String getPublicID() {
		return id == null ? "" + object.getFD() + "." + ver : id;
	}
	
	
	/**
	 * Compute the label of a node
	 * 
	 * @return the node label
	 */
	protected String computeNodeLabel() {
		
		String name = getObject().getShortName();
		if (name == null) name = "";
		
		if ("".equals(name) || "<null>".equals(name)) {
			return getPublicID();
		}
		
		if (getPublicID().equals(name)) {
			return name;
		}
		
		return getPublicID() + " " + name;
	}
	
	
	/**
	 * Get the provenance object
	 * 
	 * @return the provenance object
	 */
	public PObject getObject() {
		return object;
	}
	
	
	/**
	 * Get the file descriptor
	 * 
	 * @return the file descriptor
	 */
	public int getFD() {
		return object.getFD();
	}
	
	
	/**
	 * Get the object version
	 * 
	 * @return the version
	 */
	public int getVersion() {
		return ver;
	}
	
	
	/**
	 * Get the freeze time
	 * 
	 * @return the freeze time
	 */
	public double getFreezeTime() {
		return freezeTime;
	}
	
	
	/**
	 * Set the freeze time
	 * 
	 * @param t the new freeze time
	 */
	public void setFreezeTime(double t) {
		freezeTime = t;
	}


	/**
	 * Get the node timestamp. If not set, it will be computed from PASS's FREEZETIME.
	 *
	 * @return the time
	 */
	public double getTime() {
		
		if (time != Double.MIN_VALUE) return time - object.getGraph().getTimeBase();
		
		double T = (getPrev() == null ? freezeTime : getPrev().getFreezeTime()) - object.getGraph().getTimeBase();
		if (T < 0) T = 0;	// Do not allow negative time
		return T;
	}

	
	/**
	 * Get the start time
	 * 
	 * @return the start time
	 */
	public double getStartTime() {
		return getTime();
	}
	
	
	/**
	 * Get the end time
	 * 
	 * @return the end time
	 */
	public double getEndTime() {
		return getTime();
	}


	/**
	 * Get the node timestamp (unadjusted). If not set, it will be computed from PASS's FREEZETIME.
	 *
	 * @return the time
	 */
	public double getTimeUnadjusted() {
		
		if (time != Double.MIN_VALUE) return time;
		
		double T = getPrev() == null ? freezeTime : getPrev().getFreezeTime();
		if (T < 0) T = 0;	// Do not allow negative time
		return T;
	}
	
	
	/**
	 * Set the timestamp
	 * 
	 * @param t the new time
	 */
	public void setTime(double t) {
		time = t;
	}


	/**
	 * Get the previous version of this node
	 *
	 * @return the previous version, or null if none
	 */
	public PNode getPrev() {
		return ver == 0 ? null : object.getNode(ver - 1);
	}


	/**
	 * Get the next version of this node
	 *
	 * @return the next version, or null if none
	 */
	public PNode getNext() {
		return object.getNode(ver + 1);
	}
	
	
	/**
	 * Set an attribute
	 * 
	 * @param code the attribute code
	 * @param value the attribute value
	 */
	public void setAttribute(Attribute code, String value) {
		switch (code) {
		case TIME: setTime(Double.parseDouble(value)); break;
		case FREEZETIME: setFreezeTime(Double.parseDouble(value)); break;
		case LABEL: setLabel(value); useComputedLabel = false; break;
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
		Attribute code = object.graph.meta.getNodeAttributeCode(key);
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
		
		switch (object.graph.meta.getNodeAttributeCode(key)) {
		case TIME: return "" + getTime();
		case FREEZETIME: return "" + getFreezeTime();
		case LABEL: return getLabel();
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
	 * Get the current AUX value (used as a per-node temporary value by graph algorithms)
	 * 
	 * @return the AUX value
	 */
	public double getAux() {
		return aux;
	}
	
	
	/**
	 * Add to the AUX value (used as a per-node temporary value by graph algorithms)
	 * 
	 * @param v the value to add
	 */
	public void addAux(double v) {
		this.aux += v;
	}
	
	
	/**
	 * Set the AUX value (used as a per-node temporary value by graph algorithms)
	 * 
	 * @param aux the new value
	 */
	public void setAux(double aux) {
		this.aux = aux;
	}
	
	
	/**
	 * Get the ProvRank of the node
	 * 
	 * @return the ProvRank
	 */
	public double getProvRank() {
		return provrank;
	}
	
	
	/**
	 * Set the ProvRank of the node
	 * 
	 * @param rank the new value of ProvRank
	 */
	public void setProvRank(double rank) {
		this.provrank = rank;
	}
	
	
	/**
	 * Calculate the hash code
	 * 
	 * @return the hash code
	 */
	public int hashCode() {
		return (object.getFD() << 16) | ver;
	}
	
	
	/**
	 * Indicates whether some other object is "equal to" this one
	 * 
	 * @param obj the other object
	 * @return true if the two are equal
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof PNode)) return false;
		PNode p = (PNode) obj;
		return p.object.getFD() == object.getFD() && p.ver == ver;
	}
	
	
	/**
	 * Return the string version of the object
	 * 
	 * @return the string version
	 */
	public String toString() {
		return getPublicID();
	}
	
	
	/**
	 * Return the header for the CSV version of the object
	 * 
	 * @return the CSV header string
	 */
	public static String getHeaderCSV() {
		return "FD,Version,Name,Type,Time,Indegree,Outdegree,ProvRank";
	}
	
	
	/**
	 * Return the CSV version of the object
	 * 
	 * @return the CSV string
	 */
	public String toCSV() {
		StringBuilder b = new StringBuilder();
		
		b.append(object.getFD());
		b.append(","); b.append(getVersion());
		b.append(","); b.append("\"" + object.getName() + "\"");
		b.append(","); b.append("\"" + object.getType() + "\"");
		b.append(","); b.append(getTime());
		b.append(","); b.append(getIncomingEdges().size());
		b.append(","); b.append(getOutgoingEdges().size());
		b.append(","); b.append(Utils.LONG_DECIMAL_FORMAT.format(getProvRank()));
		
		return b.toString();
	}


	/**
	 * Compare this node with the specified node
	 * 
	 * @param other the other node to compare to
	 * @return a negative number if this &lt; other, 0 if this = other, or a positive number if this &gt; other
	 */
	@Override
	public int compareTo(BaseNode other) {
		
		if (!(other instanceof PNode)) {
			throw new IllegalArgumentException("Cannot compare different types of nodes");
		}
		
		int r = object.compareTo(((PNode) other).object);
		if (r != 0) return r;
		
		return ver - ((PNode) other).ver;
	}
	
	
	/**
	 * Node comparator by ProvRank first
	 * 
	 * @author Peter Macko
	 */
	public static class ProvRankComparator implements Comparator<PNode> {

		/**
		 * Compare two nodes
		 * 
		 * @param A the first node
		 * @param B the second node
		 * @return a negative number if A &lt; B, 0 if A = B, or a positive number if A &gt; B
		 */
		@Override
		public int compare(PNode A, PNode B) {
			
			if (A.provrank < B.provrank) return -1;
			if (A.provrank > B.provrank) return  1;
			
			return A.compareTo(B);
		}
	}

	
	/**
	 * Write the node to XML. It is expected that the resulting
	 * XML would be placed within the context of the corresponding
	 * PObject.
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {

		String s;
		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		if (id != null) attrs.addAttribute("", "", "public-id", "CDATA", "" + id);
		attrs.addAttribute("", "", "id", "CDATA", "" + getID());
		attrs.addAttribute("", "", "index", "CDATA", "" + getIndex());
		attrs.addAttribute("", "", "ver", "CDATA", "" + ver);
		if (!isVisible()) attrs.addAttribute("", "", "visible", "CDATA", "" + isVisible());
		hd.startElement("", "", DOM_ELEMENT, attrs);
		
		
		// Write the basic attributes
		
		attrs.clear();
		
		if (!useComputedLabel) {
			s = getLabel();
			hd.startElement("", "", "label", attrs);
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "label");
		}
		
		if (time != Double.MIN_VALUE) {
			s = Double.toHexString(time);
			hd.startElement("", "", "time", attrs);
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "time");
		}
		
		if (freezeTime != Double.MIN_VALUE && freezeTime != 0) {
			s = Double.toHexString(freezeTime);
			hd.startElement("", "", "freeze-time", attrs);
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "freeze-time");
		}
		
		if (provrank != Double.MIN_VALUE) {
			s = Double.toHexString(provrank);
			hd.startElement("", "", "provrank", attrs);
			hd.characters(s.toCharArray(), 0, s.length());
			hd.endElement("", "", "provrank");
		}
		
		
		// Write the attributes
		
		if (attributes != null) {
			attrs.clear();
			hd.startElement("", "", "node-attributes", attrs);
			for (Entry<String, String> e : attributes.entrySet()) {
				attrs.clear();
				attrs.addAttribute("", "", "key", "CDATA", "" + e.getKey());
				hd.startElement("", "", "node-attribute", attrs);
				hd.characters(e.getValue().toCharArray(), 0, e.getValue().length());
				hd.endElement("", "", "node-attribute");
			}
			hd.endElement("", "", "node-attributes");
		}
		
		
		// Finish
		
		hd.endElement("", "", DOM_ELEMENT);
	}
	
	
	/**
	 * Load the node from an XML element
	 * 
	 * @param object the provenance object
	 * @param element the XML element
	 * @return the node object
	 * @throws ParserException on DOM parser error
	 */
	public static PNode loadFromXML(PObject object, Element element) throws ParserException {
		
		if (!element.getNodeName().equals(DOM_ELEMENT)) {
			throw new ParserException("Expected <" + DOM_ELEMENT + ">, found <" + element.getNodeName() + ">");
		}
		
		
		// Attributes
		
		int ver = Integer.parseInt(XMLUtils.getAttribute(element, "ver"));
		int index = Integer.parseInt(XMLUtils.getAttribute(element, "index"));
		int id = Integer.parseInt(XMLUtils.getAttribute(element, "id"));
		boolean visible = Boolean.parseBoolean(XMLUtils.getAttribute(element, "visible", "true"));
		String publicID = XMLUtils.getAttribute(element, "public-id", null);
		
		
		// Create the object
		
		PNode node = new PNode(index, object, ver, publicID);
		node.setID(id);
		node.setVisible(visible);
		
		
		// Parse the basic properties
		
		String label = XMLUtils.getTextValue(element, "label", null);
		if (label != null) {
			node.useComputedLabel = false;
			node.setLabel(label);
		}
		
		String s_time = XMLUtils.getTextValue(element, "time", null);
		if (s_time != null) node.time = Double.parseDouble(s_time);
		
		String s_freezeTime = XMLUtils.getTextValue(element, "freeze-time", null);
		if (s_freezeTime != null) node.freezeTime = Double.parseDouble(s_freezeTime);
		
		String s_provrank = XMLUtils.getTextValue(element, "provrank", null);
		if (s_provrank != null) node.provrank = Double.parseDouble(s_provrank);
		
		
		// Extended attributes
		
		Element attrs = XMLUtils.getSingleOptionalElement(element, "node-attributes");
		if (attrs != null) {
			if (node.attributes == null) node.attributes = new HashMap<String, String>();
			for (org.w3c.dom.Node n = attrs.getFirstChild(); n != null; n = n.getNextSibling()) {
				if (n instanceof Element && "node-attribute".equals(n.getNodeName())) {
					Element e = (Element) n;
					String key = XMLUtils.getAttribute(e, "key");
					node.attributes.put(key, e.getTextContent());
				}
			}
		}
		
		
		// Finish
		
		return node;
	}
}
