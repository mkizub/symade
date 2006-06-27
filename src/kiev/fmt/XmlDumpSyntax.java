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


public class XmlDumpSyntax extends TextSyntax {

	final Hashtable<String,SyntaxElem> seAll;

	final DrawLayout lout_nl;
	final DrawLayout lout_nl_ba;

	final SyntaxElem seNull;

	public XmlDumpSyntax() {
		lout_nl    = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siNl,SP_ADD_AFTER,0)
			});
		lout_nl_ba = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siNl, SP_ADD_BEFORE, 0),
				new SpaceCmd(siNl, SP_ADD_AFTER, 0),
			});
		seAll = new Hashtable<String,SyntaxElem>();
		seNull = new SyntaxSpace(new DrawLayout());
		seNull.is_hidden = true;
	}
	private SyntaxElem open(String name) {
		return new SyntaxKeyword("<"+name+">",lout_nl_ba.ncopy());
	}
	private SyntaxElem close(String name) {
		return new SyntaxKeyword("</"+name+">",lout_nl_ba.ncopy());
	}
	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
		if (node == null)
			return seNull;
		String nm = node.getClass().getName();
		SyntaxElem se = seAll.get(nm);
		if (se != null)
			return se;
		SyntaxSet ss = new SyntaxSet(lout_nl.ncopy());
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			if (attr.is_space) {
				ss.elements += opt(attr.name, new CalcOptionNotEmpty(attr.name),
						setl(lout_nl.ncopy(),
							open(attr.name),
							par(plIndented, new SyntaxList(attr.name, node(), null, lout_nl.ncopy())),
							close(attr.name)
							),
						null,new DrawLayout()
						);
			}
			else if (ANode.class.isAssignableFrom(attr.clazz)) {
				ss.elements += opt(attr.name,
					setl(lout_nl.ncopy(), open(attr.name), par(plIndented, attr(attr.name)), close(attr.name))
					);
			}
			else if (attr.clazz == String.class)
				ss.elements += setl(lout_nl.ncopy(), kw("<str attr='"+attr.name+"'>"), string(attr.name), kw("</str>"));
			else if (attr.clazz == Integer.TYPE)
				ss.elements += setl(lout_nl.ncopy(), kw("<int attr='"+attr.name+"'>"), attr(attr.name), kw("</int>"));
			else if (attr.clazz == Boolean.TYPE)
				ss.elements += setl(lout_nl.ncopy(), kw("<bool attr='"+attr.name+"'>"), attr(attr.name), kw("</bool>"));
			else
				ss.elements += kw("<error attr='"+attr.name+"'"+" class='"+nm+"' />");
		}
		{
			SyntaxSet sn = new SyntaxSet(lout_nl.ncopy());
			sn.elements += new SyntaxKeyword("<node class='"+nm+"'>",lout_nl_ba.ncopy());
			sn.elements += par(plIndented, ss);
			sn.elements += new SyntaxKeyword("</node>",lout_nl_ba.ncopy());
			se = sn;
		}
		seAll.put(nm,se);
		return se;
	}
}

