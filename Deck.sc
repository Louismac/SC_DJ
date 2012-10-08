Deck {
classvar s;	
var <>bus,<>track,startstopBool,cuePos,fwdRoutine,bwdRoutine,slowRoutine,ref,fineTune=0,param,files,onetouch;

*new {arg refNo;
^super.new.initDeck(refNo);	
}
	
initDeck {arg refNo;
	{
		s=Server.default;
		s.sync;
		bus=Bus.audio(s,2);
		this.resetAutoLoad;
	}.fork;
	ref=refNo;
	startstopBool=true;
	cuePos=0;
	param=[1,1,1,1];
	//onetouch buttons ignore the MIDI off event 
	onetouch=Array.fill(6,{true});
}	

resetAutoLoad {
	var path,index=0;
	if(onetouch[index],{
		path=PathName("/Users/LouisMcc/Music/djing/sc/"++ref);
		files=path.files;
		for(0,files.size-1,{arg i;
			files[i].fullPath.postln;
			})
		;
		onetouch[index]=false;
	},{onetouch[index]=true});
}

 loadTrack {arg path;
	["PATH",path].postln;
	if(track!=nil,{track.loadedBuffer.free});
	if(startstopBool==false,{track.stop});	
	cuePos=0;
	track=Track.new(path,bus,ref);
}

loadNextTrack {
	var index=3;
	if(onetouch[index],{
		if(files.size>0) {
			this.loadTrack(files[0].fullPath);
			files.removeAt(0);
		};
		onetouch[index]=false;
	},{onetouch[index]=true});
}

getVals {
	if(track!=nil,{
		param[0]=track.pos/64;
		param[1]=track.param[2];
		param[2]=track.param[1];
		param[3]=(cuePos+fineTune)/64;
	});
	^param;
}

setCue {arg val;
	var index=5;
	if(onetouch,{
		if(val==true,{cuePos=track.pos},{cuePos=0});
		fineTune=0;
		onetouch[index]=false;
	},{onetouch[index]=true});
}

fineTuneCue {arg val;
	fineTune=val;	
}

skipForward {
	fwdRoutine=
	{
		track.skipPitch(1.2);
		1.wait;
		track.skipPitch(1.3);
		3.wait;
		track.skipPitch(3);	
	}.fork;	
}

skipSlow {
	slowRoutine=
	{
		"slow".postln;
		track.skipPitch(0.8);
		3.wait;
		track.skipPitch(0.3);	
	}.fork;	
}

skipBackwards {
	bwdRoutine=
	{
		track.skipPitch(-0.5);
		0.5.wait;
		track.skipPitch(-1);
		1.wait;
		track.skipPitch(-2);
		1.wait;
		track.skipPitch(-3);
		3.wait;
		track.skipPitch(-6);	
	}.fork;	
}

stopSkip {
	fwdRoutine.stop;
	bwdRoutine.stop;
	slowRoutine.stop;
	track.setPitch(track.param[3]);	
}

powerDown{
	var dur,index=2;
	if(onetouch[index],{
		dur=0.8;
		{
			{256.do{arg i;
				track.skipPitch(track.param[3]*(1-(i/256)));
				(dur/256).wait;
			}}.fork;
			dur.wait;
			this.startstop;	
		}.fork;
		onetouch[index]=false;
	},{onetouch[index]=true});
	
}

trigOn {
	track.trigOn(0);
}

trigOff {
	track.trigOff;
}

rateOn {
	track.rateOn;
}

rateOff {
	track.rateOff;	
}

setTrigRate {arg val;
	track.setTrigRate(val);
}

setCutRate {arg val;
	track.setCutRate(val);	
}

setToCuePos {
	var index=4;
	if(onetouch[index],{
		this.startstop;
		this.startstop;
		onetouch[index]=false;
	},{onetouch[index]=true});
}

startstop {
	var index=1;
	if(onetouch[index],{
		if(startstopBool) {
			track.postln;
			track.play(cuePos+fineTune);
			startstopBool=false
		} {
		 	track.stop;
			startstopBool=true;
		};
		onetouch[index]=false;
	},{onetouch[index]=true});
}

updatePitch {arg val;
	if(track!=nil,{track.setPitch(val)});	
}

setVol {arg val;
	if(track!=nil,{track.setVol(val)});	
}

}