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
public class SyntaxJavaExpr extends SyntaxAttr {
	@virtual typedef This  = SyntaxJavaExpr;

	@att public int					priority;
	@att public SyntaxSeparator		l_paren;
	@att public SyntaxSeparator		r_paren;
	public FormatInfoHint			hint;

	public SyntaxJavaExpr() {}
	public SyntaxJavaExpr(String name, FormatInfoHint hint, DrawLayout layout, int priority, SyntaxSeparator l_paren, SyntaxSeparator r_paren) {
		super(name,layout);
		this.hint = hint;
		this.priority = priority;
		this.l_paren = l_paren;
		this.r_paren = r_paren;
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		ASTNode n = (name == "this") ? node : (ASTNode)node.getVal(name);
		Drawable dr = new DrawJavaExpr(n, this);
		dr.init(fmt);
		return dr;
	}
}


@node
public class SyntaxJavaAccess extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaAccess;

	public SyntaxJavaAccess() {}
	public SyntaxJavaAccess(DrawLayout layout) {
		super(layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawJavaAccess(node, this);
		dr.init(fmt);
		return dr;
	}
}


@node
public class SyntaxJavaType extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaType;

	public FormatInfoHint hint;
	
	public SyntaxJavaType() {}
	public SyntaxJavaType(FormatInfoHint hint, DrawLayout layout) {
		super(layout);
		this.hint = hint;
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawJavaType(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxJavaEnumAlias extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaEnumAlias;

	public SyntaxJavaEnumAlias() {}
	public SyntaxJavaEnumAlias(DrawLayout layout) {
		super(layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawJavaEnumAlias(node, this);
		dr.init(fmt);
		return dr;
	}
}


@node
public class SyntaxJavaPackedField extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaPackedField;

	public SyntaxJavaPackedField() {}
	public SyntaxJavaPackedField(DrawLayout layout) {
		super(layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawJavaPackedField(node, this);
		dr.init(fmt);
		return dr;
	}
}


public class CalcOptionJavaFlag implements CalcOption {
	private final int mask;
	private final int offs;
	private final int val;
	public CalcOptionJavaFlag(int size, int offs, int val) {
		this.mask = (-1 << (32-size)) >>> (32-size);
		this.offs = offs;
		this.val = val;
	} 
	public boolean calc(ASTNode node) {
		if (node == null || !(node instanceof DNode)) return false;
		int f = ((DNode)node).flags >>> offs;
		f &= mask;
		return f == val;
	}
}


public class JavaSyntax extends Syntax {
	final SyntaxElem seFileUnit;
	final SyntaxElem seStructClass;
	final SyntaxElem seStructInterface;
	final SyntaxElem seStructAnnotation;
	final SyntaxElem seStructEnum;
	final SyntaxElem seStructCase;
	final SyntaxElem seStructSyntax;
	final SyntaxElem seStructView;
	final SyntaxElem seStructBody;
	final SyntaxElem seImport;
	final SyntaxElem seOpdef;
	final SyntaxElem seMetaSet;
	final SyntaxElem seMeta;
	final SyntaxElem seMetaValueScalar;
	final SyntaxElem seMetaValueArray;
	final SyntaxElem seTypeAssign;
	final SyntaxElem seTypeConstr;
	final SyntaxElem seTypeConstrClassArg;
	final SyntaxElem seFieldDecl;
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

	final SyntaxElem seExprStat;
	final SyntaxElem seReturnStat;
	final SyntaxElem seThrowStat;
	final SyntaxElem seIfElseStat;
	final SyntaxElem seCondStat;
	final SyntaxElem seLabeledStat;
	final SyntaxElem seBreakStat;
	final SyntaxElem seContinueStat;
	final SyntaxElem seGotoStat;
	final SyntaxElem seGotoCaseStat;

	final SyntaxElem seWhileStat;
	final SyntaxElem seDoWhileStat;
	final SyntaxElem seForInit;
	final SyntaxElem seForStat;
	final SyntaxElem seForEachStat;
	
	final SyntaxElem seCaseLabel;
	final SyntaxElem seSwitchStat;
	final SyntaxElem seCatchInfo;
	final SyntaxElem seFinallyInfo;
	final SyntaxElem seTryStat;
	final SyntaxElem seSynchronizedStat;
	final SyntaxElem seWithStat;
	
	final SyntaxElem seBlock;
	final SyntaxElem seRuleBlock;
	final SyntaxElem seRuleOrExpr;
	final SyntaxElem seRuleAndExpr;
	final SyntaxElem seTypeRef;
	final SyntaxElem seStructRef;
	final SyntaxElem seConstExpr;
	final SyntaxElem seConstExprTrue;
	final SyntaxElem seConstExprFalse;
	final SyntaxElem seConstExprNull;
	final SyntaxElem seConstExprChar;
	final SyntaxElem seConstExprStr;
	// rule nodes
	final SyntaxElem seRuleIstheExpr;
	final SyntaxElem seRuleIsoneofExpr;
	final SyntaxElem seRuleCutExpr;
	final SyntaxElem seRuleCallExpr;
	final SyntaxElem seRuleWhileExpr;
	final SyntaxElem seRuleExpr;
	// lvalues
	final SyntaxElem seAccessExpr;
	final SyntaxElem seIFldExpr;
	final SyntaxElem seContainerAccessExpr;
	final SyntaxElem seThisExpr;
	final SyntaxElem seLVarExpr;
	final SyntaxElem seSFldExpr;
	final SyntaxElem seOuterThisAccessExpr;
	final SyntaxElem seReinterpExpr;
	// boolean exprs
	final SyntaxElem seBinaryBooleanOrExpr;
	final SyntaxElem seBinaryBooleanAndExpr;
	final SyntaxElem seInstanceofExpr;
	final SyntaxElem seBooleanNotExpr;
	final SyntaxElem seCallExpr;
	final SyntaxElem seCallConstr;
	final SyntaxElem seClosureCallExpr;
	// new expr
	final SyntaxElem seNewExpr;
	final SyntaxElem seNewArrayExpr;
	final SyntaxElem seNewInitializedArrayExpr;
	final SyntaxElem seNewClosure;
	// others exprs
	final SyntaxElem seShadow;
	final SyntaxElem seArrayLengthExpr;
	final SyntaxElem seTypeClassExpr;
	final SyntaxElem seTypeInfoExpr;
	final SyntaxElem seStringConcatExpr;
	final SyntaxElem seCommaExpr;
	final SyntaxElem seConditionalExpr;
	final SyntaxElem seCastExpr;
	final SyntaxElem seNopExpr;
	
	final Hashtable<Operator, SyntaxElem> exprs;
	
	public SpaceInfo siNlOrBlock = new SpaceInfo("nl-block",       SP_NEW_LINE, 1,  1);

	public SpaceInfo siFldGrpNl  = new SpaceInfo("indent-block",   SP_NEW_LINE, 2, 20);
	
	public ParagraphLayout plStatIndented = new ParagraphLayoutBlock("stat-indented", 4, 20);

	protected SyntaxElem jflag(int size, int offs, int val, String name)
	{
		DrawLayout lout = new DrawLayout();
		return new SyntaxOptional(name, new CalcOptionJavaFlag(size, offs, val), kw(name), null, lout);
	}

	protected SyntaxJavaExpr expr(String expr, int priority)
	{
		DrawLayout lout = new DrawLayout();
		SyntaxJavaExpr se = new SyntaxJavaExpr(expr, null, lout, priority, sep("("), sep(")"));
		return se;
	}

	protected SyntaxJavaExpr expr(String expr, FormatInfoHint hint, int priority)
	{
		DrawLayout lout = new DrawLayout();
		SyntaxJavaExpr se = new SyntaxJavaExpr(expr, hint, lout, priority, sep("("), sep(")"));
		return se;
	}

	protected SyntaxSet expr(String expr1, Operator op, String expr2)
	{
		DrawLayout lout = new DrawLayout();
		return setl(lout, expr(expr1, op.getArgPriority(0)), oper(op), expr(expr2, op.getArgPriority(1)));
	}

	protected SyntaxSet expr(Operator op, String expr2)
	{
		DrawLayout lout = new DrawLayout();
		return setl(lout, oper(op), expr(expr2, op.getArgPriority()));
	}

	protected SyntaxSet expr(String expr1, Operator op)
	{
		DrawLayout lout = new DrawLayout();
		return setl(lout, expr(expr1, op.getArgPriority()), oper(op));
	}
	
	protected SyntaxElem accs() {
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSp, SP_ADD_BEFORE, 0),
				new SpaceCmd(siSp, SP_ADD_AFTER, 0),
			});
		return new SyntaxJavaAccess(lout);
	}
	
	protected SyntaxSeparator sep(String sep)
	{
		if (sep == ";") {
			DrawLayout lout = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siSpWORD, SP_EAT_BEFORE, 0),
					new SpaceCmd(siSpSEPR, SP_EAT_BEFORE, 0),
					new SpaceCmd(siSp,     SP_ADD_AFTER, 0),
				});
				return new SyntaxSeparator(sep,lout);
		}
		if (sep == "{" || sep == "}") {
			DrawLayout lout = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siSp, SP_ADD_AFTER, 0),
					new SpaceCmd(siSp, SP_ADD_AFTER, 0),
				});
			return new SyntaxSeparator(sep,lout);
		}
		return super.sep(sep);
	}
	
	protected SyntaxJavaType type(FormatInfoHint hint) {
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpWORD, SP_ADD_BEFORE, 0),
				new SpaceCmd(siSpSEPR, SP_ADD_AFTER, 0),
				new SpaceCmd(siSpWORD, SP_ADD_AFTER, 0),
			});
		return new SyntaxJavaType(hint,lout);
	}

	public JavaSyntax() {
		DrawLayout lout_empty = new DrawLayout();
		DrawLayout lout_nl = new DrawLayout(new SpaceCmd[]{new SpaceCmd(siNl,SP_ADD_AFTER,0)});
		DrawLayout lout_nl_nl = new DrawLayout(new SpaceCmd[]{new SpaceCmd(siNl,SP_ADD_BEFORE,0),new SpaceCmd(siNl,SP_ADD_AFTER,0)});
		DrawLayout lout_nl_grp = new DrawLayout(new SpaceCmd[]{new SpaceCmd(siNlGrp,SP_ADD_AFTER,0)});
		{
			DrawLayout lout_pkg = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlGrp, SP_ADD_AFTER, 0)
				});
			DrawLayout lout_syntax = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlGrp, SP_ADD_AFTER, 0)
				});
			// file unit
			seFileUnit = setl(lout_nl.ncopy(),
					opt("pkg", setl(lout_pkg, kw("package"), ident("pkg"), sep(";"))),
					lst("syntax", lout_syntax),
					lst("members", lout_empty.ncopy())
				);
		}
		{
			SyntaxElem sp_hid = new SyntaxSpace(new DrawLayout());
			sp_hid.is_hidden = true;
			// import
			seImport = setl(lout_nl.ncopy(),
				kw("import"),
				alt_enum("mode",
					sp_hid.ncopy(),
					kw("static"),
					kw("package"),
					kw("syntax")
					),
				ident("name"),
				opt("star",new CalcOptionTrue("star"), sep(".*"), null, lout_empty.ncopy()),
				sep(";"));
			seOpdef = setl(lout_nl.ncopy(),
				kw("operator"),
				ident("image"),
				sep(","),
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
				sep(","),
				ident("prior"),
				sep(";")
				);
			SyntaxElem typedef_prefix = setl(lout_empty.ncopy(),
					opt("meta"),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,17,1, "@virtual"),
//					jflag(1,18,1, "@unerasable"),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,10,1, "abstract")
					);
			seTypeAssign = setl(lout_nl.ncopy(), typedef_prefix.ncopy(), kw("typedef"), ident("name"), oper("="), attr("type_ref"), sep(";"));
			
			seTypeConstrClassArg = setl(lout_empty.ncopy(), ident("name"),
				opt("upper_bound",
					new CalcOption() {
						public boolean calc(ASTNode node) {
							if !(node instanceof TypeConstr) return false;
							TypeConstr tc = (TypeConstr)node;
							if (tc.upper_bound.size() == 0) return false;
							if (tc.upper_bound.size() == 1 && tc.upper_bound[0].getType() ≈ Type.tpObject) return false;
							return true;
						}
					},
					set(
						kw("extends"),
						lst("upper_bound", node(), oper("&"), lout_empty.ncopy())
						),
					null, lout_empty.ncopy()
					),
				opt("lower_bound", new CalcOptionNotEmpty("lower_bound"),
					set(
						kw("super"),
						lst("lower_bound", node(), sep("&"), lout_empty.ncopy())
						),
					null, lout_empty.ncopy()
					)
				);
			seTypeConstr = setl(lout_nl.ncopy(), typedef_prefix.ncopy(), kw("typedef"), ident("name"), 
				lst("upper_bound", set(oper("\u2264"), node()), null, lout_empty.ncopy()),
				lst("lower_bound", set(oper("\u2265"), node()), null, lout_empty.ncopy()),
				sep(";")
				);
		}
		{
			DrawLayout lout_struct = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlGrp, SP_ADD_AFTER, 0)
				});
			DrawLayout lout_struct_hdr = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlOrBlock, SP_ADD_AFTER, 0)
				});
			DrawLayout lout_struct_block_start = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlOrBlock, SP_EAT_BEFORE, 0),
					new SpaceCmd(siNl,        SP_ADD_AFTER,  0),
					new SpaceCmd(siSp,        SP_ADD_BEFORE, 0),
				});
			DrawLayout lout_struct_block_end = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNl,        SP_ADD_BEFORE, 0),
					new SpaceCmd(siNl,        SP_ADD_AFTER,  0),
					new SpaceCmd(siSp,        SP_ADD_BEFORE, 0),
				});
			SyntaxElem struct_prefix = setl(lout_struct_hdr.ncopy(),
					attr("meta"),
					opt("singleton", new CalcOption() {public boolean calc(ASTNode node) {return ((Struct)node).isSingleton();}}, kw("@singleton"), null, lout_empty.ncopy()),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,18,1, "@unerasable"),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,10,1, "abstract"),
					jflag(1,11,1, "strict")
					);
			SyntaxElem struct_args = opt("args", new CalcOptionNotEmpty("args"),
				set(
					sep("<"),
					lst("args", node(new FormatInfoHint("class-arg")), sep(","), lout_empty.ncopy()),
					sep(">")
				), null, lout_empty.ncopy());
			DrawLayout lout_ext = new DrawLayout(new SpaceCmd[]{new SpaceCmd(siSp, SP_ADD_BEFORE, 0)});
			SyntaxElem class_ext = opt("extends",
				new CalcOption(){
					public boolean calc(ASTNode node) {
						if (node instanceof Struct && node.super_bound != null && node.super_bound.getType() != Type.tpObject)
							return true;
						return false;
					}
				},
				set(
					kw("extends"), ident("super_bound")
					),
				null, lout_ext.ncopy()
				);
			SyntaxElem class_impl = opt("implements", new CalcOptionNotEmpty("interfaces"),
				set(
					kw("implements"),
					lst("interfaces", node(), sep(","), lout_empty.ncopy())
					),
				null, lout_ext.ncopy()
				);
			SyntaxElem iface_ext = opt("extends", new CalcOptionNotEmpty("interfaces"),
				set(
					kw("extends"),
					lst("interfaces", node(), sep(","), lout_empty.ncopy())
					),
				null, lout_ext.ncopy()
				);
			SyntaxList struct_members = lst("members",lout_empty.ncopy());
			struct_members.filter = new CalcOption() {
				public boolean calc(ASTNode node) {
					if (node instanceof DNode && node.isSynthetic())
						return false;
					return true;
				}
			};
			// anonymouse struct
			seStructBody = set(
					sep("{", lout_struct_block_start),
					par(plIndented, struct_members),
					sep("}", lout_struct_block_end)
					);
			// class
			seStructClass = set(
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("class"),
						ident("short_name"),
						struct_args.ncopy(),
						class_ext.ncopy(),
						class_impl.ncopy()),
					seStructBody.ncopy()
				);
			// interface
			seStructInterface = set(
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("interface"),
						ident("short_name"),
						struct_args.ncopy(),
						iface_ext),
					seStructBody.ncopy()
				);
			// view
			seStructView = set(
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("view"),
						ident("short_name"),
						struct_args.ncopy(),
						kw("of"),
						ident("view_of"),
						class_ext.ncopy(),
						class_impl.ncopy()),
					seStructBody.ncopy()
				);
			// annotation
			seStructAnnotation = set(
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("@interface"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// syntax
			seStructSyntax = set(
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("syntax"),
						ident("short_name")),
					seStructBody.ncopy()
				);

			// case
			SyntaxElem case_prefix = setl(lout_struct_hdr.ncopy(),
					attr("meta"),
					jflag(1,18,1, "@unerasable")
					);
			SyntaxList case_fields = lst("members",
				set(attr("meta"), ident("ftype"), ident("name")),
				sep(","),
				lout_empty.ncopy()
				);
			case_fields.filter = new CalcOption() {
				public boolean calc(ASTNode node) { return node instanceof Field && !node.isSynthetic(); }
			};
			seStructCase = setl(lout_nl_grp.ncopy(),
					case_prefix,
					accs(),
					kw("case"),
					ident("short_name"),
					struct_args.ncopy(),
					opt("singleton",
						new CalcOption() {
							public boolean calc(ASTNode node) { return !((Struct)node).isSingleton(); }
						},
						set(
							sep("("),
							case_fields,
							sep(")")
							),
						null,
						lout_empty.ncopy()
						),
					sep(";")
				);
			// enum
			SyntaxList enum_fields = lst("members",
				set(
					attr("name"),
					opt("alias",
						new CalcOption() {
							public boolean calc(ASTNode node) {
								return ((Field)node).getMetaAlias() != null;
							}
						},
						set(sep(":"), new SyntaxJavaEnumAlias(lout_empty.ncopy())),
						null,
						lout_empty.ncopy()
						)
					),
				sep_nl(","),
				lout_empty.ncopy());
			enum_fields.filter = new CalcOption() {
				public boolean calc(ASTNode node) {
					if (node instanceof Field && node.isEnumField())
						return true;
					return false;
				}
			};
			SyntaxList enum_members = lst("members",lout_empty.ncopy());
			enum_members.filter = new CalcOption() {
				public boolean calc(ASTNode node) {
					if ((node instanceof DNode && node.isSynthetic()) || (node instanceof Field && node.isEnumField()))
						return false;
					return true;
				}
			};
			seStructEnum = set(
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("enum"),
						ident("short_name")),
					set(
						sep("{", lout_struct_block_start.ncopy()),
						par(plIndented, enum_fields),
						sep(";"),
						new SyntaxSpace(lout_nl_grp.ncopy()),
						par(plIndented, enum_members),
						sep("}", lout_struct_block_end.ncopy())
					)
				);
		}
		{
			seMetaSet = lst("metas", lout_empty.ncopy());
			seMeta = setl(lout_nl.ncopy(), oper("@"), ident("type"),
						set(
							sep("("),
							lst("values",node(),sep(","),lout_empty.ncopy()),
							sep(")")
							)
						);
			seMetaValueScalar = set(ident("type"), oper("="), attr("value"));
			seMetaValueArray = set(ident("type"), oper("="),
						set(sep("{"),
							lst("values",node(),sep(","),lout_empty.ncopy()),
							sep("}")
							)
						);
		}
		{
			DrawLayout lout_field = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siFldGrpNl, SP_EAT_BEFORE,0),
					new SpaceCmd(siFldGrpNl, SP_ADD_AFTER, 0),
					new SpaceCmd(siNl,       SP_ADD_AFTER, 0),
				});
			SyntaxElem field_prefix = setl(lout_empty.ncopy(),
					attr("meta"),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,17,1, "@virtual"),
					opt("packed",
						new CalcOption() {
							public boolean calc(ASTNode node) {return ((Field)node).isPackedField();}
						},
						new SyntaxJavaPackedField(lout_empty.ncopy()),
						null,
						lout_empty.ncopy()
						),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,6,1,  "volatile"),
					jflag(1,7,1,  "transient"),
					jflag(1,10,1, "abstract"),
					jflag(1,11,1, "strict")
					);
			SyntaxElem var_prefix = setl(lout_empty.ncopy(),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,4,1,  "final")
					);
			// field
			seFieldDecl = setl(lout_field.ncopy(), field_prefix, accs(),
				ident("ftype"), ident("name"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))), sep(";")
				);
			// vars
			seVarDecl = set(attr("var"), sep(";"));
			seVar = set(opt("meta"), var_prefix.ncopy(),
				ident("vtype"), ident("name"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority)))
				);
			seVarNoType = set(ident("name"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))));
			// formal parameter
			seFormPar = set(opt("meta"),
				var_prefix.ncopy(),
				attr("vtype"),
				opt("stype",
					new CalcOption() {
						public boolean calc(ASTNode node) {
							FormPar fp = (FormPar)node;
							return fp.stype != null && fp.vtype.getType() ≉ fp.stype.getType();
						}
					},
					set(sep0(":"), attr("stype")),
					null,
					lout_empty.ncopy()
					),
				ident("name")
				);
		}
		{
			DrawLayout lout_method_type_args = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siSp, SP_ADD_AFTER, 0)
				});
			DrawLayout lout_method = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlGrp, SP_ADD_AFTER, 0)
				});
			SyntaxElem method_prefix = setl(lout_empty.ncopy(),
//					jflag(1,6,1,  "@bridge"),
//					jflag(1,7,1,  "@varargs"),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,17,1, "@virtual"),
					jflag(1,18,1, "@unerasable"),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,5,1,  "synchronized"),
					jflag(1,8,1,  "native"),
					jflag(1,10,1, "abstract"),
					jflag(1,11,1, "strict")
					);
			SyntaxElem init_prefix = setl(lout_empty.ncopy(),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,3,1,  "static")
					);
			SyntaxList method_params = lst("params",node(),sep(","),lout_empty.ncopy());
			method_params.filter = new CalcOption() {
				public boolean calc(ASTNode node) {
					if (node instanceof DNode && node.isSynthetic())
						return false;
					return true;
				}
			};
			SyntaxElem method_type_args = opt("targs", new CalcOptionNotEmpty("targs"),
				set(
					sep("<"),
					lst("targs", node(new FormatInfoHint("class-arg")), sep(","), lout_empty.ncopy()),
					sep(">")
				), null, lout_method_type_args.ncopy());
			// constructor
			seConstructor = setl(lout_method.ncopy(),
				setl(lout_empty.ncopy(), attr("meta"), method_prefix.ncopy(), accs(),
					ident("parent.short_name"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("conditions", lout_empty.ncopy())),
				attr("body")
				);
			// method
			seMethod = setl(lout_method.ncopy(),
				setl(lout_empty.ncopy(), attr("meta"), method_prefix.ncopy(), accs(),
					method_type_args.ncopy(),
					ident("type_ret"), ident("name"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("aliases", lout_empty.ncopy())),
				par(plIndented, lst("conditions", lout_empty.ncopy())),
				opt("body", new CalcOptionNotNull("body"), attr("body"), sep(";"), lout_empty.ncopy())
				);
			// logical rule method
			seRuleMethod = setl(lout_method.ncopy(),
				setl(lout_nl.ncopy(), attr("meta"), method_prefix.ncopy(), accs(),
					method_type_args.ncopy(),
					kw("rule"), ident("name"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("aliases", lout_empty.ncopy())),
				par(plIndented, lst("localvars", setl(lout_nl.ncopy(), node(), sep(";")), null, lout_nl.ncopy())),
				opt("body", new CalcOptionNotNull("body"), attr("body"), sep(";"), lout_empty.ncopy())
				);
			seInitializer = setl(lout_method.ncopy(), opt("meta"), init_prefix, attr("body"));
			
			seMethodAlias = setl(lout_nl_nl.ncopy(), kw("alias"), ident("name"));
			seOperatorAlias = setl(lout_nl_nl.ncopy(),
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
				ident("image")
				);
			
			seWBCCondition = opt("body",
				setl(lout_nl_nl.ncopy(),
					alt_enum("cond", kw("error"), kw("require"), kw("ensure"), kw("invariant")),
					opt("name", set(sep("["), ident("name"), sep("]"))),
					attr("body")
				));
		}
		{
			DrawLayout lout_code_block_start = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlOrBlock,     SP_EAT_BEFORE, 0),
					new SpaceCmd(siNl,            SP_ADD_AFTER,  0),
					new SpaceCmd(siSpSEPR,        SP_ADD_BEFORE, 0),
					new SpaceCmd(siSpSEPR,        SP_ADD_AFTER,  0),
				});
			DrawLayout lout_code_block_end = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siSpSEPR,        SP_ADD_BEFORE, 0),
					new SpaceCmd(siSpSEPR,        SP_ADD_AFTER,  0),
				});
			// block expression
			seBlock = set(
					sep("{", lout_code_block_start.ncopy()),
					par(plIndented, lst("stats", setl(lout_nl.ncopy(),node()),null,lout_empty.ncopy())),
					sep("}", lout_code_block_end.ncopy())
					);
			// rule block
			DrawLayout lout_rule_block_end = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siSpSEPR,        SP_ADD_BEFORE, 0),
					new SpaceCmd(siSpSEPR,        SP_ADD_AFTER,  0),
					new SpaceCmd(siNl,            SP_ADD_BEFORE,  0),
				});
			seRuleBlock = set(
					sep("{", lout_code_block_start.ncopy()),
					par(plIndented, attr("node")),
					sep("}", lout_rule_block_end.ncopy())
					);
			// rule OR block
			DrawLayout lout_rule_or = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNl,            SP_ADD_BEFORE,  0),
					new SpaceCmd(siSpSEPR,        SP_ADD_BEFORE, 0),
					new SpaceCmd(siNl,            SP_ADD_AFTER,  0),
					new SpaceCmd(siSpSEPR,        SP_ADD_AFTER,  0),
				});
			SyntaxElem rule_or = new SyntaxSeparator(";",lout_rule_or);
			seRuleOrExpr = set(
					sep("{", lout_code_block_start.ncopy()),
					lst("rules", par(plIndented, node()), rule_or, lout_nl.ncopy()),
					sep("}", lout_code_block_end.ncopy())
					);
			// rule AND block
			DrawLayout lout_rule_and = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siSpSEPR,        SP_ADD_BEFORE, 0),
					new SpaceCmd(siNl,            SP_ADD_AFTER,  0),
					new SpaceCmd(siSpSEPR,        SP_ADD_AFTER,  0),
				});
			SyntaxElem rule_and = new SyntaxSeparator(",",lout_rule_and);
			seRuleAndExpr = lst("rules", node(), rule_and, lout_nl.ncopy());
		}
		seExprStat = set(opt("expr"), sep(";"));
		seReturnStat = set(kw("return"), opt("expr"), sep(";"));
		seThrowStat = set(kw("throw"), attr("expr"), sep(";"));
		seCondStat = set(attr("cond"), opt("message", set(sep(":"), attr("message"))), sep(";"));
		seLabeledStat = set(ident("ident"), sep(":"), attr("stat"));
		seBreakStat = set(kw("break"), opt("ident", ident("ident")), sep(";"));
		seContinueStat = set(kw("continue"), opt("ident", ident("ident")), sep(";"));
		seGotoStat = set(kw("goto"), opt("ident", ident("ident")), sep(";"));
		seGotoCaseStat = set(kw("goto"), opt("expr", new CalcOptionNotNull("expr"), set(kw("case"), attr("expr")), kw("default"), lout_empty.ncopy()), sep(";"));

		{
			DrawLayout lout_cond = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlOrBlock,   SP_ADD_AFTER,  0),
					new SpaceCmd(siSp,          SP_ADD_BEFORE, 0),
					new SpaceCmd(siSp,          SP_ADD_AFTER,  0),
				});
			seIfElseStat = set(
				kw("if"),
				setl(lout_cond.ncopy(), sep("("), attr("cond"), sep(")")),
				par(plStatIndented, attr("thenSt")),
				opt("elseSt",
					set(
						setl(lout_cond.ncopy(), new SyntaxSpace(lout_nl.ncopy()), kw("else")),
						par(plStatIndented, attr("elseSt"))
						)
					)
				);
			seWhileStat = set(
				kw("while"),
				setl(lout_cond.ncopy(), sep("("), attr("cond"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seDoWhileStat = set(
				kw("do"),
				par(plStatIndented, attr("body")),
				kw("while"),
				setl(lout_cond.ncopy(), sep("("), attr("cond"), sep(")")),
				sep(";")
				);
			seForInit = set(
				ident("type_ref"),
				lst("decls",node(new FormatInfoHint("no-type")),sep(","),lout_empty.ncopy())
				);
			seForStat = set(
				kw("for"),
				setl(lout_cond.ncopy(), sep("("), opt("init"), sep(";"), opt("cond"), sep(";"), opt("iter"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seForEachStat = set(
				kw("foreach"),
				setl(lout_cond.ncopy(), sep("("), opt("var", set(attr("var"), sep(";"))), attr("container"), opt("cond", set(sep(";"), attr("cond"))), sep(")")),
				par(plStatIndented, attr("body"))
				);
			
			seCaseLabel = set(
				opt("val", new CalcOptionNotNull("val"),
					set(
						kw("case"),
						attr("val"),
						opt("pattern", new CalcOptionNotEmpty("pattern"),
							set(
								sep("("),
								lst("pattern",node(),sep(","),lout_empty.ncopy()),
								sep(")")
								),
							null,
							lout_empty.ncopy()
							)
						),
					kw("default"),
					lout_empty.ncopy()
					),
				sep(":", lout_nl.ncopy()),
				par(plIndented, lst("stats",setl(lout_nl.ncopy(),node()),null,lout_nl.ncopy()))
				);
			seSwitchStat = set(
				kw("switch"),
				setl(lout_cond.ncopy(), sep("("), attr("sel"), sep(")")),
				set(
					sep("{", lout_nl.ncopy()),
					lst("cases",setl(lout_nl.ncopy(),node()),null,lout_nl.ncopy()),
					sep("}")
					)
				);
			seCatchInfo = set(
				kw("catch"),
				setl(lout_cond.ncopy(), sep("("), attr("arg"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seFinallyInfo = set(
				kw("finally"),
				par(plStatIndented, attr("body"))
				);
			seTryStat = set(
				kw("try"),
				par(plStatIndented, attr("body")),
				lst("catchers",lout_empty.ncopy()),
				opt("finally_catcher", attr("finally_catcher"))
				);
			seSynchronizedStat = set(
				kw("synchronized"),
				setl(lout_cond.ncopy(), sep("("), attr("expr"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seWithStat = set(
				kw("with"),
				setl(lout_cond.ncopy(), sep("("), attr("expr"), sep(")")),
				par(plStatIndented, attr("body"))
				);
		}
	
		exprs = new Hashtable<Operator, SyntaxElem>();
		seConstExpr = attr("this");
		seConstExprTrue = kw("true");
		seConstExprFalse = kw("false");
		seConstExprNull = kw("null");
		seConstExprChar = charcter("value");
		seConstExprStr = string("value");
		seTypeRef = type(null);
		seStructRef = type(new FormatInfoHint("no-args"));

		seRuleIstheExpr = set(ident("var"), oper("?="), expr("expr", Constants.opAssignPriority));
		seRuleIsoneofExpr = set(ident("var"), oper("@="), expr("expr", Constants.opAssignPriority));
		seRuleCutExpr = kw("$cut");
		seRuleCallExpr = set(
				expr("obj", Constants.opAccessPriority),
				sep("."),
				ident("func.name"),
				sep("("),
				lst("args",node(),sep(","),lout_empty.ncopy()),
				sep(")")
				);
		seRuleWhileExpr = set(kw("while"), expr("expr", 1), opt("bt_expr", set(oper(":"), expr("bt_expr", 1))));
		seRuleExpr = set(expr("expr", 1), opt("bt_expr", set(oper(":"), expr("bt_expr", 1))));

		seAccessExpr = set(expr("obj", Constants.opAccessPriority), sep("."), ident("ident"));
		seIFldExpr = set(expr("obj", Constants.opAccessPriority), sep("."), ident("ident"));
		seContainerAccessExpr = set(expr("obj", Constants.opContainerElementPriority), sep("["), attr("index"), sep("]"));
		seThisExpr = opt("super",
						new CalcOption() {
							public boolean calc(ASTNode node) { return !((ThisExpr)node).isSuperExpr(); }
						},
						kw("this"),
						kw("super"),
						lout_empty.ncopy()
						);
		seLVarExpr = ident("ident");
		seSFldExpr = set(expr("obj", Constants.opAccessPriority), sep("."), ident("ident"));
		seOuterThisAccessExpr = set(ident("outer"), sep("."), kw("this"));
		seReinterpExpr = set(sep("("), kw("$reinterp"), attr("type"), sep(")"), expr("expr", Constants.opCastPriority));
		
		seBinaryBooleanOrExpr = expr("expr1", BinaryOperator.BooleanOr, "expr2");
		seBinaryBooleanAndExpr = expr("expr1", BinaryOperator.BooleanAnd, "expr2");
		seInstanceofExpr = expr("expr", BinaryOperator.InstanceOf, "type");
//			set(
//			expr("expr", Constants.opInstanceOfPriority),
//			kw("instanceof"),
//			attr("type", new FormatInfoHint("no-args"))
//			);
		seBooleanNotExpr = expr(PrefixOperator.BooleanNot, "expr");
		seCallExpr = set(
				expr("obj", new FormatInfoHint("call-accessor"), Constants.opAccessPriority),
				sep("."),
				ident("func.name"),
				sep("("),
				lst("args",node(),sep(","),lout_empty.ncopy()),
				sep(")")
				);
		seCallConstr = set(
				opt("this",
					new CalcOption() {
						public boolean calc(ASTNode node) { return ((ENode)node).isSuperExpr(); }
					},
					kw("super"),
					kw("this"),
					lout_empty.ncopy()
					),
				sep("("),
				lst("args",node(),sep(","),lout_empty.ncopy()),
				sep(")")
				);
		seClosureCallExpr = set(
				expr("expr", Constants.opCallPriority),
				sep("("),
				lst("args",node(),sep(","),lout_empty.ncopy()),
				sep(")")
				);
		seStringConcatExpr = lst("args",
				expr("this", BinaryOperator.Add.priority),
				oper(BinaryOperator.Add),
				lout_empty.ncopy()
			);
		seCommaExpr = lst("exprs",
				expr("this", BinaryOperator.Comma.priority),
				oper(BinaryOperator.Comma),
				lout_empty.ncopy()
			);

		seNewExpr = set(
				opt("outer", set(expr("outer", Constants.opAccessPriority), oper("."))),
				kw("new"),
				ident("type"),
				sep("("),
				lst("args",node(),sep(","),lout_empty.ncopy()),
				sep(")"),
				opt("clazz", attr("clazz", new FormatInfoHint("anonymouse")))
				);
		seNewArrayExpr = set(
				kw("new"),
				ident("type"),
				lst("args",
					set(
						sep("["),
						node(),
						sep("]")
					),
					null,
					lout_empty.ncopy()
					)
				);
		seNewInitializedArrayExpr = set(
				kw("new"),
				ident("arrtype"),
				sep("{"),
				lst("args",node(),sep(","),lout_empty.ncopy()),
				sep("}")
				);
		seNewClosure = set(
				kw("fun"),
				sep("("),
				lst("params",node(),sep(","),lout_empty.ncopy()),
				sep(")"),
				sep("->"),
				ident("type_ret"),
				attr("body")
				);

		seShadow = attr("node");
		seArrayLengthExpr = set(expr("obj", Constants.opAccessPriority), sep("."), kw("length"));
		seTypeClassExpr = set(ident("type"), sep("."), kw("class"));
		seTypeInfoExpr = set(ident("type"), sep("."), kw("type"));
		seConditionalExpr = set(
			expr("cond", MultiOperator.opConditionalPriority+1),
			oper("?"),
			expr("expr1", MultiOperator.opConditionalPriority+1),
			oper(":"),
			expr("expr2", MultiOperator.opConditionalPriority)
			);
		seCastExpr = set(sep("("), kw("$cast"), attr("type"), sep(")"), expr("expr", Constants.opCastPriority));
		seNopExpr = new SyntaxSpace(new DrawLayout());
		seNopExpr.is_hidden = true;
	}
	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
		switch (node) {
		case FileUnit: return seFileUnit;
		case Import: return seImport;
		case Opdef: return seOpdef;
		case TypeAssign: return seTypeAssign;
		case TypeConstr:
			if (hint != null && "class-arg".equals(hint.text))
				return seTypeConstrClassArg;
			return seTypeConstr;
		case MetaSet: return seMetaSet;
		case Meta: return seMeta;
		case MetaValueScalar: return seMetaValueScalar;
		case MetaValueArray: return seMetaValueArray;
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
			if (hint != null && "anonymouse".equals(hint.text))
				return seStructBody;
			return seStructClass;
		}
		case Field: return seFieldDecl;
		case VarDecl: return seVarDecl;
		case FormPar: return seFormPar;
		case Var:
			if (hint != null && "no-type".equals(hint.text))
				return seVarNoType;
			return seVar;
		case RuleMethod: return seRuleMethod;
		case Constructor: return seConstructor;
		case Method: return seMethod;
		case Initializer: return seInitializer;
		case ASTIdentifierAlias: return seMethodAlias;
		case ASTOperatorAlias: return seOperatorAlias;
		case WBCCondition: return seWBCCondition;
		case Block: return seBlock;
		case RuleBlock: return seRuleBlock;
		case RuleOrExpr: return seRuleOrExpr;
		case RuleAndExpr: return seRuleAndExpr;

		case ConstBoolExpr:
			return ((ConstBoolExpr)node).value ? seConstExprTrue : seConstExprFalse;
		case ConstNullExpr:
			return seConstExprNull;
		case ConstCharExpr:
			return seConstExprChar;
		case ConstStringExpr:
			return seConstExprStr;
		case ConstExpr:
			return seConstExpr;
		case TypeRef:
			if (hint != null) {
				if ("call-accessor".equals(hint.text))
				return seStructRef;
			}
			return seTypeRef;
		case Shadow: return seShadow;

		case RuleIstheExpr: return seRuleIstheExpr;
		case RuleIsoneofExpr: return seRuleIsoneofExpr;
		case RuleCutExpr: return seRuleCutExpr;
		case RuleCallExpr: return seRuleCallExpr;
		case RuleWhileExpr: return seRuleWhileExpr;
		case RuleExpr: return seRuleExpr;
		
		case ExprStat: return seExprStat;
		case ReturnStat: return seReturnStat;
		case ThrowStat: return seThrowStat;
		case IfElseStat: return seIfElseStat;
		case CondStat: return seCondStat;
		case LabeledStat: return seLabeledStat;
		case BreakStat: return seBreakStat;
		case ContinueStat: return seContinueStat;
		case GotoStat: return seGotoStat;
		case GotoCaseStat: return seGotoCaseStat;

		case WhileStat: return seWhileStat;
		case DoWhileStat: return seDoWhileStat;
		case ForInit: return seForInit;
		case ForStat: return seForStat;
		case ForEachStat: return seForEachStat;

		case CaseLabel: return seCaseLabel;
		case SwitchStat: return seSwitchStat;
		case CatchInfo: return seCatchInfo;
		case FinallyInfo: return seFinallyInfo;
		case TryStat: return seTryStat;
		case SynchronizedStat: return seSynchronizedStat;
		case WithStat: return seWithStat;

		case AccessExpr: return seAccessExpr;
		case IFldExpr: return seIFldExpr;
		case ContainerAccessExpr: return seContainerAccessExpr;
		case ThisExpr: return seThisExpr;
		case LVarExpr: return seLVarExpr;
		case SFldExpr: return seSFldExpr;
		case OuterThisAccessExpr: return seOuterThisAccessExpr;
		case ReinterpExpr: return seReinterpExpr;
		
		case BinaryBooleanOrExpr: return seBinaryBooleanOrExpr;
		case BinaryBooleanAndExpr: return seBinaryBooleanAndExpr;
		case InstanceofExpr: return seInstanceofExpr;
		case BooleanNotExpr: return seBooleanNotExpr;
		case CallExpr:
			if (((CallExpr)node).func instanceof Constructor)
				return seCallConstr;
			return seCallExpr;
		case ClosureCallExpr: return seClosureCallExpr;
		case StringConcatExpr: return seStringConcatExpr;
		case CommaExpr: return seCommaExpr;
		case ArrayLengthExpr: return seArrayLengthExpr;
		case TypeClassExpr: return seTypeClassExpr;
		case TypeInfoExpr: return seTypeInfoExpr;
		case ConditionalExpr: return seConditionalExpr;
		case CastExpr: return seCastExpr;
		case NopExpr: return seNopExpr;

		case NewExpr: return seNewExpr;
		case NewArrayExpr: return seNewArrayExpr;
		case NewInitializedArrayExpr: return seNewInitializedArrayExpr;
		case NewClosure: return seNewClosure;

		case UnaryExpr: {
			Operator op = ((UnaryExpr)node).op;
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				if (op instanceof PrefixOperator)
					se = expr(op, "expr");
				else
					se = expr("expr", op);
				exprs.put(op, se);
			}
			return se;
		}
		case IncrementExpr: {
			Operator op = ((IncrementExpr)node).op;
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				if (op instanceof PrefixOperator)
					se = expr(op, "expr");
				else
					se = expr("expr", op);
				exprs.put(op, se);
			}
			return se;
		}
		case BinaryExpr: {
			Operator op = ((BinaryExpr)node).op;
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				se = expr("expr1", op, "expr2");
				exprs.put(op, se);
			}
			return se;
		}
		case BinaryBoolExpr: {
			Operator op = ((BinaryBoolExpr)node).op;
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				se = expr("expr1", op, "expr2");
				exprs.put(op, se);
			}
			return se;
		}
		case AssignExpr: {
			Operator op = ((AssignExpr)node).op;
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				se = expr("lval", op, "value");
				exprs.put(op, se);
			}
			return se;
		}
		}
		return super.getSyntaxElem(node, hint);
	}
}

