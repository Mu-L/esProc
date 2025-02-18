package com.scudata.chart;

import org.w3c.dom.*;

import com.scudata.app.common.*;
import com.scudata.chart.edit.*;
import com.scudata.chart.element.*;
import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.expression.*;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

/**
 * ��ͼ����
 */
public class Engine {
	private ArrayList<IElement> elements;
	private ArrayList<TickAxis> axisList = new ArrayList<TickAxis>(); // ��
	private ArrayList<ICoor> coorList = new ArrayList<ICoor>(); // ����ϵ
	private ArrayList<DataElement> dataList = new ArrayList<DataElement>(); // ����ͼԪ
	private ArrayList<TimeAxis> timeList = new ArrayList<TimeAxis>(); // ʱ����

	private transient ArrayList<Shape> allShapes = new ArrayList<Shape>();
	private transient ArrayList<String> allLinks = new ArrayList<String>();
	private transient ArrayList<String> allTitles = new ArrayList<String>();
	private transient ArrayList<String> allTargets = new ArrayList<String>();
	private transient int w, h;
	private transient Graphics2D g;
	private transient ArrayList textAreas = new ArrayList(); // �ı������Ѿ�ռ�õĿռ�
	private transient StringBuffer html;

	private transient double t_maxDate = 0, t_minDate = Long.MAX_VALUE;// ����ʱ����������Сʱ���

	/**
	 * �����ͼ����
	 */
	public Engine() {
	}

	/**
	 * ����ͼ��Ԫ���б�
	 * 
	 * @param ies
	 *            ��ͼԪ��
	 */
	public void setElements(ArrayList<IElement> ies) {
		elements = ies;
	}

	/**
	 * ���ÿ̶����б�
	 * 
	 * @param tas
	 *            �̶����б�
	 */
	public void setAxisList(ArrayList<TickAxis> tas) {
		axisList = tas;
	}

	/**
	 * ��������ϵ�б�
	 * 
	 * @param ics
	 *            ����ϵ�б�
	 */
	public void setCoorList(ArrayList<ICoor> ics) {
		coorList = ics;
	}

	/**
	 * ��������ͼԪ�б�
	 * 
	 * @param des
	 *            ����ͼԪ
	 */
	public void setDataList(ArrayList<DataElement> des) {
		dataList = des;
	}

	/**
	 * ����ʱ����
	 * 
	 * @param tas
	 *            ʱ�����б�
	 */
	public void setTimeList(ArrayList<TimeAxis> tas) {
		timeList = tas;
	}

	/**
	 * ��ȡʱ����
	 * 
	 * @param name
	 *            ������
	 * @return ʱ�������
	 */
	public TimeAxis getTimeAxis(String name) {
		for (int i = 0; i < timeList.size(); i++) {
			TimeAxis axis = timeList.get(i);
			if (axis.getName().equals(name)) {
				return axis;
			}
		}
		return null;
	}

	/**
	 * ��ȡ����ͼԪ��Ӧ����״�б�
	 * 
	 * @return
	 */
	public ArrayList<Shape> getShapes() {
		return allShapes;
	}

	/**
	 * ��ȡ������֡��frameCount�ĵ�frameIndex֡ͼ��ļ�������
	 * @param frameCount;��֡��
	 * @param frameIndex;�ڼ�֡����0��ʼ
	 * @return
	 */
	public Engine getFrameEngine(int frameCount, int frameIndex){
		Engine e = clone();

		ArrayList<DataElement> des = new ArrayList<DataElement>();
		double timeSlice = (t_maxDate-t_minDate)*1f/frameCount;//ʱ���Ȱ�����֡���з�
		double frameTime = t_minDate+ timeSlice*frameIndex;//�������ǰ֡����ʱ���
		
		for(DataElement de:dataList){
			String timeAxis = de.getAxisTimeName(); 
			TimeAxis ta = e.getTimeAxis(timeAxis);
			if(ta.displayMark){
//				����ʱ���
				des.add( ta.getMarkElement(frameTime) );
			}
			if( StringUtils.isValidString(timeAxis) ){
				e.elements.remove(de);//elements�Ѿ���������ͼԪ����ʱ�����µ�����ͼԪ����Ҫ���ϵ�����ͼԪ�޳�
				des.add(de.getFrame( frameTime ) );
			}else{
				des.add(de);
			}
		}
		e.setDataList(des);
		return e;
	}

