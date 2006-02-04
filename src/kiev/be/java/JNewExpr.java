package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.NewExpr.NewExprImpl;
import kiev.vlang.NewArrayExpr.NewArrayExprImpl;
import kiev.vlang.NewInitializedArrayExpr.NewInitializedArrayExprImpl;
import kiev.vlang.NewClosure.NewClosureImpl;

@nodeview
public final view JNewExprView of NewExprImpl extends JENodeView {
	public access:ro	Type			type;
	public access:ro	JENodeView[]	args;
	public access:ro	JENodeView		outer;
	public				JENodeView		temp_expr;
	public access:ro	JMethodView		func;
	
	@getter public final JENodeView		get$outer()				{ return ((NewExprImpl)this.$view).outer==null? null : ((NewExprImpl)this.$view).outer.getJENodeView(); }
	@getter public final JENodeView[]	get$args()				{ return (JENodeView[])((NewExprImpl)this.$view).args.toJViewArray(JENodeView.class); }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewExpr: "+this);
		Type type = this.type;
		JENodeView[] args = this.args;
		code.setLinePos(this);
		while( type.isArgument() && !type.isUnerasable())
			type = type.getErasedType();
		if( type.isArgument() ) {
			if( outer != null || args.length > 0 ) {
				Kiev.reportError(this,"Constructor with arguments for type argument is not supported");
				return;
			} else {
				// If we have primitive type
				if( !type.isReference() ) {
					new ConstNullExpr().getJConstNullExprView().generate(code,type);
					return;
				}
				int i;
				for(i=0; i < code.clazz.args.length; i++)
					if (type ≈ code.clazz.args[i]) break;
				if( i >= code.clazz.args.length )
					throw new CompilerException(this,"Can't create an instance of argument type "+type);
				ENode tie = new IFldExpr(pos,new ThisExpr(pos),code.clazz.getStruct().resolveField(nameTypeInfo));
				ENode e = new CastExpr(pos,type,
					new CallExpr(pos,tie,
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("newInstance"),Type.tpObject,Type.tpInt),
						new ENode[]{new ConstIntExpr(i)}),
					true);
				e.resolve(reqType);
				e.getJENodeView().generate(code,reqType);
				return;
			}
		}
		code.addInstr(op_new,type);
		// First arg ('this' pointer) is generated by 'op_dup'
		if (reqType ≢ Type.tpVoid)
			code.addInstr(op_dup);
		// Constructor call args (first args 'this' skipped)
		if( outer != null )
			outer.generate(code,null);
		if (func.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null) {
			// Create static field for this type typeinfo
			temp_expr = jctx_clazz.accessTypeInfoField(this,type,true);
			temp_expr.generate(code,null);
			temp_expr = null;
		}
		for(int i=0; i < args.length; i++)
			args[i].generate(code,null);
		if( type.isLocalClazz() ) {
			JStructView cl = ((CompaundType)type).clazz.getJStructView();
			foreach (JDNodeView n; cl.members; n instanceof JFieldView) {
				JFieldView f = (JFieldView)n;
				if( !f.isNeedProxy() ) continue;
				JVarView v = ((JLVarExprView)f.init).var;
				code.addInstr(Instr.op_load,v);
			}
		}
		code.addInstr(op_call,func,false,type);
	}
}


@nodeview
public final view JNewArrayExprView of NewArrayExprImpl extends JENodeView {
	public access:ro	Type			type;
	public access:ro	JENodeView[]	args;
	public access:ro	int				dim;
	public access:ro	Type			arrtype;
	
	@getter public final JENodeView[]	get$args()				{ return (JENodeView[])((NewArrayExprImpl)this.$view).args.toJViewArray(JENodeView.class); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewArrayExpr: "+this);
		Type type = this.type;
		JENodeView[] args = this.args;
		code.setLinePos(this);
		if( dim == 1 ) {
			args[0].generate(code,null);
			code.addInstr(Instr.op_newarray,type);
		} else {
			for(int i=0; i < args.length; i++)
				args[i].generate(code,null);
			code.addInstr(Instr.op_multianewarray,arrtype,args.length);
		}
		if( reqType ≡ Type.tpVoid ) code.addInstr(Instr.op_pop);
	}

}

@nodeview
public final view JNewInitializedArrayExprView of NewInitializedArrayExprImpl extends JENodeView {
	public access:ro	Type			type;
	public access:ro	JENodeView[]	args;
	public access:ro	int				dim;
	public access:ro	int[]			dims;
	public access:ro	Type			arrtype;

	@getter public final int			get$dim()				{ return ((NewInitializedArrayExprImpl)this.$view).dims.length; }
	@getter public final JENodeView[]	get$args()				{ return (JENodeView[])((NewInitializedArrayExprImpl)this.$view).args.toJViewArray(JENodeView.class); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewInitializedArrayExpr: "+this);
		Type type = this.type;
		JENodeView[] args = this.args;
		code.setLinePos(this);
		if( dim == 1 ) {
			code.addConst(args.length);
			code.addInstr(Instr.op_newarray,type);
		} else {
			for(int i=0; i < dim; i++)
				code.addConst(dims[i]);
			code.addInstr(Instr.op_multianewarray,arrtype,dim);
		}
		for(int i=0; i < args.length; i++) {
			code.addInstr(Instr.op_dup);
			code.addConst(i);
			args[i].generate(code,null);
			code.addInstr(Instr.op_arr_store);
		}
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}
}

@nodeview
public final view JNewClosureView of NewClosureImpl extends JENodeView {
	public access:ro	CallType		type;
	public access:ro	JStructView		clazz;
	public access:ro	JMethodView		func;

	@getter public final CallType		get$type()				{ return (CallType)((NewClosureImpl)this.$view).type.getType(); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewClosure: "+this);
		CallType ctype = this.type;
		JStructView cl = clazz;
		code.setLinePos(this);
		code.addInstr(op_new,clazz.ctype);
		// First arg ('this' pointer) is generated by 'op_dup'
		if( reqType ≢ Type.tpVoid )
			code.addInstr(op_dup);
		// Constructor call args (first args 'this' skipped)
		if( code.method!=null && !code.method.isStatic() )
			code.addInstrLoadThis();
		code.addConst(ctype.arity);
		// Now, fill proxyed fields (vars)
		foreach (JDNodeView n; cl.members; n instanceof JFieldView) {
			JFieldView f = (JFieldView)n;
			if( !f.isNeedProxy() ) continue;
			JVarView v = ((JLVarExprView)f.init).var;
			code.addInstr(Instr.op_load,v);
		}
		code.addInstr(op_call,func,false);
		//code.stack_pop();
		//code.stack_push(ctype);
	}
}

