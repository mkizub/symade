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

import static kiev.stdlib.Debug.*;
import static kiev.vlang.WorkByContractCondition.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Method.java,v 1.6.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.2 $
 *
 */

public class Method extends ASTNode implements Named,Typed,ScopeOfNames,SetBody,Accessable {
	public static Method[]	emptyArray = new Method[0];

	/** Method's access */
	public virtual Access	acc;

	/** Name of the method */
	public NodeName			name;

	/** Return type of the method and signature (argument's types) */
	public MethodType		type;

	/** The java type of the method (if method overrides parametriezed method) */
	public MethodType		jtype;

	/** The type of the dispatcher method (if method is a multimethod) */
	public MethodType		dtype;

	/** Signatures of thrown types (structures) */
//	public Type[]			throwns = Type.emptyArray;

	/** Parameters of this method */
	public Var[]			params = Var.emptyArray;

	/** Return value of this method */
	public Var				retvar;

	/** Body of the method - ASTBlockStat or BlockStat
	 */
	public ASTNode			body;

	/** Array of attributes of this method
	 */
	public Attr[]			attrs = Attr.emptyArray;

	/** Require & ensure clauses */
	public WorkByContractCondition[]	conditions = WorkByContractCondition.emptyArray;

	/** Violated by method fields for normal methods, and checked fields
	 *  for invariant method
	 */
	public Field[]			violated_fields = Field.emptyArray;

	/** Indicates that this method is inlined by dispatcher method
	 */
	public boolean			inlined_by_dispatcher;

	public Method(ASTNode clazz, KString name, MethodType type, int acc) {
		this(clazz,name,type,null,acc);
	}

	public Method(ASTNode clazz, KString name, MethodType type, MethodType dtype, int acc) {
		super(0,acc);
		this.name = new NodeName(name);
		this.type = type;
		this.dtype = dtype;
		if( ((Struct)clazz).generated_from == null )
			this.jtype = (MethodType)(dtype==null?type:dtype).getJavaType();
		else
			this.jtype = (MethodType)Type.getRealType(((Struct)clazz).type,(dtype==null?type:dtype)).getJavaType();
        // Parent is always the class this method belongs to
		this.parent = clazz;
		this.acc = new Access(0);
	}

	public Access get$acc() {
		return acc;
	}

