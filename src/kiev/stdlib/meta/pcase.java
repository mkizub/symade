package kiev.stdlib.meta;

import java.lang.annotation.*;

import syntax kiev.stdlib.Syntax;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface pcase {
	public int tag();
	public String[] fields();
}

