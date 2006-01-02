package test.a_basic;
class test_008 {
	public static void main(String[] args) {
		int one = 1;
		int mone = -1;
		int zero = 0;

		if( zero == 0 )
			System.out.println("0==0 is true\tOK");
		else
			System.out.println("0==0 is false\tERROR");
		if( zero != 0 )
			System.out.println("0!=0 is true\tERROR");
		else
			System.out.println("0!=0 is false\tOK");
		if( zero > 0 )
			System.out.println("0>0 is true\tERROR");
		else
			System.out.println("0>0 is false\tOK");
		if( zero >= 0 )
			System.out.println("0>=0 is true\tOK");
		else
			System.out.println("0>=0 is false\tERROR");
		if( zero < 0 )
			System.out.println("0<0 is true\tERROR");
		else
			System.out.println("0<0 is false\tOK");
		if( zero <= 0 )
			System.out.println("0<=0 is true\tOK");
		else
			System.out.println("0<=0 is false\tERROR");

		if( one == 0 )
			System.out.println("1==0 is true\tERROR");
		else
			System.out.println("1==0 is false\tOK");
		if( one != 0 )
			System.out.println("1!=0 is true\tOK");
		else
			System.out.println("1!=0 is false\tERROR");
		if( one > 0 )
			System.out.println("1>0 is true\tOK");
		else
			System.out.println("1>0 is false\tERROR");
		if( one >= 0 )
			System.out.println("1>=0 is true\tOK");
		else
			System.out.println("1>=0 is false\tERROR");
		if( one < 0 )
			System.out.println("1<0 is true\tERROR");
		else
			System.out.println("1<0 is false\tOK");
		if( one <= 0 )
			System.out.println("1<=0 is true\tERROR");
		else
			System.out.println("1<=0 is false\tOK");

		if( mone == 0 )
			System.out.println("-1==0 is true\tERROR");
		else
			System.out.println("-1==0 is false\tOK");
		if( mone != 0 )
			System.out.println("-1!=0 is true\tOK");
		else
			System.out.println("-1!=0 is false\tERROR");
		if( mone > 0 )
			System.out.println("-1>0 is true\tERROR");
		else
			System.out.println("-1>0 is false\tOK");
		if( mone >= 0 )
			System.out.println("-1>=0 is true\tERROR");
		else
			System.out.println("-1>=0 is false\tOK");
		if( mone < 0 )
			System.out.println("-1<0 is true\tOK");
		else
			System.out.println("-1<0 is false\tERROR");
		if( mone <= 0 )
			System.out.println("-1<=0 is true\tOK");
		else
			System.out.println("-1<=0 is false\tERROR");

		System.out.println("int i=0; while( i < 3 ) i++;");
		int i=0;
		while( i < 3 ) {
			System.out.println(i);
			i = i + 1;
		}
		
		System.out.println("int i=5; do i++; while( i < 3 );");
		i=5;
		do {
			System.out.println(i);
			i = i + 1;
		} while( i < 3 );
		
		System.out.println("int i=0; do i++; while( i < 3 );");
		i=0;
		do {
			System.out.println(i);
			i = i + 1;
		} while( i < 3 );
		

	}
}
