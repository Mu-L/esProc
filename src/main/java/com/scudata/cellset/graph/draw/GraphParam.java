package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.text.*;

import com.scudata.cellset.graph.config.*;
import com.scudata.chart.Consts;

/**
 * ͼ�λ���ʱ���������Լ��㣬������չ���ͼ�β�������
 * @author Joancy
 *
 */
public class GraphParam {
	public GraphFontView GFV_TITLE;
	public GraphFontView GFV_VALUE;
	public GraphFontView GFV_LEGEND;
	public GraphFontView GFV_XTITLE;
	public GraphFontView GFV_XLABEL;
	public GraphFontView GFV_YTITLE;
	public GraphFontView GFV_YLABEL;

	public Color graphBackColor = new Color(255, 255, 255); /* ͼ�󱳾���ɫ */
	public Color coorColor = new Color(0, 0, 0); /* ��������ɫ */
	public Color gridColor = new Color(0, 0, 0); /* ������ɫ */
	public byte imageFormat = 0;
	public Vector catNames; /* ������� */
	public Vector serNames; /* �������� */
	public Vector serNames2; /* ����2���� */
	public int catNum; // �����catNames.size(),�����д����õ���ֵ,�ɴ�����ط�����
	public int serNum;
	public int serNum2;
	public double maxValue = 0.0; /* ���ֵ */
	public double minValue = 0.0; /* ��Сֵ */
	public double interval = 0.0; /* ͳ��ֵ��� */
	public double maxValue2 = 0; /* ���ֵ */
	public double minValue2 = 0; /* ��Сֵ */
	public double interval2 = 0.0; /* ͳ��ֵ��� */
	public int minTicknum = 1; /* ��Сֵ������ */
	public int minTicknum2 = 1; /* ��Сֵ������ */
	public int graphXInterval = 0;
	public byte timeScale = 0;
	public String timeFormat = null;
	public double scaleMark = 1; /* ֵ���������ע */
	public boolean drawLineDot = true; /* �Ƿ��עֱ��ͼ�ľ��η��� */
	public boolean isOverlapOrigin = false; /* ԭ���غ� */
	public boolean drawLineTrend = false; /* �Ƿ�ֱ��ͼ��ǰ������ */
	private byte lineThick = 1; /* ����ͼ�Ĵ�ϸ�� */

	public boolean cutPie = true; /* �Ƿ��и��ͼ��һ����� */
	public boolean isMeterColorEnd = true; /* �Ǳ��̶̿�λ����ɫĩ�� */

	public int graphMargin = -1; /* ͳ��ͼ��������ͼ���ı߿���Ҫ���ڱ�עֵ��ǩ��ʱ������λ��,-1��ʾû�����ñ߾� */
	public double barDistance = 0.0; /* ����ͼ������ͼ��� */
	public int gridLineLocation = Consts.GRID_VALUE; /* ������λ�� */
	public int gridLineStyle; /* �����߷�� */
	public int dispValueType = 0; // �Ƿ�������ͼ������ʾֵ��ʶ
	public int dispValueType2 = 0; // �Ƿ�������ͼ������ʾֵ��ʶ
	public boolean graphTransparent = false; /* ����ͼ���Ƿ�͸�� */

	public String dataMarkFormat = ""; // ͼ������ֵ��ʶ�ĸ�ʽ
	public String dataMarkFormat2 = ""; // ͼ������ֵ��ʶ�ĸ�ʽ2
	public boolean dispValueOntop = false; // �Ƿ�������ͼ������ʾֵ��ʶ
	public boolean dispValueOntop2 = false; // �Ƿ�������ͼ������ʾֵ��ʶ
	public boolean dispStackSumValue = false; // �Ƿ��ڶ�ջͼ������ʾͳ��ֵ
	public boolean dispIntersectValue = true; /* ��ʾ�ص�����ֵ */
	public boolean gradientColor = true; /* ��ɫ���� */
	public double maxPositive = 0.0; /* ���ֵ */
	public double minNegative = 0.0; /* ��Сֵ */

	public int leftMargin = 10; // ��߾� ����,������������Ϊ����
	public int rightMargin = 10; /* �ұ߾� */
	public int topMargin = 10; /* �ϱ߾� */
	public int bottomMargin = 10; /* �±߾� */
	public int tickLen = 4; /* �̶ȳ��� */
	public int coorWidth = 100; /* 3D����ռ���п�ȵİٷֱ� */
	public double categorySpan = 190; /* ����ļ��ռ���п�ȵİٷֱ� */
	public int seriesSpan = 100; /* ���м�ļ��ռ������ȵİٷֱ� */
	public int pieRotation = 50; /* ����ռ����ĳ��Ȱٷֱ� */
	public int pieHeight = 70; /* ����ͼ�ĸ߶�ռ�뾶�İٷֱ�<=100 */
	public boolean isDrawTable = false,isDataCenter=false; /* �������ݱ� */
	public int meterRainbowEdge = 16; /* �Ǳ�����ɫ��ռ�뾶��ֵ����Χ0~100 */
	public int meter3DEdge = 8; /* 3D�Ǳ��̱߿�ռ�뾶��ֵ����Χ0~100 */
	public int pieLine = 8; /* ��ͼ������ռ�뾶��ֵ����Χ0~100 */

