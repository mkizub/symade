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
 * @author Maxim Kizub
 * @version $Revision$
 *
 */


@node
public class ShadowExpr extends Expr {
	@ref public ENode expr;
	
	public ShadowExpr() {
	}
	public ShadowExpr(ENode expr) {
		this.expr = expr;
	}
	public Type getType() { return expr.getType(); }
	
	public int getPriority() {
		return expr.getPriority();
	}
	
	public void resolve(Type reqType) {
		expr.resolve(reqType);
		setResolved(true);
	}

	public void generate(Type reqType) {
		expr.generate(reqType);
	}
	
	public String toString() {
		return "(shadow of) "+expr;
	}

	public Dumper toJava(Dumper dmp) {
		return expr.toJava(dmp);
	}

}

@node
public class ArrayLengthAccessExpr extends Expr {
	@att public ENode		array;

	public ArrayLengthAccessExpr() {
	}

	public ArrayLengthAccessExpr(int pos, ENode array) {
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

	public Operator getOp() { return BinaryOperator.Access; }

	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			array.resolve(null);
			if !(array.getType().isArray())
				throw new CompilerException(pos, "Access to array length for non-array type "+array.getType());
		} finally { PassInfo.pop(this); }
		setResolved(true);
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
public class TypeClassExpr extends Expr {
	@att public TypeRef		type;

	public TypeClassExpr() {
	}

	public TypeClassExpr(int pos, TypeRef type) {
		super(pos);
		this.type = type;
	}

	public String toString() {
		return type.toString()+".class";
	}

	public Type getType() {
		return Type.tpClass;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			type.getType();
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void generate(Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating TypeClassExpr: "+this);
		PassInfo.push(this);
		try {
			Code.addConst(type.getJavaType());
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		type.toJava(dmp).append(".class").space();
		return dmp;
	}
}

@node
@dflow(out="this:?out")
public class AssignExpr extends LvalueExpr {
	
	@ref public AssignOperator	op;
	
	@dflow(in="")
	@att public ENode			lval;
	@dflow(in="lval")
	@att public ENode			value;

	public AssignExpr() {
	}

	public AssignExpr(int pos, AssignOperator op, ENode lval, ENode value) {
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
		if( value instanceof Expr && ((Expr)value).getPriority() < opAssignPriority )
			sb.append('(').append(value).append(')');
		else
			sb.append(value);
		return sb.toString();
	}

	public Type getType() { return lval.getType(); }

	public Operator getOp() { return op; }

	public void resolve(Type reqType) {
		if( isResolved() )
			return;
		setTryResolved(true);
		PassInfo.push(this);
		try {
			lval.resolve(reqType);
			Type et1 = lval.getType();
			if (op == AssignOperator.Assign && et1.isWrapper())
				value.resolve(et1.getWrappedType());
			else if (op == AssignOperator.Assign2 && et1.isWrapper())
				value.resolve(((WrapperType)et1).getUnwrappedType());
			else
				value.resolve(et1);
			if (value instanceof TypeRef)
				((TypeRef)value).toExpr(et1);
			Type et2 = value.getType();
			if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !et1.isWrapper() && !et2.isWrapper()) {
				this.postResolve(reqType);
				return;
			}
			else if( op == AssignOperator.Assign2 && et1.isWrapper() && et2.isInstanceOf(et1)) {
				this.postResolve(reqType);
				return;
			}
			else if( op == AssignOperator.AssignAdd && et1 == Type.tpString ) {
				this.postResolve(reqType);
				return;
			}
			else if( ( et1.isNumber() && et2.isNumber() ) &&
				(    op==AssignOperator.AssignAdd
				||   op==AssignOperator.AssignSub
				||   op==AssignOperator.AssignMul
				||   op==AssignOperator.AssignDiv
				||   op==AssignOperator.AssignMod
				)
			) {
				this.postResolve(reqType);
				return;
			}
			else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
				(    op==AssignOperator.AssignLeftShift
				||   op==AssignOperator.AssignRightShift
				||   op==AssignOperator.AssignUnsignedRightShift
				)
			) {
				this.postResolve(reqType);
				return;
			}
			else if( ( et1.isInteger() && et2.isInteger() ) &&
				(    op==AssignOperator.AssignBitOr
				||   op==AssignOperator.AssignBitXor
				||   op==AssignOperator.AssignBitAnd
				)
			) {
				this.postResolve(reqType);
				return;
			}
			else if( ( et1.isBoolean() && et2.isBoolean() ) &&
				(    op==AssignOperator.AssignBitOr
				||   op==AssignOperator.AssignBitXor
				||   op==AssignOperator.AssignBitAnd
				)
			) {
				this.postResolve(reqType);
				return;
			}
			// Not a standard operator, find out overloaded
			foreach(OpTypes opt; op.types ) {
				Type[] tps = new Type[]{null,et1,et2};
				ASTNode[] argsarr = new ASTNode[]{null,lval,value};
				if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
					replaceWithNodeResolve(reqType, new CallExpr(pos,lval,opt.method,new Expr[]{(Expr)value}));
					return;
				}
			}
			// Not a standard and not overloaded, try wrapped classes
			if (op != AssignOperator.Assign2) {
				if (et1.isWrapper() && et2.isWrapper()) {
					lval = et1.makeWrappedAccess(lval);
					value = et2.makeWrappedAccess(value);
					resolve(reqType);
					return;
				}
				else if (et1.isWrapper()) {
					lval = et1.makeWrappedAccess(lval);
					resolve(reqType);
					return;
				}
				else if (et2.isWrapper()) {
					value = et2.makeWrappedAccess(value);
					resolve(reqType);
					return;
				}
			}
			this.postResolve(reqType); //throw new CompilerException(pos,"Unresolved expression "+this);

		} finally { PassInfo.pop(this); }
	}

