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

import static kiev.vlang.AccessFlags.*;
import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 182 $
 *
 */

public interface StdTypes {

	public static final int flReference		=    1;
	public static final int flIntegerInCode	=    2;
	public static final int flInteger			=    4;
	public static final int flFloatInCode		=    8;
	public static final int flFloat				=   16;
	public static final int flNumber			=   20;
	public static final int flDoubleSize		=   32;
	public static final int flArray				=   64;
	public static final int flResolved			=  128;
	public static final int flBoolean			=  256;
	public static final int flArgumented		=  512;
	public static final int flWrapper			= 1024;

	public static final BaseType tpEnv;
	public static final BaseType tpAny;
	public static final BaseType tpVoid;
	public static final BaseType tpRule;
	public static final BaseType tpBoolean;
	public static final BaseType tpByte;
	public static final BaseType tpChar;
	public static final BaseType tpShort;
	public static final BaseType tpInt;
	public static final BaseType tpLong;
	public static final BaseType tpFloat;
	public static final BaseType tpDouble;
	public static final BaseType tpBooleanRef;
	public static final BaseType tpByteRef;
	public static final BaseType tpCharRef;
	public static final BaseType tpNumberRef;
	public static final BaseType tpShortRef;
	public static final BaseType tpIntRef;
	public static final BaseType tpLongRef;
	public static final BaseType tpFloatRef;
	public static final BaseType tpDoubleRef;
	public static final BaseType tpVoidRef;
	public static final BaseType tpNull;
	public static final BaseType tpObject;
	public static final BaseType tpClass;
	public static final BaseType tpDebug;
	public static final BaseType tpTypeInfo;
	public static final BaseType tpTypeInfoInterface;
	public static final BaseType tpArray;
	public static final BaseType tpCloneable;
	public static final BaseType tpString;
	public static final BaseType tpThrowable;
	public static final BaseType tpError;
	public static final BaseType tpException;
	public static final BaseType tpCastException;
	public static final BaseType tpJavaEnumeration;
	public static final BaseType tpKievEnumeration;
	public static final BaseType tpRuntimeException;
	public static final BaseType tpAssertException;
	public static final BaseType tpEnum;
	public static final BaseType tpAnnotation;
	public static final BaseType tpClosure;
	public static final BaseType tpMethod;
	public static final Struct tpClosureClazz;
	public static final Struct tpMethodClazz;

	public static final BaseType tpPrologVar;
	public static final BaseType tpRefProxy;

	public static final BaseType tpTypeSwitchHash;

