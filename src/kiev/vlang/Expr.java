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
import kiev.parser.*;
import kiev.transf.*;

import kiev.vlang.Instr.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Expr.java,v 1.6.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.2 $
 *
 */


@node
@cfnode
public class ShadowExpr extends Expr {
	@ref public Expr expr;
	
	public ShadowExpr() {
	}
	public ShadowExpr(Expr expr) {
		this.expr = expr;
	}
	public Type getType() { return expr.getType(); }
	public void cleanup() {
		parent = null;
		expr   = null;
	}
	public ASTNode resolve(Type reqType) {
		expr = expr.resolveExpr(reqType);
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		expr.generate(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		return expr.toJava(dmp);
	}

}

/*
@node
@cfnode
public class StatExpr extends Expr implements SetBody {
	@att public Statement	stat;

	public StatExpr() {
	}

	public StatExpr(int pos, Statement stat) {
		super(pos);
		this.stat = stat;
	}

	public Type getType() { return Type.tpVoid; }

	public void cleanup() {
		parent=null;
		if( stat != null ) {
			stat.cleanup();
			stat = null;
		}
	}

	public ASTNode resolve(Type reqType) {
		if( isResolved() ) return this;
		if( stat == null )
			throw new CompilerException(pos,"Missed expression");
		stat = (Statement)stat.resolve(reqType);
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		stat.generate(reqType);
		if( reqType != Type.tpVoid ) {
			throw new CompilerException(pos,"Statement can't return a value");
		}
	}

	public Dumper toJava(Dumper dmp) {
		return stat.toJava(dmp);
	}

	public boolean setBody(Statement body) {
		this.stat = body;
		return true;
	}

}
*/

@node
@cfnode
public class ArrayLengthAccessExpr extends Expr {
	@att public Expr		array;

	public ArrayLengthAccessExpr() {
	}

	public ArrayLengthAccessExpr(int pos, Expr array) {
		super(pos);
		this.array = array;
	}

	public String toString() {
		if( array.getPriority() < opAccessPriority )
			return "("+array.toString()+").length";
		else
			return array.toString()+".length";
	}

	public Type getType() {
		return Type.tpInt;
	}

	public int getPriority() { return opAccessPriority; }

	public void cleanup() {
		parent=null;
		array.cleanup();
		array = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		setResolved(true);
		// Thanks to AccessExpr, that resolved all things for us
		return this;
	}

	public void generate(Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerLengthExpr: "+this);
		PassInfo.push(this);
		try {
			array.generate(null);
			Code.addInstr(Instr.op_arrlength);
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( array.getPriority() < opAccessPriority ) {
			dmp.append('(');
			array.toJava(dmp).append(").length").space();
		} else {
			array.toJava(dmp).append(".length").space();
		}
		return dmp;
	}
}

@node
@cfnode
public class AssignExpr extends LvalueExpr {
	@ref public AssignOperator	op;
	@att public Expr			lval;
	@att public Expr			value;

	public AssignExpr() {
	}

	public AssignExpr(int pos, AssignOperator op, Expr lval, Expr value) {
		super(pos);
		this.op = op;
		this.lval = lval;
		this.value = value;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( lval.getPriority() < opAssignPriority )
			sb.append('(').append(lval).append(')');
		else
			sb.append(lval);
		sb.append(op.image);
		if( value.getPriority() < opAssignPriority )
			sb.append('(').append(value).append(')');
		else
			sb.append(value);
		return sb.toString();
	}

	public Type getType() { return lval.getType(); }

	public int getPriority() { return opAssignPriority; }

	public void cleanup() {
		parent=null;
		lval.cleanup();
		lval = null;
		value.cleanup();
		value = null;
	}

