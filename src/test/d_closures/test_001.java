package test.d_closures;

public class test_001 {

	public (int,long)->long add;
	public (long)->long incr;

	public static void main(String[] args) {
		test_001 tst = new test_001();
		tst.test();
	}

	public void test() {
		add = fun (int i,long l)->long { return l+i; };
		incr = add(1);
		long n = 100L;
		n = incr(n);
		System.out.println("incr(100L) -> "+n);
	}


}
