package stack;

import org.jdiameter.api.*;
import org.jdiameter.server.impl.StackImpl;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class DiameterTimeoutServerTest {

    @Test(enabled = false)
    public void server() throws Exception {
        StackImpl server = new StackImpl();
        InputStream serverConfigInputStream = StackConnectMultiBaseTest.class.getClassLoader().getResourceAsStream(
                "configurations/sctp-server.xml");
        Configuration serverConfig = new org.jdiameter.server.impl.helpers.XMLConfiguration(serverConfigInputStream);
        server.init(serverConfig);
        Network network = server.unwrap(Network.class);
        network.addNetworkReqListener(request -> null, ApplicationId.createByAccAppId(193, 19302));
        server.start();
        Thread.sleep(1000);
        while(true) {
            Thread.sleep(5000);
//            server.stop(1000, TimeUnit.MILLISECONDS, DisconnectCause.BUSY);
            Thread.sleep(5000);
//            server.start();
            Thread.sleep(5000);
        }
    }

}
