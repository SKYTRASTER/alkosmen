"""Алкосмен V2: stylized, editable 3D mesh for Blender 3.5+.

Open your existing alkosmen_3d_start.blend, then run this script in Scripting.
It preserves the original 02_BLOCKOUT collection (hidden, not deleted),
keeps reference images and lights, and makes a separate 05_CHARACTER_V2.
The mesh is a static modelling prototype, NOT rigged/animated/game-ready.
"""

import bpy
import math
from mathutils import Vector
from pathlib import Path

NAME = "05_CHARACTER_V2"


def find_resource_dir():
    paths = []
    try:
        paths.append(Path(__file__).resolve().parent)
    except (NameError, OSError):
        pass
    try:
        txt = bpy.context.space_data.text
        if txt and txt.filepath:
            paths.append(Path(bpy.path.abspath(txt.filepath)).resolve().parent)
    except (AttributeError, RuntimeError):
        pass
    for txt in bpy.data.texts:
        if txt.name == "upgrade_alkosmen_blender.py" and txt.filepath:
            paths.append(Path(bpy.path.abspath(txt.filepath)).resolve().parent)
    if bpy.data.filepath:
        paths.append(Path(bpy.data.filepath).resolve().parent)
    paths.append(Path.cwd())
    for p in paths:
        if (p / 'alkosmen_shirt_floral.png').is_file():
            return p
    return paths[0]


ROOT = find_resource_dir()
if not bpy.data.filepath:
    raise RuntimeError("Сначала откройте и сохраните alkosmen_3d_start.blend, затем запускайте этот скрипт")

# Nothing from earlier scenes is deleted, except a previous run of THIS script.
old = bpy.data.collections.get(NAME)
if old:
    for obj in list(old.all_objects):
        bpy.data.objects.remove(obj, do_unlink=True)
    bpy.data.collections.remove(old)
character = bpy.data.collections.new(NAME)
bpy.context.scene.collection.children.link(character)
old_blockout = bpy.data.collections.get('02_BLOCKOUT')
if old_blockout:
    old_blockout.hide_viewport = True
    old_blockout.hide_render = True


def move(obj):
    for collection in list(obj.users_collection):
        collection.objects.unlink(obj)
    character.objects.link(obj)
    return obj


def mat(name, color, roughness=0.85, metallic=0):
    full_name = 'ALKO V2 | ' + name
    m = bpy.data.materials.get(full_name) or bpy.data.materials.new(full_name)
    m.diffuse_color = (*color, 1)
    m.use_nodes = True
    # Blender may rename a shader or omit the default Principled node.
    # Detect by node type instead of relying on the English display name.
    nodes = m.node_tree.nodes
    shader = next((node for node in nodes if node.type == 'BSDF_PRINCIPLED'), None)
    if shader is None:
        shader = nodes.new('ShaderNodeBsdfPrincipled')
        shader.location = (-220, 0)
    output = next((node for node in nodes if node.type == 'OUTPUT_MATERIAL'), None)
    if output is None:
        output = nodes.new('ShaderNodeOutputMaterial')
        output.location = (120, 0)
    m.node_tree.links.new(shader.outputs['BSDF'], output.inputs['Surface'])
    shader.inputs['Base Color'].default_value = (*color, 1)
    shader.inputs['Roughness'].default_value = roughness
    shader.inputs['Metallic'].default_value = metallic
    return m

