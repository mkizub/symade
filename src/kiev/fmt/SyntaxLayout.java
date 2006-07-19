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

public class TextSyntax {
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
	
	private Hashtable<String,SyntaxElem> badSyntax;
	
	public TextSyntax parent_syntax;
	
	public SpaceInfo siSp     = new SpaceInfo("sp",       SP_SPACE,    1, 4);
	public SpaceInfo siSpSEPR = new SpaceInfo("sp-sepr",  SP_SPACE,    1, 4);
	public SpaceInfo siSpWORD = new SpaceInfo("sp-word",  SP_SPACE,    1, 4);
	public SpaceInfo siSpOPER = new SpaceInfo("sp-oper",  SP_SPACE,    1, 4);
	public SpaceInfo siNl     = new SpaceInfo("nl",       SP_NEW_LINE, 1,  1);
	public SpaceInfo siNlGrp  = new SpaceInfo("nl-group", SP_NEW_LINE, 2, 20);
	
	public ParagraphLayout plIndented = new ParagraphLayout("par-indented", 4, 20);
	
	public String escapeString(String str) {
		return str;
	}
	public String escapeChar(char ch) {
		return String.valueOf(ch);
	}
	
	public void loadFrom(FileUnit fu) {}

	public SyntaxElem getSyntaxElem(ANode node, FormatInfoHint hint) {
		if (badSyntax == null)
			badSyntax = new Hashtable<Class,SyntaxElem>();
		String cl_name = node.getClass().getName();
		SyntaxElem se = badSyntax.get(cl_name);
		if (se == null) {
			se = kw("?"+cl_name+"?");
			badSyntax.put(cl_name, se);
		}
		return se;
	}
	
	protected SyntaxParagraphLayout par(ParagraphLayout par, SyntaxElem elem) {
		SyntaxParagraphLayout spl = new SyntaxParagraphLayout(elem, par, new SpaceCmd[0]);
		return spl;
	}
	
	protected SyntaxSet set(SyntaxElem... elems) {
		SpaceCmd[] lout_empty = new SpaceCmd[0];
		SyntaxSet set = new SyntaxSet(lout_empty);
		set.elements.addAll(elems);
		return set;
	}
	
	protected SyntaxSet setl(SpaceCmd[] spaces, SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(spaces);
		set.elements.addAll(elems);
		return set;
	}

	protected SyntaxList lst(
			String name,
			SyntaxElem element,
			SyntaxElem separator,
			SpaceCmd[] spaces
	)
	{
		SyntaxList lst = new SyntaxList(name.intern(), element, separator, spaces);
		return lst;
	}

	protected SyntaxList lst(
			String name,
			SpaceCmd[] spaces
	)
	{
		SyntaxList lst = new SyntaxList(name.intern(), node(), null, spaces);
		return lst;
	}

	protected SyntaxNode node()
	{
		return new SyntaxNode();
	}

	protected SyntaxNode node(SpaceCmd[] lout)
	{
		return new SyntaxNode(lout);
	}

	protected SyntaxNode node(FormatInfoHint hint)
	{
		return new SyntaxNode(hint);
	}

	protected SyntaxAttr attr(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return new SyntaxSubAttr(slot, lout);
	}

	protected SyntaxAttr attr(String slot, SpaceCmd[] lout)
	{
		return new SyntaxSubAttr(slot, lout);
	}

