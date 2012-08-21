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

package edu.harvard.pass.orbiter.document;

import edu.harvard.pass.algorithm.*;
import edu.harvard.pass.job.*;
import edu.harvard.pass.parser.*;
import edu.harvard.pass.parser.Parser;
import edu.harvard.pass.*;
import edu.harvard.util.graph.layout.*;
import edu.harvard.util.graph.summarizer.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.gui.*;
import edu.harvard.util.job.*;
import edu.harvard.util.*;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*; 


/**
 * A provenance visualization document
 * 
 * @author Peter Macko
 */
public class Document implements java.io.Serializable {

	private static final long serialVersionUID = 4721794237917256753L;
	
	public static final String DESCRIPTION = "Orbiter Project";
	public static final String EXTENSION = "orb";
	public static final String EXTENSION_COMPRESSED = "orc";
	public static final String DOM_ELEMENT = "orbiter-project";

	private String name;
	private transient File file;
	private transient URI uri;
	
	private PGraph graph;

	private String summarizationAlgorithm = null;
	private boolean refineSummaryFileExt = false;
	private boolean refineSummaryRandomly = false;


	/**
	 * Constructor for objects of type Document
	 *
	 * @param name the document name
	 * @param graph the provenance graph
	 */
	protected Document(String name, PGraph graph) {
		this(name, graph, null, null);
	}


	/**
	 * Constructor for objects of type Document
	 *
	 * @param name the document name
	 * @param graph the provenance graph
	 * @param uri the URI
	 * @param file the file that this document corresponds to
	 */
	protected Document(String name, PGraph graph, URI uri, File file) {
		this.name = name;
		this.graph = graph;
		this.uri = uri;
		this.file = file;
	}


	/**
	 * Return the document name
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * Return the provenance graph
	 *
	 * @return the graph
	 */
	public PGraph getPGraph() {
		return graph;
	}


	/**
	 * Return the URI associated with the document
	 *
	 * @return the URI, or null if none
	 */
	public URI getURI() {
		return uri;
	}


	/**
	 * Return the file associated with the document
	 *
	 * @return the file, or null if none
	 */
	public File getFile() {
		return file;
	}


	/**
	 * Load the document. The import format is determined by the file
	 * extension, and if it is not Document.EXTENSION, it is converted
	 * appropriately
	 *
	 * @param uri the input URI
	 * @throws IOException on error
	 */
	public static Document load(URI uri) throws IOException, JobException {
		return load(uri, new DefaultJobMaster());
	}


