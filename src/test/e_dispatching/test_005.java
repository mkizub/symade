package test.e_dispatching;

interface test_005_I<X extends CharSequence> {
	X m(X x) { return x; }
}

class test_005_A implements CharSequence, test_005_I<StringBuffer> {
	StringBuffer sb = new StringBuffer("A");
	public StringBuffer m(CharSequence cs) {
		return sb;
	}
	public char charAt(int i) { return sb.charAt(i); }
	public int length() { return sb.length(); }
	public CharSequence subSequence(int s,int e) { return sb.subSequence(s,e); }
	public String toString() { return sb.toString(); }
}

class test_005_B implements CharSequence, test_005_I<String> {
	String st = "B";
	public String m(CharSequence cs) {
		return st;
	}
	public char charAt(int i) { return st.charAt(i); }
	public int length() { return st.length(); }
	public CharSequence subSequence(int s,int e) { return st.subSequence(s,e); }
	public String toString() { return st.toString(); }
}
class test_005_C implements test_005_I<String> {
	public String toString() { return "C"; }
}

class test_005 {

	public static void main(String[] args) {
		test_005_I<StringBuffer> a = new test_005_A();
		test_005_I<String>       b = new test_005_B();
		test_005_I<String>       c = new test_005_C();
		test(a, "x");
		test(b, "x");
		test(c, "x");
	}
	
	private static void test(test_005_I<CharSequence> i, String x) {
		CharSequence cs = i.m(x);
		System.out.println(cs.charAt(0));
	}
}

