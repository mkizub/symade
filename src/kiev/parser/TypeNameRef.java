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

@nodeset
public class TypeNameRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeNameRefImpl;
	@virtual typedef VView = TypeNameRefView;

	@nodeimpl
	public static final class TypeNameRefImpl extends TypeRefImpl {
		@virtual typedef ImplOf = TypeNameRef;
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

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

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

