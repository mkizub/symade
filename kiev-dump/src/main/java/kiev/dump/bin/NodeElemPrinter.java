package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;
import kiev.vtree.INode;

public class NodeElemPrinter extends ElemPrinter<NodeElem> {

	private int attr_pos;

	NodeElemPrinter(BinDumpReader reader) {
		super(reader);
	}

	String elName() { return "node"; }
	Signature elSignature() { return Signature.TAG_NODE_SIGN; }

	NodeElem makeInstance(int id, int addr) { return new NodeElem(id, addr); }
	Map<Integer,NodeElem> getTable() { return reader.nodeTable; }

	boolean readValue(TagAndVal tav) throws DumpException
	{
		NodeElem ne = (NodeElem)el;
		TypeElem te = ne.tp;
		if (tav.tag == Signature.TAG_TYPE_SIGN) {
			if (te != null)
				throw new DumpException("Corrupted dump file: duplicated node type at "+tav.pos);
			te = (TypeElem)tav.val;
			ne.tp = te;
			out.printf("%sTYPE: %4x (%s)\n", ind(), te.id, te.name);
			return true;
		}
		if (tav.tag == Signature.TAG_LINENO) {
			out.printf("%sLINE: %4x (%d)\n", ind(), tav.intVal(), tav.intVal());
			return true;
		}
		if (tav.tag == Signature.TAG_VOID) {
			AttrElem ae = te.attrs[attr_pos];
			out.printf("%s%s: void\n", ind(), ae.name, ae.id);
			attr_pos++;
			return true;
		}
		if (tav.tag == Signature.TAG_SPACE_START) {
			AttrElem ae = te.attrs[attr_pos];
			out.printf("%s%s: {\n", ind(), ae.name);
			++reader.indent;
			while ( (tav=reader.readTagAndVal(false)).tag != Signature.TAG_SPACE_END) {
				if (tav.tag == Signature.TAG_NODE_SIGN) {
				}
				else if (tav.tag == Signature.TAG_NODE_REF) {
					NodeElem nr = (NodeElem)tav.val;
					out.printf("%snref %4x\n", ind(), nr.id);
				}
				else if (tav.tag == Signature.TAG_SYMB_SIGN) {
					SymbElem se = (SymbElem)tav.val;
					out.printf("%ssymb %4x (%s)\n", ind(), se.id, se.name);
				}
				else if (tav.tag == Signature.TAG_SYMB_REF) {
					SymbElem se = (SymbElem)tav.val;
					out.printf("%ssref %4x (%s)\n", ind(), se.id, se.name);
				}
				else if (tav.tag == Signature.TAG_VOID) {
					out.printf("%svoid\n", ind());
				}
				else
					throw new DumpException("Corrupted dump file: unexpected value tag '"+tav.tag.sign+"' in node space at "+tav.pos);
			}
			--reader.indent;
			out.printf("%s}\n", ind());
			attr_pos++;
			return true;
		}
		if (tav.tag == Signature.TAG_EXT_START) {
			out.printf("%s[\n", ind());
			++reader.indent;
			for (;;) {
				TagAndVal a = reader.readTagAndVal(false);
				if (a.tag == Signature.TAG_EXT_END)
					break;
				if (a.tag != Signature.TAG_ATTR_SIGN)
					throw new DumpException("Corrupted dump file: unexpected tag '"+tav.tag.sign+"' in value map at "+tav.pos);
				AttrElem ae = (AttrElem)tav.val;
				TagAndVal v = reader.readTagAndVal(false);
				printValue(ne, ne.node, ae, v);
			}
			--reader.indent;
			out.printf("%s]\n", ind());
			return true;
		}
		// otherwice set it as an attribute value
		printValue(ne, ne.node, te.attrs[attr_pos], tav);
		attr_pos += 1;
		return true;
	}
	
	private void printValue(NodeElem ne, INode node, AttrElem ae, TagAndVal tav) throws DumpException {
		if (tav.tag == Signature.TAG_NULL) {
			out.printf("%s%s: null\n", ind(), ae.name);
			return;
		}
		if (tav.tag == Signature.TAG_NODE_SIGN) {
			out.printf("%s%s: node \n", ind(), ae.name);
			return;
		}
		if (tav.tag == Signature.TAG_NODE_REF) {
			NodeElem nr = (NodeElem)tav.val;
			out.printf("%s%s: nref %4x\n", ind(), ae.name, nr.id);
			return;
		}
		if (tav.tag == Signature.TAG_SYMB_SIGN) {
			SymbElem se = (SymbElem)tav.val;
			out.printf("%s%s: symb %4x (%s)\n", ind(), ae.name, se.id, se.name);
			return;
		}
		if (tav.tag == Signature.TAG_SYMB_REF) {
			SymbElem se = (SymbElem)tav.val;
			out.printf("%s%s: sref %4x (%s)\n", ind(), ae.name, se.id, se.name);
			return;
		}
		if (tav.tag.is_value) {
			out.printf("%s%s: %s\n", ind(), ae.name, tav.val);
			return;
		}
		if (tav.tag == Signature.TAG_CONST_SIGN) {
			ConstElem ce = (ConstElem)tav.val;
			out.printf("%s%s: const %4x (%s)\n", ind(), ae.name, ce.id, ce.value);
			return;
		}
		throw new DumpException("Corrupted dump file: unexpected signature '"+tav.tag.sign+"' at "+tav.pos);
	}
	
}
