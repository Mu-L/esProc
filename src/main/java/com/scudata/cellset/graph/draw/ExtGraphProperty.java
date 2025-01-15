package com.scudata.cellset.graph.draw;

import java.util.*;
import java.awt.*;

import com.scudata.cellset.*;
import com.scudata.cellset.graph.config.*;
import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;

/**
 * ��չͼ�η���
 * 
 * @author Joancy
 *
 */
public class ExtGraphProperty {
	public ArrayList categories;
	public ArrayList category2 = null;
	private BackGraphConfig bgc = null;
	private boolean isSplitByAxis = false;

	public IGraphProperty getIGraphProperty() {
		return prop;
	}

	/**
	 * ȡ���з��������
	 * 
	 * @return
	 */
	public Vector getCategoryNames() {
		return listCategoryNames(categories);
	}

	/**
	 * ������ṹ�ķ���ת��Ϊ�б����
	 * 
	 * @param cats
	 *            ����
	 * @return �б�
	 */
	public static ArrayList getArrayList(ExtGraphCategory[] cats) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < cats.length; i++) {
			list.add(cats[i]);
		}
		return list;
	}

	/**
	 * �г����з��������
	 * 
	 * @param categories
	 *            ����
	 * @return ����
	 */
	public Vector listCategoryNames(ExtGraphCategory[] categories) {
		return listCategoryNames(getArrayList(categories));
	}

	/**
	 * �г����з��������
	 * 
	 * @param categories
	 *            ����
	 * @return ����
	 */
	public Vector listCategoryNames(ArrayList categories) {
		Vector v = new Vector();
		if (categories == null) {
			return v;
		}
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			v.add(egc.getName()); // ����ͼ�������ʱ�и�ʽ,���Ը�ֵֻ����ԭʼ����,�����ô���
		}
		return v;
	}

	/**
	 * �г���������������ϵ�е�����
	 * 
	 * @param cats
	 *            ��������(����ϵ��)
	 * @return ϵ�е�����
	 */
	public Vector listSeriesNames(ExtGraphCategory[] cats) {
		return listSeriesNames(getArrayList(cats));
	}

	protected String getReportASeriesName(Object series) {
		return null;
	}

	/**
	 * �г���������������ϵ�е�����
	 * 
	 * @param cats
	 *            ��������(����ϵ��)
	 * @return ϵ�е�����
	 */
	public Vector listSeriesNames(ArrayList cats) {
		Vector names = new Vector();
		for (int c = 0; c < cats.size(); c++) {
			ArrayList series = ((ExtGraphCategory) cats.get(c)).getSeries();
			for (int i = 0; i < series.size(); i++) {
				Object ser = series.get(i);
				String name = "";
				if (_instanceof(ser, "ExtGraphSery")) {
					ExtGraphSery egs = (ExtGraphSery) ser;
					name = egs.getName();
				} else {
					name = getReportASeriesName(ser);
				}
				if (names.contains(name)) {
					continue;
				}
				names.add(name);
			}
		}
		return names;
	}

	/**
	 * ����Ҫָ��ϵ��������ʱ���ڷ���������ҵ���һ��ͬ����ϵ�ж��󼴿� �ú���ֻ����ָ��ϵ�����е��ã����򵱸��ص�ͼ��ʱ��ϵ�ж�����
	 * ExtGraphSery
	 * 
	 * @param sname
	 *            ϵ������
	 * @return ϵ�ж���
	 */
	public ExtGraphSery getEGS(String sname) {
		ArrayList cats = categories;
		for (int c = 0; c < cats.size(); c++) {
			ArrayList series = ((ExtGraphCategory) cats.get(c)).getSeries();
			for (int i = 0; i < series.size(); i++) {
				Object ser = series.get(i);
				String name = "";
				if (_instanceof(ser, "ExtGraphSery")) {
					ExtGraphSery egs = (ExtGraphSery) ser;
					name = egs.getName();
				} else {
					name = getReportASeriesName(ser);
				}
				if (sname.equals(name)) {
					return (ExtGraphSery) ser;
				}
			}
		}
		return null;
	}

	protected Vector getReportSeriesNames(ArrayList cats) {
		return null;
	}

	/**
	 * ��ȡ��������������ϵ�е�����,��ͬ��listSeriesNames,
	 * �ú������ݱ�����ϵ�еĸ�ʽ
	 * @param cats
	 *            ��������(����ϵ��)
	 * @return ϵ�е�����
	 */
	public Vector getSeriesNames(ArrayList cats) {
		if (cats == null) {
			return new Vector();
		}
		Vector v = getReportSeriesNames(cats);
		if (v != null)
			return v;
		return listSeriesNames(cats);
	}

	/**
	 * ���캯��
	 * @param graph ͼ�����Խӿڣ���ֵΪnullʱ���㱨���е����ഴ��һ���յ�ʵ����
	 * �����ʵ������Option����������е�listCategory�Լ�listSeries�������Ӷ�����Ҫ������Щ�����ļ̳�ִ��˳��
	 */
	public ExtGraphProperty(IGraphProperty graph) {
		if( graph==null )return;
		prop = graph;
		graphType = prop.getType();
	}

	/**
	 * �μ�PublicPropertyͬ������
	 */
	public byte getCurveType() {
		return prop.getCurveType();
	}

	/**
	 * ��ͼʱ�Ƿ����һ�������ʾ
	 * @return �Ƿ���true������false
	 */
	public boolean isCutPie() {
		return prop.isPieSpacing();
	}

	/**
	 * �μ�PublicPropertyͬ������
	 */
	public boolean isMeterColorEnd() {
		return prop.isMeterColorEnd();
	}

	/**
	 * �μ�PublicPropertyͬ������
	 */
	public boolean isMeterTick() {
		return prop.isMeterTick();
	}

	/**
	 * �μ�PublicPropertyͬ������
	 */
	public int getMeter3DEdge() {
		return prop.getMeter3DEdge();
	}

	/**
	 * �μ�PublicPropertyͬ������
	 */
	public int getMeterRainbowEdge() {
		return prop.getMeterRainbowEdge();
	}

	/**
	 * �μ�PublicPropertyͬ������
	 */
	public int getPieLine() {
		return prop.getPieLine();
	}

	/**
	 * �жϵ�ǰͼ���Ƿ�˫��ͼ
	 * @return ˫��ͼ�η���true�����򷵻�false
	 */
	public boolean is2YGraph() {
		byte type = this.getType();
		return type == GraphTypes.GT_2Y2LINE || type == GraphTypes.GT_2YCOLLINE
				|| type == GraphTypes.GT_2YCOLSTACKEDLINE;
	}

	/**
	 * �жϵ�ǰͼ���Ƿ��Ϊ��ͨ�Ķѻ�ͼ����������˫��Ķѻ�
	 * @return ��ͨ�ѻ�ͼ����true�����򷵻�false
	 */
	public boolean isNormalStacked() {
		return isNormalStacked(getType());
	}

	/**
	 * �ж�typeͼ���Ƿ��Ϊ��ͨ�Ķѻ�ͼ����������˫��Ķѻ�
	 * @param type ͼ����
	 * @return ��ͨ�ѻ�ͼ����true�����򷵻�false
	 */
	public static boolean isNormalStacked(byte type) {
		return type == GraphTypes.GT_BARSTACKED
				|| type == GraphTypes.GT_BARSTACKED3DOBJ
				|| type == GraphTypes.GT_COLSTACKED
				|| type == GraphTypes.GT_COLSTACKED3DOBJ;
	}

	/**
	 * �жϵ�ǰͼ��instance�Ƿ�Ϊ�ѻ�ͼ��
	 * @param instance ͼ��ʵ�ֵ�ʵ��
	 * @return �ѻ�ͼ��ʱ����true�����򷵻�false
	 */
	public boolean isStackedGraph(DrawBase instance) {
		if (instance == null) {
			byte type = this.getType();
			return type == GraphTypes.GT_BARSTACKED
					|| type == GraphTypes.GT_BARSTACKED3DOBJ
					|| type == GraphTypes.GT_COLSTACKED
					|| type == GraphTypes.GT_2YCOLSTACKEDLINE
					|| type == GraphTypes.GT_COLSTACKED3DOBJ;
		} else {
			String className = instance.getClass().getName();
			boolean isStacked = className.indexOf("Stacked") > 0;
			return isStacked;
		}
	}

	/**
	 * �жϵ�ǰͼ��instance�Ƿ�Ϊ����ͼ�������������ͼ
	 * @param instance ͼ��ʵ�ֵ�ʵ��
	 * @return ����ͼʱ����true�����򷵻�false
	 */
	public boolean isBarGraph(DrawBase instance) {
		if (instance == null) {
			byte type = this.getType();
			return (type == GraphTypes.GT_BAR || type == GraphTypes.GT_BAR3D
					|| type == GraphTypes.GT_BAR3DOBJ
					|| type == GraphTypes.GT_BARSTACKED || type == GraphTypes.GT_BARSTACKED3DOBJ);
		} else {
			String className = instance.getClass().getName();
			boolean isBar = className.indexOf("Bar") > 0;
			return isBar;
		}
	}

	/**
	 * ���ݷ�������catName��ȡ��չͼ�η������
	 * @param catName ���������
	 * @return ��չͼ�η���
	 */
	public ExtGraphCategory getExtGraphCategory(Object catName) {
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			if (catName.equals(egc.getName())) {
				return egc;
			}
		}
		return null;
	}

	/**
	 * �Ƿ�����ͳ��ͼ,��ʱ������ͼ,����ͼ,��̱�,ʱ��״̬ͼ���������ͼ��
	 * 
	 * @return boolean ����ͼ�η���true�����򷵻�false
	 */
	public static boolean isNormalGraph(byte type) {
		return (type != GraphTypes.GT_TIMETREND
				&& type != GraphTypes.GT_TIMESTATE
				&& type != GraphTypes.GT_GANTT && type != GraphTypes.GT_GONGZI
				&& type != GraphTypes.GT_RANGE && type != GraphTypes.GT_MILEPOST);
	}

	protected void reportRecalcProperty() {
	}

	/**
	 * ���°�������ֵ��������������Ӧ����
	 */
	public void recalcProperty() {
		discardNoNameData();
		if (isNormalGraph(getType()) && topN > 0) {
			extractTopNCat();
		}
		if (is2YGraph()) {
			split2YCat();
		}
		reportRecalcProperty();
	}

	protected void splitCategory(Vector seriesName1, Vector seriesName2) {
		ArrayList newCat1 = new ArrayList();
		ArrayList newCat2 = new ArrayList();

		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);

			ExtGraphCategory egc1 = new ExtGraphCategory();
			egc1.setName(egc.getName());
			ArrayList ser1 = new ArrayList();
			for (int j = 0; j < seriesName1.size(); j++) {
				String name = (String) seriesName1.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				ser1.add(egs);
			}
			egc1.setSeries(ser1);
			newCat1.add(egc1);

			ExtGraphCategory egc2 = new ExtGraphCategory();
			egc2.setName(egc.getName());
			ArrayList ser2 = new ArrayList();
			for (int j = 0; j < seriesName2.size(); j++) {
				String name = (String) seriesName2.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				// if (egs != null) {
				ser2.add(egs);
				// }
			}
			egc2.setSeries(ser2);
			newCat2.add(egc2);
		}
		this.categories = newCat1;
		this.category2 = newCat2;
	}

	/**
	 * ���ø�����ָ�����
	 * @param set �Ƿ���ָ�
	 */
	public void setSplitByAxis(boolean set) {
		isSplitByAxis = set;
	}

	protected boolean isSplitByAxis() {
		return isSplitByAxis;
	}

	private void split2YCat() {
		if (isSplitByAxis()) {
			Vector allSeriesName = getSeriesNames(categories);
			int total;
			total = allSeriesName.size();

			Vector seriesName1 = new Vector();
			Vector seriesName2 = new Vector();
			for (int i = 0; i < total; i++) {
				Object sName = allSeriesName.get(i);
				ExtGraphSery egs = getEGS(sName.toString());
				if (egs.getAxis() == Consts.AXIS_RIGHT) {
					seriesName2.add(sName);
				} else {
					seriesName1.add(sName);
				}
			}
			splitCategory(seriesName1, seriesName2);
		} else {
			autoSplit2YCat();
		}
	}

	private void autoSplit2YCat() {
		Vector allSeriesName = getSeriesNames(categories);
		int total, s;
		total = allSeriesName.size();

		Vector seriesName1 = new Vector();
		Vector seriesName2 = new Vector();
		s = (int) ((total + 1) / 2);
		for (int i = 0; i < allSeriesName.size(); i++) {
			if (i >= s) {
				seriesName2.add(allSeriesName.get(i));
			} else {
				seriesName1.add(allSeriesName.get(i));
			}
		}
		ArrayList newCat1 = new ArrayList();
		ArrayList newCat2 = new ArrayList();

		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);

			ExtGraphCategory egc1 = new ExtGraphCategory();
			egc1.setName(egc.getName());
			ArrayList ser1 = new ArrayList();
			for (int j = 0; j < seriesName1.size(); j++) {
				String name = (String) seriesName1.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				ser1.add(egs);
			}
			egc1.setSeries(ser1);
			newCat1.add(egc1);

			ExtGraphCategory egc2 = new ExtGraphCategory();
			egc2.setName(egc.getName());
			ArrayList ser2 = new ArrayList();
			for (int j = 0; j < seriesName2.size(); j++) {
				String name = (String) seriesName2.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				ser2.add(egs);
			}
			egc2.setSeries(ser2);
			newCat2.add(egc2);
		}
		this.categories = newCat1;
		this.category2 = newCat2;
	}

	private void discardNoNameData() {
		if (categories == null || categories.size() == 0) {
			throw new RuntimeException(
					"Error��Graph does not define categories!");
		}
		for (int i = categories.size() - 1; i >= 0; i--) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			if (!StringUtils.isValidString(egc.getNameString())) {
				categories.remove(i);
				continue;
			}
			ArrayList series = egc.getSeries();
			for (int j = series.size() - 1; j >= 0; j--) {
				Object objSer = series.get(j);
				String className = objSer.getClass().getName();
				if (_instanceof(objSer, "ExtGraphSery")) {
					ExtGraphSery egs = (ExtGraphSery) objSer;
					if (!StringUtils.isValidString(egs.getName())
							&& egs.isNull()) { // ���ҽ���ϵ�����ƺ�ֵ��Ϊ�յ�ʱ�򣬺��Ը�ϵ��
						series.remove(j);
						continue;
					}
				}
			}
		}
	}

	/**
	 * ��װһ��ʵ���жϵ�д�������������д
	 * @param ins ʵ������
	 * @param className �������
	 * @return ����ǵ�ǰ���ʵ���򷵻�true�����򷵻�false
	 */
	public static boolean _instanceof(Object ins, String className) {
		return ins.getClass().getName().endsWith(className);
	}

	private void extractTopNCat() {
		int originCatNum = categories.size();
		if (topN <= 0 || topN > (originCatNum - 2)) { // ���Otherֻ��һ����������û��ʱ,û������
			return;
		}

//		categories�ĳ�ԱExtGraphproperty����sumSeriesϵ�к�ʵ����Compare����������ʹ������sort
//		����ȡǰ����������sortΪ����Ȼ���ٷ�ת����reverse�����ǽ���λ��һ��
//		xq 2025��1��15��
		Collections.sort(categories);
		Collections.reverse(categories);
//		com.scudata.ide.common.GM.sort(categories, false); // ����ÿ�������ϵ�к�����,ȡǰTopN��

		ArrayList dataCategory = new ArrayList();
		for (int i = 0; i < topN; i++) {
			dataCategory.add(categories.get(i));
		}
		if (getFlag(IGraphProperty.FLAG_DISCARDOTHER)) {
			this.categories = dataCategory;
			return;
		}
		ExtGraphCategory otherCategoryData = new ExtGraphCategory();
		if (DrawBase.isChinese()) {
			otherCategoryData.setName("����");
		} else {
			otherCategoryData.setName("Other");
		}

		HashMap otherSeriesData = new HashMap();
		for (int i = topN; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			ArrayList series = egc.getSeries();
			for (int j = 0; j < series.size(); j++) {
				ExtGraphSery egs = (ExtGraphSery) series.get(j);
				Double seriesValue = (Double) otherSeriesData
						.get(egs.getName());
				if (seriesValue == null) {
					otherSeriesData.put(egs.getName(),
							new Double(egs.getValue()));
				} else {
					otherSeriesData.put(
							egs.getName(),
							new Double(seriesValue.doubleValue()
									+ egs.getValue()));
				}
			}
		}
		ArrayList otherSeries = new ArrayList();
		Iterator it = otherSeriesData.keySet().iterator();
		while (it.hasNext()) {
			String sName = (String) it.next();
			ExtGraphSery sd = new ExtGraphSery();
			sd.setName(sName);
			sd.setValue((Number) otherSeriesData.get(sName));
			otherSeries.add(sd);
		}
		otherCategoryData.setSeries(otherSeries);
		dataCategory.add(otherCategoryData);
		this.categories = dataCategory;
	}

	/**
	 * ȡͳ��ͼ����
	 * 
	 * @return byte ͳ��ͼ���ͣ���GraphTypes�еĳ�������
	 */
	public byte getType() {
		return graphType;
	}

	/**
	 * �Ƿ����ϵ�л�ͼ������
	 * @return �������ϵ�л��������շ��໭
	 */
	public boolean isLegendOnSery() {
		return prop.getDrawLegendBySery();
	}

	/**
	 * ȡ��������ɫ
	 * 
	 * @return int ��������ɫ
	 */
	public int getAxisColor() {
		return prop.getAxisColor();
	}

	/**
	 * ȡȫͼ������ɫ
	 * 
	 * @return int��ȫͼ������ɫ
	 */
	public int getCanvasColor() {
		return prop.getCanvasColor();
	}

	/**
	 * ȡͼ����������ɫ
	 * 
	 * @return int��ͼ����������ɫ
	 */
	public int getGraphBackColor() {
		return prop.getGraphBackColor();
	}

	/**
	 * ȡͳ��ͼ���ඨ��
	 * 
	 * @return ArrayList (GraphCategory)��ͳ��ͼ���ඨ��
	 */
	public ArrayList getCategories() {
		return this.categories;
	}

	/**
	 * ȡ�������
	 * 
	 * @return String �������
	 */
	public String getXTitle() {
		return this.xTitle;
	}

	/**
	 * ȡ�������Ķ��뷽ʽ
	 * @return ���뷽ʽ
	 */
	public byte getXTitleAlign() {
		return prop.getXTitleAlign();
	}

	/**
	 * ȡ�������
	 * 
	 * @return String �������
	 */
	public String getYTitle() {
		return this.yTitle;
	}

	/**
	 * ȡ�������Ķ��뷽ʽ
	 * @return ���뷽ʽ
	 */
	public byte getYTitleAlign() {
		return prop.getYTitleAlign();
	}

	/**
	 * ȡͳ��ͼ����
	 * 
	 * @return String��ͳ��ͼ����
	 */
	public String getGraphTitle() {
		return this.graphTitle;
	}

	/**
	 * ȡͳ��ͼ����Ķ��뷽ʽ
	 * @return ���뷽ʽ
	 */
	public byte getGraphTitleAlign() {
		return prop.getGraphTitleAlign();
	}

	/**
	 * ȡ����������
	 * 
	 * @return byte�����������ͣ�ֵΪLINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *         LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public byte getGridLineType() {
		return prop.getGridLineType();
	}

	/**
	 * �ο�PublicProperty��ͬ������
	 * @return
	 */
	public byte getGridLocation() {
		return prop.getGridLocation();
	}

	/**
	 * ȡ��������ɫ
	 * 
	 * @return int����������ɫ
	 */
	public int getGridLineColor() {
		return prop.getGridLineColor();
	}

	/**
	 * ȡ����ͼ������ͼ���
	 * 
	 * @return double������ͼ������ͼ���
	 */
	public double getBarDistance() {
		return this.barDistance;
	}

	/**
	 * ȡͼ�θ�ʽ
	 * 
	 * @return byte��ͼ�θ�ʽ, ֵΪIMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public byte getImageFormat() {
		return prop.getImageFormat();
	}

	/**
	 * �ο�PublicProperty��ͬ������
	 * @return
	 */
	public boolean isGraphTransparent() {
		return prop.isGraphTransparent();
	}

	/**
	 * �ο�PublicProperty��ͬ������
	 * @return
	 */
	public boolean isDrawDataTable() {
		return prop.isDrawDataTable();
	}

	/**
	 * �ο�PublicProperty��ͬ������
	 * @return
	 */
	public boolean isDataCenter() {
		return prop.isDataCenter();
	}

	/**
	 * ȡ�Ƿ񽥱�ɫ
	 * 
	 * @return boolean
	 */
	public boolean isGradientColor() {
		return prop.isGradientColor();
	}

	/**
	 * ȡͳ��ͼ����
	 * 
	 * @return GraphFonts��ͳ��ͼ����
	 */
	public GraphFonts getFonts() {
		return prop.getFonts();
	}

	/**
	 * ȡ�����߶���
	 * 
	 * @return ArrayList (ExtAlarmLine)�������߶���
	 */
	public ArrayList getAlarmLines() {
		if (alarms == null) {
			AlarmLine[] als = prop.getAlarmLines();
			if (als == null || als.length == 0) {
				return null;
			}
			alarms = new ArrayList();
			for (int i = 0; i < als.length; i++) {
				ExtAlarmLine eal = new ExtAlarmLine();
				eal.setAlarmValue(Double.parseDouble(als[i].getAlarmValue()));
				eal.setColor(als[i].getColor());
				eal.setLineThick(GraphParam.getLineThick(als[i].getLineThick()));
				eal.setLineType(als[i].getLineType());
				eal.setName(als[i].getName());
				eal.setDrawAlarmValue(als[i].isDrawAlarmValue());
				alarms.add(eal);
			}
		}
		return this.alarms;
	}

	public void setAlarmLines(ArrayList alarm) {
		this.alarms = alarm;
	}

	/**
	 * ȡͼ����ʾ���ݶ���
	 * 
	 * @return byte��ͼ����ʾ���ݶ��壬ֵΪDISPDATA_NONE, DISPDATA_VALUE,
	 *         DISPDATA_PERCENTAGE
	 */
	public byte getDisplayData() {
		return prop.getDisplayData();
	}
	public byte getDisplayData2() {
		return prop.getDisplayData2();
	}

	/**
	 * �ο�PublicProperty��ͬ������
	 * @return
	 */
	public boolean isDispStackSumValue() {
		return prop.isDispStackSumValue();
	}

	/**
	 * ȡͼ����ʾ���ݸ�ʽ����
	 * 
	 * @return String��ͼ����ʾ���ݸ�ʽ����
	 */
	public String getDisplayDataFormat1() {
		return this.dataFormat1;
	}

	/**
	 * ȡ����ͼ����ʾ���ݸ�ʽ����
	 * 
	 * @return String��ͼ����ʾ���ݸ�ʽ����
	 */
	public String getDisplayDataFormat2() {
		return this.dataFormat2;
	}

	/**
	 * ȡͳ��ͼ������
	 * 
	 * @return String��ͳ��ͼ������
	 */
	public String getLink() {
		return this.link;
	}

	/**
	 * ȡͼ���ĳ�����
	 * @return ���Ӵ�
	 */
	public String getLegendLink() {
		return prop.getLegendLink();
	}

	/**
	 * ȡͳ��ͼ������Ŀ�괰��
	 * 
	 * @return String��ͳ��ͼ������Ŀ�괰��
	 */
	public String getLinkTarget() {
		return this.linkTarget;
	}

	/**
	 * ȡͳ��ͼ��ͼ��λ��
	 * 
	 * @return byte��ͳ��ͼ��ͼ��λ�ã�ֵΪLEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *         LEGEND_BOTTOM, LEGEND_NONE
	 */
	public byte getLegendLocation() {
		return prop.getLegendLocation();
	}

	/**
	 * �ο�PublicPropertyͬ������
	 * @return
	 */
	public int getLegendVerticalGap() {
		return prop.getLegendVerticalGap();
	}

	/**
	 * �ο�PublicPropertyͬ������
	 * @return
	 */
	public int getLegendHorizonGap() {
		return prop.getLegendHorizonGap();
	}

	/**
	 * ȡͳ��ͼ����ɫ������
	 * 
	 * @return String��ͳ��ͼ����ɫ������
	 */
	public Palette getPlatte() {
		return this.palette;
	}

	/**
	 * ȡͳ��ֵ��ʼֵ
	 * 
	 * @return String��ͳ��ֵ��ʼֵ
	 */
	public double getYStartValue1() {
		return this.yStartValue1;
	}

	/**
	 * ȡ˫��ͼ�ڶ������ʼֵ
	 * @return ͳ����ʼֵ
	 */
	public double getYStartValue2() {
		return this.yStartValue2;
	}

	/**
	 * ȡͳ��ֵ����ֵ
	 * 
	 * @return String��ͳ��ֵ����ֵ
	 */
	public double getYEndValue1() {
		return this.yEndValue1;
	}

	/**
	 * ȡ˫��ͼ�ڶ���Ľ���ֵ
	 * @return ����ֵ
	 */
	public double getYEndValue2() {
		return this.yEndValue2;
	}

	/**
	 * ȡͳ��ֵ��ǩ���
	 * 
	 * @return double��ͳ��ֵ��ǩ���
	 */
	public double getYInterval1() {
		return this.yInterval1;
	}

	/**
	 * ȡ˫��ͼ�ڶ���ı�ǩ���
	 * @return ��ǩ���
	 */
	public double getYInterval2() {
		return this.yInterval2;
	}

	/**
	 * ȡͳ��ֵ������λ
	 * 
	 * @return double��ͳ��ֵ������λ��ֵΪUNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *         UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION, UNIT_100MILLION,
	 *         UNIT_BILLION, UNIT_001, UNIT_0001, UNIT_00001, UNIT_0000001
	 */
	public double getDataUnit() {
		return prop.getDataUnit();
	}

	/**
	 * ȡ��ͳ��ֵ���ٿ̶���
	 * 
	 * @param int ͳ��ֵ���ٿ̶���
	 */
	public int getYMinMarks() {
		return this.yMinMarks;
	}

	/**
	 * ȡ������ͼ��֮��ļ��
	 * 
	 * @return double��������ͼ��֮��ļ��
	 */
	public double getTitleMargin() {
		return this.titleMargin;
	}

	/**
	 * ȡ����ͼ�Ƿ��ע���ݵ�
	 * 
	 * @return boolean
	 */
	public boolean isDrawLineDot() {
		return prop.isDrawLineDot();
	}

	/**
	 * �ο�PublicPropertyͬ������
	 * @return
	 */
	public boolean isOverlapOrigin() {
		return prop.isOverlapOrigin();
	}

	/**
	 * ȡ����ͼ�Ƿ�������
	 * 
	 * @return boolean
	 */
	public boolean isDrawLineTrend() {
		return prop.isDrawLineTrend();
	}

	/**
	 * ����ͼ�Ƿ���Կ�ֵ
	 * 
	 * @return boolean
	 */
	public boolean isIgnoreNull() {
		return prop.ignoreNull();
	}

	/**
	 * �Զ���ͼ������
	 * 
	 * @return String
	 */
	public String getCustomClass() {
		return prop.getCustomClass();
	}

	/**
	 * �Զ���ͼ�β���
	 * 
	 * @return String
	 */
	public String getCustomParam() {
		return prop.getCustomParam();
	}

	/**
	 * ȡ����ͼ��ϸ��
	 * 
	 * @return boolean
	 */
	public byte getLineThick() {
		return prop.getLineThick();
	}

	/**
	 * �ο�PublicPropertyͬ������
	 * @return
	 */
	public byte getLineStyle() {
		return prop.getLineStyle();
	}

	/**
	 * ȡ������ֵ���ǩ�ص�ʱ�Ƿ���ʾ��һ��ֵ���ǩ
	 * 
	 * @return boolean
	 */
	public boolean isShowOverlapText() {
		return prop.isShowOverlapText();
	}

	/**
	 * ȡ�������ǩ���
	 * 
	 * @return double���������ǩ���
	 */
	public double getXInterval() {
		return this.xInterval;
	}

	/**
	 * ȡʱ������ͼ����
	 * 
	 * @return ArrayList��(ExtTimeTrendXValue) ʱ������ͼ����
	 */
	public ArrayList getTimeTrendXValues() {
		return this.ttXValues;
	}

	/**
	 * ȡʱ��״̬ͼ�����ͼ״̬�����
	 * 
	 * @return int��ʱ��״̬ͼ�����ͼ״̬�����
	 */
	public int getStatusBarWidth() {
		return this.statusBarWidth;
	}

	/**
	 * ȡʱ��״̬ͼ�����ͼʱ��̶�����
	 * 
	 * @return byte��ʱ��״̬ͼ�����ͼʱ��̶����ͣ�ֵΪTIME_YEAR, TIME_MONTH, TIME_DAY,
	 *         TIME_HOUR, TIME_MINUTE, TIME_SECOND
	 */
	public byte getStatusTimeType() {
		return prop.getStatusTimeType();
	}

	/**
	 *  �û��Ƿ���������ͼ������ͼ���
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetBarDistance() {
		return ((userSetStatus & BAR_DISTANCE) == BAR_DISTANCE);
	}

	/**
	 * �û��Ƿ�������ǰN�����ݻ�ͼ 
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetTopData() {
		return ((userSetStatus & TOP_DATA_N) == TOP_DATA_N);
	}

	/**
	 * �û��Ƿ�����ͳ��ֵ��ʼֵ
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetYStartValue1() {
		return ((userSetStatus & Y_START_VALUE1) == Y_START_VALUE1);
	}

	/**
	 * �û��Ƿ����õڶ����ͳ��ֵ��ʼֵ
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetYStartValue2() {
		return ((userSetStatus & Y_START_VALUE2) == Y_START_VALUE2);
	}

	/**
	 * �û��Ƿ�����ͳ��ֵ����ֵ
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetYEndValue1() {
		return ((userSetStatus & Y_END_VALUE1) == Y_END_VALUE1);
	}

	/**
	 * �û��Ƿ������˵ڶ���ͳ��ֵ����ֵ
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetYEndValue2() {
		return ((userSetStatus & Y_END_VALUE2) == Y_END_VALUE2);
	}

	/**
	 * �û��Ƿ�����ͳ��ֵ��ǩ���
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetYInterval1() {
		return ((userSetStatus & Y_INTERVAL1) == Y_INTERVAL1);
	}

	/**
	 * �û��Ƿ������˵ڶ���ͳ��ֵ��ǩ���
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetYInterval2() {
		return ((userSetStatus & Y_INTERVAL2) == Y_INTERVAL2);
	}

	/**
	 * �û��Ƿ�����ͳ��ֵ���ٿ̶���
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetYMinMarks() {
		return ((userSetStatus & Y_MIN_MARK) == Y_MIN_MARK);
	}

	/**
	 * �û��Ƿ����ñ�����ͼ��֮��ļ��
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetTitleMargin() {
		return ((userSetStatus & TITLE_MARGIN) == TITLE_MARGIN);
	}

	/**
	 * �û��Ƿ����÷������ǩ���
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetXInterval() {
		return ((userSetStatus & X_INTERVAL) == X_INTERVAL);
	}

	/**
	 * �û��Ƿ�����ʱ��״̬ͼ�����ͼ״̬�����
	 * @return ���������Է���true������false
	 */
	public boolean isUserSetStatusBarWidth() {
		return ((userSetStatus & STATUS_BAR_WIDTH) == STATUS_BAR_WIDTH);
	}

	/**
	 * �Ƿ������Ӱ
	 * @return ������Ӱ����true�����򷵻�false
	 */
	public boolean isDrawShade() {
		if (isStackedGraph(null)) {
			return false;
		}
		if (graphType == GraphTypes.GT_COL3D)
			return false;
		// ������ƽ̨����ʱҲ��������Ӱ
		// �ѻ�ͼ����Ӱ���ص������û�
		return prop.isDrawShade();
	}

	/**
	 * �ο�PublicPropertyͬ������
	 * @return
	 */
	public boolean isRaisedBorder() {
		return prop.isRaisedBorder();
	}

	/**
	 * �ο�PublicPropertyͬ������
	 * @return
	 */
	public boolean getFlag(byte key) {
		return prop.getFlag(key);
	}

	/**
	 * ȡ����ͼ����
	 * @return ���ö���
	 */
	public BackGraphConfig getBackGraphConfig() {
		return bgc;
	}

	/**
	 * ���ñ���ͼ����
	 * @param bgc ���ö���
	 */
	public void setBackGraphConfig(BackGraphConfig bgc) {
		this.bgc = bgc;
	}

	/**
	 * ͼ�εİ汾����7ʱ������ͼ�ξ��ε�4�߷ֱ����ò�ͬ����ɫ ��ʱԭ����AxisColor�����ϣ�˳������Ϊ�������� AXIS_ID
	 * ΪGraphProperty�ж����AXIS_��ͷ�ĳ���
	 * 
	 * @return Color, ���ΪNull���ʾΪ͸��ɫ
	 */
	public Color getAxisColor(int AXIS_ID) {
		int c = prop.getAxisColors()[AXIS_ID];
		if (c == 16777215) {
			return null;
		} else {
			return new Color(c);
		}
	}

	/**
	 * ����ͳ��ͼ����
	 * 
	 * @param type
	 *            ͳ��ͼ���ͣ���GraphTypes�еĳ�������
	 */
	public void setType(byte type) {
		graphType = type;
	}

	/**
	 * ������������ɫ
	 * 
	 * @param color
	 *            ��������ɫ
	 */
	public void setAxisColor(int color) {
		prop.setAxisColor(color);
	}

	/**
	 * ����ȫͼ������ɫ
	 * 
	 * @param color
	 *            ��ȫͼ������ɫ
	 */
	public void setCanvasColor(int color) {
		prop.setCanvasColor(color);
	}

	/**
	 * ����ͼ����������ɫ
	 * 
	 * @param color
	 *            ��ͼ����������ɫ
	 */
	public void setGraphBackColor(int color) {
		prop.setGraphBackColor(color);
	}

	/**
	 * ����ͳ��ͼ����
	 * 
	 * @param categorys
	 *            ��(ExtGraphProperty ) ͳ��ͼ����
	 */
	public void setCategories(ArrayList categorys) {
		this.categories = categorys;
	}

	/**
	 * ���ú������
	 * 
	 * @param title
	 *            �������
	 */
	public void setXTitle(String title) {
		this.xTitle = title;
	}

	/**
	 * �����������
	 * 
	 * @param title
	 *            �������
	 */
	public void setYTitle(String title) {
		this.yTitle = title;
	}

	/**
	 * ����ͳ��ͼ����
	 * 
	 * @return title��ͳ��ͼ����
	 */
	public void setGraphTitle(String title) {
		this.graphTitle = title;
	}

	/**
	 * ��������������
	 * 
	 * @param type
	 *            �����������ͣ�ֵΪLINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *            LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public void setGridLineType(byte type) {
		prop.setGridLineType(type);
	}

	/**
	 * ������������ɫ
	 * 
	 * @param color
	 *            ����������ɫ
	 */
	public void setGridLineColor(int color) {
		prop.setGridLineColor(color);
	}

	/**
	 * ��������ͼ������ͼ���
	 * 
	 * @param distance
	 *            ������ͼ������ͼ���
	 */
	public void setBarDistance(double distance) {
		userSetStatus |= BAR_DISTANCE;
		this.barDistance = distance;
	}

	/**
	 * ����ͼ�θ�ʽ
	 * 
	 * @param format
	 *            ��ͼ�θ�ʽ, ֵΪIMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public void setImageFormat(byte format) {
		prop.setImageFormat(format);
	}

	/**
	 * ����ͼ���Ƿ�͸��
	 * 
	 * @param b
	 */
	public void setGraphTransparent(boolean b) {
		prop.setGraphTransparent(b);
	}

	/**
	 * �����Ƿ񽥱�ɫ
	 * 
	 * @param b
	 */
	public void setGradientColor(boolean b) {
		prop.setGradientColor(b);
	}

	/**
	 * ������ǰN�����ݻ�ͼ
	 * 
	 * @param n
	 *            ��ǰN������
	 */
	public void setTopData(int n) {
		userSetStatus |= TOP_DATA_N;
		this.topN = n;
	}

	/**
	 * ����ͳ��ͼ����
	 * 
	 * @param font
	 *            ��ͳ��ͼ����
	 */
	public void setFonts(GraphFonts font) {
		prop.setFonts(font);
	}

	/**
	 * ����ͼ����ʾ����
	 * 
	 * @param data
	 *            ��ͼ����ʾ���ݣ�ֵΪDISPDATA_NONE, DISPDATA_VALUE, DISPDATA_PERCENTAGE
	 */
	public void setDisplayData(byte data) {
		prop.setDisplayData(data);
	}
	public void setDisplayData2(byte data) {
		prop.setDisplayData2(data);
	}

	/**
	 * ����ͼ����ʾ���ݸ�ʽ
	 * 
	 * @param format
	 *            ��ͼ����ʾ���ݸ�ʽ
	 */
	public void setDisplayDataFormat1(String format) {
		this.dataFormat1 = format;
	}

	public void setDisplayDataFormat2(String format) {
		this.dataFormat2 = format;
	}

	/**
	 * ����ͳ��ͼ������
	 * 
	 * @param link
	 *            ��ͳ��ͼ������
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * ����ͳ��ͼ������Ŀ�괰��
	 * 
	 * @param target
	 *            ��ͳ��ͼ������Ŀ�괰��
	 */
	public void setLinkTarget(String target) {
		this.linkTarget = target;
	}

	/**
	 * ����ͳ��ͼ��ͼ��λ��
	 * 
	 * @param location
	 *            ��ͳ��ͼ��ͼ��λ�ã�ֵΪLEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *            LEGEND_BOTTOM, LEGEND_NONE
	 */
	public void setLegendLocation(byte location) {
		prop.setLegendLocation(location);
	}

	/**
	 * ����ͳ��ͼ����ɫ������
	 * 
	 * @param config
	 *            ��ͳ��ͼ����ɫ������
	 */
	public void setPalette(Palette palette) {
		this.palette = palette;
	}

	/**
	 * ����ͳ��ֵ��ʼֵ
	 * 
	 * @param value
	 *            ��ͳ��ֵ��ʼֵ
	 */
	public void setYStartValue1(double value) {
		userSetStatus |= Y_START_VALUE1;
		this.yStartValue1 = value;
	}

	public void setYStartValue2(double value) {
		userSetStatus |= Y_START_VALUE2;
		this.yStartValue2 = value;
	}

	/**
	 * ����ͳ��ֵ����ֵ
	 * 
	 * @param value
	 *            ��ͳ��ֵ����ֵ
	 */
	public void setYEndValue1(double value) {
		userSetStatus |= Y_END_VALUE1;
		this.yEndValue1 = value;
	}

	public void setYEndValue2(double value) {
		userSetStatus |= Y_END_VALUE2;
		this.yEndValue2 = value;
	}

	/**
	 * ����ͳ��ֵ��ǩ���
	 * 
	 * @param interval
	 *            ��ͳ��ֵ��ǩ���
	 */
	public void setYInterval1(double interval) {
		userSetStatus |= Y_INTERVAL1;
		this.yInterval1 = interval;
	}

	public void setYInterval2(double interval) {
		userSetStatus |= Y_INTERVAL2;
		this.yInterval2 = interval;
	}

	/**
	 * ����ͳ��ֵ������λ
	 * 
	 * @param unit
	 *            ��ͳ��ֵ������λ��ֵΪUNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *            UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION,
	 *            UNIT_100MILLION, UNIT_BILLION, UNIT_001, UNIT_0001,
	 *            UNIT_00001, UNIT_0000001
	 */
	public void setDataUnit(double unit) {
		prop.setDataUnit(unit);
	}

	/**
	 * ����ͳ��ֵ���ٿ̶���
	 * 
	 * @param mark
	 *            ͳ��ֵ���ٿ̶���
	 */
	public void setYMinMarks(int mark) {
		userSetStatus |= Y_MIN_MARK;
		this.yMinMarks = mark;
	}

	/**
	 * ���ñ�����ͼ��֮��ļ��
	 * 
	 * @param margin
	 *            ��������ͼ��֮��ļ��
	 */
	public void setTitleMargin(double margin) {
		userSetStatus |= TITLE_MARGIN;
		this.titleMargin = margin;
	}

	/**
	 * ��������ͼ�Ƿ��ע���ݵ�
	 * 
	 * @param b
	 */
	public void setDrawLineDot(boolean b) {
		prop.setDrawLineDot(b);
	}

	/**
	 * ����������ֵ���ǩ�ص�ʱ�Ƿ���ʾ��һ��ֵ���ǩ
	 * 
	 * @param b
	 */
	public void setShowOverlapText(boolean b) {
		prop.setShowOverlapText(b);
	}

	/**
	 * ���÷������ǩ���
	 * 
	 * @param interval
	 *            ���������ǩ���
	 */
	public void setXInterval(double interval) {
		userSetStatus |= X_INTERVAL;
		this.xInterval = interval;
	}

	/**
	 * ����ʱ������ͼ����
	 * 
	 * @param value
	 *            (TimeTrendXValue )��ʱ������ͼ����
	 */
	public void setTimeTrendXValues(ArrayList value) {
		this.ttXValues = value;
	}

	/**
	 * ����ʱ��״̬ͼ�����ͼ״̬�����
	 * 
	 * @param width
	 *            ʱ��״̬ͼ�����ͼ״̬�����
	 */
	public void setStatusBarWidth(int width) {
		userSetStatus |= STATUS_BAR_WIDTH;
		this.statusBarWidth = width;
	}

	/**
	 * ����ʱ��״̬ͼ�����ͼʱ��̶�����
	 * 
	 * @param type
	 *            ʱ��״̬ͼ�����ͼʱ��̶����ͣ�ȡֵΪTIME_YEAR, TIME_MONTH, TIME_DAY, TIME_HOUR,
	 *            TIME_MINUTE, TIME_SECOND
	 */
	public void setStatusTimeType(byte type) {
		prop.setStatusTimeType(type);
	}

	public double getStackedMaxValue() {
		if (categories == null) {
			return 0;
		}
		double val = 0;
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			double stackedVal = egc.getPositiveSumSeries();
			if (stackedVal > val) {
				val = stackedVal;
			}
		}
		if (is2YGraph()) {// ˫��ͼʱ����������ѻ�������ֻ�������1
			return val;
		}
		if (category2 != null) {
			for (int i = 0; i < category2.size(); i++) {
				ExtGraphCategory egc = (ExtGraphCategory) category2.get(i);
				double stackedVal = egc.getPositiveSumSeries();
				if (stackedVal > val) {
					val = stackedVal;
				}
			}
		}
		return val;
	}

	public double getStackedMinValue() {
		if (categories == null) {
			return 0;
		}
		double val = 0;
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			double stackedVal = egc.getNegativeSumSeries();
			if (stackedVal < val) {
				val = stackedVal;
			}
		}
		if (category2 != null) {
			for (int i = 0; i < category2.size(); i++) {
				ExtGraphCategory egc = (ExtGraphCategory) category2.get(i);
				double stackedVal = egc.getNegativeSumSeries();
				if (stackedVal < val) {
					val = stackedVal;
				}
			}
		}
		return val;
	}

	/**
	 * �ҳ����ඨ���е����ֵ
	 * @param cats ����
	 * @return ���ֵ
	 */
	public double getMaxValue(ArrayList cats) {
		return getTerminalValue(true, cats);
	}

	/**
	 * �ҳ����ඨ���е���Сֵ
	 * @param cats ����
	 * @return ��Сֵ
	 */
	public double getMinValue(ArrayList cats) {
		return getTerminalValue(false, cats);
	}

	/**
	 * ��other����Ϊ����ϵ��
	 * @param other ϵ������
	 */
	public void setOtherStackedSeries(String other) {
		prop.setOtherStackedSeries(other);
	}
