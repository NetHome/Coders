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

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Pronto Encoder takes a message string encoded in the Pronto-format
 * and decodes it into a format used to transmit messages via RF or IR.
 * Messages can be sent by {@link nu.nethome.util.ps.impl.AudioPulsePlayer}.
 * 
 * @author Stefan
 */
@Plugin
public class ProntoEncoder implements ProtocolEncoder{

	/** Constant for the pronto remote's internal clock frequency */
	protected final double m_ProntoFreqConstant = .241246;

	protected String m_Message = "";
	protected int m_RepeatCount = 5;
	
	/**
	 * Extract the modulation frequency in Hz from a Pronto message
	 * @param prontoMessage
	 * @return modulation frequency
	 */
	public int modulationFrequency(Message prontoMessage) {
        if(prontoMessage.getFields().size() != 1 || !prontoMessage.getFields().get(0).getName().equals("Message")) {
            return 0;
        }
		Scanner scanner = new Scanner(prontoMessage.getFields().get(0).getStringValue());
		double period = 10;
		try {
			// Extract type, 0000 for sampled signal. This is the only format we support
			if (scanner.nextInt(16) != 0) return 0;
			// Extract modulation frequency period length
			period = scanner.nextInt(16) * m_ProntoFreqConstant;
			if (period < 10) return 0;
		}
		catch (NoSuchElementException e) {
			return 0;
		}		
		return (int)(1000000 / period);
	}

	/**
	 * Encode the current message as a sequence of pulse lengths. Note that this encodes
	 * both sequence #1 and sequence #2 into the pulse sequence. Use getRepeatPoint() to find out
	 * at what position sequence #2 begins.
	 * @return list of pulse lengths in micro seconds
	 */
	public int[] encode() {
		int fail[] = new int[0];
		int result[];
		Scanner scanner = new Scanner(m_Message);
		try {
			// Extract type, 0000 for sampled signal. This is the only format we support
			if (scanner.nextInt(16) != 0) return fail;
			// Extract modulation frequency period length
			double period = scanner.nextInt(16) * m_ProntoFreqConstant;
			// Number of burst pairs in sequence #1
			int count = scanner.nextInt(16) * 2;
			// Extract number of burst pairs in sequence #2 and add to the total
			count += scanner.nextInt(16) * 2;
			result = new int[count * m_RepeatCount];
			// Extract the burst pairs and convert the time to uS
			for (int i = 0; i < count; i++) {
				result[i] = (int)(period * scanner.nextInt(16) + 0.5);
				// fill in the repeat sequences
				for (int j = 1; j < m_RepeatCount; j++) {
					result[i + (j * count)] = result[i];
				}
			}
		}
		catch (NoSuchElementException e) {
			return fail;
		}
		return result;
	}

	public String getMessage() {
		return m_Message;
	}

	public void setMessage(String message) {
		m_Message = message;
	}

	public int getRepeatCount() {
		return m_RepeatCount;
	}

	public void setRepeatCount(int repeatCount) {
		if ((repeatCount > 0) && (repeatCount < 20)) {
			m_RepeatCount = repeatCount;
		}
	}

	public int getRepeatPoint() {
		Scanner scanner = new Scanner(m_Message);
		try {
			// Extract type, 0000 for sampled signal. This is the only format we support
			if (scanner.nextInt(16) != 0) return 0;
			// Extract modulation frequency period length, and ignore it
			scanner.nextInt(16);
			// Number of burst pairs in sequence #1, this is the repeat point
			return scanner.nextInt(16) * 2;
		}		
		catch (NoSuchElementException e) {
		}
		return 0;
	}

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Pronto", "None", "Pronto", 0, 5);
    }

    public int[] encode(Message message, Phase phase) throws BadMessageException {
        int fail[] = new int[0];
        int result[];
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Message")) {
                setMessage(field.getStringValue());
            } else {
                throw new BadMessageException(field);
            }
        }
        Scanner scanner = new Scanner(m_Message);
        try {
            // Extract type, 0000 for sampled signal. This is the only format we support
            if (scanner.nextInt(16) != 0) return fail;
            // Extract modulation frequency period length
            double period = scanner.nextInt(16) * m_ProntoFreqConstant;
            // Number of burst pairs in sequence #1
            int header = scanner.nextInt(16) * 2;
            // Extract number of burst pairs in sequence #2 and add to the total
            int body = scanner.nextInt(16) * 2;
            int length;
            int skip;
            if (phase == Phase.FIRST) {
                skip = 0;
                length = header;
            } else {
                skip = header;
                length = body;
            }
            result = new int[length];
            // Skip header (if needed)
            for (int i = 0; i < skip; i++) {
                scanner.nextInt(16);
            }

            // Extract the burst pairs and convert the time to uS
            for (int i = 0; i < length; i++) {
                result[i] = (int)(period * scanner.nextInt(16) + 0.5);
            }
        }
        catch (NoSuchElementException e) {
            return fail;
        }
        return result;
    }

    public static Message createMessage(String prontoString) {
        ProtocolMessage result = new ProtocolMessage("Pronto", 0, 0, 0);
        result.addField(new FieldValue("Message", prontoString));
        return result;
    }
}
