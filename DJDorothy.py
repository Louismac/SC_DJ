from dorothy import Dorothy
from tkinter import Tk, filedialog
import numpy as np
import librosa
from pythonosc import udp_client, dispatcher, osc_server
import threading
import json
from pathlib import Path

dot = Dorothy(1920,800)

class FolderBrowser:
    def __init__(self, dot, folder_path, x, y, width, height, max_per_page=10, load_callback=None):
        self.dot = dot
        self.folder = Path(folder_path)
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.max_per_page = max_per_page
        
        self.files = []
        self.current_page = 0
        self.selected_file = None
        self.button_height = 30
        self.button_spacing = 1
        self.load_callback = load_callback
        
        self.load_files()
        self.create_file_buttons()
        nav_y = self.y + self.max_per_page * (self.button_height + self.button_spacing) + 20
        
        self.dot.create_button(
            self.x, nav_y, 80, 40,
            text="< Prev",
            id="prev_page",
            on_release=self.prev_page
        )
        
        self.dot.create_button(
            self.x + 90, nav_y, 80, 40,
            text="Next >",
            id="next_page",
            on_release=self.next_page
        )
        
        # Page indicator
        page_text = f"Page {self.current_page + 1}/{self.total_pages}"
        self.dot.text(page_text, self.x + 180, nav_y + 20, size=14)


        self.dot.create_button(
            self.x, nav_y+40, 150, 50,
            text="Load Deck 1",
            id="load_deck1",
            on_release=self.load_to_deck_1
        )
        
        self.dot.create_button(
            self.x, nav_y+60+40, 150, 50,
            text="Load Deck 2",
            id="load_deck2",
            on_release=self.load_to_deck_2
        )

        self.dot.create_button(
            self.x, nav_y+120+40, 150, 50,
            text="refresh library",
            id="refresh",
            on_release=self.refresh
        )
    
    def load_files(self):
        """Load audio files from folder"""
        self.files = sorted([
            f for f in self.folder.glob('*')
            if f.suffix.lower() in ['.mp3', '.wav', '.aiff', '.aif']
        ])
        print(f"found {len(self.files)} audio files")
    
    @property
    def total_pages(self):
        return max(1, (len(self.files) - 1) // self.max_per_page + 1)
    
    def get_page_files(self):
        start = self.current_page * self.max_per_page
        end = start + self.max_per_page
        return self.files[start:end]
    
    def create_file_buttons(self):
        """Create buttons for current page"""
        page_files = self.get_page_files()
        self.file_buttons = []
        # File buttons
        for i, file_path in enumerate(page_files):
            y_pos = self.y + i * (self.button_height + self.button_spacing)
            
            self.file_buttons.append(self.dot.create_button(
                self.x, y_pos,
                self.width, self.button_height,
                text=file_path.name[:40],  # Truncate long names
                id=file_path,
                on_release=self.select_file
            ))

    
    def select_file(self, btn):
        """Select a file"""
        self.selected_file = btn.id
        print(f"Selected: {self.selected_file}")
    
    def prev_page(self,btn):
        if self.current_page > 0:
            self.current_page -= 1
            self.refresh()
    
    def next_page(self,btn):
        if self.current_page < self.total_pages - 1:
            self.current_page += 1
            self.refresh()
    
    def refresh(self,btn=None):
        """Recreate buttons for new page"""
        print("refresh!")
        self.load_files()
        for fb in self.file_buttons:
            self.dot.buttons.remove(fb)
        self.create_file_buttons()

    def load_to_deck_2(self,btn):
        """Load selected file to deck"""
        if self.selected_file:
            # Call your load function here
            print(f"Loading {self.selected_file.name} to deck {2}")
            self.load_callback(self.selected_file, 1)
        else:
            print("No file selected")
    
    def load_to_deck_1(self,btn):
        """Load selected file to deck"""
        if self.selected_file:
            # Call your load function here
            print(f"Loading {self.selected_file.name} to deck {1}")
            self.load_callback(self.selected_file, 0)
        else:
            print("No file selected")


class DJDorothy():

    def __init__(self):
        dot.start_loop(self.setup, self.draw)  
        
    def setup_osc_server(self):
        disp = dispatcher.Dispatcher()
        disp.map("/deck", self.handle_deck_update)
        disp.map("/mixer", self.handle_mixer_update)
        disp.map("/trackLoaded", self.handle_track_loaded)
        disp.map("/meter", self.handle_meter_update)

        server = osc_server.ThreadingOSCUDPServer(
            ("127.0.0.1", 5005), disp
        )
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        
    def handle_deck_update(self, address, deck_idx, pos, cue_pos, cutrate, retrig, rate, loop_end, loop_enabled):
        self.decks[deck_idx]['pos'] = pos
        self.decks[deck_idx]['cue'] = cue_pos
        self.decks[deck_idx]['rate'] = rate
        self.decks[deck_idx]['loop_end'] = loop_end
        self.decks[deck_idx]['loop_enabled'] = bool(loop_enabled)
        
    def handle_mixer_update(self, address, xfade):
        pass  # Could visualise crossfader position

    def handle_meter_update(self, address, reply_id, peak, rms):
        """Handle meter updates from SuperCollider
        reply_id: 0 = Deck 1, 1 = Deck 2, 2 = Main Mix
        """
        if reply_id == 0:
            self.decks[0]['peak'] = peak
            self.decks[0]['rms'] = rms
        elif reply_id == 1:
            self.decks[1]['peak'] = peak
            self.decks[1]['rms'] = rms
        elif reply_id == 2:
            self.main_peak = peak
            self.main_rms = rms

    def handle_track_loaded(self, address, deck_idx, success):
        if success:
            print(f"Deck {deck_idx} loaded successfully")
        else:
            print(f"Deck {deck_idx} failed to load")
    
    def load_library(self):
        print(self.library_path)
        if self.library_path.exists():
            print("loading")
            with open(self.library_path) as f:
                return json.load(f)
        return {}
    
    def save_library(self):
        with open(self.library_path, 'w') as f:
            json.dump(self.library, f, indent=2)
    
    def analyse_track(self, path, deck_idx):
        """Analyse audio file for BPM and create waveform"""
        try:
            y, sr = librosa.load(path, sr=44100, mono=True)
            
            # BPM detection
            tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
            print(tempo)
            # Create waveform (downsample for display)
            hop = len(y) // 600  
            waveform = np.array([
                np.max(np.abs(y[i:i+hop])) 
                for i in range(0, len(y), hop)
            ])
            print("waveform")
            # Find auto-cue (first strong transient after 1 sec)
            onset_env = librosa.onset.onset_strength(y=y, sr=sr)
            onsets = librosa.onset.onset_detect(
                onset_envelope=onset_env, sr=sr, units='time'
            )
            print("onsets")
            auto_cue = next((t for t in onsets if t > 1.0), 0)
            print("auto_cue")
            meta = {
                'waveform': waveform,
                'bpm': float(np.mean(tempo)),
                'auto_cue': auto_cue,
                'cue':0,
                'duration': len(y) / sr
            }
            if meta:
                self.library[str(path)] = {
                    'waveform': meta['waveform'].tolist(),
                    'bpm': float(meta['bpm']),
                    'duration': float(meta['duration']),
                    'cue': float(meta['auto_cue'])
                }
                self.save_library()
                print("save_library", deck_idx)
                deck = self.decks[deck_idx]
                deck['waveform'] = meta['waveform']
                deck['bpm'] = meta['bpm']
                deck['cue'] = meta['auto_cue']
                deck['duration'] = meta['duration']
                deck['rate'] = 1
                self.loading_complete(deck, path, deck_idx)
        except Exception as e:
            print(f"Analysis failed: {e}")
            return None
    
    def load_track(self, path, deck_idx):
        """Load track into deck"""
        #save old before loading new
        deck = self.decks[deck_idx]
        if not deck["path"] is None:
            old_path = deck['path']
            if old_path in self.library:
                self.library[old_path]['cue'] = deck["cue"]
                self.library[old_path]['rate'] = deck["rate"]
                self.save_library()
        path_str = str(path)
        print(f"loading {path} to deck {deck_idx}")
        # Check library
        if path_str in self.library:
            meta = self.library[path_str]
            deck['waveform'] = np.array(meta['waveform'])
            deck['bpm'] = meta['bpm']
            deck['duration'] = float(meta['duration'])
            deck['cue'] = float(meta['cue'])
            deck['rate'] = 1
            self.loading_complete(deck, path_str, deck_idx)
        else:
            # Analyse and save
            thread = threading.Thread(target = self.analyse_track, args = (path, deck_idx,),daemon=True)
            thread.start()
        deck['path'] = path_str
        if not deck['layer'] is None:
            dot.release_layer(deck['layer'])
        deck['layer'] = None
        print("set path")
        
    def loading_complete(self, deck, path_str, deck_idx):
        # Send to SuperCollider
        self.sc_client.send_message("/loadTrack", [deck_idx, path_str, deck['cue']])
        print("send_message")
    
    def draw_meter(self, x, y, w, h, peak, rms, color):
        """Draw a vertical VU meter"""
        # Convert to dB scale (0 to 1 linear -> -60dB to 0dB)
        def linear_to_db(val):
            if val <= 0:
                return -60
            return max(-60, 20 * np.log10(val))

        def db_to_height(db):
            # Map -60dB to 0dB -> 0 to h
            normalized = (db + 60) / 60
            return normalized * h

        # Background
        dot.fill((20, 20, 20))
        dot.stroke((60, 60, 60))
        dot.rectangle((x, y), (x + w, y + h))

        # RMS level (main meter body)
        rms_db = linear_to_db(rms)
        rms_h = db_to_height(rms_db)

        if rms_h > 0:
            # Color zones: green < -18dB, yellow < -6dB, red > -6dB
            zones = []
            rms_top = y + h - rms_h

            if rms_h > 0:  # Green zone
                zones.append(((20, 200, 20), rms_top, rms_h))

            # Draw zones
            for zone_color, zone_y, zone_h in zones:
                dot.fill(zone_color)
                dot.no_stroke()
                dot.rectangle((x + 2, zone_y), (x + w - 2, zone_y + zone_h))

        # Peak indicator (thin line)
        peak_db = linear_to_db(peak)
        peak_h = db_to_height(peak_db)
        if peak_h > 0:
            peak_y = y + h - peak_h
            dot.stroke((255, 255, 255))
            dot.line((x, peak_y), (x + w, peak_y))

    def draw_deck(self, deck_idx, x, y, w, h):
        """Draw single deck waveform + playhead + cue"""
        deck = self.decks[deck_idx]

        # Reserve space for meter on the right
        meter_width = 30
        waveform_width = w - meter_width - 10

        # Background
        dot.no_stroke()

        if deck['waveform'] is not None:
            wf = deck['waveform']
            
            # Draw waveform
            if deck['layer'] is None:
                print("no layer")
                deck['layer']  = dot.get_layer()
                with dot.layer(deck['layer']):
                    dot.set_stroke_weight(3)
                    dot.stroke((255,255,0,200) if deck_idx == 0 else (255,0,255,200) )
                    for i, amp in enumerate(wf):
                        xi = int(x + ((i / len(wf))*waveform_width))
                        h_bar = amp * (h * 0.8)
                        yi = y + h/2 - h_bar/2
                        dot.line((xi, yi), ((xi), yi+h_bar))
                        #dot.circle((xi,yi),5)
            else:
                dot.draw_layer(deck['layer'])

            # Loop region (semi-transparent overlay)
            duration = deck['duration']
            if deck['loop_enabled'] and deck['loop_end'] > deck['cue']:
                loop_start_x = x + ((deck['cue'] / duration) * waveform_width)
                loop_end_x = x + ((deck['loop_end'] / duration) * waveform_width)
                # Draw loop region as semi-transparent rectangle
                dot.fill((0, 255, 255, 30))  # Cyan with low alpha
                dot.no_stroke()
                dot.rectangle((loop_start_x, y), (loop_end_x, y + h))

                # Draw loop boundaries
                dot.stroke((0, 255, 255))  # Cyan
                dot.line((loop_start_x, y), (loop_start_x, y + h))  # Loop start (cue point)
                dot.line((loop_end_x, y), (loop_end_x, y + h))  # Loop end

            # Cue point (yellow line)
            dot.stroke(dot.yellow)
            if deck['cue'] > 0:
                dot.set_stroke_weight(5)
                cue_x = x + ((deck['cue'] / duration) * waveform_width)
                dot.line((cue_x, y), (cue_x, y + h))

            dot.stroke(dot.red)
            # Playhead (red line)
            if deck['pos'] > 0:
                dot.set_stroke_weight(5)
                play_x = x + ((deck['pos'] / duration) * waveform_width)
                dot.line((play_x, y), (play_x, y + h))

        # Label
        dot.fill(dot.yellow if deck_idx == 0 else dot.magenta)
        bpm = deck['bpm']*deck["rate"]
        label = f"Deck {deck_idx+1}"
        if deck['path']:
            label += f" - {Path(deck['path']).stem}"
        if bpm:
            label += f" ({bpm:.1f} BPM)"
        dot.text(label, x + 10, y + 20, size=14)

        # Draw meter
        meter_x = x + waveform_width + 10
        meter_color = dot.yellow if deck_idx == 0 else dot.magenta
        if deck_idx <2:
            self.draw_meter(meter_x, y + 30, meter_width, h - 40, deck['peak'], deck['rms'], meter_color)
    
    def setup(self):
        dot.background((30, 30, 35))
        
        # OSC setup
        self.sc_client = udp_client.SimpleUDPClient("127.0.0.1", 57120)
        self.setup_osc_server()
        
        # State
        self.decks = [
            {'waveform': None, 'pos': 0, 'cue': 0, 'path': None, 'bpm': 0, 'peak': 0, 'rms': 0, 'loop_end': 0, 'loop_enabled': False, "layer":None, "rate":1},
            {'waveform': None, 'pos': 0, 'cue': 0, 'path': None, 'bpm': 0, 'peak': 0, 'rms': 0, 'loop_end': 0, 'loop_enabled': False,"layer":None, "rate":1}
        ]
        self.main_peak = 0
        self.main_rms = 0
        self.library_path = Path.home() / ".dj_library.json"
        self.library = self.load_library()
        
        # UI state
        self.drag_cue = None  # Which deck's cue is being dragged

        music_folder = Path.home() / "Music" / "dj"
        self.browser_w = 300
        self.browser = FolderBrowser(
            dot, 
            music_folder,
            x=0, y=0,
            width=self.browser_w, height=dot.height,
            max_per_page=15,load_callback=self.load_track
        )
        dot.on_mouse_press = self.mouse_pressed
       
        
    def draw(self):
        dot.background((30, 30, 35))
        dot.update_buttons()
        dot.draw_buttons()

        # Reserve space for main meter on right side
        main_meter_width = 40
        main_meter_padding = 10
        available_width = dot.width - self.browser_w - main_meter_width - main_meter_padding

        # Draw both decks
        deck_h = (dot.height - 60) // 2
        try:
            self.draw_deck(0, self.browser_w, 0, available_width, deck_h)
            self.draw_deck(1, self.browser_w, deck_h, available_width, deck_h)

            # Draw main mix meter
            main_meter_x = self.browser_w + available_width + main_meter_padding
            self.draw_meter(main_meter_x, 40, main_meter_width, dot.height - 80,
                          self.main_peak, self.main_rms, (255, 255, 255))

            # Main meter label
            dot.fill((255, 255, 255))
            dot.text("MAIN", main_meter_x - 5, 25, size=12)

        except Exception as err:
            import traceback
            print(traceback.format_exc())
            print(err)
    
    def mouse_pressed(self,x,y,b):
        """Click to set cue point"""
        deck_h = (dot.height - 60) // 2

        # Account for meters in layout
        main_meter_width = 40
        main_meter_padding = 10
        available_width = dot.width - 200 - main_meter_width - main_meter_padding
        meter_width = 30

        for i in range(2):
            y_start = 0 if i == 0 else deck_h
            # Check if click is in waveform area (not in meter area)
            if dot.mouse_x > self.browser_w and dot.mouse_x < (self.browser_w + available_width - meter_width - 10):
                # Calculate position in track
                rel_x = dot.mouse_x - self.browser_w
                i = 0 if dot.mouse_y < deck_h else 1
                if self.decks[i]['waveform'] is not None:
                    waveform_width = available_width - meter_width - 10
                    duration = self.decks[i]['duration']
                    cue_time = float((rel_x/waveform_width) * duration)
                    self.decks[i]['cue'] = cue_time
                    print(f"setting cue time {cue_time} for deck {i}")
                    # Send to SC
                    self.sc_client.send_message("/setCue", [i, cue_time])

                    # Save to library
                    path = self.decks[i]['path']
                    if path in self.library:
                        self.library[path]['cue'] = cue_time
                        self.save_library()


if __name__ == "__main__":
    DJDorothy()