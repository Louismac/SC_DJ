// DJ System Refactored - Usage Example
// This example shows how to use the refactored DJ system with improved error handling
Server.killAll
(

// 1. Initialize the DJ system - this will boot the server automatically
~dj = DJ.new(4);  // Use 2 for stereo, 4 for quad

)

Track.initSynthDefs

a  = ()
a.size