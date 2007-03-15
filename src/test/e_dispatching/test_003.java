package test.e_dispatching;

interface test_003_I<X> {
	X m();
}

class test_003_A implements test_003_I<CharSequence> {
	public StringBuffer m() {
		return new StringBuffer("A");
	}
}

class test_003_B implements test_003_I<CharSequence> {
	public String m() {
		return new String("B");
	}
}

class test_003 {

	public static void main(String[] args) {
		test_003_I<CharSequence> a = new test_003_A();
		test_003_I<CharSequence> b = new test_003_B();
		test(a);
		test(b);
	}
	
	private static void test(test_003_I<CharSequence> cs) {
		System.out.println(cs.m());
	}
}

