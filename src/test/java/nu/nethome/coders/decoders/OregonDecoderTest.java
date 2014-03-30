package nu.nethome.coders.decoders;

import nu.nethome.coders.decoders.util.JirFileTestPlayer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class OregonDecoderTest {


    @Test
    public void basicJir() {
        JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.OREGON_DECODER);

        // This file contains a Nexa message repeated 6 times
        player.playFile(this.getClass().getClassLoader()
                .getResourceAsStream("nu/nethome/coders/decoders/oregon1.jir"));

        // Verify result
    }

}
