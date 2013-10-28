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

import nu.nethome.coders.encoders.X10Encoder;
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
 * Unit tests for X10Encoder and X10Decoder.
 * @author �garen
 *
 */
public class TestX10Decoder {

	private X10Encoder m_Encoder;
	private X10Decoder m_Decoder;
	private PulseTestPlayer m_Player;

	@Before
	public void setUp() throws Exception {
		m_Encoder = new X10Encoder();
		m_Player = new PulseTestPlayer();
		m_Decoder = new X10Decoder();
        m_Decoder.setTarget(m_Player);
		m_Player.setDecoder(m_Decoder);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSetAddress() {
		assertEquals(0, m_Encoder.getHouseCode());
		m_Encoder.setHouseCode(15);
		assertEquals(15, m_Encoder.getHouseCode());
		m_Encoder.setHouseCode(0x40);
		assertEquals(15, m_Encoder.getHouseCode());
	}

	@Test
	public void testSetButton() {
		assertEquals(1, m_Encoder.getButton());
		m_Encoder.setButton(2);
		assertEquals(2, m_Encoder.getButton());
		m_Encoder.setButton(-1);
		assertEquals(2, m_Encoder.getButton());
		m_Encoder.setButton(17);
		assertEquals(2, m_Encoder.getButton());
	}

	@Test
	public void testSetCommand() {
		assertEquals(1, m_Encoder.getCommand());
		m_Encoder.setCommand(0);
		assertEquals(0, m_Encoder.getCommand());
		m_Encoder.setCommand(6);
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
    public void info() {
        ProtocolInfo info = m_Encoder.getInfo();
        assertThat(info.getName(), is("X10"));
    }

	@Test
	public void testSingleMessage() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setHouseCode(1);
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
    public void testSingleAddressMessageNewInterface() throws BadMessageException {
        Message toSend = X10Encoder.buildAddressMessage(true, 2, 1);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 1));
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(1, m_Player.getMessageField(0, "HouseCode"));
        assertEquals(2, m_Player.getMessageField(0, "Button"));
        assertEquals(X10Decoder.COMMAND_ON, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
    }

    @Test
    public void testSingleCommandMessageNewInterface() throws BadMessageException {
        Message toSend = X10Encoder.buildCommandMessage(X10Encoder.COMMAND_BRIGHT, 1);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 1));
        assertEquals(1, m_Player.getMessageCount());
        assertEquals(1, m_Player.getMessageField(0, "HouseCode"));
        assertEquals(-1, m_Player.getMessageField(0, "Button"));
        assertEquals(X10Encoder.COMMAND_BRIGHT, m_Player.getMessageField(0, "Command"));
        assertEquals(0, m_Player.getMessages()[0].getRepeat());
    }


	@Test
	public void testTwoAddressMessagesNewInterface() throws BadMessageException {
        Message toSend = X10Encoder.buildAddressMessage(false, 3, 10);
        m_Player.playMessage(MessageRepeater.repeat(m_Encoder, toSend, 2));
		assertEquals(2, m_Player.getMessageCount());
		assertEquals(10, m_Player.getMessageField(0, "HouseCode"));
		assertEquals(3, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(10, m_Player.getMessageField(1, "HouseCode"));
		assertEquals(3, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}

	@Test
	public void testTwoAddressMessages() {
		m_Encoder.setRepeatCount(2);
		m_Encoder.setHouseCode(10);
		m_Encoder.setButton(3);
		m_Encoder.setCommand(0);
		m_Player.playMessage(m_Encoder.encode());
		assertEquals(2, m_Player.getMessageCount());
		assertEquals(10, m_Player.getMessageField(0, "HouseCode"));
		assertEquals(3, m_Player.getMessageField(0, "Button"));
		assertEquals(0, m_Player.getMessageField(0, "Command"));
		assertEquals(0, m_Player.getMessages()[0].getRepeat());
		assertEquals(10, m_Player.getMessageField(1, "HouseCode"));
		assertEquals(3, m_Player.getMessageField(1, "Button"));
		assertEquals(0, m_Player.getMessageField(1, "Command"));
		assertEquals(1, m_Player.getMessages()[1].getRepeat());
	}

	@Test
	public void testAllCommands() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setHouseCode(1);
		m_Encoder.setButton(2);
		for (int command = 0; command < 6; command++) {
			m_Encoder.setCommand(command);
			m_Player.playMessage(m_Encoder.encode());
			assertEquals(command + 1, m_Player.getMessageCount());
			assertEquals(1, m_Player.getMessageField(command, "HouseCode"));
			if (command < 2) {
				assertEquals(2, m_Player.getMessageField(command, "Button"));
			}
			else {
				assertEquals(-1, m_Player.getMessageField(command, "Button"));
			}
			assertEquals(command, m_Player.getMessageField(command, "Command"));
			assertEquals(0, m_Player.getMessages()[command].getRepeat());
		}
	}

	@Test
	public void testAllHouseCodes() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setButton(2);
		m_Encoder.setCommand(1);
		for (int houseCode = 0; houseCode < 16; houseCode++) {
			m_Encoder.setHouseCode(houseCode);
			m_Player.playMessage(m_Encoder.encode());
			assertEquals(houseCode + 1, m_Player.getMessageCount());
			assertEquals(houseCode, m_Player.getMessageField(houseCode, "HouseCode"));
			assertEquals(2, m_Player.getMessageField(houseCode, "Button"));
			assertEquals(1, m_Player.getMessageField(houseCode, "Command"));
			assertEquals(0, m_Player.getMessages()[houseCode].getRepeat());
		}
	}

	@Test
	public void testAllButtons() {
		m_Encoder.setRepeatCount(1);
		m_Encoder.setCommand(1);
		m_Encoder.setHouseCode(2);
		for (int button = 1; button < 17; button++) {
			m_Encoder.setButton(button);
			m_Player.playMessage(m_Encoder.encode());
			assertEquals(button, m_Player.getMessageCount());
			assertEquals(2, m_Player.getMessageField(button - 1, "HouseCode"));
			assertEquals(button, m_Player.getMessageField(button - 1, "Button"));
			assertEquals(1, m_Player.getMessageField(button - 1, "Command"));
			assertEquals(0, m_Player.getMessages()[button - 1].getRepeat());
		}
	}

}
