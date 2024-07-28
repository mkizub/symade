package kiev.dump;

import javax.xml.namespace.QName;

public final class AcceptInfo {
	public final boolean as_field;
	public final QName   to_field;
	
	public AcceptInfo(boolean as_field, QName to_field) {
		this.as_field = as_field;
		this.to_field = to_field;
	}
	public AcceptInfo(boolean as_field, String to_field) {
		this.as_field = as_field;
		this.to_field = new QName(to_field);
	}
}

