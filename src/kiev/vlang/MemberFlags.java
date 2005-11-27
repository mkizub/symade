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

/**
 * @author Maxim Kizub
 * @version $Revision: 182 $
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
	//public static final int ACC_VARARGS			= 1 << 7; // method
	public static final int ACC_NATIVE				= 1 << 8;
	public static final int ACC_INTERFACE			= 1 << 9;
	public static final int ACC_ABSTRACT			= 1 << 10;
	public static final int ACC_STRICT				= 1 << 11; // strict math
	public static final int ACC_SYNTHETIC			= 1 << 12;
	public static final int ACC_ANNOTATION			= 1 << 13;
	public static final int ACC_ENUM				= 1 << 14; // enum classes and fields of enum classes

	// Valid for bytecode mask
	public static final int JAVA_ACC_MASK		= 0xFFFF;

	// Struct specific
	//public static final int ACC_PACKAGE			= 1 << 11;
	//public static final int ACC_ARGUMENT		= 1 << 12;
	//public static final int ACC_PIZZACASE		= 1 << 13;
	//public static final int ACC_LOCAL			= 1 << 14;
	//public static final int ACC_ANONYMOUSE		= 1 << 15;
	//public static final int ACC_HAS_CASES		= 1 << 16;
	//public static final int ACC_VERIFIED		= 1 << 17;
	//public static final int ACC_MEMBERS_GENERATED		= 1 << 18;
	//public static final int ACC_STATEMENTS_GENERATED	= 1 << 19;
	//public static final int ACC_GENERATED		= 1 << 20;
	//public static final int ACC_ENUM			= 1 << 21;
	//public static final int ACC_SYNTAX			= 1 << 22;
	//public static final int ACC_PRIMITIVE_ENUM	= 1 << 23;
	//public static final int ACC_WRAPPER			= 1 << 24;
	public static final int ACC_PACKAGE			= 1 << 16;
	public static final int ACC_ARGUMENT			= 1 << 17;
	public static final int ACC_PIZZACASE			= 1 << 18;
	//public static final int ACC_ENUM				= 1 << 19;
	public static final int ACC_SYNTAX			= 1 << 20;
	public static final int ACC_WRAPPER			= 1 << 21;

	// Method specific
	//public static final int ACC_MULTIMETHOD		= 1 << 11;
	//public static final int ACC_VIRTUALSTATIC	= 1 << 12;
	//public static final int ACC_VARARGS			= 1 << 13;
	//public static final int ACC_RULEMETHOD		= 1 << 14;
	//public static final int ACC_OPERATORMETHOD	= 1 << 15;
	//public static final int ACC_GENPOSTCOND		= 1 << 16;
	//public static final int ACC_NEEDFIELDINITS	= 1 << 17;
	//public static final int ACC_INVARIANT_METHOD= 1 << 18;
	//public static final int ACC_LOCAL_METHOD	= 1 << 19;
	public static final int ACC_MULTIMETHOD		= 1 << 16; // temporary used with java flags
	public static final int ACC_VARARGS			= 1 << 17; // temporary used with java flags
	public static final int ACC_RULEMETHOD		= 1 << 18; // temporary used with java flags
	public static final int ACC_INVARIANT_METHOD	= 1 << 19; // temporary used with java flags

	// Var/field specific
	//public static final int ACC_NEED_PROXY		= 1 << 11;
	//public static final int ACC_INITIALIZED		= 1 << 12;
	//public static final int ACC_NEED_REFPROXY	= 1 << 13;
	//public static final int ACC_VIRTUAL			= 1 << 14;
	//public static final int ACC_FORWARD			= 1 << 15;
	//public static final int ACC_PACKER_FIELD	= 1 << 16;
	//public static final int ACC_PACKED_FIELD	= 1 << 17;
	//public static final int ACC_LOCALRULEVAR	= 1 << 19;
	//public static final int ACC_CLOSURE_PROXY	= 1 << 21;
	//public static final int ACC_INIT_WRAPPER	= 1 << 22;
	public static final int ACC_FORWARD			= 1 << 16; // temporary used with java flags
	public static final int ACC_VIRTUAL			= 1 << 17; // temporary used with java flags

	// Expression specific
	//public static final int ACC_USE_NOPROXY		= 1 << 11;
	//public static final int ACC_AS_FIELD		= 1 << 12;
	//public static final int ACC_CONSTEXPR		= 1 << 13;
	//public static final int ACC_TRYRESOLVED		= 1 << 14;
	//public static final int ACC_GENRESOLVE		= 1 << 15;
	//public static final int ACC_FOR_WRAPPER		= 1 << 16;

	// Statement specific
	//public static final int ACC_ABRUPTED		= 1 << 11;
	//public static final int ACC_BREAKED			= 1 << 12;
	//public static final int ACC_METHODABRUPTED	= 1 << 13;
	//public static final int ACC_AUTORETURNABLE	= 1 << 14;
	//public static final int ACC_BREAK_TARGET	= 1 << 15;

	// General
	//public static final int ACC_FROM_INNER		= 1 << 28;	// Private member accessed from inner class
	//public static final int ACC_RESOLVED		= 1 << 29;
	//public static final int ACC_HIDDEN			= 1 << 30;
	//public static final int ACC_BAD				= 1 << 31;

}


