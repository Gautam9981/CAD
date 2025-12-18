#!/usr/bin/env python3
"""
Javadoc Comment Stripper for CAD Application
Removes all Javadoc comments (/** ... */) from Java source files
while preserving regular comments (// and /* */)
"""

import os
import re
import sys
from pathlib import Path


class JavadocStripper:
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
    
    def strip_javadoc(self, content):
        """Remove Javadoc comments from Java source code"""
        # Pattern to match Javadoc comments (/** ... */)
        # This is non-greedy and handles multi-line
        javadoc_pattern = r'/\*\*.*?\*/'
        
        # Count how many we're removing
        count = len(re.findall(javadoc_pattern, content, re.DOTALL))
        
        # Remove all Javadoc comments
        cleaned = re.sub(javadoc_pattern, '', content, flags=re.DOTALL)
        
        # Clean up multiple blank lines (more than 2 consecutive)
        cleaned = re.sub(r'\n{4,}', '\n\n\n', cleaned)
        
        return cleaned, count
    
    def process_file(self, file_path):
        """Process a single Java file"""
        try:
            # Read file
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Strip Javadoc
            cleaned, count = self.strip_javadoc(content)
            
            # Only write if something changed
            if count > 0:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(cleaned)
                print(f"✓ {file_path.relative_to(self.src_dir)}: Removed {count} Javadoc comment(s)")
                self.comments_removed += count
                self.files_processed += 1
            
        except Exception as e:
            print(f"✗ Error processing {file_path}: {e}", file=sys.stderr)
    
    def process_all(self):
        """Process all Java files in the source directory"""
        java_files = list(Path(self.src_dir).rglob("*.java"))
        
        print(f"Found {len(java_files)} Java files")
        print(f"Excluding patterns: {self.exclude_patterns}\n")
        
        for java_file in sorted(java_files):
            if not self.should_exclude(java_file):
                self.process_file(java_file)
        
        print(f"\n{'='*60}")
        print(f"Summary:")
        print(f"  Files processed: {self.files_processed}")
        print(f"  Javadoc comments removed: {self.comments_removed}")
        print(f"{'='*60}")


if __name__ == "__main__":
    # Resolve paths relative to this script file
    script_dir = Path(__file__).resolve().parent
    # Assuming script is in docs/, so src is in ../src/main/java
    src_dir = script_dir.parent / "src" / "main" / "java"
    
    # Exclude CLI package as requested by user
    exclude_patterns = ['/cli/', '\\cli\\']
    
    print("=" * 60)
    print("Javadoc Comment Stripper")
    print("=" * 60)
    print(f"Source directory: {src_dir}")
    print()
    
    stripper = JavadocStripper(src_dir, exclude_patterns)
    stripper.process_all()
    
    print("\nDone! The API documentation will now use code-based inferred descriptions.")
