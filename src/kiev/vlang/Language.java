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

import kiev.fmt.ATextSyntax;
import kiev.fmt.Draw_ATextSyntax;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public interface Language {
	public Class[] getSuperLanguages();
	public Class[] getNodeClasses();
	public Draw_ATextSyntax getDefaultEditorSyntax();
	public Draw_ATextSyntax getDefaultInfoSyntax();
}

@singleton
public final class CoreLang implements Language {
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }
	public Draw_ATextSyntax getDefaultEditorSyntax() {
		if (defaultEditorSyntax == null)
			defaultEditorSyntax = Env.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java");
		return defaultEditorSyntax;
	}
	public Draw_ATextSyntax getDefaultInfoSyntax() {
		if (defaultInfoSyntax == null)
			defaultInfoSyntax = Env.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java");
		return defaultInfoSyntax;
	}
	private static Draw_ATextSyntax defaultEditorSyntax;
	private static Draw_ATextSyntax defaultInfoSyntax;
	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
	ASTNode.class,
		Symbol.class,
		SymbolRef.class,
		DNode.class,	// declaration nodes
			TypeDecl.class,		// type declaration
				MetaTypeDecl.class,		// abstract type declaration
				TypeOpDef.class,		// declare a type using operator
				TypeDef.class,			// typedef
					TypeAssign.class,	// assign typedef (alias)
					TypeConstr.class,	// constrained typedef (upper/lower bounds)
				Struct.class,			// abstract structure
					KievPackage.class,
					KievSyntax.class,
					JavaClass.class,
					JavaAnonymouseClass.class,
					JavaInterface.class,
					KievView.class,
					JavaAnnotation.class,
					PizzaCase.class,
					JavaEnum.class,
					Env.class,
			Label.class,		// code label
			Method.class,
				MethodImpl.class,	// a concrete method
				Constructor.class,	// type constructor
			Initializer.class,
			WBCCondition.class,		// work-by-contract condition
			Var.class,				// variables:
				LVar.class,				// local
				Field.class,			// fields
				RewritePattern.class,	// pattern
		ENode.class,	// expression nodes
			NopExpr.class,
			BoolExpr.class,			// boolean expressions / data flow
				BinaryBooleanOrExpr.class,
				BinaryBooleanAndExpr.class,
				BinaryBoolExpr.class,
				InstanceofExpr.class,
				BooleanNotExpr.class,
			CallExpr.class,
			CtorCallExpr.class,
			ClosureCallExpr.class,
			ConstExpr.class,
				ConstBoolExpr.class,
				ConstNullExpr.class,
				ConstByteExpr.class,
				ConstShortExpr.class,
				ConstIntExpr.class,
				ConstLongExpr.class,
				ConstCharExpr.class,
				ConstFloatExpr.class,
				ConstDoubleExpr.class,
				ConstStringExpr.class,
				ConstEnumExpr.class,
			LvalueExpr.class,
				AccessExpr.class,
				IFldExpr.class,
				ContainerAccessExpr.class,
				ThisExpr.class,
				SuperExpr.class,
				LVarExpr.class,
				SFldExpr.class,
				OuterThisAccessExpr.class,
				ReinterpExpr.class,
			MetaValue.class,
				MetaValueScalar.class,
				MetaValueArray.class,
			NewExpr.class,
				NewEnumExpr.class,
				NewArrayExpr.class,
				NewInitializedArrayExpr.class,
				NewClosure.class,
			TypeRef.class,
				TypeDeclRef.class,
				TypeClosureRef.class,
				TypeExpr.class,
				TypeNameRef.class,
			Shadow.class,
			TypeClassExpr.class,
			TypeInfoExpr.class,
			AssertEnabledExpr.class,
			AssignExpr.class,
			BinaryExpr.class,
			UnaryExpr.class,
			StringConcatExpr.class,
			CommaExpr.class,
			Block.class,
			IncrementExpr.class,
			ConditionalExpr.class,
			CastExpr.class,
			CoreExpr.class,
			InlineMethodStat.class,
			ExprStat.class,
				ReturnStat.class,
				ThrowStat.class,
				IfElseStat.class,
				CondStat.class,
				LabeledStat.class,
				BreakStat.class,
				ContinueStat.class,
				GotoStat.class,
				GotoCaseStat.class,
				LoopStat.class,
					WhileStat.class,
					DoWhileStat.class,
					ForStat.class,
					ForEachStat.class,
				SwitchStat.class,
					SwitchEnumStat.class,
					SwitchTypeStat.class,
					MatchStat.class,
					CaseLabel.class,
				TryStat.class,
					CatchInfo.class,
					FinallyInfo.class,
				SynchronizedStat.class,
				WithStat.class,
		MNode.class,	// meta-data (annotation) nodes
			UserMeta.class,		// user-defined meta
				MetaUUID.class,		// store UUID of DNode-s
				MetaPacked.class,	// data for packed fields
				MetaPacker.class,	// data for packer field
				MetaThrows.class,	// data for throwed exceptions
			MetaFlag.class,		// special meta-data, acting as flags
				MetaAccess.class,		// access rights (public, private and so on)
				MetaUnerasable.class,	// runtime type uneraseble nodes
				MetaSingleton.class,
				MetaMixin.class,
				MetaForward.class,		// forward resolving for vars/fields
				MetaVirtual.class,
				MetaMacro.class,
				MetaStatic.class,
				MetaAbstract.class,
				MetaFinal.class,
				MetaNative.class,
				MetaSynchronized.class,
				MetaTransient.class,
				MetaVolatile.class,
				MetaBridge.class,
				MetaVarArgs.class,
				MetaSynthetic.class,
		SNode.class,	// syntax nodes
			DirUnit.class,		// directory
			NameSpace.class,	// name space unit
				FileUnit.class,	// file/compilation unit, also acts as a namespace
			Opdef.class,		// operator declaration
			DeclGroup.class,	// group of nodes, which share a base type and meta-data
				DeclGroupVars.class,
				DeclGroupEnumFields.class,
			Import.class,
			Comment.class,		// comments
		// in parser
		ASTExpression.class,
		ASTOperatorAlias.class,
		ASTPragma.class,
		EToken.class,
		// to be removed
		MetaSet.class
	};
}

