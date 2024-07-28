package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

public final class Draw_Style implements Serializable {
	private static final long serialVersionUID = 5270122649222007270L;

	public String			name;
	public String[]			fallback;
	public Draw_Color		color;
	public Draw_Font		font;
	public LayoutSpace[]	spaces_before;
	public LayoutSpace[]	spaces_after;

	public Draw_Style() {
		this.color = new Draw_Color("color-black", 0);
		this.font = new Draw_Font("Dialog-PLAIN-12");
		this.spaces_before = LayoutSpace.emptyArray;
		this.spaces_after = LayoutSpace.emptyArray;
	}
	
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		if (this.fallback != null) {
			for (int i=0; i < this.fallback.length; i++)
				this.fallback[i] = this.fallback[i].intern();
		}
		return this;
	}
}

