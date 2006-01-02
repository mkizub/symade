package test.b_paramtype;

class test_002 {

  public static void main(String[] args) {
  	test_002_1<String> t = new test_002_1<String>("hello");
		System.out.println(t.toString());
  }

}

class test_002_1<A> {

  A a;
  
  public test_002_1(A a) {
  	this.a = a;
  }

  A test(int i, A j) {
	return j;
  }
  
  public String toString() {
  	return this.getClass()+":"+a;
  }

}