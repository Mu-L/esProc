package com.scudata.expression;

import java.util.ArrayList;
import java.util.List;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.CellLocation;
import com.scudata.common.DBSession;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DBObject;
import com.scudata.dm.DataStruct;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.expression.fn.PCSFunction;
import com.scudata.expression.operator.*;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;
import com.scudata.util.Variant;

/**
 * ���ʽ����
 * @author WangXiaoJun
 *
 */
public class Expression {
	public static final Expression NULL = new Expression(new Constant(null));

	public static final byte TYPE_DB = 1; // ���ݿ�����
	public static final byte TYPE_FILE = 2; // �ļ�����
	public static final byte TYPE_SEQUENCE = 3; // ����
	public static final byte TYPE_TABLE = 4; // ���
	public static final byte TYPE_CURSOR = 5; // �α�
	
	public static final byte TYPE_OTHER = 101; // �����Ϸ���ֵ����
	public static final byte TYPE_UNKNOWN = 102; // �޷�ȷ������ֵ����

	public static final boolean DoOptimize = true;

	private String expStr;
	private int location;
	private Node home;
	private ICellSet cs;

	// �Ƿ���Լ���ȫ����ֵ���и�ֵ����ʱֻ��һ���м���
	private boolean canCalculateAll;
	
	/**
	 * �������ʽ
	 * @param str �������ı��ʽ
	 */
	public Expression(String str) {
		this(null, null, str);
	}
	
	/**
	 * �������ʽ
	 * @param ctx ����������
	 * @param str �������ı��ʽ
	 */
	public Expression(Context ctx, String str) {
		this(null, ctx, str);
	}

	/**
	 * �������ʽ
	 * @param cs ��ǰ����
	 * @param ctx ����������
	 * @param str �������ı��ʽ
	 */
	public Expression(ICellSet cs, Context ctx, String str) {
		this(cs, ctx, str, DoOptimize, true);
	}

	/**
	 * �������ʽ
	 * @param cs ��ǰ����
	 * @param ctx ����������
	 * @param str �������ı��ʽ
	 * @param opt �Ƿ����Ż�
	 */
	public Expression(ICellSet cs, Context ctx, String str, boolean opt) {
		this(cs, ctx, str, opt, true);
	}

	/**
	 * �������ʽ
	 * @param cs ��ǰ����
	 * @param ctx ����������
	 * @param str �������ı��ʽ
	 * @param opt �Ƿ����Ż�
	 * @param doMacro �Ƿ������滻
	 */
	public Expression(ICellSet cs, Context ctx, String str, boolean opt, boolean doMacro) {
		this.cs = cs;
		expStr = doMacro ? replaceMacros(str, cs, ctx) : str;
		
		if (expStr != null) {
			try {
				create(cs, ctx);
			} catch (RQException re) {
				MessageManager mm = EngineMessage.get();
				re.setMessage(mm.getMessage("Expression.inExp", expStr) + re.getMessage());
				throw re;
			}
		}
		
		if (home == null) {
			home = new Constant(null);
		} else {
			home.checkValidity();
			if (opt) {
				home = home.optimize(ctx);
			}
		}
		
		canCalculateAll = home.canCalculateAll();
	}

	/**
	 * �������ʽ
	 * @param node ���ʽ���ڵ�
	 */
	public Expression(Node node) {
		home = node;
		canCalculateAll = node.canCalculateAll();
	}
	
	/**
	 * ȡ�ýڵ�
	 * @return Node
	 */
	public Node getHome() {
		return home;
	}

	/**
	 * ���ر��ʽ�Ƿ��ǳ������ʽ
	 * @return true���ǣ�false������
	 */
	public boolean isConstExpression() {
		return home instanceof Constant;
	}
	
	/**
	 * ������ʽ
	 * @param ctx ���㱨��ʱ�������Ļ�������
	 * @return ������
	 */
	public Object calculate(Context ctx) {
		return home.calculate(ctx);
	}

	/**
	 * ��������õĵ�Ԫ�񣬲���ȡ��Ԫ���ֵ��������ʽ���ǵ�Ԫ�������򷵻ؿ�
	 * @param ctx ����������
	 * @return INormalCell
	 */
	public INormalCell calculateCell(Context ctx) {
		return home.calculateCell(ctx);
	}

	/**
	 * �Ա��ʽִ�и�ֵ����
	 * @param value Object
	 * @param ctx Context
	 */
	public void assign(Object value, Context ctx) {
		home.assign(value, ctx);
	}

	/**
	 * ȡ���ʽ��
	 * @return String
	 */
	public String toString() {
		return expStr;
	}

	/**
	 * ȡ��ʶ�����֣��õ�������������ȥ��������
	 * @return String
	 */
	public String getIdentifierName() {
		if (expStr != null) {
			int end = expStr.length() - 1;
			if (end > 0 && expStr.charAt(0) == '\'' && expStr.charAt(end) == '\'') {
				return expStr.substring(1, end); // Escape.remove()
			}
		}

		return expStr;
	}
	
	/**
	 * ȡ���ʽ��Ӧ���ֶ��������ڸ��ݱ��ʽ�Զ������ֶ���
	 * @return
	 */
	public String getFieldName() {
		return getFieldName(null);
	}
	
	/**
	 * ȡ���ʽ��Ӧ���ֶ���������new֮��ĺ���ʡ���ֶ�����ʱ��
	 * A.f���f��#1���f
	 * @param ds Դ������ݽṹ���ɿ�
	 */
	public String getFieldName(DataStruct ds) {
		if (home instanceof DotOperator) {
			Node right = home.getRight();
			if (right instanceof FieldRef) {
				String name = ((FieldRef)right).getName();
				if (name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'') {
					return name.substring(1, name.length() - 1);
				} else {
					return name;
				}
			} else {
				return expStr;
			}
		} else if (ds != null && home instanceof FieldId) {
			int c = ((FieldId)home).getFieldIndex();
			if (c < ds.getFieldCount()) {
				return ds.getFieldName(c);
			} else {
				return expStr;
			}
		} else {
			return getIdentifierName();
		}
	}
	
