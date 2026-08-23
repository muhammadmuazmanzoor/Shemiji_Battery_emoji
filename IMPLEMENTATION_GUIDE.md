# Shimeji Battery API and Demo Data Implementation

The project now supports the same UI and ViewModel flows from either bundled drawable data or a Retrofit API.

## One switch for every feature

Open `app/build.gradle.kts` and change:

```kotlin
buildConfigField("boolean", "USE_REMOTE_DATA", "false")
```

- `false`: battery emojis, toolbar styles, wallpapers, and Shimeji characters come from the demo drawables.
- `true`: every feature is loaded through Retrofit.

When the backend is ready, replace:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://example.com/\"")
```

The base URL must end with `/`.

## Placeholder endpoints

The endpoint paths are in:

`app/src/main/java/com/shemiji/emogibattery/data/remote/ContentApiService.kt`

- `GET v1/battery-emojis`
- `GET v1/toolbar-styles`
- `GET v1/wallpapers`
- `GET v1/shimeji-characters`

If the backend uses different paths or JSON field names, only update `ContentApiService.kt` and its DTO mapping. The screens, ViewModels, and repositories do not need to change.

## Expected API envelope

```json
{
  "success": true,
  "message": null,
  "data": []
}
```

Example battery emoji item:

```json
{
  "id": "battery_happy",
  "name": "Happy",
  "image_url": "https://cdn.example.com/battery/happy.png",
  "is_premium": false
}
```

Example toolbar style item:

```json
{
  "id": "toolbar_midnight",
  "name": "Midnight",
  "background_color": "#16182B",
  "content_color": "#FFFFFF",
  "accent_color": "#8B8FFF",
  "is_premium": false
}
```

Example wallpaper item:

```json
{
  "id": "wallpaper_space",
  "name": "Dreamy Space",
  "category": "Space",
  "image_url": "https://cdn.example.com/wallpapers/space.jpg",
  "is_premium": false
}
```

Example Shimeji item:

```json
{
  "id": "shimeji_mochi",
  "name": "Mochi",
  "image_url": "https://cdn.example.com/shimeji/mochi.png",
  "animation_url": "https://cdn.example.com/shimeji/mochi.gif",
  "movement_speed": 1.0,
  "is_premium": false
}
```

## Implemented architecture

- Hilt application setup and injected repositories/controllers
- Retrofit, Gson, OkHttp timeouts, and debug request logging
- Local and remote data sources sharing the same domain models
- Repository-level exception and cancellation handling
- Separate ViewModels and immutable UI states for each feature
- Loading, refresh, empty/error, selection, applying, enabled, and message states
- DataStore persistence for selected battery, toolbar, wallpaper, and Shimeji
- Actual system wallpaper application for local and remote images
- Floating battery toolbar overlay with live battery percentage
- Draggable, automatically moving Shimeji overlay
- Overlay permission flow and foreground services
- Light, dark, and system theme selection

## Important integration notes

The battery toolbar and Shimeji features require the user to grant **Display over other apps** permission. Android 13 and newer may also ask for notification permission because both overlays are maintained by foreground services.

Remote Shimeji `animation_url` can point to a GIF supported by Coil. If the backend later provides sprite sheets or ZIP animation packages, add their decoder in the Shimeji data/service layer while keeping the existing ViewModel and UI contracts.
