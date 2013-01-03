Track {
	var <>pos,tempo,buf,<>param,buf,ctr,bus,<>loadedBuffer,trigLength,<>trigBuf,<>rateBuf,deckRef,trig,rate,trigRoutine,trigTime,trigVals,rateVals,trigCtr,loop,muted;
	classvar s;

	*new {arg path,recbus,ref;
	^super.new.initTrack(path,recbus,ref);
	}

	*initSynthDefs {

	SynthDef(\playTrack, { arg bufnum=0,amp=1,length=1,panPot=0,rate=1,t_trig,startPos=0,loop=0,bus=nil,deck=0;
	var env,sound,ampTrig;
		env=EnvGen.ar(Env([1,1],[(BufFrames.kr(bufnum)/44100)*length]), levelScale:amp);
		ampTrig=Impulse.kr(10);
		sound=Pan2.ar(
			PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * rate, t_trig, startPos,loop),
			panPot)*amp;
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

	initTrack {arg path,recbus,ref;
		["PATH",path].postln;
		//0=amp,1=reTrigRate,2=cutrate,3=rate
		param=[1,1,1,1];
		deckRef=ref;
		muted=false;
		loop=0;
		trigVals=[2,4,8,16,32,64];
		rateVals=[-2,-1,0.5,1,2,4];
		pos=0;
		if(path[path.size-3..path.size-1]=="mp3") {
			"mp3".postln;
			loadedBuffer=MP3.readToBuffer(s,path);
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
		param[0]=amp;
		buf.set(\amp,amp);
	}

	mute {
		muted=true;
		buf.set(\amp,0);
	}

	unmute {
		muted=false;
		buf.set(\amp,param[0]);
	}

	setPitch {arg val;
		param[3]=val;
		if(buf!=nil,{buf.set(\rate,param[3])});
	}
	
	skipPitch {arg val;
		if(buf!=nil,{buf.set(\rate,val)});
	}

	setCutRate {arg val;
		val=(val*(rateVals.size-1)).floor;
		param[2]=rateVals[val];
		if(trigBuf!=nil,{trigBuf.set(\rate,param[2])});
	}
 
	rateOn {
		rate=true;
		buf.set(\amp,0);
		rateBuf=Synth(\playTrack,[\bufnum,loadedBuffer,\startPos,((pos*44100)/64)%loadedBuffer.numFrames,\rate,param[2]*param[3],\amp,param[0],\loop,1,\bus,bus])
	}

	rateOff {
		if(trig==false,{buf.set(\amp,param[0])});
		rate=false;
		rateBuf.free;
		rateBuf=nil;	
	}
	setTrigRate {arg val;
		val=(val*(trigVals.size-1)).floor;
		param[1]=trigVals[val];
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
			trigBuf=Synth(\playTrack,[\bufnum,loadedBuffer,\startPos,((pos*44100)/64),\rate,param[3],\amp,param[0],\bus,bus]);
			trigRoutine={inf.do{
				trigBuf.set(\t_trig,1);
				(trigLength/(param[1]*param[3])).wait;
			}}.fork;
		}
	}

	trigOff {
		param[0].postln;
		if(rate==false,{buf.set(\amp,param[0])});
		trig=false;
		trigBuf.free;
		trigRoutine.stop;
		trigBuf=nil;	
	}

	play {arg cuePos;
		"PLAY".postln;
		pos=cuePos;
		buf=Synth(\playTrack,[\bufnum,loadedBuffer,\startPos,(pos*44100)/64,\amp,param[0],\bus,bus,\rate,param[3],\deck,deckRef,\loop,loop]);
		ctr={
		inf.do{
			(((param[3].abs).reciprocal)/64).wait;
			if(param[3]>0,{pos=pos+1},{pos=pos-1});
		};
		}.fork;
	}

	playLive {
		var vol;
		pos=0;
		vol=param[0];
		if(trig==true || rate==true) {
			vol=0;
		};
		buf=Synth(\playTrack,[\bufnum,loadedBuffer,\startPos,(pos*44100)/64,\amp,vol,\bus,bus,\rate,param[3],\deck,deckRef,\loop,1]);	
		ctr={
		inf.do{
			"START".postln;
			{((loadedBuffer.numFrames/loadedBuffer.sampleRate)*(64)*(param[3].reciprocal)).do{arg i;
				(((param[3].abs).reciprocal)/64).wait;
				if(param[3]>0,{pos=pos+1},{pos=pos-1});
			}}.fork;
			((loadedBuffer.numFrames/loadedBuffer.sampleRate)*(param[3].reciprocal)).wait;
			pos=0;
		}}.fork;
	}

	stop {
		buf.free;
		buf=nil;
		ctr.stop;
	}
}