package com.scudata.dm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.List;

import com.scudata.common.IOUtils;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dw.ComTable;
import com.scudata.resources.EngineMessage;

/**
 * �����ļ�
 * @author WangXiaoJun
 *
 */
public class LocalFile implements IFile {
	private String fileName;
	private String opt;
	private String parent; // ��·��
	private Context ctx;
	private Integer partition;
	
	public LocalFile(String fileName, String opt) {
		this.fileName = fileName;
		this.opt = opt;
	}

	/**
	 * ���������ļ�
	 * @param fileName ���·�������·��
	 * @param opt 
	 * @param ctx ����Ҫʱ��null
	 */
	public LocalFile(String fileName, String opt, Context ctx) {
		this.fileName = fileName;
		this.opt = opt;
		this.ctx = ctx;
	}

	/**
	 * ��Ⱥ����ʹ��
	 * @param fileName
	 * @param opt
	 * @param partition
	 */
	public LocalFile(String fileName, String opt, Integer partition) {
		this.fileName = fileName;
		this.opt = opt;
		
		if (partition != null && partition.intValue() >= 0) {
			this.partition = partition;
			//parent = Env.getMappingPath(partition);
			
			// �ҳ��ļ�������ʼλ��
			int index = fileName.lastIndexOf('\\');
			if (index == -1) {
				index = fileName.lastIndexOf('/');
			}
			
			if (index == -1) {
				this.fileName = partition.toString() + "." + fileName;
			} else {
				this.fileName = fileName.substring(0, index + 1) + 
						partition.toString() + "." + fileName.substring(index + 1);
			}
		}
	}

	/**
	 * ���ø��ļ���
	 * @param parent
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}
		
	/**
	 * �����ļ���
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	private boolean isSearchPath() {
		return opt != null && opt.indexOf('s') != -1;
	}

	private File getAppHome() {
		if (ctx != null) {
			JobSpace js = ctx.getJobSpace();
			if (js != null) return js.getAppHome();
		}
		
		return null;
	}
	
	/**
	 * �����ļ�����������·��
	 * @return File
	 */
	public File file() {
		if (parent != null) {
			return new File(parent, fileName);
		}
		
		// ���������appHome����fileNameֻ�������·��
		File appHome = getAppHome();
		if (appHome != null) {
			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(appHome, mainPath);
				return new File(tmpFile, fileName);
			} else {
				return new File(appHome, fileName);
			}
		}
		
		if (IOUtils.isAbsolutePath(fileName)) {
			return new File(fileName);
		}

