package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;
import kiev.stdlib.TypeInfo;

public class TypeElemDecoder extends ElemDecoder<TypeElem> {

	TypeElemDecoder(BinDumpReader reader) {
		super(reader);
	}

	String elName() { return "type"; }
	Signature elSignature() { return Signature.TAG_TYPE_SIGN; }

	TypeElem makeInstance(int id, int addr) { return new TypeElem(id, addr); }
	Map<Integer,TypeElem> getTable() { return reader.typeTable; }

	boolean readValue(TagAndVal tav) throws DumpException
	{
		TypeElem te = (TypeElem)el;
		if (tav.tag == Signature.TAG_TYPE_SIGN) {
			TypeElem sup = (TypeElem)tav.val;
			te.super_types = (TypeElem[])kiev.stdlib.Arrays.append(te.super_types, sup);
			return true;
		}
		if (tav.tag == Signature.TAG_ATTR_SIGN) {
			AttrElem ae = (AttrElem)tav.val;
			te.attrs = (AttrElem[])kiev.stdlib.Arrays.append(te.attrs, ae);
			return true;
		}
		if (tav.tag == Signature.TAG_CONST_SIGN) {
			ConstElem ce = (ConstElem)tav.val;
			te.consts = (ConstElem[])kiev.stdlib.Arrays.append(te.consts, ce);
			return true;
		}
		if (tav.tag == Signature.TAG_STRZ_8 || tav.tag == Signature.TAG_STRZ_16) {
			te.name = ((String)tav.val).intern();
			te.typeinfo = TypeInfo.newTypeInfo(te.name.replace('·', '.'));
			return true;
		}
		return false;
	}
}
