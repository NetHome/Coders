package nu.nethome.coders.decoders;

import nu.nethome.coders.decoders.util.JirFileTestPlayer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class PrologueDecoderTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void basicJir() {
        JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.PROLOGUE_DECODER);

        // Using a known test vector
        player.playFile(this.getClass().getClassLoader()
                .getResourceAsStream("nu/nethome/coders/decoders/prologue.jir"));

        assertThat(player.m_Messages.size(), is(5));
        assertThat(player.m_Messages.get(4).getRepeat(), is(4));
        assertThat(player.getMessageField(0, "Temp"), is(261));
        assertThat(player.getMessageField(0, "Button"), is(1));
        assertThat(player.getMessageField(0, "Battery"), is(0));
        assertThat(player.getMessageField(0, "RollingId"), is(80));
        assertThat(player.getMessageField(0, "Channel"), is(0));
        assertThat(player.getMessageField(0, "Humidity"), is(204));
        assertThat(player.getMessageField(0, "Id"), is(9));
    }
}