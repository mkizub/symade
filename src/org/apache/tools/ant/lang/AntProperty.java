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

@ThisIsANode
public abstract class AntProperty extends AntNode {
	public AntType getAntType() { return new AntTypeEmpty(); }
}

@AntJavaClass("org.apache.tools.ant.taskdefs.Property")
@AntXMLQName("property")
@AntXMLAttributes({
	@AntXMLAttribute(value="name", attr="sname"),
	@AntXMLAttribute("value")})
@ThisIsANode
public final class AntValueProperty extends AntProperty {
	@nodeAttr public AntText  value;
}

@AntJavaClass("org.apache.tools.ant.taskdefs.Property")
@AntXMLQName("property")
@AntXMLAttributes({
	@AntXMLAttribute(value="name", attr="sname"),
	@AntXMLAttribute("location")})
@ThisIsANode
public final class AntLocationProperty extends AntProperty {
	@nodeAttr public AntText  location;
}

@AntJavaClass("org.apache.tools.ant.taskdefs.UpToDate")
@AntXMLQName("uptodate")
@AntXMLAttributes({
	@AntXMLAttribute("property"),
	@AntXMLAttribute("value")})
@ThisIsANode
public final class AntUpToDate extends AntNode implements AntNamesProvider {
	
	@nodeAttr public final AntValueProperty prop;
	
	@abstract
	@nodeData public String property;
	@abstract
	@nodeData public AntText value;
	
	public AntUpToDate() {
		this.prop = new AntValueProperty();
	}

	@getter public String get$property() { return prop.sname; }
	@setter public void set$property(String value) { prop.sname = value; }
	@getter public AntText get$value() { return prop.value; }
	@setter public void set$value(AntText value) { prop.value = value; }

	public rule resolveNameR(ResInfo path)
	{
		path ?= prop
	}
}

