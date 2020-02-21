package sysu.newchain.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.format.Serialize;
import sysu.newchain.common.format.Utils;
import sysu.newchain.common.format.VarInt;
import sysu.newchain.consensus.server.pbft.msg.Signable;
import sysu.newchain.proto.NewchainProto;

public class Transaction extends Serialize implements Signable{
	static final Logger logger = LoggerFactory.getLogger(Transaction.class);
	
	Address from; 	// ���ͷ���ַ
	Address to;		// ���ܷ���ַ
	long amount;	// ���
	String time;	// ����ʱ�䣬�����ʲ�ͬ���׵������ֶ���ͬʱ�����ֶ�Ӧ��ͬ
	byte[] pubKey;	// ���ͷ���Կ
	byte[] data;	// ��������
	
	byte[] hash; 	// ����hash��Ψһ��ʶһ�ʽ���,�ɸ��������ֶμ���ó�,������洢���л�
	byte[] sign;	// ���ͷ��Խ���hash��ǩ��
	
	// �����ֶ�Ϊ���ս�������Ŀͻ�����Ϣ�����������л�
	byte[] clientPubKey = new byte[0]; 	// �ͻ��˹�Կ�����ڱ�ʶ�ͻ��ˣ�У����ǩ����
	byte[] clientSign = new byte[0]; 	// �ͻ���ǩ��
	
	// ������Ϣ
	int retCode = Respone.OK.getCode();
	String retMsg = Respone.OK.getMsg();

	public enum Respone{
		OK(1000, "ok"),
		TX_HASH_ERROR(1001, "Tx hash not matched"),
		TX_SIGN_ERROR(1002, "Tx sign error"),
		
		LEDGER_ERROR(1003, "Insufficient balance"),
		
		DAO_ERROR(1004, "DB error"),
		
		DUP_TX_ERROR(1005, "Tx duplicated"),
		
		TX_REQUEST_DUP_ERROR(2000, "The tx is waiting for response, don't request duplicately"),
		TX_REQUEST_TIMEOUT_ERROR(2001, "Tx waits for response timeout");
		
		
		Respone(int code, String msg){
			this.code = code;
			this.msg = msg;
		}
		
		private int code;
		private String msg;
		
		public int getCode() {
			return code;
		}
		
		public String getMsg() {
			return msg;
		}
	}
	
	public Transaction() {
		time = "";
		pubKey = new byte[0];
		data = new byte[0];
		hash = new byte[0];
		sign = new byte[0];
	}
	
	public Transaction(Address from, Address to, long amount, String time,
			byte[] sign, byte[] pubKey, byte[] data) {
		this.from = from;
		this.to = to;
		this.amount = amount;
		this.time = time;
		this.sign = sign;
		this.pubKey = pubKey;
		this.data = data;
		calculateAndSetHash();
	}
	
	public Transaction(Address from, Address to, long amount, String time,
			byte[] sign, byte[] pubKey, byte[] data, byte[] clientPubKey) {
		this(from, to, amount, time, sign, pubKey, data);
		this.clientPubKey = clientPubKey;
	}
	
	public Transaction(byte[] payload) throws Exception {
		super(payload);
	}
	
	public Transaction(byte[] payload, int offset) throws Exception {
		super(payload, offset);
	}
	
	public Address getFrom() {
		return from;
	}

	public void setFrom(Address from) {
		this.from = from;
	}

	public Address getTo() {
		return to;
	}

	public void setTo(Address to) {
		this.to = to;
	}

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public byte[] getSign() {
		return sign;
	}

	public void setSign(byte[] sign) {
		this.sign = sign;
	}

	public byte[] getPubKey() {
		return pubKey;
	}

	public void setPubKey(byte[] pubKey) {
		this.pubKey = pubKey;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	public byte[] getClientPubKey() {
		return clientPubKey;
	}

	public void setClientPubKey(byte[] clientPubKey) {
		this.clientPubKey = clientPubKey;
	}
	
	public byte[] getClientSign() {
		return clientSign;
	}

	public void setClientSign(byte[] clientSign) {
		this.clientSign = clientSign;
	}

	public int getRetCode() {
		return retCode;
	}

	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}

	public String getRetMsg() {
		return retMsg;
	}
	
	public void setRetMsg(String retMsg) {
		this.retMsg = retMsg;
	}
	
	public boolean isOk(){
		return retCode == Respone.OK.getCode();
	}
	
	public void setResponse(Respone respone) {
		setRetCode(respone.getCode());
		setRetMsg(respone.getMsg());
	}

	public byte[] calculateHash(){
		byte[] hash = null;
		try {
			hash = Hash.SHA256.hashTwice(this.serialize(SerializeType.TOHASH));
		} catch (Exception e) {
			logger.error("", e);
		}
		return hash;
	}
	
	public byte[] calculateAndSetHash(){
		byte[] hash = this.calculateHash();
		this.setHash(hash);
		return hash;
	}
	
	public long calculateSize() throws Exception {
		return this.serialize().length;
	}

	@Override
	public void serializeToStream(OutputStream stream) throws Exception {
		this.serializeToStream(stream, SerializeType.ALL);
	}
	
	public void serializeToStream(OutputStream stream, SerializeType type) throws Exception {
		// from
		byte[] from = Base58.decode(this.from.getEncodedBase58());
		writeByteArray(from, stream);
		// to
		byte[] to = Base58.decode(this.to.getEncodedBase58());
		writeByteArray(to, stream);
		// amount
		stream.write(new VarInt(this.amount).encode());
		// time
		writeString(time, stream);
		if (SerializeType.ALL.equals(type)) {
			// sign
			writeByteArray(this.sign, stream);
		}
		// pubKey
		writeByteArray(this.pubKey, stream);
		// data
		writeByteArray(data, stream);
	}

	public byte[] serialize(SerializeType type) throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		this.serializeToStream(stream, type);
		return stream.toByteArray();
	}
	
	@Override
	protected void deserialize() throws Exception {
		this.cursor = offset;
		from = new Address(Base58.encode(readByteArray()));
		to = new Address(Base58.encode(readByteArray()));
		amount = readVarInt();
		time = readString();
		sign = readByteArray();
		pubKey = readByteArray();
		data = readByteArray();
		calculateAndSetHash();
		this.length = this.cursor - this.offset;
	}
	
	public enum SerializeType{
		ALL,
		TOHASH
	}

	@Override
	public byte[] calculateSign(ECKey ecKey) throws Exception{
		return ecKey.sign(calculateAndSetHash()).encodeToDER();
	}
	
	@Override
	public void calculateAndSetSign(ECKey ecKey) throws Exception {
		setSign(calculateSign(ecKey));
	}

	@Override
	public boolean verifySign(byte[] pubKey) {
		ECKey ecKey = ECKey.fromPubKeyOnly(pubKey);
		return ecKey.verify(hash, sign);
	}
	
	public boolean verifySign() {
		return verifySign(pubKey);
	}
	
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("hash: ").append(Hex.encode(getHash())).append(",\n")
					.append("from: ").append(from.getEncodedBase58()).append(",\n")
					.append("to: ").append(to.getEncodedBase58()).append(",\n")
					.append("amount: ").append(amount).append(",\n")
					.append("time: ").append(time).append(",\n")
					.append("data: ").append(new String(data)).append(",\n")
					.append("pubKey: ").append(Hex.encode(pubKey)).append(",\n")
					.append("sign: ").append(Hex.encode(sign)).append("\n");
		return stringBuffer.toString();
	}
}