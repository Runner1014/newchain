package sysu.newchain.common.proto;

import com.google.protobuf.MessageOrBuilder;

/**
 * @Description TODO
 * @author jongliao
 * @date 2020��2��1�� ����9:07:24
 */
public interface ProtoCloner<O, P extends MessageOrBuilder> {
	
	public O toObject(P p);
	
	public P toProto(O o);
	
}
