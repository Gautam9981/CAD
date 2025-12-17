#!/usr/bin/env python3
"""
Code Analyzer for CAD System
Parses Java source, extracts structural logic, and infers documentation.
Generates codex.json for the frontend.
"""

import os
import re
import json
import glob
from pathlib import Path

SRC_DIR = "src/main/java"
OUTPUT_FILE = "docs/codex.json"

class JavaParser:
    def __init__(self):
        self.classes = {}
        self.relationships = []

    def parse_directory(self, root_dir):
        java_files = glob.glob(os.path.join(root_dir, "**/*.java"), recursive=True)
        for file_path in java_files:
            try:
                self.parse_file(file_path)
            except Exception as e:
                print(f"Error parsing {file_path}: {e}")

    def parse_file(self, file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        package = self.extract_pattern(r'package\s+([\w.]+);', content)
        class_name = self.extract_pattern(r'(?:public\s+)?(?:abstract\s+)?(?:class|interface|enum|record)\s+(\w+)', content)
        
        if not class_name:
            return

        full_name = f"{package}.{class_name}" if package else class_name
        
        methods = self.extract_methods(content, class_name)
        
        self.classes[full_name] = {
            "name": class_name,
            "package": package,
            "file": file_path,
            "methods": methods,
            "description": self.infer_class_description(content, class_name),
            "dependencies": self.extract_dependencies(content)
        }

    def extract_pattern(self, pattern, content):
        match = re.search(pattern, content)
        return match.group(1) if match else None

    def extract_methods(self, content, class_name):
        methods = []
        # Regex for method usage: modifier returnType name(params) throws ... {
        # This is a simplification; a true parser is better but brace counting works for bodies.
        
        # We'll stick to a regex to find the START, then brace count for BODY.
        method_pattern = re.compile(
            r'^\s*(public|protected|private|)\s*'
            r'(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?'
            r'([\w<>\[\]]+)\s+'  # Return type
            r'(\w+)\s*'          # Method name
            r'\(([^)]*)\)\s*'    # Params
            r'(?:throws\s+[\w\s,]+)?'
            r'\s*\{',            # Start of body
            re.MULTILINE
        )

        for match in method_pattern.finditer(content):
            visibility = match.group(1) or "package-private"
            return_type = match.group(2)
            name = match.group(3)
            params = match.group(4)
            start_index = match.end() - 1 # Points to {
            
            # Skip control structures masquerading as methods (if any weird formatting)
            if name in ['if', 'for', 'while', 'switch', 'catch']: 
                continue

            # Check if it's a constructor (return type is same as method name usually?? No, constructor has NO return type in regex)
            # Our regex forces a return type group 2. 
            # Constructors: public ClassName(...) 
            # Regex: modifier (returnType?) name ...
            # If it's a constructor, 'returnType' might be empty or modifiers.
            # Actually, standard method regex fails for constructors.
            # Let's use a separate pass or accept that "ClassName" might be captured as return type?
            # public ClassName() -> public (ClassName) () ?? No name.
            # This regex needs name.
            
            # Simplify: Just extract body and infer from it.
            
            body = self.extract_body(content, start_index)
            inference = self.infer_method_logic(name, body, params, return_type)
            
            methods.append({
                "name": name,
                "visibility": visibility.strip(),
                "returnType": return_type,
                "params": params,
                "description": inference["description"],
                "connections": inference["connections"],
                "codeSnippet": body[:200] + "..." if len(body) > 200 else body
            })
            
        # Separate pass for constructors
        # public ClassName(...)
        constructor_pattern = re.compile(
            rf'^\s*(public|protected|private|)\s+{class_name}\s*\(([^)]*)\)\s*\{{',
            re.MULTILINE
        )
        for match in constructor_pattern.finditer(content):
            visibility = match.group(1) or "package-private"
            params = match.group(2)
            start_index = match.end() - 1
            body = self.extract_body(content, start_index)
            
            inference = self.infer_method_logic(class_name, body, params, "constructor")

            methods.append({
                "name": class_name,
                "visibility": visibility.strip(),
                "returnType": "constructor",
                "params": params,
                "description": f"Initializes {class_name}. " + inference["description"],
                "connections": inference["connections"],
                "codeSnippet": body[:200]
            })

        return methods

    def extract_body(self, content, start_index):
        # Brace counting
        stack = 0
        body = ""
        found_start = False
        
        for i in range(start_index, len(content)):
            char = content[i]
            if char == '{':
                stack += 1
                found_start = True
            elif char == '}':
                stack -= 1
            
            body += char
            
            if found_start and stack == 0:
                break
        
        # Remove wrapper braces
        if body.startswith('{') and body.endswith('}'):
            return body[1:-1].strip()
        return body

    def infer_method_logic(self, name, body, params, return_type):
        desc = []
        connections = []
        
        lines = body.split('\n')
        
        # Logic Inference Rules
        if "System.out.println" in body:
            desc.append("Logs status to console.")
        
        if "commandManager.execute" in body or "cmdMgr.execute" in body:
            desc.append("Dispatches a command for execution.")
            connections.append("CommandManager")
            
        if "notify" in name.lower() or "fire" in name.lower():
            desc.append("Notifies registered listeners.")
            
        if "return" in body and return_type != "void":
            if "calculated" in body or "Math." in body:
                desc.append("Computes and returns the result.")
            else:
                desc.append("Returns the requested value.")

        if "this." in body and "=" in body:
            desc.append("Updates internal state properties.")
            
        if "add" in name.lower() and "list" in body.lower():
            desc.append("Adds item to internal collection.")
            
        if "remove" in name.lower():
            desc.append("Removes item from collection.")

        # Dependency Inference
        # Look for ClassName.staticMethod() or new ClassName()
        # Regex for Capitalized words
        potential_classes = set(re.findall(r'\b([A-Z]\w+)\b', body))
        ignore_list = {'String', 'Math', 'System', 'List', 'ArrayList', 'Float', 'Integer', 'Object', 'Exception', 'Override', 'Deprecated'}
        
        for cls in potential_classes:
            if cls not in ignore_list and cls != name: # Don't list self
                connections.append(cls)
                
        # Default fallback
        if not desc:
            if name.startswith("get"): desc.append(f"Retrieves {name[3:]}.")
            elif name.startswith("set"): desc.append(f"Sets {name[3:]}.")
            elif name.startswith("is"): desc.append(f"Checks if {name[2:]}.")
            else: desc.append("Executes custom logic.")
            
        return {
            "description": " ".join(desc),
            "connections": list(set(connections))
        }

    def infer_class_description(self, content, class_name):
        # Look for "implements" or "extends"
        match = re.search(r'(extends|implements)\s+(\w+)', content)
        if match:
            return f"A component that {match.group(1)} {match.group(2)}."
        return f"Core component {class_name}."

    def extract_dependencies(self, content):
        # Imports are a good proxy for dependencies
        imports = re.findall(r'import\s+([\w.]+);', content)
        deps = []
        for imp in imports:
            if not imp.startswith("java."):
                deps.append(imp.split('.')[-1]) # Just class name
        return deps

    def save_codex(self):
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(self.classes, f, indent=2)
        print(f"Generated Documentation with {len(self.classes)} classes.")

if __name__ == "__main__":
    parser = JavaParser()
    parser.parse_directory(SRC_DIR)
    parser.save_codex()
