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

import kiev.dump.DumpFactory;
import kiev.dump.XMLDumpFilter;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ThisIsANode(lang=SyntaxLang)
public abstract class ATextSyntax extends DNode implements GlobalDNodeContainer, ExportSerialized {

	@nodeAttr
	public ASTNode∅			members;
	
	@UnVersioned
	protected Draw_ATextSyntax compiled;

	public ATextSyntax() {}
	
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
	{
		path ?= this
	;
		path @= members
	}
	
	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		this.compiled = null;
		return super.preResolveIn(env, parent, slot);
	}

	public abstract Draw_ATextSyntax getCompiled();
	
	public void fillCompiled(Draw_ATextSyntax ts) {
		ts.q_name = this.qname();
		Vector<Draw_ATextSyntax> sub_syntax = new Vector<Draw_ATextSyntax>();
		foreach(ATextSyntax stx; this.members)
			sub_syntax.append(stx.getCompiled());
		ts.sub_syntax = sub_syntax.toArray();

		Vector<Draw_SyntaxNodeTemplate> node_templates = new Vector<Draw_SyntaxNodeTemplate>();
		foreach(SyntaxNodeTemplate snt; this.members; snt.sname != null && snt.sname != "" && snt.template != null) {
			node_templates.append(snt.getCompiled());
		}
		ts.node_templates = node_templates.toArray();

		Vector<Draw_SyntaxElemDecl> syntax_elements = new Vector<Draw_SyntaxElemDecl>();
		foreach(SyntaxElemDecl sed; this.members; sed.elem != null) {
			//if !(sed.rnode.dnode instanceof Struct)
			//	continue;
			//Struct s = (Struct)sed.rnode.dnode;
			//if (!VNodeUtils.isNodeKind(s))
			//	continue;
			syntax_elements.append(sed.getCompiled());
		}
		ts.declared_syntax_elements = syntax_elements.toArray();
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class TopLevelTextSyntax extends ATextSyntax {

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public ATextSyntax⇑		parent_syntax;

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public StyleSheet⇑		style_sheet;

	@nodeAttr
	public String			root_projection;

	public Draw_ATextSyntax getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_TopLevelTextSyntax();
		fillCompiled(compiled);
		if (parent_syntax.dnode != null)
			compiled.parent_syntax = parent_syntax.dnode.getCompiled();
		Vector<Draw_Style> styles = new Vector<Draw_Style>();
		foreach (ASTNode n; members) {
			//if (n instanceof DrawColor) colors.append(n.compile());
			//if (n instanceof DrawFont) fonts.append(n.compile());
			if (n instanceof SyntaxStyleDecl) styles.append(n.compile());
		}
		Draw_StyleSheet css = new Draw_StyleSheet();
		css.q_name = qname();
		css.super_styles = new Draw_StyleSheet[0];
		css.styles = styles.toArray();
		if (style_sheet.dnode != null)
			css.super_styles = (Draw_StyleSheet[])Arrays.append(css.super_styles, style_sheet.dnode.getCompiled());
		if (compiled.parent_syntax != null && compiled.parent_syntax.style_sheet != null)
			css.super_styles = (Draw_StyleSheet[])Arrays.append(css.super_styles, compiled.parent_syntax.style_sheet);
		compiled.style_sheet = css;
		((Draw_TopLevelTextSyntax)compiled).root_projection = root_projection;
		return compiled;
	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		this.compiled = null;
		if (Env.needResolving(parent_syntax)) {
			ResInfo<ATextSyntax> info = new ResInfo<ATextSyntax>(env,this,parent_syntax.name);
			if (!PassInfo.resolveNameR(this,info))
				Kiev.reportError(this,"Cannot resolve syntax '"+parent_syntax.name+"'");
			else if (parent_syntax.symbol != info.resolvedSymbol())
				parent_syntax.symbol = info.resolvedSymbol();
		}
		return super.preResolveIn(env, parent, slot);
	}
	
	public rule resolveNameR(ResInfo path)
		StyleSheet@ stl;
		ATextSyntax@ syn;
	{
		super.resolveNameR(path)
	;
		path.isSuperAllowed(),
		path.getPrevSlotName() != "style_sheet",
		stl ?= style_sheet.dnode,
		path.enterSuper() : path.leaveSuper(),
		stl.resolveNameR(path)
	;
		path.isSuperAllowed(),
		path.getPrevSlotName() != "parent_syntax",
		syn ?= parent_syntax.dnode,
		path.enterSuper() : path.leaveSuper(),
		syn.resolveNameR(path)
	}
	
}

