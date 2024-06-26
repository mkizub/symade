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
package kiev.be.java15;
import syntax kiev.Syntax;

import java.io.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 232 $
 *
 */

public class ConstPool {

	/** Constant Pool Hash */
	public final Hash<CP>	poolHash = new Hash<CP>();

	/** Constant Pool Array */
	public CP[]		pool = new CP[260];

	/** Constant Pool number of constants */
	public int		hwm = 1;

	/** Constant Pool number of java constants */
	public int		java_hwm = 1;

	public void ConstPool() {
	}

	private int putCPinPool(CP newcp) {
		if( hwm >= pool.length-2 ) {
			pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
		}
		pool[hwm] = newcp;
		newcp.pos = hwm++;
		if( newcp instanceof NumberCP ) {
			Number val = ((NumberCP)newcp).value;
			if( val instanceof Long || val instanceof Double)
				pool[hwm++] = null;
		}
		return newcp.pos;
	}

	private CP getAnyCP(CP get_cp) {
		CP cp_hash = poolHash.get(get_cp);
		if( cp_hash != null ) return cp_hash;
		return get_cp;
	}

	private int addAnyCP(CP newcp) {
		return newcp.pos;
	}

	public AsciiCP getAsciiCP(String asc) {
		AsciiCP cp = (AsciiCP)getAnyCP(AsciiCP.newAsciiCP(this,asc));
		if( cp == null )
			throw new RuntimeException("Can't find AsciiCP "+asc);
		return cp;
	}
	public AsciiCP addAsciiCP(String asc) {
		return AsciiCP.newAsciiCP(this,asc);
	}

	public ClazzCP getClazzCP(String sig) {
		ClazzCP cp = (ClazzCP)getAnyCP( ClazzCP.newClazzCP(this,sig) );
		if( cp == null )
			throw new RuntimeException("Can't find ClazzCP from signature "+sig);
		return cp;
	}
	public ClazzCP addClazzCP(String sig) {
		ClazzCP cl_cp = ClazzCP.newClazzCP(this,sig);
		return cl_cp;
	}

	public FieldCP getFieldCP(String clazz_sig, String name, String sig) {
		return (FieldCP)getAnyCP( FieldCP.newFieldCP(this,clazz_sig,name,sig) );
	}
	public FieldCP addFieldCP(String clazz_sig, String name, String sig) {
		return FieldCP.newFieldCP(this,clazz_sig,name,sig);
	}

	public MethodCP getMethodCP(String clazz_sig, String name, String sig) {
		return (MethodCP)getAnyCP( MethodCP.newMethodCP(this,clazz_sig,name,sig) );
	}
	public MethodCP addMethodCP(String clazz_sig, String name, String sig) {
		return MethodCP.newMethodCP(this,clazz_sig,name,sig);
	}

	public InterfaceMethodCP getInterfaceMethodCP(String clazz_sig, String name, String sig) {
		return (InterfaceMethodCP)getAnyCP( InterfaceMethodCP.newInterfaceMethodCP(this,clazz_sig,name,sig) );
	}
	public InterfaceMethodCP addInterfaceMethodCP(String clazz_sig, String name, String sig) {
		return InterfaceMethodCP.newInterfaceMethodCP(this,clazz_sig,name,sig);
	}

	public NameTypeCP getNameTypeCP(String name, String sig) {
		return (NameTypeCP)getAnyCP( NameTypeCP.newNameTypeCP(this,name,sig) );
	}
	public NameTypeCP addNameTypeCP(String name, String sig) {
		return NameTypeCP.newNameTypeCP(this,name,sig);
	}

	public NumberCP getNumberCP(Number val) {
		return (NumberCP)getAnyCP( NumberCP.newNumberCP(this,val) );
	}
	public NumberCP addNumberCP(Number val) {
		return NumberCP.newNumberCP(this,val);
	}

	public StringCP getStringCP(String val) {
		return (StringCP)getAnyCP( StringCP.newStringCP(this,val) );
	}
	public StringCP addStringCP(String val) {
		return StringCP.newStringCP(this,val);
	}

