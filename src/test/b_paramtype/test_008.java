package test.b_paramtype;

class test_008 {

  public static void main(String[] args) {
  	test_008_1<String> t1 = new test_008_1<String>();
	test_008_2<Float> t2 = t1.toInst<Float>();
  	Object arr = t2.toArray();
  	if (arr instanceof Float[])
  		System.out.println("OK");
  	else
		System.out.println("FAILED: "+arr.getClass());
  }
}

@unerasable
class test_008_1<A> {
	@unerasable
	<B> test_008_2<B> toInst() { return new test_008_2<B>(); }
}

@unerasable
class test_008_2<A> {
	A[] toArray() { return new A[0]; }
}
