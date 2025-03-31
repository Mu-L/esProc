package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.util.*;
import java.text.*;
import java.awt.geom.*;

import com.scudata.cellset.IColCell;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IRowCell;
import com.scudata.cellset.IStyle;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.graph.GraphProperty;
import com.scudata.cellset.graph.PublicProperty;
import com.scudata.cellset.graph.config.*;
import com.scudata.chart.ChartColor;
import com.scudata.chart.Consts;
import com.scudata.chart.CubeColor;
import com.scudata.chart.Utils;
import com.scudata.common.*;
import com.scudata.common.control.*;
import com.scudata.util.*;
/**
 * ���л�ͼ��ĳ��������
 * ��װ��ͼ�εĹ��������Լ�һЩ������ͼ����
 * @author Joancy
 *
 */
public abstract class DrawBase implements IGraph {
	public ExtGraphProperty egp;
	public Graphics2D g;
	public GraphParam gp;
	public Palette palette;
	public int VALUE_RADIUS = 2; // ֱ��ͼ��ֵ��ʶ��С���λ�ԲȦ�İ뾶
	public int SHADE_SPAN = 4;
	public Rectangle TR = new Rectangle(); // Text Rectangle �ı����ʱ�ľ��ο�ߣ�x,y������
	public ArrayList<ValueLabel> labelList = new ArrayList<ValueLabel>();
	public ArrayList<ValuePoint> pointList = new ArrayList<ValuePoint>();

	private boolean disableLink = true;
	private transient Point2D.Double vShift, hShift;// ������Ϊ͸������3Dƽ̨ʱ����Ϊλ�ò�ͬ��x,y��ƫ�������������Ǽ�ȥƫ�ƣ��������Ǽ���ƫ��
	// ������϶������Ժ���Ҫ�ȱ������ռ䣬������gp.gRect2�����ƶ���
	private ValueLabel vlTitle, vlYTitle1, vlYTitle2, vlXTitle;

	protected boolean isSVG() {
		return egp.getImageFormat() == IGraphProperty.IMAGE_SVG;
	}

	Point2D.Double getVTickPoint(double shift) {
		double x = gp.gRect1.x - vShift.x;
		double y = gp.gRect1.y + gp.gRect1.height - shift - vShift.y;
		return new Point2D.Double(x, y);
	}

	Point2D.Double getHTickPoint(double shift) {
		double x = gp.gRect1.x + shift + hShift.x;
		double y = gp.gRect1.y + gp.gRect1.height + hShift.y;
		return new Point2D.Double(x, y);
	}
/**
 * ���ý�ֹ������
 * @param b ��ֹ������
 */
	public void setDisableLink(boolean b) {
		disableLink = b;
	}

	private boolean disableLink() {
		return disableLink;
	}

	/**
	 * ��ͼ�е��ı��ռ������������л���
	 * ��ֹ�ı�������
	 */
	public void outLabels() {
		double x, y;
		String text;
		byte direction;
		Color c;
		for (int i = 0; i < labelList.size(); i++) {
			ValueLabel vl = (ValueLabel) labelList.get(i);
			x = vl.p.x;
			y = vl.p.y;
			text = vl.text;
			direction = vl.direction;
			c = vl.c;
			if (gp.GFV_VALUE.color.getAlpha() == 0) {
				gp.GFV_VALUE.outText(x, y, text, direction, c);
			} else {
				gp.GFV_VALUE.outText(x, y, text, direction);
			}
		}
		labelList.clear();

		double[] buf;
		if (vlTitle != null) {
			buf = getHTitleX(gp.gRect2, egp.getGraphTitleAlign());
			TR = gp.GFV_TITLE.getTextSize();
			x = buf[0];
			y = vlTitle.p.y - TR.height;
			text = vlTitle.text;
			direction = (byte) buf[1];
			gp.GFV_TITLE.outText(x, y, text, direction);
			vlTitle = null;//����Ҫ�������ͬ��ԭ��Ϊ��˫��ͼʱ���������飬λ������ƫ�ƣ���ɼӴ֡�
		}

		if (vlXTitle != null) {
			buf = getHTitleX(gp.gRect1, egp.getXTitleAlign());
			TR = gp.GFV_XTITLE.getTextSize();
			x = buf[0];
			y = vlXTitle.p.y - TR.height/2;
			text = vlXTitle.text;
			direction = (byte) buf[1];
			gp.GFV_XTITLE.outText(x, y, text, direction);
			vlXTitle = null;
		}

		if (vlYTitle1 != null) {
			x = vlYTitle1.p.x;
			y = getVTitleY(gp.gRect1, egp.getYTitleAlign());
			text = vlYTitle1.text;
			byte vAlign = egp.getYTitleAlign();
			if(vAlign==IStyle.VALIGN_TOP) {
				direction = GraphFontView.TEXT_ON_BOTTOM;
			}else if(vAlign==IStyle.VALIGN_BOTTOM) {
				direction = GraphFontView.TEXT_ON_TOP;
			}else {
				direction = GraphFontView.TEXT_ON_RIGHT;
			}
			gp.GFV_YTITLE.outText(x, y, text, direction);
			vlYTitle1 = null;
		}

		if (vlYTitle2 != null) {
			x = vlYTitle2.p.x;
			y = getVTitleY(gp.gRect2, egp.getYTitleAlign());
			text = vlYTitle2.text;
			byte vAlign = egp.getYTitleAlign();
			if(vAlign==IStyle.VALIGN_TOP) {
				direction = GraphFontView.TEXT_ON_BOTTOM;
			}else if(vAlign==IStyle.VALIGN_BOTTOM) {
				direction = GraphFontView.TEXT_ON_TOP;
			}else {
				direction = GraphFontView.TEXT_ON_LEFT;
			}
			gp.GFV_YTITLE.outText(x, y, text, direction);
			vlYTitle2 = null;
		}
	}

	private double[] getHTitleX(Rectangle2D.Double rect,byte align) {
		double x[] = new double[2];
		if (align == IStyle.HALIGN_LEFT) {
			x[0] = rect.x;
			x[1] = GraphFontView.TEXT_ON_RIGHT;
		} else if (align == IStyle.HALIGN_RIGHT) {
			x[0] = rect.x + gp.gRect2.width;
			x[1] = GraphFontView.TEXT_ON_LEFT;
		} else {
			x[0] = rect.x + gp.gRect2.width / 2;
			x[1] = GraphFontView.TEXT_ON_TOP;
		}
		return x;
	}

	private double getVTitleY(Rectangle2D.Double rect, byte align) {
		double y;
		if (align == IStyle.VALIGN_TOP) {
			y = rect.y;
		} else if (align == IStyle.VALIGN_BOTTOM) {
			y = rect.y + gp.gRect2.height;
		} else {
			y = rect.y + gp.gRect2.height / 2;
		}
		return y;
	}

	protected int getPointRadius() {
		int radius = gp.getLineThickByte();
		if (radius < 2)
			return 2;
		return radius;
	}

	/**
	 * �������ı������ݵ�Ҳ�ڻ��ƹ��������ڻ���
	 * �ø÷������л������ݵ�
	 */
	public void outPoints() {
		int bs = Consts.LINE_SOLID;
		float bw = 1;
		boolean drawShade = false;// ����ͼ����ʱ�����ٻ������Ӱ���������Ƶ���Ӱ���ס�Ȼ���ֱ��
		float transparent = getTransparent();

		for (int i = 0; i < pointList.size(); i++) {
			ValuePoint vp = pointList.get(i);
			Point2D.Double p = vp.p;
			int radius;
			if (vp.radius < 0) {
				radius = getPointRadius();
			} else {
				radius = vp.radius;
			}
			Color fillColor;
			if (vp.fillColor == null) {
				fillColor = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
			} else {
				fillColor = vp.fillColor;
			}

			if (drawShade) {
				Utils.drawCartesianPoint1(g, p, vp.shape, radius, radius,
						radius, bs, bw, transparent);
			}
			Utils.drawCartesianPoint2(g, p, vp.shape, radius, radius, radius,
					bs, bw, getChartColor(fillColor), vp.borderColor,
					transparent);
		}
	}

	/**
	 * ���û�ͼ��ͼ���豸
	 * @param g ͼ���豸
	 */
	public void setGraphics2D(Graphics2D g) {
		this.g = g;
	}

	/**
	 * ����ͳ��ͼ�Ĵ�С
	 * @param w ͼ�ο��
	 * @param h ͼ�θ߶�
	 */
	public void setGraphWH(int w, int h) { 
		gp.setGraphWH(w, h);
	}

	/**
	 * ������չͼ�����Ե����ã�������ͼ�ĺ��ʵ��
	 * @param egp ��չͼ������
	 * @return �����ͼʵ��
	 */
	public static Object getInstance(ExtGraphProperty egp) {
		DrawBase graph = null;
		switch (egp.getType()) {
		case GraphTypes.GT_BAR:
			graph = new DrawBar();
			break;
		case GraphTypes.GT_BAR3D:
		case GraphTypes.GT_BAR3DOBJ:
			graph = new DrawBar3DObj();
			break;
		case GraphTypes.GT_BARSTACKED:
			graph = new DrawBarStacked();
			break;
		case GraphTypes.GT_BARSTACKED3DOBJ:
			graph = new DrawBarStacked3DObj();
			break;
		case GraphTypes.GT_COL:
			graph = new DrawCol();
			break;
		case GraphTypes.GT_COL3D:
			graph = new DrawCol3D();
			break;
		case GraphTypes.GT_COL3DOBJ:
			graph = new DrawCol3DObj();
			break;
		case GraphTypes.GT_COLSTACKED:
			graph = new DrawColStacked();
			break;
		case GraphTypes.GT_COLSTACKED3DOBJ:
			graph = new DrawColStacked3DObj();
			break;
		case GraphTypes.GT_LINE:
			graph = new DrawLine();
			break;
		case GraphTypes.GT_CURVE:
			graph = new DrawCurve();
			break;
		case GraphTypes.GT_LINE3DOBJ:
			graph = new DrawLine3DObj();
			break;
		case GraphTypes.GT_PIE:
			graph = new DrawPie();
			break;
		case GraphTypes.GT_PIE3DOBJ:
			graph = new DrawPie3DObj();
			break;
		case GraphTypes.GT_AREA:
			graph = new DrawArea();
			break;
		case GraphTypes.GT_AREA3D:
			graph = new DrawArea3D();
			break;
		case GraphTypes.GT_SCATTER:
			graph = new DrawDot();
			break;
		case GraphTypes.GT_2YCOLLINE:
			graph = new Draw2YColLine();
			break;
		case GraphTypes.GT_2YCOLSTACKEDLINE:
			graph = new Draw2YColStackedLine();
			break;
		case GraphTypes.GT_2Y2LINE:
			graph = new Draw2Y2Line();
			break;
		case GraphTypes.GT_RADAR:
			graph = new DrawRadar();
			break;
		default:
			graph = new DrawCol();
		}
		graph.transProperty(egp);
		graph.VALUE_RADIUS = graph.getPointRadius();

		return graph;
	}

	/**
	 * ���������Ƿ�ʹ�ý���ɫ������ɫcת��Ϊ�����ɫ��
	 * @param c ��ɫ
	 * @return �����ɫChartColor����
	 */
	public ChartColor getChartColor(Color c) {
		ChartColor cc = new ChartColor(c);
		if (egp.isGradientColor()) {// ����ǽ���ɫ������ΪChartColor����ģʽ
			cc.setColor1(c);
			cc.setColor2(c);
			cc.setGradient(true);
		} else {
			cc.setGradient(false);
		}
		return cc;
	}

	/**
	 * ��ȡ��ɫ���еĵ�index�����ɫֵ
	 * @param index ���
	 * @return ��ɫֵ
	 */
	public Color getColor(int index) {
		return new Color(palette.getColor(index));
	}

	/**
	 * ȡ��ǰͼ�ε�͸����
	 * @return
	 */
	public float getTransparent() {
		if (egp.isGraphTransparent())
			return 0.6f;
		return 1f;
	}

