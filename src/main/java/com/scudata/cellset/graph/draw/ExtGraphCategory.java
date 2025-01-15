package com.scudata.cellset.graph.draw;

import java.util.*;

import com.scudata.util.*;

/**
 * ͼ�η��ඨ����չ����
 * 
 * @author Joancy
 *
 */
public class ExtGraphCategory implements Comparable{
  /** ������ʽ ���� ʱ��״̬ͼ������ʽ�����ͼ����̱�ͼ����Ŀ*/
  private Object name;

  /**�˷����µ�ϵ�� (ExtGraphSery���� ExtGraphTimeStatus)*/
  private ArrayList series;

  /**
   * �������
   * @return Object ��������
   */
  public Object getName() {
	return name;
  }

  /**
   * ȡ�������Ƶ��ַ�����ʾ��
   * @return �ַ�������
   */
  public String getNameString(){
	return Variant.toString(name);
  }

  /**
   * ���ܵ�ǰ�����µ�ϵ����ֵ
   * @return ����ֵ
   */
  public double getSumSeries() {
	double d = 0;
	for (int i = 0; i < series.size(); i++) {
	  ExtGraphSery egs = (ExtGraphSery) series.get(i);
	  d += egs.getValue();
	}
	return d;
  }
  /**
   * ���÷��������д���0����ֵ������
   * �ѻ�ͼ���õ�
   * @return double ��������ֵ
   */
  public double getPositiveSumSeries() {
	double d = 0;
	for (int i = 0; i < series.size(); i++) {
	  ExtGraphSery egs = (ExtGraphSery) series.get(i);
	  double v = egs.getValue();
	  if( v<=0 ){
	continue;
	  }
	  d += egs.getValue();
	}
	return d;
  }

  /**
   * ͳ��ϵ��ֵΪ���Ļ���ֵ
   * @return ��������ֵ
   */
  public double getNegativeSumSeries() {
	double d = 0;
	for (int i = 0; i < series.size(); i++) {
	  ExtGraphSery egs = (ExtGraphSery) series.get(i);
	  double v = egs.getValue();
	  if( v>=0 ){
	continue;
	  }
	  d += egs.getValue();
	}
	return d;
  }

  /**
   * ȡϵ�ж���
   * @param seriesName ϵ������
   * @return ϵ�ж���
   */
  public ExtGraphSery getExtGraphSery( Object seriesName ){
	ExtGraphSery egs;
	for( int i=0; i<series.size(); i++ ){
	  egs = (ExtGraphSery)series.get(i);
	  String name = egs.getName();
	  if(( name!=null && name.equals(seriesName)) || (name==null && seriesName==null)){
	return egs;
	  }
	}
	egs = new ExtGraphSery();
	egs.setName(Variant.toString(seriesName) );
	return egs;
  }
  /**
   * ��ñ������µ�ϵ��
   * @return ArrayList (ExtGraphSery���� ExtGraphTimeStatus) �������µ�ϵ��
   */
  public ArrayList getSeries() {
	return series;
  }

  /**
   * ��������
   * @param name ��������
   */
  public void setName(Object name) {
	this.name = name;
  }

  /**
   * ��ñ������µ�ϵ��
   * @param series (ExtGraphSery���� ExtGraphTimeStatus) �������µ�ϵ��
   */
  public void setSeries(ArrayList series) {
	this.series = series;
  }

  /**
   * ʵ�ֱȽϺ���
   * ����ϵ�еĻ���ֵ���бȽ�
   * @param o Object ������� 
   * @return int �ȽϽ��
   */
  public int compareTo(Object o) {
	ExtGraphCategory otherEgc = (ExtGraphCategory)o;
	Double self = new Double(this.getSumSeries());
	Double other = new Double( otherEgc.getSumSeries() );
	return self.compareTo( other );
  }

  public String toString() {
	  return name+":"+getSumSeries();
  }
}
