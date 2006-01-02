package test.a_basic;
class test_014 {
	public static void main(String[] args) {
		int i1 = 2, i2 = 1, i = 0;
		long l1 = 2L, l2 = 1L, l = 0L;
		float f1 = 2.f, f2 = 1.f, f = 0.f;
		double d1 = 2.D, d2 = 1.D, d = 0.D;

		i += i1 + i2;
		System.out.println("i += i1 + i2 := "+i);
		i -= i1 - i2;
		System.out.println("i -= i1 - i2 := "+i);
		i *= i1 * i2;
		System.out.println("i *= i1 * i2 := "+i);
		i /= i1 / i2;
		System.out.println("i /= i1 / i2 := "+i);
		i = 3 % 2;
		System.out.println("i = 3 % 2 := "+i);
		i = i1 & i2;
		System.out.println("i = i1 & i2 := "+i);
		i = i1 ^ i2;
		System.out.println("i = i1 ^ i2 := "+i);
		i = i1 | i2;
		System.out.println("i = i1 | i2 := "+i);
		i &= i1 & i2;
		System.out.println("i &= i1 & i2 := "+i);
		i ^= i1 ^ i2;
		System.out.println("i ^= i1 ^ i2 := "+i);
		i |= i1 | i2;
		System.out.println("i |= i1 | i2 := "+i);
		i <<= 1;
		System.out.println("i <<= 1 := "+i);
		i >>= 1;
		System.out.println("i >>= 1 := "+i);
		i >>>= 1;
		System.out.println("i >>>= 1 := "+i);
		
		l += l1 + l2;
		l -= l1 - l2;
		l *= l1 * l2;
		l /= 10L / 2L;
		l %= 3L;
		l = l1 & l2;
		l = l1 ^ l2;
		l = l1 | l2;
		l &= l1 & l2;
		l ^= l1 ^ l2;
		l |= l1 | l2;
		l <<= 1;
		l >>= 1;
		l >>>= 2;
		
		f += f1 + f2;
		f -= f1 - f2;
		f *= f1 * f2;
		f /= 10.f / 2.f;
		f %= 3.f;

		d += d1 + d2;
		d -= d1 - d2;
		d *= d1 * d2;
		d /= 10.d / 2.d;
		d %= 3.d;

		System.out.println("Done.");
	}
}
