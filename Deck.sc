Deck {
	classvar s;
	var <>bus, <>track, isStopped;
	var fwdRoutine, bwdRoutine, slowRoutine;
	var param, files, ignoreOff;
	var chans, resourceManager;
	var toSend;
	// Stutter/tap tempo variables
	var tapTimes, tapCount, stutterBPM, stutterRate, stutterActive, stutterRoutine, stutterSynth;

	*new { arg refNo, b, c, resMgr;
		^super.new.initDeck(refNo, b, c, resMgr);
	}

	initDeck { arg refNo, b, c, resMgr;
		bus = b;
		("Initializing deck " ++ refNo ++ " with bus: " ++ bus).postln;
		isStopped = true;
		chans = c;
		resourceManager = resMgr;
		toSend = ();
		param = ();
		param[\ref] = refNo;
		param[\pos] = 1;
		param[\cutrate] = 1;
		param[\retrig] = 1;
		param[\cuePos] = 0;
		param[\fineTune] = 0;
		param[\loopEnd] = 0;     // Loop end point (will be set to track end when loaded)
		param[\loopEnabled] = false;
		param[\loopLength] = 1;  // Current loop length multiplier (1, 0.5, 0.25, etc.)

		ignoreOff = Array.fill(6, { true });

		// Initialize tap tempo/stutter
		tapTimes = [];
		tapCount = 0;
		stutterBPM = 120;  // Default BPM
		stutterRate = 0.25;  // Default to 1/4 note
		stutterActive = false;
		stutterRoutine = nil;
		stutterSynth = nil;
	}

	loadTrack { arg path, cp =0;
		DJError.handle({
			("Deck " ++ param[\ref] ++ " loading track: " ++ path).postln;

			if(track.notNil) {
				this.stopTrack();
				track.cleanup();
			};

			track = Track.new(path, bus, param[\ref], chans, resourceManager);
			param[\cuePos] = cp;
			this.resetTapTempo;
		}, "Failed to load track on deck " ++ param[\ref]);
	}

	stopTrack {
		if(track.notNil && isStopped.not) {
			track.stop;
			isStopped = true;
		};
	}

	getVals {
		if(track.notNil) {
			DJError.handle({
				// track.pos.postln
				toSend[\ref] = param[\ref];
				toSend[\pos] = track.pos / 64;
				toSend[\cutrate] = track.param[\cutrate];
				toSend[\retrig] = track.param[\retrig];
				toSend[\cuePos] = (param[\cuePos] + param[\fineTune]) / 64;
				toSend[\rate] = track.param[\rate];
				toSend[\loopEnd] = track.loopEnd / 64;
				toSend[\loopEnabled] = track.loopEnabled;
				toSend[\loopLength] = param[\loopLength];
				// toSend.postln;
				^toSend;
			}, "Failed to get deck values");
		};
		^toSend;
	}

	setLoopEnd { arg pos;
		if(track.notNil) {
			DJError.handle({
				track.setLoopEnd(pos * 64);
			}, "Failed to set loop end");
		};
	}

	setLoopLength { arg multiplier;
		if(track.notNil) {
			DJError.handle({
				param[\loopLength] = multiplier;
				track.setLoopLength(multiplier);
			}, "Failed to set loop length");
		};
	}

	toggleLoop {
		if(track.notNil) {
			DJError.handle({
				param[\loopEnabled] = param[\loopEnabled].not;
				track.toggleLoop;
			}, "Failed to toggle loop");
		};
	}

	setToCuePos {
	var index=4;
	if(ignoreOff[index],{
		DJError.handle({
		//Needs 4 to get past ignoreOff (off/on/off/on)
		this.startstop;
		this.startstop;
		this.startstop;
		this.startstop;
		ignoreOff[index]=false;
		}, "Failed to set to cue point");
	},{ignoreOff[index]=true});
	}

	setCueFromOSC { arg val;
		var index = 5;
		if(ignoreOff[index]) {
			DJError.handle({
				if(track.notNil) {
					["setting cue to ",val*64].postln;
					["fineTune",param[\fineTune]].postln;
					param[\cuePos] = val*64;
					track.setCuePos(param[\cuePos]+param[\fineTune]);
				};
				ignoreOff[index] = false;
			}, "Failed to set cue point");
		} {
			ignoreOff[index] = true;
		};
	}

	setCue { arg val;
		var index = 5;
		if(ignoreOff[index]) {
			DJError.handle({
				if(val == true && track.notNil) {
					param[\cuePos] = track.pos;
					track.setCuePos(param[\cuePos]+param[\fineTune]);
				};
				ignoreOff[index] = false;
			}, "Failed to set cue point");
		} {
			ignoreOff[index] = true;
		};
	}

	fineTuneCue { arg val;
		val = val.clip(-64, 64);
		param[\fineTune] = val;
		if(track.notNil) {
			track.setCuePos(param[\cuePos]+param[\fineTune]);
		};
	}

	skipForward {
		this.stopSkipRoutines();

		fwdRoutine = {
			DJError.handle({
				if(track.notNil) {
					track.skipPitch(1.2);
					1.wait;
					track.skipPitch(1.3);
					3.wait;
					track.skipPitch(3);
				};
			}, "Skip forward failed");
		}.fork;

		if(resourceManager.notNil) {
			resourceManager.registerRoutine(fwdRoutine);
		};
	}

	skipSlow {
		this.stopSkipRoutines();

		slowRoutine = {
			DJError.handle({
				if(track.notNil) {
					"Slowing down".postln;
					track.skipPitch(0.8);
					3.wait;
					track.skipPitch(0.3);
				};
			}, "Skip slow failed");
		}.fork;

		if(resourceManager.notNil) {
			resourceManager.registerRoutine(slowRoutine);
		};
	}

	stopSkipRoutines {
		[fwdRoutine, bwdRoutine, slowRoutine].do { arg routine;
			if(routine.notNil) {
				if(resourceManager.notNil) {
					resourceManager.stopRoutine(routine);
				} {
					routine.stop;
				};
			};
		};
		fwdRoutine = nil;
		bwdRoutine = nil;
		slowRoutine = nil;
	}

	stopSkip {
		this.stopSkipRoutines();
		if(track.notNil) {
			track.setPitch(track.param[\rate]);
		};
	}

	powerDown {
		var dur, index = 2;
		if(ignoreOff[index]) {
			DJError.handle({
				// Disable looping during powerdown
				if(track.notNil && track.loopEnabled) {
					track.toggleLoop;
				};

				dur = 0.6;
				{
					{
						256.do { arg i;
							if(track.notNil) {
								track.skipPitch(track.param[\rate] * (1 - (i/256)));
							};
							(dur/256).wait;
						};
					}.fork;
					dur.wait;
					this.startstop;
				}.fork;
				ignoreOff[index] = false;
			}, "Power down failed");
		} {
			ignoreOff[index] = true;
		};
	}

	backspin {
		DJError.handle({
			if(track.notNil) {
				// Disable looping during backspin
				var wasLooping = track.loopEnabled;
				if(wasLooping) {
					track.toggleLoop;
				};

				{
					var spinupDur = 2.0;  // 300ms to reach max reverse speed
					var slowdownDur = 0.7;  // 700ms to slow down to stop
					var maxReverseSpeed = -6;  // Fast reverse
					var steps;

					// Phase 1: Quick acceleration backwards
					steps = 64;
					steps.do { arg i;
						var progress = i / steps;
						// Exponential curve for more dramatic acceleration
						var speed = progress.squared * maxReverseSpeed;
						track.skipPitch(speed);
						(spinupDur / steps).wait;
					};

					// Phase 2: Deceleration to stop (exponential slowdown)
					steps = 128;
					steps.do { arg i;
						var progress = i / steps;
						// Inverse exponential for quick slowdown
						var speed = maxReverseSpeed * (1 - progress).cubed;
						track.skipPitch(speed);
						(slowdownDur / steps).wait;
					};

					// Stop playback
					this.startstop;
				}.fork;
			};
		}, "Backspin failed");
	}

	startstop {
		var index = 1;
		if(ignoreOff[index]) {
			DJError.handle({
				if(track.isNil) {
					"No track loaded on deck".warn;
					^nil;
				};

				if(isStopped) {
					track.play(param[\cuePos]+param[\fineTune]);
					isStopped = false;
					("Deck " ++ param[\ref] ++ " started").postln;
				} {
					track.stop;
					isStopped = true;
					("Deck " ++ param[\ref] ++ " stopped").postln;
				};
				ignoreOff[index] = false;
			}, "Start/stop failed");
		} {
			ignoreOff[index] = true;
		};
	}

	updatePitch { arg val;
		if(track.notNil) {
			DJError.handle({
				track.setPitch(val);
			}, "Failed to update pitch");
		};
	}

	setVol { arg val;
		if(track.notNil) {
			DJError.handle({
				track.setVol(val);
			}, "Failed to set volume");
		};
	}

	tapTempo {
		DJError.handle({
			var currentTime = Main.elapsedTime;

			if(tapCount < 4) {
				// First 4 taps - record timing
				tapTimes = tapTimes.add(currentTime);
				tapCount = tapCount + 1;
				("Tap " ++ tapCount ++ " recorded").postln;

				if(tapCount == 4) {
					// Calculate average interval between taps
					var intervals = [],avgInterval=0;
					3.do { arg i;
						intervals = intervals.add(tapTimes[i+1] - tapTimes[i]);
					};
					avgInterval = intervals.sum / 3;
					stutterBPM = 60 / avgInterval;  // Convert interval to BPM
					("Tap tempo set: " ++ stutterBPM.round(0.1) ++ " BPM").postln;
				};
			} {
				// After 4 taps, this starts the stutter effect
				this.stutterOn;
			};
		}, "Tap tempo failed");
	}

	resetTapTempo {
		tapTimes = [];
		tapCount = 0;
		("Tap tempo reset").postln;
	}

	setStutterRate { arg rate;
		// Rate should be 1, 0.5, 0.25, 0.125, etc (1, 1/2, 1/4, 1/8...)
		stutterRate = rate;
		("Stutter rate set to " ++ rate).postln;
	}

	stutterOn {
		DJError.handle({
			if(track.notNil && track.loadedBuffer.notNil && tapCount >= 4 && stutterActive.not) {
				var stutterInterval = (60 / stutterBPM) * stutterRate;  // Time per stutter in seconds
				var stutterPos, stutterSamplePos;
				var synthName = if(chans == 4, \playTrack4, \playTrack2);

				stutterActive = true;

				// Capture the current position for stuttering
				stutterPos = track.pos;
				stutterSamplePos = (stutterPos * 44100) / 64;

				// Mute the original playback (it continues underneath)
				track.setVol(0);

				// Create a separate synth for stutter playback
				stutterSynth = Synth(synthName, [
					\bufnum, track.loadedBuffer,
					\startPos, stutterSamplePos,
					\amp, param[\amp] ? 1,
					\bus, bus,
					\rate, track.param[\rate],
					\deck, param[\ref],
					\loop, 0
				]);

				if(resourceManager.notNil) {
					resourceManager.registerSynth(stutterSynth);
				};

				// Start stutter routine - retrigger the stutter synth
				stutterRoutine = {
					while { stutterActive } {
						stutterInterval.wait;
						// Retrigger the stutter synth at the captured position
						if(stutterSynth.notNil) {
							stutterSynth.set(\t_trig, 1, \startPos, stutterSamplePos);
						};
					};
				}.fork;

				if(resourceManager.notNil) {
					resourceManager.registerRoutine(stutterRoutine);
				};

				("Stutter ON - interval: " ++ stutterInterval.round(0.001) ++ "s").postln;
			};
		}, "Stutter on failed");
	}

	stutterOff {
		DJError.handle({
			if(stutterActive) {
				stutterActive = false;

				// Stop stutter routine
				if(stutterRoutine.notNil) {
					stutterRoutine.stop;
					stutterRoutine = nil;
				};

				// Free the stutter synth
				if(stutterSynth.notNil) {
					stutterSynth.free;
					stutterSynth = nil;
				};

				// Restore original volume
				if(track.notNil) {
					track.setVol(param[\amp] ? 1);
				};

				("Stutter OFF").postln;
			};
		}, "Stutter off failed");
	}

	cleanup {
		this.stopSkipRoutines();
		this.stutterOff;
		if(track.notNil) {
			track.cleanup();
			track = nil;
		};
	}
}