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
package kiev.vtree;

import syntax kiev.Syntax;

import java.util.IdentityHashMap;

/**
 * @author Maxim Kizub
 *
 */

public enum ProjectAction {
	INIT,				// initial projection for source ports, old_value=null, new_value=null
	SET_SCALAR,			// update projection, set the scalar value, argument is Object
	SET_ELEMENT,		// update projection, set element at the i-th position of space, argument is Object, idx=i
	DEL_ELEMENT,		// update projection, delete element at the i-th position of space, argument is Object, idx=i
	INS_ELEMENT,		// update projection, insert element at the i-th position of space, argument is Object, idx=i
	UPDATED				// notify the projection about node's data update
}

public final class ProjectionValue {
	// usually AttrSlot.name
	public String id;
	public Object value;
	public ProjectionValue(String id, Object value) {
		this.id = id;
		this.value = value; 
	}
}

public class ProjectionContext extends Context {
	public NodeProjectorFactory npfactory;
	
	public ProjectionContext() {
	}

	public NodeProjectorFactory getProjectonFactory() {
		return npfactory;
	}
}

public interface Projection {
	public String        getProjId();
	public void          setSrcProj(Projection proj);
	public void          setDstProj(Projection proj);
	public void          project(ProjectAction act, ProjectionValue value, int idx);
	public void          putback(ProjectAction act, ProjectionValue value, int idx);
	public void          dispose();
}

public interface ProjSrcPort extends Projection {
	public AttrSlot      getAttrSlot();
}

public interface ProjDstPort extends Projection {
	public AttrSlot      getAttrSlot();
}

public interface NodeProjector {
	public void          project();
	public void          putback();
	public INode         getDstNode();
	public INode         getSrcNode();
	public void          dispose();
}

public interface RootNodeProjector extends NodeProjector {
}

public interface NodeProjectorFactory {
	public ProjectionContext getProjectionContext();
	public RootNodeProjector getRootProjector(INode src_root, INode dst_root);
	public NodeProjector     getProjector(INode src_node, INode dst_node);
	public INode             projectNode(INode src);	// make src->dst projection
	public INode             putbackNode(INode dst);	// make dst->src putback
	public void              dispose();
	public void              setListener(ChangeListener listener);
	public void              notifyProjectionChanged();
}


public abstract class AbstractNodeProjectorFactory implements NodeProjectorFactory {
	final
	protected ProjectionContext                       projection_context;
	protected IdentityHashMap<INode,NodeProjector>    allProjectors              = new IdentityHashMap<INode,NodeProjector>();
	protected DefaultRootNodeProjector                root_projector;
	protected ChangeListener                          listener;

	public AbstractNodeProjectorFactory(ProjectionContext projection_context) {
		this.projection_context = projection_context;
		projection_context.npfactory = this;
	}
	
	public abstract NodeProjector getProjector(INode src, INode dst);
	
	public ProjectionContext getProjectionContext() {
		return projection_context;
	}
	
	public RootNodeProjector getRootProjector(INode src_root, INode dst_root) {
		if (root_projector == null)
			root_projector = new DefaultRootNodeProjector(this, src_root, dst_root);
		return root_projector;
	}

	public INode projectNode(INode src) {
		if (src == null)
			return null;
		NodeProjector np = allProjectors.get(src);
		if (np != null)
			return np.getDstNode();
		NodeProjector np = this.getProjector(src, null);
		np.project();
		return np.getDstNode();
	}

	public INode putbackNode(INode dst) {
		if (dst == null)
			return null;
		NodeProjector np = allProjectors.get(dst);
		if (np != null)
			return np.getSrcNode();
		NodeProjector np = this.getProjector(null, dst);
		np.putback();
		return np.getSrcNode();
	}

	public void dispose() {
		this.listener = null;
		if (root_projector != null) {
			root_projector.dispose();
			root_projector = null;
		}
		if (!allProjectors.isEmpty()) {
			NodeProjector[] projectors = allProjectors.values().toArray(new NodeProjector[0]);
			allProjectors.clear();
			foreach (NodeProjector p; projectors)
				p.dispose();
		}
	}

	public void setListener(ChangeListener listener) {
		this.listener = listener;
	}
	public void notifyProjectionChanged() {
		if (this.listener != null)
			this.listener.callbackNodeChanged(new NodeChangeInfo(ChangeType.ATTR_UPDATED, root_projector.getDstNode(), null, null, null, -1));
	}
}

public class DefaultNodeProjectorFactory extends AbstractNodeProjectorFactory {
	protected Hashtable<String,NodeTypeInfo>          allProjectionNodeTypeInfos = new Hashtable<String,NodeTypeInfo>();
	
	public DefaultNodeProjectorFactory() {
		this(new ProjectionContext());
	}
	
	public DefaultNodeProjectorFactory(ProjectionContext projection_context) {
		this(projection_context);
	}
	
	public void addProjectionNodeTypeInfo(String src_id, NodeTypeInfo nti) {
		allProjectionNodeTypeInfos.put(src_id, nti);
	}

	public NodeProjector getProjector(INode src, INode dst) {
		NodeProjector np = null;
		if (src != null) {
			np = allProjectors.get(src);
			if (np != null)
				return np;
			NodeTypeInfo src_nti = src.getNodeTypeInfo();
			NodeTypeInfo dst_nti = allProjectionNodeTypeInfos.get(src_nti.getId());
			INode dst;
			if (dst_nti == null)
				dst = new PNode(src_nti.getProxy());
			else
				dst = dst_nti.newInstance();
			np = new ProxyNodeProjector(this, src, dst);
			allProjectors.put(src, np);
			allProjectors.put(dst, np);
			return np;
		}
		if (dst != null) {
			np = allProjectors.get(dst);
			if (np != null)
				return np;
			throw new RuntimeException("Cannot get projection to putback node");
		}
		throw new RuntimeException("Cannot get projector for null");
	}
}

