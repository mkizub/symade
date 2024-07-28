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
package kiev.fmt.common;

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.vtree.INode;
import kiev.vtree.Context;
import kiev.vlang.Env;

import static kiev.stdlib.Asserts.*;

public class FormatterContext extends Context {
	public final Formatter formatter;
	
	public FormatterContext(Formatter formatter) {
		this.formatter = formatter;
	}
}

public abstract class Formatter {
	
	static class LstStx {
		final Draw_ATextSyntax	stx;
		final LstStx			tl;
		LstStx(Draw_ATextSyntax stx, LstStx tl) {
			this.stx = stx;
			this.tl = tl;
		}
	}

	/** A stack of syntaxes */
	private LstStx syntax_stack;
	
	public final Env                env;
	public final FormatterContext   formatter_context;
	protected Drawable				dr_root;
	protected DrawLayoutInfo		dlb_root;
	protected Draw_StyleSheet		css;

	protected Formatter(Env env) {
		this.env = env;
		this.formatter_context = new FormatterContext(this);
	}

	public final Drawable         getRootDrawable() { return dr_root; }
	public final DrawLayoutInfo  getRootDrawLayoutBlock() { return dlb_root; }

	public abstract void formatTerm(DrawTerm dr, Object term_obj);
	public abstract void format(INode node, Drawable dr, Draw_ATextSyntax syntax, Draw_StyleSheet css);
	
	public final Drawable getDrawable(INode node, Drawable dr) {
		if (dr != null && dr.drnode == node)
			return dr;
		Draw_SyntaxElem stx_elem = syntax_stack.stx.getSyntaxElem(node, syntax_stack.tl, env);
		dr = stx_elem.makeDrawable(this,node);
		return dr;
	}
	
	public final void initSyntax(Draw_ATextSyntax stx, Draw_StyleSheet css) {
		this.syntax_stack = new LstStx(stx, null);
		this.css = css;
	}
	public final LstStx getSyntaxList() {
		return syntax_stack;
	}
	public final Draw_ATextSyntax getSyntax() {
		return syntax_stack.stx;
	}
	public final void pushSyntax(Draw_ATextSyntax stx) {
		if (stx != null)
			syntax_stack = new LstStx(stx, syntax_stack);
	}
	public final void popSyntax(Draw_ATextSyntax stx) {
		if (stx == null)
			return;
		assert (stx == syntax_stack.stx);
		syntax_stack = syntax_stack.tl;
	}
	
	public abstract int getSize(Draw_Size sz);
}

