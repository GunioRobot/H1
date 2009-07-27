package com.talis.platform.sequencing.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class NullWatcher implements Watcher {
    public void process(WatchedEvent event) { /* nada */ }
}
