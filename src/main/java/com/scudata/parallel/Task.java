package com.scudata.parallel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.CellLocation;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.CanceledException;
import com.scudata.dm.Context;
import com.scudata.dm.DfxManager;
import com.scudata.dm.FileObject;
import com.scudata.dm.IResource;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.ParallelCaller;
import com.scudata.dm.ParallelProcess;
import com.scudata.dm.RetryException;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.odbc.OdbcServer;
import com.scudata.server.unit.JdbcTask;
import com.scudata.server.unit.UnitServer;
import com.scudata.thread.Job;
import com.scudata.util.CellSetUtil;
import com.scudata.util.DatabaseUtil;

/**
 * ÿһ����ҵ�Ĺ���
 * 
 * ÿ����ҵ��Ҳ��Ϊ���񣩲������α궼��¼�������ϣ��α�����󣬿�ʼ��ʱ��ʱ 
 * �α��ÿһ�η��ʹ��̣������Ƴ�ʱ��Ҳ��һ��fetch����������ܳ���ʱ�䣬�ǲ���ʱ�ģ�
 * �α���ʹ������¿�ʼ��ʱ�� ���һ����������˶���α�󣬴�ʱ�Ѿ���ʼ��ʱ��
 * ���dfx�ڰ�����������ͷ�����α꣬���ں�����α���Ȼ���ܳ�ʱ ����
 * 
 * @author Joancy
 *
 */
public class Task extends Job implements IResource, ITask {
	Object dfxName;
	boolean isDfxFile = false;//��ס��ǰdfx����Դ������Ǵ��ļ�������ʹ�û���
	ArrayList args;
	String spaceId;
	boolean isProcessCaller = false;
	Object reduce;
	CellLocation accumulateLocation;
	CellLocation currentLocation;

	// �ֽ��̶˿ںţ���������ҵʱ�����ػ�ֱ�ӽ���ҵ���õ��ֻ��ľ���ֽ���
//	int subPort = 0;
	int processTaskId = 0;// �����̵�������

	int taskId = -1;
	long callTime = -1;
	long finishTime = -1;

	RemoteCursorProxyManager rcpm = null;
	transient Object tasker = null;
	transient boolean isCanceled = false;
	transient Response res = null;

	private long lastAccessTime = -1;
	private static List connectedDsNames = null;// �����IDE��ִ�У���IDE���øñ����������UnitContext��ConfigBean��ȡ
	private String cancelCause = null;
	transient Context context;
	MessageManager mm = ParallelMessage.get();

	/**
	 * ����һ������
	 * @param dfxName Ҫִ�е�dfx
	 * @param argList ��Ӧ�Ĳ���
	 * @param taskId �����
	 * @param spaceId �ռ��
	 */
	public Task(Object dfxName, List argList, int taskId, String spaceId) {
		this.dfxName = dfxName;
		this.args = (ArrayList) argList;
		this.taskId = taskId;
		this.spaceId = spaceId;

		JobSpaceManager.getSpace(spaceId).getResourceManager().add(this);
	}

	/**
	 * ����һ������
	 * @param dfxName Ҫִ�е�dfx
	 * @param argList �����б�
	 * @param taskId �����
	 * @param spaceId �ռ��
	 * @param isProcessCaller �Ƿ�������
	 * @param reduce reduce���ʽ
	 */
	public Task(Object dfxName, List argList, int taskId, String spaceId,
			boolean isProcessCaller, Object reduce,CellLocation accumulateLocation,
			CellLocation currentLocation) {
		this(dfxName, argList, taskId, spaceId);
		this.isProcessCaller = isProcessCaller;
		this.reduce = reduce;
		this.accumulateLocation = accumulateLocation;
		this.currentLocation = currentLocation;
}


	/**
	 * ���������̵������
	 * @param id
	 */
	public void setProcessTaskId(int id) {
		this.processTaskId = id;
	}

	/**
	 * �����Ѿ����Ӻ������Դ���� 
	 * @param dsNames ����Դ�����б�
	 */
	public static void setConnectedDsNames(List dsNames) {
		connectedDsNames = dsNames;
	}

	/**
	 * �������ķ���ˢ��
	 */
	public void access() {
		lastAccessTime = System.currentTimeMillis();
	}

	/**
	 * ���ô���ķ���ˢ��
	 */
	public void resetAccess() {
		lastAccessTime = -1;
	}

	/**
	 * ���ٵ�ǰ����
	 */
	public void destroy() {
		if (rcpm != null) {
			rcpm.destroy();
			rcpm = null;
		}
		DatabaseUtil.closeAutoDBs(context);
		isClosed = true;
	}

