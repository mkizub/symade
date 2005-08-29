/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.

 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.stdlib;

import java.util.StringTokenizer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * @author Maxim Kizub
 * @version $Revision: 1.2.2.1.2.1 $
 *
 */

public interface TypeInfoInterface {
	public TypeInfo getTypeInfoField();
}

public class TypeInfo {

	public static final TypeInfo[] emptyArray = new TypeInfo[0];

	private Class			type;
	private int				hash;
	public	TypeInfo[]		typeargs;
	public  String			name;
	public  String			classname;
	public  TypeInfo[][]	related;

	/* Use java.util.Hashtable, since our own kiev.stdlib.Hashtable uses TypeInfo! */
	public static java.util.Hashtable typehash = new java.util.Hashtable(128);

	protected TypeInfo() {
	}

	protected TypeInfo(TypeInfo[] ta, String nm, String clnm) {
		if( ta.length == 0 )
			typeargs = emptyArray;
		else
			typeargs = ta;
		name = nm.intern();
		classname = clnm.intern();
		hash = name.hashCode();
		typehash.put(nm,this);
	}

	protected TypeInfo(String nm, String clnm) {
		this(emptyArray,nm,clnm);
	}

	public int hashCode() {
		return hash;
	}

	protected Class getClazz() {
		try {
			return type=Class.forName(classname);
		} catch( Throwable e ) {
			System.err.println("Error in Class.forName for "+classname);
			e.printStackTrace();
			return null;
//			throw new RuntimeException(e.getMessage());
		}
	}

	public static TypeInfo newTypeInfo(String t) {
		try {
			t = t.intern();
			TypeInfo ti = (TypeInfo)typehash.get(t);
			if( ti != null ) return ti;
			if( t.charAt(0) == '[' ) {
				return new TypeInfo(t,t);
			}
			int indx = t.indexOf('<');
			if( indx < 0 ) {
				return new TypeInfo(t,t);
			}
			String targs = t.substring(indx+1,t.length()-1);
			String t1 = t.substring(0,indx).intern();
			TypeInfo[] args = new TypeInfo[0];
			// Split targs into args array
			char c;
			int len = targs.length();
			int start_pos = 0;
			for(int i=0; i < len; i++) {
				c = targs.charAt(i);
				if( c == ',' ) {
					TypeInfo ti = newTypeInfo(targs.substring(start_pos,i));
					ti.fill_type_info_from_reflection();
					args = (TypeInfo[])Arrays.append(args,ti);
					start_pos = i+1;
				}
				else if( c == '<' ) {
					// Scan until matched '>'
					int depth = 1;
					for(i++; depth > 0; i++) {
						c = targs.charAt(i);
						if( c == '>' ) depth--;
						else if( c == '<' ) depth++;
					}
					TypeInfo ti = newTypeInfo(targs.substring(start_pos,i));
					ti.fill_type_info_from_reflection();
					args = (TypeInfo[])Arrays.append(args,ti);
					start_pos = i;
					if (i < len && targs.charAt(i) == ',') start_pos++;
				}
			}
			if( start_pos < len ) {
				TypeInfo ti = newTypeInfo(targs.substring(start_pos,len));
				ti.fill_type_info_from_reflection();
				args = (TypeInfo[])Arrays.append(args,ti);
			}
			try {
				Class ti_class = Class.forName(t1+"$__ti__");
				Class[] tic_args = new Class[args.length];
				for (int i=0; i < args.length; i++)
					tic_args[i] = TypeInfo.class;
				Constructor ti_constr = ti_class.getConstructor(tic_args);
				TypeInfo ti = (TypeInfo)ti_constr.newInstance(args);
				ti.name = t;
				ti.hash = ti.name.hashCode();
				ti.fill_type_info_from_reflection();
				ti.classname = t1.intern();
				return ti;
			} catch (Exception e) {
				System.err.println("Error: "+e);
				throw new AssertionFailedException("Class '"+t1+"$__ti__"+"' not found");
				//return new TypeInfo(args,t,t1);
			}
		} catch( Throwable e ) {
			e.printStackTrace();
			return null;
		}
	}

	public static TypeInfo newTypeInfo(String t1, TypeInfo[] args) {
		StringBuffer sb = new StringBuffer();
		sb.append(t1).append('<');
		for(int i=0; i < args.length; i++) {
			if( i > 0 ) sb.append(',');
			sb.append(args[i].name);
		}
		sb.append('>');
		String t = sb.toString().intern();
		TypeInfo ti = (TypeInfo)typehash.get(t);
		if( ti != null ) return ti;
		return new TypeInfo(args,t,t1);
	}

