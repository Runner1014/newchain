package sysu.newchain.consensus.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.server.pbft.PbftHandler;
import sysu.newchain.consensus.server.pbft.msg.BlockMsg;
import sysu.newchain.consensus.server.pbft.msg.ReplyMsg;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;
import sysu.newchain.dao.BlockDao;
import sysu.newchain.ledger.service.LedgerService;

public class BlockProcessManager implements PbftHandler{
	static final Logger logger = LoggerFactory.getLogger(BlockProcessManager.class);
	private static final BlockProcessManager blockProcessManager = new BlockProcessManager();
	public static BlockProcessManager getInstance() {
		return blockProcessManager;
	}
	private BlockProcessManager(){}
	
	private BlockDao blockDao;
	private LedgerService ledgerService;
	private RequestResponer responer;
	
	private ConcurrentHashMap<Long, CompletableFuture<Block>> blockFutures = new ConcurrentHashMap<>();
	
	public void init(){
		logger.info("init BlockProcessManager");
		blockDao = BlockDao.getInstance();
		ledgerService = LedgerService.getInstance();
		responer = RequestResponer.getInstance();
	}
	
	@Override
	public void committed(long seqNum, long view, BlockMsg blockMsg)
			throws Exception {
		logger.debug("commited height: {}", seqNum);
		Block block = blockMsg.toBlock();
		block.getHeader().setHeight(seqNum);
		CompletableFuture<Block> curBlockFuture = new CompletableFuture<Block>();
		blockFutures.put(seqNum, curBlockFuture);
		
		// ��֤���齻�ף������ɲ��У�
		block.verifyTransactions();
		calculateBlockHash(block);
		// ִ�����飨���ɲ��У�
		ledgerService.excuteBlock(block, curBlockFuture);
		
		for (Transaction tx : block.getTransactions()) {
			ReplyMsg replyMsg = new ReplyMsg();
			replyMsg.setView(view);
			replyMsg.setTxHash(tx.getHash());
			replyMsg.setRetCode(tx.getRetCode());
			replyMsg.setRetMsg(tx.getRetMsg());
			replyMsg.setHeight(block.getHeader().getHeight());
			replyMsg.setBlockTime(block.getHeader().getTime());
			logger.debug("pubKey: {}", Base58.encode(tx.getClientPubKey()));
			logger.debug("{}", replyMsg);
			responer.sendTxResp(Base58.encode(tx.getClientPubKey()), replyMsg);
		}
	}
	
	/** ��������hash��������ǰһ����hash
	 * @param block
	 * @throws Exception
	 */
	private void calculateBlockHash(Block block) throws Exception{
		BlockHeader header = block.getHeader();
		long preHeight = header.getHeight() - 1;
		CompletableFuture<Block> preFuture = blockFutures.get(preHeight);
		byte[] preHash = null;
		if (preFuture != null) {
			// �ȴ���һ����ִ�����
			preHash = preFuture.get().getHash();
		}
		else {
			preHash = blockDao.getBlock(preHeight).getHash();
		}
		header.setPrehash(preHash);
		block.calculateAndSetHash();
		
		if (preFuture != null) {
			blockFutures.remove(preHeight);
		}
	}
}
