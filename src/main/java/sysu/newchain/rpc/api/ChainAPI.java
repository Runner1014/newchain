package sysu.newchain.rpc.api;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import sysu.newchain.rpc.dto.InsertTransRespDTO;

@JsonRpcService("/transaction")
public interface ChainAPI {
	/**
	 * @param from 		��Դ��ַ
	 * @param to		Ŀ���ַ
	 * @param amount	���
	 * @param time		ʱ���
	 * @param sign		ǩ����base58����
	 * @param pubKey	��Կ��base58����
	 * @param data		��������
	 * @return
	 * @throws Exception
	 */
	InsertTransRespDTO insertTransaction(
			@JsonRpcParam(value = "from") String from, 
			@JsonRpcParam(value = "to") String to, 
			@JsonRpcParam(value = "amount") long amount, 
			@JsonRpcParam(value = "time") String time,
			@JsonRpcParam(value = "sign") String sign, 
			@JsonRpcParam(value = "pubKey") String pubKey,
			@JsonRpcParam(value = "data") String data) throws Exception;
}
