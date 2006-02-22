package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.ConstExpr.ConstExprImpl;
import kiev.vlang.ConstExpr.ConstExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RConstExpr of ConstExprImpl extends ConstExprView {
	
	public final void resolve(Type reqType) {
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

