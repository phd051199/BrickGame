# BrickGame J2ME

Bộ Brick Game dành cho Nokia CLDC 1.1 / MIDP 2.0, tối ưu cho hai độ phân giải:

- `320×240` landscape
- `240×320` portrait

Board luôn giữ lưới LCD **10×20**, preview **4×4** và cách vẽ ô 10×10 pixel của source Brick Game gốc.

## Chương trình A–N

| Mã | Game | Luật chính |
|---|---|---|
| A-01 | Tanks | Di chuyển xe tăng, bắn xe tăng địch |
| B-02 | Breakout | Một paddle phá toàn bộ brick |
| C-03 | Double | Breakout với paddle trên và dưới |
| D-04 | Wall Ball | Đưa bóng vượt blocker lên đỉnh |
| E-05 | Race | Đua xe hai làn |
| F-06 | Highway | Đua xe ba làn, tìm làn trống |
| G-07 | Tunnel | Lái xe trong đường hầm chuyển động |
| H-08 | Shoot | Bắn đội hình block rơi xuống |
| I-09 | Stack | Bắn block để lấp và xoá hàng |
| J-10 | Invaders | Đội hình block vừa hạ xuống vừa bắn trả |
| K-11 | Snake | Snake với 16 map obstacle gốc |
| L-12 | Frogger | Băng qua tám làn và lấp đủ mười goal |
| M-13 | Match | Ghép đúng ba mã block trước khi chạm đáy |
| N-14 | Tetris | Tetris bảy tetromino |

Menu hiển thị ký tự A–N trên board, icon 4×4 trong preview và demo động của mode đang chọn.

## Giao diện

### 320×240

- Board 110×220 nằm bên trái.
- Panel 180 pixel bên phải chứa mã chương trình, tên game, preview, score, speed, level, life, sound, pause và hướng dẫn phím.
- Không scale asset, không dùng font hệ thống.

### 240×320

- Board và panel trạng thái nằm ở nửa trên.
- Hướng dẫn điều khiển được đưa xuống footer để không ép nhỏ board.

Toàn bộ text giao diện được vẽ bằng `BitmapFont`, trích từ face `Regular7` của project `NarutoBattleSymbian`. Glyph được lưu dạng row bitmask 11 pixel nên kết quả không phụ thuộc font trên từng máy Nokia.

Digit score/speed/level, icon sound và pause vẫn dùng bitmap resource gốc.

## Điều khiển

### Menu

- `4` / trái: game trước.
- `6` / phải: game sau.
- `2` / lên: tăng speed `0–15`.
- `8` / xuống: tăng level `0–15`.
- `5` / Fire: bắt đầu.
- `*`: đổi trạng thái sound.

### Trong game

- `2`, `4`, `6`, `8`: di chuyển hoặc thao tác theo game.
- `5`: fire, rotate hoặc advance.
- `0`: pause/resume.
- `#`: về menu.
- `*`: đổi trạng thái sound.

Canvas không đăng ký soft-key command để Nokia không chừa command bar làm lệch layout fullscreen.

## Build

```sh
cd /Users/duypham/Developer/BrickGame
sh build-j2me.sh
```

Output:

```text
dist/BrickGame.jar
dist/BrickGame.jad
```

Build pipeline:

1. `javac --release 8` compile source.
2. ProGuard chuyển bytecode xuống Java 1.1.
3. Tạo CLDC `StackMap`.
4. Đóng gói resource và manifest.
5. Kiểm tra JAR không còn class literal gây `VerifyError` trên KVM/MicroEmulator cũ.

Có thể thay đường dẫn SDK bằng `CLDC_JAR`, `MIDP_JAR` và `PROGUARD_JAR`.

## Test

```sh
CLDC=/Users/duypham/Developer/MIDPlay/lib/cldc_1.1.jar
MIDP=/Users/duypham/Developer/MIDPlay/lib/midp_2.0.jar

mkdir -p build/test
javac --release 8 -encoding UTF-8 \
  -classpath "$CLDC:$MIDP:build/j2me/classes" \
  -d build/test test/brickgame/EngineSmokeTest.java

java -cp "build/j2me/classes:build/test:resources" \
  brickgame.EngineSmokeTest
```

Smoke test hiện kiểm tra:

- Menu A–N và wrap hai chiều.
- Board/preview menu của đủ 14 mode.
- Khởi tạo và tick đủ 14 game.
- Speed/level wrap `15 → 0`.
- Class version 45 và CLDC StackMap.
- Không tham chiếu AWT, Swing, Stream API, reflection hoặc concurrent API.
- Khởi động bằng MicroEmulator ở `320×240` với strict app classloader.

## Nguồn và giấy phép

Phần lõi Snake, Race, Tetris, Shoot, Tanks và LCD asset bắt nguồn từ `Brick-Game-9999-in-1` của Vitaliy Boyarsky.

Các chương trình Breakout, Double, Wall Ball, Highway, Tunnel, Stack, Invaders, Frogger và Match được chuyển sang CLDC từ luật chơi và dữ liệu level của `Simple-Brick-Games` của Tobias Bielefeld, phát hành theo GPL-3.0-or-later.

Xem `THIRD_PARTY_NOTICES.md` và `LICENSE` trước khi phân phối bản build.
