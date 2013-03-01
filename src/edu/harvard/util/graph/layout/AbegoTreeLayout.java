/*
 * A Collection of Miscellaneous Utilities
 *
 * Copyright 2012
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

package edu.harvard.util.graph.layout;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.xml.transform.sax.TransformerHandler;

import org.abego.treelayout.Configuration;
import org.abego.treelayout.Configuration.Location;
import org.abego.treelayout.NodeExtentProvider;
import org.abego.treelayout.TreeForTreeLayout;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.AbstractTreeForTreeLayout;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import edu.harvard.util.ParserException;
import edu.harvard.util.XMLSerializable;
import edu.harvard.util.XMLUtils;
import edu.harvard.util.graph.BaseEdge;
import edu.harvard.util.graph.BaseGraph;
import edu.harvard.util.graph.BaseNode;
import edu.harvard.util.graph.BaseSummaryNode;
import edu.harvard.util.gui.HasWizardPanelConfigGUI;
import edu.harvard.util.gui.WizardPanel;
import edu.harvard.util.job.JobObserver;


/**
 * An interface to the Abego Tree Layout algorithm
 *
 * @author Peter Macko
 */
public class AbegoTreeLayout implements GraphLayoutAlgorithm, Cloneable, HasWizardPanelConfigGUI, XMLSerializable {

	public static final String[] ROOT_LOCATION_STRINGS = { "Top", "Left", "Right", "Bottom" };
	public static final Location[] ROOT_LOCATION_VALUES = { Location.Top, Location.Left, Location.Right, Location.Bottom };
	
	private Configuration<BaseNode> configuration;
	private Location rootLocation;
	

	/**
	 * Create an instance of class AbegoTreeLayout
	 */
	public AbegoTreeLayout() throws Exception {
		
		rootLocation = Location.Top;
		configuration = new Configuration<BaseNode>() {
			
			@Override
			public Location getRootLocation() {
				return rootLocation;
			}
			
			@Override
			public double getGapBetweenNodes(BaseNode node1, BaseNode node2) {
				return (rootLocation == Location.Top || rootLocation == Location.Bottom) ? 50 : 40;
			}
			
			@Override
			public double getGapBetweenLevels(int nextLevel) {
				return 75;
			}
			
			@Override
			public AlignmentInLevel getAlignmentInLevel() {
				return AlignmentInLevel.Center;
			}
		};
	}

	
	/**
	 * Create a clone of the algorithm with the same settings
	 * 
	 * @return the clone
	 */
	@Override
	public AbegoTreeLayout clone() {
		try {
			AbegoTreeLayout g = new AbegoTreeLayout();
			return g;
		} catch (Exception e) {
			throw new RuntimeException("Cannot re-instantiate the algorithm", e);
		}
	}


	/**
	 * Get the name of the converter
	 *
	 * @return the name
	 */
	public String getName() {
		return "AbegoTreeLayout";
	}


	/**
	 * Initialize the graph layout for the given graph
	 * 
	 * @param graph the input graph
	 * @param levels the number of levels in the hierarchy of summary nodes to precompute 
	 * @param observer the job observer
	 * @return the graph layout
	 */
	@Override
	public GraphLayout initializeLayout(BaseGraph graph, int levels, JobObserver observer) {
				
		// Initialize
		
		final BaseNode root = graph.findBaseTreeRoot();
		if (root == null) throw new IllegalArgumentException("Not a tree");
		final boolean dirOutgoing = root.getIncomingBaseEdges().isEmpty();
		
		Font font = new Font("Courier New", Font.PLAIN, 12);
		@SuppressWarnings("deprecation")
		final FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
		
		if (observer != null) observer.makeIndeterminate();

		
		// Compute the tree layout
		
		TreeForTreeLayout<BaseNode> tree = new AbstractTreeForTreeLayout<BaseNode>(root) {

			@Override
			public List<BaseNode> getChildrenList(BaseNode node) {
				return dirOutgoing ? node.getOutgoingBaseNodes() : node.getIncomingBaseNodes();
			}

			@Override
			public BaseNode getParent(BaseNode node) {
				if (node == root) return null;
				Collection<BaseEdge> parents = !dirOutgoing ? node.getOutgoingBaseEdges() : node.getIncomingBaseEdges();
				assert parents.size() == 1;
				return parents.iterator().next().getBaseOther(node);
			}
		
		};
		
		NodeExtentProvider<BaseNode> extentProvider = new NodeExtentProvider<BaseNode>() {
			
			@Override
			public double getWidth(BaseNode node) {
				return fm.stringWidth(node.getLabel() + "      ");
			}
			
			@Override
			public double getHeight(BaseNode node) {
				return fm.getHeight() * 1.5;
			}
		};
		
		TreeLayout<BaseNode> treeLayout = new TreeLayout<BaseNode>(tree, extentProvider, configuration);
		
		
		// Convert it to the graph layout
		
		GraphLayout layout = new FastGraphLayout(graph, this, getName());

		Stack<BaseNode> stack = new Stack<BaseNode>();
		stack.push(tree.getRoot());
		
		while (!stack.isEmpty()) {
			BaseNode node = stack.pop();
			
			Rectangle2D.Double bounds = treeLayout.getNodeBounds().get(node);
			GraphLayoutNode ln = new GraphLayoutNode(node, bounds.getCenterX(), bounds.getCenterY());
			ln.setSize(bounds.getWidth(), bounds.getHeight());
			layout.addLayoutNode(ln);
			
			for (BaseNode n : tree.getChildren(node)) {
				stack.push(n);
			}
		}

		for (BaseNode from : graph.getBaseNodes()) {
			for (BaseEdge edge : from.getOutgoingBaseEdges()) {
				GraphLayoutNode lf = layout.getLayoutNode(from.getIndex());
				GraphLayoutNode lt = layout.getLayoutNode(edge.getBaseTo().getIndex());
				GraphLayoutEdge le = new GraphLayoutEdge(edge, lf, lt);
				layout.addLayoutEdge(le);
			}
		}
		
		return layout;
	}


