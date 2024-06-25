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

@ThisIsANode(name="PathType", lang=CoreLang)
public class PathTypeRef extends TypeRef {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public ENode		path;

	public PathTypeRef() {}

	public PathTypeRef(ENode path) {
		this.pos = path.pos;
		this.path = path;
	}

	public void callbackChanged(NodeChangeInfo info) {
		if (info.content_change && info.slot.name == "path") {
			if (!this.isExptTypeSignature() && this.type_lnk != null)
				this.type_lnk = null;
			notifyParentThatIHaveChanged();
		}
		super.callbackChanged(info);
	}
	
	public CoreOperation getOperation(Env env) { env.coreFuncs.fPathTypeAccess.operation }

	public ENode[] getEArgs() { return new ENode[]{path}; }

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		if (path instanceof EToken && path.ident == nameThis)
			path.replaceWithNode(new ThisExpr(path.pos), this, nodeattr$path);
		return true;
	}
	public void preResolveOut(Env env, INode parent, AttrSlot slot) { getType(env); }
	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) { return true; }
	public void mainResolveOut(Env env, INode parent, AttrSlot slot) { getType(env); }

	public Type getType(Env env) {
		//if (this.type_lnk != null)
		//	return this.type_lnk;
		Type tp;
		if (path instanceof ThisExpr)
			tp = env.tenv.tpSelfTypeArg;
		else
			tp = path.getType(env);
		//this.type_lnk = tp;
		return tp;
	}

	public Struct getStruct(Env env) {
		return this.getType(env).getStruct();
	}
	public TypeDecl getTypeDecl(Env env) {
		return this.getType(env).meta_type.tdecl;
	}

	public String toString() {
		return String.valueOf(path)+".type";
	}
}

