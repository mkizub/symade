/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.
 
 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Instr.java,v 1.4 1999/01/29 01:22:23 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4 $
 *
 */

public enum Instr {
	op_nop,
	op_push_iconst,
	op_push_lconst,
	op_push_fconst,
	op_push_dconst,
	op_push_sconst,
	op_push_null,
	op_pop,
	op_load,		//(int v),
	op_store,		//(int v),
	op_arr_load,
	op_arr_store,
	op_dup,
	op_dup_x,
	op_dup_x2,
	op_dup2,
	op_swap,
	op_add,
	op_sub,
	op_mul,
	op_div,
	op_rem,
	op_neg,
	op_shl,
	op_shr,
	op_ushr,
	op_and,
	op_or,
	op_xor,
	op_incr,
	op_x2y,				//(Type to),
	op_ifeq,			//(CodeLabel l),
	op_ifne,			//(CodeLabel l),
	op_iflt,			//(CodeLabel l),
	op_ifgt,			//(CodeLabel l),
	op_ifle,			//(CodeLabel l),
	op_ifge,			//(CodeLabel l),
	op_ifcmpeq,			//(CodeLabel l),
	op_ifcmpne,			//(CodeLabel l),
	op_ifcmple,			//(CodeLabel l),
	op_ifcmplt,			//(CodeLabel l),
	op_ifcmpge,			//(CodeLabel l),
	op_ifcmpgt,			//(CodeLabel l),
	op_goto,			//(CodeLabel l),
	op_jsr,				//(CodeLabel l),
	op_ret,				//(int addr),
	op_tableswitch,		//(CodeTableSwitch sw),
	op_lookupswitch,	//(CodeLookupSwitch sw),
	op_return,
	op_getstatic,		//(Field f),
	op_putstatic,		//(Field f),
	op_getfield,		//(Field f),
	op_putfield,		//(Field f),
	op_call,			//(Method method, boolean super_flag),
	op_new,				//(Type type),
	op_newarray,		//(Type type),
	op_multianewarray,	//(int dim, Type arrtype),
	op_arrlength,
	op_throw,
	op_instanceof,		//(Type type),
	op_monitorenter,
	op_monitorexit,
	op_ifnull,			//(CodeLabel l),
	op_ifnonnull,		//(CodeLabel l),
	op_checkcast,		//(Type type),
	op_addargs,			//(Type type, int nargs)
	op_java_instr,		//(int opc),

	set_lineno,			//(int lineno),
	set_CP,				//(CP c),

	set_label,			//(CodeLabel l),
	switch_close,		//(CodeSwitch sw),
	add_var,			//(Var var, CodeVar cv),
	remove_var,			//(Var var, CodeVar cv),

	start_catcher,		//(CodeCatchInfo catcher),
	stop_catcher,		//(CodeCatchInfo catcher),
	enter_catch_handler,	//(CodeCatchInfo catcher),
	exit_catch_handler		//(CodeCatchInfo catcher),
}

