Track_refactored {
	var <>pos, tempo, <>param, buf, posRoutine, bus, <>loadedBuffer, deckRef, loop, muted;
	var trigLength, <>trigBuf, trigRoutine, trigTime, trigVals, trig, trigCtr;
	var <>rateBuf, <>rate, rateVals;
	var chans, resourceManager;
	var <>loopEnd, <>loopEnabled, loopLengthMultiplier, cuePos, originalLoopEnd;
	classvar s;

	*new { arg path, recbus, ref, c, resMgr;
		^super.new.initTrack(path, recbus, ref, c, resMgr);
	}

	*initSynthDefs {
		SynthDef(\playTrack4, { arg bufnum=0, amp=1, length=1, panPot=0, rate=1, t_trig, startPos=0, loop=0, bus=nil, deck=0;
			var env, sound, ampTrig;
			env = EnvGen.ar(Env([1,1], [(BufFrames.kr(bufnum)/44100)*length]), levelScale:amp);
			ampTrig = Impulse.kr(10);
			sound = Pan2.ar(
				PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos, loop),
				panPot
			) * amp;
			SendReply.kr(ampTrig, 'amp', [
				Amplitude.kr(
					PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos, loop) * amp
				),
				deck
			]);
			Out.ar(bus, sound * env);
		}).store;

		SynthDef(\playTrack2, { arg bufnum=0, amp=1, length=1, rate=1, t_trig, startPos=0, loop=0, bus=nil, deck=0;
			var env, sound, ampTrig;
			env = EnvGen.ar(Env([1,1], [(BufFrames.kr(bufnum)/44100)*length]), levelScale:amp);
			ampTrig = Impulse.kr(10);
			sound = PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos, loop) * amp;
			SendReply.kr(ampTrig, 'amp', [
				Amplitude.kr(
					PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos, loop) * amp
				),
				deck
			]);
			Out.ar(bus, sound * env);
		}).store;
	}

	initTrack { arg path, recbus, ref, c, resMgr;
		DJError.handle({
			("Loading track: " ++ path).postln;

			// SynthDefs are initialized once by the main DJ class

			param = ();
			param[\amp] = 1;
			param[\retrig] = 1;
			param[\cutrate] = 1;
			param[\rate] = 1;

			chans = c;
			deckRef = ref;
			resourceManager = resMgr;
			muted = false;
			loop = 0;
			cuePos = 0;
			loopEnd = 0;  // Will be set to track length after buffer loads
			originalLoopEnd = 0;  // Stores the full loop length for fractioning
			loopEnabled = false;
			loopLengthMultiplier = 1;
			s = Server.default;
			trigVals = [2, 4, 8, 16, 32, 64];
			rateVals = [-2, -1, 0.5, 1, 2, 4];
			pos = 0;

			this.loadBuffer(path);

			bus = recbus;
			trigBuf = nil;
			rateBuf = nil;
			trig = false;
			rate = false;
			trigCtr = 0;
			trigTime = List.new(0);
			trigLength = (-1);
		}, "Failed to initialize track: " ++ path);
	}

	loadBuffer { arg path;
		DJError.handle({
			DJError.checkFile(path);

			if(path.endsWith(".mp3")) {
				"Loading MP3 file".postln;
				loadedBuffer = MP3.readToBuffer(Server.default, path);
				// Set loop end after a brief delay for MP3
				{
					0.1.wait;
					loopEnd = (loadedBuffer.numFrames / 44100) * 64;
				}.fork;
			} {
				loadedBuffer = Buffer.read(s, path, action: { arg buf;
					("Buffer loaded: " ++ buf.numFrames ++ " frames").postln;
					loopEnd = (buf.numFrames / 44100) * 64;
				});
			};

			if(resourceManager.notNil) {
				resourceManager.registerBuffer(loadedBuffer);
			};
		}, "Failed to load buffer: " ++ path);
	}

	setLoop { arg val;
		loop = val.clip(0, 1);
		if(buf.notNil) {
			buf.set(\loop, loop);
		};
	}

	setCuePos { arg pos;
		cuePos = pos;
	}

	setLoopEnd { arg pos;
		loopEnd = pos;
		originalLoopEnd = pos;  // Remember the full loop length
		("Loop end set to: " ++ loopEnd).postln;
	}

	setLoopLength { arg multiplier;
		// multiplier should be 1, 0.5, 0.25, 0.125, 0.0625, 0.03125 (1, 1/2, 1/4, 1/8, 1/16, 1/32)
		var loopLengthMultiplier = multiplier;
		var fullLoopLength = originalLoopEnd - cuePos;
		var newLoopEnd = cuePos + (fullLoopLength * multiplier);
		[originalLoopEnd, cuePos, fullLoopLength, newLoopEnd].postln;
		loopEnd = newLoopEnd;
		("Loop length set to " ++ multiplier ++ ", new end: " ++ loopEnd).postln;
	}

	toggleLoop {
		loopEnabled = loopEnabled.not;
		("Loop enabled: " ++ loopEnabled).postln;
	}

	setVol { arg amp;
		param[\amp] = amp.clip(0, 2);
		if(buf.notNil && muted.not) {
			buf.set(\amp, param[\amp]);
		};
	}

	mute {
		muted = true;
		if(buf.notNil) {
			buf.set(\amp, 0);
		};
	}

	unmute {
		muted = false;
		if(buf.notNil) {
			buf.set(\amp, param[\amp]);
		};
	}


	skipPitch {arg val;
		if(buf!=nil,{buf.set(\rate,val)});
	}

	setPitch { arg val;
		param[\rate] = val.clip(-4, 4);
		if(buf.notNil) {
			buf.set(\rate, param[\rate]);
		};
	}

	play { arg playFromPos;
		DJError.handle({
			var synthName = if(chans == 4, \playTrack4, \playTrack2);
			DJError.checkServer(s);
			DJError.checkNil(loadedBuffer, "Buffer");

			("Playing track on " ++ chans ++ " channels").postln;
			([(pos * 44100) / 64, param[\rate], param[\amp]]).postln;
			pos = playFromPos;

			this.stopCurrentPlayback;


			buf = Synth(synthName, [
				\bufnum, loadedBuffer,
				\startPos, (pos * 44100) / 64,
				\amp, if(muted, 0, param[\amp]),
				\bus, bus,
				\rate, param[\rate],
				\deck, deckRef,
				\loop, loop
			]);

			if(resourceManager.notNil) {
				resourceManager.registerSynth(buf);
			};

			this.startPositionTracking;
		}, "Failed to play track");
	}

	startPositionTracking {
		if(posRoutine.notNil) {
			posRoutine.stop;
		};

		posRoutine = {
			inf.do {
				var waitTime = (param[\rate].abs.reciprocal) / 64;
				waitTime.wait;
				if(param[\rate] > 0) {
					pos = pos + 1;
					// Handle looping
					if(loopEnabled && (pos >= loopEnd)) {
						pos = cuePos;
						if(buf.notNil) {
							buf.set(\t_trig, 1, \startPos, (cuePos * 44100) / 64);
						};
					};
				} {
					pos = pos - 1;
					// Handle reverse looping
					if(loopEnabled && (pos <= cuePos)) {
						pos = loopEnd;
						if(buf.notNil) {
							buf.set(\t_trig, 1, \startPos, (loopEnd * 44100) / 64);
						};
					};
				};
			};
		}.fork;

		if(resourceManager.notNil) {
			resourceManager.registerRoutine(posRoutine);
		};
	}

	stopCurrentPlayback {
		if(buf.notNil) {
			if(resourceManager.notNil) {
				resourceManager.freeSynth(buf);
			} {
				buf.free;
			};
			buf = nil;
		};

		if(posRoutine.notNil) {
			if(resourceManager.notNil) {
				resourceManager.stopRoutine(posRoutine);
			} {
				posRoutine.stop;
			};
			posRoutine = nil;
		};
	}

	stop {
		this.stopCurrentPlayback;

		if(trigBuf.notNil) {
			if(resourceManager.notNil) {
				resourceManager.freeSynth(trigBuf);
			} {
				trigBuf.free;
			};
			trigBuf = nil;
		};

		if(trigRoutine.notNil) {
			if(resourceManager.notNil) {
				resourceManager.stopRoutine(trigRoutine);
			} {
				trigRoutine.stop;
			};
			trigRoutine = nil;
		};

		if(rateBuf.notNil) {
			if(resourceManager.notNil) {
				resourceManager.freeSynth(rateBuf);
			} {
				rateBuf.free;
			};
			rateBuf = nil;
		};
	}

	setCutRate {arg val;
		val=(val*(rateVals.size-1)).floor;
		param[\cutrate]=rateVals[val];
		if(trigBuf!=nil,{trigBuf.set(\rate,param[\cutrate])});
	}

	rateOn {
		rate=true;
		buf.set(\amp,0);
		if(chans==4,{
			rateBuf=Synth(\playTrack4,[\bufnum,loadedBuffer,
				\startPos,((pos*44100)/64)%loadedBuffer.numFrames,
				\rate,param[\cutrate]*param[\rate],
				\amp,param[\amp],
				\loop,1,
				\bus,bus
			])
		},{
			rateBuf=Synth(\playTrack2,[\bufnum,loadedBuffer,
				\startPos,((pos*44100)/64)%loadedBuffer.numFrames,
				\rate,param[\cutrate]*param[\rate],
				\amp,param[\amp],
				\loop,1,
				\bus,bus
			])
		});
	}

	rateOff {
		if(trig==false,{buf.set(\amp,param[\amp])});
		rate=false;
		rateBuf.free;
		rateBuf=nil;
	}
	setTrigRate {arg val;
		val=(val*(trigVals.size-1)).floor;
		param[\retrig]=trigVals[val];
		param.postln;
	}

	tapTempo {
		trigTime.add(Main.elapsedTime);
	}

	trigOn {arg tempo;
		[tempo,trigCtr].postln;
		if(trigCtr<4 && tempo==0) {
			this.tapTempo;
			trigCtr=trigCtr+1;
		} {
			[trigLength,tempo].postln;
			if(tempo>0,{trigLength=tempo/60});
			if(trigLength<0) {
				trigLength=0;
				3.do{arg i;
					trigLength=trigLength+(trigTime[i+1]-trigTime[i]);
				};
				trigLength=trigLength/3;
			};
			buf.set(\amp,0);
			trig=true;
			if(chans==4,{
				trigBuf=Synth(\playTrack4,[\bufnum,loadedBuffer,
					\startPos,((pos*44100)/64),
					\rate,param[\rate],\amp,param[\amp],
					\bus,bus
				]);
			},{
				trigBuf=Synth(\playTrack2,[\bufnum,loadedBuffer,
					\startPos,((pos*44100)/64),
					\rate,param[\rate],\amp,param[\amp],
					\bus,bus
				]);
			});
			trigRoutine={inf.do{
				trigBuf.set(\t_trig,1);
				(trigLength/(param[\retrig]*param[\rate])).wait;
			}}.fork;
		}
	}

	trigOff {
		if(rate==false,{buf.set(\amp,param[\amp])});
		trig=false;
		trigBuf.free;
		trigRoutine.stop;
		trigBuf=nil;
	}


	cleanup {
		this.stop;
		if(loadedBuffer.notNil) {
			if(resourceManager.notNil) {
				resourceManager.freeBuffer(loadedBuffer);
			} {
				loadedBuffer.free;
			};
			loadedBuffer = nil;
		};
	}
}