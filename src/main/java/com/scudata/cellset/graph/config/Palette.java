package com.scudata.cellset.graph.config;

import java.util.*;

import com.scudata.common.*;

import java.io.*;
import java.awt.*;
import java.awt.image.*;

/**
 * ��ɫ���������ڻ�ͳ��ͼ
 */
public class Palette {
	public final static byte PATTERN_DEFAULT = 0; // ���ͼ����ȫ���
	public final static byte PATTERN_H_THIN_LINE = 1; // ���ͼ����ˮƽϸ��
	public final static byte PATTERN_H_THICK_LINE = 2; // ���ͼ����ˮƽ����
	public final static byte PATTERN_V_THIN_LINE = 3; // ���ͼ������ֱϸ��
	public final static byte PATTERN_V_THICK_LINE = 4; // ���ͼ������ֱ����
	public final static byte PATTERN_THIN_SLASH = 5; // ���ͼ����ϸб��
	public final static byte PATTERN_THICK_SLASH = 6; // ���ͼ������б��
	public final static byte PATTERN_THIN_BACKSLASH = 7; // ���ͼ����ϸ��б��
	public final static byte PATTERN_THICK_BACKSLASH = 8; // ���ͼ�����ַ�б��
	public final static byte PATTERN_THIN_GRID = 9; // ���ͼ����ϸ����
	public final static byte PATTERN_THICK_GRID = 10; // ���ͼ����������
	public final static byte PATTERN_THIN_BEVEL_GRID = 11; // ���ͼ����ϸб����
	public final static byte PATTERN_THICK_BEVEL_GRID = 12; // ���ͼ������б����
	public final static byte PATTERN_DOT_1 = 13; // ���ͼ����ϡ���
	public final static byte PATTERN_DOT_2 = 14; // ���ͼ������ϡ��
	public final static byte PATTERN_DOT_3 = 15; // ���ͼ�������ܵ�
	public final static byte PATTERN_DOT_4 = 16; // ���ͼ�������ܵ�
	public final static byte PATTERN_SQUARE_FLOOR = 17; // ���ͼ����������ذ�ש
	public final static byte PATTERN_DIAMOND_FLOOR = 18; // ���ͼ�������εذ�ש
	public final static byte PATTERN_BRICK_WALL = 19; // ���ͼ����שǽ

	private int[] colors = null;
	private byte[] patterns = null;
	private int size = 0;

	private static Palette defaultPalette = getDefaultPalette2();
	private static Map colorMap = readPalettes();

	/**
	 * ������ɫ������ȱʡ����Ϊ20
	 */
	public Palette() {
		this(20);
	}

	/**
	 * ������ɫ����
	 * 
	 * @param capacity
	 *            ��ʼ����
	 */
	public Palette(int capacity) {
		colors = new int[capacity];
		patterns = new byte[capacity];
	}

	/**
	 * ȷ����С����
	 * 
	 * @paran mincap ��С����
	 */
	public void ensureCapacity(int mincap) {
		int[] colors = this.colors;
		if (mincap > colors.length) {
			int newcap = (colors.length * 3) / 2 + 1;
			this.colors = new int[newcap < mincap ? mincap : newcap];
			System.arraycopy(colors, 0, this.colors, 0, size);

			this.patterns = new byte[newcap < mincap ? mincap : newcap];
			System.arraycopy(patterns, 0, this.patterns, 0, size);
		}
	}

	/**
	 * ��λ��ȡ��ɫ
	 * 
	 * @param index
	 *            λ��(��0��ʼ)
	 */
	public int getColor(int index) {
		if (colors.length == 0) {
			return defaultPalette.getColor(index);
		}
		int p;
		if (index < colors.length) {
			p = index;
		} else {
			p = index % colors.length;
		}
		return this.colors[p];
	}

	public static byte[] listPatterns() {
		byte[] patterns = new byte[20];
		for (byte b = PATTERN_DEFAULT; b <= PATTERN_BRICK_WALL; b++) {
			patterns[b] = b;
		}
		return patterns;
	}

	public static int getPatternsCount() {
		return 20;
	}

