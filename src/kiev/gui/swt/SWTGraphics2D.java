/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
 *******************************************************************************/
package kiev.gui.swt;

import java.util.HashMap;
import java.util.Locale;

import kiev.fmt.common.Draw_Font;
import kiev.fmt.common.Draw_Icon;
import kiev.fmt.common.IFmtGfx;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;

/**
 * The SWT implementation of graphics. Instances of this class used for
 * formatters that format a textual representation of the AST and caching.
 */
public final class SWTGraphics2D implements IFmtGfx {

	/**
	 * The <code>Font</code> cache. 
	 */
	private static final HashMap<String, Font> fontMap = new HashMap<String, Font>();
	
	/**
	 * The <code>Image</code> cache.
	 */
	private static final HashMap<String, Image> imageMap = new HashMap<String, Image>();

	/**
	 * default Font
	 */
	private static Font defaultFont;
	
	/**
	 * last Font
	 */
	private static Font lastFont;
	
	/**
	 * last Font Name
	 */
	private static String lastFontName;
	
	/**
	 * default Image
	 */
	private static Image defaultImage;

	/**
	 * last Image
	 */
	private static Image lastImage;
	
	/**
	 * last Image Name
	 */
	private static String lastImageName;

	/**
	 * Graphics context.
	 */
	private final GC gc;

	/**
	 * text Width
	 */
	private int textWidth;
	
	/**
	 * text Height
	 */
	private int textHeight;
	
	/**
	 * text Baseline
	 */
	private int textBaseline;

	/**
	 * The constructor.
	 * @param gc the graphics context
	 */
	public SWTGraphics2D(GC gc) {
		this.gc = gc;
	}

	/**
	 * Describe the font names.
	 */
	private final static class FontDescription {
		
		/**
		 * The font name.
		 * @see FontDescription#FontDescription(String, int, int)
		 */
		private final String fontName;
		
		/**
		 * The font size
		 * @see FontDescription#FontDescription(String, int, int)
		 */
		private final int fontSize;
		
		/**
		 * The font style
		 * @see FontDescription#FontDescription(String, int, int)
		 */
		private final int fontStyle;
		
		/**
		 * The constructor.
		 * @param fontName the font name
		 * @param fontSize the font size
		 * @param fontStyle the font style
		 */
		private FontDescription(String fontName, int fontSize, int fontStyle) {
			this.fontName = fontName;
			this.fontSize = fontSize;
			this.fontStyle = fontStyle;
		}
		
	}
	
	/**
	 * Returns the actual font object created or taken from cache. Don't 
	 * dispose this objects because they are used each time the painting occur. 
	 * @param device the device
	 * @param font the font
	 * @return the Font
	 */
	static final Font decodeFont(Device device, Draw_Font font) {
		if (defaultFont == null)
			defaultFont = new Font(device, "Dialog", 12, SWT.NORMAL);
		if (font== null)
			return defaultFont;
		if (font.font_object != null)
			if (font.font_object instanceof Font)
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
		FontDescription fd = fontDecode(name);
		String str = fontEncode(fd);
		lastFont = fontMap.get(str);
		if (lastFont != null) {
			font.font_object = lastFont;
			return lastFont;
		}
		lastFontName = str;
		lastFont = new Font (device, fd.fontName, fd.fontSize, fd.fontStyle);
		font.font_object = lastFont;
		fontMap.put(lastFontName, lastFont);
		return lastFont;
	}

	/**
	 * Encoding font description back to its string
	 * @param fd the font description.
	 * @return the string 
	 */
	private final static String fontEncode(FontDescription fd){
		StringBuilder sb = new StringBuilder();
		if (fd.fontName == null || fd.fontName.isEmpty())
			sb.append("Dialog");
		else
			sb.append(fd.fontName);
		sb.append("-");		
		if ((fd.fontStyle & SWT.BOLD) != 0)
			sb.append("BOLD");
		if ((fd.fontStyle & SWT.ITALIC) != 0)
			sb.append("ITALIC");
		if ((fd.fontStyle & SWT.NORMAL) != 0)
			sb.append("PLAIN");
		sb.append("-");
		sb.append(Integer.valueOf(fd.fontSize).toString());		
		return sb.toString();
	}
	
