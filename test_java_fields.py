import os
import re
import json

entity_files = [
    "cart/Cart.java",
    "cart/CartItem.java",
    "catalog/Product.java"
]

base_path = r"c:\Users\fptshop\hbcstore-api\src\main\java\com\hbcstore\hbcstore_api"
class_fields = {}

for file_rel in entity_files:
    file_path = os.path.join(base_path, file_rel.replace('/', '\\'))
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content_no_comments = re.sub(r'//.*', '', content)
    content_no_comments = re.sub(r'/\*.*?\*/', '', content_no_comments, flags=re.DOTALL)
    
    match = re.search(r'public\s+class\s+(\w+)', content_no_comments)
    if not match:
        continue
    class_name = match.group(1)
    
    fields = {}
    for f_match in re.finditer(r'(?:private|protected)\s+([^=;]+?)\s+(\w+)\s*(?:=|;)', content_no_comments):
        field_type = f_match.group(1).strip()
        field_name = f_match.group(2).strip()
        
        if 'static' in field_type or 'final' in field_type:
            continue
            
        fields[field_name] = field_type
                
    class_fields[class_name] = fields

print(json.dumps(class_fields, indent=2))
