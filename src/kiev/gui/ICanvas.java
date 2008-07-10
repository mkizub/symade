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

import java.awt.Component;
import java.awt.event.FocusListener;

import kiev.fmt.DrawLayoutInfo;
import kiev.fmt.DrawTerm;
import kiev.fmt.IFmtGfx;
import kiev.vtree.ANode;

public interface ICanvas {
	
	public void setUIView(IUIView uiv);
	public void setBounds(int x, int y, int width, int height);
	public void setFirstLine(int val);
	public void incrFirstLine(int val);
	public IFmtGfx getFmtGraphics();
	public int getImgWidth();
	public void repaint();
	public void requestFocus();
	public boolean isDoubleBuffered();
	public void setDlb_root(DrawLayoutInfo dlb_root);
	public void setCurrent(DrawTerm current);
	public ANode getCurrent_node();
	public void setCurrent_node(ANode current_node);
	public int getTranslated_y();
	public void setTranslated_y(int translated_y);
	public DrawTerm getLast_visible();
	public void setLast_visible(DrawTerm last_visible);
	public DrawTerm getFirst_visible();
	public void setFirst_visible(DrawTerm first_visible);
	public int getCursor_offset();
	public void setCursor_offset(int cursor_offset);
	public int getFirst_line();
	public void setFirst_line(int first_line);
	public int getNum_lines();
	public void setNum_lines(int num_lines);
	public Component add (Component c);
	public void remove (Component c);
	public void addFocusListener(FocusListener l);
		
}
