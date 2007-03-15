package test.b_paramtype;

class test_006 {

  public static void main(String[] args) {
  	test_006_1<String> t = new test_006_1<String>();
  	test1(t);
  	test2(t);
  	test3(t);
  	test4(t);
  }
  
  public static void test1(Object t) {
  	if (t instanceof test_006_1<String>) {
  		System.out.println("OK: Object t instanceof test_006_1<String>");
  		return;
  	}
	System.out.println("FAILED: Object t instanceof test_006_1<String>");
  }

  public static void test2(Object t) {
  	if (t instanceof test_006_1<Float>) {
		System.out.println("FAILED: Object t instanceof test_006_1<Float>");
  		return;
  	}
	System.out.println("OK: Object t not instanceof test_006_1<Float>");
  }

  public static void test3(test_006_1<Object> t) {
  	if (t instanceof test_006_1<String>) {
  		System.out.println("OK: test_006_1<Object> t instanceof test_006_1<String>");
  		return;
  	}
	System.out.println("FAILED: test_006_1<Object> t instanceof test_006_1<String>");
  }

  public static void test4(test_006_1<Object> t) {
  	if (t instanceof test_006_1<Float>) {
		System.out.println("FAILED: test_006_1<Object> t instanceof test_006_1<Float>");
  		return;
  	}
	System.out.println("OK: test_006_1<Object> t not instanceof test_006_1<Float>");
  }

}

@unerasable
class test_006_1<A> {
}