	public void set$acc(Access a) {
		acc = a;
		acc.verifyAccessDecl(this);
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public void addViolatedField(Field f) {
		if( isInvariantMethod() ) {
			f.invs = (Method[])Arrays.appendUniq(f.invs,this);
			if( ((Struct)parent).instanceOf((Struct)f.parent) )
				violated_fields = (Field[])Arrays.appendUniq(violated_fields,f);
		} else {
			violated_fields = (Field[])Arrays.appendUniq(violated_fields,f);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(name+"(");
		for(int i=0; type.args != null && i < type.args.length; i++) {
			sb.append(type.args[i].toString());
			if( i < (type.args.length-1) ) sb.append(",");
		}
		sb.append(")->").append(type.ret);
		return sb.toString();
	}

	public static String toString(KString nm, Expr[] args) {
		return toString(nm,args,null);
	}

	public static String toString(KString nm, Expr[] args, Type ret) {
		StringBuffer sb = new StringBuffer(nm+"(");
		for(int i=0; args!=null && i < args.length; i++) {
			sb.append(args[i].getType().toString());
			if( i < (args.length-1) ) sb.append(",");
		}
		if( ret != null )
			sb.append(")->").append(ret);
		else
			sb.append(")->???");
		return sb.toString();
	}

	public NodeName getName() { return name; }

	public Type	getType() { return type.ret; }

	public Var	getRetVar() {
		if( retvar == null )
			retvar = new Var(pos,this,nameResultVar,type.ret,ACC_FINAL);
		return retvar;
	}

	public void cleanup() {
//		parent=null;
		// Methods are persistant
		if( body != null ) {
			body.cleanup();
			body = null;
		}
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(name);
	}

	public Expr[] makeArgs(Expr[] args, Type t) {
		if( isVarArgs() ) {
			Expr[] varargs = new Expr[type.args.length];
			int j;
			for(j=0; j < varargs.length-1; j++)
				varargs[j] = CastExpr.autoCast(args[j],Type.getRealType(t,type.args[j]));
			Expr[] varargs2 = new Expr[args.length - varargs.length + 1];
			for(int k=0; k < varargs2.length; j++,k++) {
				varargs2[k] = CastExpr.autoCastToReference(args[j]);
			}
			NewInitializedArrayExpr nae =
				new NewInitializedArrayExpr(getPos(),Type.tpObject,1,varargs2);
			varargs[varargs.length-1] = nae;
			return varargs;
		} else {
			int i = 0;
			int j = 0;
			if( this instanceof RuleMethod ) j++;
			for(; i < args.length; i++, j++)
				args[i] = CastExpr.autoCast(args[i],Type.getRealType(t,type.args[j]));
			return args;
		}
	}

	public boolean equals(KString name, Expr[] args, Type ret, Type type, int resfl) {
		if( this.name.equals(name) )
			return compare(name,args,ret,type,true);
		return false;
	}
	public boolean equalsByCast(KString name, Expr[] args, Type ret, Type type, int resfl) {
		if( this.name.equals(name) )
			return compare(name,args,ret,type,false);
		return false;
	}

	public boolean compare(KString name, Expr[] args, Type ret, Type type, boolean exact) throws RuntimeException {
		if( !this.name.equals(name) ) return false;
		int type_len = this.type.args.length;
		int args_len = args==null? 0 : args.length;
		if( type_len != args_len ) {
			if( !isVarArgs() ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" differ in number of params: "+type_len+" != "+args_len);
				return false;
			} else if( type_len-1 > args_len ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" not match in number of params: "+type_len+" != "+args_len);
				return false;
			}
		}
		trace(Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,args,ret));
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if( exact && !Type.getRealType(type,args[i].getType()).equals(Type.getRealType(type,this.type.args[i])) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" differ in param # "+i+": "+Type.getRealType(type,this.type.args[i])+" != "+Type.getRealType(type,args[i].getType()));
				return false;
			}
			else if( !exact && !Type.getRealType(type,args[i].getType()).isAutoCastableTo(Type.getRealType(type,this.type.args[i])) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" differ in param # "+i+": "+Type.getRealType(type,args[i].getType())+" not auto-castable to "+Type.getRealType(type,this.type.args[i]));
				return false;
			}
		}
		boolean match = false;
		if( ret == null )
			match = true;
		else if( exact &&  Type.getRealType(type,this.type.ret).equals(Type.getRealType(type,ret)) )
			match = true;
		else if( !exact && Type.getRealType(type,this.type.ret).isAutoCastableTo(Type.getRealType(type,ret)) )
			match = true;
		else
			match = false;
		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,args,ret)+(match?" match":" do not match"));
		return match;
	}

//	public Type addThrown(Type thr) {
//		throwns = (Type[])Arrays.append(throwns,thr);
//		return thr;
//	}

