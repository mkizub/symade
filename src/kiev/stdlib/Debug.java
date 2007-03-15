/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.stdlib;

import kiev.vlang.CallExpr;
import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */

/**
 *  This is special class for debugging purposes.
 *  
 *  First of all, all methods of Debug class and
 *  all child classes, that extends Debug class
 *  *MUST* be static and return nothing (has return
 *  type 'void').
 *
 *  More then that, the methods of these classes
 *  should have no side-effect on your program
 *  evaluation, since, compiler is to silently
 *  ignore all calls to these methods if it's not
 *  compiles in debug mode (-g command line switch).
 *  
 *  Even more. Compiler is allowed to automatically
 *  optimize the calls to these method by the next
 *  rules:
 *  a) if first argument is boolean - the method call
 *  may be optimized by evaluation only the first
 *  argument
 *  b) if method name equals (case-insensitive)
 *  or containce "assert" - the method may be called
 *  only if the first argument is evaluated to be "false"
 *  c) otherwice, if method name equals (case-insensitive)
 *  or containce "trace" - the method may be called
 *  only if the first argument is evaluated to be "true"
 *
 *  You may extends this class to provide you special
 *  debug/trace methods, and such methods, that calls to
 *  then may be omited in release version of you program
 *  Do not forget about the requirements to these method's
 *  declaration and usage!
 */
public class Debug {
	
	/**
	 *  The stream to output trace messages
	 *  It's defined to be public and static,
	 *  so, it's imported as well, as assert/trace methods
	 *  Note, that default log stream is System.out, not
	 *  System.err
	 */
	public static java.io.PrintStream		log = System.out;
	
	/**
	 *  Default action for failed assertion - is to
	 *  throw AssertionFailedException, but if this
	 *  handler is defined - it will be called to process
	 *  failed assertion instead.
	 *  Since it's public and static - it's declared to
	 *  be "hidden" by $ name, to not interference with
	 *  normal variables, if the whole Debug class is imported
	 */
	public static AssertionHandler			$AssertionHandler;

	public static void trace_force(String msg) {
		Debug.log.println(msg);
	}
	
	/**
	 *  trace is a generic tracing/logging method.
	 *  It uses "log" stream to trace/log program
	 *  execution.
	 *  The message is printed by log.println(String)
	 *
	 *  @param	msg		the message to log
	 */

	@macro
	public static void trace(String msg)
	{
		case CallExpr# self():
			if# ($GenTraces)
				Debug.trace_force(msg)
			else
				new # NopExpr()
	}

	/**
	 *  The conditional version of trace method.
	 *  The message is printed only of condition
	 *  is true.
	 *  Note, that compiler automatically optimizes
	 *  the usage of this method - the message is
	 *  evaluated and method is called *only* if
	 *  condition is true.
	 *
	 *  @param	cond	the condition
	 *  @param	msg		the message to log
	 */

	@macro
	public static void trace(boolean cond, String msg)
	{
		case CallExpr# self():
			if# ($GenTraces)
				{ if (cond) Debug.trace_force(msg) }
			else
				new # NopExpr()
	}

	/**
	 *  Unconditional assert method. Always throws
	 *  provided exception or pass exception
	 *  to $AssertionHandler
	 */
	public static void assert(RuntimeException t)
		throws RuntimeException
	{
		if( $AssertionHandler == null ) throw t;
		else $AssertionHandler.failedAssertion(t);
	}
	public static void assert(Error t)
		throws Error
	{
		if( $AssertionHandler == null ) throw t;
		else $AssertionHandler.failedAssertion(t);
	}

