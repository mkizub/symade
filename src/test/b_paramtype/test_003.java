package test.b_paramtype;

class test_003 {
	public static String def = "Hello world";
	public static void main(String[] args) {
		test_003_2 argS;
		test_003_1<Object> argO;
		argS = new test_003_2(def);
		argO = argS;
		System.out.println(argS.makeString("string"));
		System.out.println(argO.makeString("string"));
	}
}

class test_003_1<A extends Object> {
	A a;
	test_003_1(A aa) {
		a = aa;
	}
	public String makeString(A arg) {
		return "makeString(A): "+a;
	}
}

class test_003_2 extends test_003_1<String> {
	test_003_2(String aa) {
		super(aa);
	}
	public String makeString(String arg) {
		return "makeString(String): "+(a.concat(" - string"));
	}
}
