package kiev.fmt.common;

import java.io.ObjectStreamException;

public class Draw_ParNoIndentIfNext extends Draw_ParOption {
	private static final long serialVersionUID = 5017224636595444819L;

	public String[] names;
	Object readResolve() throws ObjectStreamException {
		if (this.names != null) {
			for (int i=0; i < this.names.length; i++) {
				String s = this.names[i];
				if (s == null)
					this.names[i] = "";
				else
					this.names[i] = s.intern();
			}
		}
		return this;
	}
}

