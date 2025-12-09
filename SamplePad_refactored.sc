SamplePad_refactored {
	var samples, buffers, synths;
	var resourceManager;
	var maxSamples = 8;

	*new { arg resMgr;
		^super.new.initSamplePad(resMgr);
	}

	*initSynthDefs {
		SynthDef(\samplePlayer, { arg bufnum, amp=0.5, rate=1, startPos=0, out=0;
			var sig = PlayBuf.ar(
				1,
				bufnum,
				BufRateScale.kr(bufnum) * rate,
				1,
				startPos,
				0,
				2
			);
			Out.ar(out, sig * amp);
		}).store;

		SynthDef(\samplePlayerStereo, { arg bufnum, amp=0.5, rate=1, startPos=0, out=0;
			var sig = PlayBuf.ar(
				2,
				bufnum,
				BufRateScale.kr(bufnum) * rate,
				1,
				startPos,
				0,
				2
			);
			Out.ar(out, sig * amp);
		}).store;
	}

	initSamplePad { arg resMgr;
		resourceManager = resMgr;
		samples = Array.newClear(maxSamples);
		buffers = Array.newClear(maxSamples);
		synths = Array.newClear(maxSamples);

		"SamplePad initialized".postln;
	}

	loadSample { arg index, path;
		DJError.handle({
			DJError.assert(
				index >= 0 && index < maxSamples,
				"Sample index out of range: " ++ index
			);

			DJError.checkFile(path);

			if(buffers[index].notNil) {
				this.clearSample(index);
			};

			buffers[index] = Buffer.read(Server.default, path, action: { arg buf;
				samples[index] = (path: path, buffer: buf);
				("Sample loaded at index " ++ index ++ ": " ++ path).postln;
			});

			if(resourceManager.notNil) {
				resourceManager.registerBuffer(buffers[index]);
			};
		}, "Failed to load sample: " ++ path);
	}

	playSample { arg index, amp = 0.5, rate = 1;
		var numChannels = buffers[index].numChannels;
		var synthName = if(numChannels == 2, \samplePlayerStereo, \samplePlayer);
		DJError.handle({
			DJError.assert(
				index >= 0 && index < maxSamples,
				"Sample index out of range: " ++ index
			);

			if(buffers[index].isNil) {
				("No sample loaded at index " ++ index).warn;
				^nil;
			};

			this.stopSample(index);



			synths[index] = Synth(synthName, [
				\bufnum, buffers[index],
				\amp, amp.clip(0, 1),
				\rate, rate.clip(0.25, 4),
				\out, 0
			]);

			if(resourceManager.notNil) {
				resourceManager.registerSynth(synths[index]);
			};

			("Playing sample " ++ index).postln;
		}, "Failed to play sample at index " ++ index);
	}

	stopSample { arg index;
		DJError.handle({
			if(synths[index].notNil) {
				if(resourceManager.notNil) {
					resourceManager.freeSynth(synths[index]);
				} {
					synths[index].free;
				};
				synths[index] = nil;
			};
		}, "Failed to stop sample at index " ++ index);
	}

	clearSample { arg index;
		DJError.handle({
			this.stopSample(index);

			if(buffers[index].notNil) {
				if(resourceManager.notNil) {
					resourceManager.freeBuffer(buffers[index]);
				} {
					buffers[index].free;
				};
				buffers[index] = nil;
				samples[index] = nil;
			};
		}, "Failed to clear sample at index " ++ index);
	}

	cleanup {
		maxSamples.do { arg i;
			this.clearSample(i);
		};
		"SamplePad cleaned up".postln;
	}
}