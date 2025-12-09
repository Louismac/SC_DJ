DJ_refactored {
	var <>mixer, <>decks, midiControl, osc, <>samplePad;
	var resourceManager, serverBootRoutine;
	var <isInitialized = false, <serverBooted = false;
	var channels, buses;

	*new { arg channels = 2;
		^super.new.initDJ(channels);
	}

	*initSynthDefs {
		DJError.handle({
			Track_refactored.initSynthDefs;
			Mixer_refactored.initSynthDefs;
			SamplePad_refactored.initSynthDefs;
			"SynthDefs initialized".postln;
		}, "Failed to initialize SynthDefs");
	}

	initDJ { arg chans;
		channels = chans;
		resourceManager = DJResourceManager.new;

		DJError.assert(
			(channels == 2) || (channels == 4),
			"Channels must be 2 or 4"
		);

		// Initialize collections early
		decks = List.new(0);

		this.initComponents;
		this.setupServer;
		^this;
	}

	setupServer {
		var server = Server.default;

		if(server.serverRunning.not) {
			"Setting up server...".postln;

			server.options.numOutputBusChannels = channels;
			server.options.numInputBusChannels = channels;
			server.options.memSize = 65536;
			server.options.numBuffers = 2048;

			server.waitForBoot({
				serverBooted = true;
				"Server booted successfully".postln;
				this.onServerBooted;
			}, 30, {
				"Server boot failed!".error;
				this.cleanup;
			});
		} {
			serverBooted = true;
			this.onServerBooted;
		};
	}

	initComponents {
		DJError.handle({

			if(midiControl.isNil) {
				midiControl = DJMIDI.new;
			};

			if(osc.isNil) {
				osc = DJOSC.new;
			};

		}, "Failed to initialize components");
	}

	onServerBooted {
		if(serverBooted.not) { ^nil };

		DJError.handle({
			var server = Server.default;

			// Initialize SynthDefs first
			this.class.initSynthDefs;
			server.sync;

			// Create buses
			buses = this.createBuses;
			server.sync;

			// Initialize decks
			"Initializing decks...".postln;
			2.do { arg i;
				var bus = buses[i.asSymbol];
				var deck = Deck_refactored.new(i, bus, channels, resourceManager);
				decks.add(deck);
				("Deck " ++ i ++ " initialized with bus: " ++ bus).postln;
			};

			// Initialize mixer with the buses
			"Initializing mixer...".postln;
			mixer = Mixer_refactored.new(buses, channels, resourceManager);

			// Initialize sample pad
			"Initializing sample pad...".postln;
			samplePad = SamplePad_refactored.new(resourceManager);

			server.sync;

			if(midiControl.notNil) {
				midiControl.onceServerBooted([mixer, decks, samplePad]);
			};

			if(osc.notNil) {
				osc.onceServerBooted([mixer, decks, samplePad]);
			};



			isInitialized = true;
			"DJ system initialized successfully".postln;
			"  - Decks: " ++ decks.size ++ " initialized".postln;
			"  - Mixer: initialized".postln;
			"  - Sample Pad: initialized".postln;
		}, "Failed to initialize after server boot", { this.cleanup });
	}

	createBuses {
		var buses = ();
		var server = Server.default;

		2.do { arg i;
			var numChannels = if(channels == 2, 1, 2);
			buses[i.asSymbol] = resourceManager.registerBus(
				Bus.audio(server, numChannels)
			);
			("Created bus for deck " ++ i).postln;
		};

		buses[\sample] = resourceManager.registerBus(
			Bus.audio(server, if(channels == 2, 1, 2))
		);
		"Created sample bus".postln;

		^buses;
	}

	loadTrack { arg deckIndex, path;
		DJError.handle({
			DJError.assert(deckIndex < decks.size, "Invalid deck index");
			DJError.checkFile(path);
			decks[deckIndex].loadTrack(path);
			("Track loaded on deck " ++ deckIndex).postln;
		}, "Failed to load track: " ++ path);
	}

	cleanup {
		"Cleaning up DJ system...".postln;

		if(resourceManager.notNil) {
			resourceManager.cleanup;
		};
		isInitialized = false;
		serverBooted = false;
	}

	quit {
		this.cleanup;
		if(Server.default.serverRunning) {
			Server.default.quit;
		};
	}
}