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
import edu.harvard.util.filter.Filter;
import edu.harvard.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 * The table of attributes
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class AttributeTable extends JTable {
	
	protected static final String[] COLUMNS = {"Attribute", "Op", "Value"};
	protected ArrayList<WithAttribute> attributeObjects;
	protected boolean withOp;

	private Listener listener;
	protected AttributeTableModel model;


	/**
	 * Create an instance of class AttributeTable
	 *
	 * @param withOp whether to include the operation column
	 */
	public AttributeTable(boolean withOp) {

		this.withOp = withOp;

		attributeObjects = new ArrayList<WithAttribute>();
		listener = new Listener();
		
		model = new AttributeTableModel();
		setModel(model);
		
		
		// Set the cell editors and renderers

		setDefaultRenderer(WithAttribute.class, new AttributeRenderer());
		setDefaultEditor(WithAttribute.class, new AttributeEditor());
		if (withOp) getColumnModel().getColumn(1).setCellEditor(new OpEditor());
		
		
		// Set the column widths and other common properties

		setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		getTableHeader().setReorderingAllowed(false);
		
		getColumnModel().getColumn(0).setPreferredWidth(64);
		if (withOp) getColumnModel().getColumn(1).setPreferredWidth(20);
		
		
		// Set the handlers
		
		addKeyListener(new MyKeyAdapter());
	}


	/**
	 * Create an instance of class AttributeTable
	 */
	public AttributeTable() {
		this(false);
	}


	/**
	 * Add an attribute object
	 *
	 * @param object an attribute or an object using a single attribute
	 */
	public void add(WithAttribute object) {
		if (isEditing()) getCellEditor().stopCellEditing();
		attributeObjects.add(object);
		object.getAttribute().addAttributeListener(listener);
		model.fireTableRowsInserted(attributeObjects.size() - 1, attributeObjects.size() - 1);
		model.fireTableDataChanged();
	}


	/**
	 * Remove an attribute object
	 *
	 * @param object an attribute or an object using a single attribute
	 */
	public void remove(WithAttribute object) {
		if (isEditing()) getCellEditor().stopCellEditing();
		int i = attributeObjects.indexOf(object);
		if (i >= 0) {
			attributeObjects.remove(i);
			object.getAttribute().removeAttributeListener(listener);
			model.fireTableRowsDeleted(i, i);
			model.fireTableDataChanged();
			repaint();
		}
	}


	/**
	 * Get an attribute object given the row index
	 *
	 * @param row the given row index
	 * @return the object at the given row
	 */
	public WithAttribute get(int row) {
		return attributeObjects.get(row);
	}
	
	
	/**
	 * Return the number of attributes (rows) in the table
	 * 
	 * @return the number of rows
	 */
	public int getAttributeCount() {
		return model.getRowCount();
	}


	/**
	 * Attribute table model
	 */
	private class AttributeTableModel extends AbstractTableModel {

		/**
		 * Return the number of columns
		 *
		 * @return the number of columns
		 */
		public int getColumnCount() {
			return withOp ? 3 : 2;
		}

		/**
		 * Return the number of rows
		 *
		 * @return the number of rows
		 */
		public int getRowCount() {
			return attributeObjects.size();
		}

		/**
		 * Return the column name
		 *
		 * @param col the column id
		 * @return the column name
		 */
		public String getColumnName(int col) {
			if (col == 0) return COLUMNS[0];
			if (col == getColumnCount() - 1) return COLUMNS[2];
			return COLUMNS[1];
		}

		/**
		 * Return the value at the given row and column
		 *
		 * @param row the row id
		 * @param col the column id
		 * @return the object
		 */
		public Object getValueAt(int row, int col) {
			
			WithAttribute w = attributeObjects.get(row);
			AbstractAttribute a = w.getAttribute();
			
			if (col == 0) {
				if (w instanceof Filter<?>) return Utils.<Filter<?>>cast(w).getName();
				return a.getName();
			}
			
			if (col == getColumnCount() - 1) return w;
			
			return a.getOperator();
		}

		/**
		 * Return the class of the given column
		 *
		 * @param col the column id
		 * @return the column class
		 */
		public Class<?> getColumnClass(int col) {
			return col == getColumnCount() - 1 ? WithAttribute.class : String.class;
		}

		/**
		 * Determine whether the given cell is editable
		 *
		 * @param row the row id
		 * @param col the column id
		 * @return true if it is editable
		 */
		public boolean isCellEditable(int row, int col) {
			return col > 0;
		}

		
		/**
		 * Change the value in the given cell
		 *
		 * @param value the new value
		 * @param row the row id
		 * @param col the column id
		 */
		public void setValueAt(Object value, int row, int col) {

			// Fire the callbacks
			
			fireTableCellUpdated(row, col);
		}
	}


	/**
	 * Cell renderer
	 */
	private class AttributeRenderer implements TableCellRenderer {
		
		private JLabel label;
		private JSlider slider;
		

		/**
		 * Create an instance of the cell renderer
		 */
		public AttributeRenderer() {
			
			// Label
			
			label = new JLabel("");
			label.setOpaque(true);
			
			
			// Slider
			
			slider = new JSlider();
			slider.setBorder(null);
			slider.setOpaque(true);
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
			WithAttribute edited = (WithAttribute) object;
			AbstractAttribute attribute = edited.getAttribute();

			if (isSelected) {
				label.setForeground(table.getSelectionForeground());
				label.setBackground(table.getSelectionBackground());
				slider.setBackground(table.getSelectionBackground());
			}
			else {
				label.setForeground(table.getForeground());
				label.setBackground(table.getBackground());
				slider.setBackground(table.getBackground());
			}
			
			
			// Complex attribute
			
			if (attribute.valueClass() == Collection.class) {
				label.setText("[" + attribute + "]");
				return label;
			}


			// Integer attribute

			if (attribute.valueClass() == Integer.class) {
				if (!attribute.isPrecise() && attribute.hasMinimum() && attribute.hasMaximum()) {
					
					Attribute<Integer> A = Utils.<Attribute<Integer>>cast(attribute);
					int min = A.getMinimum();
					int max = A.getMaximum();
					
					slider.setMinimum(min);
					slider.setMaximum(max);
					slider.setValue(A.get());

					return slider;
				}
				else {
					label.setText("" + attribute);
					return label;
				}
			}


			// Double attribute

			if (attribute.valueClass() == Double.class) {
				if (!attribute.isPrecise() && attribute.hasMinimum() && attribute.hasMaximum()) {
					
					Attribute<Double> A = Utils.<Attribute<Double>>cast(attribute);
					double min = A.getMinimum();
					double max = A.getMaximum();
					
					slider.setMinimum(0);
					slider.setMaximum(100);
					slider.setValue((int) Math.round((100 * (A.get() - min)) / (max - min)));

					return slider;
				}
				else {
					label.setText("" + attribute);
					return label;
				}
			}


			// String attribute

			if (attribute.valueClass() == String.class) {
				label.setText("" + attribute);
				return label;
			}
			
			
			// Default
			
			label.setText("<< " + attribute + " >>");
			return label;
		}
	}
	
	
	/**
	 * Cell editor
	 */
	private class AttributeEditor extends AbstractCellEditor implements TableCellEditor, ActionListener, ChangeListener {
		
		private JComponent editor;
		private JTextField field;
		private JSlider slider;
		
		private WithAttribute edited;
		private AbstractAttribute attribute;
		private AttributeRenderer renderer;


		/**
		 * Create an instance of the cell editor
		 */
		public AttributeEditor() {

			renderer = new AttributeRenderer();
			editor = null;
			
			
			// Text field
			
			field = new JTextField("");
			field.setBorder(null);
			
			
			// Slider
			
			slider = new JSlider();
			slider.setBorder(null);
			slider.addChangeListener(this);
			slider.setOpaque(true);
			new SliderToolTipHandler();
		}


		/**
		 * Callback for when the user performs an action
		 *
		 * @param e the description of the event
		 */
		public void actionPerformed(ActionEvent e) {
		}
		
		
		/**
		 * Callback for when the slider changed
		 * 
		 * @param e the description of the event
		 */
		public void stateChanged(ChangeEvent e) {
			getCellEditorValue();
		}


		/**
		 * Update the attribute value
		 *
		 * @return the edited object
		 */
		public Object getCellEditorValue() {
			
			if (attribute == null) return edited;

			try {
				
				if (editor == field) {
	
					// Integer attribute
	
					if (attribute.valueClass() == Integer.class) {
						Utils.<Attribute<Integer>>cast(attribute).set(Integer.parseInt(field.getText()));
						return edited;
					}
	
	
					// Double attribute
	
					if (attribute.valueClass() == Double.class) {
						Utils.<Attribute<Double>>cast(attribute).set(Double.parseDouble(field.getText()));
						return edited;
					}
	
	
					// String attribute
	
					if (attribute.valueClass() == String.class) {
						Utils.<Attribute<String>>cast(attribute).set(field.getText());
						return edited;
					}
				}
				else if (editor == slider) {
					
					// Integer attribute
	
					if (attribute.valueClass() == Integer.class) {
						Utils.<Attribute<Integer>>cast(attribute).setWithSilentFail(slider.getValue());
						return edited;
					}
	
					
					// Double attribute
	
					if (attribute.valueClass() == Double.class) {
						
						Attribute<Double> A = Utils.<Attribute<Double>>cast(attribute);
						double min = A.getMinimum();
						double max = A.getMaximum();
						
						A.setWithSilentFail(min + ((double) slider.getValue() / 500.0) * (max - min));
						
						return edited;
					}
				}

				// Fallback - ignore for now
			}
			catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(null, "This is not a number",
					"Editing \"" + attribute.getName() + "\"", JOptionPane.ERROR_MESSAGE);
			}
			catch (Exception e) {
				if (e instanceof NullPointerException) {
					e.printStackTrace(System.err);
					JOptionPane.showMessageDialog(null, "Internal error: NullPointerException",
							"Editing \"" + attribute.getName() + "\"", JOptionPane.ERROR_MESSAGE);
				}
				else {
					JOptionPane.showMessageDialog(null, e.getMessage(),
							"Editing \"" + attribute.getName() + "\"", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			return edited;
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
			edited = (WithAttribute) object;
			attribute = edited.getAttribute();
			
			slider.setBackground(table.getSelectionBackground());
			editor = field;


			// Integer attribute

			if (attribute.valueClass() == Integer.class) {
				if (!attribute.isPrecise() && attribute.hasMinimum() && attribute.hasMaximum()) {
					
					Attribute<Integer> A = Utils.<Attribute<Integer>>cast(attribute);
					int min = A.getMinimum();
					int max = A.getMaximum();
					
					editor = slider;
					slider.setMinimum(min);
					slider.setMaximum(max);
					slider.setValue(A.get());

					return slider;
				}
				else {
					field.setText("" + attribute);
					return field;
				}
			}


			// Double attribute

			if (attribute.valueClass() == Double.class) {
				if (!attribute.isPrecise() && attribute.hasMinimum() && attribute.hasMaximum()) {
					
					Attribute<Double> A = Utils.<Attribute<Double>>cast(attribute);
					double min = A.getMinimum();
					double max = A.getMaximum();
					
					editor = slider;
					slider.setMinimum(0);
					slider.setMaximum(500);
					slider.setValue((int) Math.round((500 * (A.get() - min)) / (max - min)));

					return slider;
				}
				else {
					field.setText("" + attribute);
					return field;
				}
			}


			// String attribute

			if (attribute.valueClass() == String.class) {
				field.setText("" + attribute);
				return field;
			}


			// Default

			return renderer.getTableCellRendererComponent(table, object, isSelected, true, row, column);
		}
		
		
		/**
		 * The handler for displaying the current value of the slider as it is changing
		 * 
		 * From: http://forums.oracle.com/forums/thread.jspa?threadID=1756101&tstart=45 
		 */
		private class SliderToolTipHandler implements MouseMotionListener, MouseListener
		{
			final JPopupMenu pop = new JPopupMenu();
			JMenuItem item = new JMenuItem();

			public SliderToolTipHandler() {
				slider.addMouseMotionListener(this);
				slider.addMouseListener(this);
				pop.add(item );
				pop.setDoubleBuffered(true);
			}

			public void showToolTip(MouseEvent me) {
				
				// Get the string version of the attribute
				String s;
				if (edited instanceof Filter<?>) {
					s = Utils.<Filter<?>>cast(edited).getAttributeString();
				}
				else {
					s = edited.toString();
				}
				item.setText(s);

				// Limit the tool-tip location relative to the slider
				Rectangle b = me.getComponent().getBounds();
				int x = me.getX();      
				x = (x > (b.x + b.width / 2) ? (b.x + b.width / 2) : (x < (b.x - b.width / 2) ? (b.x - b.width / 2) : x));

				// Show the pop-up
				pop.pack();
				pop.show(me.getComponent(), x - 5, -30);
				item.setArmed(false);
			}

			public void mouseDragged(MouseEvent me) {
				showToolTip(me);
			}

			public void mouseMoved(MouseEvent me) {
			}

			public void mousePressed(MouseEvent me) {
				showToolTip(me);
			}

			public void mouseClicked(MouseEvent me) {
			}

			public void mouseReleased(MouseEvent me) {
				pop.setVisible(false);
			}

			public void mouseEntered(MouseEvent me) {
			}

			public void mouseExited(MouseEvent me) {
			}
		}
	}


	/**
	 * Cell editor for operators
	 */
	private class OpEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

		private JComboBox combo;
		private String original;
		private AbstractAttribute attribute;


		/**
		 * Create an instance of the cell editor
		 */
		public OpEditor() {
			combo = new JComboBox();
			combo.setBorder(null);
			combo.addActionListener(this);
		}


		/**
		 * Callback for when the user performs an action
		 *
		 * @param e the description of the event
		 */
		public void actionPerformed(ActionEvent e) {
			String v = (String) combo.getSelectedItem();
			if (v != null) getCellEditorValue();
		}


		/**
		 * Update the attribute value
		 *
		 * @return the edited object
		 */
		public Object getCellEditorValue() {

			try {
				attribute.setOperator((String) combo.getSelectedItem());
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null, e.getMessage(),
					"Editing \"" + attribute.getName() + "\"", JOptionPane.ERROR_MESSAGE);
				return original;
			}

			return combo.getSelectedItem();
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
			
			attribute = get(row).getAttribute();
			original = attribute.getOperator();

			combo.removeAllItems();
			for (String s : attribute.getOperators()) combo.addItem(s);

			combo.setSelectedItem(original);
			return combo;
		}
	}


	/**
	 * A listener for changes in the attributes of the filter
	 */
	private class Listener implements AttributeListener {

		/**
		 * Callback for when the attribute value changed
		 *
		 * @param attr the attribute
		 */
		public void attributeValueChanged(AbstractAttribute attr) {
		}

		/**
		 * Callback for when the attribute constraints changed
		 *
		 * @param attr the attribute
		 */
		public void attributeConstraintsChanged(AbstractAttribute attr) {
			if (isEditing()) getCellEditor().stopCellEditing();
			model.fireTableDataChanged();
			repaint();
		}
	}
	
	
	/**
	 * Key adapter
	 */
	private class MyKeyAdapter extends KeyAdapter {
		
		/**
		 * Callback for when the key is pressed
		 * 
		 * @param event the keyboard event
		 */
		public void keyPressed(KeyEvent event) {
			
			if (event.getKeyCode() == KeyEvent.VK_ENTER) {
				//event.consume();
			}
		}
	}
}
