package com.scudata.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.ByteMap;
import com.scudata.common.DES;
import com.scudata.common.IOUtils;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.Context;
import com.scudata.dm.KeyWord;
import com.scudata.dm.LineImporter;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

/**
 * ��������д������
 * @author WangXiaoJun
 *
 */
public class CellSetUtil {
	private static final byte Type_PgmCellSet = 1;
	private static final String KEY = "rqqrrqqr"; // ������Կ
	private static final byte ENCRYPTED = 0x01;

	/**
	 * ���ֽ�����ʹ��CellSetUtil.KEY����
	 * @param bytes ����������
	 * @return ���ܺ�����
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] bytes) throws Exception {
		DES des = new DES(KEY);
		return des.encrypt(bytes);
	}
	
	/**
	 * ���ֽ�����ʹ��CellSetUtil.KEY����
	 * @param bytes ����������
	 * @return ���ܺ�����
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] bytes) throws Exception {
		DES des = new DES(KEY);
		return des.decrypt(bytes);
	}
	
	/**
	 * д�Զ�����ܡ����ܺ����ĳ�����
	 * @param fileName Ҫд����������ļ���
	 * @param cs ����������
	 * @throws Exception
	 */
	public static void writePgmCellSet(String fileName, PgmCellSet cs, String fnEncrypt, String fnDecrypt) throws Exception {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(fileName));
			writePgmCellSet(bos, cs, fnEncrypt, fnDecrypt);
		} finally {
			if(bos != null) bos.close();
		}
	}
	
	/**
	 * д�Զ�����ܡ����ܺ����ĳ�����
	 * @param out �����
	 * @param cs ����������
	 * @throws Exception
	 */
	public static void writePgmCellSet(OutputStream out, PgmCellSet cs, String fnEncrypt, String fnDecrypt) throws Exception {
		int dotIndex = fnEncrypt.lastIndexOf('.');
		if (dotIndex == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(fnEncrypt + mm.getMessage("invoke.methodNotExist"));
		}

		String className = fnEncrypt.substring(0, dotIndex);
		String methodName = fnEncrypt.substring(dotIndex + 1);
		Class<? extends Object> classObj = Class.forName(className);
		Method method = classObj.getDeclaredMethod(methodName, byte[].class);
		
		out.write('R');
		out.write('Q');
		out.write('Q');
		out.write('R');
		out.write(Type_PgmCellSet); // ��������
		
		// �汾4����������ĳ���value����ʵֵ��editValue������ֵ
		// �汾5�������˽��ܺ���
		out.write(5); 

		// ���볤��+����
		ByteArrayOutputRecord bo = new ByteArrayOutputRecord();
		String psw = cs.getPasswordHash();
		bo.writeString(psw);
		int privilege = cs.getNullPasswordPrivilege();
		bo.writeInt(privilege);
		bo.writeString(fnDecrypt);
		byte []pswBytes = bo.toByteArray();
		pswBytes = encrypt(pswBytes);
		IOUtils.writeInt(out, pswBytes.length);
		out.write(pswBytes);

		// ���Գ���+����
		ByteMap map = cs.getCustomPropMap();
		if (map == null || map.size() == 0) {
			IOUtils.writeInt(out, 0);
		} else {
			byte []mapBytes = map.serialize();
			IOUtils.writeInt(out, mapBytes.length);
			out.write(mapBytes);
		}

		byte[] csBytes = cs.serialize();
		csBytes = (byte[])method.invoke(null, csBytes);

		out.write(ENCRYPTED); // �������ݼ���
		IOUtils.writeInt(out, csBytes.length);
		out.write(csBytes);

		out.write('R');
		out.write('Q');
		out.write('Q');
		out.write('R');
		out.flush();
	}

	/**
	 * д������
	 * �������� + �汾 + ���볤�� + ���� + ���Գ��� + ���� + �Ƿ���� + ������ + ������
	 * @param out �����
	 * @param cs ����������
	 * @throws Exception
	 */
	public static void writePgmCellSet(OutputStream out, PgmCellSet cs) throws Exception {
		out.write('R');
		out.write('Q');
		out.write('Q');
		out.write('R');
		out.write(Type_PgmCellSet); // ��������
		out.write(4); // �汾4����������ĳ���value����ʵֵ��editValue������ֵ

		// ���볤��+����
		ByteArrayOutputRecord bo = new ByteArrayOutputRecord();
		String psw = cs.getPasswordHash();
		bo.writeString(psw);
		int privilege = cs.getNullPasswordPrivilege();
		bo.writeInt(privilege);
		byte []pswBytes = bo.toByteArray();
		pswBytes = encrypt(pswBytes);
		IOUtils.writeInt(out, pswBytes.length);
		out.write(pswBytes);

		// ���Գ���+����
		ByteMap map = cs.getCustomPropMap();
		if (map == null || map.size() == 0) {
			IOUtils.writeInt(out, 0);
		} else {
			byte []mapBytes = map.serialize();
			IOUtils.writeInt(out, mapBytes.length);
			out.write(mapBytes);
		}

		byte[] csBytes = cs.serialize();
		if (psw == null || psw.length() == 0) {
			out.write(0); // ��������û�м���
		} else {
			out.write(ENCRYPTED); // �������ݼ���
			csBytes = encrypt(csBytes);
		}

		IOUtils.writeInt(out, csBytes.length);
		out.write(csBytes);

		out.write('R');
		out.write('Q');
		out.write('Q');
		out.write('R');
		out.flush();
	}

	/**
	 * ���������ļ��Ƿ������
	 * @param fileName String �ļ�·����
	 * @return boolean
	 */
	public static boolean isEncrypted(String fileName) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileName);
			return isEncrypted(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * ���������ļ��Ƿ������
	 * @param is InputStream ������
	 * @return boolean
	 */
	public static boolean isEncrypted(InputStream is) {
		try {
			int c1 = is.read();
			int c2 = is.read();
			int c3 = is.read();
			int c4 = is.read();

			if (c1 != 'R' || c2 != 'Q' || c3 != 'Q' || c4 != 'R') {
				return false;
			}

			int type = is.read(); // ��������
			if (type != Type_PgmCellSet) {
				return false;
			}

			int ver = is.read(); // �汾
			if (ver < 3) {
				return false;
			} else {
				int pswLen = IOUtils.readInt(is);
				byte []pswBytes = new byte[pswLen];
				IOUtils.readFully(is, pswBytes);
				pswBytes = decrypt(pswBytes);

				ByteArrayInputRecord bi = new ByteArrayInputRecord(pswBytes);
				String psw = bi.readString();
				return psw != null && psw.length() > 0;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * ���������ļ��Ƿ�ʹ�����û��Զ������
	 * @param is InputStream ������
	 * @return boolean
	 */
	public static boolean isUserEncrypted(InputStream is) {
		try {
			int c1 = is.read();
			int c2 = is.read();
			int c3 = is.read();
			int c4 = is.read();

			if (c1 != 'R' || c2 != 'Q' || c3 != 'Q' || c4 != 'R') {
				return false;
			}

			int type = is.read(); // ��������
			if (type != Type_PgmCellSet) {
				return false;
			}

			int ver = is.read(); // �汾
			if (ver < 5) {
				return false;
			} else {
				int pswLen = IOUtils.readInt(is);
				byte []pswBytes = new byte[pswLen];
				IOUtils.readFully(is, pswBytes);
				pswBytes = decrypt(pswBytes);

				ByteArrayInputRecord bi = new ByteArrayInputRecord(pswBytes);
				bi.readString();
				bi.readInt();
				String fnDecrypt = bi.readString();
				return fnDecrypt != null && fnDecrypt.length() > 0;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * ��ȡ������Զ�������ӳ��
	 * @param fileName String
	 * @return ByteMap
	 */
	public static ByteMap readCsCustomPropMap(String fileName) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileName);
			return readCsCustomPropMap(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * ��ȡ������Զ�������ӳ��
	 * @param is InputStream
	 * @return ByteMap
	 */
	public static ByteMap readCsCustomPropMap(InputStream is) {
		try {
			int c1 = is.read();
			int c2 = is.read();
			int c3 = is.read();
			int c4 = is.read();

			if (c1 != 'R' || c2 != 'Q' || c3 != 'Q' || c4 != 'R') {
				return null;
			}

			int type = is.read(); // ��������
			int ver = is.read(); // �汾
			if (ver > 1 || type != Type_PgmCellSet) {
				int pswLen = IOUtils.readInt(is);
				if (pswLen > 0) is.skip(pswLen);
			}

			int mapLen = IOUtils.readInt(is);
			if (mapLen > 0) {
				byte []mapBytes = new byte[mapLen];
				IOUtils.readFully(is, mapBytes);
				ByteMap map = new ByteMap();
				map.fillRecord(mapBytes);
				return map;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * д������
	 * @param fileName Ҫд����������ļ���
	 * @param cs ����������
	 * @throws Exception
	 */
	public static void writePgmCellSet(String fileName, PgmCellSet cs) throws Exception {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(fileName));
			writePgmCellSet(bos, cs);
		} finally {
			if(bos != null) bos.close();
		}
	}

	/**
	 * ��������
	 * @param fileName �������ļ���
	 * @throws Exception
	 * @return PgmCellSet
	 */
	public static PgmCellSet readPgmCellSet(String fileName) throws Exception {
		return readPgmCellSet(fileName, null);
	}

	/**
	 * �������ܵĳ�����
	 * @param fileName �������ļ���
	 * @throws Exception
	 * @param psw String ����
	 * @return PgmCellSet
	 */
	public static PgmCellSet readPgmCellSet(String fileName, String psw) throws Exception {
		BufferedInputStream bis = null;
		PgmCellSet pcs;
		
		try {
			bis = new BufferedInputStream(new FileInputStream(fileName));
			pcs = readPgmCellSet(bis, psw);
		} finally {
			if(bis != null) bis.close();
		}
		
		File file = new File(fileName);
		pcs.setName(file.getPath());
		return pcs;
	}

	/**
	 * ��������
	 * @param is InputStream ������
	 * @throws Exception
	 * @return PgmCellSet
	 */
	public static PgmCellSet readPgmCellSet(InputStream is) throws Exception {
		return readPgmCellSet(is, null);
	}
	
	/**
	 * ��������
	 * @param is ������
	 * @param psw ���룬û��������Ϊ��
	 * @return PgmCellSet
	 * @throws Exception
	 */
	public static PgmCellSet readPgmCellSet(InputStream is, String psw) throws Exception {
		if (is.read() != 'R' || is.read() != 'Q' || is.read() != 'Q' || is.read() != 'R') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}

		int type = is.read(); // ��������
		if (type != Type_PgmCellSet) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}

		PgmCellSet cs = new PgmCellSet();
		int ver = is.read(); // �汾
		
		if (ver == 1) {
			int mapLen = IOUtils.readInt(is);
			if (mapLen > 0) is.skip(mapLen);

			int csLen = IOUtils.readInt(is);
			byte []csBytes = new byte[csLen];
			IOUtils.readFully(is, csBytes);

			is.read(); // R
			is.read(); // Q
			is.read(); // Q
			is.read(); // R

			// �Ȱ�ǩ�������������л�����Ҫ����Ƿ�ǩ����
			cs.fillRecord(csBytes);
			changeOldVersionParam(cs);
			return cs;
		}

		int pswLen = IOUtils.readInt(is);
		byte []pswBytes = new byte[pswLen];
		IOUtils.readFully(is, pswBytes);
		String fnDecrypt = null;
		
		if (ver > 2) {
			pswBytes = decrypt(pswBytes);
			ByteArrayInputRecord bi = new ByteArrayInputRecord(pswBytes);
			bi.readString(); //String pswHash = 
			bi.readInt(); //int nullPswPrivilege = 
			//PgmCellSet.getPrivilege(pswHash, psw, nullPswPrivilege);
			//if (privilege == PgmCellSet.PRIVILEGE_NULL) {
			//	MessageManager mm = EngineMessage.get();
			//	throw new RQException(mm.getMessage("cellset.pswError"));
			//}
			
			if (ver > 4) {
				fnDecrypt = bi.readString();
			}
		}

		int mapLen = IOUtils.readInt(is);
		if (mapLen > 0) {
			is.skip(mapLen);
		}

		// �Ƿ��м���Ȩ��
		int isEncrypted = is.read() & ENCRYPTED;
		int csLen = IOUtils.readInt(is);
		byte []csBytes = new byte[csLen];
		IOUtils.readFully(is, csBytes);

		is.read(); // R
		is.read(); // Q
		is.read(); // Q
		is.read(); // R
		
		if (fnDecrypt != null) {
			int dotIndex = fnDecrypt.lastIndexOf('.');
			if (dotIndex == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fnDecrypt + mm.getMessage("invoke.methodNotExist"));
			}
			
			String className = fnDecrypt.substring(0, dotIndex);
			String methodName = fnDecrypt.substring(dotIndex + 1);
			Class<? extends Object> classObj = Class.forName(className);
			Method method = classObj.getDeclaredMethod(methodName, byte[].class);
			csBytes = (byte[])method.invoke(null, csBytes);
		} else if (isEncrypted == ENCRYPTED) { // ���ܳ�����
			csBytes = decrypt(csBytes);
		}

		cs.fillRecord(csBytes);
		cs.setCurrentPassword(psw);
		
		if (ver < 4) {
			// �汾С��4��������Ĳ���ֵ����Ǳ༭ֵ
			changeOldVersionParam(cs);
		}
		
		return cs;
	}

	private static void changeOldVersionParam(PgmCellSet pcs) {
		// �汾С��4��������Ĳ���ֵ����Ǳ༭ֵ
		ParamList paramList = pcs.getParamList();
		if (paramList == null) {
			return;
		}
		
		for (int i = 0, size = paramList.count(); i < size; ++i) {
			Param param = paramList.get(i);
			Object value = param.getValue();
			if (value instanceof String) {
				String old = (String)value;
				value = Variant.parse(old);
				param.setValue(value);
				
				if (value instanceof String) {
					int match = Sentence.scanQuotation(old, 0);
					if (match == old.length() -1) {
						param.setEditValue('\'' + (String)value);
					} else if (old.charAt(0) == '\'') {
						param.setEditValue('\'' + old);
					} else {
						param.setEditValue(old);
					}
				} else {
					param.setEditValue(old);
				}
			}
		}
	}
	
	/**
	 * @param cellSet CellSet
	 * @param args String[]
	 * esProc Ϊdos�������뷽ʽ��ֵͨ��Ϊ������,��Ҫ�ȼ�����put��context��
	 */
	public static void putArgStringValue(CellSet cellSet,String[] args) {
		if( args==null ) {
			putArgValue(cellSet, null);
		} else {
			Object[] vals = new Object[args.length];
			for (int i = 0; i < args.length; i++) {
				vals[i] = PgmNormalCell.parseConstValue(args[i]);
			}
			
			putArgValue(cellSet, vals);
		}
	}

	/**
	 * ��args��ֵ��cellSet�в���������������
	 * @param cellSet CellSet��Ҫ���õ��������
	 * @param args Object[]���û�����Ĵ����͵Ĳ���ֵ������ΪesProc�Լ�dataHub�м�������ǰ׼����xq
	 * DataHubΪJDBC���÷�ʽ��ֵ������õ�Object���顣
	 */
	public static void putArgValue(CellSet cellSet,Object[] args) {
		ParamList params = cellSet.getParamList();
		if (params == null || params.count() == 0) {
			return;
		}
		
		Context context = cellSet.getContext();
		int c = 0;
		for (int i = 0; i < params.count(); i++) {
			Param p = params.get(i);
			if (p.getKind() != Param.VAR){//Param.ARG) {
				continue;
			}
			
			String paraName = p.getName();
			if (args != null && c < args.length) {//paras.isUserChangeable() &&
				context.setParamValue(paraName, args[c], Param.VAR);// Param.ARG);
				c++;
			} else{
				context.setParamValue(paraName, p.getValue(), Param.VAR);//, Param.ARG);
			}
		}
	}
	
	/**
	 * ִ�е����ʽ������������
	 * @param src ���ʽ
	 * @param args ����ֵ���ɵ����У���argi����
	 * @param ctx
	 * @return
	 */
	public static Object execute1(String src, Sequence args, Context ctx) {
		// ����еĲ������̶���"arg"��ͷ
		if (args != null && args.length() > 0) {
			for (int i = 1; i <= args.length(); ++i) {
				ctx.setParamValue("arg" + i, args.get(i));
			}
		}
		
		Expression exp = new Expression(ctx, src);
		return exp.calculate(ctx);
	}

	/**
	 * ִ�б��ʽ��������tab�ָ������ûس��ָ�
	 * @param src
	 * @param args ����ֵ���ɵ����У���argi����
	 * @param ctx
	 * @return
	 */
	public static Object execute(String src, Sequence args, Context ctx) {
		PgmCellSet pcs = toPgmCellSet(src);
		
		// ����еĲ������̶���"arg"��ͷ
		if (args != null && args.length() > 0) {
			for (int i = 1; i <= args.length(); ++i) {
				ctx.setParamValue("arg" + i, args.get(i));
			}
		}
		
		pcs.setContext(ctx);
		pcs.calculateResult();
		
		if (pcs.hasNextResult()) {
			return pcs.nextResult();
		} else {
			int colCount = pcs.getColCount();
			for (int r = pcs.getRowCount(); r > 0; --r) {
				for (int c = colCount; c > 0; --c) {
					PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
					if (cell.isCalculableCell() || cell.isCalculableBlock()) {
						return cell.getValue();
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * ��Ԫ����ʽ��ά�������ɳ�����
	 * @param expStrs ��Ԫ����ʽ��ɵ����ж�ά����
	 * @return ������
	 */
	public static PgmCellSet toPgmCellSet(String[][]expStrs) {
		if (expStrs == null || expStrs.length == 0) {
			return null;
		}
		
		int rowCount = expStrs.length;
		int colCount = 0;
		
		for (int r = 0; r < rowCount; ++r) {
			if (expStrs[r] != null && expStrs[r].length > colCount) {
				colCount = expStrs[r].length;
			}
		}
		
		if (colCount == 0) {
			return null;
		}
		
		PgmCellSet pcs = new PgmCellSet(rowCount, colCount);
		for (int r = 1; r <= rowCount; ++r) {
			String []row = expStrs[r - 1];
			int count = row != null ? row.length : 0;
			for (int c = 1; c <= count; ++c) {
				PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
				cell.setExpString(row[c - 1]);
			}
		}
		
		return pcs;
	}
	
	/**
	 * �ָ��ַ�������������ĸ�����
	 * @param src �ַ���������tab�ָ������ûس��ָ�
	 * @return
	 */
	public static PgmCellSet toPgmCellSet(String src) {
		if (src == null || src.length() == 0) return null;
		
		char []buffer = src.toCharArray();
		int len = buffer.length;
		int index = 0;
		
		// ��ͷ��n���ǲ���
		// #var1=xxx
		// #var2=xxx
		ParamList paramList = new ParamList();
		while (index < len && buffer[index] == '#') {
			String strParam = null;
			for(int i = ++index; i < len; ++i) {
				if (buffer[i] == '\n') {
					if (buffer[i - 1] == '\r') {
						strParam = new String(buffer, index, i - index - 1);
					} else {
						strParam = new String(buffer, index, i - index);
					}
					
					index = i + 1;
					break;
				}
			}
			
			if (strParam == null) {
				strParam = new String(buffer, index, len - index);
				index = len;
			}
			
			int s = strParam.indexOf('=');
			String paramName;
			Object paramValue = null;
			if (s != -1) {
				paramName = strParam.substring(0, s);
				paramValue = Variant.parse(strParam.substring(s + 1), false);
			} else {
				paramName = strParam;
			}
			
			paramList.add(paramName, Param.VAR, paramValue);
		}
		
		final char colSeparator = '\t';
		ArrayList<String> line = new ArrayList<String>();
		int rowCount = 10;
		int colCount = 1;
		PgmCellSet pcs = new PgmCellSet(rowCount, colCount);
		int curRow = 1;

		if (paramList.count() > 0) {
			pcs.setParamList(paramList);
		}
		
		while (index != -1) {
			index = LineImporter.readLine(buffer, index, colSeparator, line);
			int curColCount = line.size();
			if (curColCount > colCount) {
				pcs.addCol(curColCount - colCount);
				colCount = curColCount;
			}

			if (curRow > rowCount) {
				rowCount += 10;
				pcs.addRow(10);
			}
			
			for (int f = 0; f < curColCount; ++f) {
				String exp = line.get(f);
				if (exp != null && exp.length() > 0) {
					PgmNormalCell cell = pcs.getPgmNormalCell(curRow, f + 1);
					cell.setExpString(exp);
				}
			}
			
			curRow++;
			line.clear();
		}
		
		pcs.removeRow(curRow, rowCount - curRow + 1);
		changeAliasNameToCell(pcs);
		return pcs;
	}
	
	// �ı������п�����@x:...����Ԫ����һ�����������ʽ����ͨ������������ø���
	// ���������ѱ���������ת�ɸ��ӵ�����
	private static void changeAliasNameToCell(PgmCellSet pcs) {
		int rowCount = pcs.getRowCount();
		int colCount = pcs.getColCount();
		
		for (int r = 1; r <= rowCount; ++r) {
			for (int c = 1; c <= colCount; ++c) {
				PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
				String expStr = cell.getExpString();
				if (expStr != null && expStr.length() > 1 && expStr.charAt(0) == '@') {
					int end = expStr.indexOf(':');
					if (end != -1) {
						String aliasName = expStr.substring(1, end).trim();
						if (aliasName.length() > 0) {
							if (end + 1 < expStr.length()) {
								expStr = expStr.substring(end + 1);
								cell.setExpString(expStr);
							} else {
								cell.setExpString(null);
							}
							
							changeAliasNameToCell(pcs, aliasName, cell.getCellId());
						}
					}
				}
			}
		}
	}
	
	private static void changeAliasNameToCell(PgmCellSet pcs, String aliasName, String cellId) {
		int rowCount = pcs.getRowCount();
		int colCount = pcs.getColCount();
		int aliasNameLen = aliasName.length();
		
		for (int r = 1; r <= rowCount; ++r) {
			for (int c = 1; c <= colCount; ++c) {
				PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
				String expStr = cell.getExpString();
				if (expStr != null && expStr.length() > aliasNameLen) {
					expStr = changeAliasNameToCell(expStr, aliasName, cellId);
					cell.setExpString(expStr);
				}
			}
		}
	}
	
	private static String changeAliasNameToCell(String expStr, String aliasName, String cellId) {
		int aliasLen = aliasName.length();
		if (expStr == null || expStr.length() < aliasLen) {
			return expStr;
		}
		
		StringBuffer sb = null;
		int len = expStr.length();
		for (int i = 0; i < len;) {
			char c = expStr.charAt(i);
			if (c == '"' || c == '\'') {
				int match = Sentence.scanQuotation(expStr, i);
				if (match == -1) {
					if (sb != null) {
						sb.append(expStr.substring(i));
					}
					
					break;
				} else {
					if (sb != null) {
						sb.append(expStr.substring(i, match + 1));
					}
					
					i = match + 1;
				}
			} else if (KeyWord.isSymbol(c) || c == '#') {
				// #aliasName����ȡfor�ĵ�ǰѭ�����
				if (sb != null) {
					sb.append(c);
				}
				
				i++;
			} else {
				int end = KeyWord.scanId(expStr, i + 1);
				if (end - i == aliasLen && aliasName.equals(expStr.substring(i, end))) {
					if (sb == null) {
						sb = new StringBuffer();
						sb.append(expStr.substring(0, i));
					}
					
					sb.append(cellId);
				} else {
					if (sb != null) {
						sb.append(expStr.substring(i, end));
					}
				}
				
				i = end;
			}
		}
		
		if (sb == null) {
			return expStr;
		} else {
			return sb.toString();
		}
	}
	
	/**
	 * �ѳ�����תΪ�ַ���,��ͷ��n���ǲ���
	 * #var1=***
	 * #var2=***
	 * @param cs ������
	 * @return String
	 */
	public static String toString(PgmCellSet cs) {
		StringBuffer sb = new StringBuffer(1024);
		ParamList paramList = cs.getParamList();
		int paramCount = paramList == null ? 0 : paramList.count();
		boolean isFirstLine = true;
		for (int i = 0; i < paramCount; ++i) {
			Param param = paramList.get(i);
			if (isFirstLine) {
				isFirstLine = false;
			} else {
				sb.append('\n');
			}
			
			sb.append('#');
			sb.append(param.getName());
			sb.append('=');
			sb.append(Variant.toString(param.getValue()));
		}
		
		int rowCount = cs.getRowCount();
		int colCount = cs.getColCount();
		for (int r = 1; r <= rowCount; ++r) {
			if (isFirstLine) {
				isFirstLine = false;
			} else {
				sb.append('\n');
			}
			
			for (int c = 1; c <= colCount; ++c) {
				if (c > 1) {
					sb.append('\t');
				}
				
				String exp = cs.getPgmNormalCell(r, c).getExpString();
				if (exp != null) {
					sb.append(exp);
				}
			}
		}
		
		return sb.toString();
	}
}
