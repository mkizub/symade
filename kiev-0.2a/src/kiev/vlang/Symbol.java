/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.
 
 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.vlang.PassInfo.NameAndPath;
import kiev.stdlib.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Symbol.java,v 1.3 1998/10/26 23:47:22 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class Symtable extends Hashtable<KString,NameAndPath> {

	public Symtable() {
		super(1024);
	}

    /**
     * Puts the specified element into the hashtable, using the specified
     * key.  The element may be retrieved by doing a get() with the same key.
     * The key and the element cannot be null.
     * @param key the specified key in the hashtable
     * @param value the specified element
     * @exception NullPointerException If the value of the element
     * is equal to null.
     * @see Hashtable#get
     * @return the old value of the key, or null if it did not have one.
     */
    public synchronized NameAndPath put(KString key, NameAndPath value) {
		// Makes sure the key is not already in the hashtable.
		HashtableEntry<KString,NameAndPath> tab[] = table;
		int hash = key.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;

		// Creates the new entry.
		tab[index] = new HashtableEntry<KString,NameAndPath>(key, value, hash, tab[index]);
		count++;
		return value;
    }
}
