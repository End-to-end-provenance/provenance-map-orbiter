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
import edu.harvard.util.*;
import edu.harvard.util.graph.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;


/**
 * Node details
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class NodeDetailPanel extends JPanel {
	
	private final DecimalFormat defaultDecimalFormat = new DecimalFormat("#.####"); 
	
	private BaseNode node;
	
	private JPanel basicInfoPanel;
	private JLabel labelLabel;
	private JTextField labelField;
	private JLabel nameLabel;
	private JTextField nameField;
	private JLabel typeLabel;
	private JTextField typeField;
	private JLabel timeLabel;
	private JTextField timeField;
	
	private JLabel attributeLabel;
	private JTable attributeTable;
	private JScrollPane attributeScroll;
	private AttributeTableModel model;
	private Vector<Pair<String, String>> attributes;


	/**
	 * Create an instance of class NodeDetailPanel
	 */
	public NodeDetailPanel() {
		
		node = null;
		
		setLayout(new BorderLayout());
		
		attributes = new Vector<Pair<String,String>>();
		
		
		// Create the basic info panel
		
		basicInfoPanel = new JPanel();
		basicInfoPanel.setLayout(new SpringLayout());
		
		labelLabel = new JLabel("Node:", JLabel.TRAILING);
		labelLabel.setFont(labelLabel.getFont().deriveFont(Font.BOLD));
		labelField = new JTextField();
		labelField.setEditable(false);
		labelField.setBorder(null);
		labelField.setFont(labelField.getFont().deriveFont(Font.BOLD));
		labelLabel.setLabelFor(labelField);
		basicInfoPanel.add(labelLabel);
		basicInfoPanel.add(labelField);
		
		nameLabel = new JLabel("Name:", JLabel.TRAILING);
		nameField = new JTextField();
		nameField.setEditable(false);
		nameField.setBorder(null);
		nameLabel.setLabelFor(nameField);
		basicInfoPanel.add(nameLabel);
		basicInfoPanel.add(nameField);
		
		typeLabel = new JLabel("Type:", JLabel.TRAILING);
		typeField = new JTextField();
		typeField.setEditable(false);
		typeField.setBorder(null);
		typeLabel.setLabelFor(typeField);
		basicInfoPanel.add(typeLabel);
		basicInfoPanel.add(typeField);
		
		timeLabel = new JLabel("Timestamp:", JLabel.TRAILING);
		timeField = new JTextField();
		timeField.setEditable(false);
		timeField.setBorder(null);
		timeLabel.setLabelFor(timeField);
		basicInfoPanel.add(timeLabel);
		basicInfoPanel.add(timeField);
		
		attributeLabel = new JLabel("Attributes:", JLabel.TRAILING);
		basicInfoPanel.add(attributeLabel);
		basicInfoPanel.add(new JLabel(""));
		
		SpringUtilities.makeCompactGrid(basicInfoPanel, 5, 2, 12, 6, 6, 2);
		add(basicInfoPanel, BorderLayout.NORTH);
		
		
		// Add a table with extended attributes
		
		model = new AttributeTableModel();
		
		attributeTable = new JTable(model);
		attributeTable.setFillsViewportHeight(true);
		attributeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		attributeTable.getTableHeader().setReorderingAllowed(false);
		attributeTable.getColumnModel().getColumn(0).setPreferredWidth(128);
		
		Dimension d = attributeTable.getPreferredSize();
		d.setSize(d.getWidth(), 50);
		
		attributeScroll = new JScrollPane(attributeTable);
		attributeScroll.setPreferredSize(d);
		
		add(attributeScroll, BorderLayout.CENTER);
	}
	
	
	/**
	 * Clear the search by filters
	 */
	public void clear() {
		
		node = null;
		
		labelField.setText("");
		nameField.setText("");
		typeField.setText("");
		timeField.setText("");
		
		int s = attributes.size();
		attributes.clear();
		
		if (s > 0) model.fireTableRowsDeleted(0, s - 1);
	}
	
	
	/**
	 * Set the node
	 * 
	 * @param node the node
	 */
	public void setNode(BaseNode node) {
		
		clear();
		this.node = node;
		
		if (node == null) {
			return;
		}
		
		
		// Basic node info
		
		String prefix = "";
		if (node instanceof PNode) {
			prefix = "[" + ((PNode) node).getPublicID() + "] ";
		}
		labelField.setText(prefix + this.node.getLabel());
		
		
		// PNode-specific information
		
		if (node instanceof PNode) {
			PNode n = (PNode) node;
			
			
			// Basic PNode info
			
			nameField.setText(n.getObject().getName());
			typeField.setText(n.getObject().getExtendedType());
			timeField.setText(defaultDecimalFormat.format(n.getTime()));
			
			
			// Extended attributes
			
			TreeMap<String, String> m = new TreeMap<String, String>();
			m.putAll(n.getObject().getExtendedAttributes());
			m.putAll(n.getExtendedAttributes());

			for (Map.Entry<String, String> entry : m.entrySet()) {
				attributes.add(new Pair<String, String>(entry.getKey(), entry.getValue()));
			}
			
			model.fireTableRowsInserted(0, attributes.size());
			model.fireTableDataChanged();
		}
	}

	/**
	 * Attribute table model
	 */
	private class AttributeTableModel extends AbstractTableModel {
		
		protected final String[] COLUMNS = {"Attribute", "Value"};

		/**
		 * Return the number of columns
		 *
		 * @return the number of columns
		 */
		public int getColumnCount() {
			return COLUMNS.length;
		}

		/**
		 * Return the number of rows
		 *
		 * @return the number of rows
		 */
		public int getRowCount() {
			return attributes.size();
		}

		/**
		 * Return the column name
		 *
		 * @param col the column id
		 * @return the column name
		 */
		public String getColumnName(int col) {
			return COLUMNS[col];
		}

		/**
		 * Return the value at the given row and column
		 *
		 * @param row the row id
		 * @param col the column id
		 * @return the object
		 */
		public Object getValueAt(int row, int col) {
			
			Pair<String, String> a = attributes.get(row);
			
			return col == 0 ? a.getFirst() : a.getSecond();
		}

		/**
		 * Return the class of the given column
		 *
		 * @param col the column id
		 * @return the column class
		 */
		public Class<?> getColumnClass(int col) {
			return String.class;
		}

		/**
		 * Determine whether the given cell is editable
		 *
		 * @param row the row id
		 * @param col the column id
		 * @return true if it is editable
		 */
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		
		/**
		 * Change the value in the given cell
		 *
		 * @param value the new value
		 * @param row the row id
		 * @param col the column id
		 */
		public void setValueAt(Object value, int row, int col) {
			fireTableCellUpdated(row, col);
		}
	}
}
