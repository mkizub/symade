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

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@unerasable
public class Hashtable<A,B extends Object> extends Dictionary<A,B> implements Cloneable
{
    /**
     * The hash table data.
     */
    protected HashtableEntry<A,B> table[];

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

    public Hashtable(int initialCapacity, float loadFactor) {
	if ((initialCapacity <= 0) || (loadFactor <= 0.0)) {
	    throw new IllegalArgumentException("initialCapacity="+initialCapacity+" loadFactor="+loadFactor);
	}
	this.loadFactor = loadFactor;
	table = new HashtableEntry<A,B>[initialCapacity];
	threshold = (int)(initialCapacity * loadFactor);
    }

    /**
     * Constructs a new, empty hashtable with the specified initial
     * capacity.
     * @param initialCapacity the initial number of buckets
     */
    public Hashtable(int initialCapacity) {
	this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty hashtable. A default capacity and load factor
     * is used. Note that the hashtable will automatically grow when it gets
     * full.
     */
    public Hashtable() {
	this(101, 0.75f);
    }

    /**
     * Returns the number of elements contained in the hashtable.
     */
    public int size() {
	return count;
    }

    /**
     * Returns true if the hashtable contains no elements.
     */
    public boolean isEmpty() {
	return count == 0;
    }

    /**
     * Returns an enumeration of the hashtable's keys.
     * @see Hashtable#elements
     * @see Enumeration
     */
    public synchronized Enumeration<A> keys() {
	return new KeyEnumerator<A,B>(table);
    }

    /**
     * Returns an enumeration of the elements. Use the Enumeration methods
     * on the returned object to fetch the elements sequentially.
     * @see Hashtable#keys
     * @see Enumeration
     */
    public synchronized Enumeration<B> elements() {
	return new ValueEnumerator<A,B>(table);
    }

    /**
     * Returns true if the specified object is an element of the hashtable.
     * This operation is more expensive than the containsKey() method.
     * @param value the value that we are looking for
     * @exception NullPointerException If the value being searched
     * for is equal to null.
     * @see Hashtable#containsKey
     */
    public synchronized boolean contains(B value) {
	HashtableEntry<A,B> tab[] = table;
	for (int i = tab.length ; i-- > 0 ;) {
	    for (HashtableEntry<A,B> e = tab[i] ; e != null ; e = e.next) {
		if (e.value.equals(value)) {
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * Returns true if the collection contains an element for the key.
     * @param key the key that we are looking for
     * @see Hashtable#contains
     */
    public synchronized boolean containsKey(A key) {
	HashtableEntry<A,B> tab[] = table;
	int hash = key.hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashtableEntry<A,B> e = tab[index] ; e != null ; e = e.next) {
	    if ((e.hash == hash) && e.key.equals(key)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Gets the object associated with the specified key in the
     * hashtable.
     * @param key the specified key
     * @return the element for the key or null if the key
     * is not defined in the hash table.
     * @see Hashtable#put
     */
    public synchronized B get(A key) {
	HashtableEntry<A,B> tab[] = table;
	int hash = key.hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashtableEntry<A,B> e = tab[index] ; e != null ; e = e.next) {
	    if ((e.hash == hash) && e.key.equals(key)) {
		return e.value;
	    }
	}
	return null;
    }

    /**
     * Rehashes the content of the table into a bigger table.
     * This method is called automatically when the hashtable's
     * size exceeds the threshold.
     */
    protected void rehash() {
	int oldCapacity = table.length;
	HashtableEntry<A,B> oldTable[] = table;

	int newCapacity = oldCapacity * 2 + 1;
	HashtableEntry<A,B> newTable[] = new HashtableEntry<A,B>[newCapacity];

	threshold = (int)(newCapacity * loadFactor);
	table = newTable;

	//System.out.println("rehash old=" + oldCapacity + ", new=" + newCapacity + ", thresh=" + threshold + ", count=" + count);

	for (int i = oldCapacity ; i-- > 0 ;) {
	    for (HashtableEntry<A,B> old = oldTable[i] ; old != null ; ) {
		HashtableEntry<A,B> e = old;
		old = old.next;

		int index = (e.hash & 0x7FFFFFFF) % newCapacity;
		e.next = newTable[index];
		newTable[index] = e;
	    }
	}
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
    public synchronized B put(A key, B value) {
	// Makes sure the key is not already in the hashtable.
	HashtableEntry<A,B> tab[] = table;
	int hash = key.hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashtableEntry<A,B> e = tab[index] ; e != null ; e = e.next) {
	    if ((e.hash == hash) && e.key.equals(key)) {
		B result = e.value;
		e.value = value;
		return result;
	    }
	}

	if (count >= threshold) {
	    // Rehash the table if the threshold is exceeded
	    rehash();
	    return put(key, value);
	}

	// Creates the new entry.
	tab[index] = new HashtableEntry<A,B>(key, value, hash, tab[index]);
	count++;
	return null;
    }

    /**
     * Removes the element corresponding to the key. Does nothing if the
     * key is not present.
     * @param key the key that needs to be removed
     * @return the value of key, or null if the key was not found.
     */
    public synchronized B remove(A key) {
	HashtableEntry<A,B> tab[] = table;
	int hash = key.hashCode();
	int index = (hash & 0x7FFFFFFF) % tab.length;
	for (HashtableEntry<A,B> e = tab[index], prev = null ; e != null ; prev = e, e = e.next) {
	    if ((e.hash == hash) && e.key.equals(key)) {
		if (prev != null) {
		    prev.next = e.next;
		} else {
		    tab[index] = e.next;
		}
		count--;
		return e.value;
	    }
	}
	return null;
    }

    /**
     * Clears the hash table so that it has no more elements in it.
     */
    public synchronized void clear() {
	HashtableEntry<A,B> tab[] = table;
	for (int index = tab.length; --index >= 0; )
	    tab[index] = null;
	count = 0;
    }

    /**
     * Creates a clone of the hashtable. A shallow copy is made,
     * the keys and elements themselves are NOT cloned. This is a
     * relatively expensive operation.
     */
    public synchronized Object clone() {
	Hashtable<A,B> t = new Hashtable<A,B>(table.length, loadFactor);
	for (int i = table.length ; i-- > 0 ; ) {
	    t.table[i] = (table[i] != null) ? table[i].copy() : null;
	}
	return t;
    }

    /**
     * Converts to a rather lengthy String.
     */
    public synchronized String toString() {
	int max = size() - 1;
	StringBuffer buf = new StringBuffer();
	Enumeration<A> k = keys();
	Enumeration<B> e = elements();
	buf.append("{");

	for (int i = 0; i <= max; i++) {
	    buf.append(k.nextElement()).append("=").append(e.nextElement());
	    if (i < max) {
		buf.append(", ");
	    }
	}
	buf.append("}");
	return buf.toString();
    }

public static class HashtableEntry<A,B> {

    A key;
    B value;
    int hash;
    HashtableEntry<A,B> next;

    public HashtableEntry(A key, B value, int hash, HashtableEntry<A,B> next) {
	this.key = key;
	this.value = value;
	this.hash = hash;
	this.next = next;
    }

    public HashtableEntry<A,B> copy() {
	return new HashtableEntry<A,B>(key, value, hash,
				  (next != null) ? next.copy() : null);
    }
}

/**
 * A hashtable enumerator class.  This class should remain opaque
 * to the client. It will use the Enumeration interface.
 */
static class KeyEnumerator<A,B> implements Enumeration<A> {
    int index;
    HashtableEntry<A,B> table[];
    HashtableEntry<A,B> entry;

    KeyEnumerator(HashtableEntry<A,B> table[]) {
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
	    HashtableEntry<A,B> e = entry;
	    entry = e.next;
	    return e.key;
	}
	throw new NoSuchElementException("HashtableEnumerator");
    }
		
}

/**
 * A hashtable enumerator class.  This class should remain opaque
 * to the client. It will use the Enumeration interface.
 *  @author  Martin Odersky
 *  @version 1.0
 *  @since pizza 1.0alpha8 96/11/22
 */
static class ValueEnumerator<A,B> implements Enumeration<B>, Cloneable {
    int index;
    HashtableEntry<A,B> table[];
    HashtableEntry<A,B> entry;

    ValueEnumerator(HashtableEntry<A,B> table[]) {
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

    public B nextElement() {
	if (entry == null) {
	    while ((index-- > 0) && ((entry = table[index]) == null));
	}
	if (entry != null) {
	    HashtableEntry<A,B> e = entry;
	    entry = e.next;
	    return e.value;
	}
	throw new NoSuchElementException("HashtableEnumerator");
    }
}

}


