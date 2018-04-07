SC_DJ

This sketch is built up around a strong 2 decks a mixer metaphor and can work as an out of the box program to allow manual beat matching, cuing, nudging and mixing of two tracks together via a MIDI controller. 

There are also auxiliary performance effects, such as the 'powerdown', glitching and variable playback rate chopping. 

The set up currently works best with a 4 output audio interface (2 for main out, 2 for cuing), although if you have a splitter for output and headphones you could just use 2 outputs.

LOADING

Put all the .sc files into your SCClassLibrary and recompile the library

RUNNING

To get started simply run DJ.new(4)

MAPPING YOUR OWN MIDI

Its highly unlikely your MIDI controller uses my default values, so you'll probably want to map your own. I use a Korg nanoControl, but you can you whatever you want. The amount of knobs, sliders and buttons necessary probably make this a good bet however. 

To map your own controls, click the MAP MIDI button in the top right hand corner. This will open a menu displaying all the available controls. The map a specific control, click the button on the GUI then trigger the control on your MIDI controller that you with to map to that function. They will now be joined. When you're down, click the SAVE MAP button in the top right and your map will be remembered when you restart the sketch next time. 

You may have to restart the program after editting MIDI controls.

FUNCTIONS

Most of the functions are self explanatory but heres a quick rundown of the ones that are not so,

Chops

	Mutes normal playback (normal playback does however continue) and plays back audio at the rate defined by the chops rate

Cuts
	The first 4 presses of this act as a tap tempo to provide a tempo for beat synchronous glitching. All subsequent presses will result in triggering the cut effect
	Mutes normal playback (normal playback does however continue) and plays back audio glitched at a rate defined by the cuts rate. This is quantised to fractions of a beat at the tempo supplied by the tapping. 
