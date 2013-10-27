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

package nu.nethome.coders.encoders;

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.ArrayList;

/**
 * Encodes the RF version of the X10 protocol. X10 is a system for remote control
 * of lamps and other mains devices. X10 messages are normally sent via the mains
 * lines to the devices, but to be able to use remote controls there are also devices
 * which can receive RF-signals and transmit them as X10 messages via the mains. 
 * The RF-version of the protocol is based on the same protocol used in JVC IR remotes(!).
 * The protocol uses space length encoding and sends 32 bits per message, where 16 bits
 * are redundant error checking, so there are 16 real bits of data. The X10Encoder
 * can take the data from a protocol message and encode that into a sequence
 * of pulse lengths which may be played by RF-transmitting hardware for instance 
 * via the AudioPulsePlayer.
 * 
 * @see nu.nethome.coders.decoders.X10Decoder
 * @see nu.nethome.util.ps.impl.AudioPulsePlayer
 * @author Stefan Str�mberg
 *
 */
@Plugin
public class X10Encoder implements ProtocolEncoder {
	
	public static final int COMMAND_OFF = 0;
	public static final int COMMAND_ON = 1;
	public static final int COMMAND_DIM = 2;
	public static final int COMMAND_BRIGHT = 3;
	public static final int COMMAND_ALL_OFF = 4;
	public static final int COMMAND_ALL_ON = 5;

	private int m_RepeatCount = 3;
	private int m_HouseCode = 0;
	private int m_Command = 1;
	private int m_Button = 0;

	// Pulse lengths of the protocol
	private static final int X10_REPEAT = 40000 + 60;
	private static final int X10_SHORT_SPACE = 570 + 60;
	private static final int X10_LONG_SPACE = 1670 + 60;
	private static final int X10_MARK = 570 - 60;
	private static final int X10_HEADER_SPACE = 4500 + 60;
	private static final int X10_HEADER_MARK = 9000 - 60;

	// States of the state machine
	protected static final int IDLE = 0;
	protected static final int READING_HEADER = 1;
	protected static final int READING_BIT_MARK = 2;
	protected static final int READING_BIT_SPACE = 3;
	protected static final int TRAILING_BIT = 4;
	protected static final int REPEAT_SCAN = 5;

	/**
	 * Conversion table from our command coding to protocol encoding. 
	 */
	final static int commandConversion[] = {0x0020, 0x0000, 0x0098, 0x0088, 0x0080, 0x0090};

	/**
	 * The encoding of the HouseCode (which is really A-P) is irradic.
	 * We have to use a table to convert from HouseCode to encoding. 
	 * Using the HoseCode as index will give the corresponding encoding
	 */
	final static int houseCodeConversion[] = 
	{6, 7, 4, 5, 8, 9, 10, 11, 14, 15, 12, 13, 0, 1, 2, 3};
	/* The protocol encoding of the House Codes is:
	 * A 0          01100000 6
	 * B 1          01110000 7
	 * C 2          01000000 4
	 * D 3          01010000 5
	 * E 4          10000000 8
	 * F 5          10010000 9
	 * G 6          10100000 10
	 * H 7          10110000 11
	 * I 8          11100000 14
	 * J 9          11110000 15
	 * K 10         11000000 12
	 * L 11         11010000 13
	 * M 12         00000000 0
	 * N 13         00010000 1
	 * O 14         00100000 2
	 * P 15         00110000 3
	 */

	protected static final int MESSAGE_LENGTH = 32;
	protected static final long HIGH_BIT = 1L << (MESSAGE_LENGTH - 1);
	
	static final int s_Buttons[] = {0x20, 0x10, 0x04, 0x08};

	/**
	 * Encodes the current protocol message into the corresponding pulse sequence.
	 * See {@link nu.nethome.coders.decoders.X10Decoder#addBit} for details on protocol encoding.
	 */
	public int[] encode() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		long message = 0;
		
		// encode message
		long messageTemplate = 0x00000000;
		messageTemplate |= commandConversion[m_Command];
		if (m_Command < 2) {
			messageTemplate |= ((m_Button & 1) << 4) + ((m_Button & 2) << (3 -1)) + 
			((m_Button & 4) << (6 - 2)) + ((m_Button & 8) << (10 - 3));
		}
		messageTemplate |= houseCodeConversion[m_HouseCode] << 12;
		int byte1 = (int)(messageTemplate >> 8);
		int byte2 = (int)(messageTemplate & 0xFF);

		long rawMessageTemplate = (byte1 << 24) + ((byte1 ^ 0xFF) << 16) +
								  (byte2 << 8)  + (byte2 ^ 0xFF);
		