skin = mat('skin warm', (0.73, 0.445, 0.335))
skin_shadow = mat('skin shadow', (0.61, 0.30, 0.24))
beard = mat('subtle stubble', (0.37, 0.26, 0.22))
hair = mat('dark brown hair', (0.12, 0.065, 0.035))
hair_lights = mat('hair midtone', (0.21, 0.11, 0.055))
brows = mat('dark eyebrows', (0.095, 0.053, 0.035))
eye_white = mat('eye off-white', (0.93, 0.87, 0.79), 0.35)
iris = mat('iris brown', (0.22, 0.11, 0.048), 0.37)
pupil = mat('black pupil', (0.025, 0.019, 0.015), 0.24)
lips = mat('muted lips', (0.56, 0.26, 0.24))
shirt_red = mat('red collar and trim', (0.66, 0.055, 0.038))
shirt_dark = mat('shirt inner shadow', (0.28, 0.032, 0.025))
shorts = mat('beige shorts', (0.76, 0.72, 0.64))
shorts_seam = mat('shorts stitched seam', (0.59, 0.56, 0.48))
socks = mat('white socks', (0.87, 0.85, 0.79))
boots = mat('black leather', (0.046, 0.043, 0.047), 0.28)
sole = mat('boot rubber soles', (0.017, 0.017, 0.018))
laces = mat('boot stitching', (0.20, 0.18, 0.16))
bag = mat('dark brown ABIBAS backpack', (0.17, 0.125, 0.105))
bag_trim = mat('bag trim', (0.28, 0.21, 0.16))
logo_white = mat('backpack letters white', (0.91, 0.88, 0.81))
metal = mat('zipper metal', (0.64, 0.60, 0.49), .30, .45)

floral = mat('shirt floral image', (0.73, 0.04, 0.035))
texture_file = ROOT / 'alkosmen_shirt_floral.png'
if texture_file.exists():
    shader = next(node for node in floral.node_tree.nodes if node.type == 'BSDF_PRINCIPLED')
    tex = floral.node_tree.nodes.get('Alkosmen flower fabric')
    if tex is None:
        tex = floral.node_tree.nodes.new('ShaderNodeTexImage')
        tex.name = 'Alkosmen flower fabric'
    tex.image = bpy.data.images.load(str(texture_file), check_existing=True)
    tex.extension = 'REPEAT'
    tex.interpolation = 'Linear'
    floral.node_tree.links.new(tex.outputs['Color'], shader.inputs['Base Color'])
    try:
        tex.image.pack() # keep texture INSIDE saved .blend
    except RuntimeError:
        pass
else:
    print('WARNING: shirt texture absent; using plain red. Put alkosmen_shirt_floral.png beside this script.')


def smooth(obj):
    if obj.type == 'MESH':
        for poly in obj.data.polygons:
            poly.use_smooth = True
    return obj


def sphere(name, pos, size, material, seg=24, rings=16):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=rings, location=pos)
    ob = move(bpy.context.object)
    ob.name = 'V2 ' + name
    ob.scale = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    ob.data.materials.append(material)
    return smooth(ob)


def cube(name, pos, dims, material, bevel=0.0):
    bpy.ops.mesh.primitive_cube_add(size=1, location=pos)
    ob = move(bpy.context.object)
    ob.name = 'V2 ' + name
    ob.dimensions = dims
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    ob.data.materials.append(material)
    if bevel:
        mod = ob.modifiers.new('Softened edges', 'BEVEL')
        mod.width = bevel
        mod.segments = 3
        ob.modifiers.new('Normals', 'WEIGHTED_NORMAL')
    return ob


def cylinder_between(name, a, b, r1, r2, material, vertices=16):
    va, vb = Vector(a), Vector(b)
    direction = vb-va
    bpy.ops.mesh.primitive_cone_add(vertices=vertices, radius1=r1, radius2=r2,
                                    depth=direction.length, location=(va+vb)*0.5)
    ob = move(bpy.context.object)
    ob.name = 'V2 ' + name
    ob.rotation_euler = direction.to_track_quat('Z', 'Y').to_euler()
    ob.data.materials.append(material)
    return smooth(ob)


def line(name, pts, material, radius=.006, cyclic=False):
    curve = bpy.data.curves.new('V2 ' + name, type='CURVE')
    curve.dimensions = '3D'
    curve.resolution_u = 16
    curve.bevel_resolution = 2
    curve.bevel_depth = radius
    s = curve.splines.new('BEZIER')
    s.bezier_points.add(len(pts)-1)
    for p, coord in zip(s.bezier_points, pts):
        p.co = coord
        p.handle_left_type = 'AUTO'
        p.handle_right_type = 'AUTO'
    s.use_cyclic_u = cyclic
    ob = bpy.data.objects.new('V2 '+name, curve)
    character.objects.link(ob)
    ob.data.materials.append(material)
    return ob


