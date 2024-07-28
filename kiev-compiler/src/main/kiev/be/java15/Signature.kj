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
 * @version $Revision: 296 $
 *
 */

final class Signature {

	final String		sig;

	private Signature(String sig) {
		this.sig = sig;
	}

	public String toString() { return sig; }

	public int hashCode() { return sig.hashCode(); }

	public boolean equals(Object o) {
		return ( o instanceof Signature && ((Signature)o).sig.equals(sig) );
	}

	public static Type getType(JEnv jenv, String sig) {
		return Signature.getType(jenv, new StringScanner(sig));
	}
	
	public static Type getType(JEnv jenv, StringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.nextChar();

		// Check for primitive types
		switch(ch) {
		case 'V':		return jenv.vtypes.tpVoid;
		case 'Z':		return jenv.vtypes.tpBoolean;
		case 'C':		return jenv.vtypes.tpChar;
		case 'B':		return jenv.vtypes.tpByte;
		case 'S':		return jenv.vtypes.tpShort;
		case 'I':		return jenv.vtypes.tpInt;
		case 'J':		return jenv.vtypes.tpLong;
		case 'F':		return jenv.vtypes.tpFloat;
		case 'D':		return jenv.vtypes.tpDouble;
		}

		// Check if this signature is a method signature
		if (ch == '(') {
			Type[] args = null;
			Type ret = null;
			// Method signature
			args = new Type[0];
			while( sc.hasMoreChars() && sc.peekChar() != ')' )
				args = (Type[])Arrays.append(args,getType(jenv,sc));
			if( !sc.hasMoreChars() || sc.nextChar() != ')' )
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ')' expected");
			ret = getType(jenv,sc);
			return new CallType(null,null,args,ret,false);
		}

		// Normal reference type
		if( ch == '[' )
			return new ArrayType(getType(jenv,sc));

		if( ch != 'L')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - 'L' expected");
		int pos = sc.pos;
		while( sc.hasMoreChars() && (ch=sc.nextChar()) != ';' );
		if( ch != ';' )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - ';' expected");
		ClazzName cname = ClazzName.fromBytecodeName(jenv,sc.str.substring(pos,sc.pos-1));
		CompaundMetaType cmt = new CompaundMetaType(jenv.vtypes,cname.name.replace('.','·'));
		return new CompaundType(cmt, null, null);
	}

	public static Type getTypeOfClazzCP(JEnv jenv, StringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();

		// Normal reference type
		if( ch == '[' ) return new ArrayType(getType(jenv,sc));

		ClazzName cname = ClazzName.fromBytecodeName(jenv,sc.str.substring(sc.pos));
		CompaundMetaType cmt = new CompaundMetaType(jenv.vtypes,cname.name.replace('.','·'));
		return new CompaundType(cmt, null, null);
	}
	
	private static TypeConstr getTypeArgDecl(DNode dn, String aname) {
		if (dn instanceof Method) {
			foreach (TypeConstr tc; dn.targs; tc.sname == aname)
				return tc;
			return getTypeArgDecl((TypeDecl)dn.parent(), aname);
		}
		else if (dn instanceof ComplexTypeDecl) {
			foreach (TypeConstr tc; dn.args; tc.sname == aname)
				return tc;
			if (dn.isStructInner())
				return getTypeArgDecl((ComplexTypeDecl)dn.parent(), aname);
		}
		return null;
	}

