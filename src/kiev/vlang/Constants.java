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
package kiev.vlang;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 213 $
 *
 */

public interface Constants extends AccessFlags {

	/** Standard Java names for methods & vars
	 */
	public final static String nameThis			= "this";
	public final static String nameThisDollar		= "this$";
	public final static String nameImpl			= "$impl";
	public final static String nameTypeInfo		= "$typeinfo";
	public final static String nameClTypeInfo		= "__ti__";
	public final static String nameUnderscore		= "_";
	public final static String nameGetTypeInfo		= "getTypeInfoField";
	public final static String nameSuper			= "super";
	public final static String nameLength			= "length";
	public final static String nameIFaceImpl		= "_Impl_";
	public final static String nameElements		= "elements";
	public final static String nameHasMoreElements= "hasMoreElements";
	public final static String nameNextElement		= "nextElement";
	public final static String nameCaseTag			= "case$tag";
	public final static String nameTagSelf			= "case$self$tag";
	public final static String nameGetCaseTag		= "get$case$tag";
	public final static String nameFrameProxy		= "frame$proxy";
	public final static String nameVarProxy		= "var$";
	public final static String nameClone			= "clone";
	public final static String nameMMMethods		= "mm$methods";
	public final static String nameSet				= "set$";
	public final static String nameGet				= "get$";
	public final static String nameVarArgs			= "va_args";
	public final static String namePEnvParam	= "$env$";
	public static final String namePEnvLVar		= "$env";
	public final static String namePlvBase			= "lvar$base";
	public final static String nameCellVal			= "$val";
	public final static String nameEnumValuesFld	= "$values";
	public final static String nameEnumValues		= "values";
	public final static String nameEnumOrdinal		= "ordinal";
	public final static String nameInstance		= "$instance";
	public final static String nameClosureArgs		= "$args";
	public final static String nameClosureMaxArgs	= "max$args";
	public final static String nameClosureTopArg	= "top$arg";
	public final static String nameArrayTypeOp		= "T []";
	public final static String nameArrayGetOp		= "V [ V ]";
	public final static String nameArraySetOp		= "V [ V ] = V";
	public final static String nameNewOp			= "new T";
	public final static String nameCastOp			= "( T ) V";
	public final static String nameReturnVar		= "$return";
	public final static String nameResultVar		= "Result";
	public final static String nameAssertMethod	= "assert";
	public final static String nameAssertInvariantMethod		= "assertInvariant";
	public final static String nameAssertRequireMethod			= "assertRequire";
	public final static String nameAssertEnsureMethod			= "assertEnsure";
	public final static String nameFILE		= "$FILE";
	public final static String nameMETHOD		= "$METHOD";
	public final static String nameLINENO		= "$LINENO";
	public final static String nameDEBUG		= "$DEBUG";
	public final static String nameDEF			= "$D";

	public final static String nameStrBuffAppend	= "append";

	public final static String nameAny			= "any";
	public final static String nameVoid		= "void";
	public final static String nameBoolean		= "boolean";
	public final static String nameByte		= "byte";
	public final static String nameChar		= "char";
	public final static String nameShort		= "short";
	public final static String nameInt			= "int";
	public final static String nameLong		= "long";
	public final static String nameFloat		= "float";
	public final static String nameDouble		= "double";
	public final static String nameNull		= "null";
	public final static String nameRule		= "rule";


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
