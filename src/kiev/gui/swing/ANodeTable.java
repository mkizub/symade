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
package kiev.gui.swing;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTable;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.IFmtGfx;
import kiev.gui.INodeTable;
import kiev.gui.IUIView;
import kiev.gui.TableView;
import kiev.gui.event.InputEventInfo;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtChildrenIterator;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.ParentAttrSlot;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.SpaceAttrSlot;


public class ANodeTable extends JTable implements INodeTable, MouseListener {
	private static final long serialVersionUID = 6919873102266566145L;
	TableView table_view;

	ANodeTable() {
		super(new ANodeTableModel());
//		getModel().setCellEditable(false);
		setDefaultRenderer(Object.class, new DrawableTableCellRenderer()); //TODO parameters
		this.addMouseListener(this);
	}
	
	public void setUIView(IUIView uiv) {
		if (uiv instanceof TableView){
			this.table_view = (TableView)uiv;
		} else {
			throw new RuntimeException("Wrong instance of UIView"); 
		}
	}

	public IFmtGfx getFmtGraphics() {
		return new AWTGraphics2D((Graphics2D)this.getGraphics());
	}
	
	public void setRoot() {
		ANodeTableModel model = (ANodeTableModel)dataModel;
		model.setRoot(table_view);
	}

	public void format() {
		ANodeTableModel model = (ANodeTableModel)dataModel;
		model.format(table_view);
	}

	public void setRoot(ANode node) {
		DefaultTableModel tm = (DefaultTableModel)this.getModel();
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
	
	public Drawable getDrawableAt(int x, int y) {
		Point pnt = new Point(x, y);
		Object sel = this.getValueAt(this.rowAtPoint(pnt), this.columnAtPoint(pnt) );
		if (sel == null || !(sel instanceof Drawable))
			return null;
		return (Drawable)sel;
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent evt) {
		boolean consume = table_view.inputEvent(new InputEventInfo(evt));
		if (consume) {
			evt.consume();
			return;
		}
	}
}

final class ANodeTableModel extends DefaultTableModel {
	private static final long serialVersionUID = -7916598298174485497L;
	private TableView table_view;
	private EventListenerList listenerList = new EventListenerList();

	void format(TableView table_view) {
		this.table_view = table_view;
		Drawable old_root = table_view.view_root;
		table_view.formatter.format(table_view.the_root, table_view.view_root, table_view.getSyntax());
		table_view.view_root = table_view.formatter.getRootDrawable();
		if (table_view.view_root != old_root)
			fireTableModelChanged(this, table_view.view_root);
	}

	void setRoot(TableView table_view) {
		this.table_view = table_view;
		table_view.formatter.format(table_view.the_root, null, table_view.getSyntax());
		table_view.view_root = table_view.formatter.getRootDrawable();
		fireTableModelChanged(this, table_view.view_root);
	}

	public Object getRoot() {
		if (table_view == null)
			return null;
		return table_view.view_root;
	}

	private void fireTableModelChanged(TableModel source, Object elem) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TableModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TableModelListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new TableModelEvent(source);
				((TableModelListener)listeners[i+1]).tableChanged(e);
			}
		}
	}

}

class DrawableTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = -2246801602064539807L;

	public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
	{
		assert (table instanceof ANodeTable);
		if (value instanceof DrawTerm) {
			Object term_obj = ((DrawTerm)value).getTermObj();
			if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
				value = "\u25d8"; // ◘
			else if (value == DrawTerm.NULL_NODE)
				value = "\u25c6"; // ◆
			else {
				value = String.valueOf(term_obj);
				if (value == null)
					value = "\u25d8"; // ◘
			}
		}
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
	}
}

