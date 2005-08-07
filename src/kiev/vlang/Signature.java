/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Main;
import kiev.stdlib.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Signature.java,v 1.3.2.1.2.1 1999/02/15 21:45:14 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.1 $
 *
 */

public class Signature {

	KString		sig;

	private Signature(KString sig) {
		this.sig = sig;
	}

	public static KString from(BaseStruct clazz, Type[] fargs, Type[] args, Type ret) {
		KStringBuffer ksb = new KStringBuffer();
		if( ret != null ) {
			// Closure or method.
			if( clazz == MethodType.tpMethodClazz ) ;// Method
			else if( clazz.instanceOf(Type.tpClosureClazz) ) {
				ksb.append('&'); // Closure
				if( clazz != Type.tpClosureClazz )
					ksb.append(clazz.name.signature());
			}
			if (fargs.length > 0) {
				ksb.append('<');
				for(int i=0; i < fargs.length; i++)
					ksb.append(fargs[i].signature);
				ksb.append('>');
			}
			ksb.append('(');
				if(args!=null && args.length > 0) {
					for(int i=0; i < args.length; i++)
						ksb.append(args[i].signature);
				}
			ksb.append(')');
			ksb.append(ret.signature);
		} else {
			// Normal class
			if( clazz !=null && clazz.type !=null && clazz.type.isArray() && args != null && args.length > 0 ) {
				ksb.append('[');
				ksb.append(args[0].signature);
			} else {
				if( clazz !=null && clazz.type!=null /*&& clazz.generated_from == null*/ && (clazz.type.args == null || clazz.type.args.length==0) ) {
					ksb.append(clazz.type.signature);
				} else {
					if(  clazz !=null )
						ksb.append(clazz.name.signature());
					// And it's arguments
					if( args!=null && args.length > 0 ) {
						ksb.append('<');
						for(int i=0; i < args.length; i++) {
							ksb.append(args[i].signature);
						}
						ksb.append('>');
					}
				}
			}
		}
		return ksb.toKString();
	}

	public static KString fromToClazzCP(BaseStruct clazz,Type[] args, boolean full) {
		KStringBuffer ksb = new KStringBuffer(128);
		ksb.append(clazz.name.bytecode_name);
		if( args != null && args.length > 0 ) {
			ksb.append('<');
			for(int i=0; i < args.length; i++) {
				ksb.append(args[i].signature);
				if( full && args[i].isArgument() ) {
					ksb.append('<');
					ksb.append(args[i].clazz.super_type.signature);
					ksb.append('>');
				}
			}
			ksb.append('>');
		}
		return ksb.toKString();
	}

	public String toString() { return sig.toString(); }

	public int hashCode() { return sig.hashCode(); }

	public boolean equals(Object o) {
		return ( o instanceof Signature && ((Signature)o).sig.equals(sig) );
	}

	//public Type getType() {
	//	return getType(new KString.KStringScanner(sig));
	//}

