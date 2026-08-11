#!/usr/bin/env python3
"""
AR DOOM Sprite & Asset Generator
Generates pixel art PNG textures for enemies (idle, hurt, dying),
weapon icons, pickup icons, and UI elements.
"""

import os
from PIL import Image, ImageDraw

OUTPUT_DIR = "app/src/main/assets/sprites"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Helper function to upscale a low-res pixel art canvas
def scale_up(img, scale):
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)

# Helper to apply hurt tint (red/white flash)
def create_hurt_variant(base_img):
    img = base_img.copy()
    pixels = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if a > 0:
                # Tint towards bright red/white flash
                nr = min(255, int(r * 0.4 + 210))
                ng = min(255, int(g * 0.2 + 80))
                nb = min(255, int(b * 0.2 + 80))
                pixels[x, y] = (nr, ng, nb, a)
    return img

# Helper to apply dying transformation (darken, collapse down, semi-transparent)
def create_dying_variant(base_img):
    w, h = base_img.size
    # Compress vertically by 25% and shift down to simulate collapsing/slumping
    compressed = base_img.resize((w, int(h * 0.75)), Image.NEAREST)
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    canvas.paste(compressed, (0, int(h * 0.25)))
    
    pixels = canvas.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if a > 0:
                # Darken color to 40% and lower alpha to 160 (~63% opacity)
                nr = int(r * 0.4)
                ng = int(g * 0.4)
                nb = int(b * 0.4)
                na = int(a * 0.63)
                pixels[x, y] = (nr, ng, nb, na)
    return canvas

# ==========================================
# ENEMY 1: IMP (Brown/red demon, horned)
# ==========================================
def draw_imp():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    OUTLINE = (25, 12, 8, 255)
    DARK_BROWN = (75, 35, 18, 255)
    MID_BROWN = (130, 60, 25, 255)
    LIGHT_BROWN = (180, 95, 35, 255)
    BONE = (215, 195, 145, 255)
    EYE_RED = (255, 40, 0, 255)
    EYE_YELLOW = (255, 200, 0, 255)

    # Outline Body
    draw.polygon([(11, 4), (20, 4), (23, 8), (26, 12), (25, 21), (22, 30), (17, 30), (16, 22), (14, 22), (13, 30), (8, 30), (5, 21), (4, 12), (7, 8)], fill=OUTLINE)
    
    # Inner Body (Mid Brown)
    draw.polygon([(12, 5), (19, 5), (22, 9), (24, 13), (23, 20), (21, 29), (18, 29), (15, 21), (13, 29), (10, 29), (7, 20), (6, 13), (9, 9)], fill=MID_BROWN)
    
    # Horns
    draw.polygon([(12, 5), (9, 2), (7, 2), (8, 5)], fill=BONE)
    draw.polygon([(19, 5), (22, 2), (24, 2), (23, 5)], fill=BONE)
    draw.polygon([(9, 2), (7, 2), (8, 5)], fill=OUTLINE) # horn outline edge
    draw.polygon([(22, 2), (24, 2), (23, 5)], fill=OUTLINE)

    # Shading (Dark Brown on right/bottom)
    draw.rectangle([18, 12, 22, 20], fill=DARK_BROWN)
    draw.rectangle([18, 22, 20, 28], fill=DARK_BROWN)

    # Highlights (Light Brown on chest/head left)
    draw.rectangle([12, 12, 16, 18], fill=LIGHT_BROWN)
    draw.rectangle([12, 6, 15, 9], fill=LIGHT_BROWN)

    # Spikes on shoulder and chest
    draw.polygon([(8, 12), (5, 11), (7, 14)], fill=BONE)
    draw.polygon([(22, 12), (25, 11), (23, 14)], fill=BONE)
    draw.rectangle([13, 15, 14, 16], fill=BONE)
    draw.rectangle([16, 15, 17, 16], fill=BONE)

    # Claws / Hands raised
    draw.polygon([(5, 10), (3, 6), (5, 8)], fill=BONE)
    draw.polygon([(25, 10), (27, 6), (25, 8)], fill=BONE)

    # Eyes & Mouth
    draw.point((13, 8), fill=EYE_RED)
    draw.point((14, 8), fill=EYE_YELLOW)
    draw.point((17, 8), fill=EYE_YELLOW)
    draw.point((18, 8), fill=EYE_RED)
    
    # Tooth gap
    draw.rectangle([14, 10, 17, 11], fill=(20, 5, 0, 255))
    draw.point((14, 10), fill=BONE)
    draw.point((17, 10), fill=BONE)

    return scale_up(img, 4)