	/**
	 * ����ǰ��ʱ��
	 */
	private void beforeExecute() {
		callTime = System.currentTimeMillis();
	}

	/**
	 * ȡ�α������
	 * @return �α���������
	 */
	public RemoteCursorProxyManager getCursorManager() {
		if (rcpm == null) {
			rcpm = new RemoteCursorProxyManager(this);
		}
		return rcpm;
	}

	/**
	 * �жϵ�ǰ��ҵ�Ƿ�����״̬
	 * @return ������״̬����true�����򷵻�false
	 */
	public boolean isRunning() {
		return tasker != null;
	}

	/**
	 * ��������ת��Ϊ�α���ʶ���
	 * @param result ������
	 * @return �α�ӿ�
	 */
	public static ICursor toCursor(Object result) {
		if (result instanceof Sequence) {
			Sequence t = (Sequence) result;
			if (t.length() > 0) {
				MemoryCursor mc = new MemoryCursor(t);
				return mc;
			} else {
				result = "";
			}
		}
		if (result instanceof ICursor) {
			return (ICursor) result;
		}
		String[] fields;
		Object[] values;
		if (result instanceof BaseRecord) {
			BaseRecord rec = (BaseRecord) result;
			fields = rec.getFieldNames();
			values = rec.getFieldValues();
		} else {
			fields = new String[] { "_1" };
			values = new Object[] { result };
		}

		Table table = new Table(fields);
		table.newLast(values);
		MemoryCursor mc = new MemoryCursor(table);
		return mc;
	}

	/**
	 * �÷�������ODBC������ִ��dfx����Ҫ���Intergration
	 * @return ���������α���
	 * @throws Exception �������ʱ�׳��쳣
	 */
	public ICursor[] executeOdbc() throws Exception {
		Object obj = doExecute(true);
		if (obj instanceof Response) {
			Response res = (Response) obj;
			if (res.getError() != null) {
				throw res.getError();
			}
			if (res.getException() != null) {
				throw res.getException();
			}
			return null;
		}

		Sequence results = (Sequence) obj;
		int size = results.length();

		ICursor[] cursors = new ICursor[size];
		for (int i = 1; i <= size; i++) {
			cursors[i - 1] = toCursor(results.get(i));
		}
		return cursors;
	}

	/**
	 * ִ�м���
	 * @return ������ɺ����Ӧ
	 */
	public Response execute() {
		Object obj = doExecute(false);
		if (obj instanceof Response) {
			return (Response) obj;
		}

		Response res = new Response();
		res.setResult(obj);
		return res;
	}

	// ��ȡ���Ǵ�Datastore��ʱ��������ͨ�쳣��Ŀǰ��CanceledException�ǲ���Ҫ�жϱ��˵ġ�
	// ����DataStore������ȡ������Ҫ�жϱ��ˡ�
	private Exception getCancelException() {
		if (cancelCause != null) {
			MessageManager mm = ParallelMessage.get();

			String status = mm.getMessage("Task.cancel", this, cancelCause);
			if (cancelCause.equalsIgnoreCase(CanceledException.TYPE_MONITOR)) {
				return new Exception(status);
			}
		}
		return new CanceledException(cancelCause);
	}

	/**
	 * ׼������ǰ�������Ļ��� 
	 * ��ǰ�������ܲ����Զ����ӣ�ע�����ú󣬱�����Ե���DatabaseUtil.closeAutoDbs
	 * @return ����������
	 */
	
	public static Context prepareEnv() throws Exception{
		Context context = new Context();
		if (connectedDsNames == null) {
			UnitServer us = UnitServer.instance;
			OdbcServer os = OdbcServer.instance;
			if (us != null) {
				if (us.getRaqsoftConfig() != null) {
					connectedDsNames = us.getRaqsoftConfig()
							.getAutoConnectList();
				}
			} else if (os != null) {
				if (os.getRaqsoftConfig() != null) {
					connectedDsNames = os.getRaqsoftConfig()
							.getAutoConnectList();
				}
			}
		}
		DatabaseUtil.connectAutoDBs(context, connectedDsNames);
//		Esprocx.loadDataSource(context);
		return context;
	}

