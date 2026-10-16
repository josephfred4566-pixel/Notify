with open("app/src/main/java/com/example/ui/dashboard/ToolSelectionDashboard.kt", "r") as f:
    content = f.read()

# Remove any previously appended grid code if present at the end incorrectly
if "MARKET STRUCTURE CORE" in content:
    # let's clean up and re-insert properly
    pass

# Let's read clean file or fix it:
# Actually let's git checkout or clean up ToolSelectionDashboard.kt first