	/**
	 * ͼԪ�г����ӵĻ�����ȡ�������б�
	 * 
	 * @return
	 */
	public ArrayList<String> getLinks() {
		return allLinks;
	}

	/**
	 * ��������ͼ�ε�HTML�����Ӵ���
	 * 
	 * @return
	 */
	public String getHtmlLinks() {
		return generateHyperLinks(true);
	}

	/**
	 * ��ȡ������״shape�ı߽�����
	 * 
	 * @param shape
	 * @return ����ε������
	 */
	private ArrayList<Point> getShapeOutline(Shape shape) {
		// ��ȡ��״shape��ƽ��·��
		PathIterator iter = new FlatteningPathIterator(
				shape.getPathIterator(new AffineTransform()), 1);
		ArrayList<Point> points = new ArrayList<Point>();
		float[] coords = new float[6];
		while (!iter.isDone()) {
			iter.currentSegment(coords);
			int x = (int) coords[0];
			int y = (int) coords[1];
			points.add(new Point(x, y));
			iter.next();
		}
		return points;
	}

	private String getRectCoords(Rectangle rect) {
		int x, y, w, h;
		x = rect.x;
		y = rect.y;
		w = rect.width;
		h = rect.height;
		// �����Ӵ���
		int minimum = 10;
		if (w < minimum) {
			w = minimum;
		}
		if (h < minimum) {
			h = minimum;
		}
		String coords = x + "," + y + "," + (x + w) + "," + (y + h);
		return coords;
	}

	private String getPolyCoords(Shape shape) {
		StringBuffer buf = new StringBuffer();
		ArrayList<Point> polyPoints = getShapeOutline(shape);
		for (int i = 0; i < polyPoints.size(); i++) {
			Point p = polyPoints.get(i);
			if (i > 0) {
				buf.append(",");
			}
			buf.append(p.getX() + "," + p.getY());
		}
		return buf.toString();
	}

	private String dealSpecialChar(String str) {
		// ������Ÿ�Ϊ�ϲ㴦���˴����ٴ���
		return str;
	}

	private String getLinkHtml(String link, String shape, String coords,
			String title, Object target) {
		StringBuffer sb = new StringBuffer(128);
		sb.append("<area shape=\"").append(shape).append("\" coords=\"");
		sb.append(coords);
		if (StringUtils.isValidString(link)) {
			link = dealSpecialChar(link);
			sb.append("\" href=\"").append(link).append("\" target=\"")
					.append(target);
		}

		if (StringUtils.isValidString(title)) {
			title = dealSpecialChar(title);
			sb.append("\" title=\"").append(title);
		}
		sb.append("\">\n");
		return sb.toString();
	}

	// svgû����ʾ��Ϣ�����������title������Ч��
	private String getLinkSvg(String link, String shape, String coords,
			String title, Object target) {
		StringBuffer sb = new StringBuffer(128);
		link = dealSpecialChar(link);
		sb.append("<a xlink:href=\"").append(link);
		sb.append("\" target=\"");
		sb.append(target);
		sb.append("\">\n");

		sb.append("<");
		sb.append(shape);
		sb.append(" ");
		sb.append(coords);
		sb.append("/>\n");

		sb.append("</a>");

		return sb.toString();
	}