	/**
	 * Load the document. The import format is determined by the file
	 * extension, and if it is not Document.EXTENSION, it is converted
	 * appropriately
	 *
	 * @param uri the input URI
	 * @param master the job master to use
	 * @throws IOException on error
	 */
	public static Document load(URI uri, JobMaster master) throws IOException, JobException {
		
		String name = uri.toString();
		File file = null;
		
		if ("file".equals(uri.getScheme())) {
			file = new File(uri);
			name = file.getName();
		}
		
		
		// Provenance graph in a native format

		if (SupportedParsers.getInstance().accepts(uri)) {

			Pointer<PGraph> pg = new Pointer<PGraph>();
			Pointer<BaseGraph> bpg = Utils.<Pointer<BaseGraph>>cast(pg);
			
			
			// Initialize the parser
			
			Parser parser = null;
			
			try {
				parser = SupportedParsers.getInstance().getParser(uri);
			}
			catch (ParserException e) {
				throw new JobException(e);
			}
			
			
			// Initialize the graph layout algorithm
			
			GraphLayoutAlgorithm layoutAlgorithm;

			try {
				Graphviz g = new Graphviz();
				g.setBySummaries(true);
				g.setOptimizedForZoom(true);
				
				layoutAlgorithm = g;
			}
			catch (Exception e) {
				throw new IOException(e.getMessage());
			}
			
			
			// Run the Wizard
			
			SimpleWizard w = new SimpleWizard(null, "Import " + name, true);
			
			if (parser instanceof HasWizardPanelConfigGUI) {
				List<WizardPanel> l = ((HasWizardPanelConfigGUI) parser).createConfigurationGUI();
				if (l != null) for (WizardPanel p : l) w.addWizardPanel(p);
			}
			
			SummarizationConfigPanel summarizationPanel = new SummarizationConfigPanel();
			w.addWizardPanel(summarizationPanel);
			
			if (layoutAlgorithm instanceof HasWizardPanelConfigGUI) {
				List<WizardPanel> l = ((HasWizardPanelConfigGUI) layoutAlgorithm).createConfigurationGUI();
				if (l != null) for (WizardPanel p : l) w.addWizardPanel(p);
			}
			
			FinalImportConfigPanel finalPanel = new FinalImportConfigPanel();
			w.addWizardPanel(finalPanel);
			
			if (w.hasPanels()) {
				w.setMinimumSize(new Dimension(480, 320));
				if (w.run() != SimpleWizard.OK) throw new JobCanceledException();
			}


			// Build the list of jobs

			master.add(new LoadPGraphJob(uri, parser, pg));
			if (finalPanel.provRankCheck.isSelected()) master.add(new ProvRankJob(pg));
			
			Class<? extends GraphSummarizer> summarizerClass
				= SummarizationConfigPanel.SUMMARIZATION_ALGORITHMS.get(summarizationPanel.summarizationAlgorithm);
			
			if (summarizerClass == null) summarizerClass = NullSummarizer.class;
			
			try {
				master.add(new GraphSummaryJob(summarizerClass.newInstance(), bpg));
			} catch (InstantiationException e) {
				throw new InternalError();
			} catch (IllegalAccessException e) {
				throw new InternalError();
			}
			
			if (summarizerClass != NullSummarizer.class) {
				if (summarizationPanel.refineSummaryFileExt) {
					master.add(new GraphSummaryJob(new FileExtSummarizer(), bpg, "Refining graph summary"));
				}
				
				if (summarizationPanel.refineSummaryRandomly) {
					master.add(new GraphSummaryJob(new SmallGroupsGraphSummarizer(), bpg, "Refining graph summary"));
				}
			}
			
			master.add(new GraphLayoutAlgorithmJob(layoutAlgorithm, bpg, layoutAlgorithm.isZoomOptimized() ? 2 : -1));
			
			
			// Run the jobs
			
			master.run();


			// Postprocess the result

			PGraph graph = pg.get();
			
			Document d = new Document(name, graph, uri, file);
			d.summarizationAlgorithm = summarizationPanel.summarizationAlgorithm;
			d.refineSummaryFileExt = summarizationPanel.refineSummaryFileExt;
			d.refineSummaryRandomly = summarizationPanel.refineSummaryRandomly;
			
			return d;
		}

		
		// A file
		
		if (file != null) {
			
			String ext = Utils.getExtension(file);
			if (ext == null) ext = "";
	
	
			// The project
	
			if (ext.equals(Document.EXTENSION) || ext.equals(Document.EXTENSION_COMPRESSED)) {
	
				LoadDocumentJob l = new LoadDocumentJob(file.getPath());
				master.add(l);
				master.run();
	
				return l.getResult();
			}


			// Unrecognized file extension

			throw new IOException("Unrecognized file extension ." + ext);
		}


		// Unrecognized URI

		throw new IOException("Unrecognized provenance URI " + uri);
	}


	/**
	 * Recompute the layout of the document
	 *
	 * @param layoutAlgorithm the graph layout algorithm
	 * @param master the job master to use
	 * @throws IOException on error
	 */
	public void recomputeLayout(GraphLayoutAlgorithm layoutAlgorithm, JobMaster master) throws IOException, JobException {
		
		Pointer<PGraph> pg = new Pointer<PGraph>();
		pg.set(graph);


		// Build the list of jobs

		GraphLayoutAlgorithmJob l = new GraphLayoutAlgorithmJob(layoutAlgorithm, Utils.<Pointer<BaseGraph>>cast(pg));
		master.add(l);
		master.run();
	}
	
	
	/**
	 * Add default summarization jobs to the specified job master
	 * 
	 * @param graph the provenance graph
	 * @param master the job master
	 */
	public void addDefaultSummarizationJobs(PGraph graph, JobMaster master) {
		
		Pointer<BaseGraph> bpg = new Pointer<BaseGraph>(graph);
		
		Class<? extends GraphSummarizer> summarizerClass
			= SummarizationConfigPanel.SUMMARIZATION_ALGORITHMS.get(summarizationAlgorithm);
		
		if (summarizerClass != null) {
			try {
				master.add(new GraphSummaryJob(summarizerClass.newInstance(), bpg));
			} catch (InstantiationException e) {
				throw new InternalError();
			} catch (IllegalAccessException e) {
				throw new InternalError();
			}
			
			if (refineSummaryFileExt) {
				master.add(new GraphSummaryJob(new FileExtSummarizer(), bpg, "Refining graph summary"));
			}
			
			if (refineSummaryRandomly) {
				master.add(new GraphSummaryJob(new SmallGroupsGraphSummarizer(), bpg, "Refining graph summary"));
			}
		}
	}


