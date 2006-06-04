package kiev.vlang;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface AccessFlags {

	public static final int ACC_PUBLIC				= 1 << 0;
	public static final int ACC_PRIVATE			= 1 << 1;
	public static final int ACC_PROTECTED			= 1 << 2;
	public static final int ACC_STATIC				= 1 << 3;
	public static final int ACC_FINAL				= 1 << 4;
	public static final int ACC_SYNCHRONIZED		= 1 << 5; // method
	public static final int ACC_SUPER				= 1 << 5; // class
	public static final int ACC_VOLATILE			= 1 << 6; // field
	public static final int ACC_BRIDGE				= 1 << 6; // method
	public static final int ACC_TRANSIENT			= 1 << 7; // field
	public static final int ACC_VARARGS			= 1 << 7; // method
	public static final int ACC_NATIVE				= 1 << 8;
	public static final int ACC_INTERFACE			= 1 << 9;
	public static final int ACC_ABSTRACT			= 1 << 10;
	public static final int ACC_STRICT				= 1 << 11; // strict math
	public static final int ACC_SYNTHETIC			= 1 << 12;
	public static final int ACC_ANNOTATION			= 1 << 13;
	public static final int ACC_ENUM				= 1 << 14; // enum classes and fields of enum classes

	// Valid for bytecode mask
	public static final int JAVA_ACC_MASK			= 0xFFFF;

	public static final int ACC_FORWARD			= 1 << 16; // temporary used with java flags
	public static final int ACC_VIRTUAL			= 1 << 17; // temporary used with java flags
	public static final int ACC_TYPE_UNERASABLE	= 1 << 18; // temporary used with java flags
	public static final int ACC_MACRO				= 1 << 19; // macro field/method, metatypes

	// Struct specific
	public static final int ACC_PACKAGE			= ACC_PRIVATE | ACC_PUBLIC;
	public static final int ACC_SYNTAX				= ACC_PRIVATE | ACC_PROTECTED;
}


