Mixer {
classvar s;	
var decks,mainOut,cueOut,param,<>mainMix,eqBus,<>channels;

*new {arg d;
	^super.new.initMixer(d);
}

*initSynthDefs {
SynthDef(\mixOut,{arg out,bus1,bus2,xfade=0,channels=1;
	var input1,input2;
	input1=InFeedback.ar(bus1,2);
	input2=InFeedback.ar(bus2,2);
	Out.ar(out,XFade2.ar(input1,input2,xfade));
}).store;	

SynthDef(\eqMixer,{arg outBus,inBus,hiVal=0,midVal=0,loVal=0,channels=2;
	var lo,mid,hi;
	lo=BPeakEQ.ar(InFeedback.ar(inBus,2),80,0.8,loVal);
	mid=BPeakEQ.ar(lo,1500,1,midVal);
	hi=BPeakEQ.ar(lo,5000,0.8,hiVal);
	Out.ar(outBus,Limiter.ar(hi,1,0.005));
	}).store
}

initMixer {arg d;
	var mainChans,cueChans;
	channels=2;
	decks=d;
	param=[0,0,1,1];
	mainMix=0;
	s=Server.default;
	mainChans=[0,1];
	cueChans=[2,3];
	eqBus=[[0,0],[0,0]];
	{
		0.5.wait;
		2.do{arg i;
			eqBus[i][0]=Bus.audio(s,channels);
			0.5.wait;
			eqBus[i][1]=Synth(\eqMixer,[\outBus,eqBus[i][0],\inBus,decks[i].bus]);
		};
		2.wait;
		mainOut=Synth(\mixOut,[\out,mainChans,\bus1,eqBus[0][0],\bus2,eqBus[1][0]]);
		cueOut=Synth(\mixOut,[\out,cueChans,\bus1,eqBus[0][0],\bus2,eqBus[1][0]]);
	}.fork;
}

updateMix {arg xfade;
	mainOut.set(\xfade, xfade);
	mainMix=xfade;
}

updateCueMix {arg xfade;
	cueOut.set(\xfade,xfade);
}

updateLo {arg chan,val;
	eqBus[chan][1].set(\loVal,val);	
}

updateMid {arg chan,val;
	eqBus[chan][1].set(\midVal,val);	
}

updateHi {arg chan,val;
	eqBus[chan][1].set(\hiVal,val);	
}

}