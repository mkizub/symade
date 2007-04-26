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
 * @version $Revision$
 *
 */

public abstract class closure implements Cloneable {

	public static Object[]	$noargs = new Object[0];

	public Object[]		$args;
	public int			max$args;	// = 0
	public int			top$arg;	// = 0
	
	public closure(int max$args) {
		this.max$args = max$args;
		$args = max$args==0? $noargs: new Object[max$args];
	}
	
	protected void throwException(String msg) {
		throw new RuntimeException("This closure "+getClass()+" "+msg);
	}
	
	public void		call_void()		{ throwException("does not returns void"); }
	public rule		call_rule()		{ throwException("is not a rule") }
	public boolean	call_boolean()	{ throwException("does not returns boolean"); return false; }
	public byte		call_byte()		{ throwException("does not returns byte"); return (byte)0; }
	public char		call_char()		{ throwException("does not returns char"); return '\000'; }
	public short	call_short()	{ throwException("does not returns short"); return (short)0; }
	public int		call_int()		{ throwException("does not returns int"); return 0; }
	public long		call_long()		{ throwException("does not returns long"); return 0L; }
	public float	call_float()	{ throwException("does not returns float"); return 0.F; }
	public double	call_double()	{ throwException("does not returns double"); return 0.D; }
	public Object	call_Object()	{ throwException("does not returns Object"); return null; }
	
	public Object clone() {
		closure cl;
		try {
			cl = (closure)super.clone();
			if( cl.$args.length != 0 )
				cl.$args = (Object[])$args.clone();
		} catch( java.lang.CloneNotSupportedException e) {
			throw new RuntimeException("internal error: "+e.getMessage());
		}
		return cl;
	}
	
	public closure addArg(boolean arg) {
		try {
			$args[top$arg++] = Boolean.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(byte arg) {
		try {
			$args[top$arg++] = Byte.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(char arg) {
		try {
			$args[top$arg++] = Character.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(short arg) {
		try {
			$args[top$arg++] = Short.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(int arg) {
		try {
			$args[top$arg++] = Integer.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(long arg) {
		try {
			$args[top$arg++] = Long.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(float arg) {
		try {
			$args[top$arg++] = Float.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(double arg) {
		try {
			$args[top$arg++] = Double.valueOf(arg);
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
	public closure addArg(Object arg) {
		try {
			$args[top$arg++] = arg;
		} catch( java.lang.ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("argno = "+(--top$arg)+", but max args = "+max$args);
		}
		return this;
	}
}
