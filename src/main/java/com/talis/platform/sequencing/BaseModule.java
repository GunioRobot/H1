package com.talis.platform.sequencing;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.talis.jersey.auth.Authenticator;
import com.talis.jersey.auth.NullAuthenticator;
import com.talis.jersey.filters.ServerInfo;
import com.talis.platform.SystemTimestampProvider;
import com.talis.platform.TimestampProvider;

public class BaseModule extends AbstractModule{
	@Override
	protected void configure() {
		bind(TimestampProvider.class) .to(SystemTimestampProvider.class) .in(Scopes.SINGLETON);
		bind(Authenticator.class).to(NullAuthenticator.class);
		bind(ServerInfo.class).to(Version.class).in(Scopes.SINGLETON);
	}
}