	public void generate(JEnv jenv) {

		foreach(NumberCP cp; poolHash; cp.pos < 1) {
			if( hwm >= pool.length-2 ) pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			pool[hwm] = cp;
			cp.pos = hwm++;
			if( cp.value instanceof Long || cp.value instanceof Double ) {
				pool[hwm++] = null;
			}
		}
		foreach(StringCP cp; poolHash; cp.pos < 1) {
			if( hwm >= pool.length ) pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			pool[hwm] = cp;
			cp.pos = hwm++;
		}
		foreach(NodeCP cp; poolHash) {
			if( hwm >= pool.length ) pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			pool[hwm] = cp;
			cp.pos = hwm++;
		}
		foreach(ClazzCP cp; poolHash) {
			if( hwm >= pool.length ) pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			pool[hwm] = cp;
			cp.pos = hwm++;
		}
		foreach(NameTypeCP cp; poolHash) {
			if( hwm >= pool.length ) pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			pool[hwm] = cp;
			cp.pos = hwm++;
		}

		int len = hwm;

		len = hwm;
		for(int i=0; i < len; i++) {
			if( hwm >= pool.length-1 )
				pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			CP cp = pool[i];
			if( cp==null ) continue;
			if( cp instanceof StringCP ) {
				StringCP s_cp = (StringCP)cp;
				if( s_cp.asc.pos > 0 ) continue;
				pool[hwm] = s_cp.asc;
				s_cp.asc.pos = hwm++;
			}
			else if( cp instanceof ClazzCP ) {
				ClazzCP cl_cp = (ClazzCP)cp;
				if( cl_cp.asc.pos > 0 ) continue;
				pool[hwm] = cl_cp.asc;
				cl_cp.asc.pos = hwm++;
			}
			else if( cp instanceof NameTypeCP ) {
				NameTypeCP nt_cp = (NameTypeCP)cp;
				if( nt_cp.name_cp.pos < 0 ) {
					pool[hwm] = nt_cp.name_cp;
					nt_cp.name_cp.pos = hwm++;
				}
				if( nt_cp.type_cp.pos < 0 ) {
					pool[hwm] = nt_cp.type_cp;
					nt_cp.type_cp.pos = hwm++;
				}
			}
			else continue;
		}

		foreach (CP cp; poolHash.elements()) {
			if( hwm >= pool.length )
				pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			if( cp.pos > 0 ) continue;
			if( cp instanceof AsciiCP ) {
				pool[hwm] = cp;
				cp.pos = hwm++;
			}
		}

		java_hwm = hwm;

		for(int i=0; i < len; i++) {
			if( hwm >= pool.length-1 )
				pool = (CP[])Arrays.ensureSize(pool,pool.length*2);
			CP cp = pool[i];
			if( cp==null ) continue;
			if( cp instanceof ClazzCP ) {
				ClazzCP cl_cp = (ClazzCP)cp;
				if( cl_cp.pos < 0 ) {
					pool[hwm] = cl_cp;
					cl_cp.pos = hwm++;
				}
				if( cl_cp.asc.pos < 0 ) {
					pool[hwm] = cl_cp.asc;
					cl_cp.asc.pos = hwm++;
				}
			}
			else if( cp instanceof NameTypeCP ) {
				NameTypeCP nt_cp = (NameTypeCP)cp;
				if( nt_cp.pos < 0 ) {
					pool[hwm] = nt_cp;
					nt_cp.pos = hwm++;
				}
				if( nt_cp.type_cp.pos < 0 ) {
					pool[hwm] = nt_cp.type_cp;
					nt_cp.type_cp.pos = hwm++;
				}
				if( nt_cp.name_cp.pos < 0 ) {
					pool[hwm] = nt_cp.name_cp;
					nt_cp.name_cp.pos = hwm++;
				}
			}
			else continue;
		}

		pool[0] = null;
		for(int i=0; i < hwm; i++) {
			if( pool[i] == null ) continue;
			if( pool[i].pos != i )
				throw new RuntimeException("Missplaced CP: "+pool[i]+"\n\tpool pos="+i+", but cp.pos="+pool[i].pos);
		}
	}
}

public abstract class CP {
	public int				pos = -1;
	public final ConstPool constPool;

	public CP(ConstPool constPool) {
		this.constPool = constPool;
	}
	
	public abstract String toString();
	public abstract int hashCode();
	public abstract boolean equals(Object obj);
	public abstract int size();
}

