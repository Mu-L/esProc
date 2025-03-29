package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * ѭ�������е������㣬������ͬ�ֶ�ֵ�ĳ�Ա�ۻ�
 * cum(x; Gi,��)	iterate(~~+x;Gi,��)
 * @author runqian
 *
 */
public class Cum extends Function {
	private Expression exp;
	private Expression []gexps;
	
	private Object prevVal;
	private Object []prevGroupVals;
	private Current prevCurrent;

	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}
	
	/**
	 * �����ʽ����Ч�ԣ���Ч���׳��쳣
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cum" + mm.getMessage("function.missingParam"));
		}
	}
	
	private void prepare(IParam param, Context ctx) {
		if (param.isLeaf()) {
			exp = param.getLeafExpression();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cum" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cum" + mm.getMessage("function.invalidParam"));
			}
			
			exp = sub0.getLeafExpression();
			if (sub1.isLeaf()) {
				gexps = new Expression[]{sub1.getLeafExpression()};
			} else {
				int size = sub1.getSubSize();
				gexps = new Expression[size];
				for (int i = 0; i < size; ++i) {
					IParam sub = sub1.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cum" + mm.getMessage("function.invalidParam"));
					}
					
					gexps[i] = sub.getLeafExpression();
				}
			}
		}
	}

	public Object calculate(Context ctx) {
		Current current = ctx.getComputeStack().getTopCurrent();
		if (exp == null) {
			prepare(param, ctx);
			
			if (gexps != null) {
				int gcount = gexps.length;
				prevGroupVals = new Object[gcount];
				for (int i = 0; i < gcount; ++i) {
					prevGroupVals[i] = gexps[i].calculate(ctx);
				}
			}
			
			prevVal = exp.calculate(ctx);
		} else {
			// forѭ�����������cum��ջ�����п��ܱ���
			if (current != prevCurrent) {
				prevVal = null;
				if (prevGroupVals != null) {
					int gcount = prevGroupVals.length;
					for (int i = 0; i < gcount; ++i) {
						prevGroupVals[i] = null;
					}
				}
			}
			
			if (gexps == null) {
				Object val = exp.calculate(ctx);
				prevVal = Variant.add(prevVal, val);
			} else {
				boolean isSame = true;
				int gcount = gexps.length;
				for (int i = 0; i < gcount; ++i) {
					Object val = gexps[i].calculate(ctx);
					if (!Variant.isEquals(prevGroupVals[i], val)) {
						isSame = false;
						prevGroupVals[i] = val;
						
						for (++i; i < gcount; ++i) {
							prevGroupVals[i] = gexps[i].calculate(ctx);
						}
						
						break;
					}
				}
				
				if (isSame) {
					Object val = exp.calculate(ctx);
					prevVal = Variant.add(prevVal, val);
				} else {
					prevVal = exp.calculate(ctx);
				}
			}
		}
		
		prevCurrent = current;
		return prevVal;
	}
}
