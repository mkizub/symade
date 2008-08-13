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

@ThisIsANode(lang=CoreLang)
public abstract class MNode extends ASTNode {
	public static final MNode[] emptyArray = new MNode[0];

	@virtual @abstract
	public:ro String			qname;

	@getter
	public abstract String get$qname();
	public abstract JavaAnnotation getAnnotationDecl();
	public void resolve(Type reqType) {}
	public void verify() {}
	public boolean isRuntimeVisible() { return false; }
	public boolean isRuntimeInvisible() { return false; }

}

@ThisIsANode(name="UserMeta", lang=CoreLang)
public class UserMeta extends MNode {
	@abstract
	@nodeAttr public String							qname;
	@nodeAttr public SymbolRef<JavaAnnotation>		decl;
	@nodeAttr public MetaValue∅						values;

	public boolean equals(Object o) {
		if!(o instanceof UserMeta)
			return false;
		UserMeta meta = (UserMeta)o;
		if (qname != o.qname)
			return false;
		foreach (Method m; getAnnotationDecl().members) {
			MetaValue v1 = this.get(m.sname);
			MetaValue v2 = meta.get(m.sname);
			if (v1 == null && v2 == null)
				continue;
			if (v1 == null || v2 == null)
				return false;
			if (!v1.equals(v2))
				return false;
		}
		return true;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api") {
			if (attr.name == "decl")
				return false;
			if (attr.name == "qname")
				return true;
		}
		return super.includeInDump(dump, attr, val);
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (isAttached()) {
			if      (attr.name == "decl")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
			else if (attr.name == "values")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public UserMeta() {
		this.decl = new SymbolRef<JavaAnnotation>("");
	}

	public UserMeta(JavaAnnotation decl) {
		this.decl = new SymbolRef<JavaAnnotation>(decl);
	}
	
	public UserMeta(String name) {
		this.decl = new SymbolRef<JavaAnnotation>(name);
	}
	
	@getter
	public String get$qname() {
		TypeDecl s = decl.dnode;
		if (s != null)
			return s.qname();
		return decl.name;
	}

	@setter
	public void set$qname(String value) {
		this.decl.name = value;
	}

	public final JavaAnnotation getAnnotationDecl() {
		JavaAnnotation td = decl.dnode;
		if (td != null)
			return td;
		String name = decl.name;
		if (name.indexOf('\u001f') < 0) {
			ResInfo<JavaAnnotation> info = new ResInfo<JavaAnnotation>(this,name,ResInfo.noForwards);
			if (!PassInfo.resolveNameR(this,info))
				Kiev.reportError(this,"Unresolved annotation name "+name);
			JavaAnnotation ann = info.resolvedDNode();
			this.decl.symbol = info.resolvedSymbol();
			ann.checkResolved();
			return ann;
		}
		DNode scope = Env.getRoot();
		int dot;
		do {
			if !(scope instanceof ScopeOfNames)
				Kiev.reportError(this,"Unresolved identifier "+name+" in "+scope);
			dot = name.indexOf('\u001f');
			String head;
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1).intern();
			} else {
				head = name;
			}
			ResInfo info = new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
			if!(((ScopeOfNames)scope).resolveNameR(info)) {
				Kiev.reportError(this,"Unresolved identifier "+head+" in "+scope);
				return null;
			}
			scope = info.resolvedDNode();
		} while (dot > 0);
		if !(scope instanceof JavaAnnotation) {
			Kiev.reportError(this,"Unresolved annotation "+decl.name);
			return null;
		}
		this.decl.symbol = scope.symbol;
		scope.checkResolved();
		return (JavaAnnotation)scope;
	}
	
	public boolean isRuntimeVisible() {
		JavaAnnotation tdecl = getAnnotationDecl();
		UserMeta retens = (UserMeta)tdecl.getMeta("java\u001flang\u001fannotation\u001fRetention");
		if (retens == null)
			return false;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.sname == "RUNTIME")
				return true;
		}
		return false;
	}

	public boolean isRuntimeInvisible() {
		JavaAnnotation tdecl = getAnnotationDecl();
		UserMeta retens = (UserMeta)tdecl.getMeta("java\u001flang\u001fannotation\u001fRetention");
		if (retens == null)
			return true;
		MetaValue val = retens.get("value");
		if (val instanceof MetaValueScalar && val.value instanceof SFldExpr) {
			Field f = ((SFldExpr)val.value).var;
			if (f.sname == "CLASS")
				return true;
		}
		return false;
	}

	public Type getType() {
		JavaAnnotation td = getAnnotationDecl();
		if (td == null)
			return Type.tpVoid;
		return td.xtype;
	}
	
	public int size() alias length {
		return values.length;
	}
	public boolean isEmpty() {
		return values.length == 0;
	}
	
	public void verify() {
		JavaAnnotation tdecl = getAnnotationDecl();
		if (tdecl == null || !tdecl.isAnnotation()) {
			throw new CompilerException(this, "Annotation name expected");
		}
//		String name = this.qname();
//		UserMeta m = this;
//		if (m != this) {
//			this.replaceWithNode(m);
//			foreach (MetaValue v; values)
//				m.set(v.ncopy());
//			m.verify();
//		}
		foreach (MetaValue v; values)
			v.verify();
		return;
	}
	
	public void resolve(Type reqType) {
		JavaAnnotation tdecl = getAnnotationDecl();
		tdecl.checkResolved();
		for (int n=0; n < values.length; n++) {
			MetaValue v = values[n];
			Method m = null;
			foreach (Method sm; tdecl.members) {
				if( sm.hasName(v.ident)) {
					m = sm;
					break;
				}
			}
			if (m == null)
				throw new CompilerException(v, "Unresolved method "+v.ident+" in class "+tdecl);
			v.symbol = m;
			Type t = m.mtype.ret();
			if (t instanceof ArrayType) {
				if (v instanceof MetaValueScalar) {
					ASTNode val = ((MetaValueScalar)v).value;
					MetaValueArray mva = new MetaValueArray(new SymbolRef(v.pos,v.ident)); 
					mva.values.add(~val);
					values[n] = v = mva;
				}
				t = t.arg;
			}
			if (t.isReference()) {
				t.checkResolved();
				if (t.getStruct() == null || !(t ≈ Type.tpString || t ≈ Type.tpClass || t.getStruct().isAnnotation() || t.getStruct().isEnum()))
					throw new CompilerException(m, "Bad annotation value type "+t);
			}
			v.resolve(t);
		}
		// check that all non-default values are specified, and add default values
	next_method:
		foreach (Method m; tdecl.members) {
			for(int j=0; j < values.length; j++) {
				if (values[j].symbol != null)
					continue next_method;
			}
			// value not specified - does the method has a default meta-value?
			if !(m.body instanceof MetaValue)
				Kiev.reportError(this, "Annotation value "+m.sname+" is not specified");
		}
	}
	
	public Symbol[] resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "decl") {
			TypeDecl scope;
			String head;
			int dot = name.indexOf('\u001f');
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1);
			} else {
				head = name;
				name = "";
			}
			if (dot < 0) {
				int flags = ResInfo.noForwards|ResInfo.noEquals;
				Vector<Symbol> vect = new Vector<Symbol>();
				ResInfo info = new ResInfo(this,head,flags);
				foreach (PassInfo.resolveNameR(this,info)) {
					DNode dn = info.resolvedDNode();
					if ((dn instanceof KievPackage || dn instanceof JavaAnnotation) && !vect.contains(info.resolvedSymbol()))
						vect.append(info.resolvedSymbol());
				}
				return vect.toArray();
			} else {
				ResInfo<TypeDecl> info = new ResInfo<TypeDecl>(this,head,ResInfo.noForwards);
				if (!PassInfo.resolveNameR(this,info))
					return null;
				scope = info.resolvedDNode();
			}
			while (dot >= 0) {
				dot = name.indexOf('\u001f');
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
				} else {
					head = name.intern();
					name = "";
				}
				if (dot < 0) {
					int flags = ResInfo.noForwards|ResInfo.noEquals;
					Vector<Symbol> vect = new Vector<Symbol>();
					ResInfo info = new ResInfo(this,head,flags);
					foreach (scope.resolveNameR(info)) {
						DNode dn = info.resolvedDNode();
						if ((dn instanceof KievPackage || dn instanceof JavaAnnotation) && !vect.contains(info.resolvedSymbol()))
							vect.append(info.resolvedSymbol());
					}
					return vect.toArray();
				} else {
					ResInfo<TypeDecl> info = new ResInfo<TypeDecl>(this,head,ResInfo.noForwards);
					if!(scope.resolveNameR(info))
						return null;
					scope = info.resolvedDNode();
				}
			}
		}
		return super.resolveAutoComplete(name,slot);
	}

	public MetaValue get(String name) {
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
				MetaValue v = values[i];
				return v;
			}
		}
		JavaAnnotation td = getAnnotationDecl();
		foreach (Method m; td.members; m.hasName(name))
			return (MetaValue)m.body;
		throw new RuntimeException("Value "+name+" not found in "+decl+" annotation");
	}
	
	public boolean getZ(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return false;
		if (v instanceof ConstBoolExpr)
			return ((ConstBoolExpr)v).value;
		if (v instanceof ENode && v.isConstantExpr()) {
			Object val = v.getConstValue();
			if (val instanceof Boolean)
				return ((Boolean)val).booleanValue();
		}
		throw new RuntimeException("Value "+name+" in annotation "+decl+" is not a boolean constant, but "+v);
	}
	
	public int getI(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return 0;
		if (v instanceof ConstIntExpr)
			return ((ConstIntExpr)v).value;
		if (v instanceof ENode && v.isConstantExpr()) {
			Object val = v.getConstValue();
			if (val instanceof Number)
				return ((Number)val).intValue();
		}
		throw new RuntimeException("Value "+name+" in annotation "+decl+" is not an int constant, but "+v);
	}
	
	public String getS(String name) {
		MetaValueScalar mv = (MetaValueScalar)get(name);
		ASTNode v = mv.value;
		if (v == null)
			return null;
		if (v instanceof ConstStringExpr)
			return ((ConstStringExpr)v).value;
		if (v instanceof ENode && v.isConstantExpr()) {
			Object val = v.getConstValue();
			if (val instanceof String)
				return (String)val;
		}
		throw new RuntimeException("Value "+name+" in annotation "+decl+" is not a String constant, but "+v);
	}
	
	public MetaValue set(MetaValue value)
	{
		if (value == null)
			throw new NullPointerException();
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == value.ident) {
				values[i] = value;
				return value;
			}
		}
		values.append(value);
		return value;
	}

	public MetaValue setZ(String name, boolean val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
				((MetaValueScalar)values[i]).value = new ConstBoolExpr(val);
				return values[i];
			}
		}
		MetaValueScalar mv = new MetaValueScalar(new SymbolRef<DNode>(name), new ConstBoolExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue setI(String name, int val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
				((MetaValueScalar)values[i]).value = new ConstIntExpr(val);
				return values[i];
			}
		}
		MetaValueScalar mv = new MetaValueScalar(new SymbolRef<DNode>(name), new ConstIntExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue setS(String name, String val)
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
				((MetaValueScalar)values[i]).value = new ConstStringExpr(val);
				return values[i];
			}
		}
		MetaValueScalar mv = new MetaValueScalar(new SymbolRef<DNode>(name), new ConstStringExpr(val));
		values.append(mv);
		return mv;
	}

	public MetaValue unset(MetaValue value) alias del alias lfy operator -=
	{
		return unset(value.ident);
	}
	public MetaValue unset(String name) alias del alias lfy operator -=
	{
		int sz = values.length;
		for (int i=0; i < sz; i++) {
			if (values[i].ident == name) {
				MetaValue v = values[i];
				values.del(i);
				return v;
			}
		}
		return null;
	}

	public boolean contains(MetaValue value) {
		for (int i = 0 ; i >= 0 ; i--) {
			if (values[i].equals(value))
				return true;
		}
		return false;
	}
}

