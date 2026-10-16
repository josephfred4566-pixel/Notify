import os
import subprocess
import numpy as np
from PIL import Image, ImageDraw, ImageFont

WIDTH, HEIGHT = 720, 1280
FPS = 30
DURATION = 4 # seconds
TOTAL_FRAMES = FPS * DURATION

os.makedirs("frames", exist_ok=True)
os.makedirs("app/src/main/res/raw", exist_ok=True)

# Generate frames
for i in range(TOTAL_FRAMES):
    t = i / float(TOTAL_FRAMES)
    progress = i / float(TOTAL_FRAMES)
    
    # Base background: deep charcoal #111827
    img = Image.new("RGB", (WIDTH, HEIGHT), color=(17, 24, 39))
    draw = ImageDraw.Draw(img)
    
    # Draw subtle background grid
    grid_spacing = 60
    for x in range(0, WIDTH, grid_spacing):
        draw.line([(x, 0), (x, HEIGHT)], fill=(31, 41, 55), width=1)
    for y in range(0, HEIGHT, grid_spacing):
        draw.line([(0, y), (WIDTH, y)], fill=(31, 41, 55), width=1)
        
    # Draw animated trading chart waveform / candles in center
    chart_center_y = HEIGHT // 2 + 100
    points = []
    for x in range(50, WIDTH - 50, 20):
        # wave function
        offset = np.sin((x + i * 8) * 0.02) * 120 + np.cos((x - i * 5) * 0.01) * 60
        y = chart_center_y + int(offset * min(1.0, t * 2))
        points.append((x, y))
        
    if len(points) > 1:
        # draw glowing line
        for j in range(len(points) - 1):
            color = (59, 130, 246) if j % 2 == 0 else (239, 68, 68)
            draw.line([points[j], points[j+1]], fill=color, width=4)
            
    # Draw glowing circles / nodes
    for px, py in points[::3]:
        draw.ellipse([px-6, py-6, px+6, py+6], fill=(255, 255, 255), outline=(59, 130, 246), width=2)

    # Draw central branding card / glassmorphism panel
    card_alpha = min(255, int(t * 500))
    card_box = [60, HEIGHT // 2 - 220, WIDTH - 60, HEIGHT // 2 - 40]
    draw.rounded_rectangle(card_box, radius=24, fill=(30, 41, 59), outline=(59, 130, 246), width=3)
    
    # Draw Title: NOTIFY
    # Use default bitmap font or load one if available
    try:
        font_large = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 54)
        font_sub = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 24)
    except:
        font_large = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        
    # Pulsing scale effect for text
    alpha_text = int(min(255, t * 300))
    draw.text((WIDTH//2 - 110, HEIGHT // 2 - 170), "NOTIFY", fill=(59, 130, 246), font=font_large)
    draw.text((WIDTH//2 - 130, HEIGHT // 2 - 100), "PRO TRADING INTELLIGENCE", fill=(209, 213, 219), font=font_sub)
    
    # Draw status pill
    pill_box = [WIDTH // 2 - 120, HEIGHT // 2 + 20, WIDTH // 2 + 120, HEIGHT // 2 + 70]
    draw.rounded_rectangle(pill_box, radius=25, fill=(239, 68, 68))
    draw.text((WIDTH // 2 - 95, HEIGHT // 2 + 32), "PRIME SIGNAL ACTIVE", fill=(255, 255, 255), font=font_sub)
    
    filename = f"frames/frame_{i:04d}.png"
    img.save(filename)

print("Frames generated successfully.")
