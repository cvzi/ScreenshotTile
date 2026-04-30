Place real long-screenshot frames in a folder and run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.github.cvzi.screenshottile.utils.LongScreenshotReplayTest "-DlongScreenshot.inputDir=C:\path\to\frames"
```

The test writes `stitched.png`, `stitched_debug.png`, per-frame debug PNGs, and `summary.txt`.

Default output folder:

```text
app/build/reports/long-screenshot-replay/<input-folder-name>
```

Optional `stitch.properties` file in the frame folder:

```properties
repeatedBottomInsetPx=0
contentBounds=0,120,1080,2280
expectedCropTops=0,1320,1316
cropTopsOverride=0,1320,1316
assertCropTops=0,1320,1316
assertCropTolerancePx=6
assertRequiresReview=false
```

Notes:

- Frames are loaded from `*.png` files sorted by filename.
- `expectedCropTops` is passed into the stitcher as a hint.
- `assertCropTops` is optional and turns the replay into a stricter regression test once you know the right seams.
