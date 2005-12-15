package kiev.parser;

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

	@att public NameRef			name;

	public TypeNameRef() {
	}

	public TypeNameRef(KString nm) {
		name = new NameRef(nm);
	}

	public TypeNameRef(NameRef nm) {
		this.pos = nm.getPos();
		this.name = nm;
	}

	public TypeNameRef(NameRef nm, Type tp) {
		this.pos = nm.getPos();
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

