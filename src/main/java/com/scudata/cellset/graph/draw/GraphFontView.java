package com.scudata.cellset.graph.draw;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

import com.scudata.chart.Utils;
import com.scudata.common.*;

/**
 * ͼ��������ͼ
 * ��װ��������������Ϣ��������ͼ
 * @author Joancy
 *
 */
public class GraphFontView {
	public static final byte FONT_TITLE = 0;
	public static final byte FONT_LEGEND = 1;
	public static final byte FONT_XLABEL = 2;
	public static final byte FONT_YLABEL = 3;
	public static final byte FONT_XTITLE = 4;
	public static final byte FONT_YTITLE = 5;
	public static final byte FONT_VALUE = 6;
	public static final byte TEXT_FIXED = 0; // ���ֲ���У׼
	public static final byte TEXT_ON_TOP = 1; // ����λ�����ĵ��Ϸ�
	public static final byte TEXT_ON_BOTTOM = 2; // ����λ�����ĵ��·�
	public static final byte TEXT_ON_LEFT = 3; // ����λ�����ĵ����
	public static final byte TEXT_ON_RIGHT = 4; // ����λ�����ĵ��ұ�
	public static final byte TEXT_ON_CENTER = 5; // ����λ�����ĵ�

	DrawBase db;
	public String text = "";
	public String text2 = ""; // ����ͼ��Y2�����
	public Font font;
	public Color color;
	public boolean vertical = false;
	public int angle; // ������ת�Ƕȣ�ע�⵱ǰ��֧����ת0��90�ȣ�����Ķ���ȡ90�ȵ���

	// ���ֵ�ȱʡ����λ��
	byte textPosition = TEXT_FIXED;
	private boolean allowIntersect = true; // �Ƿ��������ڵ���ֵ�ص����

	// Rectangle PA = null; //��һ������ı����� PreArea
	ArrayList fontRects = new ArrayList(); // ����������ľ��ζ�hold����ֹ�ص����ʱҪ��������ռ����Ƚϣ��ϰ취��ֻ����������

	/**
	 * ����������ͼ����
	 * @param drawBase ͼ�λ���ʵ��
	 */
	public GraphFontView(DrawBase drawBase) {
		this.db = drawBase;
		allowIntersect = drawBase.egp.isShowOverlapText();
	}

	/**
	 * ��������뷴��
	 * @param direction ���뷽ʽ
	 * @return ������뷽ʽ������Ҫ�������Ȼ�ǵ�ǰ���뷽ʽ
	 */
	public static byte reverseDirection(byte direction) {
		switch (direction) {
		case TEXT_FIXED:
			return TEXT_FIXED;
		case TEXT_ON_CENTER:
			return TEXT_ON_CENTER;
		case TEXT_ON_TOP:
			return TEXT_ON_BOTTOM;
		case TEXT_ON_BOTTOM:
			return TEXT_ON_TOP;
		case TEXT_ON_LEFT:
			return TEXT_ON_RIGHT;
		case TEXT_ON_RIGHT:
			break;
		}
		return TEXT_ON_LEFT;
	}

	/**
	 * �����������
	 * @param font �������
	 */
	public void setFont(Font font) {
		this.font = font;
	}

	/**
	 * ���û����ı�ʱ���Ƿ��������������ص�
	 * @param allowIntersect �����ص�
	 */
	public void setIntersect(boolean allowIntersect) {
		this.allowIntersect = allowIntersect;
	}

	/**
	 * ������ɫ
	 * @param color ��ɫ
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * ���������Ƿ���������
	 * @param vertical ��������
	 */
	public void setVertical(boolean vertical) {
		this.vertical = vertical;
	}

	/**
	 * �����ı�����ת�Ƕ�
	 * @param angle �Ƕ�
	 */
	public void setAngle(int angle) {
		if(Math.abs(angle)>90) {
			int tmp = Math.abs(angle) % 90;
			Logger.warn("Rotate angle must between [0,90], "+tmp+" will be used instead of "+angle);
			this.angle = tmp;
		}else {
			this.angle = angle;
		}
	}

