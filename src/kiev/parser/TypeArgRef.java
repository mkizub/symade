package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeArgDef extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	private static int anonymousCounter = 100;
	
	@att public NameRef					name;
	@att public TypeRef					super_bound;
	private Type						lnk;

	public TypeArgDef() { super(new TypeDefImpl()); }

	public TypeArgDef(KString nm) {
		super(new TypeDefImpl());
		name = new NameRef(nm);
	}

	public TypeArgDef(NameRef nm) {
		super(new TypeDefImpl(nm.getPos()));
		this.name = nm;
	}

	public TypeArgDef(NameRef nm, TypeRef sup) {
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
		if (this.lnk != null)
			return this.lnk;
		ClazzName cn;
		if (parent instanceof Struct) {
			Struct s = (Struct)parent;
			foreach (TypeArgDef pa; s.package_clazz.args; pa.name.name == name.name) {
				this.lnk = pa.getType();
				if (this.lnk == null)
					throw new CompilerException(this,"Type "+this+" is not found");
				return this.lnk;
			}
			KString nm = KString.from(s.name.name+"$"+name.name);
			KString bc = KString.from(s.name.bytecode_name+"$"+name.name);
			cn = new ClazzName(nm,name.name,bc,true,true);
		} else {
			int cnt = anonymousCounter++;
			KString nm = KString.from("$"+cnt+"$"+name.name);
			cn = new ClazzName(nm,name.name,nm,true,true);
		}
		BaseType sup = null;
		if (Kiev.pass_no.ordinal() >= TopLevelPass.passArgumentInheritance.ordinal()) {
			if (super_bound != null)
				sup = (BaseType)super_bound.getType();
		}
		this.lnk = ArgumentType.newArgumentType(cn,sup);
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

