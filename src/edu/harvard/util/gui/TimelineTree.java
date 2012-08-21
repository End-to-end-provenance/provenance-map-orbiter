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
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.*;


/**
 * The tree of event durations
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class TimelineTree<T> extends JList {
	
	protected static final int EXPAND_BUTTON_LENGTH = 8;
	public static final int RIGHT_AREA_WIDTH = 200;
	
	protected int rowHeight;
	protected boolean showRoot;
	protected TimelineDecorator<T> decorator;
	
	protected EventTreeListModel model;
	protected EventRenderer renderer;
	protected Listener listener;
	protected double start;
	protected double finish;


	/**
	 * Create an instance of class TimelineTree
	 */
	public TimelineTree() {
		
		model = new EventTreeListModel();
		renderer = new EventRenderer();
		listener = new Listener();
		
		rowHeight = getFont().getSize() + 4;
		showRoot = false;
		decorator = new DefaultTimelineDecorator<T>();
		
		setModel(model);
		setCellRenderer(renderer);
		setFixedCellHeight(rowHeight);
		addMouseListener(listener);
		addComponentListener(listener);
		
		setLayoutOrientation(VERTICAL);
		setVisibleRowCount(-1);
		
		Dimension minimumSize = getMinimumSize();
		minimumSize.setSize(RIGHT_AREA_WIDTH + 100, minimumSize.getHeight());
		setMinimumSize(minimumSize);
		
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
	 * Set a root event
	 *
	 * @param event the event
	 */
	public void setRoot(TimelineEvent<T> event) {
		model.setRoot(event);
	}


	/**
	 * Clear the tree
	 */
	public void clear() {
		setRoot(null);
	}
	
	
	/**
	 * Set the timeline decorator
	 * 
	 * @param decorator the new decorator
	 */
	public void setDecorator(TimelineDecorator<T> decorator) {
		this.decorator = decorator;
	}
	
	
	/**
	 * Get the selected event
	 * 
	 * @return the selected event, or null if none
	 */
	public TimelineEvent<T> getSelectedEvent() {
		if (isSelectionEmpty()) return null;
		Subtree t = Utils.<Subtree>cast(getSelectedValue());
		return t.root;
	}
	
	
	/**
	 * A subtree
	 */
	private class Subtree {
		
		private TimelineEvent<T> root;
		private ArrayList<Subtree> children;
		private Subtree parent;
		private boolean expanded;
		private int x;
		
		
		/**
		 * Create an instance of class Subtree
		 * 
		 * @param root the root of the subtree
		 */
		public Subtree(TimelineEvent<T> root) {
			this(root, null);
		}
		
		
		/**
		 * Create an instance of class Subtree
		 * 
		 * @param root the root of the subtree
		 * @param parent the parent subtree
		 */
		protected Subtree(TimelineEvent<T> root, Subtree parent) {
			this.root = root;
			this.expanded = false;
			this.parent = parent;
			this.x = 0;
			
			if (root.getSubEventCount() > 0) {
				children = new ArrayList<Subtree>(root.getSubEventCount());
				for (TimelineEvent<T> e : root.getSubEvents()) {
					children.add(new Subtree(e, this));
				}
			}
			else {
				children = null;
			}
		}
		
		
		/**
		 * Return the root
		 * 
		 * @return the graph root
		 */
		public TimelineEvent<T> getRoot() {
			return root;
		}
		
		
		/**
		 * Return the children
		 * 
		 * @return the list of children, or null if none
		 */
		public ArrayList<Subtree> getChildren() {
			return children;
		}
		
		
		/**
		 * Return the parent
		 * 
		 * @return the parent of the root node of this subtree
		 */
		public Subtree getParent() {
			return parent;
		}
		
		
		/**
		 * Determine whether the top level of the subtree is expanded
		 * 
		 * @return true if it is expanded
		 */
		public boolean isExpanded() {
			return expanded;
		}
		
		
		/**
		 * Expand or collapse the top level of the subtree
		 * 
		 * @param expand true to expand, false to collapse
		 */
		public void setExpanded(boolean expand) {
			expanded = expand;
		}
		
		
		/**
		 * Return the number of visible elements in the subtree
		 * 
		 * @return the number of visible elements
		 */
		public int getNumberOfVisibleElements() {
			int count = 1;
			if (children != null && expanded) {
				for (Subtree t : children) {
					count += t.getNumberOfVisibleElements();
				}
			}
			return count;
		}
		
		
		/**
		 * Return the object at the given visible position
		 * 
		 * @param index the index
		 * @return the subtree at the given position, or null if the index is out of bounds 
		 */
		public Subtree getVisibleElementAt(int index) {
			if (index == 0) return this;
			if (index  < 0) return null;
			
			if (children != null && expanded) {
				for (Subtree t : children) {
					int count = t.getNumberOfVisibleElements();
					if (count >= index) {
						return t.getVisibleElementAt(index - 1);
					}
					else {
						index -= count;
					}
				}
			}
			
			return null;
		}
		
		
		/**
		 * Add the visible elements of the subtree to a collection
		 * 
		 * @param collection the collection
		 * @return the number of added elements
		 */
		public int listVisibleElements(Collection<Subtree> collection) {
			int count = 1;
			collection.add(this);
			if (children != null && expanded) {
				for (Subtree t : children) {
					count += t.listVisibleElements(collection);
				}
			}
			return count;
		}
		
		
		/**
		 * Print the subtree to the standard output
		 * 
		 * @param indent the indentation level
		 */
		public void print(int indent) {
			for (int i = 0; i < indent; i++) System.out.print(" ");
			System.out.println("" + getRoot());
			if (children != null && expanded) {
				for (Subtree t : children) {
					t.print(indent + 2);
				}
			}
		}
		
		
		/**
		 * Set the X coordinate of the subtree
		 * 
		 * @param x the new X coordinate
		 */
		public void setX(int x) {
			this.x = x;
		}
		
		
		/**
		 * Return the X coordinate of the subtree
		 * 
		 * @return the X coordinate
		 */
		public int getX() {
			return x;
		}
		
		
		/**
		 * Determine whether the subtree has any children
		 * 
		 * @return true if it has children
		 */
		public boolean hasChildren() {
			return children == null ? false : !children.isEmpty();
		}
	}
	

	/**
	 * Attribute table model
	 */
	private class EventTreeListModel extends AbstractListModel {
		
		private Subtree subtree;
		private java.util.List<Subtree> rows;
		

		/**
		 * Create an instance of class EventTreeListModel
		 */
		public EventTreeListModel() {
			subtree = null;
			rows = new ArrayList<Subtree>();
		}
		
		
		/**
		 * Set the root
		 * 
		 * @param root the new root
		 */
		public void setRoot(TimelineEvent<T> root) {
			
			// Replace the subtree
			
			int oldSize = getSize(); 
			rows.clear();
			System.gc();
			this.subtree = root == null ? null : new Subtree(root);
			if (oldSize >= 1) fireIntervalRemoved(this, 0, oldSize - 1);
			if (root == null) return;
			
			
			// Initialize the thing
			
			subtree.setExpanded(true);
			reload();
			
			
			// Fire the appropriate event
			
			int size = getSize();
			fireIntervalAdded(this, 0, size > 0 ? size - 1 : 0);
		}
		
		
		/**
		 * Return the root
		 * 
		 * @return the graph root
		 */
		public Subtree getTree() {
			return subtree;
		}
		

		/**
		 * Return the element at the given row
		 *
		 * @param row the row number
		 * @return the element 
		 */
		@Override
		public Object getElementAt(int index) {
			return rows.get(index);
		}
		

		/**
		 * Return the subtree at the given row
		 *
		 * @param row the row number
		 * @return the subtree 
		 */
		public Subtree getSubtreeAt(int index) {
			return rows.get(index);
		}

		
		/**
		 * Return the number of currently visible elements
		 *
		 * @return the number of elements
		 */
		@Override
		public int getSize() {
			if (subtree == null) return 0;
			return rows.size();
		}
		
		
		/**
		 * Expand or collapse a subtree
		 * 
		 * @param index the row number of the subtree
		 * @param expand true to expand, false to collapse
		 */
		public void setExpanded(int index, boolean expand) {
			
			Subtree t = rows.get(index);
			int oldSize = getSize();
			if (t == null) return;
			if (t.isExpanded() == expand) return;
			
			if (expand) {
				t.setExpanded(true);
				reload();
				int n = getSize() - oldSize;
				if (n > 0) {
					fireIntervalAdded(this, index + 1, index + n);
				}
			}
			else {
				t.setExpanded(false);
				reload();
				int n = oldSize - getSize();
				if (n > 0) {
					fireIntervalRemoved(this, index + 1, index + n);
				}
			}
			
			repaint();
		}
		
		
		/**
		 * Reload the rows in the list
		 */
		protected void reload() {
			rows.clear();
			if (showRoot) {
				subtree.listVisibleElements(rows);
			}
			else {
				java.util.List<Subtree> l = subtree.getChildren();
				if (l != null) {
					for (Subtree t : l) t.listVisibleElements(rows);
				}
			}
			recomputeLayout();
		}
		
		
		/**
		 * Recompute the graph layout
		 */
		public void recomputeLayout() {
			for (Subtree t : rows) renderer.getContentX(t);
		}
	}


	/**
	 * Cell renderer
	 */
	private class EventRenderer extends JPanel implements ListCellRenderer {
		
		private Subtree subtree;
		private TimelineEvent<T> event;
		private boolean selected;
		private Color contentBackground;
		

		/**
		 * Create an instance of the cell renderer
		 */
		public EventRenderer() {
			subtree = null;
			event = null;
			contentBackground = getBackground();
		}
		
		
		/**
		 * Initialize a cell renderer
		 *
		 * @param list the list
		 * @param object the edited object
		 * @param index the row index
		 * @param isSelected whether the current row is selected
		 * @param hasFocus whether the cell has focus
		 * @return the cell renderer
		 */
		public Component getListCellRendererComponent(JList list, Object object, int index,
													   boolean isSelected, boolean hasFocus) {
			subtree = Utils.<Subtree>cast(object);
			event = subtree == null ? null : subtree.getRoot();
			selected = isSelected;

			if (isSelected) {
				setForeground(list.getSelectionForeground());
				setBackground(list.getSelectionBackground());
			}
			else {
				setForeground(list.getForeground());
				setBackground(list.getBackground());
			}

			return this;
		}
		
		
		/**
		 * Determine the X coordinate of the row contents and cache the result
		 * 
		 * @param t the subtree
		 * @return the X coordinate
		 */
		public int getContentX(Subtree t) {
			
			int width = TimelineTree.this.getWidth();
			double f = (width - RIGHT_AREA_WIDTH) / (finish - start);
			if (Math.abs(finish - start) < 0.0000001) f = 1;
			int x1 = (int) ((t.getRoot().getStart() - start) * f);
			Subtree parent = t.getParent();

			int x = x1;
			if (parent != null) {
				if (showRoot || parent.getParent() != null) {
					int px = parent.getX() + EXPAND_BUTTON_LENGTH;
					if (x < px) x = px;
				}
			}
			
			t.setX(x);
			return x;
		}
		
		
		/**
		 * Paint the component
		 * 
		 * @param g the graphics context
		 */
		public void paint(Graphics g) {
			
			int width = getWidth();
			int height = getHeight();
			Color bgColor = getBackground();
			Color timeSpanColor = decorator.getColor(event);
			
			g.setColor(getBackground());
			g.fillRect(0, 0, width - RIGHT_AREA_WIDTH, height);
			
			if (selected) {
				g.setColor(new Color((contentBackground.getRed  () + 2 * bgColor.getRed  ()) / 3,
						 			 (contentBackground.getGreen() + 2 * bgColor.getGreen()) / 3,
						 			 (contentBackground.getBlue () + 2 * bgColor.getBlue ()) / 3));
			}
			else {
				g.setColor(contentBackground);
			}
			g.fillRect(width - RIGHT_AREA_WIDTH, 0, RIGHT_AREA_WIDTH, height);
			
			
			// Handle errors
			
			if (event == null) {
				g.setColor(Color.RED);
				g.drawString("<null>", 4, height);
				return;
			}
			
			
			// Draw the event duration/time-span
			
			double f = (width - RIGHT_AREA_WIDTH) / (finish - start);
			int x1 = (int) ((event.getStart()  - start) * f);
			int x2 = (int) ((event.getFinish() - start) * f);
			int w = x2 - x1;
			
			if (selected) {
				g.setColor(new Color((timeSpanColor.getRed  () + 2 * bgColor.getRed  ()) / 3,
						 			 (timeSpanColor.getGreen() + 2 * bgColor.getGreen()) / 3,
						 			 (timeSpanColor.getBlue () + 2 * bgColor.getBlue ()) / 3));
			}
			else {
				g.setColor(timeSpanColor);
			}
			g.fillRect(x1, 0, w <= 0 ? 1 : w, height);
			
			
			// Initialize drawing of the contents
			
			int x = subtree.getX();
			int ty = height - 4;
			
			Subtree parent = subtree.getParent();
			
			g.setColor(getForeground());
			
			
			// Draw the line connecting it to its parent
			
			if (parent != null) {
				Subtree prev = parent;
				int d = height / 2;
				for (Subtree p = parent.getParent(); p != null; p = p.getParent()) {
					if (!showRoot && p.getParent() == null) break;
					ArrayList<Subtree> sibilings = p.getChildren();
					boolean last = sibilings.get(sibilings.size() - 1) == prev;
					if (!last) g.drawLine(p.getX() + d, 0, p.getX() + d, height);
					prev = p;
				}
				if (showRoot || parent.getParent() != null) {
					ArrayList<Subtree> sibilings = parent.getChildren();
					boolean last = sibilings.get(sibilings.size() - 1) == subtree;
					g.drawLine(parent.getX() + d, 0, parent.getX() + d, last ? height / 2 : height);
					g.drawLine(parent.getX() + d, height / 2,
							   subtree.hasChildren() ? x + EXPAND_BUTTON_LENGTH / 2 - 1 : x + height,
							   height / 2);
				}
			}
			
			if (subtree.hasChildren() && subtree.isExpanded()) {
				int d = height / 2;
				g.drawLine(x + d, (height + EXPAND_BUTTON_LENGTH) / 2, x + d, height);
			}
			
			
			// Draw the expander button
			
			if (subtree.hasChildren()) {
				int ex = x + (height - EXPAND_BUTTON_LENGTH) / 2;
				int ey = (height - EXPAND_BUTTON_LENGTH) / 2;
				g.drawRect(ex, ey, EXPAND_BUTTON_LENGTH, EXPAND_BUTTON_LENGTH);
				g.drawLine(ex + 2, ey + EXPAND_BUTTON_LENGTH / 2,
						   ex + EXPAND_BUTTON_LENGTH - 2, ey + EXPAND_BUTTON_LENGTH / 2);
				if (!subtree.isExpanded()) {
					g.drawLine(ex + EXPAND_BUTTON_LENGTH / 2, ey + 2,
							   ex + EXPAND_BUTTON_LENGTH / 2, ey + EXPAND_BUTTON_LENGTH - 2);
				}
			}
			
				
			// Draw the label
			
			String text = decorator.getText(event);
			String text2 = decorator.getSupplementalText(event);
			
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D r = fm.getStringBounds(text + " ", g);
			Color c = getForeground();
			
			g.setColor(c);
			g.drawString(text, x + height, ty);
			
			g.setColor(new Color((2 * c.getRed  () + 1 * bgColor.getRed  ()) / 3,
								 (2 * c.getGreen() + 1 * bgColor.getGreen()) / 3,
								 (2 * c.getBlue () + 1 * bgColor.getBlue ()) / 3));
			g.drawString(text2, x + height + (int) r.getWidth(), ty);
		}
	}
	
	
	/**
	 * An event handler
	 */
	private class Listener extends MouseAdapter implements ComponentListener {
		
		/**
		 * Create an instance of the event handler
		 */
		public Listener() {
		}
		
		
		/**
		 * Handle mouse clicks
		 *  
		 * @param e the mouse event
		 */
		public void mouseClicked(MouseEvent e) {
			Subtree subtree = model.getTree();
			if (subtree == null) return;
			
			int index = locationToIndex(e.getPoint());
			Rectangle r = getCellBounds(index, index);
			if (!r.contains(e.getPoint())) index = -1;
			
			
			// Outside of the list
			
			if (index < 0) {
				clearSelection();
				return;
			}
			
			
			// Single click
			
			if (e.getClickCount() == 1) {
				ensureIndexIsVisible(index);
				Subtree t = model.getSubtreeAt(index);
				if (t != null) {
					Point p = indexToLocation(index);
					int x = (int) (e.getX() - p.getX());
					int y = (int) (e.getY() - p.getY());
					
					
					// Check the expander button
					
					if (t.hasChildren()) {
						
						int ex = t.getX() + (rowHeight - EXPAND_BUTTON_LENGTH) / 2;
						int ey = (rowHeight - EXPAND_BUTTON_LENGTH) / 2;
						
						if (x >= ex && x <= ex + EXPAND_BUTTON_LENGTH && y >= ey && y <= ey + EXPAND_BUTTON_LENGTH) {
							model.setExpanded(index, !t.isExpanded());
						}
					}
				}
			}
			
			
			// Double-click
			
			if (e.getClickCount() == 2) {
				ensureIndexIsVisible(index);
				Subtree t = model.getSubtreeAt(index);
				if (t != null) model.setExpanded(index, !t.isExpanded());
			}
		}


		/**
		 * Callback for when the component is hidden
		 * 
		 * @param e the description of the event
		 */
		@Override
		public void componentHidden(ComponentEvent e) {
			
		}


		/**
		 * Callback for when the component is moved
		 * 
		 * @param e the description of the event
		 */
		@Override
		public void componentMoved(ComponentEvent e) {
			
		}


		/**
		 * Callback for when the component is resized
		 * 
		 * @param e the description of the event
		 */
		@Override
		public void componentResized(ComponentEvent e) {
			model.recomputeLayout();
			repaint();
		}


		/**
		 * Callback for when the component is shown
		 * 
		 * @param e the description of the event
		 */
		@Override
		public void componentShown(ComponentEvent e) {
			
		}
	}
}