# ==========================================
# ENEMY 2: SOLDIER (Green humanoid with gun)
# ==========================================
def draw_soldier():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (12, 22, 12, 255)
    ARMOR_DARK = (28, 58, 28, 255)
    ARMOR_MID = (55, 115, 55, 255)
    ARMOR_LIGHT = (90, 170, 90, 255)
    SKIN = (135, 128, 100, 255)
    GUN_DARK = (45, 50, 55, 255)
    GUN_STEEL = (110, 115, 125, 255)
    VISOR_RED = (255, 20, 20, 255)

    # Base silhouette
    draw.polygon([(11, 3), (20, 3), (23, 7), (25, 12), (24, 21), (22, 30), (17, 30), (16, 22), (14, 22), (13, 30), (8, 30), (6, 21), (6, 12), (8, 7)], fill=OUTLINE)

    # Helmet & Armor
    draw.polygon([(12, 4), (19, 4), (21, 7), (21, 10), (10, 10), (10, 7)], fill=ARMOR_MID)
    draw.rectangle([12, 4, 16, 6], fill=ARMOR_LIGHT) # helmet highlight
    
    # Zombie Face
    draw.rectangle([11, 8, 20, 11], fill=SKIN)
    draw.rectangle([13, 8, 18, 9], fill=VISOR_RED) # Glowing red eyes/visor

    # Body Armor
    draw.polygon([(9, 12), (22, 12), (21, 19), (10, 19)], fill=ARMOR_MID)
    draw.rectangle([10, 12, 14, 18], fill=ARMOR_LIGHT)
    draw.rectangle([17, 12, 21, 18], fill=ARMOR_DARK)

    # Legs / Combat pants
    draw.rectangle([9, 20, 13, 27], fill=(110, 95, 65, 255))
    draw.rectangle([18, 20, 22, 27], fill=(80, 70, 45, 255))

    # Boots
    draw.rectangle([8, 28, 13, 30], fill=(30, 30, 35, 255))
    draw.rectangle([18, 28, 23, 30], fill=(30, 30, 35, 255))

    # Rifle across chest
    draw.rectangle([6, 16, 26, 18], fill=GUN_DARK)
    draw.rectangle([18, 15, 28, 17], fill=GUN_STEEL) # barrel
    draw.rectangle([10, 18, 12, 21], fill=GUN_DARK) # magazine

    return scale_up(img, 4)

# ==========================================
# ENEMY 3: DEMON (Pinky - pink/purple large demon)
# ==========================================
def draw_demon():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (30, 10, 20, 255)
    DARK_PINK = (100, 20, 60, 255)
    MID_PINK = (165, 35, 100, 255)
    LIGHT_PINK = (220, 70, 140, 255)
    BONE = (235, 225, 185, 255)
    EYE_YELLOW = (255, 240, 0, 255)

    # Broad bulky body silhouette
    draw.polygon([(7, 4), (24, 4), (28, 9), (29, 18), (26, 29), (21, 29), (19, 22), (13, 22), (11, 29), (6, 29), (3, 18), (4, 9)], fill=OUTLINE)

    # Main Body fill
    draw.polygon([(8, 5), (23, 5), (27, 10), (28, 17), (25, 28), (22, 28), (18, 21), (14, 21), (10, 28), (7, 28), (4, 17), (5, 10)], fill=MID_PINK)

    # Shading and highlights
    draw.polygon([(8, 6), (16, 6), (16, 18), (8, 18)], fill=LIGHT_PINK) # Left highlight
    draw.polygon([(17, 6), (25, 6), (26, 18), (17, 18)], fill=DARK_PINK) # Right shadow

    # Horns on head curving forward
    draw.polygon([(7, 5), (4, 2), (2, 4), (5, 8)], fill=BONE)
    draw.polygon([(24, 5), (27, 2), (29, 4), (26, 8)], fill=BONE)
    draw.polygon([(4, 2), (2, 4), (5, 8)], fill=OUTLINE)
    draw.polygon([(27, 2), (29, 4), (26, 8)], fill=OUTLINE)

    # Big gaping jaw & sharp teeth
    draw.rectangle([9, 11, 22, 17], fill=(20, 5, 10, 255))
    # Top teeth
    for x in range(9, 22, 2):
        draw.line([(x, 11), (x, 13)], fill=BONE)
    # Bottom teeth
    for x in range(10, 22, 2):
        draw.line([(x, 17), (x, 15)], fill=BONE)

    # Eyes
    draw.rectangle([10, 8, 12, 9], fill=EYE_YELLOW)
    draw.rectangle([19, 8, 21, 9], fill=EYE_YELLOW)

    # Muscular legs
    draw.rectangle([7, 22, 10, 27], fill=MID_PINK)
    draw.rectangle([21, 22, 24, 27], fill=DARK_PINK)

    return scale_up(img, 4)

