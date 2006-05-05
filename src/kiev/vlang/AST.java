package kiev.vlang;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.be.java15.JType;
import kiev.ir.java15.RNode;
import kiev.be.java15.JNode;
import kiev.be.java15.JSymbolRef;
import kiev.ir.java15.RSymbolRef;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public enum TopLevelPass {
	passStartCleanup		   ,	// start of compilation or cleanup before next incremental compilation
	passProcessSyntax		   ,	// process syntax - some import, typedef, operator and macro
	passStructTypes			   ,	// inheritance and types of structures
	passResolveMetaDecls	   ,	// resolved meta types declarations
	passResolveMetaDefaults	   ,	// resolved default values for meta-methods
	passResolveMetaValues	   ,	// resolve values in meta-data
	passCreateMembers		   ,	// create declared members of structures
	passAutoGenerateMembers	   ,	// generation of members
	passPreResolve			   ,	// pre-resolve nodes
	passMainResolve			   ,	// main resolve for vlang
	passVerify				   ,	// verify the tree before generation
	passPreGenerate			   ,	// prepare tree for generation phase
	passGenerate			   		// resolve, generate and so on - each file separatly
};

public abstract class ANode {

	private AttachInfo		p_info;
	private AttachInfo[]	ndata;

	public abstract ANode nodeCopiedTo(ANode node);

	public final boolean    isAttached()    { return p_info != null; }
	public final AttachInfo getAttachInfo() { return p_info; }

	public final void callbackAttached(ANode parent, AttrSlot pslot) {
		this.callbackAttached(new AttachInfo(this, parent, pslot));
	}
	public final void callbackAttached(AttachInfo pinfo) {
		assert (pinfo.p_slot.is_attr);
		assert(!isAttached());
		assert(pinfo.p_parent != null && pinfo.p_parent != this);
		assert(pinfo.p_self == this);
		this.p_info = pinfo;
		this.callbackAttached();
	}
	public void callbackAttached() {
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { n.callbackRootChanged(); return true; }
		});
		// notify parent about the changed slot
		parent().callbackChildChanged(p_info.p_slot);
	}
	public void callbackDetached() {
		assert(isAttached());
		// do detcah
		AttachInfo pinfo = this.p_info;
		this.p_info = null;
		pinfo.detach();
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { n.callbackRootChanged(); return true; }
		});
		// notify parent about the changed slot
		pinfo.p_parent.callbackChildChanged(pinfo.p_slot);
	}


	@getter public final ANode get$ctx_root() {
		if (!isAttached())
			return this;
		return this.getAttachInfo().get_ctx_root();
	}
	public void callbackChildChanged(AttrSlot attr) { /* do nothing */ }
	public void callbackRootChanged() { /* do nothing */ }

	public final ANode parent() { return this.p_info == null ? null : this.p_info.p_parent; }
	public final AttrSlot pslot() { return this.p_info == null ? null : this.p_info.p_slot; }
	public final ANode pprev() { return this.p_info == null ? null : this.p_info.prev(); }
	public final ANode pnext() { return this.p_info == null ? null : this.p_info.next(); }
	
	@getter public FileUnit get$ctx_file_unit() { return this.parent().get$ctx_file_unit(); }
	@getter public Struct get$ctx_clazz() { return this.parent().child_ctx_clazz; }
	@getter public Struct get$child_ctx_clazz() { return this.parent().get$child_ctx_clazz(); }
	@getter public Method get$ctx_method() { return this.parent().child_ctx_method; }
	@getter public Method get$child_ctx_method() { return this.parent().get$child_ctx_method(); }

	public AttrSlot[] values() {
		return AttrSlot.emptyArray;
	}
	public Object getVal(String name) {
		throw new RuntimeException("No @att value \"" + name + "\" in ANode");
	}
	public void setVal(String name, Object val) {
		throw new RuntimeException("No @att value \"" + name + "\" in ANode");
	}

	public final ANode getNodeData(AttrSlot attr) {
		assert (attr.isData());
		if (ndata != null) {
			foreach (AttachInfo ai; ndata) {
				if (ai.p_slot.name == attr.name)
					return ai.p_self;
			}
		}
		return null;
	}
	
	public final void addNodeData(ANode d, AttrSlot attr) {
		assert (attr.isData());
		if (ndata != null) {
			AttachInfo[] ndata = this.ndata;
			int sz = ndata.length;
			for (int i=0; i < sz; i++) {
				AttachInfo ai = ndata[i];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					if (ai.p_self == d)
						return;
					if (attr.is_attr) {
						ai.p_self.callbackDetached();
						d.callbackAttached(this, attr);
					} else {
						ndata[i] = new AttachInfo(d,this,attr);
					}
					return;
				}
			}
			AttachInfo[] tmp = new AttachInfo[sz+1];
			for (int i=0; i < sz; i++)
				tmp[i] = ndata[i];
			tmp[sz] = new AttachInfo(d,this,attr);
			this.ndata = tmp;
		} else {
			this.ndata = new AttachInfo[]{new AttachInfo(d,this,attr)};
		}
		if (attr.is_attr)
			d.callbackAttached(this, attr);
	}
	
	public final void delNodeData(AttrSlot attr) {
		AttachInfo[] ndata = this.ndata;
		assert (attr.isData());
		if (ndata != null) {
			int sz = ndata.length-1;
			for (int idx=0; idx <= sz; idx++) {
				AttachInfo ai = ndata[idx];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					AttachInfo[] tmp = new AttachInfo[sz];
					int i;
					for (i=0; i < idx; i++) tmp[i] = ndata[i];
					for (   ; i <  sz; i++) tmp[i] = ndata[i+1];
					this.ndata = tmp;
					if (attr.is_attr)
						ai.p_self.callbackDetached();
					return;
				}
			}
		}
	}

	public final void walkTree(TreeWalker walker) {
		if (walker.pre_exec(this)) {
			foreach (AttrSlot attr; this.values(); attr.is_attr) {
				Object val = this.getVal(attr.name);
				if (val == null)
					continue;
				if (attr.is_space) {
					NArr<ASTNode> vals = (NArr<ASTNode>)val;
					for (int i=0; i < vals.length; i++) {
						try {
							vals[i].walkTree(walker);
						} catch (ReWalkNodeException e) { i--; }
					}
				}
				else if (val instanceof ASTNode) {
				re_walk_node:;
					try {
						val.walkTree(walker);
					} catch (ReWalkNodeException e) {
						val = this.getVal(attr.name);
						if (val != null)
							goto re_walk_node;
					}
				}
			}
			if (ndata != null) {
				foreach (AttachInfo ai; this.ndata; ai.p_slot.is_attr)
					ai.p_self.walkTree(walker);
			}
		}
		walker.post_exec(this);
	}

	public Object copyTo(Object to$node) {
		ANode node = (ANode)to$node;
		if (this.ndata != null) {
			for (int i=0; i < this.ndata.length; i++) {
				AttachInfo ai = this.ndata[i];
				if (!ai.p_slot.is_attr)
					continue;
				ANode nd = ai.p_self.nodeCopiedTo(node);
				if (nd == null)
					continue;
				if (node.ndata == null) {
					node.ndata = new AttachInfo[]{new AttachInfo(nd,node,ai.p_slot)};
				} else {
					int sz = node.ndata.length;
					AttachInfo[] tmp = new AttachInfo[sz+1];
					for (int j=0; j < sz; j++)
						tmp[j] = node.ndata[j];
					tmp[sz] = new AttachInfo(nd,node,ai.p_slot);
					node.ndata = tmp;
				}
				nd.callbackAttached(node, ai.p_slot);
			}
		}
		return node;
	}

}

