package com.scudata.server.unit;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.*;
import com.scudata.parallel.*;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;

import java.text.*;

/**
 * ��ͼ�η�����������
 * ���ܵ���GM��ide���࣬ �ֻ�
 * 
 * @author Joancy
 *
 */
public abstract class UnitServer implements IServer {
	public static UnitServer instance;

	protected static boolean isIniting = false;
	static int objectId = 0;
	static Object idLock = new Object();

	
	/**
	 * ���û�������
	 */
	public abstract void setRaqsoftConfig(RaqsoftConfig rc);
	/**
	 * ��ȡ�����������ö���
	 */
	public abstract RaqsoftConfig getRaqsoftConfig();
	
	/**
	 * ���÷�����״̬������
	 */
	public abstract void setStartUnitListener(StartUnitListener listen);
	
	/**
	 * ��ȡ���㻷��������
	 * @return
	 */
	public abstract UnitContext getUnitContext();

	/**
	 * �ֻ��������ڲ�����Ψһ���
	 * ���ڴ���ŵȸ��ֲ���ͬ�ŵĵط�
	 * @return ��������Ψһ���
	 */
	public static int nextId() {
		synchronized (idLock) {
			int c = ++objectId;
			if (c == Integer.MAX_VALUE) {
				objectId = 1;
				c = 1;
			}
			return c;
		}
	}

	/**
	 * ����ڴ�ռ���ʣ����ڵ���
	 * @param msg ��ʾ����Ϣ
	 */
	public static void debugMemory(String msg) {
		DecimalFormat df = new DecimalFormat("###,###");
		System.gc();
		long tmp = Runtime.getRuntime().freeMemory();
		String buf = ParallelMessage.get().getMessage("UnitServer.memory",msg,df.format(tmp / 1024));
		Logger.debug( buf );
	}

	/**
	 * ִ�зֻ�������
	 * @param req �������
	 * @return ��Ӧ���
	 */
	public abstract Response execute(Request req);

	/**
	 * ��ȡ�ֻ�Ψһʵ�����������ļ�˳�����
	 * @return �ֻ�������ʵ��
	 * @throws Exception
	 */
	public static UnitServer getInstance() throws Exception {
		throw new RQException("Method not implemented.");
	};
	
	/**
	 * �������߻�ȡָ����ַ�ķֻ�
	 * @param specifyHost IP��ַ
	 * @param specifyPort �˿ں�
	 * @return �ֻ�ʵ��
	 * @throws Exception
	 */
	public static UnitServer getInstance(String specifyHost, int specifyPort) throws Exception {
		throw new RQException("Method not implemented.");
	}
	
	public static UnitServer getInstance(String host, int port, String cfgPath ) throws Exception {
		throw new RQException("Method not implemented.");
	}

	
	/**
	 * �������Ƿ����ڳ�ʼ������
	 * @return ��ʼ��ʱ����true�����򷵻�false
	 */
	public static boolean isIniting(){
		return isIniting;
	}
	
	/**
	 * ʹ�ò������÷ֻ��ĳ�ʼ���ļ�init.splx
	 * @param i����i���ֻ���i=0��ʾ�ֻ�����ʱ�̵ĵ���
	 * @param N����N���ֻ�
	 * @param j����������
	 * @return
	 */
	public static Response init(final int i, final int N, final String j){
		throw new RQException("Method not implemented.");
	}
	
	/**
	 * ִ�г�ʼ��init.dfx�ű�
	 * @param z �ֻ������
	 * @param N �ܹ��м�̨�ֻ�
	 * @param waitResult �Ƿ�ȴ���ʼ��������������̨���ȣ���ͼ�ο���̨�ȴ�
	 * �������̨����ȴ����ؽ���������ɽ��汻����
	 * @return
	 */
	public static Response init(final int i,final int N, final String j, final boolean waitResult){
		throw new RQException("Method not implemented.");
	}
	
	/**
	 * ���зֻ�����������
	 */
	public abstract void run(); // ��������

	/**
	 * �жϵ�ǰ�������Ƿ�����������
	 */
	public synchronized boolean isRunning() {
		throw new RQException("Method not implemented.");
	}
	
	/**
	 * �˳�������
	 */
	public abstract void quit();
	
	/**
	 * ֹͣ������
	 */
	public abstract void shutDown();

	/**
	 * ��ȡ��������home·��
	 * @return home·��
	 */
	public static String getHome() {
		String home = System.getProperty("start.home");
		if(home==null){
			throw new RuntimeException("start.home is not specified!");
		}
		return home;
	}


	/**
	 * ��ȡ��ǰ��������Host
	 */
	public abstract String getHost();

	/**
	 * ��ȡ�ֻ�IP
	 * @return ip��ַ
	 */
	public abstract String getIP();
	
	/**
	 * ��ǰ�������Ƿ����Զ�������
	 */
	public abstract boolean isAutoStart();

}