	protected SyntaxAttr attr(String slot, FormatInfoHint hint)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return new SyntaxSubAttr(slot, hint, lout);
	}

	protected SyntaxIdentAttr ident(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpWORD, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxIdentAttr(slot,lout);
	}

	protected SyntaxCharAttr charcter(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpSEPR, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxCharAttr(slot,lout);
	}

	protected SyntaxStrAttr string(String slot)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_ADD, 0),
			};
		return new SyntaxStrAttr(slot,lout);
	}

	protected SyntaxToken kw(String kw)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpSEPR, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpWORD, SP_NOP, SP_ADD, 0),
			};
		return new SyntaxToken(kw,lout);
	}

	protected SyntaxToken sep(String sep)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpWORD, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpSEPR, SP_EAT, SP_ADD, 0),
			};
		return new SyntaxToken(sep,lout);
	}

	protected SyntaxToken sep0(String sep)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return new SyntaxToken(sep,lout);
	}

	protected SyntaxToken sep_nl(String sep)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpWORD, SP_EAT, SP_NOP, 0),
				new SpaceCmd(siSpSEPR, SP_EAT, SP_ADD, 0),
				new SpaceCmd(siNl,     SP_NOP, SP_ADD, 0),
			};
		return new SyntaxToken(sep,lout);
	}

	protected SyntaxToken oper(Operator op)
	{
		return oper(op.toString());
	}

	protected SyntaxToken oper(String op)
	{
		SpaceCmd[] lout = new SpaceCmd[] {
				new SpaceCmd(siSpOPER, SP_ADD, SP_ADD, 0)
			};
		return new SyntaxToken(op.intern(),lout);
	}

	protected SyntaxToken sep(String sep, SpaceCmd[] spaces)
	{
		return new SyntaxToken(sep,spaces);
	}
	
	protected SyntaxOptional opt(String name)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return opt(new CalcOptionNotNull(name),attr(name),null,lout);
	}
	
	protected SyntaxOptional opt(String name, SyntaxElem opt_true)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		return opt(new CalcOptionNotNull(name),opt_true,null,lout);
	}

	protected SyntaxOptional opt(CalcOption calc, SyntaxElem opt_true, SyntaxElem opt_false, SpaceCmd[] spaces)
	{
		return new SyntaxOptional(calc,opt_true,opt_false,spaces);
	}

	protected SyntaxIntChoice alt_int(String name, SyntaxElem... options)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		SyntaxIntChoice sc = new SyntaxIntChoice(name,lout);
		sc.elements.addAll(options);
		return sc;
	}

	protected SyntaxEnumChoice alt_enum(String name, SyntaxElem... options)
	{
		SpaceCmd[] lout = new SpaceCmd[0];
		SyntaxEnumChoice sc = new SyntaxEnumChoice(name,lout);
		sc.elements.addAll(options);
		return sc;
	}

	protected SyntaxFolder folder(boolean folded_by_default, SyntaxElem folded, SyntaxElem unfolded, SpaceCmd[] spaces)
	{
		return new SyntaxFolder(folded_by_default,folded,unfolded,spaces);
	}

}

public enum SpaceKind {
	SP_SPACE,
	SP_NEW_LINE
}

public enum SpaceAction {
	SP_NOP,
	SP_ADD,
	SP_EAT
}

@node
public class SpaceInfo extends DNode {
	@virtual typedef This  = SpaceInfo;

	@att SpaceKind		kind;
	@att int			text_size;
	@att int			pixel_size;
	
	public SpaceInfo() {}
	public SpaceInfo(String name, SpaceKind kind, int text_size, int pixel_size) {
		this.id = new Symbol(name);
		this.kind = kind;
		this.text_size = text_size;
		this.pixel_size = pixel_size;
	}
}

@node
public final class SpaceCmd extends ASTNode {
	@virtual typedef This  = SpaceCmd;

	public static final SpaceCmd[] emptyArray = new SpaceCmd[0];

	@att public SymbolRef<SpaceInfo>		si;
	@att public SpaceAction					action_before;
	@att public SpaceAction					action_after;
	@att public int							from_attempt;
	
	public SpaceCmd() {
		action_before = SP_NOP;
		action_after = SP_NOP;
	}
	public SpaceCmd(SpaceInfo si, SpaceAction action_before, SpaceAction action_after) {
		this(si, action_before, action_after, 0);
	}
	public SpaceCmd(SpaceInfo si, SpaceAction action_before, SpaceAction action_after, int from_attempt) {
		this.si = new SymbolRef<SpaceInfo>(0,si);
		this.from_attempt = from_attempt;
		this.action_before = action_before;
		this.action_after = action_after;
	}

	public void preResolveOut() {
		if (si == null)
			si = new SymbolRef<SpaceInfo>(0,"sp");
		if (si.name == null)
			si.name = "sp";
		SpaceInfo@ spi;
		if (!PassInfo.resolveNameR(this,spi,new ResInfo(this,si.name)))
			Kiev.reportError(this,"Cannot resolve color '"+si.name+"'");
		else if !(spi instanceof SpaceInfo)
			Kiev.reportError(this,"Resolved '"+si.name+"' is not a color");
		else
			si.symbol = spi;
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "si") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SpaceInfo> vect = new Vector<SpaceInfo>();
			DNode@ spi;
			foreach (PassInfo.resolveNameR(this,spi,info))
				if (spi instanceof SpaceInfo) vect.append((SpaceInfo)spi);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

}

