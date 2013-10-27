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

import nu.nethome.coders.encoders.DeltronicEncoder;
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

/**
 * Unit tests for DeltronicEncoder and DeltronicDecoder.
 * @author �garen
 *
 */
public class TestDeltronicDecoderEncoder {

	private DeltronicEncoder m_Encoder;
	private DeltronicDecoder m_Decoder;
	private PulseTestPlayer m_Player;

	@Before
	public void setUp() throws Exception {
		m_Encoder = new DeltronicEncoder();
		m_Player = new PulseTestPlayer();
		m_Decoder = new DeltronicDecoder();
        m_Decoder.setTarget(m_Player);
		m_Player.setDecoder(m_Decoder);
		m_Player.setPulseWidthModification(0);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSetAddress() {
		assertEquals(0, m_Encoder.getAddress());
		m_Encoder.setAddress(0x3F);
		assertEquals(0x3F, m_Encoder.getAddress());
		m_Encoder.setAddress(0x40);
		assertEquals(0x3F, m_Encoder.getAddress());
	}

	@Test
	public void testSetButton() {
		assertEquals(0, m_Encoder.getButton());
		m_Encoder.setButton(2);
		assertEquals(2, m_Encoder.getButton());
		m_Encoder.setButton(-1);
		assertEquals(2, m_Encoder.getButton());
		m_Encoder.setButton(4);
		assertEquals(2, m_Encoder.getButton());
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
		assertEquals(3, m_Encoder.getRepeatCount());
		m_Encoder.setRepeatCount(5);
		assertEquals(5, m_Encoder.getRepeatCount());
		m_Encoder.setRepeatCount(0);
		assertEquals(5, m_Encoder.getRepeatCount());
	}

	@Test
	public void testSingleMessage() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setAddress(1);
		m_Encoder.setButton(2);
		m_Encoder.setCommand(1);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertEquals(1, m_Player.getMessageField(0, "Address"));
		assertEquals(2, m_Player.getMessageField(0, "Button"));
		assertEquals(1, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
	}

    @Test
    public void testSingleMessageNewInterface() throws BadMessageException {
        Message toSend = DeltronicEncoder.buildCommandMessage(true, 2, 1);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 1));
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(1, m_Player.getMessageField(0, "Address"));
        assertEquals(2, m_Player.getMessageField(0, "Button"));
        assertEquals(1, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
    }

	@Test
	public void testTwoMessages() {
		m_Encoder.setRepeatCount(2);
		m_Encoder.setAddress(0x1A);
		m_Encoder.setButton(3);
		m_Encoder.setCommand(0);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(2, m_Player.getMessageCount());
		assertEquals(0x1A, m_Player.getMessageField(0, "Address"));
		assertEquals(3, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(0x1A, m_Player.getMessageField(1, "Address"));
		assertEquals(3, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}

	@Test
	public void testTwoMessagesNewInterface() throws BadMessageException {
        Message toSend = DeltronicEncoder.buildCommandMessage(false, 3, 0x1a);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 2));
		assertEquals(2, m_Player.getMessageCount());
		assertEquals(0x1A, m_Player.getMessageField(0, "Address"));
		assertEquals(3, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(0x1A, m_Player.getMessageField(1, "Address"));
		assertEquals(3, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}

    @Test
    public void info() {
        ProtocolInfo info = m_Encoder.getInfo();
        assertThat(info.getName(), is("Deltronic"));
    }
}
