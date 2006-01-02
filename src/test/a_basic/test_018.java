package test.a_basic;
class test_018 {
	public static void main(String args[]) {
		String s = null;
		System.out.println("Assign s = null");
		if( s == null ) {
			System.out.println("s == null is TRUE (OK)");
		} else {
			System.out.println("s == null is FLASE (Error)");
		}
		System.out.println("Assign s = \"Hello\"");
		s = "Hello";
		if( s != null ) {
			System.out.println("s != null is TRUE (OK)");
		} else {
			System.out.println("s != null is FLASE (Error)");
		}
		System.out.println("Done.");
	}
}