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

    public boolean save(T obj) {
        return saveSet.add(obj);
    }

    public boolean saveAll(Collection<T> objs) {
        return saveSet.addAll(objs);
    }

    public Set<T> useSet(){
        useSet.clear();
        useSet.addAll(saveSet);
        saveSet.clear();
        return useSet;
    }
}
