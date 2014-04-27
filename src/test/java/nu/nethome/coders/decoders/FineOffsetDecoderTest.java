package nu.nethome.coders.decoders;

import nu.nethome.coders.decoders.util.JirFileTestPlayer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class FineOffsetDecoderTest {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void basicJir() {
        JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.FINE_OFFSET_DECODER);

        // Using a known test vector
        player.playFile(this.getClass().getClassLoader()
                .getResourceAsStream("nu/nethome/coders/decoders/fine_offset.jir"));

        assertThat(player.getMessageField(0, "Identity"), is(0x4B1));
        assertThat(player.getMessageField(0, "Temp"), is(225));
    }

    @Test
    public void negativeJir() {
        JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.FINE_OFFSET_DECODER);

        // Using a known test vector
        player.playFile(this.getClass().getClassLoader()
                .getResourceAsStream("nu/nethome/coders/decoders/fine_offset_neg.jir"));

        assertThat(player.getMessageField(0, "Identity"), is(0x46D));
        assertThat(player.getMessageField(0, "Temp"), is(-14));
    }

}