	/**
	 * Save the document. Determine the output format from the file extension
	 *
	 * @param file the output file
	 * @throws IOException on error
	 */
	public void save(File file) throws IOException, JobException {
		save(file, new DefaultJobMaster());
	}


	/**
	 * Save the document using a custom job master. Determine the output format
	 * from the file extension
	 *
	 * @param file the output file
	 * @param master the job master to use
	 * @throws IOException on error
	 */
	public void save(File file, JobMaster master) throws IOException, JobException {
	
		String ext = Utils.getExtension(file);
		if (ext == null) {
			file = new File(file.getAbsoluteFile() + "." + Document.EXTENSION_COMPRESSED);
			ext = Utils.getExtension(file);
		}


		// The project

		if (ext.equals(Document.EXTENSION) || ext.equals(Document.EXTENSION_COMPRESSED)) {
			master.add(new ExportDocumentJob(file.getPath(), this));
			master.run();
			return;
		}


		// Unrecognized file extension

		throw new IOException("Unrecognized file extension ." + ext);
	}


	/**
	 * Write the document to a project file
	 * 
	 * @param file the output file
	 * @throws IOException on I/O error
	 * @throws TransformerConfigurationException on Xerces transformer error 
	 * @throws SAXException on parser error
	 */
	public void writeToProjectFile(File file) throws IOException, TransformerConfigurationException, SAXException {
		
		String ext = Utils.getExtension(file);
		boolean compressed = ext.equals(Document.EXTENSION_COMPRESSED);

		
		// Based on: http://www.javazoom.net/services/newsletter/xmlgeneration.html
		
		PrintWriter out = null;
		
		if (compressed) {
			out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
		}
		else {
			out = new PrintWriter(file);
		}
		
		StreamResult streamResult = new StreamResult(out);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		
		
		// Initialize the SAX content handler
		
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
		//serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "orbiter.dtd");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		hd.setResult(streamResult);
		
		
		// Start the document
		
		hd.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "CDATA", name);
		attrs.addAttribute("", "", "summarization", "CDATA", summarizationAlgorithm);
		attrs.addAttribute("", "", "summarizationRefineFileExt", "CDATA", refineSummaryFileExt ? "true" : "false");
		attrs.addAttribute("", "", "summarizationRefineRandomly", "CDATA", refineSummaryRandomly ? "true" : "false");
		hd.startElement("", "", DOM_ELEMENT, attrs);
		
		
		// Write the provenance graph
		
		if (graph != null) {
			graph.writeToXML(hd);
		}
		

		// Finish the document
		
		hd.endElement("", "", DOM_ELEMENT);
		hd.endDocument();

