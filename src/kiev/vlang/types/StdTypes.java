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

	public static final ConcreteType tpEnv;
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
	public static final ConcreteType tpRule;
	public static final ConcreteType tpBooleanRef;
	public static final ConcreteType tpByteRef;
	public static final ConcreteType tpCharRef;
	public static final ConcreteType tpNumberRef;
	public static final ConcreteType tpShortRef;
	public static final ConcreteType tpIntRef;
	public static final ConcreteType tpLongRef;
	public static final ConcreteType tpFloatRef;
	public static final ConcreteType tpDoubleRef;
	public static final ConcreteType tpVoidRef;
	public static final ConcreteType tpObject;
	public static final ConcreteType tpClass;
	public static final ConcreteType tpDebug;
	public static final ConcreteType tpTypeInfo;
	public static final ConcreteType tpTypeInfoInterface;
	public static final ConcreteType tpCloneable;
	public static final ConcreteType tpString;
	public static final ConcreteType tpThrowable;
	public static final ConcreteType tpError;
	public static final ConcreteType tpException;
	public static final ConcreteType tpCastException;
	public static final ConcreteType tpJavaEnumeration;
	public static final ConcreteType tpKievEnumeration;
	public static final ConcreteType tpArrayEnumerator;
	public static final ConcreteType tpRuntimeException;
	public static final ConcreteType tpAssertException;
	public static final ConcreteType tpEnum;
	public static final ConcreteType tpAnnotation;
	public static final ConcreteType tpClosure;
	public static final Struct tpClosureClazz;

	public static final ConcreteType tpPrologVar;
	public static final ConcreteType tpRefProxy;

	public static final ConcreteType tpTypeSwitchHash;

	public static final ArrayType tpArray;

	public static final ArgType   tpArrayArg;
	public static final ArgType   tpWrapperArg;
	public static final ArgType[] tpUnattachedArgs;

	static {

		Struct tpEnvClazz = Env.root;
		tpEnv				= new ConcreteType(tpEnvClazz.imeta_type, TVarSet.emptySet);
		((Struct.StructImpl)tpEnvClazz.$v_impl).concr_type		= tpEnv;
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
		tpRule					= tpRuleClazz.concr_type;
		tpRuleClazz.setResolved(true);
		tpRule.flags			= flResolved | flReference;

		Struct java_lang = Env.newPackage(KString.from("java.lang"));
		Struct java_lang_annotation = Env.newPackage(KString.from("java.lang.annotation"));
		Struct java_util = Env.newPackage(KString.from("java.util"));
		Struct kiev_stdlib = Env.newPackage(KString.from("kiev.stdlib"));
		Struct kiev_stdlib_meta = Env.newPackage(KString.from("kiev.stdlib.meta"));

		Struct tpObjectClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Object;")),java_lang,ACC_PUBLIC);
		tpObject				= tpObjectClazz.concr_type;

		Struct tpClassClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Class;")),java_lang,ACC_PUBLIC|ACC_FINAL);
		tpClass					= tpClassClazz.concr_type;

		Struct tpDebugClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Debug;")),kiev_stdlib,ACC_PUBLIC);
		tpDebug				= tpDebugClazz.concr_type;

		Struct tpTypeInfoClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoX;")),kiev_stdlib,ACC_PUBLIC|ACC_FINAL);
		tpTypeInfo				= tpTypeInfoClazz.concr_type;

		Struct tpTypeInfoInterfaceClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeInfoXInterface;")),kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE);
		tpTypeInfoInterface				= tpTypeInfoInterfaceClazz.concr_type;

		Struct tpCloneableClazz = Env.newInterface(ClazzName.fromSignature(KString.from("Ljava/lang/Cloneable;")),java_lang,ACC_PUBLIC|ACC_INTERFACE);
		tpCloneable				= tpCloneableClazz.concr_type;
		tpCloneableClazz.setInterface(true);

		
		TypeDef tdWrapperArg = new TypeDef(KString.from("_boxed_"), tpObject);
		tpWrapperArg = new ArgType(KString.from("_boxed_"),tdWrapperArg);
		
		TypeDef tdArrayArg = new TypeDef(KString.from("_elem_"), tpAny);
		tpArrayArg = new ArgType(KString.from("_elem_"),tdArrayArg);
		tpArray					= ArrayType.newArrayType(Type.tpAny);
		tpArray.flags			|= flResolved | flReference | flArray;

		Struct tpBooleanRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Boolean;")),java_lang,ACC_PUBLIC);
		tpBooleanRef			= tpBooleanRefClazz.concr_type;

		Struct tpCharRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Character;")),java_lang,ACC_PUBLIC);
		tpCharRef			= tpCharRefClazz.concr_type;

		Struct tpNumberRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Number;")),java_lang,ACC_PUBLIC);
		tpNumberRef			= tpNumberRefClazz.concr_type;

		Struct tpByteRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Byte;")),java_lang,ACC_PUBLIC);
		tpByteRef			= tpByteRefClazz.concr_type;

		Struct tpShortRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Short;")),java_lang,ACC_PUBLIC);
		tpShortRef			= tpShortRefClazz.concr_type;

		Struct tpIntRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Integer;")),java_lang,ACC_PUBLIC);
		tpIntRef			= tpIntRefClazz.concr_type;

		Struct tpLongRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Long;")),java_lang,ACC_PUBLIC);
		tpLongRef			= tpLongRefClazz.concr_type;

		Struct tpFloatRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Float;")),java_lang,ACC_PUBLIC);
		tpFloatRef			= tpFloatRefClazz.concr_type;

		Struct tpDoubleRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Double;")),java_lang,ACC_PUBLIC);
		tpDoubleRef			= tpDoubleRefClazz.concr_type;

		Struct tpVoidRefClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Void;")),java_lang,ACC_PUBLIC);
		tpVoidRef			= tpVoidRefClazz.concr_type;

		Struct tpStringClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/String;")),java_lang,ACC_PUBLIC);
		tpString				= tpStringClazz.concr_type;

		Struct tpAnnotationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/annotation/Annotation;")),java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT);
		tpAnnotation			= tpAnnotationClazz.concr_type;
		
		Struct tpThrowableClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Throwable;")),java_lang,ACC_PUBLIC);
		tpThrowable				= tpThrowableClazz.concr_type;

		Struct tpErrorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Error;")),java_lang,ACC_PUBLIC);
		tpError				= tpErrorClazz.concr_type;

		Struct tpExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Exception;")),java_lang,ACC_PUBLIC);
		tpException				= tpExceptionClazz.concr_type;

		Struct tpCastExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/ClassCastException;")),java_lang,ACC_PUBLIC);
		tpCastException				= tpCastExceptionClazz.concr_type;

		Struct tpRuntimeExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/RuntimeException;")),java_lang,ACC_PUBLIC);
		tpRuntimeException				= tpRuntimeExceptionClazz.concr_type;

		Struct tpAssertExceptionClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/AssertionFailedException;")),kiev_stdlib,ACC_PUBLIC);
		tpAssertException				= tpAssertExceptionClazz.concr_type;

		Struct tpEnumClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/lang/Enum;")),java_lang,ACC_PUBLIC | ACC_ABSTRACT);
		tpEnum					= tpEnumClazz.concr_type;

		tpClosureClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/closure;")),kiev_stdlib,ACC_PUBLIC);
		tpClosure				= tpClosureClazz.concr_type;

		Struct tpTypeSwitchHashClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/TypeSwitchHash;")),kiev_stdlib,ACC_PUBLIC);
		tpTypeSwitchHash			= tpTypeSwitchHashClazz.concr_type;


		Struct tpJavaEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Ljava/util/Enumeration;")),java_util,ACC_PUBLIC);
		tpJavaEnumeration	= tpJavaEnumerationClazz.concr_type;
		
		Struct tpKievEnumerationClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Enumeration;")),kiev_stdlib,ACC_PUBLIC);
		tpKievEnumerationClazz.args.add(new TypeDef(KString.from("A")));
		tpKievEnumeration	= tpKievEnumerationClazz.concr_type;
		
		
		Struct tpArrayEnumeratorClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/ArrayEnumerator;")),kiev_stdlib,ACC_PUBLIC);
		tpArrayEnumeratorClazz.args.add(new TypeDef(KString.from("A")));
		tpArrayEnumerator	= tpArrayEnumeratorClazz.concr_type;
		

		Struct tpPrologVarClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/PVar;")),kiev_stdlib,ACC_PUBLIC);
		tpPrologVarClazz.args.add(new TypeDef(KString.from("A")));
		tpPrologVar	= tpPrologVarClazz.concr_type;

		Struct tpRefProxyClazz = Env.newStruct(ClazzName.fromSignature(KString.from("Lkiev/stdlib/Ref;")),kiev_stdlib,ACC_PUBLIC);
		tpRefProxyClazz.args.add(new TypeDef(KString.from("A")));
		tpRefProxy	= tpRefProxyClazz.concr_type;

		tpUnattachedArgs = new ArgType[128] ;
		for (int i=0; i < tpUnattachedArgs.length; i++) {
			TypeDef tdUnattachedArg = new TypeDef(KString.from("_"+Integer.toHexString(i)+"_"), tpAny);
			tpUnattachedArgs[i] = new ArgType(tdUnattachedArg.name.name,tdUnattachedArg);
		}
	}
}

