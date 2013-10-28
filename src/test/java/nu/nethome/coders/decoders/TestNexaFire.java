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
import nu.nethome.coders.encoders.NexaFireEncoder;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for NexaFireEncoder and NexaFireDecoder
 * @author Stefan
 *
 */
public class TestNexaFire {

	private NexaFireEncoder m_Encoder;
	private NexaFireDecoder m_Decoder;
	private PulseTestPlayer m_Player;

	@Before
	public void setUp() throws Exception {
		m_Encoder = new NexaFireEncoder();
		m_Player = new PulseTestPlayer();
		m_Decoder = new NexaFireDecoder();
        m_Decoder.setTarget(m_Player);
		m_Player.setDecoder(m_Decoder);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSetAddress() {
		assertEquals(0, m_Encoder.getAddress());
		m_Encoder.setAddress(255);
		assertEquals(255, m_Encoder.getAddress());
		m_Encoder.setAddress(0x0FFFFFFF);
		assertEquals(255, m_Encoder.getAddress());
	}

	@Test
	public void testSetRepeatCount() {
		assertEquals(15, m_Encoder.getRepeatCount());
		m_Encoder.setRepeatCount(4);
		assertEquals(4, m_Encoder.getRepeatCount());
		m_Encoder.setRepeatCount(0);
		assertEquals(4, m_Encoder.getRepeatCount());
	}

	@Test
	public void testSingleMessage() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setAddress(1);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertEquals(1, m_Player.getMessageField(0, "Address"));
	}
	
	@Test
	public void testTwoMessages() {
		m_Encoder.setRepeatCount(2);
		m_Encoder.setAddress(0xFFFEFD);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(2, m_Player.getMessageCount());
		// Verify first message
		assertEquals(0xFFFEFD, m_Player.getMessageField(0, "Address"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		// Verify second message
		assertEquals(0xFFFEFD, m_Player.getMessageField(1, "Address"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}

	@Test
	public void JirWalterK() {
		JirFileTestPlayer player = new JirFileTestPlayer(JirFileTestPlayer.ALL_DECODERS);
		player.m_FlankDetector.setFlankSwing(50);
		player.m_FlankDetector.setFlankLength(4);
		player.m_FlankDetector.setPulseWidthCompensation(0);
		
		// This file contains a NexaFire message repeated 11 times
		player.playFile(this.getClass().getClassLoader()
				.getResourceAsStream("nu/nethome/coders/decoders/nexa_fire.jir"));

		// Verify result
		assertEquals(8, player.m_Messages.size());
		assertEquals("NexaFire", player.m_Messages.get(7).getProtocol());
		assertEquals(7, player.m_Messages.get(7).getRepeat());
		assertEquals(0x0BD6BC, player.getMessageField(7, "Address"));
		
//		assertEquals(0xBC, player.getMessageField(7, "Raw1"));
//		assertEquals(0xD6, player.getMessageField(7, "Raw2"));
//		assertEquals(0x0B, player.getMessageField(7, "Raw3"));
	}
}