@node
public class ParagraphLayout extends ASTNode {
	@virtual typedef This  ≤ ParagraphLayout;

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
		if (t instanceof DrawToken && ((SyntaxToken)t.syntax).text == "{")
			return false;
		return true;
	}
}

@node
public final class DrawColor extends DNode {
	@virtual typedef This  = DrawColor;

	@att
	public int rgb_color;

	
	public DrawColor() {}
	public DrawColor(String name) {
		this.id = new Symbol(name);
	}
}

@node
public final class DrawFont extends DNode {
	@virtual typedef This  = DrawColor;

	@att
	public String font_name;

	public DrawFont() {}
	public DrawFont(String font_name) {
		this.id = new Symbol(font_name);
		this.font_name = font_name;
	}
}

public final class LayoutSpace implements Cloneable {
	static final LayoutSpace[] emptyArray = new LayoutSpace[0];
	public String		name;
	public int			from_attempt;
	public boolean		new_line;
	public boolean		eat;
	public int			text_size;
	public int			pixel_size;
	LayoutSpace setEat() {
		LayoutSpace ls = this.clone();
		ls.eat = true;
		return ls;
	}
}

public final class DrawLayout {

	private static final Font default_font = Font.decode("Dialog-PLAIN-12");
	
	public int				count;
	public boolean			is_hidden;
	public Color 			color;
	public Font				font;
	public LayoutSpace[]	spaces_before;
	public LayoutSpace[]	spaces_after;

	public DrawLayout() {
		this.count = 1;
		this.color = Color.BLACK;
		this.font = default_font;
		this.spaces_before = LayoutSpace.emptyArray;
		this.spaces_after = LayoutSpace.emptyArray;
	}
}

@node
public final class SyntaxElemDecl extends DNode {
	@att SymbolRef<Struct>		node;
	@att SyntaxElem				elem;

	public SyntaxElemDecl() {}
	public SyntaxElemDecl(Struct cls, SyntaxElem elem) {
		this.node = new SymbolRef<Struct>(0,cls);
		this.elem = elem;
	}

	public void preResolveOut() {
		if (node == null)
			node = new SymbolRef<Struct>(0,Env.newStruct("ASTNode",Env.newPackage("kiev.vlang"),0));
		if (node.name == null)
			node.name = "ASTNode";
		Struct@ s;
		if (!PassInfo.resolveNameR(this,s,new ResInfo(this,node.name)))
			Kiev.reportError(this,"Cannot resolve @node '"+node.name+"'");
		else if (!(s instanceof Struct) || !((Struct)s).isCompilerNode())
			Kiev.reportError(this,"Resolved '"+node.name+"' is not @node");
		else
			node.symbol = s;
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "node") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<Struct> vect = new Vector<Struct>();
			Struct@ s;
			foreach (PassInfo.resolveNameR(this,s,info))
				if (s instanceof Struct && ((Struct)s).isCompilerNode()) vect.append((Struct)s);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

}

@node
public abstract class SyntaxElem extends ASTNode {
	@virtual typedef This  ≤ SyntaxElem;

	public static final SyntaxElem[] emptyArray = new SyntaxElem[0];

	@att public SpaceCmd[]				spaces;
	@att public boolean					is_hidden;

	public:r,r,r,rw DrawLayout		lout;
	
	public SyntaxElem() {}
	public SyntaxElem(SpaceCmd[] spaces) {
		this.spaces.copyFrom(spaces);
	}

	public abstract Drawable makeDrawable(Formatter fmt, ANode node);
	
