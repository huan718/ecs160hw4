package com.ecs160.persistence;

import com.ecs160.persistence.annotations.LazyLoad;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import java.util.List;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LazyLoaderProxyFactory {
   
   public static Object createProxy(Class<?> clazz, Object idVal, RedisDB redisDB, Jedis jedis) throws Exception {
      ProxyFactory factory = new ProxyFactory();
      factory.setSuperclass(clazz);
      
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
                     ListLoader.loadListField(self, field, jedis, redisDB, clazz.getSimpleName() + ":" + idVal);
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
      
      Field idField = Util.getIdField(clazz);
      idField.set(proxyObj, idVal);

      return proxyObj;  
   }
}