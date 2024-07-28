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
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ViewOf(vcast=true)
public view RMethod of Method extends RDNode {

	public:ro			TypeDef[]			targs;
	public				TypeRef				type_ret;
	public:ro			CallType			mtype;
	public:ro			CallType			dtype;
	public:ro			CallType			etype;
	public:ro			Var[]				params;
	public				ENode				body;

	public:ro			Block				block;

	public Var getRetVar();
	public MetaThrows getMetaThrows();
	
	// virtual static method
	public final boolean isVirtualStatic();
	public final void setVirtualStatic(boolean on);
	// method with variable number of arguments	
	public final boolean isVarArgs();
	public final void setVarArgs(boolean on);
	// logic rule method
	public final boolean isRuleMethod();
	// need fields initialization	
	public final boolean isNeedFieldInits();
	public final void setNeedFieldInits(boolean on);
	// a method generated as invariant	
	public final boolean isInvariantMethod();
	public final void setInvariantMethod(boolean on);
	// a dispatcher (for multimethods)	
	public final boolean isDispatcherMethod();
	public final void setDispatcherMethod(boolean on);
	public final boolean isInlinedByDispatcherMethod();
	
	public Enumeration<WBCCondition> conditions() {
		return (Enumeration<WBCCondition>)((Method)this).conditions.elements();
	}

	public void resolveDecl(Env env) {
		RMethod.resolveMethod(this,env);
	}
	static void resolveMethod(@forward RMethod self, Env env) {
		if( isResolved() ) return;
		trace(Kiev.debug && Kiev.debugResolve,"Resolving method "+self);
		assert( ctx_tdecl == parent() || isInlinedByDispatcherMethod() );
		//Method.ATTR_VIOLATED_FIELDS.clear((Method)self);
		try {
			foreach(WBCCondition cond; conditions(); cond.cond == WBCType.CondRequire ) {
				resolveENode(cond.body,env.tenv.tpVoid,env);
			}
			if (body != null && !isMacro() && !(body instanceof MetaValue)) {
				body.setAutoReturnable(true);
				resolveENode(body,mtype.ret(),env);
				if (!body.isMethodAbrupted()) {
					if (mtype.ret() ≡ env.tenv.tpVoid) {
						block.stats.append(new ReturnStat(pos,null));
						body.setMethodAbrupted(true);
					} else {
						Kiev.reportError(self,"Return requared");
					}
				}
			}
			foreach(WBCCondition cond; conditions(); cond.cond == WBCType.CondEnsure ) {
				if( mtype.ret() ≢ env.tenv.tpVoid ) getRetVar();
				resolveDNode(cond,env);
			}
		} catch(Exception e ) {
			Kiev.reportError(self,e);
		}

		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
/*			Field[] violated_fields = Method.ATTR_VIOLATED_FIELDS.get((Method)self);
			foreach(Field f; violated_fields; ctx_tdecl.instanceOf(f.ctx_tdecl) ) {
				Method[] invs = Field.ATTR_INVARIANT_CHECKERS.get(f);
				foreach(Method inv; invs; ctx_tdecl.instanceOf(inv.ctx_tdecl) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(sname.startsWith(nameSet) || sname.startsWith(nameGet)) ) {
						if (((Method)self).conditions.indexOf(inv.conditions[0]) < 0)
							((Method)self).conditions.add(inv.conditions[0]);
					}
				}
			}
			Method.ATTR_VIOLATED_FIELDS.clear((Method)self);
*/		}
		
		setResolved(true);
	}
}

@ViewOf(vcast=true)
public final view RConstructor of Constructor extends RMethod {

	public void resolveDecl(Env env) {
		RMethod.resolveMethod(this,env); // super.resolveDecl(env)
		ENode[] addstats = ((Constructor)this).addstats.delToArray();
		for(int i=0; i < addstats.length; i++) {
			block.stats.insert(i,addstats[i]);
			trace(Kiev.debug && Kiev.debugResolve,"ENode added to constructor: "+addstats[i]);
		}
		for(int i=0; i < addstats.length; i++)
			resolveENode(addstats[i],env.tenv.tpVoid,env);
	}
}

@ViewOf(vcast=true)
public final view RInitializer of Initializer extends RDNode {
	public:ro ENode			body;
	public:ro Block			block;


	public void resolveDecl(Env env) {
		if( isResolved() ) return;
		
		try {
			resolveENode(body,env.tenv.tpVoid,env);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}

		setResolved(true);
	}
}

@ViewOf(vcast=true)
public final view RWBCCondition of WBCCondition extends RDNode {
	public WBCType				cond;
	public ENode				body;
	public Method				definer;

	public void resolveDecl(Env env) {
		//if (code_attr != null) return;
		if (body != null)
			resolveENode(body,env.tenv.tpVoid,env);
	}
}

