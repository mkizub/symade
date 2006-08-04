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
public class SyntaxXmlStrAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxXmlStrAttr;

	public SyntaxXmlStrAttr() {}
	public SyntaxXmlStrAttr(String name, SpaceCmd[] spaces) {
		super(name,spaces);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawXmlStrTerm(node, this, name);
		return dr;
	}
}

@node
public class XmlDumpSyntax extends TextSyntax {

	private final SpaceInfo siNl = new SpaceInfo("nl", SP_NEW_LINE, 1,  1);

	public XmlDumpSyntax() {}

	private SyntaxElem open(String name) {
		SpaceCmd[] lout_nl_ba = new SpaceCmd[] { new SpaceCmd(siNl, SP_ADD, SP_ADD, 0) };
		return new SyntaxToken("<"+name+">",lout_nl_ba);
	}
	private SyntaxElem close(String name) {
		SpaceCmd[] lout_nl_ba = new SpaceCmd[] { new SpaceCmd(siNl, SP_ADD, SP_ADD, 0) };
		return new SyntaxToken("</"+name+">",lout_nl_ba);
	}
	private SyntaxElem open0(String name) {
		return new SyntaxToken("<"+name+">",new SpaceCmd[0]);
	}
	private SyntaxElem close0(String name) {
		SpaceCmd[] lout_nl = new SpaceCmd[] { new SpaceCmd(siNl, SP_NOP, SP_ADD, 0) };
		return new SyntaxToken("</"+name+">",lout_nl);
	}
	protected SyntaxAttr attr(String slot) {
		return new SyntaxSubAttr(slot, new SpaceCmd[0]);
	}
	protected SyntaxNode node() {
		return new SyntaxNode();
	}
	protected SyntaxParagraphLayout par(SyntaxElem elem) {
		ParagraphLayout plIndented = new ParagraphLayout("par-indented", 1, 10);
		SyntaxParagraphLayout spl = new SyntaxParagraphLayout(elem, plIndented, new SpaceCmd[0]);
		return spl;
	}
	protected SyntaxSet set(SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(new SpaceCmd[0]);
		set.elements.addAll(elems);
		return set;
	}
	protected SyntaxSet setl(SpaceCmd[] spaces, SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet(spaces);
		set.elements.addAll(elems);
		return set;
	}
	protected SyntaxOptional opt(CalcOption calc, SyntaxElem opt_true, SyntaxElem opt_false, SpaceCmd[] spaces)
	{
		return new SyntaxOptional(calc,opt_true,opt_false,spaces);
	}

	public SyntaxElem getSyntaxElem(ANode node) {
		String cl_name = node.getClass().getName();
		SyntaxElemDecl sed = allSyntax.get(cl_name);
		if (sed != null)
			return sed.elem;
		SpaceCmd[] lout_nl = new SpaceCmd[] { new SpaceCmd(siNl, SP_NOP, SP_ADD, 0) };
		SpaceCmd[] lout_nl_ba = new SpaceCmd[] { new SpaceCmd(siNl, SP_ADD, SP_ADD, 0) };
		SyntaxSet ss = new SyntaxSet(lout_nl);
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			if (attr.is_space) {
				ss.elements += opt(new CalcOptionNotEmpty(attr.name),
						setl(lout_nl,
							open(attr.name),
							par(new SyntaxList(attr.name, node(), null, lout_nl)),
							close(attr.name)
							),
						null,new SpaceCmd[0]
						);
			}
			else if (ANode.class.isAssignableFrom(attr.clazz)) {
				ss.elements += opt(new CalcOptionNotNull(attr.name),
					setl(lout_nl, open(attr.name), par(attr(attr.name)), close(attr.name)),
					null,new SpaceCmd[0]
					);
			}
			else if (Enum.class.isAssignableFrom(attr.clazz))
				ss.elements += set(open0(attr.name), attr(attr.name), close0(attr.name));
			else if (attr.clazz == String.class)
				ss.elements += set(open0(attr.name), new SyntaxXmlStrAttr(attr.name,new SpaceCmd[0]), close0(attr.name));
			else if (attr.clazz == Integer.TYPE || attr.clazz == Boolean.TYPE ||
				attr.clazz == Byte.TYPE || attr.clazz == Short.TYPE || attr.clazz == Long.TYPE ||
				attr.clazz == Character.TYPE || attr.clazz == Float.TYPE || attr.clazz == Double.TYPE
				)
				ss.elements += set(open0(attr.name), attr(attr.name), close0(attr.name));
			else
				ss.elements += new SyntaxToken("<error attr='"+attr.name+"'"+" class='"+cl_name+"' />", lout_nl_ba);
		}
		{
			SyntaxSet sn = new SyntaxSet(lout_nl);
			sn.elements += new SyntaxToken("<a-node class='"+cl_name+"'>",lout_nl_ba);
			sn.elements += par(ss);
			sn.elements += new SyntaxToken("</a-node>",lout_nl_ba);
			ss = sn;
		}
		SyntaxElemDecl sed = new SyntaxElemDecl();
		sed.elem = ss;
		allSyntax.put(cl_name,sed);
		return ss;
	}
}

