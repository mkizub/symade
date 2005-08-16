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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/Enumeration.java,v 1.2.4.2 1999/05/29 21:03:10 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2.4.2 $
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
public interface Enumeration<A>
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



