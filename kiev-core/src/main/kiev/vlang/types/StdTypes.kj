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
package kiev.vlang.types;
import syntax kiev.Syntax;

import static kiev.vlang.AccessFlags.*;

import java.util.UUID;
import java.util.IdentityHashMap;

/**
 * @author Maxim Kizub
 * @version $Revision: 299 $
 *
 */

public class StdTypes {
	
	public static final int flAbstract			= 1 <<  0;
	public static final int flUnerasable		= 1 <<  1;
	public static final int flVirtual			= 1 <<  2;
	public static final int flFinal				= 1 <<  3;
	public static final int flStatic			= 1 <<  4;
	public static final int flForward			= 1 <<  5;
	public static final int flArgAppliable		= 1 <<  6;
	public static final int flValAppliable		= 1 <<  7;

	public final Env env;
	final IdentityHashMap<Symbol,MetaType> allMetaTypes;

	public final CoreType tpAny;
	public final CoreType tpVoid;
	public final CoreType tpBoolean;
	public final CoreType tpChar;
	public final CoreType tpByte;
	public final CoreType tpShort;
	public final CoreType tpInt;
	public final CoreType tpLong;
	public final CoreType tpFloat;
	public final CoreType tpDouble;
	public final CoreType tpNull;
	public final CompaundType tpRule;
	public final CompaundType tpBooleanRef;
	public final CompaundType tpByteRef;
	public final CompaundType tpCharRef;
	public final CompaundType tpNumberRef;
	public final CompaundType tpShortRef;
	public final CompaundType tpIntRef;
	public final CompaundType tpLongRef;
	public final CompaundType tpFloatRef;
	public final CompaundType tpDoubleRef;
	public final CompaundType tpVoidRef;
	public final CompaundType tpObject;
	public final CompaundType tpClass;
	public final CompaundType tpBigIntRef;
	public final CompaundType tpBigDecRef;
	public final CompaundType tpDebug;
	public final CompaundType tpTypeInfo;
	public final CompaundType tpTypeInfoInterface;
	public final CompaundType tpCloneable;
	public final CompaundType tpString;
	public final CompaundType tpThrowable;
	public final CompaundType tpError;
	public final CompaundType tpException;
	public final CompaundType tpCastException;
	public final CompaundType tpJavaEnumeration;
	public final CompaundType tpJavaIterator;
	public final CompaundType tpJavaIterable;
	public final CompaundType tpArrayEnumerator;
	public final CompaundType tpRuntimeException;
	public final CompaundType tpAssertException;
	public final CompaundType tpEnum;
	public final CompaundType tpAnnotation;
	public final CompaundType tpClosure;

	public final CompaundType tpPrologVar;
	public final CompaundType tpRefProxy;

	public final CompaundType tpTypeSwitchHash;

	public final ArgType      tpSelfTypeArg;
	public final ArgType      tpTypeOpDefArg;

	public final TemplateTVarSet		arrayTemplBindings;
	public final ArrayMetaType			arrayMetaType;
	public final Symbol						symbolArrayTDecl;
	public final ArrayType					tpArrayOfAny;
	public final ArgType						tpArrayArg;
	
	public final TemplateTVarSet		varargTemplBindings;
	public final VarargMetaType		varargMetaType;
	public final Symbol						symbolVarargTDecl;
	public final VarargType				tpVarargOfAny;
	public final ArgType						tpVarargArg;

	public final TemplateTVarSet			wildcardCoTemplBindings;
	public final WildcardCoMetaType	wildcardCoMetaType;
	public final Symbol							symbolWildcardCoTDecl;
	public final WildcardCoType			wildcardCoOfAny;
	public final ArgType							tpWildcardCoArg;
	
	public final TemplateTVarSet			wildcardContraTemplBindings;
	public final WildcardContraMetaType		wildcardContraMetaType;
	public final Symbol							symbolWildcardContraTDecl;
	public final WildcardContraType		tpWildcardContraOfAny;
	public final ArgType							tpWildcardContraArg;
	
	public final TemplateTVarSet			wrapperTemplBindings;
	public final Symbol							symbolWrapperTDecl;
	public final ArgType							tpWrapperArg;
	
	public final Symbol							symbolTupleTDecl;
	public final TupleMetaType[]			tupleMetaTypes;

