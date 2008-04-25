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

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import syntax kiev.Syntax;

import java.awt.Color;
import java.awt.Font;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ThisIsANode(lang=SyntaxLang)
public abstract class ATextSyntax extends DNode implements ScopeOfNames, GlobalDNode {
	@virtual typedef This  ≤ ATextSyntax;
	
	@nodeAttr public SymbolRef<ATextSyntax>	parent_syntax;
	@nodeAttr public ASTNode[]					members;
	          public String						q_name;	// qualified name
			  
	@UnVersioned
	protected Draw_ATextSyntax compiled;

	public ATextSyntax() {
		this.parent_syntax = new SymbolRef<ATextSyntax>();
	}

	public String qname() {
		if (q_name != null)
			return q_name;
		ANode p = parent();
		if (p instanceof ATextSyntax)
			q_name = (p.qname()+"\u001f"+sname).intern();
		else if (p instanceof NameSpace)
			q_name = (p.srpkg.name+"\u001f"+sname).intern();
		else
			q_name = sname;
		return q_name;
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "sname")
			resetNames();
		super.callbackChildChanged(ct, attr, data);
	}
	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic()) {
			resetNames();
			if (parent() instanceof NameSpace) {
				NameSpace fu = (NameSpace)parent();
				int idx = fu.getPackage().sub_decls.indexOf(this);
				if (idx < 0)
					fu.getPackage().sub_decls.add(this);
			}
		}
		super.callbackAttached(pi);
	}
	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic()) {
			resetNames();
			if (parent() instanceof NameSpace) {
				NameSpace fu = (NameSpace)parent();
				int idx = fu.getPackage().sub_decls.indexOf(this);
				if (idx >= 0)
					fu.getPackage().sub_decls.del(idx);
			}
		}
		super.callbackDetached(parent, slot);
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "auto_generated_members")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	private void resetNames() {
		q_name = null;
		if (members != null) {
			foreach (ATextSyntax s; members)
				s.resetNames();
		}
	}
	
	public rule resolveNameR(ASTNode@ node, ResInfo path)
		ATextSyntax@ syn;
	{
		path.checkNodeName(this),
		node ?= this
	;
		node @= members,
		path.checkNodeName(node)
	;
		path.isSuperAllowed(),
		parent_syntax != null && parent_syntax.dnode != null,
		path.space_prev == null || (path.space_prev.pslot().name != "parent_syntax"),
		syn ?= parent_syntax.dnode,
		path.enterSuper() : path.leaveSuper(),
		syn.resolveNameR(node,path)
	}
	
	public boolean preResolveIn() {
		if (parent_syntax.name != null && parent_syntax.name != "") {
			ATextSyntax@ ts;
			if (!PassInfo.resolveNameR(this,ts,new ResInfo(this,parent_syntax.name)))
				Kiev.reportError(this,"Cannot resolve syntax '"+parent_syntax.name+"'");
			else if (parent_syntax.symbol != ts)
				parent_syntax.symbol = ts;
		}
		return true;
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "parent_syntax") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<ATextSyntax> vect = new Vector<ATextSyntax>();
			ATextSyntax@ ts;
			foreach (PassInfo.resolveNameR(this,ts,info))
				if (!vect.contains(ts)) vect.append(ts);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
	
	public abstract Draw_ATextSyntax getCompiled();
	
	public void fillCompiled(Draw_ATextSyntax ts) {
		Vector<Draw_ATextSyntax> sub_syntax = new Vector<Draw_ATextSyntax>();
		foreach(ATextSyntax stx; this.members)
			sub_syntax.append(stx.getCompiled());
		ts.sub_syntax = sub_syntax.toArray();
		foreach(SyntaxElemDecl sed; this.members; sed.elem != null) {
			if !(sed.rnode.dnode instanceof Struct)
				continue;
			if (sed.elem == null)
				continue;
			Struct s = (Struct)sed.rnode.dnode;
			if !(s.isCompilerNode())
				continue;
			ts.allSyntax.put(s.qname().replace('\u001f','.'), sed.getCompiled());
		}
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class TextSyntax extends ATextSyntax {
	@virtual typedef This  = TextSyntax;
	
	public TextSyntax() {
		this.sname = "<text-syntax>";
	}

	public Draw_ATextSyntax getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_TextSyntax();
		fillCompiled(compiled);
		return compiled;
	}
	
}

public enum SpaceKind {
	SP_SPACE,
	SP_NEW_LINE
}

public enum SpaceAction {
	SP_NOP,
	SP_ADD,
	SP_EAT
}

@ThisIsANode(lang=SyntaxLang)
public class SpaceInfo extends DNode {
	@virtual typedef This  = SpaceInfo;

	@nodeAttr SpaceKind		kind;
	@nodeAttr int			text_size;
	@nodeAttr int			pixel_size;
	