	/**
	 * ������ʾ�ı�
	 * @param text �ı���
	 */
	public void setText(String text) {
		if (text == null) {
			return;
		}
		if (db.egp.is2YGraph()) {
			int pos = -1;
			//ֻ׼���÷ֺţ����ǵ��������÷ֿ��ı��ʽ
			if (pos < 0) {
				pos = text.indexOf(';');
			}
			if (pos < 0) {
				this.text = text;
			} else {
				this.text = text.substring(0, pos);
				this.text2 = text.substring(pos + 1);
			}
		} else {
			this.text = text;
		}
	}

	/**
	 * ����������Բ�β���ʱ�ķ�λ�������״�ͼ
	 * @param pos ��λ
	 */
	public void setTextPosition(byte pos) {
		this.textPosition = pos;
	}
	
	/**
	 * ������������ǰ�ı�
	 * @param x ������
	 * @param y ������
	 */
	public void outText(double x, double y) {
		outText(x, y, text);
	}

	/**
	 * �ú�������X���ǩ����interval�����Ƿ�����
	 * 
	 * @param x
	 *            int ������
	 * @param y
	 *            int ������
	 * @param text
	 *            String �ı�
	 * @param visible
	 *            boolean �Ƿ�ɼ�
	 */
	public void outText(double x, double y, String text, boolean visible) {
		if (visible) {
			outText(x, y, text);
		}
	}

	/**
	 * ����ı�
	 * @param x ������
	 * @param y ������
	 * @param text �ı�
	 * @param visible �ɼ�
	 * @param direction ��λ
	 */
	public void outText(double x, double y, String text, boolean visible,
			byte direction) {
		if (visible) {
			outText(x, y, text, direction);
		}
	}

	/**
	 * ����ı�
	 * @param text 
	 *            String �ı�
	 * @param x
	 *            double �ı����ʱ�����½�x
	 * @param y
	 *            double �ı����ʱ�����½�y
	 */
	public boolean outText(double x, double y, String text) {
		return outText(x, y, text, textPosition);
	}

	private Rectangle intersects(Rectangle newRect) {
		// Ҫ�Ӻ���ǰ�ң��ҵ����һ���ص�����
		for (int i = fontRects.size() - 1; i >= 0; i--) {
			Rectangle rect = (Rectangle) fontRects.get(i);
			if (rect.intersects(newRect)) {
				return rect;
			}
		}
		return null;
	}

	/**
	 * ����ı�
	 * @param x ������
	 * @param y ������
	 * @param text �ı�
	 * @param tmpColor ��ɫ
	 * @return ������ɷ���true�����򷵻�false
	 */
	public boolean outText(double x, double y, String text, Color tmpColor) {
		return outText(x, y, text, textPosition, tmpColor);
	}

	/**
	 * ����ı�
	 * @param x ������
	 * @param y ������
	 * @param text �ı�
	 * @param direction ��λ
	 * @return ������ɷ���true�����򷵻�false
	 */
	public boolean outText(double x, double y, String text, byte direction) {
		return outText(x, y, text, direction, color);
	}

	/**
	 * ����ı�
	 * @param x ������
	 * @param y ������
	 * @param text �ı�
	 * @param direction ��λ 
	 * @param textColor ��ɫ
	 * @return �������ı�����true�����򷵻�false
	 */

