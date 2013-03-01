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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.openrdf.model.Statement;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.n3.N3ParserFactory;
import org.openrdf.rio.ntriples.NTriplesParserFactory;
import org.openrdf.rio.rdfxml.RDFXMLParserFactory;
import org.openrdf.rio.trig.TriGParserFactory;
import org.openrdf.rio.trix.TriXParserFactory;
import org.openrdf.rio.turtle.TurtleParserFactory;

import edu.harvard.pass.*;
import edu.harvard.pass.PObject.Type;
import edu.harvard.util.ParserException;
import edu.harvard.util.ParserFormatException;
import edu.harvard.util.gui.*;


/**
 * A parser for RDF/N3 and RDF/XML files
 * 
 * @author Peter Macko
 */
public class RDFParser implements Parser, HasWizardPanelConfigGUI {
	
	public static final String DEFAULT_URI = "file://local/";
	private int numStatements;
	
	private RDFAnalyzer analyzer;
	private PMeta meta;
	
	private String nameEdge;
	private String typeEdge;
	private String timeEdge;
	private String freezetimeEdge;
	private Map<String, PEdge.Type> ancestryTypeMap;
	private Map<String, PObject.Type> objectTypeMap;
	
	
	/**
	 * Create an instance of class RDFParser
	 */
	public RDFParser() {
		numStatements = 0;
		analyzer = null;
		meta = new PMeta();
		ancestryTypeMap = new TreeMap<String, PEdge.Type>();
		objectTypeMap = new TreeMap<String, PObject.Type>();
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
		File file = new File(uri);
		
		
		// This checks whether the file has an appropriate format
		// and collects the information about edge names and attribute values
		
		meta = new PMeta();
		analyzer = new RDFAnalyzer();
		
		parse(uri, analyzer);
		
		if (numStatements == 0) {
			RDFFormat format = RDFFormat.forFileName(file.getName());
			if (format == RDFFormat.RDFXML || format == RDFFormat.TRIX
					|| format == RDFFormat.TRIG || format == null) {
				throw new ParserFormatException("This is probably not a RDF file");
			}
		}
		
		
		// Make educated guesses for attribute edge types
		
		nameEdge = null;
		
		if (nameEdge == null) {
			for (String s : analyzer.attributeEdges) {
				if (s.toLowerCase().indexOf("name") >= 0) { nameEdge = s; break; }
			}
		}
		if (nameEdge == null) {
			for (String s : analyzer.attributeEdges) {
				if (s.toLowerCase().indexOf("label") >= 0) { nameEdge = s; break; }
			}
		}
		
		typeEdge = null;
		
		if (typeEdge == null) {
			for (String s : analyzer.attributeEdges) {
				if (s.toLowerCase().indexOf("type") >= 0) { typeEdge = s; break; }
			}
		}
		
		timeEdge = null;
		
		if (timeEdge == null) {
			for (String s : analyzer.attributeEdges) {
				if (s.toLowerCase().indexOf("time") >= 0) { 
					if (s.toLowerCase().indexOf("freeze") < 0) { 
						timeEdge = s; break;
					}
				}
			}
		}
		
		freezetimeEdge = null;
		
		if (freezetimeEdge == null) {
			for (String s : analyzer.attributeEdges) {
				if (s.toLowerCase().indexOf("freezetime") >= 0) { freezetimeEdge = s; break; }
			}
		}
		if (freezetimeEdge == null) {
			for (String s : analyzer.attributeEdges) {
				if (s.toLowerCase().indexOf("freeze") >= 0) { 
					if (s.toLowerCase().indexOf("time") >= 0) { 
						freezetimeEdge = s; break;
					}
				}
			}
		}
		
		
		// Initialize the edge type map
		
		ancestryTypeMap.clear();
		
		Set<String> values = analyzer.ancestryEdges;
		
		if (values != null) {
			for (String value : values) {
				
				// Make an educated guess
				
				PEdge.Type t = null;
				String v = value.toLowerCase();
				
				if (t == null && v.indexOf("use"     ) >= 0) t = PEdge.Type.DATA;
				if (t == null && v.indexOf("derive"  ) >= 0) t = PEdge.Type.DATA;
				if (t == null && v.indexOf("generate") >= 0) t = PEdge.Type.DATA;
				if (t == null && v.indexOf("data"    ) >= 0) t = PEdge.Type.DATA;
				if (t == null && v.indexOf("input"   ) >= 0) t = PEdge.Type.DATA;
				if (t == null && v.indexOf("trigger" ) >= 0) t = PEdge.Type.CONTROL;
				if (t == null && v.indexOf("control" ) >= 0) t = PEdge.Type.CONTROL;
				if (t == null && v.indexOf("parent"  ) >= 0) t = PEdge.Type.CONTROL;
				if (t == null && v.indexOf("fork"    ) >= 0) t = PEdge.Type.CONTROL;
				if (t == null && v.indexOf("execute" ) >= 0) t = PEdge.Type.CONTROL;
				if (t == null && v.indexOf("where"   ) >= 0) t = PEdge.Type.DATA;
				if (t == null && v.indexOf("why"     ) >= 0) t = PEdge.Type.DATA;
				if (t == null && v.indexOf("version" ) >= 0) t = PEdge.Type.VERSION;
				if (t == null && v.indexOf("prev"    ) >= 0) t = PEdge.Type.VERSION;
				
				
				// Update the map
				
				ancestryTypeMap.put(value, t == null ? PEdge.Type.OTHER : t);
			}
		}
		
		
		
		// Initialize other objects
		
		objectTypeMap.clear();
	}
	

