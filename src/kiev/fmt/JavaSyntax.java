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
public class SyntaxJavaPackage extends SyntaxSet {
	@virtual typedef This  = SyntaxJavaPackage;

	public SyntaxJavaPackage() {}
	public SyntaxJavaPackage(Syntax stx, String id, DrawLayout layout) {
		super(stx,id,layout);
		this.elements.add(stx.kw("package"));
		this.elements.add(stx.ident("pkg"));
		this.elements.add(stx.sep(";"));
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawJavaPackage(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxJavaImport extends SyntaxSet {
	@virtual typedef This  = SyntaxJavaImport;

	public SyntaxJavaImport() {}
	public SyntaxJavaImport(Syntax stx, String id, DrawLayout layout) {
		super(stx,id,layout);
		this.elements.add(stx.kw("import"));
		this.elements.add(stx.ident("name"));
		this.elements.add(stx.sep(".*"));
		this.elements.add(stx.sep(";"));
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawJavaImport(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxJavaMethodBody extends SyntaxChoice {
	@virtual typedef This  = SyntaxJavaMethodBody;

	public SyntaxJavaMethodBody() {}
	public SyntaxJavaMethodBody(Syntax stx, String id, DrawLayout layout, SyntaxElem empty, SyntaxElem present) {
		super(stx,id,layout);
		this.elements.add(empty);
		this.elements.add(present);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawJavaMethodBody(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxJavaExpr extends SyntaxAttr {
	@virtual typedef This  = SyntaxJavaExpr;

	@att public int					priority;
	@att public SyntaxSeparator		l_paren;
	@att public SyntaxSeparator		r_paren;

	public SyntaxJavaExpr() {}
	public SyntaxJavaExpr(Syntax stx, String name, DrawLayout layout, int priority) {
		super(stx,name,layout);
		this.priority = priority;
		this.l_paren = stx.sep("(");
		this.r_paren = stx.sep(")");
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		ASTNode n = (name == "this") ? node : (ASTNode)node.getVal(name);
		Drawable dr = new DrawJavaExpr(n, this);
		dr.init(fmt);
		return dr;
	}
}

public class JavaSyntax extends Syntax {
	final SyntaxElem seFileUnit;
	final SyntaxElem seStruct;
	final SyntaxElem seImport;
	final SyntaxElem seFieldDecl;
	final SyntaxElem seVarDecl;
	final SyntaxElem seVar;
	final SyntaxElem seFormPar;
	final SyntaxElem seConstructor;
	final SyntaxElem seMethod;
	
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
	
	protected final SyntaxJavaExpr expr(String expr, int priority)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		SyntaxJavaExpr se = new SyntaxJavaExpr(this, expr, lout, priority);
		return se;
	}

	protected final SyntaxSet expr(String expr1, Operator op, String expr2)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		String id = op.toString().intern();
		return set(id, lout, expr(expr1, op.getArgPriority(0)), oper(op), expr(expr2, op.getArgPriority(1)));
	}

	protected final SyntaxSet expr(Operator op, String expr2)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		String id = op.toString().intern();
		return set(id, lout, oper(op), expr(expr2, op.getArgPriority()));
	}

	protected final SyntaxSet expr(String expr1, Operator op)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		String id = op.toString().intern();
		return set(id, lout, expr(expr1, op.getArgPriority()), oper(op));
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
			seFileUnit = set("file", lout_file,
					new SyntaxJavaPackage(this, "package", lout_pkg),
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
			seImport = new SyntaxJavaImport(this, "import", lout_syntax);
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
			// struct
			seStruct = set("struct", lout_struct,
				set("struct_hdr", lout_struct_hdr, kw("class"), ident("short_name")),
				lst(
					sep("{", lout_struct_block_start),
					null,
					attr("members"),
					null,
					null,
					sep("}", lout_struct_block_end),
					lout_struct_end
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
			seFieldDecl = set("field-decl", lout_field.ncopy(),
				ident("ftype"), ident("name"), opt("init", set("=", lout_empty.ncopy(), oper("="), expr("init", Constants.opAssignPriority))), sep(";")
				);
			// vars
			seVarDecl = set("var_decl", lout_field.ncopy(),
				attr("var"), sep(";")
				);
			seVar = set("var_decl", lout_field.ncopy(),
				ident("vtype"), ident("name"), opt("init", set("=", lout_empty.ncopy(), oper("="), expr("init", Constants.opAssignPriority)))
				);
		}
		{
			// formal parameter
			seFormPar = set("form-par", lout_empty.ncopy(),
				ident("vtype"), ident("name")
				);
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
			seConstructor = set("ctor", lout_method.ncopy(),
				set("method_hdr", lout_method_hdr.ncopy(),
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
			seMethod = set("method", lout_method.ncopy(),
				set("method_hdr", lout_method_hdr.ncopy(),
					ident("type_ret"), ident("name"),
					lst(sep("("),
						null,
						attr("params"),
						null,
						sep(","),
						sep(")"),
						lout_params.ncopy())
					),
				new SyntaxJavaMethodBody(this, "body", lout_body, sep(";"), attr("body"))
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
					new SyntaxSpace(this, " ", lout_code_block_stat),
					null,
					sep("}", lout_code_block_end),
					lout_code_block
					);
		}
		seExprStat = set("stat", lout_empty.ncopy(), attr("expr"), sep(";"));
		seReturnStat = set("stat", lout_empty.ncopy(), kw("return"), attr("expr"), sep(";"));
		seThrowStat = set("stat", lout_empty.ncopy(), kw("throw"), attr("expr"), sep(";"));
		seIfElseStat = set("stat", lout_empty.ncopy(), kw("if"), sep("("), attr("cond"), sep(")"), attr("thenSt"), opt("elseSt", set("else", lout_empty.ncopy(), kw("else"), attr("elseSt"))));
		seCondStat = set("stat", lout_empty.ncopy(), attr("cond"), opt("message", set("message", lout_empty.ncopy(), sep(":"), attr("message"))), sep(";"));
		seLabeledStat = set("stat", lout_empty.ncopy(), ident("ident"), sep(":"), attr("stat"));
		seBreakStat = set("stat", lout_empty.ncopy(), kw("break"), opt("ident", ident("ident")), sep(";"));
		seContinueStat = set("stat", lout_empty.ncopy(), kw("continue"), opt("ident", ident("ident")), sep(";"));
		seGotoStat = set("stat", lout_empty.ncopy(), kw("goto"), opt("ident", ident("ident")), sep(";"));
		seGotoCaseStat = set("stat", lout_empty.ncopy(), kw("goto"), kw("case"), attr("expr"), sep(";"));

		seWhileStat = set("stat", lout_empty.ncopy(), kw("while"), sep("("), attr("cond"), sep(")"), attr("body"));
		seDoWhileStat = set("stat", lout_empty.ncopy(), kw("do"), attr("body"), kw("while"), sep("("), attr("cond"), sep(")"), sep(";"));
		seForInit = lst(null,null,attr("decls"),null,null,null,lout_empty.ncopy());
		seForStat = set("stat", lout_empty.ncopy(), kw("for"), sep("("), attr("init"), sep(";"), attr("cond"), sep(";"), attr("iter"), sep(")"), attr("body"));
		seForEachStat = set("stat", lout_empty.ncopy(), kw("foreach"), sep("("), attr("var"), sep(";"), attr("container"), opt("filter", set("", lout_empty.ncopy(), sep(";"), attr("cond"))), sep(")"), attr("body"));
		
		seCaseLabel = set("stat", lout_empty.ncopy(), kw("case"), attr("val"), sep(":"), lst(attr("stats"),lout_empty.ncopy()));
		seSwitchStat = set("stat", lout_empty.ncopy(), kw("switch"), sep("("), attr("sel"), sep(")"), lst(sep("{"),null,attr("cases"),null,null,sep("}"),lout_empty.ncopy()));
		seCatchInfo = set("stat", lout_empty.ncopy(), kw("catch"), sep("("), attr("arg"), sep(")"), attr("body"));
		seFinallyInfo = set("stat", lout_empty.ncopy(), kw("finally"), attr("body"));
		seTryStat = set("stat", lout_empty.ncopy(), kw("try"), attr("body"), lst(attr("catchers"),lout_empty.ncopy()), opt("finally_catcher", attr("finally_catcher")));
		seSynchronizedStat = set("stat", lout_empty.ncopy(), kw("synchronized"), sep("("), attr("expr"), sep(")"), attr("body"));
		seWithStat = set("stat", lout_empty.ncopy(), kw("with"), sep("("), attr("expr"), sep(")"), attr("body"));
	
		exprs = new Hashtable<Operator, SyntaxElem>();
		seConstExpr = attr("this");
		seTypeRef = ident("lnk");
		
		seAccessExpr = set("expr", lout_empty.ncopy(), attr("obj"), oper(BinaryOperator.Access), ident("ident"));
		seIFldExpr = set("expr", lout_empty.ncopy(), attr("obj"), oper(BinaryOperator.Access), ident("ident"));
		seContainerAccessExpr = set("expr", lout_empty.ncopy(), attr("obj"), sep("["), attr("index"), sep("]"));
		seThisExpr = kw("this");
		seLVarExpr = ident("ident");
		seSFldExpr = set("expr", lout_empty.ncopy(), attr("obj"), oper(BinaryOperator.Access), ident("ident"));
		seOuterThisAccessExpr = set("expr", lout_empty.ncopy(), attr("obj"), oper(BinaryOperator.Access), kw("this"));
		seReinterpExpr = set("expr", lout_empty.ncopy(), sep("("), kw("$reinterp"), attr("type"), sep(")"), attr("expr"));
		
		seBinaryBooleanOrExpr = expr("expr1", BinaryOperator.BooleanOr, "expr2");
		seBinaryBooleanAndExpr = expr("expr1", BinaryOperator.BooleanAnd, "expr2");
		seInstanceofExpr = expr("expr", BinaryOperator.InstanceOf, "type");
		seBooleanNotExpr = expr(PrefixOperator.BooleanNot, "expr");
		seCallExpr = set("call_expr", lout_empty.ncopy(),
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

		seNewExpr = set("new_expr", lout_empty.ncopy(),
				kw("new"),
				opt("outer", set("outer", lout_empty.ncopy(), attr("outer"), oper("."))),
				ident("type"),
				lst(sep("("),
						null,
						attr("args"),
						null,
						sep(","),
						sep(")"),
						lout_empty.ncopy()
					),
				opt("clazz", kw("{...}"))
				);
		seNewArrayExpr = set("new_arr", lout_empty.ncopy(),
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
		seNewInitializedArrayExpr = set("new_init_arr", lout_empty.ncopy(),
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
		seNewClosure = set("new_closure", lout_empty.ncopy(),
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
		seArrayLengthExpr = set("expr", lout_empty.ncopy(), attr("obj"), oper(BinaryOperator.Access), kw("length"));
		seTypeClassExpr = set("expr", lout_empty.ncopy(), ident("type"), oper(BinaryOperator.Access), kw("class"));
		seTypeInfoExpr = set("expr", lout_empty.ncopy(), ident("type"), oper(BinaryOperator.Access), kw("type"));
		seConditionalExpr = set("expr", lout_empty.ncopy(),
			expr("cond", MultiOperator.opConditionalPriority+1),
			oper("?"),
			expr("expr1", MultiOperator.opConditionalPriority+1),
			oper(":"),
			expr("expr2", MultiOperator.opConditionalPriority)
			);
		seCastExpr = set("expr", lout_empty.ncopy(), sep("("), kw("$cast"), attr("type"), sep(")"), attr("expr"));
	}
	public SyntaxElem getSyntaxElem(ASTNode node) {
		switch (node) {
		case FileUnit: return seFileUnit;
		case Import: return seImport;
		case Struct: return seStruct;
		case Field: return seFieldDecl;
		case VarDecl: return seVarDecl;
		case FormPar: return seFormPar;
		case Var: return seVar;
		case Constructor: return seConstructor;
		case Method: return seMethod;
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
		return super.getSyntaxElem(node);
	}
}

