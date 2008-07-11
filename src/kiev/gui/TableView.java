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
package kiev.gui;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Drawable;
import kiev.fmt.GfxFormatter;
import kiev.gui.event.ElementEvent;
import kiev.gui.swing.ANodeTable;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;


/**
 */

public abstract class TableView extends UIView implements KeyListener {


	protected final ANodeTable table;

	public TableView(IWindow window, Draw_ATextSyntax syntax, ANodeTable table) {
		super(window, syntax);
		this.table = table;
		this.formatter = new GfxFormatter(table.getFmtGraphics());
		this.table.setTable_view(this);
		this.table.addKeyListener(this);
		this.table.addMouseListener(this);
		this.setRoot(null);
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

	
	@Override
	public void elementChanged(ElementEvent e) {
		super.elementChanged(e);
		ANode node = ((Editor)e.getSource()).getCur_elem().node;
		createModel(node);
		formatAndPaintLater(node);		
	}

	public abstract void createModel (ANode node);
}



