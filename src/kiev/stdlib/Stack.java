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
 * @version $Revision$
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
