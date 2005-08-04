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

import static kiev.stdlib.Debug.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Type.java,v 1.5.2.1.2.1 1999/05/29 21:03:12 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.1 $
 *
 */

@node(copyable=false)
public class Type extends ASTNode implements AccessFlags {
	public static Type[]	emptyArray = new Type[0];

	public static final int flReference			= 1;
	public static final int flIntegerInCode		= 2;
	public static final int flInteger			= 4;
	public static final int flFloatInCode		= 8;
	public static final int flFloat				= 16;
	public static final int flNumber			= 20;
	public static final int flDoubleSize		= 32;
	public static final int flArray				= 64;
	public static final int flResolved			= 128;
	public static final int flBoolean			= 256;
	public static final int flArgumented		= 512;

	public static Hash<Type>	typeHash;

	@ref public Struct			clazz;
	public Type[]			args = Type.emptyArray;
	public KString			signature;
	public KString			java_signature;
	public int				flags;

	@ref public static Type tpAny;
	@ref public static Type tpVoid;
	@ref public static Type tpRule;
	@ref public static Type tpBoolean;
	@ref public static Type tpByte;
	@ref public static Type tpChar;
	@ref public static Type tpShort;
	@ref public static Type tpInt;
	@ref public static Type tpLong;
	@ref public static Type tpFloat;
	@ref public static Type tpDouble;
	@ref public static Type tpBooleanRef;
	@ref public static Type tpByteRef;
	@ref public static Type tpCharRef;
	@ref public static Type tpNumberRef;
	@ref public static Type tpShortRef;
	@ref public static Type tpIntRef;
	@ref public static Type tpLongRef;
	@ref public static Type tpFloatRef;
	@ref public static Type tpDoubleRef;
	@ref public static Type tpVoidRef;
	@ref public static Type tpNull;
	@ref public static Type tpObject;
	@ref public static Type tpClass;
	@ref public static Type tpDebug;
	@ref public static Type tpTypeInfo;
	@ref public static Type tpTypeInfoInterface;
	@ref public static Type tpArray;
	@ref public static Type tpCloneable;
	@ref public static Type tpString;
	@ref public static Type tpThrowable;
	@ref public static Type tpError;
	@ref public static Type tpException;
	@ref public static Type tpCastException;
	@ref public static Type tpJavaEnumeration;
	@ref public static Type tpKievEnumeration;
	@ref public static Type tpRuntimeException;
	@ref public static Type tpAssertException;
	//@ref public static Type tpMessageException;
	//@ref public static Type tpApplayable;
	//@ref public static Type tpDynamic;
	@ref public static Type tpEnum;
	@ref public static Type tpAnnotation;
	@ref public static Struct tpClosureClazz;
	@ref public static Struct tpMethodClazz;

/*	public static Type tpCell;
	public static Type tpCellObject;
	public static Type tpCellBoolean;
	public static Type tpCellByte;
	public static Type tpCellChar;
	public static Type tpCellShort;
	public static Type tpCellInt ;
	public static Type tpCellLong;
	public static Type tpCellFloat;
	public static Type tpCellDouble;
*/
//	public static Type tpPrologEnv;
	@ref public static Type tpPrologVar;
	@ref public static Type tpRefProxy;

	@ref public static Type tpTypeSwitchHash;

