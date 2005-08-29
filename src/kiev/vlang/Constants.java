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

import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface Constants extends AccessFlags {

	/** Java bytecode constants */

    // top-level constants
    public static final int JAVA_MAGIC = 0xCAFEBABE;
    public static final short JAVA_VERSION = (short)45;
    public static final short JAVA_MINOR_VERSION = (short)3;

    // constant pool entry types
    public static final int CONSTANT_UTF8				= 1;
//    public static final int CONSTANT_UNICODE			= 2;
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

    // types for newarray
    public static final int T_CLASS			= 2;
    public static final int T_BOOLEAN		= 4;
    public static final int T_CHAR			= 5;
    public static final int T_FLOAT			= 6;
    public static final int T_DOUBLE		= 7;
    public static final int T_BYTE			= 8;
    public static final int T_SHORT			= 9;
    public static final int T_INT			= 10;
    public static final int T_LONG			= 11;

    // opcodes
    public static final int opc_nop = 0;
    public static final int opc_aconst_null = 1;
    public static final int opc_iconst_m1 = 2;
    public static final int opc_iconst_0 = 3;
    public static final int opc_iconst_1 = 4;
    public static final int opc_iconst_2 = 5;
    public static final int opc_iconst_3 = 6;
    public static final int opc_iconst_4 = 7;
    public static final int opc_iconst_5 = 8;
    public static final int opc_lconst_0 = 9;
    public static final int opc_lconst_1 = 10;
    public static final int opc_fconst_0 = 11;
    public static final int opc_fconst_1 = 12;
    public static final int opc_fconst_2 = 13;
    public static final int opc_dconst_0 = 14;
    public static final int opc_dconst_1 = 15;
    public static final int opc_bipush = 16;
    public static final int opc_sipush = 17;
    public static final int opc_ldc = 18;
    public static final int opc_ldc_w = 19;
    public static final int opc_ldc2_w = 20;
    public static final int opc_iload = 21;
    public static final int opc_lload = 22;
    public static final int opc_fload = 23;
    public static final int opc_dload = 24;
    public static final int opc_aload = 25;
    public static final int opc_iload_0 = 26;
    public static final int opc_iload_1 = 27;
    public static final int opc_iload_2 = 28;
    public static final int opc_iload_3 = 29;
    public static final int opc_lload_0 = 30;
    public static final int opc_lload_1 = 31;
    public static final int opc_lload_2 = 32;
    public static final int opc_lload_3 = 33;
    public static final int opc_fload_0 = 34;
    public static final int opc_fload_1 = 35;
    public static final int opc_fload_2 = 36;
    public static final int opc_fload_3 = 37;
    public static final int opc_dload_0 = 38;
    public static final int opc_dload_1 = 39;
    public static final int opc_dload_2 = 40;
    public static final int opc_dload_3 = 41;
    public static final int opc_aload_0 = 42;
    public static final int opc_aload_1 = 43;
    public static final int opc_aload_2 = 44;
    public static final int opc_aload_3 = 45;
    public static final int opc_iaload = 46;
    public static final int opc_laload = 47;
    public static final int opc_faload = 48;
    public static final int opc_daload = 49;
    public static final int opc_aaload = 50;
    public static final int opc_baload = 51;
    public static final int opc_caload = 52;
    public static final int opc_saload = 53;
    public static final int opc_istore = 54;
    public static final int opc_lstore = 55;
    public static final int opc_fstore = 56;
    public static final int opc_dstore = 57;
    public static final int opc_astore = 58;
    public static final int opc_istore_0 = 59;
    public static final int opc_istore_1 = 60;
    public static final int opc_istore_2 = 61;
    public static final int opc_istore_3 = 62;
    public static final int opc_lstore_0 = 63;
    public static final int opc_lstore_1 = 64;
    public static final int opc_lstore_2 = 65;
    public static final int opc_lstore_3 = 66;
    public static final int opc_fstore_0 = 67;
    public static final int opc_fstore_1 = 68;
    public static final int opc_fstore_2 = 69;
    public static final int opc_fstore_3 = 70;
    public static final int opc_dstore_0 = 71;
    public static final int opc_dstore_1 = 72;
    public static final int opc_dstore_2 = 73;
    public static final int opc_dstore_3 = 74;
    public static final int opc_astore_0 = 75;
    public static final int opc_astore_1 = 76;
    public static final int opc_astore_2 = 77;
    public static final int opc_astore_3 = 78;
    public static final int opc_iastore = 79;
    public static final int opc_lastore = 80;
    public static final int opc_fastore = 81;
    public static final int opc_dastore = 82;
    public static final int opc_aastore = 83;
    public static final int opc_bastore = 84;
    public static final int opc_castore = 85;
    public static final int opc_sastore = 86;
    public static final int opc_pop = 87;
    public static final int opc_pop2 = 88;
    public static final int opc_dup = 89;
    public static final int opc_dup_x1 = 90;
    public static final int opc_dup_x2 = 91;
    public static final int opc_dup2 = 92;
    public static final int opc_dup2_x1 = 93;
    public static final int opc_dup2_x2 = 94;
    public static final int opc_swap = 95;
    public static final int opc_iadd = 96;
    public static final int opc_ladd = 97;
    public static final int opc_fadd = 98;
    public static final int opc_dadd = 99;
    public static final int opc_isub = 100;
    public static final int opc_lsub = 101;
    public static final int opc_fsub = 102;
    public static final int opc_dsub = 103;
    public static final int opc_imul = 104;
    public static final int opc_lmul = 105;
    public static final int opc_fmul = 106;
    public static final int opc_dmul = 107;
    public static final int opc_idiv = 108;
    public static final int opc_ldiv = 109;
    public static final int opc_fdiv = 110;
    public static final int opc_ddiv = 111;
    public static final int opc_irem = 112;
    public static final int opc_lrem = 113;
    public static final int opc_frem = 114;
    public static final int opc_drem = 115;
    public static final int opc_ineg = 116;
    public static final int opc_lneg = 117;
    public static final int opc_fneg = 118;
    public static final int opc_dneg = 119;
    public static final int opc_ishl = 120;
    public static final int opc_lshl = 121;
    public static final int opc_ishr = 122;
    public static final int opc_lshr = 123;
    public static final int opc_iushr = 124;
    public static final int opc_lushr = 125;
    public static final int opc_iand = 126;
    public static final int opc_land = 127;
    public static final int opc_ior = 128;
    public static final int opc_lor = 129;
    public static final int opc_ixor = 130;
    public static final int opc_lxor = 131;
    public static final int opc_iinc = 132;
    public static final int opc_i2l = 133;
    public static final int opc_i2f = 134;
    public static final int opc_i2d = 135;
    public static final int opc_l2i = 136;
    public static final int opc_l2f = 137;
    public static final int opc_l2d = 138;
    public static final int opc_f2i = 139;
    public static final int opc_f2l = 140;
    public static final int opc_f2d = 141;
    public static final int opc_d2i = 142;
    public static final int opc_d2l = 143;
    public static final int opc_d2f = 144;
    public static final int opc_i2b = 145;
    public static final int opc_i2c = 146;
    public static final int opc_i2s = 147;
    public static final int opc_lcmp = 148;
    public static final int opc_fcmpl = 149;
    public static final int opc_fcmpg = 150;
    public static final int opc_dcmpl = 151;
    public static final int opc_dcmpg = 152;
    public static final int opc_ifeq = 153;
    public static final int opc_ifne = 154;
    public static final int opc_iflt = 155;
    public static final int opc_ifge = 156;
    public static final int opc_ifgt = 157;
    public static final int opc_ifle = 158;
    public static final int opc_if_icmpeq = 159;
    public static final int opc_if_icmpne = 160;
    public static final int opc_if_icmplt = 161;
    public static final int opc_if_icmpge = 162;
    public static final int opc_if_icmpgt = 163;
    public static final int opc_if_icmple = 164;
    public static final int opc_if_acmpeq = 165;
    public static final int opc_if_acmpne = 166;
    public static final int opc_goto = 167;
    public static final int opc_jsr = 168;
    public static final int opc_ret = 169;
    public static final int opc_tableswitch = 170;
    public static final int opc_lookupswitch = 171;
    public static final int opc_ireturn = 172;
    public static final int opc_lreturn = 173;
    public static final int opc_freturn = 174;
    public static final int opc_dreturn = 175;
    public static final int opc_areturn = 176;
    public static final int opc_return = 177;
    public static final int opc_getstatic = 178;
    public static final int opc_putstatic = 179;
    public static final int opc_getfield = 180;
    public static final int opc_putfield = 181;
    public static final int opc_invokevirtual = 182;
    public static final int opc_invokespecial = 183;
    public static final int opc_invokestatic = 184;
    public static final int opc_invokeinterface = 185;
    public static final int opc_xxxunusedxxx = 186;
    public static final int opc_new = 187;
    public static final int opc_newarray = 188;
    public static final int opc_anewarray = 189;
    public static final int opc_arraylength = 190;
    public static final int opc_athrow = 191;
    public static final int opc_checkcast = 192;
    public static final int opc_instanceof = 193;
    public static final int opc_monitorenter = 194;
    public static final int opc_monitorexit = 195;
    public static final int opc_wide = 196;
    public static final int opc_multianewarray = 197;
    public static final int opc_ifnull = 198;
    public static final int opc_ifnonnull = 199;
    public static final int opc_goto_w = 200;
    public static final int opc_jsr_w = 201;
    public static final int opc_breakpoint = 202;

    public static final int opc_newmethodref = 203;
    public static final int opc_addargs = 204;
    public static final int opc_invokemethodref = 205;

    // names of opcodes
    public static final String opcNames[] = {
        "opc_nop", "opc_aconst_null", "opc_iconst_m1", "opc_iconst_0",
        "opc_iconst_1", "opc_iconst_2", "opc_iconst_3", "opc_iconst_4",
        "opc_iconst_5", "opc_lconst_0", "opc_lconst_1", "opc_fconst_0",
        "opc_fconst_1", "opc_fconst_2", "opc_dconst_0", "opc_dconst_1",
        "opc_bipush", "opc_sipush", "opc_ldc", "opc_ldc_w", "opc_ldc2_w",
        "opc_iload", "opc_lload", "opc_fload", "opc_dload", "opc_aload",
        "opc_iload_0", "opc_iload_1", "opc_iload_2", "opc_iload_3",
        "opc_lload_0", "opc_lload_1", "opc_lload_2", "opc_lload_3",
        "opc_fload_0", "opc_fload_1", "opc_fload_2", "opc_fload_3",
        "opc_dload_0", "opc_dload_1", "opc_dload_2", "opc_dload_3",
        "opc_aload_0", "opc_aload_1", "opc_aload_2", "opc_aload_3",
        "opc_iaload", "opc_laload", "opc_faload", "opc_daload", "opc_aaload",
        "opc_baload", "opc_caload", "opc_saload", "opc_istore", "opc_lstore",
        "opc_fstore", "opc_dstore", "opc_astore", "opc_istore_0",
        "opc_istore_1", "opc_istore_2", "opc_istore_3", "opc_lstore_0",
        "opc_lstore_1", "opc_lstore_2", "opc_lstore_3", "opc_fstore_0",
        "opc_fstore_1", "opc_fstore_2", "opc_fstore_3", "opc_dstore_0",
        "opc_dstore_1", "opc_dstore_2", "opc_dstore_3", "opc_astore_0",
        "opc_astore_1", "opc_astore_2", "opc_astore_3", "opc_iastore",
        "opc_lastore", "opc_fastore", "opc_dastore", "opc_aastore",
        "opc_bastore", "opc_castore", "opc_sastore", "opc_pop", "opc_pop2",
        "opc_dup", "opc_dup_x1", "opc_dup_x2", "opc_dup2", "opc_dup2_x1",
        "opc_dup2_x2", "opc_swap", "opc_iadd", "opc_ladd", "opc_fadd",
        "opc_dadd", "opc_isub", "opc_lsub", "opc_fsub", "opc_dsub",
        "opc_imul", "opc_lmul", "opc_fmul", "opc_dmul", "opc_idiv",
        "opc_ldiv", "opc_fdiv", "opc_ddiv", "opc_irem", "opc_lrem",
        "opc_frem", "opc_drem", "opc_ineg", "opc_lneg", "opc_fneg",
        "opc_dneg", "opc_ishl", "opc_lshl", "opc_ishr", "opc_lshr",
        "opc_iushr", "opc_lushr", "opc_iand", "opc_land", "opc_ior",
        "opc_lor", "opc_ixor", "opc_lxor", "opc_iinc", "opc_i2l", "opc_i2f",
        "opc_i2d", "opc_l2i", "opc_l2f", "opc_l2d", "opc_f2i", "opc_f2l",
        "opc_f2d", "opc_d2i", "opc_d2l", "opc_d2f", "opc_i2b", "opc_i2c",
        "opc_i2s", "opc_lcmp", "opc_fcmpl", "opc_fcmpg", "opc_dcmpl",
        "opc_dcmpg", "opc_ifeq", "opc_ifne", "opc_iflt", "opc_ifge",
        "opc_ifgt", "opc_ifle", "opc_if_icmpeq", "opc_if_icmpne",
        "opc_if_icmplt", "opc_if_icmpge", "opc_if_icmpgt", "opc_if_icmple",
        "opc_if_acmpeq", "opc_if_acmpne", "opc_goto", "opc_jsr", "opc_ret",
        "opc_tableswitch", "opc_lookupswitch", "opc_ireturn", "opc_lreturn",
        "opc_freturn", "opc_dreturn", "opc_areturn", "opc_return",
        "opc_getstatic", "opc_putstatic", "opc_getfield", "opc_putfield",
        "opc_invokevirtual", "opc_invokespecial", "opc_invokestatic",
        "opc_invokeinterface", "opc_xxxunusedxxx", "opc_new", "opc_newarray",
        "opc_anewarray", "opc_arraylength", "opc_athrow", "opc_checkcast",
        "opc_instanceof", "opc_monitorenter", "opc_monitorexit", "opc_wide",
        "opc_multianewarray", "opc_ifnull", "opc_ifnonnull", "opc_goto_w",
        "opc_jsr_w", "opc_breakpoint",
        "opc_newmethodref", "opc_addargs", "opc_invokemethodref"
    };

    // and their lengths
    public static final int opcLengths[] = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 3, 3, 2, 2, 2, 2,
        2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2,99,99, 1, 1, 1,
        1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 5, 0, 3, 2, 3, 1, 1, 3, 3, 1, 1, 0, 4, 3, 3,
        5, 5, 1, 4, 4, 3
    };

	/** Structure modes
	 */
	public final static byte	modeClass		= (byte) 0;
	public final static byte	modeInterface	= (byte) 1;
	public final static byte	modePackage		= (byte) 2;
	public final static byte	modePizzaCase	= (byte) 3;
	public final static byte	modeArgument	= (byte) 4;
	public final static byte	modeUnresolved	= (byte) 5;

	/** Standard Java names for methods & vars
	 */
	public final static KString nameThis		= KString.from("this");
	public final static KString nameThisDollar	= KString.from("this$");
	public final static KString nameTypeInfo	= KString.from("$typeinfo");
	public final static KString nameClTypeInfo	= KString.from("__ti__");
