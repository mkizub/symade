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
 * @version $Revision: 1.2.4.2 $
 *
 */

/* To sucessfully store and retrieve objects from a set, the
 * object used as the elem must implement the hashCode() and equals()
 * methods.<p>
 *
 */
public
class Hash<A extends Object> implements Cloneable {
    /**
     * The hash table data.
     */
    protected HashEntry<A> table[];

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
     * Constructs a new, empty hashtable with the specified initial
     * capacity and the specified load factor.
     * @param initialCapacity the initial number of buckets
     * @param loadFactor a number between 0.0 and 1.0, it defines
     *		the threshold for rehashing the hashtable into
     *		a bigger one.
     * @exception IllegalArgumentException If the initial capacity
     * is less than or equal to zero.
     * @exception IllegalArgumentException If the load factor is
     * less than or equal to zero.
     */
    public Hash(int initialCapacity, float loadFactor) {
	if ((initialCapacity <= 0) || (loadFactor <= 0.0)) {
	    throw new IllegalArgumentException();
	}
	this.loadFactor = loadFactor;
	table = new HashEntry<A>[initialCapacity];
	threshold = (int)(initialCapacity * loadFactor);
    }

    /**
     * Constructs a new, empty set with the specified initial
     * capacity.
     * @param initialCapacity the initial number of buckets
     */
    public Hash(int initialCapacity) {
	this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty set. A default capacity and load factor
     * is used. Note that the set will automatically grow when it gets
     * full.
     */
    public Hash() {
	this(101, 0.75f);
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
     * Returns an enumeration of the elements. Use the Enumeration methods
     * on the returned object to fetch the elements sequentially.
     * @see Hashtable#keys
     * @see Enumeration
     */
    public Enumeration<A> elements() {
	return new HashEnumerator<A>(table);
    }

   /**
     * Returns true if the collection contains an element for the elem.
     * @param elem the elem that we are looking for
     * @see Hashtable#contains
     */
    public boolean contains(A elem) {
	HashEntry<A> tab[] = table;
	int hash = ((Object)elem).hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashEntry<A> e = tab[index] ; e != null ; e = e.next) {
	    if ((e.hash == hash) && ((Object)e.elem).equals((Object)elem)) {
		return true;
	    }
	}
	return false;
    }

   /**
     * Returns element that equals the given one, ore null if there is no such element
     * @param elem the elem that we are looking for
     * @see Hashtable#contains
     */
    public A get(A elem) {
	HashEntry<A> tab[] = table;
	int hash = ((Object)elem).hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashEntry<A> e = tab[index] ; e != null ; e = e.next) {
	    if ((e.hash == hash) && ((Object)e.elem).equals((Object)elem)) {
			return e.elem;
	    }
	}
	return null;
    }

   /**
     * Returns element that equals the given one by specified comparision method
     * @param elem the elem that we are looking for
     * @see Hashtable#contains
     */
    public A get(int hash, (A)->boolean cmp) {
	HashEntry<A> tab[] = table;
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashEntry<A> e = tab[index] ; e != null ; e = e.next) {
	    if ((e.hash == hash) && cmp(e.elem)) {
			return e.elem;
	    }
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
	HashEntry<A> oldTable[] = table;

	int newCapacity = oldCapacity * 2 + 1;
	HashEntry<A> newTable[] = new HashEntry<A>[newCapacity];

	threshold = (int)(newCapacity * loadFactor);
	table = newTable;

	//System.out.println("rehash old=" + oldCapacity + ", new=" + newCapacity + ", thresh=" + threshold + ", count=" + count);

	for (int i = oldCapacity ; i-- > 0 ;) {
	    for (HashEntry<A> old = oldTable[i] ; old != null ; ) {
		HashEntry<A> e = old;
		old = old.next;

		int index = (e.hash & 0x7FFFFFFF) % newCapacity;
		e.next = newTable[index];
		newTable[index] = e;
	    }
	}
    }

    /**
     * Puts the specified element into the set
     */
    public void put(A elem) {
	// Makes sure the elem is not already in the set.
	HashEntry<A> tab[] = table;
	int hash = ((Object)elem).hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashEntry<A> e = tab[index] ; e != null ; e = e.next) {
	    if ((e.hash == hash) && ((Object)e.elem).equals((Object)elem)) {
		return;
	    }
	}

	if (count >= threshold) {
	    // Rehash the table if the threshold is exceeded
	    rehash();
	    put(elem);
	}

	// Creates the new entry.
	tab[index] = new HashEntry<A>(elem, hash, tab[index]);
	count++;
    }

    /**
     * Removes the element corresponding to the elem. Does nothing if the
     * elem is not present.
     * return true iff `elem' was in set.
     * @param elem the elem that needs to be removed
     */
    public boolean remove(A elem) {
	HashEntry<A> tab[] = table;
	int hash = ((Object)elem).hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashEntry<A> e = tab[index], prev = null ; e != null ; prev = e, e = e.next) {
	    if ((e.hash == hash) && ((Object)e.elem).equals((Object)elem)) {
		if (prev != null) {
		    prev.next = e.next;
		} else {
		    tab[index] = e.next;
		}
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
	HashEntry<A> tab[] = table;
	for (int index = tab.length; --index >= 0; )
	    tab[index] = null;
	count = 0;
    }

    /**
     * Creates a clone of the set. A shallow copy is made,
     * the keys and elements themselves are NOT cloned. This is a
     * relatively expensive operation.
     */
    public Object clone() {
	Hash<A> t = new Hash<A>(table.length, loadFactor);
	for (int i = table.length ; i-- > 0 ; ) {
	    t.table[i] = (table[i] != null) ? table[i].copy() : null;
	}
	return t;
    }

    /**
     * Converts to a rather lengthy String.
     */
    public String toString() {
	int max = size() - 1;
	StringBuffer buf = new StringBuffer();
	Enumeration<A> e = elements();
	buf.append("{");

	for (int i = 0; i <= max; i++) {
	    String s2 = ((Object)e.nextElement()).toString();
	    buf.append(s2);
	    if (i < max) {
		buf.append(", ");
	    }
	}
	buf.append("}");
	return buf.toString();
    }

/**
 * Hashtable collision list.
 */
public static class HashEntry<A extends Object> {

    A elem;
    int hash;
    HashEntry<A> next;

    HashEntry(A elem, int hash, HashEntry<A> next) {
	this.elem = elem;
	this.hash = hash;
	this.next = next;
    }

    public HashEntry<A> copy() {
	return new HashEntry<A>(elem, hash,
			    (next != null) ? next.copy() : null);
    }
}

/**
 * A set enumerator class.  This class should remain opaque
 * to the client. It will use the Enumeration interface.
 */
static class HashEnumerator<A extends Object> implements Enumeration<A>, Cloneable {
    int index;
    HashEntry<A> table[];
    HashEntry<A> entry;

    HashEnumerator(HashEntry<A> table[]) {
	this.table = table;
	this.index = table.length;
    }
	
    public boolean hasMoreElements() {
	if (entry != null) {
	    return true;
	}
	while (index-- > 0) {
	    if ((entry = table[index]) != null) {
		return true;
	    }
	}
	return false;
    }

    public A nextElement() {
	if (entry == null) {
	    while ((index-- > 0) && ((entry = table[index]) == null));
	}
	if (entry != null) {
	    HashEntry<A> e = entry;
	    entry = e.next;
	    return e.elem;
	}
	throw new NoSuchElementException("HashEnumerator");
    }
}
}

