package com.ecs160;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

import java.lang.reflect.Method;
import java.util.Objects;

public final class Endpointdef {
    private final Object instance;
    private final Method method;
    private final String url;
    public Endpointdef(Object instance, Method method, String url){
        this.instance = Objects.requireNonNull(instance);
        this.method = Objects.requireNonNull(method);
        this.url = Objects.requireNonNull(url);
    }
    public Object getInstance(){return instance;}
    public Method getMethod(){return method;}
    public String getUrl(){return url;}
}