package com.ecs160.persistence;

import redis.clients.jedis.Jedis;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public class ListLoader {

   public static void persistList(String parentKey, Field field, Object value, Jedis jedis, RedisDB redisDB) {
      try {
         List<?> list = (List<?>) value;
         String listKey = parentKey + ":" + field.getName();
         jedis.del(listKey); // Clear old list

         // Handle child objects
         for (Object child : list) {
            redisDB.persist(child); 
            
            Field childIdField = Util.getIdField(child.getClass());
            Object childId = childIdField.get(child);
            String childRef = child.getClass().getName() + ":" + childId;
            jedis.rpush(listKey, childRef);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // Helper function to load a list in Redis
   public static void loadListField(Object target, Field field, Jedis jedis, RedisDB redisDB, String parentKey) {
      try {
         String listKey = parentKey + ":" + field.getName();
         List<String> references = jedis.lrange(listKey, 0, -1);
         
         List<Object> children = new ArrayList<>();
         
         ParameterizedType listType = (ParameterizedType) field.getGenericType();
         Class<?> childClass = (Class<?>) listType.getActualTypeArguments()[0];

         // Create stub object per reference
         for (String ref : references) {
            String[] parts = ref.split(":");
            if (parts.length < 2) continue;
            String childId = parts[parts.length - 1];

            Object childStub = childClass.getDeclaredConstructor().newInstance();
            Field childIdField = Util.getIdField(childClass);
            childIdField.set(childStub, Util.convertToFieldType(childIdField.getType(), childId));
            
            // Create list of parent's object field
            children.add(redisDB.load(childStub));
         }
         
         field.set(target, children);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}