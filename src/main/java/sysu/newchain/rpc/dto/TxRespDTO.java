package sysu.newchain.rpc.dto;

/**
 * @Description ���뽻�����󷵻ص�DTO
 * @author jongliao
 * @date 2020��1��20�� ����10:57:06
 */
public class TxRespDTO extends BaseRespDTO{
	String txHash;
	long height;
	String blockTime;
	
	public TxRespDTO() {
		// TODO Auto-generated constructor stub
	}
	
	public TxRespDTO(int code, String msg, String txHash, long height, String blockTime) {
		super(code, msg);
		this.txHash = txHash;
		this.height = height;
		this.blockTime = blockTime;
	}

	public String getTxHash() {
		return txHash;
	}

	public void setTxHash(String txHash) {
		this.txHash = txHash;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public String getBlockTime() {
		return blockTime;
	}

	public void setBlockTime(String blockTime) {
		this.blockTime = blockTime;
	}
	
}
