package kiev.dump.bin;

import kiev.vlang.types.MetaType;
import kiev.vlang.ComplexTypeDecl;
import kiev.vlang.Env;
import kiev.vtree.INode;

public class NodeElem extends Elem {

	// type of this node
	public TypeElem		tp;
	// node for this elem
	public INode		node;
	
	public NodeElem(int id, TypeElem tp) {
		super(id);
		this.tp = tp;
	}

	public NodeElem(int id, int addr) {
		super(id, addr);
	}

	public void build(Env env) {
		if (node instanceof ComplexTypeDecl) {
			ComplexTypeDecl td = (ComplexTypeDecl)node;
			env.tenv.callbackTypeVersionChanged(td);
		}
	}
	
	
}
