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

import nu.nethome.coders.decoders.util.JirFileTestPlayer;
import nu.nethome.coders.encoders.NexaLEncoder;
import nu.nethome.util.ps.BadMessageException;
import nu.nethome.util.ps.Message;
import nu.nethome.util.ps.MessageRepeater;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for NexaLEncoder and NexaLDecoder
 * @author Stefan
 *
 */
public class TestNexaLDecoder {

	private NexaLEncoder m_Encoder;
	private NexaLDecoder m_Decoder;
	private PulseTestPlayer m_Player;

	@Before
	public void setUp() throws Exception {
		m_Encoder = new NexaLEncoder();
		m_Player = new PulseTestPlayer();
		m_Decoder = new NexaLDecoder();
        m_Decoder.setTarget(m_Player);
		m_Player.setDecoder(m_Decoder);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSetAddress() {
		assertEquals(0, m_Encoder.getAddress());
		m_Encoder.setAddress(0xaabbcc);
		assertEquals(0xaabbcc, m_Encoder.getAddress());
	}
	
	@Test
	public void testSetButton() {
		assertEquals(1, m_Encoder.getButton());
		m_Encoder.setButton(3);
		assertEquals(3, m_Encoder.getButton());
		m_Encoder.setButton(16);
		assertEquals(16, m_Encoder.getButton());
		m_Encoder.setButton(17);
		assertEquals(17, m_Encoder.getButton());
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
		m_Encoder.setRepeatCount(4);
		assertEquals(4, m_Encoder.getRepeatCount());
		m_Encoder.setRepeatCount(0);
		assertEquals(4, m_Encoder.getRepeatCount());
	}

	@Test
	public void testSingleMessage() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setAddress(0xaabbcc);
		m_Encoder.setButton(3);
		m_Encoder.setCommand(1);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertEquals(0xaabbcc, m_Player.getMessageField(0, "Address"));
		assertEquals(3, m_Player.getMessageField(0, "Button"));
		assertEquals(1, m_Player.getMessageField(0, "Command"));
	}

    @Test
    public void testSingleMessageNewEncoder() throws BadMessageException {
        Message toSend = NexaLEncoder.buildMessage(1, 3, 0xaabbcc);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 1));
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(0xaabbcc, m_Player.getMessageField(0, "Address"));
        assertEquals(3, m_Player.getMessageField(0, "Button"));
        assertEquals(1, m_Player.getMessageField(0, "Command"));
    }

	@Test
	public void testTwoMessages() {
		m_Encoder.setRepeatCount(2);
		m_Encoder.setAddress(0x3ffffff);
		m_Encoder.setButton(17);
		m_Encoder.setCommand(0);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(2, m_Player.getMessageCount());
		assertEquals(0x3ffffff, m_Player.getMessageField(0, "Address"));
		assertEquals(17, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(0x3ffffff, m_Player.getMessageField(1, "Address"));
		//assertEquals(17, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}

	@Test
	public void testThreeMessages() {
		m_Encoder.setRepeatCount(3);
		m_Encoder.setAddress(0x248a957);
		m_Encoder.setButton(17);
		m_Encoder.setCommand(0);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(3, m_Player.getMessageCount());
		assertEquals(0x248a957, m_Player.getMessageField(0, "Address"));
		assertEquals(17, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(0x248a957, m_Player.getMessageField(1, "Address"));
		//assertEquals(17, m_Player.getMessageField(1, "Button"));
		//assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
		assertEquals(0x248a957, m_Player.getMessageField(2, "Address"));
		assertEquals(17, m_Player.getMessageField(2, "Button"));
		assertEquals(0, m_Player.getMessageField(2, "Command"));
		assertEquals(2, m_Player.getMessages()[2].getRepeat());
	}
	
	@Test
	public void basicJir() {
		JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.ALL_DECODERS);
		
		// This file contains a NexaL message repeated 11 times
		player.playFile(this.getClass().getClassLoader()
				.getResourceAsStream("nu/nethome/coders/decoders/nexal1.jir"));

		// Verify result
		assertEquals(11, player.m_Messages.size());
		assertEquals("NexaL", player.m_Messages.get(10).getProtocol());
		assertEquals(10, player.m_Messages.get(10).getRepeat());
		assertEquals(0x155be6, player.getMessageField(10, "Address"));
		assertEquals(10, player.getMessageField(10, "Button"));
	}

	@Test
	public void JirDavidNaslund() {
		JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.ALL_DECODERS);
		player.m_FlankDetector.setFlankSwing(40);
		player.m_FlankDetector.setFlankLength(4);
		player.m_FlankDetector.setPulseWidthCompensation(0);
		
		// This file contains a NexaL message repeated 11 times
		player.playFile(this.getClass().getClassLoader()
				.getResourceAsStream("nu/nethome/coders/decoders/nexal_dn.jir"));

		// Verify result
		assertEquals(5, player.m_Messages.size());
		assertEquals("NexaL", player.m_Messages.get(4).getProtocol());
		assertEquals(4, player.m_Messages.get(4).getRepeat());
		assertEquals(0x2b38aa, player.getMessageField(4, "Address"));
		assertEquals(11, player.getMessageField(4, "Button"));
	}

}
