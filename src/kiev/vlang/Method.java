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

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class Method extends DNode implements Named,Typed,ScopeOfNames,ScopeOfMethods,SetBody,Accessable,PreScanneable {
	public static Method[]	emptyArray = new Method[0];

	/** Method's access */
	@virtual
	public virtual Access				acc;

	/** Name of the method */
	public NodeName						name;

	/** Return type of the method and signature (argument's types) */
	@att public final TypeCallRef		type_ref;

	/** The type of the dispatcher method (if method is a multimethod) */
	@att public final TypeCallRef		dtype_ref;

	/** Parameters of this method */
	@att public final NArr<FormPar>		params;

    @att public final NArr<ASTAlias>	aliases;

	/** Return value of this method */
	@att public Var						retvar;

	/** Body of the method */
	@att public BlockStat				body;
	@att public PrescannedBody 			pbody;

	/** Array of attributes of this method
	 */
	public Attr[]						attrs = Attr.emptyArray;

	/** Require & ensure clauses */
	@att public final NArr<WBCCondition> conditions;

	/** Violated by method fields for normal methods, and checked fields
	 *  for invariant method
	 */
	@ref public final NArr<Field>		violated_fields;
	
	/** Default meta-value for annotation methods */
	@att public MetaValue				annotation_default;

	/** Indicates that this method is inlined by dispatcher method
	 */
	public boolean						inlined_by_dispatcher;
	
	@att protected FormPar				this_var;

	private boolean		invalid_types;
	
	@virtual public virtual abstract access:ro MethodType type; 
	@virtual public virtual abstract access:ro MethodType jtype; 
	@virtual public virtual abstract access:ro MethodType dtype; 
	
	public Method() {
	}

	public Method(KString name, MethodType mt, int fl) {
		this(name,new TypeCallRef(mt),new TypeCallRef(mt),fl);
	}

	public Method(KString name, MethodType mt, MethodType dmt, int fl) {
		this(name,new TypeCallRef(mt),new TypeCallRef(dmt),fl);
	}
	public Method(KString name, TypeCallRef type_ref, TypeCallRef dtype_ref, int fl) {
		super(0,fl);
		assert ((name != nameInit && name != nameClassInit) || this instanceof Constructor);
		this.name = new NodeName(name);
		this.type_ref = type_ref;
		if (dtype_ref != null) {
			this.dtype_ref = dtype_ref;
		} else {
			this.dtype_ref = (TypeCallRef)type_ref.copy();
		}
		this.acc = new Access(0);
		this.meta = new MetaSet(this);
	}

	@getter public Access get$acc() {
		return acc;
	}

	@setter public void set$acc(Access a) {
		acc = a;
		acc.verifyAccessDecl(this);
	}
	
	public MetaThrows getMetaThrows() {
		return (MetaThrows)this.meta.get(MetaThrows.NAME);
	}

	private void rebuildTypes() {
		type_ref.args.delAll();
		dtype_ref.args.delAll();
		foreach (FormPar fp; params) {
			type_ref.args.add((TypeRef)fp.vtype.copy());
			if (fp.stype != null)
				dtype_ref.args.add((TypeRef)fp.stype.copy());
//			else if (fp.type.isPizzaCase())
//				dtype_ref.args.add(new TypeRef(fp.vtype.getSuperType()));
			else
				dtype_ref.args.add((TypeRef)fp.vtype.copy());
		}
		invalid_types = false;
	}
	
	@getter public MethodType get$type()	{
		if (invalid_types) rebuildTypes();
		return type_ref.getMType();
	} 
	@getter public MethodType get$jtype()	{
		if (invalid_types) rebuildTypes();
		return (MethodType)dtype.getJavaType();
	}
	@getter public MethodType get$dtype()	{
		if (invalid_types) rebuildTypes();
		if (dtype_ref == null)
			dtype_ref = new TypeCallRef(type_ref.getMType());
		return dtype_ref.getMType();
	}
	
	public FormPar getThisPar() {
		if (isStatic()) {
			this_var = null;
		}
		else if (this_var == null) {
			ASTNode p = parent;
			while !(p instanceof Struct) p = p.parent;
			this_var = new FormPar(pos,Constants.nameThis,((Struct)p).type,ACC_FORWARD|ACC_FINAL);
		}
		return this_var;
	}

	public void callbackChildChanged(AttrSlot attr) {
		if (parent != null && pslot != null) {
			if      (attr.name == "params") {
				parent.callbackChildChanged(pslot);
			}
			else if (attr.name == "conditions")
				parent.callbackChildChanged(pslot);
			else if (attr.name == "annotation_default")
				parent.callbackChildChanged(pslot);
		}
		if (attr.name == "params")
			invalid_types = true;
	}
	
	public void addViolatedField(Field f) {
		if( isInvariantMethod() ) {
			f.invs = (Method[])Arrays.appendUniq(f.invs,this);
			if( ((Struct)parent).instanceOf((Struct)f.parent) )
				violated_fields.addUniq(f);
		} else {
			violated_fields.addUniq(f);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(name+"(");
		int n = type_ref.args.length;
		for(int i=0; i < n; i++) {
			sb.append(type_ref.args[i].toString());
			if( i < (n-1) )
				sb.append(",");
		}
		sb.append(")->").append(type_ref.ret);
		return sb.toString();
	}

	public static String toString(KString nm, NArr<ENode> args) {
		return toString(nm,args.toArray(),null);
	}

	public static String toString(KString nm, ENode[] args) {
		return toString(nm,args,null);
	}

	public static String toString(KString nm, NArr<ENode> args, Type ret) {
		return toString(nm,args.toArray(),ret);
	}
	
	public static String toString(KString nm, ENode[] args, Type ret) {
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

	public static String toString(KString nm, MethodType mt) {
		Type[] args = mt.args;
		StringBuffer sb = new StringBuffer(nm+"(");
		for(int i=0; i < args.length; i++) {
			sb.append(args[i].toString());
			if( i < (args.length-1) ) sb.append(",");
		}
		sb.append(")->").append(mt.ret);
		return sb.toString();
	}

	public NodeName getName() { return name; }

	public Type	getType() { return type_ref.getMType(); }

	public Var	getRetVar() {
		if( retvar == null )
			retvar = new Var(pos,nameResultVar,type_ref.ret.getType(),ACC_FINAL);
		return retvar;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(name);
	}

	public void makeArgs(NArr<ENode> args, Type t) {
		if( isVarArgs() ) {
			int j;
			for(j=0; j < type.args.length-1; j++)
				CastExpr.autoCast(args[j],Type.getRealType(t,type.args[j]));
			NArr<ENode> varargs = new NArr<ENode>();
			while(j < args.length) {
				CastExpr.autoCastToReference(args[j]);
				varargs.append(args[j]);
				args.del(j);
			}
			NewInitializedArrayExpr nae =
				new NewInitializedArrayExpr(getPos(),new TypeRef(Type.tpObject),1,varargs.toArray());
			args.append(nae);
		} else {
			int i = 0;
			int j = 0;
			if( this instanceof RuleMethod ) j++;
			for(; i < args.length; i++, j++)
				CastExpr.autoCast(args[i],Type.getRealType(t,type.args[j]));
		}
	}

	public boolean equalsByCast(KString name, MethodType mt, Type tp, ResInfo info) {
		if( this.name.equals(name) )
			return compare(name,mt,tp,info,false);
		return false;
	}
	
	public boolean compare(KString name, MethodType mt, Type tp, ResInfo info, boolean exact) {
		if( !this.name.equals(name) ) return false;
		int type_len = this.type.args.length;
		int args_len = mt.args.length;
		if( type_len != args_len ) {
			if( !isVarArgs() ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in number of params: "+type_len+" != "+args_len);
				return false;
			} else if( type_len-1 > args_len ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" not match in number of params: "+type_len+" != "+args_len);
				return false;
			}
		}
		trace(Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,mt));
		MethodType rt = (MethodType)Type.getRealType(tp,this.type);
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if( exact && !mt.args[i].equals(rt.args[i]) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+rt.args[i]+" != "+mt.args[i]);
				return false;
			}
			else if( !exact && !mt.args[i].isAutoCastableTo(rt.args[i]) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+mt.args[i]+" not auto-castable to "+rt.args[i]);
				return false;
			}
		}
		boolean match = false;
		if( mt.ret == Type.tpAny )
			match = true;
		else if( exact &&  rt.ret.equals(mt.ret) )
			match = true;
		else if( !exact && rt.ret.isAutoCastableTo(mt.ret) )
			match = true;
		else
			match = false;
		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+(match?" match":" do not match"));
		if (info != null && match)
			info.mt = rt;
		return match;
	}

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
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( !name.equals(nameInit) )
			dmp.space().append(type.ret).forsed_space().append(name);
		else
			dmp.space().append(((Struct)parent).name.short_name);
		dmp.append('(');
		for(int i=0; i < params.length; i++) {
			if (params[i].isFinal()) dmp.append("final").forsed_space();
			if (params[i].isForward()) dmp.append("forward").forsed_space();
			params[i].toJavaDecl(dmp,type_ref.args[i].getType());
			if( i < (params.length-1) ) dmp.append(",");
		}
		dmp.append(')').space();
		foreach(WBCCondition cond; conditions) 
			cond.toJava(dmp);
		if( isAbstract() || body == null ) {
			dmp.append(';').newLine();
		} else {
			dmp.append(body).newLine();
		}
		return dmp;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
		FormPar@ var;
		Type@ t;
	{
		inlined_by_dispatcher,$cut,false
	;
		!this.isStatic(),
		name.equals(nameThis),
		node ?= getThisPar()
	;
		var @= params,
		var.name.equals(name),
		node ?= var
//	;
//		t @= type.fargs,
//		t.getClazzName().short_name.equals(name),
//		node ?= new TypeRef(t)
	;
		node ?= retvar, ((Var)node).name.equals(name)
	;
		!this.isStatic(),
		var ?= getThisPar(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	;
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		Var@ n;
	{
		!this.isStatic(),
		n ?= getThisPar(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
	;
		n @= params,
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
	}

    public ASTNode pass3() {
		if !( parent instanceof Struct )
			throw new CompilerException(pos,"Method must be declared on class level only");
		Struct clazz = (Struct)parent;
		// TODO: check flags for methods
		if( clazz.isPackage() ) setStatic(true);
		if( (flags & ACC_PRIVATE) != 0 ) setFinal(false);
		else if( clazz.isClazz() && clazz.isFinal() ) setFinal(true);
		else if( clazz.isInterface() ) {
			setPublic(true);
			if( pbody == null ) setAbstract(true);
		}

//		if (argtypes.length > 0) {
//			ftypes = new Type[argtypes.length];
//			for (int i=0; i < argtypes.length; i++)
//				ftypes[i] = argtypes[i].getType();
//		}

		if (clazz.isAnnotation() && params.length > 0) {
			Kiev.reportError(pos, "Annotation methods may not have arguments");
			params.delAll();
			setVarArgs(false);
		}

		if (clazz.isAnnotation() && (body != null || pbody != null)) {
			Kiev.reportError(pos, "Annotation methods may not have bodies");
			body = null;
			pbody = null;
		}

		// push the method, because formal parameters may refer method's type args
		PassInfo.push(this);
		try {
			foreach (FormPar fp; params) {
				fp.vtype.getType(); // resolve
				if (fp.meta != null)
					fp.meta.verify();
			}
		} finally {
			PassInfo.pop(this);
		}
		if( isVarArgs() ) {
			FormPar va = new FormPar(pos,nameVarArgs,Type.newArrayType(Type.tpObject),0);
			params.append(va);
		}
		type_ref.getMType(); // resolve
		dtype_ref.getMType(); // resolve
		trace(Kiev.debugMultiMethod,"Method "+this+" has dispatcher type "+this.dtype);
		meta.verify();
		if (annotation_default != null)
			annotation_default.verify();
		foreach(ASTAlias al; aliases) al.attach(this);
		MetaThrows throwns = getMetaThrows();
        if( throwns != null ) {
			ASTNode[] mthrs = throwns.getThrowns();
        	Type[] thrs = new Type[mthrs.length];
			for (int i=0; i < mthrs.length; i++)
				thrs[i] = mthrs[i].getType();
        	ExceptionsAttr athr = new ExceptionsAttr();
        	athr.exceptions = thrs;
			this.addAttr(athr);
        }

		foreach(WBCCondition cond; conditions)
			cond.definer = this;

        return this;
    }

	public void resolveMetaDefaults() {
		if (annotation_default != null) {
			Type tp = this.type_ref.getMType().ret;
			Type t = tp;
			if (t.isArray()) {
				if (annotation_default instanceof MetaValueScalar) {
					MetaValueArray mva = new MetaValueArray(annotation_default.type);
					mva.values.add(((MetaValueScalar)annotation_default).value);
					annotation_default = mva;
				}
				t = t.args[0];
			}
			if (t.isReference()) {
				t.checkResolved();
				if (!(t == Type.tpString || t == Type.tpClass || t.isAnnotation() || t.isJavaEnum()))
					throw new CompilerException(pos, "Bad annotation value type "+tp);
			}
			annotation_default.resolve(t);
		}
	}
	
	public DataFlow getDFlow() {
		DataFlow df = (DataFlow)getNodeData(DataFlow.ID);
		if (df == null) {
			df = new DataFlow(this);
			DFState in = DFState.makeNewState();
			if (!isStatic()) {
				Var p = getThisPar();
				in = in.declNode(p);
			}
			for(int i=0; i < params.length; i++) {
				Var p = params[i];
				in = in.declNode(p);
			}
			df.in = in;
			df.out = DFState.makeNewState();
		}
		return df;
	}
	
	public DFState getDFlowIn() {
		DataFlow df = getDFlow();
		return df.in;
	}
	
	public DFState getDFlowOut() {
		DataFlow df = getDFlow();
		return df.out;
	}

	public DFState getDFlowIn(ASTNode child) {
		String name = child.pslot.name;
		if (name == "body")
			return getDFlowIn();
		if (name == "conditions") {
			WBCCondition cond = (WBCCondition)child;
			if (cond.cond == WBCType.CondRequire)
				return getDFlowIn();
			else if (cond.cond == WBCType.CondEnsure)
				return body.getDFlowOut();
			else if (cond.cond == WBCType.CondInvariant)
				return getDFlowOut();
		}
		throw new CompilerException(pos,"Internal error: getDFlowIn("+name+")");
	}
	
	public void resolveDecl() {
		if( isResolved() ) return;
		trace(Kiev.debugResolve,"Resolving method "+this);
		assert( PassInfo.clazz == parent || inlined_by_dispatcher );
		PassInfo.push(this);
		try {
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondRequire ) {
				cond.body.resolve(Type.tpVoid);
			}
			if( body != null ) {
				if (type.ret == Type.tpVoid)
					body.setAutoReturnable(true);
				body.resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret == Type.tpVoid ) {
					if( body instanceof BlockStat ) {
						((BlockStat)body).stats.append(new ReturnStat(pos,body,null));
						body.setAbrupted(true);
					}
					else if !(isInvariantMethod())
						Kiev.reportError(pos,"Return requared");
				} else {
					Kiev.reportError(pos,"Return requared");
				}
			}
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondEnsure ) {
				if( type.ret != Type.tpVoid ) getRetVar();
				cond.resolve(Type.tpVoid);
			}
		} catch(Exception e ) {
			Kiev.reportError(0/*body.getPos()*/,e);
		} finally {
			PassInfo.pop(this);
		}

		setResolved(true);
	}

	public void generate() {
		if( Kiev.debug ) System.out.println("\tgenerating Method "+this);
		PassInfo.push(this);
		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
			foreach(Field f; violated_fields; PassInfo.clazz.instanceOf((Struct)f.parent) ) {
				foreach(Method inv; f.invs; PassInfo.clazz.instanceOf((Struct)inv.parent) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(name.name.startsWith(nameSet) || name.name.startsWith(nameGet)) )
						conditions.addUniq(inv.conditions[0]);
				}
			}
		}
		try {
			foreach(WBCCondition cond; conditions; cond.cond != WBCType.CondInvariant )
				cond.generate(Type.tpVoid);
		} finally { kiev.vlang.PassInfo.pop(this); kiev.vlang.Code.generation = false; }
		if( !isAbstract() && body != null ) {
			Code.reInit();
			Code.generation = true;
			PassInfo.push(this);
			try {
				if( !isBad() ) {
					if( !isStatic() ) Code.addVar(getThisPar());
					if( params.length > 0 ) Code.addVars(params.toArray());
					if( Kiev.verify /*&& jtype != null*/ )
						generateArgumentCheck();
					if( Kiev.debugOutputC ) {
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondRequire )
							Code.importCode(cond.code);
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondInvariant ) {
							assert( cond.parent instanceof Method && cond.parent.isInvariantMethod() );
							if( !name.name.equals(nameInit) && !name.name.equals(nameClassInit) ) {
								if( !cond.parent.isStatic() )
									Code.addInstr(Instr.op_load,getThisPar());
								Code.addInstr(Instr.op_call,(Method)cond.parent,false);
							}
							setGenPostCond(true);
						}
						if( !isGenPostCond() ) {
							foreach(WBCCondition cond; conditions; cond.cond != WBCType.CondRequire ) {
								setGenPostCond(true);
								break;
							}
						}
					}
					body.generate(Type.tpVoid);
					if( Kiev.debugOutputC && isGenPostCond() ) {
						if( type.ret != Type.tpVoid ) {
							Code.addVar(getRetVar());
							Code.addInstr(Instr.op_store,getRetVar());
						}
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondInvariant ) {
							if( !cond.parent.isStatic() )
								Code.addInstr(Instr.op_load,getThisPar());
							Code.addInstr(Instr.op_call,(Method)cond.parent,false);
							setGenPostCond(true);
						}
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondEnsure )
							Code.importCode(cond.code);
						if( type.ret != Type.tpVoid ) {
							Code.addInstr(Instr.op_load,getRetVar());
							Code.addInstr(Instr.op_return);
							Code.removeVar(getRetVar());
						} else {
							Code.addInstr(Instr.op_return);
						}
					}
					if( params.length > 0 ) Code.removeVars(params.toArray());
					if( !isStatic() ) Code.removeVar(getThisPar());
				} else {
					Code.addInstr(Instr.op_new,Type.tpError);
					Code.addInstr(Instr.op_dup);
					KString msg = KString.from("Compiled with errors");
					ConstPool.addStringCP(msg);
					Code.addConst(msg);
					Method func = Type.tpError.resolveMethod(nameInit,KString.from("(Ljava/lang/String;)V"));
					Code.addInstr(Instr.op_call,func,false);
					Code.addInstr(Instr.op_throw);
				}
				Code.generateCode();
			} catch(Exception e) {
				Kiev.reportError(pos,e);
			} finally { kiev.vlang.PassInfo.pop(this); kiev.vlang.Code.generation = false; }
		}
	}

	public CodeLabel getBreakLabel() {
		return ((BlockStat)body).getBreakLabel();
	}

	public void generateArgumentCheck() {
//		if( jtype == null ) return;
		int i=0;
		for(; i < type.args.length; i++) {
			Type tp1 = jtype.args[i];
			Type tp2 = params[i].type;
			if( !tp1.equals(tp2) ) {
				Code.addInstr(Instr.op_load,params[i]);
				Code.addInstr(Instr.op_checkcast,type_ref.args[i].getType());
				Code.addInstr(Instr.op_store,params[i]);
			}
		}
	}

	public boolean setBody(Statement body) {
		trace(Kiev.debugMultiMethod,"Setting body of methods "+this);
		if (this.body == null) {
			this.body = (BlockStat)body;
		}
//		else if (isMultiMethod()){
//			BlockStat b = (BlockStat)this.body;
//			b.addStatement(body);
//		}
		else {
			throw new RuntimeException("Added body to method "+this+" which already have body");
		}

		return true;
	}

}

