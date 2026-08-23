# YFFK — YouTube For Far Kid

Ứng dụng YFFK chạy trên Android TV, dùng để phát danh sách video từ một YouTube Playlist và điều khiển phát bằng MQTT (từ xa).

## Tính năng

- Lấy danh sách video từ YouTube Playlist qua YouTube Data API v3 và hiển thị giao diện dạng YouTube TV (Leanback).
- Phát video bằng YouTube IFrame Player (thư viện `android-youtube-player`).
- Tự động phát video tiếp theo; khi hết video cuối sẽ quay lại video đầu (vòng lặp playlist).
- Giao diện:
  - Góc trái: tên playlist.
  - Góc phải: logo YouTube, đồng hồ thời gian thực và nút cài đặt (bánh răng).
- Trang Cài đặt MQTT:
  - Cấu hình kết nối đến broker (broker, client id, username, password, topic).
  - Kết nối / ngắt kết nối, lưu cấu hình.
  - Tự động kết nối lại khi mở app (nếu trước đó đã kết nối).
- Điều khiển video từ xa qua MQTT: `play` (tiếp tục), `stop` (tạm dừng), `next` (video kế tiếp).

## Công nghệ

| Thành phần | Thư viện |
|---|---|
| Giao diện TV | AndroidX Leanback 1.0.0 |
| Phát video | `com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0` |
| Gọi API | Retrofit 2.9.0 + Gson converter |
| Ảnh thumbnail | Glide 4.16.0 |
| MQTT | `org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5` |
| Min / Target / Compile SDK | 33 / 35 / 35 |

## Yêu cầu cấu hình

Trước khi build, tạo file `local.properties` (đã nằm trong `.gitignore`, không bị commit) tại thư mục gốc dự án:

```properties
YOUTUBE_API_KEY=YOUR_YOUTUBE_DATA_API_V3_KEY
YOUTUBE_PLAYLIST_ID=YOUR_PLAYLIST_ID
```

- `YOUTUBE_API_KEY`: khóa YouTube Data API v3 (tạo tại Google Cloud Console).
- `YOUTUBE_PLAYLIST_ID`: ID của playlist cần phát (ví dụ: `PLYzKnm87_04Y`).

Hai giá trị này được đưa vào `BuildConfig.YOUTUBE_API_KEY` và `BuildConfig.YOUTUBE_PLAYLIST_ID` khi build.

## Build & chạy

```bash
./gradlew assembleDebug      # build APK debug
./gradlew installDebug       # cài lên thiết bị/emulator đang kết nối
```

### Chạy trên emulator

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Nếu gặp lỗi `cmd: Can't find service: package` khi cài, package manager của emulator bị treo — hãy `adb reboot` rồi cài lại.

### Chạy trên TV Xiaomi thật

Kết nối cùng mạng Wi-Fi, bật ADB qua mạng trên TV, sau đó:

```bash
adb connect IP_CUA_TV:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Hướng dẫn sử dụng

1. Mở app trên TV/emulator → danh sách video của playlist hiển thị.
2. Chọn video bằng D-pad / chuột → bấm chọn để phát.
3. Khi một video kết thúc, video tiếp theo tự động phát; hết video cuối sẽ quay lại video đầu.
4. Nút bánh răng góc phải mở **Cài đặt MQTT**.

### Cài đặt MQTT

Trong trang Cài đặt:

- **Broker**: ví dụ `tcp://broker.hivemq.com:1883` (hoặc `mqtts://...` nếu broker dùng TLS).
- **Client ID**: để trống sẽ tự sinh.
- **Username / Password**: điền nếu broker yêu cầu xác thực.
- **Topic**: topic sẽ subscribe (mặc định `yffk/youtube-tv/command`).
- Bấm **Kết nối**. Trạng thái hiển thị bên dưới nút.
- Cấu hình được lưu lại; lần sau mở app sẽ tự động kết nối nếu trước đó đang kết nối.

### Định dạng tin nhắn điều khiển

Publish lên topic đã cấu hình (mặc định `yffk/youtube-tv/command`) một trong các JSON sau:

```json
{ "action": "play" }
```
> Tiếp tục phát video đang tạm dừng.

```json
{ "action": "stop" }
```
> Tạm dừng video đang phát.

```json
{ "action": "next" }
```
> Chuyển sang video kế tiếp trong playlist.

Tin nhắn có thể là JSON như trên, hoặc chỉ là chuỗi thuần (`play` / `stop` / `next`) — app đều hiểu.

## Cấu trúc mã nguồn

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt              # Màn hình chính (Leanback), top bar, auto-connect MQTT
├── PlayerActivity.kt            # Phát video, tự động next, nhận lệnh MQTT
├── SettingsActivity.kt          # Trang cài đặt MQTT
├── model/                       # Model YouTube API + MQTT command
├── network/YouTubeApiService.kt # Retrofit API (playlistItems, playlists)
├── ui/VideoCardPresenter.kt     # Card video trên Leanback
└── mqtt/
    ├── MqttController.kt        # Singleton kết nối MQTT + parse tin nhắn
    └── PlaybackCommandBus.kt    # Event bus chuyển lệnh tới PlayerActivity
```
