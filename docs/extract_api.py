#!/usr/bin/env python3
"""
API Documentation Extractor for CAD Application
Extracts all method signatures, parameters, and return types from Java source files
"""

import os
import re
import json
from pathlib import Path

class JavaMethodExtractor:
    def __init__(self, src_dir):
        self.src_dir = src_dir
        self.api_data = {}
        
    def extract_package(self, content):
        """Extract package name from Java file"""
        match = re.search(r'package\s+([\w.]+);', content)
        return match.group(1) if match else "default"
        
    def extract_class_name(self, content):
        """Extract class name from Java file"""
        # Match class, interface, enum, record definition at the start of a line
        # Supports modifiers: public, abstract, final, static (for nested), and 'record' (modern Java)
        pattern = r'^\s*(?:public\s+|protected\s+|private\s+)?(?:abstract\s+|static\s+|final\s+)*(?:class|interface|enum|record)\s+(\w+)'
        match = re.search(pattern, content, re.MULTILINE)
        return match.group(1) if match else "Unknown"
    
    def extract_javadoc(self, content, method_start_pos):
        """Extract Javadoc comment immediately before a method"""
        # Look backwards from method position to find Javadoc
        lines_before = content[:method_start_pos].split('\n')
        
        javadoc = {
            'description': '',
            'params': {},
            'returns': ''
        }
        
        # Find the Javadoc block (/** ... */) - must be within 5 lines of method
        javadoc_lines = []
        lines_to_check = []
        blank_line_count = 0
        
        # Work backwards, but stop if we hit too many non-javadoc lines
        for line in reversed(lines_before[-10:]):
            stripped = line.strip()
            
            # Stop if we hit a closing brace or semicolon (previous statement)
            if stripped.endswith('}') or stripped.endswith(';'):
                break
                
            # Count blank lines - if we have more than 2, stop
            if not stripped:
                blank_line_count += 1
                if blank_line_count > 2:
                    break
            
            lines_to_check.insert(0, line)
        
        # Now parse the Javadoc from these recent lines
        in_javadoc = False
        for line in reversed(lines_to_check):
            stripped = line.strip()
            
            if stripped.startswith('*/'):
                in_javadoc = True
                continue
            elif stripped.startswith('/**'):
                in_javadoc = False
                break
            elif in_javadoc:
                # Remove leading * and whitespace
                cleaned = re.sub(r'^\s*\*\s?', '', line)
                if cleaned:
                    javadoc_lines.insert(0, cleaned)
        
        if not javadoc_lines:
            return javadoc
        
        # Parse javadoc content
        description_lines = []
        
        for line in javadoc_lines:
            if line.strip().startswith('@param'):
                match = re.match(r'@param\s+(\w+)\s+(.+)', line.strip())
                if match:
                    param_name, param_desc = match.groups()
                    javadoc['params'][param_name] = param_desc
            elif line.strip().startswith('@return'):
                javadoc['returns'] = re.sub(r'@return\s+', '', line.strip())
            elif not line.strip().startswith('@'):
                description_lines.append(line.strip())
        
        javadoc['description'] = ' '.join(description_lines).strip()
        return javadoc
        
    def extract_methods(self, content, class_name):
        """Extract all method signatures from Java file"""
        methods = []
        
        # Control structures to exclude
        control_keywords = {'if', 'else', 'for', 'while', 'switch', 'case', 'try', 'catch', 'finally', 'do'}
        
        # Improved regex to match method signatures more precisely
        # This pattern looks for proper method declarations with access modifiers or return types
        method_pattern = r'^\s*(public|protected|private)?\s*(static\s+)?(final\s+)?(synchronized\s+)?([<>\w\[\],\s?]+)\s+(\w+)\s*\((.*?)\)\s*(?:throws\s+[\w\s,.<>]+)?\s*\{'
        
        for match in re.finditer(method_pattern, content, re.MULTILINE):
            method_start_pos = match.start()
            visibility = match.group(1) or "package-private"
            is_static = "static" if match.group(2) else ""
            return_type = match.group(5).strip()
            method_name = match.group(6)
            params_str = match.group(7).strip()
            
            # Skip if method name is a control keyword
            if method_name in control_keywords:
                continue
            
            # Skip constructors (same name as class)
            if method_name == class_name:
                continue

            # SKIP POTENTIAL INNER CLASS CONSTRUCTORS
            if not return_type or return_type in ['public', 'private', 'protected']:
                 continue
            if return_type == method_name:
                continue
            
            # Skip if return type looks invalid (like comment markers, etc.)
            if any(char in return_type for char in ['*', '/', '{', '}', ';']):
                continue
            
            # Extract Javadoc
            javadoc = self.extract_javadoc(content, method_start_pos)
                
            # Parse parameters
            parameters = []
            if params_str:
                # Split by comma, but respect generics
                param_parts = self.split_parameters(params_str)
                for param in param_parts:
                    param = param.strip()
                    if param and not param.startswith('//'):
                        # Extract type and name
                        parts = param.rsplit(' ', 1)
                        if len(parts) == 2:
                            param_type, param_name = parts
                            # Clean parameter name (remove default values, etc.)
                            param_name = param_name.split('=')[0].strip()
                            parameters.append({
                                "type": param_type.strip(),
                                "name": param_name,
                                "description": javadoc['params'].get(param_name, '')
                            })
            
            methods.append({
                "name": method_name,
                "visibility": visibility,
                "static": is_static,
                "returnType": return_type,
                "parameters": parameters,
                "signature": self.build_signature(visibility, is_static, return_type, method_name, parameters),
                "description": javadoc['description'],
                "returns": javadoc['returns']
            })
            
        return methods
    
    def split_parameters(self, params_str):
        """Split parameters by comma, respecting generics"""
        params = []
        current = ""
        depth = 0
        
        for char in params_str:
            if char in '<[':
                depth += 1
                current += char
            elif char in '>]':
                depth -= 1
                current += char
            elif char == ',' and depth == 0:
                params.append(current)
                current = ""
            else:
                current += char
        
        if current:
            params.append(current)
            
        return params
    
    def build_signature(self, visibility, is_static, return_type, method_name, parameters):
        """Build a human-readable method signature"""
        parts = []
        if visibility:
            parts.append(visibility)
        if is_static:
            parts.append("static")
        parts.append(return_type)
        parts.append(method_name)
        
        param_str = ", ".join([f"{p['type']} {p['name']}" for p in parameters])
        
        return f"{' '.join(parts)}({param_str})"
    
    def extract_constructors(self, content, class_name):
        """Extract constructors from Java file"""
        constructors = []
        
        # Match constructor pattern
        constructor_pattern = rf'^\s*(public|protected|private)?\s+{class_name}\s*\((.*?)\)\s*\{{' 
        
        for match in re.finditer(constructor_pattern, content, re.MULTILINE | re.DOTALL):
            visibility = match.group(1) or "package-private"
            params_str = match.group(2).strip()
            
            # Parse parameters
            parameters = []
            if params_str:
                param_parts = self.split_parameters(params_str)
                for param in param_parts:
                    param = param.strip()
                    if param:
                        parts = param.rsplit(' ', 1)
                        if len(parts) == 2:
                            param_type, param_name = parts
                            parameters.append({
                                "type": param_type.strip(),
                                "name": param_name.strip()
                            })
            
            param_list = ", ".join([f"{p['type']} {p['name']}" for p in parameters])
            constructors.append({
                "name": class_name,
                "visibility": visibility,
                "parameters": parameters,
                "signature": f"{visibility} {class_name}({param_list})"
            })
            
        return constructors
    
    def process_file(self, file_path):
        """Process a single Java file and extract API information"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        package = self.extract_package(content)
        class_name = self.extract_class_name(content)
        methods = self.extract_methods(content, class_name)
        constructors = self.extract_constructors(content, class_name)
        
        # Get relative path for display
        rel_path = os.path.relpath(file_path, self.src_dir)
        
        return {
            "package": package,
            "className": class_name,
            "file": rel_path,
            "constructors": constructors,
            "methods": methods
        }
    
    def extract_all(self):
        """Extract API information from all Java files"""
        java_files = list(Path(self.src_dir).rglob("*.java"))
        
        for java_file in sorted(java_files):
            try:
                class_info = self.process_file(str(java_file))
                full_class_name = f"{class_info['package']}.{class_info['className']}"
                self.api_data[full_class_name] = class_info
            except Exception as e:
                print(f"Error processing {java_file}: {e}")
        
        return self.api_data
    
    def save_to_json(self, output_file):
        """Save extracted API data to JSON file"""
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(self.api_data, f, indent=2)
        print(f"API data saved to {output_file}")

if __name__ == "__main__":
    src_dir = "src/main/java"
    output_file = "api-data.json"
    
    extractor = JavaMethodExtractor(src_dir)
    extractor.extract_all()
    extractor.save_to_json(output_file)
    
    # Print summary
    total_classes = len(extractor.api_data)
    total_methods = sum(len(cls['methods']) for cls in extractor.api_data.values())
    total_constructors = sum(len(cls['constructors']) for cls in extractor.api_data.values())
    
    print(f"\nExtraction Summary:")
    print(f"  Classes: {total_classes}")
    print(f"  Constructors: {total_constructors}")
    print(f"  Methods: {total_methods}")
