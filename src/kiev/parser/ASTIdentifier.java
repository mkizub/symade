/* Generated By:JJTree: Do not edit this line. ASTIdentifier.java */

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

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
@dflow(out="this:in")
public class ASTIdentifier extends ENode {
	private static KString op_instanceof = KString.from("instanceof");
	public KString name;
	
	public ASTIdentifier() {
	}

	public ASTIdentifier(KString name) {
		this.name = name;
	}

	public ASTIdentifier(int pos, KString name) {
		super(0);
		this.pos = pos;
		this.name = name;
	}

	public void set(Token t) {
		if (t.image.startsWith("ID#"))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-1));
		else
			this.name = KString.from(t.image);
        pos = t.getPos();
	}
	
	public Type getType() {
		return Type.tpVoid;
	}

	public boolean preResolveIn(TransfProcessor proc) {
		// predefined operators
		if( name == op_instanceof ) {
			ASTOperator op = new ASTOperator();
			op.pos = this.pos;
			op.image = op_instanceof;
			this.replaceWithNode(op);
			return false;
		}
		// predefined names
		if( name == Constants.nameFILE ) {
			ConstExpr ce = new ConstStringExpr(Kiev.curFile);
			ce.text_name = this.name;
			replaceWithNode(ce);
			return false;
		}
		else if( name == Constants.nameLINENO ) {
			ConstExpr ce = new ConstIntExpr(pos>>>11);
			ce.text_name = this.name;
			replaceWithNode(ce);
			return false;
		}
		else if( name == Constants.nameMETHOD ) {
			ConstExpr ce;
			if( PassInfo.method != null )
				ce = new ConstStringExpr(PassInfo.method.name.name);
			else
				ce = new ConstStringExpr(nameInit);
			ce.text_name = this.name;
			replaceWithNode(ce);
			return false;
		}
		else if( name == Constants.nameDEBUG ) {
			ConstExpr ce = new ConstBoolExpr(Kiev.debugOutputA);
			ce.text_name = this.name;
			replaceWithNode(ce);
			return false;
		}
		else if( name == Constants.nameReturnVar ) {
			Kiev.reportWarning(pos,"Keyword '$return' is deprecated. Replace with 'Result', please");
			name = Constants.nameResultVar;
		}
		else if( name == Constants.nameThis ) {
			ThisExpr te = new ThisExpr(pos);
			replaceWithNode(te);
			return false;
		}
		else if( name == Constants.nameSuper ) {
			ThisExpr te = new ThisExpr(pos);
			te.super_flag = true;
			replaceWithNode(te);
			return false;
		}

		// resolve in the path of scopes
		ASTNode@ v;
		ResInfo info = new ResInfo();
		if( !PassInfo.resolveNameR(this,v,info,name) ) {
//			if( name.startsWith(Constants.nameDEF) ) {
//				String prop = name.toString().substring(2);
//				String val = Env.getProperty(prop);
//				if( val == null ) val = Env.getProperty(prop.replace('_','.'));
//				if( val != null ) {
//					if( reqType == null || reqType == Type.tpString)
//						return new ConstStringExpr(KString.from(val));
//					if( reqType.isBoolean() )
//						if( val == "" )
//							return new ConstBoolExpr(true);
//						else
//							return new ConstBoolExpr(Boolean.valueOf(val).booleanValue());
//					if( reqType.isInteger() )
//						return new ConstIntExpr(Integer.valueOf(val).intValue());
//					if( reqType.isNumber() )
//						return new ConstDoubleExpr(Double.valueOf(val).doubleValue());
//					return new ConstStringExpr(KString.from(val));
//				}
//				if( reqType.isBoolean() )
//					return new ConstBoolExpr(false);
//				return new ConstNullExpr();
//			}
			throw new CompilerException(pos,"Unresolved identifier "+name);
		}
		if( v instanceof Struct ) {
			Struct s = (Struct)v;
			s.checkResolved();
			replaceWithNode(new TypeRef(s.type));
		}
		else if( v instanceof TypeRef ) {
			replaceWithNode((TypeRef)v);
		}
		else {
			replaceWithNode(info.buildAccess(pos, null, v));
		}
		return false;
	}
	
	public boolean preGenerate() {
		return false;
	}
	
	public void resolve(Type reqType) {
		if( name == Constants.nameFILE ) {
			replaceWithNode(new ConstStringExpr(Kiev.curFile));
			return;
		}
		else if( name == Constants.nameLINENO ) {
			replaceWithNode(new ConstIntExpr(pos>>>11));
			return;
		}
		else if( name == Constants.nameMETHOD ) {
			if( PassInfo.method != null )
				replaceWithNode(new ConstStringExpr(PassInfo.method.name.name));
			else
				replaceWithNode(new ConstStringExpr(nameInit));
			return;
		}
		else if( name == Constants.nameDEBUG ) {
			replaceWithNode(new ConstBoolExpr(Kiev.debugOutputA));
			return;
		}
		else if( name == Constants.nameReturnVar ) {
			Kiev.reportWarning(pos,"Keyword '$return' is deprecated. Replace with 'Result', please");
			name = Constants.nameResultVar;
		}
		ASTNode@ v;
		ResInfo info = new ResInfo();
		if( !PassInfo.resolveNameR(this,v,info,name) ) {
			if( name.startsWith(Constants.nameDEF) ) {
				String prop = name.toString().substring(2);
				String val = Env.getProperty(prop);
				if( val == null ) val = Env.getProperty(prop.replace('_','.'));
				if( val != null ) {
					if( reqType == null || reqType == Type.tpString) {
						replaceWithNode(new ConstStringExpr(KString.from(val)));
						return;
					}
					if( reqType.isBoolean() ) {
						if( val == "" ) 
							replaceWithNode(new ConstBoolExpr(true));
						else
							replaceWithNode(new ConstBoolExpr(Boolean.valueOf(val).booleanValue()));
						return;
					}
					if( reqType.isInteger() ) {
						replaceWithNode(new ConstIntExpr(Integer.valueOf(val).intValue()));
						return;
					}
					if( reqType.isNumber() ) {
						replaceWithNode(new ConstDoubleExpr(Double.valueOf(val).doubleValue()));
					}
					replaceWithNode(new ConstStringExpr(KString.from(val)));
					return;
				}
				if( reqType.isBoolean() )
					replaceWithNode(new ConstBoolExpr(false));
				else
					replaceWithNode(new ConstNullExpr());
				return;
			}
			throw new CompilerException(pos,"Unresolved identifier "+name);
		}
		if( v instanceof Struct ) {
			Struct s = (Struct)v;
			s.checkResolved();
			if( reqType != null && reqType.equals(Type.tpInt) ) {
				if( s.isPizzaCase() ) {
					PizzaCaseAttr case_attr = (PizzaCaseAttr)s.getAttr(attrPizzaCase);
					if( case_attr != null ) {
						replaceWithNodeResolve(reqType, new ConstIntExpr(case_attr.caseno));
						return;
					}
				}
			}
			replaceWithNode(new TypeNameRef(new NameRef(pos,name),s.type));
			return;
		}
		else if( v instanceof TypeRef ) {
			replaceWithNode((TypeRef)v);
			return;
		}
		replaceWithNodeResolve(reqType, info.buildAccess(pos, null, v));
	}

	public int		getPriority() { return 256; }

	public KString toKString() {
		return name;
	}
    
	public String toString() {
		return name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}
}

