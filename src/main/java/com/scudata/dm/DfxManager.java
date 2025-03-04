package com.scudata.dm;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.expression.Expression;

/**
 * dfx���������
 */
public class DfxManager {
	private static DfxManager dfxManager = new DfxManager();
	private HashMap<String, SoftReference<PgmCellSet>> dfxRefMap = 
		new HashMap<String, SoftReference<PgmCellSet>>();

	private HashMap<String, SoftReference<List<Expression>>> expListMap = 
			new HashMap<String, SoftReference<List<Expression>>>();
	
	private DfxManager() {}

	/**
	 * ȡdfx���������ʵ��
	 * @return DfxManager
	 */
	public static DfxManager getInstance() {
		return dfxManager;
	}

	/**
	 * �������ĳ�����
	 */
	public void clear() {
		synchronized(dfxRefMap) {
			dfxRefMap.clear();
		}
	}
	
	public void clearDfx(String name) {
		synchronized(dfxRefMap) {
			File file = new File(name);
			name = file.getPath();
			dfxRefMap.remove(name);
		}
	}
	
	/**
	 * ʹ����dfx���������������
	 * @param dfx PgmCellSet
	 */
	public void putDfx(PgmCellSet dfx) {
		Context dfxCtx = dfx.getContext();
		dfxCtx.setParent(null);
		dfxCtx.setJobSpace(null);
		dfx.reset();

		synchronized(dfxRefMap) {
			dfxRefMap.put(dfx.getName(), new SoftReference<PgmCellSet>(dfx));
		}
	}

	/**
	 * �ӻ����������ȡdfx��ʹ�������Ҫ����putDfx�������������
	 * @param name dfx�ļ���
	 * @param ctx ����������
	 * @return PgmCellSet
	 */
	public PgmCellSet removeDfx(String name, Context ctx) {
		File file = new File(name);
		name = file.getPath();
		PgmCellSet dfx = null;
		
		synchronized(dfxRefMap) {
			SoftReference<PgmCellSet> sr = dfxRefMap.remove(name);
			if (sr != null) dfx = (PgmCellSet)sr.get();
		}

		if (dfx == null) {
			return readDfx(name, ctx);
		} else {
			// ���ٹ���ctx�еı���
			Context dfxCtx = dfx.getContext();
			dfxCtx.setEnv(ctx);
			return dfx;
		}
	}

	/**
	 * �ӻ����������ȡdfx��ʹ�������Ҫ����putDfx�������������
	 * @param fo dfx�ļ�����
	 * @param ctx ����������
	 * @return PgmCellSet
	 */
	public PgmCellSet removeDfx(FileObject fo, Context ctx) {
		PgmCellSet dfx = null;
		File file = new File(fo.getFileName());
		String name = file.getPath();
		
		synchronized(dfxRefMap) {
			SoftReference<PgmCellSet> sr = dfxRefMap.remove(name);
			if (sr != null) dfx = (PgmCellSet)sr.get();
		}
		
		if (dfx == null) {
			return readDfx(fo, ctx);
		} else {
			// ���ٹ���ctx�еı���
			Context dfxCtx = dfx.getContext();
			dfxCtx.setEnv(ctx);
			return dfx;
		}
	}
	
	/**
	 * ��ȡdfx������ʹ�û���
	 * @param fo dfx�ļ�����
	 * @param ctx ����������
	 * @return PgmCellSet
	 */
	public static PgmCellSet readDfx(FileObject fo, Context ctx) {
		PgmCellSet dfx = fo.readPgmCellSet();
		dfx.resetParam();
		
		// ���ٹ���ctx�еı���
		Context dfxCtx = dfx.getContext();
		dfxCtx.setEnv(ctx);
		return dfx;
	}
	
	/**
	 * ��ȡdfx������ʹ�û���
	 * @param name dfx�ļ���
	 * @param ctx ����������
	 * @return PgmCellSet
	 */
	public static PgmCellSet readDfx(String name, Context ctx) {
		return readDfx(new FileObject(name, null, "s", ctx), ctx);
	}
	
	/**
	 * ȡ����ı��ʽ�����ʽ���������Ҫ����putExpression�����黹����
	 * @param strExp ���ʽ��
	 * @param ctx ����������
	 * @return Expression
	 */
	public Expression getExpression(String strExp, Context ctx) {
		synchronized(expListMap) {
			SoftReference<List<Expression>> ref = expListMap.get(strExp);
			if (ref != null) {
				List<Expression> expList = ref.get();
				if (expList != null && expList.size() > 0) {
					Expression exp = expList.remove(expList.size() - 1);
					exp.reset();
					return exp;
				}
			}
		}
		
		return new Expression(ctx, strExp);
	}
	
	/**
	 * ���ʽ������ɺ�ѱ��ʽ��������
	 * @param strExp ���ʽ��
	 * @param exp ���ʽ
	 */
	public void putExpression(String strExp, Expression exp) {
		synchronized(expListMap) {
			SoftReference<List<Expression>> ref = expListMap.get(strExp);
			if (ref == null) {
				List<Expression> expList = new ArrayList<Expression>();
				expList.add(exp);
				ref = new SoftReference<List<Expression>>(expList);
				expListMap.put(strExp, ref);
			} else {
				List<Expression> expList = ref.get();
				if (expList == null) {
					expList = new ArrayList<Expression>();
					expList.add(exp);
					ref = new SoftReference<List<Expression>>(expList);
					expListMap.put(strExp, ref);
				} else {
					expList.add(exp);
				}
			}
		}
	}
}