	/**
	 * ����isHtml����Html�Ļ���svg�ĳ�����
	 * 
	 * @param isHtml
	 *            ���淵��html�ĳ����Ӵ�������Ϊsvgͼ�ε����Ӵ�
	 * @return
	 */
	private String generateHyperLinks(boolean isHtml) {
		if (allLinks.isEmpty())
			return null;

		StringBuffer buf = new StringBuffer();
		String link, shape, coords, target, title;
		for (int i = 0; i < allShapes.size(); i++) {
			link = allLinks.get(i);
			Shape s = allShapes.get(i);
			target = allTargets.get(i);
			title = allTitles.get(i);
			if (isHtml) {
				if (s instanceof Rectangle) {
					shape = "rect";
					coords = getRectCoords((Rectangle) s);
				} else {// if(s instanceof Polygon){
					shape = "poly";
					coords = getPolyCoords(s);
				}
				buf.append(getLinkHtml(link, shape, coords, title, target));
			} else {// svg
				String style = " style=\"fill-opacity:0;stroke-width:0\"";
				if (s instanceof Rectangle) {
					Rectangle r = (Rectangle) s;
					shape = "rect";
					coords = "x=\"" + r.x + "\" y=\"" + r.y + "\" width=\""
							+ r.width + "\" height=\"" + r.height + "\""
							+ style;
				} else {// if(s instanceof Polygon){
					shape = "polygon";
					coords = "points=\"" + getPolyCoords(s) + "\"" + style;
				}
				buf.append(getLinkSvg(link, shape, coords, title, target));
			}
		}
		return buf.toString();
	}

