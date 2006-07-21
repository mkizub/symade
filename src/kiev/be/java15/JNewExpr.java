package kiev.be.java15;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java15.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public final view JNewExpr of NewExpr extends JENode {

	static final AttrSlot ATTR = new TmpAttrSlot("jnew temp expr",true,false,TypeInfo.newTypeInfo(ENode.class,null));	

	public:ro	JMethod			func;
	public:ro	JENode[]		args;
	public:ro	JENode			outer;
	abstract
	public 		JENode			tmp_expr;
	
	@getter public final JENode get$tmp_expr() {
		return (JENode)(ENode)ATTR.get((ENode)this);
	}
	@setter public final void set$tmp_expr(JENode e) {
		if (e != null)
			ATTR.set((ENode)this, (ENode)e);
		else
			ATTR.clear((ENode)this);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewExpr: "+this);
		Type type = this.getType();
		JENode[] args = this.args;
		code.setLinePos(this);
		while( type instanceof ArgType && !type.isUnerasable())
			type = type.getErasedType();
		if( type instanceof ArgType ) {
			if( outer != null || args.length > 0 ) {
				Kiev.reportError(this,"Constructor with arguments for type argument is not supported");
				return;
			} else {
				// If we have primitive type
				if( !type.isReference() ) {
					((JENode)new ConstNullExpr()).generate(code,type);
					return;
				}
				int i;
				for(i=0; i < ((Struct)code.clazz).args.length; i++)
					if (type ≈ ((Struct)code.clazz).args[i]) break;
				if( i >= ((Struct)code.clazz).args.length )
					throw new CompilerException(this,"Can't create an instance of argument type "+type);
				ENode tie = new IFldExpr(pos,new ThisExpr(pos),((Struct)code.clazz).resolveField(nameTypeInfo));
				ENode e = new CastExpr(pos,type,
					new CallExpr(pos,tie,
						Type.tpTypeInfo.clazz.resolveMethod("newInstance",Type.tpObject,Type.tpInt),
						new ENode[]{new ConstIntExpr(i)}
					)
				);
				e.resolve(reqType);
				((JENode)e).generate(code,reqType);
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
			tmp_expr = ((JStruct)jctx_tdecl).accessTypeInfoField(this,type,true);
			tmp_expr.generate(code,null);
			tmp_expr = null;
		}
		for(int i=0; i < args.length; i++)
			args[i].generate(code,null);
		if( type.getStruct() != null && type.getStruct().isLocal() ) {
			JStruct cl = (JStruct)((CompaundType)type).clazz;
			foreach (JField f; cl.members) {
				if( !f.isNeedProxy() ) continue;
				JVar v = ((JLVarExpr)f.init).var;
				code.addInstr(Instr.op_load,v);
			}
		}
		code.addInstr(op_call,func,false,type);
	}
}


@nodeview
public final view JNewArrayExpr of NewArrayExpr extends JENode {
	public:ro	Type				type;
	public:ro	JENode[]			args;
	public:ro	Type				arrtype;
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewArrayExpr: "+this);
		Type type = this.type;
		JENode[] args = this.args;
		code.setLinePos(this);
		if( args.length == 1 ) {
			args[0].generate(code,null);
			code.addInstr(Instr.op_newarray,type);
		} else {
			int n = 0;
			for(int i=0; i < args.length; i++) {
				JENode arg = args[i];
				if !(((ENode)arg) instanceof NopExpr) {
					arg.generate(code,null);
					n++;
				}
			}
			code.addInstr(Instr.op_multianewarray,arrtype,n);
		}
		if( reqType ≡ Type.tpVoid ) code.addInstr(Instr.op_pop);
	}

}

@nodeview
public final view JNewInitializedArrayExpr of NewInitializedArrayExpr extends JENode {
	public:ro	Type				type;
	public:ro	JENode[]			args;
	public:ro	int					dim;
	public:ro	int[]				dims;
	public:ro	Type				arrtype;

	@getter public final int	get$dim()	{ return this.dims.length; }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewInitializedArrayExpr: "+this);
		Type type = this.type;
		JENode[] args = this.args;
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
public final view JNewClosure of NewClosure extends JENode {
	public:ro	JStruct		clazz;

	@getter public final CallType	get$type()	{ return (CallType) ((NewClosure)this).getType(); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating NewClosure: "+this);
		CallType xtype = this.type;
		JStruct cl = clazz;
		code.setLinePos(this);
		code.addInstr(op_new,clazz.xtype);
		// First arg ('this' pointer) is generated by 'op_dup'
		if( reqType ≢ Type.tpVoid )
			code.addInstr(op_dup);
		// Constructor call args (first args 'this' skipped)
		if( code.method!=null && !code.method.isStatic() )
			code.addInstrLoadThis();
		code.addConst(xtype.arity);
		// Now, fill proxyed fields (vars)
		foreach (JField f; cl.members) {
			if( !f.isNeedProxy() ) continue;
			JVar v = ((JLVarExpr)f.init).var;
			code.addInstr(Instr.op_load,v);
		}
		JMethod func = clazz.resolveMethod(nameInit,KString.from("(I)V"));
		code.addInstr(op_call,func,false);
	}
}

