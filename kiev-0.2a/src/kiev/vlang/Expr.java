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

import kiev.vlang.Instr.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Expr.java,v 1.6.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.2 $
 *
 */

public class StatExpr extends Expr implements SetBody {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Statement	stat;

	public StatExpr(int pos, Statement stat) {
		super(pos);
		this.stat = stat;
		if( stat != null )
			this.stat.parent = this;
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
		if( stat != null )
			this.stat.parent = this;
		else
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
		this.stat.parent = parent;
		return true;
	}

}

public class ConstExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	Object	value;

	public ConstExpr(int pos, Object value) {
		super(pos);
		this.value = value;
	}

	public Type getType() {
		if( value == null )					return Type.tpNull;
		if( value instanceof Boolean )		return Type.tpBoolean;
		if( value instanceof Byte )			return Type.tpByte;
		if( value instanceof Short )		return Type.tpShort;
		if( value instanceof Integer )		return Type.tpInt;
		if( value instanceof Long )			return Type.tpLong;
		if( value instanceof Float )		return Type.tpFloat;
		if( value instanceof Double )		return Type.tpDouble;
		if( value instanceof KString )		return Type.tpString;
		if( value instanceof java.lang.Character )		return Type.tpChar;
		throw new Error("Internal error: unknown type of constant "+value.getClass());
	}

	public String toString() {
		if( value == null )
			return "null";
		else if( value instanceof KString )
			return '\"'+value.toString()+'\"'; //"
		else if( value instanceof java.lang.Character ) {
			char ch = ((java.lang.Character)value).charValue();
			return "'"+Convert.escape(ch)+"'";
		} else {
			String val =  value.toString();
			if( value instanceof Long ) val = val+'L';
			else if( value instanceof Float ) val = val+'F';
			else if( value instanceof Double ) val = val+'D';
			return val;
		}
	}

	public boolean isConstantExpr() { return true; }
	public Object getConstValue() { return value; }
	public int		getPriority() { return 255; }

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( value instanceof Boolean ) {
			ConstBooleanExpr cbe = new ConstBooleanExpr(pos, ((Boolean)value).booleanValue() );
			return cbe.resolve(reqType);
		}
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstExpr: "+value);
		PassInfo.push(this);
		try {
			if( value == null ) {
				// Special case for generation of parametriezed
				// with primitive types classes
				if( reqType != null && !reqType.isReference() ) {
					switch(reqType.signature.byteAt(0)) {
					case 'Z': case 'B': case 'S': case 'I': case 'C':
						Code.addConst(0);
						break;
					case 'J':
						Code.addConst(0L);
						break;
					case 'F':
						Code.addConst(0.F);
						break;
					case 'D':
						Code.addConst(0.D);
						break;
					default:
						Code.addNullConst();
						break;
					}
				}
				else
					Code.addNullConst();
			}
			else if( value instanceof Byte ) {
				Code.addConst(((Byte)value).intValue());
			}
			else if( value instanceof Short ) {
				Code.addConst(((Short)value).intValue());
			}
			else if( value instanceof Integer ) {
				Code.addConst(((Integer)value).intValue());
			}
			else if( value instanceof Character ) {
				Code.addConst((int)((Character)value).charValue());
			}
			else if( value instanceof Long ) {
				Code.addConst(((Long)value).longValue());
			}
			else if( value instanceof Float ) {
				Code.addConst(((Float)value).floatValue());
			}
			else if( value instanceof Double ) {
				Code.addConst(((Double)value).doubleValue());
			}
			else if( value instanceof KString ) {
				Code.addConst((KString)value);
			}
			else throw new RuntimeException("Internal error: unknown type of constant "+value.getClass());
			if( reqType == Type.tpVoid ) Code.addInstr(op_pop);
		} finally { PassInfo.pop(this); }
	}

	public Dumper	toJava(Dumper dmp) {
		if( value == null ) dmp.space().append("null").space();
		else if( value instanceof Number ) {
			if( value instanceof Long ) dmp.append(value).append('L');
			else if( value instanceof Float ) dmp.append(value).append('F');
			else if( value instanceof Double ) dmp.append(value).append('D');
			else dmp.append(value);
		}
		else if( value instanceof KString ) {
			dmp.append('\"');
			byte[] val = Convert.string2source(value.toString());
			dmp.append(new String(val,0));
			dmp.append('\"');
		}
		else if( value instanceof java.lang.Boolean )
			if( ((Boolean)value).booleanValue() )
				dmp.space().append("true").space();
			else
				dmp.space().append("false").space();
		else if( value instanceof java.lang.Character ) {
			char ch = ((java.lang.Character)value).charValue();
			return dmp.append('\'').append(Convert.escape(ch)).append('\'');
		}
		else throw new Error("Internal error: unknown type of constant "+value.getClass());
		return dmp;
	}

	public Dumper toCpp(Dumper dmp) {
		if( value == null ) return dmp.space().append("NULL").space();
		return toJava(dmp);
	}

    public static long parseLong(String s, int radix)
              throws NumberFormatException
    {
        if (s == null) {
            throw new NumberFormatException("null");
        }
	long result = 0;
	boolean negative = false;
	int i = 0, max = s.length();
	long limit;
	long multmin;
	int digit;

	if (max > 0) {
	    if (s.charAt(0) == '-') {
		negative = true;
		i++;
	    }
		limit = Long.MIN_VALUE;
	    multmin = limit / radix;
            if (i < max) {
                digit = Character.digit(s.charAt(i++),radix);
		if (digit < 0) {
		    throw new NumberFormatException(s);
		} else {
		    result = -digit;
		}
	    }
	    while (i < max) {
		// Accumulating negatively avoids surprises near MAX_VALUE
		digit = Character.digit(s.charAt(i++),radix);
		if (digit < 0) {
		    throw new NumberFormatException(s);
		}
//		if (result < multmin) {
//		    throw new NumberFormatException(s);
//		}
		result *= radix;
//		if (result < limit + digit) {
//		    throw new NumberFormatException(s);
//		}
		result -= digit;
	    }
	} else {
	    throw new NumberFormatException(s);
	}
	if (negative) {
	    if (i > 1) {
		return result;
	    } else {
		throw new NumberFormatException(s);
	    }
	} else {
	    return -result;
	}
    }

}

