package kiev.vlang;

import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface Constants extends AccessFlags {

	/** Standard Java names for methods & vars
	 */
	public final static KString nameThis		= KString.from("this");
	public final static KString nameThisDollar	= KString.from("this$");
	public final static KString nameView		= KString.from("$view");
	public final static KString nameTypeInfo	= KString.from("$typeinfo");
	public final static KString nameClTypeInfo	= KString.from("__ti__");
	public final static KString nameUnderscore	= KString.from("_");
	public final static KString nameGetTypeInfo	= KString.from("getTypeInfoField");
	public final static KString nameSuper		= KString.from("super");
	public final static KString nameInit		= KString.from("<init>");
	public final static KString nameClassInit	= KString.from("<clinit>");
	public final static KString nameLength		= KString.from("length");
	public final static KString nameFinalize	= KString.from("finalize");
	public final static KString nameFinalizeSig	= KString.from("()V");
	public final static KString nameIdefault	= KString.from("default");
	public final static KString nameElements	= KString.from("elements");
	public final static KString nameHasMoreElements	= KString.from("hasMoreElements");
	public final static KString nameNextElement	= KString.from("nextElement");
	public final static KString nameCaseTag		= KString.from("case$tag");
	public final static KString nameTagSelf		= KString.from("case$self$tag");
	public final static KString nameGetCaseTag	= KString.from("get$case$tag");
	public final static KString nameFrameProxy	= KString.from("frame$proxy");
	public final static KString nameVarProxy	= KString.from("var$");
	public final static KString nameClone		= KString.from("clone");
	public final static KString nameMMMethods	= KString.from("mm$methods");
	public final static KString nameSet			= KString.from("set$");
	public final static KString nameGet			= KString.from("get$");
	public final static KString nameVarArgs		= KString.from("va_args");
	public final static KString namePEnv		= KString.from("$env$");
	public final static KString namePlvBase		= KString.from("lvar$base");
	public final static KString nameCellVal		= KString.from("$val");
	public final static KString nameEnumValuesFld	= KString.from("$values");
	public final static KString nameEnumValues	= KString.from("values");
	public final static KString nameEnumOrdinal	= KString.from("ordinal");
	public final static KString nameInstance		= KString.from("$instance");
	public final static KString nameClosureArgs		= KString.from("$args");
	public final static KString nameClosureMaxArgs	= KString.from("max$args");
	public final static KString nameClosureTopArg	= KString.from("top$arg");
	public final static KString nameArrayOp			= KString.from("[]");
	public final static KString nameNewOp			= KString.from("new");
	public final static KString nameCastOp			= KString.from("$cast");
	public final static KString nameReturnVar		= KString.from("$return");
	public final static KString nameResultVar		= KString.from("Result");
	public final static KString nameAssertMethod	= KString.from("assert");
	public final static KString nameAssertInvariantMethod	= KString.from("assertInvariant");
	public final static KString nameAssertRequireMethod		= KString.from("assertRequire");
	public final static KString nameAssertEnsureMethod		= KString.from("assertEnsure");
	public final static KString nameAssertSignature	= KString.from("(Ljava/lang/String;)V");
	public final static KString nameAssertNameSignature	= KString.from("(Ljava/lang/String;Ljava/lang/String;)V");
	public final static KString nameKStringSignature	= KString.from("Lkiev/stdlib/KString;");
	public final static KString nameFILE		= KString.from("$FILE");
	public final static KString nameMETHOD		= KString.from("$METHOD");
	public final static KString nameLINENO		= KString.from("$LINENO");
	public final static KString nameDEBUG		= KString.from("$DEBUG");
	public final static KString nameDEF			= KString.from("$D");

	public final static KString nameObjGetClass		= KString.from("getClass");
	public final static KString nameObjHashCode		= KString.from("hashCode");
	public final static KString nameObjEquals		= KString.from("equals");
	public final static KString nameObjClone		= KString.from("clone");
	public final static KString nameObjToString		= KString.from("toString");
	public final static KString nameStrBuffAppend	= KString.from("append");
	public final static KString nameStrValueOf		= KString.from("valueOf");

	// Well known attributes
	public final static KString attrCode				= KString.from("Code");
	public final static KString attrSourceFile			= KString.from("SourceFile");
	public final static KString attrLocalVarTable		= KString.from("LocalVariableTable");
	public final static KString attrLinenoTable		= KString.from("LineNumberTable");
	public final static KString attrExceptions			= KString.from("Exceptions");
	public final static KString attrInnerClasses		= KString.from("InnerClasses");
	public final static KString attrConstantValue		= KString.from("ConstantValue");
	public final static KString attrRequire			= KString.from("kiev.Require");
	public final static KString attrEnsure				= KString.from("kiev.Ensure");
	public final static KString attrRVAnnotations		= KString.from("RuntimeVisibleAnnotations");
	public final static KString attrRIAnnotations		= KString.from("RuntimeInvisibleAnnotations");
	public final static KString attrRVParAnnotations	= KString.from("RuntimeVisibleParameterAnnotations");
	public final static KString attrRIParAnnotations	= KString.from("RuntimeInvisibleParameterAnnotations");
	public final static KString attrAnnotationDefault	= KString.from("AnnotationDefault");

	public final static KString nameAny		= KString.from("any");
	public final static KString sigAny			= KString.from("?");
	public final static KString nameVoid		= KString.from("void");
	public final static KString sigVoid		= KString.from("V");
	public final static KString nameBoolean	= KString.from("boolean");
	public final static KString sigBoolean		= KString.from("Z");
	public final static KString nameByte		= KString.from("byte");
	public final static KString sigByte		= KString.from("B");
	public final static KString nameChar		= KString.from("char");
	public final static KString sigChar		= KString.from("C");
	public final static KString nameShort		= KString.from("short");
	public final static KString sigShort		= KString.from("S");
	public final static KString nameInt		= KString.from("int");
	public final static KString sigInt			= KString.from("I");
	public final static KString nameLong		= KString.from("long");
	public final static KString sigLong		= KString.from("J");
	public final static KString nameFloat		= KString.from("float");
	public final static KString sigFloat		= KString.from("F");
	public final static KString nameDouble		= KString.from("double");
	public final static KString sigDouble		= KString.from("D");
	public final static KString nameNull		= KString.from("null");
	public final static KString sigNull		= KString.from("0");
	public final static KString nameRule		= KString.from("rule");
	public final static KString sigRule		= KString.from("R");


    /** Binary operators priority, image and name */
	public final static int		opAssignPriority		= 5;
	public final static int		opConditionalPriority	= 7;
	public final static int		opBooleanOrPriority		= 10;
	public final static int		opBooleanAndPriority	= 20;
	public final static int		opBitOrPriority			= 30;
	public final static int		opBitXorPriority		= 40;
	public final static int		opBitAndPriority		= 50;
	public final static int		opEqualsPriority		= 60;
	public final static int		opInstanceOfPriority	= 70;
	public final static int		opComparePriority		= 80;
	public final static int		opShiftPriority			= 90;
	public final static int		opAddPriority			= 100;
	public final static int		opMulPriority			= 150;
	public final static int		opCastPriority			= 180;
	public final static int		opNegPriority			= 200;
	public final static int		opIncrPriority			= 210;
	public final static int		opBitNotPriority		= 210;
	public final static int		opBooleanNotPriority	= 210;
	public final static int		opContainerElementPriority		= 230;
	public final static int		opCallPriority			= 240;
	public final static int		opAccessPriority		= 240;

}
