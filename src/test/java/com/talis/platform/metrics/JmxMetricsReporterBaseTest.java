/*
 *    Copyright 2010 Talis Systems Ltd
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.talis.platform.metrics;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.junit.Ignore;
import org.junit.Test;

public abstract class JmxMetricsReporterBaseTest {
	
	private static final String JMX_CONNECTOR_BINDING_NAME = "jmxrmi";

	protected abstract JmxMetricsReporterBase getReporter() throws Exception;
	
	@Test
	public void beanIsRegisteredWithPlatformMBeanServer() throws Exception {
		JmxMetricsReporterBase reporter = getReporter();
		int instanceCount = JmxMetricsReporterBase.getInstanceCountForClass(reporter.getClass());
		String beanName = reporter.getBeanName();
		ObjectName objectName = new ObjectName(beanName + instanceCount);

		MBeanServer platformServer = 
			ManagementFactory.getPlatformMBeanServer();
		Set<ObjectName> registeredBeans = 
			platformServer.queryNames(objectName, null);

		assertTrue(registeredBeans.size() == 1);
		assertTrue( registeredBeans.contains(objectName) );
	}
	
	@Test (expected=java.rmi.RemoteException.class)
	public void beanDoesNotCreateRmiRegistryByDefault() throws Exception {

		ResettableJmxConnectorJmxMetricsReporter resettableReporter = null;
		
		try {
			resettableReporter = new ResettableJmxConnectorJmxMetricsReporter();
			
			System.setProperty("javax.net.ssl.keyStore", 
					"./etc/security/.majat-jmx-server-keystore");
			System.setProperty("javax.net.ssl.keyStorePassword", "bl1ng441S");
			System.setProperty("javax.net.ssl.trustStore", 
					"./etc/security/.majat-jmx-server-keystore");
			System.setProperty("javax.net.ssl.trustStorePassword", "bl1ng441S");
			SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
			Registry registry = 
				LocateRegistry.getRegistry("localhost",
						JmxMetricsReporterBase.DEFAULT_JMX_CONNECTOR_PORT, 
						csf);
			String[] allBoundObjects = registry.list();
		} finally {
			clearAllSslSystemProperties();
			if (resettableReporter != null) {
				resettableReporter.unbindJmxConnectorAndRmiRegistry();
			}
		}
	}
	
	@Test @Ignore
	public void beanCreatesRmiRegistryWhenHostnamePropertySet() throws Exception {
		
		ResettableJmxConnectorJmxMetricsReporter nodeMetricsJmx = null;
		
		try {
			String host = "localhost";
			int port = JmxMetricsReporterBase.DEFAULT_JMX_CONNECTOR_PORT;
			setAllSslSystemProperties(host, port);
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
			Registry registry = LocateRegistry.getRegistry(host, port, csf);
			String[] allBoundObjects = registry.list();
		} finally {
			clearAllSslSystemProperties();
			if (nodeMetricsJmx != null) {
				nodeMetricsJmx.unbindJmxConnectorAndRmiRegistry();
			}
		}
	}
	
	@Test @Ignore
	public void beanRegistersJmxRmiConnectorWhenHostnamePropertySet() throws Exception {
		
		ResettableJmxConnectorJmxMetricsReporter nodeMetricsJmx = null;
		
		try {
			String host = "localhost";
			int port = JmxMetricsReporterBase.DEFAULT_JMX_CONNECTOR_PORT;
			setAllSslSystemProperties(host, port);
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
			Registry registry = LocateRegistry.getRegistry(host, port, csf);
			String[] allBoundObjects = registry.list();
			List<String> boundObjectsList = Arrays.asList(allBoundObjects);
			
			assertTrue(boundObjectsList.size() > 0);
			assertTrue(boundObjectsList.contains(JMX_CONNECTOR_BINDING_NAME));
		} finally {
			clearAllSslSystemProperties();
			if (nodeMetricsJmx != null) {
				nodeMetricsJmx.unbindJmxConnectorAndRmiRegistry();
			}
		}
	}
	
	@Test @Ignore
	public void defaultJmxRmiConnectorPortUsedWhenPortPropertyNotSet() throws Exception {
		
		ResettableJmxConnectorJmxMetricsReporter nodeMetricsJmx = null;
		
		try {
			String host = "localhost";
			setAllSslSystemProperties(host, -1);
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
			Registry registry = 
				LocateRegistry.getRegistry(host, 
									JmxMetricsReporterBase.DEFAULT_JMX_CONNECTOR_PORT, 
									csf);
			String[] allBoundObjects = registry.list();
			List<String> boundObjectsList = Arrays.asList(allBoundObjects);
			
			assertTrue(boundObjectsList.size() > 0);
			assertTrue(boundObjectsList.contains(JMX_CONNECTOR_BINDING_NAME));
			
			Remote jmxConnector = registry.lookup(JMX_CONNECTOR_BINDING_NAME);
			String jmxConnectorString = jmxConnector.toString();
			assertTrue( jmxConnectorString.indexOf(host) != -1 );
			assertTrue( jmxConnectorString.indexOf(
						"" + JmxMetricsReporterBase.DEFAULT_JMX_CONNECTOR_PORT) != -1 );
		} finally {
			clearAllSslSystemProperties();
			if (nodeMetricsJmx != null) {
				nodeMetricsJmx.unbindJmxConnectorAndRmiRegistry();
			}
		}
	}
	
	@Test @Ignore
	public void overrideDefaultJmxRmiConnectorPort() throws Exception {
		
		ResettableJmxConnectorJmxMetricsReporter nodeMetricsJmx = null;
		
		try {
			String host = "localhost";
			int port = 9876;
			setAllSslSystemProperties(host, port);
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
			Registry registry = 
				LocateRegistry.getRegistry(host, port, csf);
			String[] allBoundObjects = registry.list();
			List<String> boundObjectsList = Arrays.asList(allBoundObjects);
			
			assertTrue(boundObjectsList.size() > 0);
			assertTrue(boundObjectsList.contains(JMX_CONNECTOR_BINDING_NAME));
			
			Remote jmxConnector = registry.lookup(JMX_CONNECTOR_BINDING_NAME);
			String jmxConnectorString = jmxConnector.toString();
			assertTrue( jmxConnectorString.indexOf(host) != -1 );
			assertTrue( jmxConnectorString.indexOf("" + port) != -1 );
		} finally {
			clearAllSslSystemProperties();
			if (nodeMetricsJmx != null) {
				nodeMetricsJmx.unbindJmxConnectorAndRmiRegistry();
			}
		}
	}
	
	@Test @Ignore
	public void defaultPortUsedWhenPortPropertyIsBad() throws Exception {
		
		ResettableJmxConnectorJmxMetricsReporter nodeMetricsJmx = null;
		
		try {
			String host = "localhost";
			int port = -1;
			setAllSslSystemProperties(host, port);
			System.setProperty(JmxMetricsReporterBase.JMX_CONNECTOR_PORT_PROPERTY, "donkey");
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
			Registry registry = 
				LocateRegistry.getRegistry(host, 
						JmxMetricsReporterBase.DEFAULT_JMX_CONNECTOR_PORT, 
						csf);
			String[] allBoundObjects = registry.list();
			List<String> boundObjectsList = Arrays.asList(allBoundObjects);
			
			assertTrue(boundObjectsList.size() > 0);
			assertTrue(boundObjectsList.contains(JMX_CONNECTOR_BINDING_NAME));
			
			Remote jmxConnector = registry.lookup(JMX_CONNECTOR_BINDING_NAME);
			String jmxConnectorString = jmxConnector.toString();
			assertTrue( jmxConnectorString.indexOf(host) != -1 );
			assertTrue( jmxConnectorString.indexOf("" + JmxMetricsReporterBase.DEFAULT_JMX_CONNECTOR_PORT) != -1 );
		} finally {
			clearAllSslSystemProperties();
			if (nodeMetricsJmx != null) {
				nodeMetricsJmx.unbindJmxConnectorAndRmiRegistry();
			}
		}
	}
	
	@Test (expected=java.rmi.RemoteException.class) @Ignore
	public void rmiRegistryRefusesNonSslConnections() throws Exception {

		ResettableJmxConnectorJmxMetricsReporter nodeMetricsJmx = null;
		
		try {
			String host = "localhost";
			int port = 5432;
			setAllSslSystemProperties(host, port);
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			Registry registry = 
				LocateRegistry.getRegistry(host, port);
			String[] allBoundObjects = registry.list();
		} finally {
			clearAllSslSystemProperties();
			if (nodeMetricsJmx != null) {
				nodeMetricsJmx.unbindJmxConnectorAndRmiRegistry();
			}
		}
	}
	
	@Test (expected=java.rmi.RemoteException.class) @Ignore
	public void rmiRegistryNotStartedWhenOneAlreadyRunning() throws Exception {
		ResettableJmxConnectorJmxMetricsReporter nodeMetricsJmx = null;
		
		try {
			String host = "localhost";
			int port = 6543;
			setAllSslSystemProperties(host, port);
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
			Registry registry = 
				LocateRegistry.getRegistry(host, port, csf);
			String[] allBoundObjects = registry.list();
			List<String> boundObjectsList = Arrays.asList(allBoundObjects);
			
			assertTrue(boundObjectsList.size() > 0);
			assertTrue(boundObjectsList.contains(JMX_CONNECTOR_BINDING_NAME));
			
			Remote jmxConnector = registry.lookup(JMX_CONNECTOR_BINDING_NAME);
			String jmxConnectorString = jmxConnector.toString();
			assertTrue( jmxConnectorString.indexOf(host) != -1 );
			assertTrue( jmxConnectorString.indexOf("" + port) != -1 );
			
			host = "localhost";
			port = 3456;
			setAllSslSystemProperties(host, port);
			
			nodeMetricsJmx = new ResettableJmxConnectorJmxMetricsReporter();
			
			Registry newRegistry = 
				LocateRegistry.getRegistry(host, port, csf);
			allBoundObjects = newRegistry.list();
		} finally {
			clearAllSslSystemProperties();
			if (nodeMetricsJmx != null) {
				nodeMetricsJmx.unbindJmxConnectorAndRmiRegistry();
			}
		}
		
	}
	
	private void setAllSslSystemProperties(String host, int port) {
		System.setProperty(
				JmxMetricsReporterBase.JMX_CONNECTOR_HOSTNAME_PROPERTY, host);
		if (port != -1) {
			System.setProperty(
					JmxMetricsReporterBase.JMX_CONNECTOR_PORT_PROPERTY, "" + port);
		}
		System.setProperty("javax.net.ssl.keyStore", 
			"./etc/security/.majat-jmx-server-keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "bl1ng441S");
		System.setProperty("javax.net.ssl.trustStore", 
				"./etc/security/.majat-jmx-server-keystore");
		System.setProperty("javax.net.ssl.trustStorePassword", "bl1ng441S");
	}
	
	private void clearAllSslSystemProperties() {
		System.clearProperty(JmxMetricsReporterBase.JMX_CONNECTOR_HOSTNAME_PROPERTY);
		System.clearProperty(JmxMetricsReporterBase.JMX_CONNECTOR_PORT_PROPERTY);
		System.clearProperty("javax.net.ssl.keyStore");
		System.clearProperty("javax.net.ssl.keyStorePassword");
		System.clearProperty("javax.net.ssl.trustStore");
		System.clearProperty("javax.net.ssl.trustStorePassword");
	}
	
	public interface ResettableJmxConnectorJmxMetricsReporterMBean{ public int getZero();}
	
	class ResettableJmxConnectorJmxMetricsReporter 
	extends JmxMetricsReporterBase implements ResettableJmxConnectorJmxMetricsReporterMBean{
		
		ResettableJmxConnectorJmxMetricsReporter() throws   MalformedObjectNameException, 
																InstanceAlreadyExistsException, 
																MBeanRegistrationException, 
																NotCompliantMBeanException, 
																NullPointerException, 
																IOException {
			super();
		}
		
		public void unbindJmxConnectorAndRmiRegistry() 
				throws NotBoundException, IOException {
			
			if (rmiRegistry != null) {
				rmiRegistry.unbind(JMX_CONNECTOR_BINDING_NAME);
				UnicastRemoteObject.unexportObject(rmiRegistry, true);
				JMX_CONNECTOR_STARTED = false;
			}
		}

		@Override
		public String getBeanName() {
			return "com.talis:name=ResettableTestMetricsReporter";
		}

		@Override 
		public int getZero(){
			return 0;
		}
	}
}
	
