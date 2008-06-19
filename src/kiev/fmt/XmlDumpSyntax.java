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
public class XmlDumpSyntax extends ATextSyntax {
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

@ThisIsANode(lang=SyntaxLang)
public class NsXmlDumpSyntax extends ATextSyntax {
	@nodeAttr public String dump;

	@setter
	public void set$dump(String value) {
		this.dump = (value != null) ? value.intern() : null;
	}
	
	public NsXmlDumpSyntax() {
		this.dump = "full";
	}
	public NsXmlDumpSyntax(String dump) {
		this();
		this.dump = dump;
	}

	public Draw_ATextSyntax getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_NsXmlDumpSyntax(this.dump);
		fillCompiled(compiled);
		return compiled;
	}
}

