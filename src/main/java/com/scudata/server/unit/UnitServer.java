package com.scudata.server.unit;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.UUID;

import com.scudata.app.common.Section;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.parallel.*;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;

import java.text.*;

/**
 * ��ͼ�η�����
 * ���ܵ���GM��ide���࣬ �ֻ�
 * 
 * @author Joancy
 *
 */
public class UnitServer implements IServer {
	public static UnitServer instance;
//	public static String version = AppFrame.RELEASE_DATE;
	
	ServerSocket serverSocket = null;
	UnitContext unitContext=null;
 
	TempFileMonitor tempFileMonitor = null;
	ProxyMonitor proxyMonitor;
	
	private volatile boolean stop = true;

//	exe����ʱָ������
	private String specifyHost=null;
	private int specifyPort = 0;
		
	StartUnitListener listener = null;
	HostManager hostManager = HostManager.instance();
	private RaqsoftConfig rc = null;

	private static Object initLock = new Object();
	private static boolean isIniting = false;
	private static Response initResult = new Response();
	
	/**
	 * ���û�������
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc){
		this.rc = rc;
	}
	/**
	 * ��ȡ�����������ö���
	 */
	public RaqsoftConfig getRaqsoftConfig(){
		return rc;
	}
	
	/**
	 * ���÷�����״̬������
	 */
	public void setStartUnitListener(StartUnitListener listen){
		listener = listen;
	}
	
	/**
	 * ��ȡ���㻷��������
	 * @return
	 */
	public UnitContext getUnitContext() {
		return unitContext;
	}

	static int objectId = 0;
	static Object idLock = new Object();

	private UnitServer(){
	}
	
	private UnitServer(String host, int port) throws Exception{
		this.specifyHost = host;
		this.specifyPort = port;
	}
	
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

	private String getArgDesc(List argValues) {
		if (argValues == null || argValues.size() == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < argValues.size(); i++) {
			if (i > 0) {
				sb.append(";");
			}
			sb.append(argValues.get(i));
		}
		return sb.toString();
	}

	/**
	 * ִ�зֻ�������
	 * @param req �������
	 * @return ��Ӧ���
	 */
	public Response execute(Request req) {
		Response res = new Response();
		switch (req.getAction()) {
		case Request.SERVER_LISTTASK:
			List list = TaskManager.getTaskList();
			Table table = new Table(new String[] {"Port", "TaskId", "SPLXName",
					"ArgDesc", "BeginTime", "FinishTime" });
			for (int i = 0; i < list.size(); i++) {
				Task t = (Task) list.get(i);
				if (t.getFinishTime() > 0 || t.isProcessCaller())  {
					continue;
				}
				table.newLast(new Object[] { hostManager.getPort(), new Integer(t.getTaskID()),
						t.getDfxName(), getArgDesc(t.getArgList()),
						new Long(t.getCallTime()), new Long(t.getFinishTime()) });
			}
			res.setResult(table);
			break;
		case Request.SERVER_LISTPARAM:
			Table tableParam = new Table(new String[] {"Port", "SpaceName", "ParamName","ParamValue"});
			HashMap<String, Param[]> hm = JobSpaceManager.listSpaceParams();
			Iterator<String> it = hm.keySet().iterator();
			while (it.hasNext()) {
				String id = it.next();
				Param[] params = hm.get(id);
				if (params != null)
					for (int i = 0; i < params.length; i++) {
						Param p = params[i];
						tableParam.newLast(new Object[] { hostManager.getPort(), id,
								p.getName(),p.getValue() });
					}
			}
			ParamList gList = Env.getParamList();
			for(int i=0;i<gList.count();i++){
				Param p = gList.get(i);
				tableParam.newLast(new Object[] { hostManager.getPort(), "Global",
						p.getName(),p.getValue() });
			}
			
			res.setResult(tableParam);
			break;
		case Request.SERVER_GETUNITS_MAXNUM:
			int maxNum = hostManager.getMaxTaskNum();
			res.setResult( maxNum );
			break;
		case Request.SERVER_SHUTDOWN:
			if(listener != null){
				listener.doStop();
			}else{
				shutDown();
			}
			break;
		case Request.SERVER_GETTASKNUMS:
			int[] result = new int[2];
			result[0]=hostManager.getPreferredTaskNum();
			result[1]=hostManager.getCurrentTasks();
			res.setResult( result );
			break;
		case Request.SERVER_GETAREANO:
			String J = (String)req.getAttr(Request.GETAREANO_TaskName);
			res.setResult( Env.getAreaNo(J) );
			break;
		case Request.SERVER_GETCONCURRENTCOUNT:
			res.setResult(new Integer(PerfMonitor.getConcurrentTasks()));
			break;
		case Request.SERVER_CLOSESPACE: {
			String spaceId = (String) req.getAttr(Request.CLOSESPACE_SpaceId);
			JobSpaceManager.closeSpace(spaceId);
			break;
		}
		case Request.SERVER_GETTABLEMEMBERS:
			try {
				String spaceId = (String) req
						.getAttr(Request.FETCHCLUSTERTABLE_SpaceId);
				String tableName = (String) req
						.getAttr(Request.FETCHCLUSTERTABLE_TableName);
				Sequence tableObj = UnitClient.getMemoryTable(spaceId, tableName, unitContext.toString());
				res.setResult(new Integer(tableObj.length()));
			} catch (Exception x) {
				res.setException(x);
			}
			break;
		}
		return res;
	}

