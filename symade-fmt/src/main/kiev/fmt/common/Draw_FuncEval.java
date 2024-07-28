package kiev.fmt.common;

import java.io.ObjectStreamException;

public final class Draw_FuncEval extends Draw_SyntaxFunc {
	private static final long serialVersionUID = 420690446481034625L;

	public String				act;

	Object readResolve() throws ObjectStreamException {
		super.readResolve();
		if (this.act != null) this.act = this.act.intern();
		return this;
	}
}

