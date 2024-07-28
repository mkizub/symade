package kiev.dump;

import kiev.vlang.ENode;
import kiev.vlang.Initializer;
import kiev.vlang.Method;
import kiev.vlang.TypeDecl;
import kiev.vlang.types.TypeASTNodeRef;
import kiev.vlang.types.TypeClosureRef;
import kiev.vlang.types.TypeExpr;
import kiev.vlang.types.TypeRef;
import kiev.vtree.ANode;
import kiev.vtree.INode;
import kiev.vtree.AttrSlot;

public class BinDumpFilter implements DumpFilter {
	
	public final boolean api;
	
	public BinDumpFilter() {
		this(false);
	}
	public BinDumpFilter(boolean api) {
		this.api = api;
	}

	public boolean ignoreAttr(INode parent, AttrSlot attr) {
		if (parent instanceof ENode) {
			if (attr.name == "type_lnk") {
				if (!(parent instanceof TypeRef))
					return true;
				if (parent.getVal(attr) == null)
					return true;
			}
			if (attr.name == "symbol") {
				if (parent.getVal(attr) == null)
					return true;
			}
		}
		if (parent instanceof TypeRef) {
			if (parent instanceof TypeASTNodeRef || parent instanceof TypeClosureRef) {
				if (attr.name == "type_lnk" || attr.name == "symbol")
					return true;
			}
			if (parent instanceof TypeExpr && parent.getVal(parent.getAttrSlot("arg")) instanceof TypeASTNodeRef) {
				if (attr.name == "type_lnk" || attr.name == "symbol")
					return true;
			}
		}
		if (!api)
			return false;
		if (parent instanceof Method) {
			Method m = (Method)parent;
			if (attr.name != "body")
				return false;
			if (m.isMacro())
				return false;
			if (m.parent() instanceof TypeDecl) {
				TypeDecl p = (TypeDecl)m.parent();
				if (p.isMixin() || p.isMacro())
					return false;
			}
			return true;
		}
		return false;
	}

	public boolean ignoreNode(INode parent, AttrSlot attr, INode node) {
		if (!api)
			return false;
		if (node instanceof Initializer)
			return true;
		return false;
	}

}
