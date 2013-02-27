DJMIDI {

var mixer,decks,controls,file,fileReader,toLearn,<>isLearning,win,btns;

*new {arg m,d;
^super.new.initDJMIDI(m,d);	
}

initDJMIDI {arg m,d;
	mixer=m;
	decks=d;
	this.initDictionary;
	isLearning=false;
	
	this.connectMIDIDevice;
}

*postVals {
	this.startMIDI;
	MIDIIn.noteOff = { arg src, chan, num, vel; 	[chan,num,vel / 127].postln; };
	MIDIIn.noteOn = { arg src, chan, num, vel; 	[chan,num,vel / 127].postln; };
	MIDIIn.polytouch = { arg src, chan, num, vel; 	[chan,num,vel / 127].postln; };
	MIDIIn.control = { arg src, chan, num, val; 	[chan,num,val].postln; };
	MIDIIn.program = { arg src, chan, prog; 		[chan,prog].postln; };
	MIDIIn.touch = { arg src, chan, pressure; 	[chan,pressure].postln; };
	MIDIIn.bend = { arg src, chan, bend; 			[chan,bend - 8192].postln; };
	MIDIIn.sysex = { arg src, sysex; 			sysex.postln; };
	MIDIIn.sysrt = { arg src, chan, val; 			[chan,val].postln; };
	MIDIIn.smpte = { arg src, chan, val; 			[chan,val].postln; };	
}

*killKeys {
	MIDIResponder.removeAll;	
}

*startMIDI {
	var inPorts = 3, outPorts = 2;
	
	MIDIClient.init(inPorts,outPorts);			
	inPorts.do({ arg i; 
		MIDIIn.connect(i, MIDIClient.sources.at(i));
	});	
}

connectMIDIDevice {
	LMIDI.killKeys;
	LMIDI.startMIDI;
	this.loadMap;
	this.addLearnResponder;
}

launchGUI {
	var width=480,height=640,along=3,down=20;
	btns=();
	toLearn=nil;
	isLearning=true;
	win=SCWindow.new("Select function then press MIDI to learn",Rect(500,0,width,height)).front;
	win.userCanClose=false;
	controls.do{arg item,i;
		btns[item[\name].asSymbol]=Button(win,Rect(item[\left]*(width/along),item[\top]*(height/down),width/along,height/down))
		.states_([
			[item[\label]++"\n"++"Midi note "++item[\responder].matchEvent.note,Color.yellow,Color.black],	
			["Press MIDI to Learn",Color.black,Color.yellow]
		])
		.action_({
			item[\responder].matchEvent.note.postln;
			if(toLearn!=nil, {
				if(toLearn!=item[\name], {
					btns[toLearn.asSymbol].value_(0);
					toLearn=item[\name];
				},{
					"toLearn=nil".postln;
					toLearn=nil;
					btns[item[\name].asSymbol].states_([
						[item[\label]++"\n"++"Midi note "++item[\responder].matchEvent.note,Color.yellow,Color.black],	
						["Press MIDI to Learn",Color.black,Color.yellow]
					])
				});
			},{
				toLearn=item[\name];
			});
		});
	};
	btns[\save]=Button(win,Rect(2*(width/along),0*(height/down),width/along,height/down))
	.states_([["SAVE MAP",Color.red,Color.black]])
	.action_({this.saveMap});
}

closeGUI {
	win.close;
	isLearning=false;
}

addLearnResponder {
	CCResponder({arg src,chan,num,vel;
		if(toLearn!=nil, {
			num.postln;
			try{
				controls[toLearn.asSymbol][\responder].remove;
				controls[toLearn.asSymbol][\responder]=
					CCResponder(controls[toLearn.asSymbol][\function],nil,chan,num,nil);
				Task({1.do{{btns[toLearn.asSymbol].valueAction_(0)}.defer}},AppClock).play;
			} {arg error;
				error.postln;
			}
		});
		},nil,nil,nil,nil);
}

saveMap {
 	file=File("DJMIDIMAP.dj","w");
 	controls.do{arg item,i;
 		file.write(item[\name] ++ " " 
			++ item[\responder].matchEvent.note ++ " " 
			++ item[\responder].matchEvent.chan 
			++ " \n" ); 
 	};
 	file.close;
}

loadMap {
 	var loadedMap=FileReader.read("DJMIDIMAP.dj");
	LMIDI.killKeys;
	for(0,loadedMap.size-1,{arg i;
		loadedMap[i].postln;
		controls[loadedMap[i][0].asSymbol][\responder]=
			CCResponder({arg src,chan,num,vel;
				if(isLearning==false,{
					controls[loadedMap[i][0].asSymbol][\function].value(src,chan,num,vel);
				});
				},
				nil,loadedMap[i][2].asInteger,loadedMap[i][1].asInteger,nil);
	});
}

initDictionary {
	controls=();
	//main xfade(-1 to 1)
	controls[\mainxfade]=();
	controls[\mainxfade][\name]="mainxfade";
	controls[\mainxfade][\label]="Main Crossfader";
	controls[\mainxfade][\top]=0;
	controls[\mainxfade][\left]=1;
	controls[\mainxfade][\function]={arg src,chan,num,vel;mixer.updateMix((vel/63.5)-1)};
	//cue xfade(-1 to 1)
	controls[\cuexfade]=();
	controls[\cuexfade][\name]="cuexfade";
	controls[\cuexfade][\label]="Cue Crossfader";
	controls[\cuexfade][\top]=1;
	controls[\cuexfade][\left]=1;
	controls[\cuexfade][\function]={arg src,chan,num,vel;mixer.updateCueMix((vel/63.5)-1)};
	//Reset auto load
	controls[\resetload]=();
	controls[\resetload][\name]="resetload";
	controls[\resetload][\label]="Reset Auto Load Queue";
	controls[\resetload][\top]=5;
	controls[\resetload][\left]=1;
	controls[\resetload][\function]={arg src,chan,num,vel;decks[0].resetAutoLoad;decks[1].resetAutoLoad};
	2.do{arg i;
	// pitch
	controls[("pitch"++i).asSymbol]=();
	controls[("pitch"++i).asSymbol][\name]="pitch"++i;
	controls[("pitch"++i).asSymbol][\label]="Pitch (Chan "++(i+1)++")";
	controls[("pitch"++i).asSymbol][\top]=2;
	controls[("pitch"++i).asSymbol][\left]=i*2;
	controls[("pitch"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].updatePitch(0.9+(vel/635))};
	// start/stop
	controls[("startStop"++i).asSymbol]=();
	controls[("startStop"++i).asSymbol][\name]="startStop"++i;
	controls[("startStop"++i).asSymbol][\label]="Start/Stop (Chan "++(i+1)++")";
	controls[("startStop"++i).asSymbol][\top]=3;
	controls[("startStop"++i).asSymbol][\left]=i*2;
	controls[("startStop"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].startstop;};
	// powerdown stop
	controls[("powerdown"++i).asSymbol]=();
	controls[("powerdown"++i).asSymbol][\name]="powerdown"++i;
	controls[("powerdown"++i).asSymbol][\label]="Deck Powerdown (Chan "++(i+1)++")";
	controls[("powerdown"++i).asSymbol][\top]=4;
	controls[("powerdown"++i).asSymbol][\left]=i*2;
	controls[("powerdown"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].powerDown;};
	// next track
	controls[("nexttrack"++i).asSymbol]=();
	controls[("nexttrack"++i).asSymbol][\name]="nexttrack"++i;
	controls[("nexttrack"++i).asSymbol][\label]="Load Next Track (Chan "++(i+1)++")";
	controls[("nexttrack"++i).asSymbol][\top]=5;
	controls[("nexttrack"++i).asSymbol][\left]=i*2;
	controls[("nexttrack"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].loadNextTrack;};
	// set pos to cue
	controls[("setToCuePos"++i).asSymbol]=();
	controls[("setToCuePos"++i).asSymbol][\name]="setToCuePos"++i;
	controls[("setToCuePos"++i).asSymbol][\label]="Move To Cue (Chan "++(i+1)++")";
	controls[("setToCuePos"++i).asSymbol][\top]=6;
	controls[("setToCuePos"++i).asSymbol][\left]=i*2;
	controls[("setToCuePos"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].setToCuePos;};
	// cue set
	controls[("setCue"++i).asSymbol]=();
	controls[("setCue"++i).asSymbol][\name]="setCue"++i;
	controls[("setCue"++i).asSymbol][\label]="Set Cue Position (Chan "++(i+1)++")";
	controls[("setCue"++i).asSymbol][\top]=7;
	controls[("setCue"++i).asSymbol][\left]=i*2;
	controls[("setCue"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].setCue(true)};
	// fine tune cue
	controls[("fineTuneCue"++i).asSymbol]=();
	controls[("fineTuneCue"++i).asSymbol][\name]="fineTuneCue"++i;
	controls[("fineTuneCue"++i).asSymbol][\label]="Tweek Cue Pos (Chan "++(i+1)++")";
	controls[("fineTuneCue"++i).asSymbol][\top]=8;
	controls[("fineTuneCue"++i).asSymbol][\left]=i*2;
	controls[("fineTuneCue"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].fineTuneCue(vel-63.5)};
	// cue set 0
	controls[("setCueZero"++i).asSymbol]=();
	controls[("setCueZero"++i).asSymbol][\name]="setCueZero"++i;
	controls[("setCueZero"++i).asSymbol][\label]="Reset Cue to Start (Chan "++(i+1)++")";
	controls[("setCueZero"++i).asSymbol][\top]=9;
	controls[("setCueZero"++i).asSymbol][\left]=i*2;
	controls[("setCueZero"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].setCue(false)};
	// skip forward
	controls[("skipForward"++i).asSymbol]=();
	controls[("skipForward"++i).asSymbol][\name]="skipForward"++i;
	controls[("skipForward"++i).asSymbol][\label]="Skip Forwards (Chan "++(i+1)++")";
	controls[("skipForward"++i).asSymbol][\top]=10;
	controls[("skipForward"++i).asSymbol][\left]=i*2;
	controls[("skipForward"++i).asSymbol][\function]={arg src,chan,num,vel;
			if(vel>0,{decks[i].skipForward},{decks[i].stopSkip});
			};
	// skip slow
	controls[("skipSlow"++i).asSymbol]=();
	controls[("skipSlow"++i).asSymbol][\name]="skipSlow"++i;
	controls[("skipSlow"++i).asSymbol][\label]="Slow Down (Chan "++(i+1)++")";
	controls[("skipSlow"++i).asSymbol][\top]=11;
	controls[("skipSlow"++i).asSymbol][\left]=i*2;
	controls[("skipSlow"++i).asSymbol][\function]={arg src,chan,num,vel;
				if(vel>0,{decks[i].skipSlow},{decks[i].stopSkip});
	         };
	// skip backwards
	controls[("skipBackwards"++i).asSymbol]=();
	controls[("skipBackwards"++i).asSymbol][\name]="skipBackwards"++i;
	controls[("skipBackwards"++i).asSymbol][\label]="Skip Backwards (Chan "++(i+1)++")";
	controls[("skipBackwards"++i).asSymbol][\top]=12;
	controls[("skipBackwards"++i).asSymbol][\left]=i*2;
	controls[("skipBackwards"++i).asSymbol][\function]={arg src,chan,num,vel;
				if(vel>0,{decks[i].skipBackwards},{decks[i].stopSkip});
	         };
	// retrig
	controls[("trigOn"++i).asSymbol]=();
	controls[("trigOn"++i).asSymbol][\name]="trigOn"++i;
	controls[("trigOn"++i).asSymbol][\label]="Trigger Chops (Chan "++(i+1)++")";
	controls[("trigOn"++i).asSymbol][\top]=13;
	controls[("trigOn"++i).asSymbol][\left]=i*2;
	controls[("trigOn"++i).asSymbol][\function]={arg src,chan,num,vel;
			if(vel>0,{decks[i].trigOn},{decks[i].trigOff})
			};
	// trig rate
	controls[("setTrigRate"++i).asSymbol]=();
	controls[("setTrigRate"++i).asSymbol][\name]="setTrigRate"++i;
	controls[("setTrigRate"++i).asSymbol][\label]="Chops Rate (Chan "++(i+1)++")";
	controls[("setTrigRate"++i).asSymbol][\top]=14;
	controls[("setTrigRate"++i).asSymbol][\left]=i*2;
	controls[("setTrigRate"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].setTrigRate(vel/127)};
	// rate
	controls[("rateOn"++i).asSymbol]=();
	controls[("rateOn"++i).asSymbol][\name]="rateOn"++i;
	controls[("rateOn"++i).asSymbol][\label]="Trigger Cut (Chan "++(i+1)++")";
	controls[("rateOn"++i).asSymbol][\top]=15;
	controls[("rateOn"++i).asSymbol][\left]=i*2;
	controls[("rateOn"++i).asSymbol][\function]={arg src,chan,num,vel;
			if(vel>0,{decks[i].rateOn},{decks[i].rateOff});
			};
	// rate rate
	controls[("setCutRate"++i).asSymbol]=();
	controls[("setCutRate"++i).asSymbol][\name]="setCutRate"++i;
	controls[("setCutRate"++i).asSymbol][\label]="Cut Playback Rate (Chan "++(i+1)++")";
	controls[("setCutRate"++i).asSymbol][\top]=16;
	controls[("setCutRate"++i).asSymbol][\left]=i*2;
	controls[("setCutRate"++i).asSymbol][\function]={arg src,chan,num,vel;decks[i].setCutRate(vel/127)};
	// vol 
	controls[("setVol"++i).asSymbol]=();
	controls[("setVol"++i).asSymbol][\name]="setVol"++i;
	controls[("setVol"++i).asSymbol][\label]="Volume (Chan "++(i+1)++")";
	controls[("setVol"++i).asSymbol][\top]=17;
	controls[("setVol"++i).asSymbol][\left]=i*2;
	controls[("setVol"++i).asSymbol][\function]={arg src,chan,num,vel;
			decks[i].setVol((vel/127)*2);
		};
	//Bass
	controls[("updateLo"++i).asSymbol]=();
	controls[("updateLo"++i).asSymbol][\name]="updateLo"++i;
	controls[("updateLo"++i).asSymbol][\label]="Bass (Chan "++(i+1)++")";
	controls[("updateLo"++i).asSymbol][\top]=18;
	controls[("updateLo"++i).asSymbol][\left]=i*2;
	controls[("updateLo"++i).asSymbol][\function]={arg src,chan,num,vel;
			vel=((vel/127)*2)-1;
			if(vel<0, {
				mixer.updateLo(i,vel*40);
			},{
				mixer.updateLo(i,vel*20);
			});
		};
		
		
	//Mid
	// controls[("updateMid"++i).asSymbol]=();
	// controls[("updateMid"++i).asSymbol][\name]="updateMid"++i;
	// controls[("updateMid"++i).asSymbol][\responder]=
	// 	CCResponder({arg src,chan,num,vel;
	// 		vel=((vel/127)*2)-1;
	// 		if(vel<0, {
	// 			mixer.updateMid(i,vel*40);
	// 		},{
	// 			mixer.updateMid(i,vel*20);
	// 		});	
	// 		},nil,0,4+(i*5),nil);
	//Hi
	// CCResponder({arg src,chan,num,vel;
	// 		vel=((vel/127)*2)-1;
	// 		if(vel<0, {
	// 			mixer.updateHi(i,vel*40);
	// 		},{
	// 			mixer.updateHi(i,vel*20);
	// 		});	
	// 		},nil,0,5+(i*5),nil);
	
	
	}
}

}