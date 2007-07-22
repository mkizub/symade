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
public final class SyntaxLang implements Language {
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }
	public ATextSyntax getDefaultEditorSyntax() {
		if (defaultEditorSyntax == null)
			defaultEditorSyntax = (ATextSyntax)Env.resolveGlobalDNode("stx-fmt\u001fsyntax-for-syntax");
		return defaultEditorSyntax;
	}
	public ATextSyntax getDefaultInfoSyntax() {
		if (defaultInfoSyntax == null)
			defaultInfoSyntax = (ATextSyntax)Env.resolveGlobalDNode("stx-fmt\u001fsyntax-for-syntax");
		return defaultInfoSyntax;
	}
	private static ATextSyntax defaultEditorSyntax;
	private static ATextSyntax defaultInfoSyntax;
	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
		ATextSyntax.class,
			TextSyntax.class,
			KievTextSyntax.class,
			XmlDumpSyntax.class,
			TreeSyntax.class,
		SpaceInfo.class,
		SpaceCmd.class,
		AParagraphLayout.class,
			ParagraphLayout.class,
			ParagraphLayoutBlock.class,
		DrawColor.class,
		DrawFont.class,
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
				SyntaxIdentAttr.class,
				SyntaxCharAttr.class,
				SyntaxStrAttr.class,
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
		// xml dump
		SyntaxXmlStrAttr.class,
		SyntaxXmlTypeAttr.class
	};
}

