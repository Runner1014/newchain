package sysu.newchain.core;

import sysu.newchain.common.core.Address;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.format.Utils;

public class TransactionTest {
	public static void main(String[] args) throws Exception {
		SchnorrKey ecKey = SchnorrKey.fromPrivate(Base58.decode("FcbyAoZztZMPuaGWMfTy4Hduhz5aFHooSfqD4QyKtqUq"));
		Transaction transaction = new Transaction(new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), 20, "", null, ecKey.getPubKeyAsBytes(), "hahaha".getBytes());
		byte[] sign = ecKey.sign(transaction.getHash()).toByteArray();
		transaction.setSign(sign);
		byte[] txBytes = transaction.serialize();
		
		Transaction transaction2 = new Transaction(txBytes);
		
		System.out.println(Hex.encode(transaction2.getHash()).equals(Hex.encode(transaction.getHash())));
		
		System.out.println(ecKey.verify(transaction.getHash(), sign));
		
		System.out.println(Hex.encode(transaction.getHash()));
		System.out.println(Hex.encode(transaction.getSign()));
	}
}
