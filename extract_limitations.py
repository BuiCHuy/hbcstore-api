import PyPDF2
import re

pdf_path = r"C:\Users\fptshop\.gemini\antigravity\brain\2b69dc73-9789-4fd3-a7a1-00ea0c8f449a\media__1783050984753.pdf"

with open(pdf_path, 'rb') as f:
    reader = PyPDF2.PdfReader(f)
    for i in range(len(reader.pages)-10, len(reader.pages)):
        text = reader.pages[i].extract_text()
        if "hạn chế" in text.lower() or "phát triển" in text.lower() or "tương lai" in text.lower():
            print(f"--- PAGE {i} ---")
            print(text)
