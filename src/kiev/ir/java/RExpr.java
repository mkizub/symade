package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.Shadow.ShadowView;
import kiev.vlang.ArrayLengthExpr.ArrayLengthExprView;
import kiev.vlang.TypeClassExpr.TypeClassExprView;
import kiev.vlang.TypeInfoExpr.TypeInfoExprView;
import kiev.vlang.AssignExpr.AssignExprView;
import kiev.vlang.BinaryExpr.BinaryExprView;
import kiev.vlang.StringConcatExpr.StringConcatExprView;
import kiev.vlang.CommaExpr.CommaExprView;
import kiev.vlang.Block.BlockView;
import kiev.vlang.UnaryExpr.UnaryExprView;
import kiev.vlang.IncrementExpr.IncrementExprView;
import kiev.vlang.ConditionalExpr.ConditionalExprView;
import kiev.vlang.CastExpr.CastExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RShadow of Shadow extends ShadowView {
	
	public void resolve(Type reqType) {
		if (node instanceof ENode)
			((ENode)node).resolve(reqType);
		else
			((Initializer)node).resolveDecl();
		setResolved(true);
	}
}

@nodeview
public final view RArrayLengthExpr of ArrayLengthExpr extends ArrayLengthExprView {

	public void resolve(Type reqType) {
		obj.resolve(null);
		if !(obj.getType().isArray())
			throw new CompilerException(this, "Access to array length for non-array type "+obj.getType());
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public final view RTypeClassExpr of TypeClassExpr extends TypeClassExprView {

	public void resolve(Type reqType) {
		Type tp = type.getType();
		if (!tp.isReference()) {
			Type rt = ((CoreType)tp).getRefTypeForPrimitive();
			Field f = rt.clazz.resolveField(KString.from("TYPE"));
			replaceWithNodeResolve(reqType,new SFldExpr(pos,f));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public final view RTypeInfoExpr of TypeInfoExpr extends TypeInfoExprView {

	public void resolve(Type reqType) {
		if (isResolved())
			return;
		Type type = this.type.getType();
		CompaundType ftype = Type.tpTypeInfo;
		Struct clazz = type.getStruct();
		if (clazz.isTypeUnerasable()) {
			if (clazz.typeinfo_clazz == null)
				((RStruct)clazz).autoGenerateTypeinfoClazz();
			ftype = clazz.typeinfo_clazz.ctype;
		}
		cl_expr = new TypeClassExpr(pos,new TypeRef(clazz.ctype));
		cl_expr.resolve(Type.tpClass);
		foreach (ArgType at; ((RStruct)clazz).getTypeInfoArgs())
			cl_args.add(((RStruct)ctx_clazz).accessTypeInfoField((TypeInfoExpr)this, type.resolve(at),false));
		foreach (ENode tie; cl_args)
			tie.resolve(null);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RAssignExpr of AssignExpr extends AssignExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) {
			if (isAutoReturnable())
				ReturnStat.autoReturn(reqType, this);
			return;
		}
		lval.resolve(reqType);
		Type et1 = lval.getType();
		if (op == AssignOperator.Assign && et1 instanceof CTimeType)
			value.resolve(et1.getUnboxedType());
		else if (op == AssignOperator.Assign2 && et1 instanceof CTimeType)
			value.resolve(et1.getEnclosedType());
		else
			value.resolve(et1);
		if (value instanceof TypeRef)
			((TypeRef)value).toExpr(et1);
		Type et2 = value.getType();
		if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !(et1 instanceof CTimeType) && !(et2 instanceof CTimeType)) {
			this.resolve2(reqType);
			return;
		}
		else if( op == AssignOperator.Assign2 && et1 instanceof CTimeType && et2.isInstanceOf(et1)) {
			this.resolve2(reqType);
			return;
		}
		else if( op == AssignOperator.AssignAdd && et1 ≈ Type.tpString ) {
			this.resolve2(reqType);
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
			this.resolve2(reqType);
			return;
		}
		else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
			(    op==AssignOperator.AssignLeftShift
			||   op==AssignOperator.AssignRightShift
			||   op==AssignOperator.AssignUnsignedRightShift
			)
		) {
			this.resolve2(reqType);
			return;
		}
		else if( ( et1.isInteger() && et2.isInteger() ) &&
			(    op==AssignOperator.AssignBitOr
			||   op==AssignOperator.AssignBitXor
			||   op==AssignOperator.AssignBitAnd
			)
		) {
			this.resolve2(reqType);
			return;
		}
		else if( ( et1.isBoolean() && et2.isBoolean() ) &&
			(    op==AssignOperator.AssignBitOr
			||   op==AssignOperator.AssignBitXor
			||   op==AssignOperator.AssignBitAnd
			)
		) {
			this.resolve2(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,lval,value};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				replaceWithNodeResolve(reqType, new CallExpr(pos,~lval,opt.method,new ENode[]{~value}));
				return;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (op != AssignOperator.Assign2) {
			if (et1 instanceof CTimeType && et2 instanceof CTimeType) {
				lval = et1.makeUnboxedExpr(lval);
				value = et2.makeUnboxedExpr(value);
				resolve(reqType);
				return;
			}
			else if (et1 instanceof CTimeType) {
				lval = et1.makeUnboxedExpr(lval);
				resolve(reqType);
				return;
			}
			else if (et2 instanceof CTimeType) {
				value = et2.makeUnboxedExpr(value);
				resolve(reqType);
				return;
			}
		}
		this.resolve2(reqType); //throw new CompilerException(pos,"Unresolved expression "+this);
	}

	public:no,no,no,rw final void resolve2(Type reqType) {
		lval.resolve(null);
		if( !(lval instanceof LvalueExpr) )
			throw new RuntimeException("Can't assign to "+lval+": lvalue requared");
		Type t1 = lval.getType();
		if (t1 instanceof CTimeType && (value.isForWrapper() || op == AssignOperator.Assign2))
			t1 = t1.getEnclosedType();
		if( op==AssignOperator.AssignAdd && t1 ≈ Type.tpString ) {
			op = AssignOperator.Assign;
			value = new BinaryExpr(pos,BinaryOperator.Add,new Shadow(lval),~value);
		}
		if (value instanceof TypeRef)
			((TypeRef)value).toExpr(t1);
		else if (value instanceof ENode)
			value.resolve(t1);
		else
			throw new CompilerException(value, "Can't opeerate on "+value);
		Type t2 = value.getType();
		if (t2 instanceof CTimeType && (value.isForWrapper() || op == AssignOperator.Assign2))
			t2 = t2.getEnclosedType();
		if( op==AssignOperator.AssignLeftShift || op==AssignOperator.AssignRightShift || op==AssignOperator.AssignUnsignedRightShift ) {
			if( !t2.isIntegerInCode() ) {
				value = new CastExpr(pos,Type.tpInt,~value);
				value.resolve(Type.tpInt);
			}
		}
		else if( !t2.isInstanceOf(t1) ) {
			if( t2.isCastableTo(t1) ) {
				value = new CastExpr(pos,t1,~value);
				value.resolve(t1);
			} else {
				throw new RuntimeException("Value of type "+t2+" can't be assigned to "+lval);
			}
		}
		getDFlow().out();

		// Set violation of the field
		if( lval instanceof SFldExpr
		 || (
				lval instanceof IFldExpr
			 && ((IFldExpr)lval).obj instanceof LVarExpr
			 &&	((LVarExpr)((IFldExpr)lval).obj).ident.equals(nameThis)
			)
		) {
			if( ctx_method != null && ctx_method.isInvariantMethod() )
				Kiev.reportError(this,"Side-effect in invariant condition");
			if( ctx_method != null && !ctx_method.isInvariantMethod() ) {
				if( lval instanceof SFldExpr )
					ctx_method.addViolatedField( ((SFldExpr)lval).var );
				else
					ctx_method.addViolatedField( ((IFldExpr)lval).var );
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RBinaryExpr of BinaryExpr extends BinaryExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		expr1.resolve(null);
		expr2.resolve(null);
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( op == BinaryOperator.Add
			&& ( et1 ≈ Type.tpString || et2 ≈ Type.tpString ||
				(et1 instanceof CTimeType && et1.getUnboxedType() ≈ Type.tpString) ||
				(et2 instanceof CTimeType && et2.getUnboxedType() ≈ Type.tpString)
			   )
		) {
			if( expr1 instanceof StringConcatExpr ) {
				StringConcatExpr sce = (StringConcatExpr)expr1;
				if (et2 instanceof CTimeType) expr2 = et2.makeUnboxedExpr(expr2);
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
				replaceWithNodeResolve(Type.tpString, ~sce);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				if (et1 instanceof CTimeType) expr1 = et1.makeUnboxedExpr(expr1);
				sce.appendArg(expr1);
				if (et2 instanceof CTimeType) expr2 = et2.makeUnboxedExpr(expr2);
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
			this.resolve2(null);
			return;
		}
		else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
			(    op==BinaryOperator.LeftShift
			||   op==BinaryOperator.RightShift
			||   op==BinaryOperator.UnsignedRightShift
			)
		) {
			this.resolve2(null);
			return;
		}
		else if( ( (et1.isInteger() && et2.isInteger()) || (et1.isBoolean() && et2.isBoolean()) ) &&
			(    op==BinaryOperator.BitOr
			||   op==BinaryOperator.BitXor
			||   op==BinaryOperator.BitAnd
			)
		) {
			this.resolve2(null);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				ENode e;
				if( opt.method.isStatic() )
					replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{~expr1,~expr2}));
				else
					replaceWithNodeResolve(reqType, new CallExpr(pos,~expr1,opt.method,new ENode[]{~expr2}));
				return;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (et1 instanceof CTimeType && et2 instanceof CTimeType) {
			expr1 = et1.makeUnboxedExpr(expr1);
			expr2 = et1.makeUnboxedExpr(expr2);
			resolve(reqType);
			return;
		}
		if (et1 instanceof CTimeType) {
			expr1 = et1.makeUnboxedExpr(expr1);
			resolve(reqType);
			return;
		}
		if (et2 instanceof CTimeType) {
			expr2 = et1.makeUnboxedExpr(expr2);
			resolve(reqType);
			return;
		}
		resolve2(reqType);
	}

	public:no,no,no,rw final void resolve2(Type reqType) {
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
				expr1 = new CastExpr(pos,rt,~expr1);
				expr1.resolve(null);
			}
			if( !rt.equals(t2) && t2.isCastableTo(rt) ) {
				expr2 = new CastExpr(pos,rt,~expr2);
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
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RStringConcatExpr of StringConcatExpr extends StringConcatExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		foreach (ENode e; args)
			e.resolve(null);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RCommaExpr of CommaExpr extends CommaExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1) {
				exprs[i].resolve(Type.tpVoid);
				exprs[i].setGenVoidExpr(true);
			} else {
				exprs[i].resolve(reqType);
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static view RBlock of Block extends BlockView {
	public void resolve(Type reqType) {
		RBlock.resolveStats(reqType, this, stats);
	}

	public static void resolveStats(Type reqType, ENodeView self, NArr<ENode> stats) {
		int sz = stats.length - 1;
		for (int i=0; i <= sz; i++) {
			ENode st = stats[i];
			try {
				if( (i == sz) && self.isAutoReturnable() )
					st.setAutoReturnable(true);
				if( self.isAbrupted() && (st instanceof LabeledStat) )
					self.setAbrupted(false);
				//if( self.isAbrupted() )
				//	; //Kiev.reportWarning(stats[i].pos,"Possible unreachable statement");
				if (i < sz || reqType == Type.tpVoid) {
					st.setGenVoidExpr(true);
					st.resolve(Type.tpVoid);
				} else {
					st.resolve(reqType);
				}
				st = stats[i];
				if( st.isAbrupted() && !self.isBreaked() ) self.setAbrupted(true);
				if( st.isMethodAbrupted() && !self.isBreaked() ) self.setMethodAbrupted(true);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
	}
}

@nodeview
public static view RUnaryExpr of UnaryExpr extends UnaryExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		expr.resolve(reqType);
		Type et = expr.getType();
		if( et.isNumber() &&
			(  op==PrefixOperator.PreIncr
			|| op==PrefixOperator.PreDecr
			|| op==PostfixOperator.PostIncr
			|| op==PostfixOperator.PostDecr
			)
		) {
			replaceWithNodeResolve(reqType, new IncrementExpr(pos,op,~expr));
			return;
		}
		if( et.isAutoCastableTo(Type.tpBoolean) &&
			(  op==PrefixOperator.PreIncr
			|| op==PrefixOperator.BooleanNot
			)
		) {
			replaceWithNodeResolve(Type.tpBoolean, new BooleanNotExpr(pos,~expr));
			return;
		}
		if( et.isNumber() &&
			(  op==PrefixOperator.Pos
			|| op==PrefixOperator.Neg
			)
		) {
			this.resolve2(reqType);
			return;
		}
		if( et.isInteger() && op==PrefixOperator.BitNot ) {
			this.resolve2(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			if (ctx_clazz != null && opt.method != null && opt.method.type.arity == 1) {
				if ( !ctx_clazz.ctype.isInstanceOf(opt.method.ctx_clazz.ctype) )
					continue;
			}
			Type[] tps = new Type[]{null,et};
			ASTNode[] argsarr = new ASTNode[]{null,expr};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				ENode e;
				if ( opt.method.isStatic() )
					replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{~expr}));
				else
					replaceWithNodeResolve(reqType, new CallExpr(pos,~expr,opt.method,ENode.emptyArray));
				return;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (et instanceof CTimeType) {
			replaceWithNodeResolve(reqType, new UnaryExpr(pos,op,et.makeUnboxedExpr(expr)));
			return;
		}
		resolve2(reqType);
	}

	public:no,no,no,rw final void resolve2(Type reqType) {
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
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}


@nodeview
public static final view RIncrementExpr of IncrementExpr extends IncrementExprView {
	public void resolve(Type reqType) {
		if( isResolved() ) return;
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RConditionalExpr of ConditionalExpr extends ConditionalExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		cond.resolve(Type.tpBoolean);
		expr1.resolve(reqType);
		expr2.resolve(reqType);

		if( expr1.getType() ≉ getType() ) {
			expr1 = new CastExpr(expr1.pos,getType(),~expr1);
			expr1.resolve(getType());
		}
		if( expr2.getType() ≉ getType() ) {
			expr2 = new CastExpr(expr2.pos,getType(),~expr2);
			expr2.resolve(getType());
		}
		setResolved(true);
	}
}

@nodeview
public static final view RCastExpr of CastExpr extends CastExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		Type type = this.type.getType();
		expr.resolve(type);
		if (expr instanceof TypeRef)
			((TypeRef)expr).toExpr(type);
		Type extp = Type.getRealType(type,expr.getType());
		if( type ≡ Type.tpBoolean && extp ≡ Type.tpRule ) {
			replaceWithNode(expr);
			return;
		}
		// Try to find $cast method
		if( !extp.isAutoCastableTo(type) ) {
			if( tryOverloadedCast(extp) )
				return;
			if (extp instanceof CTimeType) {
				expr = extp.makeUnboxedExpr(expr);
				resolve(reqType);
				return;
			}
		}
		else if (extp instanceof CTimeType && extp.getUnboxedType().isAutoCastableTo(type)) {
			if( tryOverloadedCast(extp) )
				return;
			expr = extp.makeUnboxedExpr(expr);
			resolve(reqType);
			return;
		}
		else if (!extp.isInstanceOf(type) && extp.getStruct() != null && extp.getStruct().isStructView() && extp.getStruct().view_of.getType().isAutoCastableTo(type)) {
			if( tryOverloadedCast(extp) )
				return;
			this.resolve2(type);
			return;
		}
		else {
			this.resolve2(type);
			return;
		}
		if( extp.isCastableTo(type) ) {
			this.resolve2(type);
			return;
		}
		throw new CompilerException(this,"Expression "+expr+" of type "+extp+" is not castable to "+type);
	}

	public:no,no,no,rw final boolean tryOverloadedCast(Type et) {
		Method@ v;
		ResInfo info = new ResInfo(this,ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
		v.$unbind();
		CallType mt = new CallType(Type.emptyArray,this.type.getType());
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			ENode call = info.buildCall((ASTNode)this,~expr,(Method)v,info.mt,ENode.emptyArray);
			if (this.type.getType().isReference())
				call.setCastCall(true);
			replaceWithNodeResolve(type.getType(),call);
			return true;
		}
		v.$unbind();
		info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports);
		mt = new CallType(new Type[]{expr.getType()},this.type.getType());
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			assert(v.isStatic());
			ENode call = new CallExpr(pos,null,(Method)v,new ENode[]{~expr});
			replaceWithNodeResolve(type.getType(),call);
			return true;
		}
		return false;
	}

	public:no,no,no,rw final void resolve2(Type reqType) {
		Type type = this.type.getType();
		expr.resolve(type);
//		if( e instanceof Struct )
//			expr = Expr.toExpr((Struct)e,reqType,pos,parent);
//		else
//			expr = (Expr)e;
		if (reqType ≡ Type.tpVoid) {
			setResolved(true);
		}
		Type et = Type.getRealType(type,expr.getType());
		// Try wrapped field
		if (et instanceof CTimeType && et.getUnboxedType().equals(type)) {
			expr = et.makeUnboxedExpr(expr);
			resolve(reqType);
			return;
		}
		// try null to something...
		if (et ≡ Type.tpNull && reqType.isReference())
			return;
		if( type ≡ Type.tpBoolean && et ≡ Type.tpRule ) {
			replaceWithNodeResolve(type, new BinaryBoolExpr(pos,BinaryOperator.NotEquals,expr,new ConstNullExpr()));
			return;
		}
		if( type.isBoolean() && et.isBoolean() )
			return;
		if( !Kiev.javaMode && type.isInstanceOf(Type.tpEnum) && et.isIntegerInCode() ) {
			if (type.isIntegerInCode())
				return;
			Method cm = ((CompaundType)type).clazz.resolveMethod(nameCastOp,type,Type.tpInt);
			replaceWithNodeResolve(reqType, new CallExpr(pos,null,cm,new ENode[]{~expr}));
			return;
		}
		if( !Kiev.javaMode && type.isIntegerInCode() && et.isInstanceOf(Type.tpEnum) ) {
			if (et.isIntegerInCode())
				return;
			Method cf = Type.tpEnum.clazz.resolveMethod(nameEnumOrdinal, Type.tpInt);
			replaceWithNodeResolve(reqType, new CallExpr(pos,~expr,cf,ENode.emptyArray));
			return;
		}
		// Try to find $cast method
		if( !et.isAutoCastableTo(type) && tryOverloadedCast(et))
			return;

		if( et.isReference() != type.isReference() && !(expr instanceof ClosureCallExpr) )
			if( !et.isReference() && type instanceof ArgType )
				Kiev.reportWarning(this,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
			else if (et.getStruct() == null || !et.getStruct().isEnum())
				throw new CompilerException(this,"Expression "+expr+" of type "+et+" cannot be casted to type "+type);
		if( !et.isCastableTo((Type)type) ) {
			throw new RuntimeException("Expression "+expr+" cannot be casted to type "+type);
		}
		if( Kiev.verify && expr.getType() ≉ et ) {
			setResolved(true);
			return;
		}
		if( et.isReference() && et.isInstanceOf((Type)type) ) {
			setResolved(true);
			return;
		}
		if( et.isReference() && type.isReference() && et.getStruct() != null
		 && et.getStruct().package_clazz.isClazz()
		 && !(et instanceof ArgType)
		 && !et.getStruct().isStatic() && et.getStruct().package_clazz.ctype.isAutoCastableTo(type)
		) {
			replaceWithNodeResolve(reqType,
				new CastExpr(pos,type,
					new IFldExpr(pos,~expr,OuterThisAccessExpr.outerOf((Struct)et.getStruct()))
				));
			return;
		}
		if( expr.isConstantExpr() ) {
			Object val = expr.getConstValue();
			Type t = type;
			if( val instanceof Number ) {
				Number num = (Number)val;
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num.doubleValue())); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num.floatValue())); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num.longValue())); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num.intValue())); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num.intValue())); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num.intValue())); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num.intValue())); return; }
			}
			else if( val instanceof Character ) {
				char num = ((Character)val).charValue();
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)(int)num)); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) (int)num)); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  (int)num)); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   (int)num)); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) (int)num)); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  (int)num)); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
			else if( val instanceof Boolean ) {
				int num = ((Boolean)val).booleanValue() ? 1 : 0;
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num)); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num)); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num)); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num)); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num)); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num)); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
		}
		if( !et.equals(type) && expr instanceof ClosureCallExpr && et instanceof CallType ) {
			if( et.isAutoCastableTo(type) ) {
				((ClosureCallExpr)expr).is_a_call = Boolean.TRUE;
				return;
			}
			else if( et.isCastableTo(type) ) {
				((ClosureCallExpr)expr).is_a_call = Boolean.TRUE;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

