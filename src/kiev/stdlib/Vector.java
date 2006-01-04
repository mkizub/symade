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
public class Vector<A> implements Cloneable
{

	public A		data[];

	public int		count;

	public int		capacityIncrement;

	public Vector(int initialCapacity, int capacityIncrement) {
		this.data = new A[initialCapacity];
		this.capacityIncrement = capacityIncrement;
	}

	public Vector(int initialCapacity) {
		this(initialCapacity, 0);
	}

	public Vector() {
		this(10,0);
	}

	public synchronized void setFrom(Vector<A> v) {
		data = v.data;
		count = v.count;
	}

	public synchronized void copyInto(A anArray[]) {
		System.arraycopy(data,0,anArray,0,count);
	}

	public synchronized A[] copyIntoArray()
		alias toArray
		alias operator(210,fy,$cast)
	{
		A[] anArray = new A[count];
		System.arraycopy(data,0,anArray,0,count);
		return anArray;
	}

	public synchronized void trimToSize() {
		int oldCapacity = data.length;
		if (count < oldCapacity) {
			A oldData[] = data;
			data = new A[count];
			System.arraycopy(oldData, 0, data, 0, count);
		}
	}

	public synchronized void ensureCapacity(int minCapacity) {
		int oldCapacity = data.length;
		if (minCapacity > oldCapacity) {
			A oldData[] = data;
			int newCapacity = (capacityIncrement > 0) ?
			(oldCapacity + capacityIncrement) : (oldCapacity * 2);
			if (newCapacity < minCapacity) {
				newCapacity = minCapacity;
			}
			data = new A[newCapacity];
			System.arraycopy(oldData, 0, data, 0, count);
		}
	}

	public final synchronized void setSize(int newSize)
		alias set$length
	{
		if (newSize > count) {
			ensureCapacity(newSize);
		} else {
			for (int i = newSize; i < count; i++) {
				data[i] = null;
			}
		}
		count = newSize;
	}

	public int capacity() {
		return data.length;
	}

	public int size()
		alias length
		alias get$length
	{
		return count;
	}

	public boolean isEmpty() {
		return count == 0;
	}

	public synchronized Enumeration<A> elements() {
		return new VectorEnumerator<A>();
	}
    
	public boolean contains(A elem) {
		return indexOf(elem, 0) >= 0;
	}

	public int indexOf(A elem) {
		return indexOf(elem, 0);
	}

	public synchronized int indexOf(A elem, int index) {
		for (int i = index; i < count; i++) {
			if (elem.equals(data[i])) {
				return i;
			}
		}
		return -1;
	}

	public int lastIndexOf(A elem) {
		return lastIndexOf(elem, count-1);
	}

	public final synchronized int lastIndexOf(A elem, int index) {
		for (int i = index ; i >= 0 ; i--) {
			if (elem.equals(data[i])) {
				return i;
			}
		}
		return -1;
	}

	public A elementAt(int index)
		alias at
		alias get
		alias operator(210,xfy,[])
	{
		if( index >= count )
			throw new ArrayIndexOutOfBoundsException(index+" >= "+count);
		return data[index];
	}

	public final A firstElement() {
		if (count == 0) {
			throw new NoSuchElementException();
		}
		return data[0];
	}

	public A lastElement() {
		if (count == 0) {
			throw new NoSuchElementException();
		}
		return data[count - 1];
	}

	public A setElementAt(int index, A obj)
		alias set
		alias operator(210,lfy,[])
	{
		if (index >= count)
			throw new ArrayIndexOutOfBoundsException(index+" >= "+count);
		data[index] = obj;
		return obj;
	}

	public synchronized Vector<A> removeElementAt(int index)
		alias remove
	{
		if (index >= count)
			throw new ArrayIndexOutOfBoundsException(index+" >= "+count);
		int j = count - index - 1;
		if (j > 0) {
			System.arraycopy(data, index+1, data, index, j);
		}
		count--;
		data[count] = null;
		return this;
	}

	public synchronized Vector<A> insertElementAt(int index, A obj)
		alias insert
	{
		if (index > count)
			throw new ArrayIndexOutOfBoundsException(index+" > "+count);
		ensureCapacity(count + 1);
		System.arraycopy(data, index, data, index+1, count-index);
		data[index] = obj;
		count++;
		return this;
	}

	public synchronized Vector<A> addElement(A obj)
		alias append
	{
		ensureCapacity(count + 1);
		data[count++] = obj;
		return this;
	}

	public synchronized boolean removeElement(A obj) {
		int i = indexOf(obj);
		if (i >= 0) {
			removeElementAt(i);
			return true;
		}
		return false;
	}

	public final synchronized void removeAllElements()
		alias cleanup
	{
		for (int i = 0; i < count; i++) {
			data[i] = null;
		}
		count = 0;
	}

	public synchronized Object clone() {
		try { 
			Vector<A> v = (Vector<A>)super.clone();
			v.data = (A[])data.clone();
			return v;
		} catch (CloneNotSupportedException e) { 
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

	public synchronized String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");

		for(int i=0; i < count; i++) {
			buf.append(data[i]);
			if (i < count) buf.append(',');
		}
		buf.append("]");
		return buf.toString();
	}

	public class VectorEnumerator implements Enumeration {
		int current;

		public VectorEnumerator() {
			current = 0;
		}

		public boolean hasMoreElements() {
			return current < count;
		}

		public A nextElement() {
			if ( current < count ) {
				return Vector.this[current++];
			}
			throw new NoSuchElementException(Integer.toString(current));
		}
	}

}

