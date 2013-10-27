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

import nu.nethome.coders.decoders.util.JirFileTestPlayer;
import nu.nethome.coders.encoders.NexaEncoder;
import nu.nethome.util.ps.BadMessageException;
import nu.nethome.util.ps.Message;
import nu.nethome.util.ps.MessageRepeater;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for NexaEncoder and NexaDecoder.
 * @author �garen
 *
 */
public class TestNexaDecoder {

	private NexaEncoder m_Encoder;
	private NexaDecoder m_Decoder;
	private PulseTestPlayer m_Player;

	@Before
	public void setUp() throws Exception {
		m_Encoder = new NexaEncoder();
		m_Player = new PulseTestPlayer();
		m_Decoder = new NexaDecoder();
        m_Decoder.setTarget(m_Player);
		m_Player.setDecoder(m_Decoder);
		m_Player.setPulseWidthModification(0);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSetAddress() {
		assertEquals('A', m_Encoder.getAddress());
		m_Encoder.setAddress('F');
		assertEquals('F', m_Encoder.getAddress());
	}

	@Test
	public void testSetAddressNumeric() {
		assertEquals('A', m_Encoder.getAddress());
		m_Encoder.setAddress(5);
		assertEquals('F', m_Encoder.getAddress());
	}

	@Test
	public void testSetAddressNumericUpperLimit() {
		assertEquals('A', m_Encoder.getAddress());
		m_Encoder.setAddress(15);
		assertEquals('P', m_Encoder.getAddress());
	}

	@Test
	public void testSetAddressNumericLowerLimit() {
		assertEquals('A', m_Encoder.getAddress());
		m_Encoder.setAddress('B');
		m_Encoder.setAddress(0);
		assertEquals('A', m_Encoder.getAddress());
	}

	@Test
	public void testSetAddressUpperLimit() {
		assertEquals('A', m_Encoder.getAddress());
		m_Encoder.setAddress('P');
		assertEquals('P', m_Encoder.getAddress());
		m_Encoder.setAddress('Q');
		assertEquals('P', m_Encoder.getAddress());
	}

	@Test
	public void testSetAddressLowerLimit() {
		assertEquals('A', m_Encoder.getAddress());
		m_Encoder.setAddress('@');
		assertEquals('A', m_Encoder.getAddress());
	}

	@Test
	public void testSetButton() {
		assertEquals(1, m_Encoder.getButton());
		m_Encoder.setButton(2);
		assertEquals(2, m_Encoder.getButton());
	}

	@Test
	public void testSetButtonUpperLimit() {
		m_Encoder.setButton(16);
		assertEquals(16, m_Encoder.getButton());
		m_Encoder.setButton(17);
		assertEquals(16, m_Encoder.getButton());
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
		m_Encoder.setAddress('B');
		m_Encoder.setButton(2);
		m_Encoder.setCommand(1);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertEquals(1, m_Player.getMessageField(0, "HouseCode"));
		assertEquals(2, m_Player.getMessageField(0, "Button"));
		assertEquals(1, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
	}
	
	@Test
	public void testSingleHighMessage() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setAddress('P');
		m_Encoder.setButton(16);
		m_Encoder.setCommand(1);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertEquals(15, m_Player.getMessageField(0, "HouseCode"));
		assertEquals(16, m_Player.getMessageField(0, "Button"));
		assertEquals(1, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
	}

    @Test
    public void testNewEncoderInterface() throws BadMessageException {
        Message toSend = NexaEncoder.buildMessage(1, 16, 15);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 1));
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(15, m_Player.getMessageField(0, "HouseCode"));
        assertEquals(16, m_Player.getMessageField(0, "Button"));
        assertEquals(1, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
    }

	@Test
	public void testTwoMessages() {
		m_Encoder.setRepeatCount(2);
		m_Encoder.setAddress('H');
		m_Encoder.setButton(8);
		m_Encoder.setCommand(0);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(2, m_Player.getMessageCount());
		assertEquals(7, m_Player.getMessageField(0, "HouseCode"));
		assertEquals(8, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(7, m_Player.getMessageField(1, "HouseCode"));
		assertEquals(8, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}
	@Test
	public void testTwoMessagesNewInterface() throws BadMessageException {
        Message toSend = NexaEncoder.buildMessage(0, 8, 7);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 2));
		assertEquals(2, m_Player.getMessageCount());
		assertEquals(7, m_Player.getMessageField(0, "HouseCode"));
		assertEquals(8, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(7, m_Player.getMessageField(1, "HouseCode"));
		assertEquals(8, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}

	@Test
	public void testThreeMessages() {
		m_Encoder.setRepeatCount(3);
		m_Encoder.setAddress('H');
		m_Encoder.setButton(8);
		m_Encoder.setCommand(0);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(3, m_Player.getMessageCount());
		assertEquals(7, m_Player.getMessageField(0, "HouseCode"));
		assertEquals(8, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(7, m_Player.getMessageField(1, "HouseCode"));
		assertEquals(8, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
		assertEquals(7, m_Player.getMessageField(2, "HouseCode"));
		assertEquals(8, m_Player.getMessageField(2, "Button"));
		assertEquals(0, m_Player.getMessageField(2, "Command"));
		assertEquals(2, m_Player.getMessages()[2].getRepeat());
	}

	@Test
	public void basicJir() {
		JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.ALL_DECODERS - JirFileTestPlayer.Waveman_DECODER);
		
		// This file contains a Nexa message repeated 6 times
		player.playFile(this.getClass().getClassLoader()
				.getResourceAsStream("nu/nethome/coders/decoders/nexa1.jir"));

		// Verify result
		assertEquals(6, player.m_Messages.size());
		assertEquals("Nexa", player.m_Messages.get(5).getProtocol());
		assertEquals(5, player.m_Messages.get(5).getRepeat());
		assertEquals(1, player.getMessageField(5, "Command"));
		assertEquals(3, player.getMessageField(5, "Button"));
		assertEquals(2, player.getMessageField(5, "HouseCode"));
	}

}
