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

public final class Ref<A>
	$generate <boolean>,<byte>,<char>,<short>,<int>,<long>,<float>,<double>
{

	forward public A			$ref;

	public Ref() {}

	public Ref(A ref) {
		this.$ref = ref;
	}

	public String toString() {
		return "ref "+$ref;
	}

	public boolean equals(A value) {
		A r = $ref;
		if( A instanceof Object )
			return (r==null && value==null) || r.equals(value);
		else
			return r.equals(value);
	}

}