	public byte getPattern(int index) {
		if (patterns.length == 0) {
			return PATTERN_DEFAULT;
		}
		int p;
		if (index < patterns.length) {
			p = index;
		} else {
			p = index % patterns.length;
		}
		return this.patterns[p];
	}

	public static Paint getPatternPaint(Color backColor, byte pattern) {
		Paint paint = null;
		Rectangle rect;
		int x = 0, y = 0;
		BufferedImage tempbi;
		Graphics2D tempG=null;
		Color cb = backColor, cf = Color.black;
		switch (pattern) {
		case PATTERN_H_THIN_LINE: // ���ͼ����ˮƽϸ��
			rect = new Rectangle(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(0, 1, 6, 1);
			tempG.drawLine(0, 3, 6, 3);
			tempG.drawLine(0, 5, 6, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_H_THICK_LINE: // ���ͼ����ˮƽ����
			rect = new Rectangle(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.5f));
			tempG.drawLine(0, 2, 6, 2);
			tempG.drawLine(0, 5, 6, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_V_THIN_LINE: // ���ͼ������ֱϸ��
			rect = new Rectangle(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(1, 0, 1, 6);
			tempG.drawLine(3, 0, 3, 6);
			tempG.drawLine(5, 0, 5, 6);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_V_THICK_LINE: // ���ͼ������ֱ����
			rect = new Rectangle(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.5f));
			tempG.drawLine(2, 0, 2, 6);
			tempG.drawLine(5, 0, 5, 6);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THIN_SLASH: // ���ͼ����ϸб��
			rect = new Rectangle(x + 1, y + 1, 3, 3);
			tempbi = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 3, 3);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(0, 0, 3, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THICK_SLASH: // ���ͼ������б��
			rect = new Rectangle(x + 1, y + 1, 4, 4);
			tempbi = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasing(tempG);
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 4, 4);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(0, 0, 4, 4);
			tempG.drawLine(3, -1, 5, 1);
			tempG.drawLine(-1, 3, 1, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THIN_BACKSLASH: // ���ͼ����ϸ��б��
			rect = new Rectangle(x + 1, y + 1, 3, 3);
			tempbi = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 3, 3);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(2, 0, -1, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THICK_BACKSLASH: // ���ͼ�����ַ�б��
			rect = new Rectangle(x + 1, y + 1, 4, 4);
			tempbi = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasing(tempG);
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 4, 4);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(4, 0, 0, 4);
			tempG.drawLine(-1, 1, 1, -1);
			tempG.drawLine(3, 5, 5, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THIN_GRID: // ���ͼ����ϸ����
			rect = new Rectangle(x + 1, y + 1, 3, 3);
			tempbi = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 3, 3);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
			tempG.drawLine(1, 0, 1, 3);
			tempG.drawLine(0, 1, 3, 1);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THICK_GRID: // ���ͼ����������
			rect = new Rectangle(x + 1, y + 1, 5, 5);
			tempbi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 5, 5);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
			tempG.drawLine(3, 0, 3, 5);
			tempG.drawLine(0, 3, 5, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THIN_BEVEL_GRID: // ���ͼ����ϸб����
			rect = new Rectangle(x + 1, y + 1, 5, 5);
			tempbi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 5, 5);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
			tempG.drawLine(0, 0, 5, 5);
			tempG.drawLine(0, 5, 5, 0);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_THICK_BEVEL_GRID: // ���ͼ������б����
			rect = new Rectangle(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasing(tempG);
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(0, 0, 6, 6);
			tempG.drawLine(0, 6, 6, 0);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_DOT_1: // ���ͼ����ϡ���
			rect = new Rectangle(x, y, 12, 12);
			tempbi = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasing(tempG);
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 12, 12);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(2, 3, 2, 3);
			tempG.drawLine(8, 9, 8, 9);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_DOT_2: // ���ͼ������ϡ��
			rect = new Rectangle(x, y, 12, 12);
			tempbi = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 12, 12);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(2, 3, 2, 3);
			tempG.drawLine(6, 11, 6, 11);
			tempG.drawLine(10, 7, 10, 7);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_DOT_3: // ���ͼ�������ܵ�
			rect = new Rectangle(x, y, 9, 9);
			tempbi = new BufferedImage(9, 9, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasing(tempG);
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 9, 9);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(2, 2, 2, 2);
			tempG.drawLine(5, 8, 5, 8);
			tempG.drawLine(8, 5, 8, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_DOT_4: // ���ͼ�������ܵ�
			rect = new Rectangle(x, y, 4, 4);
			tempbi = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 4, 4);
			tempG.setColor(cf);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(1, 3, 1, 3);
			tempG.drawLine(3, 1, 3, 1);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_SQUARE_FLOOR: // ���ͼ����������ذ�ש
			rect = new Rectangle(0, 0, 8, 8);
			tempbi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 8, 8);
			tempG.setColor(cf);
			tempG.fillRect(0, 0, 4, 4);
			tempG.fillRect(4, 4, 4, 4);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_DIAMOND_FLOOR: // ���ͼ�������εذ�ש
			rect = new Rectangle(x + 1, y + 1, 8, 8);
			tempbi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 8, 8);
			tempG.setColor(cf);
			int[] xs = { 4, 0, 4, 8 };
			int[] ys = { 0, 4, 8, 4 };
			tempG.fillPolygon(xs, ys, 4);
			paint = new TexturePaint(tempbi, rect);
			break;
		case PATTERN_BRICK_WALL: // ���ͼ����שǽ
			rect = new Rectangle(x + 1, y + 1, 12, 12);
			tempbi = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(cb);
			tempG.fillRect(0, 0, 12, 12);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.setColor(cf);
			tempG.drawLine(0, 0, 12, 0);
			tempG.drawLine(0, 3, 12, 3);
			tempG.drawLine(0, 6, 12, 6);
			tempG.drawLine(0, 9, 12, 9);
			tempG.drawLine(2, 0, 2, 3);
			tempG.drawLine(8, 3, 8, 6);
			tempG.drawLine(2, 6, 2, 9);
			tempG.drawLine(8, 9, 8, 12);
			paint = new TexturePaint(tempbi, rect);
			break;
		}
		if(tempG!=null){
			tempG.dispose();
		}
		return paint;
	}

	// ����ͼƬ�������
	protected static void setGraphAntiAliasing(Graphics2D g) {
		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND, 0.1f));
