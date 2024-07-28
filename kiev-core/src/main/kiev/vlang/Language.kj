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
 *
 */

public interface Language {
	public String getName();
	public String getURI();
	public Class[] getSuperLanguages();
	public Class[] getNodeClasses();
	public String getDefaultEditorSyntax();
	public String getDefaultInfoSyntax();
	public String getClassByNodeName(String name);
	public AttrSlot getExtAttrByName(String name);
	public ANode makeNode(Env env, String name, String ti_sign, String uuid);
}

@ThisIsANode(lang=CoreLang)
public final class LangDecl extends DNode implements GlobalDNodeContainer {
	@nodeAttr
	public LangDecl⇑∅       super_langs;
	@nodeAttr
	public NodeDecl∅        decls;

	public ASTNode[] getContainerMembers() { decls }

	public String qname() {
		ANode p = parent();
		if (p instanceof GlobalDNode)
			return (((GlobalDNode)p).qname()+"·"+sname).intern();
		else
			return sname;
	}

	public rule resolveNameR(ResInfo path)
	{
		path ?= this
	;
		path @= decls
	}
}

public abstract class LangBase implements Language {
	
	public static final Hashtable<String,Class> allNodesMap = new Hashtable<String,Class>();
	protected String defaultEditorSyntaxName;
	protected String defaultInfoSyntaxName;
	protected String languageName;
	protected String langURI;

	public LangBase() {
		if (getClass().getAnnotation(ThisIsALanguage.class) != null)
			languageName = getClass().getAnnotation(ThisIsALanguage.class).name();
		langURI = "sop://languages/"+getName()+"?class="+getClass().getName().intern();
		foreach (Class c; getNodeClasses()) {
			ThisIsANode um = (ThisIsANode)c.getAnnotation(ThisIsANode.class);
			if (um == null)
				continue;
			String nm = um.name();
			if (nm == null || nm.length() == 0)
				nm = c.getSimpleName();
			allNodesMap.put(nm, c);
		}
	}
	
	public String getName() { return languageName; }

	public final String getURI() { return langURI; }
	
	public String getClassByNodeName(String name) {
		Class c = allNodesMap.get(name);
		if (c == null)
			throw new RuntimeException("Language "+this.getName()+" has no node "+name);
		return c.getName();
	}
	public ANode makeNode(Env env, String name, String ti_sign, String uuid) {
		Class c = allNodesMap.get(name);
		if (c == null)
			throw new RuntimeException("Language "+this.getName()+" has no node "+name);
		Symbol symbol = null;
		if (uuid != null)
			symbol = env.getSymbolByUUID(uuid);
		ANode node;
		if (ti_sign != null)
			node = (ANode)TypeInfo.newTypeInfo(ti_sign).newInstance();
		else
			node = (ANode)c.newInstance();
		if (symbol != null) {
			if (node instanceof Symbol)
				node = symbol;
			else if (node instanceof DNode) {
				node.symbol = symbol;
				env.tenv.callbackTypeVersionChanged(symbol);
			}
		}
		return node;
	}
	public AttrSlot getExtAttrByName(String name) {
		return null;
	}

	public String getDefaultEditorSyntax() {
		return defaultEditorSyntaxName;
	}
	public String getDefaultInfoSyntax() {
		return defaultInfoSyntaxName;
	}
}

