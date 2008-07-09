package kiev.gui;

import kiev.fmt.Draw_ATextSyntax;
import kiev.vtree.ANode;

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
public interface UIView {

	public boolean isRegisteredToElementEvent();
	public Draw_ATextSyntax getSyntax();
	public void setSyntax(Draw_ATextSyntax syntax);
	public abstract void setRoot(ANode root);
	public abstract void formatAndPaint(boolean full);
	public abstract void formatAndPaintLater(ANode node);
	public BgFormatter setBg_formatter(BgFormatter bg_formatter);
	
	
}
