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

import edu.harvard.pass.algorithm.ProcessTreeSummarizer;
import edu.harvard.pass.filter.*;
import edu.harvard.pass.*;
import edu.harvard.util.attribute.*;
import edu.harvard.util.filter.*;
import edu.harvard.util.graph.layout.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.gui.*;
import edu.harvard.util.job.*;
import edu.harvard.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;


/**
 * The application's main window
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame implements Runnable {

	public static final String TITLE = "Provenance Map Orbiter";
	
	private SupportedFilters filterFactory;
	
	private Document document;
	private PGraph graph;

	private JPanel panel;
	private JSplitPane mainSplitPane;
	private JSplitPane displaySplitPane;
	private EventHandler handler;
	
	private GraphDisplay<PNode, PEdge, PSummaryNode, PGraph> display;
	private PASSDecorator decorator;
	
	private JTabbedPane rightTabbedPane;
	private FilterListPanel<PNode> filterPanel;
	private SearchPanel searchPanel;
	private LegendPanel legendPanel;
	
	private JTabbedPane bottomTabbedPane;
	private JPanel timePanel;
	private JSlider timeSlider;
	private JLabel timeLabel;
	private TimelineTree<PObject> timelineTree;
	private PASSFilter.Time timeFilter;
	private SetFilter<PNode> timelineFilter;
	private NodeDetailPanel nodeDetailPanel;

	private JMenuBar mainMenu;
	
	private JMenu fileMenu;
	private JMenuItem fileOpenMenuItem;
	private JMenuItem fileOpenCPLMenuItem;
	private JMenuItem fileSaveMenuItem;
	private JMenuItem fileExportImageMenuItem;
	private JMenuItem fileExportNodeCSVMenuItem;
	private JMenuItem fileQuitMenuItem;
	
	private JMenu transformMenu;
	private JMenuItem transformFileGraphMenuItem;
	private JMenuItem transformProcessGraphMenuItem;
	private JMenuItem transformCollapseGraphMenuItem;
	private JMenuItem transformApplyFilterMenuItem;
	private JMenuItem transformHierarchicalLayoutMenuItem;
	private JMenuItem transformSpringLayoutMenuItem;
	
	private JMenu viewMenu;
	private JCheckBoxMenuItem viewDrawArrowsItem;
	private JCheckBoxMenuItem viewDrawNodesAsPointsItem;
	private JCheckBoxMenuItem viewDrawSplinesItem;
	private JMenuItem viewFullscreenMenuItem;
	
	private JMenu graphDerivationTreeMenu;

	
	/**
	 * Constructor of class MainFrame
	 */
	public MainFrame() {
		super(TITLE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		handler = new EventHandler();
		document = null;
		
		boolean mac = Utils.isMacOS();
		
		
		// Create the main panel
		
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		getContentPane().add(panel);
		
		
		// Create the display
		
		display = new GraphDisplay<PNode, PEdge, PSummaryNode, PGraph>();
		display.addGraphDisplayListener(handler);
		decorator = new PASSDecorator(null);
		display.setDecorator(decorator);
		
		
		// Create the right tabbed pane
		
		rightTabbedPane = new JTabbedPane(JTabbedPane.TOP); //RIGHT);
		
		
		// Create the legend panel
		
		legendPanel = new LegendPanel(null, display);
		if (mac) legendPanel.setOpaque(false);
		
		rightTabbedPane.addTab("Legend", null, legendPanel);
		
		
		// Create the filters

		FilterSet<PNode> f = new FilterSet<PNode>(true);
		display.addFilter(f);
		
		filterFactory = new SupportedFilters();
		filterPanel = new FilterListPanel<PNode>(null, f, filterFactory);
		if (mac) filterPanel.setOpaque(false);
		
		rightTabbedPane.addTab("Filter", null, filterPanel);
		
		
		// Create the search panel
		
		searchPanel = new SearchPanel(null, filterFactory, display);
		if (mac) searchPanel.setOpaque(false);
		
		rightTabbedPane.addTab("Search", null, searchPanel);
		
		
		// Create the bottom tabbed pane
		
		bottomTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		
		
		// Create the node detail panel
		
		nodeDetailPanel = new NodeDetailPanel();
		bottomTabbedPane.addTab("Node Details", null, nodeDetailPanel);
		
		
		// Create the timeline
		
		timePanel = new JPanel();
		timePanel.setLayout(new BorderLayout());
		
		timelineTree = new TimelineTree<PObject>();
		timelineTree.setDecorator(new PASSTimelineDecorator());
		timelineTree.addListSelectionListener(handler);
		
		JScrollPane timelineScrollPane = new JScrollPane(timelineTree);
		timelineScrollPane.setPreferredSize(new Dimension(320, 150));
		timelineScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		timePanel.add(timelineScrollPane, BorderLayout.CENTER);
		
		JPanel timeTopPanel = new JPanel();
		timeTopPanel.setLayout(new BorderLayout());
		
		timeSlider = new JSlider();
		timeSlider.addChangeListener(handler);
		timeSlider.setMajorTickSpacing(600);
		timeSlider.setMinorTickSpacing(10);
		timeSlider.setPaintTicks(true);
		timeTopPanel.add(timeSlider, BorderLayout.CENTER);
		
		timeLabel = new JLabel();
		Dimension timeLabelSize = new Dimension(TimelineTree.RIGHT_AREA_WIDTH, 8);
		timeLabel.setMinimumSize(timeLabelSize);
		timeLabel.setPreferredSize(timeLabelSize);
		timeTopPanel.add(timeLabel, BorderLayout.EAST);
		
		timePanel.add(timeTopPanel, BorderLayout.NORTH);
		
		timeFilter = new PASSFilter.Time();
		timeFilter.getAttribute().setOperator("<=");
		display.addFilter(timeFilter);
		
		timelineFilter = new SetFilter<PNode>("Timeline");
		timelineFilter.acceptAll();
		display.addFilter(timelineFilter);
		
		bottomTabbedPane.addTab("Process View & Time Line", null, timePanel);
		
		
		// Create the split panes
		
		displaySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, display, rightTabbedPane);
		displaySplitPane.setOneTouchExpandable(true);
		displaySplitPane.setDividerLocation(-200);
		displaySplitPane.setResizeWeight(1.0);
		
		mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, displaySplitPane, bottomTabbedPane);
		mainSplitPane.setOneTouchExpandable(true);
		mainSplitPane.setDividerLocation(-150);
		mainSplitPane.setResizeWeight(1.0);
		
		panel.add(mainSplitPane, BorderLayout.CENTER);
		
		
		// Create the file menu
		
		mainMenu = new JMenuBar();
		
		fileMenu = new JMenu("File");
		mainMenu.add(fileMenu);

		fileOpenMenuItem = new JMenuItem("Open", KeyEvent.VK_O);
		fileOpenMenuItem.addActionListener(handler);
		fileOpenMenuItem.setAccelerator(Utils.getKeyStrokeForMenu(KeyEvent.VK_O));
		fileMenu.add(fileOpenMenuItem);

		fileOpenCPLMenuItem = new JMenuItem("Open CPL Object", KeyEvent.VK_L);
		fileOpenCPLMenuItem.addActionListener(handler);
		fileOpenCPLMenuItem.setAccelerator(Utils.getKeyStrokeForMenu(KeyEvent.VK_L));
		fileOpenCPLMenuItem.setEnabled(edu.harvard.pass.cpl.CPL.isInstalled());
		fileMenu.add(fileOpenCPLMenuItem);

		fileSaveMenuItem = new JMenuItem("Save", KeyEvent.VK_S);
		fileSaveMenuItem.addActionListener(handler);
		fileSaveMenuItem.setAccelerator(Utils.getKeyStrokeForMenu(KeyEvent.VK_S));
		fileMenu.add(fileSaveMenuItem);

		fileMenu.addSeparator();
		
		fileExportImageMenuItem = new JMenuItem("Export Image", KeyEvent.VK_E);
		fileExportImageMenuItem.addActionListener(handler);
		fileExportImageMenuItem.setAccelerator(Utils.getKeyStrokeForMenu(KeyEvent.VK_E));
		fileMenu.add(fileExportImageMenuItem);
		
		fileExportNodeCSVMenuItem = new JMenuItem("Export Nodes as CSV");
		fileExportNodeCSVMenuItem.addActionListener(handler);
		fileMenu.add(fileExportNodeCSVMenuItem);

		fileMenu.addSeparator();

		fileQuitMenuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
		fileQuitMenuItem.addActionListener(handler);
		fileQuitMenuItem.setAccelerator(Utils.getKeyStrokeForMenu(KeyEvent.VK_Q));
		fileMenu.add(fileQuitMenuItem);
		
		
		// Create the view menu
		
		viewMenu = new JMenu("View");
		mainMenu.add(viewMenu);
		
		viewDrawArrowsItem = new JCheckBoxMenuItem("Draw Arrows");
		viewDrawArrowsItem.setState(display.isDrawingArrows());
		viewDrawArrowsItem.addActionListener(handler);
		viewMenu.add(viewDrawArrowsItem);
		
		viewDrawNodesAsPointsItem = new JCheckBoxMenuItem("Draw Nodes as Points");
		viewDrawNodesAsPointsItem.setState(display.isDrawingNodesAsPoints());
		viewDrawNodesAsPointsItem.addActionListener(handler);
		viewMenu.add(viewDrawNodesAsPointsItem);
		
		viewDrawSplinesItem = new JCheckBoxMenuItem("Draw Splines");
		viewDrawSplinesItem.setState(display.isDrawingSplines());
		viewDrawSplinesItem.addActionListener(handler);
		viewMenu.add(viewDrawSplinesItem);

		viewMenu.addSeparator();

		viewFullscreenMenuItem = new JMenuItem("Fullscreen", KeyEvent.VK_F11);
		viewFullscreenMenuItem.addActionListener(handler);
		viewFullscreenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		viewMenu.add(viewFullscreenMenuItem);
		
		
		// Create the transform menu
		
		transformMenu = new JMenu("Transform");
		mainMenu.add(transformMenu);

		transformFileGraphMenuItem = new JMenuItem("Generate File Graph");
		transformFileGraphMenuItem.addActionListener(handler);
		transformMenu.add(transformFileGraphMenuItem);

		transformProcessGraphMenuItem = new JMenuItem("Generate Process Graph");
		transformProcessGraphMenuItem.addActionListener(handler);
		transformMenu.add(transformProcessGraphMenuItem);

		transformCollapseGraphMenuItem = new JMenuItem("Collapse Versions");
		transformCollapseGraphMenuItem.addActionListener(handler);
		transformMenu.add(transformCollapseGraphMenuItem);
		
		transformApplyFilterMenuItem = new JMenuItem("Apply Current Filters");
		transformApplyFilterMenuItem.addActionListener(handler);
		transformMenu.add(transformApplyFilterMenuItem);

		transformMenu.addSeparator();

		transformHierarchicalLayoutMenuItem = new JMenuItem("Compute Hierarchical Layout");
		transformHierarchicalLayoutMenuItem.addActionListener(handler);
		transformMenu.add(transformHierarchicalLayoutMenuItem);

		transformSpringLayoutMenuItem = new JMenuItem("Compute Spring Layout");
		transformSpringLayoutMenuItem.addActionListener(handler);
		transformMenu.add(transformSpringLayoutMenuItem);

		transformMenu.addSeparator();
		
		graphDerivationTreeMenu = new JMenu("Exisiting derivations");
		transformMenu.add(graphDerivationTreeMenu);
		
		
		// Finalize the window creation
		
		setJMenuBar(mainMenu);
		
		pack();
		setDocument(null);
		Utils.centerWindow(this);

	}
	
	
	/**
	 * Run the application
	 */
	public void run() {
		run(null);
	}
	
	
	/**
	 * Run the application
	 * 
	 * @param file the file to load, or null if none
	 */
	public void run(String file) {
		
		setVisible(true);
		
		
		// Load the file, if appropriate
		
		if (file != null && !"".equals(file)) load((new File(file)).toURI());
	}
	
	
	/**
	 * Load the given URI
	 * 
	 * @param uri the URI to load
	 */
	public void load(URI uri) {

		JobMasterDialog d = null;

		try {
			d = new JobMasterDialog(MainFrame.this, "Loading the graph");
			setDocument(Document.load(uri, d));
		}
		catch (JobCanceledException e) {
			if (d != null) d.dispose();
		}
		catch (Throwable e) {

			if (e.getCause() instanceof ParserException) e = e.getCause();
			if (e.getCause() instanceof IOException) e = e.getCause();

			if (!(e.getCause() instanceof IOException)) e.printStackTrace();

			JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
					"Failed to open", JOptionPane.ERROR_MESSAGE);
			if (d != null) d.dispose();
		}
	}


	/**
	 * Return the document
	 *
	 * @return the document
	 */
	public Document getDocument() {
		return document;
	}


	/**
	 * Set the document
	 *
	 * @param doc the document
	 */
	public void setDocument(Document doc) {
		
		if (doc != null && doc.getPGraph() == null) {
			throw new IllegalArgumentException("The provenance graph is null");
		}

		document = doc;
		graph = null;
		
		
		// Handle the case in which there is no document to be loaded

		if (document == null) {
			
			display.setGraph(null, null);
			display.setDecorator(null);
			
			legendPanel.setGraph(null);
			searchPanel.setGraph(null);
			nodeDetailPanel.clear();
			
			timeSlider.setValue(0);
			timeSlider.setMinimum(0);
			timeSlider.setMaximum(100);
			timeSlider.setEnabled(false);
			
			graphDerivationTreeMenu.removeAll();
			
			setTitle(TITLE);
			return;
		}
		
		
		// Initialize the time filter
		
		timeFilter.setPGraph(doc.getPGraph());
		timeSlider.setMinimum(0);
		timeSlider.setMaximum((int) ((doc.getPGraph().getStat().getTimeUnadjustedMax() - doc.getPGraph().getTimeBase()) * 10 + 1));
		timeSlider.setValue(timeSlider.getMaximum());
		timeSlider.setEnabled(true);
		if (10 * timeSlider.getWidth() / timeSlider.getMaximum() > 5) {
			timeSlider.setMinorTickSpacing(10);
		}
		else {
			timeSlider.setMinorTickSpacing(0);
		}
		if (600 * timeSlider.getWidth() / timeSlider.getMaximum() > 5) {
			timeSlider.setMajorTickSpacing(600);
		}
		else {
			timeSlider.setMajorTickSpacing(60 * 600);
		}
		
		
		// Initialize the document
		
		setGraph(doc.getPGraph());
		
		
		// Set the window
		
		setTitle(doc.getFile() == null ? doc.getName() : doc.getFile().getName() + " : " + TITLE);
		filterPanel.clear();
		searchPanel.clear();
		nodeDetailPanel.clear();
		
		
		// Compute the timeline
		
		computeTimeline();
	}
	
	
	/**
	 * Compute the timeline
	 */
	protected void computeTimeline() {
		
		timelineTree.clear();
		timelineFilter.acceptAll();
		System.gc();
		
		if (document == null) {
			timelineTree.repaint();
			return;
		}
		
		
		// Construct the timeline
		
		TimelineEvent<PObject> root = (new ProcessTreeSummarizer()).computeProcessTimeline(document.getPGraph());
		
		
		// Update the table
		
		timelineTree.setRange((document.getPGraph().getStat().getTimeUnadjustedMin() - document.getPGraph().getTimeBase()),
							  (document.getPGraph().getStat().getTimeUnadjustedMax() - document.getPGraph().getTimeBase()));
		timelineTree.setRoot(root);
		timelineTree.repaint();
	}
	
	
	/**
	 * Set the active graph
	 * 
	 * @param g the graph to display (must be a part of the current document)
	 */
	public void setGraph(PGraph g) {
		
		// Sanity check
		
		if (g == null) {
			throw new IllegalArgumentException("Cannot set null active graph");
		}
		
		if (document == null) {
			throw new IllegalStateException("Cannot set an active graph before setting the document");
		}
		
		
		// Compute the graph layout if necessary
		
		if (g.getDefaultLayout() == null) {
			try {
				computeGraphLayout(g, new Graphviz("dot"));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		
		// Generate the derivations tree menu
		
		graphDerivationTreeMenu.removeAll();
		
		if (g.getParent() != null) {
			JMenuItem m = new JMenuItem("Parent", KeyEvent.VK_BACK_SPACE);
			m.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					setGraph(graph.getParent());
				}
			});
			m.setAccelerator(Utils.getKeyStrokeForMenu(KeyEvent.VK_BACK_SPACE));
			graphDerivationTreeMenu.add(m);
		}
		
		for (PGraph x : g.getDerivedGraphs()) {
			JMenuItem m = new JMenuItem(x.getDescription());
			m.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					JMenuItem m = Utils.<JMenuItem>cast(arg0.getSource());
					setGraph(graph.getDerivedGraph(m.getText()));
				}
			});
			graphDerivationTreeMenu.add(m);
		}
		
		
		// Replace the active graph
		
		this.graph = g;
		
		
		// Update the rest of the window
		
		legendPanel.setGraph(g);
		searchPanel.setGraph(g);
		
		
		// Finish
		
		graphLayoutChanged();
	}
	
	
	/**
	 * Update the graph display after the layout has been recomputed 
	 */
	protected void graphLayoutChanged() {
		
		if (document.getPGraph() != null) {
			// Use the settings from the document graph, not the active graph
			decorator.setPGraph(document.getPGraph());
			filterFactory.setPGraph(document.getPGraph());
		}
		
		display.setGraph(graph, graph.getDefaultLayout());
		display.setDecorator(decorator);
		display.setZoom(0);
	}
	
	
	/**
	 * Compute the graph layout using a graphical job master
	 * 
	 * @param g the graph to recompute
	 * @param algorithm the graph layout algorithm
	 * @return the new layout
	 */
	protected GraphLayout computeGraphLayout(PGraph g, GraphLayoutAlgorithm algorithm) throws JobException {
		
		Pointer<PGraph> pg = new Pointer<PGraph>(g);
		
		JobMasterDialog d = new JobMasterDialog(MainFrame.this, "Recomputing the Layout");
		if (g.getRootSummaryNode() == null) {
			try {
				getDocument().addDefaultSummarizationJobs(g, d);
			}
			catch (Exception e) {
				throw new JobException(e);
			}
		}
		GraphLayoutAlgorithmJob j = new GraphLayoutAlgorithmJob(algorithm, Utils.<Pointer<BaseGraph>>cast(pg));
		d.add(j);
		d.run();
		
		return j.getResult();
	}


    /**
     * The event handler
	 */
	private class EventHandler implements ActionListener, ChangeListener, ListSelectionListener,
										  GraphDisplayListener<PNode, PEdge, PSummaryNode, PGraph> {

		/**
		 * The constructor for instances of class EventHandler
		 */
		public EventHandler() {
		}
		

		/**
		 * Handler for actions performed by the user
		 *
		 * @param event the event description
		 */
		public void actionPerformed(ActionEvent event) {

			
			//
			// File / Open
			//
			
			if (event.getSource() == fileOpenMenuItem) {
			
				File file = FileChoosers.chooseGraphFile(MainFrame.this, "Open a graph file");
				if (file == null) return;
				
				load(file.toURI());
			}

			
			//
			// File / Open CPL Object
			//
			
			if (event.getSource() == fileOpenCPLMenuItem) {
			
				try {
					load(new URI("cpl", "object", null));
				} catch (URISyntaxException e) {
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
							"Internal Error", JOptionPane.ERROR_MESSAGE);
				}
			}


			//
			// File / Save
			//

			if (event.getSource() == fileSaveMenuItem) {

				File file = FileChoosers.chooseDocumentFile(MainFrame.this, "Save a graph file", false);
				if (file == null) return;
				if (!Utils.checkOverwrite(MainFrame.this, file)) return;

				try {
					document.save(file, new JobMasterDialog(MainFrame.this, "Saving the project"));
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to save", JOptionPane.ERROR_MESSAGE);
				}
			}


			//
			// File / Export Image
			//

			if (event.getSource() == fileExportImageMenuItem) {

				File file = GraphicUtils.chooseImageFile(MainFrame.this, "Export an image", false);
				if (file == null) return;
				if (!Utils.checkOverwrite(MainFrame.this, file)) return;

				try {
					int width = display.getWidth();
					int height = display.getHeight();
					BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					display.render(b.getGraphics(), width, height);
					GraphicUtils.saveImage(file, b);
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to export", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			
			//
			// File / Export Nodes as CSV
			//
			
			if (event.getSource() == fileExportNodeCSVMenuItem) {
				
				File file = FileChoosers.chooseCSVFile(MainFrame.this, "Export nodes as a CSV", false);
				if (file == null) return;
				if (!Utils.checkOverwrite(MainFrame.this, file)) return;

				try {
					document.getPGraph().exportNodes(file.getAbsolutePath(), null);
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to export", JOptionPane.ERROR_MESSAGE);
				}
			}


			//
			// File / Quit
			//

			if (event.getSource() == fileQuitMenuItem) {
				GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
				if (device.getFullScreenWindow() == MainFrame.this) device.setFullScreenWindow(null);
				setVisible(false);
				dispose();
			}


			//
			// View / Draw Arrows
			//

			if (event.getSource() == viewDrawArrowsItem) {
				display.setDrawingArrows(viewDrawArrowsItem.getState());
				display.repaint();
			}


			//
			// View / Draw Nodes as Points
			//

			if (event.getSource() == viewDrawNodesAsPointsItem) {
				display.setDrawingNodesAsPoints(viewDrawNodesAsPointsItem.getState());
				display.repaint();
			}


			//
			// View / Draw Splines
			//

			if (event.getSource() == viewDrawSplinesItem) {
				display.setDrawingSplines(viewDrawSplinesItem.getState());
				display.repaint();
			}


			//
			// View / Full-screen
			//

			if (event.getSource() == viewFullscreenMenuItem) {
				GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
				if (device.getFullScreenWindow() == MainFrame.this) {
					device.setFullScreenWindow(null);
				}
				else {
					device.setFullScreenWindow(MainFrame.this);
				}
			}

			
			//
			// Transform / Compute Hierarchical Layout
			//
			
			if (event.getSource() == transformHierarchicalLayoutMenuItem) {
			
				if (document == null) return;

				try {
					graph.setDefaultLayout(computeGraphLayout(graph, new Graphviz("dot")));
					graphLayoutChanged();
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to recompute the layout", JOptionPane.ERROR_MESSAGE);
				}
			}

			
			//
			// Transform / Compute Spring Layout
			//
			
			if (event.getSource() == transformSpringLayoutMenuItem) {
			
				if (document == null) return;

				try {
					graph.setDefaultLayout(computeGraphLayout(graph, new Graphviz("sfdp")));
					graphLayoutChanged();
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to recompute the layout", JOptionPane.ERROR_MESSAGE);
				}
			}

			
			//
			// Transform / Generate File Graph
			//
			
			if (event.getSource() == transformFileGraphMenuItem) {
			
				if (document == null) return;

				try {
					PASSFilter.TypeCode filter = new PASSFilter.TypeCode();
					filter.getAttribute().setOperator("=");
					filter.setTypeCode(PObject.Type.ARTIFACT);
					
					PGraph p = graph.createSummaryGraph(filter, "File graph");
					computeGraphLayout(p, new Graphviz("dot"));
					setGraph(p);
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to recompute the layout", JOptionPane.ERROR_MESSAGE);
				}
			}

			
			//
			// Transform / Generate Process Graph
			//
			
			if (event.getSource() == transformProcessGraphMenuItem) {
			
				if (document == null) return;

				try {
					PASSFilter.TypeCode filter = new PASSFilter.TypeCode();
					filter.getAttribute().setOperator("=");
					filter.setTypeCode(PObject.Type.PROCESS);
					
					PGraph p = graph.createSummaryGraph(filter, "Process graph");
					computeGraphLayout(p, new Graphviz("dot"));
					setGraph(p);
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to recompute the layout", JOptionPane.ERROR_MESSAGE);
				}
			}

			
			//
			// Transform / Collapse Versions
			//
			
			if (event.getSource() == transformCollapseGraphMenuItem) {
			
				if (document == null) return;

				try {
					PGraph p = graph.createCollapsedGraph();
					computeGraphLayout(p, new Graphviz("dot"));
					setGraph(p);
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to recompute the layout", JOptionPane.ERROR_MESSAGE);
				}
			}

			
			//
			// Transform / Apply Filters
			//
			
			if (event.getSource() == transformApplyFilterMenuItem) {
			
				if (document == null) return;

				try {
					PGraph p = graph.createSummaryGraph(display.getFilters());
					computeGraphLayout(p, new Graphviz("dot"));
					setGraph(p);
				}
				catch (Throwable e) {
					if (!(e instanceof JobCanceledException)) e.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(),
						"Failed to recompute the layout", JOptionPane.ERROR_MESSAGE);
				}
			}
		}


		/**
		 * Handle state changes
		 * 
		 * @param event the description of the event
		 */
		@Override
		public void stateChanged(ChangeEvent event) {
			
			//
			// Time Slider
			//
			if (event.getSource() == timeSlider) {
				Attribute<Double> a = Utils.<Attribute<Double>>cast(timeFilter.getAttribute());
				a.setWithSilentFail(timeSlider.getValue() / 10.0);
				
				double t = timeSlider.getValue() / 10.0;
				int h = ((int) t) / 3600;
				int m = ((int) t) / 60 - 60 * h;
				double s = t - 3600 * h - 60 * m;
				
				String l = "    ";
				if (h > 0) l += "" + h + "h ";
				if (m > 0) l += "" + m + "m ";
				if (t > 0) l += "" + ((int) s) + "." + (timeSlider.getValue() % 10) + "s ";
				timeLabel.setText(l);
				
				display.repaint();
			}
		}


		/**
		 * Handle list selection changes
		 * 
		 * @param event the description of the event 
		 */
		@Override
		public void valueChanged(ListSelectionEvent event) {
			
			//
			// Timeline
			//
			if (event.getSource() == timelineTree) {
				
				if (timelineTree.isSelectionEmpty()) {
					timelineFilter.acceptAll();
				}
				else {
					TimelineEvent<PObject> start = timelineTree.getSelectedEvent();
					
					timelineFilter.clear();
					HashSet<PNode> set = new HashSet<PNode>();
					
					
					// Enqueue all versions (nodes) associated with this process
					
					LinkedList<PNode> queue = new LinkedList<PNode>();
					for (PNode se : start.getValue().getVersions()) {
						queue.add(se);
						set.add(se);
					}
					
					
					// Create a set of all PNodes relevant to the selected process
					
					// NOTE: This does a good job on most sections of most PASS graphs;
					// this algorithm should be revisited
					
					// Add all processes related through VERSION and FORKPARENT,
					// plus their immediate ancestors and descendants
					
					while (!queue.isEmpty()) {
						PNode n = queue.removeFirst();
						
						for (PEdge g : n.getIncomingEdges()) {
							PEdge.Type t = g.getType();
							if (t != PEdge.Type.VERSION && t != PEdge.Type.CONTROL) set.add(g.getFrom());
						}
						for (PEdge g : n.getOutgoingEdges()) {
							PEdge.Type t = g.getType();
							if (t != PEdge.Type.VERSION && t != PEdge.Type.CONTROL) set.add(g.getTo());
						}
						
						for (PEdge e : n.getIncomingEdges()) {
							PNode x = e.getFrom();
							PEdge.Type t = e.getType();
							
							if (t != PEdge.Type.VERSION && t != PEdge.Type.CONTROL) continue;
							if (!set.contains(x)) {
								set.add(x);
								queue.addLast(x);
							}
						}
					}
					
					
					// Set the filter
					
					timelineFilter.set(set);
				}
				
				display.repaint();
			}
		}
		
		
		/**
		 * The user clicked a node
		 * 
		 * @param node the node
		 */
		@Override
		public void nodeClicked(PNode node) {
		}

		
		/**
		 * The user clicked a summary node
		 * 
		 * @param node the node
		 */
		@Override
		public void summaryNodeClicked(PSummaryNode node) {
		}


		/**
		 * The user clicked the background
		 */
		@Override
		public void backgroundClicked() {
		}
		
		
		/**
		 * The selection has changed
		 */
		@Override
		public void selectionChanged() {
			
			Set<BaseNode> nodes = display.getSelectedBaseNodes();
			
			if (nodes.isEmpty()) {
				nodeDetailPanel.clear();
			}
			else if (nodes.size() == 1) {
				nodeDetailPanel.setNode(nodes.iterator().next());
			}
			else {
				nodeDetailPanel.clear();
			}
		}
	}
}
