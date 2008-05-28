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
package kiev.vlang;

import java.io.File;

import syntax kiev.Syntax;


@ThisIsANode(lang=void)
public final class Project extends SNode {

	@virtual typedef This  = Project;

	@nodeAttr public DirUnit			root_dir;

	public Project() {
		this.root_dir = new DirUnit(".");
	}
	
	public void addProjectFile(String path) {
		FileUnit.makeFile(path, true);
	}
	
	public Enumeration<FileUnit> enumerateAllFiles() {
		FileEnumerator fe = new FileEnumerator(root_dir);
		if (Thread.currentThread() instanceof WorkerThread) {
			WorkerThread wt = (WorkerThread)Thread.currentThread();
			assert (wt.fileEnumerator == null);
			wt.fileEnumerator = fe;
		}
		return fe;
	}
	
	public Enumeration<FileUnit> enumerateNewFiles() {
		FileEnumerator fe = new FileEnumerator(null);
		if (Thread.currentThread() instanceof WorkerThread) {
			WorkerThread wt = (WorkerThread)Thread.currentThread();
			assert (wt.fileEnumerator == null);
			wt.fileEnumerator = fe;
		}
		return fe;
	}
	
	public static class FileEnumerator implements Enumeration<FileUnit> {
		private Vector<FileUnit> files;
		private int idx;
		public boolean hasMoreElements() {
			return idx < files.length;
		}
		public FileUnit nextElement() {
			return files[idx++];
		}
		FileEnumerator(DirUnit dir) {
			this.files = new Vector<FileUnit>();
			if (dir != null)
				addFiles(dir);
		}
		private void addFiles(DirUnit dir) {
			foreach (FileUnit fu; dir.members)
				this.files.append(fu);
			foreach (DirUnit d; dir.members)
				addFiles(d);
		}
		void addNewFile(FileUnit fu) {
			this.files.append(fu);
		}
	}
}

@ThisIsANode(lang=void)
public final class DirUnit extends SNode {

	@virtual typedef This  = DirUnit;

	public static final DirUnit[] emptyArray = new DirUnit[0];

	@nodeAttr public String			name;
	@nodeAttr public ASTNode[]		members;
	
	@setter public void set$name(String value) {
		this.name = (value == null) ? null : value.intern();
	}

	public DirUnit() {}
	
	public DirUnit(String name) {
		this.name = name;
	}
	
	public String pname() {
		if (parent() instanceof DirUnit) {
			DirUnit p = (DirUnit)parent();
			if (p.name == ".")
				return name;
			return p.pname() + '/' + name;
		}
		return name;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "proj" && attr == ANode.nodeattr$this) {
			return hasProjectFiles();
		}
		return super.includeInDump(dump, attr, val);
	}
	
	private boolean hasProjectFiles() {
		foreach (FileUnit fu; members; fu.project_file)
			return true;
		foreach (DirUnit du; members; du.hasProjectFiles())
			return true;
		return false;
	}

	private DirUnit addDir(String name) {
		DirUnit dir = new DirUnit(name);
		for (int i=0; i < members.length; i++) {
			if !(members[i] instanceof SNode)
				continue;
			SNode m = (SNode)members[i];
			if (!(m instanceof DirUnit) || ((DirUnit)m).name.compareToIgnoreCase(name) > 0) {
				members.insert(i, dir);
				return dir;
			}
		}
		members.append(dir);
		return dir;
	}

	public FileUnit addFile(FileUnit fu) {
		for (int i=0; i < members.length; i++) {
			if !(members[i] instanceof SNode)
				continue;
			SNode m = (SNode)members[i];
			if!(m instanceof FileUnit)
				continue;
			if (((FileUnit)m).fname.compareToIgnoreCase(fu.fname) > 0) {
				members.insert(i, fu);
				break;
			}
		}
		if (!fu.isAttached())
			members.append(fu);
		if (Thread.currentThread() instanceof WorkerThread) {
			WorkerThread wt = (WorkerThread)Thread.currentThread();
			if (wt.fileEnumerator != null)
				wt.fileEnumerator.addNewFile(fu);
		}
		return fu;
	}

	public DirUnit makeDir(String qname) {
		qname = qname.replace(File.separatorChar, '/');
		DirUnit dir = this;
		int start = 0;
		int end = qname.indexOf('/', start);
		while (end > 0) {
			String nm = qname.substring(start, end).intern();
			if (nm != "") {
				DirUnit dd = null;
				foreach (DirUnit d; dir.members; d.name == nm) {
					dd = d;
					break;
				}
				if (dd == null)
					dd = dir.addDir(nm);
				dir = dd;
			}
			start = end+1;
			end = qname.indexOf('/', start);
		}
		String nm = qname.substring(start).intern();
		if (nm != "") {
			DirUnit dd = null;
			foreach (DirUnit d; dir.members; d.name == nm) {
				dd = d;
				break;
			}
			if (dd == null)
				dd = dir.addDir(nm);
			dir = dd;
		}
		return dir;
	}
	
}

