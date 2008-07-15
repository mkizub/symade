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
import syntax kiev.Syntax;

import java.lang.annotation.*;

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
@Retention(RetentionPolicy.RUNTIME)
public @interface nodeAttr {
	boolean copyable() default true;
	boolean ext_data() default false;
}
// syntax-tree reference field
@Retention(RetentionPolicy.RUNTIME)
public @interface nodeData {
	boolean copyable() default true;
	boolean ext_data() default false;
}
// the field of a node is not versioned and is present in compiler version only
@Retention(RetentionPolicy.RUNTIME)
public @interface UnVersioned {}

//
// metadata of attributes
//

// scalar or space attribute arity (required or optional)
@Retention(RetentionPolicy.RUNTIME)
public @interface attrOptional {
	boolean value() default true;
}

// attribute is not copied during node deep copy
@Retention(RetentionPolicy.RUNTIME)
public @interface attrUnCopyable {
	boolean value() default true;
}

// attribute is extended data (thus, optional), and stored in ANode.ext_data[]
@Retention(RetentionPolicy.RUNTIME)
public @interface attrExtended {
	boolean value() default true;
}

// attribute is not modifiable (read-only, final)
@Retention(RetentionPolicy.RUNTIME)
public @interface attrReadOnly {
	boolean value() default true;
}

// attribute is not modifiable (read-only, final)
// attr(): dump as XML attribute, instead of element; for primitive types only
// name(): XML elemment/attribute name; use field name if empty
@Retention(RetentionPolicy.RUNTIME)
public @interface AttrXMLDumpInfo {
	boolean ignore() default false;
	boolean attr() default false;
	String name() default "";
}

//
// data flow annotation
//

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

