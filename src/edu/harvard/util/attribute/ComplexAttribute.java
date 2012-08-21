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

package edu.harvard.util.attribute;
import java.util.*;


/**
 * A complex attribute
 *
 * @author Peter Macko
 */
public class ComplexAttribute extends AbstractAttribute {
	
	protected LinkedList<AbstractAttribute> attributes;
	protected HashMap<String, AbstractAttribute> attributeMap;
	protected Listener listener;


	/**
	 * Create an instance of class Attribute
	 *
	 * @param name the attribute name
	 */
	public ComplexAttribute(String name) {
		super(name);
		attributes = new LinkedList<AbstractAttribute>();
		attributeMap = new HashMap<String, AbstractAttribute>();
		listener = new Listener();
	}


	/**
	 * Return the attributes of the filter
	 *
	 * @return the list of attributes
	 */
	public LinkedList<AbstractAttribute> getAttributes() {
		return attributes;
	}


	/**
	 * Add an attribute
	 *
	 * @param attr the attribute
	 */
	public void add(AbstractAttribute attr) {
		attributes.add(attr);
		attributeMap.put(attr.getName(), attr);
		attr.addAttributeListener(listener);
		fireAttributeValueChanged();
	}


	/**
	 * Get a specific attribute
	 *
	 * @param name the attribute name
	 * @return the given attribute
	 */
	public AbstractAttribute get(String name) {
		return attributeMap.get(name);
	}


	/**
	 * Return the number of attributes
	 *
	 * @return the number of attributes
	 */
	public int size() {
		return attributes.size();
	}


	/**
	 * Set the value to the default value
	 */
	public void setToDefault() {
		
		// I suppose it makes the most sense to let the children have the default values...
		
		for (AbstractAttribute a : attributes) {
			a.setToDefault();
		}
	}
	
	
	/**
	 * Return the expression string
	 * 
	 * @return the expression string
	 */
	public String toExpressionString() {
		
		if (attributes.size() == 0) return "";
		if (attributes.size() == 1) return attributes.getFirst().toExpressionString();
		
		StringBuilder b = new StringBuilder();
		boolean first = true;
		
		for (AbstractAttribute a : attributes) {
			String s = a.toExpressionString();
			if (s.length() == 0) continue;
			
			if (first) {
				first = false;
			}
			else {
				b.append(" and ");
			}
			
			b.append("(");
			b.append(s);
			b.append(")");
		}
		
		return b.toString();
	}


	/**
	 * Return the value as an Object
	 *
	 * @return the stored value
	 */
	public Object value() {
		return attributes;
	}


	/**
	 * Return the default value as an Object
	 *
	 * @return the stored default value
	 */
	public Object defaultValue() {
		return new LinkedList<AbstractAttribute>();
	}


	/**
	 * Return the class of the values
	 *
	 * @return the class
	 */
	public Class<?> valueClass() {
		return Collection.class;
	}


	/**
	 * A listener for changes in the children attributes
	 */
	private class Listener implements AttributeListener {

		/**
		* Callback for when the attribute value changed
		*
		* @param attr the attribute
		*/
		public void attributeValueChanged(AbstractAttribute attr) {
		
			// Forward the event
			
			for (AttributeListener l : listeners) l.attributeValueChanged(attr);
		}

		/**
		* Callback for when the attribute constraints changed
		*
		* @param attr the attribute
		*/
		public void attributeConstraintsChanged(AbstractAttribute attr) {
		
			// Forward the event
			
			for (AttributeListener l : listeners) l.attributeConstraintsChanged(attr);
		}
	}
}
