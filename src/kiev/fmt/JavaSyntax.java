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

	@att public int					idx;
	@att public int					priority;
	@att public SyntaxToken		l_paren;
	@att public SyntaxToken		r_paren;

	public SyntaxJavaExpr() {}
	public SyntaxJavaExpr(String name, FormatInfoHint hint, SpaceCmd[] spaces, int priority, SyntaxToken l_paren, SyntaxToken r_paren) {
		super(name,hint,spaces);
		this.idx = -1;
		this.priority = priority;
		this.l_paren = l_paren;
		this.r_paren = r_paren;
	}
	public SyntaxJavaExpr(int idx, FormatInfoHint hint, SpaceCmd[] spaces, int priority, SyntaxToken l_paren, SyntaxToken r_paren) {
		super("",hint,spaces);
		this.idx = idx;
		this.priority = priority;
		this.l_paren = l_paren;
		this.r_paren = r_paren;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		ANode n;
		if (idx >= 0) {
			n = ((ENode)node).getArgs()[idx];
		} else {
			n = (name == "this") ? node : (ANode)node.getVal(name);
		}
		Drawable dr = new DrawJavaExpr(n, this);
		dr.init(fmt);
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
		dr.init(fmt);
		return dr;
	}
}


@node
public class SyntaxJavaType extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaType;

	@att public FormatInfoHint hint;
	
	public SyntaxJavaType() {}
	public SyntaxJavaType(FormatInfoHint hint, SpaceCmd[] spaces) {
		super(spaces);
		this.hint = hint;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaType(node, this);
		dr.init(fmt);
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
		dr.init(fmt);
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
		dr.init(fmt);
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
	public boolean calc(ANode node) {
		if (node == null || !(node instanceof DNode)) return false;
		int f = ((DNode)node).flags >>> offs;
		f &= mask;
		return f == val;
	}
}


public class JavaSyntax extends TextSyntax {

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
	final SyntaxElem seInstanceofExpr;
	final SyntaxElem seCallExpr;
	final SyntaxElem seCallConstr;
	final SyntaxElem seClosureCallExpr;
	// new expr
	final SyntaxElem seNewExpr;
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
	final SyntaxElem seStringConcatExpr;
	final SyntaxElem seCommaExpr;
	//final SyntaxElem seConditionalExpr;
	final SyntaxElem seCastExpr;
	final SyntaxElem seNopExpr;

	final SyntaxElem seComment;
	final SyntaxElem seCommentNl;
	final SyntaxElem seCommentNlBefore;
	final SyntaxElem seCommentNlAfter;
	
	final Hashtable<Operator, SyntaxElem> exprs;
	
	public SpaceInfo siNlOrBlock = new SpaceInfo("nl-block",       SP_NEW_LINE, 1,  1);

	public SpaceInfo siFldGrpNl  = new SpaceInfo("indent-block",   SP_NEW_LINE, 2, 20);
	
	public ParagraphLayout plStatIndented = new ParagraphLayoutBlock("stat-indented", 4, 20);