	@getter
	public DrawLayout get$lout() {
		if (lout == null) {
			lout = new DrawLayout();
			compile();
		}
		return lout;
	}
	
	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		if (current_stx != this || expected_node != current_node)
			return false;
		return true;
	}
	
	protected void compile() {
		DrawLayout lout = this.lout;
		lout.is_hidden = is_hidden;
		for (int i=0; i < this.spaces.length; i++) {
			SpaceCmd sc = spaces[i];
			LayoutSpace ls = new LayoutSpace();
			if (sc.si.symbol != null) {
				SpaceInfo si = (SpaceInfo)sc.si.symbol;
				ls.name = si.id.uname;
				if (si.kind == SP_NEW_LINE) ls.new_line = true;
				ls.text_size = si.text_size;
				ls.pixel_size = si.pixel_size;
			} else {
				ls.name = sc.si.name;
				ls.text_size = 1;
				ls.pixel_size = 4;
			}
			ls.from_attempt = sc.from_attempt;
			switch (sc.action_before) {
			case SP_NOP: break;
			case SP_ADD: lout.spaces_before = (LayoutSpace[])Arrays.append(lout.spaces_before, ls); break;
			case SP_EAT: lout.spaces_before = (LayoutSpace[])Arrays.append(lout.spaces_before, ls.setEat()); break;
			}
			switch (sc.action_after) {
			case SP_NOP: break;
			case SP_ADD: lout.spaces_after = (LayoutSpace[])Arrays.append(lout.spaces_after, ls); break;
			case SP_EAT: lout.spaces_after = (LayoutSpace[])Arrays.append(lout.spaces_after, ls.setEat()); break;
			}
			lout.count = Math.max(lout.count, sc.from_attempt+1);
		}
	}

}

@node
public final class SyntaxToken extends SyntaxElem {
	@virtual typedef This  = SyntaxToken;

	@att public String					text;
	@att public SymbolRef<DrawColor>	color;
	@att public SymbolRef<DrawFont>		font;

	public SyntaxToken() {
		this.color = new SymbolRef(0,new DrawColor("default-color"));
		this.font = new SymbolRef(0,new DrawFont("default-font"));
	}
	public SyntaxToken(String text, SpaceCmd[] spaces) {
		super(spaces);
		this.color = new SymbolRef(0,new DrawColor("default-color"));
		this.font = new SymbolRef(0,new DrawFont("default-font"));
		this.text = text.intern();
	}
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawToken(node, this);
		return dr;
	}

	protected void compile() {
		super.compile();
		DrawLayout lout = this.lout;
		if (color != null)
			lout.color = new Color(color.symbol.rgb_color);
		if (font != null)
			lout.font = Font.decode(font.symbol.font_name);
	}

	public void preResolveOut() {
		{
			if (color == null)
				color = new SymbolRef<DrawColor>(0,"default-color");
			if (color.name == null)
				color.name = "default-color";
			DrawColor@ dc;
			if (!PassInfo.resolveNameR(this,dc,new ResInfo(this,color.name)))
				Kiev.reportError(this,"Cannot resolve color '"+color.name+"'");
			else if !(dc instanceof DrawColor)
				Kiev.reportError(this,"Resolved '"+color.name+"' is not a color");
			else
				color.symbol = dc;
		}

		{
			if (font == null)
				font = new SymbolRef<DrawFont>("default-font");
			if (font.name == null)
				font.name = "default-font";
			DrawFont@ df;
			if (!PassInfo.resolveNameR(this,df,new ResInfo(this,font.name)))
				Kiev.reportError(this,"Cannot resolve font '"+font.name+"'");
			else if !(df instanceof DrawFont)
				Kiev.reportError(this,"Resolved '"+font.name+"' is not a font");
			else
				font.symbol = df;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "color") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<DrawColor> vect = new Vector<DrawColor>();
			DNode@ dc;
			foreach (PassInfo.resolveNameR(this,dc,info))
				if (dc instanceof DrawColor) vect.append((DrawColor)dc);
			return vect.toArray();
		}
		if (slot.name == "font") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<DrawFont> vect = new Vector<DrawFont>();
			DNode@ df;
			foreach (PassInfo.resolveNameR(this,df,info))
				if (df instanceof DrawFont) vect.append((DrawFont)df);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

}

@node
public abstract class SyntaxAttr extends SyntaxElem {
	@virtual typedef This  ≤ SyntaxAttr;

	@att public String					name;
	@att public FormatInfoHint			hint;
	@att public SymbolRef[]				expected_types;
	@att public SyntaxToken				format;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public SyntaxAttr() {}
	public SyntaxAttr(String name, SpaceCmd[] spaces) {
		super(spaces);
		this.name = name;
	}
	public SyntaxAttr(String name, FormatInfoHint hint, SpaceCmd[] spaces) {
		super(spaces);
		this.name = name;
		this.hint = hint;
	}

	public abstract Drawable makeDrawable(Formatter fmt, ANode node);

	protected void compile() {
		super.compile();
		if (format != null) {
			DrawLayout lout = this.lout;
			if (format.color != null)
				lout.color = new Color(format.color.symbol.rgb_color);
			if (format.font != null)
				lout.font = Font.decode(format.font.symbol.font_name);
		}
	}

