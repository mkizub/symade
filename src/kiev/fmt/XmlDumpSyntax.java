/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.fmt;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import syntax kiev.Syntax;

@node
public class SyntaxXmlStrAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxXmlStrAttr;

	public SyntaxXmlStrAttr() {}
	public SyntaxXmlStrAttr(String name) {
		super(name);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawXmlStrTerm(node, this, name);
		return dr;
	}
}

@node
public class SyntaxXmlTypeAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxXmlTypeAttr;

	public SyntaxXmlTypeAttr() {}
	public SyntaxXmlTypeAttr(String name) {
		super(name);
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawXmlTypeTerm(node, this, name);
		return dr;
	}
}

@node
public class XmlDumpSyntax extends ATextSyntax {
	@virtual typedef This  = XmlDumpSyntax;

	@ref SpaceInfo siNl = new SpaceInfo("nl", SP_NEW_LINE, 1,  1);
	@ref SyntaxElemFormatDecl sefdNoNo = new SyntaxElemFormatDecl("fmt-default");
	@ref SyntaxElemFormatDecl sefdNlNl = new SyntaxElemFormatDecl("fmt-nl-nl");
	@ref SyntaxElemFormatDecl sefdNoNl = new SyntaxElemFormatDecl("fmt-no-nl");
	@ref ParagraphLayout plIndented = new ParagraphLayout("par-indented", 1, 10);

	@att public String dump;

	@setter
	public void set$dump(String value) {
		this.dump = (value != null) ? value.intern() : null;
	}
	
	public XmlDumpSyntax() {
		sefdNlNl.spaces += new SpaceCmd(siNl, SP_ADD, SP_ADD, 0);
		sefdNoNl.spaces += new SpaceCmd(siNl, SP_NOP, SP_ADD, 0);
		this.members += siNl;
		this.members += sefdNoNo;
		this.members += sefdNlNl;
		this.members += sefdNoNl;
		this.members += plIndented;
	}
	public XmlDumpSyntax(String dump) {
		this();
		this.dump = dump;
	}

	private SyntaxElem open(String name) {
		SyntaxToken st = new SyntaxToken("<"+name+">");
		//st.fmt = new RefElemFormat(sefdNlNl);
		st.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNlNl);
		return st;
	}
	private SyntaxElem close(String name) {
		SyntaxToken st = new SyntaxToken("</"+name+">");
		//st.fmt = new RefElemFormat(sefdNlNl);
		st.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNlNl);
		return st;
	}
	private SyntaxElem open0(String name) {
		SyntaxToken st = new SyntaxToken("<"+name+">");
		//st.fmt = new RefElemFormat(sefdNoNo);
		st.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNoNo);
		return st;
	}
	private SyntaxElem close0(String name) {
		SyntaxToken st = new SyntaxToken("</"+name+">");
		//st.fmt = new RefElemFormat(sefdNoNl);
		st.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNoNl);
		return st;
	}
	protected SyntaxAttr attr(String slot) {
		return new SyntaxSubAttr(slot);
	}
	protected SyntaxParagraphLayout par(SyntaxElem elem) {
		SyntaxParagraphLayout spl = new SyntaxParagraphLayout(elem, plIndented);
		return spl;
	}
	protected SyntaxSet set(SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet();
		set.elements.addAll(elems);
		return set;
	}
	protected SyntaxSet setl(SyntaxElemFormatDecl sefd, SyntaxElem... elems) {
		SyntaxSet set = new SyntaxSet();
		//set.fmt = new RefElemFormat(sefd);
		set.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefd);
		set.elements.addAll(elems);
		return set;
	}
	protected SyntaxOptional opt(CalcOption calc, SyntaxElem opt_true)
	{
		return new SyntaxOptional(calc,opt_true,null);
	}

	public SyntaxElem getSyntaxElem(ANode node) {
		String cl_name = node.getClass().getName();
		SyntaxElemDecl sed = allSyntax.get(cl_name);
		if (sed != null)
			return sed.elem;
		SpaceCmd[] lout_nl = new SpaceCmd[] { new SpaceCmd(siNl, SP_NOP, SP_ADD, 0) };
		SpaceCmd[] lout_nl_ba = new SpaceCmd[] { new SpaceCmd(siNl, SP_ADD, SP_ADD, 0) };
		SyntaxSet ss = new SyntaxSet();
		//ss.fmt = new RefElemFormat(sefdNoNl);
		ss.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNoNl);
		foreach (AttrSlot attr; node.values(); attr != ASTNode.nodeattr$this && attr != ASTNode.nodeattr$parent) {
			SyntaxElem se = null;
			if (attr.is_space) {
				SyntaxList sl = new SyntaxList(attr.name);
				sl.filter = new CalcOptionIncludeInDump(this.dump,"this");
				//sl.fmt = new RefElemFormat(sefdNoNl);
				sl.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNoNl);
				sl.elpar.symbol = plIndented;
				se = setl(sefdNoNl, open(attr.name), sl, close(attr.name));
			} else {
				if (ANode.class.isAssignableFrom(attr.clazz))
					se = setl(sefdNoNl, open(attr.name), par(attr(attr.name)), close(attr.name));
				else if (Enum.class.isAssignableFrom(attr.clazz))
					se = set(open0(attr.name), attr(attr.name), close0(attr.name));
				else if (attr.clazz == String.class)
					se = set(open0(attr.name), new SyntaxXmlStrAttr(attr.name), close0(attr.name));
				else if (attr.clazz == Operator.class)
					se = set(open0(attr.name), new SyntaxXmlStrAttr(attr.name), close0(attr.name));
				else if (Type.class.isAssignableFrom(attr.clazz))
					se = set(open0(attr.name), new SyntaxXmlTypeAttr(attr.name), close0(attr.name));
				else if (attr.clazz == Integer.TYPE || attr.clazz == Boolean.TYPE ||
					attr.clazz == Byte.TYPE || attr.clazz == Short.TYPE || attr.clazz == Long.TYPE ||
					attr.clazz == Character.TYPE || attr.clazz == Float.TYPE || attr.clazz == Double.TYPE
					)
					se = set(open0(attr.name), attr(attr.name), close0(attr.name));
				else if (attr.is_attr) {
					SyntaxToken st = new SyntaxToken("<error attr='"+attr.name+"'"+" class='"+cl_name+"' />");
					//st.fmt = new RefElemFormat(sefdNlNl);
					st.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNlNl);
					se = st;
				}
			}
			if (se != null)
				ss.elements += opt(new CalcOptionIncludeInDump(this.dump,attr.name),se);
		}
		{
			SyntaxToken st;
			SyntaxSet sn = new SyntaxSet();
			//ss.fmt = new RefElemFormat(sefdNoNl);
			ss.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNoNl);
			st = new SyntaxToken("<a-node class='"+cl_name+"'>");
			//st.fmt = new RefElemFormat(sefdNlNl);
			st.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNlNl);
			sn.elements += st;
			sn.elements += par(ss);
			st = new SyntaxToken("</a-node>");
			//st.fmt = new RefElemFormat(sefdNlNl);
			st.fmt = new SymbolRef<SyntaxElemFormatDecl>(sefdNlNl);
			sn.elements += st;
			ss = sn;
		}
		SyntaxElemDecl sed = new SyntaxElemDecl();
		sed.elem = ss;
		allSyntax.put(cl_name,sed);
		members += sed;
		return ss;
	}
}

