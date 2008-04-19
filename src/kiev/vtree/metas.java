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
package kiev.vtree;

import java.lang.annotation.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

// syntax-tree node
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ThisIsANode {
	String name() default "";
	Class lang() default void;
	boolean copyable() default true;
}
// syntax-tree attribute field
public @interface nodeAttr {
	boolean copyable() default true;
	boolean ext_data() default false;
}
// syntax-tree reference field
public @interface nodeData {
	boolean copyable() default true;
	boolean ext_data() default false;
}
// the field of a node is not versioned and is present in compiler version only
public @interface UnVersioned {}

@Retention(RetentionPolicy.RUNTIME)
public @interface DataFlowDefinition {
	String in() default "";
	String tru() default "";
	String fls() default "";
	String out() default "";
	String jmp() default "";
	String seq() default "";
	String[] links() default {};
}


