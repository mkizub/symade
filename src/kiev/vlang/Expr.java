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
	@ref public ENode expr;
	
	public ShadowExpr() {
	}
	public ShadowExpr(ENode expr) {
		this.expr = expr;
	}
	public Type getType() { return expr.getType(); }
	public void cleanup() {
		parent = null;
		expr   = null;
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

	public void resolve(Type reqType) {
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

	public int getPriority() { return opAccessPriority; }

	public void cleanup() {
		parent=null;
		array.cleanup();
		array = null;
	}

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
@cfnode
public class AssignExpr extends LvalueExpr {
	@ref public AssignOperator	op;
	@att public ENode			lval;
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

	public int getPriority() { return opAssignPriority; }

	public void cleanup() {
		parent=null;
		lval.cleanup();
		lval = null;
		value.cleanup();
		value = null;
	}

	public void resolve(Type reqType) {
		if( isResolved() ) {
			setNodeTypes();
		}
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
					replaceWithResolve(new CallAccessExpr(pos,lval,opt.method,new Expr[]{(Expr)value}), reqType);
					return;
				}
			}
			// Not a standard and not overloaded, try wrapped classes
			if (op != AssignOperator.Assign2) {
				if (et1.isWrapper() && et2.isWrapper()) {
					replaceWithResolve(new AssignExpr(pos,op,et1.makeWrappedAccess(lval),et2.makeWrappedAccess(value)), reqType);
					return;
				}
				else if (et1.isWrapper()) {
					replaceWithResolve(new AssignExpr(pos,op,et1.makeWrappedAccess(lval),value), reqType);
					return;
				}
				else if (et2.isWrapper()) {
					replaceWithResolve(new AssignExpr(pos,op,lval,et2.makeWrappedAccess(value)), reqType);
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
		if (value instanceof Expr) {
			value.resolve(t1);
		} else {
			value = new WrapedExpr(value.pos,value);
			value.resolve(t1);
		}
		Type t2 = value.getType();
		if( op==AssignOperator.AssignLeftShift || op==AssignOperator.AssignRightShift || op==AssignOperator.AssignUnsignedRightShift ) {
			if( !t2.isIntegerInCode() ) {
				value = new CastExpr(pos,Type.tpInt,value);
				value.resolve(Type.tpInt);
			}
		}
		else if( !t1.equals(t2) ) {
			if( t2.isCastableTo(t1) ) {
				value = new CastExpr(pos,t1,value);
				value.resolve(t1);
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
		setResolved(true);
		return this;
	}

	private void setNodeTypes() {
		if !(value instanceof Expr)
			return;
		Expr value = (Expr)this.value;
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

/*
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

	public void resolve(Type reqType) {
		setTryResolved(true);
		if (!(op==AssignOperator.Assign || op==AssignOperator.Assign2))
			throw new CompilerException(pos,"Initializer must use = or :=");
		lval = lval.resolveExpr(reqType);
		Type et1 = lval.getType();
		if (op == AssignOperator.Assign && et1.isWrapper())
			value = ((CFlowNode)value).resolve(et1.getWrappedType());
		else
			value = ((CFlowNode)value).resolve(et1);
		Type et2 = value.getType();
		if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !et1.isWrapper() && !et2.isWrapper()) {
			return (Expr)super.resolve(reqType);
		}
		else if((of_wrapper || op == AssignOperator.Assign2) && et1.isWrapper() && (et2 == Type.tpNull || et2.isInstanceOf(et1))) {
			return (Expr)super.resolve(reqType);
		}
		// Try wrapped classes
		if (op != AssignOperator.Assign2) {
			if (et2.isWrapper()) {
				return new InitializeExpr(pos,op,lval,et2.makeWrappedAccess(value),of_wrapper).resolveExpr(reqType);
			}
		}
		return super.resolve(reqType);
		//throw new CompilerException(pos,"Unresolved initializer expression "+this);
	}

}
*/


@node
@cfnode
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
		resolve(null);
		return getType();
//		if( e == null )
//			Kiev.reportError(pos,"Type of binary operation "+op.image+" between "+expr1+" and "+expr2+" unknown, types are "+t1+" and "+t2);
//		else
//			return e.getType();
//		return Type.tpVoid;
	}

	public void cleanup() {
		parent=null;
		expr1.cleanup();
		expr1 = null;
		expr2.cleanup();
		expr2 = null;
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
					replaceWithResolve(sce, Type.tpString);
				} else {
					StringConcatExpr sce = new StringConcatExpr(pos);
					if (et1.isWrapper()) expr1 = et1.makeWrappedAccess(expr1);
					sce.appendArg(expr1);
					if (et2.isWrapper()) expr2 = et2.makeWrappedAccess(expr2);
					sce.appendArg(expr2);
					trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
					replaceWithResolve(sce, Type.tpString);
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
			}
			else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
				(    op==BinaryOperator.LeftShift
				||   op==BinaryOperator.RightShift
				||   op==BinaryOperator.UnsignedRightShift
				)
			) {
				this.postResolve(null);
			}
			else if( ( (et1.isInteger() && et2.isInteger()) || (et1.isBoolean() && et2.isBoolean()) ) &&
				(    op==BinaryOperator.BitOr
				||   op==BinaryOperator.BitXor
				||   op==BinaryOperator.BitAnd
				)
			) {
				this.postResolve(null);
			}
			// Not a standard operator, find out overloaded
			foreach(OpTypes opt; op.types ) {
				Type[] tps = new Type[]{null,et1,et2};
				ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
				if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
					Expr e;
					if( opt.method.isStatic() )
						replaceWithResolve(new CallExpr(pos,opt.method,new ENode[]{expr1,expr2}), reqType);
					else
						replaceWithResolve(new CallAccessExpr(pos,expr1,opt.method,new ENode[]{expr2}), reqType);
					return;
				}
			}
			// Not a standard and not overloaded, try wrapped classes
			if (et1.isWrapper() && et2.isWrapper()) {
				replaceWithResolve(new BinaryExpr(pos,op,et1.makeWrappedAccess(expr1),et2.makeWrappedAccess(expr2)), reqType);
				return;
			}
			if (et1.isWrapper()) {
				replaceWithResolve(new BinaryExpr(pos,op,et1.makeWrappedAccess(expr1),expr2), reqType);
				return;
			}
			if (et2.isWrapper()) {
				replaceWithResolve(new BinaryExpr(pos,op,expr1,et2.makeWrappedAccess(expr2)), reqType);
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
				replaceWithResolve(sce, Type.tpString);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				sce.appendArg(expr1);
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
				replaceWithResolve(sce, Type.tpString);
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
					replaceWithResolve(new ConstLongExpr(val1.longValue() | val2.longValue()), null);
				else if( val1 instanceof Integer || val2 instanceof Integer )
					replaceWithResolve(new ConstIntExpr(val1.intValue() | val2.intValue()), null);
				else if( val1 instanceof Short || val2 instanceof Short )
					replaceWithResolve(new ConstShortExpr(val1.shortValue() | val2.shortValue()), null);
				else if( val1 instanceof Byte || val2 instanceof Byte )
					replaceWithResolve(new ConstByteExpr(val1.byteValue() | val2.byteValue()), null);
			}
			else if( op == BinaryOperator.BitXor ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() ^ val2.longValue()), null);
				else if( val1 instanceof Integer || val2 instanceof Integer )
					replaceWithResolve(new ConstIntExpr(val1.intValue() ^ val2.intValue()), null);
				else if( val1 instanceof Short || val2 instanceof Short )
					replaceWithResolve(new ConstShortExpr(val1.shortValue() ^ val2.shortValue()), null);
				else if( val1 instanceof Byte || val2 instanceof Byte )
					replaceWithResolve(new ConstByteExpr(val1.byteValue() ^ val2.byteValue()), null);
			}
			else if( op == BinaryOperator.BitAnd ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() & val2.longValue()), null);
				else if( val1 instanceof Integer || val2 instanceof Integer )
					replaceWithResolve(new ConstIntExpr(val1.intValue() & val2.intValue()), null);
				else if( val1 instanceof Short || val2 instanceof Short )
					replaceWithResolve(new ConstShortExpr(val1.shortValue() & val2.shortValue()), null);
				else if( val1 instanceof Byte || val2 instanceof Byte )
					replaceWithResolve(new ConstByteExpr(val1.byteValue() & val2.byteValue()), null);
			}
			else if( op == BinaryOperator.LeftShift ) {
				if( val1 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() << val2.intValue()), null);
				else
					replaceWithResolve(new ConstIntExpr(val1.intValue() << val2.intValue()), null);
			}
			else if( op == BinaryOperator.RightShift ) {
				if( val1 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() >> val2.intValue()), null);
				else if( val1 instanceof Integer )
					replaceWithResolve(new ConstIntExpr(val1.intValue() >> val2.intValue()), null);
				else if( val1 instanceof Short )
					replaceWithResolve(new ConstShortExpr(val1.shortValue() >> val2.intValue()), null);
				else if( val1 instanceof Byte )
					replaceWithResolve(new ConstByteExpr(val1.byteValue() >> val2.intValue()), null);
			}
			else if( op == BinaryOperator.UnsignedRightShift ) {
				if( val1 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() >>> val2.intValue()), null);
				else if( val1 instanceof Integer )
					replaceWithResolve(new ConstIntExpr(val1.intValue() >>> val2.intValue()), null);
				else if( val1 instanceof Short )
					replaceWithResolve(new ConstShortExpr(val1.shortValue() >>> val2.intValue()), null);
				else if( val1 instanceof Byte )
					replaceWithResolve(new ConstByteExpr(val1.byteValue() >>> val2.intValue()), null);
			}
			else if( op == BinaryOperator.Add ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithResolve(new ConstDoubleExpr(val1.doubleValue() + val2.doubleValue()), null);
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithResolve(new ConstFloatExpr(val1.floatValue() + val2.floatValue()), null);
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() + val2.longValue()), null);
				else
					replaceWithResolve(new ConstIntExpr(val1.intValue() + val2.intValue()), null);
			}
			else if( op == BinaryOperator.Sub ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithResolve(new ConstDoubleExpr(val1.doubleValue() - val2.doubleValue()), null);
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithResolve(new ConstFloatExpr(val1.floatValue() - val2.floatValue()), null);
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() - val2.longValue()), null);
				else
					replaceWithResolve(new ConstIntExpr(val1.intValue() - val2.intValue()), null);
			}
			else if( op == BinaryOperator.Mul ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithResolve(new ConstDoubleExpr(val1.doubleValue() * val2.doubleValue()), null);
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithResolve(new ConstFloatExpr(val1.floatValue() * val2.floatValue()), null);
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() * val2.longValue()), null);
				else
					replaceWithResolve(new ConstIntExpr(val1.intValue() * val2.intValue()), null);
			}
			else if( op == BinaryOperator.Div ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithResolve(new ConstDoubleExpr(val1.doubleValue() / val2.doubleValue()), null);
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithResolve(new ConstFloatExpr(val1.floatValue() / val2.floatValue()), null);
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() / val2.longValue()), null);
				else
					replaceWithResolve(new ConstIntExpr(val1.intValue() / val2.intValue()), null);
			}
			else if( op == BinaryOperator.Mod ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithResolve(new ConstDoubleExpr(val1.doubleValue() % val2.doubleValue()), null);
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithResolve(new ConstFloatExpr(val1.floatValue() % val2.floatValue()), null);
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithResolve(new ConstLongExpr(val1.longValue() % val2.longValue()), null);
				else
					replaceWithResolve(new ConstIntExpr(val1.intValue() % val2.intValue()), null);
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
@cfnode
public class StringConcatExpr extends Expr {
	@att public final NArr<ENode>	args;

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
@cfnode
public class CommaExpr extends Expr {
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

	public void cleanup() {
		parent=null;
		exprs.cleanup();
	}

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
@cfnode
public class BlockExpr extends Expr implements ScopeOfNames, ScopeOfMethods {

	@att public final NArr<ENode>		stats;
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
			n instanceof Var,
			((Var)n).name.equals(name),
			node ?= n
		;	n instanceof Struct,
			name.equals(((Struct)n).name.short_name),
			node ?= n
		;	n instanceof Typedef,
			name.equals(((Typedef)n).name),
			node ?= ((Typedef)n).type
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this,this.stats),
		n instanceof Var && n.isForward() && ((Var)n).name.equals(name),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= stats,
		n instanceof Var && n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			resolveBlockStats();
			if (res != null) {
				res.resolve(reqType);
			}
		} finally {
			Vector<Var> vars = new Vector<Var>();
			foreach (ASTNode n; stats; n instanceof Var) vars.append((Var)n);
			ScopeNodeInfoVector nip_state = NodeInfoPass.popState();
			nip_state = NodeInfoPass.cleanInfoForVars(nip_state,vars.toArray());
			NodeInfoPass.addInfo(nip_state);
			PassInfo.pop(this);
		}
	}

	public void resolveBlockStats() {
		for(int i=0; i < stats.length; i++) {
			try {
				if( stats[i] instanceof Statement ) {
					Statement st = (Statement)stats[i];
					st.resolve(Type.tpVoid);
					st = (Statement)stats[i];
					if( st.isAbrupted() ) {
						Kiev.reportError(st.pos,"Abrupted statement in BockExpr");
					}
				}
//				else if( stats[i] instanceof ASTVarDecls ) {
//					ASTVarDecls vdecls = (ASTVarDecls)stats[i];
//					// TODO: check flags for vars
//					int flags = vdecls.modifiers.getFlags();
//					Type type = ((TypeRef)vdecls.type).getType();
//					for(int j=0; j < vdecls.vars.length; j++) {
//						ASTVarDecl vdecl = (ASTVarDecl)vdecls.vars[j];
//						KString vname = vdecl.name.name;
//						Type tp = type;
//						for(int k=0; k < vdecl.dim; k++) tp = Type.newArrayType(tp);
//						Var vstat;
//						if( vdecl.init != null ) {
//							if (!type.isWrapper() || vdecl.of_wrapper) {
//								vstat = new Var(vdecl.pos,vname,tp,flags);
//								vstat.init = vdecl.init;
//							} else {
//								vstat = new Var(vdecl.pos,vname,tp,flags);
//								vstat.init = new NewExpr(vdecl.init.pos,type,new Expr[]{vdecl.init});
//							}
//						}
//						else if( vdecl.dim == 0 && type.isWrapper() && !vdecl.of_wrapper) {
//							vstat = new Var(vdecl.pos,vname,tp,flags);
//							vstat.init = new NewExpr(vdecl.pos,type,Expr.emptyArray);
//						} else {
//							vstat = new Var(vdecl.pos,vname,tp,flags);
//						}
//						if (j == 0) {
//							stats[i] = vstat;
//							vstat.resolve(Type.tpVoid);
//						} else {
//							this.insertSymbol(vstat,i+j);
//						}
//					}
//				}
//				else if( stats[i] instanceof Var ) {
//					Var var = (Var)stats[i];
//					var.resolve(Type.tpVoid);
//				}
//				else if( stats[i] instanceof Struct ) {
//					Struct decl = (Struct)stats[i];
//					if( PassInfo.method==null || PassInfo.method.isStatic())
//						decl.setStatic(true);
//					ExportJavaTop exporter = new ExportJavaTop();
//					decl.setLocal(true);
//					exporter.pass1(decl);
//					exporter.pass1_1(decl);
//					exporter.pass2(decl);
//					exporter.pass2_2(decl);
//					exporter.pass3(decl);
//					decl.autoProxyMethods();
//					decl.resolveFinalFields(false);
//					decl.resolve(Type.tpVoid);
//					//stats[i] = decl;
//				}
				else {
					//Kiev.reportError(stats[i].pos,"Unknown kind of statement/declaration "+stats[i].getClass());
					stats[i].resolve(Type.tpVoid);
				}
			} catch(Exception e ) {
				Kiev.reportError(stats[i].pos,e);
			}
		}
	}


	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockExpr");
		PassInfo.push(this);
		try {
			for(int i=0; i < stats.length; i++) {
				try {
					ASTNode n = stats[i];
					if (n instanceof Statement)
						((Statement)n).generate(Type.tpVoid);
					else if (n instanceof Expr)
						((Expr)n).generate(Type.tpVoid);
					else if (n instanceof Var)
						((Var)n).generate(Type.tpVoid);
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
			foreach (ASTNode n; stats; n instanceof Var) vars.append((Var)n);
			Code.removeVars(vars.toArray());
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		stats.cleanup();
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

	public int getPriority() { return op.priority; }

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

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
				replaceWithResolve(new IncrementExpr(pos,op,expr), reqType);
				return;
			}
			if( et.isAutoCastableTo(Type.tpBoolean) &&
				(  op==PrefixOperator.PreIncr
				|| op==PrefixOperator.BooleanNot
				)
			) {
				replaceWithResolve(new BooleanNotExpr(pos,expr), Type.tpBoolean);
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
					if ( opt.method.isStatic() || opt.method.type.args.length == 1)
						replaceWithResolve(new CallExpr(pos,opt.method,new ENode[]{expr}), reqType);
					else
						replaceWithResolve(new CallAccessExpr(pos,expr,opt.method,Expr.emptyArray), reqType);
					return;
				}
			}
			// Not a standard and not overloaded, try wrapped classes
			if (et.isWrapper()) {
				replaceWithResolve(new UnaryExpr(pos,op,et.makeWrappedAccess(expr)), reqType);
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
			replaceWithResolve(new IncrementExpr(pos,op,expr), null);
			return;
		} else if( op==PrefixOperator.BooleanNot ) {
			replaceWithResolve(new BooleanNotExpr(pos,expr), reqType);
			return;
		}
		// Check if expression is constant
		if( expr.isConstantExpr() ) {
			Number val = (Number)expr.getConstValue();
			if( op == PrefixOperator.Pos ) {
				if( val instanceof Double )
					replaceWithResolve(new ConstDoubleExpr(val.doubleValue()), null);
				else if( val instanceof Float )
					replaceWithResolve(new ConstFloatExpr(val.floatValue()), null);
				else if( val instanceof Long )
					replaceWithResolve(new ConstLongExpr(val.longValue()), null);
				else if( val instanceof Integer )
					replaceWithResolve(new ConstIntExpr(val.intValue()), null);
				else if( val instanceof Short )
					replaceWithResolve(new ConstShortExpr(val.shortValue()), null);
				else if( val instanceof Byte )
					replaceWithResolve(new ConstByteExpr(val.byteValue()), null);
			}
			else if( op == PrefixOperator.Neg ) {
				if( val instanceof Double )
					replaceWithResolve(new ConstDoubleExpr(-val.doubleValue()), null);
				else if( val instanceof Float )
					replaceWithResolve(new ConstFloatExpr(-val.floatValue()), null);
				else if( val instanceof Long )
					replaceWithResolve(new ConstLongExpr(-val.longValue()), null);
				else if( val instanceof Integer )
					replaceWithResolve(new ConstIntExpr(-val.intValue()), null);
				else if( val instanceof Short )
					replaceWithResolve(new ConstShortExpr(-val.shortValue()), null);
				else if( val instanceof Byte )
					replaceWithResolve(new ConstByteExpr(-val.byteValue()), null);
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
@cfnode
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

	public int getPriority() { return op.priority; }

	public void cleanup() {
		parent=null;
		lval.cleanup();
		lval = null;
	}

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
/*
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
			return (Expr)new ConditionalExpr(pos,(Expr)cond.copy(),(Expr)expr1.copy(),(Expr)expr2.copy()).resolve(reqType);
		}
		throw new CompilerException(pos,"Multi-operators are not implemented");
	}
}
*/

@node
@cfnode
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

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		NodeInfoPass.pushState();
		ScopeNodeInfoVector result_state = null;
		try {
			cond.resolve(Type.tpBoolean);
			NodeInfoPass.pushState();
			if( cond instanceof InstanceofExpr ) ((InstanceofExpr)cond).setNodeTypeInfo();
			else if( cond instanceof BinaryBooleanAndExpr ) {
				BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)cond;
				if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
				if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
			}
			expr1.resolve(reqType);
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
			expr2.resolve(reqType);
			ScopeNodeInfoVector else_state = NodeInfoPass.popState();

			result_state = NodeInfoPass.joinInfo(then_state,else_state);

			if( expr1.getType() != getType() ) {
				expr1 = new CastExpr(expr1.pos,getType(),expr1);
				expr1.resolve(getType());
			}
			if( expr2.getType() != getType() ) {
				expr2 = new CastExpr(expr2.pos,getType(),expr2);
				expr2.resolve(getType());
			}
		} finally {
			NodeInfoPass.popState();
			if( result_state != null ) NodeInfoPass.addInfo(result_state);
			PassInfo.pop(this);
		}
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
@cfnode
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

	public void cleanup() {
		parent=null;
		type = null;
		expr.cleanup();
		expr = null;
	}

	public Type[] getAccessTypes() {
		Type[] types = expr.getAccessTypes();
		return NodeInfoPass.addAccessType(types,type.getType());
	}

	public int getPriority() { return opCastPriority; }

	public void resolve(Type reqType) {
		if( isResolved() ) {
			setNodeCastType();
			return;
		}
		PassInfo.push(this);
		try {
			expr.resolve(type.getType());
			Type extp = Type.getRealType(type,expr.getType());
			if( type.getType() == Type.tpBoolean && extp == Type.tpRule ) {
				replaceWith(expr);
				return;
			}
			// Try to find $cast method
			if( !extp.isAutoCastableTo(type.getType()) ) {
				ENode ocast = tryOverloadedCast(extp);
				if( ocast == this ) {
					resolve(reqType);
					return;
				}
				if (extp.isWrapper()) {
					replaceWithResolve(new CastExpr(pos,type,extp.makeWrappedAccess(expr),reinterp), reqType);
					return;
				}
			}
			else if (extp.isWrapper() && extp.getWrappedType().isAutoCastableTo(type.getType())) {
				ENode ocast = tryOverloadedCast(extp);
				if( ocast == this ) {
					resolve(reqType);
					return;
				}
				replaceWithResolve(new CastExpr(pos,type,extp.makeWrappedAccess(expr),reinterp), reqType);
				return;
			}
			else {
				this.postResolve(type.getType());
				return;
			}
			if( extp.isCastableTo(type.getType()) ) {
				this.postResolve(type.getType());
				return;
			}
			if( type == Type.tpInt && extp == Type.tpBoolean && reinterp ) {	
				this.postResolve(type.getType());
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
			expr = new CallAccessExpr(pos,expr,(Method)v,new ENode[]{expr});
			expr.resolve(type.getType());
			return this;
		}
		return null;
	}

	private void postResolve(Type reqType) {
		expr.resolve(type.getType());
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
			replaceWithResolve(et.makeWrappedAccess(expr), reqType);
			return;
		}
		// try null to something...
		if (et == Type.tpNull && reqType.isReference())
			return;
		if( type == Type.tpBoolean && et == Type.tpRule ) {
			replaceWithResolve(new BinaryBoolExpr(pos,BinaryOperator.NotEquals,expr,new ConstNullExpr()), type.getType());
			return;
		}
		if( type.isBoolean() && et.isBoolean() )
			return;
		if( !Kiev.javaMode && type.isInstanceOf(Type.tpEnum) && et.isIntegerInCode() ) {
			if (type.isIntegerInCode())
				return;
			Method cm = null;
			cm = type.resolveMethod(nameCastOp,KString.from("(I)"+type.signature));
			replaceWithResolve(new CallExpr(pos,cm,new ENode[]{expr}), reqType);
			return;
		}
		if( !Kiev.javaMode && type.isIntegerInCode() && et.isInstanceOf(Type.tpEnum) ) {
			if (et.isIntegerInCode())
				return;
			Method cf = (Method)Type.tpEnum.resolveMethod(nameEnumOrdinal, KString.from("()I"));
			replaceWithResolve(new CallAccessExpr(pos,expr,cf,Expr.emptyArray), reqType);
			return;
		}
		// Try to find $cast method
		if( !et.isAutoCastableTo(type.getType()) ) {
			ENode ocast = tryOverloadedCast(et);
			if( ocast != null && ocast != this ) {
				replaceWith(ocast);
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
			setNodeCastType();
			return;
		}
		if( et.isReference() && et.isInstanceOf((Type)type) ) {
			setResolved(true);
			setNodeCastType();
			return;
		}
		if( et.isReference() && type.isReference() && et.isStruct()
		 && et.getStruct().package_clazz.isClazz()
		 && !et.isArgument()
		 && !et.isStaticClazz() && et.getStruct().package_clazz.type.isAutoCastableTo(type.getType())
		) {
			replaceWithResolve(
				new CastExpr(pos,type,
					new AccessExpr(pos,expr,OuterThisAccessExpr.outerOf((Struct)et.getStruct()))
				),
				reqType);
			return;
		}
		if( expr.isConstantExpr() ) {
			Object val = expr.getConstValue();
			Type t = type.getType();
			if( val instanceof Number ) {
				Number num = (Number)val;
				if     ( t == Type.tpDouble ) { replaceWithResolve(new ConstDoubleExpr ((double)num.doubleValue()), null); return; }
				else if( t == Type.tpFloat )  { replaceWithResolve(new ConstFloatExpr  ((float) num.floatValue()), null); return; }
				else if( t == Type.tpLong )   { replaceWithResolve(new ConstLongExpr   ((long)  num.longValue()), null); return; }
				else if( t == Type.tpInt )    { replaceWithResolve(new ConstIntExpr    ((int)   num.intValue()), null); return; }
				else if( t == Type.tpShort )  { replaceWithResolve(new ConstShortExpr  ((short) num.intValue()), null); return; }
				else if( t == Type.tpByte )   { replaceWithResolve(new ConstByteExpr   ((byte)  num.intValue()), null); return; }
				else if( t == Type.tpChar )   { replaceWithResolve(new ConstCharExpr   ((char)  num.intValue()), null); return; }
			}
			else if( val instanceof Character ) {
				char num = ((Character)val).charValue();
				if     ( t == Type.tpDouble ) { replaceWithResolve(new ConstDoubleExpr ((double)(int)num), null); return; }
				else if( t == Type.tpFloat )  { replaceWithResolve(new ConstFloatExpr  ((float) (int)num), null); return; }
				else if( t == Type.tpLong )   { replaceWithResolve(new ConstLongExpr   ((long)  (int)num), null); return; }
				else if( t == Type.tpInt )    { replaceWithResolve(new ConstIntExpr    ((int)   (int)num), null); return; }
				else if( t == Type.tpShort )  { replaceWithResolve(new ConstShortExpr  ((short) (int)num), null); return; }
				else if( t == Type.tpByte )   { replaceWithResolve(new ConstByteExpr   ((byte)  (int)num), null); return; }
				else if( t == Type.tpChar )   { replaceWithResolve(new ConstCharExpr   ((char)  num), null); return; }
			}
			else if( val instanceof Boolean ) {
				int num = ((Boolean)val).booleanValue() ? 1 : 0;
				if     ( t == Type.tpDouble ) { replaceWithResolve(new ConstDoubleExpr ((double)num), null); return; }
				else if( t == Type.tpFloat )  { replaceWithResolve(new ConstFloatExpr  ((float) num), null); return; }
				else if( t == Type.tpLong )   { replaceWithResolve(new ConstLongExpr   ((long)  num), null); return; }
				else if( t == Type.tpInt )    { replaceWithResolve(new ConstIntExpr    ((int)   num), null); return; }
				else if( t == Type.tpShort )  { replaceWithResolve(new ConstShortExpr  ((short) num), null); return; }
				else if( t == Type.tpByte )   { replaceWithResolve(new ConstByteExpr   ((byte)  num), null); return; }
				else if( t == Type.tpChar )   { replaceWithResolve(new ConstCharExpr   ((char)  num), null); return; }
			}
		}
		if( et.equals(type) ) {
			setResolved(true);
			setNodeCastType();
			return;
		}
		if( expr instanceof ClosureCallExpr && et instanceof ClosureType ) {
			if( et.isAutoCastableTo(type.getType()) ) {
				((ClosureCallExpr)expr).is_a_call = true;
				return;
			}
			else if( et.isCastableTo(type.getType()) ) {
				((ClosureCallExpr)expr).is_a_call = true;
			}
		}
		setNodeCastType();
		setResolved(true);
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
		NodeInfoPass.setNodeTypes(n,NodeInfoPass.addAccessType(expr.getAccessTypes(),type.getType()));
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
				ex.replaceWithResolve(new CastExpr(ex.pos,tp,ex), tp);
		}
	}

	public static void autoCastToReference(ENode ex) {
		Type tp = ex.getType();
		if( tp.isReference() ) return;
		if( tp == Type.tpBoolean )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpBooleanRef,new ENode[]{ex}), null);
		else if( tp == Type.tpByte )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpByteRef,new ENode[]{ex}), null);
		else if( tp == Type.tpShort )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpShortRef,new ENode[]{ex}), null);
		else if( tp == Type.tpInt )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpIntRef,new ENode[]{ex}), null);
		else if( tp == Type.tpLong )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpLongRef,new ENode[]{ex}), null);
		else if( tp == Type.tpFloat )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpFloatRef,new ENode[]{ex}), null);
		else if( tp == Type.tpDouble )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpDoubleRef,new ENode[]{ex}), null);
		else if( tp == Type.tpChar )
			ex.replaceWithResolve(new NewExpr(ex.pos,Type.tpCharRef,new ENode[]{ex}), null);
		else
			throw new RuntimeException("Unknown primitive type "+tp);
	}

	public static void autoCastToPrimitive(ENode ex) {
		Type tp = ex.getType();
		if( !tp.isReference() ) return;
		if( tp == Type.tpBooleanRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpBooleanRef.clazz.resolveMethod(
					KString.from("booleanValue"),
					KString.from("()Z")
				),Expr.emptyArray
			));
		else if( tp == Type.tpByteRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpByteRef.clazz.resolveMethod(
					KString.from("byteValue"),
					KString.from("()B")
				),Expr.emptyArray
			));
		else if( tp == Type.tpShortRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpShortRef.clazz.resolveMethod(
					KString.from("shortValue"),
					KString.from("()S")
				),Expr.emptyArray
			));
		else if( tp == Type.tpIntRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpIntRef.clazz.resolveMethod(
					KString.from("intValue"),
					KString.from("()I")
				),Expr.emptyArray
			));
		else if( tp == Type.tpLongRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpLongRef.clazz.resolveMethod(
					KString.from("longValue"),
					KString.from("()J")
				),Expr.emptyArray
			));
		else if( tp == Type.tpFloatRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpFloatRef.clazz.resolveMethod(
					KString.from("floatValue"),
					KString.from("()F")
				),Expr.emptyArray
			));
		else if( tp == Type.tpDoubleRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpDoubleRef.clazz.resolveMethod(
					KString.from("doubleValue"),
					KString.from("()D")
				),Expr.emptyArray
			));
		else if( tp == Type.tpCharRef )
			ex.replaceWith(new CallAccessExpr(ex.pos,ex,
				Type.tpCharRef.clazz.resolveMethod(
					KString.from("charValue"),
					KString.from("()C")
				),Expr.emptyArray
			));
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

