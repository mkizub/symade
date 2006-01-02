package test.a_basic;
class test_013 {
	public static void main(String[] args) {
		System.out.println("Hello"+" "+"world!");
		String[] msg = new String[5];
		msg[0] = "Hello";
		msg[1] = " ";
		msg[2] = "world";
		msg[3] = " ";
		msg[4] = "again!";
		StringBuffer sb = new StringBuffer(100);
		for(int i=0; i < msg.length; i=i+1) {
			sb.append(msg[i]);
		}
		for(int i=0; i < 5; i=i+1) {
			System.out.println(i+": "+sb);
		}
	}
}