	/**
	 * ȡ���ʽ��Ӧ���ֶ�����
	 * @param ds ���ݽṹ
	 * @return �ֶ���������������ֶα��ʽ�򷵻�-1
	 */
	public int getFieldIndex(DataStruct ds) {
		int index = ds.getFieldIndex(expStr);
		if (index != -1) {
			return index;
		} else {
			return getFieldIndex(home, ds);
		}
	}
	
	private static int getFieldIndex(Node home, DataStruct ds) {
		if (home instanceof DotOperator) {
			Node left = home.getLeft();
			if (left instanceof DotOperator || getFieldIndex(left, ds) != -1) {
				return -1;
			}
			
			Node right = home.getRight();
			if (right instanceof FieldRef) {
				String fieldName = ((FieldRef)right).getName();
				return ds.getFieldIndex(fieldName);
			}
		} else if (home instanceof UnknownSymbol) {
			String fieldName = ((UnknownSymbol)home).getName();
			return ds.getFieldIndex(fieldName);
		} else if (home instanceof FieldId) {
			int c = ((FieldId)home).getFieldIndex();
			if (c < ds.getFieldCount()) {
				return c;
			}
		}
		
		return -1;
	}

	/**
	 * ���ʽ�Ż�
	 * @param ctx ���㱨��ʱ�������Ļ�������
	 */
	public void optimize(Context ctx) {
		home = home.optimize(ctx);
	}
	
	/**
	 * ���ʽ����Ż���������Ԫ��Ͳ������ã�
	 * @param ctx ���㱨��ʱ�������Ļ�������
	 */
	public void deepOptimize(Context ctx) {
		home = home.deepOptimize(ctx);
	}

	/**
	 * �����Ƿ����ָ������
	 * @param name String
	 * @return boolean
	 */
	public boolean containParam(String name) {
		if (name == null || name.length() == 0) {
			return false;
		}

		return home.containParam(name);
	}

	/**
	 * ���ұ��ʽ���õ��Ĳ���
	 * @param resultList ParamList �����µĲ�������
	 */
	public void getUsedParams(Context ctx, ParamList resultList) {
		home.getUsedParams(ctx, resultList);
	}
	
	/**
	 * ���ұ��ʽ�п����õ����ֶΣ�����ȡ�ò�׼ȷ���߰���������
	 * @param ctx
	 * @param resultList
	 */
	public void getUsedFields(Context ctx, List<String> resultList) {
		home.getUsedFields(ctx, resultList);
	}
	
	/**
	 * ������ʽ���ֶλ��ֶ������򷵻��ֶ������飬���򷵻�null
	 * @param exp ���ʽ
	 * @return �ֶ�������
	 */
	public String[] toFields() {
		Node node = getHome();
		
		// ���ֶ�
		if (node instanceof UnknownSymbol) {
			String [] res = new String[1];
			res[0] = ((UnknownSymbol) node).getName();
			return res;
		}
		
		if (!(node instanceof ValueList))
			return null;
		
		ValueList firList = (ValueList)node;

		//SymbolParam 
		IParam param = firList.getParam();
		if (null == param)
			return null;
		
		int count = param.getSubSize();
		if (0 >= count)
			return null;
		
		String [] res = new String[count];
		for (int i = 0; i < count; i++) {
			res[i] = param.getSub(i).getLeafExpression().getIdentifierName();
		}
		
		return res;		
	}
	
	/**
	 * ȡ���ʽ�õ��ĵ�Ԫ��
	 * @param List<INormalCell> resultList
	 */
	public void getUsedCells(List<INormalCell> resultList) {
		home.getUsedCells(resultList);
	}

	/**
	 * ���ر��ʽ���һ�����ȼ���'.'�ߵĽڵ�ķ���ֵ����
	 * @param ctx Context
	 * @return byte
	 */
	public byte getExpValueType(Context ctx) {
		Node right = home;
		while (right != null && right.getPriority() < Node.PRI_SUF) { // .�����ȼ�
			right = right.getRight();
		}

		return right == null ? TYPE_OTHER : right.calcExpValueType(ctx);
	}

