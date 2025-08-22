package com.proxy.utils;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Set;
import static java.util.Collections.unmodifiableSet;
public class Headers {

    /**
     * case insensitive set
     */
    public static Set<String> headerSet(String... headers) {
        TreeSet set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(Arrays.asList(headers));
        return unmodifiableSet(set);
    }

}