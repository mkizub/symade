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
	
	public JClass(Struct vclazz) {
		super(vclazz.pos);
		this.vclazz = vclazz;
		foreach (ASTNode n; vclazz.members; n instanceof Field) {
			JField jf = new JField((Field)n);
			fields.append(jf);
		}
		foreach (ASTNode n; vclazz.members; n instanceof Method) {
			JMethod jm = new JMethod((Method)n);
			methods.append(jm);
		}
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
	
}
