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
		this.template = new SymbolRef<SyntaxJavaExprTemplate>(0,template);
	}
	public SyntaxJavaExpr(int idx, int priority, SyntaxJavaExprTemplate template) {
		super("",new SpaceCmd[0]);
		this.idx = idx;
		this.priority = priority;
		this.template = new SymbolRef<SyntaxJavaExprTemplate>(0,template);
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
				template.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxJavaExprTemplate> vect = new Vector<SyntaxJavaExprTemplate>();
			DNode@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (d instanceof SyntaxJavaExprTemplate) vect.append((SyntaxJavaExprTemplate)d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
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
public class SyntaxJavaEnumAlias extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaEnumAlias;

	public SyntaxJavaEnumAlias() {}
	public SyntaxJavaEnumAlias(SpaceCmd[] spaces) {
		super(spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaEnumAlias(node, this);
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
				template.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxJavaCommentTemplate> vect = new Vector<SyntaxJavaCommentTemplate>();
			DNode@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (d instanceof SyntaxJavaCommentTemplate) vect.append((SyntaxJavaCommentTemplate)d);
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
class CalcOptionMetaAlias extends CalcOption {
	CalcOptionMetaAlias() {}
	public boolean calc(ANode node) { return ((Field)node).getMetaAlias() != null; }
}
@node
class CalcOptionEnumField extends CalcOption {
	CalcOptionEnumField() {}
	public boolean calc(ANode node) { return (node instanceof Field && node.isEnumField()); }
}
@node
class CalcOptionEnumFilter extends CalcOption {
	CalcOptionEnumFilter() {}
	public boolean calc(ANode node) { return !((node instanceof DNode && node.isSynthetic()) || (node instanceof Field && node.isEnumField())); }
}

@node
public class JavaSyntax extends TextSyntax {

	final SyntaxJavaExprTemplate seExprTemplate;
	
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
		SyntaxSet set = new SyntaxSet(new SpaceCmd[0]);
		set.elements.addAll(elems);
		return set;
	}

	public JavaSyntax() {
		exprs = new Hashtable<Operator, SyntaxElem>();

/*		
		{
			SpaceCmd[] lout_struct = new SpaceCmd[] {
					new SpaceCmd(siNlGrp, SP_NOP, SP_ADD, 0)
				};
			SpaceCmd[] lout_struct_hdr = new SpaceCmd[] {
					new SpaceCmd(siNlOrBlock, SP_NOP, SP_ADD, 0)
				};
			SpaceCmd[] lout_struct_block_start = new SpaceCmd[] {
					new SpaceCmd(siNlOrBlock, SP_EAT, SP_NOP, 0),
					new SpaceCmd(siNl,        SP_NOP, SP_ADD,  0),
					new SpaceCmd(siSp,        SP_ADD, SP_NOP, 0),
				};
			SpaceCmd[] lout_struct_block_end = new SpaceCmd[] {
					new SpaceCmd(siNl,        SP_ADD, SP_ADD, 0),
					new SpaceCmd(siSp,        SP_ADD, SP_NOP, 0),
				};
			SyntaxElem struct_args = opt(new CalcOptionNotEmpty("args"),
				set(
					sep("<"),
					lst("args", node(/ *new FormatInfoHint("class-arg")* /), sep(","), lout_empty),
					sep(">")
				), null, lout_empty);
			SpaceCmd[] lout_ext = new SpaceCmd[] {new SpaceCmd(siSp, SP_ADD, SP_NOP, 0)};
			SyntaxElem class_ext = opt(new CalcOptionNotEmpty("super_types"),
				set(
					kw("extends"),
					lst("super_types", node(), sep(","), lout_empty)
					),
				null, lout_ext
				);
			SyntaxList struct_members = lst("members",lout_empty);
//			struct_members.filter = new CalcOption() {
//				public boolean calc(ANode node) {
//					if (node instanceof DNode && node.isSynthetic())
//						return false;
//					return true;
//				}
//			};
			// anonymouse struct
			seStructBody = set(
					sep("{", lout_struct_block_start),
					par(plIndented, struct_members),
					sep("}", lout_struct_block_end)
					);
			// class
			seStructClass = set(
					setl(lout_struct_hdr,
						attr("meta"),
						kw("class"),
						ident("id"),
						struct_args.ncopy(),
						class_ext.ncopy()),
					seStructBody.ncopy()
				);
			// interface
			seStructInterface = set(
					setl(lout_struct_hdr,
						attr("meta"),
						kw("interface"),
						ident("id"),
						struct_args.ncopy(),
						class_ext),
					seStructBody.ncopy()
				);
			// view
			seStructView = set(
					setl(lout_struct_hdr,
						attr("meta"),
						kw("view"),
						ident("id"),
						struct_args.ncopy(),
						kw("of"),
						attr("view_of"),
						class_ext.ncopy()),
					seStructBody.ncopy()
				);
			// annotation
			seStructAnnotation = set(
					setl(lout_struct_hdr,
						attr("meta"),
						kw("@interface"),
						ident("id")),
					seStructBody.ncopy()
				);
			// syntax
			seStructSyntax = set(
					setl(lout_struct_hdr,
						attr("meta"),
						kw("syntax"),
						ident("id")),
					seStructBody.ncopy()
				);

			// case
			SyntaxList case_fields = lst("members",
				set(attr("meta"), attr("ftype"), ident("id")),
				sep(","),
				lout_empty
				);
//			case_fields.filter = new CalcOption() {
//				public boolean calc(ANode node) { return node instanceof Field && !node.isSynthetic(); }
//			};
			seStructCase = setl(lout_nl_grp,
					attr("meta"),
					kw("case"),
					ident("id"),
					struct_args.ncopy(),
					opt(new CalcOptionJavaFlag("singleton", 1, 20, 0),
						set(
							sep("("),
							case_fields,
							sep(")")
							),
						null,
						lout_empty
						),
					sep(";")
				);
			// enum
			SyntaxList enum_fields = lst("members",
				set(
					attr("id"),
					opt(new CalcOptionMetaAlias(),
						set(sep(":"), new SyntaxJavaEnumAlias(lout_empty)),
						null,
						lout_empty
						)
					),
				sep_nl(","),
				lout_empty);
			enum_fields.filter = new CalcOptionEnumField();
			SyntaxList enum_members = lst("members",lout_empty);
			enum_members.filter = new CalcOptionEnumFilter();
			seStructEnum = set(
					setl(lout_struct_hdr,
						attr("meta"),
						kw("enum"),
						ident("id")),
					set(
						sep("{", lout_struct_block_start),
						par(plIndented, enum_fields),
						sep(";"),
						new SyntaxSpace(lout_nl_grp),
						par(plIndented, enum_members),
						sep("}", lout_struct_block_end)
					)
				);
		}
*/
	}

	public String escapeString(String str) {
		return '\"'+new String(Convert.string2source(str), 0)+'\"';
	}
	public String escapeChar(char ch) {
		return "'"+Convert.escape(ch)+"'";
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
						se = expr(op,(SyntaxJavaExprTemplate)((SyntaxJavaExpr)sed.elem).template.symbol);
						exprs.put(op, se);
					}
				}
				return se;
			}
		}
		return super.getSyntaxElem(node);
	}
}

