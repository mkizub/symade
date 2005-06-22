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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.tree.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/AST.java,v 1.6.2.1.2.3 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.3 $
 *
 */

// AST declarations for FileUnit, Struct-s, Import-s, Operator-s, Typedef-s, Macros-es
public interface TopLevelDecl {
	// create top-level, inner named, argument Struct-s
	public Node pass1() { return (Node)this; }
	// resolve some imports, remember typedef's names, remember
	// operator declarations, remember names/operators for type macroses
	public Node pass1_1() { return (Node)this; }
	// process inheritance for type arguments, create
	// Struct's for template types
	public Node pass2() { return (Node)this; }
	// process Struct's inheritance (extends/implements)
	public Node pass2_2() { return (Node)this; }
	// process Struct's members (fields, methods)
	public Node pass3() { return (Node)this; }
	// autoProxyMethods()
	public Node autoProxyMethods() { return (Node)this; }
	// resolveImports()
	public Node resolveImports() { return (Node)this; }
	// resolveFinalFields()
	public Node resolveFinalFields(boolean cleanup) { return (Node)this; }
};

public enum TopLevelPass /*extends int*/ {
	passStartCleanup		= 0,	// start of compilation or cleanup before next incremental compilation
	passCreateTopStruct		= 1,	// create top-level Struct
	passProcessSyntax		= 2,	// process syntax - some import, typedef, operator and macro
	passArgumentInheritance	= 3,	// inheritance of type arguments
	passStructInheritance	= 4,	// inheritance of classe/interfaces/structures
	passCreateMembers		= 5,	// create declared members of structures
	passAutoProxyMethods	= 6,	// autoProxyMethods()
	passResolveImports		= 7,	// recolve import static for import of fields and methods
	passResolveFinalFields	= 8,	// resolve final fields, to find out if they are constants
	passGenerate			= 9		// resolve, generate and so on - each file separatly
};

public abstract class Expr extends Node {

	public static Expr[] emptyArray = new Expr[0];

	public Expr(int pos) { super(pos); }

	public Expr(int pos, Node parent) { super(pos,parent); }

	public void jjtAddChild(Node n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public /*abstract*/ Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public void		generate(Type reqType) {
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation");
	}

	public int		getPriority() { return 0; }
	public boolean	isConstantExpr() { return false; }
	public Object	getConstValue() {
    	throw new RuntimeException("Request for constant value of non-constant expression");
    }
	public /*abstract*/ Node	resolve(Type reqType) {
		throw new CompilerException(pos,"Resolve call for node "+getClass());
	}
	public /*abstract*/ Expr tryResolve(Type reqType) {
		Node n = resolve(reqType);
		if( n instanceof Expr ) return (Expr)n;
		else return new WrapedExpr(pos,n,reqType);
	}
	public Expr resolveExpr(Type reqType) {
		Node e = tryResolve(reqType);
		if( e == null )
			throw new CompilerException(pos,"Unresolved expression "+this);
		Expr expr = null;
		if( e instanceof Expr ) expr = (Expr)e;
		if( e instanceof Struct ) expr = toExpr((Struct)e,reqType,pos,this.parent);
		if( e instanceof WrapedExpr ) expr = toExpr((Struct)((WrapedExpr)e).expr,reqType,pos,this.parent);
		if( expr == null )
			throw new CompilerException(e.pos,"Is not an expression");
		else if( reqType == null || reqType == Type.tpVoid )
			return expr;
//		if( reqType == Type.tpRule ) reqType = Type.tpBoolean;
		Type et = expr.getType();
//		if( et.isBoolean() && reqType.isBoolean() ) return expr;
		if( et.isInstanceOf(reqType) ) return expr;
		if( et.isReference() && reqType.isBoolean() )
			return new BinaryBooleanExpr(pos,BinaryOperator.Equals,expr,new ConstExpr(pos,null));
		if( et.isAutoCastableTo(reqType)
		 || et.isNumber() && reqType.isNumber()
		) return new CastExpr(pos,reqType,expr).tryResolve(reqType);
		throw new CompilerException(e.pos,"Expression "+expr+" is not auto-castable to type "+reqType);
	}
	public static Expr toExpr(Struct e, Type reqType, int pos, Node parent) {
		if( e.isPizzaCase() ) {
			// Pizza case may be casted to int or to itself or super-class
			PizzaCaseAttr case_attr;
			if( e.generated_from != null )
				case_attr = (PizzaCaseAttr)(e.generated_from).getAttr(attrPizzaCase);
			else
				case_attr = (PizzaCaseAttr)(e).getAttr(attrPizzaCase);
			if( case_attr == null )
				throw new RuntimeException("Internal error - can't find case_attr");
			e = Type.getRealType(reqType,e.type).clazz;
			if( !(reqType.isInteger() || e.instanceOf(reqType.clazz)) )
				throw new CompilerException(pos,"Pizza case "+e+" cannot be casted to type "+reqType);
			if( case_attr.casefields.length != 0 )
				throw new CompilerException(pos,"Empty constructor for pizza case "+e+" not found");
			if( reqType.isInteger() ) {
				Expr expr = (Expr)new ConstExpr(pos,Kiev.newInteger(case_attr.caseno)).resolve(reqType);
				if( reqType != Type.tpInt )
					expr = (Expr)new CastExpr(pos,reqType,expr).resolve(reqType);
				return expr;
			}
			// Now, check we need add type arguments
			Type tp = Type.getRealType(reqType,e.type);
			return (Expr)new NewExpr(pos,tp,Expr.emptyArray).resolve(reqType);
/*			if( case_attr != null && case_attr.casefields.length == 0 ) {
				Field f = (Field)((Struct)e).resolveName(nameTagSelf);
				if( f != null ) {
					Expr ex = new StaticFieldAccessExpr(pos,(Struct)e,(Field)f);
					ex.parent = parent;
					ex = ex.tryResolve(reqType);
					return ex;
				} else {
					throw new RuntimeException("Field "+nameTagSelf+" not found in cased class "+e);
				}
			}
*/		}
		throw new CompilerException(pos,"Expr "+e+" is not a class's case with no fields");
	}
}

public class WrapedExpr extends Expr {