//	public final static KString nameTypeInfoArgs= KString.from("typeargs");
//	public final static KString nameTypeInfoRel	= KString.from("related");
//	public final static KString nameTypeInfoFill= KString.from("$fill$typeinfo");
	public final static KString nameGetTypeInfo	= KString.from("getTypeInfoField");
	public final static KString nameSuper		= KString.from("super");
	public final static KString nameInit		= KString.from("<init>");
	public final static KString nameClassInit	= KString.from("<clinit>");
	public final static KString nameFinalize	= KString.from("finalize");
	public final static KString nameFinalizeSig	= KString.from("()V");
	public final static KString nameIdefault	= KString.from("default");
	public final static KString nameElements	= KString.from("elements");
	public final static KString nameHasMoreElements	= KString.from("hasMoreElements");
	public final static KString nameNextElement	= KString.from("nextElement");
	public final static KString nameCaseTag		= KString.from("case$tag");
	public final static KString nameTagSelf		= KString.from("case$self$tag");
	public final static KString nameGetCaseTag	= KString.from("get$case$tag");
	public final static KString nameFrameProxy	= KString.from("frame$proxy");
	public final static KString nameVarProxy	= KString.from("var$");
	public final static KString nameClone		= KString.from("clone");
	public final static KString nameMMMethods	= KString.from("mm$methods");
	public final static KString nameSet			= KString.from("set$");
	public final static KString nameGet			= KString.from("get$");
	public final static KString nameVarArgs		= KString.from("va_args");
	public final static KString namePEnv		= KString.from("$env$");
	public final static KString namePlvBase		= KString.from("lvar$base");
	public final static KString nameCellVal		= KString.from("$val");
	public final static KString nameEnumValuesFld	= KString.from("$values");
	public final static KString nameEnumValues	= KString.from("values");
	public final static KString nameEnumOrdinal	= KString.from("ordinal");
	public final static KString nameClosureArgs		= KString.from("$args");
	public final static KString nameClosureMaxArgs	= KString.from("max$args");
	public final static KString nameClosureTopArg	= KString.from("top$arg");
	public final static KString nameArrayOp			= KString.from("[]");
	public final static KString nameNewOp			= KString.from("new");
	public final static KString nameCastOp			= KString.from("$cast");
	public final static KString nameReturnVar		= KString.from("$return");
	public final static KString nameResultVar		= KString.from("Result");
	public final static KString nameAssertMethod	= KString.from("assert");
	public final static KString nameAssertInvariantMethod	= KString.from("assertInvariant");
	public final static KString nameAssertRequireMethod		= KString.from("assertRequire");
	public final static KString nameAssertEnsureMethod		= KString.from("assertEnsure");
	public final static KString nameAssertSignature	= KString.from("(Ljava/lang/String;)V");
	public final static KString nameAssertNameSignature	= KString.from("(Ljava/lang/String;Ljava/lang/String;)V");
	public final static KString nameKStringSignature	= KString.from("Lkiev/stdlib/KString;");
	public final static KString nameFILE		= KString.from("$FILE");
	public final static KString nameMETHOD		= KString.from("$METHOD");
	public final static KString nameLINENO		= KString.from("$LINENO");
	public final static KString nameDEBUG		= KString.from("$DEBUG");
	public final static KString nameDEF			= KString.from("$D");

	public final static KString nameObjGetClass		= KString.from("getClass");
	public final static KString nameObjHashCode		= KString.from("hashCode");
	public final static KString nameObjEquals		= KString.from("equals");
	public final static KString nameObjClone		= KString.from("clone");
	public final static KString nameObjToString		= KString.from("toString");
	public final static KString nameStrBuffAppend	= KString.from("append");
	public final static KString nameStrValueOf		= KString.from("valueOf");

	// Well known attributes
	public final static KString attrCode			= KString.from("Code");
	public final static KString attrSourceFile		= KString.from("SourceFile");
	public final static KString attrLocalVarTable	= KString.from("LocalVariableTable");
	public final static KString attrLinenoTable		= KString.from("LineNumberTable");
	public final static KString attrExceptions		= KString.from("Exceptions");
	public final static KString attrInnerClasses	= KString.from("InnerClasses");
	public final static KString attrConstantValue	= KString.from("ConstantValue");
	public final static KString attrClassArguments	= KString.from("kiev.ClassArguments");
