package kiev.vlang;

import kiev.Main;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import kiev.be.java15.JEnv;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class Signature {

	KString		sig;

	private Signature(KString sig) {
		this.sig = sig;
	}

	public String toString() { return sig.toString(); }

	public int hashCode() { return sig.hashCode(); }

	public boolean equals(Object o) {
		return ( o instanceof Signature && ((Signature)o).sig.equals(sig) );
	}

	public static Type getType(KString sig) {
		return Signature.getType(new KString.KStringScanner(sig));
	}
	
	public static Type getType(KString.KStringScanner sc) {
		Struct clazz;
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
		if (ch == '(') {
			// Method signature
			args = new Type[0];
			while( sc.hasMoreChars() && sc.peekChar() != ')' )
				args = (Type[])Arrays.append(args,getType(sc));
			if( !sc.hasMoreChars() || sc.nextChar() != ')' )
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ')' expected");
			ret = getType(sc);
			return new CallType(args,ret);
		}
		if (ch == '&') {
			// Closure signature
			ch = sc.peekChar();
			if( ch != '(' )
				throw new RuntimeException("Bad closure "+sc+" at pos "+sc.pos+" - '(' expected");
			args = new Type[0];
			while( sc.hasMoreChars() && sc.peekChar() != ')' )
				args = (Type[])Arrays.append(args,getType(sc));
			if( !sc.hasMoreChars() || sc.nextChar() != ')' )
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ')' expected");
			ret = getType(sc);
			return new CallType(args,ret,true);
		}

		// Normal reference type
		if( ch == '[' ) return new ArrayType(getType(sc));

		boolean isArgument = false;
		if( ch == 'A' ) isArgument = true;
		else if( ch != 'L')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - 'A' or 'L' or 'E' expected");
		int pos = sc.pos;
		while( sc.hasMoreChars() && (ch=sc.nextChar()) != ';' );
		if( ch != ';' )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ';' expected");
		ClazzName cname = null;
		if( isArgument ) {
			KString bcn = sc.str.substr(pos,sc.pos-1);
			cname = ClazzName.fromBytecodeName(bcn);
			clazz = null;
		} else {
			//cname = ClazzName.fromBytecodeName(sc.str.substr(pos,sc.pos-1));
			clazz = Env.jenv.newStruct(sc.str.substr(pos,sc.pos-1),false);
		}

		if (isArgument)
			throw new RuntimeException("not implemented"); //return new ArgType(cname,null);
		return new CompaundType(clazz.imeta_type, TVarBld.emptySet);
	}

	public static Type getTypeOfClazzCP(KString.KStringScanner sc) {
		Struct clazz;

		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();

		// Normal reference type
		if( ch == '[' ) return new ArrayType(getType(sc));

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
		clazz = Env.jenv.newStruct(sc.str.substr(pos,sc.pos), false);

		return new CompaundType(clazz.imeta_type, TVarBld.emptySet);
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
				return KString.from("Lkava/lang/Object;"); //Type.tpObject.signature;
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
			return KString.from("L"+Type.tpClosureClazz.name.bytecode_name+";");
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
}