	public void preResolveOut() {
		foreach (SymbolRef sr; expected_types) {
			TypeDecl@ td;
			if( !PassInfo.resolveNameR(this,td,new ResInfo(this,sr.name,ResInfo.noForwards)) ) {
				Kiev.reportError(sr,"Unresolved type "+sr);
				continue;
			}
			if (!(td instanceof Struct) || !((Struct)td).isCompilerNode()) {
				Kiev.reportError(sr,"Resolved type "+sr+" is not a compiler @node");
				continue;
			}
			sr.symbol = td;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "expected_types") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<Struct> vect = new Vector<Struct>();
			DNode@ s;
			foreach (PassInfo.resolveNameR(this,s,info))
				if (s instanceof Struct && ((Struct)s).isCompilerNode()) vect.append((Struct)s);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

}

@node
public class SyntaxSubAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxSubAttr;

	public SyntaxSubAttr() {}
	public SyntaxSubAttr(String name, SpaceCmd[] spaces) {
		super(name,spaces);
	}
	public SyntaxSubAttr(String name, FormatInfoHint hint, SpaceCmd[] spaces) {
		super(name,hint,spaces);
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		if (name.equals("this"))
			return super.check(cont, current_stx, expected_node, current_node);
		Object obj = expected_node.getVal(name);
		if (obj instanceof ANode) {
			SyntaxElem se = cont.fmt.getSyntax().getSyntaxElem((ANode)obj, hint);
			return se.check(cont, current_stx, (ANode)obj, current_node);
		}
		return super.check(cont, current_stx, expected_node, current_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr;
		if (name.equals("this")) {
			dr = new DrawNodeTerm(node, this, "");
		} else {
			Object obj = node.getVal(name);
			if (obj instanceof ANode)
				dr = fmt.getDrawable((ANode)obj, null, hint);
			else
				dr = new DrawNodeTerm(node, this, name);
		}
		dr.attr_syntax = this;
		return dr;
	}
}

@node
public class SyntaxList extends SyntaxAttr {
	@virtual typedef This  = SyntaxList;

	@att public SyntaxElem	element;
	@att public SyntaxElem	separator;
	@att public SyntaxElem	empty;
	@att public CalcOption filter;

	public SyntaxList() {}
	public SyntaxList(String name, SyntaxElem element, SyntaxElem separator, SpaceCmd[] spaces) {
		super(name,spaces);
		this.element = element;
		this.separator = separator;
		this.empty = new SyntaxToken("<?"+name+"?>", new SpaceCmd[0]);
		this.empty.is_hidden = true;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawNonTermList(node, this);
		return dr;
	}
}

@node
public class SyntaxIdentAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxIdentAttr;

	public SyntaxIdentAttr() {}
	public SyntaxIdentAttr(String name, SpaceCmd[] spaces) {
		super(name,spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawNodeTerm(node, this, name);
		return dr;
	}
}

@node
public class SyntaxCharAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxCharAttr;

	public SyntaxCharAttr() {}
	public SyntaxCharAttr(String name, SpaceCmd[] spaces) {
		super(name,spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawCharTerm(node, this, name);
		return dr;
	}
}

@node
public class SyntaxStrAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxStrAttr;

	public SyntaxStrAttr() {}
	public SyntaxStrAttr(String name, SpaceCmd[] spaces) {
		super(name,spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawStrTerm(node, this, name);
		return dr;
	}
}

@node
public class SyntaxSet extends SyntaxElem {
	@virtual typedef This  ≤ SyntaxSet;

	@att public SyntaxElem[] elements;

	public SyntaxSet() {}
	public SyntaxSet(SpaceCmd[] spaces) {
		super(spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawNonTermSet(node, this);
		return dr;
	}
}

@node
public class SyntaxNode extends SyntaxElem {
	@virtual typedef This  = SyntaxNode;

	@att public FormatInfoHint	hint;

	public SyntaxNode() {}
	public SyntaxNode(SpaceCmd[] spaces) {
		super(spaces);
	}
	public SyntaxNode(FormatInfoHint hint) {
		this.hint = hint;
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		SyntaxElem se = cont.fmt.getSyntax().getSyntaxElem(expected_node, hint);
		return se.check(cont, current_stx, expected_node, current_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = fmt.getDrawable(node, null, hint);
		dr.attr_syntax = this;
		return dr;
	}
}

@node
public class SyntaxSpace extends SyntaxElem {
	@virtual typedef This  = SyntaxSpace;

