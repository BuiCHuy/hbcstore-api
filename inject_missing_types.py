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

input_xml = r"c:\Users\fptshop\hbcstore-api\cl.drawio.xml"
with open(input_xml, 'r', encoding='utf-8') as f:
    xml_content = f.read()

def replace_field_value(match):
    prefix = match.group(1)
    class_name = match.group(2)
    field_name = match.group(3)
    suffix = match.group(4)
    
    if class_name in class_fields and field_name in class_fields[class_name]:
        true_type = class_fields[class_name][field_name]
        # DOUBLE ENCODING CỰC KỲ QUAN TRỌNG ĐỂ VƯỢT QUA LỖI RENDER HTML CỦA DRAW.IO
        # &amp;lt; sẽ được XML đọc thành &lt;, sau đó Draw.io (html=1) sẽ đọc thành < và hiển thị thành công!
        safe_type = true_type.replace('<', '&amp;lt;').replace('>', '&amp;gt;')
        return f'{prefix} {safe_type}{suffix}'
    
    return match.group(0)

pattern = r'(<mxCell[^>]*?parent="(\w+)"[^>]*?value="\+\s*(\w+)\s*:)[^"]*("[^>]*>)'
fixed_xml_content = re.sub(pattern, replace_field_value, xml_content)

output_xml = r"c:\Users\fptshop\hbcstore-api\cl_final.drawio.xml"
with open(output_xml, "w", encoding="utf-8") as f:
    f.write(fixed_xml_content)

print(f"Done! Double-encoded types saved to {output_xml}")
