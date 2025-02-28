package com.scudata.cellset.datamodel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.IColCell;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IRowCell;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.common.DBSession;
import com.scudata.common.Logger;
import com.scudata.common.MD5;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.FileObject;
import com.scudata.dm.IQueryable;
import com.scudata.dm.JobSpace;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Machines;
import com.scudata.dm.ParallelCaller;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.RetryException;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.Channel;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.ParamParser;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.CursorLooper;
import com.scudata.thread.Job;
import com.scudata.thread.ThreadPool;
import com.scudata.util.Variant;

// ������
public class PgmCellSet extends CellSet {
	private static final long serialVersionUID = 0x02010010;
	public static final int PRIVILEGE_FULL = 0; // ��ȫ���ƣ��������κ�����
	public static final int PRIVILEGE_EXEC = 1; // ֻ����ִ��

	private static final int SIGN_AUTOCALC = 0x00000010; // �Զ�����
	private static final int SIGN_DYNAMICPARAM = 0x00000010; // �ű����һ������Ϊ��̬����

	private int sign = 0;
	private ByteMap customPropMap; // �Զ�������

	// private String []psws = new String[2]; // 2�����룬�߼�����ǰ
	private String pswHash; // ����hashֵ
	private int nullPswPrivilege = PRIVILEGE_EXEC; // �������½ʱ��Ȩ��
	transient private int curPrivilege = PRIVILEGE_FULL; // ��ǰ��Ȩ��

	transient protected CellLocation curLct; // ��ǰ����ĵ�Ԫ���λ��
	transient private Object curDb; // DBObject��this��this��ʾ�ļ���dql

	transient private LinkedList<CmdCode> stack = new LinkedList<CmdCode>(); // �����ջ
	transient private CellLocation parseLct; // ��ǰ���ڽ������ʽ�ĵ�Ԫ��

	transient private Sequence resultValue; // result��䷵��ֵ������ĻḲ��ǰ���
	transient private int resultCurrent;
	transient private CellLocation resultLct; // result������ڵ�Ԫ��

	transient private boolean interrupt; // �Ƿ���ù�interrupt
	transient private boolean isInterrupted; // ����ʱʹ�ã���ͣ�������
	transient private boolean hasReturn = false;

	transient private String name; // ��������DfxManager��ʹ��

	// func fn(arg,��)
	transient private HashMap<String, FuncInfo> fnMap; // [������, ������Ϣ]ӳ��
	transient private ForkCmdCode forkCmdCode; // ��ǰִ�е�fork�����

	private String isvHash; // �й��ܵ�14��KIT��ʱ��д��dfxʱ��Ҫд����Ȩ�ļ���isv��MD5ֵ

	private static class CmdCode {
		protected byte type; // Command����
		protected int row; // Command���к�
		protected int col; // Command���к�
		protected int blockEndRow; // Command�Ĵ����������к�

		public CmdCode(byte type, int r, int c, int endRow) {
			this.type = type;
			this.row = r;
			this.col = c;
			this.blockEndRow = endRow;
		}
	}

	private static abstract class ForCmdCode extends CmdCode {
		protected int seq = 0; // forѭ�����

		public ForCmdCode(int r, int c, int endRow) {
			super(Command.FOR, r, c, endRow);
		}

		abstract public boolean hasNextValue();

		abstract public Object nextValue();

		public Object endValue() {
			return null;
		}

		public int getSeq() {
			return seq;
		}

		public void setSeq(int n) {
			this.seq = n;
		}
	}
	
	private static class ForkCmdCode extends CmdCode {
		protected int seq = 0; // fork�߳����

		public ForkCmdCode(int r, int c, int endRow, int seq) {
			super(Command.FORK, r, c, endRow);
			this.seq = seq;
		}
	}
	
	private static class EndlessForCmdCode extends ForCmdCode {
		public EndlessForCmdCode(int r, int c, int endRow) {
			super(r, c, endRow);
		}

		public boolean hasNextValue() {
			return true;
		}

		public Object nextValue() {
			return new Integer(++seq);
		}
	}

	private static class SequenceForCmdCode extends ForCmdCode {
		private Sequence sequence;

		public SequenceForCmdCode(int r, int c, int endRow, Sequence sequence) {
			super(r, c, endRow);
			this.sequence = sequence;
		}

		public boolean hasNextValue() {
			return seq < sequence.length();
		}

		public Object nextValue() {
			return sequence.get(++seq);
		}
	}

	private static class BoolForCmdCode extends ForCmdCode {
		private Expression exp;
		private Context ctx;

		public BoolForCmdCode(int r, int c, int endRow, Expression exp,
				Context ctx) {
			super(r, c, endRow);
			this.exp = exp;
			this.ctx = ctx;
		}

		public boolean hasNextValue() {
			Object value = exp.calculate(ctx);
			if (!(value instanceof Boolean)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.forVarTypeError"));
			}

			return ((Boolean) value).booleanValue();
		}

		public Object nextValue() {
			++seq;
			return Boolean.TRUE;
		}

		public Object endValue() {
			return Boolean.FALSE;
		}
	}

	private static class IntForCmdCode extends ForCmdCode {
		private int start;
		private int end;
		private int step;

		public IntForCmdCode(int r, int c, int endRow, int start, int end,
				int step) {
			super(r, c, endRow);
			this.start = start;
			this.end = end;
			this.step = step;
		}

		public boolean hasNextValue() {
			if (step >= 0) {
				return start <= end;
			} else {
				return start >= end;
			}
		}

		public Object nextValue() {
			Object val = new Integer(start);

			++seq;
			start += step;
			return val;
		}

		public Object endValue() {
			return new Integer(start);
		}
	}

	private static class CursorForCmdCode extends ForCmdCode {
		private ICursor cursor;
		private int count;
		private Expression gexp;
		private Context ctx;
		private Sequence table;

		// private boolean bi = false; // �Ƿ���group@i

		public CursorForCmdCode(int r, int c, int endRow, ICursor cursor,
				int count, Expression gexp, Context ctx) {
			super(r, c, endRow);
			this.cursor = cursor;
			this.count = count;
			this.gexp = gexp;
			this.ctx = ctx;
		}

		public boolean hasNextValue() {
			if (gexp == null) {
				table = cursor.fetch(count);
			} else {
				table = cursor.fetchGroup(gexp, ctx);
			}

			return table != null && table.length() > 0;
		}

		public Object nextValue() {
			++seq;
			return table;
		}

		public void close() {
			cursor.close();
		}
	}

	private static class ForkJob extends Job {
		PgmCellSet pcs;
		int row;
		int col;
		int endRow;

		public ForkJob(PgmCellSet pcs, int row, int col, int endRow) {
			this.pcs = pcs;
			this.row = row;
			this.col = col;
			this.endRow = endRow;
		}

		public void run() {
			pcs.executeFork(row, col, endRow);
		}

		public Object getResult() {
			return pcs.getForkResult();
		}
	}

	private class SubForkJob extends Job {
		private IParam param;
		private int row;
		private int col;
		private int endRow;
		private Context ctx;

		public SubForkJob(IParam param, int row, int col, int endRow,
				Context ctx) {
			this.param = param;
			this.row = row;
			this.col = col;
			this.endRow = endRow;
			this.ctx = ctx;
		}

		public void run() {
			runForkCmd(param, row, col, endRow, ctx);
		}
	}

	/**
	 * �����ж���ĺ�����Ϣ
	 * @author RunQian
	 *
	 */
	public class FuncInfo {
		private String fnName;
		private String option;
		private PgmNormalCell cell; // �������ڵ�Ԫ��
		private String[] argNames; // ������

		public FuncInfo(String fnName, PgmNormalCell cell, String[] argNames) {
			this.fnName = fnName;
			this.cell = cell;
			this.argNames = argNames;
		}
		
		public FuncInfo(String fnName, PgmNormalCell cell, String[] argNames, String option) {
			this.fnName = fnName;
			this.cell = cell;
			this.argNames = argNames;
			this.option = option;
		}

		public String getFnName() {
			return fnName;
		}

		/**
		 * ȡ�������ڵĵ�Ԫ��
		 * @return
		 */
		public PgmNormalCell getCell() {
			return cell;
		}

		/**
		 * ȡ�����Ĳ�����
		 * @return
		 */
		public String[] getArgNames() {
			return argNames;
		}

		public String getOption() {
			return option;
		}

		public void setOption(String option) {
			this.option = option;
		}
		
		public boolean hasOptParam() {
			return option != null && option.indexOf('o') != -1;
		}
		
		public Object execute(Object[] args, String opt) {
			return executeFunc(this, args, opt);
		}
	}

	public PgmCellSet() {
	}

	/**
	 * ����һ��ָ�������������ı��
	 * @param row int ����
	 * @param col int ����
	 */
	public PgmCellSet(int row, int col) {
		super(row, col);
	}

	public NormalCell newCell(int r, int c) {
		return new PgmNormalCell(this, r, c);
	}

	public RowCell newRowCell(int r) {
		return new RowCell(r);
	}

	public ColCell newColCell(int c) {
		return new ColCell(c);
	}

	public PgmNormalCell getPgmNormalCell(int row, int col) {
		return (PgmNormalCell) cellMatrix.get(row, col);
	}

	public INormalCell getCurrent() {
		return curLct == null ? null : getNormalCell(curLct.getRow(),
				curLct.getCol());
	}

	public void setCurrent(INormalCell cell) {
		if (cell == null) {
			curLct = null;
		} else {
			if (curLct == null) {
				curLct = new CellLocation(cell.getRow(), cell.getCol());
			} else {
				curLct.set(cell.getRow(), cell.getCol());
			}
		}
	}

