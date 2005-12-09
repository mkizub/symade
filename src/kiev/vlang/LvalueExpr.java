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
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.Instr.*;
import kiev.vlang.Operator.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class AccessExpr extends LvalueExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	private static KString nameWrapperSelf = KString.from("$self");
	
	@att public ENode			obj;
	
	@att public NameRef			ident;

	public AccessExpr() {}

	public AccessExpr(int pos) {
		super(pos);
	}
	
	public AccessExpr(int pos, ENode obj, NameRef ident) {
		super(pos);
		this.obj = obj;
		this.ident = ident;
	}

	public void mainResolveOut() {
		ASTNode[] res;
		Type[] tps;

		// pre-resolve result
		if( obj instanceof TypeRef ) {
			tps = new Type[]{ ((TypeRef)obj).getType() };
			res = new ASTNode[1];
			if( ident.name.equals(nameThis) )
				res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
		}
		else {
			ENode e = obj;
			//tps = new Type[]{e.getType()};
			tps = e.getAccessTypes();
			res = new ASTNode[tps.length];
			for (int si=0; si < tps.length; si++) {
				Type tp = tps[si];
				if( ident.name.equals(nameWrapperSelf) && tp.isReference() ) {
					if (tp.isWrapper()) {
						tps[si] = ((WrapperType)tp).getUnwrappedType();
						res[si] = obj;
					}
					// compatibility with previois version
					else if (tp.isInstanceOf(Type.tpPrologVar)) {
						tps[si] = tp;
						res[si] = (ENode)~obj;
					}
				}
				else if (ident.name.byteAt(0) == '$') {
					while (tp.isWrapper())
						tps[si] = tp = ((WrapperType)tp).getUnwrappedType();
				}
				else if( ident.name.equals(nameLength) ) {
					if( tp.isArray() ) {
						tps[si] = Type.tpInt;
						res[si] = new ArrayLengthExpr(pos,(ENode)e.copy(), (NameRef)ident.copy());
					}
				}
			}
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			DNode@ v;
			ResInfo info;
			if (tp.resolveNameAccessR(v,info=new ResInfo(this,ResInfo.noStatic | ResInfo.noImports),ident.name) )
				res[si] = makeExpr(v,info,(ENode)~obj);
			else if (tp.resolveStaticNameR(v,info=new ResInfo(this),ident.name))
				res[si] = makeExpr(v,info,tp.getStruct());
		}
		int cnt = 0;
		int idx = -1;
		for (int si=0; si < res.length; si++) {
			if (res[si] != null) {
				cnt ++;
				if (idx < 0) idx = si;
			}
		}
		if (cnt > 1) {
			StringBuffer msg = new StringBuffer("Umbigous access:\n");
			for(int si=0; si < res.length; si++) {
				if (res[si] == null)
					continue;
				msg.append("\t").append(res).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		if (cnt == 0) {
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			this.obj = this.obj;
			throw new CompilerException(this, msg.toString());
		}
		this.replaceWithNode(res[idx]);
	}
	
	public void resolve(Type reqType) throws CompilerException {
		ENode[] res;
		Type[] tps;

		// resolve access
		obj.resolve(null);

	try_static:
		if( obj instanceof TypeRef ) {
			tps = new Type[]{ ((TypeRef)obj).getType() };
			res = new ENode[1];
			if( ident.name.equals(nameThis) )
				res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
		}
		else {
			ENode e = obj;
			tps = e.getAccessTypes();
			res = new ENode[tps.length];
			for (int si=0; si < tps.length; si++) {
				Type tp = tps[si];
				if( ident.name.equals(nameWrapperSelf) && tp.isReference() ) {
					if (tp.isWrapper()) {
						tps[si] = ((WrapperType)tp).getUnwrappedType();
						res[si] = obj;
					}
					else if (tp.isInstanceOf(Type.tpPrologVar)) {
						tps[si] = tp;
						res[si] = obj;
					}
				}
				else if (ident.name.byteAt(0) == '$') {
					while (tp.isWrapper())
						tps[si] = tp = ((WrapperType)tp).getUnwrappedType();
				}
				else if( ident.name.equals(nameLength) ) {
					if( tp.isArray() ) {
						tps[si] = Type.tpInt;
						res[si] = new ArrayLengthExpr(pos,(ENode)e.copy(), (NameRef)ident.copy());
					}
				}
			}
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			DNode@ v;
			ResInfo info;
			if (!(obj instanceof TypeRef) &&
				tp.resolveNameAccessR(v,info=new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),ident.name) )
				res[si] = makeExpr(v,info,(ENode)~obj);
			else if (tp.resolveStaticNameR(v,info=new ResInfo(this),ident.name))
				res[si] = makeExpr(v,info,tp.getStruct());
		}
		int cnt = 0;
		int idx = -1;
		for (int si=0; si < res.length; si++) {
			if (res[si] != null) {
				cnt ++;
				if (idx < 0) idx = si;
			}
		}
		if (cnt > 1) {
			StringBuffer msg = new StringBuffer("Umbigous access:\n");
			for(int si=0; si < res.length; si++) {
				if (res[si] == null)
					continue;
				msg.append("\t").append(res).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		if (cnt == 0) {
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			this.obj = this.obj;
			throw new CompilerException(this, msg.toString());
			//return;
		}
		this.replaceWithNodeResolve(reqType,(ENode)~res[idx]);
	}

	private ENode makeExpr(ASTNode v, ResInfo info, ASTNode o) {
		if( v instanceof Field ) {
			return info.buildAccess(this, o, v);
		}
		else if( v instanceof Struct ) {
			TypeRef tr = new TypeRef(((Struct)v).type);
			return tr;
		}
		else {
			throw new CompilerException(this,"Identifier "+ident+" must be a class's field");
		}
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
    	return obj+"."+ident;
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append(ident.name);
		return dmp;
	}
}

@node
public class IFldExpr extends AccessExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	public static final int[] masks =
		{	0,
			0x1       ,0x3       ,0x7       ,0xF       ,
			0x1F      ,0x3F      ,0x7F      ,0xFF      ,
			0x1FF     ,0x3FF     ,0x7FF     ,0xFFF     ,
			0x1FFF    ,0x3FFF    ,0x7FFF    ,0xFFFF    ,
			0x1FFFF   ,0x3FFFF   ,0x7FFFF   ,0xFFFFF   ,
			0x1FFFFF  ,0x3FFFFF  ,0x7FFFFF  ,0xFFFFFF  ,
			0x1FFFFFF ,0x3FFFFFF ,0x7FFFFFF ,0xFFFFFFF ,
			0x1FFFFFFF,0x3FFFFFFF,0x7FFFFFFF,0xFFFFFFFF
		};

	@ref public Field		var;

	public IFldExpr() {
	}

	public IFldExpr(int pos, ENode obj, NameRef ident, Field var) {
		super(pos, obj, ident);
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var) {
		super(pos, obj, new NameRef(pos,var.name.name));
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var, boolean direct_access) {
		super(pos, obj, new NameRef(pos,var.name.name));
		this.var = var;
		assert(obj != null && var != null);
		if (direct_access) setAsField(true);
	}

	public String toString() {
		if( obj.getPriority() < opAccessPriority )
			return "("+obj.toString()+")."+var.toString();
		else
			return obj.toString()+"."+var.toString();
	}

	public Type getType() {
		return Type.getRealType(obj.getType(),var.type);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public LvalDNode[] getAccessPath() {
		if (obj instanceof LVarExpr) {
			LVarExpr va = (LVarExpr)obj;
			if (va.getVar().isFinal() && va.getVar().isForward())
				return new LvalDNode[]{va.getVar(), this.var};
			return null;
		}
		if (obj instanceof IFldExpr) {
			IFldExpr ae = (IFldExpr)obj;
			if !(ae.var.isFinal() || ae.var.isForward())
				return null;
			LvalDNode[] path = ae.getAccessPath();
			if (path == null)
				return null;
			return (LvalDNode[])Arrays.append(path, var);
		}
		return null;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		obj.resolve(null);

		// Set violation of the field
		if( pctx.method != null
		 && obj instanceof LVarExpr && ((LVarExpr)obj).ident.equals(nameThis)
		)
			pctx.method.addViolatedField(var);

		setResolved(true);
	}

	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.isStructInstanceOf((Struct)var.parent) )
			code.addInstr(Instr.op_checkcast,((Struct)var.parent).type);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - load only: "+this);
		code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		Field f = (Field)var;
		var.acc.verifyReadAccess(this,var);
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		code.addInstr(op_getfield,f,obj.getType());
		if( Kiev.verify && f.type.isArgument() && getType().isReference() )
			code.addInstr(op_checkcast,getType());
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - load & dup: "+this);
		code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		Field f = (Field)var;
		var.acc.verifyReadAccess(this,var);
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		code.addInstr(op_dup);
		code.addInstr(op_getfield,f,obj.getType());
		if( Kiev.verify && f.type.isArgument() && getType().isReference() )
			code.addInstr(op_checkcast,getType());
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - access only: "+this);
		code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - store only: "+this);
		code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_putfield,var,obj.getType());
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - store & dup: "+this);
		code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_dup_x);
		code.addInstr(op_putfield,var,obj.getType());
	}

	public boolean	isConstantExpr() {
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.isConstantExpr();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				return true;
			}
		}
		return false;
	}
	public Object	getConstValue() {
		var.acc.verifyReadAccess(this,var);
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.getConstValue();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				ConstantValueAttr cva = (ConstantValueAttr)var.getAttr(attrConstantValue);
				return cva.value;
			}
		}
    	throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public Dumper toJava(Dumper dmp) {
		if( obj.getPriority() < opAccessPriority ) {
			dmp.append('(').append(obj).append(").");
		} else {
			dmp.append(obj).append('.');
		}
		return dmp.append(var.name).space();
	}
}