@ThisIsANode(lang=CoreLang)
public abstract class MetaValue extends ENode {
	public static final MetaValue[] emptyArray = new MetaValue[0];

	public MetaValue() {}

	public MetaValue(SymbolRef<DNode> ident) {
		if (ident != null) {
			this.pos = ident.pos;
			this.ident = ident.name;
		}
	}

	public abstract boolean valueEquals(Object mv);

	public void verify() {
		if (parent() instanceof Method && pslot().name == "body") {
			Method m = (Method)parent();
			if (this.dnode != m)
				this.symbol = m;
		}
		else if (ident == null) {
			if (ident != "value")
				this.ident = "value";
		}
	}
	
	boolean checkValue(Type reqType, ASTNode value) {
		if (value instanceof TypeRef) {
			if (reqType ≈ Type.tpClass) {
				((TypeRef)value).getType();
				return false;
			} else {
				throw new CompilerException(this, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+
					value+" ("+value.getClass()+")");
			}
		}
		if !(value instanceof ENode)
			return false;
		ENode v = (ENode)value;
		if (v instanceof SFldExpr && ((SFldExpr)v).var.isEnumField()) {
			return false;
		}
		else if (!v.isConstantExpr())
			throw new CompilerException(this, "Annotation value must be a Constant, Class, Annotation or array of them, but found "+v+" ("+v.getClass()+")");
		Type vt = value.getType();
		if (vt ≉ reqType) {
			v.replaceWith(fun ()->ASTNode {return new CastExpr(v.pos, reqType, v);});
			return true;
		}
		if (!v.isConstantExpr())
			throw new CompilerException(this, "Annotation value must be a constant, but found "+v+" ("+v.getClass()+")");
		Type vt = v.getType();
		if (vt ≉ reqType)
			throw new CompilerException(this, "Wrong annotation value type "+vt+", type "+reqType+" is expected for value "+ident);
		return false;
	}
}

