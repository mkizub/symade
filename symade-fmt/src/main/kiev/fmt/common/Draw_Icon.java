package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

public final class Draw_Icon implements Serializable {
	private static final long serialVersionUID = -970225328131025562L;

	public String			icon_name;
	transient
	public Object			icon_object;

	public Draw_Icon(String icon_name) {
		if (icon_name != null)
			this.icon_name = icon_name.intern();
	}
	
	Object readResolve() throws ObjectStreamException {
		if (this.icon_name != null) this.icon_name = this.icon_name.intern();
		return this;
	}
}