	// ����һ���µ�������������Դ���ĵ�Ԫ��ӵ���Լ��ļ��㻷��
	public PgmCellSet newCalc() {
		Matrix m1 = cellMatrix;
		int colSize = cellMatrix.getColSize();
		int rowSize = cellMatrix.getRowSize();

		PgmCellSet pcs = new PgmCellSet();
		Matrix m2 = new Matrix(rowSize, colSize);
		pcs.cellMatrix = m2;

		for (int r = 0; r < rowSize; ++r) {
			for (int c = 0; c < colSize; ++c) {
				m2.set(r, c, m1.get(r, c));
			}
		}

		pcs.sign = sign;
		pcs.pswHash = pswHash;
		pcs.nullPswPrivilege = nullPswPrivilege;
		Context ctx = getContext();
		pcs.setContext(ctx.newComputeContext());
		pcs.name = name;
		return pcs;
	}

	// �����µ�����cursor(c,��)ʹ��
	public PgmCellSet newCursorDFX(INormalCell cell, Object[] args) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		PgmCellSet newPcs = new PgmCellSet(rowCount, colCount);

		int row = cell.getRow();
		int col = cell.getCol();
		int endRow = getCodeBlockEndRow(row, col);

		// �������ĸ���ֻ���ø�ֵ�������ñ��ʽ
		for (int r = 1; r < row; ++r) {
			for (int c = 1; c <= colCount; ++c) {
				Object val = getPgmNormalCell(r, c).getValue();
				newPcs.getPgmNormalCell(r, c).setValue(val);
			}
		}

		for (int r = endRow + 1; r <= rowCount; ++r) {
			for (int c = 1; c <= colCount; ++c) {
				Object val = getPgmNormalCell(r, c).getValue();
				newPcs.getPgmNormalCell(r, c).setValue(val);
			}
		}

		for (int r = row; r <= endRow; ++r) {
			for (int c = 1; c < col; ++c) {
				Object val = getPgmNormalCell(r, c).getValue();
				newPcs.getPgmNormalCell(r, c).setValue(val);
			}

			for (int c = col; c <= colCount; ++c) {
				INormalCell tmp = getCell(r, c);
				INormalCell cellClone = (INormalCell) tmp.deepClone();
				cellClone.setCellSet(newPcs);
				newPcs.setCell(r, c, cellClone);
			}
		}

		// �Ѳ���ֵ�赽func��Ԫ���ϼ�����ĸ���
		if (args != null) {
			int paramRow = row;
			int paramCol = col;
			for (int i = 0, pcount = args.length; i < pcount; ++i) {
				newPcs.getPgmNormalCell(paramRow, paramCol).setValue(args[i]);
				if (paramCol < colCount) {
					paramCol++;
				} else {
					break;
					// if (paramRow == getRowCount() && i < paramCount - 1) {
					// MessageManager mm = EngineMessage.get();
					// throw new RQException("call" +
					// mm.getMessage("function.paramCountNotMatch"));
					// }
					// paramRow++;
					// paramCol = 1;
				}
			}
		}