@ThisIsANode(name="MetaVal", lang=CoreLang)
public final class MetaValueScalar extends MetaValue {

	@nodeAttr public ASTNode			value;

	public MetaValueScalar() {}

	public MetaValueScalar(SymbolRef<DNode> ident) {
		super(ident);
	}

	public MetaValueScalar(SymbolRef<DNode> ident, ASTNode value) {
		super(ident);
		this.value = value;
	}

	public boolean valueEquals(Object mv) {
		if (mv instanceof MetaValueScalar && this.ident == mv.ident) {
			ASTNode v1 = this.value;
			ASTNode v2 = mv.value;
			if (v1 instanceof ENode && v2 instanceof ENode)
				return v1.valueEquals(v2);
		}
		return false;
	}

	public void verify() {
		super.verify();
		if (value instanceof MNode)
			((MNode)value).verify();
	}
	
	public void resolve(Type reqType) {
		boolean ok;
		do {
			ok = true;
			try {
				Kiev.runFrontEndProcessorsOn(value);
			} catch (ReWalkNodeException e) { ok = false; }
		} while (ok && checkValue(reqType, value));
	}
}

@ThisIsANode(name="MetaArr", lang=CoreLang)
public final class MetaValueArray extends MetaValue {

	@nodeAttr public ASTNode∅				values;