	public boolean outText(double x, double y, String text, byte direction,
			Color textColor) {
		if (text == null || text.trim().length() == 0) {
			return false;
		}
		if (font.getSize() == 0) {
			return false;
		}
		Rectangle TA = getTextSize(text); // This Area
		if (vertical || angle == 0) {
			TA = getTextSize(text);
//		} else if(angle%90!=0){
//			vertical = true; // ��������ת�Ƕȵ�ʱ��,�ü���������������Ƿ��ཻ
//			TA = getTextSize(text);
//			vertical = false;
		}
		FontMetrics fm = db.g.getFontMetrics(font);
		Point rop = getActualTextPoint((int)x, (int)y, direction, TA, fm, text); 
		TA.x = rop.x;
		TA.y = rop.y;
		if (textColor != color) { // �������ɫ���༭����ɫ��һ��ʱ����ʾʹ����ϵ�еĶ�̬��ɫ�����ֱ�ǩ����״̬��
			// ���ж��ص��������ص�ʱ����������΢����λ��������ɫ��ͬ����λ���Կ��Էֱ����
			Rectangle rect = intersects(TA);
			if (rect != null) {
				if (TA.y <= rect.y) {
					TA.y = rect.y - rect.height;
				} else {
					TA.y = rop.y + rect.height + db.VALUE_RADIUS + 2;
				}
			}
		} else {
			if (!allowIntersect && intersects(TA) != null) {
				return false;
			}
		}
		if (!fontRects.contains(TA)) {
			fontRects.add(TA);
		}

		db.g.setColor(textColor);
		db.g.setFont(font);
//		�����н�ֹ�ر�anti����֤���е��ߺ����ֶ���ƽ����
//		����Ҫ��ƽ���������������������ԣ��ĳɻ�������ʱ�رվ�ݣ��������ָ�ƽ��
		Composite com = db.g.getComposite();
		Utils.setGraphAntiAliasingOff(db.g);
//		db.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_OFF);
		// ����ʹ��������������ԭ�� 1,͸������ʱ�������ͻ���������2���������ĳ���������ɫ������Ȼ������

		x = TA.x;
		y = TA.y;
		if (vertical) {
			for (int i = 0; i < text.length(); i++) {
				String ch = text.substring(i, i + 1);
				if ("()[]{}".indexOf(ch) >= 0) {
					AffineTransform at = db.g.getTransform();
					double yy = y + i * (fm.getAscent() + 2);
					if ("([{".indexOf(ch) >= 0) {
						yy -= fm.getAscent() / 2;
					} else {
						yy -= fm.getAscent();
					}
					AffineTransform at1 = AffineTransform.getTranslateInstance(
							x + 2, yy);
					db.g.transform(at1);
					double rotateAngle = Math.toRadians(90);
					AffineTransform at2 = AffineTransform.getRotateInstance(
							rotateAngle, 0, 0);
					db.g.transform(at2);
					db.g.setStroke(new BasicStroke(1f));
					db.g.drawString(ch, 0, 0);
					db.g.setTransform(at);
				} else {
					db.g.drawString(ch, (int)x, (int)(y + i * (fm.getAscent() + 2)));
				}
			}
		} else if (angle == 0) {
			db.g.drawString(text, (int)x, (int)y);
		} else {
			double rotateAngle = Math.toRadians(-angle);
			AffineTransform at = db.g.getTransform();
			AffineTransform at1 = AffineTransform.getRotateInstance(
					rotateAngle, x, y);
			db.g.transform(at1);
			db.g.setStroke(new BasicStroke(1f));
			db.g.drawString(text, (int)x, (int)y);

			db.g.setTransform(at);
		}
		
		Utils.setGraphAntiAliasingOn(db.g);

//		���������ϣ��ٻָ�ƽ��
//		db.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_ON);

		db.g.setComposite(com);
		db.g.setStroke(new BasicStroke(0.00001f));
		return true;
	}


