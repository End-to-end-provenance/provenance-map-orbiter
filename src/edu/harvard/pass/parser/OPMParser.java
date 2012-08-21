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

package edu.harvard.pass.parser;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.openprovenance.model.*;
import org.openprovenance.model.Process;

import edu.harvard.pass.*;
import edu.harvard.util.ParserException;
import edu.harvard.util.ParserFormatException;
import edu.harvard.util.Utils;
import edu.harvard.util.gui.*;


/**
 * A parser for OPM files
 * 
 * @author Peter Macko
 */
public class OPMParser implements Parser, HasWizardPanelConfigGUI {
	
	protected static boolean DEBUG = false;
	
	protected File file;
	protected OPMGraph opm;
	
	protected String account;
	protected List<String> accounts;

	
	/**
	 * Create an instance of class OPMParser
	 */
	public OPMParser() {
		
		file = null;
		opm = null;
		
		account = null;		// The account to import (null = merge all accounts)
		this.accounts = new Vector<String>();
	}
	
	
	/**
	 * Initialize the parser given an input URI. If the file has an ambiguous type (such as *.xml),
	 * the method should check whether the file has a proper format that can be handled by the parser.
	 * 
	 * @param uri the input URI
	 * @throws ParserException on error
	 * @throws ParserFormatException if the file does not have the appropriate format
	 */
	public void initialize(URI uri) throws ParserException {
		
		if (!"file".equals(uri.getScheme())) throw new ParserFormatException();
		
		this.file = new File(uri);
		this.opm = null;
		
		this.account = null;
		this.accounts.clear();
		
		
		// Read the OPM file
		
		try {
			OPMDeserialiser d = new OPMDeserialiser();
			opm = d.deserialiseOPMGraph(file);
		}
		catch (JAXBException e) {
			throw new ParserException("OPM Parser Error",e);
		}
		
		
		// Import accounts

		if (opm.getAccounts() != null) {
			for (Account a : opm.getAccounts().getAccount()) {
				accounts.add(a.getId());
			}
		}
	}
	

	/**
	 * Get a configuration GUI for the parser 
	 * 
	 * @return a list of WizardPanel's for the GUI configuration, or null if not necessary
	 */
	public List<WizardPanel> createConfigurationGUI() {
		
		List<WizardPanel> r = new Vector<WizardPanel>();
		
		r.add(new AccountPanel());
		
		return r;
	}


