package com.endlessadventure.data;

import java.util.Map;

public class DataValidation {

	/** Gson leaves a missing field null; this turns that into a clear error 
	 * @throws MissingFieldException */
	public static <T> T require(T value, String field) throws MissingFieldException {
		if (value == null) {
			throw new MissingFieldException(field);
		}
		return value;
	}

	/** a map read from the file must exist and hold no null keys or values (an unknown enum name reads as null) 
	 * @throws MissingFieldException */
	public static <K, V> Map<K, V> requireEntries(Map<K, V> map, String field) throws MissingFieldException {
		require(map, field);
		for (Map.Entry<K, V> e : map.entrySet()) {
			if (e.getKey() == null || e.getValue() == null) {
				throw new MissingFieldException(field + " entry");
			}
		}
		return map;
	}
}
