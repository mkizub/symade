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
	
	protected Hashtable<String,SyntaxElem>		badSyntax = new Hashtable<Class,SyntaxElem>();
	protected Hashtable<String,SyntaxElemDecl>	allSyntax = new Hashtable<String,SyntaxElemDecl>();
	protected Hashtable<Pair<Operator,Class>, SyntaxElem> allSyntaxExprs = new Hashtable<Pair<Operator,Class>, SyntaxElem>();

	@nodeAttr public SymbolRef<ATextSyntax>	parent_syntax;
	@nodeAttr public ASTNode[]				members;
	@nodeAttr public ASTNode[]				auto_generated_members;
		 public String					q_name;	// qualified name

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

	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "sname")
			resetNames();
		else
			super.callbackChildChanged(attr);
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
	
	protected void cleanup() {
		auto_generated_members.delAll();
		allSyntax.clear();
		badSyntax.clear();
		allSyntaxExprs.clear();
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
	
	public void mainResolveOut() {
		this.cleanup();
		foreach(SyntaxElemDecl sed; this.members; sed.elem != null) {
			if !(sed.rnode.dnode instanceof Struct)
				continue;
			if (sed.elem == null)
				continue;
			Struct s = (Struct)sed.rnode.dnode;
			if !(s.isCompilerNode())
				continue;
			allSyntax.put(s.qname().replace('\u001f','.'), sed);
		}
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


	protected SyntaxSet expr(Operator op, SyntaxExpr sexpr)
	{
		SyntaxElem[] elems = new SyntaxElem[op.args.length];
		int earg = 0;
		for (int i=0; i < elems.length; i++) {
			OpArg arg = op.args[i];
			switch (arg) {
			case OpArg.EXPR(int priority):
				elems[i] = new SyntaxAutoParenth(sexpr.attrs[earg].ncopy(), priority, sexpr.template.dnode);
				earg++;
				continue;
			case OpArg.TYPE():
				elems[i] = new SyntaxAutoParenth(sexpr.attrs[earg].ncopy(), 255, sexpr.template.dnode);
				earg++;
				continue;
			case OpArg.OPER(String text):
				if (sexpr.template.dnode != null) {
					foreach (SyntaxToken t; sexpr.template.dnode.operators) {
						if (t.text == text) {
							elems[i] = t.ncopy();
							break;
						}
						if (t.text == "DEFAULT") {
							SyntaxToken st = t.ncopy();
							st.text = text;
							elems[i] = st;
						}
					}
				}
				if (elems[i] == null) {
					SyntaxToken st = new SyntaxToken(text);
					st.kind = SyntaxToken.TokenKind.OPERATOR;
					elems[i] = st;
				}
				continue;
			}
		}
		SyntaxSet set = new SyntaxSet();
		set.elements.addAll(elems);
		auto_generated_members.add(set);
		return set;
	}

	public SyntaxElem getSyntaxElem(ANode for_node) {
		if (for_node != null) {
			String cl_name = for_node.getClass().getName();
			SyntaxElemDecl sed = allSyntax.get(cl_name);
			if (sed != null)
				return sed.elem;
		}
		if (parent_syntax.dnode != null)
			return ((ATextSyntax)parent_syntax.dnode).getSyntaxElem(for_node);
		if (parent() instanceof ATextSyntax)
			return ((ATextSyntax)parent()).getSyntaxElem(for_node);
		SyntaxElem se;
		if (for_node == null) {
			se = badSyntax.get("<null>");
			if (se == null) {
				se = new SyntaxToken("(?null?)");
				auto_generated_members.add(se);
				badSyntax.put("<null>", se);
			}
		} else {
			ThisIsANode node_data = (ThisIsANode)for_node.getClass().getAnnotation(ThisIsANode.class);
			if (node_data != null) {
				Class lng_class = node_data.lang();
				if (lng_class != null && Language.class.isAssignableFrom(lng_class)) {
					Language lng = (Language)lng_class.getField(nameInstance).get(null);
					ATextSyntax stx = lng.getDefaultEditorSyntax();
					if (stx != this) {
						String text = lng.getClass().getName();
						SyntaxSwitch ssw = (SyntaxSwitch)badSyntax.get(text);
						if (ssw != null)
							return ssw;
						ssw = new SyntaxSwitch(
							new SyntaxToken("#lang\""+text+"\"{"),
							new SyntaxToken("}#"),
							stx
							);
						badSyntax.put(text, ssw);
						auto_generated_members.add(ssw);
						return ssw;
					}
				}
			}
			String cl_name = for_node.getClass().getName();
			se = badSyntax.get(cl_name);
			if (se == null) {
				se = new SyntaxToken("(?"+cl_name+"?)");
				auto_generated_members.add(se);
				badSyntax.put(cl_name, se);
			}
		}
		return se;
	}
}
@ThisIsANode(lang=SyntaxLang)
public final class TextSyntax extends ATextSyntax {
	@virtual typedef This  = TextSyntax;
	
	public TextSyntax() {
		this.sname = "<text-syntax>";
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

	public boolean enabled(Drawable dr) { return true; }

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
}

@ThisIsANode(lang=SyntaxLang)
public class ParagraphLayoutBlock extends AParagraphLayout {
	@virtual typedef This  = ParagraphLayoutBlock;

	@nodeAttr public String token_text;
	private String[] tokens;

	@setter
	public void set$token_text(String value) {
		if (value == null) {
			this.token_text = null;
			this.tokens = new String[0];
		} else {
			this.token_text = value.intern();
			this.tokens = value.split("\\s+");
			for (int i=0; i < this.tokens.length; i++)
				this.tokens[i] = this.tokens[i].intern();
		}
	}
	
	public boolean enabled(Drawable dr) {
		if (dr == null)
			return true;
		DrawTerm t = dr.getFirstLeaf();
		if (t instanceof DrawToken) {
			String str = ((SyntaxToken)t.syntax).text;
			foreach (String s; this.tokens; s == str)
				return false;
		}
		return true;
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

public final class LayoutSpace implements Cloneable {
	public static final LayoutSpace[] emptyArray = new LayoutSpace[0];
	public String		name;
	public int			from_attempt;
	public boolean		new_line;
	public boolean		eat;
	public int			text_size;
	public int			pixel_size;
	LayoutSpace setEat() {
		LayoutSpace ls = this.clone();
		ls.eat = true;
		return ls;
	}
}

public final class DrawLayout {

	private static final Font default_font = Font.decode("Dialog-PLAIN-12");
	
	public int				count;
	public Color 			color;
	public Font				font;
	public LayoutSpace[]	spaces_before;
	public LayoutSpace[]	spaces_after;

	public DrawLayout() {
		this.count = 0;
		this.color = Color.BLACK;
		this.font = default_font;
		this.spaces_before = LayoutSpace.emptyArray;
		this.spaces_after = LayoutSpace.emptyArray;
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
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxIdentTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxIdentTemplate;

	@nodeAttr public String				regexp_ok;
	@nodeAttr public String				esc_prefix;
	@nodeAttr public String				esc_suffix;
	@nodeAttr public ConstStringExpr[]	keywords;
	
	Pattern	pattern;

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
			pattern = Pattern.compile(regexp_ok);
		} catch (PatternSyntaxException e) {
			pattern = null;
			Kiev.reportError(this,"Syntax error in ident template pattern: "+regexp_ok);
		}
		foreach (ConstStringExpr cs; keywords; cs.value != null) {
			String interned = cs.value.intern();
			if (interned != cs.value)
				cs.value = interned;
		}
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxExpectedTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxExpectedTemplate;

	@nodeAttr public String			title;
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
	@nodeAttr public SymbolRef<DrawFont>		font;
	
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
	

	@UnVersioned
	public:r,r,r,rw DrawLayout		lout;

	public SyntaxElem() {}

	public abstract Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax);
	
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

	public void postVerify() {
		lout = null;
	}
	
	@getter
	public DrawLayout get$lout() {
		if (lout == null) {
			lout = new DrawLayout();
			compile();
		}
		return lout;
	}
	
	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
		if (curr_dr.syntax != this || expected_node != curr_dr.drnode)
			return false;
		return true;
	}
	
	private void compile() {
		DrawLayout lout = this.lout;
		lout.count = 0;
		SyntaxElemFormatDecl fmt = null;
		if (this.fmt != null)
			fmt = this.fmt.dnode;
		if (fmt == null) {
			SyntaxElemFormatDecl@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,"fmt-default")))
				return;
			fmt = d;
		}
		SpaceCmd[] spaces = fmt.spaces;
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
		if (fmt.color != null && fmt.color.dnode != null)
			lout.color = new Color(fmt.color.dnode.rgb_color);
		if (fmt.font != null && fmt.font.dnode != null)
			lout.font = Font.decode(fmt.font.dnode.font_name);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class SyntaxElemRef extends SyntaxElem {
	@virtual typedef This  = SyntaxElemRef;

	@nodeAttr public SymbolRef<ASyntaxElemDecl>		decl;
	@nodeAttr public String							text;

	public SyntaxElemRef() {
		this.decl = new SymbolRef<ASyntaxElemDecl>();
	}
	public SyntaxElemRef(ASyntaxElemDecl decl) {
		this.decl = new SymbolRef<ASyntaxElemDecl>(decl);
	}
	
	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
		return ((ASyntaxElemDecl)decl.dnode).elem.check(cont, curr_dr, expected_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		return ((ASyntaxElemDecl)decl.dnode).elem.makeDrawable(fmt,node,text_syntax);
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
	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawToken(node, this, text_syntax);
		return dr;
	}
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "kind" && kind == TokenKind.UNKNOWN)
			return false;
		return super.includeInDump(dump, attr, val);
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
	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawPlaceHolder(node, this, text_syntax);
		return dr;
	}
}

