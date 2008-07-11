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

import kiev.fmt.Drawable;
import kiev.fmt.IFmtGfx;
import kiev.vtree.ANode;

public interface INodeTable {
	
	public void setUIView(IUIView uiv);
	public IFmtGfx getFmtGraphics();
	public void setRoot();
	public void format();
	public void repaint();
	public void createModel(ANode node);
	public Drawable getDrawableAt(int x, int y);
		
}
