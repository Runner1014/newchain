package sysu.newchain.common.format;

/**
 * @Description ʮ�����Ʊ�����룺�ֽ�����<-<����--����>->ʮ�������ַ���
 * @author jongliao
 * @date 2020��1��20�� ����10:26:41
 */
public class Hex {
	
	public static String encode(byte[] input) {
		return new String(org.bouncycastle.util.encoders.Hex.encode(input));
	}
	
	public static byte[] decode(String input) {
		return org.bouncycastle.util.encoders.Hex.decode(input);
	}
}