public class TreeWalker {
	public boolean pre_exec(ANode n) { return true; }
	public void post_exec(ANode n) {}
}

public interface SetBody {
	public boolean setBody(ENode body);
}

class AttachInfo {
	final   ANode		p_self;
	final   ANode		p_parent;
	final   AttrSlot	p_slot;
	private ANode		p_ctx_root;
	AttachInfo(ANode self, ANode parent, AttrSlot slot) {
		this.p_self = self;
		this.p_parent = parent;
		this.p_slot = slot;
	}
	ANode get_ctx_root() {
		if (this.p_ctx_root != null)
			return this.p_ctx_root;
		ANode root = p_parent.get$ctx_root();
		this.p_ctx_root = root;
		return root;
	}
	ANode next() { return null; }
	ANode prev() { return null; }
	void detach() {
		this.p_ctx_root = null;
	}
};

class ListAttachInfo extends AttachInfo {
	private ListAttachInfo	p_prev;
	private ListAttachInfo	p_next;
	ListAttachInfo(ANode self, ANode parent, AttrSlot slot, ListAttachInfo prev, ListAttachInfo next) {
		super(self,parent,slot);
		if (prev != null) {
			this.p_prev = prev;
			prev.p_next = this;
		}
		if (next != null) {
			next.p_prev = this;
			this.p_next = next;
		}
	}
	ANode next() { return p_next == null ? null : p_next.p_self; }
	ANode prev() { return p_prev == null ? null : p_prev.p_self; }
	void detach() {
		if (p_prev != null)
			p_prev.p_next = p_next;
		if (p_next != null)
			p_next.p_prev = p_prev;
		super.detach();
	}
};
	