# ==========================================
# ENEMY 4: CACODEMON (Red floating eyeball)
# ==========================================
def draw_cacodemon():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (35, 5, 5, 255)
    DARK_RED = (115, 15, 15, 255)
    MID_RED = (190, 25, 25, 255)
    LIGHT_RED = (240, 70, 70, 255)
    EYE_SCLERA = (240, 240, 240, 255)
    EYE_IRIS = (0, 220, 60, 255)
    HORN_BLUE = (100, 140, 180, 255)

    # Top & Bottom Horn Spikes
    horns = [(8, 2), (12, 1), (19, 1), (23, 2), (9, 29), (22, 29)]
    for hx, hy in horns:
        draw.rectangle([hx-1, hy-1, hx+1, hy+1], fill=HORN_BLUE)

    # Spherical body outline
    draw.ellipse([3, 3, 28, 28], fill=OUTLINE)
    draw.ellipse([4, 4, 27, 27], fill=MID_RED)

    # Red Highlights & Shadows
    draw.ellipse([5, 5, 18, 18], fill=LIGHT_RED) # top-left light
    draw.ellipse([14, 14, 26, 26], fill=DARK_RED) # bottom-right shadow

    # Giant Single Eye
    draw.ellipse([10, 7, 21, 15], fill=OUTLINE)
    draw.ellipse([11, 8, 20, 14], fill=EYE_SCLERA)
    draw.ellipse([13, 9, 18, 13], fill=EYE_IRIS)
    draw.rectangle([15, 10, 16, 12], fill=(0, 50, 10, 255)) # Pupil
    draw.point((13, 9), fill=(255, 255, 255, 255)) # Eye reflection

    # Grimacing Maw full of teeth
    draw.polygon([(7, 18), (24, 18), (21, 25), (10, 25)], fill=(20, 0, 0, 255))
    # Teeth
    for x in range(8, 24, 2):
        draw.line([(x, 18), (x, 20)], fill=HORN_BLUE)
        draw.line([(x+1, 24), (x+1, 22)], fill=HORN_BLUE)

    return scale_up(img, 4)

# ==========================================
# ENEMY 5: BARON OF HELL (Green/tan horned demon)
# ==========================================
def draw_baron():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (15, 20, 10, 255)
    TAN_DARK = (85, 60, 35, 255)
    TAN_MID = (145, 105, 65, 255)
    TAN_LIGHT = (195, 155, 105, 255)
    GREEN_DARK = (25, 75, 30, 255)
    GREEN_MID = (50, 140, 55, 255)
    GREEN_LIGHT = (90, 195, 95, 255)
    HORN_DARK = (45, 40, 35, 255)
    PLASMA_GREEN = (50, 255, 120, 255)

    # Base Silhouette
    draw.polygon([(8, 3), (23, 3), (27, 8), (28, 18), (25, 30), (20, 30), (18, 22), (13, 22), (11, 30), (6, 30), (3, 18), (4, 8)], fill=OUTLINE)

    # Upper body - Tan / Brown
    draw.polygon([(9, 4), (22, 4), (26, 8), (25, 18), (6, 18), (5, 8)], fill=TAN_MID)
    draw.rectangle([9, 5, 15, 17], fill=TAN_LIGHT)
    draw.rectangle([16, 5, 24, 17], fill=TAN_DARK)

    # Lower body - Green goat legs
    draw.polygon([(6, 18), (25, 18), (24, 29), (20, 29), (17, 21), (14, 21), (11, 29), (7, 29)], fill=GREEN_MID)
    draw.rectangle([7, 19, 11, 28], fill=GREEN_LIGHT)
    draw.rectangle([20, 19, 24, 28], fill=GREEN_DARK)

    # Large Curved Horns
    draw.polygon([(9, 5), (4, 1), (2, 3), (6, 8)], fill=HORN_DARK)
    draw.polygon([(22, 5), (27, 1), (29, 3), (25, 8)], fill=HORN_DARK)

    # Green glowing eyes
    draw.point((12, 8), fill=PLASMA_GREEN)
    draw.point((19, 8), fill=PLASMA_GREEN)

    # Plasma ball in left hand
    draw.ellipse([2, 13, 7, 18], fill=PLASMA_GREEN)
    draw.ellipse([3, 14, 6, 17], fill=(220, 255, 230, 255))

    return scale_up(img, 4)

