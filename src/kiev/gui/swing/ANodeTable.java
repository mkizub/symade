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

import javax.swing.JTable;


public class ANodeTable extends JTable {
	private static final long serialVersionUID = 6919873102266566145L;
	TableView table_view;

	ANodeTable() {
		super(new ANodeTableModel());
//		getModel().setCellEditable(false);
		setDefaultRenderer(Object.class, new DrawableTableCellRenderer()); //TODO parameters
	}

	public void setRoot() {
		ANodeTableModel model = (ANodeTableModel)dataModel;
		model.setRoot(table_view);
	}

	public void format() {
		ANodeTableModel model = (ANodeTableModel)dataModel;
		model.format(table_view);
	}
}
