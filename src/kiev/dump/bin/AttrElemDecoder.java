package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;

public class AttrElemDecoder extends ElemDecoder<AttrElem> {

	AttrElemDecoder(BinDumpReader reader) {
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
			return true;
		}
		if (tav.tag == Signature.TAG_TABLE_SIGN) {
			TagAndVal sig = reader.readTagAndVal(true);
			if (sig.tag != Signature.TAG_TYPE_SIGN)
				throw new DumpException("Corrupted dump file: expected type signature at "+tav.pos);
			ae.intype = (TypeElem)sig.val;
			return true;
		}
		if (tav.tag == Signature.TAG_STRZ_8 || tav.tag == Signature.TAG_STRZ_16) {
			String name = ((String)tav.val).intern();
			ae.name = name;
			return true;
		}
		return false;
	}
}
