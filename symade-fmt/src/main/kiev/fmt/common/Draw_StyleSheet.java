package kiev.fmt.common;

import java.io.Serializable;
import java.util.Hashtable;

public class Draw_StyleSheet implements Serializable {
	private static final long serialVersionUID = 4077397682746356690L;

	public  String			q_name;	// qualified name
	public  Draw_StyleSheet[]	super_styles;
	public  Draw_Font[]		fonts;
	public  Draw_Color[]	colors;
	public  Draw_Style[]	styles;
	public  Draw_Style		default_style;

	private transient Hashtable<String,Draw_Style>		allStyles;

	public Draw_StyleSheet init() {
		if (allStyles != null)
			return this;
		allStyles = new Hashtable<String,Draw_Style>();
		init(allStyles);
		if (default_style == null) {
			default_style = new Draw_Style();
			default_style.name = "style-default";
		}
		return this;
	}

	private Draw_StyleSheet init(Hashtable<String,Draw_Style> allStyles) {
		for (Draw_Style style : styles) {
			allStyles.put(style.name, style);
			if (default_style == null && style.name == "style-default")
				default_style = style;
		}
		if (super_styles != null) {
			for (Draw_StyleSheet sup : super_styles)
				sup.init(allStyles);
		}
		return this;
	}
	
	public Draw_Style getStyle(Draw_Style style) {
		if (style == null)
			return default_style;
		Draw_Style st = allStyles.get(style.name);
		if (st != null)
			return st;
		if (style.fallback == null)
			return default_style;
		for (String name : style.fallback) {
			st = allStyles.get(name);
			if (st != null)
				return st;
		}
		return default_style;
	}

	public Draw_Style getStyle(String[] names) {
		if (names == null)
			return default_style;
		for (String name : names) {
			Draw_Style style = allStyles.get(name);
			if (style != null)
				return style;
		}
		return default_style;
	}
}
