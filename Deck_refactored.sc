Deck_refactored {
	classvar s;
	var <>bus, <>track, isStopped, cuePos;
	var fwdRoutine, bwdRoutine, slowRoutine;
	var ref, fineTune = 0, param, files, ignoreOff;
	var chans, resourceManager;

	*new { arg refNo, b, c, resMgr;
		^super.new.initDeck(refNo, b, c, resMgr);
	}

	initDeck { arg refNo, b, c, resMgr;
		bus = b;
		("Initializing deck " ++ refNo ++ " with bus: " ++ bus).postln;
		ref = refNo;
		isStopped = true;
		cuePos = 0;
		chans = c;
		resourceManager = resMgr;

		param = ();
		param[\ref] = ref;
		param[\pos] = 1;
		param[\cutrate] = 1;
		param[\retrig] = 1;
		param[\cuePos] = 1;
		param[\loopEnd] = 0;     // Loop end point (will be set to track end when loaded)
		param[\loopEnabled] = false;
		param[\loopLength] = 1;  // Current loop length multiplier (1, 0.5, 0.25, etc.)

		ignoreOff = Array.fill(6, { true });
	}

	loadTrack { arg path;
		DJError.handle({
			("Deck " ++ ref ++ " loading track: " ++ path).postln;

			if(track.notNil) {
				this.stopTrack();
				track.cleanup();
			};

			track = Track_refactored.new(path, bus, ref, chans, resourceManager);
			cuePos = 0;
			fineTune = 0;
		}, "Failed to load track on deck " ++ ref);
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
				// track.pos.postln;
				param[\pos] = track.pos / 64;
				param[\cutrate] = track.param[\cutrate];
				param[\retrig] = track.param[\retrig];
				param[\cuePos] = (cuePos + fineTune) / 64;
				param[\rate] = track.param[\rate];
				param[\loopEnd] = track.loopEnd / 64;
				param[\loopEnabled] = track.loopEnabled;
			}, "Failed to get deck values");
		};
		^param;
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
					cuePos = val*64;
					track.setCuePos(cuePos);
				} {
					cuePos = 0;
				};
				fineTune = 0;
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
					cuePos = track.pos;
					track.setCuePos(cuePos);
				} {
					cuePos = 0;
				};
				fineTune = 0;
				ignoreOff[index] = false;
			}, "Failed to set cue point");
		} {
			ignoreOff[index] = true;
		};
	}

	fineTuneCue { arg val;
		fineTune = val.clip(-64, 64);
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

	skipBackwards {
		this.stopSkipRoutines();

		bwdRoutine = {
			DJError.handle({
				if(track.notNil) {
					track.skipPitch(-0.5);
					0.5.wait;
					track.skipPitch(-1);
					1.wait;
					track.skipPitch(-2);
					1.wait;
					track.skipPitch(-3);
					3.wait;
					track.skipPitch(-6);
				};
			}, "Skip backwards failed");
		}.fork;

		if(resourceManager.notNil) {
			resourceManager.registerRoutine(bwdRoutine);
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

	startstop {
		var index = 1;
		if(ignoreOff[index]) {
			DJError.handle({
				if(track.isNil) {
					"No track loaded on deck".warn;
					^nil;
				};

				if(isStopped) {
					track.play(cuePos + fineTune);
					isStopped = false;
					("Deck " ++ ref ++ " started").postln;
				} {
					track.stop;
					isStopped = true;
					("Deck " ++ ref ++ " stopped").postln;
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

	cleanup {
		this.stopSkipRoutines();
		if(track.notNil) {
			track.cleanup();
			track = nil;
		};
	}
}