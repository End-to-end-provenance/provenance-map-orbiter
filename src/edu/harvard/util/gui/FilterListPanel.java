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

package edu.harvard.util.gui;

import edu.harvard.util.attribute.*;
import edu.harvard.util.filter.*;
import edu.harvard.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


/**
 * The list of filters
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class FilterListPanel<T> extends JPanel {
	
	private String title;
	
	private FilterSet<T> filters;
	private FilterFactory<T> factory;
	private EventHandler handler;
	
	private JLabel titleLabel;
	private AttributeTable filterTable;
	private JScrollPane scrollPane;

	private JPanel bottomPanel;
	private JPanel addPanel;
	private JComboBox filterCombo;
	private JButton addButton;
	private JPanel removePanel;
	private JButton removeButton;
	private JButton clearButton;


	/**
	 * Create an instance of class FilterListGUI
	 *
	 * @param title the title
	 * @param filters the filter set on which to operate
	 * @param factory the filter factory
	 */
	public FilterListPanel(String title, FilterSet<T> filters, FilterFactory<T> factory) {
		
		this.filters = filters;
		this.factory = factory;
		this.title = title;
		
		this.handler = new EventHandler();
		
		
		// Component basics
		
		//setMinimumSize(new Dimension(200, 320));
		setPreferredSize(new Dimension(200, 320));
		
		setLayout(new BorderLayout());
		
		
		// Add the title
		
		if (this.title != null) {
			titleLabel = new JLabel(this.title);
			add(titleLabel, BorderLayout.NORTH);
		}
		
		
		// Add the filter list
		
		filterTable = new AttributeTable(true);
		for (Filter<T> f : filters.getFilters()) {
			filterTable.add(f);
		}

		filterTable.getSelectionModel().addListSelectionListener(handler);
		filterTable.setFillsViewportHeight(true);
		
		scrollPane = new JScrollPane(filterTable);
		add(scrollPane, BorderLayout.CENTER);


		// Add the bottom panel

		bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
		bottomPanel.setOpaque(false);
		add(bottomPanel, BorderLayout.SOUTH);


		// The add filter components

		addPanel = new JPanel();
		addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.LINE_AXIS));
		addPanel.setOpaque(false);
		bottomPanel.add(addPanel);

		addPanel.add(new JLabel("Add: "));
		
		filterCombo = new JComboBox(factory.getFilterNames().toArray());
		filterCombo.addActionListener(handler);
		addPanel.add(filterCombo);

		addButton = null;
		//addButton = new JButton("Add");
		//addButton.addActionListener(handler);
		//addPanel.add(addButton);


		// The remove filter components

		removePanel = new JPanel();
		removePanel.setLayout(new BoxLayout(removePanel, BoxLayout.LINE_AXIS));
		removePanel.setOpaque(false);
		bottomPanel.add(removePanel);

		removePanel.add(new JLabel("Remove: "));
		
		removeButton = new JButton("Selected");
		removeButton.addActionListener(handler);
		removeButton.setEnabled(filterTable.getSelectedRowCount() > 0);
		removePanel.add(removeButton);
		
		clearButton = new JButton("All");
		clearButton.addActionListener(handler);
		clearButton.setEnabled(filterTable.getSelectedRowCount() > 0);
		removePanel.add(clearButton);
	}
	
	
	/**
	 * Clear the filters
	 */
	public void clear() {
		while (filterTable.getAttributeCount() > 0) {
			WithAttribute w = filterTable.get(0);
			filterTable.remove(w);
			filters.remove(Utils.<Filter<T>>cast(w));
		}
	}


	/**
	 * The event handler
	 */
	private class EventHandler implements ActionListener, ListSelectionListener {

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
			// Add a new filter
			//

			if (event.getSource() == addButton || (addButton == null && event.getSource() == filterCombo)) {
				try {

					String name = (String) filterCombo.getSelectedItem();
					Filter<T> filter = factory.create(name);

					filterTable.add(filter);
					filters.add(filter);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
						"Adding a filter", JOptionPane.ERROR_MESSAGE);
				}

				removeButton.setEnabled(filterTable.getSelectedRowCount() > 0);
				clearButton.setEnabled(filterTable.getAttributeCount() > 0);
			}


			//
			// Remove an existing filter
			//

			if (event.getSource() == removeButton) {
				try {

					int[] s = filterTable.getSelectedRows();
					if (s.length == 0) return;

					for (int i = s.length - 1; i >= 0; i--) {
						WithAttribute w = filterTable.get(s[i]);
						filterTable.remove(w);
						filters.remove(Utils.<Filter<T>>cast(w));
					}
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
						"Removing a filter", JOptionPane.ERROR_MESSAGE);
				}
				
				removeButton.setEnabled(filterTable.getSelectedRowCount() > 0);
				clearButton.setEnabled(filterTable.getAttributeCount() > 0);
			}


			//
			// Remove all existing filters
			//

			if (event.getSource() == clearButton) {
				try {
					for (int i = filterTable.getAttributeCount() - 1; i >= 0; i--) {
						WithAttribute w = filterTable.get(i);
						filterTable.remove(w);
						filters.remove(Utils.<Filter<T>>cast(w));
					}
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
						"Removing a filter", JOptionPane.ERROR_MESSAGE);
				}
				
				removeButton.setEnabled(filterTable.getSelectedRowCount() > 0);
				clearButton.setEnabled(filterTable.getAttributeCount() > 0);
			}
		}


		/**
		 * Callback for table selection events
		 *
		 * @param e the event description
		 */
		public void valueChanged(ListSelectionEvent e) {
			removeButton.setEnabled(filterTable.getSelectedRowCount() > 0);
		}
	}
}
