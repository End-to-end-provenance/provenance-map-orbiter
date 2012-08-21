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

import edu.harvard.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;


/**
 * The table of event durations
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class TimelineTable<T> extends JTable {
	
	protected static final String[] COLUMNS = {"Event", "Duration"};
	protected ArrayList<TimelineEvent<T>> events;

	protected EventTableModel model;
	protected double start;
	protected double finish;


	/**
	 * Create an instance of class TimelineTable
	 */
	public TimelineTable() {

		events = new ArrayList<TimelineEvent<T>>();
		
		model = new EventTableModel();
		setModel(model);

		setDefaultRenderer(TimelineEvent.class, new EventRenderer());

		setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		getTableHeader().setReorderingAllowed(false);
		
		getColumnModel().getColumn(0).setPreferredWidth(128);
		
		start = 0;
		finish = 100;
	}
	
	
	/**
	 * Set the displayed time range
	 * 
	 * @param start the earliest displayed time
	 * @param finish the latest displayed time
	 */
	public void setRange(double start, double finish) {
		this.start = start;
		this.finish = finish;
	}


	/**
	 * Add an event
	 *
	 * @param event the event
	 */
	public void add(TimelineEvent<T> event) {
		if (isEditing()) getCellEditor().stopCellEditing();
		events.add(event);
		model.fireTableRowsInserted(events.size() - 1, events.size() - 1);
		model.fireTableDataChanged();
	}


	/**
	 * Remove an event
	 *
	 * @param event the event to remove
	 */
	public void remove(TimelineEvent<T> event) {
		if (isEditing()) getCellEditor().stopCellEditing();
		int i = events.indexOf(event);
		if (i >= 0) {
			events.remove(i);
			model.fireTableRowsDeleted(i, i);
			model.fireTableDataChanged();
			repaint();
		}
	}
	
	
	/**
	 * Clear all events
	 */
	public void clear() {
		if (isEditing()) getCellEditor().stopCellEditing();
		int n = getEventCount();
		events.clear();
		model.fireTableRowsDeleted(0, n);
		model.fireTableDataChanged();
		repaint();
	}
	
	
	/**
	 * Sort the table
	 * 
	 * @param c the comparator
	 */
	public void sort(Comparator<TimelineEvent<?>> c) {
		Collections.sort(events, c);
		model.fireTableDataChanged();
		repaint();
	}


	/**
	 * Get an event given the row index
	 *
	 * @param row the given row index
	 * @return the event at the given row
	 */
	public TimelineEvent<T> get(int row) {
		return events.get(row);
	}
	
	
	/**
	 * Return the number of events (rows) in the table
	 * 
	 * @return the number of rows
	 */
	public int getEventCount() {
		return model.getRowCount();
	}


	/**
	 * Attribute table model
	 */
	private class EventTableModel extends AbstractTableModel {

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
			return events.size();
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
			TimelineEvent<T> e = events.get(row);
			if (col == 0) return e.getName();
			return e;
		}

		/**
		 * Return the class of the given column
		 *
		 * @param col the column id
		 * @return the column class
		 */
		public Class<?> getColumnClass(int col) {
			return col == getColumnCount() - 1 ? TimelineEvent.class : String.class;
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

			// Fire the callbacks
			
			fireTableCellUpdated(row, col);
		}
	}


	/**
	 * Cell renderer
	 */
	private class EventRenderer extends JPanel implements TableCellRenderer {
		
		private TimelineEvent<T> event;
		

		/**
		 * Create an instance of the cell renderer
		 */
		public EventRenderer() {
			event = null;
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
			event = Utils.<TimelineEvent<T>>cast(object);

			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			}
			else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}

			return this;
		}
		
		
		/**
		 * Paint the component
		 * 
		 * @param g the graphics context
		 */
		public void paint(Graphics g) {
			
			int width = getWidth();
			int height = getHeight();
			
			g.setColor(getBackground());
			g.fillRect(0, 0, width, height);
			
			double f = (width - 4) / (finish - start);
			int x1 = (int) ((event.getStart()  - start) * f);
			int x2 = (int) ((event.getFinish() - start) * f);
			int w = x2 - x1;
			
			g.setColor(Color.GREEN);
			g.fillRect(2 + x1, 0, w <= 0 ? 1 : w, height);
			
			//g.setColor(getForeground());
			//g.drawString("" + event.getStart() + " - " + event.getFinish(), 4, height);
		}
	}
}
