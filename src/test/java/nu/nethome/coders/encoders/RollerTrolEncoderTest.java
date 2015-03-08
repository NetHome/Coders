package nu.nethome.coders.encoders;

import nu.nethome.coders.RollerTrol;
import nu.nethome.coders.decoders.RollerTrolDecoder;
import nu.nethome.util.ps.ProtocolEncoder;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.Before;
import org.junit.Test;

import static nu.nethome.coders.RollerTrol.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class RollerTrolEncoderTest {

    private RollerTrolEncoder rollerTrolEncoder;
    private PulseTestPlayer player;
    private RollerTrolDecoder rollerTrolDecoder;

    @Before
    public void setUp() throws Exception {
        rollerTrolEncoder = new RollerTrolEncoder();
        player = new PulseTestPlayer();
        rollerTrolDecoder = new RollerTrolDecoder();
        rollerTrolDecoder.setTarget(player);
        player.setDecoder(rollerTrolDecoder);
        player.setPulseWidthModification(0);

    }

    @Test
    public void encodesPreamble() throws Exception {
        int[] data = rollerTrolEncoder.encode(RollerTrolEncoder.buildMessage(1, 1, 1), ProtocolEncoder.Phase.FIRST);
        assertThat(data.length,  is(greaterThanOrEqualTo(4)));
        assertThat(data[0],  is(LONG_PREAMBLE_MARK.length()));
        assertThat(data[1],  is(LONG_PREAMBLE_SPACE.length()));
        assertThat(data[2],  is(SHORT_PREAMBLE_MARK.length()));
        assertThat(data[3],  is(SHORT.length()));
    }

    @Test
    public void encodes40BitsPlusPreamble() throws Exception {
        int[] data = rollerTrolEncoder.encode(RollerTrolEncoder.buildMessage(1, 1, 1), ProtocolEncoder.Phase.FIRST);
        assertThat(data.length,  is(4 + 40 * 2));
    }

    @Test
    public void canEncodeHouseCode() throws Exception {
        player.playMessage(rollerTrolEncoder.encode(RollerTrolEncoder.buildMessage(1, 12345, 1), ProtocolEncoder.Phase.FIRST));
        assertThat(player.getMessageCount(), is(1));
        assertThat(player.getMessageField(0, RollerTrol.HOUSE_CODE_NAME), is(12345));
    }

    @Test
    public void canEncodeDeviceCode() throws Exception {
        player.playMessage(rollerTrolEncoder.encode(RollerTrolEncoder.buildMessage(1, 12345, 9), ProtocolEncoder.Phase.FIRST));
        assertThat(player.getMessageCount(), is(1));
        assertThat(player.getMessageField(0, RollerTrol.DEVICE_CODE_NAME), is(9));
    }

    @Test
    public void canEncodeCommand() throws Exception {
        player.playMessage(rollerTrolEncoder.encode(RollerTrolEncoder.buildMessage(5, 12345, 9), ProtocolEncoder.Phase.FIRST));
        assertThat(player.getMessageCount(), is(1));
        assertThat(player.getMessageField(0, RollerTrol.COMMAND_NAME), is(5));
    }
}