@node
public class Constructor extends Method {
	@att public final NArr<Statement>	addstats;

	public Constructor() {
	}

	public Constructor(MethodType mt, int fl) {
		super((fl&ACC_STATIC)==0 ? nameInit:nameClassInit, mt, fl);
	}

	public Constructor(TypeCallRef type_ref, int fl) {
		super((fl&ACC_STATIC)==0 ? nameInit:nameClassInit, type_ref, (TypeCallRef)type_ref.copy(), fl);
	}

	public void resolveDecl() {
		super.resolveDecl();
		for(int i=0; i < addstats.length; i++) {
			body.stats.insert(addstats[i],i);
			trace(Kiev.debugResolve,"Statement added to constructor: "+addstats[i]);
		}
		addstats.delAll();
	}
}

@node
public class Initializer extends DNode implements SetBody, PreScanneable {
	@att public BlockStat				body;
	@att public PrescannedBody			pbody;

	public Initializer() {
	}

	public Initializer(int pos, int flags) {
		super(pos, null);
		setFlags(flags);
	}

	public void resolveDecl() {
		if( isResolved() ) return;
		
		PassInfo.push(this);
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(0,e);
		} finally {
			PassInfo.pop(this);
		}

		setResolved(true);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Initializer");
		PassInfo.push(this);
		try {
			body.generate(reqType);
		} finally { PassInfo.pop(this); }
	}

	public boolean setBody(Statement body) {
		trace(Kiev.debugMultiMethod,"Setting body of initializer "+this);
		if (this.body == null) {
			this.body = (BlockStat)body;
		}
		else {
			throw new RuntimeException("Added body to initializer "+this+" which already has body");
		}
		return true;
	}

}

