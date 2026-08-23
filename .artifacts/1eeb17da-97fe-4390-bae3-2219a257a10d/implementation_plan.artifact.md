# Fix Shimeji Animation Disappearing on App Kill

Ensure the Shimeji and Battery Toolbar overlays persist when the main application is swiped away from the Recent Apps list.

## User Review Required

> [!IMPORTANT]
> This change moves the background services to a separate process (`:overlay`). This is the industry standard for "always-on" overlay apps, but it means the app will now consume memory in two separate processes. Both services are lightweight, so this impact is minimal.

## Proposed Changes

### [Component Name] Overlay Services Persistence

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/DELL/AndroidStudioProjects/Shemiji_Battery_animes/app/src/main/AndroidManifest.xml)
- Add `android:process=":overlay"` to `ShimejiOverlayService` and `BatteryToolbarOverlayService` declarations.

#### [MODIFY] [ShimejiOverlayService.kt](file:///C:/Users/DELL/AndroidStudioProjects/Shemiji_Battery_animes/app/src/main/java/com/shemiji/emogibattery/service/ShimejiOverlayService.kt)
- Override `onTaskRemoved` to ensure the service remains sticky.
- Clean up placeholder logs in `onDestroy`.

#### [MODIFY] [BatteryToolbarOverlayService.kt](file:///C:/Users/DELL/AndroidStudioProjects/Shemiji_Battery_animes/app/src/main/java/com/shemiji/emogibattery/service/BatteryToolbarOverlayService.kt)
- Override `onTaskRemoved` to ensure the service remains sticky.

## Verification Plan

### Manual Verification
1. Open the app and enable a Shimeji character.
2. Observe the character appearing on the screen.
3. Swipe the app away from the Recent Apps list.
4. **Expected Result**: The Shimeji character should stay on the screen and continue its movement.
5. Repeat the same steps for the Battery Toolbar.
