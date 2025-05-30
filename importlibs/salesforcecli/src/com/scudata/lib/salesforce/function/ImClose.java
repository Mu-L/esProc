package com.scudata.lib.salesforce.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// 鍏抽棴杩炴帴.
public class ImClose extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sf_close " + mm.getMessage("function.missingParam"));
		}

		Object o = param.getLeafExpression().calculate(ctx);
		if ((o instanceof ImOpen)) {
			ImOpen cls = (ImOpen)o;
			cls.m_httpPost.releaseConnection();
		}else{
			MessageManager mm = EngineMessage.get();
			throw new RQException("sf_close " + mm.getMessage("HttpPost releaseConnection false"));
		}
		
		return null;
	}
}
