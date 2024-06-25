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
package kiev.transf;

import syntax kiev.Syntax;

import java.util.Properties;

public interface PluginFactory {
	public PluginDescr getPluginDescr(String name);
}

public final class PluginDescr {
	
	public static final String[] stages = new String[]{ "fe", "fv", "me", "mv", "be" };
	
	public String name;
	public String descr;
	public boolean enabled;
	public PluginDescr[] depends;
	public ProcessorDescr[] processors;
	
	public PluginDescr(String name) {
		this.name = name;
		this.depends = new PluginDescr[0];
		this.processors = new ProcessorDescr[0];
	}
	
	public PluginDescr depends(String name) {
		this.depends = (PluginDescr[])Arrays.append(this.depends, new PluginDescr(name));
		return this;
	}
	public PluginDescr proc(ProcessorDescr proc) {
		proc.qname = this.name+":"+proc.stage+":"+proc.name;
		this.processors = (ProcessorDescr[])Arrays.append(this.processors, proc);
		return this;
	}
	
	public String toString() {
		return name;
	}
	
	public static ProcessorDescr[][] loadAllPlugins() {
		Hashtable<String,PluginDescr> plugins = new Hashtable<String,PluginDescr>();
		Hashtable<String,ProcessorDescr> processors = new Hashtable<String,ProcessorDescr>();
		// load all plugins mentioned in system properties
		Properties props = System.getProperties();
		foreach (String p; (java.util.Enumeration<String>)props.propertyNames(); p.startsWith("symade.plugin.")) {
			String name = p.substring(14).intern();
			String value = props.getProperty(p);
			trace(Kiev.debug && Kiev.verbose, "Found plugin declaration "+value);
			try {
				PluginFactory pf = (PluginFactory)Class.forName(value).newInstance();
				PluginDescr pd = pf.getPluginDescr(name);
				if (pd.name == null)
					pd.name = name;
				if (pd.descr == null)
					pd.descr = name;
				if (pd.depends == null)
					pd.depends = new PluginDescr[0];
				if (pd.processors == null)
					pd.processors = new ProcessorDescr[0];
				plugins.put(pd.name, pd);
			} catch (Exception e) {
				if (Kiev.verbose)
					e.printStackTrace();
			}
		}
		// resolve plugin dependancies
		// collect all compilation phases (processors)
		foreach (PluginDescr plugin; plugins.elements()) {
			for (int i=0; i < plugin.depends.length; i++) {
				PluginDescr pd = plugin.depends[i];
				if (pd != null)
					pd = plugins.get(pd.name);
				if (pd == null) {
					plugin.depends = (PluginDescr[])Arrays.remove(plugin.depends,i--);
					trace(Kiev.verbose, "Plugin "+plugin+" depends on unknown plugin "+plugin.depends[i]);
				} else {
					plugin.depends[i] = pd;
					trace(Kiev.debug && Kiev.verbose, "Plugin "+plugin+" depends on plugin "+pd);
				}
			}
			foreach (ProcessorDescr proc; plugin.processors) {
				String name = proc.name;
				String qname = proc.qname;
				if (qname == null) {
					qname = plugin.name+":"+proc.stage+":"+proc.name;
					proc.qname = qname;
				}
				trace(Kiev.debug && Kiev.verbose, "Plugin "+plugin+" has processor "+qname);
				processors.put(qname,proc);
			}
		}
		// resolve plugin's processor's dependancies, setup before/after links
		foreach (ProcessorDescr proc; processors.elements()) {
			ProcessorDescr[] before = proc.before;
			ProcessorDescr[] after = proc.after;
			proc.before = new ProcessorDescr[0];
			proc.after = new ProcessorDescr[0];
			foreach (ProcessorDescr pd; before; pd != null) {
				String pd_qname = pd.qname;
				pd = processors.get(pd.qname);
				if (pd == null) {
					trace(Kiev.verbose, "Processor "+proc+" runs before unknown processor "+pd_qname);
					continue;
				}
				if (pd.stage == null || !pd.stage.equals(proc.stage)) {
					trace(Kiev.verbose, "Processor "+proc+" runs before invalid stage processor "+pd_qname);
					continue;
				}
				trace(Kiev.debug && Kiev.verbose, "Processor "+proc+" runs before "+pd);
				proc.before = (ProcessorDescr[])Arrays.appendUniq(proc.before,pd);
				pd.after = (ProcessorDescr[])Arrays.appendUniq(pd.after,proc);
			}
			foreach (ProcessorDescr pd; after; pd != null) {
				String pd_qname = pd.qname;
				pd = processors.get(pd.qname);
				if (pd == null) {
					trace(Kiev.verbose, "Processor "+proc+" runs after unknown processor "+pd_qname);
					continue;
				}
				if (pd.stage == null || !pd.stage.equals(proc.stage)) {
					trace(Kiev.verbose, "Processor "+proc+" runs after invalid stage processor "+pd_qname);
					continue;
				}
				trace(Kiev.debug && Kiev.verbose, "Processor "+proc+" runs after "+pd);
				proc.after = (ProcessorDescr[])Arrays.appendUniq(proc.after,pd);
				pd.before = (ProcessorDescr[])Arrays.appendUniq(pd.before,proc);
			}
		}
		// reduce the processor's DAG graph to the list of processors
		ProcessorDescr[][] ret = new ProcessorDescr[stages.length][];
		for (int i=0; i < stages.length; i++) {
			String stage = stages[i];
			Vector<ProcessorDescr> procs = new Vector<ProcessorDescr>();
			foreach (ProcessorDescr proc; processors.elements(); stage.equals(proc.stage))
				procs.append(proc);
			Vector<ProcessorDescr> out = sort(stage, procs);
			trace(Kiev.verbose, "Stage "+stage+": "+out);
			ret[i] = out.toArray();
		}
		return ret;
	}
	private static Vector<ProcessorDescr> sort(String stage, Vector<ProcessorDescr> processors) {
		trace(Kiev.debug && Kiev.verbose, "For stage "+stage+" sorting processors "+processors);
		Vector<ProcessorDescr> out = new Vector<ProcessorDescr>();
		while (processors.size() > 0) {
			// get roots of the graph
			ProcessorDescr proc = null;
			foreach (ProcessorDescr pd; processors; pd.after.length == 0) {
				if (proc == null || proc.priority < pd.priority)
					proc = pd;
			}
			if (proc == null)
				throw new RuntimeException("Loop in processors dependancies for stage "+stage);
			processors.removeElement(proc);
			out.append(proc);
			foreach (ProcessorDescr pd; proc.before)
				pd.after = (ProcessorDescr[])Arrays.remove(pd.after,Arrays.indexOf(pd.after,proc));
		}
		return out;
	}
}

