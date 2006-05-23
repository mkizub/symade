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

import java.awt.Color;
import java.awt.Font;

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
	
	public SpaceInfo siSp     = new SpaceInfo("sp",       SP_SPACE,    1, 4);
	public SpaceInfo siSpSEPR = new SpaceInfo("sp-sepr",  SP_SPACE,    1, 4);
	public SpaceInfo siSpWORD = new SpaceInfo("sp-word",  SP_SPACE,    1, 4);
	public SpaceInfo siSpOPER = new SpaceInfo("sp-oper",  SP_SPACE,    1, 4);
	public SpaceInfo siNl     = new SpaceInfo("nl",       SP_NEW_LINE, 1,  1);
	public SpaceInfo siNlGrp  = new SpaceInfo("nl-group", SP_NEW_LINE, 2, 20);
	
	public ParagraphLayout plIndented = new ParagraphLayout("par-indented", 4, 20);
	
	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
		return kw("?"+node.getClass().getName()+"?");
	}
	
	protected SyntaxParagraphLayout par(ParagraphLayout par, SyntaxElem elem) {
		SyntaxParagraphLayout spl = new SyntaxParagraphLayout(elem, par, new DrawLayout());
		return spl;
	}
	
	protected SyntaxSet set(SyntaxElem... elems) {
		DrawLayout lout_empty = new DrawLayout();
		SyntaxSet set = new SyntaxSet(lout_empty);
		set.elements.addAll(elems);
		return set;
	}
	
	protected SyntaxSet setl(DrawLayout layout, SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(layout);
		set.elements.addAll(elems);
		return set;
	}

	protected SyntaxList lst(
			String name,
			SyntaxElem element,
			SyntaxElem separator,
			DrawLayout layout
	)
	{
		SyntaxList lst = new SyntaxList(name.intern(), element, separator, layout);
		return lst;
	}

	protected SyntaxList lst(
			String name,
			DrawLayout layout
	)
	{
		SyntaxList lst = new SyntaxList(name.intern(), node(), null, layout);
		return lst;
	}

	protected SyntaxNode node()
	{
		return new SyntaxNode();
	}

	protected SyntaxNode node(FormatInfoHint hint)
	{
		return new SyntaxNode(hint);
	}

	protected SyntaxAttr attr(String slot)
	{
		DrawLayout lout = new DrawLayout();
		return new SyntaxAttr(slot, lout);
	}

	protected SyntaxAttr attr(String slot, FormatInfoHint hint)
	{
		DrawLayout lout = new DrawLayout();
		return new SyntaxAttr(slot, hint, lout);
	}

	protected SyntaxIdentAttr ident(String slot)
	{
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpSEPR, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpWORD, SP_ADD_AFTER, 0),
			});
		return new SyntaxIdentAttr(slot,lout);
	}

	protected SyntaxCharAttr charcter(String slot)
	{
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpSEPR, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpSEPR, SP_ADD_AFTER, 0),
			});
		return new SyntaxCharAttr(slot,lout);
	}

	protected SyntaxStrAttr string(String slot)
	{
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpSEPR, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpSEPR, SP_ADD_AFTER, 0),
			});
		return new SyntaxStrAttr(slot,lout);
	}

	protected SyntaxKeyword kw(String kw)
	{
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpSEPR, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpWORD, SP_ADD_AFTER, 0),
			});
		return new SyntaxKeyword(kw,lout);
	}

	protected SyntaxSeparator sep(String sep)
	{
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpWORD, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpSEPR, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpSEPR, SP_ADD_AFTER, 0),
			});
		return new SyntaxSeparator(sep,lout);
	}

	protected SyntaxSeparator sep0(String sep)
	{
		DrawLayout lout = new DrawLayout();
		return new SyntaxSeparator(sep,lout);
	}

	protected SyntaxSeparator sep_nl(String sep)
	{
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpWORD, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpSEPR, SP_EAT_BEFORE, 0),
				new SpaceCmd(siSpSEPR, SP_ADD_AFTER, 0),
				new SpaceCmd(siNl,     SP_ADD_AFTER, 0),
			});
		return new SyntaxSeparator(sep,lout);
	}

	protected SyntaxOperator oper(Operator op)
	{
		return oper(op.toString());
	}

	protected SyntaxOperator oper(String op)
	{
		DrawLayout lout = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siSpOPER, SP_ADD_BEFORE, 0),
				new SpaceCmd(siSpOPER, SP_ADD_AFTER, 0),
			});
		return new SyntaxOperator(op.intern(),lout);
	}

	protected SyntaxSeparator sep(String sep, DrawLayout layout)
	{
		return new SyntaxSeparator(sep,layout);
	}
	
	protected SyntaxOptional opt(String name)
	{
		DrawLayout lout = new DrawLayout();
		return opt(name,new CalcOptionNotNull(name),attr(name),null,lout);
	}
	
	protected SyntaxOptional opt(String name, SyntaxElem opt_true)
	{
		DrawLayout lout = new DrawLayout();
		return opt(name,new CalcOptionNotNull(name),opt_true,null,lout);
	}

	protected SyntaxOptional opt(String name, CalcOption calc, SyntaxElem opt_true, SyntaxElem opt_false, DrawLayout lout)
	{
		return new SyntaxOptional(name,calc,opt_true,opt_false,lout);
	}

	protected SyntaxIntChoice alt_int(String name, SyntaxElem... options)
	{
		DrawLayout lout = new DrawLayout();
		SyntaxIntChoice sc = new SyntaxIntChoice(name,lout);
		sc.elements.addAll(options);
		return sc;
	}

	protected SyntaxEnumChoice alt_enum(String name, SyntaxElem... options)
	{
		DrawLayout lout = new DrawLayout();
		SyntaxEnumChoice sc = new SyntaxEnumChoice(name,lout);
		sc.elements.addAll(options);
		return sc;
	}

}

