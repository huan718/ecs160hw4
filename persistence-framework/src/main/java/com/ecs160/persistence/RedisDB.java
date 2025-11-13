package com.ecs160.persistence;

import com.ecs160.persistence.annotations.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import redis.clients.jedis.Jedis;


@PersistableObject
public class RedisDB {

    private Jedis jedisSession;

    private RedisDB() {
        jedisSession = new Jedis("localhost", 6379);;
    }

    public boolean persist(Object obj) {
        Class<?> clazz = obj.getClass();
        String className = clazz.getSimpleName();

        try {
            Field idField = null;
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                    break;
                }
            }

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

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(PersistableField.class)) {
                    Object value = field.get(obj);
                    if (value != null) {
                        // may need to change waiting on question for formatting
                        // default to simple formatting for now
                       jedisSession.hset(redisKey, field.getName(), value.toString());
                    }
                }
            }

            return true;

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Object load(Object object)  {
        Class <?> clazz = object.getClass();

        if (!clazz.isAnnotationPresent(PersistableObject.class)) {
            throw new RuntimeException("Class is not persistable");
        }

        try {
            Field idField = null;
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                    break;
                }
            }

            if (idField == null) {
                System.err.println("No @Id field found.");
                return false;
            }

            Object idVal = idField.get(object);
            if (idVal == null) {
                System.err.println("@Id cannot be null.");
                return false; 
            }

            String string_idVal = idVal.toString();
            String redisKey = clazz.getSimpleName() + ":" + string_idVal;

            Map<String, String> redisField = jedisSession.hgetAll(redisKey);
            if (redisField.isEmpty()) {
                return null;
            }

            Object obj = null;
            try {
                obj = clazz.newInstance();
                idField.set(obj, idVal); 
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
                return null;
            }

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(PersistableField.class)) {
                    String value = redisField.get(field.getName());
                    if (value != null) {
                        field.set(obj, convertToFieldType(field.getType(), value));
                    }
                }
            }

            return obj;

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }     
    }

    private Object convertToFieldType(Class<?> type, String value) {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else {
            return value;
        }
    }

}