@node
public abstract class ASTNode extends ANode implements Constants, Cloneable {

	@virtual typedef This  = ASTNode;
	@virtual typedef VView = NodeView;
	@virtual typedef JView = JNode;
	@virtual typedef RView = RNode;
	
	public static ASTNode[] emptyArray = new ASTNode[0];
    public static final AttrSlot nodeattr$flags = new AttrSlot("flags", false, false, Integer.TYPE);

	public  int				pos;
	public  int				compileflags;

	// Structures	
	public @packed:1,compileflags,16 boolean is_struct_local;
	public @packed:1,compileflags,17 boolean is_struct_anomymouse;
	public @packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
	public @packed:1,compileflags,19 boolean is_struct_members_generated;
	public @packed:1,compileflags,20 boolean is_struct_pre_generated;
	public @packed:1,compileflags,21 boolean is_struct_statements_generated;
	public @packed:1,compileflags,22 boolean is_struct_generated;
	public @packed:1,compileflags,23 boolean is_struct_type_resolved;
	public @packed:1,compileflags,24 boolean is_struct_args_resolved;
	public @packed:1,compileflags,25 boolean is_struct_bytecode;	// struct was loaded from bytecode
	public @packed:1,compileflags,26 boolean is_struct_singleton;
	public @packed:1,compileflags,27 boolean is_struct_pizza_case;
	
	// Expression flags
	public @packed:1,compileflags,16 boolean is_expr_use_no_proxy;
	public @packed:1,compileflags,17 boolean is_expr_as_field;
	public @packed:1,compileflags,18 boolean is_expr_gen_void;
	public @packed:1,compileflags,19 boolean is_expr_for_wrapper;
	public @packed:1,compileflags,20 boolean is_expr_primary;
	public @packed:1,compileflags,21 boolean is_expr_super;
	public @packed:1,compileflags,22 boolean is_expr_cast_call;
	// Statement flags
	public @packed:1,compileflags,23 boolean is_stat_abrupted;
	public @packed:1,compileflags,24 boolean is_stat_breaked;
	public @packed:1,compileflags,25 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	public @packed:1,compileflags,26 boolean is_stat_auto_returnable;
	public @packed:1,compileflags,27 boolean is_stat_break_target;
	
	// Method flags
	public @packed:1,compileflags,17 boolean is_mth_virtual_static;
	public @packed:1,compileflags,18 boolean is_mth_operator;
	public @packed:1,compileflags,19 boolean is_mth_need_fields_init;
	public @packed:1,compileflags,20 boolean is_mth_local;
	public @packed:1,compileflags,21 boolean is_mth_dispatcher;
	public @packed:1,compileflags,22 boolean is_mth_invariant;
	
	// Var/field
	public @packed:1,compileflags,16 boolean is_init_wrapper;
	public @packed:1,compileflags,17 boolean is_need_proxy;
	// Var specific
	public @packed:1,compileflags,18 boolean is_var_local_rule_var;
	public @packed:1,compileflags,19 boolean is_var_closure_proxy;
	public @packed:1,compileflags,20 boolean is_var_this;
	public @packed:1,compileflags,21 boolean is_var_super;

	// Field specific
	public @packed:1,compileflags,18 boolean is_fld_packer;
	public @packed:1,compileflags,19 boolean is_fld_packed;
	public @packed:1,compileflags,20 boolean is_fld_added_to_init;

	// General flags
	public @packed:1,compileflags,28 boolean is_accessed_from_inner;
	public @packed:1,compileflags,29 boolean is_resolved;
	public @packed:1,compileflags,30 boolean is_hidden;
	public @packed:1,compileflags,31 boolean is_bad;

	public AttrSlot[] values() {
		return AttrSlot.emptyArray;
	}
	public Object getVal(String name) {
		throw new RuntimeException("No @att value \"" + name + "\" in NodeImpl");
	}
	public void setVal(String name, Object val) {
		throw new RuntimeException("No @att value \"" + name + "\" in NodeImpl");
	}
		
	public Object copyTo(Object to$node) {
		ASTNode node = (ASTNode)super.copyTo(to$node);
		node.pos			= this.pos;
		node.compileflags	= this.compileflags;
		return node;
	}

	public final int getPosLine() { return pos >>> 11; }
	
	public ANode nodeCopiedTo(ANode node) {
		return ncopy();
	}

