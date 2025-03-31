package com.scudata.parallel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.scudata.app.common.AppUtil;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.Logger;
import com.scudata.common.ScudataLogger;
import com.scudata.common.StringUtils;
import com.scudata.common.ScudataLogger.FileHandler;
import com.scudata.common.SplServerConfig;
import com.scudata.dm.Env;
import com.scudata.common.GMBase;
import com.scudata.resources.ParallelMessage;

/**
 * �ڵ��������������
 * @author Joancy
 *
 */
public class UnitContext {
	public static String UNIT_XML = "unitServer.xml";

	HostManager hostManager = HostManager.instance();

	int tempTimeOut = 0; // ��ʱ�ļ����ʱ�䣬СʱΪ��λ��0Ϊ����鳬ʱ
	private int interval = 5, proxyTimeOut = 0; // �����������ʱ�ļ����ڵ�ʱ������0Ϊ�������ڡ��ļ��Լ��α����Ĺ���ʱ��
	private int backlog = 10; // ��������󲢷����ӣ�����ϵͳȱʡ���Ϊ50���޶���Χ1��50

	private RaqsoftConfig raqsoftConfig = null;
	private boolean checkClient = false,autoStart=false;
	private List<String> enabledClientsStart = null;
	private List<String> enabledClientsEnd = null;

	private String logFile;

	/**
	 * ȡӦ��������Ϣ
	 * @return ������Ϣ
	 */
	public RaqsoftConfig getRaqsoftConfig() {
		return raqsoftConfig;
	}

	/**
	 * ��������������Ϣ
	 * @param rc Ӧ������
	 * @param needCheckIP
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc) {
		raqsoftConfig = rc;
	}

	/**
	 * ȡ��־�ļ�
	 * @return ��־�ļ���
	 */
	public String getLogFile(){
		return logFile;
	}
	
	public boolean isCheckClients() {
		return checkClient;
	}

	/**
	 * �Ƿ��Զ�����
	 * @return �Զ���������true�����򷵻�false
	 */
	public boolean isAutoStart() {
		return autoStart;
	}
	private static boolean between(String ip, String start, String end) {
		if (!StringUtils.isValidString(end)) {
			return ip.equals(start);
		}
		return (ip.compareTo(start) >= 0 && ip.compareTo(end) <= 0);
	}

	public boolean checkClientIP(String client) {
		return checkClientIP(client, enabledClientsStart, enabledClientsEnd);
	}

	public static boolean checkClientIP(String client,
			List<String> enabledClientsStart, List<String> enabledClientsEnd) {
		if (enabledClientsStart == null || enabledClientsStart.isEmpty()) {
			return false;
		}
		for (int i = 0; i < enabledClientsStart.size(); i++) {
			String start = enabledClientsStart.get(i);
			String end = enabledClientsEnd.get(i);
			if (between(client, start, end)) {
				return true;
			}
		}
		return false;
	}

	private boolean isNodeAvailable(String host, int port) {
		try {
			InetAddress add = InetAddress.getByName(host);
			ServerSocket ss = new ServerSocket(port, 10, add);
			ss.close();
			return true;
		} catch (Exception x) {
		}
		return false;
	}

	/**
	 * �ڵ���ļ�Ĭ��ΪconfigĿ¼�£�������·����Ȼ����start.home�µľ���·��
	 * 
	 * @param relativePath
	 *            String ����ļ���
	 * @throws Exception ����ʱ�׳��쳣
	 * @return InputStream ������
	 */
	public static InputStream getUnitInputStream(String relativePath)
			throws Exception {
		relativePath = "config/" + relativePath;// �����ļ�������configĿ¼��
		InputStream inputStream = null;
		// ֻ���þ���·���µģ��ط����ˣ����㲻�嵽���õ��ĵ�
		if (inputStream == null) {
			String serverPath = GMBase.getAbsolutePath(relativePath);

			File serverFile = new File(serverPath);
			if (!serverFile.exists()) {
				throw new Exception(ParallelMessage.get().getMessage("UnitContext.noconfig",
						serverPath));
			}
			inputStream = new FileInputStream(serverPath);
		}
		return inputStream;

	}

	private static UnitConfig getUnitConfig() throws Exception {
		InputStream inputStream = getUnitInputStream(UNIT_XML);
		UnitConfig uc = new UnitConfig();
		uc.load(inputStream);
		inputStream.close();
		return uc;
	}

