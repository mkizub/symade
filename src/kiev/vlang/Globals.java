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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

operator "X ?= X" , 5;
operator "X @= X" , 5;

/**
 * @author Maxim Kizub
 *
 */

public metatype Globals extends any {

	@CompilerNode("Set")
	@macro @native
	public static <L extends Object, R extends L> R ref_assign(L lval, R val) operator "V = V";

	@CompilerNode("Set")
	@macro @native
	public static <L extends Object, R extends L> R ref_assign2(L lval, R val) operator "V := V";

	@CompilerNode("Set")
	@macro @native
	public static <L extends Object, R extends L> R@ ref_pvar_init(L@ lval, R@ val) operator "V := V";

	@CompilerNode("Set")
	@macro
	public static <L extends Object, R extends L> R ref_assign_pvar(L lval, R@ val) operator "V = V"
	{
		case AssignExpr# self():
			self.lval = (self.value).get$$var()
		case CallExpr# self():
			lval = (val).get$$var()
	}

	@CompilerNode("Set")
	@macro
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R val) operator "V = V"
	{
		case CallExpr# self():
			(lval).$bind(val)
		case AssignExpr# self():
			(self.lval).$bind(self.value)
	}

	@CompilerNode("Set")
	@macro
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R@ val) operator "V = V"
	{
		case CallExpr# self():
			(lval).$bind(val)
		case AssignExpr# self():
			(self.lval).$bind(self.value)
	}

	@CompilerNode("RuleIstheExpr")
	@macro
	public static boolean ref_pvar_is_the(Object@ lval, Object val) operator "V ?= V" ;

	@CompilerNode("RuleIsoneofExpr")
	@macro
	public static boolean ref_pvar_is_one_of(Object@ lval, Object val) operator "V @= V" ;

}

