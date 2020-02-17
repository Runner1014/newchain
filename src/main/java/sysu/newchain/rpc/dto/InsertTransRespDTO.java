package sysu.newchain.rpc.dto;

/**
 * @Description ���뽻�����󷵻ص�DTO
 * @author jongliao
 * @date 2020��1��20�� ����10:57:06
 */
public class InsertTransRespDTO extends BaseResponseDTO{
	String txHash;
	String height;
	String blockTime;
	
	public InsertTransRespDTO() {
		// TODO Auto-generated constructor stub
	}
	
	public InsertTransRespDTO(int code, String msg, String txHash, String height, String blockTime) {
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

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getBlockTime() {
		return blockTime;
	}

	public void setBlockTime(String blockTime) {
		this.blockTime = blockTime;
	}
	
}
