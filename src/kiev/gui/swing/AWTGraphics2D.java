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

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.font.TextLayout;
import java.util.HashMap;

import kiev.fmt.Draw_Font;
import kiev.fmt.Draw_Icon;
import kiev.fmt.IFmtGfx;

public final class AWTGraphics2D implements IFmtGfx {
	
	private static final HashMap<String, Font> fontMap = new HashMap<String, Font>();
	private static final HashMap<String, Image> imageMap = new HashMap<String, Image>();
	
	private static final Component img_component = new Component() {};
	private static final Font	defaultFont = new Font("Dialog", Font.PLAIN, 12);
	private static       Font   lastFont;
	private static       String lastFontName;
	private static final Image	defaultImage;
	static {
		defaultImage = Toolkit.getDefaultToolkit().getImage("stx-fmt/fail.png");
		MediaTracker mt = new MediaTracker(img_component);
		mt.addImage(defaultImage, 1);
		try { mt.waitForAll(); } catch (InterruptedException e) {}
	}
	private static       Image  lastImage;
	private static       String lastImageName;

	private final Graphics2D	gfx;
	
	private int textWidth;
	private int textHeight;
	private int textBaseline;
	
	public static Font decodeFont(Draw_Font font) {
		if (font == null)
			return defaultFont;
		if (font.font_object != null)
			return (Font)font.font_object;
		String name = font.font_name;
		if (name == null || name == "") {
			font.font_object = defaultFont;
			return defaultFont;
		}
		if (name == lastFontName) {
			font.font_object = lastFont;
			return lastFont;
		}
		lastFontName = name;
		lastFont = fontMap.get(name);
		if (lastFont != null) {
			font.font_object = lastFont;
			return lastFont;
		}
		lastFont = Font.decode(name);
		font.font_object = lastFont;
		fontMap.put(lastFontName, lastFont);
		return lastFont;
	}
	
	public static Image decodeImage(Draw_Icon img) {
		if (img == null)
			return defaultImage;
		if (img.icon_object != null)
			return (Image)img.icon_object;
		String name = img.icon_name;
		if (name == null || name == "") {
			img.icon_object = defaultImage;
			return defaultImage;
		}
		if (name == lastImageName) {
			img.icon_object = lastImage;
			return lastImage;
		}
		lastImageName = name;
		lastImage = imageMap.get(name);
		if (lastImage != null) {
			img.icon_object = lastImage;
			return lastImage;
		}
		lastImage = Toolkit.getDefaultToolkit().getImage(name);
		MediaTracker mt = new MediaTracker(img_component);
		mt.addImage(lastImage, 1);
		try { mt.waitForAll(); } catch (InterruptedException e) {}
		img.icon_object = lastImage;
		imageMap.put(lastImageName, lastImage);
		return lastImage;
	}
	
	public AWTGraphics2D(Graphics2D gfx) {
		this.gfx = gfx;
	}

	public Graphics2D getNative() { return gfx; }
	
	public void layoutIcon(Draw_Icon image) {
		if (gfx == null) return;
		Image img = decodeImage(image);
		this.textWidth = img.getWidth(null);
		this.textHeight = img.getHeight(null);
		this.textBaseline = 0;
	}
	
	public void layoutText(String text, Draw_Font font) {
		if (gfx == null) return;
		TextLayout tl = new TextLayout(text, decodeFont(font), gfx.getFontRenderContext());
		this.textWidth = (int)Math.ceil(tl.getAdvance());
		this.textHeight = (int)Math.ceil(tl.getAscent()+tl.getDescent()+tl.getLeading());
		this.textBaseline = (int)Math.ceil(tl.getAscent()+tl.getLeading());
	}
	
	public int  textWidth() { return textWidth; }
	public int  textHeight() { return textHeight; }
	public int  textBaseline() { return textBaseline; }

}
