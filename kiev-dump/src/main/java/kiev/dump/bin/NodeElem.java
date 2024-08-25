package kiev.dump.bin;

import kiev.vlang.types.MetaType;
import kiev.vlang.ComplexTypeDecl;
import kiev.vlang.Env;
import kiev.vlang.DNode;
import kiev.vlang.KievPackage;
import kiev.vlang.TypeDecl;
import kiev.vlang.ComplexTypeDecl;
import kiev.vlang.types.MetaType;
import kiev.vtree.INode;
import kiev.vtree.Symbol;

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
		if (node instanceof DNode && !node.isAttached()) {
			DNode dn = (DNode) node;
			Symbol sym = dn.getDeclSymbol();
			if (sym != null && sym.getNameSpaceSymbol() != null) {
				Symbol ns = sym.getNameSpaceSymbol();
				INode p = ns.derefDNode(env.getEnvContext());
				if (p instanceof KievPackage) {
					if (node instanceof ComplexTypeDecl) {
						TypeDecl td = (TypeDecl) node;
						MetaType mt = env.tenv.getExistingMetaType(sym);
						TypeDecl old_tdecl = mt == null ? null : mt.getTypeDecl();
						if (old_tdecl != null && old_tdecl != td)
							old_tdecl.replaceWithNode(td, old_tdecl.parent(), old_tdecl.pslot());
						else
							p.addVal(p.getAttrSlot("pkg_members"), dn);
					} else {
						p.addVal(p.getAttrSlot("pkg_members"), dn);
					}
				}
			}
		}
		if (node instanceof TypeDecl) {
			TypeDecl td = (TypeDecl)node;
			env.tenv.callbackTypeVersionChanged(td);
			td.getMetaType();
		}
	}


}
