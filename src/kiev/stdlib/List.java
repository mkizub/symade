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

/** a class for immutable lists
 * @see		ListBuffer
 * @author Martin Odersky
 * @author Maxim Kizub
 * @version $Revision$
 */

public class List<A>
{

	public case Nil;
	public case Cons<A>(A head, List<A> tail);

/** return the length of the list
 */
	public int length() {
		int count = 0;
		while( this != Nil ) {
			count++;
			this = tail();
		}
		return count;
	}

/** the first element of the list
 */
	public A head() {
		switch (this) {
		case Nil:
			throw new MatchError("Nil.head");
		case Cons(A x,_):
			return x;
		}
	}

/** the rest without the first element of the list
 */
	public List<A> tail() {
		switch (this) {
		case Nil:
			throw new MatchError("Nil.tail");
		case Cons(_, List<A> xs):
			return xs;
		}
	}

/** the list consisting of the first `n' elements of this list
 */
	public List<A> take(int n) {
		if (n == 0)
			return Nil;
		else
			switch (this) {
			case Nil:
				return this;
			case Cons(A x, List<A> xs):
				return new Cons<A>(x, xs.take(n-1));
			}
	}

/** the list without its first `n' elements
 */
	public List<A> drop(int n) {
		for( ; n > 0; n-- ) {
			switch (this) {
			case Nil:
				return this;
			case Cons(_, List<A> xs):
				this = xs;
			}
		}
		return this;
	}

/** split lists into two. The first part of the results is a list
 *  which consists of the first `n' elements of this list. The second part 
 *  is a list containing all remaining elements.
 *  @law    split(n, xs) = Pair.Pair(take(n, xs), drop(n, xs))
 */
	public Pair<List<A>, List<A>> split(int n) {
		if (n == 0)
			return new Pair<List<A>, List<A>>(Nil, this);
		else
			switch (this) {
			case Nil:
				return new Pair<List<A>, List<A>>(Nil, Nil);
			case Cons(A x, List<A> xs):
				{
				Pair<List<A>, List<A>> p = xs.split(n-1);
				p.fst = new Cons<A>(x, p.fst);
				return p;
				}
			}
   }

/** the element at position `n' in this list
 */
	public A at(int n) {
		for(;; n--) {
			switch (this) {
			case Nil:
				throw new MatchError("Nil.at");
			case Cons(A x, List<A> xs):
				if (n == 0) return x;
				else this = xs;
			}
		}
	}

/** the last element of this list
 */
	public A last() {
		return at(length() - 1);
	}

/** all elements except the last one
 */
	public List<A> init() {
		return take(length() - 1);
	}
  
/** the elements in reverse order
 */
	public List<A> reverse() {
		return reverse(this);
	}

	private List<A> reverse(List<A> xs) {
		List<A> ys = Nil;
		while (true) {
			switch (xs) {
			case Nil:
				return ys;
			case Cons(A x, List<A> xs1):
				ys = new Cons<A>(x, ys);
				xs = xs1;
			}
		}
	}
    
//	public A[] toArray()
////		alias fy operator $cast
//	{
//		A[] arr = new A[length()];
//		for(int i=0; this != List.Nil; i++) {
//			arr[i] = head();
//			this = tail();
//		}
//		return arr;
//	}

/** return the result of appending element `y' to this list as a new list
 */
	public List<A> concat(A y) {
		return concat(new Cons<A>(y, Nil));
	}

/** return the result of appending list `ys' to this list as a new list
 */
	public List<A> concat(List<A> ys) {
		switch (this) {
		case Nil:
			return ys;
		case Cons(A x, List<A> xs):
			return new Cons<A>(x, xs.concat(ys));
		}
	}

/** concatenate all element lists of `xss'
 */ 
	public static <A> List<A> concatenate(List<List<A>> xss) {
		switch (xss) {
		case Nil:
			return Nil;
		case Cons(List<A> xs, List<List<A>> xss1):
			return xs.concat(concatenate(xss1));
		}
	}

/** apply `f' to each element of this list
 */
	public <B> void forall((A)->B f) {
		List<A> xs = this;
		while (true) {
			switch (xs) {
			case Nil:
				return;
			case Cons(A x, List<A> xs1):
				f(x);
				xs = xs1;
			}
		}
	}

/** apply `f' to each element of this list and collect the results in a
 *  new list 
 */
	public <B> List<B> map((A)->B f) {
		switch (this) {
		case Nil:
			return Nil;
		case Cons(A x, List<A> xs):
			return new Cons<A>(f(x), xs.map(f));
		}
	}

/** apply `f' to each element of this list and concatenate the (list-valued)
 *  results in a new list 
 */
	public <B> List<B> bind((A)->List<B> f) {
		switch (this) {
		case Nil:
			return Nil;
		case Cons(A x, List<A> xs):
			return f(x).concat(xs.bind(f));
		}
	}
    
/** all elements for which predicate`p' is true
 */
	public List<A> filter((A)->boolean p) {
		switch (this) {
		case Nil:
			return this;
		case Cons(A x, List<A> xs):
		{
			List<A> t = xs.filter(p);
			if (p(x)) {
				if (t == xs) return this;
				return new Cons<A>(x, t);
			}
			else
				return t;
		}
		}
	}

/** fold left:
 *
 *  @law List.cons(x1,...,xN).foldl(OP, z) = (...(z OP x1) OP ... ) OP xN
 *
 */
	public <B> B foldl((B,A)->B f, B z) {
		List<A> xs = this;
		while (true) {
			switch (xs) {
			case Nil:
				return z;
			case Cons(A x, List<A> xs1):
				z = f(z, x);
				xs = xs1;
			}
		}
	}

/** fold right:
 *
 *  @law List.cons(x1,...,xN).foldr(OP, z) = x1 OP (... OP (xN OP z)...)
 *
 */
	public <B> B foldr((A,B)->B f, B z) {
		switch (this) {
		case Nil:
			return z;
		case Cons(A x, List<A> xs):
			return f(x, xs.foldr(f, z));
		}
	}

/** does this list contain element `y'?
 */
	public boolean contains(A y) {
		for(;;) {
			switch (this) {
			case Nil:
				return false;
			case Cons(A x, List<A> xs):
				if( x.equals(y) )
					return true;
				this = xs;
			}
		}
	}

/** this list without the elements in `ys'. Each element is removed
 *  at most once.
 */
	public List<A> diff(List<A> ys) {
		switch (ys) {
		case Nil:
			return this;
		case Cons(A y, List<A> ys1):
			return this.diff(y).diff(ys1);
		}
	}

/** this list without the element`y'. The element `y' is removed
 *  at most once.
 */
	public List<A> diff(A y) {
		switch (this) {
		case Nil:
			return this;
		case Cons(A x, List<A> xs):
			if (((Object)x).equals((Object)y)) {
				return xs;
			} else {
				List<A> xs1 = xs.diff(y);
				if (xs1 == xs) return this;
				else return new Cons<A>(x, xs1);
			}
		}
	}

