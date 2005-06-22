package kiev.tree;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Constants;
import kiev.vlang.Struct;
import kiev.vlang.Method;
import kiev.vlang.Field;
import kiev.vlang.Var;
import kiev.vlang.Expr;
import kiev.vlang.Statement;
import kiev.vlang.Access;
import kiev.vlang.CaseLabel;
import kiev.vlang.CatchInfo;
import kiev.vlang.Type;
import kiev.vlang.Dumper;
import kiev.vlang.CompilerException;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final $wrapper class VNode<N extends Node> {

	private forward access:ro,rw N $node;
	
	public VNode() {
	}
	
	public VNode(N node) {
		this.$node = node;
	}
	
	public final N getNode()
		alias operator(210,fy,$cast)
	{
		return this.$node;
	}
	
	public final void setNode(N node) {
		assert(node.vnode == null);
		node.vnode = this;
		this.$node = node;
	}
}

public final class VNodeArray<N extends Node> {

    private VNode<Node> $parent := null;
	private VNode<Node>[]  $nodes;
	
	public VNodeArray(Node# parent) {
		this.$parent := parent;
		this.$nodes = new VNode<Node>[0];
	}
	
	public VNodeArray(Node parent) {
		this.$parent := parent.vnode;
		this.$nodes = new VNode<Node>[0];
	}
	
	public VNodeArray(int size, Node# parent) {
		this.$parent := parent;
		this.$nodes = new VNode<Node>[size];
	}
	
	public VNodeArray(int size, Node parent) {
		this.$parent := parent.vnode;
		this.$nodes = new VNode<Node>[size];
	}
	
	public void set$parent(Node# p) {
		assert(parent == null);
		parent := p;
	}

	public int size()
		alias length
		alias get$size
		alias get$length
	{
		return $nodes.length;
	}

	public /*abstract*/ void cleanup() {
		$parent = null;
		$nodes = null;
	};
	
	public final VNode<N> get(int idx)
		alias at
		alias operator(210,xfy,[])
	{
		return $nodes[idx];
	}
	
	public VNode<N> set(int idx, VNode<N> node)
		alias operator(210,lfy,[])
	{
		$nodes[idx] = node;
		return node;
	}

	public VNode<N> add(N node)
		alias append
	{
		return add(node.vnode);
	}
	
	public VNode<N> add(VNode<N> node)
		alias append
	{
		int sz = $nodes.length;
		VNode<N>[] tmp = new VNode<N>[sz+1];
		int i;
		for (i=0; i < sz; i++)
			tmp[i] = $nodes[i];
		$nodes = tmp;
		$nodes[sz] = node;
		return node;
	}