	public Expr tryResolve(Type reqType) {
		setTryResolved(true);
		{
			Expr e = lval.tryResolve(reqType);
			if( e == null ) return null;
			lval = e;
			e = value.tryResolve(getType());
			if( e == null ) return null;
			value = e;
		}
		Type et1 = lval.getType();
		Type et2 = value.getType();
		if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !et1.isWrapper() && !et2.isWrapper()) {
			return (Expr)this.resolve(reqType);
		}
		else if( op == AssignOperator.Assign2 && et1.isWrapper() && et2.isInstanceOf(et1)) {
			return (Expr)this.resolve(reqType);
		}
		else if( op == AssignOperator.AssignAdd && et1 == Type.tpString ) {
			return (Expr)this.resolve(reqType);
		}
		else if( ( et1.isNumber() && et2.isNumber() ) &&
			(    op==AssignOperator.AssignAdd
			||   op==AssignOperator.AssignSub
			||   op==AssignOperator.AssignMul
			||   op==AssignOperator.AssignDiv
			||   op==AssignOperator.AssignMod
			)
		) {
			return (Expr)this.resolve(null);
		}
		else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
			(    op==AssignOperator.AssignLeftShift
			||   op==AssignOperator.AssignRightShift
			||   op==AssignOperator.AssignUnsignedRightShift
			)
		) {
			return (Expr)this.resolve(null);
		}
		else if( ( et1.isInteger() && et2.isInteger() ) &&
			(    op==AssignOperator.AssignBitOr
			||   op==AssignOperator.AssignBitXor
			||   op==AssignOperator.AssignBitAnd
			)
		) {
				return (Expr)this.resolve(null);
		}
		else if( ( et1.isBoolean() && et2.isBoolean() ) &&
			(    op==AssignOperator.AssignBitOr
			||   op==AssignOperator.AssignBitXor
			||   op==AssignOperator.AssignBitAnd
			)
		) {
				return (Expr)this.resolve(null);
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,lval,value};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				Expr e;
				e = new CallAccessExpr(pos,parent,lval,opt.method,new Expr[]{value}).tryResolve(reqType);
				if( e != null ) return e;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (op != AssignOperator.Assign2) {
			if (et1.isWrapper()) {
				Expr e = new AssignExpr(pos,op,et1.makeWrappedAccess(lval),value).tryResolve(reqType);
				if (e != null) return e;
			}
			if (et2.isWrapper()) {
				Expr e = new AssignExpr(pos,op,lval,et2.makeWrappedAccess(value)).tryResolve(reqType);
				if (e != null) return e;
			}
			if (et1.isWrapper() && et2.isWrapper()) {
				Expr e = new AssignExpr(pos,op,et1.makeWrappedAccess(lval),et2.makeWrappedAccess(value)).tryResolve(reqType);
				if (e != null) return e;
			}
		}
		return null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) {
			setNodeTypes();
			return this;
		}
		if( !isTryResolved() ) {
			Expr e = tryResolve(reqType);
			if( e != null ) return e;
			return this;
		}
		PassInfo.push(this);
		try {
			ASTNode lv = lval.resolve(null);
			if( !(lv instanceof LvalueExpr) )
				throw new RuntimeException("Can't assign to "+lv+": lvalue requared");
			if( (lv instanceof VarAccessExpr) && ((VarAccessExpr)lv).var.isNeedProxy() ) {
				// Check that we in local/anonymouse class, thus var need RefProxy
				Var var = ((VarAccessExpr)lv).var;
				ASTNode p = var.parent;
				while( !(p instanceof Struct) ) p = p.parent;
				if( !((Struct)p).equals(PassInfo.clazz) && !var.isNeedRefProxy() ) {
					throw new RuntimeException("Unsupported operation");
					//var.setNeedRefProxy(true);
					//Field vf = (Field)PassInfo.clazz.resolveName(var.name.name);
					//vf.type = Type.getProxyType(var.type);
				}
			}
			lval = (Expr)lv;
			Type t1 = lval.getType();
			if( op==AssignOperator.AssignAdd && t1==Type.tpString ) {
				op = AssignOperator.Assign;
				value = new BinaryExpr(pos,BinaryOperator.Add,new ShadowExpr(lval),value);
			}
			value = value.resolveExpr(t1);
			Type t2 = value.getType();
			if( op==AssignOperator.AssignLeftShift || op==AssignOperator.AssignRightShift || op==AssignOperator.AssignUnsignedRightShift ) {
				if( !t2.isIntegerInCode() ) {
					value = (Expr)new CastExpr(pos,Type.tpInt,value).resolve(Type.tpInt);
				}
			}
			else if( !t1.equals(t2) ) {
				if( t2.isCastableTo(t1) ) {
					value = (Expr)new CastExpr(pos,t1,value).resolve(t1);
				} else {
					throw new RuntimeException("Value of type "+t2+" can't be assigned to "+lval);
				}
			}
			setNodeTypes();

			// Set violation of the field
			if( lval instanceof StaticFieldAccessExpr
			 || (
			 		lval instanceof AccessExpr
				 && ((AccessExpr)lval).obj instanceof VarAccessExpr
				 &&	((VarAccessExpr)((AccessExpr)lval).obj).var.name.equals(nameThis)
				)
			) {
				if( PassInfo.method != null && PassInfo.method.isInvariantMethod() )
					Kiev.reportError(pos,"Side-effect in invariant condition");
				if( PassInfo.method != null && !PassInfo.method.isInvariantMethod() ) {
					if( lval instanceof StaticFieldAccessExpr )
						PassInfo.method.addViolatedField( ((StaticFieldAccessExpr)lval).var );
					else
						PassInfo.method.addViolatedField( ((AccessExpr)lval).var );
				}
			}


		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	private void setNodeTypes() {
		switch(lval) {
		case VarAccessExpr:
			NodeInfoPass.setNodeValue(((VarAccessExpr)lval).var,value);
			break;
		case AccessExpr:
			if !(((AccessExpr)lval).obj instanceof ThisExpr)
				break;
			NodeInfoPass.setNodeValue(((AccessExpr)lval).var,value);
			break;
		case StaticFieldAccessExpr:
			NodeInfoPass.setNodeValue(((StaticFieldAccessExpr)lval).var,value);
			break;
		}
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating AssignExpr: "+this);
		PassInfo.push(this);
		try {
			LvalueExpr lval = (LvalueExpr)this.lval;
			if( reqType != Type.tpVoid ) {
				if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) ) {
					lval.generateLoadDup();
					value.generate(null);
					Code.addInstr(op.instr);
					lval.generateStoreDupValue();
				} else {
					lval.generateAccess();
					if( Kiev.argtype != null && value.getType()==Type.tpNull )
						value.generate(Type.getRealType(Kiev.argtype,lval.getType()));
					else
						value.generate(null);
					lval.generateStoreDupValue();
				}
			} else {
				if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) ) {
					lval.generateLoadDup();
					value.generate(null);
					Code.addInstr(op.instr);
					lval.generateStore();
				} else {
					lval.generateAccess();
					if( Kiev.argtype != null && value.getType()==Type.tpNull )
						value.generate(Type.getRealType(Kiev.argtype,lval.getType()));
					else
						value.generate(null);
					lval.generateStore();
				}
			}
		} finally { PassInfo.pop(this); }
	}

	/** Just load value referenced by lvalue */
	public void generateLoad() {
		PassInfo.push(this);
		try {
			LvalueExpr lval = (LvalueExpr)this.lval;
			lval.generateLoadDup();
			if( Kiev.argtype != null && value.getType()==Type.tpNull )
				value.generate(Type.getRealType(Kiev.argtype,lval.getType()));
			else
				value.generate(null);
			if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
				Code.addInstr(op.instr);
			lval.generateStoreDupValue();
		} finally { PassInfo.pop(this); }
	}

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while)
	*/
	public void generateLoadDup() {
		PassInfo.push(this);
		try {
			throw new RuntimeException("Too complex lvalue expression "+this);
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		PassInfo.push(this);
		try {
			throw new RuntimeException("Too complex lvalue expression "+this);
		} finally { PassInfo.pop(this); }
	}

	/** Stores value using previously duped info */
	public void generateStore() {
		LvalueExpr lval = (LvalueExpr)this.lval;
		PassInfo.push(this);
		try {
			lval.generateLoadDup();
			if( Kiev.argtype != null && value.getType()==Type.tpNull )
				value.generate(Type.getRealType(Kiev.argtype,lval.getType()));
			else
				value.generate(null);
			if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
				Code.addInstr(op.instr);
			lval.generateStore();
		} finally { PassInfo.pop(this); }
	}

	/** Stores value using previously duped info, and put stored value in stack */
	public void generateStoreDupValue() {
		PassInfo.push(this);
		try {
			LvalueExpr lval = (LvalueExpr)this.lval;
			lval.generateLoadDup();
			if( Kiev.argtype != null && value.getType()==Type.tpNull )
				value.generate(Type.getRealType(Kiev.argtype,lval.getType()));
			else
				value.generate(null);
			if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
				Code.addInstr(op.instr);
			lval.generateStoreDupValue();
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( lval.getPriority() < opAssignPriority ) {
			dmp.append('(').append(lval).append(')');
		} else {
			dmp.append(lval);
		}
		if (op != AssignOperator.Assign2)
			dmp.space().append(op.image).space();
		else
			dmp.space().append(AssignOperator.Assign.image).space();
		if( value.getPriority() < opAssignPriority ) {
			dmp.append('(');
			dmp.append(value).append(')');
		} else {
			dmp.append(value);
		}
		return dmp;
	}
}


@node
@cfnode
public class InitializeExpr extends AssignExpr {
    public boolean	of_wrapper;

	public InitializeExpr() {
	}

	public InitializeExpr(int pos, AssignOperator op, Expr lval, Expr value, boolean of_wrapper) {
		super(pos,op,lval,value);
		this.of_wrapper = of_wrapper;
	}

	public Expr tryResolve(Type reqType) {
		setTryResolved(true);
		if (!(op==AssignOperator.Assign || op==AssignOperator.Assign2))
			return null;
		{
			Expr e = lval.tryResolve(reqType);
			if( e == null ) return null;
			lval = e;
			e = value.tryResolve(getType());
			if( e == null ) return null;
			value = e;
		}
		Type et1 = lval.getType();
		Type et2 = value.getType();
		if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !et1.isWrapper() && !et2.isWrapper()) {
			return (Expr)this.resolve(reqType);
		}
		else if((of_wrapper || op == AssignOperator.Assign2) && et1.isWrapper() && (et2 == Type.tpNull || et2.isInstanceOf(et1))) {
			return (Expr)this.resolve(reqType);
		}
		// Try wrapped classes
		if (op != AssignOperator.Assign2) {
			if (et2.isWrapper()) {
				Expr e = new InitializeExpr(pos,op,lval,et2.makeWrappedAccess(value),of_wrapper).tryResolve(reqType);
				if (e != null) return e;
			}
		}
		return null;
	}

}



@node
@cfnode
public class BinaryExpr extends Expr {
	@ref public BinaryOperator		op;
	@att public Expr				expr1;
	@att public Expr				expr2;

	public BinaryExpr() {
	}

	public BinaryExpr(int pos, BinaryOperator op, Expr expr1, Expr expr2) {
		super(pos);
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( expr1.getPriority() < op.priority )
			sb.append('(').append(expr1).append(')');
		else
			sb.append(expr1);
		sb.append(op.image);
		if( expr2.getPriority() < op.priority )
			sb.append('(').append(expr2).append(')');
		else
			sb.append(expr2);
		return sb.toString();
	}

	public int getPriority() { return op.priority; }

	public Type getType() {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( op==BinaryOperator.BitOr || op==BinaryOperator.BitXor || op==BinaryOperator.BitAnd ) {
			if( (t1.isInteger() && t2.isInteger()) || (t1.isBoolean() && t2.isBoolean()) ) {
				if( t1==Type.tpLong || t2==Type.tpLong ) return Type.tpLong;
				if( t1.isAutoCastableTo(Type.tpBoolean) && t2.isAutoCastableTo(Type.tpBoolean) ) return Type.tpBoolean;
				return Type.tpInt;
			}
		}
		else if( op==BinaryOperator.LeftShift || op==BinaryOperator.RightShift || op==BinaryOperator.UnsignedRightShift ) {
			if( t2.isInteger() ) {
				if( t1 == Type.tpLong ) return Type.tpLong;
				if( t1.isInteger() )	return Type.tpInt;
			}
		}
		else if( op==BinaryOperator.Add || op==BinaryOperator.Sub || op==BinaryOperator.Mul || op==BinaryOperator.Div || op==BinaryOperator.Mod ) {
			// Special case for '+' operator if one arg is a String
			if( op==BinaryOperator.Add && t1.equals(Type.tpString) || t2.equals(Type.tpString) ) return Type.tpString;

			if( t1.isNumber() && t2.isNumber() ) {
				if( t1==Type.tpDouble || t2==Type.tpDouble ) return Type.tpDouble;
				if( t1==Type.tpFloat || t2==Type.tpFloat ) return Type.tpFloat;
				if( t1==Type.tpLong || t2==Type.tpLong ) return Type.tpLong;
				return Type.tpInt;
			}
		}
		Expr e = tryResolve(null);
		if( e == null )
			Kiev.reportError(pos,"Type of binary operation "+op.image+" between "+expr1+" and "+expr2+" unknown, types are "+t1+" and "+t2);
		else
			return e.getType();
		return Type.tpVoid;
	}

	public void cleanup() {
		parent=null;
		expr1.cleanup();
		expr1 = null;
		expr2.cleanup();
		expr2 = null;
	}