	public Node	expr;
	public Type		base_type;
	public WrapedExpr(int pos, Node expr) {
		super(pos);
		this.expr = expr;
	}
	public WrapedExpr(int pos, Node expr, Type t) {
		super(pos);
		this.expr = expr;
		base_type = t;
	}
	public int		getPriority() { return 256; }
	public Type getType() {
		if( expr instanceof Type ) return Type.getRealType(base_type,(Type)expr);
		if( expr instanceof Struct ) return Type.getRealType(base_type,((Struct)expr).type);
		if( expr instanceof kiev.parser.ASTType ) return Type.getRealType(base_type,((kiev.parser.ASTType)expr).pass2());
		throw new CompilerException(pos,"Unknown wrapped node of class "+expr.getClass());
	}
	public Node resolve(Type reqType) {
		if( expr instanceof Type ) return expr;
		if( expr instanceof Struct ) return expr;
		if( expr instanceof kiev.parser.ASTType ) return ((kiev.parser.ASTType)expr).pass2();
		throw new CompilerException(pos,"Unknown wrapped node of class "+expr.getClass());
	}
}

public abstract class BooleanExpr extends Expr {

	public BooleanExpr(int pos) { super(pos); }

	public BooleanExpr(int pos, Node parent) { super(pos, parent); }

	public Type getType() { return Type.tpBoolean; }

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanExpr: "+this);
		PassInfo.push(this);
		try {
			CodeLabel label_true = Code.newLabel();
			CodeLabel label_false = Code.newLabel();

			generate_iftrue(label_true);
			Code.addConst(0);
			Code.addInstr(Instr.op_goto,label_false);
			Code.addInstr(Instr.set_label,label_true);
			Code.addConst(1);
			Code.addInstr(Instr.set_label,label_false);
			if( reqType == Type.tpVoid ) Code.addInstr(Instr.op_pop);
		} finally { PassInfo.pop(this); }
	}

	public abstract void generate_iftrue(CodeLabel label);
	public abstract void generate_iffalse(CodeLabel label);
}

public abstract class LvalueExpr extends Expr {

	public LvalueExpr(int pos) { super(pos); }

	public LvalueExpr(int pos, Node parent) { super(pos, parent); }

	public void generate(Type reqType) {
		PassInfo.push(this);
		try {
			generateLoad();
			if( reqType == Type.tpVoid )
				Code.addInstr(Instr.op_pop);
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return 256; }

	/** Just load value referenced by lvalue */
	public abstract void generateLoad();

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateLoadDup();

	/** Load info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateAccess();

	/** Stores value using previously duped info */
	public abstract void generateStore();

	/** Stores value using previously duped info, and put stored value in stack */
	public abstract void generateStoreDupValue();
}

public abstract class Statement extends Node {

	public static Statement[] emptyArray = new Statement[0];

	public Statement(int pos, Node parent) { super(pos, parent); }

	public void jjtAddChild(Node n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public void		generate(Type reqType) {
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation");
	}

	public Node	resolve(Type reqType) { return this; }

}

public interface SetBody {
	public boolean setBody(Statement body);
}

public class CompilerException extends RuntimeException {
	public int		pos;
	public Struct	clazz;
	public CompilerException(int pos, String msg) {
		super(msg);
		this.pos = pos;
		this.clazz = PassInfo.clazz;
	}
}

