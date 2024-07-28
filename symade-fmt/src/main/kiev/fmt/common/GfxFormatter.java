package kiev.fmt.common;

import java.util.regex.Pattern;

import kiev.fmt.DrawCharTerm;
import kiev.fmt.DrawCharValueTerm;
import kiev.fmt.DrawEmptyNodeTerm;
import kiev.fmt.DrawEnumValueTerm;
import kiev.fmt.DrawErrorTerm;
import kiev.fmt.DrawFloatValueTerm;
import kiev.fmt.DrawIdent;
import kiev.fmt.DrawIntValueTerm;
import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.DrawStrTerm;
import kiev.fmt.DrawStrValueTerm;
import kiev.fmt.DrawTerm;
import kiev.fmt.DrawToken;
import kiev.fmt.DrawValueTerm;
import kiev.fmt.Drawable;
import kiev.fmt.StyleProvider;
import kiev.vtree.INode;
import kiev.vtree.Symbol;
import kiev.vtree.SymbolRef;
import kiev.vlang.Env;

import static kiev.stdlib.Asserts.*;

public class GfxFormatter extends Formatter {
	
	public static final Pattern patternIdent = Pattern.compile("[\\p{Alpha}_\\-$][\\p{Alnum}_\\-$]*");
	public static final Pattern patternSepar = Pattern.compile("[\\Q.,;:()[]{}\\E]+");

	private IFmtGfx		gfx;
	private int			width;
	
	public GfxFormatter(Env env, IFmtGfx gfx) {
		super(env);
		assert(gfx != null);
		this.gfx = gfx;
		this.width = 100;
	}
	
	public void setWidth(int w) {
		if (w < 100)
			this.width = 100;
		else
			this.width = w;
	}
	
	public Draw_Style calcStyle(Drawable dr) {
		String[] style_names = dr.syntax.style_names;
		if (style_names == null || style_names.length == 0) {
			INode parent = dr.parent();
			if (parent instanceof StyleProvider)
				style_names = ((Drawable)parent).syntax.style_names;
		}
		if (style_names == null) {
			if (dr instanceof DrawTerm) {
				Object val;
				if (dr instanceof DrawIdent) {
					val = ((DrawIdent)dr).getAttrObject();
					if (dr.drnode instanceof SymbolRef)
						style_names = new String[]{"style-symref", "style-ident", "style-default"};
					else if (dr.drnode instanceof Symbol)
						style_names = new String[]{"style-symbol", "style-ident", "style-default"};
					else if (val instanceof SymbolRef)
						style_names = new String[]{"style-symref", "style-ident", "style-default"};
					else if (val instanceof Symbol)
						style_names = new String[]{"style-symbol", "style-ident", "style-default"};
					else
						style_names = new String[]{"style-ident", "style-default"};
				}
				else if (dr instanceof DrawPlaceHolder) {
					style_names = new String[]{"style-placeholder", "style-default"};
				}
				else if (dr instanceof DrawErrorTerm) {
					style_names = new String[]{"style-error", "style-default"};
				}
				else if (dr instanceof DrawEmptyNodeTerm) {
					style_names = new String[]{"style-empty", "style-default"};
				}
				else if (dr instanceof DrawValueTerm) {
					if (dr instanceof DrawCharValueTerm || dr instanceof DrawCharTerm)
						style_names = new String[]{"style-char", "style-value", "style-default"};
					else if (dr instanceof DrawStrValueTerm || dr instanceof DrawStrTerm)
						style_names = new String[]{"style-string", "style-value", "style-default"};
					else if (dr instanceof DrawIntValueTerm)
						style_names = new String[]{"style-int", "style-value", "style-default"};
					else if (dr instanceof DrawFloatValueTerm)
						style_names = new String[]{"style-float", "style-value", "style-default"};
					else if (dr instanceof DrawEnumValueTerm)
						style_names = new String[]{"style-keyword", "style-value", "style-default"};
					else
						style_names = new String[]{"style-value", "style-default"};
				}
				else if (dr instanceof DrawToken) {
					val = ((DrawToken)dr).getTermObj();
					if (val instanceof String) {
						String text = (String)val;
						if (patternIdent.matcher(text).matches())
							style_names = new String[]{("style-"+text).intern(), ("style-keyword-"+text).intern(), "style-keyword", "style-default"};
						else if (patternSepar.matcher(text).matches())
							style_names = new String[]{("style-"+text).intern(), ("style-separator-"+text).intern(), "style-separator", "style-default"};
						else
							style_names = new String[]{("style-"+text).intern(), ("style-operator-"+text).intern(), "style-operator", "style-default"};
					}
				}
			}
		}
		Draw_Style style = null;
		if (css != null)
			style = css.getStyle(style_names);
		if (style == null) {
			for (LstStx lst=getSyntaxList(); lst != null; lst = lst.tl) {
				Draw_ATextSyntax stx = lst.stx;
				if (stx.style_sheet == null)
					continue;
				style = stx.style_sheet.getStyle(style_names);
				if (style != null)
					break;
			}
		}
		return style;
	}
	
	public void formatTerm(DrawTerm dr, Object term_obj) {
		DrawLayoutInfo gfx_fmt = dr.getGfxFmtInfo();
		gfx_fmt.style = calcStyle(dr);
		gfx_fmt.x_offs = 0;
		gfx_fmt.y_offs = 0;
		gfx_fmt.width = 0;
		gfx_fmt.height = 0;
		gfx_fmt.baseline = 0;
		if (term_obj instanceof Draw_Icon) {
			gfx.layoutIcon((Draw_Icon)term_obj);
			int w = gfx.textWidth();
			int h = gfx.textHeight();
			gfx_fmt.width = w;
			gfx_fmt.height = h;
		} else {
			String text;
			if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
				text = "\u25d8"; // ◘
			else if (term_obj == DrawTerm.NULL_NODE)
				text = "\u25c6"; // ◆
			else {
				text = String.valueOf(term_obj);
				if (text == null)
					text = "\u25d8"; // ◘
			}
			if (text.length() != 0) {
				Draw_Style style = gfx_fmt.style;
				if (style != null)
					gfx.layoutText(text, style.font);
				else
					gfx.layoutText(text, null);
				int w = gfx.textWidth();
				int h = gfx.textHeight();
				gfx_fmt.width = w;
				gfx_fmt.height = h;
				gfx_fmt.baseline = gfx.textBaseline();
			}
		}
	}
	
	private DrawLayoutInfo getFirstSubBlock(DrawLayoutInfo dli) {
		for (DrawLayoutInfo d : dli.getBlocks()) {
			if (d.isSubBlock())
				return d;
			d = getFirstSubBlock(d);
			if (d != null)
				return d;
		}
		return null;
	}

	public void format(INode node, Drawable dr, Draw_ATextSyntax syntax, Draw_StyleSheet css) {
		initSyntax(syntax, css);
		dr_root = getDrawable(node, dr);
		try {
			dr_root.preFormat(this, dr_root.syntax, node);
		} catch (ChangeRootException e) {
			dr_root = e.dr;
			dr_root.preFormat(this, dr_root.syntax, node);
		}
		{
			dlb_root = new DrawLayoutInfo(null, null);
			dr_root.postFormat(dlb_root, this);
			dlb_root.closeBuild();
			dlb_root.lnkFormat(dr_root, true);
			DrawContext ctx = new DrawContext(this,dlb_root,this.width);
			ctx.postFormat(dlb_root);
		}
	}
	
	public int getSize(Draw_Size sz) {
		if (sz == null)
			return 0;
		return sz.gfx_size;
	}
}