	public static void InitializeTypes() {
		typeHash = new Hash<Type>();

		Struct tpAnyClazz = Env.newStruct(new ClazzName(
							KString.from("<any>"),
							KString.from("<any>"),
							KString.from("?"),false,false),null,ACC_PUBLIC);
		tpAny				= new Type();
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
		tpRule					= new Type();
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
		tpBoolean				= new Type();
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
		tpByte					= new Type();
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
		tpChar					= new Type();
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
		tpShort					= new Type();
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
		tpInt					= new Type();
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
		tpLong					= new Type();
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
		tpFloat					= new Type();
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
		tpDouble				= new Type();
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
		tpVoid					= new Type();
		tpVoid.clazz			= tpVoidClazz;
//		typeHash.remove(tpVoidClazz.type);
		tpVoidClazz.type		= tpVoid;
		tpVoidClazz.setResolved(true);
		tpVoid.flags			= flResolved;
		tpVoid.signature		= KString.from("V");
		tpVoid.java_signature		= KString.from("V");
		typeHash.put(tpVoid);
		Env.root.type = tpVoid;

		Struct java_lang = Env.newPackage(KString.from("java.lang"));
		Struct java_lang_annotation = Env.newPackage(KString.from("java.lang.annotation"));
		Struct java_util = Env.newPackage(KString.from("java.util"));
		Struct kiev_stdlib = Env.newPackage(KString.from("kiev.stdlib"));
		Struct kiev_stdlib_meta = Env.newPackage(KString.from("kiev.stdlib.meta"));

		Struct tpObjectClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Object;")),java_lang,ACC_PUBLIC);
		tpObject				= new Type(tpObjectClazz);
		tpObjectClazz.type		= tpObject;
		tpObject.flags			= flReference;
		typeHash.put(tpObject);

		Struct tpClassClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Class;")),java_lang,ACC_PUBLIC|ACC_FINAL);
		tpClass				= new Type(tpClassClazz);
		tpClassClazz.type		= tpClass;
		tpClass.flags			= flReference;
		typeHash.put(tpClass);

		Struct tpDebugClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Debug;")),kiev_stdlib,ACC_PUBLIC);
		tpDebug				= new Type(tpDebugClazz);
		tpDebugClazz.type	= tpDebug;
		tpDebug.flags		= flReference;
		typeHash.put(tpDebug);

		Struct tpTypeInfoClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfo;")),kiev_stdlib,ACC_PUBLIC|ACC_FINAL);
		tpTypeInfo				= new Type(tpTypeInfoClazz);
		tpTypeInfoClazz.type	= tpTypeInfo;
		tpTypeInfo.flags		= flReference;
		typeHash.put(tpTypeInfo);

		Struct tpTypeInfoInterfaceClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoInterface;")),kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE);
		tpTypeInfoInterface				= new Type(tpTypeInfoInterfaceClazz);
		tpTypeInfoInterfaceClazz.type	= tpTypeInfoInterface;
		tpTypeInfoInterface.flags		= flReference;
		typeHash.put(tpTypeInfoInterface);

		Struct tpNullClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Null;")),kiev_stdlib,ACC_PUBLIC);
		tpNull					= new Type(tpNullClazz);
		tpNullClazz.type		= tpNull;
		tpNull.flags			= flResolved | flReference;
		tpNull.clazz.super_clazz= tpObject;
		tpNull.clazz.setResolved(true);
		typeHash.put(tpNull);

		Struct tpCloneableClazz = Env.newInterface(ClazzName.fromSignature(KString.from("Ljava/lang/Cloneable;")),java_lang,ACC_PUBLIC|ACC_INTERFACE);
		tpCloneable				= new Type(tpCloneableClazz);
		tpCloneableClazz.type	= tpCloneable;
		tpCloneable.flags		= flReference;
		tpCloneableClazz.setInterface(true);
		typeHash.put(tpCloneable);


		Struct tpArrayClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Array;")),kiev_stdlib,ACC_PUBLIC);
		Struct tpArrayArgClazz = Env.newArgument(KString.from("elem"),tpArrayClazz);
		Type tpArrayArg = new Type(tpArrayArgClazz);
		tpArray				= new Type(tpArrayClazz,new Type[]{tpArrayArg});
		tpArrayClazz.type		= tpArray;
		tpArrayClazz.super_clazz = tpObject;
		tpArray.flags			|= flResolved;
		tpArrayClazz.setResolved(true);
		tpArray.flags			= flReference | flArray;
		tpArrayClazz.interfaces.add(tpCloneable);
//		tpArrayClazz.fields = new Field[]{new Field(tpArrayClazz,KString.from("length"),tpInt,(short)(Constants.ACC_FINAL|Constants.ACC_PUBLIC))};
		typeHash.put(tpArray);

		Struct tpBooleanRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Boolean;")),java_lang,ACC_PUBLIC);
		tpBooleanRef			= new Type(tpBooleanRefClazz);
		tpBooleanRefClazz.type	= tpBooleanRef;
		typeHash.put(tpBooleanRef);

		Struct tpCharRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Character;")),java_lang,ACC_PUBLIC);
		tpCharRef			= new Type(tpCharRefClazz);
		tpCharRefClazz.type	= tpCharRef;
		typeHash.put(tpCharRef);

		Struct tpNumberRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Number;")),java_lang,ACC_PUBLIC);
		tpNumberRef			= new Type(tpNumberRefClazz);
		tpNumberRefClazz.type	= tpNumberRef;
		typeHash.put(tpNumberRef);

		Struct tpByteRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Byte;")),java_lang,ACC_PUBLIC);
		tpByteRef			= new Type(tpByteRefClazz);
		tpByteRefClazz.type	= tpByteRef;
		typeHash.put(tpByteRef);

		Struct tpShortRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Short;")),java_lang,ACC_PUBLIC);
		tpShortRef			= new Type(tpShortRefClazz);
		tpShortRefClazz.type	= tpShortRef;
		typeHash.put(tpShortRef);

		Struct tpIntRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Integer;")),java_lang,ACC_PUBLIC);
		tpIntRef			= new Type(tpIntRefClazz);
		tpIntRefClazz.type	= tpIntRef;
		typeHash.put(tpIntRef);

		Struct tpLongRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Long;")),java_lang,ACC_PUBLIC);
		tpLongRef			= new Type(tpLongRefClazz);
		tpLongRefClazz.type	= tpLongRef;
		typeHash.put(tpLongRef);

		Struct tpFloatRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Float;")),java_lang,ACC_PUBLIC);
		tpFloatRef			= new Type(tpFloatRefClazz);
		tpFloatRefClazz.type	= tpFloatRef;
		typeHash.put(tpFloatRef);

		Struct tpDoubleRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Double;")),java_lang,ACC_PUBLIC);
		tpDoubleRef			= new Type(tpDoubleRefClazz);
		tpDoubleRefClazz.type	= tpDoubleRef;
		typeHash.put(tpDoubleRef);

		Struct tpVoidRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Void;")),java_lang,ACC_PUBLIC);
		tpVoidRef			= new Type(tpVoidRefClazz);
		tpVoidRefClazz.type	= tpVoidRef;
		typeHash.put(tpVoidRef);

		Struct tpStringClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/String;")),java_lang,ACC_PUBLIC);
		tpString				= new Type(tpStringClazz);
		tpStringClazz.type		= tpString;
		typeHash.put(tpString);

		Struct tpAnnotationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/annotation/Annotation;")),java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
		tpAnnotation			= new Type(tpAnnotationClazz);
		tpAnnotationClazz.type	= tpAnnotation;
		typeHash.put(tpAnnotation);
		
		Struct tpThrowableClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Throwable;")),java_lang,ACC_PUBLIC);
		tpThrowable				= new Type(tpThrowableClazz);
		tpThrowableClazz.type	= tpThrowable;
		typeHash.put(tpThrowable);

		Struct tpErrorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Error;")),java_lang,ACC_PUBLIC);
		tpError				= new Type(tpErrorClazz);
		tpErrorClazz.type	= tpError;
		typeHash.put(tpError);

		Struct tpExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Exception;")),java_lang,ACC_PUBLIC);
		tpException				= new Type(tpExceptionClazz);
		tpExceptionClazz.type	= tpException;
		typeHash.put(tpException);

		Struct tpCastExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/ClassCastException;")),java_lang,ACC_PUBLIC);
		tpCastException				= new Type(tpCastExceptionClazz);
		tpCastExceptionClazz.type	= tpCastException;
		typeHash.put(tpCastException);

		Struct tpRuntimeExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/RuntimeException;")),java_lang,ACC_PUBLIC);
		tpRuntimeException				= new Type(tpRuntimeExceptionClazz);
		tpRuntimeExceptionClazz.type	= tpRuntimeException;
		typeHash.put(tpRuntimeException);

		Struct tpAssertExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/AssertionFailedException;")),kiev_stdlib,ACC_PUBLIC);
		tpAssertException				= new Type(tpAssertExceptionClazz);
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

		Struct tpEnumClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enum;")),kiev_stdlib,ACC_PUBLIC | ACC_ABSTRACT);
		tpEnum					= new Type(tpEnumClazz);
		tpEnumClazz.type		= tpEnum;
		typeHash.put(tpEnum);

		tpMethodClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/method;")),kiev_stdlib,ACC_PUBLIC);
        tpMethodClazz.setResolved(true);
		Type tpMethod		= new Type(tpMethodClazz);
//		 new MethodType(tpMethodClazz,Type.tpVoid,Type.emptyArray,Type.emptyArray);
		tpMethodClazz.type	= tpMethod;
        tpMethod.flags = flResolved;

		tpClosureClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/closure;")),kiev_stdlib,ACC_PUBLIC);
		Type tpClosure			= new Type(tpClosureClazz);
		tpClosureClazz.type		= tpClosure;

/*		Struct tpCellClazz	= Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Cell;")),kiev_stdlib,ACC_PUBLIC);
		tpCell				= new Type(tpCellClazz);
		tpCellClazz.type	= tpCell;
		typeHash.put(tpCell);
*/
		Struct tpJavaEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/util/Enumeration;")),java_util,ACC_PUBLIC);
		tpJavaEnumeration	= new Type(tpJavaEnumerationClazz);
		tpJavaEnumerationClazz.type	= tpJavaEnumeration;
		typeHash.put(tpJavaEnumeration);

		Struct tpKievEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enumeration;")),kiev_stdlib,ACC_PUBLIC);
		Struct tpKievEnumerationArgClazz = Env.newArgument(KString.from("A"),tpKievEnumerationClazz);
		Type tpKievEnumerationArg = new Type(tpKievEnumerationArgClazz);
		tpKievEnumeration	= new Type(tpKievEnumerationClazz,new Type[]{tpKievEnumerationArg});
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
		tpPrologVarClazz.setWrapper(true);
		Struct tpPrologVarArgClazz = Env.newArgument(KString.from("A"),tpPrologVarClazz);
		Type tpPrologVarArg = new Type(tpPrologVarArgClazz);
		tpPrologVar	= new Type(tpPrologVarClazz,new Type[]{tpPrologVarArg});
		tpPrologVarClazz.type	= tpPrologVar;
		typeHash.put(tpPrologVar);

		Struct tpRefProxyClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Ref;")),kiev_stdlib,ACC_PUBLIC);
		tpRefProxyClazz.setWrapper(true);
		Struct tpRefProxyArgClazz = Env.newArgument(KString.from("A"),tpRefProxyClazz);
		Type tpRefProxyArg = new Type(tpRefProxyArgClazz);
		tpRefProxy	= new Type(tpRefProxyClazz,new Type[]{tpRefProxyArg});
		tpRefProxyClazz.type	= tpRefProxy;
		typeHash.put(tpRefProxy);

		Struct tpTypeSwitchHashClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeSwitchHash;")),kiev_stdlib,ACC_PUBLIC);
		tpTypeSwitchHash			= new Type(tpTypeSwitchHashClazz);
		tpTypeSwitchHashClazz.type	= tpTypeSwitchHash;
		typeHash.put(tpTypeSwitchHash);

	}

