package kiev.fmt.common;

import kiev.fmt.ATextSyntax;
import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.StyleProvider;
import kiev.fmt.common.Formatter.LstStx;
import kiev.vtree.INode;
import kiev.vlang.Env;

public class TextFormatter extends Formatter {
	private ATextSyntax syntax;
	
	private int lineno;

	public TextFormatter(Env env) {
		super(env);
	}

	public Draw_Style calcStyle(Drawable dr) {
		String[] style_names = dr.syntax.style_names;
		if (style_names == null || style_names.length == 0) {
			INode parent = dr.parent();
			if (parent instanceof StyleProvider)
				style_names = ((Drawable)parent).syntax.style_names;
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
		DrawLayoutInfo txt_fmt = dr.getGfxFmtInfo();
		txt_fmt.style = calcStyle(dr);
		txt_fmt.x_offs = 0;
		txt_fmt.y_offs = 0;
		txt_fmt.width = 0;
		txt_fmt.height = 0;
		txt_fmt.baseline = 0;
		if (dr instanceof DrawPlaceHolder) {
			dr.is_hidden = true;
			return;
		}
		if (term_obj instanceof Draw_Icon)
			return;
		if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
			return;
		else if (term_obj == DrawTerm.NULL_NODE)
			return;
		String text = String.valueOf(term_obj);
		if (text == null || text.length() == 0)
			return;
		int w = text.length();
		txt_fmt.width = text.length();
		txt_fmt.height = 1;
	}

	public void format(INode node, Drawable dr, Draw_ATextSyntax syntax, Draw_StyleSheet css) {
		initSyntax(syntax, css);
		dr_root = getDrawable(node, dr);
		{
			try {
				dr_root.preFormat(this, dr_root.syntax, node);
			} catch (ChangeRootException e) {
				dr_root = e.dr;
				dr_root.preFormat(this, dr_root.syntax, node);
			}
		}
		{
			dlb_root = new DrawLayoutInfo(null, null);
			dr_root.postFormat(dlb_root, this);
			dlb_root.closeBuild();
			dlb_root.lnkFormat(dr_root, false);
			DrawContext ctx = new DrawContext(this,dlb_root,1000);
			ctx.postFormat(dlb_root);
		}
	}

	public int getSize(Draw_Size sz) {
		if (sz == null)
			return 0;
		return sz.txt_size;
	}
}

