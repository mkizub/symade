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
	
	final SyntaxElem seStructClass;
	final SyntaxElem seStructInterface;
	final SyntaxElem seStructAnnotation;
	final SyntaxElem seStructEnum;
	final SyntaxElem seStructCase;
	final SyntaxElem seStructSyntax;
	final SyntaxElem seStructView;
	final SyntaxElem seStructBody;

	final Hashtable<Operator, SyntaxElem> exprs;
	
	public SpaceInfo siSp     = new SpaceInfo("sp",       SP_SPACE,    1, 4);
	public SpaceInfo siSpSEPR = new SpaceInfo("sp-sepr",  SP_SPACE,    1, 4);
	public SpaceInfo siSpWORD = new SpaceInfo("sp-word",  SP_SPACE,    1, 4);
	public SpaceInfo siSpOPER = new SpaceInfo("sp-oper",  SP_SPACE,    1, 4);
	public SpaceInfo siNl     = new SpaceInfo("nl",       SP_NEW_LINE, 1,  1);
	public SpaceInfo siNlGrp  = new SpaceInfo("nl-group", SP_NEW_LINE, 2, 20);
	public SpaceInfo siNlOrBlock = new SpaceInfo("nl-block",       SP_NEW_LINE, 1,  1);
	public SpaceInfo siFldGrpNl  = new SpaceInfo("indent-block",   SP_NEW_LINE, 2, 20);
	
	public ParagraphLayout plIndented = new ParagraphLayout("par-indented", 4, 20);
	public ParagraphLayout plStatIndented = new ParagraphLayoutBlock("stat-indented", 4, 20);

	protected SyntaxParagraphLayout par(ParagraphLayout par, SyntaxElem elem) {
		SyntaxParagraphLayout spl = new SyntaxParagraphLayout(elem, par, new SpaceCmd[0]);
		return spl;
	}
	
	protected SyntaxSet set(SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(new SpaceCmd[0], elems);
		return set;
	}
	
	protected SyntaxSet setl(SpaceCmd[] spaces, SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(spaces, elems);
		return set;
	}

	protected SyntaxList lst(
			String name,
			SyntaxElem element,
			SyntaxElem separator,
			SpaceCmd[] spaces
	)
	{
		SyntaxList lst = new SyntaxList(name.intern(), element, separator, spaces);
		return lst;
	}

	protected SyntaxList lst(
			String name,
			SpaceCmd[] spaces
	)
	{
		SyntaxList lst = new SyntaxList(name.intern(), node(), null, spaces);
		return lst;
	}

	protected SyntaxNode node()
	{
		return new SyntaxNode();
	}

	protected SyntaxNode node(SpaceCmd[] lout)
	{
		return new SyntaxNode(lout);
	}

	protected SyntaxNode node(TextSyntax stx)
	{
		return new SyntaxNode(stx);
	}

	protected SyntaxAttr attr(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return new SyntaxSubAttr(slot, lout);
	}

	protected SyntaxAttr attr(String slot, SpaceCmd[] lout)
	{
		return new SyntaxSubAttr(slot, lout);
	}

	protected SyntaxAttr attr(String slot, TextSyntax stx)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return new SyntaxSubAttr(slot, stx, lout);
	}

	protected SyntaxIdentAttr ident(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpWORD, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxIdentAttr(slot,lout);
	}

	protected SyntaxCharAttr charcter(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpSEPR, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxCharAttr(slot,lout);
	}

	protected SyntaxStrAttr string(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_ADD, 0),
			};
		return new SyntaxStrAttr(slot,lout);
	}

	protected SyntaxToken kw(String kw)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpWORD, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxToken(kw,lout);
	}

	protected SyntaxToken sep(String sep)
	{
		if (sep == ";") {
			SpaceCmd[] lout = new SpaceCmd[] {
					new SpaceCmd(siSpWORD, SP_EAT, SP_NOP, 0),
					new SpaceCmd(siSpSEPR, SP_EAT, SP_NOP, 0),
					new SpaceCmd(siSp,     SP_NOP, SP_ADD, 0),
				};
				return new SyntaxToken(sep,lout);
		}
		if (sep == "{" || sep == "}") {
			SpaceCmd[] lout = new SpaceCmd[] {
					new SpaceCmd(siSp, SP_ADD, SP_ADD, 0),
				};
			return new SyntaxToken(sep,lout);
		}
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpWORD, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpSEPR, SP_EAT, SP_ADD, 0),
			};
		return new SyntaxToken(sep,lout);
	}

	protected SyntaxToken sep0(String sep)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return new SyntaxToken(sep,lout);
	}

	protected SyntaxToken sep_nl(String sep)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpWORD, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpSEPR, SP_EAT, SP_ADD, 0),
				new SpaceCmd(siNl,     SP_NOP, SP_ADD, 0),
			};
		return new SyntaxToken(sep,lout);
	}

	protected SyntaxToken sep(String sep, SpaceCmd[] spaces)
	{
		return new SyntaxToken(sep,spaces);
	}
	
	protected SyntaxToken oper(Operator op)
	{
		return oper(op.toString());
	}

	protected SyntaxToken oper(String op)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpOPER, SP_ADD, SP_ADD, 0)
			};
		return new SyntaxToken(op.intern(),lout);
	}

	protected SyntaxOptional opt(String name)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return opt(new CalcOptionNotNull(name),attr(name),null,lout);
	}
	
	protected SyntaxOptional opt(String name, SyntaxElem opt_true)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return opt(new CalcOptionNotNull(name),opt_true,null,lout);
	}

	protected SyntaxOptional opt(CalcOption calc, SyntaxElem opt_true, SyntaxElem opt_false, SpaceCmd[] spaces)
	{
		return new SyntaxOptional(calc,opt_true,opt_false,spaces);
	}

	protected SyntaxIntChoice alt_int(String name, SyntaxElem... options)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		SyntaxIntChoice sc = new SyntaxIntChoice(name,lout);
		sc.elements.addAll(options);
		return sc;
	}

	protected SyntaxEnumChoice alt_enum(String name, SyntaxElem... options)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		SyntaxEnumChoice sc = new SyntaxEnumChoice(name,lout);
		sc.elements.addAll(options);
		return sc;
	}

	protected SyntaxFolder folder(boolean folded_by_default, SyntaxElem folded, SyntaxElem unfolded, SpaceCmd[] spaces)
	{
		return new SyntaxFolder(folded_by_default,folded,unfolded,spaces);
	}

	protected SyntaxJavaExpr expr(int idx, int priority)
	{
		SyntaxJavaExpr se = new SyntaxJavaExpr(idx, priority, seExprTemplate);
		return se;
	}

	protected SyntaxJavaExpr expr(String expr, int priority)
	{
		SyntaxJavaExpr se = new SyntaxJavaExpr(expr, priority, seExprTemplate);
		return se;
	}

	protected SyntaxSet expr(Operator op)
	{
		SyntaxElem[] elems = new SyntaxElem[op.args.length];
		int earg = 0;
		for (int i=0; i < elems.length; i++) {
			OpArg arg = op.args[i];
			switch (arg) {
			case OpArg.EXPR(int priority):
				elems[i] = expr(earg,priority);
				earg++;
				continue;
			case OpArg.TYPE():
				elems[i] = expr(earg,255);
				earg++;
				continue;
			case OpArg.OPER(String text):
				elems[i] = oper(text);
				continue;
			}
		}
		SyntaxSet set = new SyntaxSet(new SpaceCmd[0]);
		set.elements.addAll(elems);
		return set;
	}

	public JavaSyntax() {
		SpaceCmd[] lout_empty = new SpaceCmd[0];
		SpaceCmd[] lout_nl = new SpaceCmd[] {new SpaceCmd(siNl,SP_NOP,SP_ADD,0)};
		SpaceCmd[] lout_nl_nl = new SpaceCmd[] {new SpaceCmd(siNl,SP_ADD,SP_ADD,0)};
		SpaceCmd[] lout_nl_grp = new SpaceCmd[] {new SpaceCmd(siNlGrp,SP_NOP,SP_ADD,0)};
		
		seExprTemplate = new SyntaxJavaExprTemplate();
		
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
					lst("args", node(/*new FormatInfoHint("class-arg")*/), sep(","), lout_empty),
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

		exprs = new Hashtable<Operator, SyntaxElem>();
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
			if (sed != null)
				return sed.elem;
		}
		switch (node) {
		case Struct: {
			Struct s = (Struct)node;
			if (s.isEnum())
				return seStructEnum;
			if (s.isPizzaCase())
				return seStructCase;
			if (s.isSyntax())
				return seStructSyntax;
			if (s.isStructView())
				return seStructView;
			if (s.isAnnotation())
				return seStructAnnotation;
			if (s.isInterface())
				return seStructInterface;
			//if (hint != null && "anonymouse".equals(hint.text))
			//	return seStructBody;
			return seStructClass;
		}

		case TypeExpr:
		{
			TypeExpr e = (TypeExpr)node;
			Operator op = e.getOp();
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				se = expr(op);
				exprs.put(op, se);
			}
			return se;
		}
		case ENode:
		{
			ENode e = (ENode)node;
			Operator op = e.getOp();
			if (op == null)
				break;
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				se = expr(op);
				exprs.put(op, se);
			}
			return se;
		}
		}
		return super.getSyntaxElem(node);
	}
}

