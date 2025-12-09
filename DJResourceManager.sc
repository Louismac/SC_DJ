DJResourceManager {
	var <buffers, <synths, <buses, <routines;

	*new {
		^super.new.init;
	}

	init {
		buffers = IdentitySet.new;
		synths = IdentitySet.new;
		buses = IdentitySet.new;
		routines = IdentitySet.new;
	}

	registerBuffer { arg buffer;
		if(buffer.notNil) {
			buffers.add(buffer);
		};
		^buffer;
	}

	registerSynth { arg synth;
		if(synth.notNil) {
			synths.add(synth);
		};
		^synth;
	}

	registerBus { arg bus;
		if(bus.notNil) {
			buses.add(bus);
		};
		^bus;
	}

	registerRoutine { arg routine;
		if(routine.notNil) {
			routines.add(routine);
		};
		^routine;
	}

	freeBuffer { arg buffer;
		if(buffers.includes(buffer)) {
			DJError.handle({
				buffer.free;
				buffers.remove(buffer);
			}, "Failed to free buffer");
		};
	}

	freeSynth { arg synth;
		if(synths.includes(synth)) {
			DJError.handle({
				synth.free;
				synths.remove(synth);
			}, "Failed to free synth");
		};
	}

	freeBus { arg bus;
		if(buses.includes(bus)) {
			DJError.handle({
				bus.free;
				buses.remove(bus);
			}, "Failed to free bus");
		};
	}

	stopRoutine { arg routine;
		if(routines.includes(routine)) {
			DJError.handle({
				routine.stop;
				routines.remove(routine);
			}, "Failed to stop routine");
		};
	}

	cleanup {
		"Cleaning up DJ resources...".postln;

		routines.do { arg routine;
			DJError.handle({ routine.stop }, "Failed to stop routine");
		};
		routines.clear;

		synths.do { arg synth;
			DJError.handle({ synth.free }, "Failed to free synth");
		};
		synths.clear;

		buffers.do { arg buffer;
			DJError.handle({ buffer.free }, "Failed to free buffer");
		};
		buffers.clear;

		buses.do { arg bus;
			DJError.handle({ bus.free }, "Failed to free bus");
		};
		buses.clear;
	}
}