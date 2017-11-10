package io.goudai.starter.kafka.consumer;


import io.goudai.starter.kafka.core.NamedThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by freeman on 16/7/21.
 */
public class ThreadKit {
    public static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(new NamedThreadFactory("consumer", true));
}