	protected Type() { super(0); }

	protected Type(Struct clazz) {
		super(0);
		this.clazz = clazz;
		signature = Signature.from(clazz, null, null, null);
		java_signature = Signature.getJavaSignature(signature);
		flags = flReference;
		if( clazz.isArgument() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New type created: "+this+" with signature "+signature);
	}

	protected Type(Struct clazz, Type[] args) {
		super(0);
		this.clazz = clazz;
		signature = Signature.from(clazz, null, args, null);
		if( args != null && args.length > 0 ) {
			this.args = args;
			if( clazz.gens.length > 0 ) {
				boolean best_found = false;
				int i,j;
		next_gen:
				for(i=0; i < clazz.gens.length && clazz.gens[i] != null; i++) {
					for(j=0; j < args.length; j++) {
						if( !(clazz.gens[i].type.args[j].isReference() && args[j].isReference()
							|| clazz.gens[i].type.args[j] == args[j]
						))
							continue next_gen;
					}
					this.clazz = clazz = clazz.gens[i];
					best_found = true;
					break;
				}
				if( !best_found ) {
			next_gen1:
					for(i=0; i < clazz.gens.length && clazz.gens[i] != null; i++) {
						for(j=0; j < args.length; j++) {
							Type gt = clazz.gens[i].type.args[j];
							if( !( gt.isReference() && args[j].isReference()
								|| gt == args[j]
								|| (gt == Type.tpInt && args[j].isIntegerInCode())
								|| (gt == Type.tpFloat && args[j] == Type.tpDouble)
							))
								continue next_gen1;
						}
						this.clazz = clazz = clazz.gens[i];
						break;
					}
				}
			}
			java_signature = Signature.from(clazz, null, null, null);
		} else {
			args = emptyArray;
			java_signature = signature;
		}
		java_signature = Signature.getJavaSignature(java_signature);
		flags = flReference;
		if( clazz.isArgument() ) flags |= flArgumented;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		typeHash.put(this);
		trace(Kiev.debugCreation,"New type created: "+this
			+" with signature "+signature+" / "+java_signature);
	}

	protected Type(ClazzName name, Type[] args) {
		this(Env.newStruct(name),args);
	}

	public Object copy() {
		throw new CompilerException(getPos(),"Type node cannot be copied");
	};

	public static Type newJavaRefType(Struct clazz) {
		Type[] args = Type.emptyArray;
		KString signature = Signature.from(clazz,null,null,null);
		Type t = typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			t.flags |= flReference;
			return t;
		}
		t = new Type(clazz);
		t.flags |= flReference;
		if (clazz.isEnum() && clazz.isPrimitiveEnum())
			t.setMeAsPrimitiveEnum();
		return t;
	}

