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
package kiev.vtree;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 271 $
 *
 */

public final class SymUUID extends ANode.UnVersionedData {
	
	public static final SymUUID Empty = new SymUUID(0L, 0L, null);
	
	public final long	high;
	public final long	low;
	public final Symbol symbol;
	SymUUID next;
	
	/*
     * UUID = <time_low> "-" <time_mid> "-" <time_high_and_version> "-" <variant_and_sequence> "-" <node>
     * time_low               = 4*<hexOctet>
     * time_mid               = 2*<hexOctet>
     * time_high_and_version  = 2*<hexOctet>
     * variant_and_sequence   = 2*<hexOctet>
     * node                   = 6*<hexOctet>
     * hexOctet               = <hexDigit><hexDigit>
     * hexDigit               =
     *       "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
     *       | "a" | "b" | "c" | "d" | "e" | "f"
     *       | "A" | "B" | "C" | "D" | "E" | "F"
	 */
	public SymUUID(String uuid, Symbol symbol) {
		this.symbol = symbol;
		this.high = decode(uuid, 0);
		this.low = decode(uuid, 19);
	}
	public SymUUID(long high, long low, Symbol symbol) {
		this.high = high;
		this.low = low;
		this.symbol = symbol;
	}

	public boolean equals(Object obj) {
		if (obj instanceof SymUUID)
			return high == obj.high && low == obj.low;
		return false;
	}

	public int hashCode() {
		return (int)((high >> 32) ^ high ^ (low >> 32) ^ low);
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer(36);
		digits(high >> 32, 8, sb);
		sb.append('-');
		digits(high >> 16, 4, sb);
		sb.append('-');
		digits(high, 4, sb);
		sb.append('-');
		digits(low >> 48, 4, sb);
		sb.append('-');
		digits(low, 12, sb);
		return sb.toString();
	}

	private static void digits(long val, int digits, StringBuffer sb) {
		for (digits = (digits - 1) << 2; digits >= 0; digits -= 4) {
			int ch = ((int)(val >> digits)) & 0xF;
			if (ch <= 9)
				ch += '0';
			else
				ch = ch - 10 + 'a';
			sb.append((char)ch);
		}
	}

	private static long decode(String uuid, int idx) {
		long v = 0L;
		for (int shift = 64; shift > 0; idx++) {
			int ch = uuid.charAt(idx);
			if (ch == '-') continue;
			if (ch >= '0' && ch <= '9') ch -= '0';
			else if (ch >= 'a' && ch <= 'f') ch = 10 + ch - 'a';
			else if (ch >= 'A' && ch <= 'F') ch = 10 + ch - 'A';
			shift -= 4;
			v |= ((long)ch) << shift;
		}
		return v;
	}
}

public class SymUUIDHash {
    /**
     * The hash table data.
     */
    protected SymUUID[] table;

    /**
     * The total number of entries in the hash table.
     */
    protected int count;

    /**
     * Rehashes the table when count exceeds this threshold.
     */
    protected int threshold;

    /**
     * The load factor for the hashtable.
     */
    protected float loadFactor;

    /**
     * Constructs a new, empty hashtable.
     */
	public SymUUIDHash() {
		this.loadFactor = 0.75f;
		table = new SymUUID[1025];
		threshold = (int)(101 * 0.75);
	}

    /**
     * Returns the number of elements contained in the set.
     */
	public int size() {
		return count;
	}

    /**
     * Returns true if the set contains no elements.
     */
	public boolean isEmpty() {
		return count == 0;
	}

   /**
     * Returns element that equals the given one, ore null if there is no such element
     * @param elem the elem that we are looking for
     * @see Hashtable#contains
     */
	public Symbol get(SymUUID elem) {
		SymUUID[] tab = table;
		int hash = elem.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (SymUUID e = tab[index] ; e != null ; e = e.next) {
			if (e.high == elem.high && e.low == elem.low)
				return e.symbol;
		}
		return null;
	}

    /**
     * Rehashes the content of the table into a bigger table.
     * This method is called automatically when the set's
     * size exceeds the threshold.
     */
	protected void rehash() {
		int oldCapacity = table.length;
		SymUUID[] oldTable = table;
		
		int newCapacity = oldCapacity * 2 + 1;
		SymUUID[] newTable = new SymUUID[newCapacity];
		
		threshold = (int)(newCapacity * loadFactor);
		table = newTable;
		
		for (int i = oldCapacity ; i-- > 0 ;) {
			for (SymUUID old = oldTable[i] ; old != null ; ) {
				SymUUID e = old;
				old = old.next;
				int index = (e.hashCode() & 0x7FFFFFFF) % newCapacity;
				e.next = newTable[index];
				newTable[index] = e;
			}
		}
	}

    /**
     * Puts the specified element into the set
     */
	public void put(SymUUID elem) {
		// Makes sure the elem is not already in the set.
		SymUUID[] tab = table;
		int hash = elem.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (SymUUID e = tab[index] ; e != null ; e = e.next) {
			if (e == elem)
				return;
		}
		
		if (count >= threshold) {
			// Rehash the table if the threshold is exceeded
			rehash();
			put(elem);
			return;
		}
		
		// Creates the new entry.
		elem.next = tab[index];
		tab[index] = elem;
		count++;
	}

    /**
     * Removes the element corresponding to the elem. Does nothing if the
     * elem is not present.
     * return true iff `elem' was in set.
     * @param elem the elem that needs to be removed
     */
	public boolean remove(SymUUID elem) {
		SymUUID[] tab = table;
		int hash = elem.hashCode();
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (SymUUID e = tab[index], prev = null ; e != null ; prev = e, e = e.next) {
			if (e == elem) {
				if (prev != null)
					prev.next = e.next;
				else
					tab[index] = e.next;
				e.next = null;
				count--;
				return true;
			}
		}
		return false;
	}
	
    /**
     * Clears the set so that it has no more elements in it.
     */
	public void clear() {
		SymUUID[] tab = table;
		for (int index = tab.length; --index >= 0; )
			tab[index] = null;
		count = 0;
	}
}