	public static Type getType(KString.KStringScanner sc) {
		BaseStruct clazz;
		Type[] args = null;
		Type ret = null;

		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.nextChar();

		// Check for primitive types
		switch(ch) {
		case 'V':		return Type.tpVoid;
		case 'Z':		return Type.tpBoolean;
		case 'C':		return Type.tpChar;
		case 'B':		return Type.tpByte;
		case 'S':		return Type.tpShort;
		case 'I':		return Type.tpInt;
		case 'J':		return Type.tpLong;
		case 'F':		return Type.tpFloat;
		case 'D':		return Type.tpDouble;
		case 'R':		return Type.tpRule;
		}

		// Check if this signature is a method signature
		if( ch == '(' || ch == '&' || ch == '<' ) {
			// Method signature
			Type[] fargs = Type.emptyArray;
			if( ch == '<' ) {
				while( sc.hasMoreChars() && sc.peekChar() != '>' )
					fargs = (Type[])Arrays.append(fargs,getType(sc));
				sc.nextChar();
			}
			if( ch == '(' ) clazz = MethodType.tpMethodClazz;
			else {
				ch = sc.peekChar();
				if( ch == '(' )
					clazz = Type.tpClosureClazz;
				else if( ch == 'L')
					clazz = getType(sc).clazz;
				else
					throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - '(' or 'L' expected");
				ch = sc.nextChar();
				if( ch != '(' )
					throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - '(' expected");
			}
			args = new Type[0];
			while( sc.hasMoreChars() && sc.peekChar() != ')' )
				args = (Type[])Arrays.append(args,getType(sc));
			if( !sc.hasMoreChars() || sc.nextChar() != ')' )
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ')' expected");
			ret = getType(sc);
			return MethodType.newMethodType(clazz,fargs,args,ret);
		}

		// Normal reference type
		if( ch == '[' ) return Type.newArrayType(getType(sc));

		boolean isArgument = false;
		if( ch == 'A' ) isArgument = true;
		else if( ch != 'L')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - 'A' or 'L' or 'E' expected");
		int pos = sc.pos;
		while( sc.hasMoreChars() && (ch=sc.nextChar()) != ';' );
		if( ch != ';' )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ';' expected");
		if( isArgument ) {
			KString bcn = sc.str.substr(pos,sc.pos-1);
			ClazzName name;
			//if (bcn.indexOf((byte)'.') >= 0)
				name = ClazzName.fromBytecodeName(bcn,isArgument);
			//else
			//	name = ClazzName.fromOuterAndName(bcn);
			//name.isArgument = true;
			clazz = Env.newArgument(name.short_name,Env.getStruct(name.package_name()));
			clazz.setArgument(true);
			clazz.setResolved(true);
		} else {
			ClazzName name = ClazzName.fromBytecodeName(sc.str.substr(pos,sc.pos-1),isArgument);
			clazz = Env.newStruct(name);
		}

		if( !sc.hasMoreChars() )
			return Type.newJavaRefType(clazz);
		if( sc.peekChar() == '<' ) {
			args = new Type[0];
			sc.nextChar();
			while(sc.peekChar() != '>')
				args = (Type[])Arrays.append(args,getType(sc));
			sc.nextChar();
			if( isArgument ) {
				if( args.length == 0 )
					return Type.newRefType(clazz);
				else if( args.length == 1 ) {
					if( clazz.super_type==null )
						clazz.super_type = args[0];
					else if( !args[0].equals(clazz.super_type) )
						throw new RuntimeException("Is class argument signature "+sc
							+" type of argument super-class "+args[0]+" does not match "+clazz.super_type);
					return Type.newRefType(clazz);
				} else
					throw new RuntimeException("Signature of class's argument "+clazz+" specifies more than one super-class: "+args);
			} else {
				return Type.newRefType(clazz,args);
			}
		} else {
			return Type.newJavaRefType(clazz);
		}
	}

	public static Type getTypeOfClazzCP(KString.KStringScanner sc) {
		Struct clazz;
		Type[] args = null;

		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();

		// Normal reference type
		if( ch == '[' ) return Type.newArrayType(getType(sc));

		int pos = sc.pos;

		// RuleType
		if( ch == 'R' ) {
			sc.nextChar();
			if( !sc.hasMoreChars() )
				return Type.tpRule;
		}

		while( sc.hasMoreChars() && (ch=sc.peekChar()) != '<' ) sc.nextChar();
		if( sc.hasMoreChars() && ch != '<' )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - '<' expected");
		ClazzName name = ClazzName.fromBytecodeName(sc.str.substr(pos,sc.pos),false);
		clazz = Env.newStruct(name);

		if( !sc.hasMoreChars() )
			return Type.newJavaRefType(clazz);
		if( sc.peekChar() == '<' ) {
			args = new Type[0];
			sc.nextChar();
			while(sc.peekChar() != '>')
				args = (Type[])Arrays.append(args,getType(sc));
			sc.nextChar();
				return Type.newRefType(clazz,args);
		} else {
			return Type.newRefType(clazz);
		}
	}

	public static KString getJavaSignature(KString sig) {
		return getJavaSignature(new KString.KStringScanner(sig));
	}

