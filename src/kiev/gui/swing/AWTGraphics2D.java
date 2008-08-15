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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.util.HashMap;

import kiev.fmt.IFmtGfx;

public final class AWTGraphics2D implements IFmtGfx {
	
	private static final HashMap<String, Font> fontMap = new HashMap<String, Font>();
	
	private static final Font	defaultFont = new Font("Dialog", Font.PLAIN, 12);
	private static       Font   lastFont;
	private static       String lastFontName;

	private final Graphics2D	gfx;
	
	private int textWidth;
	private int textHeight;
	private int textBaseline;
	
	public static Font decodeFont(String name) {
		if (name == null || name == "")
			return defaultFont;
		if (name == lastFontName)
			return lastFont;
		lastFontName = name;
		lastFont = fontMap.get(name);
		if (lastFont != null)
			return lastFont;
		lastFont = Font.decode(name);
		fontMap.put(lastFontName, lastFont);
		return lastFont;
	}
	
	public AWTGraphics2D(Graphics2D gfx) {
		this.gfx = gfx;
	}

	public Graphics2D getNative() { return gfx; }
	
	public void layoutText(String text, String font_name) {
		if (gfx == null) return;
		TextLayout tl = new TextLayout(text, decodeFont(font_name), gfx.getFontRenderContext());
		this.textWidth = (int)Math.ceil(tl.getAdvance());
		this.textHeight = (int)Math.ceil(tl.getAscent()+tl.getDescent()+tl.getLeading());
		this.textBaseline = (int)Math.ceil(tl.getAscent()+tl.getLeading());
	}
	
	public int  textWidth() { return textWidth; }
	public int  textHeight() { return textHeight; }
	public int  textBaseline() { return textBaseline; }

}
