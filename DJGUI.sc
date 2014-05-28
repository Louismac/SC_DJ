DJGUI {
	classvar s;
	var mixer,decks,midi,window,samplePad;
	var trackDragBox, sampleDragBox, posBox,trigBox,rateBox,cueBox,launchMap,reconnectMIDI;
	var volBar,volResponder,volBarVals,mainMix,labels;
	var w,h;

	*new {arg modules;
		^super.new.initDJGUI(modules);
	}

	onceServerBooted {arg modules;
		mixer=modules[0];
		decks=modules[1];
		samplePad=modules[2];
		midi=modules[3];

		launchMap=Button(window,Rect(w*0.9,0,w*0.1,w*0.1))
		.states_([["Map MIDI",Color.yellow,Color.black],["Close",Color.black,Color.yellow]])
		.action_({
			if(midi.isLearning,{
				midi.closeGUI;
				},{
					midi.launchGUI;
			});
		});

		reconnectMIDI=Button(window,Rect(0,0,w*0.1,w*0.1))
		.states_([["Connect \n MIDI",Color.yellow,Color.black]])
		.action_({
			midi.connectMIDIDevice;
		});
	}

	initDJGUI {

		w=600;
		h=400;
		s=Server.default;

		window = Window.new("YOU ARE ABOUT TO WITNESS THE STRENGTH OF STREET KNOWLEDGE",Rect(0,0, w,h));
		window.front;
		window.alwaysOnTop_(true);
		window.view.canReceiveDragHandler={true};

		trackDragBox=List.new(0);
		sampleDragBox=List.new(0);
		posBox=List.new(0);
		rateBox=List.new(0);
		trigBox=List.new(0);
		cueBox=List.new(0);
		volBar=List.new(0);
		labels=List.new(0);

		volBarVals=[0,0];
		volResponder=OSCresponder(s.addr,'amp',{arg time,responder,msg;volBarVals[msg[4]]=msg[3]}).add;
		volBar.add(LevelIndicator.new(window,Rect(w*0.375,h/3,w*0.05,h*0.5)));
		volBar.add(LevelIndicator.new(window,Rect(w*0.575,h/3,w*0.05,h*0.5)));

		mainMix=LevelIndicator.new(window,Rect(w*0.45,h/3,w*0.1,h*0.6));
		mainMix.warning=0.65;
		mainMix.critical=0.8;

		{2.do{arg i;
			var lbls=List.new(0);
			trackDragBox.add(TextField.new(window,Rect((w*0.15)+((w*0.5)*i),h*0.15,w*0.2,h*0.2))
				.string_(""));
			sampleDragBox.add(TextField.new(window,Rect((w*0.375)+((w*0.15)*i),h*0.1,w*0.1,h*0.2))
				.string_(""));
			posBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*i),h*0.4,w*0.1,h*0.1)));
			rateBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*i),h*0.5,w*0.1,h*0.1)));
			trigBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*i),h*0.6,w*0.1,h*0.1)));
			cueBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*i),h*0.7,w*0.1,h*0.1)));
			volBar[i].warning=0.65;
			volBar[i].critical=0.8;
			lbls.add(StaticText.new(window,Rect((w*0.15)+((w*0.5)*i),h*0.05,w*0.2,h*0.1))
				.string_("Drop .mp3, .wav or .aiff files here")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window,Rect((w*0.1)+((w*0.7)*i),h*0.4,w*0.1,h*0.1))
				.string_("Time")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window,Rect((w*0.1)+((w*0.7)*i),h*0.5,w*0.1,h*0.1))
				.string_("Cuts")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window,Rect((w*0.1)+((w*0.7)*i),h*0.6,w*0.1,h*0.1))
				.string_("Chops")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window,Rect((w*0.1)+((w*0.7)*i),h*0.7,w*0.1,h*0.1))
				.string_("Cue Pos")
				.stringColor_(Color.yellow));
			labels.add(lbls);
		}}.fork(AppClock);

		labels.add(StaticText.new(window,Rect(w*0.4, 0, w*0.2,h*0.1))
			.string_("Drop Samples here")
			.stringColor_(Color.yellow));
	}

	update {arg param;
		var index=param[0];
		if(trackDragBox[index].string!="",{
			trackDragBox[index].string[0..trackDragBox[index].string.size-1].postln;
			decks[index].loadTrack(trackDragBox[index].string[0..trackDragBox[index].string.size-1]);
			trackDragBox[index].string_("");
		});
		if(sampleDragBox[index].string!="",{
			sampleDragBox[index].string[0..sampleDragBox[index].string.size-1].postln;
			samplePad.loadSample(sampleDragBox[index].string[0..sampleDragBox[index].string.size-1],index.asSymbol);
			sampleDragBox[index].string_("");
		});
		posBox[index].value_(param[1][\pos].round(0.01));
		rateBox[index].value_(param[1][\cutrate]);
		trigBox[index].value_(param[1][\retrig]);
		cueBox[index].value_(param[1][\cuePos]);
		volBar[index].value_((volBarVals[index]*0.8));
		mainMix.value_(((volBarVals[0]*((mixer.mainMix.neg+1)/2))+(volBarVals[1]*((mixer.mainMix+1)/2)))*0.8);
		window.view.background_(Color.grey(((mixer.mainMix.neg+1)/2)),0.5);
	}

}