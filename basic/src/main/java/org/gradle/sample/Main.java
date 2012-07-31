package org.gradle.sample;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Generated generated = new Generated();
        Iterable<Person> people = Arrays.asList(new Person("fred"));
        for (Person person : people) {
            System.out.println("person: " + person.getName());
        }
    }
}