	/**
	 * ͳ��ͼ����gp���Խ��л�ͼ������չͼ������egpת��Ϊ
	 * ��ͼ������
	 * @param egp ��չͼ������
	 */
	public void transProperty(ExtGraphProperty egp) {
		this.egp = egp;
		this.gp = new GraphParam();
		palette = egp.getPlatte();
		gp.catNames = egp.getCategoryNames(); // ת�������ʱ��Ҫ�õ�catNames,����ת��
		gp.serNames = egp.getSeriesNames(egp.categories);
		gp.catNum = gp.catNames.size();
		gp.serNum = gp.serNames.size();

		String text = egp.getGraphTitle();
		GraphFonts fonts = egp.getFonts();
		GraphFont gf = fonts.getTitleFont();
		gp.GFV_TITLE = getGraphFontView(gf, text, GraphFontView.FONT_TITLE);

		text = "";
		gf = fonts.getLegendFont();
		gp.GFV_LEGEND = getGraphFontView(gf, text, GraphFontView.FONT_LEGEND);

		gf = fonts.getDataFont();
		gp.GFV_VALUE = getGraphFontView(gf, text, GraphFontView.FONT_VALUE);

		text = egp.getXTitle();
		gf = fonts.getXTitleFont();
		gp.GFV_XTITLE = getGraphFontView(gf, text, GraphFontView.FONT_XTITLE);

		text = egp.getYTitle();
		gf = fonts.getYTitleFont();
		gp.GFV_YTITLE = getGraphFontView(gf, text, GraphFontView.FONT_YTITLE);

		text = "";
		gf = fonts.getXLabelFont();
		gp.GFV_XLABEL = getGraphFontView(gf, text, GraphFontView.FONT_XLABEL);

		gf = fonts.getYLabelFont();
		gp.GFV_YLABEL = getGraphFontView(gf, text, GraphFontView.FONT_YLABEL);

		gp.imageFormat = egp.getImageFormat(); // ���������±���ɫ֮ǰҪ�������ø�ʽ����,͸��ɫ��ͼ�θ�ʽ�й�ϵ
		gp.setBackColor(egp.getGraphBackColor());
		gp.dataMarkFormat = egp.getDisplayDataFormat1();
		gp.dataMarkFormat2 = egp.getDisplayDataFormat2();
		if (!StringUtils.isValidString(gp.dataMarkFormat2)) {
			gp.dataMarkFormat2 = gp.dataMarkFormat;
		}

		if (!egp.isUserSetYEndValue1()) { // end
			gp.maxValue = egp.getMaxValue(egp.getCategories());
		} else {
			gp.maxValue = egp.getYEndValue1();
		}
		if (!egp.isUserSetYEndValue2()) {
			gp.maxValue2 = egp.getMaxValue(egp.category2);
		} else {
			gp.maxValue2 = egp.getYEndValue2();
		}

		if (egp.isUserSetYInterval1()) { // interval
			gp.interval = egp.getYInterval1();
		}
		if (egp.isUserSetYInterval2()) {
			gp.interval2 = egp.getYInterval2();
		}

		if (egp.isUserSetYMinMarks()) {
			gp.minTicknum = egp.getYMinMarks();
			gp.minTicknum2 = egp.getYMinMarks();
		}

		if (!egp.isUserSetYStartValue1()) { // start
			gp.minValue = egp.getMinValue(egp.getCategories());
		} else {
			gp.minValue = egp.getYStartValue1();
			gp.baseValue = gp.minValue;
			gp.minValue -= gp.baseValue;
			gp.maxValue -= gp.baseValue;
		}

		if (!egp.isUserSetYStartValue2()) {
			gp.minValue2 = egp.getMinValue(egp.category2);
		} else {
			gp.minValue2 = egp.getYStartValue2();
			gp.baseValue2 = gp.minValue2;
			gp.minValue2 -= gp.baseValue2;
			gp.maxValue2 -= gp.baseValue2;
		}

		if (egp.isUserSetTitleMargin()) {
			gp.graphMargin = (int) egp.getTitleMargin();
		}
		if (egp.isUserSetBarDistance()) {
			gp.barDistance = egp.getBarDistance();
		}
		if (egp.isUserSetXInterval()) {
			gp.graphXInterval = (int) egp.getXInterval();
		}
		if (egp.isUserSetStatusBarWidth()) {
			gp.statusBarHeight = egp.getStatusBarWidth();
		}

		if (egp.is2YGraph()) {
			gp.serNames2 = egp.getSeriesNames(egp.category2);
			gp.serNum2 = gp.serNames2.size();
		}
		gp.dispValueType = egp.getDisplayData();
		gp.dispValueOntop = (gp.dispValueType == IGraphProperty.DISPDATA_VALUE
				|| gp.dispValueType == IGraphProperty.DISPDATA_NAME_VALUE || gp.dispValueType == IGraphProperty.DISPDATA_TITLE);
		gp.dispValueType2 = egp.getDisplayData2();
		gp.dispValueOntop2 = (gp.dispValueType2 == IGraphProperty.DISPDATA_VALUE
				|| gp.dispValueType2 == IGraphProperty.DISPDATA_NAME_VALUE || gp.dispValueType2 == IGraphProperty.DISPDATA_TITLE);

		gp.dispStackSumValue = egp.isDispStackSumValue();
		gp.scaleMark = egp.getDataUnit();
		gp.drawLineDot = egp.isDrawLineDot();
		gp.isOverlapOrigin = egp.isOverlapOrigin();
		gp.drawLineTrend = egp.isDrawLineTrend();
		gp.setLineThick(egp.getLineThick());
		gp.cutPie = egp.isCutPie();
		gp.dispIntersectValue = egp.isShowOverlapText();
		gp.gradientColor = egp.isGradientColor();
		gp.coorColor = new Color(egp.getAxisColor());
		gp.gridColor = new Color(egp.getGridLineColor());
		gp.gridLineLocation = egp.getGridLocation();
		gp.gridLineStyle = egp.getGridLineType();
		gp.graphTransparent = egp.isGraphTransparent();
		gp.timeScale = egp.getStatusTimeType();
		gp.isDrawTable = egp.isDrawDataTable();
		gp.isDataCenter = egp.isDataCenter();
		
		gp.meter3DEdge = egp.getMeter3DEdge();
		gp.meterRainbowEdge = egp.getMeterRainbowEdge();
		gp.pieLine = egp.getPieLine();

		IGraphProperty p = egp.getIGraphProperty();
		gp.leftMargin = p.getLeftMargin();
		gp.rightMargin = p.getRightMargin();
		gp.topMargin = p.getTopMargin();
		gp.bottomMargin = p.getBottomMargin();
		gp.tickLen = p.getTickLen();
		gp.coorWidth = p.getCoorWidth();
		gp.categorySpan = p.getCategorySpan();
		gp.seriesSpan = p.getSeriesSpan();
		gp.pieRotation = p.getPieRotation();
		gp.pieHeight = p.getPieHeight();

		if (egp.isStackedGraph(this)) {
			gp.maxPositive = egp.getStackedMaxValue();
			gp.minNegative = egp.getStackedMinValue();
		}
		gp.isMultiSeries = (gp.serNum+gp.serNum2 > 1) || egp.isLegendOnSery();
	}
	
	/**
	 * ��ȡ��ǰͼ���Ƿ���ϵ�л��ƣ�����˫��ͼ��Ϊ����ϵ�к�����ϵ�У�
	 * ˫��ͼʱΪ��ϵ��ͼ��
	 * @return �Ƕ�ϵ��ͼ�η���true�����򷵻�false
	 */
	public boolean isMultiSeries(){
		if(egp.is2YGraph()){
			return true;
		}
		return gp.isMultiSeries;
	}

	protected boolean isLegendOnCategory() {
		byte type = egp.getType();
		return (type == GraphTypes.GT_PIE || type == GraphTypes.GT_PIE3DOBJ || !isMultiSeries());
	}

	// �Ƿ�Բ��ͼ��
	protected boolean isCircleLegend() {
		byte type = egp.getType();
		// ˫������ͼ��ʱ��ͼ����Ȼ���з�����Բ������������
		boolean isCircle = ( type == GraphTypes.GT_LINE || type == GraphTypes.GT_SCATTER || type == GraphTypes.GT_DOT3D);
		return isCircle;
	}

	protected boolean isShowMeterTick(double tick, Vector values) {
		for (int i = 0; i < values.size(); i++) {
			Double val = (Double) values.get(i);
			if (val.doubleValue() == tick) {
				return true;
			}
		}
		return false;
	}

	protected void drawLegendRect(int seriesNo, int series1Count, int x, int y,
			int fontHeight, int nFontHeight, int textWidth,
			boolean verticalPillar, boolean isGongZi, String stmp,
			StringBuffer htmlBuffer) {
		int xx, yy, ww, hh = fontHeight;

		if (isCircleLegend()
				|| (egp.is2YGraph() && gp.isMultiSeries && seriesNo >= series1Count)) { // �ڶ����ϵ��
			g.setStroke(new BasicStroke(0.00001f));
			yy = y - nFontHeight;
			setPaint(x, yy, fontHeight, fontHeight, seriesNo, verticalPillar);
			fillOval(x, yy, fontHeight, fontHeight);
			g.setColor(gp.coorColor);
			g.setStroke(new BasicStroke(0.1f));
			g.drawOval(x, yy, fontHeight, fontHeight);
		} else {
			xx = x;
			yy = y - nFontHeight;
			ww = fontHeight;
			hh = ww;

			Color tmpc = getColor(seriesNo);
			Color bc = egp.getAxisColor(IGraphProperty.AXIS_LEGEND);
			int bs = Consts.LINE_SOLID;
			float bw = 0.1f;
			Utils.draw2DRect(g, xx, yy, ww, hh, bc, bs, bw, egp.isDrawShade(),
					egp.isRaisedBorder(), getTransparent(),
					getChartColor(tmpc), !egp.isBarGraph(this));

		}

		// �����Ӵ���
		int x1 = x;
		int y1 = y-nFontHeight;
//		String coordx = x + "," + (y - nFontHeight) + ",";
		x += fontHeight + egp.getLegendHorizonGap();
		if (isGongZi) {
			stmp = stmp.substring(stmp.indexOf(".") + 1);
		}
		if (fontHeight != nFontHeight) { // ֻҪ������,����Ҫ�ض�λY
			y = y - nFontHeight + fontHeight;
		}

		// ��λ��λ:ͼ������Ϊ�Ҳ�����,�������,���ĵ�����Ų���ͼ�����Ӹ߶�
		y -= hh / 2;

		int wrapWidth = textWidth + 2; //�����㷨Ҫȥ�����������Ǳ��ߵĵ㣬���ⲻ��ȥ���������
		// �����㷨������wrapInchingWidth,���ﲻ�ü����ò�����

		ArrayList al = UtilsBase.wrapString(stmp,
				g.getFontMetrics(gp.GFV_LEGEND.font), wrapWidth);
		for (int i = 0; i < al.size(); i++) {
			String sectText = (String) al.get(i);
			gp.GFV_LEGEND.outText(x, y, sectText);
			y += fontHeight;
		}
		int w = textWidth;
		int h = y - y1;
//		coordx += (x + textWidth) + "," + y;

		String coordx;
		if (isSVG()) {
			coordx = "x=\"" + (int)x1 + "\" y=\"" + (int)y1 + "\" width=\"" + (int)w
					+ "\" height=\"" + (int)h + "\"";
		} else {
			coordx = (int)x1 + "," + (int)y1 + "," + (int)(x1 + w) + "," + (int)(y1 + h);
		}
		if(htmlBuffer!=null) {
			htmlBuffer.append(getStgLinkHtml(egp.getLegendLink(), "rect",
					coordx, egp.getLinkTarget(), stmp));
		}
	}

	protected boolean isLegendOnSide() {
		return egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT
				|| egp.getLegendLocation() == IGraphProperty.LEGEND_RIGHT;
	}

	protected Rectangle getLegendTextRect(String text) {
		TR = gp.GFV_LEGEND.getTextSize(text);
		int limitWidth = 0;
		if (isLegendOnSide()) {
			limitWidth = gp.graphWidth / 3;
		} else {
			limitWidth = (int) (gp.graphWidth * 0.75f);
		}
		if (TR.getWidth() > limitWidth) {
			FontMetrics fm = g.getFontMetrics(gp.GFV_LEGEND.font);
			ArrayList al = UtilsBase.wrapString(text, fm, limitWidth);
			TR.setBounds(0, 0, limitWidth, (int) (al.size() * TR.getHeight()));
		}
		return TR;
	}

	/**
	 * ����ͼ�ε�ͼ��������ͼ�������ĳ�����д�볬������htmlBuffer
	 * @param htmlBuffer �����ӻ������
	 */
	public void drawLegend(StringBuffer htmlBuffer) {
		if (egp.getLegendLocation() == IGraphProperty.LEGEND_NONE) {
			return;
		}
		int row, col;
		int x, y;
		int nFontHeight = 0, fontHeight = 0;
		int maxWidth = 0;
		int maxHeight = 0;
		int legendWidth = 0;
		int legendHeight = 0;
		int intTmp = 0;
		int borderx, bordery, borderwidth, borderheight;
		int totalWidth = 0;
		int totalHeight = 0;
		boolean isGongZi = egp.getType() == GraphTypes.GT_GONGZI;

		boolean verticalPillar = !egp.isBarGraph(this);
		/* �ҳ����ͼ����� */
		if (isLegendOnCategory()) {
			int cc = gp.catNames.size();
			for (int i = 0; i < cc; i++) {
				Object o = gp.catNames.get(i);
				String tmp = Variant.toString(o);
				if (!StringUtils.isValidString(tmp)) { // ��������ʿ����һ�մ�����ʱ��ȻҪ��֤һ���ַ��Ŀ��
					tmp = "A";
				}
				TR = getLegendTextRect(tmp);
				intTmp = TR.width;
				if (maxWidth < intTmp) {
					maxWidth = intTmp;
					maxHeight = TR.height;
				}
			}
		} else {
			int ss = gp.serNames.size();
			for (int i = 0; i < ss; i++) {
				Object o = gp.serNames.get(i);
				String tmp = Variant.toString(o);
				if (isGongZi) {
					tmp = tmp.substring(tmp.indexOf(".") + 1);
				}
				if (!StringUtils.isValidString(tmp)) {
					tmp = "A";
				}
				TR = getLegendTextRect(tmp);
				intTmp = TR.width;
				if (maxWidth < intTmp) {
					maxWidth = intTmp;
					maxHeight = TR.height;
				}
			}

			if (egp.is2YGraph() || egp.isNormalStacked()) {
				if(gp.serNames2!=null){
					int ss2 = gp.serNames2.size();
					for (int i = 0; i < ss2; i++) { // 2��ĵ�2��ϵ��
						Object o = gp.serNames2.get(i);
						TR = getLegendTextRect(Variant.toString(o));
						intTmp = TR.width;
						if (maxWidth < intTmp) {
							maxWidth = intTmp;
							maxHeight = TR.height;
						}
					}
				}
			}
		}

		/* ͼ������ */
		int size = 0;
		if (isLegendOnCategory()) {
			size = gp.catNames.size();
		} else {
			if (egp.is2YGraph() || (egp.isNormalStacked() && gp.serNames2!=null)) {
				size = gp.serNames.size() + gp.serNames2.size(); // �����˵ڶ���ϵ��
			} else {
				size = gp.serNames.size();
			}
		}

		int series1Count = gp.serNames.size();
		int seriesNo;

		/* ���ͼ��λ���Ϸ����·� */
		nFontHeight = maxHeight;
		fontHeight = gp.GFV_LEGEND.getTextSize("ABC").height;

		if (!isLegendOnSide()) {
			legendWidth = fontHeight + 3 * egp.getLegendHorizonGap() + maxWidth;
			totalWidth = gp.graphWidth - gp.leftMargin - gp.rightMargin;
			col = totalWidth / legendWidth;
			if (col < 1) {
				col = 1;
			}
			row = size / col;
			if (row * col < size) {
				row++;
			}
			int tmp = gp.graphHeight - gp.bottomInset
					- ((row + 1) * egp.getLegendVerticalGap() + row * nFontHeight);

			if (row > 1) {
				borderx = gp.leftMargin + (totalWidth - legendWidth * col) / 2;
				borderwidth = legendWidth * col;
			} else {
				borderx = gp.leftMargin + (totalWidth - legendWidth * size) / 2;
				borderwidth = legendWidth * size;
			}

			if (egp.getLegendLocation() == IGraphProperty.LEGEND_TOP) {
				bordery = gp.topInset;
			} else {
				bordery = tmp;
			}
			borderheight = nFontHeight * row + (row + 1) * egp.getLegendVerticalGap();

			drawRect(borderx, bordery, borderwidth, borderheight,
					egp.getAxisColor(IGraphProperty.AXIS_LEGEND)); // ϵ����Χ���ο�

			gp.legendBoxWidth = borderwidth;
			gp.legendBoxHeight = borderheight;

			for (int i = 0; i < row; i++) {
				if (row > 1) {
					x = gp.leftMargin + (totalWidth - legendWidth * col) / 2;
				} else {
					x = gp.leftMargin + (totalWidth - legendWidth * size) / 2;
				}
				for (int j = 0; j < col; j++) {
					if ((i * col) + j >= size) {
						break;
					}

					if (egp.getLegendLocation() == IGraphProperty.LEGEND_TOP) {
						y = gp.topInset + (i + 1) * (egp.getLegendVerticalGap() + nFontHeight);
					} else {
						y = tmp + (i + 1) * (egp.getLegendVerticalGap() + nFontHeight);
					}

					Object o = null;
					seriesNo = i * col + j;
					if (isLegendOnCategory()) {
						o = gp.catNames.get(seriesNo);
					} else {
						if (seriesNo >= series1Count) { // �ڶ����ϵ��
							o = gp.serNames2.get(seriesNo - series1Count);
						} else {
							o = gp.serNames.get(seriesNo);
						}

					}
					x += egp.getLegendHorizonGap();
					drawLegendRect(seriesNo, series1Count, x, y, fontHeight,
							nFontHeight, maxWidth, verticalPillar, isGongZi,
							Variant.toString(o), htmlBuffer);
					x += nFontHeight + egp.getLegendHorizonGap();
					x += maxWidth + egp.getLegendHorizonGap();
				}
			}

			if (egp.getLegendLocation() == IGraphProperty.LEGEND_TOP) {
				gp.topInset += (row + 1) * egp.getLegendVerticalGap() + row * nFontHeight + egp.getLegendVerticalGap();
			} else {
				gp.bottomInset += (row + 1) * egp.getLegendVerticalGap() + row * nFontHeight + egp.getLegendVerticalGap();
			}
		} else { /* ���ͼ��λ����߻��ұ� */
			legendWidth = fontHeight + 3 * egp.getLegendHorizonGap() + maxWidth;
			legendHeight = nFontHeight + 2 * egp.getLegendVerticalGap();
			totalHeight = gp.graphHeight - gp.topMargin - gp.bottomMargin;
			row = totalHeight / legendHeight;
			if (row < 1) {
				row = 1;
			}
			col = size / row;
			if (row * col < size) {
				col++;

			}
			int tmp = gp.graphWidth - gp.rightInset
					- ((col + 1) * egp.getLegendHorizonGap() + col * legendWidth);

			if (col > 1) {
				bordery = gp.topMargin + (totalHeight - legendHeight * row) / 2;
				borderheight = legendHeight * row;
			} else {
				bordery = gp.topMargin + (totalHeight - legendHeight * size)
						/ 2;
				borderheight = legendHeight * size;
			}
			if (egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT) {
				borderx = gp.leftInset;
			} else {
				borderx = tmp;
			}
			borderwidth = legendWidth * col;

			drawRect(borderx, bordery, borderwidth, borderheight,
					egp.getAxisColor(IGraphProperty.AXIS_LEGEND));

			gp.legendBoxWidth = borderwidth;
			gp.legendBoxHeight = borderheight;

			for (int j = 0; j < col; j++) {
				if (col > 1) {
					y = gp.topMargin + (totalHeight - legendHeight * row) / 2
							+ nFontHeight + egp.getLegendVerticalGap();
				} else {
					y = gp.topMargin + (totalHeight - legendHeight * size) / 2
							+ nFontHeight + egp.getLegendVerticalGap();
				}
				for (int i = 0; i < row; i++) {
					if ((j * row) + i >= size) {
						break;
					}

					if (egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT) {
						x = gp.leftInset + j * legendWidth;
					} else {
						x = tmp + j * legendWidth;
					}

					Object o = null;
					seriesNo = j * row + i;
					if (isLegendOnCategory()) {
						o = gp.catNames.get(seriesNo);
					} else {
						if (seriesNo >= series1Count) { // �ڶ���ϵ��
							o = gp.serNames2.get(seriesNo - series1Count);
						} else {
							o = gp.serNames.get(seriesNo);
						}
					}
					x += egp.getLegendHorizonGap();
					drawLegendRect(seriesNo, series1Count, x, y, fontHeight,
							nFontHeight, maxWidth, verticalPillar, isGongZi,
							Variant.toString(o), htmlBuffer);
					x += nFontHeight + egp.getLegendVerticalGap();
					y += legendHeight;
				}
			}

			if (egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT) {
				gp.leftInset += (col + 1) * egp.getLegendHorizonGap() + col * legendWidth + egp.getLegendHorizonGap();
			} else {
				gp.rightInset += (col + 1) * egp.getLegendHorizonGap() + col * legendWidth + egp.getLegendHorizonGap();
			}
		}
	}

