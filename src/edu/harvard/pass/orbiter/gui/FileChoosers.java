/*
 * Provenance Map Orbiter: A visualization tool for large provenance graphs
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

package edu.harvard.pass.orbiter.gui;

import edu.harvard.pass.orbiter.document.*;
import edu.harvard.util.*;

import java.awt.*;
import java.io.*;
import javax.swing.*;


/**
 * A collection of file choosers
 * 
 * @author Peter Macko
 */
public class FileChoosers {
	
	private static FileExtensionFilter documentFilter;
	
	private static FileExtensionFilter twigFilter;
	private static FileExtensionFilter opmFilter;
	private static FileExtensionFilter rdfFilter;
	private static FileExtensionFilter phyloxmlFilter;
	private static FileExtensionFilter ddgFilter;
	
	private static FileExtensionFilter csvFilter;
	private static FileExtensionFilter graphvizFilter;
	
	private static File lastChosenGraphFile = null;
	private static File lastChosenDocumentFile = null;
	private static File lastChosenCSVFile = null;
	private static File lastChosenGraphvizFile = null;
	private static File lastChosenExportFile = null;


	/**
	 * Initialize
	 */
	static {
		
		documentFilter = new FileExtensionFilter(Document.DESCRIPTION + " (*." + Document.EXTENSION + ", *."
				+ Document.EXTENSION_COMPRESSED + ")", Document.EXTENSION, Document.EXTENSION_COMPRESSED);
		
		twigFilter = new FileExtensionFilter("PASS Twig File (*.twig, *.twig_dump)", "twig", "twig_dump");
		opmFilter = new FileExtensionFilter("OPM File (*.opm, *.n3, *.xml, *.rdf)", "opm", "n3", "xml", "rdf");
		rdfFilter = new FileExtensionFilter("RDF File (*.n3, *.nt, *.ttl, *.xml, *.rdf)", "n3", "nt", "ttl", "xml", "rdf");
		phyloxmlFilter = new FileExtensionFilter("PhyloXML file (*.phyloxml, *.xml)", "phyloxml", "xml");
		ddgFilter = new FileExtensionFilter("DDG file (*.ddg, *.txt)", "ddg", "txt");
		
		csvFilter = new FileExtensionFilter("CSV file (*.csv)", "csv");
		graphvizFilter = new FileExtensionFilter("Graphviz file (*.dot, *.gv, *.txt)", "dot", "gv", "txt");
	}
	
	
	/**
	 * Choose a graph to open
	 */
	public static File chooseGraphFile(Component parent, String title) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		if (lastChosenGraphFile != null) fc.setSelectedFile(lastChosenGraphFile);
		
		fc.setAcceptAllFileFilterUsed(false);
		if (!Utils.isLinux()) {
			fc.addChoosableFileFilter(rdfFilter);
			fc.addChoosableFileFilter(phyloxmlFilter);
			fc.addChoosableFileFilter(opmFilter);
			fc.addChoosableFileFilter(ddgFilter);
			fc.addChoosableFileFilter(twigFilter);
			fc.addChoosableFileFilter(documentFilter);
		}
		else {
			fc.addChoosableFileFilter(documentFilter);
			fc.addChoosableFileFilter(twigFilter);
			fc.addChoosableFileFilter(ddgFilter);
			fc.addChoosableFileFilter(opmFilter);
			fc.addChoosableFileFilter(phyloxmlFilter);
			fc.addChoosableFileFilter(rdfFilter);
		}
		
		FileGroupFilter all = new FileGroupFilter(fc.getChoosableFileFilters());
		if (all.size() > 1) {
			fc.addChoosableFileFilter(all);
			fc.setFileFilter(all);
		}
		
		int r = fc.showOpenDialog(parent);
		if (r != JFileChooser.APPROVE_OPTION) return null;
		