	/**
	 * ��ȡ�ֻ�Ψһʵ�����������ļ�˳�����
	 * @return �ֻ�������ʵ��
	 * @throws Exception
	 */
	public static UnitServer getInstance() throws Exception {
		return getInstance(null,0);
	}
	
	/**
	 * �������߻�ȡָ����ַ�ķֻ�
	 * @param specifyHost IP��ַ
	 * @param specifyPort �˿ں�
	 * @return �ֻ�ʵ��
	 * @throws Exception
	 */
	public static UnitServer getInstance(String specifyHost, int specifyPort) throws Exception {
		if (!StringUtils.isValidString(getHome())) {
			throw new Exception(ParallelMessage.get().getMessage("UnitServer.nohome"));
		}
		if (instance == null) {
			instance = new UnitServer(specifyHost,specifyPort);
		}
		
		return instance;
	}
	
	public static UnitServer getInstance(String host, int port, String cfgPath ) throws Exception {
		if (instance == null) {
			instance = new UnitServer( host, port);
		}
		InputStream is = new FileInputStream( cfgPath );
		SplServerConfig ssc = SplServerConfig.getCfg(is);
		is.close();
		instance.unitContext = new UnitContext(ssc);
		instance.rc = instance.unitContext.getRaqsoftConfig();
		instance.hostManager.setHost(host);
		instance.hostManager.setPort(port);
		return instance;
	}
	
	
	/**
	 * �������Ƿ����ڳ�ʼ������
	 * @return ��ʼ��ʱ����true�����򷵻�false
	 */
	public static boolean isIniting(){
		return isIniting;
	}
	
	/**
	 * ��������ĵ�����
	 * @throws Exception
	 */
	public void checkContext() throws Exception{
		if(unitContext==null){
			unitContext = new UnitContext(specifyHost,specifyPort);
			unitContext.setRaqsoftConfig(rc);
		}
	}
	
	/**
	 * ʹ�ò������÷ֻ��ĳ�ʼ���ļ�init.splx
	 * @param i����i���ֻ���i=0��ʾ�ֻ�����ʱ�̵ĵ���
	 * @param N����N���ֻ�
	 * @param j����������
	 * @return
	 */
	public static Response init(final int i, final int N, final String j){
		return init(i,N,j,true);
	}
	
	private static void outputInitMsg(){
		Exception x = initResult.getException();
		if(x!=null){
			Logger.debug(x.getMessage());
		}
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
		final String dfx = "init.splx";
		synchronized (initLock) {
			if(isIniting){
				initResult.setException( new Exception("UnitServer is initing, please try again later."));
				if(!waitResult){
					outputInitMsg();
				}
				return initResult;
			}
			FileObject fo = new FileObject(dfx, "s");
			if(!fo.isExists()){
				initResult.setException( new Exception(dfx+" is not exists."));
				if(!waitResult){
					outputInitMsg();
				}
				return initResult;
			}
			initResult.setException(null);
			isIniting = true;
		}
		
		Thread t = new Thread(){
			public void run(){
				int intId = UnitServer.nextId();
				String msg = "init("+i+","+N+","+j+") ";
				Logger.debug(msg+"begin.");
				List<Object> args = new ArrayList<Object>();
				args.add(i);
				args.add(N);
				args.add(j);
				String spaceId = UUID.randomUUID().toString();
				Task task = new Task(dfx, args, intId, spaceId);
				// TaskҲ���ӡ��ʼ����
				initResult = task.execute();
				Logger.debug(msg+" finished.");
				synchronized (initLock) {
					isIniting = false;
				}
				if(!waitResult){
					outputInitMsg();
				}
			}
		};
		
		t.start();
		if(waitResult){
			try {
				t.join();
			} catch (InterruptedException e) {
				initResult.setException(e);
			}
		}
		return initResult;
	}
	
