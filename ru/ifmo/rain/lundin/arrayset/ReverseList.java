package ru.ifmo.rain.lundin.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReverseList<T> extends AbstractList<T> {
    private List<T> list;
    private boolean isReversed;

    ReverseList(List<T> list1) {
        list = list1;
        isReversed = false;
    }

    @Override
    public T get(int i) {
        if (!isReversed) {
            return list.get(i);
        } else {
            return list.get(list.size() - i - 1);
        }
    }

    @Override
    public int size() {
        return list.size();
    }

    void setReversed() {
        isReversed = !isReversed;
    }

    boolean getReversed() {
        return isReversed;
    }

    List<T> data(){
        return list;
    }

}
