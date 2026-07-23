# Báo cáo lỗi tạo và quản lý quiz

## 1. Các lỗi đã phát hiện

1. **Một tài liệu chỉ tạo được một bộ quiz**

   Bảng `quiz_sets` đặt ràng buộc `unique (user_id, document_id)`. Vì vậy, cùng một người dùng không thể tạo nhiều bộ quiz từ cùng một tài liệu.

2. **Câu hỏi chỉ liên kết với tài liệu, không liên kết với bộ quiz**

   Bảng `quizzes` trước đây chỉ có `document_id`. Khi một tài liệu sinh quiz nhiều lần, ứng dụng không thể xác định câu hỏi thuộc lần tạo nào.

3. **Các câu hỏi của nhiều quiz bị trộn lẫn**

   Repository tải câu hỏi theo `document_id`, nên toàn bộ câu hỏi được tạo từ cùng một tài liệu bị trả về chung trong một danh sách.

4. **Không có lựa chọn mức độ khó khi tạo quiz**

   Màn hình quiz tự động sinh câu hỏi mà không cho người dùng chọn `Easy`, `Medium` hoặc `Hard`.

5. **Độ khó luôn bị lưu cố định là `MEDIUM`**

   Khi lưu câu hỏi, repository ghi cứng giá trị `MEDIUM`, không phản ánh độ khó người dùng đã chọn và không truyền yêu cầu độ khó cho AI.

6. **Xóa một quiz có thể xóa toàn bộ quiz của tài liệu**

   Hàm `delete_quiz_set` cũ xóa câu hỏi theo `document_id`. Nếu một tài liệu có nhiều bộ quiz, thao tác xóa một bộ sẽ ảnh hưởng các bộ còn lại.

7. **Lỗi `column quizzes.quiz_set_id does not exist`**

   Phiên bản ứng dụng mới truy vấn `quizzes.quiz_set_id` và `quiz_sets.difficulty`, nhưng database Supabase chưa được áp dụng migration tương ứng. PostgREST vì vậy trả về lỗi thiếu cột.

8. **Tên mức độ khó hiển thị không đồng nhất**

   Giao diện ban đầu sử dụng tên tiếng Việt trong khi dữ liệu và prompt AI sử dụng giá trị tiếng Anh. Điều này làm trải nghiệm hiển thị không thống nhất.

## 2. Phương pháp và vị trí sửa

| Phương pháp sửa | Vị trí |
|---|---|
| Bỏ ràng buộc một tài liệu chỉ có một `quiz_set`; thêm cột `difficulty` cho bộ quiz | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 16 |
| Thêm cột `quiz_set_id` vào bảng `quizzes` | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 18 |
| Tạo quiz set cho dữ liệu cũ và backfill `quiz_set_id` mà không làm mất câu hỏi | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 39 |
| Bắt buộc mọi câu hỏi phải thuộc một quiz set | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 77 |
| Thêm khóa ngoại theo cả `quiz_set_id` và `user_id` để bảo đảm đúng chủ sở hữu | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 82 |
| Tạo index cho truy vấn câu hỏi theo quiz set | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 114 |
| Sửa hàm xóa để chỉ xóa quiz set được chọn; câu hỏi được xóa theo cascade | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 148 |
| Reload PostgREST schema cache sau khi thêm cột | `supabase/migrations/202607230005_repair_quiz_schema.sql` — dòng 169 |
| Tải thư viện quiz theo từng `quiz_set` thay vì nhóm theo tài liệu | `app/src/main/java/com/example/aistudyassistant/repositories/AIContentRepository.java` — dòng 115 |
| Tải câu hỏi bằng `quiz_set_id` để không trộn nhiều quiz của một tài liệu | `app/src/main/java/com/example/aistudyassistant/repositories/AIContentRepository.java` — dòng 356 |
| Mỗi lần tạo quiz sẽ tạo một quiz set mới và lưu câu hỏi vào đúng set | `app/src/main/java/com/example/aistudyassistant/repositories/AIContentRepository.java` — dòng 385 |
| Chuẩn hóa độ khó thành `EASY`, `MEDIUM` hoặc `HARD` trước khi lưu | `app/src/main/java/com/example/aistudyassistant/repositories/AIContentRepository.java` — dòng 708 |
| Thêm hộp thoại lựa chọn `Easy`, `Medium`, `Hard`; mặc định là `Medium` | `app/src/main/java/com/example/aistudyassistant/activities/QuizActivity.java` — dòng 153 |
| Truyền độ khó đã chọn vào dịch vụ sinh quiz | `app/src/main/java/com/example/aistudyassistant/activities/QuizActivity.java` — dòng 171 |
| Điều chỉnh prompt AI theo từng mức độ khó | `app/src/main/java/com/example/aistudyassistant/api/AIClient.java` — dòng 58 |
| Truyền `quiz_set_id` và độ khó giữa màn hình quiz, kết quả và chức năng làm lại | `app/src/main/java/com/example/aistudyassistant/activities/QuizResultActivity.java` — dòng 40 |
| Hiển thị độ khó bằng tên tiếng Anh trong thư viện quiz | `app/src/main/java/com/example/aistudyassistant/adapters/StudySetAdapter.java` — dòng 106 |
| Thêm khóa Intent riêng cho quiz set và độ khó | `app/src/main/java/com/example/aistudyassistant/utils/Constants.java` — dòng 53 |
| Thêm UI test kiểm tra ba mức độ khó và mức mặc định | `app/src/androidTest/java/com/example/aistudyassistant/activities/QuizDifficultyUiTest.java` — dòng 46 |

## 3. Kết quả kiểm tra

- Migration `202607230005_repair_quiz_schema.sql` đã được áp dụng thành công lên database Supabase từ xa.
- Lịch sử migration cục bộ và từ xa đã đồng bộ từ `202607200001` đến `202607230005`.
- Database đã được bổ sung quan hệ một tài liệu có nhiều `quiz_sets`.
- Mỗi câu hỏi quiz được liên kết với đúng `quiz_set_id`.
- Xóa một quiz không còn xóa các quiz khác được tạo từ cùng tài liệu.
- Người dùng có thể chọn `Easy`, `Medium` hoặc `Hard` trước khi tạo quiz.
- Độ khó được truyền vào prompt AI, lưu trong database và hiển thị trong thư viện quiz.
- Hai UI test Espresso đã chạy trực tiếp trên emulator Android 17 với kết quả `2 tests, 0 failures`.
- Tác vụ `connectedDebugAndroidTest` hoàn tất với trạng thái `BUILD SUCCESSFUL`.
- Tác vụ `testDebugUnitTest` hoàn tất với trạng thái `BUILD SUCCESSFUL`.
