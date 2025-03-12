package com.scudata.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import com.scudata.dm.Env;

/**
 * ��־�����ΪOFF,SEVERE,WARNING,INFO,DEBUG, ���ȼ����ν��ͣ�OFF��ߣ�DEBUG��ͣ�ֻ�и��ڵ�ǰ���õ���־����Ż������
 * Ҫ���õ���־����������: 1Ϊ���ż������������϶�������־�Ŀ��ơ�
 * 						2Ϊ�������ֻ������(��)������������־�Żᱻ��¼�����ڷ����¼��
 * �����������־Ϊ1��2�Ľ�������
 * 	1�����ż������÷���ΪLogger.setLevel(l)��ֻ�и��ڿ��ż������־�Żᴫ�ݸ�2ȥ��¼���൱����ڹ��ˣ����ż�����Բ����ã�
 * 		������ʱȱʡΪ��ͼ���DEBUG��Ҳ��������־����¼���ü���Ķ�Ӧ������Config.xml������ϵͳ�ȶ���Ϊ�������ʱ��ֱ�ӹر���־��
 * 	2������������÷���Ϊhandler.setLevel(l)���÷������ܱ�ֱ�ӵ��ã�ֻ��ͨ��Logger.setPropertyConfig(p)����
 * 		��Ӽ���һ��logger.properties�ļ������á���������Ӧ���������ʽ��
 * 		A������̨�����ʽ��
 * 		B���ļ������ʽ������1���˺����־��
 * 		�Ÿ��������������������Ӧ���ļ����߿���̨�����������Ȼ���Բ����ã�
 * 		������ʱȱʡ����һ����ͼ���DEBUG�Ŀ���̨������ü���Ķ�Ӧ������һ��Ԥ�����properties�ļ��С�
 */
public class ScudataLogger {
//	��־����Ĵ�д��������Ӧ��properties�ļ��У��������泣������д������
	public static String OFF = "OFF";
	public static String SEVERE = "SEVERE";
	public static String WARNING = "WARNING";
	public static String INFO = "INFO";
	public static String DEBUG = "DEBUG";

//	��־�����ڱ����ж�Ӧ�ļ����	
	public static int iDOLOG = -1;
	public static int iOFF = 0;
	public static int iSEVERE = 10;
	public static int iWARNING = 20;
	public static int iINFO = 30;
	public static int iDEBUG = 40;
	public static String lineSeparator = System.getProperty("line.separator", "\n");

//��־�ļ�������������ɲ�ͬ��log�ļ�����ֹ�ļ������Լ������Ӵ�	
	String currentMark;

	private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ArrayList<Handler> handlers = new ArrayList<Handler>();
	private static int gateLevel = iDEBUG;
	private static ScudataLogger logger = new ScudataLogger();
	
