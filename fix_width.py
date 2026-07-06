import os

input_xml = r"c:\Users\fptshop\hbcstore-api\cl_final.drawio.xml"
with open(input_xml, 'r', encoding='utf-8') as f:
    content = f.read()

# Tăng chiều rộng của tất cả các bảng và cột từ 220 lên 340 để chứa vừa font 20
content = content.replace('width="220"', 'width="340"')

output_xml = r"c:\Users\fptshop\hbcstore-api\cl_final_fixed_width.drawio.xml"
with open(output_xml, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"Fixed width! Saved to {output_xml}")
