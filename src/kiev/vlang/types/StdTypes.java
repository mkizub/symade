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
package kiev.vlang.types;

import static kiev.vlang.AccessFlags.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface StdTypes {

	public static final int flAbstract			= 1 <<  0;
	public static final int flUnerasable		= 1 <<  1;
	public static final int flVirtual			= 1 <<  2;
	public static final int flFinal			= 1 <<  3;
	public static final int flStatic			= 1 <<  4;
	public static final int flForward			= 1 <<  5;
	public static final int flArgAppliable		= 1 <<  6;
	public static final int flValAppliable		= 1 <<  7;

	public static final CompaundType tpEnv;
	public static final CoreType tpAny;
	public static final CoreType tpVoid;
	public static final CoreType tpBoolean;
	public static final CoreType tpChar;
	public static final CoreType tpByte;
	public static final CoreType tpShort;
	public static final CoreType tpInt;
	public static final CoreType tpLong;
	public static final CoreType tpFloat;
	public static final CoreType tpDouble;
	public static final CoreType tpNull;
	public static final CompaundType tpRule;
	public static final CompaundType tpBooleanRef;
	public static final CompaundType tpByteRef;
	public static final CompaundType tpCharRef;
	public static final CompaundType tpNumberRef;
	public static final CompaundType tpShortRef;
	public static final CompaundType tpIntRef;
	public static final CompaundType tpLongRef;
	public static final CompaundType tpFloatRef;
	public static final CompaundType tpDoubleRef;
	public static final CompaundType tpVoidRef;
	public static final CompaundType tpObject;
	public static final CompaundType tpClass;
	public static final CompaundType tpDebug;
	public static final CompaundType tpTypeInfo;
	public static final CompaundType tpTypeInfoInterface;
	public static final CompaundType tpCloneable;
	public static final CompaundType tpString;
	public static final CompaundType tpThrowable;
	public static final CompaundType tpError;
	public static final CompaundType tpException;
	public static final CompaundType tpCastException;
	public static final CompaundType tpJavaEnumeration;
	public static final CompaundType tpKievEnumeration;
	public static final CompaundType tpArrayEnumerator;
	public static final CompaundType tpRuntimeException;
	public static final CompaundType tpAssertException;
	public static final CompaundType tpEnum;
	public static final CompaundType tpAnnotation;
	public static final CompaundType tpClosure;
	public static final Struct tpClosureClazz;

	public static final CompaundType tpPrologVar;
	public static final CompaundType tpRefProxy;

	public static final CompaundType tpTypeSwitchHash;

	public static final ArrayType  tpArray;
	public static final TypeConstr tdArrayArg;
	public static final ArgType    tpArrayArg;
	public static final XType      tpVararg;
	public static final ArgType		tpVarargArg;
	public static final TypeDecl	tdASTNodeType;

	public static final ArgType    tpWildcardCoArg;
	public static final TypeConstr tdWildcardCoArg;
	public static final ArgType    tpWildcardContraArg;
	public static final TypeConstr tdWildcardContraArg;
	public static final ArgType    tpWrapperArg;
	public static final TypeConstr tdWrapperArg;
	public static final ArgType    tpCallRetArg;
	public static final ArgType    tpCallTupleArg;
	public static final ArgType    tpCallThisArg;
	public static final ArgType[]  tpCallParamArgs;
	public static final ArgType[]  tpUnattachedArgs;

	static {

		Env env = Env.getRoot();
		tpEnv				= new CompaundType((CompaundMetaType)env.xmeta_type, TVarBld.emptySet);
		env.xtype			= tpEnv;

		tpAny		= new CoreType(Constants.nameAny,     null,  0);												tpAny.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpVoid		= new CoreType(Constants.nameVoid,    null,  0);												tpVoid.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpBoolean	= new CoreType(Constants.nameBoolean, tpAny, MetaType.flBoolean | MetaType.flIntegerInCode);	tpBoolean.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpChar		= new CoreType(Constants.nameChar,    tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpChar.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpByte		= new CoreType(Constants.nameByte,    tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpByte.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpShort		= new CoreType(Constants.nameShort,   tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpShort.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpInt		= new CoreType(Constants.nameInt,     tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpInt.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpLong		= new CoreType(Constants.nameLong,    tpAny, MetaType.flInteger | MetaType.flDoubleSize);		tpLong.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpFloat		= new CoreType(Constants.nameFloat,   tpAny, MetaType.flFloat);								tpFloat.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpDouble	= new CoreType(Constants.nameDouble,  tpAny, MetaType.flFloat   | MetaType.flDoubleSize);		tpDouble.meta_type.tdecl.setTypeDeclNotLoaded(true);

		Struct java_lang = env.newPackage("java\u001flang");
		Struct java_lang_annotation = env.newPackage("java\u001flang\u001fannotation");
		Struct java_util = env.newPackage("java\u001futil");
		Struct kiev_stdlib = env.newPackage("kiev\u001fstdlib");
		Struct kiev_stdlib_meta = env.newPackage("kiev\u001fstdlib\u001fmeta");

		Struct tpObjectClazz = env.newStruct("Object",java_lang,ACC_PUBLIC,new JavaClass());
		tpObjectClazz.setTypeDeclNotLoaded(true);
		tpObject				= (CompaundType)tpObjectClazz.xtype;

		Struct tpClassClazz = env.newStruct("Class",java_lang,ACC_PUBLIC|ACC_FINAL,new JavaClass());
		tpClassClazz.setTypeDeclNotLoaded(true);
		tpClass					= (CompaundType)tpClassClazz.xtype;

		tpNull		= new CoreType(Constants.nameNull,    tpObject, MetaType.flReference);
		tpNull.meta_type.tdecl.setTypeDeclNotLoaded(true);

		Struct tpRuleClazz = env.newStruct("rule",kiev_stdlib,ACC_PUBLIC|ACC_ABSTRACT,new JavaClass());
		tpRuleClazz.setTypeDeclNotLoaded(true);
		tpRule					= (CompaundType)tpRuleClazz.xtype;

		Struct tpDebugClazz = env.newStruct("Debug",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpDebugClazz.setTypeDeclNotLoaded(true);
		tpDebug				= (CompaundType)tpDebugClazz.xtype;

		Struct tpTypeInfoClazz = env.newStruct("TypeInfo",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpTypeInfoClazz.setTypeDeclNotLoaded(true);
		tpTypeInfo				= (CompaundType)tpTypeInfoClazz.xtype;

		Struct tpTypeInfoInterfaceClazz = env.newStruct("TypeInfoInterface",kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE,new JavaInterface());
		tpTypeInfoInterfaceClazz.setTypeDeclNotLoaded(true);
		tpTypeInfoInterface				= (CompaundType)tpTypeInfoInterfaceClazz.xtype;

		Struct tpCloneableClazz = env.newStruct("Cloneable",java_lang,ACC_PUBLIC|ACC_INTERFACE,new JavaInterface());
		tpCloneableClazz.setTypeDeclNotLoaded(true);
		tpCloneable				= (CompaundType)tpCloneableClazz.xtype;

		
		tdWildcardCoArg = new TypeConstr("_base_", tpAny);
		tdWildcardCoArg.setAbstract(true);
		tpWildcardCoArg = tdWildcardCoArg.getAType();
		tpWildcardCoArg.flags |= flArgAppliable | flValAppliable;
		
		tdWildcardContraArg = new TypeConstr("_base_", tpAny);
		tdWildcardContraArg.setAbstract(true);
		tpWildcardContraArg = tdWildcardContraArg.getAType();
		tpWildcardContraArg.flags |= flArgAppliable | flValAppliable;
		
		tdWrapperArg = new TypeConstr("_boxed_", tpObject);
		tdWrapperArg.setAbstract(true);
		tpWrapperArg = tdWrapperArg.getAType();
		tpWrapperArg.flags |= flArgAppliable | flValAppliable;
		
		tdArrayArg = new TypeConstr("_elem_", tpAny);
		tdArrayArg.setAbstract(true);
		tdArrayArg.variance = TypeVariance.CO_VARIANT;
		tpArrayArg = tdArrayArg.getAType();
		tpArrayArg.flags |= flArgAppliable | flValAppliable;
		tpArray					= ArrayType.newArrayType(tpArrayArg);

		TypeDecl tdVararg = env.newMetaType(new Symbol<MetaTypeDecl>("_vararg_"),kiev_stdlib,false,"8aa32751-ac53-343e-b456-6f8521b01647");
		tdVararg.setPublic();
		tdVararg.setMacro(true);
		tdVararg.setFinal(true);
		TypeConstr tdVarargArg = new TypeConstr("_elem_", tpObject);
		tdVarargArg.setAbstract(true);
		tdVararg.args += tdVarargArg;
		tpVarargArg = tdVarargArg.getAType();
		tpVarargArg.flags |= flArgAppliable | flValAppliable;
		tdVararg.super_types += new TypeRef(ArrayType.newArrayType(tpVarargArg));
		tpVararg				= (XType)tdVararg.xtype;

		tdASTNodeType = env.newMetaType(new Symbol<MetaTypeDecl>("_astnode_"),kiev_stdlib,false,"3e32f9c7-9846-393e-8c6e-11512191ec94");
		tdASTNodeType.setPublic();
		tdASTNodeType.setMacro(true);
		tdASTNodeType.setFinal(true);
		TypeConstr tdASTNodeTypeArg = new TypeConstr("_node_", tpObject);
		tdASTNodeTypeArg.setAbstract(true);
		tdASTNodeType.args += tdASTNodeTypeArg;
		ArgType tpASTNodeTypeArg;
		tpASTNodeTypeArg = tdASTNodeTypeArg.getAType();
		tpASTNodeTypeArg.flags |= flArgAppliable | flValAppliable;
		tdASTNodeType.super_types += new TypeRef(StdTypes.tpAny);

		Struct tpBooleanRefClazz = env.newStruct("Boolean",java_lang,ACC_PUBLIC,new JavaClass());
		tpBooleanRefClazz.setTypeDeclNotLoaded(true);
		tpBooleanRef			= (CompaundType)tpBooleanRefClazz.xtype;

		Struct tpCharRefClazz = env.newStruct("Character",java_lang,ACC_PUBLIC,new JavaClass());
		tpCharRefClazz.setTypeDeclNotLoaded(true);
		tpCharRef			= (CompaundType)tpCharRefClazz.xtype;

		Struct tpNumberRefClazz = env.newStruct("Number",java_lang,ACC_PUBLIC,new JavaClass());
		tpNumberRefClazz.setTypeDeclNotLoaded(true);
		tpNumberRef			= (CompaundType)tpNumberRefClazz.xtype;

		Struct tpByteRefClazz = env.newStruct("Byte",java_lang,ACC_PUBLIC,new JavaClass());
		tpByteRefClazz.setTypeDeclNotLoaded(true);
		tpByteRef			= (CompaundType)tpByteRefClazz.xtype;

		Struct tpShortRefClazz = env.newStruct("Short",java_lang,ACC_PUBLIC,new JavaClass());
		tpShortRefClazz.setTypeDeclNotLoaded(true);
		tpShortRef			= (CompaundType)tpShortRefClazz.xtype;

		Struct tpIntRefClazz = env.newStruct("Integer",java_lang,ACC_PUBLIC,new JavaClass());
		tpIntRefClazz.setTypeDeclNotLoaded(true);
		tpIntRef			= (CompaundType)tpIntRefClazz.xtype;

		Struct tpLongRefClazz = env.newStruct("Long",java_lang,ACC_PUBLIC,new JavaClass());
		tpLongRefClazz.setTypeDeclNotLoaded(true);
		tpLongRef			= (CompaundType)tpLongRefClazz.xtype;

		Struct tpFloatRefClazz = env.newStruct("Float",java_lang,ACC_PUBLIC,new JavaClass());
		tpFloatRefClazz.setTypeDeclNotLoaded(true);
		tpFloatRef			= (CompaundType)tpFloatRefClazz.xtype;

		Struct tpDoubleRefClazz = env.newStruct("Double",java_lang,ACC_PUBLIC,new JavaClass());
		tpDoubleRefClazz.setTypeDeclNotLoaded(true);
		tpDoubleRef			= (CompaundType)tpDoubleRefClazz.xtype;

		Struct tpVoidRefClazz = env.newStruct("Void",java_lang,ACC_PUBLIC,new JavaClass());
		tpVoidRefClazz.setTypeDeclNotLoaded(true);
		tpVoidRef			= (CompaundType)tpVoidRefClazz.xtype;

		Struct tpStringClazz = env.newStruct("String",java_lang,ACC_PUBLIC,new JavaClass());
		tpStringClazz.setTypeDeclNotLoaded(true);
		tpString				= (CompaundType)tpStringClazz.xtype;

		Struct tpAnnotationClazz = env.newStruct("Annotation",java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT,new JavaInterface());
		tpAnnotationClazz.setTypeDeclNotLoaded(true);
		tpAnnotation			= (CompaundType)tpAnnotationClazz.xtype;
		
		Struct tpThrowableClazz = env.newStruct("Throwable",java_lang,ACC_PUBLIC,new JavaClass());
		tpThrowableClazz.setTypeDeclNotLoaded(true);
		tpThrowable				= (CompaundType)tpThrowableClazz.xtype;

		Struct tpErrorClazz = env.newStruct("Error",java_lang,ACC_PUBLIC,new JavaClass());
		tpErrorClazz.setTypeDeclNotLoaded(true);
		tpError				= (CompaundType)tpErrorClazz.xtype;

		Struct tpExceptionClazz = env.newStruct("Exception",java_lang,ACC_PUBLIC,new JavaClass());
		tpExceptionClazz.setTypeDeclNotLoaded(true);
		tpException				= (CompaundType)tpExceptionClazz.xtype;

		Struct tpCastExceptionClazz = env.newStruct("ClassCastException",java_lang,ACC_PUBLIC,new JavaClass());
		tpCastExceptionClazz.setTypeDeclNotLoaded(true);
		tpCastException				= (CompaundType)tpCastExceptionClazz.xtype;

		Struct tpRuntimeExceptionClazz = env.newStruct("RuntimeException",java_lang,ACC_PUBLIC,new JavaClass());
		tpRuntimeExceptionClazz.setTypeDeclNotLoaded(true);
		tpRuntimeException				= (CompaundType)tpRuntimeExceptionClazz.xtype;

		Struct tpAssertExceptionClazz = env.newStruct("AssertionFailedException",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpAssertExceptionClazz.setTypeDeclNotLoaded(true);
		tpAssertException				= (CompaundType)tpAssertExceptionClazz.xtype;

		Struct tpEnumClazz = env.newStruct("Enum",java_lang,ACC_PUBLIC | ACC_ABSTRACT,new JavaClass());
		tpEnumClazz.setTypeDeclNotLoaded(true);
		tpEnum					= (CompaundType)tpEnumClazz.xtype;

		tpClosureClazz = env.newStruct("closure",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpClosureClazz.setTypeDeclNotLoaded(true);
		tpClosure				= (CompaundType)tpClosureClazz.xtype;

		Struct tpTypeSwitchHashClazz = env.newStruct("TypeSwitchHash",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpTypeSwitchHashClazz.setTypeDeclNotLoaded(true);
		tpTypeSwitchHash			= (CompaundType)tpTypeSwitchHashClazz.xtype;


		Struct tpJavaEnumerationClazz = env.newStruct("Enumeration",java_util,ACC_PUBLIC,new JavaInterface());
		tpJavaEnumerationClazz.setTypeDeclNotLoaded(true);
		tpJavaEnumeration	= (CompaundType)tpJavaEnumerationClazz.xtype;
		
		Struct tpKievEnumerationClazz = env.newStruct("Enumeration",kiev_stdlib,ACC_PUBLIC,new JavaInterface());
		tpKievEnumerationClazz.setTypeDeclNotLoaded(true);
		tpKievEnumerationClazz.args.add(new TypeConstr("A"));
		tpKievEnumeration	= (CompaundType)tpKievEnumerationClazz.xtype;
		
		
		Struct tpArrayEnumeratorClazz = env.newStruct("ArrayEnumerator",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpArrayEnumeratorClazz.setTypeDeclNotLoaded(true);
		tpArrayEnumeratorClazz.args.add(new TypeConstr("A"));
		tpArrayEnumerator	= (CompaundType)tpArrayEnumeratorClazz.xtype;
		

		Struct tpPrologVarClazz = env.newStruct("PVar",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpPrologVarClazz.setTypeDeclNotLoaded(true);
		tpPrologVarClazz.args.add(new TypeConstr("A"));
		tpPrologVar	= (CompaundType)tpPrologVarClazz.xtype;

		Struct tpRefProxyClazz = env.newStruct("Ref",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpRefProxyClazz.setTypeDeclNotLoaded(true);
		tpRefProxyClazz.args.add(new TypeConstr("A"));
		tpRefProxy	= (CompaundType)tpRefProxyClazz.xtype;

		WrapperMetaType.instance(tpWrapperArg); // kick static initializer

		TypeDef tdCallRetArg = new TypeConstr("_ret_", tpAny);
		tdCallRetArg.setAbstract(true);
		tpCallRetArg = tdCallRetArg.getAType();
		tpCallRetArg.flags |= flArgAppliable;
		
		TypeDef tdCallTupleArg = new TypeConstr("_tuple_", tpAny);
		tdCallTupleArg.setAbstract(true);
		tpCallTupleArg = tdCallTupleArg.getAType();
		tpCallTupleArg.flags |= flArgAppliable;
		
		TypeDef tdCallThisArg = new TypeConstr("_this_", tpAny);
		tdCallThisArg.setAbstract(true);
		tpCallThisArg = tdCallThisArg.getAType();
		tpCallThisArg.flags |= flArgAppliable;
		
		tpCallParamArgs = new ArgType[128];
		for (int i=0; i < tpCallParamArgs.length; i++) {
			TypeDef tdCallParamArg = new TypeConstr("_"+Integer.toHexString(i)+"_", tpAny);
			tdCallParamArg.setAbstract(true);
			tpCallParamArgs[i] = tdCallParamArg.getAType();
			tpCallParamArgs[i].flags |= flArgAppliable;
		}
		
		tpUnattachedArgs = new ArgType[128] ;
		for (int i=0; i < tpUnattachedArgs.length; i++) {
			TypeDef tdUnattachedArg = new TypeConstr("_"+Integer.toHexString(i)+"_", tpAny);
			tpUnattachedArgs[i] = tdUnattachedArg.getAType();
			tpUnattachedArgs[i].flags |= flArgAppliable;
		}
		
		tpAny.meta_type.tdecl.uuid =			"be8bba7f-b4f9-3991-8834-6552dcb237a0";
		tpVoid.meta_type.tdecl.uuid =			"ec98468f-75f6-3811-ab77-6b0a8458b3ad";
		tpBoolean.meta_type.tdecl.uuid =		"9c517365-318e-307c-acdf-6682cf309b3f";
		tpChar.meta_type.tdecl.uuid =			"7713311e-809c-30f7-964a-3d28beb7aab3";
		tpByte.meta_type.tdecl.uuid =			"89ed44f6-f9a6-3ef7-b396-d2248d5f69db";
		tpShort.meta_type.tdecl.uuid =			"f9bb2439-c397-3930-b36c-5b1565ec7841";
		tpInt.meta_type.tdecl.uuid =			"d50f9a1a-2e09-3313-8a64-6b58b300579e";
		tpLong.meta_type.tdecl.uuid =			"2d6eef81-2c5e-36e4-ab9d-136dfec1dc6b";
		tpFloat.meta_type.tdecl.uuid =			"a02d23b3-8055-3c87-b331-2b242964a7f1";
		tpDouble.meta_type.tdecl.uuid =		"d741575d-769c-3108-810e-6c0e57a4b03e";
		tpNull.meta_type.tdecl.uuid =			"6c8cef01-5c38-36c3-aab0-bd16c23e817d";

		tdArrayArg.uuid =						"74843bf1-3c28-374b-ad11-006af8a31a71";
		tdWildcardCoArg.uuid =					"311f0fb3-a9d6-33b9-8525-170de22d0f73";
		tdWildcardContraArg.uuid =				"034fcce5-a61c-38df-85ea-8cd0d238fab7";
		tdWrapperArg.uuid =						"400f213e-a4bb-3ee2-b870-9ec1951fd955";
		tdVararg.uuid =							"8aa32751-ac53-343e-b456-6f8521b01647";
		tdVarargArg.uuid =						"924f219a-37cf-3654-b761-7cb5e26ceef0";
		tdASTNodeType.uuid =					"3e32f9c7-9846-393e-8c6e-11512191ec94";
		tdASTNodeTypeArg.uuid =					"f23d4ec5-7fc2-3bbb-9b8f-46a309fc5f24";
	}
}

