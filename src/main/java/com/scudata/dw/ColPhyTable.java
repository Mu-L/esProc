package com.scudata.dw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.array.IArray;
import com.scudata.array.LongArray;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MergesCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.cursor.UpdateIdCursor;
import com.scudata.expression.Constant;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.mfn.serial.Sbs;
import com.scudata.expression.operator.Add;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.NotEquals;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Smaller;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.ThreadPool;
import com.scudata.util.Variant;

/**
 * �д������
 * @author runqian
 *
 */
public class ColPhyTable extends PhyTable {
	private transient ColumnMetaData []columns;
	private transient ColumnMetaData []allColumns; //������
	private transient ColumnMetaData []sortedColumns; // �����ֶ�
	private transient ColumnMetaData []allSortedColumns; // �����ֶκ�����
	
	private transient String []allKeyColNames; // �����ֶ������飨������
	
	private transient ColumnMetaData guideColumn;//����
	protected int sortedColStartIndex;//����������ֶθ���
	
	private static final String GUIDE_COLNAME = "_guidecol";

	/**
	 * �������л�
	 * @param groupTable
	 */
	public ColPhyTable(ComTable groupTable) {
		this.groupTable = groupTable;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
	}
	
	/**
	 * �������л�
	 * @param groupTable
	 * @param parent
	 */
	public ColPhyTable(ComTable groupTable, ColPhyTable parent) {
		this.groupTable = groupTable;
		this.parent = parent;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
	}
	
	/**
	 * �����´���һ������
	 * @param groupTable
	 * @param colNames
	 * @param serialBytesLen
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, String []colNames) throws IOException {
		this.groupTable = groupTable;
		this.tableName = "";
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		int keyStart = -1; // ��������ʼ�ֶ�
		
		// ������ʼ�ֶ�ǰ����ֶ���Ϊ�������ֶ�
		for (int i = 0; i < count; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				keyStart = i;
				break;
			}
		}
		
		for (int i = 0; i < count; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				String colName = colNames[i].substring(KEY_PREFIX.length());
				columns[i] = new ColumnMetaData(this, colName, true, true);
			} else if (i < keyStart) {
				columns[i] = new ColumnMetaData(this, colNames[i], true, false);
			} else {
				columns[i] = new ColumnMetaData(this, colNames[i], false, false);
			}
		}
		
		init();
		
		if (sortedColumns == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		tableList = new ArrayList<PhyTable>();
		this.reserve[0] = 4;
	}

	/**
	 * �����´���һ������
	 * @param groupTable
	 * @param colNames
	 * @param serialBytesLen
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, String []colNames, int []serialBytesLen) throws IOException {
		this.groupTable = groupTable;
		this.tableName = "";
		this.colNames = colNames;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		for (int i = 0; i < count; ++i) {
			columns[i] = new ColumnMetaData(this, colNames[i], serialBytesLen[i]);
		}
		
		init();
		
		if (sortedColumns == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		tableList = new ArrayList<PhyTable>();
	}

	/**
	 * ����Ĵ���
	 * @param groupTable Ҫ������������
	 * @param colNames ������
	 * @param serialBytesLen �źų���
	 * @param tableName ������
	 * @param parent �������
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, String []colNames, int []serialBytesLen,
			String tableName, ColPhyTable parent) throws IOException {
		this.groupTable = groupTable;
		this.parent = parent;
		this.tableName = tableName;
		this.colNames = colNames;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		for (int i = 0; i < count; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				String colName = colNames[i].substring(KEY_PREFIX.length());
				columns[i] = new ColumnMetaData(this, colName, true, true);
			} else {
				columns[i] = new ColumnMetaData(this, colNames[i], false, false);
			}
		}
		
		init();
		
		if (getAllSortedColumns() == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		if (parent != null) {
			//Ŀǰ����ֻ����һ��
			if (parent.parent != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.dsNotMatch"));
			}
			
			PhyTable primaryTable = parent;
			String []primarySortedColNames = primaryTable.getSortedColNames();
			String []primaryColNames = primaryTable.getColNames();
			ArrayList<String> collist = new ArrayList<String>();
			for (String name : primaryColNames) {
				collist.add(name);
			}
			
			//�ֶβ����������ֶ��ظ�
			for (int i = 0, len = colNames.length; i < len; i++) {
				if (collist.contains(colNames[i])) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(colNames[i] + mm.getMessage("dw.fieldSameToPrimaryTable"));
				}
			}
			
			for (String name : primarySortedColNames) {
				collist.remove(name);
			}

			sortedColStartIndex = primarySortedColNames.length;
			guideColumn = new ColumnMetaData(this, tableName + GUIDE_COLNAME, false, false);
		}
		
		tableList = new ArrayList<PhyTable>();
	}
	
	/**
	 * ����src����һ��ͬ���Ļ���
	 * @param groupTable Ҫ������������
	 * @param parent �������
	 * @param src �ṩ�ṹ��Դ����
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, ColPhyTable parent, ColPhyTable src) throws IOException {
		this.groupTable = groupTable;
		this.parent = parent;
		
		System.arraycopy(src.reserve, 0, reserve, 0, reserve.length);
		segmentCol = src.segmentCol;
		segmentSerialLen = src.segmentSerialLen;
		tableName = src.tableName;
		colNames = src.colNames;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		ColumnMetaData []srcCols = src.getColumns();
		for (int i = 0; i < count; ++i) {
			columns[i] = new ColumnMetaData(this, srcCols[i]);
		}
		
		init();
		
		if (sortedColumns == null && allSortedColumns == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		if (parent != null) {
			guideColumn = new ColumnMetaData(this, tableName + GUIDE_COLNAME, false, false);
		}

		dupIndexAdnCuboid(src);
		
		tableList = new ArrayList<PhyTable>();
		for (PhyTable srcSub : src.tableList) {
			tableList.add(new ColPhyTable(groupTable, this, (ColPhyTable)srcSub));
		}
	}

	/**
	 * ��ʼ������ȡά�������Ȼ�����Ϣ
	 */
	protected void init() {
		ColumnMetaData []columns = this.columns;
		int dimCount = 0;
		int keyCount = 0;
		int j = 0;
		
		// ���������飬�����ļ���ʱ����Ӱ�쵽�����������Ĵ���
		colNames = new String[columns.length];
		for (ColumnMetaData col : columns) {
			if (col.isDim()) {
				dimCount++;
			}
			
			if (col.isKey()) {
				keyCount++;
			}
			
			colNames[j++] = col.getColName();
		}
		
		if (keyCount > 0) {
			sortedColumns = new ColumnMetaData[dimCount];
			allKeyColNames = new String[keyCount];
			
			int i = 0, k = 0;
			for (ColumnMetaData col : columns) {
				if (col.isDim()) {
					sortedColumns[i++] = col;
				}
				
				if (col.isKey()) {
					allKeyColNames[k++] = col.getColName();
				}
			}
		}
		
		if (parent != null) {
			// �ϲ����������
			String []parentKeys = parent.getAllKeyColNames();
			if (keyCount > 0) {
				int parentKeyCount = parentKeys.length;
				String []tmp = new String[parentKeyCount+ keyCount ];
				System.arraycopy(parentKeys, 0, tmp, 0, parentKeyCount);
				System.arraycopy(allKeyColNames, 0, tmp, parentKeyCount, keyCount);
				allKeyColNames = tmp;
			} else {
				allKeyColNames = parentKeys;
			}
			
			String []primarySortedColNames = parent.getSortedColNames();
			sortedColStartIndex = primarySortedColNames.length;
			
			allSortedColumns = new ColumnMetaData[sortedColStartIndex + dimCount];
			ColumnMetaData []baseSortedCols = ((ColPhyTable) parent).getSortedColumns();
			int i = 0;
			if (baseSortedCols != null) {
				for (ColumnMetaData col : baseSortedCols) {
					allSortedColumns[i++] = col;
				}
			}
			if (sortedColumns != null) {
				for (ColumnMetaData col : sortedColumns) {
					allSortedColumns[i++] = col;
				}
			}
			ColumnMetaData []parentColumns = ((ColPhyTable) parent).getSortedColumns();
			allColumns = new ColumnMetaData[parentColumns.length + columns.length];
			allColNames = new String[parentColumns.length + columns.length];
			i = 0;
			for (ColumnMetaData col : parentColumns) {
				allColNames[i] = col.getColName();
				allColumns[i++] = col;
			}
			for (ColumnMetaData col : columns) {
				allColNames[i] = col.getColName();
				allColumns[i++] = col;
			}
			ds = new DataStruct(allColNames);
		} else {
			ds = new DataStruct(colNames);
		}
	}
	
	public ColumnMetaData[] getColumns() {
		return columns;
	}
	
	/**
	 * ���������С���������key�У�
	 * @return
	 */
	public ColumnMetaData[] getAllColumns() {
		if (parent == null) return columns;
		return allColumns;
	}
	
	/**
	 * ���������С��������������ֶ�)
	 * @return
	 */
	ColumnMetaData[] getTotalColumns() {
		if (parent == null) return columns;
		int baseColCount = ((ColPhyTable)parent).columns.length;
		int len = baseColCount + columns.length;
		ColumnMetaData[] cols = new ColumnMetaData[len];
		System.arraycopy(((ColPhyTable)parent).columns, 0, cols, 0, baseColCount);
		System.arraycopy(columns, 0, cols, baseColCount, columns.length);		
		return cols;
	}
	
	/**
	 * �������������ơ��������������ֶ�)
	 * @return
	 */
	public String[] getTotalColNames() {
		if (parent == null) return colNames;
		int baseColCount = parent.colNames.length;
		int len = baseColCount + colNames.length;
		String[] names = new String[len];
		System.arraycopy(parent.colNames, 0, names, 0, baseColCount);
		System.arraycopy(colNames, 0, names, baseColCount, colNames.length);		
		return names;
	}
	
	/**
	 * ����������
	 * @return
	 */
	public ColumnMetaData[] getSortedColumns() {
		return sortedColumns;
	}
	
	/**
	 * ���������У�������)
	 * @return
	 */
	public ColumnMetaData[] getAllSortedColumns() {
		if (parent == null) return sortedColumns;
		return allSortedColumns;
	}
	
	/**
	 * ������������
	 * @return
	 */
	public String[] getSortedColNames() {
		if (sortedColumns == null) return null;
		int len = sortedColumns.length;
		String []names = new String[len];
		for (int i = 0; i < len; ++i) {
			names[i] = sortedColumns[i].getColName();
		}
		return names;
	}

	/**
	 * ��������������������)
	 * @return
	 */
	public String[] getAllSortedColNames() {
		if (parent == null) return getSortedColNames();
		if (allSortedColumns == null) return null;
		int len = allSortedColumns.length;
		String []names = new String[len];
		for (int i = 0; i < len; ++i) {
			names[i] = allSortedColumns[i].getColName();
		}
		return names;
	}
	
	/**
	 * ȡ�����ֶ�����������
	 * @return �����ֶ�������
	 */
	public String[] getAllKeyColNames() {
		return allKeyColNames;
	}
	
	/**
	 * �����ֶ�������ָ����
	 * @param fields �ֶ���
	 * @return
	 */
	public ColumnMetaData[] getColumns(String []fields) {
		if (fields == null) {
			return columns;
		}
		
		ColumnMetaData []columns = this.columns;
		int srcCount = columns.length;
		int count = fields.length;
		ColumnMetaData []result = new ColumnMetaData[count];
		
		Next:
		for (int i = 0; i < count; ++i) {
			String field = fields[i];
			for (int s = 0; s < srcCount; ++s) {
				if (columns[s].isColumn(field)) {
					result[i] = columns[s];
					continue Next;
				}
			}
			
			MessageManager mm = EngineMessage.get();
			throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
		}
		
		return result;
	}
	
	/**
	 * ����ÿ���źŵĳ��ȣ�������ź��еĻ���
	 */
	public int[] getSerialBytesLen() {
		int len = columns.length;
		int []serialBytesLen = new int[len];
		for (int i = 0; i < len; i++) {
			serialBytesLen[i] = columns[i].getSerialBytesLen();
		}
		return serialBytesLen;
	}
	
