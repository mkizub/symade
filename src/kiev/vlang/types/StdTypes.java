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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.vlang.AccessFlags.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface StdTypes {

	public static final int flReference		= 1 <<  0;
	public static final int flIntegerInCode	= 1 <<  1;
	public static final int flInteger			= 1 <<  2;
	public static final int flFloatInCode		= 1 <<  3;
	public static final int flFloat				= 1 <<  4;
	public static final int flNumber			= flFloat | flInteger;
	public static final int flDoubleSize		= 1 <<  5;
	public static final int flArray				= 1 <<  6;
	public static final int flResolved			= 1 <<  7;
	public static final int flBoolean			= 1 <<  8;
	public static final int flWrapper			= 1 <<  9;
	public static final int flCallable			= 1 << 10;
	public static final int flAbstract			= 1 << 11;
	public static final int flUnerasable		= 1 << 12;
	public static final int flVirtual			= 1 << 13;
	public static final int flFinal				= 1 << 14;
	public static final int flStatic			= 1 << 15;
	public static final int flForward			= 1 << 16;
	public static final int flHidden			= 1 << 17;
	public static final int flArgAppliable		= 1 << 18;
	public static final int flValAppliable		= 1 << 19;
	public static final int flBindable			= 1 << 20;

	public static final CompaundType tpEnv;
	public static final CoreType tpAny;
	public static final CoreType tpVoid;
	public static final CoreType tpBoolean;
	public static final CoreType tpByte;
	public static final CoreType tpChar;
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

	public static final ArgType   tpWrapperArg;
	public static final ArgType   tpCallRetArg;
	public static final ArgType   tpCallThisArg;
	public static final ArgType[] tpCallParamArgs;
	public static final ArgType[] tpUnattachedArgs;

	static {

		Struct tpEnvClazz = Env.root;
		tpEnv				= new CompaundType((CompaundMetaType)tpEnvClazz.xmeta_type, TVarBld.emptySet);
		tpEnvClazz.xtype	= tpEnv;
		tpEnv.flags			= flResolved;

		tpAny		= new CoreType(Constants.nameAny,     null,  0);
		tpVoid		= new CoreType(Constants.nameVoid,    null,  0);
		tpBoolean	= new CoreType(Constants.nameBoolean, tpAny, flBoolean | flIntegerInCode);
		tpByte		= new CoreType(Constants.nameByte,    tpAny, flInteger | flIntegerInCode);
		tpChar		= new CoreType(Constants.nameChar,    tpAny, flInteger | flIntegerInCode);
		tpShort		= new CoreType(Constants.nameShort,   tpAny, flInteger | flIntegerInCode);
		tpInt		= new CoreType(Constants.nameInt,     tpAny, flInteger | flIntegerInCode);
		tpLong		= new CoreType(Constants.nameLong,    tpAny, flInteger | flDoubleSize);
		tpFloat		= new CoreType(Constants.nameFloat,   tpAny, flFloat);
		tpDouble	= new CoreType(Constants.nameDouble,  tpAny, flFloat   | flDoubleSize);

		Struct java_lang = Env.newPackage("java.lang");
		Struct java_lang_annotation = Env.newPackage("java.lang.annotation");
		Struct java_util = Env.newPackage("java.util");
		Struct kiev_stdlib = Env.newPackage("kiev.stdlib");
		Struct kiev_stdlib_meta = Env.newPackage("kiev.stdlib.meta");

		Struct tpObjectClazz = Env.newStruct("Object",java_lang,ACC_PUBLIC,new JavaClass());
		tpObject				= (CompaundType)tpObjectClazz.xtype;

		Struct tpClassClazz = Env.newStruct("Class",java_lang,ACC_PUBLIC|ACC_FINAL,new JavaClass());
		tpClass					= (CompaundType)tpClassClazz.xtype;

		tpNull		= new CoreType(Constants.nameNull,    tpObject, flReference);

//		tpRule		= new CoreType(Constants.nameRule,    flReference);
		Struct tpRuleClazz = new Struct("rule","rule",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpRule				= (CompaundType)tpRuleClazz.xtype;
		tpRuleClazz.setTypeDeclLoaded(true);
		tpRule.flags		= flResolved | flReference;
		kiev_stdlib.sub_decls += tpRuleClazz;

		Struct tpDebugClazz = Env.newStruct("Debug",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpDebug				= (CompaundType)tpDebugClazz.xtype;

		Struct tpTypeInfoClazz = Env.newStruct("TypeInfo",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpTypeInfo				= (CompaundType)tpTypeInfoClazz.xtype;

		Struct tpTypeInfoInterfaceClazz = Env.newStruct("TypeInfoInterface",kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE,new JavaInterface());
		tpTypeInfoInterface				= (CompaundType)tpTypeInfoInterfaceClazz.xtype;

		Struct tpCloneableClazz = Env.newStruct("Cloneable",java_lang,ACC_PUBLIC|ACC_INTERFACE,new JavaInterface());
		tpCloneable				= (CompaundType)tpCloneableClazz.xtype;

		
		TypeDef tdWrapperArg = new TypeConstr("_boxed_", tpObject);
		tdWrapperArg.setAbstract(true);
		tpWrapperArg = tdWrapperArg.getAType();
		tpWrapperArg.flags |= flHidden | flArgAppliable | flValAppliable;
		
		tdArrayArg = new TypeConstr("_elem_", tpAny);
		tdArrayArg.setAbstract(true);
		tpArrayArg = tdArrayArg.getAType();
		tpArrayArg.flags |= flHidden | flArgAppliable | flValAppliable;
		tpArray					= ArrayType.newArrayType(Type.tpAny);
		tpArray.flags			|= flResolved | flReference | flArray;

		TypeDecl tdVararg = Env.newMetaType(new Symbol<MetaTypeDecl>("_Vararg_"),kiev_stdlib,false);
		tdVararg.setPublic();
		tdVararg.setMacro(true);
		tdVararg.setFinal(true);
		tdVararg.setTypeDeclLoaded(true);
		TypeConstr tdVarargArg = new TypeConstr("_elem_", tpObject);
		tdVarargArg.setAbstract(true);
		tdVararg.args += tdVarargArg;
		ArgType tpVarargArg = tdVarargArg.getAType();
		tpArrayArg.flags |= flHidden | flArgAppliable | flValAppliable;
		tdVararg.super_types += new TypeRef(ArrayType.newArrayType(tpVarargArg));
		tpVararg				= (XType)tdVararg.xtype;
		//tpVararg.flags			|= flResolved | flReference | flArray;

		Struct tpBooleanRefClazz = Env.newStruct("Boolean",java_lang,ACC_PUBLIC,new JavaClass());
		tpBooleanRef			= (CompaundType)tpBooleanRefClazz.xtype;

		Struct tpCharRefClazz = Env.newStruct("Character",java_lang,ACC_PUBLIC,new JavaClass());
		tpCharRef			= (CompaundType)tpCharRefClazz.xtype;

		Struct tpNumberRefClazz = Env.newStruct("Number",java_lang,ACC_PUBLIC,new JavaClass());
		tpNumberRef			= (CompaundType)tpNumberRefClazz.xtype;

		Struct tpByteRefClazz = Env.newStruct("Byte",java_lang,ACC_PUBLIC,new JavaClass());
		tpByteRef			= (CompaundType)tpByteRefClazz.xtype;

		Struct tpShortRefClazz = Env.newStruct("Short",java_lang,ACC_PUBLIC,new JavaClass());
		tpShortRef			= (CompaundType)tpShortRefClazz.xtype;

		Struct tpIntRefClazz = Env.newStruct("Integer",java_lang,ACC_PUBLIC,new JavaClass());
		tpIntRef			= (CompaundType)tpIntRefClazz.xtype;

		Struct tpLongRefClazz = Env.newStruct("Long",java_lang,ACC_PUBLIC,new JavaClass());
		tpLongRef			= (CompaundType)tpLongRefClazz.xtype;

		Struct tpFloatRefClazz = Env.newStruct("Float",java_lang,ACC_PUBLIC,new JavaClass());
		tpFloatRef			= (CompaundType)tpFloatRefClazz.xtype;

		Struct tpDoubleRefClazz = Env.newStruct("Double",java_lang,ACC_PUBLIC,new JavaClass());
		tpDoubleRef			= (CompaundType)tpDoubleRefClazz.xtype;

		Struct tpVoidRefClazz = Env.newStruct("Void",java_lang,ACC_PUBLIC,new JavaClass());
		tpVoidRef			= (CompaundType)tpVoidRefClazz.xtype;

		Struct tpStringClazz = Env.newStruct("String",java_lang,ACC_PUBLIC,new JavaClass());
		tpString				= (CompaundType)tpStringClazz.xtype;

		Struct tpAnnotationClazz = Env.newStruct("Annotation",java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT,new JavaInterface());
		tpAnnotation			= (CompaundType)tpAnnotationClazz.xtype;
		
		Struct tpThrowableClazz = Env.newStruct("Throwable",java_lang,ACC_PUBLIC,new JavaClass());
		tpThrowable				= (CompaundType)tpThrowableClazz.xtype;

		Struct tpErrorClazz = Env.newStruct("Error",java_lang,ACC_PUBLIC,new JavaClass());
		tpError				= (CompaundType)tpErrorClazz.xtype;

		Struct tpExceptionClazz = Env.newStruct("Exception",java_lang,ACC_PUBLIC,new JavaClass());
		tpException				= (CompaundType)tpExceptionClazz.xtype;

		Struct tpCastExceptionClazz = Env.newStruct("ClassCastException",java_lang,ACC_PUBLIC,new JavaClass());
		tpCastException				= (CompaundType)tpCastExceptionClazz.xtype;

		Struct tpRuntimeExceptionClazz = Env.newStruct("RuntimeException",java_lang,ACC_PUBLIC,new JavaClass());
		tpRuntimeException				= (CompaundType)tpRuntimeExceptionClazz.xtype;

		Struct tpAssertExceptionClazz = Env.newStruct("AssertionFailedException",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpAssertException				= (CompaundType)tpAssertExceptionClazz.xtype;

		Struct tpEnumClazz = Env.newStruct("Enum",java_lang,ACC_PUBLIC | ACC_ABSTRACT,new JavaClass());
		tpEnum					= (CompaundType)tpEnumClazz.xtype;

		tpClosureClazz = Env.newStruct("closure",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpClosure				= (CompaundType)tpClosureClazz.xtype;

		Struct tpTypeSwitchHashClazz = Env.newStruct("TypeSwitchHash",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpTypeSwitchHash			= (CompaundType)tpTypeSwitchHashClazz.xtype;


		Struct tpJavaEnumerationClazz = Env.newStruct("Enumeration",java_util,ACC_PUBLIC,new JavaInterface());
		tpJavaEnumeration	= (CompaundType)tpJavaEnumerationClazz.xtype;
		
		Struct tpKievEnumerationClazz = Env.newStruct("Enumeration",kiev_stdlib,ACC_PUBLIC,new JavaInterface());
		tpKievEnumerationClazz.args.add(new TypeConstr("A"));
		tpKievEnumeration	= (CompaundType)tpKievEnumerationClazz.xtype;
		
		
		Struct tpArrayEnumeratorClazz = Env.newStruct("ArrayEnumerator",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpArrayEnumeratorClazz.args.add(new TypeConstr("A"));
		tpArrayEnumerator	= (CompaundType)tpArrayEnumeratorClazz.xtype;
		

		Struct tpPrologVarClazz = Env.newStruct("PVar",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpPrologVarClazz.args.add(new TypeConstr("A"));
		tpPrologVar	= (CompaundType)tpPrologVarClazz.xtype;

		Struct tpRefProxyClazz = Env.newStruct("Ref",kiev_stdlib,ACC_PUBLIC,new JavaClass());
		tpRefProxyClazz.args.add(new TypeConstr("A"));
		tpRefProxy	= (CompaundType)tpRefProxyClazz.xtype;


		TypeDef tdCallRetArg = new TypeConstr("_ret_", tpAny);
		tdCallRetArg.setAbstract(true);
		tpCallRetArg = tdCallRetArg.getAType();
		tpCallRetArg.flags |= flHidden | flArgAppliable;
		
		TypeDef tdCallThisArg = new TypeConstr("_this_", tpAny);
		tdCallThisArg.setAbstract(true);
		tpCallThisArg = tdCallThisArg.getAType();
		tpCallThisArg.flags |= flHidden | flArgAppliable;
		
		tpCallParamArgs = new ArgType[128];
		for (int i=0; i < tpCallParamArgs.length; i++) {
			TypeDef tdCallParamArg = new TypeConstr("_"+Integer.toHexString(i)+"_", tpAny);
			tdCallParamArg.setAbstract(true);
			tpCallParamArgs[i] = tdCallParamArg.getAType();
			tpCallParamArgs[i].flags |= flHidden | flArgAppliable;
		}
		
		tpUnattachedArgs = new ArgType[128] ;
		for (int i=0; i < tpUnattachedArgs.length; i++) {
			TypeDef tdUnattachedArg = new TypeConstr("_"+Integer.toHexString(i)+"_", tpAny);
			tpUnattachedArgs[i] = tdUnattachedArg.getAType();
			//tpUnattachedArgs[i].flags |= flHidden;
		}
	}
}