//	public final static KString attrSignature		= KString.from("kiev.Signature");
	public final static KString attrPizzaCase		= KString.from("kiev.PizzaCase");
//	public final static KString attrMethodParams	= KString.from("kiev.MethodParams");
	public final static KString attrKiev			= KString.from("kiev.Kiev");
	public final static KString attrFlags			= KString.from("kiev.Flags");
	public final static KString attrAlias			= KString.from("kiev.Alias");
	public final static KString attrTypedef			= KString.from("kiev.Typedef");
	public final static KString attrOperator		= KString.from("kiev.Operator");
	public final static KString attrImport			= KString.from("kiev.Import");
	public final static KString attrEnum			= KString.from("kiev.Enum");
	public final static KString attrCheckFields		= KString.from("kiev.CheckFields");
	public final static KString attrRequire			= KString.from("kiev.Require");
	public final static KString attrEnsure			= KString.from("kiev.Ensure");
	public final static KString attrGenerations		= KString.from("kiev.Generations");
	public final static KString attrPackedFields	= KString.from("kiev.PackedFields");
	public final static KString attrPackerField		= KString.from("kiev.PackerField");

	public final static KString attrRVAnnotations		= KString.from("RuntimeVisibleAnnotations");
	public final static KString attrRIAnnotations		= KString.from("RuntimeInvisibleAnnotations");
	public final static KString attrRVParAnnotations	= KString.from("RuntimeVisibleParameterAnnotations");
	public final static KString attrRIParAnnotations	= KString.from("RuntimeInvisibleParameterAnnotations");
	public final static KString attrAnnotationDefault	= KString.from("AnnotationDefault");

	public final static KString nameVoid	= KString.from("void");
	public final static KString sigVoid		= KString.from("V");
	public final static KString nameBoolean	= KString.from("boolean");
	public final static KString sigBoolean	= KString.from("Z");
	public final static KString nameByte	= KString.from("byte");
	public final static KString sigByte		= KString.from("B");
	public final static KString nameChar	= KString.from("char");
	public final static KString sigChar		= KString.from("C");
	public final static KString nameShort	= KString.from("short");
	public final static KString sigShort	= KString.from("S");
	public final static KString nameInt		= KString.from("int");
	public final static KString sigInt		= KString.from("I");
	public final static KString nameLong	= KString.from("long");
	public final static KString sigLong		= KString.from("J");
	public final static KString nameFloat	= KString.from("float");
	public final static KString sigFloat	= KString.from("F");
	public final static KString nameDouble	= KString.from("double");
	public final static KString sigDouble	= KString.from("D");
	public final static KString nameMethod	= KString.from("<method>");
	public final static KString sigMethod	= KString.from("(");

