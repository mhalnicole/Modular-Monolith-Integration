package edu.cit.patonog;

import edu.cit.patonog.channel.MarketplaceGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelModuleTests {

    @Test
    @DisplayName("Package encapsulation: Only MarketplaceGateway is public in channel module")
    void testChannelEncapsulation() throws ClassNotFoundException {
        assertTrue(Modifier.isPublic(MarketplaceGateway.class.getModifiers()));

        String[] internalClasses = {
                "edu.cit.patonog.channel.TianggeClient",
                "edu.cit.patonog.channel.ChannelFeedPoller",
                "edu.cit.patonog.channel.ChannelStartupRunner",
                "edu.cit.patonog.channel.ChannelOrderProcessor",
                "edu.cit.patonog.channel.ChannelBackorderResolver",
                "edu.cit.patonog.channel.ChannelStockSyncListener",
                "edu.cit.patonog.channel.MarketplaceGatewayImpl",
                "edu.cit.patonog.channel.ChannelCursor",
                "edu.cit.patonog.channel.ChannelOrder",
                "edu.cit.patonog.channel.ChannelProcessedEvent"
        };

        for (String className : internalClasses) {
            Class<?> clazz = Class.forName(className);
            assertFalse(Modifier.isPublic(clazz.getModifiers()), className + " should not be public");
        }
    }
}