@ThisIsANode(lang=SyntaxLang)
public abstract class SyntaxAttr extends SyntaxElem {
	@virtual typedef This  ≤ SyntaxAttr;

	public static final SyntaxAttr[] emptyArray = new SyntaxAttr[0];

	@nodeAttr public String							name;
	@nodeAttr public SymbolRef<ATextSyntax>			in_syntax;
	@nodeAttr public SymbolRef[]						expected_types;
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

}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxSubAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxSubAttr;

	public SyntaxSubAttr() {}
	public SyntaxSubAttr(String name) {
		super(name);
	}
	public SyntaxSubAttr(String name, ATextSyntax stx) {
		super(name,stx);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		return new DrawSubAttr(node, this, text_syntax);
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
	@nodeAttr public boolean							folded_by_default;

	public SyntaxList() {}
	public SyntaxList(String name) {
		super(name);
		this.element = new SyntaxNode();
		this.folded = new SyntaxToken("{?"+name+"?}");
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		if (in_syntax.dnode != null)
			text_syntax = in_syntax.dnode;
		Drawable dr;
		if (text_syntax instanceof TreeSyntax || prefix == null && sufix == null && empty == null)
			dr = new DrawNonTermList(node, this, text_syntax);
		else
			dr = new DrawWrapList(node, this, text_syntax);
		return dr;
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

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawIdent(node, this, text_syntax, name);
		return dr;
	}

	public boolean isOk(String text) {
		SyntaxIdentTemplate t = getTemplate();
		if (t == null) return true;
		if (t.pattern != null && !t.pattern.matcher(text).matches())
			return false;
		foreach (ConstStringExpr cs; t.keywords; cs.value == text)
			return false;
		return true;
	}
	
	public SyntaxIdentTemplate getTemplate() {
		return (SyntaxIdentTemplate)decl.dnode;
	}
	
	public String getPrefix() {
		SyntaxIdentTemplate t = getTemplate();
		if (t == null || t.esc_prefix == null) return "";
		return t.esc_prefix;
	}	
	public String getSuffix() {
		SyntaxIdentTemplate t = getTemplate();
		if (t == null || t.esc_suffix == null) return "";
		return t.esc_suffix;
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
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxCharAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxCharAttr;

	public SyntaxCharAttr() {}
	public SyntaxCharAttr(String name) {
		super(name);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawCharTerm(node, this, text_syntax, name);
		return dr;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxStrAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxStrAttr;

	public SyntaxStrAttr() {}
	public SyntaxStrAttr(String name) {
		super(name);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawStrTerm(node, this, text_syntax, name);
		return dr;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxSet extends SyntaxElem {
	@virtual typedef This  ≤ SyntaxSet;

	@nodeAttr public SyntaxElem		folded;
	@nodeAttr public SyntaxElem[]	elements;
	@nodeAttr public boolean			folded_by_default;
	@nodeAttr public boolean			nested_function_lookup;

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawNonTermSet(node, this, text_syntax);
		return dr;
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

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		return new DrawNode(node, this, text_syntax);
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

	private ATextSyntax getTargetSyntax(ANode for_node) {
		ThisIsANode node_data = (ThisIsANode)for_node.getClass().getAnnotation(ThisIsANode.class);
		Class lng_class = node_data.lang();
		Language lng = (Language)lng_class.getField(nameInstance).get(null);
		return lng.getDefaultEditorSyntax();
	}
	
	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
		if (expected_node != curr_dr.drnode)
			return false;
		if!(curr_dr instanceof DrawSyntaxSwitch)
			return false;
		DrawSyntaxSwitch curr_dss = (DrawSyntaxSwitch)curr_dr;
		if (getTargetSyntax(expected_node) != target_syntax)
			return false;
		if (curr_dss.args.length == 0)
			return true;
		if (curr_dss.args.length != 3)
			return false;
		Drawable sub_dr = curr_dss.args[1];
		if (sub_dr.text_syntax != target_syntax)
			return false;
		SyntaxElem expected_stx = target_syntax.getSyntaxElem(expected_node);
		return expected_stx.check(cont,sub_dr,expected_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		return new DrawSyntaxSwitch(node,this,text_syntax);
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxSpace extends SyntaxElem {
	@virtual typedef This  = SyntaxSpace;

	public SyntaxSpace() {}

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawSpace(node, this, text_syntax);
		return dr;
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
	
	public abstract boolean calc(ANode node);
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionAnd extends CalcOption {
	@virtual typedef This  = CalcOptionAnd;

	@nodeAttr public CalcOption[] opts;
	
	public CalcOptionAnd() {}

	public boolean calc(ANode node) {
		foreach (CalcOption opt; opts; !opt.calc(node))
			return false;
		return true;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionOr extends CalcOption {
	@virtual typedef This  = CalcOptionOr;

	@nodeAttr public CalcOption[] opts;
	
	public CalcOptionOr() {}

	public boolean calc(ANode node) {
		foreach (CalcOption opt; opts; opt.calc(node))
			return true;
		return false;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionNot extends CalcOption {
	@virtual typedef This  = CalcOptionNot;

	@nodeAttr public CalcOption opt;
	
	public CalcOptionNot() {}

	public boolean calc(ANode node) {
		if (opt == null)
			return true;
		return !opt.calc(node);
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionNotNull extends CalcOption {
	@virtual typedef This  = CalcOptionNotNull;

	public CalcOptionNotNull() {}
	public CalcOptionNotNull(String name) {
		super(name);
	}

	public boolean calc(ANode node) {
		if (node == null)
			return false;
		Object obj = null;
		try { obj = node.getVal(name); } catch (RuntimeException e) {}
		if (obj == null)
			return false;
		if (obj instanceof SymbolRef && obj.name == null)
			return false;
		return true;
	}
}

@ThisIsANode(lang=SyntaxLang)
public final class CalcOptionNotEmpty implements CalcOption {
	@virtual typedef This  = CalcOptionNotEmpty;

	public CalcOptionNotEmpty() {}
	public CalcOptionNotEmpty(String name) {
		super(name);
	}

	public boolean calc(ANode node) {
		if (node == null)
			return false;
		Object obj = node.getVal(name);
		if (obj instanceof ANode[])
			return ((ANode[])obj).length > 0;
		return false;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionTrue implements CalcOption {
	@virtual typedef This  = CalcOptionTrue;

	public CalcOptionTrue() {}
	public CalcOptionTrue(String name) {
		super(name);
	}

	public boolean calc(ANode node) {
		if (node == null) return false;
		Object val = node.getVal(name);
		if (val == null || !(val instanceof Boolean)) return false;
		return ((Boolean)val).booleanValue();
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

	public boolean calc(ANode node) {
		if (clazz == null) return true;
		return clazz.isInstance(node);
	}

	public void mainResolveOut() {
		try {
			clazz = Class.forName(name);
		} catch (Throwable t) {
			Kiev.reportError(this, "Class '"+name+"' not found");
		}
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
	
	public boolean calc(ANode node) {
		if (node instanceof DNode) {
			DNode dn = (DNode)node;
			return (dn.getMeta(name) != null);
		}
		if (node instanceof DeclGroup) {
			DeclGroup dn = (DeclGroup)node;
			return (dn.getMeta(name) != null);
		}
		return false;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class CalcOptionIsHidden implements CalcOption {
	@virtual typedef This  = CalcOptionIsHidden;

	public CalcOptionIsHidden() {}
	public CalcOptionIsHidden(String name) {
		super(name);
	}

	public boolean calc(ANode node) {
		if (node == null)
			return false;
		Object val = node;
		if !(name == null || name == "" || name == "this")
			val = node.getVal(name);
		if !(val instanceof ASTNode)
			return false;
		return ((ASTNode)val).isAutoGenerated();
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

	public boolean calc(ANode node) {
		if (node == null)
			return false;
		String name = this.name;
		if (name == null || name == "" || name == "this") {
			return node.includeInDump(dump, ASTNode.nodeattr$this, node);
		}
		AttrSlot attr = null;
		foreach (AttrSlot a; node.values(); a.name == name) {
			attr = a;
			break;
		}
		if (attr == null)
			return false;
		Object val = attr.get(node);
		if (val == null)
			return false;
		if (attr.is_space && ((Object[])val).length == 0)
			return false;
		return node.includeInDump(dump, attr, val);
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxOptional extends SyntaxElem {
	@virtual typedef This  = SyntaxOptional;

	@nodeAttr public CalcOption	calculator;
	@nodeAttr public SyntaxElem	opt_true;
	@nodeAttr public SyntaxElem	opt_false;

	public SyntaxOptional() {}
	public SyntaxOptional(CalcOption calculator, SyntaxElem opt_true, SyntaxElem opt_false) {
		this.calculator = calculator;
		this.opt_true = opt_true;
		this.opt_false = opt_false;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawOptional(node, this, text_syntax);
		return dr;
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

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawEnumChoice(node, this, text_syntax);
		return dr;
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

	public Drawable makeDrawable(Formatter fmt, ANode node, ATextSyntax text_syntax) {
		Drawable dr = new DrawFolded(node, this, text_syntax);
		return dr;
	}
}