	public Expr tryResolve(Type reqType) {
		setTryResolved(true);
		{
			Expr e = expr1.tryResolve(null);
			if( e == null ) return null;
			expr1 = e;
			e = expr2.tryResolve(null);
			if( e == null ) return null;
			expr2 = e;
		}
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( op == BinaryOperator.Add
			&& ( et1 == Type.tpString || et2 == Type.tpString ||
			    (et1.isWrapper() && et1.getWrappedType() == Type.tpString) ||
			    (et2.isWrapper() && et2.getWrappedType() == Type.tpString)
			   )
		) {
			if( expr1 instanceof StringConcatExpr ) {
				StringConcatExpr sce = (StringConcatExpr)expr1;
				Expr e = (Expr)expr2;
				if (et2.isWrapper()) e = et2.makeWrappedAccess(e);
				sce.appendArg((Expr)e.resolve(null));
				trace(Kiev.debugStatGen,"Adding "+e+" to StringConcatExpr, now ="+sce);
				return (Expr)sce.resolve(Type.tpString);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				Expr e1 = (Expr)expr1;
				if (et1.isWrapper()) e1 = et1.makeWrappedAccess(e1);
				sce.appendArg((Expr)e1.resolve(null));
				Expr e2 = (Expr)expr2;
				if (et2.isWrapper()) e2 = et2.makeWrappedAccess(e2);
				sce.appendArg((Expr)e2.resolve(null));
				trace(Kiev.debugStatGen,"Rewriting "+e1+"+"+e2+" as StringConcatExpr");
				return (Expr)sce.resolve(Type.tpString);
			}
		}
		else if( ( et1.isNumber() && et2.isNumber() ) &&
			(    op==BinaryOperator.Add
			||   op==BinaryOperator.Sub
			||   op==BinaryOperator.Mul
			||   op==BinaryOperator.Div
			||   op==BinaryOperator.Mod
			)
		) {
			return (Expr)this.resolve(null);
		}
		else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
			(    op==BinaryOperator.LeftShift
			||   op==BinaryOperator.RightShift
			||   op==BinaryOperator.UnsignedRightShift
			)
		) {
			return (Expr)this.resolve(null);
		}
		else if( ( (et1.isInteger() && et2.isInteger()) || (et1.isBoolean() && et2.isBoolean()) ) &&
			(    op==BinaryOperator.BitOr
			||   op==BinaryOperator.BitXor
			||   op==BinaryOperator.BitAnd
			)
		) {
				return (Expr)this.resolve(null);
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				Expr e;
				if( opt.method.isStatic() )
					e = new CallExpr(pos,parent,opt.method,new Expr[]{expr1,expr2}).tryResolve(reqType);
				else
					e = new CallAccessExpr(pos,parent,expr1,opt.method,new Expr[]{expr2}).tryResolve(reqType);
				if( e != null ) return e;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (et1.isWrapper()) {
			Expr e = new BinaryExpr(pos,op,et1.makeWrappedAccess(expr1),expr2).tryResolve(reqType);
			if (e != null) return e;
		}
		if (et2.isWrapper()) {
			Expr e = new BinaryExpr(pos,op,expr1,et2.makeWrappedAccess(expr2)).tryResolve(reqType);
			if (e != null) return e;
		}
		if (et1.isWrapper() && et2.isWrapper()) {
			Expr e = new BinaryExpr(pos,op,et1.makeWrappedAccess(expr1),et2.makeWrappedAccess(expr2)).tryResolve(reqType);
			if (e != null) return e;
		}
		return null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( !isTryResolved() ) {
			Expr e = tryResolve(reqType);
			if( e != null ) return e;
			return this;
		}
		PassInfo.push(this);
		try {
			expr1 = (Expr)expr1.resolve(null);
			expr2 = (Expr)expr2.resolve(null);

			Type rt = getType();
			Type t1 = expr1.getType();
			Type t2 = expr2.getType();

			// Special case for '+' operator if one arg is a String
			if( op==BinaryOperator.Add && expr1.getType().equals(Type.tpString) || expr2.getType().equals(Type.tpString) ) {
				if( expr1 instanceof StringConcatExpr ) {
					StringConcatExpr sce = (StringConcatExpr)expr1;
					sce.appendArg(expr2);
					trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
					return sce.resolve(Type.tpString);
				} else {
					StringConcatExpr sce = new StringConcatExpr(pos);
					sce.appendArg(expr1);
					sce.appendArg(expr2);
					trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
					return sce.resolve(Type.tpString);
				}
			}

			if( op==BinaryOperator.LeftShift || op==BinaryOperator.RightShift || op==BinaryOperator.UnsignedRightShift ) {
				if( !t2.isIntegerInCode() ) {
					expr2 = (Expr)new CastExpr(pos,Type.tpInt,expr2).resolve(Type.tpInt);
				}
			} else {
				if( !rt.equals(t1) && t1.isCastableTo(rt) ) {
					expr1 = (Expr)new CastExpr(pos,rt,expr1).resolve(null);
				}
				if( !rt.equals(t2) && t2.isCastableTo(rt) ) {
					expr2 = (Expr)new CastExpr(pos,rt,expr2).resolve(null);
				}
			}

			// Check if both expressions are constant
			if( expr1.isConstantExpr() && expr2.isConstantExpr() ) {
				Number val1 = (Number)expr1.getConstValue();
				Number val2 = (Number)expr2.getConstValue();
				if( op == BinaryOperator.BitOr ) {
					if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() | val2.longValue()).resolve(null);
					else if( val1 instanceof Integer || val2 instanceof Integer )
						return new ConstIntExpr(val1.intValue() | val2.intValue()).resolve(null);
					else if( val1 instanceof Short || val2 instanceof Short )
						return new ConstShortExpr(val1.shortValue() | val2.shortValue()).resolve(null);
					else if( val1 instanceof Byte || val2 instanceof Byte )
						return new ConstByteExpr(val1.byteValue() | val2.byteValue()).resolve(null);
				}
				else if( op == BinaryOperator.BitXor ) {
					if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() ^ val2.longValue()).resolve(null);
					else if( val1 instanceof Integer || val2 instanceof Integer )
						return new ConstIntExpr(val1.intValue() ^ val2.intValue()).resolve(null);
					else if( val1 instanceof Short || val2 instanceof Short )
						return new ConstShortExpr(val1.shortValue() ^ val2.shortValue()).resolve(null);
					else if( val1 instanceof Byte || val2 instanceof Byte )
						return new ConstByteExpr(val1.byteValue() ^ val2.byteValue()).resolve(null);
				}
				else if( op == BinaryOperator.BitAnd ) {
					if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() & val2.longValue()).resolve(null);
					else if( val1 instanceof Integer || val2 instanceof Integer )
						return new ConstIntExpr(val1.intValue() & val2.intValue()).resolve(null);
					else if( val1 instanceof Short || val2 instanceof Short )
						return new ConstShortExpr(val1.shortValue() & val2.shortValue()).resolve(null);
					else if( val1 instanceof Byte || val2 instanceof Byte )
						return new ConstByteExpr(val1.byteValue() & val2.byteValue()).resolve(null);
				}
				else if( op == BinaryOperator.LeftShift ) {
					if( val1 instanceof Long )
						return new ConstLongExpr(val1.longValue() << val2.intValue()).resolve(null);
					else
						return new ConstIntExpr(val1.intValue() << val2.intValue()).resolve(null);
				}
				else if( op == BinaryOperator.RightShift ) {
					if( val1 instanceof Long )
						return new ConstLongExpr(val1.longValue() >> val2.intValue()).resolve(null);
					else if( val1 instanceof Integer )
						return new ConstIntExpr(val1.intValue() >> val2.intValue()).resolve(null);
					else if( val1 instanceof Short )
						return new ConstShortExpr(val1.shortValue() >> val2.intValue()).resolve(null);
					else if( val1 instanceof Byte )
						return new ConstByteExpr(val1.byteValue() >> val2.intValue()).resolve(null);
				}
				else if( op == BinaryOperator.UnsignedRightShift ) {
					if( val1 instanceof Long )
						return new ConstLongExpr(val1.longValue() >>> val2.intValue()).resolve(null);
					else if( val1 instanceof Integer )
						return new ConstIntExpr(val1.intValue() >>> val2.intValue()).resolve(null);
					else if( val1 instanceof Short )
						return new ConstShortExpr(val1.shortValue() >>> val2.intValue()).resolve(null);
					else if( val1 instanceof Byte )
						return new ConstByteExpr(val1.byteValue() >>> val2.intValue()).resolve(null);
				}
				else if( op == BinaryOperator.Add ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstDoubleExpr(val1.doubleValue() + val2.doubleValue()).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstFloatExpr(val1.floatValue() + val2.floatValue()).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() + val2.longValue()).resolve(null);
					else
						return new ConstIntExpr(val1.intValue() + val2.intValue()).resolve(null);
				}
				else if( op == BinaryOperator.Sub ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstDoubleExpr(val1.doubleValue() - val2.doubleValue()).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstFloatExpr(val1.floatValue() - val2.floatValue()).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() - val2.longValue()).resolve(null);
					else
						return new ConstIntExpr(val1.intValue() - val2.intValue()).resolve(null);
				}
				else if( op == BinaryOperator.Mul ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstDoubleExpr(val1.doubleValue() * val2.doubleValue()).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstFloatExpr(val1.floatValue() * val2.floatValue()).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() * val2.longValue()).resolve(null);
					else
						return new ConstIntExpr(val1.intValue() * val2.intValue()).resolve(null);
				}
				else if( op == BinaryOperator.Div ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstDoubleExpr(val1.doubleValue() / val2.doubleValue()).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstFloatExpr(val1.floatValue() / val2.floatValue()).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() / val2.longValue()).resolve(null);
					else
						return new ConstIntExpr(val1.intValue() / val2.intValue()).resolve(null);
				}
				else if( op == BinaryOperator.Mod ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstDoubleExpr(val1.doubleValue() % val2.doubleValue()).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstFloatExpr(val1.floatValue() % val2.floatValue()).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstLongExpr(val1.longValue() % val2.longValue()).resolve(null);
					else
						return new ConstIntExpr(val1.intValue() % val2.intValue()).resolve(null);
				}
			}

		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BinaryExpr: "+this);
		PassInfo.push(this);
		try {
			expr1.generate(null);
			expr2.generate(null);
			Code.addInstr(op.instr);
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( expr1.getPriority() < op.priority ) {
			dmp.append('(').append(expr1).append(')');
		} else {
			dmp.append(expr1);
		}
		dmp.append(op.image);
		if( expr2.getPriority() < op.priority ) {
			dmp.append('(').append(expr2).append(')');
		} else {
			dmp.append(expr2);
		}
		return dmp;
	}
}

