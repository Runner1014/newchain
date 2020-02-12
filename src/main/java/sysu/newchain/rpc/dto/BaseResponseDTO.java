package sysu.newchain.rpc.dto;

/**
 * @Description rpc��Ӧ�����ݴ������Ļ���
 * @author jongliao
 * @date 2020��1��20�� ����10:55:58
 */
public class BaseResponseDTO {
	int code;
	String msg;
	
	public BaseResponseDTO(int code, String msg) {
		super();
		this.code = code;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	public String getMsg() {
		return msg;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
}
