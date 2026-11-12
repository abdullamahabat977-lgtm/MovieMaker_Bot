# 3D Workspace and Android Lifecycle

## Current architecture

- The 3D preview is launched from `MainActivity` with an explicit Intent.
- `Scene3DActivity` owns the 3D workspace and its `Scene3DView`.
- The 3D workspace is a full Activity page, not an AlertDialog.
- The Activity provides its own toolbar/back button; Android system Back finishes the Activity normally.
- `Scene3DView` uses OpenGL ES 2.0 and requests an RGB565/depth16 EGL framebuffer to improve compatibility with lower-end mobile GPUs.
- The default scene now renders a static low-poly humanoid built from reusable cube and octahedron meshes; the toolbar controls can switch to the diagnostic cube.
- The mannequin is a first procedural 3D mesh, not yet a rigged character: GLB import, skeletal animation, project persistence and editable transforms remain future work.
- The 80-item starter catalogue currently contains metadata and 2D illustrated previews; those thumbnails are not 3D model files.
- OpenGL ES 2.0 is declared as an optional device feature. The Activity checks runtime support before creating the renderer.
- Projects, user-created assets and movie exports remain local to the phone.

## Lifecycle rules

1. Create the GL view in `Scene3DActivity.onCreate`.
2. Call `super.onResume()`, then `sceneView.onResume()`.
3. Call `super.onPause()`, then `sceneView.onPause()`.
4. Release the Activity's view reference in `onDestroy`.
5. Do not host the GL surface inside a short-lived AlertDialog.
6. Test opening the 3D page, system Back, toolbar Back, background/foreground, and repeated open/close cycles.

## Device test checklist

- Launch the app and open the 3D workspace.
- Confirm the page fills the available screen and shows the low-poly character/grid.
- Switch between the character and diagnostic cube controls; both should render without recreating the Activity.
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
