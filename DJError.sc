DJError {
	classvar <>verbose = true;

	*handle { arg function, errorMessage = "An error occurred", onError;
		var result;
		try {
			result = function.value;
		} { arg error;
			if(verbose) {
				("DJ Error: " ++ errorMessage).error;
				error.reportError;
			};
			onError !? { onError.value(error) };
			result = nil;
		};
		^result;
	}

	*assert { arg condition, message = "Assertion failed";
		if(condition.not) {
			Error(message).throw;
		};
	}

	*checkServer { arg server;
		server = server ? Server.default;
		this.assert(server.serverRunning, "Server is not running");
		^server;
	}

	*checkNil { arg object, name = "Object";
		this.assert(object.notNil, name ++ " is nil");
		^object;
	}

	*checkFile { arg path;
		this.assert(File.exists(path), "File does not exist: " ++ path);
		^path;
	}
}