	private Sequence executeTask() throws Exception {
		context = prepareEnv();
		PgmCellSet pcs = getPgmCellSet(context);
		tasker = pcs;

		JobSpace js = JobSpaceManager.getSpace(spaceId);
		context.setJobSpace(js);
		context.addResource(this);

		pcs.setContext(context);
		Object[] argsVal = null;
		if (args != null) {
			argsVal = args.toArray();
		}

		if (ParallelCaller.isScript(dfxName)) {
			// ����еĲ������̶���"arg"��ͷ
			if (argsVal != null && argsVal.length > 0) {
				for (int i = 0; i < argsVal.length; i++) {
					context.setParamValue("arg" + (i + 1), argsVal[i]);
				}
			}
		} else {
			CellSetUtil.putArgValue(pcs, argsVal);
		}
		pcs.calculateResult();

		if (pcs.getInterrupt()) {
			throw getCancelException();
		}

		Sequence results = new Sequence();
		UnitServer server = UnitServer.instance;
		boolean isLocalExecute = (server == null);
		UnitContext uc = null;
		if (!isLocalExecute) {
			uc = server.getUnitContext();
		}

		while (pcs.hasNextResult()) {
			Object tmp = JdbcTask.checkResult( pcs.nextResult() );
			if (!isLocalExecute && (tmp instanceof ICursor)) {
				int proxyId = UnitServer.nextId();
				RemoteCursorProxyManager rcpm = getCursorManager();
				RemoteCursorProxy rcp = new RemoteCursorProxy(rcpm,
						(ICursor) tmp, proxyId);
				rcpm.addProxy(rcp);
				RemoteCursor rc = new RemoteCursor(uc.getLocalHost(),
						uc.getLocalPort(), taskId, proxyId);// , context);
				context.addResource(rc);
				results.add(rc);
			} else {
				results.add(tmp);
			}
		}
		return results;
	}

	long taskBegin = 0;

	private Object doExecute(boolean isODBC) {
		beforeExecute();
		try {
			if (isCanceled) {
				throw getCancelException();
			}

			taskBegin = System.currentTimeMillis();
			Object result = null;
			if (isProcessCaller) {
				ParallelProcess pp = new ParallelProcess(dfxName);
				tasker = pp;
				List<List> multiArgs = (List<List>) args;
				for (int i = 0; i < multiArgs.size(); i++) {
					pp.addCall(multiArgs.get(i));
				}
				pp.setJobSpaceId(spaceId);
				pp.setReduce(reduce,accumulateLocation,currentLocation);
				pp.setProcessTaskId(taskId);
				result = pp.execute();
			} else {
				Sequence seq = executeTask();
				if (isODBC) {
					result = seq;
				} else {
					if (seq.length() == 1) {
						result = seq.get(1);
					} else {
						result = seq;
					}
				}
			}
			return result;
		} catch (Throwable x) {
			// �����̵Ĵ�����Ϣ����ӡ���Ѿ����ӽ��̴�ӡ����
			if (!isProcessCaller) {
				Logger.debug(this, x);
			}

			Response res = new Response();
			if (x instanceof Error) {
				res.setError((Error) x);
			} else if (x instanceof Exception) {
				HostManager hm = HostManager.instance();
				String msg = "[" + hm + "] ";
				String causemsg = x.getMessage();
				if(causemsg!=null){
					if (causemsg.startsWith("[")) {
						msg = causemsg;
					} else {
						msg += causemsg;
					}
				}
				Exception ex;
				if (x instanceof RetryException) {
					ex = new RetryException(msg, x);
				} else {
					ex = new Exception(msg, x);
				}

				res.setException(ex);
			}
			return res;
		} finally {
			if (tasker != null) {
				if (tasker instanceof PgmCellSet) {
					finishTime = System.currentTimeMillis();
					((PgmCellSet) tasker).reset();
					 if(isDfxFile){
						 DfxManager.getInstance().putDfx((PgmCellSet) tasker);
					 }
				} else {
					ParallelProcess pp = (ParallelProcess) tasker;
					pp.close();
				}
				tasker = null;
			}

			if (rcpm == null) { // ���������α�ʱ��Ҫ�ȴ��α궼�ر�ʱ����ɾ������
				TaskManager.delTask(taskId);
			}
			access();
		}
	}

	/**
	 * ȡ����ǰ��ҵ
	 * @return ȡ���������Ӧ
	 */
	public Response cancel() {
		return cancel(null);
	}

	/**
	 * ȡ����ǰ��ҵ
	 * @param reason ��ȡ����Ϣд��ԭ��
	 * @return ȡ���������Ӧ
	 */
	public Response cancel(String reason) {
		cancelCause = reason;
		Response res = new Response();
		if (tasker == null) {
			if (callTime == -1) {
				res.setResult(Boolean.TRUE);
				isCanceled = true;
			} else {
				res.setException(new Exception("Task is finished."));
			}
		} else {
			if (tasker instanceof PgmCellSet) {
				((PgmCellSet) tasker).interrupt();
			} else {
				((ParallelProcess) tasker).cancel(reason);
			}

			res.setResult(Boolean.TRUE);
			Logger.debug(this + " is canceled.");
		}
		return res;
	}

	/**
	 * ��ȡdfx����
	 * @return ����
	 */
	public String getDfxName() {
		return ParallelCaller.dfxDelegate(dfxName);
	}

