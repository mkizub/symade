package test.a_basic;
class test_015 {
	public static void main(String[] args) {
		int i = 0;
		long l = 0L;
		float f = 0.f;
		double d = 0.D;

		System.out.println("i := "+i);
		System.out.println("++i := "+ ++i);
		System.out.println("i++ := "+ i++);
		System.out.println("i := "+i);
		System.out.println("--i := "+ --i);
		System.out.println("i-- := "+ i--);
		System.out.println("i := "+i);

		System.out.println("~1 := "+ ~1);

		i = 10;
		System.out.println("-10 := "+ -i);

		if( !false ) System.out.println("Ok !false := "+ !false);
		else System.out.println("Err !false := "+ !false);

		System.out.println("Done.");
	}
}