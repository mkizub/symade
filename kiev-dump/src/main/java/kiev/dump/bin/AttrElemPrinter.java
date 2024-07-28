package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;

public class AttrElemPrinter extends ElemPrinter<AttrElem> {

	AttrElemPrinter(BinDumpReader reader) {
		super(reader);
	}

	String elName() { return "attr"; }
	Signature elSignature() { return Signature.TAG_ATTR_SIGN; }

	AttrElem makeInstance(int id, int addr) { return new AttrElem(id, addr); }
	Map<Integer,AttrElem> getTable() { return reader.attrTable; }

	boolean readValue(TagAndVal tav) throws DumpException
	{
		AttrElem ae = (AttrElem)el;
		if (tav.tag == Signature.TAG_TYPE_SIGN) {
			ae.vtype = (TypeElem)tav.val;
			out.printf("%sVTYPE : %4x (%s)\n", ind(), ae.vtype.id, ae.vtype.name);
			return true;
		}
		if (tav.tag == Signature.TAG_TABLE_SIGN) {
			TagAndVal sig = reader.readTagAndVal(false);
			if (sig.tag != Signature.TAG_TYPE_SIGN)
				throw new DumpException("Corrupted dump file: expected type signature at "+tav.pos);
			ae.intype = (TypeElem)sig.val;
			out.printf("%sINTYPE: %4x (%s)\n", ind(), ae.intype.id, ae.intype.name);
			return true;
		}
		if (tav.tag == Signature.TAG_STRZ_8 || tav.tag == Signature.TAG_STRZ_16) {
			String name = ((String)tav.val).intern();
			ae.name = name;
			out.printf("%sNAME  : %s\n", ind(), ae.name);
			return true;
		}
		return false;
	}
}
