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
		if (parent == null)
			return false;
		if (node == null)
			return true;
		if (!parent.includeInDump(dumpModeStack.peek(), attr, node))
			return true;
		if (!node.includeInDump(dumpModeStack.peek(), ANode.nodeattr$this, node))
			return true;
		return false;
	}

	public boolean ignoreAttr(INode parent, AttrSlot attr) {
		if (parent == null)
			return false;
		if (attr instanceof ScalarAttrSlot) {
			Object val = ((ScalarAttrSlot)attr).get(parent);
			if (val == null)
				return true;
			if (parent instanceof FileUnit && (attr.name == "fname" || attr.name == "ftype"))
				return false;
			if (attr.typeinfo.clazz == Boolean.TYPE)
				return !((Boolean)val).booleanValue();
			if (attr.typeinfo.clazz == Integer.TYPE || attr.typeinfo.clazz == Byte.TYPE || attr.typeinfo.clazz == Short.TYPE || attr.typeinfo.clazz == Long.TYPE)
				return ((Number)val).longValue() == 0L;
			if (parent instanceof MetaAccess && attr.name == "flags" && ((Integer)val).intValue() == -1)
				return true;
			return !parent.includeInDump(dumpModeStack.peek(), attr, val);
		}
		else if (attr instanceof ASpaceAttrSlot) {
			Enumeration iter = ((ASpaceAttrSlot)attr).iterate(parent);
			if (!iter.hasMoreElements())
				return true;
			if (!parent.includeInDump(dumpModeStack.peek(), attr, iter))
				return true;
			while (iter.hasMoreElements()) {
				INode n = (INode)iter.nextElement();
				if (parent.includeInDump(dumpModeStack.peek(), attr, n))
					return false;
			}
			return true;
		}
		return true;
	}
}
