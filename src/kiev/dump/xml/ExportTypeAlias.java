package kiev.dump.xml;

import java.util.Vector;

import javax.xml.namespace.QName;


public class ExportTypeAlias {
	public final QName                     qname;
	public final Class                     clazz;
	public final Vector<ExportFieldAlias>  field_aliases;
	
	public ExportTypeAlias(String name, Class clazz) {
		this(new QName(name), clazz);
	}
	public ExportTypeAlias(QName qname, Class clazz) {
		this.qname = qname;
		this.clazz = clazz;
		this.field_aliases = new Vector<ExportFieldAlias>();
	}

	public ExportTypeAlias add(ExportFieldAlias fa) {
		field_aliases.add(fa);
		return this;
	}
}