		lastChosenGraphFile = fc.getSelectedFile();
		if (Document.EXTENSION.equals(Utils.getExtension(lastChosenGraphFile))) lastChosenDocumentFile = lastChosenGraphFile;
		return lastChosenGraphFile;
	}


	/**
	 * Choose a document file to open or save
	 */
	public static File chooseDocumentFile(Component parent, String title, boolean open) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setDialogType(open ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);
		if (lastChosenDocumentFile != null) fc.setSelectedFile(lastChosenDocumentFile);

		fc.setAcceptAllFileFilterUsed(false);
		fc.addChoosableFileFilter(documentFilter);
		
		FileGroupFilter all = new FileGroupFilter(fc.getChoosableFileFilters());
		if (all.size() > 1) {
			fc.addChoosableFileFilter(all);
			fc.setFileFilter(all);
		}

		int r = 0;
		if (open) {
			r = fc.showOpenDialog(parent);
		}
		else {
			r = fc.showSaveDialog(parent);
		}
		if (r != JFileChooser.APPROVE_OPTION) return null;

		lastChosenDocumentFile = fc.getSelectedFile();
		return lastChosenDocumentFile;
	}


	/**
	 * Choose a CSV file to open or save
	 */
	public static File chooseCSVFile(Component parent, String title, boolean open) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setDialogType(open ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);
		if (lastChosenCSVFile != null) fc.setSelectedFile(lastChosenCSVFile);

		fc.setAcceptAllFileFilterUsed(false);
		fc.addChoosableFileFilter(csvFilter);
		
		FileGroupFilter all = new FileGroupFilter(fc.getChoosableFileFilters());
		if (all.size() > 1) {
			fc.addChoosableFileFilter(all);
			fc.setFileFilter(all);
		}

		int r = 0;
		if (open) {
			r = fc.showOpenDialog(parent);
		}
		else {
			r = fc.showSaveDialog(parent);
		}
		if (r != JFileChooser.APPROVE_OPTION) return null;

		lastChosenCSVFile = fc.getSelectedFile();
		return lastChosenCSVFile;
	}


	/**
	 * Choose a Graphviz file to open or save
	 */
	public static File chooseGraphvizFile(Component parent, String title, boolean open) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setDialogType(open ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);
		if (lastChosenGraphvizFile != null) fc.setSelectedFile(lastChosenGraphvizFile);

		fc.setAcceptAllFileFilterUsed(false);
		fc.addChoosableFileFilter(graphvizFilter);
		
		FileGroupFilter all = new FileGroupFilter(fc.getChoosableFileFilters());
		if (all.size() > 1) {
			fc.addChoosableFileFilter(all);
			fc.setFileFilter(all);
		}

		int r = 0;
		if (open) {
			r = fc.showOpenDialog(parent);
		}
		else {
			r = fc.showSaveDialog(parent);
		}
		if (r != JFileChooser.APPROVE_OPTION) return null;

		lastChosenGraphvizFile = fc.getSelectedFile();
		return lastChosenGraphvizFile;
	}


	/**
	 * Choose a file to export
	 */
	public static File chooseExportFile(Component parent, String title) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setDialogType(JFileChooser.SAVE_DIALOG);
		if (lastChosenExportFile != null) fc.setSelectedFile(lastChosenExportFile);

		fc.setAcceptAllFileFilterUsed(false);
		if (!Utils.isLinux()) {
			fc.addChoosableFileFilter(graphvizFilter);
			fc.addChoosableFileFilter(rdfFilter);
		}
		else {
			fc.addChoosableFileFilter(rdfFilter);
			fc.addChoosableFileFilter(graphvizFilter);
		}
		
		FileGroupFilter all = new FileGroupFilter(fc.getChoosableFileFilters());
		if (all.size() > 1) {
			fc.addChoosableFileFilter(all);
			fc.setFileFilter(all);
		}

		int r = fc.showSaveDialog(parent);
		if (r != JFileChooser.APPROVE_OPTION) return null;

		lastChosenExportFile = fc.getSelectedFile();
		return lastChosenExportFile;
	}
}
