package test.h_wrappers;

// Test virtual fields

class test_001 {

	@virtual int f1;
	
	abstract @virtual int f2;
	
	public static void main(String[] args) {
		System.out.println("Testing first class");
		test_001 t = new test_001();
		t.test();
		System.out.println("Testing child class");
		t = new test_001_1();
		t.test();
	}

	void set$f2(int i) { f1 += i; }
	int get$f2() { return f1 + 1; }
	
	void set$f3(int i) { f1 += i; }
	int get$f3() { return f1 + 1; }
	
	

	void test() {
	
		int i;
		
		f1 = 1;
		System.out.println("set f1 = 1");
		i = f1;
		System.out.println("get f1 -> "+i);
		
		f2 = 1;
		System.out.println("set f2 = 1, is f1+=1");
		i = this.f2;
		System.out.println("get f2, is f1+1 -> "+i+" ( = 3)");
		
		this.f3 = 1;
		System.out.println("set f3 = 1, is f1+=1");
		i = f3;
		System.out.println("get f3, is f1+1 -> "+i+" ( = 4)");

		i = f1;
		System.out.println("get f1 -> "+i+" ( = 3)");
		
	}
}

class test_001_1 extends test_001 {

	int f2;
	int f3;

	void set$f1(int i) { f1 = i+1; }
	int get$f1() { return f1; }
	
	void set$f2(int i) { f2 = i; }
	int get$f2() { return f2; }
	
	void set$f3(int i) { f3 = i; }
	int get$f3() { return f3; }
	
	
	void test() {
	
		int i;
		
		f1 = 1;
		System.out.println("set f1 = 1, is f1 = 1 + 1");
		i = f1;
		System.out.println("get f1 -> "+i+" ( = 2)");
		
		f2 = 1;
		System.out.println("set f2 = 1, is f2=1");
		i = this.f2;
		System.out.println("get f2, is f2 -> "+i+" ( = 1)");
		
		this.f3 = 1;
		System.out.println("set f3 = 1, is f3=1");
		i = f3;
		System.out.println("get f3, is f3 -> "+i+" ( = 1)");

		i = f1;
		System.out.println("get f1 -> "+i+" ( = 2)");
		
	}
}
