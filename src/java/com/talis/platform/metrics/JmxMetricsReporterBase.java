package com.talis.platform.metrics;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class JmxMetricsReporterBase {

	public static final String JMX_CONNECTOR_HOSTNAME_PROPERTY = 
		"java.rmi.server.hostname";
	public static final String JMX_CONNECTOR_PORT_PROPERTY = 
		"com.talis.platform.reporting.metrics.jmx.connectorPort";
	public static final int DEFAULT_JMX_CONNECTOR_PORT = 4444;

    private static final Log LOG = LogFactory.getLog(JmxMetricsReporterBase.class);
    
    protected static Registry rmiRegistry;
    protected static JMXConnectorServer jmxConnectorServer;
    protected static volatile boolean JMX_CONNECTOR_STARTED = false;
    
    private static int instanceCount = 0;
    public static int getInstanceCount() {
        return instanceCount;
    }
    
    public abstract String getBeanName();
 
    public JmxMetricsReporterBase() throws MalformedObjectNameException, 
    							           InstanceAlreadyExistsException, 
    							           MBeanRegistrationException, 
    							           NotCompliantMBeanException, 
    							           NullPointerException, 
    							           IOException {
    	
        ObjectName objectName = new ObjectName(getBeanName() + instanceCount++);

        MBeanServer platformServer = 
            ManagementFactory.getPlatformMBeanServer();
        platformServer.registerMBean(this, objectName);
        LOG.info("Registered metrics MBean with JVM platform MBean server");
        
        String hostAddress = 
			System.getProperty(JMX_CONNECTOR_HOSTNAME_PROPERTY);
        
		if (hostAddress != null && JMX_CONNECTOR_STARTED == false) {
			synchronized (JmxMetricsReporterBase.class) {
				if (JMX_CONNECTOR_STARTED == false) {
					String portString = 
						System.getProperty(JMX_CONNECTOR_PORT_PROPERTY, 
											"" + DEFAULT_JMX_CONNECTOR_PORT);
					int port = DEFAULT_JMX_CONNECTOR_PORT; 
					try {
						port = Integer.parseInt(portString);
					} catch (NumberFormatException nfe) {
						LOG.error("Invalid port '" + portString + "' supplied for '" 
									+ JMX_CONNECTOR_PORT_PROPERTY + "' - using default of "
									+ DEFAULT_JMX_CONNECTOR_PORT + " instead");
					}
					
					System.setProperty("java.rmi.server.randomIDs", "true");
					HashMap<String,Object> env = new HashMap<String,Object>();
					
					SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
					SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
					env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
					env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);
					env.put("com.sun.jndi.rmi.factory.socket", csf);
					rmiRegistry = LocateRegistry.createRegistry(port, csf, ssf);
					JMXServiceURL url = new JMXServiceURL(
							"service:jmx:rmi://" + hostAddress + 
					        ":" + port + "/jndi/rmi://"+ hostAddress + ":" 
					        + port + "/jmxrmi");
					jmxConnectorServer =
						JMXConnectorServerFactory.newJMXConnectorServer(url, env, platformServer);
					jmxConnectorServer.start();
					JMX_CONNECTOR_STARTED = true;
					LOG.info("Started metrics JMX-RMI connector at " + url);
				}
			}
		} 
    }
    	
}
