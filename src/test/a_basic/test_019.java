package test.a_basic;

class test_019_parent {
	String str;
	test_019_parent(String str) {
		this.str = str;
	}
}

class test_019 extends test_019_parent {
	public static void main(String[] args) {
		test_019 test0 = new test_019("Hello world!");
		test_019 test5 = new test_019("Hello world!",5);
		System.out.println(test0.toString());
		System.out.println(test5.toString());
	}
	test_019(String str) {
		super("Test19: "+str);
	}
	test_019(String str, int n) {
		this(str);
		for(int i=0; i < n; i++)
			this.str = i+":"+this.str;
	}
	public String toString() { return str; }
}