package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.Operator;

import static kiev.fmt.IndentKind.*;
import static kiev.fmt.NewLineAction.*;
import static kiev.fmt.SpaceAction.*;

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
	
	
	protected final SyntaxElem jflag(int size, int offs, int val, String name)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return new SyntaxOptional(name, new CalcOptionJavaFlag(size, offs, val), kw(name), null, lout);
	}

	protected final SyntaxJavaExpr expr(String expr, int priority)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		SyntaxJavaExpr se = new SyntaxJavaExpr(expr, lout, priority, sep("("), sep(")"));
		return se;
	}

	protected final SyntaxSet expr(String expr1, Operator op, String expr2)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return setl(lout, expr(expr1, op.getArgPriority(0)), oper(op), expr(expr2, op.getArgPriority(1)));
	}

	protected final SyntaxSet expr(Operator op, String expr2)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return setl(lout, oper(op), expr(expr2, op.getArgPriority()));
	}

	protected final SyntaxSet expr(String expr1, Operator op)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return setl(lout, expr(expr1, op.getArgPriority()), oper(op));
	}

	public JavaSyntax() {
		DrawLayout lout_empty = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		{
			DrawLayout lout_file = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("after_file", NL_ADD_AFTER, 0)
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_pkg = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("after_package", NL_ADD_GROUP_AFTER, 0)
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_syntax = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("file_syntax_group", NL_ADD_GROUP_AFTER, 0)
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_members = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("file_members_group", NL_ADD_GROUP_AFTER, 0)
				},
				new SpaceInfo[]{}
			);
			// file unit
			seFileUnit = setl(lout_file,
					opt("pkg", setl(lout_pkg, kw("package"), ident("pkg"), sep(";"))),
					lst(attr("syntax"), lout_syntax),
					lst(attr("members"), lout_members)
				);
		}
		{
			DrawLayout lout_syntax = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("file_syntax", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			// import
			seImport = setl(lout_syntax.ncopy(),
				kw("import"),
				ident("name"),
				opt("star",new CalcOptionTrue("star"), sep(".*"), null, lout_empty.ncopy()),
				sep(";"));
			seOpdef = setl(lout_syntax.ncopy(),
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
			seTypeDef = setl(lout_syntax.ncopy(), kw("typedef"), ident("name"), 
				lst(null, oper("\u2264"), attr("upper_bound"), null, null, null, lout_empty.ncopy()),
				lst(null, oper("\u2265"), attr("lower_bound"), null, null, null, lout_empty.ncopy()),
				sep(";")
				);
		}
		{
			DrawLayout lout_struct = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("struct", NL_ADD_GROUP_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_struct_hdr = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("struct_hdr", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_struct_block_start = new DrawLayout(1, INDENT_KIND_FIXED_SIZE,
				new NewLineInfo[]{
					new NewLineInfo("struct_hdr", NL_TRANSFER, 0),
					new NewLineInfo("block_start", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{
					new SpaceInfo("block_start", SP_ADD_BEFORE, 1, 10),
					new SpaceInfo("block_start", SP_ADD_AFTER, 1, 10),
				}
			);
			DrawLayout lout_struct_block_end = new DrawLayout(1, INDENT_KIND_UNINDENT,
				new NewLineInfo[]{
					new NewLineInfo("block_end", NL_ADD_BEFORE, 0),
					new NewLineInfo("block_end", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{
					new SpaceInfo("block_start", SP_ADD_BEFORE, 1, 10),
					new SpaceInfo("block_start", SP_ADD_AFTER, 1, 10),
				}
			);
			DrawLayout lout_struct_end = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("struct", NL_ADD_GROUP_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			SyntaxElem struct_prefix = setl(lout_struct_hdr.ncopy(),
					attr("meta"),
					jflag(1,3,1,  "static"),
					jflag(1,4,1,  "final"),
					jflag(1,10,1, "abstract"),
					jflag(1,11,1, "strict")
					);
			// anonymouse struct
			seStructBody = lst(
					sep("{", lout_struct_block_start),
					null,
					attr("members"),
					null,
					null,
					sep("}", lout_struct_block_end),
					lout_struct_end
					);
			// class
			seStructClass = setl(lout_struct.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						kw("class"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// interface
			seStructInterface = setl(lout_struct.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						kw("interface"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// interface
			seStructAnnotation = setl(lout_struct.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						kw("@interface"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// enum
			seStructEnum = setl(lout_struct.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						kw("enum"),
						ident("short_name")),
					seStructBody.ncopy()
				);
			// syntax
			seStructSyntax = setl(lout_struct.ncopy(),
					setl(lout_struct_hdr.ncopy(),
						struct_prefix.ncopy(),
						kw("syntax"),
						ident("short_name")),
					seStructBody.ncopy()
				);
		}
		{
			DrawLayout lout_meta = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("meta", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			seMetaSet = lst(attr("metas"), lout_empty.ncopy());
			seMeta = setl(lout_meta, oper("@"), ident("type"), lst(sep("("),
						null,
						attr("values"),
						null,
						sep(","),
						sep(")"),
						lout_empty.ncopy()
					));
			seMetaValueScalar = set(ident("type"), oper("="), attr("value"));
			seMetaValueArray = set(ident("type"), oper("="),
					lst(sep("{"),
						null,
						attr("values"),
						null,
						sep(","),
						sep("}"),
						lout_empty.ncopy()
						)
					);
		}
		{
			DrawLayout lout_field = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("field_decl_group", NL_TRANSFER, 0),
					new NewLineInfo("field_decl_group", NL_ADD_GROUP_AFTER, 0),
					new NewLineInfo("field_decl", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			// field
			seFieldDecl = setl(lout_field.ncopy(), attr("meta"),
				ident("ftype"), ident("name"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority))), sep(";")
				);
			// vars
			seVarDecl = set(attr("var"), sep(";"));
			seVar = set(opt("meta"),
				ident("vtype"), ident("name"), opt("init", set(oper("="), expr("init", Constants.opAssignPriority)))
				);
			// formal parameter
			seFormPar = set(opt("meta"), ident("vtype"), ident("name")	);
		}
		{
			DrawLayout lout_method = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("method_decl", NL_ADD_GROUP_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_method_hdr = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("method_params", NL_DEL_BEFORE, 0),
					new NewLineInfo("method_hdr", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_params = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("method_params", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			DrawLayout lout_body = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("method_params", NL_DEL_BEFORE, 0),
					new NewLineInfo("method_hdr", NL_DEL_BEFORE, 0),
				},
				new SpaceInfo[]{}
			);
			// constructor
			seConstructor = setl(lout_method.ncopy(),
				setl(lout_method_hdr.ncopy(), attr("meta"),
					ident("parent.short_name"),
					lst(sep("("),
						null,
						attr("params"),
						null,
						sep(","),
						sep(")"),
						lout_params.ncopy())
					),
				attr("body")
				);
			// method
			seMethod = setl(lout_method.ncopy(),
				setl(lout_method_hdr.ncopy(), attr("meta"),
					ident("type_ret"), ident("name"),
					lst(sep("("),
						null,
						attr("params"),
						null,
						sep(","),
						sep(")"),
						lout_params.ncopy())
					),
				opt("body", new CalcOptionNotNull("body"),attr("body"), sep(";"), lout_body)
				);
			seInitializer = setl(lout_method.ncopy(), attr("meta"),
					attr("body")
				);
		}
		{
			DrawLayout lout_code_block = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{},
				new SpaceInfo[]{}
			);
			DrawLayout lout_code_block_start = new DrawLayout(1, INDENT_KIND_FIXED_SIZE,
				new NewLineInfo[]{
					new NewLineInfo("method_hdr", NL_DEL_BEFORE, 0),
					new NewLineInfo("method_params", NL_DEL_BEFORE, 0),
					new NewLineInfo("block_start", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{
					new SpaceInfo("block_start", SP_ADD_BEFORE, 1, 10),
					new SpaceInfo("block_start", SP_ADD_AFTER, 1, 10),
				}
			);
			DrawLayout lout_code_block_end = new DrawLayout(1, INDENT_KIND_UNINDENT,
				new NewLineInfo[]{
					new NewLineInfo("block_start", NL_DEL_BEFORE, 0),
					new NewLineInfo("block_end", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{
					new SpaceInfo("block_start", SP_ADD_BEFORE, 1, 10),
					new SpaceInfo("block_start", SP_ADD_AFTER, 1, 10),
				}
			);
			DrawLayout lout_code_block_stat = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("stat_end", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{}
			);
			// block expression
			seBlock = lst(
					sep("{", lout_code_block_start),
					null,
					attr("stats"),
					new SyntaxSpace(lout_code_block_stat),
					null,
					sep("}", lout_code_block_end),
					lout_code_block
					);
		}
		seExprStat = set(attr("expr"), sep(";"));
		seReturnStat = set(kw("return"), attr("expr"), sep(";"));
		seThrowStat = set(kw("throw"), attr("expr"), sep(";"));
		seCondStat = set(attr("cond"), opt("message", set(sep(":"), attr("message"))), sep(";"));
		seLabeledStat = set(ident("ident"), sep(":"), attr("stat"));
		seBreakStat = set(kw("break"), opt("ident", ident("ident")), sep(";"));
		seContinueStat = set(kw("continue"), opt("ident", ident("ident")), sep(";"));
		seGotoStat = set(kw("goto"), opt("ident", ident("ident")), sep(";"));
		seGotoCaseStat = set(kw("goto"), kw("case"), attr("expr"), sep(";"));

		{
			DrawLayout lout_cond = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{
					new NewLineInfo("cond_end", NL_ADD_AFTER, 0),
				},
				new SpaceInfo[]{
					new SpaceInfo("kw_start", SP_ADD_BEFORE, 1, 10),
					new SpaceInfo("cond_end", SP_ADD_AFTER, 1, 10),
				}
			);
			seIfElseStat = set(kw("if"), setl(lout_cond.ncopy(), sep("("), attr("cond"), sep(")")), attr("thenSt"), opt("elseSt", set(kw("else"), attr("elseSt"))));
			seWhileStat = set(kw("while"), setl(lout_cond.ncopy(), sep("("), attr("cond"), sep(")")), attr("body"));
			seDoWhileStat = set(kw("do"), attr("body"), kw("while"), setl(lout_cond.ncopy(), sep("("), attr("cond"), sep(")")), sep(";"));
			seForInit = lst(null,null,attr("decls"),null,null,null,lout_empty.ncopy());
			seForStat = set(kw("for"), setl(lout_cond.ncopy(), sep("("), attr("init"), sep(";"), attr("cond"), sep(";"), attr("iter"), sep(")")), attr("body"));
			seForEachStat = set(kw("foreach"), setl(lout_cond.ncopy(), sep("("), attr("var"), sep(";"), attr("container"), opt("cond", set(sep(";"), attr("cond"))), sep(")")), attr("body"));
		
			seCaseLabel = set(kw("case"), attr("val"), sep(":"), lst(attr("stats"),lout_empty.ncopy()));
			seSwitchStat = set(kw("switch"), setl(lout_cond.ncopy(), sep("("), attr("sel"), sep(")")), lst(sep("{"),null,attr("cases"),null,null,sep("}"),lout_empty.ncopy()));
			seCatchInfo = set(kw("catch"), setl(lout_cond.ncopy(), sep("("), attr("arg"), sep(")")), attr("body"));
			seFinallyInfo = set(kw("finally"), attr("body"));
			seTryStat = set(kw("try"), attr("body"), lst(attr("catchers"),lout_empty.ncopy()), opt("finally_catcher", attr("finally_catcher")));
			seSynchronizedStat = set(kw("synchronized"), sep("("), attr("expr"), sep(")"), attr("body"));
			seWithStat = set(kw("with"), sep("("), attr("expr"), sep(")"), attr("body"));
		}
	
		exprs = new Hashtable<Operator, SyntaxElem>();
		seConstExpr = attr("this");
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
				lst(sep("("),
						null,
						attr("args"),
						null,
						sep(","),
						sep(")"),
						lout_empty.ncopy()
					)
				);
		seClosureCallExpr = set(
				expr("expr", Constants.opCallPriority),
				lst(sep("("),
						null,
						attr("args"),
						null,
						sep(","),
						sep(")"),
						lout_empty.ncopy()
					)
				);
		seStringConcatExpr = lst(
				null,
				null,
				expr("args", BinaryOperator.Add.priority),
				null,
				oper(BinaryOperator.Add),
				null,
				lout_empty.ncopy()
			);
		seCommaExpr = lst(
				null,
				null,
				expr("exprs", BinaryOperator.Comma.priority),
				null,
				oper(BinaryOperator.Comma),
				null,
				lout_empty.ncopy()
			);

		seNewExpr = set(
				kw("new"),
				opt("outer", set(attr("outer"), oper("."))),
				ident("type"),
				lst(sep("("),
						null,
						attr("args"),
						null,
						sep(","),
						sep(")"),
						lout_empty.ncopy()
					),
				opt("clazz", attr("clazz", new FormatInfoHint("anonymouse")))
				);
		seNewArrayExpr = set(
				kw("new"),
				ident("type"),
				lst(null,
					sep("["),
					attr("args"),
					sep("]"),
					null,
					null,
					lout_empty.ncopy()
					)
				);
		seNewInitializedArrayExpr = set(
				kw("new"),
				ident("arrtype"),
				lst(sep("{"),
					null,
					attr("args"),
					null,
					sep(","),
					sep("}"),
					lout_empty.ncopy()
					)
				);
		seNewClosure = set(
				kw("fun"),
				lst(sep("("),
						null,
						attr("params"),
						null,
						sep(","),
						sep(")"),
						lout_empty.ncopy()
					),
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
		case ConstExpr: return seConstExpr;
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

