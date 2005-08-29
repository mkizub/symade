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
 * @author Maxim Kizub
 * @version $Revision: 1.2.2.1.2.2 $
 *
 */

public final $wrapper class Ref<A>
{

	forward public A			$val;

	public Ref() {}

	public Ref(A value) {
		this.$val = value;
	}

	public String toString() {
		return String.valueOf($val);
	}

	public boolean equals(A value) {
		A r = $val;
		return (r==null && value==null) || r.equals(value);
	}
}
