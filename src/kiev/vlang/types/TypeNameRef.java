package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;
import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeNameRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeNameRef;
	@virtual typedef VView = VTypeNameRef;

	@att public TypeRef					outer;
	@att public KString					name;

	@nodeview
	public static final view VTypeNameRef of TypeNameRef extends VTypeRef {
		public TypeRef				outer;
		public KString				name;
	}

	public TypeNameRef() {}

	public TypeNameRef(KString nm) {
		name = nm;
	}

	public TypeNameRef(NameRef nm) {
		this.pos = pos;
		this.name = nm.name;
	}

	public TypeNameRef(NameRef nm, Type tp) {
		this.pos = pos;
		this.name = nm.name;
		this.lnk = tp;
	}

	public TypeNameRef(TypeRef outer, NameRef nm) {
		this.pos = pos;
		this.outer = outer;
		this.name = nm.name;
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
			if( !PassInfo.resolveQualifiedNameR(((TypeNameRef)this),td,new ResInfo(this,ResInfo.noForwards),name) )
				throw new CompilerException(this,"Unresolved type "+name);
			td.checkResolved();
			tp = td.getType();
		}
		this.lnk = tp;
		return this.lnk;
	}

	public boolean isBound() {
		return true;
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
		return outer+"."+String.valueOf(name);
	}
	public Dumper toJava(Dumper dmp) {
		if (outer == null)
			return dmp.append(name);
		return dmp.append(outer).append('.').append(name);
	}
}

