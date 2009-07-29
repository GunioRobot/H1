package com.talis.platform.sequencing.zookeeper;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkTestHelper {
	
	static final Logger LOG = 
		LoggerFactory.getLogger(ZkTestHelper.class);
	
	public static int CONNECTION_TIMEOUT = 30000;
	public static String DEFAULT_HOST_PORT = "127.0.0.1:33221";
	protected NIOServerCnxn.Factory serverFactory = null;
	public File tmpDir;
	public int maxCnxns = 0;
	
	public ZkTestHelper() throws IOException{
		tmpDir = File.createTempFile("zk_", "_test");
		tmpDir.delete();
		tmpDir.mkdirs();
	}	
	
	public void cleanUp() throws Exception{
		stopServer();
		FileUtils.deleteDirectory(tmpDir);
	}
	
    public NIOServerCnxn.Factory createNewServerInstance(File dataDir,
            NIOServerCnxn.Factory factory, String hostPort, int maxCnxns)
        throws IOException, InterruptedException{ 
    	
    	try{
    	
        ZooKeeperServer zks = new ZooKeeperServer(dataDir, dataDir, 3000);
        
        final int PORT = getPort(hostPort);
        if (factory == null) {
            factory = new NIOServerCnxn.Factory(PORT);
        }
        factory.startup(zks);

        assertTrue("waiting for server up",
                   waitForServerUp("127.0.0.1:" + PORT,
                                              CONNECTION_TIMEOUT));
    	
        return factory;
    	}catch(IOException e){
    		e.printStackTrace();
    		throw e;
    	}
    }
    
    public boolean waitForServerUp(String hp, long timeout) {
        long start = System.currentTimeMillis();
        String split[] = hp.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        while (true) {
            try {
                Socket sock = new Socket(host, port);
                BufferedReader reader = null;
                try {
                    OutputStream outstream = sock.getOutputStream();
                    outstream.write("stat".getBytes());
                    outstream.flush();

                    reader =
                        new BufferedReader(
                                new InputStreamReader(sock.getInputStream()));
                    String line = reader.readLine();
                    if (line != null && line.startsWith("Zookeeper version:")) {
                        return true;
                    }
                } finally {
                    sock.close();
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (IOException e) {
                // ignore as this is expected
                LOG.info("server " + hp + " not up " + e);
            }

            if (System.currentTimeMillis() > start + timeout) {
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return false;
    }
    
    public boolean waitForServerDown(String hp, long timeout) {
        long start = System.currentTimeMillis();
        String split[] = hp.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        while (true) {
            try {
                Socket sock = new Socket(host, port);
                try {
                   OutputStream outstream = sock.getOutputStream();
                    outstream.write("stat".getBytes());
                    outstream.flush();
                } finally {
                    sock.close();
                }
            } catch (IOException e) {
                return true;
            }

            if (System.currentTimeMillis() > start + timeout) {
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return false;
    }

    private int getPort(String hostPort) {
        String portstr = hostPort.split(":")[1];
        String[] pc = portstr.split("/");
        if (pc.length > 1) {
            portstr = pc[0];
        }
        return Integer.parseInt(portstr);
    }
    
    public void shutdownServerInstance(NIOServerCnxn.Factory factory,
            String hostPort){
        if (factory != null) {
            factory.shutdown();
            final int PORT = getPort(hostPort);

            assertTrue("waiting for server down",
                       waitForServerDown("127.0.0.1:" + PORT,
                                          CONNECTION_TIMEOUT));
        }
    }
    
    public void startServer() throws Exception {
        LOG.info("STARTING server");
        serverFactory = createNewServerInstance(tmpDir, serverFactory, DEFAULT_HOST_PORT, maxCnxns);
        // ensure that only server and data bean are registered
//        JMXEnv.ensureOnly("InMemoryDataTree", "StandaloneServer_port");
    }

    public void stopServer() throws Exception {
        LOG.info("STOPPING server");
        shutdownServerInstance(serverFactory, DEFAULT_HOST_PORT);
        serverFactory = null;
        // ensure no beans are leftover
//        JMXEnv.ensureOnly();
    }


	
}
