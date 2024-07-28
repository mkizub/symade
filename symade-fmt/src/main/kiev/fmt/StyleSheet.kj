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

import kiev.fmt.common.*;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ThisIsANode(lang=SyntaxLang)
public class StyleSheet extends DNode implements GlobalDNodeContainer, ExportSerialized {

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	public StyleSheet⇑∅		super_style;

	@nodeAttr
	public ASTNode∅			members;
	
	@UnVersioned
	protected Draw_StyleSheet compiled;

	public final ASTNode[] getContainerMembers() { this.members }
	
	public Object getDataToSerialize() {
		return this.getCompiled().init();
	}

	public String qname() {
		String q_name = this.sname;
		INode p = parent();
		if (p instanceof GlobalDNode && !(p instanceof KievRoot))
			q_name = (((GlobalDNode)p).qname()+"·"+sname).intern();
		return q_name;
	}

	public rule resolveNameR(ResInfo path)
		StyleSheet⇑@ sup;
	{
		path ?= this
	;
		path @= members
	;
		path.isSuperAllowed(),
		path.getPrevSlotName() != "super_style",
		sup @= super_style,
		sup.dnode != null,
		path.enterSuper() : path.leaveSuper(),
		sup.dnode.resolveNameR(path)
	}
	
	public Draw_StyleSheet getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_StyleSheet();
		compiled.q_name = this.qname();
		Vector<Draw_StyleSheet> super_styles = new Vector<Draw_StyleSheet>();
		foreach (StyleSheet⇑ sup; super_style; sup.dnode != null)
			super_styles.append(sup.dnode.getCompiled());
		compiled.super_styles = super_styles.toArray();
		Vector<Draw_Color> colors = new Vector<Draw_Color>();
		Vector<Draw_Font> fonts = new Vector<Draw_Font>();
		Vector<Draw_Style> styles = new Vector<Draw_Style>();
		foreach (ASTNode n; members) {
			if (n instanceof DrawColor) colors.append(n.compile());
			if (n instanceof DrawFont) fonts.append(n.compile());
			if (n instanceof SyntaxStyleDecl) styles.append(n.compile());
		}
		compiled.fonts = fonts.toArray();
		compiled.colors = colors.toArray();
		compiled.styles = styles.toArray();
		return compiled;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class DrawColor extends DNode {
	@nodeAttr
	public int rgb_color;

	
	public DrawColor() {}
	public DrawColor(String name) {
		this.sname = name;
	}

	public Draw_Color compile() {
		return new Draw_Color(sname, rgb_color);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class DrawFont extends DNode {
	@nodeAttr
	public String font_name;

	public DrawFont() {}
	public DrawFont(String font_name) {
		this.sname = font_name;
		this.font_name = font_name;
	}

	public Draw_Font compile() {
		return new Draw_Font(font_name);
	}
}

public enum SpaceKind {
	SP_SPACE,			// space (x += size)
	SP_NEW_LINE,		// newline (y += size)
	SP_BRK_LINE		// line break if layout does not fit
}

public enum SpaceAction {
	SP_NOP,
	SP_ADD,
	SP_EAT
}

@ThisIsANode(lang=SyntaxLang)
public class SpaceInfo extends DNode {
	@nodeAttr SpaceKind		kind;
	@nodeAttr SyntaxSize		size;
	
	public SpaceInfo() {
		this.size = new SyntaxSize();
	}
	public SpaceInfo(String name, SpaceKind kind, int text_size, int pixel_size) {
		this.sname = name;
		this.size = new SyntaxSize();
		this.kind = kind;
		this.size.unit = SyntaxSizeUnit.PIXELS;
		this.size.txt_size = text_size;
		this.size.gfx_size = pixel_size;
	}

	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		if (size == null)
			size = new SyntaxSize();
		if (size.unit == null)
			size.unit = SyntaxSizeUnit.PIXELS;
		return true;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SpaceCmd extends ASTNode {
	public static final SpaceCmd[] emptyArray = new SpaceCmd[0];

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public SpaceInfo⇑					si;
	
	@nodeAttr
	public SpaceAction					action_before;
	
	@nodeAttr
	public SpaceAction					action_after;
	
	public SpaceCmd() {
		action_before = SP_NOP;
		action_after = SP_NOP;
	}
	public SpaceCmd(SpaceInfo si, SpaceAction action_before, SpaceAction action_after) {
		this.si.symbol = si.symbol;
		this.action_before = action_before;
		this.action_after = action_after;
	}

	public void preResolveOut(Env env, INode parent, AttrSlot slot) {
		if (si.name == null)
			si.name = "sp";
		super.preResolveOut(env, parent, slot);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxStyleDecl extends DNode {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	public SyntaxStyleDecl⇑∅		fallback;
	
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public DrawColor⇑				color;
	
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public DrawFont⇑				font;
	
	@nodeAttr
	public SpaceCmd∅				spaces;

	public String[] getNames() {
		Vector<String> names = new Vector<String>();
		if (sname != null)
			names.append(sname);
		foreach (SyntaxStyleDecl⇑ fb; fallback) {
			String name = fb.name;
			if (name != null && !names.contains(name))
				names.append(name);
		}
		return names.toArray();
	}

	public Draw_Style compile() {
		Draw_Style style = new Draw_Style();
		style.name = this.sname;
		if (fallback.length > 0) {
			style.fallback = new String[fallback.length];
			for (int i=0; i < style.fallback.length; i++)
				style.fallback[i] = fallback[i].name;
		}
		if (this.color != null && this.color.dnode != null)
			style.color = this.color.dnode.compile();
		if (this.font != null && this.font.dnode != null && this.font.dnode.font_name != null)
			style.font = this.font.dnode.compile();
		
		foreach (SpaceCmd sc; spaces) {
			LayoutSpace ls = new LayoutSpace();
			SpaceInfo si = (SpaceInfo)sc.si.dnode;
			if (si == null || si.kind != SP_SPACE)
				continue;
			ls.name = si.sname;
			ls.unit = si.size.unit;
			ls.gfx_size = si.size.gfx_size;
			ls.txt_size = si.size.txt_size;
			switch (sc.action_before) {
			case SP_NOP: break;
			case SP_ADD: style.spaces_before = (LayoutSpace[])Arrays.append(style.spaces_before, ls); break;
			case SP_EAT: style.spaces_before = (LayoutSpace[])Arrays.append(style.spaces_before, ls.setEat()); break;
			}
			switch (sc.action_after) {
			case SP_NOP: break;
			case SP_ADD: style.spaces_after = (LayoutSpace[])Arrays.append(style.spaces_after, ls); break;
			case SP_EAT: style.spaces_after = (LayoutSpace[])Arrays.append(style.spaces_after, ls.setEat()); break;
			}
		}
		return style;
	}
}

public enum SyntaxSizeUnit {
	PIXELS,
	POINTS, // 1/72 inch
	MILLIMETR,
	CENTIMETR,
	INCH;
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxSize extends SNode {
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public SyntaxSizeUnit unit;
	@AttrXMLDumpInfo(attr=true, name="g")
	@nodeAttr public int gfx_size;
	@AttrXMLDumpInfo(attr=true, name="t")
	@nodeAttr public int txt_size; 
	public Draw_Size getCompiled() {
		Draw_Size sz = new Draw_Size();
		sz.unit = this.unit;
		sz.gfx_size = this.gfx_size;
		sz.txt_size = this.txt_size;
		return sz;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class ParagraphOption extends ASTNode {
	public abstract Draw_ParOption getCompiled();
}

public enum SyntaxLineType {
	NONE,
	SINGLE,
	DOUBLE
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphLines extends ParagraphOption {
	@nodeAttr public SyntaxLineType top;
	@nodeAttr public SyntaxLineType bottom;
	@nodeAttr public SyntaxLineType left;
	@nodeAttr public SyntaxLineType right;
	public Draw_ParOption getCompiled() {
		Draw_ParLines l = new Draw_ParLines();
		l.top = this.top;
		l.bottom = this.bottom;
		l.left = this.left;
		l.right = this.right;
		return l;
	}
}

public enum SyntaxAlignType {
	TOP_LEFT,
	TOP_RIGHT,
	TOP_CENTER,
	CENTER_LEFT,
	CENTER_CENTER,
	CENTER_RIGHT,
	BOTTOM_LEFT,
	BOTTOM_CENTER,
	BOTTOM_RIGHT;
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphAlignBlock extends ParagraphOption {
	@nodeAttr public SyntaxAlignType align;
	public Draw_ParOption getCompiled() {
		Draw_ParAlignBlock a = new Draw_ParAlignBlock();
		a.align = this.align;
		return a;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphAlignContent extends ParagraphOption {
	@nodeAttr public SyntaxAlignType align;
	public Draw_ParOption getCompiled() {
		Draw_ParAlignContent a = new Draw_ParAlignContent();
		a.align = this.align;
		return a;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphSize extends ParagraphOption {
	@nodeAttr public SyntaxSize min_width;
	@nodeAttr public SyntaxSize max_width;
	@nodeAttr public SyntaxSize min_height;
	@nodeAttr public SyntaxSize max_height;
	public Draw_ParOption getCompiled() {
		Draw_ParSize sz = new Draw_ParSize();
		if (this.min_width != null)
			sz.min_width = this.min_width.getCompiled();
		if (this.max_width != null)
			sz.max_width = this.max_width.getCompiled();
		if (this.min_height != null)
			sz.min_height = this.min_height.getCompiled();
		if (this.max_height != null)
			sz.max_height = this.max_height.getCompiled();
		return sz;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphInset extends ParagraphOption {
	@nodeAttr public SyntaxSize top;
	@nodeAttr public SyntaxSize bottom;
	@nodeAttr public SyntaxSize left;
	@nodeAttr public SyntaxSize right;
	public Draw_ParOption getCompiled() {
		Draw_ParInset sz = new Draw_ParInset();
		if (this.top != null)
			sz.top = this.top.getCompiled();
		if (this.bottom != null)
			sz.bottom = this.bottom.getCompiled();
		if (this.left != null)
			sz.left = this.left.getCompiled();
		if (this.right != null)
			sz.right = this.right.getCompiled();
		return sz;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphNoIndent extends ParagraphOption {
	@nodeAttr
	public boolean for_next;		// disable indenting for parent (enclosing) or next (enclosed) paragraph
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	public SymbolRef<ParagraphLayout>∅			names;
	public Draw_ParOption getCompiled() {
		String[] names = new String[this.names.length];
		for (int i=0; i < names.length; i++) {
			String name = this.names[i].name;
			if (name == null)
				names[i] = "";
			else
				names[i] = name.intern();
		}
		if (for_next) {
			Draw_ParNoIndentIfNext ni = new Draw_ParNoIndentIfNext();
			ni.names = names;
			return ni;
		} else {
			Draw_ParNoIndentIfPrev ni = new Draw_ParNoIndentIfPrev();
			ni.names = names;
			return ni;
		}
	}
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphIndent extends ParagraphOption {
	@nodeAttr public SyntaxSize indent;
	@nodeAttr public SyntaxSize indent_next;
	public Draw_ParOption getCompiled() {
		Draw_ParIndent pi = new Draw_ParIndent();
		if (indent != null)
			pi.indent = indent.getCompiled();
		if (indent_next != null)
			pi.indent_next = indent_next.getCompiled();
		return pi;
	}
}

public enum ParagraphKind {
	HORIZONTAL,
	VERTICAL,
	FLOW,
	BLOCK_HORIZONTAL,
	BLOCK_VERTICAL,
	BLOCK_FLOW
}

@ThisIsANode(lang=SyntaxLang)
public final class ParagraphLayout extends DNode {
	@nodeAttr
	public ParagraphKind			flow;
	@nodeAttr
	public ParagraphOption∅		options;
	
	public ParagraphLayout() {}

	public Draw_Paragraph getCompiled() {
		Draw_Paragraph p = new Draw_Paragraph();
		p.name = sname;
		p.flow = flow;
		p.options = new Draw_ParOption[options.length];
		for (int i=0; i < p.options.length; i++) {
			p.options[i] = options[i].getCompiled();
		}
		return p;
	}
}


