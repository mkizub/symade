package kiev.ir.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.be.java15.CodeAttr;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public view RMethod of Method extends RDNode {

	public				Access				acc;
	public:ro			TypeDef[]			targs;
	public				TypeRef				type_ret;
	public				TypeRef				dtype_ret;
	public:ro			CallType			type;
	public:ro			CallType			dtype;
	public:ro			CallType			etype;
	public:ro			FormPar[]			params;
	public:ro			ASTAlias[]			aliases;
	public				ENode				body;
	public:ro			WBCCondition[]		conditions;

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
	// method with attached operator	
	public final boolean isOperatorMethod();
	public final void setOperatorMethod(boolean on);
	// need fields initialization	
	public final boolean isNeedFieldInits();
	public final void setNeedFieldInits(boolean on);
	// a method generated as invariant	
	public final boolean isInvariantMethod();
	public final void setInvariantMethod(boolean on);
	// a local method (closure code or inner method)	
	public final boolean isLocalMethod();
	public final void setLocalMethod(boolean on);
	// a dispatcher (for multimethods)	
	public final boolean isDispatcherMethod();
	public final void setDispatcherMethod(boolean on);
	public final boolean isInlinedByDispatcherMethod();

	public void resolveDecl() {
		RMethod.resolveMethod(this);
	}
	static void resolveMethod(@forward RMethod self) {
		if( isResolved() ) return;
		trace(Kiev.debugResolve,"Resolving method "+self);
		assert( ctx_tdecl == parent() || isInlinedByDispatcherMethod() );
		Method.ATTR_VIOLATED_FIELDS.clear((Method)self);
		try {
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondRequire ) {
				cond.body.resolve(Type.tpVoid);
			}
			if (body != null && !(body instanceof MetaValue)) {
				body.setAutoReturnable(true);
				body.resolve(type.ret());
				if (!body.isMethodAbrupted()) {
					if (type.ret() ≡ Type.tpVoid) {
						block.stats.append(new ReturnStat(pos,null));
						body.setMethodAbrupted(true);
					} else {
						Kiev.reportError(self,"Return requared");
					}
				}
			}
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondEnsure ) {
				if( type.ret() ≢ Type.tpVoid ) getRetVar();
				cond.resolveDecl();
			}
		} catch(Exception e ) {
			Kiev.reportError(self,e);
		}
		((Method)self).cleanDFlow();

		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
			Field[] violated_fields = Method.ATTR_VIOLATED_FIELDS.get((Method)self);
			foreach(Field f; violated_fields; ctx_tdecl.instanceOf(f.ctx_tdecl) ) {
				Method[] invs = Field.ATTR_INVARIANT_CHECKERS.get(f);
				foreach(Method inv; invs; ctx_tdecl.instanceOf(inv.ctx_tdecl) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(id.sname.startsWith(nameSet) || id.sname.startsWith(nameGet)) ) {
						if (((Method)self).conditions.indexOf(inv.conditions[0]) < 0)
							((Method)self).conditions.add(inv.conditions[0]);
					}
				}
			}
			Method.ATTR_VIOLATED_FIELDS.clear((Method)self);
		}
		
		setResolved(true);
	}
}

@nodeview
public final view RConstructor of Constructor extends RMethod {

	public void resolveDecl() {
		RMethod.resolveMethod(this); // super.resolveDecl()
		ENode[] addstats = ((Constructor)this).addstats.delToArray();
		for(int i=0; i < addstats.length; i++) {
			block.stats.insert(i,addstats[i]);
			trace(Kiev.debugResolve,"ENode added to constructor: "+addstats[i]);
		}
	}
}

@nodeview
public final view RInitializer of Initializer extends RDNode {
	public:ro ENode			body;
	public:ro Block			block;


	public void resolveDecl() {
		if( isResolved() ) return;
		
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}

		setResolved(true);
	}
}

@nodeview
public final view RWBCCondition of WBCCondition extends RDNode {
	public WBCType				cond;
	public ENode				body;
	public Method				definer;
	public CodeAttr				code_attr;

	public void resolveDecl() {
		if( code_attr != null ) return;
		body.resolve(Type.tpVoid);
	}
}

