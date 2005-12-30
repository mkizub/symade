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
		@att public TypeRef					outer;
		@att public KString					name;
		public TypeNameRefImpl() {}
		public TypeNameRefImpl(int pos) { super(pos, null); }
	}
	@nodeview
	public static final view TypeNameRefView of TypeNameRefImpl extends TypeRefView {
		public TypeRef				outer;
		public KString				name;
	}

	@att public abstract virtual KString				name;
	
	public NodeView			getNodeView()			{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()		{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public TypeNameRefView	getTypeNameRefView()	{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }

	@getter public TypeRef		get$outer()			{ return this.getTypeNameRefView().outer; }
	@getter public KString		get$name()			{ return this.getTypeNameRefView().name; }
	@setter public void		set$outer(TypeRef val)	{ this.getTypeNameRefView().outer = val; }
	@setter public void		set$name(KString val)	{ this.getTypeNameRefView().name = val; }
	
	public TypeNameRef() {
		super(new TypeNameRefImpl());
	}

	public TypeNameRef(KString nm) {
		super(new TypeNameRefImpl());
		name = nm;
	}

	public TypeNameRef(NameRef nm) {
		super(new TypeNameRefImpl(nm.getPos()));
		this.name = nm.name;
	}

	public TypeNameRef(NameRef nm, Type tp) {
		super(new TypeNameRefImpl(nm.getPos()));
		this.name = nm.name;
		this.lnk = tp;
	}

	public TypeNameRef(TypeRef outer, NameRef nm) {
		super(new TypeNameRefImpl(nm.getPos()));
		this.outer = outer;
		this.name = nm.name;
	}


	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		if (this.outer != null) {
			Type outer = this.outer.getType();
			ResInfo info = new ResInfo(this,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDef@ td;
			if!(outer.resolveStaticNameR(td,info,name))
				throw new CompilerException(this,"Unresolved type "+name+" in "+outer);
			td.checkResolved();
			this.lnk = td.getType().bind(outer.bindings());
		} else {
			TypeDef@ td;
			if( !PassInfo.resolveQualifiedNameR(this,td,new ResInfo(this,ResInfo.noForwards),name) )
				throw new CompilerException(this,"Unresolved type "+name);
			td.checkResolved();
			this.lnk = td.getType();
		}
		return this.lnk;
	}

	public String toString() {
		if (outer == null)
			return String.valueOf(name);
		return outer+String.valueOf(name);
	}
	public Dumper toJava(Dumper dmp) {
		if (outer == null)
			return dmp.append(name);
		return dmp.append(outer).append('.').append(name);
	}
}

