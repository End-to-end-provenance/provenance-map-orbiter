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

package edu.harvard.util.gui;

import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.*;
import java.io.*;
import java.util.*;

import javax.swing.*;


/**
 * A reorderable JList. The contents can be rearranged using drag & drop.
 * 
 * @author CodeIdol.com
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class ReorderableJList extends JList {
	
	// The code is based on:
	//   http://codeidol.com/java/swing/Lists-and-Combos/Reorder-a-JList-with-Drag-and-Drop/

	private static DataFlavor localObjectFlavor;
	static {
		try {
			localObjectFlavor = new DataFlavor(
					DataFlavor.javaJVMLocalObjectMimeType);
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
	}
	
	private static DataFlavor[] supportedFlavors = { localObjectFlavor };
	private DragSource dragSource;
	private DropTarget dropTarget;
	private Object dropTargetCell;
	private boolean dropTargetAbove;
	private boolean dragSourceSelected;
	private int draggedIndex = -1;
	private ReorderableListCellRenderer renderer;
	private EventHandler handler;

	
	/**
	 * Constructs a ReorderableJList with an empty, read-only, model.
	 */
	public ReorderableJList() {
		super();
		initialize();
	}

	
	/**
	 * Constructs a JList that displays elements from the specified, non-null, model.
	 * 
	 * @param dataModel the data model based on the DefaultListModel
	 */
	public ReorderableJList(DefaultListModel dataModel)  {
		super(dataModel);
		initialize();
	}

	
	/**
	 * Constructs a JList that displays the elements in the specified array.
	 * 
	 * @param listData the initial list data
	 */
	public ReorderableJList(Object[] listData)  {
		super(listData);
		initialize();
	}

	
	/**
	 * Constructs a JList that displays the elements in the specified Vector.
	 * 
	 * @param listData the initial list data
	 */
	public ReorderableJList(Vector<?> listData) {
		super(listData);
		initialize();
	}
	
	
	/**
	 * Initialize the object
	 */
	private void initialize() {
		renderer = new ReorderableListCellRenderer();
		super.setCellRenderer(renderer);
		dragSource = new DragSource();
		handler = new EventHandler();
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, handler);
		dropTarget = new DropTarget(this, handler);
	}
	
	
	/**
	 * Sets the delegate that is used to paint each cell in the list.
	 * 
	 * @param renderer the ListCellRenderer that paints list cells
	 */
	@Override
	public void setCellRenderer(ListCellRenderer renderer) {
		if (this.renderer == null || this.renderer.embedded == null) {
			super.setCellRenderer(renderer);
		}
		else {
			this.renderer.embedded = renderer;
		}
	}
	
	
	/**
	 * Returns the object responsible for painting list items. 
	 * 
	 * @return the value of the cellRenderer property
	 */
	@Override
	public ListCellRenderer getCellRenderer() {
		return this.renderer == null || this.renderer.embedded == null ? super.getCellRenderer() : this.renderer.embedded;
	}
	

	/**
	 * Event handler
	 */
	private class EventHandler implements DragSourceListener, DropTargetListener, DragGestureListener {
	
		// DragGestureListener
		public void dragGestureRecognized(DragGestureEvent dge) {
			// find object at this x,y
			Point clickPoint = dge.getDragOrigin();
			int index = locationToIndex(clickPoint);
			if (index == -1)
				return;
			Object target = getModel().getElementAt(index);
			Transferable trans = new RJLTransferable(target);
			draggedIndex = index;
			dragSource.startDrag(dge, Cursor.getDefaultCursor(), trans, this);
			dragSourceSelected = isSelectedIndex(index);
		}
	
		// DragSourceListener events
		public void dragDropEnd(DragSourceDropEvent dsde) {
			dropTargetCell = null;
			dropTargetAbove = true;
			draggedIndex = -1;
			repaint();
		}
	
		public void dragEnter(DragSourceDragEvent dsde) {
		}
	
		public void dragExit(DragSourceEvent dse) {
		}
	
		public void dragOver(DragSourceDragEvent dsde) {
		}
	
		public void dropActionChanged(DragSourceDragEvent dsde) {
		}
	
		// DropTargetListener events
		public void dragEnter(DropTargetDragEvent dtde) {
			if (dtde.getSource() != dropTarget)
				dtde.rejectDrag();
			else {
				dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
			}
		}
	
		public void dragExit(DropTargetEvent dte) {
		}
	
		public void dragOver(DropTargetDragEvent dtde) {
			// figure out which cell it's over, no drag to self
			if (dtde.getSource() != dropTarget)
				dtde.rejectDrag();
			Point dragPoint = dtde.getLocation();
			int index = locationToIndex(dragPoint);
			Rectangle r = getCellBounds(index, index);
			if (!r.contains(dragPoint)) index = getModel().getSize();
			if (dragPoint.y > r.y + r.height / 2) index++;
			if (index == -1) {
				dropTargetCell = null;
				dropTargetAbove = true;
			}
			else {
				dropTargetAbove = index < getModel().getSize();
				dropTargetCell = getModel().getElementAt(dropTargetAbove ? index : getModel().getSize() - 1);
			}
			repaint();
		}
	
		public void drop(DropTargetDropEvent dtde) {
			if (dtde.getSource() != dropTarget) {
				dtde.rejectDrop();
				return;
			}
			Point dropPoint = dtde.getLocation();
			int index = locationToIndex(dropPoint);
			Rectangle r = getCellBounds(index, index);
			if (!r.contains(dropPoint)) index = getModel().getSize();
			if (dropPoint.y > r.y + r.height / 2) index++;
			boolean dropped = false;
			try {
				if ((index == -1) || (index == draggedIndex)) {
					dtde.rejectDrop();
					return;
				}
				DefaultListModel mod = (DefaultListModel) getModel();
				if (index > mod.getSize()) index = mod.getSize();
				dtde.acceptDrop(DnDConstants.ACTION_MOVE);
				Object dragged = dtde.getTransferable().getTransferData(
						localObjectFlavor);
				// move items - note that indicies for insert will
				// change if [removed] source was before target
				boolean sourceBeforeTarget = (draggedIndex < index);
				mod.remove(draggedIndex);
				mod.add((sourceBeforeTarget ? index - 1 : index), dragged);
				dropped = true;
				if (dragSourceSelected) {
					setSelectedIndex(locationToIndex(dropPoint));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			dtde.dropComplete(dropped);
		}
	
		public void dropActionChanged(DropTargetDragEvent dtde) {
		}
	}
	
	
	/**
	 * The transferable object
	 */
	private class RJLTransferable implements Transferable {
		Object object;

		public RJLTransferable(Object o) {
			object = o;
		}

		public Object getTransferData(DataFlavor df)
				throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(df))
				return object;
			else
				throw new UnsupportedFlavorException(df);
		}

		public boolean isDataFlavorSupported(DataFlavor df) {
			return (df.equals(localObjectFlavor));
		}

		public DataFlavor[] getTransferDataFlavors() {
			return supportedFlavors;
		}
	}

	
	/**
	 * Cell renderer that paints the location of where the cell will be dropped
	 */
	private class ReorderableListCellRenderer extends DefaultListCellRenderer {
		boolean isTargetCell;
		ListCellRenderer embedded;

		public ReorderableListCellRenderer() {
			super();
			embedded = null;
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean hasFocus) {
			isTargetCell = (value == dropTargetCell);
			//isLastItem = (index == list.getModel().getSize() - 1);
			boolean showSelected = isSelected & (dropTargetCell == null);
			if (embedded == null) {
				return super.getListCellRendererComponent(list, value, index,
						showSelected, hasFocus);
			}
			else {
				return embedded.getListCellRendererComponent(list, value, index,
						showSelected, hasFocus);
			}
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (isTargetCell) {
				g.setColor(Color.black);
				if (dropTargetAbove) {
					g.drawLine(0, 0, getSize().width, 0);
				}
				else {
					g.drawLine(0, getSize().height - 1, getSize().width, getSize().height - 1);
				}
			}
		}
	}
}
