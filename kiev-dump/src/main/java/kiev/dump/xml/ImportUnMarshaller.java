package kiev.dump.xml;

import java.util.Hashtable;

import javax.xml.namespace.QName;

import kiev.Kiev;
import kiev.dump.AcceptInfo;
import kiev.dump.UnMarshaller;
import kiev.dump.UnMarshallingContext;
import kiev.dump.AttributeSet;
import kiev.stdlib.Arrays;
import kiev.vlang.ConstExpr;

public class ImportUnMarshaller implements UnMarshaller {
	
	final class ObjData {
		ImportTypeAlias info;
		Object          obj;
		ObjData(ImportTypeAlias info, Object obj) {
			this.info = info;
			this.obj = obj;
		}
	}

	private final Hashtable<QName,ImportTypeAlias>	type_aliases = new Hashtable<QName,ImportTypeAlias>();
	private ImportTypeAlias default_type_alias;
	
	public ImportUnMarshaller addTypeAlias(ImportTypeAlias info) {
		QName qname = info.qname;
		if (qname != null)
			type_aliases.put(qname, info);
		else
			default_type_alias = info;
		return this;
	}


	private java.lang.reflect.Field getFieldFromQName(Object node, QName qname) {
		String uri = qname.getNamespaceURI();
		if (uri != null && uri.length() > 0)
			return null;
		String name = qname.getLocalPart();
		try {
			return node.getClass().getField(name);
		} catch (Exception e) {}
		return null;
	}
	
    public boolean canUnMarshal(QName qname, AttributeSet attrs, UnMarshallingContext context) {
		if (type_aliases.get(qname) != null)
			return true;
		if (default_type_alias != null)
			return true;
		return false;
	}

	public AcceptInfo canAccept(Object self, QName qname, AttributeSet attrs, UnMarshallingContext context) {
		ObjData obj_dat = (ObjData)self;
		if (obj_dat.info.field_aliases.get(qname) != null)
			return new AcceptInfo(true, qname);
		if (obj_dat.info.implicit_field_aliases.get(qname) != null)
			return new AcceptInfo(false, obj_dat.info.implicit_field_aliases.get(qname));
		if (getFieldFromQName(obj_dat.obj, qname) != null)
			return new AcceptInfo(true, qname);
		return null;
	}
	
	public Object exit(Object self, UnMarshallingContext context) {
		return ((ObjData)self).obj;
	}

    public Object create(QName qname, AttributeSet attributes, UnMarshallingContext context) {
		ImportTypeAlias info = type_aliases.get(qname);
		if (info == null)
			info = default_type_alias;
		Object obj = info.newInstance(qname, attributes);
		ObjData obj_dat = new ObjData(info, obj);
		try {
			addAttributes(obj_dat, attributes);
		} catch (Exception e) {
			throw new RuntimeException("Cannot save value", e);
		}
		return obj_dat;
	}
	
	public void accept(Object self, QName qname, Object target, UnMarshallingContext context) {
		ObjData obj_dat = (ObjData)self;
		String fname = obj_dat.info.field_aliases.get(qname);
		if (fname == null)
			fname = obj_dat.info.implicit_field_aliases.get(qname);
		if (fname == null)
			fname = qname.getLocalPart();
		if (target instanceof ObjData)
			target = ((ObjData)target).obj;
		try {
			writeField(obj_dat.obj, fname, target);
		} catch (Exception e) {
			throw new RuntimeException("Cannot save value", e);
		}
	}
	
