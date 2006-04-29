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
public final view RCatchInfo of CatchInfo extends RENode {
	public Var				arg;
	public ENode			body;

	public void resolve(Type reqType) {
		try {
			body.resolve(Type.tpVoid);
			if( body.isMethodAbrupted() ) setMethodAbrupted(true);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
	}
}

@nodeview
public final view RFinallyInfo of FinallyInfo extends RENode {
	public ENode		body;
	public Var			ret_arg;

	public void resolve(Type reqType) {
		if (ret_arg == null)
			ret_arg = new Var(pos,"",Type.tpObject,0);
		try {
			body.resolve(Type.tpVoid);
			if( body.isMethodAbrupted() ) setMethodAbrupted(true);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
	}
}

@nodeview
public final view RTryStat of TryStat extends RENode {
	public		ENode				body;
	public:ro	NArr<CatchInfo>		catchers;
	public		FinallyInfo			finally_catcher;

	public void resolve(Type reqType) {
		for(int i=0; i < catchers.length; i++) {
			try {
				catchers[i].resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(catchers[i],e);
			}
		}
		if(finally_catcher != null) {
			try {
				finally_catcher.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(finally_catcher,e);
			}
		}
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		// Check if abrupted
		if( finally_catcher!= null && finally_catcher.isMethodAbrupted())
			setMethodAbrupted(true);
		else if( finally_catcher!= null && finally_catcher.isAbrupted())
			setMethodAbrupted(false);
		else {
			// Check that the body and all cases are abrupted
			boolean has_unabrupted_catcher = false;
			if( !body.isMethodAbrupted() ) has_unabrupted_catcher = true;
			else {
				for(int i=0; i < catchers.length; i++) {
					if( !catchers[i].isMethodAbrupted() ) {
						has_unabrupted_catcher = true;
						break;
					}
				}
			}
			if( !has_unabrupted_catcher ) setMethodAbrupted(true);
		}
	}
}