	public SyntaxSpace() {}
	public SyntaxSpace(SpaceCmd[] spaces) {
		super(spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawSpace(node, this);
		return dr;
	}
}

@node
public abstract class CalcOption extends ANode {
	@virtual typedef This  ≤ CalcOption;

	@att public String name;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public CalcOption() {}
	public CalcOption(String name) {
		if (name != null)
			this.name = name.intern();
	}
	
	public abstract boolean calc(ANode node);
}

@node
public final class CalcOptionNotNull extends CalcOption {
	@virtual typedef This  = CalcOptionNotNull;

	public CalcOptionNotNull() {}
	public CalcOptionNotNull(String name) {
		super(name);
	}

	public boolean calc(ANode node) {
		if (node == null)
			return false;
		Object obj = node.getVal(name);
		if (obj == null)
			return false;
		if (obj instanceof ASTNode && obj.isAutoGenerated())
			return false;
		return true;
	}
}

@node
public final class CalcOptionNotEmpty implements CalcOption {
	@virtual typedef This  = CalcOptionNotEmpty;

	public CalcOptionNotEmpty() {}
	public CalcOptionNotEmpty(String name) {
		super(name);
	}

	public boolean calc(ANode node) {
		if (node == null)
			return false;
		Object obj = node.getVal(name);
		if (obj instanceof ANode[])
			return ((ANode[])obj).length > 0;
		return false;
	}
}

@node
public class CalcOptionTrue implements CalcOption {
	@virtual typedef This  = CalcOptionTrue;

	public CalcOptionTrue() {}
	public CalcOptionTrue(String name) {
		super(name);
	}

	public boolean calc(ANode node) {
		if (node == null) return false;
		Object val = node.getVal(name);
		if (val == null || !(val instanceof Boolean)) return false;
		return ((Boolean)val).booleanValue();
	}
}


@node
public class SyntaxOptional extends SyntaxElem {
	@virtual typedef This  = SyntaxOptional;

	@att public CalcOption	calculator;
	@att public SyntaxElem	opt_true;
	@att public SyntaxElem	opt_false;

	public SyntaxOptional() {}
	public SyntaxOptional(CalcOption calculator, SyntaxElem opt_true, SyntaxElem opt_false, SpaceCmd[] spaces) {
		super(spaces);
		this.calculator = calculator;
		this.opt_true = opt_true;
		this.opt_false = opt_false;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawOptional(node, this);
		return dr;
	}
}


@node
public class SyntaxFolder extends SyntaxElem {
	@virtual typedef This  = SyntaxFolder;

	@att public boolean folded_by_default;
	@att public SyntaxElem folded;
	@att public SyntaxElem unfolded;

	public SyntaxFolder() {}
	public SyntaxFolder(boolean folded_by_default, SyntaxElem folded, SyntaxElem unfolded, SpaceCmd[] spaces) {
		super(spaces);
		this.folded_by_default = folded_by_default;
		this.folded = folded;
		this.unfolded = unfolded;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawFolded(node, this);
		return dr;
	}
}


@node
public class SyntaxIntChoice extends SyntaxAttr {
	@virtual typedef This  = SyntaxIntChoice;

	@att public SyntaxElem[] elements;

	public SyntaxIntChoice() {}
	public SyntaxIntChoice(String name, SpaceCmd[] spaces) {
		super(name,spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawIntChoice(node, this);
		return dr;
	}
}

@node
public class SyntaxEnumChoice extends SyntaxAttr {
	@virtual typedef This  = SyntaxEnumChoice;

	@att public SyntaxElem[] elements;

	public SyntaxEnumChoice() {}
	public SyntaxEnumChoice(String name, SpaceCmd[] spaces) {
		super(name,spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawEnumChoice(node, this);
		return dr;
	}
}

@node
public class SyntaxParagraphLayout extends SyntaxElem {
	@virtual typedef This  = SyntaxParagraphLayout;

	@att public SyntaxElem			elem;
	@ref public ParagraphLayout		par;

	public SyntaxParagraphLayout() {}
	public SyntaxParagraphLayout(SyntaxElem elem, ParagraphLayout par, SpaceCmd[] spaces) {
		super(spaces);
		this.elem = elem;
		this.par = par;
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawParagraph(node, this);
		return dr;
	}
}