# ==========================================
# WEAPON ICONS (64x64 pixels)
# ==========================================
def draw_weapon_pistol():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (10, 10, 15, 255)
    STEEL_DARK = (45, 50, 55, 255)
    STEEL_MID = (90, 95, 105, 255)
    STEEL_LIGHT = (180, 190, 200, 255)
    GRIP_WOOD = (55, 35, 25, 255)

    # Slide / Barrel
    draw.rectangle([7, 9, 25, 15], fill=OUTLINE)
    draw.rectangle([8, 10, 24, 14], fill=STEEL_MID)
    draw.rectangle([8, 10, 24, 11], fill=STEEL_LIGHT) # Top slide shine
    draw.rectangle([24, 11, 27, 13], fill=STEEL_DARK) # Barrel tip

    # Lower frame & Grip
    draw.polygon([(10, 15), (17, 15), (14, 26), (8, 26)], fill=OUTLINE)
    draw.polygon([(11, 15), (16, 15), (13, 25), (9, 25)], fill=GRIP_WOOD)

    # Trigger Guard
    draw.rectangle([15, 15, 19, 19], fill=OUTLINE)
    draw.rectangle([16, 16, 18, 18], fill=(0, 0, 0, 0))
    draw.line([(16, 16), (16, 18)], fill=STEEL_LIGHT) # Trigger

    return scale_up(img, 2)

def draw_weapon_shotgun():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (15, 10, 10, 255)
    WOOD_DARK = (70, 35, 15, 255)
    WOOD_LIGHT = (140, 75, 35, 255)
    STEEL_DARK = (45, 50, 55, 255)
    STEEL_MID = (110, 115, 125, 255)

    # Long Barrel
    draw.rectangle([10, 11, 29, 15], fill=OUTLINE)
    draw.rectangle([11, 12, 28, 13], fill=STEEL_MID) # Top barrel
    draw.rectangle([11, 14, 28, 14], fill=STEEL_DARK) # Under barrel tube

    # Receiver
    draw.rectangle([10, 12, 16, 18], fill=OUTLINE)
    draw.rectangle([11, 13, 15, 17], fill=STEEL_DARK)

    # Wooden Pump Forend
    draw.rectangle([18, 14, 23, 18], fill=OUTLINE)
    draw.rectangle([19, 15, 22, 17], fill=WOOD_LIGHT)

    # Wooden Stock
    draw.polygon([(3, 17), (11, 15), (11, 19), (3, 23)], fill=OUTLINE)
    draw.polygon([(4, 18), (10, 16), (10, 18), (4, 22)], fill=WOOD_DARK)
    draw.line([(5, 18), (9, 17)], fill=WOOD_LIGHT)

    return scale_up(img, 2)