@node
@cfnode
public class StringConcatExpr extends Expr {
	@att public final NArr<Expr>	args;

	@ref public static Struct clazzStringBuffer;
	@ref public static Method clazzStringBufferToString;
	@ref public static Method clazzStringBufferInit;

	static {
		try {
		clazzStringBuffer = Env.getStruct(ClazzName.fromToplevelName(KString.from("java.lang.StringBuffer"),false) );
		if( clazzStringBuffer == null )
			throw new RuntimeException("Core class java.lang.StringBuffer not found");
		clazzStringBufferToString = (Method)clazzStringBuffer.resolveMethod(
			KString.from("toString"),KString.from("()Ljava/lang/String;"));
		clazzStringBufferInit = (Method)clazzStringBuffer.resolveMethod(
			KString.from("<init>"),KString.from("()V"));
		} catch(Exception e ) {
			throw new Error("Can't initialize: "+e.getMessage());
		}
	}

	public StringConcatExpr() {
	}

	public StringConcatExpr(int pos) {
		super(pos);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < args.length; i++) {
			sb.append(args[i].toString());
			if( i < args.length-1 )
				sb.append('+');
		}
		return sb.toString();
	}

	public Type getType() {
		return Type.tpString;
	}

	public int getPriority() { return opAddPriority; }

	public void cleanup() {
		parent=null;
		foreach(ASTNode n; args; args!=null) n.cleanup();
		args = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		// Resolving of args done by BinaryExpr +
		// just add items to clazz's CP
		PassInfo.push(this);
		PassInfo.pop(this);
		setResolved(true);
		return this;
	}

	public void appendArg(Expr expr) {
		args.append(expr);
	}

	static final KString sigI = KString.from("(I)Ljava/lang/StringBuffer;");
	static final KString sigJ = KString.from("(J)Ljava/lang/StringBuffer;");
	static final KString sigZ = KString.from("(Z)Ljava/lang/StringBuffer;");
	static final KString sigC = KString.from("(C)Ljava/lang/StringBuffer;");
	static final KString sigF = KString.from("(F)Ljava/lang/StringBuffer;");
	static final KString sigD = KString.from("(D)Ljava/lang/StringBuffer;");
	static final KString sigObj = KString.from("(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
	static final KString sigStr = KString.from("(Ljava/lang/String;)Ljava/lang/StringBuffer;");
	static final KString sigArrC = KString.from("([C)Ljava/lang/StringBuffer;");
	public Method getMethodFor(Expr expr) {
		Type t = Type.getRealType(Kiev.argtype,expr.getType());
		KString sig = null;
		switch(t.java_signature.byteAt(0)) {
		case 'B':
		case 'S':
		case 'I': sig = sigI; break;
		case 'J': sig = sigJ; break;
		case 'Z': sig = sigZ; break;
		case 'C': sig = sigC; break;
		case 'F': sig = sigF; break;
		case 'D': sig = sigD; break;
		case 'L':
		case 'A':
		case '&':
		case 'R':
			if(t == Type.tpString)
				sig = sigStr;
			else
				sig = sigObj;
			break;
		case '[':
			if(t.java_signature.byteAt(1)=='C')
				sig = sigArrC;
			else
				sig = sigObj;
			break;
		}
		Method m = clazzStringBuffer.resolveMethod(KString.from("append"),sig);
		if( m == null )
			Kiev.reportError(expr.pos,"Unknown method for StringBuffer");
		return m;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating StringConcatExpr: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_new,clazzStringBuffer.type);
			Code.addInstr(op_dup);
			Code.addInstr(op_call,clazzStringBufferInit,false);
			for(int i=0; i < args.length; i++) {
				args[i].generate(null);
				Code.addInstr(op_call,getMethodFor(args[i]),false);
			}
			Code.addInstr(op_call,clazzStringBufferToString,false);
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
//		dmp.append("((new java.lang.StringBuffer())");
//		for(int i=0; i < args.length; i++) {
//			dmp.append(".append(");
//			args[i].toJava(dmp);
//			dmp.append(')');
//		}
//		dmp.append(".toString())");
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 ) dmp.append('+');
		}
		return dmp;
	}
}

@node
@cfnode
public class CommaExpr extends Expr {
	@att public final NArr<Expr>	exprs;

	public CommaExpr() {
	}

	public CommaExpr(int pos, Expr[] exprs) {
		super(pos);
		this.exprs.addAll(exprs);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < exprs.length; i++) {
			sb.append(exprs[i]);
			if( i < exprs.length-1 )
				sb.append(',');
		}
		return sb.toString();
	}

	public KString getName() { return KString.Empty; };

	public Type getType() { return exprs[exprs.length-1].getType(); }

	public int getPriority() { return 0; }

	public void cleanup() {
		parent=null;
		exprs.cleanup();
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			for(int i=0; i < exprs.length; i++) {
				if( i < exprs.length-1) {
					exprs[i] = exprs[i].resolveExpr(Type.tpVoid);
					exprs[i].setGenVoidExpr(true);
				} else {
					exprs[i] = exprs[i].resolveExpr(reqType);
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		PassInfo.push(this);
		try {
			for(int i=0; i < exprs.length; i++) {
				if( i < exprs.length-1 )
					exprs[i].generate(Type.tpVoid);
				else
					exprs[i].generate(reqType);
			}
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		for(int i=0; i < exprs.length; i++) {
			exprs[i].toJava(dmp);
			if( i < exprs.length-1 )
				dmp.append(',');
		}
		return dmp;
	}
}

@node
@cfnode
public class BlockExpr extends Expr implements ScopeOfNames, ScopeOfMethods {

	@att public final NArr<ASTNode>		stats;
	@ref public final NArr<Var>			vars;
	@att public final NArr<ASTNode>		members;
	@att public       Expr				res;

	public BlockExpr() {
	}

	public BlockExpr(int pos, ASTNode parent) {
		super(pos, parent);
	}

	public Statement addStatement(Statement st) {
		stats.append(st);
		return st;
	}

	public void setExpr(Expr res) {
		this.res = res;
	}

	public Var addVar(Var var) {
		foreach(Var v; vars; v.name.equals(var.name) ) {
			Kiev.reportWarning(pos,"Variable "+var.name+" already declared in this scope");
		}
		vars.append(var);
		return var;
	}
	
	public Type getType() {
		if (res == null) return Type.tpVoid;
		return res.getType();
	}

	public int		getPriority() { return 255; }

	public rule resolveNameR(ASTNode@ node, ResInfo info, KString name)
		ASTNode@ n;
	{
		n @= vars,
		{
			((Var)n).name.equals(name),
			node ?= n
		;	n.isForward(),
			info.enterForward(n) : info.leaveForward(n),
			n.getType().resolveNameAccessR(node,info,name)
		}
	;	n @= members,
		{	n instanceof Struct,
			name.equals(((Struct)n).name.short_name),
			node ?= n
		;	n instanceof Typedef,
			name.equals(((Typedef)n).name),
			node ?= ((Typedef)n).type
		}
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		Var@ n;
	{
		n @= vars,
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
	}

	public ASTNode resolve(Type reqType) {
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			resolveBlockStats();
			if (res != null) {
				res = res.resolveExpr(reqType);
			}
		} finally {
			ScopeNodeInfoVector nip_state = NodeInfoPass.popState();
			nip_state = NodeInfoPass.cleanInfoForVars(nip_state,vars.toArray());
			NodeInfoPass.addInfo(nip_state);
			PassInfo.pop(this);
		}
		return this;
	}

	public void resolveBlockStats() {
		for(int i=0; i < stats.length; i++) {
			try {
				if( stats[i] instanceof Statement ) {
					stats[i] = (Statement)((Statement)stats[i]).resolve(Type.tpVoid);
					if( stats[i].isAbrupted() ) {
						Kiev.reportError(stats[i].pos,"Abrupted statement in BockExpr");
					}
				}
				else if( stats[i] instanceof ASTVarDecls ) {
					ASTVarDecls vdecls = (ASTVarDecls)stats[i];
					// TODO: check flags for vars
					int flags = vdecls.modifiers.getFlags();
					Type type = ((TypeRef)vdecls.type).getType();
					ASTNode[] vstats = ASTNode.emptyArray;
					for(int j=0; j < vdecls.vars.length; j++) {
						ASTVarDecl vdecl = (ASTVarDecl)vdecls.vars[j];
						KString vname = vdecl.name.name;
						Type tp = type;
						for(int k=0; k < vdecl.dim; k++) tp = Type.newArrayType(tp);
						DeclStat vstat;
						if( vdecl.init != null ) {
							if (!type.isWrapper() || vdecl.of_wrapper)
								vstat = (Statement)new DeclStat(
									vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags),vdecl.init);
							else
								vstat = (Statement)new DeclStat(
									vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags),
									new NewExpr(vdecl.init.pos,type,new Expr[]{vdecl.init}));
						}
						else if( vdecl.dim == 0 && type.isWrapper() && !vdecl.of_wrapper)
							vstat = (Statement)new DeclStat(vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags)
								,new NewExpr(vdecl.pos,type,Expr.emptyArray));
						else
							vstat = (Statement)new DeclStat(vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags));
						vstat = (DeclStat)vstat.resolve(Type.tpVoid);
						vstats = (ASTNode[])Arrays.append(vstats,vstat);
					}
					stats[i] = vstats[0];
					for(int j=1; j < vstats.length; j++, i++) {
						stats.insert(vstats[j],i+1);
					}
				}
				else if( stats[i] instanceof Struct ) {
					Struct decl = (Struct)stats[i];
					TypeDeclStat tds = new TypeDeclStat(decl.pos,this);
					if( PassInfo.method==null || PassInfo.method.isStatic())
						decl.setStatic(true);
					ExportJavaTop exporter = new ExportJavaTop();
					decl.setLocal(true);
					tds.struct = decl;
					exporter.pass1(decl);
					exporter.pass1_1(decl);
					exporter.pass2(decl);
					exporter.pass2_2(decl);
					exporter.pass3(decl);
					tds.struct.autoProxyMethods();
					tds.struct.resolveFinalFields(false);
					stats[i] = tds;
					stats[i] = tds.resolve(null);
					members.append(tds.struct);
				}
				else
					Kiev.reportError(stats[i].pos,"Unknown kind of statement/declaration "+stats[i].getClass());
			} catch(Exception e ) {
				Kiev.reportError(stats[i].pos,e);
			}
		}
	}


	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockExpr ("+vars.size()+" vars)");
		PassInfo.push(this);
		try {
			for(int i=0; i < stats.length; i++) {
				try {
					((Statement)stats[i]).generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(stats[i].getPos(),e);
				}
			}
			if (res != null) {
				try {
					res.generate(reqType);
				} catch(Exception e ) {
					Kiev.reportError(res.getPos(),e);
				}
			}
			Code.removeVars(vars.toArray());
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		foreach(ASTNode n; stats; n!=null) n.cleanup();
		stats = null;
		foreach(ASTNode n; vars; n!=null) n.cleanup();
		vars = null;
		if (res != null) {
			res.cleanup();
			res = null;
		}
	}

	public String toString() {
		Dumper dmp = new Dumper();
		dmp.append("({").space();
		for(int i=0; i < stats.length; i++)
			stats[i].toJava(dmp).space();
		if (res != null)
			res.toJava(dmp);
		dmp.space().append("})");
		return dmp.toString();
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append("({").newLine(1);
		for(int i=0; i < stats.length; i++)
			stats[i].toJava(dmp).newLine();
		if (res != null)
			res.toJava(dmp);
		dmp.newLine(-1).append("})");
		return dmp;
	}

}

