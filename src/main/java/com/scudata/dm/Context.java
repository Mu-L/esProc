package com.scudata.dm;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.DBSession;
import com.scudata.common.ISessionFactory;
import com.scudata.expression.DfxFunction;

/**
 * �����õ���������
 * @author WangXiaoJun
 *
 */
public class Context {
	private Context parent; // ��������
	private JobSpace js; // ��ҵ�ռ�
	private Map<String, DBSession> dbSessions = new HashMap<String, DBSession>();// DBSessionӳ��
	private Map<String, ISessionFactory> dbsfs = new HashMap<String, ISessionFactory>(); // ISessionFactoryӳ��

	private ParamList paramList = new ParamList(); // �����б�
	private ComputeStack computeStack = new ComputeStack(); // �����ջ
	private String defDsName; // ȱʡ���ݿ���������

	private Random random; // �����������ֵ
	private Param iterateParam = new Param(KeyWord.ITERATEPARAM, Param.VAR, null); // ��������
	
	/**
	 * ���������Ķ���
	 */
	public Context() {
	}

	/**
	 * ���������Ķ���, �͸������Ĺ��������
	 * @param parent ��������
	 */
	public Context(Context parent) {
		this.parent = parent;
	}

	/**
	 * ���ø�������
	 * @param parent Context
	 */
	public void setParent(Context parent) {
		this.parent = parent;
	}

	/**
	 * ȡ�ø�����������
	 * @return  ��������
	 */
	public Context getParent() {
		return parent;
	}

	/**
	 * ������ȡ���ݿ��OLAP����
	 * @param dbName ���ݿ��OLAP��
	 * @return DataSourceConfig
	 */
	public DBSession getDBSession(String dbName) {
		DBSession ds = dbSessions.get(dbName);
		if (ds != null) return ds;

		if (parent != null) {
			return parent.getDBSession(dbName);
		} else {
			return null;
		}
	}

	/**
	 * �����������ݿ�����
	 * @return Map name��DBSession
	 */
	public Map<String, DBSession> getDBSessionMap() {
		return dbSessions;
	}

	/**
	 * �����������ݿ��OLAP����
	 * @param dbName ����Դ��
	 * @param dbSession ����Դ����
	 */
	public void setDBSession(String dbName, DBSession dbSession) {
		dbSessions.put(dbName, dbSession);
	}

	/**
	 * ɾ�����ݿ�����
	 * @param dbName String ���ݿ�������
	 * @return DBSession
	 */
	public DBSession removeDBSession(String dbName) {
		DBSession ds = dbSessions.remove(dbName);
		if (ds != null) return ds;
		return parent == null ? null : parent.removeDBSession(dbName);
	}

	/**
	 * ��ȡ���ݿ����ӹ���
	 * @param name String ���ݿ�����
	 * @return ISessionFactory ���ݿ����ӹ���
	 */
	public ISessionFactory getDBSessionFactory(String name) {
		ISessionFactory sf = dbsfs.get(name);
		if (sf != null) return sf;
		return parent == null ? null : parent.getDBSessionFactory(name);
	 }

	 /**
	  * �����������ݿ����ӹ���
	  * @return Map name��ISessionFactory
	  */
	 public Map<String, ISessionFactory> getDBSessionFactoryMap() {
		 return dbsfs;
	 }

	 /**
	  * �������ݿ����ӹ���
	  * @param name String ���ݿ�����
	  * @param sf ISessionFactory ���ݿ����ӹ���
	  */
	 public void setDBSessionFactory(String name, ISessionFactory sf) {
		 dbsfs.put(name, sf);
	 }

	 /**
	  * ɾ�����ݿ����ӹ���
	  * @param name String
	  * @return ISessionFactory
	  */
	 public ISessionFactory removeDBSessionFactory(String name) {
		 ISessionFactory sf = dbsfs.remove(name);
		 if (sf != null) return sf;
		 return parent == null ? null : parent.removeDBSessionFactory(name);
	 }

	/**
	 * ������ȡ����
	 * @param name ������
	 * @return DataStruct
	 */
	public Param getParam(String name) {
		// ���ٴӸ�������ȡ
		// ��·�α����Լ��������ģ�������·�α����dfx�ٴ���������·�α�����ʲ�����
		Param param = paramList.get(name);
		if (param != null) return param;
		return parent == null ? null : parent.getParam(name);
	}

	/**
	 * ���ز����б�
	 * @return ParamList
	 */
	public ParamList getParamList() {
		return paramList;
	}

	/**
	 * ���ò����б�
	 * @param list ParamList
	 */
	public void setParamList(ParamList list) {
		if (list == null) {
			this.paramList = new ParamList();
		} else {
			this.paramList = list;
		}
	}

	/**
	 * ��ӱ���
	 * @param param ����
	 */
	public void addParam(Param param) {
		paramList.add(param);
	}

	/**
	 * ������ɾ������
	 * @param name String
	 * @return Param
	 */
	public Param removeParam(String name) {
		return paramList.remove(name);
		// ���ٴӸ�������ɾ
		//if (param != null) return param;
		//return parent == null ? null : parent.removeParam(name);
	}

