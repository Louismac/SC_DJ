DJOSC {
    var mixer, decks, samplePad, midi, oscSender;
    var sendRoutine;

    *new {
        ^super.new.initOSC;
    }

    initOSC {
        oscSender = NetAddr("127.0.0.1", 5005);
        this.setupReceivers;
    }

    setupReceivers {
        OSCdef(\loadTrack, {|msg|
            var deckIndex = msg[1].asInteger;
            var path = msg[2].asString;
			var cue = msg[3].asFloat;
			[deckIndex,path,decks[deckIndex]].postln;
            try {
                decks[deckIndex].loadTrack(path, cue*64);
                oscSender.sendMsg('/trackLoaded', deckIndex, 1);
            } {|error|
                error.postln;
                oscSender.sendMsg('/trackLoaded', deckIndex, 0);
            };
        }, '/loadTrack');

        OSCdef(\setCue, {|msg|
            var deckIndex = msg[1].asInteger;
            var cuePos = msg[2].asFloat;
            decks[deckIndex].setCueFromOSC(cuePos);
        }, '/setCue');

        OSCdef(\loadSample, {|msg|
            var sampleIndex = msg[1].asInteger;
            var path = msg[2].asString;
            samplePad.loadSample(path, sampleIndex.asSymbol);
        }, '/loadSample');

        OSCdef(\meter, {|msg|
            var replyID = msg[2].asInteger;
            var peak = msg[3].asFloat;
            var rms = msg[4].asFloat;
            // Forward meter data to Python GUI
            oscSender.sendMsg('/meter', replyID, peak, rms);
        }, '/meter');
    }

    onceServerBooted {arg modules;
        mixer = modules[0];
        decks = modules[1];
        samplePad = modules[2];
        midi = modules[3];
        this.startSending;
        "OSC Server ready on port 57120".postln;
    }

    startSending {
        sendRoutine = {inf.do{
            (1/30).wait;
            try {
                if(decks.notNil and: {decks.size >= 2}, {
                    2.do{arg i;
                        var vals = decks[i].getVals;
                        oscSender.sendMsg('/deck', i,
                            vals[\pos],
                            vals[\cuePos],
                            vals[\cutrate],
                            vals[\retrig],
							vals[\rate],
							vals[\loopEnd],
							if(vals[\loopEnabled], 1, 0)
                        );
                    };
                    oscSender.sendMsg('/mixer',
                        mixer.mainMix
                    );
                });
            } {|error|
                // Silent fail for GUI updates
            };
        }}.fork(AppClock);
    }

    cleanup {
        sendRoutine.stop;
        OSCdef(\loadTrack).free;
        OSCdef(\setCue).free;
        OSCdef(\loadSample).free;
        OSCdef(\meter).free;
    }
}