	/**
	 * Returns the font description from the string representation of the font.
	 * @param str the string representation of the font
	 * @return the font description
	 */
	private final static FontDescription fontDecode(String str) {
		String fontName = str;
		String styleName = "";
		int fontSize = 12;
		int fontStyle = SWT.NORMAL;

		if (str == null) {
			return new FontDescription("Dialog", fontSize, fontStyle);
		}

		int lastHyphen = str.lastIndexOf('-');
		int lastSpace = str.lastIndexOf(' ');
		char sepChar = (lastHyphen > lastSpace) ? '-' : ' ';
		int sizeIndex = str.lastIndexOf(sepChar);
		int styleIndex = str.lastIndexOf(sepChar, sizeIndex-1);
		int strlen = str.length();

		if (sizeIndex > 0 && sizeIndex+1 < strlen) {
			try {
				fontSize =
					Integer.valueOf(str.substring(sizeIndex+1)).intValue();
				if (fontSize <= 0) {
					fontSize = 12;
				}
			} catch (NumberFormatException e) {
				styleIndex = sizeIndex;
				sizeIndex = strlen;
				if (str.charAt(sizeIndex-1) == sepChar) {
					sizeIndex--;
				}
			}
		}

		if (styleIndex >= 0 && styleIndex+1 < strlen) {
			styleName = str.substring(styleIndex+1, sizeIndex);
			styleName = styleName.toLowerCase(Locale.ENGLISH);
			if (styleName.equals("bolditalic")) {
				fontStyle = SWT.BOLD | SWT.ITALIC;
			} else if (styleName.equals("italic")) {
				fontStyle = SWT.ITALIC;
			} else if (styleName.equals("bold")) {
				fontStyle = SWT.BOLD;
			} else if (styleName.equals("plain")) {
				fontStyle = SWT.NORMAL;
			} else {
				styleIndex = sizeIndex;
				if (str.charAt(styleIndex-1) == sepChar) {
					styleIndex--;
				}
			}
			fontName = str.substring(0, styleIndex);
		} else {
			int fontEnd = strlen;
			if (styleIndex > 0) {
				fontEnd = styleIndex;
			} else if (sizeIndex > 0) {
				fontEnd = sizeIndex;
			}
			if (fontEnd > 0 && str.charAt(fontEnd-1) == sepChar) {
				fontEnd--;
			}
			fontName = str.substring(0, fontEnd);
		}
		return new FontDescription(fontName, fontSize, fontStyle);
	}

	/**
	 * Returns the actual image object created or taken from cache. Don't 
	 * dispose this objects because they are used each time the painting occur. 
	 * @param device the device
	 * @param img the image we draw
	 * @return the image
	 */
	final static Image decodeImage(Device device, Draw_Icon img) {
		if (defaultImage == null)
			defaultImage = new Image(device, "stx-fmt/fail.png");
		if (img == null)
			return defaultImage;
		if (img.icon_object != null)
			if (img.icon_object instanceof Image)
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
		lastImage = new Image(device, name);
		img.icon_object = lastImage;
		imageMap.put(lastImageName, lastImage);
		return lastImage;
	}


	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#layoutIcon(kiev.fmt.Draw_Icon)
	 */
	public void layoutIcon(Draw_Icon image) {
		if (gc == null) return;
		Image img = decodeImage(gc.getDevice(), image);
		this.textWidth = img.getBounds().width;
		this.textHeight = img.getBounds().height;
		this.textBaseline = 0;
	}

	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#layoutText(java.lang.String, kiev.fmt.Draw_Font)
	 */
	public void layoutText(String text, Draw_Font font) {
		if (gc == null) return;
		TextLayout tl = new TextLayout(gc.getDevice());
		Font fnt = decodeFont(gc.getDevice(), font);
		TextStyle style = new TextStyle(fnt, null, null);
		tl.setText(text);
		tl.setStyle(style, 0, text.length());
		FontMetrics fm = tl.getLineMetrics(0);
		this.textWidth = tl.getBounds().width;
		this.textHeight = fm.getAscent()+fm.getDescent()+fm.getLeading();
		this.textBaseline = fm.getAscent()+fm.getLeading();
		tl.dispose();
	}

	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#textWidth()
	 */
	public int  textWidth() { return textWidth; }
	
	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#textHeight()
	 */
	public int  textHeight() { return textHeight; }
	
	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#textBaseline()
	 */
	public int  textBaseline() { return textBaseline; }

	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#getNative()
	 */
	public Object getNative() {
		return gc;		
	}

}


