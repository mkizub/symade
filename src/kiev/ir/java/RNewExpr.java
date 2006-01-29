package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.NewClosure.NewClosureImpl;
import kiev.vlang.NewClosure.NewClosureView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RNewClosure of NewClosureImpl extends NewClosureView {

	public boolean preGenerate() {
		if (clazz != null)
			return true;
		ClazzName clname = ClazzName.fromBytecodeName(
			new KStringBuffer(ctx_clazz.name.bytecode_name.len+8)
				.append_fast(ctx_clazz.name.bytecode_name)
				.append_fast((byte)'$')
				.append(ctx_clazz.countAnonymouseInnerStructs())
				.toKString(),
			false
		);
		clazz = Env.newStruct(clname,ctx_clazz,0,true);
		clazz.setResolved(true);
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		if( ctx_method==null || ctx_method.isStatic() ) clazz.setStatic(true);
		if( Env.getStruct(Type.tpClosureClazz.name) == null )
			throw new RuntimeException("Core class "+Type.tpClosureClazz.name+" not found");
		clazz.super_type = Type.tpClosureClazz.ctype;
		Kiev.runProcessorsOn(clazz);
		this.getNode().getType();

		// scan the body, and replace ThisExpr with OuterThisExpr
		Struct clz = this.ctx_clazz;
		body.walkTree(new TreeWalker() {
			public void post_exec(ASTNode n) {
				if (n instanceof ThisExpr) n.replaceWithNode(new OuterThisAccessExpr(n.pos, clz));
			}
		});

		BlockStat body = ~this.body;
		Type ret = ctype.ret();
		if( ret ≢ Type.tpRule ) {
			KString call_name;
			if( ret.isReference() ) {
				ret = Type.tpObject;
				call_name = KString.from("call_Object");
			} else {
				call_name = KString.from("call_"+ret);
			}
			Method md = new Method(call_name, ret, ACC_PUBLIC);
			md.pos = pos;
			md.body = body;
			clazz.members.add(md);
		} else {
			KString call_name = KString.from("call_rule");
			RuleMethod md = new RuleMethod(call_name,ACC_PUBLIC);
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
			if( v.type.isReference() )
				val = new CastExpr(v.pos,v.type,val,true);
			else
				val = new CastExpr(v.pos,((CoreType)v.type).getRefTypeForPrimitive(),val,true);
			v.init = val;
			body.insertSymbol(v,i);
			if( !v.type.isReference() )
				 CastExpr.autoCastToPrimitive(val);
		}

		return true;
	}
	
}