@node
@cfnode
public class UnaryExpr extends Expr {
	@ref public Operator			op;
	@att public Expr				expr;

	public UnaryExpr() {
	}

	public UnaryExpr(int pos, Operator op, Expr expr) {
		super(pos);
		this.op = op;
		this.expr = expr;
	}

	public String toString() {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr )
			if( expr.getPriority() < op.priority )
				return "("+expr.toString()+")"+op.image;
			else
				return expr.toString()+op.image;
		else
			if( expr.getPriority() < op.priority )
				return op.image+"("+expr.toString()+")";
			else
				return op.image+expr.toString();
	}

	public Type getType() {
		return expr.getType();
	}

	public int getPriority() { return op.priority; }

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

	public Expr tryResolve(Type reqType) {
		setTryResolved(true);
		ASTNode ast = expr.tryResolve(reqType);
		if( ast == null )
			throw new CompilerException(pos,"Unresolved expression "+this);
		expr = (Expr)ast;
		Type et = expr.getType();
		if( et.isNumber() &&
			(  op==PrefixOperator.PreIncr
			|| op==PrefixOperator.PreDecr
			|| op==PostfixOperator.PostIncr
			|| op==PostfixOperator.PostDecr
			)
		) {
			return (Expr)new IncrementExpr(pos,op,expr).tryResolve(reqType);
		}
		if( et.isAutoCastableTo(Type.tpBoolean) &&
			(  op==PrefixOperator.PreIncr
			|| op==PrefixOperator.BooleanNot
			)
		) {
			return (Expr)new BooleanNotExpr(pos,expr).tryResolve(Type.tpBoolean);
		}
		if( et.isNumber() &&
			(  op==PrefixOperator.Pos
			|| op==PrefixOperator.Neg
			)
		) {
			return (Expr)this.resolve(reqType);
		}
		if( et.isInteger() && op==PrefixOperator.BitNot ) {
			return (Expr)this.resolve(reqType);
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			if (PassInfo.clazz != null && opt.method != null && opt.method.type.args.length == 1) {
				if ( !PassInfo.clazz.instanceOf((Struct)opt.method.parent) )
					continue;
			}
			Type[] tps = new Type[]{null,et};
			ASTNode[] argsarr = new ASTNode[]{null,expr};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				Expr e;
				if ( opt.method.isStatic() || opt.method.type.args.length == 1)
					e = new CallExpr(pos,parent,opt.method,new Expr[]{expr}).tryResolve(reqType);
				else
					e = new CallAccessExpr(pos,parent,expr,opt.method,Expr.emptyArray).tryResolve(reqType);
				if( e != null ) return e;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (et.isWrapper()) {
			Expr e = new UnaryExpr(pos,op,et.makeWrappedAccess(expr)).tryResolve(reqType);
			if (e != null) return e;
		}
		return null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			expr = (Expr)expr.resolve(null);
			if( op==PrefixOperator.PreIncr
			||  op==PrefixOperator.PreDecr
			||  op==PostfixOperator.PostIncr
			||  op==PostfixOperator.PostDecr
			) {
				return new IncrementExpr(pos,op,expr).resolve(null);
			} else if( op==PrefixOperator.BooleanNot ) {
				return new BooleanNotExpr(pos,expr).resolve(reqType);
			}
			// Check if expression is constant
			if( expr.isConstantExpr() ) {
				Number val = (Number)expr.getConstValue();
				if( op == PrefixOperator.Pos ) {
					if( val instanceof Double )
						return new ConstDoubleExpr(val.doubleValue()).resolve(null);
					else if( val instanceof Float )
						return new ConstFloatExpr(val.floatValue()).resolve(null);
					else if( val instanceof Long )
						return new ConstLongExpr(val.longValue()).resolve(null);
					else if( val instanceof Integer )
						return new ConstIntExpr(val.intValue()).resolve(null);
					else if( val instanceof Short )
						return new ConstShortExpr(val.shortValue()).resolve(null);
					else if( val instanceof Byte )
						return new ConstByteExpr(val.byteValue()).resolve(null);
				}
				else if( op == PrefixOperator.Neg ) {
					if( val instanceof Double )
						return new ConstDoubleExpr(-val.doubleValue()).resolve(null);
					else if( val instanceof Float )
						return new ConstFloatExpr(-val.floatValue()).resolve(null);
					else if( val instanceof Long )
						return new ConstLongExpr(-val.longValue()).resolve(null);
					else if( val instanceof Integer )
						return new ConstIntExpr(-val.intValue()).resolve(null);
					else if( val instanceof Short )
						return new ConstShortExpr(-val.shortValue()).resolve(null);
					else if( val instanceof Byte )
						return new ConstByteExpr(-val.byteValue()).resolve(null);
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating UnaryExpr: "+this);
		PassInfo.push(this);
		try {
			expr.generate(null);
			if( op == PrefixOperator.BitNot ) {
				if( expr.getType() == Type.tpLong )
					Code.addConst(-1L);
				else
					Code.addConst(-1);
				Code.addInstr(op_xor);
			} else {
				Code.addInstr(op.instr);
			}
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr ) {
			if( expr.getPriority() < op.priority ) {
				dmp.append('(').append(expr).append(')');
			} else {
				dmp.append(expr);
			}
			dmp.append(op.image);
		} else {
			dmp.append(op.image);
			if( expr.getPriority() < op.priority ) {
				dmp.append('(').append(expr).append(')');
			} else {
				dmp.append(expr);
			}
		}
		return dmp;
	}
}

@node
@cfnode
public class IncrementExpr extends LvalueExpr {
	@ref public Operator			op;
	@att public Expr				lval;

	public IncrementExpr() {
	}

	public IncrementExpr(int pos, Operator op, Expr lval) {
		super(pos);
		this.op = op;
		this.lval = lval;
	}

	public String toString() {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr )
			return lval.toString()+op.image;
		else
			return op.image+lval.toString();
	}

	public Type getType() {
		return lval.getType();
	}

	public int getPriority() { return op.priority; }

	public void cleanup() {
		parent=null;
		lval.cleanup();
		lval = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( (lval instanceof VarAccessExpr) && ((VarAccessExpr)lval).var.isNeedProxy() ) {
			// Check that we in local/anonymouse class, thus var need RefProxy
			Var var = ((VarAccessExpr)lval).var;
			ASTNode p = var.parent;
			while( !(p instanceof Struct) ) p = p.parent;
			if( !((Struct)p).equals(PassInfo.clazz) && !var.isNeedRefProxy() ) {
				throw new RuntimeException("Unsupported operation");
				//var.setNeedRefProxy(true);
				//Field vf = (Field)PassInfo.clazz.resolveName(var.name.name);
				//vf.type = Type.getProxyType(var.type);
			}
		}
		setResolved(true);
		return this;
	}

	private void pushProperConstant(int i) {
		Type lt = lval.getType();
		if( i > 0 ) { // 1
			if( lt == Type.tpDouble ) Code.addConst(1.D);
			else if( lt == Type.tpFloat ) Code.addConst(1.F);
			else if( lt == Type.tpLong ) Code.addConst(1L);
			else Code.addConst(1);
		} else { // -1
			if( lt == Type.tpDouble ) Code.addConst(-1.D);
			else if( lt == Type.tpFloat ) Code.addConst(-1.F);
			else if( lt == Type.tpLong ) Code.addConst(-1L);
			else Code.addConst(-1);
		}
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating IncrementExpr: "+this);
		PassInfo.push(this);
		try {
			LvalueExpr lval = (LvalueExpr)this.lval;
			if( reqType != Type.tpVoid ) {
				generateLoad();
			} else {
				if( lval instanceof VarAccessExpr ) {
					VarAccessExpr va = (VarAccessExpr)lval;
					if( va.var.getType().isIntegerInCode() && !va.var.isNeedProxy() || va.isUseNoProxy() ) {
						if( op==PrefixOperator.PreIncr || op==PostfixOperator.PostIncr ) {
							Code.addInstrIncr(va.var,1);
							return;
						}
						else if( op==PrefixOperator.PreDecr || op==PostfixOperator.PostDecr ) {
							Code.addInstrIncr(va.var,-1);
							return;
						}
					}
				}
				lval.generateLoadDup();

				if( op == PrefixOperator.PreIncr ) {
					pushProperConstant(1);
					Code.addInstr(op_add);
					lval.generateStore();
				}
				else if( op == PrefixOperator.PreDecr ) {
					pushProperConstant(-1);
					Code.addInstr(op_add);
					lval.generateStore();
				}
				else if( op == PostfixOperator.PostIncr ) {
					pushProperConstant(1);
					Code.addInstr(op_add);
					lval.generateStore();
				}
				else if( op == PostfixOperator.PostDecr ) {
					pushProperConstant(-1);
					Code.addInstr(op_add);
					lval.generateStore();
				}
			}
		} finally { PassInfo.pop(this); }
	}

	/** Just load value referenced by lvalue */
	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating IncrementExpr: - load "+this);
		PassInfo.push(this);
		try {
			LvalueExpr lval = (LvalueExpr)this.lval;
			if( lval instanceof VarAccessExpr ) {
				VarAccessExpr va = (VarAccessExpr)lval;
				if( va.var.getType().isIntegerInCode() && !va.var.isNeedProxy() || va.isUseNoProxy() ) {
					if( op == PrefixOperator.PreIncr ) {
						Code.addInstrIncr(va.var,1);
						Code.addInstr(op_load,va.var);
						return;
					}
					else if( op == PostfixOperator.PostIncr ) {
						Code.addInstr(op_load,va.var);
						Code.addInstrIncr(va.var,1);
						return;
					}
					else if( op == PrefixOperator.PreDecr ) {
						Code.addInstrIncr(va.var,-1);
						Code.addInstr(op_load,va.var);
						return;
					}
					else if( op == PostfixOperator.PostDecr ) {
						Code.addInstr(op_load,va.var);
						Code.addInstrIncr(va.var,-1);
						return;
					}
				}
			}
			lval.generateLoadDup();
			if( op == PrefixOperator.PreIncr ) {
				pushProperConstant(1);
				Code.addInstr(op_add);
				lval.generateStoreDupValue();
			}
			else if( op == PrefixOperator.PreDecr ) {
				pushProperConstant(-1);
				Code.addInstr(op_add);
				lval.generateStoreDupValue();
			}
			else if( op == PostfixOperator.PostIncr ) {
				pushProperConstant(1);
				Code.addInstr(op_add);
				lval.generateStoreDupValue();
				pushProperConstant(-1);
				Code.addInstr(op_add);
			}
			else if( op == PostfixOperator.PostDecr ) {
				pushProperConstant(-1);
				Code.addInstr(op_add);
				lval.generateStoreDupValue();
				pushProperConstant(1);
				Code.addInstr(op_add);
			}
		} finally { PassInfo.pop(this); }
	}

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while)
	*/
	public void generateLoadDup() {
		PassInfo.push(this);
		try {
			throw new RuntimeException("Too complex lvalue expression "+this);
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		PassInfo.push(this);
		try {
			throw new RuntimeException("Too complex lvalue expression "+this);
		} finally { PassInfo.pop(this); }
	}

	/** Stores value using previously duped info */
	public void generateStore() {
		PassInfo.push(this);
		try {
			throw new RuntimeException("Too complex lvalue expression "+this);
		} finally { PassInfo.pop(this); }
	}

	/** Stores value using previously duped info, and put stored value in stack */
	public void generateStoreDupValue() {
		PassInfo.push(this);
		try {
			throw new RuntimeException("Too complex lvalue expression "+this);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr ) {
			if( lval.getPriority() < op.priority ) {
				dmp.append('(').append(lval).append(')');
			} else {
				dmp.append(lval);
			}
			dmp.append(op.image);
		} else {
			dmp.append(op.image);
			if( lval.getPriority() < op.priority ) {
				dmp.append('(').append(lval).append(')');
			} else {
				dmp.append(lval);
			}
		}
		return dmp;
	}
}

