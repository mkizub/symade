package kiev.stdlib.meta;

import java.lang.annotation.*;

import syntax kiev.stdlib.Syntax;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface virtual {}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface getter {
	public String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface setter {
	public String value() default "";
}