	/**
	 * �г����зֻ�����
	 * @return �ֻ���Ϣ�б�
	 * @throws Exception ����ʱ�׳��쳣
	 */
	public static ArrayList<UnitContext.UnitInfo> listNodes() throws Exception {
		ArrayList<UnitContext.UnitInfo> uis = new ArrayList<UnitContext.UnitInfo>();
		UnitConfig uc = getUnitConfig();
		List<UnitConfig.Host> hosts = uc.getHosts();
		for (int i = 0; i < hosts.size(); i++) {
			UnitConfig.Host uchost = hosts.get(i);
			UnitInfo ui = new UnitInfo();
			ui.host = uchost.getIp();
			ui.port = uchost.getPort();
			uis.add(ui);
		}
		return uis;
	}

	/**
	 * ����ָ���ĵ�ַ����ڵ��������
	 * @param specifyHost ָ��IP
	 * @param specifyPort ָ���˿�
	 * @throws Exception �������ʱ�׳��쳣
	 */
	public UnitContext(String specifyHost, int specifyPort) throws Exception {
		UnitConfig uc = getUnitConfig();
		String host = null;
		int port = 8281;

		UnitConfig.Host ucHost = null;
		List<UnitConfig.Host> hosts = uc.getHosts();
		if (hosts.isEmpty()) {
			throw new Exception(ParallelMessage.get().getMessage("UnitContext.emptyunit"));
		}

		for (int i = 0; i < hosts.size(); i++) {
			ucHost = hosts.get(i);
			String tmpHost = ucHost.getIp();
			if (tmpHost.equalsIgnoreCase("localhost")) {
				// ֧��localhostд��ʱ��Ҫ����ת��Ϊȱʡ�ı���ip4��������127.0.0.1������û��װ����ʱ��ʹ��127.0.0.1
				tmpHost = UnitContext.getDefaultHost();
			}

			int p;
			if (specifyHost != null) {
				if (specifyHost.equalsIgnoreCase("localhost")) {// ͬ��
					specifyHost = UnitContext.getDefaultHost();
				}

				if (!tmpHost.equals(specifyHost)) {
					continue;
				} else {
					if (specifyPort == 0) {// ָ���˿�Ϊ0ʱ������docker�����ֻ���docker�Ķ˿�Ϊ�����Զ��˿ںţ�û������ָ��
						p = ucHost.getPort();
					} else {
						if (specifyPort == ucHost.getPort()) {
							p = specifyPort;
						} else {
							continue;
						}
					}
				}
			} else {
				p = ucHost.getPort();
			}

			if (tmpHost.equalsIgnoreCase("localhost")) {// ֧��localhostд��ʱ��Ҫ����ת��Ϊȱʡhost�������ֽ��̱Ƚ�
				String defIP = UnitContext.getDefaultHost();
				Logger.info("Using IP:" + defIP + " instead of:" + tmpHost
						+ ".");
				tmpHost = defIP;
			}
			if (isNodeAvailable(tmpHost, p)) {
				host = tmpHost;
				port = p;
				break;
			} else {
			}
		}

		if (host == null) {
			if (specifyHost != null) {
				throw new Exception(ParallelMessage.get().getMessage("UnitContext.failhost",
						specifyHost + ":" + specifyPort));
			} else {
				throw new Exception(ParallelMessage.get().getMessage("UnitContext.nohost"));
			}
		}

		String home = System.getProperty("start.home");
		String file = "nodes/" + UnitClient.getHostPath(host) + "_" + port + "/log/log.txt";
		File f = new File(home, file);
		File fp = f.getParentFile();
		if (!fp.exists()) {
			fp.mkdirs();
		}
		logFile = f.getAbsolutePath();
		FileHandler lfh = ScudataLogger.newFileHandler(logFile);
		ScudataLogger.addFileHandler(lfh);

		// �̶��ڵ������ʱĿ¼���� start.home/nodes/[ip_port]/tempĿ¼��
		String path = "nodes/" + UnitClient.getHostPath(host) + "_" + port + "/temp";
		f = new File(home, path);
		if (!f.exists()) {
			f.mkdirs();
		}
		path = f.getAbsolutePath();
		Env.setTempPath(path);

		checkClient = uc.isCheckClients();
		autoStart = uc.isAutoStart();
		enabledClientsStart = uc.getEnabledClientsStart();
		enabledClientsEnd = uc.getEnabledClientsEnd();

		// �����ڵ�����ýڵ����IP�滻����config.xml���ص�localHost
		hostManager.setHost(host);
		hostManager.setPort(port);

		hostManager.setMaxTaskNum(ucHost.getMaxTaskNum());
		hostManager.setPreferredTaskNum(ucHost.getPreferredTaskNum());

		// Server ����
		tempTimeOut = uc.getTempTimeOut();
		if (tempTimeOut > 0) {
			Logger.debug(ParallelMessage.get().getMessage("UnitContext.temptimeout", tempTimeOut));
		}

		int t = uc.getInterval();
		if (t > 0)
			interval = t;// ���ò���ȷʱ��ʹ��ȱʡ�����

		t = uc.getBacklog();
		if (t > 0)
			backlog = t;

		proxyTimeOut = uc.getProxyTimeOut();
	}


	
	public UnitContext( SplServerConfig ssc ) throws Exception {
		if(StringUtils.isValidString(ssc.logPath)) {
			logFile = ssc.logPath;
			File f = new File(logFile);
			File fp = f.getParentFile();
			if (!fp.exists()) {
				fp.mkdirs();
			}
			logFile = f.getAbsolutePath();
			FileHandler lfh = ScudataLogger.newFileHandler(logFile);
			ScudataLogger.addFileHandler(lfh);
		}

		// Server ����
		if(StringUtils.isValidString(ssc.tempTimeOut)) {
			tempTimeOut = Integer.parseInt(ssc.tempTimeOut);
			if (tempTimeOut > 0) {
				Logger.debug(ParallelMessage.get().getMessage("UnitContext.temptimeout", tempTimeOut));
			}
		}
		if(StringUtils.isValidString(ssc.proxyTimeOut)) {
			proxyTimeOut = Integer.parseInt(ssc.proxyTimeOut);
		}

		if(StringUtils.isValidString(ssc.interval)) {
			interval = Integer.parseInt(ssc.interval);
		}
		
		if(StringUtils.isValidString(ssc.backlog)) {
			backlog = Integer.parseInt(ssc.backlog);
		}

		if(StringUtils.isValidString(ssc.splConfig)) {
			InputStream is = new FileInputStream( ssc.splConfig );
			raqsoftConfig = ConfigUtil.load(is,true);
			is.close();
		}else {
//			raqsoftConfig = ServerConsole.loadRaqsoftConfig();
		}
	}

