package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;

public class ConstElemDecoder extends ElemDecoder<ConstElem> {

	ConstElemDecoder(BinDumpReader reader) {
		super(reader);
	}

	String elName() { return "const"; }
	Signature elSignature() { return Signature.TAG_CONST_SIGN; }

	ConstElem makeInstance(int id, int addr) { return new ConstElem(id, addr); }
	Map<Integer,ConstElem> getTable() { return reader.constTable; }

	@SuppressWarnings({"unchecked"})
	boolean readValue(TagAndVal tav) throws DumpException
	{
		ConstElem ce = (ConstElem)el;
		if (tav.tag == Signature.TAG_TYPE_SIGN) {
			ce.vtype = (TypeElem)tav.val;
			return true;
		}
		if (tav.tag.is_value) {
			ce.value = tav.val;
			if (ce.vtype.isEnum()) {
				try {
					ce.value = ce.vtype.typeinfo.clazz.getMethod("valueOf", String.class).invoke(null, ((String)ce.value).intern());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}
}
