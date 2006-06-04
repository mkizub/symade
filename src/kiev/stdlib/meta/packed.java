package kiev.stdlib.meta;

import java.lang.annotation.*;

import syntax kiev.stdlib.Syntax;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface packed {
	public int    size();
	public String in() default "";
	public int    offset() default 0;
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
private @interface packer {
	public int    size() default 0;
}