public class AsciiCP extends CP {
	String value;
	public static AsciiCP newAsciiCP(ConstPool constPool, String value) {
		AsciiCP old = (AsciiCP)constPool.poolHash.get(value.hashCode(), fun (CP asc)->boolean {
			return (asc instanceof AsciiCP) && ((AsciiCP)asc).value.equals(value);
		});
		if( old != null ) return old;
		return new AsciiCP(constPool,value);
	}
	public AsciiCP(ConstPool constPool, String value) {
		super(constPool);
		if( value==null )
			throw new RuntimeException("Null as AsciiCP's value in constant pool");
		this.value = value;
		constPool.poolHash.put(this);
	}
	public String toString() { return "AsciiCP: \""+"#"+pos+" -> "+value.toString()+'\"'; }
	public int hashCode() { return value.hashCode(); }
	public boolean equals(Object obj) {
		if( obj instanceof AsciiCP ) {
			AsciiCP asc = (AsciiCP)obj;
			if( value.equals(asc.value) )
				return true;
		}
		return false;
	}
	public int size() { return 1+2+value.length(); }
}

public class ClazzCP extends CP {
	public String			sig;
	public AsciiCP			asc;

	public static ClazzCP newClazzCP(ConstPool constPool, String sig) {
		ClazzCP old = (ClazzCP)constPool.poolHash.get(sig.hashCode(), fun (CP cl)->boolean {
			return (cl instanceof ClazzCP) && ((ClazzCP)cl).sig.equals(sig);
		});
		if( old != null ) return old;
		if( sig.charAt(0) == 'L' );
		else if( sig.charAt(0) == '[' );
		else if( sig.charAt(0) == '&' );
		else if( sig.length() == 1 );
		else
			throw new RuntimeException("Bad kiev signature "+sig);
		return new ClazzCP(constPool,sig);
	}
	public ClazzCP(ConstPool constPool, AsciiCP asc) {
		super(constPool);
		this.asc = asc;
		if( asc.value.charAt(0) == '[' || asc.value.charAt(0) == '&' )
			sig = asc.value;
		else {
			sig = 'L'+asc.value+';';
		}
	}
	public ClazzCP(ConstPool constPool, String sig) {
		super(constPool);
		this.sig = sig;
		if( sig.charAt(0) == 'L' ) {
			StringBuffer sb = new StringBuffer(sig.length()-2);
			int i = 1; // skip 'L'/'A'
			char c;
			while( (c=sig.charAt(i++)) != ';' ) sb.append(c); // copy until ';'
			if (i < sig.length()) sb.append(sig.substring(i));
			asc = AsciiCP.newAsciiCP(constPool,sb.toString());
		} else
			asc = AsciiCP.newAsciiCP(constPool,sig);
//		System.out.println("...new ClazzCP: "+sig+" -> "+asc.value);
		constPool.poolHash.put(this);
	}
	public String toString() {
		return "Class: "+"#"+pos+" -> "+asc.value+"#"+asc.pos;
	}
	public int hashCode() {
		return sig.hashCode();
	}
	public boolean equals(Object obj) {
		if( obj instanceof ClazzCP ) {
			ClazzCP cl = (ClazzCP)obj;
			if( asc.value.equals(cl.asc.value) ) return true;
		}
		return false;
	}
	public int size() { return 1+2; }
}

public class NameTypeCP extends CP {
	AsciiCP name_cp;
	AsciiCP	type_cp;

