# YFFK — YouTube For Far Kid

Ứng dụng YFFK chạy trên Android TV, dùng để phát danh sách video từ một YouTube Playlist và điều khiển phát bằng MQTT (từ xa).

## Tính năng

- Lấy danh sách video từ **tối đa 3 YouTube Playlist** qua YouTube Data API v3, mỗi playlist hiển thị thành một hàng (row) trong giao diện YouTube TV (Leanback).
- Phát video bằng YouTube IFrame Player (thư viện `android-youtube-player`).
- Tự động phát video tiếp theo; khi hết video cuối sẽ quay lại video đầu (vòng lặp playlist).
- Giao diện:
  - Góc trái: tên playlist và **thời gian đã mở app** (tính bằng phút, đếm từ lúc mở ứng dụng, reset khi tắt app).
  - Góc phải: logo YouTube, đồng hồ thời gian thực và nút cài đặt (bánh răng).
- **Hẹn giờ xem**: giới hạn số phút được xem; khi vượt quá sẽ không cho phát video nào và hiển thị thông báo. Có nút reset bộ đếm.
- Trang Cài đặt MQTT:
  - Cấu hình kết nối đến broker (broker, client id, username, password, topic).
  - 3 nút riêng biệt: **Lưu** (chỉ lưu cấu hình), **Kết nối** (lưu + nối broker), **Ngắt kết nối**.
  - Hiển thị **IP local** của TV.
  - Tự động kết nối lại khi mở app (nếu trước đó đã kết nối), có cơ chế tự động reconnect và retry khi rớt kết nối.
- Điều khiển video từ xa qua MQTT: chỉ chấp nhận `play` (tiếp tục), `stop` (tạm dừng), `next` (video kế tiếp); mọi nội dung khác bị bỏ qua.

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
YOUTUBE_PLAYLIST_ID=PLAYLIST_ID_1
YOUTUBE_PLAYLIST_ID_2=PLAYLIST_ID_2
YOUTUBE_PLAYLIST_ID_3=PLAYLIST_ID_3
```

- `YOUTUBE_API_KEY`: khóa YouTube Data API v3 (tạo tại Google Cloud Console).
- `YOUTUBE_PLAYLIST_ID`: ID playlist thứ 1 (bắt buộc).
- `YOUTUBE_PLAYLIST_ID_2`, `YOUTUBE_PLAYLIST_ID_3`: ID playlist thứ 2, 3 (tùy chọn — để trống sẽ không hiển thị hàng tương ứng).

App hỗ trợ tối đa **3 playlist**, mỗi playlist hiển thị thành một hàng (row) riêng biệt trong giao diện, tiêu đề hàng là tên playlist. Chuyển tiếp / vòng lặp video chỉ xảy ra trong nội bộ từng playlist.

Các giá trị này được đưa vào `BuildConfig.YOUTUBE_PLAYLIST_ID`, `YOUTUBE_PLAYLIST_ID_2`, `YOUTUBE_PLAYLIST_ID_3` khi build.

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
- **IP local**: hiển thị địa chỉ IP của TV (dùng để `adb connect` hoặc cấu hình mạng).
- Ba nút:
  - **Lưu**: chỉ lưu cấu hình broker/topic/... vào thiết bị, không kết nối.
  - **Kết nối**: lưu cấu hình + kết nối broker (và bật tự động kết nối khi mở app lần sau).
  - **Ngắt kết nối**: ngắt kết nối + tắt tự động kết nối.
- Trạng thái kết nối được đồng bộ thực tế mỗi khi mở trang (Đã kết nối / Đang kết nối... / Chưa kết nối).

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

Tin nhắn có thể là JSON như trên, hoặc chỉ là chuỗi thuần (`play` / `stop` / `next`) — app đều hiểu. **Chỉ 3 lệnh trên được chấp nhận**, mọi nội dung khác bị bỏ qua hoàn toàn.

### Hẹn giờ xem

Trong trang Cài đặt, mục hẹn giờ:

- **Số phút tối đa được xem**: nhập số nguyên > 0 (mặc định `60`).
- Nút **Lưu thông số**: lưu giới hạn phút (lưu vào thiết bị).
- Nút **Reset bộ đếm**: đưa bộ đếm thời gian mở app về 0 → xem bình thường trở lại.

Quy tắc:

- Thời gian mở app đếm từ lúc khởi động ứng dụng, hiển thị ở góc trái (cạnh tên playlist) theo số phút.
- Khi `thời gian mở app > số phút tối đa`:
  - Không cho phát bất kỳ video nào (kể cả chuyển video tiếp theo hay lệnh MQTT `play`).
  - Hiện thông báo **"Bạn đã xem quá số phút cho phép"**.
  - Nếu đang phát, video sẽ tự động tạm dừng.
- Sau khi bấm **Reset bộ đếm**, chức năng xem hoạt động bình thường lại.
- Khi tắt app rồi mở lại, bộ đếm tự động về 0.

## Cấu trúc mã nguồn

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt              # Màn hình chính (Leanback), top bar, uptime, auto-connect MQTT
├── PlayerActivity.kt            # Phát video, tự động next, chặn khi quá hạn, nhận lệnh MQTT
├── SettingsActivity.kt          # Trang cài đặt MQTT + hẹn giờ xem
├── SessionTimer.kt              # Bộ đếm thời gian mở app + giới hạn phút (singleton)
├── model/                       # Model YouTube API + MQTT command
├── network/YouTubeApiService.kt # Retrofit API (playlistItems, playlists)
├── ui/VideoCardPresenter.kt     # Card video trên Leanback
└── mqtt/
    ├── MqttController.kt        # Singleton kết nối MQTT + parse/validate tin nhắn
    └── PlaybackCommandBus.kt    # Event bus chuyển lệnh tới PlayerActivity
```