//		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_ON);
	}

	/**
	 * ������ɫ
	 * 
	 * @param color
	 *            ��ɫ
	 */
	public void addColor(int color) {
		ensureCapacity(size + 1);
		colors[size++] = color;
	}

	/**
	 * ������ɫ
	 * 
	 * @param index
	 *            λ��
	 * @param color
	 *            ��ɫ
	 */
	public void addColor(int index, int color) {
		ensureCapacity(size + 1);
		int[] colors = this.colors;
		int numtomove = size - index;
		System.arraycopy(colors, index, colors, index + 1, numtomove);
		colors[index] = color;
		size++;
	}

	/**
	 * ȡ�����е���ɫ��
	 */
	public int size() {
		return this.size;
	}

	/**
	 * ��������С����ɫ��
	 */
	public void trimToSize() {
		int[] colors = this.colors;
		if (size < colors.length) {
			this.colors = new int[size];
			System.arraycopy(colors, 0, this.colors, 0, size);

			this.patterns = new byte[size];
			System.arraycopy(patterns, 0, this.patterns, 0, size);
		}
	}

	/**
	 * ��������е�������ɫ
	 */
	public void clear() {
		this.size = 0;
	}

	/**
	 * ȡ��ָ������ɫ����
	 * 
	 * @return ��ɫ����
	 */
	public static Palette readColor(String color) {
		if (color == null || color.trim().length() == 0) {
			return null;
		}
		// if( ExtCellSet.get().getType()==ExtCellSet.LIC_IDE
		// ){//IDEÿ�����¶�ȡ,��ʹ�༭���������Ч
		colorMap = readPalettes();
		// }
		return (Palette) colorMap.get(color);
	}

	/**
	 * �������ļ�װ�س���ɫ��ӳ��� String ��ɫ������ ���� Palette ��ɫ�����
	 * 
	 * @param fileProperty
	 *            Properties
	 * @return Map
	 */
	public static Map loadPalettes(Properties fileProperty) {
		HashMap map = new HashMap();
		Enumeration keys = fileProperty.keys();
		while (keys.hasMoreElements()) {
			String name = (String) keys.nextElement();
			ArgumentTokenizer st = new ArgumentTokenizer(
					fileProperty.getProperty(name), ',');
			int count = st.countTokens();
			Palette pl = new Palette(count);
			pl.size = 0;
			while (st.hasNext()) {
				StringTokenizer define = new StringTokenizer(st.next(), "@");
				pl.colors[pl.size] = Integer.parseInt(define.nextToken());
				if (define.hasMoreTokens()) {
					pl.patterns[pl.size] = Byte.parseByte(define.nextToken());
				}
				pl.size++;
			}
			map.put(name, pl);
		}
		return map;
	}

	private static Map readPalettes() {
		Map all = new HashMap();
		try {
			Properties config = new Properties();
			InputStream is = null;
			String relativePath = com.scudata.common.GC.PATH_CONFIG
					+ "/color.properties";
			File f = new File(
					com.scudata.common.GM.getAbsolutePath(relativePath));
			if (f.exists()) {
				is = new FileInputStream(f);
			} else {
				is = Palette.class.getResourceAsStream(relativePath);
			}

			if (is == null) {
				is = Palette.class
						.getResourceAsStream("/config/color.properties");
			}
			if (is == null)
				return all;// û����ɫ�ļ�
			config.load(is);
			return loadPalettes(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return all;
	}

	/**
	 * ȡ��ȱʡ����ɫ����
	 * 
	 * @return ��ɫ����
	 */
	public static Palette getDefaultPalette() {
		return defaultPalette;
	}

	private static Palette getDefaultPalette2() {
//		ע���ʼsizeҪ������addColor�ĸ���ƥ�䣬������ɫѭ������
		
		int size = 7;
		Palette pl = new Palette( size );
		       
		pl.addColor(0x5b9bd5);
		pl.addColor(0xa5cb50);
		pl.addColor(0xfffb12);
		pl.addColor(0xf39725);
		pl.addColor(0xe34b48);
		pl.addColor(0x8f68b7);
		pl.addColor(0x91cbeb);
		
//		pl.addColor(MakeRGBColor(255, 0, 0, 255));
//		pl.addColor(MakeRGBColor(255, 128, 0, 255));
//		pl.addColor(MakeRGBColor(192, 255, 0, 255));
//		pl.addColor(MakeRGBColor(0, 0, 128, 255));
//		pl.addColor(MakeRGBColor(128, 0, 128, 255));
//		pl.addColor(MakeRGBColor(255, 0, 128, 255));
//		pl.addColor(MakeRGBColor(0, 128, 128, 255));
//		pl.addColor(MakeRGBColor(128, 128, 128, 255));
//		pl.addColor(MakeRGBColor(0, 255, 255, 255));
//		pl.addColor(MakeRGBColor(192, 192, 192, 255));
//		pl.addColor(MakeRGBColor(255, 128, 128, 255));
//		pl.addColor(MakeRGBColor(0, 255, 128, 255));
//		pl.addColor(MakeRGBColor(192, 255, 128, 255));
//		pl.addColor(MakeRGBColor(255, 255, 0, 255));
//		pl.addColor(MakeRGBColor(255, 255, 128, 255));
//		pl.addColor(MakeRGBColor(128, 0, 255, 255));
//		pl.addColor(MakeRGBColor(255, 0, 255, 255));
//		pl.addColor(MakeRGBColor(0, 128, 255, 255));
//		pl.addColor(MakeRGBColor(128, 128, 255, 255));
//		pl.addColor(MakeRGBColor(255, 128, 255, 255));
//		pl.addColor(MakeRGBColor(192, 255, 255, 255));
//		pl.addColor(MakeRGBColor(255, 0, 0, 255));
//		pl.addColor(MakeRGBColor(0, 255, 0, 255));
//		pl.addColor(MakeRGBColor(0, 0, 255, 255));
//		pl.addColor(MakeRGBColor(0, 128, 0, 255));
//		pl.addColor(MakeRGBColor(255, 255, 255, 255));

		return pl;
	}

	private static int MakeRGBColor(int r, int g, int b, int a) {
		return (int) (((a & 0xFF) << 24) | ((r & 0xFF) << 16)
				| ((g & 0xFF) << 8) | (b & 0xFF));
	}

}
