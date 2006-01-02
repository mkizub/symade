package test.a_basic;
class test_007 {
	public static void main(String[] args) {
		int i1 = 1, i2 = 2, i;
		long l1 = 1L, l2 = 2L, l;
		float f1 = 1.f, f2 = 2.f, f;
		double d1 = 1.D, d2 = 2.D, d;

		i = i1 + i2;
		i = i1 - i2;
		i = i1 * i2;
		i = i1 / i2;
		i = i1 % i2;
		i = i1 << i2;
		i = i1 >> i2;
		i = i1 >>> i2;
		
		l = l1 + l2;
		l = l1 - l2;
		l = l1 * l2;
		l = l1 / l2;
		l = l1 % l2;
		l = l1 << 1;
		l = l1 >> 1;
		l = l1 >>> 2;
		
		f = f1 + f2;
		f = f1 - f2;
		f = f1 * f2;
		f = f1 / f2;
		f = f1 % f2;

		d = d1 + d2;
		d = d1 - d2;
		d = d1 * d2;
		d = d1 / d2;
		d = d1 % d2;

		boolean b;

		b = i1 == i2;
		b = i1 != i2;
		b = i1 >  i2;
		b = i1 >= i2;
		b = i1 <  i2;
		b = i1 <= i2;

		b = l1 == l2;
		b = l1 != l2;
		b = l1 >  l2;
		b = l1 >= l2;
		b = l1 <  l2;
		b = l1 <= l2;

		b = f1 == f2;
		b = f1 != f2;
		b = f1 >  f2;
		b = f1 >= f2;
		b = f1 <  f2;
		b = f1 <= f2;

		b = d1 == d2;
		b = d1 != d2;
		b = d1 >  d2;
		b = d1 >= d2;
		b = d1 <  d2;
		b = d1 <= d2;

		System.out.println("Done.");
	}
}