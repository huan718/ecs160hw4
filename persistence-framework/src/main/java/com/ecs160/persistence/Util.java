package com.ecs160.persistence;

import com.ecs160.persistence.annotations.Id;
import com.ecs160.persistence.annotations.PersistableField;

import java.lang.reflect.Field;

public class Util {

   // Helper to iterate through and get id field of a class
   public static Field getIdField(Class<?> clazz) {
      for (Field f : clazz.getDeclaredFields()) {
         f.setAccessible(true);
            if (f.isAnnotationPresent(Id.class)) {
               return f;
            }
        }
      return null;
   }

   public static Object convertToFieldType(Class<?> type, String value) {
      if (type == String.class) return value;
      if (type == int.class || type == Integer.class) return Integer.parseInt(value);
      if (type == long.class || type == Long.class) return Long.parseLong(value);
      if (type == double.class || type == Double.class) return Double.parseDouble(value);
      if (type == float.class || type == Float.class) return Float.parseFloat(value);
      if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
      return value;
   }
}