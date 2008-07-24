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

@singleton
public final class SyntaxLang extends LangBase {
	static {
		defaultEditorSyntaxName = "stx-fmt\u001fsyntax-for-syntax";
		defaultInfoSyntaxName = "stx-fmt\u001fsyntax-for-syntax";
	}
	public String getName() { "syntax" }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
		ATextSyntax.class,
			TextSyntax.class,
			XmlDumpSyntax.class,
			TreeSyntax.class,
		SpaceInfo.class,
		SpaceCmd.class,
		IndentInfo.class,
		ParagraphLayout.class,
		DrawColor.class,
		DrawFont.class,
		SyntaxExpectedAttr.class,
		SyntaxNodeTemplate.class,
		ASyntaxElemDecl.class,
			PartialSyntaxElemDecl.class,
			SyntaxElemDecl.class,
			SyntaxIdentTemplate.class,
			SyntaxExpectedTemplate.class,
		SyntaxElemFormatDecl.class,
		SyntaxFunction.class,
		SyntaxFunctions.class,
		SyntaxElem.class,
			SyntaxElemRef.class,
			SyntaxToken.class,
			SyntaxPlaceHolder.class,
			SyntaxAttr.class,
				SyntaxSubAttr.class,
				SyntaxList.class,
				SyntaxListWrapper.class,
				SyntaxTreeBranch.class,
				SyntaxIdentAttr.class,
				SyntaxCharAttr.class,
				SyntaxStrAttr.class,
				SyntaxXmlStrAttr.class,
				SyntaxNode.class,
			SyntaxSwitch.class,
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
		SyntaxJavaCommentTemplate.class,
		SyntaxJavaAccessExpr.class,
		SyntaxJavaAccess.class,
		SyntaxJavaPackedField.class,
		SyntaxJavaComment.class,
		SyntaxJavaConstructorName.class,
	};
}