/**
 * ��ȡ����ϵ��
 * @return ����
 */
	public String getOtherStackedSeries() {
		return prop.getOtherStackedSeries();
	}

	private double getAlarmTerminal(boolean getMax) {
		if (alarms == null)
			return 0;
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < alarms.size(); i++) {
			ExtAlarmLine eal = (ExtAlarmLine) alarms.get(i);
			double d = eal.getAlarmValue();
			if (getMax) {
				max = Math.max(max, d);
			} else {
				min = Math.min(min, d);
			}
		}
		if (getMax) {
			return max;
		} else {
			return min;
		}
	}

	private double getTerminalValue(boolean getMax, ArrayList cats) {
		double val = 0;
		if (cats != null) {
			for (int i = 0; i < cats.size(); i++) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				ArrayList series = egc.getSeries();
				for (int j = 0; j < series.size(); j++) {
					// if (! (series.get(j) instanceof ExtGraphSery)) {
					if (!_instanceof(series.get(j), "ExtGraphSery")) {
						break;
						// return 0;
					}
					ExtGraphSery egs = (ExtGraphSery) series.get(j);
					if (getMax) {
						if (egs.getValue() > val) {
							val = egs.getValue();
						}
					} else {
						if (egs.getValue() < val) {
							val = egs.getValue();
						}
					}
				}
			}
		}
		double tVal = getAlarmTerminal(getMax);
		if (getMax) {
			return Math.max(tVal, val);
		} else {
			return Math.min(tVal, val);
		}
	}

	/** ������� */
	private String xTitle;

	/** ������� */
	private String yTitle;

	/** ͳ��ͼ���� */
	private String graphTitle;

	/** ���������� */
	/** ����ͼ������ͼ��� */
	private double barDistance = 0.0;

	/** ��ǰN�����ݻ�ͼ */
	private int topN = 0; //

	/** �����߶��� */
	private ArrayList alarms = null;

	/** ͳ��ͼ������ */
	private String link;

	/** ͳ��ͼ������Ŀ�괰�� */
	private String linkTarget;

	/** ͳ��ͼ����ɫ������ */
	private Palette palette;

	/** ������ͼ��֮��ļ�� */
	private double titleMargin;

	/** ʱ������ͼ����ȡֵ */
	private ArrayList ttXValues;

	/** ʱ��״̬ͼ�����ͼ״̬����� */
	private int statusBarWidth;

	/** ����ͼ������ͼ��� */
	private static final short BAR_DISTANCE = (short) 0x01;

	/** ��ǰN�����ݻ�ͼ */
	private static final short TOP_DATA_N = (short) 0x02;

	/** ͳ��ֵ��ʼֵ */
	private static final short Y_START_VALUE1 = (short) 0x04;
	private static final short Y_START_VALUE2 = (short) 0x08;

	/** ͳ��ֵ����ֵ */
	private static final short Y_END_VALUE1 = (short) 0x10;
	private static final short Y_END_VALUE2 = (short) 0x20;

	/** ͳ��ֵ��ǩ��� */
	private static final short Y_INTERVAL1 = (short) 0x40;
	private static final short Y_INTERVAL2 = (short) 0x80;

	/** ͳ��ֵ���ٿ̶��� */
	private static final short Y_MIN_MARK = (short) 0x100;

	/** ������ͼ��֮��ļ�� */
	private static final short TITLE_MARGIN = (short) 0x200;

	/** �������ǩ��� */
	private static final short X_INTERVAL = (short) 0x400;

	/** ʱ��״̬ͼ�����ͼ״̬����� */
	private static final short STATUS_BAR_WIDTH = (short) 0x800;

	private short userSetStatus = 0; // �û��Ƿ����ø�����

	private double yStartValue1 = 0;
	/** ͳ��ֵ��ʼֵ */
	private double yStartValue2 = 0;
	private double yEndValue1 = 0;
	/** ͳ��ֵ����ֵ */
	private double yEndValue2 = 0;
	private double yInterval1 = 0;
	/** ͳ��ֵ��ǩ��� */
	private double yInterval2 = 0;
	private int yMinMarks = 0;
	/** ͳ��ֵ���ٿ̶��� */
	private double xInterval = 0;
	/** �������ǩ��� */
	private String dataFormat1;
	/** ͼ����ʾ���ݸ�ʽ���� */
	private String dataFormat2;
	/** ͼ����ʾ���ݸ�ʽ���� */
	private byte graphType;
	private IGraphProperty prop;
}
