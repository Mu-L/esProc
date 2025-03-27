package com.scudata.chart;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import com.scudata.cellset.graph.config.Palette;
import com.scudata.chart.element.IMapAxis;
import com.scudata.common.Logger;
import com.scudata.dm.*;
import com.scudata.util.Variant;

/**
 * ��Ҫ���������ݵ�˳�����ԣ������Կ���ѭ����Ӧʱ��ʹ�ø���
 *
 */
public class Para {
//	���ֺ�legendPropertyû��Ҫ���л������ڱ�����ʼֵ����
	private transient String name;
	private transient byte legendProperty=0;//��ǰ���Զ�Ӧ��ͼ��������ĸ����ԣ�����ͼ��ʵ����3������
	
	private Object value = null;
	private String axis = null;

	private transient Engine e = null;
	private transient static ArrayList<Color> defPalette = null;

	/**
	 * ȱʡ���캯��
	 */
	public Para() {
	}

	/**
	 * ʹ�ó�ʼֵ�������
	 * @param value
	 */
	public Para(Object value) {
		this.value = value;
	}
	
	/**
	 * ʹ�ö�Ӧͼ���������͹������
	 * @param legendProperty ��Ӧͼ������ֵ
	 */
	public Para(byte legendProperty) {
		this.legendProperty = legendProperty;
	}

	/**
	 * ʹ�ó�ʼֵ�Լ���Ӧͼ���������͹������
	 * @param value ��ʼ����ֵ
	 * @param legendProperty ��Ӧͼ������
	 */
	public Para(Object value,byte legendProperty) {
		this.value = value;
		this.legendProperty = legendProperty;
	}
	
	public Para(Object value, String axis, String name) {
		this.name = name;
		Object tmp = value;
		if (tmp instanceof Sequence) {
			Sequence seq = (Sequence) tmp;
			tmp = Utils.sequenceToChartColor(seq);
			if (tmp == null) { // �������ChartColor,��Ȼ��ֵΪ������
				tmp = value;
			}
		}

		this.value = tmp;
		this.axis = axis;
	}

	/**
	 * ���ö�Ӧ��ͼ��������ֵ
	 * @param p ͼ������ֵ
	 */
	public void setLegendProperty(byte p){
		this.legendProperty = p;
	}
	
	/**
	 * ��ȡͼ������ֵ
	 * @return ����ֵ
	 */
	public byte getLegendProperty(){
		return legendProperty;
	}
	
	/**
	 * ��ȡ����ֵ
	 * @return ����ֵ
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * ��������ֵ
	 * @param value ����ֵ
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * ���û�������
	 * @param e �������
	 */
	public void setEngine(Engine e) {
		this.e = e;
	}

	/**
	 * ���ֵΪ���У���ȡ���е�ֵ����
	 * @return ����ֵ����
	 */
	public int getLength(){
		return sequenceValue().length();
	}
	
	/**
	 * �������ֵΪ�����ɫ��ChartColor��������������ֵ�а�����ChartColor
	 * ��ȡ�����ɫ���Ƿ����˽�����ɫ
	 * @return ����з���true�����򷵻�false
	 */
	public boolean hasGradientColor() {
		if (value == null || !(value instanceof Sequence))
			return chartColorValue().isGradient();
		Sequence seq = (Sequence) value;
		int len = seq.length();
		for (int i = 1; i <= len; i++) {
			ChartColor cc = chartColorValue(i);
			if (cc.isGradient())
				return true;
		}
		return false;
	}

	/**
	 * ����߼�����ֵΪ����ʱ����Ӧ�߼�ֵѭ����ȡ������ֵ
	 * ���綨���� ������ 3����ɫ�� ���߼������� �������� 4���߼�ֵʱ���� �� ��ѭ����ӦΪ��1����ɫ
	 * @param pos �߼����ݵ�λ�����
	 * @return �������ѭ����Ӧ��Ӧ����ֵ
	 */
	public Object objectValue(int pos) {
		Object val;
		if (value instanceof Sequence) {
			Sequence seq = (Sequence) value;
			pos = pos % seq.length();
			if (pos == 0) {
				pos = seq.length();
			}
			val = seq.get(pos);
		} else if (value instanceof Color) { // Ϊ�˲���Ҫ��ת���������и�ֱֵ����Color���󣬵��ӱ༭���ڻ�ȡ�Ķ�������
			val = new Integer(((Color) value).getRGB());
		} else {
			val = value;
		}
		// ��Ϊnullʱ����ֱ��ȡ���ò�����ֵ��������������û��Axis�Ĳ���ʱ����engine�޹�
		// ����ͨ����������������ͬʱ����ģ�Ҳ��ȡû����Ĳ���ֵʱ��eҲ�϶���null
		if (axis == null || e == null) {
			return val;
		}
		IMapAxis im = e.getMapAxisByName(axis);
		if (im == null) {
			return val;
		}
		if(legendProperty==0){
			throw new RuntimeException("Property "+name+" does not support legend mapping.");
		}
		return im.getMapValue(val, legendProperty);
	}