	public MetaValueArray() {}

	public MetaValueArray(SymbolRef<DNode> ident) {
		super(ident);
	}

	public MetaValueArray(SymbolRef<DNode> ident, ASTNode[] values) {
		super(ident);
		this.values.addAll(values);
	}

	public boolean valueEquals(Object mv) {
		if (mv instanceof MetaValueArray && this.ident == mv.ident) {
			MetaValueArray mva = (MetaValueArray)mv;
			if (values.length != mva.values.length)
				return false;
			for (int i=0; i < values.length; i++) {
				ASTNode v1 = values[i];
				ASTNode v2 = mva.values[i];
				if (v1 instanceof ENode && v2 instanceof ENode && !v1.valueEquals(v2))
					return false;
			}
			return true;
		}
		return false;
	}

	public void verify() {
		super.verify();
		for (int i=0; i < values.length; i++) {
			if (values[i] instanceof MNode)
				((MNode)values[i]).verify();
		}
	}
	
	public void resolve(Type reqType) {
		for (int i=0; i < values.length; i++) {
			boolean ok;
			do {
				ok = true;
				try {
					Kiev.runFrontEndProcessorsOn(this.values[i]);
				} catch (ReWalkNodeException e) { ok = false; }
			} while (ok && checkValue(reqType, this.values[i]));
		}
	}
}


