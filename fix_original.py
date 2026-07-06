import re
import os

input_file = r"c:\Users\fptshop\hbcstore-api\cl_original.drawio.xml"
output_file = r"c:\Users\fptshop\hbcstore-api\cl_original_wide_large_diamonds.drawio.xml"

if not os.path.exists(input_file):
    print("File not found.")
    exit(1)

with open(input_file, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Kéo giãn các ô từ 220 lên 340 để không bị mất chữ
content = content.replace('width="220"', 'width="340"')

# 2. Làm to các hình thoi (Composition / Aggregation)
def enlarge_diamonds(match):
    prefix = match.group(1)
    style_str = match.group(2)
    suffix = match.group(3)
    
    # Chỉ xử lý các đường (edge) có mũi tên hình thoi
    if 'endArrow=diamond' in style_str or 'startArrow=diamond' in style_str:
        # Xóa các thuộc tính size cũ nếu có
        style_str = re.sub(r'endSize=[^;]*;', '', style_str)
        style_str = re.sub(r'startSize=[^;]*;', '', style_str)
        
        # Thêm size to đùng (mặc định là 6, ta tăng lên 18)
        new_style = "endSize=18;startSize=18;" + style_str
        return f'{prefix}style="{new_style}"{suffix}'
    return match.group(0)

# Tìm tất cả các thẻ có thuộc tính style
new_content = re.sub(r'(<mxCell[^>]*?\s)style="([^"]*)"([^>]*>)', enlarge_diamonds, content)

with open(output_file, 'w', encoding='utf-8') as f:
    f.write(new_content)

print(f"Done! Saved to {output_file}")
