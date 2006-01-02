package test.h_wrappers;

public interface test_002_i {

	public virtual int i = 0;
	
	public int incr() {
		return ++i;
	}

	public int incr(int j) {
		return i+=j;
	}

}

public class test_002_1 implements test_002_i {

	public virtual int i;

}

public class test_002_2 implements test_002_i {

	public virtual int i;

}

public class test_002 {

	public static void main(String[] args) {
	
		test_002_1 t1 = new test_002_1();
		test_002_2 t2 = new test_002_2();
	
		System.out.println("t1.i = "+t1.i);
		System.out.println("t2.i = "+t2.i);
		System.out.println("t1.incr() = "+t1.incr());
		System.out.println("t1.i = "+t1.i);
		System.out.println("t2.i = "+t2.i);
		System.out.println("t2.incr(3) = "+t2.incr(3));
		System.out.println("t1.i = "+t1.i);
		System.out.println("t2.i = "+t2.i);
	
	}
}
