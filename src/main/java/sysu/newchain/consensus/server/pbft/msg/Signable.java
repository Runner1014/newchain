package sysu.newchain.consensus.server.pbft.msg;

import sysu.newchain.common.crypto.SchnorrKey;

/** ����ǩ��Ϣ�Ľӿ�
 * @author jongliao
 * @date: 2020��2��15�� ����9:59:02
 */
public interface Signable {
	public byte[] calculateSign(SchnorrKey ecKey) throws Exception;
	public void calculateAndSetSign(SchnorrKey ecKey) throws Exception;
	public boolean verifySign(byte[] pubKey);
}