//	public Var addParametr(Var par) {
//		params = (Var[])Arrays.append(params,par);
//		if( code == null ) code = new Code(this);
//		code.addVar(par);
//		return par;
//	}

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		if( !(a.name==attrOperator || a.name==attrImport
			|| a.name==attrRequire || a.name==attrEnsure) ) {
			for(int i=0; i < attrs.length; i++) {
				if(attrs[i].name == a.name) {
					attrs[i] = a;
					return a;
				}
			}
		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		for(int i=0; i < attrs.length; i++)
			if( attrs[i].name.equals(name) )
				return attrs[i];
		return null;
	}

	// TODO
	public Dumper toJavaDecl(Dumper dmp) {
		Struct cl = (Struct)parent;
		cl = Type.getRealType(Kiev.argtype,cl.type).clazz;
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( !name.equals(nameInit) )
			dmp.space()
			.append(((MethodType)Type.getRealType(Kiev.argtype,type)).ret)
			.forsed_space().append(name);
		else
			dmp.space().append(cl.name.short_name);
		dmp.append('(');
		int offset = 0;
		if( !isStatic() ) offset++;
		for(int i=offset; i < params.length; i++) {
			params[i].toJavaDecl(dmp,type.args[i-offset]);
			if( i < (params.length-1) ) dmp.append(",");
		}
		dmp.append(')').space();
		if( isAbstract() || body == null ) {
			dmp.append(';').newLine();
		} else {
			dmp.append(body).newLine();
		}
		return dmp;
	}

	public Dumper toCppDecl(Dumper dmp) {
		if (!isExportCpp())
			return dmp;
		Struct cl = (Struct)parent;
		cl = Type.getRealType(Kiev.argtype,cl.type).clazz;
		if (!isPrivate() && !isProtected())	dmp.newLine(-1).append("public:    ").newLine(1);
		else if (isProtected()) 			dmp.newLine(-1).append("protected: ").newLine(1);
		else if (isPrivate()) 				dmp.newLine(-1).append("private:   ").newLine(1);
		if( !name.equals(nameInit) ) {
			if( isAbstract() || body == null )
				dmp.append("virtual");
			dmp.space();
			Type ret = ((MethodType)Type.getRealType(Kiev.argtype,type)).ret;
			if (isNative()) ret.toCppJNI(dmp);
			else ret.toCpp(dmp);
			dmp.forsed_space().append(name);
		} else {
			dmp.space().append(cl.name.short_name);
		}
		dmp.append('(');
		int offset = 0;
		if( !isStatic() ) offset++;
		for(int i=offset; i < params.length; i++) {
			Type arg = Type.getRealType(Kiev.argtype,jtype.args[i-offset]);
			if (isNative()) arg.toCppJNI(dmp);
			else arg.toCpp(dmp);
			dmp.forsed_space().append(params[i].name.name);
			if( i < (params.length-1) ) dmp.append(",");
		}
		dmp.append(')').space();
		if (isConstCpp())
			dmp.append("const").space();
		if( isAbstract() || body == null ) {
			dmp.append(" = 0;").newLine();
		} else {
			dmp.append(body).newLine();
		}
		return dmp;
	}

	rule public resolveNameR(ASTNode@ node, List<ASTNode>@ path, KString name, Type tp, int resfl)
	{
		inlined_by_dispatcher,$cut,false
	;	node @= params, ((Var)node).name.equals(name)
	;	node @= type.fargs, ((Type)node).clazz.name.short_name.equals(name)
	;	node ?= retvar, ((Var)node).name.equals(name)
//		trace(Kiev.debugResolve,"Name "+name+" not found in method's parameters in method "+this)
	}

	public ASTNode resolve(Type reqType) {
		if( isResolved() ) return this;
		trace(Kiev.debugResolve,"Resolving method "+this);
		assert( PassInfo.clazz == parent );
		PassInfo.push(this);
		try {
			if (!inlined_by_dispatcher)
				NodeInfoPass.init();
			ScopeNodeInfoVector state = NodeInfoPass.pushState();
			state.guarded = true;
			if (!inlined_by_dispatcher) {
				for(int i=0; i < params.length; i++) {
					PassInfo.addResolvedNode(params[i],this);
					NodeInfoPass.setNodeType(params[i],params[i].type);
					NodeInfoPass.setNodeInitialized(params[i],true);
				}
			}
			foreach(WorkByContractCondition cond; conditions; cond.cond == CondRequire ) {
				cond.parent = this;
				cond.resolve(Type.tpVoid);
			}
			if( body != null ) {
				body.parent = this;
				if( type.ret == Type.tpVoid ) body.setAutoReturnable(true);
				//if( isMultiMethod() )
				//	((Struct)parent).makeDispatch(this);
				body = ((Statement)body).resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret == Type.tpVoid ) {
					if( body instanceof BlockStat ) {
						((BlockStat)body).stats = (ASTNode[])Arrays.append(((BlockStat)body).stats,new ReturnStat(pos,body,null));
						body.setAbrupted(true);
					}
					else if( body instanceof WorkByContractCondition );
					else
						Kiev.reportError(pos,"Return requared");
				} else {
					Kiev.reportError(pos,"Return requared");
				}
			}
			foreach(WorkByContractCondition cond; conditions; cond.cond == CondEnsure ) {
				cond.parent = this;
				if( type.ret != Type.tpVoid ) getRetVar();
				cond.resolve(Type.tpVoid);
			}

		} catch(Exception e ) {
			Kiev.reportError(0/*body.getPos()*/,e);
		} finally {
			if (!inlined_by_dispatcher)
				NodeInfoPass.close();
			PassInfo.pop(this);
		}

		setResolved(true);
		return this;
	}

	public void generate() {
		if( Kiev.debug ) System.out.println("\tgenerating Method "+this);
		PassInfo.push(this);
		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
			foreach(Field f; violated_fields; ((Struct)parent).instanceOf((Struct)f.parent) ) {
				foreach(Method inv; f.invs; ((Struct)parent).instanceOf((Struct)inv.parent) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(name.name.startsWith(nameSet) || name.name.startsWith(nameGet)) )
						conditions = (WorkByContractCondition[])Arrays.appendUniq(conditions,inv.body);
				}
			}
		}
		try {
			foreach(WorkByContractCondition cond; conditions; cond.cond != CondInvariant )
				cond.generate(Type.tpVoid);
		} finally { kiev.vlang.PassInfo.pop(this); kiev.vlang.Code.generation = false; }
		if( !isAbstract() && body != null ) {
			Code.reInit();
			Code.generation = true;
			PassInfo.push(this);
			try {
				if( !isBad() ) {
					if( params.length > 0 ) Code.addVars(params);
					if( Kiev.verify /*&& jtype != null*/ )
						generateArgumentCheck();
					if( Kiev.debugOutputC ) {
						foreach(WorkByContractCondition cond; conditions; cond.cond == CondRequire )
							Code.importCode(cond.code);
						foreach(WorkByContractCondition cond; conditions; cond.cond == CondInvariant ) {
							assert( cond.parent instanceof Method && cond.parent.isInvariantMethod() );
							if( !name.name.equals(nameInit) && !name.name.equals(nameClassInit) ) {
								if( !cond.parent.isStatic() )
									Code.addInstr(Instr.op_load,params[0]);
								Code.addInstr(Instr.op_call,(Method)cond.parent,false);
							}
							setGenPostCond(true);
						}
						if( !isGenPostCond() ) {
							foreach(WorkByContractCondition cond; conditions; cond.cond != CondRequire ) {
								setGenPostCond(true);
								break;
							}
						}
					}
					((Statement)body).generate(Type.tpVoid);
					if( Kiev.debugOutputC && isGenPostCond() ) {
						if( type.ret != Type.tpVoid ) {
							Code.addVar(getRetVar());
							Code.addInstr(Instr.op_store,getRetVar());
						}
						foreach(WorkByContractCondition cond; conditions; cond.cond == CondInvariant ) {
							if( !cond.parent.isStatic() )
								Code.addInstr(Instr.op_load,params[0]);
							Code.addInstr(Instr.op_call,(Method)cond.parent,false);
							setGenPostCond(true);
						}
						foreach(WorkByContractCondition cond; conditions; cond.cond == CondEnsure )
							Code.importCode(cond.code);
						if( type.ret != Type.tpVoid ) {
							Code.addInstr(Instr.op_load,getRetVar());
							Code.addInstr(Instr.op_return);
							Code.removeVar(getRetVar());
						} else {
							Code.addInstr(Instr.op_return);
						}
					}
					if( params.length > 0 ) Code.removeVars(params);
				} else {
					Code.addInstr(Instr.op_new,Type.tpError);
					Code.addInstr(Instr.op_dup);
					KString msg = KString.from("Compiled with errors");
					ConstPool.addStringCP(msg);
					Code.addConst(msg);
					Method func = Type.tpError.clazz.resolveMethod(nameInit,KString.from("(Ljava/lang/String;)V"));
					Code.addInstr(Instr.op_call,func,false);
					Code.addInstr(Instr.op_throw);
				}
				Code.generateCode();
			} catch(Exception e) {
				Kiev.reportError(pos,e);
				body = null;
			} finally { kiev.vlang.PassInfo.pop(this); kiev.vlang.Code.generation = false; }
		}
	}

	public CodeLabel getBreakLabel() {
		return ((BlockStat)body).getBreakLabel();
	}

	public void generateArgumentCheck() {
//		if( jtype == null ) return;
		int i=0;
		int j=0;
		if( !isStatic() ) j++;
		for(; i < type.args.length; i++, j++) {
			Type tp1 = Type.getRealType(Kiev.argtype,jtype.args[i]);
			Type tp2 = Type.getRealType(Kiev.argtype,params[j].type);
			if( !tp1.equals(tp2) ) {
				if (tp2.clazz.isEnum() && tp2.clazz.isPrimitiveEnum() && tp1.isIntegerInCode())
					continue;
				Code.addInstr(Instr.op_load,params[j]);
				Code.addInstr(Instr.op_checkcast,type.args[i]);
				Code.addInstr(Instr.op_store,params[j]);
			}
		}
	}

	public boolean setBody(Statement body) {
		trace(Kiev.debugMultiMethod,"Setting body of methods "+this);
		if (this.body == null) {
			this.body = body;
			this.body.parent = this;
		}
		else if (isMultiMethod()){
			BlockStat b = (BlockStat)this.body;
			b.addStatement(body);
		}
		else {
			throw new RuntimeException("Added body to method "+this+" which already have body");
		}

		return true;
	}

	public static Expr getAccessExpr(List<ASTNode> path) {
		Expr expr = new FieldAccessExpr(0,(Field)path.head());
		path = path.tail();
		foreach(ASTNode n; path)
			expr = new AccessExpr(0,expr,(Field)n);
		return expr;
	}

	public static Expr getAccessExpr(List<ASTNode> path,Expr expr) {
		foreach(ASTNode n; path)
			expr = new AccessExpr(0,expr,(Field)n);
		return expr;
	}

}

