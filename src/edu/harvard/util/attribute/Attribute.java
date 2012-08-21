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

import java.text.DecimalFormat;


/**
 * An attribute
 *
 * @author Peter Macko
 */
public class Attribute<T> extends AbstractAttribute {
	
	private final DecimalFormat defaultDecimalFormat = new DecimalFormat("#.##"); 
	
	private T value;
	private T def;
	private Class<?> valueClass;
	
	private T min;
	private T max;
	
	
	/**
	 * Create an instance of class Attribute
	 *
	 * @param name the attribute name
	 * @param precise whether the attribute value needs to be set precisely
	 * @param def the default value (cannot be null)
	 */
	public Attribute(String name, boolean precise, T def) {
		super(name, precise);
		
		this.def = def;
		this.value = def;
		this.valueClass = def.getClass();
		
		min = null;
		max = null;

		addOperator("!=");
		if (def instanceof Comparable<?>) {
			addOperator("<");
			addOperator("<=");
			addOperator(">");
			addOperator(">=");
		}
		addOperator("~");
		addOperator("!~");
	}
	
	
	/**
	 * Set the value and check the range
	 *
	 * @param v the value
	 */
	@SuppressWarnings("unchecked")
	public void set(T v) {

		boolean violated_min = min != null ? ((Comparable<T>) v).compareTo(min) < 0 : false;
		boolean violated_max = max != null ? ((Comparable<T>) v).compareTo(max) > 0 : false;
		if (violated_min || violated_max) {
			if (max == null) throw new RuntimeException("The new value cannot be less than " + min);
			if (min == null) throw new RuntimeException("The new value cannot be greater than " + max);
			throw new RuntimeException("The new value needs to be between " + min + " and " + max);
		}

		value = v;
		fireAttributeValueChanged();
	}
	
	
	/**
	 * Set the value and check the range, but fail silently
	 *
	 * @param v the value
	 */
	@SuppressWarnings("unchecked")
	public void setWithSilentFail(T v) {

		boolean violated_min = min != null ? ((Comparable<T>) v).compareTo(min) < 0 : false;
		boolean violated_max = max != null ? ((Comparable<T>) v).compareTo(max) > 0 : false;
		if (violated_min) v = min;
		if (violated_max) v = max;

		value = v;
		fireAttributeValueChanged();
	}
	
	
	/**
	 * Set the default value and check the range
	 *
	 * @param v the value
	 */
	@SuppressWarnings("unchecked")
	public void setDefaultValue(T v) {

		boolean violated_min = min != null ? ((Comparable<T>) v).compareTo(min) < 0 : false;
		boolean violated_max = max != null ? ((Comparable<T>) v).compareTo(max) > 0 : false;
		if (violated_min || violated_max) {
			if (max == null) throw new RuntimeException("The new value cannot be less than " + min);
			if (min == null) throw new RuntimeException("The new value cannot be greater than " + max);
			throw new RuntimeException("The new value needs to be between " + min + " and " + max);
		}

		def = v;
	}
	
	
	/**
	 * Force the current value to be within the bounds
	 */
	public void enforceConstraints() {
		setWithSilentFail(value);
	}
	
	
	/**
	 * Return the value
	 *
	 * @return the value
	 */
	public T get() {
		return value;
	}
	
	
	/**
	 * Set the minimum value of the attribute (inclusive)
	 *
	 * @param v the value
	 */
	@SuppressWarnings("unchecked")
	public void setMinimum(T v) {
		min = v;
		
		fireAttributeConstraintsChanged();
		if (min != null) if (((Comparable<T>) value).compareTo(min) < 0) set(min);
	}
	
	
	/**
	 * Set the maximum value of the attribute (inclusive)
	 *
	 * @param v the value
	 */
	@SuppressWarnings("unchecked")
	public void setMaximum(T v) {
		max = v;
		
		fireAttributeConstraintsChanged();
		if (max != null) if (((Comparable<T>) value).compareTo(max) > 0) set(max);
	}
	
	
	/**
	 * Set the range of the attribute
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 */
	@SuppressWarnings("unchecked")
	public void setRange(T min, T max) {
		this.min = min;
		this.max = max;
		
		fireAttributeConstraintsChanged();
		if (min != null) if (((Comparable<T>) value).compareTo(min) < 0) set(min);
		if (max != null) if (((Comparable<T>) value).compareTo(max) > 0) set(max);
	}
	
	
	/**
	 * Return the minimum value of the attribute
	 *
	 * @return the value
	 */
	public T getMinimum() {
		return min;
	}
	
	
	/**
	 * Return the maximum value of the attribute
	 *
	 * @return the value
	 */
	public T getMaximum() {
		return max;
	}
	
	
	/**
	 * Determine whether the minimum value of the attribute is set
	 *
	 * @return true if it is set
	 */
	public boolean hasMinimum() {
		return min != null;
	}
	
	
	/**
	 * Determine whether the maximum value of the attribute is set
	 *
	 * @return true if it is set
	 */
	public boolean hasMaximum() {
		return max != null;
	}
	
	
	/**
	 * Return the string value
	 *
	 * @return the value
	 */
	public String toString() {
		
		if (value instanceof Double || value instanceof Float) {
			return defaultDecimalFormat.format(value);
		}
		
		return "" + value;
	}


	/**
	 * Compare the given value to the attribute value, assuming the given value is on the left
	 *
	 * @param v the given value to compare
	 * @return true if the condition is satisfied
	 */
	@SuppressWarnings("unchecked")
	public boolean compareLeft(T v) {
		if (v == null || value == null) return false;

		if (operator.equals("=" )) return  v.equals(value);
		if (operator.equals("!=")) return !v.equals(value);

		if (v instanceof Comparable) {
			Comparable<T> cv = (Comparable<T>) v;

			if (operator.equals("<" )) return cv.compareTo(value) <  0;
			if (operator.equals("<=")) return cv.compareTo(value) <= 0;
			if (operator.equals(">" )) return cv.compareTo(value) >  0;
			if (operator.equals(">=")) return cv.compareTo(value) >= 0;
		}

		if (operator.equals("~" )) return v.toString().indexOf(value.toString()) >= 0;
		if (operator.equals("!~")) return v.toString().indexOf(value.toString()) <  0;

		throw new RuntimeException("Unsupported operator: " + operator);
	}


	/**
	 * Set the value to the default value
	 */
	public void setToDefault() {
		set(def);
	}
	
	
	/**
	 * Return the value as an Object
	 *
	 * @return the stored value
	 */
	public Object value() {
		return value;
	}
	
	
	/**
	 * Return the default value as an Object
	 *
	 * @return the stored default value
	 */
	public Object defaultValue() {
		return def;
	}


	/**
	 * Return the class of the values
	 *
	 * @return the class
	 */
	public Class<?> valueClass() {
		return valueClass;
	}
}

