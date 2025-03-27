package com.scudata.array;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.Variant;

public class StringArray implements IArray {
	private static final long serialVersionUID = 1L;
	
	private static String TEMP = new String("");
	
	private String []datas;
	private int size;
	
	public StringArray() {
		datas = new String[DEFAULT_LEN];
	}
	
	public StringArray(int initialCapacity) {
		datas = new String[++initialCapacity];
	}
	
	public StringArray(String []datas, int size) {
		this.datas = datas;
		this.size = size;
	}

	public static int compare(String d1, String d2) {
		if (d1 == null) {
			return d2 == null ? 0 : -1;
		} else if (d2 == null) {
			return 1;
		} else {
			int cmp = d1.compareTo(d2);
			return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
		}
	}
	
	private static int compare(String d1, Object d2) {
		if (d2 instanceof String) {
			if (d1 == null) {
				return -1;
			} else {
				int cmp = d1.compareTo((String)d2);
				return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
			}
		} else if (d2 == null) {
			return d1 == null ? 0 : 1;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", d1, d2,
					mm.getMessage("DataType.String"), Variant.getDataType(d2)));
		}
	}
	
	public String[] getDatas() {
		return datas;
	}

	/**
	 * ȡ��������ʹ������ڴ�����Ϣ��ʾ
	 * @return ���ʹ�
	 */
	public String getDataType() {
		MessageManager mm = EngineMessage.get();
		return mm.getMessage("DataType.String");
	}
	
	/**
	 * ��������
	 * @return
	 */
	public IArray dup() {
		int len = size + 1;
		String []newDatas = new String[len];
		System.arraycopy(datas, 0, newDatas, 0, len);
		return new StringArray(newDatas, size);
	}
	
	/**
	 * д���ݵ���
	 * @param out �����
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		int size = this.size;
		String []datas = this.datas;
		
		out.writeByte(1);
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeObject(datas[i]);
		}
	}
	
	/**
	 * �����ж�����
	 * @param in ������
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte();
		size = in.readInt();
		int len = size + 1;
		String []datas = this.datas = new String[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = (String)in.readObject();
		}
	}
	
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		int size = this.size;
		String []datas = this.datas;
		
		out.writeByte(1);
		out.writeInt(size);
		for (int i = 1; i <= size; ++i) {
			out.writeString(datas[i]);
		}

		return out.toByteArray();
	}
	
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		in.readByte();
		size = in.readInt();
		int len = size + 1;
		String []datas = this.datas = new String[len];
		
		for (int i = 1; i < len; ++i) {
			datas[i] = in.readString();
		}
	}
	
	/**
	 * ����һ��ͬ���͵�����
	 * @param count
	 * @return
	 */
	public IArray newInstance(int count) {
		return new StringArray(count);
	}

	/**
	 * ׷��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param o Ԫ��ֵ
	 */
	public void add(Object o) {
		if (o instanceof String) {
			ensureCapacity(size + 1);
			datas[++size] = (String)o;
		} else if (o == null) {
			ensureCapacity(size + 1);
			datas[++size] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * ׷��һ��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param array Ԫ������
	 */
	public void addAll(IArray array) {
		int size2 = array.size();
		if (size2 == 0) {
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + size2);
			
			System.arraycopy(stringArray.datas, 1, datas, size + 1, size2);
			size += size2;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				ensureCapacity(size + size2);
				String v = (String)obj;
				String []datas = this.datas;
				
				for (int i = 0; i < size2; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + size2);
				String []datas = this.datas;
				
				for (int i = 0; i < size2; ++i) {
					datas[++size] = null;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), array.getDataType()));
			}
		} else {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("pdm.arrayTypeError", 
			//		mm.getMessage("DataType.String"), array.getDataType()));
			ensureCapacity(size + size2);
			String []datas = this.datas;
			
			for (int i = 1; i <= size2; ++i) {
				Object obj = array.get(i);
				if (obj instanceof String) {
					datas[++size] = (String)obj;
				} else if (obj == null) {
					datas[++size] = null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.String"), Variant.getDataType(obj)));
				}
			}
		}
	}
	
	/**
	 * ׷��һ��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param array Ԫ������
	 * @param count Ԫ�ظ���
	 */
	public void addAll(IArray array, int count) {
		if (count == 0) {
		} else if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(stringArray.datas, 1, datas, size + 1, count);
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				ensureCapacity(size + count);
				String v = (String)obj;
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + count);
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = null;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), array.getDataType()));
			}
		} else {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("pdm.arrayTypeError", 
			//		mm.getMessage("DataType.String"), array.getDataType()));
			ensureCapacity(size + count);
			String []datas = this.datas;
			
			for (int i = 1; i <= count; ++i) {
				Object obj = array.get(i);
				if (obj instanceof String) {
					datas[++size] = (String)obj;
				} else if (obj == null) {
					datas[++size] = null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("pdm.arrayTypeError", 
							mm.getMessage("DataType.String"), Variant.getDataType(obj)));
				}
			}
		}
	}
	
	/**
	 * ׷��һ��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param array Ԫ������
	 * @param index Ҫ��������ݵ���ʼλ��
	 * @param count ����
	 */
	public void addAll(IArray array, int index, int count) {
		if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + count);
			
			System.arraycopy(stringArray.datas, index, datas, size + 1, count);
			size += count;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				ensureCapacity(size + count);
				String v = (String)obj;
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = v;
				}
			} else if (obj == null) {
				ensureCapacity(size + count);
				String []datas = this.datas;
				
				for (int i = 0; i < count; ++i) {
					datas[++size] = null;
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), array.getDataType()));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), array.getDataType()));
		}
	}
	
	/**
	 * ׷��һ��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param array Ԫ������
	 */
	public void addAll(Object []array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), Variant.getDataType(obj)));
			}
		}
		
		int size2 = array.length;
		ensureCapacity(size + size2);
		String []datas = this.datas;
		
		for (int i = 0; i < size2; ++i) {
			datas[++size] = (String)array[i];
		}
	}

	/**
	 * ����Ԫ�أ�������Ͳ��������׳��쳣
	 * @param index ����λ�ã���1��ʼ����
	 * @param o Ԫ��ֵ
	 */
	public void insert(int index, Object o) {
		if (o instanceof String) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			datas[index] = (String)o;
		} else if (o == null) {
			ensureCapacity(size + 1);
			
			size++;
			System.arraycopy(datas, index, datas, index + 1, size - index);
			datas[index] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(o)));
		}
	}

	/**
	 * ��ָ��λ�ò���һ��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param pos λ�ã���1��ʼ����
	 * @param array Ԫ������
	 */
	public void insertAll(int pos, IArray array) {
		if (array instanceof StringArray) {
			int numNew = array.size();
			StringArray stringArray = (StringArray)array;
			ensureCapacity(size + numNew);
			
			System.arraycopy(datas, pos, datas, pos + numNew, size - pos + 1);
			System.arraycopy(stringArray.datas, 1, datas, pos, numNew);
			
			size += numNew;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), array.getDataType()));
		}
	}
	
	/**
	 * ��ָ��λ�ò���һ��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param pos λ�ã���1��ʼ����
	 * @param array Ԫ������
	 */
	public void insertAll(int pos, Object []array) {
		for (Object obj : array) {
			if (obj != null && !(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						mm.getMessage("DataType.String"), Variant.getDataType(obj)));
			}
		}
		
		int numNew = array.length;
		ensureCapacity(size + numNew);
		
		System.arraycopy(datas, pos, datas, pos + numNew, size - pos + 1);
		System.arraycopy(array, 0, datas, pos, numNew);
		size += numNew;
	}

	public void push(String str) {
		datas[++size] = str;
	}

	public void pushString(String str) {
		datas[++size] = str;
	}

	/**
	 * ׷��Ԫ�أ��������������Ϊ���㹻�ռ���Ԫ�أ���������Ͳ��������׳��쳣
	 * @param o Ԫ��ֵ
	 */
	public void push(Object o) {
		if (o instanceof String) {
			datas[++size] = (String)o;
		} else if (o == null) {
			datas[++size] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(o)));
		}
	}
	
	/**
	 * ׷��һ���ճ�Ա���������������Ϊ���㹻�ռ���Ԫ�أ�
	 */
	public void pushNull() {
		datas[++size] = null;
	}
	
	/**
	 * ��array�еĵ�index��Ԫ����ӵ���ǰ�����У�������Ͳ��������׳��쳣
	 * @param array ����
	 * @param index Ԫ����������1��ʼ����
	 */
	public void push(IArray array, int index) {
		push(array.get(index));
	}
	
	/**
	 * ��array�еĵ�index��Ԫ����ӵ���ǰ�����У�������Ͳ��������׳��쳣
	 * @param array ����
	 * @param index Ԫ����������1��ʼ����
	 */
	public void add(IArray array, int index) {
		add(array.get(index));
	}
	
	/**
	 * ��array�еĵ�index��Ԫ���������ǰ�����ָ��Ԫ�أ�������Ͳ��������׳��쳣
	 * @param curIndex ��ǰ�����Ԫ����������1��ʼ����
	 * @param array ����
	 * @param index Ԫ����������1��ʼ����
	 */
	public void set(int curIndex, IArray array, int index) {
		set(curIndex, array.get(index));
	}

	/**
	 * ȡָ��λ��Ԫ��
	 * @param index ��������1��ʼ����
	 * @return
	 */
	public Object get(int index) {
		return datas[index];
	}
	
	public String getString(int index) {
		return datas[index];
	}

	/**
	 * ȡָ��λ��Ԫ�ص�����ֵ
	 * @param index ��������1��ʼ����
	 * @return ������ֵ
	 */
	public int getInt(int index) {
		throw new RuntimeException();
	}

	/**
	 * ȡָ��λ��Ԫ�صĳ�����ֵ
	 * @param index ��������1��ʼ����
	 * @return ������ֵ
	 */
	public long getLong(int index) {
		throw new RuntimeException();
	}
	
	/**
	 * ȡָ��λ��Ԫ�����������
	 * @param indexArray λ������
	 * @return IArray
	 */
	public IArray get(int []indexArray) {
		String []datas = this.datas;
		int len = indexArray.length;
		StringArray result = new StringArray(len);
		
		for (int i : indexArray) {
			result.pushString(datas[i]);
		}
		
		return result;
	}
	
	/**
	 * ȡָ��λ��Ԫ�����������
	 * @param indexArray λ������
	 * @param start ��ʼλ�ã�����
	 * @param end ����λ�ã�����
	 * @param doCheck true��λ�ÿ��ܰ���0��0��λ����null��䣬false���������0
	 * @return IArray
	 */
	public IArray get(int []indexArray, int start, int end, boolean doCheck) {
		String []datas = this.datas;
		int len = end - start + 1;
		String []resultDatas = new String[len + 1];
		
		if (doCheck) {
			for (int i = 1; start <= end; ++start, ++i) {
				int q = indexArray[start];
				if (q > 0) {
					resultDatas[i] = datas[q];
				}
			}
		} else {
			for (int i = 1; start <= end; ++start) {
				resultDatas[i++] = datas[indexArray[start]];
			}
		}
		
		return new StringArray(resultDatas, len);
	}
	
	/**
	 * ȡָ��λ��Ԫ�����������
	 * @param IArray λ������
	 * @return IArray
	 */
	public IArray get(IArray indexArray) {
		String []datas = this.datas;
		int len = indexArray.size();
		StringArray result = new StringArray(len);
		
		for (int i = 1; i <= len; ++i) {
			if (indexArray.isNull(i)) {
				result.pushNull();
			} else {
				result.pushString(datas[indexArray.getInt(i)]);
			}
		}
		
		return result;
	}
	
	/**
	 * ȡĳһ�������������
	 * @param start ��ʼλ�ã�������
	 * @param end ����λ�ã���������
	 * @return IArray
	 */
	public IArray get(int start, int end) {
		int newSize = end - start;
		String []newDatas = new String[newSize + 1];
		System.arraycopy(datas, start, newDatas, 1, newSize);
		return new StringArray(newDatas, newSize);
	}

	/**
	 * ʹ�б��������С��minCapacity
	 * @param minCapacity ��С����
	 */
	public void ensureCapacity(int minCapacity) {
		int oldCapacity = datas.length;
		if (oldCapacity <= minCapacity) {
			int newCapacity;
			if (minCapacity < 8) {
				newCapacity = 8;
			} else {
				newCapacity = oldCapacity + (oldCapacity >> 1);
				if (newCapacity < 0) {
					// ����Integer����
					newCapacity = oldCapacity + 0xfffffff;
					if (newCapacity < 0) {
						newCapacity = Integer.MAX_VALUE;
					}
				} else if (newCapacity <= minCapacity) {
					newCapacity = minCapacity + 1;
				}
			}

			String []newDatas = new String[newCapacity];
			System.arraycopy(datas, 0, newDatas, 0, size + 1);
			datas = newDatas;
		}
	}
	
	/**
	 * ����������ʹ����Ԫ�������
	 */
	public void trimToSize() {
		int newLen = size + 1;
		if (newLen < datas.length) {
			String []newDatas = new String[newLen];
			System.arraycopy(datas, 0, newDatas, 0, newLen);
			datas = newDatas;
		}
	}

	/**
	 * �ж�ָ��λ�õ�Ԫ���Ƿ��ǿ�
	 * @param index ��������1��ʼ����
	 * @return
	 */
	public boolean isNull(int index) {
		return datas[index] == null;
	}
	
	/**
	 * �ж�Ԫ���Ƿ���True
	 * @return BoolArray
	 */
	public BoolArray isTrue() {
		int size = this.size;
		String []datas = this.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = datas[i] != null;
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * �ж�Ԫ���Ƿ��Ǽ�
	 * @return BoolArray
	 */
	public BoolArray isFalse() {
		int size = this.size;
		String []datas = this.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			resultDatas[i] = datas[i] == null;
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * �ж�ָ��λ�õ�Ԫ���Ƿ���True
	 * @param index ��������1��ʼ����
	 * @return
	 */
	public boolean isTrue(int index) {
		// �ǿ�����true
		return datas[index] != null;
	}
	
	/**
	 * �ж�ָ��λ�õ�Ԫ���Ƿ���False
	 * @param index ��������1��ʼ����
	 * @return
	 */
	public boolean isFalse(int index) {
		// ������false
		return datas[index] == null;
	}

	/**
	 * �Ƿ��Ǽ����������ʱ���������飬��ʱ�����Ŀ��Ա��޸ģ����� f1+f2+f3��ֻ�����һ�������Ž��
	 * @return true������ʱ���������飬false��������ʱ����������
	 */
	public boolean isTemporary() {
		return datas[0] != null;
	}

	/**
	 * �����Ƿ��Ǽ����������ʱ����������
	 * @param ifTemporary true������ʱ���������飬false��������ʱ����������
	 */
	public void setTemporary(boolean ifTemporary) {
		datas[0] = ifTemporary ? TEMP : null;
	}
	
	/**
	 * ɾ�����һ��Ԫ��
	 */
	public void removeLast() {
		datas[size--] = null;
	}

	/**
	 * ɾ��ָ��λ�õ�Ԫ��
	 * @param index ��������1��ʼ����
	 */
	public void remove(int index) {
		System.arraycopy(datas, index + 1, datas, index, size - index);
		datas[size--] = null;
	}
	
	/**
	 * ɾ��ָ�������ڵ�Ԫ��
	 * @param from ��ʼλ�ã�����
	 * @param to ����λ�ã�����
	 */
	public void removeRange(int fromIndex, int toIndex) {
		System.arraycopy(datas, toIndex + 1, datas, fromIndex, size - toIndex);
		
		int newSize = size - (toIndex - fromIndex + 1);
		while (size != newSize) {
			datas[size--] = null;
		}
	}
	
	/**
	 * ɾ��ָ��λ�õ�Ԫ�أ���Ŵ�С��������
	 * @param seqs ��������
	 */
	public void remove(int []seqs) {
		int delCount = 0;
		String []datas = this.datas;
		
		for (int i = 0, len = seqs.length; i < len; ) {
			int cur = seqs[i];
			i++;

			int moveCount;
			if (i < len) {
				moveCount = seqs[i] - cur - 1;
			} else {
				moveCount = size - cur;
			}

			if (moveCount > 0) {
				System.arraycopy(datas, cur + 1, datas, cur - delCount, moveCount);
			}
			
			delCount++;
		}

		for (int i = 0, q = size; i < delCount; ++i) {
			datas[q - i] = null;
		}
		
		size -= delCount;
	}
	
	/**
	 * ����ָ�������ڵ�����
	 * @param start ��ʼλ�ã�������
	 * @param end ����λ�ã�������
	 */
	public void reserve(int start, int end) {
		int newSize = end - start + 1;
		System.arraycopy(datas, start, datas, 1, newSize);
		
		for (int i = size; i > newSize; --i) {
			datas[i] = null;
		}
		
		size = newSize;
	}

	public int size() {
		return size;
	}
	
	/**
	 * ��������ķǿ�Ԫ����Ŀ
	 * @return �ǿ�Ԫ����Ŀ
	 */
	public int count() {
		String []datas = this.datas;
		int size = this.size;
		int count = size;
		
		for (int i = 1; i <= size; ++i) {
			if (datas[i] == null) {
				count--;
			}
		}
		
		return count;
	}
	
	/**
	 * �ж������Ƿ���ȡֵΪtrue��Ԫ��
	 * @return true���У�false��û��
	 */
	public boolean containTrue() {
		int size = this.size;
		if (size == 0) {
			return false;
		}
		
		String []datas = this.datas;
		for (int i = 1; i <= size; ++i) {
			if (datas[i] != null) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * ���ص�һ����Ϊ�յ�Ԫ��
	 * @return Object
	 */
	public Object ifn() {
		int size = this.size;
		String []datas = this.datas;
		
		for (int i = 1; i <= size; ++i) {
			if (datas[i] != null) {
				return datas[i];
			}
		}
		
		return null;
	}

	/**
	 * �޸�����ָ��Ԫ�ص�ֵ��������Ͳ��������׳��쳣
	 * @param index ��������1��ʼ����
	 * @param obj ֵ
	 */
	public void set(int index, Object obj) {
		if (obj instanceof String) {
			datas[index] = (String)obj;
		} else if (obj == null) {
			datas[index] = null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("pdm.arrayTypeError", 
					mm.getMessage("DataType.String"), Variant.getDataType(obj)));
		}
	}
	
	/**
	 * ɾ�����е�Ԫ��
	 */
	public void clear() {
		Object []datas = this.datas;
		int size = this.size;
		this.size = 0;
		
		while (size > 0) {
			datas[size--] = null;
		}
	}

	/**
	 * ���ַ�����ָ��Ԫ��
	 * @param elem
	 * @return int Ԫ�ص�����,��������ڷ��ظ��Ĳ���λ��.
	 */
	public int binarySearch(Object elem) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int low = 1, high = size;
			
			while (low <= high) {
				int mid = (low + high) >> 1;
				int cmp = compare(datas[mid], v);
				if (cmp < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					return mid; // key found
				}
			}

			return -low; // key not found
		} else if (elem == null) {
			if (size > 0 && datas[1] == null) {
				return 1;
			} else {
				return -1;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", datas[1], elem,
					getDataType(), Variant.getDataType(elem)));
		}
	}
	
	// ���鰴�������򣬽��н�����ֲ���
	private int descBinarySearch(String elem) {
		String []datas = this.datas;
		int low = 1, high = size;
		
		while (low <= high) {
			int mid = (low + high) >> 1;
			int cmp = compare(datas[mid], elem);
			if (cmp < 0) {
				high = mid - 1;
			} else if (cmp > 0) {
				low = mid + 1;
			} else {
				return mid; // key found
			}
		}

		return -low; // key not found
	}
	
	/**
	 * ���ַ�����ָ��Ԫ��
	 * @param elem
	 * @param start ��ʼ����λ�ã�������
	 * @param end ��������λ�ã�������
	 * @return Ԫ�ص�����,��������ڷ��ظ��Ĳ���λ��.
	 */
	public int binarySearch(Object elem, int start, int end) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int low = start, high = end;
			
			while (low <= high) {
				int mid = (low + high) >> 1;
				int cmp = compare(datas[mid], v);
				if (cmp < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					return mid; // key found
				}
			}

			return -low; // key not found
		} else if (elem == null) {
			if (end > 0 && datas[start] == null) {
				return start;
			} else {
				return -1;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", datas[1], elem,
					getDataType(), Variant.getDataType(elem)));
		}
	}
	
	/**
	 * �����б����Ƿ����ָ��Ԫ��
	 * @param elem Object �����ҵ�Ԫ��
	 * @return boolean true��������false��������
	 */
	public boolean contains(Object elem) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int size = this.size;
			
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null && datas[i].equals(v)) {
					return true;
				}
			}
			
			return false;
		} else if (elem == null) {
			int size = this.size;
			String []datas = this.datas;
			for (int i = 1; i <= size; ++i) {
				if (datas[i] == null) {
					return true;
				}
			}
			
			return false;
		} else {
			return false;
		}
	}
	
	/**
	 * �ж������Ԫ���Ƿ��ڵ�ǰ������
	 * @param isSorted ��ǰ�����Ƿ�����
	 * @param array ����
	 * @param result ���ڴ�Ž����ֻ��ȡֵΪtrue��
	 */
	public void contains(boolean isSorted, IArray array, BoolArray result) {
		int resultSize = result.size();
		if (isSorted) {
			for (int i = 1; i <= resultSize; ++i) {
				if (result.isTrue(i) && binarySearch(array.get(i)) < 1) {
					result.set(i, false);
				}
			}
		} else {
			for (int i = 1; i <= resultSize; ++i) {
				if (result.isTrue(i) && !contains(array.get(i))) {
					result.set(i, false);
				}
			}
		}
	}
	
	/**
	 * �����б����Ƿ����ָ��Ԫ�أ�ʹ�õȺűȽ�
	 * @param elem
	 * @return boolean true��������false��������
	 */
	public boolean objectContains(Object elem) {
		Object []datas = this.datas;
		for (int i = 1, size = this.size; i <= size; ++i) {
			if (datas[i] == elem) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * ����Ԫ�����������״γ��ֵ�λ��
	 * @param elem �����ҵ�Ԫ��
	 * @param start ��ʼ����λ�ã�������
	 * @return ���Ԫ�ش����򷵻�ֵ����0�����򷵻�0
	 */
	public int firstIndexOf(Object elem, int start) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			int size = this.size;
			
			for (int i = start; i <= size; ++i) {
				if (datas[i] != null && datas[i].equals(v)) {
					return i;
				}
			}
			
			return 0;
		} else if (elem == null) {
			int size = this.size;
			String []datas = this.datas;
			for (int i = start; i <= size; ++i) {
				if (datas[i] == null) {
					return i;
				}
			}
			
			return 0;
		} else {
			return 0;
		}
	}
	
	/**
	 * ����Ԫ���������������ֵ�λ��
	 * @param elem �����ҵ�Ԫ��
	 * @param start �Ӻ��濪ʼ���ҵ�λ�ã�������
	 * @return ���Ԫ�ش����򷵻�ֵ����0�����򷵻�0
	 */
	public int lastIndexOf(Object elem, int start) {
		if (elem instanceof String) {
			String v = (String)elem;
			String []datas = this.datas;
			
			for (int i = start; i > 0; --i) {
				if (datas[i] != null && datas[i].equals(v)) {
					return i;
				}
			}
			
			return 0;
		} else if (elem == null) {
			String []datas = this.datas;
			for (int i = start; i > 0; --i) {
				if (datas[i] == null) {
					return i;
				}
			}
			
			return 0;
		} else {
			return 0;
		}
	}
	
	/**
	 * ����Ԫ�������������г��ֵ�λ��
	 * @param elem �����ҵ�Ԫ��
	 * @param start ��ʼ����λ�ã�������
	 * @param isSorted ��ǰ�����Ƿ�����
	 * @param isFromHead true����ͷ��ʼ������false����β��ǰ��ʼ����
	 * @return IntArray
	 */
	public IntArray indexOfAll(Object elem, int start, boolean isSorted, boolean isFromHead) {
		int size = this.size;
		String []datas = this.datas;
		
		if (elem == null) {
			IntArray result = new IntArray(7);
			if (isSorted) {
				if (isFromHead) {
					for (int i = start; i <= size; ++i) {
						if (datas[i] == null) {
							result.addInt(i);
						} else {
							break;
						}
					}
				} else {
					for (int i = start; i > 0; --i) {
						if (datas[i] == null) {
							result.addInt(i);
						}
					}
				}
			} else {
				if (isFromHead) {
					for (int i = start; i <= size; ++i) {
						if (datas[i] == null) {
							result.addInt(i);
						}
					}
				} else {
					for (int i = start; i > 0; --i) {
						if (datas[i] == null) {
							result.addInt(i);
						}
					}
				}
			}
			
			return result;
		} else if (!(elem instanceof String)) {
			return new IntArray(1);
		}

		String str = (String)elem;
		if (isSorted) {
			int end = size;
			if (!isFromHead) {
				end = start;
				start = 1;
			}
			
			int index = binarySearch(str, start, end);
			if (index < 1) {
				return new IntArray(1);
			}
			
			// �ҵ���һ��
			int first = index;
			while (first > start && compare(datas[first - 1], str) == 0) {
				first--;
			}
			
			// �ҵ����һ��
			int last = index;
			while (last < end && compare(datas[last + 1], str) == 0) {
				last++;
			}
			
			IntArray result = new IntArray(last - first + 1);
			if (isFromHead) {
				for (; first <= last; ++first) {
					result.pushInt(first);
				}
			} else {
				for (; last >= first; --last) {
					result.pushInt(last);
				}
			}
			
			return result;
		} else {
			IntArray result = new IntArray(7);
			if (isFromHead) {
				for (int i = start; i <= size; ++i) {
					if (compare(datas[i], str) == 0) {
						result.addInt(i);
					}
				}
			} else {
				for (int i = start; i > 0; --i) {
					if (compare(datas[i], str) == 0) {
						result.addInt(i);
					}
				}
			}
			
			return result;
		}
	}
	
	/**
	 * �������Ա�����ֵ
	 * @return IArray ����ֵ����
	 */
	public IArray abs() {
		MessageManager mm = EngineMessage.get();
		throw new RuntimeException(getDataType() + mm.getMessage("Variant2.illAbs"));
	}

	/**
	 * �������Ա��
	 * @return IArray ��ֵ����
	 */
	public IArray negate() {
		int size = this.size;
		String []datas = this.datas;
		
		// ����Ҫ�жϳ�Ա�Ƿ���null
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					datas[i] = Variant.negate(datas[i]);
				}
			}
			
			return this;
		} else {
			String []newDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					newDatas[i] = Variant.negate(datas[i]);
				}
			}
			
			StringArray  result = new StringArray(newDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * �������Ա���
	 * @return IArray ��ֵ����
	 */
	public IArray not() {
		String []datas = this.datas;
		int size = this.size;
		
		boolean []newDatas = new boolean[size + 1];
		for (int i = 1; i <= size; ++i) {
			newDatas[i] = datas[i] == null;
		}
		
		IArray  result = new BoolArray(newDatas, size);
		result.setTemporary(true);
		return result;
	}

	/**
	 * �ж�����ĳ�Ա�Ƿ����������԰���null��
	 * @return true����������false�����з�����ֵ
	 */
	public boolean isNumberArray() {
		return false;
	}

	/**
	 * ����������������Ӧ�ĳ�Ա�ĺ�
	 * @param array �Ҳ�����
	 * @return ������
	 */
	public IArray memberAdd(IArray array) {
		if (array instanceof StringArray) {
			return memberAdd((StringArray)array);
		} else if (array instanceof ConstArray) {
			return memberAdd(array.get(1));
		} else if (array instanceof ObjectArray) {
			int size = this.size;
			String []d1 = this.datas;
			Object []d2 = ((ObjectArray)array).getDatas();
			Object []resultDatas = new Object[size + 1];
			
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = Variant.add(d1[i], d2[i]);
			}
			
			ObjectArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else if (array instanceof NumberArray) {
			return memberAdd((NumberArray)array);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					array.getDataType() + mm.getMessage("Variant2.illAdd"));
		}
	}

	/**
	 * ��������ĳ�Ա��ָ�������ĺ�
	 * @param value ����
	 * @return ������
	 */
	public IArray memberAdd(Object value) {
		if (value == null) {
			return this;
		}
		
		int size = this.size;
		String []datas = this.datas;
		
		if (value instanceof String) {
			String str = (String)value;
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						datas[i] += str;
					} else {
						datas[i] = str;
					}
				}
				
				return this;
			} else {
				String []newDatas = new String[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						newDatas[i] = datas[i] + str;
					} else {
						newDatas[i] = str;
					}
				}
				
				StringArray result = new StringArray(newDatas, size);
				result.setTemporary(true);
				return result;
			}
		} else if (value instanceof Number) {
			Number n2 = (Number)value;
			Object []resultDatas = new Object[size + 1];
			
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					Number n1 = Variant.parseNumber(datas[i]);
					if (n1 == null) {
						resultDatas[i] = n2;
					} else {
						resultDatas[i] = Variant.addNum(n1, n2);
					}
				} else {
					resultDatas[i] = n2;
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
					Variant.getDataType(value) + mm.getMessage("Variant2.illAdd"));
		}
	}

	private StringArray memberAdd(StringArray array) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		String []newDatas;
		StringArray result;
		
		if (isTemporary()) {
			newDatas = d1;
			result = this;
		} else if (array.isTemporary()) {
			newDatas = d2;
			result = array;
		} else {
			newDatas = new String[size + 1];
			result = new StringArray(newDatas, size);
		}
		
		for (int i = 1; i <= size; ++i) {
			if (d1[i] != null) {
				if (d2[i] != null) {
					newDatas[i] = d1[i] + d2[i];
				} else {
					newDatas[i] = d1[i];
				}
			} else {
				newDatas[i] = d2[i];
			}
		}
		
		return result;
	}
	
	public ObjectArray memberAdd(NumberArray array) {
		int size = this.size;
		String []datas = this.datas;
		Object []resultDatas = new Object[size + 1];
		
		for (int i = 1; i <= size; ++i) {
			if (datas[i] != null) {
				Number n1 = Variant.parseNumber(datas[i]);
				if (n1 == null) {
					resultDatas[i] = array.get(i);
				} else {
					resultDatas[i] = Variant.addNum(n1, (Number)array.get(i));
				}
			} else {
				resultDatas[i] = array.get(i);
			}
		}
		
		ObjectArray result = new ObjectArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	/**
	 * ����������������Ӧ�ĳ�Ա�Ĳ�
	 * @param array �Ҳ�����
	 * @return ������
	 */
	public IArray memberSubtract(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illSubtract"));
	}

	/**
	 * ����������������Ӧ�ĳ�Ա�Ļ�
	 * @param array �Ҳ�����
	 * @return ������
	 */
	public IArray memberMultiply(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illMultiply"));
	}

	/**
	 * ��������ĳ�Ա��ָ�������Ļ�
	 * @param value ����
	 * @return ������
	 */
	public IArray memberMultiply(Object value) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				Variant.getDataType(value) + mm.getMessage("Variant2.illMultiply"));
	}

	/**
	 * ���Ҳ�����ĳ�Ա���Stringƴ�ӵ��������ĳ�Ա��
	 * @param array �Ҳ�����
	 * @return ������
	 */
	public IArray memberDivide(IArray array) {
		if (array instanceof StringArray) {
			return memberDivide((StringArray)array);
		} else if (array instanceof IntArray) {
			return memberDivide((IntArray)array);
		} else if (array instanceof LongArray) {
			return memberDivide((LongArray)array);
		} else if (array instanceof DoubleArray) {
			return memberDivide((DoubleArray)array);
		} else if (array instanceof ConstArray) {
			return memberDivide(array.get(1));
		} else if (array instanceof BoolArray) {
			return memberDivide((BoolArray)array);
		} else {
			int size = this.size;
			String []datas = this.datas;
			
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (!array.isNull(i)) {
						if (datas[i] != null) {
							datas[i] += array.get(i);
						} else {
							datas[i] = array.get(i).toString();
						}
					}
				}
				
				return this;
			} else {
				String []resultDatas = new String[size + 1];
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						if (!array.isNull(i)) {
							resultDatas[i] = datas[i] + array.get(i);
						} else {
							resultDatas[i] = datas[i];
						}
					} else if (!array.isNull(i)) {
						resultDatas[i] = array.get(i).toString();
					}
				}
				
				StringArray result = new StringArray(resultDatas, size);
				result.setTemporary(true);
				return result;
			}
		}
	}
	
	private StringArray memberDivide(Object value) {
		if (value == null) {
			return this;
		}
		
		String str = value.toString();
		int size = this.size;
		String []datas = this.datas;

		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					datas[i] += str;
				} else {
					datas[i] = str;
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (datas[i] != null) {
					resultDatas[i] = datas[i] + str;
				} else {
					resultDatas[i] = str;
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(StringArray array) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (d2[i] != null) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = d2[i];
					}
				}
			}
			
			return this;
		} else if (array.isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (d2[i] != null) {
						d2[i] = d1[i] + d2[i];
					} else {
						d2[i] = d1[i];
					}
				}
			}
			
			return array;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (d2[i] != null) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else {
					resultDatas[i] = d2[i];
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(IntArray array) {
		int size = this.size;
		String []d1 = this.datas;
		int []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Integer.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Integer.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(LongArray array) {
		int size = this.size;
		String []d1 = this.datas;
		long []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Long.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Long.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(DoubleArray array) {
		int size = this.size;
		String []d1 = this.datas;
		double []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Double.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Double.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	private StringArray memberDivide(BoolArray array) {
		int size = this.size;
		String []d1 = this.datas;
		boolean []d2 = array.getDatas();
		boolean []s2 = array.getSigns();
		
		if (isTemporary()) {
			for (int i = 1; i <= size; ++i) {
				if (s2 == null || !s2[i]) {
					if (d1[i] != null) {
						d1[i] += d2[i];
					} else {
						d1[i] = Boolean.toString(d2[i]);
					}
				}
			}
			
			return this;
		} else {
			String []resultDatas = new String[size + 1];
			for (int i = 1; i <= size; ++i) {
				if (d1[i] != null) {
					if (s2 == null || !s2[i]) {
						resultDatas[i] = d1[i] + d2[i];
					} else {
						resultDatas[i] = d1[i];
					}
				} else if (s2 == null || !s2[i]) {
					resultDatas[i] = Boolean.toString(d2[i]);
				}
			}
			
			StringArray result = new StringArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * ����������������Ӧ������Աȡ������г�Ա�����
	 * @param array �Ҳ�����
	 * @return ����������������������
	 */
	public IArray memberMod(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illMod"));
	}

	/**
	 * �����������������Ա���������г�Ա�
	 * @param array �Ҳ�����
	 * @return ����ֵ��������в����
	 */
	public IArray memberIntDivide(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illDivide"));
	}

	/**
	 * ����������������Ӧ�ĳ�Ա�Ĺ�ϵ����
	 * @param array �Ҳ�����
	 * @param relation �����ϵ������Relation�����ڡ�С�ڡ����ڡ�...��
	 * @return ��ϵ����������
	 */
	public BoolArray calcRelation(IArray array, int relation) {
		if (array instanceof StringArray) {
			return calcRelation((StringArray)array, relation);
		} else if (array instanceof ConstArray) {
			return calcRelation(array.get(1), relation);
		} else if (array instanceof ObjectArray) {
			return calcRelation((ObjectArray)array, relation);
		} else if (array instanceof BoolArray) {
			return ((BoolArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof IntArray) {
			return ((IntArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof LongArray) {
			return ((LongArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof DoubleArray) {
			return ((DoubleArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else if (array instanceof DateArray) {
			return ((DateArray)array).calcRelation(this, Relation.getInverseRelation(relation));
		} else {
			return array.calcRelation(this, Relation.getInverseRelation(relation));
		}
	}
	
	/**
	 * ����������������Ӧ�ĳ�Ա�Ĺ�ϵ����
	 * @param array �Ҳ�����
	 * @param relation �����ϵ������Relation�����ڡ�С�ڡ����ڡ�...��
	 * @return ��ϵ����������
	 */
	public BoolArray calcRelation(Object value, int relation) {
		if (value instanceof String) {
			return calcRelation((String)value, relation);
		} else if (value == null) {
			return ArrayUtil.calcRelationNull(datas, size, relation);
		} else {
			boolean b = Variant.isTrue(value);
			int size = this.size;
			String []datas = this.datas;
			
			if (relation == Relation.AND) {
				BoolArray result;
				if (!b) {
					result = new BoolArray(false, size);
				} else {
					boolean []resultDatas = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = datas[i] != null;
					}
					
					result = new BoolArray(resultDatas, size);
				}
				
				result.setTemporary(true);
				return result;
			} else if (relation == Relation.OR) {
				BoolArray result;
				if (b) {
					result = new BoolArray(true, size);
				} else {
					boolean []resultDatas = new boolean[size + 1];
					for (int i = 1; i <= size; ++i) {
						resultDatas[i] = datas[i] != null;
					}
					
					result = new BoolArray(resultDatas, size);
				}
				
				result.setTemporary(true);
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
						getDataType(), Variant.getDataType(value)));
			}
		}
	}
	
	private BoolArray calcRelation(String value, int relation) {
		int size = this.size;
		String []d1 = this.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// �Ƿ�����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// �Ƿ�����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// �Ƿ���ڵ����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// �Ƿ�С���ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// �Ƿ�С�ڵ����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// �Ƿ񲻵����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], value) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null;
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = true;
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}
	
	private BoolArray calcRelation(StringArray array, int relation) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// �Ƿ�����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// �Ƿ�����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// �Ƿ���ڵ����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// �Ƿ�С���ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// �Ƿ�С�ڵ����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// �Ƿ񲻵����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null && d2[i] != null;
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null || d2[i] != null;
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	protected BoolArray calcRelation(ObjectArray array, int relation) {
		int size = this.size;
		String []d1 = this.datas;
		Object []d2 = array.getDatas();
		boolean []resultDatas = new boolean[size + 1];
		
		if (relation == Relation.EQUAL) {
			// �Ƿ�����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) == 0;
			}
		} else if (relation == Relation.GREATER) {
			// �Ƿ�����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) > 0;
			}
		} else if (relation == Relation.GREATER_EQUAL) {
			// �Ƿ���ڵ����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) >= 0;
			}
		} else if (relation == Relation.LESS) {
			// �Ƿ�С���ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) < 0;
			}
		} else if (relation == Relation.LESS_EQUAL) {
			// �Ƿ�С�ڵ����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) <= 0;
			}
		} else if (relation == Relation.NOT_EQUAL) {
			// �Ƿ񲻵����ж�
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = compare(d1[i], d2[i]) != 0;
			}
		} else if (relation == Relation.AND) {
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null && Variant.isTrue(d2[i]);
			}
		} else { // Relation.OR
			for (int i = 1; i <= size; ++i) {
				resultDatas[i] = d1[i] != null || Variant.isTrue(d2[i]);
			}
		}
		
		BoolArray result = new BoolArray(resultDatas, size);
		result.setTemporary(true);
		return result;
	}

	/**
	 * �Ƚ���������Ĵ�С
	 * @param array �Ҳ�����
	 * @return 1����ǰ�����0������������ȣ�-1����ǰ����С
	 */
	public int compareTo(IArray array) {
		int size1 = this.size;
		int size2 = array.size();
		String []d1 = this.datas;
		
		int size = size1;
		int result = 0;
		if (size1 < size2) {
			result = -1;
		} else if (size1 > size2) {
			result = 1;
			size = size2;
		}

		if (array instanceof StringArray) {
			StringArray array2 = (StringArray)array;
			String []d2 = array2.datas;
			
			for (int i = 1; i <= size; ++i) {
				int cmp = compare(d1[i], d2[i]);
				if (cmp != 0) {
					return cmp;
				}
			}
		} else if (array instanceof ConstArray) {
			Object value = array.get(1);
			if (value instanceof String) {
				String d2 = (String)value;
				for (int i = 1; i <= size; ++i) {
					int cmp = compare(d1[i], d2);
					if (cmp != 0) {
						return cmp;
					}
				}
			} else if (value == null) {
				for (int i = 1; i <= size; ++i) {
					if (d1[i] != null) {
						return 1;
					}
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
						getDataType(), array.getDataType()));
			}
		} else if (array instanceof ObjectArray) {
			ObjectArray array2 = (ObjectArray)array;
			Object []d2 = array2.getDatas();
			
			for (int i = 1; i <= size; ++i) {
				int cmp = compare(d1[i], d2[i]);
				if (cmp != 0) {
					return cmp;
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		}
		
		return result;
	}
	
	/**
	 * ���������2����Ա�ıȽ�ֵ
	 * @param index1 ��Ա1
	 * @param index2 ��Ա2
	 * @return
	 */
	public int memberCompare(int index1, int index2) {
		return compare(datas[index1], datas[index2]);
	}
	
	/**
	 * �ж������������Ա�Ƿ����
	 * @param index1 ��Ա1
	 * @param index2 ��Ա2
	 * @return
	 */
	public boolean isMemberEquals(int index1, int index2) {
		if (datas[index1] == null) {
			return datas[index2] == null;
		} else if (datas[index2] == null) {
			return false;
		} else {
			return datas[index1].equals(datas[index2]);
		}
	}
	
	/**
	 * �ж����������ָ��Ԫ���Ƿ���ͬ
	 * @param curIndex ��ǰ�����Ԫ�ص�����
	 * @param array Ҫ�Ƚϵ�����
	 * @param index Ҫ�Ƚϵ������Ԫ�ص�����
	 * @return true����ͬ��false������ͬ
	 */
	public boolean isEquals(int curIndex, IArray array, int index) {
		Object value = array.get(index);
		if (value instanceof String) {
			return ((String)value).equals(datas[curIndex]);
		} else if (value == null) {
			return datas[curIndex] == null;
		} else {
			return false;
		}
	}
	
	/**
	 * �ж������ָ��Ԫ���Ƿ������ֵ���
	 * @param curIndex ����Ԫ����������1��ʼ����
	 * @param value ֵ
	 * @return true����ȣ�false�������
	 */
	public boolean isEquals(int curIndex, Object value) {
		if (value instanceof String) {
			return ((String)value).equals(datas[curIndex]);
		} else if (value == null) {
			return datas[curIndex] == null;
		} else {
			return false;
		}
	}
	
	/**
	 * �ж����������ָ��Ԫ�صĴ�С
	 * @param curIndex ��ǰ�����Ԫ�ص�����
	 * @param array Ҫ�Ƚϵ�����
	 * @param index Ҫ�Ƚϵ������Ԫ�ص�����
	 * @return С�ڣ�С��0�����ڣ�0�����ڣ�����0
	 */
	public int compareTo(int curIndex, IArray array, int index) {
		return compare(datas[curIndex], array.get(index));
	}
	
	/**
	 * �Ƚ������ָ��Ԫ�������ֵ�Ĵ�С
	 * @param curIndex ��ǰ�����Ԫ�ص�����
	 * @param value Ҫ�Ƚϵ�ֵ
	 * @return
	 */
	public int compareTo(int curIndex, Object value) {
		return compare(datas[curIndex], value);
	}
	
	/**
	 * ȡָ����Ա�Ĺ�ϣֵ
	 * @param index ��Ա��������1��ʼ����
	 * @return ָ����Ա�Ĺ�ϣֵ
	 */
	public int hashCode(int index) {
		if (datas[index] != null) {
			return datas[index].hashCode();
		} else {
			return 0;
		}
	}
	
	/**
	 * ���Ա��
	 * @return
	 */
	public Object sum() {
		int size = this.size;
		if (size < 1) {
			return null;
		}
		
		String []datas = this.datas;
		String result = datas[1];
		
		for (int i = 2; i <= size; ++i) {
			if (datas[i] != null) {
				if (result == null) {
					result = datas[i];
				} else {
					result += datas[i];
				}
			}
		}
		
		return result;
	}
	
	/**
	 * ��ƽ��ֵ
	 * @return
	 */
	public Object average() {
		return null;
	}
	
	/**
	 * �õ����ĳ�Ա
	 * @return
	 */
	public Object max() {
		int size = this.size;
		if (size == 0) {
			return null;
		}

		String []datas = this.datas;
		String max = null;
		
		int i = 1;
		for (; i <= size; ++i) {
			if (datas[i] != null) {
				max = datas[i];
				break;
			}
		}
		
		for (++i; i <= size; ++i) {
			if (datas[i] != null && max.compareTo(datas[i]) < 0) {
				max = datas[i];
			}
		}
		
		return max;
	}
	
	/**
	 * �õ���С�ĳ�Ա
	 * @return
	 */
	public Object min() {
		int size = this.size;
		if (size == 0) {
			return null;
		}

		String []datas = this.datas;
		String min = null;
		
		int i = 1;
		for (; i <= size; ++i) {
			if (datas[i] != null) {
				min = datas[i];
				break;
			}
		}
		
		for (++i; i <= size; ++i) {
			if (datas[i] != null && min.compareTo(datas[i]) > 0) {
				min = datas[i];
			}
		}
		
		return min;
	}

	/**
	 * ����������������Ӧ�ĳ�Ա�Ĺ�ϵ���㣬ֻ����resultΪ�����
	 * @param array �Ҳ�����
	 * @param relation �����ϵ������Relation�����ڡ�С�ڡ����ڡ�...��
	 * @param result ������������ǰ��ϵ��������Ҫ����������߼�&&����||����
	 * @param isAnd true��������� && ���㣬false��������� || ����
	 */
	public void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd) {
		if (array instanceof StringArray) {
			calcRelations((StringArray)array, relation, result, isAnd);
		} else if (array instanceof ConstArray) {
			calcRelations(array.get(1), relation, result, isAnd);
		} else if (array instanceof ObjectArray) {
			calcRelations((ObjectArray)array, relation, result, isAnd);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), array.get(1),
					getDataType(), array.getDataType()));
		} 
	}

	/**
	 * ����������������Ӧ�ĳ�Ա�Ĺ�ϵ���㣬ֻ����resultΪ�����
	 * @param array �Ҳ�����
	 * @param relation �����ϵ������Relation�����ڡ�С�ڡ����ڡ�...��
	 * @param result ������������ǰ��ϵ��������Ҫ����������߼�&&����||����
	 * @param isAnd true��������� && ���㣬false��������� || ����
	 */
	public void calcRelations(Object value, int relation, BoolArray result, boolean isAnd) {
		if (value instanceof String) {
			calcRelations((String)value, relation, result, isAnd);
		} else if (value == null) {
			ArrayUtil.calcRelationsNull(datas, size, relation, result, isAnd);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", get(1), value,
					getDataType(), Variant.getDataType(value)));
		}
	}

	private void calcRelations(String value, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		String []d1 = this.datas;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// �������ִ��&&����
			if (relation == Relation.EQUAL) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// �Ƿ���ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// �Ƿ�С���ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// �Ƿ�С�ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// �Ƿ񲻵����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], value) == 0) {
						resultDatas[i] = false;
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			// �������ִ��||����
			if (relation == Relation.EQUAL) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// �Ƿ���ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// �Ƿ�С���ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// �Ƿ�С�ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// �Ƿ񲻵����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], value) != 0) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	private void calcRelations(StringArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		String []d1 = this.datas;
		String []d2 = array.datas;
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// �������ִ��&&����
			if (relation == Relation.EQUAL) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// �Ƿ���ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// �Ƿ�С���ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// �Ƿ�С�ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// �Ƿ񲻵����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) == 0) {
						resultDatas[i] = false;
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			// �������ִ��||����
			if (relation == Relation.EQUAL) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// �Ƿ���ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// �Ƿ�С���ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// �Ƿ�С�ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// �Ƿ񲻵����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	protected void calcRelations(ObjectArray array, int relation, BoolArray result, boolean isAnd) {
		int size = this.size;
		String []d1 = this.datas;
		Object []d2 = array.getDatas();
		boolean []resultDatas = result.getDatas();
		
		if (isAnd) {
			// �������ִ��&&����
			if (relation == Relation.EQUAL) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// �Ƿ���ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS) {
				// �Ƿ�С���ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// �Ƿ�С�ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = false;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// �Ƿ񲻵����ж�
				for (int i = 1; i <= size; ++i) {
					if (resultDatas[i] && compare(d1[i], d2[i]) == 0) {
						resultDatas[i] = false;
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			// �������ִ��||����
			if (relation == Relation.EQUAL) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) == 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER) {
				// �Ƿ�����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) > 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.GREATER_EQUAL) {
				// �Ƿ���ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) >= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS) {
				// �Ƿ�С���ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) < 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.LESS_EQUAL) {
				// �Ƿ�С�ڵ����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) <= 0) {
						resultDatas[i] = true;
					}
				}
			} else if (relation == Relation.NOT_EQUAL) {
				// �Ƿ񲻵����ж�
				for (int i = 1; i <= size; ++i) {
					if (!resultDatas[i] && compare(d1[i], d2[i]) != 0) {
						resultDatas[i] = true;
					}
				}
			} else {
				throw new RuntimeException();
			}
		}
	}

	/**
	 * ����������������Ӧ�ĳ�Ա�İ�λ��
	 * @param array �Ҳ�����
	 * @return ��λ��������
	 */
	public IArray bitwiseAnd(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("and" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * ����������������Ӧ�ĳ�Ա�İ�λ��
	 * @param array �Ҳ�����
	 * @return ��λ��������
	 */
	public IArray bitwiseOr(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("or" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * ����������������Ӧ�ĳ�Ա�İ�λ���
	 * @param array �Ҳ�����
	 * @return ��λ���������
	 */
	public IArray bitwiseXOr(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("xor" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * ���������Ա�İ�λȡ��
	 * @return ��Ա��λȡ���������
	 */
	public IArray bitwiseNot() {
		MessageManager mm = EngineMessage.get();
		throw new RQException("not" + mm.getMessage("function.paramTypeError"));
	}

	/**
	 * ȡ����ʶ����ȡֵΪ����ж�Ӧ�����ݣ����������
	 * @param signArray ��ʶ����
	 * @return IArray
	 */
	public IArray select(IArray signArray) {
		int size = signArray.size();
		String []d1 = this.datas;
		String []resultDatas = new String[size + 1];
		int count = 0;
		
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = 1; i <= size; ++i) {
					if (d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (!s2[i] && d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				if (signArray.isTrue(i)) {
					resultDatas[++count] = d1[i];
				}
			}
		}
		
		return new StringArray(resultDatas, count);
	}
	
	/**
	 * ȡĳһ���α�ʶ����ȡֵΪ��������������
	 * @param start ��ʼλ�ã�������
	 * @param end ����λ�ã���������
	 * @param signArray ��ʶ����
	 * @return IArray
	 */
	public IArray select(int start, int end, IArray signArray) {
		String []d1 = this.datas;
		String []resultDatas = new String[end - start + 1];
		int count = 0;
		
		if (signArray instanceof BoolArray) {
			BoolArray array = (BoolArray)signArray;
			boolean []d2 = array.getDatas();
			boolean []s2 = array.getSigns();
			
			if (s2 == null) {
				for (int i = start; i < end; ++i) {
					if (d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			} else {
				for (int i = start; i < end; ++i) {
					if (!s2[i] && d2[i]) {
						resultDatas[++count] = d1[i];
					}
				}
			}
		} else {
			for (int i = start; i < end; ++i) {
				if (signArray.isTrue(i)) {
					resultDatas[++count] = d1[i];
				}
			}
		}
		
		return new StringArray(resultDatas, count);
	}
	
	/**
	 * ��array��ָ��Ԫ�ؼӵ���ǰ�����ָ��Ԫ����
	 * @param curIndex ��ǰ�����Ԫ�ص�����
	 * @param array Ҫ��ӵ�����
	 * @param index Ҫ��ӵ������Ԫ�ص�����
	 * @return IArray
	 */
	public IArray memberAdd(int curIndex, IArray array, int index) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(getDataType() + mm.getMessage("Variant2.with") +
				array.getDataType() + mm.getMessage("Variant2.illAdd"));
	}	

	/**
	 * �ѳ�Աת�ɶ������鷵��
	 * @return ��������
	 */
	public Object[] toArray() {
		Object []result = new Object[size];
		System.arraycopy(datas, 1, result, 0, size);
		return result;
	}
	
	/**
	 * �ѳ�Ա�ָ��������
	 * @param result ���ڴ�ų�Ա������
	 */
	public void toArray(Object []result) {
		System.arraycopy(datas, 1, result, 0, size);
	}
	
	/**
	 * �������ָ��λ�ò����������
	 * @param pos λ�ã�����
	 * @return ���غ�벿��Ԫ�ع��ɵ�����
	 */
	public IArray split(int pos) {
		String []datas = this.datas;
		int size = this.size;
		int resultSize = size - pos + 1;
		String []resultDatas = new String[resultSize + 1];
		System.arraycopy(datas, pos, resultDatas, 1, resultSize);
		
		for (int i = pos; i <= size; ++i) {
			datas[i] = null;
		}
		
		this.size = pos - 1;
		return new StringArray(resultDatas, resultSize);
	}
	
	/**
	 * ��ָ������Ԫ�ط���������������
	 * @param from ��ʼλ�ã�����
	 * @param to ����λ�ã�����
	 * @return
	 */
	public IArray split(int from, int to) {
		String []datas = this.datas;
		int oldSize = this.size;
		int resultSize = to - from + 1;
		String []resultDatas = new String[resultSize + 1];
		System.arraycopy(datas, from, resultDatas, 1, resultSize);
		
		System.arraycopy(datas, to + 1, datas, from, oldSize - to);
		this.size -= resultSize;
		
		for (int i = this.size + 1; i <= oldSize; ++i) {
			datas[i] = null;
		}
		
		return new StringArray(resultDatas, resultSize);
	}
	
	/**
	 * �������Ԫ�ؽ�������
	 */
	public void sort() {
		MultithreadUtil.sort(datas, 1, size + 1);
	}
	
	/**
	 * �������Ԫ�ؽ�������
	 * @param comparator �Ƚ���
	 */
	public void sort(Comparator<Object> comparator) {
		MultithreadUtil.sort(datas, 1, size + 1, comparator);
	}
	
	/**
	 * �����������Ƿ��м�¼
	 * @return boolean
	 */
	public boolean hasRecord() {
		return false;
	}
	
	/**
	 * �����Ƿ��ǣ���������
	 * @param isPure true������Ƿ��Ǵ�����
	 * @return boolean true���ǣ�false������
	 */
	public boolean isPmt(boolean isPure) {
		return false;
	}
	
	/**
	 * ��������ķ�ת����
	 * @return IArray
	 */
	public IArray rvs() {
		int size = this.size;
		String []datas = this.datas;
		String []resultDatas = new String[size + 1];
		
		for (int i = 1, q = size; i <= size; ++i) {
			resultDatas[i] = datas[q--];
		}
		
		return new StringArray(resultDatas, size);
	}
	
	/**
	 * ������Ԫ�ش�С����������ȡǰcount����λ��
	 * @param count ���countС��0��ȡ��|count|����λ��
	 * @param isAll countΪ����1ʱ�����isAllȡֵΪtrue��ȡ����������һ��Ԫ�ص�λ�ã�����ֻȡһ��
	 * @param isLast �Ƿ�Ӻ�ʼ��
	 * @param ignoreNull �Ƿ���Կ�Ԫ��
	 * @return IntArray
	 */
	public IntArray ptop(int count, boolean isAll, boolean isLast, boolean ignoreNull) {
		int size = this.size;
		if (size == 0) {
			return new IntArray(0);
		}
		
		String []datas = this.datas;
		if (ignoreNull) {
			if (count == 1) {
				// ȡ��Сֵ��λ��
				String minValue = null;
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							minValue = datas[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null) {
							int cmp = datas[i].compareTo(minValue);
							if (cmp < 0) {
								minValue = datas[i];
								result.clear();
								result.addInt(i);
							} else if (cmp == 0) {
								result.addInt(i);
							}
						}
					}
					
					return result;
				} else if (isLast) {
					int i = size;
					int pos = 0;
					for (; i > 0; --i) {
						if (datas[i] != null) {
							minValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if (datas[i] != null && datas[i].compareTo(minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				} else {
					int i = 1;
					int pos = 0;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							minValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null && datas[i].compareTo(minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				}
			} else if (count > 1) {
				// ȡ��С��count��Ԫ�ص�λ��
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						int index = valueArray.binarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
							if (valueArray.size() == next) {
								valueArray.removeLast();
								posArray.removeLast();
							}
						}
					}
				}
				
				return posArray;
			} else if (count == -1) {
				// ȡ���ֵ��λ��
				String maxValue = null;
				if (isAll) {
					IntArray result = new IntArray(8);
					int i = 1;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							maxValue = datas[i];
							result.addInt(i);
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null) {
							int cmp = datas[i].compareTo(maxValue);;
							if (cmp > 0) {
								maxValue = datas[i];
								result.clear();
								result.addInt(i);
							} else if (cmp == 0) {
								result.addInt(i);
							}
						}
					}
					
					return result;
				} else if (isLast) {
					int i = size;
					int pos = 0;
					for (; i > 0; --i) {
						if (datas[i] != null) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (--i; i > 0; --i) {
						if (datas[i] != null && datas[i].compareTo(maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				} else {
					int i = 1;
					int pos = 0;
					for (; i <= size; ++i) {
						if (datas[i] != null) {
							maxValue = datas[i];
							pos = i;
							break;
						}
					}
					
					for (++i; i <= size; ++i) {
						if (datas[i] != null && datas[i].compareTo(maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					if (pos != 0) {
						result.pushInt(pos);
					}
					
					return result;
				}
			} else if (count < -1) {
				// ȡ����count��Ԫ�ص�λ��
				count = -count;
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						int index = valueArray.descBinarySearch(datas[i]);
						if (index < 1) {
							index = -index;
						}
						
						if (index <= count) {
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
							if (valueArray.size() == next) {
								valueArray.remove(next);
								posArray.remove(next);
							}
						}
					}
				}
				
				return posArray;
			} else {
				return new IntArray(1);
			}
		} else {
			if (count == 1) {
				// ȡ��Сֵ��λ��
				if (isAll) {
					IntArray result = new IntArray(8);
					result.addInt(1);
					String minValue = datas[1];
					
					for (int i = 2; i <= size; ++i) {
						int cmp = compare(datas[i], minValue);
						if (cmp < 0) {
							minValue = datas[i];
							result.clear();
							result.addInt(i);
						} else if (cmp == 0) {
							result.addInt(i);
						}
					}
					
					return result;
				} else if (isLast) {
					String minValue = datas[size];
					int pos = size;
					
					for (int i = size - 1; i > 0; --i) {
						if (compare(datas[i], minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				} else {
					String minValue = datas[1];
					int pos = 1;
					
					for (int i = 2; i <= size; ++i) {
						if (compare(datas[i], minValue) < 0) {
							minValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				}
			} else if (count > 1) {
				// ȡ��С��count��Ԫ�ص�λ��
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					int index = valueArray.binarySearch(datas[i]);
					if (index < 1) {
						index = -index;
					}
					
					if (index <= count) {
						valueArray.insert(index, datas[i]);
						posArray.insertInt(index, i);
						if (valueArray.size() == next) {
							valueArray.removeLast();
							posArray.removeLast();
						}
					}
				}
				
				return posArray;
			} else if (count == -1) {
				// ȡ���ֵ��λ��
				if (isAll) {
					IntArray result = new IntArray(8);
					String maxValue = datas[1];
					result.addInt(1);
					
					for (int i = 2; i <= size; ++i) {
						int cmp = compare(datas[i], maxValue);
						if (cmp > 0) {
							maxValue = datas[i];
							result.clear();
							result.addInt(i);
						} else if (cmp == 0) {
							result.addInt(i);
						}
					}
					
					return result;
				} else if (isLast) {
					String maxValue = datas[size];
					int pos = size;
					
					for (int i = size - 1; i > 0; --i) {
						if (compare(datas[i], maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				} else {
					String maxValue = datas[1];
					int pos = 1;
					
					for (int i = 2; i <= size; ++i) {
						if (compare(datas[i], maxValue) > 0) {
							maxValue = datas[i];
							pos = i;
						}
					}
					
					IntArray result = new IntArray(1);
					result.pushInt(pos);
					return result;
				}
			} else if (count < -1) {
				// ȡ����count��Ԫ�ص�λ��
				count = -count;
				int next = count + 1;
				StringArray valueArray = new StringArray(next);
				IntArray posArray = new IntArray(next);
				
				for (int i = 1; i <= size; ++i) {
					int index = valueArray.descBinarySearch(datas[i]);
					if (index < 1) {
						index = -index;
					}
					
					if (index <= count) {
						valueArray.insert(index, datas[i]);
						posArray.insertInt(index, i);
						if (valueArray.size() == next) {
							valueArray.remove(next);
							posArray.remove(next);
						}
					}
				}
				
				return posArray;
			} else {
				return new IntArray(1);
			}
		}
	}
	
	/**
	 * ������Ԫ�ش�С������������ȡǰcount����λ��
	 * @param count ���countС��0��Ӵ�С������
	 * @param ignoreNull �Ƿ���Կ�Ԫ��
	 * @param iopt �Ƿ�ȥ�ط�ʽ������
	 * @return IntArray
	 */
	public IntArray ptopRank(int count, boolean ignoreNull, boolean iopt) {
		int size = this.size;
		if (size == 0 || count == 0) {
			return new IntArray(0);
		}
		
		String []datas = this.datas;		
		if (count > 0) {
			// ȡ��С��count��Ԫ�ص�λ��
			int next = count + 1;
			StringArray valueArray = new StringArray(next);
			IntArray posArray = new IntArray(next);

			if (iopt) {
				int curCount = 0;
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						if (curCount < count) {
							int index = valueArray.binarySearch(datas[i]);
							if (index < 1) {
								curCount++;
								index = -index;
							}
							
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int curSize = valueArray.size();
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp < 0) {
								int index = valueArray.binarySearch(datas[i]);
								if (index < 1) {
									index = -index;
									
									// ɾ�������ͬ�ĳ�Ա
									String value = valueArray.getString(curSize);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize - 1; j >= count; --j) {
										if (valueArray.getString(j).compareTo(value) == 0) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
								
								valueArray.insert(index, datas[i]);
								posArray.insertInt(index, i);
							} else if (cmp == 0) {
								valueArray.add(datas[i]);
								posArray.addInt(i);
							}
						}
					} else if (!ignoreNull) {
						if (curCount < count) {
							if (curCount == 0 || !valueArray.isNull(1)) {
								curCount++;
							}
							
							valueArray.insert(1, null);
							posArray.insertInt(1, i);
						} else {
							if (valueArray.isNull(1)) {
								valueArray.add(datas[i]);
								posArray.addInt(i);
							} else {
								// ɾ�������ͬ�ĳ�Ա
								int curSize = valueArray.size();
								String value = valueArray.getString(curSize);
								valueArray.removeLast();
								posArray.removeLast();
								for (int j = curSize - 1; j >= count; --j) {
									if (valueArray.getString(j).compareTo(value) == 0) {
										valueArray.removeLast();
										posArray.removeLast();
									} else {
										break;
									}
								}
								
								valueArray.insert(1, null);
								posArray.insertInt(1, i);
							}
						}
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						int curSize = valueArray.size();
						if (curSize < count) {
							int index = valueArray.binarySearch(datas[i]);
							if (index < 1) {
								index = -index;
							}
							
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp < 0) {
								int index = valueArray.binarySearch(datas[i]);
								if (index < 1) {
									index = -index;
								}
								
								valueArray.insert(index, datas[i]);
								posArray.insertInt(index, i);
								
								if (valueArray.memberCompare(count, curSize + 1) != 0) {
									// ɾ�������ͬ�ĳ�Ա
									String value = valueArray.getString(curSize + 1);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize; j > count; --j) {
										if (valueArray.getString(j).compareTo(value) == 0) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
							} else if (cmp == 0) {
								valueArray.add(datas[i]);
								posArray.addInt(i);
							}
						}
					} else if (!ignoreNull) {
						int curSize = valueArray.size();
						if (curSize < count) {
							valueArray.insert(1, null);
							posArray.insertInt(1, i);
						} else {
							if (valueArray.isNull(curSize)) {
								valueArray.add(datas[i]);
								posArray.addInt(i);
							} else {
								valueArray.insert(1, null);
								posArray.insertInt(1, i);
								
								if (valueArray.memberCompare(count, curSize + 1) != 0) {
									// ɾ�������ͬ�ĳ�Ա
									String value = valueArray.getString(curSize + 1);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize; j > count; --j) {
										if (valueArray.getString(j).compareTo(value) == 0) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			
			return posArray;
		} else {
			// ȡ����count��Ԫ�ص�λ��
			count = -count;
			int next = count + 1;
			StringArray valueArray = new StringArray(next);
			IntArray posArray = new IntArray(next);

			if (iopt) {
				int curCount = 0;
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						if (curCount < count) {
							int index = valueArray.descBinarySearch(datas[i]);
							if (index < 1) {
								curCount++;
								index = -index;
							}
							
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int curSize = valueArray.size();
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp > 0) {
								int index = valueArray.descBinarySearch(datas[i]);
								if (index < 1) {
									index = -index;
									
									// ɾ�������ͬ�ĳ�Ա
									String value = valueArray.getString(curSize);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize - 1; j >= count; --j) {
										if (valueArray.getString(j).compareTo(value) == 0) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
								
								valueArray.insert(index, datas[i]);
								posArray.insertInt(index, i);
							} else if (cmp == 0) {
								valueArray.add(datas[i]);
								posArray.addInt(i);
							}
						}
					}
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					if (datas[i] != null) {
						int curSize = valueArray.size();
						if (curSize < count) {
							int index = valueArray.descBinarySearch(datas[i]);
							if (index < 1) {
								index = -index;
							}
							
							valueArray.insert(index, datas[i]);
							posArray.insertInt(index, i);
						} else {
							int cmp = compareTo(i, valueArray, curSize);
							if (cmp > 0) {
								int index = valueArray.descBinarySearch(datas[i]);
								if (index < 1) {
									index = -index;
								}
								
								valueArray.insert(index, datas[i]);
								posArray.insertInt(index, i);
								
								if (valueArray.memberCompare(count, curSize + 1) != 0) {
									// ɾ�������ͬ�ĳ�Ա
									String value = valueArray.getString(curSize + 1);
									valueArray.removeLast();
									posArray.removeLast();
									for (int j = curSize; j > count; --j) {
										if (valueArray.getString(j).compareTo(value) == 0) {
											valueArray.removeLast();
											posArray.removeLast();
										} else {
											break;
										}
									}
								}
							} else if (cmp == 0) {
								valueArray.add(datas[i]);
								posArray.addInt(i);
							}
						}
					}
				}
			}
			
			return posArray;
		}
	}

	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * �ѵ�ǰ����ת�ɶ������飬�����ǰ�����Ƕ��������򷵻����鱾��
	 * @return ObjectArray
	 */
	public ObjectArray toObjectArray() {
		Object []resultDatas = new Object[size + 1];
		System.arraycopy(datas, 1, resultDatas, 1, size);
		return new ObjectArray(resultDatas, size);
	}
	
	/**
	 * �Ѷ�������ת�ɴ��������飬����ת���׳��쳣
	 * @return IArray
	 */
	public IArray toPureArray() {
		return this;
	}
	
	/**
	 * �����������������������л����
	 * @param refOrigin ����Դ�У�����������
	 * @return
	 */
	public IArray reserve(boolean refOrigin) {
		if (isTemporary()) {
			setTemporary(false);
			return this;
		} else if (refOrigin) {
			return this;
		} else {
			return dup();
		}
	}
	
	/**
	 * ������������������ѡ����Ա��������飬�ӵ�ǰ����ѡ����־Ϊtrue�ģ���other����ѡ����־Ϊfalse��
	 * @param signArray ��־����
	 * @param other ��һ������
	 * @return IArray
	 */
	public IArray combine(IArray signArray, IArray other) {
		if (other instanceof ConstArray) {
			return combine(signArray, ((ConstArray)other).getData());
		}
		
		int size = this.size;
		String []datas = this.datas;
		
		if (other instanceof StringArray) {
			String []otherDatas = ((StringArray)other).getDatas();
			
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						datas[i] = otherDatas[i];
					}
				}
				
				return this;
			} else {
				String []resultDatas = new String[size + 1];
				System.arraycopy(datas, 1, resultDatas, 1, size);
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						resultDatas[i] = otherDatas[i];
					}
				}
				
				IArray result = new StringArray(resultDatas, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			System.arraycopy(datas, 1, resultDatas, 1, size);
			
			for (int i = 1; i <= size; ++i) {
				if (signArray.isFalse(i)) {
					resultDatas[i] = other.get(i);
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}

	/**
	 * ���������ӵ�ǰ����ѡ����־Ϊtrue�ģ���־Ϊfalse���ó�value
	 * @param signArray ��־����
	 * @param other ֵ
	 * @return IArray
	 */
	public IArray combine(IArray signArray, Object value) {
		int size = this.size;
		String []datas = this.datas;
		
		if (value instanceof String || value == null) {
			String str = (String)value;
			if (isTemporary()) {
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						datas[i] = str;
					}
				}
				
				return this;
			} else {
				String []resultDatas = new String[size + 1];
				System.arraycopy(datas, 1, resultDatas, 1, size);
				
				for (int i = 1; i <= size; ++i) {
					if (signArray.isFalse(i)) {
						resultDatas[i] = str;
					}
				}
				
				IArray result = new StringArray(resultDatas, size);
				result.setTemporary(true);
				return result;
			}
		} else {
			Object []resultDatas = new Object[size + 1];
			System.arraycopy(datas, 1, resultDatas, 1, size);
			
			for (int i = 1; i <= size; ++i) {
				if (signArray.isFalse(i)) {
					resultDatas[i] = value;
				}
			}
			
			IArray result = new ObjectArray(resultDatas, size);
			result.setTemporary(true);
			return result;
		}
	}
	
	/**
	 * ����ָ������ĳ�Ա�ڵ�ǰ�����е�λ��
	 * @param array �����ҵ�����
	 * @param opt ѡ�b��ͬ��鲢�����ң�i�����ص��������У�c����������
	 * @return λ�û���λ������
	 */
	public Object pos(IArray array, String opt) {
		return ArrayUtil.pos(this, array, opt);
	}

	/**
	 * ���������Ա�Ķ����Ʊ�ʾʱ1�ĸ�����
	 * @return
	 */
	public int bit1() {
		MessageManager mm = EngineMessage.get();
		throw new RQException("bit1" + mm.getMessage("function.paramTypeError"));
	}

	/**
	 * ���������Ա��λ���ֵ�Ķ����Ʊ�ʾʱ1�ĸ�����
	 * @param array �������
	 * @return 1�ĸ�����
	 */
	public int bit1(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("bit1" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * ȡָ��λ��������ͬ��Ԫ������
	 * @param index λ��
	 * @return ������ͬ��Ԫ������
	 */
	public int getNextEqualCount(int index) {
		String []datas = this.datas;
		int size = this.size;
		int count = 1;
		
		String value = datas[index];
		if (value == null) {
			for (++index; index <= size; ++index) {
				if (datas[index] == null) {
					count++;
				} else {
					break;
				}
			}
		} else {
			for (++index; index <= size; ++index) {
				if (datas[index] != null && datas[index].equals(value)) {
					count++;
				} else {
					break;
				}
			}
		}
		
		return count;
	}
}