//	public final static Type tpVoid			= Type.tpVoid;
//	public final static Type tpBoolean		= Type.tpBoolean;
//	public final static Type tpByte			= Type.tpByte;
//	public final static Type tpChar			= Type.tpChar;
//	public final static Type tpShort		= Type.tpShort;
//	public final static Type tpInt			= Type.tpInt;
//	public final static Type tpLong			= Type.tpLong;
//	public final static Type tpFloat		= Type.tpFloat;
//	public final static Type tpDouble		= Type.tpDouble;
//	public final static Type tpNull			= Type.tpNull;
//	public final static Type tpObject		= Type.tpObject;
//	public final static Type tpArray		= Type.tpArray;
//	public final static Type tpCloneable	= Type.tpCloneable;
//	public final static Type tpString		= Type.tpString;
//	public final static Type tpThrowable	= Type.tpThrowable;
//	public final static Type tpError		= Type.tpError;
//	public final static Type tpException	= Type.tpException;
//	public final static Type tpRuntimeException = Type.tpRuntimeException;


    /** Binary operators priority, image and name */
	public final static int		opAssignPriority		= 5;
	public final static int		opConditionalPriority	= 7;
	public final static int		opBooleanOrPriority		= 10;
	public final static int		opBooleanAndPriority	= 20;
	public final static int		opBitOrPriority			= 30;
	public final static int		opBitXorPriority		= 40;
	public final static int		opBitAndPriority		= 50;
	public final static int		opEqualsPriority		= 60;
	public final static int		opInstanceOfPriority	= 70;
	public final static int		opComparePriority		= 80;
	public final static int		opShiftPriority			= 90;
	public final static int		opAddPriority			= 100;
	public final static int		opMulPriority			= 150;
	public final static int		opCastPriority			= 180;
	public final static int		opNegPriority			= 200;
	public final static int		opIncrPriority			= 210;
	public final static int		opBitNotPriority		= 210;
	public final static int		opBooleanNotPriority	= 210;
	public final static int		opContainerElementPriority		= 230;
	public final static int		opCallPriority			= 240;
	public final static int		opAccessPriority		= 240;

}
