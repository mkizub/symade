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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/PVar.java,v 1.2.2.1.2.2 1999/05/29 21:03:10 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2.2.1.2.2 $
 *
 */

public final class PVar<A>
	$generate <boolean>,<byte>,<char>,<short>,<int>,<long>,<float>,<double>
{

	forward public virtual A			$var;
	private PVar<A>						$pvar := null;
	public virtual abstract boolean		$is_bound;

	public PVar() {
		$is_bound = false;
	}

	public PVar(A var) {
		this.$var = var;
	}

	public A get$$var()
//		alias operator(210,fy,$cast)
	{
		if ($pvar.$self != null)
			return $pvar.$var;
		else
			return $var;
	}

	public void set$$var(A var) {
		$var = var;
		$pvar.$self = null;
//		if( !(A instanceof Object) )
//			$is_bound = true;
	}

	public boolean get$$is_bound() {
		if ($pvar.$self != null)
			return $pvar.get$$is_bound();
		if( A instanceof boolean )
			return $var != (A)Integer.MIN_VALUE;
		else if( A instanceof byte )
			return $var != (A)Integer.MIN_VALUE;
		else if( A instanceof char )
			return $var != (A)Integer.MIN_VALUE;
		else if( A instanceof short )
			return $var != (A)Integer.MIN_VALUE;
		else if( A instanceof int )
			return $var != (A)Integer.MIN_VALUE;
		else if( A instanceof long )
			return $var != (A)Long.MIN_VALUE;
		else if( A instanceof float )
			return $var != (A)Float.NaN;
		else if( A instanceof double )
			return $var != (A)Double.NaN;
		else // if( A instanceof Object )
			return $var != null;
	}

	public void set$$is_bound(boolean b) {
		if( b )
			throw new RuntimeException("Explisit set to be bound");
		$pvar.$self = null;
		if( A instanceof boolean )
			$var = (A)Integer.MIN_VALUE;
		else if( A instanceof byte )
			$var = (A)Integer.MIN_VALUE;
		else if( A instanceof char )
			$var = (A)Integer.MIN_VALUE;
		else if( A instanceof short )
			$var = (A)Integer.MIN_VALUE;
		else if( A instanceof int )
			$var = (A)Integer.MIN_VALUE;
		else if( A instanceof long )
			$var = (A)Long.MIN_VALUE;
		else if( A instanceof float )
			$var = (A)Float.NaN;
		else if( A instanceof double )
			$var = (A)Double.NaN;
		else //if( A instanceof Object )
			$var = null;
	}

	public String toString() {
		if( $is_bound )
			return $var.toString();
		else
			return "???";
	}

	public boolean equals(A value) {
		A v = $var;
		if( A instanceof Object )
			return (v==null && value==null) || v.equals(value);
		else
			return v.equals(value);
	}

	public void $bind(A var)
		alias operator(5,lfy,=)
	{
		this.$pvar.$self = null;
		this.$var = var;
	}

	public void $bind(PVar<A> var)
		alias operator(5,lfy,=)
	{
		this.$var = null;
		this.$pvar = var;
	}

	public boolean $bind_chk(A var)
	{
		this.$pvar.$self = null;
		this.$var = var;
		return $is_bound;
	}

	public boolean $bind_chk(PVar<A> var)
	{
		this.$var = null;
		this.$pvar = var;
		return $is_bound;
	}

	public void $unbind() {
		this.$var = null;
		this.$pvar.$self = null;
//		if( !(A instanceof Object) )
//			$is_bound = false;
	}

	public void $checkIsBinded(String name) {
		if( !$is_bound )
			throw new RuntimeException("Internal prolog error: var "+name+" is expected to be bounded");
	}
}

