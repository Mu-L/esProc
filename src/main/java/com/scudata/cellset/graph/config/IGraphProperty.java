package com.scudata.cellset.graph.config;

import com.scudata.chart.Consts;

/**
 * ͼ�����Խӿڣ��Լ�ͼ�γ�������
 * @author Joancy
 *
 */
public interface IGraphProperty{
  /** ������λ�� -- ��ֵ�� */
  public static final byte GRID_VALUE = (byte)Consts.GRID_VALUE;
  /** ������λ�� -- ������ */
  public static final byte GRID_CATEGORY = (byte)Consts.GRID_CATEGORY;
  /** ������λ�� -- ȫ��*/
  public static final byte GRID_BOTH = (byte)Consts.GRID_BOTH;

  /** ������ -- û���� */
  public static final byte LINE_NONE = (byte)Consts.LINE_NONE;

  /** ������ -- ʵ�� */
  public static final byte LINE_SOLID = (byte)Consts.LINE_SOLID;

  /** ������ -- ������ */
  public static final byte LINE_LONG_DASH = (byte)Consts.LINE_DASHED;

  /** ������ -- ������ */
  public static final byte LINE_SHORT_DASH = (byte)Consts.LINE_DOTTED;

  /** ������ -- �㻮�� */
  public static final byte LINE_DOT_DASH = (byte)Consts.LINE_DOTDASH;

  /** ������ -- ˫�㻮�� */
  public static final byte LINE_2DOT_DASH = (byte)Consts.LINE_DOUBLE;

  /** ͼ�θ�ʽ -- JPG  */
  public static final byte IMAGE_JPG = Consts.IMAGE_JPG;

  /** ͼ�θ�ʽ -- GIF  */
  public static final byte IMAGE_GIF = Consts.IMAGE_GIF;

  /** ͼ�θ�ʽ -- PNG  */
  public static final byte IMAGE_PNG = Consts.IMAGE_PNG;

  /** ͼ�θ�ʽ -- FLASH*/
  public static final byte IMAGE_FLASH = Consts.IMAGE_FLASH;

  /** ͼ�θ�ʽ -- SVG */
  public static final byte IMAGE_SVG = Consts.IMAGE_SVG;

  /** ʱ��̶����� -- ��  */
  public static final byte TIME_YEAR = (byte) 1;

  /** ʱ��̶����� -- ��  */
  public static final byte TIME_MONTH = (byte) 2;

  /** ʱ��̶����� -- ��  */
  public static final byte TIME_DAY = (byte) 3;

  /** ʱ��̶����� -- ʱ  */
  public static final byte TIME_HOUR = (byte) 4;

  /** ʱ��̶����� -- ��  */
  public static final byte TIME_MINUTE = (byte) 5;

  /** ʱ��̶����� -- ��  */
  public static final byte TIME_SECOND = (byte) 6;

  /** ͼ����ʾ���� -- ��  */
  public static final byte DISPDATA_NONE = (byte) 1;

  /** ͼ����ʾ���� -- ͳ��ֵ  */
  public static final byte DISPDATA_VALUE = (byte) 2;

  /** ͼ����ʾ���� -- �ٷֱ�  */
  public static final byte DISPDATA_PERCENTAGE = (byte) 3;

  /** ͼ����ʾ���� -- ��ʾϵ�б���  */
  public static final byte DISPDATA_TITLE = (byte) 4;
  
  /** ͼ����ʾ���� -- ���ƺ�ͳ��ֵ  */
  public static final byte DISPDATA_NAME_VALUE = (byte) 5;
  
  /** ͼ����ʾ���� -- ���ƺͰٷֱ�  */
  public static final byte DISPDATA_NAME_PERCENTAGE = (byte) 6;

  /** ͼ��λ�� -- ���  */
  public static final byte LEGEND_LEFT = (byte) 1;

  /** ͼ��λ�� -- �ұ�  */
  public static final byte LEGEND_RIGHT = (byte) 2;

  /** ͼ��λ�� -- �ϱ�  */
  public static final byte LEGEND_TOP = (byte) 3;

  /** ͼ��λ�� -- �±�  */
  public static final byte LEGEND_BOTTOM = (byte) 4;

  /** ͼ��λ�� -- ��  */
  public static final byte LEGEND_NONE = (byte) 5;

  /** ͳ��ֵ������λ -- ������ */
  public static final double UNIT_ORIGIN = 1;

  /** ͳ��ֵ������λ -- �Զ����� */
  public static final double UNIT_AUTO = 2;