	static {
		Type.typeHash = new Hash<Type>();
		Hash<Type> typeHash = Type.typeHash;

		Struct tpEnvClazz = Env.root;
		tpEnv				= new BaseType();
		tpEnv.clazz			= tpEnvClazz;
		tpEnvClazz.type		= tpEnv;
		tpEnv.flags			= flResolved;
		tpEnv.signature		= KString.from("<root>");
		tpEnv.java_signature	= KString.from("<root>");
		typeHash.put(tpEnv);

		Struct tpAnyClazz = Env.newStruct(new ClazzName(
							KString.from("<any>"),
							KString.from("<any>"),
							KString.from("?"),false,false),null,ACC_PUBLIC);
		tpAny				= new BaseType();
		tpAny.clazz			= tpAnyClazz;
//		typeHash.remove(tpAnyClazz.type);
		tpAnyClazz.type		= tpAny;
		tpAnyClazz.setResolved(true);
		tpAny.flags			= flResolved;
		tpAny.signature		= KString.from("?");
		tpAny.java_signature	= KString.from("?");
		typeHash.put(tpAny);

		Struct tpRuleClazz = Env.newStruct(new ClazzName(
							KString.from("rule"),
							KString.from("rule"),
							KString.from("R"),false,false),null,ACC_PUBLIC);
		tpRule					= new BaseType();
		tpRule.clazz			= tpRuleClazz;
//		typeHash.remove(tpRuleClazz.type);
		tpRuleClazz.type		= tpRule;
		tpRuleClazz.setResolved(true);
		tpRule.flags			= flResolved | flReference;
		tpRule.signature		= KString.from("R");
		tpRule.java_signature	= KString.from("Lkiev/stdlib/RuleFrame;");
		typeHash.put(tpRule);

		Struct tpBooleanClazz = Env.newStruct(new ClazzName(
							KString.from("boolean"),
							KString.from("boolean"),
							KString.from("Z"),false,false),null,ACC_PUBLIC);
		tpBoolean				= new BaseType();
		tpBoolean.clazz			= tpBooleanClazz;
//		typeHash.remove(tpBooleanClazz.type);
		tpBooleanClazz.type		= tpBoolean;
		tpBooleanClazz.setResolved(true);
		tpBoolean.flags			= flResolved | flIntegerInCode | flBoolean;
		tpBoolean.signature		= KString.from("Z");
		tpBoolean.java_signature		= KString.from("Z");
		typeHash.put(tpBoolean);

		Struct tpByteClazz = Env.newStruct(new ClazzName(
							KString.from("byte"),
							KString.from("byte"),
							KString.from("B"),false,false),null,ACC_PUBLIC);
		tpByte					= new BaseType();
		tpByte.clazz			= tpByteClazz;
//		typeHash.remove(tpByteClazz.type);
		tpByteClazz.type		= tpByte;
		tpByteClazz.setResolved(true);
		tpByte.flags			= flResolved | flInteger | flIntegerInCode ;
		tpByte.signature		= KString.from("B");
		tpByte.java_signature		= KString.from("B");
		typeHash.put(tpByte);

		Struct tpCharClazz = Env.newStruct(new ClazzName(
							KString.from("char"),
							KString.from("char"),
							KString.from("C"),false,false),null,ACC_PUBLIC);
		tpChar					= new BaseType();
		tpChar.clazz			= tpCharClazz;
//		typeHash.remove(tpCharClazz.type);
		tpCharClazz.type		= tpChar;
		tpCharClazz.setResolved(true);
		tpChar.flags			= flResolved | flInteger | flIntegerInCode ;
		tpChar.signature		= KString.from("C");
		tpChar.java_signature		= KString.from("C");
		typeHash.put(tpChar);

		Struct tpShortClazz = Env.newStruct(new ClazzName(
							KString.from("short"),
							KString.from("short"),
							KString.from("S"),false,false),null,ACC_PUBLIC);
		tpShort					= new BaseType();
		tpShort.clazz			= tpShortClazz;
//		typeHash.remove(tpShortClazz.type);
		tpShortClazz.type		= tpShort;
		tpShortClazz.setResolved(true);
		tpShort.flags			= flResolved | flInteger | flIntegerInCode ;
		tpShort.signature		= KString.from("S");
		tpShort.java_signature		= KString.from("S");
		typeHash.put(tpShort);

		Struct tpIntClazz = Env.newStruct(new ClazzName(
							KString.from("int"),
							KString.from("int"),
							KString.from("I"),false,false),null,ACC_PUBLIC);
		tpInt					= new BaseType();
		tpInt.clazz			= tpIntClazz;
//		typeHash.remove(tpIntClazz.type);
		tpIntClazz.type		= tpInt;
		tpIntClazz.setResolved(true);
		tpInt.flags			= flResolved | flInteger | flIntegerInCode ;
		tpInt.signature		= KString.from("I");
		tpInt.java_signature		= KString.from("I");
		typeHash.put(tpInt);

		Struct tpLongClazz = Env.newStruct(new ClazzName(
							KString.from("long"),
							KString.from("long"),
							KString.from("J"),false,false),null,ACC_PUBLIC);
		tpLong					= new BaseType();
		tpLong.clazz			= tpLongClazz;
//		typeHash.remove(tpLongClazz.type);
		tpLongClazz.type		= tpLong;
		tpLongClazz.setResolved(true);
		tpLong.flags			= flResolved | flInteger | flDoubleSize;
		tpLong.signature		= KString.from("J");
		tpLong.java_signature		= KString.from("J");
		typeHash.put(tpLong);

		Struct tpFloatClazz = Env.newStruct(new ClazzName(
							KString.from("float"),
							KString.from("float"),
							KString.from("F"),false,false),null,ACC_PUBLIC);
		tpFloat					= new BaseType();
		tpFloat.clazz			= tpFloatClazz;
//		typeHash.remove(tpFloatClazz.type);
		tpFloatClazz.type		= tpFloat;
		tpFloatClazz.setResolved(true);
		tpFloat.flags			= flResolved | flFloat ;
		tpFloat.signature		= KString.from("F");
		tpFloat.java_signature		= KString.from("F");
		typeHash.put(tpFloat);

		Struct tpDoubleClazz = Env.newStruct(new ClazzName(
							KString.from("double"),
							KString.from("double"),
							KString.from("D"),false,false),null,ACC_PUBLIC);
		tpDouble				= new BaseType();
		tpDouble.clazz			= tpDoubleClazz;
//		typeHash.remove(tpDoubleClazz.type);
		tpDoubleClazz.type		= tpDouble;
		tpDoubleClazz.setResolved(true);
		tpDouble.flags			= flResolved | flFloat | flDoubleSize;
		tpDouble.signature		= KString.from("D");
		tpDouble.java_signature		= KString.from("D");
		typeHash.put(tpDouble);

		Struct tpVoidClazz = Env.newStruct(new ClazzName(
							KString.from("void"),
							KString.from("void"),
							KString.from("V"),false,false),null,ACC_PUBLIC);
		tpVoid					= new BaseType();
		tpVoid.clazz			= tpVoidClazz;
//		typeHash.remove(tpVoidClazz.type);
		tpVoidClazz.type		= tpVoid;
		tpVoidClazz.setResolved(true);
		tpVoid.flags			= flResolved;
		tpVoid.signature		= KString.from("V");
		tpVoid.java_signature		= KString.from("V");
		typeHash.put(tpVoid);

		Struct java_lang = Env.newPackage(KString.from("java.lang"));
		Struct java_lang_annotation = Env.newPackage(KString.from("java.lang.annotation"));
		Struct java_util = Env.newPackage(KString.from("java.util"));
		Struct kiev_stdlib = Env.newPackage(KString.from("kiev.stdlib"));
		Struct kiev_stdlib_meta = Env.newPackage(KString.from("kiev.stdlib.meta"));

		Struct tpObjectClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Object;")),java_lang,ACC_PUBLIC);
		tpObject				= new BaseType(tpObjectClazz);
		tpObjectClazz.type		= tpObject;
		tpObject.flags			= flReference;
		typeHash.put(tpObject);

		Struct tpClassClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Class;")),java_lang,ACC_PUBLIC|ACC_FINAL);
		tpClass				= new BaseType(tpClassClazz);
		tpClassClazz.type		= tpClass;
		tpClass.flags			= flReference;
		typeHash.put(tpClass);

		Struct tpDebugClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Debug;")),kiev_stdlib,ACC_PUBLIC);
		tpDebug				= new BaseType(tpDebugClazz);
		tpDebugClazz.type	= tpDebug;
		tpDebug.flags		= flReference;
		typeHash.put(tpDebug);

		Struct tpTypeInfoClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfo;")),kiev_stdlib,ACC_PUBLIC|ACC_FINAL);
		tpTypeInfo				= new BaseType(tpTypeInfoClazz);
		tpTypeInfoClazz.type	= tpTypeInfo;
		tpTypeInfo.flags		= flReference;
		typeHash.put(tpTypeInfo);

		Struct tpTypeInfoInterfaceClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoInterface;")),kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE);
		tpTypeInfoInterface				= new BaseType(tpTypeInfoInterfaceClazz);
		tpTypeInfoInterfaceClazz.type	= tpTypeInfoInterface;
		tpTypeInfoInterface.flags		= flReference;
		typeHash.put(tpTypeInfoInterface);

		Struct tpNullClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Null;")),kiev_stdlib,ACC_PUBLIC);
		tpNull					= new BaseType(tpNullClazz);
		tpNullClazz.type		= tpNull;
		tpNull.flags			= flResolved | flReference;
		tpNull.clazz.super_type = tpObject;
		tpNull.clazz.setResolved(true);
		typeHash.put(tpNull);

		Struct tpCloneableClazz = Env.newInterface(ClazzName.fromSignature(KString.from("Ljava/lang/Cloneable;")),java_lang,ACC_PUBLIC|ACC_INTERFACE);
		tpCloneable				= new BaseType(tpCloneableClazz);
		tpCloneableClazz.type	= tpCloneable;
		tpCloneable.flags		= flReference;
		tpCloneableClazz.setInterface(true);
		typeHash.put(tpCloneable);


		Struct tpArrayClazz		= Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Array;")),kiev_stdlib,ACC_PUBLIC);
		ArgumentType tpArrayArg	= ArgumentType.newArgumentType(tpArrayClazz,KString.from("elem"));
		tpArray					= new BaseType(tpArrayClazz,new Type[]{tpArrayArg});
		tpArrayClazz.type		= tpArray;
		tpArrayClazz.super_type	= tpObject;
		tpArray.flags			|= flResolved;
		tpArrayClazz.setResolved(true);
		tpArray.flags			= flReference | flArray;
		tpArrayClazz.interfaces.add(new TypeRef(tpCloneable));
//		tpArrayClazz.fields = new Field[]{new Field(KString.from("length"),tpInt,(short)(Constants.ACC_FINAL|Constants.ACC_PUBLIC))};
		typeHash.put(tpArray);

		Struct tpBooleanRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Boolean;")),java_lang,ACC_PUBLIC);
		tpBooleanRef			= new BaseType(tpBooleanRefClazz);
		tpBooleanRefClazz.type	= tpBooleanRef;
		typeHash.put(tpBooleanRef);

		Struct tpCharRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Character;")),java_lang,ACC_PUBLIC);
		tpCharRef			= new BaseType(tpCharRefClazz);
		tpCharRefClazz.type	= tpCharRef;
		typeHash.put(tpCharRef);

		Struct tpNumberRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Number;")),java_lang,ACC_PUBLIC);
		tpNumberRef			= new BaseType(tpNumberRefClazz);
		tpNumberRefClazz.type	= tpNumberRef;
		typeHash.put(tpNumberRef);

		Struct tpByteRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Byte;")),java_lang,ACC_PUBLIC);
		tpByteRef			= new BaseType(tpByteRefClazz);
		tpByteRefClazz.type	= tpByteRef;
		typeHash.put(tpByteRef);

		Struct tpShortRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Short;")),java_lang,ACC_PUBLIC);
		tpShortRef			= new BaseType(tpShortRefClazz);
		tpShortRefClazz.type	= tpShortRef;
		typeHash.put(tpShortRef);

		Struct tpIntRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Integer;")),java_lang,ACC_PUBLIC);
		tpIntRef			= new BaseType(tpIntRefClazz);
		tpIntRefClazz.type	= tpIntRef;
		typeHash.put(tpIntRef);

		Struct tpLongRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Long;")),java_lang,ACC_PUBLIC);
		tpLongRef			= new BaseType(tpLongRefClazz);
		tpLongRefClazz.type	= tpLongRef;
		typeHash.put(tpLongRef);

		Struct tpFloatRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Float;")),java_lang,ACC_PUBLIC);
		tpFloatRef			= new BaseType(tpFloatRefClazz);
		tpFloatRefClazz.type	= tpFloatRef;
		typeHash.put(tpFloatRef);

		Struct tpDoubleRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Double;")),java_lang,ACC_PUBLIC);
		tpDoubleRef			= new BaseType(tpDoubleRefClazz);
		tpDoubleRefClazz.type	= tpDoubleRef;
		typeHash.put(tpDoubleRef);

		Struct tpVoidRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Void;")),java_lang,ACC_PUBLIC);
		tpVoidRef			= new BaseType(tpVoidRefClazz);
		tpVoidRefClazz.type	= tpVoidRef;
		typeHash.put(tpVoidRef);

		Struct tpStringClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/String;")),java_lang,ACC_PUBLIC);
		tpString				= new BaseType(tpStringClazz);
		tpStringClazz.type		= tpString;
		typeHash.put(tpString);

		Struct tpAnnotationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/annotation/Annotation;")),java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
		tpAnnotation			= new BaseType(tpAnnotationClazz);
		tpAnnotationClazz.type	= tpAnnotation;
		typeHash.put(tpAnnotation);
		
		Struct tpThrowableClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Throwable;")),java_lang,ACC_PUBLIC);
		tpThrowable				= new BaseType(tpThrowableClazz);
		tpThrowableClazz.type	= tpThrowable;
		typeHash.put(tpThrowable);

		Struct tpErrorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Error;")),java_lang,ACC_PUBLIC);
		tpError				= new BaseType(tpErrorClazz);
		tpErrorClazz.type	= tpError;
		typeHash.put(tpError);

		Struct tpExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Exception;")),java_lang,ACC_PUBLIC);
		tpException				= new BaseType(tpExceptionClazz);
		tpExceptionClazz.type	= tpException;
		typeHash.put(tpException);

		Struct tpCastExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/ClassCastException;")),java_lang,ACC_PUBLIC);
		tpCastException				= new BaseType(tpCastExceptionClazz);
		tpCastExceptionClazz.type	= tpCastException;
		typeHash.put(tpCastException);

		Struct tpRuntimeExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/RuntimeException;")),java_lang,ACC_PUBLIC);
		tpRuntimeException				= new BaseType(tpRuntimeExceptionClazz);
		tpRuntimeExceptionClazz.type	= tpRuntimeException;
		typeHash.put(tpRuntimeException);

		Struct tpAssertExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/AssertionFailedException;")),kiev_stdlib,ACC_PUBLIC);
		tpAssertException				= new BaseType(tpAssertExceptionClazz);
		tpAssertExceptionClazz.type	= tpAssertException;
		typeHash.put(tpAssertException);

//		Struct tpMessageExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/MessageNotUnderstoodException;")),kiev_stdlib,ACC_PUBLIC);
//		tpMessageException				= new Type(tpMessageExceptionClazz);
//		tpMessageExceptionClazz.type	= tpMessageException;
//		typeHash.put(tpMessageException);

//		Struct tpApplayableClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Applayable;")),kiev_stdlib,ACC_PUBLIC | ACC_INTERFACE);
//		tpApplayable				= new Type(tpApplayableClazz);
//		tpApplayableClazz.type		= tpApplayable;
//		typeHash.put(tpApplayable);

//		Struct tpDynamicClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Dynamic;")),kiev_stdlib,ACC_PUBLIC | ACC_INTERFACE);
//		tpDynamic				= new Type(tpDynamicClazz);
//		tpDynamicClazz.type		= tpDynamic;
//		typeHash.put(tpDynamic);

//		Struct tpEnumClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enum;")),kiev_stdlib,ACC_PUBLIC | ACC_ABSTRACT);
		Struct tpEnumClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Enum;")),java_lang,ACC_PUBLIC | ACC_ABSTRACT);
		tpEnum					= new BaseType(tpEnumClazz);
		tpEnumClazz.type		= tpEnum;
		typeHash.put(tpEnum);

		tpMethodClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/method;")),kiev_stdlib,ACC_PUBLIC);
        tpMethodClazz.setResolved(true);
		tpMethod		= new BaseType(tpMethodClazz);
//		 new MethodType(tpMethodClazz,Type.tpVoid,Type.emptyArray,Type.emptyArray);
		tpMethodClazz.type	= tpMethod;
        tpMethod.flags = flResolved;

		tpClosureClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/closure;")),kiev_stdlib,ACC_PUBLIC);
		tpClosure				= new BaseType(tpClosureClazz);
		tpClosureClazz.type		= tpClosure;

/*		Struct tpCellClazz	= Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell;")),kiev_stdlib,ACC_PUBLIC);
		tpCell				= new Type(tpCellClazz);
		tpCellClazz.type	= tpCell;
		typeHash.put(tpCell);
*/
		Struct tpJavaEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/util/Enumeration;")),java_util,ACC_PUBLIC);
		tpJavaEnumeration	= new BaseType(tpJavaEnumerationClazz);
		tpJavaEnumerationClazz.type	= tpJavaEnumeration;
		typeHash.put(tpJavaEnumeration);

		Struct tpKievEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enumeration;")),kiev_stdlib,ACC_PUBLIC);
		ArgumentType tpKievEnumerationArg = ArgumentType.newArgumentType(tpKievEnumerationClazz,KString.from("A"));
		tpKievEnumeration	= new BaseType(tpKievEnumerationClazz,new Type[]{tpKievEnumerationArg});
		tpKievEnumerationClazz.type	= tpKievEnumeration;
		typeHash.put(tpKievEnumeration);

/*		Struct tpCellObjectClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_Object;")),kiev_stdlib,ACC_PUBLIC);
		tpCellObject			= new Type(tpCellObjectClazz);
		tpCellObjectClazz.type	= tpCellObject;
		typeHash.put(tpCellObject);

		Struct tpCellBooleanClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_boolean;")),kiev_stdlib,ACC_PUBLIC);
		tpCellBoolean			= new Type(tpCellBooleanClazz);
		tpCellBooleanClazz.type	= tpCellBoolean;
		typeHash.put(tpCellBoolean);

		Struct tpCellByteClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_byte;")),kiev_stdlib,ACC_PUBLIC);
		tpCellByte			= new Type(tpCellByteClazz);
		tpCellByteClazz.type	= tpCellByte;
		typeHash.put(tpCellByte);

		Struct tpCellCharClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_char;")),kiev_stdlib,ACC_PUBLIC);
		tpCellChar			= new Type(tpCellCharClazz);
		tpCellCharClazz.type	= tpCellChar;
		typeHash.put(tpCellChar);

		Struct tpCellShortClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_short;")),kiev_stdlib,ACC_PUBLIC);
		tpCellShort			= new Type(tpCellShortClazz);
		tpCellShortClazz.type	= tpCellShort;
		typeHash.put(tpCellShort);

		Struct tpCellIntClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_int;")),kiev_stdlib,ACC_PUBLIC);
		tpCellInt			= new Type(tpCellIntClazz);
		tpCellIntClazz.type	= tpCellInt;
		typeHash.put(tpCellInt);

		Struct tpCellLongClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_long;")),kiev_stdlib,ACC_PUBLIC);
		tpCellLong			= new Type(tpCellLongClazz);
		tpCellLongClazz.type	= tpCellLong;
		typeHash.put(tpCellLong);

		Struct tpCellFloatClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_float;")),kiev_stdlib,ACC_PUBLIC);
		tpCellFloat			= new Type(tpCellFloatClazz);
		tpCellFloatClazz.type	= tpCellFloat;
		typeHash.put(tpCellFloat);

		Struct tpCellDoubleClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell_double;")),kiev_stdlib,ACC_PUBLIC);
		tpCellDouble			= new Type(tpCellDoubleClazz);
		tpCellDoubleClazz.type	= tpCellDouble;
		typeHash.put(tpCellDouble);
*/
//		Struct tpPrologEnvClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/PEnv;")),kiev_stdlib,ACC_PUBLIC);
//		tpPrologEnv				= new Type(tpPrologEnvClazz);
//		tpPrologEnvClazz.type	= tpPrologEnv;
//		typeHash.put(tpPrologEnv);

		Struct tpPrologVarClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/PVar;")),kiev_stdlib,ACC_PUBLIC);
		ArgumentType tpPrologVarArg = ArgumentType.newArgumentType(tpPrologVarClazz,KString.from("A"));
		tpPrologVar	= new BaseType(tpPrologVarClazz,new Type[]{tpPrologVarArg});
		tpPrologVarClazz.type	= tpPrologVar;
//		tpPrologVarClazz.setWrapper(true);
		typeHash.put(tpPrologVar);

		Struct tpRefProxyClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Ref;")),kiev_stdlib,ACC_PUBLIC);
		ArgumentType tpRefProxyArg = ArgumentType.newArgumentType(tpRefProxyClazz,KString.from("A"));
		tpRefProxy	= new BaseType(tpRefProxyClazz,new Type[]{tpRefProxyArg});
		tpRefProxyClazz.type	= tpRefProxy;
//		tpRefProxyClazz.setWrapper(true);
		typeHash.put(tpRefProxy);

		Struct tpTypeSwitchHashClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeSwitchHash;")),kiev_stdlib,ACC_PUBLIC);
		tpTypeSwitchHash			= new BaseType(tpTypeSwitchHashClazz);
		tpTypeSwitchHashClazz.type	= tpTypeSwitchHash;
		typeHash.put(tpTypeSwitchHash);

	}
}