public enum SpaceKind {
	SP_SPACE,
	SP_NEW_LINE
}

public enum SpaceAction {
	SP_ADD_BEFORE,
	SP_EAT_BEFORE,
	SP_ADD_AFTER,
	SP_EAT_AFTER
}

@node
public class SpaceInfo extends ASTNode {
	@virtual typedef This  = SpaceInfo;

	@att KString		name;
	@att SpaceKind		kind;
	@att int			text_size;
	@att int			pixel_size;
	@att int			from_attempt;
	
	public SpaceInfo() {}
	public SpaceInfo(String name, SpaceKind kind, int text_size, int pixel_size) {
		this.name = KString.from(name);
		this.kind = kind;
		this.text_size = text_size;
		this.pixel_size = pixel_size;
	}
}

@node
public final class SpaceCmd extends ASTNode {
	@virtual typedef This  = SpaceCmd;

	public static final SpaceCmd[] emptyArray = new SpaceCmd[0];

	private final int	idx;
	@ref SpaceInfo			si;
	@att final boolean		before;
	@att final boolean		eat;
	@att final int			from_attempt;
	
	public SpaceCmd() {}
	public SpaceCmd(SpaceInfo si, SpaceAction action) {
		this(si, action==SP_ADD_BEFORE||action==SP_EAT_BEFORE, action==SP_EAT_BEFORE||action==SP_EAT_AFTER, 0);
	}
	public SpaceCmd(SpaceInfo si, SpaceAction action, int from_attempt) {
		this(si, action==SP_ADD_BEFORE||action==SP_EAT_BEFORE, action==SP_EAT_BEFORE||action==SP_EAT_AFTER, from_attempt);
	}
	public SpaceCmd(SpaceInfo si, boolean before, boolean eat) {
		this(si, before, eat, 0);
	}
	public SpaceCmd(SpaceInfo si, boolean before, boolean eat, int from_attempt) {
		this.si = si;
		this.before = before;
		this.eat = eat;
		this.from_attempt = from_attempt;
	}
	public int getIdx() { return idx; }
}

@node
public class ParagraphLayout extends ASTNode {
	@virtual typedef This  = ParagraphLayout;

	@att KString name;
	@att int indent_text_size;
	@att int indent_pixel_size;
	@att int indent_first_line_text_size;
	@att int indent_first_line_pixel_size;
	@att boolean indent_from_current_position;
	@att boolean align_right;
	@att boolean align_rest_of_lines_right;
	
	public ParagraphLayout() {}
	public ParagraphLayout(String name, int ind_txt, int ind_pix) {
		this.name = KString.from("name");
		this.indent_text_size = ind_txt;
		this.indent_pixel_size = ind_pix;
	}
	
	public boolean enabled(DrawParagraph dr) { return true; }
}

@node
public class ParagraphLayoutBlock extends ParagraphLayout {
	@virtual typedef This  = ParagraphLayoutBlock;

	public ParagraphLayoutBlock() {}
	public ParagraphLayoutBlock(String name, int ind_txt, int ind_pix) {
		super(name, ind_txt, ind_pix);
	}
	
	public boolean enabled(DrawParagraph dr) {
		if (dr == null)
			return true;
		DrawTerm t = dr.getFirstLeaf();
		if (t instanceof DrawSeparator && ((SyntaxSeparator)t.syntax).text == "{")
			return false;
		return true;
	}
}

@node
public final class DrawColor extends ASTNode {
	public Color native_color;
	
	public DrawColor() {}
}

@node
public final class DrawFont extends ASTNode {
	public Font native_font;
	
	public DrawFont() {}
}

@node
public final class DrawFormat extends ASTNode {
	@ref public DrawColor	color;
	@ref public DrawFont	font;
	
	public DrawFormat() {}
	public DrawFormat(DrawColor color, DrawFont font) {
		this.color = color;
		this.font = font;
	}
}

@node
public final class DrawLayout extends ASTNode {
	@virtual typedef This  = DrawLayout;

	@att int				count;
	@att SpaceCmd[]			spaces;
	
	public DrawLayout() { this.count = 1; }
	public DrawLayout(SpaceCmd[] spaces) {
		this.spaces.addAll(spaces);
		int count = 1;
		for (int i=0; i < spaces.length; i++)
			count = Math.max(count, spaces[i].from_attempt+1);
		this.count = count;
	}
}