	public static KString getJavaSignature(KString.KStringScanner sc) {
		KStringBuffer ksb = new KStringBuffer();
		if( sc.peekChar() == 'A' ) {
			// Argument
			while( sc.nextChar() != ';' );
			if( sc.hasMoreChars() && sc.peekChar() == '<' ) {
				sc.nextChar();
				KString ks = getJavaSignature(sc);
				sc.nextChar();
				return ks;
			} else {
				return Type.tpObject.signature;
			}
		}
		if( sc.peekChar() == '&' ) {
			sc.nextChar();
			KString sign = null;
			if( sc.peekChar() == 'L')
				sign =  getJavaSignature(sc);
			if( sc.nextChar() != '(' )
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - '(' expected");
			while( sc.peekChar() != ')' ) getJavaSignature(sc);
			if( sc.nextChar() != ')' )
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ')' expected");
			getJavaSignature(sc);
			if( sign != null )
				return sign;
			return Type.tpClosureClazz.name.signature();
		}
		if( sc.peekChar() == '(' ) {
			ksb.append(sc.nextChar());
			while( sc.peekChar() != ')' )
				ksb.append(getJavaSignature(sc));
			ksb.append(sc.nextChar()).append(getJavaSignature(sc));
			return ksb.toKString();
		}
		if( sc.peekChar() == '[' ) {
			sc.nextChar();
			return ksb.append('[').append(getJavaSignature(sc)).toKString();
		}
		if( sc.peekChar() == 'R' ) {
			sc.nextChar();
			return ksb.append("Lkiev/stdlib/RuleFrame;").toKString();
		}
		if( sc.peekChar() != 'L' )
			return ksb.append(sc.nextChar()).toKString();

		int pos = sc.pos;
		while( sc.nextChar() != ';' );
		KString kstr = sc.str.substr(pos,sc.pos);
		BaseStruct struct = Env.classHash.get(ClazzName.fromSignature(kstr).name);
		if( struct !=null && struct.isArgument() )
			kstr = struct.super_type.clazz.name.signature();
		if( sc.peekChar() == '<' ) {
			int depth = 0;
			while( sc.hasMoreChars() ) {
				char ch=sc.nextChar();
				if( ch == '<' ) depth++;
				else if( ch == '>' ) depth--;
				if( depth==0 ) break;
			}
		}
		return kstr;
	}
/*
	public static void main(String[] args) {
		Signature sig;

		// Test primitive types
		sig = new Signature(Type.tpVoid.signature);
		if( sig.getType() != Type.tpVoid ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpBoolean.signature);
		if( sig.getType() != Type.tpBoolean ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpChar.signature);
		if( sig.getType() != Type.tpChar ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpByte.signature);
		if( sig.getType() != Type.tpByte ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpShort.signature);
		if( sig.getType() != Type.tpShort ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpInt.signature);
		if( sig.getType() != Type.tpInt ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpLong.signature);
		if( sig.getType() != Type.tpLong ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpFloat.signature);
		if( sig.getType() != Type.tpFloat ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpDouble.signature);
		if( sig.getType() != Type.tpDouble ) throw new RuntimeException("Test fails for "+sig);

		Type t;
		// Test unparametriezed ref types
		sig = new Signature(Type.tpObject.signature);
		if( (t=sig.getType()) != Type.tpObject ) throw new RuntimeException("Test fails for "+sig);
		if( t.args != null ) throw new RuntimeException("Test fails for "+sig);
		sig = new Signature(Type.tpRuntimeException.signature);
		if( (t=sig.getType()) != Type.tpRuntimeException ) throw new RuntimeException("Test fails for "+sig);
		if( t.args != null ) throw new RuntimeException("Test fails for "+sig);

		// Test parametriezed ref types
		Type tt;

		KString sign;
		sign = KString.from("Lpizza/lang/List;<AA;>");
		tt = Type.newRefType(ClazzName.fromSignature(sign));

		System.out.println("Type for pizza.lang.List<A> is "+tt+" with signature "+tt.signature);
		sig = new Signature(tt.signature);
		if( (t=sig.getType()) != tt ) throw new RuntimeException("Test fails for "+sig);
		if( t.args == null || t.args.length != 1 || t.args[0] != tt.args[0]) throw new RuntimeException("Test fails for "+sig);

	}
*/
}
