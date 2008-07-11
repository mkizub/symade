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

import java.awt.Graphics2D;

import javax.swing.JTable;

import kiev.fmt.IFmtGfx;
import kiev.gui.TableView;


public class ANodeTable extends JTable {
	private static final long serialVersionUID = 6919873102266566145L;
	protected TableView table_view;

	ANodeTable() {
		super(new ANodeTableModel());
//		getModel().setCellEditable(false);
		setDefaultRenderer(Object.class, new DrawableTableCellRenderer()); //TODO parameters
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

	/**
	 * @return the table_view
	 */
	public TableView getTable_view() {
		return table_view;
	}

	/**
	 * @param table_view the table_view to set
	 */
	public void setTable_view(TableView table_view) {
		this.table_view = table_view;
	}
}
