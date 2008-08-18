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

import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.GfxFormatter;
import kiev.gui.event.ElementEvent;
import kiev.vtree.ANode;


/**
 */

public class TableView extends UIView {


	protected final INodeTable table;

	public TableView(IWindow window, INodeTable table, Draw_ATextSyntax syntax) {
		super(window, table, syntax);
		this.table = table;
		this.table.setUIView(this);
		this.formatter = new GfxFormatter(table.getFmtGraphics());
		this.setRoot(null);
	}

	@Override
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
	
	@Override
	public void elementChanged(ElementEvent e) {
		super.elementChanged(e);
		ANode node = ((Editor)e.getSource()).getCur_elem().node;
		table.setRoot(node);
		formatAndPaintLater(node);		
	}

	/**
	 * @return the table
	 */
	public INodeTable getView_table() {
		return table;
	}

}


