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
		@att public ArgumentType			lnk;
		public TypeDefImpl() {}
		public TypeDefImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view TypeDefView of TypeDefImpl extends TypeDeclView {
		public NameRef				name;
		public TypeRef				super_bound;
		public ArgumentType			lnk;
	}

	@att public abstract virtual NameRef				name;
	@att public abstract virtual TypeRef				super_bound;
	@att        abstract virtual ArgumentType			lnk;
	
	public NodeView			getNodeView()			{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public DNodeView		getDNodeView()			{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public TypeDeclView		getTypeDeclView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public TypeDefView		getTypeDefView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }

	@getter public NameRef		get$name()			{ return this.getTypeDefView().name; }
	@getter public TypeRef		get$super_bound()	{ return this.getTypeDefView().super_bound; }
	@getter public ArgumentType	get$lnk()			{ return this.getTypeDefView().lnk; }
	@setter public void		set$name(NameRef val)			{ this.getTypeDefView().name = val; }
	@setter public void		set$super_bound(TypeRef val)	{ this.getTypeDefView().super_bound = val; }
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
	
	// a typedef's argument is forward
	public final boolean isTypeForward() { return getMetaForward() != null; }
	// a typedef's argument is virtual
	public final boolean isTypeVirtual() { return getMetaVirtual() != null; }

	public final MetaForward getMetaForward() {
		MetaSet ms = this.meta;
		if (ms != null) {
			foreach (Meta m; ms.metas; m instanceof MetaForward)
				return (MetaForward)m;
		}
		return null;
	}

	public final MetaVirtual getMetaVirtual() {
		MetaSet ms = this.meta;
		if (ms != null) {
			foreach (Meta m; ms.metas; m instanceof MetaVirtual)
				return (MetaVirtual)m;
		}
		return null;
	}

	public Type getType() {
		return getAType();
	}
	public ArgumentType getAType() {
		if (this.lnk != null)
			return this.lnk;
		if (this.meta != null)
			this.meta.verify();
		Type sup = Type.tpObject;
		if (super_bound != null)
			sup = super_bound.getType();
		this.lnk = new ArgumentType(name.name,(DNode)parent,sup,isTypeUnerasable(),isTypeVirtual(),isTypeForward());
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

