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

import java.awt.event.ActionListener;

import kiev.gui.event.EventListenerList;
import kiev.gui.event.ElementEvent;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public interface IWindow extends ActionListener {
	public EventListenerList getListenerList();
	public void fireElementChanged(ElementEvent e);
	public void openEditor(FileUnit fu);
	public void openEditor(FileUnit fu, ANode[] path);
	public void closeEditor(Editor ed);
	public InfoView getInfo_view();
	public void setInfo_view(InfoView info_view); 
	public UIView getCurrentView();
}
