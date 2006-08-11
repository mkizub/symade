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
public static final view RField of Field extends RLvalDNode {
	public		TypeRef			ftype;
	public		ENode			init;
	public		ConstExpr		const_value;
	
	@getter public final Type	get$type();
	
	// packer field (auto-generated for packed fields)
	public final boolean isPackerField();
	public final void setPackerField(boolean on);
	// packed field
	public final boolean isPackedField();
	public final void setPackedField(boolean on);
	// field's initializer was already added to class initializer
	public final boolean isAddedToInit();
	public final void setAddedToInit(boolean on);

	public void resolveDecl() {
		meta.resolve();
		Type tp = this.type;
		if (init instanceof TypeRef)
			((TypeRef)init).toExpr(type);
		if (tp instanceof CTimeType) {
			this.open();
			init = tp.makeInitExpr(this,init);
			try {
				Kiev.runProcessorsOn(init);
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
					this.open();
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

