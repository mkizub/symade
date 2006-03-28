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
	final SyntaxElem seField;
	final SyntaxElem seFormPar;
	final SyntaxElem seConstructor;
	final SyntaxElem seMethod;
	final SyntaxElem seBlock;
	final SyntaxElem seExprStat;
	final SyntaxElem seTypeRef;
	final SyntaxElem seConstExpr;
	final SyntaxElem seLVarExpr;
	final SyntaxElem seThisExpr;
	final SyntaxElem seBinaryBooleanOrExpr;
	final SyntaxElem seBinaryBooleanAndExpr;
	final SyntaxElem seInstanceofExpr;
	final SyntaxElem seBooleanNotExpr;
	final SyntaxElem seCallExpr;
	
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
			seField = set("field", lout_field,
				ident("ftype"), ident("name"), sep(";")
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
		seExprStat = set("stat-expr", lout_empty.ncopy(), attr("expr"), sep(";"));
		
		exprs = new Hashtable<Operator, SyntaxElem>();
		seConstExpr = attr("value");
		seTypeRef = ident("lnk");
		seLVarExpr = ident("ident");
		seThisExpr = kw("this");
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
	}
	public SyntaxElem getSyntaxElem(ASTNode node) {
		switch (node) {
		case FileUnit: return seFileUnit;
		case Import: return seImport;
		case Struct: return seStruct;
		case Field: return seField;
		case FormPar: return seFormPar;
		case Constructor: return seConstructor;
		case Method: return seMethod;
		case ExprStat: return seExprStat;
		case Block: return seBlock;
		case ConstExpr: return seConstExpr;
		case TypeRef: return seTypeRef;
		case LVarExpr: return seLVarExpr;
		case ThisExpr: return seThisExpr;
		case BinaryBooleanOrExpr: return seBinaryBooleanOrExpr;
		case BinaryBooleanAndExpr: return seBinaryBooleanAndExpr;
		case InstanceofExpr: return seInstanceofExpr;
		case BooleanNotExpr: return seBooleanNotExpr;
		case CallExpr: return seCallExpr;
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
			Operator op = ((BinaryExpr)node).op;
			SyntaxElem se = exprs.get(op);
			if (se == null) {
				se = expr("expr1", op, "expr2");
				exprs.put(op, se);
			}
			return se;
		}
		}
		return super.getSyntaxElem(node);
	}
}

