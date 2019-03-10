package ru.ifmo.rain.lundin.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {
    private final Comparator<Student> naturalOrder = Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName).thenComparing(Student::getId).thenComparing(Student::getGroup);
    private final Comparator<Group> naturalOrderGroups = Comparator.comparing(Group::getName);

    private static String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private Stream<Map.Entry<String, List<Student>>> getGroupsStream(Collection<Student> collection) {
        return collection.stream()
                .collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList()))
                .entrySet().stream();
    }

    private List<Group> getGroupsBy(Collection<Student> collection, Function<List<Student>, List<Student>> order) {
        return getGroupsStream(collection)
                .map(elem -> new Group(elem.getKey(), order.apply(elem.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getGroupsBy(collection, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getGroupsBy(collection, this::sortStudentsById);
    }


    private String getLargestGroupBy(Collection<Student> collection, Function<List<Student>, Integer> arraySizeGetter) {
        return getGroupsStream(collection)
                .max(Comparator.comparingInt(
                        (Map.Entry<String, List<Student>> group) -> arraySizeGetter.apply(group.getValue()))
                        .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> collection) {
        return getLargestGroupBy(collection, List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> collection) {
        return getLargestGroupBy(collection, v -> getDistinctFirstNames(v).size());
    }


    private Stream<String> getStream(Collection<Student> studentList, Function<Student, String> foo) {
        return studentList.stream().map(foo);
    }

    private List<String> streamToListOfStrings(Stream<String> stringStream) {
        return stringStream.collect(Collectors.toList());
    }

    private List<String> functionToList(Collection<Student> list, Function<Student, String> foo) {
        return streamToListOfStrings(getStream(list, foo));
    }

    @Override
    public List<String> getFirstNames(List<Student> list) {
        return functionToList(list, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return functionToList(list, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> list) {
        return functionToList(list, Student::getGroup);
    }


    @Override
    public List<String> getFullNames(List<Student> list) {
        return functionToList(list, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return getStream(list, Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> list) {
        return list.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private List<Student> sortStudentByComparator(Collection<Student> collection, Comparator<Student> cmp) {
        return collection.stream().sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return sortStudentByComparator(collection, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return sortStudentByComparator(collection, naturalOrder);
    }

    private List<Student> findStudentsByPredicate(Collection<Student> collection, Predicate<Student> predicate) {
        return collection.stream().filter(predicate).sorted(naturalOrder).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return findStudentsByPredicate(collection, (Student student) -> student.getFirstName().equals(s));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return findStudentsByPredicate(collection, (Student student) -> student.getLastName().equals(s));
    }

    private List<Student> findStudentsById(Collection<Student> collection, int s) {
        return findStudentsByPredicate(collection, (Student student) -> student.getId() == s);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, String s) {
        return findStudentsByPredicate(collection, (Student student) -> student.getGroup().equals(s));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, String s) {
        return collection.stream()
                .filter((Student student) -> student.getGroup().equals(s))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}