def draw_weapon_chaingun():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (15, 15, 20, 255)
    STEEL_DARK = (35, 35, 40, 255)
    STEEL_MID = (100, 105, 115, 255)
    STEEL_LIGHT = (170, 175, 185, 255)
    BRASS = (210, 170, 40, 255)

    # Main Housing Body
    draw.rectangle([7, 9, 18, 21], fill=OUTLINE)
    draw.rectangle([8, 10, 17, 20], fill=STEEL_DARK)
    draw.rectangle([9, 11, 16, 14], fill=STEEL_MID)

    # Six-Barrel Bundle
    draw.rectangle([18, 11, 29, 19], fill=OUTLINE)
    for y in [12, 14, 16, 18]:
        draw.line([(18, y), (28, y)], fill=STEEL_LIGHT if y%4==0 else STEEL_MID)
    # Barrel rings
    draw.rectangle([21, 11, 22, 19], fill=STEEL_DARK)
    draw.rectangle([26, 11, 27, 19], fill=STEEL_DARK)

    # Ammo Belt / Box underneath
    draw.rectangle([5, 19, 13, 26], fill=OUTLINE)
    draw.rectangle([6, 20, 12, 25], fill=BRASS)
    draw.line([(8, 20), (8, 25)], fill=(120, 90, 20, 255))

    # Top Handle
    draw.polygon([(9, 9), (12, 5), (16, 5), (17, 9)], fill=OUTLINE)
    draw.polygon([(10, 9), (12, 6), (15, 6), (16, 9)], fill=STEEL_MID)

    return scale_up(img, 2)

# ==========================================
# PICKUP ICONS (64x64 pixels)
# ==========================================
def draw_pickup_health():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (20, 20, 25, 255)
    BOX_WHITE = (230, 235, 240, 255)
    BOX_SHADOW = (170, 175, 185, 255)
    RED_CROSS = (210, 20, 20, 255)
    RED_LIGHT = (255, 70, 70, 255)

    # Container Outer Box
    draw.rectangle([4, 7, 27, 26], fill=OUTLINE)
    draw.rectangle([5, 8, 26, 25], fill=BOX_WHITE)
    draw.rectangle([16, 8, 26, 25], fill=BOX_SHADOW) # Right shadow half

    # Handle on top
    draw.rectangle([11, 4, 20, 8], fill=OUTLINE)
    draw.rectangle([13, 6, 18, 7], fill=(0, 0, 0, 0)) # handle gap

    # Bright Red Cross
    # Vertical bar
    draw.rectangle([13, 11, 18, 22], fill=OUTLINE)
    draw.rectangle([14, 12, 17, 21], fill=RED_CROSS)
    # Horizontal bar
    draw.rectangle([8, 14, 23, 19], fill=OUTLINE)
    draw.rectangle([9, 15, 22, 18], fill=RED_CROSS)
    # Cross highlight
    draw.rectangle([14, 12, 15, 21], fill=RED_LIGHT)

    return scale_up(img, 2)

def draw_pickup_ammo():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (20, 20, 10, 255)
    OLIVE_DARK = (90, 85, 25, 255)
    OLIVE_MID = (150, 135, 30, 255)
    OLIVE_LIGHT = (195, 175, 45, 255)
    BRASS = (240, 200, 40, 255)

    # Ammo Box
    draw.rectangle([4, 8, 27, 26], fill=OUTLINE)
    draw.rectangle([5, 9, 26, 25], fill=OLIVE_MID)
    draw.rectangle([5, 9, 26, 12], fill=OLIVE_LIGHT) # Top lid
    draw.rectangle([16, 13, 26, 25], fill=OLIVE_DARK) # Right shadow

    # Latch / Clasp
    draw.rectangle([14, 11, 17, 15], fill=OUTLINE)
    draw.rectangle([15, 12, 16, 14], fill=(200, 200, 200, 255))

    # Shell/Bullet symbol on box front
    for i in range(3):
        bx = 9 + i*5
        draw.rectangle([bx, 16, bx+2, 22], fill=BRASS)
        draw.polygon([(bx, 16), (bx+1, 14), (bx+2, 16)], fill=BRASS)

    return scale_up(img, 2)

