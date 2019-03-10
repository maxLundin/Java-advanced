package ru.ifmo.rain.lundin.arrayset;

import java.util.*;

public class Test {
    public static void main(String[] args) {
        ArrayList al = new ArrayList<Integer>(Arrays.asList(10, 20, 30, 40));
        TreeSet<Integer> set = new TreeSet<>();
        set.add(1);
        set.add(2);
        ArrayList<Integer> arrayList = new ArrayList<>((Set)set);
    }
}