public class WorkByContractCondition extends Statement implements SetBody {

	public static WorkByContractCondition[]	emptyArray = new WorkByContractCondition[0];

	public static final int CondRequire		= 1;
	public static final int CondEnsure 		= 2;
	public static final int CondInvariant	= 3;

	public int			cond;
	public KString		name;
	public Statement	body;
	public CodeAttr		code;
	public Method		definer;

	public WorkByContractCondition(int pos, int cond, KString name, Statement body) {
		super(pos,null);
		this.name = name;
		this.cond = cond;
		this.body = body;
		if( body != null )
			body.parent = this;
	}

	public ASTNode resolve(Type reqType) {
		if( code != null ) return this;
		body = (Statement)body.resolve(Type.tpVoid);
		return this;
	}

	public void generate(Type reqType) {
		if( cond == CondInvariant ) {
			body.generate(Type.tpVoid);
			Code.addInstr(Instr.op_return);
		}
		else if( code == null ) {
			Code.reInit();
			Code.generation = true;
			Code.cond_generation = true;
			PassInfo.push(this);
			Method m = (Method)PassInfo.method;
			try {
				if( m.params.length > 0 ) Code.addVars(m.params);
				if( cond==CondEnsure && m.type.ret != Type.tpVoid ) Code.addVar(m.getRetVar());
				body.generate(Type.tpVoid);
				if( cond==CondEnsure && m.type.ret != Type.tpVoid ) Code.removeVar(m.getRetVar());
				if( m.params.length > 0 ) Code.removeVars(m.params);
				Code.generateCode(this);
			} catch(Exception e) {
				Kiev.reportError(pos,e);
			} finally {
				PassInfo.pop(this);
				Code.generation = false;
				Code.cond_generation = false;
			}
		} else {
			code.generate();
		}
	}

	public boolean setBody(Statement body) {
		this.body = body;
		this.body.parent = this;
		return true;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(body);
	}
}
