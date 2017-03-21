package stack;

import org.jdiameter.api.*;
import org.jdiameter.server.impl.StackImpl;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class DiameterTimeoutClientTest {

    @Test(enabled = false)
    public void client() throws Exception {
        StackImpl client = new StackImpl();
        InputStream clientConfigStream = StackConnectMultiBaseTest.class.getClassLoader().getResourceAsStream(
                "configurations/sctp-client.xml");
        Configuration clientConfig = new org.jdiameter.server.impl.helpers.XMLConfiguration(clientConfigStream);
        client.init(clientConfig);
        Network network = client.unwrap(Network.class);
        network.addNetworkReqListener(request -> null, ApplicationId.createByAccAppId(193, 19302));
        client.start(Mode.ALL_PEERS, 10000, TimeUnit.MILLISECONDS);
        while(true) Thread.sleep(100);
    }

}
