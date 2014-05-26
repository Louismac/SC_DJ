DJGUI {
classvar s;
var mixer,decks,midi,window;
var dragBox, posBox,trigBox,rateBox,cueBox,launchMap,reconnectMIDI;
var volBar,volResponder,volBarVals,mainMix,labels;

*new {arg modules;
^super.new.initDJGUI(modules);
}

onceServerBooted {arg modules;
	mixer=modules[0];
	decks=modules[1];
	midi=modules[2];

	launchMap=Button(window,Rect(440,0,60,50))
	.states_([["Map MIDI",Color.yellow,Color.black],["Close",Color.black,Color.yellow]])
	.action_({
		if(midi.isLearning,{
			midi.closeGUI;
		},{
			midi.launchGUI;
		});
	});

	reconnectMIDI=Button(window,Rect(0,0,60,50))
	.states_([["Connect \n MIDI",Color.yellow,Color.black]])
	.action_({
		midi.connectMIDIDevice;
	});
}

initDJGUI {
	s=Server.default;

	window = Window.new("YOU ARE ABOUT TO WITNESS THE STRENGTH OF STREET KNOWLEDGE",Rect(0,0, 500,300));
	window.front;
	window.alwaysOnTop_(true);
	window.view.canReceiveDragHandler={true};

	dragBox=List.new(0);
	posBox=List.new(0);
	rateBox=List.new(0);
	trigBox=List.new(0);
	cueBox=List.new(0);
	volBar=List.new(0);
	labels=List.new(0);

	volBarVals=[0,0];
	volResponder=OSCresponder(s.addr,'amp',{arg time,responder,msg;volBarVals[msg[4]]=msg[3]}).add;
	volBar.add(LevelIndicator.new(window,Rect(200,100,20,150)));
	volBar.add(LevelIndicator.new(window,Rect(300,100,20,150)));

	mainMix=LevelIndicator.new(window,Rect(240,100,40,180));
	mainMix.warning=0.65;
	mainMix.critical=0.8;

	{2.do{arg i;
		var lbls=List.new(0);
		dragBox.add(TextField.new(window,Rect(75+(260*i), 55, 100,25))
		.string_(""));
		posBox.add(NumberBox.new(window, Rect(100+(260*i),100,50,20)));
		rateBox.add(NumberBox.new(window, Rect(100+(260*i),220,50,20)));
	    trigBox.add(NumberBox.new(window, Rect(100+(260*i),180,50,20)));
	    cueBox.add(NumberBox.new(window, Rect(100+(260*i),140,50,20)));
	    volBar[i].warning=0.65;
	    volBar[i].critical=0.8;
	 	lbls.add(StaticText.new(window,Rect(75+(260*i),10,100,45))
			.string_("Drop .mp3, .wav or .aiff files here")
			.background_(Color.white));
	    lbls.add(StaticText.new(window,Rect(40+(380*i),100,50,20)).string_("Time").stringColor_(Color.yellow));
	    lbls.add(StaticText.new(window,Rect(40+(380*i),220,50,20)).string_("Cuts").stringColor_(Color.yellow));
	    lbls.add(StaticText.new(window,Rect(40+(380*i),180,50,20)).string_("Chops").stringColor_(Color.yellow));
	    lbls.add(StaticText.new(window,Rect(40+(380*i),140,50,20)).string_("Cue Pos").stringColor_(Color.yellow));
		labels.add(lbls);
	}}.fork(AppClock);
}

update {arg param;
	var index=param[0];
	if(dragBox[index].string!="",{
		dragBox[index].string[0..dragBox[index].string.size-1].postln;
		decks[index].loadTrack(dragBox[index].string[0..dragBox[index].string.size-1]);
		dragBox[index].string_("");
	});
	posBox[index].value_(param[1][0].round(0.01));
	rateBox[index].value_(param[1][1]);
	trigBox[index].value_(param[1][2]);
	cueBox[index].value_(param[1][3]);
	volBar[index].value_((volBarVals[index]*0.8));
	mainMix.value_(((volBarVals[0]*((mixer.mainMix.neg+1)/2))+(volBarVals[1]*((mixer.mainMix+1)/2)))*0.8);
	window.view.background_(Color.grey(((mixer.mainMix.neg+1)/2)),0.5);
}

}