package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.TypeArgDef;

import static kiev.vlang.AccessFlags.*;
import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
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
	public static final int flWrapper			=  512;
	public static final int flCallable			= 1024;
	public static final int flArgumented		= 2048;
	public static final int flUnbound			= 4096;

	public static final BaseType tpEnv;
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
	public static final BaseType tpRule;
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
	public static final BaseType tpObject;
	public static final BaseType tpClass;
	public static final BaseType tpDebug;
	public static final BaseType tpTypeInfo;
	public static final BaseType tpTypeInfoInterface;
	public static final BaseType tpCloneable;
	public static final BaseType tpString;
	public static final BaseType tpThrowable;
	public static final BaseType tpError;
	public static final BaseType tpException;
	public static final BaseType tpCastException;
	public static final BaseType tpJavaEnumeration;
	public static final BaseType tpKievEnumeration;
	public static final BaseType tpArrayEnumerator;
	public static final BaseType tpRuntimeException;
	public static final BaseType tpAssertException;
	public static final BaseType tpEnum;
	public static final BaseType tpAnnotation;
	public static final BaseType tpClosure;
	public static final Struct tpClosureClazz;

	public static final BaseType tpPrologVar;
	public static final BaseType tpRefProxy;

	public static final BaseType tpTypeSwitchHash;

	public static final ArrayType tpArray;

	static {

		Struct tpEnvClazz = Env.root;
		tpEnv				= new BaseType(KString.from("<root>"), tpEnvClazz);
		tpEnvClazz.type		= tpEnv;
		tpEnv.flags			= flResolved;

		tpAny		= new CoreType(Constants.sigAny,     Constants.nameAny,     0);
		tpVoid		= new CoreType(Constants.sigVoid,    Constants.nameVoid,    0);
		tpBoolean	= new CoreType(Constants.sigBoolean, Constants.nameBoolean, flBoolean | flIntegerInCode);
		tpByte		= new CoreType(Constants.sigByte,    Constants.nameByte,    flInteger | flIntegerInCode);
		tpChar		= new CoreType(Constants.sigChar,    Constants.nameChar,    flInteger | flIntegerInCode);
		tpShort		= new CoreType(Constants.sigShort,   Constants.nameShort,   flInteger | flIntegerInCode);
		tpInt		= new CoreType(Constants.sigInt,     Constants.nameInt,     flInteger | flIntegerInCode);
		tpLong		= new CoreType(Constants.sigLong,    Constants.nameLong,    flInteger | flDoubleSize);
		tpFloat		= new CoreType(Constants.sigFloat,   Constants.nameFloat,   flFloat);
		tpDouble	= new CoreType(Constants.sigDouble,  Constants.nameDouble,  flFloat   | flDoubleSize);
		tpNull		= new CoreType(Constants.sigNull,    Constants.nameNull,    flReference);
//		tpRule		= new CoreType(Constants.sigRule,    Constants.nameRule,    flReference);

		Struct tpRuleClazz = Env.newStruct(new ClazzName(
							KString.from("rule"),
							KString.from("rule"),
							KString.from("R"),false,false),null,ACC_PUBLIC);
		tpRule					= new BaseType(KString.from("R"), tpRuleClazz);
		tpRuleClazz.type		= tpRule;
		tpRuleClazz.setResolved(true);
		tpRule.flags			= flResolved | flReference;

		Struct java_lang = Env.newPackage(KString.from("java.lang"));
		Struct java_lang_annotation = Env.newPackage(KString.from("java.lang.annotation"));
		Struct java_util = Env.newPackage(KString.from("java.util"));
		Struct kiev_stdlib = Env.newPackage(KString.from("kiev.stdlib"));
		Struct kiev_stdlib_meta = Env.newPackage(KString.from("kiev.stdlib.meta"));

		Struct tpObjectClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Object;")),java_lang,ACC_PUBLIC);
		tpObject				= new BaseType(tpObjectClazz);
		tpObjectClazz.type		= tpObject;
		tpObject.flags			= flReference;

		Struct tpClassClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Class;")),java_lang,ACC_PUBLIC|ACC_FINAL);
		tpClass				= new BaseType(tpClassClazz);
		tpClassClazz.type		= tpClass;
		tpClass.flags			= flReference;

		Struct tpDebugClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Debug;")),kiev_stdlib,ACC_PUBLIC);
		tpDebug				= new BaseType(tpDebugClazz);
		tpDebugClazz.type	= tpDebug;
		tpDebug.flags		= flReference;

		Struct tpTypeInfoClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfo;")),kiev_stdlib,ACC_PUBLIC|ACC_FINAL);
		tpTypeInfo				= new BaseType(tpTypeInfoClazz);
		tpTypeInfoClazz.type	= tpTypeInfo;
		tpTypeInfo.flags		= flReference;

		Struct tpTypeInfoInterfaceClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoInterface;")),kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE);
		tpTypeInfoInterface				= new BaseType(tpTypeInfoInterfaceClazz);
		tpTypeInfoInterfaceClazz.type	= tpTypeInfoInterface;
		tpTypeInfoInterface.flags		= flReference;

		Struct tpCloneableClazz = Env.newInterface(ClazzName.fromSignature(KString.from("Ljava/lang/Cloneable;")),java_lang,ACC_PUBLIC|ACC_INTERFACE);
		tpCloneable				= new BaseType(tpCloneableClazz);
		tpCloneableClazz.type	= tpCloneable;
		tpCloneable.flags		= flReference;
		tpCloneableClazz.setInterface(true);

		tpArray					= ArrayType.newArrayType(Type.tpAny);
		tpArray.flags			|= flResolved | flReference | flArray;

		Struct tpBooleanRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Boolean;")),java_lang,ACC_PUBLIC);
		tpBooleanRef			= new BaseType(tpBooleanRefClazz);
		tpBooleanRefClazz.type	= tpBooleanRef;

		Struct tpCharRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Character;")),java_lang,ACC_PUBLIC);
		tpCharRef			= new BaseType(tpCharRefClazz);
		tpCharRefClazz.type	= tpCharRef;

		Struct tpNumberRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Number;")),java_lang,ACC_PUBLIC);
		tpNumberRef			= new BaseType(tpNumberRefClazz);
		tpNumberRefClazz.type	= tpNumberRef;

		Struct tpByteRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Byte;")),java_lang,ACC_PUBLIC);
		tpByteRef			= new BaseType(tpByteRefClazz);
		tpByteRefClazz.type	= tpByteRef;

		Struct tpShortRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Short;")),java_lang,ACC_PUBLIC);
		tpShortRef			= new BaseType(tpShortRefClazz);
		tpShortRefClazz.type	= tpShortRef;

		Struct tpIntRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Integer;")),java_lang,ACC_PUBLIC);
		tpIntRef			= new BaseType(tpIntRefClazz);
		tpIntRefClazz.type	= tpIntRef;

		Struct tpLongRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Long;")),java_lang,ACC_PUBLIC);
		tpLongRef			= new BaseType(tpLongRefClazz);
		tpLongRefClazz.type	= tpLongRef;

		Struct tpFloatRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Float;")),java_lang,ACC_PUBLIC);
		tpFloatRef			= new BaseType(tpFloatRefClazz);
		tpFloatRefClazz.type	= tpFloatRef;

		Struct tpDoubleRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Double;")),java_lang,ACC_PUBLIC);
		tpDoubleRef			= new BaseType(tpDoubleRefClazz);
		tpDoubleRefClazz.type	= tpDoubleRef;

		Struct tpVoidRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Void;")),java_lang,ACC_PUBLIC);
		tpVoidRef			= new BaseType(tpVoidRefClazz);
		tpVoidRefClazz.type	= tpVoidRef;

		Struct tpStringClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/String;")),java_lang,ACC_PUBLIC);
		tpString				= new BaseType(tpStringClazz);
		tpStringClazz.type		= tpString;

		Struct tpAnnotationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/annotation/Annotation;")),java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
		tpAnnotation			= new BaseType(tpAnnotationClazz);
		tpAnnotationClazz.type	= tpAnnotation;
		
		Struct tpThrowableClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Throwable;")),java_lang,ACC_PUBLIC);
		tpThrowable				= new BaseType(tpThrowableClazz);
		tpThrowableClazz.type	= tpThrowable;

		Struct tpErrorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Error;")),java_lang,ACC_PUBLIC);
		tpError				= new BaseType(tpErrorClazz);
		tpErrorClazz.type	= tpError;

		Struct tpExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Exception;")),java_lang,ACC_PUBLIC);
		tpException				= new BaseType(tpExceptionClazz);
		tpExceptionClazz.type	= tpException;

		Struct tpCastExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/ClassCastException;")),java_lang,ACC_PUBLIC);
		tpCastException				= new BaseType(tpCastExceptionClazz);
		tpCastExceptionClazz.type	= tpCastException;

		Struct tpRuntimeExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/RuntimeException;")),java_lang,ACC_PUBLIC);
		tpRuntimeException				= new BaseType(tpRuntimeExceptionClazz);
		tpRuntimeExceptionClazz.type	= tpRuntimeException;

		Struct tpAssertExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/AssertionFailedException;")),kiev_stdlib,ACC_PUBLIC);
		tpAssertException				= new BaseType(tpAssertExceptionClazz);
		tpAssertExceptionClazz.type	= tpAssertException;

		Struct tpEnumClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Enum;")),java_lang,ACC_PUBLIC | ACC_ABSTRACT);
		tpEnum					= new BaseType(tpEnumClazz);
		tpEnumClazz.type		= tpEnum;

		tpClosureClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/closure;")),kiev_stdlib,ACC_PUBLIC);
		tpClosure				= new BaseType(tpClosureClazz);
		tpClosureClazz.type		= tpClosure;

		Struct tpJavaEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/util/Enumeration;")),java_util,ACC_PUBLIC);
		tpJavaEnumeration	= new BaseType(tpJavaEnumerationClazz);
		tpJavaEnumerationClazz.type	= tpJavaEnumeration;

		Struct tpKievEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enumeration;")),kiev_stdlib,ACC_PUBLIC);
		tpKievEnumerationClazz.args.add(new TypeArgDef(KString.from("A")));
		ArgumentType tpKievEnumerationArg = tpKievEnumerationClazz.args[0].getAType();
		tpKievEnumeration	= new BaseType(
					KString.from("Lkiev/stdlib/Enumeration;<A:Akiev/stdlib/Enumeration$A;>"),
					tpKievEnumerationClazz,new Type[]{tpKievEnumerationArg});
		tpKievEnumerationClazz.type	= tpKievEnumeration;
		
		
		Struct tpArrayEnumeratorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/ArrayEnumerator;")),kiev_stdlib,ACC_PUBLIC);
		tpArrayEnumeratorClazz.args.add(new TypeArgDef(KString.from("A")));
		ArgumentType tpArrayEnumeratorArg = tpArrayEnumeratorClazz.args[0].getAType();
		tpArrayEnumerator	= new BaseType(
					KString.from("Lkiev/stdlib/ArrayEnumerator;<A:Akiev/stdlib/ArrayEnumerator$A;>"),
					tpArrayEnumeratorClazz,new Type[]{tpArrayEnumeratorArg});
		tpArrayEnumeratorClazz.type	= tpArrayEnumerator;
		

		Struct tpPrologVarClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/PVar;")),kiev_stdlib,ACC_PUBLIC);
		tpPrologVarClazz.args.add(new TypeArgDef(KString.from("A")));
		ArgumentType tpPrologVarArg = tpPrologVarClazz.args[0].getAType();
		tpPrologVar	= new BaseType(
					KString.from("Lkiev/stdlib/PVar;<A:Akiev/stdlib/PVar$A;>"),
					tpPrologVarClazz,new Type[]{tpPrologVarArg});
		tpPrologVarClazz.type	= tpPrologVar;

		Struct tpRefProxyClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Ref;")),kiev_stdlib,ACC_PUBLIC);
		tpRefProxyClazz.args.add(new TypeArgDef(KString.from("A")));
		ArgumentType tpRefProxyArg = tpRefProxyClazz.args[0].getAType();
		tpRefProxy	= new BaseType(
					KString.from("Lkiev/stdlib/Ref;<A:Akiev/stdlib/Ref$A;>"),
					tpRefProxyClazz,new Type[]{tpRefProxyArg});
		tpRefProxyClazz.type	= tpRefProxy;

		Struct tpTypeSwitchHashClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeSwitchHash;")),kiev_stdlib,ACC_PUBLIC);
		tpTypeSwitchHash			= new BaseType(tpTypeSwitchHashClazz);
		tpTypeSwitchHashClazz.type	= tpTypeSwitchHash;

	}
}

