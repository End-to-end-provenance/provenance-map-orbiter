/*
 * A Collection of Miscellaneous Utilities
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

package edu.harvard.util.graph;

import java.io.*;
import java.util.*;


/**
 * An attribute to be attached to every node, but external to the Node objects
 * 
 * @author Peter Macko
 * 
 * @param <T> the type of the node attribute
 */
public class PerNodeAttribute<T> implements Serializable {
	
	private static final long serialVersionUID = -9184650623942194434L;
	
	private String name;
	Vector<T> values;
	
	
	/**
	 * Create an instance of class GraphLayout
	 * 
	 * @param name the attribute name
	 */
	public PerNodeAttribute(String name) {
		this.name = name;
		this.values = new Vector<T>();
	}
	
	
	/**
	 * Return the attribute name
	 * 
	 * @return the attribute name
	 */
	public String getName() {
		return name; 
	}
	
	
	/**
	 * Set a value for a node
	 * 
	 * @param node the node
	 * @param value the new value
	 */
	public void set(BaseNode node, T value) {
		int index = node.getIndex();
		for (int i = values.size(); i <= index; i++) values.add(null);
		values.set(index, value);
	}
	
	
	/**
	 * Retrieve a value for the given node
	 * 
	 * @param node the node
	 * @return the value
	 */
	public T get(BaseNode node) {
		return values.get(node.getIndex());
	}
}