	/**
	 * ��ȡȱʡ����������
	 * @return ��������
	 */
	public static String getDefaultHost() {
		String[] allHosts = AppUtil.getLocalIps();
		String tmpHost = "127.0.0.1";
		if (allHosts.length > 0) {
			for(int i=0;i<allHosts.length;i++){
				tmpHost = allHosts[i];
				if(tmpHost.indexOf(':')>0)continue;//ȱʡ��ѡIP6
				if(!tmpHost.equals("127.0.0.1")) break;
			}
		}
		return tmpHost;
	}

	/**
	 * ȡ��ʱ�ļ��ĳ�ʱ
	 * @return ʱ��
	 */
	public int getTimeOut() {
		return tempTimeOut;
	}

	/**
	 * ͬgetTimeOut
	 * @return
	 */
	public int getTimeOutHour() {
		return tempTimeOut;
	}

	/**
	 * ȡ��鳬ʱ��ʱ����
	 * @return ʱ����
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * ��ȡ���Ӳ�����
	 * @return ����������
	 */
	public int getBacklog() {
		return backlog;
	}
	/**
	 * ȡ����ʱ
	 * @return ����ʱʱ��
	 */
	public int getProxyTimeOut() {
		return proxyTimeOut;
	}

	/**
	 * ͬgetProxyTimeOut
	 * @return
	 */
	public int getProxyTimeOutHour() {
		return proxyTimeOut;
	}

	/**
	 * ȡ������������
	 * @return ��������
	 */
	public String getLocalHost() {
		return hostManager.getHost();
	}

	/**
	 * ȡ���ض˿ں�
	 * @return �˿ں�
	 */
	public int getLocalPort() {
		return hostManager.getPort();
	} // �ڵ���˿�

	/**
	 * ��������Ϣʵ��toString
	 */
	public String toString() {
		return hostManager.toString();
	}

	/**
	 * �ֻ���Ϣ
	 * @author Joancy
	 *
	 */
	public static class UnitInfo {
		private String host = null;
		private int port = 8281;

		public UnitInfo() {
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

	}
}