package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

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
	public static final int flArgumented		= 1 << 11;
	public static final int flRtArgumented		= 1 << 12;
	public static final int flArgVirtual		= 1 << 13;

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

	public static final ArgumentType   tpArrayArg;
	public static final ArgumentType   tpWrapperArg;
	public static final ArgumentType[] tpUnattachedArgs;

	static {

		Struct tpEnvClazz = Env.root;
		tpEnv				= new BaseType(tpEnvClazz);
		((Struct.StructImpl)tpEnvClazz.$v_impl).type		= tpEnv;
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
		tpRule					= tpRuleClazz.type;
		tpRuleClazz.setResolved(true);
		tpRule.flags			= flResolved | flReference;

		Struct java_lang = Env.newPackage(KString.from("java.lang"));
		Struct java_lang_annotation = Env.newPackage(KString.from("java.lang.annotation"));
		Struct java_util = Env.newPackage(KString.from("java.util"));
		Struct kiev_stdlib = Env.newPackage(KString.from("kiev.stdlib"));
		Struct kiev_stdlib_meta = Env.newPackage(KString.from("kiev.stdlib.meta"));

		Struct tpObjectClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Object;")),java_lang,ACC_PUBLIC);
		tpObject				= tpObjectClazz.type;

		Struct tpClassClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Class;")),java_lang,ACC_PUBLIC|ACC_FINAL);
		tpClass					= tpClassClazz.type;

		Struct tpDebugClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Debug;")),kiev_stdlib,ACC_PUBLIC);
		tpDebug				= tpDebugClazz.type;

		Struct tpTypeInfoClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfo;")),kiev_stdlib,ACC_PUBLIC|ACC_FINAL);
		tpTypeInfo				= tpTypeInfoClazz.type;

		Struct tpTypeInfoInterfaceClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoInterface;")),kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE);
		tpTypeInfoInterface				= tpTypeInfoInterfaceClazz.type;

		Struct tpCloneableClazz = Env.newInterface(ClazzName.fromSignature(KString.from("Ljava/lang/Cloneable;")),java_lang,ACC_PUBLIC|ACC_INTERFACE);
		tpCloneable				= tpCloneableClazz.type;
		tpCloneableClazz.setInterface(true);

		
		tpWrapperArg = new ArgumentType(KString.from("_boxed_"),null,Type.tpObject, false, false);
		
		tpArrayArg = new ArgumentType(KString.from("_elem_"),null,Type.tpAny, false, false);
		tpArray					= ArrayType.newArrayType(Type.tpAny);
		tpArray.flags			|= flResolved | flReference | flArray;

		Struct tpBooleanRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Boolean;")),java_lang,ACC_PUBLIC);
		tpBooleanRef			= tpBooleanRefClazz.type;

		Struct tpCharRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Character;")),java_lang,ACC_PUBLIC);
		tpCharRef			= tpCharRefClazz.type;

		Struct tpNumberRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Number;")),java_lang,ACC_PUBLIC);
		tpNumberRef			= tpNumberRefClazz.type;

		Struct tpByteRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Byte;")),java_lang,ACC_PUBLIC);
		tpByteRef			= tpByteRefClazz.type;

		Struct tpShortRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Short;")),java_lang,ACC_PUBLIC);
		tpShortRef			= tpShortRefClazz.type;

		Struct tpIntRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Integer;")),java_lang,ACC_PUBLIC);
		tpIntRef			= tpIntRefClazz.type;

		Struct tpLongRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Long;")),java_lang,ACC_PUBLIC);
		tpLongRef			= tpLongRefClazz.type;

		Struct tpFloatRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Float;")),java_lang,ACC_PUBLIC);
		tpFloatRef			= tpFloatRefClazz.type;

		Struct tpDoubleRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Double;")),java_lang,ACC_PUBLIC);
		tpDoubleRef			= tpDoubleRefClazz.type;

		Struct tpVoidRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Void;")),java_lang,ACC_PUBLIC);
		tpVoidRef			= tpVoidRefClazz.type;

		Struct tpStringClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/String;")),java_lang,ACC_PUBLIC);
		tpString				= tpStringClazz.type;

		Struct tpAnnotationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/annotation/Annotation;")),java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
		tpAnnotation			= tpAnnotationClazz.type;
		
		Struct tpThrowableClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Throwable;")),java_lang,ACC_PUBLIC);
		tpThrowable				= tpThrowableClazz.type;

		Struct tpErrorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Error;")),java_lang,ACC_PUBLIC);
		tpError				= tpErrorClazz.type;

		Struct tpExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Exception;")),java_lang,ACC_PUBLIC);
		tpException				= tpExceptionClazz.type;

		Struct tpCastExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/ClassCastException;")),java_lang,ACC_PUBLIC);
		tpCastException				= tpCastExceptionClazz.type;

		Struct tpRuntimeExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/RuntimeException;")),java_lang,ACC_PUBLIC);
		tpRuntimeException				= tpRuntimeExceptionClazz.type;

		Struct tpAssertExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/AssertionFailedException;")),kiev_stdlib,ACC_PUBLIC);
		tpAssertException				= tpAssertExceptionClazz.type;

		Struct tpEnumClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Enum;")),java_lang,ACC_PUBLIC | ACC_ABSTRACT);
		tpEnum					= tpEnumClazz.type;

		tpClosureClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/closure;")),kiev_stdlib,ACC_PUBLIC);
		tpClosure				= tpClosureClazz.type;

		Struct tpTypeSwitchHashClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeSwitchHash;")),kiev_stdlib,ACC_PUBLIC);
		tpTypeSwitchHash			= tpTypeSwitchHashClazz.type;


		Struct tpJavaEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/util/Enumeration;")),java_util,ACC_PUBLIC);
		tpJavaEnumeration	= tpJavaEnumerationClazz.type;
		
		Struct tpKievEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enumeration;")),kiev_stdlib,ACC_PUBLIC);
		tpKievEnumerationClazz.args.add(new TypeDef(KString.from("A")));
		tpKievEnumeration	= tpKievEnumerationClazz.type;
		
		
		Struct tpArrayEnumeratorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/ArrayEnumerator;")),kiev_stdlib,ACC_PUBLIC);
		tpArrayEnumeratorClazz.args.add(new TypeDef(KString.from("A")));
		tpArrayEnumerator	= tpArrayEnumeratorClazz.type;
		

		Struct tpPrologVarClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/PVar;")),kiev_stdlib,ACC_PUBLIC);
		tpPrologVarClazz.args.add(new TypeDef(KString.from("A")));
		tpPrologVar	= tpPrologVarClazz.type;

		Struct tpRefProxyClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Ref;")),kiev_stdlib,ACC_PUBLIC);
		tpRefProxyClazz.args.add(new TypeDef(KString.from("A")));
		tpRefProxy	= tpRefProxyClazz.type;

		tpUnattachedArgs = new ArgumentType[128] ;
		for (int i=0; i < tpUnattachedArgs.length; i++)
			tpUnattachedArgs[i] = new ArgumentType(KString.from("_"+Integer.toHexString(i)+"_"),null,Type.tpAny, false, false);
	}
}