	/**
	 * Get a configuration GUI for the parser 
	 * 
	 * @return a list of WizardPanel's for the GUI configuration, or null if not necessary
	 */
	public List<WizardPanel> createConfigurationGUI() {
		
		List<WizardPanel> r = new Vector<WizardPanel>();
		
		r.add(new EdgeConfigPanel());
		r.add(new AncestryTypeConfigPanel());
		r.add(new NodeTypeConfigPanel());
		
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
		

		// Guess the language & get the appropriate parser
		
		RDFFormat format = RDFFormat.forFileName(file.getName(), RDFFormat.N3);
		org.openrdf.rio.RDFParser parser = createParser(format);
		parser.setRDFHandler(new MyRDFHandler(handler));
		
		
		// Prepare the metadata
		
		meta = new PMeta();
		
		if (nameEdge != null) meta.addObjectAttributeCode(nameEdge, PObject.Attribute.NAME);
		if (typeEdge != null) meta.addObjectAttributeCode(typeEdge, PObject.Attribute.TYPE);
		if (timeEdge != null) meta.addNodeAttributeCode(timeEdge, PNode.Attribute.TIME);
		if (freezetimeEdge != null) meta.addNodeAttributeCode(freezetimeEdge, PNode.Attribute.FREEZETIME);
		
		for (String k : objectTypeMap.keySet()) {
			PObject.Type t = objectTypeMap.get(k);
			if (t != null && t != PObject.Type.OTHER) {
				meta.addObjectExtType(k, t);
			}
		}
		
		for (String k : ancestryTypeMap.keySet()) {
			PEdge.Type t = ancestryTypeMap.get(k);
			if (t != null && t != PEdge.Type.OTHER) {
				meta.addEdgeLabel(k, t);
			}
		}
		
		if (handler != null) {
			handler.setMeta(meta);
		}
		
		
		// Parse
		
		try {
			FileReader reader = new FileReader(file);
			
			if (handler != null) handler.beginParsing();
			numStatements = 0;
			parser.parse(reader, DEFAULT_URI);
			reader.close();
			if (handler != null) handler.endParsing();
		}
		catch (RDFParseException e) {
			throw new ParserException("RDF Parser Error", e);
		}
		catch (RDFHandlerException e) {
			if (e.getCause() instanceof ParserException) {
				throw (ParserException) e.getCause();
			}
			throw new ParserException("I/O Error", e);
		}
		catch (IOException e) {
			throw new ParserException("I/O Error", e);
		}
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
		
		return RDFFormat.forFileName(file.getName()) != null;
	}
	
	
	/**
	 * Get a RDF parser given the file format
	 * 
	 * @param format the RDF format
	 * @return the RDF parser
	 */
	public static org.openrdf.rio.RDFParser createParser(RDFFormat format) {
		
		if (format == RDFFormat.N3) return (new N3ParserFactory()).getParser();
		if (format == RDFFormat.NTRIPLES) return (new NTriplesParserFactory()).getParser();
		if (format == RDFFormat.RDFXML) return (new RDFXMLParserFactory()).getParser();
		if (format == RDFFormat.TRIG) return (new TriGParserFactory()).getParser();
		if (format == RDFFormat.TRIX) return (new TriXParserFactory()).getParser();
		if (format == RDFFormat.TURTLE) return (new TurtleParserFactory()).getParser();
		
		throw new IllegalArgumentException();
	}
	
	
	/**
	 * Handler for the RDF Parser
	 */
	private class MyRDFHandler extends RDFHandlerBase {
		
