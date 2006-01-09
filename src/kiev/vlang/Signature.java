package kiev.vlang;

import kiev.Main;
import kiev.stdlib.*;

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
/*
	public static KString from(Struct clazz, Type[] args) {
		KStringBuffer ksb = new KStringBuffer();
		ksb.append(clazz.name.signature());
		// And it's arguments
		boolean empty = true;
		if( clazz.args.length > 0 ) {
			ksb.append('<');
			for(int i=0; i < args.length; i++) {
				ksb.append(args[i].signature);
			}
			ksb.append('>');
		}
		return ksb.toKString();
	}

	public static KString from(Struct clazz, TVarSet bindings) {
		KStringBuffer ksb = new KStringBuffer();
		ksb.append(clazz.name.signature());
		// And it's type bindings
		boolean empty = true;
		foreach (TVar tv; bindings.tvars) {
			if (tv.var.definer != clazz) {
				if (!tv.isBound() && !tv.isAlias())
					continue;
			}
			if (empty) { ksb.append('<'); empty = false; }
			ClazzName name = tv.var.name;
			if (clazz == tv.var.definer)
				ksb.append(name.short_name);
			else
				ksb.append(name.bytecode_name);
			ksb.append(':').append(tv.result().signature);
		}
		if (!empty) ksb.append('>');
		return ksb.toKString();
	}

	public static KString fromToClazzCP(Struct clazz,Type[] args, boolean full) {
		KStringBuffer ksb = new KStringBuffer(128);
		ksb.append(clazz.name.bytecode_name);
		if( args != null && args.length > 0 ) {
			ksb.append('<');
			for(int i=0; i < args.length; i++) {
				ksb.append(args[i].signature);
				if( full && args[i].isArgument() ) {
					ksb.append('<');
					ksb.append(args[i].getSuperType().signature);
					ksb.append('>');
				}
			}
			ksb.append('>');
		}
		return ksb.toKString();
	}
*/
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
			return new MethodType(args,ret);
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
			return new ClosureType(args,ret);
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
			cname = ClazzName.fromBytecodeName(bcn,isArgument);
			clazz = null;
		} else {
			cname = ClazzName.fromBytecodeName(sc.str.substr(pos,sc.pos-1),isArgument);
			clazz = Env.newStruct(cname);
		}

//		if( !sc.hasMoreChars() ) {
//			if (isArgument)
//				throw new RuntimeException("not implemented"); //return new ArgType(cname,null);
//			return ConcreteType.createRefType(clazz, TVarSet.emptySet);
//		}
//		if( sc.peekChar() == '<' ) {
//			args = new Type[0];
//			sc.nextChar();
//			while(sc.peekChar() != '>')
//				args = (Type[])Arrays.append(args,getType(sc));
//			sc.nextChar();
//			if( isArgument ) {
//				if( args.length == 0 )
//					throw new RuntimeException("not implemented"); //return new ArgType(cname,null);
//				else if( args.length == 1 ) {
//					if !( args[0] instanceof ConcreteType )
//						throw new RuntimeException("Bad super-class "+args[0]+" of argument "+cname);
//					throw new RuntimeException("not implemented"); //return new ArgType(cname,(ConcreteType)args[0]);
//				} else
//					throw new RuntimeException("Signature of class's argument "+cname+" specifies more than one super-class: "+args);
//			} else {
//				return ConcreteType.createRefType(clazz,args);
//			}
//		} else {
			if (isArgument)
				throw new RuntimeException("not implemented"); //return new ArgType(cname,null);
			return new ConcreteType(clazz.imeta_type, TVarSet.emptySet);
//		}
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
		ClazzName name = ClazzName.fromBytecodeName(sc.str.substr(pos,sc.pos),false);
		clazz = Env.newStruct(name);

//		Type[] args = Type.emptyArray;
//		if( sc.hasMoreChars() && sc.peekChar() == '<' ) {
//			args = new Type[0];
//			sc.nextChar();
//			while(sc.peekChar() != '>')
//				args = (Type[])Arrays.append(args,getType(sc));
//			sc.nextChar();
//		}
		return new ConcreteType(clazz.imeta_type, TVarSet.emptySet);
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
		Struct struct = Env.classHash.get(ClazzName.fromSignature(kstr).name);
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