	// **********************�˱�ע���ϵ����Ծ���Ҫ��ExtGraphProperty��ʼ������,����Ϊ��ͼ�������õ����м����
	public Rectangle2D.Double graphRect, gRect1, gRect2; /* �������� */
	public double coorScale = 1.0; /* ����MarkΪAutoʱ,����ʵ�����ű����ҷŵ�������� */
	public double coorScale2 = 1.0; /* ֵ������� */
	public Vector coorValue = new Vector(); /* ֵ����,�����ʱ������ͼ����Ϊx��ʱ��̶�ֵ */
	public Vector coorValue2 = new Vector(); /* ֵ����,�����ʱ������ͼ����Ϊx��ʱ��̶�ֵ */
	public int tickNum = 10; /* ֵ������ */
	public int tickNum2 = 10; /* ֵ������ */
	public double valueBaseLine = 0; /* ֵ����� */
	public double baseValue = 0; /* �������Сֵ��Ϊ��ͼ�Ļ��ߣ����ֵ���ڴ洢��Сֵ */
	public double baseValue2 = 0; /* �������Сֵ��Ϊ��ͼ�Ļ��ߣ����ֵ���ڴ洢��Сֵ */
	public int graphWidth = 640; /* ͼ���� */
	public int graphHeight = 480; /* ͼ��߶� */
	public int legendBoxWidth = 0; /* ͼ����� */
	public int legendBoxHeight = 0; /* ͼ���߶� */

	public boolean isMultiSeries = false; /* �Ƿ��ж��ϵ��ֵ */
	public Date stateBegin = java.sql.Timestamp.valueOf("2999-01-01 00:00:00");
	public Date stateEnd = java.sql.Timestamp.valueOf("1900-01-01 00:00:00");

	public int topInset = 0; // ͼ���ϱ߾�
	public int bottomInset = 0; /* ͼ���±߾� */
	public int leftInset = 0; /* ͼ����߾� */
	public int rightInset = 0; /* ͼ���ұ߾� */
	public int statusBarHeight = 10; /* ״̬ͼ�����߶� */

	//�˱�ע����Ϊû�б༭������,������������Ϊ����
	public Color lineColor = Color.lightGray; /* �ֹ�����ɫ */

	/**
	 * ����ͳ��ͼ����ɫ
	 * 
	 * @param rgb
	 *            ͳ��ͼ����ɫ
	 */

	public void setBackColor(int rgb) {
		// ͸��
		if (rgb == 16777215 && imageFormat != IGraphProperty.IMAGE_JPG) {
			graphBackColor = null;
		} else {
			graphBackColor = new Color(rgb);
		}
	}

	/**
	 * ��������ͼ�Ĵֶ�
	 * @param thick �ֶ�
	 */
	public void setLineThick(byte thick) {
		this.lineThick = thick;
	}

	/**
	 * ȥֱ�ߴֶ�
	 * @return �ֶ�ֵ
	 */
	public float getLineThick() {
		return getLineThick(lineThick);
	}

	/**
	 * ���ߴ�ת��Ϊbyte����
	 * @return byte���͵Ĵֶ�ֵ
	 */
	public byte getLineThickByte() {
		return lineThick;
	}

	/**
	 * �������ж����thickDefineת��Ϊʵ�ʻ�ͼʱ���ߴֶ�
	 * 
	 * @param thickDefine
	 *            byte
	 * @return float
	 */
	public static float getLineThick(byte thickDefine) {
		float thick = 0.1f;
		switch (thickDefine) {
		case 0:
			return 0;
		case 1:
			thick = 0.1f;
			break;
		default:
			thick = thickDefine - 1.0f;
		}
		return thick;
	}

	/**
	 * ����ͳ��ͼ�Ŀ��
	 * @param w ���
	 * @param h �߶�
	 */
	public void setGraphWH(int w, int h) {
		if (w > 0) {
			graphWidth = w;
		}
		if (h > 0) {
			graphHeight = h;
		}
	}

	/**
	 * ������ת��Ϊ���Ĵ�д����
	 * @param dd ����
	 * @return ���Ķ���
	 */
	public static String xToChinese(double dd) {
		try {
			String s = "��Ҽ��������½��ƾ�";
			String s1 = "ʰ��Ǫ��ʰ��Ǫ��ʰ��Ǫ��";
			String m;
			int j;
			StringBuffer k = new StringBuffer();
			m = String.valueOf(Math.round(dd));
			for (j = m.length(); j >= 1; j--) {
				char n = s.charAt(Integer.parseInt(m.substring(m.length() - j,
						m.length() - j + 1)));
				if (n == '��' && k.charAt(k.length() - 1) == '��') {
					continue;
				}
				k.append(n);
				if (n == '��') {
					continue;
				}
				int u = j - 2;
				if (u >= 0) {
					k.append(s1.charAt(u));
				}
				if (u > 3 && u < 7) {
					k.append('��');
				}
				if (u > 7) {
					k.append('��');
				}
			}
			if (k.length() > 0 && k.charAt(k.length() - 1) == '��') {
				k.deleteCharAt(k.length() - 1);
			}
			if (k.length() > 0 && k.charAt(0) == 'Ҽ') {
				k.deleteCharAt(0);
			}
			return k.toString();
		} catch (Exception x) {
			DecimalFormatSymbols dfs = new DecimalFormatSymbols(
					Locale.getDefault());
			DecimalFormat df = new DecimalFormat("#.#E0", dfs);
			return df.format(dd);
		}
	}

}
