package sysu.newchain.consensus.server.pbft.msg.log;

import sysu.newchain.consensus.server.pbft.msg.BlockMsg;

/** pbft�㷨 �׶�ת�� ʱ��Ҫ���Ĳ������׶�ת������Ϣ��־�д���
 * pre-prepared -- enter prepare phase -->
 * prepared -- enter commit phase -->
 * committed
 * @author jongliao
 * @date: 2020��2��21�� ����9:52:16
 */
public interface PhaseShiftHandler {
	
	public enum Status{
		PRE_PREPARED,
		PREPARED,
		COMMITED;
		public static Status fromBytes(byte[] data){
			return Status.valueOf(new String(data));
		}
		public byte[] toByteArray(){
			return this.toString().getBytes();
		}
	}
	
	public void enterPrepare(long seqNum, long view, byte[] digest) throws Exception;
	
	public void enterCommit(long seqNum, long view, byte[] digest) throws Exception;
	
	public void commited(long seqNum, long view, BlockMsg blockMsg) throws Exception;
	
}