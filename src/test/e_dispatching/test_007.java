package test.e_dispatching;

interface test_007_I {
	String m()
		alias n
	{
		return "I";
	}
}

class test_007_A implements test_007_I {
	public String m() {
		return "B";
	}
}

class test_007_B implements test_007_I {
}

class test_007 {

	public static void main(String[] args) {
		test_007_I a = new test_007_A();
		test_007_I b = new test_007_B();
		test(a);
		test(b);
	}
	
	private static void test(test_007_I i) {
		System.out.println(i.n());
	}
}