def draw_pickup_armor():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (10, 20, 30, 255)
    SILVER = (180, 190, 205, 255)
    BLUE_DARK = (20, 60, 120, 255)
    BLUE_MID = (40, 120, 210, 255)
    CYAN_LIGHT = (80, 200, 255, 255)

    # Shield Outer Silhouette
    draw.polygon([(5, 4), (26, 4), (27, 16), (16, 28), (4, 16)], fill=OUTLINE)
    
    # Silver Rim
    draw.polygon([(6, 5), (25, 5), (26, 15), (16, 27), (5, 15)], fill=SILVER)

    # Inner Shield Crest
    draw.polygon([(8, 7), (23, 7), (24, 14), (16, 24), (7, 14)], fill=BLUE_DARK)
    draw.polygon([(8, 7), (16, 7), (16, 24), (7, 14)], fill=BLUE_MID) # Left half lighter

    # Glossy Shine Diagonal
    draw.polygon([(9, 8), (14, 8), (8, 14)], fill=CYAN_LIGHT)

    # Central Emblem (Cross/Star)
    draw.rectangle([15, 11, 17, 17], fill=CYAN_LIGHT)
    draw.rectangle([12, 13, 20, 15], fill=CYAN_LIGHT)

    return scale_up(img, 2)

# ==========================================
# UI ELEMENTS (128x128 pixels)
# ==========================================
def draw_title_logo():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # DOOM style logo text on 128x128 grid
    # We will draw large blocky 3D angled letters for "DOOM"
    OUTLINE = (10, 0, 0, 255)
    RED_DARK = (120, 10, 10, 255)
    ORANGE_MID = (230, 90, 10, 255)
    YELLOW_LIGHT = (255, 210, 30, 255)
    WHITE_FLASH = (255, 250, 200, 255)

    # Outer 3D Shadow Layer (Offset down-right)
    def draw_doom_text(dx, dy, color):
        # 'D'
        draw.rectangle([12+dx, 40+dy, 22+dx, 88+dy], fill=color)
        draw.polygon([(22+dx, 40+dy), (36+dx, 48+dy), (36+dx, 80+dy), (22+dx, 88+dy)], fill=color)
        
        # 'O' (1)
        draw.polygon([(40+dx, 48+dy), (48+dx, 40+dy), (60+dx, 40+dy), (68+dx, 48+dy), (68+dx, 80+dy), (60+dx, 88+dy), (48+dx, 88+dy), (40+dx, 80+dy)], fill=color)

        # 'O' (2)
        draw.polygon([(72+dx, 48+dy), (80+dx, 40+dy), (92+dx, 40+dy), (100+dx, 48+dy), (100+dx, 80+dy), (92+dx, 88+dy), (80+dx, 88+dy), (72+dx, 80+dy)], fill=color)

        # 'M'
        draw.rectangle([104+dx, 40+dy, 112+dx, 88+dy], fill=color)
        draw.rectangle([120+dx, 40+dy, 126+dx, 88+dy], fill=color)
        draw.polygon([(108+dx, 40+dy), (116+dx, 64+dy), (124+dx, 40+dy)], fill=color)

    # Draw black thick outline
    for ox in range(-3, 4):
        for oy in range(-3, 4):
            if ox*ox + oy*oy <= 9:
                draw_doom_text(ox, oy, OUTLINE)

    # Draw 3D extrude (Dark Red)
    for offset in range(5, 0, -1):
        draw_doom_text(offset, offset, RED_DARK)

    # Draw Main Face Gradient / Layers
    draw_doom_text(0, 0, ORANGE_MID)
    draw_doom_text(0, -2, YELLOW_LIGHT)
    draw_doom_text(0, -4, WHITE_FLASH)

    # Cut out holes for D, O, O
    def clear_holes(color):
        # D hole
        draw.polygon([(22, 52), (28, 56), (28, 72), (22, 76)], fill=color)
        # O1 hole
        draw.polygon([(48, 52), (60, 52), (60, 76), (48, 76)], fill=color)
        # O2 hole
        draw.polygon([(80, 52), (92, 52), (92, 76), (80, 76)], fill=color)

    # Clear inner letter holes with transparent / outline
    clear_holes((0, 0, 0, 0))

    return img

