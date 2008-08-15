/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.gui.swt;

import java.util.Vector;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.IFmtGfx;
import kiev.gui.INodeTable;
import kiev.gui.IUIView;
import kiev.gui.TableView;
import kiev.gui.event.EventListenerList;
import kiev.gui.event.InputEventInfo;
import kiev.gui.swing.AWTGraphics2D;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtChildrenIterator;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.ParentAttrSlot;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.SpaceAttrSlot;


public class ANodeTable extends TableViewer implements INodeTable, MouseListener {
	/** The formatter of the current view */
	public final DrawableEntry entry = new DrawableEntry();
	TableView table_view;
	ANodeTableContentProvider tableContentProvider;

	class DrawableEntry {
		final Renderer renderer = new Renderer();
		public void draw(Event event, Drawable value) {
			if (event.gc == null) return;
			GC gc = event.gc;
			if (value instanceof DrawTerm) {
				Object term_obj = ((DrawTerm)value).getTermObj();
				String s;
				if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
					s = "\u25d8"; // ◘
				else if (value == DrawTerm.NULL_NODE)
					s = "\u25c6"; // ◆
				else {
					s = String.valueOf(term_obj);
					if (s == null)
						s = "\u25d8"; // ◘
				}
				gc.drawString(s, event.x, event.y);
			}
		}

		public int getHeight(Event event, Drawable value) {
			String s = "";
			if (value instanceof DrawTerm) {
				Object term_obj = ((DrawTerm)value).getTermObj();
				if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
					s = "\u25d8"; // ◘
				else if (value == DrawTerm.NULL_NODE)
					s = "\u25c6"; // ◆
				else {
					s = String.valueOf(term_obj);
					if (s == null)
						s = "\u25d8"; // ◘
				}
			}
			return event.gc.textExtent(s).y;

		}

		public int getWidth(Event event, Drawable value) {
			String s = "";
			if (value instanceof DrawTerm) {
				Object term_obj = ((DrawTerm)value).getTermObj();
				if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
					s = "\u25d8"; // ◘
				else if (value == DrawTerm.NULL_NODE)
					s = "\u25c6"; // ◆
				else {
					s = String.valueOf(term_obj);
					if (s == null)
						s = "\u25d8"; // ◘
				}
			}
			return event.gc.textExtent(s).x + 4;
		}

	}

	ANodeTable(Composite parent, int style) {
		super(parent, style);
		tableContentProvider = new ANodeTableContentProvider();
		setContentProvider(tableContentProvider); 
		setLabelProvider(new OwnerDrawLabelProvider() {
			protected void measure(Event event, Object element) {
				Drawable dr = (Drawable) element;
				event.setBounds(new Rectangle(event.x, event.y, entry.getWidth(event, dr),
						entry.getHeight(event, dr)));
			}
			protected void paint(Event event, Object element) {
				Drawable dr = (Drawable) element;
				entry.draw(event, dr);
			}
		});
		getControl().addMouseListener(this);
		OwnerDrawLabelProvider.setUpOwnerDraw(this);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.FILL_BOTH);

