package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

public final class Draw_Color implements Serializable {
	private static final long serialVersionUID = -1158924530238324507L;

	public String			color_name;
	public int              rgb_color;
	transient
	public Object			color_object;

	public Draw_Color(String color_name, int rgb_color) {
		if (color_name != null)
			this.color_name = color_name.intern();
		this.rgb_color = rgb_color;
	}
	
	Object readResolve() throws ObjectStreamException {
		if (this.color_name != null) this.color_name = this.color_name.intern();
		return this;
	}
}