	/**
	 * ����ָ���������õ�ǰ���ģʽ
	 * @param x ������x
	 * @param y ������y
	 * @param w �����
	 * @param h ���߶�
	 * @param c �����ɫ
	 * @param verticalPillar �Ƿ�����Բ��
	 */
	public void setPaint(double x, double y, double w, double h, Color c,
			boolean verticalPillar) {
		setPaint(x, y, w, h, c, verticalPillar, Palette.PATTERN_DEFAULT);
	}

	/**
	 * ����ָ���������õ�ǰ���ģʽ
	 * @param x ������x
	 * @param y ������y
	 * @param w �����
	 * @param h ���߶�
	 * @param pIndex ��ɫ���
	 * @param verticalPillar �Ƿ�����Բ��
	 */
	public void setPaint(double x, double y, double w, double h, int pIndex,
			boolean verticalPillar) {
		Color c = getColor(pIndex);
		byte p = palette.getPattern(pIndex);
		setPaint(x, y, w, h, c, verticalPillar, p);
	}

	protected void setPaint(double x, double y, double w, double h, Color c,
			boolean verticalPillar, byte fillPattern) {
		GradientPaint paint;
		if (gp.gradientColor) {
			Color cc = c;
			Color dd = c;

			int cValue = c.getRGB();
			if (cValue < -13410000) { // ��ɫ
				cc = Color.lightGray;
				dd = c;
			} else if (cValue < -8355712) { // �������ɫ����
				cc = c.brighter();
				dd = cc.darker().darker();
			} else {
				cc = c;
				dd = cc.darker().darker().darker();
			}

			if (verticalPillar) { // ��������
				paint = new GradientPaint((float)x, (float)y, cc, (float)(x + w), (float)y, dd);
			} else {
				paint = new GradientPaint((float)x, (float)y, cc, (float)x, (float)(y + h), dd);
			}
			g.setPaint(paint);
		} else if (egp.isRaisedBorder()
				|| fillPattern == Palette.PATTERN_DEFAULT) {
			g.setColor(c);
		} else {
			g.setPaint(Palette.getPatternPaint(c, fillPattern));
		}
	}

	protected String getStgLinkHtml(String link, String shape, String coords,
			Object target, Object legendValue) {
		return getStgLinkHtml(link, shape, coords, target, legendValue, null,
				null, null);
	}

	protected String replaceAll(String src, String sold, String snew) {
		return Sentence.replace(src, 0, sold, snew, Sentence.IGNORE_CASE
				+ Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE
				+ Sentence.ONLY_PHRASE);

	}

	protected String getTitle(String tips,Object categoryValue,Object legendValue,String fmtedVal) {
		String title = "";
		if (StringUtils.isValidString(tips)) {
			title = tips;
		} else {
			if (StringUtils.isValidString(categoryValue)) {
				title = categoryValue.toString();
			}
			if (StringUtils.isValidString(legendValue)) {
				title += " " + legendValue.toString();
			}
			if (fmtedVal != null) {
				title += " " + fmtedVal;
			}
		}
		title = replaceAll(title, "\"", "&quot;");
		return title;
	}
	
	protected String getStgLinkHtml(String link, String shape, String coords,
			Object target, Object legendValue, Object categoryValue,
			String fmtedVal, String tips) {
		if (link == null) {
			return null;
		}
		if (StringUtils.isValidString(link)) {
			// �ĳ����滻����㣬ͬʱ�ɰ�category��������
			if (StringUtils.isValidString(legendValue)) {
				link = replaceAll(link, "@legend", legendValue.toString());
			}
			if (StringUtils.isValidString(categoryValue)) {
				link = replaceAll(link, "@category", categoryValue.toString());
			}
			if (StringUtils.isValidString(tips)) {
				link = replaceAll(link, "@title", tips);
			}
		}

		String title = getTitle(tips,categoryValue,legendValue,fmtedVal);
		StringBuffer sb = new StringBuffer(128);
		if (isSVG()) {
			sb.append("<a");
			if (StringUtils.isValidString(link)) {
				sb.append(" xlink:href=\"").append(link).append("\" target=\"");
				sb.append(target).append("\"");
			}
			sb.append(">\n");
			if (StringUtils.isValidString(title)) {
				sb.append("<title>").append(title);
				sb.append("</title>");
			}

			sb.append("<").append(shape);
			sb.append(" ").append(coords);
			sb.append(" style=\"fill:transparent;stroke:transparent\"/>\n");

			sb.append("</a>\n");
		} else {
			sb.append("<area shape=\"").append(shape).append("\" coords=\"");
			sb.append(coords);
			if (StringUtils.isValidString(link)) {
				sb.append("\" href=\"").append(link).append("\" target=\"")
						.append(target);
			}

			if (StringUtils.isValidString(title)) {
				sb.append("\" title=\"").append(title);
			}
			sb.append("\">\n");
		}
		return sb.toString();
	}

	/**
	 * ��ָ��λ�û��������������
	 * @param x ������x
	 * @param y ������y
	 * @param w ���ӿ��
	 * @param h ���Ӹ߶�
	 * @param z ���ӽ���
	 */
	public void drawRectCubeLine(int x, int y, int w, int h, int z) {
		g.setColor(gp.coorColor);
		int hx, hy; // �����忴�������Ǹ��������
		hx = x + z;
		hy = y + h - z;

		g.drawLine(hx, hy, hx - z, hy + z);
		g.drawLine(hx, hy, hx, hy - h);
		g.drawLine(hx, hy, hx + w, hy);

	}

	/**
	 * ָ��λ�ô���һ�����Σ�cΪnullʱ��͸������������
	 * @param x ������x
	 * @param y ������y
	 * @param w ���
	 * @param h �߶�
	 * @param c �߿���ɫ
	 */
	public void drawRect(double x, double y, double w, double h, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Utils.drawRect(g, x, y, w, h);
	}

	/**
	 * ��ָ��������������ֱ��
	 * ���øú���֮ǰ�����ȵ��� Utils.setStroke(); ȷ����ǰ��������ȷ����䷽��
	 * @param x1 ��1������
	 * @param y1 ��1������
	 * @param x2 ��2������
	 * @param y2 ��2������
	 * @param c ��ɫ��nullʱ������
	 */
	public void drawLine(double x1, double y1, double x2, double y2, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Utils.drawLine(g,x1, y1, x2, y2);
	}

	/**
	 * ��ָ��������������һ�λ�
	 * @param x ������
	 * @param y ������
	 * @param w ���
	 * @param h �߶�
	 * @param startAngle ��ʼ�Ƕ�
	 * @param arcAngle ���ĽǶȳ�
	 * @param c ��ɫ��nullʱ������
	 */
	public void drawArc(int x, int y, int w, int h, int startAngle,
			int arcAngle, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		g.drawArc(x, y, w, h, startAngle, arcAngle);
	}

	/**
	 * ����ָ����ɫ������״s
	 * @param s ��״����
	 * @param c ��ɫ��nullʱ������
	 */
	public void drawShape(Shape s, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		g.draw(s);
	}

	/**
	 * ����ָ��λ�û��ƶ����
	 * @param x ���x������ֵ
	 * @param y ���y������ֵ
	 * @param n ��ĸ���
	 * @param c ��ɫ��nullʱ������
	 */
	public void drawPolygon(double[] x, double[] y, int n, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Shape poly = Utils.newPolygon2D(x, y);
		g.draw(poly);
	}

	public void drawPolygon(int[] x, int[] y, int n, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Polygon poly = new Polygon(x, y, n);
		g.draw(poly);
	}

	/**
	 * ����ָ��λ�û�����Բ
	 * @param x x����
	 * @param y y����
	 * @param w ���
	 * @param h �߶�
	 * @param c ��ɫ��nullʱ������
	 */
	public void drawOval(int x, int y, int w, int h, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		g.drawOval(x, y, w, h);
	}

	/**
	 * ����ͼ������������ָ��λ�������Բ
	 * @param x x����
	 * @param y y����
	 * @param w ���
	 * @param h �߶�
	 */
	public void fillOval(int x, int y, int w, int h) {
		if (egp.isDrawShade()) {
			Paint p = g.getPaint();
			Color c = g.getColor();
			g.setColor(Color.lightGray);
			g.fillOval(x + SHADE_SPAN, y + SHADE_SPAN, w, h);
			g.setColor(c);
			g.setPaint(p);
		}
		g.fillOval(x, y, w, h);
	}

	/**
	 * ����ͼ������������ָ��λ��������
	 * @param x x����
	 * @param y y����
	 * @param w ���
	 * @param h �߶�
	 */
	public void fillRect(double x, double y, double w, double h) {
		Color bc = null;
		int bs = 0;
		float bw = 0;
		ChartColor fillColor = getChartColor(g.getColor());
		Utils.draw2DRect(g, x, y, w, h, bc, bs, bw, egp.isDrawShade(),
				egp.isRaisedBorder(), getTransparent(), fillColor,
				!egp.isBarGraph(this));
	}

