package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public metatype Globals extends any {

	@macro @native
	public static <L extends Object, R extends L> R ref_assign(L lval, R val) alias lfy operator = ;

	@macro @native
	public static <L extends Object, R extends L> R ref_assign2(L lval, R val) alias lfy operator := ;

	@macro @native
	public static <L extends Object, R extends L> PVar<R> ref_pvar_init(L@ lval, PVar<R> val) alias lfy operator := ;

	@macro
	public static <L extends Object, R extends L> R ref_assign_pvar(L lval, R@ val) alias lfy operator =
	{
		case AssignExpr# self():
			new #AssignExpr(lval=self.lval,op=self.op,value=new #CallExpr(obj=self.value,ident="get$$var"))
		case CallExpr# self():
			new #AssignExpr(lval=lval,op="V = V",value=new #CallExpr(obj=val,ident="get$$var"))
	}

	@macro
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R val) alias lfy operator =
	{
		case CallExpr# self():
			new #CallExpr(obj=lval,ident="$bind",args={val})
		case AssignExpr# self():
			new #CallExpr(obj=self.lval,ident="$bind",args={self.value})
	}

	@macro
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R@ val) alias lfy operator =
	{
		case CallExpr# self():
			new #CallExpr(obj=lval,ident="$bind",args={val})
		case AssignExpr# self():
			new #CallExpr(obj=self.lval,ident="$bind",args={self.value})
	}

}

