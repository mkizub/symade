package kiev.dump.bin;

import java.util.Map;

import kiev.dump.DumpException;

public class ConstElemPrinter extends ElemPrinter<ConstElem> {

	ConstElemPrinter(BinDumpReader reader) {
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
			TypeElem te = (TypeElem)tav.val;
			out.printf("%sVTYPE: %4x (%s)\n", ind(), te.id, te.name);
			ce.vtype = te;
			return true;
		}
		if (tav.tag.is_value) {
			ce.value = tav.val;
			out.printf("%sVALUE: %s\n", ind(), ce.value);
			//if (ce.vtype.isEnum()) {
			//	try {
			//		ce.value = ce.vtype.typeinfo.clazz.getMethod("valueOf", String.class).invoke(null, ((String)ce.value).intern());
			//	} catch (Exception e) {
			//		e.printStackTrace();
			//	}
			//}
			return true;
		}
		return false;
	}
}