	private Point getActualTextPoint(int x, int y, byte direction,
			Rectangle TA, FontMetrics fm, String text) {
		if (direction == TEXT_FIXED) {
			return new Point(x, y);
		}
		if (vertical) {
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x -= TA.width / 2;
				break;
			case TEXT_ON_TOP:
				x -= TA.width / 2;
				y -= TA.height;
				break;
			case TEXT_ON_LEFT:
				x -= TA.width;
				y -= TA.height / 2;
				break;
			case TEXT_ON_RIGHT:
				y -= TA.height / 2;
				break;
			case TEXT_ON_CENTER:
				x -= TA.width / 2;
				y -= TA.height / 2;
				break;
			}
			y += fm.getAscent() + 2;
		} else if (angle == 0) {
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x -= TA.width / 2;
				y += TA.height;
				break;
			case TEXT_ON_TOP:
				x -= TA.width / 2;
				break;
			case TEXT_ON_LEFT:
				x -= TA.width;
				y += TA.height / 2;
				break;
			case TEXT_ON_RIGHT:
				y += TA.height / 2;
				break;
			case TEXT_ON_CENTER:
				x -= TA.width / 2;
				y += TA.height / 2;
				break;
			}
		} else if(angle==90){
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x += TA.width / 2;
				y += TA.height;
				break;
			case TEXT_ON_TOP:
				x += TA.width / 2;
				break;
			case TEXT_ON_LEFT:
				y += TA.height / 2;
				break;
			case TEXT_ON_RIGHT:
				x += TA.width;
				y += TA.height / 2;
				break;
			case TEXT_ON_CENTER:
				x += TA.width / 2;
				y += TA.height / 2;
				break;
			}
		}else {
//			��ת�ǶȽ�֧��0-90��
			double rotateAngle = Math.toRadians(angle);
//			Rectangle tmpTA = getTextSize(text);
			
			FontMetrics tfm = db.g.getFontMetrics(font);
			int tw = tfm.stringWidth(text);
			int th = tfm.getAscent();
			double  dotLeft = th * Math.sin(rotateAngle );
			double  dotRight = tw * Math.cos(rotateAngle );
			double halfW = TA.width/2;
			double halfH = TA.height/2;
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x -= dotRight;
				y += TA.height;
				break;
			case TEXT_ON_TOP:
//				x += halfW-dotLeft;
				break;
			case TEXT_ON_LEFT:
				x -= dotRight;
				y += halfH;
				break;
			case TEXT_ON_RIGHT:
				x += dotLeft;
				y += halfH;
				break;
			case TEXT_ON_CENTER:
				x -= halfW-dotLeft;
				y += halfH;
				break;
			}
		}

		int gap = 2;
		switch (direction) {
		case TEXT_ON_BOTTOM:
			y += gap;
			break;
		case TEXT_ON_TOP:
			y -= gap;
			break;
		case TEXT_ON_LEFT:
			x -= gap;
			break;
		case TEXT_ON_RIGHT:
			x += gap;
			break;
		}

		return new Point(x, y);
	}

	/**
	 * ��ȡ��ǰ�ı���ռ����
	 * @return ���������ı�����
	 */
	public Rectangle getTextSize() {
		return getTextSize(text);
	}

	/**
	 * ��ȡָ���ı��ٵ�ǰ�����µ���ռ����
	 * @param text �ı�
	 * @return ��������
	 */
	public Rectangle getTextSize(String text) {
		if (text == null) {
			return new Rectangle();
		}
		if (vertical) {
			return getVerticalArea(text);
		}
		if (angle % 180== 0) {
			return getHorizonArea(text);
		}
		return getRotationArea(text);
	}

	private Rectangle getVerticalArea(String text) {
		if (!StringUtils.isValidString(text)) {
			text = "A";
		}
		Rectangle area = new Rectangle();
		FontMetrics fm = db.g.getFontMetrics(font);
		int hh = fm.getAscent() + 2; // �������ּ���2����ļ�϶
		area.width = fm.stringWidth(text.substring(0, 1));
		area.height = hh * text.length();
		return area;
	}

	private Rectangle getHorizonArea(String text) {
		Rectangle area = new Rectangle();
		FontMetrics fm = db.g.getFontMetrics(font);
		int hw = fm.stringWidth(text);
		int hh = fm.getAscent();
		area.width = hw;
		area.height = hh - fm.getLeading() - 2;// ������˵Ascent���Ѿ������ֻ����ϲ��ռ��ˣ���ʵ���Ƕ��ˣ�΢��һ��2
		return area;
	}

	private Rectangle getRotationArea(String text) {
		if (!StringUtils.isValidString(text)) {
			text = "A";
		}

		Rectangle area = new Rectangle();
		FontMetrics fm = db.g.getFontMetrics(font);
		int hw = fm.stringWidth(text);
		int hh = fm.getAscent();
		double djx =  Math.sqrt(hw * hw + hh * hh); // �Խ��߳���
		double textAngle = Math.atan(hh / (hw * 1.0f)), tmpAngle; // ���ֱ���ĶԽ�����ױߵĽǶ�
																	// ��λ������
		int aw, ah;
		// �����Ǿ��Σ�������hw��ת�Ƕȵõ��߶ȣ����öԽ���ȥ��ת
		tmpAngle = textAngle + Math.toRadians(angle);
		ah = (int) (djx * Math.sin(tmpAngle));

		tmpAngle = Math.toRadians(angle) - textAngle;
		aw = (int) (djx * Math.cos(tmpAngle));

		if (aw == 0) {
			aw = fm.stringWidth(text.substring(0, 1));
		}
		if (ah == 0) {
			ah = hh;
		}
		area.width = Math.abs( aw );
		area.height = Math.abs( ah );
		return area;
	}

}
