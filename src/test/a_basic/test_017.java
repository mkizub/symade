package test.a_basic;
class test_017 {
	public static void main(String[] args) {
		try {
			args[0] = "Hello world!";
		} catch( ArrayIndexOutOfBoundsException e) {
			System.out.println("ArrayIndexOutOfBoundsException catch block caught, printing stack trace...");
			e.printStackTrace();
			args = new String[1];
			args[0] = "No arguments was entered";
		} finally {
			System.out.println("finally block done.");
		}
		System.out.println("Result args[0] is "+args[0]);
	}
}