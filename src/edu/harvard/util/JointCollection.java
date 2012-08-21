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

package edu.harvard.util;

import java.util.*;


/**
 * A simple collection that consists of a several smaller (mutable) collections
 * 
 * @author Peter Macko
 *
 * @param <E> the common type for the objects in the sub-collections
 */
public class JointCollection<E> extends AbstractCollection<E> implements java.io.Serializable {

	private static final long serialVersionUID = 1975529649044689081L;
	
	private ArrayList<Collection<? extends E>> collections;
	
	
	/**
	 * Create an instance of class JointCollection
	 */
	public JointCollection() {
		collections = new ArrayList<Collection<? extends E>>();
	}
	
	
	/**
	 * Create an instance of class JointCollection
	 * 
	 * @param collection the collection
	 */
	public JointCollection(Collection<? extends E> collection) {
		this();
		addCollection(collection);
	}
	
	
	/**
	 * Create an instance of class JointCollection
	 * 
	 * @param collection1 the first collection
	 * @param collection2 the second collection
	 */
	public JointCollection(Collection<? extends E> collection1, Collection<? extends E> collection2) {
		this();
		addCollection(collection1);
		addCollection(collection2);
	}
	
	
	/**
	 * Add a collection to the list of collections
	 * 
	 * @param collection the collection to add
	 */
	public void addCollection(Collection<? extends E> collection) {
		collections.add(collection);
	}


	@Override
	public Iterator<E> iterator() {
		return new JointIterator();
	}


	/**
	 * Determine the number of elements in this collection
	 * 
	 * @return the number of elements
	 */
	@Override
	public int size() {
		int s = 0;
		for (Collection<? extends E> c : collections) {
			s += c.size();
		}
		return s;
	}
	
	
	/**
	 * The iterator
	 */
	private class JointIterator implements Iterator<E> {
		
		private Iterator<Collection<? extends E>> mainIterator;
		private Collection<? extends E> current;
		private Iterator<Collection<E>> subIterator;
		
		
		/**
		 * Create an instance of class JointIterator
		 */
		public JointIterator() {
			
			mainIterator = collections.iterator();
			
			current = mainIterator.hasNext() ? mainIterator.next() : null;
			subIterator = current == null ? null : Utils.<Iterator<Collection<E>>>cast(current.iterator());
		}

		
		/**
		 * Determine whether we have the next element
		 * 
		 * @return true if there are more elements
		 */
		@Override
		public boolean hasNext() {
			
			if (current == null) return false;
			if (subIterator.hasNext()) return true;
			
			
			while (mainIterator.hasNext()) {
				current = mainIterator.next();
				subIterator = Utils.<Iterator<Collection<E>>>cast(current.iterator());
				if (subIterator.hasNext()) return true;
			}
			
			return false;
		}

		
		/**
		 * Get the next element
		 * 
		 * @return the next element
		 */
		@Override
		public E next() {
			
			if (current == null) throw new NoSuchElementException();
			if (subIterator.hasNext()) return Utils.<E>cast(subIterator.next());
			
			while (mainIterator.hasNext()) {
				current = mainIterator.next();
				subIterator = Utils.<Iterator<Collection<E>>>cast(current.iterator());
				if (subIterator.hasNext()) return Utils.<E>cast(subIterator.next());
			}
			
			throw new NoSuchElementException();
		}

		
		/**
		 * Remove an element (disabled)
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
