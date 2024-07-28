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
public class Stack<A> extends Vector<A> implements Cloneable
{

	public Stack(int initialCapacity, int capacityIncrement) {
		super(initialCapacity,capacityIncrement);
	}

	public Stack(int initialCapacity) {
		super(initialCapacity, 0);
	}

	public Stack() {
		super(10,0);
	}

	public void push(A obj) {
		addElement(obj);
	}

	public A pop() {
		if (count==0)
			throw new NoSuchElementException("pop on empty stack");
		A obj = data[--count];
		data[count] = null;
		return obj;
	}

	public A peek() {
		if (count==0)
			throw new NoSuchElementException("peek on empty stack");
		return data[count-1];
	}

	public synchronized Enumeration<A> elements() {
		return new StackEnumerator();
	}
    
	public class StackEnumerator implements Enumeration {
		int current;

		public StackEnumerator() {
			current = size();
		}

		public boolean hasMoreElements() {
			return current > 0;
		}

		public A nextElement() {
			if ( current > 0 ) {
				return Stack.this[--current];
			}
			throw new NoSuchElementException();
		}
	}
}
