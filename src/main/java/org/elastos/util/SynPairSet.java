package org.elastos.util;

import jnr.ffi.annotations.Synchronized;

import java.util.*;

@Synchronized
public class SynPairSet<T> {

    private Set<T> saveSet = new HashSet<>();
    private Set<T> useSet = new HashSet<>();


    public void init() {
        saveSet.clear();
        useSet.clear();
    }

    public void save2Set(T obj) {
        saveSet.add(obj);
    }

    public void saveAll2Set(Set<T> objs) {
        saveSet.addAll(objs);
    }

    public Set<T> useSet(){
        useSet.clear();
        useSet.addAll(saveSet);
        saveSet.clear();
        return useSet;
    }
}
