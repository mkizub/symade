package kiev.dump.bin;

class TagAndVal {

	final int pos;
	final Signature tag;
	final Object val;
	
	TagAndVal(final int pos, final Signature tag, final Object val) {
		this.pos = pos;
		this.tag = tag;
		this.val = val;
	}
	public int intVal() { return ((Number)val).intValue(); }
	public long longVal() { return ((Number)val).longValue(); }
	public float floatVal() { return ((Number)val).floatValue(); }
	public double doubleVal() { return ((Number)val).doubleValue(); }
	public boolean boolVal() {
		if (val instanceof Number)
			return ((Number)val).intValue() != 0;
		else if (val instanceof Boolean)
			return ((Boolean)val).booleanValue();
		return false;
	}
	public char charVal() { return ((Character)val).charValue(); }
	public String strVal() {
		if (val == null || val instanceof String)
			return (String)val;
		return String.valueOf(val);
	}
}