def draw_skull_icon():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    OUTLINE = (20, 15, 10, 255)
    BONE_DARK = (140, 130, 105, 255)
    BONE_MID = (200, 190, 160, 255)
    BONE_LIGHT = (240, 235, 210, 255)
    RED_EYE = (255, 20, 0, 255)
    HORN_DARK = (50, 40, 35, 255)

    # Demonic Horns
    draw.polygon([(8, 6), (3, 1), (1, 3), (5, 9)], fill=HORN_DARK)
    draw.polygon([(23, 6), (28, 1), (30, 3), (26, 9)], fill=HORN_DARK)

    # Cranium Base
    draw.polygon([(8, 6), (23, 6), (27, 11), (26, 20), (22, 28), (10, 28), (5, 20), (4, 11)], fill=OUTLINE)
    draw.polygon([(9, 7), (22, 7), (26, 12), (25, 19), (21, 27), (11, 27), (6, 19), (5, 12)], fill=BONE_MID)

    # Highlights & Shadows
    draw.polygon([(9, 7), (16, 7), (16, 26), (11, 26)], fill=BONE_LIGHT) # Left light
    draw.polygon([(17, 7), (22, 7), (25, 12), (20, 26)], fill=BONE_DARK) # Right shadow

    # Ominous Hollow Eye Sockets
    draw.rectangle([8, 12, 13, 17], fill=OUTLINE)
    draw.rectangle([18, 12, 23, 17], fill=OUTLINE)
    
    # Glowing Red Pupils
    draw.point((11, 14), fill=RED_EYE)
    draw.point((19, 14), fill=RED_EYE)

    # Triangular Nose Cavity
    draw.polygon([(15, 18), (17, 18), (16, 20)], fill=OUTLINE)

    # Jagged Teeth / Jaw
    draw.rectangle([10, 22, 21, 26], fill=OUTLINE)
    for x in range(11, 21, 2):
        draw.line([(x, 22), (x, 24)], fill=BONE_LIGHT)
        draw.line([(x+1, 26), (x+1, 24)], fill=BONE_LIGHT)

    return scale_up(img, 4)

# ==========================================
# MAIN GENERATION PROCESS
# ==========================================
def main():
    created_files = []

    # 1. Enemies
    enemies = {
        "imp": draw_imp,
        "soldier": draw_soldier,
        "demon": draw_demon,
        "cacodemon": draw_cacodemon,
        "baron": draw_baron
    }

    print("Generating Enemy Sprites...")
    for type_name, draw_fn in enemies.items():
        # Base Idle
        idle_img = draw_fn()
        idle_path = os.path.join(OUTPUT_DIR, f"{type_name}_idle.png")
        idle_img.save(idle_path)
        created_files.append(idle_path)

        # Hurt state
        hurt_img = create_hurt_variant(idle_img)
        hurt_path = os.path.join(OUTPUT_DIR, f"{type_name}_hurt.png")
        hurt_img.save(hurt_path)
        created_files.append(hurt_path)

        # Dying state
        dying_img = create_dying_variant(idle_img)
        dying_path = os.path.join(OUTPUT_DIR, f"{type_name}_dying.png")
        dying_img.save(dying_path)
        created_files.append(dying_path)

    # 2. Weapon Icons
    print("Generating Weapon Icons...")
    weapons = {
        "weapon_pistol.png": draw_weapon_pistol,
        "weapon_shotgun.png": draw_weapon_shotgun,
        "weapon_chaingun.png": draw_weapon_chaingun
    }
    for file_name, draw_fn in weapons.items():
        img = draw_fn()
        path = os.path.join(OUTPUT_DIR, file_name)
        img.save(path)
        created_files.append(path)

    # 3. Pickup Icons
    print("Generating Pickup Icons...")
    pickups = {
        "pickup_health.png": draw_pickup_health,
        "pickup_ammo.png": draw_pickup_ammo,
        "pickup_armor.png": draw_pickup_armor
    }
    for file_name, draw_fn in pickups.items():
        img = draw_fn()
        path = os.path.join(OUTPUT_DIR, file_name)
        img.save(path)
        created_files.append(path)

    # 4. UI Elements
    print("Generating UI Elements...")
    ui_elements = {
        "title_logo.png": draw_title_logo,
        "skull_icon.png": draw_skull_icon
    }
    for file_name, draw_fn in ui_elements.items():
        img = draw_fn()
        path = os.path.join(OUTPUT_DIR, file_name)
        img.save(path)
        created_files.append(path)

    print("\n--- Generation Summary ---")
    print(f"Total files generated: {len(created_files)}")
    for f in created_files:
        print(f" - {f}")

if __name__ == "__main__":
    main()
