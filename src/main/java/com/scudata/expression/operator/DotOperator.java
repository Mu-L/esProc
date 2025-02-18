package com.scudata.expression.operator;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.Move;
import com.scudata.expression.Node;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * ���������.
 * A.f()
 * @author WangXiaoJun
 *
 */
public class DotOperator extends Operator {
	public DotOperator() {
		priority = PRI_SUF;
	}
	
	/**
	 * �����ʽ����Ч�ԣ���Ч���׳��쳣
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}
	
	public Node optimize(Context ctx) {
		// ����Ҳຯ�������޸��������ֵ���������������г���������Ȳ�������
		// ����[1,2,3].contain(n)
		if (!right.ifModifySequence()) {
			left = left.optimize(ctx, true);
			right = right.optimize(ctx);
			return this;
		} else {
			return super.optimize(ctx);
		}
	}

	/**
	 * �����������ֵ
	 * @param ctx ����������
	 * @return ������
	 */
	private Object getLeftObject(Context ctx) {
		Object obj = left.calculate(ctx);
		
		// n.f()��f�����к���ʱ��n���ͳ�to(n)
		if (obj instanceof Number && right.isSequenceFunction()) {
			int n = ((Number)obj).intValue();
			if (n > 0) {
				return new Sequence(1, n);
			} else {
				return new Sequence(0);
			}
		} else {
			return obj;
		}
	}
	
	public Object calculate(Context ctx) {
		Object leftValue = getLeftObject(ctx);
		if (leftValue == null) {
			return null;
		}

		Node right = this.right;
		while (right != null) {
			if (right.isLeftTypeMatch(leftValue)) {
				right.setDotLeftObject(leftValue);
				Object result = right.calculate(ctx);
				right.releaseDotLeftObject();
				return result;
			} else {
				right = right.getNextFunction();
			}
		}
		
		String fnName;
		if (this.right instanceof Function) {
			fnName = ((Function)this.right).getFunctionName();
		} else {
			fnName = "";
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
	}
	
	public Object assign(Object value, Context ctx) {
		Object leftValue = getLeftObject(ctx);
		if (leftValue == null) {
			return null;
		}

		Node right = this.right;
		while (right != null) {
			if (right.isLeftTypeMatch(leftValue)) {
				right.setDotLeftObject(leftValue);
				return right.assign(value, ctx);
			} else {
				right = right.getNextFunction();
			}
		}
		
		String fnName;
		if (this.right instanceof Function) {
			fnName = ((Function)this.right).getFunctionName();
		} else {
			fnName = "";
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
	}

	public Object addAssign(Object value, Context ctx) {
		Object leftValue = getLeftObject(ctx);
		if (leftValue == null) {
			return null;
		}

		Node right = this.right;
		while (right != null) {
			if (right.isLeftTypeMatch(leftValue)) {
				right.setDotLeftObject(leftValue);
				return right.addAssign(value, ctx);
			} else {
				right = right.getNextFunction();
			}
		}
		
		String fnName;
		if (this.right instanceof Function) {
			fnName = ((Function)this.right).getFunctionName();
		} else {
			fnName = "";
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
	}

	public Object move(Move node, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return null;
		}
		
		right.setDotLeftObject(result1);
		return right.move(node, ctx);
	}

	public Object moveAssign(Move node, Object value, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return value;
		}
		
		right.setDotLeftObject(result1);
		return right.moveAssign(node, value, ctx);
	}
	
	public Object moves(Move node, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return null;
		}
		
		right.setDotLeftObject(result1);
		return right.moves(node, ctx);
	}
	
	/**
	 * ����������еĽ��
	 * @param ctx ����������
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		right.setLeft(left);
		return right.calculateAll(ctx);
		//return right.calculateAll(left, ctx);
	}
	
	/**
	 * ����signArray��ȡֵΪsign����
	 * @param ctx
	 * @param signArray �б�ʶ����
	 * @param sign ��ʶ
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		right.setLeft(left);
		return right.calculateAll(ctx, signArray, sign);
	}
	
	/**
	 * �����߼��������&&���Ҳ���ʽ
	 * @param ctx ����������
	 * @param leftResult &&�����ʽ�ļ�����
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		right.setLeft(left);
		return right.calculateAnd(ctx, leftResult);

		/*BoolArray result = leftResult.isTrue();
		int size = result.size();
		Current current = ctx.getComputeStack().getTopCurrent();
		
		for (int i = 1; i <= size; ++i) {
			if (result.isTrue(i)) {
				current.setCurrent(i);
				Object value = calculate(ctx);
				if (Variant.isFalse(value)) {
					result.set(i, false);
				}
			}
		}
		
		return result;*/
	}
	
	/**
	 * �жϸ�����ֵ��Χ�Ƿ����㵱ǰ�������ʽ
	 * @param ctx ����������
	 * @return ȡֵ����Relation. -1��ֵ��Χ��û������������ֵ��0��ֵ��Χ��������������ֵ��1��ֵ��Χ��ֵ����������
	 */
	public int isValueRangeMatch(Context ctx) {
		right.setLeft(left);
		return right.isValueRangeMatch(ctx);
		/*if (right instanceof MemberFunction) {
			IArray array = left.calculateRange(ctx);
			if (array instanceof ConstArray) {
				Object obj = array.get(1);
				return ((MemberFunction)right).isValueRangeMatch(obj, ctx);
			} else {
				return Relation.PARTICALMATCH;
			}
		} else {
			return Relation.PARTICALMATCH;
		}*/
	}
	
	/**
	 * ������ʽ��ȡֵ��Χ
	 * @param ctx ����������
	 * @return
	 */
	public IArray calculateRange(Context ctx) {
		right.setLeft(left);
		return right.calculateRange(ctx);
	}
	
	/**
	 * ���ؽڵ��Ƿ񵥵�������
	 * @return true���ǵ��������ģ�false������
	 */
	public boolean isMonotone() {
		return left.isMonotone() && right.isMonotone();
	}
}
