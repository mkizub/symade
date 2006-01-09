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

public class TypeDef extends TypeDecl {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeDefImpl;
	@virtual typedef VView = TypeDefView;

	@node
	public static final class TypeDefImpl extends TypeDeclImpl {
		@virtual typedef ImplOf = TypeDef;
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

	public VView getVView() { return new VView(this.$v_impl); }

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
	
	// a typedef's argument is virtual
	public final boolean isTypeVirtual() { return getMetaVirtual() != null; }

	public final MetaVirtual getMetaVirtual() {
		return (MetaVirtual)this.getNodeData(MetaVirtual.ID);
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
		this.lnk = new ArgumentType(name.name,this,sup,isTypeUnerasable(),isTypeVirtual());
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

