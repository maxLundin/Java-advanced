package ru.ifmo.rain.lundin.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private List<T> elements;
    private Comparator<? super T> cmp;

    public ArraySet() {
        elements = new ArrayList<>();
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    private ArraySet(Comparator<? super T> my_cmp) {
        this(new ArrayList<>(), my_cmp);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> my_cmp) {
        NavigableSet<T> set = new TreeSet<>(my_cmp);
        cmp = my_cmp;
        set.addAll(collection);
        elements = new ArrayList<>(set);
    }

    private ArraySet(List<T> collection, Comparator<? super T> my_cmp) {
        elements = collection;
        cmp = my_cmp;
    }

    private int findRealIndex(T t) {
        return Collections.binarySearch(elements, t, cmp);
    }

//    private <T extends Comparable<T>> int findRealIndexCmp(T t) {
//        return Collections.binarySearch(elements, t, new Comparator<T>() {
//            @Override
//            public int compare(T t, T t1) {
//                return t.compareTo(t1);
//            }
//        });
//    }
//
    private int findIndex(T t, boolean firstORsecond, boolean include) {
        int ind;
//        if (cmp == null) {
//            ind = findRealIndexCmp(t);
//        } else {
            ind = findRealIndex(t);
//        }
        if (ind >= 0) {
            return ind;
        } else {
            if (firstORsecond) {
                if (include) {
                    return -ind - 1;
                } else {
                    return -ind - 2;
                }
            } else {
                if (include) {
                    return -ind - 2;
                } else {
                    return -ind - 1;
                }
            }
        }
    }

    private T findElem(T t, int a, int b) {
        int pos = findRealIndex(t);
        if (pos < 0) {
            pos = -pos - 1 + a;
            if (checkSize(pos)) {
                return elements.get(pos);
            } else {
                return null;
            }
        } else {
            if (checkSize(pos + b)) {
                return elements.get(pos + b);
            } else {
                return null;
            }
        }
    }

    @Override
    public T lower(T t) {
        return findElem(t, -1, -1);
    }

    @Override
    public T floor(T t) {
        return findElem(t, -1, 0);
    }

    @Override
    public T ceiling(T t) {
        return findElem(t, 0, 0);
    }

    @Override
    public T higher(T t) {
        return findElem(t, 0, 1);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast");
    }


    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return (findRealIndex((T) Objects.requireNonNull(o)) != -1);
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    @Override
    public NavigableSet<T> descendingSet() {
        ReverseList<T> list;
        if (elements instanceof ReverseList) {
            list = new ReverseList<>(((ReverseList<T>) elements).data());

            if (!((ReverseList) elements).getReversed())
                list.setReversed();
        } else {
            list = new ReverseList<>(elements);
            list.setReversed();
        }
        return new ArraySet<T>(list, Collections.reverseOrder(cmp));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T t, boolean b, T t1, boolean b1) {
        int top, down;

        top = findIndex(t, true, b);
        down = findIndex(t1, false, b1);

        if (!b) {
            ++top;
        }
        if (!b1) {
            --down;
        }

        if (top <= down)
            return new ArraySet<>(elements.subList(top, down + 1), cmp);
        else {
            return new ArraySet<>(cmp);
        }
    }

    @Override
    public NavigableSet<T> headSet(T t, boolean b) {
        try {
            return subSet(first(), true, t, b);
        } catch (NoSuchElementException e) {
            return new ArraySet<>(cmp);
        }
    }

    @Override
    public NavigableSet<T> tailSet(T t, boolean b) {
        try {
            return subSet(t, b, last(), true);
        } catch (NoSuchElementException e) {
            return new ArraySet<>(cmp);
        }
    }

    @Override
    public Comparator<? super T> comparator() {
        return cmp;
    }

    @Override
    public SortedSet<T> subSet(T t, T e1) {
        if (cmp.compare(t, e1) > 0) {
            throw new IllegalArgumentException();
        }
        return subSet(t, true, e1, false);
    }

    @Override
    public SortedSet<T> headSet(T t) {
        if (checkSize(0)) {
            return subSet(first(), true, t, false);
        } else {
            return new ArraySet<>(cmp);
        }
    }

    @Override
    public SortedSet<T> tailSet(T t) {
        if (checkSize(0)) {
            return subSet(t, true, last(), true);
        } else {
            return new ArraySet<>(cmp);
        }
    }

    private boolean checkSize(int pos) {
        return !(pos < 0 || pos >= elements.size());
    }

    private void checkSizeThrow(int pos) {
        if (!checkSize(pos)) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T first() {
        checkSizeThrow(0);
        return elements.get(0);
    }

    @Override
    public T last() {
        checkSizeThrow(elements.size() - 1);
        return elements.get(elements.size() - 1);
    }
}
