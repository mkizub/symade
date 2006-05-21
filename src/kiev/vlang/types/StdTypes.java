package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.vlang.AccessFlags.*;
import static kiev.stdlib.Debug.*;

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

	public static final ArgType   tpWrapperArg;
	public static final ArgType   tpCallRetArg;
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

		Struct tpObjectClazz = Env.newStruct("Object",java_lang,ACC_PUBLIC);
		tpObject				= tpObjectClazz.xtype;

		Struct tpClassClazz = Env.newStruct("Class",java_lang,ACC_PUBLIC|ACC_FINAL);
		tpClass					= tpClassClazz.xtype;

		tpNull		= new CoreType(Constants.nameNull,    tpObject, flReference);

//		tpRule		= new CoreType(Constants.nameRule,    flReference);
		Struct tpRuleClazz = new Struct(new Symbol("rule"),Env.root,ACC_PUBLIC);
		tpRule				= tpRuleClazz.xtype;
		tpRuleClazz.setResolved(true);
		tpRule.flags		= flResolved | flReference;

		Struct tpDebugClazz = Env.newStruct("Debug",kiev_stdlib,ACC_PUBLIC);
		tpDebug				= tpDebugClazz.xtype;

		Struct tpTypeInfoClazz = Env.newStruct("TypeInfo",kiev_stdlib,ACC_PUBLIC|ACC_FINAL);
		tpTypeInfo				= tpTypeInfoClazz.xtype;

		Struct tpTypeInfoInterfaceClazz = Env.newStruct("TypeInfoInterface",kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE);
		tpTypeInfoInterface				= tpTypeInfoInterfaceClazz.xtype;

		Struct tpCloneableClazz = Env.newStruct("Cloneable",java_lang,ACC_PUBLIC|ACC_INTERFACE);
		tpCloneable				= tpCloneableClazz.xtype;

		
		TypeDef tdWrapperArg = new TypeConstr("_boxed_", tpObject);
		tpWrapperArg = tdWrapperArg.getAType();
		tpWrapperArg.flags |= flHidden;
		
		tdArrayArg = new TypeConstr("_elem_", tpAny);
		tpArrayArg = tdArrayArg.getAType();
		tpArrayArg.flags |= flHidden;
		tpArray					= ArrayType.newArrayType(Type.tpAny);
		tpArray.flags			|= flResolved | flReference | flArray;

		Struct tpBooleanRefClazz = Env.newStruct("Boolean",java_lang,ACC_PUBLIC);
		tpBooleanRef			= tpBooleanRefClazz.xtype;

		Struct tpCharRefClazz = Env.newStruct("Character",java_lang,ACC_PUBLIC);
		tpCharRef			= tpCharRefClazz.xtype;

		Struct tpNumberRefClazz = Env.newStruct("Number",java_lang,ACC_PUBLIC);
		tpNumberRef			= tpNumberRefClazz.xtype;

		Struct tpByteRefClazz = Env.newStruct("Byte",java_lang,ACC_PUBLIC);
		tpByteRef			= tpByteRefClazz.xtype;

		Struct tpShortRefClazz = Env.newStruct("Short",java_lang,ACC_PUBLIC);
		tpShortRef			= tpShortRefClazz.xtype;

		Struct tpIntRefClazz = Env.newStruct("Integer",java_lang,ACC_PUBLIC);
		tpIntRef			= tpIntRefClazz.xtype;

		Struct tpLongRefClazz = Env.newStruct("Long",java_lang,ACC_PUBLIC);
		tpLongRef			= tpLongRefClazz.xtype;

		Struct tpFloatRefClazz = Env.newStruct("Float",java_lang,ACC_PUBLIC);
		tpFloatRef			= tpFloatRefClazz.xtype;

		Struct tpDoubleRefClazz = Env.newStruct("Double",java_lang,ACC_PUBLIC);
		tpDoubleRef			= tpDoubleRefClazz.xtype;

		Struct tpVoidRefClazz = Env.newStruct("Void",java_lang,ACC_PUBLIC);
		tpVoidRef			= tpVoidRefClazz.xtype;

		Struct tpStringClazz = Env.newStruct("String",java_lang,ACC_PUBLIC);
		tpString				= tpStringClazz.xtype;

		Struct tpAnnotationClazz = Env.newStruct("Annotation",java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
		tpAnnotation			= tpAnnotationClazz.xtype;
		
		Struct tpThrowableClazz = Env.newStruct("Throwable",java_lang,ACC_PUBLIC);
		tpThrowable				= tpThrowableClazz.xtype;

		Struct tpErrorClazz = Env.newStruct("Error",java_lang,ACC_PUBLIC);
		tpError				= tpErrorClazz.xtype;

		Struct tpExceptionClazz = Env.newStruct("Exception",java_lang,ACC_PUBLIC);
		tpException				= tpExceptionClazz.xtype;

		Struct tpCastExceptionClazz = Env.newStruct("ClassCastException",java_lang,ACC_PUBLIC);
		tpCastException				= tpCastExceptionClazz.xtype;

		Struct tpRuntimeExceptionClazz = Env.newStruct("RuntimeException",java_lang,ACC_PUBLIC);
		tpRuntimeException				= tpRuntimeExceptionClazz.xtype;

		Struct tpAssertExceptionClazz = Env.newStruct("AssertionFailedException",kiev_stdlib,ACC_PUBLIC);
		tpAssertException				= tpAssertExceptionClazz.xtype;

		Struct tpEnumClazz = Env.newStruct("Enum",java_lang,ACC_PUBLIC | ACC_ABSTRACT);
		tpEnum					= tpEnumClazz.xtype;

		tpClosureClazz = Env.newStruct("closure",kiev_stdlib,ACC_PUBLIC);
		tpClosure				= tpClosureClazz.xtype;

		Struct tpTypeSwitchHashClazz = Env.newStruct("TypeSwitchHash",kiev_stdlib,ACC_PUBLIC);
		tpTypeSwitchHash			= tpTypeSwitchHashClazz.xtype;


		Struct tpJavaEnumerationClazz = Env.newStruct("Enumeration",java_util,ACC_PUBLIC);
		tpJavaEnumeration	= tpJavaEnumerationClazz.xtype;
		
		Struct tpKievEnumerationClazz = Env.newStruct("Enumeration",kiev_stdlib,ACC_PUBLIC);
		tpKievEnumerationClazz.args.add(new TypeConstr("A"));
		tpKievEnumeration	= tpKievEnumerationClazz.xtype;
		
		
		Struct tpArrayEnumeratorClazz = Env.newStruct("ArrayEnumerator",kiev_stdlib,ACC_PUBLIC);
		tpArrayEnumeratorClazz.args.add(new TypeConstr("A"));
		tpArrayEnumerator	= tpArrayEnumeratorClazz.xtype;
		

		Struct tpPrologVarClazz = Env.newStruct("PVar",kiev_stdlib,ACC_PUBLIC);
		tpPrologVarClazz.args.add(new TypeConstr("A"));
		tpPrologVar	= tpPrologVarClazz.xtype;

		Struct tpRefProxyClazz = Env.newStruct("Ref",kiev_stdlib,ACC_PUBLIC);
		tpRefProxyClazz.args.add(new TypeConstr("A"));
		tpRefProxy	= tpRefProxyClazz.xtype;


		TypeDef tdCallRetArg = new TypeConstr("_ret_", tpAny);
		tpCallRetArg = tdCallRetArg.getAType();
		tpCallRetArg.flags |= flHidden;
		
		tpCallParamArgs = new ArgType[128];
		for (int i=0; i < tpCallParamArgs.length; i++) {
			TypeDef tdCallParamArg = new TypeConstr("_"+Integer.toHexString(i)+"_", tpAny);
			tpCallParamArgs[i] = tdCallParamArg.getAType();
			tpCallParamArgs[i].flags |= flHidden;
		}
		
		tpUnattachedArgs = new ArgType[128] ;
		for (int i=0; i < tpUnattachedArgs.length; i++) {
			TypeDef tdUnattachedArg = new TypeConstr("_"+Integer.toHexString(i)+"_", tpAny);
			tpUnattachedArgs[i] = tdUnattachedArg.getAType();
			//tpUnattachedArgs[i].flags |= flHidden;
		}
	}
}

