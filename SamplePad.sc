SamplePad {

	var samples,bus;

	*new {arg b;
		^super.new.initSamplePad(b);
	}

	*initSynthDefs {

	SynthDef(\playSample, { arg bufnum=0,amp=1,length=1,panPot=0,rate=1,t_trig,startPos=0,loop=0,bus=nil,deck=0;
	var env,sound,ampTrig;
		env=EnvGen.ar(Env([1,1],[(BufFrames.kr(bufnum)/44100)*length]), levelScale:amp);
		ampTrig=Impulse.kr(10);
		sound=Pan2.ar(
			PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos,loop),
			panPot)*amp;
		Out.ar(bus,sound*env);
	}
	).store;

	}

	initSamplePad {arg b;
		samples=();
		bus=b;
	}

	triggerSample {arg key;
		if(samples[key]!=nil,{
			Synth(\playSample,[\bufnum,samples[key],\bus,bus]);
		});
	}

	loadSample {arg path,key;
		if(path[path.size-3..path.size-1]=="mp3") {
			"mp3".postln;
			[path,path.class].postln;
			samples[key]=MP3.readToBuffer(Server.default,path);
		} {
			samples[key]=Buffer.read(Server.default,path);
		};
	}

}