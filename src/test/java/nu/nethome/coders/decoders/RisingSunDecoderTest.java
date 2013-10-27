/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stestr@nethome.nu>
 *
 * This file is part of OpenNetHome.
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

import nu.nethome.coders.encoders.RisingSunEncoder;
import nu.nethome.util.ps.BadMessageException;
import nu.nethome.util.ps.Message;
import nu.nethome.util.ps.MessageRepeater;
import nu.nethome.util.ps.ProtocolInfo;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RisingSunDecoderTest {
    private RisingSunEncoder m_Encoder;
    private RisingSunDecoder m_Decoder;
    private PulseTestPlayer m_Player;

    @Before
    public void setUp() throws Exception {
        m_Encoder = new RisingSunEncoder();
        m_Player = new PulseTestPlayer();
        m_Decoder = new RisingSunDecoder();
        m_Decoder.setTarget(m_Player);
        m_Player.setDecoder(m_Decoder);
        m_Player.setPulseWidthModification(0);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSetAddress() {
        assertEquals(1, m_Encoder.getChannel());
        m_Encoder.setChannel(4);
        assertEquals(4, m_Encoder.getChannel());
    }

    @Test
    public void testSetAddressNumericUpperLimit() {
        assertEquals(1, m_Encoder.getChannel());
        m_Encoder.setChannel(4);
        assertEquals(4, m_Encoder.getChannel());
    }

    @Test
    public void testSetAddressUpperLimit() {
        assertEquals(1, m_Encoder.getChannel());
        m_Encoder.setChannel(4);
        assertEquals(4, m_Encoder.getChannel());
        m_Encoder.setChannel(5);
        assertEquals(4, m_Encoder.getChannel());
    }

    @Test
    public void testSetAddressLowerLimit() {
        assertEquals(1, m_Encoder.getChannel());
        m_Encoder.setChannel(0);
        assertEquals(1, m_Encoder.getChannel());
    }

    @Test
    public void testSetButton() {
        assertEquals(1, m_Encoder.getButton());
        m_Encoder.setButton(2);
        assertEquals(2, m_Encoder.getButton());
    }

    @Test
    public void testSetButtonUpperLimit() {
        m_Encoder.setButton(4);
        assertEquals(4, m_Encoder.getButton());
        m_Encoder.setButton(17);
        assertEquals(4, m_Encoder.getButton());
    }

    @Test
    public void testSetButtonLowerLimit() {
        m_Encoder.setButton(1);
        assertEquals(1, m_Encoder.getButton());
        m_Encoder.setButton(0);
        assertEquals(1, m_Encoder.getButton());
    }

	
    @Test
    public void testSetCommand() {
        assertEquals(1, m_Encoder.getCommand());
        m_Encoder.setCommand(0);
        assertEquals(0, m_Encoder.getCommand());
        m_Encoder.setCommand(2);
        assertEquals(0, m_Encoder.getCommand());
    }

    @Test
    public void testSetRepeatCount() {
        assertEquals(5, m_Encoder.getRepeatCount());
        m_Encoder.setRepeatCount(6);
        assertEquals(6, m_Encoder.getRepeatCount());
        m_Encoder.setRepeatCount(0);
        assertEquals(6, m_Encoder.getRepeatCount());
    }

    @Test
    public void testSingleMessage() {
        m_Encoder.setRepeatCount(1);
        m_Encoder.setChannel(1);
        m_Encoder.setButton(2);
        m_Encoder.setCommand(1);
        m_Player.playMessage(m_Encoder.encode());
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(1, m_Player.getMessageField(0, "Channel"));
        assertEquals(2, m_Player.getMessageField(0, "Button"));
        assertEquals(1, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
    }
	
    @Test
    public void testSingleHighMessage() {
        m_Encoder.setRepeatCount(1);
        m_Encoder.setChannel(4);
        m_Encoder.setButton(4);
        m_Encoder.setCommand(1);
        m_Player.playMessage(m_Encoder.encode());
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(4, m_Player.getMessageField(0, "Channel"));
        assertEquals(4, m_Player.getMessageField(0, "Button"));
        assertEquals(1, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
    }

    @Test
    public void testNewEncoderInterface() throws BadMessageException {
        Message toSend = RisingSunEncoder.buildMessage(1, 4, 3);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 1));
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(3, m_Player.getMessageField(0, "Channel"));
        assertEquals(4, m_Player.getMessageField(0, "Button"));
        assertEquals(1, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
    }

    @Test
    public void testTwoMessages() {
        m_Encoder.setRepeatCount(2);
        m_Encoder.setChannel(2);
        m_Encoder.setButton(1);
        m_Encoder.setCommand(0);
        m_Player.playMessage(m_Encoder.encode());
        assertEquals(2, m_Player.getMessageCount());
        assertEquals(2, m_Player.getMessageField(0, "Channel"));
        assertEquals(1, m_Player.getMessageField(0, "Button"));
        assertEquals(0, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
        assertEquals(2, m_Player.getMessageField(1, "Channel"));
        assertEquals(1, m_Player.getMessageField(1, "Button"));
        assertEquals(0, m_Player.getMessageField(1, "Command"));
        assertEquals(1, m_Player.getMessages()[1].getRepeat());
    }
    @Test
    public void testTwoMessagesNewInterface() throws BadMessageException {
        Message toSend = RisingSunEncoder.buildMessage(0, 2, 3);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 2));
        assertEquals(2, m_Player.getMessageCount());
        assertEquals(3, m_Player.getMessageField(0, "Channel"));
        assertEquals(2, m_Player.getMessageField(0, "Button"));
        assertEquals(0, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
        assertEquals(3, m_Player.getMessageField(1, "Channel"));
        assertEquals(2, m_Player.getMessageField(1, "Button"));
        assertEquals(0, m_Player.getMessageField(1, "Command"));
        assertEquals(1, m_Player.getMessages()[1].getRepeat());
    }

    @Test
    public void testThreeMessages() {
        m_Encoder.setRepeatCount(3);
        m_Encoder.setChannel(1);
        m_Encoder.setButton(2);
        m_Encoder.setCommand(0);
        m_Player.playMessage(m_Encoder.encode());
        assertEquals(3, m_Player.getMessageCount());
        assertEquals(1, m_Player.getMessageField(0, "Channel"));
        assertEquals(2, m_Player.getMessageField(0, "Button"));
        assertEquals(0, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
        assertEquals(1, m_Player.getMessageField(1, "Channel"));
        assertEquals(2, m_Player.getMessageField(1, "Button"));
        assertEquals(0, m_Player.getMessageField(1, "Command"));
        assertEquals(1, m_Player.getMessages()[1].getRepeat());
        assertEquals(1, m_Player.getMessageField(2, "Channel"));
        assertEquals(2, m_Player.getMessageField(2, "Button"));
        assertEquals(0, m_Player.getMessageField(2, "Command"));
        assertEquals(2, m_Player.getMessages()[2].getRepeat());
    }

    @Test
    public void info() {
        ProtocolInfo info = m_Encoder.getInfo();
        assertThat(info.getName(), is("RisingSun"));
    }
}
