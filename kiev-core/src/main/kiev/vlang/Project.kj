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
import syntax kiev.Syntax;

import java.io.File;

import kiev.dump.DumpFactory;

@ThisIsANode(lang=void)
public final class Project extends SNode {

	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public String				name;
	@nodeAttr public DirUnit			root_dir;
	@nodeAttr public ProjectSyntaxInfo∅	syntax_infos;
	@nodeAttr public ProjectStyleInfo∅	style_infos;
	@AttrBinDumpInfo(ignore=true)
	@AttrXMLDumpInfo(ignore=true)
	@nodeData public ASTNode∅			compilationUnits;
	@AttrBinDumpInfo(ignore=true)
	@AttrXMLDumpInfo(ignore=true)
	@nodeData public LVar				thisPar;
	@AttrBinDumpInfo(ignore=true)
	@AttrXMLDumpInfo(ignore=true)
	@nodeData public ErrorInfo∅			errors;
	

	public Project() {
		this.root_dir = new DirUnit(".");
	}
	
	@setter public final void set$name(String value) {
		this.name = (value == null) ? null : value.intern();
	}

	public void addProjectFile(String path) {
		FileUnit.makeFile(path, this, true);
	}
	
	public FileUnit getLoadedFile(File f) {
		String path = DumpFactory.getRelativePath(f);
		path = path.replace(File.separatorChar, '/');
		DirUnit dir;
		String name;
		int end = path.lastIndexOf('/');
		if (end < 0) {
			dir = this.root_dir;
			name = path;
		} else {
			dir = this.root_dir.getSubDir(path.substring(0,end));
			name = path.substring(end+1);
		}
		if (dir == null)
			return null;
		foreach (FileUnit fu; dir.members; name.equals(fu.fname))
			return fu;
		return null;
	}
	
	public Enumeration<CompilationUnit> enumerateAllCompilationUnits() {
		CompilationUnitEnumerator fe = new CompilationUnitEnumerator(false);
		return fe;
	}
	
	public Enumeration<CompilationUnit> enumerateNewCompilationUnits() {
		CompilationUnitEnumerator fe = new CompilationUnitEnumerator(true);
		return fe;
	}
	
	class CompilationUnitEnumerator implements Enumeration<CompilationUnit> {
		private int idx;
		public boolean hasMoreElements() {
			return idx < compilationUnits.length;
		}
		public CompilationUnit nextElement() {
			return (CompilationUnit)compilationUnits[idx++];
		}
		CompilationUnitEnumerator(boolean only_new) {
			if (only_new)
				idx = compilationUnits.length;
		}
	}
}

@ThisIsANode(lang=void)
public final class ProjectSyntaxInfo extends SNode {
	@AttrXMLDumpInfo(attr=true, name="type")
	@nodeAttr public String file_type;
	@AttrXMLDumpInfo(attr=true, name="ext")
	@nodeAttr public String file_ext;
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public String description;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public String qname;
	
	@nodeAttr public ProjectSyntaxFactory syntax;
	@nodeAttr public ProjectSyntaxFactory printer;
	@nodeAttr public ProjectSyntaxFactory parser;
	
	@setter public void set$qname(String value) {
		this.qname = value;
		if (syntax == null && value != null) {
			ProjectSyntaxFactoryAny f = new ProjectSyntaxFactoryAny();
			f.factory = "kiev·fmt·common·DefaultTextProcessor";
			f.addParam("class", value);
			syntax = f;
		}
	}
}

public interface TextProcessor {
	public void setProperty(String name, String value);
}

@ThisIsANode(lang=void)
public abstract class ProjectSyntaxFactory extends SNode {
	@nodeAttr public ProjectSyntaxParam∅	params;

	public abstract TextProcessor makeTextProcessor();
	
	public void addParam(String name, String value) {
		ProjectSyntaxParam p = new ProjectSyntaxParam();
		p.name = name;
		p.value = value;
		params.append(p);
	}
}

@ThisIsANode(lang=void)
public final class ProjectSyntaxFactoryXmlDump extends ProjectSyntaxFactory {
	public TextProcessor makeTextProcessor() {
		TextProcessor tp = (TextProcessor)Class.forName("kiev.fmt.common.DefaultTextProcessor").newInstance();
		tp.setProperty("class", "<xml-dump>");
		foreach (ProjectSyntaxParam p; params)
			tp.setProperty(p.name, p.value);
		return tp;
	}
}

@ThisIsANode(lang=void)
public final class ProjectSyntaxFactoryBinDump extends ProjectSyntaxFactory {
	public TextProcessor makeTextProcessor() {
		TextProcessor tp = (TextProcessor)Class.forName("kiev.fmt.common.DefaultTextProcessor").newInstance();
		tp.setProperty("class", "<bin-dump>");
		foreach (ProjectSyntaxParam p; params)
			tp.setProperty(p.name, p.value);
		return tp;
	}
}

@ThisIsANode(lang=void)
public final class ProjectSyntaxFactoryAny extends ProjectSyntaxFactory {
	@AttrXMLDumpInfo(attr=true, name="factory")
	@nodeAttr public String					factory;
	
	public TextProcessor makeTextProcessor() {
		TextProcessor tp = (TextProcessor)Class.forName(factory.replace('·','.')).newInstance();
		foreach (ProjectSyntaxParam p; params)
			tp.setProperty(p.name, p.value);
		return tp;
	}
}

@ThisIsANode(lang=void)
public final class ProjectSyntaxParam extends SNode {
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public String name;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public String value;
}

@ThisIsANode(lang=void)
public final class ProjectStyleInfo extends SNode {
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public String description;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public String qname;
}

@ThisIsANode(lang=void)
public interface CompilationUnit extends ASTNode {
	public boolean isInterfaceOnly();
}

@ThisIsANode(lang=void)
public final class DirUnit extends SNode {

	public static final DirUnit[] emptyArray = new DirUnit[0];

	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public String			name;
	@nodeAttr public ASTNode∅		members;
	
	@setter public final void set$name(String value) {
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
	
	public boolean hasProjectFiles() {
		foreach (FileUnit fu; members; fu.is_project_file)
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
		ANode p = this.parent();
		while (p != null && !(p instanceof Project))
			p = p.parent();
		if (p instanceof Project)
			p.compilationUnits.append(fu);
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
	
	public DirUnit getSubDir(String qname) {
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
					return null;
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
				return null;
			dir = dd;
		}
		return dir;
	}
	
}

