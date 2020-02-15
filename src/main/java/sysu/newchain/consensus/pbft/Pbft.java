package sysu.newchain.consensus.pbft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.RspList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.msg.BlockMsg;
import sysu.newchain.consensus.pbft.msg.CommitMsg;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.PrePrepareMsg;
import sysu.newchain.consensus.pbft.msg.PrepareMsg;
import sysu.newchain.consensus.pbft.msg.log.MsgLog;
import sysu.newchain.consensus.pbft.msg.log.PhaseShiftHandler;
import sysu.newchain.core.Block;
import sysu.newchain.properties.AppConfig;
import sysu.newchain.properties.NodeInfo;
import sysu.newchain.properties.NodesProperties;
import sysu.newchain.proto.BlockPb;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.NewchainProto;
import sysu.newchain.proto.MsgWithSignPb.MsgCase;
import sysu.newchain.proto.PrePreparePb;
import sysu.newchain.proto.PreparePb;
import sysu.newchain.proto.TransactionPb;

/**
 * @Description TODO
 * @author jongliao
 * @date 2020��1��20�� ����10:24:02
 */
public class Pbft extends ReceiverAdapter implements PhaseShiftHandler{
	
	public static final Logger logger = LoggerFactory.getLogger(Pbft.class);
	
	JChannel channel;
	ECKey ecKey;
	long nodeId; // �ڵ�id
	long size; // �ܽڵ���
	long f; // �����̰�ռͥ����ڵ���
	boolean isPrimary = false; // �Ƿ�Ϊ���ڵ�
	AtomicLong view = new AtomicLong(0); // ��ͼ���
	AtomicLong seqNum = new AtomicLong(0); // �������кţ�����߶ȣ�
	
	List<Function<View, Void>> roleChangeListeners = new ArrayList<Function<View,Void>>();
	MsgLog msgLog = new MsgLog();
	public Pbft() throws Exception {
		ecKey = ECKey.fromPrivate(Base58.decode(AppConfig.getNodePriKey()));
		channel = new JChannel();
		// ���ýڵ�id
		nodeId = AppConfig.getNodeId();
		channel.setName(String.valueOf(nodeId));
		channel.setDiscardOwnMessages(true);
		channel.setReceiver(this);
		size = NodesProperties.getNodesSize();
		f = (size - 1) / 3; // TODO ��ȷ��
		msgLog.setF(f);
		msgLog.setHandler(this);
	}
	
	public void start() throws Exception {
		logger.info("start pbft");
		// ���ӵ�ָ����Ⱥ
		channel.connect("pbft");
//		IpAddress physicalAddress = (IpAddress) channel.down(new Event( 
//				Event.GET_PHYSICAL_ADDRESS, channel.getAddress())); 
//		String ip = physicalAddress.getIpAddress().getHostAddress(); 
//		int port = physicalAddress.getPort();
//		System.out.println(ip + ":" + port);
		// ������ڵ�
		checkPrimary();
		logger.info("nodeId: {}, primary: {}, view: {}, nodeSize: {}", nodeId, getPrimary(), view.get(), size);
	}
	
	public Address getAddress(long nodeId) {
		for (Address address: channel.getView().getMembers()) {
			if (address.toString().equals(String.valueOf(nodeId))) {
				return address;
			}
		}
		return null;
	}
	
	public boolean isPrimary() {
		return isPrimary;
	}
	
	public long getPrimary() {
		return view.get() % size;
	}

	private void stop() {
		channel.close();
	}
	
	public long incrementAndGetSeqNum() {
		return seqNum.incrementAndGet();
	}
	
