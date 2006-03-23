package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.fmt.IndentKind.*;
import static kiev.fmt.NewLineAction.*;
import static kiev.fmt.SpaceAction.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


public class Syntax {
	public static final int SYNTAX_KIND_SPACE   = 1;
	public static final int SYNTAX_KIND_TOKEN   = 2;
	public static final int SYNTAX_KIND_INDEX   = 3;
	public static final int SYNTAX_KIND_ATTR    = 4;
	public static final int SYNTAX_KIND_LIST    = 5;
	public static final int SYNTAX_KIND_ENUM    = 6;
	public static final int SYNTAX_KIND_BLOCK   = 7;
	public static final int SYNTAX_KIND_FIELDS  = 8;
	public static final int SYNTAX_KIND_CONTEXT = 9;
	
	public static final int SYNTAX_ROLE_NONE               = 0;
	public static final int SYNTAX_ROLE_ARR_EMPTY          = 1;
	public static final int SYNTAX_ROLE_ARR_FOLDED         = 2;
	public static final int SYNTAX_ROLE_ARR_PREFIX         = 3;
	public static final int SYNTAX_ROLE_ARR_ELEM_PREFIX    = 4;
	public static final int SYNTAX_ROLE_ARR_ELEMENT        = 5;
	public static final int SYNTAX_ROLE_ARR_ELEM_SUFFIX    = 6;
	public static final int SYNTAX_ROLE_ARR_SEPARATOR      = 7;
	public static final int SYNTAX_ROLE_ARR_SUFFIX         = 8;
	
	public Syntax parent_syntax;
	
	public SyntaxElem getSyntaxElem(ASTNode node) {
		return kw("?"+node.getClass().getName()+"?");
	}
	
	protected final SyntaxSet set(String id, DrawLayout layout, SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(this,id,layout);
		set.elements.addAll(elems);
		return set;
	}

	protected final SyntaxList lst(
			SyntaxElem prefix,
			SyntaxElem elem_prefix,
			SyntaxAttr element,
			SyntaxElem elem_suffix,
			SyntaxElem separator,
			SyntaxElem suffix,
			DrawLayout layout
	)
	{
		SyntaxList lst = new SyntaxList(this,element.ID,layout);
		lst.prefix = prefix;
		lst.elem_prefix = elem_prefix;
		lst.element = element;
		lst.elem_suffix = elem_suffix;
		lst.separator = separator;
		lst.suffix = suffix;
		return lst;
	}

	protected final SyntaxList lst(
			SyntaxAttr element,
			DrawLayout layout
	)
	{
		SyntaxList lst = new SyntaxList(this,element.ID,layout);
		lst.element = element;
		return lst;
	}

	protected final SyntaxAttr attr(String slot)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return new SyntaxAttr(this,slot, lout);
	}

	protected final SyntaxIdentAttr ident(String slot)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{
				new SpaceInfo("word", SP_ADD_BEFORE, 1, 10),
				new SpaceInfo("word", SP_ADD_AFTER, 1, 10),
			}
		);
		return new SyntaxIdentAttr(this,slot,lout);
	}

	protected final SyntaxKeyword kw(String kw)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{
				new SpaceInfo("word", SP_ADD_BEFORE, 1, 10),
				new SpaceInfo("word", SP_ADD_AFTER, 1, 10),
			}
		);
		return new SyntaxKeyword(this,kw,lout);
	}

	protected final SyntaxSeparator sep(String sep)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{
				new SpaceInfo("word", SP_EAT_BEFORE, 1, 10),
				new SpaceInfo("sep", SP_EAT_BEFORE, 1, 10),
				new SpaceInfo("sep", SP_ADD_AFTER, 1, 10),
			}
		);
		return new SyntaxSeparator(this,sep,lout);
	}

	protected final SyntaxSeparator sep(String sep, DrawLayout layout)
	{
		return new SyntaxSeparator(this,sep,layout);
	}
	
	protected final SyntaxElem opt(String prop, SyntaxElem element)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return new SyntaxOptional(this, prop, element, null,lout);
	}

	protected final SyntaxElem alt(String prop, SyntaxElem element, SyntaxElem altern)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return new SyntaxOptional(this, prop, element, altern, lout);
	}
}

public enum IndentKind {
	INDENT_KIND_NONE,
	INDENT_KIND_TOKEN_SIZE,
	INDENT_KIND_FIXED_SIZE,
	INDENT_KIND_UNINDENT
}

public enum NewLineAction {
	NL_ADD_AFTER,
	NL_ADD_GROUP_AFTER,
	NL_ADD_BEFORE,
	NL_ADD_GROUP_BEFORE,
	NL_DEL_BEFORE,
	NL_DEL_GROUP_BEFORE,
	NL_TRANSFER
}

public enum SpaceAction {
	SP_ADD_BEFORE,
	SP_ADD_AFTER,
	SP_EAT_BEFORE,
	SP_EAT_AFTER
}

@node
public class NewLineInfo extends ASTNode {
	@virtual typedef This  = NewLineInfo;

	@att String			name;
	@att NewLineAction	action;
	@att int			from_attempt;
	
	public NewLineInfo() {}
	public NewLineInfo(String name, NewLineAction action, int from_attempt) {
		this.name = name;
		this.action = action;
		this.from_attempt = from_attempt;
	}
}

@node
public class SpaceInfo extends ASTNode {
	@virtual typedef This  = SpaceInfo;

