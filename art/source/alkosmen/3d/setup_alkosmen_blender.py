"""Alkosmen — Blender scene starter; run via Blender Scripting > Run Script.

Requires Blender 4.x or newer. Keep the four PNGs next to this .py.
Creates Blender scene, three image reference planes, editable rough blockout,
camera and lights. A .glb can be exported later, once the model is properly finished.

NB: The blockout is NOT a completed, rigged, textured game character.
"""

import bpy
import math
from pathlib import Path
from mathutils import Vector

SCRIPT_NAME = "setup_alkosmen_blender.py"

def find_folder():
    possible = []
    try:
        possible.append(Path(__file__).resolve().parent)
    except (NameError, OSError):
        pass
    # Scripting > Text Editor > Open the .py, then Run Script.
    try:
        t = bpy.data.texts.get(SCRIPT_NAME)
        if t and t.filepath:
            possible.append(Path(bpy.path.abspath(t.filepath)).resolve().parent)
    except Exception:
        pass
    try:
        t = bpy.context.space_data.text
        if t and t.filepath:
            possible.append(Path(bpy.path.abspath(t.filepath)).resolve().parent)
    except Exception:
        pass
    possible.append(Path.cwd())
    filenames = ["alkosmen_ref_front.png", "alkosmen_ref_side.png", "alkosmen_ref_back.png"]
    for folder in possible:
        if all((folder / f).is_file() for f in filenames):
            return folder
    raise FileNotFoundError("Keep setup_alkosmen_blender.py alongside the three alkosmen_ref_*.png images")

ROOT = find_folder()
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)
for col in list(bpy.data.collections):
    if col.name in {"01_REFERENCE", "02_BLOCKOUT", "03_RIG_GUIDES", "04_LIGHTS"}:
        bpy.data.collections.remove(col)

def collection(name):
    c = bpy.data.collections.new(name)
    bpy.context.scene.collection.children.link(c)
    return c

references = collection("01_REFERENCE")
blockout = collection("02_BLOCKOUT")
rig_guides = collection("03_RIG_GUIDES")
lights = collection("04_LIGHTS")

scene = bpy.context.scene
scene.unit_settings.system = 'METRIC'
scene.unit_settings.scale_length = 1.0
try:
    scene.render.engine = 'BLENDER_EEVEE_NEXT'
except TypeError:
    scene.render.engine = 'BLENDER_EEVEE'
scene.render.resolution_x = 1280
scene.render.resolution_y = 720
scene.render.resolution_percentage = 100
scene.world.color = (0.13, 0.13, 0.15)


def move_collection(obj, target):
    for c in list(obj.users_collection):
        c.objects.unlink(obj)
    target.objects.link(obj)


def material(name, rgb, roughness=0.85):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*rgb, 1.0)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get('Principled BSDF')
    if bsdf:
        bsdf.inputs['Base Color'].default_value = (*rgb, 1)
        bsdf.inputs['Roughness'].default_value = roughness
    return mat

skin = material('Skin · теплый', (0.70, 0.40, 0.29))
shirt = material('Shirt · красный цветочный', (0.78, 0.06, 0.055))
shorts = material('Shorts · светлые', (0.76, 0.72, 0.60))
boots = material('Boots · черные', (0.075, 0.06, 0.055))
socks = material('Socks · белые', (0.90, 0.86, 0.80))
hair = material('Hair · каштановые', (0.18, 0.09, 0.050))
bag = material('Backpack · ABIBAS', (0.22, 0.17, 0.13))
leaf = material('Patterns · зелёные', (0.15, 0.47, 0.21))
flower = material('Patterns · желтые', (0.98, 0.68, 0.22))


def sphere(name, pos, size, mat):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=16, ring_count=12, location=pos)
    ob = bpy.context.object
    ob.name = name
    ob.scale = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    ob.data.materials.append(mat)
    move_collection(ob, blockout)
    for p in ob.data.polygons:
        p.use_smooth = True
    return ob


def cube(name, pos, size, mat, bevel=0.05):
    bpy.ops.mesh.primitive_cube_add(size=1, location=pos)
    ob = bpy.context.object
    ob.name = name
    ob.dimensions = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    ob.data.materials.append(mat)
    move_collection(ob, blockout)
    if bevel:
        mod = ob.modifiers.new('Rounded edges', 'BEVEL')
        mod.width = bevel
        mod.segments = 2
        ob.modifiers.new('Weighted normals', 'WEIGHTED_NORMAL')
    return ob

