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

package nu.nethome.coders.decoders.util;

import nu.nethome.coders.decoders.*;
import nu.nethome.util.ps.FieldValue;
import nu.nethome.util.ps.ProtocolDecoderSink;
import nu.nethome.util.ps.ProtocolMessage;
import nu.nethome.util.ps.RawProtocolMessage;
import nu.nethome.util.ps.impl.FIRFilter6000;
import nu.nethome.util.ps.impl.ProtocolDecoderGroup;
import nu.nethome.util.ps.impl.SimpleFlankDetector;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class JirFileTestPlayer implements ProtocolDecoderSink{
	
	public static final int SIRC_DECODER = 1 << 0;
	public static final int RC6_DECODER = 1 << 1;
	public static final int RC5_DECODER = 1 << 2;
	public static final int JVC_DECODER = 1 << 3;
	public static final int Viasat_DECODER = 1 << 4;
	public static final int Pioneer_DECODER = 1 << 5;
	public static final int HK_DECODER = 1 << 6;
	public static final int UPM_DECODER = 1 << 7;
	public static final int Nexa_DECODER = 1 << 8;
	public static final int NexaL_DECODER = 1 << 9;
	public static final int Deltronic_DECODER = 1 << 10;
	public static final int X10_DECODER = 1 << 11;
	public static final int Waveman_DECODER = 1 << 12;
	public static final int NexaFire_DECODER = 1 << 13;
	public static final int OREGON_DECODER = 1 << 14;
	public static final int FINE_OFFSET_DECODER = 1 << 15;
	public static final int ROLLERTROL_DECODER = 1 << 16;
	public static final int ROLLERTROL_G_DECODER = 1 << 17;
	public static final int PROLOGUE_DECODER = 1 << 18;

	public static final int ALL_DECODERS = 0x7FFFFFFF;


	public SimpleFlankDetector m_FlankDetector;
	public ProtocolDecoderGroup m_ProtocolDecoders = new ProtocolDecoderGroup();
	public FIRFilter6000 m_Filter;
	
	public ArrayList<ProtocolMessage> m_Messages = new ArrayList<ProtocolMessage>();
	public boolean m_PartlyParsedMessage = false;
	
	public JirFileTestPlayer(int decoders) {
		
		// Create the Protocol-Decoders and add them to the decoder group
		if ((decoders & SIRC_DECODER) != 0) m_ProtocolDecoders.add(new SIRCDecoder());
		if ((decoders & RC6_DECODER) != 0) m_ProtocolDecoders.add(new RC6Decoder());
		if ((decoders & RC5_DECODER) != 0) m_ProtocolDecoders.add(new RC5Decoder());
		if ((decoders & JVC_DECODER) != 0) m_ProtocolDecoders.add(new JVCDecoder());
		if ((decoders & Viasat_DECODER) != 0) m_ProtocolDecoders.add(new ViasatDecoder());
		if ((decoders & Pioneer_DECODER) != 0) m_ProtocolDecoders.add(new PioneerDecoder());
		if ((decoders & HK_DECODER) != 0) m_ProtocolDecoders.add(new HKDecoder());
		if ((decoders & UPM_DECODER) != 0) m_ProtocolDecoders.add(new UPMDecoder());
		if ((decoders & Nexa_DECODER) != 0) m_ProtocolDecoders.add(new NexaDecoder());
		if ((decoders & NexaL_DECODER) != 0) m_ProtocolDecoders.add(new NexaLDecoder());
		if ((decoders & Deltronic_DECODER) != 0) m_ProtocolDecoders.add(new DeltronicDecoder());
		if ((decoders & X10_DECODER) != 0) m_ProtocolDecoders.add(new X10Decoder());
		if ((decoders & Waveman_DECODER) != 0) m_ProtocolDecoders.add(new WavemanDecoder());
		if ((decoders & NexaFire_DECODER) != 0) m_ProtocolDecoders.add(new NexaFireDecoder());
		if ((decoders & OREGON_DECODER) != 0) m_ProtocolDecoders.add(new OregonDecoder());
		if ((decoders & FINE_OFFSET_DECODER) != 0) m_ProtocolDecoders.add(new FineOffsetDecoder());
		if ((decoders & ROLLERTROL_DECODER) != 0) m_ProtocolDecoders.add(new RollerTrolDecoder());
		if ((decoders & ROLLERTROL_G_DECODER) != 0) m_ProtocolDecoders.add(new RollerTrolGDecoder());
		if ((decoders & PROLOGUE_DECODER) != 0) m_ProtocolDecoders.add(new PrologueDecoder());

        // Set the Sink - which is this class
        m_ProtocolDecoders.setTarget(this);

		// Create The Flank Detector and attach the decoders
		m_FlankDetector = new SimpleFlankDetector();
		m_FlankDetector.setProtocolDecoder(m_ProtocolDecoders);
		
		// Create the FIR-Filter and attach to the samplers
		m_Filter = new FIRFilter6000(m_FlankDetector);
		
		m_FlankDetector.setSampleRate(44100);
		
	}

	public void playFile(InputStream fileStream) {
		ObjectInputStream ois;
		
		// Add a quiet period (200ms) before signal
		int numberOfEndSamples = m_FlankDetector.getSampleRate() / 5;
		for (int i = 0; i < numberOfEndSamples; i++) {
			m_FlankDetector.addSample(0);
		}

		int lastSample = 0;
		try {
			ois = new ObjectInputStream(fileStream);

			int length = ois.readInt();
			for (int i = 0; i < length; i++) {
				ProtocolMessage irm = (ProtocolMessage) ois.readObject();
				// Check that it is a raw sample
				if (irm.getProtocol().equals("Raw")) {
					RawProtocolMessage mess = (RawProtocolMessage) irm;
					for (int sample : mess.m_Samples) {
						// Push sample into decoders
						m_FlankDetector.addSample(sample);
						lastSample = sample;
					}
				}

			}
			ois.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		// Add a quiet period (200ms)to end detection
		numberOfEndSamples = m_FlankDetector.getSampleRate() / 5;
		for (int i = 0; i < numberOfEndSamples; i++) {
			m_FlankDetector.addSample(lastSample);
		}


	}
	
	public int getMessageField(int messageNumber, String fieldName) {
		if (messageNumber >= m_Messages.size()) {
			return -1;
		}
		List<FieldValue> fields = m_Messages.get(messageNumber).getFields();
		for (FieldValue field : fields) {
			if (fieldName.equals(field.getName())) {
				return field.getValue();
			}
		}
		return -1;
	}

	public String getMessageFieldString(int messageNumber, String fieldName) {
		if (messageNumber >= m_Messages.size()) {
			return "";
		}
		List<FieldValue> fields = m_Messages.get(messageNumber).getFields();
		for (FieldValue field : fields) {
			if (fieldName.equals(field.getName()) && (field.getStringValue() != null)) {
				return field.getStringValue();
			}
		}
		return "";
	}

	public void parsedMessage(ProtocolMessage message) {
		m_Messages.add(message);
	}

	public void partiallyParsedMessage(String protocol, int bits) {
		m_PartlyParsedMessage  = true;
	}

	public void reportLevel(int level) {
		// Nothing to do
	}

}
