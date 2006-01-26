package test.d_closures;

public class test_002 {

	long l;

	public static void main(String[] args) {
		test_002 tst = new test_002(40L);
		System.out.println(test_002.test1(10));
		System.out.println(tst.test2(8));
		System.out.println(tst.test3());
	}
	
	public test_002(long l) {
		this.l = l;
	}
	
	public String toString() { return "test_002("+l+")"; }

	public static String test1(int i) {
		float f = 2.2f;
		()->String c = fun ()->String { return String.valueOf(i+f); };
		return c();
	}
	
	public String test2(int i) {
		()->String c = fun ()->String { return String.valueOf(i+l); };
		return c();
	}

	public String test3() {
		()->String c = fun ()->String { return String.valueOf(this); };
		return c();
	}


}
