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

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "path") {
			if (this.type_lnk != null)
				this.type_lnk = null;
			if (this.isAttached())
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
		}
		super.callbackChildChanged(ct, attr, data);
	}
	
	public Operator getOp() { return Operator.PathTypeAccess; }

	public ENode[] getArgs() { return new ENode[]{path}; }

	public boolean preResolveIn() {
		if (path instanceof EToken && path.ident == nameThis)
			path.replaceWithNode(new ThisExpr(path.pos));
		return true;
	}
	public void preResolveOut() { getType(); }
	public boolean mainResolveIn() { return true; }
	public void mainResolveOut() { getType(); }

	public Type getType() {
		if (this.type_lnk != null)
			return this.type_lnk;
		Type tp;
		if (path instanceof ThisExpr)
			tp = StdTypes.tpSelfTypeArg;
		else
			tp = path.getType();
		this.type_lnk = tp;
		return tp;
	}

	public Struct getStruct() {
		return this.getType().getStruct();
	}
	public TypeDecl getTypeDecl() {
		return this.getType().meta_type.tdecl;
	}

	public String toString() {
		return String.valueOf(path)+".type";
	}
}