	private ENode postResolve(Type reqType) {
		lval.resolve(null);
		if( !(lval instanceof LvalueExpr) )
			throw new RuntimeException("Can't assign to "+lval+": lvalue requared");
		if( (lval instanceof VarAccessExpr) && ((VarAccessExpr)lval).var.isNeedProxy() ) {
			// Check that we in local/anonymouse class, thus var need RefProxy
			Var var = ((VarAccessExpr)lval).var;
			ASTNode p = var.parent;
			while( !(p instanceof Struct) ) p = p.parent;
			if( !((Struct)p).equals(PassInfo.clazz) && !var.isNeedRefProxy() ) {
				throw new RuntimeException("Unsupported operation");
			}
		}
		Type t1 = lval.getType();
		if( op==AssignOperator.AssignAdd && t1==Type.tpString ) {
			op = AssignOperator.Assign;
			value = new BinaryExpr(pos,BinaryOperator.Add,new ShadowExpr(lval),value);
		}
		if (value instanceof Expr)
			value.resolve(t1);
		else if (value instanceof TypeRef)
			((TypeRef)value).toExpr(t1);
		else
			throw new CompilerException(value.pos, "Can't opeerate on "+value);
		Type t2 = value.getType();
		if( op==AssignOperator.AssignLeftShift || op==AssignOperator.AssignRightShift || op==AssignOperator.AssignUnsignedRightShift ) {
			if( !t2.isIntegerInCode() ) {
				value = new CastExpr(pos,Type.tpInt,value);
				value.resolve(Type.tpInt);
			}
		}
		else if( !t2.isInstanceOf(t1) ) {
			if( t2.isCastableTo(t1) ) {
				value = new CastExpr(pos,t1,value);
				value.resolve(t1);
			} else {
				throw new RuntimeException("Value of type "+t2+" can't be assigned to "+lval);
			}
		}
		getDFlow().out();

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
		setResolved(true);
		return this;
	}

	public DFState calcDFlowOut() {
		return addNodeTypeInfo(value.getDFlow().out());
	}
	
	private DFState addNodeTypeInfo(DFState dfs) {
		if !(value instanceof Expr)
			return dfs;
		DNode[] path = null;
		switch(lval) {
		case VarAccessExpr:
			path = new DNode[]{((VarAccessExpr)lval).var};
			break;
		case AccessExpr:
			path = ((AccessExpr)lval).getAccessPath();
			break;
		case StaticFieldAccessExpr:
			path = new DNode[]{((StaticFieldAccessExpr)lval).var};
			break;
		}
		if (path != null)
			return dfs.setNodeValue(path,value);
		return dfs;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating AssignExpr: "+this);
		PassInfo.push(this);
		try {
			Expr value = (Expr)this.value;
			LvalueExpr lval = (LvalueExpr)this.lval;
			if( reqType != Type.tpVoid ) {
				if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) ) {
					lval.generateLoadDup();
					value.generate(null);
					Code.addInstr(op.instr);
					lval.generateStoreDupValue();
				} else {
					lval.generateAccess();
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
			Expr value = (Expr)this.value;
			LvalueExpr lval = (LvalueExpr)this.lval;
			lval.generateLoadDup();
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
			Expr value = (Expr)this.value;
			lval.generateLoadDup();
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
			Expr value = (Expr)this.value;
			LvalueExpr lval = (LvalueExpr)this.lval;
			lval.generateLoadDup();
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
		if( value instanceof Expr && ((Expr)value).getPriority() < opAssignPriority ) {
			dmp.append('(');
			dmp.append(value).append(')');
		} else {
			dmp.append(value);
		}
		return dmp;
	}
}


@node
public class BinaryExpr extends Expr {
	@ref public BinaryOperator		op;
	@att public ENode				expr1;
	@att public ENode				expr2;