@node
@cfnode
public class MultiExpr extends Expr {
	@ref public MultiOperator			op;
	@att public final NArr<ASTNode>		exprs;

	public MultiExpr() {
	}

	public MultiExpr(int pos, MultiOperator op, List<ASTNode> exprs) {
		super(pos);
		this.op = op;
		this.exprs.addAll(exprs.toArray());
	}

	public void cleanup() {
		parent=null;
		exprs.cleanup();
	}

	public Expr tryResolve(Type reqType) {
		if( op == MultiOperator.Conditional ) {
			Expr cond = ((Expr)exprs[0]).tryResolve(Type.tpBoolean);
			if( cond == null )
				return null;
			Expr expr1 = ((Expr)exprs[1]).tryResolve(reqType);
			if( expr1 == null )
				return null;
			Expr expr2 = ((Expr)exprs[2]).tryResolve(reqType);
			if( expr2 == null )
				return null;
			return (Expr)new ConditionalExpr(pos,cond,expr1,expr2).resolve(reqType);
		}
		throw new CompilerException(pos,"Multi-operators are not implemented");
	}
}


@node
@cfnode
public class ConditionalExpr extends Expr {
	@att public Expr		cond;
	@att public Expr		expr1;
	@att public Expr		expr2;

	public ConditionalExpr() {
	}