public final class ProcessorDescr {
	
	private ProcessorDescr(String qname) {
		this.qname = qname;
	}
	public ProcessorDescr(String name, String stage, int pr, Class clazz) {
		this.name = name;
		this.stage = stage;
		this.priority = pr;
		this.clazz = clazz;
		this.before = new ProcessorDescr[0];
		this.after = new ProcessorDescr[0];
	}
	
	public ProcessorDescr before(String qname) {
		this.before = (ProcessorDescr[])Arrays.append(this.before, new ProcessorDescr(qname));
		return this;
	}
	public ProcessorDescr after(String qname) {
		this.after = (ProcessorDescr[])Arrays.append(this.after, new ProcessorDescr(qname));
		return this;
	}
	
	public String name; // processor's simple name
	public String qname; // qualified name in form "plugin:stage:processor", like "vnode:fe:pass3" or "virt-fld:me:pre-generate"
	public String descr;
	public String stage; // "fe", "fv", "me", "mv", "be"
	// list of processors this one must run before
	public ProcessorDescr[] before;
	// list of processors this one must run after
	public ProcessorDescr[] after;
	// the priority of this processors, to sort processors that runs before/after the same pair of processors;
	// the higher priority - the earlier it will execute; negative priority means as late as possible execution.
	public int priority;
	
	private Class clazz;
	
	public AbstractProcessor makeProcessor(Env env, int proc_id) {
		return (AbstractProcessor)clazz.getConstructor(Env.class,Integer.TYPE).newInstance(env,Integer.valueOf(proc_id));
	}
	
	public String toString() {
		if (qname != null)
			return qname;
		return name;
	}
}

public abstract class APlugin {
	public final String name;
	public final String descr;
	public boolean enabled;
	public TransfProcessor[] fe; // front-end processors
	public VerifyProcessor[] fv; // front-end verification processors
	public BackendProcessor[] me; // mid-end processors
	public BackendProcessor[] be; // back-end processors
}