@node
public class ContainerAccessExpr extends LvalueExpr {
	
	@dflow(out="index") private static class DFI {
	@dflow(in="this:in")	ENode		obj;
	@dflow(in="obj")		ENode		index;
	}

	@att public ENode		obj;
	
	@att public ENode		index;

	public ContainerAccessExpr() {
	}

	public ContainerAccessExpr(int pos, ENode obj, ENode index) {
		super(pos);
		this.obj = obj;
		this.index = index;
	}

	public String toString() {
		if( obj.getPriority() < opContainerElementPriority )
			return "("+obj.toString()+")["+index.toString()+"]";
		else
			return obj.toString()+"["+index.toString()+"]";
	}

	public Type getType() {
		try {
			Type t = obj.getType();
			if( t.isArray() ) {
				return Type.getRealType(t,t.args[0]);
			}
			else {
				// Resolve overloaded access method
				Method@ v;
				MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType()},Type.tpAny);
				ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
				if( !PassInfo.resolveBestMethodR(t,v,info,nameArrayOp,mt) )
					return Type.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+t);
				return Type.getRealType(t,((Method)v).type.ret);
			}
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type t = obj.getType();
		if( t.isArray() ) {
			return new Type[]{Type.getRealType(t,t.args[0])};
		} else {
			Struct s = t.getStruct();
		lookup_op:
			for(;;) {
				s.checkResolved();
				if (s instanceof Struct) {
					Struct ss = (Struct)s;
					foreach(ASTNode n; ss.members; n instanceof Method && ((Method)n).name.equals(nameArrayOp))
						return new Type[]{Type.getRealType(t,((Method)n).type.ret)};
				}
				if( s.super_type != null ) {
					s = s.super_type.clazz;
					continue;
				}
				//throw new RuntimeException("Resolved object "+obj+" of type "+t+" is not an array and does not overrides '[]' operator");
				return Type.emptyArray;
			}
		}
	}

	public int getPriority() { return opContainerElementPriority; }

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		obj.resolve(null);
		if( obj.getType().getStruct() != null ) {
			// May be an overloaded '[]' operator, ensure overriding
			Struct s = obj.getType().getStruct();
		lookup_op:
			for(;;) {
				s.checkResolved();
				if (s instanceof Struct) {
					Struct ss = (Struct)s;
					foreach(ASTNode n; ss.members; n instanceof Method && ((Method)n).name.equals(nameArrayOp))
						break lookup_op;
				}
				if( s.super_type != null ) {
					s = s.super_type.clazz;
					continue;
				}
				throw new RuntimeException("Resolved object "+obj+" of type "+obj.getType()+" is not an array and does not overrides '[]' operator");
			}
		}
		index.resolve(null);
		setResolved(true);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load only: "+this);
		code.setLinePos(this.getPosLine());
		if( obj.getType().isArray() ) {
			obj.generate(code,null);
			index.generate(code,null);
			code.addInstr(Instr.op_arr_load);
		} else {
			// Resolve overloaded access method
			Method@ v;
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType()},Type.tpAny);
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt));
			obj.generate(code,null);
			index.generate(code,null);
			Method func = (Method)v;
			code.addInstr(Instr.op_call,func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret.isReference()
			 && ( !getType().isStructInstanceOf(func.type.ret.getStruct()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load & dup: "+this);
		code.setLinePos(this.getPosLine());
		if( obj.getType().isArray() ) {
			obj.generate(code,null);
			index.generate(code,null);
			code.addInstr(op_dup2);
			code.addInstr(Instr.op_arr_load);
		} else {
			throw new CompilerException(this,"Too complex expression for overloaded operator '[]'");
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - access only: "+this);
		code.setLinePos(this.getPosLine());
		obj.generate(code,null);
		index.generate(code,null);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store only: "+this);
		code.setLinePos(this.getPosLine());
		Type objType = obj.getType();
		if( objType.isArray() ) {
			code.addInstr(Instr.op_arr_store);
		} else {
			// Resolve overloaded set method
			Method@ v;
			// We need to get the type of object in stack
			Type t = code.stack_at(0);
			ENode o = new LVarExpr(pos,new Var(pos,KString.Empty,t,0));
			Struct s = objType.getStruct();
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(objType,v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+objType);
			code.addInstr(Instr.op_call,(Method)v,false,objType);
			// Pop return value
			code.addInstr(Instr.op_pop);
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store & dup: "+this);
		code.setLinePos(this.getPosLine());
		if( obj.getType().isArray() ) {
			code.addInstr(op_dup_x2);
			code.addInstr(Instr.op_arr_store);
		} else {
			// Resolve overloaded set method
			Method@ v;
			// We need to get the type of object in stack
			Type t = code.stack_at(0);
			if( !(code.stack_at(1).isIntegerInCode() || code.stack_at(0).isReference()) )
				throw new CompilerException(this,"Index of '[]' can't be of type double or long");
			ENode o = new LVarExpr(pos,new Var(pos,KString.Empty,t,0));
			Struct s = obj.getType().getStruct();
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt));
			// The method must return the value to duplicate
			Method func = (Method)v;
			code.addInstr(Instr.op_call,func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret.isReference()
			 && ( !getType().isStructInstanceOf(func.type.ret.getStruct()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public Dumper toJava(Dumper dmp) {
		if( obj.getPriority() < opContainerElementPriority ) {
			dmp.append('(').append(obj).append(')');
		} else {
			dmp.append(obj);
		}
		dmp.append('[').append(index).append(']');
		return dmp;
	}
}

@node
public class ThisExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static public final FormPar thisPar = new FormPar(0,Constants.nameThis,Type.tpVoid,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD);
	
	public boolean super_flag;
	
	public ThisExpr() {
	}
	public ThisExpr(int pos) {
		super(pos);
	}
	public ThisExpr(boolean super_flag) {
		this.super_flag = super_flag;
	}

	public String toString() { return super_flag ? "super" : "this"; }

	public Type getType() {
		try {
			if (pctx.clazz == null)
				return Type.tpVoid;
			if (pctx.clazz.name.short_name.equals(nameIdefault))
				return pctx.clazz.package_clazz.type;
			if (super_flag)
				pctx.clazz.type.getSuperType();
			return pctx.clazz.type;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		if (pctx.method != null &&
			pctx.method.isStatic() &&
			!pctx.clazz.name.short_name.equals(nameIdefault)
		)
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
		setResolved(true);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load only: "+this);
		code.setLinePos(this.getPosLine());
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addNullConst();
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load & dup: "+this);
		code.setLinePos(this.getPosLine());
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addNullConst();
		}
		code.addInstr(op_dup);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store only: "+this);
		code.setLinePos(this.getPosLine());
		if (!code.method.isStatic())
			code.addInstrStoreThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_store,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addInstr(op_pop);
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store & dup: "+this);
		code.setLinePos(this.getPosLine());
		code.addInstr(op_dup);
		if (!code.method.isStatic())
			code.addInstrStoreThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_store,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addInstr(op_pop);
		}
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(toString()).space();
	}
}

@node
public class LVarExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static final KString namePEnv = KString.from("$env");

	@att
	public NameRef		ident;
	@ref
	private Var			var;

	public LVarExpr() {
	}
	public LVarExpr(int pos, Var var) {
		super(pos);
		this.var = var;
		this.ident = new NameRef(pos, var.name.name);
	}
	public LVarExpr(int pos, KString name) {
		super(pos);
		this.ident = new NameRef(pos, name);
	}
	public LVarExpr(KString name) {
		this.ident = new NameRef(name);
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.ident = new NameRef(pos, ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2)));
		else
			this.ident = new NameRef(pos, KString.from(t.image));
	}
	
	public String toString() {
		if (var == null)
			return ident.toString();
		return var.toString();
	}

	public Type getType() {
		try {
			return var.type;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{getVar()});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.type};
		return (Type[])sni.getTypes().clone();
	}

	public Var getVar() {
		if (var != null)
			return var;
		Var@ v;
		ResInfo info = new ResInfo(this);
		if( !PassInfo.resolveNameR(this,v,info,ident.name) )
			throw new CompilerException(this,"Unresolved var "+ident);
		var = v;
		return var;
	}

	public boolean preResolveIn(TransfProcessor proc) {
		getVar(); // calls resolving
		return false;
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		getVar(); // calls resolving
		return false;
	}
	
	public boolean preGenerate() {
		if (getVar().isLocalRuleVar()) {
			RuleMethod rm = (RuleMethod)pctx.method;
			assert(rm.params[0].type == Type.tpRule);
			Var pEnv = null;
			foreach (ENode n; rm.body.stats; n instanceof VarDecl) {
				VarDecl vd = (VarDecl)n;
				if (vd.var.name.equals(namePEnv)) {
					assert(vd.var.type.isInstanceOf(Type.tpRule));
					pEnv = vd.var;
					break;
				}
			}
			if (pEnv == null) {
				Kiev.reportError(this, "Cannot find "+namePEnv);
				return false;
			}
			Struct s = ((LocalStructDecl)((BlockStat)rm.body).stats[0]).clazz;
			Field f = s.resolveField(ident.name);
			replaceWithNode(new IFldExpr(pos, new LVarExpr(pos, pEnv), (NameRef)~ident, f));
		}
		return true;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		// Check if we try to access this var from local inner/anonymouse class
		if( pctx.clazz.isLocal() ) {
			if( getVar().pctx.clazz != this.pctx.clazz ) {
				var.setNeedProxy(true);
				setAsField(true);
				// Now we need to add this var as a fields to
				// local class and to initializer of this class
				Field vf;
				if( (vf = pctx.clazz.resolveField(ident.name,false)) == null ) {
					// Add field
					vf = pctx.clazz.addField(new Field(ident.name,var.type,ACC_PUBLIC));
					vf.setNeedProxy(true);
					vf.init = (ENode)this.copy();
				}
			}
		}
		setResolved(true);
	}

	public Field resolveProxyVar(Code code) {
		Field proxy_var = code.clazz.resolveField(ident.name,false);
		if( proxy_var == null && code.method.isStatic() && !code.method.isVirtualStatic() )
			throw new CompilerException(this,"Proxyed var cannot be referenced from static context");
		return proxy_var;
	}

	public Field resolveVarVal() {
		BaseType prt = Type.getProxyType(var.type);
		Field var_valf = prt.clazz.resolveField(nameCellVal);
		return var_valf;
	}

	public void resolveVarForConditions(Code code) {
		if( code.cond_generation ) {
			// Bind the correct var
			if( getVar().parent != code.method ) {
				assert( var.parent instanceof Method, "Non-parametrs var in condition" );
				if( ident.name==nameResultVar ) var = code.method.getRetVar();
				else for(int i=0; i < code.method.params.length; i++) {
					Var v = code.method.params[i];
					if( !v.name.equals(var.name) ) continue;
					assert( var.type.equals(v.type), "Type of vars in overriden methods missmatch" );
					var = v;
					break;
				}
				trace(Kiev.debugStatGen,"Var "+var+" substituted for condition");
			}
			assert( var.parent == code.method, "Can't find var for condition" );
		}
	}

	public void generateVerifyCheckCast(Code code) {
		if( !Kiev.verify ) return;
		if( !var.type.isReference() || var.type.isArray() ) return;
		Type chtp = null;
		if( getVar().parent instanceof Method ) {
			Method m = (Method)var.parent;
			for(int i=0; i < m.params.length; i++) {
				if( var == m.params[i] ) {
					chtp = m.jtype.args[i];
					break;
				}
			}
		}
		if( chtp == null )
			chtp = var.type.getJavaType();
		if( !var.type.isStructInstanceOf(chtp.getStruct()) ) {
			code.addInstr(op_checkcast,var.type);
			return;
		}
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - load only: "+this);
		code.setLinePos(this.getPosLine());
		if( code.cond_generation ) resolveVarForConditions(code);
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but code.var["+var.getBCpos()+"] == null");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.type);
			} else {
				code.addInstr(op_load,var);
			}
			if( var.isNeedRefProxy() ) {
				code.addInstr(op_getfield,resolveVarVal(),code.clazz.type);
			}
		}
		generateVerifyCheckCast(code);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - load & dup: "+this);
		code.setLinePos(this.getPosLine());
		if( code.cond_generation ) resolveVarForConditions(code);
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but code.var["+var.getBCpos()+"] == null");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				if( var.isNeedRefProxy() ) {
					code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.type);
					code.addInstr(op_dup);
				} else {
					code.addInstr(op_dup);
					code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.type);
				}
			} else {
				code.addInstr(op_load,var);
				code.addInstr(op_dup);
			}
			if( var.isNeedRefProxy() ) {
				code.addInstr(op_getfield,resolveVarVal(),resolveProxyVar(code).getType());
			}
		}
		generateVerifyCheckCast(code);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - access only: "+this);
		code.setLinePos(this.getPosLine());
		if( code.cond_generation ) resolveVarForConditions(code);
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				if( var.isNeedRefProxy() ) {
					code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.type);
					code.addInstr(op_dup);
				} else {
					code.addInstr(op_dup);
				}
			} else {
				if( var.isNeedRefProxy() )
					code.addInstr(op_load,var);
			}
		}
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - store only: "+this);
		code.setLinePos(this.getPosLine());
		if( code.cond_generation ) resolveVarForConditions(code);
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but code.var["+var.getBCpos()+"] == null");
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				if( !var.isNeedRefProxy() ) {
					code.addInstr(op_putfield,resolveProxyVar(code),code.clazz.type);
				} else {
					code.addInstr(op_putfield,resolveVarVal(),code.clazz.type);
				}
			} else {
				if( !var.isNeedRefProxy() ) {
					code.addInstr(op_store,var);
				} else {
					code.addInstr(op_putfield,resolveVarVal(),code.clazz.type);
				}
			}
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - store & dup: "+this);
		code.setLinePos(this.getPosLine());
		if( code.cond_generation ) resolveVarForConditions(code);
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but code.var["+var.getBCpos()+"] == null");
			code.addInstr(op_dup);
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				code.addInstr(op_dup_x);
				if( !var.isNeedRefProxy() ) {
					code.addInstr(op_putfield,resolveProxyVar(code),code.clazz.type);
				} else {
					code.addInstr(op_putfield,resolveVarVal(),code.clazz.type);
				}
			} else {
				if( !var.isNeedRefProxy() ) {
					code.addInstr(op_dup);
					code.addInstr(op_store,var);
				} else {
					code.addInstr(op_dup_x);
					code.addInstr(op_putfield,resolveVarVal(),code.clazz.type);
				}
			}
		}
		generateVerifyCheckCast(code);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(var);
		if( var.isNeedRefProxy() )
			dmp.append(".val");
		return dmp.space();
	}
}

