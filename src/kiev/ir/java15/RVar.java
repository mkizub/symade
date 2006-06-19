package kiev.ir.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static view RVar of Var extends RLvalDNode {
	public	TypeRef		vtype;
	public	ENode		init;
	public	int			bcpos;

	@getter public final Type get$type();
	
	// is a local var in a rule 
	public final boolean isLocalRuleVar();
	public final void setLocalRuleVar(boolean on);
	// closure proxy
	public final boolean isClosureProxy();
	public final void setClosureProxy(boolean on);
	// "this" var
	public final boolean isVarThis();
	public final void setVarThis(boolean on);
	// "super" var
	public final boolean isVarSuper();
	public final void setVarSuper(boolean on);

	public void resolveDecl() {
		if( isResolved() ) return;
		Type tp = this.type;
		if (init instanceof TypeRef)
			((TypeRef)init).toExpr(tp);
		if (tp instanceof CTimeType) {
			this.open();
			init = tp.makeInitExpr((Var)this,init);
			try {
				Kiev.runProcessorsOn(init);
				init.resolve(tp.getEnclosedType());
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		else if (init != null) {
			try {
				init.resolve(tp);
				Type it = init.getType();
				if( !it.isInstanceOf(tp) ) {
					this.open();
					init = new CastExpr(init.pos,tp,~init);
					init.resolve(tp);
				}
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		getDFlow().out();
		setResolved(true);
	}
}

@nodeview
public static final view RFormPar of FormPar extends RVar {
	public TypeRef		stype;
	public int			kind;

	@getter public final Type get$dtype();

	public void resolveDecl() {
		Type tp = this.type;
		setResolved(true);
	}
}


