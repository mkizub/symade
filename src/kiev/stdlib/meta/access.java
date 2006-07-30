package kiev.stdlib.meta;

import java.lang.annotation.*;

import syntax kiev.stdlib.Syntax;

@Retention(RetentionPolicy.RUNTIME)
public @interface access {
	public String simple() default "";
	public int flags() default 0xFFFFFFFF;
}
