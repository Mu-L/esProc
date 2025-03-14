package com.scudata.cellset.graph;

import java.io.*;
import java.awt.*;

import com.scudata.cellset.*;
import com.scudata.cellset.graph.config.*;
import com.scudata.common.*;

/**
 * 
 * ����ͼ�εĹ�������
 * 
 * @author Joancy
 *
 */
public class PublicProperty implements IGraphProperty, ICloneable,
		Externalizable, IRecord {
//	ͼ����ɫ16777215Ϊ͸�����༭��͸��ɫΪnull���������������ýӿڶ���ΪColor���
	/** ͳ��ͼ���� */
	private byte type = GraphTypes.GT_COL;
	private byte curveType = CURVE_LAGRANGE;
	private byte borderStyle = IStyle.LINE_SOLID;
	private float borderWidth = 0.75f;
	private int borderColor = Color.black.getRGB();
	private boolean borderShadow = true;

	/** ��������ɫ */
	private int axisColor = Color.black.getRGB();

	/** ȫͼ������ɫ */
	private int canvasColor = Color.white.getRGB();

	/** ͼ����������ɫ */
	private int graphBackColor = Color.white.getRGB();

	/** ������� */
	private String xTitle;

	/** ������� */
	private String yTitle;

	/** ͳ��ͼ���� */
	private String graphTitle;

	/** ������λ��*/
	private byte gridLineLocation = GRID_VALUE;
	/** ���������� */
	private byte gridLineType = LINE_NONE;

	/** ��������ɫ */
	private int gridLineColor = Color.lightGray.getRGB();

	/** ����ͼ������ͼ��� */
	private int barDistance;

	/** ͼ�θ�ʽ */
	private byte imageFormat = IMAGE_JPG;

	/** ͼ���Ƿ�͸�� */
	private boolean graphTransparent = false;

	/** �������ݱ� */
	private boolean isDrawDataTable = false,isDataCenter=false;
	
	/** �Ƿ񽥱�ɫ,ע���������raisedBorder�ǻ���� */
	private boolean gradientColor = true;

	/** ��ǰN�����ݻ�ͼ */
	private int topData;

	/** ʱ��״̬ͼ������ͼ����̱�ͼ��ʼʱ����ʽ */
	private String statusStartTimeExp;

	/** ʱ��״̬ͼ������ͼ����̱�ͼ����ʱ����ʽ */
	private String statusEndTimeExp;

	/** ʱ��״̬ͼ������ʽ�����ͼ����̱�ͼ����Ŀ���ʽ */
	private String statusCategoryExp;

	/** ʱ��״̬ͼ�����ͼ״̬���ʽ */
	private String statusStateExp;

	/** ʱ��״̬ͼ�����ͼ״̬����� */
	private String statusBarWidth;

	/** ʱ��״̬ͼ�����ͼʱ��̶����� */
	private byte statusTimeType = TIME_HOUR;
	private String statusTimeFormat = null;

	/** ͳ��ͼ�е����� */
	private GraphFonts fonts = new GraphFonts();

	/** �����߶��� */
	private AlarmLine[] alarms;

	/** ͼ����ʾ���ݶ��� */
	private byte displayData = DISPDATA_NONE;
	private byte displayData2 = DISPDATA_NONE;

	/** ͼ����ʾ���ݸ�ʽ���� */
	private String displayDataFormat;

	/** ͳ��ͼ������ */
	private String link;

	/** ͳ��ͼͼ�������� 2009.5.13 xq add */
	private String legendLink;

	/** ͳ��ͼ������Ŀ�괰�� */
	private String linkTarget;

	/** ͳ��ͼ��ͼ��λ�� */
	private byte legendLocation = LEGEND_NONE;

	/** ͳ��ͼ��ͼ�������� */
	private int legendVerticalGap = 4;
	/** ͳ��ͼ��ͼ�������� */
	private int legendHorizonGap = 4;

	/** �ܰ�ϵ�л�ͼ�� */
	private boolean drawLegendBySery = false;

	/** ͳ��ͼ����ɫ������ */
	private String colorConfig = "";

	/** ͳ��ֵ��ʼֵ */
	private String yStartValue;

	/** ͳ��ֵ����ֵ */
	private String yEndValue;

	/** ͳ��ֵ��ǩ��� */
	private String yInterval;

	/** ͳ��ֵ������λ */
	private double dataUnit = UNIT_ORIGIN;

	/** ͳ��ֵ���ٿ̶��� */
	private int yMinMarks = 2;

	/** ������ͼ��֮��ļ�� */
	private int titleMargin = 20;

	/** ����ͼ�Ƿ��ע���ݵ� */
	private boolean drawLineDot = true;
	private boolean isOverlapOrigin = false;

	/** ����ͼ�Ƿ������� */
	private boolean drawLineTrend = false;

	/** ����ͼֱ�ߴ�ϸ�� */
	private byte lineThick = 1;
	private byte lineStyle = LINE_SOLID;

	/** ������ֵ���ǩ�ص�ʱ�Ƿ���ʾ��һ��ֵ���ǩ */
	private boolean showOverlapText = true;

	/** ��ͼ���Ƿ�����һ����ʾ */
	private boolean pieSpacing = false;
	private boolean isMeterColorEnd = true;
	private boolean isMeterTick = false;
	private int meter3DEdge = 8;
	private int meterRainbowEdge = 16;
	private int pieLine = 8;

	private boolean isDispStackSumValue = false;
	/** �������ǩ��� */
	private int xInterval;

	/** ����ͼ�Ƿ���Կ�ֵ ֻ��������ͼ��Ч */
	private boolean ignoreNull;

	/** �Զ���ͼ������ */
	private String customClass;

	/** �Զ���ͼ����ز��� */
	private String customParam;

	/** ƽ������ͼ,ƽ���ͼ��ƽ����ͼ�Ƿ���Ӱ */
	private boolean drawShade = true;

	/** ƽ����ͼ��ͻ���߿� */
	private boolean raisedBorder = false;

	/** ͼ�εı���ͼ */
	private BackGraphConfig backGraph = null;

	/** topN ʱ�Ƿ񶪵�other���� */
	private long flag = 0;

	/** ������ɫ�Լ�ͼ���߿���ɫ��������,����������ñ��ඨ��ĳ��� AXIS_xxx */
	/** ��ɫ���壬����ԭ������ɫ������ηֿ�Ϊ�ĸ��ߵ���ɫ��˳������Ϊ�ϣ��£����ң�ͼ�� */
	private int[] axisColors = new int[20];

	private int leftMargin = 10; // ��߾�
	private int rightMargin = 10; /* �ұ߾� */
	private int topMargin = 10; /* �ϱ߾� */
	private int bottomMargin = 10; /* �±߾� */
	private int tickLen = 4; /* �̶ȳ��� */
	private int coorWidth = 100; /* 3D����ռ���п�ȵİٷֱ� */
	private double categorySpan = 190; /* ����ļ��ռ���п�ȵİٷֱ� */
	private int seriesSpan = 100; /* ���м�ļ��ռ������ȵİٷֱ� */
	private int pieRotation = 50; /* ����ռ����ĳ��Ȱٷֱ� */
	private int pieHeight = 70; /* ����ͼ�ĸ߶�ռ�뾶�İٷֱ�<=100 */

	private String otherStackedSeries=null;
	
	/**
	 * ȱʡֵ���캯��
	 */
	public PublicProperty() {
		/* Init Property ע��ó�ʼ������ͬ��һ�� ReportGraphProperty*/
		for (int i = 0; i < axisColors.length; i++) {
			axisColors[i] = 16777215;//͸��ɫ
		}
		axisColors[this.AXIS_BOTTOM]=Color.lightGray.getRGB();
		type = GraphTypes.GT_COL3DOBJ;
		graphBackColor = new Color(187,192,181).getRGB();
		this.gridLineType = LINE_SOLID;
		this.gridLineColor = new Color(172,187,153).getRGB();
		this.gradientColor = true;
		this.imageFormat = IMAGE_SVG;
		this.pieSpacing = false;
	}

	/**
	 * ȡͳ��ͼ����
	 * 
	 * @return byte ͳ��ͼ���ͣ���GraphTypes�еĳ�������
	 */
	public byte getType() {
		return type;
	}

	/**
	 * ����ͳ��ͼ����
	 * 
	 * @param type ͳ��ͼ���ͣ���GraphTypes�еĳ�������
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * ������������
	 * @param curveType ����ֵ���ο�	IGraphProperty.CURVE_XXX
	 */
	public void setCurveType(byte curveType) {
		this.curveType = curveType;
	}

	/**
	 * ��ȡ��������
	 * @return ���ߵ�����
	 */
	public byte getCurveType() {
		return curveType;
	}

	/**
	 * ȡ�Զ���ͼ��������
	 * 
	 * @return String �Զ���ͼ������
	 */
	public String getCustomClass() {
		return customClass;
	}

	/**
	 * �����Զ���ͼ��������
	 */
	public void setCustomClass(String customClass) {
		this.customClass = customClass;
	}

	/**
	 * ȡ�Զ���ͼ�������
	 * 
	 * @return String �Զ���ͼ�����
	 */
	public String getCustomParam() {
		return customParam;
	}

	/**
	 * �����Զ���ͼ�������
	 */
	public void setCustomParam(String customParam) {
		this.customParam = customParam;
	}

	/**
	 * ȡ��������ɫ
	 * 
	 * @return int RGBֵ��ʾ����ɫ
	 */
	public int getAxisColor() {
		return axisColor;
	}

	/**
	 * ������������ɫ
	 * �÷����Ѿ��������ã�������axisColors
	 * @param color ��������ɫ
	 */
	public void setAxisColor(int color) {
		this.axisColor = color;
	}

	/**
	 * ����ָ����������ɫ
	 * @param index ���
	 * @param c ��ɫ
	 */
	public void setAxisColor(int index, Color c) {
		this.axisColors[index] = color(c);
	}

	private int color(Color c){
		if( c==null ) return 16777215;
		return c.getRGB();
	}
	
	/**
	 * ���ñ߿������
	 * @param borderStyle �߿���
	 * @param borderWidth �߿�ֶ�
	 * @param borderColor �߿���ɫ
	 * @param borderShadow �߿���Ӱ
	 */
	public void setBorder(byte borderStyle, float borderWidth, Color borderColor,
			boolean borderShadow) {
		this.borderStyle = borderStyle;
		this.borderWidth = borderWidth;
		this.borderColor = color(borderColor);
		this.borderShadow = borderShadow;
	}

	/**
	 * ȥ�߿���
	 * @return ���ֵ
	 */
	public byte getBorderStyle() {
		return borderStyle;
	}

	/**
	 * ȡ�߿�ֶ�
	 * @return �ֶ�ֵ
	 */
	public float getBorderWidth() {
		return borderWidth;
	}

	/**
	 * ȡRGBֵ��ʾ����ɫ��Ӧ��ͼ�õ�Color
	 * ��ɫֵΪ16777215ʱ��ʾ͸��ɫ������null
	 * @param c RGB��ʾ����ɫֵ
	 * @return ��ɫ����
	 */
	public static Color getColorObject(int c){
		if(c==16777215) return null;
		return new Color(c);
	}
	
	/**
	 * ȡ�߿���ɫ
	 * @return RGB��ʾ��������ɫ
	 */
	public int getBorderColor() {
		return borderColor;
	}

	/**
	 * �߿��Ƿ������Ӱ
	 * @return ����Ӱ����true�����򷵻�false
	 */
	public boolean getBorderShadow() {
		return borderShadow;
	}

	/**
	 * ȡȫͼ������ɫ
	 * 
	 * @return int��ȫͼ������ɫ
	 */
	public int getCanvasColor() {
		return canvasColor;
	}

	/**
	 * ����ȫͼ������ɫ
	 * 
	 * @param color
	 *            ȫͼ������ɫ
	 */
	public void setCanvasColor(int color) {
		this.canvasColor = color;
	}
	
	/**
	 * ����ȫͼ����ɫ
	 * @param c ��ɫ����
	 */
	public void setCanvasColor(Color c) {
		this.canvasColor = color(c);
	}

	/**
	 * ȡͼ����������ɫ
	 * 
	 * @return int��ͼ����������ɫ
	 */
	public int getGraphBackColor() {
		return this.graphBackColor;
	}

	/**
	 * ����ͼ����������ɫ
	 * 
	 * @param color ͼ����������ɫ
	 */
	public void setGraphBackColor(int color) {
		this.graphBackColor = color;
	}
	/**
	 * ����ͼ����������ɫ
	 * @param c ��ɫ����
	 */
	public void setGraphBackColor(Color c) {
		this.graphBackColor = color(c);
	}

	/**
	 * ȡ�������
	 * 
	 * @return String �������
	 */
	public String getXTitle() {
		return xTitle;
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
	 * ȡ�������
	 * 
	 * @return String �������
	 */
	public String getYTitle() {
		return yTitle;
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
	 * ȡͳ��ͼ����
	 * 
	 * @return String��ͳ��ͼ����
	 */
	public String getGraphTitle() {
		return graphTitle;
	}

	/**
	 * ����ͳ��ͼ����
	 * 
	 * @param title
	 *            ͳ��ͼ����
	 */
	public void setGraphTitle(String title) {
		this.graphTitle = title;
	}

	/**
	 * ȡ����������
	 * 
	 * @return byte�����������ͣ�ֵΪIGraphProperty.LINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *         LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public byte getGridLineType() {
		return gridLineType;
	}

	/**
	 * ��������������
	 * 
	 * @param type
	 *            ����������, ȡֵΪLINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *            LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public void setGridLineType(byte type) {
		this.gridLineType = type;
	}

	/**
	 * ȡ��������ɫ
	 * 
	 * @return int����������ɫ
	 */
	public int getGridLineColor() {
		return gridLineColor;
	}

	/**
	 * ������������ɫ
	 * 
	 * @param color
	 *            ��������ɫ
	 */
	public void setGridLineColor(int color) {
		this.gridLineColor = color;
	}

	/**
	 * ȡ����ͼ������ͼ���
	 * 
	 * @return String������ͼ������ͼ���
	 */
	public int getBarDistance() {
		return barDistance;
	}

	/**
	 * ��������ͼ������ͼ���
	 * 
	 * @param distance
	 *            ����ͼ������ͼ���
	 */
	public void setBarDistance(int distance) {
		this.barDistance = distance;
	}

	/**
	 * ȡͼ�θ�ʽ
	 * 
	 * @return byte��ͼ�θ�ʽ, ֵΪIMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public byte getImageFormat() {
		return imageFormat;
	}

	/**
	 * ����ͼ�θ�ʽ
	 * 
	 * @param format
	 *            ͼ�θ�ʽ��ȡֵΪIMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public void setImageFormat(byte format) {
		this.imageFormat = format;
	}

	/**
	 * ȡͼ���Ƿ�͸��
	 * 
	 * @return boolean
	 */
	public boolean isGraphTransparent() {
		return graphTransparent;
	}

	/**
	 * �Ƿ���ͼ���·��������ݱ�
	 * @return ����ʱ����true�����򷵻�false
	 */
	public boolean isDrawDataTable(){
		return isDrawDataTable;
	}
	
	/**
	 * �����Ƿ�������ݱ�
	 * @param b �Ƿ����
	 */
	public void setDrawDataTable(boolean b){
		this.isDrawDataTable = b;
	}
	/**
	 * ����������ݱ������ڸ����м��Ƿ����
	 * @return ������л��Ʒ���true�����򷵻�false
	 */
	public boolean isDataCenter(){
		return isDataCenter;
	}
	/**
	 * �������ݱ�������Ƿ���л���
	 * ��������еĻ������������
	 * @param b ������ʾ
	 */
	public void setDataCenter(boolean b){
		this.isDataCenter = b;
	}
	/**
	 * ����ͼ���Ƿ�͸��
	 * 
	 * @param b �Ƿ�͸��
	 */
	public void setGraphTransparent(boolean b) {
		this.graphTransparent = b;
	}

	/**
	 * ȡ�Ƿ񽥱�ɫ
	 * 
	 * @return boolean �Ƿ񽥱�ɫ
	 */
	public boolean isGradientColor() {
		return gradientColor;
	}

	/**
	 * �����Ƿ񽥱�ɫ
	 * 
	 * @param b �Ƿ�ʹ�ý���ɫ
	 */
	public void setGradientColor(boolean b) {
		this.gradientColor = b;
	}

	/**
	 * ȡ��ǰN�����ݻ�ͼ
	 * 
	 * @return int����ǰN�����ݻ�ͼ
	 */
	public int getTopData() {
		return topData;
	}

	/**
	 * ������ǰN�����ݻ�ͼ
	 * 
	 * @param n  ��������
	 */
	public void setTopData(int n) {
		this.topData = n;
	}

	/**
	 * ȡʱ��״̬ͼ������ͼ����̱�ͼ��ʼʱ����ʽ
	 * 
	 * @return String��ʱ��״̬ͼ������ͼ����̱�ͼ��ʼʱ����ʽ
	 */
	public String getStatusStartTimeExp() {
		return statusStartTimeExp;
	}

	/**
	 * ����ʱ��״̬ͼ������ͼ����̱�ͼ��ʼʱ����ʽ
	 * 
	 * @param exp ʱ��״̬ͼ������ͼ����̱�ͼ��ʼʱ����ʽ
	 */
	public void setStatusStartTimeExp(String exp) {
		this.statusStartTimeExp = exp;
	}

	/**
	 * ȡʱ��״̬ͼ������ͼ����̱�ͼ����ʱ����ʽ
	 * 
	 * @return String��ʱ��״̬ͼ������ͼ����̱�ͼ����ʱ����ʽ
	 */
	public String getStatusEndTimeExp() {
		return statusEndTimeExp;
	}

	/**
	 * ����ʱ��״̬ͼ������ͼ����̱�ͼ����ʱ����ʽ
	 * 
	 * @param exp ʱ��״̬ͼ������ͼ����̱�ͼ����ʱ����ʽ
	 */
	public void setStatusEndTimeExp(String exp) {
		this.statusEndTimeExp = exp;
	}

	/**
	 * ȡʱ��״̬ͼ������ʽ�����ͼ����̱�ͼ����Ŀ���ʽ
	 * 
	 * @return String��ʱ��״̬ͼ������ʽ�����ͼ����̱�ͼ����Ŀ���ʽ
	 */
	public String getStatusCategoryExp() {
		return statusCategoryExp;
	}

	/**
	 * ����ʱ��״̬ͼ������ʽ�����ͼ����̱�ͼ����Ŀ���ʽ
	 * 
	 * @param exp ʱ��״̬ͼ������ʽ�����ͼ����̱�ͼ����Ŀ���ʽ
	 */
	public void setStatusCategoryExp(String exp) {
		this.statusCategoryExp = exp;
	}

	/**
	 * ȡʱ��״̬ͼ�����ͼ״̬���ʽ
	 * 
	 * @return String��ʱ��״̬ͼ�����ͼ״̬���ʽ
	 */
	public String getStatusStateExp() {
		return statusStateExp;
	}

	/**
	 * ����ʱ��״̬ͼ�����ͼ״̬���ʽ
	 * 
	 * @param exp ʱ��״̬ͼ�����ͼ״̬���ʽ
	 */
	public void setStatusStateExp(String exp) {
		this.statusStateExp = exp;
	}

	/**
	 * ȡʱ��״̬ͼ�����ͼ״̬�����
	 * 
	 * @return String��ʱ��״̬ͼ�����ͼ״̬�����
	 */
	public String getStatusBarWidth() {
		return statusBarWidth;
	}

	/**
	 * ����ʱ��״̬ͼ�����ͼ״̬�����
	 * 
	 * @param width ʱ��״̬ͼ�����ͼ״̬�����
	 */
	public void setStatusBarWidth(String width) {
		this.statusBarWidth = width;
	}

	/**
	 * ȡʱ��״̬ͼ�����ͼʱ��̶�����
	 * 
	 * @return byte��ʱ��״̬ͼ�����ͼʱ��̶����ͣ�ֵΪTIME_YEAR, TIME_MONTH, TIME_DAY,
	 *         TIME_HOUR, TIME_MINUTE, TIME_SECOND
	 */
	public byte getStatusTimeType() {
		return statusTimeType;
	}
	public String getStatusTimeFormat() {
		return statusTimeFormat;
	}

	/**
	 * ����ʱ��״̬ͼ�����ͼʱ��̶�����
	 * 
	 * @param type
	 *            ʱ��״̬ͼ�����ͼʱ��̶����ͣ�ȡֵΪTIME_YEAR, TIME_MONTH, TIME_DAY, TIME_HOUR,
	 *            TIME_MINUTE, TIME_SECOND
	 */
	public void setStatusTimeType(byte type) {
		this.statusTimeType = type;
	}
	public void setStatusTimeFormat(String fmt) {
		this.statusTimeFormat = fmt;
	}

	/**
	 * ȡͳ��ͼ����
	 * 
	 * @return GraphFonts��ͳ��ͼ����
	 */
	public GraphFonts getFonts() {
		return fonts;
	}

	/**
	 * ����ͳ��ͼ����
	 * 
	 * @param fonts ͳ��ͼ����
	 */
	public void setFonts(GraphFonts fonts) {
		this.fonts = fonts;
	}

	/**
	 * ȡ�����߶���
	 * 
	 * @return AlarmLine[]�������߶���
	 */
	public AlarmLine[] getAlarmLines() {
		return alarms;
	}

	/**
	 * ���þ����߶���
	 * 
	 * @param alarms
	 *            �����߶���
	 */
	public void setAlarmLines(AlarmLine[] alarms) {
		this.alarms = alarms;
	}

	/**
	 * ȡͼ����ʾ���ݶ���
	 * 
	 * @return byte��ͼ����ʾ���ݶ��壬ֵΪDISPDATA_NONE, DISPDATA_VALUE,
	 *         DISPDATA_PERCENTAGE
	 */
	public byte getDisplayData() {
		return displayData;
	}
	public byte getDisplayData2() {
		return displayData2;
	}

	/**
	 * ����ͼ����ʾ���ݶ���
	 * 
	 * @param displayData
	 *            ͼ����ʾ���ݶ��壬ȡֵΪDISPDATA_NONE, DISPDATA_VALUE, DISPDATA_PERCENTAGE
	 */
	public void setDisplayData(byte displayData) {
		this.displayData = displayData;
	}
	public void setDisplayData2(byte displayData) {
		this.displayData2 = displayData;
	}

	/**
	 * ȡͼ����ʾ���ݸ�ʽ����
	 * 
	 * @return String��ͼ����ʾ���ݸ�ʽ����
	 */
	public String getDisplayDataFormat() {
		return displayDataFormat;
	}

	/**
	 * ����ͼ����ʾ���ݸ�ʽ����
	 * 
	 * @param format
	 *            ͼ����ʾ���ݸ�ʽ���壬˫��ͼʱ�÷ֺŸ���
	 */
	public void setDisplayDataFormat(String format) {
		this.displayDataFormat = format;
	}

	/**
	 * ȡͳ��ͼ������
	 * 
	 * @return String��ͳ��ͼ������
	 */
	public String getLink() {
		return link;
	}

	/**
	 * ȡͳ��ͼ�е�ͼ��������
	 * @return String ͼ��������
	 */
	public String getLegendLink() {
		return legendLink;
	}

	/**
	 * ����ͳ��ͼ������
	 * 
	 * @param link ͳ��ͼ������
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * ����ͼ��������
	 * @param link ͼ��������
	 */
	public void setLegendLink(String link) {
		this.legendLink = link;
	}

	/**
	 * ȡͳ��ͼ������Ŀ�괰��
	 * 
	 * @return String��ͳ��ͼ������Ŀ�괰��
	 */
	public String getLinkTarget() {
		return linkTarget;
	}

	/**
	 * ����ͳ��ͼ������Ŀ�괰��
	 * 
	 * @param target ͳ��ͼ������Ŀ�괰��
	 */
	public void setLinkTarget(String target) {
		this.linkTarget = target;
	}

	/**
	 * ȡͳ��ͼ��ͼ��λ��
	 * 
	 * @return byte��ͳ��ͼ��ͼ��λ�ã�ֵΪLEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *         LEGEND_BOTTOM, LEGEND_NONE
	 */
	public byte getLegendLocation() {
		return legendLocation;
	}

	/**
	 * ����ͳ��ͼ��ͼ��λ��
	 * 
	 * @param location
	 *            ͳ��ͼ��ͼ��λ��, ȡֵΪLEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *            LEGEND_BOTTOM, LEGEND_NONE
	 */
	public void setLegendLocation(byte location) {
		this.legendLocation = location;
	}

	/**
	 * ����ͼ�����ֵ������϶
	 * @param gap ��϶ֵ
	 */
	public void setLegendVerticalGap(int gap){
		this.legendVerticalGap = gap;
	}
	/**
	 * ȡͼ���������϶ֵ
	 * @return int ��϶ֵ
	 */
	public int getLegendVerticalGap(){
		return legendVerticalGap;
	}
	
	/**
	 * ����ͼ���ĺ����϶ֵ
	 * @param gap ��϶ֵ
	 */
	public void setLegendHorizonGap(int gap){
		this.legendHorizonGap = gap;
	}
	
	/**
	 * ȥͼ�������϶ֵ
	 * @return ��϶ֵ
	 */
	public int getLegendHorizonGap(){
		return legendHorizonGap;
	}

	/**
	 * ͼ���Ƿ���ϵ�л���
	 * @return ��ϵ�л���ʱ����true�����򷵻�false
	 */
	public boolean getDrawLegendBySery() {
		return drawLegendBySery;
	}

	/**
	 * �����Ƿ���ϵ��ֵ����ͼ��
	 * @param b �Ƿ�ϵ�л�ͼ��
	 */
	public void setDrawLegendBySery(boolean b) {
		this.drawLegendBySery = b;
	}

	/**
	 * ȡͳ��ͼ����ɫ������
	 * 
	 * @return String��ͳ��ͼ����ɫ������
	 */
	public String getColorConfig() {
		return colorConfig;
	}

	/**
	 * ����ͳ��ͼ����ɫ������
	 * 
	 * @param config
	 *            ͳ��ͼ����ɫ������
	 */
	public void setColorConfig(String config) {
		this.colorConfig = config;
	}

	/**
	 * ȡͳ��ֵ��ʼֵ
	 * 
	 * @return String��ͳ��ֵ��ʼֵ
	 */
	public String getYStartValue() {
		return yStartValue;
	}

	/**
	 * ����ͳ��ֵ��ʼֵ
	 * 
	 * @param value
	 *            ͳ��ֵ��ʼֵ, ˫��ͼʱ�÷ֺŸ���
	 */
	public void setYStartValue(String value) {
		this.yStartValue = value;
	}

	/**
	 * ȡͳ��ֵ����ֵ
	 * 
	 * @return String��ͳ��ֵ����ֵ
	 */
	public String getYEndValue() {
		return yEndValue;
	}

	/**
	 * ����ͳ��ֵ����ֵ
	 * 
	 * @param value
	 *            ͳ��ֵ����ֵ, ˫��ͼʱ�÷ֺŸ���
	 */
	public void setYEndValue(String value) {
		this.yEndValue = value;
	}

	/**
	 * ȡͳ��ֵ��ǩ���
	 * 
	 * @return String��ͳ��ֵ��ǩ���
	 */
	public String getYInterval() {
		return yInterval;
	}

	/**
	 * ����ͳ��ֵ��ǩ���
	 * 
	 * @param interval
	 *            ͳ��ֵ��ǩ�����˫��ͼʱ�÷ֺŸ���
	 */
	public void setYInterval(String interval) {
		this.yInterval = interval;
	}

	/**
	 * ȡͳ��ֵ������λ
	 * 
	 * @return double��ͳ��ֵ������λ��ֵΪUNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *         UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION, UNIT_100MILLION,
	 *         UNIT_BILLION, UNIT_001, UNIT_0001, UNIT_00001, UNIT_0000001
	 */
	public double getDataUnit() {
		return dataUnit;
	}

	/**
	 * ����ͳ��ֵ������λ
	 * 
	 * @param unit
	 *            ͳ��ֵ������λ��ȡֵΪUNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *            UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION,
	 *            UNIT_100MILLION, UNIT_BILLION, UNIT_001, UNIT_0001,
	 *            UNIT_00001, UNIT_0000001
	 */
	public void setDataUnit(double unit) {
		this.dataUnit = unit;
	}

	/**
	 * ȡͳ��ֵ���ٿ̶���
	 * 
	 * @return String��ͳ��ֵ���ٿ̶���
	 */
	public int getYMinMarks() {
		return yMinMarks;
	}

	/**
	 * ����ͳ��ֵ���ٿ̶���
	 * 
	 * @param marks
	 *            ͳ��ֵ���ٿ̶���
	 */
	public void setYMinMarks(int marks) {
		this.yMinMarks = marks;
	}

	/**
	 * ȡ������ͼ��֮��ļ��
	 * 
	 * @return String��������ͼ��֮��ļ��
	 */
	public int getTitleMargin() {
		return titleMargin;
	}

	/**
	 * ���ñ�����ͼ��֮��ļ��
	 * 
	 * @param margin
	 *            ������ͼ��֮��ļ��
	 */
	public void setTitleMargin(int margin) {
		this.titleMargin = margin;
	}

	/**
	 * ȡ����ͼ�Ƿ��ע���ݵ�
	 * 
	 * @return boolean �������ݵ�ʱ����true������false
	 */
	public boolean isDrawLineDot() {
		return drawLineDot;
	}

	/**
	 * ����ͼ�Ƿ�������
	 * 
	 * @return boolean ����������ʱ����true�����򷵻�false
	 */
	public boolean isDrawLineTrend() {
		return drawLineTrend;
	}

	/**
	 * ����ͼ�Ĵ�ϸ��
	 * 
	 * @return byte �ֶ�
	 */
	public byte getLineThick() {
		return lineThick;
	}

	/**
	 * ȡ���ߵ����ͷ��
	 * @return ���
	 */
	public byte getLineStyle() {
		return lineStyle;
	}

	/**
	 * ��������ͼ�Ƿ��ע���ݵ�
	 * 
	 * @param b �Ƿ��ע
	 */
	public void setDrawLineDot(boolean b) {
		this.drawLineDot = b;
	}

	/**
	 * ����ԭ���غ�
	 * @param b �Ƿ��غ�
	 */
	public void setOverlapOrigin(boolean b) {
		this.isOverlapOrigin = b;
	}
	
	/**
	 * ȡԭ���Ƿ��غ�
	 * @return �غ�ʱ����true�����򷵻�false
	 */
	public boolean isOverlapOrigin() {
		return isOverlapOrigin;
	}
	
	/**
	 * ��������ͼ�Ƿ�������
	 * 
	 * @param b �Ƿ�������
	 */
	public void setDrawLineTrend(boolean b) {
		this.drawLineTrend = b;
	}

	/**
	 * ��������ͼ��ϸ��
	 * 
	 * @byte thick �ֶ�
	 */
	public void setLineThick(byte thick) {
		this.lineThick = thick;
	}

	/**
	 * ��������ͼ������
	 * @param style  ����
	 */
	public void setLineStyle(byte style) {
		this.lineStyle = style;
	}

	/**
	 * ȡ������ֵ���ǩ�ص�ʱ�Ƿ���ʾ��һ��ֵ���ǩ
	 * 
	 * @return boolean �����ı��ص�ʱ����true�����򷵻�false
	 */
	public boolean isShowOverlapText() {
		return showOverlapText;
	}

	/**
	 * ����������ֵ���ǩ�ص�ʱ�Ƿ���ʾ��һ��ֵ���ǩ
	 * 
	 * @param b �Ƿ������ı��ص�����
	 */
	public void setShowOverlapText(boolean b) {
		this.showOverlapText = b;
	}

	/**
	 * ȡ��ͼ���Ƿ�����һ����ʾ
	 * 
	 * @return boolean �Ƿ����
	 */
	public boolean isPieSpacing() {
		return pieSpacing;
	}

	/**
	 * �Ǳ�����ɫ�Ƿ񽫿̶Ȼ��Ƶ���ɫĩ��
	 * ���綨��20��Ϊ��ɫ��ʾ���ʣ�30��Ϊ��ɫ��ʾ����
	 * ����ĩ�˻�����Ϊ����0��20����ɫ��20��30����ɫ
	 * ����ĩ��ʱָ�̶�λ����ɫ�м䣬��ʱ15��25����ɫ����20λ����ɫ�м䣻��25��30�Ż���ɫ��
	 */
	public boolean isMeterColorEnd() {
		return isMeterColorEnd;
	}
	/**
	 * �Ƿ�����Ǳ��̶̿ȱ�ǩ
	 * �����̶�ʱ���������ı�ǩ
	 */
	public boolean isMeterTick() {
		return isMeterTick;
	}
	/**
	 * �����Ƿ�����Ǳ��̶̿ȱ�ǩ
	 * @param b �Ƿ����
	 */
	public void setMeterTick(boolean b){
		isMeterTick = b;
	}
	
	/**
	 * ȡ�Ǳ���3d�߿���
	 */
	public int getMeter3DEdge() {
		return meter3DEdge;
	}
	
	/**
	 * ȡ�Ǳ��̲ʺ�ߺ��
	 */
	public int getMeterRainbowEdge() {
		return meterRainbowEdge;
	}
	/**
	 * ȡ��ͼ��������
	 */
	public int getPieLine() {
		return pieLine;
	}
	
	/**
	 * �ѻ�ͼ��ʱ���Ƿ���ʾ����ֵ
	 */
	public boolean isDispStackSumValue(){
		return isDispStackSumValue;
	}
	/**
	 * ���öѻ�ͼʱ�Ƿ���ƻ���ֵ
	 * @param b �Ƿ����
	 */
	public void setDispStackSumValue(boolean b){
		isDispStackSumValue = b;
	}
	 
	/**
	 * ���ñ�ͼ���Ƿ�����һ����ʾ
	 * 
	 * @param b
	 *            ��ͼ���Ƿ�����һ����ʾ
	 */
	public void setPieSpacing(boolean b) {
		this.pieSpacing = b;
	}

	/**
	 * ȡ�������ǩ���
	 * 
	 * @return String���������ǩ���
	 */
	public int getXInterval() {
		return xInterval;
	}

	/**
	 * ���÷������ǩ���
	 * 
	 * @param interval
	 *            �������ǩ���
	 */
	public void setXInterval(int interval) {
		this.xInterval = interval;
	}

	/**
	 * ����ͼ�Ƿ���Կ�ֵ
	 * 
	 * @return boolean
	 */
	public boolean ignoreNull() {
		return ignoreNull;
	}

	/**
	 * ��������ͼ�Ƿ���Կ�ֵ
	 * 
	 * @param b
	 */
	public void setIgnoreNull(boolean b) {
		this.ignoreNull = b;
	}

	/**
	 * �Ƿ���Ӱ
	 */
	public boolean isDrawShade() {
		return drawShade;
	}

	/**
	 * �����Ƿ���Ӱ
	 * @param isDrawShade �Ƿ���Ӱ
	 */
	public void setDrawShade(boolean isDrawShade) {
		drawShade = isDrawShade;
	}

	/**
	 * ��ͼ��ʽ���Ƿ�͹���߿�
	 */
	public boolean isRaisedBorder() {
		return raisedBorder;
	}

	/**
	 * ������ͼ�Ƿ����͹���߿�
	 * @param isRaisedBorder �Ƿ�͹���߿�
	 */
	public void setRaisedBorder(boolean isRaisedBorder) {
		raisedBorder = isRaisedBorder;
	}

	/**
	 * ����������ı�־λ�������ȡʱ������
	 * �ο�getFlag(prop)
	 * @return
	 */
	public long getFlag() {
		return flag;
	}

	/**
	 * ���ñ�־λ
	 * @param newFlag ��־λ
	 */
	public void setFlag(long newFlag) {
		flag = newFlag;
	}

	/**
	 * ��ȡ��־λ���ԣ�������
	 * Ŀǰֻʵ��FLAG_DISCARDOTHER
	 */
	public boolean getFlag(byte prop) {
		return (this.flag & (0x01 << prop)) != 0;
	}

	/**
	 * ���ñ�־λ�Ĳ���ֵ
	 * @param prop ��־λ
	 * @param isOn ����״̬
	 */
	public void setFlag(byte prop, boolean isOn) {
		if (isOn) {
			this.flag |= 0x01 << prop;
		} else {
			this.flag &= ~(0x01 << prop);
		}
	}

	/**
	 * ȡ����ͼ����
	 * @return ����ͼ����
	 */
	public BackGraphConfig getBackGraphConfig() {
		return backGraph;
	}

	/**
	 * ���ñ���ͼ����
	 * @param backGraphConfig ����ͼ����
	 */
	public void setBackGraphConfig(BackGraphConfig backGraphConfig) {
		backGraph = backGraphConfig;
	}

	/**
	 * ȡͼ���ı��Լ�ͼ������ɫ����
	 */
	public int[] getAxisColors() {
		return axisColors;
	}

	/**
	 * ����ͼ���ı����Լ�ͼ������ɫ����
	 */
	public void setAxisColors(int[] colors) {
		axisColors = colors;
	}

	/**
	 * ȡͼ����߾�
	 */
	public int getLeftMargin() {
		return leftMargin;
	}

	/**
	 * ����ͼ�ε���߾�
	 * @param leftMargin
	 */
	public void setLeftMargin(int leftMargin) {
		this.leftMargin = leftMargin;
	}

	/**
	 * ȡͼ�ε��ұ߾�
	 */
	public int getRightMargin() {
		return rightMargin;
	}

	/**
	 * ����ͼ�ε��ұ߾�
	 * @param rightMargin �ұ߾�
	 */
	public void setRightMargin(int rightMargin) {
		this.rightMargin = rightMargin;
	}

	/**
	 * ȡͼ�εĶ��߾�
	 */
	public int getTopMargin() {
		return topMargin;
	}

	/**
	 * ����ͼ�εĶ��߾�
	 * @param topMargin ���߾�
	 */
	public void setTopMargin(int topMargin) {
		this.topMargin = topMargin;
	}

	/**
	 * ȡͼ�εĵױ߾�
	 */
	public int getBottomMargin() {
		return bottomMargin;
	}

	/**
	 * ����ͼ�εĵױ߾�
	 * @param bottomMargin �ױ߾�
	 */
	public void setBottomMargin(int bottomMargin) {
		this.bottomMargin = bottomMargin;
	}

	/**
	 * ȡ�̶ȵĻ��Ƴ���
	 */
	public int getTickLen() {
		return tickLen;
	}

	/**
	 * ���ÿ̶ȵĻ��Ƴ���
	 * @param tickLen ����
	 */
	public void setTickLen(int tickLen) {
		this.tickLen = tickLen;
	}

	/**
	 * ȡ����ϵ���
	 */
	public int getCoorWidth() {
		return coorWidth;
	}

	/**
	 * ��ȡ����ϵ���
	 * @param coorWidth ���ֵ
	 */
	public void setCoorWidth(int coorWidth) {
		this.coorWidth = coorWidth;
	}

	/**
	 * ȡͼ�η���ļ�϶ֵ
	 */
	public double getCategorySpan() {
		return categorySpan;
	}

	/**
	 * ����ͼ�η���ֵ֮��ļ�϶
	 * @param categorySpan ��϶ֵ
	 */
	public void setCategorySpan(double categorySpan) {
		this.categorySpan = categorySpan;
	}

	/**
	 * ȡ����ϵ��֮��ļ�϶ֵ
	 */
	public int getSeriesSpan() {
		return seriesSpan;
	}

	/**
	 * ����ϵ��֮��ļ�϶ֵ
	 * @param seriesSpan ��϶ֵ
	 */
	public void setSeriesSpan(int seriesSpan) {
		this.seriesSpan = seriesSpan;
	}

	/**
	 * ȡ��ͼ����ת�Ƕ�
	 */
	public int getPieRotation() {
		return pieRotation;
	}

	/**
	 * ���ñ�ͼ����ת�Ƕ�
	 * @param pieRotation �Ƕ�
	 */
	public void setPieRotation(int pieRotation) {
		this.pieRotation = pieRotation;
	}

	/**
	 * ȡ�����ͼ�ĸ߶�
	 */
	public int getPieHeight() {
		return pieHeight;
	}

	/**
	 * ���������ͼ�ĸ߶�
	 * @param pieHeight �߶�ֵ
	 */
	public void setPieHeight(int pieHeight) {
		this.pieHeight = pieHeight;
	}

	/**
	 * ���ù�������
	 * @param pp ��������
	 */
	public void setPublicProperty(PublicProperty pp){
		type=pp.getType();
		axisColor = pp.getAxisColor();
		canvasColor = pp.getCanvasColor();
		graphBackColor = pp.getGraphBackColor();
		xTitle = pp.getXTitle();
		yTitle = pp.getYTitle();
		graphTitle = pp.getGraphTitle();
		gridLineType = pp.getGridLineType();
		gridLineColor = pp.getGridLineColor();
		barDistance = pp.getBarDistance();
		imageFormat = pp.getImageFormat();
		graphTransparent = pp.isGraphTransparent();
		isDrawDataTable = pp.isDrawDataTable();
		isDataCenter = pp.isDataCenter();
		
		gradientColor = pp.isGradientColor();
		topData = pp.getTopData();
		statusStartTimeExp = pp.getStatusStartTimeExp();
		statusEndTimeExp = pp.getStatusEndTimeExp();
		statusCategoryExp = pp.getStatusCategoryExp();
		statusStateExp = pp.getStatusStateExp();
		statusBarWidth = pp.getStatusBarWidth();
		statusTimeType = pp.getStatusTimeType();
		statusTimeFormat=pp.getStatusTimeFormat();
		fonts = (GraphFonts)pp.getFonts().deepClone();
		if (pp.getAlarmLines() != null) {
			AlarmLine[] aline = new AlarmLine[pp.getAlarmLines().length];
			for (int i = 0; i < aline.length; i++) {
				aline[i] = (AlarmLine) pp.getAlarmLines()[i].deepClone();
			}
			alarms = aline;
		}
		displayData = pp.getDisplayData();
		displayData2 = pp.getDisplayData2();
		displayDataFormat = pp.getDisplayDataFormat();
		link = pp.getLink();
		linkTarget = pp.getLinkTarget();
		legendLocation = pp.getLegendLocation();
		legendVerticalGap = pp.getLegendVerticalGap();
		legendHorizonGap = pp.getLegendHorizonGap();
		drawLegendBySery = pp.getDrawLegendBySery();
		colorConfig = pp.getColorConfig();
		yStartValue = pp.getYStartValue();
		yEndValue = pp.getYEndValue();
		yInterval = pp.getYInterval();
		dataUnit = pp.getDataUnit();
		yMinMarks = pp.getYMinMarks();
		titleMargin = pp.getTitleMargin();
		drawLineDot = pp.isDrawLineDot();
		drawLineTrend = pp.isDrawLineTrend();
		lineThick = pp.getLineThick();
		showOverlapText = pp.isShowOverlapText();
		xInterval = pp.getXInterval();
		pieSpacing = pp.isPieSpacing();
		ignoreNull = pp.ignoreNull();
		customClass = pp.getCustomClass();
		customParam = pp.getCustomParam();
		drawShade = pp.isDrawShade();
		raisedBorder = pp.isRaisedBorder();
		backGraph = pp.getBackGraphConfig();
		axisColors = pp.getAxisColors();
		flag = pp.getFlag();
		leftMargin = pp.getLeftMargin();
		rightMargin = pp.getRightMargin();
		topMargin = pp.getTopMargin();
		bottomMargin = pp.getBottomMargin();
		tickLen = pp.getTickLen();
		coorWidth = pp.getCoorWidth();
		categorySpan = pp.getCategorySpan();
		seriesSpan = pp.getSeriesSpan();
		pieRotation = pp.getPieRotation();
		pieHeight = pp.getPieHeight();
		legendLink = pp.getLegendLink();
		curveType = pp.getCurveType();
		lineStyle = pp.getLineStyle();
		setBorder(pp.getBorderStyle(), pp.getBorderWidth(),
				getColorObject(pp.getBorderColor()), pp.getBorderShadow());
		isDispStackSumValue = pp.isDispStackSumValue();
		otherStackedSeries = pp.getOtherStackedSeries();
		isOverlapOrigin = pp.isOverlapOrigin();
	}
	
	/**
	 * ��ȿ�¡
	 * 
	 * @return Object ��¡��ͼ������
	 */
	public Object deepClone() {
		GraphProperty gp = new GraphProperty();
		gp.setPublicProperty(this);
		return gp;
	}

	/**
	 * ���汾���л�
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(10);
		out.writeByte(type);
		out.writeInt(axisColor);
		out.writeInt(canvasColor);
		out.writeInt(graphBackColor);
		out.writeObject(xTitle);
		out.writeObject(yTitle);
		out.writeObject(graphTitle);
		out.writeByte(gridLineType);
		out.writeInt(gridLineColor);
		out.writeInt(barDistance);
		out.writeByte(imageFormat);
		out.writeBoolean(graphTransparent);
		out.writeBoolean(gradientColor);
		out.writeInt(topData);
		out.writeObject(statusStartTimeExp);
		out.writeObject(statusEndTimeExp);
		out.writeObject(statusCategoryExp);
		out.writeObject(statusStateExp);
		out.writeObject(statusBarWidth);
		out.writeByte(statusTimeType);
		out.writeObject(fonts);
		out.writeObject(alarms);
		out.writeByte(displayData);
		out.writeObject(displayDataFormat);
		out.writeObject(link);
		out.writeObject(linkTarget);
		out.writeByte(legendLocation);
		out.writeBoolean(drawLegendBySery);
		out.writeObject(colorConfig);
		out.writeObject(yStartValue);
		out.writeObject(yEndValue);
		out.writeObject(yInterval);
		out.writeDouble(dataUnit);
		out.writeInt(yMinMarks);
		out.writeInt(titleMargin);
		out.writeBoolean(drawLineDot);
		out.writeBoolean(showOverlapText);
		out.writeBoolean(pieSpacing);
		out.writeInt(xInterval);
		out.writeByte(lineThick);
		out.writeBoolean(drawLineTrend);
		out.writeBoolean(ignoreNull);
		out.writeObject(customClass);
		out.writeObject(customParam);
		out.writeBoolean(drawShade);
		out.writeBoolean(raisedBorder);
		out.writeObject(backGraph);
		out.writeObject(axisColors);
		out.writeLong(flag);
		out.writeInt(leftMargin);
		out.writeInt(rightMargin);
		out.writeInt(topMargin);
		out.writeInt(bottomMargin);
		out.writeInt(tickLen);
		out.writeInt(coorWidth);
		out.writeDouble(categorySpan);
		out.writeInt(seriesSpan);
		out.writeInt(pieRotation);
		out.writeInt(pieHeight);
		out.writeObject(legendLink);
		out.writeByte(curveType);
		out.writeByte(lineStyle);
		out.writeByte(borderStyle);
		out.writeFloat(borderWidth);
		out.writeInt(borderColor);
		out.writeBoolean(borderShadow);
		out.writeBoolean(isDispStackSumValue);
		out.writeBoolean(isDrawDataTable);
		out.writeObject(otherStackedSeries);
		out.writeBoolean(isOverlapOrigin);
		out.writeInt(legendVerticalGap);
		out.writeInt(legendHorizonGap);
		out.writeBoolean(isDataCenter);
		out.writeByte(displayData2);
		out.writeObject(statusTimeFormat);
	}

	/**
	 * ���汾�����л�
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		byte ver = in.readByte();
		type = in.readByte();
		canvasColor = in.readInt();
		graphBackColor = in.readInt();
		xTitle = (String) in.readObject();
		yTitle = (String) in.readObject();
		graphTitle = (String) in.readObject();
		gridLineType = in.readByte();
		gridLineColor = in.readInt();
		barDistance = in.readInt();
		imageFormat = in.readByte();
		graphTransparent = in.readBoolean();
		gradientColor = in.readBoolean();
		topData = in.readInt();
		statusStartTimeExp = (String) in.readObject();
		statusEndTimeExp = (String) in.readObject();
		statusCategoryExp = (String) in.readObject();
		statusStateExp = (String) in.readObject();
		statusBarWidth = (String) in.readObject();
		statusTimeType = in.readByte();
		fonts = (GraphFonts) in.readObject();
		alarms = (AlarmLine[]) in.readObject();
		displayData = in.readByte();
		displayDataFormat = (String) in.readObject();
		link = (String) in.readObject();
		linkTarget = (String) in.readObject();
		legendLocation = in.readByte();
		drawLegendBySery = in.readBoolean();
		colorConfig = (String) in.readObject();
		yStartValue = (String) in.readObject();
		yEndValue = (String) in.readObject();
		yInterval = (String) in.readObject();
		dataUnit = in.readDouble();
		yMinMarks = in.readInt();
		titleMargin = in.readInt();
		drawLineDot = in.readBoolean();
		showOverlapText = in.readBoolean();
		pieSpacing = in.readBoolean();
		xInterval = in.readInt();
		lineThick = in.readByte();
		drawLineTrend = in.readBoolean();
		ignoreNull = in.readBoolean();
		customClass = (String) in.readObject();
		customParam = (String) in.readObject();
		drawShade = in.readBoolean();
		raisedBorder = in.readBoolean();
		backGraph = (BackGraphConfig) in.readObject();
		axisColors = (int[]) in.readObject();
		flag = in.readLong();
		leftMargin = in.readInt();
		rightMargin = in.readInt();
		topMargin = in.readInt();
		bottomMargin = in.readInt();
		tickLen = in.readInt();
		coorWidth = in.readInt();
		categorySpan = in.readDouble();
		seriesSpan = in.readInt();
		pieRotation = in.readInt();
		pieHeight = in.readInt();
		legendLink = (String) in.readObject();
		curveType = in.readByte();
		lineStyle = in.readByte();
		if (ver > 1) {
			borderStyle = in.readByte();
			borderWidth = in.readFloat();
			borderColor = in.readInt();
			borderShadow = in.readBoolean();
		}
		if(ver>2){
			isDispStackSumValue = in.readBoolean();
		}
		if(ver>3){
			isDrawDataTable = in.readBoolean();
		}
		if(ver>4){
			otherStackedSeries = (String)in.readObject();
		}
		if(ver>5){
			isOverlapOrigin = in.readBoolean();
		}
		if(ver>6){
			legendVerticalGap = in.readInt();
			legendHorizonGap = in.readInt();
		}
		if(ver>7){
			isDataCenter = in.readBoolean();
		}
		if(ver>8){
			displayData2 = in.readByte();
		}
		if(ver>9){
			statusTimeFormat = (String)in.readObject();
		}
		
	}

	/**
	 * ʵ��IRecord�ӿ�
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeByte(type);
		out.writeInt(axisColor);
		out.writeInt(canvasColor);
		out.writeInt(graphBackColor);
		out.writeString(xTitle);
		out.writeString(yTitle);
		out.writeString(graphTitle);
		out.writeByte(gridLineType);
		out.writeInt(gridLineColor);
		out.writeInt(barDistance);
		out.writeByte(imageFormat);
		out.writeBoolean(graphTransparent);
		out.writeBoolean(gradientColor);
		out.writeInt(topData);
		out.writeString(statusStartTimeExp);
		out.writeString(statusEndTimeExp);
		out.writeString(statusCategoryExp);
		out.writeString(statusStateExp);
		out.writeString(statusBarWidth);
		out.writeByte(statusTimeType);
		out.writeRecord(fonts);
		if (alarms == null) {
			out.writeShort((short) 0);
		} else {
			int size = alarms.length;
			out.writeShort((short) size);
			for (int i = 0; i < size; i++) {
				out.writeRecord(alarms[i]);
			}
		}
		out.writeByte(displayData);
		out.writeString(displayDataFormat);
		out.writeString(link);
		out.writeString(linkTarget);
		out.writeByte(legendLocation);
		out.writeBoolean(drawLegendBySery);
		out.writeString(colorConfig);
		out.writeString(yStartValue);
		out.writeString(yEndValue);
		out.writeString(yInterval);
		out.writeDouble(dataUnit);
		out.writeInt(yMinMarks);
		out.writeInt(titleMargin);
		out.writeBoolean(drawLineDot);
		out.writeBoolean(showOverlapText);
		out.writeBoolean(pieSpacing);
		out.writeInt(xInterval);
		out.writeByte(lineThick);
		out.writeBoolean(drawLineTrend);
		out.writeBoolean(ignoreNull);
		out.writeString(customClass);
		out.writeString(customParam);
		out.writeBoolean(drawShade);
		out.writeBoolean(raisedBorder);
		out.writeRecord(backGraph);

		if (axisColors == null) {
			out.writeShort((short) 0);
		} else {
			int size = axisColors.length;
			out.writeShort((short) size);
			for (int i = 0; i < size; i++) {
				out.writeInt(axisColors[i]);
			}
		}
		out.writeLong(flag);
		out.writeInt(leftMargin);
		out.writeInt(rightMargin);
		out.writeInt(topMargin);
		out.writeInt(bottomMargin);
		out.writeInt(tickLen);
		out.writeInt(coorWidth);
		out.writeDouble(categorySpan);
		out.writeInt(seriesSpan);
		out.writeInt(pieRotation);
		out.writeInt(pieHeight);
		out.writeString(legendLink);
		out.writeByte(curveType);
		out.writeByte(lineStyle);
		out.writeByte(borderStyle);
		out.writeFloat(borderWidth);
		out.writeInt(borderColor);
		out.writeBoolean(borderShadow);
		out.writeBoolean(isDispStackSumValue);
		out.writeBoolean(isDrawDataTable);
		out.writeString(otherStackedSeries);
		
		out.writeBoolean(isOverlapOrigin);
		out.writeInt(legendVerticalGap);
		out.writeInt(legendHorizonGap);
		out.writeBoolean(isDataCenter);
		out.writeByte(displayData2);
		out.writeString(statusTimeFormat);
		return out.toByteArray();
	}

	/**
	 * ʵ��IRecord�ӿ�
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		type = in.readByte();
		axisColor = in.readInt();
		canvasColor = in.readInt();
		graphBackColor = in.readInt();
		xTitle = in.readString();
		yTitle = in.readString();
		graphTitle = in.readString();
		gridLineType = in.readByte();
		gridLineColor = in.readInt();
		barDistance = in.readInt();
		imageFormat = in.readByte();
		graphTransparent = in.readBoolean();
		gradientColor = in.readBoolean();
		topData = in.readInt();
		statusStartTimeExp = in.readString();
		statusEndTimeExp = in.readString();
		statusCategoryExp = in.readString();
		statusStateExp = in.readString();
		statusBarWidth = in.readString();
		statusTimeType = in.readByte();
		fonts = (GraphFonts) in.readRecord(new GraphFonts());
		short an = in.readShort();
		if (an > 0) {
			alarms = new AlarmLine[an];
			for (int i = 0; i < an; i++) {
				alarms[i] = (AlarmLine) in.readRecord(new AlarmLine());
			}
		}
		displayData = in.readByte();
		displayDataFormat = in.readString();
		link = in.readString();
		linkTarget = in.readString();
		legendLocation = in.readByte();
		drawLegendBySery = in.readBoolean();
		colorConfig = in.readString();
		yStartValue = in.readString();
		yEndValue = in.readString();
		yInterval = in.readString();
		dataUnit = in.readDouble();
		yMinMarks = in.readInt();
		titleMargin = in.readInt();
		drawLineDot = in.readBoolean();
		showOverlapText = in.readBoolean();
		pieSpacing = in.readBoolean();
		xInterval = in.readInt();
		lineThick = in.readByte();
		drawLineTrend = in.readBoolean();
		ignoreNull = in.readBoolean();
		customClass = in.readString();
		customParam = in.readString();
		drawShade = in.readBoolean();
		raisedBorder = in.readBoolean();
		backGraph = (BackGraphConfig) in.readRecord(new BackGraphConfig());
		an = in.readShort();
		if (an > 0) {
			axisColors = new int[an];
			for (int i = 0; i < an; i++) {
				axisColors[i] = in.readInt();
			}
		}
		flag = in.readLong();
		leftMargin = in.readInt();
		rightMargin = in.readInt();
		topMargin = in.readInt();
		bottomMargin = in.readInt();
		tickLen = in.readInt();
		coorWidth = in.readInt();
		categorySpan = in.readDouble();
		seriesSpan = in.readInt();
		pieRotation = in.readInt();
		pieHeight = in.readInt();
		legendLink = in.readString();
		curveType = in.readByte();
		lineStyle = in.readByte();
		if (in.available() > 0) {
			borderStyle = in.readByte();
			borderWidth = in.readFloat();
			borderColor = in.readInt();
			borderShadow = in.readBoolean();
		}
		if(in.available()>0){
			isDispStackSumValue = in.readBoolean();
		}
		if(in.available()>0){
			isDrawDataTable = in.readBoolean();
		}
		if(in.available()>0){
			otherStackedSeries = in.readString();
		}
		if(in.available()>0){
			isOverlapOrigin = in.readBoolean();
		}
		if(in.available()>0){
			legendVerticalGap = in.readInt();
			legendHorizonGap = in.readInt();
		}
		if(in.available()>0){
			isDataCenter = in.readBoolean();
		}
		if(in.available()>0){
			displayData2 = in.readByte();
		}
		if(in.available()>0){
			statusTimeFormat = in.readString();
		}
	}

	/**
	 * ���������ѻ�ϵ��
	 * ���繲��������ѧӢ���7�ſΣ�����ֻ��ע������ѧ�������Ŀγ̿��Ի�����һ����������ϵ��
	 */
	public void setOtherStackedSeries(String other) {
		this.otherStackedSeries = other;
	}

	/**
	 * ȡ�����ѻ�����
	 */
	public String getOtherStackedSeries() {
		return otherStackedSeries;
	}

	/**
	 * ��������ˮƽ���뷽ʽ
	 */
	public byte getXTitleAlign() {
		return IStyle.HALIGN_CENTER;
	}
	
	/**
	 * �������Ĵ�ֱ���뷽ʽ
	 */
	public byte getYTitleAlign() {
		return IStyle.VALIGN_MIDDLE;
	}

	/**
	 * ͼ�α����ˮƽ���뷽ʽ
	 */
	public byte getGraphTitleAlign() {
		return IStyle.HALIGN_CENTER;
	}

	/**
	 * ������λ��
	 */
	public byte getGridLocation() {
		return gridLineLocation;
	}

	/**
	 * ����������λ��
	 */
	public void setGridLocation(byte loc) {
		gridLineLocation = loc;
	}

}
