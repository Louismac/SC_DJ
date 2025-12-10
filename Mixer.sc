Mixer {
	classvar s;
	var buses, mainOut, cueOut, param, <>mainMix, eqBus, echoBus, channels;
	var resourceManager;

	*new { arg d, c, resMgr;
		^super.new.initMixer(d, c, resMgr);
	}

	*initSynthDefs {
		// Main output with meters (mono)
		SynthDef(\mixOut2Metered, { arg out, bus1, bus2, xfade=0, sampleBus;
			var input1, input2, tracks, samples, output;
			input1 = InFeedback.ar(bus1, 1);
			input2 = InFeedback.ar(bus2, 1);
			tracks = XFade2.ar(input1, input2, xfade);

			samples = InFeedback.ar(sampleBus, 1);
			output = XFade2.ar(tracks, samples, 0);

			// Send meter data for each deck and main mix
			SendPeakRMS.kr(input1, 20, 3, '/meter', replyID: 0);
			SendPeakRMS.kr(input2, 20, 3, '/meter', replyID: 1);
			SendPeakRMS.kr(output, 20, 3, '/meter', replyID: 2);

			Out.ar(out, Limiter.ar(output, 0.99, 0.01));
		}).store;

		// Cue output without meters (mono)
		SynthDef(\mixOut2, { arg out, bus1, bus2, xfade=0, sampleBus;
			var input1, input2, tracks, samples, output;
			input1 = InFeedback.ar(bus1, 1);
			input2 = InFeedback.ar(bus2, 1);
			tracks = XFade2.ar(input1, input2, xfade);

			samples = InFeedback.ar(sampleBus, 1);
			output = XFade2.ar(tracks, samples, 0);

			Out.ar(out, Limiter.ar(output, 0.99, 0.01));
		}).store;

		// Main output with meters (stereo)
		SynthDef(\mixOut4Metered, { arg out, bus1, bus2, xfade=0, sampleBus;
			var input1, input2, tracks, samples, output;
			input1 = InFeedback.ar(bus1, 2);
			input2 = InFeedback.ar(bus2, 2);
			tracks = XFade2.ar(input1, input2, xfade);

			samples = InFeedback.ar(sampleBus, 2);
			output = XFade2.ar(tracks, samples, 0);

			// Send meter data for each deck and main mix
			SendPeakRMS.kr(input1, 20, 3, '/meter', replyID: 0);
			SendPeakRMS.kr(input2, 20, 3, '/meter', replyID: 1);
			SendPeakRMS.kr(output, 20, 3, '/meter', replyID: 2);

			Out.ar(out, Limiter.ar(output, 0.99, 0.01));
		}).store;

		// Cue output without meters (stereo)
		SynthDef(\mixOut4, { arg out, bus1, bus2, xfade=0, sampleBus;
			var input1, input2, tracks, samples, output;
			input1 = InFeedback.ar(bus1, 2);
			input2 = InFeedback.ar(bus2, 2);
			tracks = XFade2.ar(input1, input2, xfade);

			samples = InFeedback.ar(sampleBus, 2);
			output = XFade2.ar(tracks, samples, 0);

			Out.ar(out, Limiter.ar(output, 0.99, 0.01));
		}).store;

		SynthDef(\echo2, { arg outBus, inBus, echoOn=0;
			var input, echo, output;
			input = InFeedback.ar(inBus, 1);

			// Echo effect with feedback
			echo = CombC.ar(input, 2.0, 0.375, 3.0);  // ~375ms delay, 3s decay

			// Mix dry and wet based on echoOn (0 = dry only, 1 = with echo)
			output = XFade2.ar(input, echo, echoOn * 2 - 1);

			Out.ar(outBus, output);
		}).store;

		SynthDef(\echo4, { arg outBus, inBus, echoOn=0;
			var input, echo, output;
			input = InFeedback.ar(inBus, 2);

			// Echo effect with feedback
			echo = CombC.ar(input, 2.0, 0.375, 3.0);  // ~375ms delay, 3s decay

			// Mix dry and wet based on echoOn (0 = dry only, 1 = with echo)
			output = XFade2.ar(input, echo, echoOn * 2 - 1);

			Out.ar(outBus, output);
		}).store;

		SynthDef(\eqMixer2, { arg outBus, inBus, hiVal=0.5, midVal=0.5, loVal=0.5;
			var input, lo, mid, hi, loAmp, midAmp, hiAmp, output;
			input = InFeedback.ar(inBus, 1);

			// DJ-style EQ: 0 = kill, 0.5 = neutral (flat), 1 = boost
			// Split into 3 bands using crossover filters
			lo = LPF.ar(input, 300);
			mid = BPF.ar(input, 1500, 2);
			hi = HPF.ar(input, 4000);

			// Map 0->1 to 0->2 (linear amplitude: 0=kill, 0.5=neutral, 1=boost)
			loAmp = loVal * 2;
			midAmp = midVal * 2;
			hiAmp = hiVal * 2;

			// Mix bands back together with amplitude control
			output = (lo * loAmp) + (mid * midAmp) + (hi * hiAmp);

			Out.ar(outBus, Limiter.ar(output, 1, 0.005));
		}).store;

		SynthDef(\eqMixer4, { arg outBus, inBus, hiVal=0.5, dj=0.5, loVal=0.5;
			var input, output, lo, mid, hi, loAmp, midAmp, hiAmp;
			var lpfCutoffFreq = dj.linexp(0, 0.5, 20, 10000);
			var hpfCutoffFreq = dj.linexp(0.5, 1, 20, 10000);
			input = InFeedback.ar(inBus, 2);
			lo = LPF.ar(input, 300);
			mid = BPF.ar(input, 1500, 2);
			hi = HPF.ar(input, 4000);
			loAmp = loVal * 2;
			midAmp = 1;
			hiAmp = hiVal * 2;

			// Mix bands back together with amplitude control
			output = (lo * loAmp) + (mid * midAmp) + (hi * hiAmp);
			output = RHPF.ar(
			RLPF.ar(
				output,
				lpfCutoffFreq
				),
				hpfCutoffFreq
			);


			Out.ar(outBus, Limiter.ar(output, 1, 0.005));
		}).store;
	}

	initMixer { arg b, c, resMgr;
		DJError.handle({
			Mixer.initSynthDefs();
			channels = c;
			buses = b;
			resourceManager = resMgr;
			param = [0, 0, 1, 1];
			mainMix = 0;
			s = Server.default;
			echoBus = [[nil, nil], [nil, nil]];  // [bus, synth] for each deck
			eqBus = [[nil, nil], [nil, nil]];

			("Initializing mixer with " ++ channels ++ " channels").postln;

			Server.default.sync;

			this.setupEchoBuses;
			this.setupEQBuses;
			this.setupMixerSynths;

		}, "Failed to initialize mixer");
	}

	setupEchoBuses {
		DJError.handle({
			var numChannels = if(channels == 4, 2, 1);
			var echoSynthName = if(channels == 4, \echo4, \echo2);

			2.do { arg i;
				echoBus[i][0] = Bus.audio(s, numChannels);

				if(resourceManager.notNil) {
					resourceManager.registerBus(echoBus[i][0]);
				};

				Server.default.sync;

				echoBus[i][1] = Synth(echoSynthName, [
					\outBus, echoBus[i][0],
					\inBus, buses[i.asSymbol],
					\echoOn, 0
				]);

				if(resourceManager.notNil) {
					resourceManager.registerSynth(echoBus[i][1]);
				};

				("Echo bus " ++ i ++ " created").postln;
			};
		}, "Failed to setup echo buses");
	}

	setupEQBuses {
		DJError.handle({
			var numChannels = if(channels == 4, 2, 1);
			var eqSynthName = if(channels == 4, \eqMixer4, \eqMixer2);

			2.do { arg i;
				eqBus[i][0] = Bus.audio(s, numChannels);

				if(resourceManager.notNil) {
					resourceManager.registerBus(eqBus[i][0]);
				};

				Server.default.sync;

				eqBus[i][1] = Synth(eqSynthName, [
					\outBus, eqBus[i][0],
					\inBus, echoBus[i][0],  // Read from echo bus instead of deck bus
					\hiVal, 0.5,
					\midVal, 0.5,
					\loVal, 0.5
				]);

				if(resourceManager.notNil) {
					resourceManager.registerSynth(eqBus[i][1]);
				};

				("EQ bus " ++ i ++ " created").postln;
			};
		}, "Failed to setup EQ buses");
	}

	setupMixerSynths {
		DJError.handle({

			var mainSynthName = if(channels == 4, \mixOut4Metered, \mixOut2Metered);
			var cueSynthName = if(channels == 4, \mixOut4, \mixOut2);
			var mainOutChannel = if(channels == 4, [0, 1], 1);
			var cueOutChannel = if(channels == 4, [2, 3], 0);
			Server.default.sync;

			// Main output with meters
			mainOut = Synth(mainSynthName, [
				\out, mainOutChannel,
				\bus1, eqBus[0][0],
				\bus2, eqBus[1][0],
				\sampleBus, buses[\sample],
				\xfade, 0
			]);

			if(resourceManager.notNil) {
				resourceManager.registerSynth(mainOut);
			};

			// Cue output without meters
			cueOut = Synth(cueSynthName, [
				\out, cueOutChannel,
				\bus1, eqBus[0][0],
				\bus2, eqBus[1][0],
				\sampleBus, buses[\sample],
				\xfade, 0
			]);

			if(resourceManager.notNil) {
				resourceManager.registerSynth(cueOut);
			};

			"Mixer synths created".postln;
		}, "Failed to setup mixer synths");
	}

	updateMix { arg xfade;
		DJError.handle({
			xfade = xfade.clip(-1, 1);
			if(mainOut.notNil) {
				mainOut.set(\xfade, xfade);
				mainMix = xfade;
			};
		}, "Failed to update mix");
	}

	updateCueMix { arg xfade;
		DJError.handle({
			xfade = xfade.clip(-1, 1);
			if(cueOut.notNil) {
				cueOut.set(\xfade, xfade);
			};
		}, "Failed to update cue mix");
	}

	updateLo { arg chan, val;
		DJError.handle({
			chan = chan.clip(0, 1);
			val = val.clip(0, 1);  // DJ-style: 0 = kill, 0.5 = neutral, 1 = boost
			if(eqBus[chan][1].notNil) {
				eqBus[chan][1].set(\loVal, val);
			};
		}, "Failed to update low EQ");
	}

	updateDJ { arg chan, val;
		DJError.handle({
			chan = chan.clip(0, 1);
			val = val.clip(0, 1);  // DJ-style: 0 = kill, 0.5 = neutral, 1 = boost
			if(eqBus[chan][1].notNil) {
				eqBus[chan][1].set(\dj, val);
			};
		}, "Failed to update mid EQ");
	}

	updateHi { arg chan, val;
		DJError.handle({
			chan = chan.clip(0, 1);
			val = val.clip(0, 1);  // DJ-style: 0 = kill, 0.5 = neutral, 1 = boost
			if(eqBus[chan][1].notNil) {
				eqBus[chan][1].set(\hiVal, val);
			};
		}, "Failed to update high EQ");
	}

	toggleEcho { arg chan;
		DJError.handle({
			chan = chan.clip(0, 1);
			if(echoBus[chan][1].notNil) {
				// Get current echo state and toggle it
				echoBus[chan][1].get(\echoOn, { arg val;
					var newVal = if(val == 0, 1, 0);
					echoBus[chan][1].set(\echoOn, newVal);
					("Echo " ++ chan ++ " set to: " ++ newVal).postln;
				});
			};
		}, "Failed to toggle echo");
	}

	cleanup {
		"Cleaning up mixer...".postln;
	}
}