package com.scudata.expression.mfn.record;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.expression.IParam;
import com.scudata.expression.RecordFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * ȡ��¼ָ���ֶε�ֵ������ָ���ֶε�ֵ
 * r.field(F) r.field(F, v)
 * @author RunQian
 *
 */
public class FieldValue extends RecordFunction {
	private String prevName; // ��һ�μ�����ֶ���
	private DataStruct prevDs; // ��һ����¼�����ݽṹ
	private int prevCol; // ��һ����¼�ֶε����

	/**
	 * �����ʽ����Ч�ԣ���Ч���׳��쳣
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field" + mm.getMessage("function.missingParam"));
		}
	}
	
	// '+=' ��ֵ����
	public Object addAssign(Object value, Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				int findex = ((Number)obj).intValue();
				if (findex > 0) {
					// �ֶδ�0��ʼ����
					findex--;
				} else if (findex == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("0" + mm.getMessage("ds.fieldNotExist"));
				} // С��0�Ӻ���
				
				Object result = Variant.add(srcRecord.getFieldValue(findex), value);
				srcRecord.set(findex, result);
				return result;
			} else if (obj instanceof String) {
				if (obj != prevName || srcRecord.dataStruct() != prevDs) {
					prevName = (String)obj;
					prevDs = srcRecord.dataStruct();
					prevCol = prevDs.getFieldIndex(prevName);
					
					if (prevCol < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(prevName + mm.getMessage("ds.fieldNotExist"));
					}
				}
				
				Object result = Variant.add(srcRecord.getNormalFieldValue(prevCol), value);
				srcRecord.setNormalFieldValue(prevCol, result);
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				int findex = ((Number)obj).intValue();
				if (findex > 0) {
					// �ֶδ�0��ʼ����
					findex--;
				} else if (findex == 0) {
					return null;
				} // С��0�Ӻ���
				
				return srcRecord.getFieldValue2(findex);
			} else if (obj instanceof String) {
				if (obj == prevName && srcRecord.dataStruct() == prevDs) {
					if (prevCol >= 0) {
						return srcRecord.getNormalFieldValue(prevCol);
					} else {
						return null;
					}
				}
				
				prevName = (String)obj;
				prevDs = srcRecord.dataStruct();
				prevCol = prevDs.getFieldIndex(prevName);
				if (prevCol >= 0) {
					return srcRecord.getNormalFieldValue(prevCol);
				} else {
					return null;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			Object value = sub1.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				int findex = ((Number)obj).intValue();
				if (findex > 0) {
					// �ֶδ�0��ʼ����
					findex--;
				} else if (findex == 0) {
					return null;
				} // С��0�Ӻ���
				
				srcRecord.set2(findex, value);
			} else if (obj instanceof String) {				
				int findex = srcRecord.getFieldIndex((String)obj);
				if (findex >= 0) {
					srcRecord.set2(findex, value);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.paramTypeError"));
			}

			return null;
		}
	}
}
