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
package kiev.bytecode;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 247 $
 *
 */

public interface BytecodeFileConstants {

	/** JAVA_MAGIC - signature of JVM bytecode file*/
	public static final int JAVA_MAGIC = 0xCAFEBABE;

	/** JAVA_VERSION - current magor version of JVM classes */
	public static final short JAVA_VERSION = (short)49;

	/** JAVA_MINOR_VERSION - current minor version of JVM classes */
	public static final short JAVA_MINOR_VERSION = (short)0;

	/** Constant pool types */
	public static final int CONSTANT_UTF8				= 1;
	public static final int CONSTANT_UNICODE			= 2;
	public static final int CONSTANT_INTEGER			= 3;
	public static final int CONSTANT_FLOAT				= 4;
	public static final int CONSTANT_LONG				= 5;
	public static final int CONSTANT_DOUBLE				= 6;
	public static final int CONSTANT_CLASS				= 7;
	public static final int CONSTANT_STRING				= 8;
	public static final int CONSTANT_FIELD				= 9;
	public static final int CONSTANT_METHOD				= 10;
	public static final int CONSTANT_INTERFACEMETHOD	= 11;
	public static final int CONSTANT_NAMEANDTYPE		= 12;
	public static final int CONSTANT_METHOD_HANDLE		= 15;
	public static final int CONSTANT_METHOD_TYPE		= 16;
	public static final int CONSTANT_INVOKE_DYNAMIC		= 18;

	/** Class', field's and method's (access) flags */
	public static final int ACC_PUBLIC					= 1 << 0;
	public static final int ACC_PRIVATE				= 1 << 1;
	public static final int ACC_PROTECTED				= 1 << 2;
	public static final int ACC_STATIC					= 1 << 3;
	public static final int ACC_FINAL					= 1 << 4;
	public static final int ACC_SYNCHRONIZED			= 1 << 5;
	public static final int ACC_SUPER					= 1 << 5;
	public static final int ACC_VOLATILE				= 1 << 6;
	public static final int ACC_TRANSIENT				= 1 << 7;
	public static final int ACC_NATIVE					= 1 << 8;
	public static final int ACC_INTERFACE				= 1 << 9;
	public static final int ACC_ABSTRACT				= 1 << 10;
	public static final int ACC_STRICT					= 1 << 11; // strict math
	public static final int ACC_SYNTHETIC				= 1 << 12;
	public static final int ACC_ANNOTATION				= 1 << 13;
	public static final int ACC_ENUM					= 1 << 14; // enum classes and fields of enum classes
	
}

public interface JavaOpcodes {

