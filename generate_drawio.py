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
edges = []

class_width = 240
field_height = 26

for file_rel in entity_files:
    file_path = os.path.join(base_path, file_rel.replace('/', '\\'))
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # find class name
    match = re.search(r'public\s+class\s+(\w+)', content)
    if not match:
        continue
    class_name = match.group(1)
    
    # find fields
    # ignoring methods, finding private/protected fields
    fields = []
    lines = content.split('\n')
    
    for line in lines:
        line = line.strip()
        if line.startswith('private') or line.startswith('protected'):
            # e.g., private String name;
            # private Product product = new Product();
            parts = line.split()
            if len(parts) >= 3:
                # Type is parts[1] (might have generic <..>), Name is parts[2]
                field_type = parts[1]
                field_name = parts[2].replace(';', '')
                fields.append({'name': field_name, 'type': field_type})
                
        # basic relationship parsing
        if '@ManyToOne' in line or '@OneToOne' in line or '@OneToMany' in line or '@ManyToMany' in line:
            # The next field declaration is the target
            pass
            
    classes[class_name] = fields

# Basic layout logic (grid)
x_start = 50
y_start = 50
x_spacing = 300
y_spacing = 400
cols = 5

xml = []
xml.append('<mxGraphModel dx="1200" dy="800" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="3000" pageHeight="3000" math="0" shadow="0">')
xml.append('  <root>')
xml.append('    <mxCell id="0" />')
xml.append('    <mxCell id="1" parent="0" />')

cell_id = 2

# generate classes
idx = 0
for class_name, fields in classes.items():
    row = idx // cols
    col = idx % cols
    x = x_start + col * x_spacing
    y = y_start + row * y_spacing
    
    height = 26 + len(fields) * 26
    
    xml.append(f'    <mxCell id="{class_name}" parent="1" style="swimlane;fontStyle=1;childLayout=stackLayout;horizontal=1;startSize=26;fillColor=#ffffff;strokeColor=#000000;fontColor=#000000;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=1;marginBottom=0;whiteSpace=wrap;html=1;" value="{class_name}" vertex="1">')
    xml.append(f'      <mxGeometry height="{height}" width="{class_width}" x="{x}" y="{y}" as="geometry" />')
    xml.append('    </mxCell>')
    
    for f_idx, field in enumerate(fields):
        y_offset = 26 + f_idx * 26
        f_name = field['name']
        f_type = field['type'].replace('<', '&lt;').replace('>', '&gt;')
        label = f'- {f_name}: {f_type}'
        xml.append(f'    <mxCell id="{class_name}_f{f_idx}" parent="{class_name}" style="text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=4;spacingRight=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=1;" value="{label}" vertex="1">')
        xml.append(f'      <mxGeometry height="26" width="{class_width}" y="{y_offset}" as="geometry" />')
        xml.append('    </mxCell>')
    
    idx += 1

# Let's generate relationships. Simple approach:
# If a field type matches another class_name, draw an association.
drawn_edges = set()

for class_name, fields in classes.items():
    for field in fields:
        target_type = field['type']
        # Handle List<Type> or Set<Type>
        m = re.search(r'(?:List|Set)<(\w+)>', target_type)
        is_collection = False
        if m:
            target_type = m.group(1)
            is_collection = True
            
        if target_type in classes and target_type != class_name: # avoid self loop for simplicity or draw it if needed
            edge_key = tuple(sorted([class_name, target_type]))
            
            # we draw directional from class_name to target_type
            source_label = "1" if not is_collection else "0..*"
            target_label = "0..*" if is_collection else "1"
            
            xml.append(f'    <mxCell id="Edge_{class_name}_{field["name"]}" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=none;endFill=0;strokeWidth=1.5;" edge="1" parent="1" source="{class_name}" target="{target_type}">')
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

with open(r"c:\Users\fptshop\.gemini\antigravity\brain\2b69dc73-9789-4fd3-a7a1-00ea0c8f449a\diagram_23_tables.xml", "w", encoding="utf-8") as f:
    f.write("\n".join(xml))

print("Done")
