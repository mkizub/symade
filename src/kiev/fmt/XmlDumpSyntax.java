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

	final SpaceCmd[] lout_nl;
	final SpaceCmd[] lout_nl_ba;

	final SyntaxElem seNull;

	public XmlDumpSyntax() {
		lout_nl    = new SpaceCmd[] {
				new SpaceCmd(siNl,SP_ADD_AFTER,0)
			};
		lout_nl_ba = new SpaceCmd[] {
				new SpaceCmd(siNl, SP_ADD_BEFORE, 0),
				new SpaceCmd(siNl, SP_ADD_AFTER, 0)
			};
		seAll = new Hashtable<String,SyntaxElem>();
		seNull = new SyntaxSpace();
		seNull.is_hidden = true;
	}

	public String escapeString(String str) {
		StringBuffer sb = new StringBuffer(str);
		boolean changed = false;
		for(int i=0; i < sb.length(); i++) {
			switch (sb.charAt(i)) {
			case '&':  sb.setCharAt(i, '&'); sb.insert(i+1,"amp;");  i += 4; changed = true; continue;
			case '<':  sb.setCharAt(i, '&'); sb.insert(i+1,"lt;");   i += 3; changed = true; continue;
			case '>':  sb.setCharAt(i, '&'); sb.insert(i+1,"gt;");   i += 3; changed = true; continue;
			case '\"': sb.setCharAt(i, '&'); sb.insert(i+1,"quot;"); i += 5; changed = true; continue;
			case '\'': sb.setCharAt(i, '&'); sb.insert(i+1,"apos;"); i += 5; changed = true; continue;
			}
		}
		if (changed) return sb.toString();
		return str;
	}
	public String escapeChar(char ch) {
		switch (ch) {
		case '&':  return "&amp;";
		case '<':  return "&lt;";
		case '>':  return "&gt;";
		case '\"': return "&quot;";
		case '\'': return "&apos;";
		default: return String.valueOf(ch);
		}
	}
	
	private SyntaxElem open(String name) {
		return new SyntaxKeyword("<"+name+">",lout_nl_ba);
	}
	private SyntaxElem close(String name) {
		return new SyntaxKeyword("</"+name+">",lout_nl_ba);
	}
	private SyntaxElem open0(String name) {
		return new SyntaxKeyword("<"+name+">",new SpaceCmd[0]);
	}
	private SyntaxElem close0(String name) {
		return new SyntaxKeyword("</"+name+">",lout_nl);
	}
	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
		if (node == null)
			return seNull;
		String nm = node.getClass().getName();
		SyntaxElem se = seAll.get(nm);
		if (se != null)
			return se;
		SyntaxSet ss = new SyntaxSet(lout_nl);
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			if (attr.is_space) {
				ss.elements += opt(attr.name, new CalcOptionNotEmpty(attr.name),
						setl(lout_nl,
							open(attr.name),
							par(plIndented, new SyntaxList(attr.name, node(), null, lout_nl)),
							close(attr.name)
							),
						null,new SpaceCmd[0]
						);
			}
			else if (ANode.class.isAssignableFrom(attr.clazz)) {
				ss.elements += opt(attr.name,
					setl(lout_nl, open(attr.name), par(plIndented, attr(attr.name)), close(attr.name))
					);
			}
			else if (Enum.class.isAssignableFrom(attr.clazz))
				ss.elements += set(open0(attr.name), attr(attr.name), close0(attr.name));
			else if (attr.clazz == String.class)
				ss.elements += set(open0(attr.name), new SyntaxStrAttr(attr.name,new SpaceCmd[0]), close0(attr.name));
			else if (attr.clazz == Integer.TYPE || attr.clazz == Boolean.TYPE ||
				attr.clazz == Byte.TYPE || attr.clazz == Short.TYPE || attr.clazz == Long.TYPE ||
				attr.clazz == Character.TYPE || attr.clazz == Float.TYPE || attr.clazz == Double.TYPE
				)
				ss.elements += set(open0(attr.name), attr(attr.name), close0(attr.name));
			else
				ss.elements += kw("<error attr='"+attr.name+"'"+" class='"+nm+"' />");
		}
		{
			SyntaxSet sn = new SyntaxSet(lout_nl);
			sn.elements += new SyntaxKeyword("<a-node class='"+nm+"'>",lout_nl_ba);
			sn.elements += par(plIndented, ss);
			sn.elements += new SyntaxKeyword("</a-node>",lout_nl_ba);
			se = sn;
		}
		seAll.put(nm,se);
		return se;
	}
}

