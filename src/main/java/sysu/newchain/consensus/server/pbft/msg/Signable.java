package sysu.newchain.consensus.server.pbft.msg;

import sysu.newchain.common.crypto.ECKey;

/** ����ǩ��Ϣ�Ľӿ�
 * @author jongliao
 * @date: 2020��2��15�� ����9:59:02
 */
public interface Signable {
	public byte[] calculateSign(ECKey ecKey) throws Exception;
	public void calculateAndSetSign(ECKey ecKey) throws Exception;
	public boolean verifySign(byte[] pubKey);
}
