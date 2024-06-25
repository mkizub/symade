package kiev.fmt.common;

import kiev.fmt.Drawable;

public class ChangeRootException extends RuntimeException {
	public final Drawable dr;
	public ChangeRootException(Drawable dr) {
		this.dr = dr;
	}
}
