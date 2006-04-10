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

	public SyntaxJavaExpr() {}
	public SyntaxJavaExpr(String name, DrawLayout layout, int priority, SyntaxSeparator l_paren, SyntaxSeparator r_paren) {
		super(name,layout);
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
	final SyntaxElem seStructSyntax;
	final SyntaxElem seStructBody;
	final SyntaxElem seImport;
	final SyntaxElem seOpdef;
	final SyntaxElem seMetaSet;
	final SyntaxElem seMeta;
	final SyntaxElem seMetaValueScalar;
	final SyntaxElem seMetaValueArray;
	final SyntaxElem seTypeDef;
	final SyntaxElem seFieldDecl;
	final SyntaxElem seVarDecl;
	final SyntaxElem seVar;
	final SyntaxElem seFormPar;
	final SyntaxElem seConstructor;
	final SyntaxElem seMethod;
	final SyntaxElem seInitializer;
	
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
	final SyntaxElem seTypeRef;
	final SyntaxElem seConstExpr;
	final SyntaxElem seConstExprTrue;
	final SyntaxElem seConstExprFalse;
	final SyntaxElem seConstExprNull;
	final SyntaxElem seConstExprChar;
	final SyntaxElem seConstExprStr;
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
	
	final Hashtable<Operator, SyntaxElem> exprs;
	
	public SpaceInfo siNlOrBlock = new SpaceInfo("nl-block",       SP_NEW_LINE, 1, 10);

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
		SyntaxJavaExpr se = new SyntaxJavaExpr(expr, lout, priority, sep("("), sep(")"));
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

	public JavaSyntax() {
		DrawLayout lout_empty = new DrawLayout();
		DrawLayout lout_nl = new DrawLayout(new SpaceCmd[]{new SpaceCmd(siNl,SP_ADD_AFTER,0)});
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
			// import
			seImport = setl(lout_nl.ncopy(),
				kw("import"),
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
			seTypeDef = setl(lout_nl.ncopy(), kw("typedef"), ident("name"), 
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
					jflag(1,12,1, "@synthetic"),
					jflag(1,18,1, "@unerasable"),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,10,1, "abstract"),
					jflag(1,11,1, "strict")
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
			seStructClass = setl(lout_empty.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("class"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// interface
			seStructInterface = setl(lout_empty.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("interface"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// interface
			seStructAnnotation = setl(lout_empty.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("@interface"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// syntax
			seStructSyntax = setl(lout_empty.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						accs(),
						kw("syntax"),
						ident("short_name")),
					seStructBody.ncopy()
				);

			SyntaxList enum_fields = lst("members",attr("name"),sep(","),lout_empty.ncopy());
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
			// enum
			seStructEnum = setl(lout_empty.ncopy(),
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
					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,17,1, "@virtual"),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,6,1,  "volatile"),
					jflag(1,7,1,  "transient"),
					jflag(1,10,1, "abstract"),
					jflag(1,11,1, "strict")
					);
			SyntaxElem var_prefix = setl(lout_empty.ncopy(),
					jflag(1,12,1, "@synthetic"),
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
			// formal parameter
			seFormPar = set(opt("meta"), var_prefix.ncopy(), ident("vtype"), ident("name")	);
		}
		{
			DrawLayout lout_method = new DrawLayout(new SpaceCmd[]{
					new SpaceCmd(siNlGrp, SP_ADD_AFTER, 0)
				});
			SyntaxElem method_prefix = setl(lout_empty.ncopy(),
					jflag(1,6,1,  "@bridge"),
					jflag(1,7,1,  "@varargs"),
					jflag(1,12,1, "@synthetic"),
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
					jflag(1,12,1, "@synthetic"),
					jflag(1,3,1,  "static")
					);
			// constructor
			seConstructor = setl(lout_method.ncopy(),
				setl(lout_empty.ncopy(), attr("meta"), method_prefix.ncopy(), accs(),
					ident("parent.short_name"),
					set(sep("("),
						lst("params",node(),sep(","),lout_empty.ncopy()),
						sep(")")
						)
					),
				attr("body")
				);
			// method
			seMethod = setl(lout_method.ncopy(),
				setl(lout_empty.ncopy(), attr("meta"), method_prefix.ncopy(), accs(),
					ident("type_ret"), ident("name"),
					set(sep("("),
						lst("params",node(),sep(","),lout_empty.ncopy()),
						sep(")")
						)
					),
				opt("body", new CalcOptionNotNull("body"), attr("body"), sep(";"), lout_empty.ncopy())
				);
			seInitializer = setl(lout_method.ncopy(), opt("meta"), init_prefix, attr("body"));
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
			seForInit = lst("decls",node(),sep(","),lout_empty.ncopy());
			seForStat = set(
				kw("for"),
				setl(lout_cond.ncopy(), sep("("), opt("init"), sep(";"), opt("cond"), sep(";"), opt("iter"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seForEachStat = set(
				kw("foreach"),
				setl(lout_cond.ncopy(), sep("("), attr("var"), sep(";"), attr("container"), opt("cond", set(sep(";"), attr("cond"))), sep(")")),
				par(plStatIndented, attr("body"))
				);
			
			seCaseLabel = set(
				opt("val", new CalcOptionNotNull("val"), set(kw("case"), attr("val")), kw("default"),lout_empty.ncopy()),
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
		seTypeRef = ident("lnk");
		
		seAccessExpr = set(attr("obj"), sep("."), ident("ident"));
		seIFldExpr = set(attr("obj"), sep("."), ident("ident"));
		seContainerAccessExpr = set(attr("obj"), sep("["), attr("index"), sep("]"));
		seThisExpr = kw("this");
		seLVarExpr = ident("ident");
		seSFldExpr = set(attr("obj"), sep("."), ident("ident"));
		seOuterThisAccessExpr = set(attr("obj"), sep("."), kw("this"));
		seReinterpExpr = set(sep("("), kw("$reinterp"), attr("type"), sep(")"), attr("expr"));
		
		seBinaryBooleanOrExpr = expr("expr1", BinaryOperator.BooleanOr, "expr2");
		seBinaryBooleanAndExpr = expr("expr1", BinaryOperator.BooleanAnd, "expr2");
		seInstanceofExpr = expr("expr", BinaryOperator.InstanceOf, "type");
		seBooleanNotExpr = expr(PrefixOperator.BooleanNot, "expr");
		seCallExpr = set(
				attr("obj"),
				sep("."),
				ident("func.name"),
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
				kw("new"),
				opt("outer", set(attr("outer"), oper("."))),
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
		seArrayLengthExpr = set(attr("obj"), sep("."), kw("length"));
		seTypeClassExpr = set(ident("type"), sep("."), kw("class"));
		seTypeInfoExpr = set(ident("type"), sep("."), kw("type"));
		seConditionalExpr = set(
			expr("cond", MultiOperator.opConditionalPriority+1),
			oper("?"),
			expr("expr1", MultiOperator.opConditionalPriority+1),
			oper(":"),
			expr("expr2", MultiOperator.opConditionalPriority)
			);
		seCastExpr = set(sep("("), kw("$cast"), attr("type"), sep(")"), attr("expr"));
	}
	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
		switch (node) {
		case FileUnit: return seFileUnit;
		case Import: return seImport;
		case Opdef: return seOpdef;
		case TypeDef: return seTypeDef;
		case MetaSet: return seMetaSet;
		case Meta: return seMeta;
		case MetaValueScalar: return seMetaValueScalar;
		case MetaValueArray: return seMetaValueArray;
		case Struct: {
			Struct s = (Struct)node;
			if (s.isEnum())
				return seStructEnum;
			if (s.isSyntax())
				return seStructSyntax;
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
		case Var: return seVar;
		case Constructor: return seConstructor;
		case Method: return seMethod;
		case Initializer: return seInitializer;
		case Block: return seBlock;
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
		case TypeRef: return seTypeRef;
		case Shadow: return seShadow;
		
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
		case CallExpr: return seCallExpr;
		case ClosureCallExpr: return seClosureCallExpr;
		case StringConcatExpr: return seStringConcatExpr;
		case CommaExpr: return seCommaExpr;
		case ArrayLengthExpr: return seArrayLengthExpr;
		case TypeClassExpr: return seTypeClassExpr;
		case TypeInfoExpr: return seTypeInfoExpr;
		case ConditionalExpr: return seConditionalExpr;
		case CastExpr: return seCastExpr;

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

