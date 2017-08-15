Mixer {
	classvar s;
	var buses,mainOut,cueOut,param,<>mainMix,eqBus,channels;

	*new {arg d,c;
		^super.new.initMixer(d,c);
	}

	*initSynthDefs {
		SynthDef(\mixOut2,{arg out,bus1,bus2,xfade=0,sampleBus;
			var input1,input2,tracks,samples,output;
			input1=InFeedback.ar(bus1,1);
			input2=InFeedback.ar(bus2,1);
			tracks=XFade2.ar(input1,input2,xfade);

			samples=InFeedback.ar(sampleBus,1);
			output=XFade2.ar(tracks,samples,0);
			Out.ar(out,output);
		}).store;

		SynthDef(\mixOut4,{arg out,bus1,bus2,xfade=0,sampleBus;
			var input1,input2,tracks,samples,output;
			input1=InFeedback.ar(bus1,2);
			input2=InFeedback.ar(bus2,2);
			tracks=XFade2.ar(input1,input2,xfade);

			samples=InFeedback.ar(sampleBus,2);
			output=XFade2.ar(tracks,samples,0);

			Out.ar(out,output);
		}).store;

		SynthDef(\eqMixer2,{arg outBus,inBus,hiVal=0,midVal=0,loVal=0;
			var lo,mid,hi;
			lo=BPeakEQ.ar(InFeedback.ar(inBus,1),80,0.8,loVal);
			mid=BPeakEQ.ar(lo,1500,1,midVal);
			hi=BPeakEQ.ar(lo,5000,0.8,hiVal);
			Out.ar(outBus,Limiter.ar(hi,1,0.005));
		}).store;

		SynthDef(\eqMixer4,{arg outBus,inBus,hiVal=0,midVal=0,loVal=0;
			var lo,mid,hi;
			lo=BPeakEQ.ar(InFeedback.ar(inBus,2),80,0.8,loVal);
			mid=BPeakEQ.ar(lo,1500,1,midVal);
			hi=BPeakEQ.ar(lo,5000,0.8,hiVal);
			Out.ar(outBus,Limiter.ar(hi,1,0.005));
		}).store;
	}

	initMixer {arg b,c;
		channels=c;
		buses=b;
		param=[0,0,1,1];
		mainMix=0;
		s=Server.default;
		eqBus=[[0,0],[0,0]];
		["channels",channels,"buses",buses].postln;
		if(channels==4,{
			{
				0.5.wait;
				2.do{arg i;
					eqBus[i][0]=Bus.audio(s,2);
					0.5.wait;
					eqBus[i][1]=Synth(\eqMixer4,[\outBus,eqBus[i][0],\inBus,buses[i.asSymbol]]);
				};
				2.wait;
				mainOut=Synth(\mixOut4,[
					\out,[0,1],
					\bus1,eqBus[0][0],
					\bus2,eqBus[1][0],
					\sampleBus,buses[\sample],
				]);
				cueOut=Synth(\mixOut4,[
					\out,[2,3],
					\bus1,eqBus[0][0],
					\bus2,eqBus[1][0],
					\sampleBus,buses[\sample],
				]);
			}.fork;
		},{
			{
				0.5.wait;
				2.do{arg i;
					eqBus[i][0]=Bus.audio(s,1);
					0.5.wait;
					eqBus[i][1]=Synth(\eqMixer2,[\outBus,eqBus[i][0],\inBus,buses[i.asSymbol]]);
				};
				2.wait;
				mainOut=Synth(\mixOut2,[
					\out,1,
					\bus1,eqBus[0][0],
					\bus2,eqBus[1][0],
					\sampleBus,buses[\sample],
				]);
				cueOut=Synth(\mixOut2,[
					\out,0,
					\bus1,eqBus[0][0],
					\bus2,eqBus[1][0],
					\sampleBus,buses[\sample],
				]);
			}.fork;
		});
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