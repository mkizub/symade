package kiev.dump.xml;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import kiev.vlang.Constants;
import kiev.vlang.DNode;
import kiev.vlang.Env;
import kiev.vlang.Language;

public class DumpUnMarshallingContext extends AUnMarshallingContext {
	
	public File file;
	public DNode pkg;
	public String tdname;
	public boolean is_interface_only;
	public boolean is_import;
	
	public final Vector<DelayedTypeInfo> delayed_types = new Vector<DelayedTypeInfo>();
	public final Hashtable<String,Language> languages = new Hashtable<String,Language>();

	public Language getLanguage(String uri) {
		Language lng = languages.get(uri);
		if (lng != null)
			return lng;
		int p = uri.indexOf("?class=");
		if (p < 0)
			return null;
		String cl_name = uri.substring(p+7);
		try {
			Class lng_class = Class.forName(cl_name);
			lng = (Language)lng_class.getField(Constants.nameInstance).get(null);
			languages.put(uri, lng);
			return lng;
		} catch (Exception e) {
			return null;
		}
	}
	
	public DumpUnMarshallingContext(Env env) {
		super(env);
		this.unmarshallers.push(new ANodeUnMarshaller());
	}
}

