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
package org.apache.tools.ant.lang;
import syntax kiev.Syntax;

import kiev.vdom.*;
import org.apache.tools.ant.taskdefs.MacroDef;

@ThisIsANode
public abstract class AntLibDef extends AntNode {}

@AntJavaClass("org.apache.tools.ant.taskdefs.Typedef")
@AntXMLQName("typedef")
@AntXMLAttributes({
	@AntXMLAttribute(value="name", attr="sname"),
	@AntXMLAttribute("classname")})
@ThisIsANode
public class AntTypeDef extends AntLibDef {
	@nodeAttr public String classname;
}

@AntJavaClass("org.apache.tools.ant.taskdefs.Taskdef")
@AntXMLQName("taskdef")
@AntXMLAttributes({
	@AntXMLAttribute(value="name", attr="sname"),
	@AntXMLAttribute("classname")})
@ThisIsANode
public final class AntTaskDef extends AntLibDef {
	@nodeAttr public String classname;
}

@AntJavaClass("org.apache.tools.ant.taskdefs.PreSetDef")
@AntXMLQName("presetdef")
@AntXMLAttributes({@AntXMLAttribute(value="name", attr="sname")})
@ThisIsANode
public class AntPresetDef extends AntLibDef {

	boolean isTaskContainer() { true }

	public AntTask getAntTask() {
		foreach (AntTask at; members)
			return at;
		return null;
	}
}

@AntJavaClass("org.apache.tools.ant.taskdefs.MacroDef")
@AntXMLQName("macrodef")
@AntXMLAttributes({@AntXMLAttribute(value="name", attr="sname")})
@ThisIsANode
public class AntMacroDef extends AntLibDef implements ScopeOfNames {

	public rule resolveNameR(ResInfo path)
	{
		path ?= this
	;	path @= members
	}

}

@AntJavaClass("org.apache.tools.ant.taskdefs.MacroDef$Attribute")
@AntXMLQName("attribute")
@AntXMLAttributes({@AntXMLAttribute(value="name", attr="sname")})
@ThisIsANode
public class AntMacroAttribute extends AntNode {
}

@AntJavaClass("org.apache.tools.ant.taskdefs.MacroDef$TemplateElement")
@AntXMLQName("element")
@AntXMLAttributes({@AntXMLAttribute(value="name", attr="sname")})
@ThisIsANode
public class AntMacroElement extends AntLibDef {

	@nodeData AntMacroData data;
	
	public boolean isImplicit() {
		foreach (AntAttribute attr; attributes; attr.prefix == null && "implicit".equalsIgnoreCase(attr.name)) {
			String val = attr.text.toText();
			return ("true".equals(val) || "yes".equals(val) || "on".equals(val));
		}
		return false;
	}
}

@AntJavaClass("org.apache.tools.ant.taskdefs.MacroDef$Text")
@AntXMLQName("element")
@AntXMLAttributes({@AntXMLAttribute(value="name", attr="sname")})
@ThisIsANode
public class AntMacroText extends AntNode {
}

