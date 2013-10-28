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


@Plugin
public class HKDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_HEADER = 1;
	protected static final int READING_BIT_MARK = 2;
	protected static final int READING_BIT_SPACE = 3;
	protected static final int TRAILING_BIT = 4;
	protected static final int REPEAT_SCAN = 5;
	protected static final int REPEAT_HEADER_SCAN = 6;
	protected static final int REPEAT_BODY_SPACE_SCAN = 7;
	protected static final int REPEAT_BODY_MARK_SCAN = 8;
	protected static final int REPEAT_REPEAT_SCAN = 9;

	protected int m_State = IDLE;
	
	protected int m_Command = 0;
	protected int m_CommandCheck = 0;
	protected int m_Address = 0;
	protected int m_AddressCheck = 0;

	protected int m_LastCommand = 0;
	protected int m_LastAddress = 0;
		
	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected ProtocolDecoderSink m_Sink = null;

	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("HK", "Space Length", "Harman/Kardon", 16, 5);
	}

	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < standard * 0.4;
	}

	protected boolean pulseCompare(double candidate, double min, double max) {
		return ((candidate > min) && (candidate < max));
	}
	
	protected void reportPartial() {
		if (m_BitCounter > 0) {
			m_Sink.partiallyParsedMessage("HK", m_BitCounter);
		}
	}
	
	protected void addBit(int b) {
		if (m_BitCounter < 8) {
			m_Address >>= 1;
			m_Address |= (b << 7);
		}
		else if (m_BitCounter < 16) {
			m_AddressCheck >>= 1;
			m_AddressCheck |= (b << 7);
		}
		else if (m_BitCounter < 24) {
			m_Command >>= 1;
			m_Command |= (b << 7);
		}
		else if (m_BitCounter < 32) {
			m_CommandCheck >>= 1;
			m_CommandCheck |= (b << 7);
		}
		// Check if this is a complete message
		if (m_BitCounter == 31){
			// It is, verify checksum
			if (((m_Address & 0xF0) != ((m_AddressCheck ^ 0xFF) & 0xF0)) ||
				((m_AddressCheck & 0x0F) != 0) ||
				(m_Command != (m_CommandCheck ^ 0xFF))) {
				// Checksum error
				reportPartial();
				m_State = IDLE;
				return;
			}
			ProtocolMessage message = new ProtocolMessage("HK", m_Command, m_Address, 4);
			message.setRawMessageByteAt(0, m_Address);
			message.setRawMessageByteAt(1, m_AddressCheck);
			message.setRawMessageByteAt(2, m_Command);
			message.setRawMessageByteAt(3, m_CommandCheck);
			message.addField(new FieldValue("Command", m_Command));
			message.addField(new FieldValue("Address", m_Address));
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
				if (pulseCompare(pulse, 8400.0 + 60) && state) {
					m_State = READING_HEADER;
					m_Command = 0;
					m_Address = 0;
					m_CommandCheck = 0;
					m_AddressCheck = 0;
					m_BitCounter = 0;					
				}
				break;
			}
			case READING_HEADER: {
				if (pulseCompare(pulse, 4200.0 - 60)) {
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
				if (pulseCompare(pulse, 50, 700)) {
					m_State = READING_BIT_SPACE;
				}
				else {
					m_State = IDLE;
					reportPartial();
				}
				break;
			}
			case READING_BIT_SPACE: {
				if (pulseCompare(pulse, 1575.0 - 60)) {
					m_State = READING_BIT_MARK;
					addBit(1);
				}
				else if (pulseCompare(pulse, 524.0 + 60)) {
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
				if (pulseCompare(pulse, 100, 700)) {
					m_State = REPEAT_SCAN;
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_SCAN: {
				if (pulseCompare(pulse, 45000.0)) {
					m_State = REPEAT_HEADER_SCAN;
				}
				else {
					m_RepeatCount = 0;
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_HEADER_SCAN: {
				if (pulseCompare(pulse, 9000.0)) {
					m_State = REPEAT_BODY_SPACE_SCAN;
				}
				else {
					m_RepeatCount = 0;
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_BODY_SPACE_SCAN: {
				if (pulseCompare(pulse, 2300.0 - 60)) {
					m_State = REPEAT_BODY_MARK_SCAN;
				}
				else {
					m_RepeatCount = 0;
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_BODY_MARK_SCAN: {
				if (pulseCompare(pulse, 600.0 + 60)) {
					m_State = REPEAT_REPEAT_SCAN;
					// Ok, this was arepeat stub, create a fake new message
					m_RepeatCount++;
					ProtocolMessage message = new ProtocolMessage("HK", m_Command, m_Address, 4);
					message.setRepeat(m_RepeatCount);
					message.setRawMessageByteAt(0, m_Address);
					message.setRawMessageByteAt(1, m_AddressCheck);
					message.setRawMessageByteAt(2, m_Command);
					message.setRawMessageByteAt(3, m_CommandCheck);
					message.addField(new FieldValue("Command", m_Command));
					message.addField(new FieldValue("Address", m_Address));
					// Report the parsed message
					m_Sink.parsedMessage(message);
				}
				else {
					m_RepeatCount = 0;
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_REPEAT_SCAN: {
				if (pulseCompare(pulse, 95000.0)) {
					m_State = REPEAT_HEADER_SCAN;
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