public class DefaultRootNodeProjector implements RootNodeProjector {
	
	public NodeProjectorFactory npfactory;
	public INode src_root;
	public INode dst_root;
	
	public DefaultRootNodeProjector(NodeProjectorFactory npfactory, INode src_root, INode dst_root) {
		this.npfactory = npfactory;
		this.src_root = src_root;
		this.dst_root = dst_root;
	}
	
	public void project() {
		dst_root = npfactory.projectNode(src_root);
	}

	public void putback() {
		src_root = npfactory.putbackNode(dst_root);
	}

	public INode getSrcNode() {
		return src_root;
	}

	public INode getDstNode() {
		return dst_root;
	}

	public void dispose() {
		src_root = null;
		dst_root = null;
	}

}

public final class IgnoredNodeChangeInfo {
	private final INode    parent;
	private final AttrSlot slot;
	private final Object   value;
	private final int      idx;
	public IgnoredNodeChangeInfo(INode parent, AttrSlot slot, Object value, int idx) {
		this.parent = parent;
		this.slot = slot;
		this.value = value;
		this.idx = idx;
	}
	public boolean match(NodeChangeInfo info) {
		if (info.parent != this.parent || info.slot != this.slot || info.idx != this.idx)
			return false;
		if (info.new_value == null || this.value == null || info.new_value instanceof INode)
			return (this.value == info.new_value);
		return info.new_value.equals(this.value);
	}
}

public class WrapProjection implements Projection, ChangeListener {
	private final String            proj_id;
	private final AttrSlot          wr_slot;
	private final INode             wrapper;
	private Projection              src_port;
	private Projection              dst_port;
	private IgnoredNodeChangeInfo   ignore_notification;
	private boolean                 projected;

	public WrapProjection(String proj_id, AttrSlot wr_slot, INode wrapper) {
		this.proj_id = proj_id;
		this.wr_slot = wr_slot;
		this.wrapper = wrapper;
		this.wrapper.asANode().addListener(this);
	}
	public String getProjId() { return proj_id; }
	public void setSrcProj(Projection proj) { this.src_port = proj; }
	public void setDstProj(Projection proj) { this.dst_port = proj; }
	public void project(ProjectAction act, ProjectionValue value, int idx) {
		Object projected_value;
		assert (value.id == proj_id);
		switch (act) {
		case ProjectAction.SET_SCALAR:
			if (value.value == null && !projected)
				return;
			ignoreCallbackEvent(value.value,-1);
			((ScalarAttrSlot)wr_slot).set(wrapper.asANode(),value.value);
			dst_port.project(projected ? ProjectAction.UPDATED : ProjectAction.SET_SCALAR, new ProjectionValue(proj_id,wrapper), -1);
			projected = true;
			return;
		case ProjectAction.SET_ELEMENT:
			ignoreCallbackEvent(value.value,idx);
			((SpaceAttrSlot)wr_slot).set(wrapper.asANode(),idx,((INode)value.value).asANode());
			dst_port.project(ProjectAction.UPDATED, new ProjectionValue(proj_id,wrapper), -1);
			return;
		case ProjectAction.DEL_ELEMENT:
			ignoreCallbackEvent(null,idx);
			((SpaceAttrSlot)wr_slot).del(wrapper,idx);
			dst_port.project(ProjectAction.UPDATED, new ProjectionValue(proj_id,wrapper), -1);
			return;
		case ProjectAction.INS_ELEMENT:
			ignoreCallbackEvent(value.value,idx);
			((SpaceAttrSlot)wr_slot).insert(wrapper,idx,(INode)value.value);
			dst_port.project(projected ? ProjectAction.UPDATED : ProjectAction.SET_SCALAR, new ProjectionValue(proj_id,wrapper), -1);
			projected = true;
			return;
		case ProjectAction.UPDATED:
			if (wrapper != null)
				dst_port.project(ProjectAction.UPDATED, new ProjectionValue(proj_id,wrapper), -1);
			return;
		}
		throw new RuntimeException("Unknown projection action "+act);
	}
	public void putback(ProjectAction act, ProjectionValue value, int idx) {
		switch (act) {
		case ProjectAction.SET_SCALAR:
			if (wrapper == value.value)
				return;
			if (value.value == null) {
				// delete wrapper action
				projected = false;
				if (wr_slot instanceof ScalarAttrSlot) {
					src_port.putback(ProjectAction.SET_SCALAR, new ProjectionValue(proj_id,null), -1);
				}
				else if (wr_slot instanceof SpaceAttrSlot) {
					INode[] nodes = ((SpaceAttrSlot)wr_slot).getArray(wrapper);
					for (int i=nodes.length-1; i >= 0; i--)
						src_port.putback(ProjectAction.DEL_ELEMENT, new ProjectionValue(proj_id,null), i);
				}
				else if (wr_slot instanceof ExtSpaceAttrSlot) {
					Vector<INode> vect = new Vector<INode>();
					foreach (INode n; ((ExtSpaceAttrSlot)wr_slot).iterate(wrapper))
						vect.append(n);
					INode[] nodes = vect.toArray();
					for (int i=nodes.length-1; i >= 0; i--)
						src_port.putback(ProjectAction.DEL_ELEMENT, new ProjectionValue(proj_id,null), i);
				}
				return;
			}
			// insert wrapper action
			INode wr_ins = (INode)value.value;
			if (wr_slot instanceof ScalarAttrSlot) {
				Object wr_value = ((ScalarAttrSlot)wr_slot).get(wr_ins);
				if (wr_value instanceof INode && wr_value.isAttached())
					wr_value.detach();
				((ScalarAttrSlot)wr_slot).set(wrapper,wr_value);
			}
			else if (wr_slot instanceof SpaceAttrSlot) {
				((SpaceAttrSlot)wr_slot).delAll(wrapper);
				INode[] nodes = ((SpaceAttrSlot)wr_slot).delToArray(wr_ins);
				for (int i=0; i < nodes.length; i++)
					((SpaceAttrSlot)wr_slot).add(wrapper, nodes[i]);
			}
			else if (wr_slot instanceof ExtSpaceAttrSlot) {
				((ExtSpaceAttrSlot)wr_slot).delAll(wrapper);
				foreach (INode n; ((ExtSpaceAttrSlot)wr_slot).iterate(wr_ins))
					((ExtSpaceAttrSlot)wr_slot).add(wrapper, n.detach());
			}
			dst_port.project(projected ? ProjectAction.UPDATED : ProjectAction.SET_SCALAR, new ProjectionValue(proj_id,wrapper), -1);
			projected = true;
			return;
		}
		throw new RuntimeException("Unknown putback action "+act);
	}
	private void ignoreCallbackEvent(Object value, int idx) {
		if (wrapper != null)
			this.ignore_notification = new IgnoredNodeChangeInfo(wrapper, wr_slot, value, idx);
	}
	// listener interface
	public void callbackNodeChanged(NodeChangeInfo info) {
		if (info.ct == ChangeType.ATTR_MODIFIED && info.slot == wr_slot) {
			assert (info.parent == wrapper);
			if (this.ignore_notification != null) {
				IgnoredNodeChangeInfo ignore_notification = this.ignore_notification;
				this.ignore_notification = null;
				if (ignore_notification.match(info))
					return;
			}
			if (wr_slot instanceof ScalarAttrSlot) {
				src_port.putback(ProjectAction.SET_SCALAR, new ProjectionValue(proj_id,info.new_value), -1);
			}
			else if (wr_slot instanceof SpaceAttrSlot) {
				if (info.new_value == null) 		// delete
					src_port.putback(ProjectAction.DEL_ELEMENT, new ProjectionValue(proj_id,null), info.idx);
				else if (info.old_value == null)	// insert
					src_port.putback(ProjectAction.INS_ELEMENT, new ProjectionValue(proj_id,info.new_value), info.idx);
				else								// replace
					src_port.putback(ProjectAction.SET_ELEMENT, new ProjectionValue(proj_id,info.new_value), info.idx);
			}
		}
	}
	public void dispose() {
		wrapper.asANode().delListener(this);
		projected = false;
		if (this.src_port != null) {
			Projection src_port = this.src_port;
			this.src_port = null;
			src_port.dispose();
		}
		if (this.dst_port != null) {
			Projection dst_port = this.dst_port;
			this.dst_port = null;
			dst_port.dispose();
		}
	}
}

