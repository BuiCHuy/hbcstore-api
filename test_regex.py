import re

input_xml = r"c:\Users\fptshop\hbcstore-api\cl.drawio.xml"
with open(input_xml, 'r', encoding='utf-8') as f:
    xml_content = f.read()

pattern = r'(<mxCell[^>]*?parent="(\w+)"[^>]*?value="\+\s*(\w+)\s*:)[^"]*("[^>]*>)'

matches = re.finditer(pattern, xml_content)
count = 0
for match in matches:
    count += 1
    if count <= 5:
        print("Match:", match.group(2), match.group(3))

print(f"Total matches: {count}")
