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

package nu.nethome.coders.encoders;

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.ProtocolInfo;

/**
 * The Waveman Encoder is an encoder for the RF-protocol used by Waveman products
 * such as remote switches, dimmers, PIR-detectors and so on. The WavemanEncoder
 * can take the data from a protocol message and encode that into a sequence
 * of pulse lengths which may be played by RF-transmitting hardware for instance 
 * via the AudioPulsePlayer.
 * 
 * @author Stefan
 */
@Plugin
public class WavemanEncoder extends NexaEncoder {

	public WavemanEncoder() {
		super();
	}

	/**
	 * The protocol for Waveman is the same as for Nexa, except for the off
	 * command encoding, where all three bits in the command byte are set to 0.
	 * <br>
	 * s = Start bit = 0<br>
	 * b = Button number<br>
	 * a = Address<br>
	 * o = On/Off-bit<br>
	 * <br>
	 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_<br>
	 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0<br>
	 *  x o x o x o x x  x b x b x b x b  x a x a x a x a   s<br>
	 *
	 */
	protected void setup() {
		offCommandValue = 0x00;
		onCommandValue = 0x54 << 17;
	}

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Waveman", "Space Length", "Waveman", 32, 5);
    }
}
