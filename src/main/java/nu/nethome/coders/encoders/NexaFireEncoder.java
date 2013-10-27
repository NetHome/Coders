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

import nu.nethome.coders.decoders.NexaFireDecoder;
import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.ArrayList;

/**
 * The NexaFireEncoder is an encoder for the RF-protocol used by NEXA Fire Detectors 
 * with RF connection. The NexaFireEncoder can take the data from a protocol message 
 * and encode that into a sequence of pulse lengths which may be played by 
 * RF-transmitting hardware for instance via the AudioPulsePlayer.
 * 
 * @author Stefan
 *
 */
@Plugin
public class NexaFireEncoder implements ProtocolEncoder {
	
	public int m_RepeatCount = 15;
	private int m_Address = 0;
	
	// Get the pulse lengths from the decoder
	protected int HEADER_MARK = NexaFireDecoder.NEXAF_HEADER_MARK.length();
	protected int HEADER_SPACE = NexaFireDecoder.NEXAF_HEADER_SPACE.length();
	protected int MARK = NexaFireDecoder.NEXAF_MARK.length();
	protected int LONG_SPACE = NexaFireDecoder.NEXAF_LONG_SPACE.length();
	protected int SHORT_SPACE = NexaFireDecoder.NEXAF_SHORT_SPACE.length();
	protected int REPEAT = NexaFireDecoder.NEXAF_REPEAT.length();

	public NexaFireEncoder() {
		setup();
	}
	
	/**
	 * Template method pattern. This is called by the constructor in order to be able to let
	 * subclasses change pulse lengths and other parameters 
	 */
	protected void setup() {
	}

	/**
	 * Encodes the current message according to the following protocol:
	 * 
	 * a = Address<br>
	 * <br>
	 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____<br>
	 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0<br>
	 *  a a a a a a a a  a a a a a a a a  a a a a a a a a<br>
	 */
	public int[] encode() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		long message = 0;
		
		// Start actually encoding the data pulses
		for (int i = 0; i < m_RepeatCount; i++) {
			message = m_Address;
			
			// Encode header
			result.add(HEADER_MARK);
			result.add(HEADER_SPACE);
			
			// 24 bits of data
			for (int j = 0; j < 24; j++) {
				result.add(MARK);
				if ((message & 1) == 1) {
					result.add(LONG_SPACE);
				}
				else {
					result.add(SHORT_SPACE);
				}
				message >>= 1;
			}
			// Add trailing mark and repeat space
			result.add(MARK);
			result.add(REPEAT);
		}
		int resultArray[] = new int[result.size()];
		for (int i = 0; i < result.size(); i++) {
			resultArray[i] = result.get(i);
		}
		return resultArray;
	}

	/**
	 * @return the Address
	 */
	public int getAddress() {
		return m_Address;
	}

	/**
	 * @param address the Address to set
	 */
	public void setAddress(int address) {
		if (address > 0xFFFFFF) return;
		this.m_Address = address;
	}

	public int getRepeatCount() {
		return m_RepeatCount;
	}

	public void setRepeatCount(int repeatCount) {
		if ((repeatCount < 1) || (repeatCount > 100)) {
			return;
		}
		this.m_RepeatCount = repeatCount;
	}

    @Override
    public ProtocolInfo getInfo() {
        return new ProtocolInfo("NexaFire", "Space Length", "NexaFire", 24, 15);
    }

    @Override
    public int[] encode(Message message, Phase phase) throws BadMessageException {
        if (Phase.FIRST == phase) {
            return new int[0];
        }
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Address")) {
                setAddress(field.getValue());
            } else {
                throw new BadMessageException(field);
            }
        }
        setRepeatCount(1);
        return encode();
    }

    @Override
    public int modulationFrequency(Message message) {
        return 0;
    }
}

