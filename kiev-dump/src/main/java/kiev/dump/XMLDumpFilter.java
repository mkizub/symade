package kiev.dump;

import java.util.Stack;
import java.util.Enumeration;
import kiev.vlang.FileUnit;
import kiev.vlang.MetaAccess;
import kiev.vtree.ANode;
import kiev.vtree.INode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.ExtSpaceIterator;
import kiev.vtree.ScalarAttrSlot;

public class XMLDumpFilter implements DumpFilter {
	
	public final Stack<String> dumpModeStack = new Stack<String>();
	
	public XMLDumpFilter(String dumpMode) {
		this.dumpModeStack.push(dumpMode);
	}
	
	public boolean ignoreNode(INode parent, AttrSlot attr, INode node) {
		if (node == null)
			return true;
		if (!parent.asANode().includeInDump(dumpModeStack.peek(), attr, node))
			return true;
		if (!node.asANode().includeInDump(dumpModeStack.peek(), ANode.nodeattr$this, node))
			return true;
		return false;
	}

	public boolean ignoreAttr(INode node, AttrSlot attr) {
		if (node == null || attr == null)
			return true;
		if (attr instanceof ScalarAttrSlot) {
			Object val = ((ScalarAttrSlot)attr).get(node);
			if (val == null)
				return true;
			if (node instanceof FileUnit && (attr.name == "fname" || attr.name == "ftype"))
				return false;
			if (attr.typeinfo.clazz == Boolean.TYPE)
				return !((Boolean)val).booleanValue();
			if (attr.typeinfo.clazz == Integer.TYPE || attr.typeinfo.clazz == Byte.TYPE || attr.typeinfo.clazz == Short.TYPE || attr.typeinfo.clazz == Long.TYPE)
				return ((Number)val).longValue() == 0L;
			if (node instanceof MetaAccess && attr.name == "flags" && ((Integer)val).intValue() == -1)
				return true;
			return !node.asANode().includeInDump(dumpModeStack.peek(), attr, val);
		}
		else if (attr instanceof ASpaceAttrSlot) {
			Enumeration iter = ((ASpaceAttrSlot)attr).iterate(node);
			if (!iter.hasMoreElements())
				return true;
			if (!node.asANode().includeInDump(dumpModeStack.peek(), attr, iter))
				return true;
			while (iter.hasMoreElements()) {
				INode n = (INode)iter.nextElement();
				if (node.asANode().includeInDump(dumpModeStack.peek(), attr, n))
					return false;
			}
			return true;
		}
		return true;
	}
}
