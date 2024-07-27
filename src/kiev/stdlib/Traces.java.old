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

public class Traces {
	/**
	 *  trace is a generic tracing/logging method.
	 *  It uses "log" stream to trace/log program
	 *  execution.           
	 *  The message is printed by log.println(String)
	 *
	 *  @param	msg		the message to log
	 */

	@macro
	public static void trace(String msg)
	{
		case Call# self():
			if# ($GenTraces)
				Debug.trace_force(msg)
			else
				new # NopExpr()
	}

	/**
	 *  The conditional version of trace method.
	 *  The message is printed only of condition
	 *  is true.
	 *  Note, that compiler automatically optimizes
	 *  the usage of this method - the message is
	 *  evaluated and method is called *only* if
	 *  condition is true.
	 *
	 *  @param	cond	the condition
	 *  @param	msg		the message to log
	 */

	@macro
	public static void trace(boolean cond, String msg)
	{
		case Call# self():
			if# ($GenTraces)
				{ if (cond) Debug.trace_force(msg) }
			else
				new # NopExpr()
	}

}