		private ParserHandler handler;
		
		
		/**
		 * Create an instance of class MyRDFHandler
		 * 
		 * @param handler the parser handler
		 */
		public MyRDFHandler(ParserHandler handler) {
			this.handler = handler;
		}
		
		
		/**
		 * Handle a namespace
		 * 
		 * @param prefix the namespace prefix
		 * @param uri the namespace URI
		 * @throws RDFHandlerException on error
		 */
		@Override
		public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
			super.handleNamespace(prefix, uri);
			
			if (uri.indexOf("openprovenance.org") >= 0) {
				throw new RDFHandlerException(new ParserFormatException("This appears to be an OPM graph"));
			}
		}
		

		/**
		 * Handle an RDF statement
		 * 
		 * @param st the statement to handle
		 * @throws RDFHandlerException on error
		 */
		@Override
		public void handleStatement(Statement st) throws RDFHandlerException {
			
			numStatements++;
			if (handler == null) return;	
			
			org.openrdf.model.Resource r = st.getSubject();
			org.openrdf.model.URI p = st.getPredicate();
			org.openrdf.model.Value v = st.getObject();
			
			
			// Get the string versions of the SPO values
			
			String s_pnode = r.stringValue();
			if (s_pnode.startsWith(DEFAULT_URI)) s_pnode = s_pnode.substring(DEFAULT_URI.length());
			
			String s_edge = p.stringValue();
			if (s_edge.startsWith(DEFAULT_URI)) s_edge = s_edge.substring(DEFAULT_URI.length());
			
			String s_value = v.stringValue();
			boolean anc = v instanceof org.openrdf.model.URI;
			if (anc) {
				if (s_value.startsWith(DEFAULT_URI)) s_value = s_value.substring(DEFAULT_URI.length());
			}
			
			
			// Feed the data into the parser

			try {
				if (anc) {
					handler.loadTripleAncestry(s_pnode, s_edge, s_value);
				}
				else {
					handler.loadTripleAttribute(s_pnode, s_edge, s_value);
				}
			}
			catch (ParserException e) {
				throw new RDFHandlerException(e);
			}
		}
	}
	
	
	/**
	 * A handler for parser events that analyzes the RDF file, so that
	 * it can be later properly parsed into a provenance graph
	 * 
	 * @author Peter Macko
	 */
	private class RDFAnalyzer implements ParserHandler {
		
		public Set<String> ancestryEdges;
		public Set<String> attributeEdges;
		public Map<String, HashSet<String>> attributeValues;
		
		
		/**
		 * Start loading the graph
		 * 
		 * @throws ParserException on error
		 */
		public void beginParsing() throws ParserException {
			attributeEdges = new HashSet<String>();
			ancestryEdges = new HashSet<String>();
			attributeValues = new HashMap<String, HashSet<String>>();
		}
		

		/**
		 * Process an ancestry triple
		 * 
		 * @param s_pnode the string version of a p-node
		 * @param s_edge the string version of an edge
		 * @param s_value the string version of the second p-node
		 * @throws ParserException on error
		 */
		public void loadTripleAncestry(String s_pnode, String s_edge, String s_value) throws ParserException {
			ancestryEdges.add(s_edge);
		}
		

		/**
		 * Process an attribute triple
		 * 
		 * @param s_pnode the string version of a p-node
		 * @param s_edge the string version of an edge
		 * @param s_value the string version of a value
		 * @throws ParserException on error
		 */
		public void loadTripleAttribute(String s_pnode, String s_edge, String s_value) throws ParserException {
			
			attributeEdges.add(s_edge);
			
			if (!attributeValues.containsKey(s_edge)) {
				attributeValues.put(s_edge, new HashSet<String>());
			}
			
			attributeValues.get(s_edge).add(s_value);
		}
		
		
		/**
		 * Finish loading the graph
		 * 
		 * @throws ParserException on error
		 */
		public void endParsing() throws ParserException {
		}


		/**
		 * Set the graph metadata object
		 * 
		 * @param meta the new metadata object
		 */
		@Override
		public void setMeta(PMeta meta) {
			RDFParser.this.meta = meta;
		}


		/**
		 * Return the PMeta object
		 * 
		 * @return the PMeta object
		 */
		@Override
		public PMeta getMeta() {
			return meta;
		}
	}
	
	
	/**
	 * Configuration panel - select attribute edge meanings
	 */
	private class EdgeConfigPanel extends WizardPanel implements ActionListener {
		
		private JLabel topLabel;
		private JLabel nameLabel;
		private JComboBox nameCombo;
		private JLabel typeLabel;
		private JComboBox typeCombo;
		private JLabel timeLabel;
		private JComboBox timeCombo;
		private JCheckBox versioningCheck;
		private JLabel freezetimeLabel;
		private JComboBox freezetimeCombo;
		
		private Vector<String> edges;
		
		
		/**
		 * Create an instance of ConfigPanel
		 */
		public EdgeConfigPanel() {
			super("Configure RDF parser");


			// Initialize the panel

			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			int gridy = 0;


			// Get the list of edges

			edges = new Vector<String>();

			if (analyzer != null) {
				for (String s : analyzer.attributeEdges) {
					edges.add(s);
				}
			}

			Collections.sort(edges);
			edges.add(0, "-- none --");


			// Header

			topLabel = new JLabel("Please select the appropriate attribute edge names:");
			topLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			c.weightx = 1;
			c.weighty = 0;
			panel.add(topLabel, c);
			c.weightx = 0;
			c.gridwidth = 1;

			gridy++;

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(new JLabel(" "), c);
			c.gridwidth = 1;

			gridy++;


			// Object names

			nameLabel = new JLabel("Object name:   ");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(nameLabel, c);

			nameCombo = new JComboBox(edges);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(nameCombo, c);
			
			int i = edges.indexOf(nameEdge);
			if (i >= 0) nameCombo.setSelectedIndex(i);

			gridy++;


			// Object types

			typeLabel = new JLabel("Object type:   ");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(typeLabel, c);

			typeCombo = new JComboBox(edges);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(typeCombo, c);
			
			i = edges.indexOf(typeEdge);
			if (i >= 0) typeCombo.setSelectedIndex(i);

			gridy++;


			// Object timestamps
			
			// TODO: What if we need additional (not only) timestamp edges?

			timeLabel = new JLabel("Node timestamp:   ");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(timeLabel, c);

			timeCombo = new JComboBox(edges);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(timeCombo, c);
			
			i = edges.indexOf(timeEdge);
			if (i >= 0) timeCombo.setSelectedIndex(i);

			gridy++;


			// Versioning

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(new JLabel(" "), c);
			c.gridwidth = 1;

			gridy++;

			versioningCheck = new JCheckBox("The provenance trace includes versioning");
			versioningCheck.setSelected(freezetimeEdge != null);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(versioningCheck, c);
			c.gridwidth = 1;

			gridy++;


			// Freeze times

			freezetimeLabel = new JLabel("Freeze time:");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(freezetimeLabel, c);

			freezetimeCombo = new JComboBox(edges);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(freezetimeCombo, c);
			
			i = edges.indexOf(freezetimeEdge);
			if (i >= 0) freezetimeCombo.setSelectedIndex(i);

			gridy++;


			// Finish

			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			c.weighty = 1;
			panel.add(new JLabel(" "), c);
			c.gridwidth = 1;

			gridy++;

			versioningCheck.addActionListener(this);
			
			updateEnabled();
		}


		/**
		 * Callback for when the next button was clicked
		 */
		protected void wizardNext() {
			
			if (nameCombo.getSelectedIndex() > 0) {
				nameEdge = "" + nameCombo.getSelectedItem();
			}
			else {
				nameEdge = null;
			}
			
			if (typeCombo.getSelectedIndex() > 0) {
				typeEdge = "" + typeCombo.getSelectedItem();
			}
			else {
				typeEdge = null;
			}
			
			if (timeCombo.getSelectedIndex() > 0) {
				timeEdge = "" + timeCombo.getSelectedItem();
			}
			else {
				timeEdge = null;
			}
			
			if (versioningCheck.isSelected() && freezetimeCombo.getSelectedIndex() > 0) {
				freezetimeEdge = "" + freezetimeCombo.getSelectedItem();
			}
			else {
				freezetimeEdge = null;
			}
		}


		/**
		 * Update the enabled properties
		 */
		private void updateEnabled() {

			freezetimeLabel.setEnabled(versioningCheck.isSelected());
			freezetimeCombo.setEnabled(versioningCheck.isSelected());
		}


		/**
		 * A callback for when an action has been performed
		 * 
		 * @param e the action event
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			updateEnabled();
		}
	}
	
	
	/**
	 * Configuration panel for categorizing the types of ancestry edges
	 */
	private class AncestryTypeConfigPanel extends WizardPanel {
		
		private JLabel topLabel;
		private JTable table;
		private JScrollPane tableScroll;
		private TableModel model;
		
		private Vector<String> keys;
		private Map<String, PEdge.Type> editorMap;
		private Map<PEdge.Type, String> rendererMap;
		
		
		/**
		 * Create an instance of AncestryTypeConfigPanel
		 */
		public AncestryTypeConfigPanel() {
			super("Categorize ancestry edge types");
			
			
			// Initialize the panel
			
			panel.setLayout(new BorderLayout());
			
			topLabel = new JLabel("Categorize the object types:");
			topLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
			panel.add(topLabel, BorderLayout.NORTH);
			
			
			// Create the table model
			
			keys = new Vector<String>();
			
			editorMap = new TreeMap<String, PEdge.Type>();
			editorMap.put("-- other --", PEdge.Type.OTHER);
			editorMap.put("Control flow", PEdge.Type.CONTROL);
			editorMap.put("Data flow", PEdge.Type.DATA);
			editorMap.put("Version edge", PEdge.Type.VERSION);
			
			rendererMap = new TreeMap<PEdge.Type, String>();
			for (java.util.Map.Entry<String, PEdge.Type> m : editorMap.entrySet()) {
				rendererMap.put(m.getValue(), m.getKey());
			}
			
			model = new TableModel();
			
		
			// Create the table
			
			table = new JTable(model);
			table.setFillsViewportHeight(true);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			table.getTableHeader().setReorderingAllowed(false);
			table.getColumnModel().getColumn(1).setPreferredWidth(128);

			table.getColumnModel().getColumn(1).setCellRenderer(new CellRenderer());
			table.getColumnModel().getColumn(1).setCellEditor(new CellEditor());
			
			tableScroll = new JScrollPane(table);
			tableScroll.setPreferredSize(new Dimension(tableScroll.getPreferredSize().width, 100));
			
			panel.add(tableScroll, BorderLayout.CENTER);
		}
		
		
		/**
		 * Prepare the panel to be displayed (this is a callback
		 * for just before the wizard panel is displayed)
		 */
		protected void prepare() {
			
			// Update the table model
			
			keys.clear();
			for (String k : ancestryTypeMap.keySet()) keys.add(k);
			
			model.fireTableDataChanged();
		}
		
		
		/**
		 * Table model
		 */
		@SuppressWarnings("serial")
		private class TableModel extends AbstractTableModel {

			/**
			 * Return the number of columns
			 *
			 * @return the number of columns
			 */
			public int getColumnCount() {
				return 2;
			}

			/**
			 * Return the number of rows
			 *
			 * @return the number of rows
			 */
			public int getRowCount() {
				return keys.size();
			}

			/**
			 * Return the column name
			 *
			 * @param col the column id
			 * @return the column name
			 */
			public String getColumnName(int col) {
				return col == 0 ? "Edge Type" : "Category";
			}

			/**
			 * Return the value at the given row and column
			 *
			 * @param row the row id
			 * @param col the column id
			 * @return the object
			 */
			public Object getValueAt(int row, int col) {
				String key = keys.get(row);
				if (col == 0) return key;
				return ancestryTypeMap.get(key);
			}

			/**
			 * Return the class of the given column
			 *
			 * @param col the column id
			 * @return the column class
			 */
			public Class<?> getColumnClass(int col) {
				return col == 1 ? PEdge.Type.class : String.class;
			}

			/**
			 * Determine whether the given cell is editable
			 *
			 * @param row the row id
			 * @param col the column id
			 * @return true if it is editable
			 */
			public boolean isCellEditable(int row, int col) {
				return col == 1;
			}

			
			/**
			 * Change the value in the given cell
			 *
			 * @param value the new value
			 * @param row the row id
			 * @param col the column id
			 */
			public void setValueAt(Object value, int row, int col) {
				
				if (col != 1) return;
				if (!(value instanceof PEdge.Type)) throw new InternalError();
				
				ancestryTypeMap.put(keys.get(row), (PEdge.Type) value);
				

				// Fire the callbacks
				
				fireTableCellUpdated(row, col);
			}
		}
		
		
		/**
		 * Cell renderer
		 */
		private class CellRenderer implements TableCellRenderer {
			
			private JLabel label;
			

			/**
			 * Create an instance of the cell renderer
			 */
			public CellRenderer() {
				label = new JLabel("");
				label.setOpaque(true);
			}
			
			
			/**
			 * Initialize a cell renderer
			 *
			 * @param table the table
			 * @param object the edited object
			 * @param isSelected whether the current row is selected
			 * @param hasFocus whether the cell has focus
			 * @param row the row number
			 * @param column the column number
			 * @return the cell renderer
			 */
			public Component getTableCellRendererComponent(JTable table, Object object,
														   boolean isSelected, boolean hasFocus,
														   int row, int column) {
				
				if (!(object instanceof PEdge.Type)) throw new InternalError();
				PEdge.Type t = (PEdge.Type) object;
				
				if (isSelected) {
					label.setForeground(table.getSelectionForeground());
					label.setBackground(table.getSelectionBackground());
				}
				else {
					label.setForeground(table.getForeground());
					label.setBackground(table.getBackground());
				}
				
				label.setText(rendererMap.get(t));
				return label;
			}
		}
		
		
		/**
		 * Cell editor
		 */
		@SuppressWarnings("serial")
		private class CellEditor extends AbstractCellEditor implements TableCellEditor {
			
			private JComboBox comboBox;


			/**
			 * Create an instance of the cell editor
			 */
			public CellEditor() {
				
				comboBox = new JComboBox();
				comboBox.setBorder(null);
				
				for (String k : editorMap.keySet()) {
					comboBox.addItem(k);
				}
			}


			/**
			 * Update the attribute value
			 *
			 * @return the edited object
			 */
			public Object getCellEditorValue() {
				
				String k = "" + comboBox.getSelectedItem();
				PEdge.Type t = editorMap.get(k);
				
				if (t == null) throw new InternalError();
				
				return t;
			}


			/**
			 * Initialize a cell editor
			 *
			 * @param table the table
			 * @param object the edited object
			 * @param isSelected whether the current row is selected
			 * @param row the row number
			 * @param column the column number
			 * @return the cell editor
			 */
			public Component getTableCellEditorComponent(JTable table,
														 Object object,
														 boolean isSelected,
														 int row,
														 int column) {
				
				if (!(object instanceof PEdge.Type)) throw new InternalError();
				PEdge.Type t = (PEdge.Type) object;
				
				comboBox.setSelectedItem(rendererMap.get(t));
				
				return comboBox;
			}
		}
	}
	
	
	/**
	 * Configuration panel for categorizing the types
	 */
	private class NodeTypeConfigPanel extends WizardPanel {
		
		private JLabel topLabel;
		private JTable table;
		private JScrollPane tableScroll;
		private TableModel model;
		
		private Vector<String> keys;
		private Map<String, PObject.Type> editorMap;
		private Map<PObject.Type, String> rendererMap;
		
		
		/**
		 * Create an instance of NodeTypeConfigPanel
		 */
		public NodeTypeConfigPanel() {
			super("Categorize node types");
			
			
			// Initialize the panel
			
			panel.setLayout(new BorderLayout());
			
			topLabel = new JLabel("Categorize the object types:");
			topLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
			panel.add(topLabel, BorderLayout.NORTH);
			
			
			// Create the table model
			
			keys = new Vector<String>();
			
			editorMap = new TreeMap<String, PObject.Type>();
			editorMap.put("-- other --", PObject.Type.OTHER);
			editorMap.put("Agent", PObject.Type.AGENT);
			editorMap.put("Artifact", PObject.Type.ARTIFACT);
			editorMap.put("Process", PObject.Type.PROCESS);
			
			rendererMap = new TreeMap<PObject.Type, String>();
			for (java.util.Map.Entry<String, Type> m : editorMap.entrySet()) {
				rendererMap.put(m.getValue(), m.getKey());
			}
			
			model = new TableModel();
			
		
			// Create the table
			
			table = new JTable(model);
			table.setFillsViewportHeight(true);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			table.getTableHeader().setReorderingAllowed(false);
			table.getColumnModel().getColumn(1).setPreferredWidth(128);

			table.getColumnModel().getColumn(1).setCellRenderer(new CellRenderer());
			table.getColumnModel().getColumn(1).setCellEditor(new CellEditor());
			
			tableScroll = new JScrollPane(table);
			tableScroll.setPreferredSize(new Dimension(tableScroll.getPreferredSize().width, 100));
			
			panel.add(tableScroll, BorderLayout.CENTER);
		}
		
		
		/**
		 * Prepare the panel to be displayed (this is a callback
		 * for just before the wizard panel is displayed)
		 */
		protected void prepare() {
			
			// Initialize the object type map
			
			objectTypeMap.clear();
			
			Set<String> values = analyzer.attributeValues.get(typeEdge);
			
			if (values != null) {
				for (String value : values) {
					
					// Make an educated guess
					
					PObject.Type t = null;
					String v = value.toLowerCase();
					
					if (t == null && v.indexOf("artifact") >= 0) t = PObject.Type.ARTIFACT;
					if (t == null && v.indexOf("file"    ) >= 0) t = PObject.Type.ARTIFACT;
					if (t == null && v.indexOf("cell"    ) >= 0) t = PObject.Type.ARTIFACT;
					if (t == null && v.indexOf("table"   ) >= 0) t = PObject.Type.ARTIFACT;
					if (t == null && v.indexOf("row"     ) >= 0) t = PObject.Type.ARTIFACT;
					if (t == null && v.indexOf("object"  ) >= 0) t = PObject.Type.ARTIFACT;
					if (t == null && v.indexOf("process" ) >= 0) t = PObject.Type.PROCESS;
					if (t == null && v.indexOf("proc"    ) >= 0) t = PObject.Type.PROCESS;
					if (t == null && v.indexOf("operator") >= 0) t = PObject.Type.PROCESS;
					if (t == null && v.indexOf("agent"   ) >= 0) t = PObject.Type.AGENT;
					if (t == null && v.indexOf("user"    ) >= 0) t = PObject.Type.AGENT;
					
					
					// Update the map
					
					objectTypeMap.put(value, t == null ? PObject.Type.OTHER : t);
				}
			}
			
			
			// Update the table model
			
			keys.clear();
			for (String k : objectTypeMap.keySet()) keys.add(k);
			
			model.fireTableDataChanged();
			
			
			// Move to the next panel, if there is nothing to set
			
			// TODO Make this work
			//if (objectTypeMap.isEmpty()) getWizard().next();
		}
		
		
		/**
		 * Table model
		 */
		@SuppressWarnings("serial")
		private class TableModel extends AbstractTableModel {

			/**
			 * Return the number of columns
			 *
			 * @return the number of columns
			 */
			public int getColumnCount() {
				return 2;
			}

			/**
			 * Return the number of rows
			 *
			 * @return the number of rows
			 */
			public int getRowCount() {
				return keys.size();
			}

			/**
			 * Return the column name
			 *
			 * @param col the column id
			 * @return the column name
			 */
			public String getColumnName(int col) {
				return col == 0 ? "Object Type" : "OPM Category";
			}

			/**
			 * Return the value at the given row and column
			 *
			 * @param row the row id
			 * @param col the column id
			 * @return the object
			 */
			public Object getValueAt(int row, int col) {
				String key = keys.get(row);
				if (col == 0) return key;
				return objectTypeMap.get(key);
			}

			/**
			 * Return the class of the given column
			 *
			 * @param col the column id
			 * @return the column class
			 */
			public Class<?> getColumnClass(int col) {
				return col == 1 ? PObject.Type.class : String.class;
			}

			/**
			 * Determine whether the given cell is editable
			 *
			 * @param row the row id
			 * @param col the column id
			 * @return true if it is editable
			 */
			public boolean isCellEditable(int row, int col) {
				return col == 1;
			}

			
			/**
			 * Change the value in the given cell
			 *
			 * @param value the new value
			 * @param row the row id
			 * @param col the column id
			 */
			public void setValueAt(Object value, int row, int col) {
				
				if (col != 1) return;
				if (!(value instanceof PObject.Type)) throw new InternalError();
				
				objectTypeMap.put(keys.get(row), (PObject.Type) value);
				

				// Fire the callbacks
				
				fireTableCellUpdated(row, col);
			}
		}
		
		
		/**
		 * Cell renderer
		 */
		private class CellRenderer implements TableCellRenderer {
			
			private JLabel label;
			

			/**
			 * Create an instance of the cell renderer
			 */
			public CellRenderer() {
				label = new JLabel("");
				label.setOpaque(true);
			}
			
			
			/**
			 * Initialize a cell renderer
			 *
			 * @param table the table
			 * @param object the edited object
			 * @param isSelected whether the current row is selected
			 * @param hasFocus whether the cell has focus
			 * @param row the row number
			 * @param column the column number
			 * @return the cell renderer
			 */
			public Component getTableCellRendererComponent(JTable table, Object object,
														   boolean isSelected, boolean hasFocus,
														   int row, int column) {
				
				if (!(object instanceof PObject.Type)) throw new InternalError();
				PObject.Type t = (PObject.Type) object;
				
				if (isSelected) {
					label.setForeground(table.getSelectionForeground());
					label.setBackground(table.getSelectionBackground());
				}
				else {
					label.setForeground(table.getForeground());
					label.setBackground(table.getBackground());
				}
				
				label.setText(rendererMap.get(t));
				return label;
			}
		}
		
		
		/**
		 * Cell editor
		 */
		@SuppressWarnings("serial")
		private class CellEditor extends AbstractCellEditor implements TableCellEditor {
			
			private JComboBox comboBox;


			/**
			 * Create an instance of the cell editor
			 */
			public CellEditor() {
				
				comboBox = new JComboBox();
				comboBox.setBorder(null);
				
				for (String k : editorMap.keySet()) {
					comboBox.addItem(k);
				}
			}


			/**
			 * Update the attribute value
			 *
			 * @return the edited object
			 */
			public Object getCellEditorValue() {
				
				String k = "" + comboBox.getSelectedItem();
				PObject.Type t = editorMap.get(k);
				
				if (t == null) throw new InternalError();
				
				return t;
			}


			/**
			 * Initialize a cell editor
			 *
			 * @param table the table
			 * @param object the edited object
			 * @param isSelected whether the current row is selected
			 * @param row the row number
			 * @param column the column number
			 * @return the cell editor
			 */
			public Component getTableCellEditorComponent(JTable table,
														 Object object,
														 boolean isSelected,
														 int row,
														 int column) {
				
				if (!(object instanceof PObject.Type)) throw new InternalError();
				PObject.Type t = (PObject.Type) object;
				
				comboBox.setSelectedItem(rendererMap.get(t));
				
				return comboBox;
			}
		}
	}
}