	/**
	 * @Description: ��ӽ�ɫת�������
	 * @param listener
	 * @return �Ƿ���ӳɹ�
	 */
	public boolean addRoleChangeListeners(Function<View, Void> listener) {
		return roleChangeListeners.add(listener);
	}
	
//	// key(view, reqNum) -> pre-prepare
//	Map<String, MsgWithSign> prePrepareMsgs = new HashMap<>();
//	
//	// key(view, seqNum, digest) -> (replica -> prepare)
//	Map<String, Map<Long, MsgWithSign>> prepareMsgs = new HashMap<>();
//	
//	// key(view, seqNum, digest) -> (replica -> prepare)
//	Map<String, Map<Long, MsgWithSign>> commitMsgs = new HashMap<>();
//	
	@Override
	public void receive(Message msg) {
		try {
			MsgWithSign msgWithSign = new MsgWithSign(msg.getBuffer());
//			logger.debug("bytes to sign: {}, pubKey: {}, sign: {}", 
//					Hex.encode(msgWithSign.getBytesToSign()), 
//					NodesProperties.get(getPrimary()).getPubKey(),
//					Hex.encode(msgWithSign.getSign()));
			logger.debug("msg type: {}", msgWithSign.getMsgCase());
			switch (msgWithSign.getMsgCase()) {
				case PREPREPAREMSG:
					onPrePrepare(msgWithSign);
					break;
				case PREPAREMSG:
					onPrepare(msgWithSign);
					break;
				case COMMITMSG:
					onCommit(msgWithSign);
				default:
					break;
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	/** ���ڵ���յ�������Ϣ��Ŀǰֻ���������������ڰ��������б�ÿ�����������ɷ���Ŀͻ���ǩ��
	 * @param operator
	 * @throws Exception
	 */
	public void onRequest(byte[] operator) throws Exception {
		logger.debug("onRequest");
		if (isPrimary) {
			MsgWithSign msgWithSign = new MsgWithSign(operator);
			switch (msgWithSign.getMsgCase()) {
			case BLOCKMSG:
				onBlock(msgWithSign.getBlockMsg());
				break;
			default:
				break;
			}
		}
		else {
			throw new Exception("not primary!");
		}
	}
	
	/** ���յ��������󣬽���pre-prepare�׶�
	 * @param blockMsg
	 * @throws Exception
	 */
	private void onBlock(BlockMsg blockMsg) throws Exception {
		logger.debug("onBlock");
		PrePrepareMsg prePrepareMsg = new PrePrepareMsg();
		prePrepareMsg.setView(view.get());
		// assigns a sequence number
		prePrepareMsg.setSeqNum(seqNum.incrementAndGet());
		prePrepareMsg.setBlockMsg(blockMsg);
		prePrepareMsg.calculateAndSetDigestOfBlock();
		MsgWithSign msgWithSign = new MsgWithSign();
		msgWithSign.setPrePrepareMsg(prePrepareMsg);
		msgWithSign.calculateAndSetSign(ecKey);
//		logger.debug("BytesToSign: {}, priKey {} , sign: {}", 
//				Hex.encode(msgWithSign.getBytesToSign()), 
//				ecKey.getPriKeyAsBase58(),
//				Hex.encode(msgWithSign.getSign()));
		msgLog.add(msgWithSign);
		logger.debug("multicasts a pre-prepare message, {}", prePrepareMsg);
		channel.send(new Message(null, msgWithSign.toByteArray()));
	}
	
	/** ���յ�pre-prepare��Ϣ
	 * @param msgWithSign
	 * @throws Exception
	 */
	private void onPrePrepare(MsgWithSign msgWithSign) throws Exception {
		PrePrepareMsg prePrepareMsg = msgWithSign.getPrePrepareMsg();
		logger.debug("onPrePrepare: {}", prePrepareMsg);
		// TODO �Ƿ���ǰ����״̬���˵�һЩ,����У����
		// A backup accepts a pre-prepare message <PRE-PREPARE, v, n, d, block> provided: 
		// 1. the signatures in the request(transactions in block) 
		// 		and the pre-prepare message are correct 
		// 		and d is the digest for block message
		// 2. it is in view v
		// 3. it has not accepted a pre-prepare message for view v and sequence number n containing a different digest����MsgLog��ʵ�֣�
		// 4. (TODO) the sequence number in the pre-prepare message is between a low water mark, h, and a high water mark, H
		
		// 1
		// ��֤���ڵ�ǩ��
		if (!msgWithSign.verifySign(Base58.decode(NodesProperties.get(getPrimary()).getPubKey()))) {
			logger.error("message sign error, type: {}", msgWithSign.getMsgCase());
			return;
		}
		// ��֤��������digest
		if (!Arrays.equals(prePrepareMsg.calculateDigestOfBlock(), prePrepareMsg.getDigestOfBlock())) {
			return;
		}
		// ��֤������ÿ����������Ŀͻ���ǩ��
		BlockMsg blockmsg = prePrepareMsg.getBlockMsg();
		if (!blockmsg.verifyTxSigns()) {
			logger.error("pre-prepare: sign of tx message from client in block error, seqNum: {}", prePrepareMsg.getSeqNum());
			return;
		}
		
		// 2
		if (prePrepareMsg.getView() != this.view.get()) {
			logger.error("view in pre-prepare message does not match the cur view");
			return;
		}
		msgLog.add(msgWithSign);
		enterPrepare(prePrepareMsg.getSeqNum(), prePrepareMsg.getView(), prePrepareMsg.getDigestOfBlock());
	}

	/** ���յ�prepare��Ϣ
	 * @param msgWithSign
	 * @throws Exception
	 */
	private void onPrepare(MsgWithSign msgWithSign) throws Exception{
		PrepareMsg prepareMsg = msgWithSign.getPrepareMsg();
		logger.debug("onPrepare: {}", prepareMsg);
		// A replica (including the primary) accepts prepare messages and adds them to its log provided 
		// 1. their signatures are correct, 
		// 2. their view number equals the replica��s current view, 
		// 3. and their sequence number is between h and H. (TODO) 
		// 1
		if (!msgWithSign.verifySign(Base58.decode(NodesProperties.get(prepareMsg.getReplica()).getPubKey()))) {
			logger.error("message sign error, type: {}", msgWithSign.getMsgCase());
			return;
		}
		// 2
		if (prepareMsg.getView() != view.get()) {
			return;
		}
		// 3 TODO
		msgLog.add(msgWithSign);
	}
	
	/** ���յ�commit��Ϣ
	 * @param msgWithSign
	 * @throws Exception 
	 */
	private void onCommit(MsgWithSign msgWithSign) throws Exception {
		CommitMsg commitMsg = msgWithSign.getCommitMsg();
		logger.debug("onCommit: {}", commitMsg);
		// Replicas accept commit messages and insert them in their log provided 
		// they are properly signed, 
		// the view number in the message is equal to the replica��s current view, 
		// and the sequence number is between h and H
		// 1
		if (!msgWithSign.verifySign(Base58.decode(NodesProperties.get(commitMsg.getReplica()).getPubKey()))) {
			logger.error("message sign error, type: {}", msgWithSign.getMsgCase());
			return;
		}
		// 2
		if (commitMsg.getView() != view.get()) {
			return;
		}
		// 3 TODO
		
		msgLog.add(msgWithSign);
	}
	
	@Override
	public void viewAccepted(View view) {
//		viewChange();
//		System.out.println("isPrimary:" + isPrimary());
	}
	
	private void viewChange() {
		view.incrementAndGet();
		checkPrimary();
	}
	
	private void checkPrimary() {
		boolean isPrimary = nodeId == getPrimary(); 
		if (isPrimary != this.isPrimary) {
			this.isPrimary = isPrimary;
			for (Function<View, Void> listener : roleChangeListeners) {
				listener.apply(channel.getView());
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		new Pbft().start();
//		System.out.println("�����ܲ��ܵ�����");
	}

	@Override
	public void enterPrepare(long seqNum, long view, byte[] digest) throws Exception {
		PrepareMsg prepareMsg = new PrepareMsg();
		prepareMsg.setSeqNum(seqNum);
		prepareMsg.setView(view);
		prepareMsg.setDigestOfBlock(digest);
		prepareMsg.setReplica(nodeId);
		MsgWithSign prepareMsgWithSign = new MsgWithSign();
		prepareMsgWithSign.setPrepareMsg(prepareMsg);
		prepareMsgWithSign.calculateAndSetSign(ecKey);
		logger.debug("multicasting a PREPARE message, {}", prepareMsg);
		channel.send(new Message(null, prepareMsgWithSign.toByteArray()));
		msgLog.add(prepareMsgWithSign);
	}

	@Override
	public void enterCommit(long seqNum, long view, byte[] digest) throws Exception {
		CommitMsg commitMsg = new CommitMsg();
		commitMsg.setSeqNum(seqNum);
		commitMsg.setView(view);
		commitMsg.setDigestOfBlock(digest);
		commitMsg.setReplica(nodeId);
		MsgWithSign commitMsgWithSign = new MsgWithSign();
		commitMsgWithSign.setCommitMsg(commitMsg);
		commitMsgWithSign.calculateAndSetSign(ecKey);
		logger.debug("multicasts a commit message, {}", commitMsg);
		channel.send(new Message(null, commitMsgWithSign.toByteArray()));
		msgLog.add(commitMsgWithSign);
	}

	@Override
	public void commited(long seqNum) {
		logger.debug("commit seqNum: {}", seqNum);
	}
}