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
import kiev.vtree.ANode;

public interface IUIView {

	public Draw_ATextSyntax getSyntax();
	public void setSyntax(Draw_ATextSyntax syntax);
	public void setRoot(ANode root);
	public void formatAndPaint(boolean full);
	public void formatAndPaintLater(ANode node);
	public IUIViewPeer getViewPeer();

}