		out.close();
	}


	/**
	 * Load the document from a project file
	 *
	 * @param file the input file
	 * @param observer the job observer
	 * @throws IOException on I/O error
	 * @throws ParserException on DOM parser error
	 * @throws ParserConfigurationException on XML parser configuration error
	 * @throws SAXException on XML parser error
	 */
	public static Document loadFromProjectFile(File file, JobObserver observer)
			throws IOException, ParserException, ParserConfigurationException, SAXException {

		String ext = Utils.getExtension(file);
		boolean compressed = ext.equals(Document.EXTENSION_COMPRESSED);
		InputStream in = null;
		
		if (compressed) {
			FileInputStream fis = new FileInputStream(file);
			in = new GZIPInputStream(fis);
		}
		else {
			in = new FileInputStream(file);
		}

		
		// Based on: http://www.totheriver.com/learn/xml/xmltutorial.html
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		
		DocumentParser dp = new DocumentParser();
		dp.hanlder_PGraph = PGraph.createSAXParserHandler();
		
		try {
			sp.parse(in, dp);
		}
		catch (SAXException e) {
			if (e.getCause() instanceof ParserException) {
				throw (ParserException) e.getCause();
			}
			throw e;
		}
		
		
		// Check the result
		
		if (dp.documentName == null) {
			throw new ParserException("Document name is not specified");
		}
		
		if (dp.hanlder_PGraph.first.get() == null) {
			throw new ParserException("No graph was loaded");
		}
		
		
		// Create the document
		
		Document d = new Document(dp.documentName, dp.hanlder_PGraph.first.get(), file.toURI(), file);
		d.summarizationAlgorithm = dp.summarizationAlgorithm;
		d.refineSummaryFileExt = dp.refineSummaryFileExt;
		d.refineSummaryRandomly = dp.refineSummaryRandomly;
		
		return d;
	}
	
	
	/**
	 * SAX document parser
	 */
	private static class DocumentParser extends DefaultHandler {
		
		private int depth = 0;
		
		public String documentName = null;
		public Pair<Pointer<PGraph>, DefaultHandler> hanlder_PGraph = null;
		
		private boolean inside_PGraph = false;

		private String summarizationAlgorithm = "Process Tree";
		private boolean refineSummaryFileExt = true;
		private boolean refineSummaryRandomly = false;

		
		/**
		 * Start new element
		 * 
		 * @param uri the URI
		 * @param localName the element's local name
		 * @param qName the element's fully qualified name
		 * @param attributes the attributes
		 * @throws SAXException on error
		 */
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			
			
			// Depth 0: Document
			
			if (depth == 0) {
				if (!qName.equals(DOM_ELEMENT)) {
					throw new SAXException("Expected <" + DOM_ELEMENT + ">, found <" + qName + ">");
				}
				
				documentName = XMLUtils.getAttribute(attributes, "name");
				
				if (XMLUtils.getAttribute(attributes, "summarization", null) != null)
					summarizationAlgorithm = XMLUtils.getAttribute(attributes, "summarization");
				if (XMLUtils.getAttribute(attributes, "summarizationRefineFileExt", null) != null)
					refineSummaryFileExt = "true".equalsIgnoreCase(XMLUtils.getAttribute(attributes, "summarizationRefineFileExt"));
				if (XMLUtils.getAttribute(attributes, "summarizationRefineRandomly", null) != null)
					refineSummaryRandomly = "true".equalsIgnoreCase(XMLUtils.getAttribute(attributes, "summarizationRefineRandomly"));
			}
			
			
			// Depth 1: Graph
			
			if (depth == 1) {
				if (qName.equals(PGraph.DOM_ELEMENT)) {
					if (hanlder_PGraph.first.get() != null) {
						throw new SAXException("Multiple <" + PGraph.DOM_ELEMENT + ">'s are specified");
					}
					inside_PGraph = true;
					hanlder_PGraph.second.startElement(uri, localName, qName, attributes);
				}
				else {
					throw new SAXException("Expected <" + PGraph.DOM_ELEMENT + ">, found <" + qName + ">");
				}
			}
			
			
			// Depth 2+: Inside a delegate
			
			if (depth >= 2) {
				if (inside_PGraph) {
					hanlder_PGraph.second.startElement(uri, localName, qName, attributes);
				}
			}
			
			
			// Update depth
			
			depth++;
		}
		

		/**
		 * Characters
		 * 
		 * @param ch the array of characters
		 * @param start the start within the array
		 * @param length the length of the string 
		 * @throws SAXException on error
		 */
		public void characters(char[] ch, int start, int length) throws SAXException {
			
			if (inside_PGraph) {
				hanlder_PGraph.second.characters(ch, start, length);
			}
		}
		
		
		/**
		 * Finish an element
		 * 
		 * @param uri the URI
		 * @param localName the element's local name
		 * @param qName the element's fully qualified name
		 * @param attributes the attributes
		 * @throws SAXException on error
		 */
		public void endElement(String uri, String localName, String qName) throws SAXException {
			
			depth--;
			
			
			// Depth 1: Graph
			
			if (depth == 1) {
				if (inside_PGraph && qName.equals(PGraph.DOM_ELEMENT)) {
					hanlder_PGraph.second.endElement(uri, localName, qName);
					inside_PGraph = false;
				}
			}
			
			
			// Depth 2+: Inside a delegate
			
			if (depth >= 2) {
				if (inside_PGraph) {
					hanlder_PGraph.second.endElement(uri, localName, qName);
				}
			}
		}
	}
	
	
	/**
	 * Configuration panel
	 */
	private static class SummarizationConfigPanel extends WizardPanel implements ActionListener {
		
		public static final Map<String, Class<? extends GraphSummarizer>> SUMMARIZATION_ALGORITHMS;
		private static String lastSummarizationAlgorithm = "Timestamps";
		private static boolean lastRefineSummaryFileExt = true;
		private static boolean lastRefineSummaryRandomly = false;

		public String summarizationAlgorithm = null;
		public boolean refineSummaryFileExt = false;
		public boolean refineSummaryRandomly = false;

		private JLabel topLabel;
		private JLabel algorithmLabel;
		private JComboBox algorithmCombo;
		private JCheckBox refineFileExtCheck;
		private JCheckBox refineRandomlyCheck;
		
		
		static {
			
			TreeMap<String, Class<? extends GraphSummarizer>> m = new TreeMap<String, Class<? extends GraphSummarizer>>();
			
			m.put("<none>", null);
			m.put("Timestamps", TimestampsSummarizer.class);
			m.put("Timestamps (processes only)", TimestampsSummarizer.ProcessesOnly.class);
			m.put("Process Tree", ProcessTreeSummarizer.class);
			
			SUMMARIZATION_ALGORITHMS = Collections.unmodifiableMap(m);
			if (!SUMMARIZATION_ALGORITHMS.containsKey(lastSummarizationAlgorithm)) throw new InternalError();
		}

		
		/**
		 * Create an instance of SummarizationConfigPanel
		 */
		public SummarizationConfigPanel() {
			super("Configure Summarization");
			
			
			// Initialize the panel
			
			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			
			int gridy = 0;
			
			
			// Header

			topLabel = new JLabel("Configure node summarization:");
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
			
			
			// Algorithm
			
			algorithmLabel = new JLabel("Summarize nodes by:   ");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(algorithmLabel, c);
			
			String[] algList = new String[SUMMARIZATION_ALGORITHMS.keySet().size()];
			int algList_i = 0;
			for (String s : SUMMARIZATION_ALGORITHMS.keySet()) {
				algList[algList_i++] = s;
			}
			algorithmCombo = new JComboBox(algList);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(algorithmCombo, c);
			
			int i = 0, index = 0;
			for (String a : algList) {
				if (a.equals(lastSummarizationAlgorithm)) index = i;
				i++;
			}
			
			algorithmCombo.setSelectedIndex(index);
			
			gridy++;
			
			
			// Options

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(new JLabel(" "), c);
			c.gridwidth = 1;

			gridy++;
			
			refineFileExtCheck = new JCheckBox("Refine the summary using file extensions");
			refineFileExtCheck.setSelected(lastRefineSummaryFileExt);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(refineFileExtCheck, c);
			c.gridwidth = 1;
			
			gridy++;
			
			refineRandomlyCheck = new JCheckBox("Randomly refine the summary if necessary (not recommended)");
			refineRandomlyCheck.setSelected(lastRefineSummaryRandomly);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(refineRandomlyCheck, c);
			c.gridwidth = 1;
			
			gridy++;
			
			
			// Finish

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			c.weighty = 1;
			panel.add(new JLabel(" "), c);
			c.weighty = 0;
			c.gridwidth = 1;

			gridy++;
			
			algorithmCombo.addActionListener(this);
			
			updateEnabled();
		}
		
		
		/**
		 * Callback for when the next button was clicked
		 */
		protected void wizardNext() {
			
			summarizationAlgorithm = (String) algorithmCombo.getSelectedItem();
			boolean none = "<none>".equalsIgnoreCase(summarizationAlgorithm);
			
			refineSummaryFileExt = refineFileExtCheck.isSelected() && !none;
			refineSummaryRandomly = refineRandomlyCheck.isSelected() && !none;
			
			lastSummarizationAlgorithm = summarizationAlgorithm;
			lastRefineSummaryFileExt = refineSummaryFileExt;
			lastRefineSummaryRandomly = refineSummaryRandomly;
		}
		
		
		/**
		 * Update the enabled properties
		 */
		private void updateEnabled() {
			
			String a = (String) algorithmCombo.getSelectedItem();
			boolean none = "<none>".equalsIgnoreCase(a);
			
			refineFileExtCheck.setEnabled(!none);
			refineRandomlyCheck.setEnabled(!none);
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
	 * Final configuration panel for document import
	 */
	private static class FinalImportConfigPanel extends WizardPanel {
		
		private JLabel topLabel;
		private JLabel moreLabel;
		public  JCheckBox provRankCheck;
		
		
		/**
		 * Create an instance of FinalImportConfigPanel
		 */
		public FinalImportConfigPanel() {
			super("Import");
			
			
			// Initialize the panel
			
			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			
			int gridy = 0;
			
			
			// Header

			topLabel = new JLabel("<html>The document is ready for import.<br/>Please select any additional import options below.</html>");
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
			
			
			// More to compute

			moreLabel = new JLabel("Additional options:");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(moreLabel, c);
			c.gridwidth = 1;

			gridy++;
			
			provRankCheck = new JCheckBox("Compute ProvRank (a measure of ubiquity of nodes)");
			provRankCheck.setSelected(false);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			panel.add(provRankCheck, c);
			c.gridwidth = 1;
			
			gridy++;
			
			
			// Finish

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			c.weighty = 1;
			panel.add(new JLabel(" "), c);
			c.weighty = 0;
			c.gridwidth = 1;

			gridy++;
		}
	}
}
