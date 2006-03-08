package kiev.ir.java;

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
	public		Access			acc;
	public		NodeName		name;
	public		TypeRef			ftype;
	public		ENode			init;
	public		ConstExpr		const_value;
	public:ro	NArr<Method>	invs;
	
	@getter public final Type	get$type();
	
	// is a field of enum
	public final boolean isEnumField();
	public final void setEnumField(boolean on);
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
		foreach (Meta m; meta)
			m.resolve();
		Type tp = this.type;
		if (init instanceof TypeRef)
			((TypeRef)init).toExpr(type);
		if (tp instanceof CTimeType) {
			init = tp.makeInitExpr(this,init);
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

