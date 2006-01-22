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

	public static final ArrayType tpArray;

	public static final ArgType   tpArrayArg;
	public static final ArgType   tpWrapperArg;
	public static final ArgType   tpCallRetArg;
	public static final ArgType[] tpCallParamArgs;
	public static final ArgType[] tpUnattachedArgs;

	static {

		Struct tpEnvClazz = Env.root;
		tpEnv				= new CompaundType(tpEnvClazz.imeta_type, TVarBld.emptySet);
		((Struct.StructImpl)tpEnvClazz.$v_impl).ctype		= tpEnv;
		tpEnv.flags			= flResolved;

		tpAny		= new CoreType(Constants.nameAny,     0);
		tpVoid		= new CoreType(Constants.nameVoid,    0);
		tpBoolean	= new CoreType(Constants.nameBoolean, flBoolean | flIntegerInCode);
		tpByte		= new CoreType(Constants.nameByte,    flInteger | flIntegerInCode);
		tpChar		= new CoreType(Constants.nameChar,    flInteger | flIntegerInCode);
		tpShort		= new CoreType(Constants.nameShort,   flInteger | flIntegerInCode);
		tpInt		= new CoreType(Constants.nameInt,     flInteger | flIntegerInCode);
		tpLong		= new CoreType(Constants.nameLong,    flInteger | flDoubleSize);
		tpFloat		= new CoreType(Constants.nameFloat,   flFloat);
		tpDouble	= new CoreType(Constants.nameDouble,  flFloat   | flDoubleSize);
		tpNull		= new CoreType(Constants.nameNull,    flReference);
//		tpRule		= new CoreType(Constants.nameRule,    flReference);

		Struct tpRuleClazz = Env.newStruct(new ClazzName(
							KString.from("rule"),
							KString.from("rule"),
							KString.from("R"),false,false),null,ACC_PUBLIC);
		tpRule					= tpRuleClazz.ctype;
		tpRuleClazz.setResolved(true);
		tpRule.flags			= flResolved | flReference;

		Struct java_lang = Env.newPackage(KString.from("java.lang"));
		Struct java_lang_annotation = Env.newPackage(KString.from("java.lang.annotation"));
		Struct java_util = Env.newPackage(KString.from("java.util"));
		Struct kiev_stdlib = Env.newPackage(KString.from("kiev.stdlib"));
		Struct kiev_stdlib_meta = Env.newPackage(KString.from("kiev.stdlib.meta"));

		Struct tpObjectClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Object;")),java_lang,ACC_PUBLIC);
		tpObject				= tpObjectClazz.ctype;

		Struct tpClassClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Class;")),java_lang,ACC_PUBLIC|ACC_FINAL);
		tpClass					= tpClassClazz.ctype;

		Struct tpDebugClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Debug;")),kiev_stdlib,ACC_PUBLIC);
		tpDebug				= tpDebugClazz.ctype;

		Struct tpTypeInfoClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoX;")),kiev_stdlib,ACC_PUBLIC|ACC_FINAL);
		tpTypeInfo				= tpTypeInfoClazz.ctype;

		Struct tpTypeInfoInterfaceClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoXInterface;")),kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE);
		tpTypeInfoInterface				= tpTypeInfoInterfaceClazz.ctype;

		Struct tpCloneableClazz = Env.newInterface(ClazzName.fromSignature(KString.from("Ljava/lang/Cloneable;")),java_lang,ACC_PUBLIC|ACC_INTERFACE);
		tpCloneable				= tpCloneableClazz.ctype;
		tpCloneableClazz.setInterface(true);

		
		TypeDef tdWrapperArg = new TypeDef(KString.from("_boxed_"), tpObject);
		tpWrapperArg = new ArgType(KString.from("_boxed_"),tdWrapperArg);
		tpWrapperArg.flags |= flHidden;
		
		TypeDef tdArrayArg = new TypeDef(KString.from("_elem_"), tpAny);
		tpArrayArg = new ArgType(KString.from("_elem_"),tdArrayArg);
		tpArrayArg.flags |= flHidden;
		tpArray					= ArrayType.newArrayType(Type.tpAny);
		tpArray.flags			|= flResolved | flReference | flArray;

		Struct tpBooleanRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Boolean;")),java_lang,ACC_PUBLIC);
		tpBooleanRef			= tpBooleanRefClazz.ctype;

		Struct tpCharRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Character;")),java_lang,ACC_PUBLIC);
		tpCharRef			= tpCharRefClazz.ctype;

		Struct tpNumberRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Number;")),java_lang,ACC_PUBLIC);
		tpNumberRef			= tpNumberRefClazz.ctype;

		Struct tpByteRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Byte;")),java_lang,ACC_PUBLIC);
		tpByteRef			= tpByteRefClazz.ctype;

		Struct tpShortRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Short;")),java_lang,ACC_PUBLIC);
		tpShortRef			= tpShortRefClazz.ctype;

		Struct tpIntRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Integer;")),java_lang,ACC_PUBLIC);
		tpIntRef			= tpIntRefClazz.ctype;

		Struct tpLongRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Long;")),java_lang,ACC_PUBLIC);
		tpLongRef			= tpLongRefClazz.ctype;

		Struct tpFloatRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Float;")),java_lang,ACC_PUBLIC);
		tpFloatRef			= tpFloatRefClazz.ctype;

		Struct tpDoubleRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Double;")),java_lang,ACC_PUBLIC);
		tpDoubleRef			= tpDoubleRefClazz.ctype;

		Struct tpVoidRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Void;")),java_lang,ACC_PUBLIC);
		tpVoidRef			= tpVoidRefClazz.ctype;

		Struct tpStringClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/String;")),java_lang,ACC_PUBLIC);
		tpString				= tpStringClazz.ctype;

		Struct tpAnnotationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/annotation/Annotation;")),java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
		tpAnnotation			= tpAnnotationClazz.ctype;
		
		Struct tpThrowableClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Throwable;")),java_lang,ACC_PUBLIC);
		tpThrowable				= tpThrowableClazz.ctype;

		Struct tpErrorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Error;")),java_lang,ACC_PUBLIC);
		tpError				= tpErrorClazz.ctype;

		Struct tpExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Exception;")),java_lang,ACC_PUBLIC);
		tpException				= tpExceptionClazz.ctype;

		Struct tpCastExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/ClassCastException;")),java_lang,ACC_PUBLIC);
		tpCastException				= tpCastExceptionClazz.ctype;

		Struct tpRuntimeExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/RuntimeException;")),java_lang,ACC_PUBLIC);
		tpRuntimeException				= tpRuntimeExceptionClazz.ctype;

		Struct tpAssertExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/AssertionFailedException;")),kiev_stdlib,ACC_PUBLIC);
		tpAssertException				= tpAssertExceptionClazz.ctype;

		Struct tpEnumClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Enum;")),java_lang,ACC_PUBLIC | ACC_ABSTRACT);
		tpEnum					= tpEnumClazz.ctype;

		tpClosureClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/closure;")),kiev_stdlib,ACC_PUBLIC);
		tpClosure				= tpClosureClazz.ctype;

		Struct tpTypeSwitchHashClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeSwitchHash;")),kiev_stdlib,ACC_PUBLIC);
		tpTypeSwitchHash			= tpTypeSwitchHashClazz.ctype;


		Struct tpJavaEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/util/Enumeration;")),java_util,ACC_PUBLIC);
		tpJavaEnumeration	= tpJavaEnumerationClazz.ctype;
		
		Struct tpKievEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enumeration;")),kiev_stdlib,ACC_PUBLIC);
		tpKievEnumerationClazz.args.add(new TypeDef(KString.from("A")));
		tpKievEnumeration	= tpKievEnumerationClazz.ctype;
		
		
		Struct tpArrayEnumeratorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/ArrayEnumerator;")),kiev_stdlib,ACC_PUBLIC);
		tpArrayEnumeratorClazz.args.add(new TypeDef(KString.from("A")));
		tpArrayEnumerator	= tpArrayEnumeratorClazz.ctype;
		

		Struct tpPrologVarClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/PVar;")),kiev_stdlib,ACC_PUBLIC);
		tpPrologVarClazz.args.add(new TypeDef(KString.from("A")));
		tpPrologVar	= tpPrologVarClazz.ctype;

		Struct tpRefProxyClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Ref;")),kiev_stdlib,ACC_PUBLIC);
		tpRefProxyClazz.args.add(new TypeDef(KString.from("A")));
		tpRefProxy	= tpRefProxyClazz.ctype;


		TypeDef tdCallRetArg = new TypeDef(KString.from("_ret_"), tpAny);
		tpCallRetArg = new ArgType(tdCallRetArg.name.name,tdCallRetArg);
		tpCallRetArg.flags |= flHidden;
		
		tpCallParamArgs = new ArgType[128];
		for (int i=0; i < tpCallParamArgs.length; i++) {
			TypeDef tdCallParamArg = new TypeDef(KString.from("_"+Integer.toHexString(i)+"_"), tpAny);
			tpCallParamArgs[i] = new ArgType(tdCallParamArg.name.name,tdCallParamArg);
			tpCallParamArgs[i].flags |= flHidden;
		}
		
		tpUnattachedArgs = new ArgType[128] ;
		for (int i=0; i < tpUnattachedArgs.length; i++) {
			TypeDef tdUnattachedArg = new TypeDef(KString.from("_"+Integer.toHexString(i)+"_"), tpAny);
			tpUnattachedArgs[i] = new ArgType(tdUnattachedArg.name.name,tdUnattachedArg);
			tpUnattachedArgs[i].flags |= flHidden;
		}
	}
}

