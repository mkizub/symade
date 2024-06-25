package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

public final class Draw_Font implements Serializable {
	private static final long serialVersionUID = -8603205874484295675L;

	public String			font_name;
	transient
	public Object			font_object;

	public Draw_Font(String font_name) {
		if (font_name != null)
			this.font_name = font_name.intern();
	}
	
	Object readResolve() throws ObjectStreamException {
		if (this.font_name != null) this.font_name = this.font_name.intern();
		return this;
	}
}

