package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

operator "X ?= X" , 5;
operator "X @= X" , 5;

/**
 * @author Maxim Kizub
 *
 */

public metatype Globals extends any {

	@CompilerNode("Set")
	@macro @native
	public static <L extends Object, R extends L> R ref_assign(L lval, R val) operator "V = V";

	@CompilerNode("Set")
	@macro @native
	public static <L extends Object, R extends L> R ref_assign2(L lval, R val) operator "V := V";

	@CompilerNode("Set")
	@macro @native
	public static <L extends Object, R extends L> R@ ref_pvar_init(L@ lval, R@ val) operator "V := V";

	@CompilerNode("Set")
	@macro
	public static <L extends Object, R extends L> R ref_assign_pvar(L lval, R@ val) operator "V = V"
	{
		case AssignExpr# self():
			new #AssignExpr(lval=self.lval,op=self.op,value=new #CallExpr(obj=self.value,ident="get$$var"))
		case CallExpr# self():
			new #AssignExpr(lval=lval,op="V = V",value=new #CallExpr(obj=val,ident="get$$var"))
	}

	@CompilerNode("Set")
	@macro
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R val) operator "V = V"
	{
		case CallExpr# self():
			new #CallExpr(obj=lval,ident="$bind",args={val})
		case AssignExpr# self():
			new #CallExpr(obj=self.lval,ident="$bind",args={self.value})
	}

	@CompilerNode("Set")
	@macro
	public static <L extends Object, R extends L> void ref_pvar_bind(L@ lval, R@ val) operator "V = V"
	{
		case CallExpr# self():
			new #CallExpr(obj=lval,ident="$bind",args={val})
		case AssignExpr# self():
			new #CallExpr(obj=self.lval,ident="$bind",args={self.value})
	}

	@CompilerNode("RuleIstheExpr")
	@macro
	public static boolean ref_pvar_is_the(Object@ lval, Object val) operator "V ?= V" ;

	@CompilerNode("RuleIsoneofExpr")
	@macro
	public static boolean ref_pvar_is_one_of(Object@ lval, Object val) operator "V @= V" ;

}

