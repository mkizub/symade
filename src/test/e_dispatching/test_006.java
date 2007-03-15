package test.e_dispatching;

class test_006_A<A> {
	public A m(A a) {
		return a;
	}
}

class test_006_B<B> extends test_006_A<B> {
	public B m(B b) {
		return super.m(b);
	}
}

class test_006_C extends test_006_B<String> {
	public String m(String c) {
		return "C";
	}
}

class test_006 {

	public static void main(String[] args) {
		test_006_A<String> a = new test_006_A<String>();
		test_006_B<String> b = new test_006_B<String>();
		test_006_C         c = new test_006_C();
		test(a);
		test(b);
		test(c);
	}
	
	private static void test(test_006_A<String> a) {
		System.out.println(a.m("x"));
	}
}