	public final TemplateTVarSet			callTemplBindingsStatic;
	public final TemplateTVarSet			callTemplBindingsThis;
	public final Symbol							symbolCallTDecl;
	public final CallMetaType					call_static_instance;
	public final CallMetaType					call_this_instance;
	public final CallMetaType					closure_static_instance;
	public final CallMetaType					closure_this_instance;
	public final ArgType      tpCallRetArg;
	public final ArgType      tpCallTupleArg;
	public final ArgType[]    tpCallParamArgs;
	public final ArgType[]    tpUnattachedArgs;

	public final Symbol symbolTDeclASTNodeType;

	public StdTypes(Env env) {
		this.env = env;
		env.tenv = this;
		
		allMetaTypes = new IdentityHashMap<Symbol,MetaType>();

		tpAny		= new CoreType(this, Constants.nameAny,     null,  0);												tpAny.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpVoid		= new CoreType(this, Constants.nameVoid,    null,  0);												tpVoid.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpBoolean	= new CoreType(this, Constants.nameBoolean, tpAny, MetaType.flBoolean | MetaType.flIntegerInCode);	tpBoolean.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpChar		= new CoreType(this, Constants.nameChar,    tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpChar.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpByte		= new CoreType(this, Constants.nameByte,    tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpByte.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpShort		= new CoreType(this, Constants.nameShort,   tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpShort.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpInt		= new CoreType(this, Constants.nameInt,     tpAny, MetaType.flInteger | MetaType.flIntegerInCode);	tpInt.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpLong		= new CoreType(this, Constants.nameLong,    tpAny, MetaType.flInteger | MetaType.flDoubleSize);		tpLong.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpFloat		= new CoreType(this, Constants.nameFloat,   tpAny, MetaType.flFloat);								tpFloat.meta_type.tdecl.setTypeDeclNotLoaded(true);
		tpDouble	= new CoreType(this, Constants.nameDouble,  tpAny, MetaType.flFloat   | MetaType.flDoubleSize);		tpDouble.meta_type.tdecl.setTypeDeclNotLoaded(true);

		KievPackage java_lang            = env.newPackage("java·lang");
		KievPackage java_lang_annotation = env.newPackage("java·lang·annotation");
		KievPackage java_math            = env.newPackage("java·math");
		KievPackage java_util            = env.newPackage("java·util");
		KievPackage kiev_stdlib          = env.newPackage("kiev·stdlib");
		KievPackage kiev_stdlib_meta     = env.newPackage("kiev·stdlib·meta");

		Struct tpObjectClazz = env.newStruct("Object",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpObjectClazz.setTypeDeclNotLoaded(true);
		tpObject				= (CompaundType)tpObjectClazz.getType(env);

		Struct tpClassClazz = env.newStruct("Class",java_lang,ACC_PUBLIC|ACC_FINAL,new JavaClass(),null);
		tpClassClazz.setTypeDeclNotLoaded(true);
		tpClass					= (CompaundType)tpClassClazz.getType(env);

		tpNull		= new CoreType(this, Constants.nameNull,    tpObject, MetaType.flReference);
		tpNull.meta_type.tdecl.setTypeDeclNotLoaded(true);

		Struct tpRuleClazz = env.newStruct("rule",kiev_stdlib,ACC_PUBLIC|ACC_ABSTRACT,new JavaClass(),null);
		tpRuleClazz.setTypeDeclNotLoaded(true);
		tpRule					= (CompaundType)tpRuleClazz.getType(env);

		Struct tpDebugClazz = env.newStruct("Debug",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tpDebugClazz.setTypeDeclNotLoaded(true);
		tpDebug				= (CompaundType)tpDebugClazz.getType(env);

		Struct tpTypeInfoClazz = env.newStruct("TypeInfo",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tpTypeInfoClazz.setTypeDeclNotLoaded(true);
		tpTypeInfo				= (CompaundType)tpTypeInfoClazz.getType(env);

		Struct tpTypeInfoInterfaceClazz = env.newStruct("TypeInfoInterface",kiev_stdlib,ACC_PUBLIC|ACC_INTERFACE,new JavaClass(),null);
		tpTypeInfoInterfaceClazz.setTypeDeclNotLoaded(true);
		tpTypeInfoInterface				= (CompaundType)tpTypeInfoInterfaceClazz.getType(env);

		Struct tpCloneableClazz = env.newStruct("Cloneable",java_lang,ACC_PUBLIC|ACC_INTERFACE,new JavaClass(),null);
		tpCloneableClazz.setTypeDeclNotLoaded(true);
		tpCloneable				= (CompaundType)tpCloneableClazz.getType(env);

		{
			TypeConstr tdWildcardCoArg = new TypeConstr(new Symbol("_base_"), tpAny);
			tdWildcardCoArg.setAbstract(true);
			tpWildcardCoArg = tdWildcardCoArg.getAType(env);
			tpWildcardCoArg.flags |= flArgAppliable | flValAppliable;
			wildcardCoTemplBindings = new TemplateTVarSet(-1, new TVarBld(tpWildcardCoArg, null));
			MetaTypeDecl tdWildcardCo = (MetaTypeDecl)env.resolveGlobalDNode("kiev·stdlib·_wildcard_co_variant_");
			assert (tdWildcardCo == null);
			symbolWildcardCoTDecl = env.makeGlobalSymbol("kiev·stdlib·_wildcard_co_variant_");
			symbolWildcardCoTDecl.setUUID(env,"6c99b10d-3003-3176-8086-71be6cee5c51");
			tdWildcardCo = new MetaTypeDecl(symbolWildcardCoTDecl);
			tdWildcardCo.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdWildcardCo.super_types.insert(0, new TypeRef(tpAny));
			tdWildcardCo.args.add(tdWildcardCoArg);
			tdWildcardCo.setTypeDeclNotLoaded(true);
			wildcardCoMetaType = new WildcardCoMetaType(this,tdWildcardCo);
			wildcardCoOfAny = new WildcardCoType(tpAny);
			kiev_stdlib.pkg_members.add(tdWildcardCo);
		}

		{
			TypeConstr tdWildcardContraArg = new TypeConstr(new Symbol("_base_"), tpAny);
			tdWildcardContraArg.setAbstract(true);
			tpWildcardContraArg = tdWildcardContraArg.getAType(env);
			tpWildcardContraArg.flags |= flArgAppliable | flValAppliable;
			wildcardContraTemplBindings = new TemplateTVarSet(-1, new TVarBld(tpWildcardContraArg, null));
			MetaTypeDecl tdWildcardContra = (MetaTypeDecl)env.resolveGlobalDNode("kiev·stdlib·_wildcard_contra_variant_");
			assert (tdWildcardContra == null);
			symbolWildcardContraTDecl = env.makeGlobalSymbol("kiev·stdlib·_wildcard_contra_variant_");
			symbolWildcardContraTDecl.setUUID(env,"933ac6b8-4d03-3799-9bb3-3c9bc1883707");
			tdWildcardContra = new MetaTypeDecl(symbolWildcardContraTDecl);
			tdWildcardContra.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdWildcardContra.super_types.insert(0, new TypeRef(tpAny));
			tdWildcardContra.args.add(tdWildcardContraArg);
			tdWildcardContra.setTypeDeclNotLoaded(true);
			wildcardContraMetaType = new WildcardContraMetaType(this,tdWildcardContra);
			tpWildcardContraOfAny = new WildcardContraType(tpAny);
			kiev_stdlib.pkg_members.add(tdWildcardContra);
		}
		
		{
			TypeConstr tdWrapperArg = new TypeConstr(new Symbol("_boxed_"), tpObject);
			tdWrapperArg.setAbstract(true);
			tpWrapperArg = tdWrapperArg.getAType(env);
			tpWrapperArg.flags |= flArgAppliable | flValAppliable;
			wrapperTemplBindings = new TemplateTVarSet(-1, new TVarBld(tpWrapperArg, null));
			MetaTypeDecl tdWrapper = (MetaTypeDecl)env.resolveGlobalDNode("kiev·stdlib·_wrapper_");
			assert (tdWrapper == null);
			symbolWrapperTDecl = env.makeGlobalSymbol("kiev·stdlib·_wrapper_");
			symbolWrapperTDecl.setUUID(env,"67544053-836d-3bac-b94d-0c4b14ae9c55");
			tdWrapper = new MetaTypeDecl(symbolWrapperTDecl);
			tdWrapper.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdWrapper.super_types.insert(0, new TypeRef(tpObject));
			tdWrapper.args.add(tdWrapperArg);
			tdWrapper.setTypeDeclNotLoaded(true);
			kiev_stdlib.pkg_members.add(tdWrapper);
			new WrapperMetaType(this);
		}
		
		{
			TypeConstr tdArrayArg = new TypeConstr(new Symbol("_elem_"), tpAny);
			tdArrayArg.setAbstract(true);
			tdArrayArg.variance = TypeVariance.CO_VARIANT;
			tpArrayArg = tdArrayArg.getAType(env);
			tpArrayArg.flags |= flArgAppliable | flValAppliable;
			arrayTemplBindings = new TemplateTVarSet(-1, new TVarBld(tpArrayArg, null));
			symbolArrayTDecl = env.makeGlobalSymbol("kiev·stdlib·_array_");
			symbolArrayTDecl.setUUID(env,"bbf03b4b-62d4-3e29-8f0d-acd6c47b9a04");
			MetaTypeDecl tdArray = (MetaTypeDecl)env.resolveGlobalDNode("kiev·stdlib·_array_");
			assert  (tdArray == null);
			tdArray = new MetaTypeDecl(symbolArrayTDecl);
			tdArray.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdArray.super_types.insert(0, new TypeRef(tpObject));
			tdArray.args.add((TypeConstr)tpArrayArg.definer);
			tdArray.setTypeDeclNotLoaded(true);
			arrayMetaType = new ArrayMetaType(this,tdArray);
			tpArrayOfAny = ArrayType.newArrayType(tpAny);
			kiev_stdlib.pkg_members.add(tdArray);
			Field length = new Field("length", tpInt, ACC_PUBLIC|ACC_FINAL|ACC_MACRO|ACC_NATIVE);
			length.setMeta(new MetaAccess("public",0xAA)); //public:ro
			tdArray.members.add(length);
			Method get = new MethodImpl("get", tpArrayArg, ACC_PUBLIC|ACC_MACRO|ACC_NATIVE);
			get.params.add(new LVar(0,"idx",tpInt,Var.VAR_LOCAL,0));
			get.aliases += new OperatorAlias(Constants.nameArrayGetOp, get);
			//CoreExpr ce = new CoreExpr();
			//get.body = ce;
			//ce.ident = "kiev.stdlib._array_:get";
			tdArray.members.add(get);
		}

		{
			TypeConstr tdVarargArg = new TypeConstr(new Symbol("_elem_"), tpObject);
			tdVarargArg.symbol.setUUID(env, "924f219a-37cf-3654-b761-7cb5e26ceef0");
			tdVarargArg.setTypeDeclNotLoaded(true);
			tdVarargArg.setAbstract(true);
			tdVarargArg.variance = TypeVariance.CO_VARIANT;
			tpVarargArg = tdVarargArg.getAType(env);
			tpVarargArg.flags |= flArgAppliable | flValAppliable;
			{
				TVarBld set =  new TVarBld(tpVarargArg, null);
				set.append(tpArrayArg, tpVarargArg);
				varargTemplBindings = new TemplateTVarSet(-1, set);
			}
			MetaTypeDecl tdVararg = (MetaTypeDecl)env.resolveGlobalDNode("kiev·stdlib·_vararg_");
			assert  (tdVararg == null);
			symbolVarargTDecl = env.makeGlobalSymbol("kiev·stdlib·_vararg_");
			symbolVarargTDecl.setUUID(env,"8aa32751-ac53-343e-b456-6f8521b01647");
			tdVararg = new MetaTypeDecl(symbolVarargTDecl);
			tdVararg.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdVararg.super_types.insert(0, new TypeRef(ArrayType.newArrayType(tpVarargArg)));
			tdVararg.args.add(tdVarargArg);
			tdVararg.setTypeDeclNotLoaded(true);
			varargMetaType = new VarargMetaType(this,tdVararg);
			tpVarargOfAny = VarargType.newVarargType(tpAny);
			kiev_stdlib.pkg_members.add(tdVararg);
			this.callbackTypeVersionChanged(tdVararg);
		}

		ASTNodeMetaType.init();
		symbolTDeclASTNodeType = env.makeGlobalSymbol("kiev·stdlib·_astnode_");
		MetaTypeDecl tdASTNodeType = new MetaTypeDecl(symbolTDeclASTNodeType);
		tdASTNodeType.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdASTNodeType.symbol.setUUID(env,"3e32f9c7-9846-393e-8c6e-11512191ec94");
		tdASTNodeType.setTypeDeclNotLoaded(true);
		kiev_stdlib.pkg_members.add(tdASTNodeType);
		TypeConstr tdASTNodeTypeArg = new TypeConstr(new Symbol("_node_"), tpObject);
		tdASTNodeTypeArg.setAbstract(true);
		tdASTNodeType.args += tdASTNodeTypeArg;
		ArgType tpASTNodeTypeArg;
		tpASTNodeTypeArg = tdASTNodeTypeArg.getAType(env);
		tpASTNodeTypeArg.flags |= flArgAppliable | flValAppliable;
		tdASTNodeType.super_types += new TypeRef(tpAny);

		Struct tpBooleanRefClazz = env.newStruct("Boolean",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpBooleanRefClazz.setTypeDeclNotLoaded(true);
		tpBooleanRef			= (CompaundType)tpBooleanRefClazz.getType(env);

		Struct tpCharRefClazz = env.newStruct("Character",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpCharRefClazz.setTypeDeclNotLoaded(true);
		tpCharRef			= (CompaundType)tpCharRefClazz.getType(env);

		Struct tpNumberRefClazz = env.newStruct("Number",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpNumberRefClazz.setTypeDeclNotLoaded(true);
		tpNumberRef			= (CompaundType)tpNumberRefClazz.getType(env);

		Struct tpByteRefClazz = env.newStruct("Byte",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpByteRefClazz.setTypeDeclNotLoaded(true);
		tpByteRef			= (CompaundType)tpByteRefClazz.getType(env);

		Struct tpShortRefClazz = env.newStruct("Short",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpShortRefClazz.setTypeDeclNotLoaded(true);
		tpShortRef			= (CompaundType)tpShortRefClazz.getType(env);

		Struct tpIntRefClazz = env.newStruct("Integer",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpIntRefClazz.setTypeDeclNotLoaded(true);
		tpIntRef			= (CompaundType)tpIntRefClazz.getType(env);

		Struct tpLongRefClazz = env.newStruct("Long",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpLongRefClazz.setTypeDeclNotLoaded(true);
		tpLongRef			= (CompaundType)tpLongRefClazz.getType(env);

		Struct tpFloatRefClazz = env.newStruct("Float",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpFloatRefClazz.setTypeDeclNotLoaded(true);
		tpFloatRef			= (CompaundType)tpFloatRefClazz.getType(env);

		Struct tpDoubleRefClazz = env.newStruct("Double",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpDoubleRefClazz.setTypeDeclNotLoaded(true);
		tpDoubleRef			= (CompaundType)tpDoubleRefClazz.getType(env);

		Struct tpBigIntClazz = env.newStruct("BigInteger",java_math,ACC_PUBLIC,new JavaClass(),null);
		tpBigIntClazz.setTypeDeclNotLoaded(true);
		tpBigIntRef			= (CompaundType)tpBigIntClazz.getType(env);

		Struct tpBigDecClazz = env.newStruct("BigDecimal",java_math,ACC_PUBLIC,new JavaClass(),null);
		tpBigDecClazz.setTypeDeclNotLoaded(true);
		tpBigDecRef			= (CompaundType)tpBigDecClazz.getType(env);

		Struct tpVoidRefClazz = env.newStruct("Void",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpVoidRefClazz.setTypeDeclNotLoaded(true);
		tpVoidRef			= (CompaundType)tpVoidRefClazz.getType(env);

		Struct tpStringClazz = env.newStruct("String",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpStringClazz.setTypeDeclNotLoaded(true);
		tpString				= (CompaundType)tpStringClazz.getType(env);

		Struct tpAnnotationClazz = env.newStruct("Annotation",java_lang_annotation,ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT,new JavaClass(),null);
		tpAnnotationClazz.setTypeDeclNotLoaded(true);
		tpAnnotation			= (CompaundType)tpAnnotationClazz.getType(env);
		
		Struct tpThrowableClazz = env.newStruct("Throwable",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpThrowableClazz.setTypeDeclNotLoaded(true);
		tpThrowable				= (CompaundType)tpThrowableClazz.getType(env);

		Struct tpErrorClazz = env.newStruct("Error",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpErrorClazz.setTypeDeclNotLoaded(true);
		tpError				= (CompaundType)tpErrorClazz.getType(env);

		Struct tpExceptionClazz = env.newStruct("Exception",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpExceptionClazz.setTypeDeclNotLoaded(true);
		tpException				= (CompaundType)tpExceptionClazz.getType(env);

		Struct tpCastExceptionClazz = env.newStruct("ClassCastException",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpCastExceptionClazz.setTypeDeclNotLoaded(true);
		tpCastException				= (CompaundType)tpCastExceptionClazz.getType(env);

		Struct tpRuntimeExceptionClazz = env.newStruct("RuntimeException",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpRuntimeExceptionClazz.setTypeDeclNotLoaded(true);
		tpRuntimeException				= (CompaundType)tpRuntimeExceptionClazz.getType(env);

		Struct tpAssertExceptionClazz = env.newStruct("AssertionFailedException",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tpAssertExceptionClazz.setTypeDeclNotLoaded(true);
		tpAssertException				= (CompaundType)tpAssertExceptionClazz.getType(env);

		Struct tpEnumClazz = env.newStruct("Enum",java_lang,ACC_PUBLIC | ACC_ABSTRACT,new JavaClass(),null);
		tpEnumClazz.setTypeDeclNotLoaded(true);
		tpEnum					= (CompaundType)tpEnumClazz.getType(env);

		Struct tdClosure = env.newStruct("closure",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tdClosure.setTypeDeclNotLoaded(true);
		tpClosure				= (CompaundType)tdClosure.getType(env);

		Struct tpTypeSwitchHashClazz = env.newStruct("TypeSwitchHash",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tpTypeSwitchHashClazz.setTypeDeclNotLoaded(true);
		tpTypeSwitchHash			= (CompaundType)tpTypeSwitchHashClazz.getType(env);


		Struct tpJavaEnumerationClazz = env.newStruct("Enumeration",java_util,ACC_PUBLIC,new JavaClass(),null);
		tpJavaEnumerationClazz.setTypeDeclNotLoaded(true);
		tpJavaEnumeration	= (CompaundType)tpJavaEnumerationClazz.getType(env);

		Struct tpJavaIteratorClazz = env.newStruct("Iterator",java_util,ACC_PUBLIC,new JavaClass(),null);
		tpJavaIteratorClazz.setTypeDeclNotLoaded(true);
		tpJavaIterator	= (CompaundType)tpJavaIteratorClazz.getType(env);

		Struct tpJavaIterableClazz = env.newStruct("Iterable",java_lang,ACC_PUBLIC,new JavaClass(),null);
		tpJavaIterableClazz.setTypeDeclNotLoaded(true);
		tpJavaIterable	= (CompaundType)tpJavaIterableClazz.getType(env);

		
		Struct tpArrayEnumeratorClazz = env.newStruct("ArrayEnumerator",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tpArrayEnumeratorClazz.setTypeDeclNotLoaded(true);
		tpArrayEnumeratorClazz.args.add(new TypeConstr(new Symbol("A")));
		tpArrayEnumerator	= (CompaundType)tpArrayEnumeratorClazz.getType(env);
		

		Struct tpPrologVarClazz = env.newStruct("PVar",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tpPrologVarClazz.setTypeDeclNotLoaded(true);
		tpPrologVarClazz.args.add(new TypeConstr(new Symbol("A")));
		tpPrologVar	= (CompaundType)tpPrologVarClazz.getType(env);

		Struct tpRefProxyClazz = env.newStruct("Ref",kiev_stdlib,ACC_PUBLIC,new JavaClass(),null);
		tpRefProxyClazz.setTypeDeclNotLoaded(true);
		tpRefProxyClazz.args.add(new TypeConstr(new Symbol("A")));
		tpRefProxy	= (CompaundType)tpRefProxyClazz.getType(env);

		TypeDef tdCallRetArg = new TypeConstr(new Symbol("_ret_"), tpAny);
		tdCallRetArg.setAbstract(true);
		tpCallRetArg = tdCallRetArg.getAType(env);
		tpCallRetArg.flags |= flArgAppliable;
		
		TypeDef tdCallTupleArg = new TypeConstr(new Symbol("_tuple_"), tpAny);
		tdCallTupleArg.setAbstract(true);
		tpCallTupleArg = tdCallTupleArg.getAType(env);
		tpCallTupleArg.flags |= flArgAppliable;
		
		TypeDef tdSelfTypeArg = new TypeConstr(new Symbol("_this_"), tpAny);
		tdSelfTypeArg.setAbstract(true);
		tpSelfTypeArg = tdSelfTypeArg.getAType(env);
		tpSelfTypeArg.flags |= flArgAppliable;
		
		TypeDef tdTypeOpDefArg = new TypeConstr(new Symbol("_oparg_"), tpAny);
		tdTypeOpDefArg.setAbstract(true);
		tpTypeOpDefArg = tdTypeOpDefArg.getAType(env);
		tpTypeOpDefArg.flags |= flArgAppliable;
		
		tpCallParamArgs = new ArgType[128];
		for (int i=0; i < tpCallParamArgs.length; i++) {
			Symbol sym = new Symbol("_"+Integer.toHexString(i)+"_");
			TypeDef tdCallParamArg = new TypeConstr(sym, tpAny);
			tdCallParamArg.setAbstract(true);
			tpCallParamArgs[i] = tdCallParamArg.getAType(env);
			tpCallParamArgs[i].flags |= flArgAppliable;
			sym.setUUID(env, UUID.nameUUIDFromBytes(("kiev.stdlib._call_type_."+sym.sname).getBytes()).toString());
		}
		
		tpUnattachedArgs = new ArgType[128] ;
		for (int i=0; i < tpUnattachedArgs.length; i++) {
			Symbol sym = new Symbol("_"+Integer.toHexString(i)+"_");
			TypeDef tdUnattachedArg = new TypeConstr(sym, tpAny);
			tpUnattachedArgs[i] = tdUnattachedArg.getAType(env);
			tpUnattachedArgs[i].flags |= flArgAppliable;
			sym.setUUID(env, UUID.nameUUIDFromBytes(("kiev.stdlib.any."+sym.sname).getBytes()).toString());
		}
		
		{
			MetaTypeDecl tdTuple = (MetaTypeDecl)env.resolveGlobalDNode("kiev·stdlib·_tuple_");
			assert (tdTuple == null);
			symbolTupleTDecl = env.makeGlobalSymbol("kiev·stdlib·_tuple_");
			tdTuple = new MetaTypeDecl(symbolTupleTDecl);
			tdTuple.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdTuple.super_types.add(new TypeRef(tpAny));
			kiev_stdlib.pkg_members.add(tdTuple);
			
			tupleMetaTypes = new TupleMetaType[32];
			for (int i=0; i < tupleMetaTypes.length; i++) {
				Symbol sym = symbolTupleTDecl.makeGlobalSubSymbol("("+i+")");
				tupleMetaTypes[i] = new TupleMetaType(this,sym,i);
			}
		}
	
		{
			TVarBld set = new TVarBld();
			set.append(tpCallRetArg, null);
			set.append(tpCallTupleArg, null);
			callTemplBindingsStatic = new TemplateTVarSet(-1, set);
	
			set = new TVarBld();
			set.append(tpCallRetArg, null);
			set.append(tpCallTupleArg, null);
			set.append(tpSelfTypeArg, null);
			callTemplBindingsThis = new TemplateTVarSet(-1, set);
	
			MetaTypeDecl tdCall = (MetaTypeDecl)env.resolveGlobalDNode("kiev·stdlib·_call_type_");
			assert (tdCall == null);
			symbolCallTDecl = env.makeGlobalSymbol("kiev·stdlib·_call_type_");
			symbolCallTDecl.setUUID(env,"25395a72-2b16-317a-85b2-5490309bdffc");
			tdCall = new MetaTypeDecl(symbolCallTDecl);
			tdCall.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdCall.setTypeDeclNotLoaded(true);
			call_static_instance    = new CallMetaType(this, callTemplBindingsStatic, MetaType.flCallable);
			call_this_instance      = new CallMetaType(this, callTemplBindingsThis,   MetaType.flCallable);
			closure_static_instance = new CallMetaType(this, callTemplBindingsStatic, MetaType.flCallable | MetaType.flReference);
			closure_this_instance   = new CallMetaType(this, callTemplBindingsThis,   MetaType.flCallable | MetaType.flReference);
			kiev_stdlib.pkg_members.add(tdCall);
		}
		
		tpAny.meta_type.tdecl.symbol.setUUID(		env, "be8bba7f-b4f9-3991-8834-6552dcb237a0");
		tpVoid.meta_type.tdecl.symbol.setUUID(		env, "ec98468f-75f6-3811-ab77-6b0a8458b3ad");
		tpBoolean.meta_type.tdecl.symbol.setUUID(	env, "9c517365-318e-307c-acdf-6682cf309b3f");
		tpChar.meta_type.tdecl.symbol.setUUID(		env, "7713311e-809c-30f7-964a-3d28beb7aab3");
		tpByte.meta_type.tdecl.symbol.setUUID(		env, "89ed44f6-f9a6-3ef7-b396-d2248d5f69db");
		tpShort.meta_type.tdecl.symbol.setUUID(		env, "f9bb2439-c397-3930-b36c-5b1565ec7841");
		tpInt.meta_type.tdecl.symbol.setUUID(		env, "d50f9a1a-2e09-3313-8a64-6b58b300579e");
		tpLong.meta_type.tdecl.symbol.setUUID(		env, "2d6eef81-2c5e-36e4-ab9d-136dfec1dc6b");
		tpFloat.meta_type.tdecl.symbol.setUUID(		env, "a02d23b3-8055-3c87-b331-2b242964a7f1");
		tpDouble.meta_type.tdecl.symbol.setUUID(	env, "d741575d-769c-3108-810e-6c0e57a4b03e");
		tpNull.meta_type.tdecl.symbol.setUUID(		env, "6c8cef01-5c38-36c3-aab0-bd16c23e817d");

		tpCallRetArg.definer.symbol.setUUID(		env, "c71b6316-2bd5-3ade-97ea-f4098908ffc3");
		tpCallTupleArg.definer.symbol.setUUID(			env, "ec0237da-7ea1-3fb4-b4ae-88c8e294ec20");
		tpSelfTypeArg.definer.symbol.setUUID(				env, "e496f784-d631-3ff4-a50b-e60b4de15126");
		tpTypeOpDefArg.definer.symbol.setUUID(				env, "f2c48241-b352-41af-9bea-f1cab98f9888");
		tpArrayArg.definer.symbol.setUUID(					env, "74843bf1-3c28-374b-ad11-006af8a31a71"); tpArrayArg.definer.setTypeDeclNotLoaded(true);
		tpWildcardCoArg.definer.symbol.setUUID(				env, "311f0fb3-a9d6-33b9-8525-170de22d0f73"); tpWildcardCoArg.definer.setTypeDeclNotLoaded(true);
		tpWildcardContraArg.definer.symbol.setUUID(			env, "034fcce5-a61c-38df-85ea-8cd0d238fab7"); tpWildcardContraArg.definer.setTypeDeclNotLoaded(true);
		tpWrapperArg.definer.symbol.setUUID(				env, "400f213e-a4bb-3ee2-b870-9ec1951fd955"); tpWrapperArg.definer.setTypeDeclNotLoaded(true);
		tpASTNodeTypeArg.definer.symbol.setUUID(			env, "f23d4ec5-7fc2-3bbb-9b8f-46a309fc5f24"); tdASTNodeTypeArg.setTypeDeclNotLoaded(true);
	}
	
	public MetaType getExistingMetaType(Symbol sym) {
		return allMetaTypes.get(sym);
	}
	
	public void callbackTypeVersionChanged(TypeDecl td) {
		if (td != null)
			callbackTypeVersionChanged(td.symbol);
	}
	
	public void callbackTypeVersionChanged(Symbol sym) {
		MetaType mt = allMetaTypes.get(sym);
		if (mt != null)
			mt.callbackTypeVersionChanged();
	}
	
	
}

