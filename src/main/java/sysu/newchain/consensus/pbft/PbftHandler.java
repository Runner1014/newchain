package sysu.newchain.consensus.pbft;

import sysu.newchain.consensus.pbft.msg.BlockMsg;

public interface PbftHandler {
	public void commited(long seqNum, long view, BlockMsg blockMsg) throws Exception;
}