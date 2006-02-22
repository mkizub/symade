package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.Var.VarImpl;
import kiev.vlang.Var.VarView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static final view RVar of VarImpl extends VarView {
	public void resolveDecl() {
		if( isResolved() ) return;
		Type tp = this.type;
		if (init instanceof TypeRef)
			((TypeRef)init).toExpr(tp);
		if (tp instanceof CTimeType) {
			init = tp.makeInitExpr(((VarImpl)this)._self,init);
			try {
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