public abstract class AbstractNodeProjector implements NodeProjector, ChangeListener {

	protected final NodeProjectorFactory    npfactory;
	protected       INode                   src_node;
	protected       INode                   dst_node;
	protected       Projection[]            ports;
	private IgnoredNodeChangeInfo   ignore_notification;

	public AbstractNodeProjector(NodeProjectorFactory npfactory, INode src_node, INode dst_node) {
		this.npfactory = npfactory;
		this.src_node = src_node;
		this.dst_node = dst_node;
		src_node.asANode().addListener(this);
		dst_node.asANode().addListener(this);
	}
	
	public abstract String getPutbackProjId(AttrSlot slot, Object value);
	
	public final void link(Projection... projections) {
		Projection prev = null;
		foreach (Projection proj; projections) {
			if (prev != null) {
				prev.setDstProj(proj);
				proj.setSrcProj(prev);
			}
			prev = proj;
		}
	}

	public void project() {
		Projection[] ports = this.ports;
		if (ports != null) {
			foreach (ProjSrcPort p; ports)
				p.project(ProjectAction.INIT,null,-1);
		}
	}

	public void putback() {
		Projection[] ports = this.ports;
		if (ports != null) {
			foreach (ProjDstPort p; ports)
				p.putback(ProjectAction.INIT,null,-1);
		}
	}

	public INode getSrcNode() {
		return src_node;
	}

	public INode getDstNode() {
		return dst_node;
	}

	public void dispose() {
		if (src_node != null) {
			src_node.asANode().delListener(this);
			src_node = null;
		}
		if (dst_node != null) {
			dst_node.asANode().delListener(this);
			dst_node = null;
		}
		Projection[] ports = this.ports;
		this.ports = null;
		if (ports != null) {
			foreach (Projection p; ports)
				p.dispose();
		}
	}
	
	protected void ignoreSrcCallbackEvent(AttrSlot slot, Object value, int idx) {
		this.ignore_notification = new IgnoredNodeChangeInfo(src_node, slot, value, idx);
	}

	protected void ignoreDstCallbackEvent(AttrSlot slot, Object value, int idx) {
		this.ignore_notification = new IgnoredNodeChangeInfo(dst_node, slot, value, idx);
	}