	/**
	 * Ϊ�˷�ֹͼԪ�ص������ƹ�����ÿ�������ͼԪλ�ö��Ỻ�� �÷��������ж�ָ���ľ���λ��rect�Ƿ���Ѿ������ͼ��Ԫ�����ཻ
	 * 
	 * @param rect
	 *            ָ���ľ���λ��
	 * @return ������ཻʱ����true������false
	 */
	public boolean intersectTextArea(Rectangle rect) {
		int size = textAreas.size();
		for (int i = 0; i < size; i++) {
			Rectangle tmp = (Rectangle) textAreas.get(i);
			if (tmp.intersects(rect)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * ÿ����һ��ͼԪ����Ҫ����ǰͼԪ������λ����Ϣ��ӵ����滺��
	 * 
	 * @param rect
	 *            ͼԪ��Ӧ�ľ�������λ��
	 */
	public void addTextArea(Rectangle rect) {
		textAreas.add(rect);
	}

	private IElement getElement(Sequence chartParams) {
		ChartParam cp = (ChartParam) chartParams.get(1);
		ElementInfo ei = ElementLib.getElementInfo(cp.getName());
		if (ei == null) {
			throw new RuntimeException("Unknown chart element: " + cp.getName());
		}
		ObjectElement oe = ei.getInstance();
		oe.loadProperties(chartParams);
		return oe;
	}

	/**
	 * �ö���õ�ͼԪ���й����ͼ����
	 * 
	 * @param chartElements
	 *            ͼ��Ԫ������
	 */
	public Engine(Sequence chartElements) {
		int size = chartElements.length();
		this.elements = new ArrayList<IElement>();
		for (int i = 1; i <= size; i++) {
			Sequence chartParams = (Sequence) chartElements.get(i);
			IElement e = getElement(chartParams);
			this.elements.add(e);
		}
		prepare();
	}

	/**
	 * ��ȡ�̶����б�
	 * 
	 * @return �̶���
	 */
	public ArrayList<TickAxis> getAxisList() {
		return axisList;
	}

	/**
	 * ��ȡ����ͼԪ�б�
	 * 
	 * @return ����ͼԪ
	 */
	public ArrayList<DataElement> getDataList() {
		return dataList;
	}

	/**
	 * ��ȡ����ϵ�б�һ����������Բ��ö������ϵ���Ա�������ͼ��
	 * 
	 * @return ����ϵ
	 */
	public ArrayList<ICoor> getCoorList() {
		return coorList;
	}

	/**
	 * ���ݿ̶�������ƻ�ȡ��Ӧ�Ŀ̶������
	 * 
	 * @param name
	 *            �̶�������
	 * @return �̶������
	 */
	public TickAxis getAxisByName(String name) {
		for (int i = 0; i < axisList.size(); i++) {
			TickAxis axis = axisList.get(i);
			if (axis.getName().equals(name)) {
				return axis;
			}
		}
		return null;
	}

	public IMapAxis getMapAxisByName(String name) {
		for (int i = 0; i < elements.size(); i++) {
			IElement e = elements.get(i);
			if (e instanceof IMapAxis) {
				IMapAxis ma = (IMapAxis) e;
				if (ma.getName().equals(name)) {
					return ma;
				}
			}
		}
		return null;
	}

	private ArrayList<DataElement> getDataElementsOnAxis(String axis) {
		ArrayList<DataElement> al = new ArrayList<DataElement>();
		int size = dataList.size();
		for (int i = 0; i < size; i++) {
			DataElement de = dataList.get(i);
			if (de.isPhysicalCoor()) {
				continue;
			}

			if (de.getAxis1Name().equals(axis)
					|| de.getAxis2Name().equals(axis)) {
				al.add(de);
			}
		}
		return al;
	}

	private ArrayList<DataElement> getDataElementsOnTime(String axis) {
		ArrayList<DataElement> al = new ArrayList<DataElement>();
		int size = dataList.size();
		for (int i = 0; i < size; i++) {
			DataElement de = dataList.get(i);

			if (de.getAxisTimeName().equals(axis)) {
				al.add(de);
			}
		}
		return al;
	}

	private void prepare() {
		// ֻ���Ƿ����ͼʱ��ֻ��һ��ͼ������������ͼ��ֵ�������Զ���ö�����ȡͼ��ֵ��
		ArrayList<Legend> legends = new ArrayList<Legend>();
		// ������ͼԪ
		for (int i = 0; i < elements.size(); i++) {
			IElement e = elements.get(i);
			e.setEngine(this);
			if (e instanceof TickAxis) {
				if (axisList.contains(e)) {
					continue;
				}
				axisList.add((TickAxis) e);
			}
			if (e instanceof Legend) {
				legends.add((Legend) e);
			}
			if (e instanceof TimeAxis) {
				timeList.add((TimeAxis) e);
			}
		}
		// ��������ͼԪ�������õ�������ϵ
		for (int i = 0; i < elements.size(); i++) {
			Object e = elements.get(i);
			if (!(e instanceof DataElement)) {
				continue;
			}
			DataElement de = (DataElement) e;
			dataList.add(de);
			if (de.isPhysicalCoor()) {
				continue;
			}

			String name1 = de.getAxis1Name();
			String deName = de.getClass().getName();
			int lastDot = deName.lastIndexOf(".");
			deName = "Data element " + deName.substring(lastDot + 1);
			if (!StringUtils.isValidString(name1)) {
				throw new RuntimeException(deName
						+ "'s property axis1 is not valid.");
			}
			String name2 = de.getAxis2Name();
			if (!StringUtils.isValidString(name2)) {
				throw new RuntimeException(deName
						+ "'s property axis2 is not valid.");
			}
			TickAxis axis1 = getAxisByName(name1);
			if (axis1 == null) {
				throw new RuntimeException(deName + "'s axis1: " + name1
						+ " is not defined.");
			}
			TickAxis axis2 = getAxisByName(name2);
			if (axis2 == null) {
				throw new RuntimeException(deName + "'s axis2: " + name2
						+ " is not defined.");
			}

			int L1 = axis1.getLocation();
			int L2 = axis2.getLocation();
			ICoor coor = null;
			RuntimeException re = new RuntimeException("Axis " + name1
					+ " and " + name2
					+ " can not construct a coordinate system.");
			switch (L1) {
			case Consts.AXIS_LOC_H:
				if (L2 != Consts.AXIS_LOC_V) {
					throw re;
				}
				coor = new CartesianCoor();
				break;
			case Consts.AXIS_LOC_V:
				if (L2 != Consts.AXIS_LOC_H) {
					throw re;
				}
				coor = new CartesianCoor();
				break;
			case Consts.AXIS_LOC_POLAR:
				if (L2 != Consts.AXIS_LOC_ANGLE) {
					throw re;
				}
				coor = new PolarCoor();
				break;
			case Consts.AXIS_LOC_ANGLE:
				if (L2 != Consts.AXIS_LOC_POLAR) {
					throw re;
				}
				coor = new PolarCoor();
				break;
			}
			coor.setAxis1(axis1);
			coor.setAxis2(axis2);

			if (!coorList.contains(coor)) {
				coorList.add(coor);
			}
		}

		// ����ͼԪ��ͼǰ׼������,����ͼԪ��������ݣ����Ե�����ͼԪ֮ǰ׼��
		for (int i = 0; i < dataList.size(); i++) {
			DataElement de = dataList.get(i);
			de.prepare();
		}

		// ö����ͼԪ��׼������Ϊ��������ֵ��׼���������漰���ۻ�ʱ���ᰴ��ö��ֵ���ۻ��������ֵ
		for (int i = 0; i < axisList.size(); i++) {
			TickAxis axis = axisList.get(i);
			if (axis instanceof EnumAxis) {
				ArrayList<DataElement> al = getDataElementsOnAxis(axis
						.getName());
				axis.prepare(al);
				if (legends.size() == 1) {
					Sequence seq = ((EnumAxis) axis).series;
					if (seq == null || seq.length() == 0) {
						seq = ((EnumAxis) axis).categories;
					}
					Legend legend = legends.get(0);
					if (legend.legendText.getLength() == 0) {
						legend.legendText.setValue(seq);
					}
				}

			}
		}

		// ��ͼԪ��ͼǰ׼������
		for (int i = 0; i < axisList.size(); i++) {
			TickAxis axis = axisList.get(i);
			if (axis instanceof EnumAxis) {
				continue;
			}
			ArrayList<DataElement> al = getDataElementsOnAxis(axis.getName());
			axis.prepare(al);
		}

		// ʱ����׼������
		for (int i = 0; i < timeList.size(); i++) {
			TimeAxis ta = timeList.get(i);
			ArrayList<DataElement> al = getDataElementsOnTime(ta.getName());
			ta.prepare(al);
			t_maxDate = Math.max(ta.getMaxDate(), t_maxDate);
			t_minDate = Math.min(ta.getMinDate(), t_minDate);
		}
	}

	/**
	 * ��ȡ���л�ͼԪ���б�
	 * 
	 * @return ȫ����ͼԪ��
	 */
	public ArrayList<IElement> getElements() {
		return elements;
	}

	/**
	 * �Ƿ񶯻�
	 * 
	 * @return ����Ƕ�������true
	 */
	public boolean isAnimate() {
		return !timeList.isEmpty();
	}

	private byte[] generateSVG(int w, int h) throws Exception {
		Object batikDom = Class.forName(
				"org.apache.batik.dom.GenericDOMImplementation").newInstance();

		DOMImplementation domImpl = (DOMImplementation) AppUtil.invokeMethod(
				batikDom, "getDOMImplementation", new Object[] {});
		// org.apache.batik.dom.GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);

		// Create an instance of the SVG Generator.
		Class cls = Class.forName("org.apache.batik.svggen.SVGGraphics2D");
		Constructor con = cls.getConstructor(new Class[] { Document.class });
		Object g2d = con.newInstance(new Object[] { document });

		// org.apache.batik.svggen.SVGGraphics2D ggd = new
		// org.apache.batik.svggen.SVGGraphics2D(document);

		draw((Graphics2D) g2d, 0, 0, w, h, null);

		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Writer out = new OutputStreamWriter(baos, "UTF-8");
		// StringWriter out = new StringWriter();//ֱ��д����ȥ����ת���ַ���

		AppUtil.invokeMethod(g2d, "stream", new Object[] { out,
				new Boolean(useCSS) }, new Class[] { Writer.class,
				boolean.class });
		// g2d.stream(out, useCSS);

		out.flush();
		out.close();
		// return out.toString().getBytes();

		baos.close();

		byte[] bs = baos.toByteArray();
		
		//���Դ�������SVG��Ԫ�ؿ�ߣ�����Ч
//        Element svgRoot = document.getDocumentElement();
//        svgRoot.setAttribute("width", w+"");
//        svgRoot.setAttribute("height", h+"");
//		ʹ�ô�Ӳƴ
		String buf = new String(bs, "UTF-8");
		int n = buf.lastIndexOf("<svg");
		StringBuffer sb = new StringBuffer();
		sb.append(buf.substring(0, n+4));
		String view = " width=\""+w+"\" height=\""+h+"\" ";
		sb.append(view);
		sb.append(buf.substring(n+5));
		bs = sb.toString().getBytes("UTF-8");

        String links = generateHyperLinks(false);
		if (links != null) {// ƴ���ϳ�����
			buf = new String(bs, "UTF-8");
			n = buf.lastIndexOf("</svg");
			sb = new StringBuffer();
			sb.append(buf.substring(0, n));
			sb.append(links);
			sb.append("</svg>");
			bs = sb.toString().getBytes("UTF-8");
		}

		return bs;
	}

	/**
	 * ������Ļ���ͼ��ת��Ϊ��Ӧ��ʽ��ͼ������
	 * 
	 * @param bi
	 *            ��ת���Ļ���ͼ��
	 * @param imageFmt
	 *            Ŀ��ͼƬ��ʽ��Consts.IMAGE_XXX
	 * @return ת��ΪͼƬ��ʽ����ֽ�����
	 */
	public static byte[] getImageBytes(BufferedImage bi, byte imageFmt)
			throws Exception {
		byte[] bytes = null;
		switch (imageFmt) {
		case Consts.IMAGE_GIF:
			bytes = ImageUtils.writeGIF(bi);
			break;
		case Consts.IMAGE_JPG:
			bytes = ImageUtils.writeJPEG(bi);
			break;
		case Consts.IMAGE_PNG:
			bytes = ImageUtils.writePNG(bi);
			break;
		case Consts.IMAGE_TIFF:
			bytes = ImageUtils.writeGIF(bi);
			break;
		}
		return bytes;
	}

	/**
	 * ����ָ������Լ����ͼ�θ�ʽ���ƻ���ͼ��
	 * 
	 * @param w
	 *            ͼ�ο�ȣ�����
	 * @param h
	 *            ͼ�θ߶ȣ�����
	 * @param imageFmt
	 *            ��������ͼƬ��ʽ
	 * @return �����Ļ���ͼ��
	 */
	public BufferedImage calcBufferedImage(int w, int h, byte imageFmt) {
		BufferedImage bi = null;

		if (imageFmt == Consts.IMAGE_GIF || imageFmt == Consts.IMAGE_PNG) {
			bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		} else if (imageFmt == Consts.IMAGE_JPG) {
			bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		} else { // Flash
			imageFmt = Consts.IMAGE_PNG;
			bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D lg = (Graphics2D) bi.getGraphics();

		Utils.setIsGif( imageFmt == Consts.IMAGE_GIF );
		if (imageFmt == Consts.IMAGE_PNG) {
		} else if (imageFmt == Consts.IMAGE_JPG) {
			// ȱʡΪ��ɫ����������ȱʡ��ɫ�ѿ�
			lg.setColor(Color.white);
			lg.fillRect(0, 0, w, h);
		} else { // Flash
		}

		draw(lg, 0, 0, w, h, null);
		return bi;
	}

	/**
	 * ����ͬcalcBufferedImage�������ο�ǰ������
	 * 
	 * @param w
	 * @param h
	 * @param imageFmt
	 * @return ������ɺ��ͼƬ�ֽ�����
	 */
	public byte[] calcImageBytes(int w, int h, byte imageFmt) {
		Graphics2D lg = null;
		try {
			if (w + h == 0) {
				int[] wh = getBackOrginalWH();
				if (wh != null) {
					w = wh[0];
					h = wh[1];
				}
			}
			if (imageFmt == Consts.IMAGE_SVG) {
				return generateSVG(w, h);
			}

			BufferedImage bi = calcBufferedImage(w, h, imageFmt);
			return getImageBytes(bi, imageFmt);
		} catch (Exception x) {
			throw new RuntimeException(x);
		} finally {
			if (lg != null) {
				lg.dispose();
			}
		}
	}

	/**
	 * ��ȡ����ͼ��ԭʼ��ߣ�����ͼ��䷽ʽ��Ҫ���ݸ���Ϣ��Ӧ����������
	 * 
	 * @return ��͸߹��ɵ���������
	 */
	public int[] getBackOrginalWH() {
		int size = elements.size();
		for (int i = 0; i < size; i++) {
			IElement e = elements.get(i);
			if (e instanceof com.scudata.chart.element.BackGround) {
				BackGround bg = (BackGround) e;
				return bg.getOrginalWH();
			}
		}
		return null;
	}

	/**
	 * ִ�л�ͼ����������htmlΪnullʱ�����ɳ�����
	 * 
	 * @param g
	 *            ͼ���豸
	 * @param x
	 *            x����
	 * @param y
	 *            y����
	 * @param w
	 *            ͼ�ο��
	 * @param h
	 *            ͼ�θ߶�
	 * @param html
	 *            ���ڻ���ͼ���в����ĳ�������Ϣ
	 */
	public void draw(Graphics2D g, int x, int y, int w, int h, StringBuffer html) {
		long b = System.currentTimeMillis();

		this.g = g;
		this.w = w;
		this.h = h;
		this.html = html;
		// ֻ����x��yΪ����������
		if (x + y != 0) {
			g.translate(x, y);
		}
		Utils.setGraphAntiAliasingOn(g);
		textAreas.clear();
		ArrayList<IElement> bufElements = new ArrayList<IElement>();// ����ͼԪ��ͼԪ�������ƣ��Ȼ��ƹ��ģ��ʹӻ����Ƴ�
		bufElements.addAll(elements);

		int size = bufElements.size();
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) elements.get(i);
			e.beforeDraw();
		}

		// ��һ�����Ȼ�����ͼ������һ��ֻ����һ�����˴����ж�
		for (int i = 0; i < size; i++) {
			IElement e = elements.get(i);
			if (e instanceof com.scudata.chart.element.BackGround) {
				e.drawBack();
				bufElements.remove(e);// �Ѿ����ƹ���ͼԪ�ӻ���ȥ��
			}
		}

		// �ڶ�����������ͼԪ
		drawElements(getAxisList());
		bufElements.removeAll(getAxisList());

		// ����������������ͼԪ
		drawElements(getDataList());
		ArrayList<DataElement> des = getDataList();
		for (int i = 0; i < des.size(); i++) {
			DataElement de = des.get(i);
			Object ss = de.getShapes();
			if (ss == null)
				continue;
			allShapes.addAll(de.getShapes());
			ArrayList<String> links = de.getLinks();
			allLinks.addAll(links);
			allTitles.addAll(de.getTitles());
			// for (int n = 0; n < links.size(); n++) {
			// allTargets.add(de.getTarget());// target��link����
			// }
			allTargets.addAll(de.getTargets());
		}
		bufElements.removeAll(getDataList());

		// ���Ĳ�������ʣ���ͼԪ
		drawElements(bufElements);

		if (x + y != 0) {
			g.translate(-x, -y);
		}

		long e = System.currentTimeMillis();
		Logger.debug("Calc chart last time: " + (e - b) + " ms");
	}

	private void drawElements(ArrayList als) {
		int size = als.size();
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) als.get(i);
			e.drawBack();
		}
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) als.get(i);
			e.draw();
		}
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) als.get(i);
			e.drawFore();
		}
	}

	/**
	 * ��ȡ��ǰ��ͼ�豸
	 * 
	 * @return
	 */
	public Graphics2D getGraphics() {
		return g;
	}

	/**
	 * ��ȡ�����Ƶ�ͼ�ο�ȣ���λ����
	 * 
	 * @return ͼ�ο��
	 */
	public int getW() {
		return w;
	}

	/**
	 * ��ȡ�����Ƶ�ͼ�θ߶ȣ���λ����
	 * 
	 * @return ͼ�θ߶�
	 */
	public int getH() {
		return h;
	}

	/**
	 * ����valֵλ��ͼ���е�����λ�ã�val����1ʱ��ֱ�ӱ�ʾ����λ�ã� 0<val<=1ʱ����ʾ����ڵ�ǰͼ�ο�ȵı���λ�ã�
	 * val<1ʱ����ʾ��������½�ʱ�ķ���λ��
	 * 
	 * @param val
	 *            ��ת������ֵ
	 * @return ת����ĺ���ͼ�ξ���λ������ֵ������ʵ�����ȣ������ۻ����
	 */
	public double getXPixel(double val) {
		return getPixel(val, getW());
	}

	/**
	 * ����ͬgetXPixel���ο���Ӧ����
	 * 
	 * @param val
	 *            ��ת������ֵ
	 * @return ������������
	 */
	public double getYPixel(double val) {
		return getPixel(val, getH());
	}

	private double getPixel(double val, double length) {
		if (val > 1) { // val����1ʱ��ʾ������������
			return val;
		} else if (val >= 0) {
			// �����ʾ���ͼ��length�ı�������
			return length * val;
		} else {// ����ʱ����һ����µ�������
			if (val > -1) {
				val = length * val;
			}
			return length + val;
		}
	}

	public StringBuffer getHtml() {
		return html;
	}

	/**
	 * ��¡��ǰ��ͼ����
	 */
	public Engine clone() {
		Engine e = new Engine();
		e.elements = (ArrayList<IElement>) elements.clone();
		e.axisList = (ArrayList<TickAxis>) axisList.clone();
		e.coorList = (ArrayList<ICoor>) coorList.clone();
		e.dataList = (ArrayList<DataElement>) dataList.clone();
		e.timeList = (ArrayList<TimeAxis>)timeList.clone();
		for (IElement ie : e.elements) {
			ie.setEngine(e);
		}

		return e;
	}

	public static void main(String[] args) {
		BufferedImage bi = new BufferedImage(633, 450,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, 633, 450);
		Utils.setGraphAntiAliasingOn(g);

		// Point2D.Double p1 = new Point2D.Double(10, 10);
		// Point2D.Double p2 = new Point2D.Double(100, 100);
		// Paint paint = new GradientPaint(50, 50, Color.red, 100, 50,
		// Color.blue,
		// true);
		// g.setPaint(paint);
		// g.fillRect((int) p1.getX(), (int) p1.getY(), 90, 90);
		Path2D p = new Path2D.Double();
		p.moveTo(10, 10);
		p.lineTo(20, 10);
		p.lineTo(20, 20);
		p.closePath();
		g.setColor(Color.black);
		g.draw(p);

		int angle = 90;
		int x =100, y=100;
		g.drawOval(x, y, 3, 3);
		String text = "Text";
		g.drawString(text, x, y);
		
		double rotateAngle = Math.toRadians(-angle);
		AffineTransform at = g.getTransform();
		AffineTransform at1 = AffineTransform.getRotateInstance(
				rotateAngle, x, y);
		g.transform(at1);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Color.red);
		g.drawString(text, (int)x, (int)y);
		
		g.dispose();
		try {
			FileOutputStream os = new FileOutputStream("E:/test.jpg");
			com.scudata.common.ImageUtils.writeJPEG(bi, os);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		g.dispose();
		System.out.println("OK");
		/**/
	}
}
