package com.scudata.server.http;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.Logger;
import com.scudata.parallel.UnitContext;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;
import com.sun.net.httpserver.HttpServer;

/**
 * dfx�����Http������ʵ��
 * 
 * @author Joancy
 *
 */
public class SplxServerInIDE implements IServer {
	public static final String HTTP_CONFIG_FILE = "HttpServer.xml";
	public static SplxServerInIDE instance=null;
	
	private HttpServer httpServer;
	private HttpContext ctx=null;
	private RaqsoftConfig rc = null;
	StartUnitListener listener = null;

	/**
	 * ����������Ϣ
	 * @param rc ����
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc){
		this.rc = rc;
	}
	/**
	 * ��ȡ������Ϣ
	 * @return ����
	 */
	public RaqsoftConfig getRaqsoftConfig(){
		return rc;
	}
	
	/**
	 * ��ȡ���������Ķ���
	 * @return �����Ķ���
	 */
	public HttpContext getContext(){
		return ctx;
	}
	
	/**
	 * ��ȡ������Ψһʵ��
	 * @return ������ʵ��
	 * @throws Exception ����ʵ������ʱ�׳��쳣
	 */
	public static SplxServerInIDE getInstance() throws Exception {
		if (instance == null) {
			instance = new SplxServerInIDE();
		}
		return instance;
	}
	
	/**
	 * ����������
	 * @return �����ɹ�����true��ʧ�ܷ���false
	 * @throws Throwable ���������г����׳��쳣
	 */
	public boolean start() throws Throwable {
		if (httpServer != null)
			return false;
//			�ȼ���������ļ��Ƿ����
		InputStream is = UnitContext.getUnitInputStream(HttpContext.HTTP_CONFIG_FILE);
		is.close();
		ctx = new HttpContext(true);
		String host = ctx.getHost();
		int port = ctx.getPort();
		Logger.info(ParallelMessage.get().getMessage("SplxServerInIDE.starting"));
//		Logger.info("Release date:"+AppFrame.RELEASE_DATE);
		
		InetAddress ia = InetAddress.getByName(host);
		try{
			InetSocketAddress inetSock = new InetSocketAddress(ia,port);
			httpServer = HttpServer.create(inetSock, ctx.getMaxLinks());
			LinksPool.setMaxLinks( ctx.getMaxLinks() );
		}catch(java.net.BindException ex){
			ex.printStackTrace();
			throw new Exception(ParallelMessage.get().getMessage("SplxServerInIDE.portbind",port));
		}
		SplxHttpHandler dhh = new SplxHttpHandler();
		dhh.setIServer(this);
		httpServer.createContext("/", dhh);
		httpServer.setExecutor(null);
		httpServer.start();
		if (listener != null) {
			listener.serverStarted(port);
		}

		Logger.info(ParallelMessage.get().getMessage("SplxServerInIDE.started", ctx.getDefaultUrl()));
		return true;
	}

	/**
	 * �رշ�����
	 */
	public void shutDown() {
		stop();
	}

	/**
	 * ִ��ֹͣ������
	 * @return �ɹ�ͣ�����񷵻�true�����򷵻�false
	 */
	public boolean stop() {
		if (httpServer == null)
			return false;
		httpServer.stop(2); // ���ȴ�2��
		httpServer = null;
		Logger.info(ParallelMessage.get().getMessage("SplxServerInIDE.stop"));
		return true;
	}

	/**
	 * ��ʼ���з���
	 */
	public void run() {
		try {
			start();
		} catch (Throwable e) {
			if (listener != null) {
				listener.serverStartFail();
			}
			e.printStackTrace();
		}
	}

	/**
	 * ��ȡ����״̬
	 * @return �������з���true�����򷵻�false
	 */
	public boolean isRunning() {
		return httpServer != null;
	}
	
	/**
	 * ���÷�������������
	 */
	public void setStartUnitListener(StartUnitListener listen) {
		listener = listen;
	}

	/**
	 * ��ȡ��������ַ
	 * @return ��������ַ
	 */
	public String getHost() {
		return ctx.toString();
	}

	/**
	 * �Ƿ��Զ���������
	 * @return �Զ���������true�����򷵻�false
	 */
	public boolean isAutoStart() {
		if(ctx==null){
			ctx = new HttpContext(true);
		}
		return ctx.isAutoStart();
	}

}