	/**
	 * ���ϲ��������֪����Ӧ���������ͣ����Ϊ���Σ�ȡ��һ������
	 * @return ��һ������ֵ
	 */
	public int intValue() {
		return intValue(0);
	}

	/**
	 * ����λ�û�ȡ��Ӧ����������ֵ��λ�ñȲ�������Ҫ��ʱ����ѭ��ȡֵ
	 * @param pos ������Ӧλ��
	 * @return ������Ӧ����ֵ
	 */
	public int intValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return 0;
		}
		if (val instanceof Number) {
			return ((Number) val).intValue();
		}
		return Integer.parseInt(val.toString());
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @return ������ֵ
	 */
	public float floatValue() {
		return floatValue(0);
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @param pos 
	 * @return
	 */
	public float floatValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return 0;
		}
		if (val instanceof Number) {
			return ((Number) val).floatValue();
		}
		return Float.parseFloat(val.toString());
	}

	
	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @return ʵ��ֵ
	 */
	public double doubleValue() {
		return doubleValue(0);
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @param pos ����λ��
	 * @return ʵ��ֵ
	 */
	public double doubleValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return 0;
		}
		if (val instanceof Number) {
			return ((Number) val).doubleValue();
		}
		return Double.parseDouble(val.toString());
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @return ����ֵ
	 */
	public Date dateValue() {
		return dateValue(0);
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @param pos  ����λ��
	 * @return ����ֵ
	 */
	public Date dateValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return null;
		}
		if (val instanceof Date) {
			return (Date) val;
		}
		val = Variant.parseDate(val.toString());
		if (val instanceof Date) {
			return (Date) val;
		}
		return null;
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @return ����ֵ
	 */
	public boolean booleanValue() {
		return booleanValue(0);
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @param pos  ����λ��
	 * @return ����ֵ
	 */
	public boolean booleanValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return false;
		}
		if (val instanceof Boolean) {
			return ((Boolean) val).booleanValue();
		}
		return Boolean.valueOf(val.toString()).booleanValue();
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @return �����ɫֵ
	 */
	public ChartColor chartColorValue() {
		return chartColorValue(1);
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @param pos  ����λ��
	 * @return �����ɫֵ
	 */
	public ChartColor chartColorValue(int pos) {
		Object val = objectValue(pos);
		if (val instanceof Sequence) {
			val = Utils.sequenceToChartColor((Sequence) val);
		}
		if (val == null) {
			val = defColorValue(pos);
		}

		ChartColor cc;
		if (val instanceof ChartColor) {
			cc = ((ChartColor) val).deepClone();
		} else if (val instanceof Color) {
			cc = new ChartColor((Color) val);
		} else {
			cc = new ChartColor(Integer.parseInt(val.toString()));
		}
		return cc;
	}

	
	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @return �ַ���ֵ
	 */
	public String stringValue() {
		return stringValue(0);
	}

	/**
	 * �÷�ͬintValue��������Ӧ�����ο���
	 * @param pos  ����λ��
	 * @return �ַ���ֵ
	 */
	public String stringValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return null;
		}
		return Variant.toString(val);
	}

	/**
	 * ʹ��ȱʡ����ɫ��ɫ���ȡ��ɫѭ��ֵ
	 * @param pos �߼����ݶ�Ӧ����
	 * @return ��ɫֵ
	 */
	public static Color defColorValue(int pos) {
		pos--; 
		ArrayList<Color> palette = getHexPalette();

		pos = pos % palette.size();
		return palette.get(pos);
	}

	public static Object cycleValue(ArrayList values, int pos) {
		pos = pos % values.size();
		return values.get(pos);
	}

	/**
	 * ����������ɫ������ʱ�����ص�ɫ������ɫ
	 * @param pos λ��
	 * @return ��ɫ
	 */
	public Color colorValueNullAsDef(int pos) {
		Color c = colorValue(pos);
		if( c==null ) return defColorValue(pos);
		return c;
	}
	
	/**
	 * �в��ֲ�����Ҫ����Ϊû�ж�����ɫʱ��������nullʱ�����������Ե���ɫ ���ԣ�
	 * ����������û�������ɫ��ʹ��ϵͳ��ɫ�壬���Ƿ�ʹ�õ�ɫ�����ɫ
	 * ���ϲ�����������˸ú�����Ȼ�᷵��nullֵ
	 * ���ܷ���null����ɫʹ��colorValueNullAsDef������
	 * @param pos int λ��
	 * @return Color ��ɫ
	 */
	public Color colorValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return null;
		}

		if (val instanceof Color) {
			return (Color) val;
		} else if (val instanceof ChartColor) {
			return ((ChartColor) val).getColor1();
		} else if (val instanceof Sequence) {
			ChartColor cc = ChartColor.getInstance((Sequence) val);
			return cc.getColor1();
		}
		int ci = Integer.parseInt(val.toString());
		if( ci ==16777215) return null;//͸��ɫʱ������null����
		return new Color(ci);
	}

	public Sequence sequenceValue() {
		if (value instanceof Sequence) {
			return (Sequence) value;
		}
		Sequence seq = new Sequence();
		if(value!=null){
			seq.add(value);
		}
		return seq;
	}

	private static String[] hexColors = new String[] { "AFD8F8", "F6BD0F",
			"8BBA00", "FF8E46", "008E8E", "D64646", "8E468E", "588526",
			"B3AA00", "008ED6", "9D080D", "A186BE", "CC6600", "FDC689",
			"ABA000", "F26D7D", "FFF200", "0054A6", "F7941C", "CC3300",
			"006600", "663300", "6DCFF6" };
	private static ArrayList<Color> hexPalette = null;

	public static ArrayList<Color> getHexPalette() {
		if (hexPalette == null) {
			hexPalette = loadConfigFile();
			if(hexPalette!=null) return hexPalette;
			
			hexPalette = new ArrayList<Color>();
			for (int i = 0; i < hexColors.length; i++) {
				String tmp = hexColors[i];
				int r = Integer.parseInt(tmp.substring(0, 2), 16);
				int g = Integer.parseInt(tmp.substring(2, 4), 16);
				int b = Integer.parseInt(tmp.substring(4, 6), 16);
				hexPalette.add(new Color(r, g, b));
			}
		}
		return hexPalette;
	}

	public static ArrayList<Color> getDefPalette() {
		if (defPalette == null) {
			defPalette = loadConfigFile();
			if(defPalette!=null) return defPalette;

			defPalette = new ArrayList<Color>();
			defPalette.add(new Color(128, 128, 0, 255));
			defPalette.add(new Color(255, 128, 0, 255));
			defPalette.add(new Color(192, 255, 0, 255));
			defPalette.add(new Color(0, 0, 128, 255));
			defPalette.add(new Color(128, 0, 128, 255));
			defPalette.add(new Color(255, 0, 128, 255));
			defPalette.add(new Color(0, 128, 128, 255));
			defPalette.add(new Color(128, 128, 128, 255));
			defPalette.add(new Color(0, 255, 255, 255));
			defPalette.add(new Color(192, 192, 192, 255));
			defPalette.add(new Color(255, 128, 128, 255));
			defPalette.add(new Color(0, 255, 128, 255));
			defPalette.add(new Color(192, 255, 128, 255));
			defPalette.add(new Color(255, 255, 0, 255));
			defPalette.add(new Color(255, 255, 128, 255));
			defPalette.add(new Color(128, 0, 255, 255));
			defPalette.add(new Color(255, 0, 255, 255));
			defPalette.add(new Color(0, 128, 255, 255));
			defPalette.add(new Color(128, 128, 255, 255));
			defPalette.add(new Color(255, 128, 255, 255));
			defPalette.add(new Color(192, 255, 255, 255));
			defPalette.add(new Color(255, 0, 0, 255));
			defPalette.add(new Color(0, 255, 0, 255));
			defPalette.add(new Color(0, 0, 255, 255));
			defPalette.add(new Color(0, 128, 0, 255));
			defPalette.add(new Color(255, 255, 255, 255));
		}
		return defPalette;
	}
	
	private static ArrayList<Color> loadConfigFile() {
		try {
			Properties config = new Properties();
			InputStream is = null;
			String name = "/chartcolor.properties";
//			������ͼԪ���õ�����ͳ��ͼ����ͳ��ͼ�������ļ�Ϊcolor.properties,���뼯������ʽ��ͬ
			String relativePath = com.scudata.common.GC.PATH_CONFIG
					+ name;
			File f = new File(
					com.scudata.common.GM.getAbsolutePath(relativePath));
			if (f.exists()) {
				is = new FileInputStream(f);
			} else {
				is = Palette.class.getResourceAsStream(relativePath);
			}

			if (is == null) {
				is = Palette.class
						.getResourceAsStream("/config"+name);
			}
			if (is == null)
				return null;// û����ɫ�ļ�
			config.load(is);
			String obj = (String)config.getProperty("default");
			if(obj==null){
				return null;
			}
			if(obj.startsWith("[")){
				obj = obj.substring(1,obj.length()-1);
			}
			StringTokenizer st = new StringTokenizer(obj,",");
			ArrayList<Color> colors = new ArrayList<Color>();
			while (st.hasMoreElements()){
				String tmp = st.nextToken();
				int value = Integer.parseInt(tmp);
				Color c = new Color(value);
				colors.add(c);
			}
			Logger.debug("Load "+name+" OK.");
			return colors;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args){
		getHexPalette();	
	}
}
