package com.ast_generator;

public class Settings {
    public static int MAX_METHOD_CALL_DEPTH; 
    public static boolean IGNORE_TEST;

    public static void setMaxMethodCallDepth(int maxMethodCallDepth) {
        MAX_METHOD_CALL_DEPTH = maxMethodCallDepth;
    }

    public static void setIgnoreTest(boolean ignoreTest) {
        IGNORE_TEST = ignoreTest;
    }
}
