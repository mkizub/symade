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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ThisIsANode(lang=SyntaxLang)
public abstract class ATextSyntax extends DNode implements ScopeOfNames, GlobalDNode, DumpSerialized {
	@nodeAttr public SymbolRef<ATextSyntax>	parent_syntax;
	@nodeAttr public ASTNode∅					members;
	          public String						q_name;	// qualified name
			  
	@UnVersioned
	protected Draw_ATextSyntax compiled;

	public ATextSyntax() {
		this.parent_syntax = new SymbolRef<ATextSyntax>();
	}
	
	public Object getDataToSerialize() {
		return this.getCompiled().init();
	}

	public String qname() {
		if (q_name != null)
			return q_name;
		ANode p = parent();
		if (p instanceof GlobalDNode)
			q_name = (((GlobalDNode)p).qname()+"\u001f"+sname).intern();
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
		if (pi.isSemantic())
			resetNames();
		super.callbackAttached(pi);
	}
	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic())
			resetNames();
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
		path.getPrevSlotName() != "parent_syntax",
		syn ?= parent_syntax.dnode,
		path.enterSuper() : path.leaveSuper(),
		syn.resolveNameR(node,path)
	}
	
	public boolean preResolveIn() {
		this.compiled = null;
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
		if (parent_syntax.dnode != null)
			ts.parent_syntax = parent_syntax.dnode.getCompiled();
		else if (parent() instanceof ATextSyntax)
			ts.parent_syntax = ((ATextSyntax)parent()).getCompiled();
		Vector<Draw_ATextSyntax> sub_syntax = new Vector<Draw_ATextSyntax>();
		foreach(ATextSyntax stx; this.members)
			sub_syntax.append(stx.getCompiled());
		ts.sub_syntax = sub_syntax.toArray();
		Vector<Draw_SyntaxElemDecl> syntax_elements = new Vector<Draw_SyntaxElemDecl>();
		foreach(SyntaxElemDecl sed; this.members; sed.elem != null) {
			if !(sed.rnode.dnode instanceof Struct)
				continue;
			if (sed.elem == null)
				continue;
			Struct s = (Struct)sed.rnode.dnode;
			if !(s.isCompilerNode())
				continue;
			syntax_elements.append(sed.getCompiled());
		}
		ts.declared_syntax_elements = syntax_elements.toArray();
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class TextSyntax extends ATextSyntax {
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

public enum ParagraphFlow {
	HORIZONTAL,
	VERTICAL,
	FLOW
}

@ThisIsANode(lang=SyntaxLang)
public abstract class AParagraphLayout extends DNode {
	@nodeAttr int indent_text_size;
	@nodeAttr int indent_pixel_size;
	@nodeAttr int next_indent_text_size;
	@nodeAttr int next_indent_pixel_size;
	@nodeAttr boolean indent_from_current_position;
	@nodeAttr ParagraphFlow flow;
	
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
		if (attr.name == "indent_from_current_position") {
			if (val instanceof Boolean && !val.booleanValue())
				return false;
		}
		return super.includeInDump(dump, attr, val);
	}
	
	public abstract Draw_Paragraph getCompiled();

}

@ThisIsANode(lang=SyntaxLang)
public final class ParagraphLayout extends AParagraphLayout {
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
	@nodeAttr
	public int rgb_color;

	
	public DrawColor() {}
	public DrawColor(String name) {
		this.sname = name;
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
}

@ThisIsANode(lang=SyntaxLang)
public abstract class ASyntaxElemDecl extends DNode {
	@nodeAttr public SyntaxElem				elem;

	public ASyntaxElemDecl() {}
	public ASyntaxElemDecl(SyntaxElem elem) {
		this.elem = elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class PartialSyntaxElemDecl extends ASyntaxElemDecl {
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemDecl extends ASyntaxElemDecl {
	@nodeAttr public SymbolRef<Struct>			rnode;
	@nodeAttr public SyntaxExpectedAttr∅		attr_types;

	public SyntaxElemDecl() {
		this.rnode = new SymbolRef<Struct>();
	}
	public SyntaxElemDecl(Struct cls, SyntaxElem elem) {
		super(elem);
		this.rnode = new SymbolRef<Struct>(cls);
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
			Kiev.reportWarning(this,"Resolved '"+rnode.name+"' is not @node");
		if (rnode.symbol != s)
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

	private ExpectedTypeInfo makeExpectedTypeInfo(SymbolRef sr) {
		if (sr.dnode instanceof Struct) {
			Struct s = (Struct)sr.dnode;
			ExpectedTypeInfo eti = new ExpectedTypeInfo();
			eti.title = s.sname;
			eti.typeinfo = TypeInfo.newTypeInfo(Class.forName(s.qname().replace('\u001f','.')),null);
			return eti;
		}
		else if (sr.dnode instanceof SyntaxExpectedTemplate) {
			SyntaxExpectedTemplate exp = (SyntaxExpectedTemplate)sr.dnode;
			ExpectedTypeInfo eti = new ExpectedTypeInfo();
			eti.title = exp.title;
			eti.subtypes = new ExpectedTypeInfo[exp.expected_types.length];
			for (int i=0; i < eti.subtypes.length; i++)
				eti.subtypes[i] = makeExpectedTypeInfo(exp.expected_types[i]);
			return eti;
		}
		return new ExpectedTypeInfo();
	}

	public Draw_SyntaxElemDecl getCompiled() {
		Draw_SyntaxElemDecl dr_decl = new Draw_SyntaxElemDecl();
		dr_decl.elem = this.elem.getCompiled(dr_decl);
		dr_decl.clazz_name = this.rnode.dnode.qname().replace('\u001f','.').intern();
		foreach (SyntaxExpectedAttr exp; attr_types) {
			ExpectedAttrTypeInfo eti = new ExpectedAttrTypeInfo();
			eti.title = exp.title;
			eti.attr_name = exp.attr_name;
			eti.subtypes = new ExpectedTypeInfo[exp.expected_types.length];
			for (int i=0; i < eti.subtypes.length; i++)
				eti.subtypes[i] = makeExpectedTypeInfo(exp.expected_types[i]);
			if (dr_decl.attr_types == null)
				dr_decl.attr_types = new ExpectedAttrTypeInfo[]{eti};
			else
				dr_decl.attr_types = (ExpectedAttrTypeInfo[])Arrays.append(dr_decl.attr_types, eti);
		}
		return dr_decl;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxIdentTemplate extends ASyntaxElemDecl {
	@nodeAttr public String				regexp_ok;
	@nodeAttr public String				esc_prefix;
	@nodeAttr public String				esc_suffix;
	@nodeAttr public ConstStringExpr∅	keywords;
	
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
	
	public static final SyntaxExpectedTemplate[] emptyArray = new SyntaxExpectedTemplate[0];
	
	@nodeAttr public String				title;
	@nodeAttr public String				attr_name;
	@nodeAttr public SymbolRef∅		expected_types; // ASTNode-s or SyntaxExpectedTemplate-s 

	@nodeData public AttrSlot			attr_slot;

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
	@nodeAttr public SpaceCmd∅				spaces;
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
			lout.rgb_color = this.color.dnode.rgb_color;
		if (this.font != null && this.font.dnode != null && this.font.dnode.font_name != null)
			lout.font_name = this.font.dnode.font_name.intern();
		return lout;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxFunction extends ASTNode {
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
	@nodeAttr public SyntaxFunction∅	funcs;

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
public class SyntaxExpectedAttr extends ASTNode {
	
	public static final SyntaxExpectedAttr[] emptyArray = new SyntaxExpectedAttr[0];
	
	@nodeAttr public String				title;
	@nodeAttr public String				attr_name;
	@nodeAttr public SymbolRef∅		expected_types; // ASTNode-s or SyntaxExpectedTemplate-s 

	@nodeData public AttrSlot			attr_slot;

	public SyntaxExpectedAttr() {}

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
public abstract class SyntaxElem extends ASTNode {
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
		if (slot.name == "par") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<AParagraphLayout> vect = new Vector<AParagraphLayout>();
			AParagraphLayout@ dc;
			foreach (PassInfo.resolveNameR(this,dc,info))
				if (!vect.contains(dc)) vect.append(dc);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public abstract Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl);

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
	@nodeAttr public SymbolRef<ASyntaxElemDecl>	decl;
	@nodeAttr public String							text;

	public SyntaxElemRef() {
		this.decl = new SymbolRef<ASyntaxElemDecl>();
	}
	public SyntaxElemRef(ASyntaxElemDecl decl) {
		this.decl = new SymbolRef<ASyntaxElemDecl>(decl);
	}
	
	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		return ((ASyntaxElemDecl)decl.dnode).elem.getCompiled(elem_decl);
	}

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

public static enum SyntaxTokenKind {
	UNKNOWN, KEYWORD, OPERATOR, SEPARATOR
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxToken extends SyntaxElem {

	public static final SyntaxToken[] emptyArray = new SyntaxToken[0];

	@nodeAttr public String					text;
	@nodeAttr public SyntaxTokenKind		kind;

	@setter
	public void set$text(String value) {
		this.text = (value != null) ? value.intern() : null;
	}
	
	public SyntaxToken() {
		this.kind = SyntaxTokenKind.UNKNOWN;
	}
	public SyntaxToken(String text) {
		this.text = text;
		this.kind = SyntaxTokenKind.UNKNOWN;
	}
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "kind" && kind == SyntaxTokenKind.UNKNOWN)
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxToken dr_elem = new Draw_SyntaxToken(elem_decl);
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
	@nodeAttr public String					text;
	@nodeAttr public String					attr_name;

	@setter
	public void set$text(String value) {
		this.text = (value != null) ? value.intern() : null;
	}
	
	@setter
	public void set$attr_name(String value) {
		this.attr_name = (value != null) ? value.intern() : null;
	}
	
	public SyntaxPlaceHolder() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxPlaceHolder dr_elem = new Draw_SyntaxPlaceHolder(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxPlaceHolder dr_elem = (Draw_SyntaxPlaceHolder)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.text = this.text;
		dr_elem.attr_name = this.attr_name;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class SyntaxAttr extends SyntaxElem {
	public static final SyntaxAttr[] emptyArray = new SyntaxAttr[0];

	@nodeAttr public String							name;
	@nodeAttr public SymbolRef<ATextSyntax>		in_syntax;
	@nodeAttr public SyntaxElem						empty;
	@nodeData public AttrSlot						attr_slot;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public SyntaxAttr() {
		this.in_syntax = new SymbolRef<ATextSyntax>();
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "attr_slot")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public void preResolveOut() {
		super.preResolveOut();
		if (in_syntax.name != null && in_syntax.name != "") {
			ATextSyntax@ s;
			if (!PassInfo.resolveNameR(this,s,new ResInfo(this,in_syntax.name,ResInfo.noForwards)))
				Kiev.reportError(in_syntax,"Unresolved syntax "+in_syntax);
			else if (in_syntax.symbol != s)
				in_syntax.symbol = s;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
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
		dr_elem.attr_slot = this.attr_slot;
		if (this.in_syntax.dnode != null)
			dr_elem.in_syntax = this.in_syntax.dnode.getCompiled();
		if (this.empty != null)
			dr_elem.empty = this.empty.getCompiled(dr_elem.elem_decl);
	}
	
	public Struct getExpectedType() {
		ANode p = parent();
		while (p != null && !(p instanceof SyntaxAttr || p instanceof ASyntaxElemDecl))
			p = p.parent();
		if (p instanceof SyntaxElemDecl)
			return ((SyntaxElemDecl)p).rnode.dnode;
		return null;
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxSubAttr extends SyntaxAttr {
	public SyntaxSubAttr() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxSubAttr dr_elem = new Draw_SyntaxSubAttr(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void mainResolveOut() {
		super.mainResolveOut();
		Struct s = getExpectedType();
		if (s != null) {
			Class cls = Class.forName(s.qname().replace('\u001f','.'));
			java.lang.reflect.Field fld = cls.getDeclaredField(nameEnumValuesFld);
			fld.setAccessible(true);
			AttrSlot[] slots = (AttrSlot[])fld.get(null);
			AttrSlot slot = null;
			foreach (AttrSlot s; slots; s.name == name) {
				slot = s;
				break;
			}
			if (slot != null)
				attr_slot = slot;
			else
				Kiev.reportWarning(this,"Cannot find attribute '"+name+"' in "+cls);
		}
	}
	
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxList extends SyntaxAttr {
	@nodeAttr public SyntaxElem						element;
	@nodeAttr public SyntaxElem						separator;
	@nodeAttr public SyntaxElem						prefix;
	@nodeAttr public SyntaxElem						sufix;
	@nodeAttr public CalcOption						filter;

	public SyntaxList() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxList dr_elem = new Draw_SyntaxList(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxList dr_elem = (Draw_SyntaxList)_dr_elem;
		super.fillCompiled(dr_elem);
		if (this.element != null)
			dr_elem.element = this.element.getCompiled(dr_elem.elem_decl);
		if (this.separator != null)
			dr_elem.separator = this.separator.getCompiled(dr_elem.elem_decl);
		if (this.prefix != null)
			dr_elem.prefix = this.prefix.getCompiled(dr_elem.elem_decl);
		if (this.sufix != null)
			dr_elem.sufix = this.sufix.getCompiled(dr_elem.elem_decl);
		if (this.filter != null)
			dr_elem.filter = this.filter.getCompiled();
	}

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxListWrapper extends SyntaxAttr {
	@nodeAttr public SyntaxElem						prefix;
	@nodeAttr public SyntaxElem						sufix;
	@nodeAttr public SyntaxList						list;

	public SyntaxListWrapper() {
		this.list = new SyntaxList();
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "list" && data instanceof SyntaxList) {
			this.name = ((SyntaxList)data).name;
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxListWrapper dr_elem = new Draw_SyntaxListWrapper(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxListWrapper dr_elem = (Draw_SyntaxListWrapper)_dr_elem;
		super.fillCompiled(dr_elem);
		if (this.prefix != null)
			dr_elem.prefix = this.prefix.getCompiled(dr_elem.elem_decl);
		if (this.sufix != null)
			dr_elem.sufix = this.sufix.getCompiled(dr_elem.elem_decl);
		if (this.list != null)
			dr_elem.list = (Draw_SyntaxList)this.list.getCompiled(dr_elem.elem_decl);
	}

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxTreeBranch extends SyntaxAttr {
	@nodeAttr public SyntaxElem						folded;
	@nodeAttr public SyntaxElem						element;
	@nodeAttr public CalcOption						filter;

	public SyntaxTreeBranch() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxTreeBranch dr_elem = new Draw_SyntaxTreeBranch(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxTreeBranch dr_elem = (Draw_SyntaxTreeBranch)_dr_elem;
		super.fillCompiled(dr_elem);
		if (this.folded != null)
			dr_elem.folded = this.folded.getCompiled(dr_elem.elem_decl);
		if (this.element != null)
			dr_elem.element = this.element.getCompiled(dr_elem.elem_decl);
		if (this.filter != null)
			dr_elem.filter = this.filter.getCompiled();
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxIdentAttr extends SyntaxAttr {
	@nodeAttr public SymbolRef<SyntaxIdentTemplate>		decl;

	public SyntaxIdentAttr() {
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

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxIdentAttr dr_elem = new Draw_SyntaxIdentAttr(elem_decl);
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
	public SyntaxCharAttr() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxCharAttr dr_elem = new Draw_SyntaxCharAttr(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxStrAttr extends SyntaxAttr {
	public SyntaxStrAttr() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxStrAttr dr_elem = new Draw_SyntaxStrAttr(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxXmlStrAttr extends SyntaxAttr {
	public SyntaxXmlStrAttr() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxXmlStrAttr dr_elem = new Draw_SyntaxXmlStrAttr(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}


@ThisIsANode(lang=SyntaxLang)
public class SyntaxSet extends SyntaxElem {
	@nodeAttr public SyntaxElem∅	elements;
	@nodeAttr public boolean		nested_function_lookup;

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxSet dr_elem = new Draw_SyntaxSet(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxSet dr_elem = (Draw_SyntaxSet)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.elements = new Draw_SyntaxElem[this.elements.length];
		for (int i=0; i < dr_elem.elements.length; i++)
			dr_elem.elements[i] = this.elements[i].getCompiled(dr_elem.elem_decl);
		dr_elem.nested_function_lookup = this.nested_function_lookup;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxNode extends SyntaxAttr {

	public SyntaxNode() {
		this.name = "";
		this.attr_slot = ANode.nodeattr$this;
	}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxNode dr_elem = new Draw_SyntaxNode(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(copyable=false,lang=SyntaxLang)
public class SyntaxSwitch extends SyntaxElem {
	@nodeAttr SyntaxToken	prefix;
	@nodeData ATextSyntax	target_syntax;
	@nodeAttr SyntaxToken	suffix;
	
	SyntaxSwitch(SyntaxToken pr, SyntaxToken sf, ATextSyntax stx) {
		this.prefix = pr;
		this.suffix = sf;
		this.target_syntax = stx;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxSwitch dr_elem = (Draw_SyntaxSwitch)_dr_elem;
		super.fillCompiled(dr_elem);
		if (this.prefix != null)
			dr_elem.prefix = (Draw_SyntaxToken)this.prefix.getCompiled(dr_elem.elem_decl);
		if (this.target_syntax != null)
			dr_elem.target_syntax = (Draw_ATextSyntax)this.target_syntax.getCompiled();
		if (this.suffix != null)
			dr_elem.suffix = (Draw_SyntaxToken)this.suffix.getCompiled(dr_elem.elem_decl);
	}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxSwitch dr_elem = new Draw_SyntaxSwitch(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxSpace extends SyntaxElem {
	public SyntaxSpace() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxSpace dr_elem = new Draw_SyntaxSpace(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class CalcOption extends ASTNode {
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
	@nodeAttr public CalcOption∅ opts;
	
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
	@nodeAttr public CalcOption∅ opts;
	
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
	public CalcOptionHasNoSyntaxParent() {}

	public Draw_CalcOption getCompiled() {
		Draw_CalcOptionHasNoSyntaxParent c = new Draw_CalcOptionHasNoSyntaxParent();
		c.name = this.name;
		return c;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionIncludeInDump implements CalcOption {
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
	@nodeAttr public CalcOption			calculator;
	@nodeAttr public SyntaxElem			opt_true;
	@nodeAttr public SyntaxElem			opt_false;

	public SyntaxOptional() {}
	public SyntaxOptional(CalcOption calculator, SyntaxElem opt_true, SyntaxElem opt_false) {
		this.calculator = calculator;
		this.opt_true = opt_true;
		this.opt_false = opt_false;
	}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxOptional dr_elem = new Draw_SyntaxOptional(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxOptional dr_elem = (Draw_SyntaxOptional)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.calculator = this.calculator.getCompiled();
		if (this.opt_true != null)
			dr_elem.opt_true = this.opt_true.getCompiled(dr_elem.elem_decl);
		if (this.opt_false != null)
			dr_elem.opt_false = this.opt_false.getCompiled(dr_elem.elem_decl);
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxEnumChoice extends SyntaxAttr {
	@nodeAttr public SyntaxElem∅ elements;

	public SyntaxEnumChoice() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxEnumChoice dr_elem = new Draw_SyntaxEnumChoice(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxEnumChoice dr_elem = (Draw_SyntaxEnumChoice)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.elements = new Draw_SyntaxElem[this.elements.length];
		for (int i=0; i < dr_elem.elements.length; i++)
			dr_elem.elements[i] = this.elements[i].getCompiled(dr_elem.elem_decl);
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxFolder extends SyntaxElem {
	@nodeAttr public boolean folded_by_default;
	@nodeAttr public SyntaxElem folded;
	@nodeAttr public SyntaxElem unfolded;

	public SyntaxFolder() {}
	public SyntaxFolder(boolean folded_by_default, SyntaxElem folded, SyntaxElem unfolded) {
		this.folded_by_default = folded_by_default;
		this.folded = folded;
		this.unfolded = unfolded;
	}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxFolder dr_elem = new Draw_SyntaxFolder(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxFolder dr_elem = (Draw_SyntaxFolder)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.folded = this.folded.getCompiled(dr_elem.elem_decl);
		dr_elem.unfolded = this.unfolded.getCompiled(dr_elem.elem_decl);
		dr_elem.folded_by_default = this.folded_by_default;
	}
}

