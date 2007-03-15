package test.a_basic;
// Access tests
public class test_021 {

	public static String s = "static field";
	public String vs = "instance field";

	public static int si = 10;
	public int ii = 10;

	public static void main(String[] args) {
		try {
			if( static_access_tests(true,1,"hello") )
				System.out.println("Static access test OK");
			else
				System.out.println("Static access test ERROR");
		} catch ( RuntimeException e ) {
				System.out.println("Static access test ERROR");
				e.printStackTrace(System.out);
		}

		test_021 t = new test_021();
		try {
			if( t.virtual_access_tests(true,1,"hello") )
				System.out.println("Virtual access test OK");
			else
				System.out.println("Virtual access test ERROR");
		} catch ( RuntimeException e ) {
				System.out.println("Virtual access test ERROR");
				e.printStackTrace(System.out);
		}

		return;
	}

	public static boolean static_access_tests(boolean b, int i, String str) {
		// Method args
		if( b!=true ) throw new RuntimeException("Error b!=true ");
		if( !b ) throw new RuntimeException("Error !b ");
		if( i != 1 ) throw new RuntimeException("Error i != 1 ");
		if( i > 1 ) throw new RuntimeException("Error i > 1 ");
		if( i < 1 ) throw new RuntimeException("Error i < 1 ");
		if( str == null ) throw new RuntimeException("Error str == null ");
		if( !str.equals("hello") ) throw new RuntimeException("Error !str.equals(\"hello\") ");

		// Static class fields
		if( s == null ) throw new RuntimeException("Error s == null ");
		if( !s.equals("static field") ) throw new RuntimeException("Error !s.equals(\"static field\") ");
		if( si != 10 ) throw new RuntimeException("Error si != 10 ");
		if( si > 10 ) throw new RuntimeException("Error si > 10 ");
		if( si < 10 ) throw new RuntimeException("Error si < 10 ");

		// Local vars
		int j = 10;
		Integer n = new Integer(10);
		
		if( j != 10 ) throw new RuntimeException("Error j != 10 ");
		if( j > 10 ) throw new RuntimeException("Error j > 10 ");
		if( j < 10 ) throw new RuntimeException("Error j < 10 ");
		if( !(n instanceof Integer) ) throw new RuntimeException("Error !(n instanceof Integer) ");
		if( n.intValue() != j ) throw new RuntimeException("Error n.intValue() != j ");
		if( !n.equals(new Integer(j)) ) throw new RuntimeException("Error !n.equals(new Integer(j)) ");

		// Increment tests
		if( i++ != 1 || i != 2 ) throw new RuntimeException("Error i++ != 1 || i != 2 => i is "+i);
		if( j++ != 10 || j != 11 ) throw new RuntimeException("Error j++ != 10 || j != 11 => j is "+j);
		if( si++ != 10 || si != 11 ) throw new RuntimeException("Error si++ != 10 || si != 11 => si is "+si);
		if( ++i != 3 || i != 3 ) throw new RuntimeException("Error ++i != 3 || i != 3 => i is "+i);
		if( ++j != 12 || j != 12 ) throw new RuntimeException("Error ++j != 12 || j != 12 => j is "+j);
		if( ++si != 12 || si != 12 ) throw new RuntimeException("Error ++si != 12 || si != 12 => si is "+si);

		// OpAssign tests
		if( (i += 3) != 6 || i != 6 ) throw new RuntimeException("Error (i += 3) != 6 || i != 6 ");
		if( (j *= 2) != 24 || j != 24 ) throw new RuntimeException("Error (j *= 2) != 24 || j != 24 ");
		if( (si /= 3) != 4 || si != 4 ) throw new RuntimeException("Error (si /= 3) != 4 || si != 4 ");

		return true;
	}

	public boolean virtual_access_tests(boolean b, int i, String str) {
		// Method args
		if( b!=true ) throw new RuntimeException("Error b!=true ");
		if( !b ) throw new RuntimeException("Error !b ");
		if( i != 1 ) throw new RuntimeException("Error i != 1 ");
		if( i > 1 ) throw new RuntimeException("Error i > 1 ");
		if( i < 1 ) throw new RuntimeException("Error i < 1 ");
		if( str == null ) throw new RuntimeException("Error str == null ");
		if( !str.equals("hello") ) throw new RuntimeException("Error !str.equals(\"hello\") ");

		// Static class fields
		si = 10;
		if( s == null ) throw new RuntimeException("Error s == null ");
		if( !s.equals("static field") ) throw new RuntimeException("Error !s.equals(\"static field\") ");
		if( si != 10 ) throw new RuntimeException("Error si != 10 ");
		if( si > 10 ) throw new RuntimeException("Error si > 10 ");
		if( si < 10 ) throw new RuntimeException("Error si < 10 ");

		// Instance class fields
		if( vs == null ) throw new RuntimeException("Error vs == null ");
		if( !vs.equals("instance field") ) throw new RuntimeException("Error !vs.equals(\"instance field\") ");
		if( ii != 10 ) throw new RuntimeException("Error ii != 10 ");
		if( ii > 10 ) throw new RuntimeException("Error ii > 10 ");
		if( ii < 10 ) throw new RuntimeException("Error ii < 10 ");

		// Local vars
		int j = 10;
		Integer n = new Integer(10);
		
		if( j != 10 ) throw new RuntimeException("Error j != 10 ");
		if( j > 10 ) throw new RuntimeException("Error j > 10 ");
		if( j < 10 ) throw new RuntimeException("Error j < 10 ");
		if( !(n instanceof Integer) ) throw new RuntimeException("Error !(n instanceof Integer) ");
		if( n.intValue() != j ) throw new RuntimeException("Error n.intValue() != j ");
		if( !n.equals(new Integer(j)) ) throw new RuntimeException("Error !n.equals(new Integer(j)) ");

		// Increment tests
		si = 10;
		ii = 10;
		if( i++ != 1 || i != 2 ) throw new RuntimeException("Error i++ != 1 || i != 2 ");
		if( j++ != 10 || j != 11 ) throw new RuntimeException("Error j++ != 10 || j != 11 ");
		if( si++ != 10 || si != 11 ) throw new RuntimeException("Error si++ != 10 || si != 11 ");
		if( ii++ != 10 || ii != 11 ) throw new RuntimeException("Error ii++ != 10 || ii != 11 ");
		if( ++i != 3 || i != 3 ) throw new RuntimeException("Error ++i != 3 || i != 3 ");
		if( ++j != 12 || j != 12 ) throw new RuntimeException("Error ++j != 12 || j != 12 ");
		if( ++si != 12 || si != 12 ) throw new RuntimeException("Error ++si != 12 || si != 12 ");
		if( ++ii != 12 || ii != 12 ) throw new RuntimeException("Error ++ii != 12 || ii != 12 ");

		// OpAssign tests
		if( (i += 3) != 6 || i != 6 ) throw new RuntimeException("Error (i += 3) != 6 || i != 6 ");
		if( (j *= 2) != 24 || j != 24 ) throw new RuntimeException("Error (j *= 2) != 24 || j != 24 ");
		if( (si /= 3) != 4 || si != 4 ) throw new RuntimeException("Error (si /= 3) != 4 || si != 4 ");
		if( (ii /= 3) != 4 || ii != 4 ) throw new RuntimeException("Error (ii /= 3) != 4 || ii != 4 ");

		return true;
	}



}