	/**
	 * ���зֻ�����������
	 */
	public void run(){
//		Logger.info("Release date:"+version);
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run1"));
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run2",getHome()));

		try {
			checkContext();
			String host = unitContext.getLocalHost();
			InetAddress add = InetAddress.getByName(host);
			serverSocket = new ServerSocket(unitContext.getLocalPort(), unitContext.getBacklog(), add);
			int timeOut = 3;
			serverSocket.setSoTimeout(timeOut * 1000);
		} catch (Exception x) {
			if(listener!=null){
				listener.serverStartFail();
			}
			x.printStackTrace();
			return;
		}

		if (StringUtils.isValidString(Env.getTempPath())) {
			UnitContext uc = getUnitContext();
			int timeOut = uc.getTimeOut();
			int interval = uc.getInterval();
			tempFileMonitor = new TempFileMonitor(timeOut,interval);
			tempFileMonitor.start();
		}
		 Logger.debug("Using main path:"+Env.getMainPath());

		proxyMonitor = new ProxyMonitor();
		proxyMonitor.start();
		
		Logger.info(ParallelMessage.get().getMessage("UnitServer.run3", unitContext));
		ThreadGroup threadGroup = new ThreadGroup("UnitWorkerGroup");
		Response res = init(0,0,null);
		if(res.getException()!=null){
			Logger.debug(res.getException().getMessage());
		}
		setStop(false,unitContext.getLocalPort());

		while ( !stop ) {
			Socket socket = null;
			boolean isThreadStart = false;
			try {
				socket = serverSocket.accept();
				SocketData sd = new SocketData(socket);
				sd.holdCommunicateStreamServer();
				UnitWorker uw = new UnitWorker(threadGroup,"UnitWorker");
				uw.setSocket(sd);
				if( unitContext.isCheckClients() ){
					InetAddress ia = socket.getInetAddress();
					String client = ia.getHostAddress();
					if(!unitContext.checkClientIP( client )){
						uw.setErrorCheck( client );
					}
				}
				uw.start();
				isThreadStart = true;
			} catch (java.net.SocketTimeoutException ste) {
//	����˽��ܿͻ���socket��3���ʱ����ѭ�������������ܵ�ʱ�乩����stop��������񣬸��쳣��Ϣ���ڿ���̨���			
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			} catch (java.net.SocketException se) {
				se.printStackTrace();
			} catch (Throwable t) {//�����ڴ����ʱ
				t.printStackTrace(); // ĳ��socket�����쳣ʱ�����ܵ�������
			}finally{
				if(socket!=null && !isThreadStart){
//					����߳�������ǰ���Ѿ��쳣����������ر�socket
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		try{
			serverSocket.close();
		}catch(Exception x){}
		if (tempFileMonitor != null) {
			tempFileMonitor.stopThread();
		}
		proxyMonitor.stopThread();
		
		Thread[] threads = new Thread[threadGroup.activeCount()];
		threadGroup.enumerate(threads);
		for (int i = 0; i < threads.length; i++) {
			Thread t = threads[i];
			if (t.isAlive() && (t instanceof UnitWorker)) {
				((UnitWorker)t).shutdown();
			}
		}
		Logger.info(ParallelMessage.get().getMessage("UnitServer.runend", unitContext));
		instance = null;		
		if( isQuit ) {
			System.exit(0);
		}
	} // ��������

	/**
	 * �жϵ�ǰ�������Ƿ�����������
	 */
	public synchronized boolean isRunning() {
		return !stop;
	}
	
	private synchronized void setStop(boolean b,int port){
		stop = b;
		if(!stop && listener!=null){
			listener.serverStarted(port);
		}
	}

	boolean isQuit = false;
	/**
	 * �˳�������
	 */
	public void quit(){
		isQuit = true;
	}
	
	/**
	 * ֹͣ������
	 */
	public void shutDown() {
		stop = true;
	} // ��ֹ���з����̣߳������رշ�����

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
	 * ��������ں���
	 * @param args ��������
	 */
	public static void main(String[] args) {
		try {
			String specifyHost = null;
			int specifyPort = 0;
			Section sect = new Section();
			
			for (int i = 0; i < args.length; i++) {
				String buf = args[i];
//				��HostManager�������Ĳ���������������һ���ո�ֿ��Ĵ����ٴν⿪
				if(buf.indexOf(" ")>-1){
					StringTokenizer st = new StringTokenizer(buf," ");
					while(st.hasMoreTokens()){
						sect.addSection( st.nextToken() );
					}
				}else{
					sect.addSection(buf);
				}
			}
			args = sect.toStringArray();
			
			for (int i = 0; i < args.length; i++) {
				String buf = args[i];
				
				int index = buf.lastIndexOf(':');
				if (index > 0 && specifyHost == null) {
					specifyHost = buf.substring(0, index).trim();
					specifyPort = Integer.parseInt(buf.substring(index + 1).trim());
				}
			}

			UnitServer server = UnitServer.getInstance(specifyHost,specifyPort);
			server.run();
		} catch (Exception x) {
			x.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * ��ȡ��ǰ��������Host
	 */
	public String getHost() {
		return unitContext.toString();
	}

	/**
	 * ��ȡ�ֻ�IP
	 * @return ip��ַ
	 */
	public String getIP() {
		return unitContext.getLocalHost();
	}
	
	/**
	 * ��ǰ�������Ƿ����Զ�������
	 */
	public boolean isAutoStart() {
		try{
			checkContext();
			return unitContext.isAutoStart();
		}catch(Exception x){
			return false;
		}
	}

}
