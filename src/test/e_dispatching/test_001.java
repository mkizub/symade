package test.e_dispatching;

class test_001_A {

	void m() {
		System.out.println("A");
	}

}

class test_001_B extends test_001_A {

	void m() {
		System.out.println("B");
	}

	void s() {
		super.m();
	}
}

class test_001 {

	public static void main(String[] args) {
		test_001_B b = new test_001_B();
		b.m(); // test virtual call
		b.s(); // test super-call
	}
}