	public static final int opc_nop				=   0;
	public static final int opc_aconst_null		=   1;
	public static final int opc_iconst_m1		=   2;
	public static final int opc_iconst_0		=   3;
	public static final int opc_iconst_1		=   4;
	public static final int opc_iconst_2		=   5;
	public static final int opc_iconst_3		=   6;
	public static final int opc_iconst_4		=   7;
	public static final int opc_iconst_5		=   8;
	public static final int opc_lconst_0		=   9;
	public static final int opc_lconst_1		=  10;
	public static final int opc_fconst_0		=  11;
	public static final int opc_fconst_1		=  12;
	public static final int opc_fconst_2		=  13;
	public static final int opc_dconst_0		=  14;
	public static final int opc_dconst_1		=  15;
	public static final int opc_bipush			=  16;
	public static final int opc_sipush			=  17;
	public static final int opc_ldc				=  18;
	public static final int opc_ldc_w			=  19;
	public static final int opc_ldc2_w			=  20;
	public static final int opc_iload			=  21;
	public static final int opc_lload			=  22;
	public static final int opc_fload			=  23;
	public static final int opc_dload			=  24;
	public static final int opc_aload			=  25;
	public static final int opc_iload_0			=  26;
	public static final int opc_iload_1			=  27;
	public static final int opc_iload_2			=  28;
	public static final int opc_iload_3			=  29;
	public static final int opc_lload_0			=  30;
	public static final int opc_lload_1			=  31;
	public static final int opc_lload_2			=  32;
	public static final int opc_lload_3			=  33;
	public static final int opc_fload_0			=  34;
	public static final int opc_fload_1			=  35;
	public static final int opc_fload_2			=  36;
	public static final int opc_fload_3			=  37;
	public static final int opc_dload_0			=  38;
	public static final int opc_dload_1			=  39;
	public static final int opc_dload_2			=  40;
	public static final int opc_dload_3			=  41;
	public static final int opc_aload_0			=  42;
	public static final int opc_aload_1			=  43;
	public static final int opc_aload_2			=  44;
	public static final int opc_aload_3			=  45;
	public static final int opc_iaload			=  46;
	public static final int opc_laload			=  47;
	public static final int opc_faload			=  48;
	public static final int opc_daload			=  49;
	public static final int opc_aaload			=  50;
	public static final int opc_baload			=  51;
	public static final int opc_caload			=  52;
	public static final int opc_saload			=  53;
	public static final int opc_istore			=  54;
	public static final int opc_lstore			=  55;
	public static final int opc_fstore			=  56;
	public static final int opc_dstore			=  57;
	public static final int opc_astore			=  58;
	public static final int opc_istore_0		=  59;
	public static final int opc_istore_1		=  60;
	public static final int opc_istore_2		=  61;
	public static final int opc_istore_3		=  62;
	public static final int opc_lstore_0		=  63;
	public static final int opc_lstore_1		=  64;
	public static final int opc_lstore_2		=  65;
	public static final int opc_lstore_3		=  66;
	public static final int opc_fstore_0		=  67;
	public static final int opc_fstore_1		=  68;
	public static final int opc_fstore_2		=  69;
	public static final int opc_fstore_3		=  70;
	public static final int opc_dstore_0		=  71;
	public static final int opc_dstore_1		=  72;
	public static final int opc_dstore_2		=  73;
	public static final int opc_dstore_3		=  74;
	public static final int opc_astore_0		=  75;
	public static final int opc_astore_1		=  76;
	public static final int opc_astore_2		=  77;
	public static final int opc_astore_3		=  78;
	public static final int opc_iastore			=  79;
	public static final int opc_lastore			=  80;
	public static final int opc_fastore			=  81;
	public static final int opc_dastore			=  82;
	public static final int opc_aastore			=  83;
	public static final int opc_bastore			=  84;
	public static final int opc_castore			=  85;
	public static final int opc_sastore			=  86;
	public static final int opc_pop				=  87;
	public static final int opc_pop2			=  88;
	public static final int opc_dup				=  89;
	public static final int opc_dup_x1			=  90;
	public static final int opc_dup_x2			=  91;
	public static final int opc_dup2			=  92;
	public static final int opc_dup2_x1			=  93;
	public static final int opc_dup2_x2			=  94;
	public static final int opc_swap			=  95;
	public static final int opc_iadd			=  96;
	public static final int opc_ladd			=  97;
	public static final int opc_fadd			=  98;
	public static final int opc_dadd			=  99;
	public static final int opc_isub			= 100;
	public static final int opc_lsub			= 101;
	public static final int opc_fsub			= 102;
	public static final int opc_dsub			= 103;
	public static final int opc_imul			= 104;
	public static final int opc_lmul			= 105;
	public static final int opc_fmul			= 106;
	public static final int opc_dmul			= 107;
	public static final int opc_idiv			= 108;
	public static final int opc_ldiv			= 109;
	public static final int opc_fdiv			= 110;
	public static final int opc_ddiv			= 111;
	public static final int opc_irem			= 112;
	public static final int opc_lrem			= 113;
	public static final int opc_frem			= 114;
	public static final int opc_drem			= 115;
	public static final int opc_ineg			= 116;
	public static final int opc_lneg			= 117;
	public static final int opc_fneg			= 118;
	public static final int opc_dneg			= 119;
	public static final int opc_ishl			= 120;
	public static final int opc_lshl			= 121;
	public static final int opc_ishr			= 122;
	public static final int opc_lshr			= 123;
	public static final int opc_iushr			= 124;
	public static final int opc_lushr			= 125;
	public static final int opc_iand			= 126;
	public static final int opc_land			= 127;
	public static final int opc_ior				= 128;
	public static final int opc_lor				= 129;
	public static final int opc_ixor			= 130;
	public static final int opc_lxor			= 131;
	public static final int opc_iinc			= 132;
	public static final int opc_i2l				= 133;
	public static final int opc_i2f				= 134;
	public static final int opc_i2d				= 135;
	public static final int opc_l2i				= 136;
	public static final int opc_l2f				= 137;
	public static final int opc_l2d				= 138;
	public static final int opc_f2i				= 139;
	public static final int opc_f2l				= 140;
	public static final int opc_f2d				= 141;
	public static final int opc_d2i				= 142;
	public static final int opc_d2l				= 143;
	public static final int opc_d2f				= 144;
	public static final int opc_i2b				= 145;
	public static final int opc_i2c				= 146;
	public static final int opc_i2s				= 147;
	public static final int opc_lcmp			= 148;
	public static final int opc_fcmpl			= 149;
	public static final int opc_fcmpg			= 150;
	public static final int opc_dcmpl			= 151;
	public static final int opc_dcmpg			= 152;
	public static final int opc_ifeq			= 153;
	public static final int opc_ifne			= 154;
	public static final int opc_iflt			= 155;
	public static final int opc_ifge			= 156;
	public static final int opc_ifgt			= 157;
	public static final int opc_ifle			= 158;
	public static final int opc_if_icmpeq		= 159;
	public static final int opc_if_icmpne		= 160;
	public static final int opc_if_icmplt		= 161;
	public static final int opc_if_icmpge		= 162;
	public static final int opc_if_icmpgt		= 163;
	public static final int opc_if_icmple		= 164;
	public static final int opc_if_acmpeq		= 165;
	public static final int opc_if_acmpne		= 166;
	public static final int opc_goto			= 167;
	public static final int opc_jsr				= 168;
	public static final int opc_ret				= 169;
	public static final int opc_tableswitch		= 170;
	public static final int opc_lookupswitch	= 171;
	public static final int opc_ireturn			= 172;
	public static final int opc_lreturn			= 173;
	public static final int opc_freturn			= 174;
	public static final int opc_dreturn			= 175;
	public static final int opc_areturn			= 176;
	public static final int opc_return			= 177;
	public static final int opc_getstatic		= 178;
	public static final int opc_putstatic		= 179;
	public static final int opc_getfield		= 180;
	public static final int opc_putfield		= 181;
	public static final int opc_invokevirtual	= 182;
	public static final int opc_invokespecial	= 183;
	public static final int opc_invokestatic	= 184;
	public static final int opc_invokeinterface	= 185;
	public static final int opc_xxxunusedxxx	= 186;
	public static final int opc_new				= 187;
	public static final int opc_newaray			= 188;
	public static final int opc_anewarray		= 189;
	public static final int opc_arraylength		= 190;
	public static final int opc_athrow			= 191;
	public static final int opc_checkcast		= 192;
	public static final int opc_instanceof		= 193;
	public static final int opc_monitorenter	= 194;
	public static final int opc_monitorexit		= 195;
	public static final int opc_wide			= 196;
	public static final int opc_multianewrray	= 197;
	public static final int opc_ifnull			= 198;
	public static final int opc_ifnonnull		= 199;
	public static final int opc_goto_w			= 200;
	public static final int opc_jsr_w			= 201;
	public static final int opc_breakpoint		= 202;

