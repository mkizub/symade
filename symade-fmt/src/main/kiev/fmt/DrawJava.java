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

import syntax kiev.Syntax;

import kiev.fmt.common.*;

@ThisIsANode(copyable=false)
public class DrawJavaAccess extends DrawTerm {

	public DrawJavaAccess(INode node, Formatter fmt, Draw_SyntaxJavaAccess syntax) {
		super(node, fmt, syntax);
	}

	public boolean isTextual() { true }

	public void formatTerm(Formatter fmt) {
		fmt.formatTerm(this, getTermObj());
	}

	public Object getTermObj() {
		MetaAccess acc = (MetaAccess)drnode;
		String text;
		if (acc.simple == "public")
			text = "public"+mkString(0xFF);
		else if (acc.simple == "protected")
			text = "protected"+mkString(0x3F);
		else if (acc.simple == "private")
			text = "private"+mkString(0x03);
		else
			text = "@access"+mkString(0x0F);
		return text;
	}

	private String mkString(int expected) {
		MetaAccess acc = (MetaAccess)drnode;
		if (acc.flags == -1 || acc.flags == expected)
			return "";
		StringBuffer sb = new StringBuffer(":");

		if( acc.r_public && acc.w_public ) sb.append("rw,");
		else if( acc.r_public ) sb.append("ro,");
		else if( acc.w_public ) sb.append("wo,");
		else sb.append("no,");

		if( acc.r_protected && acc.w_protected ) sb.append("rw,");
		else if( acc.r_protected ) sb.append("ro,");
		else if( acc.w_protected ) sb.append("wo,");
		else sb.append("no,");

		if( acc.r_default && acc.w_default ) sb.append("rw,");
		else if( acc.r_default ) sb.append("ro,");
		else if( acc.w_default ) sb.append("wo,");
		else sb.append("no,");

		if( acc.r_private && acc.w_private ) sb.append("rw");
		else if( acc.r_private ) sb.append("ro");
		else if( acc.w_private ) sb.append("wo");
		else sb.append("no");

		return sb.toString();
	}

}

@ThisIsANode(copyable=false)
public class DrawJavaPackedField extends DrawTerm {

	public DrawJavaPackedField(INode node, Formatter fmt, Draw_SyntaxJavaPackedField syntax) {
		super(node, fmt, syntax);
	}

	public boolean isTextual() { true }

	public void formatTerm(Formatter fmt) {
		fmt.formatTerm(this, getTermObj());
	}

	public Object getTermObj() {
		MetaPacked mp = (MetaPacked)drnode;
		String text = "@packed("+mp.size;
		if (mp.fld != null)
			text += ","+mp.fld+","+mp.offset;
		text += ")";
		return text;
	}
}

