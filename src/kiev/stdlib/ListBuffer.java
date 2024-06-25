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
 * This Class is a growable buffer for lists, modelled after class
 * java.lang.StringBuffer. It is mainly used
 * to create Lists.
 *
 * Note that the method toList() does not create a copy of the
 * internal buffer.  Instead the buffer is marked as shared. Any
 * further changes to the buffer will cause a copy to be made. <p>
 * @author Martin Odersky
 * @author Maxim Kizub
 * @version $Revision: 213 $
 */

import kiev.stdlib.List.*;

public final class ListBuffer<A>
{

	private List<A> elems;
	private Cons<A> last;

	private int count;
	private boolean shared;

	/**
	 * Constructs an empty List buffer.
	 */
	public ListBuffer() {
		elems = Nil;
		last = null;
		count = 0;
		shared = false;
	}

	/**
	 * Returns the length (character count) of the buffer.
	 */
	public int length() {
		return count;
	}

	/**
	 * Copies list and sets last.
	 */
	private List<A> copy(List<A> xs) {
		switch (xs) {
		case Nil:
			last = null;
			return Nil;
		case Cons(A y, List<A> ys):
			if (ys == Nil) {
				last = (Cons<A>)xs;
				return new Cons<A>(y, Nil);
			} else {
				return new Cons<A>(y, copy(ys));
			}
		}
	}

	private void copyWhenShared() {
		if (shared) {
			elems = copy(elems);
			shared = false;
		}
	}

/** truncates buffer to a maximum of `n' elements
 */
	public synchronized void setLength(int n) {
		copyWhenShared();
		if (n < 0) {
			throw new IndexOutOfBoundsException();
		}
		else if (n == 0) {
			last = null;
			elems = Nil;
			count = 0;
		}
		else if (n < count) {
			last = (Cons<A>)elems;
			for (int i = 1; i < n; i++)
				last = (Cons<A>)last.tail;
			last.tail = Nil;
			count = n;
		}
	}

/** removes fist `n' elements in List buffer
 */
	public synchronized void remove(int n) {
		if (n < 0) {
			throw new IndexOutOfBoundsException();
		}
		else if (n < count) {
			for (int i = 0; i < n; i++)
				elems = elems.tail();
			count = count - n;
		}
		else {
			elems = Nil;
			last = null;
			count = 0;
		}
	}

	public synchronized ListBuffer<A> append(A elem) {
		copyWhenShared();
		Cons<A> newlast = new Cons<A>(elem, Nil);
		if (last == null) elems = newlast;
		else last.tail = newlast;
		last = newlast;
		count++;
		return this;
	}

	/**
	 * Prepends an element to the beginning of this buffer.
	 * @param elem	the element to be appended
	 * @return 	the ListBuffer itself, NOT a new one.
	 */
	public synchronized ListBuffer<A> prepend(A elem) {
		Cons<A> newhead = new Cons<A>(elem, elems);
		elems = newhead;
		if (last == null) last = newhead;
		count++;
		return this;
	}

	/**
	 * Inserts an element into this buffer.
	 * @param offset	the offset at which to insert, 0 <= offset <= length
	 * @param elem	the element to insert
	 * @return 		the ListBuffer itself, NOT a new one.
	 * @exception	IndexOutOfBoundsException If the offset is invalid.
	 */
	public synchronized ListBuffer<A> insert(int offset, A elem) {
		if (offset < 0 || offset > count) {
			throw new IndexOutOfBoundsException();
		}
		else if (offset == 0) {
			prepend(elem);
		}
		else {
			copyWhenShared();
			Cons<A> c = (Cons<A>)elems;
			for (int i = 1; i < offset; i++)
				c = (Cons<A>)c.tail;
			Cons<A> newelem = new Cons<A>(elem, c.tail);
			c.tail = newelem;
			if (newelem.tail == Nil) last = newelem;
			count++;
		}
		return this;
	}

	/**
	 * returns an element in the buffer.
	 * @param offset	the offset of the element to return
	 * @exception	IndexOutOfBoundsException If the offset is invalid.
	 */
	public A getAt(int offset) {
		if (offset < 0 || offset >= count) {
			throw new IndexOutOfBoundsException();
		} else {
			Cons<A> c = (Cons<A>)elems;
			for (int i = 0; i < offset; i++) {
				c = (Cons<A>)c.tail;
			}
			return c.head;
		}
	}

	/**
	 * updates an element in the buffer.
	 * @param offset	the offset at which to update
	 * @param elem	the new element
	 * @return 		the ListBuffer itself, NOT a new one.
	 * @exception	IndexOutOfBoundsException If the offset is invalid.
	 */
	public synchronized ListBuffer<A> setAt(int offset, A elem) {
		if (offset < 0 || offset >= count) {
			throw new IndexOutOfBoundsException();
		} else {
			copyWhenShared();
			Cons<A> c = (Cons<A>)elems;
			for (int i = 0; i < offset; i++) {
				c = (Cons<A>)c.tail;
			}
			c.head = elem;
		}
		return this;
	}

	public Enumeration<A> elements() {
		return elems.elements();
	}

	/**
	 * Converts to a List representing the data in the buffer.
	 */
	public List<A> toList() {
		shared = true;
		return elems;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		foreach (A e; elems) {
			sb.append(e).append(',');
		}
		sb.append('}');
		return sb.toString();
	}
}
