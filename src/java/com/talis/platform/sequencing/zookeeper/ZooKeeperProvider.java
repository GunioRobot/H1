package com.talis.platform.sequencing.zookeeper;

import org.apache.zookeeper.ZooKeeper;

import com.google.inject.throwingproviders.ThrowingProvider;
import com.talis.platform.sequencing.SequencingException;

public interface ZooKeeperProvider
extends ThrowingProvider<ZooKeeper, SequencingException>{

	public static final String DEFAULT_SERVER_LIST_LOCATION = "/zkservers";
	public static final String SERVER_LIST_LOCATION_PROPERTY = 
		"com.talis.plaftform.sequencing.zookeeper.servers";

	public static final int DEFAULT_SESSION_TIMEOUT = 10 * 1000;
	public static final String SESSION_TIMEOUT_PROPERTY = 
		"com.talis.plaftform.sequencing.zookeeper.session.timeout";

	public static final int DEFAULT_CONNECTION_TIMEOUT = 10 * 1000;
	public static final String CONNECTION_TIMEOUT_PROPERTY = 
		"com.talis.platform.sequencing.zookeeper.connection.timeout";
}