	/**
	 * Parse an object identified by the given URI
	 * 
	 * @param uri the input URI
	 * @param handler the callback for parser events
	 * @throws ParserException on error
	 */
	public void parse(URI uri, ParserHandler handler) throws ParserException {
		
		if (!"file".equals(uri.getScheme())) throw new ParserFormatException();
		File file = new File(uri);
		
		if (DEBUG) {
			System.err.println();
			System.err.println("OPM Import " + file.getName());
		}
		
		
		// Read the OPM file, if the file does not match
		
		if (!file.equals(this.file) || opm == null) {
			
			this.file = file;
			this.opm = null;
			
			try {
				OPMDeserialiser d = new OPMDeserialiser();
				opm = d.deserialiseOPMGraph(file);
			}
			catch (JAXBException e) {
				throw new ParserException("OPM Parser Error",e);
			}
		}
		
		if (DEBUG) {
			System.err.println("Account: " + account);
		}
		
		
		// Start
		
		PMeta meta = new PMeta();
		meta.addObjectAttributeCode("Type", PObject.Attribute.TYPE);
		meta.addObjectExtType("Agent", PObject.Type.AGENT);
		meta.addObjectExtType("Artifact", PObject.Type.ARTIFACT);
		meta.addObjectExtType("Process", PObject.Type.PROCESS);
		
		handler.beginParsing();
		handler.setMeta(meta);
		
		
		// Import artifacts, processes, and agents
		
		if (DEBUG) System.err.println("Agents:");
		
		if (opm.getAgents() != null) {
			for (Agent a : opm.getAgents().getAgent()) {
				
				if (account != null) if (!containsAccount(a.getAccount(), account)) continue;
				
				String id = a.getId();
				handler.loadTripleAttribute(id, "Type", "Agent");
				
				if (DEBUG) System.err.println("  " + id);
				
				for (JAXBElement<? extends EmbeddedAnnotation> e : a.getAnnotation()) {
					updateMeta(meta, e);
					handler.loadTripleAttribute(id, e.getName().toString(), annotationToString(e.getValue()));
				}
			}
		}
		
		if (DEBUG) System.err.println("Artifacts:");
		
		if (opm.getArtifacts() != null) {
			for (Artifact a : opm.getArtifacts().getArtifact()) {
				
				if (account != null) if (!containsAccount(a.getAccount(), account)) continue;
				
				String id = a.getId();
				handler.loadTripleAttribute(id, "Type", "Artifact");
				
				if (DEBUG) System.err.println("  " + id);
				
				for (JAXBElement<? extends EmbeddedAnnotation> e : a.getAnnotation()) {
					updateMeta(meta, e);
					handler.loadTripleAttribute(id, e.getName().toString(), annotationToString(e.getValue()));
				}
			}
		}
		
		if (DEBUG) System.err.println("Processes:");
		
		if (opm.getProcesses() != null) {
			for (Process p : opm.getProcesses().getProcess()) {
				
				if (account != null) if (!containsAccount(p.getAccount(), account)) continue;
				
				String id = p.getId();
				handler.loadTripleAttribute(id, "Type", "Process");
				
				if (DEBUG) System.err.println("  " + id);
				
				for (JAXBElement<? extends EmbeddedAnnotation> e : p.getAnnotation()) {
					updateMeta(meta, e);
					handler.loadTripleAttribute(id, e.getName().toString(), annotationToString(e.getValue()));
				}
			}
		}
		
		
		// Import dependencies
		
		if (DEBUG) System.err.println("Dependencies:");
		
		Dependencies dependency = opm.getDependencies();
		List<Object> dependencies = dependency == null ? null : dependency.getUsedOrWasGeneratedByOrWasTriggeredBy();
		if (dependencies == null) dependencies = new Vector<Object>();
		
		for (Object d : dependencies) {
			
			if (account != null) if (!containsAccount(getDependencyAccounts(d), account)) continue;
			
			String cause = getDependencyCause(d);
			String type = getDependencyExtType(d);
			String effect = getDependencyEffect(d);
			
			if (DEBUG) System.err.println("  " + effect + " --" + type + "--> " + cause);
			
			PEdge.Type t = getDependencyType(d);
			meta.addEdgeLabel(type, t);
			
			handler.loadTripleAncestry(effect, type, cause);
		}
		
		
		handler.endParsing();
	}
	
	
	/**
	 * Determine whether the parser accepts the given URI
	 * 
	 * @param uri the input URI
	 * @return true if it accepts the input
	 */
	public boolean accepts(URI uri) {
		
		if (!"file".equals(uri.getScheme())) return false;
		File file = new File(uri);
		
		String ext = Utils.getExtension(file);
		
		if ("opm".equals(ext)) return true;
		if ("xml".equals(ext)) return true;
		if ("n3" .equals(ext)) return true;
		if ("rdf".equals(ext)) return true;
		
		return false;
	}
	
	
	/**
	 * Return the string value of an OPM annotation
	 * 
	 * @param value the value of an OPM annotation
	 * @return the string value
	 */
	public static String annotationToString(EmbeddedAnnotation value) {
		
		if (value instanceof Label) return ((Label)value ).getValue();
		if (value instanceof Value) return "" + ((Value)value ).getContent();
		if (value instanceof Type) return ((Type)value ).getValue();
		if (value instanceof PName) return ((PName)value ).getValue();
		if (value instanceof Profile) return ((Profile)value ).getValue();
		if (value instanceof Annotation) return ((Annotation)value ).toString();
		
		return "" + value;
	}
	
	
	/**
	 * Return the object attribute code of an OPM annotation
	 * 
	 * @param value the value of an OPM annotation
	 * @return the attribute code
	 */
	public static PObject.Attribute annotationObjectAttributeCode(EmbeddedAnnotation value) {
		
		if (value instanceof Label) return PObject.Attribute.NAME;
		if (value instanceof Type) return PObject.Attribute.TYPE;
		
		return PObject.Attribute.OTHER;
	}
	
	
	/**
	 * Return the node attribute code of an OPM annotation
	 * 
	 * @param value the value of an OPM annotation
	 * @return the attribute code
	 */
	public static PNode.Attribute annotationNodeAttributeCode(EmbeddedAnnotation value) {
		
		return PNode.Attribute.OTHER;
	}
	
	
	/**
	 * Update the graph metadata based on an annotation
	 * 
	 * @param meta the metadata
	 * @param annotation the annotation
	 */
	private void updateMeta(PMeta meta, JAXBElement<? extends EmbeddedAnnotation> annotation) {
		
		PObject.Attribute oa = annotationObjectAttributeCode(annotation.getValue());
		if (oa != PObject.Attribute.OTHER) {
			meta.addObjectAttributeCode(annotation.getName().toString(), oa);
			return;
		}
		
		PNode.Attribute na = annotationNodeAttributeCode(annotation.getValue());
		if (na != PNode.Attribute.OTHER) {
			meta.addNodeAttributeCode(annotation.getName().toString(), na);
			return;
		}
	}
	
	
	/**
	 * Return the ID of the cause of the OPM dependency 
	 * 
	 * @param d the OPM dependency
	 * @return the string ID
	 */
	public static String getDependencyCause(Object d) {
		
		Object o = null;
		if (d == null) throw new IllegalArgumentException("Dependency is null");
		
		if (d instanceof Used) o = ((Used) d).getCause().getRef();
		else if (d instanceof WasControlledBy) o = ((WasControlledBy) d).getCause().getRef();
		else if (d instanceof WasDerivedFrom) o = ((WasDerivedFrom) d).getCause().getRef();
		else if (d instanceof WasGeneratedBy) o = ((WasGeneratedBy) d).getCause().getRef();
		else if (d instanceof WasTriggeredBy) o = ((WasTriggeredBy) d).getCause().getRef();
		else if (d instanceof UsedStar) o = ((Used) d).getCause().getRef();
		else if (d instanceof WasDerivedFromStar) o = ((WasDerivedFrom) d).getCause().getRef();
		else if (d instanceof WasGeneratedByStar) o = ((WasGeneratedBy) d).getCause().getRef();
		else if (d instanceof WasTriggeredByStar) o = ((WasTriggeredBy) d).getCause().getRef();
		else throw new IllegalArgumentException("Invalid dependency class " + d.getClass().getCanonicalName());
		
		if (o == null) throw new IllegalArgumentException("The \"cause\" reference is null");
		
		if (o instanceof Identifiable) return ((Identifiable) o).getId();
		else throw new IllegalArgumentException("The \"cause\" object is not Identifiable: " + o.getClass());
	}
	
	
	/**
	 * Return the ID of the effect of the OPM dependency 
	 * 
	 * @param d the OPM dependency
	 * @return the string ID
	 */
	public static String getDependencyEffect(Object d) {
		
		Object o = null;
		if (d == null) throw new IllegalArgumentException("Dependency is null");
		
		if (d instanceof Used) o = ((Used) d).getEffect().getRef();
		else if (d instanceof WasControlledBy) o = ((WasControlledBy) d).getEffect().getRef();
		else if (d instanceof WasDerivedFrom) o = ((WasDerivedFrom) d).getEffect().getRef();
		else if (d instanceof WasGeneratedBy) o = ((WasGeneratedBy) d).getEffect().getRef();
		else if (d instanceof WasTriggeredBy) o = ((WasTriggeredBy) d).getEffect().getRef();
		else if (d instanceof UsedStar) o = ((Used) d).getEffect().getRef();
		else if (d instanceof WasDerivedFromStar) o = ((WasDerivedFrom) d).getEffect().getRef();
		else if (d instanceof WasGeneratedByStar) o = ((WasGeneratedBy) d).getEffect().getRef();
		else if (d instanceof WasTriggeredByStar) o = ((WasTriggeredBy) d).getEffect().getRef();
		else throw new IllegalArgumentException("Invalid dependency class " + d.getClass().getCanonicalName());
		
		if (o instanceof Identifiable) return ((Identifiable) o).getId();
		else throw new IllegalArgumentException();
	}
	
	
	/**
	 * Return the type of the OPM dependency 
	 * 
	 * @param d the OPM dependency
	 * @return the string type
	 */
	public static String getDependencyExtType(Object d) {
		
		if (d == null) throw new IllegalArgumentException("Dependency is null");

		if (d instanceof Used) return "Used";
		if (d instanceof WasControlledBy) return "WasControlledBy";
		if (d instanceof WasDerivedFrom) return "WasDerivedFrom";
		if (d instanceof WasGeneratedBy) return "WasGeneratedBy";
		if (d instanceof WasTriggeredBy) return "WasTriggeredBy";
		if (d instanceof UsedStar) return "Used*";
		if (d instanceof WasDerivedFromStar) return "WasDerivedFrom*";
		if (d instanceof WasGeneratedByStar) return "WasGeneratedBy*";
		if (d instanceof WasTriggeredByStar) return "WasTriggeredBy*";
		
		throw new IllegalArgumentException("Invalid dependency class " + d.getClass().getCanonicalName());
	}
	
	
	/**
	 * Return the type of the OPM dependency 
	 * 
	 * @param d the OPM dependency
	 * @return the type code
	 */
	public static PEdge.Type getDependencyType(Object d) {
		
		if (d == null) throw new IllegalArgumentException("Dependency is null");

		if (d instanceof Used) return PEdge.Type.DATA;
		if (d instanceof WasControlledBy) return PEdge.Type.CONTROL;
		if (d instanceof WasDerivedFrom) return PEdge.Type.DATA;
		if (d instanceof WasGeneratedBy) return PEdge.Type.DATA;
		if (d instanceof WasTriggeredBy) return PEdge.Type.CONTROL;
		if (d instanceof UsedStar) return PEdge.Type.DATA;
		if (d instanceof WasDerivedFromStar) return PEdge.Type.DATA;
		if (d instanceof WasGeneratedByStar) return PEdge.Type.DATA;
		if (d instanceof WasTriggeredByStar) return PEdge.Type.CONTROL;
		
		throw new IllegalArgumentException("Invalid dependency class " + d.getClass().getCanonicalName());
	}
	
	
	/**
	 * Return the list of accounts of an OPM dependency 
	 * 
	 * @param d the OPM dependency
	 * @return the list of account references
	 */
	public static List<AccountRef> getDependencyAccounts(Object d) {
		
		if (d == null) throw new IllegalArgumentException("Dependency is null");
		
		if (d instanceof Used) return ((Used) d).getAccount();
		if (d instanceof WasControlledBy) return ((WasControlledBy) d).getAccount();
		if (d instanceof WasDerivedFrom) return ((WasDerivedFrom) d).getAccount();
		if (d instanceof WasGeneratedBy) return ((WasGeneratedBy) d).getAccount();
		if (d instanceof WasTriggeredBy) return ((WasTriggeredBy) d).getAccount();
		if (d instanceof UsedStar) return ((Used) d).getAccount();
		if (d instanceof WasDerivedFromStar) return ((WasDerivedFrom) d).getAccount();
		if (d instanceof WasGeneratedByStar) return ((WasGeneratedBy) d).getAccount();
		if (d instanceof WasTriggeredByStar) return ((WasTriggeredBy) d).getAccount();

		throw new IllegalArgumentException("Invalid dependency class " + d.getClass().getCanonicalName());
	}

	
	/**
	 * Determine if the given account appears in the list
	 * 
	 * @param list the list of account references
	 * @param account the account ID
	 * @return true if the list contains the given account ID
	 */
	public static boolean containsAccount(List<AccountRef> list, String account) {
		
		for (AccountRef a : list) {
			Object v = a.getRef();
			if (v instanceof Identifiable) {
				if (account.equals(((Identifiable) v).getId())) return true;
			}
			else if (v == null) {
			}
			else {
				throw new IllegalArgumentException();
			}
		}
		
		return false;
	}
	
	
	/**
	 * Configuration panel for choosing the OPM account to import
	 */
	private class AccountPanel extends WizardPanel {
		
