package io.goudai.starter.kafka.core;

import java.util.Arrays;

public class StringUtils {
    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
    //bin/kafka-topics.sh  --zookeeper node1-zookeeper:2181,node2-zookeeper:2181,node3-zookeeper:2181 --partitions 50 --replication-factor 3 --topic test2 --if-not-exists --create test2
}
