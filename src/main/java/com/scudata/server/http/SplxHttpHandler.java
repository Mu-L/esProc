package com.scudata.server.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.FileUtils;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.SegmentSet;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.mfn.sequence.Export;
import com.scudata.ide.spl.Esprocx;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.thread.Job;
import com.scudata.util.CellSetUtil;
import com.scudata.util.DatabaseUtil;
import com.scudata.util.Variant;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Http��������
 * 
 * ֧�ֵ�urlд��:
 * 1  http://..../splx()
 * 2  http://..../splx(arg1,arg2,...)
 * 3  http://localhost:.../splx1(...)splx2.splx
 * 4  http://localhost:.../.../splx?arg1=v1&arg2=v2  ���ܴ���չ��������������ʺź�Ĳ���
 * 5  http://localhost:.../.../splx/arg1/arg2/...    restful���arg1��ʼ���ǲ���ֵ����˳����splx�еĲ�����Ӧ
 * HTTP�����url����ʽ�������������
 * 1��ֻ��һ��splx�ļ������splx�в�������������������д����ֵ��ֵ���ö��ŷָ���
 * ���û�в�������д�������š� ��������ϵͳĬ�ϸ�ʽת�����ַ�������
 * 2��������splx��������������û���Ҫ�ķ��ش���ʽ��ϵͳĬ�ϵĲ�ͬ��
 * 		 ��1��splx��д�����1�������ͬ��splx2�ǵ������ĳ��򣬲���ֵΪsplx1�ķ���ֵ��
 * �û���splx2�н�splx1�ķ���ֵת�����Լ���Ҫ��ʽ���ַ�����Ȼ����Ϊsplx2�ķ���ֵ
 *  
 * @author Joancy
 *
 */
public class SplxHttpHandler implements HttpHandler {
	private IServer server = null;
	static MessageManager mm = ParallelMessage.get();

	/**
	 * ���÷�����
	 * @param is ������
	 */
	public void setIServer(IServer is) {
		server = is;
	}