	// listener interface
	public void callbackNodeChanged(NodeChangeInfo info) {
		if (info.ct != ChangeType.ATTR_MODIFIED)
			return;
		if (this.ignore_notification != null) {
			IgnoredNodeChangeInfo ignore_notification = this.ignore_notification;
			this.ignore_notification = null;
			if (ignore_notification.match(info))
				return;
		}
		INode node = info.parent;
		AttrSlot slot = info.slot;
		if (node == src_node) {
			foreach (ProjSrcPort p; ports; p.getAttrSlot() == slot) {
				if (slot instanceof ScalarAttrSlot) {
					p.project(ProjectAction.SET_SCALAR,new ProjectionValue(slot.name,info.new_value),-1);
				}
				else if (slot instanceof SpaceAttrSlot) {
					if (info.old_value == null)
						p.project(ProjectAction.INS_ELEMENT, new ProjectionValue(slot.name,info.new_value), info.idx);
					else if (info.new_value == null)
						p.project(ProjectAction.DEL_ELEMENT, new ProjectionValue(slot.name,null), info.idx);
					else
						p.project(ProjectAction.SET_ELEMENT, new ProjectionValue(slot.name,info.new_value), info.idx);
				}
			}
		}
		else if (node == dst_node) {
			foreach (ProjDstPort p; ports; p.getAttrSlot() == slot) {
				if (slot instanceof ScalarAttrSlot) {
					String id = getPutbackProjId(slot,info.new_value);
					p.putback(ProjectAction.SET_SCALAR,new ProjectionValue(id,info.new_value),-1);
				}
				else if (slot instanceof SpaceAttrSlot) {
					String id = getPutbackProjId(slot,info.new_value);
					if (info.old_value == null)
						p.putback(ProjectAction.INS_ELEMENT, new ProjectionValue(id,info.new_value), info.idx);
					else if (info.new_value == null)
						p.putback(ProjectAction.DEL_ELEMENT, new ProjectionValue(id,null), info.idx);
					else
						p.putback(ProjectAction.SET_ELEMENT, new ProjectionValue(id,info.new_value), info.idx);
				}
			}
		}
	}

	public class ScalarSrcPort implements ProjSrcPort {
		private final ScalarAttrSlot src_slot;	// source slot in XMLNodeProjector.this.src_node
		private       Projection     dst_proj;	// attached destination projection
		
		public ScalarSrcPort(ScalarAttrSlot src_slot) {
			this.src_slot = src_slot;
		}
		public AttrSlot   getAttrSlot() { src_slot }
		public String     getProjId() { return src_slot.name; }
		public void       setSrcProj(Projection proj) { throw new RuntimeException("Set source projection"); }
		public void       setDstProj(Projection proj) { this.dst_proj = proj; }
		
