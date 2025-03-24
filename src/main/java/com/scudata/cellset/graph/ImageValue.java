package com.scudata.cellset.graph;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.*;

import com.scudata.common.*;

/**
 * ͳ��ͼ����ͼ���ͼƬ���ݵ��������
 * @author Joancy
 *
 */
public class ImageValue implements ICloneable, Externalizable, IRecord {
	private static final long serialVersionUID = 1L;

	private byte imageType;
	private String html;
	private byte[] value;//�����Ǻθ�ʽ����Ҫ����ͼ���������ڵ������ߴ�ӡ��ͼ���豸
	private String customHtml;

	private byte[] flashXml;
	
	private transient IRedraw redraw = null;
	
	public void setIRedraw(IRedraw ir){
		redraw = ir;
	}
	
	public IRedraw getIRedraw() {
		return redraw;
	}
	
	public boolean canRedraw(){
		return redraw!=null;
	}
	
	public void repaint(Graphics g, int w, int h){
		redraw.repaint((Graphics2D)g, w, h);
	}
	/**
	 * ȱʡ���캯��
	 */
	public ImageValue() {
	}

	/**
	 * ָ�������Ĺ��캯��
	 * @param value ͼ�����ݵ��ֽ�����
	 * @param type ͼ�θ�ʽ
	 * @param html ͼ�ζ�Ӧ�ĳ�����
	 */
	public ImageValue( byte[] value, byte type, String html ) {
		this.value = value;
		this.imageType = type;
		this.html = html;
	}

	/**
	 * ָ�������Ĺ��캯��
	 * @param value ͼ�����ݵ��ֽ�����
	 * @param type ͼ�θ�ʽ
	 * @param html ͼ�ζ�Ӧ�ĳ�����
	 * @param flashXml flash��ʽ��ͼ����������
	 * ����flashͼ��û����java��ֱ�ӻ��ƣ�����flash��ʽʱ�ᱣ�����ݸ���
	 * һ��Ϊ��ͨͼ������value�����ڽ������
	 * һ��Ϊflash��������flashXml���������
	 */
	public ImageValue( byte[] value, byte type, String html, byte[] flashXml ) {
		this.value = value;
		this.imageType = type;
		this.html = html;
		this.flashXml = flashXml;
	}

	/**
	 * �����ֽ����ݵ�ͼ������
	 * @param value ͼ������
	 */
	public void setValue( byte[] value ) {
		this.value = value;
	}

	/**
	 * ����ͼƬ�ĸ�ʽ
	 * @param type ͼƬ��ʽ ֵΪ��IGraphProperty.IMAGE_XXX
	 */
	public void setImageType( byte type ) {
		this.imageType = type;
	}

	/**
	 * ͳ��ͼʱ������г����ӣ������������ý���
	 * @param html �������ı�
	 */
	public void setHtml( String html ) {
		this.html = html;
	}

	/**
	 * ȡͼƬ��������
	 * @return ͼƬ����
	 */
	public byte[] getValue() {
		return this.value;
	}

	/**
	 * ȡͼƬ��ʽ
	 * @return ͼƬ��ʽ
	 */
	public byte getImageType() {
		return this.imageType;
	}

	/**
	 * ȡ�������ı�
	 * @return ����������
	 */
	public String getHtml() {
		return this.html;
	}

	/**
	 * Ҳ�����Զ��峬���ӣ������Զ�������
	 * @param s ����������
	 */
	public void setCustomHtml( String s ) {
		customHtml = s;
	}

	/**
	 * ȡ�Զ��峬��������
	 * @return �Զ��峬����
	 */
	public String getCustomHtml() {
		return customHtml;
	}

	/**
	 * ���ͼƬ����flash������flash����
	 * @param bytes flash���ݵ��ֽ�����
	 */
	public void setFlashXml( byte[] bytes ) {
		this.flashXml = bytes;
	}

	/**
	 * ȡflash���ݵ��ֽ�����
	 * @return �ֽ�����
	 */
	public byte[] getFlashXml() {
		return this.flashXml;
	}

	/**
	 * svg����flash����ͬʱ���ڣ�ʵ��������ͬ�����÷���
	 * ���洢�����ǹ��õ�
	 * @param bytes byte[] svg��ʽ����������
	 */
	public void setSvgBytes( byte[] bytes ) {
		this.flashXml = bytes;
	}

	/**
	 * ȡsvg��ʽ����������
	 * @return �ֽ�����
	 */
	public byte[] getSvgBytes() {
		return this.flashXml;
	}

	/**
	 * ʵ�����л��ӿ�
	 */
	public void writeExternal( ObjectOutput out ) throws IOException {
		out.writeByte( 3 ); //Macro�İ汾
		out.write( imageType );
		out.writeObject( value );
		out.writeObject( html );
		out.writeObject( flashXml );
		out.writeObject( customHtml );
	}

	/**
	 * ʵ�����л��ӿ�
	 */
	public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		byte version = in.readByte();
		imageType = in.readByte();
		value = ( byte[] ) in.readObject();
		html = ( String ) in.readObject();
		if ( version > 1 ) {
			flashXml = ( byte[] ) in.readObject();
		}
		else if ( imageType == GraphProperty.IMAGE_FLASH ) {
			flashXml = value;
		}
		if( version > 2 ) customHtml = ( String ) in.readObject();
	}

	/**
	 * ʵ��IRecord�ӿ�
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeByte( imageType );
		out.writeBytes( value );
		out.writeString( html );
		out.writeBytes( flashXml );
		out.writeString( customHtml );
		return out.toByteArray();
	}

	/**
	 * ʵ��IRecord�ӿ�
	 */
	public void fillRecord( byte[] buf ) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord( buf );
		imageType = in.readByte();
		value = in.readBytes();
		html = in.readString();
		if ( in.available() > 0 ) {
			flashXml = in.readBytes();
		}
		else if ( imageType == GraphProperty.IMAGE_FLASH ) {
			flashXml = value;
		}
		if( in.available() > 0 ) {
			customHtml = in.readString();
		}
	}

	/**
	 * ��ȿ�¡һ��ͼ�����ݶ���
	 * @return ��¡��Ķ���
	 */
	public Object deepClone() {
		ImageValue v = new ImageValue();
		v.imageType = this.imageType;
		if ( this.value != null ) {
			v.value = ( byte[] )this.value.clone();
		}
		if ( this.html != null ) {
			v.html = new String( this.html );
		}
		if ( this.flashXml != null ) {
			v.flashXml = this.flashXml;
		}
		if ( this.customHtml != null ) {
			v.setCustomHtml( this.customHtml );
		}
		return v;
	}

}
