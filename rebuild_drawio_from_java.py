import os
import re

# 1. TRÍCH XUẤT TỌA ĐỘ TỪ BẢN VẼ CỦA USER (cl_fixed.drawio.xml)
layout_map = {}
input_xml = r"c:\Users\fptshop\hbcstore-api\cl_fixed.drawio.xml"
if os.path.exists(input_xml):
    with open(input_xml, 'r', encoding='utf-8') as f:
        xml_content = f.read()
    
    cell_pattern = r'<mxCell id="(\w+)" parent="1" [^>]*value="\1"[^>]*>.*?<mxGeometry[^>]*>'
    for match in re.finditer(cell_pattern, xml_content, re.DOTALL):
        class_name = match.group(1)
        geom_tag = match.group(0)
        
        # FIXED: Added \b to prevent matching vertex="1"
        x_match = re.search(r'\bx="([^"]+)"', geom_tag)
        y_match = re.search(r'\by="([^"]+)"', geom_tag)
        
        x = float(x_match.group(1)) if x_match else 0
        y = float(y_match.group(1)) if y_match else 0
        
        layout_map[class_name] = (x, y)

# 2. ĐỌC LẠI CHÍNH XÁC CÁC THUỘC TÍNH VÀ KIỂU DỮ LIỆU TỪ JAVA
entity_files = [
    "auth/EmailVerificationToken.java",
    "auth/PasswordResetToken.java",
    "cart/Cart.java",
    "cart/CartItem.java",
    "catalog/Brand.java",
    "catalog/Category.java",
    "catalog/Product.java",
    "catalog/ProductAttribute.java",
    "catalog/ProductImage.java",
    "catalog/Subcategory.java",
    "coupon/Coupon.java",
    "notification/Notification.java",
    "order/OrderDetail.java",
    "order/StoreOrder.java",
    "promotion/Promotion.java",
    "promotion/PromotionBrand.java",
    "promotion/PromotionCategory.java",
    "promotion/PromotionProduct.java",
    "refund/RefundRequest.java",
    "review/ProductReview.java",
    "review/ReviewSettings.java",
    "shipping/ShippingSettings.java",
    "user/User.java"
]

base_path = r"c:\Users\fptshop\hbcstore-api\src\main\java\com\hbcstore\hbcstore_api"
classes = {}
class_width = 280

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
    
    fields = []
    for f_match in re.finditer(r'(?:private|protected)\s+([^=;]+?)\s+(\w+)\s*(?:=|;)', content_no_comments):
        field_type = f_match.group(1).strip()
        field_name = f_match.group(2).strip()
        
        if 'static' in field_type or 'final' in field_type:
            continue
            
        fields.append({'name': field_name, 'type': field_type})
                
    classes[class_name] = fields

# 3. TẠO LẠI FILE XML HOÀN HẢO
xml = []
xml.append('<mxGraphModel dx="2000" dy="1500" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="4000" pageHeight="3000" math="0" shadow="0">')
xml.append('  <root>')
xml.append('    <mxCell id="0" />')
xml.append('    <mxCell id="1" parent="0" />')

for class_name, fields in classes.items():
    if class_name in layout_map:
        x, y = layout_map[class_name]
    else:
        x, y = 50, 50
        
    height = 36 + len(fields) * 32
    
    xml.append(f'    <mxCell id="{class_name}" parent="1" style="swimlane;fontStyle=1;childLayout=stackLayout;horizontal=1;startSize=36;fillColor=#ffffff;strokeColor=#000000;fontColor=#000000;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=1;marginBottom=0;whiteSpace=wrap;html=1;rounded=1;fontSize=20;" value="{class_name}" vertex="1">')
    xml.append(f'      <mxGeometry height="{height}" width="{class_width}" x="{x}" y="{y}" as="geometry" />')
    xml.append('    </mxCell>')
    
    for f_idx, field in enumerate(fields):
        y_offset = 36 + f_idx * 32
        f_name = field['name']
        f_type = field['type'].replace('<', '&lt;').replace('>', '&gt;')
        label = f'+ {f_name}: {f_type}'
        xml.append(f'    <mxCell id="{class_name}_f{f_idx}" parent="{class_name}" style="text;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;spacingLeft=8;spacingRight=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=1;fontSize=18;" value="{label}" vertex="1">')
        xml.append(f'      <mxGeometry height="32" width="{class_width}" y="{y_offset}" as="geometry" />')
        xml.append('    </mxCell>')

compositions = [
    ("StoreOrder", "OrderDetail"), ("Cart", "CartItem"), ("Product", "ProductAttribute"), 
    ("Product", "ProductImage"), ("Product", "ProductReview")
]
aggregations = [
    ("Category", "Subcategory"), ("Promotion", "PromotionBrand"), 
    ("Promotion", "PromotionCategory"), ("Promotion", "PromotionProduct")
]

drawn_edges = set()

for class_name, fields in classes.items():
    for field in fields:
        target_type = field['type']
        m = re.search(r'(?:List|Set)&lt;(\w+)&gt;|(?:List|Set)<(\w+)>', target_type)
        is_collection = False
        if m:
            target_type = m.group(1) if m.group(1) else m.group(2)
            is_collection = True
            
        if target_type in classes and target_type != class_name:
            edge_key = tuple(sorted([class_name, target_type]))
            if edge_key in drawn_edges:
                continue
            drawn_edges.add(edge_key)

            style = "edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;endArrow=classic;endFill=1;strokeWidth=1.5;"
            source_label = "1" if not is_collection else "1..n"
            target_label = "1..n" if is_collection else "1..1"
            
            xml.append(f'    <mxCell id="Edge_{class_name}_{field["name"]}" style="{style}" edge="1" parent="1" source="{class_name}" target="{target_type}">')
            xml.append('      <mxGeometry relative="1" as="geometry" />')
            xml.append('    </mxCell>')
            
            xml.append(f'    <mxCell id="L1_Edge_{class_name}_{field["name"]}" value="{source_label}" style="edgeLabel;html=1;align=center;verticalAlign=bottom;resizable=0;points=[];labelBackgroundColor=none;fontSize=16;" vertex="1" connectable="0" parent="Edge_{class_name}_{field["name"]}">')
            xml.append('      <mxGeometry x="-0.8" relative="1" as="geometry"><mxPoint y="-5" as="offset" /></mxGeometry>')
            xml.append('    </mxCell>')
            
            xml.append(f'    <mxCell id="L2_Edge_{class_name}_{field["name"]}" value="{target_label}" style="edgeLabel;html=1;align=center;verticalAlign=bottom;resizable=0;points=[];labelBackgroundColor=none;fontSize=16;" vertex="1" connectable="0" parent="Edge_{class_name}_{field["name"]}">')
            xml.append('      <mxGeometry x="0.8" relative="1" as="geometry"><mxPoint y="-5" as="offset" /></mxGeometry>')
            xml.append('    </mxCell>')

xml.append('  </root>')
xml.append('</mxGraphModel>')

output_xml = r"c:\Users\fptshop\hbcstore-api\cl_perfect_fixed.drawio.xml"
with open(output_xml, "w", encoding="utf-8") as f:
    f.write("\n".join(xml))

print(f"Done! Saved to {output_xml}")
