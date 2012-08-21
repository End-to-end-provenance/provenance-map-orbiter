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

import java.io.File;
import java.util.LinkedList;

import javax.swing.filechooser.*;


/**
 * A file filter that acts as a combination of multiple filters
 *
 * @author Peter Macko
 * @version 1.00
 */
public class FileGroupFilter extends FileFilter {
	
	public static final String DEFAULT_NAME = "All Supported Formats";
	
	private String name;
	private LinkedList<FileFilter> filters;
	
	
    /**
	 * Create an empty file filter
	 *
	 * @param name the name of the filter
	 */
	public FileGroupFilter(String name) {
		this.name = name;
		this.filters = new LinkedList<FileFilter>();
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param name the name of the filter
	 * @param filters the array of file filters
	 */
	public FileGroupFilter(String name, FileFilter[] filters) {
		this(name);
		for (int i = 0; i < filters.length; i++) add(filters[i]);
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param filters the array of file filters
	 */
	public FileGroupFilter(FileFilter[] filters) {
		this(DEFAULT_NAME, filters);
	}
	
	
	/**
	 * Return the number of filters in the group
	 * 
	 * @return the number of the filters
	 */
	public int size() {
		return filters.size();
	}
	
	
	/**
	 * Add a filter to the group
	 *
	 * @param filter the filter to add
	 */
	public void add(FileFilter filter) {
		filters.add(filter);
	}
	
	
	/**
	 * Determine whether the given file should be accepted by the filter
	 *
	 * @param f the file
	 * @return true if the file was accepted by the filter
	 */
    public boolean accept(File f) {

    	for (FileFilter filter : filters) {
        	if (filter.accept(f)) return true;
        }
    	
    	return false;
    }
	
	
    /**
	 * Return the name (description) of the filter
	 *
	 * @return the name of the filter
	 */
    public String getDescription() {
        return name;
    }
}
