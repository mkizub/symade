package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;

import kiev.vlang.TypeDef.TypeDefImpl;
import kiev.vlang.TypeDef.TypeDefView;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeArgDef extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	private static int anonymousCounter = 100;
	
	@node
	public static final class TypeArgDefImpl extends TypeDefImpl {
		@att public NameRef					name;
		@att public TypeRef					super_bound;
		@att public ArgumentType			lnk;
		public TypeArgDefImpl() {}
		public TypeArgDefImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view TypeArgDefView of TypeArgDefImpl extends TypeDefView {
		public NameRef				name;
		public TypeRef				super_bound;
		public ArgumentType			lnk;
	}

	@att public abstract virtual NameRef				name;
	@att public abstract virtual TypeRef				super_bound;
	@att        abstract virtual ArgumentType			lnk;
	
	public NodeView			getNodeView()			{ return new TypeArgDefView((TypeArgDefImpl)this.$v_impl); }
	public DNodeView		getDNodeView()			{ return new TypeArgDefView((TypeArgDefImpl)this.$v_impl); }
	public TypeDefView		getTypeDefView()		{ return new TypeArgDefView((TypeArgDefImpl)this.$v_impl); }
	public TypeArgDefView	getTypeArgDefView()		{ return new TypeArgDefView((TypeArgDefImpl)this.$v_impl); }

	@getter public NameRef		get$name()			{ return this.getTypeArgDefView().name; }
	@getter public TypeRef		get$super_bound()	{ return this.getTypeArgDefView().super_bound; }
	@getter public ArgumentType	get$lnk()			{ return this.getTypeArgDefView().lnk; }
	@setter public void		set$name(NameRef val)			{ this.getTypeArgDefView().name = val; }
	@setter public void		set$super_bound(TypeRef val)	{ this.getTypeArgDefView().super_bound = val; }
	@setter public void		set$lnk(ArgumentType val)		{ this.getTypeArgDefView().lnk = val; }
	
	public TypeArgDef() { super(new TypeDefImpl()); }

	public TypeArgDef(KString nm) {
		super(new TypeArgDefImpl());
		name = new NameRef(nm);
	}

	public TypeArgDef(NameRef nm) {
		super(new TypeArgDefImpl(nm.getPos()));
		this.name = nm;
	}

	public TypeArgDef(NameRef nm, TypeRef sup) {
		super(new TypeArgDefImpl(nm.getPos()));
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
			KString nm = KString.from(s.name.name+"$"+name.name);
			KString bc = KString.from(s.name.bytecode_name+"$"+name.name);
			cn = new ClazzName(nm,name.name,bc,true,true);
		} else {
			int cnt = anonymousCounter++;
			KString nm = KString.from("$"+cnt+"$"+name.name);
			cn = new ClazzName(nm,name.name,nm,true,true);
		}
		Type sup = Type.tpObject;
		if (super_bound != null)
			sup = super_bound.getType();
		this.lnk = ArgumentType.newArgumentType(cn,ctx_clazz,sup);
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

