package org.gradle.sample;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Generated generated = new Generated();
        System.out.println("build type: " + new BuildTypeImpl().getBuildType());
        Iterable<Person> people = new People();
        for (Person person : people) {
            System.out.println("person: " + person.getName());
        }
    }
}
