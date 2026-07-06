import re

input_file = r"c:\Users\fptshop\hbcstore-api\cl_final_fixed_width.drawio.xml"
output_file = r"c:\Users\fptshop\hbcstore-api\cl_final_fixed_width_straight.drawio.xml"

with open(input_file, 'r', encoding='utf-8') as f:
    content = f.read()

def update_style(match):
    prefix = match.group(1)
    style_str = match.group(2)
    suffix = match.group(3)
    
    # Remove old routing/corner styles
    style_str = re.sub(r'edgeStyle=[^;]*;', '', style_str)
    style_str = re.sub(r'curved=[^;]*;', '', style_str)
    style_str = re.sub(r'rounded=[^;]*;', '', style_str)
    style_str = re.sub(r'orthogonalLoop=[^;]*;', '', style_str)
    style_str = re.sub(r'jettySize=[^;]*;', '', style_str)
    
    # Add new perfect routing styles
    new_style = "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;" + style_str
    
    return f'{prefix}style="{new_style}"{suffix}'

# Regex to find mxCell tags that have edge="1" and extract their style attribute
# We look for <mxCell ... style="..." ... edge="1" ... > or similar.
# Since attributes can be in any order, we do a multi-step replacement.

# Find all mxCell tags
def process_tag(match):
    tag = match.group(0)
    if 'edge="1"' in tag:
        # Update style attribute inside this tag
        tag = re.sub(r'(<mxCell[^>]*?\s)style="([^"]*)"([^>]*>)', update_style, tag)
    return tag

new_content = re.sub(r'<mxCell[^>]+>', process_tag, content)

with open(output_file, 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Edge styles successfully updated. Saved to cl_fixed.drawio.xml")