	/**
	 * �����ĵ� cx,cy��c1��c2����ɫ��״��ɢ�ݶ������״s Ŀǰ��֧��cx��cy����״s�ڵ����
	 * @param s
	 *            Shape ��״����
	 * @param c1
	 *            Color ��ɫ1
	 * @param c2
	 *            Color ��ɫ2
	 * @param cx
	 *            int ���ĵ�x
	 * @param cy 
	 *            int ���ĵ�y
	 */
	public void fillDotGradientShape(Shape s, Color c1, Color c2, double cx, double cy) {
		Rectangle sBounds = s.getBounds();
		double w = sBounds.getWidth();
		double h = sBounds.getHeight();
		int r = (int) Math.sqrt(w * w + h * h);
		int dAngle = 5;
		int sAngle = 0;

		Arc2D.Double a2dd = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2,
				sAngle, dAngle + 3, Arc2D.PIE);
		java.awt.geom.Area sA = new java.awt.geom.Area(s);
		Point2D.Double p1 = new Point2D.Double(cx, cy);
		while (sAngle <= 360) {
			java.awt.geom.Area da = new java.awt.geom.Area(a2dd);
			java.awt.geom.Area a1 = new java.awt.geom.Area(s);
			a1.add(da);
			a1.subtract(sA);

			da.subtract(a1);
			Point2D p2 = a2dd.getStartPoint();
			p2.setLocation((p1.getX() + p2.getX()) / 2,
					(p1.getY() + p2.getY()) / 2);
			g.setPaint(new GradientPaint(p1, c1, p2, c2));
			g.fill(da);
			sAngle += dAngle;
			a2dd.setAngleStart(sAngle);
		}
	}


	/**
	 * ����ָ����������һ��������
	 * 
	 * @param x
	 *            int �������x �������y ����h���������ͻ���������ó�
	 * @param w
	 *            int �������w
	 * @param h
	 *            int �������h
	 * @param z
	 *            int ������z���
	 * @param dz
	 *            int ������z��λ��
	 * @param colorIndex
	 *            int ��ɫ���
	 * @param sb
	 *            StringBuffer ���ɳ����ӵĻ���
	 * @param cat
	 *            Object ��ǰ����ֵ
	 * @param egs
	 *            ExtGraphSery ϵ��
	 */
	public void drawRectCube(double x, double w, double h, double z, double dz,
			int colorIndex, StringBuffer sb, Object cat, ExtGraphSery egs) {
		ChartColor chartColor = getChartColor(getColor(colorIndex));
		double xx, yy, ww, hh;
		if (h >= 0) {
			xx = x + dz;
			yy = gp.valueBaseLine - h - dz;
			ww = w;
			hh = h;
		} else {
			xx = x + dz;
			yy = gp.valueBaseLine - dz;
			ww = w;
			hh = Math.abs(h);
		}
		Color bc = egp.getAxisColor(PublicProperty.AXIS_COLBORDER);
		int bs = Consts.LINE_SOLID;
		float bw = 1;
		double coorShift = z;
		Utils.draw3DRect(g, xx, yy, ww, hh, bc, bs, bw, egp.isDrawShade(),
				egp.isRaisedBorder(), getTransparent(), chartColor,
				!egp.isBarGraph(this), coorShift);
		htmlLink(xx, yy, ww, hh, sb, cat, egs);

	}

	protected Rectangle2D.Double rightTop, rightBottom, leftTop, leftBottom; // ������Ӧ�Ѿ�ռ���˵�λ��

	/**
	 * ��Բ�������ֵ�����ݽǶȵ���Text�����ꡣ�״�ͼ�Լ���ͼ�õ�
	 * 
	 * @param gfv
	 *            GraphFontView ���嶨����ͼ
	 * @param text
	 *            String �ı�ֵ
	 * @param angle
	 *            double ��ǰ��ֵ����Բ�ϵĽǶ�λ��
	 * @param x
	 *            int �ı���x
	 * @param y
	 *            int �ı���y
	 */
	public void drawOutCircleText(GraphFontView gfv, String text, double angle,
			double x, double y) {
		if (!StringUtils.isValidString(text)) {
			return;
		}
		if (egp.isShowOverlapText()) {
			drawOutCircleText(gfv, text, angle, x, y, false);
		} else {
			if (!drawOutCircleText(gfv, text, angle, x, y, false)) {
				drawOutCircleText(gfv, text, angle, x, y, true);
			}
		}
	}

	/**
	 * ��Բ���ڵ���ֵ�����ݽǶȵ���Text�����ꡣ�״�ͼ�Լ���ͼ�õ�
	 * 
	 * @param gfv
	 *            GraphFontView ���嶨����ͼ
	 * @param text
	 *            String �ı�ֵ
	 * @param angle
	 *            double ��ǰ��ֵ����Բ�ϵĽǶ�λ��
	 * @param x
	 *            int �ı���x
	 * @param y
	 *            int �ı���y
	 */
	public void drawInnerCircleText(GraphFontView gfv, String text,
			double angle, double x, double y) {
		if (!StringUtils.isValidString(text)) {
			return;
		}
		gfv.outText(x, y, text,
				GraphFontView.reverseDirection(angleToPosition(gfv, angle)));
	}

	protected byte angleToPosition(GraphFontView gfv, double angle) {
		if (angle >= 360) {
			angle -= 360;
		}
		if (gfv.vertical || gfv.angle == 90) {
			if (angle < 5) {
				return GraphFontView.TEXT_ON_RIGHT;
			} else if (angle < 175) {
				return GraphFontView.TEXT_ON_TOP;
			} else if (angle < 185) {
				return GraphFontView.TEXT_ON_LEFT;
			} else if (angle < 355) {
				return GraphFontView.TEXT_ON_BOTTOM;
			} else {
				return GraphFontView.TEXT_ON_RIGHT;
			}
		} else if (gfv.angle == 0) {
			if (angle < 85) {
				return GraphFontView.TEXT_ON_RIGHT;
			} else if (angle < 95) {
				return GraphFontView.TEXT_ON_TOP;
			} else if (angle < 265) {
				return GraphFontView.TEXT_ON_LEFT;
			} else if (angle < 275) {
				return GraphFontView.TEXT_ON_BOTTOM;
			} else {
				return GraphFontView.TEXT_ON_RIGHT;
			}
		} else {
			if (angle < 85) {
				return GraphFontView.TEXT_ON_RIGHT;
			} else if (angle < 95) {
				return GraphFontView.TEXT_ON_TOP;
			} else if (angle < 180 + gfv.angle) {
				return GraphFontView.TEXT_ON_LEFT;
			} else if (angle < 360 - gfv.angle) {
				return GraphFontView.TEXT_ON_BOTTOM;
			} else {
				return GraphFontView.TEXT_ON_RIGHT;
			}
		}
	}

	protected boolean drawOutCircleText(GraphFontView gfv, String text,
			double angle, double x, double y, boolean drawExtend) {
		double tmpAngle = angle;
		int w = gfv.getTextSize(text).width;
		int h = gfv.getTextSize(text).height;

		while (tmpAngle < 0) {
			tmpAngle += 360;
		}
		while (tmpAngle > 360) {
			tmpAngle -= 360;
		}

		boolean isLeft = tmpAngle > 90 && tmpAngle < 270;
		boolean isTop = !(tmpAngle > 180 && tmpAngle < 360);
		if (drawExtend) {
			double x1 = x, y1 = y;
			if (isLeft && isTop) { // ���� - ��� //
				if (leftTop == null) {
					leftTop = new Rectangle2D.Double(gp.gRect2.x, gp.gRect2.y, 0, 0);
				}
				x = leftTop.x;
				y = leftTop.y + leftTop.height + 5;
				leftTop = new Rectangle2D.Double(leftTop.x, y, w, h);

				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				y += h / 2;
			} else if (isLeft && !isTop) { // ���� �� �±�
				if (leftBottom == null) {
					leftBottom = new Rectangle2D.Double(gp.gRect2.x, gp.gRect2.y
							+ gp.gRect2.height, 0, 0);
				}
				x = leftBottom.x;
				y = leftBottom.y - h;
				leftBottom = new Rectangle2D.Double(x, leftBottom.y, w, h);
				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				w /= 2;
			} else if (!isLeft && isTop) { // ���� �� �ϱ�
				if (rightTop == null) {
					rightTop = new Rectangle2D.Double(gp.gRect2.x + gp.gRect2.width,
							gp.gRect2.y, 0, 0);
				}
				x = rightTop.x - w - 2;
				x = x1 > x ? rightTop.x : x;

				y = rightTop.y + h;
				rightTop = new Rectangle2D.Double(x, rightTop.y, w, h);
				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				x -= w / 2;
			} else { // ���� �� �ұ�
				if (rightBottom == null) {
					rightBottom = new Rectangle2D.Double(gp.gRect2.x + gp.gRect2.width,
							gp.gRect2.y + gp.gRect2.height, 0, 0);
				}
				x = rightBottom.x - w;
				x = x1 > x ? rightBottom.x : x;

				y = rightBottom.y - h - 5;
				rightBottom = new Rectangle2D.Double(rightBottom.x, y, w, h);
				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				h /= 2;
			}

			isLeft = x1 > x;
		} else {
			byte direction = angleToPosition(gfv, angle);

			if (!(this instanceof DrawRadar)) {// �״�ͼʱ����ʹ���ӳ���
				int shiftX = 10;
				double x1 = x, y1 = y, x2 = x, y2 = y;
				if (direction == GraphFontView.TEXT_ON_LEFT) {
					x1 = x - shiftX;
					x = x1;
				} else if (direction == GraphFontView.TEXT_ON_RIGHT) {
					x2 = x + shiftX;
					x = x2;
				}
				boolean isOutText = gfv.outText(x, y, text, direction);
				if (isOutText) {
					Color tmpc = egp.getAxisColor(GraphProperty.AXIS_PIEJOIN);
					Line2D.Double tmpShape = new Line2D.Double(x1, y1, x2, y2);
					g.setStroke(new BasicStroke(1.0f));
					drawShape(tmpShape, tmpc);
				}
				return isOutText;
			}

			return gfv.outText(x, y, text, direction);
		}
		if (isLeft) {
			x -= w;
		}
		if (!isTop) {
			y += h;
		}

		return gfv.outText(x, y, text, GraphFontView.TEXT_FIXED); /**/
	}

	/**
	 * ���ݲ��������ı�
	 * @param text �ı�
	 * @param x ������
	 * @param y ������
	 * @param gf ���嶨��
	 */
	public void drawText(String text, int x, int y, GraphFont gf) {
		if (!StringUtils.isValidString(text)) {
			return;
		}
		g.setColor(new Color(gf.getColor()));
	}

	/**
	 * ������������
	 * @param delx ������֮��Ŀ��
	 * @param ci ���������
	 */
	public void drawGridLineV(double delx, int ci) {
		if (ci == 0 || ci == gp.tickNum
				|| gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_CATEGORY){
			return;
		}
		BasicStroke stroke = getLineStroke(gp.gridLineStyle, 1f);
		if (stroke == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(stroke);

		Utils.drawLine(g,gp.gRect2.x + ci * delx, gp.gRect2.y
				+ gp.gRect2.height, gp.gRect2.x + ci * delx,
				gp.gRect2.y + 1);
		g.setStroke(new BasicStroke(0.00001f));
		Utils.drawLine(g,
				// б����̫�̣�ֻ��ֱ��
				gp.gRect1.x + ci * delx,
				gp.gRect1.y + gp.gRect1.height,
				gp.gRect2.x + ci * delx, gp.gRect2.y + gp.gRect2.height);
	}

	/**
	 * ������������
	 * @param dely ������֮��ĸ߶�
	 * @param ci ���������
	 */
	public void drawGridLine(double dely, int ci) {
		Color c = egp.getAxisColor(IGraphProperty.AXIS_LEFT);
		double tmpi = gp.gRect1.y + gp.gRect1.height - ci * dely;
		if (c != null) {
			Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
			drawLine(gp.gRect1.x - vShift.x - gp.tickLen, tmpi, gp.gRect1.x
					- vShift.x, tmpi, c); // Y�������С����
		}

		// ���������߶��߸���߿��غϣ�Ŀǰ����߿���ɫ���ȣ�
		if (ci == 0 || ci == gp.tickNum
				|| gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_CATEGORY){
			return;
		}
		BasicStroke bs = getLineStroke(gp.gridLineStyle, 1f);
		if (bs == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(bs);
		Utils.drawLine(g,gp.gRect2.x, gp.gRect2.y + gp.gRect2.height - ci
				* dely, gp.gRect2.x + gp.gRect2.width - 1, gp.gRect2.y
				+ gp.gRect2.height - ci * dely);

		if (c != null) {// ������᲻Ϊ͸��ɫʱ����Ҫ����б������
			// б����̫�̣�ֻ��ֱ�� ;
			Utils.drawLine(g,gp.gRect1.x, gp.gRect1.y + gp.gRect1.height - ci
					* dely, gp.gRect2.x,
					gp.gRect2.y + gp.gRect2.height - ci * dely);
		}
	}

	
	/**
	 * ���������������
	 * @param catX ����ĺ�����
	 */
	public void drawGridLineCategoryV(double catX) {
		if (gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_VALUE){
			return;
		}
		BasicStroke stroke = getLineStroke(gp.gridLineStyle, 1f);
		if (stroke == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(stroke);

		Utils.drawLine(g,catX, gp.gRect2.y+ gp.gRect2.height, 
				catX,gp.gRect2.y + 1);
	}

	/**
	 * ���������������
	 * @param catY �����������
	 */
	public void drawGridLineCategory(double catY) {
		if (gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_VALUE){
			return;
		}
		BasicStroke stroke = getLineStroke(gp.gridLineStyle, 1f);
		if (stroke == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(stroke);

		Utils.drawLine(g,gp.gRect2.x+1, catY,gp.gRect2.x+gp.gRect2.width,catY);
	}
	
	/**
	 * �ó���ͼ����Χ�ռ�
	 */
	public void keepGraphSpace() {
		boolean isPie = (this instanceof DrawPie)
				|| (this instanceof DrawPie3DObj);
		int space = gp.graphMargin;
		if (isPie) {
			if (space > -1) {
				gp.leftInset += space;
				gp.rightInset += space;
				gp.topInset += space;
				gp.bottomInset += space;
				return;
			}

			ArrayList cats = egp.categories;
			int cc = cats.size();
			double maxW = 0;
			double maxH = 0;
			for (int i = 0; i < cc; i++) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				ArrayList sers = egc.getSeries();
				for (int n = 0; n < sers.size(); n++) {
					String fmt;
					String text = "";
					ExtGraphSery egs = (ExtGraphSery) sers.get(n);
					// ��ʾ��ֵ��ʾ
					switch (gp.dispValueType) {
					case 1: // ����ʾ
						text = "";
						break;
					case 2: // ��ʾ��ֵ
						if (StringUtils.isValidString(gp.dataMarkFormat)) {
							fmt = gp.dataMarkFormat;
						} else {
							fmt = "";
						}
						text = getFormattedValue(egs.getValue(), fmt);
						break;
					default: // ��ʾ�ٷֱ�
						if (StringUtils.isValidString(gp.dataMarkFormat)) {
							fmt = gp.dataMarkFormat;
						} else {
							fmt = "0.00%";
						}
						text = getFormattedValue(egs.getValue(), fmt);
						break;
					}

					Rectangle rec = gp.GFV_VALUE.getTextSize(text);
					if (rec.getWidth() > maxW) {
						maxW = rec.getWidth();
					}
					if (rec.getHeight() > maxH) {
						maxH = rec.getHeight();
					}
				}
			}
			gp.leftInset += maxW;
			gp.rightInset += maxW;
			gp.topInset += maxH;
			gp.bottomInset += maxH;
		} else {
			if (space == -1) {
				space = 5;
			}
			gp.leftInset += space;
			gp.rightInset += space;
			gp.topInset += space;
			gp.bottomInset += space;
		}
	}

	/**
	 * ��ͳ��ͼ���� 
	 */
	public void drawTitle() {
		if (!StringUtils.isValidString(gp.GFV_TITLE.text)) {
			return;
		}
		int y = 0;
		TR = gp.GFV_TITLE.getTextSize();
		y = gp.topInset + TR.height;

		Point2D.Double p = new Point2D.Double(0, y);
		vlTitle = new ValueLabel(gp.GFV_TITLE.text, p, null);
		gp.topInset += TR.height + 4;
	}

	/**
	 * ��ƽ��ͼ������ľ���
	 */
	public void drawGraphRect() {
		if (gp.graphBackColor != null) {
			double bx, by, bw, bh;
			bx = gp.gRect2.x;
			by = gp.gRect2.y;
			bw = gp.gRect2.width;
			bh = gp.gRect2.height;
			if (egp.isGradientColor()) {
				CubeColor ccr = new CubeColor(gp.graphBackColor);
				ChartColor tmpcc = new ChartColor();
				tmpcc.setColor1(ccr.getLight(0.95f));
				tmpcc.setColor2(ccr.getLight(0.65f));
				tmpcc.setAngle(0);
				Utils.setPaint(g, bx, by, bw, bh, tmpcc);
			} else {
				g.setColor(gp.graphBackColor);
			}
			Utils.fillRect(g,bx, by, bw, bh);
		}

		double vx = 0, vy = 0, hx = 0, hy = 0;
		// �����x��yƫ���Լ������

		g.setStroke(new BasicStroke(1f));
		Color c;
		double x1 = gp.gRect2.x;
		double y1 = gp.gRect2.y;
		double x2 = x1 + gp.gRect2.width;
		double y2 = y1;
		c = egp.getAxisColor(IGraphProperty.AXIS_TOP);
		drawLine(x1, y1, x2, y2, c);

		x1 = x2;
		y1 = y2 + gp.gRect2.height;
		c = egp.getAxisColor(IGraphProperty.AXIS_RIGHT);
		drawLine(x1, y1, x2, y2, c);

		double x, y, w, h;
		double coorShift = gp.gRect2.x - gp.gRect1.x;
		c = egp.getAxisColor(IGraphProperty.AXIS_BOTTOM);
		if (c != null) {
			if (coorShift != 0) {
				if (egp.isGradientColor()) {
					x = gp.gRect1.x;
					y = gp.gRect1.y + gp.gRect1.height;
					w = gp.gRect1.width;
					h = Utils.getPlatformH(coorShift);
					hy = h;
					ChartColor fillColor = new ChartColor(c);
					Utils.draw3DRect(g, x, y, w, h, null, 0, 0, false, false,
							1, fillColor, true, coorShift);
				} else {
					x2 = x1 - gp.gRect2.width;
					y2 = y1;
					drawLine(x1, y1, x2, y2, c);
					drawLine(gp.gRect1.x, gp.gRect1.y + gp.gRect1.height,
							gp.gRect1.x + gp.gRect1.width, gp.gRect1.y
									+ gp.gRect1.height, c);
					drawLine(gp.gRect1.x, gp.gRect1.y + gp.gRect1.height,
							gp.gRect2.x, gp.gRect2.y + gp.gRect2.height, c);
					drawLine(gp.gRect1.x + gp.gRect1.width, gp.gRect1.y
							+ gp.gRect1.height, gp.gRect2.x + gp.gRect2.width,
							gp.gRect2.y + gp.gRect2.height, c);
				}
			} else {
				x1 = gp.gRect1.x;
				y1 = gp.gRect1.y + gp.gRect1.height;
				x2 = x1 + gp.gRect1.width;
				y2 = y1;
				drawLine(x1, y1, x2, y2, c);
			}
		} else {
			hx = coorShift;
			hy = -coorShift;
		}
		hShift = new Point2D.Double(hx, hy);

		c = egp.getAxisColor(IGraphProperty.AXIS_LEFT);
		if (c != null) {
			if (coorShift != 0) {
				if (egp.isGradientColor()) {
					w = Utils.getPlatformH(coorShift);
					x = gp.gRect1.x - w;
					y = gp.gRect1.y;
					h = gp.gRect1.height;
					vx = w;
					ChartColor fillColor = new ChartColor(c);
					Utils.draw3DRect(g, x, y, w, h, null, 0, 0, false, false,
							1, fillColor, false, coorShift);
				} else {
					x1 = gp.gRect2.x;
					y1 = gp.gRect2.y;
					drawLine(x1, y1, x2, y2, c);
					drawLine(gp.gRect1.x, gp.gRect1.y, gp.gRect1.x, gp.gRect1.y
							+ gp.gRect1.height, c);
					drawLine(gp.gRect1.x, gp.gRect1.y + gp.gRect1.height,
							gp.gRect2.x, gp.gRect2.y + gp.gRect2.height, c);
					drawLine(gp.gRect1.x, gp.gRect1.y, gp.gRect2.x,
							gp.gRect2.y, c);
				}
			} else {
				x1 = gp.gRect1.x;
				y1 = gp.gRect1.y;
				x2 = x1;
				y2 = y1 + gp.gRect1.height;
				drawLine(x1, y1, x2, y2, c);
			}
		} else {
			vx = -coorShift;
			vy = coorShift;
		}
		vShift = new Point2D.Double(vx, vy);
	}

	private String getLeftText() {
		String label = "";
		if (egp.isBarGraph(this)) {
			label = gp.GFV_YTITLE.text;
		} else {
			label = gp.GFV_XTITLE.text;
		}
		return label;
	}

	/**
	 * �����������
	 */
	public void drawLabel() {
		/* ��X����� */
		String label = getLeftText();

		int x = 0;
		int y = 0;
		if (StringUtils.isValidString(label)) {
			TR = gp.GFV_XTITLE.getTextSize(label);
			y = gp.graphHeight - gp.bottomInset;
			vlXTitle = new ValueLabel(label, new Point2D.Double(0, y), null);
			gp.bottomInset += TR.height + 2;
		}

		/* ��Y����� */
		if (egp.isBarGraph(this)) {
			label = gp.GFV_XTITLE.text;
		} else {
			label = gp.GFV_YTITLE.text;
		}

		if (StringUtils.isValidString(label)) {
			// Y1�����
			TR = gp.GFV_YTITLE.getTextSize(label);
			x = gp.leftInset;

			// �������ݱ�ʱ�����ݱ��λ�û���ռ����λ�ã�Ų������ȥ��
			vlYTitle1 = new ValueLabel(label, new Point2D.Double(x, 0), null);
			gp.leftInset += TR.width + 4;
		}

		if (egp.is2YGraph() && StringUtils.isValidString(gp.GFV_YTITLE.text2)) { // Y2�����
			TR = gp.GFV_YTITLE.getTextSize(gp.GFV_YTITLE.text2);
			x = gp.graphWidth - gp.rightInset;

			vlYTitle2 = new ValueLabel(gp.GFV_YTITLE.text2, new Point2D.Double(x, 0),
					null);
			gp.rightInset += TR.width + 4;
		}

	}

	/**
	 * ��ָ��λ�û�һ�����ݵ�
	 * @param pt �������
	 * @param shape �����״
	 * @param radius �뾶
	 * @param borderStyle �߿���
	 * @param borderWeight �߿�ֶ�
	 * @param backColor ����ɫ
	 * @param foreColor �߿���ɫ
	 */
	public void drawPoint(Point2D.Double pt, int shape, double radius, int borderStyle,
			float borderWeight, Color backColor, Color foreColor) {
		if (egp.isDrawShade()) {
			Utils.drawCartesianPoint1(g, pt, shape, radius, radius, radius,
					borderStyle, borderWeight, getTransparent());
		}
		Utils.drawCartesianPoint2(g, pt, shape, radius, radius, radius,
				borderStyle, borderWeight, getChartColor(backColor), foreColor,
				getTransparent());
	}

	/**
	 * ��ָ����������������������ݣ���д�뻺��sb
	 * @param x1 ������
	 * @param y1 ������
	 * @param w ���
	 * @param h �߶�
	 * @param sb html����
	 * @param category ����ֵ
	 * @param egs ϵ��ֵ
	 */
	public void htmlLink(double x1, double y1, double w, double h, StringBuffer sb,
			Object category, ExtGraphSery egs) {
		if (disableLink() || sb==null) {
			return;
		}
		// �����Ӵ���
		int minimum = 10;
		if (w < minimum) {
			w = minimum;
		}
		if (h < minimum) {
			h = minimum;
		}
		String coordx;
		if (isSVG()) {
//			if (!StringUtils.isValidString(egp.getLink())) {
//				Logger.debug("SVG graph must specify hyper link to generate dynamic links.");
//				return;
//			}

			coordx = "x=\"" + (int)x1 + "\" y=\"" + (int)y1 + "\" width=\"" + (int)w
					+ "\" height=\"" + (int)h + "\"";
		} else {
			coordx = (int)x1 + "," + (int)y1 + "," + (int)(x1 + w) + "," + (int)(y1 + h);
		}
		sb.append(getStgLinkHtml(egp.getLink(), "rect", coordx,
				egp.getLinkTarget(), egs.getName(), category,
				getDispValue(egs), egs.getTips()));
	}

	/**
	 * ��ָ������������������ݣ���д�뻺��sb����������
	 * @param x1 ������
	 * @param y1 ������
	 * @param w ���
	 * @param h �߶�
	 * @param sb html����
	 * @param category ����ֵ
	 * @param egs ϵ��ֵ
	 */
	public void htmlLink2(double x1, double y1, double w, double h, StringBuffer sb,
			Object category, ExtGraphSery egs) {
		// �����Ӵ���
		if (disableLink() || sb==null) {
			return;
		}
		int minimum = 10;
		if (w < minimum) {
			w = minimum;
		}
		if (h < minimum) {
			h = minimum;
		}
		String coordx;
		if (isSVG()) {
			if (!StringUtils.isValidString(egp.getLink())) {
				return;
			}
			coordx = "x=\"" + (int)x1 + "\" y=\"" + (int)y1 + "\" width=\"" + (int)w
					+ "\" height=\"" + (int)h + "\"";
		} else {
			coordx = (int)x1 + "," + (int)y1 + "," + (int)(x1 + w) + "," + (int)(y1 + h);
		}
		sb.append(getStgLinkHtml(egp.getLink(), "rect", coordx,
				egp.getLinkTarget(), egs.getName(), category,
				getDispValue2(egs), egs.getTips()));
	}

	private boolean debuglink = true;

	/**
	 * ����һ�λ���״�ĳ����ӣ���������д�뻺��sb
	 * @param ddd ��Χ��
	 * @param sb html����
	 * @param category ����ֵ
	 * @param egs ϵ��ֵ
	 * @param ddd2 ��Χ��
	 */
	public void htmlLink(Arc2D.Double ddd, StringBuffer sb, Object category,
			ExtGraphSery egs, Arc2D.Double ddd2) {
		if (disableLink()) {
			return;
		}
		String shape = "";
		if (isSVG()) {
			if (!StringUtils.isValidString(egp.getLink())) {
				if(debuglink){
					Logger.debug("SVG graph must specify link to generate hyper links.");
					debuglink = false;
				}
				return;
			}
			shape = "polygon";
		} else {
			shape = "poly";
		}
		StringBuffer coords = new StringBuffer();
		int cx = (int) ddd.getCenterX();
		int cy = (int) ddd.getCenterY();
		double start = ddd.getAngleStart();
		double end = ddd.getAngleStart() + ddd.getAngleExtent();
		double delta = 5;
		if (isSVG()) {
			coords.append("points=\"");
		}
		String seperator = ",";
		if (isSVG()) {
			seperator = " ";
		}
		if (ddd2 == null) {
			coords.append(cx);
			coords.append(seperator + cy + ",");
		}

		coords.append((int) ddd.getStartPoint().getX());
		coords.append(seperator + (int) ddd.getStartPoint().getY());

		start += delta;
		while (start < end) {
			Arc2D.Double tmp = new Arc2D.Double(ddd.getBounds(), start, delta,
					Arc2D.PIE);
			coords.append("," + (int) tmp.getEndPoint().getX());
			coords.append(seperator + (int) tmp.getEndPoint().getY());
			start += delta;
		}

		coords.append("," + (int) ddd.getEndPoint().getX());
		coords.append(seperator + (int) ddd.getEndPoint().getY());

		if (ddd2 != null) { // С������÷�����
			coords.append("," + (int) ddd2.getEndPoint().getX());
			coords.append(seperator + (int) ddd2.getEndPoint().getY());

			double start2 = ddd2.getAngleStart();
			double end2 = ddd2.getAngleStart() + ddd2.getAngleExtent() - delta;

			while (end2 > start2) {
				Arc2D.Double tmp = new Arc2D.Double(ddd2.getBounds(), end2,
						delta, Arc2D.PIE);
				coords.append("," + (int) tmp.getStartPoint().getX());
				coords.append(seperator + (int) tmp.getStartPoint().getY());
				end2 -= delta;
			}
			coords.append("," + (int) ddd2.getStartPoint().getX());
			coords.append(seperator + (int) ddd2.getStartPoint().getY());
		}

		// ��ͼ��htmllink���ݲ���ʹ�ø�ʽ������Ϊ��ͼ������ʾ����Ϊ�ٷֱ�ʱ��link������Ϊԭֵ������ʹ�ö���ĸ�ʽ
		String dispVal = null;
		if (!egs.isNull()) {
			double scaledVal = getScaledValue(egs.getValue(), true);
			dispVal = getDispValue(scaledVal, egs.getTips());
		}
		if (isSVG()) {
			coords.append("\"");
		}
		sb.append(getStgLinkHtml(egp.getLink(), shape, coords.toString(),
				egp.getLinkTarget(), egs.getName(), category, dispVal,
				egs.getTips()));
	}

	/**
	 * ȡ����stroke
	 * @return stroke
	 */
	public Stroke getLineStroke() {
		return getLineStroke(egp.getLineStyle(), gp.getLineThick());
	}

	/**
	 * ���ݶ������ֱ��stroke
	 * @param style ����
	 * @param thick �ֶ�
	 * @return stroke
	 */
	public BasicStroke getLineStroke(int style, float thick) {
		if (thick == 0) {
			return null;
		}
		float dashes[] = { 2 };
		switch (style) {
		case IGraphProperty.LINE_SOLID:
			return new BasicStroke(thick);
		case IGraphProperty.LINE_DOT_DASH:
			dashes = new float[] { 2, 2, 6, 2 };
			break;
		case IGraphProperty.LINE_2DOT_DASH:
			dashes = new float[] { 2, 2, 2, 2, 6, 2 }; // һ�ڵĶ���
														// ʵ2,��2,ʵ2,��2,ʵ6,��2
			break;
		case IGraphProperty.LINE_LONG_DASH:
			dashes = new float[] { 5 };
			break;
		case IGraphProperty.LINE_SHORT_DASH:
			dashes = new float[] { 2 };
			break;
		default:
			return null;
		}
		return new BasicStroke(thick, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_BEVEL, thick, dashes, 0);
	}

	/**
	 * ���ھ���������Ϊ�������ģ��������ǩ�����ü�����Կվ�����ģ����Բ���ֵ���ۻ����
	 * �ú������������ֵ�����������ǩ��ʱ�����������°��տվ����㸲�Ǹ÷����Ա�֤���ߵ�����һ��
	 * 
	 * @param alarmVal
	 *            double ����ֵ
	 * @return Point null
	 */
	public Point2D.Double getCoorPoint(double alarmVal) {
		return null;
	}

	/**
	 * ���ƾ�����
	 */
	public void drawWarnLine() {
		ArrayList warnLines = egp.getAlarmLines();
		if (warnLines == null) {
			return;
		}

		double x1, y1, x2, y2;
		Stroke s = g.getStroke();
		int cc = warnLines.size();

		double topTickVal = ((Number) gp.coorValue.get(gp.tickNum))
				.doubleValue();
		double bottomTickVal = ((Number) gp.coorValue.get(0)).doubleValue();
		double tickArea = Math.abs(bottomTickVal - topTickVal);

		for (int i = 0; i < cc; i++) {
			ExtAlarmLine eal = (ExtAlarmLine) warnLines.get(i);
			byte lineType = eal.getLineType();
			float thick = eal.getLineThick();
			Stroke bs = getLineStroke(lineType, thick);

			if (bs == null) {
				continue;
			}
			g.setStroke(bs);

			int lineColor = eal.getColor();
			g.setColor(new Color(lineColor));

			double value = eal.getAlarmValue() / gp.scaleMark;
			y1 = gp.gRect2.y + (Math.abs(value - topTickVal) / tickArea)
					* gp.gRect2.height;

			x1 = gp.gRect2.x;
			x2 = x1 + gp.gRect2.width;
			y2 = y1;
			Utils.drawLine(g,x1, y1, x2, y2);
			if(!eal.isDrawAlarmValue()){
				continue;
			}
			String scoory = getFormattedValue(value);
			TR.setBounds(gp.GFV_YLABEL.getTextSize(scoory));
			x1 = gp.gRect1.x - gp.tickLen; // - TR.width
			y1 += gp.gRect1.y - gp.gRect2.y;
			Color tmp = gp.GFV_YLABEL.color;
			gp.GFV_YLABEL.color = new Color(lineColor);
			gp.GFV_YLABEL.outText(x1, y1, scoory, GraphFontView.TEXT_ON_LEFT);
			gp.GFV_YLABEL.color = tmp;
		}
		g.setStroke(s);
	}

	/**
	 * ���ƺ���������ľ�����
	 */
	public void drawWarnLineH() {
		ArrayList warnLines = egp.getAlarmLines();
		if (warnLines == null) {
			return;
		}
		double x1, y1, x2, y2;
		Stroke s = g.getStroke();
		int cc = warnLines.size();

		double rightTickVal = ((Number) gp.coorValue.get(gp.tickNum))
				.doubleValue();
		double leftTickVal = ((Number) gp.coorValue.get(0)).doubleValue();
		double tickArea = Math.abs(rightTickVal - leftTickVal);

		for (int i = 0; i < cc; i++) {
			ExtAlarmLine eal = (ExtAlarmLine) warnLines.get(i);
			int lineType = eal.getLineType();
			Stroke bs = getLineStroke(lineType, eal.getLineThick());
			if (bs == null) {
				continue;
			}
			g.setStroke(bs);
			int lineColor = eal.getColor();
			g.setColor(new Color(lineColor));

			double value = eal.getAlarmValue() / gp.scaleMark;
			x1 = gp.gRect2.x + (Math.abs(value - leftTickVal) / tickArea)
					* gp.gRect2.width;

			y1 = gp.gRect2.y;
			x2 = x1;
			y2 = y1 + gp.gRect2.height;
			Utils.drawLine(g,x1, y1, x2, y2);
			if(!eal.isDrawAlarmValue()){
				continue;
			}
			String scoory = getFormattedValue(value);
			TR.setBounds(gp.GFV_YLABEL.getTextSize(scoory));
			x1 -= gp.gRect1.x - gp.gRect2.x; // TR.width / 2;
			y1 = y2 + gp.tickLen; // + TR.height
			Color tmp = gp.GFV_XLABEL.color;
			gp.GFV_XLABEL.color = new Color(lineColor);
			gp.GFV_XLABEL.outText(x1, y1, scoory, GraphFontView.TEXT_ON_BOTTOM);
			gp.GFV_XLABEL.color = tmp;
		}
		g.setStroke(s);
	}

	/**
	 * ������ĸ������Է�װ��������ͼ�������ͼ��������
	 * @param gf ���嶨��
	 * @param text ����
	 * @param fontType ��������
	 * @return ������ͼ
	 */
	public GraphFontView getGraphFontView(GraphFont gf, String text,
			int fontType) {
		GraphFontView gfv = new GraphFontView(this);
		gfv.setAngle(gf.getAngle());
		gfv.setVertical(gf.isVerticalText());
		Color c = new Color(gf.getColor());
		if (gf.getColor() == 16777215) {
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
		}
		gfv.setColor(c);
		Font font = getFont(gf.getFamily(), gf.getSize(), fontType,
				gf.isAutoResize(), gf.isBold(), false, text);
		gfv.setFont(font);
		gfv.setText(text);
		gfv.setTextPosition(getTextDefaultPosition(fontType));
		return gfv;
	}

	protected Font getFont(String face, int size, int fontType,
			boolean autoSize, boolean isBold, boolean isItalic, String text) {
		int style = Font.PLAIN;
		if (isBold) {
			style = style + Font.BOLD;
		}
		if (isItalic) {
			style = style + Font.ITALIC;
		}
		if (face == null || face.trim().length() < 1) {
			face = "dialog";
		}

		if (!autoSize) {
			return new Font(face, style, size);
		}

		int len = 0;
		if (egp.getCategories().size() == 0) {
			len = 4;
		} else {
			if (gp.catNames.get(0) == null) {
				len = 0;
			} else {
				len = Variant.toString(gp.catNames.get(0)).length();
			}
		}
		switch (fontType) {
		case GraphFontView.FONT_TITLE:
			size = Math.round((gp.graphWidth < gp.graphHeight ? gp.graphWidth
					: gp.graphHeight) / 15);
			size = size > 17 ? 17 : size;
			break;
		case GraphFontView.FONT_LEGEND:
			size = Math.round((gp.graphWidth - gp.leftInset)
					/ (gp.serNames.size() == 0 ? 1 : gp.serNames.size())
					/ (len == 0 ? 1 : len));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_XLABEL:
			int tmp = 1;
			size = Math.round((gp.graphWidth - gp.leftInset)
					/ (gp.catNames.size() == 0 ? 1 : gp.catNames.size()) / tmp);
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_XTITLE:

			size = Math.round((gp.graphWidth - gp.leftInset - gp.rightInset)
					/ ((text == null || text.trim().length() == 0) ? 1 : text
							.length()));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_YLABEL:
			size = Math.round((gp.graphHeight - gp.topInset - gp.bottomInset)
					/ (gp.tickNum == 0 ? 1 : gp.tickNum));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_YTITLE:
			size = Math.round((gp.graphHeight - gp.topInset - gp.bottomInset)
					/ ((text == null || text.trim().length() == 0) ? 1 : text
							.length()));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_VALUE:
			break;
		}
		return new Font(face, style, size);
	}

	protected byte getTextDefaultPosition(int fontType) {
		byte pos = GraphFontView.TEXT_FIXED;
		switch (fontType) {
		case GraphFontView.FONT_TITLE:
			pos = GraphFontView.TEXT_ON_BOTTOM;
			break;
		case GraphFontView.FONT_LEGEND:
			pos = GraphFontView.TEXT_ON_RIGHT;
			break;
		case GraphFontView.FONT_XLABEL:
			pos = GraphFontView.TEXT_ON_BOTTOM;
			break;
		case GraphFontView.FONT_XTITLE:
			pos = GraphFontView.TEXT_ON_TOP;
			break;
		case GraphFontView.FONT_YLABEL:
			pos = GraphFontView.TEXT_ON_LEFT;
			break;
		case GraphFontView.FONT_YTITLE:
			pos = GraphFontView.TEXT_ON_RIGHT;
			break;
		case GraphFontView.FONT_VALUE:
			pos = GraphFontView.TEXT_ON_TOP;
			break;
		}
		return pos;
	}

	/**
	 * �����������ֵ��ǩ,�����ʱ������ͼ���򴴽�x���ʱ��̶�ֵ
	 */
	public void createCoorValue() {
		boolean isUserValue = egp.isUserSetYEndValue1();
		createCoorValue(!isUserValue);
	}

	// �ѻ�ͼ�����ֵ��Ȼû�����ã���Ϊ�����и����˴�����Ϊ�������ֵ
	private double getAutoInterval(double max) {
		if (max > 50) {
			return (10 * gp.coorScale);
		} else if (max > 5) {
			return (5 * gp.coorScale);
		}
		return (1 * gp.coorScale);
	}

	/**
	 * ��������̶�ֵ
	 * @param adjustMaxValue �����̶����ֵΪȡ��ֵ
	 */
	public void createCoorValue(boolean adjustMaxValue) {
		if (!egp.isStackedGraph(this)) {
			gp.maxPositive = gp.maxValue;
			gp.minNegative = gp.minValue;
		}

		double tmp;
		if (gp.maxValue < gp.minValue && adjustMaxValue) {
			tmp = gp.maxValue;
			gp.maxValue = gp.minValue;
			gp.minValue = tmp;
		}

		if (gp.maxValue != 0) {
			tmp = gp.maxValue * 0.1;
			if (tmp == gp.maxValue) {
				throw new RuntimeException("The max value is infinite.");
			}
		}
		if (gp.minValue != 0) {
			tmp = gp.minValue * 10;
			if (tmp == gp.minValue) {
				throw new RuntimeException("The min value is infinite.");
			}
		}

		if (egp.is2YGraph()) {
			boolean isUserValue2 = egp.isUserSetYEndValue2();
			createCoorValue2(!isUserValue2);
		}

		double doubleTmp = 0.0;
		gp.coorScale = 1;

		if (gp.minValue >= 0.0) {
			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			while (gp.maxValue > 100.00) {
				gp.coorScale *= 10;
				gp.maxValue *= 0.1;
			}

			while (gp.maxValue <= 1.0) {
				gp.coorScale *= 0.1;
				gp.maxValue *= 10;
			}
			if (gp.interval == 0.0) {
				gp.interval = getAutoInterval(gp.maxValue);
			}
			double veryMin = 0.000001;// ���û��趨�ļ��ֵ��Ȼû�����������ܽӽ�ʱ�����ٵ���
			if (gp.maxValue % (gp.interval / gp.coorScale) > veryMin
					&& adjustMaxValue) {
				gp.maxValue = gp.maxValue - gp.maxValue
						% (gp.interval / gp.coorScale)
						+ (gp.interval / gp.coorScale);
			}
			gp.tickNum = (int) (gp.maxValue / (gp.interval / gp.coorScale));

			if (gp.minTicknum > 0 && gp.tickNum < gp.minTicknum) {
				gp.tickNum = gp.minTicknum;
				if (gp.maxValue % gp.tickNum > 0 && adjustMaxValue) {
					gp.maxValue = gp.maxValue - gp.maxValue % gp.tickNum
							+ gp.tickNum;
				}
			}
			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			for (int i = 0; i <= gp.tickNum; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * gp.maxValue / gp.tickNum;
				} else {
					doubleTmp = i * gp.maxValue * (gp.coorScale / gp.scaleMark)
							/ gp.tickNum;
				}
				gp.coorValue.add(i, new Double(doubleTmp + gp.baseValue));
			}
		} else if (gp.maxValue <= 0) {
			doubleTmp = gp.maxValue;
			gp.maxValue = Math.abs(gp.minValue);
			gp.minValue = Math.abs(doubleTmp);
			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			while (gp.maxValue > 100.00) {
				gp.coorScale *= 10;
				gp.maxValue *= 0.1;
			}
			while (gp.maxValue < 1.0) {
				gp.coorScale *= 0.1;
				gp.maxValue *= 10;
			}
			if (gp.interval == 0.0) {
				gp.interval = getAutoInterval(gp.maxValue);
			}
			if (gp.maxValue % (gp.interval / gp.coorScale) != 0
					&& adjustMaxValue) {
				gp.maxValue = gp.maxValue - gp.maxValue
						% (gp.interval / gp.coorScale)
						+ (gp.interval / gp.coorScale);
			}
			gp.tickNum = (int) (gp.maxValue / (gp.interval / gp.coorScale));
			if (gp.minTicknum > 0 && gp.tickNum < gp.minTicknum) {
				gp.tickNum = gp.minTicknum;
				if (gp.maxValue % gp.tickNum > 0 && adjustMaxValue) {
					gp.maxValue = gp.maxValue - gp.maxValue % gp.tickNum
							+ gp.tickNum;
				}
			}

			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			for (int i = 0; i <= gp.tickNum; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = -(gp.tickNum - i) * gp.maxValue / gp.tickNum;
				} else {
					doubleTmp = -(gp.tickNum - i) * gp.maxValue
							* (gp.coorScale / gp.scaleMark) / gp.tickNum;
				}
				gp.coorValue.add(i, new Double(doubleTmp + gp.baseValue));
			}
		} else {

			double absMax = gp.maxValue;
			double absMin = Math.abs(gp.minValue);
			if (gp.maxValue < Math.abs(gp.minValue)) {
				absMin = absMax;
				absMax = Math.abs(gp.minValue);
			}

			if (absMax == 0.0) {
				absMax = 1.0;
			}
			while (absMax > 100.00) {
				gp.coorScale *= 10;
				absMax *= 0.1;
			}
			while (absMax < 1.0) {
				gp.coorScale *= 0.1;
				absMax *= 10;
			}
			if (gp.interval == 0.0) {
				gp.interval = getAutoInterval(absMax);
			}
			if (absMax % (gp.interval / gp.coorScale) != 0 && adjustMaxValue) {
				absMax = absMax - absMax % (gp.interval / gp.coorScale)
						+ (gp.interval / gp.coorScale);
			}
			gp.tickNum = (int) (absMax / (gp.interval / gp.coorScale));

			if (gp.minTicknum > 0 && gp.tickNum < gp.minTicknum) {
				gp.tickNum = gp.minTicknum;
				if (absMax % gp.tickNum > 0 && adjustMaxValue) {
					absMax = absMax - absMax % gp.tickNum + gp.tickNum;
				}
			}

			if (gp.scaleMark != IGraphProperty.UNIT_AUTO) {
				absMin = absMin * (gp.scaleMark / gp.coorScale);
			}
			boolean addPositive = false;
			boolean addNegative = false;
			int intTmp = gp.tickNum;
			gp.coorValue.add(new Double(0 + gp.baseValue));
			for (int i = 1; i <= intTmp; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * absMax / intTmp;
				} else {
					doubleTmp = i * absMax * (gp.coorScale / gp.scaleMark)
							/ intTmp;
				}
				if (doubleTmp < gp.maxValue) {
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				} else if (!addPositive) {
					addPositive = true;
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				}
				doubleTmp *= -1f;
				if (doubleTmp > gp.minValue) {
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				} else if (!addNegative) {
					addNegative = true;
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				}
			}
			Collections.sort(gp.coorValue);
//			GM.sort(gp.coorValue, true);
			gp.tickNum = gp.coorValue.size() - 1;
			gp.maxValue = Math
					.abs(((Number) gp.coorValue.get(0)).doubleValue())
					+ Math.abs(((Number) gp.coorValue.get(gp.tickNum))
							.doubleValue());
			gp.maxValue *= (gp.scaleMark / gp.coorScale);
		}
		gp.minValue = 0;

		boolean isChinese = GCBase.LANGUAGE == GCBase.ASIAN_CHINESE;
		if (isChinese) {
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale != 1) {
				if (gp.coorScale > 1000) {
					gp.GFV_YTITLE.text = gp.GFV_YTITLE.text + "("
							+ gp.xToChinese(gp.coorScale) + ")";
				} else {
					gp.GFV_YTITLE.text = gp.GFV_YTITLE.text + "(������1:"
							+ gp.coorScale + ")";
				}
			} else if (gp.scaleMark != IGraphProperty.UNIT_ORIGIN) {
				if (gp.scaleMark == IGraphProperty.UNIT_THOUSAND) {
					gp.GFV_YTITLE.text += "(ǧ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10THOUSAND) {
					gp.GFV_YTITLE.text += "(��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_MILLION) {
					gp.GFV_YTITLE.text += "(����)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10MILLION) {
					gp.GFV_YTITLE.text += "(ǧ��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_100MILLION) {
					gp.GFV_YTITLE.text += "(��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_BILLION) {
					gp.GFV_YTITLE.text += "(ʮ��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_001) {
					gp.GFV_YTITLE.text += "(�ٷ�֮һ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0001) {
					gp.GFV_YTITLE.text += "(ǧ��֮һ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_00001) {
					gp.GFV_YTITLE.text += "(���֮һ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0000001) {
					gp.GFV_YTITLE.text += "(�����֮һ)";
				}
			}
		} else { // English
			double unit = 1;
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale != 1) {
				unit = gp.coorScale;
			} else {
				unit = gp.scaleMark;
			}
			if (unit != 1) {
				gp.GFV_YTITLE.text += "(" + getFormattedValue(unit, "#.#E0")
						+ ")";
			}
		}
	}

	public void createCoorValue2(boolean adjustMaxValue) {
		double doubleTmp = 0.0;
		gp.coorScale2 = 1;

		if (gp.minValue2 >= 0.0) {
			if (gp.maxValue2 == 0.0 && adjustMaxValue) {
				gp.maxValue2 = 1.0;

			}
			while (gp.maxValue2 > 100.00) {
				gp.coorScale2 *= 10;
				gp.maxValue2 *= 0.1;
			}

			while (gp.maxValue2 < 1.0) {
				gp.coorScale2 *= 0.1;
				gp.maxValue2 *= 10;
			}
			if (gp.interval2 == 0.0) {
				if (gp.maxValue2 > 50) {
					gp.interval2 = (10 * gp.coorScale2);
				} else {
					gp.interval2 = (5 * gp.coorScale2);
				}
			}
			if (gp.maxValue2 % (gp.interval2 / gp.coorScale2) != 0
					&& adjustMaxValue) {
				gp.maxValue2 = gp.maxValue2 - gp.maxValue2
						% (gp.interval2 / gp.coorScale2)
						+ (gp.interval2 / gp.coorScale2);
			}
			gp.tickNum2 = (int) (gp.maxValue2 / (gp.interval2 / gp.coorScale2));

			if (gp.minTicknum2 > 0 && gp.tickNum2 < gp.minTicknum2) {
				gp.tickNum2 = gp.minTicknum2;
				if (gp.maxValue2 % gp.tickNum2 > 0 && adjustMaxValue) {
					gp.maxValue2 = gp.maxValue2 - gp.maxValue2 % gp.tickNum2
							+ gp.tickNum2;
				}
			}
			if (gp.maxValue2 == 0.0 && adjustMaxValue) {
				gp.maxValue2 = 1.0;

			}
			for (int i = 0; i <= gp.tickNum2; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * gp.maxValue2 / gp.tickNum2;
				} else {
					doubleTmp = i * gp.maxValue2
							* (gp.coorScale2 / gp.scaleMark) / gp.tickNum2;
				}

				gp.coorValue2.add(i, new Double(doubleTmp + gp.baseValue2));
			}
		} else if (gp.maxValue2 <= 0) {
			doubleTmp = gp.maxValue2;
			gp.maxValue2 = Math.abs(gp.minValue2);
			gp.minValue2 = Math.abs(doubleTmp);
			if (gp.maxValue2 == 0.0) {
				gp.maxValue2 = 1.0;

			}
			while (gp.maxValue2 > 100.00) {
				gp.coorScale2 *= 10;
				gp.maxValue2 *= 0.1;
			}
			while (gp.maxValue2 < 1.0) {
				gp.coorScale2 *= 0.1;
				gp.maxValue2 *= 10;
			}
			if (gp.interval2 == 0.0) {
				if (gp.maxValue2 > 50) {
					gp.interval2 = (10 * gp.coorScale2);
				} else {
					gp.interval2 = (5 * gp.coorScale2);
				}
			}
			if (gp.maxValue2 % (gp.interval2 / gp.coorScale2) != 0
					&& adjustMaxValue) {
				gp.maxValue2 = gp.maxValue2 - gp.maxValue2
						% (gp.interval2 / gp.coorScale2)
						+ (gp.interval2 / gp.coorScale2);
			}
			gp.tickNum2 = (int) (gp.maxValue2 / (gp.interval2 / gp.coorScale2));
			if (gp.minTicknum2 > 0 && gp.tickNum2 < gp.minTicknum2) {
				gp.tickNum2 = gp.minTicknum2;
				if (gp.maxValue2 % gp.tickNum2 > 0 && adjustMaxValue) {
					gp.maxValue2 = gp.maxValue2 - gp.maxValue2 % gp.tickNum2
							+ gp.tickNum2;
				}
			}

			if (gp.maxValue2 == 0.0 && adjustMaxValue) {
				gp.maxValue2 = 1.0;
			}
			for (int i = 0; i <= gp.tickNum2; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = -(gp.tickNum2 - i) * gp.maxValue2 / gp.tickNum2;
				} else {
					doubleTmp = -(gp.tickNum2 - i) * gp.maxValue2
							* (gp.coorScale2 / gp.scaleMark) / gp.tickNum2;
				}
				gp.coorValue2.add(i, new Double(doubleTmp));
			}
		} else {
			double absMax = gp.maxValue2;
			double absMin = Math.abs(gp.minValue2);
			if (gp.maxValue2 < Math.abs(gp.minValue2)) {
				absMin = absMax;
				absMax = Math.abs(gp.minValue2);
			}

			if (absMax == 0.0) {
				absMax = 1.0;
			}
			while (absMax > 100.00) {
				gp.coorScale2 *= 10;
				absMax *= 0.1;
			}
			while (absMax < 1.0) {
				gp.coorScale2 *= 0.1;
				absMax *= 10;
			}
			if (gp.interval2 == 0.0) {
				if (absMax > 50) {
					gp.interval2 = (10 * gp.coorScale2);
				} else {
					gp.interval2 = (5 * gp.coorScale2);
				}
			}
			if (absMax % (gp.interval2 / gp.coorScale2) != 0 && adjustMaxValue) {
				absMax = absMax - absMax % (gp.interval2 / gp.coorScale2)
						+ (gp.interval2 / gp.coorScale2);
			}
			gp.tickNum2 = (int) (absMax / (gp.interval2 / gp.coorScale2));

			if (gp.minTicknum > 0 && gp.tickNum2 < gp.minTicknum) {
				gp.tickNum2 = gp.minTicknum;
				if (absMax % gp.tickNum2 > 0 && adjustMaxValue) {
					absMax = absMax - absMax % gp.tickNum2 + gp.tickNum2;
				}
			}

			if (gp.scaleMark != IGraphProperty.UNIT_AUTO) {
				absMin = absMin * (gp.scaleMark / gp.coorScale2);
			}
			boolean addPositive = false;
			boolean addNegative = false;
			int intTmp = gp.tickNum2;
			gp.coorValue2.add(new Double(0 + gp.baseValue2));
			for (int i = 1; i <= intTmp; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * absMax / intTmp;
				} else {
					doubleTmp = i * absMax * (gp.coorScale2 / gp.scaleMark)
							/ intTmp;
				}
				if (doubleTmp < gp.maxValue2) {
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				} else if (!addPositive) {
					addPositive = true;
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				}
				doubleTmp *= -1f;
				if (doubleTmp > gp.minValue2) {
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				} else if (!addNegative) {
					addNegative = true;
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				}
			}
			Collections.sort(gp.coorValue2);
