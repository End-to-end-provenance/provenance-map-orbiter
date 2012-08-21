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
import edu.harvard.util.filter.*;
import edu.harvard.util.graph.*;
import edu.harvard.util.gui.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Search by attribute
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class SearchPanel extends JPanel {
	
	private String title;
	
	private FilterSet<PNode> filters;
	private FilterFactory<PNode> factory;
	private EventHandler handler;
	
	private PGraph graph;
	private GraphDisplay<PNode, PEdge, PSummaryNode, PGraph> display;
	
	private JLabel titleLabel;
	private FilterListPanel<PNode> filterPanel;
	private JSplitPane splitPane;
	
	private JPanel resultPanel;
	private JLabel resultLabel;
	private JList resultList;
	private DefaultListModel resultListModel;
	private JScrollPane resultScroll;


	/**
	 * Create an instance of class SearchPanel
	 *
	 * @param title the title
	 * @param factory the filter factory
	 * @param display the display component
	 */
	public SearchPanel(String title, FilterFactory<PNode> factory, GraphDisplay<PNode, PEdge, PSummaryNode, PGraph> display) {
		
		this.title = title;
		this.factory = factory;
		this.display = display;
		
		this.graph = null;
		this.handler = new EventHandler();
		
		
		// Filters
		
		this.filters = new FilterSet<PNode>(false);
		this.filters.addFilterListener(handler);
		this.display.addHighlightFilter(filters);
		
		this.display.getFilters().addFilterListener(handler);
		
		
		// Component basics
		
		setPreferredSize(new Dimension(200, 320));
		setLayout(new BorderLayout());
		
		
		// Add the title
		
		if (this.title != null) {
			titleLabel = new JLabel(this.title);
			add(titleLabel, BorderLayout.NORTH);
		}
		
		
		// Filter panel
		
		filterPanel = new FilterListPanel<PNode>("Search by Attribute", filters, this.factory);
		filterPanel.setOpaque(false);
		
		
		// Results
		
		resultPanel = new JPanel();
		resultPanel.setLayout(new BorderLayout());
		resultPanel.setOpaque(false);
		
		resultLabel = new JLabel("Results");
		resultPanel.add(resultLabel, BorderLayout.NORTH);
		
		resultListModel = new DefaultListModel();
		
		resultList = new JList(resultListModel);
		resultList.addListSelectionListener(handler);
		resultList.addMouseListener(handler);
		resultList.setCellRenderer(new PNodeListRenderer());
		
		resultScroll = new JScrollPane(resultList);
		resultPanel.add(resultScroll, BorderLayout.CENTER);
		
		
		// Add the filter list
		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filterPanel, resultPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(200);
		splitPane.setResizeWeight(0.2);
		splitPane.setOpaque(false);
		
		add(splitPane, BorderLayout.CENTER);
	}
	
	
	/**
	 * Clear the search by filters
	 */
	public void clear() {
		filterPanel.clear();
	}
	
	
	/**
	 * Set the provenance graph
	 * 
	 * @param graph the graph
	 */
	public void setGraph(PGraph graph) {
		this.graph = graph;
		clear();
	}


	/**
	 * The event handler
	 */
	private class EventHandler extends MouseAdapter implements ListSelectionListener, FilterListener<PNode> {

		/**
		 * The constructor for instances of class EventHandler
		 */
		public EventHandler() {
		}
		
		/**
		 * Callback for when the filter changed
		 *
		 * @param filter the filter
		 */
		public void filterChanged(Filter<PNode> filter) {
			
			resultListModel.clear();
			
			if (filters.isEmpty()) {
				return;
			}
			
			for (PNode n : graph.getNodes()) {
				
				if (!n.isVisible()) continue;
				if (!display.getFilters().accept(n)) continue;
				if (!filters.accept(n)) continue;
				
				resultListModel.addElement(n);
			}
		}

		/**
		 * Callback for table selection events
		 *
		 * @param e the event description
		 */
		public void valueChanged(ListSelectionEvent e) {
			
			Object os = resultList.getSelectedValue();
			if (os == null) return;
			
			if (os instanceof BaseNode) {
				BaseNode n = (BaseNode) os;
				
				display.selectNode(n);
				display.repaint();
			}
		}
		
		/**
		 * Callback for clicking
		 * 
		 * @param e the event description
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			
			if (e.getSource() == resultList && e.getClickCount() >= 2) {
				
				Object os = resultList.getSelectedValue();
				if (os == null) return;
				
				if (os instanceof BaseNode) {
					BaseNode n = (BaseNode) os;
					
					display.selectNode(n);
					display.centerAt(n);
				}
			}
		}
	}
	

	/**
	 * List renderer for a list of PNodes
	 * 
	 * @author Peter Macko
	 */
	class PNodeListRenderer extends JLabel implements ListCellRenderer {

		// Based on: http://download.oracle.com/javase/tutorial/uiswing/components/combobox.html#renderer
		
		
		/**
		 * Create an instance of class PNodeListRenderer
		 */
		public PNodeListRenderer() {
			setOpaque(true);
			setHorizontalAlignment(LEFT);
			setVerticalAlignment(CENTER);
		}

		
		/**
		 * This method finds the image and text corresponding to the selected
		 * value and returns the label, set up to display the text and image.
		 * 
		 * @param list the list
		 * @param value the value to display
		 * @param index the index of the value
		 * @param isSelected whether the value is selected
		 * @param cellHasFocus whether the value has focus
		 * @return the component to display
		 */
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			int h = getFont().getSize();
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			
			if (value instanceof PNode) {
				setText(((PNode) value).getLabel());
				setIcon(new NodeEdgeIcon((PNode) value, h, h, display.getDecorator()));
			}
			else if (value instanceof BaseNode) {
				setText(((BaseNode) value).getLabel());
			}
			else {
				setText("<" + value + ">");
			}

			return this;
		}

	}
}
