package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.VarDecl.VarDeclImpl;
import kiev.vlang.VarDecl.VarDeclView;
import kiev.vlang.LocalStructDecl.LocalStructDeclImpl;
import kiev.vlang.LocalStructDecl.LocalStructDeclView;
import kiev.vlang.NopExpr.NopExprImpl;
import kiev.vlang.NopExpr.NopExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 359 $
 *
 */

@nodeview
public static final view RVarDecl of VarDeclImpl extends VarDeclView {
	public RVarDecl(VarDeclImpl impl) { super(impl); }
}

@nodeview
public static final view RLocalStructDecl of LocalStructDeclImpl extends LocalStructDeclView {
}

@nodeview
public static final view RNopExpr of NopExprImpl extends NopExprView {
}