@singleton
public final class LogicLang implements Language {
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }
	public Draw_ATextSyntax getDefaultEditorSyntax() {
		if (defaultEditorSyntax == null)
			defaultEditorSyntax = Env.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java");
		return defaultEditorSyntax;
	}
	public Draw_ATextSyntax getDefaultInfoSyntax() {
		if (defaultInfoSyntax == null)
			defaultInfoSyntax = Env.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java");
		return defaultInfoSyntax;
	}
	private static Draw_ATextSyntax defaultEditorSyntax;
	private static Draw_ATextSyntax defaultInfoSyntax;
	private static Class[] superLanguages = {CoreLang.class};
	private static Class[] nodeClasses = {
		RuleMethod.class,		// extends Method
		RuleBlock.class,		// body of RuleMethod
		ASTRuleNode.class,		// abstract rule node
			RuleOrExpr.class,		// OR join of rules
			RuleAndExpr.class,		// AND sequence of rules
			RuleIstheExpr.class,	// bind/check one value
			RuleIsoneofExpr.class,	// iterate/check values from a container
			RuleCutExpr.class,		// local method cut
			RuleCallExpr.class,		// call/iterate another rule method
			RuleExprBase.class,		// base expression
				RuleWhileExpr.class,	// check while external expression is true
				RuleExpr.class			// check if external expression is true
	};
}

@singleton
public final class MacroLang implements Language {
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }
	public Draw_ATextSyntax getDefaultEditorSyntax() {
		if (defaultEditorSyntax == null)
			defaultEditorSyntax = Env.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java");
		return defaultEditorSyntax;
	}
	public Draw_ATextSyntax getDefaultInfoSyntax() {
		if (defaultInfoSyntax == null)
			defaultInfoSyntax = Env.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java");
		return defaultInfoSyntax;
	}
	private static Draw_ATextSyntax defaultEditorSyntax;
	private static Draw_ATextSyntax defaultInfoSyntax;
	private static Class[] superLanguages = {CoreLang.class};
	private static Class[] nodeClasses = {
		RewriteMatch.class,
		RewritePattern.class,
		RewriteCase.class,
		RewriteNodeFactory.class,
		RewriteNodeArg.class,
		RewriteNodeArgArray.class,
		BlockRewr.class,
		IfElseRewr.class,
		SwitchRewr.class,
		ForEachRewr.class,
		MacroAccessExpr.class,
		MacroSubstExpr.class,
		MacroSubstTypeRef.class,
		MacroBinaryBoolExpr.class,
		MacroHasMetaExpr.class
	};
}

