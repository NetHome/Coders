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

import java.util.ArrayList;

/**
 * Encodes a signal that when played with an AudioPulseTransmitter will be heard 
 * as a short beep if speakers are attached to the audio output used. This is
 * used for testing setup and configuration and should NOT be used when an
 * RF-Transmitting device is attached to the output.
 * 
 * @author Stefan
 */
public class ShortBeepEncoder {
	
	private static final float MICROSECS_PER_SEC = 1000000;
	private int m_Frequency = 2000;
	private float m_Duration = 0.8F;
	private ArrayList<Integer> result = new ArrayList<Integer>();

	/**
	 * Encode a pulse set which will be heard as a short beep if played by
	 * an AudioProtocolTransmitter.
	 * @return
	 */
	public int[] encode() {
		int noFlanks = (int)(m_Duration * MICROSECS_PER_SEC * 2F) / m_Frequency;
		int flank = ((int)MICROSECS_PER_SEC / m_Frequency) / 2;
		for (int i = 0; i < noFlanks; i++) {
			result.add(flank);
		}
		int resultArray[] = new int[result.size()];
		for (int i = 0; i < result.size(); i++) {
			resultArray[i] = result.get(i);
		}
		return resultArray;
	}

	public int getFrequency() {
		return m_Frequency;
	}

	/**
	 * @param frequency Frequency of the beep,1 - 18000 Hz
	 */
	public void setFrequency(int frequency) {
		if ((frequency < 0) || (frequency > 18000)) {
			throw new IllegalArgumentException("Bad frequence value");
		}
		m_Frequency = frequency;
	}

	public float getDuration() {
		return m_Duration;
	}

	/**
	 * @param duration Duration of the Beep, 0 - 1 Second
	 */
	public void setDuration(float duration) {
		if ((duration < 0) || (duration > 1)) {
			throw new IllegalArgumentException("Bad duration value");
		}
		m_Duration = duration;
	}

}
