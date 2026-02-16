import sys
import ast

def clean_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            source = f.read()
    except UnicodeDecodeError:
        print(f"Skipping {filepath} (binary or encoding issue)")
        return
            
    try:
        parsed = ast.parse(source)
    except SyntaxError as e:
        print(f"Skipping {filepath} (Syntax Error: {e})")
        return
    
    for node in ast.walk(parsed):
        if not isinstance(node, (ast.FunctionDef, ast.ClassDef, ast.AsyncFunctionDef, ast.Module)):
            continue
            
        if not node.body:
            continue
            
        if isinstance(node.body[0], ast.Expr):
            val = node.body[0].value
            is_docstring = False
            if isinstance(val, ast.Constant) and isinstance(val.value, str):
                 is_docstring = True
            elif isinstance(val, ast.Str):
                 is_docstring = True

            if is_docstring:
                node.body.pop(0) 

    if sys.version_info < (3, 9):
        print(f"Error: Python 3.9+ required for ast.unparse (Current: {sys.version})")
        return

    try:
        clean_source = ast.unparse(parsed)
    except Exception as e:
        print(f"Error unparsing {filepath}: {e}")
        return
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(clean_source)
    print(f"Cleaned {filepath}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 remove_py_comments.py <file1> <file2> ...")
        sys.exit(1)
        
    for f in sys.argv[1:]:
        clean_file(f)
