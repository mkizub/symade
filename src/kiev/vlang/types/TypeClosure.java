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

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(lang=CoreLang)
public class TypeClosureRef extends TypeRef {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public TypeRef∅		args;
	@nodeAttr public TypeRef		ret;

	public TypeClosureRef() {}
	
	public TypeClosureRef(CallType tp) {
		this.type_lnk = tp;
		assert (tp.isReference());
	}

	public Type getType(Env env) {
		if (this.type_lnk != null)
			return this.type_lnk;
		Type[] args = new Type[this.args.length];
		for(int i=0; i < args.length; i++) {
			args[i] = this.args[i].getType(env);
		}
		Type ret = this.ret.getType(env);
		this.type_lnk = new CallType(null,null,args,ret,true);
		return this.type_lnk;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "type_lnk")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public Struct getStruct(Env env) {
		return null;
	}
	public TypeDecl getTypeDecl(Env env) {
		return (TypeDecl)env.tenv.symbolCallTDecl.dnode;
	}
}
