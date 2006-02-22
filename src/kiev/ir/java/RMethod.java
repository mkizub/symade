package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.Method.MethodImpl;
import kiev.vlang.Method.MethodView;
import kiev.vlang.Constructor.ConstructorImpl;
import kiev.vlang.Initializer.InitializerImpl;
import kiev.vlang.Initializer.InitializerView;
import kiev.vlang.WBCCondition.WBCConditionImpl;
import kiev.vlang.WBCCondition.WBCConditionView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public view RMethod of MethodImpl extends MethodView {
	public void resolveDecl() {
		if( isResolved() ) return;
		trace(Kiev.debugResolve,"Resolving method "+this);
		assert( ctx_clazz == parent_node || inlined_by_dispatcher );
		try {
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondRequire ) {
				cond.body.resolve(Type.tpVoid);
			}
			if( body != null ) {
				body.setAutoReturnable(true);
				body.resolve(type.ret());
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret() ≡ Type.tpVoid ) {
					body.stats.append(new ReturnStat(pos,null));
					body.setMethodAbrupted(true);
				} else {
					Kiev.reportError(this,"Return requared");
				}
			}
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondEnsure ) {
				if( type.ret() ≢ Type.tpVoid ) getRetVar();
				cond.resolveDecl();
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		this.cleanDFlow();

		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
			foreach(Field f; violated_fields; ctx_clazz.instanceOf(f.ctx_clazz) ) {
				foreach(Method inv; f.invs; ctx_clazz.instanceOf(inv.ctx_clazz) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(name.name.startsWith(nameSet) || name.name.startsWith(nameGet)) )
						conditions.addUniq(inv.conditions[0]);
				}
			}
		}
		
		setResolved(true);
	}
}

@nodeview
public final view RConstructor of ConstructorImpl extends RMethod {
	public:ro	NArr<ENode>			addstats;

	public void resolveDecl() {
		super.resolveDecl();
		ENode[] addstats = this.addstats.delToArray();
		for(int i=0; i < addstats.length; i++) {
			body.stats.insert(addstats[i],i);
			trace(Kiev.debugResolve,"ENode added to constructor: "+addstats[i]);
		}
	}
}

@nodeview
public final view RInitializer of InitializerImpl extends InitializerView {
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
public final view RWBCCondition of WBCConditionImpl extends WBCConditionView {
	public void resolveDecl() {
		if( code_attr != null ) return;
		body.resolve(Type.tpVoid);
	}
}

