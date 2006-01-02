package test.a_basic;
class test_012 {
	public static void main(String[] args) {
        System.out.println("Hello"+" "+"world!");
		String[] msg = new String[5];
		msg[0] = "Hello";
		msg[1] = " ";
		msg[2] = "world";
		msg[3] = " ";
		msg[4] = "again!";
		StringBuffer sb = new StringBuffer(100);
		int i=0;
		while(i < msg.length) {
			sb.append(msg[i]);
			i = i + 1;
		}
		i=0;
		while(i < 10) {
			System.out.println(i+": "+sb);
			i = i + 1;
		}
	}
}