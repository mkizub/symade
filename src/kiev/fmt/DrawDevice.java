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
package kiev.fmt;

import syntax kiev.Syntax;

public class TextPrinter {
	StringBuffer sb;
	int pos_x = 0;
	int pos_y = 0;
	
	public TextPrinter(StringBuffer sb) {
		this.sb = sb;
	}
	
	public void draw(Drawable root) {
		DrawTerm dr_leaf = root.getFirstLeaf();
		TxtDrawTermLayoutInfo leaf = (TxtDrawTermLayoutInfo)dr_leaf.dt_fmt;
		for (; leaf != null; leaf= leaf.getNext()) {
			int x = leaf.x;
			int y = leaf.lineno - 1;

			while (pos_y < y) {
				sb.append('\n');
				pos_y++;
				pos_x = 0;
			}
			while (pos_x < x) {
				sb.append(' ');
				pos_x++;
			}
			
			Object term_obj = leaf.dterm.getTermObj();
			if (term_obj != null && term_obj != DrawTerm.NULL_NODE && term_obj != DrawTerm.NULL_VALUE) {
				String text = String.valueOf(term_obj);
				if (text != null) {
					sb.append(text);
					pos_x += text.length();
					x += text.length();
				}
			}
			while (pos_x < x) {
				sb.append(' ');
				pos_x++;
			}
		}
	}
}

