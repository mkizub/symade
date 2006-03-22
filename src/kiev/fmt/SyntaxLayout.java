package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

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
	
	protected final SyntaxSet set(String id, int layout, SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(this,id, new int[]{layout});
		set.elements = elems;
		return set;
	}

	protected final SyntaxList lst(
			SyntaxElem prefix,
			SyntaxElem elem_prefix,
			SyntaxAttr element,
			SyntaxElem elem_suffix,
			SyntaxElem separator,
			SyntaxElem suffix
	)
	{
		SyntaxList lst = new SyntaxList(this,element.ID, new int[]{0x0000});
		lst.prefix = prefix;
		lst.elem_prefix = elem_prefix;
		lst.element = element;
		lst.elem_suffix = elem_suffix;
		lst.separator = separator;
		lst.suffix = suffix;
		return lst;
	}

	protected final SyntaxList lst(
			SyntaxAttr element
	)
	{
		SyntaxList lst = new SyntaxList(this,element.ID, new int[]{0x0000});
		lst.element = element;
		return lst;
	}

	protected final SyntaxList lst_sf(
			SyntaxAttr element,
			SyntaxElem elem_suffix
	)
	{
		SyntaxList lst = new SyntaxList(this,element.ID, new int[]{0x0000});
		lst.element = element;
		lst.elem_suffix = elem_suffix;
		return lst;
	}

	protected final SyntaxAttr attr(String slot)
	{
		return new SyntaxAttr(this,slot, new int[]{0x1100});
	}

	protected final SyntaxIdentAttr ident(String slot)
	{
		return new SyntaxIdentAttr(this,slot, new int[]{0x1100});
	}

	protected final SyntaxKeyword kw(String kw)
	{
		return new SyntaxKeyword(this,kw, new int[]{0x1100});
	}

	protected final SyntaxSeparator sep(String sep)
	{
		return new SyntaxSeparator(this,sep, new int[]{0xF100});
	}

	protected final SyntaxSeparator sep(String sep, int layout)
	{
		return new SyntaxSeparator(this,sep,new int[]{layout});
	}
	
	protected final SyntaxElem opt(SyntaxAttr opt, SyntaxElem element)
	{
		return new SyntaxOptional(this, opt, element, new int[]{0x1100});
	}

	protected final int lout(int nl, int el, int er, int ind) {
		int lout = nl & 0xFF;
		lout |= (er & 0xF) << 8;
		lout |= (el & 0xF) << 12;
		lout |= (ind & 3) << 18;
		return lout;
	}

}

public abstract class SyntaxElem {

	public static final int INDENT_KIND_NONE			= 0;
	public static final int INDENT_KIND_TOKEN_SIZE		= 1;
	public static final int INDENT_KIND_FIXED_SIZE		= 2;
	public static final int INDENT_KIND_UNINDENT		= 3;

	// newline types mask
	public static final int NL_MASK_TRANSFERABLE		=  1;
	public static final int NL_MASK_DISCARDABLE		=  2;
	public static final int NL_MASK_DOUBLE				=  4;
	public static final int NL_MASK_BEFORE_TOKEN		=  8;
	public static final int NL_MASK_FORSED				= 16;
	public static final int NL_MASK_DO_TRANSFER		=  2 <<  16;
	public static final int NL_MASK_DO_DISCARD			=  4 <<  26;
	public static final int NL_MASK_DO_DISCARD_DBL		=  8 <<  46;
	// newline consts
	public static final int NL_NONE         = 0;
	public static final int NL_TRANS        =           NL_MASK_TRANSFERABLE;
	public static final int NL_DICS         = NL_TRANS |NL_MASK_DISCARDABLE;

	private int[] layouts;

	public final String ID;
	public final Syntax stx;

	public SyntaxElem(Syntax stx, String ID, int[] layouts) {
		this.ID = ID.intern();
		this.stx = stx;
		this.layouts = layouts;
	}

	public abstract Drawable makeDrawable(Formatter fmt, ASTNode node);

	public final int getLayoutsCount() {
		return layouts.length;
	}
	
	public final int getNewlineKind(int idx) {
		if (idx >= layouts.length) idx = layouts.length - 1;
		return layouts[idx] & 0xFF;
	}

	public final int getExtraSpaceRight(int idx) {
		if (idx >= layouts.length) idx = layouts.length - 1;
		return (layouts[idx] << 20) >> 28;
	}

	public final int getExtraSpaceLeft(int idx) {
		if (idx >= layouts.length) idx = layouts.length - 1;
		return (layouts[idx] << 16) >> 28;
	}

	public final boolean isHidden(int idx) {
		if (idx >= layouts.length) idx = layouts.length - 1;
		return ((layouts[idx] >> 16) & 1) != 0;
	}

	public final boolean isRightAssociated(int idx) {
		if (idx >= layouts.length) idx = layouts.length - 1;
		return ((layouts[idx] >> 17) & 1) != 0;
	}

	public final int getIndentKind(int idx) {
		if (idx >= layouts.length) idx = layouts.length - 1;
		return (layouts[idx] >> 18) & 3;
	}
}

