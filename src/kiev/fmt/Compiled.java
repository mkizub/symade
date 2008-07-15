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

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class LayoutSpace implements Cloneable, Serializable {
	public static final LayoutSpace[] emptyArray = new LayoutSpace[0];
	
	public String		name;
	public int			from_attempt;
	public boolean		new_line;
	public boolean		eat;
	public int			text_size;
	public int			pixel_size;

	LayoutSpace setEat() {
		LayoutSpace ls = (LayoutSpace)this.clone();
		ls.eat = true;
		return ls;
	}

	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}

public final class Draw_Layout implements Serializable {

	public int				count;
	public int				rgb_color;
	public String			font_name;
	public LayoutSpace[]	spaces_before;
	public LayoutSpace[]	spaces_after;

	public Draw_Layout() {
		this.count = 0;
		this.rgb_color = 0;
		this.font_name = "Dialog-PLAIN-12";
		this.spaces_before = LayoutSpace.emptyArray;
		this.spaces_after = LayoutSpace.emptyArray;
	}

	Object readResolve() throws ObjectStreamException {
		if (this.font_name != null) this.font_name = this.font_name.intern();
		return this;
	}
}

public class Draw_IndentInfo implements Serializable {
	public String name;
	public int text_size;
	public int pixel_size;
	public int next_text_size;
	public int next_pixel_size;
	public boolean from_current_position;
	
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}

public class Draw_Paragraph implements Serializable {
	public String name;
	public Draw_IndentInfo indent;
	public String[] no_indent_if_prev;
	public String[] no_indent_if_next;
	public ParagraphFlow flow;
	
	Object readResolve() throws ObjectStreamException {
		if (this.name != null)
			this.name = this.name.intern();
		if (this.no_indent_if_prev != null) {
			for (int i=0; i < this.no_indent_if_prev.length; i++) {
				String s = this.no_indent_if_prev[i];
				if (s == null)
					this.no_indent_if_prev[i] = "";
				else
					this.no_indent_if_prev[i] = s.intern();
			}
		}
		if (this.no_indent_if_next != null) {
			for (int i=0; i < this.no_indent_if_next.length; i++) {
				String s = this.no_indent_if_next[i];
				if (s == null)
					this.no_indent_if_next[i] = "";
				else
					this.no_indent_if_next[i] = s.intern();
			}
		}
		return this;
	}
}

public final class Draw_SyntaxFunction implements Serializable {
	public static final Draw_SyntaxFunction[] emptyArray = new Draw_SyntaxFunction[0];

	public String				title;
	public String				act;
	public String				attr;

	public Draw_SyntaxFunction() {
		act = "<nop>"; //SyntaxFuncActions.FuncNop;
	}

	Object readResolve() throws ObjectStreamException {
		if (this.title != null) this.title = this.title.intern();
		if (this.act != null) this.act = this.act.intern();
		if (this.attr != null) this.attr = this.attr.intern();
		return this;
	}
}

public final class ExpectedAttrTypeInfo implements Serializable {
	public static final ExpectedAttrTypeInfo[] emptyArray = new ExpectedAttrTypeInfo[0];

	public String				title;
	public String				attr_name;
	public ExpectedTypeInfo[]	subtypes;

	Object readResolve() throws ObjectStreamException {
		if (this.title != null) this.title = this.title.intern();
		if (this.attr_name != null) this.attr_name = this.attr_name.intern();
		return this;
	}
}

public final class ExpectedTypeInfo implements Serializable {
	public static final ExpectedTypeInfo[] emptyArray = new ExpectedTypeInfo[0];

	public String				title;
	public TypeInfo				typeinfo;
	public ExpectedTypeInfo[]	subtypes;

	Object readResolve() throws ObjectStreamException {
		if (this.title != null) this.title = this.title.intern();
		return this;
	}
}

public abstract class Draw_CalcOption implements Serializable {
	public static final Draw_CalcOption[] emptyArray = new Draw_CalcOption[0];
	public abstract boolean calc(ANode node);
}
public final class Draw_CalcOptionAnd extends Draw_CalcOption {
	public Draw_CalcOption[]				opts = Draw_CalcOption.emptyArray;

	public boolean calc(ANode node) {
		foreach (Draw_CalcOption opt; opts; !opt.calc(node))
			return false;
		return true;
	}
}
public final class Draw_CalcOptionOr extends Draw_CalcOption {
	public Draw_CalcOption[]				opts = Draw_CalcOption.emptyArray;

	public boolean calc(ANode node) {
		foreach (Draw_CalcOption opt; opts; opt.calc(node))
			return true;
		return false;
	}
}
public final class Draw_CalcOptionNot extends Draw_CalcOption {
	public Draw_CalcOption					opt;

	public boolean calc(ANode node) {
		if (opt == null)
			return true;
		return !opt.calc(node);
	}
}
public final class Draw_CalcOptionNotNull extends Draw_CalcOption {
	public String							name;

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
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
public final class Draw_CalcOptionNotEmpty extends Draw_CalcOption {
	public String							name;

