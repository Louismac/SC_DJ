LMIDI {

var mixer,decks,category,controls,file,fileReader;

*new {arg m,d;
^super.new.initLMIDI(m,d);	
}

initLMIDI {arg m,d;
	mixer=m;
	decks=d;
	LMIDI.startMIDI;
	controls=();
	this.mapKeys;
	this.loadMap;
	this.launchGUI;
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

launchGUI {
	var win,btns,width=480,height=640,along=4,down=10;
	btns=();
	win=SCWindow.new("",Rect(0,0,width,height)).front;
	controls.do{arg item,i;
		btns[item[\name].asSymbol]=Button(win,Rect((i%down)*(width/down),((i/down).floor)*(height/along),width/down,height/along))
		.states_([[item[\name]++"\n"++item[\responder].matchEvent.note,Color.yellow,Color.black]])
		.action_({
			item[\responder].learn;
			item[\name]++"\n"++item[\responder].matchEvent.note.postln;
			btns[item[\name].asSymbol].states_(
				[[item[\name]++"\n"++item[\responder].matchEvent.note,Color.yellow,Color.black]])
			});
	};
	btns[\save]=Button(win,Rect((width/down)*(down-1),(height/along)*(along-1),width/down,height/along))
	.states_([["SAVE MAP",Color.red,Color.black]])
	.action_({this.saveMap});
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
	for(0,loadedMap.size-1,{arg i;
		loadedMap[i].postln;
		controls[loadedMap[i][0].asSymbol][\responder].matchEvent=(MIDIEvent(nil,nil,loadedMap[i][2],loadedMap[i][1],nil));
	});
}

mapKeys {
	//main xfade(-1 to 1)
	controls.postln;
	controls[\mainxfade]=();
	controls[\mainxfade][\name]="mainxfade";
	controls[\mainxfade][\index]=0;
	controls[\mainxfade][\responder]=
		CCResponder({arg src,chan,num,vel;mixer.updateMix((vel/63.5)-1)},nil,0,[6,52],nil);
	//cue xfade(-1 to 1)
	controls[\cuexfade]=();
	controls[\cuexfade][\name]="cuexfade";
	controls[\cuexfade][\index]=1;
	controls[\cuexfade][\responder]=
		CCResponder({arg src,chan,num,vel;mixer.updateCueMix((vel/63.5)-1)},nil,0,[18,61],nil);
	//Reset auto load
	controls[\resetload]=();
	controls[\resetload][\name]="resetload";
	controls[\resetload][\index]=2;
	controls[\resetload][\responder]=
		CCResponder({arg src,chan,num,vel;decks[0].resetAutoLoad;decks[1].resetAutoLoad},nil,0,27,nil);
	2.do{arg i;
	// pitch
	controls[("pitch"++i).asSymbol]=();
	controls[("pitch"++i).asSymbol][\name]="pitch"++i;
	controls[("pitch"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].updatePitch(0.9+(vel/635))},nil,0,2+(i*5),nil);
	// start/stop
	controls[("startStop"++i).asSymbol]=();
	controls[("startStop"++i).asSymbol][\name]="startStop"++i;
	controls[("startStop"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].startstop;},nil,0,23+(i*5),nil);
	// powerdown stop
	controls[("powerdown"++i).asSymbol]=();
	controls[("powerdown"++i).asSymbol][\name]="powerdown"++i;
	controls[("powerdown"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].powerDown;},nil,0,36+(i*5),nil);
	// next track
	controls[("nexttrack"++i).asSymbol]=();
	controls[("nexttrack"++i).asSymbol][\name]="nexttrack"++i;
	controls[("nexttrack"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].loadNextTrack;},nil,0,35+(i*5),nil);
	// set pos to cue
	controls[("setToCuePos"++i).asSymbol]=();
	controls[("setToCuePos"++i).asSymbol][\name]="setToCuePos"++i;
	controls[("setToCuePos"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].setToCuePos;},nil,0,33+(i*5),nil);
	// cue set
	controls[("setCue"++i).asSymbol]=();
	controls[("setCue"++i).asSymbol][\name]="setCue"++i;
	controls[("setCue"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].setCue(true)},nil,0,24+(i*5),nil);
	// fine tune cue
	controls[("fineTuneCue"++i).asSymbol]=();
	controls[("fineTuneCue"++i).asSymbol][\name]="fineTuneCue"++i;
	controls[("fineTuneCue"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].fineTuneCue(vel-63.5)},nil,0,14+(i*5),nil);
	// cue set 0
	controls[("setCueZero"++i).asSymbol]=();
	controls[("setCueZero"++i).asSymbol][\name]="setCueZero"++i;
	controls[("setCueZero"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].setCue(false)},nil,0,34+(i*5),nil);
	// skip forward
	controls[("skipForward"++i).asSymbol]=();
	controls[("skipForward"++i).asSymbol][\name]="skipForward"++i;
	controls[("skipForward"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;
			if(vel>0,{decks[i].skipForward},{decks[i].stopSkip});
			},nil,0,48+(i*41),nil);
	// skip slow
	controls[("skipSlow"++i).asSymbol]=();
	controls[("skipSlow"++i).asSymbol][\name]="skipSlow"++i;
	controls[("skipSlow"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;
				if(vel>0,{decks[i].skipSlow},{decks[i].stopSkip});
	         },nil,0,47+(i*41),nil);
	// skip backwards
	controls[("skipBackwards"++i).asSymbol]=();
	controls[("skipBackwards"++i).asSymbol][\name]="skipBackwards"++i;
	controls[("skipBackwards"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;
				if(vel>0,{decks[i].skipBackwards},{decks[i].stopSkip});
	         },nil,0,49+(i*41),nil);
	// retrig
	controls[("trigOn"++i).asSymbol]=();
	controls[("trigOn"++i).asSymbol][\name]="trigOn"++i;
	controls[("trigOn"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;
			if(vel>0,{decks[i].trigOn},{decks[i].trigOff})
			},nil,0,25+(i*5),nil);
	// trig rate
	controls[("setTrigRate"++i).asSymbol]=();
	controls[("setTrigRate"++i).asSymbol][\name]="setTrigRate"++i;
	controls[("setTrigRate"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].setTrigRate(vel/127)},nil,0,16+(i*5),nil);
	// rate
	controls[("rateOn"++i).asSymbol]=();
	controls[("rateOn"++i).asSymbol][\name]="rateOn"++i;
	controls[("rateOn"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;
			if(vel>0,{decks[i].rateOn},{decks[i].rateOff});
			},nil,0,26+(i*5),nil);
	// rate rate
	controls[("setCutRate"++i).asSymbol]=();
	controls[("setCutRate"++i).asSymbol][\name]="setCutRate"++i;
	controls[("setCutRate"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;decks[i].setCutRate(vel/127)},nil,0,17+(i*5),nil);
	// vol 
	controls[("setVol"++i).asSymbol]=();
	controls[("setVol"++i).asSymbol][\name]="setVol"++i;
	controls[("setVol"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;
			decks[i].setVol((vel/127)*2);
		},nil,0,3+(i*5),nil);
	//Bass
	controls[("updateLo"++i).asSymbol]=();
	controls[("updateLo"++i).asSymbol][\name]="updateLo"++i;
	controls[("updateLo"++i).asSymbol][\responder]=
	CCResponder({arg src,chan,num,vel;
			vel=((vel/127)*2)-1;
			if(vel<0, {
				mixer.updateLo(i,vel*40);
			},{
				mixer.updateLo(i,vel*20);
			});
		},nil,0,15+(i*5),nil);
	//Mid
	controls[("updateMid"++i).asSymbol]=();
	controls[("updateMid"++i).asSymbol][\name]="updateMid"++i;
	controls[("updateMid"++i).asSymbol][\responder]=
		CCResponder({arg src,chan,num,vel;
			vel=((vel/127)*2)-1;
			if(vel<0, {
				mixer.updateMid(i,vel*40);
			},{
				mixer.updateMid(i,vel*20);
			});	
			},nil,0,4+(i*5),nil);
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