	public static void addTypeArgs(JEnv jenv, DNode dn, StringScanner sc) {
		if (!sc.hasMoreChars() || sc.peekChar() != '<')
			return;

		sc.nextChar();
		do {
			int pos = sc.pos;
			while (sc.hasMoreChars() && sc.nextChar() != ':');
			String aname = sc.str.substring(pos,sc.pos-1);
			Symbol sym = null;
			if (dn instanceof TypeDecl) {
				if (dn.symbol.isGlobalSymbol())
					sym = dn.symbol.makeGlobalSubSymbol(aname);
			}
			if (sym == null)
				sym = new Symbol(aname);
			TypeConstr arg = new TypeConstr(sym);
			arg.setAbstract(true);
			if (dn instanceof Method)
				((Method)dn).targs += arg;
			else
				((ComplexTypeDecl)dn).args += arg;
			if (sc.peekChar() != ':' && sc.peekChar() != '>') {
				Type bnd = getTypeFromFieldSignature(jenv,dn,sc);
				arg.super_types += new TypeRef(bnd);
			}
			while (sc.hasMoreChars() && sc.peekChar() == ':') {
				sc.nextChar();
				Type bnd = getTypeFromFieldSignature(jenv,dn,sc);
				arg.super_types += new TypeRef(bnd);
			}
		} while (sc.hasMoreChars() && sc.peekChar() != '>');
		if (sc.nextChar() != '>')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - '>' expected");
	}

	public static Type getTypeFromFieldSignature(JEnv jenv, DNode dn, StringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();

		// Noral type
		if (ch == 'L')
			return getClassTypeSignature(jenv, dn, sc);

		sc.nextChar();

		switch(ch) {
		case 'V': return jenv.vtypes.tpVoid;
		case 'Z': return jenv.vtypes.tpBoolean;
		case 'C': return jenv.vtypes.tpChar;
		case 'B': return jenv.vtypes.tpByte;
		case 'S': return jenv.vtypes.tpShort;
		case 'I': return jenv.vtypes.tpInt;
		case 'J': return jenv.vtypes.tpLong;
		case 'F': return jenv.vtypes.tpFloat;
		case 'D': return jenv.vtypes.tpDouble;
		}

		// Array type
		if (ch == '[')
			return new ArrayType(getTypeFromFieldSignature(jenv, dn, sc));

		// Type argument type
		if (ch == 'T') {
			int pos = sc.pos;
			while (sc.hasMoreChars() && sc.nextChar() != ';');
			String aname = sc.str.substring(pos,sc.pos-1).intern();
			TypeConstr tc = getTypeArgDecl(dn, aname);
			if (tc == null)
				throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" unknown type argument: "+aname);
			return tc.getAType(jenv.env);
		}

		throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected 'T' or '[' or 'L'");
	}

	public static CompaundType getClassTypeSignature(JEnv jenv, DNode dn, StringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.nextChar();

		if (ch != 'L')
			throw new RuntimeException("Bad class type signature "+sc+" at pos "+sc.pos+" expected 'L'");
	
		int pos = sc.pos;
		do {
			ch = sc.nextChar();
		} while (sc.hasMoreChars() && ch != '<' && ch != ';' && ch != '.');
		String cname = sc.str.substring(pos,sc.pos-1);
		CompaundMetaType cmt = new CompaundMetaType(jenv.vtypes, cname.replace('/','·'));
		CompaundType ct;
		if (ch == '<') {
			cmt.tdecl.checkResolved(jenv.env);
			TVarBld vs = new TVarBld();
			int aidx = 0;
			// type arguments
			while (sc.hasMoreChars() && (ch=sc.peekChar()) != '>') {
				Type param;
				if (ch == '*') {
					// co-variant wildcard with upper bound = java.lang.Object
					sc.nextChar();
					param = new WildcardCoType(jenv.vtypes.tpObject);
				}
				else if (ch == '+') {
					sc.nextChar();
					param = getTypeFromFieldSignature(jenv, dn, sc); // co-variant wildcard with upper bound
					param = new WildcardCoType(param);
				}
				else if (ch == '-') {
					sc.nextChar();
					param = getTypeFromFieldSignature(jenv, dn, sc); // contra-variant wildcard with lower bound
					param = new WildcardContraType(param);
				}
				else {
					param = getTypeFromFieldSignature(jenv, dn, sc);
				}
				vs.append(cmt.tdecl.args[aidx++].getAType(jenv.env), param);
			}
			ch = sc.nextChar();
			assert (ch == '>');
			ch = sc.nextChar();
			ct = (CompaundType)cmt.make(vs);
		} else {
			ct = (CompaundType)cmt.make(null);
		}
		while (ch == '.') {
			CompaundType outer = ct;
			outer.checkResolved();
			// inner class
			int pos = sc.pos;
			do {
				ch = sc.nextChar();
			} while (sc.hasMoreChars() && ch != '<' && ch != ';' && ch != '.');
			cname = sc.str.substring(pos,sc.pos-1);
			cmt = new CompaundMetaType(jenv.vtypes, outer.meta_type.qname() + '·' + cname);
			cmt.tdecl.checkResolved(jenv.env);
			TVarBld vs = new TVarBld();
			TypeAssign ta = ((ComplexTypeDecl)cmt.tdecl).ometa_tdef;
			if (ta == null)
				Kiev.reportWarning("in signature "+sc+" at pos "+sc.pos+": type "+ct+" inner of "+outer+" must have outer TypeAssign");
			else
				vs.append(ta.getAType(jenv.env), outer);
			if (ch == '<') {
				int aidx = 0;
				// type arguments
				while (sc.hasMoreChars() && (ch=sc.peekChar()) != '>') {
					Type param;
					if (ch == '*') {
						// co-variant wildcard with upper bound = java.lang.Object
						param = new WildcardCoType(jenv.vtypes.tpObject);
					}
					else if (ch == '+') {
						param = getTypeFromFieldSignature(jenv, dn, sc); // co-variant wildcard with upper bound
						param = new WildcardCoType(param);
					}
					else if (ch == '-') {
						param = getTypeFromFieldSignature(jenv, dn, sc); // contra-variant wildcard with lower bound
						param = new WildcardContraType(param);
					}
					else {
						param = getTypeFromFieldSignature(jenv, dn, sc);
					}
					vs.append(cmt.tdecl.args[aidx++].getAType(jenv.env), param);
				}
				ch = sc.nextChar();
				assert (ch == '>');
				ch = sc.nextChar();
			}
			if (vs.getArgsLength() > 0)
				ct = (CompaundType)cmt.make(vs);
			else
				ct = (CompaundType)cmt.make(null);
		}
		if (ch != ';')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected ';' but found '"+ch+"'");
		return ct;
	}

	public static CallType getTypeFromMethodSignature(JEnv jenv, Method m, StringScanner sc) {
		if( !sc.hasMoreChars() )
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" - empty");

		char ch = sc.peekChar();
		if (ch != '(')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected '(' but found '"+ch+"'");
		sc.nextChar();
		
		Vector<Type> args = new Vector<Type>();
		while (sc.hasMoreChars() && (ch=sc.peekChar()) != ')') {
			Type arg = getTypeFromFieldSignature(jenv, m, sc);
			args.append(arg);
		}
		if (ch != ')')
			throw new RuntimeException("Bad signature "+sc+" at pos "+sc.pos+" expected ')' but found '"+ch+"'");
		sc.nextChar();
		Type ret = getTypeFromFieldSignature(jenv, m, sc);
		Vector<Type> targs = new Vector<Type>();
		foreach (TypeConstr targ; m.targs)
			targs.append(targ.getAType(jenv.env));
		Type accessor = null;
		if (!m.isStatic())
			accessor = ((TypeDecl)m.parent()).getType(jenv.env);
		return new CallType(accessor, targs.toArray(), args.toArray(), ret, false);
	}

}

public class StringScanner {

	public String	str;
	public int		pos;
	
	public StringScanner(String str) {
		this.str = str;
		pos = 0;
	}
	
	public String toString() { return str; }
	
	public boolean hasMoreChars() {
		return pos < str.length();
	}

	public char nextChar() {
		return str.charAt(pos++);
	}

	public char peekChar() {
		return str.charAt(pos);
	}
}

