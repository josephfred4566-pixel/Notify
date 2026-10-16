path = "app/src/main/java/com/example/ui/dashboard/ToolSelectionDashboard.kt"
with open(path, "r") as f:
    content = f.read()

key_str = 'item(key = "market_structure_grid")'
parts = content.split(key_str)
print(f"Parts count: {len(parts)}")
if len(parts) > 2:
    new_content = parts[0] + key_str + parts[1] + "".join(parts[2:])
    with open(path, "w") as f:
        f.write(new_content)
    print("Successfully removed duplicate market_structure_grid item")
else:
    print("No duplicate found or unexpected structure")
