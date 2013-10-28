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

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

/**
 * Decodes the RF version of the X10 protocol. X10 is a system for remote control
 * of lamps and other mains devices. X10 messages are normally sent via the mains
 * lines to the devices, but to be able to use remote controls there are also devices
 * which can receive RF-signals and transmit them as X10 messages via the mains. 
 * The RF-version of the protocol is based on the same protocol used in JVC IR remotes(!).
 * The protocol uses space length encoding and sends 32 bits per message, where 16 bits
 * are redundant error checking, so there are 16 real bits of data.<br>
 * <br>
 * Decoded messages have two or three parameters:<br>
 * <br>
 * <b>Command:</b> Message command, (0 - 5), see {@link nu.nethome.coders.encoders.X10Encoder#setCommand} for details.<br>
 * <b>HouseCode:</b> House code address of the message (0 - 15)<br>
 * <b>Button:</b> Button address of the message (1 - 16), this is only sent for commands 0 and 1.<br>
 * 
 * @see nu.nethome.coders.encoders.X10Encoder
 * @author Stefan Str�mberg
 */
@Plugin
public class X10Decoder implements ProtocolDecoder {

	public static final int COMMAND_OFF = 0;
	public static final int COMMAND_ON = 1;
	public static final int COMMAND_DIM = 2;
	public static final int COMMAND_BRIGHT = 3;
	public static final int COMMAND_ALL_OFF = 4;
	public static final int COMMAND_ALL_ON = 5;

	private static final double X10REPEAT_MAX = 42000.0;
	private static final double X10REPEAT_MIN = 38000.0;
	private static final double X10SHORT_SPACE = 524.0;
	private static final double X10LONG_SPACE = 1575.0;
	private static final double X10MARK_MAX = 700;
	private static final double X10MARK_MIN = 300;
	private static final double X10HEADER_SPACE = 4500.0;
	private static final double X10HEADER_MARK = 9000.0;

	protected static final int IDLE = 0;
	protected static final int READING_HEADER = 1;
	protected static final int READING_BIT_MARK = 2;
	protected static final int READING_BIT_SPACE = 3;
	protected static final int TRAILING_BIT = 4;
	protected static final int REPEAT_SCAN = 5;