	private void writeField(Object self, String fname, Object data) throws Exception {
		Class self_clazz = self.getClass();
		java.lang.reflect.Field fld = null;
		java.lang.reflect.Method setter = null;
		java.lang.reflect.Method getter = null;
		String sname = "set"+Character.toUpperCase(fname.charAt(0))+fname.substring(1);
		String gname = "get"+Character.toUpperCase(fname.charAt(0))+fname.substring(1);
		for (java.lang.reflect.Method m : self_clazz.getMethods()) {
			if (setter == null && m.getName().equals(sname) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 1)
				setter = m;
			if (getter == null && m.getName().equals(gname) && m.getReturnType() != Void.TYPE && m.getParameterTypes().length == 0)
				getter = m;
			if (setter != null && getter != null)
				break;
		}
		if (setter == null) {
			for (java.lang.reflect.Field f : self_clazz.getFields()) {
				if (f.getName().equals(fname)) {
					fld = f;
					break;
				}
			}
		}
		Class fld_clazz = null;
		if (setter != null)
			fld_clazz = setter.getParameterTypes()[0];
		else if (fld != null)
			fld_clazz = fld.getType();
		else {
			Kiev.reportWarning("Attribute '"+fname+"' has no such setter or field in "+self_clazz);
			return;
		}
		Class clazz = fld_clazz.isArray() ? fld_clazz.getComponentType() : fld_clazz;
		
		if (data != null && data instanceof String) {
			String value = (String)data;
			if (clazz == String.class)
				;
			else if (clazz == Boolean.TYPE || clazz == Boolean.class)
				data = Boolean.valueOf(value.trim());
			else if (clazz == Integer.TYPE || clazz == Integer.class)
				data = Integer.valueOf((int)parseLong(value));
			else if (clazz == Byte.TYPE || clazz == Byte.class)
				data = Byte.valueOf((byte)parseLong(value));
			else if (clazz == Short.TYPE || clazz == Short.class)
				data = Short.valueOf((short)parseLong(value));
			else if (clazz == Long.TYPE || clazz == Long.class)
				data = Long.valueOf(parseLong(value));
			else if (clazz == Float.TYPE || clazz == Float.class)
				data = Float.valueOf(value.trim());
			else if (clazz == Double.TYPE || clazz == Double.class)
				data = Double.valueOf(value.trim());
			else if (clazz == Character.TYPE || clazz == Character.class)
				data = Character.valueOf(value.trim().charAt(0));
			else if (Enum.class.isAssignableFrom(clazz))
				data = clazz.getMethod("valueOf",String.class).invoke(null,value.trim());
			else {
				Kiev.reportWarning("Attribute '"+fname+"' of "+self.getClass()+" uses unsupported "+clazz);
				return;
			}
		}
		writeField(self, fld_clazz, getter, setter, fld, data);
	}

	private void writeField(Object self, Class fld_clazz, java.lang.reflect.Method getter, java.lang.reflect.Method setter, java.lang.reflect.Field fld, Object data) throws Exception {
		if (fld != null) {
			if (fld_clazz.isArray()) {
				Object arr = fld.get(self);
				if (arr == null)
					arr = java.lang.reflect.Array.newInstance(fld.getType().getComponentType(),0);
				arr = Arrays.append(arr, data);
				fld.set(self, arr);
			} else {
				fld.set(self,data);
			}
		} else {
			if (fld_clazz.isArray()) {
				Object arr = getter.invoke(self, new Object[0]);
				if (arr == null)
					arr = java.lang.reflect.Array.newInstance(fld_clazz.getComponentType(),0);
				arr = Arrays.append(arr, data);
				setter.invoke(self, arr);
			} else {
				setter.invoke(self, data);
			}
		}
	}

	private void addAttributes(ObjData obj_dat, AttributeSet attributes) throws Exception {
		int n = attributes.getCount();
		for (int i=0; i < n; i++)
			writeAttribute(obj_dat, new QName(attributes.getURI(i), attributes.getName(i)), attributes.getValue(i));
	}

	//@SuppressWarnings("unchecked")
	private void writeAttribute(ObjData obj_dat, QName qname, String value) throws Exception {
		String fname = obj_dat.info.field_aliases.get(qname);
		if (fname == null)
			fname = qname.getLocalPart();
		if (fname.length() == 0)
			return;	// ignore this value
		writeField(obj_dat.obj, fname, value);
	}

	private static long parseLong(String text) {
		text = text.trim();
		int radix;
		if( text.startsWith("0x") || text.startsWith("0X") ) { text = text.substring(2); radix = 16; }
		else if( text.startsWith("0") && text.length() > 1 ) { text = text.substring(1); radix = 8; }
		else { radix = 10; }
		if (text.charAt(text.length()-1) == 'L' || text.charAt(text.length()-1) == 'l') {
			text = text.substring(0,text.length()-1);
			if (text.length() == 0)
				return 0L; // 0L 
		}
		long l = ConstExpr.parseLong(text,radix);
		return l;
	}
}

