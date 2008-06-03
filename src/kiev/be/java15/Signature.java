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

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

final class Signature {

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
		}

		// Check if this signature is a method signature
		if (ch == '(') {
			Type[] args = null;
			Type ret = null;
			// Method signature
			args = new Type[0];
			while( sc.hasMoreChars() && sc.peekChar() != ')' )
				args = (Type[])Arrays.append(args,getType(sc));
			if( !sc.hasMoreChars() || sc.nextChar() != ')' )
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ')' expected");
			ret = getType(sc);
			return new CallType(null,null,args,ret,false);
		}

		// Normal reference type
		if( ch == '[' )
			return new ArrayType(getType(sc));

		if( ch != 'L')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - 'L' expected");
		int pos = sc.pos;
		while( sc.hasMoreChars() && (ch=sc.nextChar()) != ';' );
		if( ch != ';' )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ';' expected");
		ClazzName cname = ClazzName.fromBytecodeName(sc.str.substr(pos,sc.pos-1));
		CompaundMetaType cmt = new CompaundMetaType(cname.name.toString().replace('.','\u001f'));
		return new CompaundType(cmt, TVarBld.emptySet);
	}

	public static Type getTypeOfClazzCP(KString.KStringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();

		// Normal reference type
		if( ch == '[' ) return new ArrayType(getType(sc));

		ClazzName cname = ClazzName.fromBytecodeName(sc.str.substr(sc.pos));
		CompaundMetaType cmt = new CompaundMetaType(cname.name.toString().replace('.','\u001f'));
		return new CompaundType(cmt, TVarBld.emptySet);
	}
	
	private static TypeConstr getTypeArgDecl(DNode dn, String aname) {
		if (dn instanceof Method) {
			foreach (TypeConstr tc; dn.targs; tc.sname == aname)
				return tc;
			return getTypeArgDecl((TypeDecl)dn.parent(), aname);
		}
		else if (dn instanceof TypeDecl) {
			foreach (TypeConstr tc; dn.args; tc.sname == aname)
				return tc;
			if (dn.isStructInner())
				return getTypeArgDecl((TypeDecl)dn.parent(), aname);
		}
		return null;
	}

	public static void addTypeArgs(DNode dn, KString.KStringScanner sc) {
		if (!sc.hasMoreChars() || sc.peekChar() != '<')
			return;

		sc.nextChar();
		do {
			int pos = sc.pos;
			while (sc.hasMoreChars() && sc.nextChar() != ':');
			KString aname = sc.str.substr(pos,sc.pos-1);
			TypeConstr arg = new TypeConstr(aname.toString());
			arg.setAbstract(true);
			if (dn instanceof Method)
				((Method)dn).targs += arg;
			else
				((TypeDecl)dn).args += arg;
			if (sc.peekChar() != ':' && sc.peekChar() != '>') {
				Type bnd = getTypeFromFieldSignature(dn,sc);
				arg.super_types += new TypeRef(bnd);
			}
			while (sc.hasMoreChars() && sc.peekChar() == ':') {
				sc.nextChar();
				Type bnd = getTypeFromFieldSignature(dn,sc);
				arg.super_types += new TypeRef(bnd);
			}
		} while (sc.hasMoreChars() && sc.peekChar() != '>');
		if (sc.nextChar() != '>')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - '>' expected");
	}

	public static Type getTypeFromFieldSignature(DNode dn, KString.KStringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();

		// Noral type
		if (ch == 'L')
			return getClassTypeSignature(dn, sc);

		sc.nextChar();

		switch(ch) {
		case 'V': return Type.tpVoid;
		case 'Z': return Type.tpBoolean;
		case 'C': return Type.tpChar;
		case 'B': return Type.tpByte;
		case 'S': return Type.tpShort;
		case 'I': return Type.tpInt;
		case 'J': return Type.tpLong;
		case 'F': return Type.tpFloat;
		case 'D': return Type.tpDouble;
		}

		// Array type
		if (ch == '[')
			return new ArrayType(getTypeFromFieldSignature(dn, sc));

		// Type argument type
		if (ch == 'T') {
			int pos = sc.pos;
			while (sc.hasMoreChars() && sc.nextChar() != ';');
			String aname = sc.str.substr(pos,sc.pos-1).toString().intern();
			TypeConstr tc = getTypeArgDecl(dn, aname);
			if (tc == null)
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" unknown type argument: "+aname);
			return tc.getAType();
		}

		throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected 'T' or '[' or 'L'");
	}

	public static CompaundType getClassTypeSignature(DNode dn, KString.KStringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.nextChar();

		if (ch != 'L')
			throw new RuntimeException("Bad class type signature "+sc+" at pos "+sc.pos+" expected 'L'");
	
		int pos = sc.pos;
		do {
			ch = sc.nextChar();
		} while (sc.hasMoreChars() && ch != '<' && ch != ';' && ch != '.');
		String cname = sc.str.substr(pos,sc.pos-1).toString();
		CompaundMetaType cmt = new CompaundMetaType(cname.replace('/','\u001f'));
		cmt.tdecl.checkResolved();
		TVarBld vs = new TVarBld();
		if (ch == '<') {
			int aidx = 0;
			// type arguments
			while (sc.hasMoreChars() && (ch=sc.peekChar()) != '>') {
				Type param;
				if (ch == '*') {
					// co-variant wildcard with upper bound = java.lang.Object
					sc.nextChar();
					param = new WildcardCoType(StdTypes.tpObject);
				}
				else if (ch == '+') {
					sc.nextChar();
					param = getTypeFromFieldSignature(dn, sc); // co-variant wildcard with upper bound
					param = new WildcardCoType(param);
				}
				else if (ch == '-') {
					sc.nextChar();
					param = getTypeFromFieldSignature(dn, sc); // contra-variant wildcard with lower bound
					param = new WildcardContraType(param);
				}
				else {
					param = getTypeFromFieldSignature(dn, sc);
				}
				vs.append(cmt.tdecl.args[aidx++].getAType(), param);
			}
			ch = sc.nextChar();
			assert (ch == '>');
			ch = sc.nextChar();
		}
		CompaundType ct = (CompaundType)cmt.make(vs);
		while (ch == '.') {
			CompaundType outer = ct;
			outer.checkResolved();
			// inner class
			int pos = sc.pos;
			do {
				ch = sc.nextChar();
			} while (sc.hasMoreChars() && ch != '<' && ch != ';' && ch != '.');
			cname = sc.str.substr(pos,sc.pos-1).toString();
			cmt = new CompaundMetaType(outer.meta_type.qname() + '\u001f' + cname);
			cmt.tdecl.checkResolved();
			if (ch == '<') {
				vs = new TVarBld();
				int aidx = 0;
				// type arguments
				while (sc.hasMoreChars() && (ch=sc.peekChar()) != '>') {
					Type param;
					if (ch == '*') {
						// co-variant wildcard with upper bound = java.lang.Object
						param = new WildcardCoType(StdTypes.tpObject);
					}
					else if (ch == '+') {
						param = getTypeFromFieldSignature(dn, sc); // co-variant wildcard with upper bound
						param = new WildcardCoType(param);
					}
					else if (ch == '-') {
						param = getTypeFromFieldSignature(dn, sc); // contra-variant wildcard with lower bound
						param = new WildcardContraType(param);
					}
					else {
						param = getTypeFromFieldSignature(dn, sc);
					}
					vs.append(cmt.tdecl.args[aidx++].getAType(), param);
				}
				ch = sc.nextChar();
				assert (ch == '>');
				ch = sc.nextChar();
			}
			ct = (CompaundType)cmt.make(vs);
			ct.checkResolved();
			TypeAssign ta = ct.meta_type.tdecl.ometa_tdef;
			if (ta == null)
				Kiev.reportWarning("in signature "+sc+" at pos "+sc.pos+": type "+ct+" inner of "+outer+" must have outer TypeAssign");
			else
				ct = (CompaundType)ct.rebind(new TVarBld(ta.getAType(), outer));
		}
		if (ch != ';')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected ';' but found '"+ch+"'");
		return ct;
	}

	public static CallType getTypeFromMethodSignature(Method m, KString.KStringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();
		if (ch != '(')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected '(' but found '"+ch+"'");
		sc.nextChar();
		
		Vector<Type> args = new Vector<Type>();
		while (sc.hasMoreChars() && (ch=sc.peekChar()) != ')') {
			Type arg = getTypeFromFieldSignature(m, sc);
			args.append(arg);
		}
		if (ch != ')')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected ')' but found '"+ch+"'");
		sc.nextChar();
		Type ret = getTypeFromFieldSignature(m, sc);
		Vector<Type> targs = new Vector<Type>();
		foreach (TypeConstr targ; m.targs)
			targs.append(targ.getAType());
		return CallType.createCallType(null, targs.toArray(), args.toArray(), ret, false);
	}

}