@node
public class SFldExpr extends AccessExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@ref public Field		var;

	public SFldExpr() {
	}

	public SFldExpr(int pos, Field var) {
		super(pos);
		this.obj = new TypeRef(pos,((Struct)var.parent).type);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
	}

	public SFldExpr(int pos, Field var, boolean direct_access) {
		super(pos);
		this.obj = new TypeRef(pos,((Struct)var.parent).type);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		if (direct_access) setAsField(true);
	}

	public String toString() { return var.toString(); }

	public boolean	isConstantExpr() {
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.isConstantExpr();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				return true;
			}
		}
		return false;
	}
	public Object	getConstValue() {
		var.acc.verifyReadAccess(this,var);
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.getConstValue();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				ConstantValueAttr cva = (ConstantValueAttr)var.getAttr(attrConstantValue);
				return cva.value;
			}
		}
    	throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public Type getType() {
		try {
			return var.type;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type[] types;
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			types = new Type[]{var.type};
		else
			types = (Type[])sni.getTypes().clone();
		return types;
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		// Set violation of the field
		if( pctx.method != null )
			pctx.method.addViolatedField(var);
		setResolved(true);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - load only: "+this);
		code.setLinePos(this.getPosLine());
		var.acc.verifyReadAccess(this,var);
		code.addInstr(op_getstatic,var,code.clazz.type);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - load & dup: "+this);
		code.setLinePos(this.getPosLine());
		var.acc.verifyReadAccess(this,var);
		code.addInstr(op_getstatic,var,code.clazz.type);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - store only: "+this);
		code.setLinePos(this.getPosLine());
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_putstatic,var,code.clazz.type);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - store & dup: "+this);
		code.setLinePos(this.getPosLine());
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_dup);
		code.addInstr(op_putstatic,var,code.clazz.type);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Dumper toJava(Dumper dmp) {
		Struct cl = (Struct)var.parent;
		ClazzName cln = cl.type.getClazzName();
		return dmp.space().append(cln).append('.').append(var.name).space();
	}

}

