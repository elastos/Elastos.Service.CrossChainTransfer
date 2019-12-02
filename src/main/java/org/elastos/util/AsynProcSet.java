package org.elastos.util;

import jnr.ffi.annotations.Synchronized;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Synchronized
public class AsynProcSet<T> {
    private Set<T> procSet = Collections.synchronizedSet(new HashSet<>());
    private Deque<T> inputList = new ConcurrentLinkedDeque<>();
    private Set<List<T>> usingSet = Collections.synchronizedSet(new HashSet<>());

    public AsynProcSet() {
        procSet.clear();
        inputList.clear();
        usingSet.clear();
    }

    public void save2Set(T data) {
        if (procSet.contains(data)) {
            return;
        }
        procSet.add(data);
        inputList.add(data);
    }

    public int size(){
        return procSet.size();
    }

    public boolean isEmpty(){
        return inputList.isEmpty();
    }

    public List<T>usingAllData(){
        List<T> usingList = new ArrayList<>();
        usingList.addAll(inputList);
        inputList.clear();
        if (!usingList.isEmpty()) {
            usingSet.add(usingList);
        }
        return usingList;
    }

    public List<T> usingData(int size) {
        List<T> usingList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            T data = inputList.poll();
            if (null != data) {
                usingList.add(data);
            } else {
                break;
            }
        }

        if (!usingList.isEmpty()) {
            usingSet.add(usingList);
        }

        return usingList;
    }

    public void backData(List<T> usingList) {
        if (usingSet.contains(usingList)) {
            inputList.addAll(usingList);
            usingSet.remove(usingList);
        }
    }

    public void releaseData(List<T> usingList) {
        if (usingSet.contains(usingList)) {
            usingSet.remove(usingList);
            procSet.removeAll(usingList);
        }
    }
}
