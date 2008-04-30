package test.i_methresolv;

public class test_001 {

	public static void main(String[] args) {
		test_001 t = new test_001();
		test1(1);
		test1(1,new Integer(2));
		test1(1,new Integer(2),"hello");
		test1(1,new Integer(2),"hello"," world");
	
		test2("hi!");
		test2("hi!",new Integer(2));
		test2("hi!",new Integer(2),"hello");
		test2("hi!",new Integer(2),"hello"," world");
	
		t.test3(2);
		t.test3(2,new Integer(2));
		t.test3(2,new Integer(2),"hello");
		t.test3(2,new Integer(2),"hello"," world");
	
		t.test4("hi from instance!");
		t.test4("hi from instance!",new Integer(2));
		t.test4("hi from instance!",new Integer(2),"hello");
		t.test4("hi from instance!",new Integer(2),"hello"," world");
	
	}
	
	public static void test1(int i, Object... va_args) {
		System.out.println("test(int,...) is called: i = "+i);
		if( va_args != null ) {
			for(int j=0; j < va_args.length; j++) {
				System.out.println("\t"+va_args[j].getClass()+"\t"+va_args[j]);
			}
		}
	}

	public static void test2(String s, Object... va_args) {
		System.out.println("test(int,...) is called: s = "+s);
		if( va_args != null ) {
			for(int j=0; j < va_args.length; j++) {
				System.out.println("\t"+va_args[j].getClass()+"\t"+va_args[j]);
			}
		}
	}

	public void test3(int i, Object... va_args) {
		System.out.println("test(int,...) is called: i = "+i);
		if( va_args != null ) {
			for(int j=0; j < va_args.length; j++) {
				System.out.println("\t"+va_args[j].getClass()+"\t"+va_args[j]);
			}
		}
	}

	public void test4(String s, Object... va_args) {
		System.out.println("test(int,...) is called: s = "+s);
		if( va_args != null ) {
			for(int j=0; j < va_args.length; j++) {
				System.out.println("\t"+va_args[j].getClass()+"\t"+va_args[j]);
			}
		}
	}

}
