package kiev.vlang;

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

	// Struct specific
	public static final int ACC_PACKAGE			= 1 << 18;
	public static final int ACC_PIZZACASE			= 1 << 19;
	public static final int ACC_SINGLRTON			= 1 << 20;
	public static final int ACC_SYNTAX				= 1 << 21;
	public static final int ACC_WRAPPER			= 1 << 22;
	public static final int ACC_VIEW				= 1 << 23;
	public static final int ACC_BYTECODE			= 1 << 24; // loaded from bytecode

	// Method specific
	public static final int ACC_MULTIMETHOD		= 1 << 16; // temporary used with java flags
	public static final int ACC_RULEMETHOD			= 1 << 18; // temporary used with java flags
	public static final int ACC_INVARIANT_METHOD	= 1 << 19; // temporary used with java flags

	// Var/field specific
	public static final int ACC_FORWARD			= 1 << 16; // temporary used with java flags
	public static final int ACC_VIRTUAL			= 1 << 17; // temporary used with java flags

}