public enum WBCType {
	public CondUnknown,
	public CondRequire,
	public CondEnsure,
	public CondInvariant;
}

@node
public class WBCCondition extends DNode {

	public WBCType					cond;
	@att public ASTIdentifier		name;
	@att public Statement			body;
	public CodeAttr					code;
	@ref public Method				definer;

	public WBCCondition() {
	}

	public WBCCondition(int pos, WBCType cond, KString name, Statement body) {
		super(pos,null);
		if (name != null)
			this.name = new ASTIdentifier(pos, name);
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
		if( code != null ) return;
		body.resolve(Type.tpVoid);
	}

	public void generate(Type reqType) {
		if( cond == WBCType.CondInvariant ) {
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
				if( !m.isStatic() ) Code.addVar(m.getThisPar());
				if( m.params.length > 0 ) Code.addVars(m.params.toArray());
				if( cond==WBCType.CondEnsure && m.type.ret != Type.tpVoid ) Code.addVar(m.getRetVar());
				body.generate(Type.tpVoid);
				if( cond==WBCType.CondEnsure && m.type.ret != Type.tpVoid ) Code.removeVar(m.getRetVar());
				if( m.params.length > 0 ) Code.removeVars(m.params.toArray());
				if( !m.isStatic() ) Code.removeVar(m.getThisPar());
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
		return true;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(body);
	}
}

