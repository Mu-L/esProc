package com.scudata.chart.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.*;

import com.scudata.app.common.AppUtil;
import com.scudata.cellset.IStyle;
import com.scudata.cellset.graph.PublicProperty;
import com.scudata.cellset.graph.StatisticGraph;
import com.scudata.cellset.graph.config.AlarmLine;
import com.scudata.cellset.graph.config.GraphFont;
import com.scudata.cellset.graph.config.GraphFonts;
import com.scudata.cellset.graph.config.IGraphProperty;
import com.scudata.cellset.graph.draw.DrawBase;
import com.scudata.cellset.graph.draw.ExtGraphCategory;
import com.scudata.cellset.graph.draw.ExtGraphProperty;
import com.scudata.cellset.graph.draw.ExtGraphSery;
import com.scudata.chart.Consts;
import com.scudata.chart.Engine;
import com.scudata.chart.ObjectElement;
import com.scudata.chart.Utils;
import com.scudata.chart.edit.*;
import com.scudata.chart.element.EnumAxis;
import com.scudata.common.StringUtils;
import com.scudata.dm.*;
import com.scudata.common.control.BorderStyle;
import com.scudata.common.control.CellBorder;
import com.scudata.util.Variant;

/**
 * ͼ��ͼԪ����
 * ����ͼ�ε�ͨ������
 * @author Joancy
 *
 */
public class GraphBase extends ObjectElement{
	// �����ϵ�е�ֵ����ʽ[v1,v2,...,vn]��v
	public Sequence categories = null;
	// ��ֵ���У���ʽ[w1,w2,...,wn]��w
	public Sequence values = null;

	/** ��Χ�߿� */
	public int borderStyle = IStyle.LINE_SOLID;
	public float borderWidth = 0.75f;
	public Color borderColor = Color.lightGray;
	public boolean borderShadow = false;

	/** ͼ�α��� */
	public String graphTitle;
	public String graphTitleFont;
//��Ʒͼ�δӱ���4��ֲ�����ģ���֧�����з�����Բ�ֳ����Ϊ��������
	public boolean graphTitleBold = false;
	public boolean graphTitleVertical = false;
	public int graphTitleSize = 16;
	public Color graphTitleColor = Color.black;
	public int graphTitleAngle = 0;
	public int graphTitleMargin = 5;

	/** ���ݱ�ʶ */
	public String dataFont;// = "Dialog";
	public boolean dataBold = false;
	public boolean dataVertical = false;
	public int dataSize = 12;
	public Color dataColor = Color.black;
	public int dataAngle;
	public int displayData = IGraphProperty.DISPDATA_NONE;
	public int displayData2 = IGraphProperty.DISPDATA_NONE;
	public String displayDataFormat;

	/** ͼ�� */
	public byte legendLocation = IGraphProperty.LEGEND_RIGHT;
	public int legendVerticalGap = 4;
	public int legendHorizonGap = 4;
	public boolean drawLegendBySery = false;
	public String legendFont;// = "Dialog";
	public boolean legendBold = false;
	public boolean legendVertical = false;
	public int legendSize = 12;
	public Color legendColor = Color.black;
	public int legendAngle;

	// ����
	/** ͼ���Ƿ�͸�� */
	public boolean graphTransparent = false;
	/** �Ƿ񽥱�ɫ,ע���������raisedBorder�ǻ���� */
	public boolean gradientColor = true;
	
	/** ��ǰN�����ݻ�ͼ */
	public int topData;
	/** ������ֵ���ǩ�ص�ʱ�Ƿ���ʾ��һ��ֵ���ǩ */
	public boolean showOverlapText = true;
	/** ����������ɫ */
	public Color canvasColor = Color.white;
	public Color legendBorderColor = Color.darkGray;

	/**
	 * ��ȡ�༭������Ϣ�б�
	 * @return ������Ϣ�б�
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(GraphBase.class, this);

		paramInfos.add(null, new ParamInfo("categories", Consts.INPUT_NORMAL));
		paramInfos.add(null, new ParamInfo("values", Consts.INPUT_NORMAL));

		String group = "border";
		paramInfos.add(group, new ParamInfo("borderStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos
				.add(group, new ParamInfo("borderWidth", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("borderColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("borderShadow",
				Consts.INPUT_CHECKBOX));

		group = "graphTitle";
		paramInfos.add(group, new ParamInfo("graphTitle", Consts.INPUT_NORMAL));
		paramInfos.add(group,
				new ParamInfo("graphTitleFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("graphTitleBold", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("graphTitleVertical", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("graphTitleSize",
				Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("graphTitleColor",
				Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("graphTitleAngle",
				Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("graphTitleMargin",
				Consts.INPUT_INTEGER));


		group = "dataMarks";
		paramInfos.add(group, new ParamInfo("displayData",
				Consts.INPUT_DISPLAYDATA));
		paramInfos.add(group, new ParamInfo("displayData2",
				Consts.INPUT_DISPLAYDATA));
		paramInfos.add(group, new ParamInfo("displayDataFormat",
				Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("dataFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("dataBold", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("dataVertical", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("dataSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("dataColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("dataAngle", Consts.INPUT_INTEGER));

		group = "legend";
		paramInfos.add(group, new ParamInfo("legendLocation",
				Consts.INPUT_LEGENDLOCATION));
		paramInfos.add(group, new ParamInfo("drawLegendBySery",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("legendFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("legendBold", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("legendVertical", Consts.INPUT_CHECKBOX));
		paramInfos.add(group,
				new ParamInfo("legendSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("legendColor", Consts.INPUT_COLOR));
		paramInfos.add(group,
				new ParamInfo("legendAngle", Consts.INPUT_INTEGER));

		group = "graphColor";
		paramInfos.add(group, new ParamInfo("canvasColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("legendBorderColor",
				Consts.INPUT_COLOR));


		group = "other";
		paramInfos.add(group, new ParamInfo("graphTransparent",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("gradientColor",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("showOverlapText",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("topData", Consts.INPUT_INTEGER));

		return paramInfos;
	}

	/**
	 * ͼ���Ƿ�ɼ�
	 * @return �ɼ�ʱ����true�����򷵻�false
	 */
	public boolean isVisible() {
		return true;
	}