		private JLabel topLabel;
		private JList list;
		private JScrollPane listScroll;
		private DefaultListModel model;
		
		
		/**
		 * Create an instance of AccountPanel
		 */
		public AccountPanel() {
			super("Choose account");
			
			
			// Initialize the panel
			
			panel.setLayout(new BorderLayout());
			
			topLabel = new JLabel("Choose an OPM account to import:");
			topLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
			panel.add(topLabel, BorderLayout.NORTH);
			
			
			// Create the list model
			
			model = new DefaultListModel();
			int index = 0;
			
			if (accounts.isEmpty()) {
				model.addElement("<default>");
			}
			else {
				int i = 1;
				model.addElement("<all>");
				for (String a : accounts) {
					model.addElement(a);
					if (a.equals(account)) index = i;
					i++;
				}
			}
			
		
			// Create the list
			
			list = new JList(model);
			list.setSelectedIndex(index);
			
			listScroll = new JScrollPane(list);
			panel.add(listScroll, BorderLayout.CENTER);
			
			
			// Mouse handler for JList
			
			MouseListener mouseListener = new MouseAdapter() {
			    public void mouseClicked(MouseEvent e) {
			        if (e.getClickCount() == 2) {
			            getWizard().next();
			         }
			    }
			};
			list.addMouseListener(mouseListener);
		}
		
		
		/**
		 * Callback for when the next button was clicked
		 */
		protected void wizardNext() {
			
			int i = list.getSelectedIndex();
			
			if (accounts.isEmpty() || i == 0) {
				account = null;
			}
			else {
				account = accounts.get(i - 1);
			}
		}
	}
}
