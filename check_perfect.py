import re
with open(r'c:\Users\fptshop\hbcstore-api\cl_perfect.drawio.xml', 'r', encoding='utf-8') as f:
    xml_content = f.read()

cell_pattern = r'<mxCell id="(\w+)" parent="1" [^>]*value="\1"[^>]*>.*?<mxGeometry[^>]*>'
layout = {}
for match in re.finditer(cell_pattern, xml_content, re.DOTALL):
    class_name = match.group(1)
    geom_tag = match.group(0)
    
    x_match = re.search(r'\bx="([^"]+)"', geom_tag)
    y_match = re.search(r'\by="([^"]+)"', geom_tag)
    
    x = float(x_match.group(1)) if x_match else 0
    y = float(y_match.group(1)) if y_match else 0
    
    layout[class_name] = (x, y)

print(f"Found {len(layout)} classes in cl_perfect.")
for k, v in layout.items():
    print(f"{k}: {v}")
