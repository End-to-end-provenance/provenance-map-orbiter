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
 * An abstract attribute
 *
 * @author Peter Macko
 */
public abstract class AbstractAttribute implements WithAttribute {
	
	protected String name;
	protected boolean precise;
	protected String operator;
	protected LinkedList<String> operators;
	protected LinkedList<AttributeListener> listeners;


	/**
	 * Create an instance of class Attribute
	 *
	 * @param name the attribute name
	 * @param precise whether the attribute value needs to be set precisely
	 */
	protected AbstractAttribute(String name, boolean precise) {

		this.name = name;
		this.precise = precise;
		
		this.listeners = new LinkedList<AttributeListener>();
		this.operators = new LinkedList<String>();

		this.operators.add("=");
		operator = "=";
	}


	/**
	 * Create an instance of class Attribute
	 *
	 * @param name the attribute name
	 */
	protected AbstractAttribute(String name) {
		this(name, true);
	}
	
	
	/**
	 * Return the name of the attribute
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Return the type of the attribute
	 *
	 * @return true if the value is supposed to be precise
	 */
	public boolean isPrecise() {
		return precise;
	}
	
	
	/**
	 * Determine whether the minimum value of the attribute is set
	 *
	 * @return true if it is set
	 */
	public boolean hasMinimum() {
		return false;
	}
	
	
	/**
	 * Determine whether the maximum value of the attribute is set
	 *
	 * @return true if it is set
	 */
	public boolean hasMaximum() {
		return false;
	}


	/**
	 * Return the string representation of the value of the attribute
	 *
	 * @return the value as string
	 */
	public String toString() {
		return "" + value();
	}
	
	
	/**
	 * Return the expression string
	 * 
	 * @return the expression string
	 */
	public String toExpressionString() {
		String v = toString();
		return name + " " + operator + " " + v;
	}


	/**
	 * Return the attribute
	 *
	 * @return the attribute
	 */
	public AbstractAttribute getAttribute() {
		return this;
	}


	/**
	 * Set an operator
	 *
	 * @param op the operator
	 */
	public void setOperator(String op) {
		if (!operators.contains(op)) throw new RuntimeException("Unsupported operator: " + op);
		operator = op;
		fireAttributeValueChanged();
	}


	/**
	 * Get an operator
	 *
	 * @return the operator
	 */
	public String getOperator() {
		return operator;
	}


	/**
	 * Get the list of supported operators
	 *
	 * @return the list of operators
	 */
	public List<String> getOperators() {
		return operators;
	}


	/**
	 * Add an operator to the list of supported operators
	 *
	 * @param op the operator to add
	 */
	public void addOperator(String op) {
		if (!operators.contains(op)) {
			if (operators.isEmpty()) operator = op;
			operators.add(op);
		}
	}


	/**
	 * Remove an operator from the list of supported operators
	 *
	 * @param op the operator to remove
	 */
	public void removeOperator(String op) {
		operators.remove(op);
		if (operator.equals(op)) {
			if (operators.contains("=")) {
				operator = "=";
			}
			else {
				operator = operators.getFirst();
			}
		}
	}


	/**
	 * Remove all operators from the list of supported operators
	 */
	public void clearOperators() {
		operators.clear();
		operator = "";
	}


	/**
	 * Add a listener
	 *
	 * @param listener the listener to add
	 */
	public void addAttributeListener(AttributeListener listener) {
		if (!listeners.contains(listener)) listeners.add(listener);
	}


	/**
	 * Remove a listener
	 *
	 * @param listener the listener to add
	 */
	public void removeAttributeListener(AttributeListener listener) {
		listeners.remove(listener);
	}


	/**
	 * Fire all callbacks when a value changed
	 */
	protected void fireAttributeValueChanged() {
		for (AttributeListener l : listeners) l.attributeValueChanged(this);
	}


	/**
	 * Callback for when the attribute constraints changed
	 */
	protected void fireAttributeConstraintsChanged() {
		for (AttributeListener l : listeners) l.attributeConstraintsChanged(this);
	}


	/**
	 * Check whether the value is equal
	 *
	 * @param v the other value
	 * @return true if the attribute value is equal to the specified value
	 */
	public boolean valueEquals(Object v) {
		return value().equals(v);
	}


	/**
	 * Set the value to the default value
	 */
	public abstract void setToDefault();


	/**
	 * Return the value as an Object
	 *
	 * @return the stored value
	 */
	public abstract Object value();


	/**
	 * Return the default value as an Object
	 *
	 * @return the stored default value
	 */
	public abstract Object defaultValue();


	/**
	 * Return the class of the values
	 *
	 * @return the class
	 */
	public abstract Class<?> valueClass();
}
