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
 Base classe of rule frames, for
 java-compatible (unoptimized) mode
 of bytecode generation
*/

public abstract class #id"rule"# extends Object {
	public static boolean jcontains(Enumeration enumeration, Object value) {
		foreach(Object val; enumeration; val!=null && (val==value || val.equals(value)) )
			return true;
		return false;
	}

	public static boolean jcontains(java.util.Iterator iterator, Object value) {
		foreach(Object val; iterator; val!=null && (val==value || val.equals(value)) )
			return true;
		return false;
	}

	public static boolean jcontains(java.lang.Iterable iterable, Object value) {
		foreach(Object val; iterable; val!=null && (val==value || val.equals(value)) )
			return true;
		return false;
	}
}

/**
 The wrapper for prolog values
*/

@unerasable
public final class PVar<A> implements TypeInfoInterface
{

	private A			$_var_;
	
	@virtual
	@forward
	@abstract
	public A			$var;
	
	@virtual
	@abstract
	public:ro boolean	$is_bound;

	public PVar() {}

	public PVar(A var) {
		this.$_var_ = var;
	}

	@getter
	public A get$$var()
		alias $get_var
		alias $cast
		alias operator "( T ) V"
	{
		return $_var_;
	}

	@getter
	public boolean get$$is_bound()
		alias $get_is_bound
	{
		return $_var_ != null;
	}

	public String toString() {
		if( $is_bound )
			return $var.toString();
		else
			return "???";
	}

	public boolean equals(Object value) {
		A v = $var;
		return (v==null && value==null) || v.equals(value);
	}

	@setter
	public void $bind(A var)
		alias set$$var
	{
		this.$_var_ = (A)var;
	}

	public boolean $bind_chk(Object var)
	{
		if !(var instanceof A)
			return false;
		this.$_var_ = /*(A)*/var;
		return true;
	}

	public boolean $rebind_chk(Object var)
	{
		$unbind();
		return $bind_chk(var);
	}

	public void $unbind() {
		this.$_var_ = null;
	}
}

