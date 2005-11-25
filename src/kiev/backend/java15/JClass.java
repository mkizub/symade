package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


@node(copyable=false)
@dflow(in="root()")
public class JPackage extends DNode implements Named, ScopeOfNames {
	@att KString			name;
	@att NArr<JPackage>		sub_package;
	@att NArr<JClazz>		sub_clazz;
	
	private Struct			vstruct;
	private KString			qname;
	
	public JPackage(KString name) {
		this.name = name;
	}
	
	public Struct getStruct() {
		if (vstruct == null)
			vstruct = Env.newPackage(getQName());
		return vstruct;
	}
	
	public JPackage getParentPackage() {
		return (JPackage)parent;
	}
	
	public KString getQName() {
		if (qname != null)
			return qname;
		JPackage p = getParentPackage();
		if (p == null || p.name == KString.Empty)
			qname = name;
		else
			qname = KString.from(p.getQName()+"."+name);
		return qname;
	}
	
	public void callbackRootChanged() {
		super.callbackRootChanged();
		qname = null;
		vstruct = null;
	}
	
	public void importSubTree() {
		Struct s = getStruct();
		foreach (DNode d; s.members; d instanceof Struct) {
			if (d.isPackage()) {
				JPackage jp = new JPackage(d.name.short_name);
				sub_package.append(jp);
				jp.importSubTree();
			} else {
				JClazz jc = new JClazz(d.name);
				sub_clazz.append(jc);
				jc.importSubTree();
			}
		}
	}
}

@node(copyable=false)
@dflow(in="root()")
public class JClass extends DNode implements Named, ScopeOfNames, ScopeOfMethods, ScopeOfOperators, Accessable {
	/** Struct of vlang that produced this java class */
	@ref
	public final Struct						vclazz;

	/** Array of subclasses of this class */
	@att
	@dflow(in="", seq="false")
	public final NArr<JClass>				sub_clazz;

	/** Array of fields of this class */
	@att
	@dflow(in="", seq="false")
	public final NArr<JField>				fields;

	/** Array of methods of this class */
	@att
	@dflow(in="", seq="false")
	public final NArr<JMethod>				methods;

	/** Class' access */
	@virtual
	public virtual abstract Access			acc;

	/** Array of attributes of this structure */
	public Attr[]							attrs = Attr.emptyArray;
	
	public JClass(KString name) {
		this.name = name;
	}

	public Struct getStruct() {
		if (vclazz == null)
			vclazz = Env.getStruct(getQName());
		return vclazz;
	}
	
	public Object copy() {
		throw new CompilerException(this,"JClass node cannot be copied");
	};

	@getter public Access get$acc() { return vclazz.get$acc(); }
	@setter public void set$acc(Access a) { vclazz.set$acc(a); }
	public NodeName getName() { return vclazz.name; }
	
	public int hashCode() { return getName().hashCode(); }

	public Dumper toJava(Dumper dmp) {
		ClazzName name = vclazz.name;
		if (isArgument() || isLocal())
			dmp.append(name.short_name);
		else
			dmp.append(name);
		return dmp;
	}
	
	public void importSubTree() {
		Struct s = getStruct();
		foreach (DNode d; s.members; d instanceof Struct) {
			if (d.isPackage()) {
				JPackage jp = new JPackage(d.name.short_name);
				sub_package.append(jp);
				jp.importSubTree();
			} else {
				JClazz jc = new JClazz(d.name);
				sub_clazz.append(jc);
				jc.importSubTree();
			}
		}
	}
}
