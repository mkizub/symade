package test.b_paramtype;

class test_004 {
	public static void main(String[] args) {
		test_004_1<String> arg = new test_004_1<String>(args[0]);
		String str = arg.test();
		System.out.println(arg+" containce "+str);
		str = arg.a;
		System.out.println(arg.a+" containce "+str);
		test_004_1<Integer> argi = new test_004_1<Integer>(Integer.valueOf(args[0]));
		Integer i = argi.test();
		System.out.println(argi+" containce "+i);
		i = argi.a;
		System.out.println(argi.a+" containce "+i);
	}
}

class test_004_1<A extends Object> {
	A a;
	test_004_1(A aa) {
		a = aa;
	}

	A test() { return a; }

	public String toString() {
		return a.toString()+"->"+a.getClass();
	}
}

