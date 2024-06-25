package kiev.dump.xml;

import java.util.Hashtable;

import javax.xml.namespace.QName;

import kiev.dump.AttributeSet;
import kiev.stdlib.TypeInfo;

public class ImportTypeAlias {
	public final QName                    qname;
	public final TypeInfo                 ti;
	public final Hashtable<QName,String>  field_aliases;
	public final Hashtable<QName,String>  implicit_field_aliases;
	
	public ImportTypeAlias() {
		this((QName)null, (TypeInfo)null);
	}
	public ImportTypeAlias(String name, Class clazz) {
		this(new QName(name), TypeInfo.makeTypeInfo(clazz, null));
	}
	public ImportTypeAlias(QName qname, TypeInfo ti) {
		this.qname = qname;
		this.ti = ti;
		this.field_aliases = new Hashtable<QName,String>();
		this.implicit_field_aliases = new Hashtable<QName,String>();
	}

	public ImportTypeAlias addFieldAlias(QName qn, String fname) {
		field_aliases.put(qn, fname);
		return this;
	}
	public ImportTypeAlias addFieldAlias(String name, String fname) {
		field_aliases.put(new QName(name), fname);
		return this;
	}
	public ImportTypeAlias addImplicitFieldAlias(QName qn, String fname) {
		implicit_field_aliases.put(qn, fname);
		return this;
	}
	public ImportTypeAlias addImplicitFieldAlias(String name, String fname) {
		implicit_field_aliases.put(new QName(name), fname);
		return this;
	}
	
	public Object newInstance(QName qname, AttributeSet attributes) {
		return ti.newInstance();
	}
}

