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
package kiev.stdlib;

import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public class Asserts {
	/**
	 *  Conditional version of assert method.
	 *  The exception will be throwed if
	 *  condition is "false", i.e. - assertion
	 *  check failed.
	 *
	 *  @param	cond	the condition
	 */
	@macro
	public static void assert(boolean cond)
	{
		case Call# self():
			if# ($GenAsserts)
				{ if (new #AssertEnabledExpr() && ! cond) Debug.assert(); }
			else
				new #NopExpr()
	}

	/**
	 *  Conditional version of assert method with
	 *  message of exception specified
	 *
	 *  @param	cond	the condition
	 *  @param	msg		the message for exception
	 */
	@macro
	public static void assert(boolean cond, String msg)
	{
		case Call# self():
			if# ($GenAsserts)
				{ if (new #AssertEnabledExpr() && ! cond) Debug.assert(msg); }
			else
				new #NopExpr()
	}

	/**
	 *  Conditional version of assert method with
	 *  explicit exception specified to be throwed
	 *  or passed to $AssertionHandler
	 *
	 *  @param	cond	the condition
	 *  @param	t		the Throwable for exception
	 */
	@macro
	public static void assert(boolean cond, Throwable t)
	{
		case Call# self():
			if# ($GenAsserts)
				{ if (new #AssertEnabledExpr() && ! cond) Debug.assert(t); }
			else
				new #NopExpr()
	}

}
