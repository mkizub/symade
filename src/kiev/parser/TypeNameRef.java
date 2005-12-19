package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;
import static kiev.stdlib.Debug.*;

import kiev.vlang.TypeRef.TypeRefImpl;
import kiev.vlang.TypeRef.TypeRefView;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeNameRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class TypeNameRefImpl extends TypeRefImpl {
		@att public NameRef					name;
		public TypeNameRefImpl() {}
		public TypeNameRefImpl(int pos) { super(pos, null); }
	}
	@nodeview
	public static final view TypeNameRefView of TypeNameRefImpl extends TypeRefView {
		public NameRef				name;
	}

	@att public abstract virtual NameRef				name;
	
	public NodeView			getNodeView()			{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()		{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public TypeNameRefView	getTypeNameRefView()	{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }

	@getter public NameRef		get$name()			{ return this.getTypeNameRefView().name; }
	@setter public void		set$name(NameRef val)	{ this.getTypeNameRefView().name = val; }
	
	public TypeNameRef() {
		super(new TypeNameRefImpl());
	}

	public TypeNameRef(KString nm) {
		super(new TypeNameRefImpl());
		name = new NameRef(nm);
	}

	public TypeNameRef(NameRef nm) {
		super(new TypeNameRefImpl(nm.getPos()));
		this.name = nm;
	}

	public TypeNameRef(NameRef nm, Type tp) {
		super(new TypeNameRefImpl(nm.getPos()));
		this.name = nm;
		this.lnk = tp;
	}


	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		KString nm = name.name;
		DNode@ v;
		if( !PassInfo.resolveQualifiedNameR(this,v,new ResInfo(this,ResInfo.noForwards),nm) )
			throw new CompilerException(this,"Unresolved identifier "+nm);
		if( v instanceof TypeDef ) {
			TypeDef td = (TypeDef)v;
			td.checkResolved();
			this.lnk = td.getType();
		}
		if (this.lnk == null)
			throw new CompilerException(this,"Type "+this+" is not found");
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

