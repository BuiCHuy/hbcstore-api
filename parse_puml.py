import os
import re

def extract_puml(directory):
    puml = ["@startuml", "skinparam classAttributeIconSize 0", "skinparam shadowing false", "skinparam roundcorner 5", "skinparam class {", "    BackgroundColor White", "    ArrowColor Black", "    BorderColor Black", "}"]
    relationships = []
    
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                path = os.path.join(root, file)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if "@Entity" in content:
                        class_match = re.search(r'public class (\w+)', content)
                        if not class_match:
                            continue
                        class_name = class_match.group(1)
                        puml.append(f"\nclass {class_name} {{")
                        
                        lines = content.split('\n')
                        for line in lines:
                            line = line.strip()
                            # Match private fields
                            field_match = re.search(r'private\s+([A-Za-z0-9_<>,\s]+)\s+(\w+)\s*;', line)
                            if field_match:
                                type_name = field_match.group(1).strip()
                                field_name = field_match.group(2).strip()
                                # Ignore static/final
                                if "static " in line or "final " in line:
                                    continue
                                puml.append(f"  +{field_name}: {type_name}")
                                
                                # simple relationship detection
                                if type_name in ["Category", "Subcategory", "Brand", "User", "Product", "StoreOrder", "Coupon", "Cart"]:
                                    relationships.append(f"{type_name} \"1\" <-- \"0..*\" {class_name}")
                                elif type_name.startswith("List<") or type_name.startswith("Set<"):
                                    inner_match = re.search(r'<(.*?)>', type_name)
                                    if inner_match:
                                        inner_type = inner_match.group(1)
                                        # Only add relationship if inner_type is an entity
                                        relationships.append(f"{class_name} \"1\" *-- \"0..*\" {inner_type}")

                        puml.append("}")

    puml.append("\n' ====== MỐI QUAN HỆ CƠ BẢN ======")
    for rel in set(relationships):
        puml.append(rel)
        
    puml.append("@enduml")
    return "\n".join(puml)

if __name__ == "__main__":
    with open("plantuml_output.txt", "w", encoding="utf-8") as out:
        out.write(extract_puml("src/main/java/com/hbcstore/hbcstore_api"))
    print("Done generating plantuml_output.txt")