		// Start encoding the data pulses
		for (int i = 0; i < m_RepeatCount; i++) {
			message = rawMessageTemplate;

			// Encode header
			result.add(X10_HEADER_MARK);
			result.add(X10_HEADER_SPACE);

			// Encode message bits
			for (int j = 0; j < (MESSAGE_LENGTH); j++) {
				result.add(X10_MARK);
				if ((message & HIGH_BIT) == HIGH_BIT) {
					result.add(X10_LONG_SPACE);
				}
				else {
					result.add(X10_SHORT_SPACE);
				}
				message <<= 1;
			}
			// Add end pulse
			result.add(X10_MARK);
			// Add the repeat delay
			result.add(X10_REPEAT);
		}
		int resultArray[] = new int[result.size()];
		for (int i = 0; i < result.size(); i++) {
			resultArray[i] = result.get(i);
		}
		return resultArray;
	}

	/**
	 * Get the HouseCode address of the message
	 * @return HouseCode
	 */
	public int getHouseCode() {
		return m_HouseCode;
	}

	/**
	 * Set the HouseCode address of the message. In X10, these are called A - P, but
	 * here we use the corresponding number. 
	 * @param HouseCode (0 - 15)
	 */
	public void setHouseCode(int HouseCode) {
		if ((HouseCode > (15)) || (HouseCode < 0)) {
			return;
		}
		m_HouseCode = HouseCode;
	}

	/**
	 * Get the command, see {@link setCommand}.
	 * @return command
	 */
	public int getCommand() {
		return m_Command;
	}

	/**
	 * Sets the command. Following commands are available:<br>
	 * {@link COMMAND_OFF} which turns off the device with corresponding HouseCode and Button<br>
	 * {@link COMMAND_ON} which turns off the device with corresponding HouseCode and Button<br>
	 * {@link COMMAND_DIM} which dims the latest addressed device with corresponding HouseCode.
	 * Note that the button address is ignored in this message.<br>
	 * {@link COMMAND_BRIGHT} which brightens the latest addressed device with corresponding HouseCode.
	 * Note that the button address is ignored in this message.<br>
	 * {@link COMMAND_ALL_OFF} which turns off all devices with corresponding HouseCode.
	 * Button address is ignored in this message.<br>
	 * {@link COMMAND_ALL_ON} which turns on all devices with corresponding HouseCode.
	 * Button address is ignored in this message.<br>
	 *  
	 * @param command (0 - 5)
	 */
	public void setCommand(int command) {
		if ((command > 5) || (command < 0)) {
			return;
		}
		m_Command = command;
	}

	/**
	 * See {@link setRepeatCount}
	 * @return repeat count
	 */
	public int getRepeatCount() {
		return m_RepeatCount;
	}

	/**
	 * Set the number of times the message is repeated in the encoding. Normally a 
	 * message which is transmitted over RF or IR is repeated a number of times in
	 * case some messages are lost due to interference. A normal value would be
	 * around 3.
	 * 
	 * @param repeatCount number of repeats (1 - 100)
	 */
	public void setRepeatCount(int repeatCount) {
		if ((repeatCount < 1) || (repeatCount > 100)) {
			return;
		}
		this.m_RepeatCount = repeatCount;
	}

	/**
	 * See {@link setButton}
	 * @return
	 */
	public int getButton() {
		return m_Button + 1;
	}

	/**
	 * Sets the button address of the message. Button corresponds to the button number
	 * you would press on a RF-remote, and is the device address within the HouseCode.
	 * Button address is numbered from 1 to 16. 
	 * @param button The button address (1 - 16)
	 */
	public void setButton(int button) {
		if ((button < 1) || (button > 16)) {
			return;
		}
		m_Button = button - 1;
	}

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("X10", "Space Length", "X10", 16, 5);
    }

    public int[] encode(Message message, Phase phase) throws BadMessageException {
        if (Phase.FIRST == phase) {
            return new int[0];
        }
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Command")) {
                setCommand(field.getValue());
            } else if (field.getName().equals("Button")) {
                setButton(field.getValue());
            } else if (field.getName().equals("HouseCode")) {
                setHouseCode(field.getValue());
            } else {
                throw new BadMessageException(field);
            }
        }
        setRepeatCount(1);
        return encode();
    }

    @Override
    public int modulationFrequency(Message message) {
        return 40000;
    }

    public static Message buildAddressMessage(boolean lampOn, int button, int houseCode) {
        int command = lampOn ? COMMAND_ON : COMMAND_OFF;
        ProtocolMessage result = new ProtocolMessage("X10", command, button, 0);
        result.addField(new FieldValue("Command", command));
        result.addField(new FieldValue("Button", button));
        result.addField(new FieldValue("HouseCode", houseCode));
        return result;
    };

    public static Message buildCommandMessage(int command, int houseCode) {
        ProtocolMessage result = new ProtocolMessage("X10", command, command, 0);
        result.addField(new FieldValue("Command", command));
        result.addField(new FieldValue("HouseCode", houseCode));
        return result;
    };
}