	public BinaryExpr() {
	}

	public BinaryExpr(int pos, BinaryOperator op, ENode expr1, ENode expr2) {
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

	public Operator getOp() { return op; }

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
		resolve(null);
		return getType();
//		if( e == null )
//			Kiev.reportError(pos,"Type of binary operation "+op.image+" between "+expr1+" and "+expr2+" unknown, types are "+t1+" and "+t2);
//		else
//			return e.getType();
//		return Type.tpVoid;
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			expr1.resolve(null);
			expr2.resolve(null);
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
					if (et2.isWrapper()) expr2 = et2.makeWrappedAccess(expr2);
					sce.appendArg(expr2);
					trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
					replaceWithNodeResolve(Type.tpString, sce);
				} else {
					StringConcatExpr sce = new StringConcatExpr(pos);
					if (et1.isWrapper()) expr1 = et1.makeWrappedAccess(expr1);
					sce.appendArg(expr1);
					if (et2.isWrapper()) expr2 = et2.makeWrappedAccess(expr2);
					sce.appendArg(expr2);
					trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
					replaceWithNodeResolve(Type.tpString, sce);
				}
				return;
			}
			else if( ( et1.isNumber() && et2.isNumber() ) &&
				(    op==BinaryOperator.Add
				||   op==BinaryOperator.Sub
				||   op==BinaryOperator.Mul
				||   op==BinaryOperator.Div
				||   op==BinaryOperator.Mod
				)
			) {
				this.postResolve(null);
				return;
			}
			else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
				(    op==BinaryOperator.LeftShift
				||   op==BinaryOperator.RightShift
				||   op==BinaryOperator.UnsignedRightShift
				)
			) {
				this.postResolve(null);
				return;
			}
			else if( ( (et1.isInteger() && et2.isInteger()) || (et1.isBoolean() && et2.isBoolean()) ) &&
				(    op==BinaryOperator.BitOr
				||   op==BinaryOperator.BitXor
				||   op==BinaryOperator.BitAnd
				)
			) {
				this.postResolve(null);
				return;
			}
			// Not a standard operator, find out overloaded
			foreach(OpTypes opt; op.types ) {
				Type[] tps = new Type[]{null,et1,et2};
				ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
				if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
					Expr e;
					if( opt.method.isStatic() )
						replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{expr1,expr2}));
					else
						replaceWithNodeResolve(reqType, new CallExpr(pos,expr1,opt.method,new ENode[]{expr2}));
					return;
				}
			}
			// Not a standard and not overloaded, try wrapped classes
			if (et1.isWrapper() && et2.isWrapper()) {
				expr1 = et1.makeWrappedAccess(expr1);
				expr2 = et1.makeWrappedAccess(expr2);
				resolve(reqType);
				return;
			}
			if (et1.isWrapper()) {
				expr1 = et1.makeWrappedAccess(expr1);
				resolve(reqType);
				return;
			}
			if (et2.isWrapper()) {
				expr2 = et1.makeWrappedAccess(expr2);
				resolve(reqType);
				return;
			}
			postResolve(reqType);

		} finally { PassInfo.pop(this); }
	}

	private void postResolve(Type reqType) {
		expr1.resolve(null);
		expr2.resolve(null);

		Type rt = getType();
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();

		// Special case for '+' operator if one arg is a String
		if( op==BinaryOperator.Add && expr1.getType().equals(Type.tpString) || expr2.getType().equals(Type.tpString) ) {
			if( expr1 instanceof StringConcatExpr ) {
				StringConcatExpr sce = (StringConcatExpr)expr1;
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
				replaceWithNodeResolve(Type.tpString, sce);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				sce.appendArg(expr1);
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
				replaceWithNodeResolve(Type.tpString, sce);
			}
			return;
		}

		if( op==BinaryOperator.LeftShift || op==BinaryOperator.RightShift || op==BinaryOperator.UnsignedRightShift ) {
			if( !t2.isIntegerInCode() ) {
				expr2 = new CastExpr(pos,Type.tpInt,expr2);
				expr2.resolve(Type.tpInt);
			}
		} else {
			if( !rt.equals(t1) && t1.isCastableTo(rt) ) {
				expr1 = new CastExpr(pos,rt,expr1);
				expr1.resolve(null);
			}
			if( !rt.equals(t2) && t2.isCastableTo(rt) ) {
				expr2 = new CastExpr(pos,rt,expr2);
				expr2.resolve(null);
			}
		}

		// Check if both expressions are constant
		if( expr1.isConstantExpr() && expr2.isConstantExpr() ) {
			Number val1 = (Number)expr1.getConstValue();
			Number val2 = (Number)expr2.getConstValue();
			if( op == BinaryOperator.BitOr ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() | val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() | val2.intValue()));
			}
			else if( op == BinaryOperator.BitXor ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() ^ val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() ^ val2.intValue()));
			}
			else if( op == BinaryOperator.BitAnd ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() & val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() & val2.intValue()));
			}
			else if( op == BinaryOperator.LeftShift ) {
				if( val1 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() << val2.intValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() << val2.intValue()));
			}
			else if( op == BinaryOperator.RightShift ) {
				if( val1 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() >> val2.intValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() >> val2.intValue()));
			}
			else if( op == BinaryOperator.UnsignedRightShift ) {
				if( val1 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() >>> val2.intValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() >>> val2.intValue()));
			}
			else if( op == BinaryOperator.Add ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() + val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() + val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() + val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() + val2.intValue()));
			}
			else if( op == BinaryOperator.Sub ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() - val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() - val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() - val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() - val2.intValue()));
			}
			else if( op == BinaryOperator.Mul ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() * val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() * val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() * val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() * val2.intValue()));
			}
			else if( op == BinaryOperator.Div ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() / val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() / val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() / val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() / val2.intValue()));
			}
			else if( op == BinaryOperator.Mod ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() % val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() % val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() % val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() % val2.intValue()));
			}
		}
		setResolved(true);
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
public class StringConcatExpr extends Expr {
	@att public final NArr<ENode>	args;