	/**
	 * Conversion table from received command bits to our command coding. We are
	 * adding 2 to the received command bit command to leave room for the on/off commands.
	 */
	final static int commandConversion[] = 
	{1, 0, COMMAND_ALL_OFF, COMMAND_BRIGHT, COMMAND_ALL_ON, COMMAND_DIM};

	
	/**
	 * The encoding of the HouseCode (which is really A-P) is irradic.
	 * We have to use a table to convert from encoding to HouseCode. 
	 * Using the received code as index will give the corresponding HouseCode
	 * in numeric format (0-15 instead of A-P)
	 */
	final static int houseCodeConversion[] = 
	{12, 13, 14, 15, 2, 3, 0, 1, 4, 5, 6, 7, 10, 11, 8, 9};
	/*
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

	protected int m_State = IDLE;
	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected ProtocolDecoderSink m_Sink = null;
	private long m_Data = 0;
	private long m_LastData = 0;
	
	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("X10", "Space Length", "X10", 16, 5);
	}

	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < standard * 0.4;
	}

	protected boolean pulseCompare(double candidate, double min, double max) {
		return ((candidate > min) && (candidate < max));
	}
	
	protected void reportPartial() {
		if (m_BitCounter > 0) {
			m_Sink.partiallyParsedMessage("X10", m_BitCounter);
		}
	}
	
	/**
	 * Protocol sends 32 bits where 16 are for error checking. Byte 1' is the same
	 * as Byte 1 but all bits inverted, and similar for Byte 2'. Messages can be sent
	 * in two modes. When turning lamps on and off, the actual device number (Button)
	 * is sent in the message (address mode). When dimming and brighting is sent,
	 * (command mode) no device number is sent in the message, so the command applies
	 * to the latest addressed device. House Code is always sent.
	 * <br> 
	 * N= Not On<br>
	 * C= Command bit, 0->address mode, 1->command mode<br>
	 * 0-3 Button address<br>
	 * a= House Code (scattered coding)<br>
	 * c= command. 00=All Off, 01=Bright, 10=All On, 11=Dim<br> 
	 * <br>
	 * ____Byte 1_____  ____Byte 1'____  ____Byte 2_____  ____Byte 2'____<br>
	 * 7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0<br>
	 * a a a a   3                       C 2 N 0 1        address mode<br>
	 * a a a a                           C     c c        command mode<br>
	 * <br>
	 * @see  "ftp://ftp.x10.com/pub/manuals/cm17a_protocol.txt"
	 * @param b Bit to add, 0 or 1
	 */
	protected void addBit(int b) {
		m_Data  <<= 1;
		m_Data |= b;
		// Check if this is a complete message
		if (m_BitCounter == 31){
			// It is, get the separate bytes
			int byte2Check = (int)(m_Data & 0xFF);
			int byte2 = (int)((m_Data >> 8) & 0xFF);
			int byte1Check = (int)((m_Data >> 16) & 0xFF);
			int byte1 = (int)((m_Data >> 24) & 0xFF);
			
			// Verify checksum
			if ((byte2 != (byte2Check ^ 0xFF)) || (byte1 != (byte1Check ^ 0xFF))) {
				// Checksum error
				reportPartial();
				m_State = IDLE;
				return;
			}
			// Decode data
			int houseCode = houseCodeConversion[byte1 >> 4];
			int button = 0;
			int command = 0;
			// Check if it is command mode
			if ((byte2 & 0x80) != 0) {
				command = commandConversion[((byte2 >> 3) & 0x03) + 2];
			}
			else {
				command = commandConversion[((byte2 >> 5) & 0x01)];
				button = ((byte2 >> 4) & 1) + (((byte2 >> 3) & 1) << 1) + 
					(((byte2 >> 6) & 1) << 2) + (((byte1 >> 2) & 1) << 3) + 1;
			}
				
			ProtocolMessage message = new ProtocolMessage("X10", command, (houseCode << 4) + button, 2);
			message.setRawMessageByteAt(0, byte1);
			message.setRawMessageByteAt(1, byte2);
			// It is, check if this really is a repeat
			if ((m_RepeatCount > 0) && (m_Data == m_LastData)) {
				message.setRepeat(m_RepeatCount);
			}
			else {
				// It is not a repeat, reset counter
				m_RepeatCount = 0;
			}
			message.addField(new FieldValue("Command", command));
			if (command < 2) {
				message.addField(new FieldValue("Button", button));
			}
			message.addField(new FieldValue("HouseCode", houseCode));
			// Report the parsed message
			m_Sink.parsedMessage(message);
			m_State = TRAILING_BIT;		
		}
		m_BitCounter++;
	}
	
	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (pulseCompare(pulse, X10HEADER_MARK) && state) {
					m_State = READING_HEADER;
					m_Data = 0;
					m_BitCounter = 0;					
				}
				else {
					m_RepeatCount = 0;
				}
				break;
			}
			case READING_HEADER: {
				if (pulseCompare(pulse, X10HEADER_SPACE)) {
					m_State = READING_BIT_MARK;
				}
				else {
					m_State = IDLE;
					reportPartial();
				}
				break;
			}
			case READING_BIT_MARK: {
				// The mark pulse seems to vary a lot in length
				if (pulseCompare(pulse, X10MARK_MIN, X10MARK_MAX)) {
					m_State = READING_BIT_SPACE;
				}
				else {
					m_State = IDLE;
					reportPartial();
				}
				break;
			}
			case READING_BIT_SPACE: {
				if (pulseCompare(pulse, X10LONG_SPACE)) {
					m_State = READING_BIT_MARK;
					addBit(1);
				}
				else if (pulseCompare(pulse, X10SHORT_SPACE)) {
					m_State = READING_BIT_MARK;
					addBit(0);
				}
				else {
					m_State = IDLE;
					reportPartial();
				}
				break;
			}

			case TRAILING_BIT: {
				// The mark pulse seems to vary a lot in length
				if (pulseCompare(pulse, X10MARK_MIN, X10MARK_MAX)) {
					m_State = REPEAT_SCAN;
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_SCAN: {
				if (pulseCompare(pulse, X10REPEAT_MIN, X10REPEAT_MAX)) {
					m_RepeatCount += 1; // Start repeat sequence
					// Save this sequence
					m_LastData = m_Data;
					m_State = IDLE;
				}
				else {
					m_RepeatCount = 0;
					m_State = IDLE;
				}
				break;
			}
		}
        return m_State;
	}
}

