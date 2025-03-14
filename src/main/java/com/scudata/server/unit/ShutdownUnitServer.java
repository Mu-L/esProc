package com.scudata.server.unit;

import java.util.List;

import com.scudata.app.common.AppUtil;
import com.scudata.app.common.Section;
import com.scudata.common.StringUtils;
import com.scudata.parallel.UnitClient;
import com.scudata.parallel.UnitContext;

/**
 * ͨ������Զ�̹رշ�����
 * 
 * @author Joancy
 *
 */
public class ShutdownUnitServer {
	static String[] allHosts;
	static{
		allHosts = AppUtil.getLocalIps();
	}

	/**
	 * �������ʶ�ȡ���зֻ���Ϣ��Ȼ��ر����зֻ�
	 * @return ��ȷ�ػ�����true�����򷵻�false
	 * @throws Exception
	 */
	public static boolean autoClose() throws Exception{
		String home = System.getProperty("start.home");

		if (!StringUtils.isValidString( home )) {
			throw new Exception("start.home is not specified!");
		}
		List<UnitContext.UnitInfo> hosts = UnitContext.listNodes();
		if(hosts==null || hosts.isEmpty()){
			System.out.println("No node server found under: "+home);
			return false;
		}
		for (int i = 0; i < hosts.size(); i++) {
			UnitContext.UnitInfo ui = hosts.get(i);
			String host = ui.getHost();
			if(!(AppUtil.isLocalIP(host))) continue;
			int port = ui.getPort();
			if (close(host, port)) {
			}
		}

		return true;
	}

	/**
	 * �ر�ָ���ķ�����
	 * @param host ������IP
	 * @param port �˿ں�
	 * @return �ɹ��ػ�����true�����򷵻�false
	 */
	public static boolean close(String host, int port){
		if(!StringUtils.isValidString(host)){
			host = UnitContext.getDefaultHost();
		}
		UnitClient uc = new UnitClient(host,port);
		if(uc.isAlive()){
			uc.shutDown();
			System.out.println(uc+" is shut downed.");
			return true;
		}else{
			System.out.println(uc+" is not alive.");
			return false;
		}
	}
	
	public static void main(String[] args) {
		String host = null;
		int port = 0;
		String arg;
		if (args.length == 1) {
			arg = args[0].trim();
			if (arg.trim().indexOf(" ") > 0) {
				Section st = new Section(arg, ' ');
				args = st.toStringArray();
			}
		}

		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				arg = args[i].toLowerCase();
				// System.err.println("arg "+i+"="+arg);
				if (arg.equals("com.scudata.server.unit.shutdownunitserver")) { // ��bat�򿪵��ļ�������������ǲ���
					continue;
				}

				if (host == null) {
					host = arg;
				} else if (port == 0) {
					port = Integer.parseInt(arg);
				} else {
					break;
				}
			}
		}
		
		try {
			if (host == null) {
				autoClose();
				Thread.sleep(3000);
			}else{
				if(!close(host,port)){
					System.out.println(host+":"+port+" does not on running.");
				}
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		System.exit(0);
	}
	

}