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

import nu.nethome.coders.encoders.ProntoEncoder;
import nu.nethome.util.ps.BadMessageException;
import nu.nethome.util.ps.Message;
import nu.nethome.util.ps.MessageRepeater;
import nu.nethome.util.ps.ProtocolEncoder;
import nu.nethome.util.ps.impl.PulseTestPlayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Unit tests for X10Encoder and X10Decoder.
 * @author �garen
 *
 */
public class TestProntoDecoder {

	private ProntoEncoder m_Encoder;
	private ProntoDecoder m_Decoder;
	private PulseTestPlayer m_Player;

	@Before
	public void setUp() throws Exception {
		m_Encoder = new ProntoEncoder();
		m_Player = new PulseTestPlayer();
		m_Decoder = new ProntoDecoder();
        m_Decoder.setTarget(m_Player);
		m_Player.setDecoder(m_Decoder);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGeneral() {
		m_Decoder.addBurst(10000, false);
	}
	@Test
	public void testSetCommand() {
		assertEquals("", m_Encoder.getMessage());
		m_Encoder.setMessage("0000 1111 2222");
		assertEquals("0000 1111 2222", m_Encoder.getMessage());
	}

	@Test
	public void testSetRepeatCount() {
		assertEquals(5, m_Encoder.getRepeatCount());
		m_Encoder.setRepeatCount(6);
		assertEquals(6, m_Encoder.getRepeatCount());
		m_Encoder.setRepeatCount(0);
		assertEquals(6, m_Encoder.getRepeatCount());
	}

	protected final String sonyMessage1 =
			"0000 0067 0000 0015 0060 0018 0018 0018 0030 0018 0030 0018 0030 " + 
			"0018 0018 0018 0030 0018 0018 0018 0018 0018 0030 0018 0018 0018 " +
			"0030 0018 0030 0018 0030 0018 0018 0018 0018 0018 0030 0018 0018 " +
			"0018 0018 0018 0030 0018 0018 03f6";

	protected final String sonyMessage2 =
			"0000 0067 0001 0014 0060 0018 0018 0018 0030 0018 0030 0018 0030 " +
			"0018 0018 0018 0030 0018 0018 0018 0018 0018 0030 0018 0018 0018 " +
			"0030 0018 0030 0018 0030 0018 0018 0018 0018 0018 0030 0018 0018 " +
			"0018 0018 0018 0030 0018 0018 03f6";

    @Test
    public void testSingleEncodingNoHeader() throws BadMessageException {
        Message toSend = ProntoEncoder.createMessage(sonyMessage1);
        int header[] = m_Encoder.encode(toSend, ProtocolEncoder.Phase.FIRST);
        int body[] = m_Encoder.encode(toSend, ProtocolEncoder.Phase.REPEATED);
        assertThat(header.length, is(0));
        assertThat(body.length, is(42));
    }

    @Test
    public void testSingleEncodingWithHeader() throws BadMessageException {
        Message toSend = ProntoEncoder.createMessage(sonyMessage2);
        int header[] = m_Encoder.encode(toSend, ProtocolEncoder.Phase.FIRST);
        int body[] = m_Encoder.encode(toSend, ProtocolEncoder.Phase.REPEATED);
        assertThat(header.length, is(2));
        assertThat(body.length, is(40));
    }


	@Test
	public void testSingleMessageNoMod() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setMessage(sonyMessage1);
		m_Player.setPulseWidthModification(0);
		m_Decoder.setPulseWidthModification(0);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertEquals(sonyMessage1, m_Player.getMessageFieldString(0, "Message"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
	}

	@Test
	public void testSingleMessageMod() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setMessage(sonyMessage1);
		m_Player.setPulseWidthModification(60);
		m_Decoder.setPulseWidthModification(60);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertEquals(sonyMessage1, m_Player.getMessageFieldString(0, "Message"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
	}

	@Test
	public void testSingleMessageFailMod() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setMessage(sonyMessage1);
		m_Player.setPulseWidthModification(0);
		m_Decoder.setPulseWidthModification(60);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(1, m_Player.getMessageCount());
		assertFalse(sonyMessage1.equals(m_Player.getMessageFieldString(0, "Message")));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
	}

	@Test
	public void testThreeMessages() {
		m_Encoder.setRepeatCount(3);
		m_Encoder.setMessage(sonyMessage1);
		m_Player.setPulseWidthModification(60);
		m_Decoder.setPulseWidthModification(60);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(3, m_Player.getMessageCount());
		assertEquals(sonyMessage1, m_Player.getMessageFieldString(0, "Message"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(sonyMessage1, m_Player.getMessageFieldString(1, "Message"));
		assertEquals(0, m_Player.getMessages()[1].getRepeat());
		assertEquals(sonyMessage1, m_Player.getMessageFieldString(2, "Message"));
		assertEquals(0, m_Player.getMessages()[2].getRepeat());
	}

    @Test
    public void testThreeMessagesNewInterface() throws BadMessageException {
        Message toSend = ProntoEncoder.createMessage(sonyMessage1);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 3));
        assertEquals(3, m_Player.getMessageCount());
        assertEquals(sonyMessage1, m_Player.getMessageFieldString(0, "Message"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
        assertEquals(sonyMessage1, m_Player.getMessageFieldString(1, "Message"));
        assertEquals(0, m_Player.getMessages()[1].getRepeat());
        assertEquals(sonyMessage1, m_Player.getMessageFieldString(2, "Message"));
        assertEquals(0, m_Player.getMessages()[2].getRepeat());
    }
}
