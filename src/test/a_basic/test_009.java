package test.a_basic;
class test_009 {
	public static void main(String[] args) {
		test_009 test = new test_009();

		System.out.println(test.return_boolean());
		System.out.println(test.return_byte());
		System.out.println(test.return_short());
		System.out.println(test.return_int());
		System.out.println(test.return_long());
		System.out.println(test.return_float());
		System.out.println(test.return_double());
		System.out.println(test.return_object());

		test.test_all();
	}

	boolean return_boolean() {
		return 1==1;
	}

	byte return_byte() {
		return (byte)1;
	}

	short return_short() {
		return (short)2;
	}

	int return_int() {
		return 3;
	}

	long return_long() {
		return 4L;
	}

	float return_float() {
		return 5.f;
	}

	double return_double() {
		return 6.D;
	}

	String return_object() {
		return toString();
	}

	public String toString() {
		return "It's me, an object";
	}

	void test_all() {
		System.out.println("Testing again...");
		System.out.println(return_boolean());
		System.out.println(return_byte());
		System.out.println(return_short());
		System.out.println(return_int());
		System.out.println(return_long());
		System.out.println(return_float());
		System.out.println(return_double());
		System.out.println(return_object());
	}
}