	public ConditionalExpr(int pos, Expr cond, Expr expr1, Expr expr2) {
		super(pos);
		this.cond = cond;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(').append(cond).append(") ? ");
		sb.append('(').append(expr1).append(") : ");
		sb.append('(').append(expr2).append(") ");
		return sb.toString();
	}

	public Type getType() {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( t1.isReference() && t2.isReference() ) {
			if( t1 == t2 ) return t1;
			if( t1 == Type.tpNull ) return t2;
			if( t2 == Type.tpNull ) return t1;
			return Type.leastCommonType(t1,t2);
		}
		if( t1.isNumber() && t2.isNumber() ) {
			if( t1 == t2 ) return t1;
			return Type.upperCastNumbers(t1,t2);
		}
		return expr1.getType();
	}

	public int getPriority() { return opConditionalPriority; }

	public void cleanup() {
		parent=null;
		cond.cleanup();
		cond = null;
		expr1.cleanup();
		expr1 = null;
		expr2.cleanup();
		expr2 = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		NodeInfoPass.pushState();
		ScopeNodeInfoVector result_state = null;
		try {
			cond = cond.resolveExpr(Type.tpBoolean);
			NodeInfoPass.pushState();
			if( cond instanceof InstanceofExpr ) ((InstanceofExpr)cond).setNodeTypeInfo();
			else if( cond instanceof BinaryBooleanAndExpr ) {
				BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)cond;
				if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
				if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
			}
			expr1 = reqType==null?(Expr)expr1.resolve(null):expr1.resolveExpr(reqType);
			ScopeNodeInfoVector then_state = NodeInfoPass.popState();
			NodeInfoPass.pushState();
			if( cond instanceof BooleanNotExpr ) {
				BooleanNotExpr bne = (BooleanNotExpr)cond;
				if( bne.expr instanceof InstanceofExpr ) ((InstanceofExpr)bne.expr).setNodeTypeInfo();
				else if( bne.expr instanceof BinaryBooleanAndExpr ) {
					BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)bne.expr;
					if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
					if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
				}
			}
			expr2 = reqType==null?(Expr)expr2.resolve(null):expr2.resolveExpr(reqType);
			ScopeNodeInfoVector else_state = NodeInfoPass.popState();

			result_state = NodeInfoPass.joinInfo(then_state,else_state);

			if( expr1.getType() != getType() ) expr1 = (Expr)new CastExpr(expr1.pos,getType(),expr1).resolve(getType());
			if( expr2.getType() != getType() ) expr2 = (Expr)new CastExpr(expr2.pos,getType(),expr2).resolve(getType());
		} finally {
			PassInfo.pop(this);
			NodeInfoPass.popState();
			if( result_state != null ) NodeInfoPass.addInfo(result_state);
		}
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		PassInfo.push(this);
		try {
			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					expr1.generate(null);
				} else {
					expr2.generate(null);
				}
			} else {
				CodeLabel elseLabel = Code.newLabel();
				CodeLabel endLabel = Code.newLabel();
				BoolExpr.gen_iffalse(cond, elseLabel);
				expr1.generate(null);
				Code.addInstr(Instr.op_goto,endLabel);
				Code.addInstr(Instr.set_label,elseLabel);
				expr2.generate(null);
				if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
				Code.addInstr(Instr.set_label,endLabel);
			}
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("((");
		dmp.append(cond).append(") ? (");
		dmp.append(expr1).append(") : (");
		dmp.append(expr2).append("))");
		return dmp;
	}
}

@node
@cfnode
public class CastExpr extends Expr {
	@ref public Type			type;
	@att public Expr			expr;
	public boolean				explicit = false;
	public boolean				reinterp = false;

	public CastExpr() {
	}

	public CastExpr(int pos, Type type, Expr expr) {
		super(pos);
		this.type = type;
		this.expr = expr;
	}

	public CastExpr(int pos, Type type, Expr expr, boolean expl) {
		super(pos);
		this.type = type;
		this.expr = expr;
		explicit = expl;
	}

	public CastExpr(int pos, Type type, Expr expr, boolean expl, boolean reint) {
		super(pos);
		this.type = type;
		this.expr = expr;
		explicit = expl;
		reinterp = reint;
	}

	public String toString() {
		return "(("+type+")"+expr+")";
	}

	public Type getType() {
		return type;
	}

	public void cleanup() {
		parent=null;
		type = null;
		expr.cleanup();
		expr = null;
	}

	public Type[] getAccessTypes() {
		Type[] types = expr.getAccessTypes();
		return NodeInfoPass.addAccessType(types,type);
	}

	public int getPriority() { return opCastPriority; }

	public Expr tryResolve(Type reqType) {
		Expr ex = (Expr)expr.tryResolve(type);
		if( ex == null ) return null;
		expr = ex;
		Type extp = Type.getRealType(type,expr.getType());
		if( type == Type.tpBoolean && extp == Type.tpRule )	return ex;
		// Try to find $cast method
		if( !extp.isAutoCastableTo(type) ) {
			Expr ocast = tryOverloadedCast(extp);
			if( ocast == this ) return (Expr)resolve(reqType);
			if (extp.isWrapper()) {
				return new CastExpr(pos,type,extp.makeWrappedAccess(expr),explicit,reinterp).tryResolve(reqType);
			}
		}
		else if (extp.isWrapper() && extp.getWrappedType().isAutoCastableTo(type)) {
			Expr ocast = tryOverloadedCast(extp);
			if( ocast == this ) return (Expr)resolve(reqType);
			return new CastExpr(pos,type,extp.makeWrappedAccess(expr),explicit,reinterp).tryResolve(reqType);
		}
		else {
//			if( extp.isReference() && type.isReference() ) {
//				trace(true,"unneded cast: "+extp+" is autocastable to "+type);
//				return ex;
//			}
			return (Expr)this.resolve(type);
		}
		if( extp.isCastableTo(type) ) {
			return (Expr)this.resolve(type);
		}
		if( type == Type.tpInt && extp == Type.tpBoolean && reinterp )	
			return (Expr)this.resolve(type);
		throw new CompilerException(pos,"Expression "+ex+" of type "+extp+" is not castable to "+type);
	}

