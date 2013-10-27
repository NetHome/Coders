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

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

@Plugin
public class SIRCDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_HEADER = 1;
	protected static final int READING_BIT_MARK = 2;
	protected static final int READING_BIT_SPACE = 3;
	protected static final int REPEAT_SCAN = 4;

	protected int m_State = IDLE;
	
	protected int m_Command = 0;
	protected int m_Address = 0;
	protected int m_Extra = 0;

	protected int m_LastCommand = 0;
	protected int m_LastAddress = 0;
	protected int m_LastExtra = 0;
		
	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected ProtocolDecoderSink m_Sink = null;

	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("SIRC20", "Mark Length", "Sony", 20, 5);
	}

	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < standard * 0.3;
	}
	
	protected void addBit(int b) {
		if (m_BitCounter < 7) {
			m_Command += b << m_BitCounter;
		}
		else if (m_BitCounter < 12) {
			m_Address += b << (m_BitCounter - 7);
		}
		else if (m_BitCounter < 20) {
			m_Extra += b << (m_BitCounter - 12);
		}
		// Check if this is a complete message
		if (m_BitCounter == 19){
			// It is, create the message
			ProtocolMessage message = new ProtocolMessage("SIRC20", m_Command, m_Address, 3);
			message.setRawMessageByteAt(0, m_Command);
			message.setRawMessageByteAt(1, m_Address);
			message.setRawMessageByteAt(2, m_Extra);
			message.addField(new FieldValue("Command", m_Command));
			message.addField(new FieldValue("Address", m_Address));
			message.addField(new FieldValue("Extended", m_Extra));

			// It is, check if this really is a repeat
			if ((m_RepeatCount > 0) && (m_Command == m_LastCommand) && (m_Address == m_LastAddress)
				 && (m_Extra == m_LastExtra)) {
				message.setRepeat(m_RepeatCount);
			}
			else {
				// It is not a repeat, reset counter
				m_RepeatCount = 0;
			}
			// Report the parsed message
			m_Sink.parsedMessage(message);
			m_State = REPEAT_SCAN;		
		}
		m_BitCounter++;
	}
	
	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (pulseCompare(pulse, 2400.0 + 60) && state) {
					m_State = READING_HEADER;
					m_Command = 0;
					m_Address = 0;
					m_Extra = 0;
					m_BitCounter = 0;					
				}
				break;
			}
			case READING_HEADER: {
				if (pulseCompare(pulse, 600.0 - 60)) {
					m_State = READING_BIT_MARK;
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_MARK: {
				if (pulseCompare(pulse, 1200.0 + 60)) {
					m_State = READING_BIT_SPACE;
					addBit(1);
				}
				else if (pulseCompare(pulse, 600.0 + 60)) {
					m_State = READING_BIT_SPACE;
					addBit(0);
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_SPACE: {
				if (pulseCompare(pulse, 600.0 - 60)) {
					m_State = READING_BIT_MARK;
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_SCAN: {
				if (pulseCompare(pulse, 12000.0)) {
					m_RepeatCount += 1; // Start repeat sequence
					// Save this sequence
					m_LastCommand = m_Command;
					m_LastAddress = m_Address;
					m_LastExtra = m_Extra ;
				}
				else {
					m_RepeatCount = 0;
				}
				m_State = IDLE;
				break;
			}
		}
        return m_State;
	}
}