	// build data flow for this node
	public final DataFlowInfo getDFlow() {
		DataFlowInfo df = (DataFlowInfo)getNodeData(DataFlowInfo.ATTR);
		if (df == null) {
			df = DataFlowInfo.newDataFlowInfo(this);
			this.addNodeData(df, DataFlowInfo.ATTR);
		}
		return df;
	}

	public final ASTNode replaceWithNode(ASTNode node) {
		assert(isAttached());
		if (pslot().is_space) {
			assert(node != null);
			NArr<ASTNode> space = (NArr<ASTNode>)parent().getVal(pslot().name);
			int idx = space.indexOf(this);
			assert(idx >= 0);
			if (node.pos == 0) node.pos = this.pos;
			space[idx] = node;
		}
		else if (pslot().isData()) {
			assert(((ASTNode)parent()).getNodeData(pslot()) == this);
			if (node != null && node.pos == 0) node.pos = this.pos;
			((ASTNode)parent()).addNodeData(node, pslot());
		}
		else {
			assert(parent().getVal(pslot().name) == this);
			if (node != null && node.pos == 0) node.pos = this.pos;
			parent().setVal(pslot().name, node);
		}
		assert(node == null || node.isAttached());
		return node;
	}
	public final ASTNode replaceWith(()->ASTNode fnode) {
		assert(isAttached());
		ASTNode parent = this.parent();
		AttrSlot pslot = this.getAttachInfo().p_slot;
		if (pslot.is_space) {
			NArr<ASTNode> space = (NArr<ASTNode>)parent.getVal(pslot.name);
			int idx = space.indexOf(this);
			assert(idx >= 0);
			space[idx] = this.getDummyNode();
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			space[idx] = n;
			assert(n.isAttached());
			return n;
		}
		else if (pslot.isData()) {
			assert(parent.getNodeData(pslot) == this);
			parent.delNodeData(pslot);
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			parent.addNodeData(n, pslot);
			assert(n.isAttached());
			return n;
		}
		else {
			assert(parent.getVal(pslot.name) == this);
			parent.setVal(pslot.name, this.getDummyNode());
			ASTNode n = fnode();
			if (n != null && n.pos == 0) n.pos = this.pos;
			parent.setVal(pslot.name, n);
			assert(n == null || n.isAttached());
			return n;
		}
	}