	public static Struct clazzStringBuffer;
	public static Method clazzStringBufferToString;
	public static Method clazzStringBufferInit;

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
			throw new RuntimeException("Can't initialize: "+e.getMessage());
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

	public Operator getOp() { return BinaryOperator.Add; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			foreach (ENode e; args)
				e.resolve(null);
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void appendArg(ENode expr) {
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
	public Method getMethodFor(ENode expr) {
		Type t = expr.getType();
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
@dflow
public class CommaExpr extends Expr {
	@dflow(in="", seq=true)
	@att public final NArr<ENode>	exprs;

	public CommaExpr() {
	}

	public CommaExpr(ENode expr) {
		super(expr.pos);
		this.exprs.add(expr);
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

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			for(int i=0; i < exprs.length; i++) {
				if( i < exprs.length-1) {
					exprs[i].resolve(Type.tpVoid);
					exprs[i].setGenVoidExpr(true);
				} else {
					exprs[i].resolve(reqType);
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
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
@dflow(out="this:?out")
public class BlockExpr extends Expr implements ScopeOfNames, ScopeOfMethods {

	@dflow(in="", seq=true)
	@att public final NArr<ENode>		stats;
	
	@dflow(in="stats")
	@att public       ENode				res;

	public BlockExpr() {
	}

	public BlockExpr(int pos, ASTNode parent) {
		super(pos, parent);
	}

	public void setExpr(ENode res) {
		this.res = res;
	}

	public ENode addStatement(ENode st) {
		stats.append(st);
		return st;
	}

	public void addSymbol(Named sym) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ENode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl.pos,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.append(decl);
	}

	public void insertSymbol(Named sym, int idx) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ASTNode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl.pos,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.insert(decl,idx);
	}
	
	public Type getType() {
		if (res == null) return Type.tpVoid;
		return res.getType();
	}

	public int		getPriority() { return 255; }

	public rule resolveNameR(ASTNode@ node, ResInfo info, KString name)
		ASTNode@ n;
	{
		n @= new SymbolIterator(this,this.stats),
		{
			n instanceof VarDecl,
			((VarDecl)n).var.name.equals(name),
			node ?= ((VarDecl)n).var
		;	n instanceof LocalStructDecl,
			name.equals(((LocalStructDecl)n).clazz.name.short_name),
			node ?= ((LocalStructDecl)n).clazz
		;	n instanceof Typedef,
			name.equals(((Typedef)n).name),
			node ?= ((Typedef)n).type
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this,this.stats),
		n instanceof VarDecl && ((VarDecl)n).var.isForward() && ((VarDecl)n).var.name.equals(name),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this,this.stats),
		n instanceof VarDecl && ((VarDecl)n).var.isForward(),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		((VarDecl)n).var.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			BlockStat.resolveBlockStats(this, stats);
			if (res != null) {
				res.resolve(reqType);
			}
		} finally {
			PassInfo.pop(this);
		}
	}

	public DFState calcDFlowOut() {
		Vector<Var> vars = new Vector<Var>();
		foreach (ASTNode n; stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
		if (res != null) {
			if (vars.length > 0)
				return res.getDFlow().out().cleanInfoForVars(vars.toArray());
			else
				return res.getDFlow().out();
		}
		else if (stats.length > 0) {
			if (vars.length > 0)
				return stats[stats.length-1].getDFlow().out().cleanInfoForVars(vars.toArray());
			else
				return stats[stats.length-1].getDFlow().out();
		}
		else {
			return getDFlow().in();
		}
	}
	
	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockExpr");
		PassInfo.push(this);
		try {
			for(int i=0; i < stats.length; i++) {
				try {
					stats[i].generate(Type.tpVoid);
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
			Vector<Var> vars = new Vector<Var>();
			foreach (ASTNode n; stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
			Code.removeVars(vars.toArray());
		} finally { PassInfo.pop(this); }
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
public class UnaryExpr extends Expr {
	@ref public Operator			op;
	@att public ENode				expr;

	public UnaryExpr() {
	}

	public UnaryExpr(int pos, Operator op, ENode expr) {
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

	public Operator getOp() { return op; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			setTryResolved(true);
			expr.resolve(reqType);
			Type et = expr.getType();
			if( et.isNumber() &&
				(  op==PrefixOperator.PreIncr
				|| op==PrefixOperator.PreDecr
				|| op==PostfixOperator.PostIncr
				|| op==PostfixOperator.PostDecr
				)
			) {
				replaceWithNodeResolve(reqType, new IncrementExpr(pos,op,expr));
				return;
			}
			if( et.isAutoCastableTo(Type.tpBoolean) &&
				(  op==PrefixOperator.PreIncr
				|| op==PrefixOperator.BooleanNot
				)
			) {
				replaceWithNodeResolve(Type.tpBoolean, new BooleanNotExpr(pos,expr));
				return;
			}
			if( et.isNumber() &&
				(  op==PrefixOperator.Pos
				|| op==PrefixOperator.Neg
				)
			) {
				this.postResolve(reqType);
				return;
			}
			if( et.isInteger() && op==PrefixOperator.BitNot ) {
				this.postResolve(reqType);
				return;
			}
			// Not a standard operator, find out overloaded
			foreach(OpTypes opt; op.types ) {
				if (PassInfo.clazz != null && opt.method != null && opt.method.type.args.length == 1) {
					if ( !PassInfo.clazz.type.isStructInstanceOf((Struct)opt.method.parent) )
						continue;
				}
				Type[] tps = new Type[]{null,et};
				ASTNode[] argsarr = new ASTNode[]{null,expr};
				if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
					Expr e;
					if ( opt.method.isStatic() )
						replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{expr}));
					else
						replaceWithNodeResolve(reqType, new CallExpr(pos,expr,opt.method,Expr.emptyArray));
					return;
				}
			}
			// Not a standard and not overloaded, try wrapped classes
			if (et.isWrapper()) {
				replaceWithNodeResolve(reqType, new UnaryExpr(pos,op,et.makeWrappedAccess(expr)));
				return;
			}
			postResolve(reqType);
		} finally { PassInfo.pop(this); }
	}

	private void postResolve(Type reqType) {
		expr.resolve(null);
		if( op==PrefixOperator.PreIncr
		||  op==PrefixOperator.PreDecr
		||  op==PostfixOperator.PostIncr
		||  op==PostfixOperator.PostDecr
		) {
			replaceWithNodeResolve(reqType, new IncrementExpr(pos,op,expr));
			return;
		} else if( op==PrefixOperator.BooleanNot ) {
			replaceWithNodeResolve(reqType, new BooleanNotExpr(pos,expr));
			return;
		}
		// Check if expression is constant
		if( expr.isConstantExpr() ) {
			Number val = (Number)expr.getConstValue();
			if( op == PrefixOperator.Pos ) {
				if( val instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val.doubleValue()));
				else if( val instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val.floatValue()));
				else if( val instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val.longValue()));
				else if( val instanceof Integer )
					replaceWithNodeResolve(new ConstIntExpr(val.intValue()));
				else if( val instanceof Short )
					replaceWithNodeResolve(new ConstShortExpr(val.shortValue()));
				else if( val instanceof Byte )
					replaceWithNodeResolve(new ConstByteExpr(val.byteValue()));
			}
			else if( op == PrefixOperator.Neg ) {
				if( val instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(-val.doubleValue()));
				else if( val instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(-val.floatValue()));
				else if( val instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(-val.longValue()));
				else if( val instanceof Integer )
					replaceWithNodeResolve(new ConstIntExpr(-val.intValue()));
				else if( val instanceof Short )
					replaceWithNodeResolve(new ConstShortExpr(-val.shortValue()));
				else if( val instanceof Byte )
					replaceWithNodeResolve(new ConstByteExpr(-val.byteValue()));
			}
		}
		setResolved(true);
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
public class IncrementExpr extends LvalueExpr {
	@ref public Operator			op;
	@att public ENode				lval;

	public IncrementExpr() {
	}

	public IncrementExpr(int pos, Operator op, ENode lval) {
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

	public Operator getOp() { return op; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if( (lval instanceof VarAccessExpr) && ((VarAccessExpr)lval).var.isNeedProxy() ) {
			// Check that we in local/anonymouse class, thus var need RefProxy
			Var var = ((VarAccessExpr)lval).var;
			ASTNode p = var.parent;
			while( !(p instanceof Struct) ) p = p.parent;
			if( !((Struct)p).equals(PassInfo.clazz) && !var.isNeedRefProxy() ) {
				throw new RuntimeException("Unsupported operation");
			}
		}
		setResolved(true);
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
public class ConditionalExpr extends Expr {
	@att public ENode		cond;
	@att public ENode		expr1;
	@att public ENode		expr2;

	public ConditionalExpr() {
	}

	public ConditionalExpr(int pos, ENode cond, ENode expr1, ENode expr2) {
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

	public Operator getOp() { return MultiOperator.Conditional; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			cond.resolve(Type.tpBoolean);
			expr1.resolve(reqType);
			expr2.resolve(reqType);

			if( expr1.getType() != getType() ) {
				expr1 = new CastExpr(expr1.pos,getType(),expr1);
				expr1.resolve(getType());
			}
			if( expr2.getType() != getType() ) {
				expr2 = new CastExpr(expr2.pos,getType(),expr2);
				expr2.resolve(getType());
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
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
public class CastExpr extends Expr {
	@att public TypeRef			type;
	@att public ENode			expr;
	public boolean				explicit = false;
	public boolean				reinterp = false;

	public CastExpr() {
	}

	public CastExpr(int pos, Type type, ENode expr) {
		super(pos);
		this.type = new TypeRef(type);
		this.expr = expr;
	}

	public CastExpr(int pos, TypeRef type, ENode expr) {
		super(pos);
		this.type = type;
		this.expr = expr;
	}

	public CastExpr(int pos, Type type, ENode expr, boolean reint) {
		this(pos, type, expr);
		reinterp = reint;
	}

	public CastExpr(int pos, TypeRef type, ENode expr, boolean reint) {
		this(pos, type, expr);
		reinterp = reint;
	}

	public String toString() {
		return "(("+type+")"+expr+")";
	}

	public Type getType() {
		return type.getType();
	}

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public int getPriority() { return opCastPriority; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		Type type = this.type.getType();
		PassInfo.push(this);
		try {
			expr.resolve(type);
			if (expr instanceof TypeRef)
				((TypeRef)expr).toExpr(type);
			Type extp = Type.getRealType(type,expr.getType());
			if( type == Type.tpBoolean && extp == Type.tpRule ) {
				replaceWithNode(expr);
				return;
			}
			// Try to find $cast method
			if( !extp.isAutoCastableTo(type) ) {
				ENode ocast = tryOverloadedCast(extp);
				if( ocast == this ) {
					resolve(reqType);
					return;
				}
				if (extp.isWrapper()) {
					expr = extp.makeWrappedAccess(expr);
					resolve(reqType);
					return;
				}
			}
			else if (extp.isWrapper() && extp.getWrappedType().isAutoCastableTo(type)) {
				ENode ocast = tryOverloadedCast(extp);
				if( ocast == this ) {
					resolve(reqType);
					return;
				}
				expr = extp.makeWrappedAccess(expr);
				resolve(reqType);
				return;
			}
			else {
				this.postResolve(type);
				return;
			}
			if( extp.isCastableTo(type) ) {
				this.postResolve(type);
				return;
			}
			if( type == Type.tpInt && extp == Type.tpBoolean && reinterp ) {	
				this.postResolve(type);
				return;
			}
			throw new CompilerException(pos,"Expression "+expr+" of type "+extp+" is not castable to "+type);
		} finally { PassInfo.pop(this); 	}
	}

	public ENode tryOverloadedCast(Type et) {
		ASTNode@ v;
		ResInfo info = new ResInfo(ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
		v.$unbind();
		MethodType mt = MethodType.newMethodType(null,Type.emptyArray,this.type.getType());
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			expr = info.buildCall(pos,expr,(Method)v,ENode.emptyArray);
			return this;
		}
		v.$unbind();
		info = new ResInfo(ResInfo.noForwards|ResInfo.noImports);
		mt = MethodType.newMethodType(null,new Type[]{expr.getType()},this.type.getType());
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			assert(v.isStatic());
			expr = new CallExpr(pos,expr,(Method)v,new ENode[]{expr});
			expr.resolve(type.getType());
			return this;
		}
		return null;
	}

	private void postResolve(Type reqType) {
		Type type = this.type.getType();
		expr.resolve(type);
//		if( e instanceof Struct )
//			expr = Expr.toExpr((Struct)e,reqType,pos,parent);
//		else
//			expr = (Expr)e;
		if (reqType == Type.tpVoid) {
			setResolved(true);
		}
		Type et = Type.getRealType(type,expr.getType());
		// Try wrapped field
		if (et.isWrapper() && et.getWrappedType().equals(type)) {
			expr = et.makeWrappedAccess(expr);
			resolve(reqType);
			return;
		}
		// try null to something...
		if (et == Type.tpNull && reqType.isReference())
			return;
		if( type == Type.tpBoolean && et == Type.tpRule ) {
			replaceWithNodeResolve(type, new BinaryBoolExpr(pos,BinaryOperator.NotEquals,expr,new ConstNullExpr()));
			return;
		}
		if( type.isBoolean() && et.isBoolean() )
			return;
		if( !Kiev.javaMode && type.isInstanceOf(Type.tpEnum) && et.isIntegerInCode() ) {
			if (type.isIntegerInCode())
				return;
			Method cm = null;
			cm = type.resolveMethod(nameCastOp,KString.from("(I)"+type.signature));
			replaceWithNodeResolve(reqType, new CallExpr(pos,null,cm,new ENode[]{expr}));
			return;
		}
		if( !Kiev.javaMode && type.isIntegerInCode() && et.isInstanceOf(Type.tpEnum) ) {
			if (et.isIntegerInCode())
				return;
			Method cf = (Method)Type.tpEnum.resolveMethod(nameEnumOrdinal, KString.from("()I"));
			replaceWithNodeResolve(reqType, new CallExpr(pos,expr,cf,Expr.emptyArray));
			return;
		}
		// Try to find $cast method
		if( !et.isAutoCastableTo(type) ) {
			ENode ocast = tryOverloadedCast(et);
			if( ocast != null && ocast != this ) {
				replaceWithNodeResolve(type, ocast);
				return;
			}
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
			setResolved(true);
			return;
		}
		if( et.isReference() && et.isInstanceOf((Type)type) ) {
			setResolved(true);
			return;
		}
		if( et.isReference() && type.isReference() && et.isStruct()
		 && et.getStruct().package_clazz.isClazz()
		 && !et.isArgument()
		 && !et.isStaticClazz() && et.getStruct().package_clazz.type.isAutoCastableTo(type)
		) {
			replaceWithNodeResolve(reqType,
				new CastExpr(pos,type,
					new AccessExpr(pos,expr,OuterThisAccessExpr.outerOf((Struct)et.getStruct()))
				));
			return;
		}
		if( expr.isConstantExpr() ) {
			Object val = expr.getConstValue();
			Type t = type;
			if( val instanceof Number ) {
				Number num = (Number)val;
				if     ( t == Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num.doubleValue())); return; }
				else if( t == Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num.floatValue())); return; }
				else if( t == Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num.longValue())); return; }
				else if( t == Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num.intValue())); return; }
				else if( t == Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num.intValue())); return; }
				else if( t == Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num.intValue())); return; }
				else if( t == Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num.intValue())); return; }
			}
			else if( val instanceof Character ) {
				char num = ((Character)val).charValue();
				if     ( t == Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)(int)num)); return; }
				else if( t == Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) (int)num)); return; }
				else if( t == Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  (int)num)); return; }
				else if( t == Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   (int)num)); return; }
				else if( t == Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) (int)num)); return; }
				else if( t == Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  (int)num)); return; }
				else if( t == Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
			else if( val instanceof Boolean ) {
				int num = ((Boolean)val).booleanValue() ? 1 : 0;
				if     ( t == Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num)); return; }
				else if( t == Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num)); return; }
				else if( t == Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num)); return; }
				else if( t == Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num)); return; }
				else if( t == Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num)); return; }
				else if( t == Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num)); return; }
				else if( t == Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
		}
		if( et.equals(type) ) {
			setResolved(true);
			return;
		}
		if( expr instanceof ClosureCallExpr && et instanceof ClosureType ) {
			if( et.isAutoCastableTo(type) ) {
				((ClosureCallExpr)expr).is_a_call = true;
				return;
			}
			else if( et.isCastableTo(type) ) {
				((ClosureCallExpr)expr).is_a_call = true;
			}
		}
		setResolved(true);
	}

	public static void autoCast(ENode ex, TypeRef tp) {
		autoCast(ex, tp.getType());
	}
	public static void autoCast(ENode ex, Type tp) {
		Type at = ex.getType();
		if( !at.equals(tp) ) {
			if( at.isReference() && !tp.isReference() && Type.getRefTypeForPrimitive(tp).equals(at) )
				autoCastToPrimitive(ex);
			else if( !at.isReference() && tp.isReference() && Type.getRefTypeForPrimitive(at).equals(tp) )
				autoCastToReference(ex);
			else if( at.isReference() && tp.isReference() && at.isInstanceOf(tp) )
				;
			else
				ex.replaceWithResolve(tp, fun ()->ENode {return new CastExpr(ex.pos,tp,ex);});
		}
	}

	public static void autoCastToReference(ENode ex) {
		Type tp = ex.getType();
		if( tp.isReference() ) return;
		Type ref;
		if( tp == Type.tpBoolean )		ref = Type.tpBooleanRef;
		else if( tp == Type.tpByte )	ref = Type.tpByteRef;
		else if( tp == Type.tpShort )	ref = Type.tpShortRef;
		else if( tp == Type.tpInt )		ref = Type.tpIntRef;
		else if( tp == Type.tpLong )	ref = Type.tpLongRef;
		else if( tp == Type.tpFloat )	ref = Type.tpFloatRef;
		else if( tp == Type.tpDouble )	ref = Type.tpDoubleRef;
		else if( tp == Type.tpChar )	ref = Type.tpCharRef;
		else
			throw new RuntimeException("Unknown primitive type "+tp);
		ex.replaceWith(fun ()->ENode {return new NewExpr(ex.pos,ref,new ENode[]{ex});});
	}

	public static void autoCastToPrimitive(ENode ex) {
		Type tp = ex.getType();
		if( !tp.isReference() ) return;
		if( tp == Type.tpBooleanRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpBooleanRef.clazz.resolveMethod(
					KString.from("booleanValue"),
					KString.from("()Z")
				),Expr.emptyArray
			);});
		else if( tp == Type.tpByteRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpByteRef.clazz.resolveMethod(
					KString.from("byteValue"),
					KString.from("()B")
				),Expr.emptyArray
			);});
		else if( tp == Type.tpShortRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpShortRef.clazz.resolveMethod(
					KString.from("shortValue"),
					KString.from("()S")
				),Expr.emptyArray
			);});
		else if( tp == Type.tpIntRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpIntRef.clazz.resolveMethod(
					KString.from("intValue"),
					KString.from("()I")
				),Expr.emptyArray
			);});
		else if( tp == Type.tpLongRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpLongRef.clazz.resolveMethod(
					KString.from("longValue"),
					KString.from("()J")
				),Expr.emptyArray
			);});
		else if( tp == Type.tpFloatRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpFloatRef.clazz.resolveMethod(
					KString.from("floatValue"),
					KString.from("()F")
				),Expr.emptyArray
			);});
		else if( tp == Type.tpDoubleRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpDoubleRef.clazz.resolveMethod(
					KString.from("doubleValue"),
					KString.from("()D")
				),Expr.emptyArray
			);});
		else if( tp == Type.tpCharRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,ex,
				Type.tpCharRef.clazz.resolveMethod(
					KString.from("charValue"),
					KString.from("()C")
				),Expr.emptyArray
			);});
		else
			throw new RuntimeException("Type "+tp+" is not a reflection of primitive type");
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
				if( type.isReference() )
					Code.addInstr(Instr.op_checkcast,type.getType());
			} else {
			    if (reinterp) {
			        if (t.isIntegerInCode() && type.isIntegerInCode())
			            ; //generate nothing, both values are int-s
			        else
						throw new CompilerException(pos,"Expression "+expr+" of type "+t+" cannot be reinterpreted to type "+type);
			    } else {
				    Code.addInstr(Instr.op_x2y,type.getType());
				}
			}
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("((").append(type).append(")(");
		dmp.append(expr).append("))");
		return dmp;
	}
}

