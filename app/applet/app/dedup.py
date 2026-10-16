path = "app/src/main/java/com/example/ui/dashboard/ToolSelectionDashboard.kt"
with open(path, "r") as f:
    content = f.read()

# Let us find and remove the second item(key = "market_structure_grid") block
key_str = 'item(key = "market_structure_grid")'
parts = content.split(key_str)
print(f"Parts count: {len(parts)}")
if len(parts) > 2:
    # Keep parts[0] + key_str + parts[1] and join the rest without key_str
    # Actually, parts[0] is before 1st occurrence. parts[1] is between 1st and 2nd. parts[2] is after 2nd.
    new_content = parts[0] + key_str + parts[1] + "".join(parts[2:])
    with open(path, "w") as f:
        f.write(new_content)
    print("Successfully removed duplicate market_structure_grid item")
else:
    print("No duplicate found or unexpected structure")