	public boolean calc(ANode node) {
		if (node == null)
			return false;
		Object obj = node.getVal(name);
		if (obj instanceof ANode[])
			return ((ANode[])obj).length > 0;
		return false;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
public final class Draw_CalcOptionTrue extends Draw_CalcOption {
	public String							name;

	public boolean calc(ANode node) {
		if (node == null) return false;
		Object val = node.getVal(name);
		if (val == null || !(val instanceof Boolean)) return false;
		return ((Boolean)val).booleanValue();
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
public final class Draw_CalcOptionClass extends Draw_CalcOption {
	public Class							clazz;

	public boolean calc(ANode node) {
		if (clazz == null) return true;
		return clazz.isInstance(node);
	}
}
public final class Draw_CalcOptionHasMeta extends Draw_CalcOption {
	public String							name;
	
	public boolean calc(ANode node) {
		if (node instanceof DNode) {
			DNode dn = (DNode)node;
			return (dn.getMeta(name) != null);
		}
		return false;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
public final class Draw_CalcOptionIsHidden extends Draw_CalcOption {
	public String							name;

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
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
public final class Draw_CalcOptionHasNoSyntaxParent extends Draw_CalcOption {
	public String							name;

	public boolean calc(ANode node) {
		if (node == null)
			return true;
		Object val = node;
		if !(name == null || name == "" || name == "this")
			val = node.getVal(name);
		if !(val instanceof ANode)
			return true;
		ANode syntax_parent = ANode.nodeattr$syntax_parent.get((ANode)val);
		return (syntax_parent == null);
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
public final class Draw_CalcOptionIncludeInDump extends Draw_CalcOption {
	public String							name;
	public String							dump;

	public Draw_CalcOptionIncludeInDump() {}
	public Draw_CalcOptionIncludeInDump(String dump, String name) {
		this.dump = dump;
		this.name = name;
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
		if (attr instanceof SpaceAttrSlot) {
			ANode[] vals = attr.getArray(node);
			if (vals.length == 0)
				return false;
			return node.includeInDump(dump, attr, vals);
		}
		else if (attr instanceof ExtSpaceAttrSlot) {
			ExtChildrenIterator iter = attr.iterate(node);
			if (!iter.hasMoreElements())
				return false;
			return node.includeInDump(dump, attr, iter);
		}
		else if (attr instanceof ScalarAttrSlot) {
			Object val = attr.get(node);
			if (val == null)
				return false;
			return node.includeInDump(dump, attr, val);
		}
		return false;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		if (this.dump != null) this.dump = this.dump.intern();
		return this;
	}
}


public class Draw_SyntaxElem implements Serializable {
	public static final Draw_SyntaxElem[] emptyArray = new Draw_SyntaxElem[0];
	
	final
	public Draw_SyntaxElemDecl				elem_decl;
	public Draw_Paragraph					par;
	public Draw_SyntaxFunction[]			funcs = Draw_SyntaxFunction.emptyArray;
	public Draw_Layout						lout = new Draw_Layout();
	
	public Draw_SyntaxElem(Draw_SyntaxElemDecl elem_decl) {
		this.elem_decl = elem_decl;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		throw new AbstractMethodError(this.getClass()+".makeDrawable");
	}

	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
		if (curr_dr.syntax != this || expected_node != curr_dr.drnode)
			return false;
		return true;
	}
	
}

public class Draw_SyntaxToken extends Draw_SyntaxElem implements Cloneable {
	public String							text;
	public SyntaxTokenKind					kind;

	public Draw_SyntaxToken(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	public Draw_SyntaxToken(Draw_SyntaxElemDecl elem_decl, String text) {
		super(elem_decl);
		this.text = text;
		this.kind = SyntaxTokenKind.UNKNOWN;
	}
	public Draw_SyntaxToken(Draw_SyntaxElemDecl elem_decl, String text, SyntaxTokenKind kind) {
		super(elem_decl);
		this.text = text;
		this.kind = kind;
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawToken(node, this, text_syntax);
		return dr;
	}
	
	public Draw_SyntaxToken copyWithText(String text) {
		Draw_SyntaxToken st = (Draw_SyntaxToken)this.clone();
		st.text = text;
		return st;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.text != null) this.text = this.text.intern();
		return this;
	}
}

public class Draw_SyntaxPlaceHolder extends Draw_SyntaxElem {
	public String							text;
	public String							attr_name;

	public Draw_SyntaxPlaceHolder(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawPlaceHolder(node, this, text_syntax);
		return dr;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.text != null) this.text = this.text.intern();
		if (this.attr_name != null) this.attr_name = this.attr_name.intern();
		return this;
	}

	public ExpectedTypeInfo[] getExpectedTypes() {
		if (elem_decl != null)
			return elem_decl.getExpectedTypes(attr_name);
		return null;
	}
}

public class Draw_SyntaxSpace extends Draw_SyntaxElem {
	public Draw_SyntaxSpace(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawSpace(node, this, text_syntax);
		return dr;
	}
}

public abstract class Draw_SyntaxAttr extends Draw_SyntaxElem {
	public static final Draw_SyntaxAttr[] emptyArray = new Draw_SyntaxAttr[0];

	public String							name;
	public Draw_ATextSyntax					in_syntax;
	public Draw_SyntaxElem					empty;

	public transient AttrSlot				attr_slot;

	public Draw_SyntaxAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		if (this.empty instanceof Draw_SyntaxPlaceHolder) {
			Draw_SyntaxPlaceHolder dsph = (Draw_SyntaxPlaceHolder)this.empty;
			if (dsph.attr_name == null)
				dsph.attr_name = this.name;
		}
		return this;
	}
	public ExpectedTypeInfo[] getExpectedTypes() {
		if (elem_decl != null)
			return elem_decl.getExpectedTypes(name);
		return null;
	}
}

public class Draw_SyntaxSubAttr extends Draw_SyntaxAttr {
	public Draw_SyntaxSubAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		return new DrawSubAttr(node, this, text_syntax);
	}
}

public class Draw_SyntaxNode extends Draw_SyntaxAttr {
	public Draw_SyntaxNode(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		return new DrawNode(node, this, text_syntax);
	}
}

public class Draw_SyntaxIdentAttr extends Draw_SyntaxAttr {
	public Draw_SyntaxIdentTemplate		template;

	public Draw_SyntaxIdentAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawIdent(node, this, text_syntax);
		return dr;
	}

	public boolean isOk(String text) {
		Draw_SyntaxIdentTemplate t = template;
		if (t == null) return true;
		if (t.pattern != null && !t.pattern.matcher(text).matches())
			return false;
		foreach (ConstStringExpr cs; t.keywords; cs.value == text)
			return false;
		return true;
	}
	
	public String getPrefix() {
		Draw_SyntaxIdentTemplate t = template;
		if (t == null || t.esc_prefix == null) return "";
		return t.esc_prefix;
	}	
	public String getSuffix() {
		Draw_SyntaxIdentTemplate t = template;
		if (t == null || t.esc_suffix == null) return "";
		return t.esc_suffix;
	}	

}

public class Draw_SyntaxCharAttr extends Draw_SyntaxAttr {
	public Draw_SyntaxCharAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawCharTerm(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxStrAttr extends Draw_SyntaxAttr {
	public Draw_SyntaxStrAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawStrTerm(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxXmlStrAttr extends Draw_SyntaxAttr {
	public String prefix;
	public String suffix;
	
	public Draw_SyntaxXmlStrAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Draw_SyntaxXmlStrAttr(Draw_SyntaxElemDecl elem_decl, AttrSlot attr_slot, String prefix, String suffix) {
		super(elem_decl);
		this.name = attr_slot.name;
		this.attr_slot = attr_slot;
		this.prefix = prefix;
		this.suffix = suffix;
	}
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawXmlStrTerm(node, this, text_syntax);
		return dr;
	}
	public String getPrefix() {
		if (prefix == null) return "";
		return prefix;
	}	
	public String getSuffix() {
		if (suffix == null) return "";
		return suffix;
	}	
}

public class Draw_SyntaxXmlTypeAttr extends Draw_SyntaxXmlStrAttr {
	public Draw_SyntaxXmlTypeAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Draw_SyntaxXmlTypeAttr(Draw_SyntaxElemDecl elem_decl, AttrSlot attr_slot) {
		super(elem_decl, attr_slot,null,null);
	}
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawXmlTypeTerm(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxList extends Draw_SyntaxAttr {
	public Draw_SyntaxElem					element;
	public Draw_SyntaxElem					separator;
	public Draw_SyntaxElem					prefix;
	public Draw_SyntaxElem					sufix;
	public Draw_CalcOption					filter;

	public Draw_SyntaxList(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		if (in_syntax != null)
			text_syntax = in_syntax;
		Drawable dr = new DrawNonTermList(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxListWrapper extends Draw_SyntaxAttr {
	public Draw_SyntaxElem					prefix;
	public Draw_SyntaxElem					sufix;
	public Draw_SyntaxList					list;

	public Draw_SyntaxListWrapper(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		if (in_syntax != null)
			text_syntax = in_syntax;
		if (list != null)
			this.name = list.name;
		Drawable dr = new DrawListWrapper(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxTreeBranch extends Draw_SyntaxAttr {
	public Draw_SyntaxElem					folded;
	public Draw_SyntaxElem					element;
	public Draw_CalcOption					filter;

	public Draw_SyntaxTreeBranch(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		if (in_syntax != null)
			text_syntax = in_syntax;
		Drawable dr = new DrawTreeBranch(node, this, text_syntax);
		return dr;
	}
}


public class Draw_SyntaxSet extends Draw_SyntaxElem {
	public Draw_SyntaxElem[]				elements = Draw_SyntaxElem.emptyArray;
	public boolean							nested_function_lookup;

	public Draw_SyntaxSet(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawNonTermSet(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxSwitch extends Draw_SyntaxElem {
	public Draw_SyntaxToken					prefix;
	public Draw_ATextSyntax					target_syntax;
	public Draw_SyntaxToken					suffix;
	
	public Draw_SyntaxSwitch(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Draw_SyntaxSwitch(Draw_SyntaxElemDecl elem_decl, Draw_SyntaxToken prefix, Draw_SyntaxToken suffix, Draw_ATextSyntax target_syntax) {
		super(elem_decl);
		this.prefix = prefix;
		this.suffix = suffix;
		this.target_syntax = target_syntax;
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		return new DrawSyntaxSwitch(node,this,text_syntax);
	}
	
	private Draw_ATextSyntax getTargetSyntax(ANode for_node) {
		ThisIsANode node_data = (ThisIsANode)for_node.getClass().getAnnotation(ThisIsANode.class);
		Class lng_class = node_data.lang();
		Language lng = (Language)lng_class.getField(Constants.nameInstance).get(null);
		return SyntaxManager.getDefaultEditorSyntax(lng);
	}

	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
		if (expected_node != curr_dr.drnode)
			return false;
		if!(curr_dr instanceof DrawSyntaxSwitch)
			return false;
		DrawSyntaxSwitch curr_dss = (DrawSyntaxSwitch)curr_dr;
		if (getTargetSyntax(expected_node) != target_syntax)
			return false;
		Drawable sub_dr = curr_dss.element;
		if (sub_dr == null)
			return true;
		if (sub_dr.text_syntax != target_syntax)
			return false;
		Draw_SyntaxElem expected_stx = target_syntax.getSyntaxElem(expected_node);
		return expected_stx.check(cont,sub_dr,expected_node);
	}
}

public class Draw_SyntaxOptional extends Draw_SyntaxElem {
	public Draw_CalcOption					calculator;
	public Draw_SyntaxElem					opt_true;
	public Draw_SyntaxElem					opt_false;

	public Draw_SyntaxOptional(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawOptional(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxEnumChoice extends Draw_SyntaxAttr {
	public Draw_SyntaxElem[]				elements = Draw_SyntaxElem.emptyArray;

	public Draw_SyntaxEnumChoice(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawEnumChoice(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxFolder extends Draw_SyntaxElem {
	public boolean							folded_by_default;
	public Draw_SyntaxElem					folded;
	public Draw_SyntaxElem					unfolded;

	public Draw_SyntaxFolder(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawFolded(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxExpr extends Draw_SyntaxElem {
	public Draw_SyntaxExprTemplate			template;
	public Draw_SyntaxAttr[]				attrs = Draw_SyntaxAttr.emptyArray;

	public Draw_SyntaxExpr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		if (node instanceof ENode && node.getOp() != null)
			return fmt.getDrawable(node, null, text_syntax);
		return new DrawLispExpr(node, this, text_syntax);
	}

	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
		if (expected_node != curr_dr.drnode)
			return false;
		if (expected_node instanceof ENode && expected_node.getOp() == null)
			return true;
		return false;
	}
}

public class Draw_SyntaxAutoParenth extends Draw_SyntaxElem {
	public Draw_SyntaxExprTemplate			template;
	public Draw_SyntaxAttr					attr;
	public int								priority;
	
	public Draw_SyntaxAutoParenth(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Draw_SyntaxAutoParenth(Draw_SyntaxElemDecl elem_decl, Draw_SyntaxAttr attr, int priority, Draw_SyntaxExprTemplate template) {
		super(elem_decl);
		this.template = template;
		this.attr = attr;
		this.priority = priority;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Object obj;
		if (attr instanceof SyntaxNode) {
			obj = node;
		} else try {
			obj = node.getVal(attr.name);
		} catch (RuntimeException e) {
			obj = "";
		}
		if (obj instanceof ENode) {
			if (obj.isPrimaryExpr() || obj.getPriority() < this.priority)
				return new DrawAutoParenth(node, this, text_syntax);
		}
		return attr.makeDrawable(fmt, node, text_syntax);
	}

	public boolean check(DrawContext cont, Drawable curr_dr, ANode expected_node) {
		Object obj;
		if (attr instanceof SyntaxNode) {
			obj = expected_node;
		} else try {
			obj = expected_node.getVal(attr.name);
		} catch (RuntimeException e) {
			obj = "";
		}
		if (obj instanceof ENode) {
			if (obj.isPrimaryExpr() || obj.getPriority() < this.priority)
				return expected_node == curr_dr.drnode;
		}
		return attr.check(cont, curr_dr, expected_node);
	}
}

public class Draw_SyntaxJavaAccessExpr extends Draw_SyntaxElem {
	public Draw_SyntaxElem					obj_elem;
	public Draw_SyntaxToken					separator;
	public Draw_SyntaxElem					fld_elem;

	public Draw_SyntaxJavaAccessExpr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaAccessExpr(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxJavaAccess extends Draw_SyntaxElem {
	public Draw_SyntaxJavaAccess(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaAccess(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxJavaPackedField extends Draw_SyntaxElem {
	public Draw_SyntaxJavaPackedField(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaPackedField(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxJavaConstructorName extends Draw_SyntaxElem {
	public Draw_SyntaxJavaConstructorName(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaConstructorName(node, this, text_syntax);
		return dr;
	}
}

public class Draw_SyntaxJavaComment extends Draw_SyntaxElem {
	public Draw_SyntaxJavaCommentTemplate	template;

	public Draw_SyntaxJavaComment(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Drawable makeDrawable(Formatter fmt, ANode node, Draw_ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaComment(node, this, text_syntax);
		return dr;
	}
}



public class Draw_SyntaxElemDecl implements Serializable {
	public Draw_SyntaxElem					elem;
	public String 							clazz_name;
	public ExpectedAttrTypeInfo[]			attr_types;
	
	public ExpectedTypeInfo[] getExpectedTypes(String attr_name) {
		if (attr_name == null || attr_name.length() == 0 || attr_types == null)
			return null;
		Vector<ExpectedTypeInfo> expected = new Vector<ExpectedTypeInfo>();
		foreach (ExpectedAttrTypeInfo exp; attr_types; exp != null && exp.subtypes != null && attr_name.equals(exp.attr_name)) {
			foreach (ExpectedTypeInfo eti; exp.subtypes)
				expected.append(eti);
		}
		if (expected.length == 0)
			return null;
		return expected.toArray();
	}
}

public class Draw_SyntaxIdentTemplate implements Serializable {
	transient Pattern	pattern;

	public String 							regexp_ok;
	public String 							esc_prefix;
	public String 							esc_suffix;
	public String[]							keywords;

	@getter Pattern get$pattern() {
		if (pattern != null)
			return pattern;
		if (regexp_ok == null)
			regexp_ok = ".*";
		try {
			pattern = Pattern.compile(regexp_ok);
		} catch (PatternSyntaxException e) {
			System.out.println("Syntax error in ident template pattern: "+regexp_ok);
			try { pattern = Pattern.compile(".*"); } catch (PatternSyntaxException e) {}
		}
		return pattern;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.esc_prefix != null) this.esc_prefix = this.esc_prefix.intern();
		if (this.esc_suffix != null) this.esc_suffix = this.esc_suffix.intern();
		if (this.keywords != null) {
			for (int i=0; i < this.keywords.length; i++)
				this.keywords[i] = this.keywords[i].intern();
		}
		return this;
	}
}

public class Draw_SyntaxExprTemplate implements Serializable {
	public Draw_SyntaxElem					elem;
	public Draw_SyntaxToken					l_paren;
	public Draw_SyntaxToken					bad_op;
	public Draw_SyntaxToken					r_paren;
	public Draw_SyntaxToken[]				operators;
}

public class Draw_SyntaxJavaCommentTemplate implements Serializable {
	public Draw_SyntaxElem					elem;
	public Draw_SyntaxElem					newline;
	public Draw_SyntaxElem					lin_beg;
	public Draw_SyntaxElem					doc_beg;
	public Draw_SyntaxElem					cmt_beg;
	public Draw_SyntaxElem					cmt_end;
}


public class Draw_ATextSyntax implements Serializable {
	protected transient Hashtable<String,Draw_SyntaxElem>					badSyntax;
	protected transient Hashtable<String,Draw_SyntaxElem>					allSyntax;
	protected transient Hashtable<Pair<Operator,Class>, Draw_SyntaxElem> 	allSyntaxExprs;

	public Draw_ATextSyntax				parent_syntax;
	public Draw_ATextSyntax[]			sub_syntax;
	public Draw_SyntaxElemDecl[]		declared_syntax_elements;
	public String						q_name;	// qualified name

	
	Object readResolve() throws ObjectStreamException {
		if (this.q_name != null) this.q_name = this.q_name.intern();
		this.init();
		return this;
	}

	public Draw_ATextSyntax init() {
		if (allSyntax != null)
			return this;
		badSyntax = new Hashtable<String,Draw_SyntaxElem>();
		allSyntax = new Hashtable<String,Draw_SyntaxElem>();
		allSyntaxExprs = new Hashtable<Pair<Operator,Class>, Draw_SyntaxElem>();
		foreach (Draw_SyntaxElemDecl sed; declared_syntax_elements) {
			allSyntax.put(sed.clazz_name, sed.elem);
		}
		foreach (Draw_ATextSyntax stx; sub_syntax)
			stx.init();
		return this;
	}

	protected Draw_SyntaxSet expr(Operator op, Draw_SyntaxExpr sexpr)
	{
		Draw_SyntaxElem[] elems = new Draw_SyntaxElem[op.args.length];
		int earg = 0;
		for (int i=0; i < elems.length; i++) {
			OpArg arg = op.args[i];
			switch (arg) {
			case OpArg.EXPR(int priority):
				elems[i] = new Draw_SyntaxAutoParenth(null,sexpr.attrs[earg], priority, sexpr.template);
				earg++;
				continue;
			case OpArg.TYPE():
				elems[i] = new Draw_SyntaxAutoParenth(null,sexpr.attrs[earg], 255, sexpr.template);
				earg++;
				continue;
			case OpArg.OPER(String text):
				if (sexpr.template != null) {
					foreach (Draw_SyntaxToken t; sexpr.template.operators) {
						if (t.text == text) {
							elems[i] = t;
							break;
						}
						if (t.text == "DEFAULT")
							elems[i] = t.copyWithText(text);
					}
				}
				if (elems[i] == null)
					elems[i] = new Draw_SyntaxToken(null,text, SyntaxTokenKind.OPERATOR);
				continue;
			}
		}
		Draw_SyntaxSet set = new Draw_SyntaxSet(null);
		set.elements = elems;
		return set;
	}

	public Draw_SyntaxElem getSyntaxElem(ANode for_node) {
		if (for_node != null) {
			String cl_name = for_node.getClass().getName();
			Draw_SyntaxElem elem = allSyntax.get(cl_name);
			if (elem != null)
				return elem;
		}
		if (parent_syntax != null)
			return parent_syntax.getSyntaxElem(for_node);
		Draw_SyntaxElem se;
		if (for_node == null) {
			se = badSyntax.get("<null>");
			if (se == null) {
				se = new Draw_SyntaxToken(null,"(?null?)");
				badSyntax.put("<null>", se);
			}
		} else {
			ThisIsANode node_data = (ThisIsANode)for_node.getClass().getAnnotation(ThisIsANode.class);
			if (node_data != null) {
				Class lng_class = node_data.lang();
				if (lng_class != null && Language.class.isAssignableFrom(lng_class)) {
					Language lng = (Language)lng_class.getField(Constants.nameInstance).get(null);
					Draw_ATextSyntax stx = SyntaxManager.getDefaultEditorSyntax(lng);
					if (stx != this) {
						String text = lng.getClass().getName();
						Draw_SyntaxSwitch ssw = (Draw_SyntaxSwitch)badSyntax.get(text);
						if (ssw != null)
							return ssw;
						ssw = new Draw_SyntaxSwitch(null,
							new Draw_SyntaxToken(null,"#lang\""+text+"\"{"),
							new Draw_SyntaxToken(null,"}#"),
							stx
							);
						badSyntax.put(text, ssw);
						return ssw;
					}
				}
			}
			String cl_name = for_node.getClass().getName();
			se = badSyntax.get(cl_name);
			if (se == null) {
				se = new Draw_SyntaxToken(null,"(?"+cl_name+"?)");
				badSyntax.put(cl_name, se);
			}
		}
		return se;
	}
}

public class Draw_TextSyntax extends Draw_ATextSyntax {
}

public abstract class Draw_AXmlDumpSyntax extends Draw_ATextSyntax {
	public String dump = "full";
	protected Draw_Layout loutSpNo;
	protected Draw_Layout loutNoNo;
	protected Draw_Layout loutNlNl;
	protected Draw_Layout loutNoNl;
	protected Draw_Paragraph plIndented;

	public Draw_AXmlDumpSyntax() {
		this("full");
	}
	public Draw_AXmlDumpSyntax(String dump) {
		this.dump = dump;
		SpaceInfo siSp = new SpaceInfo("sp", SP_SPACE, 1,  4);
		SpaceInfo siNl = new SpaceInfo("nl", SP_NEW_LINE, 1,  1);
		SyntaxElemFormatDecl sefdNoNo = new SyntaxElemFormatDecl("fmt-default");
		SyntaxElemFormatDecl sefdSpNo = new SyntaxElemFormatDecl("fmt-sp-no");
		SyntaxElemFormatDecl sefdNlNl = new SyntaxElemFormatDecl("fmt-nl-nl");
		SyntaxElemFormatDecl sefdNoNl = new SyntaxElemFormatDecl("fmt-no-nl");

		sefdSpNo.spaces += new SpaceCmd(siSp, SP_ADD, SP_NOP, 0);
		sefdNlNl.spaces += new SpaceCmd(siNl, SP_ADD, SP_ADD, 0);
		sefdNoNl.spaces += new SpaceCmd(siNl, SP_NOP, SP_ADD, 0);

		loutSpNo = sefdSpNo.compile();
		loutNoNo = sefdNoNo.compile();
		loutNlNl = sefdNlNl.compile();
		loutNoNl = sefdNoNl.compile();

		plIndented = new Draw_Paragraph();
		plIndented.indent = new Draw_IndentInfo();
		plIndented.indent.text_size = 1;
		plIndented.indent.pixel_size = 10;
	}

	protected Draw_SyntaxElem tok(String text, Draw_Layout lout) {
		Draw_SyntaxToken st = new Draw_SyntaxToken(null,text);
		st.lout = lout;
		return st;
	}
	protected Draw_SyntaxElem tok(Draw_Layout lout, String text) {
		Draw_SyntaxToken st = new Draw_SyntaxToken(null,text);
		st.lout = lout;
		return st;
	}
	protected Draw_SyntaxElem tok(String text) { tok(text, loutNoNo) }
	protected Draw_SyntaxElem open(String name) { tok("<"+name+">", loutNlNl) }
	protected Draw_SyntaxElem close(String name) { tok("</"+name+">", loutNlNl) }
	protected Draw_SyntaxElem open0(String name) { tok("<"+name+">", loutNoNo) }
	protected Draw_SyntaxElem close0(String name) { tok("</"+name+">", loutNoNo) }

	protected Draw_SyntaxAttr attr(AttrSlot slot) {
		Draw_SyntaxSubAttr st = new Draw_SyntaxSubAttr(null);
		st.name = slot.name;
		st.attr_slot = slot;
		return st;
	}
	protected Draw_SyntaxElem par(Draw_SyntaxElem elem) {
		elem.par = plIndented;
		return elem;
	}
	protected Draw_SyntaxSet set(Draw_SyntaxElem... elems) {
		Draw_SyntaxSet set = new Draw_SyntaxSet(null);
		set.elements = elems;
		return set;
	}
	protected Draw_SyntaxSet setl(Draw_Layout lout, Draw_SyntaxElem... elems) {
		Draw_SyntaxSet set = new Draw_SyntaxSet(null);
		set.lout = lout;
		set.elements = elems;
		return set;
	}
	protected Draw_SyntaxOptional opt(Draw_CalcOption calc, Draw_SyntaxElem opt_true)
	{
		Draw_SyntaxOptional st = new Draw_SyntaxOptional(null);
		st.calculator = calc;
		st.opt_true = opt_true;
		return st;
	}
}
	
public class Draw_XmlDumpSyntax extends Draw_AXmlDumpSyntax {
	
	public Draw_XmlDumpSyntax() { this("full"); }
	public Draw_XmlDumpSyntax(String dump) { super(dump); }

	public Draw_SyntaxElem getSyntaxElem(ANode node) {
		String cl_name = node.getClass().getName();
		{
			Draw_SyntaxElem elem = allSyntax.get(cl_name);
			if (elem != null)
				return elem;
		}
		Draw_SyntaxSet ss = new Draw_SyntaxSet(null);
		ss.lout = loutNoNl;
		foreach (AttrSlot attr; node.values(); attr != ASTNode.nodeattr$this && attr != ASTNode.nodeattr$parent) {
			Draw_SyntaxElem se = null;
			if (attr instanceof SpaceAttrSlot) {
				Draw_SyntaxList sl = new Draw_SyntaxList(null);
				sl.name = attr.name;
				sl.element = new Draw_SyntaxNode(null);
				sl.filter = new Draw_CalcOptionIncludeInDump(this.dump,"this");
				sl.lout = loutNoNl;
				se = setl(loutNoNl, open(attr.name), sl, close(attr.name));
			} else {
				if (ANode.class.isAssignableFrom(attr.clazz))
					se = setl(loutNoNl, open(attr.name), par(attr(attr)), close(attr.name));
				else if (Enum.class.isAssignableFrom(attr.clazz))
					se = set(open0(attr.name), attr(attr), close0(attr.name));
				else if (attr.clazz == String.class || attr.clazz == Character.TYPE || attr.clazz == Operator.class)
					se = set(open0(attr.name), new Draw_SyntaxXmlStrAttr(null,attr,null,null), close0(attr.name));
				else if (Type.class.isAssignableFrom(attr.clazz))
					se = set(open0(attr.name), new Draw_SyntaxXmlTypeAttr(null,attr), close0(attr.name));
				else if (attr.clazz == Integer.TYPE || attr.clazz == Boolean.TYPE ||
					attr.clazz == Byte.TYPE || attr.clazz == Short.TYPE || attr.clazz == Long.TYPE ||
					attr.clazz == Float.TYPE || attr.clazz == Double.TYPE
					)
					se = set(open0(attr.name), attr(attr), close0(attr.name));
				else if (attr.is_attr) {
					se = tok("<error attr='"+attr.name+"'"+" class='"+cl_name+"' />", loutNlNl);
				}
			}
			if (se != null) {
				//ss.elements += opt(new Draw_CalcOptionIncludeInDump(this.dump,attr.name),se);
				ss.elements = (Draw_SyntaxElem[])Arrays.append(ss.elements, opt(new Draw_CalcOptionIncludeInDump(this.dump,attr.name),se));
			}
		}
		ss = setl(loutNoNl, tok("<a-node class='"+cl_name+"'>", loutNlNl), par(ss), tok("</a-node>", loutNlNl));
		allSyntax.put(cl_name,ss);
		return ss;
	}
}

public class Draw_NsXmlDumpSyntax extends Draw_AXmlDumpSyntax {
	
	public Draw_NsXmlDumpSyntax() { this("full"); }
	public Draw_NsXmlDumpSyntax(String dump) { super(dump); }

	public Draw_SyntaxElem getSyntaxElem(ANode node) {
		String cl_name = node.getClass().getName();
		{
			Draw_SyntaxElem elem = allSyntax.get(cl_name);
			if (elem != null)
				return elem;
		}
		Draw_SyntaxSet attrs = new Draw_SyntaxSet(null);
		Draw_SyntaxSet elems = new Draw_SyntaxSet(null);
		elems.lout = loutNoNl;
		foreach (AttrSlot attr; node.values(); attr != ASTNode.nodeattr$this && attr != ASTNode.nodeattr$parent) {
			if (attr.isXmlIgnore())
				continue;
			Draw_SyntaxElem se = null;
			Draw_SyntaxElem ae = null;
			if (attr instanceof SpaceAttrSlot) {
				Draw_SyntaxList sl = new Draw_SyntaxList(null);
				sl.name = attr.name;
				sl.element = new Draw_SyntaxNode(null);
				sl.filter = new Draw_CalcOptionIncludeInDump(this.dump,"this");
				sl.lout = loutNoNl;
				se = setl(loutNoNl, open(attr.name), sl, close(attr.name));
			}
			else if (attr.isXmlAttr()) {
				String anm = attr.getXmlFullName();
				if (anm == null || anm.length() == 0)
					anm = attr.name;
				if (Enum.class.isAssignableFrom(attr.clazz))
					ae = setl(loutSpNo, tok(anm+"='"), attr(attr), tok("'"));
				else if (attr.clazz == String.class || attr.clazz == Character.TYPE || attr.clazz == Operator.class)
					ae = setl(loutSpNo, tok(anm+"="), new Draw_SyntaxXmlStrAttr(null,attr,"'","'"));
				else if (Type.class.isAssignableFrom(attr.clazz))
					ae = setl(loutSpNo, tok(anm+"='"), new Draw_SyntaxXmlTypeAttr(null,attr), tok("'"));
				else if (attr.clazz == Integer.TYPE || attr.clazz == Boolean.TYPE ||
					attr.clazz == Byte.TYPE || attr.clazz == Short.TYPE || attr.clazz == Long.TYPE ||
					attr.clazz == Character.TYPE || attr.clazz == Float.TYPE || attr.clazz == Double.TYPE
					)
					ae = setl(loutSpNo, tok(anm+"='"), attr(attr), tok("'"));
				else if (attr.is_attr || ANode.class.isAssignableFrom(attr.clazz)) {
					ae = tok(loutSpNo, "error_attr='"+attr.name+" class="+cl_name+"'");
				}
			}
			else {
				if (ANode.class.isAssignableFrom(attr.clazz))
					se = setl(loutNoNl, open(attr.name), par(attr(attr)), close(attr.name));
				else if (Enum.class.isAssignableFrom(attr.clazz))
					se = set(open0(attr.name), attr(attr), close0(attr.name));
				else if (attr.clazz == String.class || attr.clazz == Character.TYPE || attr.clazz == Operator.class)
					se = set(open0(attr.name), new Draw_SyntaxXmlStrAttr(null,attr,null,null), close0(attr.name));
				else if (Type.class.isAssignableFrom(attr.clazz))
					se = set(open0(attr.name), new Draw_SyntaxXmlTypeAttr(null,attr), close0(attr.name));
				else if (attr.clazz == Integer.TYPE || attr.clazz == Boolean.TYPE ||
					attr.clazz == Byte.TYPE || attr.clazz == Short.TYPE || attr.clazz == Long.TYPE ||
					attr.clazz == Float.TYPE || attr.clazz == Double.TYPE
					)
					se = set(open0(attr.name), attr(attr), close0(attr.name));
				else if (attr.is_attr) {
					se = tok(loutNlNl, "<error attr='"+attr.name+"'"+" class='"+cl_name+"' />");
				}
			}
			if (se != null) {
				elems.elements = (Draw_SyntaxElem[])Arrays.append(elems.elements, opt(new Draw_CalcOptionIncludeInDump(this.dump,attr.name),se));
			}
			if (ae != null) {
				attrs.elements = (Draw_SyntaxElem[])Arrays.append(attrs.elements, opt(new Draw_CalcOptionIncludeInDump(this.dump,attr.name),ae));
			}
		}
		Draw_SyntaxElem sn;
		Language lng = node.getCompilerLang();
		String lng_name = lng == null ? "void" : lng.getName();
		String node_name = lng_name+":"+node.getCompilerNodeName(); // <core:Var>
		{
			Draw_SyntaxElem start;
			if (attrs.elements.length == 0 && elems.elements.length == 0)
				sn = tok(loutNlNl, "<"+node_name+"/>");
			else if (attrs.elements.length == 0)
				sn = set(tok(loutNlNl, "<"+node_name+">"), par(elems), tok(loutNlNl, "</"+node_name+">"));
			else if (elems.elements.length == 0)
				sn = setl(loutNlNl, tok("<"+node_name), attrs, tok("/>"));
			else
				sn = setl(loutNlNl, tok("<"+node_name), attrs, tok(loutNoNl, ">"), par(elems), tok(loutNlNl, "</"+node_name+">"));
		}
		allSyntax.put(cl_name,sn);
		return sn;
	}
}

public class Draw_KievTextSyntax extends Draw_ATextSyntax {
	public Draw_SyntaxElem getSyntaxElem(ANode node) {
		if (node != null) {
			String cl_name = node.getClass().getName();
			Draw_SyntaxElem sed = allSyntax.get(cl_name);
			if (sed != null) {
				Draw_SyntaxElem se = sed;
				if (node instanceof ENode && se instanceof Draw_SyntaxExpr) {
					ENode e = (ENode)node;
					Operator op = e.getOp();
					if (op == null)
						return se;
					se = allSyntaxExprs.get(new Pair<Operator,Class>(op,node.getClass()));
					if (se == null) {
						se = expr(op, (Draw_SyntaxExpr)sed);
						allSyntaxExprs.put(new Pair<Operator,Class>(op,node.getClass()), se);
					}
					return se;
				}
				return se;
			}
		}
		return super.getSyntaxElem(node);
	}
}

public class Draw_TreeSyntax extends Draw_ATextSyntax {
	protected Draw_Layout loutSpNo;
	protected Draw_Layout loutNoNo;
	protected Draw_Layout loutNlNl;
	protected Draw_Layout loutNoNl;
	protected Draw_Paragraph plIndented;

	public Draw_TreeSyntax() {
		SpaceInfo siSp = new SpaceInfo("sp", SP_SPACE, 1,  4);
		SpaceInfo siNl = new SpaceInfo("nl", SP_NEW_LINE, 1,  1);
		SyntaxElemFormatDecl sefdNoNo = new SyntaxElemFormatDecl("fmt-default");
		SyntaxElemFormatDecl sefdSpNo = new SyntaxElemFormatDecl("fmt-sp-no");
		SyntaxElemFormatDecl sefdNlNl = new SyntaxElemFormatDecl("fmt-nl-nl");
		SyntaxElemFormatDecl sefdNoNl = new SyntaxElemFormatDecl("fmt-no-nl");

		sefdSpNo.spaces += new SpaceCmd(siSp, SP_ADD, SP_NOP, 0);
		sefdNlNl.spaces += new SpaceCmd(siNl, SP_ADD, SP_ADD, 0);
		sefdNoNl.spaces += new SpaceCmd(siNl, SP_NOP, SP_ADD, 0);

		loutSpNo = sefdSpNo.compile();
		loutNoNo = sefdNoNo.compile();
		loutNlNl = sefdNlNl.compile();
		loutNoNl = sefdNoNl.compile();

		plIndented = new Draw_Paragraph();
		plIndented.indent = new Draw_IndentInfo();
		plIndented.indent.next_text_size = 2;
		plIndented.indent.next_pixel_size = 20;
	}

	public Draw_SyntaxElem getSyntaxElem(ANode node) {
		if (node == null)
			return super.getSyntaxElem(node);
		String cl_name = node.getClass().getName();
		Draw_SyntaxElem sed = allSyntax.get(cl_name);
		if (sed != null) {
			Draw_SyntaxElem se = sed;
			if (node instanceof ENode && se instanceof Draw_SyntaxExpr) {
				ENode e = (ENode)node;
				Operator op = e.getOp();
				if (op == null)
					return se;
				se = allSyntaxExprs.get(new Pair<Operator,Class>(op,node.getClass()));
				if (se == null) {
					se = expr(op, (Draw_SyntaxExpr)sed);
					allSyntaxExprs.put(new Pair<Operator,Class>(op,node.getClass()), se);
				}
			}
			return se;
		}
		Draw_SyntaxElemDecl elem_decl = new Draw_SyntaxElemDecl();
		Draw_SyntaxTreeBranch stb = new Draw_SyntaxTreeBranch(elem_decl);
		stb.par = plIndented;
		elem_decl.elem = stb;
		elem_decl.clazz_name = cl_name;
		{
			String name = node.getClass().getName();
			int idx = name.lastIndexOf('.');
			if (idx >= 0)
				name = name.substring(idx+1);
			stb.folded = new Draw_SyntaxToken(elem_decl,name);
			stb.folded.lout = loutNlNl;
		}
		Draw_SyntaxSet ss = new Draw_SyntaxSet(elem_decl);
		stb.element = ss;
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			if (attr instanceof SpaceAttrSlot || attr instanceof ExtSpaceAttrSlot) {
				Draw_SyntaxTreeBranch lst = new Draw_SyntaxTreeBranch(elem_decl);
				lst.par = plIndented;
				lst.folded =  new Draw_SyntaxToken(elem_decl,attr.name+"∅");
				lst.folded.lout = loutNlNl;
				lst.name = attr.name;
				lst.attr_slot = attr;
				lst.element = new Draw_SyntaxNode(elem_decl);
				lst.element.lout = loutNlNl;
				ss.elements = (Draw_SyntaxElem[])Arrays.append(ss.elements, lst);
			}
			else if (attr.is_child) {
				Draw_SyntaxTreeBranch lst = new Draw_SyntaxTreeBranch(elem_decl);
				lst.par = plIndented;
				lst.folded =  new Draw_SyntaxToken(elem_decl,attr.name);
				lst.folded.lout = loutNlNl;
				lst.name = attr.name;
				lst.attr_slot = attr;
				Draw_SyntaxSet ass = new Draw_SyntaxSet(elem_decl);
				lst.element = ass;
				lst.element.lout = loutNlNl;
				Draw_SyntaxSubAttr sa = new Draw_SyntaxSubAttr(elem_decl);
				sa.name = attr.name;
				sa.attr_slot = attr;
				ass.elements = (Draw_SyntaxElem[])Arrays.append(ass.elements, sa);
				ss.elements = (Draw_SyntaxElem[])Arrays.append(ss.elements, lst);
			}
			else {
				Draw_SyntaxSet ass = new Draw_SyntaxSet(elem_decl);
				ass.lout = loutNlNl;
				ass.elements = (Draw_SyntaxElem[])Arrays.append(ass.elements, new Draw_SyntaxToken(elem_decl,attr.name+": "));
				Draw_SyntaxSubAttr st = new Draw_SyntaxSubAttr(elem_decl);
				st.name = attr.name;
				st.attr_slot = attr;
				ass.elements = (Draw_SyntaxElem[])Arrays.append(ass.elements, st);
				ss.elements = (Draw_SyntaxElem[])Arrays.append(ss.elements, ass);
			}
		}
		allSyntax.put(cl_name,stb);
		return ss;
	}
}
