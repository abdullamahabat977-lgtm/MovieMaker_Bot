# 3D Workspace and Android Lifecycle

## Current architecture

- `MainActivity` exposes the 3D preview from both home states (linked and not linked), so the local preview can open without an account or network connection.
- Local project creation/listing and the built-in starter asset catalogue are also available before account linking; only account, balance and permission services need the network.
- `Scene3DActivity` remains the standalone procedural preview page.
- `SceneEditorActivity` opens a saved project and owns its `Scene3DView`, object controls and local timeline panels.
- Both GL Activities forward `onResume`/`onPause` to the `GLSurfaceView`; the scene editor guards repeated pause/resume calls.
- The 3D workspace is a full Activity page, not an AlertDialog.
- The Activity provides its own toolbar/back button; Android system Back finishes the Activity normally.
- `Scene3DView` uses OpenGL ES 2.0 and requests an RGB565/depth16 EGL framebuffer to improve compatibility with lower-end mobile GPUs.
- The default scene now renders a static low-poly humanoid built from reusable cube and octahedron meshes; the toolbar controls can switch to the diagnostic cube.
- The mannequin is a first procedural 3D mesh, not yet a rigged character: GLB import, skeletal animation, project persistence and editable transforms remain future work.
- The standalone preview provides four skin-tone choices and six shirt-color choices.
- The project editor persists object type, label, position, scale, skin palette and shirt palette in the local project JSON.
- The project editor stores basic `idle`, `walk`, `run` and `talk` timeline clips as frame ranges. This is clip metadata only; skeletal animation playback is not implemented yet.
- The editor preview displays the selected procedural character or cube with its saved transform. It does not yet render every scene object simultaneously.
- The starter catalogue contains metadata and 2D illustrated previews; those thumbnails are not 3D model files.
- OpenGL ES 2.0 is declared as an optional device feature. The Activity checks runtime support before creating the renderer.
- Projects, user-created assets and movie exports remain local to the phone.

## Lifecycle rules

1. Create the GL view inside the owning Activity's `onCreate`.
2. Call `super.onResume()`, then forward `onResume()` to the GL view once.
3. Forward `onPause()` to the GL view once, then call `super.onPause()`.
4. Keep the GL surface in an Activity layout, never in a short-lived AlertDialog.
5. Test opening the project editor, system Back, toolbar Back, background/foreground, and repeated open/close cycles.

## Device test checklist

- Launch the app and open the 3D workspace.
- Confirm the page fills the available screen and shows the low-poly character/grid.
- Switch between the character and diagnostic cube controls; both should render without recreating the Activity.
- Change each skin-tone and shirt-color swatch; verify the character updates and the cube remains unchanged.
- Drag with one finger and pinch with two fingers.
- Use the toolbar Back button and Android system Back; the app should return to the home screen without closing.
- Reopen the 3D page several times.
- Switch to another app and return; rendering should resume.
- If it still closes, capture Android crash/logcat output for the `MovieStudio3D` tag and the fatal exception stack trace. CI compilation alone does not prove runtime stability.

## Reference projects reviewed

- Android Open Source Project GLSurfaceView activity sample: demonstrates Activity `onResume`/`onPause` forwarding to the GL view. Apache-2.0 notice applies to AOSP source files; inspect the exact file header before copying source.
- Google Filament Android samples: useful future reference for model rendering, textures, lighting and glTF loading. Review the repository's current license and third-party asset licenses before reuse.
- SceneView Android SDK and samples: uses Filament on Android and demonstrates glTF/GLB models, camera controls, animation and lifecycle-managed scene resources. The SceneView repository states Apache-2.0 for its project code, while bundled sample models and third-party assets may have separate licenses.

No renderer migration is approved by this document. Keep the lightweight OpenGL ES prototype until a candidate engine is evaluated against minSdk 23, Gradle/Kotlin compatibility, APK size, memory use and the target phone's GPU, then built and tested on-device.
