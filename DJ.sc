DJ{

var mixer,<>decks,midiControl,gui;

*new {
^super.new.initDJ;
}

*initSynthDefs {
	Track.initSynthDefs;
	Mixer.initSynthDefs;
}
	
initDJ {
	decks=List.new(0);
	2.do{arg i;
		decks.add(Deck.new(i))
	};
	mixer=Mixer.new(decks);
	midiControl=DJMIDI.new(mixer,decks); 	
	
	gui=DJGUI.new([mixer,decks,midiControl]);
	this.updateGUI;
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