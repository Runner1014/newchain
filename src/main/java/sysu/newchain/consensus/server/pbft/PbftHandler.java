package sysu.newchain.consensus.server.pbft;

import sysu.newchain.consensus.server.pbft.msg.BlockMsg;

/** pbft��ʶ��ɺ�Ĵ���
 * @author jongliao
 * @date: 2020��2��21�� ����9:57:23
 */
public interface PbftHandler {
	public void committed(long seqNum, long view, BlockMsg blockMsg) throws Exception;
}