	public SpaceInfo() {}
	public SpaceInfo(String name, SpaceKind kind, int text_size, int pixel_size) {
		this.sname = name;
		this.kind = kind;
		this.text_size = text_size;
		this.pixel_size = pixel_size;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SpaceCmd extends ASTNode {
	@virtual typedef This  = SpaceCmd;

	public static final SpaceCmd[] emptyArray = new SpaceCmd[0];

	@nodeAttr public SymbolRef<SpaceInfo>		si;
	@nodeAttr public SpaceAction					action_before;
	@nodeAttr public SpaceAction					action_after;
	@nodeAttr public int							from_attempt;
	
	public SpaceCmd() {
		si = new SymbolRef<SpaceInfo>();
		action_before = SP_NOP;
		action_after = SP_NOP;
	}
	public SpaceCmd(SpaceInfo si, SpaceAction action_before, SpaceAction action_after) {
		this(si, action_before, action_after, 0);
	}
	public SpaceCmd(SpaceInfo si, SpaceAction action_before, SpaceAction action_after, int from_attempt) {
		this.si = new SymbolRef<SpaceInfo>(si);
		this.from_attempt = from_attempt;
		this.action_before = action_before;
		this.action_after = action_after;
	}

	public void preResolveOut() {
		if (si.name == null)
			si.name = "sp";
		SpaceInfo@ spi;
		if (!PassInfo.resolveNameR(this,spi,new ResInfo(this,si.name)))
			Kiev.reportError(this,"Cannot resolve color '"+si.name+"'");
		else if (si.symbol != spi)
			si.symbol = spi;
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "si") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SpaceInfo> vect = new Vector<SpaceInfo>();
			SpaceInfo@ spi;
			foreach (PassInfo.resolveNameR(this,spi,info))
				if (!vect.contains(spi)) vect.append(spi);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

}

@ThisIsANode(lang=SyntaxLang)
public abstract class AParagraphLayout extends DNode {
	@virtual typedef This  ≤ AParagraphLayout;

	@nodeAttr int indent_text_size;
	@nodeAttr int indent_pixel_size;
	@nodeAttr int next_indent_text_size;
	@nodeAttr int next_indent_pixel_size;
	@nodeAttr boolean indent_from_current_position;
	@nodeAttr boolean align_right;
	@nodeAttr boolean flow;
	
	public AParagraphLayout() {}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "indent_text_size" ||
			attr.name == "indent_pixel_size" ||
			attr.name == "next_indent_text_size" ||
			attr.name == "next_indent_pixel_size"
		) {
			if (val instanceof Integer && val.intValue() == 0)
				return false;
		}
		if (attr.name == "indent_from_current_position" ||
			attr.name == "align_right" ||
			attr.name == "flow"
		) {
			if (val instanceof Boolean && !val.booleanValue())
				return false;
		}
		return super.includeInDump(dump, attr, val);
	}
	
	public abstract Draw_Paragraph getCompiled();

}

@ThisIsANode(lang=SyntaxLang)
public final class ParagraphLayout extends AParagraphLayout {
	@virtual typedef This  = ParagraphLayout;

	public ParagraphLayout() {}
	public ParagraphLayout(String name, int ind_txt, int ind_pix) {
		this.sname = name;
		this.indent_text_size = ind_txt;
		this.indent_pixel_size = ind_pix;
	}