public class ArrayLengthAccessExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Expr		array;

	public ArrayLengthAccessExpr(int pos, Expr array) {
		super(pos);
		this.array = array;
		this.array.parent = this;
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

public class AssignExpr extends LvalueExpr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public AssignOperator	op;
	public Expr				lval;
	public Expr				value;

	public AssignExpr(int pos, AssignOperator op, Expr lval, Expr value) {
		super(pos);
		this.op = op;
		this.lval = lval;
		this.lval.parent = this;
		this.value = value;
		this.value.parent = this;
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
		if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !et1.clazz.isWrapper() && !et2.clazz.isWrapper()) {
			return (Expr)this.resolve(reqType);
		}
		else if( op == AssignOperator.Assign2 && et1.clazz.isWrapper() && et2.isInstanceOf(et1)) {
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
			if (et1.clazz.isWrapper()) {
				Expr e = new AssignExpr(pos,op,new AccessExpr(lval.pos,lval,et1.clazz.wrapped_field),value).tryResolve(reqType);
				if (e != null) return e;
			}
			if (et2.clazz.isWrapper()) {
				Expr e = new AssignExpr(pos,op,lval,new AccessExpr(value.pos,value,et2.clazz.wrapped_field)).tryResolve(reqType);
				if (e != null) return e;
			}
			if (et1.clazz.isWrapper() && et2.clazz.isWrapper()) {
				Expr e = new AssignExpr(pos,op,new AccessExpr(lval.pos,lval,et1.clazz.wrapped_field),new AccessExpr(value.pos,value,et2.clazz.wrapped_field)).tryResolve(reqType);
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
					var.setNeedRefProxy(true);
					Field vf = (Field)PassInfo.clazz.resolveName(var.name.name);
					vf.type = Type.getProxyType(var.type);
				}
			}
			if( Kiev.kaffe &&
				(lv instanceof VarAccessExpr) &&
				((VarAccessExpr)lv).var.isClosureProxy() &&
				!((VarAccessExpr)lv).var.isNeedRefProxy()
			) {
				// Check that we in local closure, thus var need RefProxy
				Var var = ((VarAccessExpr)lv).var;
				ASTNode p = var.parent;
				while( !(p instanceof Method) ) p = p.parent;
				if( p == PassInfo.method && p.isLocalMethod() ) {
					var.setNeedRefProxy(true);
					// Change type of closure method
					Type[] types = (Type[])PassInfo.method.type.args.clone();
					int i=0;
					for(; i < PassInfo.method.params.length; i++)
						if(PassInfo.method.params[i].name.equals(var.name))
							break;
					Debug.assert(i < PassInfo.method.params.length,"Can't find method parameter "+var);
					if( !PassInfo.method.isStatic() )
						i--;
					types[i] = Type.getProxyType(var.type);
					PassInfo.method.type = MethodType.newMethodType(
						null,types,PassInfo.method.type.ret);
					// Need to resolve initial var and mark it as RefProxy
					KString name = var.name.name;
					PVar<ASTNode> v = new PVar<ASTNode>();
					PVar<List<ASTNode>> path = new PVar<List<ASTNode>>(List.Nil);
					if( !PassInfo.resolveNameR(v,path,name,null,0) ) {
						Kiev.reportError(pos,"Internal error: can't find var "+name);
					}
					Var pv = (Var)v.$var;
					pv.setNeedRefProxy(true);
				}
			}
			lval = (Expr)lv;
			Type t1 = lval.getType();
			if( op==AssignOperator.AssignAdd && t1==Type.tpString ) {
				op = AssignOperator.Assign;
				value = new BinaryExpr(pos,BinaryOperator.Add,lval,value);
				value.parent = this;
			}
			value = value.resolveExpr(t1);
			if( value.isConstantExpr() && !t1.clazz.isPrimitiveEnum())
				value = new ConstExpr(value.pos,value.getConstValue()).resolveExpr(t1);
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
			if( lval instanceof FieldAccessExpr || lval instanceof StaticFieldAccessExpr
			 || (
			 		lval instanceof AccessExpr
				 && ((AccessExpr)lval).obj instanceof VarAccessExpr
				 &&	((VarAccessExpr)((AccessExpr)lval).obj).var.name.equals(nameThis)
				)
			) {
				if( PassInfo.method != null && PassInfo.method.isInvariantMethod() )
					Kiev.reportError(pos,"Side-effect in invariant condition");
				if( PassInfo.method != null && !PassInfo.method.isInvariantMethod() ) {
					if( lval instanceof FieldAccessExpr )
						PassInfo.method.addViolatedField( ((FieldAccessExpr)lval).var );
					else if( lval instanceof StaticFieldAccessExpr )
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
		case FieldAccessExpr:
			NodeInfoPass.setNodeValue(((FieldAccessExpr)lval).var,value);
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
				if( op != AssignOperator.Assign) {
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
				if( op != AssignOperator.Assign ) {
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
			if( op != AssignOperator.Assign ) Code.addInstr(op.instr);
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
			if( op != AssignOperator.Assign ) Code.addInstr(op.instr);
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
			if( op != AssignOperator.Assign ) Code.addInstr(op.instr);
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


public class InitializeExpr extends AssignExpr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

    public boolean	of_wrapper;

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
		if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !et1.clazz.isWrapper() && !et2.clazz.isWrapper()) {
			return (Expr)this.resolve(reqType);
		}
		else if((of_wrapper || op == AssignOperator.Assign2) && et1.clazz.isWrapper() && (et2 == Type.tpNull || et2.isInstanceOf(et1))) {
			return (Expr)this.resolve(reqType);
		}
		// Try wrapped classes
		if (op != AssignOperator.Assign2) {
			if (et2.clazz.isWrapper()) {
				Expr e = new InitializeExpr(pos,op,lval,new AccessExpr(value.pos,value,et2.clazz.wrapped_field),of_wrapper).tryResolve(reqType);
				if (e != null) return e;
			}
		}
		return null;
	}

}



public class BinaryExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public BinaryOperator		op;
	public Expr					expr1;
	public Expr					expr2;

	public BinaryExpr(int pos, BinaryOperator op, Expr expr1, Expr expr2) {
		super(pos);
		this.op = op;
		this.expr1 = expr1;
		this.expr1.parent = this;
		this.expr2 = expr2;
		this.expr2.parent = this;
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
			    (et1.clazz.isWrapper() && Type.getRealType(et1,et1.clazz.wrapped_field.type) == Type.tpString) ||
			    (et2.clazz.isWrapper() && Type.getRealType(et2,et2.clazz.wrapped_field.type) == Type.tpString)
			   )
		) {
			if( expr1 instanceof StringConcatExpr ) {
				StringConcatExpr sce = (StringConcatExpr)expr1;
				Expr e = (Expr)expr2;
				if (et2.clazz.isWrapper()) e = new AccessExpr(e.pos,e,et2.clazz.wrapped_field);
				sce.appendArg((Expr)e.resolve(null));
				trace(Kiev.debugStatGen,"Adding "+e+" to StringConcatExpr, now ="+sce);
				return (Expr)sce.resolve(Type.tpString);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				Expr e1 = (Expr)expr1;
				if (et1.clazz.isWrapper()) e1 = new AccessExpr(e1.pos,e1,et1.clazz.wrapped_field);
				sce.appendArg((Expr)e1.resolve(null));
				Expr e2 = (Expr)expr2;
				if (et2.clazz.isWrapper()) e2 = new AccessExpr(e2.pos,e2,et2.clazz.wrapped_field);
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
		if (et1.clazz.isWrapper()) {
			Expr e = new BinaryExpr(pos,op,new AccessExpr(expr1.pos,expr1,et1.clazz.wrapped_field),expr2).tryResolve(reqType);
			if (e != null) return e;
		}
		if (et2.clazz.isWrapper()) {
			Expr e = new BinaryExpr(pos,op,expr1,new AccessExpr(expr2.pos,expr2,et2.clazz.wrapped_field)).tryResolve(reqType);
			if (e != null) return e;
		}
		if (et1.clazz.isWrapper() && et2.clazz.isWrapper()) {
			Expr e = new BinaryExpr(pos,op,new AccessExpr(expr1.pos,expr1,et1.clazz.wrapped_field),new AccessExpr(expr2.pos,expr2,et2.clazz.wrapped_field)).tryResolve(reqType);
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
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() | val2.longValue())).resolve(null);
					else if( val1 instanceof Integer || val2 instanceof Integer )
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() | val2.intValue())).resolve(null);
					else if( val1 instanceof Short || val2 instanceof Short )
						return new ConstExpr(pos,Kiev.newShort(val1.shortValue() | val2.shortValue())).resolve(null);
					else if( val1 instanceof Byte || val2 instanceof Byte )
						return new ConstExpr(pos,Kiev.newByte(val1.byteValue() | val2.byteValue())).resolve(null);
				}
				else if( op == BinaryOperator.BitXor ) {
					if( val1 instanceof Long || val2 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() ^ val2.longValue())).resolve(null);
					else if( val1 instanceof Integer || val2 instanceof Integer )
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() ^ val2.intValue())).resolve(null);
					else if( val1 instanceof Short || val2 instanceof Short )
						return new ConstExpr(pos,Kiev.newShort(val1.shortValue() ^ val2.shortValue())).resolve(null);
					else if( val1 instanceof Byte || val2 instanceof Byte )
						return new ConstExpr(pos,Kiev.newByte(val1.byteValue() ^ val2.byteValue())).resolve(null);
				}
				else if( op == BinaryOperator.BitAnd ) {
					if( val1 instanceof Long || val2 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() & val2.longValue())).resolve(null);
					else if( val1 instanceof Integer || val2 instanceof Integer )
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() & val2.intValue())).resolve(null);
					else if( val1 instanceof Short || val2 instanceof Short )
						return new ConstExpr(pos,Kiev.newShort(val1.shortValue() & val2.shortValue())).resolve(null);
					else if( val1 instanceof Byte || val2 instanceof Byte )
						return new ConstExpr(pos,Kiev.newByte(val1.byteValue() & val2.byteValue())).resolve(null);
				}
				else if( op == BinaryOperator.LeftShift ) {
					if( val1 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() << val2.intValue())).resolve(null);
					else
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() << val2.intValue())).resolve(null);
				}
				else if( op == BinaryOperator.RightShift ) {
					if( val1 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() >> val2.intValue())).resolve(null);
					else if( val1 instanceof Integer )
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() >> val2.intValue())).resolve(null);
					else if( val1 instanceof Short )
						return new ConstExpr(pos,Kiev.newShort(val1.shortValue() >> val2.intValue())).resolve(null);
					else if( val1 instanceof Byte )
						return new ConstExpr(pos,Kiev.newByte(val1.byteValue() >> val2.intValue())).resolve(null);
				}
				else if( op == BinaryOperator.UnsignedRightShift ) {
					if( val1 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() >>> val2.intValue())).resolve(null);
					else if( val1 instanceof Integer )
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() >>> val2.intValue())).resolve(null);
					else if( val1 instanceof Short )
						return new ConstExpr(pos,Kiev.newShort(val1.shortValue() >>> val2.intValue())).resolve(null);
					else if( val1 instanceof Byte )
						return new ConstExpr(pos,Kiev.newByte(val1.byteValue() >>> val2.intValue())).resolve(null);
				}
				else if( op == BinaryOperator.Add ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstExpr(pos,Kiev.newDouble(val1.doubleValue() + val2.doubleValue())).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstExpr(pos,Kiev.newFloat(val1.floatValue() + val2.floatValue())).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() + val2.longValue())).resolve(null);
					else
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() + val2.intValue())).resolve(null);
				}
				else if( op == BinaryOperator.Sub ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstExpr(pos,Kiev.newDouble(val1.doubleValue() - val2.doubleValue())).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstExpr(pos,Kiev.newFloat(val1.floatValue() - val2.floatValue())).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() - val2.longValue())).resolve(null);
					else
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() - val2.intValue())).resolve(null);
				}
				else if( op == BinaryOperator.Mul ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstExpr(pos,Kiev.newDouble(val1.doubleValue() * val2.doubleValue())).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstExpr(pos,Kiev.newFloat(val1.floatValue() * val2.floatValue())).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() * val2.longValue())).resolve(null);
					else
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() * val2.intValue())).resolve(null);
				}
				else if( op == BinaryOperator.Div ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstExpr(pos,Kiev.newDouble(val1.doubleValue() / val2.doubleValue())).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstExpr(pos,Kiev.newFloat(val1.floatValue() / val2.floatValue())).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() / val2.longValue())).resolve(null);
					else
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() / val2.intValue())).resolve(null);
				}
				else if( op == BinaryOperator.Mod ) {
					if( val1 instanceof Double || val2 instanceof Double )
						return new ConstExpr(pos,Kiev.newDouble(val1.doubleValue() % val2.doubleValue())).resolve(null);
					else if( val1 instanceof Float || val2 instanceof Float )
						return new ConstExpr(pos,Kiev.newFloat(val1.floatValue() % val2.floatValue())).resolve(null);
					else if( val1 instanceof Long || val2 instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(val1.longValue() % val2.longValue())).resolve(null);
					else
						return new ConstExpr(pos,Kiev.newInteger(val1.intValue() % val2.intValue())).resolve(null);
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

public class StringConcatExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Expr[]	args		= new Expr[0];

	public static Struct clazzStringBuffer;
	public static Method clazzStringBufferToString;
	public static Method clazzStringBufferInit;

	static {
		try {
		clazzStringBuffer = Env.getStruct(ClazzName.fromToplevelName(KString.from("java.lang.StringBuffer")) );
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
		expr.parent = this;
		args = (Expr[])Arrays.append(args,expr);
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

public class CommaExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Expr[]		exprs;

	public CommaExpr(int pos, Expr[] exprs) {
		super(pos);
		this.exprs = exprs;
		foreach(Expr e; exprs; e!=null) e.parent = this;
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
		foreach(ASTNode n; exprs; exprs!=null) n.cleanup();
		exprs = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			for(int i=0; i < exprs.length; i++) {
				if( i < exprs.length-1)
					exprs[i] = exprs[i].resolveExpr(Type.tpVoid);
				else
					exprs[i] = exprs[i].resolveExpr(reqType);
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

public class UnaryExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Operator				op;
	public Expr					expr;

	public UnaryExpr(int pos, Operator op, Expr expr) {
		super(pos);
		this.op = op;
		this.expr = expr;
		this.expr.parent = this;
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
			if( !(expr instanceof BooleanExpr) )
				expr = (Expr)new BooleanWrapperExpr(expr.pos,expr).tryResolve(Type.tpBoolean);
			return (Expr)new BooleanNotExpr(pos,(BooleanExpr)expr).tryResolve(Type.tpBoolean);
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
		if (et.clazz.isWrapper()) {
			Expr e = new UnaryExpr(pos,op,new AccessExpr(expr.pos,expr,et.clazz.wrapped_field)).tryResolve(reqType);
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
				if( !(expr instanceof BooleanExpr) )
					expr = (Expr)new BooleanWrapperExpr(expr.pos,expr).resolve(Type.tpBoolean);
				return new BooleanNotExpr(pos,(BooleanExpr)expr).resolve(reqType);
			}
			// Check if expression is constant
			if( expr.isConstantExpr() ) {
				Number val = (Number)expr.getConstValue();
				if( op == PrefixOperator.Pos ) {
					if( val instanceof Double )
						return new ConstExpr(pos,val).resolve(null);
					else if( val instanceof Float )
						return new ConstExpr(pos,val).resolve(null);
					else if( val instanceof Long )
						return new ConstExpr(pos,val).resolve(null);
					else if( val instanceof Integer )
						return new ConstExpr(pos,val).resolve(null);
					else if( val instanceof Short )
						return new ConstExpr(pos,val).resolve(null);
					else if( val instanceof Byte )
						return new ConstExpr(pos,val).resolve(null);
				}
				else if( op == PrefixOperator.Neg ) {
					if( val instanceof Double )
						return new ConstExpr(pos,Kiev.newDouble(-val.doubleValue())).resolve(null);
					else if( val instanceof Float )
						return new ConstExpr(pos,Kiev.newFloat(-val.floatValue())).resolve(null);
					else if( val instanceof Long )
						return new ConstExpr(pos,Kiev.newLong(-val.longValue())).resolve(null);
					else if( val instanceof Integer )
						return new ConstExpr(pos,Kiev.newInteger(-val.intValue())).resolve(null);
					else if( val instanceof Short )
						return new ConstExpr(pos,Kiev.newShort(-val.shortValue())).resolve(null);
					else if( val instanceof Byte )
						return new ConstExpr(pos,Kiev.newByte(-val.byteValue())).resolve(null);
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

public class IncrementExpr extends LvalueExpr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Operator				op;
	public Expr					lval;

	public IncrementExpr(int pos, Operator op, Expr lval) {
		super(pos);
		this.op = op;
		this.lval = lval;
		this.lval.parent = this;
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
				var.setNeedRefProxy(true);
				Field vf = (Field)PassInfo.clazz.resolveName(var.name.name);
				vf.type = Type.getProxyType(var.type);
			}
		}
		if( Kiev.kaffe &&
			(lval instanceof VarAccessExpr) &&
			((VarAccessExpr)lval).var.isClosureProxy() &&
			!((VarAccessExpr)lval).var.isNeedRefProxy()
		) {
			// Check that we in local closure, thus var need RefProxy
			Var var = ((VarAccessExpr)lval).var;
			ASTNode p = var.parent;
			while( !(p instanceof Method) ) p = p.parent;
			if( p == PassInfo.method && p.isLocalMethod() ) {
				var.setNeedRefProxy(true);
				// Change type of closure method
				Type[] types = (Type[])PassInfo.method.type.args.clone();
				int i=0;
				for(; i < PassInfo.method.params.length; i++)
					if(PassInfo.method.params[i].name.equals(var.name))
						break;
				Debug.assert(i < PassInfo.method.params.length,"Can't find method parameter "+var);
				if( !PassInfo.method.isStatic() )
					i--;
				types[i] = Type.getProxyType(var.type);
				PassInfo.method.type = MethodType.newMethodType(
					null,types,PassInfo.method.type.ret);
				// Need to resolve initial var and mark it as RefProxy
				KString name = var.name.name;
				PVar<ASTNode> v = new PVar<ASTNode>();
				PVar<List<ASTNode>> path = new PVar<List<ASTNode>>(List.Nil);
				if( !PassInfo.resolveNameR(v,path,name,null,0) ) {
					Kiev.reportError(pos,"Internal error: can't find var "+name);
				}
				Var pv = (Var)v.$var;
				pv.setNeedRefProxy(true);
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

public class MultiExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public MultiOperator	op;
	public List<ASTNode>	exprs;

	public MultiExpr(int pos, MultiOperator op, List<ASTNode> exprs) {
		super(pos);
		this.op = op;
		this.exprs = exprs;
		foreach(Expr e; exprs; e!=null) e.parent = this;
	}

	public void cleanup() {
		parent=null;
		foreach(ASTNode n; exprs; n!=null) n.cleanup();
		exprs = null;
	}

	public Expr tryResolve(Type reqType) {
		if( op == MultiOperator.Conditional ) {
			Expr cond = ((Expr)exprs.head()).tryResolve(Type.tpBoolean);
			if( cond == null )
				return null;
			Expr expr1 = ((Expr)exprs.at(1)).tryResolve(reqType);
			if( expr1 == null )
				return null;
			Expr expr2 = ((Expr)exprs.at(2)).tryResolve(reqType);
			if( expr2 == null )
				return null;
			return (Expr)new ConditionalExpr(pos,cond,expr1,expr2).resolve(reqType);
		}
		throw new CompilerException(pos,"Multi-operators are not implemented");
	}
}


public class ConditionalExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Expr		cond;
	public Expr		expr1;
	public Expr		expr2;

	public ConditionalExpr(int pos, Expr cond, Expr expr1, Expr expr2) {
		super(pos);
		this.cond = cond;
		this.cond.parent = this;
		this.expr1 = expr1;
		this.expr1.parent = this;
		this.expr2 = expr2;
		this.expr2.parent = this;
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
			if( !(cond instanceof BooleanExpr) )
				cond = (Expr)new BooleanWrapperExpr(cond.pos,cond).resolve(Type.tpBoolean);
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
				((BooleanExpr)cond).generate_iffalse(elseLabel);
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

public class CastExpr extends Expr {

	import kiev.stdlib.Debug;
	import kiev.vlang.Instr;

	public Type					type;
	public Expr					expr;
	public boolean				explicit = false;
	public boolean				reinterp = false;

	public CastExpr(int pos, Type type, Expr expr) {
		super(pos);
		this.type = type;
		this.expr = expr;
		this.expr.parent = this;
	}

	public CastExpr(int pos, Type type, Expr expr, boolean expl) {
		super(pos);
		this.type = type;
		this.expr = expr;
		this.expr.parent = this;
		explicit = expl;
	}

	public CastExpr(int pos, Type type, Expr expr, boolean expl, boolean reint) {
		super(pos);
		this.type = type;
		this.expr = expr;
		this.expr.parent = this;
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
		expr.parent = this;
		Expr ex = (Expr)expr.tryResolve(type);
		if( ex == null ) return null;
		expr = ex;
		Type extp = Type.getRealType(type,expr.getType());
		if( type == Type.tpBoolean && extp == Type.tpRule )	return ex;
		// Try to find $cast method
		if( !extp.isAutoCastableTo(type) ) {
			Expr ocast = tryOverloadedCast(extp);
			if( ocast == this ) return (Expr)resolve(reqType);
		}
		else if (extp.clazz.isWrapper() && Type.getRealType(extp,extp.clazz.wrapped_field.type).isAutoCastableTo(type)) {
			Expr ocast = tryOverloadedCast(extp);
			if( ocast == this ) return (Expr)resolve(reqType);
			return new CastExpr(pos,type,new AccessExpr(expr.pos,expr,extp.clazz.wrapped_field),explicit,reinterp).tryResolve(reqType);
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
		throw new CompilerException(pos,"Expression "+ex+" of type "+extp+" is not castable to "+type);
	}

	public Expr tryOverloadedCast(Type et) {
		PVar<ASTNode> v = new PVar<ASTNode>();
		PVar<List<ASTNode>> path = new PVar<List<ASTNode>>(List.Nil);
		Struct cl = et.clazz;
		v.$var = null;
		path.$var = List.Nil;
		if( PassInfo.resolveBestMethodR(cl,v,path,nameCastOp,Expr.emptyArray,this.type,et,0) ) {
			Expr ce;
			if( path.$var  == List.Nil )
				ce = new CallAccessExpr(pos,parent,expr,(Method)v.$var,Expr.emptyArray);
			else {
				ce = new CallAccessExpr(pos,parent,Method.getAccessExpr(path.$var,expr),(Method)v.$var,Expr.emptyArray);
			}
			expr = ce;
			return this;
		}
		v.$var = null;
		path.$var = List.Nil;
		if( PassInfo.resolveMethodR(v,path,nameCastOp,new Expr[]{expr},this.type,et,ResolveFlags.Static) ) {
			assert(v.$var.isStatic());
			Expr ce = (Expr)new CallAccessExpr(pos,parent,expr,(Method)v.$var,new Expr[]{expr}).resolve(type);
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
			if( e instanceof Struct )
				expr = Expr.toExpr((Struct)e,reqType,pos,parent);
			else
				expr = (Expr)e;
			Type et = Type.getRealType(type,expr.getType());
			// Try wrapped field
			if (et.clazz.isWrapper() && et.clazz.wrapped_field.type.equals(type)) {
				return new AccessExpr(pos,parent,expr,et.clazz.wrapped_field).resolve(reqType);
			}
			// try null to something...
			if (et == Type.tpNull && reqType.isReference())
				return expr;
			if( et.clazz.equals(Type.tpPrologVar.clazz) && type.equals(et.args[0]) ) {
				Field varf = (Field)et.clazz.resolveName(KString.from("$var"));
				return new AccessExpr(pos,parent,expr,varf).resolve(reqType);
			}
			if( type.clazz.equals(Type.tpPrologVar.clazz) && et.equals(type.args[0]) )
				return new NewExpr(pos,
						Type.newRefType(Type.tpPrologVar.clazz,new Type[]{et}),
						new Expr[]{expr})
					.resolve(reqType);
			if( type == Type.tpBoolean && et == Type.tpRule )
				return new BinaryBooleanExpr(pos,
					BinaryOperator.NotEquals,
					expr,
					new ConstExpr(expr.pos,null)).resolve(type);
			if( type.isBoolean() && et.isBoolean() ) return expr;
			if( !Kiev.javaMode && type.isInstanceOf(Type.tpEnum) && et.isIntegerInCode() ) {
				if (type.isIntegerInCode())
					return this;
				Method cm = null;
				cm = type.clazz.resolveMethod(nameCastOp,KString.from("(I)"+type.signature));
				return new CallExpr(pos,parent,cm,new Expr[]{expr}).resolve(reqType);
			}
			if( !Kiev.javaMode && type.isIntegerInCode() && et.isInstanceOf(Type.tpEnum) ) {
				if (et.isIntegerInCode())
					return this;
				Field cf = (Field)Type.tpEnum.clazz.resolveName(nameEnumVal);
				return new AccessExpr(pos,parent,expr,cf).resolve(reqType);
			}
			// Try to find $cast method
			if( !et.isAutoCastableTo(type) ) {
				Expr ocast = tryOverloadedCast(et);
				if( ocast != null && ocast != this ) return ocast;
			}

			if( et.isReference() != type.isReference() && !(expr instanceof ClosureCallExpr) )
				if( !et.isReference() && type.clazz.isArgument() )
					Kiev.reportWarning(pos,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
				else if (!et.clazz.isEnum())
					throw new CompilerException(pos,"Expression "+expr+" of type "+et+" cannot be casted to type "+type);
			if( /*!explicit &&*/ !et.isCastableTo((Type)type) ) {
				throw new RuntimeException("Expression "+expr+" cannot be casted to type "+type);
			}
			if( Kiev.verify && expr.getType() != et ) {
				setNodeCastType();
				return this;
			}
			if( /*!explicit &&*/ et.isReference() && et.isInstanceOf((Type)type) ) return expr;
			if( et.isReference() && type.isReference() && et.clazz.package_clazz.isClazz()
			 && !et.clazz.isArgument()
			 && !et.clazz.isStatic() && et.clazz.package_clazz.type.isAutoCastableTo(type)
			) {
				return new CastExpr(pos,type,
					new AccessExpr(pos,expr,OuterThisAccessExpr.outerOf(et.clazz)),explicit
				).resolve(reqType);
			}
			if( expr.isConstantExpr() ) {
				Object val = expr.getConstValue();
				if( val instanceof Number ) {
					Number num = (Number)val;
					if( type == Type.tpDouble ) return new ConstExpr(pos,Kiev.newDouble(num.doubleValue())).resolve(null);
					else if( type == Type.tpFloat ) return new ConstExpr(pos,Kiev.newFloat(num.floatValue())).resolve(null);
					else if( type == Type.tpLong ) return new ConstExpr(pos,Kiev.newLong(num.longValue())).resolve(null);
					else if( type == Type.tpInt ) return new ConstExpr(pos,Kiev.newInteger(num.intValue())).resolve(null);
					else if( type == Type.tpShort ) return new ConstExpr(pos,Kiev.newShort(num.intValue())).resolve(null);
					else if( type == Type.tpByte ) return new ConstExpr(pos,Kiev.newByte(num.intValue())).resolve(null);
					else if( type == Type.tpChar ) return new ConstExpr(pos,Kiev.newCharacter(num.intValue())).resolve(null);
				}
				else if( val instanceof Character ) {
					char num = ((Character)val).charValue();
					if( type == Type.tpDouble ) return new ConstExpr(pos,Kiev.newDouble((double)(int)num)).resolve(null);
					else if( type == Type.tpFloat ) return new ConstExpr(pos,Kiev.newFloat((float)(int)num)).resolve(null);
					else if( type == Type.tpLong ) return new ConstExpr(pos,Kiev.newLong((long)(int)num)).resolve(null);
					else if( type == Type.tpInt ) return new ConstExpr(pos,Kiev.newInteger((int)num)).resolve(null);
					else if( type == Type.tpShort ) return new ConstExpr(pos,Kiev.newShort((short)num)).resolve(null);
					else if( type == Type.tpByte ) return new ConstExpr(pos,Kiev.newByte((byte)(int)num)).resolve(null);
					else if( type == Type.tpChar ) return new ConstExpr(pos,Kiev.newCharacter(num)).resolve(null);
				}
			}
			if( et.equals(type) ) return expr;
			if( expr instanceof ClosureCallExpr && et instanceof MethodType ) {
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
		switch(expr) {
		case VarAccessExpr:			n = ((VarAccessExpr)expr).var;	break;
		case FieldAccessExpr:		n = ((FieldAccessExpr)expr).var;	break;
		case StaticFieldAccessExpr:	n = ((StaticFieldAccessExpr)expr).var;	break;
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
				if (t.clazz.isEnum() && t.clazz.isPrimitiveEnum() && !type.isReference()) {
					if (t.clazz.getPrimitiveEnumType() != type)
						Code.addInstr(Instr.op_x2y,type);
				} else {
					if( t.isReference() != type.isReference() )
						throw new CompilerException(pos,"Expression "+expr+" of type "+t+" cannot be casted to type "+type);
					if( Type.getRealType(Kiev.argtype,type).isReference() )
						Code.addInstr(Instr.op_checkcast,Type.getRealType(Kiev.argtype,type));
				}
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

