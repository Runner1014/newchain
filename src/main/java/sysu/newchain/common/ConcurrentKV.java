package sysu.newchain.common;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/** ConcurrentHashMap �� Rocksdb ��ϣ�ʵ�ֲ���key/value�洢��map��Ϊһ������
 * @author jongliao
 * @date: 2020��2��14�� ����7:30:58
 */
public class ConcurrentKV extends DataBase implements ConcurrentMap<String, String>{
	private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentKV.class);
	
	private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();

	public ConcurrentKV(String dbName) {
		super(dbName);
		init();
//		load();
	}
	
	private void load() {
		RocksIterator iterator = rocksDB.newIterator();
		for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
			byte[] key = iterator.key();
			byte[] value = iterator.value();
			map.put(new String(key, Charset.forName("ISO-8859-1")), new String(value, Charset.forName("ISO-8859-1")));
		}
	}
	
	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public String get(Object key) {
		return map.get(key);
	}

	@Override
	public String put(String key, String value) {
		String oldValue = map.put(key, value);
		putDB(key, value);
		return oldValue;
	}

	@Override
	public String remove(Object key) {
		String value = map.remove(key);
		deleteDB((String) key);
		return value;
	}

	@Deprecated
	@Override
	public void putAll(Map<? extends String, ? extends String> m) {
		LOGGER.error("not implement");
	}
	
	@Deprecated
	@Override
	public void clear() {
		LOGGER.error("not implement");
	}

	@Deprecated
	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<String> values() {
		return map.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return map.entrySet();
	}

	@Override
	public String putIfAbsent(String key, String value) {
		String oldValue = map.putIfAbsent(key, value);
		// oldValueΪnull˵��ԭ��Ϊ�գ���Ҫ����
		if (oldValue == null) {
			putDB(key, value);
		}
		return oldValue;
	}

	@Override
	public boolean remove(Object key, Object value) {
		boolean isRemoved = map.remove(key, value);
		if (isRemoved) {
			deleteDB((String) key);
		}
		return isRemoved;
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		boolean isReplaced = map.replace(key, oldValue, newValue);
		if(isReplaced){
			putDB(key, newValue);
		}
		return isReplaced;
	}

	@Override
	public String replace(String key, String value) {
		String oldValue = map.replace(key, value);
		// ���ھ�ֵ������
		if (oldValue != null) {
			putDB(key, value);
		}
		return oldValue;
	}
	
	@Override
	public String compute(
			String key,
			BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
		String value = map.compute(key, remappingFunction);
		// 1. remappingFunction���ؽ��Ϊnull��ɾ����ֵ
		if (value == null) {
			deleteDB(key);
		}
		// 2. ����Ϊ������ֵ
		else {
			putDB(key, value);
		}
		return value;
	}
	
	@Override
	public String computeIfPresent(
			String key,
			BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
		String value = map.computeIfPresent(key, remappingFunction);
		// 1. ������ �� remappingFunction���ؽ��Ϊnull��ɾ����ֵ
		if (value == null) {
			deleteDB(key);
		}
		// 2. ����Ϊ������ֵ
		else {
			putDB(key, value);
		}
		return value;
	}
	
	@Override
	public String computeIfAbsent(String key,
			Function<? super String, ? extends String> mappingFunction) {
		String value = map.computeIfAbsent(key, mappingFunction);
		// 1. remappingFunction���ؽ��Ϊnull��ɾ����ֵ
		if (value == null) {
			deleteDB(key);
		}
		// 2. ԭ�ȴ��� �� remappingFunction���ؽ����Ϊnull������Ϊ������ֵ
		else {
			putDB(key, value);
		}
		return value;
	}
	
	private void putDB(String key, String value){
		try {
			rocksDB.put(key.getBytes(Charset.forName("ISO-8859-1")), value.getBytes(Charset.forName("ISO-8859-1")));
		} catch (RocksDBException e) {
			LOGGER.error("", e);
		}
	}
	
	private void deleteDB(String key){
		try {
			rocksDB.delete(key.getBytes());
		} catch (RocksDBException e) {
			LOGGER.error("", e);
		}
	}
}