	/**
	 * ͼԪ���ػ�ǰ��ֵ��ʼ�������ڳ�ʼ��ֵ��ͼԪ���������Ⱥ����ã�
	 * ����Ҫ�ȳ�ʼ�����ٿ�ʼ��ͼ 
	 */
	public void beforeDraw() {
	} 

	/**
	 * ���Ʊ�����
	 * ע���ڱ�����Ӧ�������е����ɫ����Ӱ�ȣ��Է����Ǻ����Ĳ㡣
	 * �ò㲻�ܻ��߼����� 
	 */
	public void drawBack() {
	} 

	protected GraphFont getGraphFont(String family, boolean bold, boolean vertical, int size,
			Color color, int angle) {
		GraphFont gf = new GraphFont();
		gf.setFamily(family);
		gf.setBold(bold);
		gf.setVerticalText(vertical);
		gf.setSize(size);
		gf.setColor(color.getRGB());
		gf.setAngle(angle);
		return gf;
	}

	protected GraphFonts getGraphFonts() {
		GraphFonts gfs = new GraphFonts();
		GraphFont gf = getGraphFont(graphTitleFont, graphTitleBold,graphTitleVertical,
				graphTitleSize, graphTitleColor, graphTitleAngle);
		gfs.setTitleFont(gf);
		gf = getGraphFont(dataFont, dataBold,dataVertical, dataSize, dataColor, dataAngle);
		gfs.setDataFont(gf);
		gf = getGraphFont(legendFont, legendBold,legendVertical, legendSize, legendColor,
				legendAngle);
		gfs.setLegendFont(gf);
		return gfs;
	}

	protected AlarmLine[] getWarnLines() {
			return null;
	}

	// ��ͼ�θ����Լ������ԣ�����PublicProperty���󷵻ظ�GraphElement.
	protected PublicProperty getPublicProperty() {
		PublicProperty pp = new PublicProperty();
		pp.setBorder((byte) borderStyle, borderWidth, borderColor,
				borderShadow);

		pp.setGraphTitle(graphTitle);
		pp.setFonts(getGraphFonts());
		pp.setTitleMargin(graphTitleMargin);
		pp.setDisplayData((byte) displayData);
		pp.setDisplayData2((byte) displayData2);
		pp.setDisplayDataFormat(displayDataFormat);
		pp.setLegendLocation(legendLocation);
		pp.setLegendVerticalGap(legendVerticalGap);
		pp.setLegendHorizonGap(legendHorizonGap);
		
		pp.setDrawLegendBySery(drawLegendBySery);

		pp.setAlarmLines(getWarnLines());
		
		
		pp.setCanvasColor(canvasColor);
		
		pp.setGraphTransparent(graphTransparent);
		pp.setGradientColor(gradientColor);
		pp.setTopData(topData);
		pp.setShowOverlapText(showOverlapText);
		pp.setAxisColor(PublicProperty.AXIS_LEGEND,legendBorderColor );

		return pp;
	}
	
	protected boolean isSplitByAxis(){
		return false;
	}
	
//	����Ϊ1�� ����Ϊ2
	protected byte getSeriesAxis(String serName){
		return Consts.AXIS_LEFT;
	}
	
