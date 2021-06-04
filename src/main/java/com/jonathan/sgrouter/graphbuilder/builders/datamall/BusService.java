package com.jonathan.sgrouter.graphbuilder.builders.datamall;

public class BusService {
	double[] freq = new double[4]; // AM_off, PM_off, AM_peak, PM_peak

	public BusService(String[] freq) {
		//int numValid = 0;
		//double sumValid = 0;
		for (int i = 0; i < 4; i++) {
			if (validFreq(freq[i])) {
				this.freq[i] = processFreq(freq[i]);
				//numValid++;
				//sumValid += this.freq[i];
			} else
				this.freq[i] = -1;
		}
		/*double avgValid = sumValid / numValid;

		if (numValid == 0)
			for (int i = 0; i < 4; i++)
				this.freq[i] = 10;
		else {
			for (int i = 0; i < 4; i++) {
				if (this.freq[i] == -1) {
					int repIdx = i % 2 == 1 ? i - 1 : i + 1;
					if (this.freq[repIdx] == -1)
						this.freq[i] = avgValid;
					else
						this.freq[i] = this.freq[repIdx];
				}
			}
		}*/
	}

	public double[] getFreqArr(){
		return freq;
	}

	boolean validFreq(String s) {
		return s.matches("(^\\d{1,2})|(^\\d{1,2}-\\d{1,2})");
	}

	double processFreq(String s) {
		String[] startEnd = s.split("-");
		if (startEnd.length == 1)
			return Double.parseDouble(startEnd[0]);
		return (Double.parseDouble(startEnd[0]) + Double.parseDouble(startEnd[1])) / 2;
	}

	@Override
	public String toString() {
		return String.format("%f %f %f %f",freq[0],freq[1],freq[2],freq[3]);
	}
}