	// break target (ENodes)
	public final boolean isBreakTarget() {
		return this.is_stat_break_target;
	}
	public final void setBreakTarget(boolean on) {
		if (this.is_stat_break_target != on) {
			this.is_stat_break_target = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// the (private) field/method/struct is accessed from inner class (and needs proxy access)
	@getter public final boolean isAccessedFromInner() {
		return this.is_accessed_from_inner;
	}
	@setter public final void setAccessedFromInner(boolean on) {
		if (this.is_accessed_from_inner != on) {
			this.is_accessed_from_inner = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// resolved
	@getter public final boolean isResolved() {
		return this.is_resolved;
	}
	@setter public final void setResolved(boolean on) {
		if (this.is_resolved != on) {
			this.is_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// hidden
	@getter public final boolean isHidden() {
		return this.is_hidden;
	}
	@setter public final void setHidden(boolean on) {
		if (this.is_hidden != on) {
			this.is_hidden = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// bad
	@getter public final boolean isBad() {
		return this.is_bad;
	}
	@setter public final void setBad(boolean on) {
		if (this.is_bad != on) {
			this.is_bad = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	public final void cleanDFlow() {
		walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) n.delNodeData(DataFlowInfo.ATTR); return true; }
		});
	}
	
	public Type getType() { return Type.tpVoid; }

	@nodeview
	public static abstract view NodeView of ASTNode implements Constants {
		public String toString();
		public Dumper toJava(Dumper dmp);
		
		public int			pos;
		public int			compileflags;
		
		@getter public final ANode get$ctx_root();
		@getter public final FileUnit get$ctx_file_unit();
		@getter public final Struct get$ctx_clazz();
		@getter public final Struct get$child_ctx_clazz();
		@getter public final Method get$ctx_method();
		@getter public final Method get$child_ctx_method();

		public final ANode parent();
		public final AttrSlot pslot();
		public AttrSlot[] values();
		public Object getVal(String name);
		public void setVal(String name, Object val);
		public final void callbackChildChanged(AttrSlot attr);
		public final void callbackRootChanged();
		public final ANode getNodeData(AttrSlot attr);
		public final void addNodeData(ANode d, AttrSlot attr);
		public final void delNodeData(AttrSlot attr);
		public DataFlowInfo getDFlow();
		public final ASTNode replaceWithNode(ASTNode node);
		public final ASTNode replaceWith(()->ASTNode fnode);
		public final boolean isAttached();
		public final boolean isBreakTarget();
		public final void    setBreakTarget(boolean on);
		public final boolean isAccessedFromInner();
		public final void    setAccessedFromInner(boolean on);
		public final boolean isResolved();
		public final void    setResolved(boolean on);
		public final boolean isHidden();
		public final void    setHidden(boolean on);
		public final boolean isBad();
		public final void    setBad(boolean on);

		public final Type getType();

		public boolean preResolveIn() { return true; }
		public void preResolveOut() {}
		public boolean mainResolveIn() { return true; }
		public void mainResolveOut() {}
		public boolean preVerify() { return true; }
		public void postVerify() {}
	}
	
	public ASTNode() {}

	public final This detach()
		alias operator (210,fy,~)
	{
		if (!isAttached())
			return this;
		if (pslot().is_space) {
			((NArr<ASTNode>)parent().getVal(pslot().name)).detach(this);
		}
		else if (pslot().isData()) {
			((ASTNode)parent()).delNodeData(pslot());
		}
		else {
			parent().setVal(pslot().name,null);
		}
		assert(!isAttached());
		return this;
	}
	
	public abstract ASTNode getDummyNode();
	
	public final This ncopy() {
		return (This)this.copy();
	}
	public abstract Object copy();

    public Dumper toJava(Dumper dmp) {
    	dmp.append("/* INTERNAL ERROR - ").append(this.getClass().toString()).append(" */");
    	return dmp;
    }
	
	public DFFunc newDFFuncIn(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncIn() for "+getClass()); }
	public DFFunc newDFFuncOut(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncOut() for "+getClass()); }
	public DFFunc newDFFuncTru(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncTru() for "+getClass()); }
	public DFFunc newDFFuncFls(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncFls() for "+getClass()); }

	public final boolean preResolveIn() { return ((VView)this).preResolveIn(); }
	public final void preResolveOut() { ((VView)this).preResolveOut(); }
	public final boolean mainResolveIn() { return ((VView)this).mainResolveIn(); }
	public final void mainResolveOut() { ((VView)this).mainResolveOut(); }
	public final boolean preVerify() { return ((VView)this).preVerify(); }
	public final void postVerify() { ((VView)this).postVerify(); }

	public final boolean preGenerate() { return ((RView)this).preGenerate(); }
	
}


@node
public class SymbolRef extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SymbolRef;
	@virtual typedef VView = VSymbolRef;
	@virtual typedef JView = JSymbolRef;
	@virtual typedef RView = RSymbolRef;

	@att public String		name; // unresolved name
	@ref public Symbol		symbol; // resolved symbol

	@nodeview
	public static view VSymbolRef of SymbolRef extends NodeView {
		public String	name;
		public Symbol	symbol;
	}

	public SymbolRef() {}

	public SymbolRef(String name) {
		this.name = name;
	}

	public SymbolRef(int pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	public SymbolRef(int pos, Symbol id) {
		this.pos = pos;
		this.name = id.sname;
		this.symbol = id;
	}

	public boolean equals(Object nm) {
		if (nm instanceof Symbol) return nm == this.name;
		if (nm instanceof SymbolRef) return nm.name == this.name;
		if (nm instanceof String) return nm == this.name;
		if (nm instanceof KString) {
			Kiev.reportWarning(this,"Compare SymbolRef.equals(KString)")
		}
		return false;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = t.image;
	}
	
	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public String toString() { return symbol == null ? name : symbol.toString(); }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}
}

public class CompilerException extends RuntimeException {
	public ASTNode	from;
	public CError	err_id;
	public CompilerException(String msg) {
		super(msg);
	}
	public CompilerException(ASTNode from, String msg) {
		super(msg);
		this.from = from;
	}
	public CompilerException(ASTNode from, CError err_id, String msg) {
		super(msg);
		this.from = from;
		this.err_id = err_id;
	}
	public CompilerException(ASTNode.NodeView from, String msg) {
		super(msg);
		this.from = (ASTNode)from;
	}
	public CompilerException(ASTNode.NodeView from, CError err_id, String msg) {
		super(msg);
		this.from = (ASTNode)from;
		this.err_id = err_id;
	}
	public CompilerException(JNode from, String msg) {
		super(msg);
		this.from = (ASTNode)from;
	}
	public CompilerException(JNode from, CError err_id, String msg) {
		super(msg);
		this.from = (ASTNode)from;
		this.err_id = err_id;
	}
}

public class ReWalkNodeException extends RuntimeException {
	public static final ReWalkNodeException instance = new ReWalkNodeException();
	private ReWalkNodeException() {}
}