@node
public abstract class SyntaxElem extends ASTNode {
	@virtual typedef This  = SyntaxElem;

	public static final SyntaxElem[] emptyArray = new SyntaxElem[0];

	@att public DrawLayout			layout;
	@att public DrawFormat			fmt;
	@att public boolean				is_hidden;
	
	public SyntaxElem() {}
	public SyntaxElem(DrawLayout layout) {
		this.layout = layout;
		this.fmt = new DrawFormat(new DrawColor(), new DrawFont());
	}

	public abstract Drawable makeDrawable(Formatter fmt, ASTNode node);

}

@node
public class SyntaxSpace extends SyntaxElem {
	@virtual typedef This  = SyntaxSpace;

	public SyntaxSpace() {}
	public SyntaxSpace(DrawLayout layout) {
		super(layout);
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
	public SyntaxToken(String text, DrawLayout layout) {
		super(layout);
		this.text = text.intern();
	}
}

@node
public class SyntaxKeyword extends SyntaxToken {
	@virtual typedef This  = SyntaxKeyword;

	public SyntaxKeyword() {}
	public SyntaxKeyword(String text, DrawLayout layout) {
		super(text, layout);
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
	public SyntaxOperator(String text, DrawLayout layout) {
		super(text, layout);
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
	public SyntaxSeparator(String text, DrawLayout layout) {
		super(text, layout);
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

	@att public String		name;
	@att public SyntaxElem element;
	@att public SyntaxElem separator;
	     public CalcOption filter;

	public SyntaxList() {}
	public SyntaxList(String name, SyntaxElem element, SyntaxElem separator, DrawLayout layout) {
		super(layout);
		this.name = name.intern();
		this.element = element;
		this.separator = separator;
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

	@att public SyntaxElem[] elements;

	public SyntaxSet() {}
	public SyntaxSet(DrawLayout layout) {
		super(layout);
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
	public SyntaxAttr(String name, DrawLayout layout) {
		super(layout);
		this.name = name.intern();
	}
	public SyntaxAttr(String name, FormatInfoHint hint, DrawLayout layout) {
		super(layout);
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
public class SyntaxNode extends SyntaxElem {
	@virtual typedef This  = SyntaxNode;

	@att public FormatInfoHint	hint;

	public SyntaxNode() {}
	public SyntaxNode(FormatInfoHint hint) {
		super(new DrawLayout());
		this.hint = hint;
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		return fmt.getDrawable(node, hint);
	}
}

@node
public class SyntaxIdentAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxIdentAttr;

	public SyntaxIdentAttr() {}
	public SyntaxIdentAttr(String name, DrawLayout layout) {
		super(name,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawNodeTerm(node, this, name);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxCharAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxCharAttr;

	public SyntaxCharAttr() {}
	public SyntaxCharAttr(String name, DrawLayout layout) {
		super(name,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawCharTerm(node, this, name);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxStrAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxStrAttr;

	public SyntaxStrAttr() {}
	public SyntaxStrAttr(String name, DrawLayout layout) {
		super(name,layout);
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawStrTerm(node, this, name);
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
		if (node == null)
			return false;
		Object obj = node.getVal(name);
		if (obj == null)
			return false;
		if (obj instanceof ASTNode && obj.isHidden())
			return false;
		return true;
	}
}

public class CalcOptionNotEmpty implements CalcOption {
	private final String name;
	public CalcOptionNotEmpty(String name) { this.name = name.intern(); } 
	public boolean calc(ASTNode node) {
		if (node == null)
			return false;
		Object obj = node.getVal(name);
		if (obj instanceof ASTNode[])
			return obj.length > 0;
		return false;
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
	public SyntaxOptional(String name, CalcOption calculator, SyntaxElem opt_true, SyntaxElem opt_false, DrawLayout layout) {
		super(layout);
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
	@virtual typedef This  = SyntaxIntChoice;

	@att public String name;

	public SyntaxIntChoice() {}
	public SyntaxIntChoice(String name, DrawLayout layout) {
		super(layout);
		this.name = name.intern();
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawIntChoice(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxEnumChoice extends SyntaxSet {
	@virtual typedef This  = SyntaxEnumChoice;

	@att public String name;

	public SyntaxEnumChoice() {}
	public SyntaxEnumChoice(String name, DrawLayout layout) {
		super(layout);
		this.name = name.intern();
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawEnumChoice(node, this);
		dr.init(fmt);
		return dr;
	}
}

@node
public class SyntaxParagraphLayout extends SyntaxElem {
	@virtual typedef This  = SyntaxParagraphLayout;

	@att public SyntaxElem			elem;
	@ref public ParagraphLayout		par;

	public SyntaxParagraphLayout() {}
	public SyntaxParagraphLayout(SyntaxElem elem, ParagraphLayout par, DrawLayout layout) {
		super(layout);
		this.elem = elem;
		this.par = par;
	}

	public Drawable makeDrawable(Formatter fmt, ASTNode node) {
		Drawable dr = new DrawParagraph(node, this);
		dr.init(fmt);
		return dr;
	}
}


