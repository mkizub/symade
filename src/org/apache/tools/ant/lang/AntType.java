/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package org.apache.tools.ant.lang;
import syntax kiev.Syntax;

import java.io.File;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.TaskContainer;

final class NameAndType {
	final String name;
	final AntType atype;
	NameAndType(String name, AntType atype) {
		this.name = name;
		this.atype = atype;
	}
}

public abstract class AntType {
	public abstract NameAndType[] getAttributes();
	public abstract NameAndType[] getElements();
	public abstract boolean allowsText();
	public abstract boolean isTaskContainer();
}

public final class AntTypeEmpty extends AntType {
	public NameAndType[] getAttributes() { return new NameAndType[0]; }
	public NameAndType[] getElements() { return new NameAndType[0]; }
	public String toString() { return "ant type any"; }
	public boolean allowsText() { false }
	public boolean isTaskContainer() { false }
}

public final class AntTypeOfClass extends AntType {
	public final String classname;
	public final Class clazz;
	
	public AntTypeOfClass(String classname) {
		this.classname = classname;
		Class clazz = null;
		try {
			clazz = Class.forName(classname);
		} catch (Exception e) { e.printStackTrace(); }
		this.clazz = clazz;
	}

	public AntTypeOfClass(Class clazz) {
		this.classname = clazz.getName();
		this.clazz = clazz;
	}

	public String toString() { return "ant type of class "+classname; }

	public boolean allowsText() {
		try {
			Class clazz = Class.forName(classname);
			return (clazz.getMethod("addText", String.class) != null);
		} catch (Exception e) { return false; }
	}

	public boolean isTaskContainer() {
		try {
			return TaskContainer.class.isAssignableFrom(clazz);
		} catch (Exception e) { return false; }
	}

	public NameAndType[] getAttributes() {
		Vector<NameAndType> attrs = new Vector<NameAndType>();
		try {
			Class clazz = Class.forName(classname);
			next_setter:
			foreach (java.lang.reflect.Method m; clazz.getMethods(); m.getName().startsWith("set") && m.getReturnType() == Void.TYPE) {
				try {
					// additional checks and attribute name
					if (m.getParameterTypes().length != 1)
						continue;
					Class param = m.getParameterTypes()[0];
					// check the setter argument class
					if (!checkArgumentClass(param))
						continue;
					String aname = m.getName().substring(3);
					if (aname.equals("Location") && param == Location.class)
						continue;
					if (aname.equals("TaskType") && param == String.class)
						continue;
					if (aname.length() == 0 || !Character.isUpperCase(aname.charAt(0)))
						continue;
					aname = Character.toLowerCase(aname.charAt(0))+aname.substring(1);
					attrs.append(new NameAndType(aname, new AntTypeOfClass(param)));
				} catch (Exception e) {}
			}
		} catch (Exception e) { return new NameAndType[0]; }
		return attrs.toArray();
	}

	public NameAndType[] getElements() {
		Vector<NameAndType> elems = new Vector<NameAndType>();
		try {
			Class clazz = Class.forName(classname);
			foreach (java.lang.reflect.Method m; clazz.getMethods()) {
				String mname = m.getName();
				if (mname.startsWith("create") && m.getParameterTypes().length == 0 && m.getReturnType() != Void.TYPE) {
					String aname = mname.substring(6);
					if (aname.length() == 0 || !Character.isUpperCase(aname.charAt(0)))
						continue;
					aname = Character.toLowerCase(aname.charAt(0))+aname.substring(1);
					elems.append(new NameAndType(aname, new AntTypeOfClass(m.getReturnType())));
					continue;
				}
				if ((mname.startsWith("add") || mname.startsWith("addConfigured")) && m.getParameterTypes().length == 1 && m.getReturnType() == Void.TYPE) {
					String aname = mname.substring(3);
					if (aname.startsWith("Configured"))
						aname = aname.substring(10);
					if (aname.length() == 0) {
						elems.append(new NameAndType("", new AntTypeOfClass(m.getParameterTypes()[0])));
						continue;
					}
					if (!Character.isUpperCase(aname.charAt(0)))
						continue;
					aname = Character.toLowerCase(aname.charAt(0))+aname.substring(1);
					elems.append(new NameAndType(aname, new AntTypeOfClass(m.getParameterTypes()[0])));
					continue;
				}
			}
		} catch (Exception e) { return new NameAndType[0]; }
		return elems.toArray();
	}

	private static boolean checkArgumentClass(Class cls) {
		if (cls == String.class) return true;
		if (cls == Boolean.class || cls == Boolean.TYPE) return true;
		if (cls == Character.class || cls == Character.TYPE) return true;
		if (cls == Integer.class || cls == Integer.TYPE) return true;
		if (cls == Byte.class || cls == Byte.TYPE) return true;
		if (cls == Short.class || cls == Short.TYPE) return true;
		if (cls == Long.class || cls == Long.TYPE) return true;
		if (cls == Float.class || cls == Float.TYPE) return true;
		if (cls == Double.class || cls == Double.TYPE) return true;
		if (cls == Class.class) return true;
		if (cls == File.class) return true;
		if (EnumeratedAttribute.class.isAssignableFrom(cls)) return true;
		if (Enum.class.isAssignableFrom(cls)) return true;
		try { // First try with Project.
			if (cls.getConstructor(new Class[] {Project.class, String.class}) != null)
			return true;
		} catch (Exception e) {}
		try { // OK, try without.
			if (cls.getConstructor(new Class[] {String.class}) != null)
			return true;
		} catch (Exception e) {}
		return false;
	}
}

public final class AntTypeOfMacro extends AntType {
	public final AntMacroDef mdef;

	public AntTypeOfMacro(AntMacroDef mdef) {
		this.mdef = mdef;
	}

	public String toString() { return "ant type of macro "+mdef; }

	public NameAndType[] getAttributes() {
		Vector<NameAndType> vect = new Vector<NameAndType>();
		foreach (AntMacroAttribute a; mdef.members; a.sname != null && a.sname != "")
			vect.append(new NameAndType(a.sname, new AntTypeOfClass(String.class)));
		return vect.toArray();
	}

	public NameAndType[] getElements() {
		Vector<NameAndType> vect = new Vector<NameAndType>();
		foreach (AntMacroElement e; mdef.members; e.sname != null && e.sname != "") {
			if (!e.isImplicit()) {
				vect.append(new NameAndType(e.sname, new AntTypeOfClass(Object.class))); return vect.toArray();
			} else {
				AntMacroData md = e.data;
				if (md != null && md.parent() instanceof AntNode) {
					AntNode an = (AntNode)md.parent();
					AntType at = an.getAntType();
					foreach (NameAndType nat; at.getElements())
						vect.append(new NameAndType(nat.name,nat.atype));
				}
			}
		}
		return vect.toArray();
	}

	public boolean allowsText() {
		foreach (AntMacroText e; mdef.members)
			return true;
		return false;
	}

	public boolean isTaskContainer() { false }

}