# Units are metres. Origin sits between feet. Character looks toward negative Y.
# Intentionally simple and separate meshes for later sculpting.
cube('Torso · red Hawaiian shirt', (0, 0, 1.22), (0.59, 0.35, 0.63), shirt, .09)
cube('Hips · light shorts', (0, 0, .87), (.53, .34, .27), shorts, .045)
for label, sign in [('L', 1), ('R', -1)]:
    x = sign*.16
    sphere(f'Upper arm {label}', (sign*.36, -.01, 1.28), (.12, .13, .25), skin)
    sphere(f'Hand {label}', (sign*.39, -.015, .98), (.093, .085, .11), skin)
    cube(f'Shorts leg {label}', (x, 0, .76), (.23, .27, .23), shorts, .04)
    sphere(f'Leg {label}', (x, 0, .46), (.113, .12, .23), skin)
    cube(f'Sock {label}', (x, -.015, .23), (.19, .21, .14), socks, .025)
    cube(f'Boot {label}', (x, -.075, .12), (.24, .38, .23), boots, .04)
sphere('Neck', (0, 0, 1.59), (.13, .13, .15), skin)
sphere('Head · placeholder for sculpt', (0, -.05, 1.77), (.25, .235, .29), skin)
sphere('Hair volume', (0, .015, 1.98), (.26, .24, .13), hair)
sphere('Nose red · adjust', (0, -.295, 1.77), (.082, .085, .07), skin)
cube('ABIBAS backpack · sculpt later', (0, .27, 1.29), (.44, .23, .51), bag, .11)
for x in [-.20,.20]:
    cube('Backpack strap', (x, -.205, 1.32), (.065, .075, .52), bag, .028)
# Pattern placeholders on front of shirt. Production artwork needs proper UV textures.
for idx, (x,z) in enumerate([(-.18,1.42),(.18,1.31),(-.10,1.1),(.12,1.46),(.2,1.06)]):
    sphere('Flower placeholder %02d' % idx,(x,-.187,z),(.040,.012,.042),flower)
for idx,(x,z) in enumerate([(-.18,1.27),(.06,1.20),(.20,1.53)]):
    sphere('Leaf placeholder %02d' % idx,(x,-.187,z),(.048,.011,.025),leaf)

# Poses: front looks from negative Y, back from positive Y, side from X.
# Ref images are image empties, not modeled geometry.
def reference(name, filename, pos, yaw):
    image = bpy.data.images.load(str(ROOT / filename), check_existing=True)
    ob = bpy.data.objects.new(name, None)
    references.objects.link(ob)
    ob.empty_display_type = 'IMAGE'
    ob.data = image
    ob.empty_display_size = 0.92  # image width in metres; portrait roughly 1.85 m high
    ob.empty_image_offset = (-0.5, -0.5)
    ob.location = pos
    ob.rotation_euler = (math.radians(90), 0, math.radians(yaw))
    ob.color = (1,1,1,0.65)
    ob.show_in_front = True
    ob.hide_render = True
    return ob

reference('REF front · front orthographic', 'alkosmen_ref_front.png', (0, +0.88, 0.94), 0)
reference('REF side · right orthographic', 'alkosmen_ref_side.png', (+0.86, 0, 0.94), -90)
reference('REF back · rear orthographic', 'alkosmen_ref_back.png', (0, -0.88, 0.94), 180)

# Joint locators for the future skeleton; not weighted armature animation.
for name, p in [('Root',(0,0,0.9)),('Neck',(0,0,1.59)),('Head',(0,0,1.83)),
                ('Hand.L',(.4,0,1.0)),('Hand.R',(-.4,0,1.0)),
                ('Foot.L',(.16,-.08,.12)),('Foot.R',(-.16,-.08,.12))]:
    ob = bpy.data.objects.new('Joint guide '+name, None)
    rig_guides.objects.link(ob)
    ob.empty_display_type = 'SPHERE'
    ob.empty_display_size = .08
    ob.location = p
rig_guides.hide_viewport = True

# Preview camera and lights.
def point_at(obj, position):
    direction = Vector(position)-obj.location
    obj.rotation_euler = direction.to_track_quat('-Z','Y').to_euler()

bpy.ops.object.camera_add(location=(3.1,-4.8,2.65))
cam = bpy.context.object
cam.name = 'Preview ISO camera'
move_collection(cam, lights)
point_at(cam,(0,0,1.0))
cam.data.type='ORTHO'
cam.data.ortho_scale=3.0
scene.camera=cam

bpy.ops.object.light_add(type='AREA',location=(-3,-3,4))
light=bpy.context.object
light.name='Softbox warm'
move_collection(light,lights)
light.data.energy=450
light.data.shape='DISK'
light.data.size=4
point_at(light,(0,0,1))

# Select a single mesh so the object list stays simple.
bpy.ops.object.select_all(action='DESELECT')
root = bpy.data.objects.get('Torso · red Hawaiian shirt')
if root:
    root.select_set(True)
    bpy.context.view_layer.objects.active = root

blend_path = ROOT / 'alkosmen_3d_start.blend'
bpy.ops.wm.save_as_mainfile(filepath=str(blend_path))
print('ALKO Blender starter saved:',blend_path)
print('NOTE: basic unrigged sculpting blockout only, not a finished production model.')