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

@node
public class TextSyntax extends DNode implements ScopeOfNames {
	
	protected Hashtable<String,SyntaxElem>		badSyntax = new Hashtable<Class,SyntaxElem>();
	protected Hashtable<String,SyntaxElemDecl>	allSyntax = new Hashtable<String,SyntaxElemDecl>();
	
	@att public SymbolRef<TextSyntax>	parent_syntax;
	@att public ASTNode[]				members;
	
	public TextSyntax() {
		id.sname = "<text-syntax>";
		parent_syntax = new SymbolRef<TextSyntax>();
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		ASTNode@ syn;
	{
		path.checkNodeName(this),
		node ?= this
	;
		node @= members,
		node instanceof DNode && path.checkNodeName(node)
	}

	public boolean preResolveIn() {
		if (parent_syntax.name != null && parent_syntax.name != "") {
			TextSyntax@ ts;
			if (!PassInfo.resolveNameR(this,ts,new ResInfo(this,parent_syntax.name)))
				Kiev.reportError(this,"Cannot resolve syntax '"+parent_syntax.name+"'");
			else if !(ts instanceof TextSyntax)
				Kiev.reportError(this,"Resolved '"+parent_syntax.name+"' is not a syntax");
			else
				parent_syntax.symbol = ts;
		}
		return true;
	}
	
	public void mainResolveOut() {
		foreach(SyntaxElemDecl sed; this.members; sed.elem != null) {
			if !(sed.node.symbol instanceof Struct)
				continue;
			if (sed.elem == null)
				continue;
			Struct s = (Struct)sed.node.symbol;
			if !(s.isCompilerNode())
				continue;
			allSyntax.put(s.qname(), sed);
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "parent_syntax") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<TextSyntax> vect = new Vector<TextSyntax>();
			DNode@ ts;
			foreach (PassInfo.resolveNameR(this,ts,info))
				if (ts instanceof TextSyntax) vect.append((TextSyntax)ts);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}


	public String escapeString(String str) {
		return str;
	}
	public String escapeChar(char ch) {
		return String.valueOf(ch);
	}
	
	public SyntaxElem getSyntaxElem(ANode node) {
		if (node != null) {
			String cl_name = node.getClass().getName();
			SyntaxElemDecl sed = allSyntax.get(cl_name);
			if (sed != null)
				return sed.elem;
		}
		if (parent_syntax.symbol != null)
			return ((TextSyntax)parent_syntax.symbol).getSyntaxElem(node);
		if (parent() instanceof TextSyntax)
			return ((TextSyntax)parent()).getSyntaxElem(node);
		if (badSyntax == null)
			badSyntax = new Hashtable<Class,SyntaxElem>();
		String cl_name = node.getClass().getName();
		SyntaxElem se = badSyntax.get(cl_name);
		if (se == null) {
			se = new SyntaxToken("(?"+cl_name+"?)",new SpaceCmd[0]);
			badSyntax.put(cl_name, se);
		}
		return se;
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
		this.id = name;
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
		si = new SymbolRef<SpaceInfo>();
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
public class ParagraphLayout extends DNode {
	@virtual typedef This  ≤ ParagraphLayout;

	@att int indent_text_size;
	@att int indent_pixel_size;
	@att int indent_first_line_text_size;
	@att int indent_first_line_pixel_size;
	@att boolean indent_from_current_position;
	@att boolean align_right;
	@att boolean align_rest_of_lines_right;
	
	public ParagraphLayout() {}
	public ParagraphLayout(String name, int ind_txt, int ind_pix) {
		this.id.sname = name;
		this.indent_text_size = ind_txt;
		this.indent_pixel_size = ind_pix;
	}
	
	public boolean enabled(DrawParagraph dr) { return true; }
}

@node
public class ParagraphLayoutBlock extends ParagraphLayout {
	@virtual typedef This  = ParagraphLayoutBlock;

	@att String token_text;
	private String[] tokens;

	@setter
	public void set$token_text(String value) {
		if (value == null) {
			this.token_text = null;
			this.tokens = new String[0];
		} else {
			this.token_text = value.intern();
			this.tokens = value.split("\\s+");
			for (int i=0; i < this.tokens.length; i++)
				this.tokens[i] = this.tokens[i].intern();
		}
	}
	
	public ParagraphLayoutBlock() {}
	public ParagraphLayoutBlock(String name, int ind_txt, int ind_pix) {
		super(name, ind_txt, ind_pix);
		token_text = "{";
	}
	
	public boolean enabled(DrawParagraph dr) {
		if (dr == null)
			return true;
		DrawTerm t = dr.getFirstLeaf();
		if (t instanceof DrawToken) {
			String str = ((SyntaxToken)t.syntax).text;
			foreach (String s; this.tokens; s == str)
				return false;
		}
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
		this.id = name;
	}
}

@node
public final class DrawFont extends DNode {
	@virtual typedef This  = DrawColor;

	@att
	public String font_name;

	public DrawFont() {}
	public DrawFont(String font_name) {
		this.id = font_name;
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
public abstract class AbstractSyntaxElemDecl extends DNode {
	@virtual typedef This  ≤ AbstractSyntaxElemDecl;

	@att SyntaxElem				elem;

	public AbstractSyntaxElemDecl() {}
	public AbstractSyntaxElemDecl(SyntaxElem elem) {
		this.elem = elem;
	}
}

@node
public class SyntaxElemDecl extends AbstractSyntaxElemDecl {
	@virtual typedef This  = SyntaxElemDecl;

	@att SymbolRef<Struct>		node;

	public SyntaxElemDecl() {
		this.node = new SymbolRef<Struct>();
	}
	public SyntaxElemDecl(Struct cls, SyntaxElem elem) {
		super(elem);
		this.node = new SymbolRef<Struct>(0,cls);
	}

	public void preResolveOut() {
		if (node == null)
			node = new SymbolRef<Struct>(0,Env.newStruct("ASTNode", Env.newPackage("kiev.vlang"), 0));
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
public final class SyntaxElemFormatDecl extends DNode {
	@virtual typedef This  = SyntaxElemFormatDecl;

	@att public SpaceCmd[]				spaces;
	@att public SymbolRef<DrawColor>	color;
	@att public SymbolRef<DrawFont>		font;
	
	public SyntaxElemFormatDecl() {
		this.color = new SymbolRef<DrawColor>(0,new DrawColor("default-color"));
		this.font = new SymbolRef<DrawFont>(0,new DrawFont("default-font"));
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
public abstract class DrawElemFormat extends ASTNode {
	@virtual typedef This  ≤ DrawElemFormat;

	@att public boolean					is_hidden;

	public DrawElemFormat() {}
	
	public abstract SpaceCmd[] getSpaces();
	public abstract DrawColor  getColor();
	public abstract DrawFont   getFont();
}

@node
public final class SimpleElemFormat extends DrawElemFormat {
	@virtual typedef This  = DrawElemFormat;

	@att public SpaceCmd[]				spaces;

	public SimpleElemFormat() {}
	public SimpleElemFormat(SpaceCmd[] spaces) {
		this.spaces.copyFrom(spaces);
	}
	
	public SpaceCmd[] getSpaces() { return spaces; }
	public DrawColor  getColor() { return null; }
	public DrawFont   getFont() { return null; }
}

@node
public final class FullElemFormat extends DrawElemFormat {
	@virtual typedef This  = DrawElemFormat;

	@att public SpaceCmd[]				spaces;
	@att public SymbolRef<DrawColor>	color;
	@att public SymbolRef<DrawFont>		font;

	public FullElemFormat() {
		this.color = new SymbolRef<DrawColor>(0,new DrawColor("default-color"));
		this.font = new SymbolRef<DrawFont>(0,new DrawFont("default-font"));
	}
	public FullElemFormat(SpaceCmd[] spaces) {
		this.spaces.copyFrom(spaces);
		this.color = new SymbolRef<DrawColor>(0,new DrawColor("default-color"));
		this.font = new SymbolRef<DrawFont>(0,new DrawFont("default-font"));
	}
	
	public SpaceCmd[] getSpaces() { return spaces; }
	public DrawColor  getColor() { return color.symbol; }
	public DrawFont   getFont() { return font.symbol; }

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
public final class RefElemFormat extends DrawElemFormat {
	@virtual typedef This  = RefElemFormat;

	@att public SymbolRef<SyntaxElemFormatDecl>	decl;

	public RefElemFormat() {
		decl = new SymbolRef<SyntaxElemFormatDecl>();
	}
	public RefElemFormat(SyntaxElemFormatDecl decl) {
		decl = new SymbolRef<SyntaxElemFormatDecl>(0, decl);
	}
	
	public SpaceCmd[] getSpaces() { return decl.symbol == null ? SpaceCmd.emptyArray : decl.symbol.spaces; }
	public DrawColor  getColor() { return decl.symbol == null ? null : decl.symbol.color.symbol; }
	public DrawFont   getFont() { return decl.symbol == null ? null : decl.symbol.font.symbol; }

	public void preResolveOut() {
		if (decl.name == null) {
			Kiev.reportError(this,"Unspecified format declaration");
			return;
		}
		SyntaxElemFormatDecl@ d;
		if (!PassInfo.resolveNameR(this,d,new ResInfo(this,decl.name)))
			Kiev.reportError(this,"Cannot resolve format declaration '"+decl.name+"'");
		else if !(d instanceof SyntaxElemFormatDecl)
			Kiev.reportError(this,"Resolved '"+decl.name+"' is not a format declaration");
		else
			decl.symbol = d;
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "decl") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxElemFormatDecl> vect = new Vector<SyntaxElemFormatDecl>();
			DNode@ dc;
			foreach (PassInfo.resolveNameR(this,dc,info))
				if (dc instanceof SyntaxElemFormatDecl) vect.append((SyntaxElemFormatDecl)dc);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

}

@node
public abstract class SyntaxElem extends ASTNode {
	@virtual typedef This  ≤ SyntaxElem;

	public static final SyntaxElem[] emptyArray = new SyntaxElem[0];

	@att public DrawElemFormat		fmt;

	public:r,r,r,rw DrawLayout		lout;
	
	public SyntaxElem() {
		this.fmt = new SimpleElemFormat();
	}
	public SyntaxElem(SpaceCmd[] spaces) {
		this.fmt = new SimpleElemFormat(spaces);
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
	
	private void compile() {
		DrawLayout lout = this.lout;
		lout.count = 1;
		if (fmt == null)
			return;
		lout.is_hidden = fmt.is_hidden;
		SpaceCmd[] spaces = fmt.getSpaces();
		for (int i=0; i < spaces.length; i++) {
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
		if (fmt.getColor() != null)
			lout.color = new Color(fmt.getColor().rgb_color);
		if (fmt.getFont() != null)
			lout.font = Font.decode(fmt.getFont().font_name);
	}
}

@node
public final class SyntaxElemRef extends SyntaxElem {
	@virtual typedef This  = SyntaxToken;

	@att public SymbolRef<SyntaxElemDecl>		decl;

	public SyntaxElemRef() {
		this.decl = new SymbolRef<SyntaxElemDecl>();
	}
	public SyntaxElemRef(SyntaxElemDecl decl) {
		this.decl = new SymbolRef<SyntaxElemDecl>(0,decl);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		return ((SyntaxElemDecl)decl.symbol).elem.makeDrawable(fmt,node);
	}

	public void preResolveOut() {
		if (decl.name != null && decl.name != "") {
			DNode@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,decl.name,ResInfo.noForwards)))
				Kiev.reportError(decl,"Unresolved syntax element decl "+decl);
			else if !(d instanceof SyntaxElemDecl)
				Kiev.reportError(decl,"Resolved "+decl+" is not a syntax element decl");
			else
				decl.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "decl") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxElemDecl> vect = new Vector<SyntaxElemDecl>();
			DNode@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (d instanceof SyntaxElemDecl) vect.append((SyntaxElemDecl)d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@node
public final class SyntaxToken extends SyntaxElem {
	@virtual typedef This  = SyntaxToken;

	@att public String					text;

	@setter
	public void set$text(String value) {
		this.text = (value != null) ? value.intern() : null;
	}
	
	public SyntaxToken() {}
	public SyntaxToken(String text, SpaceCmd[] spaces) {
		super(spaces);
		this.text = text;
	}
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawToken(node, this);
		return dr;
	}
}

@node
public abstract class SyntaxAttr extends SyntaxElem {
	@virtual typedef This  ≤ SyntaxAttr;

	@att public String					name;
	@att public SymbolRef<TextSyntax>	in_syntax;
	@att public SymbolRef[]				expected_types;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public SyntaxAttr() {
		this.in_syntax = new SymbolRef<TextSyntax>();
	}
	public SyntaxAttr(String name, SpaceCmd[] spaces) {
		super(spaces);
		this.name = name;
		this.in_syntax = new SymbolRef<TextSyntax>();
	}
	public SyntaxAttr(String name, TextSyntax stx, SpaceCmd[] spaces) {
		super(spaces);
		this.name = name;
		this.in_syntax = new SymbolRef<TextSyntax>(0,stx);
	}

	public abstract Drawable makeDrawable(Formatter fmt, ANode node);

	public void preResolveOut() {
		foreach (SymbolRef sr; expected_types; sr.name != null) {
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
		if (in_syntax.name != null && in_syntax.name != "") {
			DNode@ s;
			if (!PassInfo.resolveNameR(this,s,new ResInfo(this,in_syntax.name,ResInfo.noForwards)))
				Kiev.reportError(in_syntax,"Unresolved syntax "+in_syntax);
			else if !(s instanceof TextSyntax)
				Kiev.reportError(in_syntax,"Resolved type "+in_syntax+" is not a text syntax");
			else
				in_syntax.symbol = s;
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
		if (slot.name == "in_syntax") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<TextSyntax> vect = new Vector<TextSyntax>();
			DNode@ s;
			foreach (PassInfo.resolveNameR(this,s,info))
				if (s instanceof TextSyntax) vect.append((TextSyntax)s);
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
	public SyntaxSubAttr(String name, TextSyntax stx, SpaceCmd[] spaces) {
		super(name,stx,spaces);
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		Object obj;
		try {
			obj = expected_node.getVal(name);
		} catch (RuntimeException e) {
			obj = "";
		}
		if (obj instanceof ANode) {
			SyntaxElem se;
			if (in_syntax.symbol != null)
				se = ((TextSyntax)in_syntax.symbol).getSyntaxElem((ANode)obj);
			else
				se = cont.fmt.getSyntax().getSyntaxElem((ANode)obj);
			return se.check(cont, current_stx, (ANode)obj, current_node);
		}
		return super.check(cont, current_stx, expected_node, current_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr;
		Object obj;
		try {
			obj = node.getVal(name);
		} catch (RuntimeException e) {
			obj = "<?error:"+name+"?>";
		}
		if (obj instanceof ANode)
			dr = fmt.getDrawable((ANode)obj, null, (TextSyntax)in_syntax.symbol);
		else
			dr = new DrawNodeTerm(node, this, name);
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
		this.empty.fmt.is_hidden = true;
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
	public SyntaxSet(SpaceCmd[] spaces, SyntaxElem[] elems) {
		super(spaces);
		elements.addAll(elems);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawNonTermSet(node, this);
		return dr;
	}
}

@node
public class SyntaxNode extends SyntaxAttr {
	@virtual typedef This  = SyntaxNode;


	public SyntaxNode() {}
	public SyntaxNode(SpaceCmd[] spaces) {
		super("",spaces);
	}
	public SyntaxNode(TextSyntax stx) {
		super("",new SpaceCmd[0]);
		this.in_syntax.name = stx.id.sname;
		this.in_syntax.symbol = stx;
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		SyntaxElem se;
		if (in_syntax.symbol != null)
			se = ((TextSyntax)in_syntax.symbol).getSyntaxElem(expected_node);
		else
			se = cont.fmt.getSyntax().getSyntaxElem(expected_node);
		return se.check(cont, current_stx, expected_node, current_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = fmt.getDrawable(node, null, (TextSyntax)in_syntax.symbol);
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

	@att public SyntaxElem						elem;
	@att public SymbolRef<ParagraphLayout>		par;

	public SyntaxParagraphLayout() {
		par = new SymbolRef<ParagraphLayout>();
	}
	public SyntaxParagraphLayout(SyntaxElem elem, ParagraphLayout par, SpaceCmd[] spaces) {
		super(spaces);
		this.elem = elem;
		this.par = new SymbolRef<ParagraphLayout>(0,par);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawParagraph(node, this);
		return dr;
	}

	public void preResolveOut() {
		if (par.name == null) {
			Kiev.reportError(this,"Unspecified paragraph declaration");
			return;
		}
		ParagraphLayout@ d;
		if (!PassInfo.resolveNameR(this,d,new ResInfo(this,par.name)))
			Kiev.reportError(this,"Cannot resolve paragraph declaration '"+par.name+"'");
		else if !(d instanceof ParagraphLayout)
			Kiev.reportError(this,"Resolved '"+par.name+"' is not a paragraph declaration");
		else
			par.symbol = d;
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "par") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<ParagraphLayout> vect = new Vector<ParagraphLayout>();
			DNode@ dc;
			foreach (PassInfo.resolveNameR(this,dc,info))
				if (dc instanceof ParagraphLayout) vect.append((ParagraphLayout)dc);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

}


