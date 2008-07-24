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

import kiev.fmt.Draw_SyntaxFunction;
import kiev.fmt.Drawable;
import kiev.gui.Editor.CurElem;


public interface IEditor {

	public void makeCurrentVisible();
	public ActionPoint getActionPoint(boolean next);
	public Drawable getFunctionTarget(Draw_SyntaxFunction sf);
	public CurElem getCur_elem();
	
}