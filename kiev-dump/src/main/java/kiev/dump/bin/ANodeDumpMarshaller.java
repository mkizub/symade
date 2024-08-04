package kiev.dump.bin;

import java.util.Enumeration;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vlang.MetaFlag;
import kiev.vtree.INode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.ExtSpaceIterator;
import kiev.vtree.NameAndUUID;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.Symbol;
import kiev.vtree.SymbolRef;

public class ANodeDumpMarshaller implements Marshaller {

    /**
     * Determines whether the converter can marshall a particular object.
     * @param data The object to be marshalled.
     */
    public boolean canMarshal(Object data, MarshallingContext context) {
		return data instanceof INode;
    }

    /**
     * Marshal an object to XML data.
     *
     * @param data    The object to be marshalled.
     * @param writer  A stream to write to.
     * @param context A context that allows nested objects to be processed.
     */
    public void marshal(Object data, DumpWriter writer, MarshallingContext _context) throws Exception
    {
    	BinDumpWriter out = (BinDumpWriter)writer;
    	DumpMarshallingContext context = (DumpMarshallingContext)_context;
		INode node = (INode)data;

		NodeElem nelem = context.getNodeElem(node);
		nelem.saddr = context.writer.getStreamPos();
		out.startBlock(Signature.TAG_NODE_SIGN);
		out.writeElemID(nelem);
		out.writeElemFlags(nelem);
		out.writeTypeRef(nelem.tp);

		for (AttrElem ae : nelem.tp.attrs) {
			AttrSlot attr = ae.getAttrSlot();
			if (context.filter.ignoreAttr(node, attr)) {
				out.writeVoid();
				continue;
			}
			if (attr instanceof ScalarAttrSlot) {
				Object obj = ((ScalarAttrSlot)attr).get(node);
				marshal_attr(ae, obj, out, context);
				continue;
			}
			if (attr instanceof ASpaceAttrSlot) {
				out.writeSpaceStart();
				Enumeration en = ((ASpaceAttrSlot)attr).iterate(node);
				while (en.hasMoreElements()) {
					INode n = (INode)en.nextElement();
					if (!context.filter.ignoreNode(node, attr, n))
						marshal_attr(ae, n, out, context);
				}
				out.writeSpaceEnd();
			}
		}

		//ExtSpaceIterator en = node.asANode().getExtSpaceIterator(null);
		//if (en.hasMoreElements()) {
		//	out.writeExtDataStart();
		//	while (en.hasMoreElements()) {
		//		AttrSlot attr = en.nextAttrSlot();
		//		INode n = en.nextElement();
		//		if (!attr.isAttr() || !attr.isExternal())
		//			continue;
		//		if (context.filter.ignoreAttr(node, attr))
		//			continue;
		//		AttrElem ae = context.getAttrElem(attr);
		//		out.writeAttrRef(ae);
		//		marshal_attr(ae, n, out, context);
		//	}
		//	out.writeExtDataEnd();
		//}
		int lineno = node.asANode().getLineNo();
		if (lineno > 0) {
			if (lineno > 0xFFFF) {
				out.writeValueTag(Signature.TAG_LONG);
				out.writeValueTag(Signature.TAG_LINENO);
				out.writeInt32(lineno);
			} else {
				out.writeValueTag(Signature.TAG_LINENO);
				out.writeInt16(lineno);
			}
		}
		out.endBlock(Signature.TAG_NODE_SIGN);
		nelem.eaddr = context.writer.getStreamPos();
    }

    private void marshal_attr(AttrElem ae, Object obj, BinDumpWriter out, DumpMarshallingContext context) throws Exception {
		if (obj == null) {
			out.writeNull();
			return;
		}
		if (ae.vtype == TypeElem.teSYMREF) {
			if (obj instanceof Symbol) {
				SymbElem se = context.getSymbolElem((Symbol)obj);
				if (se == null)
					out.writeVoid();
				else
					out.writeSymbolRef(se, true);
				return;
			}
		}
		if (ae.vtype == TypeElem.teSYMBOL) {
			SymbElem se = context.getSymbolElem((Symbol)obj);
			if (se == null)
				out.writeVoid();
			else
				out.writeSymbolRef(se, !ae.isChild());
			return;
		}
    	if (obj instanceof SymbolRef) {
			SymbolRef ref = (SymbolRef)obj;
			Object val = ref.getVal(ref.getAttrSlot("ident_or_symbol_or_type"));
			if (val instanceof Symbol) {
				Symbol sym = (Symbol)val;
				SymbElem se = context.getSymbolElem(sym);
				if (se == null)
					out.writeVoid();
				else
					out.writeSymbolRef(se, true);
	    		return;
			}
			if (val instanceof NameAndUUID) {
				NameAndUUID n = (NameAndUUID)val;
				SymbElem se = context.getSymbolElem(n);
				if (se == null)
					out.writeVoid();
				else
					out.writeSymbolRef(se, true);
	    		return;
			}
			String name = (String)val;
			if (val == null)
				out.writeNull();
			else
				out.writeString(name);
    		return;
    	}
    	if (obj instanceof Symbol) {
			SymbElem se = context.getSymbolElem((Symbol)obj);
			if (se == null)
				out.writeVoid();
			else
				out.writeSymbolRef(se, true);
    		return;
    	}
		if (!ae.isChild() && obj instanceof INode) {
			NodeElem ne = context.getNodeElem(((INode)obj).asANode());
			if (ne == null || ne.id == 0)
				out.writeVoid();
			else
				out.writeNodeRef(ne, true);
			return;
		}
		if (obj instanceof MetaFlag) {
			NodeElem ne = context.getNodeElem((MetaFlag)obj);
			out.writeNodeRef(ne, true);
			return;
		}
    	if (obj instanceof INode) {
    		this.marshal(obj, out, context);
    		return;
    	}
    	new DataMarshaller().marshal(obj, out, context);
    }

}
