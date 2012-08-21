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

package edu.harvard.pass.orbiter.gui;

import edu.harvard.pass.*;
import edu.harvard.util.gui.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;


/**
 * Search by attribute
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class LegendPanel extends JPanel {
	
	private String title;
	private EventHandler handler;
	
	private PGraph graph;
	private GraphDisplay<PNode, PEdge, PSummaryNode, PGraph> display;
	private PASSDecorator decorator;
	
	private TreeMap<String, PASSDecorator.ColorNodesBy> nameToColorNodesBy;
	private HashMap<PASSDecorator.ColorNodesBy, String> colorNodesByToName;
	
	private JScrollPane scroll;
	private JPanel panel;

	private JLabel titleLabel;
	
	private JLabel nodeColorTitleLabel;
	private JComboBox nodeColorComboBox;


	/**
	 * Create an instance of class LegendPanel
	 *
	 * @param title the title
	 * @param display the display component
	 */
	public LegendPanel(String title, GraphDisplay<PNode, PEdge, PSummaryNode, PGraph> display) {
		
		this.title = title;
		this.display = display;
		
		this.graph = null;
		this.handler = new EventHandler();
		
		this.decorator = this.display.getDecorator() instanceof PASSDecorator ? (PASSDecorator) this.display.getDecorator() : null;
		
		
		// Initialize the "color nodes by" maps
		
		nameToColorNodesBy = new TreeMap<String, PASSDecorator.ColorNodesBy>();
		colorNodesByToName = new HashMap<PASSDecorator.ColorNodesBy, String>();
		
		colorNodesByToName.put(PASSDecorator.ColorNodesBy.TYPE, "Type");
		colorNodesByToName.put(PASSDecorator.ColorNodesBy.PROVRANK, "ProvRank");
		colorNodesByToName.put(PASSDecorator.ColorNodesBy.DEPTH, "Depth");
		
		for (Map.Entry<PASSDecorator.ColorNodesBy, String> e : colorNodesByToName.entrySet()) {
			nameToColorNodesBy.put(e.getValue(), e.getKey());
		}
		
		Vector<String> colorNodesByNames = new Vector<String>();
		colorNodesByNames.addAll(nameToColorNodesBy.keySet());
		
		
		// Initialize the panel
		
		setLayout(new BorderLayout());
		
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.setOpaque(true);
		
		scroll = new JScrollPane(panel);
		scroll.setBorder(null);
		scroll.setOpaque(true);
		add(scroll, BorderLayout.CENTER);
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		
		int gridy = 0;
		
		
		// Header

		if (this.title != null) {
			titleLabel = new JLabel(this.title);
			titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 3;
			c.weightx = 1;
			c.weighty = 0;
			panel.add(titleLabel, c);
			c.weightx = 0;
			c.gridwidth = 1;
			
			gridy++;
	
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 3;
			panel.add(new JLabel(" "), c);
			c.gridwidth = 1;
	
			gridy++;
		}
		
		
		// Node colors
		
		nodeColorTitleLabel = new JLabel("Nodes, color by:  ");
		nodeColorTitleLabel.setVerticalAlignment(JLabel.CENTER);
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 2;
		panel.add(nodeColorTitleLabel, c);
		c.gridwidth = 1;
		
		nodeColorComboBox = new JComboBox(colorNodesByNames);
		nodeColorComboBox.setSelectedItem(colorNodesByToName.get(decorator.getColorNodesBy()));
		nodeColorComboBox.addActionListener(handler);
		c.gridx = 2;
		c.gridy = gridy;
		c.weightx = 1;
		panel.add(nodeColorComboBox, c);
		c.weightx = 0;
		
		int h = nodeColorTitleLabel.getFont().getSize();
		int w = h * 3;

		gridy++;
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel(" "), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridwidth = 3;
		
		c.gridy = gridy++;
		panel.add(new JLabel("Agent",
				new NodeEdgeIcon(decorator.colorAgent, decorator.nodeOutlineColor,
						NodeEdgeIcon.Shape.ELLIPSE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Artifact or File",
				new NodeEdgeIcon(decorator.colorArtifact, decorator.nodeOutlineColor,
						NodeEdgeIcon.Shape.ELLIPSE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Process",
				new NodeEdgeIcon(decorator.colorProcess, decorator.nodeOutlineColor,
						NodeEdgeIcon.Shape.ELLIPSE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Non-Provenance File",
				new NodeEdgeIcon(decorator.colorNPFile, decorator.nodeOutlineColor,
						NodeEdgeIcon.Shape.ELLIPSE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Pipe",
				new NodeEdgeIcon(decorator.colorPipe, decorator.nodeOutlineColor,
						NodeEdgeIcon.Shape.ELLIPSE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Other",
				new NodeEdgeIcon(decorator.colorOther, decorator.nodeOutlineColor,
						NodeEdgeIcon.Shape.ELLIPSE, w, h),
				JLabel.LEFT), c);
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel(" "), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel(" "), c);
		c.gridwidth = 1;
		
		gridy++;
		
		
		// Edges
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel("Edges"), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel(" "), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridwidth = 3;
		
		c.gridy = gridy++;
		panel.add(new JLabel("Data Dependency",
				new NodeEdgeIcon(decorator.colorData, decorator.colorData,
						NodeEdgeIcon.Shape.LINE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Control Dependency",
				new NodeEdgeIcon(decorator.colorControl, decorator.colorControl,
						NodeEdgeIcon.Shape.LINE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Version Edge",
				new NodeEdgeIcon(decorator.colorVersion, decorator.colorVersion,
						NodeEdgeIcon.Shape.LINE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Compound Edge",
				new NodeEdgeIcon(decorator.colorCompound, decorator.colorCompound,
						NodeEdgeIcon.Shape.LINE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Other",
				new NodeEdgeIcon(decorator.colorEdgeOther, decorator.colorEdgeOther,
						NodeEdgeIcon.Shape.LINE, w, h),
				JLabel.LEFT), c);
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel(" "), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel(" "), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel("Edges of Selected Nodes"), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		panel.add(new JLabel(" "), c);
		c.gridwidth = 1;
		
		gridy++;
		
		c.gridx = 0;
		c.gridwidth = 3;
		
		c.gridy = gridy++;
		panel.add(new JLabel("Ancestors",
				new NodeEdgeIcon(decorator.colorEdgeSelectedFrom, decorator.colorEdgeSelectedFrom,
						NodeEdgeIcon.Shape.THICK_LINE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Descendants",
				new NodeEdgeIcon(decorator.colorEdgeSelectedTo, decorator.colorEdgeSelectedTo,
						NodeEdgeIcon.Shape.THICK_LINE, w, h),
				JLabel.LEFT), c);
		
		c.gridy = gridy++;
		panel.add(new JLabel("Both",
				new NodeEdgeIcon(decorator.colorEdgeSelectedBoth, decorator.colorEdgeSelectedBoth,
						NodeEdgeIcon.Shape.THICK_LINE, w, h),
				JLabel.LEFT), c);
		
		
		// Finish
		
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		c.weightx = 1;
		c.weighty = 1;
		panel.add(new JLabel(" "), c);
	}
	
	
	/**
	 * Set the provenance graph
	 * 
	 * @param graph the graph
	 */
	public void setGraph(PGraph graph) {
		this.graph = graph;
	}
	
	
	/**
	 * Get the associated graph
	 * 
	 * @return graph
	 */
	public PGraph getGraph() {
		return graph;
	}


	/**
	 * The event handler
	 */
	private class EventHandler implements ActionListener {

		/**
		 * The constructor for instances of class EventHandler
		 */
		public EventHandler() {
		}

		
		/**
		 * Action event handler
		 * 
		 * @param e the event
		 */
		@Override
		public void actionPerformed(ActionEvent e) {

			// ComboBox
			
			if (e.getSource() == nodeColorComboBox) {
				PASSDecorator.ColorNodesBy c = nameToColorNodesBy.get(nodeColorComboBox.getSelectedItem());
				decorator.setColorNodesBy(c);
				display.repaint();
			}
		}
	}
}
