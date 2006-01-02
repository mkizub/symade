package test.a_basic;
public class test_022 {

	int i;

	public static void main(String[] args) {
	
		test_022 t = new test_022_1(1);
		Object o = t;
		System.out.println("o = "+o.i);
	
	}
	
	public test_022( int i ) { this.i = i; }

}

public class test_022_1 extends test_022 {
	public test_022_1(int i) {
		super(i);
	}
}
