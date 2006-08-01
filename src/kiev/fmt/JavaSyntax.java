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

	public SyntaxJavaComment() {}
	public SyntaxJavaComment(SpaceCmd[] spaces) {
		super(spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaComment(node, this);
		return dr;
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
class CalcOptionUpperBound extends CalcOption {
	CalcOptionUpperBound() {}
	public boolean calc(ANode node) {
		if !(node instanceof TypeConstr) return false;
		TypeConstr tc = (TypeConstr)node;
		if (tc.super_types.length == 0) return false;
		if (tc.super_types.length == 1 && tc.super_types[0].getType() ≈ Type.tpObject) return false;
		return true;
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
class CalcOptionPackedField extends CalcOption {
	CalcOptionPackedField() {}
	public boolean calc(ANode node) {return ((Field)node).isPackedField();}
}
@node
class CalcOptionSType extends CalcOption {
	CalcOptionSType() {}
	public boolean calc(ANode node) {
		FormPar fp = (FormPar)node;
		return fp.stype != null && fp.vtype.getType() ≉ fp.stype.getType();
	}
}
@node
class CalcOptionNotSuper extends CalcOption {
	CalcOptionNotSuper() {}
	public boolean calc(ANode node) { return !((ENode)node).isSuperExpr(); }
}

@node
public class JavaSyntax extends TextSyntax {

	final SyntaxJavaExprTemplate seExprTemplate;
	
	final SyntaxElem seFileUnit;
	final SyntaxElem seStructClass;
	final SyntaxElem seStructInterface;
	final SyntaxElem seStructAnnotation;
	final SyntaxElem seStructEnum;
	final SyntaxElem seStructCase;
	final SyntaxElem seStructSyntax;
	final SyntaxElem seStructView;
	final SyntaxElem seStructBody;
	final SyntaxElem seVarDecl;
	final SyntaxElem seVar;
	final SyntaxElem seVarNoType;
	final SyntaxElem seFormPar;
	final SyntaxElem seConstructor;
	final SyntaxElem seMethod;
	final SyntaxElem seInitializer;
	final SyntaxElem seRuleMethod;
	final SyntaxElem seMethodAlias;
	final SyntaxElem seOperatorAlias;
	final SyntaxElem seWBCCondition;

	final SyntaxElem seTypeRef;
	final SyntaxElem seTypeNameRef;
	final SyntaxElem seTypeClosureRef;
	// lvalues
	final SyntaxElem seThisExpr;
	final SyntaxElem seOuterThisAccessExpr;
	final SyntaxElem seReinterpExpr;
	// boolean exprs
	final SyntaxElem seInstanceofExpr;
	final SyntaxElem seCallConstr;
	// new expr
	final SyntaxElem seNewArrayExpr;
	final SyntaxElem seNewInitializedArrayExpr;
	final SyntaxElem seNewClosure;
	// rewrites
	final SyntaxElem seRewriteMatch;
	final SyntaxElem seRewritePattern;
	final SyntaxElem seRewriteCase;
	final SyntaxElem seRewriteNodeFactory;
	final SyntaxElem seRewriteNodeArg;
	final SyntaxElem seRewriteNodeArgArray;
	// others exprs
	final SyntaxElem seShadow;
	final SyntaxElem seTypeClassExpr;
	final SyntaxElem seTypeInfoExpr;
	final SyntaxElem seAssertEnabledExpr;
	final SyntaxElem seCastExpr;
	final SyntaxElem seNopExpr;

	final SyntaxElem seComment;
	final SyntaxElem seCommentNl;
	final SyntaxElem seCommentNlBefore;
	final SyntaxElem seCommentNlAfter;
	
	
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
			SpaceCmd[] lout_pkg = new SpaceCmd[] {
					new SpaceCmd(siNlGrp, SP_NOP, SP_ADD, 0)
				};
			// file unit
			seFileUnit = setl(lout_nl,
					opt("pkg", setl(lout_pkg, kw("package"), attr("pkg"), sep(";"))),
					lst("members", lout_nl)
				);
		}
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
		{
			// vars
			seVarDecl = set(attr("meta"),
				attr("vtype"), ident("id"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))),
				sep(";"));
			seVar = set(attr("meta"),
				attr("vtype"), ident("id"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority)))
				);
			seVarNoType = set(ident("id"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))));
			// formal parameter
			seFormPar = set(attr("meta"),
				attr("vtype"),
				opt(new CalcOptionSType(),
					set(sep0(":"), attr("stype")),
					null,
					lout_empty
					),
				ident("id")
				);
		}
		{
			SpaceCmd[] lout_method_type_args = new SpaceCmd[] {
					new SpaceCmd(siSp, SP_NOP, SP_ADD, 0)
				};
			SpaceCmd[] lout_method = new SpaceCmd[] {
					new SpaceCmd(siNlGrp, SP_NOP, SP_ADD, 0)
				};
			SyntaxList method_params = lst("params",node(),sep(","),lout_empty);
			method_params.filter = new CalcOptionJavaFlag("synthetic", 1, 12, 1);
			SyntaxElem method_type_args = opt(new CalcOptionNotEmpty("targs"),
				set(
					sep("<"),
					lst("targs", node(/*new FormatInfoHint("class-arg")*/), sep(","), lout_empty),
					sep(">")
				), null, lout_method_type_args);
			// constructor
			seConstructor = setl(lout_method,
				setl(lout_empty, attr("meta"),
					ident("id"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("conditions", lout_empty)),
				attr("body")
				);
			// method
			seMethod = setl(lout_method,
				setl(lout_empty, attr("meta"),
					method_type_args.ncopy(),
					attr("type_ret"), ident("id"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("aliases", lout_empty)),
				par(plIndented, lst("conditions", lout_empty)),
				opt(new CalcOptionNotNull("body"), attr("body"), sep(";"), lout_empty)
				);
			// logical rule method
			seRuleMethod = setl(lout_method,
				setl(lout_nl, attr("meta"),
					method_type_args.ncopy(),
					kw("rule"), ident("id"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("aliases", lout_empty)),
				par(plIndented, lst("localvars", setl(lout_nl, node(), sep(";")), null, lout_nl)),
				opt(new CalcOptionNotNull("body"), attr("body"), sep(";"), lout_empty)
				);
			seInitializer = setl(lout_method, attr("meta"), attr("body"));
			
			seMethodAlias = setl(lout_nl_nl, kw("alias"), attr("name"));
			seOperatorAlias = setl(lout_nl_nl,
				kw("alias"),
				alt_int("opmode",
					kw("lfy"),
					kw("xfx"),
					kw("xfy"),
					kw("yfx"),
					kw("yfy"),
					kw("xf"),
					kw("yf"),
					kw("fx"),
					kw("fy"),
					kw("xfxfy")
					),
				kw("operator"),
				attr("image")
				);
			
			seWBCCondition = opt("body",
				setl(lout_nl_nl,
					alt_enum("cond", kw("error"), kw("require"), kw("ensure"), kw("invariant")),
					opt("id", set(sep("["), ident("id"), sep("]"))),
					attr("body")
				));
		}

		exprs = new Hashtable<Operator, SyntaxElem>();
		seTypeRef = ident("ident");
		seTypeNameRef = set(
				opt("outer", set(attr("outer"),sep("."))),
				ident("ident"),
				opt(new CalcOptionNotEmpty("args"),
					set(
						sep("<"),
						lst("args", node(), sep(","), lout_empty),
						sep(">")
					), null, lout_empty
				)
			);
		seTypeClosureRef = set(
			sep("("),
			lst("args", node(), sep(","), lout_empty),
			sep(")->"),
			attr("ret")
			);

		seThisExpr = opt(new CalcOptionNotSuper(),
						kw("this"),
						kw("super"),
						lout_empty
						);
		seOuterThisAccessExpr = set(attr("outer"), sep("."), kw("this"));
		seReinterpExpr = set(sep("("), kw("$reinterp"), attr("type"), sep(")"), expr("expr", Constants.opCastPriority));
		
		seInstanceofExpr = //expr("expr", Operator.InstanceOf, "type");
			set(
			expr("expr", Constants.opInstanceOfPriority),
			kw("instanceof"),
			attr("type")
			);
		seCallConstr = set(
				opt(new CalcOptionNotSuper(),
					kw("this"),
					kw("super"),
					lout_empty
					),
				sep("("),
				lst("args",node(),sep(","),lout_empty),
				sep(")")
				);

		seNewArrayExpr = set(
				kw("new"),
				attr("type"),
				lst("args",
					set(
						sep("["),
						node(),
						sep("]")
					),
					null,
					lout_empty
					)
				);
		seNewInitializedArrayExpr = set(
				kw("new"),
				attr("type"),
				sep("{"),
				lst("args",node(),sep(","),lout_empty),
				sep("}")
				);
		seNewClosure = set(
				kw("fun"),
				sep("("),
				lst("params",node(),sep(","),lout_empty),
				sep(")"),
				sep("->"),
				attr("type_ret"),
				attr("body")
				);
		{
			seRewriteMatch = set(
					sep("{", lout_nl),
					par(plIndented, lst("cases",setl(lout_nl,node()),null,lout_nl)),
					sep("}")
					);
			seRewriteCase = set(
					kw("case"),
					attr("var"),
					sep(":", lout_nl),
					par(plIndented, lst("stats",setl(lout_nl,node(/*new FormatInfoHint("stat")*/)),null,lout_nl))
					);
			SpaceCmd[] lout_pattern_args = new SpaceCmd[] {
					new SpaceCmd(siSp, SP_NOP, SP_ADD, 0)
				};
			seRewritePattern = set(
					attr("meta"),
					attr("vtype"),
					ident("id"),
					opt(new CalcOptionNotEmpty("vars"),
						set(
							sep("("),
							lst("vars", node(), sep(","), lout_empty),
							sep(")")
						), null, lout_pattern_args
					)
				);
			seRewriteNodeFactory = set(
					kw("new"),
					oper("#"),
					attr("type"),
					sep("("),
					lst("args",node(),sep(","),lout_empty),
					sep(")")
					);
			seRewriteNodeArg = set(ident("attr"), oper("="), attr("node"));
			seRewriteNodeArgArray = set(
				sep("{"),
				lst("args",node(),sep(","),lout_empty),
				sep("}")
				);
		}


		seShadow = attr("node");
		seTypeClassExpr = set(attr("type"), sep("."), kw("class"));
		seTypeInfoExpr = set(attr("type"), sep("."), kw("type"));
		seAssertEnabledExpr = kw("$assertionsEnabled");
		seCastExpr = set(sep("("), kw("$cast"), attr("type"), sep(")"), expr("expr", Constants.opCastPriority));
		seNopExpr = new SyntaxSpace(new SpaceCmd[0]);
		seNopExpr.fmt.is_hidden = true;
		
		SpaceCmd[] lout_comment = new SpaceCmd[] {
					new SpaceCmd(siSp, SP_ADD, SP_ADD, 0),
				};
		seComment = new SyntaxJavaComment(lout_comment);

		SpaceCmd[] lout_comment_nl = new SpaceCmd[] {
					new SpaceCmd(siNl, SP_ADD, SP_ADD, 0),
					new SpaceCmd(siSp, SP_ADD, SP_ADD, 0),
				};
		seCommentNl = new SyntaxJavaComment(lout_comment_nl);

		SpaceCmd[] lout_comment_nl_before = new SpaceCmd[] {
					new SpaceCmd(siNl, SP_ADD, SP_NOP, 0),
					new SpaceCmd(siSp, SP_ADD, SP_ADD, 0),
				};
		seCommentNlBefore = new SyntaxJavaComment(lout_comment_nl_before);

		SpaceCmd[] lout_comment_nl_after = new SpaceCmd[] {
					new SpaceCmd(siNl, SP_NOP, SP_ADD,  0),
					new SpaceCmd(siSp, SP_ADD, SP_ADD, 0),
				};
		seCommentNlAfter = new SyntaxJavaComment(lout_comment_nl_after);
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
		case FileUnit: return seFileUnit;
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
		case FormPar: return seFormPar;
		case Var:
			//if (hint != null) {
			//	if ("no-type".equals(hint.text))
			//		return seVarNoType;
			//	if ("stat".equals(hint.text))
			//		return seVarDecl;
			//}
			return seVar;
		case RuleMethod: return seRuleMethod;
		case Constructor: return seConstructor;
		case Method: return seMethod;
		case Initializer: return seInitializer;
		case ASTIdentifierAlias: return seMethodAlias;
		case ASTOperatorAlias: return seOperatorAlias;
		case WBCCondition: return seWBCCondition;

		case TypeRef: return seTypeRef;
		case TypeNameRef: return seTypeNameRef;
		case TypeClosureRef: return seTypeClosureRef;

		case Shadow: return seShadow;

		case ThisExpr: return seThisExpr;
		case OuterThisAccessExpr: return seOuterThisAccessExpr;
		case ReinterpExpr: return seReinterpExpr;
		
		case InstanceofExpr: return seInstanceofExpr;
//		case CallExpr:
//			if (((CallExpr)node).func instanceof Constructor)
//				return seCallConstr;
//			return seCallExpr;
		case TypeClassExpr: return seTypeClassExpr;
		case TypeInfoExpr: return seTypeInfoExpr;
		case AssertEnabledExpr: return 	seAssertEnabledExpr;

		case CastExpr: return seCastExpr;
		case NopExpr: return seNopExpr;

		case NewArrayExpr: return seNewArrayExpr;
		case NewInitializedArrayExpr: return seNewInitializedArrayExpr;
		case NewClosure: return seNewClosure;
		
		case RewriteMatch: return seRewriteMatch;
		case RewriteCase: return seRewriteCase;
		case RewritePattern: return seRewritePattern;
		case RewriteNodeFactory: return seRewriteNodeFactory;
		case RewriteNodeArg: return seRewriteNodeArg;
		case RewriteNodeArgArray: return seRewriteNodeArgArray;

		case Comment: {
			Comment c = (Comment)node;
			if (c.doc_form || c.multiline) return seCommentNl;
			if (c.nl_before && c.nl_after) return seCommentNl;
			if (c.eol_form || c.nl_after) return seCommentNlAfter;
			if (c.nl_before) return seCommentNlBefore;
			return seComment;
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