	public int hashCode() {
		int hash = 0;
		for(;;) {
			switch (this) {
			case Nil:
				return hash;
			case Cons(A x, List<A> xs):
				hash = hash * 41 + x.hashCode();
				this = xs;
			}
		}
	}

	public boolean equals(Object other) {
		if (other instanceof List<A>) {
			List<A> o = (List<A>)other;
			for(;;) {
				switch (this) {
				case Nil:
					return o == Nil;
				case Cons(A x, List<A> xs):
					switch (o) {
					case Nil:
						return false;
					case Cons(A y, List<A> ys):
						if( !y.equals(x) )
							return false;
						this = xs;
						o = ys;
					}
				}
			}
		} else {
			return false;
		}
	}

/** a String representation of this list
 */
	public String toString() {
		return "List(" + elementsToString(", ") + ")";
	}

/** a String representation of the elements in this list with `sep' used
 *  as a separator
 */
	public String elementsToString(String sep) {
		StringBuffer sb = new StringBuffer();
		switch (this) {
		case Nil:
			break;
		case Cons(A x, List<A> xs):
			sb.append(x);
			this = xs;
		}
	for_loop:
		for(;;) {
			switch (this) {
			case Nil:
				break for_loop;
			case Cons(A x, List<A> xs):
				sb.append(sep).append(x);
				this = xs;
			}
		}
		return sb.toString();
	}

/** return the elements of this list as an enumeration
 */
	public Enumeration<A> elements() {
		return new ListEnumerator<A>(this);
	}

/** zip two lists into a list of pairs
 */
	public List<Pair<A,A>> zip(List<A> ys) {
		if( this == Nil || ys == Nil )
			return (List< Pair<A,A> >)Nil;
		// BUG if the type is explicit, not infered
		//return new Cons<Pair<A,A>>(new Pair<A,A>(head(), ys.head()), tail().zip(ys.tail()));
		return (List< Pair<A,A> >) new Cons(new Pair<A,A>(head(), ys.head()), tail().zip(ys.tail()));
	}

/** the elements of array `elems' as a list
 */
	public static List<A> fromArray(A[] elems)
		alias lfy operator new
	{
		List<A> l = Nil;
		int i = elems.length;
		while (i > 0) {
			i--;
			l = new Cons<A>(elems[i], l);
		}
		return l;
	}

/** copy this list into array `elems'. return `elems' as a result.
 */
	public A[] copy(A[] elems) {
		return copy(elems, 0);
	}

/** copy this list into array `elems' staring from given `offset'.
 *  return `elems' as a result.
 */
	public A[] copy(A[] elems, int offset) {
		for(;; offset++) {
			switch (this) {
			case Nil:
				return elems;
			case Cons(A x, List<A> xs):
				elems[offset] = x;
				this = xs;
			}
		}
	}

/** list's consisting of given (0-10) elements
 */
	public static <A> List<A> newList()
	{
		return Nil;
	}

	public static <A> List<A> newList(A hd)
	{
		return new Cons<A>(hd,Nil);
	}

	public static <A> List<A> newList(A hd, A ... va_args)
	{
		List<A> nl = Nil;
		for(int i=va_args.length-1; i >= 0; i--) {
			A h = (A)va_args[i];
			nl = new Cons<A>(h,nl);
		}
		return new Cons<A>(hd,nl);
	}

static class ListEnumerator<A> implements Enumeration<A>, Cloneable {

	private List<A> lst;
	
	ListEnumerator(List<A> lst) {
		this.lst = lst;
	}
	
	public boolean hasMoreElements() {
		return lst != List.Nil;
	}

	public A nextElement() {
		switch (lst) {
		case List.Cons(A x, List<A> xs):
			lst = xs;
			return x;
		case List.Nil:
			throw new NoSuchElementException();
		}
	}
}

}

