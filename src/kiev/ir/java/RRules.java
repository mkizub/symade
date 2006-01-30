package kiev.ir.java;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import kiev.vlang.RuleMethod.RuleMethodImpl;
import kiev.vlang.RuleMethod.RuleMethodView;
import kiev.vlang.RuleBlock.RuleBlockImpl;
import kiev.vlang.RuleBlock.RuleBlockView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RRuleMethod of RuleMethodImpl extends RuleMethodView {
	public boolean preGenerate() {
		Var penv = params[0];
		assert(penv.name.name == namePEnv && penv.getType() ≡ Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
		if( body instanceof RuleBlock ) {
			body.preGenerate();
			Kiev.runProcessorsOn(body);
			body.cleanDFlow();
		}
		return true;
	}
}

@nodeview
public final view RRuleBlock of RuleBlockImpl extends RuleBlockView {
	public boolean preGenerate() {
		node.resolve(Type.tpVoid);
		fields_buf = new StringBuffer();
		node.resolve1(new JumpNodes(false,null,false,null,false));
		StringBuffer sb = new StringBuffer(256);
		sb.append("{ ");
		// Declare private method frame class
		String tmpClassName = "frame$$";
		sb.append("static class ").append(tmpClassName).append(" extends rule{\n");
		sb.append("int bt$;\n");
		RuleMethod rule_method = (RuleMethod)ctx_method;
		// Backtrace holders
		for (int i=0; i < rule_method.max_depth; i++)
			sb.append("int bt$").append(i).append(";\n");
		// Local variables
		foreach(Var v; rule_method.localvars) {
			String tp = Kiev.reparseType(v.type);
			if( v.type.isWrapper() )
				sb.append(tp+' '+v.name.name+" := new "+tp+"();\n");
			else
				sb.append(tp+' '+v.name.name+";\n");
		}
		// tmp variables inserted here
		sb.append(fields_buf.toString());
		fields_buf = null;
		sb.append("}\n");
		// Create new method frame or hash values from
		// existing one
		sb.append(tmpClassName).append(" $env;\n");
		sb.append("int bt$;\n");
		sb.append("if("+namePEnv+"==null) {\n");
		sb.append(" $env=new ").append(tmpClassName).append("(); bt$=0;\n");
		sb.append(" goto enter$1;\n");
		sb.append("}\n");
		if (rule_method.base != 1) {
			sb.append("else{\n");
			sb.append(" $env=($cast ").append(tmpClassName).append(")"+namePEnv+";\n");
			sb.append(" bt$=$env.bt$;\n");
			sb.append("}\n");
			sb.append("switch(bt$) {\ncase 0:\n");
		} else {
			// BUG!!!
			sb.append("else{\n$env=($cast ").append(tmpClassName).append(")"+namePEnv+";}\n");
		}
		sb.append("return null;\n");
		node.createText(sb);
		// Close method
		if (rule_method.base != 1)
			sb.append("}\nreturn null;\n");
		sb.append("}\n");
		trace(Kiev.debugRules,"Rule text generated:\n"+sb);
		Block mbody = Kiev.parseBlock(this.getNode(),sb);
		ctx_method.body = mbody;
		mbody.stats.addAll(stats.delToArray());
		return false;
	}
}