	private void create(ICellSet cs, Context ctx) {
		int len = expStr.length();
		int inBrackets = 0;
		Node preNode = null;

		while (location < len) {
			char c = expStr.charAt(location);
			if (Character.isWhitespace(c)) {
				location++;
				continue;
			}

			Node node = null;
			switch (c) {
			case '(':
				if (preNode != null && !(preNode instanceof Operator)) { //A1(2)
					node = new ElementRef();
					((Function)node).setParameter(cs, ctx, scanParameter());
					break;
				} else if (preNode instanceof DotOperator) { // A1.(exp)
					node = new Calc();
					((Function)node).setParameter(cs, ctx, scanParameter());
					break;
				} else { // 1 * (2 + 3)
					inBrackets++;
					location++;
					continue;
				}
			case ')':
				inBrackets--;
				if (inBrackets < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
				}
				location++;
				continue;
			case '+':
				location++;
				if (location < len && expStr.charAt(location) == '+') { // ++
					node = new MemAdd();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // +=
					node = new AddAssign();
					location++;
				} else {
					if (preNode != null && !(preNode instanceof Operator)) {
						node = new Add();
					} else {
						node = new Plus();
					}
				}
				break;
			case '-':
				location++;
				if (location < len && expStr.charAt(location) == '-') { // --
					node = new MemSubtract();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // -=
					node = new SubtractAssign();
					location++;
				} else {
					if (preNode != null && !(preNode instanceof Operator)) {
						node = new Subtract();
					} else {
						node = new Negative();
					}
				}
				break;
			case '*':
				if (preNode == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\"*\"" + mm.getMessage("operator.missingLeftOperation"));
				}
				
				location++;
				if (location < len && expStr.charAt(location) == '*') { // **
					node = new MemMultiply();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // *=
					node = new MultiplyAssign();
					location++;
				} else {
					node = new Multiply();
				}
				break;
			case '/':
				if (preNode == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\"*\"" + mm.getMessage("operator.missingLeftOperation"));
				}

				location++;
				if (location < len && expStr.charAt(location) == '/') { // //
					node = new MemDivide();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // /=
					node = new DivideAssign();
					location++;
				} else {
					node = new Divide();
				}
				break;
			case '%':
				location++;
				if (location < len && expStr.charAt(location) == '%') { // %%
					node = new MemMod();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // %=
					node = new ModAssign();
					location++;
				} else {
					node = new Mod();
				}
				break;
			case '=':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new Equals();
					location++;
				} else {
					node = new Assign();
				}
				break;
			case '!':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new NotEquals();
					location++;
				} else {
					node = new Not();
				}
				break;
			case '>':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new NotSmaller();
					location++;
				} else {
					node = new Greater();
				}
				break;
			case '<':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new NotGreater();
					location++;
				} else {
					node = new Smaller();
				}
				break;
			case '&':
				location++;
				if (location < len && expStr.charAt(location) == '&') {
					node = new And();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // &=
					node = new UnionAssign();
					location++;
				} else {
					node = new Union(); // ����
				}
				break;
			case '^':
				location++;
				if (location < len && expStr.charAt(location) == '=') { // ^=
					node = new ISectAssign();
					location++;
				} else {
					node = new ISect();
				}
				break;
			case '|':
				location++;
				if (location < len && expStr.charAt(location) == '|') {
					node = new Or();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // |=
					node = new ConjAssign();
					location++;
				} else {
					node = new Conj();
				}
				break;
			case '\\':
				location++;
				if (location < len && expStr.charAt(location) == '\\') {
					node = new MemIntDivide();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // \=
					node = new IntDivideAssign();
					location++;
				} else {
					node = new Diff();
				}
				break;
			case ',':
				node = new Comma();
				location++;
				break;
			case '.':
				if (preNode == null || preNode instanceof Operator) {
					location++;
					String id = '.' + scanId();
					Object obj = Variant.parse(id);
					if (obj instanceof String) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("Expression.unknownExpression") + id);
					}
					node = new Constant(obj);
				} else {
					node = new DotOperator(); // series.fun  record.field
					location++;
				}
				break;
			case '"':
				int dqmatch = Sentence.scanQuotation(expStr, location);
				if (dqmatch == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\"" + mm.getMessage("Expression.illMatched"));
				}
				String str = expStr.substring(location + 1, dqmatch);
				location = dqmatch + 1;
				node = new Constant(Escape.remove(str));
				break;
			case '\'':
				int qmatch = Sentence.scanQuotation(expStr, location);
				if (qmatch == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("'" + mm.getMessage("Expression.illMatched"));
				}
				String strID = expStr.substring(location + 1, qmatch);
				location = qmatch + 1;

				if (preNode instanceof DotOperator) {
					node = new FieldRef(strID);
				} else {
					node = new UnknownSymbol(strID);
				}
				break;
			case '[':
				int match = Sentence.scanBracket(expStr, location);
				if (match == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("[,]" + mm.getMessage("Expression.illMatched"));
				}

				if (preNode == null || preNode instanceof Operator) {
					node = new ValueList(); // ����[elm1, elm2, ...]
				} else { // series[2] field[2] series.field[2] series[2].field H[hc] F[hc]
					node = new Move();
				}

				((Function)node).setParameter(cs, ctx, expStr.substring(location + 1, match));
				location = match + 1;
				break;
			case '{':
				match = Sentence.scanBrace(expStr, location);
				if (match == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("{,}" + mm.getMessage("Expression.illMatched"));
				}

				if (preNode == null || preNode instanceof Operator) {
					//{} ��¼���ʽ
					node = new CreateRecord();
				} else {
					// �ź�����
					node = new Moves();
				}
				
				((Function)node).setParameter(cs, ctx, expStr.substring(location + 1, match));
				location = match + 1;
				break;
			default:
				node = createNode(cs, ctx, preNode);
			}

			// x y	���ִ�����x��y�ϲ�
			if (preNode instanceof Constant && node instanceof Constant &&
				((Constant)preNode).append((Constant)node)) {
				continue;
			}

			node.setInBrackets(inBrackets);
			preNode = node;
			if (home == null) {
				home = node;
			} else {
				Node right = home;
				Node parent = null;

				while (right != null && right.getPriority() < node.getPriority()) {
					parent = right;
					right = right.getRight();
				}
				node.setLeft(right);
				if (parent != null) {
					parent.setRight(node);
				} else {
					home = node;
				}
			}
		}

		if (inBrackets > 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
		}
	}

	private Node createNode(ICellSet cs, Context ctx, Node preNode) {
		String id = scanId();
		int idLen = id.length();
		if (idLen < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + expStr.charAt(location));
		}

		if (KeyWord.isCurrentElement(id)) {
			return new CurrentElement(); // ~, A.~
		} else if (KeyWord.isIterateParam(id)) {
			return new IterateParam(); // ~~
		} else if (KeyWord.isCurrentSeq(id)) {
			return new CurrentSeq(); // #, A.#
		} else if (KeyWord.isFieldId(id)) {
			return new FieldId(id); // #n, r.#n
		} else if (KeyWord.isCurrentCellSeq(id)) {
			return new CurrentCellSeq(); // #@
		} else if (KeyWord.isElementId(id)) {
			return new CurrentElementId(id); // ~n
		} else if (KeyWord.isArg(id)) { // ?��?n
			return new ArgNode(id);
		}

		//����series.select(...),series.field
		if (preNode instanceof DotOperator) {
			return createMemberNode(cs, id, ctx);
		}

		if (id.startsWith(KeyWord.CURRENTSEQ)) { // #A1 #F
			if (cs instanceof PgmCellSet) {
				INormalCell cell = cs.getCell(id.substring(1));
				if (cell != null) {
					return new ForCellCurSeq((PgmCellSet)cs, cell.getRow(), cell.getCol());
				}
			}
			
			return new FieldFuzzyRef(id.substring(1));
		}

		if (id.equals("$") && isNextChar('[')) { // �ַ���
			int index = expStr.indexOf('[', location);
			int match = Sentence.scanBracket(expStr, index);
			if (match != -1) {
				location = match + 1;
				return new Constant(Escape.remove(expStr.substring(index + 1, match).trim()));
			}
		}

		if (cs != null) {
			INormalCell cell = cs.getCell(id);
			if (cell != null) return new CSVariable(cell);

			if (KeyWord.isCurrentCell(id)) return new CurrentCell(cs);

			if (KeyWord.isSubCodeBlock(id) && cs instanceof PgmCellSet) {
				return new SubVal((PgmCellSet)cs);
			}
		}

		//����������
		Param var = EnvUtil.getParam(id, ctx);
		if (var != null) {
			// connection�ɲ�������
			Object val = var.getValue();
			if (val instanceof DBSession) {
				return new Constant(new DBObject((DBSession)val));
			}
			
			byte kind = var.getKind();
			switch (kind) {
			case Param.VAR:
				return new VarParam(var);
			case Param.ARG:
				return new ArgParam(var);
			default:
				return new ConstParam(id, var.getValue());
			}
		}
		
		// ���Ƚ����ɱ���������ͺ�����������function@���ú���
		// function() or function@opt()
		if (isNextChar('(')) {
			int atIdx = id.indexOf(KeyWord.OPTION);
			String fnName = id;
			String fnOpt = null;
			
			if (atIdx != -1) {
				fnName = id.substring(0, atIdx);
				fnOpt = id.substring(atIdx + 1);
			}

			if (FunctionLib.isFnName(fnName)) {
				Function fn = FunctionLib.newFunction(fnName);
				fn.setOption(fnOpt);
				fn.setParameter(cs, ctx, scanParameter());
				return fn;
			}
			
			DfxFunction dfx = Context.getDFXFunction(fnName, ctx);
			if (dfx != null) {
				String param = scanParameter();
				return dfx.newFunction(cs, ctx, fnOpt, param);
			}
			
			if (cs instanceof PgmCellSet) {
				PgmCellSet.FuncInfo funcInfo = ((PgmCellSet)cs).getFuncInfo(fnName);
				if (funcInfo != null) {
					Function fn = new PCSFunction(funcInfo);
					fn.setOption(fnOpt);
					fn.setParameter(cs, ctx, scanParameter());
					return fn;
				}
			}
		}
		
		if (ctx != null) {
			DBSession dbs = ctx.getDBSession(id);
			if (dbs != null) {
				return new Constant(new DBObject(dbs));
			}
		}

		// ���������ж�
		if (isNextChar('.') && isNumber(id)) {
			int prevPos = location++;
			if (isNextChar('(')) { // n.()
				location = prevPos;
			} else {
				Object obj = Variant.parse(id + '.' + scanId());
				if (obj instanceof String) {
					location = prevPos;
				} else {
					return new Constant(obj);
				}
			}
		}

		Object value = Variant.parse(id);
		if (value instanceof String) { // �ֶ���;
			return new UnknownSymbol( (String) value);
		} else {
			return new Constant(value);
		}
	}

	//�������к�������,������
	private Node createMemberNode(ICellSet cs, String id, Context ctx) {
		if (isNextChar('(')) {
			int atIdx = id.indexOf(KeyWord.OPTION);
			String fnName = id;
			String fnOpt = null;
			
			// A.@m(x)
			if (atIdx == 0) {
				fnOpt = id.substring(1);
				Calc calc = new Calc();
				calc.setOption(fnOpt);
				calc.setParameter(cs, ctx, scanParameter());
				return calc;
			}
			
			if (atIdx != -1) {
				fnName = id.substring(0, atIdx);
				fnOpt = id.substring(atIdx + 1);
			}

			if (FunctionLib.isMemberFnName(fnName)) {
				MemberFunction mfn = FunctionLib.newMemberFunction(fnName);
				mfn.setOption(fnOpt);
				mfn.setParameter(cs, ctx, scanParameter());
				return mfn;
			}
		}

		if (id.startsWith(KeyWord.CURRENTSEQ)) { // ~.#F
			return new FuzzyFieldRef(id.substring(1));
		} else {
			return new FieldRef(id);
		}
	}

	// ������һ���ַ��Ƿ���c�����ַ�����
	private boolean isNextChar(char c) {
		int len = expStr.length();
		for (int i = location; i < len; ++i) {
			if (expStr.charAt(i) == c) {
				return true;
			} else if (!Character.isWhitespace(expStr.charAt(i))) {
				return false;
			}
		}
		return false;
	}

	private boolean isNumber(String num) {
		int length = num.length();
		for (int i = 0; i < length; ++i) {
			char c = num.charAt(i);
			if (c < '0' || c > '9') return false;
		}
		return true;
	}

	private String scanId() {
		int len = expStr.length();
		int begin = location;
		
		while (location < len) {
			char c = expStr.charAt(location);
			if (KeyWord.isSymbol(c)) {
				break;
			} else {
				location++;
			}
		}
		
		return expStr.substring(begin, location);
	}

	private String scanParameter() {
		int len = expStr.length();
		while (location < len) {
			char c = expStr.charAt(location);
			if (Character.isWhitespace(c)) {
				location++;
			} else {
				break;
			}
		}
		
		if (location == len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.missingParam"));
		}
		
		char c = expStr.charAt(location);
		if (c != '(') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.missingParam"));
		}
		
		int match = scanParenthesis(expStr, location);
		if (match == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
		}
		
		String param = expStr.substring(location + 1, match);
		location = match + 1;
		return param;
	}

	/**
	 * ���к��滻
	 * @param text String ${macroName}, $(cellName), $(), $C$1, ~$r
	 * @param cs ICellSet
	 * @param ctx Context
	 * @return String
	 */
	public static String replaceMacros(String text, ICellSet cs, Context ctx) {
		if (text == null) return null;

		int len = text.length();
		StringBuffer newStr = null;

		PgmCellSet pcs = null;
		if (cs instanceof PgmCellSet) pcs = (PgmCellSet)cs;

		int idx = 0;
		while(idx < len) {
			char c = text.charAt(idx);
			// �ַ����е�$�����滻
			if (c == '\'' || c == '\"') {
				int match = Sentence.scanQuotation(text, idx);
				if (match < 0) {
					if (newStr != null) newStr.append(c);
					idx += 1;
				} else {
					if (newStr != null) newStr.append(text.substring(idx, match + 1));
					idx = match + 1;
				}
			} else if (KeyWord.isSymbol(c)) {
				if (newStr != null) newStr.append(c);
				idx += 1;
			} else {
				int last = KeyWord.scanId(text, idx + 1);
				char lc = text.charAt(last - 1);
				if (last < len && lc == '$') { // str${}, str$()
					char nc = text.charAt(last);
					if (nc == '{') {
						int match = Sentence.scanBrace(text, last);
						if (match == -1) { // ���Ų�ƥ��
							MessageManager mm = EngineMessage.get();
							throw new RQException("{,}" + mm.getMessage("Expression.illMatched"));
						}

						if (newStr == null) {
							newStr = new StringBuffer(len + 80);
							newStr.append(text.substring(0, idx));
						}

						newStr.append(text.substring(idx, last - 1));
						newStr.append(getMacroValue(text.substring(last + 1, match), cs, ctx));
						idx = match + 1;
					} else if (nc == '(') {
						// $(c)  $()
						int match = scanParenthesis(text, last);
						if (match == -1) { // ���Ų�ƥ��
							MessageManager mm = EngineMessage.get();
							throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
						}

						if (pcs == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("$()" + mm.getMessage("Expression.missingCs"));
						}

						String strCell = text.substring(last + 1, match).trim();
						String retStr = pcs.getMacroReplaceString(strCell);

						if (newStr == null) {
							newStr = new StringBuffer(len + 80);
							newStr.append(text.substring(0, idx));
						}

						newStr.append(text.substring(idx, last - 1));
						newStr.append(retStr);
						idx = match + 1;
					} else {
						if (newStr != null) newStr.append(text.substring(idx, last));
						idx = last;
					}
				} else {
					// id, $A$1, ~$r, #$A$1, #$A
					String subStr = text.substring(idx, last);
					String cellId = removeAbsoluteSymbol(subStr);
					if (cellId == null) {
						if (newStr != null) newStr.append(subStr);
					} else {
						if (newStr == null) {
							newStr = new StringBuffer(len + 80);
							newStr.append(text.substring(0, idx));
						}

						newStr.append(cellId);
					}

					idx = last;
				}
			}
		}

		return newStr == null ? text : newStr.toString();
	}
	
	/**
	 * �����ı��Ƿ�������滻
	 * @param text
	 * @return
	 */
	public static boolean containMacro(String text) {
		if (text == null) {
			return false;
		}

		int len = text.length();
		int idx = 0;
		while(idx < len) {
			char c = text.charAt(idx);
			// �ַ����е�$�����滻
			if (c == '\'' || c == '\"') {
				int match = Sentence.scanQuotation(text, idx);
				if (match < 0) {
					return false;
				} else {
					idx = match + 1;
				}
			} else if (KeyWord.isSymbol(c)) {
				idx += 1;
			} else {
				int last = KeyWord.scanId(text, idx + 1);
				char lc = text.charAt(last - 1);
				if (last < len && lc == '$') { // str${}, str$()
					char nc = text.charAt(last);
					if (nc == '{') {
						int match = Sentence.scanBrace(text, last);
						return match != -1;
					} else if (nc == '(') {
						// $(c)  $()
						int match = scanParenthesis(text, last);
						if (match == -1) { // ���Ų�ƥ��
							return false;
						} else {
							idx = match + 1;
						}
					} else {
						idx = last;
					}
				} else {
					idx = last;
				}
			}
		}

		return false;
	}

	// $A$1, #$A$1ȥ��$, �������������ʽ����null
	private static String removeAbsoluteSymbol(String text) {
		if (text.length() < 3) return null;

		char ch = text.charAt(0);
		if (ch == KeyWord.CELLPREFIX) { // #
			ch = text.charAt(1);
			if (ch == '$') {
				int idx2 = text.indexOf('$', 3);
				if (idx2 == -1) { // #$A1
					String strId = text.substring(2);
					if (CellLocation.parse(strId) != null) {
						return KeyWord.CELLPREFIX + text.substring(2);
					} else {
						return null;
					}
				} else {
					String strCell = text.substring(2, idx2) + text.substring(idx2 + 1);
					if (CellLocation.parse(strCell) != null) {
						return KeyWord.CELLPREFIX + strCell;
					} else {
						return null;
					}
				}
			} else {
				int idx1 = text.indexOf('$', 2);
				if (idx1 == -1) {
					return null;
				} else {
					// #A$1
					String strCell = text.substring(1, idx1) + text.substring(idx1 + 1);
					if (CellLocation.parse(strCell) != null) {
						return KeyWord.CELLPREFIX + strCell;
					} else {
						return null;
					}
				}
			}
		} else if (ch == '$') {
			int idx2 = text.indexOf('$', 2);
			if (idx2 == -1) { // $A1
				String strId = text.substring(1);
				if (CellLocation.parse(strId) != null) {
					return text.substring(1);
				} else {
					return null;
				}
			} else {
				// $A$1
				String strCell = text.substring(1, idx2) + text.substring(idx2 + 1);
				return CellLocation.parse(strCell) == null ? null : strCell;
			}
		} else {
			int idx1 = text.indexOf('$');
			if (idx1 == -1) return null;

			// A$1
			String strCell = text.substring(0, idx1) + text.substring(idx1 + 1);
			return CellLocation.parse(strCell) == null ? null : strCell;
		}
	}

	private static String getMacroValue(String str, ICellSet cs, Context ctx) {
		Expression exp = new Expression(cs, ctx, str);
		Object obj = exp.calculate(ctx);
		if (obj instanceof String) {
			return (String)obj;
		} else if (obj == null) {
			return "";
		} else {
			// �Զ�ת���ַ���
			return Variant.toString(obj);
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("Variant2.macroTypeError"));
		}
	}
	
	/**���Ʊ��ʽ�����ڶ��̼߳���
	 * @param ctx Context
	 */
	public Expression newExpression(Context ctx) {
		if (expStr != null) {
			return new Expression(cs, ctx, expStr, true, false);
		} else {
			return new Expression(home);
		}
	}

	/**
	 * ������һ��ƥ���Բ���ţ������ڵ�Բ���ű�������������ת���
	 * @param str ��Ҫ�������ŵ�ԭ��
	 * @param start  ��ʼλ��,����Բ������ԭ���е�λ��
	 * @return ���ҵ�,�򷵻�ƥ�����Բ������ԭ���е�λ��,���򷵻�-1
	 */
	public static int scanParenthesis(String str, int start) {
		//if (str.charAt(start) != '(') return -1;

		int len = str.length();
		for (int i = start + 1; i < len;) {
			char ch = str.charAt(i);
			switch (ch) {
			case '(':
				i = scanParenthesis(str, i);
				if (i < 0)
					return -1;
				i++;
				break;
			case '\"':
			case '\'':
				int q = Sentence.scanQuotation(str, i, '\\');
				if (q < 0) {
					i++;
				} else {
					i = q + 1;
				}
				break;
			case '[': // $[str]
				if (i > start && str.charAt(i - 1) == '$') {
					q = Sentence.scanBracket(str, i, '\\');
					if (q < 0) {
						i++;
					} else {
						i = q + 1;
					}
				} else {
					i++;
				}
				break;
			case ')':
				return i;
			default:
				i++;
				break;
			}
		}
		return -1;
	}
	
	/**
	 * �ж�һ����������Ƿ������һ��������һ�£���������
	 * @param obj	���жϵ���
	 * @param base	�ο�����
	 * @return	true	obj������Ϊbase��
	 */
	public static boolean ifIs(Object obj, Class<?> base) {
		if (null == obj)
			return false;
		if (obj.getClass() == base
				|| (null != obj.getClass() && obj.getClass().getSuperclass() == base))
			return true;
		return false;
	}
	
	/**
	 * ��һ���ַ����е�ĳ�������滻Ϊ�µ��ַ���
	 * 
	 * @param src	Դ�ַ���
	 * @param func	Ҫ��ɾ���ĺ�����
	 * @param newStr	�滻�����ַ���
	 * 					Ϊnull��Ϊ�գ���ʾɾ��
	 * @return
	 */
	public static String replaceFunc(String src, String func, String newStr) {
		/*int start = src.indexOf(func);
		if (start != -1) {
			return src.substring(0, start) + newStr + src.substring(start + func.length());
		} else {
			return src;
		}*/
		return StringUtils.replace(src, func, newStr);
	}

	/**
	 * ȡ�ò�������������Ϊ�ο��࣬���Բο���Ϊ�������
	 * 
	 * @param op	��Ӧ������
	 * @param base	Ҫȡ�õĺ���������
	 * @return	���ض�Ӧ������
	 */
	public static ArrayList<Object> getSpecFunc(Operator op, Class<?> base) {
		ArrayList<Object> classes;
		Node left = op.getLeft();
		
		if (ifIs(left, base)) {
			classes = new ArrayList<Object>();
			classes.add(left);
		} else if (left instanceof Function){
			classes = getSpecFunc((Function)left, base);
		} else if (left instanceof Operator) {
			classes = getSpecFunc((Operator)left, base);
		} else {
			classes = new ArrayList<Object>();
		}
		
		Node right = op.getRight();
		if (ifIs(right, base)) {
			classes.add(right);
		} else if (right instanceof Function){
			classes.addAll(getSpecFunc((Function)right, base));
		} else if (right instanceof Operator) {
			classes.addAll(getSpecFunc((Operator)right, base));
		}
		
		return classes;
	}
	
	/**
	 * ȡ�ú�����������Ϊ�ο��࣬���Բο���Ϊ�������
	 * 
	 * @param op	��Ӧ�ĺ���
	 * @param base	Ҫȡ�õĺ���������
	 * @return	���ض�Ӧ������
	 */
	public static ArrayList<Object> getSpecFunc(Function fun, Class<?> base) {
		ArrayList<Object> funcs;
		Node left = fun.getLeft();
		if (left == null) {
			funcs = new ArrayList<Object>();
		} else if (ifIs(left, base)) {
			funcs = new ArrayList<Object>();
			funcs.add(left);
		} else if (left instanceof Function){
			funcs = getSpecFunc((Function)left, base);
		} else if (left instanceof Operator) {
			funcs = getSpecFunc((Operator)left, base);
		} else {
			funcs = new ArrayList<Object>();
		}
		
		IParam par = fun.getParam();
		if (null == par)
			return funcs;
		int subCount = par.getSubSize();
		if (0 == subCount) {
			if (ifIs(fun, base)) {
				//String str = ((Gather)par).getFunctionString();
				funcs.add(fun);
			} else if (par instanceof Function){
				funcs.addAll(getSpecFunc((Function)par, base));
			} else if (par instanceof Operator) {
				funcs.addAll(getSpecFunc((Operator)par, base));
			} else if (par instanceof Expression) {
				funcs.addAll(getSpecFunc((Expression)par, base));
			} else if (par.isLeaf()) {
				funcs.addAll(getSpecFunc(par.getLeafExpression(), base));
			} else {
				funcs.addAll(getSpecFunc(par, base));
			}
		}
		
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = par.getSub(i);
			if (null == sub) {
				continue;
			} else if (ifIs(sub, base)) {
				funcs.add(sub);
			} else if (sub instanceof Function){
				funcs.addAll(getSpecFunc((Function)sub, base));
			} else if (sub instanceof Operator) {
				funcs.addAll(getSpecFunc((Operator)sub, base));
			} else if (sub instanceof Expression) {
				funcs.addAll(getSpecFunc((Expression)sub, base));
			} else if (sub.isLeaf()) {
				funcs.addAll(getSpecFunc(sub.getLeafExpression(), base));
			} else {
				funcs.addAll(getSpecFunc(sub, base));
			}
		}

		return funcs;
	}
	
	/**
	 * ȡ�ò�����������Ϊ�ο��࣬���Բο���Ϊ�������
	 * 
	 * @param param	��Ӧ�Ĳ���
	 * @param base	Ҫȡ�õĺ���������
	 * @return	���ض�Ӧ������
	 */
	public static ArrayList<Object> getSpecFunc(IParam param, Class<?> base) {
		ArrayList<Object> funcs = new ArrayList<Object>();

		int subCount = param.getSubSize();
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = param.getSub(i);
			if (null == sub) {
				continue;
			} else if (ifIs(sub, base)) {
				funcs.add(sub);
			} else if (sub instanceof Function){
				funcs.addAll(getSpecFunc((Function)sub, base));
			} else if (sub instanceof Operator) {
				funcs.addAll(getSpecFunc((Operator)sub, base));
			} else if (sub instanceof Expression) {
				funcs.addAll(getSpecFunc((Expression)sub, base));
			} else if (sub.isLeaf()) {
				funcs.addAll(getSpecFunc(sub.getLeafExpression(), base));
			} else {
				funcs.addAll(getSpecFunc(sub, base));
			}
		}

		return funcs;
	}
	
	/**
	 * ȡ�ñ��ʽ��������Ϊ�ο��࣬���Բο���Ϊ�������
	 * 
	 * @param op	��Ӧ������
	 * @param type	Ҫȡ�õĺ���������
	 * 		1-- ����ΪGather����
	 * 		2-- ����ΪGatherEx����
	 * @return	���ض�Ӧ������
	 */
	public static ArrayList<Object> getSpecFunc(Expression exp, Class<?> base) {
		
		ArrayList<Object> funcs = new ArrayList<Object>();
		Node home = exp.getHome();
		
		if (null == home) {
			return funcs;
		} else if (ifIs(home, base)) {
			funcs.add(home);
		} else if (home instanceof Function){
			funcs.addAll(getSpecFunc((Function)home, base));
		} else if (home instanceof Operator) {
			funcs.addAll(getSpecFunc((Operator)home, base));
		} 
		
		return funcs;
	}
	

	/**
	 * �ж��������ʽ�ַ����Ƿ�ȼ�
	 * 		���ַ�����ȫ��ͬ����һ���ȼۡ�
	 * 		����˫���ź͵������ڵĿո�ɺ���
	 * 	�ú���û�п���ת���������������ֻ���ڴ��Թ���ĳ��ϣ�Ŀ���ǽ��90%�����⡣
	 * @param exp1	��һ�����ʽ
	 * @param exp2      �ڶ������ʽ
	 * @return	true	�������ʽ�ȼ�
	 * 			false	�������ʽ���ȼ�
	 */
	public static boolean sameExpression(String exp1, String exp2) {
	    /** �ַ���1����**/
		int len = exp1.length();
		/** �Աȵ����ַ���2��λ�� **/
	    int set = 0;
	    /** �Ƿ��ڽ��������ڵöԱ� **/
	    boolean bstr = false;
	    for (int i = 0; i < len; i++) {
	    	char ch1 = exp1.charAt(i);
	    	char ch2 = exp2.charAt(set);
	    	// ����exp1�ո�
	    	if (!bstr && ' ' == ch1)
	    		continue;
	    	
	    	// ����exp2�ո�
	    	if (!bstr && ' ' == ch2) {
	    		while (' ' == ch2) {
	    			set++;
	    			ch2 = exp2.charAt(set);
	    		}
	    	}
	    	
	    	// �����Ƚ�
	    	if (ch1 != ch2)
	    		return false;
	    	
	    	// �������ű��
	    	if ('\"' == ch1 || '\'' == ch1) {
	    		bstr = !bstr;
	    	}
	    }
		return true;
	}
	/**
	 * ȡ�ò����������µľۺϺ���
	 * 
	 * @param op	��Ӧ������
	 * @return
	 */
	public static ArrayList<String> gatherParam(Operator op) {
		
		ArrayList<String> gathers = new ArrayList<String>();
		
		Node left = op.getLeft();
		if (left instanceof Gather) {
			String str = ((Gather)left).getFunctionString();
			gathers.add(0, str);
		} else if (left instanceof Function){
			ArrayList<String> temp = gatherParam((Function)left);
			temp.addAll(gathers);
			gathers = temp;
		} else if (left instanceof Operator) {
			ArrayList<String> temp = gatherParam((Operator)left);
			temp.addAll(gathers);
			gathers = temp;
		}
		
		Node right = op.getRight();
		if (right instanceof Gather) {
			String str = ((Gather)right).getFunctionString();
			gathers.add(str);
		} else if (right instanceof Function){
			gathers.addAll(gatherParam((Function)right));
		} else if (right instanceof Operator) {
			gathers.addAll(gatherParam((Operator)right));
		}
		
		return gathers;
	}
	
	/**
	 * ȡ�ú��������л���ΪGather�ĺ��������������еġ�
	 * @param fun
	 * @return
	 */
	public static ArrayList<String> gatherParam(Function fun) {
		ArrayList<String> gathers = new ArrayList<String>();
		
		IParam par = fun.getParam();
		if (null == par) {
			return gathers;
		}
		
		int subCount = par.getSubSize();
		if (0 == subCount) {
			if (par instanceof Gather) {
				String str = ((Gather)par).getFunctionString();
				gathers.add(str);
			} else if (par instanceof Function){
				gathers.addAll(gatherParam((Function)par));
			} else if (par instanceof Operator) {
				gathers.addAll(gatherParam((Operator)par));
			} else if (par instanceof Expression) {
				gathers.addAll(gatherParam((Expression)par));
			} else if (par.isLeaf()) {
				gathers.addAll(gatherParam(par.getLeafExpression()));
			} else {
				gathers.addAll(gatherParam(par));
			}
		}
		
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = par.getSub(i);
			if (null == sub) {
				continue;
			} else if (sub instanceof Gather) {
				String str = ((Gather)sub).getFunctionString();
				gathers.add(str);
			} else if (sub instanceof Function){
				gathers.addAll(gatherParam((Function)sub));
			} else if (sub instanceof Operator) {
				gathers.addAll(gatherParam((Operator)sub));
			} else if (sub instanceof Expression) {
				gathers.addAll(gatherParam((Expression)sub));
			} else if (sub.isLeaf()) {
				gathers.addAll(gatherParam(sub.getLeafExpression()));
			} else {
				gathers.addAll(gatherParam(sub));
			}
		}

		return gathers;
	}
	
	/**
	 * ȡ�ò����µľۺϺ���
	 * @param par	
	 * @return
	 */
	public static ArrayList<String> gatherParam(IParam param) {
		ArrayList<String> gathers = new ArrayList<String>();

		int subCount = param.getSubSize();
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = param.getSub(i);
			if (null == sub) {
				continue;
			} else if (sub instanceof Gather) {
				String str = ((Gather)sub).getFunctionString();
				gathers.add(str);
			} else if (sub instanceof Function){
				gathers.addAll(gatherParam((Function)sub));
			} else if (sub instanceof Operator) {
				gathers.addAll(gatherParam((Operator)sub));
			} else if (sub instanceof Expression) {
				gathers.addAll(gatherParam((Expression)sub));
			} else if (sub.isLeaf()) {
				gathers.addAll(gatherParam(sub.getLeafExpression()));
			} else {
				gathers.addAll(gatherParam(sub));
			}
		}
		
		
		return gathers;
	}
	
	/**
	 * ȡ�ñ��ʽ�����л���ΪGather�ĺ��������������еġ�
	 * @param exp
	 * @return
	 */
	public static ArrayList<String> gatherParam(Expression exp) {
		ArrayList<String> gathers = new ArrayList<String>();
		Node home = exp.getHome();
		
		if (null == home) {
			return gathers;
		} else if (home instanceof Gather) {
			String str = ((Gather)home).getFunctionString();
			gathers.add(str);
		} else if (home instanceof Function){
			gathers.addAll(gatherParam((Function)home));
		} else if (home instanceof Operator) {
			gathers.addAll(gatherParam((Operator)home));
		} 
		
		return gathers;
	}
	
	/**
	 * ����������еĽ��
	 * @param ctx ����������
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (canCalculateAll) {
			return home.calculateAll(ctx);
		} else {
			Current current = ctx.getComputeStack().getTopCurrent();
			int len = current.length();
			ObjectArray array = new ObjectArray(len);
			array.setTemporary(true);
			
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object value = home.calculate(ctx);
				array.push(value);
			}
			
			return array;
		}
	}
	/**
	 * �ж��Ƿ���Լ���ȫ����ֵ���и�ֵ����ʱֻ��һ���м���
	 * @return
	 */
	public boolean canCalculateAll() {
		return canCalculateAll;
	}
	
	/**
	 * ������ʽ��ȡֵ��Χ
	 * @param ctx ����������
	 * @return
	 */
	public IArray calculateRange(Context ctx) {
		return home.calculateRange(ctx);
	}
	
	/**
	 * �жϸ�����ֵ��Χ�Ƿ����㵱ǰ�������ʽ
	 * @param ctx ����������
	 * @return ȡֵ����Relation. -1��ֵ��Χ��û������������ֵ��0��ֵ��Χ��������������ֵ��1��ֵ��Χ��ֵ����������
	 */
	public int isValueRangeMatch(Context ctx) {
		return home.isValueRangeMatch(ctx);
	}
	
	/**
	 * �����߼��������&&���Ҳ���ʽ
	 * @param ctx ����������
	 * @param leftResult &&�����ʽ�ļ�����
	 * @return
	 */
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		if (leftResult == null) {
			return home.calculateAll(ctx);
		} else {
			return home.calculateAnd(ctx, leftResult);
		}
	}
	
	/**
	 * ����signArray��ȡֵΪsign����
	 * @param ctx
	 * @param signArray �б�ʶ����
	 * @param sign ��ʶ
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		return home.calculateAll(ctx, signArray, sign);
	}

	/**
	 * ���ؽڵ��Ƿ񵥵�������
	 * @return true���ǵ��������ģ�false������
	 */
	public boolean isMonotone() {
		return home.isMonotone();
	}
	
	/**
	 * ���ñ��ʽ�����ڱ��ʽ���棬���ִ��ʹ�ò�ͬ�������ģ�������������йصĻ�����Ϣ
	 */
	public void reset() {
		home.reset();
	}
}
