package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;

import syntax kiev.Syntax;

import kiev.vlang.TypeDecl.TypeDeclImpl;
import kiev.vlang.TypeDecl.TypeDeclView;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeDef extends TypeDecl {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class TypeDefImpl extends TypeDeclImpl {
		@att public NameRef					name;
		@att public TypeRef					super_bound;
		@att public boolean					erasable;
		@att public ArgumentType			lnk;
		public TypeDefImpl() {}
		public TypeDefImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view TypeDefView of TypeDefImpl extends TypeDeclView {
		public NameRef				name;
		public TypeRef				super_bound;
		public boolean				erasable;
		public ArgumentType			lnk;
	}

	@att public abstract virtual NameRef				name;
	@att public abstract virtual TypeRef				super_bound;
	@att public abstract virtual boolean				erasable;
	@att        abstract virtual ArgumentType			lnk;
	
	public NodeView			getNodeView()			{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public DNodeView		getDNodeView()			{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public TypeDeclView		getTypeDeclView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public TypeDefView	getTypeDefView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }

	@getter public NameRef		get$name()			{ return this.getTypeDefView().name; }
	@getter public TypeRef		get$super_bound()	{ return this.getTypeDefView().super_bound; }
	@getter public boolean		get$erasable()		{ return this.getTypeDefView().erasable; }
	@getter public ArgumentType	get$lnk()			{ return this.getTypeDefView().lnk; }
	@setter public void		set$name(NameRef val)			{ this.getTypeDefView().name = val; }
	@setter public void		set$super_bound(TypeRef val)	{ this.getTypeDefView().super_bound = val; }
	@setter public void		set$erasable(boolean val)		{ this.getTypeDefView().erasable = val; }
	@setter public void		set$lnk(ArgumentType val)		{ this.getTypeDefView().lnk = val; }
	
	public TypeDef() { super(new TypeDefImpl()); }

	public TypeDef(KString nm) {
		super(new TypeDefImpl());
		name = new NameRef(nm);
	}

	public TypeDef(NameRef nm) {
		super(new TypeDefImpl(nm.getPos()));
		this.name = nm;
	}

	public TypeDef(NameRef nm, TypeRef sup) {
		super(new TypeDefImpl(nm.getPos()));
		this.name = nm;
		this.super_bound = sup;
	}

	public NodeName getName() {
		return new NodeName(name.name);
	}

	public boolean checkResolved() {
		Type t = this.getType();
		if (t != null && t.getStruct() != null)
			return t.getStruct().checkResolved();
		return true;
	}
	
	public boolean isBound() {
		return true;
	}

	public Type getType() {
		return getAType();
	}
	public ArgumentType getAType() {
		if (this.lnk != null)
			return this.lnk;
		ClazzName cn;
		if (parent instanceof Struct) {
			Struct s = (Struct)parent;
			MetaErasable er = s.getMetaErasable();
			this.erasable = (er == null || er.value);
		} else {
			this.erasable = true;
		}
		Type sup = Type.tpObject;
		if (super_bound != null)
			sup = super_bound.getType();
		this.lnk = new ArgumentType(name.name,(DNode)parent,sup,erasable);
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

