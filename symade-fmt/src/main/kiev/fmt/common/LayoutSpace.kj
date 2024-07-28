package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

import kiev.fmt.SpaceKind;

public final class LayoutSpace extends Draw_Size implements Cloneable, Serializable {
	private static final long serialVersionUID = 5736472699626215338L;

	public static final LayoutSpace[] emptyArray = new LayoutSpace[0];
	
	public String		name;
	public SpaceKind	kind;
	public boolean		eat;

	public LayoutSpace setEat() {
		LayoutSpace ls = null;
		try {
			ls = (LayoutSpace)this.clone();
		} catch (CloneNotSupportedException e) {}
		ls.eat = true;
		return ls;
	}

	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}

