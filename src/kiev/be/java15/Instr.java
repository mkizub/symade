/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.be.java15;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 213 $
 *
 */

public enum Instr {
	op_nop,
	op_push_iconst,
	op_push_lconst,
	op_push_fconst,
	op_push_dconst,
	op_push_sconst,
	op_push_tconst,
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
	op_iadd,
	op_ladd,
	op_fadd,
	op_dadd,
	op_isub,
	op_lsub,
	op_fsub,
	op_dsub,
	op_imul,
	op_lmul,
	op_fmul,
	op_dmul,
	op_idiv,
	op_ldiv,
	op_fdiv,
	op_ddiv,
	op_irem,
	op_lrem,
	op_frem,
	op_drem,
	op_ineg,
	op_lneg,
	op_fneg,
	op_dneg,
	op_ishl,
	op_lshl,
	op_ishr,
	op_lshr,
	op_iushr,
	op_lushr,
	op_iand,
	op_land,
	op_ior,
	op_lor,
	op_ixor,
	op_lxor,
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