	public VNode<N> insert(int idx, VNode<N> node)
	{
		int sz = $nodes.length;
		VNode<N>[] tmp = new VNode<N>[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		for (i++; i < sz; i++)
			tmp[i+1] = $nodes[i];
		tmp[idx] = node;
		$nodes = tmp;
		return node;
	}

	public VNode<N> del(int idx)
	{
		int sz = $nodes.length;
		VNode<N>[] tmp = new VNode<N>[sz-1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		for (i++; i < sz; i++)
			tmp[i-1] = $nodes[i];
		VNode<N> ret = $nodes[idx];
		$nodes = tmp;
		return ret;
	}

	public boolean contains(N node) {
		for (int i=0; i < $nodes.length; i++) {
			if ($nodes[i].equals(node))
				return true;
		}
		return false;
	}

	public Enumeration<N> elements() {
		return new Enumeration<N>() {
			int current;
			public boolean hasMoreElements() { return current < VNodeArray.this.size(); }
			public A nextElement() {
				if ( current < size() ) return VNodeArray.this[current++];
				throw new NoSuchElementException(Integer.toString(VNodeArray.this.size()));
			}
		};
	}

}

public final $wrapper class VNodeRef<R extends Node> {

	private forward access:ro,rw VNode<R> $ref := null;
	
	public VNodeRef() {
	}
	
	public VNodeRef(VNode<R> ref) {
		this.$ref = ref;
	}
	
	public final VNode<R> getRef()
		alias operator(210,fy,$cast)
	{
		return this.$ref;
	}
	
	public final void setRef(VNode<R> ref) {
		this.$ref = ref;
	}
}

public abstract class CreateInfo {
}

public abstract class Node implements Constants {
	public int			pos;
	// the node which encapsulates this implementation (version)
    public VNode<Node>		vnode;
	// the reason of creating and other information about this node
	public CreateInfo		src_info;
    // the parent node in the tree
    public VNode<Node>		parent;
	// node flags
	public int				flags;

	public Node() {
		this(0, (Node#)null);
	}
	
	public Node(int pos) {
		this(pos, (Node#)null);
	}
	
	public Node(Node# parent) {
		this(0, parent);
	}
	
	public Node(int pos, Node# parent) {
		this.pos = pos;
		this.parent = parent;
		new VNode<Node>(this);
	}
	
	public Node(int pos, Node parent) {
		this.pos = pos;
		this.parent = parent.vnode;
		new VNode<Node>(this);
	}
	
	public final Node# getVNode()
		alias operator(210,fy,$cast)
	{
		return this.vnode;
	}
	
	public void set$parent(Node# p) {
		assert(parent == null);
		parent = p;
	}
	
	public /*abstract*/ void cleanup() {
		parent = null;
		src_info = null;
	};
	
    public final int getPos() { return pos; }
    public final int getPosLine() { return pos >>> 11; }
    public final int getPosColumn() { return pos & 0x3FF; }
    public final int setPos(int line, int column) { return pos = (line << 11) | (column & 0x3FF); }
    public final int setPos(int pos) { return this.pos = pos; }
	
	public Type getType() { return Type.tpVoid; }

    public Dumper toJava(Dumper dmp) {
    	dmp.append("/* INTERNAL ERROR - ").append(this.getClass().toString()).append(" */");
    	return dmp;
    }

	public Node pass1()   { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public Node pass1_1() { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public Node pass2()   { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public Node pass2_2() { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public Node pass3(Object obj)  { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public Node autoProxyMethods() { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public Node resolveImports()   { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public Node resolveFinalFields(boolean cleanup) { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }

	public int setFlags(int fl) {
		trace(Kiev.debugFlags,"Member "+this+" flags set to 0x"+Integer.toHexString(fl)+" from "+Integer.toHexString(flags));
		flags = fl;
		if( this instanceof Struct ) {
			Struct self = (Struct)this;
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		else if( this instanceof Method ) {
			Method self = (Method)this;
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		else if( this instanceof Field ) {
			Field self = (Field)this;
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		return flags;
	}
	public int getFlags() { return flags; }
	public short getJavaFlags() { return (short)(flags & JAVA_ACC_MASK); }

	public boolean isPublic()		{ return (flags & ACC_PUBLIC) != 0; }
	public boolean isPrivate()		{ return (flags & ACC_PRIVATE) != 0; }
	public boolean isProtected()	{ return (flags & ACC_PROTECTED) != 0; }
	public boolean isPackageVisable()	{ return (flags & (ACC_PROTECTED|ACC_PUBLIC|ACC_PROTECTED)) == 0; }
	public boolean isStatic()		{ return (flags & ACC_STATIC) != 0; }
	public boolean isFinal()		{ return (flags & ACC_FINAL) != 0; }
	public boolean isSynchronized()	{ return (flags & ACC_SYNCHRONIZED) != 0; }
	public boolean isVolatile()		{ return (flags & ACC_VOLATILE) != 0; }
	public boolean isTransient()	{ return (flags & ACC_TRANSIENT) != 0; }
	public boolean isNative()		{ return (flags & ACC_NATIVE) != 0; }
	public boolean isInterface()	{ return (flags & ACC_INTERFACE) != 0; }
	public boolean isAbstract()		{ return (flags & ACC_ABSTRACT) != 0; }
	public boolean isSuper()		{ return (flags & ACC_SUPER) != 0; }

	// Struct specific
	public boolean isPackage()		{ return (flags & ACC_PACKAGE) != 0; }
	public boolean isClazz()		{ return (flags & (ACC_PACKAGE|ACC_INTERFACE|ACC_ARGUMENT)) == 0; }
	public boolean isArgument()		{ return (flags & ACC_ARGUMENT) != 0; }
	public boolean isPizzaCase()	{ return (flags & ACC_PIZZACASE) != 0; }
	public boolean isLocal()		{ return (flags & ACC_LOCAL) != 0; }
	public boolean isAnonymouse()	{ return (flags & ACC_ANONYMOUSE) != 0; }
	public boolean isHasCases()		{ return (flags & ACC_HAS_CASES) != 0; }
	public boolean isVerified()		{ return (flags & ACC_VERIFIED) != 0; }
	public boolean isMembersGenerated()		{ return (flags & ACC_MEMBERS_GENERATED) != 0; }
	public boolean isStatementsGenerated()	{ return (flags & ACC_STATEMENTS_GENERATED) != 0; }
	public boolean isGenerated()	{ return (flags & ACC_GENERATED) != 0; }
	public boolean isEnum()			{ return (flags & ACC_ENUM) != 0; }
	public boolean isSyntax()		{ return (flags & ACC_SYNTAX) != 0; }
	public boolean isPrimitiveEnum(){ return (flags & ACC_PRIMITIVE_ENUM) != 0; }
	public boolean isWrapper()		{ return (flags & ACC_WRAPPER) != 0; }

	// Method specific
	public boolean isMultiMethod()	{ return (flags & ACC_MULTIMETHOD) != 0; }
	public boolean isVirtualStatic(){ return (flags & ACC_VIRTUALSTATIC) != 0; }
	public boolean isVarArgs()		{ return (flags & ACC_VARARGS) != 0; }
	public boolean isRuleMethod()	{ return (flags & ACC_RULEMETHOD) != 0; }
	public boolean isOperatorMethod()	{ return (flags & ACC_OPERATORMETHOD) != 0; }
	public boolean isGenPostCond()	{ return (flags & ACC_GENPOSTCOND) != 0; }
	public boolean isNeedFieldInits()	{ return (flags & ACC_NEEDFIELDINITS) != 0; }
	public boolean isInvariantMethod()	{ return (flags & ACC_INVARIANT_METHOD) != 0; }
	public boolean isLocalMethod()		{ return (flags & ACC_LOCAL_METHOD) != 0; }
	public boolean isProduction()	{ return (flags & ACC_PRODUCTION) != 0; }

	// Var specific
	public boolean isNeedProxy()	{ return (flags & ACC_NEED_PROXY) != 0; }
	public boolean isNeedRefProxy()	{ return (flags & ACC_NEED_REFPROXY) != 0; }
//	public boolean isPrologVar()	{ return (flags & ACC_PROLOGVAR) != 0; }
//	public boolean isLocalPrologVar()	{ return (flags & ACC_LOCALPROLOGVAR) != 0; }
	public boolean isLocalRuleVar()	{ return (flags & ACC_LOCALRULEVAR) != 0; }
//	public boolean isLocalPrologForVar()	{ return (flags & ACC_LOCALPROLOGFORVAR) != 0; }
	public boolean isClosureProxy()	{ return (flags & ACC_CLOSURE_PROXY) != 0; }
	public boolean isInitWrapper()	{ return (flags & ACC_INIT_WRAPPER) != 0; }

	// Field specific
	public boolean isVirtual()		{ return (flags & ACC_VIRTUAL) != 0; }
	public boolean isPackerField()	{ return (flags & ACC_PACKER_FIELD) != 0; }
	public boolean isPackedField()	{ return (flags & ACC_PACKED_FIELD) != 0; }

	// Var/field
	public boolean isForward()		{ return (flags & ACC_FORWARD) != 0; }

	// Expr specific
	public boolean isUseNoProxy()	{ return (flags & ACC_USE_NOPROXY) != 0; }
	public boolean isAsField()		{ return (flags & ACC_AS_FIELD) != 0; }
	public boolean isConstExpr()	{ return (flags & ACC_CONSTEXPR) != 0; }
	public boolean isTryResolved()	{ return (flags & ACC_TRYRESOLVED) != 0; }
	public boolean isGenResolve()	{ return (flags & ACC_GENRESOLVE) != 0; }
	public boolean isForWrapper()	{ return (flags & ACC_FOR_WRAPPER) != 0; }

	// Statement specific
	public boolean isAbrupted()	{ return (flags & ACC_ABRUPTED) != 0; }
	public boolean isBreaked()	{ return (flags & ACC_BREAKED) != 0; }
	public boolean isMethodAbrupted()	{ return (flags & ACC_METHODABRUPTED) != 0; }
	public boolean isAutoReturnable()	{ return (flags & ACC_AUTORETURNABLE) != 0; }
	public boolean isBreakTarget()	{ return (flags & ACC_BREAK_TARGET) != 0; }
	public boolean isProductionSome()	{ return (flags & ACC_PRODUCTION_SOME) != 0; }
	public boolean isProductionAny()	{ return (flags & ACC_PRODUCTION_ANY) != 0; }
	public boolean isProductionMaybe()	{ return (flags & ACC_PRODUCTION_MAYBE) != 0; }

	// General
	public boolean isAccessedFromInner()	{ return (flags & ACC_FROM_INNER) != 0; }
	public boolean isResolved()		{ return (flags & ACC_RESOLVED) != 0; }
	public boolean isHidden()		{ return (flags & ACC_HIDDEN) != 0; }
	public boolean isBad()			{ return (flags & ACC_BAD) != 0; }

	public void setPublic(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PUBLIC set to "+on+" from "+((flags & ACC_PUBLIC)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PUBLIC;
	}
	public void setPrivate(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRIVATE set to "+on+" from "+((flags & ACC_PRIVATE)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PRIVATE;
	}
	public void setProtected(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PROTECTED set to "+on+" from "+((flags & ACC_PROTECTED)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PROTECTED;
	}
	public void setStatic(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_STATIC set to "+on+" from "+((flags & ACC_STATIC)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_STATIC;
		else flags &= ~ACC_STATIC;
	}
	public void setFinal(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FINAL set to "+on+" from "+((flags & ACC_FINAL)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FINAL;
		else flags &= ~ACC_FINAL;
	}
	public void setSynchronized(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SYNCHRONIZED set to "+on+" from "+((flags & ACC_SYNCHRONIZED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_SYNCHRONIZED;
		else flags &= ~ACC_SYNCHRONIZED;
	}
	public void setVolatile(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VOLATILE set to "+on+" from "+((flags & ACC_VOLATILE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VOLATILE;
		else flags &= ~ACC_VOLATILE;
	}
	public void setTransient(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_TRANSIENT set to "+on+" from "+((flags & ACC_TRANSIENT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_TRANSIENT;
		else flags &= ~ACC_TRANSIENT;
	}
	public void setNative(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NATIVE set to "+on+" from "+((flags & ACC_NATIVE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NATIVE;
		else flags &= ~ACC_NATIVE;
	}
	public void setInterface(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_INTERFACE set to "+on+" from "+((flags & ACC_INTERFACE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_INTERFACE;
		else flags &= ~ACC_INTERFACE;
	}
	public void setAbstract(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ABSTRACT set to "+on+" from "+((flags & ACC_ABSTRACT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ABSTRACT;
		else flags &= ~ACC_ABSTRACT;
	}
	public void setSuper(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SUPER set to "+on+" from "+((flags & ACC_SUPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_SUPER;
		else flags &= ~ACC_SUPER;
	}

	// Struct specific
	public void setPackage(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PACKAGE set to "+on+" from "+((flags & ACC_PACKAGE)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_INTERFACE|ACC_PACKAGE);
		if( on ) flags |= ACC_PACKAGE;
	}
	public void setArgument(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ARGUMENT set to "+on+" from "+((flags & ACC_ARGUMENT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ARGUMENT;
		else flags &= ~ACC_ARGUMENT;
	}
	public void setPizzaCase(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PIZZACASE set to "+on+" from "+((flags & ACC_PIZZACASE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PIZZACASE;
		else flags &= ~ACC_PIZZACASE;
	}
	public void setLocal(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCAL set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_LOCAL;
		else flags &= ~ACC_LOCAL;
	}
	public void setAnonymouse(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ANONYMOUSE set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ANONYMOUSE;
		else flags &= ~ACC_ANONYMOUSE;
	}
	public void setHasCases(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_HAS_CASES set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_HAS_CASES;
		else flags &= ~ACC_HAS_CASES;
	}
	public void setVerified(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VERIFIED set to "+on+" from "+((flags & ACC_VERIFIED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VERIFIED;
		else flags &= ~ACC_VERIFIED;
	}
	public void setMembersGenerated(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_MEMBERS_GENERATED set to "+on+" from "+((flags & ACC_MEMBERS_GENERATED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_MEMBERS_GENERATED;
		else flags &= ~ACC_MEMBERS_GENERATED;
	}
	public void setStatementsGenerated(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_STATEMENTS_GENERATED set to "+on+" from "+((flags & ACC_STATEMENTS_GENERATED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_STATEMENTS_GENERATED;
		else flags &= ~ACC_STATEMENTS_GENERATED;
	}
	public void setGenerated(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_GENERATED set to "+on+" from "+((flags & ACC_GENERATED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_GENERATED;
		else flags &= ~ACC_GENERATED;
	}
	public void setEnum(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ENUM set to "+on+" from "+((flags & ACC_ENUM)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_INTERFACE|ACC_PACKAGE|ACC_ENUM);
		if( on ) flags |= ACC_ENUM;
	}
	public void setSyntax(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SYNTAX set to "+on+" from "+((flags & ACC_SYNTAX)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_INTERFACE|ACC_PACKAGE|ACC_ENUM|ACC_SYNTAX);
		if( on ) flags |= ACC_SYNTAX;
	}
	public void setPrimitiveEnum(boolean on) {
		assert(this instanceof Struct && this.isEnum(),"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRIMITIVE_ENUM set to "+on+" from "+((flags & ACC_PRIMITIVE_ENUM)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~ACC_PRIMITIVE_ENUM;
		if( on ) flags |= ACC_PRIMITIVE_ENUM;
	}
	public void setWrapper(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_WRAPPER set to "+on+" from "+((flags & ACC_WRAPPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_WRAPPER;
		else flags &= ~ACC_WRAPPER;
	}

	// Method specific
	public void setMultiMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_MULTIMETHOD set to "+on+" from "+((flags & ACC_MULTIMETHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_MULTIMETHOD;
		else flags &= ~ACC_MULTIMETHOD;
	}
	public void setVirtualStatic(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VIRTUALSTATIC set to "+on+" from "+((flags & ACC_VIRTUALSTATIC)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VIRTUALSTATIC;
		else flags &= ~ACC_VIRTUALSTATIC;
	}
	public void setVarArgs(boolean on) {
		assert(this instanceof Method || this instanceof kiev.parser.ASTMethodDeclaration,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VARARGS set to "+on+" from "+((flags & ACC_VARARGS)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VARARGS;
		else flags &= ~ACC_VARARGS;
	}
	public void setRuleMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_RULEMETHOD set to "+on+" from "+((flags & ACC_RULEMETHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_RULEMETHOD;
		else flags &= ~ACC_RULEMETHOD;
	}
	public void setOperatorMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_OPERATORMETHOD set to "+on+" from "+((flags & ACC_OPERATORMETHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_OPERATORMETHOD;
		else flags &= ~ACC_OPERATORMETHOD;
	}
	public void setGenPostCond(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_GENPOSTCOND set to "+on+" from "+((flags & ACC_GENPOSTCOND)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_GENPOSTCOND;
		else flags &= ~ACC_GENPOSTCOND;
	}
	public void setNeedFieldInits(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NEEDFIELDINITS set to "+on+" from "+((flags & ACC_NEEDFIELDINITS)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NEEDFIELDINITS;
		else flags &= ~ACC_NEEDFIELDINITS;
	}
	public void setInvariantMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_INVARIANT_METHOD set to "+on+" from "+((flags & ACC_INVARIANT_METHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_INVARIANT_METHOD;
		else flags &= ~ACC_INVARIANT_METHOD;
	}
	public void setLocalMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCAL_METHOD set to "+on+" from "+((flags & ACC_LOCAL_METHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_LOCAL_METHOD;
		else flags &= ~ACC_LOCAL_METHOD;
	}
	public void setProduction(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION set to "+on+" from "+((flags & ACC_PRODUCTION)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION;
		else flags &= ~ACC_PRODUCTION;
	}

	// Var specific
	public void setNeedProxy(boolean on) {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NEED_PROXY set to "+on+" from "+((flags & ACC_NEED_PROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NEED_PROXY;
		else flags &= ~ACC_NEED_PROXY;
	}
	public void setNeedRefProxy(boolean on) {
		assert(this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NEED_REFPROXY set to "+on+" from "+((flags & ACC_NEED_REFPROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NEED_PROXY | ACC_NEED_REFPROXY;
		else flags &= ~ACC_NEED_REFPROXY;
	}
//	public void setPrologVar(boolean on) {
//		assert(this instanceof Var,"For node "+this.getClass());
//		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PROLOGVAR set to "+on+" from "+((flags & ACC_PROLOGVAR)!=0)+", now 0x"+Integer.toHexString(flags));
//		if( on ) flags |= ACC_PROLOGVAR;
//		else flags &= ~ACC_PROLOGVAR;
//	}
//	public void setLocalPrologVar(boolean on) {
//		assert(this instanceof Var || this instanceof kiev.parser.ASTFormalParameter,"For node "+this.getClass());
//		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCALPROLOGVAR set to "+on+" from "+((flags & ACC_LOCALPROLOGVAR)!=0)+", now 0x"+Integer.toHexString(flags));
//		if( on ) flags |= ACC_LOCALPROLOGVAR;
//		else flags &= ~ACC_LOCALPROLOGVAR;
//	}
	public void setLocalRuleVar(boolean on) {
		assert(this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCALRULEVAR set to "+on+" from "+((flags & ACC_LOCALRULEVAR)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_LOCALRULEVAR;
		else flags &= ~ACC_LOCALRULEVAR;
	}
//	public void setLocalPrologForVar(boolean on) {
//		assert(this instanceof Var,"For node "+this.getClass());
//		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCALPROLOGFORVAR set to "+on+" from "+((flags & ACC_LOCALPROLOGFORVAR)!=0)+", now 0x"+Integer.toHexString(flags));
//		if( on ) flags |= ACC_LOCALPROLOGFORVAR;
//		else flags &= ~ACC_LOCALPROLOGFORVAR;
//	}
	public void setClosureProxy(boolean on) {
		assert(this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_CLOSURE_PROXY set to "+on+" from "+((flags & ACC_CLOSURE_PROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_CLOSURE_PROXY;
		else flags &= ~ACC_CLOSURE_PROXY;
	}

	// Var/field specific
	public void setInitWrapper(boolean on) {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_INIT_WRAPPER set to "+on+" from "+((flags & ACC_INIT_WRAPPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_INIT_WRAPPER;
		else flags &= ~ACC_INIT_WRAPPER;
	}


	// Field specific
	public void setVirtual(boolean on) {
		assert(this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VIRTUAL set to "+on+" from "+((flags & ACC_VIRTUAL)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VIRTUAL;
		else flags &= ~ACC_VIRTUAL;
	}
	public void setPackerField(boolean on) {
		assert(this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PACKER_FIELD set to "+on+" from "+((flags & ACC_PACKER_FIELD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PACKER_FIELD;
		else flags &= ~ACC_PACKER_FIELD;
	}
	public void setPackedField(boolean on) {
		assert(this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PACKED_FIELD set to "+on+" from "+((flags & ACC_PACKED_FIELD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PACKED_FIELD;
		else flags &= ~ACC_PACKED_FIELD;
	}

	// Var/field
	public void setForward(boolean on) {
		assert(this instanceof Field || this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FORWARD set to "+on+" from "+((flags & ACC_FORWARD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FORWARD;
		else flags &= ~ACC_FORWARD;
	}

	// Expr specific
	public void setUseNoProxy(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_USE_NOPROXY set to "+on+" from "+((flags & ACC_USE_NOPROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_USE_NOPROXY;
		else flags &= ~ACC_USE_NOPROXY;
	}
	public void setAsField(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_AS_FIELD set to "+on+" from "+((flags & ACC_AS_FIELD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_AS_FIELD;
		else flags &= ~ACC_AS_FIELD;
	}
	public void setConstExpr(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_CONSTEXPR set to "+on+" from "+((flags & ACC_CONSTEXPR)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_CONSTEXPR;
		else flags &= ~ACC_CONSTEXPR;
	}
	public void setTryResolved(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_TRYRESOLVED set to "+on+" from "+((flags & ACC_TRYRESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_TRYRESOLVED;
		else flags &= ~ACC_TRYRESOLVED;
	}
	public void setGenResolve(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_GENRESOLVE set to "+on+" from "+((flags & ACC_GENRESOLVE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_GENRESOLVE;
		else flags &= ~ACC_GENRESOLVE;
	}
	public void setForWrapper(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FOR_WRAPPER set to "+on+" from "+((flags & ACC_FOR_WRAPPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FOR_WRAPPER;
		else flags &= ~ACC_FOR_WRAPPER;
	}

	// Statement specific
	public void setAbrupted(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ABRUPTED set to "+on+" from "+((flags & ACC_ABRUPTED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ABRUPTED;
		else flags &= ~ACC_ABRUPTED;
	}
	public void setBreaked(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_BREAKED set to "+on+" from "+((flags & ACC_BREAKED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_BREAKED;
		else flags &= ~ACC_BREAKED;
	}
	public void setMethodAbrupted(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_METHODABRUPTED set to "+on+" from "+((flags & ACC_METHODABRUPTED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ABRUPTED | ACC_METHODABRUPTED;
		else flags &= ~ACC_METHODABRUPTED;
	}
	public void setAutoReturnable(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_AUTORETURNABLE set to "+on+" from "+((flags & ACC_AUTORETURNABLE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_AUTORETURNABLE;
		else flags &= ~ACC_AUTORETURNABLE;
	}
	public void setBreakTarget(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_BREAK_TARGET set to "+on+" from "+((flags & ACC_BREAK_TARGET)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_BREAK_TARGET;
		else flags &= ~ACC_BREAK_TARGET;
	}
	public void setProductionSome(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION_SOME set to "+on+" from "+((flags & ACC_PRODUCTION_SOME)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION_SOME;
		else flags &= ~ACC_PRODUCTION_SOME;
	}
	public void setProductionAny(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION_ANY set to "+on+" from "+((flags & ACC_PRODUCTION_ANY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION_ANY;
		else flags &= ~ACC_PRODUCTION_ANY;
	}
	public void setProductionMaybe(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION_MAYBE set to "+on+" from "+((flags & ACC_PRODUCTION_MAYBE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION_MAYBE;
		else flags &= ~ACC_PRODUCTION_MAYBE;
	}

	// General
	public void setAccessedFromInner(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FROM_INNER set to "+on+" from "+((flags & ACC_FROM_INNER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FROM_INNER;
		else flags &= ~ACC_FROM_INNER;
	}
	public void setResolved(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_RESOLVED set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_RESOLVED;
		else flags &= ~ACC_RESOLVED;
	}
	public void setHidden(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_HIDDEN set to "+on+" from "+((flags & ACC_HIDDEN)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_HIDDEN;
		else flags &= ~ACC_HIDDEN;
	}
	public void setBad(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_BAD set to "+on+" from "+((flags & ACC_BAD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_BAD;
		else flags &= ~ACC_BAD;
	}

}
