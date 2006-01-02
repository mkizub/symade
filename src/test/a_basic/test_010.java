package test.a_basic;
class test_010 {
	public static void main(String[] args) {
		test_010 test = new test_010();

		System.out.println(test.return_boolean(true));
		System.out.println(test.return_byte((byte)1));
		System.out.println(test.return_char('c'));
		System.out.println(test.return_short((short)2));
		System.out.println(test.return_int(3));
		System.out.println(test.return_long(4L));
		System.out.println(test.return_float(5.F));
		System.out.println(test.return_double(6.D));
		System.out.println(test.return_object(new test_010()));

		test.test_all();
	}

	boolean return_boolean(boolean val) {
		return val;
	}

	byte return_byte(byte val) {
		return val;
	}

	char return_char(char val) {
		return val;
	}

	short return_short(short val) {
		return val;
	}

	int return_int(int val) {
		return val;
	}

	long return_long(long val) {
		return val;
	}

	float return_float(float val) {
		return val;
	}

	double return_double(double val) {
		return val;
	}

	String return_object(test_010 val) {
		return val.toString();
	}

	public String toString() {
		return "It's me, an object";
	}

	void test_all() {
		System.out.println("Testing again...");
		System.out.println(return_boolean(true));
		System.out.println(return_byte((byte)1));
		System.out.println(return_char('c'));
		System.out.println(return_short((short)2));
		System.out.println(return_int(3));
		System.out.println(return_long(4L));
		System.out.println(return_float(5.F));
		System.out.println(return_double(6.D));
		System.out.println(return_object(new test_010()));
	}
}