	private void transferData(ExtGraphProperty egp) {
		// transfer categories to ExtGraphProperty structure
		ArrayList<ExtGraphCategory> egcList = new ArrayList<ExtGraphCategory>();
		if (categories == null) {
			egp.setGraphTitle("Demo data");
			ExtGraphCategory egc = new ExtGraphCategory();
			egc.setName("A");
			ArrayList<ExtGraphSery> series = new ArrayList<ExtGraphSery>();
			egc.setSeries(series);
			ExtGraphSery egs = new ExtGraphSery();
			egs.setName("Series1");
			egs.setValue(new Integer(80));
			series.add(egs);
			egcList.add(egc);

			egc = new ExtGraphCategory();
			egc.setName("B");
			series = new ArrayList<ExtGraphSery>();
			egc.setSeries(series);
			egs = new ExtGraphSery();
			egs.setName("Series1");
			egs.setValue(new Integer(55));
			series.add(egs);
			egcList.add(egc);

			egc = new ExtGraphCategory();
			egc.setName("C");
			series = new ArrayList<ExtGraphSery>();
			egc.setSeries(series);
			egs = new ExtGraphSery();
			egs.setName("Series1");
			egs.setValue(new Integer(70));
			series.add(egs);
			egcList.add(egc);
		} else {
			int c = categories.length();
			for(int i=1;i<=c;i++){
				Object obj = categories.get(i);
				if(!(obj instanceof String)){
					categories.set(i, Variant.toString(obj));
				}
			}
			Sequence catNames = EnumAxis.extractCatNames(categories);
			Sequence serNames = EnumAxis.extractSerNames(categories);
			if(serNames.length()==0){
				serNames.add(null);
			}
			for( c=1;c<=catNames.length();c++){
				ExtGraphCategory egc = new ExtGraphCategory();
				String catName = Variant.toString(catNames.get(c));
				egc.setName( catName );
				ArrayList<ExtGraphSery> series = new ArrayList<ExtGraphSery>();
				egc.setSeries(series);
				for( int s=1;s<=serNames.length();s++){
					ExtGraphSery egs = new ExtGraphSery();
					String serName = Variant.toString( serNames.get(s));
					egs.setName( serName );
					int index = Utils.indexOf(categories, catName, serName);
					if (index == 0) { // ĳ�������ϵ�е���ֵȱ��
						continue;
					}
					egs.setValue( (Number)values.get(index));
					if(isSplitByAxis()){
						egs.setAxis( getSeriesAxis(serName) );
					}
					series.add(egs);
				}
				egcList.add(egc);
			}
		}
		egp.setSplitByAxis(isSplitByAxis());
		egp.setCategories(egcList);
	}

	/**
	 * �����м��
	 */
	public void draw() {
		PublicProperty pp = getPublicProperty();
		ExtGraphProperty egp = StatisticGraph.calc1(pp);
		transferData(egp);
		egp.recalcProperty();
		DrawBase graph = (DrawBase)DrawBase.getInstance(egp);
		graph.transProperty(egp);

		if (!StringUtils.isValidString(egp.getLinkTarget())) {
			egp.setLinkTarget("_blank");
		}

		int i = egp.getCanvasColor();
		int dShadow = 5;
		Graphics2D g = e.getGraphics();
		int w = e.getW();
		int h = e.getH();
		if (i != 16777215) { // �������͸��ɫ�򻭵װ�ɫ
			if (pp.getBorderShadow()) {
				g.setBackground( Color.LIGHT_GRAY );
				g.clearRect(dShadow, dShadow, w , h);
			}
			g.setBackground(new Color(i));
			g.clearRect(0, 0, w - dShadow, h - dShadow);
		}
		StatisticGraph.drawBackGraph(g, egp, w - dShadow, h - dShadow);
		
		if (pp.getBorderStyle() != IStyle.LINE_NONE && pp.getBorderWidth() > 0
				&& pp.getBorderColor() != AppUtil.TRANSPARENT_COLOR) {
			BorderStyle bs = new BorderStyle();
			bs.setTBStyle(pp.getBorderStyle());
			bs.setBBStyle(pp.getBorderStyle());
			bs.setLBStyle(pp.getBorderStyle());
			bs.setRBStyle(pp.getBorderStyle());

			bs.setTBWidth(pp.getBorderWidth());
			bs.setBBWidth(pp.getBorderWidth());
			bs.setLBWidth(pp.getBorderWidth());
			bs.setRBWidth(pp.getBorderWidth());

			bs.setTBColor(pp.getBorderColor());
			bs.setBBColor(pp.getBorderColor());
			bs.setLBColor(pp.getBorderColor());
			bs.setRBColor(pp.getBorderColor());

			CellBorder.setEnv(g, bs, 1, 1, 1, 1, false);
			CellBorder.drawBorder(0, 0, w - dShadow, h - dShadow);
		}

		if (w > 50 && h > 50) {
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND, 0.1f));
			DrawBase db = (DrawBase) graph;
			db.setGraphics2D(g);
			db.setGraphWH(w - dShadow, h - dShadow);
			db.draw(null);
		}
	}

	/**
	 * ����ǰ����
	 * ǰ�������ͼ�εı��ߣ��Լ����� 
	 */
	public void drawFore() {
	} 

	/**
	 * ����ͼ��ǰ�����ݻ���׼��
	 */
	public void prepare() {
		Utils.setParamsEngine(this);
	}

	protected transient Engine e;

	/**
	 * ����ͼ������
	 * @param e ͼ������
	 */
	public void setEngine(Engine e) {
		this.e = e;
	}

	/**
	 * ��ȡͼ������
	 * @return ͼ������
	 */
	public Engine getEngine() {
		return e;
	}

	/**
	 * ��ȡ������������״
	 * @return null
	 */
	public ArrayList<Shape> getShapes() {
		return null;
	}

	/**
	 * ��ȡ������
	 * @return null
	 */
	public ArrayList<String> getLinks() {
		return null;
	}

}