def shirt_torso():
    # Elliptical-ring mesh: the front is toward -Y. More substantial midsection.
    rings = [(0.96,.29,.225), (1.10,.315,.243), (1.29,.307,.230),
             (1.47,.294,.206), (1.54,.236,.173)]
    segments = 32
    vertices=[]
    faces=[]
    UV=[]
    for i,(z,rx,ry) in enumerate(rings):
        for j in range(segments):
            t=2*math.pi*j/segments
            vertices.append((rx*math.cos(t), .005+ry*math.sin(t), z))
    for i in range(len(rings)-1):
        for j in range(segments):
            k=(j+1)%segments
            faces.append((i*segments+j, i*segments+k,(i+1)*segments+k,(i+1)*segments+j))
            UV.append([j/segments*2.7,k/segments*2.7,k/segments*2.7,j/segments*2.7])
    mesh=bpy.data.meshes.new('shirt tailored rings')
    mesh.from_pydata(vertices,[],faces)
    mesh.update()
    ob=bpy.data.objects.new('V2 Hawaiian shirt · shaped',mesh)
    character.objects.link(ob)
    ob.data.materials.append(floral)
    uv=mesh.uv_layers.new(name='Fabric repeat UV')
    for i,poly in enumerate(mesh.polygons):
        u_vals=UV[i]
        # Avoid seam stretching by raising the u of the first vertex when on wrap edge.
        if (i%segments)==segments-1:
            u_vals=[(segments-1)/segments*2.7,2.7,2.7,(segments-1)/segments*2.7]
        lower=(i//segments)/(len(rings)-1)*1.6
        upper=((i//segments)+1)/(len(rings)-1)*1.6
        uvcoords=[(u_vals[0],lower),(u_vals[1],lower),(u_vals[2],upper),(u_vals[3],upper)]
        for lid,vec in zip(poly.loop_indices,uvcoords):
            uv.data[lid].uv=vec
    smooth(ob)
    ob.modifiers.new('Subtle clothing thickness','SOLIDIFY').thickness=.011
    ob.modifiers.new('Shirt surface normals','WEIGHTED_NORMAL')
    return ob


# 1. Legs, shorts, torso and arm proportions.
shirt_torso()
sphere('undershirt opening', (0,-.183,1.48), (.096,.045,.12), skin)
for side in [-1,1]:
    x=side*.16
    leg='L' if side>0 else 'R'
    sphere(f'short cuff {leg}', (x,0,.81), (.15,.145,.12), shorts)
    cylinder_between(f'shorts leg {leg}',(x,0,.78),(x,0,1.00),.15,.162,shorts)
    sphere(f'knee {leg}',(x,-.013,.55),(.114,.122,.13),skin)
    cylinder_between(f'calf {leg}',(x,-.016,.26),(x,-.008,.64),.106,.126,skin)
    sphere(f'ankle and sock {leg}',(x,-.018,.255),(.114,.112,.100),socks)
    for rib in range(3):
        cylinder_between(f'sock rib {leg} {rib}',(x,-.018,.27+rib*.019),(x,-.018,.273+rib*.019),.117,.117,socks)
    # boots face forward: toes toward -Y.
    cube(f'boot foot {leg}',(x,-.095,.116),(.255,.405,.165),boots,.062)
    cube(f'boot shaft {leg}',(x,.012,.208),(.23,.222,.21),boots,.051)
    cube(f'boot outsole {leg}',(x,-.101,.049),(.273,.421,.072),sole,.023)
    for j in range(4):
        z=.200-j*.032
        line(f'boot laces {leg} {j}',[(x-.065,-.123,z),(x,-.139,z-.007),(x+.065,-.123,z)],laces,.004)
    # short pockets and two front seam highlights
    line(f'shorts side seam {leg}',[(side*.28,-.05,1.00),(side*.291,-.055,.85),(side*.278,-.01,.79)],shorts_seam,.0035)
    shoulder=(side*.32,0,1.43)
    sleeve_end=(side*.43,-.007,1.27)
    cylinder_between(f'flower shirt sleeve {leg}', sleeve_end, shoulder, .154,.174,floral)
    cylinder_between(f'bare forearm {leg}', (side*.43,-.013,1.24), (side*.47,-.025,1.01), .098,.11,skin)
    sphere(f'elbow {leg}',(side*.43,-.015,1.24),(.108,.105,.114),skin)
    sphere(f'palm {leg}',(side*.469,-.048,.945),(.089,.078,.115),skin)
    for f in range(4):
        xp=side*.47+(f-1.5)*.032
        sphere(f'finger {leg} {f}',(xp,-.102,.879),(.015,.021,.049),skin,12,8)
    sphere(f'thumb {leg}',(side*.399,-.105,.975),(.029,.032,.065),skin,12,8)

# Torso seams, trim and buttons
line('shirt front placket',[(0,-.188,1.50),(0,-.248,1.28),(0,-.238,1.04)],shirt_dark,.008)
for z in [1.43,1.33,1.23,1.13,1.03]:
    sphere('small shirt button',(0,-.252,z),(.009,.010,.009),metal,12,8)
# Shirt collar 2 halves using rounded pieces.
for s in [-1,1]:
    collar=cube('shirt collar wing', (s*.095,-.17,1.52),(.18,.065,.09),shirt_red,.018)
    collar.rotation_euler[1]=s*math.radians(22)

# 2. A more believable face, moderately heavy jaw, dark cropped curls.
sphere('neck',(0,-.015,1.585),(.142,.147,.147),skin)
sphere('head main',(0,-.045,1.790),(.247,.228,.270),skin,32,24)
sphere('fuller jawline',(0,-.080,1.660),(.210,.197,.133),skin,32,24)
sphere('chin',(0,-.225,1.654),(.129,.038,.059),skin,24,16)
for side in [-1,1]:
    eye='L' if side>0 else 'R'
    sphere(f'ear {eye}',(side*.248,-.042,1.788),(.061,.047,.088),skin,20,12)
    sphere(f'ear inner {eye}',(side*.287,-.061,1.782),(.015,.045,.054),skin_shadow,16,12)
    sphere(f'eyeball {eye}',(side*.101,-.252,1.832),(.067,.030,.044),eye_white,20,12)
    sphere(f'iris {eye}',(side*.098,-.278,1.827),(.026,.010,.027),iris,20,12)
    sphere(f'pupil {eye}',(side*.098,-.288,1.826),(.013,.005,.014),pupil,16,8)
    sphere(f'eye highlight {eye}',(side*.091,-.292,1.836),(.006,.003,.006),eye_white,12,8)
    line(f'heavy eyebrow {eye}',[(side*.043,-.255,1.909),(side*.100,-.267,1.935),(side*.169,-.237,1.916)],brows,.016)
    line(f'lower eyelid {eye}',[(side*.050,-.257,1.803),(side*.101,-.271,1.789),(side*.151,-.241,1.799)],skin_shadow,.005)
    # low-contrast scruffy sideburn/stubble, not a cartoon beard mask
    sphere(f'stubble cheek {eye}',(side*.190,-.176,1.697),(.042,.027,.077),beard,16,12)
# Realistic modest rounded nose, not oversized red clown nose.
sphere('nose bridge',(0,-.276,1.785),(.047,.054,.099),skin,20,12)
sphere('nose tip',(0,-.320,1.733),(.065,.057,.044),skin,24,16)
for side in [-1,1]:
    sphere('nostril shadow',(side*.039,-.363,1.717),(.021,.009,.010),skin_shadow,12,8)
line('moustache left',[(-.095,-.267,1.702),(-.045,-.311,1.702),(-.012,-.328,1.696)],beard,.010)
line('moustache right',[(.012,-.328,1.696),(.045,-.311,1.702),(.095,-.267,1.702)],beard,.010)
line('mouth relaxed',[(-.081,-.279,1.668),(0,-.307,1.663),(.077,-.278,1.669)],lips,.006)
line('chin stubble', [(-.100,-.218,1.641),(0,-.243,1.632),(.100,-.219,1.641)],beard,.009)

# Hair cap + individual rough dark curls; intentionally loose, not a helmet.
sphere('hair undercap',(0,-.018,2.010),(.246,.219,.106),hair,32,18)
for j in range(5):
    for k in range(6):
        x=-.208+k*.081 + (j%2)*.018
        y=-.186+j*.083
        ell=(x/.246)**2 + (y/.226)**2
        if ell>1.17:
            continue
        z=2.025+.055*(1-ell/1.17)+.019*math.sin((j+2)*3+(k+1)*5)
        curl=sphere(f'hair curl {j:02d}-{k:02d}',(x,y,z),(.070,.063,.050),
                    hair if (j+k)%3 else hair_lights,12,10)
        curl.rotation_euler=(.3,.15,((k+j)%5)*.25)
for side in [-1,1]:
    for k in range(4):
        z=1.94+(k%2)*.046
        y=-.125+k*.064
        sphere('short side curls',(side*.234,y,z),(.040,.052,.045),hair,12,8)

# 3. Brown backpack behind character and fully readable text in Blender.
cube('backpack main',(0,.286,1.276),(.46,.210,.517),bag,.080)
cube('backpack pocket',(0,.415,1.140),(.335,.085,.180),bag_trim,.039)
for side in [-1,1]:
    s=side
    line('backpack shoulder strap',[(s*.198,.254,1.507),(s*.250,-.028,1.530),(s*.233,-.212,1.394),(s*.209,-.225,1.141)],bag_trim,.040)
    cube('bag pocket',(s*.223,.324,1.286),(.079,.094,.220),bag,.019)
line('bag top zipper', [(-.15,.412,1.472),(0,.426,1.475),(.15,.412,1.472)],metal,.006)
line('bag lower zipper', [(-.15,.464,1.185),(0,.467,1.187),(.15,.464,1.185)],metal,.005)

# Blender text lies in local XY plane; orientation rotates to the backpack-facing +Y normal.
bpy.ops.object.text_add(location=(0,.398,1.317),rotation=(math.radians(90),0,math.radians(180)))
label=move(bpy.context.object)
label.name='V2 Backpack logo · ABIBAS'
label.data.body='ABIBAS'
label.data.align_x='CENTER'
label.data.align_y='CENTER'
label.data.size=.088
label.data.extrude=.0009
label.data.bevel_depth=.0003
label.data.materials.append(logo_white)

# Optional rig placement guides, but no false claim of working animation/rig.
for name, pos in [('hip',(0,0,.89)),('spine',(0,0,1.29)),('neck',(0,0,1.58)),
                  ('head',(0,0,1.82)),('hand.L',(.47,-.03,.96)),('hand.R',(-.47,-.03,.96)),
                  ('foot.L',(.16,-.09,.08)),('foot.R',(-.16,-.09,.08))]:
    e=bpy.data.objects.new('V2 Joint suggestion '+name,None)
    character.objects.link(e)
    e.empty_display_type='SPHERE'
    e.empty_display_size=.027
    e.location=pos
    e.hide_set(True)
    e.hide_render=True

# Select a single new object and focus the 3D viewport when present.
bpy.ops.object.select_all(action='DESELECT')
main=bpy.data.objects.get('V2 Hawaiian shirt · shaped')
if main:
    main.select_set(True)
    bpy.context.view_layer.objects.active=main

scene=bpy.context.scene
scene.unit_settings.system='METRIC'
scene.unit_settings.scale_length=1.0
scene.render.resolution_x=1400
scene.render.resolution_y=1400
scene.render.resolution_percentage=100
if scene.camera:
    scene.camera.data.type='ORTHO'
    scene.camera.data.ortho_scale=2.6

for screen in bpy.data.screens:
    for area in screen.areas:
        if area.type == 'VIEW_3D':
            area.spaces.active.region_3d.view_distance = 3.7

output=ROOT / 'alkosmen_3d_v2.blend'
# Save to a NEW name: prior .blend and hidden starter geometry stay intact.
bpy.ops.wm.save_as_mainfile(filepath=str(output), check_existing=False)
print('ALKO V2 created:', output)
print('Not rigged or animated. Sculpt and fit to images before exporting .glb for libGDX.')
