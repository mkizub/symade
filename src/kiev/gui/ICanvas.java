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

import kiev.fmt.DrawLayoutInfo;
import kiev.fmt.DrawTerm;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.vtree.ANode;

public interface ICanvas extends IUIViewPeer {
	
	public void setBounds(int x, int y, int width, int height);
	public void setFirstLine(int val);
	public void incrFirstLine(int val);
	public int getImgWidth();
	public boolean isDoubleBuffered();
	public void setDlb_root(DrawLayoutInfo dlb_root);
	public void setCurrent(DrawTerm current, ANode current_node);
	public int getTranslated_y();
	public GfxDrawTermLayoutInfo getLast_visible();
	public GfxDrawTermLayoutInfo getFirst_visible();
	public int getCursor_offset();
	public void setCursor_offset(int cursor_offset);
	public int getFirst_line();
	public void setFirst_line(int first_line);
	public int getNum_lines();
		
}
