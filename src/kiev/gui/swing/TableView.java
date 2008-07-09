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

import kiev.vtree.*;
import kiev.vlang.FileUnit;
import kiev.fmt.*;
import kiev.gui.event.ElementEvent;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.event.*;


/**
 */

public class TableView extends UIView implements KeyListener {


	protected final ANodeTable table;

	public TableView(Window window, Draw_ATextSyntax syntax, ANodeTable table) {
		super(window, syntax);
		this.table = table;
		this.formatter = new GfxFormatter((Graphics2D)table.getGraphics());
		this.table.table_view = this;
		this.table.addKeyListener(this);
		this.table.addMouseListener(this);
		this.setRoot(null);
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
	
	public void setRoot(ANode root) {
		this.the_root = root;
		this.table.setRoot();
	}

	@Override
	public void formatAndPaint(boolean full) {
		table.format();
		table.repaint();
	}

	@Override
	public void formatAndPaintLater(ANode node) {
		this.the_root = node;
		this.bg_formatter.schedule_run();
	}
	
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() >= 2) {
			Object sel = table.getValueAt(table.rowAtPoint(new Point(e.getX(), e.getY())), table.columnAtPoint(new Point(e.getX(), e.getY())) );
			if (sel == null || !(sel instanceof Drawable))
				return;
			Drawable dr = (Drawable)sel;
			java.util.Vector<ANode> v = new java.util.Vector<ANode>();
			ANode n = dr.get$drnode();
			//if (n instanceof DNode)
			//	n = n.id;
			v.add(n);
			while (n != null && !(n instanceof FileUnit)) {
				n = n.parent();
				v.add(n);
			}
			if (!(n instanceof FileUnit))
				return;
			parent_window.openEditor((FileUnit)n, v.toArray(new ANode[v.size()]));
			e.consume();
		}
	}


	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

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
	
	@Override
	public void elementChanged(ElementEvent e) {
		super.elementChanged(e);
		ANode node = ((Editor)e.getSource()).cur_elem.node;
		createModel(node);
		formatAndPaintLater(node);		
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
		if (value instanceof Drawable)
			value = ((Drawable)value).getText();
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
	}
}

