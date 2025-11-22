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

    private Jedis jedisSession;

    public RedisDB() {
        jedisSession = new Jedis("localhost", 6379);
    }

    public boolean persist(Object obj) {
        Class<?> clazz = obj.getClass();
        String className = clazz.getSimpleName();

        try {
            // 1. Find and Validate ID
            Field idField = getIdField(clazz);
            if (idField == null) {
                System.err.println("No @Id field found.");
                return false;
            }

            Object idVal = idField.get(obj);
            if (idVal == null) {
                System.err.println("@Id cannot be null.");
                return false;
            }

            String redisKey = className + ":" + idVal.toString();

            // 2. Iterate Fields
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(PersistableField.class)) {
                    Object value = field.get(obj);
                    if (value != null) {
                        if (value instanceof List) {
                            // --- Handle List (Children) ---
                            List<?> list = (List<?>) value;
                            String listKey = redisKey + ":" + field.getName();
                            jedisSession.del(listKey); // Clear old list

                            for (Object child : list) {
                                // Recursive persist
                                persist(child); 
                                
                                // Save Child ID reference
                                Field childIdField = getIdField(child.getClass());
                                Object childId = childIdField.get(child);
                                String childRef = child.getClass().getName() + ":" + childId;
                                jedisSession.rpush(listKey, childRef);
                            }
                        } else {
                            // --- Handle Simple Field ---
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

        if (!clazz.isAnnotationPresent(PersistableObject.class)) {
            throw new RuntimeException("Class is not persistable");
        }

        try {
            // 1. Get ID from stub object
            Field idField = getIdField(clazz);
            if (idField == null) return null;

            Object idVal = idField.get(object);
            if (idVal == null) return null;

            // 2. Create Javassist Proxy
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(clazz);
            
            // Define the Handler (Interceptor)
            MethodHandler handler = new MethodHandler() {
                @Override
                public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                    // Check for @LazyLoad on the method being called
                    if (thisMethod.isAnnotationPresent(LazyLoad.class)) {
                        LazyLoad lazyAnn = thisMethod.getAnnotation(LazyLoad.class);
                        String targetFieldName = lazyAnn.field();

                        Field field = clazz.getDeclaredField(targetFieldName);
                        field.setAccessible(true);
                        Object currentValue = field.get(self);

                        // If null, it means we haven't loaded it yet. Load now!
                        if (currentValue == null) {
                           // System.out.println("Lazy Loading triggered for: " + targetFieldName);
                            if (field.getType() == List.class) {
                                loadListField(self, field, clazz.getSimpleName() + ":" + idVal);
                            }
                            // Return the newly loaded value
                            return field.get(self);
                        }
                    }
                    // Delegate to the original method
                    return proceed.invoke(self, args);
                }
            };

            // Instantiate the proxy
            Object proxyObj = factory.create(new Class<?>[0], new Object[0], handler);

            // 3. Populate ID on Proxy
            idField.set(proxyObj, idVal);

            // 4. Populate Simple Fields (Eagerly) on Proxy
            // We do NOT populate Lazy fields here.
            String redisKey = clazz.getSimpleName() + ":" + idVal.toString();
            Map<String, String> redisData = jedisSession.hgetAll(redisKey);

            if (redisData.isEmpty()) return null; // Object not found

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(PersistableField.class)) {
                    // Skip if this field corresponds to a LazyLoad method
                    if (isFieldLazy(clazz, field.getName())) {
                        continue; 
                    }

                    if (field.getType() == List.class) {
                        // Eager load list if NOT lazy
                        loadListField(proxyObj, field, redisKey);
                    } else {
                        // Simple field
                        String val = redisData.get(field.getName());
                        if (val != null) {
                            field.set(proxyObj, convertToFieldType(field.getType(), val));
                        }
                    }
                }
            }

            return proxyObj;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Helpers ---

    private void loadListField(Object target, Field field, String parentKey) throws Exception {
        String listKey = parentKey + ":" + field.getName();
        List<String> references = jedisSession.lrange(listKey, 0, -1);
        
        List<Object> children = new ArrayList<>();
        
        // Determine generic type (e.g. List<Player>)
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        Class<?> childClass = (Class<?>) listType.getActualTypeArguments()[0];

        for (String ref : references) {
            // ref format: "com.package.Player:10"
            String[] parts = ref.split(":");
            if (parts.length < 2) continue;
            String childId = parts[parts.length - 1];

            // Recursively load child
            Object childStub = childClass.newInstance();
            Field childIdField = getIdField(childClass);
            childIdField.set(childStub, convertToFieldType(childIdField.getType(), childId));
            
            children.add(load(childStub));
        }
        
        field.set(target, children);
    }

    private boolean isFieldLazy(Class<?> clazz, String fieldName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(LazyLoad.class)) {
                if (m.getAnnotation(LazyLoad.class).field().equals(fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Field getIdField(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(Id.class)) {
                return f;
            }
        }
        return null;
    }

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