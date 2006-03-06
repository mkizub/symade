package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.VarDecl.VarDeclView;
import kiev.vlang.LocalStructDecl.LocalStructDeclView;
import kiev.vlang.NopExpr.NopExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 359 $
 *
 */

@nodeview
public static final view RVarDecl of VarDecl extends VarDeclView {
}

@nodeview
public static final view RLocalStructDecl of LocalStructDecl extends LocalStructDeclView {
}

@nodeview
public static final view RNopExpr of NopExpr extends NopExprView {
}

