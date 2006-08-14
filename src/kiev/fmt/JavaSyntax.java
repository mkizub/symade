package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.Operator;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


@node
public class SyntaxJavaExprTemplate extends AbstractSyntaxElemDecl {
	@virtual typedef This  = SyntaxJavaExprTemplate;

	@att public SyntaxToken		l_paren;
	@att public SyntaxToken		r_paren;
	@att public SyntaxToken[]	operators;

	public SyntaxJavaExprTemplate() {
		super(new SyntaxNode());
		this.l_paren = new SyntaxToken("(",new SpaceCmd[0]);
		this.r_paren = new SyntaxToken(")",new SpaceCmd[0]);
	}
}

@node
public class SyntaxJavaCommentTemplate extends AbstractSyntaxElemDecl {
	@virtual typedef This  = SyntaxJavaCommentTemplate;

	@att public SyntaxElem		newline;
	@att public SyntaxElem		lin_beg;
	@att public SyntaxElem		doc_beg;
	@att public SyntaxElem		cmt_beg;
	@att public SyntaxElem		cmt_end;

	public SyntaxJavaCommentTemplate() {}
}

@node
public class SyntaxJavaExpr extends SyntaxAttr {
	@virtual typedef This  = SyntaxJavaExpr;

	@att public int									idx;
	@att public int									priority;
	@att public SymbolRef<SyntaxJavaExprTemplate>	template;

	public SyntaxJavaExpr() {
		super("",new SpaceCmd[0]);
		this.idx = -1;
		this.template = new SymbolRef<SyntaxJavaExprTemplate>(0,"java-expr-template");
	}
	public SyntaxJavaExpr(String name, int priority, SyntaxJavaExprTemplate template) {
		super(name,new SpaceCmd[0]);
		this.idx = -1;
		this.priority = priority;
		this.template = new SymbolRef<SyntaxJavaExprTemplate>((Symbol<SyntaxJavaExprTemplate>)template.id);
	}
	public SyntaxJavaExpr(int idx, int priority, SyntaxJavaExprTemplate template) {
		super("",new SpaceCmd[0]);
		this.idx = idx;
		this.priority = priority;
		this.template = new SymbolRef<SyntaxJavaExprTemplate>((Symbol<SyntaxJavaExprTemplate>)template.id);
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		//if (this != current_stx)
		//	return false;
		ANode n;
		if (idx >= 0) {
			n = ((ENode)expected_node).getArgs()[idx];
		} else {
			n = (name == "this") ? expected_node : (ANode)expected_node.getVal(name);
		}
		if (n != current_node)
			return false;
		return true;
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		ANode n;
		if (idx >= 0) {
			n = ((ENode)node).getArgs()[idx];
		} else {
			n = (name == "this") ? node : (ANode)node.getVal(name);
		}
		Drawable dr = new DrawJavaExpr(n, this);
		return dr;
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			DNode@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved java expression template "+template);
			else if !(d instanceof SyntaxJavaExprTemplate)
				Kiev.reportError(template,"Resolved "+template+" is not a java expression template");
			else
				template.symbol = d.id;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxJavaExprTemplate> vect = new Vector<SyntaxJavaExprTemplate>();
			SyntaxJavaExprTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (d instanceof SyntaxJavaExprTemplate && !vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@node
public class SyntaxJavaAccessExpr extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaAccessExpr;

	@att public SyntaxElem			obj_elem;
	@att public SyntaxToken			separator;
	@att public SyntaxElem			fld_elem;

	public SyntaxJavaAccessExpr() {}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaAccessExpr(node, this);
		return dr;
	}
}

@node
public class SyntaxJavaAccess extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaAccess;

	public SyntaxJavaAccess() {}
	public SyntaxJavaAccess(SpaceCmd[] spaces) {
		super(spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaAccess(node, this);
		return dr;
	}
}

@node
public class SyntaxJavaPackedField extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaPackedField;

	public SyntaxJavaPackedField() {}
	public SyntaxJavaPackedField(SpaceCmd[] spaces) {
		super(spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaPackedField(node, this);
		return dr;
	}
}


@node
public class SyntaxJavaComment extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaComment;

	@att public SymbolRef<SyntaxJavaCommentTemplate>	template;

	public SyntaxJavaComment() {
		this.template = new SymbolRef<SyntaxJavaCommentTemplate>();
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaComment(node, this);
		return dr;
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			DNode@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved java expression template "+template);
			else if !(d instanceof SyntaxJavaCommentTemplate)
				Kiev.reportError(template,"Resolved "+template+" is not a java comment template");
			else
				template.symbol = d.id;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxJavaCommentTemplate> vect = new Vector<SyntaxJavaCommentTemplate>();
			SyntaxJavaCommentTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (d instanceof SyntaxJavaCommentTemplate && !vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@node
public final class CalcOptionJavaFlag extends CalcOption {
	@virtual typedef This  = CalcOptionJavaFlag;

	private final int mask;
	private final int offs;
	private final int val;
	public CalcOptionJavaFlag() {}
	public CalcOptionJavaFlag(String name, int size, int offs, int val) {
		super(name);
		this.mask = (-1 << (32-size)) >>> (32-size);
		this.offs = offs;
		this.val = val;
	}
	public boolean calc(ANode node) {
		if (node == null || !(node instanceof DNode)) return false;
		int f = ((DNode)node).flags >>> offs;
		f &= mask;
		return f == val;
	}
}

@node
public class KievTextSyntax extends TextSyntax {

	final Hashtable<Operator, SyntaxElem> exprs;
	
	private SyntaxSet expr(Operator op, SyntaxJavaExprTemplate template)
	{
		SyntaxElem[] elems = new SyntaxElem[op.args.length];
		int earg = 0;
		for (int i=0; i < elems.length; i++) {
			OpArg arg = op.args[i];
			switch (arg) {
			case OpArg.EXPR(int priority):
				elems[i] = new SyntaxJavaExpr(earg, priority, template);
				earg++;
				continue;
			case OpArg.TYPE():
				elems[i] = new SyntaxJavaExpr(earg, 255, template);
				earg++;
				continue;
			case OpArg.OPER(String text):
				if (template != null) {
					foreach (SyntaxToken t; template.operators) {
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
				if (elems[i] == null)
					elems[i] = new SyntaxToken(text,new SpaceCmd[0]);
				continue;
			}
		}
		SyntaxSet set = new SyntaxSet();
		set.elements.addAll(elems);
		return set;
	}

	public KievTextSyntax() {
		exprs = new Hashtable<Operator, SyntaxElem>();
	}

	protected void cleanup() {
		exprs.clear();
		super.cleanup();
	}

	public SyntaxElem getSyntaxElem(ANode node) {
		if (node != null) {
			String cl_name = node.getClass().getName();
			SyntaxElemDecl sed = allSyntax.get(cl_name);
			if (sed != null) {
				SyntaxElem se = sed.elem;
				if (node instanceof ENode && se instanceof SyntaxJavaExpr && se.name == "") {
					ENode e = (ENode)node;
					Operator op = e.getOp();
					if (op == null)
						return se;
					se = exprs.get(op);
					if (se == null) {
						se = expr(op,(SyntaxJavaExprTemplate)((SyntaxJavaExpr)sed.elem).template.dnode);
						exprs.put(op, se);
					}
				}
				return se;
			}
		}
		return super.getSyntaxElem(node);
	}
}

