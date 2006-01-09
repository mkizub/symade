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
		@att public TypeRef					lower;
		public TypeNameRefImpl() {}
		public TypeNameRefImpl(int pos) { super(pos, null); }
	}
	@nodeview
	public static final view TypeNameRefView of TypeNameRefImpl extends TypeRefView {
		public TypeRef				outer;
		public KString				name;
		public TypeRef				lower;
	}

	@att public abstract virtual TypeRef				outer;
	@att public abstract virtual KString				name;
	@att public abstract virtual TypeRef				lower;
	
	public NodeView			getNodeView()			{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()		{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }
	public TypeNameRefView	getTypeNameRefView()	{ return new TypeNameRefView((TypeNameRefImpl)this.$v_impl); }

	@getter public TypeRef		get$outer()			{ return this.getTypeNameRefView().outer; }
	@getter public KString		get$name()			{ return this.getTypeNameRefView().name; }
	@getter public TypeRef		get$lower()			{ return this.getTypeNameRefView().lower; }
	@setter public void		set$outer(TypeRef val)	{ this.getTypeNameRefView().outer = val; }
	@setter public void		set$name(KString val)	{ this.getTypeNameRefView().name = val; }
	@setter public void		set$lower(TypeRef val)	{ this.getTypeNameRefView().lower = val; }
	
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
	public void setLowerBound(Type tp) {
		this.lower = new TypeRef(tp);
		this.lnk = null;
	}

	public Type getTypeWithoutLower() {
		if (this.lnk != null)
			return this.lnk;
		Type tp;
		if (this.outer != null) {
			Type outer = this.outer.getType();
			ResInfo info = new ResInfo(this,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.resolveStaticNameR(td,info,name))
				throw new CompilerException(this,"Unresolved type "+name+" in "+outer);
			td.checkResolved();
			tp = td.getType().bind(outer.bindings());
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveQualifiedNameR(this,td,new ResInfo(this,ResInfo.noForwards),name) )
				throw new CompilerException(this,"Unresolved type "+name);
			td.checkResolved();
			tp = td.getType();
		}
		return tp;
	}
	
	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
	    Type tp = getTypeWithoutLower();
		if (this.lower != null) {
			Type lt = this.lower.getType();
			if (!lt.isInstanceOf(tp))
				throw new CompilerException(this,"Type '"+lt+"' is not a lower bound of "+tp);
			tp = tp.toTypeWithLowerBound(lt);
		}
		this.lnk = tp;
		return this.lnk;
	}

	public Struct getStruct() {
		if (this.lnk != null) return this.lnk.getStruct();
		if (this.outer != null) {
			Struct outer = this.outer.getStruct();
			ResInfo info = new ResInfo(this,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.resolveNameR(td,info,name))
				throw new CompilerException(this,"Unresolved type "+name+" in "+outer);
			return td.getStruct();
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveQualifiedNameR(this,td,new ResInfo(this,ResInfo.noForwards),name) )
				throw new CompilerException(this,"Unresolved type "+name);
			return td.getStruct();
		}
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