		public void project(ProjectAction act, ProjectionValue value, int idx) {
			switch (act) {
			case ProjectAction.INIT:
				if (src_slot.isChild())
					value = new ProjectionValue(src_slot.name,npfactory.projectNode((INode)src_slot.get(src_node)));
				else
					value = new ProjectionValue(src_slot.name,src_slot.get(src_node));
				dst_proj.project(ProjectAction.SET_SCALAR,value,-1);
				return;
			case ProjectAction.SET_SCALAR:
				assert (value.id == src_slot.name);
				if (src_slot.isChild())
					dst_proj.project(ProjectAction.SET_SCALAR,new ProjectionValue(src_slot.name,npfactory.projectNode((INode)value.value)),-1);
				else
					dst_proj.project(ProjectAction.SET_SCALAR,new ProjectionValue(src_slot.name,value.value),-1);
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void putback(ProjectAction act, ProjectionValue value, int idx) {
			assert (value.id == src_slot.name);
			switch (act) {
			case ProjectAction.SET_SCALAR:
				if (src_slot.isWrittable()) {
					Object val;
					if (src_slot.isChild())
						val = npfactory.putbackNode((INode)value.value);
					else
						val = value.value;
					ignoreSrcCallbackEvent(src_slot, val, -1);
					src_slot.set(src_node,val);
				} else {
					Object roval = src_slot.get(src_node);
					if (src_slot.isChild())
						dst_proj.project(ProjectAction.SET_SCALAR,new ProjectionValue(src_slot.name,npfactory.projectNode((INode)roval)),-1);
					else
						dst_proj.project(ProjectAction.SET_SCALAR,new ProjectionValue(src_slot.name,roval),-1);
					if (src_slot.isChild() && value.value != null) {
						CopyContext cc = new Copier();
						INode node = npfactory.putbackNode((INode)value.value);
						node.asANode().copyTo(roval, cc);
						cc.updateLinks();
					}
				}
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void dispose() {
			if (this.dst_proj != null) {
				Projection dst_proj = this.dst_proj;
				this.dst_proj = null;
				dst_proj.dispose();
			}
		}
	}
	public class SpaceSrcPort implements ProjSrcPort {
		private final SpaceAttrSlot  src_slot;	// source slot in XMLNodeProjector.this.src_node
		private       Projection     dst_proj;	// attached destination projection
		
		public SpaceSrcPort(SpaceAttrSlot src_slot) {
			this.src_slot = src_slot;
		}
		public AttrSlot   getAttrSlot() { src_slot }
		public String     getProjId() { return src_slot.name; }
		public void       setSrcProj(Projection proj) { throw new RuntimeException("Set source projection"); }
		public void       setDstProj(Projection proj) { this.dst_proj = proj; }
		
		public void project(ProjectAction act, ProjectionValue value, int idx) {
			switch (act) {
			case ProjectAction.INIT:
				idx = 0;
				foreach (INode n; src_slot.getArray(src_node)) {
					if (src_slot.isChild())
						n = npfactory.projectNode(n);
					dst_proj.project(ProjectAction.INS_ELEMENT,new ProjectionValue(src_slot.name,n),idx);
					idx += 1;
				}
				return;
			case ProjectAction.SET_ELEMENT:
				assert (value.id == src_slot.name);
				if (src_slot.isChild())
					dst_proj.project(ProjectAction.SET_ELEMENT,new ProjectionValue(src_slot.name,npfactory.projectNode((INode)value.value)),idx);
				else
					dst_proj.project(ProjectAction.SET_ELEMENT,value,idx);
				return;
			case ProjectAction.DEL_ELEMENT:
				assert (value.id == src_slot.name);
				dst_proj.project(ProjectAction.DEL_ELEMENT,value,idx);
				return;
			case ProjectAction.INS_ELEMENT:
				assert (value.id == src_slot.name);
				if (src_slot.isChild())
					dst_proj.project(ProjectAction.INS_ELEMENT,new ProjectionValue(src_slot.name,npfactory.projectNode((INode)value.value)),idx);
				else
					dst_proj.project(ProjectAction.INS_ELEMENT,value,idx);
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void putback(ProjectAction act, ProjectionValue value, int idx) {
			assert (value.id == src_slot.name);
			INode val = (INode)value.value;
			switch (act) {
			case ProjectAction.SET_ELEMENT:
				if (src_slot.isChild())
					val = npfactory.putbackNode(val);
				ignoreSrcCallbackEvent(src_slot, val, idx);
				src_slot.set(src_node, idx, val);
				return;
			case ProjectAction.DEL_ELEMENT:
				ignoreSrcCallbackEvent(src_slot, null, idx);
				src_slot.del(src_node, idx);
				return;
			case ProjectAction.INS_ELEMENT:
				if (src_slot.isChild())
					val = npfactory.putbackNode(val);
				ignoreSrcCallbackEvent(src_slot, val, idx);
				src_slot.insert(src_node, idx, val);
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void dispose() {
			if (this.dst_proj != null) {
				Projection dst_proj = this.dst_proj;
				this.dst_proj = null;
				dst_proj.dispose();
			}
		}
	}
	public class ExtSpaceSrcPort implements ProjSrcPort {
		private final ExtSpaceAttrSlot  src_slot;	// source slot in XMLNodeProjector.this.src_node
		private       Projection        dst_proj;	// attached destination projection
		
		public ExtSpaceSrcPort(ExtSpaceAttrSlot src_slot) {
			this.src_slot = src_slot;
		}
		public AttrSlot   getAttrSlot() { src_slot }
		public String     getProjId() { return src_slot.name; }
		public void       setSrcProj(Projection proj) { throw new RuntimeException("Set source projection"); }
		public void       setDstProj(Projection proj) { this.dst_proj = proj; }
		
		public void project(ProjectAction act, ProjectionValue value, int idx) {
			switch (act) {
			case ProjectAction.INIT:
				idx = 0;
				foreach (INode n; src_slot.iterate(src_node)) {
					if (src_slot.isChild())
						n = npfactory.projectNode(n);
					dst_proj.project(ProjectAction.INS_ELEMENT,new ProjectionValue(src_slot.name,n),idx++);
				}
				return;
			case ProjectAction.SET_ELEMENT:
				assert (value.id == src_slot.name);
				if (src_slot.isChild())
					dst_proj.project(ProjectAction.SET_ELEMENT,new ProjectionValue(src_slot.name,npfactory.projectNode((INode)value.value)),idx);
				else
					dst_proj.project(ProjectAction.SET_ELEMENT,value,idx);
				return;
			case ProjectAction.DEL_ELEMENT:
				assert (value.id == src_slot.name);
				dst_proj.project(ProjectAction.DEL_ELEMENT,value,idx);
				return;
			case ProjectAction.INS_ELEMENT:
				assert (value.id == src_slot.name);
				if (src_slot.isChild())
					dst_proj.project(ProjectAction.INS_ELEMENT,new ProjectionValue(src_slot.name,npfactory.projectNode((INode)value.value)),idx);
				else
					dst_proj.project(ProjectAction.INS_ELEMENT,value,idx);
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void putback(ProjectAction act, ProjectionValue value, int idx) {
			assert (value.id == src_slot.name);
			INode val = (INode)value.value;
			switch (act) {
			case ProjectAction.SET_ELEMENT:
				if (src_slot.isChild())
					val = npfactory.putbackNode(val);
				ignoreSrcCallbackEvent(src_slot, val, idx);
				src_slot.set(src_node, idx, val);
				return;
			case ProjectAction.DEL_ELEMENT:
				ignoreSrcCallbackEvent(src_slot, null, idx);
				src_slot.del(src_node,idx);
				return;
			case ProjectAction.INS_ELEMENT:
				if (src_slot.isChild())
					val = npfactory.putbackNode(val);
				ignoreSrcCallbackEvent(src_slot, val, idx);
				src_slot.insert(src_node, idx, val);
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void dispose() {
			if (this.dst_proj != null) {
				Projection dst_proj = this.dst_proj;
				this.dst_proj = null;
				dst_proj.dispose();
			}
		}
	}
	
	public class ScalarDstPort implements ProjDstPort {
		private final ScalarAttrSlot dst_slot;	// source slot in XMLNodeProjector.this.src_node
		private       Projection     src_proj;	// attached destination projection
		
		public ScalarDstPort(ScalarAttrSlot dst_slot) {
			this.dst_slot = dst_slot;
		}
		public AttrSlot   getAttrSlot() { dst_slot }
		public String     getProjId() { return dst_slot.name; }
		public void       setSrcProj(Projection proj) { this.src_proj = proj; }
		public void       setDstProj(Projection proj) { throw new RuntimeException("Set destination projection"); }
		
		public void project(ProjectAction act, ProjectionValue value, int idx) {
			switch (act) {
			case ProjectAction.SET_SCALAR:
				ignoreDstCallbackEvent(dst_slot, value.value, -1);
				dst_slot.set(dst_node,value.value);
				npfactory.notifyProjectionChanged();
				return;
			case ProjectAction.UPDATED:
				npfactory.notifyProjectionChanged();
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void putback(ProjectAction act, ProjectionValue value, int idx) {
			switch (act) {
			case ProjectAction.INIT:
				Object val = dst_slot.get(dst_node);
				String id = getPutbackProjId(dst_slot, val);
				value = new ProjectionValue(id,val);
				src_proj.putback(ProjectAction.SET_SCALAR,value,-1);
				return;
			case ProjectAction.SET_SCALAR:
				src_proj.putback(ProjectAction.SET_SCALAR,value,-1);
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void dispose() {
			if (this.src_proj != null) {
				Projection src_proj = this.src_proj;
				this.src_proj = null;
				src_proj.dispose();
			}
		}
	}
	public class SpaceDstPort implements ProjDstPort {
		private final SpaceAttrSlot  dst_slot;	// source slot in XMLNodeProjector.this.src_node
		private       Projection     src_proj;	// attached destination projection
		
		public SpaceDstPort(SpaceAttrSlot dst_slot) {
			this.dst_slot = dst_slot;
		}
		public AttrSlot   getAttrSlot() { dst_slot }
		public String     getProjId() { return dst_slot.name; }
		public void       setSrcProj(Projection proj) { this.src_proj = proj; }
		public void       setDstProj(Projection proj) { throw new RuntimeException("Set destination projection"); }
		
		public void project(ProjectAction act, ProjectionValue value, int idx) {
			switch (act) {
			case ProjectAction.SET_ELEMENT:
				ignoreDstCallbackEvent(dst_slot, value.value, idx);
				dst_slot.set(dst_node,idx,(INode)value.value);
				npfactory.notifyProjectionChanged();
				return;
			case ProjectAction.DEL_ELEMENT:
				ignoreDstCallbackEvent(dst_slot, null, idx);
				dst_slot.del(dst_node,idx);
				npfactory.notifyProjectionChanged();
				return;
			case ProjectAction.INS_ELEMENT:
				ignoreDstCallbackEvent(dst_slot, value.value, idx);
				dst_slot.insert(dst_node,idx,(INode)value.value);
				npfactory.notifyProjectionChanged();
				return;
			case ProjectAction.UPDATED:
				npfactory.notifyProjectionChanged();
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void putback(ProjectAction act, ProjectionValue value, int idx) {
			switch (act) {
			case ProjectAction.INIT:
				idx = 0;
				foreach (INode n; dst_slot.getArray(dst_node)) {
					String id = getPutbackProjId(dst_slot, n);
					src_proj.putback(ProjectAction.INS_ELEMENT,new ProjectionValue(id,n),idx);
					idx += 1;
				}
				return;
			case ProjectAction.SET_ELEMENT:
				src_proj.putback(ProjectAction.SET_ELEMENT,value,idx);
				return;
			case ProjectAction.DEL_ELEMENT:
				src_proj.putback(ProjectAction.DEL_ELEMENT,value,idx);
				return;
			case ProjectAction.INS_ELEMENT:
				src_proj.putback(ProjectAction.SET_ELEMENT,value,idx);
				return;
			}
			throw new RuntimeException("Unknown projection action "+act);
		}
		public void dispose() {
			if (this.src_proj != null) {
				Projection src_proj = this.src_proj;
				this.src_proj = null;
				src_proj.dispose();
			}
		}
	}
}

public class JoinProjection implements Projection {
	public static abstract class SrcInfo {
		static final SrcInfo[] emptyArray = new SrcInfo[0];
		public final String        proj_id;
		public final Projection    src_proj;
		SrcInfo(Projection proj) {
			this.proj_id = proj.getProjId();
			this.src_proj = proj;
		}
	}
	public static final class SrcScalarInfo extends SrcInfo {
		public int idx;
		SrcScalarInfo(Projection proj) {
			super(proj);
			idx = -1;
		}
	}
	
	protected SrcInfo[]     srcinfos = SrcInfo.emptyArray;
	protected Projection    dst_proj;
	
	public JoinProjection() {}
	
	public SrcInfo mapPutback(ProjectionValue value) {
		foreach (SrcInfo si; srcinfos; si.proj_id == value.id)
			return si;
		return null;
	}
	
	public String getProjId() { return ""; }
	public void setSrcProj(Projection proj) { srcinfos = (SrcInfo[])Arrays.append(srcinfos, new SrcScalarInfo(proj)); }
	public void setDstProj(Projection proj) { this.dst_proj = proj; }
	
	private SrcInfo getSrcInfo(String proj_id) {
		foreach (SrcInfo si; srcinfos; si.proj_id == proj_id)
			return si;
		return null;
	}
	
	private SrcInfo getSrcInfo(int idx) {
		foreach (SrcInfo si; srcinfos) {
			if (si instanceof SrcScalarInfo) {
				if (si.idx == idx)
					return si;
			} else {
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			}
		}
		return null;
	}
	
	private int getStartIndex(String proj_id) {
		// find existing index
		foreach (SrcInfo si; srcinfos; si.proj_id == proj_id) {
			if (si instanceof SrcScalarInfo) {
				if (si.idx >= 0)
					return si.idx;
				break;
			} else {
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			}
		}
		// allocate new index
		int idx = 0;
		foreach (SrcInfo si; srcinfos) {
			if (si instanceof SrcScalarInfo) {
				if (si.idx >= idx)
					idx = si.idx + 1;
			} else {
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			}
		}
		return idx;
	}

	private void insertScalarIndex(SrcScalarInfo ssi, int idx) {
		foreach (SrcInfo si; srcinfos) {
			if (si instanceof SrcScalarInfo) {
				if (si.idx >= idx)
					si.idx += 1;
			} else {
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			}
		}
		if (ssi != null)
			ssi.idx = idx;
	}

	private void deleteScalarIndex(SrcScalarInfo ssi, int idx) {
		foreach (SrcInfo si; srcinfos) {
			if (si instanceof SrcScalarInfo) {
				if (si.idx >= idx)
					si.idx -= 1;
			} else {
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			}
		}
		if (ssi != null)
			ssi.idx = -1;
	}

	public void project(ProjectAction act, ProjectionValue value, int idx) {
		SrcInfo si;
		switch (act) {
		case ProjectAction.SET_SCALAR:
			si = getSrcInfo(value.id);
			assert (si.proj_id == value.id);
			if (value.value == null) {
				if (((SrcScalarInfo)si).idx < 0)
					return;
				idx = getStartIndex(value.id);
				dst_proj.project(ProjectAction.DEL_ELEMENT, value, idx);
				deleteScalarIndex((SrcScalarInfo)si, idx);
			}
			else if (((SrcScalarInfo)si).idx < 0) {
				idx = getStartIndex(value.id);
				dst_proj.project(ProjectAction.INS_ELEMENT, value, idx);
				insertScalarIndex((SrcScalarInfo)si, idx);
			}
			else {
				idx = getStartIndex(value.id);
				dst_proj.project(ProjectAction.SET_ELEMENT, value, idx);
			}
			return;
		case ProjectAction.UPDATED:
			dst_proj.project(ProjectAction.UPDATED, null, -1);
			return;
		}
		throw new RuntimeException("Unknown projection action "+act);
	}

	public void putback(ProjectAction act, ProjectionValue value, int idx) {
		SrcInfo si;
		switch (act) {
		case ProjectAction.SET_ELEMENT:
			si = getSrcInfo(idx);
			if (si instanceof SrcScalarInfo)
				si.src_proj.putback(ProjectAction.SET_SCALAR, new ProjectionValue(si.proj_id,value.value), -1);
			else
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			return;
		case ProjectAction.DEL_ELEMENT:
			si = getSrcInfo(idx);
			if (si instanceof SrcScalarInfo) {
				si.src_proj.putback(ProjectAction.SET_SCALAR, new ProjectionValue(si.proj_id,null), -1);
				deleteScalarIndex((SrcScalarInfo)si, idx);
			} else
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			return;
		case ProjectAction.INS_ELEMENT:
			si = mapPutback(value);
			if (si == null) {
				insertScalarIndex(null, idx);
				return;
			}
			if (si instanceof SrcScalarInfo) {
				insertScalarIndex((SrcScalarInfo)si, idx);
				si.src_proj.putback(ProjectAction.SET_SCALAR, new ProjectionValue(si.proj_id,value.value), -1);
			} else
				throw new RuntimeException("Unknown source projection info "+si.getClass());
			return;
		}
		throw new RuntimeException("Unknown putback action "+act);
	}

	public void dispose() {
		if (this.dst_proj != null) {
			Projection dst_proj = this.dst_proj;
			this.dst_proj = null;
			dst_proj.dispose();
		}
		if (this.srcinfos != null) {
			SrcInfo[] srcinfos = this.srcinfos;
			this.srcinfos = null;
			foreach (SrcInfo si; srcinfos)
				si.src_proj.dispose();
		}
	}
}

public class ProxyNodeProjector implements NodeProjector, ChangeListener {

	protected final NodeProjectorFactory    npfactory;
	protected       INode                   src_node;
	protected       INode                   dst_node;
	private IgnoredNodeChangeInfo   ignore_notification;

	public ProxyNodeProjector(NodeProjectorFactory npfactory, INode src_node, INode dst_node) {
		this.npfactory = npfactory;
		this.src_node = src_node;
		this.dst_node = dst_node;
		src_node.asANode().addListener(this);
		dst_node.asANode().addListener(this);
	}

	public void project() {
		AttrSlot[] src_attrs = src_node.getNodeTypeInfo().getAllAttributes();
		AttrSlot[] dst_attrs = dst_node.getNodeTypeInfo().getAllAttributes();
		foreach (AttrSlot src_slot; src_attrs; src_slot.isAttr()) {
			foreach (AttrSlot dst_slot; dst_attrs; dst_slot.isAttr() && dst_slot.name == src_slot.name) {
				if (src_slot instanceof ScalarAttrSlot) {
					ScalarAttrSlot ss = (ScalarAttrSlot)src_slot;
					ScalarAttrSlot ds = (ScalarAttrSlot)dst_slot;
					Object val = ss.get(src_node);
					if (val instanceof INode)
						val = npfactory.projectNode((INode)val);
					ignoreDstCallbackEvent(ds, val, -1);
					ds.set(dst_node, val);
				}
				else if (src_slot instanceof SpaceAttrSlot) {
					SpaceAttrSlot ss = (SpaceAttrSlot)src_slot;
					SpaceAttrSlot ds = (SpaceAttrSlot)dst_slot;
					INode[] vals = ss.getArray(src_node);
					for (int i=0; i < vals.length; i++) {
						INode val = (INode)npfactory.projectNode((INode)vals[i]);
						ignoreDstCallbackEvent(ds, val, i);
						ds.add(dst_node, val);
					}
				}
				else if (src_slot instanceof ExtSpaceAttrSlot) {
					ExtSpaceAttrSlot ss = (ExtSpaceAttrSlot)src_slot;
					ExtSpaceAttrSlot ds = (ExtSpaceAttrSlot)dst_slot;
					int i = 0;
					foreach (INode val; ss.iterate(src_node)) {
						val = (INode)npfactory.projectNode(val);
						ignoreDstCallbackEvent(ds, val, i);
						ds.add(dst_node, val);
						i += 1;
					}
				}
			}
		}
	}

	public void putback() {
		throw new RuntimeException("Putback not implemented");
	}

	public INode getSrcNode() {
		return dst_node;
	}

	public INode getDstNode() {
		return dst_node;
	}

	public void dispose() {
		if (src_node != null) {
			src_node.asANode().delListener(this);
			src_node = null;
		}
		if (dst_node != null) {
			dst_node.asANode().delListener(this);
			dst_node = null;
		}
	}
	
	protected void ignoreSrcCallbackEvent(AttrSlot slot, Object value, int idx) {
		this.ignore_notification = new IgnoredNodeChangeInfo(src_node, slot, value, idx);
	}

	protected void ignoreDstCallbackEvent(AttrSlot slot, Object value, int idx) {
		this.ignore_notification = new IgnoredNodeChangeInfo(dst_node, slot, value, idx);
	}

	// listener interface
	public void callbackNodeChanged(NodeChangeInfo info) {
		if (info.ct != ChangeType.ATTR_MODIFIED)
			return;
		if (this.ignore_notification != null) {
			IgnoredNodeChangeInfo ignore_notification = this.ignore_notification;
			this.ignore_notification = null;
			if (ignore_notification.match(info))
				return;
		}
		INode node = info.parent;
		AttrSlot slot = info.slot;
		if (node == src_node) {
			foreach (AttrSlot dst_slot; dst_node.getNodeTypeInfo().getAllAttributes(); dst_slot.isAttr() && dst_slot.name == slot.name) {
				if (slot instanceof ScalarAttrSlot) {
					ScalarAttrSlot ss = (ScalarAttrSlot)slot;
					ScalarAttrSlot ds = (ScalarAttrSlot)dst_slot;
					Object val = info.new_value;
					if (val instanceof INode)
						val = npfactory.projectNode((INode)val);
					ignoreDstCallbackEvent(ds, val, -1);
					ds.set(dst_node, val);
					npfactory.notifyProjectionChanged();
				}
				else if (slot instanceof SpaceAttrSlot) {
					SpaceAttrSlot ss = (SpaceAttrSlot)slot;
					SpaceAttrSlot ds = (SpaceAttrSlot)dst_slot;
					Object val = info.new_value;
					if (val instanceof INode)
						val = npfactory.projectNode((INode)val);
					ignoreDstCallbackEvent(ds, val, info.idx);
					if (info.old_value == null)
						ds.insert(dst_node, info.idx, (INode)val);
					else if (info.new_value == null)
						ds.del(dst_node, info.idx);
					else
						ds.set(dst_node, info.idx, (INode)val);
					npfactory.notifyProjectionChanged();
				}
				//else if (slot instanceof ExtSpaceAttrSlot) {
				//	ExtSpaceAttrSlot ss = (ExtSpaceAttrSlot)slot;
				//	ExtSpaceAttrSlot ds = (ExtSpaceAttrSlot)dst_slot;
				//	Object val = info.new_value;
				//	if (val instanceof INode)
				//		val = npfactory.projectNode((INode)val);
				//	ignoreDstCallbackEvent(ds, val, info.idx);
				//	if (info.old_value == null)
				//		ds.insert(dst_node, val, info.idx);
				//	else if (info.new_value == null)
				//		ds.del(dst_node, info.idx);
				//	else
				//		ds.set(dst_node, val, info.idx);
				//	npfactory.notifyProjectionChanged();
				//}
			}
		}
		else if (node == dst_node) {
			foreach (AttrSlot src_slot; src_node.getNodeTypeInfo().getAllAttributes(); src_slot.isAttr() && src_slot.name == slot.name) {
				if (slot instanceof ScalarAttrSlot) {
					ScalarAttrSlot ss = (ScalarAttrSlot)src_slot;
					ScalarAttrSlot ds = (ScalarAttrSlot)slot;
					Object val = info.new_value;
					if (val instanceof INode)
						val = npfactory.putbackNode((INode)val);
					ignoreSrcCallbackEvent(ss, val, -1);
					ss.set(src_node, val);
				}
				else if (slot instanceof SpaceAttrSlot) {
					SpaceAttrSlot ss = (SpaceAttrSlot)src_slot;
					SpaceAttrSlot ds = (SpaceAttrSlot)slot;
					Object val = info.new_value;
					val = npfactory.putbackNode((INode)val);
					ignoreSrcCallbackEvent(ss, val, info.idx);
					if (info.old_value == null)
						ss.insert(src_node, info.idx, (INode)val);
					else if (info.new_value == null)
						ss.del(src_node, info.idx);
					else
						ss.set(src_node, info.idx, (INode)val);
				}
				//else if (slot instanceof ExtSpaceAttrSlot) {
				//	ExtSpaceAttrSlot ss = (ExtSpaceAttrSlot)src_slot;
				//	ExtSpaceAttrSlot ds = (ExtSpaceAttrSlot)slot;
				//	Object val = info.new_value;
				//	if (val instanceof INode)
				//		val = npfactory.projectNode((INode)val);
				//	ignoreSrcCallbackEvent(ss, val, info.idx);
				//	if (info.old_value == null)
				//		ss.insert(src_node, val, info.idx);
				//	else if (info.new_value == null)
				//		ss.del(src_node, info.idx);
				//	else
				//		ss.set(src_node, val, info.idx);
				//}
			}
		}
	}
}

