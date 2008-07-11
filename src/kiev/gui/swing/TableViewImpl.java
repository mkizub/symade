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
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Drawable;
import kiev.gui.IWindow;
import kiev.gui.TableView;
import kiev.gui.event.EventListenerList;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtChildrenIterator;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.ParentAttrSlot;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.SpaceAttrSlot;

public class TableViewImpl extends TableView {

	public TableViewImpl(IWindow window, Draw_ATextSyntax syntax, ANodeTable table) {
		super(window, syntax, table);
	}

	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask == (KeyEvent.CTRL_DOWN_MASK|KeyEvent.ALT_DOWN_MASK)) {
			switch (code) {
			case KeyEvent.VK_S: {
				evt.consume();
				// build a menu of types to instantiate
				JPopupMenu m = new JPopupMenu();
				m.add(new JMenuItem(new RenderActions.SetSyntaxAction(this,"Project Tree Syntax", "stx-fmt\u001fsyntax-for-project-tree", false)));
				m.add(new JMenuItem(new RenderActions.SetSyntaxAction(this,"Project Tree Syntax  (current)", "stx-fmt\u001fsyntax-for-project-tree", true)));
				m.show(table, 0, 0);
				break;
			}
			}
		}
	}

	public void createModel (ANode node){
		DefaultTableModel tm = (DefaultTableModel)table.getModel();
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
		if (value instanceof DrawTerm)
			value = ((DrawTerm)value).getText();
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
	}
}
