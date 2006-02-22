package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.Field.FieldImpl;
import kiev.vlang.Field.FieldView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static final view RField of FieldImpl extends FieldView {
	public void resolveDecl() {
		foreach (Meta m; meta)
			m.resolve();
		Type tp = this.type;
		if (init instanceof TypeRef)
			((TypeRef)init).toExpr(type);
		if (tp instanceof CTimeType) {
			init = tp.makeInitExpr(((FieldImpl)this)._self,init);
			try {
				init.resolve(tp.getEnclosedType());
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		else if( init != null ) {
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
		setResolved(true);
	}
}