@node
public class OuterThisAccessExpr extends AccessExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@ref public Struct		outer;
	public Field[]			outer_refs = Field.emptyArray;

	public OuterThisAccessExpr() {
	}

	public OuterThisAccessExpr(int pos, Struct outer) {
		super(pos);
		this.obj = new TypeRef(pos,outer.type);
		this.ident = new NameRef(pos,nameThis);
		this.outer = outer;
	}

	public String toString() { return outer.name.toString()+".this"; }

	public Type getType() {
		return outer.type;
	}

	public static Field outerOf(Struct clazz) {
		foreach (ASTNode n; clazz.members; n instanceof Field) {
			Field f = (Field)n;
			if( f.name.name.startsWith(nameThisDollar) ) {
				trace(Kiev.debugResolve,"Name of field "+f+" starts with this$");
				return f;
			}
		}
		return null;
	}

	public void resolve(Type reqType) throws RuntimeException {
		outer_refs = Field.emptyArray;
		trace(Kiev.debugResolve,"Resolving "+this);
		Field ou_ref = outerOf(pctx.clazz);
		if( ou_ref == null )
			throw new RuntimeException("Outer 'this' reference in non-inner or static inner class "+pctx.clazz);
		do {
			trace(Kiev.debugResolve,"Add "+ou_ref+" of type "+ou_ref.type+" to access path");
			outer_refs = (Field[])Arrays.append(outer_refs,ou_ref);
			if( ou_ref.type.isInstanceOf(outer.type) ) break;
			ou_ref = outerOf(ou_ref.type.getStruct());
		} while( ou_ref!=null );
		if( !outer_refs[outer_refs.length-1].type.isInstanceOf(outer.type) )
			throw new RuntimeException("Outer class "+outer+" not found for inner class "+pctx.clazz);
		if( Kiev.debugResolve ) {
			StringBuffer sb = new StringBuffer("Outer 'this' resolved as this");
			for(int i=0; i < outer_refs.length; i++)
				sb.append("->").append(outer_refs[i].name);
			System.out.println(sb.toString());
		}
		if( pctx.method.isStatic() && !pctx.method.isVirtualStatic() ) {
			throw new RuntimeException("Access to 'this' in static method "+pctx.method);
		}
		setResolved(true);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load only: "+this);
		code.setLinePos(this.getPosLine());
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length; i++)
			code.addInstr(op_getfield,outer_refs[i],code.clazz.type);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load & dup: "+this);
		code.setLinePos(this.getPosLine());
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length; i++) {
			if( i == outer_refs.length-1 ) code.addInstr(op_dup);
			code.addInstr(op_getfield,outer_refs[i],code.clazz.type);
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - access only: "+this);
		code.setLinePos(this.getPosLine());
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length-1; i++) {
			code.addInstr(op_getfield,outer_refs[i],code.clazz.type);
		}
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - store only: "+this);
		code.addInstr(op_putfield,outer_refs[outer_refs.length-1],code.clazz.type);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SimpleAccessExpr - store & dup: "+this);
		code.addInstr(op_dup_x);
		code.addInstr(op_putfield,outer_refs[outer_refs.length-1],code.clazz.type);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Dumper toJava(Dumper dmp) { return dmp.space().append(outer.name.name).append(".this").space(); }
}