	/**
	 * Update an existing layout by incrementally expanding the given summary node
	 * 
	 * @param layout the graph to update
	 * @param node the summary node to expand
	 * @param observer the job observer
	 */
	@Override
	public void updateLayout(GraphLayout layout, BaseSummaryNode node, JobObserver observer) {
		if (observer != null) observer.makeIndeterminate();
		throw new UnsupportedOperationException();
	}


	/**
	 * Determine whether the algorithm produces zoom-based (or zoom-optimized) layouts
	 * 
	 * @return true if it produces zoom-optimized layouts
	 */
	@Override
	public boolean isZoomOptimized() {
		return false;
	}

	
	/**
	 * Compute the layout for the entire graph
	 * 
	 * @param graph the input graph
	 * @param observer the job observer
	 * @return the graph layout
	 */
	@Override
	public GraphLayout computeLayout(BaseGraph graph, JobObserver observer) {
		return initializeLayout(graph, -1, observer);
	}
	

	/**
	 * Get a configuration GUI for the parser 
	 * 
	 * @return a list of WizardPanel's for the GUI configuration, or null if not necessary
	 */
	public List<WizardPanel> createConfigurationGUI() {
		
		List<WizardPanel> r = new Vector<WizardPanel>();
		
		r.add(new ConfigPanel());
		
		return r;
	}
	
	
	/**
	 * Write the object to XML
	 * 
	 * @param hd the XML output
	 * @throws SAXException on error
	 */
	public void writeToXML(TransformerHandler hd) throws SAXException {
		
		AttributesImpl attrs = new AttributesImpl();
		
		attrs.clear();
		hd.startElement("", "", "abego-tree-layout", attrs);
		
		attrs.clear();
		String s = "";
		for (int i = 0; i < ROOT_LOCATION_VALUES.length; i++) {
			if (ROOT_LOCATION_VALUES[i].equals(rootLocation)) {
				s = ROOT_LOCATION_STRINGS[i];
				break;
			}
		}
		hd.startElement("", "", "root-location", attrs);
		hd.characters(s.toCharArray(), 0, s.length());
		hd.endElement("", "", "root-location");
		
		hd.endElement("", "", "abego-tree-layout");
	}
	
	
	/**
	 * Load the object from XML
	 * 
	 * @param element the XML DOM element
	 * @throws SAXException on error
	 */
	public void loadFromXML(Element element) throws SAXException, ParserException {
		
		if (!element.getNodeName().equals("abego-tree-layout")) {
			throw new ParserException("Expected <abego-tree-layout>, found <" + element.getNodeName() + ">");
		}
		
		String s = XMLUtils.getTextValue(element, "root-location");
		for (int i = 0; i < ROOT_LOCATION_STRINGS.length; i++) {
			if (ROOT_LOCATION_STRINGS[i].equalsIgnoreCase(s)) {
				rootLocation = ROOT_LOCATION_VALUES[i];
				break;
			}
		}
	}

	
	/**
	 * Configuration panel
	 */
	private class ConfigPanel extends WizardPanel implements ActionListener {
		
		private JLabel topLabel;
		private JLabel rootLocationLabel;
		private JComboBox rootLocationCombo;

		
		/**
		 * Create an instance of ConfigPanel
		 */
		public ConfigPanel() {
			super("Configure Abego Tree Layout");
			
			
			// Initialize the panel
			
			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			
			int gridy = 0;
			
			
			// Header

			topLabel = new JLabel("Configure graph layout:");
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
			
			
			// Rank-direction
			
			rootLocationLabel = new JLabel("Root location:   ");
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = gridy;
			panel.add(rootLocationLabel, c);
			
			rootLocationCombo = new JComboBox(ROOT_LOCATION_STRINGS);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = gridy;
			panel.add(rootLocationCombo, c);
			
			int i = 0; int index = 0;
			for (Location a : ROOT_LOCATION_VALUES) {
				if (a.equals(rootLocation)) index = i;
				i++;
			}
			
			rootLocationCombo.setSelectedIndex(index);
			
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
			
			updateEnabled();
		}
		
		
		/**
		 * Callback for when the next button was clicked
		 */
		protected void wizardNext() {
			
			rootLocation = ROOT_LOCATION_VALUES[rootLocationCombo.getSelectedIndex()];
		}
		
		
		/**
		 * Update the enabled properties
		 */
		private void updateEnabled() {
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
}
