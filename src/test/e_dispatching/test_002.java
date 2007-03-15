package test.e_dispatching;

import java.lang.reflect.*;

class test_002_A {

	public Object m() {
		return "A";
	}

}

class test_002_B extends test_002_A {

	public String m() {
		return "B";
	}

}

class test_002_C extends test_002_B {
	// nothing overloaded
}

class test_002 {

	public static void main(String[] args) {
		test_002_C c = new test_002_C();
		System.out.println(c.m()); // test overloaded call
		test(c);
		foreach (Method m; test_002_C.class.getMethods()) {
			if (m.getDeclaringClass() == test_002_C.class)
				System.out.println("FAILED non-gen: "+m);
				
		}
		foreach (Method m; test_002_B.class.getMethods(); m.getName().equals("m")) {
			if (m.getDeclaringClass() != test_002_B.class)
				System.out.println("FAILED gen: "+m);
			else
				System.out.println("OK gen: "+m);				
		}
	}
	
	private static void test(test_002_A a) {
		System.out.println(a.m());
	}
}

