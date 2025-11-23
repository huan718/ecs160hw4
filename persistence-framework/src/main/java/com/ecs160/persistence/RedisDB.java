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

@PersistableObject
public class RedisDB {

    private final Jedis jedisSession;

    public RedisDB() {
        jedisSession = new Jedis("localhost", 6379);
    }

    public boolean persist(Object obj) {
        Class<?> clazz = obj.getClass();
        String className = clazz.getSimpleName();

        try {
            // Finds field with Id as Redis key
            Field idField = getIdField(clazz);
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
                            List<?> list = (List<?>) value;
                            String listKey = redisKey + ":" + field.getName();
                            jedisSession.del(listKey); // Clear old list

                            // Handle child objects
                            for (Object child : list) {
                                persist(child); 
                                
                                Field childIdField = getIdField(child.getClass());
                                Object childId = childIdField.get(child);
                                String childRef = child.getClass().getName() + ":" + childId;
                                jedisSession.rpush(listKey, childRef);
                            }
                        } else {
                            // Otherwise store as hash in Redis
                            jedisSession.hset(redisKey, field.getName(), value.toString());
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

    public Object load(Object object) {
        Class<?> clazz = object.getClass();

        // Check for non-persistable object
        if (!clazz.isAnnotationPresent(PersistableObject.class)) {
            throw new RuntimeException("Class is not persistable");
        }

        try {
            // Get Id field and null handling
            Field idField = getIdField(clazz);
            if (idField == null) {
                System.err.println("No @Id field found.");    
                return null;
            }

            Object idVal = idField.get(object);
            if (idVal == null) {
                System.err.println("@Id cannot be null.");
                return null;
            }

            // Create proxy factory and set parent class
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(clazz);
            
            // Implement interface MethodHandler
            MethodHandler handler = new MethodHandler() {
                // Override invoke method
                @Override
                public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                    if (thisMethod.isAnnotationPresent(LazyLoad.class)) {
                        // Get field name target in annotation
                        LazyLoad lazy = thisMethod.getAnnotation(LazyLoad.class);
                        String targetFieldName = lazy.field();

                        // Lazy load null values
                        Field field = clazz.getDeclaredField(targetFieldName);
                        field.setAccessible(true);
                        Object currentValue = field.get(self);

                        // Load empty fields
                        if (currentValue == null) {
                            if (List.class.isAssignableFrom(field.getType())) {
                                loadListField(self, field, clazz.getSimpleName() + ":" + idVal);
                            }
                            
                            return field.get(self);
                        }
                    }
                    
                    // Return original method if no lazy load
                    return proceed.invoke(self, args);
                }
            };

            // Create proxy object and set handler
            Object proxyObj = factory.create(new Class<?>[0], new Object[0], handler);
            idField.set(proxyObj, idVal);

            // Build Redis key 
            String redisKey = clazz.getSimpleName() + ":" + idVal.toString();
            Map<String, String> redisData = jedisSession.hgetAll(redisKey);

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
                    loadListField(proxyObj, field, redisKey);
                } else {
                    String val = redisData.get(field.getName());
                    if (val != null) {
                        field.set(proxyObj, convertToFieldType(field.getType(), val));
                    }
                }
            }

            return proxyObj;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper function to load a list in Redis
    private void loadListField(Object target, Field field, String parentKey) throws Exception {
        // Build key
        String listKey = parentKey + ":" + field.getName();
        List<String> references = jedisSession.lrange(listKey, 0, -1);
        
        List<Object> children = new ArrayList<>();
        
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        Class<?> childClass = (Class<?>) listType.getActualTypeArguments()[0];

        // Create stub object per reference
        for (String ref : references) {
            String[] parts = ref.split(":");
            if (parts.length < 2) continue;
            String childId = parts[parts.length - 1];

            Object childStub = childClass.newInstance();
            Field childIdField = getIdField(childClass);
            childIdField.set(childStub, convertToFieldType(childIdField.getType(), childId));
            
            // Create list of parent's object field
            children.add(load(childStub));
        }
        
        field.set(target, children);
    }

    // Helper to iterate through and get id field of a class
    private Field getIdField(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(Id.class)) {
                return f;
            }
        }
        return null;
    }

    // Helper function to convert various simple class types to string
    private Object convertToFieldType(Class<?> type, String value) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        return value;
    }
}