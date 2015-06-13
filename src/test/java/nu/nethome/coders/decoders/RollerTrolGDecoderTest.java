package nu.nethome.coders.decoders;

import nu.nethome.coders.RollerTrolG;
import nu.nethome.coders.encoders.RisingSunEncoder;
import nu.nethome.coders.encoders.RollerTrolGEncoder;
import nu.nethome.util.ps.BadMessageException;
import nu.nethome.util.ps.Message;
import nu.nethome.util.ps.MessageRepeater;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;


public class RollerTrolGDecoderTest {

    public static final int CHANNEL = 3;
    public static final int ADDRESS = 123456;
    private RollerTrolGEncoder encoder;
    private PulseTestPlayer player;
    private RollerTrolGDecoder decoder;

    @Before
    public void setUp() throws Exception {
        encoder = new RollerTrolGEncoder();
        player = new PulseTestPlayer();
        decoder = new RollerTrolGDecoder();
        decoder.setTarget(player);
        player.setDecoder(decoder);
        player.setPulseWidthModification(0);

    }

    @Test
    public void testNewEncoderInterface() throws BadMessageException {
        Message toSend = RollerTrolGEncoder.buildMessage(RollerTrolG.COMMAND_STOP, ADDRESS, CHANNEL);
        player.playMessage(MessageRepeater.repeat(encoder, toSend, 1));
        assertThat(player.getMessageCount(), is(1));
        assertThat(player.getMessageField(0, RollerTrolG.COMMAND_NAME), is(RollerTrolG.COMMAND_STOP));
        assertThat(player.getMessageField(0, RollerTrolG.ADDRESS_NAME), is(ADDRESS));
        assertThat(player.getMessageField(0, RollerTrolG.CHANNEL_NAME), is(CHANNEL));
        assertThat(player.getMessages()[0].getRepeat(), is(0));
    }
}
