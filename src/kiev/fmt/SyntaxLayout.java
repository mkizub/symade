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
	
	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
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
		return new SyntaxAttr(this, slot, lout);
	}

	protected final SyntaxAttr attr(String slot, FormatInfoHint hint)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return new SyntaxAttr(this, slot, hint, lout);
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

	protected final SyntaxOperator oper(Operator op)
	{
		return oper(op.toString());
	}

	protected final SyntaxOperator oper(String op)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{
				new SpaceInfo("word", SP_ADD_BEFORE, 1, 10),
				new SpaceInfo("word", SP_ADD_AFTER, 1, 10),
				new SpaceInfo("sep", SP_ADD_BEFORE, 1, 10),
				new SpaceInfo("sep", SP_ADD_AFTER, 1, 10),
				new SpaceInfo("oper", SP_ADD_BEFORE, 1, 10),
				new SpaceInfo("oper", SP_ADD_AFTER, 1, 10),
			}
		);
		return new SyntaxOperator(this,op.intern(),lout);
	}

	protected final SyntaxSeparator sep(String sep, DrawLayout layout)
	{
		return new SyntaxSeparator(this,sep,layout);
	}
	
	protected final SyntaxOptional opt(String name)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return opt(name,new CalcOptionNotNull(name),attr(name),null,lout);
	}
	
	protected final SyntaxOptional opt(String name, SyntaxElem opt_true)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		return opt(name,new CalcOptionNotNull(name),opt_true,null,lout);
	}

	protected final SyntaxOptional opt(String name, CalcOption calc, SyntaxElem opt_true, SyntaxElem opt_false, DrawLayout lout)
	{
		return new SyntaxOptional(this,name,calc,opt_true,opt_false,lout);
	}

	protected final SyntaxIntChoice alt_int(String name, SyntaxElem... options)
	{
		DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
			new NewLineInfo[]{},
			new SpaceInfo[]{}
		);
		SyntaxIntChoice sc = new SyntaxIntChoice(this,name,lout);
		sc.elements.addAll(options);
		return sc;
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

	@att public String			name;
	@att public FormatInfoHint	hint;

	public SyntaxAttr() {}
	public SyntaxAttr(Syntax stx, String name, DrawLayout layout) {
		super(stx,name,layout);
		this.name = name.intern();
	}
	public SyntaxAttr(Syntax stx, String name, FormatInfoHint hint, DrawLayout layout) {
		super(stx,name,layout);
		this.name = name.intern();
		this.hint = hint;
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		if (name.equals("this"))
			return new DrawNodeTerm(node, this, "");
		Object obj = node.getVal(name);
		if (obj instanceof ASTNode)
			return fmt.getDrawable((ASTNode)obj, hint);
		Drawable dr = new DrawNodeTerm(node, this, name);
		dr.init(fmt);
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

public interface CalcOption {
	public boolean calc(ASTNode node);
}

public class CalcOptionNotNull implements CalcOption {
	private final String name;
	public CalcOptionNotNull(String name) { this.name = name.intern(); } 
	public boolean calc(ASTNode node) {
		return node != null && node.getVal(name) != null;
	}
}

public class CalcOptionTrue implements CalcOption {
	private final String name;
	public CalcOptionTrue(String name) { this.name = name.intern(); } 
	public boolean calc(ASTNode node) {
		if (node == null) return false;
		Object val = node.getVal(name);
		if (val == null || !(val instanceof Boolean)) return false;
		return ((Boolean)val).booleanValue();
	}
}


@node
public class SyntaxOptional extends SyntaxElem {
	@virtual typedef This  = SyntaxOptional;

	@ref public CalcOption calculator;
	@att public SyntaxElem opt_true;
	@att public SyntaxElem opt_false;
	@att public String name;

	public SyntaxOptional() {}
	public SyntaxOptional(Syntax stx, String name, CalcOption calculator, SyntaxElem opt_true, SyntaxElem opt_false, DrawLayout layout) {
		super(stx,name,layout);
		this.calculator = calculator;
		this.opt_true = opt_true;
		this.opt_false = opt_false;
		this.name = name.intern();
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawOptional(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxIntChoice extends SyntaxSet {
	@virtual typedef This  = SyntaxOptional;

	public SyntaxIntChoice() {}
	public SyntaxIntChoice(Syntax stx, String name, DrawLayout layout) {
		super(stx,name,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawIntChoice(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxMultipleChoice extends SyntaxSet {
	@virtual typedef This  = SyntaxMultipleChoice;

	@att public String name;

	public SyntaxMultipleChoice() {}
	public SyntaxMultipleChoice(Syntax stx, String name, DrawLayout layout) {
		super(stx,name,layout);
		this.name = name.intern();
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawMultipleChoice(node, this);
		dr.init(fmt);
		return dr;
	}
}