	public void setMeAsPrimitiveEnum() {
		flags &= ~flReference;
		flags |= flIntegerInCode;
		java_signature = clazz.getPrimitiveEnumType().java_signature;
	}

	public static Type newRefType(Struct clazz) {
		if( clazz != null && clazz.type != null && clazz.type.args.length > 0 )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.type.args.length+" arguments");
		Type[] args = Type.emptyArray;
		KString signature = Signature.from(clazz,null,null,null);
		Type t = typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			t.flags |= flReference;
			return t;
		}
		t = new Type(clazz);
		t.flags |= flReference;
		return t;
	}

	public static Type newRefType(Struct clazz, Type[] args) {
		if( clazz != null && clazz.type != null && clazz.type.args.length != args.length )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.type.args.length+" arguments");
		if( clazz != null && clazz.type != null ) {
			for(int i=0; i < args.length; i++) {
				if( !args[i].isInstanceOf(clazz.type.args[i]) ) {
					if( clazz.type.args[i].clazz.super_clazz == Type.tpObject && !args[i].isReference())
						;
					else
						throw new RuntimeException("Type "+args[i]+" must be an instance of "+clazz.type.args[i]);
				}
			}
		}
		KString signature = Signature.from(clazz,null,args,null);
		Type t = typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			return t;
		}
		t = new Type(clazz,args);
		t.flags |= flReference;
		return t;
	}

	public static Type newRefType(ClazzName name) {
		return newRefType(Env.newStruct(name));
	}

	public static Type newRefType(ClazzName name, Type[] args) {
		return newRefType(Env.newStruct(name),args);
	}

	public static Type newArrayType(Type type) {
		KString sign = new KStringBuffer(type.signature.len).append('[').append(type.signature).toKString();
		Type t = typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		t = new Type();
		t.clazz = tpArray.clazz;
		t.args = new Type[]{type};
		t.signature = sign;
		t.java_signature = new KStringBuffer(type.java_signature.len+1).append_fast((byte)'[')
			.append_fast(type.java_signature).toKString();
		t.flags	 |= flReference | flArray;
		if( t.args[0].isArgumented() ) t.flags |= flArgumented;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type created: "+t
			+" with signature "+t.signature+" / "+t.java_signature);
		return t;
	}

	public static Type fromSignature(KString sig) {
		switch( sig.byteAt(0) ) {
		case 'V':		return tpVoid;
		case 'Z':		return tpBoolean;
		case 'C':		return tpChar;
		case 'B':		return tpByte;
		case 'S':		return tpShort;
		case 'I':		return tpInt;
		case 'J':		return tpLong;
		case 'F':		return tpFloat;
		case 'D':		return tpDouble;
		default:
			return Signature.getType(new KString.KStringScanner(sig));
		}
	}

	public int hashCode() { return signature.hashCode(); }

	public void cleanup() {
		// Type is persistent
	}

	public String toString() {
		if( isArray() )
			return args[0]+"[]";
		StringBuffer str = new StringBuffer();
		if( clazz.generated_from != null )
			str.append(clazz.generated_from.name.toString());
		else
			str.append(clazz.name.toString());
		if( args != null && args.length > 0 ) {
			str.append('<');
			for(int i=0; i < args.length; i++) {
				str.append(args[i]);
				if( i < args.length-1)
					str.append(',');
			}
			str.append('>');
		}
		return str.toString();
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public boolean equals(Object to) {
		if(to != null && to instanceof Type ) return equals((Type)to);
		return false;
	}

	public boolean string_equals(Type to) {
		return signature.equals( to.signature );
	}

	public boolean equals(Type to) {
		if( signature.equals( ((Type)to).signature ) ) return true;
		else if( this.isBoolean() && to.isBoolean() ) return true;
		else if( clazz.isArgument() ) return clazz.super_clazz.equals(to);
		else if( ((Type)to).clazz.isArgument() ) return this.equals(((Type)to).clazz.super_clazz);
		else return false;
	}

	public boolean checkResolved() {
		try {
			clazz.checkResolved();
		} catch(Exception e ) {
			if( Kiev.verbose ) e.printStackTrace( /* */System.out /* */ );
			throw new RuntimeException("Unresolved type:"+e);
		}
		return true;
	}

	public boolean isInstanceOf(Type t) {
		return isInstanceOf(this,t);
	}

	public static boolean isInstanceOf(Type t1, Type t2) {
		if( t1.equals(t2) ) return true;
		if( t1.isReference() && t2.equals(Type.tpObject) ) return true;
		try {
			t1.clazz.checkResolved();
			t2.clazz.checkResolved();
		} catch(Exception e ) {
			if( Kiev.verbose ) e.printStackTrace( /* */System.out /* */ );
			throw new RuntimeException("Unresolved type:"+e);
		}
		// Is this a case class without arguments?
//		if( t1.clazz.isPizzaCase() && t1.clazz.super_clazz.clazz.equals(t2.clazz) )
//			return true;
		// Instance of closure
		if( t1.clazz.instanceOf(Type.tpClosureClazz) &&
			t2.clazz.instanceOf(Type.tpClosureClazz) ) {
			if( t1.args.length != t2.args.length ) return false;
			for(int i=0; i < t1.args.length; i++)
				if( !isInstanceOf(t1.args[i],t2.args[i]) ) return false;
			return true;
		}
		// Check class1 == class2 && arguments
		if( t1.clazz != null && t2.clazz != null && t1.clazz.equals(t2.clazz) ) {
			int t1_args_len = t1.args==null?0:t1.args.length;
			int t2_args_len = t2.args==null?0:t2.args.length;
			if( t1_args_len != t2_args_len ) return false;
			if( t1_args_len == 0 ) return true;
			for(int i=0; i < t1.args.length; i++)
				if( !isInstanceOf(t1.args[i],t2.args[i]) ) return false;
			return true;
		}
		if( t1.clazz.super_clazz != null
		 && isInstanceOf(Type.getRealType(t1,t1.clazz.super_clazz),t2) ) return true;
		for(int i=0; t1.clazz.interfaces!=null && i < t1.clazz.interfaces.length; i++)
			if( isInstanceOf(t1.clazz.interfaces[i],t2) ) return true;
		return false;
	}

	public boolean codeEquivalentTo(Type t) {
		if( this.equals(t) ) return true;
		if( isIntegerInCode() && t.isIntegerInCode() ) return true;
		if( isReference() && t.isReference() && isInstanceOf(t) ) return true;
		return false;
	}

	public boolean isAutoCastableTo(Type t)
	{
		if( t == Type.tpVoid ) return true;
		if( this.isReference() && t.isReference() && (this==tpNull || t==tpNull) ) return true;
		if( isInstanceOf(t) ) return true;
		if( this.isReference() && t.isReference() && this.clazz.package_clazz.isClazz()
		 && !this.clazz.isArgument()
		 && !this.clazz.isStatic() && this.clazz.package_clazz.type.isAutoCastableTo(t)
		)
			return true;
		if( this == Type.tpRule && t == Type.tpBoolean ) return true;
		if( this.isBoolean() && t.isBoolean() ) return true;
		if( this.isReference() && !t.isReference() ) {
			if( getRefTypeForPrimitive(t) == this ) return true;
			else if( !Kiev.javaMode && t==Type.tpInt && this.isInstanceOf(Type.tpEnum) )
				return true;
		}
		if( !this.isReference() && !t.isReference() && !Kiev.javaMode && clazz.isPrimitiveEnum() ) {
			return clazz.getPrimitiveEnumType().isAutoCastableTo(t);
		}
		if( this.isReference() && !t.isReference() ) {
			if( getRefTypeForPrimitive(this) == t ) return true;
			else if( !Kiev.javaMode && this==Type.tpInt && t.isInstanceOf(Type.tpEnum) ) return true;
		}
		if( this==tpByte && ( t==tpShort || t==tpInt || t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( (this==tpShort || this==tpChar) && (t==tpInt || t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( this==tpInt && (t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( this==tpLong && ( t==tpFloat || t==tpDouble) ) return true;
		if( this==tpFloat && t==tpDouble ) return true;
		if( this.clazz.instanceOf(tpPrologVar.clazz) || t.clazz.instanceOf(tpPrologVar.clazz) ) {
			if( this.clazz.instanceOf(tpPrologVar.clazz) && t.clazz.instanceOf(tpPrologVar.clazz) )
				return this.args[0].isAutoCastableTo(t.args[0]);
			else if( this.clazz.instanceOf(tpPrologVar.clazz) && args[0].isAutoCastableTo(t) ) return true;
			else if( t.clazz.instanceOf(tpPrologVar.clazz) && this.isAutoCastableTo(t.args[0]) ) return true;
			return false;
		}
		if( this.clazz.isWrapper() ) {
			if( Type.getRealType(this,this.clazz.wrapped_field.type).isAutoCastableTo(t) ) return true;
			return false;
		}
		if( this instanceof MethodType
		 && this.clazz != tpMethodClazz
		 && !(t instanceof MethodType)
		 && this.args.length == 0
		 ) {
			if( ((MethodType)this).ret.isAutoCastableTo(t) ) return true;
		}
		return false;
	}

	public Type betterCast(Type t1, Type t2) {
		if( equals(t1) ) return t1;
		if( equals(t2) ) return t2;
		if( isBoolean() && t1.isBoolean() ) return t1;
		if( isBoolean() && t2.isBoolean() ) return t2;
		if( isNumber() ) {
			if( isInteger() ) {
				if( this == tpByte )
					if( t1==tpShort || t2==tpShort ) return tpShort;
					else if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpChar )
					if( t1==tpShort || t2==tpShort ) return tpShort;
					else if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpShort )
					if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpInt )
					if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
			} else {
				if( this == tpFloat )
					if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpDouble )
					if( t1==tpDouble || t2==tpDouble) return tpDouble;
					else return null;
			}
		}
		else if( this.isReference() ) {
			if( t1.isReference() && !t2.isReference() ) return t1;
			else if( !t1.isReference() && t2.isReference() ) return t2;
			else if( !t1.isReference() && !t2.isReference() ) return null;
			if( this == tpNull ) return null;
			if( isInstanceOf(t1) ) {
				if( !isInstanceOf(t2) ) return t1;
				else if( t2.isInstanceOf(t1) ) return t2;
				else return t1;
			}
			else if( isInstanceOf(t2) ) return t2;
			if( t1.clazz.instanceOf(Type.tpPrologVar.clazz) && t1.clazz.instanceOf(Type.tpPrologVar.clazz) ) {
				Type tp1 = t1.args[0];
				Type tp2 = t2.args[0];
				Type tp_better = betterCast(tp1,tp2);
				if( tp_better != null ) {
					if( tp_better == tp1 ) return t1;
					if( tp_better == tp2 ) return t2;
				}
			}
			return null;
		}
		return null;
	}

	public static Type leastCommonType(Type tp1, Type tp2) {
		Type tp = tp1;
		while( tp != null ) {
			if( tp1.isInstanceOf(tp) && tp2.isInstanceOf(tp) ) return tp;
			tp = tp.clazz.super_clazz;
		}
		return tp;
	}

	public static Type upperCastNumbers(Type tp1, Type tp2) {
		assert( tp1.isNumber() );
		assert( tp2.isNumber() );
		if( tp1==Type.tpDouble || tp2==Type.tpDouble) return Type.tpDouble;
		if( tp1==Type.tpFloat || tp2==Type.tpFloat) return Type.tpFloat;
		if( tp1==Type.tpLong || tp2==Type.tpLong) return Type.tpLong;
		if( tp1==Type.tpInt || tp2==Type.tpInt) return Type.tpInt;
		if( tp1==Type.tpChar || tp2==Type.tpChar) return Type.tpChar;
		if( tp1==Type.tpShort || tp2==Type.tpShort) return Type.tpShort;
		if( tp1==Type.tpByte || tp2==Type.tpByte) return Type.tpByte;
		throw new RuntimeException("Bad number types "+tp1+" or "+tp2);
	}

	public boolean isCastableTo(Type t) {
		if( isNumber() && t.isNumber() ) return true;
		if( this.isReference() && t.isReference() && (this==tpNull || t==tpNull) ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.isReference() && t.isReference() && (this.clazz.isInterface() || t.clazz.isInterface()) ) return true;
		if( this.isReference() && t.isReference() && this.clazz.package_clazz.isClazz()
		 && !this.clazz.isArgument()
		 && !this.clazz.isStatic() && this.clazz.package_clazz.type.isAutoCastableTo(t)
		)
			return true;
		if( t.clazz.isPrimitiveEnum())
			return this.isCastableTo(t.clazz.getPrimitiveEnumType());
		if( t.clazz.isEnum())
			return this.isCastableTo(Type.tpInt);
		if( t.clazz.isArgument() && isCastableTo(t.clazz.super_clazz) ) return true;
		if( t.clazz.isArgument() && !this.isReference() ) {
//			Kiev.reportWarning(0,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
			return true;
		}
		if( this instanceof MethodType
		 && this.clazz != tpMethodClazz
		 && !(t instanceof MethodType)
		 && this.args.length == 0
		 ) {
			if( ((MethodType)this).ret.isCastableTo(t) ) return true;
		}
		return false;
	}

	public static Type getRefTypeForPrimitive(Type tp) {
		if( tp.isReference() ) return tp;
		if( tp == Type.tpBoolean ) return Type.tpBooleanRef;
//		else if( tp == Type.tpRule ) return Type.tpBooleanRef;
		else if( tp == Type.tpByte ) return Type.tpByteRef;
		else if( tp == Type.tpShort ) return Type.tpShortRef;
		else if( tp == Type.tpInt ) return Type.tpIntRef;
		else if( tp == Type.tpLong ) return Type.tpLongRef;
		else if( tp == Type.tpFloat ) return Type.tpFloatRef;
		else if( tp == Type.tpDouble ) return Type.tpDoubleRef;
		else if( tp == Type.tpChar ) return Type.tpCharRef;
		else if( tp == Type.tpVoid ) return Type.tpVoidRef;
		else if( tp.clazz.isPrimitiveEnum() )
			return getRefTypeForPrimitive(tp.clazz.getPrimitiveEnumType());
		else
			throw new RuntimeException("Unknown primitive type "+tp);
	}

	public Type getNonArgsType() {
		if( clazz.isArgument() ) return clazz.super_clazz.getNonArgsType();
		if( args.length == 0 || isArray() ) return this;
		Type[] targs = clazz.type.args;
		Type[] jargs = new Type[targs.length];
		for(int i=0; i < targs.length; i++) {
			jargs[i] = targs[i].getNonArgsType();
		}
		return Type.newRefType(clazz,jargs);
	}


	public final boolean isReference()		{ return (flags & flReference)		!= 0 ; }
	public final boolean isArray()			{ return (flags & flArray)			!= 0 ; }
	public final boolean isIntegerInCode()	{ return (flags & flIntegerInCode)	!= 0 ; }
	public final boolean isInteger()		{ return (flags & flInteger)		!= 0 ; }
	public final boolean isFloatInCode()	{ return (flags & flFloatInCode)	!= 0 ; }
	public final boolean isFloat()			{ return (flags & flFloat)			!= 0 ; }
	public final boolean isNumber()			{ return (flags & flNumber)			!= 0 ; }
	public final boolean isDoubleSize()		{ return (flags & flDoubleSize)		!= 0 ; }
	public final boolean isResolved()		{ return (flags & flResolved)		!= 0 ; }
	public final boolean isBoolean()		{ return (flags & flBoolean)		!= 0 ; }
	public final boolean isArgumented()		{ return (flags & flArgumented)		!= 0 ; }

	public Type getJavaType() {
		if( !isReference() ) {
//			if( this == Type.tpRule ) return Type.tpBoolean;
			return this;
		}
		if( isArray() ) return newArrayType(args[0].getJavaType());
		if( clazz.isArgument() ) {
			if( Kiev.argtype != null ) {
				Type t = Type.getRealType(Kiev.argtype,this);
				if( t != this )
					return t;
			}
			return clazz.super_clazz.getJavaType();
		}
		if( this instanceof MethodType ) {
			if( clazz.instanceOf(Type.tpClosureClazz) )
				return newJavaRefType(clazz);
			if( args.length == 0 )
				return MethodType.newMethodType(null,null,Type.emptyArray,((MethodType)this).ret.getJavaType());
			Type[] targs = new Type[args.length];
			for(int i=0; i < args.length; i++)
				targs[i] = args[i].getJavaType();
			return MethodType.newMethodType(null,null,targs,((MethodType)this).ret.getJavaType());
		}
		if (clazz.isEnum() && clazz.isPrimitiveEnum())
			return clazz.getPrimitiveEnumType();
		if( args.length == 0 ) return this;
		return newJavaRefType(clazz);
	}

	private static int get_real_type_depth = 0;

	public static Type getRealType(Type t1, Type t2) {
		trace(Kiev.debugResolve,"Get real type of "+t2+" in "+t1);
		if( t2 == null ) return null;
		if( !t2.isArgumented() ) {
//			trace(Kiev.debugResolve,"Type "+t2+" is not argumented");
			return t2;
		}
		if( t1 == null || !t2.isReference() ) return t2;
		// No type for rewriting rules
		if( get_real_type_depth > 32 ) return t2;
		get_real_type_depth++;
		try {
		if( t1.isArray() ) return getRealType(t1.args[0],t2);
		if( t2.isArray() ) return Type.newArrayType(getRealType(t1,t2.args[0]));
//		makeTypeMap(tpmap=0,t1);
//		if( tpmap_top == 0 ) return t2;
		// Type does not containce rules
//		while( t1.args==null || t1.args.length==0 ) {
//			if( t1.clazz.super_clazz == null ) return t2;
//			t1 = t1.clazz.super_clazz;
//		}
		if( t2.clazz.isArgument() ) {
			for(int i=0; i < t1.args.length && i < t1.clazz.type.args.length; i++) {
				if( t1.clazz.type.args[i].string_equals(t2) ) {
					trace(Kiev.debugResolve,"type "+t2+" is resolved as "+t1.args[i]);
					return t1.args[i];
				}
				if( t1.clazz.generated_from != null ) {
					if( t1.clazz.generated_from.type.args[i].string_equals(t2) ) {
						trace(Kiev.debugResolve,"type "+t2+" is resolved as "+t1.args[i]);
						return t1.args[i];
					}
				}
/*				if( !t1.clazz.package_clazz.isPackage() ) {
					if( t1.clazz.package_clazz.type.args[i].string_equals(t2) ) {
						trace(Kiev.debugResolve,"type "+t2+" is resolved as "+t1.args[i]);
						return t1.args[i];
					}
				}
				if( !t1.clazz.package_clazz.isPackage() && t1.clazz.package_clazz.generated_from != null ) {
					if( t1.clazz.package_clazz.generated_from.type.args[i].string_equals(t2) ) {
						trace(Kiev.debugResolve,"type "+t2+" is resolved as "+t1.args[i]);
						return t1.args[i];
					}
				}
				trace(Kiev.debugResolve,"type "+t2+" is not an argument "+t1.clazz.type.args[i]);
*/			}
			// Search in super-class and super-interfaces
			Type tp;
			Struct rs = t1.clazz;
			if(	rs.super_clazz!=null
			&&  rs.super_clazz.args != null
			&&  (tp=getRealType(getRealType(t1,rs.super_clazz),t2))!=t2 )
				return tp;
			for(int i=0; rs.interfaces!=null && i < rs.interfaces.length; i++) {
				if( rs.interfaces[i].args!=null
				&& (tp=getRealType(getRealType(t1,rs.interfaces[i]),t2))!=t2 )
					return tp;
			}
			// Not found, return itself
			return t2;
		}
		// Well, isn't an argument, but may be a type with arguments
		if( t2.args.length == 0 && !(t2 instanceof MethodType) ) return t2;
		Type[] tpargs = new Type[t2.args.length];
		Type tpret = null;
		for(int i=0; i < tpargs.length; i++) {
			// Check it's not an infinite loop
			if( t2.args[i].string_equals(t2) )
				throw new RuntimeException("Ciclyc parameter # "+i+":"+t2.args[i]+" in type "+t2);
			tpargs[i] = getRealType(t1,t2.args[i]);
		}
		boolean isRewritten = false;
		if( t2 instanceof MethodType ) {
			tpret = getRealType(t1,((MethodType)t2).ret);
			if( tpret != ((MethodType)t2).ret ) isRewritten = true;
		}
		// Check if anything was rewritten
		for(int i=0; i < tpargs.length; i++) {
			if( tpargs[i] != t2.args[i] ) { isRewritten = true; break; }
		}
		if( isRewritten ) {
			Type tp;
			// Check we must return a MethodType, a ClosureType or an array
			if( t2.clazz == tpArray.clazz )
				tp = newArrayType(tpargs[0]);
			else if( t2 instanceof MethodType )
				tp = MethodType.newMethodType(t2.clazz,null,tpargs,tpret);
//			else if( t2.clazz == ClosureType.tpClosureClazz )
//				tp = ClosureType.newClosureType(tpargs,getRealType(t1,((ClosureType)t2).ret));
			else
				tp = newRefType(t2.clazz,tpargs);
			trace(Kiev.debugResolve,"Type "+t2+" rewritten into "+tp+" using "+t1);
			return tp;
		}
		// Nothing was rewritten...
		if( t1.clazz.super_clazz != null ) return getRealType(t1.clazz.super_clazz,t2);
		return t2;
		} finally { get_real_type_depth--; }
	}

	public static Type getProxyType(Type tp) {
		return newRefType(Type.tpRefProxy.clazz,new Type[]{tp});
//		if( tp.isReference() )			return Type.tpCellObject;
//		else if( tp == Type.tpBoolean )	return Type.tpCellBoolean;
//		else if( tp == Type.tpByte )	return Type.tpCellByte;
//		else if( tp == Type.tpChar )	return Type.tpCellChar;
//		else if( tp == Type.tpShort)	return Type.tpCellShort;
//		else if( tp == Type.tpInt  )	return Type.tpCellInt ;
//		else if( tp == Type.tpLong )	return Type.tpCellLong;
//		else if( tp == Type.tpFloat)	return Type.tpCellFloat;
//		else if( tp == Type.tpDouble)	return Type.tpCellDouble;
//		return tp;
	}

	public void checkJavaSignature() {
		if( clazz.isArgument() ) {
			Type jt = getJavaType();
			java_signature = jt.java_signature;
		}
	}

	public Dumper toJava(Dumper dmp) {
		if( isArray() )
			return dmp.append(args[0]).append("[]");
		else
			return clazz.toJava(dmp);
	}

}


@node(copyable=false)
public class MethodType extends Type {
	@ref public Type		ret;
	public Type[]	fargs;	// formal arguments for parametriezed methods

	private MethodType(Struct clazz, Type ret, Type[] args, Type[] fargs) {
		super(clazz==null?tpMethodClazz:clazz,args);
		this.ret = ret;
		this.fargs = fargs;
		signature = Signature.from(clazz,fargs,args,ret);
		if( clazz == tpMethodClazz ) {
			KStringBuffer ksb = new KStringBuffer(64);
			ksb.append((byte)'(');
			for(int i=0; i < args.length; i++)
				ksb.append(args[i].java_signature);
			ksb.append((byte)')');
			ksb.append(ret.java_signature);
			java_signature = ksb.toKString();
		} else {
			java_signature = Signature.getJavaSignature(new KString.KStringScanner(signature));
		}
		if( clazz != tpMethodClazz ) flags |= flReference;
		if( clazz.isArgument() ) flags |= flArgumented;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		if( ret.isArgumented() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New method type created: "+this+" with signature "+signature+" / "+java_signature);
	}

	public static MethodType newMethodType(Struct clazz, Type[] args, Type ret) {
		return newMethodType(clazz,null,args,ret);
	}
	public static MethodType newMethodType(Struct clazz, Type[] fargs, Type[] args, Type ret) {
		if (clazz == null) clazz = tpMethodClazz;
		if (fargs == null) fargs = Type.emptyArray;
		KString sign = Signature.from(clazz,fargs,args,ret);
		MethodType t = (MethodType)typeHash.get(sign.hashCode(),fun (Type t)->boolean {
			return t.signature.equals(sign) && t.clazz.equals(clazz); });
		if( t != null ) return t;
		t = new MethodType(clazz,ret,args,fargs);
		return t;
	}

	public Object copy() {
		throw new CompilerException(getPos(),"MethodType node cannot be copied");
	};

	public String toString() {
		StringBuffer str = new StringBuffer();
//		if( clazz.instanceOf(Type.tpClosureClazz) )
//			str.append('&');
		if (fargs != null && fargs.length > 0) {
			str.append('<');
			for(int i=0; i < fargs.length; i++) {
				str.append(fargs[i]);
				if( i < fargs.length-1)
					str.append(',');
			}
			str.append('>');
		}
		str.append('(');
		if( args != null && args.length > 0 ) {
			for(int i=0; i < args.length; i++) {
				str.append(args[i]);
				if( i < args.length-1)
					str.append(',');
			}
		}
		str.append(")->").append(ret);
		return str.toString();
	}

	public MethodType getMMType() {
		Type[] types = new Type[args.length];
		for(int i=0; i < types.length; i++) {
			if( !args[i].isReference() ) types[i] = args[i];
			else types[i] = Type.tpObject;
		}
		return MethodType.newMethodType(clazz,fargs,types,ret);
	}

	public boolean greater(MethodType tp) {
		if( args.length != tp.args.length ) return false;
		if( !ret.isInstanceOf(tp.ret) ) return false;
		boolean gt = false;
		for(int i=0; i < args.length; i++) {
			Type t1 = args[i];
			Type t2 = tp.args[i];
			if( !t1.string_equals(t2) ) {
				if( t1.isInstanceOf(t2) ) {
					trace(Kiev.debugMultiMethod,"Type "+args[i]+" is greater then "+t2);
					gt = true;
				} else {
					trace(Kiev.debugMultiMethod,"Types "+args[i]+" and "+tp.args[i]+" are uncomparable");
					return false;
				}
			} else {
				trace(Kiev.debugMultiMethod,"Types "+args[i]+" and "+tp.args[i]+" are equals");
			}
		}
		return gt;
	}

	public int compare(MethodType tp) {
		if( args.length != tp.args.length ) return 0;
		if( !ret.equals(tp.ret) ) return 0;
		boolean gt = false;
		boolean lt = false;
		for(int i=0; i < args.length; i++) {
			if( !args[i].string_equals(tp.args[i]) ) {
				if( args[i].isInstanceOf(tp.args[i]) ) gt = true;
				else if( tp.args[i].isInstanceOf(args[i]) ) lt = true;
				else return 0;
			}
		}
		if( gt && lt ) return 0;
		if(gt) return 1;
		if(lt) return -1;
		return 0;
	}

	public boolean isMultimethodSuper(MethodType tp) {
		if( args.length != tp.args.length ) return false;
		if( !tp.ret.isInstanceOf(ret) ) return false;
		for(int i=0; i < args.length; i++) {
			if( !args[i].equals(tp.args[i]) )
				return false;
		}
		return true;
	}

	public boolean argsClassesEquals(MethodType tp) {
		if( args.length != tp.args.length ) return false;
		for(int i=0; i < args.length; i++)
			if( !args[i].clazz.equals(tp.args[i].clazz) )
				return false;
		return true;
	}

	public void checkJavaSignature() {
		if( clazz == tpMethodClazz ) {
			KStringBuffer ksb = new KStringBuffer(64);
			ksb.append((byte)'(');
			for(int i=0; i < args.length; i++)
				ksb.append(args[i].java_signature);
			ksb.append((byte)')');
			ksb.append(ret.java_signature);
			java_signature = ksb.toKString();
			trace(Kiev.debugCreation,"Type "+this+" with signature "+signature+" java signature changed to "+java_signature);
		}
	}
}
