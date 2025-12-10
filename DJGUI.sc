DJGUI{
	classvar s;
	var mixer, decks, midi, window, samplePad;
	var trackDragBox, sampleDragBox, posBox, trigBox, rateBox, cueBox, launchMap, reconnectMIDI;
	var volBar, volResponder, volBarVals, mainMix, labels;
	var w, h;
	var resourceManager;
	var <isInitialized = false;

	*new { arg resMgr;
		^super.new.initDJGUI(resMgr);
	}

	initDJGUI { arg resMgr;
		resourceManager = resMgr;

		DJError.handle({
			w = 600;
			h = 400;
			s = Server.default;

			this.createWindow;
			this.initializeContainers;
			this.createInterface;

			isInitialized = true;
			"GUI initialized".postln;
		}, "Failed to initialize GUI");
	}

	createWindow {
		DJError.handle({
			window = Window.new(
				"DJ Control Interface",
				Rect(0, 0, w, h)
			);
			window.front;
			window.alwaysOnTop_(true);
			window.view.canReceiveDragHandler = { true };
			window.onClose = { this.cleanup };

			"GUI window created".postln;
		}, "Failed to create window");
	}

	initializeContainers {
		trackDragBox = List.new(0);
		sampleDragBox = List.new(0);
		posBox = List.new(0);
		rateBox = List.new(0);
		trigBox = List.new(0);
		cueBox = List.new(0);
		volBar = List.new(0);
		labels = List.new(0);
		volBarVals = [0, 0];
	}

	createInterface {
		DJError.handle({
			// Volume responder with error handling
			if(s.serverRunning) {
				volResponder = OSCFunc({ arg msg, time, addr, recvPort;
					DJError.handle({
						if(msg[4].notNil && msg[3].notNil) {
							volBarVals[msg[4]] = msg[3];
						};
					}, "Volume responder error");
				}, '/amp', s.addr);
			};

			// Create volume bars
			volBar.add(LevelIndicator.new(window, Rect(w*0.375, h/3, w*0.05, h*0.5)));
			volBar.add(LevelIndicator.new(window, Rect(w*0.575, h/3, w*0.05, h*0.5)));

			// Main mix indicator
			mainMix = LevelIndicator.new(window, Rect(w*0.45, h/3, w*0.1, h*0.6));
			mainMix.warning = 0.65;
			mainMix.critical = 0.8;

			// Create deck controls
			{
				2.do { arg i;
					this.createDeckControls(i);
				};
			}.fork(AppClock);

			// Sample drop area label
			labels.add(StaticText.new(window, Rect(w*0.4, 0, w*0.2, h*0.1))
				.string_("Drop Samples here")
				.stringColor_(Color.yellow));

		}, "Failed to create interface");
	}

	createDeckControls { arg index;
		var lbls = List.new(0);

		DJError.handle({
			// Track drag box
			trackDragBox.add(
				TextField.new(window, Rect((w*0.15)+((w*0.5)*index), h*0.15, w*0.2, h*0.2))
					.string_("")
			);

			// Sample drag box
			sampleDragBox.add(
				TextField.new(window, Rect((w*0.375)+((w*0.15)*index), h*0.1, w*0.1, h*0.2))
					.string_("")
			);

			// Number boxes
			posBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*index), h*0.4, w*0.1, h*0.1)));
			rateBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*index), h*0.5, w*0.1, h*0.1)));
			trigBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*index), h*0.6, w*0.1, h*0.1)));
			cueBox.add(NumberBox.new(window, Rect((w*0.2)+((w*0.5)*index), h*0.7, w*0.1, h*0.1)));

			// Volume bar settings
			volBar[index].warning = 0.65;
			volBar[index].critical = 0.8;

			// Labels
			lbls.add(StaticText.new(window, Rect((w*0.15)+((w*0.5)*index), h*0.05, w*0.2, h*0.1))
				.string_("Drop audio files here")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window, Rect((w*0.1)+((w*0.7)*index), h*0.4, w*0.1, h*0.1))
				.string_("Time")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window, Rect((w*0.1)+((w*0.7)*index), h*0.5, w*0.1, h*0.1))
				.string_("Cuts")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window, Rect((w*0.1)+((w*0.7)*index), h*0.6, w*0.1, h*0.1))
				.string_("Chops")
				.stringColor_(Color.yellow));
			lbls.add(StaticText.new(window, Rect((w*0.1)+((w*0.7)*index), h*0.7, w*0.1, h*0.1))
				.string_("Cue Pos")
				.stringColor_(Color.yellow));

			labels.add(lbls);
		}, "Failed to create deck controls for index " ++ index);
	}

	onceServerBooted { arg modules;
		DJError.handle({
			mixer = modules[0];
			decks = modules[1];
			samplePad = modules[2];
			midi = modules[3];

			// Create MIDI control buttons
			if(midi.notNil) {
				launchMap = Button(window, Rect(w*0.9, 0, w*0.1, w*0.1))
					.states_([
						["Map MIDI", Color.yellow, Color.black],
						["Close", Color.black, Color.yellow]
					])
					.action_({
						DJError.handle({
							if(midi.isLearning) {
								midi.closeGUI;
							} {
								midi.launchGUI;
							};
						}, "MIDI mapping action failed");
					});

				reconnectMIDI = Button(window, Rect(0, 0, w*0.1, w*0.1))
					.states_([["Connect\nMIDI", Color.yellow, Color.black]])
					.action_({
						DJError.handle({
							midi.connectMIDIDevice;
						}, "MIDI reconnect failed");
					});
			};

			"GUI connected to modules".postln;
		}, "Failed to connect GUI to modules");
	}

	update { arg param;
		DJError.handle({
			var index = param[0];

			if(index.isNil || param[1].isNil) {
				^nil;
			};

			// Handle track loading via drag and drop
			if(trackDragBox[index].notNil && trackDragBox[index].string != "") {
				var path = trackDragBox[index].string;
				// Remove any trailing newline or space
				if(path.last == $\n || path.last == $ ) {
					path = path[0..path.size-2];
				};

				if(decks[index].notNil) {
					("Loading track: " ++ path).postln;
					decks[index].loadTrack(path);
					trackDragBox[index].string_("");
				};
			};

			// Handle sample loading via drag and drop
			if(sampleDragBox[index].notNil && sampleDragBox[index].string != "") {
				var path = sampleDragBox[index].string;
				// Remove any trailing newline or space
				if(path.last == $\n || path.last == $ ) {
					path = path[0..path.size-2];
				};

				if(samplePad.notNil) {
					("Loading sample: " ++ path).postln;
					samplePad.loadSample(index, path);
					sampleDragBox[index].string_("");
				};
			};

			// Update display values
			if(posBox[index].notNil && param[1][\pos].notNil) {
				posBox[index].value_(param[1][\pos].round(0.01));
			};

			if(rateBox[index].notNil && param[1][\cutrate].notNil) {
				rateBox[index].value_(param[1][\cutrate]);
			};

			if(trigBox[index].notNil && param[1][\retrig].notNil) {
				trigBox[index].value_(param[1][\retrig]);
			};

			if(cueBox[index].notNil && param[1][\cuePos].notNil) {
				cueBox[index].value_(param[1][\cuePos]);
			};

			// Update volume bars
			if(volBar[index].notNil) {
				volBar[index].value_((volBarVals[index] * 0.8).clip(0, 1));
			};

			// Update main mix indicator
			if(mainMix.notNil && mixer.notNil && mixer.mainMix.notNil) {
				var mixVal = ((volBarVals[0] * ((mixer.mainMix.neg + 1) / 2)) +
					(volBarVals[1] * ((mixer.mainMix + 1) / 2))) * 0.8;
				mainMix.value_(mixVal.clip(0, 1));

				// Update background color based on crossfade position
				window.view.background_(Color.grey(((mixer.mainMix.neg + 1) / 2)), 0.5);
			};
		}, "GUI update failed");
	}

	close {
		this.cleanup;
	}

	cleanup {
		DJError.handle({
			if(volResponder.notNil) {
				volResponder.free;
				volResponder = nil;
			};

			if(window.notNil) {
				window.close;
				window = nil;
			};

			isInitialized = false;
			"GUI cleaned up".postln;
		}, "Failed to cleanup GUI");
	}
}