	public static NameTypeCP newNameTypeCP(ConstPool constPool, String name,String sig) {
		NameTypeCP old = (NameTypeCP)constPool.poolHash.get(name.hashCode() * sig.hashCode(), fun (CP nt)->boolean {
			return (nt instanceof NameTypeCP)
				&& ((NameTypeCP)nt).name_cp.value.equals(name)
				&& ((NameTypeCP)nt).type_cp.value.equals(sig);
		});
		if( old != null ) return old;
		return new NameTypeCP(constPool,name,sig);
	}
	public NameTypeCP(ConstPool constPool, AsciiCP name_cp, AsciiCP type_cp) {
		super(constPool);
		this.name_cp = name_cp;
		this.type_cp = type_cp;
		constPool.poolHash.put(this);
	}
	public NameTypeCP(ConstPool constPool, String name,String sig) {
		super(constPool);
		name_cp = AsciiCP.newAsciiCP(constPool,name);
		type_cp = AsciiCP.newAsciiCP(constPool,sig);
		constPool.poolHash.put(this);
	}
	public String toString() { return "NameTypeCP: "+"#"+pos+" -> "
		+name_cp.value+"#"+name_cp.pos+", "+type_cp.value+"#"+type_cp.pos; }
	public int hashCode() { return name_cp.value.hashCode() * type_cp.value.hashCode(); }
	public boolean equals(Object obj) {
		if( obj instanceof NameTypeCP ) {
			NameTypeCP nt = (NameTypeCP)obj;
			if( name_cp.value.equals(nt.name_cp.value)
			 && type_cp.value.equals(nt.type_cp.value) )
				return true;
		}
		return false;
	}
	public int size() { return 1+2+2; }
}

public abstract class NodeCP extends CP {
	ClazzCP		clazz_cp;
	NameTypeCP	nt_cp;
	public NodeCP(ConstPool constPool) {
		super(constPool);
	}
}

public class FieldCP extends NodeCP {
	public static FieldCP newFieldCP(ConstPool constPool, String clazz_sig, String name, String sig) {
		ClazzCP clazz_cp = ClazzCP.newClazzCP(constPool,clazz_sig);
		NameTypeCP nt_cp = NameTypeCP.newNameTypeCP(constPool,name,sig);
		FieldCP old = (FieldCP)constPool.poolHash.get(clazz_cp.hashCode() * nt_cp.hashCode(), fun (CP fld)->boolean {
			return (fld instanceof FieldCP)
				&& ((FieldCP)fld).clazz_cp.equals(clazz_cp)
				&& ((FieldCP)fld).nt_cp.equals(nt_cp);
		});
		if( old != null ) return old;
		return new FieldCP(constPool,clazz_cp,nt_cp);
	}
	public FieldCP(ConstPool constPool, ClazzCP clazz_cp, NameTypeCP nt_cp) {
		super(constPool);
		this.clazz_cp = clazz_cp;
		this.nt_cp = nt_cp;
		constPool.poolHash.put(this);
	}
	public String toString() { return "FieldCP: "+"#"+pos+" -> "+clazz_cp+", "+nt_cp; }
	public int hashCode() { return clazz_cp.hashCode() * nt_cp.hashCode(); }
	public boolean equals(Object obj) {
		if( obj instanceof FieldCP ) {
			FieldCP fld = (FieldCP)obj;
			if( clazz_cp.equals(fld.clazz_cp) && nt_cp.equals(fld.nt_cp) )
				return true;
		}
		return false;
	}
	public int size() { return 1+2+2; }
}

public class MethodCP extends NodeCP {
	public static MethodCP newMethodCP(ConstPool constPool, String clazz_sig, String name, String sig) {
		ClazzCP clazz_cp = ClazzCP.newClazzCP(constPool,clazz_sig);
		NameTypeCP nt_cp = NameTypeCP.newNameTypeCP(constPool,name,sig);
		MethodCP old = (MethodCP)constPool.poolHash.get(clazz_cp.hashCode() * nt_cp.hashCode(), fun (CP fld)->boolean {
			return (fld instanceof MethodCP)
				&& ((MethodCP)fld).clazz_cp.equals(clazz_cp)
				&& ((MethodCP)fld).nt_cp.equals(nt_cp);
		});
		if( old != null ) return old;
		return new MethodCP(constPool,clazz_cp,nt_cp);
	}
	public MethodCP(ConstPool constPool, ClazzCP clazz_cp, NameTypeCP nt_cp) {
		super(constPool);
		this.clazz_cp = clazz_cp;
		this.nt_cp = nt_cp;
		constPool.poolHash.put(this);
	}
	public String toString() { return "MethodCP: "+"#"+pos+" -> "+clazz_cp+", "+nt_cp; }
	public int hashCode() { return clazz_cp.hashCode() * nt_cp.hashCode(); }
	public boolean equals(Object obj) {
		if( obj instanceof MethodCP ) {
			MethodCP fld = (MethodCP)obj;
			if( clazz_cp.equals(fld.clazz_cp) && nt_cp.equals(fld.nt_cp) )
				return true;
		}
		return false;
	}
	public int size() { return 1+2+2; }
}

