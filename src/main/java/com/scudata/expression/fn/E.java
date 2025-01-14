package com.scudata.expression.fn;

import java.util.Date;

import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.excel.ExcelUtils;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.mfn.sequence.Export;
import com.scudata.resources.EngineMessage;

/**
 * E(x) x�Ƕ�������ʱ��ת���ɶ������ÿ��һ����¼����һ���Ǳ��⡣ x�Ǵ������Ϊ�س��ָ���/TAB�ָ��еĴ����Ȳ���ת����
 * x�����/����ʱ��ת���ɶ������С� x����ֵ��Excel���������/ʱ�䡣 x������/ʱ����ת����ֵ�� δ���������򷵻�x����
 * 
 * @b �ޱ���
 * @p ����������ת�õģ����ת�ض�������ʱҲת��
 * @s x�����ʱ���سɻس�/TAB�ָ��Ĵ�
 * @1 ת�ɵ������У�x�ǵ�ֵʱ����[x]��x�Ƕ������з���x.conj()����@pʱ���к���
 * @2 x�ǵ�ֵʱ����[[x]]����@pʱ��ת�÷���
 * 
 */
public class E extends Function {

	/**
	 * �Խڵ����Ż�
	 * 
	 * @param ctx
	 *            ����������
	 * @param Node
	 *            �Ż���Ľڵ�
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			// �Բ������Ż�
			param.optimize(ctx);
		}

		return this;
	}

	/**
	 * ����
	 */
	public Object calculate(Context ctx) {
		final String FUNC_NAME = "E";
		String opt = option;
		if (param == null) {
			return null;
		}
		IParam xParam;
		if (param.getType() != IParam.Normal) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.invalidParam"));
		} else {
			xParam = param;
		}
		if (xParam == null) {
			return null;
		}
		if (!xParam.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(FUNC_NAME
					+ mm.getMessage("function.invalidParam"));
		}
		Object x = xParam.getLeafExpression().calculate(ctx);
		if (x == null) {
			return null;
		}
		boolean isB = opt != null && opt.indexOf("b") != -1;
		boolean isP = opt != null && opt.indexOf("p") != -1;
		boolean isS = opt != null && opt.indexOf("s") != -1;
		boolean is1 = opt != null && opt.indexOf("1") != -1;
		boolean is2 = opt != null && opt.indexOf("2") != -1;

		if (is1) { // ת�ɵ�������
			Sequence seq;
			if (x instanceof Sequence) {
				seq = (Sequence) x;
				if (isTableOrPmt(seq)) {
					// x�����/����ʱ��ת���ɶ������С�
					if (isP) {
						seq = pmtToSequenceP(seq, !isB);
					} else {
						seq = pmtToSequence(seq, !isB);
					}
					seq = seq.conj(null);
				} else if (isSequence2(seq)) { // x�Ƕ������з���x.conj()
					if (isP) { // ��@pʱ���к���
						seq = ExcelUtils.transpose(seq);
					}
					seq = seq.conj(null);
				} else {
					// �������в�������
				}
			} else { // x�ǵ�ֵʱ����[x]
				seq = new Sequence();
				seq.add(x);
			}
			return seq;
		}

		if (is2) { // ת�ɶ�������
			Sequence seq;
			if (x instanceof Sequence) {
				seq = (Sequence) x;
				if (isTableOrPmt(seq)) {
					// x�����/����ʱ��ת���ɶ������С�
					if (isP) {
						seq = pmtToSequenceP(seq, !isB);
					} else {
						seq = pmtToSequence(seq, !isB);
					}
				} else if (isSequence2(seq)) { // ��������
					if (isP) { // ��@pʱ��ת�÷���
						seq = ExcelUtils.transpose(seq);
					}
				} else { // Excel������Ӧ���е������У�Ҳ����һ�°�
					seqToSeq2(seq);
				}
			} else { // x�ǵ�ֵʱ����[[x]]
				seq = new Sequence();
				Sequence memSeq = new Sequence();
				memSeq.add(x);
				seq.add(memSeq);
			}
			return seq;
		}

