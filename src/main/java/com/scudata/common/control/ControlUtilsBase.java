package com.scudata.common.control;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.HashMap;

import com.scudata.app.common.StringUtils2;
import com.scudata.common.StringUtils;

/**
 * The basic tool class of the control
 *
 */
public class ControlUtilsBase {
	/**
	 * ��ס�ַ��Ŀ�ȣ����ٻ�ͼ�ٶ�
	 */
	private static HashMap<String, Integer> fmWidthBuf = new HashMap<String, Integer>();
	/**
	 * ���б�
	 */
	private static ArrayList<String> emptyArrayList = new ArrayList<String>();
	/**
	 * �����ı����е�ӳ���KEY���ַ�����VALUE�����к���ı��б�
	 */
	public static HashMap<String, ArrayList<String>> wrapStringBuffer = new HashMap<String, ArrayList<String>>();

	/**
	 * ����
	 * 
	 * @param text
	 *            Ҫ���е��ı�
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            ���
	 * @return
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			int w) {
		return wrapString(text, fm, w, -1);
	}

	/**
	 * ��ȡ�ַ�������ʾ���
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            Ҫ��ʾ���ַ���
	 * @return ���
	 */
	public static int stringWidth(FontMetrics fm, String text) {
		String key = fm.hashCode() + "," + text.hashCode();
		Integer val = fmWidthBuf.get(key);
		int width;
		if (val == null) {
			width = fm.stringWidth(text);
			val = new Integer(width);
			fmWidthBuf.put(key, val);
		} else {
			width = val.intValue();
		}
		return width;
	}

	/**
	 * ����
	 * 
	 * @param text
	 *            Ҫ���е��ı�
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            ���
	 * @param maxRow
	 *            �������(�����������ľͲ�Ҫ��)
	 * @return
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			int w, int maxRow) {
		if (!StringUtils.isValidString(text) || w < 1) {
			return emptyArrayList;
		}
		boolean isExp = text != null && text.startsWith("=");
		String hashKey = text.hashCode() + "" + fm.hashCode() + w + maxRow;
		ArrayList<String> wrapedString = wrapStringBuffer.get(hashKey);
		if (wrapedString == null) {
			if (isExp) {
				wrapedString = StringUtils2.wrapExpString(text, fm, w, false,
						maxRow);
			} else {
				// String \n do not break lines, only line breaks char is
				// allowed
				// text = StringUtils.replace(text, "\\n", "\n");
				text = StringUtils.replace(text, "\t", "        ");

				if (text.indexOf('\n') < 0 && stringWidth(fm, text) < w) {
					wrapedString = new ArrayList<String>();
					wrapedString.add(text);
					if (maxRow > 0 && wrapedString.size() > maxRow) {
						wrapStringBuffer.put(hashKey, wrapedString);
						return wrapedString;
					}
				} else {
					// ��jdk6��New Times Roman����ʹ��LineBreakMeasurer����jvm�˳��쳣��
					// �㲻���������ʲôʱ��ʹ�õ�LineBreakMeasurer����ʱ�滻�ɱ�������з�����wunan
					// 2018-05-29
					wrapedString = StringUtils2.wrapString(text, fm, w, false,
							maxRow);
				}
			}
			wrapStringBuffer.put(hashKey, wrapedString);
		}
		return wrapedString;
	}
}
