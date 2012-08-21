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
import java.util.HashSet;
import javax.swing.filechooser.*;


/**
 * A file extension filter.
 *
 * @author Peter Macko
 * @version 1.00
 */
public class FileExtensionFilter extends FileFilter {
	
	private String name;
	private HashSet<String> accept;
	
	
    /**
	 * Create an empty file filter
	 *
	 * @param name the name of the filter
	 */
	public FileExtensionFilter(String name) {
		this.name = name;
		this.accept = new HashSet<String>();
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param name the name of the filter
	 * @param exts the accepted file name extensions
	 */
	public FileExtensionFilter(String name, String[] exts) {
		this(name);
		for (int i = 0; i < exts.length; i++) add(exts[i]);
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param name the name of the filter
	 * @param ext the accepted file name extension
	 */
	public FileExtensionFilter(String name, String ext) {
		this(name);
		add(ext);
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param name the name of the filter
	 * @param ext1 the accepted file name extensions
	 * @param ext2 another accepted file name extensions
	 */
	public FileExtensionFilter(String name, String ext1, String ext2) {
		this(name);
		add(ext1);
		add(ext2);
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param name the name of the filter
	 * @param ext1 the accepted file name extensions
	 * @param ext2 another accepted file name extensions
	 * @param ext3 another accepted file name extensions
	 */
	public FileExtensionFilter(String name, String ext1, String ext2, String ext3) {
		this(name);
		add(ext1);
		add(ext2);
		add(ext3);
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param name the name of the filter
	 * @param ext1 the accepted file name extensions
	 * @param ext2 another accepted file name extensions
	 * @param ext3 another accepted file name extensions
	 * @param ext4 another accepted file name extensions
	 */
	public FileExtensionFilter(String name, String ext1, String ext2, String ext3, String ext4) {
		this(name);
		add(ext1);
		add(ext2);
		add(ext3);
		add(ext4);
	}


    /**
	 * Create a pre-initialized file filter
	 *
	 * @param name the name of the filter
	 * @param ext1 the accepted file name extensions
	 * @param ext2 another accepted file name extensions
	 * @param ext3 another accepted file name extensions
	 * @param ext4 another accepted file name extensions
	 * @param ext5 another accepted file name extensions
	 */
	public FileExtensionFilter(String name, String ext1, String ext2, String ext3, String ext4, String ext5) {
		this(name);
		add(ext1);
		add(ext2);
		add(ext3);
		add(ext4);
		add(ext5);
	}
	
	
	/**
	 * Add a supported file extension
	 *
	 * @param ext the extension to add
	 */
	public void add(String ext) {
		accept.add(ext.toLowerCase());
	}
	
	
	/**
	 * Determine whether the given file should be accepted by the filter
	 *
	 * @param f the file
	 * @return true if the file was accepted by the filter
	 */
    public boolean accept(File f) {
		
        if (f.isDirectory()) return true;
		
        String extension = Utils.getExtension(f);
        return extension != null ? accept.contains(extension.toLowerCase()) : false;
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
