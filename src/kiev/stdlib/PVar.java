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

@unerasable
public final class PVar<A> implements TypeInfoInterface
{

	private A			$_var_;
	private PVar<A>		$_pvar_;
	
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
		alias fy operator $cast
	{
		if ($_pvar_ != null)
			return $_pvar_.$get_var();
		else
			return $_var_;
	}

	@getter
	public boolean get$$is_bound()
		alias $get_is_bound
	{
		if ($_pvar_ != null)
			return $_pvar_.$get_is_bound();
		return $_var_ != null;
	}

	public String toString() {
		if( $is_bound )
			return $var.toString();
		else
			return "???";
	}

	public boolean equals(A value) {
		A v = $var;
		return (v==null && value==null) || v.equals(value);
	}

	public boolean equals(PVar<A> value) {
		A v1 = $var;
		A v2 = value.$var;
		return (v1==null && v2==null) || v1.equals(v2);
	}

	@setter
	public void $bind(A var)
		alias set$$var
	{
		this.$_pvar_ = null;
		this.$_var_ = (A)var;
	}

	public void $bind(PVar<A> var)
	{
		this.$_var_ = null;
		if (var.$is_bound)
			this.$_var_ = (A)var.$var;
		else
			this.$_pvar_ = var;
	}

	public boolean $bind_chk(Object var)
	{
		if !(var instanceof A)
			return false;
		this.$_pvar_ = null;
		this.$_var_ = (A)var;
		return true;
	}

	public boolean $bind_chk(PVar<Object> var)
	{
		if !(var instanceof PVar<A>/*!this.getTypeInfoField().$instanceof(var)*/)
			return false;
		this.$_var_ = null;
		if (var.$is_bound) {
			this.$_var_ = (A)var.$var;
			return true;
		} else {
			this.$_pvar_ = var;
			return false;
		}
	}

	public boolean $rebind_chk(Object var)
	{
		$unbind();
		return $bind_chk(var);
	}

	public boolean $rebind_chk(PVar<Object> var)
	{
		$unbind();
		return $bind_chk(var);
	}

	public void $unbind() {
		this.$_var_ = null;
		this.$_pvar_ = null;
	}

	public void $checkIsBinded(String name) {
		if( !$is_bound )
			throw new RuntimeException("Internal prolog error: var "+name+" is expected to be bounded");
	}
}