		if (x instanceof Number) { // excel����ʱ�����ֵת��java����ʱ��
			Date date = ExcelUtils.excelDateNumber2JavaDate((Number) x);
			return date;
		} else if (x instanceof Date) { // java����ʱ��ת��excel����ʱ�����ֵ
			Number excelDateNumber = ExcelUtils
					.javaDate2ExcelDateNumber((Date) x);
			return excelDateNumber;
		} else if (x instanceof Sequence) {
			Sequence seq = (Sequence) x;
			if (isS && seq instanceof Table) {
				// @s x�����ʱ���سɻس�/TAB�ָ��Ĵ�
				return exportS((Table) seq, !isB);
			}
			if (isTableOrPmt(seq)) {
				// x�����/����ʱ��ת���ɶ������С�
				if (isP) {
					seq = pmtToSequenceP(seq, !isB);
				} else {
					seq = pmtToSequence(seq, !isB);
				}
				return seq;
			} else if (isSequence2(seq)) {
				// x�Ƕ�������ʱ��ת���ɶ������ÿ��һ����¼����һ���Ǳ��⡣
				if (isP) {
					seq = sequenceToTableP(seq, !isB);
				} else {
					seq = sequenceToTable(seq, !isB);
				}
				return seq;
			} else {
				return x;
			}
		} else if (x instanceof String) {
			// x�Ǵ������Ϊ�س��ָ���/TAB�ָ��еĴ����Ȳ���ת����
			return transposeString(x, isB, isP);
		}
		return x;
	}

	/**
	 * x�Ǵ������Ϊ�س��ָ���/TAB�ָ��еĴ����Ȳ���ת����
	 * 
	 * @return
	 */
	public static Object transposeString(Object x, boolean isB, boolean isP) {
		if (!StringUtils.isValidString(x)) {
			return x;
		}
		Sequence seq = importS((String) x, !isB);
		if (seq == null) {
			return x;
		}
		if (isTableOrPmt(seq)) { // ��������ת�ɶ�������
			if (isP) {
				seq = pmtToSequenceP(seq, !isB);
			} else {
				seq = pmtToSequence(seq, !isB);
			}
		} else {
			if (isP) { // ����ת�÷���
				seq = ExcelUtils.transpose(seq);
			}
		}
		return seq;
	}

	private static boolean isTableOrPmt(Object x) {
		if (x instanceof Table)
			return true;
		if (x instanceof Sequence) {
			Sequence seq = (Sequence) x;
			return seq.isPmt();
		}
		return false;
	}

	/**
	 * �س��ָ���/TAB�ָ��еĴ���ת�����
	 * 
	 * @param str
	 *            �س��ָ���/TAB�ָ��еĴ�
	 * @param hasTitle
	 *            �Ƿ��б�����
	 * @return
	 */
	private static Sequence importS(String str, boolean hasTitle) {
		StringBuffer buf = new StringBuffer();
		buf.append(Escape.addEscAndQuote(str));
		buf.append(".import");
		if (hasTitle)
			buf.append("@t");
		buf.append("(;" + COL_SEP + ")");
		Expression exp = new Expression(buf.toString());
		Sequence seq = (Sequence) exp.calculate(new Context());
		return seq;
	}

	/**
	 * ���ת�ɻس�/TAB�ָ��Ĵ�
	 * 
	 * @param t
	 *            ���
	 * @return �س�/TAB�ָ��Ĵ�
	 */
	private String exportS(Table t, boolean hasTitle) {
		String opt = hasTitle ? "t" : null;
		String exportStr = Export.export(t, null, null, COL_SEP, opt,
				new Context());
		return exportStr;
	}

	/**
	 * �����������У�ת��Ϊ�������С�����ǰ��Ҫ��֤pmt����������
	 * 
	 * @param pmt
	 *            ����������
	 * @param hasTitle
	 *            �Ƿ��б�����
	 * @return ��������
	 */
	private static Sequence pmtToSequence(Sequence pmt, boolean hasTitle) {
		if (pmt == null)
			return pmt;
		int len = pmt.length();
		Sequence seq;
		if (hasTitle) { // ��������Ϊ��һ�м�¼
			seq = new Sequence(len + 1);
			DataStruct ds = pmt.dataStruct();
			if (ds != null) {
				seq.add(new Sequence(ds.getFieldNames()));
			}
		} else {
			seq = new Sequence(len);
		}
		BaseRecord r;
		for (int i = 1; i <= len; i++) {
			r = (BaseRecord) pmt.get(i);
			seq.add(new Sequence(r.getFieldValues()));
		}
		return seq;
	}

	/**
	 * �����������У�ת��Ϊ�������У�����ת�÷��ء�����ǰ��Ҫ��֤pmt����������
	 * 
	 * @param pmt
	 *            ����������
	 * @param hasTitle
	 *            �Ƿ��б�����
	 * @return ��������
	 */
	private static Sequence pmtToSequenceP(Sequence pmt, boolean hasTitle) {
		if (pmt == null)
			return pmt;
		BaseRecord r;
		Object firstMem = pmt.ifn();
		String[] fieldNames = null;
		int colCount = 0;
		if (hasTitle) {
			DataStruct ds = pmt.dataStruct();
			if (ds == null) // �б����е����ݽṹ����Ϊnull
				return pmt;
			fieldNames = ds.getFieldNames();
			colCount = fieldNames.length;
		} else {
			if (firstMem == null) {
				// ��Աȫ�ǿգ�����һ�����������
				Sequence seq = new Sequence();
				seq.add(new Sequence());
				return seq;
			}
			r = (BaseRecord) firstMem;
			colCount = r.getFieldCount();
		}
		int len = pmt.length();
		Sequence seq = new Sequence(len);
		Sequence colSeq;
		for (int c = 0; c < colCount; c++) {
			colSeq = new Sequence(len);
			if (fieldNames != null) { // �б���ʱ�ȼӱ���
				colSeq.add(fieldNames[c]);
			}
			for (int i = 1; i <= len; i++) {
				r = (BaseRecord) pmt.get(i);
				if (r == null) {
					colSeq.add(null);
				} else {
					if (r.getFieldCount() > c)
						colSeq.add(r.getFieldValue(c));
					else
						colSeq.add(null);
				}
			}
			seq.add(colSeq);
		}
		return seq;
	}

	/**
	 * ����������ת��Ϊ���
	 * 
	 * @param seq
	 *            ��������
	 * @param hasTitle
	 *            �Ƿ��б�����
	 * @return ���
	 */
	private Table sequenceToTable(Sequence seq, boolean hasTitle) {
		Table t = null;
		Sequence rowSeq;
		int cc = 0;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			rowSeq = (Sequence) seq.get(i);
			if (rowSeq == null || rowSeq.length() == 0) {
				if (t == null)
					continue;
				else
					t.newLast();
			}
			if (t == null) {
				cc = rowSeq.length();
				String[] colNames;
				if (hasTitle) {
					colNames = getColNames(rowSeq);
				} else {
					colNames = getDefaultColNames(cc);
				}
				t = new Table(colNames);
				if (hasTitle) {
					continue;
				}
			}
			Object[] rowData = new Object[cc];
			for (int c = 1, count = Math.min(cc, rowSeq.length()); c <= count; c++) {
				rowData[c - 1] = rowSeq.get(c);
			}
			t.newLast(rowData);
		}
		return t;
	}

	/**
	 * ������ת�õĶ�������ת��Ϊ���
	 * 
	 * @param seq
	 *            ��������
	 * @param hasTitle
	 *            �Ƿ��б�����
	 * @return ���
	 */
	private Table sequenceToTableP(Sequence seq, boolean hasTitle) {
		Table t = null;
		int len = seq.length();
		int colCount = ExcelUtils.getSequenceColCount(seq);
		Sequence colSeq;
		Sequence rowSeq;
		boolean isEmptyCol;
		for (int c = 0; c < colCount; c++) {
			colSeq = new Sequence(len);
			isEmptyCol = true;
			for (int i = 1; i <= len; i++) {
				rowSeq = (Sequence) seq.get(i);
				if (rowSeq == null) {
					colSeq.add(null);
				} else {
					if (rowSeq.length() > c) {
						colSeq.add(rowSeq.get(c + 1));
						isEmptyCol = false;
					} else {
						colSeq.add(null);
					}
				}
			}
			if (isEmptyCol) {
				if (t == null)
					continue;
				else
					t.newLast();
			}
			if (t == null) {
				String[] colNames;
				if (hasTitle) {
					colNames = getColNames(colSeq);
				} else {
					colNames = getDefaultColNames(len);
				}
				t = new Table(colNames);
				if (hasTitle) {
					continue;
				}
			}
			t.newLast(colSeq.toArray());
		}
		return t;
	}

	/**
	 * �������������ֶ���
	 * 
	 * @param seq
	 * @return
	 */
	private String[] getColNames(Sequence seq) {
		int len = seq.length();
		String[] colNames = new String[len];
		Object val;
		String colName;
		for (int i = 1; i <= len; i++) {
			val = seq.get(i);
			if (val != null) {
				colName = String.valueOf(val);
			} else {
				colName = null;
			}
			if (!StringUtils.isValidString(colName)) {
				colName = "_" + i;
			}
			colNames[i - 1] = colName;
		}
		return colNames;
	}

	/**
	 * ����ȱʡ�ֶ���
	 * 
	 * @param cc
	 * @return
	 */
	private String[] getDefaultColNames(int cc) {
		String[] colNames = new String[cc];
		for (int c = 1; c <= cc; c++) {
			colNames[c - 1] = "_" + c;
		}
		return colNames;
	}

	/**
	 * �Ƿ��������
	 * 
	 * @param seq
	 *            ����
	 * @return �Ƿ��������
	 */
	private boolean isSequence2(Sequence seq) {
		if (seq == null)
			return false;
		Object obj;
		boolean memIsSequence = false;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			obj = seq.get(i);
			if (obj != null) {
				if (obj instanceof Sequence) {
					memIsSequence = true;
				} else {
					return false; // �ǿճ�Ա�������з���false
				}
			}
		}
		return memIsSequence;
	}

	/**
	 * ��������תΪ��������
	 */
	private void seqToSeq2(Sequence seq) {
		Object obj;
		Sequence memSeq;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			obj = seq.get(i);
			if (obj != null && obj instanceof Sequence) {
				continue;
			} else {
				memSeq = new Sequence();
				memSeq.add(obj);
				seq.set(i, memSeq);
			}
		}
	}

	private static final String COL_SEP = "\t";
}
