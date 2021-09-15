package com.coctrl.milo.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kangaroo
 * @date 2020/9/20 12:47
 */
public class SubscriptValues {
    private static Map<String, Object> subscriptValues = new HashMap<>();

    public static Map<String, Object> getSubscriptValues() {
        return subscriptValues;
    }

    public static void setSubscriptValues(Map<String, Object> subscriptValues) {
        SubscriptValues.subscriptValues = subscriptValues;
    }
}
