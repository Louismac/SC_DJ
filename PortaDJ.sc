PortaDJ {

var mixer,<>decks,midiControl,gui;

*new {
^super.new.initPortaDJ;
}

*initSynthDefs {
	Track.initSynthDefs;
	Mixer.initSynthDefs;
}
	
initPortaDJ {
	decks=List.new(0);
	2.do{arg i;
		decks.add(Deck.new(i))
	};
	mixer=Mixer.new(decks);
	gui=PortaGUI.new(mixer,decks);
	this.updateGUI;
	midiControl=LMIDI.new(mixer,decks); 	
}

updateGUI {
{inf.do{
	(1/64).wait;
	2.do{arg i;
	gui.update([0,decks[0].getVals]);
	gui.update([1,decks[1].getVals]);
	}
}}.fork(AppClock);	
}

}