//			GM.sort(gp.coorValue2, true);
			gp.tickNum2 = gp.coorValue2.size() - 1;
			gp.maxValue2 = Math.abs(((Number) gp.coorValue2.get(0))
					.doubleValue())
					+ Math.abs(((Number) gp.coorValue2.get(gp.tickNum2))
							.doubleValue());
			gp.maxValue2 *= (gp.scaleMark / gp.coorScale2);
		}
		gp.minValue2 = 0;
		boolean ISCHINESE = (Locale.getDefault().equals(Locale.CHINA)
				|| Locale.getDefault().equals(Locale.CHINESE) || Locale
				.getDefault().equals(Locale.SIMPLIFIED_CHINESE));
		if (ISCHINESE) {
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale2 != 1) {
				if (gp.coorScale2 > 1000) {
					gp.GFV_YTITLE.text2 = gp.GFV_YTITLE.text2 + "("
							+ gp.xToChinese(gp.coorScale2) + ")";
				} else {
					gp.GFV_YTITLE.text2 = gp.GFV_YTITLE.text2 + "(������1:"
							+ gp.coorScale2 + ")";
				}
			} else if (gp.scaleMark != IGraphProperty.UNIT_ORIGIN) {
				if (gp.scaleMark == IGraphProperty.UNIT_THOUSAND) {
					gp.GFV_YTITLE.text2 += "(ǧ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10THOUSAND) {
					gp.GFV_YTITLE.text2 += "(��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_MILLION) {
					gp.GFV_YTITLE.text2 += "(����)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10MILLION) {
					gp.GFV_YTITLE.text2 += "(ǧ��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_BILLION) {
					gp.GFV_YTITLE.text2 += "(��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_100MILLION) {
					gp.GFV_YTITLE.text2 += "(ʮ��)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_001) {
					gp.GFV_YTITLE.text2 += "(�ٷ�֮һ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0001) {
					gp.GFV_YTITLE.text2 += "(ǧ��֮һ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_00001) {
					gp.GFV_YTITLE.text2 += "(���֮һ)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0000001) {
					gp.GFV_YTITLE.text2 += "(�����֮һ)";
				}
			}
		} else { // English
			double unit = 1;
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale != 1) {
				unit = gp.coorScale;
			} else {
				unit = gp.scaleMark;
			}
			if (unit != 1) {
				gp.GFV_YTITLE.text += "(" + getFormattedValue(unit, "#.#E0")
						+ ")";
			}
		}
	}

	String getFormattedCatVal(Object catVal) {
		return Variant.toString(catVal);
	}

	protected float getDataTableX(int catIndex) {
		float axisLen = gp.graphWidth - gp.leftInset - gp.rightInset;
		float catWidth = axisLen / gp.catNum;
		float pos = gp.leftInset + (catIndex + 1) * catWidth - catWidth / 2;
		return pos;
	}

	/**
	 * �������ݱ�
	 * Ŀǰֻ��������״ͼ���·������ݱ�
	 */
	public void drawDataTable() {
		float axisWidth = gp.graphWidth - gp.leftInset - gp.rightInset;
		CellSet cs = getDataTable(axisWidth);
		float tableH = getDataTableHeight(cs);

		Utils.setGraphAntiAliasingOff(g);

		IColCell cc1 = cs.getColCell(1);
		float col1W = cc1.getWidth();
		float x = gp.leftInset - col1W;
		float y = gp.graphHeight - gp.bottomInset - tableH;
		// draw table
		int rows = cs.getRowCount();
		int cols = cs.getColCount();
		for (int r = 1; r <= rows; r++) {
			IRowCell rc = cs.getRowCell(r);
			float h = rc.getHeight();
			for (int c = 1; c <= cols; c++) {
				if (c == 1) {
					x = gp.leftInset - col1W;
				}
				IColCell cc = cs.getColCell(c);
				INormalCell nc = cs.getCell(r, c);
				float w = cc.getWidth();
				if (r == 1 && c == 1) {
				} else {
					Color cr = egp.getAxisColor(IGraphProperty.AXIS_BOTTOM);
					Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);
					drawShape(rect, cr);
					boolean isCenter = true;
					if(r == 1 || c == 1){
						isCenter =	true;
					}else{
						isCenter =	gp.isDataCenter;
					}
					 
					Font font;
					Color color;
					if (isCenter) {
						font = gp.GFV_XLABEL.font;
						color = gp.GFV_XLABEL.color;
					} else {
						font = gp.GFV_VALUE.font;
						color = gp.GFV_VALUE.color;
					}
					drawText(nc.getExpString(), (int) x, (int) y, (int) w,
							(int) h, isCenter, font, color);
				}
				x += w;
			}
			y += h;
		}

		gp.bottomInset += tableH;
		Utils.setGraphAntiAliasingOn(g);
	}

	private void drawText(String text, int x, int y, int w, int h,
			boolean isCenter, Font font, Color c) {
		if (text == null || text.trim().length() == 0) {
			return;
		}

		FontMetrics fm = g.getFontMetrics(font);
		int fw = 0;// stringWidth(fm, text);
		int ascent = fm.getAscent();
		int height = fm.getHeight();
		ArrayList<String> al = DrawStringUtils2.wrapString(text, fm, w, false);
		int lineH = DrawStringUtils2.getTextRowHeight(fm);
		if (al.size() < 2) {
			if (lineH > h) {
				lineH = h;
			}
		}
		int yy = y;
		yy = y + (h - lineH * al.size()) / 2;
		if (yy < y) {
			yy = y;
		}
		for (int i = 0; i < al.size(); i++) {
			if (i > 0 && yy + lineH > y + h) { // ��һ�����ǻ��ƣ�����������ڿ��ⲻ���������ڸǱ�ĸ�������
				break;
			}

			String wrapedText = (String) al.get(i);
			fw = fm.stringWidth(wrapedText);
			int x1 = x;
			if (isCenter) {
				x1 = x + (w - fw) / 2;
			} else {
				x1 = x + w - fw;
			}
			int y1 = yy + (lineH - height) / 2 + ascent;
			g.setColor(c);
			g.setFont(font);
			g.drawString(wrapedText, x1, y1);
			yy += lineH;
		}

	}

	/**
	 * ������������λ��
	 * */
	public void adjustCoorInset() {
		int maxValW = 0;
		int maxValH = 0;
		int maxCatW = 0;
		int maxCatH = 0;
		int intTmp = 0;

		for (int i = 0; i < gp.tickNum; i++) {
			Object coory = gp.coorValue.get(i);
			String scoory = Variant.toString(coory);
			if (coory instanceof Number && gp.dataMarkFormat != null
					&& gp.dataMarkFormat.trim().length() > 0) {
				DecimalFormatSymbols dfs = new DecimalFormatSymbols(
						Locale.getDefault());
				DecimalFormat df = new DecimalFormat(gp.dataMarkFormat, dfs);
				scoory = df.format(((Number) coory).doubleValue());
			}
			TR = gp.GFV_YLABEL.getTextSize(scoory);
			intTmp = TR.width;
			if (maxValW < intTmp) {
				maxValW = intTmp;
			}
			intTmp = TR.height;
			if (maxValH < intTmp) {
				maxValH = intTmp;
			}
		}

		for (int j = 0; j < gp.catNum; j++) {
			Object o = gp.catNames.get(j);
			TR = gp.GFV_XLABEL.getTextSize(getFormattedCatVal(o));
			intTmp = TR.width;
			if (maxCatW < intTmp) {
				maxCatW = intTmp;
			}
			intTmp = TR.height;
			if (maxCatH < intTmp) {
				maxCatH = intTmp;
			}
		}

		if (egp.isBarGraph(this)) {
			gp.leftInset += maxCatW + 4;
			gp.bottomInset += maxValH + 4;
		} else {
			gp.leftInset += maxValW + 4;
			if (!gp.isDrawTable) {// Ŀǰ�����ݱ��֧����X���ϻ���
				gp.bottomInset += maxCatH + 4;
			}

			if (egp.is2YGraph()) {
				for (int i = 0; i < gp.tickNum2; i++) {
					Object coory = gp.coorValue2.get(i);
					String scoory = Variant.toString(coory);
					if (coory instanceof Number && gp.dataMarkFormat != null
							&& gp.dataMarkFormat.trim().length() > 0) {
						DecimalFormatSymbols dfs = new DecimalFormatSymbols(
								Locale.getDefault());
						DecimalFormat df = new DecimalFormat(gp.dataMarkFormat,
								dfs);
						scoory = df.format(((Number) coory).doubleValue());
					}
					TR = gp.GFV_YLABEL.getTextSize(scoory);
					intTmp = TR.width;
					if (maxValW < intTmp) {
						maxValW = intTmp;
					}
					intTmp = TR.height;
					if (maxValH < intTmp) {
						maxValH = intTmp;
					}
				}
				gp.rightInset += maxValW + 4;
			}

			if (gp.isDrawTable) {// Ŀǰ�����ݱ��֧����X���ϻ���
				drawDataTable();
			}
		}
	}

	/**
	 * ��ʼ��ͳ��ͼ�߾�
	 */
	public void initGraphInset() {
		gp.leftInset = gp.leftMargin;
		gp.topInset = gp.topMargin;
		gp.rightInset = gp.rightMargin;
		gp.bottomInset = gp.bottomMargin;
	}

	/**
	 * ָ����ʽ����ʽ������
	 * 
	 * @param value
	 *            double Ҫ��ʽ������ֵ
	 * @param fmt
	 *            String ��ʽ
	 * @return String ��ʽ����Ĵ�
	 */
	public String getFormattedValue(double value, String fmt) {
		double tmp = value;
		String sVal = new Double(tmp).toString();
		if (StringUtils.isValidString(fmt)) {
			DecimalFormatSymbols dfs = new DecimalFormatSymbols(
					Locale.getDefault());
			DecimalFormat df = new DecimalFormat(fmt, dfs);
			sVal = df.format(tmp);
		}
		return sVal;
	}

	/**
	 * ʹ�����õĸ�ʽ����ʽ����ֵ
	 * @param value
	 *            double��Ҫ��ʽ������ֵ
	 * @return String����ʽ�����Ĵ�
	 */
	public String getFormattedValue(double value) {
		return getFormattedValue(value, gp.dataMarkFormat);
	}

	/**
	 * ��ԭֵ���ݵ�λ�������ֵ
	 * @param orginalVal ԭʼ��ֵ
	 * @param isFirstAxis �Ƿ��һ����ֵ
	 * @return �����������ֵ
	 */
	public double getScaledValue(double orginalVal, boolean isFirstAxis) {
		double scaledVal;
		if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
			double coorScale = isFirstAxis ? gp.coorScale : gp.coorScale2;
			scaledVal = orginalVal / coorScale;
		} else {
			scaledVal = orginalVal / gp.scaleMark;
		}
		return scaledVal;
	}

	/**
	 * ��ȡϵ�ж����tip��ʾֵ
	 * @param egs ϵ��
	 * @return ��ʾֵ
	 */
	public String getDispValue(ExtGraphSery egs) {
		return getDispValue(null, egs, 0);
	}

	/**
	 * ��ȡ�����Լ�ϵ�еĳ�����tip��ʾֵ
	 * @param egc ����
	 * @param egs ϵ��
	 * @param serNum ϵ�и���
	 * @return ��ʽ����ʾֵ
	 */
	public String getDispValue(ExtGraphCategory egc, ExtGraphSery egs,
			int serNum) {
		if (egs.isNull()) {
			return null;
		}
		double scaledVal = getScaledValue(egs.getValue(), true);
		String txt = getDispValue(scaledVal, egs.getTips());
		if (egc != null
				&& gp.dispValueType == IGraphProperty.DISPDATA_NAME_VALUE) {
			txt = getDispName(egc, egs, serNum) + "," + txt;
		}
		return txt;
	}

	/**
	 * ��ȡϵ�ж����tip��ʾֵ��˫��ͼ��2����
	 * @param egs ϵ��
	 * @return ��ʾֵ
	 */
	public String getDispValue2(ExtGraphSery egs) {
		return getDispValue2(null, egs, 0);
	}

	/**
	 * ��ȡ�����Լ�ϵ�еĳ�����tip��ʾֵ��˫��ͼ��2��������
	 * @param egc ����
	 * @param egs ϵ��
	 * @param serNum ϵ�и���
	 * @return ��ʽ����ʾֵ
	 */
	public String getDispValue2(ExtGraphCategory egc, ExtGraphSery egs,
			int serNum) {
		if (egs.isNull()) {
			return null;
		}
		double scaledVal = getScaledValue(egs.getValue(), false);
		String txt = getDispValue2(scaledVal, egs.getTips());
		if (egc != null
				&& gp.dispValueType2 == IGraphProperty.DISPDATA_NAME_VALUE) {
			txt = getDispName(egc, egs, serNum) + "," + txt;
		}
		return txt;
	}
	
	public String getDispValue2(double value, String title) {
		if (gp.dispValueType2 == IGraphProperty.DISPDATA_TITLE) {
			return title;
		}
		String txt = getFormattedValue(value, gp.dataMarkFormat2);
		return txt;
	}

	/**
	 * �������ý���ʾֵ��ʾΪtitle����value�ĸ�ʽ��ֵ
	 * @param value ԭʼ��ֵ
	 * @param title ȱʡ��ʾ����
	 * @param fmt ��ʽ
	 * @return string ��ʽ����ʾֵ
	 */
	public String getDispValue(double value, String title) {
		if (gp.dispValueType == IGraphProperty.DISPDATA_TITLE) {
			return title;
		}
		String txt = getFormattedValue(value, gp.dataMarkFormat);
		return txt;
	}

	/**
	 * ����������ֱ��
	 * @param b ��ʼ��
	 * @param e ������
	 */
	public void drawLine(Point2D.Double b, Point2D.Double e) {
		drawLine(b, e, egp.isDrawShade());
	}

	/**
	 * ����������ֱ��
	 * @param b ��ʼ��
	 * @param e ������
	 * @param drawShade �Ƿ������Ӱ
	 */
	public void drawLine(Point2D.Double b, Point2D.Double e, boolean drawShade) {
		if (b == null || e == null) {
			return;
		}
		Stroke old = g.getStroke();
		Stroke stroke = getLineStroke();
		if (stroke != null) {
			g.setStroke(stroke);
			if (drawShade) {
				Color c = g.getColor();
				g.setColor(Color.lightGray);
				Utils.drawLine(g,b.x + SHADE_SPAN, b.y + SHADE_SPAN,
						e.x + SHADE_SPAN, e.y + SHADE_SPAN);
				g.setColor(c);
			}
			Utils.drawLine(g,b.x, b.y, e.x, e.y);
		}
		g.setStroke(old);
	}

	/**
	 * ��ǰ�����Ƿ�Ϊ���Ļ���
	 * @return ����Ƿ���true�����򷵻�false
	 */
	public static boolean isChinese() {
		return (Locale.getDefault().equals(Locale.CHINA)
				|| Locale.getDefault().equals(Locale.CHINESE) || Locale
				.getDefault().equals(Locale.SIMPLIFIED_CHINESE));
	}

	/**
	 * ��ȡ�����ϵ�е���ʾ��
	 * @param egc ����
	 * @param egs ϵ��
	 * @param serNum ϵ�����
	 * @return ��ʾ��
	 */
	public static String getDispName(ExtGraphCategory egc, ExtGraphSery egs,
			int serNum) {
		if (serNum == 1) {
			return egc.getNameString();
		}
		return egs.getName();
	}

	/**
	 * �������ݱ�ĸ߶�
	 * @param cs ���ݱ���������
	 * @return �߶�
	 */
	public float getDataTableHeight(CellSet cs) {
		int rows = cs.getRowCount();
		float total = 0;
		for (int r = 1; r <= rows; r++) {
			IRowCell rc = cs.getRowCell(r);
			total += rc.getHeight();
		}
		return total;
	}

	/**
	 * ��ȡ���ֺ��иߣ��п��ĵ����ݱ�����
	 * @param axisLen ��ĳ���
	 * @return ���ݱ�����
	 */
	public CellSet getDataTable(double axisLen) {
		int rows = gp.serNum + 1;
		int cols = gp.catNum + 1;

		if (egp.is2YGraph() || egp.category2 != null) {
			rows += gp.serNum2;
		}
		PgmCellSet cs = new PgmCellSet(rows, cols);
		double catWidth = axisLen / gp.catNum;
		IColCell cc = cs.getColCell(1);
		cc.setWidth(gp.leftInset-5);

		ArrayList cats = egp.categories;
		ArrayList cats2 = egp.category2;
		for (int i = 0; i < gp.catNum; i++) {
			cc = cs.getColCell(i + 2);
			cc.setWidth((float) catWidth);

			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			String value = egc.getNameString();
			INormalCell nc = cs.getCell(1, i + 2);
			nc.setExpString(value);

			for (int j = 0; j < gp.serNum; j++) {
				String serName = gp.serNames.get(j).toString();
				if (i == 0) {
					nc = cs.getCell(j + 2, 1);
					nc.setExpString(serName);
				}
				ExtGraphSery egs = egc.getExtGraphSery(serName);
				if (egs.isNull()) {
					continue;
				}
				double scaledVal = getScaledValue(egs.getValue(), true);
				String txt = getFormattedValue(scaledVal, gp.dataMarkFormat);
				nc = cs.getCell(j + 2, i + 2);
				nc.setExpString(txt);
			}

			if (egp.is2YGraph() || cats2 != null) {
				ExtGraphCategory egc2 = (ExtGraphCategory) cats2.get(i);
				for (int j = 0; j < gp.serNum2; j++) {
					String serName = gp.serNames2.get(j).toString();
					if (i == 0) {
						nc = cs.getCell(gp.serNum + j + 2, 1);
						nc.setExpString(serName);
					}
					ExtGraphSery egs = egc2.getExtGraphSery(serName);
					if (egs.isNull()) {
						continue;
					}
					double scaledVal = getScaledValue(egs.getValue(), true);
					String txt = getFormattedValue(scaledVal,
							gp.dataMarkFormat2);
					nc = cs.getCell(gp.serNum + j + 2, i + 2);
					nc.setExpString(txt);
				}
			}
		}
		// ���ݱ�ķ����Լ�ϵ�����Ʋ��� x���ǩ���壬�Զ��Ŵ��и�
		for (int r = 1; r <= rows; r++) {
			IRowCell rc = cs.getRowCell(r);
			float rowH = rc.getHeight();
			for (int c = 1; c <= cols; c++) {
				Font font;
				if (r == 1 || c == 1) {
					font = gp.GFV_XLABEL.font;
				} else {
					font = gp.GFV_VALUE.font;
				}
				cc = cs.getColCell(c);
				float w = cc.getWidth();
				INormalCell nc = cs.getCell(r, c);
				float cellH = getCellH(nc, font, w);
				if (cellH > rowH) {
					rowH = cellH;
				}
			}
			rc.setHeight(rowH);
		}
		return cs;
	}

	private float getCellH(INormalCell nc, Font font, float w) {
		FontMetrics fm = g.getFontMetrics(font);
		String text = nc.getExpString();
		ArrayList<String> al = DrawStringUtils2.wrapString(text, fm, w, false);
		int lineH = DrawStringUtils2.getTextRowHeight(fm);
		if (al.size() < 2) {
			return lineH;
		}
		float totalH = lineH * al.size();
		totalH += 2;
		return totalH;
	}

}
