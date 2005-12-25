/* Generated By:JJTree: Do not edit this line. ASTAnonymouseClosure.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.transf.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ASTAnonymouseClosure extends ENode implements ScopeOfNames {
	
	@dflow(out="this:in") private static class DFI {}
	
	@node
	public static class ASTAnonymouseClosureImpl extends ENodeImpl {
		@att public NArr<FormPar>				params;
		@att public TypeRef						rettype;
		@att public BlockStat					body;
		@att public NewClosure					new_closure;
		@att public Struct						clazz;
		@ref public ClosureType					ctype;
		public ASTAnonymouseClosureImpl() {}
		public ASTAnonymouseClosureImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ASTAnonymouseClosureView of ASTAnonymouseClosureImpl extends ENodeView {
		public access:ro	NArr<FormPar>				params;
		public				TypeRef						rettype;
		public				BlockStat					body;
		public				NewClosure					new_closure;
		public				Struct						clazz;
		public				ClosureType					ctype;
	}
	
	@att public abstract virtual access:ro	NArr<FormPar>				params;
	@att public abstract virtual			TypeRef						rettype;
	@att public abstract virtual			BlockStat					body;
	@att public abstract virtual			NewClosure					new_closure;
	@att public abstract virtual			Struct						clazz;
	@ref public abstract virtual			ClosureType					ctype;
	
	@getter public NArr<FormPar>	get$params()			{ return this.getASTAnonymouseClosureView().params; }
	@getter public TypeRef			get$rettype()			{ return this.getASTAnonymouseClosureView().rettype; }
	@getter public BlockStat		get$body()				{ return this.getASTAnonymouseClosureView().body; }
	@getter public NewClosure		get$new_closure()		{ return this.getASTAnonymouseClosureView().new_closure; }
	@getter public Struct			get$clazz()				{ return this.getASTAnonymouseClosureView().clazz; }
	@getter public ClosureType		get$ctype()				{ return this.getASTAnonymouseClosureView().ctype; }
	
	@setter public void		set$rettype(TypeRef val)		{ this.getASTAnonymouseClosureView().rettype = val; }
	@setter public void		set$body(BlockStat val)			{ this.getASTAnonymouseClosureView().body = val; }
	@setter public void		set$new_closure(NewClosure val)	{ this.getASTAnonymouseClosureView().new_closure = val; }
	@setter public void		set$clazz(Struct val)			{ this.getASTAnonymouseClosureView().clazz = val; }
	@setter public void		set$ctype(ClosureType val)		{ this.getASTAnonymouseClosureView().ctype = val; }

	public NodeView						getNodeView()					{ return new ASTAnonymouseClosureView((ASTAnonymouseClosureImpl)this.$v_impl); }
	public ENodeView					getENodeView()					{ return new ASTAnonymouseClosureView((ASTAnonymouseClosureImpl)this.$v_impl); }
	public ASTAnonymouseClosureView		getASTAnonymouseClosureView()	{ return new ASTAnonymouseClosureView((ASTAnonymouseClosureImpl)this.$v_impl); }
	
	public ASTAnonymouseClosure() {
		super(new ASTAnonymouseClosureImpl());
	}
	
  	public void set(Token t) {
    	pos = t.getPos();
	}

	public Type getType() {
		return ctype;
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		Var@ p;
	{
		p @= params,
		p.name.equals(name),
		node ?= p
	}
	
	public boolean preResolveIn(TransfProcessor proc) {
		proc.preResolve(rettype);
		foreach (FormPar fp; params) proc.preResolve(fp);
		return false; // don't pre-resolve me
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		proc.mainResolve(rettype);
		foreach (FormPar fp; params) proc.mainResolve(fp);
	
		ClazzName clname = ClazzName.fromBytecodeName(
			new KStringBuffer(ctx_clazz.name.bytecode_name.len+8)
				.append_fast(ctx_clazz.name.bytecode_name)
				.append_fast((byte)'$')
				.append(ctx_clazz.countAnonymouseInnerStructs())
				.toKString(),
			false
		);
		clazz = Env.newStruct(clname,ctx_clazz,0,true);
		clazz.type = Type.createRefType(clazz,Type.emptyArray);
		clazz.setResolved(true);
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		if( ctx_method==null || ctx_method.isStatic() ) clazz.setStatic(true);
		if( Env.getStruct(Type.tpClosureClazz.name) == null )
			throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
		clazz.super_type = Type.tpClosureClazz.type;

		Type[] types = new Type[params.length];
		for(int i=0; i < types.length; i++)
			types[i] = params[i].type;
		Type ret = rettype.getType();
		this.ctype = ClosureType.newClosureType(types,ret);

		return false; // don't pre-resolve me
	}
	
	public void resolve(Type reqType) {
		if( isResolved() ) {
			replaceWithNode((ENode)~new_closure);
			return;
		}
		BlockStat body = (BlockStat)~this.body;
		Type ret = ctype.ret;
		if( ret != Type.tpRule ) {
			KString call_name;
			if( ret.isReference() ) {
				ret = Type.tpObject;
				call_name = KString.from("call_Object");
			} else {
				call_name = KString.from("call_"+ret);
			}
			Method md = new Method(call_name, MethodType.newMethodType(Type.emptyArray,ret),ACC_PUBLIC);
			md.pos = pos;
			md.body = body;
			clazz.members.add(md);
		} else {
			KString call_name = KString.from("call_rule");
			RuleMethod md = new RuleMethod(call_name, MethodType.newMethodType(Type.emptyArray,Type.tpRule),ACC_PUBLIC);
			md.pos = pos;
			md.body = body;
			clazz.members.add(md);
		}

		FormPar[] params = this.params.delToArray();
		for(int i=0; i < params.length; i++) {
			FormPar v = params[i];
			ENode val = new ContainerAccessExpr(pos,
				new IFldExpr(pos,new ThisExpr(pos),Type.tpClosureClazz.resolveField(nameClosureArgs)),
				new ConstIntExpr(i));
			if( !v.type.isReference() ) {
				BaseType celltp = BaseType.getProxyType(v.type);
				val = new IFldExpr(v.getPos(),
						new CastExpr(v.getPos(),celltp,val,true),
						celltp.clazz.resolveField(nameCellVal)
					);
			} else {
				val = new CastExpr(v.getPos(),v.type,val,true);
			}
			v.init = val;
			body.insertSymbol(v,i);
		}
		setResolved(true);

		Kiev.runProcessorsOn(clazz);
		new_closure = new NewClosure(pos,new TypeClosureRef(ctype), (Struct)~this.clazz);
		replaceWithNodeResolve(reqType, (ENode)~new_closure);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append("fun (");
		for(int i=0; i < params.length; i++) {
			sb.append(params[i]);
			if( i < params.length-1 ) sb.append(',');
		}
		sb.append(") {...}");
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append("fun").space().append('(');
		for(int i=0; i < params.length; i++) {
			params[i].toJava(dmp);
			if( i < params.length-1 ) dmp.append(',');
		}
		dmp.append(')').space();
		body.toJava(dmp);
		return dmp;
	}
}