	private ScudataLogger() {
		Handler h = new ConsoleHandler();
		addHandler(h);
		try {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					reset();
				}
			});
		} catch (IllegalStateException e) {
		}
	}

	private void reset() {
		for (Handler h : handlers) {
			h.close();
		}
	}

	public void clearHandlers() {
		handlers.clear();
		System.err.println("Clear logger\r\n");
	}

	/**
	 * �г�����֧�ֵ���־������ı���д���������ڽ���༭�ȡ�
	 * @return	����ȫ����־������ַ�������
	 */
	public static String[] listLevelNames() {
		return new String[] { OFF, SEVERE, WARNING, INFO, DEBUG };
	}

	/**
	 * ��ȡ��־�ı���д����Ӧ����־�����
	 * @param level	Ҫ��Ӧ����־����
	 * @return	��Ӧ����־�����
	 */
	public static int getLevel(String level) {
		if (!StringUtils.isValidString(level)) {
			level = INFO;
		}
		level = level.toUpperCase();
		if (level.equals(OFF))
			return iOFF;
		if (level.equals(SEVERE))
			return iSEVERE;
		if (level.equals(WARNING))
			return iWARNING;
		if (level.equals(INFO))
			return iINFO;
		if (level.equals(DEBUG))
			return iDEBUG;
		return iINFO;
	}

	/**
	 * ��ȡ��־����ŵ��ı���д��
	 * @param level Ҫ��Ӧ����־�����
	 * @return	��Ӧ����־����
	 */
	public static String getLevelName(int level) {
		if (level == iDOLOG)
			return "";
		if (level == iOFF)
			return OFF;
		if (level == iSEVERE)
			return SEVERE;
		if (level == iWARNING)
			return WARNING;
		if (level == iINFO)
			return INFO;
		if (level == iDEBUG)
			return DEBUG;
		return DEBUG;

	}

	private String format(int level, Object msg, Throwable t) {
		StringBuffer sb = new StringBuffer();
		Date now = java.util.Calendar.getInstance().getTime();
		sb.append('[').append(fmt.format(now)).append("] ");

		sb.append(lineSeparator);
		String name = getLevelName(level); 
		if(name!=""){
			sb.append(name);
			sb.append(": ");
		}
		String message = (msg == null ? null : msg.toString());
		if(message!=null) {
			sb.append(message);
			if(!message.endsWith(lineSeparator)) {
				sb.append(lineSeparator);
			}
		}
		if (t != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}

	void addHandler(Handler h) {
		handlers.add(h);
		System.err.println("Using logger:"+h+"\r\n");
	}

	/**
	 * ͨ��������õ�properties�ļ��Զ�������Ӧ����־���������ṩ�÷���ʹ�ó���Ա�����ֶ������Ӧ����־��������
	 * �÷������һ��������ļ�����־��������
	 * @param fh	��Ҫ��ӵ��ļ����������
	 */
	public static void addFileHandler(FileHandler fh) {
		logger.handlers.add(fh);
	}

	/**
	 * ȱʡ����£���־��Ĭ�ϲ���һ�����������̨����־��������
	 * �÷������һ������̨��������
	 * @param ch	���������̨����־������
	 */
	public static void addConsoleHandler(ConsoleHandler ch) {
		logger.handlers.add(ch);
	}

	/**
	 * ��synchronized �ĵ��˴�����ǰ��handler�ڲ�����֤ѭ��handlersʱ���̰߳�ȫ
	 * http://111.198.29.167:9000/browse/REPORT-1478
	 * @param level
	 * @param msg
	 * @param t
	 */
	private synchronized void doLog(int level, Object msg, Throwable t) {
		String message = format(level, msg, t);
		for (Handler h : handlers) {
			h.log(level, message);
		}
	}

	public static FileHandler newFileHandler(String path) throws Exception {
		return logger.new FileHandler(path);
	}

	private static Handler getHandler(String name, Properties p)
			throws Exception {
		String tmp = p.getProperty(name);
		if (tmp == null)
			return null;
		Handler h = null;
		if (tmp.equalsIgnoreCase("Console")) {
			h = logger.new ConsoleHandler();
		} else {// �ļ�
			String file = tmp;
			tmp = p.getProperty(name + ".encoding");
			String buf = p.getProperty(name + ".isFixedFileName");
			String maxSize =p.getProperty(name + ".maxSize");
			boolean isFixedFileName = false;
			if (StringUtils.isValidString(buf)) {
				isFixedFileName = Boolean.parseBoolean(buf);
			}
			h = logger.new FileHandler(file, tmp, isFixedFileName,maxSize);
		}
		tmp = p.getProperty(name + ".level");
		if (StringUtils.isValidString(tmp)) {
			int l = getLevel(tmp);
			h.setLevel(l);
		}
		return h;
	}

/**
 * ��־�����������������properties�ļ���ʱ��ʹ�ø÷��������ļ����ݶ���õ���־��������	
 * @param p	����Ϊ���Ը�ʽ����־����������
 * @throws Exception ��ʽ����ʱ�׳��쳣
 */
	public static void setPropertyConfig(Properties p) throws Exception {
//		if(Logger.isUseSLF4J()) {
//			throw new Exception("ScudataLogger is using slf4j frame, properties is not usable.");
//		}
		logger.clearHandlers();

		String key = "Logger";
		String val = p.getProperty(key);
		if (!StringUtils.isValidString(val))
			throw new Exception("Can not find key 'Logger'.");
		StringTokenizer st = new StringTokenizer(val, ",");
		while (st.hasMoreTokens()) {
			String tmp = st.nextToken();
			try {
				Handler h = getHandler(tmp, p);
				if (h != null) {
					logger.addHandler(h);
				}
			} catch (Exception x) {
				// ĳ�ļ�Handler����ʱ������쳣��Ϣ����Ӱ�������һ��
				x.printStackTrace();
			}
		}
	}

	private static Throwable isException(Object msg){
		Throwable t = null;
		if(msg instanceof Throwable){
			t = (Throwable)msg;
		}
		return t;
	}

	/**
	 * ����־�м�¼һ��������Ϣ���÷������ڼ�����ǰ�İ汾����Ҫ���ø÷�����
	 * ��Ӧ����־����ΪSEVERE
	 * @param msg ����¼����־��Ϣ
	 */
	public static void error(Object msg) {
		error(msg, isException(msg));
	}

	/**
	 * ��¼�����Ĵ�����Ϣ�Լ�¼�����ϸ���쳣��Ϣ
	 * @param msg ����¼����־��Ϣ
	 * @param t	��Ӧ����ϸ�쳣
	 */
	public static void error(Object msg, Throwable t) {
		severe(msg, t);
	}

	/**
	 * ǿ�Ƽ�¼һ����Ϣ�Լ��쳣����ʹ�Ѿ�ʹ����OFF������Ȼ�������
	 * @param msg ����¼����־��Ϣ
	 * @param t	��Ӧ����ϸ�쳣
	 */
	public static void doLog(Object msg, Throwable t) {
		logger.doLog(iDOLOG, msg, t);
	}

	/**
	 * ǿ�Ƽ�¼һ����Ϣ����ʹ�Ѿ�ʹ����OFF������Ȼ�������
	 * @param msg ����¼����־��Ϣ
	 */
	public static void doLog(Object msg) {
		ScudataLogger.doLog(msg, null);
	}

	/**
	 * ���ڼ�¼�����е����س����ü������Ϣʱ�û�����鿴��־��ȷ������ԭ��
	 * @param msg ����¼����־��Ϣ��ͨ�����ڼ�����߲����쳣����ϸ����
	 * @param t	��Ӧ����ϸ�쳣
	 */
	public static void severe(Object msg, Throwable t) {
		logger.doLog(iSEVERE, msg, t);
	}

	/**
	 * �򵥼�¼�����е�������Ϣ
	 * @param msg	����¼����Ϣ������Ϣ������ı��������¼�ı�����������쳣�࣬��������Ӧ��severe(msg,t)����
	 */
	public static void severe(Object msg) {
		severe(msg, isException(msg));
	}

	/**
	 * ͬwarning������������ǰ�汾�ļ��ݣ���Ҫ���ø÷�����
	 * @param msg
	 */
	public static void warn(Object msg) {
		warn(msg, isException(msg));
	}

	public static void warn(Object msg, Throwable t) {
		warning(msg, t);
	}

	/**
	 * ��ϸ��¼һ��������Ϣ�Լ���Ӧ�쳣
	 * @param msg	����¼����Ϣ
	 * @param t	��Ӧ����ϸ�쳣
	 */
	public static void warning(Object msg, Throwable t) {
		logger.doLog(iWARNING, msg, t);
	}

	/**
	 * ��¼һ��������Ϣ
	 * @param msg	����¼����Ϣ
	 */
	public static void warning(Object msg) {
		warning(msg, isException(msg));
	}

	/**
	 * ��ϸ��¼һ����ͨ��Ϣ�Լ���Ӧ�쳣
	 * @param msg	����¼����Ϣ
	 * @param t	��Ӧ����ϸ�쳣
	 */
	public static void info(Object msg, Throwable t) {
		logger.doLog(iINFO, msg, t);
	}

	/**
	 * �򵥼�¼һ����ͨ��Ϣ
	 * @param msg	����¼����Ϣ
	 */
	public static void info(Object msg) {
		info(msg, isException(msg));
	}

	/**
	 * ��ϸ��¼һ��������Ϣ�Լ���Ӧ�쳣
	 * @param msg	����¼����Ϣ
	 * @param t	��Ӧ����ϸ�쳣
	 */
	public static void debug(Object msg, Throwable t) {
		logger.doLog(iDEBUG, msg, t);
	}

	/**
	 * �򵥼�¼һ��������Ϣ
	 * @param msg	����¼����Ϣ
	 */
	public static void debug(Object msg) {
		debug(msg, isException(msg));
	}

	/**
	 * �жϵ�ǰ��־�Ŀ��ż����Ƿ�Ϊ����ģʽ
	 * @return �Ƿ����ģʽ
	 */
	public static boolean isDebugLevel() {
		int level = gateLevel;
		return level == iDEBUG;
	}

	/**
	 * ͨ�������£�����ģʽ��������м������־��������ǰ�汾���Ƿ���Լ���д������Ҫ���ø÷�����
	 * @return �Ƿ����ģʽ
	 */
	public static boolean isAllLevel() {
		return isDebugLevel();
	}

	/**
	 * ���õ�ǰ��־�ļ�¼����
	 * @param level	���ļ��������־��������
	 */
	public static void setLevel(String level) {
		int l = getLevel(level);
		gateLevel = l;
//		System.err.println("Log level:" + getLevelName(gateLevel));
	}

	/**
	 * ��ȡ��ǰ��־��¼����
	 * @return	���ؼ�¼�����
	 */
	public static int getLevel() {
		return gateLevel;
	}

	private synchronized String getDateMark() {
		return formatter.format(new Date());
	}


	abstract class Handler {
		int logLevel = iDEBUG;// iINFO;

		void setLevel(int level) {
			this.logLevel = level;
		}

		int getLevel() {
			return logLevel;
		}

		void log(int level, String msg) {
			if (level > gateLevel) {
				return;
			}
			doLog(level, msg);
		}

		abstract void doLog(int level, String msg);

		abstract void close();
	}

	public class ConsoleHandler extends Handler {
		void doLog(int level, String msg) {
			if (level > logLevel)
				return;
			System.err.println(msg);// out����������ݣ�����dos����̨�ռ�
		}

		void close() {
		}
		
		public String toString() {
			return "Console,"+getLevelName(logLevel);
		}
	}

	public class FileHandler extends Handler {
		String fileName, encoding = "UTF-8";
		boolean isFixedFileName = false;
		int maxFileSize = 10*1024*1024;
		String absolutePath = null;//���ڹ������·�����ļ�ʱ�����������Ĳ�ͬ������ɾ���·����һ�£����Ͼ���·��������һ���ļ����ɾ���·�����Ժ�����·�����øþ���·��
		File currentFile = null;
		BufferedWriter br = null;
		FileOutputStream fos = null;

		public FileHandler(String file) throws Exception {
			this(file, null,false,null);
		}
//		maxSize:  ��λΪM�� ����д10�� ����10M
		public FileHandler(String file, String encode,boolean isFixedFileName,String maxSize) throws Exception {
			this.fileName = file;
			this.isFixedFileName = isFixedFileName;
			setMaxFileSize(maxSize);
			if (encode != null && !encode.isEmpty()) {
				this.encoding = encode;
			}
			Object[] files = getLogFile( getBaseFile(), isFixedFileName);
			currentFile = (File)files[0];
			absolutePath = (String)files[1];
			fos = new FileOutputStream(currentFile, true);
			br = new BufferedWriter(new OutputStreamWriter(fos, encoding));
		}

		private String getBaseFile() {
			String baseFile = fileName;
			if(absolutePath!=null) {
				baseFile = absolutePath;
			}
			return baseFile;
		}
		
		public void setFixedFileName(boolean fix) {
			isFixedFileName = fix;
		}

		/**
		 * ��������ļ��ߴ磬��λM
		 * @param maxSize
		 */
		public void setMaxFileSize(String maxSize) {
			if(StringUtils.isValidString(maxSize)) {
				try {
					if(maxSize.toLowerCase().endsWith("m")) {
						int len = maxSize.length();
						maxSize = maxSize.substring(0,len-1);
					}
					maxFileSize = Integer.parseInt(maxSize)*1024*1024;
				}catch(Exception x) {
					
				}
			}
		}

		void doLog(int level, String msg) {
			if (level > logLevel)
				return;
			if (!isFixedFileName) {
				String mark = getDateMark();
				
				if (!currentMark.equals(mark) ||
						(currentFile!=null && currentFile.length()>maxFileSize)) {
					try {
						br.close();
						currentFile = (File)getLogFile(getBaseFile(),isFixedFileName)[0];
						fos = new FileOutputStream(currentFile, true);
						br = new BufferedWriter(new OutputStreamWriter(fos,
								encoding));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}

			try {
				br.newLine();
				br.write(msg);
				br.flush();
			} catch (Exception e) {
			}

		}

		void close() {
			try {
				br.close();
			} catch (Exception e) {
			}
		}
		
		private ArrayList<String> bufFiles = new ArrayList<String>();
		private Object[] getLogFile(String fileName, boolean isFixedFileName) {
			Object[] files = new Object[2];//��0���ع���File����1���ؾ���·��
			File f = new File(fileName);
			if (!f.isAbsolute()) {
				String home = System.getProperty("start.home");
				if (home != null) {
					f = new File(home, fileName);
				} else {
					ServletContext sc = Env.getApplication();
					if (sc != null) {
						home = sc.getRealPath("/");
					}
					if (home != null) {
						f = new File(home, fileName);// �����webӦ�õĸ�Ŀ¼
					} else {
						// �������Ϊwar������ʱ��home��Ȼ��Ϊnull����ʱ����ڵ�ǰ�Ĺ���·����Ҳ��web
						// server������exe��·����
						f = new File(f.getAbsolutePath());// �ڵ�ǰ����·�����ļ�
					}
				}
				files[1] = f.getAbsolutePath();
			}
			String filePath;
			if (isFixedFileName) {
				filePath = f.getAbsolutePath();
			} else {
				String parentPath = f.getParent();
				String file = f.getName();
				if (!parentPath.endsWith(File.separator)) {
					parentPath += File.separator;
				}
				String pattern = parentPath;// + File.separator;
				if (file.endsWith(".log")) {
					pattern += file.substring(0, file.length() - 4);
				} else {
					pattern += file;
				}
				currentMark = getDateMark();
				int count=0;
				filePath = pattern + "_" + currentMark+ count + ".log";
				File tmp = new File(filePath);
				while(tmp.length()>maxFileSize) {
					count = count + 1;
					filePath = pattern + "_" + currentMark+ count + ".log";
					tmp = new File(filePath);
				}
			}
			if(!bufFiles.contains(filePath)) {
				if(!Logger.isUseSLF4J()) {//ʹ�ÿ��ʱ�����õ���־���ñ��ò�����
					System.err.println("Raqsoft is using log file:\r\n" + filePath + "\r\n");
				}
				
				bufFiles.add(filePath);
				if(bufFiles.size()>1024) {
					bufFiles.clear();
				}
			}

			f = new File(filePath);
			
			File p = f.getParentFile();
			if (!p.exists()) {
				p.mkdirs();
			}
			files[0] = f;
			return files;
		}

		public String toString() {
			return fileName+","+getLevelName(logLevel)+","+maxFileSize/(1024*1024)+"M";
		}
		
	}

	public static void main(String[] args) throws Exception {
		int c = 0;
		ScudataLogger.setLevel(OFF);
		for(int i=0;i<10;i++){
			ScudataLogger.doLog(i);
		}
		
		System.exit(0);
		
		File file = new File("D:/logger.properties");
		FileInputStream is = new FileInputStream(file);
		Properties p = new Properties();
		p.load(is);
		ScudataLogger.setPropertyConfig(p);

		// Logger.setLevel("WARNING");
		Logger.setLevel("severe");

		Thread t1 = new Thread() {
			public void run() {
				String name = "t1:";
				Logger.severe(name + "severe");
				Logger.warning(name + "warning");
				Logger.info(name + "info");
				Logger.debug(name + "debug");
			}
		};
		Thread t2 = new Thread() {
			public void run() {
				String name = "t2:";
				Logger.severe(name + "severe");
				Logger.warning(name + "warning");
				Logger.info(name + "info");
				Logger.debug(name + "debug");
			}
		};
		Thread t3 = new Thread() {
			public void run() {
				String name = "t3:";
				Logger.severe(name + "����");
				Logger.warning(name + "����");
				Logger.info(name + "��Ϣ");
				Logger.debug(name + "����");
			}
		};
		t1.start();
		t2.start();
		t3.start();

		t1.join();
		t2.join();
		t3.join();
		Logger.info("info test");
		System.exit(0);
	}

}
