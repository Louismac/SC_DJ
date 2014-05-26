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
	var r;
	r=Routine(
	{
		//Server needs to be booted after GUI made for drag and drop reasons
		Server.killAll;

		3.wait;
		midiControl=DJMIDI.new;

		gui=DJGUI.new;

		4.wait;

		Server.default.boot;

		4.wait;

		decks=List.new(0);
		2.do{arg i;
			decks.add(Deck.new(i))
		};
		mixer=Mixer.new(decks);
		midiControl.onceServerBooted(mixer,decks);
		gui.onceServerBooted([mixer,decks,midiControl]);
		this.updateGUI;
	});

	AppClock.play(r);
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