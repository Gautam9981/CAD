#!/usr/bin/env python3
"""
Comment Stripper for CAD Application
Removes all comments (//, /* ... */, /** ... */) from Java source files
while preserving code structure and strings.
"""

import os
import re
import sys
from pathlib import Path


class CommentStripper:
    def __init__(self, src_dir, exclude_patterns=None):
        self.src_dir = src_dir
        self.exclude_patterns = exclude_patterns or []
        self.files_processed = 0
        self.comments_removed = 0
        
    def should_exclude(self, file_path):
        """Check if file should be excluded based on patterns"""
        path_str = str(file_path)
        for pattern in self.exclude_patterns:
            if pattern in path_str:
                return True
        return False
    
    def strip_comments(self, content):
        """Remove all comments from Java source code"""
        # Regex to match strings (ignore contents) or comments
        # Group 1: Line comment // ...
        # Group 2: Block comment /* ... */ (includes Javadoc /** ... */)
        # Group 3: String literal "..."
        pattern = r'(//[^\n]*)|(/\*[\s\S]*?\*/)|("(?:\\.|[^"\\])*")'
        
        count = 0
        
        def replacer(match):
            nonlocal count
            # If it's a comment (Group 1 or 2), replace with empty string (or newline for line comments to preserve line numbers if desired, but user said 'delete')
            # Actually, replacing line comment with nothing might merge lines if not careful, but usually // is at end or on own line.
            # Ideally:
            # - // comment \n -> \n (preserve newline)
            # - /* ... */ -> " " or "" depending on context. standard is usually space to avoid merging code.
            
            if match.group(1): # Line comment
                count += 1
                return "" 
            elif match.group(2): # Block comment
                count += 1
                return " " # Replace with space to avoid adhering tokens
            else: # String literal
                return match.group(3) # Keep string as is
        
        cleaned = re.sub(pattern, replacer, content)
        
        # Clean up multiple blank lines (more than 2 consecutive)
        cleaned = re.sub(r'\n{3,}', '\n\n', cleaned)
        
        return cleaned, count
    
    def process_file(self, file_path):
        """Process a single Java file"""
        try:
            # Read file
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Strip Comments
            cleaned, count = self.strip_comments(content)
            
            # Write back if changed (or force write to ensure cleanup)
            # Because we might change indentation or whitespace, good to write if clean different
            if content != cleaned:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(cleaned)
                print(f"✓ {file_path.relative_to(self.src_dir)}: Removed {count} comment(s)")
                self.comments_removed += count
                self.files_processed += 1
            
        except Exception as e:
            print(f"✗ Error processing {file_path}: {e}", file=sys.stderr)
    
    def process_all(self):
        """Process all Java files in the source directory"""
        java_files = list(Path(self.src_dir).rglob("*.java"))
        
        print(f"Found {len(java_files)} Java files")
        
        for java_file in sorted(java_files):
            if not self.should_exclude(java_file):
                self.process_file(java_file)
        
        print(f"\n{'='*60}")
        print(f"Summary:")
        print(f"  Files processed: {self.files_processed}")
        print(f"  Comments removed: {self.comments_removed}")
        print(f"{'='*60}")


if __name__ == "__main__":
    # Resolve paths relative to this script file
    script_dir = Path(__file__).resolve().parent
    # Assuming script is in docs/, so src is in ../src/main/java
    src_dir = script_dir.parent / "src" / "main" / "java"
    
    # User requested to strip ALL comments, so no exclusions
    exclude_patterns = []
    
    print("=" * 60)
    print("Universal Comment Stripper")
    print("=" * 60)
    print(f"Source directory: {src_dir}")
    print()
    
    stripper = CommentStripper(src_dir, exclude_patterns)
    stripper.process_all()
    
    print("\nDone! All comments have been stripped.")