	protected SyntaxElem jflag(int size, int offs, int val, String name)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return new SyntaxOptional(name, new CalcOptionJavaFlag(size, offs, val), kw(name), null, lout);
	}

	protected SyntaxJavaExpr expr(int idx, int priority)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		SyntaxJavaExpr se = new SyntaxJavaExpr(idx, null, lout, priority, sep("("), sep(")"));
		return se;
	}

	protected SyntaxJavaExpr expr(String expr, int priority)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		SyntaxJavaExpr se = new SyntaxJavaExpr(expr, null, lout, priority, sep("("), sep(")"));
		return se;
	}

	protected SyntaxJavaExpr expr(String expr, FormatInfoHint hint, int priority)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		SyntaxJavaExpr se = new SyntaxJavaExpr(expr, hint, lout, priority, sep("("), sep(")"));
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
				elems[i] = type(null);
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

	protected SyntaxElem accs() {
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSp, SP_ADD, SP_NOP, 0),
				new SpaceCmd(siSp, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxJavaAccess(lout);
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
		return super.sep(sep);
	}
	
	protected SyntaxJavaType type(FormatInfoHint hint) {
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpWORD, SP_ADD, SP_ADD, 0),
				new SpaceCmd(siSpSEPR, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxJavaType(hint,lout);
	}

	public JavaSyntax() {
		SpaceCmd[] lout_empty = new SpaceCmd[0];
		SpaceCmd[] lout_nl = new SpaceCmd[] {new SpaceCmd(siNl,SP_NOP,SP_ADD,0)};
		SpaceCmd[] lout_nl_nl = new SpaceCmd[] {new SpaceCmd(siNl,SP_ADD,SP_ADD,0)};
		SpaceCmd[] lout_nl_grp = new SpaceCmd[] {new SpaceCmd(siNlGrp,SP_NOP,SP_ADD,0)};
		{
			SpaceCmd[] lout_pkg = new SpaceCmd[] {
					new SpaceCmd(siNlGrp, SP_NOP, SP_ADD, 0)
				};
			// file unit
			seFileUnit = setl(lout_nl,
					opt("pkg", setl(lout_pkg, kw("package"), ident("pkg"), sep(";"))),
					lst("members", lout_nl)
				);
		}
		{
			SyntaxElem sp_hid = new SyntaxSpace(new SpaceCmd[0]);
			sp_hid.is_hidden = true;
			// import
			seImport = setl(lout_nl,
				kw("import"),
				alt_enum("mode",
					sp_hid.ncopy(),
					kw("static"),
					kw("package"),
					kw("syntax")
					),
				ident("name"),
				opt("star",new CalcOptionTrue("star"), sep(".*"), null, lout_empty),
				sep(";"));
			seOpdef = setl(lout_nl,
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
			SyntaxElem typedef_prefix = setl(lout_empty,
					opt("meta"),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,17,1, "@virtual"),
//					jflag(1,18,1, "@unerasable"),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,10,1, "abstract")
					);
			seTypeAssign = setl(lout_nl, typedef_prefix.ncopy(), kw("typedef"), ident("id"), oper("="), attr("type_ref"), sep(";"));
			
			seTypeConstrClassArg = setl(lout_empty, ident("id"),
				opt("upper_bound",
					new CalcOption() {
						public boolean calc(ANode node) {
							if !(node instanceof TypeConstr) return false;
							TypeConstr tc = (TypeConstr)node;
							if (tc.super_types.length == 0) return false;
							if (tc.super_types.length == 1 && tc.super_types[0].getType() ≈ Type.tpObject) return false;
							return true;
						}
					},
					set(
						kw("extends"),
						lst("super_types", node(), oper("&"), lout_empty)
						),
					null, lout_empty
					),
				opt("lower_bound", new CalcOptionNotEmpty("lower_bound"),
					set(
						kw("super"),
						lst("lower_bound", node(), sep("&"), lout_empty)
						),
					null, lout_empty
					)
				);
			seTypeConstr = setl(lout_nl, typedef_prefix.ncopy(), kw("typedef"), ident("id"), 
				lst("super_types", set(oper("\u2264"), node()), null, lout_empty),
				lst("lower_bound", set(oper("\u2265"), node()), null, lout_empty),
				sep(";")
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
			SyntaxElem struct_prefix = setl(lout_struct_hdr,
					attr("meta"),
					opt("singleton", new CalcOption() {public boolean calc(ANode node) {return ((Struct)node).isSingleton();}}, kw("@singleton"), null, lout_empty),
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
					lst("args", node(new FormatInfoHint("class-arg")), sep(","), lout_empty),
					sep(">")
				), null, lout_empty);
			SpaceCmd[] lout_ext = new SpaceCmd[] {new SpaceCmd(siSp, SP_ADD, SP_NOP, 0)};
			SyntaxElem class_ext = opt("extends", new CalcOptionNotEmpty("super_types"),
				set(
					kw("extends"),
					lst("super_types", node(), sep(","), lout_empty)
					),
				null, lout_ext
				);
			SyntaxList struct_members = lst("members",lout_empty);
			struct_members.filter = new CalcOption() {
				public boolean calc(ANode node) {
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
					setl(lout_struct_hdr,
						struct_prefix.ncopy(),
						accs(),
						kw("class"),
						ident("id"),
						struct_args.ncopy(),
						class_ext.ncopy()),
					seStructBody.ncopy()
				);
			// interface
			seStructInterface = set(
					setl(lout_struct_hdr,
						struct_prefix.ncopy(),
						accs(),
						kw("interface"),
						ident("id"),
						struct_args.ncopy(),
						class_ext),
					seStructBody.ncopy()
				);
			// view
			seStructView = set(
					setl(lout_struct_hdr,
						struct_prefix.ncopy(),
						accs(),
						kw("view"),
						ident("id"),
						struct_args.ncopy(),
						kw("of"),
						ident("view_of"),
						class_ext.ncopy()),
					seStructBody.ncopy()
				);
			// annotation
			seStructAnnotation = set(
					setl(lout_struct_hdr,
						struct_prefix.ncopy(),
						accs(),
						kw("@interface"),
						ident("id")),
					seStructBody.ncopy()
				);
			// syntax
			seStructSyntax = set(
					setl(lout_struct_hdr,
						struct_prefix.ncopy(),
						accs(),
						kw("syntax"),
						ident("id")),
					seStructBody.ncopy()
				);

			// case
			SyntaxElem case_prefix = setl(lout_struct_hdr,
					attr("meta"),
					jflag(1,18,1, "@unerasable")
					);
			SyntaxList case_fields = lst("members",
				set(attr("meta"), ident("ftype"), ident("id")),
				sep(","),
				lout_empty
				);
			case_fields.filter = new CalcOption() {
				public boolean calc(ANode node) { return node instanceof Field && !node.isSynthetic(); }
			};
			seStructCase = setl(lout_nl_grp,
					case_prefix,
					accs(),
					kw("case"),
					ident("id"),
					struct_args.ncopy(),
					opt("singleton",
						new CalcOption() {
							public boolean calc(ANode node) { return !((Struct)node).isSingleton(); }
						},
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
					opt("alias",
						new CalcOption() {
							public boolean calc(ANode node) {
								return ((Field)node).getMetaAlias() != null;
							}
						},
						set(sep(":"), new SyntaxJavaEnumAlias(lout_empty)),
						null,
						lout_empty
						)
					),
				sep_nl(","),
				lout_empty);
			enum_fields.filter = new CalcOption() {
				public boolean calc(ANode node) {
					if (node instanceof Field && node.isEnumField())
						return true;
					return false;
				}
			};
			SyntaxList enum_members = lst("members",lout_empty);
			enum_members.filter = new CalcOption() {
				public boolean calc(ANode node) {
					if ((node instanceof DNode && node.isSynthetic()) || (node instanceof Field && node.isEnumField()))
						return false;
					return true;
				}
			};
			seStructEnum = set(
					setl(lout_struct_hdr,
						struct_prefix.ncopy(),
						accs(),
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
			seMetaSet = lst("metas", lout_empty);
			seMeta = setl(lout_nl, oper("@"), ident("type"),
						set(
							sep("("),
							lst("values",node(),sep(","),lout_empty),
							sep(")")
							)
						);
			seMetaValueScalar = set(ident("ident"), oper("="), attr("value"));
			seMetaValueArray = set(ident("ident"), oper("="),
						set(sep("{"),
							lst("values",node(),sep(","),lout_empty),
							sep("}")
							)
						);
		}
		{
			SpaceCmd[] lout_field = new SpaceCmd[] {
					new SpaceCmd(siFldGrpNl, SP_EAT, SP_ADD,0),
					new SpaceCmd(siNl,       SP_NOP, SP_ADD, 0),
				};
			SyntaxElem field_prefix = setl(lout_empty,
					attr("meta"),
//					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,17,1, "@virtual"),
					opt("packed",
						new CalcOption() {
							public boolean calc(ANode node) {return ((Field)node).isPackedField();}
						},
						new SyntaxJavaPackedField(lout_empty),
						null,
						lout_empty
						),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,6,1,  "volatile"),
					jflag(1,7,1,  "transient"),
					jflag(1,10,1, "abstract"),
					jflag(1,11,1, "strict")
					);
			SyntaxElem var_prefix = setl(lout_empty,
//					jflag(1,12,1, "@synthetic"),
					jflag(1,16,1, "@forward"),
					jflag(1,4,1,  "final")
					);
			// field
			seFieldDecl = setl(lout_field, field_prefix, accs(),
				ident("ftype"), ident("id"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))), sep(";")
				);
			// vars
			seVarDecl = set(opt("meta"), var_prefix.ncopy(),
				ident("vtype"), ident("id"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))),
				sep(";"));
			seVar = set(opt("meta"), var_prefix.ncopy(),
				ident("vtype"), ident("id"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority)))
				);
			seVarNoType = set(ident("id"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))));
			// formal parameter
			seFormPar = set(opt("meta"),
				var_prefix.ncopy(),
				attr("vtype"),
				opt("stype",
					new CalcOption() {
						public boolean calc(ANode node) {
							FormPar fp = (FormPar)node;
							return fp.stype != null && fp.vtype.getType() ≉ fp.stype.getType();
						}
					},
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
			SyntaxElem method_prefix = setl(lout_empty,
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
			SyntaxElem init_prefix = setl(lout_empty,
//					jflag(1,12,1, "@synthetic"),
					jflag(1,3,1,  "static")
					);
			SyntaxList method_params = lst("params",node(),sep(","),lout_empty);
			method_params.filter = new CalcOption() {
				public boolean calc(ANode node) {
					if (node instanceof DNode && node.isSynthetic())
						return false;
					return true;
				}
			};
			SyntaxElem method_type_args = opt("targs", new CalcOptionNotEmpty("targs"),
				set(
					sep("<"),
					lst("targs", node(new FormatInfoHint("class-arg")), sep(","), lout_empty),
					sep(">")
				), null, lout_method_type_args);
			// constructor
			seConstructor = setl(lout_method,
				setl(lout_empty, attr("meta"), method_prefix.ncopy(), accs(),
					ident("parent.id"),
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
				setl(lout_empty, attr("meta"), method_prefix.ncopy(), accs(),
					method_type_args.ncopy(),
					ident("type_ret"), ident("id"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("aliases", lout_empty)),
				par(plIndented, lst("conditions", lout_empty)),
				opt("body", new CalcOptionNotNull("body"), attr("body"), sep(";"), lout_empty)
				);
			// logical rule method
			seRuleMethod = setl(lout_method,
				setl(lout_nl, attr("meta"), method_prefix.ncopy(), accs(),
					method_type_args.ncopy(),
					kw("rule"), ident("id"),
					set(sep("("),
						method_params.ncopy(),
						sep(")")
						)
					),
				par(plIndented, lst("aliases", lout_empty)),
				par(plIndented, lst("localvars", setl(lout_nl, node(), sep(";")), null, lout_nl)),
				opt("body", new CalcOptionNotNull("body"), attr("body"), sep(";"), lout_empty)
				);
			seInitializer = setl(lout_method, opt("meta"), init_prefix, attr("body"));
			
			seMethodAlias = setl(lout_nl_nl, kw("alias"), ident("name"));
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
				ident("image")
				);
			
			seWBCCondition = opt("body",
				setl(lout_nl_nl,
					alt_enum("cond", kw("error"), kw("require"), kw("ensure"), kw("invariant")),
					opt("id", set(sep("["), ident("id"), sep("]"))),
					attr("body")
				));
		}
		{
			SpaceCmd[] lout_code_block_start = new SpaceCmd[] {
					new SpaceCmd(siNlOrBlock,     SP_EAT, SP_NOP, 0),
					new SpaceCmd(siNl,            SP_NOP, SP_ADD,  0),
					new SpaceCmd(siSpSEPR,        SP_ADD, SP_ADD, 0),
				};
			SpaceCmd[] lout_code_block_end = new SpaceCmd[] {
					new SpaceCmd(siSpSEPR,        SP_ADD, SP_ADD, 0),
				};
			// block expression
			seBlock = set(
					sep("{", lout_code_block_start),
					par(plIndented, lst("stats", setl(lout_nl,node(new FormatInfoHint("stat"))),null,lout_empty)),
					sep("}", lout_code_block_end)
					);
			// rule block
			SpaceCmd[] lout_rule_block_end = new SpaceCmd[] {
					new SpaceCmd(siSpSEPR,        SP_ADD, SP_ADD, 0),
					new SpaceCmd(siNl,            SP_ADD, SP_NOP,  0),
				};
			seRuleBlock = set(
					sep("{", lout_code_block_start),
					par(plIndented, attr("node")),
					sep("}", lout_rule_block_end)
					);
			// rule OR block
			SpaceCmd[] lout_rule_or = new SpaceCmd[] {
					new SpaceCmd(siNl,            SP_ADD, SP_ADD,  0),
					new SpaceCmd(siSpSEPR,        SP_ADD, SP_ADD, 0),
				};
			SyntaxElem rule_or = new SyntaxToken(";",lout_rule_or);
			seRuleOrExpr = set(
					sep("{", lout_code_block_start),
					lst("rules", par(plIndented, node()), rule_or, lout_nl),
					sep("}", lout_code_block_end)
					);
			// rule AND block
			SpaceCmd[] lout_rule_and = new SpaceCmd[] {
					new SpaceCmd(siSpSEPR,        SP_ADD, SP_ADD, 0),
					new SpaceCmd(siNl,            SP_NOP, SP_ADD,  0),
				};
			SyntaxElem rule_and = new SyntaxToken(",",lout_rule_and);
			seRuleAndExpr = lst("rules", node(), rule_and, lout_nl);
		}
		seExprStat = set(opt("expr"), sep(";"));
		seReturnStat = set(kw("return"), opt("expr"), sep(";"));
		seThrowStat = set(kw("throw"), attr("expr"), sep(";"));
		seCondStat = set(attr("cond"), opt("message", set(sep(":"), attr("message"))), sep(";"));
		seLabeledStat = set(ident("lbl.id"), sep(":"), attr("stat"));
		seBreakStat = set(kw("break"), opt("ident", ident("ident")), sep(";"));
		seContinueStat = set(kw("continue"), opt("ident", ident("ident")), sep(";"));
		seGotoStat = set(kw("goto"), opt("ident", ident("ident")), sep(";"));
		seGotoCaseStat = set(kw("goto"), opt("expr", new CalcOptionNotNull("expr"), set(kw("case"), attr("expr")), kw("default"), lout_empty), sep(";"));

		{
			SpaceCmd[] lout_cond = new SpaceCmd[] {
					new SpaceCmd(siNlOrBlock,   SP_NOP, SP_ADD,  0),
					new SpaceCmd(siSp,          SP_ADD, SP_ADD, 0),
				};
			seIfElseStat = set(
				kw("if"),
				setl(lout_cond, sep("("), attr("cond"), sep(")")),
				par(plStatIndented, attr("thenSt")),
				opt("elseSt",
					set(
						setl(lout_cond, new SyntaxSpace(lout_nl), kw("else")),
						par(plStatIndented, attr("elseSt"))
						)
					)
				);
			seWhileStat = set(
				kw("while"),
				setl(lout_cond, sep("("), attr("cond"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seDoWhileStat = set(
				kw("do"),
				par(plStatIndented, attr("body")),
				kw("while"),
				setl(lout_cond, sep("("), attr("cond"), sep(")")),
				sep(";")
				);
			seForInit = set(
				ident("type_ref"),
				lst("decls",node(new FormatInfoHint("no-type")),sep(","),lout_empty)
				);
			seForStat = set(
				kw("for"),
				setl(lout_cond, sep("("), opt("init"), sep(";"), opt("cond"), sep(";"), opt("iter"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seForEachStat = set(
				kw("foreach"),
				setl(lout_cond, sep("("), opt("var", set(attr("var"), sep(";"))), attr("container"), opt("cond", set(sep(";"), attr("cond"))), sep(")")),
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
								lst("pattern",node(),sep(","),lout_empty),
								sep(")")
								),
							null,
							lout_empty
							)
						),
					kw("default"),
					lout_empty
					),
				sep(":", lout_nl),
				par(plIndented, lst("stats",setl(lout_nl,node(new FormatInfoHint("stat"))),null,lout_nl))
				);
			seSwitchStat = set(
				kw("switch"),
				setl(lout_cond, sep("("), attr("sel"), sep(")")),
				set(
					sep("{", lout_nl),
					lst("cases",setl(lout_nl,node()),null,lout_nl),
					sep("}")
					)
				);
			seCatchInfo = set(
				kw("catch"),
				setl(lout_cond, sep("("), attr("arg"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seFinallyInfo = set(
				kw("finally"),
				par(plStatIndented, attr("body"))
				);
			seTryStat = set(
				kw("try"),
				par(plStatIndented, attr("body")),
				lst("catchers",lout_empty),
				opt("finally_catcher", attr("finally_catcher"))
				);
			seSynchronizedStat = set(
				kw("synchronized"),
				setl(lout_cond, sep("("), attr("expr"), sep(")")),
				par(plStatIndented, attr("body"))
				);
			seWithStat = set(
				kw("with"),
				setl(lout_cond, sep("("), attr("expr"), sep(")")),
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
				ident("func"),
				sep("("),
				lst("args",node(),sep(","),lout_empty),
				sep(")")
				);
		seRuleWhileExpr = set(kw("while"), expr("expr", 1), opt("bt_expr", set(oper(":"), expr("bt_expr", 1))));
		seRuleExpr = set(expr("expr", 1), opt("bt_expr", set(oper(":"), expr("bt_expr", 1))));

		seAccessExpr = set(expr("obj", Constants.opAccessPriority), sep("."), ident("ident"));
		seIFldExpr = set(expr("obj", Constants.opAccessPriority), sep("."), ident("ident"));
		seContainerAccessExpr = set(expr("obj", Constants.opContainerElementPriority), sep("["), attr("index"), sep("]"));
		seThisExpr = opt("super",
						new CalcOption() {
							public boolean calc(ANode node) { return !((ThisExpr)node).isSuperExpr(); }
						},
						kw("this"),
						kw("super"),
						lout_empty
						);
		seLVarExpr = ident("ident");
		seSFldExpr = set(expr("obj", Constants.opAccessPriority), sep("."), ident("ident"));
		seOuterThisAccessExpr = set(ident("outer"), sep("."), kw("this"));
		seReinterpExpr = set(sep("("), kw("$reinterp"), attr("type"), sep(")"), expr("expr", Constants.opCastPriority));
		
		seInstanceofExpr = //expr("expr", Operator.InstanceOf, "type");
			set(
			expr("expr", Constants.opInstanceOfPriority),
			kw("instanceof"),
			attr("type", new FormatInfoHint("no-args"))
			);
		seCallExpr = set(
				expr("obj", new FormatInfoHint("call-accessor"), Constants.opAccessPriority),
				sep("."),
				ident("ident"),
				sep("("),
				lst("args",node(),sep(","),lout_empty),
				sep(")")
				);
		seCallConstr = set(
				opt("this",
					new CalcOption() {
						public boolean calc(ANode node) { return ((ENode)node).isSuperExpr(); }
					},
					kw("super"),
					kw("this"),
					lout_empty
					),
				sep("("),
				lst("args",node(),sep(","),lout_empty),
				sep(")")
				);
		seClosureCallExpr = set(
				expr("expr", Constants.opCallPriority),
				sep("("),
				lst("args",node(),sep(","),lout_empty),
				sep(")")
				);
		seStringConcatExpr = lst("args",
				expr("this", Operator.Add.priority),
				oper("+"),
				lout_empty
			);
		seCommaExpr = lst("exprs",
				expr("this", Operator.Comma.priority),
				oper(","),
				lout_empty
			);

		seNewExpr = set(
				opt("outer", set(expr("outer", Constants.opAccessPriority), oper("."))),
				kw("new"),
				ident("type"),
				sep("("),
				lst("args",node(),sep(","),lout_empty),
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
					lout_empty
					)
				);
		seNewInitializedArrayExpr = set(
				kw("new"),
				ident("arrtype"),
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
				ident("type_ret"),
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
					par(plIndented, lst("stats",setl(lout_nl,node(new FormatInfoHint("stat"))),null,lout_nl))
					);
			SpaceCmd[] lout_pattern_args = new SpaceCmd[] {
					new SpaceCmd(siSp, SP_NOP, SP_ADD, 0)
				};
			seRewritePattern = set(
					opt("meta"),
					jflag(1,16,1, "@forward"),
					ident("vtype"),
					ident("id"),
					opt("vars", new CalcOptionNotEmpty("vars"),
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
					ident("node_class"),
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
		seTypeClassExpr = set(ident("type"), sep("."), kw("class"));
		seTypeInfoExpr = set(ident("type"), sep("."), kw("type"));
		seAssertEnabledExpr = kw("$assertionsEnabled");
		//seConditionalExpr = set(
		//	expr("cond", Operator.opConditionalPriority+1),
		//	oper("?"),
		//	expr("expr1", Operator.opConditionalPriority+1),
		//	oper(":"),
		//	expr("expr2", Operator.opConditionalPriority)
		//	);
		seCastExpr = set(sep("("), kw("$cast"), attr("type"), sep(")"), expr("expr", Constants.opCastPriority));
		seNopExpr = new SyntaxSpace(new SpaceCmd[0]);
		seNopExpr.is_hidden = true;
		
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

	public SyntaxElem getSyntaxElem(ANode node, FormatInfoHint hint) {
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
		case FormPar: return seFormPar;
		case Var:
			if (hint != null) {
				if ("no-type".equals(hint.text))
					return seVarNoType;
				if ("stat".equals(hint.text))
					return seVarDecl;
			}
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
		
		case InstanceofExpr: return seInstanceofExpr;
		case CallExpr:
			if (((CallExpr)node).func instanceof Constructor)
				return seCallConstr;
			return seCallExpr;
		case ClosureCallExpr: return seClosureCallExpr;
		case StringConcatExpr: return seStringConcatExpr;
		case CommaExpr: return seCommaExpr;
		case TypeClassExpr: return seTypeClassExpr;
		case TypeInfoExpr: return seTypeInfoExpr;
		case AssertEnabledExpr: return 	seAssertEnabledExpr;

		//case ConditionalExpr: return seConditionalExpr;
		case CastExpr: return seCastExpr;
		case NopExpr: return seNopExpr;

		case NewExpr: return seNewExpr;
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

		case ENode:
		{
			ENode e = (ENode)node;
			Operator op = e.getOp();
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				se = expr(op);
				exprs.put(op, se);
			}
			return se;
		}
		}
		return super.getSyntaxElem(node, hint);
	}
}

