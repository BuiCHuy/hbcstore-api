import os
import re

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
class_width = 240

for file_rel in entity_files:
    file_path = os.path.join(base_path, file_rel.replace('/', '\\'))
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    match = re.search(r'public\s+class\s+(\w+)', content)
    if not match:
        continue
    class_name = match.group(1)
    
    fields = []
    lines = content.split('\n')
    
    for line in lines:
        line = line.strip()
        if line.startswith('private') or line.startswith('protected'):
            parts = line.split()
            if len(parts) >= 3:
                field_type = parts[1]
                field_name = parts[2].replace(';', '')
                fields.append({'name': field_name, 'type': field_type})
                
    classes[class_name] = fields

# Tâm Điểm: Product(2400,1600), StoreOrder(4000,1600), User(4000,3200)
layout_map = {
    # == TRUNG TÂM SẢN PHẨM ==
    "Product": (2400, 1600),
    "Brand": (1600, 1600),
    "Category": (1600, 800),
    "Subcategory": (800, 800),
    "ProductImage": (2400, 800),
    "ProductAttribute": (1600, 2400),
    
    # == CẦU NỐI (BRIDGE) ==
    "OrderDetail": (3200, 1600), # Nối Product & StoreOrder
    "CartItem": (2400, 2400),    # Nối Product & Cart
    "Cart": (3200, 2400),        # Nối CartItem & User
    "ProductReview": (3200, 3200), # Nối Product & User
    
    # == TRUNG TÂM ĐẶT HÀNG ==
    "StoreOrder": (4000, 1600),
    "Coupon": (4000, 800),
    "Notification": (4800, 1600),
    "ShippingSettings": (4800, 800),
    "RefundRequest": (4800, 2400),
    
    # == TRUNG TÂM USER ==
    "User": (4000, 3200),
    "ReviewSettings": (2400, 3200),
    "EmailVerificationToken": (4800, 3200),
    "PasswordResetToken": (4000, 4000),
    
    # == KHUYẾN MÃI ==
    "Promotion": (800, 0),
    "PromotionBrand": (800, 1600),
    "PromotionCategory": (1600, 0),
    "PromotionProduct": (2400, 0)
}

xml = []
xml.append('<mxGraphModel dx="2000" dy="1500" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="6000" pageHeight="5000" math="0" shadow="0">')
xml.append('  <root>')
xml.append('    <mxCell id="0" />')
xml.append('    <mxCell id="1" parent="0" />')

for class_name, fields in classes.items():
    if class_name in layout_map:
        x, y = layout_map[class_name]
    else:
        x, y = 50, 50 # fallback
        
    height = 26 + len(fields) * 26
    
    xml.append(f'    <mxCell id="{class_name}" parent="1" style="swimlane;fontStyle=0;childLayout=stackLayout;horizontal=1;startSize=26;fillColor=none;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=1;marginBottom=0;whiteSpace=wrap;html=1;" value="{class_name}" vertex="1">')
    xml.append(f'      <mxGeometry height="{height}" width="{class_width}" x="{x}" y="{y}" as="geometry" />')
    xml.append('    </mxCell>')
    
    for f_idx, field in enumerate(fields):
        y_offset = 26 + f_idx * 26
        f_name = field['name']
        f_type = field['type'].replace('<', '&lt;').replace('>', '&gt;')
        label = f'+ {f_name}: {f_type}'
        xml.append(f'    <mxCell id="{class_name}_f{f_idx}" parent="{class_name}" style="text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=4;spacingRight=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=1;" value="{label}" vertex="1">')
        xml.append(f'      <mxGeometry height="26" width="{class_width}" y="{y_offset}" as="geometry" />')
        xml.append('    </mxCell>')

compositions = [
    ("StoreOrder", "OrderDetail"),
    ("Cart", "CartItem"),
    ("Product", "ProductAttribute"),
    ("Product", "ProductImage"),
    ("Product", "ProductReview")
]
aggregations = [
    ("Category", "Subcategory"),
    ("Promotion", "PromotionBrand"),
    ("Promotion", "PromotionCategory"),
    ("Promotion", "PromotionProduct")
]

drawn_edges = set()

for class_name, fields in classes.items():
    for field in fields:
        target_type = field['type']
        m = re.search(r'(?:List|Set)<(\w+)>', target_type)
        is_collection = False
        if m:
            target_type = m.group(1)
            is_collection = True
            
        if target_type in classes and target_type != class_name:
            edge_key = tuple(sorted([class_name, target_type]))
            if edge_key in drawn_edges:
                continue
            drawn_edges.add(edge_key)

            # Dùng orthogonal Edge Style
            style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=none;endFill=0;strokeWidth=1.5;"
            source_label = "1" if not is_collection else "1..n"
            target_label = "1..n" if is_collection else "1..1"

            if (class_name, target_type) in compositions:
                style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;startArrow=diamondThin;startFill=1;endArrow=none;endFill=0;strokeWidth=1.5;"
            elif (target_type, class_name) in compositions:
                style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=diamondThin;endFill=1;startArrow=none;startFill=0;strokeWidth=1.5;"

            elif (class_name, target_type) in aggregations:
                style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;startArrow=diamondThin;startFill=0;endArrow=none;endFill=0;strokeWidth=1.5;"
            elif (target_type, class_name) in aggregations:
                style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=diamondThin;endFill=0;startArrow=none;startFill=0;strokeWidth=1.5;"
            
            xml.append(f'    <mxCell id="Edge_{class_name}_{field["name"]}" style="{style}" edge="1" parent="1" source="{class_name}" target="{target_type}">')
            xml.append('      <mxGeometry relative="1" as="geometry" />')
            xml.append('    </mxCell>')
            
            xml.append(f'    <mxCell id="L1_Edge_{class_name}_{field["name"]}" value="{source_label}" style="edgeLabel;html=1;align=center;verticalAlign=bottom;resizable=0;points=[];labelBackgroundColor=none;" vertex="1" connectable="0" parent="Edge_{class_name}_{field["name"]}">')
            xml.append('      <mxGeometry x="-0.8" relative="1" as="geometry"><mxPoint y="-5" as="offset" /></mxGeometry>')
            xml.append('    </mxCell>')
            
            xml.append(f'    <mxCell id="L2_Edge_{class_name}_{field["name"]}" value="{target_label}" style="edgeLabel;html=1;align=center;verticalAlign=bottom;resizable=0;points=[];labelBackgroundColor=none;" vertex="1" connectable="0" parent="Edge_{class_name}_{field["name"]}">')
            xml.append('      <mxGeometry x="0.8" relative="1" as="geometry"><mxPoint y="-5" as="offset" /></mxGeometry>')
            xml.append('    </mxCell>')

xml.append('  </root>')
xml.append('</mxGraphModel>')

with open(r"c:\Users\fptshop\hbcstore-api\drawio_23_tables_hub.xml", "w", encoding="utf-8") as f:
    f.write("\n".join(xml))

print("Done")
