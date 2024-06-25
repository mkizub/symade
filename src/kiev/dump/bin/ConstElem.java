package kiev.dump.bin;

public class ConstElem extends Elem {

	// type of data (type of value)
	public TypeElem	vtype;
	// value of this constant
	public Object value;
	
	public ConstElem(int id, int addr) {
		super(id, addr);
	}
	public ConstElem(int id, TypeElem vtype, Object value) {
		super(id);
		this.vtype = vtype;
		this.value = value;
	}
}