@ThisIsALanguage(name="core")
@singleton
public final class CoreLang extends LangBase {
	{
		this.defaultEditorSyntaxName = "stx-fmt·syntax-for-java";
		this.defaultInfoSyntaxName = "stx-fmt·syntax-for-java";
	}
	public String getName() { return "core"; }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }
	
	public ANode makeNode(Env env, String name, String ti_sign, String uuid) {
		if (ti_sign == null && name.equals("SymbolRef"))
			return new SymbolRef();
		if (uuid != null) {
			Symbol symbol = env.getSymbolByUUID(uuid);
			if (symbol != null && symbol.dnode != null) {
				DNode dn = symbol.dnode;
				env.tenv.callbackTypeVersionChanged(symbol);
				if (dn.isTypeDeclNotLoaded()) {
					dn.cleanupOnReload();
					return dn;
				}
			}
		}
		return super.makeNode(env, name, ti_sign, uuid);
	}

	public AttrSlot getExtAttrByName(String name) {
		//if (name.equals("comment"))
		//	return Comment.ATTR_COMMENT;
		return null;
	}

	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
	ASTNode.class,
		Symbol.class,
		Alias.class,
		OperatorAlias.class,
		SymbolRef.class,
		DNode.class,	// declaration nodes
			KievSyntax.class,
			TypeDecl.class,		// type declaration
				TypeOpDef.class,		// declare a type using operator
				TypeDef.class,			// typedef
					TypeAssign.class,	// assign typedef (alias)
					TypeConstr.class,	// constrained typedef (upper/lower bounds)
				ComplexTypeDecl.class,
					MetaTypeDecl.class,		// abstract type declaration
				Struct.class,			// abstract structure
					KievPackage.class,
						KievRoot.class,
					JavaClass.class,
					JavaAnonymouseClass.class,
					KievView.class,
					JavaAnnotation.class,
					PizzaCase.class,
					JavaEnum.class,
					SymadeNode.class,
			LangDecl.class,
			NodeDecl.class,
			NodeAttribute.class,
			Label.class,		// code label
			Opdef.class,		// operator declaration
			OpArgument.class,
				OpArgEXPR.class,
					OpArgEXPR_X.class,
					OpArgEXPR_Y.class,
					OpArgEXPR_Z.class,
					OpArgEXPR_P.class,
				OpArgTYPE.class,
				OpArgIDNT.class,
				OpArgNODE.class,
				OpArgOPER.class,
				OpArgLIST.class,
					OpArgLIST_ANY.class,
					OpArgLIST_ONE.class,
					OpArgLIST_NUM.class,
				OpArgSEQS.class,
				OpArgALTR.class,
				OpArgOPTIONAL.class,
			CoreOperation.class,
			Method.class,
				MethodImpl.class,	// a concrete method
				MethodGetter.class,	// a field getter method
				MethodSetter.class,	// a field setter method
				Constructor.class,	// type constructor
			Initializer.class,
			WBCCondition.class,		// work-by-contract condition
			Var.class,				// variables:
				LVar.class,				// local
				Field.class,			// fields
				RewritePattern.class,	// pattern
		ENode.class,	// expression nodes
			NopExpr.class,
			ArgExpr.class,
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
				ConstRadixExpr.class,
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
				PathTypeRef.class,
				TypeArgRef.class,
				TypeASTNodeRef.class,
				TypeNameRef.class,
				TypeNameArgsRef.class,
				TypeInnerNameRef.class,
			Shadow.class,
			TypeClassExpr.class,
			TypeInfoExpr.class,
			AssertEnabledExpr.class,
			AssignExpr.class,
			ModifyExpr.class,
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
				MetaGetter.class,
				MetaSetter.class,
			MetaFlag.class,		// special meta-data, acting as flags
				MetaAccess.class,		// access rights (public, private and so on)
				MetaPublic.class,
				MetaPrivate.class,
				MetaProtected.class,
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
			SyntaxScope.class,
				NameSpace.class,	// name space unit
				FileUnit.class,	// file/compilation unit, also acts as a namespace
			Import.class,
				ImportImpl.class,
				ImportStatic.class,
				ImportMethod.class,
				ImportOperators.class,
			ImportSyntax.class,
			ATextNode.class,
				TextElem.class,
				TextBrk.class,
				TextLine.class,
				Text.class,
					Comment.class,		// comments
		// in parser
		ASTExpression.class,
		EToken.class
	};
}

@ThisIsALanguage(name="logic")
@singleton
public final class LogicLang extends LangBase {
	{
		this.defaultEditorSyntaxName = "stx-fmt·syntax-for-java";
		this.defaultInfoSyntaxName = "stx-fmt·syntax-for-java";
	}
	public String getName() { return "logic"; }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

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

@ThisIsALanguage(name="macro")
@singleton
public final class MacroLang extends LangBase {
	{
		this.defaultEditorSyntaxName = "stx-fmt·syntax-for-java";
		this.defaultInfoSyntaxName = "stx-fmt·syntax-for-java";
	}
	public String getName() { return "macro"; }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

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
		MacroListIntExpr.class,
		MacroSelfExpr.class,
		MacroAccessExpr.class,
		MacroSubstExpr.class,
		MacroSubstTypeRef.class,
		MacroBinaryBoolExpr.class,
		MacroHasMetaExpr.class
	};
}

