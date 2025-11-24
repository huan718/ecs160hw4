package com.ecs160.persistence;

import com.ecs160.persistence.annotations.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import redis.clients.jedis.Jedis;

public class RedisDB {
    private Jedis jedis;

    public RedisDB() {
        this.jedis = new Jedis("localhost", 6379);
    }

    public boolean persist(Object obj) {
        Class<?> clazz = obj.getClass();
        String className = clazz.getSimpleName();

        try {
            // Finds field with Id as Redis key
            Field idField = Util.getIdField(clazz);
            if (idField == null) {
                System.err.println("No @Id field found.");
                return false;
            }

            // Null handling
            Object idVal = idField.get(obj);
            if (idVal == null) {
                System.err.println("@Id cannot be null.");
                return false;
            }

            String redisKey = className + ":" + idVal.toString();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(PersistableField.class)) {
                    Object value = field.get(obj);
                    if (value != null) {
                        // If list recursively persist element
                        if (value instanceof List) {
                            ListLoader.persistList(redisKey, field, value, jedis, this);
                        } else {
                            // Otherwise store as hash in Redis
                            jedis.hset(redisKey, field.getName(), value.toString());
                        }
                    }
                }
            }
            return true;

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }    

    public Object load(Object obj) { 
        Class<?> clazz = obj.getClass();

        // Check for non-persistable object
        if (!clazz.isAnnotationPresent(PersistableObject.class)) {
            throw new RuntimeException("Class is not persistable");
        }

        try {
            // Get Id field and null handling
            Field idField = Util.getIdField(clazz);
            if (idField == null) {
                System.err.println("No @Id field found.");    
                return null;
            }

            Object idVal = idField.get(obj);
            if (idVal == null) {
                System.err.println("@Id cannot be null.");
                return null;
            }

            Object proxyObj = LazyLoaderProxyFactory.createProxy(clazz, idVal, this, jedis);

            // Build Redis key 
            String redisKey = clazz.getSimpleName() + ":" + idVal.toString();
            Map<String, String> redisData = jedis.hgetAll(redisKey);

            if (redisData.isEmpty()) return null; 

            // Iterate through fields and load non-lazy
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);

                // Skip non-persistent fields
                if (!field.isAnnotationPresent(PersistableField.class)) continue;
                // Skip lazy-loaded fields
                if (field.isAnnotationPresent(LazyLoad.class)) continue;
                // List handling
                if (List.class.isAssignableFrom(field.getType())) {
                    ListLoader.loadListField(proxyObj, field, jedis, this, redisKey);
                } else {
                    String val = redisData.get(field.getName());
                    if (val != null) {
                        field.set(proxyObj, Util.convertToFieldType(field.getType(), val));
                    }
                }
            }

            return proxyObj;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
    }
}