	/**
	 * ��ȡ�����б�
	 * @return �����б�
	 */
	public List getArgList() {
		return args;
	}

	/**
	 * ��ȡ������
	 * @return ������
	 */
	public int getTaskID() {
		return taskId;
	}

	/**
	 * ȡ����ʼ�����ʱ��
	 * @return ����ʼʱ��(ȱʡ-1)
	 */
	public long getCallTime() {
		return callTime;
	}

	/**
	 * ��������ʼ�����ʱ��
	 * @param callTime ʱ��
	 */
	public void setCallTime(long callTime) {
		this.callTime = callTime;
	}

	/**
	 * ȡ���������ɺ��ʱ��
	 * @return �������ʱ��(ȱʡ-1)
	 */
	public long getFinishTime() {
		return finishTime;
	}

	/**
	 * ��������������ʱ��ʱ��
	 * @param finishTime ���ʱ��
	 */
	public void setFinishTime(long finishTime) {
		this.finishTime = finishTime;
	}

	PgmCellSet getPgmCellSet(Context ctx) {
		DfxManager dfxManager = DfxManager.getInstance();
		PgmCellSet pcs;
		if (ParallelCaller.isScript(dfxName)) {
			String dfx = (String) dfxName;
			pcs = CellSetUtil.toPgmCellSet(dfx);
		} else if (dfxName instanceof String) {
			String dfx = (String) dfxName;
			FileObject fo = new FileObject(dfx, "s");
			isDfxFile = true;
			pcs = dfxManager.removeDfx(fo, ctx);
		} else {
			pcs = (PgmCellSet) dfxName;
		}
		return pcs;
	}

	/**
	 * ���������ķ��ʳ�ʱ
	 */
	public boolean checkTimeOut(int timeOut) {
		if (lastAccessTime < 0) {
			return false; // ��û����������ܼ�����
		}
		// ������룬timeOut��λΪ��
		long unvisit = (System.currentTimeMillis() - lastAccessTime) / 1000;
		if (unvisit > timeOut) {
			Logger.info(this + " is timeout.");
			destroy();
			return true;
		}
		return false;
	}

	/**
	 * ��������toStringʵ��
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();

		if (processTaskId == 0) {
			sb.append(" [" + ParallelCaller.dfxDelegate(dfxName) + "] ");
			sb.append(ParallelCaller.args2String(args));
			sb.append(mm.getMessage("Task.taskid", taskId));
		} else {
			sb.append(mm
					.getMessage("Task.taskAndMainId", taskId, processTaskId));
		}

		System.out.println(sb);
		return sb.toString();
	}

	/**
	 * ��ʼ����
	 */
	public void run() {
		long l1 = System.currentTimeMillis();
//		��ҵ�����������㼶��ProcessCaller�ӵ�һ����ҵ�������ת��Ϊ����������ҵ�߳�
//		�����㼶���������run��������һ���ProcessCaller����Ϣ����
		if (!(isProcessCaller && args.size()<2)) {
			Logger.debug(mm.getMessage("Task.taskBegin", this));
		}
		res = execute();
		long l2 = System.currentTimeMillis();
		DecimalFormat df = new DecimalFormat("###,###");
		long lastTime = l2 - l1;
		if (!(isProcessCaller && args.size()<2)) {
			Logger.debug(mm.getMessage("Task.taskEnd", this, df.format(lastTime)));
		}
	}

	/**
	 * �жϵ�ǰ�����Ƿ�����������Ŀǰ��ʱȡ���˷ֽ��̣����ǳ����߼���Ȼ�������̽ӵ���ҵ�������ηָ��ֽ��̣���ʱ�ķֽ�����Ȼ���Լ���
	 * ����ڼ�ع���ʱ����Ҫ�˵�������������Ϊ��ʱ�����̵���ҵ���ֽ�����һ������ҵ��
	 * @return
	 */
	public boolean isProcessCaller() {
		return isProcessCaller;
	}
	public void setProcessCaller( boolean b) {
		isProcessCaller = b;
	}
	public void setReduce(Object reduce,CellLocation accuLoc, CellLocation currentLoc) {
		this.reduce = reduce;
		this.accumulateLocation = accuLoc;
		this.currentLocation = currentLoc;
	}
	/**
	 * ��ȡ������ɵ���Ӧ
	 * @return ��Ӧ����
	 */
	public Response getResponse() {
		return res;
	}

	boolean isClosed = false;

	/**
	 * �رյ�ǰ��ҵ���ͷ������Դ
	 */
	public void close() {
		if (!isClosed) {
			destroy();
			TaskManager.delTask(taskId);
		}
	}

}