	/**
	 * ��ȡexp���漰����
	 * @param exps
	 * @return
	 */
	public ColumnMetaData[] getColumns(Expression []exps) {
		if (exps == null) {
			return columns;
		}
		
		ColumnMetaData []columns = this.columns;
		int srcCount = columns.length;
		int count = exps.length;
		ColumnMetaData []result = new ColumnMetaData[count];

		Next:
		for (int i = 0; i < count; ++i) {
			if (exps[i].getHome() instanceof UnknownSymbol) {
				String col = exps[i].getIdentifierName();
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].isColumn(col)) {
						result[i] = columns[s];
						continue Next;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException(col + mm.getMessage("ds.fieldNotExist"));
			}
		}
		return result;
	}
	
	/**
	 * ��ȡexp����Ҫ�������(k.sbs() k1+k2)
	 * @param exps
	 * @return
	 */
	public ArrayList<ColumnMetaData> getExpColumns(Expression []exps) {
		if (exps == null) {
			return null;
		}
		
		ArrayList<ColumnMetaData> result = new ArrayList<ColumnMetaData>();
		ColumnMetaData []columns = this.columns;
		int srcCount = columns.length;
		int count = exps.length;

		Next:
		for (int i = 0; i < count; ++i) {
			if (exps[i].getHome() instanceof DotOperator) {
				String col = null;
				Object left = exps[i].getHome().getLeft();
				Object right = exps[i].getHome().getRight();
				if (left instanceof UnknownSymbol && right instanceof Sbs) {
					col = ((UnknownSymbol)left).getName();
				} else {
					continue;
				}
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].isColumn(col)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						continue Next;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException(col + mm.getMessage("ds.fieldNotExist"));
			} else if (exps[i].getHome() instanceof Add) {
				String col1 = null;
				String col2 = null;
				Object obj1 = exps[i].getHome().getLeft();
				Object obj2 = exps[i].getHome().getRight();
				if ((obj1 instanceof UnknownSymbol) 
						&& (obj2 instanceof UnknownSymbol)) {
					col1 = ((UnknownSymbol)obj1).getName();
					col2 = ((UnknownSymbol)obj2).getName();
				}
				boolean b1 = false,b2 = false;
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].isColumn(col1)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						b1 = true;
					}
					if (columns[s].isColumn(col2)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						b2 = true;
					}
				}
				if (b1 && b2) continue;
				MessageManager mm = EngineMessage.get();
				throw new RQException(col1 + " or " + col2 + mm.getMessage("ds.fieldNotExist"));
			}
		}
		if (result.size() == 0) {
			return null;
		}
		return result;
	}

	/**
	 * �����ֶ��������
	 * @param field
	 * @return
	 */
	public ColumnMetaData getColumn(String field) {
		ColumnMetaData []columns = this.columns;
		for (ColumnMetaData col : columns) {
			if (col.isColumn(field)) {
				return col;
			}
		}
		
		return null;
	}

	/**
	 * ���ظ���ĵ���
	 * @return
	 */
	public ColumnMetaData getGuideColumn() {
		return guideColumn;
	}
	
	/**
	 * �����һ��ռ�
	 */
	protected void applyFirstBlock() throws IOException {
		if (segmentBlockLink.isEmpty()) {
			segmentBlockLink.setFirstBlockPos(groupTable.applyNewBlock());
			ColumnMetaData []columns = this.columns;
			for (ColumnMetaData col : columns) {
				col.applySegmentFirstBlock();
			}
			
			for (ColumnMetaData col : columns) {
				col.applyDataFirstBlock();
			}
			
			if (parent != null) {
				guideColumn.applySegmentFirstBlock();
				guideColumn.applyDataFirstBlock();
			}
		}
	}
	
	/**
	 * ׼��д����׷�ӡ�ɾ�����޸�����ǰ���á�
	 * ���ú��Թؼ���Ϣ���б��ݣ���ֹдһ�������ʱ��������
	 */
	protected void prepareAppend() throws IOException {
		applyFirstBlock();
		
		segmentWriter = new BlockLinkWriter(segmentBlockLink, true);
		for (ColumnMetaData col : columns) {
			col.prepareWrite();
		}
		if (parent != null) {
			guideColumn.prepareWrite();
		}
	}
	
	/**
	 * ����д
	 */
	protected void finishAppend() throws IOException {
		segmentWriter.finishWrite();
		segmentWriter = null;
		for (ColumnMetaData col : columns) {
			col.finishWrite();
		}
		
		if (parent != null) {
			guideColumn.finishWrite();
		}
		
		groupTable.save();
		updateIndex();
	}
	
	/**
	 * ��ȡ��ͷ����
	 */
	public void readExternal(BufferReader reader) throws IOException {
		reader.read(reserve);
		tableName = reader.readUTF();
		colNames = reader.readStrings();
		dataBlockCount = reader.readInt32();
		totalRecordCount = reader.readLong40();
		segmentBlockLink.readExternal(reader);
		curModifyBlock = reader.readByte();
		modifyBlockLink1.readExternal(reader);
		modifyBlockLink2.readExternal(reader);
		
		int count = reader.readInt();
		columns = new ColumnMetaData[count];
		for (int i = 0; i < count; ++i) {
			columns[i] = new ColumnMetaData(this);
			columns[i].readExternal(reader, reserve[0]);
		}
		
		count = reader.readInt();
		if (count > 0) {
			maxValues = new Object[count];
			for (int i = 0; i < count; ++i) {
				maxValues[i] = reader.readObject();
			}
		}
	
		hasPrimaryKey = reader.readBoolean();
		isSorted = reader.readBoolean();
		boolean isPrimaryTable  = reader.readBoolean();
		if (!isPrimaryTable) {
			guideColumn = new ColumnMetaData(this);
			guideColumn.readExternal(reader, reserve[0]);
		}
		
		indexNames = reader.readStrings();
		if (indexNames == null) {
			indexFields = null;
			indexValueFields = null;
		} else {
			int indexCount = indexNames.length;
			indexFields = new String[indexCount][];
			for (int i = 0; i < indexCount; i++) {
				indexFields[i] = reader.readStrings();
			}
			indexValueFields = new String[indexCount][];
			for (int i = 0; i < indexCount; i++) {
				indexValueFields[i] = reader.readStrings();
			}
		}

		if (groupTable.reserve[0] > 2) {
			cuboids = reader.readStrings();//�汾3����
		}
		segmentCol = (String)reader.readObject();
		segmentSerialLen = reader.readInt();
		init();
		
		count = reader.readInt();
		tableList = new ArrayList<PhyTable>(count);
		for (int i = 0; i < count; ++i) {
			PhyTable table = new ColPhyTable(groupTable, this);
			table.readExternal(reader);
			tableList.add(table);
		}
	}
	
	/**
	 * д����ͷ����
	 */
	public void writeExternal(BufferWriter writer) throws IOException {
		reserve[0] = 5;
		writer.write(reserve);
		writer.writeUTF(tableName);
		writer.writeStrings(colNames);
		writer.writeInt32(dataBlockCount);
		writer.writeLong40(totalRecordCount);
		segmentBlockLink.writeExternal(writer);
		writer.writeByte(curModifyBlock);
		modifyBlockLink1.writeExternal(writer);
		modifyBlockLink2.writeExternal(writer);
		
		ColumnMetaData []columns = this.columns;
		int count = columns.length;
		writer.writeInt(count);
		for (int i = 0; i < count; ++i) {
			columns[i].writeExternal(writer);
		}
		
		if (maxValues == null) {
			writer.writeInt(0);
		} else {
			writer.writeInt(maxValues.length);
			for (Object val : maxValues) {
				writer.writeObject(val);
			}
			
			writer.flush();
		}
		
		writer.writeBoolean(hasPrimaryKey);
		writer.writeBoolean(isSorted);
		writer.writeBoolean(parent == null);
		if (parent != null) {
			guideColumn.writeExternal(writer);
		}
		
		writer.writeStrings(indexNames);
		if (indexNames != null) {
			for (int i = 0, indexCount = indexNames.length; i < indexCount; i++) {
				writer.writeStrings(indexFields[i]);
			}
			for (int i = 0, indexCount = indexNames.length; i < indexCount; i++) {
				writer.writeStrings(indexValueFields[i]);
			}
		}

		writer.writeStrings(cuboids);//�汾3����
		
		writer.writeObject(segmentCol);
		writer.flush();
		writer.writeInt(segmentSerialLen);
		
		ArrayList<PhyTable> tableList = this.tableList;
		count = tableList.size();
		writer.writeInt(count);
		for (int i = 0; i < count; ++i) {
			tableList.get(i).writeExternal(writer);
		}
	}
	
	/**
	 * ׷�Ӹ����һ������
	 * @param data
	 * @param start ��ʼ����
	 * @param recList ��������
	 * @throws IOException
	 */
	private void appendAttachedDataBlock(Sequence data, boolean []isMyCol, LongArray recList) throws IOException {
		ColumnMetaData []columns = this.allColumns;
		int count = columns.length;

		Object []minValues = new Object[count];;//һ�����Сάֵ
		Object []maxValues = new Object[count];;//һ������άֵ
		Object []startValues = new Object[count];
		int[] dataTypeInfo = new int[count];
				
		BufferWriter bufferWriter = guideColumn.getColDataBufferWriter();
		BufferWriter bufferWriters[] = new BufferWriter[count];
		
		DataBlockWriterJob[] jobs = new DataBlockWriterJob[count];
		ThreadPool pool = ThreadPool.newInstance(count);
		
		int end = data.length();
		try {
			//д����
			bufferWriter.write(DataBlockType.LONG);
			for (int i = 1; i <= end; ++i) {
				bufferWriter.writeLong(recList.getLong(i));
			}
			bufferWriter.writeBoolean(false);
			
			//д����������
			for (int i = 0; i < count; i++) {
				if (!isMyCol[i])
					continue;
				bufferWriters[i] = columns[i].getColDataBufferWriter();
				Sequence dict = columns[i].getDict();
				jobs[i] = new DataBlockWriterJob(bufferWriters[i], data, dict, i, 1, end, 
						maxValues, minValues, startValues, dataTypeInfo);
				pool.submit(jobs[i]);
			}
			
			for (int i = 0; i < count; ++i) {
				if (!isMyCol[i]) continue;
				jobs[i].join();
			}
		} finally {
			pool.shutdown();
		}
		
		//ͳ������������
		boolean doCheck = groupTable.isCheckDataPure();
		for (int j = 0; j < count; j++) {
			if (!isMyCol[j]) continue;
			columns[j].adjustDataType(dataTypeInfo[j], doCheck);
			columns[j].initDictArray();
		}

		if (recList.size() == 0) {
			//����ǿտ飬�����дһ��null
			bufferWriter.writeObject(null);
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				bufferWriters[j].writeObject(null);
			}
		}
		
		guideColumn.appendColBlock(bufferWriter.finish());

		//�ύÿ���п�buffer
		for (int j = 0; j < count; j++) {
			if (!isMyCol[j]) continue;
			columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
		}
		
		//���·ֶ���Ϣbuffer
		appendSegmentBlock(end);
	}
	
	/**
	 * ׷�Ӹ����һ������(�ɸ�ʽ)
	 * @param data
	 * @param start ��ʼ����
	 * @param recList ��������
	 * @throws IOException
	 */
	public void appendAttachedDataBlockV3(Sequence data, boolean []isMyCol, LongArray recList) throws IOException {
		BaseRecord r;
		ColumnMetaData []columns = this.allColumns;
		int count = columns.length;
		int []serialBytesLen = new int[count];
		Object []minValues = null;//һ�����Сάֵ
		Object []maxValues = null;//һ������άֵ
		Object []startValues = null;
		
		if (sortedColumns != null) {
			minValues = new Object[count];
			maxValues = new Object[count];
			startValues = new Object[count];
		}
		
		BufferWriter bufferWriter = guideColumn.getColDataBufferWriter();
		BufferWriter bufferWriters[] = new BufferWriter[count];
		for (int i = 0; i < count; i++) {
			if (!isMyCol[i]) continue;
			serialBytesLen[i] = columns[i].getSerialBytesLen();
			bufferWriters[i] = columns[i].getColDataBufferWriter();
		}
		
		int end = data.length();
		for (int i = 1; i <= end; ++i) {
			//д����
			bufferWriter.writeObject(recList.get(i - 1));
			
			r = (BaseRecord) data.get(i);
			Object[] vals = r.getFieldValues();
			//��һ��д�����е�buffer
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				Object obj = vals[j];
				if (serialBytesLen[j] > 0) {
					if (obj instanceof SerialBytes) {
						bufferWriters[j].writeObject(obj);
					} else {
						Long val;
						if (obj instanceof Integer) {
							val = (Integer)obj & 0xFFFFFFFFL;
						} else {
							val = (Long) obj;
						}
						bufferWriters[j].writeObject(new SerialBytes(val, serialBytesLen[j]));
					}
				} else {
					bufferWriters[j].writeObject(obj);
				}
			}
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				Object obj = vals[j];
				if (columns[j].isDim()) {
					if (Variant.compare(obj, maxValues[j], true) > 0)
						maxValues[j] = obj;
					if (i == 1)
					{
						minValues[j] = obj;//��һ��Ҫ��ֵ����Ϊnull��ʾ��С
						startValues[j] = obj;
					}
					if (Variant.compare(obj, minValues[j], true) < 0)
						minValues[j] = obj;
				}
			}
		}

		if (recList.size() == 0) {
			//����ǿտ飬�����дһ��null
			bufferWriter.writeObject(null);
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				bufferWriters[j].writeObject(null);
			}
		}
		
		guideColumn.appendColBlock(bufferWriter.finish());
		
		if (sortedColumns == null) {
			//�ύÿ���п�buffer
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				columns[j].appendColBlock(bufferWriters[j].finish());
			}
			//���·ֶ���Ϣbuffer
			appendSegmentBlock(end);
			return;
		}

		//�ύÿ���п�buffer
		for (int j = 0; j < count; j++) {
			if (!isMyCol[j]) continue;
			if (!columns[j].isDim()) {
				//׷���п�
				columns[j].appendColBlock(bufferWriters[j].finish());
			} else {
				//׷��ά��
				columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
			}
		}
		
		//���·ֶ���Ϣbuffer
		appendSegmentBlock(end);
	}
	
	/**
	 * ��data���е�ָ����Χ������д��(�¸�ʽ)
	 * @param data ��������
	 * @param start ��ʼλ��
	 * @param end ����λ��
	 * @throws IOException
	 */
	private void appendDataBlock(Sequence data, int start, int end) throws IOException {
		ColumnMetaData []columns = this.columns;
		int count = columns.length;
		Object []minValues = new Object[count];//һ�����Сάֵ
		Object []maxValues = new Object[count];//һ������άֵ
		Object []startValues = new Object[count];
		int[] dataTypeInfo = new int[count];

		BufferWriter bufferWriters[] = new BufferWriter[count];
		DataBlockWriterJob[] jobs = new DataBlockWriterJob[count];
		ThreadPool pool = ThreadPool.newInstance(count);
		
		try {
			for (int i = 0; i < count; i++) {
				bufferWriters[i] = columns[i].getColDataBufferWriter();
				Sequence dict = columns[i].getDict();
				jobs[i] = new DataBlockWriterJob(bufferWriters[i], data, dict, i, start, end, 
						maxValues, minValues, startValues, dataTypeInfo);
				pool.submit(jobs[i]);
			}
			
			for (int i = 0; i < count; ++i) {
				jobs[i].join();
			}
		} finally {
			pool.shutdown();
		}
		
		//ͳ������������
		boolean doCheck = groupTable.isCheckDataPure();
		for (int j = 0; j < count; j++) {
			columns[j].adjustDataType(dataTypeInfo[j], doCheck);
			columns[j].initDictArray();
		}
		
		//�ύÿ���п�buffer
		for (int j = 0; j < count; j++) {
			columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
		}
		
		//���·ֶ���Ϣbuffer
		appendSegmentBlock(end - start + 1);
	}
	
	/**
	 * ��data���е�ָ����Χ������д��(�ɸ�ʽ)
	 * @param data ��������
	 * @param start ��ʼλ��
	 * @param end ����λ��
	 * @throws IOException
	 */
	public void appendDataBlockV3(Sequence data, int start, int end) throws IOException {
		BaseRecord r;
		ColumnMetaData []columns = this.columns;
		int count = columns.length;
		int []serialBytesLen = new int[count];
		Object []minValues = null;//һ�����Сάֵ
		Object []maxValues = null;//һ������άֵ
		Object []startValues = null;
		
		if (sortedColumns != null) {
			minValues = new Object[count];
			maxValues = new Object[count];
			startValues = new Object[count];
		}
		
		BufferWriter bufferWriters[] = new BufferWriter[count];
		for (int i = 0; i < count; i++) {
			serialBytesLen[i] = columns[i].getSerialBytesLen();
			bufferWriters[i] = columns[i].getColDataBufferWriter();
		}
		
		IArray mems = data.getMems();
		for (int i = start; i <= end; ++i) {
			r = (BaseRecord) mems.get(i);
			mems.set(i, null);
			
			Object[] vals = r.getFieldValues();
			//��һ��д�����е�buffer
			for (int j = 0; j < count; j++) {
				Object obj = vals[j];
				if (serialBytesLen[j] > 0) {
					if (obj instanceof SerialBytes) {
						if (((SerialBytes)obj).length() > serialBytesLen[j]) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.indexOutofBound"));
						}
						bufferWriters[j].writeObject(obj);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.needSerialBytes"));
					}
				} else {
					bufferWriters[j].writeObject(obj);
				}
				if (columns[j].isDim()) {
					if (Variant.compare(obj, maxValues[j], true) > 0)
						maxValues[j] = obj;
					if (i == start) {
						minValues[j] = obj;//��һ��Ҫ��ֵ����Ϊnull��ʾ��С
						startValues[j] = obj;
					}
					if (Variant.compare(obj, minValues[j], true) < 0)
						minValues[j] = obj;
				}
			}
		}
		
		if (sortedColumns == null) {
			//�ύÿ���п�buffer
			for (int j = 0; j < count; j++) {
				columns[j].appendColBlock(bufferWriters[j].finish());
			}
			//���·ֶ���Ϣbuffer
			appendSegmentBlock(end - start + 1);
			return;
		}

		//�ύÿ���п�buffer
		for (int j = 0; j < count; j++) {
			if (!columns[j].isDim()) {
				//׷���п�
				columns[j].appendColBlock(bufferWriters[j].finish());
			} else {
				//׷��ά��
				columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
			}
		}
		
		//���·ֶ���Ϣbuffer
		appendSegmentBlock(end - start + 1);
	}
	
	/**
	 * ���α������д��
	 * @param cursor
	 * @throws IOException
	 */
	private void appendNormal(ICursor cursor) throws IOException {
		Sequence data = cursor.fetch(MIN_BLOCK_RECORD_COUNT);
		while (data != null && data.length() > 0) {
			appendDataBlock(data, 1, data.length());
			data = cursor.fetch(MIN_BLOCK_RECORD_COUNT);
		}
	}
	
	/**
	 * ���α������д�� ������
	 * @param cursor
	 * @throws IOException
	 */
	private void appendAttached(ICursor cursor) throws IOException {
		PhyTable primaryTable = parent;
		int pBlockCount = primaryTable.getDataBlockCount();//����������ܿ���
		int curBlockCount = dataBlockCount;//Ҫ׷�ӵĿ�ʼ���
		int pkeyEndIndex = sortedColStartIndex;
		
		String []primaryTableKeys = primaryTable.getSortedColNames();
		ArrayList<String> primaryTableKeyList = new ArrayList<String>();
		for (String name : primaryTableKeys) {
			primaryTableKeyList.add(name);
		}
		String []colNames = getAllColNames();
		int fcount = colNames.length;
		boolean []isMyCol = new boolean[fcount];
		for (int i = 0; i < fcount; i++) {
			if (primaryTableKeyList.contains(colNames[i])) {
				isMyCol[i] = false;
			} else {
				isMyCol[i] = true;
			}
		}
		
		Cursor cs;
		if (primaryTable.totalRecordCount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.baseTableNull"));
		}
		cs = (Cursor) primaryTable.cursor(primaryTableKeys);
		cs.setSegment(curBlockCount, curBlockCount + 1);
		Sequence pkeyData = cs.fetch(ICursor.MAXSIZE);
		int pkeyIndex = 1;
		int pkeyDataLen = pkeyData.length();
		ComTableRecord curPkey = (ComTableRecord) pkeyData.get(1);
		Object []curPkeyVals = curPkey.getFieldValues();
		
		int sortedColCount = allSortedColumns.length;
		Object []tableMaxValues = this.maxValues;
		Object []lastValues = new Object[sortedColCount];//��һ��ά��ֵ
		
		LongArray guideCol = new LongArray(MIN_BLOCK_RECORD_COUNT);
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		BaseRecord r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();
		
		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (BaseRecord) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}
				
				//�ұ�����������Ӧ�ļ�¼
				while (true) {
					int cmp = Variant.compareArrays(curPkeyVals, vals, pkeyEndIndex);
					if (cmp == 0) {
						break;
					} else if (cmp < 0) {
						pkeyIndex++;
						if (pkeyIndex > pkeyDataLen) {
							//ע�⣺��ʱ�п���seq��û�м�¼�����Ҫ׷��һ���տ�
							//������һ���˾��ύһ��
							appendAttachedDataBlock(seq, isMyCol, guideCol);
							seq.clear();
							guideCol = new LongArray(MIN_BLOCK_RECORD_COUNT);
							
							//ȡ��һ����������
							curBlockCount++;
							if (curBlockCount >= pBlockCount) {
								//����ȡ������ˣ������ﲻӦ�û������ݣ����쳣
								MessageManager mm = EngineMessage.get();
								throw new RQException(mm.getMessage("dw.appendNotMatch") + r.toString(null));
							}
							cs = (Cursor) primaryTable.cursor(primaryTableKeys);
							cs.setSegment(curBlockCount, curBlockCount + 1);
							pkeyData = cs.fetch(ICursor.MAXSIZE);
							pkeyIndex = 1;
							pkeyDataLen = pkeyData.length();
						}
						curPkey = (ComTableRecord) pkeyData.get(pkeyIndex);
						curPkeyVals = curPkey.getFieldValues();
					} else if (cmp > 0) {
						//û�ҵ���Ӧ�������¼�����쳣
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.appendNotMatch") + r.toString(null));
					}
				}
				
				//���������ȷ��Ҫ׷��һ����
				guideCol.add(curPkey.getRecordSeq());//��ӵ���
				
				//��������������
				if (isSorted) {
					if (tableMaxValues != null) {
						int cmp = Variant.compareArrays(vals, tableMaxValues, sortedColCount);
						if (cmp < 0) {
							//����׷�ӵ����ݱ�������
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("dw.appendAttachedTable"));
						} else if (cmp == 0){
							if (hasPrimaryKey) hasPrimaryKey = false;
						} else {
							System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
						}
					} else {
						tableMaxValues = maxValues = new Object[sortedColCount];
						System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
					}
				}
				
				seq.add(r);//�������ݴ�				
				System.arraycopy(vals, 0, lastValues, 0, sortedColCount);//����һ��άֵ				
			}
			data = cursor.fetch(ICursor.FETCHCOUNT);
		}
		
		//�������һ���п� (������ȡ������ˣ������ﻹ�еĻ����Ͳ�����)
		if (seq.length() > 0) {
			appendAttachedDataBlock(seq, isMyCol, guideCol);
		}
		
	}
	
	/**
	 * ���α������д����д��ʱ��Ҫ���зֶΡ�
	 * @param cursor
	 * @throws IOException
	 */
	private void appendSegment(ICursor cursor) throws IOException {
		int recCount = 0;
		int sortedColCount = sortedColumns.length;
		Object []tableMaxValues = this.maxValues;

		String segmentCol = getSegmentCol();
		int segmentSerialLen = getSegmentSerialLen();
		int segmentIndex = 0;

		for (int i = 0; i < sortedColCount; i++) {
			if (segmentCol.equals(sortedColumns[i].getColName())) {
				segmentIndex = i;
				break;
			}
		}
		int cmpLen = segmentIndex + 1;
		int serialBytesLen = sortedColumns[segmentIndex].getSerialBytesLen();
		if (segmentSerialLen == 0 || segmentSerialLen > serialBytesLen) {
			segmentSerialLen = serialBytesLen;
		}
		Object []lastValues = new Object[cmpLen];//��һ��ά��ֵ
		Object []curValues = new Object[cmpLen];//��ǰ��ά��ֵ
		
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		BaseRecord r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();
		
		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (BaseRecord) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}

				//�����ж��Ƿ�һ���п���
				if (recCount >= MIN_BLOCK_RECORD_COUNT){
					System.arraycopy(vals, 0, curValues, 0, cmpLen);
					if (0 != Variant.compareArrays(lastValues, curValues, cmpLen)) {
						appendDataBlock(seq, 1, seq.length());
						seq.clear();
						recCount = 0;
					}
				}
				
				//��������������
				if (isSorted) {
					if (tableMaxValues != null) {
						int cmp = Variant.compareArrays(vals, tableMaxValues, sortedColCount);
						if (cmp < 0) {
							//hasPrimaryKey = false;//���ٴ�������
							isSorted = false;
							maxValues = null;
						} else if (cmp == 0){
							//if (hasPrimaryKey) hasPrimaryKey = false;//���ٴ�������
						} else {
							System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
						}
					} else {
						tableMaxValues = maxValues = new Object[sortedColCount];
						System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
					}
					if (tableList.size() > 0 && !hasPrimaryKey) {
						//���ڸ���ʱ������׷�ӵ����ݱ�������Ψһ
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.appendPrimaryTable"));
					}
				}
				
				seq.add(r);//�������ݴ�
				System.arraycopy(vals, 0, lastValues, 0, cmpLen);
				recCount++;
			}
			data = cursor.fetch(ICursor.FETCHCOUNT);
		}
		
		//�������һ���п�
		if (seq.length() > 0) {
			appendDataBlock(seq, 1, seq.length());
		}
		
	}
	
	/**
	 * ���α������д����д��ʱ��Ҫ�ж������Ƿ��ά����
	 * @param cursor
	 * @throws IOException
	 */
	private void appendSorted(ICursor cursor) throws IOException {
		int recCount = 0;
		int sortedColCount = sortedColumns.length;
		Object []tableMaxValues = this.maxValues;
		Object []lastValues = new Object[sortedColCount];//��һ��ά��ֵ
		
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		BaseRecord r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();

		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (BaseRecord) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}

				//�����ж��Ƿ�һ���п���
				if (recCount >= MAX_BLOCK_RECORD_COUNT) {
					//��ʱ�ύһ��
					appendDataBlock(seq, 1, MAX_BLOCK_RECORD_COUNT/2);
					seq = (Sequence) seq.get(MAX_BLOCK_RECORD_COUNT/2 + 1, seq.length() + 1);
					recCount = seq.length(); 
				} else if (recCount >= MIN_BLOCK_RECORD_COUNT){
					boolean doAppend = false;
					if (0 != Variant.compareArrays(lastValues, vals, sortedColCount)) {
						doAppend = true;
					}
					if (doAppend) {
						appendDataBlock(seq, 1, seq.length());
						seq.clear();
						recCount = 0;
					}
				}
				
				//��������������
				if (isSorted) {
					if (tableMaxValues != null) {
						int cmp = Variant.compareArrays(vals, tableMaxValues, sortedColCount);
						if (cmp < 0) {
							hasPrimaryKey = false;
							isSorted = false;
							maxValues = null;
						} else if (cmp == 0){
							if (hasPrimaryKey) {
								hasPrimaryKey = false;
							}
						} else {
							System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
						}
						if (tableList.size() > 0 && !hasPrimaryKey) {
							//���ڸ���ʱ������׷�ӵ����ݱ�������Ψһ
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("dw.appendPrimaryTable"));
						}
					} else {
						tableMaxValues = maxValues = new Object[sortedColCount];
						System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
					}
				}
				
				seq.add(r);//�������ݴ�				
				System.arraycopy(vals, 0, lastValues, 0, sortedColCount);//����һ��άֵ				
				recCount++;
			}
			data = cursor.fetch(ICursor.FETCHCOUNT);
		}
		
		//�������һ���п�
		if (seq.length() > 0) {
			appendDataBlock(seq, 1, seq.length());
		}
		
	}
	
	private void mergeAppend(ICursor cursor, String opt) throws IOException {
		// ��֧�ִ���������鲢׷��
		if (!isSingleTable()) {
			throw new RQException("'append@m' is unimplemented in annex table!");
		}
		
		// ������ݽṹ�Ƿ����
		Sequence data = cursor.peek(ICursor.FETCHCOUNT);		
		if (data == null || data.length() <= 0) {
			return;
		}
		
		//�жϽṹƥ��
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		ColumnMetaData []columns = this.columns;
		int colCount = columns.length;
		if (colCount != ds.getFieldCount()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		for (int i = 0; i < colCount; i++) {
			if (!ds.getFieldName(i).equals(columns[i].getColName())) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.dsNotMatch"));
			}
		}
		
		// �鲢���������ȱ��浽��ʱ�ļ�
		ColComTable groupTable = (ColComTable)getGroupTable();
		File srcFile = groupTable.getFile();
		File tmpFile = File.createTempFile("tmpdata", "", srcFile.getParentFile());
		ColComTable tmpGroupTable = null;
		
		try {
			Context ctx = new Context();
			tmpGroupTable = new ColComTable(tmpFile, groupTable);
			
			PhyTable baseTable = tmpGroupTable.getBaseTable();
			
			int dcount = sortedColumns.length;
			Expression []mergeExps = new Expression[dcount];
			for (int i = 0; i < dcount; ++i) {
				mergeExps[i] = new Expression(sortedColumns[i].getColName());
			}
			
			// ���鲢
			Cursor srcCursor = new Cursor(this);
			ICursor []cursors = new ICursor[]{srcCursor, cursor};
			MergesCursor mergeCursor = new MergesCursor(cursors, mergeExps, ctx);
			String[] indexNames = baseTable.indexNames;
			String[] cuboids = baseTable.cuboids;
			baseTable.deleteIndex(null);//��ʱ�ļ�����Ҫ��appendʱ����index��cuboid
			baseTable.deleteCuboid(null);
			baseTable.append(mergeCursor);
			baseTable.appendCache();
			baseTable.indexNames = indexNames;
			baseTable.cuboids = cuboids;
			tmpGroupTable.save();
			baseTable.close();
			
			// �رղ�ɾ������ļ�������ʱ�ļ�������Ϊ����ļ���
			groupTable.raf.close();
			if (groupTable.file.delete()) {
				tmpFile.renameTo(groupTable.file);
			} else {
				tmpFile.delete();
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.needCloseTable"));
			}
			
			// ���´����
			groupTable.reopen();
			groupTable.baseTable.resetIndex(ctx);
			groupTable.baseTable.resetCuboid(ctx);
		} finally {
			if (tmpGroupTable != null) {
				tmpGroupTable.raf.close();
			}
		}
	}
	
	/**
	 * �Թ鲢��ʽ׷��(�ݲ�֧���и�������)
	 */
	public void append(ICursor cursor, String opt) throws IOException {
		if (opt != null && opt.indexOf('w') != -1) {
			int []keys = getDataStruct().getPKIndex();
			int deleteField = this.getDeleteFieldIndex(null, null);
			cursor = new UpdateIdCursor(cursor, keys, deleteField);
		}
		
		if (isSorted && opt != null) {
			if (opt.indexOf('y') != -1) {
				Sequence data = cursor.fetch();
				ColPhyTable ctmd = (ColPhyTable)getSupplementTable(false);
				if (ctmd == null) {
					append_y(data);
				} else {
					ctmd.append_y(data);
				}
			} else if (opt.indexOf('a') != -1) {
				ColPhyTable ctmd = (ColPhyTable)getSupplementTable(true);
				ctmd.mergeAppend(cursor, opt);
			} else if (opt.indexOf('m') != -1) {
				mergeAppend(cursor, opt);
			} else {
				append(cursor);
				if (opt.indexOf('i') != -1) {
					appendCache();
				}
			}
		} else if (opt != null) {
			if (opt.indexOf('y') != -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			append(cursor);
			if (opt.indexOf('i') != -1) {
				appendCache();
			}
		} else {
			append(cursor);
		}
	}

	/**
	 * ׷���α�����ݵ����
	 * @param cursor
	 */
	public void append(ICursor cursor) throws IOException {
		getGroupTable().checkWritable();
		
		// ���û��ά�ֶ���ȡGroupTable.MIN_BLOCK_RECORD_COUNT����¼
		// ���������3��ά�ֶ�d1��d2��d3������ά�ֶε�ֵȡ������MIN_BLOCK_RECORD_COUNT����¼
		// ���[d1,d2,d3]��������Ҫ��[d1,d2]ֵ��ͬ�ĸ������������֮��Ҫ��[d1,d2,d3]ֵ��ͬ�Ĳ���������
		// �����ͬ�ĳ�����MAX_BLOCK_RECORD_COUNT������MAX_BLOCK_RECORD_COUNT / 2��Ϊһ��
		// ��ÿһ�е�����д��BufferWriterȻ�����finish�õ��ֽ����飬�ٵ���compressѹ�����ݣ����д��ColumnMetaData
		// ��ά�ֶ�ʱҪ����maxValues��hasPrimaryKey������Ա�����hasPrimaryKeyΪfalse���ٸ���
		if (cursor == null) {
			return;
		}

		Sequence data = cursor.peek(MIN_BLOCK_RECORD_COUNT);		
		if (data == null || data.length() <= 0) {
			return;
		}
		
		//�жϽṹƥ��
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		ColumnMetaData []allColumns;
		if (parent == null) {
			allColumns = columns;
		} else {
			allColumns = this.allColumns;
		}
		int count = allColumns.length;
		for (int i = 0; i < count; i++) {
			if (!ds.getFieldName(i).equals(allColumns[i].getColName())) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.dsNotMatch"));
			}
		}
		
		//����α����ݲ���1��
		if (data.length() < MIN_BLOCK_RECORD_COUNT) {
			if (appendCache == null) {
				appendCache = data;
			} else {
				appendCache.addAll(data);
			}
			data = null;
			cursor.close();
			if (appendCache.length() >= MIN_BLOCK_RECORD_COUNT) {
				appendCache();
			}
			return;
		}
		
		//����л�������
		if (appendCache != null) {
			ICursor []cursorArray = new ICursor[2];
			cursorArray[0] = new MemoryCursor(appendCache);
			cursorArray[1] = cursor;
			cursor = new ConjxCursor(cursorArray);
			appendCache = null;
		}
		
		// ׼��д����
		prepareAppend();
		
		if (parent != null) {
			parent.appendCache();
			appendAttached(cursor);
		} else if (sortedColumns == null) {
			appendNormal(cursor);
		} else if (getSegmentCol() == null) {
			appendSorted(cursor);
		} else {
			appendSegment(cursor);
		}
		
		// ����д���ݣ����浽�ļ�
		finishAppend();
	}
	
	protected void appendSegmentBlock(int recordCount) throws IOException {
		dataBlockCount++;//���ݿ�
		totalRecordCount += recordCount;//�ܼ�¼��
		segmentWriter.writeInt32(recordCount);//
	}
	
	/**
	 * ȡ�ֶι������ȼ�
	 * @param col
	 * @return
	 */
	public int getColumnFilterPriority(ColumnMetaData col) {
		if (sortedColumns != null) {
			int len = sortedColumns.length;
			for (int i = 0; i < len; ++i) {
				if (sortedColumns[i] == col) {
					return i;
				}
			}
			
			return len;
		} else {
			return 0;
		}
	}
	
	/**
	 * �������������α�
	 */
	public ICursor cursor() {
		ComTable groupTable = getGroupTable();
		groupTable.checkReadable();
		
		ICursor cs;
		if (parent != null) {
			cs = JoinTableCursor.createAnnexCursor(this);
		} else {
			cs = new Cursor(this);
		}
		
		PhyTable tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor();
			return merge(cs, cs2);
		}
	}

	/**
	 * �������������α�
	 * @param exps ȡ���ֶα��ʽ����expsΪnullʱ����fieldsȡ����
	 * @param fields ȡ���ֶε�������
	 * @param filter ���˱��ʽ
	 * @param fkNames ָ��FK���˵��ֶ�����
	 * @param codes ָ��FK���˵���������
	 * @param opts �����ֶν��й�����ѡ��
	 * @param opt ѡ��
	 * @param ctx ������
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, String opt, Context ctx) {
		ComTable groupTable = getGroupTable();
		groupTable.checkReadable();
		
		ICursor cs = JoinTableCursor.createAnnexCursor(this, exps, fields, filter, fkNames, codes, ctx);
		if (cs == null) {
			cs = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
		}
		
		PhyTable tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
			return merge(cs, cs2);
		}
	}

	public IFilter getFirstDimFilter(Expression exp, Context ctx) {
		Object obj = Cursor.parseFilter(this, exp, ctx);
		if (obj instanceof IFilter) {
			ColumnMetaData firstDim = getSortedColumns()[0];
			IFilter filter = (IFilter)obj;
			if (!filter.isMultiFieldOr() && filter.getColumn() == firstDim) {
				return filter;
			} else {
				return null;
			}
		} else if (obj instanceof ArrayList) {
			ColumnMetaData firstDim = getSortedColumns()[0];
			@SuppressWarnings("unchecked")
			ArrayList<Object> list = (ArrayList<Object>)obj;
			for (Object f : list) {
				if (f instanceof IFilter) {
					IFilter filter = (IFilter)f;
					if (!filter.isMultiFieldOr() && filter.getColumn() == firstDim) {
						return filter;
					}
				}
			}
		}

		return null;
	}
	
	/**
	 * �ѱ��ʽ���н��в��
	 * @param exp ���ʽ
	 * @param ctx ����������
	 * @return
	 */
	public IFilter[] getSortedFieldFilters(Expression exp, Context ctx) {
		Object obj = Cursor.parseFilter(this, exp, ctx);
		if (obj instanceof IFilter) {
			IFilter filter = (IFilter)obj;
			if (filter.isMultiFieldOr()) {
				return null;
			}
			
			ColumnMetaData column = filter.getColumn();
			if (column == null || !column.hasMaxMinValues()) {
				return null;
			} else {
				return new IFilter[] {filter};
			}
		} else if (obj instanceof ArrayList) {
			ArrayList<Object> list = (ArrayList<Object>)obj;
			ArrayList<IFilter> filterList = new ArrayList<IFilter>();
			
			for (Object f : list) {
				if (f instanceof IFilter) {
					IFilter filter = (IFilter)f;
					if (!filter.isMultiFieldOr()) {
						ColumnMetaData column = filter.getColumn();
						if (column != null && column.hasMaxMinValues()) {
							filterList.add(filter);
						}
					}
				}
			}
			
			if (filterList.size() > 0) {
				IFilter []filters = new IFilter[filterList.size()];
				filterList.toArray(filters);
				Arrays.sort(filters);
				return filters;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * �����������Ķ�·�α�
	 * @param exps ȡ���ֶα��ʽ������expsΪnullʱ����fieldsȡ����
	 * @param fields ȡ���ֶε�������
	 * @param filter ���˱��ʽ
	 * @param fkNames ָ��FK���˵��ֶ�����
	 * @param codes ָ��FK���˵���������
	 * @param pathCount ·��
	 * @param opt ѡ��
	 * @param ctx ������
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes, String []opts, int pathCount, String opt, Context ctx) {
		if (pathCount < 2) {
			return cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
		}
		
		PhyTable tmd = getSupplementTable(false);
		int blockCount = getDataBlockCount();
		if (blockCount == 0) {
			if (tmd == null) {
				return new MemoryCursor(null);
			} else {
				return tmd.cursor(exps, fields, filter, fkNames, codes, opts, pathCount, opt, ctx);
			}
		}
		
		// �������������ά�ֶΣ����ҳ����˱��ʽ�й��ڵ�һ��ά������
		// �õ�һ��ά���������˳����������Ŀ��ٲ�ֳɶ�·�α�
		//IFilter dimFilter = null;
		//if (filter != null && parent == null && getSortedColumns() != null) {
		//	dimFilter = getFirstDimFilter(filter, ctx);
		//}
		
		// �Ȱ������ֶ��ҳ��������������ݿ飬�ٽ��зֶ�
		IFilter []filters = null;
		if (filter != null && parent == null && (opt == null || opt.indexOf('w') == -1)) {
			filters = getSortedFieldFilters(filter, ctx);
		}
		
		ICursor []cursors;
		if (filters == null) {
			int avg = blockCount / pathCount;
			if (avg < 1) {
				avg = 1;
				pathCount = blockCount;
			}
			
			// ǰ��Ŀ�ÿ�ζ�һ��
			int mod = blockCount % pathCount;
			cursors = new ICursor[pathCount];
			int start = 0;
			for (int i = 0; i < pathCount; ++i) {
				int end = start + avg;
				if (i < mod) {
					end++;
				}
				
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}
				
				ICursor cursor = JoinTableCursor.createAnnexCursor(this, exps, fields, filter, fkNames, codes, ctx);
				if (cursor == null) {
					cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
				}
				
				if (cursor instanceof Cursor) {
					((Cursor) cursor).setSegment(start, end);
				} else {
					((JoinTableCursor) cursor).setSegment(start, end);
				}

				cursors[i] = cursor;
				start = end;
			}
		} else {
			IntArrayList list = new IntArrayList();
			int filterCount = filters.length;
			ObjectReader []readers = new ObjectReader[filterCount];
			
			for (int f = 0; f < filterCount; ++f) {
				ColumnMetaData column = filters[f].getColumn();
				readers[f] = column.getSegmentReader();
			}
			
			try {
				for (int i = 0; i < blockCount; ++i) {
					boolean match = true;
					for (int f = 0; f < filterCount; ++f) {
						readers[f].readLong40();
						Object minValue = readers[f].readObject();
						Object maxValue = readers[f].readObject();
						readers[f].skipObject();
						
						if (match && !filters[f].match(minValue, maxValue)) {
							match = false;
						}
					}
					
					if (match) {
						list.addInt(i);
					}
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			blockCount = list.size();
			if (blockCount == 0) {
				return new MemoryCursor(null);
			}
			
			int avg = blockCount / pathCount;
			if (avg < 1) {
				// ÿ�β���һ��
				cursors = new ICursor[blockCount];
				for (int i = 0; i < blockCount; ++i) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
					Cursor cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
					int b = list.getInt(i);
					cursor.setSegment(b, b + 1);
					cursors[i] = cursor;
				}
			} else {
				// ǰ��Ŀ�ÿ�ζ�һ��
				int mod = blockCount % pathCount;
				cursors = new ICursor[pathCount];
				int start = 0;
				for (int i = 0; i < pathCount; ++i) {
					int end = start + avg;
					if (i < mod) {
						end++;
					}
					
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
					Cursor cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
					cursor.setSegment(list.getInt(start), list.getInt(end - 1) + 1);
					cursors[i] = cursor;
					start = end;
				}
			}
		}
		
		MultipathCursors mcs = new MultipathCursors(cursors, ctx);
		if (tmd == null) {
			return mcs;
		}
		
		String []sortFields = ((IDWCursor)cursors[0]).getSortFields();
		if (sortFields != null) {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, mcs, null, ctx);
			return merge(mcs, (MultipathCursors)cs2, sortFields);
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, pathCount, opt, ctx);
			return conj(mcs, cs2);
		}
	}

	/**
	 * ���طֶ��α꣬�ѻ����ΪsegCount�Σ����ص�segSeq�ε�����
	 * @param exps ȡ���ֶα��ʽ������expsΪnullʱ����fieldsȡ����
	 * @param fields ȡ���ֶε�������
	 * @param filter ���˱��ʽ
	 * @param fkNames ָ��FK���˵��ֶ�����
	 * @param codes ָ��FK���˵���������
	 * @param segSeq �ڼ���
	 * @param segCount  �ֶ���
	 * @param opt ѡ��
	 * @param ctx ������
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes, String []opts, int segSeq, int segCount, String opt, Context ctx) {
		getGroupTable().checkReadable();
		
		if (filter != null) {
			// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
			filter = filter.newExpression(ctx);
		}
		
		IDWCursor cursor = (IDWCursor)JoinTableCursor.createAnnexCursor(this, exps, fields, filter, fkNames, codes, ctx);
		if (cursor == null) {
			cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
		}
		
		if (segCount < 2) {
			return cursor;
		}
		
		int startBlock = 0;
		int endBlock = -1;
		int avg = dataBlockCount / segCount;
		
		if (avg < 1) {
			// ÿ�β���һ��
			if (segSeq <= dataBlockCount) {
				startBlock = segSeq - 1;
				endBlock = segSeq;
			}
		} else {
			if (segSeq > 1) {
				endBlock = segSeq * avg;
				startBlock = endBlock - avg;
				
				// ʣ��Ŀ�����ÿ�ζ�һ��
				int mod = dataBlockCount % segCount;
				int n = mod - (segCount - segSeq);
				if (n > 0) {
					endBlock += n;
					startBlock += n - 1;
				}
			} else {
				endBlock = avg;
			}
		}

		cursor.setSegment(startBlock, endBlock);
		return cursor;
	}
	
	public static Sequence fetchToValue(IDWCursor cursor, String []names, Object []vals) {
		// ֻȡ��һ��ļ�¼�������һ��û�����������ľͷ���
		Sequence seq = cursor.getStartBlockData(ICursor.FETCHCOUNT);
		if (seq == null || seq.length() == 0) {
			return null;
		}
		
		int fcount = names.length;
		int []findex = new int[fcount];
		DataStruct ds = ((BaseRecord)seq.getMem(1)).dataStruct();
		for (int f = 0; f < fcount; ++f) {
			findex[f] = ds.getFieldIndex(names[f]);
			if (findex[f] == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(names[f] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		Sequence result = null;
		Object []curVals = new Object[fcount];
		
		while (true) {
			int len = seq.length();
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)seq.getMem(i);
				for (int f = 0; f < fcount; ++f) {
					curVals[f] = r.getNormalFieldValue(findex[f]);
				}
				
				if (Variant.compareArrays(curVals, vals) >= 0) {
					if (i == 1) {
						cursor.setCache(seq);
						return result;
					} else if (result == null) {
						cursor.setCache(seq.split(i));
						result = seq;
					} else {
						cursor.setCache(seq.split(i));
						result.addAll(seq);
					}
					return result;
				}
			}
			
			if (result == null) {
				result = seq;
			} else {
				result.addAll(seq);
			}
			
			seq = cursor.getStartBlockData(ICursor.FETCHCOUNT);
			if (seq == null || seq.length() == 0) {
				return result;
			}
		}
	}
	
	/**
	 * ���ݶ�·�α�mcs������һ��ͬ���ֶεĶ�·�α�
	 * @param exps ȡ���ֶα��ʽ������expsΪnullʱ����fieldsȡ����
	 * @param fields ȡ���ֶε�������
	 * @param filter ���˱��ʽ
	 * @param fkNames ָ��FK���˵��ֶ�����
	 * @param codes ָ��FK���˵���������
	 * @param mcs ��·�α�
	 * @param opt ѡ��
	 * @param ctx ������
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes,  String []opts, MultipathCursors mcs, String opt, Context ctx) {
		getGroupTable().checkReadable();
		
		ICursor []srcCursors = mcs.getParallelCursors();
		int segCount = srcCursors.length;
		if (segCount == 1) {
			ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
			ICursor []cursors = new ICursor[] {cs};
			return new MultipathCursors(cursors, ctx);
		}
		
		Object [][]minValues = new Object [segCount][];
		int fcount = -1;
		
		for (int i = 1; i < segCount; ++i) {
			minValues[i] = srcCursors[i].getSegmentStartValues(opt);
			if (minValues[i] != null) {
				if (fcount == -1) {
					fcount = minValues[i].length;
				} else if (fcount != minValues[i].length) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
				}
			}
		}
		
		if (fcount == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
		}

		String []dimFields;
		ColumnMetaData[] sortedCols;
		if (opt != null && opt.indexOf('k') != -1) {
			// ��kѡ��ʱ���׼���Ϊͬ���ֶ��ֶ�
			String []keys = getAllKeyColNames();
			if (keys == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
			}
			
			fcount = 1;
			dimFields = new String[] {keys[0]};
			sortedCols = new ColumnMetaData[] {getColumn(keys[0])};
		} else {
			sortedCols = getAllSortedColumns();
			if (sortedCols.length < fcount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
			}
			
			dimFields = new String[fcount];
			for (int f = 0; f < fcount; ++f) {
				dimFields[f] = sortedCols[f].getColName();
			}
		}
		
		int blockCount = getDataBlockCount();
		ICursor []cursors = new ICursor[segCount];
		int startBlock = 0;
		int currentBlock = 0; // ��ǰ������Сֵ�Ŀ�
		
		// ��Ҫ��ͷ���α�Ķ�Ӧ��ǰ����α�����
		int []appendSegs = new int[segCount];
		for (int s = 0; s < segCount; ++s) {
			appendSegs[s] = -1;
		}
		
		try {
			ObjectReader []readers = new ObjectReader[fcount];
			Object []blockMinVals = new Object[fcount];
			Object []blockMaxVals = new Object[fcount];
			Object []prevMaxVals = new Object[fcount];
			
			for (int f = 0; f < fcount; ++f) {
				readers[f] = sortedCols[f].getSegmentReader();
				readers[f].readLong40();
				readers[f].skipObject(); // ��Сֵ
				blockMaxVals[f] = readers[f].readObject(); // ���ֵ
				blockMinVals[f] = readers[f].readObject(); // ��һ����¼��ֵ
			}
			
			Next:
			for (int s = 0; s < segCount; ++s) {
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}
				
				int nextSeg = s + 1;
				Object []nextMinValue = null;
				while (nextSeg < segCount) {
					nextMinValue = minValues[nextSeg];
					if (nextMinValue != null) {
						break;
					} else {
						nextSeg++;
					}
				}
				
				if (nextMinValue == null) {
					cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
					((IDWCursor)cursors[s]).setSegment(startBlock, blockCount);
					startBlock = blockCount;
					continue;
				}
				
				while (currentBlock < blockCount) {
					int cmp = Variant.compareArrays(blockMinVals, nextMinValue);
					if (cmp > 0) {
						cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
						
						if (currentBlock > 0) {
							((IDWCursor)cursors[s]).setSegment(startBlock, currentBlock - 1);
							startBlock = currentBlock - 1;
							appendSegs[nextSeg] = s;
						} else {
							((IDWCursor)cursors[s]).setSegment(0, 0);
						}
						
						continue Next;
					} else if (cmp == 0) {
						cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
						
						// ǰһ���п���������һ·�α���ʼֵ��ȵ�
						if (currentBlock > 0 && Variant.compareArrays(prevMaxVals, nextMinValue) >= 0) {
							((IDWCursor)cursors[s]).setSegment(startBlock, currentBlock - 1);
							startBlock = currentBlock - 1;
							appendSegs[nextSeg] = s;
						} else {
							((IDWCursor)cursors[s]).setSegment(startBlock, currentBlock);
							startBlock = currentBlock;
						}
						
						continue Next;
					} else {
						currentBlock++;
						if (currentBlock < blockCount) {
							Object []tmp = prevMaxVals;
							prevMaxVals = blockMaxVals;
							blockMaxVals = tmp;
							
							for (int f = 0; f < fcount; ++f) {
								readers[f].readLong40();
								readers[f].skipObject(); // ��Сֵ
								blockMaxVals[f] = readers[f].readObject(); // ���ֵ
								blockMinVals[f] = readers[f].readObject(); // ��һ����¼��ֵ
							}
						}
					}
				}
				
				cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				if (s + 1 == segCount) {
					((IDWCursor)cursors[s]).setSegment(startBlock, blockCount);
					startBlock = blockCount;
				} else {
					// ��ǰ���ѵ����һ�Σ����ձ�Ķ�·�α껹û�����һ·
					((IDWCursor)cursors[s]).setSegment(startBlock, blockCount - 1);
					startBlock = blockCount - 1;
					appendSegs[nextSeg] = s;
				}
			}
			
			for (int i = segCount - 1; i > 0; --i) {
				if (appendSegs[i] != -1) {
					Sequence seq = fetchToValue((IDWCursor)cursors[i], dimFields, minValues[i]);
					((IDWCursor)cursors[appendSegs[i]]).setAppendData(seq);
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}

		MultipathCursors result = new MultipathCursors(cursors, ctx);
		PhyTable tmd = getSupplementTable(false);
		if (tmd == null) {
			return result;
		}
		
		String []sortFields = ((IDWCursor)cursors[0]).getSortFields();
		if (sortFields != null) {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, result, null, ctx);
			return merge(result, (MultipathCursors)cs2, sortFields);
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, mcs, null, ctx);
			return conj(result, cs2);
		}
	}

	// �в��ļ�ʱ�����ݸ���
	private Sequence update(PhyTable stmd, Sequence data, String opt) throws IOException {
		boolean isUpdate = true, isInsert = true, isSave = true;
		Sequence result = null;
		if (opt != null) {
			if (opt.indexOf('i') != -1) isUpdate = false;
			if (opt.indexOf('u') != -1) {
				if (!isUpdate) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
				
				isInsert = false;
			}
			
			if (opt.indexOf('n') != -1) result = new Sequence();
			if (opt.indexOf('m') != -1) isSave = false;
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		// �Ը������ݽ�������
		data.sortFields(getAllSortedColNames());
		appendCache();
		
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//�Ƿ���һ���εĵײ�insert(�ӱ�)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//�ӱ��Ӧ�������α�ţ�0��ʾ��������
			ColPhyTable baseTable = (ColPhyTable) this.groupTable.baseTable;
			RecordSeqSearcher baseSearcher = new RecordSeqSearcher(baseTable);
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k), temp);
					block[i] = temp[0];
					if (seqs[i] < 0) {
						//����ǲ��룬Ҫ�ж�һ���Ƿ������������
						long seq = baseSearcher.findNext(r.getFieldValue(k));
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//�ӱ��������ݱ���������
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//�����������������ᴦ��
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			} else {
				Object []keyValues = new Object[keyCount];
				int baseKeyCount = sortedColStartIndex;
				Object []baseKeyValues = new Object[baseKeyCount];
				
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
						if (k < baseKeyCount) {
							baseKeyValues[k] = keyValues[k]; 
						}
					}
					
					seqs[i] = searcher.findNext(keyValues, temp);
					block[i] = temp[0];
					if (seqs[i] < 0 || block[i] > 0) {
						//����ǲ��룬Ҫ�ж�һ���Ƿ������������
						long seq = baseSearcher.findNext(baseKeyValues);
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//�ӱ��������ݱ���������
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//�����������������ᴦ��
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			}
		}
		
		// ��Ҫ��������ĵ���append׷��
		Sequence append = new Sequence();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean needUpdateSubTable = false;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr.toRecord());
						modifyRecords.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else {
					append.add(sr);
				}
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							s++;
							tmp.add(mr);
						} else {
							if ((mr.getState() == ModifyRecord.STATE_UPDATE && isUpdate) || 
									(mr.getState() == ModifyRecord.STATE_DELETE && isInsert)) {
								// ״̬����update
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr.setRecord(sr.toRecord(), ModifyRecord.STATE_UPDATE);
								if (result != null) {
									result.add(sr);
								}
							}

							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						if (isUpdate) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr.toRecord());
							tmp.add(mr);
							
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr.setRecord(sr.toRecord());
									if (result != null) {
										result.add(sr);
									}
								}
								
								tmp.add(mr);
								s++;
								t++;
							} else {
								append.add(data.getMem(t));
								t++;
							}
						} else {
							append.add(data.getMem(t));
							t++;
						}
					} else {
						append.add(data.getMem(t));
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				BaseRecord sr = (BaseRecord)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr.toRecord());
						tmp.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else {
					append.add(sr);
				}
			}
			
			this.modifyRecords = tmp;
			if (srcLen != tmp.size()) {
				needUpdateSubTable = true;
			}
		}
		
		if (!isPrimaryTable) {
			//�ӱ������Ҫ�����������޸�
			update(parent.getModifyRecords());
			
			for (ModifyRecord r : modifyRecords) {
				if (r.getState() == ModifyRecord.STATE_INSERT) {
					if (r.getParentRecordSeq() == 0) {
						this.modifyRecords = null;
						this.modifyRecords = getModifyRecords();
						//�ӱ��������ݱ���������
						MessageManager mm = EngineMessage.get();
						throw new RQException(r.getRecord().toString(null) + mm.getMessage("grouptable.invalidData"));
					}
				}
			}
			
		}
		
		if (isSave) {
			saveModifyRecords();
		}
		
		if (isPrimaryTable && needUpdateSubTable) {
			//������insert���ͱ�����������ӱ���
			ArrayList<PhyTable> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				ColPhyTable t = ((ColPhyTable)tableList.get(i));
				boolean needSave = t.update(modifyRecords);
				if (needSave) {
					t.saveModifyRecords();
				}
			}
		}
		
		if (append.length() > 0) {
			Sequence seq = stmd.update(append, opt);
			if (result != null) {
				result.addAll(seq);
			}
		}
		
		if (isSave) {
			groupTable.save();
		}
		
		return result;
	}
	
	/**
	 * ���»���
	 */
	public Sequence update(Sequence data, String opt) throws IOException {
		if (data != null) {
			data = new Sequence(data);
		}
		
		if (!hasPrimaryKey) {
			//û��άʱ����append
			boolean hasY = opt != null && opt.indexOf('y') != -1;
			if (hasY) {
				append_y(data);
			} else {
				append(new MemoryCursor(data));
			}
			return data;
		}
		
		ComTable groupTable = getGroupTable();
		groupTable.checkWritable();
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			return update(tmd, data, opt);
		}
		
		boolean isInsert = true,isUpdate = true;
		Sequence result = null;
		if (opt != null) {
			if (opt.indexOf('y') != -1) {
				return update_y(data, opt);
			}
			
			if (opt.indexOf('i') != -1) isUpdate = false;
			if (opt.indexOf('u') != -1) {
				if (!isUpdate) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
				
				isInsert = false;
			}
			
			if (opt.indexOf('n') != -1) result = new Sequence();
		}
		
		long totalRecordCount = this.totalRecordCount;
		if (totalRecordCount == 0) {
			if (isInsert) {
				ICursor cursor = new MemoryCursor(data);
				append(cursor);
				appendCache();
				if (result != null) {
					result.addAll(data);
				}
			}
			
			return result;
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		// �Ը������ݽ�������
		data.sortFields(getAllSortedColNames());
		appendCache();
		
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//�Ƿ���һ���εĵײ�insert(�ӱ�)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//�ӱ��Ӧ�������α�ţ�0��ʾ��������
			ColPhyTable baseTable = (ColPhyTable) this.groupTable.baseTable;
			RecordSeqSearcher baseSearcher = new RecordSeqSearcher(baseTable);
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k), temp);
					block[i] = temp[0];
					if (seqs[i] < 0) {
						//����ǲ��룬Ҫ�ж�һ���Ƿ������������
						long seq = baseSearcher.findNext(r.getFieldValue(k));
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//�ӱ��������ݱ���������
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//�����������������ᴦ��
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			} else {
				Object []keyValues = new Object[keyCount];
				int baseKeyCount = sortedColStartIndex;
				Object []baseKeyValues = new Object[baseKeyCount];
				
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
						if (k < baseKeyCount) {
							baseKeyValues[k] = keyValues[k]; 
						}
					}
					
					seqs[i] = searcher.findNext(keyValues, temp);
					block[i] = temp[0];
					if (seqs[i] < 0 || block[i] > 0) {
						//����ǲ��룬Ҫ�ж�һ���Ƿ������������
						long seq = baseSearcher.findNext(baseKeyValues);
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//�ӱ��������ݱ���������
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//�����������������ᴦ��
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			}
		}
		
		// ��Ҫ��������ĵ���append׷��
		Sequence append = new Sequence();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean needUpdateSubTable = false;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr.toRecord());
						modifyRecords.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[i];
					if (seq <= totalRecordCount || block[i] > 0) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[i]);
						//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
						//����������Ϊָ������α�ţ���������������޸�
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[i]);
						}
						modifyRecords.add(r);
					} else {
						append.add(sr);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							s++;
							tmp.add(mr);
						} else {
							if ((mr.getState() == ModifyRecord.STATE_UPDATE && isUpdate) || 
									(mr.getState() == ModifyRecord.STATE_DELETE && isInsert)) {
								// ״̬����update
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr.setRecord(sr.toRecord(), ModifyRecord.STATE_UPDATE);
								if (result != null) {
									result.add(sr);
								}
							}

							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						if (isUpdate) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr.toRecord());
							tmp.add(mr);
							
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr.setRecord(sr.toRecord());
									if (result != null) {
										result.add(sr);
									}
								}
								
								tmp.add(mr);
								s++;
								t++;
							} else {
								if (isInsert) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
									mr.setBlock(block[t]);
									//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
									//����������Ϊָ������α�ţ���������������޸�
									if (!isPrimaryTable) {
										mr.setParentRecordSeq(recNum[t]);
									}
									modifyRecords.add(mr);
									tmp.add(mr);
									if (result != null) {
										result.add(sr);
									}
								}
								
								t++;
							}
						} else {
							if (isInsert) {
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
								mr.setBlock(block[t]);
								//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
								//����������Ϊָ������α�ţ���������������޸�
								if (!isPrimaryTable) {
									mr.setParentRecordSeq(recNum[t]);
								}
								modifyRecords.add(mr);
								tmp.add(mr);
								if (result != null) {
									result.add(sr);
								}
							}
							
							t++;
						}
					} else {
						if (isInsert) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
							mr.setBlock(block[t]);
							//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
							//����������Ϊָ������α�ţ���������������޸�
							if (!isPrimaryTable) {
								mr.setParentRecordSeq(recNum[t]);
							}
							modifyRecords.add(mr);
							tmp.add(mr);
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				BaseRecord sr = (BaseRecord)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr.toRecord());
						tmp.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[t];
					if (seq <= totalRecordCount) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[t]);
						//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
						//����������Ϊָ������α�ţ���������������޸�
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[t]);
						}
						modifyRecords.add(r);
						tmp.add(r);
					} else {
						append.add(sr);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
			
			this.modifyRecords = tmp;
			if (srcLen != tmp.size()) {
				needUpdateSubTable = true;
			}
		}
		
		if (!isPrimaryTable) {
			//�ӱ������Ҫ�����������޸�
			update(parent.getModifyRecords());
			
			for (ModifyRecord r : modifyRecords) {
				if (r.getState() == ModifyRecord.STATE_INSERT) {
					if (r.getParentRecordSeq() == 0) {
						this.modifyRecords = null;
						this.modifyRecords = getModifyRecords();
						//�ӱ��������ݱ���������
						MessageManager mm = EngineMessage.get();
						throw new RQException(r.getRecord().toString(null) + mm.getMessage("grouptable.invalidData"));
					}
				}
			}
			
		}
		
		saveModifyRecords();
		
		if (isPrimaryTable && needUpdateSubTable) {
			//������insert���ͱ�����������ӱ���
			ArrayList<PhyTable> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				ColPhyTable t = ((ColPhyTable)tableList.get(i));
				boolean needSave = t.update(modifyRecords);
				if (needSave) {
					t.saveModifyRecords();
				}
			}
		}
		
		if (append.length() > 0) {
			ICursor cursor = new MemoryCursor(append);
			append(cursor);
			appendCache();
		} else {
			groupTable.save();
		}
		
		return result;
	}
	
	// �ںϵ��ڴ��еĲ�������д�����
	private void append_y(Sequence data) throws IOException {
		if (data == null || data.length() == 0) {
			return;
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		int len = data.length();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
		}
				
		BaseRecord r1 = (BaseRecord)data.get(1);
		String []pks = getAllSortedColNames();
		int keyCount = pks == null ? 0 : pks.length;
		int []keyIndex = new int[keyCount];
		for (int i = 0; i < keyCount; ++i) {
			keyIndex[i] = ds.getFieldIndex(pks[i]);
		}
		
		if (keyCount > 0 && maxValues != null && r1.compare(keyIndex, maxValues) < 0) {
			// ��Ҫ�鲢
			long []seqs = new long[len + 1];
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
			
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_INSERT, sr.toRecord());
					modifyRecords.add(r);
				} else {
					ModifyRecord r = new ModifyRecord(-seqs[i], ModifyRecord.STATE_INSERT, sr.toRecord());
					modifyRecords.add(r);
				}
			}
		} else {
			long seq = totalRecordCount + 1;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
				modifyRecords.add(r);
			}
		}
	}

	// �ںϵ��ڴ��еĲ�������д�����
	private Sequence update_y(Sequence data, String opt) throws IOException {
		boolean isInsert = true,isUpdate = true;
		Sequence result = null;
		if (opt != null) {
			if (opt.indexOf('i') != -1) isUpdate = false;
			if (opt.indexOf('u') != -1) {
				if (!isUpdate) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
				
				isInsert = false;
			}
			
			if (opt.indexOf('n') != -1) result = new Sequence();
		}
		
		long totalRecordCount = this.totalRecordCount;
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		// �Ը������ݽ�������
		data.sortFields(getAllSortedColNames());
		appendCache();
		
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//�Ƿ���һ���εĵײ�insert(�ӱ�)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//�ӱ��Ӧ�������α�ţ�0��ʾ��������
			ColPhyTable baseTable = (ColPhyTable) this.groupTable.baseTable;
			RecordSeqSearcher baseSearcher = new RecordSeqSearcher(baseTable);
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k), temp);
					block[i] = temp[0];
					if (seqs[i] < 0) {
						//����ǲ��룬Ҫ�ж�һ���Ƿ������������
						long seq = baseSearcher.findNext(r.getFieldValue(k));
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//�ӱ��������ݱ���������
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//�����������������ᴦ��
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			} else {
				Object []keyValues = new Object[keyCount];
				int baseKeyCount = sortedColStartIndex;
				Object []baseKeyValues = new Object[baseKeyCount];
				
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
						if (k < baseKeyCount) {
							baseKeyValues[k] = keyValues[k]; 
						}
					}
					
					seqs[i] = searcher.findNext(keyValues, temp);
					block[i] = temp[0];
					if (seqs[i] < 0 || block[i] > 0) {
						//����ǲ��룬Ҫ�ж�һ���Ƿ������������
						long seq = baseSearcher.findNext(baseKeyValues);
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//�ӱ��������ݱ���������
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//�����������������ᴦ��
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			}
		}
		
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean needUpdateSubTable = false;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr.toRecord());
						modifyRecords.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[i];
					if (seq <= totalRecordCount || block[i] > 0) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[i]);
						//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
						//����������Ϊָ������α�ţ���������������޸�
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[i]);
						}
						modifyRecords.add(r);
					} else {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[i]);
						//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
						//����������Ϊָ������α�ţ���������������޸�
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[i]);
						}
						
						modifyRecords.add(r);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							s++;
							tmp.add(mr);
						} else {
							if ((mr.getState() == ModifyRecord.STATE_UPDATE && isUpdate) || 
									(mr.getState() == ModifyRecord.STATE_DELETE && isInsert)) {
								// ״̬����update
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr.setRecord(sr.toRecord(), ModifyRecord.STATE_UPDATE);
								if (result != null) {
									result.add(sr);
								}
							}

							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						if (isUpdate) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr.toRecord());
							tmp.add(mr);
							
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr.setRecord(sr.toRecord());
									if (result != null) {
										result.add(sr);
									}
								}
								
								tmp.add(mr);
								s++;
								t++;
							} else {
								if (isInsert) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
									mr.setBlock(block[t]);
									//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
									//����������Ϊָ������α�ţ���������������޸�
									if (!isPrimaryTable) {
										mr.setParentRecordSeq(recNum[t]);
									}
									modifyRecords.add(mr);
									tmp.add(mr);
									if (result != null) {
										result.add(sr);
									}
								}
								
								t++;
							}
						} else {
							if (isInsert) {
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
								mr.setBlock(block[t]);
								//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
								//����������Ϊָ������α�ţ���������������޸�
								if (!isPrimaryTable) {
									mr.setParentRecordSeq(recNum[t]);
								}
								modifyRecords.add(mr);
								tmp.add(mr);
								if (result != null) {
									result.add(sr);
								}
							}
							
							t++;
						}
					} else {
						if (isInsert) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
							mr.setBlock(block[t]);
							//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
							//����������Ϊָ������α�ţ���������������޸�
							if (!isPrimaryTable) {
								mr.setParentRecordSeq(recNum[t]);
							}
							modifyRecords.add(mr);
							tmp.add(mr);
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				BaseRecord sr = (BaseRecord)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr.toRecord());
						tmp.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[t];
					if (seq <= totalRecordCount) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[t]);
						//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
						//����������Ϊָ������α�ţ���������������޸�
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[t]);
						}
						modifyRecords.add(r);
						tmp.add(r);
					} else {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[t]);
						//������ӱ�insert Ҫ����parentRecordSeq����Ϊ�ӱ�insert�Ŀ���ָ����������
						//����������Ϊָ������α�ţ���������������޸�
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[t]);
						}
						
						modifyRecords.add(r);
						tmp.add(r);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
			
			this.modifyRecords = tmp;
			if (srcLen != tmp.size()) {
				needUpdateSubTable = true;
			}
		}
		
		if (!isPrimaryTable) {
			//�ӱ������Ҫ�����������޸�
			update(parent.getModifyRecords());
			
			for (ModifyRecord r : modifyRecords) {
				if (r.getState() == ModifyRecord.STATE_INSERT) {
					if (r.getParentRecordSeq() == 0) {
						this.modifyRecords = null;
						this.modifyRecords = getModifyRecords();
						//�ӱ��������ݱ���������
						MessageManager mm = EngineMessage.get();
						throw new RQException(r.getRecord().toString(null) + mm.getMessage("grouptable.invalidData"));
					}
				}
			}
			
		}
		
		//saveModifyRecords();
		
		if (isPrimaryTable && needUpdateSubTable) {
			//������insert���ͱ�����������ӱ���
			ArrayList<PhyTable> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				ColPhyTable t = ((ColPhyTable)tableList.get(i));
				boolean needSave = t.update(modifyRecords);
				if (needSave) {
					t.saveModifyRecords();
				}
			}
		}
		
		//if (append.length() > 0) {
		//	ICursor cursor = new MemoryCursor(append);
		//	append(cursor);
		//} else {
		//	groupTable.save();
		//}
				
		return result;
	}
	
	/**
	 * ��дһЩ�е�����
	 * ע�⣺���������Ҫ��֤ԭ�����ﲻ����ֶΣ����еķֶζ�����ԭ���ġ�
	 * @param cursor Ҫд�������
	 * @param opt	ѡ��
	 * @throws IOException
	 */
	public void update(ICursor cursor, String opt) throws IOException {
		/**
		 * ����cursor���ݵĽṹ�����Ҫ���µ���
		 */
		Sequence temp = cursor.peek(1);
		String[] fields = ((BaseRecord)temp.getMem(1)).getFieldNames();
		ColumnMetaData[] columns = getColumns(fields);
		
		/**
		 * ���Ŀǰÿ���ֶεļ�¼����
		 */
		BlockLinkReader rowCountReader = getSegmentReader();
		int blockCount = getDataBlockCount();
		long recordCountArray[] = new long[blockCount];
		try {
			for (int i = 0; i < blockCount; ++i) {
				recordCountArray[i]  = rowCountReader.readInt32();
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				rowCountReader.close();
			} catch (Exception e){};
		}
		
		/**
		 * ��Ҫ���µ������
		 */
		for (ColumnMetaData col : columns) {
			BlockLink blockLink = col.getSegmentBlockLink();//�ֶ���Ϣ��
			blockLink.setFirstBlockPos(blockLink.firstBlockPos);
			blockLink.freeIndex = 0;
			
			blockLink = col.getDataBlockLink();//���ݿ�
			blockLink.setFirstBlockPos(blockLink.firstBlockPos);
			blockLink.freeIndex = 0;
			
			col.getDict().clear();
			col.initDictArray();
		}
		
		/**
		 * дǰ�����ĳ�ʼ��
		 */
		for (ColumnMetaData col : columns) {
			col.prepareWrite();
		}
		int columnCount = columns.length;
		BufferWriter bufferWriters[] = new BufferWriter[columnCount];
		Object []minValues = new Object[columnCount];
		Object []maxValues = new Object[columnCount];
		Object []startValues = new Object[columnCount];
		int[] dataTypeInfo = new int[columnCount];
		
		/**
		 * ѭ��д��������
		 */
		for (long count : recordCountArray) {
			Sequence data = cursor.fetch((int) count);
			int end = data.length();
			for (int i = 0; i < columnCount; i++) {
				//д�����ݵ�ÿ���п�
				bufferWriters[i] = columns[i].getColDataBufferWriter();
				Sequence dict = columns[i].getDict();
				DataBlockWriterJob.writeDataBlock(bufferWriters[i], data, dict, i, 1, end, 
						maxValues, minValues, startValues, dataTypeInfo);
				
				//ͳ������������
				boolean doCheck = groupTable.isCheckDataPure();
				columns[i].adjustDataType(dataTypeInfo[i], doCheck);
				columns[i].initDictArray();
				
				//�ύÿ���п�buffer
				columns[i].appendColBlock(bufferWriters[i].finish(), minValues[i], maxValues[i], startValues[i]);
			}
			
		}
		
		/**
		 * �ύ����Ϣ
		 */
		for (ColumnMetaData col : columns) {
			col.finishWrite();
		}
		groupTable.save();
		updateIndex();
	}
	
	/** ����data�����ݵ�άֵ��ɾ��ָ���ļ�¼
	 * @param data 
	 * @param opt
	 */
	public Sequence delete(Sequence data, String opt) throws IOException {
		boolean deleteByBaseKey = false;//ֻ�����ڲ�ɾ���ӱ����������ʱ������@n
		if (opt != null && opt.indexOf('s') != -1) {
			deleteByBaseKey = true;
		}
		
		if (!hasPrimaryKey && !deleteByBaseKey) {
			//û��άʱ������
			return null;
		}
		
		if (data != null) {
			data = new Sequence((Sequence)data);
		}
		
		ComTable groupTable = getGroupTable();
		groupTable.checkWritable();
		
		Sequence result1 = null;
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			// �в��ļ�ʱ��ɾ�����ļ��е����ݣ����ļ��в����ڵ�����Դ�ļ���ɾ��
			result1 = tmd.delete(data, "n");
			data = (Sequence) data.diff(result1, false);
		}
		
		appendCache();
		boolean nopt = false, isSave = true;
		if (opt != null) {
			if (opt.indexOf('n') != -1) {
				nopt = true;
			}
			
			if (opt.indexOf('y') != -1) {
				isSave = false;
			}
		}
		
		long totalRecordCount = this.totalRecordCount;
		if (totalRecordCount == 0 || data == null || data.length() == 0) {
			return nopt ? result1 : null;
		}
		
		Sequence result = null;
		if (nopt) {
			result = new Sequence();
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
				
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		if (deleteByBaseKey) {
			keyCount = sortedColStartIndex;
		}
		
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		if (deleteByBaseKey) {
			data.sortFields(parent.getSortedColNames());
		} else {
			data.sortFields(getAllSortedColNames());
		}
		
		int len = data.length();
		long []seqs = new long[len + 1];
		int temp[] = new int[1];
		LongArray seqList = null;
		Sequence seqListData = null;
		if (deleteByBaseKey) {
			seqList = new LongArray(len * 10);
			seqListData = new Sequence(len);
		}
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			Object []keyValues = new Object[keyCount];
			int baseKeyCount = sortedColStartIndex;
			Object []baseKeyValues = new Object[baseKeyCount];
			
			for (int i = 1; i <= len;) {
				BaseRecord r = (BaseRecord)data.getMem(i);
				for (int k = 0; k < keyCount; ++k) {
					keyValues[k] = r.getFieldValue(keyIndex[k]);
					if (k < baseKeyCount) {
						baseKeyValues[k] = keyValues[k]; 
					}
				}
				
				if (deleteByBaseKey) {
					long s = searcher.findNext(keyValues, keyCount);
					if (s <= 0) {
						i++;//���Ҳ���ʱ��++
					} else {
						seqList.add(s);
						seqListData.add(r);
					}

				} else {
					seqs[i] = searcher.findNext(keyValues, temp);
					i++;
				}
				
			}
		}
		
		if (deleteByBaseKey) {
			len = seqList.size();
			if (0 == len) {
				return result;
			}
			
			seqs = seqList.getDatas();
			data = seqListData;
		}
		
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean modified = true;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			for (int i = 1; i <= len; ++i) {
				if (seqs[i] > 0) {
					ModifyRecord r = new ModifyRecord(seqs[i]);
					modifyRecords.add(r);
					
					if (result != null) {
						result.add(data.getMem(i));
					}
				}
			}
			
			if (modifyRecords.size() > 0) {
				this.modifyRecords = modifyRecords;
			} else {
				modified = false;
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								tmp.add(mr);
							} else if (cmp == 0) {
								if (result != null) {
									result.add(data.getMem(t));
								}
							} else {
								if (result != null) {
									result.add(data.getMem(t));
								}

								ModifyRecord r = new ModifyRecord(seqs[t]);
								tmp.add(r);
								tmp.add(mr);
								t++;
							}
							s++;
							continue;
						} else {
							if (result != null && mr.getState() == ModifyRecord.STATE_UPDATE) {
								result.add(data.getMem(t));
							}
							
							mr.setDelete();
							s++;
							t++;
						}
					} else {
						mr = new ModifyRecord(seq2);
						if (result != null) {
							result.add(data.getMem(t));
						}
	
						t++;
					}
					
					tmp.add(mr);
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (result != null) {
									result.add(data.getMem(t));
								}
	
								s++;
								t++;
							} else {
								t++;
							}
						} else {
							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				if (seqs[t] > 0) {
					if (result != null) {
						result.add(data.getMem(t));
					}

					ModifyRecord r = new ModifyRecord(seqs[t]);
					tmp.add(r);
				}
			}
			
			this.modifyRecords = tmp;
		}
		
		if (modified) {
			if (isPrimaryTable) {
				//������delete���ͱ���ͬ��delete�ӱ�
				ArrayList<PhyTable> tableList = getTableList();
				int size = tableList.size();
				for (int i = 0; i < size; ++i) {
					ColPhyTable t = ((ColPhyTable)tableList.get(i));
					t.delete(data, "s");//ɾ���ӱ�����
					t.delete(data);//ɾ���ӱ���
				}
				
				//������ɾ����������λ�û�仯����Ҫͬ���ӱ���
				for (int i = 0; i < size; ++i) {
					ColPhyTable t = ((ColPhyTable)tableList.get(i));
					t.update(this.modifyRecords);
					t.saveModifyRecords();
				}
			}
			
			if (!deleteByBaseKey && isSave) {
				saveModifyRecords();
			}
		}
		
		if (!deleteByBaseKey  && isSave) {
			groupTable.save();
		}
		
		if (nopt) {
			result.addAll(result1);
		}
		
		return result;
	}
	
	//��������Ĳ�����ͬ�������Լ��Ĳ���
	private boolean update(ArrayList<ModifyRecord> baseModifyRecords) throws IOException {
		getGroupTable().checkWritable();
		
		if (baseModifyRecords == null) return false;
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		if (modifyRecords == null) {
			return false;
		}
		//int fieldsLen = columns.length;
		int len = sortedColStartIndex;
		int []index = new int[len];
		int []findex = getSortedColIndex();
		for (int i = 0; i < len; ++i) {
			index[i] = findex[i];
		}
		
		boolean find = false;
		int parentRecordSeq = 0;
		for (ModifyRecord mr : baseModifyRecords) {
			parentRecordSeq++;
			BaseRecord mrec = mr.getRecord();
			
			if (mr.getState() != ModifyRecord.STATE_DELETE) {
				for (ModifyRecord r : modifyRecords) {
					if (r.getState() == ModifyRecord.STATE_DELETE) {
						continue;
					}
					
					BaseRecord rec = r.getRecord();
					int cmp = rec.compare(mrec, index);
					if (cmp == 0) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							r.setParentRecordSeq(-parentRecordSeq);
						} else {
							r.setParentRecordSeq(mr.getRecordSeq());
						}
						find = true;
					}
				}
			}
		}
		return find;
	}
	
	//����dataɾ���ӱ�Ĳ���
	private boolean delete(Sequence data) throws IOException {
		ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>();
		ArrayList<ModifyRecord> srcModifyRecords = new ArrayList<ModifyRecord>();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		if (modifyRecords == null) {
			return false;
		}
		tmp.addAll(modifyRecords);
		
		int len = sortedColStartIndex;
		int []index = new int[len];
		int []findex = getSortedColIndex();
		for (int i = 0; i < len; ++i) {
			index[i] = findex[i];
		}
		
		len = data.length();
		boolean delete = false;
		for (int i = 1; i <= len; i++) {
			BaseRecord mrec = (BaseRecord) data.get(i);
			srcModifyRecords.clear();
			srcModifyRecords.addAll(tmp);
			tmp.clear();
			for (ModifyRecord r : srcModifyRecords) {
				int state = r.getState();
				if (state == ModifyRecord.STATE_UPDATE) {
					BaseRecord rec = r.getRecord();
					int cmp = rec.compare(mrec, index);
					if (cmp == 0) {
						r.setDelete();
						r.setRecord(null);
						delete = true;
					}
					tmp.add(r);
				} else if (state == ModifyRecord.STATE_INSERT) {
					BaseRecord rec = r.getRecord();
					int cmp = rec.compare(mrec, index);
					if (cmp == 0) {
						delete = true;
					} else {
						tmp.add(r);
					}
				} else {
					tmp.add(r);
				}
			}
		}
		
		if (delete) {
			this.modifyRecords = tmp;
		}
		return delete;
	}
	
	private Object[][] toKeyValuesArray(Sequence values) {
		int valueLen = values.length();
		if (valueLen == 0) {
			return null;
		}
		
		ColumnMetaData[] cols = getSortedColumns();		
		Object [][]dimValues = new Object[valueLen][];
		Object obj = values.getMem(1);
		int dimCount;
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			dimCount = seq.length();
			if (dimCount > cols.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("find" + mm.getMessage("function.invalidParam"));
			}
			
			dimValues = new Object[valueLen][];
			for (int i = 1; i <= valueLen; ++i) {
				seq = (Sequence)values.getMem(i);
				dimValues[i - 1] = seq.toArray();
			}
		} else if (obj instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)obj;
			int []keyIndex = r.dataStruct().getPKIndex();
			if (keyIndex == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}

			dimCount = keyIndex.length;
			if (dimCount > cols.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("find" + mm.getMessage("function.invalidParam"));
			}
			
			dimValues = new Object[valueLen][];
			for (int i = 1; i <= valueLen; ++i) {
				r = (BaseRecord)values.getMem(i);
				Object []cur = new Object[dimCount];
				for (int f = 0; f < dimCount; ++f) {
					cur[f] = r.getNormalFieldValue(keyIndex[f]);
				}
				
				dimValues[i - 1] = cur;
			}
		} else {
			dimCount = 1;
			dimValues = new Object[valueLen][];
			for (int i = 1; i <= valueLen; ++i) {
				dimValues[i - 1] = new Object[]{values.getMem(i)};
			}
		}
		
		return dimValues;
	}
	
	/**
	 * ����Ƿ���ʱ���find
	 * @param values
	 * @return
	 */
	private Table findsByTimekey(Sequence values, String []selFields) {
		String []keys = getAllSortedColNames();
		Expression exp = new Expression("null.contain(" + keys[0] + ")");
		Sequence keyValues = new Sequence();
		int valueLen = values.length();
		keyValues = new Sequence();
		for (int i = 1; i <= valueLen; ++i) {
			Object obj = values.getMem(i);
			if (obj instanceof Sequence) {
				Sequence seq = (Sequence) obj;
				keyValues.add(seq.getMem(1));
			} else {
				keyValues.add(obj);
			}
			
		}
		
		Context ctx = new Context();
		exp.getHome().setLeft(new Constant(keyValues));
		Sequence temp = cursor(selFields, exp, ctx).fetch();
		if (temp == null) return null;
		
		Sequence result = new Sequence(valueLen);
		for (int i = 1; i <= valueLen; ++i) {
			result.add(temp.findByKey(values.getMem(i), false));
		}
		Table table = new Table(result.dataStruct());
		table.addAll(result);
		return table;
	}
	/**
	 * �����������Ҽ�¼
	 * @param values
	 */
	public Table finds(Sequence values) throws IOException {
		return finds(values, null);
	}
	
	/**
	 * �����������Ҽ�¼��selFieldsΪѡ����
	 * @param values
	 * @param selFields
	 */
	public Table finds(Sequence values, String []selFields) throws IOException {
		getGroupTable().checkReadable();
		
		if (!hasPrimaryKey()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessKey"));
		}
		
		if (getGroupTable().hasTimeKey()) {
			return findsByTimekey(values, selFields);
		}
		
		if (parent != null || getModifyRecords() != null) {
			String []keys = getAllSortedColNames();
			int keyCount = keys.length;
			Expression exp;
			Sequence keyValues = values;
			
			if (keyCount == 1) {
				exp = new Expression("null.contain(" + keys[0] + ")"); 
				Object obj = values.getMem(1);
				int valueLen = values.length();
				if (valueLen == 0) {
					return null;
				}
				if (obj instanceof Sequence) {
					Sequence seq = (Sequence)obj;
					int dimCount = seq.length();
					if (dimCount > keyCount) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("find" + mm.getMessage("function.invalidParam"));
					}
					
					keyValues = new Sequence();
					for (int i = 1; i <= valueLen; ++i) {
						seq = (Sequence)values.getMem(i);
						keyValues.add(seq.getMem(1));
					}
				}
			} else {
				String str = "null.contain([";
				for (int i = 0; i < keyCount; i++) {
					str += keys[i];
					if (i != keyCount - 1) {
						str += ",";
					}
				}
				str += "])";
				exp = new Expression(str); 
			}

			Context ctx = new Context();
			exp.getHome().setLeft(new Constant(keyValues));
			Sequence result = cursor(selFields, exp, ctx).fetch();
			if (result == null) return null;
			Table table = new Table(result.dataStruct());
			table.addAll(result);
			return table;
		}
		
		if (selFields == null) {
			selFields = getColNames();
		}
		
		Object [][]dimValues = toKeyValuesArray(values);
		if (dimValues == null) {
			return null;
		}
		
		int valueLen = dimValues.length;
		int dimCount = dimValues[0].length;
		
		// ��ά�ֶκ�ѡ���кϲ�
		ColumnMetaData[] cols = getSortedColumns();
		ArrayList<ColumnMetaData> list = new ArrayList<ColumnMetaData>();
		for (int i = 0; i < dimCount; ++i) {
			list.add(cols[i]);
		}
		
		for (String field : selFields) {
			ColumnMetaData col = getColumn(field);
			if (col == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
			}
			
			if (!list.contains(col)) {
				list.add(col);
			}
		}
		
		int colCount = list.size();
		int []findex = new int [colCount]; // ÿ���ֶ��ڽ�������ݽṹ�е��ֶκ�
		ColumnMetaData []columns = new ColumnMetaData[colCount];
		list.toArray(columns);
		DataStruct ds = new DataStruct(selFields);
		
		//�ж��Ƿ�������ѡ��
		boolean hasKey = true;
		for (int i = 0; i < dimCount; ++i) {
			if (ds.getFieldIndex(cols[i].getColName()) == -1) {
				hasKey = false;
				break;
			}
		}
		if (hasKey) {
			ds.setPrimary(getSortedColNames());
		}
		
		BlockLinkReader rowCountReader = getSegmentReader();
		BlockLinkReader []colReaders = new BlockLinkReader[colCount];
		ObjectReader []segmentReaders = new ObjectReader[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			ColumnMetaData col = list.get(i);
			colReaders[i] = col.getColReader(false);
			segmentReaders[i] = col.getSegmentReader();
			findex[i] = ds.getFieldIndex(col.getColName());
		}
				
		int valueIndex = 0;
		Table table = new Table(ds, valueLen);
		int blockCount = getDataBlockCount();
		long []prevPos = new long[colCount];
		long []curPos = new long[colCount];

		Object []curStartValues = new Object[dimCount];
		int prevCount = rowCountReader.readInt32();
		Object []tmpDimValues = new Object[dimCount];
		
		for (int f = 0; f < dimCount; ++f) {
			prevPos[f] = segmentReaders[f].readLong40();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
		}
		
		for (int f = dimCount; f < colCount; ++f) {
			prevPos[f] = segmentReaders[f].readLong40();
			if (columns[f].hasMaxMinValues()) {
				segmentReaders[f].skipObject();
				segmentReaders[f].skipObject();
				segmentReaders[f].skipObject();
			}
		}
		
		IntArrayList indexList = new IntArrayList();		
		for (int b = 1; b < blockCount; ++b) {
			int curCount = rowCountReader.readInt32();
			for (int f = 0; f < dimCount; ++f) {
				curPos[f] = segmentReaders[f].readLong40();
				segmentReaders[f].skipObject();
				segmentReaders[f].skipObject();
				curStartValues[f] = segmentReaders[f].readObject();
			}
			
			for (int f = dimCount; f < colCount; ++f) {
				curPos[f] = segmentReaders[f].readLong40();
				if (columns[f].hasMaxMinValues()) {
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
				}
			}
			
			Object []curDimValues = dimValues[valueIndex];
			int cmp = Variant.compareArrays(curStartValues, curDimValues);
			if (cmp > 0) {
				BufferReader []readers = new BufferReader[colCount];
				for (int f = 0; f < dimCount; ++f) {
					readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
				}
				
				for (int i = 0; i < prevCount; ++i) {
					for (int f = 0; f < dimCount; ++f) {
						tmpDimValues[f] = readers[f].readObject();
					}
					
					cmp = Variant.compareArrays(tmpDimValues, curDimValues);
					if (cmp == 0) {
						BaseRecord r = table.newLast();
						for (int f = 0; f < dimCount; ++f) {
							if (findex[f] != -1) {
								r.setNormalFieldValue(findex[f], curDimValues[f]);
							}
						}
						
						indexList.addInt(i);
						valueIndex++;
						if (valueIndex == valueLen) {
							break;
						} else {
							curDimValues = dimValues[valueIndex];
							if (Variant.compareArrays(curStartValues, curDimValues) <= 0) {
								break;
							}
						}
					} else if (cmp > 0) {
						for (++valueIndex; valueIndex < valueLen; ++valueIndex) {
							curDimValues = dimValues[valueIndex];
							cmp = Variant.compareArrays(tmpDimValues, curDimValues);
							if (cmp == 0) {
								BaseRecord r = table.newLast();
								for (int f = 0; f < dimCount; ++f) {
									if (findex[f] != -1) {
										r.setNormalFieldValue(findex[f], curDimValues[f]);
									}
								}
								
								indexList.addInt(i);
								break;
							} else if (cmp < 0) {
								break;
							}
						}
						
						if (valueIndex == valueLen) {
							break;
						}
					}
				}
				
				int count = indexList.size();
				if (count > 0) {
					for (int f = dimCount; f < colCount; ++f) {
						readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
					}
					
					int prev = 0;
					for (int i = 0, m = table.length() - count + 1; i < count; ++i, ++m) {
						BaseRecord r = (BaseRecord)table.getMem(m);
						int index = indexList.getInt(i);
						for (int f = dimCount; f < colCount; ++f) {
							for (int j = prev; j < index; ++j) {
								readers[f].skipObject();
							}
							
							r.setNormalFieldValue(findex[f], readers[f].readObject());
						}
						
						prev = index + 1;
					}
					
					indexList.clear();
				}
				
				if (valueIndex == valueLen) {
					break;
				}
			}
			
			prevCount = curCount;
			long []tmpPos = prevPos;
			prevPos = curPos;
			curPos = tmpPos;
		}
		
		if (valueIndex < valueLen) {
			BufferReader []readers = new BufferReader[colCount];
			for (int f = 0; f < dimCount; ++f) {
				readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
			}
			
			Object []curDimValues = dimValues[valueIndex];
			for (int i = 0; i < prevCount; ++i) {
				for (int f = 0; f < dimCount; ++f) {
					tmpDimValues[f] = readers[f].readObject();
				}
				
				int cmp = Variant.compareArrays(tmpDimValues, curDimValues);
				if (cmp == 0) {
					BaseRecord r = table.newLast();
					for (int f = 0; f < dimCount; ++f) {
						if (findex[f] != -1) {
							r.setNormalFieldValue(findex[f], curDimValues[f]);
						}
					}
					
					indexList.addInt(i);
					valueIndex++;
					if (valueIndex == valueLen) {
						break;
					} else {
						curDimValues = dimValues[valueIndex];
					}
				} else if (cmp > 0) {
					for (++valueIndex; valueIndex < valueLen; ++valueIndex) {
						curDimValues = dimValues[valueIndex];
						cmp = Variant.compareArrays(tmpDimValues, curDimValues);
						if (cmp == 0) {
							BaseRecord r = table.newLast();
							for (int f = 0; f < dimCount; ++f) {
								if (findex[f] != -1) {
									r.setNormalFieldValue(findex[f], curDimValues[f]);
								}
							}
							
							indexList.addInt(i);
							break;
						} else if (cmp < 0) {
							break;
						}
					}
					
					if (valueIndex == valueLen) {
						break;
					}
				}
			}
			
			int count = indexList.size();
			if (count > 0) {
				for (int f = dimCount; f < colCount; ++f) {
					readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
				}
				
				int prev = 0;
				for (int i = 0, m = table.length() - count + 1; i < count; ++i, ++m) {
					BaseRecord r = (BaseRecord)table.getMem(m);
					int index = indexList.getInt(i);
					for (int f = dimCount; f < colCount; ++f) {
						for (int j = prev; j < index; ++j) {
							readers[f].skipObject();
						}
						
						r.setNormalFieldValue(findex[f], readers[f].readObject());
					}
					
					prev = index + 1;
				}
			}
		}
		
		return table;
	}

	/**
	 * ����n���طֶε�ֵ��ÿ������
	 * @param keys �ֶ��ֶ�
	 * @param list ���ص�ÿ������
	 * @param values ���طֶε�ֵ
	 * @param n ������ÿ������
	 * @throws IOException 
	 */
	public void getSegmentInfo(String []keys, ArrayList<Integer> list, Sequence values, int n) throws IOException {
		ColumnMetaData []columns = getColumns(keys);
		int colCount = keys.length;
		BlockLinkReader rowCountReader = getSegmentReader();
		ObjectReader []segmentReaders = new ObjectReader[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			segmentReaders[i] = columns[i].getSegmentReader();
		}
		
		int blockCount = getDataBlockCount();
		int  sum = rowCountReader.readInt32();
		for (int f = 0; f < colCount; ++f) {
			segmentReaders[f].readLong40();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
		}
		
		for (int i = 1; i < blockCount; ++i) {
			int cnt = rowCountReader.readInt32();
			if (sum + cnt > n) {
				list.add(sum);
				sum = cnt;
				Object []vals = new Object[colCount];
				for (int f = 0; f < colCount; ++f) {
					segmentReaders[f].readLong40();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					vals[f] = segmentReaders[f].readObject();
				}
				values.add(vals);
			} else {
				sum += cnt;
				for (int f = 0; f < colCount; ++f) {
					segmentReaders[f].readLong40();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
				}
			}
		}
		list.add(sum);//���һ���ֶ��������п���ֻ����һ��
	}
	
	/**
	 * ����n���طֶε�Ͷ���
	 * @param keys �ֶ��ֶ�
	 * @param list ���ص�ÿ������
	 * @param values ���طֶε�ֵ
	 * @param n ������ÿ������
	 * @throws IOException 
	 */
	public void getSegmentInfo2(String []keys, ArrayList<Integer> list, Sequence values, int n) throws IOException {
		ColumnMetaData []columns = getColumns(keys);
		int colCount = keys.length;
		BlockLinkReader rowCountReader = getSegmentReader();
		ObjectReader []segmentReaders = new ObjectReader[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			segmentReaders[i] = columns[i].getSegmentReader();
		}
		
		int blockCount = getDataBlockCount();
		list.add(0);
		int  sum = rowCountReader.readInt32();
		for (int f = 0; f < colCount; ++f) {
			segmentReaders[f].readLong40();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
		}
		
		for (int i = 1; i < blockCount; ++i) {
			int cnt = rowCountReader.readInt32();
			if (sum + cnt > n) {
				if (i + 1 != blockCount) {
					list.add(i);
					list.add(i);
					sum = cnt;
					Object []vals = new Object[colCount];
					for (int f = 0; f < colCount; ++f) {
						segmentReaders[f].readLong40();
						segmentReaders[f].skipObject();
						segmentReaders[f].skipObject();
						vals[f] = segmentReaders[f].readObject();
					}
					values.add(vals);
				}
				

			} else {
				sum += cnt;
				for (int f = 0; f < colCount; ++f) {
					segmentReaders[f].readLong40();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
				}
			}
		}
		list.add(blockCount);//������
	}
	
	/**
	 * �����е������Сֵ
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public Object[] getMaxMinValue(String key) throws IOException {
		if (this.totalRecordCount == 0) {
			return null;
		}
		ColumnMetaData column = getColumn(key);
		if (column == null) {
			return null;
		}
		if (!column.hasMaxMinValues()) {
			Expression max = new Expression("max(" + key +")");
			Expression min = new Expression("min(" + key +")");
			Expression[] exps = new Expression[] {max, min};
			Sequence seq = cursor(new String[] {key}).groups(null, null, exps, null, null, new Context());
			return ((BaseRecord)seq.get(1)).getFieldValues();
		}
		
		ObjectReader segmentReader = column.getSegmentReader();
		
		int blockCount = getDataBlockCount();
		Object max, min, obj;
		segmentReader.readLong40();
		min = segmentReader.readObject();
		max = segmentReader.readObject();
		segmentReader.skipObject();
		
		for (int i = 1; i < blockCount; ++i) {
			segmentReader.readLong40();
			obj = segmentReader.readObject();
			if (Variant.compare(obj, min) < 0) {
				min = obj;
			}
			obj = segmentReader.readObject();
			if (Variant.compare(obj, max) > 0) {
				max = obj;
			}
			segmentReader.skipObject();
		}
		return new Object[] {max, min};
	}
	
	/**
	 * ����ÿ���ֶεļ�¼��
	 */
	public long[] getSegmentInfo() {
		BlockLinkReader rowCountReader = getSegmentReader();
		int blockCount = getDataBlockCount();
		long recCountOfSegment[] = new long[blockCount];
		long sum = 0;
		try {
			for (int i = 0; i < blockCount; ++i) {
				sum += rowCountReader.readInt32();
				recCountOfSegment[i] = sum;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				rowCountReader.close();
			} catch (Exception e){};
		}
		return recCountOfSegment;
	}
	
	/**
	 * �ϲ���������ļ�
	 * @param table ��һ�����
	 * @throws IOException
	 */
	public void append(PhyTable other) throws IOException {
		if (!(other instanceof ColPhyTable)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}
		
		ColPhyTable table = (ColPhyTable) other;
		getGroupTable().checkWritable();
		table.getGroupTable().checkReadable();
		
		if (!table.getDataStruct().isCompatible(getDataStruct())) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		if (getModifyRecords() != null || table.getModifyRecords() != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("grouptable.invalidData"));
		}
		
		// ȡ��׷�ӵı��������¼��ά�ֶ�ֵ�����жϺϲ���ı��Ƿ������������
		Object []startValues = null;
		if (isSorted) {
			ColumnMetaData []columns = table.getSortedColumns();
			int count = columns.length;
			startValues = new Object[count];
			for (int i = 0; i < count; ++i) {
				ObjectReader segmentReader = columns[i].getSegmentReader();
				segmentReader.readLong40();
				segmentReader.skipObject();
				segmentReader.skipObject();
				startValues[i] = segmentReader.readObject();
			}
		}
		
		if (hasPrimaryKey) {
			if (!table.hasPrimaryKey || Variant.compareArrays(maxValues, startValues) >= 0) {
				hasPrimaryKey = false;
			}
		}
		
		if (!hasPrimaryKey && isSorted) {
			if (!table.isSorted || Variant.compareArrays(maxValues, startValues) > 0) {
				isSorted = false;
			}
		}
		
		// ׼��д����
		prepareAppend();
		
		ColumnMetaData []columns = this.columns;
		ColumnMetaData []columns2 = table.columns;
		int colCount = columns.length;
		
		BlockLinkReader rowCountReader = table.getSegmentReader();
		BlockLinkReader []colReaders2 = new BlockLinkReader[colCount];
		ObjectReader []segmentReaders2 = new ObjectReader[colCount];
		byte[][] dictBuffer = new byte[colCount][];
		BufferWriter[] bufferWriters = new BufferWriter[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			colReaders2[i] = columns2[i].getColReader(true);
			segmentReaders2[i] = columns2[i].getSegmentReader();
			
			//���ĳ����ȫ���ֵ䣬���������
			Sequence dict = columns2[i].getDict();
			if (dict != null && dict.length() == 0) {
				dictBuffer[i] = null;
			} else {
				BufferWriter bufferWriter = columns[i].getColDataBufferWriter();
				bufferWriter.writeObject(dict);
				dictBuffer[i] = bufferWriter.finish();
				bufferWriters[i] = bufferWriter;
			}
			
		}
		
		
		int blockCount = table.getDataBlockCount();
		for (int i = 0; i < blockCount; ++i) {
			for (int j = 0; j < colCount; j++) {
				if (dictBuffer[j] == null) {
					columns[j].copyColBlock(colReaders2[j], segmentReaders2[j]);
				} else {
					columns[j].copyColBlock(colReaders2[j], segmentReaders2[j], bufferWriters[j], dictBuffer[j]);
				}
			}
			
			//���·ֶ���Ϣbuffer
			appendSegmentBlock(rowCountReader.readInt32());
		}
		
		// ����д���ݣ����浽�ļ�
		finishAppend();
		
		rowCountReader.close();
		for (int i = 0; i < colCount; ++i) {
			colReaders2[i].close();
			segmentReaders2[i].close();
		}
	}
	
	/**
	 * ��ò����ʼ�ļ�¼���ڵĿ��
	 * @return ���
	 */
	public int getFirstBlockFromModifyRecord() {
		long minSeq = Long.MAX_VALUE;
		ArrayList<ModifyRecord> recs = getModifyRecords();
		if (recs != null) {
			for (ModifyRecord rec : recs) {
				long seq = rec.getRecordSeq();
				if (minSeq > seq) {
					minSeq = seq;
				}
			}
		}
		if (minSeq == Long.MAX_VALUE) {
			return -1;
		}
		long []recCountOfSegment = getSegmentInfo();
		int len = recCountOfSegment.length;
		for (int i = 0; i < len; i++) {
			if (minSeq <= recCountOfSegment[i]) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * ���ø���������
	 * @param block ��ֹ���
	 * @return ����ļ�ĩβ�Ľض�λ��
	 */
	public long resetByBlock(int block) {
		BlockLinkReader rowCountReader = getSegmentReader();
		long sum = 0;
		int blockSize = getGroupTable().blockSize;
		try {
			//�ѷֶ���Ϣ������
			for (int i = 0; i < block; ++i) {
				int cnt = rowCountReader.readInt32();
				sum += cnt;
			}

			//�޸ķֶ���Ϣ
			segmentBlockLink.lastBlockPos = rowCountReader.position();
			segmentBlockLink.freeIndex = rowCountReader.getCaret();
			
			//��������
			totalRecordCount = sum;
			dataBlockCount = block;
			//��ղ���
			modifyRecords.clear();
			saveModifyRecords();
			
			maxValues = null;
			//����ÿ���п�
			long resetPos = 0;//��������ļ�ĩβ�Ľض�λ��
			for (ColumnMetaData col : columns) {
				ObjectReader reader;
				BlockLinkReader segmentReader = new BlockLinkReader(col.getSegmentBlockLink());
				try {
					segmentReader.loadFirstBlock();
					reader = new ObjectReader(segmentReader, blockSize - ComTable.POS_SIZE);
				} catch (IOException e) {
					segmentReader.close();
					throw new RQException(e.getMessage(), e);
				}
				
				//��ȡ�п�ķֶ���Ϣ
				for (int i = 0; i < block; ++i) {
					reader.readLong40();
					if (col.hasMaxMinValues()) {
						reader.readObject();
						reader.readObject();
						reader.readObject();
					}
				}
				
				//��д�п�ķֶ���Ϣ
				BlockLink blockLink = col.getSegmentBlockLink();
				blockLink.freeIndex = (int) (reader.position() % blockSize);
				blockLink.lastBlockPos = segmentReader.position();
				
				long tempPos = reader.readLong40();//�����ֹ��ʼ�ĵ�ַ
				if (resetPos < tempPos) {
					resetPos = tempPos;
				}
				reader.close();
				segmentReader.close();

				blockLink = col.getDataBlockLink();
				blockLink.freeIndex = (int) (tempPos % blockSize);
				blockLink.lastBlockPos = tempPos - (tempPos % blockSize);
			}

			if (parent != null) {
				BlockLinkReader segmentReader = new BlockLinkReader(guideColumn.getSegmentBlockLink());
				ObjectReader reader;
				try {
					segmentReader.loadFirstBlock();
					reader = new ObjectReader(segmentReader, blockSize - ComTable.POS_SIZE);
				} catch (IOException e) {
					segmentReader.close();
					throw new RQException(e.getMessage(), e);
				}
				//��ȡ���зֶ���Ϣ
				for (int i = 0; i < block; ++i) {
					reader.readLong40();
				}
				//��д�п�ķֶ���Ϣ
				BlockLink blockLink = guideColumn.getSegmentBlockLink();
				blockLink.freeIndex = (int) (reader.position() % blockSize);
				blockLink.lastBlockPos = segmentReader.position();
				
				long tempPos = reader.readLong40();//�����ֹ��ʼ�ĵ�ַ
				if (resetPos < tempPos) {
					resetPos = tempPos;
				}
				reader.close();
				segmentReader.close();

				blockLink = guideColumn.getDataBlockLink();
				blockLink.freeIndex = (int) (tempPos % blockSize);
				blockLink.lastBlockPos = tempPos - (tempPos % blockSize);
			}
			
			return (resetPos - (resetPos % blockSize) + blockSize);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				rowCountReader.close();
			} catch (Exception e){};
		}
	}
	
	/**
	 * ���field�Ƿ���ά�ֶ�
	 * ����ǣ�����ݱ��ʽ���㷶Χ
	 * ���򷵻�NULL
	 * @param field
	 * @param node ���ʽ��home�ڵ�
	 * @param ctx
	 */
	long[] checkDim(String field, Node node, Context ctx) {
		ColumnMetaData col = getColumn(field);
		if (col == null || !col.isDim()) {
			return null;
		}
		
		int operator = 0;
		if (node instanceof Equals) {
			operator = IFilter.EQUAL;
		} else if (node instanceof Greater) {
			operator = IFilter.GREATER;
		} else if (node instanceof NotSmaller) {
			operator = IFilter.GREATER_EQUAL;
		} else if (node instanceof Smaller) {
			operator = IFilter.LESS;
		} else if (node instanceof NotGreater) {
			operator = IFilter.LESS_EQUAL;
		} else if (node instanceof NotEquals) {
			operator = IFilter.NOT_EQUAL;
		} else {
			return null;
		}
		
		Object value;
		if (node.getRight() instanceof UnknownSymbol) {
			value = node.getLeft().calculate(ctx);
		} else {
			value = node.getRight().calculate(ctx);
		}

		long seq = 0;
		int blockCount = dataBlockCount;
		ColumnFilter filter = new ColumnFilter(col, 0, operator, value);
		LongArray intervals = new LongArray(blockCount);
		BlockLinkReader rowCountReader = getSegmentReader();
		ObjectReader segmentReader = col.getSegmentReader();
		try {
			Object maxValue, minValue;
			for (int i = 0; i < blockCount; ++i) {
				int recordCount = rowCountReader.readInt32();
				segmentReader.readLong40();
				minValue = segmentReader.readObject();
				maxValue = segmentReader.readObject();
				segmentReader.skipObject();
				if (filter.match(minValue, maxValue) && recordCount != 1) {
					intervals.add(seq + 1);
					intervals.add(seq + recordCount);
				}
				seq += recordCount;
			} 
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		if (intervals.size() == 0) {
			return null;
		}
		return intervals.getDatas();
	}
	
	/**
	 * д�뻺�������
	 */
	public void appendCache() throws IOException {
		if (appendCache == null) return;
		
		ICursor cursor = new MemoryCursor(appendCache);
		// ׼��д����
		prepareAppend();
		
		if (parent != null) {
			parent.appendCache();
			appendAttached(cursor);
		} else if (sortedColumns == null) {
			appendNormal(cursor);
		} else if (getSegmentCol() == null) {
			appendSorted(cursor);
		} else {
			appendSegment(cursor);
		}

		appendCache = null;
		// ����д���ݣ����浽�ļ�
		finishAppend();
	}
	
	/**
	 * �����������Ķ�·�α� (���ڼ�Ⱥ�Ľڵ��)
	 * @param exps ȡ���ֶα��ʽ����expsΪnullʱ����fieldsȡ����
	 * @param fields ȡ���ֶε�������
	 * @param filter ���˱��ʽ
	 * @param fkNames ָ��FK���˵��ֶ�����
	 * @param codes ָ��FK���˵���������
	 * @param pathSeq �ڼ���
	 * @param pathCount �ڵ����
	 * @param pathCount2 �ڵ����ָ���Ŀ���
	 * @param opt ѡ��
	 * @param ctx ������
	 * @return
	 */
	public ICursor cursor(Expression []exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes, String[] opts, int pathSeq, int pathCount, int pathCount2, String opt, Context ctx) {
		if (dataBlockCount < pathCount || pathCount2 < 2) {
			//����������ڽڵ�������߽ڵ����ָ���Ŀ�������2
			return cursor(exps, fields, filter, fkNames, codes, opts, pathSeq, pathCount, opt, ctx);
		}
				
		ICursor []cursors = new ICursor[pathCount2];
		
		//�õ����ָ���ǰ�ڵ���Ŀ���
		int avg = dataBlockCount / pathCount;
		int count = avg;
		if (pathSeq == pathCount) {
			count += dataBlockCount % pathCount;
		}
		
		int offset = (pathSeq - 1) * avg;
		if (count < pathCount2) {
			//���Ҫ����Ŀ���С�ڲ�����
			int i = 0;
			for (; i < count; i++) {
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}

				cursors[i] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				((IDWCursor) cursors[i]).setSegment(offset + i, offset + i + 1);
			}
			
			for (; i < pathCount2; i++) {
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}

				cursors[i] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				((IDWCursor) cursors[i]).setSegment(0, -1);
			}
		} else {
			//�õ����ָ���ǰ�ڵ����ÿһ·�Ŀ���
			int avg2 = count / pathCount2;
			for (int i = 0; i < pathCount2; i++) {
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}
				
				cursors[i] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				if (i != pathCount2 - 1) {
					((IDWCursor) cursors[i]).setSegment(offset + avg2 * i, offset + avg2 * (i + 1));
				} else {
					((IDWCursor) cursors[i]).setSegment(offset + avg2 * i, offset + count);
				}
			}
		}

		return new MultipathCursors(cursors, ctx);
	}

	/**
	 * ���ݸ������α꣬����ͬ���ֶεĶ�·�α�
	 * @param exps ȡ���ֶα��ʽ����expsΪnullʱ����fieldsȡ����
	 * @param fields ȡ���ֶε�������
	 * @param filter ���˱��ʽ
	 * @param fkNames ָ��FK���˵��ֶ�����
	 * @param codes ָ��FK���˵���������
	 * @param opts �����ֶν��й�����ѡ��
	 * @param cursor �ο��α�
	 * @param seg �ڵ����
	 * @param endValues β��Ҫ׷�ӵļ�¼
	 * @param opt ѡ��
	 * @param ctx
	 * @return
	 */
	public ICursor cursor(Expression []exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes, String[] opts, ICursor cursor, int seg, Object [][]endValues, String opt, Context ctx) {
		getGroupTable().checkReadable();
		
		ArrayList<ICursor> csList = new ArrayList<ICursor>();
		ICursor []srcs;
		if (cursor instanceof MultipathCursors) {
			srcs = ((MultipathCursors) cursor).getParallelCursors();
		} else {
			srcs = new ICursor[]{cursor};
		}
		
		for (ICursor cs : srcs) {
			if (!(cs instanceof Cursor) && !(cs instanceof JoinTableCursor)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
			}
		}
		
		PhyTable table = ((IDWCursor) srcs[0]).getTableMetaData();
		String []names = table.getAllSortedColNames();
		if (names == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
		}
		String segCol = table.getSegmentCol();
		if (segCol != null) {
			for (int i = names.length - 1; i >= 0; --i) {
				if (names[i].equals(segCol)) {
					String []tmp = new String[i + 1];
					System.arraycopy(names, 0, tmp, 0, i + 1);
					names = tmp;
					break;
				}
			}
		}
		
		int fcount = names.length;
		if (seg != 0) {
			//������ǵ�һ���ڵ��
			BaseRecord rec = new Record(cursor.getDataStruct());
			Sequence tempSeq = new Sequence();
			tempSeq.add(rec);
			csList.add(new MemoryCursor(tempSeq));
		}
		
		int firstNullIndex = -1;
		for (int i = 0, len = srcs.length; i < len; i++) {
			if (srcs[i].peek(1) == null && firstNullIndex < 0) {
				firstNullIndex = i;
			}
			csList.add(srcs[i]);
		}
		
		if (seg + 1 <= endValues.length) {
			//����������һ���ڵ��
			Object []objs = endValues[seg];
			BaseRecord rec = new Record(cursor.getDataStruct());
			for (int f = 0; f < fcount; f++) {
				rec.set(names[f], objs[f]);
			}
			Sequence tempSeq = new Sequence();
			tempSeq.add(rec);
			if (firstNullIndex < 0) {
				csList.add(new MemoryCursor(tempSeq));
			} else {
				csList.add(firstNullIndex, new MemoryCursor(tempSeq));
			}
		}
		
		srcs = new ICursor[csList.size()];
		csList.toArray(srcs);
		
		int segCount = srcs.length;

		ColumnMetaData[] sortedCols = getAllSortedColumns();
		if (sortedCols.length < fcount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
		}
		
		String []dimFields = new String[fcount];
		for (int f = 0; f < fcount; ++f) {
			dimFields[f] = sortedCols[f].getColName();
		}
		
		Object [][]minValues = new Object [segCount][];
		int dataSegCount = segCount; // �����ݵĶ�·�α�·��
		
		for (int i = 0; i < segCount; ++i) {
			Sequence seq = srcs[i].peek(1);
			if (seq == null) {
				dataSegCount = i;
				for (++i; i < segCount; ++i) {
					if (srcs[i].peek(1) != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
					}
				}
				
				break;
			}

			BaseRecord r = (BaseRecord)seq.get(1);
			Object []vals = new Object[fcount];
			minValues[i] = vals;
			for (int f = 0; f < fcount; ++f) {
				vals[f] = r.getFieldValue(names[f]);
			}
		}
		
		ObjectReader []readers = new ObjectReader[fcount];
		for (int f = 0; f < fcount; ++f) {
			readers[f] = sortedCols[f].getSegmentReader();
		}
		
		int blockCount = getDataBlockCount();
		int s = 1;
		Object []blockMinVals = new Object[fcount];
		ICursor []cursors = new ICursor[segCount];
		boolean []isEquals = new boolean[segCount];
		
		//�ÿ��α겹ȫ
		for (int i = dataSegCount; i < segCount; ++i) {
			Cursor cs = new Cursor(this, fields, filter, fkNames, codes, opts, ctx);
			cs.setSegment(0, -1);
			cursors[i] = cs;
		}
		try {
			// ����ͬ���ֶεģ���Ҫ��ͷȥβ
			int startBlock = 0;
			for (int b = 0; b < blockCount && s < dataSegCount; ++b) {
				for (int f = 0; f < fcount; ++f) {
					readers[f].readLong40();
					readers[f].skipObject();
					readers[f].skipObject();
					blockMinVals[f] = readers[f].readObject(); //startValue
				}
				
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}
				
				int cmp = Variant.compareArrays(blockMinVals, minValues[s]);
				if (cmp > 0) {
					ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
					cursors[s - 1] = cs;
					if (b > 0) {
						((IDWCursor) cs).setSegment(startBlock, b - 1);
						startBlock = b - 1;
					} else {
						((IDWCursor) cs).setSegment(startBlock, 0);
						startBlock = 0;
					}
					
					isEquals[s] = false;
					s++;
				} else if (cmp == 0) {
					ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
					cursors[s - 1] = cs;
					((IDWCursor) cs).setSegment(startBlock, b);
					startBlock = b;
					
					isEquals[s] = true;
					s++;
				}
			}
			
			if (s == dataSegCount) {
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}
				
				ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				cursors[s - 1] = cs;
				((IDWCursor) cs).setSegment(startBlock, blockCount);
				
				for (int i = 1; i < s; ++i) {
					if (!isEquals[i]) {
						Sequence seq = fetchToValue((IDWCursor)cursors[i], dimFields, minValues[i]);
						((IDWCursor) cursors[i - 1]).setAppendData(seq);
					}
				}
			} else {
				// ���һ�ε���ʼֵС�ڵ��ڲ��յĶ�·�α���м�ε���ʼֵ
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}
				
				ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				cursors[s - 1] = cs;
				((IDWCursor) cs).setSegment(startBlock, blockCount - 1);
				
				for (int i = 1; i < s; ++i) {
					if (!isEquals[i]) {
						Sequence seq = fetchToValue((IDWCursor)cursors[i], dimFields, minValues[i]);
						((IDWCursor) cursors[i - 1]).setAppendData(seq);
					}
				}
				
				// �������һ���α꣬�����նδ����һ��ȡ����Ӧ��ֵ
				if (filter != null) {
					// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
					filter = filter.newExpression(ctx);
				}
				
				cursors[dataSegCount - 1] = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
				((IDWCursor) cursors[dataSegCount - 1]).setSegment(blockCount - 1, blockCount);
				
				Sequence seq = fetchToValue((IDWCursor)cursors[dataSegCount - 1], dimFields, minValues[s]);
				((IDWCursor) cursors[s - 1]).setAppendData(seq);
				
				for (; s < dataSegCount - 1; ++s) {
					if (filter != null) {
						// �ֶβ��ж�ȡʱ��Ҫ���Ʊ��ʽ��ͬһ�����ʽ��֧�ֲ�������
						filter = filter.newExpression(ctx);
					}
					
					cursors[s] = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
					((IDWCursor) cursors[s]).setSegment(blockCount - 1, blockCount - 1);
					//cursors[s] = new MemoryCursor(null);
					
					seq = fetchToValue((IDWCursor)cursors[dataSegCount - 1], dimFields, minValues[s + 1]);
					((IDWCursor) cursors[s]).setAppendData(seq);
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		csList.clear();
		int len = cursors.length - 1;
		if (seg != 1) {
			for (int i = 1; i < len; i++) {
				csList.add(cursors[i]);
			}
		} else {
			for (int i = 0; i < len; i++) {
				csList.add(cursors[i]);
			}
		}
		if (seg + 1 > endValues.length) {
			csList.add(cursors[len]);
		}
		
		cursors = new ICursor[csList.size()];
		csList.toArray(cursors);
		return new MultipathCursors(cursors, ctx);
	}
	
	/**
	 * ���һ��
	 * @param colName ����
	 * @param exp ��ֵ���ʽ
	 * @param ctx 
	 */
	public void addColumn(String colName, Expression exp, Context ctx) {
		//����в���������reset
		if (getModifyRecords() != null) {
			groupTable.reset(null, null, ctx, null);
		}
		
		//������Ƿ��Ѿ�����
		ColumnMetaData existCol = getColumn(colName);
		if (null != existCol) {
			if (existCol.isDim() || existCol.isKey() || this.getModifyRecords() != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("alter" + mm.getMessage("dw.columnNotEditable"));
			}
		}
		
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			// �в��ļ�ʱ�ȴ����ļ�
			tmd.addColumn(colName, exp, ctx);
		}
		
		//�½���һ����
		ColumnMetaData col = new ColumnMetaData(this, colName, false, false);
		ICursor cursor = cursor();
		BlockLinkReader rowCountReader = getSegmentReader();
		
		try {
			//��ʼ���п�
			col.applySegmentFirstBlock();
			col.applyDataFirstBlock();
			col.prepareWrite();
			int curBlock = 0, endBlock = getDataBlockCount();
			
			//����ÿ�εļ�¼����������ȡ������������exp����
			while (curBlock < endBlock) {
				curBlock++;
				
				//ȡ��������
				int recordCount = rowCountReader.readInt32();
				Sequence data = (Sequence) cursor.fetch(recordCount);
				data = data.newTable(new String[] {""}, new Expression[]{exp}, ctx);
				
				//�Ѽ���õ����ݣ�д������
				Object []minValues = new Object[1];//һ�����Сάֵ
				Object []maxValues = new Object[1];//һ������άֵ
				Object []startValues = new Object[1];
				int[] dataTypeInfo = new int[1];
				
				BufferWriter bufferWriter = col.getColDataBufferWriter();
				Sequence dict = col.getDict();
				int len = data.length();
				DataBlockWriterJob.writeDataBlock(bufferWriter, data, dict, 0, 1, len, maxValues, minValues, startValues, dataTypeInfo);
				
				//ͳ������������
				boolean doCheck = groupTable.isCheckDataPure();
				col.adjustDataType(dataTypeInfo[0], doCheck);
				
				//�ύ�п�buffer
				col.appendColBlock(bufferWriter.finish(), minValues[0], maxValues[0], startValues[0]);
			}
			
			col.finishWrite();
			cursor.close();
			
			if (existCol != null) {
				//�滻
				int len = columns.length;
				for (int i = 0; i < len; i++) {
					ColumnMetaData column = columns[i];
					if (column.getColName().equals(colName)) {
						columns[i] = col;
						break;
					}
				}
				groupTable.save();
			} else {
				//׷��
				ColumnMetaData []newColumns = java.util.Arrays.copyOf(columns, columns.length + 1);
				String []newColNames = java.util.Arrays.copyOf(colNames, colNames.length + 1);
				newColumns[columns.length] = col;
				newColNames[columns.length] = colName;
				columns = newColumns;
				colNames = newColNames;
				groupTable.save();
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	public void deleteColumn(String colName) {
		//����в���������reset
		if (getModifyRecords() != null) {
			groupTable.reset(null, null, new Context(), null);
		}
				
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			// �в��ļ�ʱ��ɾ�����ļ��е�
			tmd.deleteColumn(colName);
		}
		
		ColumnMetaData col = getColumn(colName);
		
		//������Ƿ����
		if (col == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("dw.columnNotExist"));
		}
		
		//����Ƿ���ɾ��ά�л�������
		if (col.isDim() || col.isKey()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("dw.columnNotEditable"));
		}
		
		ColumnMetaData []newColumns = new ColumnMetaData[columns.length - 1];
		String []newColNames = new String[colNames.length - 1];
		int i = 0;
		for (ColumnMetaData cmd : columns) {
			if (cmd != col) {//�����õ������öԱ�
				newColumns[i] = cmd;
				newColNames[i++] = cmd.getColName();
			}
		}
		
		columns = newColumns;
		colNames = newColNames;
		
		try {
			groupTable.save();
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}