	/**
	 * ���ò�����ֵ��������������������һ��VAR���͵Ĳ���
	 * @param name String
	 * @param value Object
	 */
	public void setParamValue(String name, Object value) {
		Param p = getParam(name);
		if (p == null) {
			addParam(new Param(name, Param.VAR, value));
		} else {
			p.setValue(value);
		}
	}

	/**
	 * ���ò�����ֵ��������������������һ��paramType���͵Ĳ���
	 * @param name String
	 * @param value Object
	 * @param paramType byte Param.VAR  ARG
	 */
	public void setParamValue(String name, Object value, byte paramType) {
		Param p = getParam(name);
		if (p == null) {
			addParam(new Param(name, paramType, value));
		} else {
			p.setValue(value);
		}
	}

	/**
	 * ȡ�����ջ�����ڳ�Ա�����������ѹջ
	 * @return
	 */
	public ComputeStack getComputeStack() {
		return computeStack;
	}

	/**
	 * ����ȱʡ���ݿ���������
	 * @return String
	 */
	public String getDefDBsessionName() {
		return this.defDsName;
	}

	/**
	 * ����ȱʡ���ݿ���������
	 * @param dsn String
	 */
	public void setDefDBsessionName(String dsn) {
		this.defDsName = dsn;
	}

	/**
	 * ����ȱʡ���ݿ�����
	 * @return DBSession
	 */
	public DBSession getDefDBsession() {
		return getDBSession(defDsName);
	}

	// ����һ�������ӵ����ݿ�
	public DBSession getDBSession() {
		if (dbSessions.size() != 0) {
			return dbSessions.values().iterator().next();
		}

		if (parent != null) {
			return parent.getDBSession();
		} else {
			return null;
		}
	}

	/**
	 * ����random����
	 * @return Random
	 */
	public Random getRandom() {
		if (random == null) random = new Random();
		return random;
	}

	/**
	 * ���ظ�Ϊseed��random����
	 * @param seed long ��
	 * @return Random
	 */
	public Random getRandom(long seed) {
		if (random == null) {
			random = new Random(seed);
		} else {
			random.setSeed(seed);
		}

		return random;
	}

	/**
	 * ���ù����ռ�
	 * @param space �����ռ�
	 */
	public void setJobSpace(JobSpace space) {
		this.js = space;
	}

	/**
	 * ȡ�����ռ�
	 * @return JobSpace
	 */
	public JobSpace getJobSpace() {
		return js;
	}

	/**
	 * ȡ��Դ������
	 * @return ResourceManager
	 */
	public ResourceManager getResourceManager() {
		if (js != null) {
			return js.getResourceManager();
		} else {
			return null;
		}
	}
	
	/**
	 * ���ָ����Դ����Դ������
	 * @param resource ��Դ
	 */
	public void addResource(IResource resource) {
		ResourceManager rm = getResourceManager();
		if (rm != null) {
			rm.add(resource);
		}
	}
	
	/**
	 * ��ָ����Դ����Դ��������ɾ��
	 * @param resource ��Դ
	 */
	public void removeResource(IResource resource) {
		ResourceManager rm = getResourceManager();
		if (rm != null) {
			rm.remove(resource);
		}
	}
	
	/**
	 * �����µļ��㻷��
	 * @return Context
	 */
	public Context newComputeContext() {
		// ����ָ��thisΪ����func���ò����������׵���StackOverflowError�쳣
		Context ctx = new Context();
		ctx.js = js;
		ctx.dbSessions = dbSessions;
		ctx.dbsfs = dbsfs;
		ctx.defDsName = defDsName;

		ParamList paramList = this.paramList;
		ParamList paramList2 = ctx.paramList;
		for (int i = 0, count = paramList.count(); i < count; ++i) {
			Param p = paramList.get(i);
			paramList2.add(new Param(p));
		}

		return ctx;
	}
	
	/**
	 * ����ָ�������ĵļ��㻷���������Ʊ���
	 * @param ctx ����������
	 */
	public void setEnv(Context ctx) {
		js = ctx.js;
		dbSessions = ctx.dbSessions;
		dbsfs = ctx.dbsfs;
		defDsName = ctx.defDsName;
	}

	/**
	 * ȡ��������
	 * @return Param
	 */
	public Param getIterateParam() {
		return iterateParam;
	}
	
	/**
	 * ��ӳ���������
	 * @param fnName ������
	 * @param dfxPathName ������·����
	 */
	public void addDFXFunction(String fnName, String dfxPathName, String opt) {
		js.addDFXFunction(fnName, dfxPathName, opt);
	}
	
	/**
	 * ��ӳ���������
	 * @param fnName ������
	 * @param funcInfo ��������Ϣ
	 */
	public void addDFXFunction(String fnName, PgmCellSet.FuncInfo funcInfo) {
		js.addDFXFunction(fnName, funcInfo);
	}
	
	/**
	 * ɾ������������
	 * @param fnName ������
	 */
	public void removeDFXFunction(String fnName) {
		js.removeDFXFunction(fnName);
	}
	
	/**
	 * ���ݺ�����ȡ������
	 * @param fnName ������
	 * @return ����������
	 */
	public DfxFunction getDFXFunction(String fnName) {
		if (js != null) {
			return js.getDFXFunction(fnName);
		} else {
			return null;
		}
	}
}
