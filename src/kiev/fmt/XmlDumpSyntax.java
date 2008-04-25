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

@ThisIsANode(lang=SyntaxLang)
public class SyntaxXmlStrAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxXmlStrAttr;

	public SyntaxXmlStrAttr() {}
	public SyntaxXmlStrAttr(String name) {
		super(name);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxXmlStrAttr dr_elem = new Draw_SyntaxXmlStrAttr();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxXmlTypeAttr extends SyntaxAttr {
	@virtual typedef This  = SyntaxXmlTypeAttr;

	public SyntaxXmlTypeAttr() {}
	public SyntaxXmlTypeAttr(String name) {
		super(name);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxXmlTypeAttr dr_elem = new Draw_SyntaxXmlTypeAttr();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class XmlDumpSyntax extends ATextSyntax {
	@virtual typedef This  = XmlDumpSyntax;

	@nodeAttr public String dump;

	@setter
	public void set$dump(String value) {
		this.dump = (value != null) ? value.intern() : null;
	}
	
	public XmlDumpSyntax() {
		this.dump = "full";
	}
	public XmlDumpSyntax(String dump) {
		this();
		this.dump = dump;
	}

	public Draw_ATextSyntax getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_XmlDumpSyntax(this.dump);
		fillCompiled(compiled);
		return compiled;
	}
}