public class SyntaxSpace extends SyntaxElem {
	public SyntaxSpace(Syntax stx, String ID, int[] layouts) {
		super(stx,ID,layouts);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawSpace(node, this);
		dr.init(fmt);
		return dr;
	}
}

public abstract class SyntaxToken extends SyntaxElem {
	public final String text;
	public SyntaxToken(Syntax stx, String ID, String text, int[] layouts) {
		super(stx,ID,layouts);
		this.text = text.intern();
	}
}

public class SyntaxKeyword extends SyntaxToken {
	public SyntaxKeyword(Syntax stx, String text, int[] layouts) {
		super(stx, text, text, layouts);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawKeyword(node, this);
		dr.init(fmt);
		return dr;
	}
}

public class SyntaxOperator extends SyntaxToken {
	public SyntaxOperator(Syntax stx, String text, int[] layouts) {
		super(stx, text, text, layouts);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawOperator(node, this);
		dr.init(fmt);
		return dr;
	}
}

public class SyntaxSeparator extends SyntaxToken {
	public SyntaxSeparator(Syntax stx, String text, int[] layouts) {
		super(stx, text, text, layouts);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawSeparator(node, this);
		dr.init(fmt);
		return dr;
	}
}

public class SyntaxOptional extends SyntaxElem {
	public SyntaxAttr opt;
	public SyntaxElem element;

	public SyntaxOptional(Syntax stx, SyntaxAttr opt, SyntaxElem element, int[] layouts) {
		super(stx, opt.name, layouts);
		this.opt = opt;
		this.element = element;
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawOptional(node, this);
		dr.init(fmt);
		return dr;
	}
}

public class SyntaxList extends SyntaxElem {
	public SyntaxElem prefix;
	public SyntaxElem elem_prefix;
	public SyntaxAttr element;
	public SyntaxElem elem_suffix;
	public SyntaxElem separator;
	public SyntaxElem suffix;
	public SyntaxList(Syntax stx, String id, int[] layouts) {
		super(stx,id,layouts);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawNonTermList(node, this);
		dr.init(fmt);
		return dr;
	}
}

public class SyntaxSet extends SyntaxElem {
	public SyntaxElem[] elements;
	public SyntaxSet(Syntax stx, String id, int[] layouts) {
		super(stx,id,layouts);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawNonTermSet(node, this);
		dr.init(fmt);
		return dr;
	}
}

public class SyntaxAttr extends SyntaxElem {
	public final String name;
	public SyntaxAttr(Syntax stx, String name, int[] layouts) {
		super(stx,name,layouts);
		this.name = name.intern();
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = fmt.getDrawable((ASTNode)node.getVal(name));
		dr.init(fmt);
		return dr;
	}
}

public class SyntaxIdentAttr extends SyntaxAttr {
	public final String name;
	public SyntaxIdentAttr(Syntax stx, String name, int[] layouts) {
		super(stx,name,layouts);
		this.name = name.intern();
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
	final SyntaxElem seFormPar;
	final SyntaxElem seConstructor;
	
	public JavaSyntax() {
		// file unit
		seFileUnit = set("file", 0,
				opt(attr("pkg"),
					set("package", lout(SyntaxElem.NL_MASK_FORSED|SyntaxElem.NL_MASK_DOUBLE,-1,0,0),
						kw("package"), ident("pkg"), sep(";")
						)
					),
				lst(attr("syntax")),
				lst(attr("members"))
			);
		// import
		seImport = set("import", lout(SyntaxElem.NL_MASK_FORSED,-1,0,0),
			kw("import"), ident("name"), opt(attr("star"), sep(".*")), sep(";")
			);
		// struct
		seStruct = set("struct", lout(SyntaxElem.NL_MASK_FORSED|SyntaxElem.NL_MASK_DOUBLE,-1,0,0),
			kw("class"), ident("short_name"),
			lst(
				sep("{", lout(SyntaxElem.NL_TRANS,1,1,SyntaxElem.INDENT_KIND_FIXED_SIZE)),
				null,
				attr("members"),
				null,
				null,
				sep("}", lout(SyntaxElem.NL_TRANS,1,1,SyntaxElem.INDENT_KIND_UNINDENT))
				)
			);
		// frmal parameter
		seFormPar = set("form-par", 0,
			ident("vtype"), ident("name")
			);
		// constructor
		seConstructor = set("ctor", lout(SyntaxElem.NL_MASK_FORSED|SyntaxElem.NL_MASK_DOUBLE,-1,0,0),
			ident("parent.short_name"),
			lst(sep("(", lout(0,-1,-1,0)),
				null,attr("params"),null,sep(","),
				sep(")"))
			);
	}
	public SyntaxElem getSyntaxElem(ASTNode node) {
		switch (node) {
		case FileUnit: return seFileUnit;
		case Import: return seImport;
		case Struct: return seStruct;
		case FormPar: return seFormPar;
		case Constructor: return seConstructor;
		}
		return super.getSyntaxElem(node);
	}
}