  /** ͳ��ֵ������λ -- ǧ */
  public static final double UNIT_THOUSAND = 1000;

  /** ͳ��ֵ������λ -- �� */
  public static final double UNIT_10THOUSAND = 10000;

  /** ͳ��ֵ������λ -- ���� */
  public static final double UNIT_MILLION = 1000000;

  /** ͳ��ֵ������λ -- ǧ�� */
  public static final double UNIT_10MILLION = 10000000;

  /** ͳ��ֵ������λ -- �� */
  public static final double UNIT_100MILLION = 100000000;

  /** ͳ��ֵ������λ -- ʮ�� */
  public static final double UNIT_BILLION = 1000000000;

  /** ͳ��ֵ������λ -- �ٷ�֮һ */
  public static final double UNIT_001 = 0.01;

  /** ͳ��ֵ������λ -- ǧ��֮һ */
  public static final double UNIT_0001 = 0.001;

  /** ͳ��ֵ������λ -- ���֮һ */
  public static final double UNIT_00001 = 0.0001;

  /** ͳ��ֵ������λ -- �����֮һ */
  public static final double UNIT_0000001 = 0.000001;

  public static final int AXIS_TOP = 0;
  public static final int AXIS_BOTTOM = 1;
  public static final int AXIS_LEFT = 2;
  public static final int AXIS_RIGHT = 3;
  public static final int AXIS_LEGEND = 4;
  public static final int AXIS_COLBORDER = 5;
  public static final int AXIS_PIEJOIN = 6;

  public static final byte CURVE_LAGRANGE = 0; //������������
  public static final byte CURVE_AKIMA = 1; //����������
  public static final byte CURVE_3SAMPLE = 2; //������������

  public static byte FLAG_DISCARDOTHER = 0;


  public int getLeftMargin();
  public int getRightMargin();
  public int getTopMargin();
  public int getBottomMargin();
  public int getTickLen();
  public int getCoorWidth();
  public double getCategorySpan();
  public int getSeriesSpan();
  public int getPieRotation();
  public int getPieHeight();
  public byte getType();
  public byte getCurveType();
  public boolean isPieSpacing();
  public boolean isMeterColorEnd();
  public boolean isMeterTick();
  public boolean isGradientColor();
  public GraphFonts getFonts();
  public AlarmLine[] getAlarmLines();
  public byte getDisplayData();
  public byte getDisplayData2();
  public boolean isDispStackSumValue();
  public byte getLegendLocation();
  
  public int getLegendVerticalGap();
  public int getLegendHorizonGap();
  
  public boolean getDrawLegendBySery();
  public int getAxisColor();
  public int getCanvasColor();
  public int getGraphBackColor();
  public byte getGridLocation();
  public byte getGridLineType();
  public int getGridLineColor();
  public byte getImageFormat();
  public boolean isGraphTransparent();
  public String getLegendLink();
  public double getDataUnit();
  public boolean isDrawLineDot();
  public boolean isOverlapOrigin();
  public boolean isDrawLineTrend();
  public boolean ignoreNull();
  public String getCustomClass();
  public String getCustomParam();
  public byte getLineThick();
  public byte getLineStyle();
  public boolean isShowOverlapText();
  public byte getStatusTimeType();
  public String getStatusTimeFormat();
  public boolean isDrawShade();
  public boolean isRaisedBorder();
  public boolean getFlag(byte key);
  public int[] getAxisColors();
  public void setAxisColors(int[] colors);
  public void setAxisColor(int color);
  public void setCanvasColor(int color);
  public void setGraphBackColor(int color);
  public void setGridLocation(byte loc);
  public void setGridLineType(byte type);
  public void setGridLineColor(int color);
  public void setImageFormat(byte format);
  public void setGraphTransparent(boolean b);
  public void setGradientColor(boolean b);
  public void setFonts(GraphFonts font);
  public void setDisplayData(byte data);
  public void setDisplayData2(byte data);
  public void setLegendLocation(byte location);
  public void setDataUnit(double unit);
  public void setDrawLineDot(boolean b);
  public void setShowOverlapText(boolean b);
  public void setStatusTimeType(byte type);
  public boolean isDrawDataTable();
  public boolean isDataCenter();
  
  public int getMeter3DEdge();
  public int getMeterRainbowEdge();
  public int getPieLine();

  public void setOtherStackedSeries(String other);
  public String getOtherStackedSeries();
  
  public byte getXTitleAlign();
  public byte getYTitleAlign();
  public byte getGraphTitleAlign();
  
}
