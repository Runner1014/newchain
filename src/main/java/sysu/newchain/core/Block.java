package sysu.newchain.core;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.noneDSA;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Serialize;
import sysu.newchain.common.format.VarInt;

public class Block extends Serialize{
	BlockHeader header;
	List<Transaction> transactions;
	
	public Block() {
		header = new BlockHeader();
		transactions = new ArrayList<Transaction>();
	}
	
	public Block(byte[] payload) throws Exception {
		super(payload);
	}
	
	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}
	
	public BlockHeader getHeader() {
		return header;
	}

	public void setHeader(BlockHeader header) {
		this.header = header;
	}
	
	public void addTransaction(Transaction tx) {
		if (transactions == null) {
			transactions = new ArrayList<Transaction>();
		}
		transactions.add(tx);
	}
	
	@Override
	public void serializeToStream(OutputStream stream) throws Exception {
		this.header.serializeToStream(stream);
		if (transactions != null) {
			stream.write(new VarInt(transactions.size()).encode());
			for (Transaction tx : transactions) {
				tx.serializeToStream(stream);
			}
		}
		else {
        	stream.write(new VarInt(0).encode());
        }
	}

	@Override
	protected void deserialize() throws Exception {
		this.cursor = this.offset;
		this.header = new BlockHeader(payload, this.cursor);
		this.cursor += this.header.getLength();
		int transNum = (int) readVarInt();
		transactions = new ArrayList<Transaction>(transNum);
		for (int i = 0; i < transNum; i++) {
			Transaction tx = new Transaction(payload, this.cursor);
			transactions.add(tx);
			this.cursor += tx.getLength();
		}
		this.length = this.cursor - this.offset;
	};
	
	public byte[] getHash() {
		return header.getHash();
	}
	
	public byte[] calculateHash() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.writeBytes(new VarInt(header.getHeight()).encode());
		stream.writeBytes(header.getTime().getBytes());
		stream.writeBytes(header.getPrehash());
		for(Transaction tx : transactions) {
			stream.writeBytes(tx.getHash());
		}
		byte[] bytesToHash = stream.toByteArray();
		return Hash.SHA256.hashTwice(bytesToHash);
	}
	
	public byte[] calculateAndSetHash() {
		byte[] hash = calculateHash();
		this.header.setHash(hash);
		return hash;
	}
	
}