	/**
	 * ����Http����
	 */
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			if (HttpContext.dfxHome == null) {
				String mainPath = Env.getMainPath();
				if (mainPath == null || mainPath.trim().length() == 0) {
					throw new Exception(
							mm.getMessage("DfxHttpHandler.emptyMain"));
				}
				mainPath = StringUtils.replace(mainPath, "\\", "/");
				HttpContext.dfxHome = mainPath.trim();
			}
			URI uri = httpExchange.getRequestURI();
			String path = uri.getPath();
			if (path.equals("/shutdown")) {
				server.shutDown();
				return;
			}
			// �����ͷһ�η���ʱ���ᷢ����ôһ�����󣬺�����
			if (path.equals("/favicon.ico")) {
				return;
			}
			if( LinksPool.canCreateLink() ) {
				new Thread(new HandlerThread(httpExchange)).start();
				LinksPool.addLink();
			}
			else {
				throw new Exception( "�������������������ֵ" + LinksPool.getMaxLinks() + "�����Ժ��ٷ���" );
			}
		} catch (Throwable t) {
			try {
				String result;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					t.printStackTrace(new PrintStream(baos));
				} finally {
					baos.close();
				}
				result = "error:" + baos.toString();
				result = StringUtils.replace( result, "\n", "<br>" );
				Logger.severe(t);
				byte[] bytes = result.getBytes("UTF-8");
				httpExchange.getResponseHeaders().add( "Content-Type", "text/html;charset=UTF-8" );
				httpExchange.sendResponseHeaders( 500, bytes.length );
				OutputStream os = httpExchange.getResponseBody();
				os.write(bytes);
				os.close();
			}
			catch( Throwable th ){
				Logger.severe(th);
			}
			finally {
				try {
					httpExchange.close();
				}
				catch( Throwable th){}
			}
			//SplxServerInIDE dsi = (SplxServerInIDE)server;
			//dsi.shutDown();     //ΪʲôҪshutdown��
		}
		
	}

	private Object obj2String(Object obj) {
		if (obj == null)
			return "";
		if( obj instanceof String || obj instanceof byte[] )
			return obj;
		String result = "";
		if (obj instanceof Table) { // Table
			Table t = (Table) obj;
			result = Export.export(t, null, null, null, "t", new Context());
		} else if (obj instanceof Sequence) {// ���У�����
			result = ((Sequence) obj).toExportString();
		} else if (obj instanceof BaseRecord) {// ��¼
			result = ((BaseRecord) obj).toString("tq");
		} else {
			result = obj.toString();
		}
		return result;
	}

	class HandlerThread extends Job{
		HttpExchange httpExchange;
		
		public HandlerThread(HttpExchange httpExchange){
			this.httpExchange = httpExchange;
		}
		
		public void run() {
			Object result = "";
			String contentType = "text/html;charset=UTF-8";
			String headers = null;
			int status = 200;     //������Ӧ��
			Context ctx1 = null;
			Headers reqHeaders = null;
			try {
				URI uri = httpExchange.getRequestURI();
				String path = uri.getPath().trim();
				reqHeaders = httpExchange.getRequestHeaders();

				if (path.equals("/")) {
					String url = SplxServerInIDE.getInstance().getContext().getDefaultUrl();
					result = mm.getMessage("DfxHttpHandler.demo", url);
				} else {
					String ext = null;
					int extpos = path.lastIndexOf( "." );
					if( extpos > 0 ) {
						ext = path.substring( extpos + 1 );
						ext = ext.toLowerCase();
					}
					if( ext != null ) {
						if( "png".equals( ext ) || "jpg".equals( ext ) || "gif".equals( ext ) || "jpeg".equals( ext ) ) {
							contentType = "image";
						}
						else {
							ext = null;
						}
					}
					if( ext != null ) {   //����ͼƬ�ļ�
						String fileName = path.substring( 1 );
						FileObject fo = new FileObject(fileName, "sp");
						InputStream is = null;
						try {
							is = fo.getInputStream();
							result = getStreamBytes( is );
						}
						catch( Throwable th ) {
							status = 404;
						}
						finally {
							try{ is.close(); }catch( Throwable t ){}
						}
					}
					else {    //û����չ���ģ�������splx
						if( path.endsWith( ".splx" ) || path.endsWith( ".spl" ) || path.endsWith( ".dfx" ) ) {  //����Щ��βʱ���Զ��������()
							path += "()";
						}
						String fileName = "";
						String splx2 = "";
						String params = "";   //���в���ֵ�����ڼ�¼spl��ʽ��()��д�Ĳ���ֵ
						HashMap<String,String> paramsMap = new HashMap<String,String>();
						int pos = path.indexOf( "(" );
						if( pos > 0 ) { 
							path = path.trim();
							pos = path.lastIndexOf(")");
							if (pos < 0)
								throw new Exception(mm.getMessage(
										"DfxHttpHandler.erroruri", path));
							String path1 = path.substring(0, pos);
							if (pos < path.length() - 1)
								splx2 = path.substring(pos + 1).trim();
							int start = path1.indexOf("(");
							if (start < 0)
								throw new Exception(mm.getMessage(
										"DfxHttpHandler.erroruri", path1));
							params = path1.substring(start + 1).trim();
							fileName = path1.substring(1, start).trim();
						}
						else {
							//path��û��()��˵����restful��ʽ��url: http://..../sapPath/splx/argvalue1/argvalue2/...
							//����argvalue�ǲ���ֵ������˳����splx�еĲ�����Ӧ
							//�����ǳ���url��ʽ http://localhost:.../.../splx?arg1=v1&arg2=v2
							//û��splx2
							path = path.trim();
							String queryParams = uri.getQuery();
							if( queryParams == null || queryParams.trim().length() == 0 ) {   //restful
								ArrayList<String> saps = SplxServerInIDE.getInstance().getContext().getSapPath();
								String prefix = "";
								for( int k = 0; k < saps.size(); k++ ) {
									String sap = saps.get( k );
									if( !sap.startsWith( "/" ) ) sap = "/" + sap;
									if( path.startsWith( sap ) ) {
										if( sap.length() > prefix.length() ) prefix = sap;
									}
								}
								path = path.substring( prefix.length() );
								pos = path.indexOf( "/", 1 );
								String path1 = path;   //path1����splx�ļ�·��
								String args = "";
								if( pos > 0 ) {
									path1 = path.substring( 0, pos );
									args = path.substring( pos + 1 );
								} 
								path1 = prefix + path1;
								fileName = path1.substring(1).trim();
								if( args.length() > 0 ) {
									ArgumentTokenizer at = new ArgumentTokenizer( args, '/' );
									int k = 0;
									while( at.hasMoreTokens() ) {
										String tmp = at.nextToken().trim();
										if( k > 0 ) params += ",";
										params += tmp;
										k++;
									}
								}
							}
							else {   //����url��ʽ
								fileName = path.substring( 1 );
								String args = queryParams.trim();
								if( args.length() > 0 ) {
									SegmentSet segs = new SegmentSet( args, true, '&' );
									Iterator it = segs.keySet().iterator();
									while( it.hasNext() ) {
										String paramName = (String)it.next();
										String paramValue = segs.get( paramName );
										paramsMap.put( paramName, paramValue );
									}
								}
							}
						}
						PgmCellSet pcs1 = null;
						try {
							FileObject fo = new FileObject(fileName, "s");
							if( !fo.isExists() ) {
								String fn = fileName.toLowerCase();
								if( !fn.endsWith(".splx") && !fn.endsWith(".spl") && !fn.endsWith(".dfx") ) {
									fo = new FileObject(fileName + ".splx", "s");
									if( !fo.isExists() ) {
										fo = new FileObject(fileName + ".spl", "s");
									}
									if( !fo.isExists() ) {
										fo = new FileObject(fileName + ".dfx", "s");
									}
								}
							}
							if( !fo.isExists() ) {
								throw new Exception( "File " + fileName + " is not exist." );
							}
							pcs1 = fo.readPgmCellSet();
						}
						catch( Throwable th ) {
							status = 404;
							th.printStackTrace();
						}
						if( status == 200 ) {
							ParamList list = pcs1.getParamList();
							ArgumentTokenizer at = new ArgumentTokenizer(params, ',');
							boolean hasPost = false;
							ctx1 = Esprocx.prepareEnv();
							if( list != null ) {
								for( int i = 0; i < list.count(); i++ ) {
									Param p = (Param) list.get(i);
									Object value = p.getValue();
									String pName = p.getName();
									if( "argpost".equals( pName ) ) hasPost = true;
									//if( value == null ) continue;
									ctx1.setParamValue( pName, value );
								}
							}
							for (int i = 0; at.hasMoreTokens() && list != null; i++) {
								String pvalue = at.nextToken();
								//if (pvalue == null || pvalue.trim().length() == 0) continue;
								Param p = (Param) list.get(i);
								if( p == null ) continue;
								ctx1.setParamValue(p.getName(), Variant.parse(pvalue));
							}
							if( paramsMap.size() > 0 && list != null ) {
								Iterator<String> it = paramsMap.keySet().iterator();
								while( it.hasNext() ) {
									String paramName = it.next();
									String paramValue = paramsMap.get( paramName );
									ctx1.setParamValue( paramName, Variant.parse( paramValue ) );
								}
							}
							//��ȡpost����
							if( hasPost ) {
								InputStream reqis = null;
								try {
									reqis = httpExchange.getRequestBody();
									BufferedReader br = new BufferedReader( new InputStreamReader( reqis, "UTF-8" ) ); 
									StringBuffer sb = new StringBuffer();
									String line = null;
									while( ( line = br.readLine() ) != null ) {
										sb.append( line ).append( "\n" );
									}
									ctx1.setParamValue( "argpost", sb.toString() );
									//Logger.info( "����argpost����Ϊ��" + sb.toString() );
								}
								finally {
									try{ if( reqis != null ) reqis.close(); }catch(Throwable t){}
								}
							}
							//post end
							SplxServerInIDE server = SplxServerInIDE.instance;
							if (server != null) {
								RaqsoftConfig rc = server.getRaqsoftConfig();
								if (rc != null) {
									List<String> dbs = rc.getAutoConnectList();
									DatabaseUtil.connectAutoDBs(ctx1, dbs);
								}
							}
		
							try {
								pcs1.reset();
								pcs1.setContext(ctx1);
								pcs1.calculateResult();
							}
							finally {
								JobSpaceManager.closeSpace( ctx1.getJobSpace().getID() );
							}
							Object obj1 = pcs1.nextResult();
							if (splx2.length() == 0) { // ˵��ֻ��һ��splx
								result = obj2String(obj1);
							} else {
								if (!splx2.startsWith("/"))
									splx2 = "/" + splx2;
								PgmCellSet pcs2 = CellSetUtil
										.readPgmCellSet(HttpContext.dfxHome + splx2);
								ParamList list2 = pcs2.getParamList();
								Context ctx2 = new Context();
								ctx2.setParamValue(((Param) list2.get(0)).getName(), obj1);
								pcs2.reset();
								pcs2.setContext(ctx2);
								pcs2.calculateResult();
								Object obj2 = pcs2.nextResult();
								result = obj2String(obj2);
							}
							Object hs = pcs1.nextResult();   
							if( hs != null ) {
								String shs = (String)obj2String( hs );
								if( shs.toLowerCase().indexOf( "content-type" ) >= 0 ) {
									//��splx����һ��ͼƬʱ���õ�2������ֵָ��{content-type:image/xxx}
									headers = shs;
								}
							}
						}
					}
				}
			}catch (Throwable t) {
				try {
					status = 500;
					result = "error:";
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try {
						t.printStackTrace(new PrintStream(baos));
					} finally {
						try {
							baos.close();
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
					result = "error:" + baos.toString();
					result = StringUtils.replace( (String)result, "\n", "<br>" );
	//				t.printStackTrace();
					Logger.severe(t);
				}
				catch( Throwable th ) {
					Logger.severe(th);
				}
			} finally {
				try {
					DatabaseUtil.closeAutoDBs(ctx1);
				}catch( Throwable ex){}
			}
			
			try{
				String encoding = "UTF-8"; 
				byte[] bytes = null;
				if( result instanceof String ) {
					bytes = ((String)result).getBytes(encoding);
				}
				else {
					bytes = (byte[])result;
					String type = FileUtils.getFileFormat( bytes );
					if( FileUtils.FORMAT_GIF.equals( type )	|| FileUtils.FORMAT_JPG.equals( type ) || FileUtils.FORMAT_PNG.equals( type ) ) {
						contentType = "image";
					}
				}
				if( status == 404 ) {
					httpExchange.sendResponseHeaders( 404, -1 );
				}
				else {
					httpExchange.sendResponseHeaders( status, bytes.length );
				}
				Headers hs = httpExchange.getResponseHeaders();
				boolean hasContentType = false;
				if( headers != null && headers.trim().length() > 0 ) {
					JSONObject jo = new JSONObject( headers );
					Iterator<String> it = jo.keys();
					while( it.hasNext() ) {
						String key = it.next();
						if( key.toLowerCase().equals( "content-type" ) ) {
							hasContentType = true;
							String ct = jo.getString(key);
							int pos = ct.indexOf( "charset=" );
							if( pos >= 0 ) encoding = ct.substring( pos + 8 ).trim();
						}
						if( key.toLowerCase().equals( "status" ) ) {
							status = jo.getInt( key );
						}
						else hs.add( key, jo.getString( key ) );
					}
				}
				if( !hasContentType ) {
					hs.add( "Content-Type", contentType );
				}
				
				// ֧�ֿ���
				hs.add("Access-Control-Allow-Origin", "*");
				hs.add("Access-Control-Allow-Methods", "POST,GET,PUT,DELETE");
				hs.add("Access-Control-Allow-Headers", "*");
				hs.add("Access-Control-Allow-Credentials", "true");
				
				if( status != 404 ) {
					try {
						OutputStream os = httpExchange.getResponseBody();
						os.write(bytes);
						os.close();
					}
					catch( Throwable th ) {
						String msg = th.getMessage();
						if( msg == null || msg.indexOf( "not sent" ) < 0 ) {  //response headers not sent yet�����Ӱ��ʹ�ã����ñ���
							throw th;
						}
					}
				}
			}
			catch(Throwable x){
				Logger.severe( x );
			}
			finally {
				try {
					httpExchange.close();
				}
				catch( Throwable th){}
				LinksPool.removeLink();
			}
		}
		
		public byte[] getStreamBytes(InputStream is) throws Exception {
			ArrayList al = new ArrayList();
			int totalBytes = 0;
			byte[] b = new byte[102400];
			int readBytes = 0;
			while ((readBytes = is.read(b)) > 0) {
				byte[] bb = new byte[readBytes];
				System.arraycopy(b, 0, bb, 0, readBytes);
				al.add(bb);
				totalBytes += readBytes;
			}
			b = new byte[totalBytes];
			int pos = 0;
			for (int i = 0; i < al.size(); i++) {
				byte[] bb = (byte[]) al.get(i);
				System.arraycopy(bb, 0, b, pos, bb.length);
				pos += bb.length;
			}
			return b;
		}
		
	}

}