		getControl().setLayoutData(data);
	}

	public void setUIView(IUIView uiv) {
		if (uiv instanceof TableView){
			this.table_view = (TableView)uiv;
		} else {
			throw new RuntimeException("Wrong instance of UIView"); 
		}
	}

	public IFmtGfx getFmtGraphics() {
		return new AWTGraphics2D(entry.renderer.getGraphics2D());
	}

	public void setRoot() {
		tableContentProvider.setRoot(table_view);
	}

	public void format() {
		tableContentProvider.format(table_view);
	}


	public Drawable getDrawableAt(int x, int y) {
		Point p = new Point(x, y);
		Item item = this.getItemAt(p);
		Object sel = item.getData();
		if (sel == null || !(sel instanceof Drawable))
			return null;
		return (Drawable)sel;
	}


	public void repaint() {
		// TODO Auto-generated method stub

	}

	public void requestFocus() {
		// TODO Auto-generated method stub

	}

	public void mouseDoubleClick(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseDown(MouseEvent e) {
		table_view.inputEvent(new InputEventInfo(e));
	}

	public void mouseUp(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void setRoot(ANode node) {
		tableContentProvider.setRoot(node);
		
	}
}

class ANodeTableContentProvider implements IStructuredContentProvider {
	private TableView table_view;
	private EventListenerList listenerList = new EventListenerList();
	private TableModel data = new TableModel();
	private static final Object[] EMPTY = new Object[] {};

	class TableModel {
		protected Vector<Object>    dataVector;
		protected Vector<Object>    columnIdentifiers;
		public TableModel() {
			this(0, 0);
		}
		public TableModel(int rowCount, int columnCount) {
			setDataVector(newVector(rowCount), newVector(columnCount));
		}
		public TableModel(Vector<Object> columnNames, int rowCount) {
			setDataVector(newVector(rowCount), columnNames);
		}
		public void setDataVector(Object[][] dataVector, Object[] columnIdentifiers) {
			setDataVector(convertToVector(dataVector), convertToVector(columnIdentifiers));
		}
		protected Vector<Object> convertToVector(Object[][] anArray) {
			if (anArray == null) {
				return null;
			}
			Vector<Object> v = new Vector<Object>(anArray.length);
			for (int i=0; i < anArray.length; i++) {
				v.addElement(convertToVector(anArray[i]));
			}
			return v;
		}
		protected Vector<Object> convertToVector(Object[] anArray) {
			if (anArray == null) { 
				return null;
			}
			Vector<Object> v = new Vector<Object>(anArray.length);
			for (int i=0; i < anArray.length; i++) {
				v.addElement(anArray[i]);
			}
			return v;
		}

		public void setDataVector(Vector<Object> dataVector, Vector<Object> columnIdentifiers) {
			this.dataVector = nonNullVector(dataVector);
			this.columnIdentifiers = nonNullVector(columnIdentifiers); 
			justifyRows(0, getRowCount()); 
			fireTableStructureChanged();
		}
		public void fireTableStructureChanged() {
			fireTableChanged(new TableContentEvent(this, TableContentEvent.HEADER_ROW));
		}
		public void fireTableChanged(TableContentEvent e) {
			// Guaranteed to return a non-null array
			Object[] listeners = listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length-2; i>=0; i-=2) {
				if (listeners[i]==TableContentListener.class) {
					((TableContentListener)listeners[i+1]).tableContentChanged(e);
				}
			}
		}
		public int getRowCount() {
			return dataVector.size();
		}
		public int getColumnCount() {
			return columnIdentifiers.size();
		}

		private Vector<Object> nonNullVector(Vector<Object> v) { 
			return (v != null) ? v : new Vector<Object>(); 
		} 

		private Vector<Object> newVector(int size) { 
			Vector<Object> v = new Vector<Object>(size); 
			v.setSize(size); 
			return v; 
		}
		public void addRow(Object[] rowData) {
			addRow(convertToVector(rowData));
		}
		public void addRow(Vector<Object> rowData) {
			insertRow(getRowCount(), rowData);
		}
		public void insertRow(int row, Object[] rowData) {
			insertRow(row, convertToVector(rowData));
		}
		public void insertRow(int row, Vector<Object> rowData) {
			dataVector.insertElementAt(rowData, row); 
			justifyRows(row, row+1); 
			fireTableRowsInserted(row, row);
		}
		public void fireTableRowsInserted(int firstRow, int lastRow) {
			fireTableChanged(new TableContentEvent(this, firstRow, lastRow,
					TableContentEvent.ALL_COLUMNS, TableContentEvent.INSERT));
		}

		@SuppressWarnings("unchecked")
		private void justifyRows(int from, int to) { 
			// Sometimes the TableModel is subclassed 
			// instead of the AbstractTableModel by mistake. 
			// Set the number of rows for the case when getRowCount 
			// is overridden. 
			dataVector.setSize(getRowCount()); 

			for (int i = from; i < to; i++) { 
				if (dataVector.elementAt(i) == null) { 
					dataVector.setElementAt(new Vector<Object>(), i); 
				}
				((Vector<Object>)dataVector.elementAt(i)).setSize(getColumnCount());
			}
		}
    public Vector<Object> getDataVector() {
      return dataVector;
    }
	}
	public void setRoot(ANode node) {
		TableModel tm = (TableModel)this.getData();
		if (node == null){ tm.setDataVector ((Object[][])null, null); return;}
		String[] newIdentifiers = new String[] {"Class", "Attr", "Node"};
		tm.setDataVector(null, newIdentifiers);
		if (node == null) return;
		Object[] rowData = {node.getClass()};
		tm.addRow(rowData);
		for (AttrSlot slot: node.values()) {
			String name = slot.name; 
			Object[] rowData1 = {"", name};
			tm.addRow(rowData1);
			if (slot instanceof ScalarAttrSlot) {
				ScalarAttrSlot s = (ScalarAttrSlot)slot;
				Object obj = s.get(node);
				Object[] rowData2 = {"", "", obj};
				tm.addRow(rowData2);
			}
			else if (slot instanceof SpaceAttrSlot) {
				SpaceAttrSlot s = (SpaceAttrSlot)slot;
				ANode[] arr = s.getArray(node); 
				for (ANode an: arr){
					Object[] rowData2 = {"", "", an};
					tm.addRow(rowData2);
				}
			}
			else if (slot instanceof ExtSpaceAttrSlot) {
				ExtSpaceAttrSlot s = (ExtSpaceAttrSlot)slot;
				for (ExtChildrenIterator i = s.iterate(node); i.hasMoreElements();){
					ANode an = i.nextElement();
					Object[] rowData2 = {"", "", an};
					tm.addRow(rowData2);
				}
			}
			else if (slot instanceof ParentAttrSlot) {
				ParentAttrSlot s = (ParentAttrSlot)slot;
				Object obj = s.get(node);
				Object[] rowData2 = {"", "", obj};
				tm.addRow(rowData2);

			}
		}
	}

	void format(TableView table_view) {
		this.table_view = table_view;
		Drawable old_root = table_view.view_root;
		table_view.formatter.format(table_view.the_root, table_view.view_root, table_view.getSyntax());
		table_view.view_root = table_view.formatter.getRootDrawable();
		if (table_view.view_root != old_root)
			fireTableContentChanged(this, table_view.view_root);
	}

	void setRoot(TableView table_view) {
		this.table_view = table_view;
		table_view.formatter.format(table_view.the_root, null, table_view.getSyntax());
		table_view.view_root = table_view.formatter.getRootDrawable();
		fireTableContentChanged(this, table_view.view_root);
	}

	public Object getRoot() {
		if (table_view == null)
			return null;
		return table_view.view_root;
	}

	private void fireTableContentChanged(Object source, Object elem) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TableContentEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TableContentListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new TableContentEvent(source);
				((TableContentListener)listeners[i+1]).tableContentChanged(e);
			}
		}
	}

	public Object[] getElements(Object arg0) {
		Object[] elements = data.getDataVector().toArray();
		if (elements != null) return elements;
		return EMPTY;
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the data
	 */
	public TableModel getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(TableModel data) {
		this.data = data;
	}

}

