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

import java.lang.annotation.*;

// Used to set AntNode sub-class or attribute XML qname
@Retention(RetentionPolicy.RUNTIME)
public @interface AntXMLQName {
	public String value();
	public String uri() default "";
	public String prefix() default "";
}
// Used to link node and XML attribute 
@Retention(RetentionPolicy.RUNTIME)
public @interface AntXMLAttribute {
	public String attr() default ""; // node attribute name, defaults to XML name (specified by 'value')
	public String value(); // XML name
	public String uri() default "";
	public String prefix() default "";
}
// List of links between node and XML attributes 
@Retention(RetentionPolicy.RUNTIME)
public @interface AntXMLAttributes {
	public AntXMLAttribute[] value();
}
// Specifies Java class served by this AntNode class
@Retention(RetentionPolicy.RUNTIME)
public @interface AntJavaClass {
	public String value();
}

public interface AntNamesProvider extends ScopeOfNames {}

public class AntClassMapInfo {
	// the SymADE node class, into which the ant class is mapped
	public final Class ast_clazz;
	public AntClassMapInfo(Class ast_clazz) {
		this.ast_clazz = ast_clazz;
	}
}

