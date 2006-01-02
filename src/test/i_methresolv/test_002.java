package test.i_methresolv;

public class test_002 extends test_001 {

	public static void main(String[] args) {
		test_002 t = new test_002();
		test1(1);
		test1(1,2);
		test1(1,2,'h');
		test1(1,2,true);
	
		test2("hi!");
		test2("hi!",2.f);
		test2("hi!",2.D,"hello");
		test2("hi!",2,(short)8,(long)1438856896);
	
		t.test3(2);
		t.test3(2,2.d);
		t.test3(2,2.f,"hello");
		t.test3(2,(double)2,"hello"," world");
	
		t.test4("hi from instance!");
		t.test4("hi from instance!",2);
		t.test4("hi from instance!",297125607590645093L);
		t.test4("hi from instance!",2,"hello"," world");
	
	}
	
}