	@att String			name;
	@att SpaceAction	action;
	@att int			text_size;
	@att int			pixel_size;
	
	public SpaceInfo() {}
	public SpaceInfo(String name, SpaceAction action, int text_size, int pixel_size) {
		this.name = name;
		this.action = action;
		this.text_size = text_size;
		this.pixel_size = pixel_size;
	}
}


@node
public final class DrawLayout extends ASTNode {
	@virtual typedef This  = DrawLayout;

	@att int				count;
	@att IndentKind			indent;
	@att NArr<NewLineInfo>	new_lines;
	@att NArr<SpaceInfo>	spaces;
	
	public DrawLayout() {}
	public DrawLayout(int count, IndentKind indent, NewLineInfo[] new_lines, SpaceInfo[] spaces) {
		this.count = count;
		this.indent = indent;
		this.new_lines.addAll(new_lines);
		this.spaces.addAll(spaces);
	}
}

@node
public abstract class SyntaxElem extends ASTNode {
	@virtual typedef This  = SyntaxElem;

	@att public DrawLayout			layout;
	@att public final String		ID;
	@att public boolean				is_hidden;
	
	@ref public final Syntax		stx;

	public SyntaxElem() {}
	public SyntaxElem(Syntax stx, String ID, DrawLayout layout) {
		this.ID = ID.intern();
		this.stx = stx;
		this.layout = layout;
	}

	public abstract Drawable makeDrawable(Formatter fmt, ASTNode node);

}

@node
public class SyntaxSpace extends SyntaxElem {
	@virtual typedef This  = SyntaxSpace;

	public SyntaxSpace() {}
	public SyntaxSpace(Syntax stx, String ID, DrawLayout layout) {
		super(stx,ID,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawSpace(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public abstract class SyntaxToken extends SyntaxElem {
	@virtual typedef This  = SyntaxToken;

	@att public String text;

	public SyntaxToken() {}
	public SyntaxToken(Syntax stx, String ID, String text, DrawLayout layout) {
		super(stx,ID,layout);
		this.text = text.intern();
	}
}

@node
public class SyntaxKeyword extends SyntaxToken {
	@virtual typedef This  = SyntaxKeyword;

	public SyntaxKeyword() {}
	public SyntaxKeyword(Syntax stx, String text, DrawLayout layout) {
		super(stx, text, text, layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawKeyword(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxOperator extends SyntaxToken {
	@virtual typedef This  = SyntaxOperator;

	public SyntaxOperator() {}
	public SyntaxOperator(Syntax stx, String text, DrawLayout layout) {
		super(stx, text, text, layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawOperator(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxSeparator extends SyntaxToken {
	@virtual typedef This  = SyntaxSeparator;

	public SyntaxSeparator() {}
	public SyntaxSeparator(Syntax stx, String text, DrawLayout layout) {
		super(stx, text, text, layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawSeparator(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxOptional extends SyntaxElem {
	@virtual typedef This  = SyntaxOptional;

	@att public String prop;
	@att public SyntaxElem element;
	@att public SyntaxElem altern;

	public SyntaxOptional() {}
	public SyntaxOptional(Syntax stx, String prop, SyntaxElem element, SyntaxElem altern, DrawLayout layout) {
		super(stx, prop, layout);
		this.prop = prop;
		this.element = element;
		this.altern = altern;
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawOptional(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxList extends SyntaxElem {
	@virtual typedef This  = SyntaxList;

	@att public SyntaxElem prefix;
	@att public SyntaxElem elem_prefix;
	@att public SyntaxAttr element;
	@att public SyntaxElem elem_suffix;
	@att public SyntaxElem separator;
	@att public SyntaxElem suffix;

	public SyntaxList() {}
	public SyntaxList(Syntax stx, String id, DrawLayout layout) {
		super(stx,id,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawNonTermList(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxSet extends SyntaxElem {
	@virtual typedef This  = SyntaxSet;

	@att public NArr<SyntaxElem> elements;

	public SyntaxSet() {}
	public SyntaxSet(Syntax stx, String id, DrawLayout layout) {
		super(stx,id,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawNonTermSet(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxAttr extends SyntaxElem {
	@virtual typedef This  = SyntaxAttr;

	@att public String name;

	public SyntaxAttr() {}
	public SyntaxAttr(Syntax stx, String name, DrawLayout layout) {
		super(stx,name,layout);
		this.name = name.intern();
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = fmt.getDrawable((ASTNode)node.getVal(name));
		//dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxIdentAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxIdentAttr;

	public SyntaxIdentAttr() {}
	public SyntaxIdentAttr(Syntax stx, String name, DrawLayout layout) {
		super(stx,name,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawNodeTerm(node, this, name);
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
	
	public JavaSyntax() {
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
					opt("pkg.name",
						set("package", lout_pkg, kw("package"), ident("pkg"), sep(";")	)
						),
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
			seImport = set("import", lout_syntax,
				kw("import"), ident("name"), opt("star", sep(".*")), sep(";")
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
			DrawLayout lout_form_par = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{},
				new SpaceInfo[]{}
			);
			// formal parameter
			seFormPar = set("form-par", lout_form_par,
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
				alt("body", attr("body"), sep(";"))
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
			// block expression
			seBlock = lst(
					sep("{", lout_code_block_start),
					null,
					attr("stats"),
					null,
					null,
					sep("}", lout_code_block_end),
					lout_code_block
					);
		}
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
		case Block: return seBlock;
		}
		return super.getSyntaxElem(node);
	}
}

