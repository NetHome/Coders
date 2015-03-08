package nu.nethome.coders.decoders;

import nu.nethome.coders.RollerTrol;
import nu.nethome.coders.decoders.util.JirFileTestPlayer;
import org.junit.Test;

import static nu.nethome.coders.RollerTrol.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RollerTrolDecoderTest {
    public void setUp() throws Exception {

    }

    @Test
    public void basicJir() {
        JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.ROLLERTROL_DECODER);

        // Using a known test vector
        player.playFile(this.getClass().getClassLoader()
                .getResourceAsStream("nu/nethome/coders/decoders/rollertrol_3_stop.jir"));

        assertThat(player.getMessageField(0, DEVICE_CODE_NAME), is(3));
        assertThat(player.getMessageField(0, COMMAND_NAME), is(STOP));
        assertThat(player.getMessageField(0, HOUSE_CODE_NAME), is(36600));
        assertThat(player.m_Messages.size(), is(15));
        assertThat(player.m_Messages.get(14).getRepeat(), is(14));
    }

}
