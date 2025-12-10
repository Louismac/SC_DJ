import pandas as pd
from pydub import AudioSegment
from pydub.silence import detect_nonsilent
from pydub.effects import normalize
import os
import json

def split_audio_from_csv_multifile(audio_files, csv_file, output_dir="split_songs", 
                                    silence_thresh=-40, min_silence_len=2000,
                                    duration_tolerance=0.15, state_file="progress.json"):
    """
    Split multiple audio files based on CSV track list with progress tracking.
    
    Args:
        audio_files: List of audio file paths in order
        csv_file: Path to CSV with track information
        output_dir: Directory to save split files
        silence_thresh: Silence threshold in dBFS
        min_silence_len: Minimum silence length in ms
        duration_tolerance: Allowed duration mismatch as fraction
        state_file: JSON file to track progress between files
    """
    
    # Create output directory
    os.makedirs(output_dir, exist_ok=True)
    
    # Load or initialise state
    if os.path.exists(state_file):
        with open(state_file, 'r') as f:
            state = json.load(f)
        start_idx = state['next_track_index']
        print(f"Resuming from track {start_idx + 1}")
    else:
        start_idx = 0
        state = {'next_track_index': 0, 'completed_files': []}
    
    # Load CSV
    print(f"Loading track list from {csv_file}...")
    df = pd.read_csv(csv_file)
    total_tracks = len(df)
    
    current_track_idx = start_idx
    
    for file_num, audio_file in enumerate(audio_files):
        if audio_file in state.get('completed_files', []):
            print(f"Skipping already processed file: {audio_file}")
            continue
        
        print(f"\n{'='*60}")
        print(f"Processing file {file_num + 1}/{len(audio_files)}: {audio_file}")
        print(f"Starting at track {current_track_idx + 1}/{total_tracks}")
        print(f"{'='*60}\n")
        
        # Load audio file
        print(f"Loading {audio_file}...")
        audio = AudioSegment.from_file(audio_file)
        audio_duration_ms = len(audio)
        
        # Detect non-silent chunks
        print("Detecting songs...")
        nonsilent_ranges = detect_nonsilent(
            audio,
            min_silence_len=min_silence_len,
            silence_thresh=silence_thresh
        )
        
        print(f"Found {nonsilent_ranges} audio segments in this file")
        
        # Calculate remaining expected duration
        remaining_tracks = df.iloc[current_track_idx:]
        cumulative_expected = 0
        tracks_in_file = []
        
        # Estimate how many tracks fit in this file
        for i, (idx, row) in enumerate(remaining_tracks.iterrows()):
            track_duration = row['Duration (ms)']
            # Account for gaps (estimate 3 seconds between tracks)
            cumulative_expected += track_duration + 500
            
            if cumulative_expected <= audio_duration_ms + 30000:  # 30s buffer
                tracks_in_file.append(idx)
            else:
                break
        
        print(f"Expecting approximately {len(tracks_in_file)} tracks in this file")
        
        # Match detected segments to expected tracks
        matched_segments = []
        expected_start = 0
        
        for local_idx, track_idx in enumerate(tracks_in_file):
            row = df.iloc[track_idx]
            expected_duration = row['Duration (ms)']
            
            # Find best matching segment
            best_match = None
            best_score = float('inf')
            
            for start, end in nonsilent_ranges:
                if (start, end) in matched_segments:
                    continue
                
                detected_duration = end - start
                duration_error = abs(detected_duration - expected_duration) / expected_duration
                start_error = abs(start - expected_start) / max(expected_start, 1000)
                
                # Combined score
                score = duration_error * 2 + start_error * 0.5
                
                if score < best_score and duration_error < duration_tolerance:
                    best_score = score
                    best_match = (start, end)
            
            if best_match:
                matched_segments.append(best_match)
                expected_start = best_match[1] + 500  # Expect 3s gap
            else:
                matched_segments.append(None)
        
        # Export each segment
        for local_idx, track_idx in enumerate(tracks_in_file):
            row = df.iloc[track_idx]
            track_name = row['Track Name']
            artist_name = row['Artist Name(s)']
            expected_duration = row['Duration (ms)']
            segment_range = matched_segments[local_idx]
            
            if segment_range is None:
                print(f"⚠️  Could not match: {track_name} - {artist_name}")
                continue
            
            start, end = segment_range
            detected_duration = end - start
            duration_diff = (detected_duration - expected_duration) / expected_duration * 100
            
            # Create safe filename
            safe_name = f"{track_name} - {artist_name}"
            safe_name = "".join(c for c in safe_name if c.isalnum() or c in (' ', '-', '_', ',')).strip()
            safe_name = safe_name[:200]
            output_path = os.path.join(output_dir, f"{track_idx+1:03d}_{safe_name}.wav")
            
            # Extract and normalise segment
            print(f"Exporting [{track_idx+1}/{total_tracks}]: {track_name} - {artist_name}")
            print(f"  Duration: Expected {expected_duration/1000:.1f}s, Got {detected_duration/1000:.1f}s ({duration_diff:+.1f}%)")
            
            segment = audio[start:end]
            segment = normalize(segment)
            segment.export(output_path, format="wav", bitrate="320k")
            
            current_track_idx += 1
        
        # Save progress
        state['next_track_index'] = current_track_idx
        state['completed_files'].append(audio_file)
        with open(state_file, 'w') as f:
            json.dump(state, f, indent=2)
        
        print(f"\n✓ Completed file {file_num + 1}. Progress saved.")
        print(f"Processed tracks {start_idx + 1} to {current_track_idx}")
    
    print(f"\n{'='*60}")
    print(f"✓ All files complete! Total tracks processed: {current_track_idx}")
    print(f"Files saved to {output_dir}/")
    print(f"{'='*60}")
    
    # Clean up state file
    if os.path.exists(state_file):
        os.remove(state_file)

# Example usage
if __name__ == "__main__":
    audio_files = [
        # "/Users/louisbusby/Music/youtube-rips/TOplaylist101225-1.wav",
        #"/Users/louisbusby/Music/youtube-rips/TOplaylist101225-2.wav"
        #"/Users/louisbusby/Music/youtube-rips/TOplaylist101225-3.wav"
        "/Users/louisbusby/Music/youtube-rips/TOplaylist101225-4.wav"
    ]
    
    split_audio_from_csv_multifile(
        audio_files,
        "track-4.csv",
        output_dir="split_songs",
        silence_thresh=-45,
        min_silence_len=500,
        duration_tolerance=0.15,
        state_file="progress.json"
    )