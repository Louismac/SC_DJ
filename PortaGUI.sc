PortaGUI {
classvar s;
var mixer,decks,window,dragBox,loadButton,posBox,trigBox,rateBox,cueBox,volBar,volResponder,volBarVals,mainMix,labels;

*new {arg m,d;
^super.new.initPortaGUI(m,d);	
}	

initPortaGUI {arg m,d;
	s=Server.default;
	mixer=m;
	decks=d;
	window = SCWindow.new("YOU ARE ABOUT TO WITNESS THE STRENGTH OF STREET KNOWLEDGE",Rect(0,0, 500,300));
	window.front;
	window.alwaysOnTop_(true);
	dragBox=List.new(0);
	loadButton=List.new(0);
	posBox=List.new(0);
	rateBox=List.new(0);
	trigBox=List.new(0);
	cueBox=List.new(0);
	volBar=List.new(0);
	labels=[[0,0,0,0],[0,0,0,0]];
	volBarVals=[0,0];
	volResponder=OSCresponder(s.addr,'amp',{arg time,responder,msg;volBarVals[msg[4]]=msg[3]}).add;
	volBar.add(SCLevelIndicator.new(window,Rect(200,100,20,150)));
	volBar.add(SCLevelIndicator.new(window,Rect(300,100,20,150)));
	mainMix=SCLevelIndicator.new(window,Rect(240,100,40,180));
	mainMix.warning=0.65;
	mainMix.critical=0.8;

	{2.do{arg i;
		dragBox.add(SCTextView.new(window,Rect(75+(260*i), 20, 100,20)));
		// loadButton.add(SCButton.new(window,Rect(75+(260*i),60,100,20)));
		// loadButton[i].states_([ 
		// 	[ "LOAD", Color(1.0, 1.0, 1.0, 1.0), Color(0.0, 0.0, 0.0, 1.0) ]
		// ]);
		// loadButton[i].action_({
			// dragBox[i].string[1..dragBox[i].string.size-2].postln;
			// decks[i].loadTrack(dragBox[i].string[1..dragBox[i].string.size-2]);
			// dragBox[i].string_("");
		// });
		posBox.add(SCNumberBox.new(window, Rect(100+(260*i),100,50,20)));
		rateBox.add(SCNumberBox.new(window, Rect(100+(260*i),220,50,20)));
	    trigBox.add(SCNumberBox.new(window, Rect(100+(260*i),180,50,20)));
	    cueBox.add(SCNumberBox.new(window, Rect(100+(260*i),140,50,20)));
	    volBar[i].warning=0.65;
	    volBar[i].critical=0.8;
	    labels[i][0]=SCStaticText.new(window,Rect(40+(380*i),100,50,20)).string_("Time").stringColor_(Color.yellow);
	    labels[i][1]=SCStaticText.new(window,Rect(40+(380*i),220,50,20)).string_("Cut Rate").stringColor_(Color.yellow);
	    labels[i][0]=SCStaticText.new(window,Rect(40+(380*i),180,50,20)).string_("Trig Rate").stringColor_(Color.yellow);
	    labels[i][0]=SCStaticText.new(window,Rect(40+(380*i),140,50,20)).string_("Cue Pos").stringColor_(Color.yellow);
	}}.fork(AppClock);
}

update {arg param;
	var index=param[0];
	if(dragBox[index].string.size>0,{
		dragBox[index].string[1..dragBox[index].string.size-2].postln;
		decks[index].loadTrack(dragBox[index].string[1..dragBox[index].string.size-2]);
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