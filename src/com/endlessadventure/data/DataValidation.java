package com.endlessadventure.data;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;

public class DataValidation {
	
	/** checks that a record read by Gson has no missing value anywhere inside, nested records, lists and maps included;
	 *  the error names the field by its path, such as "playerData.inventory[2]", taken from the record's own field names */
	public static void checkNotNull(Object value, String path) throws MissingFieldException {
		if (value == null) throw new MissingFieldException(path);
		switch (value) {
			case Record r -> {
				for (RecordComponent c : r.getClass().getRecordComponents()) {
					checkNotNull(read(c, r), path.isEmpty() ? c.getName() : path + "." + c.getName());
				}
			}
			case List<?> list -> {
				for (int i = 0; i < list.size(); i++) checkNotNull(list.get(i), path + "[" + i + "]");
			}
			case Map<?, ?> map -> {
				for (Map.Entry<?, ?> e : map.entrySet()) {
					if (e.getKey() == null) throw new MissingFieldException(path + " key"); //an unknown enum name reads as null
					checkNotNull(e.getValue(), path + "." + e.getKey());
				}
			}
			default -> {}
		}
	}

	private static Object read(RecordComponent c, Record r) {
		try {
			Method accessor = c.getAccessor();
			accessor.setAccessible(true); //the save records are package-private; allowed inside our own module
			return accessor.invoke(r);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot read " + c.getName(), e);
		}
	}
}
