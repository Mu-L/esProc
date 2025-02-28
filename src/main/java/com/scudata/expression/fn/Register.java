package com.scudata.expression.fn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmCellSet.FuncInfo;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DfxManager;
import com.scudata.dm.FileObject;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * ��dfx�ļ��Ǽ�Ϊ����
 * register(f,dfx) 
 * �Ǽ�dfx�ļ�Ϊ����f��֮��ú��������������ű���ʹ�ã��������ʽд��Ϊ��f(xi,...)������xi,...Ϊdfx�ļ��еĲ�����
 * ����������ö��ŷָ���
 * @author runqian
 *
 */
public class Register extends Function {
	public Node optimize(Context ctx) {
		return this;
	}
	
	/**
	 * �����ʽ����Ч�ԣ���Ч���׳��쳣
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			/*Object name = param.getLeafExpression().calculate(ctx);
			if (!(name instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("register" + mm.getMessage("function.paramTypeError"));
			}
			
			FunctionLib.removeDFXFunction((String)name);
			return name;*/
			
			Object obj = param.getLeafExpression().calculate(ctx);
			FileObject fo;
			
			if (obj instanceof String) {
				fo = new FileObject((String)obj, null, "s", ctx);
			} else if (obj instanceof FileObject) {
				fo = (FileObject)obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("register" + mm.getMessage("function.paramTypeError"));
			}
			
			PgmCellSet pcs = DfxManager.readDfx(fo, ctx);
			pcs.execute();
			HashMap<String, FuncInfo> map = pcs.getFunctionMap();
			Set<Map.Entry<String,FuncInfo>> set = map.entrySet();
			Iterator<Map.Entry<String,FuncInfo>> iterator = set.iterator();
			
			while (iterator.hasNext()) {
				Map.Entry<String,FuncInfo> entry = iterator.next();
				ctx.addDFXFunction(entry.getKey(), entry.getValue());
			}
			
			return Boolean.valueOf(map.size() > 0);
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.invalidParam"));
		}
		
		Object name = sub0.getLeafExpression().calculate(ctx);
		if (!(name instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.paramTypeError"));
		}
		
		Object dfx = sub1.getLeafExpression().calculate(ctx);
		if (!(dfx instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("register" + mm.getMessage("function.paramTypeError"));
		}
		
		ctx.addDFXFunction((String)name, (String)dfx, option);
		return name;
	}
}
