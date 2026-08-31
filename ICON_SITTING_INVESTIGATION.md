# Research Plan: Reliable Icon Sitting and Drag Recovery

> Core question: Why does long-hold icon seating fail, and how can a seated Shimeji remain immediately touchable and draggable without changing existing movement?
> min_rounds: 10

## Dimensions
1. Accessibility window selection — the overlay may hide the launcher root used for icon hit-testing.
2. Launcher node representation — icon nodes differ by launcher, class, label, clickability, and bounds.
3. Coordinate and gesture lifecycle — raw touch coordinates, long-hold scheduling, and move cancellation must agree.
4. Overlay input behavior — the seated overlay must continue receiving touch and drag events.
5. State and sensor lifecycle — sitting, dragging, shaking, falling, and cleanup must transition safely.
6. Compatibility and privacy — retrieval must remain narrow, local, disclosed, and degrade safely.

## Completion criteria
- [x] Root cause supported by code/API evidence from multiple angles.
- [x] Icon hit-testing avoids selecting the app's own accessibility overlay.
- [x] Long-hold seating handles common labeled/clickable launcher node structures.
- [x] A seated Shimeji remains touchable and draggable.
- [x] Shake release still enters the existing fall/bounce flow.
- [x] Unit tests and Android debug build pass.
- [x] Verifier PASS after the minimum exploration rounds.

## Scope
- In: accessibility overlay, launcher icon hit-testing, touch/hold handling, sitting state, accelerometer lifecycle, tests.
- Out: redesigning existing traversal, launching icons, reading unrelated screen content, UI redesign.

## Final findings and implementation

- Android defines the active accessibility window partly by current touch. During a touch on the Shimeji overlay, `rootInActiveWindow` is therefore not a reliable way to reach the launcher beneath it.
- The service now requests interactive windows, searches application roots from the default launcher, excludes this app's overlay package, and uses the active root only as a safe fallback.
- Hit-testing probes the Shimeji body/feet and finger, so icon detection no longer depends on where the sprite was grabbed.
- The former seated touch path left `motion=SITTING`, blocked `dragTo`, skipped `endDrag`, and disabled shaking. It now distinguishes a seated tap from a real drag and calls an explicit `beginDragFromSeat()` transition after movement crosses the drag threshold.
- A stationary tap keeps the character seated and restores the accelerometer. A real drag preserves the touch offset and returns to the existing edge-attach or fall/bounce behavior on release.
- Accessibility inspection occurs only after the deliberate 650 ms hold and no node content is retained or transmitted.

## Verification record

| Claim | Method | Result |
|---|---|---|
| Existing movement preserved | Existing traversal/fall tests | Passed |
| Sitting stays pinned until released | JVM unit test | Passed |
| Seated pickup does not jump or fall | New JVM unit test | Passed |
| Mid-screen release falls | New JVM unit test | Passed |
| Bottom-edge release walks | New JVM unit test | Passed |
| Android sources compile and package | `testDebugUnitTest assembleDebug` | BUILD SUCCESSFUL (52 tasks) |
| Investigation quality | Independent verifier after 11 rounds | PASS |

## Residual device validation

Launcher accessibility trees vary by device. The compiled implementation covers common labeled/clickable icon structures, but final visual placement should still be exercised on the target phone and launcher. The dormant `restore(SITTING)` physics behavior intentionally falls because no launcher anchor is persisted; the active accessibility overlay does not currently use that restore path.
