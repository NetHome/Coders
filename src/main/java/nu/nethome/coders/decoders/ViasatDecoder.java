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
public class ViasatDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_HEADER = 1;
	protected static final int READING_HEADER2 = 6;
	protected static final int HI_IN = 2;
	protected static final int HI_BETWEEN = 3;
	protected static final int LO_IN = 4;
	protected static final int LO_BETWEEN = 5;
	
	protected static final int VIA_SHORT = 320;
	protected static final int VIA_LONG = 720;

	protected int m_State = IDLE;
	
	protected int m_Byte1 = 0;
	protected int m_Byte2 = 0;
	protected int m_Byte3 = 0;
	protected int m_Byte4 = 0;
	protected int m_LastWord = 0;
	

	protected int m_LastCommand = 0;
	protected int m_LastAddress = 0;
	protected int m_LastExtra = 0;
		
	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected double m_LastValue = 0;	
	protected ProtocolDecoderSink m_Sink = null;

	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("Viasat", "Diff Manchester", "Viasat", 32, 5);
	}

	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < standard * 0.3;
	}
	
	protected void addBit(int b) {
		if (m_BitCounter < 8) {
			m_Byte1 = (m_Byte1 << 1) + b ;
		} 
		else if (m_BitCounter < 16){
			m_Byte2 = (m_Byte2 << 1) + b ;
		} 
		else if (m_BitCounter < 24) { 
			m_Byte3 = (m_Byte3 << 1) + b ;
		} 
		else if (m_BitCounter < 32) {
			m_Byte4 = (m_Byte4 << 1) + b ;
		}	
			// Check if this is a complete message 
		if (m_BitCounter == 31){
			// It is, create the message
			ProtocolMessage message = new ProtocolMessage("Viasat", m_Byte3, m_Byte4, 4);
			message.setRawMessageByteAt(0, m_Byte1);
			message.setRawMessageByteAt(1, m_Byte2);
			message.setRawMessageByteAt(2, m_Byte3);
			message.setRawMessageByteAt(3, m_Byte4);
			
			message.addField(new FieldValue("Header", m_Byte1));
			message.addField(new FieldValue("Flip", m_Byte2));
			message.addField(new FieldValue("Command", m_Byte3));
			message.addField(new FieldValue("Checksum?", m_Byte4));
			
			// Check if it is a repeat
			int newWord = m_Byte4 << 24 + m_Byte4 << 16 + m_Byte4 << 8 + m_Byte4;
			if ((newWord == m_LastWord) && m_RepeatCount != 0) {
				message.setRepeat(m_RepeatCount);
			}
			else {
				m_RepeatCount = 0;
			}

			// Report the parsed message
			m_Sink.parsedMessage(message);
			// Reset state
			m_State = IDLE;
			// Save message for repeat check
			m_LastWord = newWord;
		}
		m_BitCounter++;
	}
	
	public void parseMan(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (pulseCompare(pulse, 2666.0) && state) {
					m_State = READING_HEADER;
					m_Byte1 = 0;
					m_Byte2 = 0;
					m_Byte3 = 0;
					m_Byte4 = 0;
					m_BitCounter = 0;

					if (pulseCompare(m_LastValue, 75000.0)) {
						m_RepeatCount++;
					}
					else {
						m_RepeatCount = 0;
					}
				}
				break;
			}
			case READING_HEADER: {
				if (pulseCompare(pulse, 1850.0)) {
					m_State = LO_IN;
				}
				else {
					m_Sink.partiallyParsedMessage("Viasat h", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case HI_IN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = LO_BETWEEN;
				} else if (pulseCompare(pulse, VIA_LONG)) {
					m_State = LO_IN;
					addBit(0);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case HI_BETWEEN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = LO_IN;
					addBit(0);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case LO_IN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = HI_BETWEEN;
					break;
				} else if (pulseCompare(pulse, VIA_LONG)) {
					m_State = HI_IN;
					addBit(1);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case LO_BETWEEN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = HI_IN;
					addBit(1);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
		}
		m_LastValue = pulse;
	}

	
	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (pulseCompare(pulse, 2666.0) && state) {
					m_State = READING_HEADER;
					m_Byte1 = 0;
					m_Byte2 = 0;
					m_Byte3 = 0;
					m_Byte4 = 0;
					m_BitCounter = 0;

					if (pulseCompare(m_LastValue, 85000.0)) {
						m_RepeatCount++;
					}
					else {
						m_RepeatCount = 0;
					}
				}
				break;
			}
			case READING_HEADER: {
				if (pulseCompare(pulse, 1850.0)) {
					m_State = HI_BETWEEN;
				}
				else {
					m_Sink.partiallyParsedMessage("Viasat h1", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_HEADER2: {
				if (pulseCompare(pulse, VIA_LONG)) {
					m_State = HI_BETWEEN;
				}
				else {
					m_Sink.partiallyParsedMessage("Viasat h2", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case HI_IN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = LO_BETWEEN;
					addBit(0);
				} else if (pulseCompare(pulse, VIA_LONG)) {
					m_State = LO_IN;
					addBit(1);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case HI_BETWEEN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = LO_IN;
				} else if (pulseCompare(pulse, VIA_LONG)) {
					m_State = LO_BETWEEN;
					addBit(0);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case LO_IN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = HI_BETWEEN;
					addBit(0);
					break;
				} else if (pulseCompare(pulse, VIA_LONG)) {
					m_State = HI_IN;
					addBit(1);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case LO_BETWEEN: {
				if (pulseCompare(pulse, VIA_SHORT)) {
					m_State = HI_IN;
				} else if (pulseCompare(pulse, VIA_LONG)) {
					m_State = HI_BETWEEN;
					addBit(0);
				} else {
					m_Sink.partiallyParsedMessage("Viasat", m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
		}
		m_LastValue = pulse;
        return m_State;
	}
}

