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
package kiev.vlang;

import syntax kiev.Syntax;

/**
 * TODO: to be removed
 *
 * @author Maxim Kizub
 *
 */

public metatype Globals extends any {

	@macro @native @CompilerNode("CmpNode")
	public static boolean node_ref_eq(Any# o1, Any# o2) operator "V == V" ;

	@macro @native @CompilerNode("CmpNode")
	public static boolean node_ref_neq(Any# o1, Any# o2) operator "V != V" ;

	@macro @native @CompilerNode("CmpNode")
	public static boolean node_ref_eq_null(Any# o1, #id"null"# o2) operator "V == V" ;

	@macro @native @CompilerNode("CmpNode")
	public static boolean node_ref_neq_null(Any# o1, #id"null"# o2) operator "V != V" ;

}