  public static final int opc_newmethodref = 203;
  public static final int opc_addargs = 204;
  public static final int opc_invokemethodref = 205;

}

public interface NewArrayConstants {

    // types for newarray
    public static final int T_CLASS				= 2;
    public static final int T_BOOLEAN			= 4;
    public static final int T_CHAR				= 5;
    public static final int T_FLOAT				= 6;
    public static final int T_DOUBLE			= 7;
    public static final int T_BYTE				= 8;
    public static final int T_SHORT				= 9;
    public static final int T_INT				= 10;
    public static final int T_LONG				= 11;

}

public interface BytecodeAttributeNames {

	public final static String attrCode				= "Code";
	public final static String attrSourceFile			= "SourceFile";
	public final static String attrSourceDebugExtension= "SourceDebugExtension";
	public final static String attrSignature			= "Signature";
	public final static String attrLocalVarTable		= "LocalVariableTable";
	public final static String attrLinenoTable		= "LineNumberTable";
	public final static String attrExceptions			= "Exceptions";
	public final static String attrInnerClasses		= "InnerClasses";
	public final static String attrConstantValue		= "ConstantValue";
//	public final static String attrClassArguments		= "kiev.ClassArguments";
//	public final static String attrPizzaCase			= "kiev.PizzaCase";
//	public final static String attrKiev				= "kiev.Kiev";
//	public final static String attrFlags				= "kiev.Flags";
//	public final static String attrAlias				= "kiev.Alias";
//	public final static String attrTypedef			= "kiev.Typedef";
//	public final static String attrOperator			= "kiev.Operator";
//	public final static String attrImport				= "kiev.Import";
//	public final static String attrEnum				= "kiev.Enum";
	public final static String attrRequire			= "kiev.Require";
	public final static String attrEnsure				= "kiev.Ensure";
//	public final static String attrCheckFields		= "kiev.CheckFields";
//	public final static String attrGenerations		= "kiev.Generations";
//	public final static String attrPackedFields		= "kiev.PackedFields";
//	public final static String attrPackerField		= "kiev.PackerField";
	public final static String attrRVAnnotations		= "RuntimeVisibleAnnotations";
	public final static String attrRIAnnotations		= "RuntimeInvisibleAnnotations";
	public final static String attrRVParAnnotations	= "RuntimeVisibleParameterAnnotations";
	public final static String attrRIParAnnotations	= "RuntimeInvisibleParameterAnnotations";
	public final static String attrAnnotationDefault	= "AnnotationDefault";
}

