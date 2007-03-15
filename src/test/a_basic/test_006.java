package test.a_basic;
class test_006 {
	public static void main(String[] args) {
		test_006 test = new test_006();
		print_static();
		test.print();
		test.print_private();
	}

	test_006() {
		System.out.println("Instance Constructor");
	}

	static void print_static() {
		System.out.println("Static method");
	}

	void print() {
		System.out.println("Virtual method");
	}

	private void print_private() {
		System.out.println("Virtual private method");
	}

}