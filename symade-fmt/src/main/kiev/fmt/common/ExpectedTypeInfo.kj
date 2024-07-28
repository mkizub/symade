package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

import kiev.stdlib.TypeInfo;

public final class ExpectedTypeInfo implements Serializable {
	private static final long serialVersionUID = -3603386177345584762L;
	public static final ExpectedTypeInfo[] emptyArray = new ExpectedTypeInfo[0];

	public String				title;
	public String				signature;
	transient
	private TypeInfo			typeinfo;
	public ExpectedTypeInfo[]	subtypes;

	Object readResolve() throws ObjectStreamException {
		if (this.title != null) this.title = this.title.intern();
		return this;
	}
	public TypeInfo getTypeInfo() {
		if (this.typeinfo == null && this.signature != null)
			this.typeinfo = TypeInfo.newTypeInfo(this.signature);
		return this.typeinfo;
	}
}

