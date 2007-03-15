package test.e_dispatching;

interface test_004_I {
	String m() {
		return "I";
	}
}

class test_004_A implements test_004_I {
	public String m() {
		return "B";
	}
}

class test_004_B implements test_004_I {
}

class test_004 {

	public static void main(String[] args) {
		test_004_I a = new test_004_A();
		test_004_I b = new test_004_B();
		test(a);
		test(b);
	}
	
	private static void test(test_004_I i) {
		System.out.println(i.m());
	}
}

