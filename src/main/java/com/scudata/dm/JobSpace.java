package com.scudata.dm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.expression.DfxFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.FunctionLib;
import com.scudata.parallel.UnitClient;
import com.scudata.resources.EngineMessage;

/**
 * ����ռ�
 * @author RunQian
 *
 */
public class JobSpace {
	private String id;
	private ParamList paramList = new ParamList(); // ���ȫ�̱���
	private long lastAccess = System.currentTimeMillis(); // ������ʱ��
	private File appHome = null;

	private ArrayList<UnitClient> unitClients = new ArrayList<UnitClient>();
	private ResourceManager rm = new ResourceManager();

	// ����������ӳ���[������,������·����]
	private HashMap<String, DfxFunction> dfxFnMap = new HashMap<String, DfxFunction>(256);
	
	 public JobSpace(String ID) {
		this.id = ID;
	}

	public String toString(){
		return "JobSpace "+id;
	}
	
	public String description(){
		StringBuffer sb = new StringBuffer();
		sb.append("[ "+toString());
		if(paramList.count()>0){
			sb.append(" [Params:");
			int n = paramList.count();
			for(int i=0;i<n;i++){
				if(i>0){
					sb.append(",");
				}
				sb.append(paramList.get(i).getName());
			}
			sb.append(" ]");
		}
		if(appHome!=null){
			sb.append(" App home:");
			sb.append( appHome );
		}
		if(unitClients.size()>0){
			sb.append(" [Callx nodes:");
			int n = unitClients.size();
			for(int i=0;i<n;i++){
				if(i>0){
					sb.append(",");
				}
				sb.append(unitClients.get(i));
			}
			sb.append(" ]");
		}
		
		sb.append(" ]");
		return sb.toString();
	}
	
	public String getID() {
		lastAccess = System.currentTimeMillis();
		return id;
	}

	/**
	 * ȡ���б���
	 * 
	 * @return Param[]
	 */
	public Param[] getAllParams() {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();

			int size = paramList.count();
			Param[] params = new Param[size];
			for (int i = 0; i < size; ++i) {
				params[i] = paramList.get(i);
			}

			return params;
		}
	}

	/**
	 * ������ȡ����
	 * 
	 * @param name ������
	 * @return DataStruct
	 */
	public Param getParam(String name) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			return paramList.get(name);
		}
	}

	/**
	 * ��ӱ���
	 * 
	 * @param param ����
	 */
	public void addParam(Param param) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			paramList.add(param);
		}
	}

	/**
	 * ������ɾ������
	 * 
	 * @param name String
	 * @return Param
	 */
	public Param removeParam(String name) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			return paramList.remove(name);
		}
	}

	/**
	 * ɾ�����б���
	 */
	public void clearParam() {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			paramList.clear();
		}
	}

	/**
	 * ���ñ�����ֵ��������������������һ��
	 * 
	 * @param name String ������
	 * @param value Object ����ֵ
	 */
	public void setParamValue(String name, Object value) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();

			Param p = paramList.get(name);
			if (p == null) {
				paramList.add(new Param(name, Param.VAR, value));
			} else {
				p.setValue(value);
			}
		}
	}
	
	// ����ס�����ټ���x��Ϊ��֧��ͬ����env(v,v+n)
	public Object setParamValue(String name, Expression x, Context ctx) {
		Param p;
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			p = paramList.get(name);
			if (p == null) {
				p = new Param(name, Param.VAR, null);
				paramList.add(p);
			}
		}
		
		synchronized(p) {
			Object value = x.calculate(ctx);
			p.setValue(value);
			return value;
		}
	}

	/**
	 * ����������ʱ��
	 * 
	 * @return long
	 */
	public long getLastAccessTime() {
		return lastAccess;
	}

	public void addHosts(String host, int port) { // callx��������ָ��host�󣬽�host����hosts�������·���ʱ��
		UnitClient uc = new UnitClient(host, port);
		synchronized (unitClients) {
			if (!unitClients.contains(uc)) {
				unitClients.add(uc);
			}
		}
	}
	
	public void close() {
		close(true);
	}
	
	public void closeResource(){
		close(false);
	}
	
	private void close(boolean paramCleared){
		//���������������callx(h)����Ҫ���������h����space�����ڴ˴�֪ͨ�ֻ��رտռ�
		//�ֻ������̵��÷ֽ��̲����Ŀռ䲻���طֻ�Units��Ϣ�����������ڹرտռ�ʱ����HostManager�ҳ��Լ���units�ٹر�		
		for (int i = 0; i < unitClients.size(); i++) {
			UnitClient uc = unitClients.get(i);
			uc.closeSpace(id);
		}
		
		if(paramCleared) paramList.clear();
		rm.close();
		DfxManager.getInstance().clear();
	}

	public boolean checkTimeOut(int timeOut) {
		// ������룬timeOut��λΪ��
		long unvisit = (System.currentTimeMillis() - lastAccess) / 1000;
		if (unvisit > timeOut) {
			// destroy();
			return true;
		}
		return false;
	}

	/**
	 * ȡ��Դ������
	 * @return
	 */
	public ResourceManager getResourceManager() {
		return rm;
	}
	
	/** ��Ӧ����Ŀ¼ */
	public void setAppHome(File f){
		this.appHome = f;
	}
	/** ȡӦ����Ŀ¼ */
	public File getAppHome() {
		return this.appHome;
	}
	/** ȡӦ�ó���Ŀ¼ */
	public File getAppProgPath() {
		return new File(this.appHome, "prog");
	}
	
	/**
	 * ��ӳ���������
	 * @param fnName ������
	 * @param dfxPathName ������·����
	 */
	public void addDFXFunction(String fnName, String dfxPathName, String opt) {
		// ������ȫ�ֺ�������
		if (FunctionLib.isFnName(fnName)) {// || dfxFnMap.containsKey(fnName)
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(
					mm.getMessage("FunctionLib.repeatedFunction") + fnName);
		}

		// ���º����滻�ɵ�
		DfxFunction old = dfxFnMap.put(fnName, new DfxFunction(dfxPathName, opt));
		if (old != null) {
			// �������
			DfxManager.getInstance().clearDfx(dfxPathName);
		}
	}

	/**
	 * ��ӳ���������
	 * @param fnName ������
	 * @param funcInfo ��������Ϣ
	 */
	public void addDFXFunction(String fnName, PgmCellSet.FuncInfo funcInfo) {
		dfxFnMap.put(fnName, new DfxFunction(funcInfo));
	}
	
	/**
	 * ɾ������������
	 * @param fnName ������
	 */
	public void removeDFXFunction(String fnName) {
		dfxFnMap.remove(fnName);
	}

	/**
	 * ���ݺ�����ȡ������
	 * @param fnName ������
	 * @return ����������
	 */
	public DfxFunction getDFXFunction(String fnName) {
		return dfxFnMap.get(fnName);
	}
}
