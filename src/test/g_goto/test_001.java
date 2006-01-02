package test.g_goto;
class test_001 {

	public static void main(String[] args) {
	
	int i = 0;
	goto disp;
	
	l0:;
		System.out.println("goto l0");
		goto disp;
	
	l1:;
		System.out.println("goto l1");
		goto disp;
	
		{
	l2:;
			System.out.println("goto l2");
			goto disp;
		}
	
		try {
	l3:;
			System.out.println("goto l3");
			goto disp;
		} finally {
			System.out.println("finally after goto l3");
		}
	
		boolean fin_back = true;
		try {
	l4:;
			System.out.println("goto l4");
			goto disp;
	l5:;
			System.out.println("goto l5");
		} finally {
			System.out.println("finally after goto l4/l5");
			if( fin_back ) {
				fin_back = false;
				goto l5;
			}
		}
	
	disp:
		switch(i++) {
		case 0: goto l0;
		case 1: goto l1;
		case 2: goto l2;
		case 3: goto l3;
		case 4: goto l4;
		}
		
		System.out.println("bye");
	
	}
}