@ThisIsANode(lang=SyntaxLang)
public final class TextSyntax extends ATextSyntax {

	public Draw_ATextSyntax getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_TextSyntax();
		fillCompiled(compiled);
		return compiled;
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
public final class SyntaxNodeTemplate extends DNode {
	public static final SyntaxNodeTemplate[] emptyArray = new SyntaxNodeTemplate[0];

	@nodeAttr public ASTNode				template;

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) { return false; }
	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) { return false; }
	public boolean preVerify(Env env, INode parent, AttrSlot slot) { return false; }

	public Draw_SyntaxNodeTemplate getCompiled() {
		Draw_SyntaxNodeTemplate templ = new Draw_SyntaxNodeTemplate();
		templ.name = this.sname;
		if (template != null) {
			templ.template_node = template;
			templ.dump = DumpFactory.getXMLDumper().serializeToXmlData(Env.getEnv(), new XMLDumpFilter("full"), new ANode[]{template});
		}
		return templ;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class PartialSyntaxElemDecl extends ASyntaxElemDecl {
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemDecl extends ASyntaxElemDecl {
	@nodeAttr
	final
	public DNode⇑					rnode;

	@nodeAttr
	public SyntaxFunc∅				funcs;
	
	public SyntaxElemDecl() {}

	public void preResolveOut(Env env, INode parent, AttrSlot slot) {
		super.preResolveOut(env, parent, slot);
		if (rnode.name == null)
			return;
		rnode.resolveSymbol(SeverError.Warning, fun (DNode dn)->boolean {
			return (dn instanceof Struct && VNodeUtils.isNodeKind((Struct)dn)) || (dn instanceof NodeDecl);
		});
	}
	
	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "rnode")
			return SymbolRef.autoCompleteSymbol(rnode, str, slot, fun (DNode n)->boolean {
				return (n instanceof Struct && VNodeUtils.isNodeKind((Struct)n)) || (n instanceof NodeDecl);
			});
		return super.resolveAutoComplete(str,slot);
	}

	public Draw_SyntaxElemDecl getCompiled() {
		Draw_SyntaxElemDecl dr_decl = new Draw_SyntaxElemDecl();
		dr_decl.elem = this.elem.getCompiled(dr_decl);
		DNode rndecl = this.rnode.dnode;
		if (rndecl instanceof SymadeNode)
			dr_decl.node_type_id = rndecl.makeNodeTypeInfo().getId();
		else if (rndecl instanceof NodeDecl)
			dr_decl.node_type_id = rndecl.makeNodeTypeInfo().getId();
		else if (this.rnode.name != null)
			dr_decl.node_type_id = this.rnode.name;
		Draw_SyntaxFunc[] funcs = new Draw_SyntaxFunc[this.funcs.length];
		for (int i=0; i < funcs.length; i++)
			funcs[i] = this.funcs[i].getCompiled();
		dr_decl.funcs = funcs;
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

	public void preResolveOut(Env env, INode parent, AttrSlot slot) {
		super.preResolveOut(env, parent, slot);
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
public class SyntaxTypeRef extends ASTNode {
	public static final SyntaxTypeRef[] emptyArray = new SyntaxTypeRef[0];

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public Struct⇑				clazz;
	
	@nodeAttr
	public SyntaxTypeRef∅		args; 

	public String getSignature() {
		Struct s = clazz.dnode;
		String sign = s.qname().replace('·','.');
		if (args.length > 0) {
			sign += "«";
			foreach (SyntaxTypeRef tr; args)
				sign += tr.getSignature() + ";";
			sign += "»";
		}
		return sign;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxExpectedType extends DNode {
	
	@nodeAttr public String				title;
	@nodeAttr public SyntaxTypeRef		type_ref;

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxExpectedTemplate extends DNode {
	
	@nodeAttr public String				title;
	@nodeAttr public SymbolRef∅			expected_types; // ASTNode-s or SyntaxExpectedTemplate-s or SyntaxExpectedType

	public void preResolveOut(Env env, INode parent, AttrSlot slot) {
		super.preResolveOut(env, parent, slot);
		foreach (SymbolRef sr; expected_types; Env.needResolving(sr))
			sr.resolveSymbol(SeverError.Error, fun (DNode dn)->boolean {
				return dn instanceof Struct && VNodeUtils.isNodeKind((Struct)dn)
					|| dn instanceof SyntaxExpectedTemplate
					|| dn instanceof SyntaxExpectedType
					;
			});
	}
	
	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "expected_types")
			return SymbolRef.autoCompleteSymbol(this, str, slot, fun (DNode n)->boolean {
				return n instanceof Struct && VNodeUtils.isNodeKind((Struct)n)
					|| n instanceof SyntaxExpectedTemplate
					|| n instanceof SyntaxExpectedType
					;
			});
		return super.resolveAutoComplete(str,slot);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemFormatDecl extends DNode {
	@nodeAttr
	public SpaceCmd∅				spaces;
	
	public SyntaxElemFormatDecl() {}
	public SyntaxElemFormatDecl(String name) {
		this.sname = name;
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
				ls.kind = si.kind;
				ls.unit = si.size.unit;
				ls.gfx_size = si.size.gfx_size;
				ls.txt_size = si.size.txt_size;
			} else {
				ls.name = sc.si.name;
				ls.kind = SP_SPACE;
				ls.unit = SyntaxSizeUnit.PIXELS;
				ls.txt_size = 1;
				ls.gfx_size = 4;
			}
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
			if (ls.kind == SP_BRK_LINE)
				lout.has_alt = true;
		}
		return lout;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class SyntaxFunc extends ASTNode {
	public static SyntaxFunc[] emptyArray = new SyntaxFunc[0];

	@nodeAttr public String				title;
	@nodeAttr public String				attr;

	public abstract Draw_SyntaxFunc getCompiled();

	private NodeTypeInfo getExpectedType() {
		INode p = parent();
		if (p instanceof SyntaxElemDecl) {
			DNode nd = ((SyntaxElemDecl)p).rnode.dnode;
			if (nd instanceof SymadeNode)
				return nd.makeNodeTypeInfo();
			if (nd instanceof NodeDecl)
				return nd.makeNodeTypeInfo();
		}
		return null;
	}

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "attr") {
			try {
				NodeTypeInfo nti = getExpectedType();
				if (nti == null)
					return null;
				AutoCompleteResult result = new AutoCompleteResult(false);
				foreach (AttrSlot a; nti.getAllAttributes(); a.name.startsWith(str))
					result.append(a.name, null, null, a.name);
				return result;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return super.resolveAutoComplete(str,slot);
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxFuncEval extends SyntaxFunc {

	@nodeAttr public String				act;

	public Draw_SyntaxFunc getCompiled() {
		Draw_FuncEval dr_func = new Draw_FuncEval();
		dr_func.title = this.title;
		if (this.attr != null)
			dr_func.attr = this.attr.intern();
		dr_func.act = this.act;
		return dr_func;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxFuncSetEnum extends SyntaxFunc {

	@nodeAttr public ConstStringExpr∅			names;

	public Draw_SyntaxFunc getCompiled() {
		Draw_FuncSetEnum dr_func = new Draw_FuncSetEnum();
		dr_func.title = this.title;
		if (this.attr != null)
			dr_func.attr = this.attr.intern();
		dr_func.names = new String[this.names.length];
		for (int i=0; i < this.names.length; i++)
			dr_func.names[i] = this.names[i].value;
		return dr_func;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxFuncNewByTemplate extends SyntaxFunc {

	@nodeAttr public SymbolRef∅			expected_types; // ASTNode-s or SyntaxExpectedTemplate-s or SyntaxExpectedType 

	public void preResolveOut(Env env, INode parent, AttrSlot slot) {
		super.preResolveOut(env, parent, slot);
		foreach (SymbolRef sr; expected_types; Env.needResolving(sr))
			sr.resolveSymbol(SeverError.Error, fun (DNode dn)->boolean {
				return dn instanceof Struct && VNodeUtils.isNodeKind((Struct)dn)
					|| dn instanceof SyntaxExpectedTemplate
					|| dn instanceof SyntaxExpectedType
					;
			});
	}
	
	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "expected_types")
			return SymbolRef.autoCompleteSymbol(this, str, slot, fun (DNode n)->boolean {
				return n instanceof Struct && VNodeUtils.isNodeKind((Struct)n)
					|| n instanceof SyntaxExpectedTemplate
					|| n instanceof SyntaxExpectedType
					;
			});
		return super.resolveAutoComplete(str,slot);
	}

	private ExpectedTypeInfo makeExpectedTypeInfo(SymbolRef sr) {
		if (sr.dnode instanceof Struct) {
			Struct s = (Struct)sr.dnode;
			ExpectedTypeInfo eti = new ExpectedTypeInfo();
			eti.title = s.sname;
			eti.signature = s.qname().replace('·','.');
			//eti.typeinfo = TypeInfo.makeTypeInfo(Class.forName(s.qname().replace('·','.')),null);
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
		else if (sr.dnode instanceof SyntaxExpectedType) {
			SyntaxExpectedType exp = (SyntaxExpectedType)sr.dnode;
			ExpectedTypeInfo eti = new ExpectedTypeInfo();
			eti.title = exp.title;
			eti.signature = exp.type_ref.getSignature();
			//eti.typeinfo = exp.type_ref.getTypeInfo();
			return eti;
		}
		return new ExpectedTypeInfo();
	}

	public Draw_SyntaxFunc getCompiled() {
		Draw_FuncNewByTemplate dr_func = new Draw_FuncNewByTemplate();
		dr_func.title = this.title;
		if (this.attr != null)
			dr_func.attr = this.attr.intern();
		dr_func.subtypes = new ExpectedTypeInfo[this.expected_types.length];
		for (int i=0; i < dr_func.subtypes.length; i++)
			dr_func.subtypes[i] = makeExpectedTypeInfo(this.expected_types[i]);
		return dr_func;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxFuncNewByFactory extends SyntaxFunc {

	@nodeAttr public String factory; 

	public Draw_SyntaxFunc getCompiled() {
		Draw_FuncNewByFactory dr_func = new Draw_FuncNewByFactory();
		dr_func.title = this.title;
		if (this.attr != null)
			dr_func.attr = this.attr.intern();
		dr_func.factory = this.factory;
		return dr_func;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class SyntaxElem extends ASTNode {
	public static final SyntaxElem[] emptyArray = new SyntaxElem[0];

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public SyntaxStyleDecl⇑						style;

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public SyntaxElemFormatDecl⇑				fmt;

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public ParagraphLayout⇑						par;

	public SyntaxElem() {}

	public abstract Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl);

	protected final ASyntaxElemDecl getSyntaxElemDecl() {
		INode p = parent();
		while (p != null && !(p instanceof ASyntaxElemDecl))
			p = p.parent();
		if (p instanceof ASyntaxElemDecl)
			return (ASyntaxElemDecl)p;
		return null;
	}

	public void fillCompiled(Draw_SyntaxElem dr_elem) {
		if (this.par != null && this.par.dnode != null)
			dr_elem.par = this.par.dnode.getCompiled();
		if (style.dnode != null)
			dr_elem.style_names = style.dnode.getNames();
		dr_elem.lout = compile();
	}

	private Draw_Layout compile() {
		SyntaxElemFormatDecl fmt = null;
		if (this.fmt != null)
			fmt = this.fmt.dnode;
		if (fmt == null) {
			try {
				ResInfo<SyntaxElemFormatDecl> info = new ResInfo<SyntaxElemFormatDecl>(Env.getEnv(),this,"fmt-default");
				if (PassInfo.resolveNameR(this,info)) {
					fmt = info.resolvedDNode();
					return fmt.compile();
				}
			} catch (Exception e) {}
			Draw_Layout dflt = new Draw_Layout();
			LayoutSpace sp = new LayoutSpace();
			sp.name = "sp";
			sp.unit = SyntaxSizeUnit.PIXELS;
			sp.txt_size = 1;
			sp.gfx_size = 4;
			dflt.spaces_before = new LayoutSpace[]{ sp };
			dflt.spaces_after  = new LayoutSpace[]{ sp };
			return dflt;
		}
		return fmt.compile();
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemRef extends SyntaxElem {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public ASyntaxElemDecl⇑			decl;
	
	@nodeAttr
	public String					text;

	public SyntaxElemRef() {}
	public SyntaxElemRef(ASyntaxElemDecl decl) {
		this.decl.symbol = decl.symbol;
	}
	
	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		return ((ASyntaxElemDecl)decl.dnode).elem.getCompiled(elem_decl);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxToken extends SyntaxElem {

	public static final SyntaxToken[] emptyArray = new SyntaxToken[0];

	@nodeAttr public String					text;

	@setter
	public void set$text(String value) {
		this.text = (value != null) ? value.intern() : null;
	}
	
	public SyntaxToken() {}
	public SyntaxToken(String text) {
		this.text = text;
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
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxIcon extends SyntaxElem {

	@nodeAttr public String					icon_name;

	@setter
	public void set$icon_name(String value) {
		this.icon_name = (value != null) ? value.intern() : null;
	}
	
	public SyntaxIcon() {}
	public SyntaxIcon(String icon_name) {
		this.icon_name = icon_name;
	}
	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxIcon dr_elem = new Draw_SyntaxIcon(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxIcon dr_elem = (Draw_SyntaxIcon)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.icon = new Draw_Icon(icon_name);
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
	}

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "attr_name") {
			try {
				NodeTypeInfo nti = getExpectedType();
				if (nti == null)
					return null;
				AutoCompleteResult result = new AutoCompleteResult(false);
				foreach (AttrSlot a; nti.getAllAttributes(); a.name.startsWith(str))
					result.append(a.name, null, null, a.name);
				return result;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return super.resolveAutoComplete(str,slot);
	}

	public NodeTypeInfo getExpectedType() {
		INode p = parent();
		while (p != null && !(p instanceof ASyntaxElemDecl))
			p = p.parent();
		if (p instanceof SyntaxElemDecl) {
			DNode nd = ((SyntaxElemDecl)p).rnode.dnode;
			if (nd instanceof SymadeNode)
				return nd.makeNodeTypeInfo();
			if (nd instanceof NodeDecl)
				return nd.makeNodeTypeInfo();
		}
		return null;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxAttr extends SyntaxElem {
	public static final SyntaxAttr[] emptyArray = new SyntaxAttr[0];

	@nodeAttr
	public String							name;
	
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve @AttrXMLDumpInfo(attr=true)
	final
	public ATextSyntax⇑						in_syntax;
	
	@nodeAttr
	public SyntaxElem						prefix;
	@nodeAttr
	public SyntaxElem						sufix;
	@nodeAttr
	public SyntaxElem						empty;
	
	@nodeAttr
	public SyntaxAttrFormat					format;
	
	@setter
	public final void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public SyntaxAttr() {}

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "name" || slot.name == "ident_or_symbol_or_type") {
			try {
				NodeTypeInfo nti = getExpectedType();
				if (nti == null)
					return null;
				AutoCompleteResult result = new AutoCompleteResult(false);
				foreach (AttrSlot a; nti.getAllAttributes(); a.name.startsWith(str))
					result.append(a.name, null, null, a.name);
				return result;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return super.resolveAutoComplete(str,slot);
	}
	
	private AttrSlot findNodeAttr(NodeTypeInfo nti) {
		foreach (AttrSlot a; nti.getAllAttributes(); name.equals(a.name))
			return a;
		return null;
	}
	
	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		if (name == null || name.length() == 0) {
			Kiev.reportError(this, "Attribute name is empty");
			return false;
		}
		ASyntaxElemDecl sed = getSyntaxElemDecl();
		String sed_name = sed == null ? null : sed.sname;
		NodeTypeInfo nti = getExpectedType();
		if (nti == null) {
			//Kiev.reportWarning(this, "Cannot find node type for attribute: "+name+" in syntax decl "+sed_name);
			return true;
		}
		AttrSlot na = findNodeAttr(nti);
		if (na == null) {
			;//Kiev.reportWarning(this, "Cannot find node attribute: "+name+" in node "+nd.qname()+" in syntax decl "+sed_name);
		} else {
			if (na instanceof ASpaceAttrSlot) {
				if (format == null)
					format = new SyntaxList();
				//else if !(format instanceof SyntaxList)
				//	Kiev.reportWarning(this, "Space attribute: "+name+" in node "+nd.qname()+" showed not as list in syntax decl "+sed_name);
			}
		}
		if (format instanceof SyntaxList) {
			if (this.prefix == null && this.sufix == null && this.empty == null) {
				SyntaxPlaceHolder ph = new SyntaxPlaceHolder();
				if (this.name != null)
					ph.text = "<"+name+">";
				else
					ph.text = "<list>";
				this.empty = ph;
			}
		}
		return true;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxAttr dr_elem = (Draw_SyntaxAttr)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.name = this.name;
		//dr_elem.attr_slot = this.attr_slot;
		if (this.in_syntax.dnode != null)
			dr_elem.in_syntax = this.in_syntax.dnode.getCompiled();
		if (this.prefix != null)
			dr_elem.prefix = this.prefix.getCompiled(dr_elem.elem_decl);
		if (this.sufix != null)
			dr_elem.sufix = this.sufix.getCompiled(dr_elem.elem_decl);
		if (this.empty != null)
			dr_elem.empty = this.empty.getCompiled(dr_elem.elem_decl);
	}
	
	public NodeTypeInfo getExpectedType() {
		INode p = parent();
		while (p != null && !(p instanceof ASyntaxElemDecl))
			p = p.parent();
		if (p instanceof SyntaxElemDecl) {
			DNode nd = ((SyntaxElemDecl)p).rnode.dnode;
			if (nd instanceof SymadeNode)
				return nd.makeNodeTypeInfo();
			if (nd instanceof NodeDecl)
				return nd.makeNodeTypeInfo();
		}
		return null;
	}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxAttr dr_elem = null;
		if (format instanceof SyntaxList) {
			SyntaxList slst = (SyntaxList)format;
			dr_elem = new Draw_SyntaxList(elem_decl);
			fillCompiled(dr_elem);
			format.fillCompiled(dr_elem);
			if (slst.par != null && slst.par.dnode != null)
				return new Draw_SyntaxElemWrapper(dr_elem, slst.par.dnode.getCompiled());
		}
		else if (format instanceof SyntaxTreeBranch) {
			dr_elem = new Draw_SyntaxTreeBranch(elem_decl);
			fillCompiled(dr_elem);
			format.fillCompiled(dr_elem);
		}
		else if (format instanceof SyntaxTokenAttr) {
			dr_elem = new Draw_SyntaxTokenAttr(elem_decl);
			fillCompiled(dr_elem);
			format.fillCompiled(dr_elem);
		}
		else if (format instanceof SyntaxIdentAttr) {
			dr_elem = new Draw_SyntaxIdentAttr(elem_decl);
			fillCompiled(dr_elem);
			format.fillCompiled(dr_elem);
		}
		else if (format instanceof SyntaxCharAttr) {
			dr_elem = new Draw_SyntaxCharAttr(elem_decl);
			fillCompiled(dr_elem);
		}
		else if (format instanceof SyntaxStrAttr) {
			dr_elem = new Draw_SyntaxStrAttr(elem_decl);
			fillCompiled(dr_elem);
		}
		else if (format instanceof SyntaxEnumChoice) {
			dr_elem = new Draw_SyntaxEnumChoice(elem_decl);
			fillCompiled(dr_elem);
			format.fillCompiled(dr_elem);
		}
		else {
			dr_elem = new Draw_SyntaxSubAttr(elem_decl);
			fillCompiled(dr_elem);
		}
		if (this.prefix != null || this.sufix != null || this.empty != null)
			return new Draw_SyntaxElemWrapper(dr_elem);
		return dr_elem;
	}

}

@ThisIsANode(lang=SyntaxLang)
public abstract class SyntaxAttrFormat extends ASTNode {
	public void fillCompiled(Draw_SyntaxElem _dr_elem) {}
}
@ThisIsANode(lang=SyntaxLang)
public final class SyntaxSubAttr extends SyntaxAttrFormat {
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxList extends SyntaxAttrFormat {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public ParagraphLayout⇑						par;

	@nodeAttr
	public SyntaxElem							element;
	
	@nodeAttr
	public CalcOption							filter;

	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		if (element == null)
			element = new SyntaxNode();
		return true;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxList dr_elem = (Draw_SyntaxList)_dr_elem;
		if (this.element != null)
			dr_elem.element = this.element.getCompiled(dr_elem.elem_decl);
		else
			dr_elem.element = new Draw_SyntaxNode(dr_elem.elem_decl);
		if (this.filter != null)
			dr_elem.filter = this.filter.getCompiled();
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxTreeBranch extends SyntaxAttrFormat {
	@nodeAttr public SyntaxElem						folded;
	@nodeAttr public SyntaxElem						element;
	@nodeAttr public CalcOption						filter;

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxTreeBranch dr_elem = (Draw_SyntaxTreeBranch)_dr_elem;
		if (this.folded != null)
			dr_elem.folded = this.folded.getCompiled(dr_elem.elem_decl);
		if (this.element != null)
			dr_elem.element = this.element.getCompiled(dr_elem.elem_decl);
		if (this.filter != null)
			dr_elem.filter = this.filter.getCompiled();
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxTokenAttr extends SyntaxAttrFormat {
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxIdentAttr extends SyntaxAttrFormat {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve(sever=SeverError.Warning)
	final
	public SyntaxIdentTemplate⇑		decl;

	public SyntaxIdentAttr() {
		this.decl.name = "ident-template";
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxIdentAttr dr_elem = (Draw_SyntaxIdentAttr)_dr_elem;
		if (this.decl.dnode != null)
			dr_elem.template = this.decl.dnode.getCompiled();
	}

}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxCharAttr extends SyntaxAttrFormat {
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxStrAttr extends SyntaxAttrFormat {
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
		//dr_elem.nested_function_lookup = this.nested_function_lookup;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxNode extends SyntaxElem {

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public ATextSyntax⇑						in_syntax;
	
	@nodeAttr
	public SyntaxElem						prefix;
	@nodeAttr
	public SyntaxElem						sufix;
	@nodeAttr
	public SyntaxElem						empty;
	
	public SyntaxNode() {}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxNode dr_elem = (Draw_SyntaxNode)_dr_elem;
		super.fillCompiled(dr_elem);
		if (this.in_syntax.dnode != null)
			dr_elem.in_syntax = this.in_syntax.dnode.getCompiled();
		if (this.prefix != null)
			dr_elem.prefix = this.prefix.getCompiled(dr_elem.elem_decl);
		if (this.sufix != null)
			dr_elem.sufix = this.sufix.getCompiled(dr_elem.elem_decl);
		if (this.empty != null)
			dr_elem.empty = this.empty.getCompiled(dr_elem.elem_decl);
	}
	
	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxNode dr_elem = new Draw_SyntaxNode(elem_decl);
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
	public final void set$name(String value) {
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

	public void mainResolveOut(Env env, INode parent, AttrSlot slot) {
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
//		Draw_CalcOptionHasNoSyntaxParent c = new Draw_CalcOptionHasNoSyntaxParent();
//		c.name = this.name;
//		return c;
		return null;
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
//		Draw_CalcOptionIncludeInDump c = new Draw_CalcOptionIncludeInDump();
//		c.name = this.name;
//		c.dump = this.dump;
//		return c;
		return null;
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
public final class SyntaxEnumChoice extends SyntaxAttrFormat {
	@nodeAttr public SyntaxElem∅ elements;

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxEnumChoice dr_elem = (Draw_SyntaxEnumChoice)_dr_elem;
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