		String mainPath = Env.getMainPath();
		if (mainPath != null && mainPath.length() > 0) {
			return new File(mainPath, fileName);
		} else {
			return new File(fileName);
		}
	}
	
	/**
	 * �����ļ�������ļ������ڷ���null
	 * @return
	 */
	public File getFile() {
		if (parent != null) {
			File file = new File(parent, fileName);
			if (file.exists()) {
				return file;
			}
		}
		
		// ���������appHome����fileNameֻ�������·��
		File appHome = getAppHome();
		if (appHome != null) {
			// ��ѡ��sʱ������·��������·���б��������Ŀ¼
			if (isSearchPath() && Env.getPaths() != null) {
				for (String path : Env.getPaths()) {
					File tmpFile = new File(appHome, path);
					tmpFile = new File(tmpFile, fileName);
					if (tmpFile.exists()) return tmpFile;
				}
			}

			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(appHome, mainPath);
				tmpFile = new File(tmpFile, fileName);
				if (tmpFile.exists()) return tmpFile;
			} else {
				File tmpFile = new File(appHome, fileName);
				if (tmpFile.exists()) return tmpFile;
			}

			return null;
		}
		
		if (IOUtils.isAbsolutePath(fileName)) {
			File file = new File(fileName);
			if (file.exists()) {
				return file;
			} else {
				return null;
			}
		}

		// ��ѡ��sʱ������·��������·���б��������Ŀ¼
		if (isSearchPath()) {
			String []paths = Env.getPaths();
			if (paths != null) {
				for (int i = 0, count = paths.length; i < count; ++i) {
					File tmpFile = new File(paths[i], fileName);
					if (tmpFile.exists()) return tmpFile;
				}
			}
		}

		String mainPath = Env.getMainPath();
		if (mainPath != null && mainPath.length() > 0) {
			File tmpFile = new File(mainPath, fileName);
			if (tmpFile.exists()) return tmpFile;
		}

		File file = new File(fileName);
		if (file.exists()) {
			return file;
		} else {
			return null;
		}
	}

	/**
	 * ȡ������
	 * @throws IOException
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		try {
			if (parent != null) {
				File file = new File(parent, fileName);
				if (file.exists()) return new FileInputStream(file);
			}

			// ���������appHome����fileNameֻ�������·��
			File appHome = getAppHome();
			if (appHome != null) {
				// ��ѡ��sʱ������·��������·���б��������Ŀ¼
				if (isSearchPath()) {
					InputStream in = IOUtils.findResource(fileName);
					if (in != null) return in;
					
					in = Env.getStreamFromApp(fileName);
					if (in != null) return in;

					String []paths = Env.getPaths();
					if (paths != null) {
						for (String path : paths) {
							File tmpFile = new File(appHome, path);
							tmpFile = new File(tmpFile, fileName);
							if (tmpFile.exists()) return new FileInputStream(tmpFile);
						}
					}
				}

				String mainPath = Env.getMainPath();
				if (mainPath != null && mainPath.length() > 0) {
					File tmpFile = new File(appHome, mainPath);
					tmpFile = new File(tmpFile, fileName);
					if (tmpFile.exists()) return new FileInputStream(tmpFile);
				} else {
					File tmpFile = new File(appHome, fileName);
					if (tmpFile.exists()) return new FileInputStream(tmpFile);
				}

				throw new FileNotFoundException(fileName);
			}

			if (IOUtils.isAbsolutePath(fileName)) {
				return new FileInputStream(fileName);
			}

			// ��ѡ��sʱ������·��������·���б��������Ŀ¼
			if (isSearchPath()) {
				InputStream in = IOUtils.findResource(fileName);
				if (in != null) return in;

				in = Env.getStreamFromApp(fileName);
				if (in != null) return in;
				
				String []paths = Env.getPaths();
				if (paths != null) {
					for (int i = 0, count = paths.length; i < count; ++i) {
						File tmpFile = new File(paths[i], fileName);
						if (tmpFile.exists()) return new FileInputStream(tmpFile);
					}
				}
			}

			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(mainPath, fileName);
				if (tmpFile.exists()) return new FileInputStream(tmpFile);
			}

			return new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * ȡ��������ļ��������򴴽�
	 * @param isAppend boolean �Ƿ�׷��
	 * @throws FileNotFoundException
	 * @return OutputStream
	 */
	public OutputStream getOutputStream(boolean isAppend) {
		try {
			File file = getFileForWrite();
			file.getParentFile().mkdirs();
			return new FileOutputStream(file, isAppend);
		} catch (FileNotFoundException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * ȡ�ܹ����д����������ļ��������򴴽�
	 * @param isAppend boolean �Ƿ�׷��
	 * @return RandomOutputStream
	 */
	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		try {
			File file = getFileForWrite();
			file.getParentFile().mkdirs();
			
			RandomAccessFile randomFile = new RandomAccessFile(file, "rw");
			if (!isAppend) {
				randomFile.setLength(0);
			} else {
				randomFile.seek(randomFile.length());
			}
		
			return new FileRandomOutputStream(randomFile);
		} catch (IOException e) {
			throw new RQException(e);
		}
		//return new FileRandomOutputStream(getFileOutputStream(isAppend));
	}

	private File getFileForWrite() {
		if (parent != null) {
			return new File(parent, fileName);
		}

		// ���������appHome����fileNameֻ�������·��
		File appHome = getAppHome();
		if (appHome != null) {
			String mainPath = Env.getMainPath();
			if (mainPath != null && mainPath.length() > 0) {
				File tmpFile = new File(appHome, mainPath);
				return new File(tmpFile, fileName);
			} else {
				return new File(appHome, fileName);
			}
		}

		if (IOUtils.isAbsolutePath(fileName)) {
			return new File(fileName);
		}

		String mainPath = Env.getMainPath();
		if (mainPath != null && mainPath.length() > 0) {
			return new File(mainPath, fileName);
		} else {
			return new File(fileName);
		}
	}
	
	/**
	 * �����ļ��Ƿ����
	 * @return boolean
	 */
	public boolean exists() {
		// resource�����Fileû���ҵ�
		//return getFile() != null;
		
		InputStream is = null;
		try {
			// ����ļ������ڻ����쳣
			is = getInputStream();
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * �����ļ���С
	 * @return long
	 */
	public long size() {
		File file = getFile();
		if (file != null) {
			return file.length();
		} else {
			return 0;
		}
	}

	/**
	 * ��������޸�ʱ��
	 * @return long
	 */
	public long lastModified() {
		File file = getFile();
		if (file != null) {
			return file.lastModified();
		} else {
			return 0;
		}
	}

	/**
	 * ɾ�������ص��ļ�
	 * @param file
	 * @return
	 */
	private boolean deleteCtxFiles(File file) {
		if (fileName.endsWith(".ctx")) {
			ComTable table = null;
			try {
				table = ComTable.open(file, ctx);
				List<File> files = table.getFiles(false, true);
				table.close();
				for (File f : files) {
					f.delete();
				}
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * ɾ���ļ��������Ƿ�ɹ�
	 * @return boolean
	 */
	public boolean delete() {
		File file = getFile();
		if (file != null) {
			deleteCtxFiles(file);
			return file.delete();
		} else {
			return false;
		}
	}

	/**
	 * ɾ��·��������������ļ�����·��
	 * @return
	 */
	public boolean deleteDir() {
		File file = getFile();
		if (file == null) {
			return false;
		}
		
		return deleteDir(file);
	}
	
	private static boolean deleteDir(File file) {
		File []subs = file.listFiles();
		if (subs != null) {
			for (File sub : subs) {
				deleteDir(sub);
			}
		}
		
		return file.delete();
	}
	
	/**
	 * �ƶ������ص������ļ�
	 * @param file
	 * @param destFile
	 * @param isCopy
	 * @param auto �Զ����������ļ�
	 */
	private void moveCtxFiles(File file, File destFile, boolean isCopy, boolean auto) {
		if (file.isDirectory()) {
			return;
		}
		
		if (fileName.endsWith(".ctx")) {
			try {
				ComTable table = ComTable.open(file, ctx);
				List<File> files = table.getFiles(false, auto);
				table.close();
				
				int fcount = files.size();
				if (fcount == 0) return;
				File[] destFiles = new File[fcount];
				String dest = destFile.getAbsolutePath();
				int pos = file.getAbsolutePath().length();
				
				for (int i = 0; i < fcount; i++) {
					String name = files.get(i).getAbsolutePath().substring(pos);
					destFiles[i] = new File(dest + name);
				}
				
				for (int i = 0; i < fcount; i++) {
					if (isCopy) {
						copyFile(files.get(i), destFiles[i]);
					} else {
						destFiles[i].delete();
						files.get(i).renameTo(destFiles[i]);
					}
				}
			} catch (IOException e) {
				throw new RQException(e);
			}
		}
	}
	
	/**
	 * �ƶ��ļ���path��pathֻ���ļ��������
	 * @param dest String Ŀ���ļ������ļ�·����
	 * @param opt String y��Ŀ���ļ��Ѵ���ʱǿ�и���ȱʡ��ʧ�ܣ�c�����ƣ�
	 * 					 p��Ŀ���ļ������Ŀ¼���������Ŀ¼��Ĭ���������Դ�ļ��ĸ�Ŀ¼
	 * @return boolean true���ɹ���false��ʧ��
	 */
	public boolean move(String dest, String opt) {
		File file = getFile();
		if (file == null || !file.exists()) return false;

		boolean isCover = false, isCopy = false, isMain = false, auto = false;
		if (opt != null) {
			if (opt.indexOf('y') != -1) isCover = true;
			if (opt.indexOf('c') != -1) isCopy = true;
			if (opt.indexOf('p') != -1) isMain = true;
			if (opt.indexOf('a') != -1) auto = true;
		}

		File destFile = new File(dest);
		boolean isDir = destFile.isDirectory();
		if (!isDir && !destFile.isFile() && dest.length() > 1) {
			// �ļ�������ʱ�޷��ж����ļ������ļ��У���ʱ��·�����Ƿ��зָ����ж�
			char c = dest.charAt(dest.length() - 1);
			isDir = c == '/' || c == '\\';
		}
		
		if (!isDir && partition != null && partition.intValue() >= 0) {
			// �ҳ��ļ�������ʼλ��
			int index = dest.lastIndexOf('\\');
			if (index == -1) {
				index = dest.lastIndexOf('/');
			}
			
			if (index == -1) {
				dest = partition.toString() + "." + dest;
			} else {
				dest = dest.substring(0, index + 1) + 
						partition.toString() + "." + dest.substring(index + 1);
			}
			
			destFile = new File(dest);
		}
		
		if (!destFile.isAbsolute()) {
			if (isMain) {
				File appHome = getAppHome();
				String mainPath = Env.getMainPath();
				if (appHome != null) {
					if (mainPath != null && mainPath.length() > 0) {
						destFile = new File(appHome, mainPath);
						destFile = new File(destFile, dest);
					} else {
						destFile = new File(appHome, dest);
					}
				} else if (mainPath != null && mainPath.length() > 0) {
					destFile = new File(mainPath, dest);
				}
			} else {
				destFile = new File(file.getParentFile(), dest);
			}
		} else if (getAppHome() != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("file.fileNotExist", dest));
		}

		// ��������ļ������Զ���Դ�ļ���
		if (isDir && !file.isDirectory()) {
			destFile = new File(destFile, file.getName());
		}
		
		if (!isCover && destFile.exists()) {
			return false;
		}

		File parent = destFile.getParentFile();
		if (parent != null) parent.mkdirs();

		moveCtxFiles(file, destFile, isCopy, auto);
		
		if (isCopy) {
			if (file.isDirectory()) {
				return copyDirectory(file, destFile);
			} else {
				return copyFile(file, destFile);
			}
		} else {
			destFile.delete();
			return file.renameTo(destFile);
		}
	}

	/**
	 * ������ʱ�ļ�
	 * @param prefix String
	 * @return String ���ؾ���·���ļ���
	 */
	public String createTempFile(String prefix) {
		try {
			File file = getFile();
			if (file == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileNotExist", fileName));
			}
			
			if (file.isDirectory()) {
				File tmpFile = File.createTempFile(prefix, "", file);
				return tmpFile.getAbsolutePath();
			} else {
				String suffix = "";
				String name = file.getName();
				int index = name.lastIndexOf('.');
				if (index != -1) suffix = name.substring(index);

				file = file.getParentFile();
				File tmpFile = File.createTempFile(prefix, suffix, file);
				return tmpFile.getAbsolutePath();
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * �Ѹ�����·����ɾ��ǰ�����Ŀ¼�������������Ŀ¼��·��
	 * @param pathName �ļ�·����
	 * @param ctx ����������
	 * @return ��ȡ������·����
	 */
	public static String removeMainPath(String pathName, Context ctx) {
		File home = null;
		JobSpace js = ctx.getJobSpace();
		if (js != null) {
			home = js.getAppHome();
		}
		
		
		String main = Env.getMainPath();
		if (main != null && main.length() > 0) {
			if (home == null) {
				home = new File(main);
			} else {
				home = new File(home, main);
			}
		} else if (home == null) {
			return pathName;
		}
		
		String strHome = home.getAbsolutePath();
		int len = strHome.length();
		if (pathName.length() > len && pathName.substring(0, len).equalsIgnoreCase(strHome)) {
			// ȥ��ǰ���б�ܻ�б��
			char c = pathName.charAt(len);
			if (c == '\\' || c == '/') {
				len++;
				if (pathName.length() > len) {
					c = pathName.charAt(len);
					if (c == '\\' || c == '/') {
						len++;
					}
				}
			}
			
			return pathName.substring(len);
		} else {
			return pathName;
		}
	}
	
	/**
	 * �����ļ���ָ���ļ�����
	 * @param s Դ�ļ�
	 * @param t Ŀ���ļ���
	 * @return
	 */
	public static boolean copyDirectory(File s, File t) {
		File destDir = new File(t, s.getName());
		destDir.mkdirs();
		
		boolean result = true;
		File[] files = s.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					if (!copyFile(file, new File(destDir, file.getName()))) {
						result = false;
					}
				} else if (file.isDirectory()) {
					if (!copyDirectory(file, destDir)) {
						result = false;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * �����ļ������ݵ�ָ���ļ�
	 * @param s Դ�ļ�
	 * @param t Ŀ���ļ�
	 * @return true���ɹ�
	 */
	public static boolean copyFile(File s, File t) {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		try {
			fis = new FileInputStream(s);
			fos = new FileOutputStream(t);
			FileChannel in = fis.getChannel();
			FileChannel out = fos.getChannel();
			in.transferTo(0, in.size(), out); // ��������ͨ�������Ҵ�inͨ����ȡ��Ȼ��д��outͨ��
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			IOException ie = null;
			try {
				fis.close();
			} catch (IOException e) {
				ie = e;
			}
			try {
				fos.close();
			} catch (IOException e) {
				ie = e;
			}
			
			if (ie != null) {
				throw new RQException(ie);
			}
		}
		
		return true;
	}
	
	/**
	 * �����ļ���С
	 * @param size ��С
	 */
	public void setFileSize(long size) {
		File file = getFile();
		if (file != null) {
			try {
				RandomAccessFile rf = new RandomAccessFile(file, "rw");
				rf.setLength(size);
				rf.close();
			} catch (IOException e) {
				throw new RQException(e);
			}
		}
	}
	
	/**
	 * ȡ����
	 * @return Integer ���û�����÷����򷵻ؿ�
	 */
	public Integer getPartition() {
		return partition;
	}
	
	/**
	 * ȡ��������ļ����������֧���򷵻�null
	 * @return RandomAccessFile
	 */
	public RandomAccessFile getRandomAccessFile() {
		File file = file();
		try {
			if (file.canWrite()) {
				return new RandomAccessFile(file, "rw");
			} else {
				return new RandomAccessFile(file, "r");
			}
		} catch (FileNotFoundException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * �����Ƿ������ļ�
	 * @return true�������ļ���false���������ļ�
	 */
	public boolean isCloudFile() {
		return false;
	}
}
