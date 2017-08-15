Track {
	var <>pos,tempo,<>param,buf,posRoutine,bus,<>loadedBuffer,deckRef,loop,muted;
	var trigLength,<>trigBuf,trigRoutine,trigTime,trigVals,trig,trigCtr;
	var <>rateBuf,rate,rateVals;
	var chans;
	classvar s;

	*new {arg path,recbus,ref,c;
	^super.new.initTrack(path,recbus,ref,c);
	}

	*initSynthDefs {

	SynthDef(\playTrack4, { arg bufnum=0,amp=1,length=1,panPot=0,rate=1,t_trig,startPos=0,loop=0,bus=nil,deck=0;
	var env,sound,ampTrig;
		env=EnvGen.ar(Env([1,1],[(BufFrames.kr(bufnum)/44100)*length]), levelScale:amp);
		ampTrig=Impulse.kr(10);
		sound=
			Pan2.ar(PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos,loop),panPot)*amp;
		//Amplitude has to take a single channeled buffer
		SendReply.kr(ampTrig,'amp',[
		Amplitude.kr(
			PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos,loop)*amp),
			deck
			]);
		Out.ar(bus,sound*env);
	}
	).store;

	SynthDef(\playTrack2, { arg bufnum=0,amp=1,length=1,rate=1,t_trig,startPos=0,loop=0,bus=nil,deck=0;
	var env,sound,ampTrig;
		env=EnvGen.ar(Env([1,1],[(BufFrames.kr(bufnum)/44100)*length]), levelScale:amp);
		ampTrig=Impulse.kr(10);
		sound=
			PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos,loop)*amp;
		//Amplitude has to take a single channeled buffer
		SendReply.kr(ampTrig,'amp',[
		Amplitude.kr(
			PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos,loop)*amp),
			deck
			]);
		Out.ar(bus,sound*env);
	}
	).store;
	}

	initTrack {arg path,recbus,ref,c;
		["PATH",path].postln;
		//0=amp,1=reTrigRate,2=cutrate,3=rate,
		param=();
		param[\amp]=1;
		param[\retrig]=1;
		param[\cutrate]=1;
		param[\rate]=1;
		chans=c;
		deckRef=ref;
		muted=false;
		loop=0;
		s=Server.default;
		trigVals=[2,4,8,16,32,64];
		rateVals=[-2,-1,0.5,1,2,4];
		pos=0;
		if(path[path.size-3..path.size-1]=="mp3") {
			"mp3".postln;
			[path,path.class].postln;
			loadedBuffer=MP3.readToBuffer(Server.default,path);
		} {
			loadedBuffer=Buffer.read(s,path);
		};
		bus=recbus;
		trigBuf=nil;
		rateBuf=nil;
		trig=false;
		rate=false;
		trigCtr=0;
		trigTime=List.new(0);
		trigLength=(-1);
	}

	setLoop {arg val;
		loop=val;
	}

	setVol {arg amp;
		param[\amp]=amp;
		buf.set(\amp,amp);
	}

	mute {
		muted=true;
		buf.set(\amp,0);
	}

	unmute {
		muted=false;
		buf.set(\amp,param[\amp]);
	}

	setPitch {arg val;
		param[\rate]=val;
		if(buf!=nil,{buf.set(\rate,param[\rate])});
	}

	skipPitch {arg val;
		if(buf!=nil,{buf.set(\rate,val)});
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

	play {arg cuePos;
		("PLAY:"++chans).postln;
		pos=cuePos;
		if(chans==4,{
			buf=Synth(\playTrack4,[\bufnum,loadedBuffer,
				\startPos,(pos*44100)/64,
				\amp,param[\amp],
				\bus,bus,
				\rate,param[\rate],
				\deck,deckRef,
				\loop,loop
			]);
		},{
			buf=Synth(\playTrack2,[\bufnum,loadedBuffer,
				\startPos,(pos*44100)/64,
				\amp,param[\amp],
				\bus,bus,
				\rate,param[\rate],
				\deck,deckRef,
				\loop,loop
			]);
		});
		posRoutine={
		inf.do{
			(((param[\rate].abs).reciprocal)/64).wait;
			if(param[\rate]>0,{pos=pos+1},{pos=pos-1});
		};
		}.fork;
	}

	stop {
		buf.free;
		buf=nil;
		posRoutine.stop;
	}
}