package kiev.dump.bin;

import kiev.vlang.Env;

/**
 * Base class for binary data elements 
 */
public abstract class Elem implements Comparable<Object> {
	// element id
	public int					id;
	// optional element flags
	public int					flags;
	// optional comment of this element
	public CommentElem	comment;
	// start address
	public int					saddr;
	// end address
	public int					eaddr;
	
	public Elem(int id) {
		this.id = id;
	}

	public Elem(int id, int saddr) {
		this.id = id;
		this.saddr = saddr;
	}

	public int compareTo(Object el) {
		return this.id - ((Elem)el).id;
	}
	
	public boolean isWritten() {
		return saddr > 0;
	}
	public boolean isRead() {
		return eaddr > 0;
	}

	public void build(Env env) {}
}
