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

/**
 * @author Maxim Kizub
 * @version $Revision: 213 $
 *
 */

@unerasable
public abstract class Dictionary<A,B extends Object>
{
    /**
     * Returns the number of elements contained within the Dictionary. 
     */
    abstract public int size();

    /**
     * Returns true if the Dictionary contains no elements.
     */
    abstract public boolean isEmpty();

    /**
     * Returns an enumeration of the Dictionary's keys.
     * @see Dictionary#elements
     * @see Enumeration
     */
    abstract public Enumeration<A> keys();

    /**
     * Returns an enumeration of the elements. Use the Enumeration methods 
     * on the returned object to fetch the elements sequentially.
     * @see Dictionary#keys
     * @see Enumeration
     */
    abstract public Enumeration<B> elements();

    /**
     * Gets the object associated with the specified key in the Dictionary.
     * @param key the key in the hash table
     * @returns the element for the key, or null if the key
     * 		is not defined in the hash table.
     * @see Dictionary#put
     */
    abstract public B get(A key);

    /**
     * Puts the specified element into the Dictionary, using the specified
     * key.  The element may be retrieved by doing a get() with the same 
     * key.  The key and the element cannot be null.
     * @param key the specified hashtable key
     * @param value the specified element 
     * @return the old value of the key, or null if it did not have one.
     * @exception NullPointerException If the value of the specified
     * element is null.
     * @see Dictionary#get
     */
    abstract public B put(A key, B value);

    /**
     * Removes the element corresponding to the key. Does nothing if the
     * key is not present.
     * @param key the key that needs to be removed
     * @return the value of key, or null if the key was not found.
     */
    abstract public B remove(A key);
}