	public Draw_Paragraph getCompiled() {
		Draw_Paragraph p = new Draw_Paragraph();
		p.indent_text_size = indent_text_size;
		p.indent_pixel_size = indent_pixel_size;
		p.next_indent_text_size = next_indent_text_size;
		p.next_indent_pixel_size = next_indent_pixel_size;
		p.indent_from_current_position = indent_from_current_position;
		p.flow = flow;
		return p;
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class ParagraphLayoutBlock extends AParagraphLayout {
	@virtual typedef This  = ParagraphLayoutBlock;

	@nodeAttr public String token_text;

	public Draw_Paragraph getCompiled() {
		Draw_ParagraphBlock p = new Draw_ParagraphBlock();
		p.indent_text_size = indent_text_size;
		p.indent_pixel_size = indent_pixel_size;
		p.next_indent_text_size = next_indent_text_size;
		p.next_indent_pixel_size = next_indent_pixel_size;
		p.indent_from_current_position = indent_from_current_position;
		p.flow = flow;
		if (this.token_text != null) {
			p.tokens = token_text.split("\\s+");
			for (int i=0; i < p.tokens.length; i++)
				p.tokens[i] = p.tokens[i].intern();
		}
		return p;
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class DrawColor extends DNode {
	@virtual typedef This  = DrawColor;

	@nodeAttr
	public int rgb_color;

	
	public DrawColor() {}
	public DrawColor(String name) {
		this.sname = name;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class DrawFont extends DNode {
	@virtual typedef This  = DrawFont;

	@nodeAttr
	public String font_name;

	public DrawFont() {}
	public DrawFont(String font_name) {
		this.sname = font_name;
		this.font_name = font_name;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class ASyntaxElemDecl extends DNode {
	@virtual typedef This  ≤ ASyntaxElemDecl;

	@nodeAttr public SyntaxElem				elem;

	public ASyntaxElemDecl() {}
	public ASyntaxElemDecl(SyntaxElem elem) {
		this.elem = elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class PartialSyntaxElemDecl extends ASyntaxElemDecl {
	@virtual typedef This  = PartialSyntaxElemDecl;
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemDecl extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxElemDecl;

	@nodeAttr public SymbolRef<Struct>		rnode;

	public SyntaxElemDecl() {
		this.rnode = new SymbolRef<Struct>();
	}
	public SyntaxElemDecl(Struct cls, SyntaxElem elem) {
		super(elem);
		this.rnode = new SymbolRef<Struct>((Symbol<Struct>)cls);
	}

	public void preResolveOut() {
		if (rnode == null)
			rnode = new SymbolRef<Struct>();
		if (rnode.name == null)
			rnode.name = "ASTNode";
		Struct@ s;
		if (!PassInfo.resolveNameR(this,s,new ResInfo(this,rnode.name)))
			Kiev.reportError(this,"Cannot resolve @node '"+rnode.name+"'");
		else if (!s.isCompilerNode())
			Kiev.reportError(this,"Resolved '"+rnode.name+"' is not @node");
		else if (rnode.symbol != s)
			rnode.symbol = s;
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "rnode") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<Struct> vect = new Vector<Struct>();
			Struct@ s;
			foreach (PassInfo.resolveNameR(this,s,info))
				if (s.isCompilerNode() && !vect.contains(s)) vect.append(s);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public Draw_SyntaxElemDecl getCompiled() {
		Draw_SyntaxElemDecl dr_decl = new Draw_SyntaxElemDecl();
		dr_decl.elem = this.elem.getCompiled();
		dr_decl.clazz_name = this.rnode.dnode.qname().replace('\u001f','.').intern();
		return dr_decl;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxIdentTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxIdentTemplate;

	@nodeAttr public String				regexp_ok;
	@nodeAttr public String				esc_prefix;
	@nodeAttr public String				esc_suffix;
	@nodeAttr public ConstStringExpr[]	keywords;
	
	@setter
	public void set$esc_prefix(String value) {
		this.esc_prefix = (value != null) ? value.intern() : null;
	}
	@setter
	public void set$esc_suffix(String value) {
		this.esc_suffix = (value != null) ? value.intern() : null;
	}

	public SyntaxIdentTemplate() {
		super(new SyntaxNode());
	}

	public void preResolveOut() {
		if (regexp_ok == null)
			regexp_ok = ".*";
		try {
			Pattern.compile(regexp_ok);
		} catch (PatternSyntaxException e) {
			Kiev.reportError(this,"Syntax error in ident template pattern: "+regexp_ok);
		}
		foreach (ConstStringExpr cs; keywords; cs.value != null) {
			String interned = cs.value.intern();
			if (interned != cs.value)
				cs.value = interned;
		}
	}

	public Draw_SyntaxIdentTemplate getCompiled() {
		Draw_SyntaxIdentTemplate dr_decl = new Draw_SyntaxIdentTemplate();
		dr_decl.regexp_ok = this.regexp_ok;
		dr_decl.esc_prefix = this.esc_prefix;
		dr_decl.esc_suffix = this.esc_suffix;
		dr_decl.keywords = new String[this.keywords.length];
		for (int i=0; i < dr_decl.keywords.length; i++)
			dr_decl.keywords[i] = this.keywords[i].value;
		return dr_decl;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxExpectedTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxExpectedTemplate;

	@nodeAttr public String				title;
	@nodeAttr public SymbolRef[]		expected_types; // ASTNode-s or SyntaxExpectedTemplate-s 

	public SyntaxExpectedTemplate() {
		super(new SyntaxNode());
	}

	public void preResolveOut() {
		foreach (SymbolRef sr; expected_types) {
			DNode@ dn;
			if (!PassInfo.resolveNameR(this,dn,new ResInfo(this,sr.name)))
				Kiev.reportError(this,"Cannot resolve @node '"+sr.name+"'");
			else if !(dn instanceof Struct && ((Struct)dn).isCompilerNode() || dn instanceof SyntaxExpectedTemplate)
				Kiev.reportError(this,"Resolved '"+sr.name+"' is not a @node or SyntaxExpectedTemplate");
			else if (sr.symbol != dn)
				sr.symbol = dn;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "expected_types") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<DNode> vect = new Vector<DNode>();
			DNode@ dn;
			foreach (PassInfo.resolveNameR(this,dn,info))
				if ((dn instanceof Struct && ((Struct)dn).isCompilerNode() || dn instanceof SyntaxExpectedTemplate) && !vect.contains(dn))
					vect.append(dn);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemFormatDecl extends DNode {
	@virtual typedef This  = SyntaxElemFormatDecl;

	@nodeAttr public SpaceCmd[]				spaces;
	@nodeAttr public SymbolRef<DrawColor>	color;
	@nodeAttr public SymbolRef<DrawFont>	font;
	
	public SyntaxElemFormatDecl() {
		this.sname = "fmt-";
	}
	public SyntaxElemFormatDecl(String name) {
		this.sname = name;
	}

	public void preResolveOut() {
		if (color != null && color.name != null && color.name != "") {
			DrawColor@ dc;
			if (!PassInfo.resolveNameR(this,dc,new ResInfo(this,color.name)))
				Kiev.reportError(this,"Cannot resolve color '"+color.name+"'");
			else if (color.symbol != dc)
				color.symbol = dc;
		}

		if (font != null && font.name != null && font.name != "") {
			DrawFont@ df;
			if (!PassInfo.resolveNameR(this,df,new ResInfo(this,font.name)))
				Kiev.reportError(this,"Cannot resolve font '"+font.name+"'");
			else if (font.symbol != df)
				font.symbol = df;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "color") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<DrawColor> vect = new Vector<DrawColor>();
			DrawColor@ dc;
			foreach (PassInfo.resolveNameR(this,dc,info))
				if (!vect.contains(dc)) vect.append(dc);
			return vect.toArray();
		}
		if (slot.name == "font") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<DrawFont> vect = new Vector<DrawFont>();
			DrawFont@ df;
			foreach (PassInfo.resolveNameR(this,df,info))
				if (!vect.contains(df)) vect.append(df);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public Draw_Layout compile() {
		Draw_Layout lout = new Draw_Layout();
		SpaceCmd[] spaces = this.spaces;
		for (int i=0; i < spaces.length; i++) {
			SpaceCmd sc = spaces[i];
			LayoutSpace ls = new LayoutSpace();
			if (sc.si.dnode != null) {
				SpaceInfo si = (SpaceInfo)sc.si.dnode;
				ls.name = si.sname;
				if (si.kind == SP_NEW_LINE) ls.new_line = true;
				ls.text_size = si.text_size;
				ls.pixel_size = si.pixel_size;
			} else {
				ls.name = sc.si.name;
				ls.text_size = 1;
				ls.pixel_size = 4;
			}
			ls.from_attempt = sc.from_attempt;
			switch (sc.action_before) {
			case SP_NOP: break;
			case SP_ADD: lout.spaces_before = (LayoutSpace[])Arrays.append(lout.spaces_before, ls); break;
			case SP_EAT: lout.spaces_before = (LayoutSpace[])Arrays.append(lout.spaces_before, ls.setEat()); break;
			}
			switch (sc.action_after) {
			case SP_NOP: break;
			case SP_ADD: lout.spaces_after = (LayoutSpace[])Arrays.append(lout.spaces_after, ls); break;
			case SP_EAT: lout.spaces_after = (LayoutSpace[])Arrays.append(lout.spaces_after, ls.setEat()); break;
			}
			lout.count = Math.max(lout.count, sc.from_attempt);
		}
		if (this.color != null && this.color.dnode != null)
			lout.color = new Color(this.color.dnode.rgb_color);
		if (this.font != null && this.font.dnode != null)
			lout.font = Font.decode(this.font.dnode.font_name);
		return lout;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxFunction extends ASTNode {
	@virtual typedef This  = SyntaxFunction;

	public static SyntaxFunction[] emptyArray = new SyntaxFunction[0];

	@nodeAttr public String				title;
	@nodeAttr public String				act;
	@nodeAttr public String				attr;

	public SyntaxFunction() {
		act = "<nop>"; //SyntaxFuncActions.FuncNop;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxFunctions extends ASTNode {
	@virtual typedef This  = SyntaxFunctions;

	@nodeAttr public SyntaxFunction[]	funcs;

	public SyntaxFunctions() {}
	
	public Draw_SyntaxFunction[] getCompiled() {
		Draw_SyntaxFunction[] funcs = new Draw_SyntaxFunction[this.funcs.length];
		for (int i=0; i < funcs.length; i++) {
			SyntaxFunction sf = this.funcs[i];
			Draw_SyntaxFunction f = new Draw_SyntaxFunction();
			f.title = sf.title;
			f.act = sf.act;
			f.attr = sf.attr;
			funcs[i] = f;
		}
		return funcs;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class SyntaxElem extends ASTNode {
	@virtual typedef This  ≤ SyntaxElem;

	public static final SyntaxElem[] emptyArray = new SyntaxElem[0];

	@nodeAttr
	public SymbolRef<SyntaxElemFormatDecl>		fmt;
	@nodeAttr
	public SymbolRef<AParagraphLayout>			par;
	@nodeAttr(ext_data=true)
	public SyntaxFunctions						funcs;
	

	public SyntaxElem() {}

	public void preResolveOut() {
		if (fmt != null && fmt.name != null && fmt.name != "") {
			SyntaxElemFormatDecl@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,fmt.name)))
				Kiev.reportError(this,"Cannot resolve format declaration '"+fmt.name+"'");
			else if (fmt.symbol != d)
				fmt.symbol = d;
		}
		if (par != null && par.name != null && par.name != "") {
			AParagraphLayout@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,par.name)))
				Kiev.reportError(this,"Cannot resolve paragraph declaration '"+par.name+"'");
			else if (par.symbol != d)
				par.symbol = d;
		}
	}

	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "fmt") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxElemFormatDecl> vect = new Vector<SyntaxElemFormatDecl>();
			SyntaxElemFormatDecl@ dc;
			foreach (PassInfo.resolveNameR(this,dc,info))
				if (!vect.contains(dc)) vect.append(dc);
			return vect.toArray();
		}
		if (slot.name == "par" || slot.name == "elpar" ) {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<AParagraphLayout> vect = new Vector<AParagraphLayout>();
			AParagraphLayout@ dc;
			foreach (PassInfo.resolveNameR(this,dc,info))
				if (!vect.contains(dc)) vect.append(dc);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public abstract Draw_SyntaxElem getCompiled();

	public void fillCompiled(Draw_SyntaxElem dr_elem) {
		if (this.par != null && this.par.dnode != null)
			dr_elem.par = this.par.dnode.getCompiled();
		if (this.funcs != null)
			dr_elem.funcs = this.funcs.getCompiled();
		dr_elem.lout = compile();
	}

	private Draw_Layout compile() {
		SyntaxElemFormatDecl fmt = null;
		if (this.fmt != null)
			fmt = this.fmt.dnode;
		if (fmt == null) {
			SyntaxElemFormatDecl@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,"fmt-default")))
				return new Draw_Layout();
			fmt = d;
		}
		return fmt.compile();
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemRef extends SyntaxElem {
	@virtual typedef This  = SyntaxElemRef;

	@nodeAttr public SymbolRef<ASyntaxElemDecl>	decl;
	@nodeAttr public String							text;

	public SyntaxElemRef() {
		this.decl = new SymbolRef<ASyntaxElemDecl>();
	}
	public SyntaxElemRef(ASyntaxElemDecl decl) {
		this.decl = new SymbolRef<ASyntaxElemDecl>(decl);
	}
	
	public Draw_SyntaxElem getCompiled() {
		return ((ASyntaxElemDecl)decl.dnode).elem.getCompiled();
	}

//	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
//		return ((ASyntaxElemDecl)decl.dnode).elem.check(cont, curr_dr, expected_node);
//	}
//	
//	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
//		return ((ASyntaxElemDecl)decl.dnode).elem.makeDrawable(fmt,node,text_syntax);
//	}

	public void preResolveOut() {
		super.preResolveOut();
		if (decl.name != null && decl.name != "") {
			DNode@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,decl.name,ResInfo.noForwards)))
				Kiev.reportError(decl,"Unresolved syntax element decl "+decl);
			else if (decl.symbol != d)
				decl.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "decl") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<ASyntaxElemDecl> vect = new Vector<ASyntaxElemDecl>();
			ASyntaxElemDecl@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxToken extends SyntaxElem {
	@virtual typedef This  = SyntaxToken;

	public static enum TokenKind {
		UNKNOWN, KEYWORD, OPERATOR, SEPARATOR
	}

	public static final SyntaxToken[] emptyArray = new SyntaxToken[0];

	@nodeAttr public String					text;
	@nodeAttr public TokenKind				kind;

	@setter
	public void set$text(String value) {
		this.text = (value != null) ? value.intern() : null;
	}
	
	public SyntaxToken() {
		this.kind = TokenKind.UNKNOWN;
	}
	public SyntaxToken(String text) {
		this.text = text;
		this.kind = TokenKind.UNKNOWN;
	}
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "kind" && kind == TokenKind.UNKNOWN)
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxToken dr_elem = new Draw_SyntaxToken();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxToken dr_elem = (Draw_SyntaxToken)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.text = this.text;
		dr_elem.kind = this.kind;
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxPlaceHolder extends SyntaxElem {
	@virtual typedef This  = SyntaxPlaceHolder;

	@nodeAttr public String					text;

	@setter
	public void set$text(String value) {
		this.text = (value != null) ? value.intern() : null;
	}
	
	public SyntaxPlaceHolder() {}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxPlaceHolder dr_elem = new Draw_SyntaxPlaceHolder();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxPlaceHolder dr_elem = (Draw_SyntaxPlaceHolder)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.text = this.text;
	}

}

@ThisIsANode(lang=SyntaxLang)
public abstract class SyntaxAttr extends SyntaxElem {
	@virtual typedef This  ≤ SyntaxAttr;

	public static final SyntaxAttr[] emptyArray = new SyntaxAttr[0];

	@nodeAttr public String							name;
	@nodeAttr public SymbolRef<ATextSyntax>		in_syntax;
	@nodeAttr public SymbolRef[]					expected_types;
	@nodeAttr public SyntaxElem						empty;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public SyntaxAttr() {
		this.in_syntax = new SymbolRef<ATextSyntax>();
	}
	public SyntaxAttr(String name) {
		this.name = name;
		this.in_syntax = new SymbolRef<ATextSyntax>();
	}
	public SyntaxAttr(String name, ATextSyntax stx) {
		this.name = name;
		this.in_syntax = new SymbolRef<ATextSyntax>(stx);
	}

	public void preResolveOut() {
		super.preResolveOut();
		foreach (SymbolRef sr; expected_types; sr.name != null) {
			DNode@ dn;
			if( !PassInfo.resolveNameR(this,dn,new ResInfo(this,sr.name,ResInfo.noForwards)) ) {
				Kiev.reportError(sr,"Unresolved type "+sr);
				continue;
			}
			if !(dn instanceof Struct && ((Struct)dn).isCompilerNode() || dn instanceof SyntaxExpectedTemplate) {
				Kiev.reportError(sr,"Resolved type "+sr+" is not a compiler @node or SyntaxExpectedTemplate");
				continue;
			}
			if (sr.symbol != dn)
				sr.symbol = dn;
		}
		if (in_syntax.name != null && in_syntax.name != "") {
			ATextSyntax@ s;
			if (!PassInfo.resolveNameR(this,s,new ResInfo(this,in_syntax.name,ResInfo.noForwards)))
				Kiev.reportError(in_syntax,"Unresolved syntax "+in_syntax);
			else if (in_syntax.symbol != s)
				in_syntax.symbol = s;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "expected_types") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<DNode> vect = new Vector<DNode>();
			DNode@ dn;
			foreach (PassInfo.resolveNameR(this,dn,info))
				if ((dn instanceof Struct && ((Struct)dn).isCompilerNode() || dn instanceof SyntaxExpectedTemplate) && !vect.contains(dn))
					vect.append(dn);
			return vect.toArray();
		}
		if (slot.name == "in_syntax") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<ATextSyntax> vect = new Vector<ATextSyntax>();
			ATextSyntax@ s;
			foreach (PassInfo.resolveNameR(this,s,info))
				if (!vect.contains(s)) vect.append(s);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxAttr dr_elem = (Draw_SyntaxAttr)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.name = this.name;
		if (this.in_syntax.dnode != null)
			dr_elem.in_syntax = this.in_syntax.dnode.getCompiled();
		dr_elem.expected_types = this.expected_types;
		if (this.empty != null)
			dr_elem.empty = this.empty.getCompiled();
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxSubAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxSubAttr;

	public SyntaxSubAttr() {}
	public SyntaxSubAttr(String name) {
		super(name);
	}
	public SyntaxSubAttr(String name, ATextSyntax stx) {
		super(name,stx);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxSubAttr dr_elem = new Draw_SyntaxSubAttr();
		fillCompiled(dr_elem);
		return dr_elem;
	}

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxList extends SyntaxAttr {
	@virtual typedef This  = SyntaxList;

	@nodeAttr public SyntaxElem						folded;
	@nodeAttr public SyntaxElem						element;
	@nodeAttr public SyntaxElem						separator;
	@nodeAttr public SyntaxElem						prefix;
	@nodeAttr public SyntaxElem						sufix;
	@nodeAttr public CalcOption						filter;
	@nodeAttr public SymbolRef<AParagraphLayout>	elpar;
	@nodeAttr public boolean						folded_by_default;

	public SyntaxList() {}
	public SyntaxList(String name) {
		super(name);
		this.element = new SyntaxNode();
		this.folded = new SyntaxToken("{?"+name+"?}");
	}

	public void preResolveOut() {
		super.preResolveOut();
		if (elpar != null && elpar.name != null && elpar.name != "") {
			AParagraphLayout@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,elpar.name)))
				Kiev.reportError(this,"Cannot resolve paragraph declaration '"+elpar.name+"'");
			else if (elpar.symbol != d)
				elpar.symbol = d;
		}
	}	

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxList dr_elem = new Draw_SyntaxList();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxList dr_elem = (Draw_SyntaxList)_dr_elem;
		super.fillCompiled(dr_elem);
		if (this.folded != null)
			dr_elem.folded = this.folded.getCompiled();
		if (this.element != null)
			dr_elem.element = this.element.getCompiled();
		if (this.separator != null)
			dr_elem.separator = this.separator.getCompiled();
		if (this.prefix != null)
			dr_elem.prefix = this.prefix.getCompiled();
		if (this.sufix != null)
			dr_elem.sufix = this.sufix.getCompiled();
		if (this.filter != null)
			dr_elem.filter = this.filter.getCompiled();
		if (this.elpar != null && this.elpar.dnode != null)
			dr_elem.elpar = this.elpar.dnode.getCompiled();
		dr_elem.folded_by_default = this.folded_by_default;
	}

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxIdentAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxIdentAttr;

	@nodeAttr public SymbolRef<SyntaxIdentTemplate>		decl;

	public SyntaxIdentAttr() {
		this.decl = new SymbolRef<SyntaxIdentTemplate>(0,"ident-template");
	}
	public SyntaxIdentAttr(String name) {
		super(name);
		this.decl = new SymbolRef<SyntaxIdentTemplate>(0,"ident-template");
	}

	public void preResolveOut() {
		super.preResolveOut();
		if (decl.name != null && decl.name != "") {
			SyntaxIdentTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,decl.name,ResInfo.noForwards)))
				Kiev.reportWarning(decl,"Unresolved ident template "+decl);
			if (decl.symbol != d)
				decl.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "decl") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxIdentTemplate> vect = new Vector<SyntaxIdentTemplate>();
			SyntaxIdentTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxIdentAttr dr_elem = new Draw_SyntaxIdentAttr();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxIdentAttr dr_elem = (Draw_SyntaxIdentAttr)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.template = this.decl.dnode.getCompiled();
	}

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxCharAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxCharAttr;

	public SyntaxCharAttr() {}
	public SyntaxCharAttr(String name) {
		super(name);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxCharAttr dr_elem = new Draw_SyntaxCharAttr();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxStrAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxStrAttr;

	public SyntaxStrAttr() {}
	public SyntaxStrAttr(String name) {
		super(name);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxStrAttr dr_elem = new Draw_SyntaxStrAttr();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxSet extends SyntaxElem {
	@virtual typedef This  ≤ SyntaxSet;

	@nodeAttr public SyntaxElem		folded;
	@nodeAttr public SyntaxElem[]	elements;
	@nodeAttr public boolean		folded_by_default;
	@nodeAttr public boolean		nested_function_lookup;

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxSet dr_elem = new Draw_SyntaxSet();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxSet dr_elem = (Draw_SyntaxSet)_dr_elem;
		super.fillCompiled(dr_elem);
		if (this.folded != null)
			dr_elem.folded = this.folded.getCompiled();
		dr_elem.elements = new Draw_SyntaxElem[this.elements.length];
		for (int i=0; i < dr_elem.elements.length; i++)
			dr_elem.elements[i] = this.elements[i].getCompiled();
		dr_elem.folded_by_default = this.folded_by_default;
		dr_elem.nested_function_lookup = this.nested_function_lookup;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxNode extends SyntaxAttr {
	@virtual typedef This  = SyntaxNode;


	public SyntaxNode() {}
	public SyntaxNode(ATextSyntax stx) {
		super("");
		this.in_syntax.name = stx.sname;
		this.in_syntax.symbol = stx;
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxNode dr_elem = new Draw_SyntaxNode();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(copyable=false,lang=SyntaxLang)
public class SyntaxSwitch extends SyntaxElem {
	@virtual typedef This  = SyntaxSwitch;

	@nodeAttr SyntaxToken	prefix;
	@nodeData ATextSyntax	target_syntax;
	@nodeAttr SyntaxToken	suffix;
	
	SyntaxSwitch(SyntaxToken pr, SyntaxToken sf, ATextSyntax stx) {
		this.prefix = pr;
		this.suffix = sf;
		this.target_syntax = stx;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxSpace extends SyntaxElem {
	@virtual typedef This  = SyntaxSpace;

	public SyntaxSpace() {}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxSpace dr_elem = new Draw_SyntaxSpace();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class CalcOption extends ASTNode {
	@virtual typedef This  ≤ CalcOption;

	public static final CalcOption[] emptyArray = new CalcOption[0];

	@nodeAttr public String name;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public CalcOption() {}
	public CalcOption(String name) {
		if (name != null)
			this.name = name.intern();
	}
	
	public abstract Draw_CalcOption getCompiled();
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionAnd extends CalcOption {
	@virtual typedef This  = CalcOptionAnd;

	@nodeAttr public CalcOption[] opts;
	
	public CalcOptionAnd() {}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionAnd c = new Draw_CalcOptionAnd();
		c.opts = new Draw_CalcOption[this.opts.length];
		for (int i=0; i < c.opts.length; i++)
			c.opts[i] = this.opts[i].getCompiled();
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionOr extends CalcOption {
	@virtual typedef This  = CalcOptionOr;

	@nodeAttr public CalcOption[] opts;
	
	public CalcOptionOr() {}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionOr c = new Draw_CalcOptionOr();
		c.opts = new Draw_CalcOption[this.opts.length];
		for (int i=0; i < c.opts.length; i++)
			c.opts[i] = this.opts[i].getCompiled();
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionNot extends CalcOption {
	@virtual typedef This  = CalcOptionNot;

	@nodeAttr public CalcOption opt;
	
	public CalcOptionNot() {}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionNot c = new Draw_CalcOptionNot();
		c.opt = this.opt.getCompiled();
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionNotNull extends CalcOption {
	@virtual typedef This  = CalcOptionNotNull;

	public CalcOptionNotNull() {}
	public CalcOptionNotNull(String name) {
		super(name);
	}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionNotNull c = new Draw_CalcOptionNotNull();
		c.name = this.name;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionNotEmpty implements CalcOption {
	@virtual typedef This  = CalcOptionNotEmpty;

	public CalcOptionNotEmpty() {}
	public CalcOptionNotEmpty(String name) {
		super(name);
	}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionNotEmpty c = new Draw_CalcOptionNotEmpty();
		c.name = this.name;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionTrue implements CalcOption {
	@virtual typedef This  = CalcOptionTrue;

	public CalcOptionTrue() {}
	public CalcOptionTrue(String name) {
		super(name);
	}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionTrue c = new Draw_CalcOptionTrue();
		c.name = this.name;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionClass implements CalcOption {
	@virtual typedef This  = CalcOptionClass;

	private Class clazz;
	
	public CalcOptionClass() {}
	public CalcOptionClass(String name) {
		super(name);
	}

	public void mainResolveOut() {
		try {
			clazz = Class.forName(name);
		} catch (Throwable t) {
			Kiev.reportError(this, "Class '"+name+"' not found");
		}
	}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionClass c = new Draw_CalcOptionClass();
		c.clazz = this.clazz;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionHasMeta implements CalcOption {
	@virtual typedef This  = CalcOptionHasMeta;

	public CalcOptionHasMeta() {}
	public CalcOptionHasMeta(String name) {
		super(name);
	}

	@setter
	public void set$name(String value) {
		if (value != null && value.indexOf('.') >= 0)
			value = value.replace('.','\u001f');
		this.name = (value != null) ? value.intern() : null;
	}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionHasMeta c = new Draw_CalcOptionHasMeta();
		c.name = this.name;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionIsHidden implements CalcOption {
	@virtual typedef This  = CalcOptionIsHidden;

	public CalcOptionIsHidden() {}
	public CalcOptionIsHidden(String name) {
		super(name);
	}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionIsHidden c = new Draw_CalcOptionIsHidden();
		c.name = this.name;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionHasNoSyntaxParent implements CalcOption {
	@virtual typedef This  = CalcOptionHasNoSyntaxParent;

	public CalcOptionHasNoSyntaxParent() {}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionHasNoSyntaxParent c = new Draw_CalcOptionHasNoSyntaxParent();
		c.name = this.name;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionIncludeInDump implements CalcOption {
	@virtual typedef This  = CalcOptionIncludeInDump;

	@nodeAttr public String dump;

	@setter
	public void set$dump(String value) {
		this.dump = (value != null) ? value.intern() : null;
	}
	
	public CalcOptionIncludeInDump() {}
	public CalcOptionIncludeInDump(String dump, String name) {
		super(name);
		this.dump = dump;
	}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionIncludeInDump c = new Draw_CalcOptionIncludeInDump();
		c.name = this.name;
		c.dump = this.dump;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxOptional extends SyntaxElem {
	@virtual typedef This  = SyntaxOptional;

	@nodeAttr public CalcOption			calculator;
	@nodeAttr public SyntaxElem			opt_true;
	@nodeAttr public SyntaxElem			opt_false;

	public SyntaxOptional() {}
	public SyntaxOptional(CalcOption calculator, SyntaxElem opt_true, SyntaxElem opt_false) {
		this.calculator = calculator;
		this.opt_true = opt_true;
		this.opt_false = opt_false;
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxOptional dr_elem = new Draw_SyntaxOptional();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxOptional dr_elem = (Draw_SyntaxOptional)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.calculator = this.calculator.getCompiled();
		if (this.opt_true != null)
			dr_elem.opt_true = this.opt_true.getCompiled();
		if (this.opt_false != null)
			dr_elem.opt_false = this.opt_false.getCompiled();
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxEnumChoice extends SyntaxAttr {
	@virtual typedef This  = SyntaxEnumChoice;

	@nodeAttr public SyntaxElem[] elements;

	public SyntaxEnumChoice() {}
	public SyntaxEnumChoice(String name) {
		super(name);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxEnumChoice dr_elem = new Draw_SyntaxEnumChoice();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxEnumChoice dr_elem = (Draw_SyntaxEnumChoice)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.elements = new Draw_SyntaxElem[this.elements.length];
		for (int i=0; i < dr_elem.elements.length; i++)
			dr_elem.elements[i] = this.elements[i].getCompiled();
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxFolder extends SyntaxElem {
	@virtual typedef This  = SyntaxFolder;

	@nodeAttr public boolean folded_by_default;
	@nodeAttr public SyntaxElem folded;
	@nodeAttr public SyntaxElem unfolded;

	public SyntaxFolder() {}
	public SyntaxFolder(boolean folded_by_default, SyntaxElem folded, SyntaxElem unfolded) {
		this.folded_by_default = folded_by_default;
		this.folded = folded;
		this.unfolded = unfolded;
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxFolder dr_elem = new Draw_SyntaxFolder();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxFolder dr_elem = (Draw_SyntaxFolder)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.folded = this.folded.getCompiled();
		dr_elem.unfolded = this.unfolded.getCompiled();
		dr_elem.folded_by_default = this.folded_by_default;
	}
}

