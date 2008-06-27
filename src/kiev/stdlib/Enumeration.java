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
 * @version $Revision$
 *
 */

/**
 * The Enumeration interface specifies a set of methods that may be used
 * to enumerate, or count through, a set of values. The enumeration is
 * consumed by use; its values may only be counted once.<p>
 *
 * For example, to print all elements of a Vector v:
 * <pre>
 *	for (Enumeration e = v.elements() ; e.hasMoreElements() ;) {
 *	    System.out.println(e.nextElement());
 *	}
 * </pre>
 * @see Vector
 * @see Hashtable
 */
public interface Enumeration<+A>
{
    /**
     * Returns true if the enumeration contains more elements; false
     * if its empty.
     */
    boolean hasMoreElements();

    /**
     * Returns the next element of the enumeration. Calls to this
     * method will enumerate successive elements.
     * @exception NoSuchElementException If no more elements exist.
     */
    A nextElement();
}



