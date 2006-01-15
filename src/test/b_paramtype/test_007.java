package test.b_paramtype;

class test_007 {

  public static void main(String[] args) {
  	test_007_1<String> t1 = new test_007_1<String>();
  	Object[] arr = t1.toArray();
  	if (arr instanceof String[])
  		System.out.println("OK");
  	else
		System.out.println("FAILED: "+arr.getClass());
	test_007_2<String> t2 = t1.toInst();
  	arr = t2.toArray();
  	if (arr instanceof String[])
  		System.out.println("OK");
  	else
		System.out.println("FAILED: "+arr.getClass());
  }
}

@unerasable
class test_007_1<A> {
	A[] toArray() { return new A[0]; }
	test_007_2<A> toInst() { return new test_007_2<A>(); }
}

@unerasable
class test_007_2<A> {
	A[] toArray() { return new A[0]; }
}
