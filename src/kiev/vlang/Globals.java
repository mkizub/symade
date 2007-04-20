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

	@macro @CompilerNode("Set")
	public static <L extends Object, R extends L> R ref_assign_pvar(L lval, R@ val) operator "V = V"
	{
		case AssignExpr# self():
			self.lval = (self.value).get$$var()
		case CallExpr# self():
			lval = (val).get$$var()
	}

	@macro @CompilerNode("Set")
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R val) operator "V = V"
	{
		case CallExpr# self():
			(lval).$bind(val)
		case AssignExpr# self():
			(self.lval).$bind(self.value)
	}

	@macro @CompilerNode("Set")
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R@ val) operator "V = V"
	{
		case CallExpr# self():
			(lval).$bind(val)
		case AssignExpr# self():
			(self.lval).$bind(self.value)
	}

}

