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

import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

public final view JNewExpr of NewExpr extends JENode {

	public:ro	JMethod			func;
	public:ro	JENode			outer;
	public:ro	JENode			tpinfo;
	public:ro	JENode[]		args;
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating NewExpr: "+this);
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
					if (type ≈ ((Struct)code.clazz).args[i].getAType()) break;
				if( i >= ((Struct)code.clazz).args.length )
					throw new CompilerException(this,"Can't create an instance of argument type "+type);
				ENode tie = new IFldExpr(pos,new ThisExpr(pos),((Struct)code.clazz).resolveField(nameTypeInfo));
				ENode e = new CastExpr(pos,type,
					new CallExpr(pos,tie,
						Type.tpTypeInfo.tdecl.resolveMethod("newInstance",Type.tpObject,Type.tpInt),
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
		// Generate outer instance argument for this constructor call
		if( outer != null )
			outer.generate(code,null);
		// Generate typeinfo argument for this constructor call
		if (tpinfo != null)
			tpinfo.generate(code,null);
		// Constructor call args (first args 'this' skipped)
		for(int i=0; i < args.length; i++)
			args[i].generate(code,null);
		if( type.getStruct() != null && type.getStruct().isLocal() ) {
			JStruct cl = (JStruct)(Struct)((CompaundType)type).tdecl;
			foreach (JField f; cl.getAllFields()) {
				if( !f.isNeedProxy() ) continue;
				JVar v = ((JLVarExpr)f.init).var;
				code.addInstr(Instr.op_load,v);
			}
		}
		code.addInstr(op_call,func,true,type);
	}
}


public final view JNewArrayExpr of NewArrayExpr extends JENode {
	public:ro	Type				type;
	public:ro	JENode[]			args;
	public:ro	Type				arrtype;
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating NewArrayExpr: "+this);
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

public final view JNewInitializedArrayExpr of NewInitializedArrayExpr extends JENode {
	public:ro	Type				type;
	public:ro	JENode[]			args;
	public:ro	int					dim;
	public:ro	int[]				dims;

	@getter public final int	get$dim()	{ return this.dims.length; }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating NewInitializedArrayExpr: "+this);
		Type type = this.type;
		JENode[] args = this.args;
		code.setLinePos(this);
		if( dim == 1 ) {
			type = ((ArrayType)type).arg;
			code.addConst(args.length);
			code.addInstr(Instr.op_newarray,type);
		} else {
			for(int i=0; i < dim; i++)
				code.addConst(dims[i]);
			code.addInstr(Instr.op_multianewarray,type,dim);
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

public final view JNewClosure of NewClosure extends JENode {
	public:ro	JStruct		clazz;

	@getter public final CallType	get$type()	{ return (CallType) ((NewClosure)this).getType(); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating NewClosure: "+this);
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
		foreach (JField f; cl.getAllFields()) {
			if( !f.isNeedProxy() ) continue;
			JVar v = ((JLVarExpr)f.init).var;
			code.addInstr(Instr.op_load,v);
		}
		JMethod func = clazz.resolveMethod(null,KString.from("(I)V"));
		code.addInstr(op_call,func,true);
	}
}

