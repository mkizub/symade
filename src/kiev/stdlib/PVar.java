/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.

 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.stdlib;

import syntax kiev.stdlib.Syntax;

@unerasable
public final class PVar<A>
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
		this.$_var_ = var;
	}

	public void $bind(PVar<A> var)
	{
		this.$_var_ = null;
		if (var.$is_bound)
			this.$_var_ = var.$var;
		else
			this.$_pvar_ = var;
	}

	public boolean $bind_chk(A var)
	{
		this.$_pvar_ = null;
		this.$_var_ = var;
		return var != null;
	}

	public boolean $bind_chk(PVar<A> var)
	{
		this.$_var_ = null;
		if (var.$is_bound) {
			this.$_var_ = var.$var;
			return true;
		} else {
			this.$_pvar_ = var;
			return false;
		}
	}

	public boolean $rebind_chk(A var)
	{
		$unbind();
		return $bind_chk(var);
	}

	public boolean $rebind_chk(PVar<A> var)
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

