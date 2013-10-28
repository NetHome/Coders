/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
 *
 * This file is part of OpenNetHome (http://www.nethome.nu).
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nu.nethome.coders.decoders;

import nu.nethome.coders.encoders.ZhejiangEncoder;
import nu.nethome.util.ps.BadMessageException;
import nu.nethome.util.ps.FieldValue;
import nu.nethome.util.ps.Message;
import nu.nethome.util.ps.ProtocolEncoder;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * User: Stefan
 * Date: 2013-03-24
 * Time: 22:26
 */
public class ZhejiangDecoderTest {
    private ZhejiangEncoder encoder;
    private ZhejiangDecoder decoder;
    private PulseTestPlayer player;

    @Before
    public void setUp() throws Exception {
        encoder = new ZhejiangEncoder();
        player = new PulseTestPlayer();
        decoder = new ZhejiangDecoder();
        decoder.setTarget(player);
        player.setDecoder(decoder);
        player.setPulseWidthModification(0);
    }

    @Test
    public void canCreateCommand() {
        Message message = ZhejiangEncoder.buildMessage(1, 2, 3);
        assertThat(message.getFields().size(), is(3));
        assertThat(message.getFields(), hasItem(new FieldValue("Command", 1)));
        assertThat(message.getFields(), hasItem(new FieldValue("Button", 2)));
        assertThat(message.getFields(), hasItem(new FieldValue("Address", 3)));
    }

    @Test
    public void canDecodeEncodedMessage() throws BadMessageException {
        player.playMessage(encoder.encode(ZhejiangEncoder.buildMessage(1, 2, 3), ProtocolEncoder.Phase.REPEATED));
        assertThat(player.getMessageCount(), is(1));
        assertThat(player.getMessageField(0, "Command"), is(1));
        assertThat(player.getMessageField(0, "Button"), is(2));
        assertThat(player.getMessageField(0, "Address"), is(3));
        assertThat(player.getMessages()[0].getRepeat(), is(0));
    }

    @Test
    public void CompareWithKnownOnTestVector() throws BadMessageException {
        player.playMessage(encoder.encode(ZhejiangEncoder.buildMessage(1, 2, 0), ProtocolEncoder.Phase.REPEATED));
        assertThat(player.getMessageCount(), is(1));
        assertThat(player.getMessages()[0].getRawMessage()[0], is(0));
        assertThat(player.getMessages()[0].getRawMessage()[1], is(0x2A));
        assertThat(player.getMessages()[0].getRawMessage()[2], is(0x2B));
        assertThat(player.getMessages()[0].getRawMessage()[3], is(0xFF));
    }

    @Test
    public void CompareWithKnownOffTestVector() throws BadMessageException {
        player.playMessage(encoder.encode(ZhejiangEncoder.buildMessage(0, 2, 0), ProtocolEncoder.Phase.REPEATED));
        assertThat(player.getMessageCount(), is(1));
        assertThat(player.getMessages()[0].getRawMessage()[0], is(0));
        assertThat(player.getMessages()[0].getRawMessage()[1], is(0x8A));
        assertThat(player.getMessages()[0].getRawMessage()[2], is(0x2B));
        assertThat(player.getMessages()[0].getRawMessage()[3], is(0xFF));
    }

}
