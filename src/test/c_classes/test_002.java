package test.c_classes;

public class test_002 {

	public case case1;
	public case case2();
	public case case3(int i);
	public case case4(double d);
	public case case5(Object o);
	public case case6(Long j);
	public case case7(String s);
	public case case8() {
		System.out.println("case8");
	}
	public case case9(String s) {
		System.out.println("case9: "+s);
	}

	public static void main(String[] args) {
		test_002[] tests = new test_002[] {
			    case1,
			new case2(),
			new case3(1),
			new case4(2.D),
			new case5("Object"),
			new case6(new Long(3L)),
			new case7("String"),
			new case8(),
			new case9("Hello World")
		};
		for(int i=0; i < tests.length; i++) {
			System.out.println("Selecting case "+tests[i].getClass());
			switch(tests[i]) {
			case case1:
				System.out.println("case selected: case1");
				break;
			case case2():
				System.out.println("case selected: case2");
				break;
			case case3(int ii):
				System.out.println("case selected: case3 = "+ii);
				break;
			case case4(double dd):
				System.out.println("case selected: case4 = "+dd);
				break;
			case case5(Object obj):
				System.out.println("case selected: case5 = "+obj);
				break;
			case case6(Long LL):
				System.out.println("case selected: case6 = "+LL);
				break;
			case case7(String str):
				System.out.println("case selected: case7 = "+str);
				break;
			case case8():
				System.out.println("case selected: case8");
				break;
			case case9(String hello):
				System.out.println("case selected: case9 = \""+hello+'\"');
				break;
			default:
				System.out.println("default selected - ERROR!");
			}
		}
	}

}
