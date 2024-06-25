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
package kiev.fmt;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsALanguage(name="syntax")
@singleton
public final class SyntaxLang extends LangBase {
	{
		this.defaultEditorSyntaxName = "stx-fmt·syntax-for-syntax";
		this.defaultInfoSyntaxName = "stx-fmt·syntax-for-syntax";
	}
	public String getName() { return "syntax"; }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
		ATextSyntax.class,
			TextSyntax.class,
			TopLevelTextSyntax.class,
			TreeSyntax.class,
		StyleSheet.class,
		SpaceInfo.class,
		SpaceCmd.class,
		ParagraphLayout.class,
		SyntaxSize.class,
		ParagraphOption.class,
			ParagraphLines.class,
			ParagraphAlignBlock.class,
			ParagraphAlignContent.class,
			ParagraphSize.class,
			ParagraphInset.class,
			ParagraphNoIndent.class,
			ParagraphIndent.class,
		DrawColor.class,
		DrawFont.class,
		SyntaxNodeTemplate.class,
		SyntaxTypeRef.class,
		SyntaxExpectedType.class,
		SyntaxExpectedTemplate.class,
		ASyntaxElemDecl.class,
			PartialSyntaxElemDecl.class,
			SyntaxElemDecl.class,
			SyntaxIdentTemplate.class,
		SyntaxStyleDecl.class,
		SyntaxElemFormatDecl.class,
		SyntaxFunc.class,
			SyntaxFuncEval.class,
			SyntaxFuncSetEnum.class,
			SyntaxFuncNewByTemplate.class,
			SyntaxFuncNewByFactory.class,
		SyntaxElem.class,
			SyntaxElemRef.class,
			SyntaxToken.class,
			SyntaxIcon.class,
			SyntaxPlaceHolder.class,
			SyntaxAttr.class,
			SyntaxAttrFormat.class,
				SyntaxSubAttr.class,
				SyntaxList.class,
				SyntaxTreeBranch.class,
				SyntaxTokenAttr.class,
				SyntaxIdentAttr.class,
				SyntaxCharAttr.class,
				SyntaxStrAttr.class,
			SyntaxNode.class,
			SyntaxSet.class,
			SyntaxSpace.class,
			CalcOption.class,
				CalcOptionAnd.class,
				CalcOptionOr.class,
				CalcOptionNot.class,
				CalcOptionNotNull.class,
				CalcOptionNotEmpty.class,
				CalcOptionTrue.class,
				CalcOptionClass.class,
				CalcOptionHasMeta.class,
				CalcOptionIsHidden.class,
				CalcOptionHasNoSyntaxParent.class,
				CalcOptionIncludeInDump.class,
			SyntaxOptional.class,
			SyntaxEnumChoice.class,
			SyntaxFolder.class,
		// expressions support
		SyntaxExprTemplate.class,
		SyntaxExpr.class,
		SyntaxAutoParenth.class,
		// java syntax
		SyntaxJavaAccess.class,
		SyntaxJavaPackedField.class
	};
}

