DJ{

	var mixer,<>decks,midiControl,gui,samplePad;

	*new {arg c;
		^super.new.initDJ(c);
	}

	*initSynthDefs {
		Track.initSynthDefs;
		Mixer.initSynthDefs;
		SamplePad.initSynthDefs;
	}

	initDJ {arg chans;
		var r,buses;
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
				buses=();

				2.do{arg i;
					if(chans==2,{
						buses[i.asSymbol]=Bus.audio(Server.default,1);
						["making single channel bus"].postln;
					},{
						buses[i.asSymbol]=Bus.audio(Server.default,2);
						["making 2 channel bus"].postln;
					});
					0.5.wait;
					decks.add(Deck.new(i,buses[i.asSymbol],chans));
				};

				//Samplebus
				if(chans==2,{
					buses[\sample]=Bus.audio(Server.default,1);
					["making single channel bus"].postln;
				},{
					buses[\sample]=Bus.audio(Server.default,2);
					["making 2 channel bus"].postln;
				});

				0.5.wait;

				mixer=Mixer.new(buses,chans);

				samplePad=SamplePad.new;

				midiControl.onceServerBooted([mixer,decks,samplePad]);
				gui.onceServerBooted([mixer,decks,samplePad,midiControl]);
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