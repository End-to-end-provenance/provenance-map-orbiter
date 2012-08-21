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

import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import edu.harvard.util.ParserException;
import edu.harvard.util.XMLUtils;
import edu.harvard.util.graph.*;


/**
 * An edge in the provenance graph
 * 
 * @author Peter Macko
 */
public class PEdge extends Edge<PNode> implements java.io.Serializable {
	
	private static final long serialVersionUID = 2454239871456772999L;
	
	public static final String DOM_ELEMENT = "edge";
	
	
	/**
	 * Edge type
	 */
	public enum Type {
		DATA,		// Data dependency
		CONTROL,	// Control dependency
		VERSION,	// Version edge
		COMPOUND,	// Combination of multiple edges, possibly of different types
		OTHER		// Other
	}
	
	
	private Type type;


	/**
	 * Constructor for objects of type PEdge
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @param type the edge type
	 * @param label the label
	 */
	public PEdge(PNode from, PNode to, Type type, String label) {
		super(from, to, label);
		this.type = type;		
	}


	/**
	 * Constructor for objects of type PEdge
	 * 
	 * @param from the from node
	 * @param to the to node
	 * @param type the edge type
	 */
	public PEdge(PNode from, PNode to, Type type) {
		super(from, to);
		this.label = null;
		this.type = type;		
	}
	
	
	/**
	 * Create an instance of class PEdge using a specified index. Use at your own risk.
	 * 
	 * @param index the edge index
	 * @param from the from node
	 * @param to the to node
	 * @param type the edge type
	 * @param label the label
	 */
	protected PEdge(int index, PNode from, PNode to, Type type, String label) {
		super(index, from, to, label);
		this.type = type;		
	}
	
	
	/**
	 * Get the label
	 * 
	 * @return the label
	 */
	@Override
	public String getLabel() {
		return label == null ? ((PGraph) from.getGraph()).meta.getEdgeLabel(type) : label;
	}
	
	
	/**
	 * Get the edge type
	 * 
	 * @return the edge type
	 */
	public Type getType() {
		return type;
	}


	/**
	 * Write the edge to XML
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {

		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		attrs.addAttribute("", "", "index", "CDATA", "" + getIndex());
		attrs.addAttribute("", "", "from", "CDATA", "" + from.getIndex());
		attrs.addAttribute("", "", "to", "CDATA", "" + to.getIndex());
		attrs.addAttribute("", "", "type", "CDATA", "" + type);
		hd.startElement("", "", DOM_ELEMENT, attrs);
		if (label != null) hd.characters(label.toCharArray(), 0, label.length());
		hd.endElement("", "", DOM_ELEMENT);
	}
	
	
	/**
	 * Load the edge object from an XML element
	 * 
	 * @param graph the provenance graph
	 * @param element the XML element
	 * @return the edge object
	 * @throws ParserException on DOM parser error
	 */
	public static PEdge loadFromXML(PGraph graph, Element element) throws ParserException {
		
		if (!element.getNodeName().equals(DOM_ELEMENT)) {
			throw new ParserException("Expected <" + DOM_ELEMENT + ">, found <" + element.getNodeName() + ">");
		}
		
		
		// Attributes
		
		int index = Integer.parseInt(XMLUtils.getAttribute(element, "index"));
		int from = Integer.parseInt(XMLUtils.getAttribute(element, "from"));
		int to = Integer.parseInt(XMLUtils.getAttribute(element, "to"));
		PEdge.Type type = Enum.valueOf(PEdge.Type.class, XMLUtils.getAttribute(element, "type"));
		String label = element.getTextContent();
		
		
		// Create the object
		
		return new PEdge(index, graph.getNode(from), graph.getNode(to), type, label);
	}
}
