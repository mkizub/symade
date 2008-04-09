package test.a_basic;
class test_016 {

	public static void main(String[] args) {
        	for(int i=0; i < 10; i++) {
			switch(i) {
			case 1:
				System.out.println("case: "+i);
				break;
			case 2:
				System.out.println("case: "+i);
				break;
			case 3:
				System.out.println("case: "+i);
				break;
			case 4:
				System.out.println("case: "+i);
			}
			switch(i) {
			case 1:
				System.out.println("case: "+i);
				break;
			case 2:
				System.out.println("case: "+i);
				break;
			case 3:
				System.out.println("case: "+i);
				break;
			case 4:
				System.out.println("case: "+i);
				break;
			default:
				System.out.println("default: "+i);
				break;
			}
		}

		int[] arr = new int[6];
		arr[0] = 1;
		arr[1] = 89;
		arr[2] = 15;
		arr[3] = 888;
		arr[4] = 6758876;
		arr[5] = 865;
	next_j:
        	for(int j=0; j < arr.length; j++) {
			switch(arr[j]) {
			case 865:
				System.out.println("case: "+arr[j]);
				continue next_j;
			case 888:
				System.out.println("case: "+arr[j]);
				continue next_j;
			case 3:
				System.out.println("case: "+arr[j]);
				continue next_j;
			case 1:
				System.out.println("case: "+arr[j]);
				break;
			}
			switch(arr[j]) {
			case 865:
				System.out.println("case: "+arr[j]);
				continue next_j;
			case 888:
				System.out.println("case: "+arr[j]);
				continue next_j;
			case 3:
				System.out.println("case: "+arr[j]);
				continue next_j;
			case 1:
				System.out.println("case: "+arr[j]);
				continue next_j;
			default:
				System.out.println("default: "+arr[j]);
				continue next_j;
			}
		}
	}
}
