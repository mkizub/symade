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
 * @version $Revision: 182 $
 *
 */

/** The super class of all enumerations */
public abstract class Enum {
	
	private final String $name;
	private final int $ordinal;
	private final String $text;
	
	protected Enum(String name, int ordinal) {
		this.$name = name.intern();
		this.$ordinal = ordinal;
		this.$text = name;
	}
	
	protected Enum(String name, int ordinal, String text) {
		this.$name = name.intern();
		this.$ordinal = ordinal;
		this.$text = text.intern();
	}
	
	public final String name() {
		return $name;
	}
	
	public final int ordinal() {
		return $ordinal;
	}
	
	public String toString() {
		return $name;
	}

}
