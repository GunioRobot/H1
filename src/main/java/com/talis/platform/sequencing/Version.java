package com.talis.platform.sequencing;

import java.io.IOException;

import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.talis.jersey.filters.ServerInfo;

public class Version implements ServerInfo {

	private static final String UNSET = "unset";

	static private Metadata metadata = new Metadata() ;
	public static String version = metadata.get("h1.version", UNSET);
	public static String buildTime = metadata.get("h1.build.datetime", UNSET);
	public static String buildNumber = metadata.get("h1.build.number", UNSET);
	public static String repository = metadata.get("h1.repository.version", UNSET);
	public static String component = metadata.get("h1.component", UNSET);
	public static String identifier = component + "/" + version + "-" + buildNumber + "." + repository;

	@Override
	public String getServerIdentifier() {
		return identifier;
	}

}

class Metadata {

	private static final String PROPERTIES_FILENAME = "h1.version.properties";
	private final Properties properties = new Properties();
	private static final Logger LOG = LoggerFactory.getLogger( Metadata.class );

	public Metadata() {
		ClassLoader classLoader = Metadata.class.getClassLoader();
		InputStream in = classLoader.getResourceAsStream(PROPERTIES_FILENAME) ;
		if ( in != null ) {
			try { 
				properties.load(in) ; 
			} catch (InvalidPropertiesFormatException e) {
				LOG.error(PROPERTIES_FILENAME + " is malformed.", e);
			} catch (IOException e) {
				LOG.error("Unable to find or read " + PROPERTIES_FILENAME, e);
			}
		}
	}

	public String get(String name) {
		return get(name, null);
	}

	public String get(String name, String defaultValue) {
		if (properties == null) {
			return defaultValue;
		}
		return properties.getProperty(name, defaultValue);
	}

}