		newPcs.setContext(getContext());
		newPcs.setCurrent(cell);
		newPcs.setNext(row, col + 1, false);
		newPcs.name = name;
		return newPcs;
	}

	/**
	 * ��ȿ�¡
	 * @return ��¡���Ķ���
	 */
	public Object deepClone() {
		PgmCellSet pcs = new PgmCellSet();

		int colSize = cellMatrix.getColSize();
		int rowSize = cellMatrix.getRowSize();
		pcs.cellMatrix = new Matrix(rowSize, colSize);
		;

		for (int col = 1; col < colSize; col++) {
			for (int row = 1; row < rowSize; row++) {
				INormalCell cell = getCell(row, col);
				INormalCell cellClone = (INormalCell) cell.deepClone();
				cellClone.setCellSet(pcs);
				pcs.cellMatrix.set(row, col, cellClone);
			}
		}

		// ���׸�����׸�
		for (int col = 1; col < colSize; col++)
			pcs.cellMatrix.set(0, col, getColCell(col).deepClone());
		for (int row = 1; row < rowSize; row++)
			pcs.cellMatrix.set(row, 0, getRowCell(row).deepClone());

		ParamList param = getParamList();
		if (param != null) {
			pcs.setParamList((ParamList) param.deepClone());
		}

		pcs.sign = sign;
		if (customPropMap != null) {
			pcs.customPropMap = (ByteMap) customPropMap.deepClone();
		}

		pcs.pswHash = pswHash;
		pcs.nullPswPrivilege = nullPswPrivilege;
		pcs.name = name;
		return pcs;
	}

	/**
	 * д���ݵ���
	 * @param out ObjectOutput �����
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeByte(2);
		out.writeInt(sign); // ���뱨��4����
		out.writeObject(customPropMap);
		out.writeObject(pswHash);
		out.writeInt(nullPswPrivilege);
		
		out.writeObject(name); // �汾2д��
	}

	/**
	 * �����ж�����
	 * @param in ObjectInput ������
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		int v = in.readByte();
		sign = in.readInt();
		customPropMap = (ByteMap) in.readObject();
		pswHash = (String) in.readObject();
		nullPswPrivilege = in.readInt();
		
		if (v > 1) {
			name = (String)in.readObject();
		}
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();

		// ���л���Ԫ�����
		int rowCount = getRowCount();
		int colCount = getColCount();
		out.writeInt(rowCount);
		out.writeInt(colCount);
		for (int row = 1; row <= rowCount; ++row) {
			IRowCell rc = getRowCell(row);
			out.writeRecord(rc);
		}
		for (int col = 1; col <= colCount; ++col) {
			IColCell cc = getColCell(col);
			out.writeRecord(cc);
		}
		for (int row = 1; row <= rowCount; ++row) {
			for (int col = 1; col <= colCount; ++col) {
				INormalCell nc = getCell(row, col);
				out.writeRecord(nc);
			}
		}

		out.writeRecord(paramList);
		out.writeInt(sign); // ���뱨��4����
		out.writeRecord(customPropMap);

		out.writeStrings(null); // Ϊ�˼���psws
		out.writeInt(0); // ����֮ǰ�Ļ���

		out.writeString(pswHash);
		out.writeInt(nullPswPrivilege);

		isvHash = null;
		out.writeString(isvHash);
		return out.toByteArray();
	}

	/**
	 * �����ж�����
	 * @param buf byte[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);

		// ���ɵ�Ԫ�����
		int rowCount = in.readInt();
		int colCount = in.readInt();
		cellMatrix = new Matrix(rowCount + 1, colCount + 1);
		for (int row = 1; row <= rowCount; ++row) {
			RowCell rc = (RowCell) in.readRecord(newRowCell(row));
			cellMatrix.set(row, 0, rc);
		}
		for (int col = 1; col <= colCount; ++col) {
			ColCell cc = (ColCell) in.readRecord(newColCell(col));
			cellMatrix.set(0, col, cc);
		}
		for (int row = 1; row <= rowCount; ++row) {
			for (int col = 1; col <= colCount; ++col) {
				NormalCell nc = (NormalCell) in.readRecord(newCell(row, col));
				cellMatrix.set(row, col, nc);
			}
		}

		paramList = (ParamList) in.readRecord(new ParamList());
		sign = in.readInt();
		customPropMap = (ByteMap) in.readRecord(new ByteMap());

		if (in.available() > 0) {
			in.readStrings();
			if (in.available() > 0) {
				in.readInt(); // ����֮ǰ�Ļ���
				if (in.available() > 0) {
					pswHash = in.readString();
					nullPswPrivilege = in.readInt();
					if (in.available() > 0) {
						isvHash = in.readString();
					}
				}
			}
		}
	}

	// ����else�����
	private void skipCodeBlock() {
		int curRow = curLct.getRow();
		int curCol = curLct.getCol();
		int endBlock = getCodeBlockEndRow(curRow, curCol);

		// ��һ��Ҫִ�еĵ�Ԫ���赽������ĵ�Ԫ��
		setNext(endBlock + 1, 1, true);
	}

	// ����һ��Ҫִ�еĸ��赽if����Ӧ��else������һ��
	// ��ǰ��Ϊif����
	private void toElseCmd() {
		int curRow = curLct.getRow();
		int curCol = curLct.getCol();
		int totalCol = getColCount();

		int level = 0;
		Command command;

		// �ڱ�����Ѱ��else��֧
		for (int c = curCol + 1; c <= totalCol; ++c) {
			PgmNormalCell cell = getPgmNormalCell(curRow, c);
			if ((command = cell.getCommand()) != null) {
				byte type = command.getType();
				if (type == Command.ELSE) {
					if (level == 0) { // �ҵ���Ӧ��else��֧
						setNext(curRow, c + 1, false);
						return;
					} else {
						level--;
					}
				} else if (type == Command.ELSEIF) {
					if (level == 0) { // �ҵ���Ӧ��elseif��֧
						setNext(curRow, c, false);
						runIfCmd(cell, command);
						return;
					}
				} else if (type == Command.IF) {
					level++;
				}
			}
		}

		// ���������
		int endBlock = getCodeBlockEndRow(curRow, curCol);
		int nextRow = endBlock + 1;
		if (nextRow <= getRowCount()) {
			for (int c = 1; c <= totalCol; ++c) {
				PgmNormalCell cell = getPgmNormalCell(nextRow, c);
				if (!cell.isBlankCell()) {
					if (c != curCol) { // û��else��֧
						setNext(nextRow, c, true);
					} else {
						command = cell.getCommand();
						if (command == null) {
							setNext(nextRow, c, true);
						} else {
							byte type = command.getType();
							if (type == Command.ELSE) { // �ҵ���Ӧ��else��֧
								setNext(nextRow, c + 1, false);
							} else if (type == Command.ELSEIF) { // �ҵ���Ӧ��elseif��֧
								setNext(nextRow, c, false);
								runIfCmd(cell, command);
							} else {
								setNext(nextRow, c, true);
							}
						}
					}
					return;
				}
			}
		} else {
			setNext(nextRow, 1, true); // ������ѭ��������
		}
	}

	private int getIfBlockEndRow(int prow, int pcol) {
		int level = 0;
		int totalCol = getColCount();
		Command command;

		// �ڱ�����Ѱ��else��֧
		for (int c = pcol + 1; c <= totalCol; ++c) {
			PgmNormalCell cell = getPgmNormalCell(prow, c);
			if ((command = cell.getCommand()) != null) {
				byte type = command.getType();
				if (type == Command.ELSE) {
					if (level == 0) { // �ҵ���Ӧ��else��֧
						return prow;
					} else {
						level--;
					}
				} else if (type == Command.ELSEIF) {
					if (level == 0) { // �ҵ���Ӧ��elseif��֧
						return prow;
					}
				} else if (type == Command.IF) {
					level++;
				}
			}
		}

		// ���������
		int endBlock = getCodeBlockEndRow(prow, pcol);
		int totalRow = getRowCount();
		if (endBlock < totalRow) {
			int nextRow = endBlock + 1;
			for (int c = 1; c <= totalCol; ++c) {
				PgmNormalCell cell = getPgmNormalCell(nextRow, c);
				if (cell.isBlankCell())
					continue;

				command = cell.getCommand();
				if (command == null) {
					return endBlock;
				} else {
					byte type = command.getType();
					if (type == Command.ELSE) { // �ҵ���Ӧ��else��֧
						return getCodeBlockEndRow(nextRow, c);
					} else if (type == Command.ELSEIF) { // �ҵ���Ӧ��elseif��֧
						return getIfBlockEndRow(nextRow, c);
					} else {
						return endBlock;
					}
				}
			}
			throw new RuntimeException();
		} else {
			return endBlock;
		}
	}

	// ִ��if�����
	private void runIfCmd(NormalCell cell, Command command) {
		Context ctx = getContext();
		Expression exp = command.getExpression(this, ctx);
		if (exp == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("if" + mm.getMessage("function.invalidParam"));
		}

		Object value = exp.calculate(ctx);
		cell.setValue(value);
		if (Variant.isTrue(value)) {
			setNext(curLct.getRow(), curLct.getCol() + 1, false);
		} else {
			toElseCmd();
		}
	}

	/**
	 * ����for��Ԫ��ǰ��ѭ����� #A1
	 * @param r int for��Ԫ����к�
	 * @param c int for��Ԫ����к�
	 * @return int
	 */
	public int getForCellRepeatSeq(int r, int c) {
		for (int i = 0; i < stack.size(); ++i) {
			CmdCode cmd = stack.get(i);
			if (cmd.row == r && cmd.col == c) {
				if (cmd.type == Command.FOR) {
					return ((ForCmdCode) cmd).getSeq();
				} else {
					break;
				}
			}
		}

		if (forkCmdCode != null && forkCmdCode.row == r && forkCmdCode.col == c) {
			return forkCmdCode.seq;
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException("#" + CellLocation.getCellId(r, c)
				+ mm.getMessage("engine.needInFor"));
	}

	// ִ��for�����
	private void runForCmd(NormalCell cell, Command command) {
		int row = curLct.getRow();
		int col = curLct.getCol();
		if (stack.size() > 0) {
			CmdCode cmd = stack.getFirst();
			if (cmd != null && cmd.row == row && cmd.col == col) {
				// ִ����һ��ѭ��
				ForCmdCode forCmd = (ForCmdCode) cmd;
				if (forCmd.hasNextValue()) {
					cell.setValue(forCmd.nextValue());
					setNext(row, col + 1, false); // ִ����һ��Ԫ��
				} else {
					// ����ѭ��
					cell.setValue(forCmd.endValue());
					stack.removeFirst();
					setNext(cmd.blockEndRow + 1, 1, true);
				}

				return;
			}
		}

		// �״�ִ��ѭ��������ѭ������
		ForCmdCode cmdCode;
		Context ctx = getContext();
		int endRow = getCodeBlockEndRow(row, col);
		Expression exp = command.getExpression(this, ctx);
		if (exp == null) {
			cmdCode = new EndlessForCmdCode(row, col, endRow);
		} else {
			Object value = exp.calculate(ctx);
			if (value instanceof Number) {
				IParam param = command.getParam(this, ctx);
				if (param.isLeaf()) {
					cmdCode = new IntForCmdCode(row, col, endRow, 1,
							((Number) value).intValue(), 1);
				} else {
					int size = param.getSubSize();
					if (size > 3) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub = param.getSub(1);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					Object obj = sub.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(
								mm.getMessage("engine.forVarTypeError"));
					}

					int start = ((Number) value).intValue();
					int end = ((Number) obj).intValue();
					int step;
					if (size > 2) {
						sub = param.getSub(2);
						if (sub == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("for"
									+ mm.getMessage("function.invalidParam"));
						}

						obj = sub.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(
									mm.getMessage("engine.forVarTypeError"));
						}

						step = ((Number) obj).intValue();
					} else {
						if (start <= end) {
							step = 1;
						} else {
							step = -1;
						}
					}

					cmdCode = new IntForCmdCode(row, col, endRow, start, end,
							step);
				}
			} else if (value instanceof Sequence) {
				cmdCode = new SequenceForCmdCode(row, col, endRow,
						(Sequence) value);
			} else if (value instanceof Boolean) {
				cell.setValue(value);
				if (((Boolean) value).booleanValue()) {
					cmdCode = new BoolForCmdCode(row, col, endRow, exp, ctx);
					cmdCode.setSeq(1);
					stack.addFirst(cmdCode);
					setNext(row, col + 1, false);
				} else {
					setNext(endRow + 1, 1, true); // ����ѭ��
				}

				return;
			} else if (value instanceof ICursor) {
				IParam param = command.getParam(this, ctx);
				int count = ICursor.FETCHCOUNT;
				Expression gexp = null;

				if (param.getType() == IParam.Semicolon) {
					if (param.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub = param.getSub(1);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					} else if (sub.isLeaf()) {
						gexp = sub.getLeafExpression();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}
				} else if (!param.isLeaf()) {
					if (param.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub = param.getSub(1);
					if (sub != null) {
						Object countObj = sub.getLeafExpression()
								.calculate(ctx);
						if (!(countObj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(
									mm.getMessage("engine.forVarTypeError"));
						}

						count = ((Number) countObj).intValue();
						if (count < 1) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("for"
									+ mm.getMessage("function.invalidParam"));
						}
					}
				}

				cmdCode = new CursorForCmdCode(row, col, endRow,
						(ICursor) value, count, gexp, ctx);
			} else if (value == null) {
				cmdCode = null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.forVarTypeError"));
			}
		}

		if (cmdCode != null && cmdCode.hasNextValue()) {
			cell.setValue(cmdCode.nextValue());
			stack.addFirst(cmdCode);
			setNext(row, col + 1, false);
		} else {
			setNext(endRow + 1, 1, true); // ����ѭ��
		}
	}

	private void runContinueCmd(Command command) {
		CellLocation forLct = command.getCellLocation(getContext());
		int index = -1;

		for (int i = 0, size = stack.size(); i < size; ++i) {
			CmdCode cmd = stack.get(i);
			if (cmd.type == Command.FOR) {
				if (forLct == null
						|| (forLct.getRow() == cmd.row && forLct.getCol() == cmd.col)) {
					index = i;
					break;
				}
			}
		}

		if (index == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("next" + mm.getMessage("engine.needInFor"));
		}

		for (int i = 0; i < index; ++i) {
			// �����ڲ�forѭ��
			CmdCode cmd = stack.removeFirst();
			if (cmd.type == Command.FOR) {
				endForCommand((ForCmdCode) cmd);
			}
		}

		CmdCode cmd = stack.getFirst();
		setNext(cmd.row, cmd.col, false);
	}

	private void runBreakCmd(Command command) {
		CellLocation forLct = command.getCellLocation(getContext());
		int index = -1;

		for (int i = 0, size = stack.size(); i < size; ++i) {
			CmdCode cmd = stack.get(i);
			if (cmd.type == Command.FOR) {
				if (forLct == null
						|| (forLct.getRow() == cmd.row && forLct.getCol() == cmd.col)) {
					index = i;
					break;
				}
			}
		}

		if (index == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("break" + mm.getMessage("engine.needInFor"));
		}

		for (int i = 0; i < index; ++i) {
			// �����ڲ�forѭ��
			CmdCode cmd = stack.removeFirst();
			if (cmd.type == Command.FOR) {
				endForCommand((ForCmdCode) cmd);
			}
		}

		CmdCode cmd = stack.removeFirst();
		endForCommand((ForCmdCode) cmd);
		setNext(cmd.blockEndRow + 1, 1, true);
	}

	private void runGotoCmd(Command command) {
		CellLocation lct = command.getCellLocation(getContext());
		if (lct == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(command.getLocation()
					+ mm.getMessage("cellset.cellNotExist"));
		}

		int r = lct.getRow();
		int c = lct.getCol();
		int index = -1;

		for (int i = 0, size = stack.size(); i < size; ++i) {
			CmdCode cmd = stack.get(i);
			if (r > cmd.blockEndRow || r < cmd.row) {
				index = i;
			} else if (c <= cmd.col) {
				if (r == cmd.row) {
					index = i;
				} else {
					// ��������ѭ���ڵ�ǰ��Ŀհ׸�
					MessageManager mm = EngineMessage.get();
					throw new RQException(
							mm.getMessage("cellset.invalidGotoCell"));
				}
			} else {
				break;
			}
		}

		for (int i = 0; i <= index; ++i) {
			// �����ڲ�forѭ��
			CmdCode cmd = stack.removeFirst();
			if (cmd.type == Command.FOR) {
				endForCommand((ForCmdCode) cmd);
			}
		}

		setNext(r, c, false);
	}

	// �������е�ѭ�����
	private void endForCommand(ForCmdCode cmd) {
		if (cmd instanceof CursorForCmdCode) {
			((CursorForCmdCode) cmd).close();
		}
	}

	// ��������ִ��fork���������
	private PgmCellSet newForkPgmCellSet(int row, int col, int endRow,
			Context ctx, boolean isLocal) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		PgmCellSet pcs = new PgmCellSet(rowCount, colCount);

		if (isLocal) {
			for (int r = 1; r <= rowCount; ++r) {
				for (int c = 1; c <= colCount; ++c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					PgmNormalCell newCell = pcs.getPgmNormalCell(r, c);
					newCell.setExpString(cell.getExpString());
					newCell.setValue(cell.getValue());
				}
			}

			pcs.setContext(ctx.newComputeContext());
		} else {
			// �������ʱ����������˱�����fork�������ĸ���������Щ�����͸���ֵ
			ParamList usedParams = new ParamList();
			ArrayList<INormalCell> usedCells = new ArrayList<INormalCell>();

			for (int r = row; r <= endRow; ++r) {
				for (int c = col + 1; c <= colCount; ++c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					cell.getUsedParamsAndCells(usedParams, usedCells);
					PgmNormalCell newCell = pcs.getPgmNormalCell(r, c);
					newCell.setExpString(cell.getExpString());
				}
			}

			pcs.setParamList(usedParams);
			for (INormalCell cell : usedCells) {
				int r = cell.getRow();
				int c = cell.getCol();
				if (r < row || r > endRow || c < col) {
					pcs.getPgmNormalCell(r, c).setValue(cell.getValue());
				}
			}
		}

		pcs.name = name;
		return pcs;
	}

	private void executeFork(int row, int col, int endRow) {
		curLct = new CellLocation(row, col);
		setNext(row, col + 1, false);
		CellLocation lct = curLct;
		if (lct == null || lct.getRow() > endRow)
			return;

		do {
			lct = runNext2();
		} while (lct != null && lct.getRow() <= endRow && resultValue == null);

		if (resultValue == null) {
			// δ����result��endȱʡ���ش��������һ�������ֵ
			int colCount = getColCount();
			for (int r = endRow; r >= row; --r) {
				for (int c = colCount; c > col; --c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					if (cell.isCalculableCell() || cell.isCalculableBlock()) {
						Object val = cell.getValue();
						resultValue = new Sequence(1);
						resultValue.add(val);
						return;
					}
				}
			}
		}
	}

	private Object getForkResult() {
		if (resultValue != null) {
			if (resultValue.length() == 0) {
				return null;
			} else if (resultValue.length() == 1) {
				return resultValue.get(1);
			} else {
				return resultValue;
			}
		} else {
			return null;
		}
	}

	// �ж�fork���Ƿ������fork��������fork����ִ��
	private boolean isNextCommandBlock(int prevEndRow, int col, byte cmdType) {
		int totalRowCount = getRowCount();
		if (prevEndRow == totalRowCount) {
			return false;
		}

		int nextRow = prevEndRow + 1;
		PgmNormalCell cell = getPgmNormalCell(nextRow, col);
		Command nextCommand = cell.getCommand();
		if (nextCommand == null || nextCommand.getType() != cmdType) {
			return false;
		}

		for (int c = 1; c < col; ++c) {
			cell = getPgmNormalCell(nextRow, c);
			if (!cell.isBlankCell()) {
				return false;
			}
		}

		return true;
	}

	private void runForkCmd(Command command, Context ctx) {
		int row = curLct.getRow();
		int col = curLct.getCol();
		int endRow = getCodeBlockEndRow(row, col);
		IParam param = command.getParam(this, ctx);

		// ֻ�е���fork
		if (!isNextCommandBlock(endRow, col, Command.FORK)) {
			runForkCmd(param, row, col, endRow, ctx);
		} else {
			// ���������fork����ִ��
			ArrayList<SubForkJob> list = new ArrayList<SubForkJob>();
			while (true) {
				SubForkJob job = new SubForkJob(param, row, col, endRow, ctx);
				list.add(job);

				if (isNextCommandBlock(endRow, col, Command.FORK)) {
					row = endRow + 1;
					endRow = getCodeBlockEndRow(row, col);
					PgmNormalCell cell = getPgmNormalCell(row, col);
					command = cell.getCommand();
					param = command.getParam(this, ctx);
				} else {
					break;
				}
			}

			ThreadPool pool = ThreadPool.newInstance(list.size());
			try {
				for (SubForkJob job : list) {
					pool.submit(job);
				}

				for (SubForkJob job : list) {
					job.join();
				}
			} finally {
				pool.shutdown();
			}
		}

		setNext(endRow + 1, 1, true);
	}

	// fork ��.;h,s
	private void runForkxCmd(IParam param, int row, int col, int endRow,
			Context ctx) {
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fork"
					+ mm.getMessage("function.invalidParam"));
		}

		IParam leftParam = param.getSub(0);
		IParam rightParam = param.getSub(1);
		Object hostObj;
		if (rightParam == null || !rightParam.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fork"
					+ mm.getMessage("function.invalidParam"));
		} else {
			hostObj = rightParam.getLeafExpression().calculate(ctx);
		}

		Machines mc = new Machines();
		if (!mc.set(hostObj)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("callx"
					+ mm.getMessage("function.invalidParam"));
		}

		String[] hosts = mc.getHosts();
		int[] ports = mc.getPorts();

		int mcount = -1; // ������
		Object[] args = null; // ����
		if (leftParam == null) {
		} else if (leftParam.isLeaf()) {
			Object val = leftParam.getLeafExpression().calculate(ctx);
			if (val instanceof Sequence) {
				int len = ((Sequence) val).length();
				if (len == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.invalidParam"));
				}

				mcount = len;
			}

			args = new Object[] { val };
		} else {
			int pcount = leftParam.getSubSize();
			args = new Object[pcount];
			for (int p = 0; p < pcount; ++p) {
				IParam sub = leftParam.getSub(p);
				if (sub != null) {
					args[p] = sub.getLeafExpression().calculate(ctx);
					if (args[p] instanceof Sequence) {
						int len = ((Sequence) args[p]).length();
						if (len == 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fork"
									+ mm.getMessage("function.invalidParam"));
						}

						if (mcount == -1) {
							mcount = len;
						} else if (mcount != len) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(
									"fork"
											+ mm.getMessage("function.paramCountNotMatch"));
						}
					}
				}
			}
		}

		// ����fork�����������񴫸��ڵ��
		PgmCellSet pcs = newForkPgmCellSet(row, col, endRow, ctx, false);
		ParallelCaller caller = new ParallelCaller(pcs, hosts, ports);
		caller.setContext(ctx);
		// if (mcount > 0 && mcount < hosts.length) {
		// caller.setOptions("a");
		// }

		// ������û�ж�Ӧ��reduce
		int nextRow = endRow + 1;
		if (nextRow <= getRowCount()) {
			Command command = getPgmNormalCell(nextRow, col).getCommand();
			if (command != null && command.getType() == Command.REDUCE) {
				int reduceEndRow = getCodeBlockEndRow(nextRow, col);
				PgmCellSet reduce = newForkPgmCellSet(nextRow, col,
						reduceEndRow, ctx, false);
				caller.setReduce(reduce, new CellLocation(row, col),
						new CellLocation(nextRow, col));
			}
		}

		if (args != null) {
			// ͨ������������ݲ�����Ȼ������fork���ڸ�ı��ʽΪ=����
			final String pname = "tmp_fork_param";
			ParamList pl = pcs.getParamList();
			if (pl == null) {
				pl = new ParamList();
				pcs.setParamList(pl);
			}

			pl.add(0, new Param(pname, Param.VAR, null));
			pcs.getPgmNormalCell(row, col).setExpString("=" + pname);

			if (mcount == -1) {
				mcount = 1;
			}

			int pcount = args.length;
			for (int i = 1; i <= mcount; ++i) {
				ArrayList<Object> list = new ArrayList<Object>(1);
				if (pcount == 1) {
					if (args[0] instanceof Sequence) {
						Sequence sequence = (Sequence) args[0];
						list.add(sequence.get(i));
					} else {
						list.add(args[0]);
					}
				} else {
					Sequence seq = new Sequence(pcount);
					list.add(seq);

					for (int p = 0; p < pcount; ++p) {
						if (args[p] instanceof Sequence) {
							Sequence sequence = (Sequence) args[p];
							seq.add(sequence.get(i));
						} else {
							seq.add(args[p]);
						}
					}
				}

				caller.addCall(list);
			}
		}

		JobSpace js = ctx.getJobSpace();
		if (js != null)
			caller.setJobSpaceId(js.getID());

		Object result = caller.execute();
		getPgmNormalCell(row, col).setValue(result);
	}

	// ִ��fork�����
	private void runForkCmd(IParam param, int row, int col, int endRow,
			Context ctx) {
		Object[] args;
		if (param == null) {
			args = new Object[] { null };
			// MessageManager mm = EngineMessage.get();
			// throw new RQException("fork" +
			// mm.getMessage("function.missingParam"));
		} else if (param.getType() == IParam.Semicolon) {
			runForkxCmd(param, row, col, endRow, ctx);
			return;
		} else if (param.isLeaf()) {
			Object val = param.getLeafExpression().calculate(ctx);
			if (val instanceof MultipathCursors) {
				ICursor[] cursors = ((MultipathCursors) val)
						.getParallelCursors();
				val = new Sequence(cursors);
			}

			args = new Object[] { val };
		} else {
			int pcount = param.getSubSize();
			args = new Object[pcount];
			for (int i = 0; i < pcount; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.invalidParam"));
				}

				args[i] = sub.getLeafExpression().calculate(ctx);
			}
		}

		int pcount = args.length; // ��������
		int mcount = -1; // ������
		for (int i = 0; i < pcount; ++i) {
			if (args[i] instanceof Sequence) {
				int len = ((Sequence) args[i]).length();
				if (len == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.invalidParam"));
				}

				if (mcount == -1) {
					mcount = len;
				} else if (mcount != len) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.paramCountNotMatch"));
				}
			}
		}

		if (mcount == -1) {
			mcount = 1;
		}

		ForkJob[] jobs = new ForkJob[mcount];
		Sequence result = new Sequence(mcount);
		ThreadPool pool = ThreadPool.newInstance(mcount);

		try {
			for (int i = 0; i < mcount; ++i) {
				// ������α���������һ�������ģ������α��︽�ӵ�������ͬһ�������Ļ���Ӱ��
				PgmCellSet pcs = newForkPgmCellSet(row, col, endRow, ctx, true);
				pcs.forkCmdCode = new ForkCmdCode(row, col, endRow, i + 1);
				
				Context newCtx = pcs.getContext();

				Object val;
				if (pcount == 1) {
					if (args[0] instanceof Sequence) {
						val = ((Sequence) args[0]).get(i + 1);
					} else {
						val = args[0];
					}

					if (val instanceof ICursor) {
						((ICursor) val).setContext(newCtx);
					}
				} else {
					Sequence seq = new Sequence(pcount);
					val = seq;
					for (int p = 0; p < pcount; ++p) {
						if (args[p] instanceof Sequence) {
							Object mem = ((Sequence) args[p]).get(i + 1);
							seq.add(mem);
							if (mem instanceof ICursor) {
								((ICursor) mem).setContext(newCtx);
							}
						} else {
							seq.add(args[p]);
							if (args[p] instanceof ICursor) {
								((ICursor) args[p]).setContext(newCtx);
							}
						}
					}
				}

				pcs.getPgmNormalCell(row, col).setValue(val);
				jobs[i] = new ForkJob(pcs, row, col, endRow);
				pool.submit(jobs[i]);
			}

			for (int i = 0; i < mcount; ++i) {
				jobs[i].join();
				result.add(jobs[i].getResult());
			}
		} finally {
			pool.shutdown();
		}

		getPgmNormalCell(row, col).setValue(result);
	}

	private void runChannelCmd(Command command, Context ctx) {
		IParam param = command.getParam(this, ctx);
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("channel"
					+ mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (!(obj instanceof ICursor)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("channel"
					+ mm.getMessage("function.paramTypeError"));
		}

		ICursor cs = (ICursor) obj;
		CellLocation curLct = this.curLct;
		int row = curLct.getRow();
		int col = curLct.getCol();
		int endRow = getCodeBlockEndRow(row, col);

		// ��ÿ��channel���ڵĸ��Ӵ����ܵ�������Ϊ��Ԫ��ֵ
		ArrayList<PgmNormalCell> cellList = new ArrayList<PgmNormalCell>();
		PgmNormalCell cell = getPgmNormalCell(row, col);

		// ������ܵ��������ٸ��α긽��push���㣬��Ϊ�α���ܵ���fetch@0������һ��������
		// �����û������ܵ������㣬���α껺��������򲻻�ִ�йܵ��������ӵ�����
		Channel channel = cs.newChannel(ctx, false);
		cell.setValue(channel);
		cellList.add(cell);
		setNext(row, col + 1, false);

		do {
			curLct = runNext2();
		} while (curLct != null && curLct.getRow() <= endRow);

		channel.addPushToCursor(cs);

		// ����������������channel�����
		while (isNextCommandBlock(endRow, col, Command.CHANNEL)) {
			row = endRow + 1;
			cell = getPgmNormalCell(row, col);
			if (cell.getCommand().getParam(this, ctx) != null) {
				break;
			}

			endRow = getCodeBlockEndRow(row, col);
			channel = cs.newChannel(ctx, false);
			cell.setValue(channel);
			cellList.add(cell);

			setNext(row, col + 1, false);
			do {
				curLct = runNext2();
			} while (curLct != null && curLct.getRow() <= endRow);

			channel.addPushToCursor(cs);
		}

		// �����α�����
		if (cs instanceof MultipathCursors) {
			MultipathCursors mcs = (MultipathCursors) cs;
			ICursor[] cursors = mcs.getCursors();
			int csCount = cursors.length;
			ThreadPool pool = ThreadPool.newInstance(csCount);

			try {
				CursorLooper[] loopers = new CursorLooper[csCount];
				for (int i = 0; i < csCount; ++i) {
					loopers[i] = new CursorLooper(cursors[i]);
					pool.submit(loopers[i]);
				}

				for (CursorLooper looper : loopers) {
					looper.join();
				}
			} finally {
				pool.shutdown();
			}
		} else {
			while (true) {
				Sequence src = cs.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0)
					break;
			}
		}

		// ȡ���ܵ��ļ����������Ԫ��
		for (PgmNormalCell chCell : cellList) {
			channel = (Channel) chCell.getValue();
			chCell.setValue(channel.result());
		}

		setNext(endRow + 1, 1, true);
	}

	private void runReturnCmd(Command command) {
		hasReturn = true;
		Context ctx = getContext();
		Expression[] exps = command.getExpressions(this, ctx);
		int count = exps.length;

		resultValue = new Sequence(count);
		for (int i = 0; i < count; ++i) {
			if (exps[i] == null) {
				resultValue.add(null);
			} else {
				Object obj = exps[i].calculate(ctx);
				resultValue.add(obj);
			}
		}

		int r = curLct.getRow();
		int c = curLct.getCol();
		getCell(r, c).setValue(resultValue);
		resultLct = new CellLocation(r, c);
		
		// �������α���Ҫ���return�����Բ���������ִ�У����õĵط��Լ��ж��Ƿ����
		setNext(r, c + 1, false);
		//runFinished();
	}

	private void runSqlCmd(NormalCell cell, SqlCommand command) {
		Context ctx = getContext();
		Object dbObj;
		Expression dbExp = command.getDbExpression(this, ctx);

		if (dbExp != null) {
			if (command.isLogicSql()) {
				dbObj = FileObject.createSimpleQuery();
				curDb = dbObj;
			} else {
				Object obj = dbExp.calculate(ctx);
				if (!(obj instanceof DBObject) && !(obj instanceof IQueryable)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(command.getDb()
							+ mm.getMessage("engine.dbsfNotExist"));
				}

				dbObj = obj;
				curDb = dbObj;
			}
		} else {
			if (curDb == null) {
				DBSession dbs = ctx.getDBSession();
				if (dbs == null) {
					dbObj = FileObject.createSimpleQuery();
					curDb = dbObj;
				} else {
					dbObj = new DBObject(dbs);
					curDb = dbObj;
				}
			} else {
				dbObj = curDb;
			}
		}

		// ���������
		// int endRow = getCodeBlockEndRow(curLct.getRow(), curLct.getCol());
		String sql = command.getSql();
		if (sql == null) {
			setNext(curLct.getRow(), curLct.getCol() + 1, false);
			return;
		}

		IParam param = command.getParam(this, ctx);
		Object[] paramVals = null;
		byte[] types = null;

		if (param != null) {
			ParamInfo2 pi = ParamInfo2.parse(param, "SQL command", true, false);
			paramVals = pi.getValues1(ctx);
			Object[] typeObjs = pi.getValues2(ctx);
			int count = typeObjs.length;

			types = new byte[count];
			for (int i = 0; i < count; ++i) {
				if (typeObjs[i] == null)
					continue;
				if (!(typeObjs[i] instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("SQL command"
							+ mm.getMessage("function.paramTypeError"));
				}

				types[i] = ((Number) typeObjs[i]).byteValue();
			}
		}

		Object val;
		if (dbObj instanceof DBObject) {
			String opt = command.getOption();
			if (command.isQuery()) {
				if (opt == null || opt.indexOf('1') == -1) {
					val = ((DBObject) dbObj).query(sql, paramVals, types, opt,
							ctx);
				} else {
					val = ((DBObject) dbObj).query1(sql, paramVals, types, opt);
				}
			} else {
				val = ((DBObject) dbObj).execute(sql, paramVals, types, opt);
			}
		} else {
			val = ((IQueryable)dbObj).query(sql, paramVals, this, ctx);
		}

		cell.setValue(val);
		// ���������
		// setNext(endRow + 1, 1, true);
		setNext(curLct.getRow(), curLct.getCol() + 1, false);
	}

	private void runTryCmd(NormalCell cell, Command command) {
		int row = cell.getRow();
		int col = cell.getCol();
		int endRow = getCodeBlockEndRow(row, col);
		CmdCode cmdCode = new CmdCode(Command.TRY, row, col, endRow);
		stack.addFirst(cmdCode);
		setNext(row, col + 1, false);
	}

	private void clearArea(IParam startParam, IParam endParam, Context ctx) {
		if (startParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("clear" + mm.getMessage("function.invalidParam"));
		}

		INormalCell startCell = startParam.getLeafExpression().calculateCell(ctx);
		if (startCell == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("clear"
					+ mm.getMessage("function.invalidParam"));
		}

		ICellSet cs = startCell.getCellSet();
		int left = startCell.getCol();
		int top = startCell.getRow();
		int right;
		int bottom;

		// ��дA1:��ʾ�����A1Ϊ����Ĵ�����ֵ
		if (endParam == null) {
			right = getColCount();
			bottom = getCodeBlockEndRow(top, left);
		} else {
			INormalCell endCell = endParam.getLeafExpression().calculateCell(ctx);
			if (endCell == null || endCell.getCellSet() != cs) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("clear" + mm.getMessage("function.invalidParam"));
			}

			right = endCell.getCol();
			bottom = endCell.getRow();
		}

		if (top <= bottom) {
			if (left <= right) { // ���� - ����
				for (int r = top; r <= bottom; ++r) {
					for (int c = left; c <= right; ++c) {
						cs.getCell(r, c).clear();
					}
				}
			} else { // ���� - ����
				for (int r = top; r <= bottom; ++r) {
					for (int c = left; c >= right; --c) {
						cs.getCell(r, c).clear();
					}
				}
			}
		} else {
			if (left <= right) { // ���� - ����
				for (int r = top; r >= bottom; --r) {
					for (int c = left; c <= right; ++c) {
						cs.getCell(r, c).clear();
					}
				}
			} else { // ���� - ����
				for (int r = top; r >= bottom; --r) {
					for (int c = left; c >= right; --c) {
						cs.getCell(r, c).clear();
					}
				}
			}
		}
	}

	private void runClearCmd(Command command) {
		Context ctx = getContext();
		IParam param = command.getParam(this, ctx);
		if (param == null) {
		} else if (param.isLeaf()) {
			INormalCell cell = param.getLeafExpression().calculateCell(ctx);
			if (cell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]"
						+ mm.getMessage("function.invalidParam"));
			}

			cell.clear();
		} else if (param.getType() == IParam.Comma) { // ,
			int size = param.getSubSize();
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
				} else if (sub.isLeaf()) {
					INormalCell cell = sub.getLeafExpression().calculateCell(
							ctx);
					if (cell == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("[]"
								+ mm.getMessage("function.invalidParam"));
					}

					cell.clear();
				} else { // :
					clearArea(sub.getSub(0), sub.getSub(1), ctx);
				}
			}
		} else if (param.getType() == IParam.Colon) { // :
			clearArea(param.getSub(0), param.getSub(1), ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("clear"
					+ mm.getMessage("function.invalidParam"));
		}

		setNext(curLct.getRow(), curLct.getCol() + 1, false);
	}

	private void runEndCmd(Command command) {
		Context ctx = getContext();
		IParam param = command.getParam(this, ctx);

		if (param == null) {
			runFinished();
			// throw new RetryException("error");
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			runFinished();
			throw new RetryException("error " + Variant.toString(obj));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("error"
					+ mm.getMessage("function.invalidParam"));
		}
	}

	// ������һ��Ҫִ�еĵ�Ԫ��
	public void setNext(int row, int col, boolean isCheckStack) {
		int colCount = getColCount();
		if (col > colCount) {
			row++;
			col = 1;
			isCheckStack = true; // ���м���ջ
		}

		if (isCheckStack) {
			while (stack.size() > 0) {
				// ��һ��Ҫִ�еĵ�Ԫ���Ƿ��ڴ���鷶Χ��
				CmdCode cmd = stack.getFirst();
				if (row > cmd.blockEndRow) {
					if (cmd.type == Command.FOR) {
						// ������һ��ѭ��
						curLct.set(cmd.row, cmd.col);
						return;
					} else {
						// ����try�飬��������ջ
						stack.removeFirst();
					}
				} else {
					break;
				}
			}
		}

		if (row > getRowCount()) {
			runFinished();
		} else {
			// �����յ�Ԫ��ע�͸񡢳�����
			PgmNormalCell cell = getPgmNormalCell(row, col);
			if (cell.isBlankCell() || cell.isNoteCell() || cell.isConstCell()) {
				setNext(row, col + 1, false);
			} else if (cell.isNoteBlock()) { // ����ע�Ϳ�
				setNext(getCodeBlockEndRow(row, col) + 1, 1, true);
			} else {
				curLct.set(row, col);
			}
		}
	}

	/**
	 * ���ص�Ԫ������Ľ������к�
	 * @param prow int
	 * @param pcol int
	 * @return int
	 */
	public int getCodeBlockEndRow(int prow, int pcol) {
		int totalRow = getRowCount();
		for (int row = prow + 1; row <= totalRow; ++row) {
			for (int c = 1; c <= pcol; ++c) {
				PgmNormalCell cell = getPgmNormalCell(row, c);
				if (!cell.isBlankCell()) {
					return row - 1;
				}
			}
		}

		return totalRow;
	}

	private CellLocation runNext2() {
		Context ctx = getContext();
		if (curLct == null) {
			// ��ʼִ��ʱ����һ��Ҫִ�еĸ��赽��һ����Ԫ����
			curLct = new CellLocation();
			setNext(1, 1, false);
			hasReturn = false;

			return curLct;
		}

		try {
			// ִ�е�ǰ�ĵ�Ԫ�񣬲��ҳ���һ��Ҫִ�еĸ�
			PgmNormalCell cell = getPgmNormalCell(curLct.getRow(),
					curLct.getCol());
			Command command = cell.getCommand();

			if (command == null) {
				cell.calculate();
				if (cell.isCalculableBlock() || cell.isExecutableBlock()) {
					int endRow = getCodeBlockEndRow(curLct.getRow(),
							curLct.getCol());
					setNext(endRow + 1, 1, true);
				} else {
					setNext(curLct.getRow(), curLct.getCol() + 1, false);
				}
			} else {
				byte type = command.getType();
				switch (type) {
				case Command.IF:
					runIfCmd(cell, command);
					break;
				case Command.ELSE:
				case Command.ELSEIF:
					skipCodeBlock();
					break;
				case Command.FOR:
					runForCmd(cell, command);
					break;
				case Command.CONTINUE:
					runContinueCmd(command);
					break;
				case Command.BREAK:
					runBreakCmd(command);
					break;
				case Command.FUNC:
				case Command.REDUCE:
					skipCodeBlock();
					break;
				case Command.RETURN:
				case Command.RESULT:
					// MessageManager mm = EngineMessage.get();
					// throw new
					// RQException(mm.getMessage("engine.unknownRet"));
					runReturnCmd(command);
					break;
				case Command.SQL:
					runSqlCmd(cell, (SqlCommand) command);
					break;
				case Command.CLEAR:
					runClearCmd(command);
					break;
				case Command.END:
					runEndCmd(command);
					break;
				case Command.FORK:
					runForkCmd(command, ctx);
					break;
				case Command.GOTO:
					runGotoCmd(command);
					break;
				case Command.CHANNEL:
					runChannelCmd(command, ctx);
					break;
				case Command.TRY:
					runTryCmd(cell, command);
					break;
				default:
					throw new RuntimeException();
				}
			}
		} catch (RetryException re) {
			throw re;
		} catch (RQException re) {
			String cellId = curLct.toString();
			if (name != null) {
				cellId = "[" + name + "]." + cellId;
			}
			
			String msg = re.getMessage();
			if (goCatch(cellId + ' ' + msg)) {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				Logger.error(msg, re);
			} else {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				re.setMessage(msg);
				throw re;
			}
		} catch (Throwable e) {
			String cellId = curLct.toString();
			if (name != null) {
				cellId = "[" + name + "]." + cellId;
			}
			
			String msg = e.getMessage();
			if (goCatch(msg)) {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				Logger.error(msg, e);
			} else {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				throw new RQException(msg, e);
			}
		}

		return curLct;
	}

	private boolean goCatch(String error) {
		while (stack.size() > 0) {
			// ��һ��Ҫִ�еĵ�Ԫ���Ƿ��ڴ���鷶Χ��
			CmdCode cmd = stack.getFirst();
			if (cmd.type == Command.TRY) {
				stack.removeFirst();
				setNext(cmd.blockEndRow + 1, 1, true);
				getPgmNormalCell(cmd.row, cmd.col).setValue(error);
				return true;
			} else {
				stack.removeFirst();
			}
		}

		return false;
	}

	/**
	 * ִ����һ����Ԫ��������ؿ���ִ�����
	 * @return CellLocation
	 */
	public CellLocation runNext() {
		// try {
		// enterTask();
		return runNext2();
		// } finally {
		// leaveTask();
		// }
	}

	/**
	 * �ӵ�ǰ�����ִ�У�ֱ���������
	 */
	public void run() {
		while (true) {
			if (isInterrupted) {
				isInterrupted = false;
				break;
			}
			
			if (runNext2() == null) {
				break;
			} else if (hasReturn()) {
				// ����return��ִֹͣ��
				runFinished();
				break;
			}
		}
	}

	/**
	 * ���μ��������г�������ĵ�Ԫ�񣬷������һ�������Ľ��
	 * @param int row
	 * @param int col
	 * @return Object
	 */
	public Object executeSubCell(int row, int col) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		if (row < 1 || row > rowCount || col < 1 || col > colCount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(CellLocation.getCellId(row, col)
					+ mm.getMessage("cellset.cellNotExist"));
		}

		if (col == colCount)
			return null;

		// �����ֳ�
		CellLocation oldLct = curLct;
		LinkedList<CmdCode> oldStack = stack;
		Object retVal = null;

		try {
			curLct = new CellLocation();
			stack = new LinkedList<CmdCode>();

			int endRow = getCodeBlockEndRow(row, col);
			setNext(row, col + 1, false); // ��ִ������,���Ӹ�ʼִ��
			for (; curLct != null;) {
				int curRow = curLct.getRow();
				if (curRow > endRow)
					break;
				int curCol = curLct.getCol();

				runNext2();

				PgmNormalCell cell = getPgmNormalCell(curRow, curCol);
				if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					retVal = cell.getValue();
				}
			}
		} finally {
			// �ָ��ֳ�
			curLct = oldLct;
			stack = oldStack;
		}

		return retVal;
	}

	/**
	 * ִ��ĳ����Ԫ�������ѭ������ִ�д����
	 * @param row int ��Ԫ���к�
	 * @param col int ��Ԫ���к�
	 */
	public void runCell(int row, int col) {
		// if (curLct == null) checkLicense();

		PgmNormalCell cell = getPgmNormalCell(row, col);
		if (!cell.needCalculate())
			return;

		// �����ֳ�
		CellLocation oldLct = curLct;
		LinkedList<CmdCode> oldStack = stack;

		try {
			// enterTask();

			curLct = new CellLocation(row, col);
			stack = new LinkedList<CmdCode>();
			Command cmd = cell.getCommand();
			if (cmd == null) {
				cell.calculate();
			} else {
				CellLocation lct;
				int endRow;
				byte type = cmd.getType();

				switch (type) {
				case Command.ELSE:
				case Command.ELSEIF:
				case Command.CONTINUE:
				case Command.BREAK:
				case Command.FUNC:
				case Command.REDUCE:
					break;
				case Command.RETURN:
				case Command.RESULT:
					runReturnCmd(cmd);
					break;
				case Command.IF:
					endRow = getIfBlockEndRow(row, col);
					do {
						lct = runNext2();
					} while (lct != null && lct.getRow() <= endRow);
					break;
				case Command.FOR:
					endRow = getCodeBlockEndRow(row, col);
					do {
						lct = runNext2();
					} while (lct != null && lct.getRow() <= endRow);
					break;
				case Command.SQL:
					runSqlCmd(cell, (SqlCommand) cmd);
					break;
				case Command.CLEAR:
					runClearCmd(cmd);
					break;
				case Command.END:
					runEndCmd(cmd);
					break;
				case Command.FORK:
					runForkCmd(cmd, getContext());
					break;
				case Command.CHANNEL:
					runChannelCmd(cmd, getContext());
					break;
				case Command.TRY:
					// runTryCmd(cell, cmd);
					break;
				default:
					throw new RuntimeException();
				}
			}
		} finally {
			// leaveTask();

			// �ָ��ֳ�
			curLct = oldLct;
			stack = oldStack;
		}
	}

	/**
	 * ִ��ָ�����ӵ��Ӻ������ɵݹ����
	 * @param row int �Ӻ������ڵ���
	 * @param col int �Ӻ������ڵ���
	 * @param args Object[] ��������
	 * @param opt String i�����ݹ���ã����ø�������
	 * @return Object �Ӻ�������ֵ
	 */
	public Object executeFunc(int row, int col, Object[] args, String opt) {
		PgmNormalCell cell = getPgmNormalCell(row, col);
		Command cmd = cell.getCommand();
		if (cmd == null || cmd.getType() != Command.FUNC) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.callNeedSub"));
		}

		String expStr = cmd.getExpression();
		if (expStr != null && expStr.length() > 0) {
			int nameEnd = KeyWord.scanId(expStr, 0);
			String fnName = expStr.substring(0, nameEnd);
			return executeFunc(fnName, args, opt);
		}

		int endRow = getCodeBlockEndRow(row, col);
		if (opt != null && opt.indexOf('i') != -1) {
			CellLocation oldLct = curLct;
			Object result = executeFunc(row, col, endRow, args);
			curLct = oldLct;
			return result;
		}

		// ����������ĸ���
		PgmCellSet pcs = newCalc();
		int colCount = getColCount();
		for (int r = row; r <= endRow; ++r) {
			for (int c = col; c <= colCount; ++c) {
				INormalCell tmp = getCell(r, c);
				INormalCell cellClone = (INormalCell) tmp.deepClone();
				cellClone.setCellSet(pcs);
				pcs.cellMatrix.set(r, c, cellClone);
			}
		}

		return pcs.executeFunc(row, col, endRow, args);
	}

	/**
	 * ���ݺ�����ȡ������Ϣ
	 * @param fnName ������
	 * @return
	 */
	public FuncInfo getFuncInfo(String fnName) {
		return getFunctionMap().get(fnName);
	}

	/**
	 * ִ��ָ�����ֵ��Ӻ������ɵݹ����
	 * @param funcInfo ������Ϣ
	 * @param args Object[] ��������
	 * @param opt String i�����ݹ���ã����ø�������
	 * @return Object ��������ֵ
	 */
	public Object executeFunc(FuncInfo funcInfo, Object[] args, String opt) {
		PgmNormalCell cell = funcInfo.getCell();
		int row = cell.getRow();
		int col = cell.getCol();
		int colCount = getColCount();
		int endRow = getCodeBlockEndRow(row, col);

		// ����������ĸ���
		PgmCellSet pcs = newCalc();
		String[] argNames = funcInfo.getArgNames();
		if (argNames != null) {
			// �Ѳ����赽��������
			int argCount = argNames.length;
			if (args == null || args.length != argCount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcInfo.getFnName()
						+ mm.getMessage("function.paramCountNotMatch"));
			}

			Context ctx = pcs.getContext();
			for (int i = 0; i < argCount; ++i) {
				ctx.setParamValue(argNames[i], args[i]);
			}
		}

		for (int r = row; r <= endRow; ++r) {
			for (int c = col; c <= colCount; ++c) {
				INormalCell tmp = getCell(r, c);
				INormalCell cellClone = (INormalCell) tmp.deepClone();
				cellClone.setCellSet(pcs);
				pcs.cellMatrix.set(r, c, cellClone);
			}
		}

		// ���������ֺͲ����ĺ������ٽ��������뵥Ԫ��
		return pcs.executeFunc(row, col, endRow, null); // args
	}
	
	/**
	 * ִ��ָ�����ֵ��Ӻ������ɵݹ����
	 * @param fnName ������
	 * @param args Object[] ��������
	 * @param opt String i�����ݹ���ã����ø�������
	 * @return Object ��������ֵ
	 */
	public Object executeFunc(String fnName, Object[] args, String opt) {
		FuncInfo funcInfo = getFuncInfo(fnName);
		if (funcInfo == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(fnName + mm.getMessage("Expression.unknownFunction"));
		}
		
		return executeFunc(funcInfo, args, opt);
	}

	private Object executeFunc(int row, int col, int endRow, Object[] args) {
		int colCount = getColCount();

		// �Ѳ���ֵ�赽func��Ԫ���ϼ�����ĸ���
		if (args != null) {
			int paramRow = row;
			int paramCol = col;
			for (int i = 0, pcount = args.length; i < pcount; ++i) {
				getPgmNormalCell(paramRow, paramCol).setValue(args[i]);
				if (paramCol < colCount) {
					paramCol++;
				} else {
					break;
					// if (paramRow == getRowCount() && i < paramCount - 1) {
					// MessageManager mm = EngineMessage.get();
					// throw new RQException("call" +
					// mm.getMessage("function.paramCountNotMatch"));
					// }
					// paramRow++;
					// paramCol = 1;
				}
			}
		}

		curLct = new CellLocation(row, col);
		setNext(row, col + 1, false); // ���Ӹ�ʼִ��

		for (; curLct != null;) {
			int curRow = curLct.getRow();
			if (curRow > endRow) { // �����˴���飬û��return��
				break;
			}

			int curCol = curLct.getCol();
			PgmNormalCell cell = getPgmNormalCell(curRow, curCol);
			Command cmd = cell.getCommand();
			if (cmd == null) {
				runNext2();
			} else if (cmd.getType() == Command.RETURN) {
				Context ctx = getContext();
				Expression exp = cmd.getExpression(this, ctx);
				if (exp != null) {
					return exp.calculate(ctx);
				} else {
					return null;
				}
			} else {
				runNext2();
			}
		}

		// δ����returnȱʡ���ش���������һ�������ֵ
		for (int r = endRow; r >= row; --r) {
			for (int c = colCount; c > col; --c) {
				PgmNormalCell cell = getPgmNormalCell(r, c);
				if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					return cell.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * ִ���Ӻ��������ɵݹ����
	 * @param row int �Ӻ������ڵ���
	 * @param col int �Ӻ������ڵ���
	 * @param args Object[] ��������
	 * @return Object �Ӻ�������ֵ
	 */
	/*
	 * public Object executeFunc_nr(int row, int col, Object []args) { if (row <
	 * 1 || row > getRowCount() || col < 1 || col > getColCount()) {
	 * MessageManager mm = EngineMessage.get(); throw new
	 * RQException(mm.getMessage("engine.callNeedSub")); }
	 * 
	 * PgmNormalCell cell = getPgmNormalCell(row, col); Command cmd =
	 * cell.getCommand(); if (cmd == null || cmd.getType() != Command.FUNC) {
	 * MessageManager mm = EngineMessage.get(); throw new
	 * RQException(mm.getMessage("engine.callNeedSub")); }
	 * 
	 * // �Ѳ���ֵ�赽func��Ԫ���ϼ�����ĸ��� if (args != null) { int paramRow = row; int
	 * paramCol = col; for (int i = 0, pcount = args.length; i < pcount; ++i) {
	 * getPgmNormalCell(paramRow, paramCol).setValue(args[i]); if (paramCol <
	 * getColCount()) { paramCol++; } else { break; //if (paramRow ==
	 * getRowCount() && i < paramCount - 1) { // MessageManager mm =
	 * EngineMessage.get(); // throw new RQException("call" +
	 * mm.getMessage("function.paramCountNotMatch")); //} //paramRow++;
	 * //paramCol = 1; } } }
	 * 
	 * 
	 * // �����ֳ� CellLocation oldLct = curLct; LinkedList <CmdCode> oldStack =
	 * stack;
	 * 
	 * try { curLct = new CellLocation(row, col); stack = new LinkedList
	 * <CmdCode>(); setNext(row, col + 1, false); // ���Ӹ�ʼִ�� int endRow =
	 * getCodeBlockEndRow(row, col);
	 * 
	 * for (; curLct != null;) { int curRow = curLct.getRow(); if (curRow >
	 * endRow) { // �����˴���飬û��return�� return null; }
	 * 
	 * int curCol = curLct.getCol(); cell = getPgmNormalCell(curRow, curCol);
	 * cmd = cell.getCommand(); if (cmd == null) { runNext2(); } else if
	 * (cmd.getType() == Command.RETURN) { Context ctx = getContext();
	 * Expression exp = cmd.getExpression(this, ctx); if (exp != null) { return
	 * exp.calculate(ctx); } else { return null; } } else { runNext2(); } } }
	 * finally { // �ָ��ֳ� curLct = oldLct; stack = oldStack; }
	 * 
	 * return null; }
	 */

	/**
	 * �������У��ͷ���Դ����ֵ�Ա���
	 */
	public void runFinished() {
		super.runFinished();
		curLct = null;
		stack.clear();
		curDb = null;
	}

	public void reset() {
		super.reset();
		resultValue = null;
		resultCurrent = 0;
		resultLct = null;
		interrupt = false;
		isInterrupted = false;
		hasReturn = false;
		fnMap = null;
	}

	// ---------------------------calc end------------------------------

	protected void setParseCurrent(int row, int col) {
		if (parseLct == null) {
			parseLct = new CellLocation(row, col);
		} else {
			parseLct.set(row, col);
		}
	}

	// $() $(a) $(a:b)
	public String getMacroReplaceString(String strCell) {
		Context ctx = getContext();
		strCell = Expression.replaceMacros(strCell, this, ctx);
		strCell = strCell.trim();

		int sr, sc, er, ec;
		int colonIndex;
		if (strCell != null && (colonIndex = strCell.indexOf(':')) != -1) {
			String startStr = strCell.substring(0, colonIndex);
			INormalCell startCell = getCell(startStr);
			if (startCell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(startStr
						+ mm.getMessage("cellset.cellNotExist"));
			}

			String endStr = strCell.substring(colonIndex + 1);
			INormalCell endCell = getCell(endStr);
			if (endCell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(endStr
						+ mm.getMessage("cellset.cellNotExist"));
			}

			sr = startCell.getRow();
			sc = startCell.getCol();
			er = endCell.getRow();
			ec = endCell.getCol();

			if (sr > er || sc > ec) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\":\""
						+ mm.getMessage("operator.cellLocation"));
			}
		} else {
			INormalCell cell = getCell(strCell);
			if (cell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(strCell
						+ mm.getMessage("cellset.cellNotExist"));
			}

			er = sr = cell.getRow();
			ec = sc = cell.getCol();
		}

		StringBuffer buffer = new StringBuffer(100);
		for (int r = sr; r <= er; ++r) {
			for (int c = sc; c <= ec; ++c) {
				PgmNormalCell cell = getPgmNormalCell(r, c);
				if (cell.isBlankCell() || cell.isNoteCell())
					continue;
				if (cell.isNoteBlock()) {
					// ����ע�Ϳ�
					r = getCodeBlockEndRow(r, c);
					break;
				}

				setParseCurrent(r, c);
				String cellStr = cell.getMacroReplaceString();
				buffer.append(Expression.replaceMacros(cellStr, this, ctx));
				buffer.append(',');
			}
		}

		int len = buffer.length();
		return len > 0 ? buffer.substring(0, len - 1) : "";
	}

	public String getPrevCellSet(String str, int pos) {
		return null;
	}

	// call�Ƿ��ж�
	public boolean isCallInterrupted() {
		return curLct != null && resultValue == null;
	}

	/**
	 * �������񣬷���result��Ԫ���ֵ
	 * @return Object
	 */
	public Object execute() {
		// �����һ��result���
		resultValue = null;
		while (runNext2() != null && resultValue == null) {
			if (isInterrupted) {
				isInterrupted = false;
				break;
			}
		}

		if (resultValue != null) {
			if (resultValue.length() == 0) {
				return null;
			} else if (resultValue.length() == 1) {
				return resultValue.get(1);
			} else {
				return resultValue;
			}
		} else {
			// δ����result��endȱʡ���ش��������һ�������ֵ
			return getLastCalculableCellValue();
		}
	}

	// ȡ���һ�������ĸ�ֵ
	public Object getLastCalculableCellValue() {
		int colCount = getColCount();
		for (int r = getRowCount(); r > 0; --r) {
			for (int c = colCount; c > 0; --c) {
				PgmNormalCell cell = getPgmNormalCell(r, c);
				if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					Object val = cell.getValue();
					resultValue = new Sequence(1);
					resultValue.add(val);
					return val;
				}
			}
		}

		return null;
	}

	/**
	 * ��ʼ�������񷵻ؽ��
	 */
	public void calculateResult() {
		execute();
	}

	/**
	 * �Ƿ��н������
	 * @return boolean
	 */
	public boolean hasNextResult() {
		if (resultValue != null && resultValue.length() > 0) {
			return true;
		} else if (curLct == null) {
			return false; // �������
		} else {
			resultValue = null;
			resultCurrent = 0;
			while (runNext2() != null && resultValue == null) {
				if (isInterrupted) {
					isInterrupted = false;
					return false;
				}
			}

			return hasNextResult();
		}
	}

	// �Ƿ�ִ�е���return���
	public boolean hasReturn() {
		return hasReturn;
	}

	/**
	 * ȡ��һ�����
	 * @return Object
	 */
	public Object nextResult() {
		if (!hasNextResult())
			return null;

		if (resultCurrent < 1)
			resultCurrent = 1; // �״�ȡ

		Object obj = resultValue.get(resultCurrent);
		if (resultCurrent < resultValue.length()) {
			resultValue.set(resultCurrent, null);
			resultCurrent++;
		} else {
			resultValue = null;
		}

		return obj;
	}

	/**
	 * ȡ��һ�������λ��
	 * @return CellLocation
	 */
	public CellLocation nextResultLocation() {
		if (!hasNextResult())
			return null;
		return resultLct;
	}

	/**
	 * �ж�ִ�У��Ե�Ԫ��Ϊ��λ
	 */
	public void interrupt() {
		interrupt = true;
		isInterrupted = true;
	}

	public boolean getInterrupt() {
		return interrupt;
	}

	/**
	 * �����Ƿ��Զ�����
	 * @param b boolean
	 */
	public void setAutoCalc(boolean b) {
		if (b) {
			sign |= SIGN_AUTOCALC;
		} else {
			sign &= ~SIGN_AUTOCALC;
		}
	}

	/**
	 * �����Ƿ��Զ�����
	 * @return boolean
	 */
	public boolean isAutoCalc() {
		return (sign & SIGN_AUTOCALC) == SIGN_AUTOCALC;
	}
	
	/**
	 * �������һ�������Ƿ��Ƕ�̬����
	 * @param b boolean
	 */
	public void setDynamicParam(boolean b) {
		if (b) {
			sign |= SIGN_DYNAMICPARAM;
		} else {
			sign &= ~SIGN_DYNAMICPARAM;
		}
	}

	/**
	 * �������һ�������Ƿ��Ƕ�̬����
	 * @return boolean
	 */
	public boolean isDynamicParam() {
		return (sign & SIGN_DYNAMICPARAM) == SIGN_DYNAMICPARAM;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * ȡ�Զ�������ӳ��
	 * @return ByteMap
	 */
	public ByteMap getCustomPropMap() {
		return customPropMap;
	}

	/**
	 * �����Զ�������ӳ��
	 * @param map ByteMap
	 */
	public void setCustomPropMap(ByteMap map) {
		if (!isExecuteOnly()) {
			this.customPropMap = map;
		}
	}

	/**
	 * ��������
	 * @param psw String
	 */
	public void setPassword(String psw) {
		if (psw == null || psw.length() == 0) {
			this.pswHash = null;
		} else {
			MD5 md5 = new MD5();
			this.pswHash = md5.getMD5ofStr(psw);
		}
	}

	public String getPasswordHash() {
		return pswHash;
	}

	/**
	 * �����������½ʱ��Ȩ��
	 * @param p int��PRIVILEGE_FULL��PRIVILEGE_EXEC
	 */
	// public void setNullPasswordPrivilege(int p) {
	// this.nullPswPrivilege = p;
	// }

	/**
	 * ȡ�������½ʱ��Ȩ��
	 * @return int��PRIVILEGE_FULL��PRIVILEGE_EXEC
	 */
	public int getNullPasswordPrivilege() {
		return this.nullPswPrivilege;
	}

	/**
	 * ���õ�ǰ������
	 * @param psw String
	 */
	public void setCurrentPassword(String psw) {
		this.curPrivilege = getPrivilege(pswHash, psw, nullPswPrivilege);
	}

	public static int getPrivilege(String pswHash, String psw,
			int nullPswPrivilege) {
		if (pswHash == null) {
			return PRIVILEGE_FULL;
		} else if (psw == null || psw.length() == 0) {
			return nullPswPrivilege;
		} else {
			MD5 md5 = new MD5();
			psw = md5.getMD5ofStr(psw);
			if (psw.equals(pswHash)) {
				return PRIVILEGE_FULL;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("cellset.pswError"));
			}
		}
	}

	/**
	 * ���ص�ǰ�����Ȩ��
	 * @return int
	 */
	public int getCurrentPrivilege() {
		return curPrivilege;
	}

	public boolean isExecuteOnly() {
		return curPrivilege == PRIVILEGE_EXEC;
	}

	public HashMap<String, FuncInfo> getFunctionMap() {
		if (fnMap == null) {
			// ���������ж���ĺ��������ɺ�����ӳ���
			fnMap = new HashMap<String, FuncInfo>();
			int rowCount = getRowCount();
			int colCount = getColCount();
			Context ctx = getContext();

			for (int r = 1; r <= rowCount; ++r) {
				for (int c = 1; c <= colCount; ++c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					Command command = cell.getCommand();
					if (command == null || command.getType() != Command.FUNC) {
						continue;
					}

					String expStr = command.getExpression();
					if (expStr == null || expStr.length() == 0) {
						continue;
					}

					int len = expStr.length();
					int nameEnd = KeyWord.scanId(expStr, 0);
					String fnName = expStr.substring(0, nameEnd);
					String fnOpt = null;
					int atIdx = fnName.indexOf(KeyWord.OPTION);
					
					if (atIdx != -1) {
						fnOpt = fnName.substring(atIdx + 1);
						fnName = fnName.substring(0, atIdx);
					}
					
					if (nameEnd == len) {
						FuncInfo funcInfo = new FuncInfo(fnName, cell, null, fnOpt);
						fnMap.put(expStr, funcInfo);
					} else {
						for (; nameEnd < len
								&& Character.isWhitespace(expStr.charAt(nameEnd)); ++nameEnd) {
						}

						if (nameEnd == len) {
							FuncInfo funcInfo = new FuncInfo(fnName, cell, null, fnOpt);
							fnMap.put(fnName, funcInfo);
						} else if (expStr.charAt(nameEnd) == '('
								&& expStr.charAt(len - 1) == ')') {
							String[] argNames = null;
							IParam param = ParamParser.parse(
									expStr.substring(nameEnd + 1, len - 1),
									this, ctx, false);
							if (param != null) {
								argNames = param.toStringArray("func", false);
							}

							FuncInfo funcInfo = new FuncInfo(fnName, cell, argNames, fnOpt);
							fnMap.put(fnName, funcInfo);
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("func" + mm.getMessage("function.invalidParam"));
						}
					}
				}
			}
		}
		
		return fnMap;
	}
}
