package com.scudata.expression;

import com.scudata.cellset.ICellSet;
import com.scudata.dm.Context;
import com.scudata.expression.fn.Call;

/**
 * ע��ĽŲ�����
 * @author WangXiaoJun
 *
 */
class DfxFunction {
	private String dfxPathName; // �ű�·����
	private boolean hasOptParam; // �Ƿ���ѡ�����
	
	public DfxFunction(String dfxPathName, String opt) {
		this.dfxPathName = dfxPathName;
		hasOptParam = opt != null && opt.indexOf('o') != -1;
	}
	
	public Function newFunction(ICellSet cs, Context ctx, String opt, String param) {
		Function fun = new Call();
		if (hasOptParam) {
			if (opt == null) {
				opt = "null";
			} else {
				opt = '"' + opt + '"';
			}
			
			if (param == null) {
				param = opt;
			} else {
				param = opt + ',' + param;
			}
		}
		
		if (param == null || param.length() == 0) {
			fun.setParameter(cs, ctx, '"' + dfxPathName + '"');
		} else {
			fun.setParameter(cs, ctx, '"' + dfxPathName + '"' + ',' + param);
		}
		
		return fun;
	}
}
