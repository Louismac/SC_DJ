// DJ System Refactored - Usage Example
// This example shows how to use the refactored DJ system with improved error handling

(

// 1. Initialize the DJ system - this will boot the server automatically
~dj = DJ_refactored.new(4);  // Use 2 for stereo, 4 for quad

// The system initializes asynchronously. Wait for it to complete:
fork {
	// Wait for initialization to complete
	while { ~dj.isInitialized.not } {
		0.1.wait;
	};

	"DJ System ready!".postln;
	("Decks initialized: " ++ ~dj.decks.size).postln;
	"Mixer initialized: yes".postln;

	// 2. Load tracks onto decks
	// Make sure to use absolute paths to your audio files
	// ~dj.loadTrack(0, "/path/to/your/track1.wav");
	// ~dj.loadTrack(1, "/path/to/your/track2.wav");
};
)

Platform.userAppSupportDir

// 3. Control the decks (after initialization)
(
// Start/stop deck 0
~dj.decks[0].startstop;

// Set volume for deck 0 (0-1)
~dj.decks[0].setVol(0.8);

// Update pitch for deck 0 (-4 to 4)
~dj.decks[0].updatePitch(1.05);

// Set cue point on current position
~dj.decks[0].setCue(true);
)

// 4. Control the mixer
(
// Crossfade between decks (-1 = deck 0, 1 = deck 1, 0 = center)
~dj.mixer.updateMix(0);

// EQ controls (channel, value in dB -12 to 12)
~dj.mixer.updateLo(0, -3);   // Low EQ for deck 0
~dj.mixer.updateMid(0, 2);    // Mid EQ for deck 0
~dj.mixer.updateHi(0, 0);     // High EQ for deck 0
)

// 5. Clean shutdown
(
~dj.cleanup;  // Clean up resources
~dj.quit;     // Quit server
)

// === Additional Features ===

// Skip controls
~dj.decks[0].skipForward;    // Speed up temporarily
~dj.decks[0].skipBackwards;  // Reverse temporarily
~dj.decks[0].skipSlow;       // Slow down temporarily
~dj.decks[0].stopSkip;       // Return to normal speed

// Power down effect
~dj.decks[0].powerDown;

// Loop control
~dj.decks[0].track.setLoop(1);  // Enable loop
~dj.decks[0].track.setLoop(0);  // Disable loop

// === Error Handling Examples ===

// All operations are now protected with error handling
(
// This will safely fail if file doesn't exist
~dj.loadTrack(0, "/nonexistent/file.wav");
// Error message will be posted but system continues running
)

// === Migration Notes ===
/*
Key differences from original code:

1. Error Handling:
   - All operations wrapped in DJError.handle
   - System won't crash on errors
   - Clear error messages

2. Resource Management:
   - Automatic cleanup of buffers, synths, buses
   - Memory leak prevention
   - Clean shutdown process

3. Validation:
   - File existence checks
   - Server state verification
   - Parameter range validation

4. Better Organization:
   - Separated error handling (DJError)
   - Resource management (DJResourceManager)
   - Cleaner class structure

5. Safer Initialization:
   - Server boot verification
   - Component initialization checks
   - Graceful failure handling

To migrate existing code:
- Replace DJ with DJ_refactored
- Replace Track with Track_refactored
- Replace Deck with Deck_refactored
- Replace Mixer with Mixer_refactored
- Add error handling to custom code
*/