	/**
	 *  Unconditional assert method. Always throws
	 *  AssertionFailedException or pass exception
	 *  to $AssertionHandler
	 */
	public static void assert() {
		AssertionFailedException afe =
			new AssertionFailedException();
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertInvariant() {
		AssertionFailedException afe =
			new InvariantFailedException();
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertRequire() {
		AssertionFailedException afe =
			new RequireFailedException();
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertEnsure() {
		AssertionFailedException afe =
			new EnsureFailedException();
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	/**
	 *  Unconditional assert method. Always throws
	 *  AssertionFailedException or pass exception
	 *  to $AssertionHandler
	 *
	 *  @param	msg		the message for exception
	 */
	public static void assert(String msg) {
		AssertionFailedException afe =
			new AssertionFailedException(msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertInvariant(String msg) {
		AssertionFailedException afe =
			new InvariantFailedException(msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertRequire(String msg) {
		AssertionFailedException afe =
			new RequireFailedException(msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertEnsure(String msg) {
		AssertionFailedException afe =
			new EnsureFailedException(msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	/**
	 *  Unconditional assert method. Always throws
	 *  AssertionFailedException or pass exception
	 *  to $AssertionHandler
	 *
	 *  @param	name	the name of assertion
	 *  @param	msg		the message for exception
	 */
	public static void assert(String name,String msg) {
		AssertionFailedException afe =
			new AssertionFailedException(name,msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertInvariant(String name,String msg) {
		AssertionFailedException afe =
			new InvariantFailedException(name,msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertRequire(String name,String msg) {
		AssertionFailedException afe =
			new RequireFailedException(name,msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	public static void assertEnsure(String name,String msg) {
		AssertionFailedException afe =
			new EnsureFailedException(name,msg);
		if( $AssertionHandler == null ) throw afe;
		else $AssertionHandler.failedAssertion(afe);
	}

	/**
	 *  Conditional version of assert method.
	 *  The exception will be throwed if
	 *  condition is "false", i.e. - assertion
	 *  check failed.
	 *
	 *  @param	cond	the condition
	 */
	@macro
	public static void assert(boolean cond)
	{
		case CallExpr# self():
			if# ($GenAsserts)
				{ if (new #AssertEnabledExpr() && ! cond) Debug.assert(); }
			else
				new #NopExpr()
	}

	/**
	 *  Conditional version of assert method with
	 *  message of exception specified
	 *
	 *  @param	cond	the condition
	 *  @param	msg		the message for exception
	 */
	@macro
	public static void assert(boolean cond, String msg)
	{
		case CallExpr# self():
			if# ($GenAsserts)
				{ if (new #AssertEnabledExpr() && ! cond) Debug.assert(msg); }
			else
				new #NopExpr()
	}

	/**
	 *  Conditional version of assert method with
	 *  explicit exception specified to be throwed
	 *  or passed to $AssertionHandler
	 *
	 *  @param	cond	the condition
	 *  @param	t		the Throwable for exception
	 */
	@macro
	public static void assert(boolean cond, Throwable t)
	{
		case CallExpr# self():
			if# ($GenAsserts)
				{ if (new #AssertEnabledExpr() && ! cond) Debug.assert(t); }
			else
				new #NopExpr()
	}

}

/**
 *  This exception is used by assert methods and
 *  work-by-contract conditions of Kiev language
 *  (pre and post conditions and class' invariants
 *
 *  AssertionFailedException extends RuntimeException,
 *  so, you do not need to declare it in 'throws'
 *  clause of method declaration.
 */

public class AssertionFailedException extends RuntimeException {

	public final String name;

	public AssertionFailedException() { super(); }

	public AssertionFailedException(String msg) { super(msg); }

	public AssertionFailedException(String name, String msg) {
		super(msg);
		this.name = name;
	}

}

public class RequireFailedException extends AssertionFailedException {
	public RequireFailedException() { super(); }
	public RequireFailedException(String msg) { super(msg); }
	public RequireFailedException(String name, String msg) { super(name,msg); }
}

public class EnsureFailedException extends AssertionFailedException {
	public EnsureFailedException() { super(); }
	public EnsureFailedException(String msg) { super(msg); }
	public EnsureFailedException(String name, String msg) { super(name,msg); }
}

public class InvariantFailedException extends AssertionFailedException {
	public InvariantFailedException() { super(); }
	public InvariantFailedException(String msg) { super(msg); }
	public InvariantFailedException(String name, String msg) { super(name,msg); }
}

/**
 *  AssertionHandler interface is used by Debug
 *  class for non-default assertion handling
 */
public interface AssertionHandler {
	@virtual
	public abstract boolean	enabled;
	public void failedAssertion(Throwable e);
}

/**
 *  An implementation of AssertionHandler for
 *  text mode applications.
 *  It prints the rized exception, and allows,
 *  interactivly, print the stack trace,
 *  and then throw exception, ignore exception
 *  or just abort the program execition
 */
public class TTYAssertionHandler implements AssertionHandler {
	
	public static java.io.PrintStream		err = System.err;
	public static java.io.InputStream		in = System.in;

	@virtual public boolean enabled = true;
	
	@getter public boolean get$enabled() { return enabled; }
	@setter public void set$enabled(boolean e) { enabled = e; }
	
	public TTYAssertionHandler() { this(true); }
	public TTYAssertionHandler(boolean enabled) { this.enabled = enabled; }
	
	public void failedAssertion(Throwable e) {
		if( !enabled ) throw (RuntimeException)e;
		int ch;
	show_message:
		for(;;) {
			this.err.println("\n"+b+"Assertion failed: "+m+e.toString());
			this.err.println(b+"T"+m+")hrow, "+b+"P"+m+")rint stack trace, "+b+"I"+m+")gnore, "+b+"A"+m+")bort execution:");
			try {
				ch=this.in.read();
			} catch( java.io.IOException e ) {
				ch = -1;
			}
			switch(ch) {
			case -1:
			case 'T': case 't':		throw (RuntimeException)e;
			case 'P': case 'p':		e.printStackTrace(err); continue;
			case 'I': case 'i':		return;
			case 'A': case 'a':		System.exit(1); throw (RuntimeException)e;
			case ' ': case '\t': case '\r': case '\n':
				for(;;) {
					ch=this.in.read();
					if( Character.isSpace((char)ch) ) continue;
					goto case ch;
				}
			}
			this.err.println("respond with 't', 'p', 'i' or 'a'");
		}
	}
	
	public static boolean colored = false;
	
	@virtual private abstract String b;
	@setter private void set$b(String s){}
	@getter private String get$b() { return (colored ? "\033[01;36m" : ""); }

	@virtual private abstract String m;
	@setter private void set$m(String s){}
	@getter private String get$m() { return (colored ? "\033[0m" : ""); }
}

