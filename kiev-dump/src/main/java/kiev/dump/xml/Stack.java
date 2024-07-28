package kiev.dump.xml;

import java.util.Iterator;
import java.util.ListIterator;

public class Stack<E> extends java.util.Stack<E> {

	static class ReversIterator<E> implements Iterator<E> {
		private final ListIterator<E> lit;
		ReversIterator(ListIterator<E> lit) {
			this.lit = lit;
		}
		public boolean hasNext() {
			return lit.hasPrevious();
		}
		public E next() {
			return lit.previous();
		}
		public void remove() {
			lit.remove();
		}
		
	};

	@Override
	public Iterator<E> iterator() {
		return new ReversIterator<E>(this.listIterator(this.size()));
	}
}
