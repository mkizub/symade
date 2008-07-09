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

import java.awt.Graphics;

import kiev.fmt.DrawLayoutInfo;

public interface Canvas {
	
	public void setUIView(UIView uiv);
	public void setBounds(int x, int y, int width, int height);
	public void setFirstLine(int val);
	public void incrFirstLine(int val);
	public Graphics getGraphics();
	public int getImgWidth();
	public void repaint();
	public void requestFocus();
	public boolean isDoubleBuffered();
	public void setDlb_root(DrawLayoutInfo dlb_root);
}
