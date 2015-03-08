package nu.nethome.coders.encoders;

import nu.nethome.coders.RollerTrol;
import nu.nethome.util.ps.ProtocolEncoder;
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

    @Before
    public void setUp() throws Exception {
        rollerTrolEncoder = new RollerTrolEncoder();
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

}