public class InterfaceMethodCP extends NodeCP {
	public static InterfaceMethodCP newInterfaceMethodCP(ConstPool constPool, String clazz_sig, String name, String sig) {
		ClazzCP clazz_cp = ClazzCP.newClazzCP(constPool,clazz_sig);
		NameTypeCP nt_cp = NameTypeCP.newNameTypeCP(constPool,name,sig);
		InterfaceMethodCP old = (InterfaceMethodCP)constPool.poolHash.get(clazz_cp.hashCode() * nt_cp.hashCode(), fun (CP fld)->boolean {
			return (fld instanceof InterfaceMethodCP)
				&& ((InterfaceMethodCP)fld).clazz_cp.equals(clazz_cp)
				&& ((InterfaceMethodCP)fld).nt_cp.equals(nt_cp);
		});
		if( old != null ) return old;
		return new InterfaceMethodCP(constPool,clazz_cp,nt_cp);
	}
	public InterfaceMethodCP(ConstPool constPool, ClazzCP clazz_cp, NameTypeCP nt_cp) {
		super(constPool);
		this.clazz_cp = clazz_cp;
		this.nt_cp = nt_cp;
		constPool.poolHash.put(this);
	}
	public String toString() { return "InterfaceMethodCP: "+"#"+pos+" -> "+clazz_cp+", "+nt_cp; }
	public int hashCode() { return clazz_cp.hashCode() * nt_cp.hashCode(); }
	public boolean equals(Object obj) {
		if( obj instanceof InterfaceMethodCP ) {
			InterfaceMethodCP fld = (InterfaceMethodCP)obj;
			if( clazz_cp.equals(fld.clazz_cp) && nt_cp.equals(fld.nt_cp) )
				return true;
		}
		return false;
	}
	public int size() { return 1+2+2; }
}

public class NumberCP extends CP {
	Number					value;

	public static NumberCP newNumberCP(ConstPool constPool, Number value) {
		NumberCP old = (NumberCP)constPool.poolHash.get(value.hashCode(), fun (CP cl)->boolean {
			return (cl instanceof NumberCP) && ((NumberCP)cl).value.equals(value);
		});
		if( old != null ) return old;
		return new NumberCP(constPool,value);
	}
	public NumberCP(ConstPool constPool, Number value) {
		super(constPool);
		this.value = value;
		constPool.poolHash.put(this);
	}
	public String toString() { return "Number("+value.getClass()+"): "+"#"+pos+" -> "+value; }
	public int hashCode() { return value.hashCode(); }
	public boolean equals(Object obj) {
		if( obj instanceof NumberCP ) {
			NumberCP num = (NumberCP)obj;
			if( value.equals(num.value) )
				return true;
		}
		return false;
	}
	public int size() {
		if( value instanceof Long || value instanceof Double )
			return 1+8;
		else
			return 1+4;
	}
}

public class StringCP extends CP {
	public AsciiCP			asc;

	public static StringCP newStringCP(ConstPool constPool, String value) {
		StringCP old = (StringCP)constPool.poolHash.get(value.hashCode(), fun (CP cl)->boolean {
			return (cl instanceof StringCP)
				&& ((StringCP)cl).asc.value.equals(value);
		});
		if( old != null ) return old;
		return new StringCP(constPool,value);
	}
	public StringCP(ConstPool constPool, AsciiCP asc) {
		super(constPool);
		this.asc = asc;
		constPool.poolHash.put(this);
	}
	public StringCP(ConstPool constPool, String value) {
		super(constPool);
		asc = AsciiCP.newAsciiCP(constPool,value);
		constPool.poolHash.put(this);
	}
	public String toString() { return "String: "+"#"+pos+" -> "+asc.value; }
	public int hashCode() { return asc.value.hashCode(); }
	public boolean equals(Object obj) {
		if( obj instanceof StringCP ) {
			StringCP str = (StringCP)obj;
			if( asc.value.equals(str.asc.value) )
				return true;
		}
		return false;
	}
	public int size() { return 1+2; }
}