	private void fill_type_info_from_reflection() {
		if (classname != null)
			return;
//		System.err.println("fill_type_info_from_reflection() for "+this);
		Class this_class = this.getClass();
		if (this_class == TypeInfo.class) {
//			System.err.println("fill_type_info_from_reflection() - simply TypeInfo");
			typeargs = emptyArray;
			return;
		}
		// count number of parents
		int n = 0;
		Class p=this_class;
		for(; p != null && p != TypeInfo.class; p = p.getSuperclass())
			n++;
//		System.err.println("fill_type_info_from_reflection() - found "+n+" parents");
		if (p == null)
			throw new RuntimeException("Incorrect typeinfo "+this_class);
		related = new TypeInfo[n][];

		fill_type_info_from_reflection(this_class,n-1);

		typeargs = related[n-1];
	}

	private void fill_type_info_from_reflection(Class cl, int n) {
		if (n > 0) fill_type_info_from_reflection(cl.getSuperclass(),n-1);
//		System.err.println("fill_type_info_from_reflection("+cl+","+n+")");
		Field[] fields = cl.getDeclaredFields();
		int rn = 0;
		for (int i=0; i < fields.length; i++) {
			Field f = fields[i];
			if (f.getName().startsWith("$typeinfo"))
				rn++;
		}

//		System.err.println("fill_type_info_from_reflection("+cl+","+n+") - found "+rn+" $typeinfo fields");
		related[n] = rn > 0 ? new TypeInfo[rn] : emptyArray;

		rn = 0;
		for (int i=0; i < fields.length; i++) {
			Field f = fields[i];
			if (f.getName().startsWith("$typeinfo")) {
//				System.err.println("fill_type_info_from_reflection("+cl+","+n+") - added "+f.getName()+"="+f.get(this));
				related[n][rn] = (TypeInfo)f.get(this);
				rn++;
			}
		}
	}

	public String toString() {
		return name;
	}

	public boolean $instanceof(Object obj) {
		if( obj == null ) return false;
		if( type == null ) {
			if( !getClazz().isInstance(obj) ) return false;
		}
		else if( !type.isInstance(obj) ) return false;
		if( obj instanceof TypeInfoInterface ) {
			TypeInfo ti = ((TypeInfoInterface)obj).getTypeInfoField();
			return this.$ti_instanceof_ti(ti);
		} else {
			return typeargs == emptyArray;
		}
	}

	public boolean $instanceof(Object obj, TypeInfo oti) {
		if( obj == null ) return false;
		if( type == null ) {
			if( !getClazz().isInstance(obj) ) return false;
		}
		else if( !type.isInstance(obj) ) return false;
		if( typeargs.length != oti.typeargs.length ) return false;
		for(int i=0; i < typeargs.length; i++) {
			if( !typeargs[i].$ti_instanceof_ti(oti.typeargs[i]) ) return false;
		}
		return true;
	}

	public boolean $ti_instanceof_ti(TypeInfo oti) {
		if( name != oti.name ) {
			if( type == null ) getClazz();
			if( oti.type == null ) oti.getClazz();
			if( !type.isAssignableFrom(oti.type) ) return false;
			if( typeargs.length != oti.typeargs.length ) return false;
			for(int i=0; i < typeargs.length; i++) {
				if( !typeargs[i].$ti_instanceof_ti(oti.typeargs[i]) ) return false;
			}
		}
		return true;
	}

	public Object newInstance() {
		if( type == null )
			return getClazz().newInstance();
		else
			return type.newInstance();
	}

	public Object newInstance(int arg) {
		return typeargs[arg].newInstance();
	}

	public Object newArray(int i) {
		if( type == null )
			return java.lang.reflect.Array.newInstance(getClazz(),i);
		else
			return java.lang.reflect.Array.newInstance(type,i);
	}

	public Object newArray(int[] i) {
		if( type == null )
			return java.lang.reflect.Array.newInstance(getClazz(),i);
		else
			return java.lang.reflect.Array.newInstance(type,i);
	}

	public Object newArray(int arg, int i) {
		return typeargs[arg].newArray(i);
	}

	public Object newArray(int arg, int[] i) {
		return typeargs[arg].newArray(i);
	}

}

