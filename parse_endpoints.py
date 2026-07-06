import os
import re
import json

base_path = r"c:\Users\fptshop\hbcstore-api\src\main\java\com\hbcstore\hbcstore_api"

controllers = {}

for root, dirs, files in os.walk(base_path):
    for file in files:
        if file.endswith("Controller.java"):
            file_path = os.path.join(root, file)
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Xóa comments
            content = re.sub(r'//.*', '', content)
            content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
            
            # Tìm class name và RequestMapping của class
            class_match = re.search(r'class\s+(\w+Controller)', content)
            if not class_match:
                continue
            class_name = class_match.group(1)
            
            base_mapping = ""
            base_mapping_match = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']\s*\)', content)
            if base_mapping_match:
                base_mapping = base_mapping_match.group(1)
                
            endpoints = []
            
            # Tìm các method mapping
            method_pattern = r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*|path\s*=\s*)?["\']([^"\']*)["\']\s*(?:,[^\)]*)?\))?[\s\S]*?(?:public|protected|private)\s+[\w<>\?,\s\[\]]+\s+(\w+)\s*\('
            
            for match in re.finditer(method_pattern, content):
                http_method = match.group(1).upper()
                path = match.group(2) if match.group(2) else ""
                method_name = match.group(3)
                
                full_path = base_mapping + path
                endpoints.append({
                    "method": http_method,
                    "path": full_path.replace('//', '/'),
                    "action": method_name
                })
                
            controllers[class_name] = endpoints

output_path = r"c:\Users\fptshop\hbcstore-api\endpoints_summary.json"
with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(controllers, f, indent=4, ensure_ascii=False)

print(f"Summary written to {output_path}")