	public Expr tryOverloadedCast(Type et) {
		ASTNode@ v;
		ResInfo info = new ResInfo(ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
		v.$unbind();
		MethodType mt = MethodType.newMethodType(null,Type.emptyArray,this.type);
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			Expr ce = info.buildCall(pos,expr,(Method)v,Expr.emptyArray);
			expr = ce;
			return this;
		}
		v.$unbind();
		info = new ResInfo(ResInfo.noForwards|ResInfo.noImports);
		mt = MethodType.newMethodType(null,new Type[]{expr.getType()},this.type);
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			assert(v.isStatic());
			Expr ce = (Expr)new CallAccessExpr(pos,parent,expr,(Method)v,new Expr[]{expr}).resolve(type);
			expr = ce;
			return this;
		}
		return null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) {
			setNodeCastType();
			return this;
		}
		PassInfo.push(this);
		try {
			ASTNode e = expr.resolve(type);
			if( e instanceof BaseStruct )
				expr = Expr.toExpr((BaseStruct)e,reqType,pos,parent);
			else
				expr = (Expr)e;
			if (reqType == Type.tpVoid) {
				setResolved(true);
				return this;
			}
			Type et = Type.getRealType(type,expr.getType());
			// Try wrapped field
			if (et.isWrapper() && et.getWrappedType().equals(type)) {
				return et.makeWrappedAccess(expr).resolve(reqType);
			}
			// try null to something...
			if (et == Type.tpNull && reqType.isReference())
				return expr;
//			if( et.clazz.equals(Type.tpPrologVar.clazz) && type.equals(et.args[0]) ) {
//				Field varf = (Field)et.clazz.resolveName(KString.from("$var"));
//				return new AccessExpr(pos,parent,expr,varf).resolve(reqType);
//			}
//			if( type.clazz.equals(Type.tpPrologVar.clazz) && et.equals(type.args[0]) )
//				return new NewExpr(pos,
//						Type.newRefType(Type.tpPrologVar.clazz,new Type[]{et}),
//						new Expr[]{expr})
//					.resolve(reqType);
			if( type == Type.tpBoolean && et == Type.tpRule )
				return new BinaryBoolExpr(pos,
					BinaryOperator.NotEquals,
					expr,
					new ConstNullExpr()).resolve(type);
			if( type.isBoolean() && et.isBoolean() ) return expr;
			if( !Kiev.javaMode && type.isInstanceOf(Type.tpEnum) && et.isIntegerInCode() ) {
				if (type.isIntegerInCode())
					return this;
				Method cm = null;
				cm = type.resolveMethod(nameCastOp,KString.from("(I)"+type.signature));
				return new CallExpr(pos,parent,cm,new Expr[]{expr}).resolve(reqType);
			}
			if( !Kiev.javaMode && type.isIntegerInCode() && et.isInstanceOf(Type.tpEnum) ) {
				if (et.isIntegerInCode())
					return this;
				Method cf = (Method)Type.tpEnum.resolveMethod(nameEnumOrdinal, KString.from("()I"));
				return new CallAccessExpr(pos,parent,expr,cf,Expr.emptyArray).resolve(reqType);
			}
			// Try to find $cast method
			if( !et.isAutoCastableTo(type) ) {
				Expr ocast = tryOverloadedCast(et);
				if( ocast != null && ocast != this ) return ocast;
			}

			if( et.isReference() != type.isReference() && !(expr instanceof ClosureCallExpr) )
				if( !et.isReference() && type.isArgument() )
					Kiev.reportWarning(pos,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
				else if (!et.isEnum())
					throw new CompilerException(pos,"Expression "+expr+" of type "+et+" cannot be casted to type "+type);
			if( !et.isCastableTo((Type)type) && !(reinterp && et.isIntegerInCode() && type.isIntegerInCode() )) {
				throw new RuntimeException("Expression "+expr+" cannot be casted to type "+type);
			}
			if( Kiev.verify && expr.getType() != et ) {
				setNodeCastType();
				return this;
			}
			if( et.isReference() && et.isInstanceOf((Type)type) ) return expr;
			if( et.isReference() && type.isReference() && et.isStruct()
			 && et.getStruct().package_clazz.isClazz()
			 && !et.isArgument()
			 && !et.isStaticClazz() && et.getStruct().package_clazz.type.isAutoCastableTo(type)
			) {
				return new CastExpr(pos,type,
					new AccessExpr(pos,expr,OuterThisAccessExpr.outerOf((Struct)et.getStruct())),explicit
				).resolve(reqType);
			}
			if( expr.isConstantExpr() ) {
				Object val = expr.getConstValue();
				if( val instanceof Number ) {
					Number num = (Number)val;
					if     ( type == Type.tpDouble ) return new ConstDoubleExpr ((double)num.doubleValue()).resolve(null);
					else if( type == Type.tpFloat )  return new ConstFloatExpr  ((float) num.floatValue()).resolve(null);
					else if( type == Type.tpLong )   return new ConstLongExpr   ((long)  num.longValue()).resolve(null);
					else if( type == Type.tpInt )    return new ConstIntExpr    ((int)   num.intValue()).resolve(null);
					else if( type == Type.tpShort )  return new ConstShortExpr  ((short) num.intValue()).resolve(null);
					else if( type == Type.tpByte )   return new ConstByteExpr   ((byte)  num.intValue()).resolve(null);
					else if( type == Type.tpChar )   return new ConstCharExpr   ((char)  num.intValue()).resolve(null);
				}
				else if( val instanceof Character ) {
					char num = ((Character)val).charValue();
					if     ( type == Type.tpDouble ) return new ConstDoubleExpr ((double)(int)num).resolve(null);
					else if( type == Type.tpFloat )  return new ConstFloatExpr  ((float) (int)num).resolve(null);
					else if( type == Type.tpLong )   return new ConstLongExpr   ((long)  (int)num).resolve(null);
					else if( type == Type.tpInt )    return new ConstIntExpr    ((int)   (int)num).resolve(null);
					else if( type == Type.tpShort )  return new ConstShortExpr  ((short) (int)num).resolve(null);
					else if( type == Type.tpByte )   return new ConstByteExpr   ((byte)  (int)num).resolve(null);
					else if( type == Type.tpChar )   return new ConstCharExpr   ((char)  num).resolve(null);
				}
				else if( val instanceof Boolean ) {
					int num = ((Boolean)val).booleanValue() ? 1 : 0;
					if     ( type == Type.tpDouble ) return new ConstDoubleExpr ((double)num).resolve(null);
					else if( type == Type.tpFloat )  return new ConstFloatExpr  ((float) num).resolve(null);
					else if( type == Type.tpLong )   return new ConstLongExpr   ((long)  num).resolve(null);
					else if( type == Type.tpInt )    return new ConstIntExpr    ((int)   num).resolve(null);
					else if( type == Type.tpShort )  return new ConstShortExpr  ((short) num).resolve(null);
					else if( type == Type.tpByte )   return new ConstByteExpr   ((byte)  num).resolve(null);
					else if( type == Type.tpChar )   return new ConstCharExpr   ((char)  num).resolve(null);
				}
			}
			if( et.equals(type) ) return expr;
			if( expr instanceof ClosureCallExpr && et instanceof ClosureType ) {
				if( et.isAutoCastableTo(type) ) {
					((ClosureCallExpr)expr).is_a_call = true;
					return expr;
				}
				else if( et.isCastableTo(type) ) {
					((ClosureCallExpr)expr).is_a_call = true;
				}
			}
			setNodeCastType();
		} finally {
			PassInfo.pop(this);
		}
		setResolved(true);
		return this;
	}

	public void setNodeCastType() {
		ASTNode n;
		if (type == Type.tpVoid) return;
		switch(expr) {
		case VarAccessExpr:			n = ((VarAccessExpr)expr).var;	break;
		case StaticFieldAccessExpr:	n = ((StaticFieldAccessExpr)expr).var;	break;
		case AccessExpr:
			if !(((AccessExpr)expr).obj instanceof ThisExpr)
				return;
			n = ((AccessExpr)expr).var;
			break;
		default: return;
		}
		NodeInfoPass.setNodeTypes(n,NodeInfoPass.addAccessType(expr.getAccessTypes(),type));
	}

	public static Expr autoCast(Expr ex, Type tp) {
		Type at = ex.getType();
		if( !at.equals(tp) ) {
			if( at.isReference() && !tp.isReference() && Type.getRefTypeForPrimitive(tp).equals(at) )
				return autoCastToPrimitive(ex);
			else if( !at.isReference() && tp.isReference() && Type.getRefTypeForPrimitive(at).equals(tp) )
				return autoCastToReference(ex);
			else if( at.isReference() && tp.isReference() && at.isInstanceOf(tp) )
				return ex;
			else
				return new CastExpr(ex.pos,tp,ex).resolveExpr(tp);
		}
		return ex;
	}

	public static Expr autoCastToReference(Expr ex) {
		Type tp = ex.getType();
		if( tp.isReference() ) return ex;
		Expr ex1;
		if( tp == Type.tpBoolean )
			ex1 = new NewExpr(ex.pos,Type.tpBooleanRef,new Expr[]{ex});
		else if( tp == Type.tpByte )
			ex1 = new NewExpr(ex.pos,Type.tpByteRef,new Expr[]{ex});
		else if( tp == Type.tpShort )
			ex1 = new NewExpr(ex.pos,Type.tpShortRef,new Expr[]{ex});
		else if( tp == Type.tpInt )
			ex1 = new NewExpr(ex.pos,Type.tpIntRef,new Expr[]{ex});
		else if( tp == Type.tpLong )
			ex1 = new NewExpr(ex.pos,Type.tpLongRef,new Expr[]{ex});
		else if( tp == Type.tpFloat )
			ex1 = new NewExpr(ex.pos,Type.tpFloatRef,new Expr[]{ex});
		else if( tp == Type.tpDouble )
			ex1 = new NewExpr(ex.pos,Type.tpDoubleRef,new Expr[]{ex});
		else if( tp == Type.tpChar )
			ex1 = new NewExpr(ex.pos,Type.tpCharRef,new Expr[]{ex});
		else
			throw new RuntimeException("Unknown primitive type "+tp);
		ex1.parent = ex.parent;
		return (Expr)ex1.resolve(null);
	}

	public static Expr autoCastToPrimitive(Expr ex) {
		Type tp = ex.getType();
		if( !tp.isReference() ) return ex;
		Expr ex1;
		if( tp == Type.tpBooleanRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpBooleanRef.clazz.resolveMethod(
					KString.from("booleanValue"),
					KString.from("()Z")
				),Expr.emptyArray
			);
		else if( tp == Type.tpByteRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpByteRef.clazz.resolveMethod(
					KString.from("byteValue"),
					KString.from("()B")
				),Expr.emptyArray
			);
		else if( tp == Type.tpShortRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpShortRef.clazz.resolveMethod(
					KString.from("shortValue"),
					KString.from("()S")
				),Expr.emptyArray
			);
		else if( tp == Type.tpIntRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpIntRef.clazz.resolveMethod(
					KString.from("intValue"),
					KString.from("()I")
				),Expr.emptyArray
			);
		else if( tp == Type.tpLongRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpLongRef.clazz.resolveMethod(
					KString.from("longValue"),
					KString.from("()J")
				),Expr.emptyArray
			);
		else if( tp == Type.tpFloatRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpFloatRef.clazz.resolveMethod(
					KString.from("floatValue"),
					KString.from("()F")
				),Expr.emptyArray
			);
		else if( tp == Type.tpDoubleRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpDoubleRef.clazz.resolveMethod(
					KString.from("doubleValue"),
					KString.from("()D")
				),Expr.emptyArray
			);
		else if( tp == Type.tpCharRef )
			ex1 = new CallAccessExpr(ex.pos,ex.parent,ex,
				Type.tpCharRef.clazz.resolveMethod(
					KString.from("charValue"),
					KString.from("()C")
				),Expr.emptyArray
			);
		else
			throw new RuntimeException("Type "+tp+" is not a reflection of primitive type");
		return ex1;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CastExpr: "+this);
		PassInfo.push(this);
		try {
			expr.generate(null);
			Type t = expr.getType();
			if( t.isReference() ) {
				if( t.isReference() != type.isReference() )
					throw new CompilerException(pos,"Expression "+expr+" of type "+t+" cannot be casted to type "+type);
				if( Type.getRealType(Kiev.argtype,type).isReference() )
					Code.addInstr(Instr.op_checkcast,Type.getRealType(Kiev.argtype,type));
			} else {
			    if (reinterp) {
			        if (t.isIntegerInCode() && type.isIntegerInCode())
			            ; //generate nothing, both values are int-s
			        else
						throw new CompilerException(pos,"Expression "+expr+" of type "+t+" cannot be reinterpreted to type "+type);
			    } else {
				    Code.addInstr(Instr.op_x2y,type);
				}
			}
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("((").append(Type.getRealType(Kiev.argtype,type)).append(")(");
		dmp.append(expr).append("))");
		return dmp;
	}
}

